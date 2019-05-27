package com.hit.server;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import com.hit.control.Game;
import com.hit.control.OpenGame;
import com.hit.services.GameServerController;
import javaNK.util.debugging.Logger;
import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

public class Server implements Runnable, PropertyChangeListener
{
	private Protocol generalCommunicator;
	private GameServerController controller;
	private volatile boolean running;
	
	/**
	 * @param port - The port that this will listens to
	 */
	public Server(int port) {
		try { this.generalCommunicator = new Protocol(port, null); }
		catch (IOException e) { e.printStackTrace(); }
		
		this.controller = new GameServerController(this);
		new Thread(this).start();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("running"))
			running = (boolean) e.getNewValue();
	}

	@Override
	public void run() {
		while(true) {
			while(running) {
				try {
					JSON msg = generalCommunicator.receive();
					
					//the game the client is referring to
					Game game = Game.valueOf(msg.getString("game"));
					
					//clients send their port with the message
					int targetPort = msg.getInt("port");
					
					//sleep before answering
					try { Thread.sleep(100); }
					catch (InterruptedException e) {}
					
					switch(msg.getType()) {
						case "new_client": {
							//check if client already playing
							if (!controller.isAllowed(targetPort, game)) continue;
							
							//create a new protol that listens to the new client
							Protocol newProt = new Protocol(false);
							newProt.setTargetPort(targetPort);
							
							OpenGame openGame = controller.addClient(newProt, game, msg);
							
							//notify client about his new target port
							generalCommunicator.setTargetPort(msg.getInt("port"));
							JSON message = new JSON("new_client");
							message.put("port", newProt.getPort());
							generalCommunicator.send(message);
							
							try { Thread.sleep(100); }
							catch (InterruptedException e) {}
							
							//inform all about the beginning of the game
							if (openGame.canRun()) notifyStart(openGame);
							
							Logger.print("Added player at port " + newProt.getPort() + ".");
							break;
						}
						case "leaving_client": {
							controller.closeGame(targetPort, game);
							
							Logger.print("Player from port " + targetPort + " "
									   + "has left the " + game.name() + " game.");
							break;
						}
						case "happy_client": {
							controller.restartGame(targetPort, game);
							break;
						}
						default: Logger.error(msg, "Not available.");
					}
				}
				catch(IOException e) {
					Logger.print("Encountered a problem: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Notify all clients about the beginning of the game.
	 * 
	 * @param openGame - The open game that's starting 
	 * @throws IOException when at least one of the clients' protocols is unavailable.
	 */
	public void notifyStart(OpenGame openGame) throws IOException {
		boolean gaveTurn = false;
		boolean firstTurnGiver;
		
		for (Protocol clientProtocol : openGame.getClients()) {
			//give first turn to the first player in list
			if (!gaveTurn) {
				firstTurnGiver = true;
				gaveTurn = true;
			}
			else firstTurnGiver = false;
			
			JSON message = new JSON("start_game");
			message.put("game", openGame.getGame().name());
			message.put("turn", firstTurnGiver);
			
			generalCommunicator.setTargetPort(clientProtocol.getTargetPort());
			generalCommunicator.send(message);
		}
	}
	
	/**
	 * Notify one client with a custom message.
	 * @param prot - The protocol of the client
	 * @param msg - The JSON message
	 * @throws IOException when the client's protocol is unavailable
	 */
	public void notify(Protocol prot, JSON msg) throws IOException {
		generalCommunicator.setTargetPort(prot.getTargetPort());
		generalCommunicator.send(msg);
	}
}