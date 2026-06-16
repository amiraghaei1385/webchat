package repository;

import models.Session;
import java.util.List;
import java.util.Optional;

// قرارداد مربوط به تمام عملیات ذخیره‌سازی و بازیابی نشست‌ها (Session)
public interface SessionRepository {

    void save(Session session);

    Optional<Session> findByToken(String token);

    List<Session> findByUserId(String userId);

    void update(Session session);

    void deleteByToken(String token);

    void deleteAllByUserId(String userId);
}