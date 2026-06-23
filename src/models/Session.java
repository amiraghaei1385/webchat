package models;

import java.time.LocalDateTime;

// Represents an active login session for a user.
// Used for "Remember Me" functionality and session validation.

public class Session {

    private String sessionToken; // unique random token
    private String userId; // owner of this session
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean isActive;

    public Session() {
    }

    public Session(String sessionToken, String userId, LocalDateTime expiresAt) {
        this.sessionToken = sessionToken;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.isActive = true;
    }

    // Returns true if the session is still valid (not expired and active).
    public boolean isValid() {
        return isActive && LocalDateTime.now().isBefore(expiresAt);
    }

    // Getters
    public String getSessionToken() {
        return sessionToken;
    }

    public String getUserId() {
        return userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return isActive;
    }

    // Setters
    public void setSessionToken(String token) {
        this.sessionToken = token;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    public void setExpiresAt(LocalDateTime t) {
        this.expiresAt = t;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    @Override
    public String toString() {
        return "Session{userId='" + userId + "', expiresAt=" + expiresAt + ", isActive=" + isActive + "}";
    }
}