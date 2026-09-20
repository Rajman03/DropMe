package com.example.testapp.feature.auth;

import com.example.testapp.core.SupabaseSqlFix;
import com.example.testapp.data.AuthManager;
import com.example.testapp.core.AuthUser;
import com.example.testapp.core.LoginThrottle;
import com.example.testapp.core.PinSecurityManager;
import com.example.testapp.core.PrivacyPolicyDialog;
import com.example.testapp.data.SupabaseClient;
import com.example.testapp.core.UserProfile;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AuthController {

    public interface OnAuthSuccessListener {
        void onAuthSuccess();
    }

    private final Activity activity;
    private final AuthManager authManager;
    private final UserProfile userProfile;
    private final OnAuthSuccessListener listener;

    private ScrollView layoutAuth;
    private ScrollView layoutPinAuth;
    private View layoutMainApp;

    // Login & Register Form views
    private Button tabLogin;
    private Button tabRegister;
    private LinearLayout groupName;

    private EditText etName;
    private EditText etLogin;
    private EditText etPassword;

    private Button btnSubmit;
    private FrameLayout btnLogoSecret;
    private TextView tvAuthPrivacyPolicy;

    private boolean isRegisterMode = false;
    private final LoginThrottle loginThrottle = new LoginThrottle();

    // PIN Screen views (custom style matching screenshot)
    private TextView tvPinScreenTitle;
    private TextView tvPinScreenSubtitle;
    private TextView tvPinErrorMessage;
    private LinearLayout pinBoxesContainer;

    private FrameLayout pinBox1;
    private FrameLayout pinBox2;
    private FrameLayout pinBox3;
    private FrameLayout pinBox4;

    private TextView tvPinDigit1;
    private TextView tvPinDigit2;
    private TextView tvPinDigit3;
    private TextView tvPinDigit4;

    private final StringBuilder enteredPin = new StringBuilder();
    private AuthUser currentPendingUser;

    public AuthController(Activity activity, AuthManager authManager, UserProfile userProfile, OnAuthSuccessListener listener) {
        this.activity = activity;
        this.authManager = authManager;
        this.userProfile = userProfile;
        this.listener = listener;

        bindViews();
        setupActions();
        setupPinKeypad();
        setMode(false); // default to Login mode
    }

    private void bindViews() {
        layoutAuth = activity.findViewById(R.id.layoutAuth);
        layoutPinAuth = activity.findViewById(R.id.layoutPinAuth);
        // layoutMainApp jest w module :app - wstrzykiwany przez attachMainAppView(...)

        tabLogin = activity.findViewById(R.id.tabAuthLogin);
        tabRegister = activity.findViewById(R.id.tabAuthRegister);
        groupName = activity.findViewById(R.id.groupAuthName);

        etName = activity.findViewById(R.id.etAuthName);
        etLogin = activity.findViewById(R.id.etAuthLogin);
        etPassword = activity.findViewById(R.id.etAuthPassword);

        btnSubmit = activity.findViewById(R.id.btnAuthSubmit);
        btnLogoSecret = activity.findViewById(R.id.btnAuthLogoSecret);
        tvAuthPrivacyPolicy = activity.findViewById(R.id.tvAuthPrivacyPolicy);

        // PIN Screen
        tvPinScreenTitle = activity.findViewById(R.id.tvPinScreenTitle);
        tvPinScreenSubtitle = activity.findViewById(R.id.tvPinScreenSubtitle);
        tvPinErrorMessage = activity.findViewById(R.id.tvPinErrorMessage);
        pinBoxesContainer = activity.findViewById(R.id.pinBoxesContainer);

        pinBox1 = activity.findViewById(R.id.pinBox1);
        pinBox2 = activity.findViewById(R.id.pinBox2);
        pinBox3 = activity.findViewById(R.id.pinBox3);
        pinBox4 = activity.findViewById(R.id.pinBox4);

        tvPinDigit1 = activity.findViewById(R.id.tvPinDigit1);
        tvPinDigit2 = activity.findViewById(R.id.tvPinDigit2);
        tvPinDigit3 = activity.findViewById(R.id.tvPinDigit3);
        tvPinDigit4 = activity.findViewById(R.id.tvPinDigit4);
    }

    /**
     * Ekran główny aplikacji należy do modułu :app, więc nie może być wyszukany
     * tutaj przez R.id. Wstrzykuje go MainActivity.
     */
    public void attachMainAppView(View mainAppRoot) {
        this.layoutMainApp = mainAppRoot;
    }

    private void setupActions() {
        if (tabLogin != null) tabLogin.setOnClickListener(v -> setMode(false));
        if (tabRegister != null) tabRegister.setOnClickListener(v -> setMode(true));

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> handleAuthSubmit());
        }

        if (tvAuthPrivacyPolicy != null) {
            tvAuthPrivacyPolicy.setOnClickListener(v -> PrivacyPolicyDialog.show(activity));
        }

        // Logo has no backdoors
    }

    private void setupPinKeypad() {
        bindKey(R.id.btnKey0, "0");
        bindKey(R.id.btnKey1, "1");
        bindKey(R.id.btnKey2, "2");
        bindKey(R.id.btnKey3, "3");
        bindKey(R.id.btnKey4, "4");
        bindKey(R.id.btnKey5, "5");
        bindKey(R.id.btnKey6, "6");
        bindKey(R.id.btnKey7, "7");
        bindKey(R.id.btnKey8, "8");
        bindKey(R.id.btnKey9, "9");

        View btnBackspace = activity.findViewById(R.id.btnKeyBackspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> onBackspaceClicked());
        }

        View btnFingerprint = activity.findViewById(R.id.btnKeyFingerprint);
        if (btnFingerprint != null) {
            btnFingerprint.setOnClickListener(v -> {
                Toast.makeText(activity, "Biometria niedostępna na tym urządzeniu", Toast.LENGTH_SHORT).show();
            });
        }

        View btnForgot = activity.findViewById(R.id.btnPinForgot);
        if (btnForgot != null) {
            btnForgot.setOnClickListener(v -> showForgotPinDialog());
        }

        View btnBackToLogin = activity.findViewById(R.id.btnPinBackToLogin);
        if (btnBackToLogin != null) {
            btnBackToLogin.setOnClickListener(v -> showAuthScreen());
        }
    }

    private void bindKey(int resId, String digit) {
        View keyView = activity.findViewById(resId);
        if (keyView != null) {
            keyView.setOnClickListener(v -> onDigitClicked(digit));
        }
    }

    private void onDigitClicked(String digit) {
        if (enteredPin.length() >= 4) return;

        enteredPin.append(digit);
        if (tvPinErrorMessage != null) tvPinErrorMessage.setText("");
        updatePinBoxes();

        if (enteredPin.length() == 4) {
            validateEnteredPin();
        }
    }

    private void onBackspaceClicked() {
        if (enteredPin.length() > 0) {
            enteredPin.deleteCharAt(enteredPin.length() - 1);
            if (tvPinErrorMessage != null) tvPinErrorMessage.setText("");
            updatePinBoxes();
        }
    }

    private void updatePinBoxes() {
        int length = enteredPin.length();
        updateBox(pinBox1, tvPinDigit1, 0, length);
        updateBox(pinBox2, tvPinDigit2, 1, length);
        updateBox(pinBox3, tvPinDigit3, 2, length);
        updateBox(pinBox4, tvPinDigit4, 3, length);
    }

    private void updateBox(FrameLayout box, TextView tvDigit, int index, int currentLen) {
        if (box == null || tvDigit == null) return;

        if (index < currentLen) {
            // Filled box: blue border and distinct asterisk (*)
            box.setBackgroundResource(R.drawable.pin_box_filled);
            tvDigit.setText("✻"); // or "*" clean star
            tvDigit.setTextColor(0xFF0D6EFD);
        } else if (index == currentLen) {
            // Current focused box
            box.setBackgroundResource(R.drawable.pin_box_active);
            tvDigit.setText("");
        } else {
            // Empty box
            box.setBackgroundResource(R.drawable.pin_box_empty);
            tvDigit.setText("");
        }
    }

    private void validateEnteredPin() {
        String pin = enteredPin.toString();
        boolean isValid = PinSecurityManager.verifyUserPin(activity, currentPendingUser, pin);

        if (isValid) {
            // Success!
            if (tvPinErrorMessage != null) tvPinErrorMessage.setText("");
            AuthUser user = currentPendingUser;
            authManager.setCurrentUser(user, userProfile);
            Toast.makeText(activity, "Autoryzacja udana! Zalogowano jako " + user.getRank(), Toast.LENGTH_SHORT).show();
            clearInputs();
            dismissAuth();
        } else {
            // Wrong PIN error
            if (tvPinErrorMessage != null) {
                tvPinErrorMessage.setText("Nieprawidłowy kod PIN!");
            }
            if (pinBox1 != null) pinBox1.setBackgroundResource(R.drawable.pin_box_error);
            if (pinBox2 != null) pinBox2.setBackgroundResource(R.drawable.pin_box_error);
            if (pinBox3 != null) pinBox3.setBackgroundResource(R.drawable.pin_box_error);
            if (pinBox4 != null) pinBox4.setBackgroundResource(R.drawable.pin_box_error);

            if (pinBoxesContainer != null) {
                pinBoxesContainer.postDelayed(() -> {
                    enteredPin.setLength(0);
                    updatePinBoxes();
                    if (tvPinErrorMessage != null) tvPinErrorMessage.setText("");
                }, 800);
            } else {
                enteredPin.setLength(0);
                updatePinBoxes();
            }
        }
    }

    private void showForgotPinDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("🔑 Zapomniałeś kodu PIN?")
                .setMessage("Skontaktuj się z Twórcą (Creator) konta w celu zresetowania kodu PIN.\n\nMożesz również zmienić PIN po zalogowaniu w zakładce Profilu.")
                .setPositiveButton("ROZUMIEM", null)
                .show();
    }

    public void showPinScreen(AuthUser user) {
        this.currentPendingUser = user;
        this.enteredPin.setLength(0);

        if (layoutAuth != null) layoutAuth.setVisibility(View.GONE);
        if (layoutMainApp != null) layoutMainApp.setVisibility(View.GONE);
        if (layoutPinAuth != null) layoutPinAuth.setVisibility(View.VISIBLE);

        if (tvPinScreenTitle != null) {
            tvPinScreenTitle.setText("Weryfikacja PIN");
        }

        if (tvPinScreenSubtitle != null) {
            String roleName = user.isCreator() ? "Creator (Twórca)" : "Administrator";
            tvPinScreenSubtitle.setText("Weryfikacja 2FA • " + roleName + " @" + user.getUsername());
        }

        if (tvPinErrorMessage != null) {
            tvPinErrorMessage.setText("");
        }

        updatePinBoxes();
    }

    public void setMode(boolean register) {
        this.isRegisterMode = register;

        if (tabLogin != null) {
            tabLogin.setBackgroundResource(register ? android.R.color.transparent : R.drawable.btn_pill_dark);
            tabLogin.setTextColor(activity.getColor(register ? R.color.text_secondary : R.color.white));
        }

        if (tabRegister != null) {
            tabRegister.setBackgroundResource(register ? R.drawable.btn_pill_dark : android.R.color.transparent);
            tabRegister.setTextColor(activity.getColor(register ? R.color.white : R.color.text_secondary));
        }

        if (groupName != null) groupName.setVisibility(register ? View.VISIBLE : View.GONE);

        if (btnSubmit != null) {
            btnSubmit.setText(register ? "Zarejestruj się i odblokuj" : "Zaloguj się");
        }
    }

    private void handleAuthSubmit() {
        String login = etLogin != null ? etLogin.getText().toString().trim() : "";
        String pass = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (login.isEmpty() || pass.isEmpty()) {
            Toast.makeText(activity, "Wpisz login oraz hasło", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isRegisterMode) {
            String name = etName != null ? etName.getText().toString().trim() : "";

            if (name.isEmpty()) {
                Toast.makeText(activity, "Podaj imię i nazwisko", Toast.LENGTH_SHORT).show();
                return;
            }

            if (btnSubmit != null) {
                btnSubmit.setEnabled(false);
                btnSubmit.setText("Rejestracja w Supabase...");
            }

            authManager.registerInSupabase(login, pass, name, userProfile, new SupabaseClient.Callback<AuthUser>() {
                @Override
                public void onSuccess(AuthUser user) {
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText(isRegisterMode ? "Zarejestruj się i odblokuj" : "Zaloguj się");
                    }
                    Toast.makeText(activity, "✓ Konto @" + user.getUsername() + " utworzone!", Toast.LENGTH_SHORT).show();
                    clearInputs();
                    dismissAuth();
                }

                @Override
                public void onError(String error) {
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText(isRegisterMode ? "Zarejestruj się i odblokuj" : "Zaloguj się");
                    }
                    Toast.makeText(activity, error != null ? error : "Błąd rejestracji", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (loginThrottle.isLocked()) {
                long sec = loginThrottle.getRemainingSeconds();
                Toast.makeText(activity, "Zbyt wiele prób logowania. Odczekaj " + sec + " s.", Toast.LENGTH_LONG).show();
                return;
            }

            if (btnSubmit != null) {
                btnSubmit.setEnabled(false);
                btnSubmit.setText("Logowanie...");
            }

            authManager.loginInSupabase(login, pass, userProfile, new SupabaseClient.Callback<AuthUser>() {
                @Override
                public void onSuccess(AuthUser user) {
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Zaloguj się");
                    }
                    loginThrottle.recordSuccess();

                    if ((user.isAdmin() || user.isCreator()) && user.getPin() != null && !user.getPin().isEmpty()) {
                        showPinScreen(user);
                    } else {
                        authManager.setCurrentUser(user, userProfile);
                        Toast.makeText(activity, "✓ Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show();
                        clearInputs();
                        dismissAuth();
                    }
                }

                @Override
                public void onError(String error) {
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Zaloguj się");
                    }
                    loginThrottle.recordFailure();
                    Toast.makeText(activity, error != null ? error : "Nieprawidłowy login lub hasło", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showSqlErrorDialog(String error) {
        new AlertDialog.Builder(activity)
                .setTitle("🛠️ Połączenie z Supabase")
                .setMessage("Aby włączyć chmurę Supabase:\n1. Otwórz w przeglądarce Supabase -> SQL Editor\n2. Kliknij poniższy przycisk, aby skopiować kod SQL\n3. Wklej w SQL Editor i kliknij 'Run'")
                .setPositiveButton("📋 KOPIUJ KOD SQL", (dialog, which) -> {
                    String sql = SupabaseSqlFix.SQL;

                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("DropMe SQL Fix", sql);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(activity, "✓ Skopiowano kod SQL do schowka! Wklej go w Supabase SQL Editor.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("ZAMKNIJ", null)
                .show();
    }

    private void clearInputs() {
        if (etLogin != null) etLogin.setText("");
        if (etPassword != null) etPassword.setText("");
        if (etName != null) etName.setText("");
        enteredPin.setLength(0);
    }

    public void showAuthScreen() {
        if (layoutAuth != null) layoutAuth.setVisibility(View.VISIBLE);
        if (layoutPinAuth != null) layoutPinAuth.setVisibility(View.GONE);
        if (layoutMainApp != null) layoutMainApp.setVisibility(View.GONE);
    }

    public void dismissAuth() {
        if (layoutAuth != null) layoutAuth.setVisibility(View.GONE);
        if (layoutPinAuth != null) layoutPinAuth.setVisibility(View.GONE);
        if (layoutMainApp != null) layoutMainApp.setVisibility(View.VISIBLE);

        if (listener != null) {
            listener.onAuthSuccess();
        }
    }
}
