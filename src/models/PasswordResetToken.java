package models;

import java.time.LocalDateTime;

// مدل توکن برای ریست پس
public class PasswordResetToken {

    private String token;
    private String iduser;
    private String temppass;
    private LocalDateTime createat;
    private LocalDateTime expiresat;
    private boolean isused;

    public PasswordResetToken() {
    }

    public PasswordResetToken(String token, String iduser, String temppassword, LocalDateTime expiresat) {
        this.token = token;
        this.iduser = iduser;
        this.temppass = temppassword;
        this.createat = LocalDateTime.now();
        this.expiresat = expiresat;
        this.isused = false;
    }

    // ایا توکن منقضی شده
    public boolean isValid() {
        return !isused && LocalDateTime.now().isBefore(expiresat);
    }

    // Getters
    public String getToken() {
        return token;
    }

    public String getUserId() {
        return iduser;
    }

    public String getTempPassword() {
        return temppass;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    public LocalDateTime getExpiresAt() {
        return expiresat;
    }

    public boolean isUsed() {
        return isused;
    }

    // Setters
    public void setToken(String token) {
        this.token = token;
    }

    public void setUserId(String iduser) {
        this.iduser = iduser;
    }

    public void setTempPassword(String pwd) {
        this.temppass = pwd;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createat = t;
    }

    public void setExpiresAt(LocalDateTime t) {
        this.expiresat = t;
    }

    public void setUsed(boolean used) {
        this.isused = used;
    }

    @Override
    public String toString() {
        return "PasswordResetToken{userId='" + iduser + "', isUsed=" + isused + ", isValid=" + isValid() + "}";
    }
}