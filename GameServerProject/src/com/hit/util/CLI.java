package com.hit.util;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.io.OutputStream;

import javaNK.util.debugging.Logger;
import javaNK.util.math.Range;
import javaNK.util.threads.ThreadUtility;

public class CLI implements Runnable
{
	private static final Range<Integer> PARALLEL_GAMES_RANGE = new Range<Integer>(1, 10);
	
	private int parallelGames;
	private PropertyChangeSupport propertyChangeHandler;
	private boolean running;
	
	/**
	 * @param in - The stream from which the CLI commands are received
	 * @param out - The stream that gets the CLI responses
	 */
	public CLI(InputStream in, OutputStream out) {
		Logger.configInputStream(in);
		Logger.configOutputStream(out);
		this.propertyChangeHandler = new PropertyChangeSupport(this);
		this.parallelGames = 1;
	}
	
	@Override
	public void run() {
		String command;
		Logger.newLine();
		
		while(true) {
			command = Logger.inputLine();
			
			switch(command) {
				case "START": {
					if (running) Logger.error("The server is already running.");
					else {
						propertyChangeHandler.firePropertyChange("running", running, true);
						running = true;
						writeResponse("The server has been started.");
					}
					
					break;
				}
				case "SHUTDOWN": {
					if (!running) Logger.error("The server is already down.");
					else {
						propertyChangeHandler.firePropertyChange("running", running, false);
						running = false;
						writeResponse("The server has been shut down.");
					}
					
					break;
				}
				case "GAME_SERVER_CONFIG": {
					int oldAmount = parallelGames;
					
					int amount = Logger.inputInt();
					
					if (PARALLEL_GAMES_RANGE.intersects(amount)) parallelGames = amount;
					else fail("Amount of parallel games must be between "
							+ PARALLEL_GAMES_RANGE.getMin() + " and " + PARALLEL_GAMES_RANGE.getMax());
					
					propertyChangeHandler.firePropertyChange("parallel", oldAmount, parallelGames);
					writeResponse("Available parallel games amount is now " + amount + ".");
					break;
				}
				default: {
					fail("Unrecognized command.");
					ThreadUtility.delay(50);
				}
			}
		}
	}
	
	/**
	 * Write a response to a command in the console.
	 * 
	 * @param response - A Response to the last received command
	 */
	private void writeResponse(String response) {
		Logger.print(response);
	}
	
	private void fail(String reason) {
		String addition = (reason != null) ? " " + reason : "";
		Logger.error("Invalid input." + addition + ".");
	}
	
	/**
	 * Add a property-change listener to the CLI.
	 * 
	 * @param listener - A new property-change listener to add
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeHandler.addPropertyChangeListener(listener);
	}
	
	/**
	 * Remove a property-change listener from the CLI.
	 * 
	 * @param listener - A property-change listener to remove
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeHandler.removePropertyChangeListener(listener);
	}
}