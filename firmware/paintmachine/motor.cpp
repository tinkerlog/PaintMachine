
#include "motor.h"

Motor::Motor(int pwmPin, int dirPin, int encAPin, int encBPin, int maxSpeed, float stepsPerMm) {

  this->pwmPin = pwmPin;
  this->dirPin = dirPin;
  this->encAPin = encAPin;
  this->encBPin = encBPin;
  this->maxSpeed = maxSpeed;
  this->stepsPerMm = stepsPerMm;
  
  pinMode(pwmPin, OUTPUT);
  pinMode(dirPin, OUTPUT);

  state = MOTOR_STATE_IDLE;

  target = 0;
  speed = 0;
  position = 0;
  deltaPos = 1;

  inverted = false;
}

//double aggKp=4, aggKi=0.2, aggKd=1;
//double consKp=1, consKi=0.05, consKd=0.25;

// 0.2 0.025 0.1
// 0.2 0.04 0.1

// small steps (1000)  0.1 0.03 0.2

//double kp = 0.1;
double kp = 0.15;
double ki = 0.03;
//double ki = 0.05;
//double kd = 0.2;
double kd = 0.5;

double p = 0.0;
double i = 0.0;
double d = 0.0;

int Motor::compute() {

  //double p = 0.0;
  //double i = 0.0;
  //double d = 0.0;

  double error = target - position;
  errorSum += error;
  errorSum = (errorSum > 2000) ? 2000 : (errorSum < -2000) ? -2000 : errorSum;

  p = error * kp;
  i = errorSum * ki;
  d = (error - lastError) * kd;
  lastError = error;

  double out = p + i + d;
  out = (out > 0) ? out + 7 : out - 7;
  out = constrain(out, -maxSpeed, maxSpeed);
  return out;
}

void Motor::loop() {
  if (state != MOTOR_STATE_TARGET) {
    return;
  }
  if (abs(target - position) > 10) {
    // ramp up speed
    int compSpeed = compute();
    int speedDelta = compSpeed - speed;
    if (abs(speedDelta) > 50) {
      speed += speedDelta * 0.4;
    }
    else {
      speed = compSpeed;
    }
    setDirection(inverted ? speed < 0 : speed > 0);
    analogWrite(pwmPin, abs(speed));
  }
  else {
    state = MOTOR_STATE_IDLE;
    analogWrite(pwmPin, 0);
  }
}

void Motor::setInverted(boolean inverted) {
  this->inverted = inverted;
  deltaPos = inverted ? -1 : +1;
}

void Motor::setTarget(int target) {
  state = MOTOR_STATE_TARGET;
  this->target = target;
}

void Motor::setTargetMm(int targetInMm) {
  state = MOTOR_STATE_TARGET;
  setTarget(targetInMm * stepsPerMm);
}

void Motor::setSpeed(int speed) {
  speed = constrain(speed, -maxSpeed, maxSpeed);
  this->speed = speed;
  state = (abs(speed) > 0) ? MOTOR_STATE_SPEED : MOTOR_STATE_IDLE;
  setDirection(inverted ? speed < 0 : speed > 0);
  analogWrite(pwmPin, abs(speed));
}

void Motor::setDirection(int direction) {
  this->direction = direction;
  digitalWrite(dirPin, direction);
}

void Motor::setPosition(int position) {
  this->position = position;
}

boolean Motor::isRunning() {
  return state != MOTOR_STATE_IDLE;
}

void Motor::debug() {
  Serial.print("pos: ");          Serial.print(position);
  Serial.print(", target: ");     Serial.print(target);
  Serial.print(", last error: "); Serial.print(lastError);
  Serial.print(", errorSum: ");   Serial.print(errorSum);
  Serial.print(", speed: ");      Serial.print(speed);
  Serial.print(", pid: ");        Serial.print(p);
  Serial.print(", ");             Serial.print(i);
  Serial.print(", ");             Serial.println(d);
}

void Motor::encoderA(void) {
  position = digitalRead(encBPin) ? position + deltaPos : position - deltaPos;
}

void Motor::encoderB(void) {
  position = digitalRead(encAPin) ? position - deltaPos : position + deltaPos;
}

void Motor::enableMotors() {
  digitalWrite(ENABLE_PIN, HIGH);
}

void Motor::disableMotors() {
  digitalWrite(ENABLE_PIN, LOW);
}
