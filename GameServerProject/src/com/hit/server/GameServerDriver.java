package com.hit.server;
import com.hit.util.CLI;
import javaNK.util.debugging.Logger;
import javaNK.util.networking.PortGenerator;

public class GameServerDriver
{
	public static void main(String[] args) {
		//init utility services
		Logger.config("Server");
		PortGenerator.allocate("server_port", 5080);
		
		CLI cli = new CLI(System.in, System.out);
		Server server = new Server(PortGenerator.getAllocated("server_port"));
		cli.addPropertyChangeListener(server);
		new Thread(cli).start();
	}
}