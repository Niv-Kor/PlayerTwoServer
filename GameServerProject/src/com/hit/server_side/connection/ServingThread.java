package com.hit.server_side.connection;
import java.io.IOException;
import com.hit.server_side.game_controlling.ServerSideController;
import com.hit.server_side.game_controlling.ServerSideGame;
import game_algo.GameBoard.GameMove;
import game_algo.IGameAlgo.GameState;

public class ServingThread extends Thread
{
	private ServerSideProtocol serverSideProtocol;
	private int playerIndex;
	private ServerSideGame game;
	
	public ServingThread(ServerSideGame game, int playerIndex, ServerSideProtocol serverSideProtocol) {
		this.game = game;
		this.playerIndex = playerIndex;
		this.serverSideProtocol = serverSideProtocol;
	}
	
	@Override
	public void run() {
		while (!interrupted()) {
			try {
				String[] msg = serverSideProtocol.receive();
				
				delay();
				
				switch(msg[0]) {
					case "getsign": {
						serverSideProtocol.send("getsign:" + game.getPlayerSign());
						break;
					}
					case "p2getsign": {
						serverSideProtocol.send("p2getsign:" + game.getComputerSign());
						break;
					}
					case "move": {
						int row = Integer.parseInt(msg[1]);
						int col = Integer.parseInt(msg[2]);
						GameMove move = new GameMove(row, col);
						
						game.lastMovePlayerIndex = playerIndex;
						game.getSmartModel().updatePlayerMove(move);
						
						delay();
						
						String notificationStr = "p2move:" + row + ":" + col + ":";
						ServerSideController.informOthers(game, serverSideProtocol, notificationStr);
						break;
					}
					case "compmove": {
						game.getSmartModel().calcComputerMove();
						
						//notify player 1 which move was made
						GameMove compMove = game.anonymousMoveBuffer;
						int row = compMove.getRow();
						int col = compMove.getColumn();
						
						delay();
						
						String notificationStr = "p2move:" + row + ":" + col + ":";
						ServerSideController.informAll(game, notificationStr);
						break;
					}
					case "placecomp": {
						int row = Integer.parseInt(msg[1]);
						int col = Integer.parseInt(msg[2]);
						
						char[][] cloneMatrix = game.getSmartModel().getBoardState().clone();
						cloneMatrix[row][col] = game.getComputerSign();
						
						delay();
						
						String notificationStr = "p2move:" + row + ":" + col + ":";
						ServerSideController.informAll(game, notificationStr);
						break;
					}
					case "random": {
						game.lastMovePlayerIndex = playerIndex;
						game.getRandomModel().updatePlayerMove(null);
						
						GameMove p2Move = game.anonymousMoveBuffer;
						int row = p2Move.getRow();
						int col = p2Move.getColumn();
						
						delay();
						
						String notificationStr = "p2move:" + row + ":" + col + ":";
						ServerSideController.informOthers(game, serverSideProtocol, notificationStr);
						break;
					}
					case "comprandom": {
						game.getRandomModel().calcComputerMove();
						
						//notify player 1 which move was made
						GameMove compMove = game.anonymousMoveBuffer;
						int row = compMove.getRow();
						int col = compMove.getColumn();
						
						String notificationStr = "p2move:" + row + ":" + col + ":";
						ServerSideController.informAll(game, notificationStr);
						break;
					}
					case "tryend": {
						attemptEndgame();
						break;
					}
					default: throwUnrecognizedMessage(msg[0], "not available");
				}
			}
			catch(IOException e) {
				ServerLogger.print("encountered problem.");
				e.printStackTrace();
			}
		}
	}
	
	public void attemptEndgame() throws IOException {
		GameState state = game.getSmartModel().getGameState(null);
		
		if (state != GameState.IN_PROGRESS) {
			delay();
			ServerSideController.informEnd(game);
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
	
	public ServerSideProtocol getProtocol() { return serverSideProtocol; }
	public ServerSideGame getGame() { return game; }
}