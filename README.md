# BOAR
Basic Obstacle Avoidance Robot

# Dependencies
- "jSerialComm" is required on the RasberryPi to communicate with the arduino (run `wget https://github.com/Fazecast/jSerialComm/releases/download/v2.6.2/jSerialComm-2.6.2.jar
` to install)

# Parts
## Lidar ([RPLIDAR A1M8](https://www.slamtec.com/en/lidar/a1))
[SDK and Documentation](https://www.slamtec.com/en/support#rplidar-a-series)

### Verrify conection to RasberryPi
To verrify, that the lidar is connected to the RasberryPi you can run `ls /dev/ttyUSB*` this should display somthing like: `/dev/ttyUSB0`

### Displaying current data on windows machienes
To connect the Lidar to a Windows machine, you need to install the drivers, and then start the program.
1. Install [drivers](https://github.com/The-Bug-Bashers/BOAR/tree/main/Parts/Lidar/Windows_Drivers) (either [64x](https://github.com/The-Bug-Bashers/BOAR/blob/main/Parts/Lidar/Windows_Drivers/CP210xVCPInstaller_x64.exe) or [84x](https://github.com/The-Bug-Bashers/BOAR/blob/main/Parts/Lidar/Windows_Drivers/CP210xVCPInstaller_x86.exe))
2. Connect Lidar to computer via USB
3. Find out COM port of Lidar
    -  The Lidar should show up in ports tab in device manager as `Silicon Labs CP210x USB to UART Bridge` + the com port.
    -  If it does not show up, the drivers are not installed correctly (A restart is required after driver instalation!)
4. Download and open the latest version of [frame_grabber.exe
](https://github.com/Slamtec/rplidar_sdk/releases)
5. Open frame_grabber.exe and select COM port of Lidar
