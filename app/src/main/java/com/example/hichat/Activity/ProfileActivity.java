package com.example.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
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
import com.example.hichat.R;
import com.example.hichat.databinding.ActivityProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileBinding binding;
    private String receiverUserID,senderUserID;
    private String current_state;
    FirebaseAuth mAuth;

    private DatabaseReference userRef,chatRequestRef,contactsRef;
    String userToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        senderUserID = mAuth.getCurrentUser().getUid();
        updateStatus("online");

        userRef = FirebaseDatabase.getInstance().getReference().child("users");

        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("chat requests");
        contactsRef = FirebaseDatabase.getInstance().getReference().child("contacts");
        receiverUserID = getIntent().getExtras().get("visit_user_id").toString();

        current_state = "new";
        retrieveUserInfo();
    }

    private void retrieveUserInfo() {

        userRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ((snapshot.exists()) && (snapshot.hasChild("profileImage")))
                {
                    String userImage = snapshot.child("profileImage").getValue().toString();
                    String userName = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();
                     userToken = snapshot.child("token").getValue().toString();

                    Glide.with(ProfileActivity.this)
                            .load(userImage)
                            .placeholder(R.drawable.avatar)
                            .into(binding.visitProfileImage);

                    binding.visitUserName.setText(userName);
                    binding.visitStatus.setText(userStatus);

                    manageChatRequest();

                }
                else
                {
                    String userName = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();

                    binding.visitUserName.setText(userName);
                    binding.visitStatus.setText(userStatus);

                    manageChatRequest();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void manageChatRequest() {
        chatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(receiverUserID))
                        {
                            String request_type = snapshot.child(receiverUserID).child("request_type").getValue().toString();

                            if (request_type.equals("sent"))
                            {
                                current_state = "request_sent";
                                binding.sendRequesBtn.setText("Cancel Chat Request");
                            }
                            else if (request_type.equals("received"))
                            {
                                current_state="request_received";
                                binding.sendRequesBtn.setText("Accept Request");
                                binding.declineRequestBtn.setVisibility(View.VISIBLE);
                                binding.declineRequestBtn.setEnabled(true);

                                binding.declineRequestBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        cancelChatRequest();
                                    }
                                });
                            }
                        }
                        else
                        {
                            contactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.hasChild(receiverUserID))
                                            {
                                                current_state = "friends";
                                                binding.sendRequesBtn.setText("Remove Contact");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

          binding.sendRequesBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    binding.sendRequesBtn.setEnabled(false);

                    if (current_state.equals("new"))
                    {
                        sendChatRequest();
                    }

                    if (current_state.equals("request_sent"))
                    {
                        cancelChatRequest();
                    }
                    if (current_state.equals("request_received"))
                    {
                        acceptRequest();
                    }
                    if (current_state.equals("friends"))
                    {
                        removeSpecificContact();
                    }

                }
            });
        }

    private void removeSpecificContact() {

        contactsRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {
                            contactsRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {
                                                binding.sendRequesBtn.setEnabled(true);
                                                current_state="new";
                                                binding.sendRequesBtn.setText("Send Request");

                                                binding.declineRequestBtn.setVisibility(View.INVISIBLE);
                                                binding.declineRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    private void acceptRequest() {

        contactsRef.child(senderUserID).child(receiverUserID)
                .child("contacts").setValue("saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {
                            contactsRef.child(receiverUserID).child(senderUserID)
                                    .child("contacts").setValue("saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {
                                                chatRequestRef.child(senderUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful())
                                                                {
                                                                    chatRequestRef.child(receiverUserID).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                    binding.sendRequesBtn.setEnabled(true);
                                                                                    current_state = "friends";
                                                                                    binding.sendRequesBtn.setText("Remove Contact");

                                                                                    binding.declineRequestBtn.setVisibility(View.INVISIBLE);
                                                                                    binding.declineRequestBtn.setEnabled(false);
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

    private void cancelChatRequest() {
        chatRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {
                            chatRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {
                                                binding.sendRequesBtn.setEnabled(true);
                                                current_state="new";
                                                binding.sendRequesBtn.setText("Send Request");

                                                binding.declineRequestBtn.setVisibility(View.INVISIBLE);
                                                binding.declineRequestBtn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void sendChatRequest() {
        chatRequestRef.child(senderUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {
                            chatRequestRef.child(receiverUserID).child(senderUserID)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful())
                                            {

                                                binding.sendRequesBtn.setEnabled(true);
                                                current_state = "request_sent";
                                                binding.sendRequesBtn.setText("Cancel Chat Request");
                                                userRef.child(senderUserID)
                                                        .addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                if (snapshot.exists())
                                                                {
                                                                    String reqSenderName = snapshot.child("name").getValue().toString();
                                                                    sendNotification("New Chat Request",reqSenderName+" wants to connect with you",userToken);
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

    void sendNotification(String title, String message, String token){
        try {

            RequestQueue queue = Volley.newRequestQueue(this);
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

    private void updateStatus(String state)
    {
        HashMap<String, Object> stateMap = new HashMap<>();
        stateMap.put("state",state);
        FirebaseDatabase.getInstance().getReference().child("presence").child(senderUserID)
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
}