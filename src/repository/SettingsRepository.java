package repository;

import models.UserSettings;
import java.util.Optional;

// قرارداد مربوط به ذخیره‌سازی و بازیابی تنظیمات کاربران
public interface SettingsRepository {

    void save(UserSettings settings);

    Optional<UserSettings> findByUserId(String userId);

    void update(UserSettings settings);
}