package com.socialmedia.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import com.socialmedia.hichat.R;
import com.socialmedia.hichat.databinding.ActivitySettingBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

public class SettingActivity extends AppCompatActivity {

    ActivitySettingBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog;

    private static final int GALLERY_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating profile...");
        dialog.setCancelable(false);

        binding.nameBox.requestFocus();

        database = FirebaseDatabase.getInstance();
        storage=FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();
        updateStatus("online");

        RetrieveUserInfo();

        binding.settingsimageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,GALLERY_PICK);

            }
        });

        binding.setupProfilebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.nameBox.getText().toString();
                String status = binding.statusBox.getText().toString();

                if (name.isEmpty()){
                    binding.nameBox.setError("Please type a name");
                    return;
                }

                if (status.isEmpty()){
                    binding.statusBox.setError("Please write a status");
                    return;
                }

                dialog.show();

                if (selectedImage != null){
                    StorageReference reference = storage.getReference().child("Profiles").child(auth.getUid());
                    reference.putFile(selectedImage).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if (task.isSuccessful()){
                                reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        HashMap<String,Object> profileMap = new HashMap<>();
                                        profileMap.put("profileImage",uri.toString());
                                        profileMap.put("uid",auth.getUid());
                                        profileMap.put("name", binding.nameBox.getText().toString());
                                        profileMap.put("status",binding.statusBox.getText().toString());

                                        database.getReference()
                                                .child("users")
                                                .child(auth.getUid())
                                                .updateChildren(profileMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        dialog.dismiss();
                                                        Toast.makeText(SettingActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                                                        Intent intent = new Intent(SettingActivity.this,MainActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                    }
                                });
                            }
                        }
                    });
                }else {
                    String uid = auth.getUid();
                    //String phone=auth.getCurrentUser().getPhoneNumber();

                    HashMap<String,Object> elseProfileMap = new HashMap<>();
                    elseProfileMap.put("uid",uid);
                    elseProfileMap.put("name", binding.nameBox.getText().toString());
                    elseProfileMap.put("status",binding.statusBox.getText().toString());
                   // User user = new User(uid, name, status,phone, "No Image");

                    database.getReference()
                            .child("users")
                            .child(uid)
                            .updateChildren(elseProfileMap)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dialog.dismiss();
                                    Toast.makeText(SettingActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(SettingActivity.this,MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }
            }
        });

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

    private void RetrieveUserInfo() {
        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if ((snapshot.exists()) && (snapshot.hasChild("name")) && (snapshot.hasChild("status")) && (snapshot.hasChild("profileImage")) )
                        {
                            String retrieveName = snapshot.child("name").getValue().toString();
                            String retrieveStatus = snapshot.child("status").getValue().toString();
                            String retrieveProfileImage = snapshot.child("profileImage").getValue().toString();

                            binding.nameBox.setText(retrieveName);
                            binding.statusBox.setText(retrieveStatus);
                            Glide.with(getApplicationContext())
                                    .load(retrieveProfileImage)
                                    .placeholder(R.drawable.avatar)
                                    .into(binding.settingsimageView);
                        }
                        else if ((snapshot.exists()) && (snapshot.hasChild("name")) && (snapshot.hasChild("status")))
                        {
                            String retrieveName = snapshot.child("name").getValue().toString();
                            String retrieveStatus = snapshot.child("status").getValue().toString();
                            binding.nameBox.setText(retrieveName);
                            binding.statusBox.setText(retrieveStatus);
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

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null){
            selectedImage = data.getData();

                CropImage.activity(selectedImage)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1,1)
                        .start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {
                Glide.with(getApplicationContext())
                .load(result.getUri()).into(binding.settingsimageView);
                selectedImage = result.getUri();
            }
        }
    }

    private void updateStatus(String state)
    {
        HashMap<String, Object> stateMap = new HashMap<>();
        stateMap.put("state",state);
        database.getReference().child("presence").child(auth.getCurrentUser().getUid())
                .updateChildren(stateMap);
    }
}