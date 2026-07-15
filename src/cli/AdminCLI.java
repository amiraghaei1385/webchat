package cli;

import models.Group;
import models.GroupMember;
import models.Message;
import models.ReportedMessage;
import models.User;
import security.PasswordHasher;
import services.AdminService;
import services.MessageService;
import java.util.*;

// کلاس اصلی ادمین
public class AdminCLI {
    private static final String ENV_adminuser = "ADMIN_USERNAME";
    private static final String ENV_adminpass = "ADMIN_PASSWORD";
    private static final String default_AD_user = "admin";
    private static final String default_AD_passhash = PasswordHasher.hash("Admin@123");
    private final String adminUsername;
    private final String adminPasswordHash;
    private final boolean usingEnvCredentials;
    private static final int MAX_logincount = 3;
    private final AdminService adminservice;
    private final MessageService messageservice;
    private final Scanner scanner;

    // سازنده
    public AdminCLI(AdminService adminservice, MessageService messageservice) {
        this.adminservice = adminservice;
        this.messageservice = messageservice;
        this.scanner = new Scanner(System.in);
        // env
        String envUsername = System.getenv(ENV_adminuser);
        String envPassword = System.getenv(ENV_adminpass);
        if (envUsername != null && !envUsername.isBlank()
                && envPassword != null && !envPassword.isBlank()) {
            this.adminUsername = envUsername;
            this.adminPasswordHash = PasswordHasher.hash(envPassword);
            this.usingEnvCredentials = true;
        } else {
            // استفاده پیش فرض
            this.adminUsername = default_AD_user;
            this.adminPasswordHash = default_AD_passhash;
            this.usingEnvCredentials = false;
        }
    }

    // اجرای اصلی
    // متد ران که برنامه رو شروع میکنه
    public void run() {
        System.out.println("===== Admin CLI =====");
        // اگه ای ان وی نباشه هشدار میده
        if (!usingEnvCredentials) {
            System.out.println("[Warning] ADMIN_USERNAME/ADMIN_PASSWORD environment variables "
                    + "are not set. Falling back to development default credentials. "
                    + "Set these environment variables before deploying to production.");
        }
        // احراز هویت
        boolean loginOk = authenticate();
        if (!loginOk) {
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
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }

        System.out.println("Exiting Admin CLI. Goodbye.");
    }

    // اینجا یوزر و پس رو میگیریم و چک میکنیم
    private boolean authenticate() {
        int attempt = 1;
        while (attempt <= MAX_logincount) {
            String username = readLine("Username: ");
            String password = readLine("Password: ");
            boolean usernameMatches = adminUsername.equals(username);
            boolean passwordMatches = PasswordHasher.verify(password, adminPasswordHash);
            if (usernameMatches && passwordMatches) {
                return true;
            }
            int attemptsLeft = MAX_logincount - attempt;
            System.out.println("Invalid username or password. Attempts left: " + attemptsLeft);
            attempt = attempt + 1;
        }
        return false;
    }

    // منوی گزینه ها چاپ میشه
    private void printMenu() {
        System.out.println();
        System.out.println("---------------------------------");
        System.out.println(" 1. List all users");
        System.out.println(" 2. Add user");
        System.out.println(" 3. Delete user");
        System.out.println(" 4. Edit user");
        System.out.println(" 5. List all groups");
        System.out.println(" 6. List group members");
        System.out.println(" 7. Create group");
        System.out.println(" 8. Delete group");
        System.out.println(" 9. Edit group");
        System.out.println("10. Add user to group");
        System.out.println("11. Remove user from group");
        System.out.println("12. View reported messages");
        System.out.println("13. Dismiss a report");
        System.out.println(" 0. Exit");
        System.out.println("---------------------------------");
    }

    // اینجا هر کدوم از گزینه هارو که با عدد انتخاب میکنیمش برامون اجرا میکنه
    private boolean handleChoice(String choice) {
        String option = choice.trim();
        if (option.equals("1")) {
            listUsers();
            return true;
        }
        if (option.equals("2")) {
            addUser();
            return true;
        }
        if (option.equals("3")) {
            deleteUser();
            return true;
        }
        if (option.equals("4")) {
            editUser();
            return true;
        }
        if (option.equals("5")) {
            listGroups();
            return true;
        }
        if (option.equals("6")) {
            listGroupMembers();
            return true;
        }
        if (option.equals("7")) {
            createGroup();
            return true;
        }
        if (option.equals("8")) {
            deleteGroup();
            return true;
        }
        if (option.equals("9")) {
            editGroup();
            return true;
        }
        if (option.equals("10")) {
            addUserToGroup();
            return true;
        }
        if (option.equals("11")) {
            removeUserFromGroup();
            return true;
        }
        if (option.equals("12")) {
            viewReportedMessages();
            return true;
        }
        if (option.equals("13")) {
            dismissReport();
            return true;
        }
        if (option.equals("0")) {
            return false;
        }
        System.out.println("Invalid option. Please try again.");
        return true;
    }

    // مدیریت کاربران
    // چاپ لیست کاربران
    private void listUsers() {
        List<User> users = adminservice.getAllUsers();
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

    // کاربر جدید اضافه کردن
    private void addUser() {
        String iduser = readLine("New user ID: ");
        String user = readLine("Username: ");
        String pass = readLine("Password: ");
        User created = adminservice.addUser(iduser, user, pass);
        System.out.println("User created successfully: " + created);
    }

    // حذف کاربر
    private void deleteUser() {
        String iduser = readLine("User ID to delete: ");
        boolean sure = confirm("Are you sure you want to delete user '" + iduser + "'? This cannot be undone.");
        if (!sure) {
            System.out.println("Cancelled.");
            return;
        }
        adminservice.deleteUser(iduser);
        System.out.println("User deleted: " + iduser);
    }

    // ویرایش کاربر
    private void editUser() {
        String iduser = readLine("User ID to edit: ");
        String newuser = readLine("New username (leave empty to keep unchanged): ");
        String newiduser = readLine("New user ID (leave empty to keep unchanged): ");
        String usernameparam = null;
        if (!newuser.isBlank()) {
            usernameparam = newuser;
        }
        String useridparam = null;
        if (!newiduser.isBlank()) {
            useridparam = newiduser;
        }
        User update = adminservice.editUser(iduser, usernameparam, useridparam);
        System.out.println("User updated successfully: " + update);
    }

    // مدیریت گروه‌ها
    // چاپ لیست گروه‌ها انجام میشود
    private void listGroups() {
        List<Group> groups = adminservice.getAllGroups();
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

    // ساخت گروه جدید
    private void createGroup() {
        String name = readLine("Group name: ");
        String idowner = readLine("Owner user ID: ");
        Group group = adminservice.createGroup(name, idowner);
        System.out.println("Group created successfully: " + group);
    }

    // چاپ اعضای گروه انجام میشه
    private void listGroupMembers() {
        String idgroup = readLine("Group ID: ");
        List<GroupMember> members = adminservice.getGroupMembers(idgroup);
        if (members.isEmpty()) {
            System.out.println("No members found for this group.");
            return;
        }
        System.out.println("Members of group " + idgroup + ":");
        for (GroupMember member : members) {
            System.out.printf(" - userId=%s | role=%s%n", member.getUserId(), member.getRole());
        }
    }

    // حذف گروه
    private void deleteGroup() {
        String idgroup = readLine("Group ID to delete: ");
        boolean sure = confirm("Are you sure you want to delete group '" + idgroup + "'? This cannot be undone.");
        if (!sure) {
            System.out.println("Cancelled.");
            return;
        }
        adminservice.deleteGroup(idgroup);
        System.out.println("Group deleted: " + idgroup);
    }

    // ویرایش گروه
    private void editGroup() {
        String idgroup = readLine("Group ID to edit: ");
        String newname = readLine("New group name (leave empty to keep unchanged): ");
        String newdescription = readLine("New description (leave empty to keep unchanged): ");
        String nameparam = null;
        if (!newname.isBlank()) {
            nameparam = newname;
        }
        String descriptionParam = null;
        if (!newdescription.isBlank()) {
            descriptionParam = newdescription;
        }
        Group updated = adminservice.editGroup(idgroup, nameparam, descriptionParam);
        System.out.println("Group updated successfully: " + updated);
    }

    // افزودن کاربر به گروه
    private void addUserToGroup() {
        String idgroup = readLine("Group ID: ");
        String iduser = readLine("User ID to add: ");
        adminservice.addUserToGroup(idgroup, iduser);
        System.out.println("User " + iduser + " added to group " + idgroup);
    }

    // حذف کاربر از گروه
    private void removeUserFromGroup() {
        String idgroup = readLine("Group ID: ");
        String iduser = readLine("User ID to remove: ");
        boolean sure = confirm("Remove user '" + iduser + "' from group '" + idgroup + "'?");
        if (!sure) {
            System.out.println("Cancelled.");
            return;
        }
        adminservice.removeUserFromGroup(idgroup, iduser);
        System.out.println("User " + iduser + " removed from group " + idgroup);
    }

    // پیام‌های گزارش‌شده
    // چاپ پیام‌های گزارش
    private void viewReportedMessages() {
        List<ReportedMessage> reports = adminservice.getReportedMessages();
        if (reports.isEmpty()) {
            System.out.println("No reported messages.");
            return;
        }
        System.out.println("Total reports: " + reports.size());
        for (ReportedMessage report : reports) {
            String idsender = resolveSenderId(report.getMessageId());
            System.out.printf(" - reportId=%s | messageId=%s | sender=%s | reportedBy=%s | reason=%s%n",
                    report.getId(), report.getMessageId(), idsender,
                    report.getReporterId(), report.getReason());
        }
    }

    // فرستندهپیام پیدا میشه
    private String resolveSenderId(String messageId) {
        if (messageservice == null) {
            return "unknown";
        }
        Optional<Message> message = messageservice.findById(messageId);
        if (message.isEmpty()) {
            return "unknown (message not found)";
        }
        return message.get().getSenderId();
    }

    // گزارش رد میشه
    private void dismissReport() {
        String idreport = readLine("Report ID to dismiss: ");
        boolean sure = confirm("Dismiss report '" + idreport + "'? This cannot be undone.");
        if (!sure) {
            System.out.println("Cancelled.");
            return;
        }
        adminservice.dismissReport(idreport);
        System.out.println("Report dismissed: " + idreport);
    }

    // توابع کمکی
    // گرفتن تأییدیه از ادمین
    private boolean confirm(String message) {
        String answer = readLine(message + " [y/N]: ");
        if (answer == null) {
            return false;
        }
        String trimmed = answer.trim();
        if (trimmed.equalsIgnoreCase("y") || trimmed.equalsIgnoreCase("yes")) {
            return true;
        }
        return false;
    }

    // خواندن یک خط ورودی
    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }
}