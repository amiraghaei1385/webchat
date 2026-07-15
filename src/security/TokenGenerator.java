package security;

import java.security.SecureRandom;
import java.util.Base64;

// تولید توکن های تصادفی و امن برای نشست و بازیابی رمز عبور
public class TokenGenerator {
    static final int TOKEN_byte = 32;
    static final SecureRandom SECURE_rand = new SecureRandom();

    private TokenGenerator() {
    }

    // تولید توکن امن برای بازیابی یا تغییر رمز عبور
    public static String generatePasswordResetToken() {
        return besaz(TOKEN_byte);
    }

    // تولید توکن نشست امن
    public static String generateSessionToken() {
        return besaz(TOKEN_byte);
    }

    // اصل تولید توکن
    static String besaz(int tedadByte) {
        byte[] bytes = new byte[tedadByte];
        SECURE_rand.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}