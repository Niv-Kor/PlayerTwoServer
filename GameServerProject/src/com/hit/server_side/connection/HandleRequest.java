package com.hit.server_side.connection;
import java.io.IOException;

import com.hit.server_side.game_controlling.BoardGameHandler;
import com.hit.server_side.game_controlling.ServerSideController;
import com.hit.server_side.game_controlling.Game;

import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo;
import game_algo.IGameAlgo.GameState;

public class HandleRequest extends Thread
{
	private Protocol protocol;
	private int playerIndex;
	private Game game;
	private BoardGameHandler gameHandler;
	
	public HandleRequest(Game game, int playerIndex, Protocol serverSideProtocol) {
		this.game = game;
		this.playerIndex = playerIndex;
		this.protocol = serverSideProtocol;
	}
	
	@Override
	public void run() {
		while (!interrupted()) {
			try {
				JSON msg = protocol.receive();
				delay();
				
				switch(msg.getType()) {
					case "player_sign": {
						JSON message = new JSON("player_sign");
						message.put("sign", "" + game.getPlayerSign());
						protocol.send(message);
						break;
					}
					case "player2_sign": {
						JSON message = new JSON("player2_sign");
						message.put("sign", "" + game.getComputerSign());
						protocol.send(message);
						break;
					}
					case "player_move": {
						int row = msg.getInt("row");
						int col = msg.getInt("column");
						GameMove move = new GameMove(row, col);
						if (!gameHandler.updatePlayerMove(move, game.getPlayerSign(), playerIndex)) break;
						
						delay();
						
						JSON message = new JSON("player2_move");
						message.put("row", row);
						message.put("column", col);
						ServerSideController.informOthers(game, protocol, message);
						break;
					}
					case "computer_move": {
						GameMove compMove = gameHandler.calcComputerMove(game.getComputerSign());
						
						//notify player 1 which move was made
						int row = compMove.getRow();
						int col = compMove.getColumn();
						
						delay();
						
						JSON message = new JSON("player2_move");
						message.put("row", row);
						message.put("column", col);
						ServerSideController.informAll(game, message);
						break;
					}
					case "place_computer": {
						int row = msg.getInt("row");
						int col = msg.getInt("column");
						gameHandler.place(new GameMove(row, col), game.getComputerSign());
						break;
					}
					case "place_player": {
						int row = msg.getInt("row");
						int col = msg.getInt("column");
						gameHandler.place(new GameMove(row, col), game.getPlayerSign());
						
						delay();
						
						JSON message = new JSON("player2_move");
						message.put("row", row);
						message.put("column", col);
						ServerSideController.informOthers(game, protocol, message);
						break;
					}
					case "player_random": {
						GameMove move = gameHandler.randomMove();
						JSON message = new JSON("player_random");
						message.put("row", move.getRow());
						message.put("column", move.getColumn());
						protocol.send(message);
						
						delay();
						
						message.setType("player2_move");
						ServerSideController.informOthers(game, protocol, message);
						break;
					}
					case "computer_random": {
						GameMove move = gameHandler.randomCompMove();
						JSON message = new JSON("computer_random");
						message.put("row", move.getRow());
						message.put("column", move.getColumn());
						protocol.send(message);
						
						message.setType("player2_move");
						ServerSideController.informOthers(game, protocol, message);
						break;
					}
					case "is_over": {
						attemptEndgame();
						break;
					}
					default: throwUnrecognizedMessage(msg.getType(), "not available");
				}
			}
			catch(IOException e) {
				ServerLogger.print("encountered problem.");
				e.printStackTrace();
			}
		}
	}
	
	public void attemptEndgame() throws IOException {
		GameState state = gameHandler.getGameState(game.getPlayerSign(), playerIndex);
		
		if (state != GameState.IN_PROGRESS) {
			delay();
			
			//notify the client about the game state
			JSON message = new JSON("end_game");
			message.put("game", game.name());
			message.put("state", state.name());
			protocol.send(message);
		}
	}
	
	private void throwUnrecognizedMessage(String msg, String reason) {
		ServerLogger.print("unrecognized command '" + msg + "', " + reason + ".");
	}
	
	private void delay() {
		try { Thread.sleep(100); }
		catch (InterruptedException e) {}
	}
	
	public int getPlayerIndex() { return playerIndex; }
	
	public Protocol getProtocol() { return protocol; }
	public Game getGame() { return game; }

	public void setGameAlgorithm(IGameAlgo gameAlgo) {
		gameHandler = new BoardGameHandler(gameAlgo);
	}
}