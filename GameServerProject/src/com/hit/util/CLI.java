package com.hit.util;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.InputStream;
import java.io.OutputStream;

import javaNK.util.debugging.Logger;
import javaNK.util.threads.ThreadUtility;

public class CLI implements Runnable
{
	public static final int DEFAULT_BACKLOG = 10;
	
	private PropertyChangeSupport propertyChangeHandler;
	private boolean running;
	private int backlog;
	
	/**
	 * @param in - The stream from which the CLI commands are received
	 * @param out - The stream that gets the CLI responses
	 */
	public CLI(InputStream in, OutputStream out) {
		Logger.configInputStream(in);
		Logger.configOutputStream(out);
		this.propertyChangeHandler = new PropertyChangeSupport(this);
		this.backlog = DEFAULT_BACKLOG;
	}
	
	@Override
	public void run() {
		String command;
		
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
					if (running) Logger.error("Cannot change configurations while the server is running.");
					else {
						int oldValue = backlog;
						backlog = Logger.inputInt();
						
						propertyChangeHandler.firePropertyChange("backlog", oldValue, backlog);
						writeResponse("Backlog value is now " + backlog + ".");
					}
					
					break;
				}
				default: {
					Logger.error("Unrecognized command.");
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