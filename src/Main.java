import cli.AdminCLI;
import repository.file.*;
import security.RateLimiter;
import server.ServerBootstrap;
import services.*;
import java.io.IOException;

/**
 * نقطه‌ی ورود برنامه.
 * نحوه اجرا:
 * java Main
 */
public class Main {

    public static void main(String[] args) {

        // ۱. راه‌اندازی سرور در یک daemon thread جداگانه
        Thread serverThread = new Thread(() -> {
            try {
                ServerBootstrap.start();
            } catch (IOException e) {
                System.err.println("[Main] Failed to start server: " + e.getMessage());
                System.exit(1);
            }
        }, "server-main");
        serverThread.setDaemon(true);
        serverThread.start();

        // ۲. ساخت وابستگی‌های CLI ادمین
        FileUserRepository userRepo = new FileUserRepository();
        FileGroupRepository groupRepo = new FileGroupRepository();
        FileMessageRepository messageRepo = new FileMessageRepository();
        FileChatRepository chatRepo = new FileChatRepository();
        FileReportedMessageRepository reportRepo = new FileReportedMessageRepository();

        RateLimiter rateLimiter = new RateLimiter();
        GroupService groupService = new GroupService(groupRepo, chatRepo, userRepo);
        AdminService adminService = new AdminService(userRepo, groupRepo, reportRepo, groupService);
        MessageService messageService = new MessageService(messageRepo, chatRepo, reportRepo, rateLimiter);

        // ۳. ثبت Shutdown Hook برای خاموش کردن تمیز برنامه
        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> System.out.println("\n[Main] Shutting down..."), "shutdown-hook"));

        // ۴. اجرای CLI در thread اصلی
        // (برنامه تا زمانی که ادمین از CLI خارج نشود اینجا می‌ماند)
        AdminCLI adminCLI = new AdminCLI(adminService, messageService);
        adminCLI.run();

        System.exit(0);
    }
}