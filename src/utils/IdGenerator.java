package utils;

import java.util.UUID;

/**
 * تولید آیدی یکتا برای موجودیت‌های سیستم.
 */
public class IdGenerator {

    private IdGenerator() {}

    /**
     * یک آیدی یکتا تولید می‌کند.
     * مثال خروجی: "550e8400-e29b-41d4-a716-446655440000"
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
