

#include "input.h"


char *readToken(char *str, char *buf, char delimiter) {
  uint8_t c = 0;
  while (true) {
    c = *str++;
    if ((c == delimiter) || (c == '\0')) {
      break;
    }
    else if (c != ' ') {
      *buf++ = c;
    }
  }
  *buf = '\0';
  return str;
}

byte readLine(UARTClass *cmdSerial, char *line, int size) {
  int length = 0;
  char c;
  while (length < size) {
    if (cmdSerial->available()) {
      c = cmdSerial->read();
      length++;
      if ((c == '\r') || (c == '\n')) {
	*line = '\0';
        break;
      }
      *line++ = c;
    }
  }
  return length;
}

boolean readCmdLine(UARTClass *cmdSerial, char* line) {
  static int linePtr = 0;
  int c;
  if (cmdSerial->available()) {
    c = cmdSerial->read();
    if (c == '\n') {
      line[linePtr] = '\0';
      linePtr = 0;
      return true;
    }
    line[linePtr++] = c;    
  }
  return false;
}

void readImage(char *line, byte *image, int* imageWidth, int* imageHeight) {
  int i;
  int value;
  char buf[16];

  line = readToken(line, buf, ' ');
  *imageWidth = atoi(buf);
  line = readToken(line, buf, ' ');
  *imageHeight = atoi(buf);

  Serial.print("width: "); Serial.print(*imageWidth);
  Serial.print(", height: "); Serial.println(*imageHeight);

  int length = strlen(line);
  Serial.print("len: "); Serial.println(length);

  int expected = *imageWidth * *imageHeight;
  Serial.print("expected: "); Serial.println(expected);

  for (i = 0; i < expected; i++) {
    if (i > MAX_IMAGE_SIZE) {
      Serial.println("buffer exceeded");
      return;
    }
    else if (i > length) {
      Serial.println("not enough data");
      return;
    }

    line = readToken(line, buf, ' ');
    value = atoi(buf);

    // Serial.println(value);
    image[i] = value;    
  }

}

