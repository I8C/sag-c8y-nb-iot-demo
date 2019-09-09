/*  
* Copyright 2018 i8c N.V. (www.i8c.be)
* 
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
#include <Arduino.h>
#include <Sodaq_TPH.h>
#include <AirQuality.h>
#include <Wire.h>
#include "ATT_NBIOT.h"
#include <EEPROM.h>

#define debugSerial Serial
#define nbiotSerial Serial1

#define MODEM_ON_OFF_PIN 23

#define SERIAL_BAUD 57600

/* PIN NUMBERS */
#define LIGHT_SENSOR A0
#define AIR_SENSOR A2  //"switched"
#define LOUDNESS_SENSOR A4
#define PIR_MOTION_SENSOR 4

#define ADC_AREF 3.3
#define BATVOLTPIN A6
#define BATVOLT_R1 4.7
#define BATVOLT_R2 10

AirQuality airqualitysensor;

// deviceId: IMEI on the device (written on the UBLOX SARA N211 module)
const char *devicePW = "";
const char *apn = "iot.orange.be";

const char *server = "3.120.251.198";
const char *port = "8888";

ATT_NBIOT device(devicePW, apn); //setAttDevice to overwrite later

void(* resetFunc) (void) = 0;//reset/reboot function

void setup()
{
  // turn on the power for the secondary row of grove connectors ("switched")
  pinMode(GROVEPWR, OUTPUT);  
  digitalWrite(GROVEPWR, HIGH);
  
  delay(1000);
  debugSerial.begin(SERIAL_BAUD);
  nbiotSerial.begin(9600);

  debugSerial.println("Initializing and connecting... ");
  
  //read password from storage
  debugSerial.println("saved EEPROM passw: ");
  debugSerial.println(String(readPW()));
  device.setAttDevice(readPW(), apn);
  
  initSensors();
  device.init(nbiotSerial, debugSerial, MODEM_ON_OFF_PIN);
  
  if (device.connect(server, port))
    debugSerial.println("Connected!");
  else
  {
    debugSerial.println("Connection failed!");
    resetFunc(); //reboot to try again
  }
}

unsigned long sendNextAt = 0;  // Keep track of time
void loop() 
{
  if (sendNextAt < millis())
  {
    //check for incoming data:
    int amountbytes = device.getPendingUDPBytes();
    if (amountbytes > 0)
    {
      debugSerial.println("incoming data!");
      byte data[amountbytes];
      SaraN2UDPPacketMetadata p;
      int size = device.socketReceiveBytes(data, amountbytes, &p);
      if (size && String(p.ip) == String(server)) {//save password if it comes from the server
        
        String passw = "";
        for (byte i = 0; i < amountbytes; i++){
          passw = passw + String((char)data[i]);
        }
        writePW(passw);
        device.setAttDevice(readPW(), apn);
        debugSerial.println("password: " + passw);
      }
      
    }

    
    //read values
    int lightValue = analogRead(LIGHT_SENSOR);
    int loudnessValue = analogRead(LOUDNESS_SENSOR);
    int motionValue = digitalRead(PIR_MOTION_SENSOR);  
    float temperatureValue = tph.readTemperature(); 
    int humidityValue = (int)tph.readHumidity();
    long pressureValue = tph.readPressure();
    int battValue = (int)(getRealBatteryVoltage() * 1000.0);
    int airqualityValue = airqualitysensor.slope();

    //print values in debug
    debugSerial.println(String("light: " + String(lightValue,DEC)));
    debugSerial.println(String("loudness: " + String(loudnessValue,DEC)));
    debugSerial.println(String("motion: " + String(motionValue,DEC)));
    debugSerial.println(String("temp: " + String(temperatureValue,DEC)));
    debugSerial.println(String("humidity: " + String(humidityValue,DEC)));
    debugSerial.println(String("pressure: " + String(pressureValue,DEC)));
    debugSerial.println(String("battery: " + String(battValue,DEC)));
    debugSerial.println(String("airquality: " + String(airqualityValue,DEC)));

  	//2 bytes range: 0 ~ 65535  degrees range with the following code:(-273.15 ~ +382.20)
  	int convertedTemp = (int)((temperatureValue + 273.15) * 100);
	
    //put the data in a packet:
    byte packet[14];
    //light sensor -> 2 bytes
    packet[0] = (byte)((lightValue >> 8) & 0xff);
    packet[1] = (byte)(lightValue & 0xff);
    //loudness sensor -> 2 bytes
    packet[2] = (byte)((loudnessValue >> 8) & 0xff);
    packet[3] = (byte)(loudnessValue & 0xff);
    //PIR motion sensor -> 1 byte
    packet[4] = (byte)(motionValue & 0xff);
    //temperature sensor -> 2 bytes
    packet[5] = (byte)((convertedTemp >> 8) & 0xff);
    packet[6] = (byte)(convertedTemp & 0xff);
    //humidity sensor -> 1 byte
    packet[7] = (byte)(humidityValue & 0xff);
    //pressure sensor -> 3 bytes
    packet[8] = (byte)((pressureValue >> 16) & 0xff);
    packet[9] = (byte)((pressureValue >> 8) & 0xff);
    packet[10] = (byte)(pressureValue & 0xff);
    //battery voltage -> 2 bytes
    packet[11] = (byte)((battValue >> 8) & 0xff);
    packet[12] = (byte)(battValue & 0xff);
    //air quality -> 1 byte
    packet[13] = (byte)(airqualityValue & 0xff);

    //send packet to server.
    device.sendPayload(packet, sizeof(packet));
    /*
    if (current_quality==0)
        Serial.println("High pollution! Force signal active");
    else if (current_quality==1)
        Serial.println("High pollution!");
    else if (current_quality==2)
        Serial.println("Low pollution!");
    else if (current_quality ==3)
        Serial.println("Fresh air");
     */
    sendNextAt = millis() + 10000;
  }
}


void initSensors()
{
  debugSerial.println("Initializing sensors, this can take a few seconds...");
  pinMode(LIGHT_SENSOR, INPUT);
  pinMode(LOUDNESS_SENSOR, INPUT);
  pinMode(PIR_MOTION_SENSOR, INPUT); 
  airqualitysensor.init(AIR_SENSOR);
  tph.begin();
}

void clearEEPROM()
{
  for (int i = 0 ; i < EEPROM.length() ; i++) {
    if(EEPROM.read(i) != 0)                     //skip already "empty" addresses
    {
      EEPROM.write(i, 0);                       //write 0 to address i
    }
  }
  debugSerial.println("EEPROM erased");
}

void writePW(String passw)
{
  int len = passw.length();
  byte data[len+1];
  passw.getBytes(data, len+1);
  for (int i=0; i<len; i++)
  {
    if (EEPROM.read(i+1) != data[i])
    {
      EEPROM.write(i+1, data[i]);
    }
  }
  if (EEPROM.read(len+1) != '\0')
  {
    EEPROM.write(len+1, '\0'); 
  }
}
char * readPW()
{
  static char passw[21];
  for (int i = 0 ; i < 20 ; i++) {
    byte value = EEPROM.read(i+1);
    passw[i] = (char)value;
  }
  //passw[11] = '\0'; // already written by writepw
  return passw;
}

float getRealBatteryVoltage()
{
  uint16_t batteryVoltage = analogRead(BATVOLTPIN);
  return (ADC_AREF / 1023.0) * (BATVOLT_R1 + BATVOLT_R2) / BATVOLT_R2 * batteryVoltage;
} 

ISR(TIMER1_OVF_vect)
{
  if(airqualitysensor.counter==61)//set 2 seconds as a detected duty
  {
      airqualitysensor.last_vol=airqualitysensor.first_vol;
      airqualitysensor.first_vol=analogRead(A2);
      airqualitysensor.counter=0;
      airqualitysensor.timer_index=1;
      PORTB=PORTB^0x20;
  }
  else
  {
    airqualitysensor.counter++;
  }
}
