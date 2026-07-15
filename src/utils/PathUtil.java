package utils;

import java.io.File;

// کمک برای ساخت مسیر فایل های رسانه
public class PathUtil {

    // پوشه اصلی رسانه ها
    public static File mediaDir() {
        return new File("storage/media");
    }

    // پوشه یک چت خاص 
    public static File mediaChatDir(String chatId) {
        File dir = new File(mediaDir(), chatId);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    // مسیر فایل باینری 
    public static File mediaFile(String chatId, String messageId, String extension) {
        String fileName;
        if (extension == null || extension.isEmpty()) {
            fileName = messageId;
        } else {
            fileName = messageId + "." + extension;
        }
        return new File(mediaChatDir(chatId), fileName);
    }

    // مسیر فایل تصویر بندانگشتی
    public static File thumbnailFile(String chatId, String messageId) {
        return new File(mediaChatDir(chatId), "thumb_" + messageId + ".jpg");
    }

    // گرفتن پسوند از نام فایل
    public static String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        int dotidx = originalFileName.lastIndexOf(".");
        if (dotidx == -1 || dotidx == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(dotidx + 1);
    }
}