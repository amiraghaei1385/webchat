package utils;

import java.io.*;

// کار با فایل باینری
public class FileUtil {

    // نوشتن بایت در فایل
    public static void writeBytesAtomic(File file, byte[] data) {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            FileOutputStream out = new FileOutputStream(file);
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // حذف فایل
    public static void delete(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    // خواندن بایت از فایل
    public static byte[] readBytesOrNull(File file) {
        if (!file.exists()) {
            return null;
        }
        try {
            FileInputStream in = new FileInputStream(file);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int byteRead = in.read(chunk);
            while (byteRead != -1) {
                buffer.write(chunk, 0, byteRead);
                byteRead = in.read(chunk);
            }
            in.close();
            return buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}