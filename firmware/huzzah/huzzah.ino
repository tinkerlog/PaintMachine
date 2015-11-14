
#include <ESP8266WiFi.h>

#define SERIAL_DEBUG 0

#if SERIAL_DEBUG
#define DEBUG_PRINT(c) Serial.print(c)
#define DEBUG_PRINTLN(c) Serial.println(c)
#else
#define DEBUG_PRINT(c)
#define DEBUG_PRINTLN(c)
#endif

#define SSID "PRINTBOT"
#define PASSWORD "password"
#define LED 0
#define CMD_PING "PING"
#define CMD_STATE "STATE"

#define MSG_CONNECT "conn"
#define MSG_DISCONNECT "disc"

WiFiServer server(81);

boolean alreadyConnected = false;

WiFiClient client;
char buffer[64];

void setup() {
  Serial.begin(115200);
  
  // WiFi.mode(WIFI_AP);
  // WiFi.softAP(SSID, PASSWORD);
  WiFi.softAP(SSID);
  IPAddress myIP = WiFi.softAPIP();

  pinMode(LED, OUTPUT);
  digitalWrite(LED, LOW);
  
  for (int i = 0; i < 5; i++) {
    digitalWrite(LED, LOW);
    delay(100);
    digitalWrite(LED, HIGH);
    delay(100);
  }

  server.begin();

}
 
int emptyCount = 0;
long ledSwitch = 0;
long lastReceivedTimeout = -1;
boolean ledOn = true;

void loop() {

  client = server.available();
  if (client) {
    digitalWrite(LED, LOW);    
    while (client.connected()) {

      if (lastReceivedTimeout > 0 && millis() > lastReceivedTimeout) {
	client.write("TIMEOUT");
	client.flush();
	client.stop();
	break;
      }

      while (client.available()) {
	lastReceivedTimeout = millis() + 5000;
	Serial.write(client.read());
      }
      
      if (Serial.available()) {	
	size_t len = Serial.available();
	uint8_t sbuf[len];
	Serial.readBytes(sbuf, len);
	client.write(sbuf, len);
	// delay(1);
      }      
    }
    // client lost    
    digitalWrite(LED, HIGH);
  }
  else {
    lastReceivedTimeout = -1;
    if (millis() > ledSwitch) {
      ledSwitch = millis() + 200;
      ledOn = !ledOn;
      digitalWrite(LED, ledOn);
    }
  }  
  delay(5);  
  
}

