#!/bin/bash
# installArduinoInstaller.sh
# Installs the Arduino installer under /opt/arduino
# Execute this script as user vagrant
#  
# Copyright 2019 i8c N.V. (www.i8c.be)
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# 
cd /tmp

if [ ! -d /opt/arduino-1.8.5 ]; then
    sudo mkdir /opt/arduino-1.8.5 && sudo chown -R vagrant:vagrant /opt/arduino-1.8.5
fi

wget https://downloads.arduino.cc/arduino-1.8.5-linux64.tar.xz
cd /opt/arduino-1.8.5

tar -xJf /tmp/arduino-1.8.5-linux64.tar.xz

cd arduino-1.8.5
./install.sh