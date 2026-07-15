package server;

import services.UserService;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

// سرور وب سوکتم
public class ChatWebSocketServer {
    private final int port;
    private final SessionManager sessionManager;
    private final UserService userService;
    private ServerSocket serverSocket;
    private volatile boolean running = false;
    // اتصال های فعال هر کاربر
    private final Map<String, List<WsConnection>> connections = new ConcurrentHashMap<>();
    private static final String WS_magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public ChatWebSocketServer(int port, SessionManager sessionManager, UserService userService) {
        this.port = port;
        this.sessionManager = sessionManager;
        this.userService = userService;
    }

    // اینجا سرور شروع میشه
    public void start() {
        running = true;
        Thread acceptThread = new Thread(new AcceptLoop());
        acceptThread.start();
    }

    // حلقه پذیرش اتصال
    private class AcceptLoop implements Runnable {
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("WebSocket server started on port " + port);
                while (running) {
                    Socket client = serverSocket.accept();
                    Thread thread = new Thread(new ConnectionHandler(client));
                    thread.start(); // برای هر کلاینت یه ترد جدا
                }
            } catch (IOException e) {

            }
        }
    }

    // اینجا سرور متوقف میشه
    public void stop() throws IOException {
        running = false;
        serverSocket.close();
    }

    // پردازش یک اتصال در ترد جدا انجام میشه
    private class ConnectionHandler implements Runnable {
        private final Socket socket;

        ConnectionHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            handleConnection(socket);
        }
    }

    // اینجا وقت حذف یعنی اتصال کاربر حذف میشه
    private void removeConnection(String idUser, Socket socket) {
        List<WsConnection> list = connections.get(idUser);
        if (list == null)
            return;
        for (WsConnection c : list) {
            if (c.socket == socket) {
                list.remove(c);
                break;
            }
        }
        if (list.isEmpty()) {
            connections.remove(idUser);
            userService.setOffline(idUser);
        }
    }

    // و اینجا تمام کار های که برای مدیریت بخش اتصال لازمه پیاده شده
    private void handleConnection(Socket socket) {
        String idUser = null;
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = socket.getOutputStream();
            String requestLine = reader.readLine();
            if (requestLine == null) {
                socket.close();
                return;
            }
            Map<String, String> header = readHeaders(reader);
            String wsKey = header.get("Sec-WebSocket-Key");
            if (wsKey == null) {
                socket.close();
                return;
            }
            // بررسی توکن کاربر
            String token = extractToken(requestLine);
            Optional<models.User> optUser = sessionManager.validate(token);
            if (optUser.isEmpty()) {
                out.write("HTTP/1.1 401 Unauthorized\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                socket.close();
                return;
            }
            idUser = optUser.get().getId();
            // پاسخ هندشیک
            String acceptKey = generateAcceptKey(wsKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n" + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();
            // ثبت اتصال کاربر
            WsConnection connection = new WsConnection(socket, out);
            List<WsConnection> list = connections.get(idUser);
            if (list == null) {
                list = new CopyOnWriteArrayList<WsConnection>();
                connections.put(idUser, list);
            }
            list.add(connection);
            userService.setOnline(idUser);
            // حلقه خواندن
            InputStream in = socket.getInputStream();
            while (true) {
                Frame frame = readFrame(in);
                if (frame == null || frame.opcode == 0x8) {
                    break; // اتصال قطع شد عزیزم
                }
                if (frame.opcode == 0x9) {
                    sendFrame(out, 0xA, frame.payload); // pong
                }
            }
        } catch (Exception e) {

        } finally {
            if (idUser != null) {
                removeConnection(idUser, socket);
            }
            try {
                socket.close();
            } catch (IOException e) {

            }
        }
    }

    // میفرستیم به کاربر
    public void sendToUser(String idUser, String jsonPayload) {
        List<WsConnection> list = connections.get(idUser);
        if (list == null)
            return;
        for (WsConnection connection : list) {
            try {
                synchronized (connection) {
                    sendFrame(connection.out, 0x1, jsonPayload.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException e) {
                // هرجا از اینا دیدی بدون قطع شدم که ارور میدم
            }
        }
    }

    // چک میشه که انلاین هست یا نه
    public boolean isOnline(String idUser) {
        List<WsConnection> list = connections.get(idUser);
        return list != null && !list.isEmpty();
    }

    // ارسال به چند کاربر
    public void sendToUsers(List<String> idUsers, String jsonPayload) {
        for (String idUser : idUsers) {
            sendToUser(idUser, jsonPayload);
        }
    }

    // خواندن هدر ها
    private Map<String, String> readHeaders(BufferedReader reader) throws IOException {
        Map<String, String> header = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                header.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return header;
    }

    // تولید کلید هندشیک
    private String generateAcceptKey(String wsKey) throws Exception {
        String combined = wsKey + WS_magic;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    // استخراج توکن از یو آر ال
    private String extractToken(String requestLine) {
        int qIndex = requestLine.indexOf('?');
        if (qIndex == -1)
            return null;
        int spaceIndex = requestLine.indexOf(' ', qIndex);
        String query = spaceIndex == -1 ? requestLine.substring(qIndex + 1)
                : requestLine.substring(qIndex + 1, spaceIndex);
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals("token")) {
                return parts[1];
            }
        }
        return null;
    }

    // خواندن یک فریم
    private Frame readFrame(InputStream in) throws IOException {
        int b1 = in.read();
        if (b1 == -1)
            return null;
        int opcode = b1 & 0x0F;
        int b2 = in.read();
        if (b2 == -1)
            return null;
        boolean masked = (b2 & 0x80) != 0;
        long lenPayload = b2 & 0x7F;
        if (lenPayload == 126) {
            lenPayload = ((in.read() & 0xFF) << 8) | (in.read() & 0xFF);
        } else if (lenPayload == 127) {
            lenPayload = 0;
            for (int i = 0; i < 8; i++) {
                lenPayload = (lenPayload << 8) | (in.read() & 0xFF);
            }
        }
        byte[] maskKey = new byte[4];
        if (masked) {
            if (in.read(maskKey) != 4)
                return null;
        }
        byte[] payload = new byte[(int) lenPayload];
        int total = 0;
        while (total < payload.length) {
            int r = in.read(payload, total, payload.length - total);
            if (r == -1)
                return null;
            total += r;
        }
        // باز کردن ماسک
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }
        return new Frame(opcode, payload);
    }

    // نوشتن یک فریم
    private void sendFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        out.write(0x80 | opcode);
        int len = payload.length;
        if (len <= 125) {
            out.write(len);
        } else if (len <= 65535) {
            out.write(126);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) {
                out.write((int) ((len >> (8 * i)) & 0xFF));
            }
        }
        out.write(payload);
        out.flush();
    }

    // یک فریم خونده شده
    private static final class Frame {
        final int opcode;
        final byte[] payload;

        Frame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }
    }

    // یک اتصال فعال
    private static final class WsConnection {
        final Socket socket;
        final OutputStream out;

        WsConnection(Socket socket, OutputStream out) {
            this.socket = socket;
            this.out = out;
        }
    }
}