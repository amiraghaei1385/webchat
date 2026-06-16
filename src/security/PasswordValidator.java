package security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

//  بررسی می‌کند که آیا رمز عبور پیشنهادی تمام قوانین پروژه را رعایت می‌کند یا خیر

public class PasswordValidator {

    // الگوهای Regex (برای استفاده مجدد یک بار کامپایل می‌شوند)
    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
    // کاراکترهای ویژه مطابق با مستندات پروژه
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*]");

    // Public API

    // رمز عبور را بر اساس تمام قوانین اعتبارسنجی بررسی می‌کند
    public static ValidationResult validate(String plainPassword, String username) {
        List<String> errors = new ArrayList<>();

        if (plainPassword == null || plainPassword.isEmpty()) {
            errors.add("Password must not be empty.");
            return new ValidationResult(errors); // ادامه بررسی فایده‌ای ندارد
        }

        // قانون ۱: حداقل طول رمز عبور
        if (plainPassword.length() < MIN_LENGTH) {
            errors.add("Password must be at least " + MIN_LENGTH + " characters long.");
        }

        // قانون ۲: وجود حداقل یک حرف بزرگ
        if (!HAS_UPPERCASE.matcher(plainPassword).find()) {
            errors.add("Password must contain at least one uppercase letter (A-Z).");
        }

        // قانون ۳: وجود حداقل یک حرف کوچک
        if (!HAS_LOWERCASE.matcher(plainPassword).find()) {
            errors.add("Password must contain at least one lowercase letter (a-z).");
        }

        // قانون ۴: وجود حداقل یک رقم
        if (!HAS_DIGIT.matcher(plainPassword).find()) {
            errors.add("Password must contain at least one digit (0-9).");
        }

        // قانون ۵: وجود حداقل یک کاراکتر ویژه
        if (!HAS_SPECIAL.matcher(plainPassword).find()) {
            errors.add("Password must contain at least one special character: ! @ # $ % ^ & *");
        }

        // قانون ۶: رمز عبور نباید شامل نام کاربری باشد (بدون حساسیت به بزرگی و کوچکی
        // حروف)
        if (username != null && !username.isEmpty()) {
            if (plainPassword.toLowerCase().contains(username.toLowerCase())) {
                errors.add("Password must not contain the username.");
            }
        }

        return new ValidationResult(errors);
    }

    // نسخه ساده‌تر اعتبارسنجی که بررسی نام کاربری را انجام نمی‌دهد
    public static ValidationResult validate(String plainPassword) {
        return validate(plainPassword, null);
    }

    // کلاس داخلی نگهدارنده نتیجه اعتبارسنجی
    public static class ValidationResult {

        private final List<String> errors;

        private ValidationResult(List<String> errors) {
            this.errors = errors;
        }

        // اگر تمام قوانین رعایت شده باشند مقدار true برمی‌گرداند
        public boolean isValid() {
            return errors.isEmpty();
        }

        // لیست خطاهای اعتبارسنجی را برمی‌گرداند
        public List<String> getErrors() {
            return errors;
        }

        // تمام خطاها را در یک رشته و با جداکننده خط جدید برمی‌گرداند.

        public String getErrorsSummary() {
            return String.join("\n", errors);
        }

        @Override
        public String toString() {
            return isValid() ? "ValidationResult{VALID}" : "ValidationResult{INVALID: " + errors + "}";
        }
    }
}