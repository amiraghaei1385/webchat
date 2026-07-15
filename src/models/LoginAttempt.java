package models;

import java.time.LocalDateTime;

// مدل لاگین اتمپ
public class LoginAttempt {

    private static final int MAX_failattempt = 5;
    private static final int LOCK_durationmin = 15;
    private String iduser;
    private int failcount;
    private LocalDateTime lastattemptat;
    private LocalDateTime lockeduntil;

    public LoginAttempt() {
    }

    public LoginAttempt(String iduser) {
        this.iduser = iduser;
        this.failcount = 0;
        this.lastattemptat = LocalDateTime.now();
        this.lockeduntil = null;
    }

    public void recordFailure() {
        failcount++;
        lastattemptat = LocalDateTime.now();
        if (failcount >= MAX_failattempt) {
            lockeduntil = LocalDateTime.now().plusMinutes(LOCK_durationmin);
        }
    }

    public void recordSuccess() {
        failcount = 0;
        lockeduntil = null;
        lastattemptat = LocalDateTime.now();
    }

    public boolean isLocked() {
        return lockeduntil != null && LocalDateTime.now().isBefore(lockeduntil);
    }

    // Getters
    public String getUserId() {
        return iduser;
    }

    public int getFailedCount() {
        return failcount;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastattemptat;
    }

    public LocalDateTime getLockedUntil() {
        return lockeduntil;
    }

    // Setters
    public void setUserId(String iduser) {
        this.iduser = iduser;
    }

    public void setFailedCount(int count) {
        this.failcount = count;
    }

    public void setLastAttemptAt(LocalDateTime t) {
        this.lastattemptat = t;
    }

    public void setLockedUntil(LocalDateTime t) {
        this.lockeduntil = t;
    }

    @Override
    public String toString() {
        return "LoginAttempt{userId='" + iduser + "', failedCount=" + failcount + ", isLocked=" + isLocked() + "}";
    }
}