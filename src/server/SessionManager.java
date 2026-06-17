package server;

import models.User;
import services.AuthService;
import java.util.Optional;

// اعتبارسنجی session در هر درخواست
public class SessionManager {

    private final AuthService authService;

    public SessionManager(AuthService authService) {
        this.authService = authService;
    }

    // بررسی می‌کند که آیا توکن session معتبر است
    public Optional<User> validate(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        return authService.validateSession(sessionToken);
    }

    // بررسی می‌کند که آیا کاربر لاگین است
    public boolean isAuthenticated(String sessionToken) {
        return validate(sessionToken).isPresent();
    }
}