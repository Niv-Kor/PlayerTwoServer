package com.hit.server_side.connection;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.hit.server_side.game_controlling.ServerSideController;
import com.hit.server_side.game_controlling.Game;

public class Server implements Runnable, PropertyChangeListener
{
	private Protocol seeker;
	private List<Protocol> serverSideProtocols;
	private GameServerController controller;
	private volatile boolean running;
	
	public Server(int port) {
		this.serverSideProtocols = new ArrayList<Protocol>();
		this.controller = new GameServerController(port);
		new Thread(this).start();
	}
	
	private void throwUnrecognizedMessage(String msg, String reason) {
		ServerLogger.error("Unrecognized command '" + msg + "', " + reason + ".");
	}
	
	private boolean isDuplicate(int port) {
		return ServerSideController.getPortsSet().contains(port);
	}

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if (e.getPropertyName().equals("running"))
			running = (boolean) e.getNewValue();
	}

	@Override
	public void run() {
		
	}
}