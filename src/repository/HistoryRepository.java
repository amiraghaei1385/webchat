package repository;

import models.MessageHistory;
import java.util.List;

// قرارداد مربوط به ذخیره‌سازی و بازیابی تاریخچه‌ی ویرایش پیام‌ها
public interface HistoryRepository {

    void save(MessageHistory history);

    // برگرداندن تمام نسخه‌های تاریخچه یک پیام، به ترتیب نسخه (قدیمی به جدید)
    List<MessageHistory> findByMessageId(String chatId, String messageId);

    // تعداد نسخه‌های ثبت‌شده برای یک پیام (برای تعیین شماره نسخه بعدی)
    int countByMessageId(String chatId, String messageId);

    // حذف تمام تاریخچه‌ی یک پیام (مثلاً هنگام حذف کامل پیام)
    void deleteByMessageId(String chatId, String messageId);
}
