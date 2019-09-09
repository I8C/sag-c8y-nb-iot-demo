#!/bin/bash
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

MAVEN_POM_ARTIFACT_ID=nbIoTAgent
SRC_LOCATION=../../../../src/NB-IoT_agent

if [ ! -d "$SRC_LOCATION/target/" ]; then
  echo "dir doesn't exist"
  sleep 3
  pushd $SRC_LOCATION
  mvn package -DskipTests
  popd
fi

cp -f $SRC_LOCATION/target/$MAVEN_POM_ARTIFACT_ID-*.jar .

docker build -t sag-c8y-nb-iot-demo-agent .
# Cleanup
rm $MAVEN_POM_ARTIFACT_ID-*.jar
