package com.socialmedia.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import de.hdodenhof.circleimageview.CircleImageView;
import jp.shts.android.storiesprogressview.StoriesProgressView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.socialmedia.hichat.Model.Story;
import com.socialmedia.hichat.Model.User;

import com.socialmedia.hichat.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class StoryActivity extends AppCompatActivity implements StoriesProgressView.StoriesListener {

    int counter = 0;
    long pressTime =0L;
    long limit = 500L;

    StoriesProgressView storiesProgressView;
    ImageView image, story_photo;
    TextView story_username;

    LinearLayout r_seen;
    TextView seen_number;
    ImageView story_delete;

    List<String> images;
    List<String>  storyids;
    String userid;

    RecyclerView viewrecyclerView;
    DatabaseReference viewRef,userRef;
    private String saveCurrentTime;
    private Query query;

    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()){
                case MotionEvent.ACTION_DOWN:
                    pressTime = System.currentTimeMillis();
                    storiesProgressView.pause();
                    return false;
                case MotionEvent.ACTION_UP:
                    long now = System.currentTimeMillis();
                    storiesProgressView.resume();
                    return limit < now - pressTime;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        updateStatus("online");

        r_seen = findViewById(R.id.r_seen);
        seen_number = findViewById(R.id.seen_number);
        story_delete = findViewById(R.id.story_delete);

        storiesProgressView = findViewById(R.id.stories);
        image = findViewById(R.id.image);
        story_photo = findViewById(R.id.story_photo);
        story_username = findViewById(R.id.story_username);

        r_seen.setVisibility(View.GONE);
        story_delete.setVisibility(View.GONE);

        userid = getIntent().getStringExtra("userid");
        userRef = FirebaseDatabase.getInstance().getReference().child("users");

        if (userid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
            r_seen.setVisibility(View.VISIBLE);
            story_delete.setVisibility(View.VISIBLE);
        }

        getStories(userid);
        userInfo(userid);

        View reverse = findViewById(R.id.reverse);
        reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.reverse();
            }
        });
        reverse.setOnTouchListener(onTouchListener);

        View skip = findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.skip();
            }
        });
        skip.setOnTouchListener(onTouchListener);

        r_seen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storiesProgressView.pause();
                BottomSheetDialog sheetDialog = new BottomSheetDialog(StoryActivity.this,R.style.BottomSheetDialogTheme);
                View sheetView = LayoutInflater.from(StoryActivity.this)
                        .inflate(R.layout.views_bottom_dialog,(LinearLayout)findViewById(R.id.dialog_container));
                sheetView.findViewById(R.id.views_cancel).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sheetDialog.dismiss();
                        storiesProgressView.resume();
                    }
                });
                viewrecyclerView = sheetView.findViewById(R.id.views_recyclerlist);
                viewrecyclerView.setLayoutManager(new LinearLayoutManager(StoryActivity.this));
                sheetDialog.setContentView(sheetView);
                sheetDialog.setCanceledOnTouchOutside(true);
                sheetDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        storiesProgressView.resume();
                    }
                });
                sheetDialog.show();
                loadViewList(storyids.get(counter));
            }
        });

        story_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("story")
                        .child(userid).child(storyids.get(counter));
                reference.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(StoryActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void loadViewList(String storyid) {

        viewRef = FirebaseDatabase.getInstance().getReference();
        query = viewRef.child("story").child(userid)
                .child(storyid).child("views").orderByChild("timeStamp");

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<User>()
                        .setQuery(query,User.class)
                        .build();

        FirebaseRecyclerAdapter<User,ViewsViewHolder> adapter = new FirebaseRecyclerAdapter<User, ViewsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewsViewHolder holder, int position, @NonNull User model) {

                String userIDs = getRef(position).getKey();

                FirebaseDatabase.getInstance().getReference("users")
                        .child(userIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        Glide.with(getApplicationContext())
                                .load(user.getProfileImage())
                                .placeholder(R.drawable.avatar)
                                .into(holder.viewProfileImage);
                        holder.viewUserName.setText(user.getName());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

                FirebaseDatabase.getInstance().getReference("story")
                        .child(userid).child(storyid).child("views").child(userIDs).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists())
                        {
                                String views_time = snapshot.child("time").getValue().toString();
                                holder.viewTime.setText(views_time);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }

            @NonNull
            @Override
            public ViewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.views_item,parent,false);
                return new ViewsViewHolder(view);
            }
        };

        viewrecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ViewsViewHolder extends  RecyclerView.ViewHolder {

        TextView viewUserName,viewTime;
        CircleImageView viewProfileImage;

        public ViewsViewHolder(@NonNull View itemView) {
            super(itemView);

            viewUserName = itemView.findViewById(R.id.views_username);
            viewProfileImage = itemView.findViewById(R.id.views_profileimg);
            viewTime=itemView.findViewById(R.id.views_Time);
        }
    }

    @Override
    public void onNext() {
        Glide.with(getApplicationContext())
                .load(images.get(++counter))
                .into(image);
        addView(storyids.get(counter));
        seenNumber(storyids.get(counter));
    }

    @Override
    public void onPrev() {

        if ((counter - 1) < 0) return;
        Glide.with(getApplicationContext()).load(images.get(--counter)).into(image);
        seenNumber(storyids.get(counter));
    }

    @Override
    public void onComplete() {
        finish();
    }

    @Override
    protected void onDestroy() {
        storiesProgressView.destroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        storiesProgressView.pause();
        updateStatus("offline");
        super.onPause();
    }

    @Override
    protected void onResume() {
        storiesProgressView.resume();
        updateStatus("online");
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateStatus("online");
    }

    private void getStories(String userid){
        images = new ArrayList<>();
        storyids = new ArrayList<>();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("story")
                .child(userid);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                images.clear();
                storyids.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()){
                    Story story = snapshot1.getValue(Story.class);
                    long timecurrent = System.currentTimeMillis();
                    if (timecurrent > story.getTimestart() && timecurrent < story.getTimeend()){
                        images.add(story.getImageurl());
                        storyids.add(story.getStoryid());
                    }
                }

                storiesProgressView.setStoriesCount(images.size());
                storiesProgressView.setStoryDuration(5000L);
                storiesProgressView.setStoriesListener(StoryActivity.this);
                storiesProgressView.startStories(counter);

                Glide.with(getApplicationContext())
                        .load(images.get(counter))
                        .into(image);

                addView(storyids.get(counter));
                seenNumber(storyids.get(counter));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void userInfo(String userid){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("users")
                .child(userid);
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                Glide.with(getApplicationContext())
                        .load(user.getProfileImage())
                        .placeholder(R.drawable.avatar)
                        .into(story_photo);
                story_username.setText(user.getName());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void addView(String storyid){
       if (!userid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
        {
            Date date = new Date();

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
            saveCurrentTime = currentTime.format(calendar.getTime());

            HashMap<String,Object> map = new HashMap<>();
            map.put("value","true");
            map.put("time",saveCurrentTime);
            map.put("timeStamp",-1*date.getTime());

            FirebaseDatabase.getInstance().getReference("story").child(userid)
                    .child(storyid).child("views").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(map);
        }
    }

    private void seenNumber(String storyid){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("story")
                .child(userid).child(storyid).child("views");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                seen_number.setText(""+snapshot.getChildrenCount());
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
        FirebaseDatabase.getInstance().getReference().child("presence").child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .updateChildren(stateMap);
    }

}