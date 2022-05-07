package com.socialmedia.hichat.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.cooltechworks.views.shimmer.ShimmerRecyclerView;
import com.socialmedia.hichat.Common.Common;
import com.socialmedia.hichat.Model.Contacts;

import com.socialmedia.hichat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RequestsFragment extends Fragment {

    private View view;
    private ShimmerRecyclerView recyclerView;
    private DatabaseReference chatRequestRef,userRef,contactRef;
    private FirebaseAuth mAuth;
    private String currentUserID;
    String currentUserName;
    private Activity mActivity;
    private RelativeLayout relativeLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressDialog dialog;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = getActivity();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    public RequestsFragment() {
        // Required empty public constructor
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
        view = inflater.inflate(R.layout.fragment_requests, container, false);
        MainActivity main = (MainActivity) getActivity();
        main.getSupportActionBar().show();
        main.getSupportActionBar().setTitle("Request");
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        swipeRefreshLayout=view.findViewById(R.id.swipe_layout);
        relativeLayout=view.findViewById(R.id.request_item_relative);
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("chat requests");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");
        contactRef = FirebaseDatabase.getInstance().getReference().child("contacts");
        recyclerView = view.findViewById(R.id.recycler_chat_request_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(mActivity));

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
                if (Common.isConnectedToInternet(mActivity))
                {
                    loadRequestList();
                    handler.postDelayed(hideAdapter,200);
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
                if (Common.isConnectedToInternet(mActivity)) {
                    loadRequestList();
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

    private void loadRequestList() {
        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(chatRequestRef.child(currentUserID),Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts,RequestViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull Contacts model) {

                holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);

                userRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {
                            currentUserName = snapshot.child("name").getValue().toString();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
                final  String list_user_id = getRef(position).getKey();


                DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();
                getTypeRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {
                            String type = snapshot.getValue().toString();

                            if (type.equals("received"))
                            {
                                userRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.hasChild("profileImage"))
                                        {
                                            final String profileImage = snapshot.child("profileImage").getValue().toString();


                                            if (mActivity == null)
                                            {
                                                return;
                                            }
                                            Glide.with(mActivity).load(profileImage)
                                                    .placeholder(R.drawable.avatar)
                                                    .into(holder.profileImage);
                                        }


                                        final String userName = snapshot.child("name").getValue().toString();
                                        final String userStatus = snapshot.child("status").getValue().toString();
                                        final String userToken = snapshot.child("token").getValue().toString();

                                        holder.userName.setText(userName);
                                        holder.userStatus.setText("wants to connect with you");

                                        holder.itemView.findViewById(R.id.request_accept_btn).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                dialog = new ProgressDialog(mActivity);
                                                dialog.setMessage("Please wait...");
                                                dialog.setCancelable(false);
                                                dialog.show();

                                                Date date = new Date();
                                                HashMap<String,Object> friendMap = new HashMap<>();
                                                friendMap.put("contact","Saved");
                                                friendMap.put("timeStamp",-1*date.getTime());
                                                friendMap.put("name",userName.toLowerCase());

                                                HashMap<String,Object> currentUserMap = new HashMap<>();
                                                currentUserMap.put("contact","Saved");
                                                currentUserMap.put("timeStamp",-1*date.getTime());
                                                currentUserMap.put("name",currentUserName.toLowerCase());

                                                contactRef.child(currentUserID).child(list_user_id)
                                                        .setValue(friendMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful())
                                                        {
                                                            contactRef.child(list_user_id).child(currentUserID)
                                                                    .setValue(currentUserMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful())
                                                                    {
                                                                        chatRequestRef.child(currentUserID).child(list_user_id)
                                                                                .removeValue()
                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                    @Override
                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                        if (task.isSuccessful())
                                                                                        {
                                                                                            chatRequestRef.child(list_user_id).child(currentUserID)
                                                                                                    .removeValue()
                                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                                            if (task.isSuccessful())
                                                                                                            {
                                                                                                                if (mActivity == null)
                                                                                                                {
                                                                                                                    return;
                                                                                                                }
                                                                                                                Toast.makeText(mActivity, "New Contact Added", Toast.LENGTH_SHORT).show();
                                                                                                                userRef.child(currentUserID)
                                                                                                                        .addValueEventListener(new ValueEventListener() {
                                                                                                                            @Override
                                                                                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                                                                                if (snapshot.exists())
                                                                                                                                {
                                                                                                                                    String reqSenderName = snapshot.child("name").getValue().toString();
                                                                                                                                    sendNotification("New Contact Added",reqSenderName+" has accepted your request",userToken);
                                                                                                                                }
                                                                                                                            }

                                                                                                                            @Override
                                                                                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                                                                                            }
                                                                                                                        });
                                                                                                            }
                                                                                                        }
                                                                                                    });
                                                                                        }
                                                                                    }
                                                                                });
                                                                    }
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            }
                                        });

                                        holder.itemView.findViewById(R.id.request_cancel_btn).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                chatRequestRef.child(currentUserID).child(list_user_id)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful())
                                                                {
                                                                    chatRequestRef.child(list_user_id).child(currentUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful())
                                                                                    {
                                                                                        if (mActivity == null)
                                                                                        {
                                                                                            return;
                                                                                        }
                                                                                        Toast.makeText(mActivity, "Cancelled Request", Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        });



                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }

                            else if (type.equals("sent"))
                            {
                                Button request_sent_btn = holder.itemView.findViewById(R.id.request_accept_btn);
                                request_sent_btn.setText("Req Sent");

                                //holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.INVISIBLE);

                                userRef.child(list_user_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.hasChild("profileImage"))
                                        {
                                            final String profileImage = snapshot.child("profileImage").getValue().toString();

                                            if (mActivity==null)
                                            {
                                                return;
                                            }
                                            Glide.with(mActivity).load(profileImage)
                                                    .placeholder(R.drawable.avatar)
                                                    .into(holder.profileImage);
                                        }


                                        final String userName = snapshot.child("name").getValue().toString();
                                        final String userStatus = snapshot.child("status").getValue().toString();

                                        holder.userName.setText(userName);
                                        holder.userStatus.setText("you have sent a request to "+userName);

                                        holder.itemView.findViewById(R.id.request_cancel_btn).setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[] = new CharSequence[]
                                                        {
                                                                "Cancel Chat Request"
                                                        };

                                                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                                                builder.setTitle("Already Sent Request");
                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int i) {
                                                        if (i==0)
                                                        {
                                                            chatRequestRef.child(currentUserID).child(list_user_id)
                                                                    .removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful())
                                                                            {
                                                                                chatRequestRef.child(list_user_id).child(currentUserID)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if (task.isSuccessful())
                                                                                                {
                                                                                                    if (mActivity ==null)
                                                                                                    {
                                                                                                        return;
                                                                                                    }
                                                                                                    Toast.makeText(mActivity, "Cancelled Request", Toast.LENGTH_SHORT).show();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }

            @NonNull
            @Override
            public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_display_layout,parent,false);
                return new RequestViewHolder(view);
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

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView userName,userStatus;
        CircleImageView profileImage;
        Button acceptBtn,cancelBtn;
        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.find_friend_username);
            userStatus = itemView.findViewById(R.id.find_friend_status);
            profileImage = itemView.findViewById(R.id.find_friend_profileimg);
            acceptBtn = itemView.findViewById(R.id.request_accept_btn);
            cancelBtn= itemView.findViewById(R.id.request_cancel_btn);

        }
    }

    void sendNotification(String title, String message, String token){
        try {

            RequestQueue queue = Volley.newRequestQueue(mActivity);
            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject data = new JSONObject();
            data.put("title", title);
            data.put("body", message);
            JSONObject notificationData = new JSONObject();
            notificationData.put("notification", data);
            notificationData.put("to",token);

            JsonObjectRequest request = new JsonObjectRequest(url, notificationData
                    , new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (mActivity == null)
                    {
                        return;
                    }
                    dialog.dismiss();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Toast.makeText(ChatActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    NetworkResponse response = error.networkResponse;
                    if (error instanceof ServerError && response != null) {
                        try {
                            String res = new String(response.data,
                                    HttpHeaderParser.parseCharset(response.headers, "utf-8"));
                            // Now you can use any deserializer to make sense of data
                            JSONObject obj = new JSONObject(res);
                        } catch (UnsupportedEncodingException e1) {
                            // Couldn't properly decode data to string
                            e1.printStackTrace();
                        } catch (JSONException e2) {
                            // returned data is not JSONObject?
                            e2.printStackTrace();
                        }
                    }
                }
            }){
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> map = new HashMap<>();
                    String key = "Key=AAAAlHGbxHc:APA91bFHC0mL4wwevrMa4aVuCGGSX0G4a99x6JI0uLypTnFoYQSCyM4EsCt-7ByNW-vUeD9g9qEqYIchUEP7YxwpApqaSShCyoqOdago3rRypgmCbRBHvxKCU8CGZNddt6a2qbPMg0ry";
                    map.put("Content-Type", "application/json");
                    map.put("Authorization", key);
                    return map;
                }
            };

            queue.add(request);

        }catch (Exception ex){

        }
    }

}