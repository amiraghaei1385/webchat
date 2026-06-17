package services;

import models.Chat;
import models.ChatType;
import repository.ChatRepository;
import utils.IdGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// مدیریت چت‌ها (خصوصی، گروهی و پیام‌های ذخیره‌شده).
public class ChatService {

    private final ChatRepository chatRepository;

    public ChatService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    // ایجاد چت خصوصی بین دو کاربر.
    // اگر چت از قبل وجود داشته باشد، همان را برمی‌گرداند.
    public Chat getOrCreatePrivateChat(String userId1, String userId2) {
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Use Saved Messages for messaging yourself.");
        }

        List<Chat> userChats = chatRepository.findByUserId(userId1);
        for (Chat chat : userChats) {
            if (chat.getType() == ChatType.PRIVATE && chat.getMemberIds().contains(userId2)) {
                return chat;
            }
        }

        Chat chat = new Chat(IdGenerator.generate(), ChatType.PRIVATE);
        chat.addMember(userId1);
        chat.addMember(userId2);
        chatRepository.save(chat);
        return chat;
    }

    // ایجاد صندوق پیام‌های ذخیره‌شده برای کاربر جدید.
    // هنگام ثبت‌نام فراخوانی می‌شود.
    public Chat createSavedMessagesChat(String userId) {
        Chat chat = new Chat(IdGenerator.generate(), ChatType.SAVED_MESSAGES);
        chat.addMember(userId);
        chatRepository.save(chat);
        return chat;
    }

    // دریافت لیست چت‌های کاربر به ترتیب آخرین پیام.
    // برای صفحه اصلی استفاده می‌شود.
    public List<Chat> getUserChats(String userId) {
        List<Chat> chats = new ArrayList<>(chatRepository.findByUserId(userId));
        chats.sort((a, b) -> {
            if (a.getLastMessageAt() == null)
                return 1;
            if (b.getLastMessageAt() == null)
                return -1;
            return b.getLastMessageAt().compareTo(a.getLastMessageAt());
        });
        return chats;
    }

    // دریافت یک چت با آیدی.
    public Optional<Chat> findById(String chatId) {
        return chatRepository.findById(chatId);
    }
}