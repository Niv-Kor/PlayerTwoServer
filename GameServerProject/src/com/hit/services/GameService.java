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
		
		//init each game's set of open games
		for (Game game : Game.values())
			openGames.put(game, new HashSet<OpenGame>());
		
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
		//client is trying to open two instances of the same game
		OpenGame playedGame = playedGame(clientProt.getTargetPort(), game);
		if (playedGame != null) return playedGame;
		
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
		openGames.get(game).add(newGame);
		
		//add the new game to the correct pending queue
		if (game.getGoalAmount() > 1) pendingGames.get(game).add(newGame);
		
		return newGame;
	}
	
	public void closeGame(int clientPort, Game game) {
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
		openGames.get(game).remove(playedGame(clientPort, game));
	}
	
	public void reissue(int clientPort, Game game) {
		OpenGame openGame = playedGame(clientPort, game);
		if (openGame != null) openGame.reissue();
	}
	
	public boolean isPlaying(int clientPort, Game game) {
		return playedGame(clientPort, game) != null;
	}
	
	public OpenGame playedGame(int clientPort, Game game) {
		for (OpenGame openGame : openGames.get(game))
			if (openGame.hasPlayer(clientPort)) return openGame;
		
		return null;
	}
}