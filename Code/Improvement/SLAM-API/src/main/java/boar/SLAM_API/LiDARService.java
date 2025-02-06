package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.net.PortUnreachableException;

public class LiDARService {
    private final SerialPort serialPort;

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        if (!serialPort.openPort()) {throw new RuntimeException("Error: LiDAR not detected on /dev/ttyUSB0!");}

        serialPort.setBaudRate(115200);

        openSerialPort();
        stopMotor();
    }

    public void startMotor() {
        if (serialPort.isOpen()) {
            serialPort.clearDTR();
        } else {
            throw new IllegalStateException("Can not start motor without open port");
        }
    }

    public void stopMotor() {
        if (serialPort.isOpen()) {
            serialPort.setDTR();
        } else {
            throw new IllegalStateException("Can not stop motor without open port");
        }
    }

    public void startScanning() {
        if (serialPort.isOpen()) {
            startMotor();
            sendCommand((byte) 0xA5, (byte) 0x20); // Start scanning
        } else {
            throw new IllegalStateException("Can not start scanning without open port");
        }

    }

    public void stopScanning() {
        if (serialPort.isOpen()) {
            sendCommand((byte) 0xA5, (byte) 0x25); // Stop scanning
            stopMotor();
        } else {
            throw new IllegalStateException("Can not stop scanning without open port");
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
            throw new IllegalStateException("Can not write bytes without initialised port");
        }
    }

    private int parseDistance(byte[] data) {
        return ((data[3] & 0xFF) | ((data[4] & 0xFF) << 8)) / 4; // LiDAR distance parsing logic
    }

    public int getFrontDistance() {
        if (!serialPort.isOpen()) return -1;

        InputStream inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[5];

        try {
            long startTime = System.currentTimeMillis();
            int bestDistance = -1;

            while (System.currentTimeMillis() - startTime < 1000) {  // Timeout after 1 second
                if (inputStream.available() >= 5) {  // Ensure we have enough bytes
                    int readBytes = inputStream.read(buffer);
                    if (readBytes == 5) {
                        // Correctly extract the angle
                        int angleRaw = ((buffer[1] & 0xFF) | ((buffer[2] & 0xFF) << 8));
                        float angle = (angleRaw >> 6) * 0.01f;  // Convert to degrees

                        // Correctly extract the distance
                        int distanceRaw = ((buffer[3] & 0xFF) | ((buffer[4] & 0xFF) << 8));
                        int distance = (distanceRaw / 4);  // Distance in mm

                        // Check if this reading is from the front (angle ~0°)
                        if (angle >= 355 || angle <= 5) {
                            if (distance > 0 && (bestDistance == -1 || distance < bestDistance)) {
                                bestDistance = distance;
                            }
                        }
                    }
                } else {
                    Thread.sleep(10);  // Avoid CPU overuse, wait for data
                }
            }

            return bestDistance;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }




}
