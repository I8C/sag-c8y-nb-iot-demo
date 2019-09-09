package be.i8c.nbiotagent;
/*  
 * Copyright 2019 i8c N.V. (www.i8c.be)
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

import com.cumulocity.model.util.DateTimeUtils;

import java.util.Arrays;
import java.util.Base64;

public class DataPacket {

  private String ipAddress;
  private int port;
  
  private String deviceId = "";//IMEI
  private String password = "";
  private String cumulocityId = "";
  private byte[] payload;
  
  private String creationdateTimestamp;
  
  /** Creates a new datapacket from data from device.
   * @param ipAddress packet ip
   * @param port packet port
   * @param data packet data
   */
  public DataPacket(String ipAddress, int port, byte[] data) {
    this.ipAddress = ipAddress;
    this.port = port;
    this.creationdateTimestamp = DateTimeUtils.nowLocal().toString();
    
    this.extractData(data);
  }
  
  /** Creates a new datapacket from a queue message.
   * @param message the message from the queu containing device data
   */
  public DataPacket(String message) {
    final String[] data = message.split("\n");
    this.ipAddress = data[0];
    this.port = Integer.parseInt(data[1]);
    
    if (data.length > 2) {
      this.password = data[2];
      this.cumulocityId = data[3];
    }
  }
  
  private void extractData(byte[] data) {
    int newLine = nextNewLine(data);
    if (newLine == -1) {
      return; 
    }
    this.deviceId = new String(Arrays.copyOfRange(data, 0, newLine));
    data = Arrays.copyOfRange(data, newLine + 1, data.length);

    newLine = nextNewLine(data);
    if (newLine == -1) {
      return; 
    }
    this.password = new String(Arrays.copyOfRange(data, 0, newLine));
    data = Arrays.copyOfRange(data, newLine + 1, data.length);

    newLine = nextNewLine(data);
    if (newLine == -1) {
      return; 
    }
    this.cumulocityId = new String(Arrays.copyOfRange(data, 0, newLine));
    
    payload = Arrays.copyOfRange(data, newLine + 1, data.length);
  }

  private int nextNewLine(byte[] data) {
    int i = 0;
    while (data[i] != 10) {
      i++; // loop over the data until a newline is detected  (dec)10 == (hex)0A  == "\n" == newline
      if (i >= data.length) {
        return -1; //no newline in data
      }
    }
    return i;
  }
  
  public String getIpAddress() {
    return this.ipAddress;
  }
  
  public int getPort() {
    return this.port;
  }
  
  public boolean isValid() {
    return !this.deviceId.equals("");
  }

  public boolean isRegisterMessage() {
    return this.password.equals("") || this.cumulocityId.equals("");
  }

  public boolean isMeasurementMessage() {
    return !this.password.equals("") && !this.cumulocityId.equals("") && this.payload.length == 14;
  }
  
  /** Creates and returns the message containing all data for a register message.
   * @return registerMessage
   */
  public String getRegisterMessage() {
    return this.ipAddress + "\n" + this.port + "\n" + this.deviceId;
  }
  
  
  /** Creates and returns the message containing all data for a push measurements message.
   * @return pushMeasurementsMessage
   * @throws Exception exception
   */
  public String getPushMeasurementMessage() throws Exception {
    return this.ipAddress + "\n" + this.port + "\n"
        + this.deviceId + "\n" + this.password + "\n" + this.cumulocityId + "\n"
        + this.creationdateTimestamp + "\n" + new String(Base64.getEncoder().encode(payload));
  }

  /** Creates the message to containing the password and cumulocityid to send to the device.
   * @return password \n CumulocityId
   */
  public String getDeviceMessage() {
    return this.password + "\n" + this.cumulocityId;
  }
}
