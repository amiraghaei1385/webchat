package services;

import models.LoginAttempt;
import models.Session;
import models.User;
import repository.SessionRepository;
import repository.UserRepository;
import security.PasswordHasher;
import security.PasswordValidator;
import security.SessionValidator;
import java.util.Optional;
import repository.LoginAttemptRepository;

// مدیریت ورو خروج و ثبت‌نام کاربران
public class AuthService {

    private final UserRepository userrepo;
    private final SessionRepository sessionrepo;
    private final ChatService chatserv;
    private final LoginAttemptRepository loginattemptrepo;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository,
            ChatService chatService, LoginAttemptRepository loginAttemptRepository) {
        this.userrepo = userRepository;
        this.sessionrepo = sessionRepository;
        this.chatserv = chatService;
        this.loginattemptrepo = loginAttemptRepository;
    }

    // ثبت‌نام
    public Session register(String userId, String username, String plainPassword, String confirmPassword) {
        // منحصربه‌فرد بودن آیدی
        if (userrepo.findById(userId).isPresent()) {
            throw new IllegalArgumentException("User ID already taken.");
        }
        // بررسی تطابق رمز عبور
        if (!plainPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        // ب منحصربه‌فرد بودن نام کاربری
        if (userrepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken.");
        }
        // اعتبار قوانین رمز عبور
        PasswordValidator.ValidationResult result = PasswordValidator.validate(plainPassword, username);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.getErrorsSummary());
        }
        // سذخیره کاربر جدید
        String passwordhash = PasswordHasher.hash(plainPassword);
        User user = new User(userId, username, passwordhash);
        userrepo.save(user);
        chatserv.createSavedMessagesChat(userId);
        // ورود خودکار پس از ثبت‌نام
        Session session = SessionValidator.createSession(userId, false);
        sessionrepo.save(session);
        return session;
    }

    // ورود
    public Session login(String username, String plainPassword, boolean rememberMe) {
        Optional<User> optuser = userrepo.findByUsername(username);
        if (optuser.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password.");
        }
        User user = optuser.get();
        // بررسی قفل بودن حساب
        Optional<LoginAttempt> optattempt = loginattemptrepo.findByUserId(user.getId());
        LoginAttempt attempt;
        if (optattempt.isPresent()) {
            attempt = optattempt.get();
        } else {
            attempt = new LoginAttempt(user.getId());
        }
        // بررسی صحت رمز عبور
        if (!PasswordHasher.verify(plainPassword, user.getPasswordHash())) {
            attempt.recordFailure();
            loginattemptrepo.save(attempt);
            throw new IllegalArgumentException("Invalid username or password.");
        }
        if (attempt.isLocked()) {
            throw new IllegalStateException("Account is temporarily locked. Please try again later.");
        }
        // ورود موفق
        attempt.recordSuccess();
        loginattemptrepo.save(attempt);
        Session session = SessionValidator.createSession(user.getId(), rememberMe);
        sessionrepo.save(session);
        return session;
    }

    // خروج
    public void logout(String sessionToken) {
        Optional<Session> optsession = sessionrepo.findByToken(sessionToken);
        if (optsession.isEmpty()) {
            return;
        }
        Session session = optsession.get();
        SessionValidator.invalidate(session);
        sessionrepo.update(session);
    }

    // اعتبارسنجی نشست
    public Optional<User> validateSession(String sessionToken) {
        Optional<Session> optsession = sessionrepo.findByToken(sessionToken);
        if (optsession.isEmpty()) {
            return Optional.empty();
        }
        Session session = optsession.get();
        if (!SessionValidator.isValid(session)) {
            return Optional.empty();
        }
        return userrepo.findById(session.getUserId());
    }
}