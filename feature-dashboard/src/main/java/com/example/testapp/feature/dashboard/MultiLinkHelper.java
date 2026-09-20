package com.example.testapp.feature.dashboard;

import com.example.testapp.core.MultiLinkProfile;
import com.example.testapp.core.QRCodeHelper;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MultiLinkHelper {

    public static void showViewDialog(Activity activity, MultiLinkProfile profile) {
        if (activity == null || profile == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_multilink_view, null, false);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        ImageView ivQr = view.findViewById(R.id.ivMultiLinkQr);
        ProgressBar pbLoading = view.findViewById(R.id.pbMultiLinkLoading);
        TextView tvName = view.findViewById(R.id.tvMlViewName);
        TextView tvBio = view.findViewById(R.id.tvMlViewBio);
        LinearLayout containerBadges = view.findViewById(R.id.containerMlBadges);
        Button btnOpenWeb = view.findViewById(R.id.btnOpenWebCard);
        Button btnShare = view.findViewById(R.id.btnShareMultiLink);
        Button btnClose = view.findViewById(R.id.btnCloseMultiLinkView);

        String dispName = profile.getName().isEmpty() ? "Moja Wizytówka" : profile.getName();
        if (tvName != null) tvName.setText(dispName);
        if (tvBio != null) tvBio.setText(profile.getBio());

        // Fill badge icons
        if (containerBadges != null) {
            containerBadges.removeAllViews();
            addBadgeIfPresent(activity, containerBadges, R.drawable.ic_instagram, profile.getInstagram());
            addBadgeIfPresent(activity, containerBadges, R.drawable.ic_tiktok, profile.getTiktok());
            addBadgeIfPresent(activity, containerBadges, R.drawable.ic_snapchat, profile.getSnapchat());
            addBadgeIfPresent(activity, containerBadges, R.drawable.ic_discord, profile.getDiscord());
            addBadgeIfPresent(activity, containerBadges, R.drawable.ic_twitter_x, profile.getTwitter());
            addBadgeIfPresent(activity, containerBadges, R.drawable.ic_globe, profile.getWebsite());
        }

        // Determine what payload the QR code should carry
        String lastWebUrl = profile.getLastWebUrl(activity);
        String qrPayload = (lastWebUrl != null && !lastWebUrl.isEmpty()) ? lastWebUrl : profile.buildVCard();

        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        QRCodeHelper.generateQrCode(activity, qrPayload, profile.getThemeColor(), bitmap -> {
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            if (ivQr != null && bitmap != null) {
                ivQr.setImageBitmap(bitmap);
            }
        });

        // Always publish fresh styled card so friends and user get the updated design
        profile.publishWebCard(activity, new MultiLinkProfile.WebCardCallback() {
            @Override
            public void onSuccess(String publicWebUrl) {
                activity.runOnUiThread(() -> {
                    QRCodeHelper.generateQrCode(activity, publicWebUrl, profile.getThemeColor(), bitmap -> {
                        if (ivQr != null && bitmap != null) ivQr.setImageBitmap(bitmap);
                    });
                });
            }

            @Override
            public void onError(String error) {
                // Fallback to vCard already loaded
            }
        });

        if (btnOpenWeb != null) {
            btnOpenWeb.setOnClickListener(v -> {
                String url = profile.getLastWebUrl(activity);
                if (url != null && !url.isEmpty()) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(activity, "Nie można otworzyć przeglądarki", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(activity, "Trwa publikowanie strony wizytówki...", Toast.LENGTH_SHORT).show();
                    profile.publishWebCard(activity, new MultiLinkProfile.WebCardCallback() {
                        @Override
                        public void onSuccess(String publicWebUrl) {
                            activity.runOnUiThread(() -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(publicWebUrl));
                                activity.startActivity(intent);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            activity.runOnUiThread(() -> Toast.makeText(activity, "Błąd otwierania strony: " + error, Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            });
        }

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> {
                String shareText = profile.getLastWebUrl(activity);
                if (shareText == null || shareText.isEmpty()) {
                    shareText = profile.buildVCard();
                }
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(ClipData.newPlainText("Wizytówka", shareText));
                    Toast.makeText(activity, "Skopiowano link do schowka! 📋", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public static void showEditDialog(Activity activity, MultiLinkProfile profile, Runnable onUpdated) {
        if (activity == null || profile == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_multilink_edit, null, false);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText etName = view.findViewById(R.id.etMlName);
        EditText etBio = view.findViewById(R.id.etMlBio);
        EditText etIg = view.findViewById(R.id.etMlInstagram);
        EditText etTt = view.findViewById(R.id.etMlTiktok);
        EditText etSnap = view.findViewById(R.id.etMlSnapchat);
        EditText etDc = view.findViewById(R.id.etMlDiscord);
        EditText etTw = view.findViewById(R.id.etMlTwitter);
        EditText etWeb = view.findViewById(R.id.etMlWebsite);

        Button btnSave = view.findViewById(R.id.btnSaveMultiLink);
        Button btnCancel = view.findViewById(R.id.btnCancelMultiLink);

        if (etName != null) etName.setText(profile.getName());
        if (etBio != null) etBio.setText(profile.getBio());
        if (etIg != null) etIg.setText(profile.getInstagram());
        if (etTt != null) etTt.setText(profile.getTiktok());
        if (etSnap != null) etSnap.setText(profile.getSnapchat());
        if (etDc != null) etDc.setText(profile.getDiscord());
        if (etTw != null) etTw.setText(profile.getTwitter());
        if (etWeb != null) etWeb.setText(profile.getWebsite());

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                profile.setName(etName != null ? etName.getText().toString().trim() : "");
                profile.setBio(etBio != null ? etBio.getText().toString().trim() : "");
                profile.setInstagram(etIg != null ? etIg.getText().toString().trim() : "");
                profile.setTiktok(etTt != null ? etTt.getText().toString().trim() : "");
                profile.setSnapchat(etSnap != null ? etSnap.getText().toString().trim() : "");
                profile.setDiscord(etDc != null ? etDc.getText().toString().trim() : "");
                profile.setTwitter(etTw != null ? etTw.getText().toString().trim() : "");
                profile.setWebsite(etWeb != null ? etWeb.getText().toString().trim() : "");

                profile.save(activity);
                profile.publishWebCard(activity, null);

                Toast.makeText(activity, "Zaktualizowano Wizytówkę Multi-Link! ✨", Toast.LENGTH_SHORT).show();
                if (onUpdated != null) onUpdated.run();
                dialog.dismiss();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private static void addBadgeIfPresent(Context context, LinearLayout container, int iconRes, String value) {
        if (value == null || value.trim().isEmpty() || container == null) return;
        ImageView iv = new ImageView(context);
        int size = (int) (24 * context.getResources().getDisplayMetrics().density);
        int margin = (int) (4 * context.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(margin, 0, margin, 0);
        iv.setLayoutParams(lp);
        iv.setImageResource(iconRes);
        container.addView(iv);
    }
}
