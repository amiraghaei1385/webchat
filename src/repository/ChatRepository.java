package repository;
 
import models.Chat;
import java.util.List;
import java.util.Optional;
 
// قرارداد مربوط به تمام عملیات ذخیره‌سازی و بازیابی چت‌ها
public interface ChatRepository {
 
    void save(Chat chat);
 
    Optional<Chat> findById(String id);
 
    // برگرداندن تمام چت‌های کاربر به ترتیب آخرین پیام (جدیدترین به قدیمی‌ترین)
    List<Chat> findByUserId(String userId);
 
    void update(Chat chat);
 
    void delete(String id);
}
