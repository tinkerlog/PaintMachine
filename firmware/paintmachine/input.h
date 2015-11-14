#ifndef input_h
#define input_h

#include "Arduino.h"

#define MAX_IMAGE_SIZE (8*1024)

char *readToken(char *str, char *buf, char delimiter);

byte readLine(UARTClass *cmdSerial, char *line, int size);

boolean readCmdLine(UARTClass *cmdSerial, char* line);

void readImage(char *line, byte *image, int* imageWidth, int* imageHeight);

#endif
