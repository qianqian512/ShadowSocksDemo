package org.hum.netty.bytebuf;

import java.util.List;
import org.hum.netty.bytebuf.Decoder.State;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

public class Decoder extends ReplayingDecoder<State> {

	enum State {
		INIT, SUCCESS, FAILURE
	}

	public Decoder() {
		super(State.INIT);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		switch (state()) {
		case INIT:
			int len = in.readInt();
			FullByteMessage msg = new FullByteMessage();
			msg.datas = new byte[len];
			in.readBytes(msg.datas);
			state(State.SUCCESS);
			out.add(msg);
			break;
		case SUCCESS:
			break;
		case FAILURE:
			break;
		}
	}
}