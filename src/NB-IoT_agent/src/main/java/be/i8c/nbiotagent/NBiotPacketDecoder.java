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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

import org.springframework.stereotype.Component;


@ChannelHandler.Sharable
@Component("NBiotPacketDecoder")
public class NBiotPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {
  
  @Override
  protected void decode(ChannelHandlerContext ctx, DatagramPacket datagram, List<Object> out)
      throws Exception {

    if (!ctx.equals(NBiotPacketEncoder.ctx)) {
      NBiotPacketEncoder.ctx = ctx;//used by NBiotPacketEncoder to be able to send packets back.
    }
    
    //extract bytes from datagram
    byte[] bytes = new byte[datagram.content().readableBytes()];
    datagram.content().readBytes(bytes);
    
    //add to out so the pipeline can use it
    out.add(new DataPacket(datagram.sender().getHostName(), datagram.sender().getPort(), bytes));
  }
  

}
