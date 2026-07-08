package security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * رمزنگاری و رمزگشایی محتوای پیام‌ها قبل از ذخیره‌سازی روی دیسک.
 *
 * الگوریتم: AES/CBC/PKCS5Padding با کلید ۱۲۸ بیتی.
 * برای هر پیام یک IV (Initialization Vector) تصادفی جدید تولید می‌شود
 * تا حتی دو پیام با محتوای یکسان، خروجی رمزشده‌ی متفاوتی داشته باشند.
 *
 * فرمت خروجی نهایی (که در فایل ذخیره می‌شود): Base64(IV) + ":" + Base64(CipherText)
 */
public final class MessageEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH_BYTES = 16;

    // رشته‌ی پایه‌ای که کلید AES ۱۲۸ بیتی از آن مشتق می‌شود.
    // در استقرار واقعی: System.getenv("MESSAGE_ENCRYPTION_SECRET")
    private static final String SECRET_SEED =
            System.getProperty("message.encryption.secret", "ChatApp-Phase2-SecretKey-2026");

    private static final SecretKeySpec SECRET_KEY = buildKey(SECRET_SEED);
    private static final SecureRandom RANDOM = new SecureRandom();

    private MessageEncryptor() {
        // کلاس Utility - نمونه‌سازی مجاز نیست
    }

    /**
     * کلید AES ۱۲۸ بیتی را از یک رشته‌ی دلخواه با طول متغیر می‌سازد.
     * از SHA-256 برای هش کردن و سپس برش به ۱۶ بایت اول استفاده می‌شود.
     */
    private static SecretKeySpec buildKey(String seed) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] fullHash = sha256.digest(seed.getBytes(StandardCharsets.UTF_8));
            byte[] key16 = new byte[16];
            System.arraycopy(fullHash, 0, key16, 0, 16);
            return new SecretKeySpec(key16, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }

    /**
     * رمزنگاری یک متن ساده (plain text).
     * اگر ورودی null باشد، همان مقدار بدون تغییر برگردانده می‌شود.
     */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, new IvParameterSpec(iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            String ivBase64 = Base64.getEncoder().encodeToString(iv);
            String cipherBase64 = Base64.getEncoder().encodeToString(cipherBytes);
            return ivBase64 + ":" + cipherBase64;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message content", e);
        }
    }

    /**
     * رمزگشایی یک متن که قبلا رمزنگاری شده.
     * اگر ورودی null یا خالی باشد، همان مقدار بدون تغییر برگردانده می‌شود.
     */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }
        try {
            String[] parts = encryptedText.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Malformed encrypted payload");
            }
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, new IvParameterSpec(iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);

            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message content", e);
        }
    }
}