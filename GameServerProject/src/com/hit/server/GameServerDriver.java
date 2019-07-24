package com.hit.server;
import com.hit.util.CLI;
import javaNK.util.communication.NetworkInformation;
import javaNK.util.debugging.Logger;
import server_information.ServerData;

public class GameServerDriver
{
	public static void main(String[] args) {
		NetworkInformation serverNetwork = null;
		
		try {
			//initiate utility services
			Logger.configName("Server");
			Logger.configErrorStream(System.err);
			serverNetwork = new NetworkInformation(ServerData.PORT, ServerData.IP_ADDRESS);
			System.err.println("Network Information: " + serverNetwork);
			
			CLI cli = new CLI(System.in, System.out);
			Server server = new Server(serverNetwork);
			cli.addPropertyChangeListener(server);
			new Thread(cli).start();
		}
		catch (Exception ex) { Logger.error(ex); }
	}
}