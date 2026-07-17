package services;

import models.Contact;
import repository.ContactRepository;
import repository.UserRepository;
import java.util.*;

// مدیریت مخاطبین و بلاک کاربران
public class ContactService {

    private final ContactRepository contactrepo;
    private final UserRepository userrepo;

    public ContactService(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactrepo = contactRepository;
        this.userrepo = userRepository;
    }

    // ا مخاطب اضافه میشه
    public Contact addContact(String ownerId, String contactId) {
        if (ownerId.equals(contactId)) {
            throw new IllegalArgumentException("You cannot add yourself as a contact.");
        }
        if (userrepo.findById(contactId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        Optional<Contact> existing = contactrepo.findByOwnerAndContact(ownerId, contactId);
        if (existing.isPresent()) {
            Contact contact = existing.get();
            if (contact.isContact()) {
                throw new IllegalArgumentException("Contact already exists.");
            }
            contact.setContact(true);
            contactrepo.update(contact);
            return contact;
        }
        Contact contact = new Contact(ownerId, contactId);
        contactrepo.save(contact);
        return contact;
    }

    // کابر بلاک میشه
    public void blockUser(String ownerId, String targetId) {
        if (userrepo.findById(targetId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        if (ownerId.equals(targetId)) {
            throw new IllegalArgumentException("You cannot block yourself.");
        }
        Optional<Contact> existing = contactrepo.findByOwnerAndContact(ownerId, targetId);
        Contact contact;
        if (existing.isPresent()) {
            contact = existing.get();
        } else {
            contact = new Contact(ownerId, targetId);
            contact.setContact(false);
            contactrepo.save(contact);
        }
        contact.setBlocked(true);
        contactrepo.update(contact);
    }

    // دریافت لیست کاربران بلاک شده
    public List<Contact> getBlockedUsers(String ownerId) {
        List<Contact> all = contactrepo.findByOwnerId(ownerId);
        List<Contact> result = new ArrayList<>();
        for (Contact c : all) {
            if (c.isBlocked()) {
                result.add(c);
            }
        }
        return result;
    }

    // دریافت لیست مخاطبین واقعی
    public List<Contact> getContacts(String ownerId) {
        List<Contact> all = contactrepo.findByOwnerId(ownerId);
        List<Contact> result = new ArrayList<>();
        for (Contact c : all) {
            if (c.isContact()) {
                result.add(c);
            }
        }
        return result;
    }

    public Optional<Contact> findContact(String ownerId, String contactId) {
        return contactrepo.findByOwnerAndContact(ownerId, contactId);
    }

    // حذف مخاطب
    public void removeContact(String ownerId, String contactId) {
        Optional<Contact> found = contactrepo.findByOwnerAndContact(ownerId, contactId);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Contact not found.");
        }
        Contact contact = found.get();
        if (contact.isBlocked()) {
            contact.setContact(false);
            contactrepo.update(contact);
        } else {
            contactrepo.delete(ownerId, contactId);
        }
    }

    // کاربر انبلاک میشه
    public void unblockUser(String ownerId, String targetId) {
        Optional<Contact> found = contactrepo.findByOwnerAndContact(ownerId, targetId);
        if (found.isEmpty()) {
            return;
        }
        Contact contact = found.get();
        contact.setBlocked(false);
        if (!contact.isContact()) {
            contactrepo.delete(ownerId, targetId);
        } else {
            contactrepo.update(contact);
        }
    }

    public boolean isBlocked(String ownerId, String targetId) {
        Optional<Contact> found = contactrepo.findByOwnerAndContact(ownerId, targetId);
        if (found.isEmpty()) {
            return false;
        }
        return found.get().isBlocked();
    }
}