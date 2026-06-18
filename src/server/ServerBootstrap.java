package server;

import controllers.*;
import services.*;
import java.io.IOException;

// راه‌اندازی تمام اجزای سرور و وصل کردن آن‌ها به هم
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
            SettingsService settingsService) throws IOException {

        SessionManager sessionManager = new SessionManager(authService);

        HttpApiServer httpServer = new HttpApiServer(HTTP_PORT);

        // ثبت کنترلرها (فاز ۱: حداقل یک مسیر برای هر صفحه)
        AuthController authController = new AuthController(authService, sessionManager);
        httpServer.register("POST", "/api/auth/login", authController);
        httpServer.register("POST", "/api/auth/register", authController);
        httpServer.register("POST", "/api/auth/logout", authController);

        ChatController chatController = new ChatController(chatService, messageService, sessionManager);
        httpServer.register("GET", "/api/chats", chatController);
        httpServer.register("POST", "/api/chats/private", chatController);

        UserController userController = new UserController(userService, sessionManager);
        httpServer.register("GET", "/api/users", userController);

        ContactController contactController = new ContactController(contactService, sessionManager);
        httpServer.register("GET", "/api/contacts", contactController);
        httpServer.register("POST", "/api/contacts", contactController);

        GroupController groupController = new GroupController(groupService, sessionManager);
        httpServer.register("GET", "/api/groups", groupController);
        httpServer.register("POST", "/api/groups", groupController);

        httpServer.start();

        ChatWebSocketServer wsServer = new ChatWebSocketServer(WS_PORT, sessionManager);
        wsServer.start();

        System.out.println("Server is running. HTTP: " + HTTP_PORT + ", WS: " + WS_PORT);
    }
}