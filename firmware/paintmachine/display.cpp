
#include "display.h"

#include <Wire.h>
#include "Adafruit_LEDBackpack.h"
#include "Adafruit_GFX.h"


Adafruit_AlphaNum4 display0 = Adafruit_AlphaNum4();
Adafruit_AlphaNum4 display1 = Adafruit_AlphaNum4();

char message[80];
char blanks[] = "        ";
int pos;

void initDisplay() {
  display0.begin(0x71);  // pass in the address
  display1.begin(0x70);  // pass in the address
  pos = 0;
}

void clearDisplay() {
  display0.clear();
  display1.clear();
  display0.writeDisplay();
  display1.writeDisplay();
}

/*
void setMessage(char* msg) {
  int msgLen = strlen(msg);
  strcpy(message, msg);
  char* p = message
}
*/

void display(char* msg) {
  clearDisplay();
  int c;
  int pos = 0;
  boolean displayDot = false;
  while (pos < 8) {
    c = *msg++;
    if (c == '\0') {
      break;
    }
    displayDot = (c == '.');
    if (pos < 4) {      
      display0.writeDigitAscii(pos, c, displayDot);
    } 
    else {
      display1.writeDigitAscii(pos-4, c, displayDot);
    }
    pos++;
  }
  display0.writeDisplay();
  display1.writeDisplay();
}

void updateDisplay() {
}


