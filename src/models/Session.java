package models;

import java.time.LocalDateTime;

// مدل نشست
public class Session {

    private String sessiontoken;
    private String iduser;
    private LocalDateTime createat;
    private LocalDateTime expiresat;
    private boolean isactive;

    public Session() {
    }

    public Session(String sessiontoken, String iduser, LocalDateTime expiresat) {
        this.sessiontoken = sessiontoken;
        this.iduser = iduser;
        this.createat = LocalDateTime.now();
        this.expiresat = expiresat;
        this.isactive = true;
    }

    //بررسی فعال بودن نشست
    public boolean isValid() {
        return isactive && LocalDateTime.now().isBefore(expiresat);
    }

    // Getters
    public String getSessionToken() {
        return sessiontoken;
    }

    public String getUserId() {
        return iduser;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    public LocalDateTime getExpiresAt() {
        return expiresat;
    }

    public boolean isActive() {
        return isactive;
    }

    // Setters
    public void setSessionToken(String token) {
        this.sessiontoken = token;
    }

    public void setUserId(String iduser) {
        this.iduser = iduser;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createat = t;
    }

    public void setExpiresAt(LocalDateTime t) {
        this.expiresat = t;
    }

    public void setActive(boolean active) {
        this.isactive = active;
    }

    @Override
    public String toString() {
        return "Session{userId='" + iduser + "', expiresAt=" + expiresat + ", isActive=" + isactive + "}";
    }
}