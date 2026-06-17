package services;

import models.User;
import repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// مدیریت اطلاعات کاربران.

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // دریافت اطلاعات کاربر

    // دریافت کاربر با آیدی.

    public Optional<User> findById(String userId) {
        return userRepository.findById(userId);
    }

    /**
     * دریافت کاربر با نام کاربری.
     * برای صفحه ایجاد گفتگو و جستجوی مخاطب استفاده می‌شود.
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * دریافت لیست تمام کاربران.
     * برای CLI ادمین استفاده می‌شود.
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // وضعیت آنلاین

    /**
     * کاربر را آنلاین می‌کند.
     * هنگام اتصال WebSocket فراخوانی می‌شود.
     */
    public void setOnline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(true);
            userRepository.update(user);
        });
    }

    /**
     * کاربر را آفلاین می‌کند و زمان آخرین حضور را ثبت می‌کند.
     * هنگام قطع اتصال WebSocket فراخوانی می‌شود.
     */
    public void setOffline(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(false);
            user.setLastSeenAt(LocalDateTime.now());
            userRepository.update(user);
        });
    }

    // ویرایش پروفایل (صفحه تنظیمات)

    /**
     * تغییر نام نمایشی کاربر.
     */
    public User updateUsername(String userId, String newUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (newUsername == null || newUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty.");
        }

        user.setUsername(newUsername);
        userRepository.update(user);
        return user;
    }

    /**
     * تغییر آیدی منحصربه‌فرد کاربر.
     * بررسی می‌شود که آیدی جدید قبلاً توسط کاربر دیگری استفاده نشده باشد.
     */
    public User updateUserId(String currentUserId, String newUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (newUserId == null || newUserId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be empty.");
        }
        if (!newUserId.equals(currentUserId) && userRepository.findById(newUserId).isPresent()) {
            throw new IllegalArgumentException("User ID already taken.");
        }

        user.setId(newUserId);
        userRepository.update(user);
        return user;
    }

    /**
     * تنظیم یا تغییر عکس پروفایل کاربر.
     * مسیر فایل رسانه (که قبلاً در پوشه storage/media ذخیره شده) دریافت می‌شود.
     */
    public User updateProfilePicture(String userId, String picturePath) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setProfilePicPath(picturePath);
        userRepository.update(user);
        return user;
    }

    /**
     * حذف عکس پروفایل کاربر.
     */
    public User removeProfilePicture(String userId) {
        return updateProfilePicture(userId, null);
    }

    /**
     * حذف حساب کاربری (soft delete).
     * داده‌های کاربر پاک نمی‌شوند، فقط به عنوان حذف‌شده علامت‌گذاری می‌شوند
     * تا تاریخچه پیام‌های دیگران (مثلاً در گروه‌ها) دچار خطا نشود.
     */
    public void deleteAccount(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setDeleted(true);
        user.setOnline(false);
        userRepository.update(user);
    }
}