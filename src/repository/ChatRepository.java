package repository;
 
import models.Chat;
import java.util.List;
import java.util.Optional;
 
public interface ChatRepository {
 
    void save(Chat chat);
 
    Optional<Chat> findById(String id);
 
    // برگرداندن تمام چت‌های کاربر به ترتیب آخرین پیام 
    List<Chat> findByUserId(String userId);
 
    void update(Chat chat);
 
    void delete(String id);
}
