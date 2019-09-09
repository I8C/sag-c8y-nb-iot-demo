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

import static io.netty.buffer.Unpooled.buffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

import java.net.InetSocketAddress;

import org.apache.camel.Body;
import org.springframework.stereotype.Component;

@ChannelHandler.Sharable
@Component("NBiotPacketEncoder")
public class NBiotPacketEncoder {
  public static ChannelHandlerContext ctx;
  
  
  /** encode & send the datapacket.
   * @param datapacket the packet to encode & send
   * @throws Exception Exception
   */
  public void encode(@Body DataPacket datapacket)
      throws Exception {
    
    byte[] outMsg = datapacket.getDeviceMessage().getBytes();
    ByteBuf buf = buffer(outMsg.length);
    buf.writeBytes(outMsg);

    //make sure ctx exists. (set by NBiotPacketDecoder when any packet arrives)
    while (NBiotPacketEncoder.ctx == null) { }

    DatagramPacket dp = new DatagramPacket(buf, 
      /*dest*/  new InetSocketAddress(datapacket.getIpAddress(), datapacket.getPort()), 
      /*src*/   new InetSocketAddress("0.0.0.0", 8888));
    NBiotPacketEncoder.ctx.writeAndFlush(dp);// sends using proper working ctx
    
  }
}
