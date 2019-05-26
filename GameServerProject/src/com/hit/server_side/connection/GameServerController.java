package com.hit.server_side.connection;
import java.io.IOException;
import com.hit.server_side.game_controlling.Game;
import com.hit.server_side.game_controlling.ServerSideController;

//create new gameServices or connect clients to exist gameServices upon communication
public class GameServerController implements Runnable
{
	private Protocol seeker;
	private volatile boolean running;
	
	public GameServerController(int port) {
		//init client seeker, to add or remove clients from server
		try { this.seeker = new Protocol(port, null); }
		catch (IOException e) { e.printStackTrace(); }
		
		new Thread(this).start();
	}
	
	@Override
	public void run() {
		while(true) {
			while(running) {
				try {
					JSON msg = seeker.receive();
					
					//the game the client is referring to
					Game game = Game.valueOf(msg.getString("game"));
					
					//clients send their port with the message
					int targetPort = msg.getInt("port");
					
					//sleep before answering
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					
					switch(msg.getType()) {
						//a client wants to join the server
						case "new_client": {
							if (isDuplicate(targetPort) || game.ready()) break;
							
							Protocol newProt = new Protocol();
							serverSideProtocols.add(newProt);
							newProt.setTargetPort(targetPort);
							seeker.setTargetPort(targetPort);
							
							//notify client with his new target port
							JSON message = new JSON("new_client");
							message.put("port", newProt.getPort());
							seeker.send(message);
							
							ServerSideController.addClient(game, newProt);
							game.addClient();
							
							ServerLogger.print("added player at port " + newProt.getPort() + ".");
							break;
						}
						//a client wants to leave the server
						case "leaving_client": {
							if (!isDuplicate(targetPort) || game.getClientsAmount() == 0) break;
							
							Protocol freeConnection = ServerSideController.removeClient(targetPort);
							if (freeConnection != null) {
								ServerLogger.print("client with port " + freeConnection.getPort() + " removed.");
								game.removeClient();
							}
							break;
						}
						default: throwUnrecognizedMessage(msg.getType(), "not available");
					}
					
					//start game if there's enough clients
					if (game.ready() && !game.isRunning()) {
						try { Thread.sleep(1000); }
						catch (InterruptedException e) {}
						
						ServerSideController.startGame(game);
						game.run(true);
					}
				}
				catch(IOException e) {
					ServerLogger.print("encountered problem.");
					e.printStackTrace();
				}
			}
		}
	}
}
