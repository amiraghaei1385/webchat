package server;

import models.User;
import services.AuthService;
import java.util.Optional;

// اعتبارسنجی در هر درخواست
public class SessionManager {

    private final AuthService authService;

    public SessionManager(AuthService authService) {
        this.authService = authService;
    }

    // بررسی می‌کنه که آیا توکن معتبر است
    public Optional<User> validate(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        return authService.validateSession(sessionToken);
    }

    // بررسی میکنه ایا کاربر لاگین هست یا نه
    public boolean isAuthenticated(String sessionToken) {
        return validate(sessionToken).isPresent();
    }
}