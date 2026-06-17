package services;

import models.LoginAttempt;
import models.Session;
import models.User;
import repository.SessionRepository;
import repository.UserRepository;
import security.PasswordHasher;
import security.PasswordValidator;
import security.SessionValidator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
// مدیریت ورود، خروج و ثبت‌نام کاربران.

public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ChatService chatService;
    // تعداد تلاش‌های ناموفق ورود
    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository, ChatService chatService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.chatService = chatService;
      }

    // ثبت‌نام //

    // یک کاربر جدید ثبت می‌کند.

    public Session register(String userId, String username, String plainPassword, String confirmPassword) {
        // بررسی تطابق رمز عبور
        if (!plainPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // اعتبارسنجی قوانین رمز عبور
        PasswordValidator.ValidationResult result = PasswordValidator.validate(plainPassword, username);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getErrorsSummary());
        }

        // بررسی منحصربه‌فرد بودن آیدی
        if (userRepository.findById(userId).isPresent()) {
            throw new IllegalArgumentException("User ID already taken.");
        }

        // بررسی منحصربه‌فرد بودن نام کاربری
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken.");
        }

        // ساخت و ذخیره کاربر جدید
        String passwordHash = PasswordHasher.hash(plainPassword);
        User user = new User(userId, username, passwordHash);
        userRepository.save(user);
        chatService.createSavedMessagesChat(userId);
        // ورود خودکار پس از ثبت‌نام
        Session session = SessionValidator.createSession(userId, false);
        sessionRepository.save(session);
        return session;
    }

    // ورود //

    // ورود کاربر به سیستم.

    public Session login(String username, String plainPassword, boolean rememberMe) {
        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        User user = optUser.get();

        // بررسی قفل بودن حساب
        LoginAttempt attempt = loginAttempts.computeIfAbsent(user.getId(), LoginAttempt::new);
        if (attempt.isLocked()) {
            throw new IllegalStateException("Account is temporarily locked. Please try again later.");
        }

        // بررسی صحت رمز عبور
        if (!PasswordHasher.verify(plainPassword, user.getPasswordHash())) {
            attempt.recordFailure();
            throw new IllegalArgumentException("Invalid username or password.");
        }

        // ورود موفق
        attempt.recordSuccess();
        Session session = SessionValidator.createSession(user.getId(), rememberMe);
        sessionRepository.save(session);
        return session;
    }

    // خروج //

    // خروج کاربر با غیرفعال کردن نشست جاری.

    public void logout(String sessionToken) {
        sessionRepository.findByToken(sessionToken).ifPresent(session -> {
            SessionValidator.invalidate(session);
            sessionRepository.update(session);
        });
    }

    // اعتبارسنجی نشست //

    // معتبر بودن نشست را بررسی می‌کند.

    public Optional<User> validateSession(String sessionToken) {
        return sessionRepository.findByToken(sessionToken)
                .filter(SessionValidator::isValid)
                .flatMap(session -> userRepository.findById(session.getUserId()));
    }
}