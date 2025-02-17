package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LiDARService {
    private final SerialPort serialPort;
    private final BlockingQueue<String> dataQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    // A lock to ensure that the continuous thread and dumpData() do not conflict.
    private final Object serialLock = new Object();

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        if (!serialPort.openPort()) {
            throw new RuntimeException("Error: LiDAR not detected on /dev/ttyUSB0!");
        }
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);
        serialPort.setBaudRate(115200);
        startMotor();
        startScan();
        startContinuousReading();
    }

    private void startMotor() {
        if (serialPort.isOpen()) {
            serialPort.clearDTR(); // Starts the motor
        } else {
            throw new IllegalStateException("Cannot start motor without an open port");
        }
    }

    private void startScan() {
        if (serialPort.isOpen()) {
            byte[] startScanCommand = {(byte) 0xA5, 0x20};
            synchronized (serialLock) {
                serialPort.writeBytes(startScanCommand, startScanCommand.length);
            }
        } else {
            throw new IllegalStateException("Cannot start scan without an open port");
        }
    }

    /**
     * This continuous reading thread collects packets as they arrive.
     */
    private void startContinuousReading() {
        new Thread(() -> {
            try (InputStream inputStream = serialPort.getInputStream()) {
                byte[] buffer = new byte[256];
                while (running) {
                    int numBytes;
                    synchronized (serialLock) {
                        numBytes = inputStream.read(buffer);
                    }
                    if (numBytes > 0) {
                        System.out.println("Received bytes: " + numBytes);
                        processLiDARData(buffer, numBytes);
                    } else {
                        System.out.println("No data received...");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Processes incoming data into JSON messages and adds them to the queue.
     * (This method is used for the "getLatestData" command.)
     */
    private void processLiDARData(byte[] data, int length) {
        for (int i = 0; i < length - 4; i += 5) {
            // In our protocol the first byte’s LSB is the "start flag".
            if ((data[i] & 0x01) == 0) {
                System.out.println("LiDAR data not valid: " + new String(data));
            } else {
                // Angle is given as a Q6 value in two bytes.
                int angle_q6 = ((data[i + 1] & 0xFF) | ((data[i + 2] & 0x7F) << 8));
                double angle = angle_q6 / 64.0;
                int distance = ((data[i + 3] & 0xFF)) | ((data[i + 4] & 0xFF) << 8);
                if (distance > 0) {
                    String message = String.format("{\"angle\": %.2f, \"distance\": %d}", angle, distance);
                    dataQueue.offer(message);
                }
            }
        }
    }

    /**
     * Returns the latest data point from the continuous reading.
     */
    public String getLatestData() {
        return dataQueue.poll();
    }

    /**
     * Performs one full scan by issuing the start command, reading the header,
     * and then collecting packets until a full rotation is detected.
     * Data points are formatted as: {angle; distance} and separated by commas.
     */
    public String dumpData() {
        StringBuilder scanData = new StringBuilder();
        try (InputStream inputStream = serialPort.getInputStream()) {
            synchronized (serialLock) {
                // (Re)issue the start scan command so we know the stream is fresh.
                byte[] startScanCommand = {(byte) 0xA5, 0x20};
                serialPort.writeBytes(startScanCommand, startScanCommand.length);
                serialPort.getOutputStream().flush();

                // Read the LiDAR scan descriptor header (expecting 5 bytes).
                byte[] header = new byte[5];
                int headerBytes = inputStream.read(header, 0, 5);
                if (headerBytes != 5 || header[0] != (byte) 0xA5 || header[1] != 0x5A) {
                    return "{\"error\":\"Unexpected LiDAR header response.\"}";
                }

                boolean scanStarted = false;
                double initialAngle = 0.0;
                boolean fullScanCollected = false;
                while (!fullScanCollected) {
                    byte[] packet = new byte[5];
                    int count = inputStream.read(packet, 0, 5);
                    if (count != 5) {
                        continue; // Incomplete packet; try again.
                    }
                    // Verify the packet is valid (start flag in LSB of byte 0).
                    if ((packet[0] & 0x01) == 0) {
                        continue;
                    }
                    int angle_q6 = ((packet[1] & 0xFF) | ((packet[2] & 0x7F) << 8));
                    double angle = angle_q6 / 64.0;
                    int distance = ((packet[3] & 0xFF)) | ((packet[4] & 0xFF) << 8);
                    if (distance <= 0) {
                        continue; // Skip invalid measurements.
                    }
                    // If this packet has the start flag...
                    if ((packet[0] & 0x01) != 0) {
                        if (!scanStarted) {
                            scanStarted = true;
                            initialAngle = angle;
                            scanData.append(String.format("{%.2f; %d}, ", angle, distance));
                        } else {
                            // When the angle “wraps around” (i.e. becomes less than the initial angle),
                            // we assume a full scan has been collected.
                            if (angle < initialAngle) {
                                fullScanCollected = true;
                                break;
                            } else {
                                scanData.append(String.format("{%.2f; %d}, ", angle, distance));
                            }
                        }
                    } else {
                        if (scanStarted) {
                            scanData.append(String.format("{%.2f; %d}, ", angle, distance));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\":\"Exception during full scan: " + e.getMessage() + "\"}";
        }
        // Remove the trailing comma and space, if present.
        if (scanData.length() >= 2) {
            scanData.setLength(scanData.length() - 2);
        }
        return scanData.toString();
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
