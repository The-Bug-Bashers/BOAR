/*
 * Authors: Jannik Händel, Justus Dicker
 *
 * License: Creative Commons Attribution-NonCommercial-ShareAlike (CC BY-NC-SA)
 *
 * This code is open-source and may be used, modified, and shared under the following conditions:
 * - You must credit the original authors when redistributing or modifying the code.
 * - You may NOT use this code or any derived work for commercial purposes.
 * - Any modifications or derivative works must be released under the same license.
 * - The authors are NOT accountable for any damages, harm, or issues resulting from the use of this code.
 *
 * More details about this license: https://creativecommons.org/licenses/by-nc-sa/4.0/
 */


// Define parameters which need to be fine-tuned (units provided in backers)

#define delayUntilNextScanningCycle 1 // Delay between sensor updates (ms)

#define drivingSpeed 255 // Normal driving speed (PWM value 0-255)
#define slowDrivingSpeed 240 // Slow driving speed (PWM value 0-255)

#define breakDuration 150 // Duration the robot waits to fully stop (ms)

#define USSStopThreshold 15 // Distance threshold for stopping (cm)
#define USSSlowdownThreshold 25 // Distance threshold for slowing down (cm)

#define turnOuterWheelSpeed 255 // Speed for outer wheel during turns (PWM value 0-255)
#define turnInnerWheelSpeed 250 // Speed for inner wheel during turns (PWM value 0-255)
#define turn90DegreeDuration 425 // Time to complete a 90-degree turn (ms)
#define turn10DegreeDuration 110 // Time to complete a 10-degree turn (ms)


// Define pins

// Left Motor
#define motorLeftSpeed 5 // speed (pwm pin)
#define motorLeftForward 3 // direction control (forward)
#define motorLeftBackward 4 // direction control (backward)

// Right Motor
#define motorRightSpeed 6 // speed (pwm pin)
#define motorRightForward 12 // direction control (forward)
#define motorRightBackward 2 /// direction control (backward)

#define IRLeft 9 // Left infrared sensor
#define IRRight 10 // Right infrared sensor

#define USSEcho A1 // Echo pin for ultrasonic sensor
#define USSTrigger 11 // Trigger pin for ultrasonic sensor


// Define global variables
bool isIRLeft, isIRRight; // IR sensor readings
short forwardDrivingSpeed; // 0 = not driving forward, 1 = slow forward, 2 = fast forward
int USSDistance; // Distance measured by ultrasonic sensor


void setup() {
  // Set sensor pins
  pinMode(IRLeft, INPUT);
  pinMode(IRRight, INPUT);
  pinMode(USSEcho, INPUT);
  pinMode(USSTrigger, OUTPUT);

  // Set motor control pins
  pinMode(motorLeftSpeed, OUTPUT);
  pinMode(motorLeftForward, OUTPUT);
  pinMode(motorLeftBackward, OUTPUT);
  pinMode(motorRightSpeed, OUTPUT);
  pinMode(motorRightForward, OUTPUT);
  pinMode(motorRightBackward, OUTPUT);

  Serial.begin(9600); // Initialize serial communication
}

void loop() {
  updateSensorValues(); // Update stored sensor values to current sensor values

  // Output sensor values through serial Monitor
  Serial.print("Sensor values: IRL: ");
  Serial.print(isIRLeft);
  Serial.print(" IRR: ");
  Serial.print(isIRRight);
  Serial.print(" SSS: ");
  Serial.println(USSDistance);

  // Evaluate sensor data and decide best response
  if (isIRLeft || isIRRight || USSDistance <= USSStopThreshold) { // Check if distance is critically low
    if (isIRLeft) { // If  wall detected in shallow angle left: turn slightly right
      Serial.println("Turning slightly Left");
      forwardDrivingSpeed = 0;
      turn(false, true);
    } else if (isIRRight) { // If  wall detected in shallow angle right: turn slightly left
      Serial.println("Turning slightly Right");
      forwardDrivingSpeed = 0;
      turn(false, false);
    } else { // If wall detected right in front: turn to furthest direction (either 90 degree left or right)
      Serial.println("Turning to furthest direction");
      forwardDrivingSpeed = 0;
      turnToFurthestDirection();
    }
  } else if (USSDistance <= USSSlowdownThreshold && forwardDrivingSpeed != 1) { // If wall in front near robot: drive slow
    Serial.println("Driving Slow");
    drive(slowDrivingSpeed, true);
    forwardDrivingSpeed = 1;
  } else if (USSDistance > USSSlowdownThreshold && forwardDrivingSpeed != 2) { // If wall in front far away: drive fast
    Serial.println("Driving Fast");
    forwardDrivingSpeed = 2;
    drive(drivingSpeed, true);
  }

  delay(delayUntilNextScanningCycle); // Wait before next iteration
}

// Function to turn towards the furthest detected direction (either 90 degree left or right)
void turnToFurthestDirection() {
  turn(true, false); // Turn left (90°)
  delay(10);
  updateSensorValues();
  const int distanceLeft = USSDistance; // Save distance to left wall

  turn(true, true); // Turn right (90°)
  turn(true, true); // Turn right again (90°) to measure distance of right wall
  delay(10);
  updateSensorValues();
  const int distanceRight = USSDistance; // Save distance to left wall

  // Turn back to the side with more space
  if (distanceLeft > distanceRight) {
    turn(true, false); // Turn left (90°)
    turn(true, false); // Turn left (90°)
  }
}

// Function to update sensor values
void updateSensorValues() {
  isIRLeft = digitalRead(IRLeft) ? 0 : 1; // Read ans set if left IR sensor triggered
  isIRRight = digitalRead(IRRight) ? 0 : 1; // Read and set right if IR sensor triggered

  // Measure distance using ultrasonic sensor
  digitalWrite(USSTrigger, 1);
  delay(10);
  digitalWrite(USSTrigger, 0);
  USSDistance = (pulseIn(USSEcho, 1) / 2) * 0.03432; // Convert to cm
}

// Function to drive the robot forward or backward
void drive(short speed, bool driveForward) {
  stopMotors(); // Ensure motors are stopped before driving

  analogWrite(motorLeftSpeed, speed);
  analogWrite(motorRightSpeed, speed);

  if (driveForward) {
    digitalWrite(motorLeftForward, 1);
    delay(1); // wait in order to not break motor controller
    digitalWrite(motorRightForward, 1);
  } else {
    digitalWrite(motorLeftBackward, 1);
    delay(1); // wait in order to not break motor controller
    digitalWrite(motorRightBackward, 1);
  }
}

// Function to turn the robot
void turn(bool turn90Degree, bool turnRight) {
  short innerMotorSpeed, innerMotorDirection, outerMotorSpeed, outerMotorDirection;
  int turnDuration;

  // Assign motor control based on turn direction
  if (turnRight) {
    innerMotorSpeed = motorRightSpeed;
    innerMotorDirection = motorRightBackward;
    outerMotorSpeed = motorLeftSpeed;
    outerMotorDirection = motorLeftForward;
  } else {
    innerMotorSpeed = motorLeftSpeed;
    innerMotorDirection = motorLeftBackward;
    outerMotorSpeed = motorRightSpeed;
    outerMotorDirection = motorRightForward;
  }

  turnDuration = turn90Degree ? turn90DegreeDuration : turn10DegreeDuration; // if method was called to turn 90 Degrees: set turn duration duration to turn 90 degree duration

  breakMotors(); // Stop before turning

  analogWrite(innerMotorSpeed, turnInnerWheelSpeed);
  analogWrite(outerMotorSpeed, turnOuterWheelSpeed);
  digitalWrite(innerMotorDirection, 1);
  delay(1); // wait in order to not break motor controller
  digitalWrite(outerMotorDirection, 1);

  delay(turnDuration);

  breakMotors(); // Stop after turn
}

// Function to stop all motors
void stopMotors() {
  digitalWrite(motorLeftForward, 0);
  digitalWrite(motorLeftBackward, 0);
  digitalWrite(motorRightForward, 0);
  digitalWrite(motorRightBackward, 0);
}

// Function to apply a short brake to the motors
void breakMotors() {
  digitalWrite(motorRightSpeed, 1);
  digitalWrite(motorLeftSpeed, 1);

  digitalWrite(motorLeftForward, 1);
  delay(1); // wait in order to not break motor controller
  digitalWrite(motorLeftBackward, 1);
  delay(1); // wait in order to not break motor controller
  digitalWrite(motorRightForward, 1);
  delay(1); // wait in order to not break motor controller
  digitalWrite(motorRightBackward, 1);

  delay(breakDuration);

  stopMotors();
}