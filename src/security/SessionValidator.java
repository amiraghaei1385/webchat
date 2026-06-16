package security;

import models.Session;

import java.time.LocalDateTime;

// اعتبارسنجی توکن‌های نشست (Session) که در درخواست‌های HTTP یا WebSocket ارسال می‌شوند
// پیدا کردن Session بر اساس توکن بر عهده SessionRepository است
// تا این کلاس فقط شامل منطق کسب‌وکار باشد
public class SessionValidator {

    // مدت زمان اعتبار نشست عادی پس از ورود
    private static final int SESSION_DURATION_HOURS = 24;

    // مدت زمان اعتبار نشست در حالت مرا به خاطر بسپار
    private static final int REMEMBER_ME_DURATION_DAYS = 30;

    // جلوگیری از ساخت شیء از این کلاس؛ تمام متدها استاتیک هستند
    private SessionValidator() {
    }

    // رابط عمومی کلاس

    // بررسی می‌کند که آیا Session داده‌شده معتبر است یا خیر
    public static boolean isValid(Session session) {
        if (session == null) {
            return false;
        }
        return session.isValid(); // بررسی اعتبار به متد Session.isValid واگذار می‌شود
    }

    // ایجاد یک Session جدید برای کاربر با یک توکن تازه
    // معمولاً بلافاصله پس از ورود موفق کاربر فراخوانی می‌شود
    public static Session createSession(String userId, boolean rememberMe) {
        String token = TokenGenerator.generateSessionToken();

        LocalDateTime expiresAt = rememberMe
                ? LocalDateTime.now().plusDays(REMEMBER_ME_DURATION_DAYS)
                : LocalDateTime.now().plusHours(SESSION_DURATION_HOURS);

        return new Session(token, userId, expiresAt);
    }

    // غیرفعال کردن Session
    public static void invalidate(Session session) {
        if (session != null) {
            session.setActive(false);
        }
    }

    // دلیل نامعتبر بودن Session را به صورت متنی برمی‌گرداند
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