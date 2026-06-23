package models;

import java.time.LocalDateTime;

// Represents a temporary token used for password recovery.
// A random password is generated server-side and linked to this token.
// (Bonus feature - Phase 1 skeleton only)

public class PasswordResetToken {

    private String token; // random secure token
    private String userId;
    private String tempPassword; // randomly generated temporary password (hashed)
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isUsed;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String token, String userId, String tempPassword, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.tempPassword = tempPassword;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.isUsed = false;
    }

    // Returns true if the token can still be used.
    public boolean isValid() {
        return !isUsed && LocalDateTime.now().isBefore(expiresAt);
    }

    // Getters
    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getTempPassword() {
        return tempPassword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return isUsed;
    }

    // Setters
    public void setToken(String token) {
        this.token = token;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTempPassword(String pwd) {
        this.tempPassword = pwd;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    public void setExpiresAt(LocalDateTime t) {
        this.expiresAt = t;
    }

    public void setUsed(boolean used) {
        this.isUsed = used;
    }

    @Override
    public String toString() {
        return "PasswordResetToken{userId='" + userId + "', isUsed=" + isUsed + ", isValid=" + isValid() + "}";
    }
}