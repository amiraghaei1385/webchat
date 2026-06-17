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

    //  دریافت اطلاعات کاربر                                              

    //  دریافت کاربر با آیدی.
     
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

    //  وضعیت آنلاین                                                      

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
}