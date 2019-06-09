package com.hit.control;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo;
import game_algo.IGameAlgo.GameState;

public class BoardGameHandler
{
	private IGameAlgo smartAlgo, randomAlgo, compAlgo;
	private int rows, cols;
	private boolean addedSigns;
	
	public BoardGameHandler(Game game, IGameAlgo compGameAlgo) {
		this.smartAlgo = game.getSmartModel();
		this.randomAlgo = game.getRandomModel();
		this.compAlgo = compGameAlgo;
		
		//merge all game algo objects so they share the same board state
		randomAlgo.merge(smartAlgo);
		compAlgo.merge(smartAlgo);
		
		this.rows = game.getBoardSize().height;
		this.cols = game.getBoardSize().width;
		this.addedSigns = game.areSignsAdded();
	}
	
	/**
	 * Make a move for the human player.
	 * 
	 * @param move - The move to make
	 * @param playerSign - The player's sign
	 * @param playerIndex - The player's index (critical when playing online, but meaningless if not)
	 * @return true if the move is legal and had been applied to the board, or false otherwise.
	 */
	public boolean updatePlayerMove(GameMove move, char playerSign, int playerIndex) {
		//find last move
		GameMove oldSpot = null;
		if (!addedSigns) oldSpot = find((char) (playerSign + playerIndex));
		
		//make move
		boolean success = smartAlgo.updatePlayerMove(move);
		
		/*
		 * Replace the sign with a unique sign that reflects the player's index.
		 * This solution solves the problem of multiplaying, where both players
		 * use the gameAlgo.updatePlayerMove() method, with the same sign.
		 * When it comes to checking the board for a victory, all the signs are the same.
		 * 
		 * When playing in single player mode, the player's index is always 0,
		 * and thus not affected by this solution. 
		 */
		if (success) {
			if (!addedSigns && oldSpot != null) place(oldSpot, '-');
			place(move, (char) (playerSign + playerIndex));
		}
		
		return success;
	}
	
	/**
	 * Make a move for the computer.
	 * 
	 * @param compSign - The computer player's sign
	 * @return the move that the computer made.
	 */
	public GameMove calcComputerMove(char compSign) {
		//get an instance of the game algo board
		char[][] originBoard = compAlgo.getBoardState();
		
		//copy the board for later use
		char[][] beforeBoard = new char[rows][cols];
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				beforeBoard[i][j] = originBoard[i][j];
		
		//make the move
		compAlgo.calcComputerMove();
		
		//look for a changed cell - this is the computer's move
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (beforeBoard[i][j] != compSign && originBoard[i][j] == compSign)
					return new GameMove(i, j);
			}
		}
		
		return null; //formal return statement
	}
	
	/**
	 * Make a random move for the human player.
	 * 
	 * @return the move that had been made.
	 */
	public GameMove randomMove(char playerSign, int playerIndex) {
		char[][] before = copyBoardState();
		randomAlgo.updatePlayerMove(null);
		GameMove spot = findLastMove(before, randomAlgo.getBoardState());
		place(spot, (char) (playerSign + playerIndex));
		return spot;
	}
	
	/**
	 * Make a random move for the computer player.
	 * 
	 * @return the move that had been made.
	 */
	public GameMove randomCompMove() {
		char[][] before = copyBoardState();
		randomAlgo.calcComputerMove();
		return findLastMove(before, randomAlgo.getBoardState());
	}
	
	/**
	 * @return a copy of the game's board state.
	 */
	private char[][] copyBoardState() {
		char[][] original = smartAlgo.getBoardState();
		char[][] res = new char[rows][cols];
		
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				res[i][j] = original[i][j];

		return res;
	}
	
	/**
	 * Find the last move that had been applied to the board.
	 * 
	 * @param before - The board before the applied move
	 * @param after - The board after the applied move
	 * @return the spot that the last move took place on, or null if nothing changed.
	 */
	private GameMove findLastMove(char[][] before, char[][] after) {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (before[i][j] != after[i][j])
					return new GameMove(i, j);
			}
		}
		
		return null;
	}
	
	/**
	 * Place a player on the board manually.
	 * 
	 * @param spot - The spot to place the player on
	 * @param sign - The player's sign
	 */
	public void place(GameMove spot, char sign) {
		char[][] cloneBoard = smartAlgo.getBoardState().clone();
		cloneBoard[spot.getRow()][spot.getColumn()] = sign;
	}
	
	/**
	 * Get the game state for an individual player.
	 * 
	 * @param playerSign - The sign of the checked player 
	 * @return the checked player's game state.
	 */
	public GameState getGameState(char playerSign, int playerIndex) {
		GameMove anySignSpot = null;
		char[][] board = smartAlgo.getBoardState();
		int rows = board.length, cols = board[0].length;
		
		//find a spot on the board that contains the argument sign
		char uniqueSign = (char) (playerSign + playerIndex);
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				if (board[i][j] == uniqueSign)
					anySignSpot = new GameMove(i, j);
		
		return smartAlgo.getGameState(anySignSpot);
	}
	
	/**
	 * Find a player's sign on the board.
	 * 
	 * @param playerSign - The sign to look for
	 * @return the spot on the board where that sign is found, or null if it couldn't be found.
	 */
	private GameMove find(char playerSign) {
		char[][] cloneBoard = smartAlgo.getBoardState().clone();
		
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (cloneBoard[i][j] == playerSign)
					return new GameMove(i, j);
			}
		}
		
		return null;
	}
	
	/**
	 * Print the board to the console. Used for debugging.
	 */
	public void printBoard() {
		char[][] cloneBoard = smartAlgo.getBoardState().clone();
		int rows = cloneBoard.length, cols = cloneBoard[0].length;
		
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				System.out.print(cloneBoard[i][j]);
			}
			System.out.println();
		}
		System.out.println();
	}
}