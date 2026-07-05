package repository.file;

import models.Contact;
import repository.ContactRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

// ذخیره‌سازی فایل‌محور مخاطبین؛ هر رکورد یک فایل storage/contacts/{ownerId}__{contactId}.txt دارد
public class FileContactRepository implements ContactRepository {

    private final Map<String, Contact> store = new HashMap<>();

    public FileContactRepository() {
        loadAll();
    }

    private String key(String ownerId, String contactId) {
        return ownerId + ":" + contactId;
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.contactsDir());
        for (String json : contents) {
            Contact contact = JsonUtil.fromJson(json, Contact.class);
            if (contact != null && contact.getOwnerId() != null && contact.getContactId() != null) {
                store.put(key(contact.getOwnerId(), contact.getContactId()), contact);
            }
        }
    }

    private void persist(Contact contact) {
        Path path = PathUtil.contactFile(contact.getOwnerId(), contact.getContactId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(contact));
    }

    @Override
    public void save(Contact contact) {
        store.put(key(contact.getOwnerId(), contact.getContactId()), contact);
        persist(contact);
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
        persist(contact);
    }

    @Override
    public void delete(String ownerId, String contactId) {
        store.remove(key(ownerId, contactId));
        FileUtil.delete(PathUtil.contactFile(ownerId, contactId));
    }
}