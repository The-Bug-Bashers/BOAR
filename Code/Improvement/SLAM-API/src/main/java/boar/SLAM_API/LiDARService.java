package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;

import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;


public class LiDARService {
    private final SerialPort serialPort;

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        if (!serialPort.openPort()) {
            throw new RuntimeException("Error: LiDAR not detected on /dev/ttyUSB0!");
        }

        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 200, 0);

        serialPort.setBaudRate(115200);
        openSerialPort();
        stopMotor();
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

    public void startScanning() {
        if (serialPort.isOpen()) {
            startMotor();
            sendCommand((byte) 0xA5, (byte) 0x20); // Start scanning
        } else {
            throw new IllegalStateException("Cannot start scanning without an open port");
        }
    }

    public void stopScanning() {
        if (serialPort.isOpen()) {
            sendCommand((byte) 0xA5, (byte) 0x25); // Stop scanning
            stopMotor();
        } else {
            throw new IllegalStateException("Cannot stop scanning without an open port");
        }
    }

    private void openSerialPort() {
        if (!serialPort.isOpen()) {
            boolean success = serialPort.openPort();
            if (!success) {
                throw new RuntimeException("Failed to open serial port!");
            }
        }
    }

    private void sendCommand(byte... command) {
        if (serialPort.isOpen()) {
            serialPort.writeBytes(command, command.length);
        } else {
            throw new IllegalStateException("Cannot write bytes without initialized port");
        }
    }

    public void readDistanceData(WebSocketSession session) {
        new Thread(() -> {
            try (InputStream inputStream = serialPort.getInputStream()) {
                byte[] buffer = new byte[256];
                while (serialPort.isOpen()) {
                    int numBytes = inputStream.read(buffer);

                    if (numBytes <= 0) {
                        System.err.println("Error: no data received");
                        continue;
                    }

                    processLiDARData(buffer, numBytes, session);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void processLiDARData(byte[] data, int length, WebSocketSession session) {
        for (int i = 0; i < length - 4; i += 5) {
            if ((data[i] & 0x01) == 0) {
                System.out.println("LiDAR data not valid: " + new String(data));
            } else {
                //int angle = ((data[i + 1] & 0xFF) | ((data[i + 2] & 0x7F) << 8)) / 64; TODO: remove if proofed false
                int angle = (((data[i + 2] & 0xFF) << 8) | (data[i + 1] & 0xFF)) / 64;
                int distance = ((data[i + 3] & 0xFF) | ((data[i + 4] & 0xFF) << 8));

                if (distance > 0) {
                    sendWebSocketMessage(session, String.format("{\"angle\": %d, \"distance\": %d}", angle, distance));
                } else {
                    sendWebSocketMessage(session, String.format("{\"angle\": %d, \"falseDistance\": %d}", angle, distance));
                }
            }
        }
    }

    private synchronized void sendWebSocketMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
