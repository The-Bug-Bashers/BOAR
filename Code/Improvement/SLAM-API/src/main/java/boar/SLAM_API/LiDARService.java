package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;

public class LiDARService {
    private SerialPort serialPort;

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        serialPort.setBaudRate(115200);
    }

    public void startScanning() {
        if (!serialPort.isOpen()) {
            boolean success = serialPort.openPort();
            if (!success) {
                throw new RuntimeException("Failed to open serial port!");
            }
        }
        sendCommand((byte) 0xA5, (byte) 0x60); // ✅ Corrected: Start scanning
    }

    public void stopScanning() {
        if (serialPort.isOpen()) {
            sendCommand((byte) 0xA5, (byte) 0x65); // ✅ Corrected: Stop scanning
            serialPort.closePort();
        }
    }

    public int getFrontDistance() {
        if (!serialPort.isOpen()) return -1;

        InputStream inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[9];
        try {
            int readBytes = inputStream.read(buffer);
            if (readBytes > 0) {
                return parseDistance(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void sendCommand(byte... command) {
        if (!serialPort.isOpen()) {
            System.err.println("Error: Serial port is not open!");
            return;
        }
        serialPort.writeBytes(command, command.length);
    }

    private int parseDistance(byte[] data) {
        return ((data[3] & 0xFF) | ((data[4] & 0xFF) << 8)) / 4;
    }
}
