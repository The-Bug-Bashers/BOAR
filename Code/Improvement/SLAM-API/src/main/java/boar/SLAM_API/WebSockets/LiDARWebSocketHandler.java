package boar.SLAM_API.WebSockets;

import boar.SLAM_API.LiDARService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.PortUnreachableException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LiDARWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    final LiDARService lidarService = new LiDARService();
    private ScheduledExecutorService scheduler;
    private static boolean scanning = false;
    private Thread streamingThread;
    private volatile boolean streaming = false;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String command = jsonNode.get("command").asText();

            switch (command) {
                case "startScann":
                    if (scanning) {session.sendMessage(new TextMessage("{\"error\": \"Already scanning!\"}")); break;}
                    lidarService.startScanning();
                    scanning = true;
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning started\"}"));
                    break;
                case "stopScann":
                    if (!scanning) {session.sendMessage(new TextMessage("{\"error\": \"No active scann to stop!\"}")); break;}
                    lidarService.stopScanning();
                    stopDistanceStreaming();
                    scanning = false;
                    session.sendMessage(new TextMessage("{\"message\": \"LiDAR scanning stopped\"}"));
                    break;
                case "startStreamFrontDistance":
                    if (!streaming) {
                        streaming = true;
                        streamingThread = new Thread(() -> {
                            while (streaming && session.isOpen()) {
                                try {
                                    // This call blocks until a new full rotation is processed.
                                    int frontDistance = lidarService.getFrontDistance();
                                    session.sendMessage(new TextMessage("{\"distance_cm\": " + frontDistance + "}"));
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

    private void streamDistance(WebSocketSession session) {
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
