package com.example.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

//import com.example.hichat.Adapter.TopStatusAdapter;
import com.example.hichat.R;
import com.example.hichat.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.iammert.library.readablebottombar.ReadableBottomBar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseDatabase database;
    private String currentUserID;
    private DatabaseReference rootRef;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    //ArrayList<User> users;
    //UsersAdapter usersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        mAuth=FirebaseAuth.getInstance();

        database = FirebaseDatabase.getInstance();
        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String token) {
                        HashMap<String,Object> map = new HashMap<>();
                        map.put("token",token);
                        database.getReference()
                                .child("users")
                                .child(FirebaseAuth.getInstance().getUid())
                                .updateChildren(map);

                    }
                });

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content,new ChatFragment());
        transaction.commit();



        setSupportActionBar(binding.toolbar);


        binding.bottomNavigationView.setOnItemSelectListener(new ReadableBottomBar.ItemSelectListener() {
            @Override
            public void onItemSelected(int i) {
                FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
                switch (i){
                    case 0:
                        trans.replace(R.id.content,new ChatFragment());
                        break;
                    case 1:
                        trans.replace(R.id.content,new ContactsFragment());
                        break;
                    case 2:
                        trans.replace(R.id.content,new RequestsFragment());
                        break;
                }
                trans.commit();
            }
        });

    }


    @Override
    protected void onPause() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null){
            updateUserStatus("offline");
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUserStatus("online");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateUserStatus("online");
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
        {
            //FirebaseAuth.getInstance().signOut();
            sendUserToAuthenticationActivity();
        }
        else {
            updateUserStatus("online");
            verifyUserExistance();
        }

    }

    private void verifyUserExistance() {
        String currentUserID = mAuth.getCurrentUser().getUid();

        rootRef.child("users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if ((snapshot.child("name").exists()))
                {

                }
                else
                {
                    Intent intent = new Intent(MainActivity.this,SettingActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendUserToAuthenticationActivity() {
        Intent loginIntent = new Intent(MainActivity.this,Authentication.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        finish();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null){
            updateUserStatus("offline");
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item=menu.findItem(R.id.search);
        if(item!=null)
            item.setVisible(false);
        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case R.id.find_friend:
                Intent intent=new Intent(MainActivity.this,FindFriendsActivity.class);
                startActivity(intent);
                break;


            case R.id.search:
                Toast.makeText(this, "search clicked", Toast.LENGTH_SHORT).show();
                break;

            case R.id.settings:
                startActivity(new Intent(MainActivity.this,SettingActivity.class));
                break;

            case R.id.invite:
                Intent inviteintent=new Intent(Intent.ACTION_SEND);
                final String appPackagename=this.getPackageName();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                            .setTextColor(ContextCompat.getColor(MainActivity.this,R.color.green));
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                            .setTextColor(ContextCompat.getColor(MainActivity.this,R.color.black));

                });

                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void updateUserStatus(String state)
    {
        String saveCurrentTime, saveCurrentDate;

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime()) ;

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime()) ;

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time",saveCurrentTime);
        onlineStateMap.put("date",saveCurrentDate);
        onlineStateMap.put("state",state);

        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        rootRef.child("presence").child(currentUserID)
                .updateChildren(onlineStateMap);

    }
}