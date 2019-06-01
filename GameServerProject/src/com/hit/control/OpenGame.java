package com.hit.control;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hit.exception.UnknownIdException;
import com.hit.server.HandleRequest;
import com.hit.services.GameServerController;

import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

public class OpenGame
{
	private Game game;
	private BoardGameHandler handler;
	private GameServerController controller;
	private Map<Protocol, HandleRequest> clients;
	private Set<Integer> clientPorts;
	
	/**
	 * @param game - The game to open
	 * @throws UnknownIdException when the game's not recognized.
	 */
	public OpenGame(GameServerController controller, Game game) throws UnknownIdException {
		if (game == null) throw new UnknownIdException();
		
		this.game = game;
		this.clients = new HashMap<Protocol, HandleRequest>();
		this.clientPorts = new HashSet<Integer>();
		this.handler = new BoardGameHandler(game.getSmartModel());
		this.controller = controller;
	}
	
	/**
	 * @return true if the game has enough clients to run.
	 */
	public boolean canRun() {
		return clients.size() == game.getGoalAmount();
	}
	
	/**
	 * Add a client to the game.
	 * @return true if after adding the client, the game can run.
	 */
	public boolean subscribe(Protocol prot) {
		if (!canRun()) {
			try {
				clients.put(prot, new HandleRequest(this, prot, clients.size()));
				clientPorts.add(prot.getTargetPort());
			}
			catch(IOException e) { return false; }
		}
		return canRun();
	}
	
	/**
	 * Remove a client from the game.
	 * @param port - The port of the client to remove
	 */
	public void removeClient(int port) {
		for (Protocol client : clients.keySet()) {
			if (client.getPort() == port) clients.remove(client);
			clientPorts.remove(port);
		}
	}
	
	/**
	 * @return the boardGame's handler.
	 */
	public BoardGameHandler getBoardHandler() { return handler; }
	
	/**
	 * @return all of the game's client protocols.
	 */
	public Set<Protocol> getClients() { return clients.keySet(); }
	
	public boolean hasPlayer(int port) { return clientPorts.contains(port); }
	
	/**
	 * @return the game that's opened.
	 */
	public Game getGame() { return game; }
	
	
	public void notifyAll(JSON msg) {
		notifyOthers(null, msg);
	}
	
	public void notifyOthers(Protocol exception, JSON msg) {
		Set<Protocol> alternativeSet = new HashSet<Protocol>(getClients());
		alternativeSet.remove(exception);
		controller.notifySet(alternativeSet, msg);
	}
	
	public void reissue() {
		handler = new BoardGameHandler(game.getSmartModel());
		
		for (Protocol prot : getClients()) {
			clients.get(prot).reissueBoardHandler(handler);
		}
	}
}