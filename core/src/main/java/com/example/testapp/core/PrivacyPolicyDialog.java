package com.example.testapp.core;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

public class PrivacyPolicyDialog {

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_privacy_policy, null);
        dialog.setContentView(view);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        View btnCloseTop = view.findViewById(R.id.btnPrivacyCloseTop);
        if (btnCloseTop != null) {
            btnCloseTop.setOnClickListener(v -> dialog.dismiss());
        }

        Button btnAccept = view.findViewById(R.id.btnPrivacyAccept);
        if (btnAccept != null) {
            btnAccept.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
