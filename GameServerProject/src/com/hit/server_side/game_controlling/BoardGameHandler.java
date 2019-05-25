package com.hit.server_side.game_controlling;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo;
import game_algo.IGameAlgo.GameState;

public class BoardGameHandler
{
	private IGameAlgo gameAlgo;
	
	public BoardGameHandler(IGameAlgo gameAlgo) {
		this.gameAlgo = gameAlgo;
	}
	
	public boolean updatePlayerMove(GameMove move) {
		return gameAlgo.updatePlayerMove(move);
	}
	
	public GameMove calcComputerMove(char compSign) {
		gameAlgo.calcComputerMove();
		
		char[][] board = gameAlgo.getBoardState();
		for (int i = 0; i < board.length; i++) {
			for (int j = 0; j < board[i].length; j++) {
				if (board[i][j] == compSign)
					return new GameMove(i, j);
			}
		}
		
		return null; //format return statement
	}
	
	public void place(GameMove spot, char sign) {
		char[][] cloneBoard = gameAlgo.getBoardState().clone();
		cloneBoard[spot.getRow()][spot.getColumn()] = sign;
	}
	
	public GameState getGameState() {
		return gameAlgo.getGameState(null);
	}
}