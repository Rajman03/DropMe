package com.example.testapp.feature.profile;

import com.example.testapp.core.AuthUser;
import com.example.testapp.data.AuthManager;
import com.example.testapp.core.PinSecurityManager;

import android.app.Activity;
import android.widget.Toast;

/**
 * Wspólny helper bezpieczeństwa dla wrażliwych operacji na koncie
 * (zmiana hasła / zmiana e-maila) wymagający kodu PIN dla rang
 * Administrator oraz Creator.
 */
public final class SensitiveActionGuard {

    public interface OnGuardPassed {
        void onAllowed();
    }

    private SensitiveActionGuard() {
    }

    /**
     * Dla Admina/Creatora najpierw weryfikuje PIN, a następnie wywołuje akcję.
     * Jeśli użytkownik nie ma jeszcze ustawionego PIN-u, prosi o jego
     * jednorazowe utworzenie (i zapisuje go na koncie), po czym wykonuje akcję.
     * Dla pozostałych rang (np. Guest, Premium) akcja wykonywana jest od razu.
     */
    public static void guard(Activity activity, AuthManager authManager, AuthUser user,
                             String actionName, OnGuardPassed action) {
        boolean needsPin = user != null && (user.isAdmin() || user.isCreator());
        if (!needsPin) {
            action.onAllowed();
            return;
        }

        if (PinSecurityManager.hasPin(activity, user)) {
            // Standardowa weryfikacja istniejącego PIN-u
            PinSecurityManager.promptAdminPin(activity, user, role -> action.onAllowed());
            return;
        }

        // Brak PIN-u na koncie - jednorazowe ustawienie przed operacją
        Toast.makeText(activity,
                "Operacja \"" + actionName + "\" wymaga kodu PIN. Ustaw go teraz (jednorazowo).",
                Toast.LENGTH_LONG).show();

        PinSecurityManager.promptChangePinDialog(activity,
                "Ustaw kod PIN",
                "Zabezpiecz swoje konto administratora",
                newPin -> {
                    if (authManager != null) {
                        String[] err = new String[1];
                        authManager.updateUserPin(user.getUsername(), newPin, user, err);
                    }
                    Toast.makeText(activity, "Kod PIN został ustawiony!", Toast.LENGTH_SHORT).show();
                    action.onAllowed();
                });
    }
}
