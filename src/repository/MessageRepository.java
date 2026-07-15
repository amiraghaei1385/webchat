package repository;
 
import models.Message;
import java.util.List;
import java.util.Optional;

public interface MessageRepository {
 
    void save(Message message);
 
    Optional<Message> findById(String id);
 
    // برگرداندن پیام‌های چت به ترتیب زمان ارسال 
    List<Message> findByChatId(String chatId);
 
    void update(Message message);
 
    void delete(String id);
}