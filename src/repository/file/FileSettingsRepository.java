package repository.file;

import models.UserSettings;
import repository.SettingsRepository;
import java.util.*;

// ذخیره‌سازی تنظیمات کاربران در حافظه (فاز ۱)
// در فاز ۲ این کلاس با نسخه‌ی مبتنی بر فایل متنی جایگزین یا تکمیل می‌شود
public class FileSettingsRepository implements SettingsRepository {

    // کلید: userId -> مقدار: تنظیمات همان کاربر
    private final Map<String, UserSettings> store = new HashMap<>();

    @Override
    public void save(UserSettings settings) {
        store.put(settings.getUserId(), settings);
    }

    @Override
    public Optional<UserSettings> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public void update(UserSettings settings) {
        store.put(settings.getUserId(), settings);
    }
}
