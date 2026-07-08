package utils;

import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;
import java.io.*;

/**
 * ابزارهای سطح پایین برای خواندن/نوشتن ایمن (thread-safe) فایل‌ها.
 *
 * چون سرور باید به‌صورت هم‌زمان (چندنخی) به چندین کاربر پاسخ دهد، تمام
 * عملیات نوشتن باید در برابر race condition محافظت شوند. این کلاس از دو
 * لایه محافظت استفاده می‌کند:
 *
 * به ازای هر مسیر فایل، در سطح JVM
 * (چندین Thread این پروسه نباید هم‌زمان یک فایل را بنویسند).
 * 2) نوشتن atomic با فایل موقت + rename، تا در صورت crash سرور در حین
 * نوشتن، فایل نیمه‌نوشته و خراب باقی نماند.
 *
 * تمام متدها checked IOException را به RuntimeException تبدیل می‌کنند
 * تا امضای متدهای Repository ساده و تمیز بماند؛ خطا در سطح Controller
 * با catch(Exception) مدیریت می‌شود.
 */
public final class FileUtil {

    // یک قفل مجزا برای هر مسیر فایل تا نوشتن‌های همزمان فایل‌های مختلف
    // مسدود یکدیگر نشوند (فقط دسترسی هم‌زمان به یک فایل واحد سریالایز می‌شود)
    private static final int STRIPE_COUNT = 256;
    private static final ReentrantReadWriteLock[] STRIPES = new ReentrantReadWriteLock[STRIPE_COUNT];

    static {
        for (int i = 0; i < STRIPE_COUNT; i++) {
            STRIPES[i] = new ReentrantReadWriteLock();
        }
    }

    private FileUtil() {
        // کلاس Utility - نمونه‌سازی مجاز نیست
    }

    private static ReentrantReadWriteLock lockFor(Path path) {
        int hash = Math.abs(path.toAbsolutePath().normalize().toString().hashCode());
        return STRIPES[hash % STRIPE_COUNT];
    }

    // ─── نوشتن ────────────────────────────────────────────────────────────

    /**
     * نوشتن اتمیک محتوای متنی روی یک فایل.
     * ابتدا در یک فایل موقت نوشته می‌شود سپس با اتمیک
     * جایگزین فایل اصلی می‌گردد تا در صورت قطع ناگهانی سرور، فایل هدف
     * هرگز نیمه‌نوشته (corrupt) نباشد.
     */
    public static void writeAtomic(Path path, String content) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.writeLock().lock();
        try {
            ensureParentDirExists(path);
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp-" +
                    System.nanoTime());
            Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write file: " + path, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** افزودن یک خط جدید به انتهای فایل (append) - برای لاگ‌های append-only. */
    public static void appendLine(Path path, String line) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.writeLock().lock();
        try {
            ensureParentDirExists(path);
            Files.write(path,
                    (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to file: " + path, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /** نوشتن باینری اتمیک - برای فایل‌های رسانه (تصویر، صدا، ویدیو، سند). */
    public static void writeBytesAtomic(Path path, byte[] data) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.writeLock().lock();
        try {
            ensureParentDirExists(path);
            Path tmp = path.resolveSibling(path.getFileName().toString() + ".tmp-" +
                    System.nanoTime());
            Files.write(tmp, data);
            Files.move(tmp, path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write binary file: " + path, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // خواندن
    /**
     * خواندن کامل محتوای فایل به‌صورت رشته. اگر فایل وجود نداشته باشد null
     * برمی‌گرداند.
     */
    public static String readOrNull(Path path) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.readLock().lock();
        try {
            if (!Files.exists(path)) {
                return null;
            }
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static byte[] readBytesOrNull(Path path) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.readLock().lock();
        try {
            if (!Files.exists(path)) {
                return null;
            }
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read binary file: " + path, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    // خواندن تمام خطوط یک فایل append-only. لیست خالی اگر فایل وجود نداشته باشد.

    public static List<String> readLines(Path path) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.readLock().lock();
        try {
            if (!Files.exists(path)) {
                return new ArrayList<>();
            }
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines: " + path, e);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * خواندن تمام فایل‌های داخل یک پوشه (بدون زیرپوشه‌ها).
     * برای بارگذاری اولیه‌ی Repositoryها هنگام راه‌اندازی سرور استفاده می‌شود.
     * اگر پوشه وجود نداشته باشد، لیست خالی برمی‌گرداند (خطا نمی‌دهد).
     */
    public static List<String> readAllInDirectory(Path dir) {
        List<String> contents = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return contents;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .sorted()
                    .toList()) {
                String content = readOrNull(p);
                if (content != null && !content.isBlank()) {
                    contents.add(content);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + dir, e);
        }
        return contents;
    }

    // لیست تمام زیرپوشه‌ها در یک مسیر
    public static List<Path> listSubDirectories(Path dir) {
        List<Path> result = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(dir)) {
            for (Path p : files.filter(Files::isDirectory).sorted().toList()) {
                result.add(p);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to list subdirectories: " + dir, e);
        }
        return result;
    }

    // حذف

    public static void delete(Path path) {
        ReentrantReadWriteLock lock = lockFor(path);
        lock.writeLock().lock();
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + path, e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    // حذف بازگشتی یک پوشه و تمام محتویاتش (برای پاک‌سازی رسانه‌های یک چت مثلاً).
    public static void deleteDirectoryRecursive(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        ReentrantReadWriteLock lock = lockFor(p);
                        lock.writeLock().lock();
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete: " + p, e);
                        } finally {
                            lock.writeLock().unlock();
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory: " + dir, e);
        }
    }

    // کمکی
    public static void ensureParentDirExists(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory for: " + path, e);
        }
    }

    public static void ensureDirExists(Path dir) {
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + dir, e);
        }
    }

    public static boolean exists(Path path) {
        return Files.exists(path);
    }
}
