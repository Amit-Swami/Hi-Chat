package com.example.hichat.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.hichat.R;
import com.example.hichat.databinding.ActivityAuthenticationBinding;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Authentication extends AppCompatActivity {

    ActivityAuthenticationBinding binding;
    FirebaseAuth auth;
    String enteredPhoneNumber,phoneNo;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthenticationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();



       if (auth.getCurrentUser() != null)
        {
            Intent intent= new Intent(Authentication.this,MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
            startActivity(intent);
            finish();
        }

        binding.phoneBox.requestFocus();

        binding.continueBtn.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                enteredPhoneNumber = binding.phoneBox.getText().toString().trim();
                binding.countrycode.registerCarrierNumberEditText(binding.phoneBox);

                phoneNo = binding.countrycode.getSelectedCountryCodeWithPlus()+enteredPhoneNumber;

                if (TextUtils.isEmpty(enteredPhoneNumber))
                {
                    binding.phoneBox.setError("Enter Phone Number");
                }
                else {
                    otpSend();
                }
            }
        });
    }

    private void otpSend() {

        dialog = new ProgressDialog(this);
        dialog.setMessage("Sending OTP...");
        dialog.setCancelable(false);
        dialog.show();

        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                dialog.dismiss();
                Toast.makeText(Authentication.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {

                dialog.dismiss();
                Toast.makeText(Authentication.this, "OTP send successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Authentication.this,OTPActivity.class);
                intent.putExtra("phoneNumber",phoneNo);
                intent.putExtra("verificationId", verificationId);
                startActivity(intent);

            }
        };

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNo)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }


}