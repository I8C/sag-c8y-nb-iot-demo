#!/bin/bash
# installEclipseInstaller.sh
# Installs the Eclipse installer under /opt/eclipse
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

if [ ! -d /opt/eclipse ]; then
	sudo mkdir /opt/eclipse && sudo chown -R vagrant:vagrant /opt/eclipse
fi

wget http://ftp.snt.utwente.nl/pub/software/eclipse//oomph/products/eclipse-inst-linux64.tar.gz
gunzip eclipse-inst-linux64.tar.gz

cd /opt/eclipse

tar -xvf /tmp/eclipse-inst-linux64.tar
