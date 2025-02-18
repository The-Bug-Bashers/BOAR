package boar.SLAM_API.WebSockets;

import boar.SLAM_API.LiDARService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class CommandWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LiDARService lidarService = new LiDARService();
    private final AtomicReference<WebSocketSession> currentSession = new AtomicReference<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (currentSession.compareAndSet(null, session)) {
            System.out.println("Command client connected: " + session.getId());
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"Another client is already connected.\"}"));
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        String command = jsonNode.get("command").asText();

        if ("printLatestData".equals(command)) {
            String data = lidarService.getLatestData();
            session.sendMessage(new TextMessage(data));
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"Unknown command!\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (currentSession.compareAndSet(session, null)) {
            System.out.println("Command client disconnected: " + session.getId());
        }
    }
}
