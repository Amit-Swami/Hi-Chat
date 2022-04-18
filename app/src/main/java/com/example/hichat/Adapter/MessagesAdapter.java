package com.example.hichat.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.hichat.Activity.ImageViewerActivity;
import com.example.hichat.Activity.MainActivity;
import com.example.hichat.Model.Message;
import com.example.hichat.R;
import com.example.hichat.databinding.CustomMessagesLayoutBinding;
import com.example.hichat.databinding.ItemReceiveBinding;
import com.example.hichat.databinding.ItemSentBinding;
import com.github.pgreze.reactions.ReactionPopup;
import com.github.pgreze.reactions.ReactionsConfig;
import com.github.pgreze.reactions.ReactionsConfigBuilder;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    List<Message> userMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private Context context;

    //new
    public static final int MSG_TYPE_LEFT=0;
    public static final int MSG_TYPE_RIGHT=1;
    FirebaseUser fuser;

    public MessagesAdapter(Context context,List<Message> userMessagesList)
    {

        this.context = context;
        this.userMessagesList=userMessagesList;

    }

    @NonNull
    @Override

    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == MSG_TYPE_RIGHT)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_right, parent, false);
            //old-------mAuth = FirebaseAuth.getInstance();
            return new MessageViewHolder(view);
        }
        else
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_left, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        Message message = userMessagesList.get(position);

        String fromMsgType = message.getType();

        holder.show_message.setVisibility(View.GONE);
        holder.show_message_time.setVisibility(View.GONE);
        holder.seen_tick.setVisibility(View.GONE);
        holder.show_image.setVisibility(View.GONE);
        holder.show_img_time.setVisibility(View.GONE);
        holder.seen_img_tick.setVisibility(View.GONE);
        holder.show_document.setVisibility(View.GONE);
        holder.show_docx_time.setVisibility(View.GONE);
        holder.seen_docx_tick.setVisibility(View.GONE);

        if (fromMsgType.equals("text")) {


            if (message.getFrom().equals(fuser.getUid())) {

                holder.show_message.setVisibility(View.VISIBLE);
                holder.show_message.setText(message.getMessage());
                holder.show_message_time.setVisibility(View.VISIBLE);
                holder.show_message_time.setText(message.getTime());

                if (message.isSeen == 1) {
                    holder.seen_tick.setVisibility(View.VISIBLE);
                    Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/blue_tick.png?alt=media&token=5d37be6f-3bef-4b61-9ea2-4db80a949a08")
                            .into(holder.seen_tick);
                } else {
                    holder.seen_tick.setVisibility(View.VISIBLE);
                    Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/grey_tick.png?alt=media&token=bc3c2120-97a0-4fcb-8fad-12443265a4e1")
                            .into(holder.seen_tick);
                }

            } else {
                holder.seen_tick.setVisibility(View.GONE);
                holder.show_message.setVisibility(View.VISIBLE);
                holder.show_message.setText(message.getMessage());
                holder.show_message_time.setVisibility(View.VISIBLE);
                holder.show_message_time.setText(message.getTime());
            }
        } else if (fromMsgType.equals("image")) {

            if (message.getFrom().equals(fuser.getUid())) {

                holder.show_image.setVisibility(View.VISIBLE);
                holder.show_img_time.setVisibility(View.VISIBLE);
                holder.show_img_time.setText(message.getTime());
                Glide.with(context).load(message.getMessage())
                        .placeholder(R.drawable.placeholder)
                        .into(holder.show_image);

                        if (message.isSeen == 1)
                        {
                            holder.seen_img_tick.setVisibility(View.VISIBLE);
                            Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/blue_tick.png?alt=media&token=5d37be6f-3bef-4b61-9ea2-4db80a949a08")
                                    .into(holder.seen_img_tick);
                        }
                        else
                        {
                            holder.seen_img_tick.setVisibility(View.VISIBLE);
                            Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/grey_tick.png?alt=media&token=bc3c2120-97a0-4fcb-8fad-12443265a4e1")
                                    .into(holder.seen_img_tick);
                        }
            }
            else
            {
                holder.show_image.setVisibility(View.VISIBLE);
                holder.show_img_time.setVisibility(View.VISIBLE);
                holder.show_img_time.setText(message.getTime());
                Glide.with(context).load(message.getMessage())
                        .placeholder(R.drawable.placeholder)
                        .into(holder.show_image);
            }
        }
        else if (fromMsgType.equals("pdf") || fromMsgType.equals("docx"))
        {
            if (message.getFrom().equals(fuser.getUid()))
            {
                holder.show_document.setVisibility(View.VISIBLE);
                holder.show_docx_time.setVisibility(View.VISIBLE);
                holder.show_docx_time.setText(message.getTime());

                Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/docs_icon.png?alt=media&token=1331c975-0d36-4fcc-a1c4-47661257ac0e")
                        .into(holder.show_document);

                if (message.isSeen == 1) {
                    holder.seen_docx_tick.setVisibility(View.VISIBLE);
                    Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/blue_tick.png?alt=media&token=5d37be6f-3bef-4b61-9ea2-4db80a949a08")
                            .into(holder.seen_docx_tick);
                } else {
                    holder.seen_docx_tick.setVisibility(View.VISIBLE);
                    Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/grey_tick.png?alt=media&token=bc3c2120-97a0-4fcb-8fad-12443265a4e1")
                            .into(holder.seen_docx_tick);
                }

            }
            else
            {
                holder.show_document.setVisibility(View.VISIBLE);
                holder.show_docx_time.setVisibility(View.VISIBLE);
                holder.show_docx_time.setText(message.getTime());

                Glide.with(context).load("https://firebasestorage.googleapis.com/v0/b/hi--chat-da818.appspot.com/o/docs_icon.png?alt=media&token=1331c975-0d36-4fcc-a1c4-47661257ac0e")
                        .into(holder.show_document);

            }
        }


        if (message.getFrom().equals(fuser.getUid()))
        {
            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {

                    if (userMessagesList.get(position).getType().equals("pdf") || userMessagesList.get(position).getType().equals("docx"))
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for everyone",
                                        "Download and View This Document",
                                        "Cancel"

                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int i)
                            {
                                if (i == 0)
                                {
                                    deleteGraphicMessage(position, holder);
                                }
                                else  if (i == 1)
                                {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("text"))
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for everyone",
                                        "Cancel",

                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int i)
                            {
                                if (i == 0)
                                {
                                    deleteMessage(position,holder);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("image"))
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for everyone",
                                        "View This Image",
                                        "Cancel"

                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int i)
                            {
                                if (i == 0)
                                {
                                    deleteGraphicMessage(position,holder);

                                }
                                else  if (i == 1)
                                {
                                    Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url",userMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        }
        else
        {
            holder.itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if (userMessagesList.get(position).getType().equals("pdf") || userMessagesList.get(position).getType().equals("docx"))
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for everyone",
                                        "Download and View This Document",
                                        "Cancel"

                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if (i == 0)
                                {
                                    deleteReceiverMessage(position,holder);
                                }
                                else  if (i == 1)
                                {
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);

                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("text") )
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for everyone",
                                        "Cancel"

                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if (i == 0)
                                {
                                    deleteReceiverMessage(position,holder);
                                }

                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("image"))
                    {
                        CharSequence options[] = new CharSequence[]
                                {
                                        "Delete for everyone",
                                        "View This Image",
                                        "Cancel"

                                };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete Message?");

                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i) {
                                if (i == 0)
                                {
                                    deleteReceiverMessage(position,holder);
                                }
                                else  if (i == 1)
                                {
                                    Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
                                    intent.putExtra("url",userMessagesList.get(position).getMessage());
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        }
    }

    private void deleteGraphicMessage(int position, MessageViewHolder holder) {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("messages")
                .child(userMessagesList.get(position).getMessageID())
                .removeValue();
    }

    @Override
    public int getItemCount()
    {
        return userMessagesList.size();
    }

    private void deleteMessage(final int position, final MessageViewHolder holder)
    {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("messages")
                .child(userMessagesList.get(position).getMessageID())
                .child("message").setValue("This message was deleted!");

    }

    private void deleteReceiverMessage(int position, MessageViewHolder holder)
    {
        Toast.makeText(context, "You can delete only your messages!", Toast.LENGTH_LONG).show();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder
    {
        public TextView show_message,show_message_time,show_img_time,show_docx_time;
        public ImageView seen_tick,show_image,seen_img_tick,show_document,seen_docx_tick;


        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            show_message = itemView.findViewById(R.id.show_message);
            show_message_time = itemView.findViewById(R.id.show_msg_time);
            seen_tick = itemView.findViewById(R.id.seen_tick);
            show_image=itemView.findViewById(R.id.show_image);
            show_img_time=itemView.findViewById(R.id.show_img_time);
            seen_img_tick=itemView.findViewById(R.id.seen_tick_img);
            show_document=itemView.findViewById(R.id.show_document);
            show_docx_time=itemView.findViewById(R.id.show_docx_time);
            seen_docx_tick=itemView.findViewById(R.id.seen_tick_docx);
        }
    }

    //new
    @Override
    public int getItemViewType(int position) {
        fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (userMessagesList.get(position).getFrom().equals(fuser.getUid()))
        {
            return MSG_TYPE_RIGHT;
        }
        else
        {
            return MSG_TYPE_LEFT;
        }
    }
}