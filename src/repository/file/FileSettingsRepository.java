package repository.file;

import models.UserSettings;
import repository.SettingsRepository;
import utils.FileUtil;
import utils.JsonUtil;
import utils.PathUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// ذخیره‌سازی فایل‌محور تنظیمات کاربران؛ هر کاربر یک فایل storage/settings/{userId}.txt دارد
public class FileSettingsRepository implements SettingsRepository {

    private final Map<String, UserSettings> store = new ConcurrentHashMap<>();

    public FileSettingsRepository() {
        loadAll();
    }

    private void loadAll() {
        List<String> contents = FileUtil.readAllInDirectory(PathUtil.settingsDir());
        for (String json : contents) {
            UserSettings settings = JsonUtil.fromJson(json, UserSettings.class);
            if (settings != null && settings.getUserId() != null) {
                store.put(settings.getUserId(), settings);
            }
        }
    }

    private void persist(UserSettings settings) {
        Path path = PathUtil.settingsFile(settings.getUserId());
        FileUtil.writeAtomic(path, JsonUtil.toJson(settings));
    }

    @Override
    public void save(UserSettings settings) {
        store.put(settings.getUserId(), settings);
        persist(settings);
    }

    @Override
    public Optional<UserSettings> findByUserId(String userId) {
        return Optional.ofNullable(store.get(userId));
    }

    @Override
    public void update(UserSettings settings) {
        store.put(settings.getUserId(), settings);
        persist(settings);
    }
}