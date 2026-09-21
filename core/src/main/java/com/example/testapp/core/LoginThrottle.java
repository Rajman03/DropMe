package com.example.testapp.core;

import android.content.SharedPreferences;
import android.os.SystemClock;

/**
 * Blokada logowania po nieudanych próbach.
 * Wersja trwała: licznik i moment blokady zapisywane w SharedPreferences,
 * więc restart aplikacji NIE zeruje blokady (wcześniej blokada żyła tylko
 * w pamięci procesu i była omijana przez restart aplikacji).
 * Trzymane są wyłącznie wartości liczbowe - żadnych danych logowania.
 */
public class LoginThrottle {

    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_LOCK_MS = 30_000L; // 30 seconds
    private static final long MAX_LOCK_MS = 15 * 60_000L; // 15 minutes

    private static final String KEY_FAILED = "failed_attempts";
    private static final String KEY_LOCKED_UNTIL = "locked_until_elapsed";
    private static final String KEY_LOCK_DURATION = "current_lock_duration";

    private final SharedPreferences prefs;

    /** Nowe API: trwały throttle wsparty SharedPreferences (wymagany Context). */
    public LoginThrottle(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /** Stare API (bez pamięci trwałej) - zachowane dla kompatybilności/testów. */
    public LoginThrottle() {
        this.prefs = null;
    }

    private long getLockedUntil() {
        if (prefs == null) return lockedUntilElapsed;
        return prefs.getLong(KEY_LOCKED_UNTIL, 0L);
    }

    private void setLockedUntil(long value) {
        if (prefs == null) {
            lockedUntilElapsed = value;
            return;
        }
        prefs.edit().putLong(KEY_LOCKED_UNTIL, value).apply();
    }

    private int getFailed() {
        if (prefs == null) return failedAttempts;
        return prefs.getInt(KEY_FAILED, 0);
    }

    private void setFailed(int value) {
        if (prefs == null) {
            failedAttempts = value;
            return;
        }
        prefs.edit().putInt(KEY_FAILED, value).apply();
    }

    private long getLockDuration() {
        if (prefs == null) return currentLockDurationMs;
        return prefs.getLong(KEY_LOCK_DURATION, INITIAL_LOCK_MS);
    }

    private void setLockDuration(long value) {
        if (prefs == null) {
            currentLockDurationMs = value;
            return;
        }
        prefs.edit().putLong(KEY_LOCK_DURATION, value).apply();
    }

    public synchronized boolean isLocked() {
        long now = SystemClock.elapsedRealtime();
        return now < getLockedUntil();
    }

    public synchronized long getRemainingSeconds() {
        long now = SystemClock.elapsedRealtime();
        long until = getLockedUntil();
        if (now < until) {
            return (until - now) / 1000L;
        }
        return 0L;
    }

    public synchronized void recordFailure() {
        int attempts = getFailed() + 1;
        setFailed(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            setLockedUntil(SystemClock.elapsedRealtime() + getLockDuration());
            // Double lock duration on repeated lockouts up to 15 mins
            setLockDuration(Math.min(getLockDuration() * 2, MAX_LOCK_MS));
            setFailed(0);
        }
    }

    public synchronized void recordSuccess() {
        setFailed(0);
        setLockedUntil(0L);
        setLockDuration(INITIAL_LOCK_MS);
    }

    public synchronized int getFailedAttempts() {
        return getFailed();
    }

    // Pola tylko dla trybu bez SharedPreferences (stare API)
    private int failedAttempts = 0;
    private long lockedUntilElapsed = 0L;
    private long currentLockDurationMs = INITIAL_LOCK_MS;
}
