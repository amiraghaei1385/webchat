package services;

import models.Chat;
import models.ChatFolder;
import repository.ChatFolderRepository;
import repository.ChatRepository;
import utils.IdGenerator;
import java.util.*;

// مدیریت سنجاق و آرشیو و پوشه‌های چت
public class PinAndFolderService {
    private final ChatRepository chatrepo;
    private final ChatFolderRepository folderrepo;

    public PinAndFolderService(ChatRepository chatRepository, ChatFolderRepository folderRepository) {
        this.chatrepo = chatRepository;
        this.folderrepo = folderRepository;
    }

    // سنجاق کردن چت
    public void setPinned(String chatId, String requesterId, boolean pinned) {
        Chat chat = getOwnedChat(chatId, requesterId);
        chat.setPinned(pinned);
        chatrepo.update(chat);
    }

    // آرشیو کردن چت
    public void setArchived(String chatId, String requesterId, boolean archived) {
        Chat chat = getOwnedChat(chatId, requesterId);
        chat.setArchived(archived);
        chatrepo.update(chat);
    }

    // چت‌های آرشیوشده کاربر
    public List<Chat> getArchivedChats(String userId) {
        List<Chat> res = new ArrayList<>();
        for (Chat chat : chatrepo.findByUserId(userId)) {
            if (chat.isArchived()) {
                res.add(chat);
            }
        }
        return res;
    }

    // چت‌های سنجاق‌شده کاربر
    public List<Chat> getPinnedChats(String userId) {
        List<Chat> res = new ArrayList<>();
        for (Chat chat : chatrepo.findByUserId(userId)) {
            if (chat.isPinned()) {
                res.add(chat);
            }
        }
        return res;
    }

    // لیست پوشه‌های کاربر
    public List<ChatFolder> getFolders(String ownerId) {
        return folderrepo.findByOwnerId(ownerId);
    }

    // پوشه کامل حذف میشه
    public void deleteFolder(String folderId, String requesterId) {
        synchronized (folderId.intern()) {
            ChatFolder folder = getOwnedFolder(folderId, requesterId);

            for (String chatId : folder.getChatIds()) {
                Optional<Chat> optchat = chatrepo.findById(chatId);
                if (optchat.isPresent()) {
                    Chat chat = optchat.get();
                    if (folderId.equals(chat.getFolderId())) {
                        chat.setFolderId(null);
                        chatrepo.update(chat);
                    }
                }
            }

            folderrepo.delete(folderId);
        }
    }

    // پوشه جدید ساختع میشه
    public ChatFolder createFolder(String ownerId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Folder name cannot be empty.");
        }
        ChatFolder folder = new ChatFolder(IdGenerator.generate(), ownerId, name);
        List<ChatFolder> existing = folderrepo.findByOwnerId(ownerId);
        folder.setOrderIndex(existing.size());
        folderrepo.save(folder);
        return folder;
    }

    // تغییر نام پوشه
    public void renameFolder(String folderId, String requesterId, String newName) {
        ChatFolder folder = getOwnedFolder(folderId, requesterId);
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Folder name cannot be empty.");
        }
        folder.setName(newName);
        folderrepo.update(folder);
    }

    // تغییر ترتیب پوشه
    public void reorderFolder(String folderId, String requesterId, int newOrderIndex) {
        ChatFolder folder = getOwnedFolder(folderId, requesterId);
        folder.setOrderIndex(newOrderIndex);
        folderrepo.update(folder);
    }

    // افزودن چت به پوشه
    public void addChatToFolder(String folderId, String chatId, String requesterId) {
        Chat chat = getOwnedChat(chatId, requesterId);
        ChatFolder folder = getOwnedFolder(folderId, requesterId);
        String previousFolderId = chat.getFolderId();
        chat.setFolderId(folderId);
        chatrepo.update(chat);
        // خ پوشه قبلی خارج میشیم
        if (previousFolderId != null && !previousFolderId.equals(folderId)) {
            synchronized (previousFolderId.intern()) {
                Optional<ChatFolder> optprev = folderrepo.findById(previousFolderId);
                if (optprev.isPresent()) {
                    ChatFolder prevFolder = optprev.get();
                    prevFolder.getChatIds().remove(chatId);
                    folderrepo.update(prevFolder);
                }
            }
        }
        // افزودن به پوشه جدید
        synchronized (folderId.intern()) {
            Optional<ChatFolder> optfresh = folderrepo.findById(folderId);
            if (optfresh.isEmpty()) {
                throw new IllegalArgumentException("Folder not found.");
            }
            ChatFolder freshFolder = optfresh.get();
            if (!freshFolder.getChatIds().contains(chatId)) {
                freshFolder.getChatIds().add(chatId);
                folderrepo.update(freshFolder);
            }
        }
    }

    // خارج کردن چت از پوشه
    public void removeChatFromFolder(String chatId, String requesterId) {
        Chat chat = getOwnedChat(chatId, requesterId);
        String folderId = chat.getFolderId();
        if (folderId == null) {
            return;
        }
        synchronized (folderId.intern()) {
            Optional<ChatFolder> optfolder = folderrepo.findById(folderId);
            if (optfolder.isPresent()) {
                ChatFolder folder = optfolder.get();
                folder.getChatIds().remove(chatId);
                folderrepo.update(folder);
            }
        }
        chat.setFolderId(null);
        chatrepo.update(chat);
    }

    // بازیابی پوشه با بررسی مالکیت
    private ChatFolder getOwnedFolder(String folderId, String requesterId) {
        Optional<ChatFolder> optfolder = folderrepo.findById(folderId);
        if (optfolder.isEmpty()) {
            throw new IllegalArgumentException("Folder not found.");
        }
        ChatFolder folder = optfolder.get();
        if (!folder.getOwnerId().equals(requesterId)) {
            throw new IllegalStateException("Access denied: this folder does not belong to you.");
        }
        return folder;
    }

    // بازیابی چت با بررسی عضویت
    private Chat getOwnedChat(String chatId, String requesterId) {
        Optional<Chat> optchat = chatrepo.findById(chatId);
        if (optchat.isEmpty()) {
            throw new IllegalArgumentException("Chat not found.");
        }
        Chat chat = optchat.get();
        if (!chat.getMemberIds().contains(requesterId)) {
            throw new IllegalStateException("Access denied: you are not a member of this chat.");
        }
        return chat;
    }
}