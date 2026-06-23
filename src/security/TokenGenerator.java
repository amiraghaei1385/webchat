package security;

import java.security.SecureRandom;
import java.util.Base64;

// تولید توکن‌های تصادفی و امن برای مدیریت نشست‌ها (Session)
// و فرآیند بازیابی رمز عبور

public class TokenGenerator {

    // تعداد بایت‌های تصادفی قبل از تبدیل به Base64
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // جلوگیری از ساخت شیء از این کلاس؛ تمام متدها استاتیک هستند
    private TokenGenerator() {
    }

    // Public API //

    // تولید یک توکن نشست امن و مناسب برای استفاده در URL

    public static String generateSessionToken() {
        return generate(TOKEN_BYTES);
    }

    // تولید توکن امن برای بازیابی یا تغییر رمز عبور
    // از همان میزان تصادفی بودن توکن نشست استفاده می‌کند
    public static String generatePasswordResetToken() {
        return generate(TOKEN_BYTES);
    }

    // متدهای کمکی خصوصی

    // تولیدکننده اصلی توکن
    // تعداد مشخصی بایت تصادفی تولید کرده و
    // آن‌ها را به Base64 سازگار با URL تبدیل می‌کند
    private static String generate(int byteCount) {
        byte[] bytes = new byte[byteCount];
        SECURE_RANDOM.nextBytes(bytes);
        // استفاده از Base64 سازگار با URL باعث می‌شود
        // کاراکترهای '+' و '/' در هدرها و آدرس‌های HTTP ظاهر نشوند
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}