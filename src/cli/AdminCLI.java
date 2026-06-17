package cli;

import models.Group;
import models.GroupMember;
import models.Message;
import models.ReportedMessage;
import models.User;
import security.PasswordHasher;
import services.AdminService;
import services.MessageService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * رابط خط فرمان (CLI) برای تعامل ادمین با سیستم.
 *
 * ادمین با وارد کردن username و password از پیش تعریف‌شده وارد این محیط
 * می‌شود و می‌تواند کاربران، گروه‌ها و پیام‌های گزارش‌شده را مدیریت کند.
 * این اطلاعات ربطی به حساب‌های کاربری عادی سیستم ندارند.
 *
 * تمام منطق کسب‌وکار در {AdminService} پیاده‌سازی شده است؛ این کلاس
 * فقط مسئول گرفتن ورودی از کاربر (ادمین)، نمایش منو و چاپ خروجی است.
 */
public class AdminCLI {

    // نام متغیرهای محیطی که اطلاعات ورود ادمین از آن‌ها خوانده می‌شود.
    // قرار دادن این مقادیر در کد به‌صورت متن ساده ریسک امنیتی دارد؛
    // به همین دلیل ابتدا از Environment Variables خوانده می‌شوند.
    private static final String ENV_ADMIN_USERNAME = "ADMIN_USERNAME";
    private static final String ENV_ADMIN_PASSWORD = "ADMIN_PASSWORD";

    // مقادیر پیش‌فرض، فقط برای حالتی که متغیر محیطی تنظیم نشده باشد
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    // رمز عبور پیش‌فرض به‌صورت هش‌شده (با همان الگوریتم PasswordHasher)
    // نگه‌داری می‌شود تا حتی در حالت پیش‌فرض هم متن ساده در کد نباشد."
    private static final String DEFAULT_ADMIN_PASSWORD_HASH =
            PasswordHasher.hash("Admin@123");

    private final String adminUsername;
    private final String adminPasswordHash; // در صورت استفاده از مقدار محیطی، در لحظه هش می‌شود
    private final boolean usingEnvCredentials;

    // حداکثر تعداد تلاش مجاز برای ورود به CLI
    private static final int MAX_LOGIN_ATTEMPTS = 3;

    private final AdminService adminService;
    private final MessageService messageService;
    private final Scanner scanner;

    public AdminCLI(AdminService adminService, MessageService messageService) {
        this.adminService = adminService;
        this.messageService = messageService;
        this.scanner = new Scanner(System.in);

        String envUsername = System.getenv(ENV_ADMIN_USERNAME);
        String envPassword = System.getenv(ENV_ADMIN_PASSWORD);

        if (envUsername != null && !envUsername.isBlank()
                && envPassword != null && !envPassword.isBlank()) {
            // اگر متغیرهای محیطی تنظیم شده باشند، رمز عبور وارد شده توسط
            // ادمین در لحظه ورود با هش همین مقدار مقایسه می‌شود.
            this.adminUsername = envUsername;
            this.adminPasswordHash = PasswordHasher.hash(envPassword);
            this.usingEnvCredentials = true;
        } else {
            // در غیر این صورت، از مقدار پیش‌فرض (فقط برای محیط توسعه) استفاده می‌شود.
            this.adminUsername = DEFAULT_ADMIN_USERNAME;
            this.adminPasswordHash = DEFAULT_ADMIN_PASSWORD_HASH;
            this.usingEnvCredentials = false;
        }
    }

    // اجرای حلقه اصلی CLI //

    /**
     * نقطه شروع اجرای CLI.
     * ابتدا احراز هویت ادمین انجام می‌شود و در صورت موفقیت، منوی اصلی نمایش
     * داده می‌شود.
     */
    public void run() {
        System.out.println("===== Admin CLI =====");
        if (!usingEnvCredentials) {
            System.out.println("[Warning] ADMIN_USERNAME/ADMIN_PASSWORD environment variables "
                    + "are not set. Falling back to development default credentials. "
                    + "Set these environment variables before deploying to production.");
        }

        if (!authenticate()) {
            System.out.println("Too many failed attempts. Exiting Admin CLI.");
            return;
        }

        System.out.println("Login successful. Welcome, admin.");
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine("Select an option: ");
            try {
                running = handleChoice(choice);
            } catch (IllegalArgumentException | IllegalStateException e) {
                // خطاهای کنترل‌شده از سرویس‌ها به صورت پیام ساده نمایش داده می‌شوند
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                // هر خطای غیرمنتظره دیگر نیز بدون متوقف کردن CLI گزارش می‌شود
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }

        System.out.println("Exiting Admin CLI. Goodbye.");
    }

    // احراز هویت ادمین //

    /**
     * نام کاربری و رمز عبور ادمین را می‌گیرد و با مقادیر بارگذاری‌شده
     * (از Environment Variables یا مقدار پیش‌فرض) مقایسه می‌کند.
     * رمز عبور هرگز به‌صورت متن ساده مقایسه نمی‌شود؛ مقایسه 
     *  روی مقدار هش‌شده انجام می‌شود.
     * true در صورت ورود موفق، false اگر تعداد تلاش‌های ناموفق از حد
     *         مجاز بگذرد.
     */
    private boolean authenticate() {
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String username = readLine("Username: ");
            String password = readLine("Password: ");

            boolean usernameMatches = adminUsername.equals(username);
            boolean passwordMatches = PasswordHasher.verify(password, adminPasswordHash);

            if (usernameMatches && passwordMatches) {
                return true;
            }

            System.out.println("Invalid username or password. Attempts left: "
                    + (MAX_LOGIN_ATTEMPTS - attempt));
        }
        return false;
    }

    // منو //

    private void printMenu() {
        System.out.println();
        System.out.println("---------------------------------");
        System.out.println(" 1. List all users");
        System.out.println(" 2. Add user");
        System.out.println(" 3. Delete user");
        System.out.println(" 4. List all groups");
        System.out.println(" 5. List group members");
        System.out.println(" 6. Create group");
        System.out.println(" 7. Delete group");
        System.out.println(" 8. Add user to group");
        System.out.println(" 9. Remove user from group");
        System.out.println("10. View reported messages");
        System.out.println("11. Dismiss a report");
        System.out.println(" 0. Exit");
        System.out.println("---------------------------------");
    }

    /**
     * بر اساس گزینه انتخاب‌شده، عملیات مربوطه را اجرا می‌کند.
     *  false اگر کاربر گزینه خروج را انتخاب کرده باشد، در غیر این
     *         صورت true تا حلقه اصلی ادامه یابد.
     */
    private boolean handleChoice(String choice) {
        switch (choice.trim()) {
            case "1":
                listUsers();
                return true;
            case "2":
                addUser();
                return true;
            case "3":
                deleteUser();
                return true;
            case "4":
                listGroups();
                return true;
            case "5":
                listGroupMembers();
                return true;
            case "6":
                createGroup();
                return true;
            case "7":
                deleteGroup();
                return true;
            case "8":
                addUserToGroup();
                return true;
            case "9":
                removeUserFromGroup();
                return true;
            case "10":
                viewReportedMessages();
                return true;
            case "11":
                dismissReport();
                return true;
            case "0":
                return false;
            default:
                System.out.println("Invalid option. Please try again.");
                return true;
        }
    }

    // مدیریت کاربران //

    private void listUsers() {
        List<User> users = adminService.getAllUsers();
        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }
        System.out.println("Total users: " + users.size());
        for (User user : users) {
            System.out.printf(" - id=%s | username=%s | online=%s | deleted=%s%n",
                    user.getId(), user.getUsername(), user.isOnline(), user.isDeleted());
        }
    }

    private void addUser() {
        String userId = readLine("New user ID: ");
        String username = readLine("Username: ");
        String password = readLine("Password: ");

        User created = adminService.addUser(userId, username, password);
        System.out.println("User created successfully: " + created);
    }

    private void deleteUser() {
        String userId = readLine("User ID to delete: ");
        if (!confirm("Are you sure you want to delete user '" + userId + "'? This cannot be undone.")) {
            System.out.println("Cancelled.");
            return;
        }
        adminService.deleteUser(userId);
        System.out.println("User deleted: " + userId);
    }

    // مدیریت گروه‌ها //

    private void listGroups() {
        List<Group> groups = adminService.getAllGroups();
        if (groups.isEmpty()) {
            System.out.println("No groups found.");
            return;
        }
        System.out.println("Total groups: " + groups.size());
        for (Group group : groups) {
            System.out.printf(" - id=%s | name=%s | ownerId=%s%n",
                    group.getId(), group.getName(), group.getOwnerId());
        }
    }

    private void listGroupMembers() {
        String groupId = readLine("Group ID: ");
        List<GroupMember> members = adminService.getGroupMembers(groupId);
        if (members.isEmpty()) {
            System.out.println("No members found for this group.");
            return;
        }
        System.out.println("Members of group " + groupId + ":");
        for (GroupMember member : members) {
            System.out.printf(" - userId=%s | role=%s%n", member.getUserId(), member.getRole());
        }
    }

    private void createGroup() {
        String name = readLine("Group name: ");
        String ownerId = readLine("Owner user ID: ");
        Group group = adminService.createGroup(name, ownerId);
        System.out.println("Group created successfully: " + group);
    }

    private void deleteGroup() {
        String groupId = readLine("Group ID to delete: ");
        if (!confirm("Are you sure you want to delete group '" + groupId + "'? This cannot be undone.")) {
            System.out.println("Cancelled.");
            return;
        }
        adminService.deleteGroup(groupId);
        System.out.println("Group deleted: " + groupId);
    }

    private void addUserToGroup() {
        String groupId = readLine("Group ID: ");
        String userId = readLine("User ID to add: ");
        adminService.addUserToGroup(groupId, userId);
        System.out.println("User " + userId + " added to group " + groupId);
    }

    private void removeUserFromGroup() {
        String groupId = readLine("Group ID: ");
        String userId = readLine("User ID to remove: ");
        if (!confirm("Remove user '" + userId + "' from group '" + groupId + "'?")) {
            System.out.println("Cancelled.");
            return;
        }
        adminService.removeUserFromGroup(groupId, userId);
        System.out.println("User " + userId + " removed from group " + groupId);
    }

    // پیام‌های گزارش‌شده //

    private void viewReportedMessages() {
        List<ReportedMessage> reports = adminService.getReportedMessages();
        if (reports.isEmpty()) {
            System.out.println("No reported messages.");
            return;
        }
        System.out.println("Total reports: " + reports.size());
        for (ReportedMessage report : reports) {
            String senderId = resolveSenderId(report.getMessageId());
            System.out.printf(" - reportId=%s | messageId=%s | sender=%s | reportedBy=%s | reason=%s%n",
                    report.getId(), report.getMessageId(), senderId,
                    report.getReporterId(), report.getReason());
        }
    }

    /**
     * با استفاده از MessageService، فرستنده‌ی پیام گزارش‌شده را پیدا می‌کند.
     * اگر پیام حذف یا یافت نشود، یک مقدار قابل‌فهم برمی‌گردد به‌جای خطا دادن.
     */
    private String resolveSenderId(String messageId) {
        if (messageService == null) {
            return "unknown";
        }
        Optional<Message> message = messageService.findById(messageId);
        return message.map(Message::getSenderId).orElse("unknown (message not found)");
    }

    private void dismissReport() {
        String reportId = readLine("Report ID to dismiss: ");
        if (!confirm("Dismiss report '" + reportId + "'? This cannot be undone.")) {
            System.out.println("Cancelled.");
            return;
        }
        adminService.dismissReport(reportId);
        System.out.println("Report dismissed: " + reportId);
    }

    // کمکی //

    /**
     * برای عملیات حساس و غیرقابل‌برگشت (مانند حذف کاربر یا گروه)، یک
     * تأییدیه صریح از ادمین می‌گیرد تا یک اشتباه تایپی ساده باعث از
     * دست رفتن داده نشود. فقط پاسخ‌های "y" یا "yes" (بدون حساسیت به
     * بزرگی/کوچکی حروف) به‌عنوان تأیید پذیرفته می‌شوند.
     */
    private boolean confirm(String message) {
        String answer = readLine(message + " [y/N]: ");
        return answer != null && (answer.trim().equalsIgnoreCase("y")
                || answer.trim().equalsIgnoreCase("yes"));
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}