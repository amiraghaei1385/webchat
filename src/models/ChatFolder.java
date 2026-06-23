package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Represents a custom folder that groups multiple chats together.
// (Bonus feature - Phase 1 skeleton only)

public class ChatFolder {

    private String id;
    private String ownerId; // user who created this folder
    private String name; 
    private List<String> chatIds; // chats inside this folder
    private int orderIndex; // display order among folders
    private LocalDateTime createdAt;

    public ChatFolder() {
        this.chatIds = new ArrayList<>();
    }

    public ChatFolder(String id, String ownerId, String name) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.chatIds = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public List<String> getChatIds() {
        return chatIds;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setChatIds(List<String> ids) {
        this.chatIds = ids;
    }

    public void setOrderIndex(int index) {
        this.orderIndex = index;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    @Override
    public String toString() {
        return "ChatFolder{id='" + id + "', name='" + name + "', chats=" + chatIds.size() + "}";
    }
}