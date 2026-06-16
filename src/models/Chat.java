package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//Represents a chat conversation (private, group, or saved messages).

public class Chat {
    private String id;
    private ChatType type;
    private List<String> memberIds; // user IDs
    private String lastMessageId; // ID of the most recent message
    private LocalDateTime lastMessageAt;
    private boolean isArchived;
    private boolean isPinned;
    private LocalDateTime createdAt;

    public Chat() {
        this.memberIds = new ArrayList<>();
    }

    public Chat(String id, ChatType type) {
        this.id = id;
        this.type = type;
        this.memberIds = new ArrayList<>();
        this.isArchived = false;
        this.isPinned = false;
        this.createdAt = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public ChatType getType() {
        return type;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public String getLastMessageId() {
        return lastMessageId;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public void setMemberIds(List<String> ids) {
        this.memberIds = ids;
    }

    public void setLastMessageId(String id) {
        this.lastMessageId = id;
    }

    public void setLastMessageAt(LocalDateTime t) {
        this.lastMessageAt = t;
    }

    public void setArchived(boolean archived) {
        this.isArchived = archived;
    }

    public void setPinned(boolean pinned) {
        this.isPinned = pinned;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }

    // Adds a member to the chat if not already present.
    public void addMember(String userId) {
        if (!memberIds.contains(userId)) {
            memberIds.add(userId);
        }
    }

    // Removes a member from the chat.
    public void removeMember(String userId) {
        memberIds.remove(userId);
    }

    @Override
    public String toString() {
        return "Chat{id='" + id + "', type=" + type + ", members=" + memberIds.size() + "}";
    }
}