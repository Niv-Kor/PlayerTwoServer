import java.beans.PropertyChangeSupport;
import java.util.Scanner;

import com.hit.server_side.connection.Server;
import com.hit.server_side.connection.ServerLogger;

import general_utility.math.Range;

public class CLI implements Runnable
{
	private static final Range<Integer> PARALLEL_GAMES_RANGE = new Range<Integer>(1, 10);
	
	private int parallelGames;
	private Scanner scanner;
	private PropertyChangeSupport propertyChangeHandler;
	private Server server;
	private boolean running;
	
	public CLI() {
		this.scanner = new Scanner(System.in);
		this.propertyChangeHandler = new PropertyChangeSupport(this);
		this.parallelGames = 1;
		this.server = new Server();
	}
	
	@Override
	public void run() {
		String command;
		
		while(true) {
			ServerLogger.newLine();
			command = scanner.nextLine();
			
			switch(command) {
				case "GAME_SERVER_CONFIG": {
					int oldAmount = parallelGames;
					int amount = scanner.nextInt();
					
					if (PARALLEL_GAMES_RANGE.intersects(amount)) parallelGames = amount;
					else fail("Amount of parallel games must be between "
							+ PARALLEL_GAMES_RANGE.getMin() + " and " + PARALLEL_GAMES_RANGE.getMax());
					
					propertyChangeHandler.firePropertyChange("parallel", oldAmount, parallelGames);
					break;
				}
				case "START": {
					server.start();
					propertyChangeHandler.firePropertyChange("running", running, true);
					running = true;
					ServerLogger.print("Server started.");
					break;
				}
				case "SHUTDOWN": {
					server.interrupt();
					propertyChangeHandler.firePropertyChange("running", running, false);
					running = false;
					ServerLogger.print("Server was shut down.");
					break;
				}
				default: fail("Unrecognized command");
			}
		}
	}
	
	private void fail(String reason) {
		String addition = (reason != null) ? " " + reason : "";
		ServerLogger.print("Invalid input." + addition + ".");
	}
}