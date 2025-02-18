package boar.SLAM_API;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LiDARService {
    private final SerialPort serialPort;
    private volatile boolean running = true;

    // Global storage for the complete scan data
    private final List<String> globalScanData = new ArrayList<>();
    // Temporary storage for accumulating points until a full scan is complete
    private final List<String> currentScanData = new ArrayList<>();

    public LiDARService() {
        serialPort = SerialPort.getCommPort("/dev/ttyUSB0");
        if (!serialPort.openPort()) {
            throw new RuntimeException("Error: LiDAR not detected on /dev/ttyUSB0!");
        }
        // Increase timeout to allow more time for data to arrive
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 0);
        serialPort.setBaudRate(115200);

        startMotor();
        startScan();
        // Allow the LiDAR time to initialize and start sending data
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startContinuousReading();
    }

    private void startMotor() {
        if (serialPort.isOpen()) {
            // This command may vary depending on your wiring/setup
            serialPort.clearDTR();
        } else {
            throw new IllegalStateException("Cannot start motor without an open port");
        }
    }

    private void startScan() {
        if (serialPort.isOpen()) {
            byte[] startScanCommand = {(byte) 0xA5, 0x20};
            serialPort.writeBytes(startScanCommand, startScanCommand.length);
        } else {
            throw new IllegalStateException("Cannot start scan without an open port");
        }
    }

    private void startContinuousReading() {
        new Thread(() -> {
            try (InputStream inputStream = serialPort.getInputStream()) {
                byte[] buffer = new byte[256];
                while (running) {
                    try {
                        int numBytes = inputStream.read(buffer);
                        if (numBytes > 0) {
                            System.out.println("Received bytes: " + numBytes);
                            processLiDARData(buffer, numBytes);
                        } else {
                            System.out.println("No data received...");
                        }
                    } catch (SerialPortTimeoutException e) {
                        // Timeout is expected if no data is available; log and continue
                        System.out.println("Read timed out: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Synchronized helper: update global scan storage from current scan data
    private synchronized void updateGlobalScanData() {
        globalScanData.clear();
        globalScanData.addAll(currentScanData);
    }

    // Synchronized helper: add a new data point
    private synchronized void addDataPoint(String dataPoint) {
        currentScanData.add(dataPoint);
    }

    // Synchronized helper: clear the temporary storage
    private synchronized void clearCurrentScanData() {
        currentScanData.clear();
    }

    /**
     * Process incoming LiDAR data.
     * Assumes each measurement is 5 bytes and that the LSB of the first byte indicates the start of a new scan.
     */
    private void processLiDARData(byte[] data, int length) {
        // Process data in chunks of 5 bytes
        for (int i = 0; i <= length - 5; i += 5) {
            // Determine if this measurement is marked as the start of a new scan.
            boolean isNewScan = (data[i] & 0x01) == 1;
            int rawAngle = ((data[i + 2] & 0xFF) << 7) | ((data[i + 1] & 0xFF) >> 1);
            double angle = rawAngle / 64.0;
            int distance = ((data[i + 3] & 0xFF) | ((data[i + 4] & 0xFF) << 8));

            // Only consider valid points (non-zero distance)
            if (distance > 0) {
                String dataPoint = String.format("{\"angle\": %.2f, \"distance\": %d}", angle, distance);
                // If a new scan is detected and we already have data accumulated,
                // update the global scan data and clear the temporary storage.
                if (isNewScan && !currentScanData.isEmpty()) {
                    updateGlobalScanData();
                    clearCurrentScanData();
                }
                addDataPoint(dataPoint);
            }
        }
    }

    /**
     * Returns the global LiDAR scan data as a JSON array string.
     */
    public synchronized String getLatestData() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < globalScanData.size(); i++) {
            sb.append(globalScanData.get(i));
            if (i < globalScanData.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
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
