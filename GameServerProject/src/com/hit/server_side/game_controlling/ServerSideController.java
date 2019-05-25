package com.hit.server_side.game_controlling;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hit.server_side.connection.GeneralService;
import com.hit.server_side.connection.JSON;
import com.hit.server_side.connection.ServerLogger;
import com.hit.server_side.connection.Protocol;
import com.hit.server_side.connection.ServingThread;

import game_algo.IGameAlgo;

public class ServerSideController
{
	private static List<ServingThread> servingThreads;
	
	public static void init() {
		servingThreads = new ArrayList<ServingThread>();
	}
	
	public static void startGame(ServerSideGame game) {
		boolean gaveTurn = false;
		boolean firstTurnGiver;
		IGameAlgo gameAlgo;
		
		try { gameAlgo = game.getSmartModel(); }
		catch (Exception e) {
			ServerLogger.print("Could not start a game of " + game.name() + ".");
			e.printStackTrace();
			return;
		}
		
		for (ServingThread st : servingThreads) {
			if (st.getGame() == game) {
				st.setGameAlgorithm(gameAlgo);
				
				//give first turn to the first player in list
				if (!gaveTurn) {
					firstTurnGiver = true;
					gaveTurn = true;
				}
				else firstTurnGiver = false;
				
				JSON message = new JSON("start_game");
				message.put("game", game.name());
				message.put("turn", firstTurnGiver);
				
				GeneralService.notify(message, st.getProtocol().getTargetPort());
			}
		}
	}
	
	public static void addClient(ServerSideGame game, Protocol protocol) {
		/*
		 * The playerIndex variable suppose to solve the problem
		 * where gameBoard.updatePlayerMove() does not know which of
		 * the players that are currently connected to the server called it.
		 * 
		 * Thus, the method always puts the same character on the board,
		 * as both players in the server are using the same enum constant of BoardSigns.PLAYER.
		 * 
		 * This problem causes the game to miscalculate the GameState,
		 * and therefore we give each player in the server a unique (temporary) index,
		 * determined by the amount of clients the server held before adding him.
		 */
		int playerIndex = game.getClientsAmount();
		
		ServingThread st = new ServingThread(game, playerIndex, protocol);
		servingThreads.add(st);
		st.start();
	}

	public static Protocol removeClient(int target) {
		for (ServingThread st : servingThreads) {
			if (st.getProtocol().getTargetPort() == target) {
				st.interrupt();
				servingThreads.remove(st);
				return st.getProtocol();
			}
		}
		
		return null;
	}
	
	public static Set<Integer> getPortsSet() {
		Set<Integer> ports = new HashSet<Integer>();
		
		for (ServingThread st : servingThreads)
			ports.add(st.getProtocol().getTargetPort());
		
		return ports;
	}

	/**
	 * Send a collective message to the clients of each serving thread.
	 * 
	 * @param game - The related game
	 * @param exception - The one serving thread to skip when sending the messages
	 * @param msg - The message to send
	 * @throws IOException when one or more targeted protocols are unavailable.
	 */
	public static void informOthers(ServerSideGame game, Protocol exception, JSON msg) throws IOException {
		Protocol tempProt;
		
		for (ServingThread st : servingThreads) {
			if (st.getGame() == game) {
				tempProt = st.getProtocol();
				if (tempProt != exception) tempProt.send(msg);
			}
		}
	}
	
	public static void informAll(ServerSideGame game, JSON msg) throws IOException {
		informOthers(game, null, msg);
	}
}