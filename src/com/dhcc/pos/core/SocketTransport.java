package com.dhcc.pos.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

import com.dhcc.pos.packets.util.ConvertUtil;

public class SocketTransport {

	/*
	 * private static String host = "192.168.22.18"; 
	 * private static int port = 6061; 
	 * private static int timeout = 45000; 
	 * private static int headlength = 2;
	 */
	private String host;
	private int port;
	private int timeout;
	private int headlength;
	private int TPDUlength;

	/** 返回的报文 */
	String readLine = null;
	// byte[] lenBuffer = new byte[in.available()];// 报文头
	byte[] lenBuffer = null;// 报文头
	/** 2字节的报文长度值 */
	byte[] reqHeaderLenght = new byte[2];// 报文头
	/** 2字节的报文长度值 */
	byte[] respHeaderLenght = new byte[2];// 报文头
	Socket socket = null;
	/** 输出流 */
	OutputStream out = null;
	/** 输入流 */
	InputStream is = null;
	/** 响应报文 */
	byte[] respMsg = null;

	public SocketTransport() {
		this.host = "58.221.92.138";
		this.port = 9099;
		this.timeout = 45000;
		this.headlength = 12;
	}

	public SocketTransport(String host, int port, int timeout, int headlength) {
		this.host = host;
		this.port = port;
		this.timeout = timeout;
		this.headlength = headlength;
	}


	/**
	 * @param reqMsg
	 * @return
	 * @throws CommunicationException
	 */
	public byte[] sendData(byte reqMsg[]) throws Exception {

		System.out.println("####################sendData####################" + "\r");

		/** 第二种（合理） 拿到报文总字节长度（前两个字节定义的长度） */
		int reqMsgLen = reqMsg.length;

		reqHeaderLenght[0] = (byte) ((reqMsgLen & 0xff00) >> 8);
		reqHeaderLenght[1] = (byte) (reqMsgLen & 0xff);

		/**
		 * 组装字节类型报文 数据长度+{头文件（tpdu[BCD压缩5字节]+头文件[BCD压缩6字节]）+
		 * 报文类型【BCD压缩2字节】+位图【8字节】&&位图对应的域值}
		 * */
		System.out.println("reqMsgLen：" + reqMsgLen);
		ByteBuffer sendBuf = ByteBuffer.allocate(reqHeaderLenght.length + reqMsgLen);
		/* 2个字节的报文长度值 */
		sendBuf.put(reqHeaderLenght);
		/* 头文件（tpdu[BCD压缩5字节]+头文件[BCD压缩6字节]）+ 报文类型【BCD压缩2字节】+位图【8字节】&&位图对应的域值+ */
		sendBuf.put(reqMsg);

		reqMsg = sendBuf.array();
		System.out.println("发送报文msg 16进制:\r" + ConvertUtil.trace(reqMsg));
		
		try {
			// 获取一个连接到socket服务的套接字
			socket = new Socket(host, port);

			/* 设置超时时间 */
			socket.setSoTimeout(timeout);

			is = socket.getInputStream();

			out = socket.getOutputStream();

			out.write(reqMsg);

			out.flush();

			
			respMsg = revData(respHeaderLenght, is);
			
			System.out.println("接收到的交易平台报文16进制:\r" + ConvertUtil.trace(respMsg));
			/**
			 * 响应报文写入文件
			 * */

			/**
			 * ==========================记录响应报文到文件中（可关闭）======================
			 */

		} catch (ConnectException e) {
			e.printStackTrace();
			System.out.println("无法连接到地址");
			throw new Exception("网络问题请重试");
		} catch (SocketException e) {
			e.printStackTrace();
			System.out.println("socket协议有误");
			throw new Exception("网络问题请重试");
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception("网络问题请重试");
		} finally {
			try {
				if (is != null) {
					is.close();
				}
				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
				throw new Exception("关闭socket异常");
			}
		}
		return respMsg;
	}

	
	
	/**
	 * @param respHeaderLenght
	 * @param is
	 * @return
	 */
	private byte[] revData(byte[] respHeaderLenght, InputStream is) {
		try {
			is.read(respHeaderLenght);
			int size = ((respHeaderLenght[0] & 0xff) << 8) | (respHeaderLenght[1] & 0xff);

			respMsg = new byte[size];

			long k = 0;
			char c;
			int readCount = 0; // 已经成功读取的字节的个数

			while (readCount < size) {
				/** 一次性拿到 */
				readCount += is.read(respMsg, readCount, size - readCount);
			}

			for (byte b : respMsg) {
				// convert byte to character
				if (b == 0)
					// if b is empty
					c = '-';
				else
					// if b is read
					c = (char) b;

				// prints character
				System.out.print(c);
			}
			if (k == -1) {
				is.close();
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return respMsg;
	}

}
