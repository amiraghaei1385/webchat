package services;

import models.Contact;
import repository.ContactRepository;
import repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// مدیریت مخاطبین و بلاک کاربران.
public class ContactService {

    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public ContactService(ContactRepository contactRepository, UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    // افزودن مخاطب
    public Contact addContact(String ownerId, String contactId) {
        if (ownerId.equals(contactId)) {
            throw new IllegalArgumentException("You cannot add yourself as a contact.");
        }
        if (userRepository.findById(contactId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        Optional<Contact> existing = contactRepository.findByOwnerAndContact(ownerId, contactId);

        // اگر قبلاً بلاک شده بود، فقط isContact را true کن
        if (existing.isPresent()) {
            Contact contact = existing.get();
            if (contact.isContact()) {
                throw new IllegalArgumentException("Contact already exists.");
            }
            contact.setContact(true);
            contactRepository.update(contact);
            return contact;
        }

        Contact contact = new Contact(ownerId, contactId);
        contactRepository.save(contact);
        return contact;
    }

    // دریافت لیست مخاطبین واقعی (بدون غریبه‌های بلاک‌شده)
    public List<Contact> getContacts(String ownerId) {
        return contactRepository.findByOwnerId(ownerId).stream()
                .filter(Contact::isContact)
                .collect(Collectors.toList());
    }

    public Optional<Contact> findContact(String ownerId, String contactId) {
        return contactRepository.findByOwnerAndContact(ownerId, contactId);
    }

    // بلاک کردن کاربر
    public void blockUser(String ownerId, String targetId) {
        if (ownerId.equals(targetId)) {
            throw new IllegalArgumentException("You cannot block yourself.");
        }
        if (userRepository.findById(targetId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        Contact contact = contactRepository.findByOwnerAndContact(ownerId, targetId)
                .orElseGet(() -> {
                    // غریبه است؛ isContact=false تا در لیست مخاطبین نیاید
                    Contact newContact = new Contact(ownerId, targetId);
                    newContact.setContact(false);
                    contactRepository.save(newContact);
                    return newContact;
                });

        contact.setBlocked(true);
        contactRepository.update(contact);
    }

    // آنبلاک کردن کاربر
    public void unblockUser(String ownerId, String targetId) {
        contactRepository.findByOwnerAndContact(ownerId, targetId).ifPresent(contact -> {
            contact.setBlocked(false);
            // اگر غریبه بود، ردیف رو کلاً پاک کن
            if (!contact.isContact()) {
                contactRepository.delete(ownerId, targetId);
            } else {
                contactRepository.update(contact);
            }
        });
    }

    public boolean isBlocked(String ownerId, String targetId) {
        return contactRepository.findByOwnerAndContact(ownerId, targetId)
                .map(Contact::isBlocked)
                .orElse(false);
    }

    // حذف مخاطب
    public void removeContact(String ownerId, String contactId) {
        Contact contact = contactRepository.findByOwnerAndContact(ownerId, contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found."));

        if (contact.isBlocked()) {
            // بلاک را نگه دار، فقط از مخاطبین حذف کن
            contact.setContact(false);
            contactRepository.update(contact);
        } else {
            contactRepository.delete(ownerId, contactId);
        }
    }
}