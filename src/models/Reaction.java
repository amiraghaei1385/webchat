package models;

import java.time.LocalDateTime;

// Represents an emoji reaction placed by a user on a message.
// (Bonus feature - Phase 1 skeleton only)

public class Reaction {

    private String id;
    private String messageId;
    private String userId;
    private String emoji;
    private LocalDateTime reactedAt;

    public Reaction() {
    }

    public Reaction(String id, String messageId, String userId, String emoji) {
        this.id = id;
        this.messageId = messageId;
        this.userId = userId;
        this.emoji = emoji;
        this.reactedAt = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmoji() {
        return emoji;
    }

    public LocalDateTime getReactedAt() {
        return reactedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setReactedAt(LocalDateTime t) {
        this.reactedAt = t;
    }

    @Override
    public String toString() {
        return "Reaction{messageId='" + messageId + "', userId='" + userId + "', emoji='" + emoji + "'}";
    }
}