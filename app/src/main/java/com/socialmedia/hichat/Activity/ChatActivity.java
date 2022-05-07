package com.socialmedia.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import com.socialmedia.hichat.Adapter.MessagesAdapter;
import com.socialmedia.hichat.Model.Message;

import com.socialmedia.hichat.R;
import com.socialmedia.hichat.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;

    private String  messageReceiverID, messageReceiverName , messageReceiverImage, messageSenderID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef,reference,contactRef;

    private String token;
    private List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessagesAdapter messagesAdapter;
    private String checker = "", myUrl = "";
    private StorageTask uploadTask;
    private Uri fileUri;
    private ProgressDialog loadingBar;

    ValueEventListener seenListener;
    int unreadMsg=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        loadingBar = new ProgressDialog(this);


        mAuth = FirebaseAuth.getInstance();
        messageSenderID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        contactRef = FirebaseDatabase.getInstance().getReference("contacts");
        /*reference = FirebaseDatabase.getInstance().getReference("messages");*/
        updateStatus("online");

        messageReceiverID = getIntent().getStringExtra("uid");
        messageReceiverName = getIntent().getStringExtra("name");
        messageReceiverImage = getIntent().getStringExtra("image");
        token = getIntent().getStringExtra("token");

        /*messagesAdapter = new MessagesAdapter(this,messageList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(messagesAdapter);*/
        binding.recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ChatActivity.this);
        linearLayoutManager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        displayMessage(messageSenderID, messageReceiverID);

        binding.name.setText(messageReceiverName);
        Glide.with(ChatActivity.this)
                .load(messageReceiverImage)
                .placeholder(R.drawable.avatar)
                .into(binding.profileimg);

        checkUnreadMsg();

        binding.back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendMessage();
            }
        });

        displayLastSeen();

        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence options[] = new CharSequence[]
                        {
                                "Images",
                                "PDF Files",
                                "MS Word Files"

                        };
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select the File");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if (i == 0)
                        {
                            checker = "image";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent,"select Image"),438);
                        }
                        else if (i == 1)
                        {
                            checker = "pdf";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent,"select PDF File"),438);

                        }
                        else if (i == 2)
                        {
                            checker = "docx";

                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent,"select MS Word File"),438);
                        }
                    }
                });
                builder.show();
            }
        });

        final Handler handler = new Handler();
        binding.messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                HashMap<String, Object> stateMap = new HashMap<>();
                stateMap.put("state","typing...");
                RootRef.child("presence").child(messageSenderID)
                        .updateChildren(stateMap);
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(userStoppedTyping,1000);

            }

            Runnable userStoppedTyping = new Runnable() {
                @Override
                public void run() {
                    HashMap<String, Object> stateMap = new HashMap<>();
                    stateMap.put("state","online");
                    RootRef.child("presence").child(messageSenderID)
                            .updateChildren(stateMap);
                }
            };
        });

    }

    private void checkUnreadMsg() {

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("messages");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snapshot1:snapshot.getChildren())
                        {
                            Message message = snapshot1.getValue(Message.class);

                            if (message.getFrom().equals(messageSenderID) && message.getTo().equals(messageReceiverID))
                            {
                                if (message.isSeen == 1) {
                                    unreadMsg=1;

                                } else {
                                    unreadMsg = message.unreadMsg;
                                    unreadMsg++;
                                }
                            }
                        }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null)
        {

            loadingBar.setTitle("Sending "+ checker);
            loadingBar.setMessage("Please wait, Image/File is getting sent...");
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();

            Date date = new Date();

            fileUri = data.getData();
            if (!checker.equals("image"))
            {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");

                DatabaseReference userMessageKeyRef = RootRef.child("messages").push();

                String messagePushID = userMessageKeyRef.getKey();

                StorageReference filePath = storageReference.child(messagePushID + "." + checker);

                filePath.putFile(fileUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String downloadUrl = uri.toString();

                                Calendar calendar = Calendar.getInstance();
                                SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
                                String saveCurrentDateForDox = currentDate.format(calendar.getTime());

                                SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
                                String saveCurrentTimeForDox = currentTime.format(calendar.getTime());

                                Map messageImageBody = new HashMap();
                                messageImageBody.put("message",downloadUrl);
                                messageImageBody.put("name",fileUri.getLastPathSegment());
                                messageImageBody.put("type",checker);
                                messageImageBody.put("from",messageSenderID);
                                messageImageBody.put("to", messageReceiverID);
                                messageImageBody.put("messageID", messagePushID);
                                messageImageBody.put("time", saveCurrentTimeForDox);
                                messageImageBody.put("date", saveCurrentDateForDox);
                                messageImageBody.put("unreadMsg",unreadMsg);
                                messageImageBody.put("timeStamp",-1*date.getTime());


                                userMessageKeyRef.setValue(messageImageBody);
                                loadingBar.dismiss();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                loadingBar.dismiss();
                                Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double p = (100.0* taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        loadingBar.setMessage((int) p + " % Uploading...");
                    }
                }).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful())
                    {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, "Message sent successfully", Toast.LENGTH_SHORT).show();
                        FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .child(messageSenderID)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists())
                                        {
                                            final String msgSenderName = snapshot.child("name").getValue().toString();
                                            //Message message = new Message(messageSenderID,messageText,"text");
                                            sendNotification(msgSenderName,"Pdf/Docx",token);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                        updateTimeStamp();

                    }
                    else
                    {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }
                    binding.messageBox.setText("");
                }
            });

            }
            else if (checker.equals("image"))
            {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");


                DatabaseReference userMessageKeyRef = RootRef.child("messages").push();

                String messagePushID = userMessageKeyRef.getKey();

                StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");

                uploadTask = filePath.putFile(fileUri);

                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception
                    {
                        if (!task.isSuccessful())
                        {
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                      if (task.isSuccessful())
                      {
                          Uri downloadUrl = task.getResult();
                          myUrl = downloadUrl.toString();

                          Calendar calendar = Calendar.getInstance();
                          SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
                          String saveCurrentDateForImg = currentDate.format(calendar.getTime());

                          SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
                          String saveCurrentTimeForImg = currentTime.format(calendar.getTime());

                          Map messageImageBody = new HashMap();
                          messageImageBody.put("message", myUrl);
                          messageImageBody.put("name", fileUri.getLastPathSegment());
                          messageImageBody.put("type", checker);
                          messageImageBody.put("from", messageSenderID);
                          messageImageBody.put("to",messageReceiverID);
                          messageImageBody.put("messageID", messagePushID);
                          messageImageBody.put("time",saveCurrentTimeForImg);
                          messageImageBody.put("date",saveCurrentDateForImg);
                          messageImageBody.put("unreadMsg",unreadMsg);
                          messageImageBody.put("timeStamp",-1*date.getTime());


                          userMessageKeyRef.setValue(messageImageBody).addOnCompleteListener(new OnCompleteListener() {
                              @Override
                              public void onComplete(@NonNull Task task) {
                                  if (task.isSuccessful())
                                  {
                                      loadingBar.dismiss();
                                      Toast.makeText(ChatActivity.this, "Message sent successfully", Toast.LENGTH_SHORT).show();
                                      FirebaseDatabase.getInstance().getReference()
                                              .child("users")
                                              .child(messageSenderID)
                                              .addValueEventListener(new ValueEventListener() {
                                                  @Override
                                                  public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                      if (snapshot.exists())
                                                      {
                                                          final String msgSenderName = snapshot.child("name").getValue().toString();
                                                          //Message message = new Message(messageSenderID,messageText,"text");
                                                          sendNotification(msgSenderName,"Photo.jpg",token);
                                                      }
                                                  }

                                                  @Override
                                                  public void onCancelled(@NonNull DatabaseError error) {

                                                  }
                                              });

                                      updateTimeStamp();

                                  }
                                  else
                                  {
                                      loadingBar.dismiss();
                                      Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                                  }
                                  binding.messageBox.setText("");
                              }
                          });

                      }
                    }
                });

            }
            else
            {
                loadingBar.dismiss();
                Toast.makeText(this, "Nothing Selected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void displayLastSeen()
    {
        RootRef.child("presence")
                .child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    if (snapshot.hasChild("state")) {
                        String state = snapshot.child("state").getValue().toString();
                        String date = snapshot.child("date").getValue().toString();
                        String time = snapshot.child("time").getValue().toString();

                        if (state.equals("online")) {
                            binding.lastSeen.setText("online");
                        } else if (state.equals("offline")) {
                            binding.lastSeen.setText("Last Seen: " + date + " " + time);
                        } else if (state.equals("typing...")) {
                            binding.lastSeen.setText("typing...");
                        }
                    } else {
                        binding.lastSeen.setText("offline");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    void sendNotification(String name, String message, String token){
        try {

            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "https://fcm.googleapis.com/fcm/send";

            JSONObject data = new JSONObject();
            data.put("title", name);
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

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        seenMessage(messageReceiverID);
        updateStatus("online");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        messageList.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //messageList.clear();
        reference.removeEventListener(seenListener);
        updateStatus("offline");
    }


    @Override
    protected void onRestart() {
        super.onRestart();
        updateStatus("online");
        //displayMessage(messageSenderID, messageReceiverID);

    }

    private void sendMessage()
    {

        Date date = new Date();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        String saveCurrentDateForText = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        String saveCurrentTimeForText = currentTime.format(calendar.getTime());
        String messageText = binding.messageBox.getText().toString();

        if (TextUtils.isEmpty(messageText))
        {
            Toast.makeText(this, "first write your message...", Toast.LENGTH_SHORT).show();
        }
        else
        {

            DatabaseReference userMessageKeyRef = RootRef.child("messages").push();

            final String messagePushID = userMessageKeyRef.getKey();


            Map messageTextBody = new HashMap();
            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to",messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time",saveCurrentTimeForText);
            messageTextBody.put("date",saveCurrentDateForText);
            messageTextBody.put("isSeen",0);
            messageTextBody.put("unreadMsg",unreadMsg);
            messageTextBody.put("timeStamp",-1*date.getTime());

            userMessageKeyRef.setValue(messageTextBody).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful())
                    {
                        binding.messageBox.setText("");
                        FirebaseDatabase.getInstance().getReference()
                                .child("users")
                                .child(messageSenderID)
                                .addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists())
                                        {
                                            final String msgSenderName = snapshot.child("name").getValue().toString();
                                            //Message message = new Message(messageSenderID,messageText,"text");
                                            sendNotification(msgSenderName,messageText,token);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });

                    }
                    else
                    {
                        Toast.makeText(ChatActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }



                }
            });

            updateTimeStamp();

        }

    }

    private void updateTimeStamp() {

        DatabaseReference msgRef = FirebaseDatabase.getInstance().getReference("messages");
        msgRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot:snapshot.getChildren())
                {
                    Message message=dataSnapshot.getValue(Message.class);

                    if (message.getFrom().equals(messageSenderID) && message.getTo().equals(messageReceiverID) ||
                            message.getFrom().equals(messageReceiverID) && message.getTo().equals(messageSenderID))
                    {
                        HashMap<String,Object> hashMap = new HashMap();
                        hashMap.put("timeStamp",message.getTimeStamp());

                        contactRef.child(messageSenderID).child(messageReceiverID).updateChildren(hashMap)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful())
                                        {
                                            contactRef.child(messageReceiverID)
                                                    .child(messageSenderID)
                                                    .updateChildren(hashMap);
                                        }
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

    private void seenMessage(String friendId)
    {

       reference = FirebaseDatabase.getInstance().getReference("messages");

                seenListener = reference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snapshot1:snapshot.getChildren()){
                            Message message = snapshot1.getValue(Message.class);
                            if (message.getTo().equals(messageSenderID) && message.getFrom().equals(friendId))
                            {
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("isSeen",1);
                                hashMap.put("unreadMsg",0);
                                snapshot1.getRef().updateChildren(hashMap);

                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


    }

    private void displayMessage(final String myid, final String friendid) {

        messageList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("messages");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();

                for (DataSnapshot ds: snapshot.getChildren()) {

                    Message message = ds.getValue(Message.class);

                    if (message.getFrom().equals(myid) && message.getTo().equals(friendid) ||
                            message.getFrom().equals(friendid) && message.getTo().equals(myid)) {

                        messageList.add(message);
                    }

                    messagesAdapter = new MessagesAdapter(ChatActivity.this,messageList);
                    binding.recyclerView.setAdapter(messagesAdapter);

                }

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
        RootRef.child("presence").child(messageSenderID)
                .updateChildren(stateMap);
    }

    
}
