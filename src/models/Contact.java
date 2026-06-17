package models;

import java.time.LocalDateTime;

// Represents a contact relationship between two users.
// Also tracks whether the owner has blocked the contact.

public class Contact {

    private String ownerId; // the user who added this contact
    private String contactId; // the user who was added
    private String nickname; // optional custom name set by the owner
    private boolean isBlocked;
    // true اگر کاربر واقعاً به مخاطبین اضافه شده باشد.
    // false یعنی این رکورد صرفاً برای نگه‌داشتن وضعیت بلاک یک "غریبه" ساخته شده
    // (کاربری که contact نیست ولی بلاک شده) و نباید در لیست مخاطبین نمایش داده شود.
    private boolean isContact;
    private LocalDateTime addedAt;

    public Contact() {
    }

    public Contact(String ownerId, String contactId) {
        this.ownerId = ownerId;
        this.contactId = contactId;
        this.isBlocked = false;
        this.isContact = true;
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

    public boolean isContact() {
        return isContact;
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

    public void setContact(boolean contact) {
        this.isContact = contact;
    }

    public void setAddedAt(LocalDateTime t) {
        this.addedAt = t;
    }

    @Override
    public String toString() {
        return "Contact{ownerId='" + ownerId + "', contactId='" + contactId
                + "', isContact=" + isContact + ", isBlocked=" + isBlocked + "}";
    }
}