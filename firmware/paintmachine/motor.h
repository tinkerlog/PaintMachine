#ifndef motor_h
#define motor_h

#include "Arduino.h"

#define ENABLE_PIN 40

#define MOTOR_STATE_IDLE   0
#define MOTOR_STATE_TARGET 1
#define MOTOR_STATE_SPEED  2

#define DIR_LEFT     0
#define DIR_RIGHT    1
#define DIR_FORWARD  0
#define DIR_BACKWARD 1

class Motor {

 public:
  Motor(int pwmPin, int dirPin, int encAPin, int encBPin, int maxSpeed, float stepsPerMm);

  void setMaxSpeed(int maxSpeed);
  void setInverted(boolean inverted);
  void setTarget(int target);
  void setTargetMm(int targetInMm);
  int getTarget() { return target; }
  void setSpeed(int speed);
  int getSpeed() { return speed; }
  void setDirection(int direction);
  int getDirection() { return direction; }
  void setPosition(int position);
  int getPosition() { return position; }

  void encoderA();
  void encoderB();
  void loop();
  void debug();

  int getState() { return state; }
  boolean isRunning();

  static void enableMotors();
  static void disableMotors();

 private:

  int compute();

  int pwmPin;
  int dirPin;
  int encAPin;
  int encBPin;
  int direction;
  int position;
  int deltaPos;
  int target;
  int maxSpeed;
  int speed;
  int state;
  float stepsPerMm;
  
  boolean inverted;

  volatile int errorSum = 0;
  volatile double lastError = 0.0;

};

#endif
