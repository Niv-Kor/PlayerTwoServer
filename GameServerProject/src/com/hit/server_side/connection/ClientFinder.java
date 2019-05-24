package com.hit.server_side.connection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hit.server_side.game_controlling.ServerSideController;
import com.hit.server_side.game_controlling.ServerSideGame;

public class ClientFinder extends Thread
{
	private ServerSideProtocol finder;
	private List<ServerSideProtocol> serverSideProtocols;
	
	public ClientFinder() {
		this.serverSideProtocols = new ArrayList<ServerSideProtocol>();
		
		try { this.finder = new ServerSideProtocol(PortGenerator.AllocatedPorts.CLIENT_FINDER.getPort(), null); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@Override
	public void run() {
		while(!interrupted()) {
			try {
				String[] msg = finder.receive();
				
				//the game the client is referring to
				ServerSideGame game = ServerSideGame.valueOf(msg[1]);
				
				//clients send their port with the message
				int targetPort = Integer.parseInt(msg[2]);
				
				//sleep before answering
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				switch(msg[0]) {
					//a client wants to join the server
					case "hello": {
						if (isDuplicate(targetPort) || game.ready()) break;
						
						ServerSideProtocol newProt = new ServerSideProtocol();
						serverSideProtocols.add(newProt);
						newProt.setTargetPort(targetPort);
						finder.setTargetPort(targetPort);
						finder.send("hello:" + newProt.getPort() + ":");
						
						ServerSideController.addClient(game, newProt);
						game.addClient();
						
						ServerLogger.print("added player at port " + newProt.getPort() + ".");
						break;
					}
					//a client wants to leave the server
					case "bye": {
						if (!isDuplicate(targetPort) || game.getClientsAmount() == 0) break;
						
						ServerSideProtocol freeConnection = ServerSideController.removeClient(targetPort);
						if (freeConnection != null) {
							ServerLogger.print("client with port " + freeConnection.getPort() + " removed.");
							game.removeClient();
						}
						break;
					}
					default: throwUnrecognizedMessage(msg[0], "not available");
				}
				
				//start game if there's enough clients
				if (game.ready() && !game.isRunning()) {
					try { Thread.sleep(1000); }
					catch (InterruptedException e) {}
					
					ServerSideController.startGame(game);
					game.run(true);
				}
			}
			catch(IOException e) {
				ServerLogger.print("encountered problem.");
				e.printStackTrace();
			}
		}
	}
	
	private void throwUnrecognizedMessage(String msg, String reason) {
		ServerLogger.print("unrecognized command '" + msg + "', " + reason + ".");
	}
	
	private boolean isDuplicate(int port) {
		return ServerSideController.getPortsSet().contains(port);
	}
}