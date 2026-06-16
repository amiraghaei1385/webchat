package models;

import java.time.LocalDateTime;

// Represents a contact relationship between two users.
// Also tracks whether the owner has blocked the contact.

public class Contact {

    private String ownerId; // the user who added this contact
    private String contactId; // the user who was added
    private String nickname; // optional custom name set by the owner
    private boolean isBlocked;
    private LocalDateTime addedAt;

    public Contact() {
    }

    public Contact(String ownerId, String contactId) {
        this.ownerId = ownerId;
        this.contactId = contactId;
        this.isBlocked = false;
        this.addedAt = LocalDateTime.now();
    }

    // Getters
    public String getOwnerId() {
        return ownerId;
    }

    public String getContactId() {
        return contactId;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    // Setters
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setBlocked(boolean blocked) {
        this.isBlocked = blocked;
    }

    public void setAddedAt(LocalDateTime t) {
        this.addedAt = t;
    }

    @Override
    public String toString() {
        return "Contact{ownerId='" + ownerId + "', contactId='" + contactId + "', isBlocked=" + isBlocked + "}";
    }
}