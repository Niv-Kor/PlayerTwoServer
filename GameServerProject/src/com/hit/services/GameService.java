package com.hit.services;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.exception.UnknownIdException;

import javaNK.util.networking.Protocol;

public class GameService
{
	private Map<Entry<Integer, Game>, OpenGame> openGames;
	private Map<Game, Queue<OpenGame>> pendingGames;
	
	public GameService() {
		/*
		 * This data structure holds three parameters:
		 * 1. The client's port (to identify him).
		 * 2. The game the client is playing (because he's able to play more than one).
		 * 3. The OpenGame instance the player is using in his game.
		 */
		this.openGames = new HashMap<Entry<Integer, Game>, OpenGame>();
		
		/*
		 * This data structure holds all the games that had been created
		 * but not yet populated with enough clients to start.
		 * Each game has a queue of open games, wainting to start.
		 */
		this.pendingGames = new HashMap<Game, Queue<OpenGame>>();
		
		//init each game's queue of pending open games
		for (Game game : Game.values())
			pendingGames.put(game, new LinkedList<OpenGame>());
	}
	
	/**
	 * Start a new game.
	 * If a game is not ready to start, it will be waiting is a pending queue for more clients to connect.
	 * If a pending game already exists, inform it about with client's identity and subscribe him.  
	 * 
	 * @param clientProt - The protocol of the client
	 * @param game - The game the clients wants to play
	 * @return the OpenGame object the client was subscribed to.
	 * @throws UnknownIdException when the game is not recognized by the server.
	 */
	public OpenGame startGame(GameServerController controller, Protocol clientProt, Game game) throws UnknownIdException {
		SimpleEntry<Integer, Game> clientEntry = new SimpleEntry<Integer, Game>(clientProt.getTargetPort(), game);
		
		//client is trying to open two instances of the same game
		if (isPlaying(clientProt.getTargetPort(), game)) return openGames.get(clientEntry);
		
		//check if there's an open game waiting for more clients
		OpenGame pending = pendingGames.get(game).peek();
		if (pending != null) {
			//if done waiting for clients remove from pending list
			if (pending.subscribe(clientProt))
				return pendingGames.get(game).poll();
		}
		
		//open a new game for the client
		OpenGame newGame = new OpenGame(controller, game);
		newGame.subscribe(clientProt);
		openGames.put(clientEntry, newGame);
		
		//add the new game to the correct pending queue
		if (game.getGoalAmount() > 1) pendingGames.get(game).add(newGame);
		
		return newGame;
	}
	
	public void closeGame(int clientPort, Game game) {
		SimpleEntry<Integer, Game> clientEntry = new SimpleEntry<Integer, Game>(clientPort, game);
		Queue<OpenGame> pendingRemoval = new LinkedList<OpenGame>();
		
		//remove the client from the game he waited for (if needed)
		for (OpenGame openGame : pendingGames.get(game)) {
			openGame.removeClient(clientPort);
			
			//delete open game if no clients are subscribed
			if (openGame.getClients().size() == 0) pendingRemoval.add(openGame);
		}
		
		//remove them while iterating over a different data structure to avoid errors
		while (!pendingRemoval.isEmpty()) pendingGames.get(game).remove(pendingRemoval.poll());
		
		//close the game that the client was playing
		openGames.remove(clientEntry);
	}
	
	public void reissue(int clientPort, Game game) {
		if (!isPlaying(clientPort, game)) {
			System.out.println("is not playing!");
			return;
		}
		
		SimpleEntry<Integer, Game> clientEntry = new SimpleEntry<Integer, Game>(clientPort, game);
		openGames.get(clientEntry).reissue();
	}
	
	public boolean isPlaying(int clientPort, Game game) {
		SimpleEntry<Integer, Game> clientEntry = new SimpleEntry<Integer, Game>(clientPort, game);
		return openGames.containsKey(clientEntry);
	}
}