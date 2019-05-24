package com.hit.server_side.connection;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ServerSideProtocol
{
	protected InetAddress serverAddress;
	protected DatagramSocket socket;
	protected Integer port, target;
	
	/**
	 * @throws IOException when the socket cannot connect to the host.
	 */
	public ServerSideProtocol() throws IOException {
		this.serverAddress = InetAddress.getLocalHost();
		this.port = PortGenerator.nextPort();
		connect();
	}
	
	/**
	 * @param port - The port this protocol will use
	 * @param targetPort - The port this protocol will communicate with
	 * @throws IOException when the socket cannot connect to the host.
	 */
	public ServerSideProtocol(Integer port, Integer targetPort) throws IOException {
		this();
		this.target = targetPort;
		disconnect();
		this.port = port;
		connect();
	}
	
	/**
	 * Create a socket and connect to the host.
	 * 
	 * @throws SocketException when the socket is already binded.
	 */
	private void connect() throws SocketException {
		socket = new DatagramSocket(port);
	}
	
	/**
	 * Close the socket.
	 */
	private void disconnect() {
		socket.close();
	}
	
	/**
	 * Send a String message to the target port.
	 * 
	 * @param msg - The message to send
	 * @throws IOException when the target port is unavailable for sending messages to.
	 */
	public void send(String msg) throws IOException {
		byte[] data = msg.getBytes();
		DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, target);
		socket.send(packet);
	}
	
	/**
	 * Receive a message from the target port.
	 * This is a dangerous method and can cause starvation if not handled carefully.
	 * 
	 * @return an array of the message parts, where the first part is the request string.
	 * @throws IOException when the target port is unavailable for receiving messages from.
	 */
	public String[] receive() throws IOException {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		socket.receive(packet);
		String message = new String(packet.getData(), 0, packet.getLength());
		return message.split(":");
	}
	
	
	/**
	 * @return the port this protocol uses.
	 */
	public int getPort() { return port; }
	
	/**
	 * @return the target port this protocol communicates with.
	 */
	public int getTargetPort() { return target; }
	
	/**
	 * @param p - The new port to use
	 */
	public void setPort(int p) { port = p; }
	
	/**
	 * @param p - The new target port to communicate with
	 */
	public void setTargetPort(int p) { target = p; }
}