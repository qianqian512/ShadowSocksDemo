package org.hum.socks.v7.inside;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpProxyServer {

	private int port;
	private ServerBootstrap serverBootstrap;

	public HttpProxyServer(int port) {
		this.port = port;
		init();
	}

	public void init() {
		serverBootstrap = new ServerBootstrap();
		NioEventLoopGroup masterLoopGroup = new NioEventLoopGroup(1);
		NioEventLoopGroup slaveLoopGroup = new NioEventLoopGroup(8);
		serverBootstrap.group(masterLoopGroup, slaveLoopGroup);
		serverBootstrap.channel(NioServerSocketChannel.class);
		serverBootstrap.childHandler(new ChannelInitializer<Channel>() {
			@Override
			protected void initChannel(Channel ch) throws Exception {
				ch.pipeline().addLast(new HttpServerCodec());
				ch.pipeline().addLast(new HttpObjectAggregator(65536));
				ch.pipeline().addLast(new HttpProxyServerHandler());
			}
		});
	}

	public void start() {
		serverBootstrap.bind(port);
	}
	
	public static void main(String[] args) {
		new HttpProxyServer(5432).start();
	}
}
