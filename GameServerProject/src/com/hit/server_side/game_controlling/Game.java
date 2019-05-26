package com.hit.server_side.game_controlling;
import java.awt.Dimension;

import game_algo.GameBoard;
import game_algo.IGameAlgo;
import games.CatchTheBunny;
import games.CatchTheBunnyRandom;
import games.CatchTheBunnySmart;
import games.TicTacToe;
import games.TicTacToeRandom;
import games.TicTacToeSmart;

public enum Game
{
	TIC_TAC_TOE(2, new Dimension(3, 3),
				TicTacToeSmart.class, TicTacToeRandom.class,
				TicTacToe.BoardSigns.PLAYER.getSign(),
				TicTacToe.BoardSigns.COMPUTER.getSign()),
	
	CATCH_THE_BUNNY(2, new Dimension(9, 9),
					CatchTheBunnySmart.class, CatchTheBunnyRandom.class,
					CatchTheBunny.BoardSigns.PLAYER.getSign(),
					CatchTheBunny.BoardSigns.COMPUTER.getSign());
	
	private int clients, clientsGoal;
	private boolean running;
	private char playerSign, compSign;
	private Dimension boardSize;
	private Class<? extends GameBoard> smartClass, randomClass;
	
	private Game(int goal, Dimension boardSize,
						   Class<? extends GameBoard> smrtCls,
						   Class<? extends GameBoard> rndCls,
						   char playerSign, char compSign) {
		
		this.clients = 0;
		this.clientsGoal = goal;
		this.boardSize = boardSize;
		this.smartClass = smrtCls;
		this.randomClass = rndCls;
		this.playerSign = playerSign;
		this.compSign = compSign;
	}
	
	public IGameAlgo getSmartModel() throws Exception {
		return smartClass.asSubclass(GameBoard.class).
			   getConstructor(int.class, int.class).
			   newInstance(boardSize.width, boardSize.height);
	}
	
	public IGameAlgo getRandomModel() throws Exception {
		return randomClass.asSubclass(GameBoard.class).
			   getConstructor(int.class, int.class).
			   newInstance(boardSize.width, boardSize.height);
	}
	
	public int getClientsAmount() { return clients; }
	public int getGoalAmount() { return clientsGoal; }
	public char getPlayerSign() { return playerSign; }
	public char getComputerSign() { return compSign; }
	public void addClient() { if (!ready()) ++clients; }
	public void removeClient() {
		if (clients > 0) --clients;
		run(false);
	}
	public void run(boolean flag) { running = flag; }
	public boolean isRunning() { return running; }
	public boolean ready() { return clients == clientsGoal; }
}