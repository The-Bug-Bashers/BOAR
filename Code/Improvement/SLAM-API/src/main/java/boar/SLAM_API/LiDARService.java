package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LiDARService {
    private final SerialPort serialPort;
    private final BlockingQueue<String> dataQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        if (!serialPort.openPort()) {
            throw new RuntimeException("Error: LiDAR not detected on /dev/ttyUSB0!");
        }
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);
        serialPort.setBaudRate(115200);
        stopMotor();
        startContinuousReading();
    }

    private void startContinuousReading() {
        new Thread(() -> {
            try (InputStream inputStream = serialPort.getInputStream()) {
                byte[] buffer = new byte[256];
                while (running) {
                    int numBytes = inputStream.read(buffer);
                    if (numBytes > 0) {
                        processLiDARData(buffer, numBytes);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processLiDARData(byte[] data, int length) {
        for (int i = 0; i < length - 4; i += 5) {
            if ((data[i] & 0x01) == 0) {
                System.out.println("LiDAR data not valid: " + new String(data));
            } else {
                int rawAngle = ((data[i + 2] & 0xFF) << 7) | ((data[i + 1] & 0xFF) >> 1);
                double angle = rawAngle / 64.0;
                int distance = ((data[i + 3] & 0xFF) | ((data[i + 4] & 0xFF) << 8));
                if (distance > 0) {
                    String message = String.format("{\"angle\": %.2f, \"distance\": %d}", angle, distance);
                    dataQueue.offer(message);
                }
            }
        }
    }

    public String getLatestData() {
        return dataQueue.poll();
    }

    public void startMotor() {
        if (serialPort.isOpen()) {
            serialPort.clearDTR();
        } else {
            throw new IllegalStateException("Cannot start motor without an open port");
        }
    }

    public void stopMotor() {
        if (serialPort.isOpen()) {
            serialPort.setDTR();
        } else {
            throw new IllegalStateException("Cannot stop motor without an open port");
        }
    }

    public void close() {
        running = false;
        stopMotor();
        if (serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}
