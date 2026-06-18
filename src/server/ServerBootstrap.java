package server;

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
        // فاز ۱: کنترلرها اینجا register می‌شوند
        httpServer.start();

        ChatWebSocketServer wsServer = new ChatWebSocketServer(WS_PORT, sessionManager);
        wsServer.start();

        System.out.println("Server is running. HTTP: " + HTTP_PORT + ", WS: " + WS_PORT);
    }
}
