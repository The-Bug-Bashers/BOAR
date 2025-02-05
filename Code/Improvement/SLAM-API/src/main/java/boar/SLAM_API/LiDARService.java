package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;

public class LiDARService {
    private SerialPort serialPort;

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        serialPort.setBaudRate(115200);
    }

    public void startMotor() {
        if (!serialPort.isOpen()) {
            boolean success = serialPort.openPort();
            if (!success) {
                throw new RuntimeException("Failed to open serial port!");
            }
        }
        serialPort.setDTR();  // ✅ Correctly enables motor
    }

    public void stopMotor() {
        if (serialPort.isOpen()) {
            serialPort.clearDTR();  // ✅ Correctly stops motor
            serialPort.closePort();
        }
    }

    public void startScanning() {
        startMotor();  // Make sure motor is running
        sendCommand((byte) 0xA5, (byte) 0x20);  // ✅ Start scanning
    }

    public void stopScanning() {
        sendCommand((byte) 0xA5, (byte) 0x25);  // ✅ Stop scanning
        stopMotor();  // Turn off motor after stopping scanning
    }

    private void sendCommand(byte... command) {
        if (!serialPort.isOpen()) {
            System.err.println("Error: Serial port is not open!");
            return;
        }
        serialPort.writeBytes(command, command.length);
    }
}
