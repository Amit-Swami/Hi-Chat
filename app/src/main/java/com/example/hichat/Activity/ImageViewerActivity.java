package com.example.hichat.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.hichat.Adapter.MessagesAdapter;
import com.example.hichat.R;
import com.example.hichat.databinding.ActivityImageViewerBinding;

public class ImageViewerActivity extends AppCompatActivity {

    private String imageUrl;

    ActivityImageViewerBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageUrl = getIntent().getStringExtra("url");

        Glide.with(ImageViewerActivity.this)
                .load(imageUrl)
                .into(binding.imageViewer);
    }

}