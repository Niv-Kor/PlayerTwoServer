package com.hit.services;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import com.hit.control.ClientIdentity;
import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.exception.UnknownIdException;
import com.hit.server.Server;
import game_algo.IGameAlgo.GameState;
import javaNK.util.communication.JSON;
import javaNK.util.communication.NetworkInformation;
import javaNK.util.communication.Protocol;
import javaNK.util.debugging.Logger;

public class GameServerController
{
	private Server server;
	private GameService gameService;
	
	/**
	 * @param server - The main Server object
	 */
	public GameServerController(Server server) {
		this.gameService = new GameService();
		this.server = server;
	}
	
	/**
	 * Find a pending game for a client, or open a new game for him.
	 * 
	 * @param clientProt - The port of the client to add
	 * @param game - The game the client wants to play
	 * @param msg - The message the client sent to the server
	 * @return the game that has been assigned to the client.
	 */
	public OpenGame addClient(Protocol clientProt, Game game, JSON msg) {
		try {
			boolean reserved = msg.getBoolean("reserved");
			boolean singlePlayer = msg.getBoolean("single_player");
			Set<NetworkInformation> reservations = new HashSet<NetworkInformation>();
			
			//add the reserved ports to the reservations set (if they exist)
			JSON[] reservationsArr = msg.getJSONArray("reservations");
			for (JSON reservedClient : reservationsArr)
				reservations.add(new NetworkInformation(reservedClient));
			
			String name = msg.getString("name");
			String avatarID = msg.getString("avatar");
			ClientIdentity id = new ClientIdentity(name, avatarID, clientProt, null, singlePlayer);
			
			return gameService.startGame(this, id, game, reservations, reserved);
		}
		catch(IOException | UnknownIdException e) {
			Logger.error(msg, "The game '"
							+ Game.valueOf(msg.getString("game")).name() + "' "
							+ "is not recognized by the server.");
		}
		
		return null;
	}
	
	/**
	 * Remove a player from an open game and also potentially close the game.
	 * The game will not be closed if it's still pending for other subscribers,
	 * and will be closed for all the players if it had already started.
	 * If a game has been closed unexpectedly, all players will be notified.
	 * 
	 * @param clientInfo - The network information of the client that's closing the game
	 * @param game - The game to close
	 * @param expected - False if the end of the game is unexpected (players will be notified)
	 * @return the OpenGame object of the game that got closed (or just removed a subscriber).
	 */
	public OpenGame closeGame(NetworkInformation clientInfo, Game game, boolean expected) {
		OpenGame closedGame = gameService.getPlayedGame(clientInfo, game);
		
		//notify all players about unexpected end of the game
		if (!expected && closedGame != null) {
			JSON unexpecetedEnd = new JSON("end_game");
			unexpecetedEnd.put("game", game.name());
			unexpecetedEnd.put("state", GameState.IN_PROGRESS.name());
			closedGame.notifyAll(unexpecetedEnd);
		}
		
		return gameService.closeGame(clientInfo, game);
	}
	
	/**
	 * Restart a game for a client
	 * 
	 * @param clientInfo - The network information of the client that asked for the game to be restarted
	 * @param game - The game to restart
	 * @return the game that has been restarted, or null if the game doesn't exist from the first place.
	 */
	public OpenGame restartGame(NetworkInformation clientInfo, Game game) {
		return gameService.reissue(clientInfo, game);
	}
	
	/**
	 * Notify a set of clients with a JSON message.
	 * 
	 * @param protocols - A set of the clients' protocols
	 * @param msg - The message to notify the clients with
	 */
	public void notifySet(Set<Protocol> protocols, JSON msg) {
		for (Protocol prot : protocols) {
			try { server.notify(prot, msg); }
			catch(IOException e) {
				Logger.error(msg, "Could not contact the client " + prot.getRemoteNetworkInformation() + ".");
			}
		}
	}
	
	/**
	 * Verify that a client is not already playing a specific game.
	 * 
	 * @param clientInfo - The network information of the client to check
	 * @param game - The game to check that the client isn't playing
	 * @return true if the client is not playing that game.
	 */
	public boolean isAllowed(NetworkInformation clientInfo, Game game) {
		return !gameService.isPlaying(clientInfo, game);
	}
}