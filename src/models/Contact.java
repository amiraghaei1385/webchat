package models;

import java.time.LocalDateTime;

// مدل کانتکت
public class Contact {

    private String idowner;
    private String idcontact;
    private String nickname;
    private boolean isblocked;
    private LocalDateTime addat;
    private boolean iscontact;

    public Contact() {
    }

    public Contact(String idowner, String idcontact) {
        this.idowner = idowner;
        this.idcontact = idcontact;
        this.isblocked = false;
        this.iscontact = true;
        this.addat = LocalDateTime.now();
    }

    // Getters
    public String getOwnerId() {
        return idowner;
    }

    public String getContactId() {
        return idcontact;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isBlocked() {
        return isblocked;
    }

    public LocalDateTime getAddedAt() {
        return addat;
    }

    // Setters
    public void setOwnerId(String idowner) {
        this.idowner = idowner;
    }

    public void setContactId(String idcontact) {
        this.idcontact = idcontact;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setBlocked(boolean blocked) {
        this.isblocked = blocked;
    }

    public void setAddedAt(LocalDateTime t) {
        this.addat = t;
    }

    public boolean isContact() {
        return iscontact;
    }

    public void setContact(boolean iscontact) {
        this.iscontact = iscontact;
    }

    @Override
    public String toString() {
        return "Contact{ownerId='" + idowner + "', contactId='" + idcontact + "', isBlocked=" + isblocked + "}";
    }
}