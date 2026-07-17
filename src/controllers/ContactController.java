package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Contact;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.ContactService;
import java.io.IOException;
import java.util.List;

// کنترلر مخاطبین و بلاک کردن
public class ContactController implements HttpHandler {
    private final ContactService contactserv;
    private final SessionManager sessionmanager;

    public ContactController(ContactService contactService, SessionManager sessionManager) {
        this.contactserv = contactService;
        this.sessionmanager = sessionManager;
    }

    // ورودی اصلی درخواست‌ها
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);

        // چک احراز هویت واسه همه‌ی مسیرهای این کنترلر
        User user = sessionmanager.validate(ctx.getSessionToken()).orElse(null);
        if (user == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        String path = ctx.getPath();
        String method = ctx.getMethod();
        try {
            if (method.equals("POST") && path.equals("/api/contacts")) {
                doAddContact(ctx, user);
            } else if (method.equals("GET") && path.equals("/api/contacts")) {
                doGetContacts(ctx, user);
            } else if (method.equals("DELETE") && path.matches("/api/contacts/[^/]+")) {
                doRemoveContact(ctx, user);
            } else if (method.equals("GET") && path.equals("/api/contacts/blocked")) {
                doGetBlockedUsers(ctx, user);
            } else if (method.equals("GET") && path.matches("/api/contacts/[^/]+/blocked")) {
                doCheckBlocked(ctx, user);
            } else if (method.equals("POST") && path.matches("/api/contacts/[^/]+/block")) {
                doBlockUser(ctx, user);
            } else if (method.equals("DELETE") && path.matches("/api/contacts/[^/]+/block")) {
                doUnblockUser(ctx, user);
            } else {
                HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Not found.\"}");
            }
        } catch (IllegalArgumentException e) {
            HttpApiServer.sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (IllegalStateException e) {
            HttpApiServer.sendResponse(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            HttpApiServer.sendResponse(exchange, 500, "{\"error\":\"Internal server error.\"}");
        }
    }

    // اینجا مخاطب اضافه میشه
    private void doAddContact(RequestContext ctx, User user) throws IOException {
        String idcontact = getStr(ctx.getBody(), "contactId");
        Contact contact = contactserv.addContact(user.getId(), idcontact);
        HttpApiServer.sendResponse(ctx.getExchange(), 201, contactToJson(contact));
    }

    // اینجا مخاطب حذف میشه
    private void doRemoveContact(RequestContext ctx, User user) throws IOException {
        String idcontact = getIdFromPath(ctx.getPath(), 3);
        contactserv.removeContact(user.getId(), idcontact);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"Contact removed.\"}");
    }

    // اینجا لیست مخاطبین برگردونده میشه
    private void doGetContacts(RequestContext ctx, User user) throws IOException {
        List<Contact> contacts = contactserv.getContacts(user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < contacts.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(contactToJson(contacts.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // بلاک و آنبلاک 
    // بلاک کردن یه کاربر
    private void doBlockUser(RequestContext ctx, User user) throws IOException {
        String idtarget = getIdFromPath(ctx.getPath(), 3);
        contactserv.blockUser(user.getId(), idtarget);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"User blocked.\"}");
    }

    // اینجا لیست کاربران بلاک شده برگردونده میشه
    private void doGetBlockedUsers(RequestContext ctx, User user) throws IOException {
        List<Contact> listblocked = contactserv.getBlockedUsers(user.getId());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < listblocked.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(contactToJson(listblocked.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // چک بلاک بودن
    private void doCheckBlocked(RequestContext ctx, User user) throws IOException {
        String idtarget = getIdFromPath(ctx.getPath(), 3);
        boolean block = contactserv.isBlocked(user.getId(), idtarget);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"isBlocked\":" + block + "}");
    }

    // آنبلاک کردن یه کاربر
    private void doUnblockUser(RequestContext ctx, User user) throws IOException {
        String idtarget = getIdFromPath(ctx.getPath(), 3);
        contactserv.unblockUser(user.getId(), idtarget);
        HttpApiServer.sendResponse(ctx.getExchange(), 200, "{\"message\":\"User unblocked.\"}");
    }

    // تبدیل به جیسون //
    private String contactToJson(Contact contact) {
        return "{\"ownerId\":\"" + contact.getOwnerId() + "\","
                + "\"contactId\":\"" + contact.getContactId() + "\","
                + "\"nickname\":\"" + escape(contact.getNickname()) + "\","
                + "\"isBlocked\":" + contact.isBlocked() + "}";
    }

    // کمکی //
    private String getIdFromPath(String path, int index) {
        String[] parts = path.split("/");
        return parts.length > index ? parts[index] : "";
    }

    // خواندن مقدار رشته‌ای از جیسون دستی
    private String getStr(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx == -1)
            return "";
        int vs = idx + search.length();
        while (vs < json.length() && json.charAt(vs) == ' ')
            vs++;
        if (vs >= json.length() || json.charAt(vs) != '"')
            return "";
        int start = vs + 1;
        int end = json.indexOf('"', start);
        return end == -1 ? "" : json.substring(start, end);
    }

    // فرار دادن کاراکترهای خاص
    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}