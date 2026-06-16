package models;

import java.time.LocalDateTime;

// Represents a group chat's metadata (name, picture, owner, etc.).
// Member list is managed via GroupMember objects.

public class Group {

    private String id; // unique ID
    private String chatId; // linked Chat object ID
    private String name;
    private String description;
    private String picturePath; // path group avatar
    private String ownerId; // user who created the group
    private LocalDateTime createdAt;

    public Group() {
    }

    public Group(String id, String chatId, String name, String ownerId) {
        this.id = id;
        this.chatId = chatId;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPicturePath() {
        return picturePath;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public void setPicturePath(String path) {
        this.picturePath = path;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    @Override
    public String toString() {
        return "Group{id='" + id + "', name='" + name + "', ownerId='" + ownerId + "'}";
    }
}