package repository;

import models.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    void save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);

    void update(PasswordResetToken token);

    void delete(String token);
 
    // حذف تمام توکن‌های قبلی یک کاربر
    void deleteAllByUserId(String userId);
}
