package com.hit.server_side.game_controlling;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo;

public class BoardGameHandler
{
	private IGameAlgo gameAlgo;
	
	public BoardGameHandler(IGameAlgo gameAlgo) {
		this.gameAlgo = gameAlgo;
	}
	
	public boolean updatePlayerMove(GameMove move) {
		return gameAlgo.updatePlayerMove(move);
	}
	
	public void calcComputerMove() {
		gameAlgo.calcComputerMove();
	}
}