package repository;

import models.MessageHistory;
import java.util.List;

public interface HistoryRepository {

    void save(MessageHistory history);

    List<MessageHistory> findByMessageId(String chatId, String messageId);

    List<MessageHistory> findByChatId(String chatId);

    int countByMessageId(String chatId, String messageId);

    void deleteByMessageId(String chatId, String messageId);
}
