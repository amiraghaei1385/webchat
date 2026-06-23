package repository;
 
import models.Message;
import java.util.List;
import java.util.Optional;
 
// قرارداد مربوط به تمام عملیات ذخیره‌سازی و بازیابی پیام‌ها
public interface MessageRepository {
 
    void save(Message message);
 
    Optional<Message> findById(String id);
 
    // برگرداندن پیام‌های چت به ترتیب زمان ارسال (قدیمی‌ترین به جدیدترین)
    List<Message> findByChatId(String chatId);
 
    void update(Message message);
 
    void delete(String id);
}