#ifndef spraycan_h
#define spraycan_h

#include "Arduino.h"

#define MAX_X 128
#define MAX_Y 48

#define Y_REVERSE_OFFSET 375
#define Y_START_OFFSET 375



class SprayCan {

 public:
  SprayCan(int sprayPin, int pixelHeigth);
  void prepareImage(byte *image, int width, int height);
  void startImage();
  void printColumn(int column, int position, boolean isForward);
  boolean isEmpty(int column);
  int getYMin(int column);
  int getYMax(int column);
  void finishColumn();
  void finishImage();
  void spray(boolean isOn);

 private:
  void toggleSpray();
  
  int sprayPin;
  int pixelHeight;
  int pixelHeight_2;
  int width;
  int height;  
  int positions[MAX_X][MAX_Y];
  int endPositions[MAX_X][2];
  int posCount;

  int actColumn;
  int actPos;
  boolean actIsOn;
  
};
 
#endif
