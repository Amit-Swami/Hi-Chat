package com.example.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.hdodenhof.circleimageview.CircleImageView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.hichat.Adapter.ContactsAdapter;
import com.example.hichat.Common.Common;
import com.example.hichat.Model.Contacts;
import com.example.hichat.Model.User;
import com.example.hichat.R;
import com.example.hichat.databinding.ActivityFindFriendsBinding;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FindFriendsActivity extends AppCompatActivity {

    ActivityFindFriendsBinding binding;
    FirebaseDatabase database;
    ArrayList<Contacts> contacts;
    ContactsAdapter contactsAdapter;
    SwipeRefreshLayout swipeRefreshLayout;
    RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding =ActivityFindFriendsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Find Friends");
        swipeRefreshLayout=findViewById(R.id.swipe_layout);
        relativeLayout=findViewById(R.id.relativelayout);
        database = FirebaseDatabase.getInstance();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Common.isConnectedToInternet(FindFriendsActivity.this))
                {
                    loadFindFriendList();
                }
                else
                {
                    Snackbar.make(relativeLayout, "Please check your connection !!", Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
        });

        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (Common.isConnectedToInternet(FindFriendsActivity.this)) {
                    loadFindFriendList();
                }
                else
                {
                    Snackbar.make(relativeLayout, "Please check your connection !!", Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
        });

    }

    private void loadFindFriendList() {

        updateStatus("online");
        contacts = new ArrayList<>();

        contactsAdapter = new ContactsAdapter(this,contacts);

        binding.findFriendsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.findFriendsRecyclerview.setAdapter(contactsAdapter);
        binding.findFriendsRecyclerview.showShimmerAdapter();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contacts.clear();
                for (DataSnapshot snapshot1:snapshot.getChildren()){
                    Contacts contact = snapshot1.getValue(Contacts.class);
                    if (!contact.getUid().equals(FirebaseAuth.getInstance().getUid())) {
                        contacts.add(contact);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
                binding.findFriendsRecyclerview.hideShimmerAdapter();
                contactsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    private void updateStatus(String state)
    {
        HashMap<String, Object> stateMap = new HashMap<>();
        stateMap.put("state",state);
        database.getReference().child("presence").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(stateMap);
    }

    @Override
    protected void onPause() {
        updateStatus("offline");
        super.onPause();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateStatus("online");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus("online");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.findfriendmenu,menu);
        MenuItem item = menu.findItem(R.id.contactsearch);
        androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) item.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String newText) {
                processSearch();
                contactsAdapter.getFilter().filter(newText);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                processSearch();
                contactsAdapter.getFilter().filter(newText);

                return false;
            }
        });
        return true;
    }

    private void processSearch() {
        contactsAdapter = new ContactsAdapter(this,contacts);
        binding.findFriendsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.findFriendsRecyclerview.setAdapter(contactsAdapter);

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contacts.clear();
                for (DataSnapshot snapshot1:snapshot.getChildren()){
                    Contacts contact = snapshot1.getValue(Contacts.class);
                    if (!contact.getUid().equals(FirebaseAuth.getInstance().getUid()))
                        contacts.add(contact);
                }
                contactsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}