import cli.AdminCLI;
import repository.file.*;
import security.RateLimiter;
import server.ServerBootstrap;
import services.*;

import java.io.IOException;

// نقطه‌ی ورود برنامه.
public class Main {

    public static void main(String[] args) {

        // ۱. ساخت repository ها — فقط یک بار
        FileUserRepository userRepo = new FileUserRepository();
        FileChatRepository chatRepo = new FileChatRepository();
        FileMessageRepository messageRepo = new FileMessageRepository();
        FileGroupRepository groupRepo = new FileGroupRepository();
        FileContactRepository contactRepo = new FileContactRepository();
        FileSessionRepository sessionRepo = new FileSessionRepository();
        FileReportedMessageRepository reportRepo = new FileReportedMessageRepository();
        FileSettingsRepository settingsRepo = new FileSettingsRepository();

        // ۲. ساخت service ها — فقط یک بار
        RateLimiter rateLimiter = new RateLimiter();
        ChatService chatService = new ChatService(chatRepo);
        AuthService authService = new AuthService(userRepo, sessionRepo, chatService);
        UserService userService = new UserService(userRepo);
        MessageService messageService = new MessageService(messageRepo, chatRepo, reportRepo, rateLimiter);
        GroupService groupService = new GroupService(groupRepo, chatRepo, userRepo);
        ContactService contactService = new ContactService(contactRepo, userRepo);
        AdminService adminService = new AdminService(userRepo, groupRepo, reportRepo, groupService);
        SettingsService settingsService = new SettingsService(userRepo, settingsRepo);

        // ۳. راه‌اندازی سرور در یک daemon thread — همون service ها رو پاس می‌ده
        Thread serverThread = new Thread(() -> {
            try {
                ServerBootstrap.start(
                        authService, userService, chatService, messageService,
                        groupService, contactService, adminService, settingsService);
            } catch (IOException e) {
                System.err.println("[Main] Failed to start server: " + e.getMessage());
                System.exit(1);
            }
        }, "server-main");
        serverThread.setDaemon(true);
        serverThread.start();

        // ۴. Shutdown Hook
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> System.out.println("\n[Main] Shutting down..."), "shutdown-hook"));

        // ۵. اجرای CLI در thread اصلی — همون service ها رو استفاده می‌کنه
        AdminCLI adminCLI = new AdminCLI(adminService, messageService);
        adminCLI.run();

        System.exit(0);
    }
}