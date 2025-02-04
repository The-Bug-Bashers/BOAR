This is the code for the first version ob the Robot

## Parts
- 2x standard hobby motor  (no stepper moto) + wheels
- 1x Arduino Uno
- 1x 6v (4x AA) battery pack
- 1x motor controller
- 2x standard Arduino IR sensor
- 1x standard arduino ultra sonic sensor

# Function 
1. while one of the IR sensors triggers (object within 10cm) the bot turns in opposite direction
2. if object in front (using USS) farther than 20cm drive forwards full speed
3. else if object in front (using USS) farther than10 cm drive forwards halve speed
4. else (object in front nearer then 10cm) scan obcect distance 90° left and right, turn to the furthest direction 