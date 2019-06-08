package com.hit.services;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.exception.UnknownIdException;
import com.hit.server.Server;

import game_algo.IGameAlgo.GameState;
import javaNK.util.debugging.Logger;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

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
			Set<Integer> reservations = new HashSet<Integer>();
			
			//add the reserved ports to the reservations set (if they exist)
			String[] reservationsArr = msg.getStringArray("reservations");
			for (String reservedPort : reservationsArr)
				reservations.add(Integer.parseInt(reservedPort));
			
			return gameService.startGame(this, clientProt, game, reservations, reserved, singlePlayer);
		}
		catch(UnknownIdException e) {
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
	 * @param clientPort - The port of the client that's closing the game
	 * @param game - The game to close
	 * @param expected - False if the end of the game is unexpected (players will be notified)
	 * @return the OpenGame object of the game that got closed (or just removed a subscriber).
	 */
	public OpenGame closeGame(int clientPort, Game game, boolean expected) {
		OpenGame closedGame = gameService.playedGame(clientPort, game);
		
		//notify all players about unexpected end of the game
		if (!expected && closedGame != null) {
			JSON unexpecetedEnd = new JSON("end_game");
			unexpecetedEnd.put("game", game.name());
			unexpecetedEnd.put("state", GameState.IN_PROGRESS.name());
			closedGame.notifyAll(unexpecetedEnd);
		}
		
		return gameService.closeGame(clientPort, game);
	}
	
	/**
	 * Restart a game for a client
	 * 
	 * @param clientPort - The client that asked for the game to be restarted
	 * @param game - The game to restart
	 * @return the game that has been restarted, or null if the game doesn't exist from the first place.
	 */
	public OpenGame restartGame(int clientPort, Game game) {
		return gameService.reissue(clientPort, game);
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
				Logger.error(msg, "Something went wrong with port " + prot.getRemotePort() + ".");
			}
		}
	}
	
	/**
	 * Verify that a client is not already playing a specific game.
	 * 
	 * @param clientPort - The port of the client to check
	 * @param game - The game to check that the client isn't playing
	 * @return true if the client is not playing that game.
	 */
	public boolean isAllowed(int clientPort, Game game) {
		return !gameService.isPlaying(clientPort, game);
	}
}