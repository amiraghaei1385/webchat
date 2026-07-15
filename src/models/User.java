package models;

import java.time.LocalDateTime;
import java.time.LocalDate;

// مدل کاربر
public class User {

    private String id;
    private String username;
    private String passhash;
    private String profilepicpath;
    private String bio;
    private LocalDateTime createat;
    private LocalDateTime lastseenat;
    private boolean isonline;
    private boolean isdeleted;
    private String backgroundpicpath;
    private String email;
    private LocalDate birthdate;

    public User() {
    }

    public User(String id, String username, String passwordhash) {
        this.id = id;
        this.username = username;
        this.passhash = passwordhash;
        this.createat = LocalDateTime.now();
        this.lastseenat = LocalDateTime.now();
        this.isonline = false;
        this.isdeleted = false;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passhash;
    }

    public String getProfilePicPath() {
        return profilepicpath;
    }

    public String getBio() {
        return bio;
    }

    public LocalDateTime getCreatedAt() {
        return createat;
    }

    public LocalDateTime getLastSeenAt() {
        return lastseenat;
    }

    public boolean isOnline() {
        return isonline;
    }

    public boolean isDeleted() {
        return isdeleted;
    }

    public String getBackgroundPicPath() {
        return backgroundpicpath;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthdate;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordhash) {
        this.passhash = passwordhash;
    }

    public void setProfilePicPath(String path) {
        this.profilepicpath = path;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createat = t;
    }

    public void setLastSeenAt(LocalDateTime t) {
        this.lastseenat = t;
    }

    public void setOnline(boolean online) {
        this.isonline = online;
    }

    public void setDeleted(boolean deleted) {
        this.isdeleted = deleted;
    }

    public void setBackgroundPicPath(String backgroundpicpath) {
        this.backgroundpicpath = backgroundpicpath;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setBirthDate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', isOnline=" + isonline + "}";
    }
}