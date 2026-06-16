package models;

import java.time.LocalDateTime;

// Represents a single text message within a chat.
// Message content is stored encrypted in the database.

public class Message {

    private String id;
    private String chatId;
    private String senderId;
    private String encryptedContent; // content stored encrypted (see MessageEncryptor)
    private LocalDateTime sentAt;
    private LocalDateTime editedAt; // null if never edited
    private boolean isDeleted;
    private String replyToMessageId; // null if not a reply

    public Message() {
    }

    public Message(String id, String chatId, String senderId, String encryptedContent) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.encryptedContent = encryptedContent;
        this.sentAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    // Returns true if the message has been edited at least once.
    public boolean isEdited() {
        return editedAt != null;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getChatId() {
        return chatId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getEncryptedContent() {
        return encryptedContent;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setEncryptedContent(String content) {
        this.encryptedContent = content;
    }

    public void setSentAt(LocalDateTime t) {
        this.sentAt = t;
    }

    public void setEditedAt(LocalDateTime t) {
        this.editedAt = t;
    }

    public void setDeleted(boolean deleted) {
        this.isDeleted = deleted;
    }

    public void setReplyToMessageId(String id) {
        this.replyToMessageId = id;
    }

    @Override
    public String toString() {
        return "Message{id='" + id + "', chatId='" + chatId + "', senderId='" + senderId + "', isDeleted=" + isDeleted
                + "}";
    }
}