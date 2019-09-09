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

import java.io.IOException;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
public class CumulocityHandlerTest {

  //can't test the CumulocityHandler fully without hard-coding a working username and password.
  //one could mock the CumulocityHandler but not much would be left that actually gets 'tested'
  //you'd only be really testing the String.split("\n") function.  
  
  @Test
  public void testCreateMeasurement() throws InterruptedException, IOException {
    
    byte[] testMeasurementData = 
        this.createTestMeasurementData(555, 55, 1, 26.640015f, 33, 101234, 4141, 3);

    CumulocityHandler cuHandler = new CumulocityHandler();
    String createdMeasurement = 
        cuHandler.createMeasurement("123", "faketimestamp", testMeasurementData).toJSON();
    
    String validateMeasurement = "{"
        + "\"source\":{\"id\":\"123\",\"type\":\"com_cumulocity_model_ID\",\"value\":null},"
        + "\"type\":"        + "\"Sodaq\","
        + "\"Loudness\":"    + "{\"I\":{\"unit\":\"loudness\"," + "\"value\":55}},"
        + "\"Motion\":"      + "{\"m\":{\"unit\":\"motion\","   + "\"value\":1}},"
        + "\"Temperature\":" + "{\"T\":{\"unit\":\"*C\","       + "\"value\":26.640015}},"
        + "\"AirQuality\":"  + "{\"q\":{\"unit\":\"quality\","  + "\"value\":3}},"
        + "\"Light\":"       + "{\"E\":{\"unit\":\"light\","    + "\"value\":555}},"
        + "\"Humidity\":"    + "{\"h\":{\"unit\":\"%RH\","      + "\"value\":33}},"
        + "\"Battery\":"     + "{\"U\":{\"unit\":\"mV\","       + "\"value\":4141}},"
        + "\"time\":"        + "\"faketimestamp\","
        + "\"Pressure\":"    + "{\"p\":{\"unit\":\"Pa\","       + "\"value\":101234}}"
        + "}";
    
    //doing "import static org.junit.Assert.*" causes checkstyle violations.
    org.junit.Assert.assertEquals(validateMeasurement, createdMeasurement);    
  }
  
  
  /**
   * creates a measurement from given information.
   * @param lightValue       possible device output: 0-1000
   * @param loudnessValue    possible device output: 0-1000
   * @param motionValue      possible device output: 0 / 1
   * @param temperatureValue possible device output: -273.15 ~ +382.20 , 0.01 precision
   * @param humidityValue    possible device output: 0-100
   * @param pressureValue    possible device output: +-101900 Pa
   * @param battValue        possible device output: 0 ~ +-4200
   * @param airqualityValue  possible device output: 1 / 2 / 3
   * @return returns the created measurement byte[]
   */
  private byte[] createTestMeasurementData(int lightValue, int loudnessValue, 
      int motionValue, float temperatureValue, int humidityValue, 
      long pressureValue, int battValue, int airqualityValue) {
    
    //convert the -273.15 ~ +382.20 range to a 0-65536 range to use the full 2 bytes
    int convertedTemp = (int)((temperatureValue + 273.15) * 100);
    
    return new byte[] {
        (byte) ((lightValue >> 8) & 0xff),     (byte) (lightValue & 0xff),
        (byte) ((loudnessValue >> 8) & 0xff),  (byte) (loudnessValue & 0xff),
        (byte) (motionValue & 0xff),
        (byte) ((convertedTemp >> 8) & 0xff),  (byte) (convertedTemp & 0xff),
        (byte) (humidityValue & 0xff),
        (byte) ((pressureValue >> 16) & 0xff), (byte) ((pressureValue >> 8) & 0xff), 
        (byte) (pressureValue & 0xff),
        (byte) ((battValue >> 8) & 0xff),      (byte) (battValue & 0xff),
        (byte) (airqualityValue & 0xff)};
  }
}
