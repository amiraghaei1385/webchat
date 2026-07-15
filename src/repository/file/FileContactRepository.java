package repository.file;

import models.Contact;
import repository.ContactRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileContactRepository implements ContactRepository {
    private final Map<String, Contact> contacts = new ConcurrentHashMap<>();
    private final File fold = new File("storage/contacts");

    public FileContactRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    private String key(String ownerId, String contactId) {
        return ownerId + ":" + contactId;
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            Contact contact = readContactFromFile(file);
            if (contact != null) {
                contacts.put(key(contact.getOwnerId(), contact.getContactId()), contact);
            }
        }
    }

    // خواندن یکی
    private Contact readContactFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String idowner = reader.readLine();
            String idcontact = reader.readLine();
            String nickname = reader.readLine();
            String block = reader.readLine();
            String addedat = reader.readLine();
            String icontact = reader.readLine();
            reader.close();
            Contact contact = new Contact();
            contact.setOwnerId(fixEmpty(idowner));
            contact.setContactId(fixEmpty(idcontact));
            contact.setNickname(fixEmpty(nickname));
            contact.setBlocked(Boolean.parseBoolean(block));
            if (addedat != null && !addedat.equals("null")) {
                contact.setAddedAt(LocalDateTime.parse(addedat));
            }
            contact.setContact(Boolean.parseBoolean(icontact));
            return contact;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از خطای مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن
    private void saveFile(Contact contact) {
        File file = new File(fold,
                contact.getOwnerId() + "__" + contact.getContactId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(contact.getOwnerId()));
            writer.newLine();
            writer.write(safe(contact.getContactId()));
            writer.newLine();
            writer.write(safe(contact.getNickname()));
            writer.newLine();
            writer.write(String.valueOf(contact.isBlocked()));
            writer.newLine();
            if (contact.getAddedAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(safe(contact.getAddedAt().toString()));
            }
            writer.newLine();
            writer.write(String.valueOf(contact.isContact()));
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // مقدار خالی برای نوشتن
    private String safe(String value) {
        if (value == null) {
            return "null";
        }
        return value;
    }

    @Override
    public void save(Contact contact) {
        contacts.put(key(contact.getOwnerId(), contact.getContactId()), contact);
        saveFile(contact);
    }

    @Override
    public void update(Contact contact) {
        contacts.put(key(contact.getOwnerId(), contact.getContactId()), contact);
        saveFile(contact);
    }

    @Override
    public List<Contact> findByOwnerId(String ownerId) {
        List<Contact> res = new ArrayList<>();
        for (Contact contact : contacts.values()) {
            if (contact.getOwnerId().equals(ownerId)) {
                res.add(contact);
            }
        }
        return res;
    }

    @Override
    public Optional<Contact> findByOwnerAndContact(String ownerId, String contactId) {
        return Optional.ofNullable(contacts.get(key(ownerId, contactId)));
    }

    @Override
    public void delete(String ownerId, String contactId) {
        contacts.remove(key(ownerId, contactId));
        File file = new File(fold, ownerId + "__" + contactId + ".txt");
        if (file.exists()) {
            file.delete();
        }
    }
}