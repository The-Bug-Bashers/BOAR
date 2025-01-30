// Define parameters which need to be finetuned

const int breakDuration = 20; // Durstion the robot waits to fully stop in ms

const int drivingSpeed = 0; // in analog write value (0-255)

// Turning parameters
const short turnAutherWheelSpeed = 0; // in analog write value (0-255)
const short turnInnerWheelSpeed = 0; // in analog write value (0-255)
const int turn90DegreeDuration = 0;// in ms 
const int turn10DegreeDuration = 0;// in ms 


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
const short USSTrigger = 0;
const short USSEcho = 0;


// Define variables

bool isIRLeft, isIRRight;
int USSDistance;



void setup() {

}

void loop() {

}




// Utility functions

// Sensor utility functions

updateSensorValues() {
  isIRLeft = analogRead
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