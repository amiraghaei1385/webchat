package repository;

import models.Reaction;
import java.util.List;
import java.util.Optional;

// قرارداد مربوط به ذخیره‌سازی و بازیابی ری‌اکشن‌های (ایموجی) پیام‌ها
public interface ReactionRepository {

    void save(Reaction reaction);

    // یافتن ری‌اکشن فعلی یک کاربر خاص روی یک پیام خاص (هر کاربر حداکثر یک ری‌اکشن فعال روی هر پیام دارد)
    Optional<Reaction> findByMessageAndUser(String messageId, String userId);

    // تمام ری‌اکشن‌های ثبت‌شده روی یک پیام (برای نمایش شمارش هر ایموجی)
    List<Reaction> findByMessageId(String messageId);

    void update(Reaction reaction);

    void delete(String messageId, String userId);

    // حذف تمام ری‌اکشن‌های یک پیام (مثلاً هنگام حذف کامل پیام)
    void deleteByMessageId(String messageId);
}
