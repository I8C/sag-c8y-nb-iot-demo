#Copyright 2018 i8c N.V. (www.i8c.be)
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.

#!/bin/bash
sudo chown vagrant /dev/ttyUSB0
cp /opt/arduino-1.8.5/arduino-1.8.5/hardware/arduino/avr/cores/arduino/Arduino.h /opt/arduino-1.8.5/arduino-1.8.5/hardware/arduino/avr/cores/arduino/arduino.h
cp -r arduino-nbiot-sdk-master/ /opt/arduino-1.8.5/arduino-1.8.5/libraries/
cp -r AirQuality_Sensor/ /opt/arduino-1.8.5/arduino-1.8.5/libraries/
cp -r TPH_Library/ /opt/arduino-1.8.5/arduino-1.8.5/libraries/
