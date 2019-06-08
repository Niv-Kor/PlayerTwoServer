package com.hit.services;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.exception.UnknownIdException;
import javaNK.util.networking.Protocol;

public class GameService
{
	private Map<Game, Set<OpenGame>> openGames;
	private Map<Game, Queue<OpenGame>> pendingGames;
	
	public GameService() {
		/*
		 * This data structure holds all the games that are currently being played.
		 */
		this.openGames = new HashMap<Game, Set<OpenGame>>();
		
		//initiate each game's set of open games
		for (Game game : Game.values())
			openGames.put(game, new HashSet<OpenGame>());
		
		/*
		 * This data structure holds all the games that had been created
		 * but not yet populated with enough clients to start.
		 * Each game has a queue of open games, waiting to start.
		 */
		this.pendingGames = new HashMap<Game, Queue<OpenGame>>();
		
		//initiate each game's queue of pending open games
		for (Game game : Game.values())
			pendingGames.put(game, new LinkedList<OpenGame>());
	}
	
	/**
	 * Start a new game.
	 * If a game is not ready to start, it will be waiting is a pending queue for more clients to connect.
	 * If a pending game already exists, inform it about with client's identity and subscribe him.  
	 * 
	 * @param controller - The main GameServerController object
	 * @param clientProt - The protocol of the client
	 * @param game - The game the clients wants to play
	 * @param reservations - The clients this incoming client wants to reserve spots for
	 * @param reserved - True is the incoming client has a reserved spot in a pending game
	 * @param singlePlayer - True if the incoming client wants to play against the computer
	 * @return the OpenGame object the client was subscribed to.
	 * @throws UnknownIdException when the game is not recognized by the server.
	 */
	public OpenGame startGame(GameServerController controller, Protocol clientProt,
							  Game game, Set<Integer> reservations, boolean reserved,
							  boolean singlePlayer) throws UnknownIdException {
		
		//client is trying to open two instances of the same game
		OpenGame playedGame = playedGame(clientProt.getRemotePort(), game);
		if (playedGame != null) return playedGame;
		
		//find a pending game for the client
		if (!singlePlayer) {
			for (OpenGame pendingGame : pendingGames.get(game)) {
				//there's a game that's waiting for this client
				boolean waitedForClient = reserved && pendingGame.getReservations().contains(clientProt.getRemotePort());
				
				//no one is waiting for this client - join a free for all game
				boolean freeForAll = !reserved && pendingGame.getReservations().isEmpty();
				
				if (waitedForClient || freeForAll) {
					pendingGame.subscribe(clientProt, 1);
					
					//if done waiting for clients remove from pending list
					if (pendingGame.canRun()) pendingGames.get(game).remove(pendingGame);
					return pendingGame;
				}
			}
		}
		
		//open a new game for the client
		OpenGame newGame = new OpenGame(controller, game, reservations);
		int amountOfPlayers = singlePlayer ? 2 : 1;
		newGame.subscribe(clientProt, amountOfPlayers);
		openGames.get(game).add(newGame);
		
		//add the new game to the correct pending queue
		if (game.getGoalAmount() > 1) pendingGames.get(game).add(newGame);
		return newGame;
	}
	
	/**
	 * Remove a player from an open game and also potentially close the game.
	 * The game will not be closed if it's still pending for other subscribers,
	 * and will be closed for all the players if it had already started.
	 * 
	 * @param clientPort - The port of the client that's closing the game
	 * @param game - The game to close
	 * @return the OpenGame object of the game that got closed (or just removed a subscriber).
	 */
	public OpenGame closeGame(int clientPort, Game game) {
		OpenGame playedGame = playedGame(clientPort, game);
		Queue<OpenGame> pendingRemoval = new LinkedList<OpenGame>();
		
		//remove the client from the game he waited for (if needed)
		for (OpenGame pendingGame : pendingGames.get(game)) {
			pendingGame.removeClient(clientPort);
			
			//delete open game if no clients are subscribed
			if (pendingGame.getClients().isEmpty()) pendingRemoval.add(pendingGame);
		}
		
		//remove them while iterating over a different data structure to avoid errors
		while (!pendingRemoval.isEmpty()) {
			OpenGame removedGame = pendingRemoval.poll();
			pendingGames.get(game).remove(removedGame);
		}
		
		//close the game that the client was playing
		if (playedGame != null) {
			playedGame.removeClient(clientPort);
			
			//remove game if it contains no clients
			if (playedGame.getClients().isEmpty())
				openGames.get(game).remove(playedGame);
		}
		
		return playedGame;
	}
	
	/**
	 * Restart a game for a client
	 * 
	 * @param clientPort - The client that asked for the game to be restarted
	 * @param game - The game to restart
	 * @return the game that has been restarted, or null if the game doesn't exist from the first place.
	 */
	public OpenGame reissue(int clientPort, Game game) {
		OpenGame openGame = playedGame(clientPort, game);
		if (openGame != null) openGame.reissue();
		return openGame;
	}
	
	/**
	 * Check if a client is playing a specific game right now.
	 * 
	 * @param clientPort - The client's port that needs to be checked
	 * @param game - The game that the client is playing
	 * @return true if the client is playing that game right now.
	 */
	public boolean isPlaying(int clientPort, Game game) {
		return playedGame(clientPort, game) != null;
	}
	
	/**
	 * Get an open game that's played by a specific client.
	 * 
	 * @param clientPort - The port of the client that's playing the game
	 * @param game - The game that the client is playing
	 * @return the open game that the client is now playing 
	 */
	public OpenGame playedGame(int clientPort, Game game) {
		for (OpenGame openGame : openGames.get(game))
			if (openGame.hasClient(clientPort)) return openGame;
		
		return null;
	}
}