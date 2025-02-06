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
        byte[] buffer = new byte[5]; // Read 5 bytes per point
        int frontDistance = -1;
        double closestAngle = 360; // Start with max angle

        try {
            while (inputStream.available() > 0) {
                int readBytes = inputStream.read(buffer);
                if (readBytes == 5) {
                    boolean isNewScan = (buffer[0] & 0x01) != 0;
                    boolean isValid = (buffer[0] & 0x02) != 0;
                    double angle = ((buffer[1] & 0xFF) | ((buffer[2] & 0xFF) << 8)) / 64.0;
                    int distance = ((buffer[3] & 0xFF) | ((buffer[4] & 0xFF) << 8)) / 4; // mm to cm

                    if (isNewScan && isValid) {
                        // Find the closest angle to 0°
                        if (Math.abs(angle) < closestAngle) {
                            closestAngle = Math.abs(angle);
                            frontDistance = distance;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return frontDistance;
    }

}
