package repository;
 
import models.Contact;
import java.util.List;
import java.util.Optional;
 
// قرارداد مربوط به تمام عملیات ذخیره‌سازی و بازیابی مخاطبین
public interface ContactRepository {
 
    void save(Contact contact);
 
    Optional<Contact> findByOwnerAndContact(String ownerId, String contactId);
 
    List<Contact> findByOwnerId(String ownerId);
 
    void update(Contact contact);
 
    void delete(String ownerId, String contactId);
}