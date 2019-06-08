package com.hit.server;
import com.hit.util.CLI;

import javaNK.util.debugging.Logger;
import javaNK.util.networking.PortGenerator;

public class GameServerDriver
{
	public static void main(String[] args) {
		try {
			//initiateS utility services
			Logger.configName("Server");
			Logger.configErrorStream(System.err);
			PortGenerator.allocate("server_port", 5081);
			
			CLI cli = new CLI(System.in, System.out);
			Server server = new Server(PortGenerator.getAllocated("server_port"));
			cli.addPropertyChangeListener(server);
			new Thread(cli).start();
		}
		catch(Exception e) {
			Logger.error(e);
		}
	}
}