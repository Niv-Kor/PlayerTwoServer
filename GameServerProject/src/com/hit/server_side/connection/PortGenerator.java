package com.hit.server_side.connection;
import java.net.DatagramSocket;
import java.net.SocketException;
import general_utility.math.RNG;

public class PortGenerator
{
	public static final int GENERAL_SERVICE = 1024;
	public static final int CLIENT_FINDER = 1025;
	private static final int MIN_PORT = 1026;
	private static final int MAX_PORT = (int) Character.MAX_VALUE;
	
	/**
	 * Generate an available port number, ready for connection.
	 * @return a free port number.
	 */
	public static int nextPort() {
		DatagramSocket testSocket;
		int port;
		
		//test ports until one manages to connect
		while (true) {
			try {
				port = RNG.generate(MIN_PORT, MAX_PORT);
				testSocket = new DatagramSocket(port);
				testSocket.close();
				break;
			}
			catch(SocketException e) {}
		}
		
		return port;
	}
}