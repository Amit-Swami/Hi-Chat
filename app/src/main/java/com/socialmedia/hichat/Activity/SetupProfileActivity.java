package com.socialmedia.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.socialmedia.hichat.Model.User;

import com.socialmedia.hichat.R;

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
import com.socialmedia.hichat.databinding.ActivitySetupProfileBinding;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class SetupProfileActivity extends AppCompatActivity {

    ActivitySetupProfileBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri selectedImage;
    ProgressDialog dialog, restoringdatadialog;
    AlertDialog mDialog;
    private static final int GALLERY_PICK = 1;
    private int count=0;
    CheckBox checkBox1,checkBox2,checkBox3,checkBox4,checkBox5,checkBoxEnd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance();
        storage=FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        dialog = new ProgressDialog(this);
        dialog.setMessage("Updating profile...");
        dialog.setCancelable(false);

        ugcAlertDialog();


        binding.nameBox.requestFocus();



        binding.imageView.setOnClickListener(new View.OnClickListener() {
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

    private void ugcAlertDialog() {

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(SetupProfileActivity.this);
        View mView = getLayoutInflater().inflate(R.layout.ugc_dialog, null);
        checkBox1=mView.findViewById(R.id.checkBox1);
        checkBox2=mView.findViewById(R.id.checkBox2);
        checkBox3=mView.findViewById(R.id.checkBox3);
        checkBox4=mView.findViewById(R.id.checkBox4);
        checkBox5=mView.findViewById(R.id.checkBox5);
        checkBoxEnd = mView.findViewById(R.id.checkBoxEnd);
        mBuilder.setTitle("UGC Policy");
        //mBuilder.setMessage("User-Generated Content Policy (UGC).If you are going to use this app then accept these policies.");
        mBuilder.setIcon(R.drawable.ic_baseline_policy_24);

        checkBox1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox1.isChecked())
                {
                    count++;
                }
                else
                {
                    count--;
                }
                // do logic
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count >= 5);
            }
        });
        checkBox2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox2.isChecked())
                {
                    count++;
                }
                else
                {
                    count--;
                }
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count >= 5);
            }
        });
        checkBox3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox3.isChecked())
                {
                    count++;
                }
                else
                {
                    count--;
                }
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count >= 5);
            }
        });
        checkBox4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox4.isChecked())
                {
                    count++;
                }
                else
                {
                    count--;
                }
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count >= 5);
            }
        });
        checkBox5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox5.isChecked())
                {
                    count++;
                }
                else
                {
                    count--;
                }
                mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(count >= 5);
            }
        });



        mBuilder.setView(mView);
        mBuilder.setCancelable(false);
        mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                restoringdatadialog = new ProgressDialog(SetupProfileActivity.this);
                restoringdatadialog.setMessage("Restoring data...");
                restoringdatadialog.setCancelable(false);
                restoringdatadialog.show();

                checkForRestoreData();
            }
        });

        mDialog = mBuilder.create();
        mDialog.show();
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        checkBoxEnd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(compoundButton.isChecked()){
                    storeDialogStatus(true);
                }else{
                    storeDialogStatus(false);
                }
            }
        });

        if(getDialogStatus()){
            mDialog.hide();
            restoringdatadialog = new ProgressDialog(SetupProfileActivity.this);
            restoringdatadialog.setMessage("Restoring data...");
            restoringdatadialog.setCancelable(false);
            restoringdatadialog.show();

            checkForRestoreData();
        }else{
            mDialog.show();
        }
    }

    private void storeDialogStatus(boolean isChecked){
        SharedPreferences mSharedPreferences = getSharedPreferences("CheckItem", MODE_PRIVATE);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putBoolean("item", isChecked);
        mEditor.apply();
    }

    private boolean getDialogStatus(){
        SharedPreferences mSharedPreferences = getSharedPreferences("CheckItem", MODE_PRIVATE);
        return mSharedPreferences.getBoolean("item", false);
    }

    private void checkForRestoreData() {

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
                binding.imageView.setImageURI(result.getUri());
                selectedImage = result.getUri();
            }
        }
    }


}