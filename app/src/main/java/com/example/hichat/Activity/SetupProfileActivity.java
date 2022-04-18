package com.example.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.hichat.Model.User;
import com.example.hichat.R;
import com.example.hichat.databinding.ActivitySetupProfileBinding;
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

public class SetupProfileActivity extends AppCompatActivity {

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog, restoringdatadialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating profile...");
        dialog.setCancelable(false);

        restoringdatadialog = new ProgressDialog(this);
        restoringdatadialog.setMessage("Restoring data...");
        restoringdatadialog.setCancelable(false);
        restoringdatadialog.show();

        binding.nameBox.requestFocus();

        database = FirebaseDatabase.getInstance();
        storage=FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();



        if (auth.getCurrentUser() != null)
        {

            database.getReference().child("users").child(auth.getCurrentUser().getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
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
                                        .into(binding.imageView);

                                restoringdatadialog.dismiss();
                                Intent intent = new Intent(SetupProfileActivity.this,MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                            else if ((snapshot.exists()) && (snapshot.hasChild("name")) && (snapshot.hasChild("status")))
                            {
                                String retrieveName = snapshot.child("name").getValue().toString();
                                String retrieveStatus = snapshot.child("status").getValue().toString();
                                binding.nameBox.setText(retrieveName);
                                binding.statusBox.setText(retrieveStatus);

                                restoringdatadialog.dismiss();
                                Intent intent = new Intent(SetupProfileActivity.this,MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                            else
                            {
                                restoringdatadialog.dismiss();
                                Toast.makeText(SetupProfileActivity.this, "No Data Found!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

        }

        binding.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,45);

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
                                        String imageUrl = uri.toString();
                                        String uid = auth.getUid();
                                        String phone=auth.getCurrentUser().getPhoneNumber();
                                        String name= binding.nameBox.getText().toString();
                                        String status = binding.statusBox.getText().toString();

                                        User user = new User(uid, name,status,phone, imageUrl);

                                        database.getReference()
                                                .child("users")
                                                .child(uid)
                                                .setValue(user)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        dialog.dismiss();
                                                        Intent intent = new Intent(SetupProfileActivity.this,MainActivity.class);
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
                    String phone=auth.getCurrentUser().getPhoneNumber();

                    User user = new User(uid, name,status,phone, "No Image");

                    database.getReference()
                            .child("users")
                            .child(uid)
                            .setValue(user)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    dialog.dismiss();
                                    Intent intent = new Intent(SetupProfileActivity.this,MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null){
            if (data.getData() != null){
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1,1)
                        .start(this);
                /*binding.imageView.setImageURI(data.getData());
                selectedImage = data.getData();*/
            }
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK)
            {

                binding.imageView.setImageURI(result.getUri());
                selectedImage = result.getUri();
            }
        }
    }


}