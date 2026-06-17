package server;

import repository.file.*;
import services.*;
import security.RateLimiter;
import java.io.IOException;

// راه‌اندازی تمام اجزای سرور و وصل کردن آن‌ها به هم
public class ServerBootstrap {

    private static final int HTTP_PORT = 8080;
    private static final int WS_PORT = 8081;

    public static void start() throws IOException {

        // ۱. ساخت repository ها
        FileUserRepository userRepo = new FileUserRepository();
        FileChatRepository chatRepo = new FileChatRepository();
        FileMessageRepository messageRepo = new FileMessageRepository();
        FileGroupRepository groupRepo = new FileGroupRepository();
        FileContactRepository contactRepo = new FileContactRepository();
        FileSessionRepository sessionRepo = new FileSessionRepository();
        FileReportedMessageRepository reportRepo = new FileReportedMessageRepository();
        FileSettingsRepository settingsRepo = new FileSettingsRepository();

        // ۲. ساخت service ها
        RateLimiter rateLimiter = new RateLimiter();
        ChatService chatService = new ChatService(chatRepo);
        AuthService authService = new AuthService(userRepo, sessionRepo, chatService);
        UserService userService = new UserService(userRepo);
        MessageService messageService = new MessageService(messageRepo, chatRepo, reportRepo, rateLimiter);
        GroupService groupService = new GroupService(groupRepo, chatRepo, userRepo);
        ContactService contactService = new ContactService(contactRepo, userRepo);
        AdminService adminService = new AdminService(userRepo, groupRepo, reportRepo, groupService);
        SettingsService settingsService = new SettingsService(userRepo, settingsRepo);

        // ۳. ساخت session manager
        SessionManager sessionManager = new SessionManager(authService);

        // ۴. ساخت و راه‌اندازی HTTP server
        HttpApiServer httpServer = new HttpApiServer(HTTP_PORT);
        // فاز ۱: کنترلرها اینجا register می‌شوند
        // httpServer.register("POST", "/api/auth/login", new AuthController(...));
        httpServer.start();

        // ۵. ساخت و راه‌اندازی WebSocket server
        ChatWebSocketServer wsServer = new ChatWebSocketServer(WS_PORT, sessionManager);
        wsServer.start();

        System.out.println("Server is running. HTTP: " + HTTP_PORT + ", WS: " + WS_PORT);
    }
}