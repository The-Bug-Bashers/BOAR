import com.fazecast.jSerialComm.*;

public class RPLidarScanner {
    public static void main(String[] args) {
        SerialPort lidarPort = SerialPort.getCommPort("/dev/ttyUSB0"); // Adjust if necessary
        lidarPort.setBaudRate(115200);
        lidarPort.setNumDataBits(8);
        lidarPort.setNumStopBits(1);
        lidarPort.setParity(SerialPort.NO_PARITY);

        if (!lidarPort.openPort()) {
            System.out.println("Failed to open port!");
            return;
        }

        try {
            // Send command to start scanning (0xA5 0x20)
            lidarPort.getOutputStream().write(new byte[]{(byte) 0xA5, 0x20});
            lidarPort.getOutputStream().flush();

            // Read incoming scan data
            byte[] buffer = new byte[5];  // LIDAR responses start with a descriptor
            lidarPort.getInputStream().read(buffer, 0, 5);

            if (buffer[0] != (byte) 0xA5 || buffer[1] != 0x5A) {
                System.out.println("Unexpected response from LiDAR.");
                return;
            }

            System.out.println("LiDAR scanning...");

            byte[] data = new byte[5]; // Each data frame consists of 5 bytes per point
            while (lidarPort.getInputStream().read(data) == 5) {
                int angle_q6 = ((data[1] & 0xFF) | ((data[2] & 0x7F) << 8)); // Angle (Q6 format)
                double angle = angle_q6 / 64.0;  // Convert to degrees
                int distance_mm = ((data[3] & 0xFF) | (data[4] & 0xFF) << 8); // Distance in mm

                if (distance_mm > 0) {  // Ignore invalid readings
                    System.out.println("{" + angle + "; " + distance_mm + "}");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lidarPort.closePort();
        }

        System.out.println("Scan complete.");
    }
}
