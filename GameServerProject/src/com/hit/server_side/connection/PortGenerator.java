package com.hit.server_side.connection;
import java.net.DatagramSocket;
import java.net.SocketException;
import math.RNG;

public class PortGenerator
{
	public static enum AllocatedPorts {
		GENERAL_SERVICE,
		CLIENT_FINDER,
		LAUNCHER_APPLICANT;
		
		private int port;
		
		private AllocatedPorts() {
			port = nextPort();
		}
		
		/**
		 * @return allocated port number.
		 */
		public int getPort() { return port; }
	}
	
	private static final int MIN_PORT = 1024;
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
				port = (int) RNG.generate(MIN_PORT, MAX_PORT);
				testSocket = new DatagramSocket(port);
				testSocket.close();
				break;
			}
			catch(SocketException e) {}
		}
		
		return port;
	}
}