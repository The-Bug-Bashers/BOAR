package boar.SLAM_API;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LiDARWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LiDARService lidarService = new LiDARService();
    private ScheduledExecutorService scheduler;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String command = jsonNode.get("command").asText();

            switch (command) {
                case "start":
                    lidarService.startScanning();
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning started\"}"));
                    break;
                case "stop":
                    lidarService.stopScanning();
                    stopDistanceStreaming();
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning stopped\"}"));
                    break;
                case "dstart":
                    if (scheduler == null || scheduler.isShutdown()) {
                        scheduler = Executors.newSingleThreadScheduledExecutor();
                        scheduler.scheduleAtFixedRate(() -> sendDistance(session), 0, 1, TimeUnit.SECONDS);
                    }
                    session.sendMessage(new TextMessage("{\"message\": \"Distance streaming started\"}"));
                    break;
                case "dstop": // ✅ New command to stop distance streaming
                    stopDistanceStreaming();
                    session.sendMessage(new TextMessage("{\"message\": \"Distance streaming stopped\"}"));
                    break;
                default:
                    session.sendMessage(new TextMessage("{\"error\": \"Unknown command\"}"));
            }
        } catch (Exception e) {
            session.sendMessage(new TextMessage("{\"error\": \"Internal server error: " + e.getMessage() + "\"}"));
            e.printStackTrace();
        }
    }

    private void sendDistance(WebSocketSession session) {
        try {
            int frontDistance = lidarService.getFrontDistance();
            session.sendMessage(new TextMessage("{\"distance_cm\": " + frontDistance + "}"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopDistanceStreaming() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            scheduler = null;
        }
    }
}
