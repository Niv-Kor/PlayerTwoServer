package com.hit.server;
import java.io.IOException;
import com.hit.control.BoardGameHandler;
import com.hit.control.Game;
import com.hit.control.OpenGame;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo.GameState;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;
import javaNK.util.networking.RespondCase;
import javaNK.util.networking.RespondEngine;

public class HandleRequest extends RespondEngine
{
	private int playerIndex;
	private Game game;
	private OpenGame openGame;
	private BoardGameHandler boardHandler;
	
	public HandleRequest(OpenGame openGame, Protocol prot, int playerIndex) throws IOException {
		super(prot);
		
		this.openGame = openGame;
		this.game = openGame.getGame();
		this.playerIndex = playerIndex;
		this.boardHandler = openGame.getBoardHandler();
		new Thread(this).start();
	}
	
	public boolean attemptEndgame() throws IOException {
		GameState state = boardHandler.getGameState(game.getPlayerSign(), playerIndex);

		//notify the client about the game state
		if (state != GameState.IN_PROGRESS) {
			JSON message = new JSON("end_game");
			message.put("game", game.name());
			message.put("state", state.name());
			protocol.send(message);
			
			return true;
		}
		else return false;
	}
	
	@Override
	protected void initCases() {
		//get player's sign
		addCase(new RespondCase() {
			@Override
			public String getType() { return "player_sign"; }

			@Override
			public void respond(JSON msg) throws Exception {
				JSON message = new JSON("player_sign");
				message.put("sign", "" + game.getPlayerSign());
				protocol.send(message);
			}
		});
		
		//get player 2's sign
		addCase(new RespondCase() {
			@Override
			public String getType() { return "player2_sign"; }

			@Override
			public void respond(JSON msg) throws Exception {
				JSON message = new JSON("player2_sign");
				message.put("sign", "" + game.getComputerSign());
				protocol.send(message);
			}
		});
		
		//make a player move
		addCase(new RespondCase() {
			@Override
			public String getType() { return "player_move"; }

			@Override
			public void respond(JSON msg) throws Exception {
				int row = msg.getInt("row");
				int col = msg.getInt("column");
				GameMove move = new GameMove(row, col);
				
				//make the move + check if unsuccessful
				boolean success = boardHandler.updatePlayerMove(move, game.getPlayerSign(), playerIndex);
				
				JSON message = new JSON("player_move");
				message.put("success", success);
				protocol.send(message);
				
				JSON p2message = new JSON("player2_move");
				p2message.put("row", row);
				p2message.put("column", col);
				openGame.notifyOthers(protocol, p2message);
			}
		});
		
		//make a computer move
		addCase(new RespondCase() {
			@Override
			public String getType() { return "computer_move"; }

			@Override
			public void respond(JSON msg) throws Exception {
				GameMove compMove = boardHandler.calcComputerMove(game.getComputerSign());
				
				//notify player 1 which move was made
				int row = compMove.getRow();
				int col = compMove.getColumn();
				
				JSON message = new JSON("player2_move");
				message.put("row", row);
				message.put("column", col);
				openGame.notifyAll(message);
			}
		});
		
		//place the player's sign on the board manually
		addCase(new RespondCase() {
			@Override
			public String getType() { return "place_player"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				int row = msg.getInt("row");
				int col = msg.getInt("column");
				boardHandler.place(new GameMove(row, col), game.getPlayerSign());
				
				JSON message = new JSON("player2_move");
				message.put("row", row);
				message.put("column", col);
				openGame.notifyOthers(protocol, message);
			}
		});
		
		//place the computer's sign on the board manually
		addCase(new RespondCase() {
			@Override
			public String getType() { return "place_computer"; }

			@Override
			public void respond(JSON msg) throws Exception {
				int row = msg.getInt("row");
				int col = msg.getInt("column");
				boardHandler.place(new GameMove(row, col), game.getComputerSign());
			}
		});
		
		//make a random player move
		addCase(new RespondCase() {
			@Override
			public String getType() { return "player_random"; }

			@Override
			public void respond(JSON msg) throws Exception {
				GameMove move = boardHandler.randomMove();
				JSON message = new JSON("player_random");
				message.put("row", move.getRow());
				message.put("column", move.getColumn());
				protocol.send(message);
				
				message.setType("player2_move");
				openGame.notifyOthers(protocol, message);
			}
		});
		
		//make a random computer move
		addCase(new RespondCase() {
			@Override
			public String getType() { return "computer_random"; }
			
			@Override
			public void respond(JSON msg) throws Exception {
				GameMove move = boardHandler.randomCompMove();
				JSON message = new JSON("computer_random");
				message.put("row", move.getRow());
				message.put("column", move.getColumn());
				protocol.send(message);
				
				message.setType("player2_move");
				openGame.notifyOthers(protocol, message);
			}
		});
		
		//check that the game is over
		addCase(new RespondCase() {
			@Override
			public String getType() { return "is_over"; }

			@Override
			public void respond(JSON msg) throws Exception {
				System.out.println("got is over");
				boolean over = attemptEndgame();
				
				JSON message = new JSON("is_over");
				message.put("over", over);
				protocol.send(message);
			}
		});
	}
	
	public int getPlayerIndex() { return playerIndex; }
	
	public Protocol getProtocol() { return protocol; }
	
	public Game getGame() { return game; }

	public void reissueBoardHandler(BoardGameHandler handler) { boardHandler = handler; }
}