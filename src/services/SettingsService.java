package services;

import models.User;
import models.UserSettings;
import repository.SettingsRepository;
import repository.UserRepository;
import java.util.Optional;

// مدیریت تنظیمات رابط کاربری (مانند حالت شب/روز).
public class SettingsService {

    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;

    public SettingsService(UserRepository userRepository, SettingsRepository settingsRepository) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
    }

    // دریافت تنظیمات کاربر //

    /**
     * دریافت تنظیمات کاربر. اگر تنظیماتی برای کاربر ثبت نشده باشد،
     * یک نمونه‌ی پیش‌فرض ساخته و ذخیره می‌شود.
     */
    public UserSettings getSettings(String userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }

        Optional<UserSettings> existing = settingsRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // ساخت تنظیمات پیش‌فرض برای کاربری که تا الان تنظیماتش ذخیره نشده
        UserSettings defaults = new UserSettings(userId);
        settingsRepository.save(defaults);
        return defaults;
    }

    // تغییر حالت شب/روز //

    /**
     * تغییر حالت نمایش (شب/روز) برای یک کاربر.
     */
    public UserSettings setDarkMode(String userId, boolean darkMode) {
        UserSettings settings = getSettings(userId);
        settings.setDarkMode(darkMode);
        settingsRepository.update(settings);
        return settings;
    }

    /**
     * تغییر وضعیت حالت شب/روز به حالت مخالف وضعیت فعلی.
     */
    public UserSettings toggleDarkMode(String userId) {
        UserSettings settings = getSettings(userId);
        settings.setDarkMode(!settings.isDarkMode());
        settingsRepository.update(settings);
        return settings;
    }
}