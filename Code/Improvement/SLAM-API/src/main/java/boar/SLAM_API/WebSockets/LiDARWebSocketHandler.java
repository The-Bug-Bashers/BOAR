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

            switch (command) {
                case "getLatestData":
                    String latestData = lidarService.getLatestData();
                    if (latestData != null) {
                        session.sendMessage(new TextMessage(latestData));
                    } else {
                        session.sendMessage(new TextMessage("{\"message\": \"No new data available\"}"));
                    }
                    break;
                case "dumpData":
                    // On-demand full scan dump.
                    String dumpData = lidarService.dumpData();
                    if (dumpData != null && !dumpData.isEmpty()) {
                        session.sendMessage(new TextMessage(dumpData));
                    } else {
                        session.sendMessage(new TextMessage("{\"message\": \"No scan data available\"}"));
                    }
                    break;
                default:
                    session.sendMessage(new TextMessage("{\"error\": \"Unknown command!\"}"));
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("{\"error\": \"Internal server error: " + e.getMessage() + "\"}"));
            e.printStackTrace();
        }
    }
}
