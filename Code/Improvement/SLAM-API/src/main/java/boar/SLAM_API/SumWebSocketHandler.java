package boar.SLAM_API;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class SumWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            // Parse JSON message
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            int a = jsonNode.get("a").asInt();
            int b = jsonNode.get("b").asInt();
            int sum = a + b;

            // Send result back as JSON
            String response = objectMapper.writeValueAsString(new SumResponse(sum));
            session.sendMessage(new TextMessage(response));
        } catch (Exception e) {
            session.sendMessage(new TextMessage("{\"error\": \"Invalid input\"}"));
        }
    }

    // Helper class for JSON response
    private static class SumResponse {
        public int sum;

        public SumResponse(int sum) {
            this.sum = sum;
        }
    }
}
