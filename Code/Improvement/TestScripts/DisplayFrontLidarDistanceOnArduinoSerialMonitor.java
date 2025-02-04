import com.fazecast.jSerialComm.SerialPort;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DisplayFrontLidarDistanceOnArduinoSerialMonitor {
    private static final String LIDAR_PORT = "/dev/ttyUSB0";  // USB port for RPLIDAR
    private static final String ARDUINO_PORT = "/dev/serial1"; // UART to Arduino

    public static void main(String[] args) {
        SerialPort lidarPort = SerialPort.getCommPort(LIDAR_PORT);
        SerialPort arduinoPort = SerialPort.getCommPort(ARDUINO_PORT);

        lidarPort.setBaudRate(115200);
        arduinoPort.setBaudRate(9600);

        if (!lidarPort.openPort() || !arduinoPort.openPort()) {
            System.out.println("Error: Could not open ports.");
            return;
        }

        System.out.println("LIDAR (USB) and Arduino (UART) connected.");

        try {
            byte[] requestScan = {(byte) 0xA5, 0x20};  // Start RPLIDAR scanning
            lidarPort.writeBytes(requestScan, requestScan.length);

            byte[] buffer = new byte[5];
            while (true) {
                if (lidarPort.bytesAvailable() > 0) {
                    lidarPort.readBytes(buffer, buffer.length);

                    // Decode RPLIDAR data (simplified for distance at 0°)
                    int angle = buffer[2] & 0xFF;
                    int distance = ByteBuffer.wrap(new byte[]{buffer[3], buffer[4]})
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .getShort();

                    if (angle == 0) {  // Object directly in front
                        System.out.println("Distance: " + distance + " mm");

                        // Send distance to Arduino
                        String message = distance + "\n";
                        arduinoPort.writeBytes(message.getBytes(), message.length());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lidarPort.closePort();
            arduinoPort.closePort();
        }
    }
}
