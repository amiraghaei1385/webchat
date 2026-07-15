package services;

import models.Chat;
import models.ChatType;
import models.Group;
import models.Message;
import models.User;
import repository.ChatRepository;
import repository.GroupRepository;
import repository.MessageRepository;
import repository.UserRepository;
import security.MessageEncryptor;
import java.util.*;

// جستجو بین چت‌ها و پیام‌ها
public class SearchService {

    private final ChatRepository chatrepo;
    private final MessageRepository messagerepo;
    private final UserRepository userrepo;
    private final GroupRepository grouprepo;

    public SearchService(ChatRepository chatRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            GroupRepository groupRepository) {
        this.chatrepo = chatRepository;
        this.messagerepo = messageRepository;
        this.userrepo = userRepository;
        this.grouprepo = groupRepository;
    }

    // بررسی تطابق نام چت با عبارت جستجو
    private boolean matchesChatName(Chat chat, String requesterId, String normalizedQuery) {
        if (chat.getType() == ChatType.GROUP) {
            Optional<Group> optgroup = grouprepo.findByChatId(chat.getId());
            if (optgroup.isEmpty()) {
                return false;
            }
            return optgroup.get().getName().toLowerCase().contains(normalizedQuery);
        }
        if (chat.getType() == ChatType.PRIVATE) {
            String otherUserId = null;
            for (String id : chat.getMemberIds()) {
                if (!id.equals(requesterId)) {
                    otherUserId = id;
                    break;
                }
            }
            if (otherUserId == null) {
                return false;
            }
            Optional<User> optuser = userrepo.findById(otherUserId);
            if (optuser.isEmpty()) {
                return false;
            }
            return optuser.get().getUsername().toLowerCase().contains(normalizedQuery);
        }
        return false;
    }

    // جستجوی چت با نام گروه یا کاربر
    public List<Chat> searchChats(String userId, String query) {
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        String normalizequery = query.trim().toLowerCase();
        List<Chat> res = new ArrayList<>();
        for (Chat chat : chatrepo.findByUserId(userId)) {
            if (matchesChatName(chat, userId, normalizequery)) {
                res.add(chat);
            }
        }
        return res;
    }

    // جستجوی متنی در پیام‌های یک چت
    public List<Message> searchMessagesInChat(String chatId, String requesterId, String query) {
        Optional<Chat> optchat = chatrepo.findById(chatId);
        if (query == null || query.isBlank()) {
            return new ArrayList<>();
        }
        if (optchat.isEmpty() || !optchat.get().getMemberIds().contains(requesterId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        String normalizequery = query.trim().toLowerCase();
        List<Message> res = new ArrayList<>();
        for (Message message : messagerepo.findByChatId(chatId)) {
            if (message.isDeleted()) {
                continue;
            }
            String decrypted = MessageEncryptor.decrypt(message.getEncryptedContent());
            if (decrypted != null && decrypted.toLowerCase().contains(normalizequery)) {
                res.add(toDecryptedCopy(message));
            }
        }
        return res;
    }

    private Message toDecryptedCopy(Message original) {
        Message copy = new Message();
        copy.setId(original.getId());
        copy.setSenderId(original.getSenderId());
        copy.setEncryptedContent(MessageEncryptor.decrypt(original.getEncryptedContent()));
        copy.setDeleted(original.isDeleted());
        copy.setEditedAt(original.getEditedAt());
        copy.setChatId(original.getChatId());
        copy.setMediaMessageId(original.getMediaMessageId());
        copy.setReplyToMessageId(original.getReplyToMessageId());
        copy.setHasMedia(original.isHasMedia());
        copy.setSentAt(original.getSentAt());
        copy.setForwardedFromMessageId(original.getForwardedFromMessageId());
        return copy;
    }
}
