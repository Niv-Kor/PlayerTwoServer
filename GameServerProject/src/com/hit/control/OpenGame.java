package com.hit.control;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.hit.exception.UnknownIdException;
import com.hit.server.HandleRequest;
import com.hit.services.GameServerController;
import game_algo.IGameAlgo;
import game_algo.IGameAlgo.GameState;
import javaNK.util.math.RNG;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

public class OpenGame
{
	private static long IDGenerator = 0;
	private Game game;
	private BoardGameHandler handler;
	private GameServerController controller;
	private volatile Set<ClientIdentity> clients;
	private volatile Set<Integer> reservations;
	private volatile boolean running;
	private int subsAmount;
	private long ID;
	
	/**
	 * @param game - The game to open
	 * @param reservations - A set of reserved clients for this game
	 * @throws UnknownIdException when the game's not recognized.
	 */
	public OpenGame(GameServerController controller, Game game, Set<Integer> reservations) throws UnknownIdException {
		if (game == null) throw new UnknownIdException();
		
		/*
		 * Protocol - The client's private protocol
		 * Boolean - True if the client is playing single player mode
		 * HandleRequest - The client's private handler during the game's session
		 */
		this.ID = IDGenerator++;
		this.game = game;
		this.clients = new HashSet<ClientIdentity>();
		this.reservations = reservations;
		this.handler = new BoardGameHandler(game, chooseRandomGameAlgo());
		this.controller = controller;
		this.subsAmount = 0;
	}
	
	/**
	 * @return true if the game has enough subscribers to run.
	 */
	public boolean canRun() {
		return subsAmount == game.getGoalAmount();
	}
	
	/**
	 * Add a client to the game.
	 * 
	 * @param identity - The client's identity
	 * @return true if after adding the client, the game can run.
	 */
	public boolean subscribe(ClientIdentity identity) {
		//add a client if the game is not running yet
		if (!canRun()) {
			try {
				//create client entry
				HandleRequest handler = new HandleRequest(this, identity.getProtocol(), clients.size());
				identity.setHandler(handler);
				int amount = identity.isSociopath() ? 2 : 1;
				
				//add as a subscriber
				clients.add(identity);
				subsAmount += amount;
			}
			catch(IOException e) { return false; }
		}
		
		return canRun();
	}
	
	/**
	 * Initiate crucial components and start the game.
	 */
	public void start() { if (canRun()) pauseGame(false); }
	
	/**
	 * Remove a client from the game.
	 * 
	 * @param port - The port of the client to remove
	 */
	public void removeClient(int port) {
		boolean couldRun = canRun(); //see if the game could run before the removal
		Protocol leavingClientProt = null;
		boolean removed = false;
		
		ClientIdentity id = identify(port);
		
		if (id != null) {
			leavingClientProt = id.getProtocol();
			id.getHandler().kill();
			int amount = id.isSociopath() ? 2 : 1;
			subsAmount -= amount;
			clients.remove(id);
			removed = true;
		}
		
		//terminate game - send all a message
		if (couldRun && removed && running)
			announceDisconnection(leavingClientProt);
	}
	
	/**
	 * Find the correct ClientIdentity object, compatible with the client's port number.
	 * 
	 * @param port - Port number of the client
	 * @return the ClientIdentity object of the client.
	 */
	private ClientIdentity identify(int port) {
		for (ClientIdentity id : clients)
			if (id.getProtocol().getRemotePort() == port) return id;
		
		return null;
	}
	
	/**
	 * @return the boardGame's handler.
	 */
	public BoardGameHandler getBoardHandler() { return handler; }
	
	/**
	 * @param prot - The client's protocol
	 * @return the client's private request handler.
	 */
	public HandleRequest getRequestHandler(Protocol prot) {
		return identify(prot.getRemotePort()).getHandler(); 
	}
	
	/**
	 * @return all of the game's client protocols.
	 */
	public Set<ClientIdentity> getClients() { return clients; }
	
	/**
	 * Get a set of reserved clients, that should come and join the game.
	 * 
	 * @return a set of the reserved clients.
	 */
	public Set<Integer> getReservations() { return reservations; }
	
	/**
	 * @param port - The port of the player to check
	 * @return true if the player that own's that port is subscribed to this open game.
	 */
	public boolean hasClient(int port) { return identify(port) != null; }
	
	/**
	 * @return the game that's opened.
	 */
	public Game getGame() { return game; }
	
	/**
	 * @return the amount of clients that subscribed to the game.
	 */
	public int getSubscribersAmount() { return subsAmount; }
	
	/**
	 * Notify all participants of this open game with a message.
	 * 
	 * @param msg - The message to send all participants
	 */
	public void notifyAll(JSON msg) {
		notifyOthers(null, msg);
	}
	
	/**
	 * Notify all (remaining) clients, about the disconnection of one client from the game.
	 * 
	 * @param protocol - The protocol of the disconnected client
	 */
	public void announceDisconnection(Protocol protocol) {
		if (!running) return;
		
		JSON deathNote = new JSON("end_game");
		deathNote.put("game", game.name());
		deathNote.put("state", GameState.PARTNER_DISCONNECTED.name());
		notifyOthers(protocol, deathNote);
		
		pauseGame(true);
	}
	
	/**
	 * Notify all participants of this open game, excluding one, with a message.
	 * 
	 * @param exclude - The one participant to exclude
	 * @param msg - The message to send all other participants
	 */
	public void notifyOthers(Protocol exclude, JSON msg) {
		Set<Protocol> alternativeSet = getAllProtocols();
		alternativeSet.remove(exclude);
		controller.notifySet(alternativeSet, msg);
	}
	
	public Set<Protocol> getAllProtocols() {
		Set<Protocol> set = new HashSet<Protocol>();
		
		for (ClientIdentity id : clients)
			set.add(id.getProtocol());
		
		return set;
	}
	
	/**
	 * Renew the game.
	 */
	public void reissue() {
		handler = new BoardGameHandler(game, chooseRandomGameAlgo());
		
		for (ClientIdentity id : clients)
			id.getHandler().reissueBoardHandler(handler);
	}
	
	/**
	 * @return either smart game model or random game model, with 50% chance for each.
	 */
	private IGameAlgo chooseRandomGameAlgo() {
		return RNG.unstableCondition(50) ? game.getSmartModel() : game.getRandomModel();
	}
	
	/**
	 * Formally tell the open game that the game is now paused.
	 * 
	 * @param flag - True to pause
	 */
	public void pauseGame(boolean flag) { running = !flag; }
	
	@Override
	public String toString() {
		String str = "[OPEN GAME #" + ID + ", "
				   + " Game name: " + game.name() + ", "
				   + " Clients:\n";
		
		for (ClientIdentity id : clients)
			str = str.concat(id + "\n");
		
		return str.substring(0, str.length() - 1) + "]";
	}
}