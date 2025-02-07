// Define parameters which need to be fine tuned

const int breakDuration = 100; // Duration the robot waits to fully stop in ms

const short drivingSpeed = 255; // in analog write value (0-255)
const short slowDrivingSpeed = 255; // in analog write value (0-255)

const short USSStopThreshold = 15; // in cm
const short USSSlowdownThreshold = 25; // in cm

// Turning parameters
const short turnOuterWheelSpeed = 255; // in analog write value (0-255)
const short turnInnerWheelSpeed = 255; // in analog write value (0-255)
const int turn90DegreeDuration = 625; // in ms
const int turn10DegreeDuration = 100; // in ms

const int delayUntilNextScanningCycle = 500; // in ms


// Define pins

// Define motor pins
int motorLeftSpeed = 5; // ENA
int motorLeftForward = 3; // IN1
int motorLeftBackward = 4; // IN2
int motorRightSpeed = 6; // ENB
int motorRightForward = 12; // IN3
int motorRightBackward = 2; // IN4

// Define infrared sensor pins
#define IRLeft 9
#define IRRight 10

// Define ultrasonic sensor pins
#define USSEcho A1
#define USSTrigger 11


// Define variables
bool isIRLeft, isIRRight;
short forwardDrivingSpeed; // 0 = not driving forward, 1 = Slowly forward, 2 = Fast forward
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
  Serial.print("Sensor values: IRL: ");
  Serial.print(isIRLeft);
  Serial.print(" IRR: ");
  Serial.print(isIRRight);
  Serial.print(" SSS: ");
  Serial.println(USSDistance);

  if (isIRLeft || isIRRight || USSDistance <= USSStopThreshold) {
    if (isIRLeft) {
      Serial.println("Turning slightly Left");
      forwardDrivingSpeed = 0;
      turn(false, true);
    } else if (isIRRight) {
      Serial.println("Turning slightly Right");
      forwardDrivingSpeed = 0;
      turn(false, false);
    } else {
      Serial.println("Turning to furthest direction");
      forwardDrivingSpeed = 0;
      turnToFurthestDirection();
    }
  } else if (USSDistance <= USSSlowdownThreshold && forwardDrivingSpeed != 1) {
    Serial.println("Driving Slow");
    drive(slowDrivingSpeed, true);
    forwardDrivingSpeed = 1;
  } else if (USSDistance > USSSlowdownThreshold && forwardDrivingSpeed != 2) {
    Serial.println("Driving Fast");    
    forwardDrivingSpeed = 2;
    drive(drivingSpeed, true);
  }

  delay(delayUntilNextScanningCycle);
}


// Functions

void turnToFurthestDirection() {
  turn(true, false);
  delay(1000);
  updateSensorValues();
  const short distanceLeft = USSDistance;

  turn(true, true);
  turn(true, true);
  delay(1000);
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
  isIRLeft = digitalRead(IRLeft) ? 0 : 1;
  isIRRight = digitalRead(IRRight) ? 0 : 1;

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
    delay(1);
    digitalWrite(motorRightForward, 1);
  } else {
    digitalWrite(motorLeftBackward, 1);
    delay(1);
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
  analogWrite(autherMotorSpeed, turnOuterWheelSpeed);
  digitalWrite(innerMotorDirection, 1);
  delay(1);
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
  delay(1);
  digitalWrite(motorLeftBackward, 1);
  delay(1);
  digitalWrite(motorRightForward, 1);
  delay(1);
  digitalWrite(motorRightBackward, 1);

  delay(breakDuration);

  stopMotors();
}