package com.socialmedia.hichat.Activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;
import smartdevelop.ir.eram.showcaseviewlib.config.Gravity;
import smartdevelop.ir.eram.showcaseviewlib.listener.GuideListener;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;
import android.widget.SearchView;

import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.socialmedia.hichat.Adapter.ChatFirebaseAdapter;
import com.socialmedia.hichat.Adapter.StoryAdapter;
import com.socialmedia.hichat.Common.Common;
import com.socialmedia.hichat.Model.Contacts;
import com.socialmedia.hichat.Model.Story;

import com.socialmedia.hichat.R;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class ChatFragment extends Fragment {

    private View view;
    private ShimmerRecyclerView recyclerView;
    private Toolbar toolbar;
    private ShimmerRecyclerView recyclerView_story;
    private StoryAdapter storyAdapter;
    ChatFirebaseAdapter adapter;
    private List<Story> storyList;
    private List<String> contactList;
    private DatabaseReference chatsRef,usersRef;
    private Query query;
    private FirebaseAuth mAuth;
    private String currentUserID;
    private Context mContext;
    private RelativeLayout relativeLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        view =  inflater.inflate(R.layout.fragment_chat, container, false);
        MainActivity main = (MainActivity) getActivity();
        main.getSupportActionBar().hide();
        toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("Hi! Chat");
        /*((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle("Hi! Chat");*/
        toolbar.inflateMenu(R.menu.topmenu);
        Menu menu = toolbar.getMenu();
        MenuItem item = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) item.getActionView();
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String s) {
                                processSearch(s.toLowerCase());
                                return false;
                            }

                            @Override
                            public boolean onQueryTextChange(String s) {
                                processSearch(s.toLowerCase());
                                return false;
                            }
                        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){

                    case R.id.find_friend:
                        Intent intent=new Intent(mContext,FindFriendsActivity.class);
                        startActivity(intent);
                        break;
                    /*case R.id.search:

                        break;*/

                    case R.id.settings:
                        startActivity(new Intent(mContext,SettingActivity.class));
                        break;

                    case R.id.invite:
                        Intent inviteintent=new Intent(Intent.ACTION_SEND);
                        final String appPackagename=getContext().getPackageName();
                        String strApplink="";

                        try
                        {
                            strApplink="http://play.google.com/store/apps/details?id=" + appPackagename;
                        }
                        catch (ActivityNotFoundException a){
                            strApplink="http://play.google.com/store/apps/details?id=" + appPackagename;
                        }
                        inviteintent.setType("text/link");
                        String sharebody="Let's chat on Hi! Chat app! It's a simple app which we can use to message, update or check stories and can make a new friend from all over the world.\n"+
                                "Get it at:"+strApplink;
                        inviteintent.putExtra(Intent.EXTRA_TEXT,sharebody);
                        startActivity(inviteintent);
                        break;

                    case R.id.policy:
                        Uri uri = Uri.parse("https://numerous-saddle.000webhostapp.com/Hi!chatPolicy.html"); // missing 'http://' will cause crashed
                        Intent policyIntent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(policyIntent);
                        break;

                    case R.id.logout:
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setTitle("Logout")
                                .setMessage("Do you really want to logout?")
                                .setNegativeButton("CANCEL", (dialog, which) -> dialog.dismiss())
                                .setPositiveButton("LOGOUT", (dialog, which) -> {
                                    updateUserStatus("offline");
                                    mAuth.signOut();
                                    sendUserToAuthenticationActivity();
                                })
                                .setCancelable(false);
                        AlertDialog dialog = builder.create();
                        dialog.setOnShowListener(dialog1 ->  {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(ContextCompat.getColor(mContext,R.color.green));
                            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                    .setTextColor(ContextCompat.getColor(mContext,R.color.black));

                        });

                        dialog.show();
                        break;
                }
                return false;
            }
        });

        SharedPreferences preferences =getContext().getSharedPreferences("myPref", MODE_PRIVATE);
        boolean shouldInsertData = preferences.getBoolean("shouldInsertData", true);

        if(shouldInsertData){

            //insert your data into the preferences
            new GuideView.Builder(getContext())
                    .setContentText("A lot of Features like Find Friends, Settings(update profile), Privacy Policy and more can be found here.")
                    .setGravity(Gravity.auto)
                    .setDismissType(DismissType.anywhere)
                    .setTargetView(toolbar.findViewById(R.id.menu_list))
                    .setContentTextSize(16)
                    .setGuideListener(new GuideListener() {
                        @Override
                        public void onDismiss(View view) {

                        }
                    }).build().show();

            preferences.edit().putBoolean("shouldInsertData", false).apply();

        }


        relativeLayout = view.findViewById(R.id.chat_item_relative);
        swipeRefreshLayout=view.findViewById(R.id.swipe_layout);
        recyclerView = view.findViewById(R.id.recycler_chat_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();


        chatsRef = FirebaseDatabase.getInstance().getReference();
        query = chatsRef.child("contacts").child(currentUserID).orderByChild("timeStamp");
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");

        recyclerView_story = view.findViewById(R.id.statusList);
        recyclerView_story.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager1 = new LinearLayoutManager(mContext,
                LinearLayoutManager.HORIZONTAL,false);
        recyclerView_story.setLayoutManager(linearLayoutManager1);
        storyList = new ArrayList<>();
        storyAdapter = new StoryAdapter(mContext, storyList);
        recyclerView_story.setAdapter(storyAdapter);
        recyclerView_story.showShimmerAdapter();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Common.isConnectedToInternet(mContext))
                {
                    loadchatList();
                    checkContact();
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
                if (Common.isConnectedToInternet(mContext)) {
                    loadchatList();
                    checkContact();
                }
                else
                {
                    Snackbar.make(relativeLayout, "Please check your connection !!", Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
        });


        return view;
    }

    private void processSearch(String s) {

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(FirebaseDatabase.getInstance().getReference().child("contacts").child(currentUserID).orderByChild("name").startAt(s).endAt(s +"\uf8ff"),Contacts.class)
                        .build();

        ChatFirebaseAdapter adapter = new ChatFirebaseAdapter(mContext,options,relativeLayout);
        recyclerView.setAdapter(adapter);
        adapter.startListening();

    }

    private void checkContact()
    {
        
        contactList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("contacts")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactList.clear();
                for (DataSnapshot snapshot1:snapshot.getChildren())
                {
                    contactList.add(snapshot1.getKey());
                }

                readStory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }



    private void loadchatList() {
        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(query,Contacts.class)
                        .build();

        adapter = new ChatFirebaseAdapter(mContext, options, relativeLayout);
        recyclerView.setAdapter(adapter);
        recyclerView.showShimmerAdapter();
        adapter.startListening();
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        loadchatList();
        checkContact();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private void readStory(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("story");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long timecurrent = System.currentTimeMillis();
                storyList.clear();
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    storyList.add(new Story("", 0, 0, "",
                            FirebaseAuth.getInstance().getCurrentUser().getUid()));
                }
                for (String id : contactList){
                    int countStory = 0;
                    Story story = null;
                    for (DataSnapshot snapshot1 : snapshot.child(id).getChildren()){
                        story = snapshot1.getValue(Story.class);
                        if (timecurrent > story.getTimestart() && timecurrent < story.getTimeend()){
                            countStory++;
                        }
                    }

                    if (countStory > 0){
                        storyList.add(story);
                    }
                }
                recyclerView_story.hideShimmerAdapter();
                recyclerView.hideShimmerAdapter();
                storyAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void updateUserStatus(String state)
    {


        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("state",state);

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference().child("presence").child(currentUserID)
                .updateChildren(onlineStateMap);

    }

    private void sendUserToAuthenticationActivity() {
        Intent loginIntent = new Intent(mContext,Authentication.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        getActivity().finish();
    }
}