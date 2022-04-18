package com.example.hichat.View;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.hichat.R;

import java.util.Objects;

public class DialogViewUser {

    private Context context;
    private String finalRetrieveImage;

    public DialogViewUser(Context context, String finalRetrieveImage) {
        this.context = context;
        this.finalRetrieveImage=finalRetrieveImage;
        initialize();
    }

    private void initialize() {

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_view_user);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        ImageView display_profile;

        display_profile=dialog.findViewById(R.id.display_profile);
        Glide.with(context).load(finalRetrieveImage)
                .placeholder(R.drawable.avatar)
                .into(display_profile);

        dialog.show();
    }

}
