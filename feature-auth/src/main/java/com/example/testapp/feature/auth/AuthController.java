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
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

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
    private LinearLayout groupEmail;

    private TextView tvLoginLabel;
    private EditText etEmail;
    private EditText etLogin;
    private EditText etPassword;
    private TextView tvForgotPassword;

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
        groupEmail = activity.findViewById(R.id.groupAuthEmail);

        tvLoginLabel = activity.findViewById(R.id.tvAuthLoginLabel);
        etEmail = activity.findViewById(R.id.etAuthEmail);
        etLogin = activity.findViewById(R.id.etAuthLogin);
        etPassword = activity.findViewById(R.id.etAuthPassword);
        tvForgotPassword = activity.findViewById(R.id.tvAuthForgotPassword);

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

        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
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

        if (tvLoginLabel != null) {
            tvLoginLabel.setText(register ? "Nazwa użytkownika (Username)" : "Login");
        }

        if (etLogin != null) {
            etLogin.setHint(register ? "np. jan_kowalski" : "Wpisz login lub e-mail");
        }

        if (groupEmail != null) {
            groupEmail.setVisibility(register ? View.VISIBLE : View.GONE);
        }

        if (tvForgotPassword != null) {
            tvForgotPassword.setVisibility(register ? View.GONE : View.VISIBLE);
        }

        if (btnSubmit != null) {
            btnSubmit.setText(register ? "Zarejestruj się i potwierdź e-mail" : "Zaloguj się");
        }
    }

    private void handleAuthSubmit() {
        if (isRegisterMode) {
            handleRegisterSubmit();
        } else {
            handleLoginSubmit();
        }
    }

    private void handleRegisterSubmit() {
        String username = etLogin != null ? etLogin.getText().toString().trim() : "";
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";
        String pass = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (username.isEmpty()) {
            Toast.makeText(activity, "Podaj nazwę użytkownika (Username)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!username.matches("^[a-zA-Z0-9._-]{3,32}$")) {
            Toast.makeText(activity, "Username musi mieć 3-32 znaki (litery, cyfry, . _ -)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            Toast.makeText(activity, "Podaj prawidłowy adres e-mail (np. nazwa@domena.pl)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pass.length() < 6) {
            Toast.makeText(activity, "Hasło musi mieć co najmniej 6 znaków", Toast.LENGTH_SHORT).show();
            return;
        }

        if (authManager.findUser(username) != null) {
            Toast.makeText(activity, "Użytkownik o takim loginie już istnieje!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (authManager.findUserByEmail(email) != null) {
            Toast.makeText(activity, "Ten adres e-mail jest już przypisany do innego konta!", Toast.LENGTH_SHORT).show();
            return;
        }

        long createdAt = System.currentTimeMillis();

        if (btnSubmit != null) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("Wysyłanie kodu...");
        }

        authManager.sendEmailOtp(email, new SupabaseClient.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (btnSubmit != null) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Zarejestruj się");
                }
                showEmailVerificationDialog(username, email, pass, createdAt);
            }

            @Override
            public void onError(String error) {
                if (btnSubmit != null) {
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Zarejestruj się");
                }
                showEmailSendErrorDialog(error);
            }
        });
    }

    private void showEmailVerificationDialog(String username, String email, String password, long codeCreatedAt) {
        Dialog dialog = new Dialog(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_email_verify, null, false);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvEmail = dialogView.findViewById(R.id.tvVerifyEmail);
        EditText etCode = dialogView.findViewById(R.id.etVerifyCode);
        TextView tvInfo = dialogView.findViewById(R.id.tvVerifyInfo);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmVerify);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelVerify);

        if (tvEmail != null) tvEmail.setText(email);
        if (tvInfo != null) {
            tvInfo.setText("⏱️ Kod jest ważny przez 10 minut. Jeśli nie widzisz wiadomości, sprawdź folder SPAM.");
        }

        final int[] attemptsLeft = new int[]{3};

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                if (System.currentTimeMillis() - codeCreatedAt > 10 * 60 * 1000) {
                    Toast.makeText(activity, "Kod weryfikacyjny wygasł (ważny 10 minut). Zarejestruj się ponownie.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }

                String entered = etCode != null ? etCode.getText().toString().trim() : "";
                if (entered.length() != 6) {
                    Toast.makeText(activity, "Wpisz 6-cyfrowy kod z e-maila", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnConfirm.setEnabled(false);
                btnConfirm.setText("Weryfikacja kodu...");

                authManager.verifyEmailOtp(email, entered, new SupabaseClient.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ok) {
                        // Kod poprawny - zapis profilu w Supabase
                        btnConfirm.setText("Zapisywanie profilu w Supabase...");
                        authManager.registerInSupabase(username, email, password, userProfile, new SupabaseClient.Callback<AuthUser>() {
                            @Override
                            public void onSuccess(AuthUser user) {
                                dialog.dismiss();
                                Toast.makeText(activity, "✓ Konto @" + user.getUsername() + " zostało pomyślnie utworzone!", Toast.LENGTH_LONG).show();
                                clearInputs();
                                dismissAuth();
                            }

                            @Override
                            public void onError(String error) {
                                btnConfirm.setEnabled(true);
                                btnConfirm.setText("Potwierdź i utwórz konto");
                                showSqlErrorDialog("Błąd zapisu profilu w Supabase: " + (error != null ? error : "Brak uprawnień RLS"));
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Potwierdź i utwórz konto");

                        if (!SupabaseClient.ERR_INVALID_OTP.equals(error)) {
                            // Błąd sieci / konfiguracji - nie zużywa prób
                            Toast.makeText(activity, error != null ? error : "Błąd weryfikacji kodu", Toast.LENGTH_LONG).show();
                            return;
                        }

                        attemptsLeft[0]--;
                        if (attemptsLeft[0] <= 0) {
                            Toast.makeText(activity, "Wykorzystano 3 próby. Zarejestruj się ponownie, aby dostać nowy kod.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            return;
                        }
                        if (tvInfo != null) {
                            tvInfo.setText("⚠️ Nieprawidłowy kod! Pozostało prób: " + attemptsLeft[0]);
                            tvInfo.setTextColor(0xFFEF4444);
                        }
                        Toast.makeText(activity, "Nieprawidłowy kod weryfikacyjny! Pozostało prób: " + attemptsLeft[0], Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }

    private void handleLoginSubmit() {
        String login = etLogin != null ? etLogin.getText().toString().trim() : "";
        String pass = etPassword != null ? etPassword.getText().toString().trim() : "";

        if (login.isEmpty() || pass.isEmpty()) {
            Toast.makeText(activity, "Wpisz login oraz hasło", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void showForgotPasswordDialog() {
        Dialog dialog = new Dialog(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_forgot_password, null, false);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        EditText etForgotEmail = dialogView.findViewById(R.id.etForgotEmail);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitForgotEmail);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelForgotEmail);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String email = etForgotEmail != null ? etForgotEmail.getText().toString().trim().toLowerCase() : "";
                if (email.isEmpty() || !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    Toast.makeText(activity, "Podaj prawidłowy adres e-mail", Toast.LENGTH_SHORT).show();
                    return;
                }

                AuthUser existing = authManager.findUserByEmail(email);
                if (existing == null) {
                    Toast.makeText(activity, "Nie znaleziono konta powiązanego z adresem " + email, Toast.LENGTH_LONG).show();
                    return;
                }

                if (btnSubmit != null) {
                    btnSubmit.setEnabled(false);
                    btnSubmit.setText("Wysyłanie kodu...");
                }

                long createdAt = System.currentTimeMillis();

                authManager.sendEmailOtp(email, new SupabaseClient.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        dialog.dismiss();
                        Toast.makeText(activity, "✓ Wysłaliśmy kod na adres " + email, Toast.LENGTH_LONG).show();
                        showResetPasswordDialog(email, createdAt);
                    }

                    @Override
                    public void onError(String error) {
                        if (btnSubmit != null) {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Wyślij kod weryfikacyjny");
                        }
                        showEmailSendErrorDialog(error);
                    }
                });
            });
        }

        dialog.show();
    }

    private void showResetPasswordDialog(String email, long codeCreatedAt) {
        Dialog dialog = new Dialog(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_reset_password, null, false);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        TextView tvInfo = dialogView.findViewById(R.id.tvResetEmailInfo);
        EditText etCode = dialogView.findViewById(R.id.etResetCode);
        EditText etNewPass = dialogView.findViewById(R.id.etResetNewPass);
        EditText etConfirmPass = dialogView.findViewById(R.id.etResetConfirmPass);
        Button btnSubmit = dialogView.findViewById(R.id.btnSubmitResetPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelResetPassword);

        if (tvInfo != null) {
            tvInfo.setText("Wysłaliśmy 6-cyfrowy kod na adres:\n" + email + "\n(sprawdź też folder SPAM)");
        }

        final int[] attemptsLeft = new int[]{3};

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                if (System.currentTimeMillis() - codeCreatedAt > 10 * 60 * 1000) {
                    Toast.makeText(activity, "Kod weryfikacyjny wygasł (ważny 10 minut). Wyślij kod ponownie.", Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                    return;
                }

                String enteredCode = etCode != null ? etCode.getText().toString().trim() : "";
                String newPass = etNewPass != null ? etNewPass.getText().toString().trim() : "";
                String confirmPass = etConfirmPass != null ? etConfirmPass.getText().toString().trim() : "";

                if (enteredCode.isEmpty()) {
                    Toast.makeText(activity, "Wpisz 6-cyfrowy kod z e-maila", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (newPass.length() < 6) {
                    Toast.makeText(activity, "Hasło musi mieć co najmniej 6 znaków!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(activity, "Nowe hasła nie są identyczne!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSubmit.setEnabled(false);
                btnSubmit.setText("Weryfikacja w Supabase...");

                authManager.verifyEmailOtp(email, enteredCode, new SupabaseClient.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean ok) {
                        String[] outErr = new String[1];
                        if (authManager.resetPasswordByEmail(email, newPass, outErr)) {
                            dialog.dismiss();
                            Toast.makeText(activity, "✓ Hasło zostało zmienione! Możesz się teraz zalogować.", Toast.LENGTH_LONG).show();
                            setMode(false);
                            if (etLogin != null) etLogin.setText(email);
                            if (etPassword != null) etPassword.setText("");
                        } else {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText("Ustaw nowe hasło");
                            Toast.makeText(activity, outErr[0] != null ? outErr[0] : "Błąd resetu hasła", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText("Ustaw nowe hasło");

                        if (!SupabaseClient.ERR_INVALID_OTP.equals(error)) {
                            Toast.makeText(activity, error != null ? error : "Błąd weryfikacji kodu", Toast.LENGTH_LONG).show();
                            return;
                        }

                        attemptsLeft[0]--;
                        if (attemptsLeft[0] <= 0) {
                            Toast.makeText(activity, "Wykorzystano limit 3 prób. Wyślij kod ponownie.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                            return;
                        }
                        Toast.makeText(activity, "Nieprawidłowy kod! Pozostało prób: " + attemptsLeft[0], Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }

    private void showEmailSendErrorDialog(String error) {
        new AlertDialog.Builder(activity)
                .setTitle("Nie udało się wysłać e-maila")
                .setMessage(error != null && !error.isEmpty() ? error : "Nieznany błąd Supabase Auth.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showSqlErrorDialog(String error) {
        String msg = (error != null && !error.isEmpty())
                ? ("⚠️ " + error + "\n\n")
                : "";
        new AlertDialog.Builder(activity)
                .setTitle("🛠️ Supabase - Wymagana konfiguracja")
                .setMessage(msg + "Baza Supabase zablokowała zapis przez Row Level Security (RLS).\n\nAby to naprawić:\n1. Otwórz w przeglądarce: Supabase -> SQL Editor\n2. Kliknij poniższy przycisk 'KOPIUJ KOD SQL'\n3. Wklej w SQL Editor i kliknij 'Run'")
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
        if (etEmail != null) etEmail.setText("");
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
