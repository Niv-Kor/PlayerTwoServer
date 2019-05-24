package com.hit.server_side.connection;
import java.io.IOException;

public class GeneralService
{
	private static ServerSideProtocol serverSideProtocol;
	
	public static void init() throws IOException {
		serverSideProtocol = new ServerSideProtocol(PortGenerator.AllocatedPorts.GENERAL_SERVICE.getPort(), null);
	}
	
	public static boolean notify(String msg, int targetPort) {
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
