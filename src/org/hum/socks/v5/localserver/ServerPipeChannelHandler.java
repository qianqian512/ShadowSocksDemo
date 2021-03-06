package org.hum.socks.v5.localserver;

import org.hum.socks.v5.common.Constant;
import org.hum.socks.v5.common.PipeChannelHandler;
import org.hum.socks.v5.common.codec.ProxyConnectMessageEncoder;
import org.hum.socks.v5.common.codec.ProxyPreparedMessageDecoder;
import org.hum.socks.v5.common.model.ProxyConnectMessage;
import org.hum.socks.v5.common.model.ProxyPreparedMessage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;

public class ServerPipeChannelHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {

	private final String PROXY_HOST = "127.0.0.1";
	private final int PROXY_PORT = 1081;
	
	@Override
	protected void channelRead0(final ChannelHandlerContext browserCtx, final SocksCmdRequest msg) throws Exception {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.group(browserCtx.channel().eventLoop());
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.handler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline().addLast(new ProxyPreparedMessageDecoder());
				ch.pipeline().addLast(new PrepareConnectChannelHandler(browserCtx));
			}
		});
		// bootstrap.handler(new EncryptPipeChannelHandler("local.pipe1", browserCtx.channel())); // 读proxy并向browser写（从proxy到browser）
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
		bootstrap.connect(PROXY_HOST, PROXY_PORT).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(final ChannelFuture proxyServerChannelFuture) throws Exception {
				// 将ip和port输出到proxy-server
				ProxyConnectMessage connectMsg = new ProxyConnectMessage(Constant.MAGIC_NUMBER, msg.host().length(), msg.host(), (short) msg.port());
				proxyServerChannelFuture.channel().pipeline().addLast(new ProxyConnectMessageEncoder());
				proxyServerChannelFuture.channel().writeAndFlush(connectMsg).addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						browserCtx.pipeline().remove(ServerPipeChannelHandler.this);
					}
				});
			}
		});
	}
	
	private static class PrepareConnectChannelHandler extends SimpleChannelInboundHandler<ProxyPreparedMessage> {
		
		private ChannelHandlerContext browserCtx;
		public PrepareConnectChannelHandler(ChannelHandlerContext browserCtx) {
			this.browserCtx = browserCtx;
		}

		@Override
		protected void channelRead0(ChannelHandlerContext proxyCtx, ProxyPreparedMessage msg) throws Exception {
			// 开启数据转发管道，读proxy并向browser写（从proxy到browser）
			proxyCtx.pipeline().addLast(new PipeChannelHandler("local.pipe1", browserCtx.channel()));
			proxyCtx.pipeline().remove(PrepareConnectChannelHandler.class);
			proxyCtx.pipeline().remove(ProxyPreparedMessageDecoder.class);
			// 读browser并向proxy写（从browser到proxy）
			browserCtx.pipeline().addLast(new PipeChannelHandler("local.pipe2", proxyCtx.channel()));
			// 与proxy-server握手完成后，告知browser socks协议结束，后面可以开始发送真正数据了(为了保证数据传输正确性，flush最好还是放到后面)
			browserCtx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.IPv4));
			System.out.println("flush browser success");
		}
	}
}
