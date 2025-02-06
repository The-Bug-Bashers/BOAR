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
        byte[] buffer = new byte[5];  // LiDAR packets are usually 5 bytes per measurement
        try {
            int bestDistance = -1;
            int readBytes;

            while ((readBytes = inputStream.read(buffer)) > 0) {
                if (readBytes == 5) {  // Ensure full packet
                    int angle = ((buffer[1] & 0xFF) | ((buffer[2] & 0xFF) << 8)) >> 6; // Extract angle
                    int distance = ((buffer[3] & 0xFF) | ((buffer[4] & 0xFF) << 8)) / 4; // Extract distance

                    if (angle >= 355 || angle <= 5) {  // Front-facing data
                        if (distance > 0 && (bestDistance == -1 || distance < bestDistance)) {
                            bestDistance = distance;
                        }
                    }
                }
            }
            return bestDistance;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


}
