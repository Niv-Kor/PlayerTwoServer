package com.hit.control;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo;
import game_algo.IGameAlgo.GameState;

public class BoardGameHandler
{
	private IGameAlgo gameAlgo;
	
	public BoardGameHandler(IGameAlgo gameAlgo) {
		this.gameAlgo = gameAlgo;
	}
	
	public boolean updatePlayerMove(GameMove move, char playerSign, int playerIndex) {
		boolean success = gameAlgo.updatePlayerMove(move);
		
		/*
		 * Replace the sign with a unique sign that reflects the player's index.
		 * This solution solves the problem of multiplaying, where both players
		 * use the gameAlgo.updatePlayerMove() method, with the same sign.
		 * When it comes to checking the board for a victory, all the signs are the same.
		 * 
		 * When playing in single player mode, the player's index is always 0,
		 * and thus not affected by this solution. 
		 */
		if (success) place(move, (char) (playerSign + playerIndex));
		System.out.println("success " + success);
		return success;
	}
	
	public GameMove calcComputerMove(char compSign) {
		//get an instance of the game algo board
		char[][] originBoard = gameAlgo.getBoardState();
		int rows = originBoard.length, cols = originBoard[0].length;
		
		//copy the board for later use
		char[][] beforeBoard = new char[rows][cols];
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				beforeBoard[i][j] = originBoard[i][j];
		
		//make the move
		gameAlgo.calcComputerMove();
		
		//look for a changed cell - this is the computer's move
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				if (beforeBoard[i][j] != compSign && originBoard[i][j] == compSign)
					return new GameMove(i, j);
			}
		}
		
		return null; //format return statement
	}
	
	//TODO
	public GameMove randomMove() {
		return null;
	}
	
	//TODO
	public GameMove randomCompMove() {
		return null;
	}
	
	/**
	 * Place a player on the board manually.
	 * @param spot - The spot to place the player on
	 * @param sign - The player's sign
	 */
	public void place(GameMove spot, char sign) {
		char[][] cloneBoard = gameAlgo.getBoardState().clone();
		cloneBoard[spot.getRow()][spot.getColumn()] = sign;
	}
	
	/**
	 * Get the game state for an individual player.
	 * @param playerSign - The sign of the checked player 
	 * @return the checked player's game state.
	 */
	public GameState getGameState(char playerSign, int playerIndex) {
		GameMove anySignSpot = null;
		char[][] board = gameAlgo.getBoardState();
		int rows = board.length, cols = board[0].length;
		
		//find a spot on the board that contains the argument sign
		char uniqueSign = (char) (playerSign + playerIndex);
		for (int i = 0; i < rows; i++)
			for (int j = 0; j < cols; j++)
				if (board[i][j] == uniqueSign)
					anySignSpot = new GameMove(i, j);
		
		return gameAlgo.getGameState(anySignSpot);
	}
	
	public void printBoard() {
		char[][] cloneBoard = gameAlgo.getBoardState().clone();
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