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
    // Instead of a ScheduledExecutorService, we use a dedicated thread.
    private Thread streamingThread;
    private volatile boolean streaming = false;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String command = jsonNode.get("command").asText();

            switch (command) {
                case "startScann":
                    if (streaming) {
                        session.sendMessage(new TextMessage("{\"error\": \"Already scanning!\"}"));
                        break;
                    }
                    lidarService.startScanning();
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning started\"}"));
                    break;
                case "stopScann":
                    if (!streaming) {
                        session.sendMessage(new TextMessage("{\"error\": \"No active scan to stop!\"}"));
                        break;
                    }
                    lidarService.stopScanning();
                    // Also stop the streaming thread if running.
                    stopDistanceStreaming();
                    streaming = false;
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning stopped\"}"));
                    break;
                case "startStreamFrontDistance":
                    if (!streaming) {
                        streaming = true;
                        streamingThread = new Thread(() -> {
                            while (streaming && session.isOpen()) {
                                try {
                                    // This call will block until a full rotation is complete and a valid front distance is obtained.
                                    int frontDistance = lidarService.getFrontDistance();
                                    // Send the distance (in mm) as a JSON message.
                                    session.sendMessage(new TextMessage("{\"distance_mm\": " + frontDistance + "}"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        streamingThread.start();
                        session.sendMessage(new TextMessage("{\"message\": \"Distance streaming started\"}"));
                    } else {
                        session.sendMessage(new TextMessage("{\"error\": \"Already streaming front distance!\"}"));
                    }
                    break;
                case "stopStreamFrontDistance":
                    if (streaming) {
                        streaming = false;
                        if (streamingThread != null) {
                            streamingThread.interrupt();
                        }
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
