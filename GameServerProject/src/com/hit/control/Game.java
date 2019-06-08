package com.hit.control;
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
	TIC_TAC_TOE(2, new Dimension(3, 3), true,
				TicTacToeSmart.class, TicTacToeRandom.class,
				TicTacToe.BoardSigns.PLAYER.getSign(),
				TicTacToe.BoardSigns.COMPUTER.getSign()),
	
	CATCH_THE_BUNNY(2, new Dimension(9, 9), false,
					CatchTheBunnySmart.class, CatchTheBunnyRandom.class,
					CatchTheBunny.BoardSigns.PLAYER.getSign(),
					CatchTheBunny.BoardSigns.COMPUTER.getSign());
	
	private int clientsGoal;
	private char playerSign, compSign;
	private boolean addedSigns;
	private Dimension boardSize;
	private Class<? extends GameBoard> smartClass, randomClass;
	
	/**
	 * @param goal - The amount of clients needed to start a game
	 * @param boardSize - The size of the board (amount of cells)
	 * @param addedSigns - True if the game adds signs on the board, or false if they're moved
	 * @param smrtCls - The class of the game's smart model
	 * @param rndCls - The class of the game's random model
	 * @param playerSign - Sign of the human player on the board
	 * @param compSign - Sign of the computer player on the board
	 */
	private Game(int goal, Dimension boardSize, boolean addedSigns,
			     Class<? extends GameBoard> smrtCls,
			     Class<? extends GameBoard> rndCls,
			     char playerSign, char compSign) {
		
		this.clientsGoal = goal;
		this.boardSize = boardSize;
		this.smartClass = smrtCls;
		this.randomClass = rndCls;
		this.playerSign = playerSign;
		this.compSign = compSign;
		this.addedSigns = addedSigns;
	}
	
	/**
	 * @return the game's smart model object.
	 */
	public IGameAlgo getSmartModel() { return getModel(smartClass); }
	
	/**
	 * @return the game's random model object.
	 */
	public IGameAlgo getRandomModel() { return getModel(randomClass); }
	
	/**
	 * @param c - The model's class
	 * @return a new object of the model.
	 */
	private IGameAlgo getModel(Class<? extends GameBoard> c) {
		try { return c.asSubclass(GameBoard.class).getConstructor().newInstance(); }
		catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * @return true if the game adds signs to board, or false if it moves them around. 
	 */
	public boolean areSignsAdded() { return addedSigns; }
	
	/**
	 * @return the amount of clients needed to start a game.
	 */
	public int getGoalAmount() { return clientsGoal; }
	
	/**
	 * @return the human player's sign on the board.
	 */
	public char getPlayerSign() { return playerSign; }
	
	/**
	 * @return the computer player's sign on the board.
	 */
	public char getComputerSign() { return compSign; }
	
	/**
	 * @return the size of the board (rows X columns).
	 */
	public Dimension getBoardSize() { return boardSize; }
}