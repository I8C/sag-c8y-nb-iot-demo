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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;


@RunWith(CamelSpringBootRunner.class)
@SpringBootTest
@MockEndpoints
public class UdpConsumerTest {

  @EndpointInject(uri = "{{udpConsumer.outboundMeasurementQueue}}")
  private MockEndpoint measurementEndpoint;
  
  @EndpointInject(uri = "{{udpConsumer.outboundRegisterQueue}}")
  private MockEndpoint registerEndpoint;

  private int loadTestsClientsAmount = 5;
  private int packetSizeTestSize = 500;
  
  /**
   * reset endpoints before each test.
   */
  @Before
  public void resetEndPoints() {
    this.registerEndpoint.reset();
    this.measurementEndpoint.reset();
    //to prevent using DirtiesContext resetting is required.
  }
  
  
  @Test
  public void testSendMeasurementNormal() throws InterruptedException, IOException {
    
    byte[] payload = createFakePayload(
        /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
        /*device pw:*/   "10ch4r1uNk", 
        /*cumulocityId*/ "1234", 
        /*measurements*/ 14);
    
    this.sendPacket("localhost", 8888, payload);
    
    /* doesn't work even when the packets timestamps are mocked and the packets are equal:
    this.MeasurementEndpoint.expectedBodiesReceived(
        "i8cTEST\npassword\n1234\n192.168.1.1\n4321\ntimestamp\nAmgACwBzmiQBjf8QIgM=");
    producerTemplate.sendBody("{{udpConsumer.sourceEndPoint}}", packet);*/

    this.measurementEndpoint.expectedMessageCount(1);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(0);
    this.registerEndpoint.assertIsSatisfied(15L);
    
  }
  

  @Test
  public void testSendMeasurementBigPayload() throws InterruptedException, IOException {
    
    byte[] payload = createFakePayload(
        /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
        /*device pw:*/   "10ch4r1uNk", 
        /*cumulocityId*/ "1234", 
        /*measurements*/ this.packetSizeTestSize);
    
    this.sendPacket("localhost", 8888, payload);

    //agent drops packets with a payload that is not 14 bytes
    this.measurementEndpoint.expectedMessageCount(0);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(0);
    this.registerEndpoint.assertIsSatisfied(15L);
    
  }

  
  //load tests:
  @Test
  public void testSendManyMeasurementsNormal() throws InterruptedException, IOException {
    //lots of different devices send measurements:
    for (int i = 0; i < loadTestsClientsAmount; i++) {

      byte[] payload = createFakePayload(
          /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
          /*device pw:*/   "10ch4r1uNk", 
          /*cumulocityId*/ "1234", 
          /*measurements*/ 14);
      
      this.sendPacket("localhost", 8888, payload);
    }
    
    this.measurementEndpoint.expectedMessageCount(loadTestsClientsAmount);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(0);
    this.registerEndpoint.assertIsSatisfied(15L);
    
  }

  
  @Test
  public void testSendManyMeasurementsBigPayload() throws InterruptedException, IOException {
    //lots of different devices send measurements:
    for (int i = 0; i < loadTestsClientsAmount; i++) {
      
      byte[] payload = createFakePayload(
          /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
          /*device pw:*/   "10ch4r1uNk", 
          /*cumulocityId*/ "1234", 
          /*measurements*/ this.packetSizeTestSize);
      
      this.sendPacket("localhost", 8888, payload);
    }
    
    //agent drops packets with a payload that is not 14 bytes
    this.measurementEndpoint.expectedMessageCount(0);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(0);
    this.registerEndpoint.assertIsSatisfied(15L);
    
  }

  
  //register tests:
  @Test
  public void testSendRegister() throws InterruptedException, IOException {
    
    byte[] payload = createFakePayload(
        /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
        /*device pw:*/   "", 
        /*cumulocityId*/ "", 
        /*measurements*/ 14);
    
    this.sendPacket("localhost", 8888, payload);

    this.measurementEndpoint.expectedMessageCount(0);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(1);
    this.registerEndpoint.assertIsSatisfied(15L);
    
  }
  
  
  @Test
  public void testSendRegisterNoData() throws InterruptedException, IOException {
    
    byte[] payload = createFakePayload(
        /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
        /*device pw:*/   "", 
        /*cumulocityId*/ "", 
        /*measurements*/ 0);
    
    this.sendPacket("localhost", 8888, payload);

    this.measurementEndpoint.expectedMessageCount(0);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(1);
    this.registerEndpoint.assertIsSatisfied(15L);
  }
  

  @Test
  public void testSendManyRegistersNoData() throws InterruptedException, IOException {

    for (int i = 0; i < loadTestsClientsAmount; i++) {
      
      byte[] payload = createFakePayload(
          /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
          /*device pw:*/   "", 
          /*cumulocityId*/ "", 
          /*measurements*/ 0);
      
      this.sendPacket("localhost", 8888, payload);
    }
    
    this.measurementEndpoint.expectedMessageCount(0);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(loadTestsClientsAmount);
    this.registerEndpoint.assertIsSatisfied(15L);
  }
  

  @Test
  public void testSendManyRegistersNormal() throws InterruptedException, IOException {
    for (int i = 0; i < loadTestsClientsAmount; i++) {
      
      byte[] payload = createFakePayload(
          /*device addr:*/ "" + ((long)(Math.random() * 1000000000000000L + 1)),
          /*device pw:*/   "", 
          /*cumulocityId*/ "", 
          /*measurements*/ 14);
      
      this.sendPacket("localhost", 8888, payload);
    }
    
    this.measurementEndpoint.expectedMessageCount(0);
    this.measurementEndpoint.assertIsSatisfied(15L);
    this.registerEndpoint.expectedMessageCount(loadTestsClientsAmount);
    this.registerEndpoint.assertIsSatisfied(15L);
  }
  
  
  private byte[] createFakePayload(
      String deviceAddress, String password, String cumulocityId, int payloadSize) {
    
    //header with credentials:
    byte[] fakeDeviceCredHeader = 
        (deviceAddress + "\n" + password + "\n" + cumulocityId + "\n").getBytes();
    
    //fake random device measurement:
    byte[] fakeDeviceMeasurementData = new byte[payloadSize];
    new Random().nextBytes(fakeDeviceMeasurementData);
    
    return mergeByteArrays(fakeDeviceCredHeader, fakeDeviceMeasurementData);
  }
  
  /**
   * Merges variable amount of byte[].
   * @param byteArrays variable amount of byte[] parameters
   * @return a merged byte[] which contains all byte[] parameters
   */
  private byte[] mergeByteArrays(byte[]...byteArrays) {
    int finalByteArrayLength = 0;
    
    for (byte[] array: byteArrays) {
      finalByteArrayLength += array.length;
    }
    
    byte[] finalByteArray = new byte[finalByteArrayLength];
    
    for (byte[] array: byteArrays) {
      
      System.arraycopy(array, 0, finalByteArray, 
          (finalByteArray.length - finalByteArrayLength), array.length);
      finalByteArrayLength -= array.length;
    }
    return finalByteArray;
  }
  
  private void sendPacket(String host, int port, byte[] payload) throws IOException {
    /* //test using mockito: 
    DataPacket packet = Mockito.spy(new DataPacket( "192.168.1.1", 4321, testData));
    Mockito.when(packet.getTimeStamp()).thenReturn("faketimestamp");
     */
    DatagramSocket socket = new DatagramSocket(); 
    InetAddress address = InetAddress.getByName(host);
    DatagramPacket datagramPacket = new DatagramPacket(payload, payload.length, address, port); 
    socket.connect(address, port); 
    socket.send(datagramPacket);
    socket.close();
  }
}