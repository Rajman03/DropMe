package com.example.testapp.core;

import android.os.SystemClock;

public class LoginThrottle {

    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_LOCK_MS = 30_000L; // 30 seconds
    private static final long MAX_LOCK_MS = 15 * 60_000L; // 15 minutes

    private int failedAttempts = 0;
    private long lockedUntilElapsed = 0L;
    private long currentLockDurationMs = INITIAL_LOCK_MS;

    public synchronized boolean isLocked() {
        long now = SystemClock.elapsedRealtime();
        if (now < lockedUntilElapsed) {
            return true;
        }
        return false;
    }

    public synchronized long getRemainingSeconds() {
        long now = SystemClock.elapsedRealtime();
        if (now < lockedUntilElapsed) {
            return (lockedUntilElapsed - now) / 1000L;
        }
        return 0L;
    }

    public synchronized void recordFailure() {
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            lockedUntilElapsed = SystemClock.elapsedRealtime() + currentLockDurationMs;
            // Double lock duration on repeated lockouts up to 15 mins
            currentLockDurationMs = Math.min(currentLockDurationMs * 2, MAX_LOCK_MS);
        }
    }

    public synchronized void recordSuccess() {
        failedAttempts = 0;
        lockedUntilElapsed = 0L;
        currentLockDurationMs = INITIAL_LOCK_MS;
    }

    public synchronized int getFailedAttempts() {
        return failedAttempts;
    }
}
