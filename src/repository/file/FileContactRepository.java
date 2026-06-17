package repository.file;

import models.Contact;
import repository.ContactRepository;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی در حافظه
public class FileContactRepository implements ContactRepository {

    private final Map<String, Contact> store = new HashMap<>();

    private String key(String ownerId, String contactId) {
        return ownerId + ":" + contactId;
    }

    @Override
    public void save(Contact contact) {
        store.put(key(contact.getOwnerId(), contact.getContactId()), contact);
    }

    @Override
    public Optional<Contact> findByOwnerAndContact(String ownerId, String contactId) {
        return Optional.ofNullable(store.get(key(ownerId, contactId)));
    }

    @Override
    public List<Contact> findByOwnerId(String ownerId) {
        return store.values().stream()
                .filter(c -> c.getOwnerId().equals(ownerId))
                .collect(Collectors.toList());
    }

    @Override
    public void update(Contact contact) {
        store.put(key(contact.getOwnerId(), contact.getContactId()), contact);
    }

    @Override
    public void delete(String ownerId, String contactId) {
        store.remove(key(ownerId, contactId));
    }
}