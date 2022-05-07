package com.socialmedia.hichat.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;
import com.socialmedia.hichat.Activity.ChatActivity;
import com.socialmedia.hichat.Model.Contacts;
import com.socialmedia.hichat.Model.Message;

import com.socialmedia.hichat.R;
import com.socialmedia.hichat.View.DialogViewUser;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class ChatFirebaseAdapter extends FirebaseRecyclerAdapter<Contacts, ChatFirebaseAdapter.ChatsViewHolder> {

    String theLastMsg,theLastMsgTime;
    Context mContext;
    private FirebaseAuth mAuth;
    private String currentUserID;
    private RelativeLayout relativeLayout;

    public ChatFirebaseAdapter(Context context, @NonNull FirebaseRecyclerOptions<Contacts> options,RelativeLayout relativeLayout) {
        super(options);
        this.mContext = context;
        this.relativeLayout=relativeLayout;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatsViewHolder holder, int position, @NonNull Contacts model) {

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        final String usersIDs = getRef(position).getKey();

        FirebaseDatabase.getInstance().getReference().child("users").child(usersIDs).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists())
                {

                    String retrieveImage = null;
                    if (snapshot.hasChild("profileImage"))
                    {
                        retrieveImage = snapshot.child("profileImage").getValue().toString();

                        if (mContext == null)
                        {
                            return;
                        }
                        Glide.with(mContext.getApplicationContext())
                                .load(retrieveImage)
                                .placeholder(R.drawable.avatar)
                                .into(holder.profileImage);
                    }

                    final String retrieveName = snapshot.child("name").getValue().toString();
                    final String retrieveToken = snapshot.child("token").getValue().toString();
                    /*final String retrieveLastMsg = snapshot.child("status").getValue().toString();*/

                    holder.userName.setText(retrieveName);


                    FirebaseDatabase.getInstance().getReference("presence")
                            .child(usersIDs).addValueEventListener(new ValueEventListener() {
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

                    String finalRetrieveImage = retrieveImage;
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(mContext, ChatActivity.class);
                            intent.putExtra("name",retrieveName);
                            intent.putExtra("image", finalRetrieveImage);
                            intent.putExtra("uid",usersIDs);
                            intent.putExtra("token",retrieveToken);
                            mContext.startActivity(intent);
                        }
                    });

                    holder.profileImage.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new DialogViewUser(mContext,finalRetrieveImage);
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        if (FirebaseAuth.getInstance().getCurrentUser().getUid() !=null)
        {
            lastMessage(usersIDs,holder.lastMsg,holder.msgTime,holder.unread_msg_count);
        }else
        {
            holder.lastMsg.setText("Tap to chat");
            holder.msgTime.setText("00:00 PM");
        }
    }

    @NonNull
    @Override
    public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_item,parent,false);
        return new ChatsViewHolder(view);
    }

    @Override
    public void onDataChanged() {
        super.onDataChanged();
        if (getItemCount() == 0)
        {
            relativeLayout.setVisibility(View.VISIBLE);
        }
        else
        {
            relativeLayout.setVisibility(View.GONE);
        }
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {

        CircleImageView profileImage,onlineIcon;
        TextView userName,lastMsg,msgTime;
        ImageView unread_msg_count;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            userName = itemView.findViewById(R.id.username);
            lastMsg = itemView.findViewById(R.id.lastMsg);
            profileImage = itemView.findViewById(R.id.profileimg);
            msgTime = itemView.findViewById(R.id.msgTime);
            onlineIcon = itemView.findViewById(R.id.online_icon);
            unread_msg_count=itemView.findViewById(R.id.unread_msg_count);
        }
    }

    private void lastMessage(String userId, TextView last_msg, TextView last_msg_time, ImageView unread_msg_count){
        theLastMsg = "default";
        theLastMsgTime = "default";
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("messages");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snapshot1:snapshot.getChildren()){
                    Message message = snapshot1.getValue(Message.class);
                    if (message.getTo().equals(currentUserID) && message.getFrom().equals(userId) ||
                            message.getTo().equals(userId) && message.getFrom().equals(currentUserID)){
                        theLastMsgTime = message.getTime();
                        if (message.getType().equals("text")) {
                            theLastMsg = message.getMessage();
                        }else if (message.getType().equals("image"))
                        {
                            theLastMsg="Photo.jpg";
                        }else if (message.getType().equals("pdf"))
                        {
                            theLastMsg="Pdf File";
                        }
                        else if (message.getType().equals("docx"))
                        {
                            theLastMsg="MsWord docx File";
                        }

                    }
                    if (message.getFrom().equals(userId)&&message.getTo().equals(currentUserID))
                    {
                        if (message.unreadMsg != 0) {
                            TextDrawable drawable = TextDrawable.builder()
                                    .buildRound("" + message.unreadMsg, Color.RED);
                            unread_msg_count.setImageDrawable(drawable);
                        }
                    }
                }

                switch (theLastMsg)
                {
                    case "default":
                        last_msg.setText("Tap to chat");
                        break;

                    default:
                        last_msg.setText(theLastMsg);
                        break;
                }

                theLastMsg = "default";
                switch (theLastMsgTime)
                {
                    case "default":
                        last_msg_time.setText("00:00 pm");
                        break;

                    default:
                        last_msg_time.setText(theLastMsgTime);
                }
                theLastMsgTime="default";

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}