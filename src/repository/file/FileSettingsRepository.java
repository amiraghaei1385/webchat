package repository.file;

import models.UserSettings;
import repository.SettingsRepository;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileSettingsRepository implements SettingsRepository {
    private final Map<String, UserSettings> settings = new ConcurrentHashMap<>();
    private final File fold = new File("storage/settings");

    public FileSettingsRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            UserSettings userSettings = readSettingsFromFile(file);
            if (userSettings != null) {
                settings.put(userSettings.getUserId(), userSettings);
            }
        }
    }

    // خواندن یکی
    private UserSettings readSettingsFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String iduser = reader.readLine();
            String darkmode = reader.readLine();
            String backgroundpath = reader.readLine();
            String notifenable = reader.readLine();
            String notifpreviewenable = reader.readLine();
            String language = reader.readLine();
            reader.close();
            UserSettings settings = new UserSettings();
            settings.setUserId(fixEmpty(iduser));
            settings.setDarkMode(Boolean.parseBoolean(darkmode));
            settings.setBackgroundImagePath(fixEmpty(backgroundpath));
            settings.setNotificationsEnabled(Boolean.parseBoolean(notifenable));
            settings.setNotificationPreviewEnabled(Boolean.parseBoolean(notifpreviewenable));
            settings.setLanguage(fixEmpty(language));
            return settings;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از خطای مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن
    private void saveFile(UserSettings settings) {
        File file = new File(fold, settings.getUserId() + ".txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(settings.getUserId()));
            writer.newLine();
            writer.write(String.valueOf(settings.isDarkMode()));
            writer.newLine();
            writer.write(safe(settings.getBackgroundImagePath()));
            writer.newLine();
            writer.write(String.valueOf(settings.isNotificationsEnabled()));
            writer.newLine();
            writer.write(String.valueOf(settings.isNotificationPreviewEnabled()));
            writer.newLine();
            writer.write(safe(settings.getLanguage()));
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // مقدار خالی برای نوشتن
    private String safe(String value) {
        if (value == null) {
            return "null";
        }
        return value;
    }

    @Override
    public void save(UserSettings settings) {
        this.settings.put(settings.getUserId(), settings);
        saveFile(settings);
    }

    @Override
    public void update(UserSettings settings) {
        this.settings.put(settings.getUserId(), settings);
        saveFile(settings);
    }

    @Override
    public Optional<UserSettings> findByUserId(String userId) {
        return Optional.ofNullable(settings.get(userId));
    }

}