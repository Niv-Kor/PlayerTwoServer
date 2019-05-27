package com.hit.util;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import javaNK.util.debugging.Logger;
import javaNK.util.math.Range;

public class CLI implements Runnable
{
	private static final Range<Integer> PARALLEL_GAMES_RANGE = new Range<Integer>(1, 10);
	
	private int parallelGames;
	private Scanner scanner;
	private PropertyChangeSupport propertyChangeHandler;
	private boolean running;
	
	public CLI(InputStream in, OutputStream out) {
		this.scanner = new Scanner(in);
		this.propertyChangeHandler = new PropertyChangeSupport(this);
		this.parallelGames = 1;
	}
	
	@Override
	public void run() {
		String command;
		
		while(true) {
			Logger.newLine();
			command = scanner.nextLine();
			
			switch(command) {
				case "GAME_SERVER_CONFIG": {
					int oldAmount = parallelGames;
					
					Logger.newLine();
					int amount = scanner.nextInt();
					
					System.out.println("continue");
					
					if (PARALLEL_GAMES_RANGE.intersects(amount)) parallelGames = amount;
					else fail("Amount of parallel games must be between "
							+ PARALLEL_GAMES_RANGE.getMin() + " and " + PARALLEL_GAMES_RANGE.getMax());
					
					propertyChangeHandler.firePropertyChange("parallel", oldAmount, parallelGames);
					break;
				}
				case "START": {
					propertyChangeHandler.firePropertyChange("running", running, true);
					running = true;
					Logger.print("The server has been started.");
					break;
				}
				case "SHUTDOWN": {
					propertyChangeHandler.firePropertyChange("running", running, false);
					running = false;
					Logger.print("The server has been shut down.");
					break;
				}
				default: {
					fail("unrecognized command");
					try { Thread.sleep(50); }
					catch(InterruptedException e) {}
				}
			}
		}
	}
	
	private void fail(String reason) {
		String addition = (reason != null) ? " " + reason : "";
		Logger.error("Invalid input." + addition + ".");
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeHandler.addPropertyChangeListener(listener);
	}
}