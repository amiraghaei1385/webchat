package security;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// محدودکننده نرخ ارسال پیام در حافظه
// نحوه عملکرد (پنجره لغزان):
// برای هر کاربر صف کوچکی از زمان‌های ارسال پیام نگهداری می‌شود.
// قبل از ثبت پیام جدید، زمان‌های خارج از بازه حذف می‌شوند.
// اگر تعداد پیام‌های باقی‌مانده به حد مجاز رسیده باشد، ارسال رد می‌شود
public class RateLimiter {

    // حداکثر تعداد پیام مجاز در بازه زمانی مشخص
    private static final int MAX_MESSAGES = 5;
    // مدت زمان پنجره لغزان بر حسب میلی‌ثانیه (1 ثانیه)
    private static final long WINDOW_MS = 1_000L;

    // صف زمان‌های ارسال برای هر کاربر (بر حسب میلی‌ثانیه)
    // ConcurrentHashMap امکان دسترسی همزمان چند نخ را فراهم می‌کند
    private final Map<String, Deque<Long>> userTimestamps = new ConcurrentHashMap<>();

    // Public API //

    // بررسی می‌کند که آیا کاربر در این لحظه مجاز به ارسال پیام هست یا خیر
    // در صورت مجاز بودن، زمان فعلی ثبت می‌شود تا در دفعات بعد محاسبه شود
    public synchronized boolean allowSend(String userId) {
        long now = Instant.now().toEpochMilli();

        // دریافت یا ایجاد صف زمان‌های ارسال برای این کاربر
        Deque<Long> timestamps = userTimestamps.computeIfAbsent(userId, k -> new ArrayDeque<>());

        // حذف زمان‌های خارج از بازه فعلی پنجره لغزان
<<<<<<< Updated upstream
        while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= WINDOW_MS) {
            timestamps.pollFirst();
        }
=======
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= WINDOW_MS) {
                timestamps.pollFirst();
            }
>>>>>>> Stashed changes

            if (timestamps.size() >= MAX_MESSAGES) {
                // تعداد پیام‌های مجاز در یک ثانیه پر شده است
                return false;
            }

<<<<<<< Updated upstream
        // ثبت این ارسال و اجازه ادامه کار
        timestamps.addLast(now);
        return true;
=======
            // ثبت این ارسال و اجازه ادامه کار
            timestamps.addLast(now);
            return true;
        }
>>>>>>> Stashed changes
    }

    // تعداد پیام‌های ارسال‌شده توسط کاربر در بازه فعلی را برمی‌گرداند
    public synchronized int currentCount(String userId) {
        long now = Instant.now().toEpochMilli();
        Deque<Long> timestamps = userTimestamps.get(userId);
        if (timestamps == null)
            return 0;

<<<<<<< Updated upstream
        // حذف رکوردهای قدیمی قبل از شمارش
=======
<<<<<<< Updated upstream
     // حذف رکوردهای قدیمی قبل از شمارش
       synchronized (timestamps) {
>>>>>>> Stashed changes
        while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= WINDOW_MS) {
            timestamps.pollFirst();
        }
<<<<<<< Updated upstream
        return timestamps.size();
    }

=======
    }   
=======
        // حذف رکوردهای قدیمی قبل از شمارش
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) >= WINDOW_MS) {
                timestamps.pollFirst();
            }
            return timestamps.size();
        }
    }

>>>>>>> Stashed changes
>>>>>>> Stashed changes
    // اطلاعات ثبت‌شده برای کاربر را حذف می‌کند
    // معمولاً هنگام خروج کاربر از سیستم استفاده می‌شود
    public void clear(String userId) {
        userTimestamps.remove(userId);
    }
}