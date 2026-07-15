package security;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// محدود کننده تعداد پیام هر کاربر تو یه بازه زمانی
public class RateLimiter {

    static final int MAX_message = 5;
    static final long MS_window = 1_000L;
    final Map<String, Deque<Long>> timeuser = new ConcurrentHashMap<>();

    // چک میکنه کاربر الان اجازه ارسال پیام داره یا نه
    // اگه اجازه داشت زمان فعلی ثبت میشه
    public boolean allowSend(String idUser) {
        long alan = Instant.now().toEpochMilli();
        Deque<Long> times = timeuser.get(idUser);
        if (times == null) {
            times = new ArrayDeque<>();
            timeuser.put(idUser, times);
        }
        synchronized (times) {
            while (!times.isEmpty() && (alan - times.peekFirst()) >= MS_window) {
                times.pollFirst();
            }

            if (times.size() >= MAX_message) {
                return false;
            }
            times.addLast(alan);
            return true;
        }
    }

    // اطلاعات کاربر رو پاک میکنه
    public void clear(String idUser) {
        timeuser.remove(idUser);
    }

    // تعداد پیام های کاربر تو بازه فعلی رو برمیگردونه
    public int currentCount(String idUser) {
        long alan = Instant.now().toEpochMilli();
        Deque<Long> times = timeuser.get(idUser);
        if (times == null)
            return 0;

        synchronized (times) {
            while (!times.isEmpty() && (alan - times.peekFirst()) >= MS_window) {
                times.pollFirst();
            }
            return times.size();
        }
    }
}