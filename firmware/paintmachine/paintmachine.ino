/*

 PAINT MACHINE
 =============

 Visit tinkerlog.com for more details.


 Resolution
 ==========
 Y0-Ymax 0-25300 ~= 1710 mm  ==> 14.8 steps / mm
 X         15000 ~= 1000 mm  ==> 15 steps / mm

 Y0-Ymax = 1700 - 100 ==> 1600 (save zones)

 Pixel size 35mm x 35mm
 MaxY = 1600 / 35 ~= 46 pixel
 16:9 ==> 82 x 46 px
 4:3  ==> 61 x 46 px

 Pixel size 30mm x 30mm
 MaxY = 1600 / 30 ~= 53 pixel
 16:9 ==> 94 * 53 px
  4:3 ==> 71 * 53 px

 Problems
 ========
 1. short on one encoder channel
 2. analogWrite on pin 6
 3. noise on encoder lines ==> shielded cables
 4. esp8266 reboot loop ==> add cap & 7805


 Monitoring Vbat
 ===============
 
 Voltage divider   GND --- 10k -+- 100k --- Vbat 
 sensing on analog pin 3   
 1024 ticks @ 3.3V ==> 0.00322 per tick
                   ==> 0.00322 * 11 (because of 110K) = 0.03542
 Example: 
   325 ticks
   325 * 3542 = 1151150
   1151150 / 100000 = 11
   (1151150 % 100000) / 10000 = 5 ==> 11.5V  
   

 Pololu DRV8801 motor driver
 ===========================
  https://www.pololu.com/product/2136

               +----------------+
 VDD 3.3V -----| VDD     _FAULT |
               | BRAKE       CS |
    PIN40 -----| _SLEEP     VMM |----- motor BAT +
     PIN7 -----| DIR       OUT- |----- motor
     PIN6 -----| PWM       OUT+ |----- motor
      GND -----| GND        GND |----- motor BAT -
               +----------------+

 Pololu 50:1 Metal Gearmotor
 ===========================
  https://www.pololu.com/product/1440  
                           
  Red	motor power (connects to one motor terminal)
 Black	motor power (connects to the other motor terminal)
 Green	encoder GND
 Blue	encoder Vcc (3.5 – 20 V)
 Yellow	encoder A output
 White	encoder B output            


 Adafruit backpack LEDs
 ======================
  http://www.adafruit.com/product/1911
  https://learn.adafruit.com/adafruit-led-backpack/0-54-alphanumeric
  https://learn.adafruit.com/adafruit-led-backpack/changing-i2c-address

 Display --- Arduino Due
     SCL --- 21
     SDA --- 20
     GND --- GND
     VCC --- 5V
    Vi2C --- 3V

  Vi2C VCC GND SDA SCL


 Adafruit Huzzah ESP8266
 =======================
  https://www.adafruit.com/products/2471
  https://learn.adafruit.com/adafruit-huzzah-esp8266-breakout/using-arduino-ide

 ESP8266 --- Arduino Due
    VBat --- VIN
     GND --- GND
      Rx --- Tx3 14, Serial 3
      Tx --- Rx3 15


 End stops
 =========
 right 5V  --- 43
 right SWT --- 45
 right GND --- 47
  left 5V  --- 49
  left SWT --- 51
  left GND --- 53
 

 IRL2203N
 ========
  solenoid --> spray can
  pin 8

             
 Arduino DUE
 ===========
  00-01 Serial 0
  19-18 Serial 1
  17-16 Serial 2
  15-14 Serial 3

  02-13 PWM

*/

#include "motor.h"
#include "input.h"
#include "display.h"
#include "spraycan.h"
#include <Wire.h>
#include "Adafruit_LEDBackpack.h"
#include "Adafruit_GFX.h"
#include "DueTimer.h"

// motor pins
#define RIGHT_DIR 7
#define RIGHT_PWM 6
#define  HEAD_DIR 5
// using analogWrite on pin 4 made timer 6 stop. rewired to 9.
//#define  HEAD_PWM 4 
#define  HEAD_PWM 9
#define  LEFT_DIR 3
#define  LEFT_PWM 2
#define ENABLE 40

#define RIGHT_ENC_A 24
#define RIGHT_ENC_B 28
#define  HEAD_ENC_A 30
#define  HEAD_ENC_B 32
#define  LEFT_ENC_A 34
#define  LEFT_ENC_B 36

#define  LEFT_MAX_SPEED 200
#define RIGHT_MAX_SPEED 200
#define  HEAD_MAX_SPEED 220

// end switches
#define END_SWITCH_RIGHT_PWR 43
#define END_SWITCH_RIGHT_SWT 45
#define END_SWITCH_RIGHT_GND 47
#define  END_SWITCH_LEFT_PWR 49
#define  END_SWITCH_LEFT_SWT 51
#define  END_SWITCH_LEFT_GND 53

// print pin
#define SPRAY_PIN 8

// vbat measure pin
#define VBAT_MEASURE_PIN 3
#define VBAT_MUL 3542

#define LED_PIN 13
#define LED(on) (digitalWrite(LED_PIN, on))
#define INIT_HEAD_SPEED 50
#define MAX_BUFFER_SIZE (8*1024)
#define MAX_IMAGE_SIZE (8*1024)

#define X_STEPS_PER_MM 11.7F
#define Y_STEPS_PER_MM 14.5F
#define Y_MIN 0
#define Y_MAX 24650
#define Y_MIN_MM 0
#define Y_MAX_MM 1700
#define Y_OVERSHOOT 750
#define Y_SAFE_MM 30
#define X_FINISH_MM 200
#define PIXEL_WIDTH_MM 35
#define PIXEL_HEIGHT_MM 35
#define PIXEL_HEIGHT (PIXEL_HEIGHT_MM * Y_STEPS_PER_MM)

// states
#define STATE_INIT              0
#define STATE_INIT_LEFT         1
#define STATE_INIT_RIGHT        2
#define STATE_IDLE              3
#define STATE_DRAW_START        4
#define STATE_DRAW_WAIT_Y0      5
#define STATE_DRAW_LINE_YM      6
#define STATE_DRAW_COL_YM       7
#define STATE_DRAW_LINE_Y0      8
#define STATE_DRAW_COL_Y0       9
#define STATE_DRAW_FINISH       10
#define STATE_DRAW_WAIT_FINISH  11
#define STATE_MOVING            12
#define STATE_GOTO              13
#define STATE_ERROR             14
#define STATE_TEST1             15
#define STATE_TEST2             16
#define STATE_TEST3             17

char* states[] = {
  "INIT",        // 0
  "INIT LFT",    // 1
  "INIT RGT",    // 2
  "IDLE",        // 3
  "DRAWING",     // 4
  "WAIT Y0",     // 5
  "DRAW YM",     // 6
  "COL YM",      // 7
  "DRAW Y0",     // 8
  "COL Y0",      // 9
  "DRAW FIN",    // 10
  "WAIT FIN",    // 11
  "MOVING",      // 12
  "GOTO",        // 13
  "ERROR",       // 14
  "TEST1",       // 15
  "TEST2",       // 16
  "TEST3"        // 17
};

#define CMD_STAT "stat"
#define CMD_IMAG "imag"
#define CMD_PING "ping"
#define CMD_MOVE "move"
#define CMD_GOTO "goto"

long lastTime;
long nextUpdate = 0;
int supply = 0;
char supplyStr[8];
int state = STATE_INIT;
char line[MAX_BUFFER_SIZE];
char buf[32];

byte image[MAX_IMAGE_SIZE];
int imageWidth;
int imageHeight;
int actColumn;

volatile boolean leftEndSwitch = false;
volatile boolean rightEndSwitch = false;
volatile int loopCount = 0;

Motor  leftMotor( LEFT_PWM,  LEFT_DIR,  LEFT_ENC_A,  LEFT_ENC_B,  LEFT_MAX_SPEED, X_STEPS_PER_MM);
Motor rightMotor(RIGHT_PWM, RIGHT_DIR, RIGHT_ENC_A, RIGHT_ENC_B, RIGHT_MAX_SPEED, X_STEPS_PER_MM);
Motor  headMotor( HEAD_PWM,  HEAD_DIR,  HEAD_ENC_A,  HEAD_ENC_B,  HEAD_MAX_SPEED, Y_STEPS_PER_MM);

SprayCan sprayCan(SPRAY_PIN, PIXEL_HEIGHT);

UARTClass *cmdSerial;

void setup() {

  // setup 
  pinMode(2, OUTPUT);
  digitalWrite(2, LOW);
  
  Serial.begin(115200);  
  pinMode(LED_PIN, OUTPUT);

  Serial3.begin(115200);
  cmdSerial = &Serial3;
  // Serial.begin(115200);
  // cmdSerial = &Serial;

  // setup motor decoders
  Motor::disableMotors();
  attachInterrupt( LEFT_ENC_A,  leftEncoderA, RISING);
  attachInterrupt( LEFT_ENC_B,  leftEncoderB, RISING);  
  attachInterrupt(RIGHT_ENC_A, rightEncoderA, RISING);
  attachInterrupt(RIGHT_ENC_B, rightEncoderB, RISING);  
  attachInterrupt( HEAD_ENC_A,  headEncoderA, RISING);
  attachInterrupt( HEAD_ENC_B,  headEncoderB, RISING);  
  rightMotor.setInverted(true);

  // setup right end switch
  pinMode(END_SWITCH_RIGHT_PWR, OUTPUT);
  pinMode(END_SWITCH_RIGHT_GND, OUTPUT);
  digitalWrite(END_SWITCH_RIGHT_PWR, HIGH);
  digitalWrite(END_SWITCH_RIGHT_GND, LOW);
  attachInterrupt(END_SWITCH_RIGHT_SWT, rightSwitch, CHANGE);
  
  // setup left end switch
  pinMode(END_SWITCH_LEFT_PWR, OUTPUT);
  pinMode(END_SWITCH_LEFT_GND, OUTPUT);
  digitalWrite(END_SWITCH_LEFT_PWR, HIGH);
  digitalWrite(END_SWITCH_LEFT_GND, LOW);
  attachInterrupt(END_SWITCH_LEFT_SWT, leftSwitch, CHANGE);
  leftSwitch();
  rightSwitch();
  
  // setup display
  initDisplay();

  // wait 5 seconds
  int i = 0;
  for (i = 5; i > 0; i--) {
    itoa(i, buf, 10);
    display(buf);
    delay(1000);
  }
  
  // clear serial buffer
  while (cmdSerial->available()) {
    cmdSerial->read();
  }

  Motor::enableMotors();

  // Timer5.attachInterrupt(updateMotors2).setFrequency(20).start();
  Timer6.attachInterrupt(updateMotors).setFrequency(20).start();

  // state = STATE_IDLE;
  // state = STATE_TEST1;
  
  Serial.println("READY!");  
  display("READY");
}

void checkSupply() {
  supply = analogRead(VBAT_MEASURE_PIN) * VBAT_MUL;
  int vmaj = supply / 100000;
  int vmin = (supply % 100000) / 10000;
  // Serial.print("battery: ");
  // Serial.print(vmaj); Serial.print(".");
  // Serial.println(vmin);
  char *str = supplyStr;
  itoa(vmaj, str, 10);
  str = (vmaj >= 10) ? str+2 : str+1;
  *str = '.';
  str++;
  itoa(vmin, str, 10);
  // Serial.println(supplyStr);
  display(supplyStr);
}

void reportStatus() {
  char buf[40];
  sprintf(buf, "%d %d %d", leftMotor.getPosition(), rightMotor.getPosition(), headMotor.getPosition());
  Serial.println(buf);
  cmdSerial->println(buf);  
}

boolean handlePing(char *line) {
  if (strncmp(line, CMD_STAT, 4) == 0) {
    Serial.println("request: stat");
    reportStatus();
    return true;
  }
  else if (strncmp(line, CMD_PING, 4) == 0) {
    Serial.println("request: ping");
    cmdSerial->println("OK");
    return true;
  }
  else {
    Serial.print("not allowed: ");
    Serial.println(line);
    cmdSerial->println("NOTOK");
    return false;
  }
}

int doRequest(char *line) {

  int left = 0, right = 0, head = 0, fire = 0;
  int xTarget = 0, headTarget = 0;
  char buf[10];

  if (strncmp(line, CMD_IMAG, 4) == 0) {
    Serial.println("image");
    line += 5; // skip command and space
    readImage(line, image, &imageWidth, &imageHeight);
    sprayCan.prepareImage(image, imageWidth, imageHeight);
    cmdSerial->println("OK");
    return STATE_DRAW_START;
  }
  else if (strncmp(line, CMD_MOVE, 4) == 0) {
    line += 5; // skip command and space
    line = readToken(line, buf, ' ');
    left = atoi(buf);
    display(buf);
    line = readToken(line, buf, ' ');
    right = atoi(buf);
    line = readToken(line, buf, ' ');
    head = atoi(buf);
    line = readToken(line, buf, ' ');
    fire = atoi(buf);
    Serial.print("move left: "); Serial.print(left);
    Serial.print(", right: "); Serial.print(right);
    Serial.print(", head: "); Serial.print(head);
    Serial.print(", fire: "); Serial.println(fire);
    leftMotor.setSpeed(left);
    rightMotor.setSpeed(right);
    headMotor.setSpeed(head);
    sprayCan.spray(fire == 1);
    cmdSerial->println("OK");
    return (left == 0 && right == 0 && head == 0 && fire == 0) ? STATE_IDLE : STATE_MOVING;
  }
  else if (strncmp(line, CMD_GOTO, 4) == 0) {
    line += 5; // skip command and space
    line = readToken(line, buf, ' ');
    xTarget = atoi(buf);
    line = readToken(line, buf, ' ');
    headTarget = atoi(buf);
    Serial.print("goto: "); Serial.print(xTarget);
    Serial.print(" "); Serial.println(headTarget);
    leftMotor.setTargetMm(xTarget);
    rightMotor.setTargetMm(xTarget);
    headMotor.setTargetMm(headTarget);
    cmdSerial->println("OK");
    return STATE_GOTO;
  }
  else {
    if (handlePing(line)) {
      return STATE_IDLE;
    }
  }
  return STATE_ERROR;
}

/*
 * Advances in x direction to the next column, that has data to print.
 */
void advanceColumn(int maxColumn) {  
  do {
    actColumn++;
    Serial.print("--- column: ");
    Serial.println(actColumn);
  } while (sprayCan.isEmpty(actColumn) && actColumn < maxColumn);
  int xPos = PIXEL_WIDTH_MM * actColumn;
  leftMotor.setTargetMm(xPos);
  rightMotor.setTargetMm(xPos);  
}

void finishingMove() {
  int finTarget = actColumn * PIXEL_WIDTH_MM + X_FINISH_MM;
  leftMotor.setTargetMm(finTarget);
  rightMotor.setTargetMm(finTarget);
}

void resetOrigin() {
  leftMotor.setPosition(0);
  rightMotor.setPosition(0);
  headMotor.setPosition(0);
}

/*
 * Finds the y target position for this column. 
 * Uses the min/max of the current and the next column to compute, how far
 * to move the carriage. Adds some mm to overshoot, to be able to accelerate
 * or brake.
 */
int findYTarget(int column, boolean forward) {
  int y1 = forward ? sprayCan.getYMax(column) : sprayCan.getYMin(column);
  int y2 = forward ? sprayCan.getYMax(column+1) : sprayCan.getYMin(column+1);
  int target = forward ? max(y1, y2) : min(y1, y2);
  Serial.print("col: "); Serial.print(column);
  Serial.print(", forward: "); Serial.print(forward);
  Serial.print(", y1: "); Serial.print(y1);
  Serial.print(", y2: "); Serial.print(y2);
  Serial.print(", result: "); Serial.print(target);
  target = target + (forward ? Y_OVERSHOOT : -Y_OVERSHOOT);  
  target = constrain(target, Y_MIN, Y_MAX);
  Serial.print(", overshoot: "); Serial.println(target);
  return target;
}

void loop() {

  static long testTime = 0;
  static int nextState = 0;
  static int oldCount;
  static boolean supplyIsOn = false;
  static boolean ledIsOn = 0;
  static long ledTime;
  static long drawTime;
  char buf[8];
  
  int oldState = state;
  boolean isCmdComplete;

  // blink to show the main loop is working
  long now = millis();
  if (now > ledTime) {
    ledTime = now + 200;
    ledIsOn = !ledIsOn;
    LED(ledIsOn);
  }

  // read command
  isCmdComplete = readCmdLine(cmdSerial, line);
  if (isCmdComplete &&
      (state == STATE_INIT_LEFT || state == STATE_INIT_RIGHT ||
       state == STATE_GOTO || state == STATE_DRAW_WAIT_Y0 ||
       state == STATE_DRAW_LINE_YM || state == STATE_DRAW_COL_YM ||
       state == STATE_DRAW_LINE_Y0 || state == STATE_DRAW_COL_Y0 ||
       state == STATE_DRAW_FINISH ||
       state == STATE_TEST1 || state == STATE_TEST2)) {
    handlePing(line);
  }
  
  switch (state) {
  case STATE_TEST1:
    if (testTime < millis()) {
      testTime = millis() + 6000;
      nextState = STATE_TEST2;
      state = STATE_TEST3;
      leftMotor.setTargetMm(50);
      rightMotor.setTargetMm(50);
    }
    break;
  case STATE_TEST2:
    if (testTime < millis()) {
      testTime = millis() + 6000;
      nextState = STATE_TEST1;
      state = STATE_TEST3;
      leftMotor.setTargetMm(0);
      rightMotor.setTargetMm(0);
    }
    break;
  case STATE_TEST3:
    if (!leftMotor.isRunning() && !rightMotor.isRunning()) {
      state = nextState;
    }
    else {
      if (oldCount != loopCount) {
	oldCount = loopCount;
	leftMotor.debug();
      }
    }
    break;
  case STATE_INIT:
    if (!leftEndSwitch) {
      headMotor.setSpeed(-INIT_HEAD_SPEED);
    }
    state = STATE_INIT_LEFT;
    break;
  case STATE_INIT_LEFT:
    // goto left end switch
    if (leftEndSwitch) {
      headMotor.setSpeed(0);
      delay(50);
      resetOrigin();
      headMotor.setTargetMm(Y_SAFE_MM);
      state = STATE_INIT_RIGHT;
    }
    break;
  case STATE_INIT_RIGHT:
    // back off a bit
    if (!headMotor.isRunning()) {
      resetOrigin();
      state = STATE_IDLE;
    }
    break;
  case STATE_MOVING:
  case STATE_IDLE:    
    if (isCmdComplete) {
      state = doRequest(line);
    }    
    if (nextUpdate < millis()) {
      if (supplyIsOn) {
	display(states[state]);
      }
      else {
	checkSupply();
      }
      supplyIsOn = !supplyIsOn;
      nextUpdate = millis() + 2000;
    }
    break;
  case STATE_GOTO:
    if (!leftMotor.isRunning() && !rightMotor.isRunning() && !headMotor.isRunning()) {
      state = STATE_IDLE;
    }
    break;
  case STATE_DRAW_START:
    actColumn = 0;
    headMotor.setTargetMm(Y_MIN_MM);
    leftMotor.setPosition(0);
    rightMotor.setPosition(0);
    state = STATE_DRAW_WAIT_Y0;
    break;
  case STATE_DRAW_WAIT_Y0:
    // before starting print, move to y0
    if (!headMotor.isRunning()) {
      sprayCan.startImage();
      actColumn = -1;
      advanceColumn(imageWidth-1);
      if (actColumn == 0) {
	state = STATE_DRAW_LINE_YM;
	headMotor.setTarget(findYTarget(actColumn, true));
      }
      else if (actColumn == imageWidth-1) {
	state = STATE_DRAW_FINISH;
      }
      else {
	state = STATE_DRAW_COL_Y0;
      }
    }
    break;
  case STATE_DRAW_LINE_YM:
    // drawing line Y0 --> Ymax
    if (!headMotor.isRunning()) {
      if (actColumn == imageWidth-1) {
	state = STATE_DRAW_FINISH;
      }
      else {
	state = STATE_DRAW_COL_YM;
	sprayCan.finishColumn();
	advanceColumn(imageWidth-1);
	if (actColumn == imageWidth-1) {
	  state = STATE_DRAW_FINISH;
	}
      }
    }
    else {
      // draw
      sprayCan.printColumn(actColumn, headMotor.getPosition(), true);
    }
    break;
  case STATE_DRAW_COL_YM:
    // advance X (head at Ymax)
    if (!leftMotor.isRunning() && !rightMotor.isRunning()) {
      state = STATE_DRAW_LINE_Y0;
      headMotor.setTarget(findYTarget(actColumn, false));
    }
    break;
  case STATE_DRAW_LINE_Y0:
    // drawing line Ymax --> Y0
    if (!headMotor.isRunning()) {
      if (actColumn == imageWidth-1) {
	state = STATE_DRAW_FINISH;
      }
      else {
	state = STATE_DRAW_COL_Y0;
	sprayCan.finishColumn();
	advanceColumn(imageWidth-1);
	if (actColumn == imageWidth-1) {
	  state = STATE_DRAW_FINISH;
	}
      }
    }
    else {
      // draw
      sprayCan.printColumn(actColumn, headMotor.getPosition(), false);      
    }
    break;
  case STATE_DRAW_COL_Y0:
    // avance X (head at Y0)
    if (!leftMotor.isRunning() && !rightMotor.isRunning()) {
      state = STATE_DRAW_LINE_YM;
      headMotor.setTarget(findYTarget(actColumn, true));
    }
    break;
  case STATE_DRAW_FINISH:
    state = STATE_DRAW_WAIT_FINISH;
    sprayCan.finishImage();
    finishingMove();
    break;
  case STATE_DRAW_WAIT_FINISH:
    // finishing move
    if (!leftMotor.isRunning() && !rightMotor.isRunning()) {
      state = STATE_IDLE;
    }
    break;
  case STATE_ERROR:
    Serial.println("errored, stopped");
    Serial.println(line);
    display("ERROR");
    cmdSerial->print("ERROR ");
    cmdSerial->println(line);
    delay(5000);
    state = STATE_IDLE;
  }

  if (state != oldState) {
    Serial.print("state: "); 
    Serial.print(states[oldState]); 
    Serial.print(" --> ");
    Serial.print(states[state]);
    Serial.print(", ");
    Serial.print(loopCount);
    Serial.print(", left: "); Serial.print(leftMotor.getPosition());
    Serial.print(", right: "); Serial.print(rightMotor.getPosition());
    Serial.print(", head: "); Serial.print(headMotor.getPosition());
    Serial.println();
    display(states[state]);
  }

}

/*----------------------------------------------------------
 * Below are methods that handle interrupts.
 * Interrupt handlers have to be void (*)().
 */
void updateMotors() {
  leftMotor.loop();
  rightMotor.loop();
  headMotor.loop();
  loopCount++;
}

void leftEncoderA() {
  leftMotor.encoderA();
}

void leftEncoderB() {
  leftMotor.encoderB();
}

void rightEncoderA() {
  rightMotor.encoderA();
}

void rightEncoderB() {
  rightMotor.encoderB();
}

void headEncoderA() {
  headMotor.encoderA();
}

void headEncoderB() {
  headMotor.encoderB();
}

void leftSwitch() {
  leftEndSwitch = digitalRead(END_SWITCH_LEFT_SWT);
}

void rightSwitch() {
  rightEndSwitch = digitalRead(END_SWITCH_RIGHT_SWT);  
}

