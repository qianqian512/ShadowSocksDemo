package org.hum.socks.v0._2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Http代理，无论收到什么样的Request，只返回固定Response
 * @author huming
 */
public class HttpSimpleServer {

	static class Config {
		public static final int PORT = 8002;
	}
	
	public static void main(String[] args) {
		try {
			@SuppressWarnings("resource")
			ServerSocket server = new ServerSocket(Config.PORT);
			System.out.println("server started, listenning on " + Config.PORT);
			while (true) {
				// 1.客户端连接到Web服务器
				Socket socket = server.accept();
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String requestHeader;
				// 2.服务端接收客户端发送的请求
				System.out.println("request line:" + br.readLine());
				System.out.println("request header:");
				while (!(requestHeader = br.readLine()).equals("")) {
					System.out.println(requestHeader);
				}
				
				StringBuilder sbuilder = new StringBuilder();
				for (int i = 0; i < 1000; i++) {
					sbuilder.append("hello world " + i);
				}
				
				// 3.服务器处理请求饭后返回HTTP响应
				PrintWriter pw = new PrintWriter(socket.getOutputStream());
				String resp = "HTTP/1.1 200 OK \n"
						+ "Content-type:text/html\n"
						+ "\n"
						+ "<h1>" + sbuilder + "</h1>";
				System.out.println("write len=" + resp.length());
				pw.write(resp);
				pw.flush();
				
				// 4.释放连接
				socket.close();
				
				// 5.后续...客户端处理服务端的响应
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
