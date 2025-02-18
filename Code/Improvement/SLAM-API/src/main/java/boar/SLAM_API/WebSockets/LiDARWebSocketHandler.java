package boar.SLAM_API.WebSockets;

import boar.SLAM_API.LiDARService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class LiDARWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LiDARService lidarService = new LiDARService();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String command = jsonNode.get("command").asText();

            if ("printLatestData".equals(command)) {
                String data = lidarService.getLatestData();
                session.sendMessage(new TextMessage(data));
            } else {
                session.sendMessage(new TextMessage("{\"error\": \"Unknown command!\"}"));
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("{\"error\": \"Internal server error: " + e.getMessage() + "\"}"));
            e.printStackTrace();
        }
    }
}
