package repository;

import models.Reaction;
import java.util.List;
import java.util.Optional;

public interface ReactionRepository {

    void save(Reaction reaction);

    // یافتن ری‌اکشن فعلی یک کاربر خاص روی یک پیام خاص 
    Optional<Reaction> findByMessageAndUser(String messageId, String userId);

    // تمام ری‌اکشن‌های ثبت‌شده روی یک پیام 
    List<Reaction> findByMessageId(String messageId);

    void update(Reaction reaction);

    void delete(String messageId, String userId);

    // حذف تمام ری‌اکشن‌های یک پیام
    void deleteByMessageId(String messageId);
}
