package repository.file;

import models.MediaMessage;
import repository.MediaMessageRepository;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FileMediaMessageRepository implements MediaMessageRepository {
    private final Map<String, MediaMessage> mediaMap = new ConcurrentHashMap<>();
    private final File fold = new File("storage/media");

    public FileMediaMessageRepository() {
        if (!fold.exists()) {
            fold.mkdirs();
        }
        loadAll();
    }

    private String key(String chatId, String messageId) {
        return chatId + ":" + messageId;
    }

    // خواندن همه
    private void loadAll() {
        File[] files = fold.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.getName().endsWith(".meta.txt")) {
                continue;
            }
            MediaMessage media = readMediaFromFile(file);
            if (media != null) {
                mediaMap.put(key(media.getChatId(), media.getMessageId()), media);
            }
        }
    }

    // خواندن یکی
    private MediaMessage readMediaFromFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String idmessage = reader.readLine();
            String idchat = reader.readLine();
            String idsender = reader.readLine();
            String mediatype = reader.readLine();
            String filepath = reader.readLine();
            String originalfileName = reader.readLine();
            String mimetype = reader.readLine();
            String filesize = reader.readLine();
            String durationSeconds = reader.readLine();
            String PXwidth = reader.readLine();
            String PXheight = reader.readLine();
            String caption = reader.readLine();
            String thumbnailpath = reader.readLine();
            String sentat = reader.readLine();
            String isdelete = reader.readLine();
            reader.close();

            MediaMessage media = new MediaMessage();
            media.setMessageId(fixEmpty(idmessage));
            media.setChatId(fixEmpty(idchat));
            media.setSenderId(fixEmpty(idsender));
            if (mediatype != null && !mediatype.equals("null")) {
                media.setMediaType(MediaMessage.MediaType.valueOf(mediatype));
            }
            media.setFilePath(fixEmpty(filepath));
            media.setOriginalFileName(fixEmpty(originalfileName));
            media.setMimeType(fixEmpty(mimetype));
            if (filesize != null && !filesize.equals("null")) {
                media.setFileSizeBytes(Long.parseLong(filesize));
            }
            if (durationSeconds != null && !durationSeconds.equals("null")) {
                media.setDurationSeconds(Integer.parseInt(durationSeconds));
            }
            if (PXwidth != null && !PXwidth.equals("null")) {
                media.setWidthPx(Integer.parseInt(PXwidth));
            }
            if (PXheight != null && !PXheight.equals("null")) {
                media.setHeightPx(Integer.parseInt(PXheight));
            }
            media.setCaption(fixEmpty(caption));
            media.setThumbnailPath(fixEmpty(thumbnailpath));
            if (sentat != null && !sentat.equals("null")) {
                media.setSentAt(LocalDateTime.parse(sentat));
            }
            media.setDeleted(Boolean.parseBoolean(isdelete));
            return media;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // جلوگیری از خطای مقدار خالی
    private String fixEmpty(String value) {
        if (value == null || value.equals("null")) {
            return null;
        }
        return value;
    }

    // نوشتن
    private void saveFile(MediaMessage media) {
        File file = new File(fold,
                media.getChatId() + "__" + media.getMessageId() + ".meta.txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(safe(media.getMessageId()));
            writer.newLine();
            writer.write(safe(media.getChatId()));
            writer.newLine();
            writer.write(safe(media.getSenderId()));
            writer.newLine();
            if (media.getMediaType() == null) {
                writer.write(safe(null));
            } else {
                writer.write(media.getMediaType().name());
            }
            writer.newLine();
            writer.write(safe(media.getFilePath()));
            writer.newLine();
            writer.write(safe(media.getOriginalFileName()));
            writer.newLine();
            writer.write(safe(media.getMimeType()));
            writer.newLine();
            writer.write(String.valueOf(media.getFileSizeBytes()));
            writer.newLine();
            writer.write(String.valueOf(media.getDurationSeconds()));
            writer.newLine();
            writer.write(String.valueOf(media.getWidthPx()));
            writer.newLine();
            writer.write(String.valueOf(media.getHeightPx()));
            writer.newLine();
            writer.write(safe(media.getCaption()));
            writer.newLine();
            writer.write(safe(media.getThumbnailPath()));
            writer.newLine();
            if (media.getSentAt() == null) {
                writer.write(safe(null));
            } else {
                writer.write(media.getSentAt().toString());
            }
            writer.newLine();
            writer.write(String.valueOf(media.isDeleted()));
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // مقدار خالی برای نوشتن
    private String safe(String value) {
        if (value == null) {
            return "null";
        }
        return value;
    }

    @Override
    public void update(MediaMessage media) {
        mediaMap.put(key(media.getChatId(), media.getMessageId()), media);
        saveFile(media);
    }

    @Override
    public Optional<MediaMessage> findByMessageId(String chatId, String messageId) {
        return Optional.ofNullable(mediaMap.get(key(chatId, messageId)));
    }

    @Override
    public void delete(String chatId, String messageId) {
        mediaMap.remove(key(chatId, messageId));
        File file = new File(fold, chatId + "__" + messageId + ".meta.txt");
        if (file.exists()) {
            file.delete();
        }
    }

    @Override
    public void save(MediaMessage media) {
        mediaMap.put(key(media.getChatId(), media.getMessageId()), media);
        saveFile(media);
    }
}