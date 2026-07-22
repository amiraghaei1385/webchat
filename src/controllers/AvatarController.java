package controllers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.Group;
import models.User;
import server.HttpApiServer;
import server.RequestContext;
import server.SessionManager;
import services.GroupService;
import services.UserService;
import java.io.*;
import java.util.Optional;

// کنترلر عکس پروفایل و عکس گروه
public class AvatarController implements HttpHandler {
    private final UserService userserv;
    private final GroupService groupserv;
    private final SessionManager sessionmanager;

    public AvatarController(UserService userService, GroupService groupService, SessionManager sessionManager) {
        this.userserv = userService;
        this.groupserv = groupService;
        this.sessionmanager = sessionManager;
    }

    public void handle(HttpExchange exchange) throws IOException {
        RequestContext ctx = new RequestContext(exchange);
        String path = ctx.getPath();
        User requester = sessionmanager.validate(ctx.getSessionToken()).orElse(null);
        if (requester == null) {
            HttpApiServer.sendResponse(exchange, 401, "{\"error\":\"Unauthorized.\"}");
            return;
        }
        String[] parts = path.split("/");
        boolean isgrouppath = parts.length >= 5 && parts[3].equals("group");
        if (isgrouppath) {
            String idgroup = parts[4];
            handleGroupAvatar(exchange, idgroup);
            return;
        }
        if (parts.length >= 4) {
            String iduser = parts[3];
            handleUserAvatar(exchange, iduser);
            return;
        }
        HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Avatar not found.\"}");
    }

    // آواتار گروه
    private void handleGroupAvatar(HttpExchange exchange, String idgroup) throws IOException {
        Optional<Group> found = groupserv.findById(idgroup);
        if (found.isEmpty()) {
            HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Avatar not found.\"}");
            return;
        }
        String picpath = found.get().getPicturePath();
        sendPictureFile(exchange, picpath);
    }

    // آواتار کاربر
    private void handleUserAvatar(HttpExchange exchange, String iduser) throws IOException {
        Optional<User> found = userserv.findById(iduser);
        if (found.isEmpty()) {
            HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Avatar not found.\"}");
            return;
        }
        String picpath = found.get().getProfilePicPath();
        sendPictureFile(exchange, picpath);
    }

    // خواندن و فرستادن فایل عکس
    private void sendPictureFile(HttpExchange exchange, String picpath) throws IOException {
        if (picpath == null || picpath.isEmpty()) {
            HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Avatar not found.\"}");
            return;
        }
        File file = new File(picpath);
        if (!file.exists()) {
            HttpApiServer.sendResponse(exchange, 404, "{\"error\":\"Avatar not found.\"}");
            return;
        }
        int readtotal = 0;
        FileInputStream in = new FileInputStream(file);
        int filelength = (int) file.length();
        byte[] filebytes = new byte[filelength];
        while (readtotal < filelength) {
            int readnow = in.read(filebytes, readtotal, filelength - readtotal);
            if (readnow == -1) {
                break;
            }
            readtotal += readnow;
        }
        in.close();
        String mime = "application/octet-stream";
        String lowerpath = picpath.toLowerCase();
        if (lowerpath.endsWith(".png")) {
            mime = "image/png";
        } else if (lowerpath.endsWith(".jpg") || lowerpath.endsWith(".jpeg")) {
            mime = "image/jpeg";
        } else if (lowerpath.endsWith(".gif")) {
            mime = "image/gif";
        } else if (lowerpath.endsWith(".webp")) {
            mime = "image/webp";
        }
        exchange.getResponseHeaders().add("Content-Type", mime);
        exchange.sendResponseHeaders(200, filebytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(filebytes);
        out.close();
    }
}