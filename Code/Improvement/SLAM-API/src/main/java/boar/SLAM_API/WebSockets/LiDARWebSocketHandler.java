package boar.SLAM_API.WebSockets;

import boar.SLAM_API.LiDARService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class LiDARWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    final LiDARService lidarService = new LiDARService();
    // Use a dedicated thread for streaming
    private Thread streamingThread;
    private volatile boolean streaming = false;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String command = jsonNode.get("command").asText();

            switch (command) {
                case "startScan":
                    // Start scanning if not already scanning.
                    lidarService.startScanning();
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning started\"}"));
                    break;
                case "stopScan":
                    lidarService.stopScanning();
                    stopDistanceStreaming();
                    streaming = false;
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning stopped\"}"));
                    break;
                case "startStreamDistance":
                    if (!streaming) {
                        streaming = true;
                        lidarService.startScanning();
                        lidarService.readDistanceData(session);
                        session.sendMessage(new TextMessage("{\"message\": \"Distance streaming started\"}"));
                    } else {
                        session.sendMessage(new TextMessage("{\"error\": \"Already streaming distance!\"}"));
                    }
                    break;
                case "stopStreamDistance":
                    if (streaming) {
                        streaming = false;
                        stopDistanceStreaming();
                        session.sendMessage(new TextMessage("{\"message\": \"Distance streaming stopped\"}"));
                    } else {
                        session.sendMessage(new TextMessage("{\"error\": \"Not streaming distance!\"}"));
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

    private void stopDistanceStreaming() {
        streaming = false;
        if (streamingThread != null && streamingThread.isAlive()) {
            streamingThread.interrupt();
            streamingThread = null;
        }
    }
}
