package com.hit.server;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.services.GameServerController;

import javaNK.util.debugging.Logger;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;
import javaNK.util.networking.RespondEngine;
import javaNK.util.networking.RespondCase;

public class Server extends RespondEngine implements PropertyChangeListener
{
	private GameServerController controller;
	
	public Server(int port) throws IOException {
		super(port);
		this.controller = new GameServerController(this);
		new Thread(this).start();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("running"))
			pause(!(boolean) e.getNewValue());
	}

	/**
	 * Notify all clients about the beginning of the game.
	 * 
	 * @param openGame - The open game that's starting 
	 * @throws IOException when at least one of the clients' protocols is unavailable.
	 */
	public void notifyStart(OpenGame openGame) throws IOException {
		boolean gaveTurn = false;
		boolean firstTurnGiver;
		
		for (Protocol clientProtocol : openGame.getClients()) {
			//give first turn to the first player in list
			if (!gaveTurn) {
				firstTurnGiver = true;
				gaveTurn = true;
			}
			else firstTurnGiver = false;
			
			JSON message = new JSON("start_game");
			message.put("game", openGame.getGame().name());
			message.put("turn", firstTurnGiver);
			
			protocol.setTargetPort(clientProtocol.getTargetPort());
			protocol.send(message);
		}
	}
	
	/**
	 * Notify one client with a custom message.
	 * @param prot - The protocol of the client
	 * @param msg - The JSON message
	 * @throws IOException when the client's protocol is unavailable
	 */
	public void notify(Protocol prot, JSON msg) throws IOException {
		protocol.setTargetPort(prot.getTargetPort());
		protocol.send(msg);
	}
	
	protected void initCases() {
		//new client service
		addCase(new RespondCase() {
			@Override
			public String getType() { return "new_client"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				//the game the client is referring to
				Game game = Game.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				
				//check if client already playing
				if (!controller.isAllowed(targetPort, game)) return;
				
				//create a new protocol that listens to the new client
				Protocol newProt = new Protocol();
				newProt.setTargetPort(targetPort);
				
				OpenGame openGame = controller.addClient(newProt, game, msg);
				
				//notify client about his new target port
				protocol.setTargetPort(targetPort);
				JSON message = new JSON("new_client");
				message.put("port", newProt.getPort());
				protocol.send(message);
				
				try { Thread.sleep(100); }
				catch (InterruptedException e) {}
				
				//inform all about the beginning of the game
				if (openGame.canRun()) notifyStart(openGame);
				
				Logger.print("Added player at port " + newProt.getPort() + ".");
			}
		});
		
		//leaving client service
		addCase(new RespondCase() {
			@Override
			public String getType() { return "leaving_client"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				//the game the client is referring to
				Game game = Game.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				
				controller.closeGame(targetPort, game);
				Logger.print("Player from port " + targetPort + " has left the " + game.name() + " game.");
			}
		});
		
		//restart game for client service
		addCase(new RespondCase() {
			@Override
			public String getType() { return "happy_client"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				//the game the client is referring to
				Game game = Game.valueOf(msg.getString("game"));
				
				//clients send their port with the message
				int targetPort = msg.getInt("port");
				
				controller.restartGame(targetPort, game);
			}
		});
	}
}