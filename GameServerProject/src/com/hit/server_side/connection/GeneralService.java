package com.hit.server_side.connection;
import java.io.IOException;

public class GeneralService
{
	private static Protocol serverSideProtocol;
	
	public static void init() throws IOException {
		serverSideProtocol = new Protocol(PortGenerator.GENERAL_SERVICE, null);
	}
	
	public static boolean notify(JSON msg, int targetPort) {
		try {
			serverSideProtocol.setTargetPort(targetPort);
			serverSideProtocol.send(msg);
			ServerLogger.print("sent '" + msg + "' to " + serverSideProtocol.getTargetPort());
			return true;
		}
		catch (IOException e) {
			ServerLogger.print("Could not inform client with the message '" + msg + "'.");
			e.printStackTrace();
			return false;
		}
	}
}
