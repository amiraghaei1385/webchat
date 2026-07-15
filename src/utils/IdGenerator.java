package utils;

import java.util.UUID;

// تولید آیدی یکتا برای سیستم
public class IdGenerator {

    private IdGenerator() {}

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
