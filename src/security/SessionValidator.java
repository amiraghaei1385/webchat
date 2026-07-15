package security;

import models.Session;
import java.time.LocalDateTime;

// اعتبارسنجی توکن نشست که تو درخواست های  یا وب سوکت میاد
public class SessionValidator {
    static final int SESSION_durationhours = 24;
    static final int REMEMBER_ME_durationdays= 30;

    // جلوگیری از ساخت شیء، 
    private SessionValidator() {
    }
    // چک میکنه نشست داده شده معتبره یا نه
    public static boolean isValid(Session session) {
        if (session == null) {
            return false;
        }
        return session.isValid();
    }

    // یه نشست جدید با توکن تازه واسه کاربر میسازه
    public static Session createSession(String idUser, boolean rememberMe) {
        String token = TokenGenerator.generateSessionToken();
        LocalDateTime zamanEnghezaa = rememberMe
                ? LocalDateTime.now().plusDays(REMEMBER_ME_durationdays)
                : LocalDateTime.now().plusHours(SESSION_durationhours);
        return new Session(token, idUser, zamanEnghezaa);
    }

    // غیرفعال کردن 
    public static void invalidate(Session session) {
        if (session != null) {
            session.setActive(false);
        }
    }

    // دلیل نامعتبر بودن نشست رو به صورت متن برمیگردونه
    public static String diagnose(Session session) {
        if (session == null)
            return "session not found";
        if (!session.isActive())
            return "session revoked";
        if (LocalDateTime.now().isAfter(session.getExpiresAt()))
            return "session expired";
        return "valid";
    }
}