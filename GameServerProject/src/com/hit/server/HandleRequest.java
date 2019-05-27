package com.hit.server;
import java.io.IOException;
import com.hit.control.BoardGameHandler;
import com.hit.control.Game;
import com.hit.control.OpenGame;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo.GameState;
import javaNK.util.debugging.Logger;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

public class HandleRequest implements Runnable
{
	private Protocol protocol;
	private int playerIndex;
	private Game game;
	private OpenGame openGame;
	private BoardGameHandler boardHandler;
	
	public HandleRequest(OpenGame openGame, Protocol protocol, int playerIndex) {
		this.openGame = openGame;
		this.game = openGame.getGame();
		this.playerIndex = playerIndex;
		this.protocol = protocol;
		this.boardHandler = openGame.getBoardHandler();
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				JSON msg = protocol.receive();
				
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
						if (!boardHandler.updatePlayerMove(move, game.getPlayerSign(), playerIndex)) break;
						
						delay();
						
						JSON message = new JSON("player2_move");
						message.put("row", row);
						message.put("column", col);
						
						openGame.notifyOthers(protocol, message);
						break;
					}
					case "computer_move": {
						GameMove compMove = boardHandler.calcComputerMove(game.getComputerSign());
						
						//notify player 1 which move was made
						int row = compMove.getRow();
						int col = compMove.getColumn();
						
						delay();
						
						JSON message = new JSON("player2_move");
						message.put("row", row);
						message.put("column", col);
						openGame.notifyAll(message);
						break;
					}
					case "place_computer": {
						int row = msg.getInt("row");
						int col = msg.getInt("column");
						boardHandler.place(new GameMove(row, col), game.getComputerSign());
						break;
					}
					case "place_player": {
						int row = msg.getInt("row");
						int col = msg.getInt("column");
						boardHandler.place(new GameMove(row, col), game.getPlayerSign());
						
						delay();
						
						JSON message = new JSON("player2_move");
						message.put("row", row);
						message.put("column", col);
						openGame.notifyOthers(protocol, message);
						break;
					}
					case "player_random": {
						GameMove move = boardHandler.randomMove();
						JSON message = new JSON("player_random");
						message.put("row", move.getRow());
						message.put("column", move.getColumn());
						protocol.send(message);
						
						delay();
						
						message.setType("player2_move");
						openGame.notifyOthers(protocol, message);
						break;
					}
					case "computer_random": {
						GameMove move = boardHandler.randomCompMove();
						JSON message = new JSON("computer_random");
						message.put("row", move.getRow());
						message.put("column", move.getColumn());
						protocol.send(message);
						
						message.setType("player2_move");
						openGame.notifyOthers(protocol, message);
						break;
					}
					case "is_over": {
						boolean over = attemptEndgame();
						
						JSON message = new JSON("is_over");
						message.put("over", over);
						protocol.send(message);
						break;
					}
					default: Logger.error(msg, "Not available");
				}
			}
			catch(IOException e) {
				Logger.error("Encountered a problem.");
				e.printStackTrace();
			}
			boardHandler.printBoard();
		}
	}
	
	public boolean attemptEndgame() throws IOException {
		GameState state = boardHandler.getGameState(game.getPlayerSign(), playerIndex);

		if (state != GameState.IN_PROGRESS) {
			delay();
			
			//notify the client about the game state
			JSON message = new JSON("end_game");
			message.put("game", game.name());
			message.put("state", state.name());
			protocol.send(message);
			
			return true;
		}
		else return false;
	}
	
	private void delay() {
		try { Thread.sleep(100); }
		catch (InterruptedException e) {}
	}
	
	public int getPlayerIndex() { return playerIndex; }
	
	public Protocol getProtocol() { return protocol; }
	
	public Game getGame() { return game; }

	public void reissueBoardHandler(BoardGameHandler handler) { boardHandler = handler; }
}