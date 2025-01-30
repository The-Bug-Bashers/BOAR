// Define parameters which need to be finetuned

const int breakDuration = 20; // Duration the robot waits to fully stop in ms

const short drivingSpeed = 0; // in analog write value (0-255)
const short slowDrivingSpeed = 0; // in analog write value (0-255)

const short USSStopThreshold = 0; // in cm
const short USSSlowdownThreshold = 0; // in cm

// Turning parameters
const short turnAutherWheelSpeed = 0; // in analog write value (0-255)
const short turnInnerWheelSpeed = 0; // in analog write value (0-255)
const int turn90DegreeDuration = 0; // in ms 
const int turn10DegreeDuration = 0; // in ms 


// Define pins

// Define motor pins
const short motorLeftSpeed = 0; // ENA
const short motorLeftForward = 0; // IN1
const short motorLeftBackward = 0; // IN2
const short motorRightSpeed = 0; // ENB
const short motorRightForward = 0; // IN3
const short motorRightBackward = 0; // IN4

// Define infrared sensor pins
const short IRLeft = 0;
const short IRRight = 0;

// Define ultrasonic sensor pins
const short USSEcho = 0;
const short USSTrigger = 0;


// Define variables
bool isIRLeft, isIRRight;
int USSDistance;


void setup() {
  pinMode(IRLeft, INPUT);
  pinMode(IRRight, INPUT);
  pinMode(USSEcho, INPUT);
  pinMode(USSTrigger, OUTPUT);

  pinMode(motorLeftSpeed, OUTPUT);
  pinMode(motorLeftForward, OUTPUT);
  pinMode(motorLeftBackward, OUTPUT);
  pinMode(motorRightSpeed, OUTPUT);
  pinMode(motorRightForward, OUTPUT);
  pinMode(motorRightBackward, OUTPUT);

  Serial.begin(9600);
}

void loop() {
  updateSensorValues();

  if (isIRLeft || isIRRight || USSDistance <= USSStopThreshold) {
    if (isIRLeft) {
      turn(false, true);
    } else if (isIRRight) {
      turn(false, false);
    } else {
      turnToFurthestDirection();
    }
  } else if (USSDistance <= USSSlowdownThreshold) {
    drive(slowDrivingSpeed, true);
  } else {
    drive(drivingSpeed, true);
  }
}


// Functions

void turnToFurthestDirection() {
  turn(true, false);
  updateSensorValues();
  const short distanceLeft = USSDistance;

  turn(true, true);
  turn(true, true);
  updateSensorValues();
  const short distanceRight = USSDistance;

  if (distanceLeft > distanceRight) {
    turn(true, false);
    turn(true, false);
  }
}


// Utility functions

// Sensor utility functions

void updateSensorValues() {
  isIRLeft = digitalRead(IRLeft);
  isIRRight = digitalRead(IRRight);

  digitalWrite(USSTrigger, 1);
  delay(10);
  digitalWrite(USSTrigger, 0);
  USSDistance = (pulseIn(USSEcho, 1) / 2) * 0.03432;
}


// Motor utility functions

void drive(short speed, bool driveForward) {
  stopMotors();

  analogWrite(motorLeftSpeed, speed);
  analogWrite(motorRightSpeed, speed);

  if (driveForward) {
    digitalWrite(motorLeftForward, 1);
    digitalWrite(motorRightForward, 1);
  } else {
    digitalWrite(motorLeftBackward, 1);
    digitalWrite(motorRightBackward, 1);
  }
}

void turn(bool turn90Degre, bool turnRight) {
  short innerMotorSpeed, innerMotorDirection, autherMotorSpeed, autherMotorDirection;
  int turnDuration;


  if (turnRight) {
    innerMotorSpeed = motorRightSpeed;
    innerMotorDirection = motorRightBackward;

    autherMotorSpeed = motorLeftSpeed;
    autherMotorDirection = motorLeftForward;
  } else {
    innerMotorSpeed = motorLeftSpeed;
    innerMotorDirection = motorLeftBackward;

    autherMotorSpeed = motorRightSpeed;
    autherMotorDirection = motorRightForward;
  }

  if (turn90Degre) {
    turnDuration = turn90DegreeDuration;
  } else {
    turnDuration = turn10DegreeDuration;
  }

  breakMotors();

  analogWrite(innerMotorSpeed, turnInnerWheelSpeed);
  analogWrite(autherMotorSpeed, turnAutherWheelSpeed);
  digitalWrite(innerMotorDirection, 1);
  digitalWrite(autherMotorDirection, 1);

  delay(turnDuration);

  breakMotors();
}


void stopMotors() {
  digitalWrite(motorLeftForward, 0);
  digitalWrite(motorLeftBackward, 0);
  digitalWrite(motorRightForward, 0);
  digitalWrite(motorRightBackward, 0);
}

void breakMotors() {
  digitalWrite(motorRightSpeed, 1);
  digitalWrite(motorLeftSpeed, 1);

  digitalWrite(motorLeftForward, 1);
  digitalWrite(motorLeftBackward, 1);
  digitalWrite(motorRightForward, 1);
  digitalWrite(motorRightBackward, 1);

  delay(breakDuration);

  stopMotors();
}