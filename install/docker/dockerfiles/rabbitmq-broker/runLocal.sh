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

# Run this file to run the rabbitmq-broker locally inside a docker container
docker run -p 15672:15672 -p 5672:5672 -e RABBITMQ_DEFAULT_USER='user1' -e RABBITMQ_DEFAULT_PASS='pass1' -e RABBITMQ_DEFAULT_VHOST='vhost1' sag-c8y-nb-iot-demo-rabbitmq-broker