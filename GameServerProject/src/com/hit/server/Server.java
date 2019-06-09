package com.hit.server;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.hit.control.ClientIdentity;
import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.services.GameServerController;
import com.hit.util.CLI;

import javaNK.util.debugging.Logger;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;
import javaNK.util.networking.ResponseCase;
import javaNK.util.networking.ResponseEngine;
import javaNK.util.threads.ThreadUtility;

public class Server extends ResponseEngine implements PropertyChangeListener
{
	private GameServerController controller;
	private Set<Integer> clients;
	private int backlog;
	
	/**
	 * @param port - The port this server will listen to
	 * @throws IOException when the port is unavailable.
	 */
	public Server(int port) throws IOException {
		super(port, false);
		
		this.controller = new GameServerController(this);
		this.clients = new HashSet<Integer>();
		this.backlog = CLI.DEFAULT_BACKLOG;
		start();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		switch (e.getPropertyName()) {
			case "running": {
				boolean running = (boolean) e.getNewValue();
				pause(!running); //pause the thread
				
				//close all running games
				if (!running) {
					for (int clientPort : clients)
						for (Game game : Game.values())
							controller.closeGame(clientPort, game, false);
				}
				
				break;
			}
			case "backlog": backlog = (int) e.getNewValue(); break;
		}
	}

	/**
	 * Notify all clients about the beginning of the game.
	 * 
	 * @param openGame - The open game that's starting 
	 * @throws IOException when at least one of the clients' protocols is unavailable.
	 */
	protected void notifyStart(OpenGame openGame) throws IOException {
		boolean gaveTurn = false;
		boolean firstTurnGiver;
		Set<Protocol> allProtocols = openGame.getAllProtocols();
		
		for (Protocol clientProtocol : allProtocols) {
			//give first turn to the first player in list
			if (!gaveTurn) {
				firstTurnGiver = true;
				gaveTurn = true;
			}
			else firstTurnGiver = false;
			
			JSON message = new JSON("start_game");
			message.put("available", true);
			message.put("game", openGame.getGame().name());
			message.put("turn", firstTurnGiver);
			
			for (ClientIdentity id : openGame.getClients())
				if (id.getProtocol() != clientProtocol) //send only the OTHER clients' information
					message.putJSON("other_player", id.generateJSONObject("-"));
			
			protocol.setRemotePort(clientProtocol.getRemotePort());
			
			//start handle request thread
			openGame.getRequestHandler(clientProtocol).start();
			protocol.send(message);
		}
		
		openGame.start();
	}
	
	/**
	 * Notify one client with a custom message.
	 * @param prot - The protocol of the client
	 * @param msg - The JSON message
	 * @throws IOException when the client's protocol is unavailable
	 */
	public void notify(Protocol prot, JSON msg) throws IOException {
		protocol.setRemotePort(prot.getRemotePort());
		protocol.send(msg);
	}
	
	protected void initCases() {
		//new client service
		addCase(new ResponseCase() {
			@Override
			public String getType() { return "new_client"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				//the game the client is referring to
				Game game = Game.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				protocol.setRemotePort(targetPort);
				
				//reached limit of clients
				if (clients.size() >= backlog) {
					JSON message = new JSON("new_client");
					message.put("port", 0);
					message.put("available", false);
					protocol.send(message);
					return;
				}
				
				//check if client already playing
				if (!controller.isAllowed(targetPort, game)) return;
				
				//create a new protocol that listens to the new client
				Protocol newProt = new Protocol();
				newProt.setRemotePort(targetPort);
				
				OpenGame openGame = controller.addClient(newProt, game, msg);
				
				//notify client about his new target port
				JSON message = new JSON("new_client");
				message.put("port", newProt.getLocalPort());
				message.put("available", true);
				protocol.send(message);
				
				ThreadUtility.delay(100);
				Logger.print("A client from port " + newProt.getRemotePort()
						   + " has subscribed to\n" + openGame + ".");
				
				clients.add(targetPort);
				
				//inform all about the beginning of the game
				if (openGame.canRun()) notifyStart(openGame);
			}
		});
		
		//leaving client service
		addCase(new ResponseCase() {
			@Override
			public String getType() { return "leaving_client"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				//the game the client is referring to
				Game game = Game.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				
				OpenGame openGame = controller.closeGame(targetPort, game, true);
				Logger.print("A client from port " + targetPort + " has left\n" + openGame + ".");
				clients.remove(targetPort);
			}
		});
		
		//restart game for client service
		addCase(new ResponseCase() {
			@Override
			public String getType() { return "happy_client"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				//the game the client is referring to
				Game game = Game.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				
				OpenGame openGame = controller.restartGame(targetPort, game);
				
				//inform all about the beginning of the game
				if (openGame.canRun()) notifyStart(openGame);
			}
		});
	}
}