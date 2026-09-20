package com.example.testapp.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class QrDetailsDialog {

    public static void show(Activity activity, UserQrItem item) {
        if (activity == null || activity.isFinishing() || item == null) {
            return;
        }

        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_qr, null);

        ImageView ivPlatform = dialogView.findViewById(R.id.ivDialogPlatformIcon);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogPlatformTitle);
        TextView tvUrl = dialogView.findViewById(R.id.tvDialogUrl);
        ImageView ivQr = dialogView.findViewById(R.id.ivQrCodeImage);
        ProgressBar pbLoading = dialogView.findViewById(R.id.pbQrLoading);
        Button btnOpen = dialogView.findViewById(R.id.btnDialogOpenLink);
        Button btnCopy = dialogView.findViewById(R.id.btnDialogCopyLink);
        View btnClose = dialogView.findViewById(R.id.btnDialogClose);

        SocialPlatform platform = SocialPlatform.fromKey(item.getIcon());
        if (ivPlatform != null) {
            ivPlatform.setImageResource(platform.getIconRes());
        }
        if (tvTitle != null) {
            tvTitle.setText(item.getName());
        }
        if (tvUrl != null) {
            tvUrl.setText(item.getUrl());
        }
        if (pbLoading != null) {
            pbLoading.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        QRCodeHelper.generateQrCode(item.getUrl(), item.getQrColor(), bitmap -> {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            if (ivQr != null && bitmap != null) {
                ivQr.setImageBitmap(bitmap);
            }
        });

        if (btnOpen != null) {
            btnOpen.setOnClickListener(v -> openUrl(activity, item.getUrl()));
        }

        if (btnCopy != null) {
            btnCopy.setOnClickListener(v -> copyToClipboard(activity, item.getUrl()));
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private static void openUrl(Context context, String rawUrl) {
        try {
            String url = rawUrl != null ? rawUrl.trim() : "";
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Nie można otworzyć linku", Toast.LENGTH_SHORT).show();
        }
    }

    private static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Link", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Link skopiowany do schowka!", Toast.LENGTH_SHORT).show();
        }
    }
}
