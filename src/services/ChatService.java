package services;

import models.Chat;
import models.ChatType;
import repository.ChatRepository;
import utils.IdGenerator;
import java.util.*;

// مدیریت چت‌ها
public class ChatService {

    private final ChatRepository chatrepo;
    private final ContactService contactserv;

    public ChatService(ChatRepository chatRepository, ContactService contactService) {
        this.chatrepo = chatRepository;
        this.contactserv = contactService;
    }

    // چت خصوصی بین دو کاربر ایجاد میشه
    public Chat getOrCreatePrivateChat(String userId1, String userId2) {
        if (userId1.equals(userId2)) {
            throw new IllegalArgumentException("Use Saved Messages for messaging yourself.");
        }
        if (contactserv.isBlocked(userId2, userId1) || contactserv.isBlocked(userId1, userId2)) {
            throw new IllegalStateException("Cannot start a chat: one user has blocked the other.");
        }
        List<Chat> userchats = chatrepo.findByUserId(userId1);
        for (Chat chat : userchats) {
            if (chat.getType() == ChatType.PRIVATE && chat.getMemberIds().contains(userId2)) {
                return chat;
            }
        }
        Chat chat = new Chat(IdGenerator.generate(), ChatType.PRIVATE);
        chat.addMember(userId1);
        chat.addMember(userId2);
        chatrepo.save(chat);
        return chat;
    }

    // صندوق پیام‌های ذخیره‌شده برای کاربر جدید ایجاد میشه
    public Chat createSavedMessagesChat(String userId) {
        Chat chat = new Chat(IdGenerator.generate(), ChatType.SAVED_MESSAGES);
        chat.addMember(userId);
        chatrepo.save(chat);
        return chat;
    }

    // ب لیست چت‌های کاربر به ترتیب آخرین پیام دریافت میشه
    public List<Chat> getUserChats(String userId) {
        List<Chat> chats = new ArrayList<>(chatrepo.findByUserId(userId));
        for (int i = 0; i < chats.size(); i++) {
            for (int j = 0; j < chats.size() - i - 1; j++) {
                Chat a = chats.get(j);
                Chat b = chats.get(j + 1);
                if (isOlder(a, b)) {
                    Chat temp = chats.get(j);
                    chats.set(j, chats.get(j + 1));
                    chats.set(j + 1, temp);
                }
            }
        }
        return chats;
    }

    // بررسی مرتب‌سازی
    private boolean isOlder(Chat a, Chat b) {
        if (a.getLastMessageAt() == null)
            return true;
        if (b.getLastMessageAt() == null)
            return false;
        return a.getLastMessageAt().compareTo(b.getLastMessageAt()) < 0;
    }

    // دریافت یک چت با آیدی
    public Optional<Chat> findById(String chatId) {
        return chatrepo.findById(chatId);
    }

    // علامت‌گذاری اینکه کاربر پیام‌های این چت رو دیده
    public void markAsRead(String chatId, String userId) {
        Optional<Chat> found = chatrepo.findById(chatId);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Chat not found.");
        }
        Chat chat = found.get();
        if (!chat.getMemberIds().contains(userId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        chat.markReadBy(userId);
        chatrepo.update(chat);
    }
}