package models;

import java.time.LocalDateTime;

// Tracks failed login attempts for a user account.
// After 5 consecutive failures the account is temporarily locked.

public class LoginAttempt {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private String userId;
    private int failedCount; // number of consecutive failed attempts
    private LocalDateTime lastAttemptAt;
    private LocalDateTime lockedUntil; // null if not locked

    public LoginAttempt() {
    }

    public LoginAttempt(String userId) {
        this.userId = userId;
        this.failedCount = 0;
        this.lastAttemptAt = LocalDateTime.now();
        this.lockedUntil = null;
    }

    // Records a failed login. Locks the account if MAX_FAILED_ATTEMPTS is reached.
    public void recordFailure() {
        failedCount++;
        lastAttemptAt = LocalDateTime.now();
        if (failedCount >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
        }
    }

    // Resets the counter after a successful login.
    public void recordSuccess() {
        failedCount = 0;
        lockedUntil = null;
        lastAttemptAt = LocalDateTime.now();
    }

    // Returns true if the account is currently locked.
    public boolean isLocked() {
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setFailedCount(int count) {
        this.failedCount = count;
    }

    public void setLastAttemptAt(LocalDateTime t) {
        this.lastAttemptAt = t;
    }

    public void setLockedUntil(LocalDateTime t) {
        this.lockedUntil = t;
    }

    @Override
    public String toString() {
        return "LoginAttempt{userId='" + userId + "', failedCount=" + failedCount + ", isLocked=" + isLocked() + "}";
    }
}