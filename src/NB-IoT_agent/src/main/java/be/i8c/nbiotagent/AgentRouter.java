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
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

/**
 * The Agent Camel route to route device messages from the devices to Cumulocity 
 * and optional responses back from Cumulocity to the devices with
 * the rabbitmq queueing broker in between.
 */
@Component
public class AgentRouter extends RouteBuilder { 

  @Override
  public void configure() {
    
    //ROUTE 1: from UDP to rabbitmq
    from("{{udpConsumer.sourceEndPoint}}")
      .filter(simple("${body.isValid}"))
        .choice()
          .when(simple("${body.isMeasurementMessage}"))
            .transform().simple("${body.getPushMeasurementMessage}")
            .to("{{udpConsumer.outboundMeasurementQueue}}")
          .when(simple("${body.isRegisterMessage}"))
            .transform().simple("${body.getRegisterMessage}")
            .to("{{udpConsumer.outboundRegisterQueue}}")
        .end();
    
    //ROUTE 2: from rabbitmq, push measurement to cumulocity, authfail: send to credentialsConsumer
    from("{{measurementConsumer.inboundQueue}}")
      .transform().method("CumulocityHandler", "pushMeasurement(${body})")
        .filter(simple("${body} != ''"))
          .removeHeaders("*")//prevent sending also to inboundQueue
          .to("{{measurementConsumer.outboundQueue}}")
        .end();
    
    //ROUTE 3: from rabbitmq, register on cumulocity, cred response: send to credentialsConsumer
    from("{{registerConsumer.inboundQueue}}")
      .transform().method("CumulocityHandler", "registerDevice(${body})")
        .filter(simple("${body} != ''"))
          .removeHeaders("*")//prevent sending also to inboundQueue
          .to("{{registerConsumer.outboundQueue}}")
        .end();
    
    //ROUTE 4: from rabbitmq credentialQueue to device
    from("{{credentialConsumer.inboundQueue}}")
      .transform().method("DataPacketCreator", "createPacketFromMessage(${body})")
      .transform().method("NBiotPacketEncoder", "encode(${body})");//sends the packet to the device
  }

}
