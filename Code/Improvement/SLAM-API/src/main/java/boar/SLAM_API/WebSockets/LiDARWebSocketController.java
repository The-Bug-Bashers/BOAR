import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LiDARWebSocketController {

    @MessageMapping("/lidar")
    @SendTo("/topic/lidarData")
    public String sendLiDARData(String message) {
        // This method can be used to handle messages from clients if needed
        return message;
    }
}
