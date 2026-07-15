import cli.AdminCLI;
import repository.file.*;
import security.RateLimiter;
import server.ServerBootstrap;
import services.*;

import java.io.IOException;

// نقطه ورود برنامه
public class Main {

    public static void main(String[] args) {

        // ساخت ریپازیتوری ها
        FileUserRepository userRepo = new FileUserRepository();
        FileChatRepository chatRepo = new FileChatRepository();
        FileMessageRepository messageRepo = new FileMessageRepository();
        FileGroupRepository groupRepo = new FileGroupRepository();
        FileContactRepository contactRepo = new FileContactRepository();
        FileSessionRepository sessionRepo = new FileSessionRepository();
        FileReportedMessageRepository reportRepo = new FileReportedMessageRepository();
        FileSettingsRepository settingsRepo = new FileSettingsRepository();
        FileHistoryRepository historyRepo = new FileHistoryRepository();
        FileChatFolderRepository folderRepo = new FileChatFolderRepository();
        FileLoginAttemptRepository loginAttemptRepo = new FileLoginAttemptRepository();
        FileReactionRepository reactionRepo = new FileReactionRepository();
        FileMediaMessageRepository mediaRepo = new FileMediaMessageRepository();
        FilePasswordResetTokenRepository passwordResetRepo = new FilePasswordResetTokenRepository();
        FileStoryRepository storyRepo = new FileStoryRepository();

        // ساخت سرویس ها
        RateLimiter rateLimiter = new RateLimiter();
        ContactService contactService = new ContactService(contactRepo, userRepo);
        ChatService chatService = new ChatService(chatRepo, contactService);
        AuthService authService = new AuthService(userRepo, sessionRepo, chatService, loginAttemptRepo);
        UserService userService = new UserService(userRepo);
        HistoryService historyService = new HistoryService(historyRepo, chatRepo, messageRepo);
        MessageService messageService = new MessageService(messageRepo, chatRepo, reportRepo, rateLimiter,
                historyService, contactService);
        GroupService groupService = new GroupService(groupRepo, chatRepo, userRepo);
        AdminService adminService = new AdminService(userRepo, groupRepo, reportRepo, groupService, chatRepo);
        SettingsService settingsService = new SettingsService(userRepo, settingsRepo);
        PinAndFolderService pinAndFolderService = new PinAndFolderService(chatRepo, folderRepo);
        ReactionService reactionService = new ReactionService(reactionRepo, chatRepo, messageRepo);
        MediaService mediaService = new MediaService(mediaRepo, messageRepo, chatRepo);
        SearchService searchService = new SearchService(chatRepo, messageRepo, userRepo, groupRepo);
        PasswordResetService passwordResetService = new PasswordResetService(userRepo, passwordResetRepo, sessionRepo);
        StoryService storyService = new StoryService(storyRepo, userRepo, contactRepo);

        // راه اندازی سرور تو یه ترد جدا
        ServerRunnable serverJob = new ServerRunnable(authService, userService, chatService, messageService,
                groupService, contactService, adminService, settingsService,
                pinAndFolderService, historyService, reactionService,
                mediaService, searchService, passwordResetService, storyService);
        Thread serverThread = new Thread(serverJob, "server-main");
        serverThread.setDaemon(true);
        serverThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownRunnable(), "shutdown-hook"));
        AdminCLI adminCLI = new AdminCLI(adminService, messageService);
        adminCLI.run();

        System.exit(0);
    }

    // کلاس اجرای سرور تو ترد جدا
    static class ServerRunnable implements Runnable {
        private final AuthService authService;
        private final UserService userService;
        private final ChatService chatService;
        private final MessageService messageService;
        private final GroupService groupService;
        private final ContactService contactService;
        private final AdminService adminService;
        private final SettingsService settingsService;
        private final PinAndFolderService pinAndFolderService;
        private final HistoryService historyService;
        private final ReactionService reactionService;
        private final MediaService mediaService;
        private final SearchService searchService;
        private final PasswordResetService passwordResetService;
        private final StoryService storyService;

        ServerRunnable(AuthService authService, UserService userService, ChatService chatService,
                MessageService messageService, GroupService groupService, ContactService contactService,
                AdminService adminService, SettingsService settingsService,
                PinAndFolderService pinAndFolderService, HistoryService historyService,
                ReactionService reactionService, MediaService mediaService, SearchService searchService,
                PasswordResetService passwordResetService, StoryService storyService) {
            this.authService = authService;
            this.userService = userService;
            this.chatService = chatService;
            this.messageService = messageService;
            this.groupService = groupService;
            this.contactService = contactService;
            this.adminService = adminService;
            this.settingsService = settingsService;
            this.pinAndFolderService = pinAndFolderService;
            this.historyService = historyService;
            this.reactionService = reactionService;
            this.mediaService = mediaService;
            this.searchService = searchService;
            this.passwordResetService = passwordResetService;
            this.storyService = storyService;
        }

        @Override
        public void run() {
            try {
                ServerBootstrap.start(
                        authService, userService, chatService, messageService,
                        groupService, contactService, adminService, settingsService,
                        pinAndFolderService, historyService, reactionService,
                        mediaService, searchService, passwordResetService, storyService);
            } catch (IOException e) {
                System.err.println("[Main] Failed to start server: " + e.getMessage());
                System.exit(1);
            }
        }
    }
    // کلاس پیام خروج برنامه
    static class ShutdownRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("\n[Main] Shutting down...");
        }
    }
}