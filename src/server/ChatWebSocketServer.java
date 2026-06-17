package server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

// سرور WebSocket برای ارتباط real-time
public class ChatWebSocketServer {

    private final int port;
    private final SessionManager sessionManager;
    private ServerSocket serverSocket;

    // نگه‌داری اتصال‌های فعال: userId -> socket
    private final Map<String, Socket> connections = new ConcurrentHashMap<>();

    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public ChatWebSocketServer(int port, SessionManager sessionManager) {
        this.port = port;
        this.sessionManager = sessionManager;
    }

    // راه‌اندازی سرور در یک thread جداگانه
    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                System.out.println("WebSocket server started on port " + port);
                while (!serverSocket.isClosed()) {
                    Socket client = serverSocket.accept();
                    // هر اتصال در یک thread جداگانه مدیریت می‌شود
                    new Thread(() -> handleConnection(client)).start();
                }
            } catch (IOException e) {
                System.err.println("WebSocket server error: " + e.getMessage());
            }
        }, "ws-server").start();
    }

    public void stop() throws IOException {
        if (serverSocket != null)
            serverSocket.close();
    }

    // مدیریت یک اتصال WebSocket
    private void handleConnection(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            OutputStream out = socket.getOutputStream();

            // خواندن درخواست HTTP upgrade
            Map<String, String> headers = readHeaders(reader);
            String wsKey = headers.get("Sec-WebSocket-Key");

            if (wsKey == null) {
                socket.close();
                return;
            }

            // ارسال پاسخ handshake
            String acceptKey = generateAcceptKey(wsKey);
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";
            out.write(response.getBytes(StandardCharsets.UTF_8));
            out.flush();

        } catch (Exception e) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    // خواندن هدرهای HTTP
    private Map<String, String> readHeaders(BufferedReader reader) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int idx = line.indexOf(":");
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return headers;
    }

    // تولید کلید پاسخ WebSocket طبق استاندارد RFC 6455
    private String generateAcceptKey(String wsKey) throws Exception {
        String combined = wsKey + WS_GUID;
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}