package security;

import java.util.Base64;

// رمزنگاری پیام ها
public final class MessageEncryptor {
    // کلید رمزنگاری
    static final String key = System.getProperty("secretKey", "mykey123");

    private MessageEncryptor() {
    }

    // رمزنگاری متن
    public static String encrypt(String text) {
        if (text == null) {
            return null;
        }
        byte[] keybytes = key.getBytes();
        byte[] textbytes = text.getBytes();
        byte[] res = new byte[textbytes.length];
        for (int i = 0; i < textbytes.length; i++) {
            res[i] = (byte) (textbytes[i] ^ keybytes[i % keybytes.length]);
        }
        return Base64.getEncoder().encodeToString(res);
    }

    // رمزگشایی متن
    public static String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return encryptedText;
        }
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] keybytes = key.getBytes();
            byte[] res = new byte[encryptedBytes.length];
            for (int i = 0; i < encryptedBytes.length; i++) {
                res[i] = (byte) (encryptedBytes[i] ^ keybytes[i % keybytes.length]);
            }
            return new String(res);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message content", e);
        }
    }
}