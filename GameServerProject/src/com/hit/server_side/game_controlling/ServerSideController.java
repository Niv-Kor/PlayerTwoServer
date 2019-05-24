package com.hit.server_side.game_controlling;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hit.server_side.connection.ClientFinder;
import com.hit.server_side.connection.GeneralService;
import com.hit.server_side.connection.ServerLogger;
import com.hit.server_side.connection.ServerSideProtocol;
import com.hit.server_side.connection.ServingThread;

import game_algo.IGameAlgo.GameState;

public class ServerSideController
{
	private static ClientFinder finder;
	private static List<ServingThread> servingThreads;
	
	public static void init() {
		servingThreads = new ArrayList<ServingThread>();
		finder = new ClientFinder();
	}
	
	public static void find() {
		finder.start();
	}
	
	public static void startGame(ServerSideGame game) {
		boolean gaveTurn = false;
		String firstTurnGiver;
		
		for (ServingThread st : servingThreads) {
			if (st.getGame() == game) {
				//give first turn to the first player in list
				if (!gaveTurn) {
					firstTurnGiver = "getturn";
					gaveTurn = true;
				}
				else firstTurnGiver = "noturn";
				
				GeneralService.notify("start:" + game.name() + ":" + firstTurnGiver + ":", st.getProtocol().getTargetPort());
			}
		}
	}
	
	public static void addClient(ServerSideGame game, ServerSideProtocol protocol) {
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

	public static ServerSideProtocol removeClient(int target) {
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

	public static void informOthers(ServerSideGame game, ServerSideProtocol exception, String msg) throws IOException {
		ServerSideProtocol tempProt;
		
		for (ServingThread st : servingThreads) {
			if (st.getGame() == game) {
				tempProt = st.getProtocol();
				if (tempProt != exception) {
					tempProt.send(msg);
					ServerLogger.print(tempProt.getPort() + " received " + msg);
				}
			}
		}
	}
	
	public static void informAll(ServerSideGame game, String msg) throws IOException {
		informOthers(game, null, msg);
	}
	
	public static void informEnd(ServerSideGame game) {
		GameState individualState;
		int targetPort;
		String msg;
		
		for (ServingThread st : servingThreads) {
			if (st.getGame() == game) {
				individualState = game.playerGameState[st.getPlayerIndex()];
				msg = "end:" + game.name() + ":" + individualState + ":";
				targetPort = st.getProtocol().getTargetPort();
				
				System.out.println("player with index " + st.getPlayerIndex());
				System.out.println("got " + msg);
				
				GeneralService.notify(msg, targetPort);
			}
		}
		
		System.out.println(Arrays.toString(game.playerGameState));
	}
}