package com.example.testapp.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PinSecurityManager {

    private static final String PREFS_NAME = "security_pin_prefs";
    private static final String KEY_ADMIN_PIN_HASH = "custom_admin_pin_hash";
    private static final String KEY_CREATOR_PIN_HASH = "custom_creator_pin_hash";

    public interface PinVerificationCallback {
        void onPinVerified(String role);
    }

    public interface OnPinSetCallback {
        void onPinSet(String pin);
    }

    public static String getCreatorPinHash(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_CREATOR_PIN_HASH, null);
    }

    public static void setCreatorPin(Context context, String newPin) {
        if (context == null || newPin == null || CredentialHasher.isTrivialPin(newPin)) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String hashed = CredentialHasher.hash(newPin.trim());
        prefs.edit().putString(KEY_CREATOR_PIN_HASH, hashed).apply();
    }

    public static String getAdminPinHash(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ADMIN_PIN_HASH, null);
    }

    public static void setAdminPin(Context context, String newPin) {
        if (context == null || newPin == null || CredentialHasher.isTrivialPin(newPin)) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String hashed = CredentialHasher.hash(newPin.trim());
        prefs.edit().putString(KEY_ADMIN_PIN_HASH, hashed).apply();
    }

    public static boolean verifyUserPin(Context context, AuthUser user, String enteredPin) {
        if (user == null || enteredPin == null) return false;
        String cleanPin = enteredPin.trim();

        // 1. Check account-specific PIN if set
        if (user.getPin() != null && !user.getPin().trim().isEmpty()) {
            if (CredentialHasher.verify(cleanPin, user.getPin().trim())) {
                return true;
            }
        }

        // 2. Creator checks (custom creator PIN hash)
        if (user.isCreator() && context != null) {
            String creatorHash = getCreatorPinHash(context);
            if (creatorHash != null && CredentialHasher.verify(cleanPin, creatorHash)) {
                return true;
            }
        }

        // 3. Admin checks
        if (user.isAdmin() && context != null) {
            String adminHash = getAdminPinHash(context);
            if (adminHash != null && CredentialHasher.verify(cleanPin, adminHash)) {
                return true;
            }
        }

        return false;
    }

    public interface OnPinSetListener {
        void onPinSet(String newPin);
    }

    public static void promptSetAdminPinDialog(android.content.Context context, String username, OnPinSetListener callback) {
        if (context == null || callback == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pin_auth, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvPinTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvPinHint);
        EditText etPin = dialogView.findViewById(R.id.etPinInput);
        Button btnSubmit = dialogView.findViewById(R.id.btnConfirmPin);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelPin);

        if (tvTitle != null) tvTitle.setText("Ustaw PIN Administratora");
        if (tvDesc != null) tvDesc.setText("Wprowadź 4-cyfrowy kod PIN dla @" + username);

        if (btnSubmit != null && etPin != null) {
            btnSubmit.setOnClickListener(v -> {
                String entered = etPin.getText().toString().trim();
                if (entered.length() != 4 || !entered.matches("\\d{4}")) {
                    Toast.makeText(context, "Wprowadź dokładnie 4 cyfry PIN-u", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                callback.onPinSet(entered);
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    // Convenience method for unit tests
    public static boolean verifyUserPin(AuthUser user, String enteredPin) {
        return verifyUserPin(null, user, enteredPin);
    }

    public static void promptAdminPin(Activity activity, AuthUser user, PinVerificationCallback callback) {
        if (activity == null || callback == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_pin_auth, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvPinTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvPinHint);
        EditText etPin = dialogView.findViewById(R.id.etPinInput);
        Button btnSubmit = dialogView.findViewById(R.id.btnConfirmPin);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelPin);

        boolean isCreator = user != null && user.isCreator();
        if (tvTitle != null) tvTitle.setText(isCreator ? "Weryfikacja Właściciela" : "Weryfikacja Administratora");
        if (tvDesc != null) tvDesc.setText("Wprowadź swój 4-cyfrowy kod PIN zabezpieczający.");

        if (btnSubmit != null && etPin != null) {
            btnSubmit.setOnClickListener(v -> {
                String entered = etPin.getText().toString().trim();
                if (entered.length() != 4) {
                    Toast.makeText(activity, "Wprowadź dokładnie 4 cyfry PIN-u", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (verifyUserPin(activity, user, entered)) {
                    dialog.dismiss();
                    callback.onPinVerified(isCreator ? "Creator" : "Admin");
                } else {
                    Toast.makeText(activity, "Nieprawidłowy kod PIN!", Toast.LENGTH_SHORT).show();
                    etPin.setText("");
                }
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }
}
