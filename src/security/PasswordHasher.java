package security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// هش کردن رمز عبور با الگوریتم و سالت
public class PasswordHasher {

    static final String algo = "SHA-256";
    static final int saltbyte = 16;
    static final String seprator = ":";

    // رمز عبور خام رو هش میکنه و برمیگردونه
    public static String hash(String pass) {
        if (pass == null || pass.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
        }
        byte[] salt = saltBesaz();
        byte[] hashBytes = hashBesaz(salt, pass);
        String hashBase = Base64.getEncoder().encodeToString(hashBytes);
        String saltBas = Base64.getEncoder().encodeToString(salt);
        return saltBas + seprator + hashBase;
    }

    // چک میکنه رمز واردشده با هش ذخیره شده یکی هست یا نه
    public static boolean verify(String pass, String hashDakhereh) {
        if (pass == null || hashDakhereh == null) {
            return false;
        }
        String[] parts = hashDakhereh.split(seprator, 2);
        if (parts.length != 2) {
            return false; 
        }
        byte[] hashDorost;
        byte[] salt;
        try {
            salt = Base64.getDecoder().decode(parts[0]);
            hashDorost = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] hashFeli = hashBesaz(salt, pass);
        return moghayeseAmn(hashDorost, hashFeli);
    }

    // یه سالت تصادفی میسازه
    static byte[] saltBesaz() {
        byte[] salt = new byte[saltbyte];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    // مقایسه دو آرایه بایت با زمان ثابت
    // برای جلوگیری از حمله تایمینگ
    static boolean moghayeseAmn(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int farg = 0;
        for (int i = 0; i < a.length; i++) {
            farg |= a[i] ^ b[i];
        }
        return farg == 0;
    }

    // هش رو روی سالت و رمز عبور حساب میکنه
    static byte[] hashBesaz(byte[] salt, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algo);
            digest.update(salt);
            digest.update(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available.", e);
        }
    }

}