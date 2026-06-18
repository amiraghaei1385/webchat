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

// مدیریت مخاطبین 
public class ContactController implements HttpHandler {

    private final ContactService contactService;
    private final SessionManager sessionManager;

    public ContactController(ContactService contactService, SessionManager sessionManager) {
        this.contactService = contactService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);

        // بررسی احراز هویت برای تمام endpoint های این کنترلر
        User user = sessionManager.validate(ctx.getSessionToken()).orElse(null);
        if (user == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }

        String path = ctx.getPath();
        String method = ctx.getMethod();

        try {
            if ("POST".equals(method) && "/api/contacts".equals(path)) {
                handleAddContact(ctx, user);

            } else if ("GET".equals(method) && "/api/contacts".equals(path)) {
                handleGetContacts(ctx, user);

            } else {
                HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Not found.\"}");
            }
        } catch (IllegalArgumentException e) {
            HttpApiServer.sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            HttpApiServer.sendResponse(exchange, 500, "{\"error\":\"Internal server error.\"}");
        }
    }

    // افزودن مخاطب //
    // افزودن کاربر دیگر به لیست مخاطبین، با وارد کردن آیدی او
    private void handleAddContact(RequestContext ctx, User user) throws IOException {
        String contactId = parseStr(ctx.getBody(), "contactId");

        Contact contact = contactService.addContact(user.getId(), contactId);
        HttpApiServer.sendResponse(ctx.getExchange(), 201, contactToJson(contact));
    }

    // دریافت لیست مخاطبین //
    private void handleGetContacts(RequestContext ctx, User user) throws IOException {
        List<Contact> contacts = contactService.getContacts(user.getId());

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < contacts.size(); i++) {
            if (i > 0)
                sb.append(",");
            sb.append(contactToJson(contacts.get(i)));
        }
        sb.append("]");
        HttpApiServer.sendResponse(ctx.getExchange(), 200, sb.toString());
    }

    // تبدیل مدل به JSON //
    private String contactToJson(Contact contact) {
        return "{\"ownerId\":\"" + contact.getOwnerId() + "\","
                + "\"contactId\":\"" + contact.getContactId() + "\","
                + "\"nickname\":\"" + escape(contact.getNickname()) + "\","
                + "\"isBlocked\":" + contact.isBlocked() + "}";
    }

    // کمکی //
    // استخراج مقدار string از JSON بدون کتابخانه خارجی
    private String parseStr(String json, String key) {
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

    private String escape(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}