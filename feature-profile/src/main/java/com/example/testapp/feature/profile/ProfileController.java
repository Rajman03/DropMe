package com.example.testapp.feature.profile;

import com.example.testapp.core.SupabaseSqlFix;
import com.example.testapp.data.AuthManager;
import com.example.testapp.core.AuthUser;
import com.example.testapp.core.PinSecurityManager;
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
import android.widget.EditText;
import android.widget.LinearLayout;
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
    private Button btnChangeAdminPin;

    private TextView tvAccountUsername;
    private TextView tvAccountCloudStatus;
    private Button btnLogOut;
    private Button btnChangePassword;
    private Button btnChangeEmail;
    private Button btnDeleteAccount;
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
        btnChangeAdminPin = activity.findViewById(R.id.btnChangeAdminPin);

        tvAccountUsername = activity.findViewById(R.id.tvAccountUsername);
        tvAccountCloudStatus = activity.findViewById(R.id.tvAccountCloudStatus);
        btnLogOut = activity.findViewById(R.id.btnLogOut);
        btnChangePassword = activity.findViewById(R.id.btnChangePassword);
        btnChangeEmail = activity.findViewById(R.id.btnChangeEmail);
        btnDeleteAccount = activity.findViewById(R.id.btnDeleteAccount);
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
                        Toast.makeText(activity, "Pomyślnie zsynchronizowano profil z Supabase!", Toast.LENGTH_SHORT).show();
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

        if (btnChangeAdminPin != null) {
            btnChangeAdminPin.setOnClickListener(v -> {
                boolean hasAdminPrivilege = userProfile.isCreator() || userProfile.isAdmin()
                        || (authManager != null && authManager.getCurrentUser() != null && (authManager.getCurrentUser().isAdmin() || authManager.getCurrentUser().isCreator()));
                if (!hasAdminPrivilege) {
                    Toast.makeText(activity, "Opcja zmiany PIN jest dostępna wyłącznie dla rangi Administrator lub Twórca!", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthUser currentUser = authManager != null ? authManager.getCurrentUser() : null;
                PinSecurityManager.promptFullPinChangeFlow(activity, currentUser, newPin -> {
                    if (userProfile.isCreator()) {
                        PinSecurityManager.setCreatorPin(activity, newPin);
                    }
                    if (userProfile.isAdmin()) {
                        PinSecurityManager.setAdminPin(activity, newPin);
                    }

                    if (currentUser != null) {
                        String[] err = new String[1];
                        authManager.updateUserPin(currentUser.getUsername(), newPin, currentUser, err);
                    }

                    Toast.makeText(activity, "Kod PIN został pomyślnie zmieniony!", Toast.LENGTH_SHORT).show();
                    refreshViews();
                    if (listener != null) listener.onProfileUpdated();
                });
            });
        }

        if (btnLogOut != null) {
            btnLogOut.setOnClickListener(v -> showLogoutDialog());
        }

        if (btnChangePassword != null) {
            btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        if (btnChangeEmail != null) {
            btnChangeEmail.setOnClickListener(v -> showChangeEmailDialog());
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
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

    /** Walidacja inline widoczna w dialogu zamiast znikających Toastów. */
    private void showFieldError(TextView target, String message) {
        if (target == null) return;
        target.setText(message);
        target.setTextColor(activity.getColor(R.color.expense_red));
        target.setVisibility(View.VISIBLE);
    }

    /** Usuń Konto: dostępne dla każdej zalogowanej rangi poza chronionym kontem Creatora. */
    private boolean canDeleteOwnAccount() {
        AuthUser current = (authManager != null) ? authManager.getCurrentUser() : null;
        return current != null && !current.isCreator() && !userProfile.isCreator();
    }

    private void showChangePasswordDialog() {
        AuthUser current = (authManager != null) ? authManager.getCurrentUser() : null;
        if (current == null) {
            Toast.makeText(activity, "Nie jesteś zalogowany", Toast.LENGTH_SHORT).show();
            return;
        }

        if (current.isGuest()) {
            Toast.makeText(activity, "Konto gościa nie posiada hasła. Zarejestruj się!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Admin/Creator: najpierw weryfikacja kodu PIN, potem dialog zmiany hasła
        SensitiveActionGuard.guard(activity, authManager, current, "zmiana hasła", () ->
                showChangePasswordDialogInner(current));
    }

    private void showChangePasswordDialogInner(AuthUser current) {
        Dialog dialog = new Dialog(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_change_password, null, false);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvChangePasswordSubtitle);
        EditText etCurrentPass = dialogView.findViewById(R.id.etCurrentPassword);
        EditText etNewPass = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPass = dialogView.findViewById(R.id.etConfirmPassword);
        TextView tvMessage = dialogView.findViewById(R.id.tvPasswordMessage);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitChangePassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelChangePassword);

        if (tvSubtitle != null) {
            tvSubtitle.setText("Użytkownik: @" + current.getUsername());
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit == null || etCurrentPass == null || etNewPass == null || etConfirmPass == null) {
            return;
        }

        btnSubmit.setOnClickListener(v -> {
            String curPass = etCurrentPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confPass = etConfirmPass.getText().toString();

            if (curPass.trim().isEmpty()) {
                showFieldError(tvMessage, "Wpisz aktualne hasło");
                return;
            }
            if (!current.verifyPassword(curPass)) {
                showFieldError(tvMessage, "Aktualne hasło jest niepoprawne!");
                return;
            }
            if (newPass.length() < 6) {
                showFieldError(tvMessage, "Nowe hasło musi mieć co najmniej 6 znaków!");
                return;
            }
            if (newPass.equals(curPass)) {
                showFieldError(tvMessage, "Nowe hasło musi różnić się od aktualnego!");
                return;
            }
            if (!newPass.equals(confPass)) {
                showFieldError(tvMessage, "Nowe hasła nie są identyczne!");
                return;
            }

            btnSubmit.setEnabled(false);
            btnSubmit.setText("Zapisywanie...");
            tvMessage.setVisibility(View.GONE);

            String[] err = new String[1];
            if (authManager.updateCurrentUserPassword(newPass, err)) {
                dialog.dismiss();
                Toast.makeText(activity, "Twoje hasło zostało pomyślnie zmienione!", Toast.LENGTH_SHORT).show();
            } else {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Zmień hasło");
                showFieldError(tvMessage, err[0] != null ? err[0] : "Błąd zmiany hasła");
            }
        });

        dialog.show();
    }

    /**
     * Zmiana e-maila w 2 krokach z podwójną weryfikacją:
     * 1. Użytkownik podaje nowy adres -> wysyłamy kod OTP na STARY i NOWY adres.
     * 2. Użytkownik wpisuje OBA kody -> po weryfikacji obu zapisujemy nowy adres.
     */
    private void showChangeEmailDialog() {
        AuthUser current = (authManager != null) ? authManager.getCurrentUser() : null;
        if (current == null) {
            Toast.makeText(activity, "Nie jesteś zalogowany", Toast.LENGTH_SHORT).show();
            return;
        }

        if (current.isGuest()) {
            Toast.makeText(activity, "Konto gościa nie posiada adresu e-mail. Zarejestruj się!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Admin/Creator: najpierw weryfikacja kodu PIN, potem dialog zmiany e-maila
        SensitiveActionGuard.guard(activity, authManager, current, "zmiana e-maila", () ->
                showChangeEmailDialogInner(current, oldEmailGetter(current)));
    }

    private String oldEmailGetter(AuthUser current) {
        String email = current.getEmail();
        return (email != null && !email.trim().isEmpty()) ? email.trim().toLowerCase() : "";
    }

    private void showChangeEmailDialogInner(AuthUser current, String oldEmail) {
        Dialog dialog = new Dialog(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_change_email, null, false);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        LinearLayout groupStep1 = dialogView.findViewById(R.id.groupChangeEmailStep1);
        LinearLayout groupStep2 = dialogView.findViewById(R.id.groupChangeEmailStep2);
        TextView tvCurrentEmail = dialogView.findViewById(R.id.tvChangeEmailCurrent);
        TextView tvCodeInfo = dialogView.findViewById(R.id.tvChangeEmailCodeInfo);
        TextView tvMessage = dialogView.findViewById(R.id.tvChangeEmailMessage);
        EditText etNewEmail = dialogView.findViewById(R.id.etNewEmail);
        EditText etCodeOld = dialogView.findViewById(R.id.etChangeEmailCode);
        EditText etCodeNew = dialogView.findViewById(R.id.etChangeEmailCodeNew);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitChangeEmail);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelChangeEmail);

        if (oldEmail == null || oldEmail.isEmpty()) {
            Toast.makeText(activity, "Twoje konto nie ma zapisanego adresu e-mail. Skontaktuj się z Twórcą.", Toast.LENGTH_LONG).show();
            return;
        }

        if (tvCurrentEmail != null) {
            tvCurrentEmail.setText("Aktualny e-mail: " + oldEmail);
        }

        final boolean[] stepTwo = {false};
        final String[] pendingNewEmail = {null};
        final long[] codeCreatedAt = {0};

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit == null || etNewEmail == null || etCodeOld == null || etCodeNew == null) {
            return;
        }

        btnSubmit.setOnClickListener(v -> {
            if (!stepTwo[0]) {
                // KROK 1: walidacja + wysyłka kodów na STARY i NOWY adres
                String[] err = new String[1];
                String cleanEmail = authManager.validateNewEmail(etNewEmail.getText().toString(), err);
                if (cleanEmail == null) {
                    showFieldError(tvMessage, err[0] != null ? err[0] : "Nieprawidłowy adres e-mail");
                    return;
                }

                btnSubmit.setEnabled(false);
                btnSubmit.setText("Wysyłanie kodów...");
                tvMessage.setVisibility(View.GONE);

                // Najpierw NOWY adres (OSA w Supabase wymaga odstępu między wysyłkami)
                authManager.sendEmailOtp(cleanEmail, new SupabaseClient.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        // Potem STARY adres
                        authManager.sendEmailOtp(oldEmail, new SupabaseClient.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean r2) {
                                stepTwo[0] = true;
                                pendingNewEmail[0] = cleanEmail;
                                codeCreatedAt[0] = System.currentTimeMillis();

                                if (groupStep1 != null) groupStep1.setVisibility(View.GONE);
                                if (groupStep2 != null) groupStep2.setVisibility(View.VISIBLE);
                                if (tvCodeInfo != null) {
                                    tvCodeInfo.setText("Wysłaliśmy 6-cyfrowe kody na OBA adresy:\n"
                                            + oldEmail + "\n" + cleanEmail);
                                }
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Potwierdź i zapisz nowy e-mail");
                                Toast.makeText(activity, "Kody weryfikacyjne wysłano na oba adresy", Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onError(String error) {
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Wyślij kod weryfikacyjny");
                                Toast.makeText(activity, error != null ? error : "Nie udało się wysłać kodu na stary adres", Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Wyślij kod weryfikacyjny");
                        Toast.makeText(activity, error != null ? error : "Nie udało się wysłać kodu na nowy adres", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                // KROK 2: weryfikacja OBU kodów + zapis nowego adresu
                if (System.currentTimeMillis() - codeCreatedAt[0] > 10 * 60 * 1000) {
                    Toast.makeText(activity, "Kody wygasły (ważne 10 minut). Zacznij od nowa.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }

                String enteredOld = etCodeOld.getText().toString().trim();
                String enteredNew = etCodeNew.getText().toString().trim();
                if (enteredOld.length() != 6 || enteredNew.length() != 6) {
                    showFieldError(tvMessage, "Wpisz oba 6-cyfrowe kody");
                    return;
                }

                btnSubmit.setEnabled(false);
                btnSubmit.setText("Weryfikacja kodów...");
                tvMessage.setVisibility(View.GONE);

                // Najpierw kod ze STAREGO adresu (potwierdzenie własności)
                authManager.verifyEmailOtp(oldEmail, enteredOld, new SupabaseClient.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ok) {
                        // Potem kod z NOWEGO adresu (potwierdzenie własności nowej skrzynki)
                        authManager.verifyEmailOtp(pendingNewEmail[0], enteredNew, new SupabaseClient.Callback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean ok2) {
                                String[] err = new String[1];
                                if (authManager.applyEmailChange(pendingNewEmail[0], err)) {
                                    dialog.dismiss();
                                    refreshViews();
                                    Toast.makeText(activity, "Nowy adres e-mail został zapisany!", Toast.LENGTH_LONG).show();
                                } else {
                                    btnSubmit.setEnabled(true);
                                    btnSubmit.setText("Potwierdź i zapisz nowy e-mail");
                                    showFieldError(tvMessage, err[0] != null ? err[0] : "Błąd zapisu e-maila");
                                }
                            }

                            @Override
                            public void onError(String error) {
                                btnSubmit.setEnabled(true);
                                btnSubmit.setText("Potwierdź i zapisz nowy e-mail");
                                showFieldError(tvMessage, "Nowy e-mail: " + (error != null ? error : "nieprawidłowy lub wygasły kod"));
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Potwierdź i zapisz nowy e-mail");
                        showFieldError(tvMessage, "Stary e-mail: " + (error != null ? error : "nieprawidłowy lub wygasły kod"));
                    }
                });
            }
        });

        dialog.show();
    }

    private void showDeleteAccountDialog() {
        AuthUser current = (authManager != null) ? authManager.getCurrentUser() : null;
        if (current == null) {
            Toast.makeText(activity, "Nie jesteś zalogowany", Toast.LENGTH_SHORT).show();
            return;
        }

        // Konto Creatora jest chronione systemowo - pozostałe rangi (także Admin) mogą się usunąć.
        if (current.isCreator() || userProfile.isCreator()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Ochrona konta Twórcy")
                    .setMessage("Konto Głównego Twórcy (Creator) nie może zostać usunięte z poziomu aplikacji.")
                    .setPositiveButton("ROZUMIEM", null)
                    .show();
            return;
        }

        // Admin potwierdza operację własnym kodem PIN (z jednorazowym ustawieniem PIN-u, jeśli go brak).
        SensitiveActionGuard.guard(activity, authManager, current, "usunięcie konta", () ->
                confirmDeleteAccount(current));
    }

    private void confirmDeleteAccount(AuthUser current) {
        new AlertDialog.Builder(activity)
                .setTitle("Trwałe usunięcie konta")
                .setMessage("Czy na pewno chcesz bezpowrotnie usunąć konto @" + current.getUsername() + "?\n\nTwoje dane i profil zostaną bezpowrotnie usunięte z bazy danych.")
                .setPositiveButton("USUŃ KONTO", (dialog, which) -> {
                    String[] err = new String[1];
                    if (authManager.deleteSelfAccount(userProfile, err)) {
                        Toast.makeText(activity, "Twoje konto zostało pomyślnie usunięte", Toast.LENGTH_LONG).show();
                        if (listener != null) listener.onLogoutRequested();
                    } else {
                        Toast.makeText(activity, err[0] != null ? err[0] : "Błąd usuwania konta", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("ANULUJ", null)
                .show();
    }

    private void showSupabaseSqlDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Synchronizacja z Supabase")
                .setMessage("Twoje konto działa poprawnie i jest zapisane na Twoim telefonie.\n\nAby włączyć kopię zapasową w chmurze Supabase:\n\n1. Wejdź na panel Supabase -> SQL Editor\n2. Kliknij poniższy przycisk, aby skopiować gotowy kod SQL\n3. Wklej kod i kliknij zielony przycisk 'Run'")
                .setPositiveButton("KOPIUJ KOD SQL", (dialog, which) -> {
                    String sql = SupabaseSqlFix.SQL;

                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Supabase Fix SQL", sql);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(activity, "Skopiowano kod SQL do schowka! Wklej go w Supabase SQL Editor.", Toast.LENGTH_LONG).show();
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

    public void refreshViews() {            String displayName;
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

        // Awatar z pierwszą literą nazwy (zamiast emoji)
        String avatarLetter = displayName.trim().isEmpty() ? "U" : displayName.trim().substring(0, 1).toUpperCase();
        if (tvHeaderAvatar != null) tvHeaderAvatar.setText(avatarLetter);
        if (tvProfileAvatarLarge != null) tvProfileAvatarLarge.setText(avatarLetter);
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
                tvAccountCloudStatus.setText("Aktywna");
                tvAccountCloudStatus.setTextColor(Color.parseColor("#059669"));
            } else {
                tvAccountCloudStatus.setText("Lokalna (dotknij aby połączyć)");
                tvAccountCloudStatus.setTextColor(Color.parseColor("#3B82F6"));
            }
        }

        if (tvProfileSyncStatus != null) {
            if (SupabaseClient.isLastSyncSuccess()) {
                tvProfileSyncStatus.setText("Zsynchronizowano z chmurą");
                tvProfileSyncStatus.setTextColor(Color.parseColor("#059669"));
            } else {
                tvProfileSyncStatus.setText("Zapisano na urządzeniu");
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

        // Dostęp do panelu zarządzania i opcji PIN wyłącznie dla administratorów (Admin oraz Creator)
        boolean isPrivileged = userProfile.isCreator() || userProfile.isAdmin()
                || (authManager != null && authManager.getCurrentUser() != null && (authManager.getCurrentUser().isAdmin() || authManager.getCurrentUser().isCreator()));
        if (cardUserManagement != null) {
            cardUserManagement.setVisibility(isPrivileged ? View.VISIBLE : View.GONE);
            if (isPrivileged) {
                if (tvManagementHeaderTitle != null) {
                    tvManagementHeaderTitle.setText(userProfile.isCreator() ? "Zarządzanie Użytkownikami" : "Panel Administratora");
                }
                if (tvManagementDesc != null) {
                    tvManagementDesc.setText("Zarządzaj kontami oraz kodem PIN administratora.");
                }
                if (btnOpenUserManagement != null) {
                    btnOpenUserManagement.setText("Zarządzaj");
                }
                if (btnChangeAdminPin != null) {
                    btnChangeAdminPin.setVisibility(View.VISIBLE);
                    btnChangeAdminPin.setText("Zmień PIN");
                }
            } else {
                if (btnChangeAdminPin != null) {
                    btnChangeAdminPin.setVisibility(View.GONE);
                }
            }
        }

        if (btnDeleteAccount != null) {
            btnDeleteAccount.setVisibility(canDeleteOwnAccount() ? View.VISIBLE : View.GONE);
        }

        // Opcje konta (hasło / e-mail) mają sens tylko dla zalogowanych kont z hasłem
        boolean hasRealAccount = loggedIn
                && authManager.getCurrentUser() != null
                && !authManager.getCurrentUser().isGuest();
        if (btnChangePassword != null) {
            btnChangePassword.setVisibility(hasRealAccount ? View.VISIBLE : View.GONE);
        }
        if (btnChangeEmail != null) {
            btnChangeEmail.setVisibility(hasRealAccount ? View.VISIBLE : View.GONE);
        }
    }
}
