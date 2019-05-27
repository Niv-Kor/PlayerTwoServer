package com.hit.services;
import java.io.IOException;
import java.util.Set;

import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.exception.UnknownIdException;
import com.hit.server.Server;

import javaNK.util.debugging.Logger;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

public class GameServerController
{
	private Server server;
	private GameService gameService;
	
	public GameServerController(Server server) {
		this.gameService = new GameService();
		this.server = server;
	}
	
	public OpenGame addClient(Protocol clientProt, Game game, JSON msg) {
		try { return gameService.startGame(this, clientProt, game); }
		catch(UnknownIdException e) {
			Logger.error(msg, "The game '"
							+ Game.valueOf(msg.getString("game")).name() + "' "
							+ "is not recognized by the server.");
		}
		
		return null;
	}
	
	public void closeGame(int clientPort, Game game) {
		gameService.closeGame(clientPort, game);
	}
	
	public void restartGame(int clientPort, Game game) {
		gameService.reissue(clientPort, game);
	}
	
	public void notifySet(Set<Protocol> protocols, JSON msg) {
		for (Protocol prot : protocols) {
			try { server.notify(prot, msg); }
			catch(IOException e) {
				Logger.error(msg, "Something went wrong with port " + prot.getTargetPort() + ".");
			}
		}
	}
	
	public boolean isAllowed(int clientPort, Game game) {
		return !gameService.isPlaying(clientPort, game);
	}
}