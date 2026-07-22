package server;

import controllers.*;
import services.*;
import java.io.IOException;

// اینجا بخش خوشگلمونه که همه شون راه اندازی میشن و به هم وصل میشه
public class ServerBootstrap {

    private static final int HTTP_PORT = 8080;
    private static final int WS_PORT = 8081;

    public static void start(
            AuthService authService,
            UserService userService,
            ChatService chatService,
            MessageService messageService,
            GroupService groupService,
            ContactService contactService,
            AdminService adminService,
            SettingsService settingsService,
            PinAndFolderService pinAndFolderService,
            HistoryService historyService,
            ReactionService reactionService,
            MediaService mediaService,
            SearchService searchService,
            PasswordResetService passwordResetService,
            StoryService storyService) throws IOException {
        SessionManager sessionManager = new SessionManager(authService);
        HttpApiServer httpServer = new HttpApiServer(HTTP_PORT);

        // احراز هویت
        AuthController authController = new AuthController(authService, sessionManager, passwordResetService);
        httpServer.register("POST", "/api/auth/login", authController);
        httpServer.register("POST", "/api/auth/register", authController);
        httpServer.register("POST", "/api/auth/logout", authController);
        httpServer.register("POST", "/api/auth/forgot-password", authController);
        httpServer.register("POST", "/api/auth/reset-password", authController);
        // چت‌ها و پیام‌ها
        ChatController chatController = new ChatController(chatService, messageService,
                historyService, pinAndFolderService, sessionManager, userService, groupService);
        httpServer.register("POST", "/api/chats", chatController);
        httpServer.register("GET", "/api/chats", chatController);
        httpServer.register("DELETE", "/api/chats", chatController);
        httpServer.register("POST", "/api/chats/private", chatController);
        httpServer.register("PUT", "/api/chats", chatController);
        // عکس پروفایل و عکس گروه
        AvatarController avatarController = new AvatarController(userService, groupService, sessionManager);
        httpServer.register("GET", "/api/avatars", avatarController);
        // کاربران
        UserController userController = new UserController(userService, sessionManager);
        httpServer.register("GET", "/api/users", userController);
        httpServer.register("PUT", "/api/users/me/username", userController);
        httpServer.register("PUT", "/api/users/me/id", userController);
        httpServer.register("PUT", "/api/users/me/profile-picture", userController);
        httpServer.register("DELETE", "/api/users/me/profile-picture", userController);
        httpServer.register("PUT", "/api/users/me/background-picture", userController);
        httpServer.register("DELETE", "/api/users/me/background-picture", userController);
        httpServer.register("DELETE", "/api/users/me", userController);
        httpServer.register("PUT", "/api/users/me/bio", userController);
        httpServer.register("POST", "/api/users/me/profile-picture/upload", userController);
        httpServer.register("PUT", "/api/users/me/email", userController);
        httpServer.register("DELETE", "/api/users/me/email", userController);
        httpServer.register("PUT", "/api/users/me/birth-date", userController);
        httpServer.register("DELETE", "/api/users/me/birth-date", userController);
        // مخاطبین
        ContactController contactController = new ContactController(contactService, sessionManager);
        httpServer.register("GET", "/api/contacts", contactController);
        httpServer.register("POST", "/api/contacts", contactController);
        httpServer.register("DELETE", "/api/contacts", contactController);
        // گروه‌ها
        GroupController groupController = new GroupController(groupService, sessionManager);
        httpServer.register("GET", "/api/groups", groupController);
        httpServer.register("POST", "/api/groups", groupController);
        httpServer.register("DELETE", "/api/groups", groupController);
        httpServer.register("PUT", "/api/groups", groupController);
        // تنظیمات
        SettingsController settingsController = new SettingsController(settingsService, sessionManager);
        httpServer.register("GET", "/api/settings", settingsController);
        httpServer.register("PUT", "/api/settings/dark-mode", settingsController);
        // ری‌اکشن‌ها اخ جون
        ReactionController reactionController = new ReactionController(reactionService, sessionManager);
        httpServer.register("GET", "/api/messages", reactionController);
        httpServer.register("DELETE", "/api/messages", reactionController);
        httpServer.register("POST", "/api/messages", reactionController);
        // رسانه‌ها
        MediaController mediaController = new MediaController(mediaService, sessionManager);
        httpServer.register("DELETE", "/api/media", mediaController);
        httpServer.register("POST", "/api/media", mediaController);
        httpServer.register("GET", "/api/media", mediaController);
        httpServer.register("PUT", "/api/media", mediaController);
        // بخش سرچ که امتیازی
        SearchController searchController = new SearchController(searchService, sessionManager);
        httpServer.register("GET", "/api/search/chats", searchController);
        httpServer.register("GET", "/api/search/messages", searchController);
        // استوری
        StoryController storyController = new StoryController(storyService, sessionManager);
        httpServer.register("GET", "/api/stories/feed", storyController);
        httpServer.register("GET", "/api/stories/user", storyController);
        httpServer.register("DELETE", "/api/stories", storyController);
        httpServer.register("POST", "/api/stories", storyController);
        httpServer.register("GET", "/api/stories", storyController);

        httpServer.start();

        ChatWebSocketServer wsServer = new ChatWebSocketServer(WS_PORT, sessionManager, userService);
        wsServer.start();

        // اتصال دیرهنگام سرویس پیام‌ها به سرور سوکت
        // تا بتواند بعد از ذخیره‌ی موفق هر پیامآن را برادکست کند
        messageService.setWebSocketServer(wsServer);

        System.out.println("Server is running. HTTP: " + HTTP_PORT + ", WS: " + WS_PORT);
    }
}