package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

//هش کردن رمز عبور و بررسی صحت ان با استفاده از الگوریتم SHA-256 و salt
//فرمت ذخیره سازی در پایگاه داده به این شکله  SALT:HASH
// salt به مقدار تصادفیbase64 تبدیل شده
// HASH نتیجه SHA-256 روی salt + password است که آن هم به Base64 تبدیل شده است.

public class PasswordHasher {

    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_BYTES = 16;
    private static final String SEPARATOR = ":";

    // Public API

    // رمز عبور خام را هش کرده و رشته‌ای مناسب برای ذخیره در پایگاه داده برمی‌گرداند
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
        }

        byte[] salt = generateSalt();
        byte[] hashBytes = computeHash(salt, plainPassword);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);

        return saltBase64 + SEPARATOR + hashBase64;
    }

    // بررسی می‌کند که آیا رمز واردشده با مقدار ذخیره‌شده مطابقت دارد یا خیر
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }

        String[] parts = storedHash.split(SEPARATOR, 2);
        if (parts.length != 2) {
            return false; // فرمت مقدار ذخیره‌شده نامعتبر است
        }

        byte[] salt;
        byte[] expectedHash;
        try {
            salt = Base64.getDecoder().decode(parts[0]);
            expectedHash = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return false; // تبدیل Base64 با خطا مواجه شد
        }

        byte[] actualHash = computeHash(salt, plainPassword);
        return slowEquals(expectedHash, actualHash);
    }

    // متدهای کمکی خصوصی
    // تولید یک Salt تصادفی با امنیت رمزنگاری
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // هش SHA-256 را روی ترکیب Salt و رمز عبور محاسبه می‌کند
    private static byte[] computeHash(byte[] salt, String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(salt);
            digest.update(plainPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to exist in every Java SE implementation
            throw new RuntimeException("SHA-256 algorithm not available.", e);
        }
    }

    // مقایسه آرایه‌های بایتی با زمان اجرای ثابت
    // برای جلوگیری از حملات Timing Attack
    // همواره تمام طول هر دو آرایه بررسی می‌شود
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}