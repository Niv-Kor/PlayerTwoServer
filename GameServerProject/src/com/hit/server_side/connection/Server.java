package com.hit.server_side.connection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hit.server_side.game_controlling.ServerSideController;
import com.hit.server_side.game_controlling.ServerSideGame;

public class Server extends Thread implements PropertyChangeListener
{
	private Protocol finder;
	private List<Protocol> serverSideProtocols;
	
	public Server() {
		this.serverSideProtocols = new ArrayList<Protocol>();
		
		try { this.finder = new Protocol(PortGenerator.CLIENT_FINDER, null); }
		catch (IOException e) { e.printStackTrace(); }
	}
	
	@Override
	public void run() {
		while(!isInterrupted()) {
			try {
				JSON msg = finder.receive();
				
				//the game the client is referring to
				ServerSideGame game = ServerSideGame.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				
				//sleep before answering
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				switch(msg.getType()) {
					//a client wants to join the server
					case "new_client": {
						if (isDuplicate(targetPort) || game.ready()) break;
						
						Protocol newProt = new Protocol();
						serverSideProtocols.add(newProt);
						newProt.setTargetPort(targetPort);
						finder.setTargetPort(targetPort);
						
						//notify client with his new target port
						JSON message = new JSON("new_client");
						message.put("port", newProt.getPort());
						finder.send(message);
						
						ServerSideController.addClient(game, newProt);
						game.addClient();
						
						ServerLogger.print("added player at port " + newProt.getPort() + ".");
						break;
					}
					//a client wants to leave the server
					case "leaving_client": {
						if (!isDuplicate(targetPort) || game.getClientsAmount() == 0) break;
						
						Protocol freeConnection = ServerSideController.removeClient(targetPort);
						if (freeConnection != null) {
							ServerLogger.print("client with port " + freeConnection.getPort() + " removed.");
							game.removeClient();
						}
						break;
					}
					default: throwUnrecognizedMessage(msg.getType(), "not available");
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

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}