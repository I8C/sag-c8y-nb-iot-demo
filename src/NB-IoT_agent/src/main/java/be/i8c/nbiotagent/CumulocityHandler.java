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

import c8y.IsDevice;

import com.cumulocity.model.authentication.CumulocityCredentials;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.sdk.client.PlatformImpl;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.devicecontrol.DeviceCredentialsApi;

import java.util.Base64;
import java.util.HashMap;

import org.apache.camel.Body;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component("CumulocityHandler")
public class CumulocityHandler {
  
  @Value("${cumulocity.baseUrl}")
  private String baseUrl;
  @Value("${cumulocity.bootstrapUsername}")
  private String bootstrapUsername;
  @Value("${cumulocity.bootstrapPassword}")
  private String bootstrapPassword;
  
  private DeviceCredentialsApi registerCredentialsApi;

  private void configureRegisterPlatform() { 
    //can't use a constructor instead of this (nullpointerexception)
    if (this.registerCredentialsApi == null) {
      
      PlatformImpl registerplatform = new PlatformImpl(this.baseUrl, 
          new CumulocityCredentials.Builder(this.bootstrapUsername, 
              this.bootstrapPassword).build());
      
      registerplatform.setForceInitialHost(true);
      
      this.registerCredentialsApi = registerplatform.getDeviceCredentialsApi();
      
      registerplatform.close();
      
    }
  }
  
  
  
  /** tries to register the device.
   *  this function will split the queuemessage into ip, port and deviceId (IMEI)
   *  the polling for the password happens using the Cumulocity SDK 
   *  this polling will cause an exception when no password is available(not accepted on cumulocity)
   *  this SDK exception will be caught, the function will return an empty string in that case
   *  when the password is available:
   *    it will authenticate and create the 'SODAQ' device in the inventory
   *    it will return a string containing the ip, port, password and cumulocity ID
   * @param queueRegisterMessage the queue message containing all device data.
   * @return returns the password if available.
   */
  @SuppressWarnings("serial")
  public String registerDevice(String queueRegisterMessage) {
    
    final String[] data = queueRegisterMessage.split("\n");
    
    final String ipAddress = data[0];
    final int port = Integer.parseInt(data[1]);
    
    final String deviceId = data[2]; //IMEI

    //Cumulocity username format is device_<ID>
    final String username = "device_" + deviceId;
    
    this.configureRegisterPlatform();
    try {
      //will cause SDKException if no password is available:
      String password = 
          registerCredentialsApi.pollCredentials(deviceId).getPassword();

      //creates the authenticated platform:
      PlatformImpl platform = new PlatformImpl(this.baseUrl, 
          new CumulocityCredentials.Builder(username, password).build());
      platform.setForceInitialHost(true);
      
      //create device on platform:
      ManagedObjectRepresentation mo = new ManagedObjectRepresentation();
      mo.setName(username);
      mo.setType("Sodaq_device");
      mo.set(new IsDevice());
      mo.set(new HashMap<String,Integer>() {{
          put("responseInterval", 1);
        }
      }, "c8y_RequiredAvailability");

      mo = platform.getInventoryApi().create(mo);
      platform.close();
      
      return ipAddress + "\n" + port + "\n" + password + "\n" + mo.getId().getValue();

    } catch (SDKException e) {
      //failed to register device
      
      return "";
    }
  }


  /** tries to push a device measurement.
   * @param queueMeasurementMessage the queue message containing all device data.
   * @return if the authentication failed it will return a message to reset the device.
   */
  public String pushMeasurement(@Body String queueMeasurementMessage) {
    
    final String[] data = queueMeasurementMessage.split("\n");

    final String ipAddress = data[0];
    final int port = Integer.parseInt(data[1]);
    
    final String deviceId = data[2];//IMEI
    final String password = data[3];
    final String cumulocityId = data[4];
    
    final String creationdateTimestamp = data[5];
    final byte[] devicePayload = Base64.getDecoder().decode(data[6].getBytes());

    
    //Cumulocity requires the username to begin with "device_"
    String username = "device_" + deviceId; 
    
    PlatformImpl platform = new PlatformImpl(this.baseUrl, 
        new CumulocityCredentials.Builder(username, password).build());
    platform.setForceInitialHost(true);
    
    try {
      //incorrect cred: 'getMeasurementApi()' SDKException
      platform.getMeasurementApi().createWithoutResponse(
          this.createMeasurement(cumulocityId, creationdateTimestamp, devicePayload));

      platform.close();
      
    } catch (SDKException e) {
      
      platform.close();
      return ipAddress + "\n" + port + "\n\n";
    }
    return "";
  }

  
  /**
   * Creates a new MeasurementRepresentation.
   * @param cumulocityId the cumulocity ID
   * @param creationdateTimestamp the timestamp cumulocity will use to put this measurement on
   * @param payload the device payload containing all values
   * @return a new MeasurementRepresentation.
   */
  public MeasurementRepresentation createMeasurement(
        final String cumulocityId, final String creationdateTimestamp, byte[] payload) {

    final int light =     Integer.parseInt(String.format("%02X%02X", 
                            payload[0], payload[1]), 16);
    
    final int loudness =  Integer.parseInt(String.format("%02X%02X", 
                            payload[2], payload[3]), 16);
    
    final int motion =      payload[4];
    
    //temp 2 bytes: 0 ~ 65535 -> 0 ~ 655.35 K -> -273.15 ~ +382.20 �C
    final float temperature = Integer.parseInt(String.format("%02X%02X", 
                            payload[5], payload[6]), 16) * 0.01f - 273.15f;
    
    final int humidity =    payload[7];
    
    final int pressure =  Integer.parseInt(String.format("%02X%02X%02X", 
                            payload[8], payload[9], payload[10]), 16);
    
    final int battery =   Integer.parseInt(String.format("%02X%02X", 
                            payload[11], payload[12]), 16);
    
    final int airquality =  payload[13];
    
    return new MeasurementRepresentation() {{
          setProperty("time", creationdateTimestamp);
          setSource(new ManagedObjectRepresentation() {{
              setProperty("type", "com_cumulocity_model_ID");
              setProperty("id", cumulocityId);
              setProperty("value", null);
            }
          });
          setType("Sodaq");
          set(createReading("E", "light",    light),       "Light");
          set(createReading("I", "loudness", loudness),    "Loudness");
          set(createReading("m", "motion",   motion),      "Motion");
          set(createReading("T", "*C",       temperature), "Temperature");
          set(createReading("h", "%RH",      humidity),    "Humidity");
          set(createReading("p", "Pa",       pressure),    "Pressure");
          set(createReading("U", "mV",       battery),     "Battery");
          set(createReading("q", "quality",  airquality),  "AirQuality");
        }
    };
  }
  
  

  /**
   * creates a new reading map.
   * @param fragment the fragment name for this measurement, example: T for temperature, A for area
   * @param unit the unit of the value of the measurement, example: �C
   * @param value the value of the measurement, example: 21
   * @return a map of the fragment containing a map of the value and the unit.
   */
  @SuppressWarnings("serial")
  private HashMap<String, Object> createReading(
      final String fragment, final String unit, final Object value) {
    
    return new HashMap<String, Object>() {{
          put(fragment, new HashMap<String, Object>() {{ 
              put("unit",  unit); 
              put("value", value); 
            }
          }); 
        }
    };
  }
  
}
