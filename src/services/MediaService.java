package services;

import models.Chat;
import models.Message;
import models.MediaMessage;
import repository.ChatRepository;
import repository.MediaMessageRepository;
import repository.MessageRepository;
import utils.*;
import java.io.File;
import java.util.Optional;

// مدیریت آپلود و دانلود رسانه‌ها
public class MediaService {
    // ح حجم فایل
    private static final long MAX_file_size = 20L * 1024 * 1024;
    private final MediaMessageRepository mediarepo;
    private final MessageRepository messagerepo;
    private final ChatRepository chatrepo;

    public MediaService(MediaMessageRepository mediaRepository,
            MessageRepository messageRepository,
            ChatRepository chatRepository) {
        this.mediarepo = mediaRepository;
        this.messagerepo = messageRepository;
        this.chatrepo = chatRepository;
    }

    // دانلود رسانه
    public byte[] downloadMedia(String chatId, String messageId, String requesterId) {
        Optional<Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty()) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        Chat chat = optchat.get();
        if (!chat.getMemberIds().contains(requesterId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        Optional<MediaMessage> optmedia = mediarepo.findByMessageId(chatId, messageId);
        if (optmedia.isEmpty()) {
            throw new IllegalArgumentException("Media not found.");
        }
        MediaMessage media = optmedia.get();
        if (media.isDeleted()) {
            throw new IllegalStateException("This media has been removed.");
        }
        byte[] data = FileUtil.readBytesOrNull(new File(media.getFilePath()));
        if (data == null) {
            throw new IllegalStateException("Media file is missing from storage.");
        }
        return data;
    }

    // آپلود رسانه جدید
    public MediaMessage uploadMedia(String chatId, String senderId, byte[] fileBytes,
            String originalFileName, String mimeType, MediaMessage.MediaType mediaType,
            String caption) {
        Optional<Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty()) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        Chat chat = optchat.get();
        // بررسی عضو بودن فرستنده
        if (!chat.getMemberIds().contains(senderId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        if (fileBytes.length > MAX_file_size) {
            throw new IllegalArgumentException("File size exceeds the maximum allowed limit (20 MB).");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("File is empty.");
        }
        if (caption != null && caption.length() > 1024) {
            throw new IllegalArgumentException("Caption is too long (max 1024 characters).");
        }
        if (mediaType == null) {
            throw new IllegalArgumentException("Media type is required.");
        }
        String extension = PathUtil.extractExtension(originalFileName);
        String idmessage = IdGenerator.generate();
        // نوشتن
        File filePath = PathUtil.mediaFile(chatId, idmessage, extension);
        FileUtil.writeBytesAtomic(filePath, fileBytes);
        // ساخت پیام
        Message message = new Message(idmessage, chatId, senderId, null);
        message.setHasMedia(true);
        message.setMediaMessageId(idmessage);
        messagerepo.save(message);
        // ساخت دیتای رسانه
        MediaMessage media = new MediaMessage(idmessage, chatId, senderId, mediaType,
                filePath.getPath(), originalFileName, mimeType, fileBytes.length);
        media.setCaption(caption);
        mediarepo.save(media);
        // بروزرسانی آخرین پیام
        chat.setLastMessageId(message.getId());
        chat.setLastMessageAt(message.getSentAt());
        chatrepo.update(chat);
        return media;
    }

    // حذف رسانه
    public void deleteMedia(String chatId, String messageId, String requesterId) {
        Optional<MediaMessage> optmedia = mediarepo.findByMessageId(chatId, messageId);
        if (optmedia.isEmpty()) {
            throw new IllegalArgumentException("Media not found.");
        }
        MediaMessage media = optmedia.get();
        if (media.isDeleted()) {
            throw new IllegalStateException("Media is already deleted.");
        }
        if (!media.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("You can only delete your own media.");
        }
        // حذف فایل
        FileUtil.delete(new File(media.getFilePath()));
        if (media.getThumbnailPath() != null) {
            FileUtil.delete(new File(media.getThumbnailPath()));
        }
        media.setDeleted(true);
        mediarepo.update(media);
        // پیام به عنوان حذف شده علامت زده میشه
        Optional<Message> optmessage = messagerepo.findById(messageId);
        if (optmessage.isPresent()) {
            Message message = optmessage.get();
            message.setDeleted(true);
            messagerepo.update(message);
        }
    }

    // ویرایش کپشن رسانه
    public void editCaption(String chatId, String messageId, String requesterId, String newCaption) {
        Optional<MediaMessage> optmedia = mediarepo.findByMessageId(chatId, messageId);
        if (optmedia.isEmpty()) {
            throw new IllegalArgumentException("Media not found.");
        }
        MediaMessage media = optmedia.get();
        if (!media.getSenderId().equals(requesterId)) {
            throw new IllegalStateException("You can only edit your own media.");
        }
        if (media.isDeleted()) {
            throw new IllegalStateException("Cannot edit a deleted media message.");
        }
        if (newCaption != null && newCaption.length() > 1024) {
            throw new IllegalArgumentException("Caption is too long (max 1024 characters).");
        }
        media.setCaption(newCaption);
        mediarepo.update(media);
    }

    // گرفتن دیتای رسانه
    public Optional<MediaMessage> findMetadata(String chatId, String messageId) {
        return mediarepo.findByMessageId(chatId, messageId);
    }
}