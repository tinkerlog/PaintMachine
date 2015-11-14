
#include "spraycan.h"


SprayCan::SprayCan(int sprayPin, int pixelHeight) {
  this->sprayPin = sprayPin;
  this->pixelHeight = pixelHeight;
  this->posCount = 0;

  pinMode(sprayPin, OUTPUT);
  digitalWrite(sprayPin, LOW);
}

void SprayCan::prepareImage(byte *image, int width, int height) {
  this->width = width;
  this->height = height;
  
  int x, y = 0;
  for (x = 0; x < MAX_X; x++) {
    for (y = 0; y < MAX_Y; y++) {
      positions[x][y] = -1;
    }
  }

  Serial.println("prepare image");
  
  boolean isOn = false;
  for (x = 0; x < width; x++) {
    posCount = 0;
    // Serial.print("x: "); Serial.println(x);
    int yMin = -1;
    int yMax = -1;
    int yPos = 0;
    for (y = 0; y < height; y++) {      
      byte pixel = image[x*height+y];
      yPos = y * pixelHeight + Y_START_OFFSET;
      // Serial.print("y: "); Serial.print(y);
      // Serial.print(", pixel: "); Serial.print(pixel);
      // Serial.print(", pos: "); Serial.print(yPos);
      if (pixel > 0 && !isOn) {
	positions[x][posCount++] = yPos;
	isOn = true;
	if (yMin == -1) {
	  yMin = yPos;
	}
	// Serial.println(", switch on");
      }
      else if (pixel == 0 && isOn) {
	positions[x][posCount++] = yPos;
	yMax = yPos;
	isOn = false;
	// Serial.println(", switch off");
      }
      else {
	// Serial.println(", no change");
      }
    }
    if (isOn) {
      positions[x][posCount++] = yPos;
      yMax = yPos;
      // Serial.println("end of col, switch off");
      isOn = false;
    }
    endPositions[x][0] = yMin;
    endPositions[x][1] = yMax;
    Serial.print("poscount: "); Serial.print(posCount);
    Serial.print(", yMin: "); Serial.print(yMin);
    Serial.print(", yMax: "); Serial.println(yMax);
  }
}

void SprayCan::spray(boolean isOn) {
  actIsOn = isOn;
  digitalWrite(sprayPin, isOn);
}

void SprayCan::startImage() {
  actPos = 0;
  actIsOn = false;
}

void SprayCan::finishImage() {
  digitalWrite(sprayPin, LOW);
  actIsOn = false;
}

void SprayCan::finishColumn() {
  digitalWrite(sprayPin, LOW);
  actIsOn = false;
  actPos = 0;
}

void SprayCan::toggleSpray() {
  actIsOn = !actIsOn;
  digitalWrite(sprayPin, actIsOn);
}

boolean SprayCan::isEmpty(int column) {
  if (column < MAX_X) {
    return endPositions[column][0] == -1;
  }
  return true;
}

int SprayCan::getYMin(int column) {
  if (column < MAX_X) {
    return endPositions[column][0];
  }
  return -1;
}

int SprayCan::getYMax(int column) {
  if (column < MAX_X) {
    return endPositions[column][1];
  }
  return -1;
}

void SprayCan::printColumn(int column, int position, boolean isForward) {

  static int count = 0;

  count++;
  
  if (column != actColumn) {
    count = 0;
    // start with a new column
    Serial.println("----- new column");
    actColumn = column;
    if (isForward) {
      actPos = 0;
    }
    else {
      int i;
      int pos, lastPos = 0;
      for (i = 0; i < MAX_Y; i++) {
	lastPos = pos;
	int target = positions[column][i];
	// Serial.print("i: "); Serial.print(i);
	// Serial.print(", target: "); Serial.println(target);
	if (target == -1) {
	  // we reached the end
	  actPos = i-1;
	  break;
	}
      }
    }
  }

  if (actPos == -1) {
    return;
  }
  
  int targetPos = positions[column][actPos];
  if (targetPos == -1) {
    if (count % 10000 == 0) {
      Serial.print("print col: "); Serial.print(column);
      Serial.print(", position: "); Serial.print(position);
      Serial.print(", actPos: "); Serial.print(actPos);
      Serial.println(", no target!");
    }
    return;
  }
  if (position > targetPos && isForward) {
    toggleSpray();
    Serial.print("print col: "); Serial.print(column);
    Serial.print(", position: "); Serial.print(position);
    Serial.print(", actPos: "); Serial.print(actPos);
    Serial.print(", target: "); Serial.print(targetPos);
    Serial.print(", isOn: "); Serial.println(actIsOn);
    actPos++;
  }
  else if (position < (targetPos+Y_REVERSE_OFFSET) && !isForward) {
    toggleSpray();
    Serial.print("print col: "); Serial.print(column);
    Serial.print(", position: "); Serial.print(position);
    Serial.print(", actPos: "); Serial.print(actPos);
    Serial.print(", target: "); Serial.print(targetPos);
    Serial.print(", isOn: "); Serial.println(actIsOn);
    actPos--;
  }
  else {
    if (count % 10000 == 0) {
      Serial.print("print col: "); Serial.print(column);
      Serial.print(", position: "); Serial.print(position);
      Serial.print(", actPos: "); Serial.print(actPos);
      Serial.println(", < target");
    }
  }
  
}

