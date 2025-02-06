package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;

public class LiDARService {
    private final SerialPort serialPort;

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        if (!serialPort.openPort()) {
            throw new RuntimeException("Error: LiDAR not detected on /dev/ttyUSB0!");
        }
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

    /**
     * This method reads measurement packets continuously and, for one full 360° scan,
     * picks the smallest valid distance from the “front” (i.e. if angle is ≥350° or ≤10°).
     * Debug logging prints each raw measurement.
     * It detects the start of a new rotation using the start flag in the quality byte.
     *
     * @return the best (smallest) front distance (in millimeters) from one complete rotation.
     */
    public int getFrontDistance() {
        if (!serialPort.isOpen()) return -1;

        InputStream inputStream = serialPort.getInputStream();
        byte[] buffer = new byte[5];
        int bestDistance = -1;
        boolean firstScanStarted = false;
        // Accept measurements if angle is ≥350° OR ≤10°
        float lowerAngleBound = 350.0f;
        float upperAngleBound = 10.0f;

        while (true) {
            try {
                if (inputStream.available() >= 5) {
                    int readBytes = inputStream.read(buffer);
                    if (readBytes == 5) {
                        // Assume the lowest bit of buffer[0] holds the "start flag"
                        boolean newScan = (buffer[0] & 0x01) != 0;

                        // Convert the angle from Q6 fixed-point format (divide by 64.0)
                        int angleRaw = ((buffer[1] & 0xFF) | ((buffer[2] & 0xFF) << 8));
                        float angle = angleRaw / 64.0f;  // angle in degrees

                        // Extract distance (per protocol, divide raw by 4)
                        int distanceRaw = ((buffer[3] & 0xFF) | ((buffer[4] & 0xFF) << 8));
                        int distance = distanceRaw / 4;

                        // Debug logging: print each measurement
                        System.out.println("Raw measurement: angle=" + angle + "°, distance=" + distance + " mm, newScan=" + newScan);

                        // When a new scan is detected and we've been processing a rotation already,
                        // return the accumulated best front distance if any valid measurement was seen.
                        if (newScan) {
                            if (firstScanStarted) {
                                if (bestDistance != -1) {
                                    System.out.println("Returning best front distance: " + bestDistance + " mm");
                                    return bestDistance;
                                }
                                // No valid front measurement in this rotation; reset and continue.
                                bestDistance = -1;
                            }
                            firstScanStarted = true;
                        }

                        // Check if the measurement is in the front region.
                        // We accept it if the angle is ≥350° OR ≤10°.
                        if (angle >= lowerAngleBound || angle <= upperAngleBound) {
                            if (distance > 0 && (bestDistance == -1 || distance < bestDistance)) {
                                bestDistance = distance;
                                System.out.println("Updated best front distance: " + bestDistance + " mm");
                            }
                        }
                    }
                } else {
                    Thread.sleep(1); // short sleep to prevent busy-waiting
                }
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }
}
