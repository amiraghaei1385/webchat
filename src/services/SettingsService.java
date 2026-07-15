package services;

import models.UserSettings;
import repository.SettingsRepository;
import repository.UserRepository;
import java.util.Optional;

// مدیریت تنظیمات کاربر
public class SettingsService {

    private final UserRepository userrepo;
    private final SettingsRepository settingsrepo;

    public SettingsService(UserRepository userRepository, SettingsRepository settingsRepository) {
        this.userrepo = userRepository;
        this.settingsrepo = settingsRepository;
    }

    // تغییر حالت به عکس فعلی
    public UserSettings toggleDarkMode(String userId) {
        UserSettings settings = getSettings(userId);
        settings.setDarkMode(!settings.isDarkMode());
        settingsrepo.update(settings);
        return settings;
    }

    // حالت دارک مود
    public UserSettings setDarkMode(String userId, boolean darkMode) {
        UserSettings settings = getSettings(userId);
        settings.setDarkMode(darkMode);
        settingsrepo.update(settings);
        return settings;
    }

    // دریافت تنظیمات کاربر
    public UserSettings getSettings(String userId) {
        if (userrepo.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found.");
        }
        Optional<UserSettings> existing = settingsrepo.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // تنظیمات پیش‌فرض ساخته میشه
        UserSettings def = new UserSettings(userId);
        settingsrepo.save(def);
        return def;
    }
}