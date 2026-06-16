package models;

import java.time.LocalDateTime;
// Represents a registered user in the system.
public class User {

    private String id; // unique ID
    private String username; // display name
    private String passwordHash; // bcrypt algo
    private String profilePicPath; // path profile
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime lastSeenAt;
    private boolean isOnline;
    private boolean isDeleted;

    public User() {
    }

    public User(String id, String username, String passwordHash) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.createdAt = LocalDateTime.now();
        this.lastSeenAt = LocalDateTime.now();
        this.isOnline = false;
        this.isDeleted = false;
    }
    // Getters
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getProfilePicPath() {
        return profilePicPath;
    }

    public String getBio() {
        return bio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setProfilePicPath(String path) {
        this.profilePicPath = path;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    public void setLastSeenAt(LocalDateTime t) {
        this.lastSeenAt = t;
    }

    public void setOnline(boolean online) {
        this.isOnline = online;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    @Override
    public String toString() {
        return "User{id='" + id + "', username='" + username + "', isOnline=" + isOnline + "}";
    }
}