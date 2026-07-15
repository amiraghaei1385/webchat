package repository;

import models.MessageHistory;
import java.util.List;

public interface HistoryRepository {

    void save(MessageHistory history);

    // برگرداندن تمام نسخه‌های تاریخچه یک پیامبه ترتیب نسخه
    List<MessageHistory> findByMessageId(String chatId, String messageId);

    int countByMessageId(String chatId, String messageId);

    void deleteByMessageId(String chatId, String messageId);
}
