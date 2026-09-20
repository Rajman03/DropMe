package com.example.testapp.feature.profile;

import com.example.testapp.core.SupabaseSqlFix;
import com.example.testapp.data.AuthManager;
import com.example.testapp.core.AuthUser;
import com.example.testapp.core.PrivacyPolicyDialog;
import com.example.testapp.data.SupabaseClient;
import com.example.testapp.core.UserProfile;
import com.example.testapp.core.UserQrManager;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ProfileController {

    public interface ProfileActionCallback {
        void onProfileUpdated();
        void onLogoutRequested();
    }

    private final Activity activity;
    private final UserProfile userProfile;
    private final AuthManager authManager;
    private final ProfileActionCallback listener;

    private TextView tvHeaderAvatar;
    private TextView tvHeaderRankBadge;

    private TextView tvProfileSyncStatus;
    private Button btnProfileSyncCloud;
    private View ivProfileAvatarRing;
    private TextView tvProfileAvatarLarge;
    private TextView tvProfileDisplayName;
    private TextView tvProfileHandle;
    private TextView tvProfileRankBadge;

    private TextView tvStatQrCount;
    private TextView tvStatQrLimit;
    private TextView tvStatRank;

    private View cardUserManagement;
    private TextView tvManagementHeaderTitle;
    private TextView tvManagementDesc;
    private Button btnOpenUserManagement;

    private TextView tvAccountUsername;
    private TextView tvAccountCloudStatus;
    private Button btnLogOut;
    private Button btnProfilePrivacyPolicy;

    public ProfileController(Activity activity, UserProfile userProfile, AuthManager authManager, ProfileActionCallback listener) {
        this.activity = activity;
        this.userProfile = userProfile;
        this.authManager = authManager;
        this.listener = listener;

        bindViews();
        setupActions();
    }

    private void bindViews() {
        // tvHeaderAvatar / tvHeaderRankBadge należą do nagłówka w module :app
        // i są wstrzykiwane przez attachHeaderViews(...)

        tvProfileSyncStatus = activity.findViewById(R.id.tvProfileSyncStatus);
        btnProfileSyncCloud = activity.findViewById(R.id.btnProfileSyncCloud);
        ivProfileAvatarRing = activity.findViewById(R.id.ivProfileAvatarRing);
        tvProfileAvatarLarge = activity.findViewById(R.id.tvProfileAvatarLarge);
        tvProfileDisplayName = activity.findViewById(R.id.tvProfileDisplayName);
        tvProfileHandle = activity.findViewById(R.id.tvProfileHandle);
        tvProfileRankBadge = activity.findViewById(R.id.tvProfileRankBadge);

        tvStatQrCount = activity.findViewById(R.id.tvStatQrCount);
        tvStatQrLimit = activity.findViewById(R.id.tvStatQrLimit);
        tvStatRank = activity.findViewById(R.id.tvStatRank);

        cardUserManagement = activity.findViewById(R.id.cardUserManagement);
        tvManagementHeaderTitle = activity.findViewById(R.id.tvManagementHeaderTitle);
        tvManagementDesc = activity.findViewById(R.id.tvManagementDesc);
        btnOpenUserManagement = activity.findViewById(R.id.btnOpenUserManagement);

        tvAccountUsername = activity.findViewById(R.id.tvAccountUsername);
        tvAccountCloudStatus = activity.findViewById(R.id.tvAccountCloudStatus);
        btnLogOut = activity.findViewById(R.id.btnLogOut);
        btnProfilePrivacyPolicy = activity.findViewById(R.id.btnProfilePrivacyPolicy);
    }

    /**
     * Nagłówek aplikacji należy do modułu :app, więc widoki są wstrzykiwane
     * z MainActivity zamiast wyszukiwania po R.id.
     */
    public void attachHeaderViews(TextView avatar, TextView rankBadge) {
        this.tvHeaderAvatar = avatar;
        this.tvHeaderRankBadge = rankBadge;
    }

    private void setupActions() {
        if (btnProfileSyncCloud != null) {
            btnProfileSyncCloud.setOnClickListener(v -> {
                btnProfileSyncCloud.setEnabled(false);
                btnProfileSyncCloud.setText("Łączenie...");
                authManager.syncWithSupabase(userProfile, () -> {
                    btnProfileSyncCloud.setEnabled(true);
                    btnProfileSyncCloud.setText("Odśwież");
                    refreshViews();
                    if (listener != null) listener.onProfileUpdated();
                    if (SupabaseClient.isLastSyncSuccess()) {
                        Toast.makeText(activity, "✓ Pomyślnie zsynchronizowano profil z Supabase!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity, "Konto zapisane na urządzeniu. Dotknij informacji o chmurze, aby połączyć Supabase.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        if (btnOpenUserManagement != null) {
            btnOpenUserManagement.setOnClickListener(v -> {
                UserManagementDialog.show(activity, authManager, userProfile, () -> {
                    refreshViews();
                    if (listener != null) listener.onProfileUpdated();
                });
            });
        }

        if (btnLogOut != null) {
            btnLogOut.setOnClickListener(v -> showLogoutDialog());
        }

        if (btnProfilePrivacyPolicy != null) {
            btnProfilePrivacyPolicy.setOnClickListener(v -> PrivacyPolicyDialog.show(activity));
        }

        if (tvAccountCloudStatus != null) {
            tvAccountCloudStatus.setOnClickListener(v -> showSupabaseSqlDialog());
        }

        if (tvProfileSyncStatus != null) {
            tvProfileSyncStatus.setOnClickListener(v -> showSupabaseSqlDialog());
        }
    }

    private void showSupabaseSqlDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("🛠️ Synchronizacja z Supabase")
                .setMessage("Twoje konto działa poprawnie i jest zapisane na Twoim telefonie.\n\nAby włączyć kopię zapasową w chmurze Supabase:\n\n1. Wejdź na panel Supabase -> SQL Editor\n2. Kliknij poniższy przycisk, aby skopiować gotowy kod SQL\n3. Wklej kod i kliknij zielony przycisk 'Run'")
                .setPositiveButton("📋 KOPIUJ KOD SQL", (dialog, which) -> {
                    String sql = SupabaseSqlFix.SQL;

                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Supabase Fix SQL", sql);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(activity, "✓ Skopiowano kod SQL do schowka! Wklej go w Supabase SQL Editor.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("ZAMKNIJ", null)
                .show();
    }

    private void showLogoutDialog() {
        Dialog dialog = new Dialog(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_logout, null, false);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmLogout);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelLogout);

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) listener.onLogoutRequested();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public void refreshViews() {
        String displayName;
        String username;
        boolean loggedIn = (authManager != null && authManager.getCurrentUser() != null);

        if (loggedIn) {
            AuthUser cur = authManager.getCurrentUser();
            username = cur.getUsername();
            String dn = cur.getDisplayName();
            if (dn != null && !dn.trim().isEmpty() && !"Użytkownik".equals(dn)) {
                displayName = dn;
            } else {
                displayName = username;
            }
        } else {
            displayName = "Użytkownik (Gość)";
            username = "";
        }

        // Clean user avatar
        final String defaultAvatar = "👤";
        if (tvHeaderAvatar != null) tvHeaderAvatar.setText(defaultAvatar);
        if (tvProfileAvatarLarge != null) tvProfileAvatarLarge.setText(defaultAvatar);
        if (tvProfileDisplayName != null) tvProfileDisplayName.setText(displayName);

        if (tvProfileHandle != null) {
            if (loggedIn) {
                tvProfileHandle.setText("@" + username + " • Baza Supabase");
            } else {
                tvProfileHandle.setText("Konto gościa (lokalne) • Zaloguj się");
            }
        }

        if (tvAccountUsername != null) {
            if (loggedIn) {
                tvAccountUsername.setText("@" + username);
            } else {
                tvAccountUsername.setText("Nie zalogowano (dotknij wyloguj/zaloguj)");
            }
        }

        if (tvAccountCloudStatus != null) {
            if (SupabaseClient.isLastSyncSuccess()) {
                tvAccountCloudStatus.setText("Aktywna ✓");
                tvAccountCloudStatus.setTextColor(Color.parseColor("#059669"));
            } else {
                tvAccountCloudStatus.setText("Lokalna (dotknij aby połączyć) ℹ️");
                tvAccountCloudStatus.setTextColor(Color.parseColor("#3B82F6"));
            }
        }

        if (tvProfileSyncStatus != null) {
            if (SupabaseClient.isLastSyncSuccess()) {
                tvProfileSyncStatus.setText("Zsynchronizowano z chmurą ✓");
                tvProfileSyncStatus.setTextColor(Color.parseColor("#059669"));
            } else {
                tvProfileSyncStatus.setText("Zapisano na urządzeniu ✓");
                tvProfileSyncStatus.setTextColor(Color.parseColor("#3B82F6"));
            }
        }

        // Clean badge and ring
        int badgeRes = R.drawable.badge_status_ok;
        int ringRes = R.drawable.avatar_ring_guest;
        String badgeText = "Użytkownik";

        if (userProfile.isCreator()) {
            badgeRes = R.drawable.badge_owner;
            ringRes = R.drawable.avatar_ring_creator;
            badgeText = "Twórca";
        } else if (userProfile.isAdmin()) {
            badgeRes = R.drawable.badge_admin;
            ringRes = R.drawable.avatar_ring_admin;
            badgeText = "Admin";
        }

        if (ivProfileAvatarRing != null) {
            ivProfileAvatarRing.setBackgroundResource(ringRes);
        }

        if (tvHeaderRankBadge != null) {
            tvHeaderRankBadge.setBackgroundResource(badgeRes);
            tvHeaderRankBadge.setText(badgeText);
            tvHeaderRankBadge.setTextColor(Color.WHITE);
        }

        if (tvProfileRankBadge != null) {
            tvProfileRankBadge.setBackgroundResource(badgeRes);
            tvProfileRankBadge.setText(badgeText);
            tvProfileRankBadge.setTextColor(Color.WHITE);
        }

        // Stats Bar - Unlimited QR codes
        UserQrManager qrManager = new UserQrManager(activity);
        int qrCount = qrManager.getCount();
        if (tvStatQrCount != null) {
            tvStatQrCount.setText(String.valueOf(qrCount));
        }
        if (tvStatQrLimit != null) {
            tvStatQrLimit.setText("Bez limitu");
            tvStatQrLimit.setTextColor(Color.parseColor("#059669"));
        }
        if (tvStatRank != null) {
            tvStatRank.setText("Aktywne");
        }

        // Hide technical backend panel from ordinary users completely
        boolean isPrivileged = userProfile.isAdmin() || (authManager != null && authManager.getCurrentUser() != null && authManager.getCurrentUser().isAdmin());
        if (cardUserManagement != null) {
            cardUserManagement.setVisibility(isPrivileged ? View.VISIBLE : View.GONE);
            if (isPrivileged) {
                if (tvManagementHeaderTitle != null) tvManagementHeaderTitle.setText("Zarządzanie Użytkownikami");
                if (tvManagementDesc != null) tvManagementDesc.setText("Podgląd i zarządzanie kontami w aplikacji.");
                if (btnOpenUserManagement != null) btnOpenUserManagement.setText("Otwórz Panel");
            }
        }
    }
}
