package security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// چک میکنه رمز عبور پیشنهادی قوانین پروژه رو رعایت میکنه یا نه
public class PasswordValidator {

    static final int minlenghth = 8;
    static final Pattern HAS_uppercase = Pattern.compile("[A-Z]");
    static final Pattern HAS_lowercase = Pattern.compile("[a-z]");
    static final Pattern HAS_digit = Pattern.compile("[0-9]");
    static final Pattern HAS_special = Pattern.compile("[!@#$%^&*]");

    // رمز عبور رو با تمام قوانین چک میکنه
    public static ValidationResult validate(String pass, String user) {
        List<String> khataha = new ArrayList<>();
        if (pass == null || pass.isEmpty()) {
            khataha.add("Password must not be empty.");
            return new ValidationResult(khataha);
        }
        if (!HAS_lowercase.matcher(pass).find()) {
            khataha.add("Password must contain at least one lowercase letter (a-z).");
        }
        if (pass.length() < minlenghth) {
            khataha.add("Password must be at least " + minlenghth + " characters long.");
        }
        if (!HAS_special.matcher(pass).find()) {
            khataha.add("Password must contain at least one special character: ! @ # $ % ^ & *");
        }
        if (!HAS_digit.matcher(pass).find()) {
            khataha.add("Password must contain at least one digit (0-9).");
        }
        if (user != null && !user.isEmpty()) {
            if (pass.toLowerCase().contains(user.toLowerCase())) {
                khataha.add("Password must not contain the username.");
            }
        }
        if (!HAS_uppercase.matcher(pass).find()) {
            khataha.add("Password must contain at least one uppercase letter (A-Z).");
        }
        return new ValidationResult(khataha);
    }

    // نسخه ساده که نام کاربری رو چک نمیکنه
    public static ValidationResult validate(String password) {
        return validate(password, null);
    }

    // کلاس نگهدارنده نتیجه اعتبارسنجی
    public static class ValidationResult {
        final List<String> khataha;

        ValidationResult(List<String> khataha) {
            this.khataha = khataha;
        }

        public boolean isValid() {
            return khataha.isEmpty();
        }

        // لیست خطا ها رو برمیگردونه
        public List<String> getErrors() {
            return khataha;
        }

        // همه خطا ها رو با خط جدید تو یه رشته برمیگردونه
        public String getErrorsSummary() {
            return String.join("\n", khataha);
        }

        @Override
        public String toString() {
            return isValid() ? "ValidationResult{VALID}" : "ValidationResult{INVALID: " + khataha + "}";
        }
    }
}