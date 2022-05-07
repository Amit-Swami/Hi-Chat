package com.socialmedia.hichat.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import de.hdodenhof.circleimageview.CircleImageView;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.socialmedia.hichat.Common.Common;
import com.socialmedia.hichat.Model.Contacts;

import com.socialmedia.hichat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ContactsFragment extends Fragment {

    private View view;
    //private Toolbar toolbar;
    private ShimmerRecyclerView recyclerView;
    private DatabaseReference contactRef,userRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    private Context mContext;
    private RelativeLayout relativeLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    public ContactsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mContext = context;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem item=menu.findItem(R.id.search);
        if(item!=null)
            item.setVisible(false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_contacts, container, false);

        MainActivity main = (MainActivity) getActivity();
        main.getSupportActionBar().show();
        main.getSupportActionBar().setTitle("Saved Contact");
        swipeRefreshLayout=view.findViewById(R.id.swipe_layout);
        relativeLayout=view.findViewById(R.id.contact_item_relative);
        recyclerView = view.findViewById(R.id.recycler_contact_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        contactRef  = FirebaseDatabase.getInstance().getReference().child("contacts").child(currentUserID);
        userRef  = FirebaseDatabase.getInstance().getReference().child("users");
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        final Handler handler = new Handler();
        Runnable hideAdapter = new Runnable() {
            @Override
            public void run() {
                recyclerView.hideShimmerAdapter();
            }
        };
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (Common.isConnectedToInternet(mContext))
                {
                    loadContactList();
                    handler.postDelayed(hideAdapter,500);

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
                    loadContactList();
                    handler.postDelayed(hideAdapter,200);
                }
                else
                {
                    Snackbar.make(relativeLayout, "Please check your connection !!", Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
        });


    }

    private void loadContactList() {

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(contactRef,Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts,ContactsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ContactsViewHolder holder, int position, @NonNull Contacts model) {

                String userIds = getRef(position).getKey();

                userRef.child(userIds).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot)
                    {
                        if (snapshot.exists())
                        {

                            FirebaseDatabase.getInstance().getReference("presence")
                                    .child(userIds).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.hasChild("state"))
                                    {
                                        String state = snapshot.child("state").getValue().toString();
                                        String date = snapshot.child("date").getValue().toString();
                                        String time = snapshot.child("time").getValue().toString();

                                        if (state.equals("online"))
                                        {
                                            holder.onlineIcon.setVisibility(View.VISIBLE);
                                        }
                                        else if (state.equals("offline"))
                                        {
                                            holder.onlineIcon.setVisibility(View.INVISIBLE);
                                        }
                                        else if (state.equals("typing..."))
                                        {
                                            holder.onlineIcon.setVisibility(View.VISIBLE);
                                        }
                                    }
                                    else{
                                        holder.onlineIcon.setVisibility(View.INVISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });


                            if (snapshot.hasChild("profileImage"))
                            {
                                if (mContext == null)
                                {
                                    return;
                                }
                                String profileImage = snapshot.child("profileImage").getValue().toString();
                                String userName = snapshot.child("name").getValue().toString();
                                String userStatus = snapshot.child("status").getValue().toString();

                                holder.userName.setText(userName);
                                holder.userStatus.setText(userStatus);
                                Glide.with(mContext).load(profileImage)
                                        .placeholder(R.drawable.avatar)
                                        .into(holder.profileImage);
                            }

                            else
                            {
                                String userName = snapshot.child("name").getValue().toString();
                                String userStatus = snapshot.child("status").getValue().toString();

                                holder.userName.setText(userName);
                                holder.userStatus.setText(userStatus);
                            }

                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String visit_user_id = userIds;
                                    Intent intent = new Intent(mContext, ProfileActivity.class);
                                    intent.putExtra("visit_user_id",visit_user_id);
                                    mContext.startActivity(intent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }

            @NonNull
            @Override
            public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_display_layout,parent,false);
                return new ContactsViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();
                if (getItemCount()==0)
                {
                    relativeLayout.setVisibility(View.VISIBLE);
                }
                else
                {
                    relativeLayout.setVisibility(View.GONE);
                }
            }
        };

        recyclerView.setAdapter(adapter);
        recyclerView.showShimmerAdapter();
        adapter.startListening();
        swipeRefreshLayout.setRefreshing(false);

    }
    

    public static class ContactsViewHolder extends  RecyclerView.ViewHolder {

        TextView userName , userStatus;
        CircleImageView profileImage, onlineIcon;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.find_friend_username);
            userStatus = itemView.findViewById(R.id.find_friend_status);
            profileImage = itemView.findViewById(R.id.find_friend_profileimg);
            onlineIcon = itemView.findViewById(R.id.online_icon);
        }
    }
}