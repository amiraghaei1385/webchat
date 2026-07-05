package utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * مدیریت متمرکز تمام مسیرهای پایگاه‌داده فایلی و رسانه‌های پروژه.
 *
 * تمام کلاس‌های Repository و Service باید مسیرهای خود را از این کلاس
 * بگیرند تا در صورت جابه‌جایی ریشه پروژه (server root)، فقط یک نقطه
 * نیاز به تغییر داشته باشد.
 *
 * ساختار پوشه‌ها دقیقاً مطابق سند «ساختار_پروژه» است:
 * storage/
 * ├── users/
 * ├── groups/
 * ├── chats/
 * ├── messages/
 * ├── history/
 * ├── reports/
 * ├── folders/
 * ├── media/
 * ├── sessions/
 * ├── login-attempts/
 * ├── password-resets/
 * └── backups/
 */
public final class PathUtil {

    // ریشه پایگاه‌داده فایلی. قابل تغییر با system property در صورت نیاز
    // (مثلاً برای تست‌ها: -Dstorage.root=/tmp/test-storage)
    private static final String STORAGE_ROOT = System.getProperty("storage.root", "storage");

    private PathUtil() {
        // کلاس Utility - نمونه‌سازی مجاز نیست
    }

    // ─── ریشه‌ها ──────────────────────────────────────────────────────────

    public static Path root() {
        return Paths.get(STORAGE_ROOT);
    }

    public static Path usersDir() {
        return root().resolve("users");
    }

    public static Path groupsDir() {
        return root().resolve("groups");
    }

    public static Path chatsDir() {
        return root().resolve("chats");
    }

    public static Path messagesDir() {
        return root().resolve("messages");
    }

    public static Path historyDir() {
        return root().resolve("history");
    }

    public static Path reportsDir() {
        return root().resolve("reports");
    }

    public static Path foldersDir() {
        return root().resolve("folders");
    }

    public static Path mediaDir() {
        return root().resolve("media");
    }

    public static Path sessionsDir() {
        return root().resolve("sessions");
    }

    public static Path loginAttemptsDir() {
        return root().resolve("login-attempts");
    }

    public static Path passwordResetsDir() {
        return root().resolve("password-resets");
    }

    public static Path backupsDir() {
        return root().resolve("backups");
    }

    public static Path contactsDir() {
        return root().resolve("contacts");
    }

    public static Path settingsDir() {
        return root().resolve("settings");
    }

    public static Path reactionsDir() {
        return root().resolve("reactions");
    }

    // ─── مسیرهای فایل مشخص (per-entity) ──────────────────────────────────

    /** مسیر فایل یک کاربر: storage/users/{id}.txt */
    public static Path userFile(String id) {
        return usersDir().resolve(sanitize(id) + ".txt");
    }

    public static Path groupFile(String id) {
        return groupsDir().resolve(sanitize(id) + ".txt");
    }

    /** پوشه اعضای یک گروه خاص: storage/groups/{groupId}/members/ */
    public static Path groupMembersDir(String groupId) {
        return groupsDir().resolve(sanitize(groupId)).resolve("members");
    }

    public static Path groupMemberFile(String groupId, String userId) {
        return groupMembersDir(groupId).resolve(sanitize(userId) + ".txt");
    }

    public static Path chatFile(String id) {
        return chatsDir().resolve(sanitize(id) + ".txt");
    }

    public static Path messageFile(String id) {
        return messagesDir().resolve(sanitize(id) + ".txt");
    }

    /** تاریخچه ویرایش: storage/history/{chatId}/{messageId}/{historyId}.txt */
    public static Path historyDir(String chatId, String messageId) {
        return historyDir().resolve(sanitize(chatId)).resolve(sanitize(messageId));
    }

    public static Path historyFile(String chatId, String messageId, String historyId) {
        return historyDir(chatId, messageId).resolve(sanitize(historyId) + ".txt");
    }

    public static Path reportFile(String id) {
        return reportsDir().resolve(sanitize(id) + ".txt");
    }

    public static Path folderFile(String id) {
        return foldersDir().resolve(sanitize(id) + ".txt");
    }

    /** رسانه‌های یک چت: storage/media/{chatId}/{messageId}.{ext} */
    public static Path mediaChatDir(String chatId) {
        return mediaDir().resolve(sanitize(chatId));
    }

    public static Path mediaFile(String chatId, String messageId, String extension) {
        String ext = (extension == null || extension.isBlank()) ? "" : "." + extension;
        return mediaChatDir(chatId).resolve(sanitize(messageId) + ext);
    }

    public static Path mediaMetaFile(String chatId, String messageId) {
        return mediaChatDir(chatId).resolve(sanitize(messageId) + ".meta.txt");
    }

    public static Path mediaThumbnailFile(String chatId, String messageId, String extension) {
        String ext = (extension == null || extension.isBlank()) ? "" : "." + extension;
        return mediaChatDir(chatId).resolve("thumb_" + sanitize(messageId) + ext);
    }

    public static Path sessionFile(String token) {
        return sessionsDir().resolve(sanitize(token) + ".txt");
    }

    public static Path loginAttemptFile(String userId) {
        return loginAttemptsDir().resolve(sanitize(userId) + ".txt");
    }

    public static Path passwordResetFile(String token) {
        return passwordResetsDir().resolve(sanitize(token) + ".txt");
    }

    public static Path contactFile(String ownerId, String contactId) {
        return contactsDir().resolve(sanitize(ownerId) + "__" + sanitize(contactId) + ".txt");
    }

    public static Path settingsFile(String userId) {
        return settingsDir().resolve(sanitize(userId) + ".txt");
    }

    public static Path reactionFile(String id) {
        return reactionsDir().resolve(sanitize(id) + ".txt");
    }

    public static Path backupFile(String name) {
        return backupsDir().resolve(sanitize(name));
    }

    // ─── ابزار کمکی ───────────────────────────────────────────────────────

    /**
     * پاک‌سازی شناسه‌ها قبل از استفاده در نام فایل تا از Path Traversal
     * (مثلاً "../../etc/passwd") و کاراکترهای غیرمجاز جلوگیری شود.
     */
    public static String sanitize(String rawId) {
        if (rawId == null) {
            return "unknown";
        }
        // فقط حروف، عدد، خط‌تیره و آندرلاین مجازند
        String cleaned = rawId.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return cleaned.isBlank() ? "unknown" : cleaned;
    }

    /** پسوند فایل را از یک نام فایل اصلی استخراج می‌کند (بدون نقطه). */
    public static String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        int dot = originalFileName.lastIndexOf('.');
        if (dot == -1 || dot == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(dot + 1).toLowerCase();
    }

    public static String separator() {
        return File.separator;
    }
}
