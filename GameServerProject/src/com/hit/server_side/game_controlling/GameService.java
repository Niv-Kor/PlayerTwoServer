package com.hit.server_side.game_controlling;
import java.util.HashSet;
import java.util.Set;
import com.hit.server_side.connection.HandleRequest;

public class GameService
{
	private Game game;
	private Set<HandleRequest> handlers;
	
	public GameService(Game game) {
		this.handlers = new HashSet<HandleRequest>();
		this.game = game;
	}
	
	public void addHandler(HandleRequest handler) {
		handlers.add(handler);
	}
	
	public void startGame() {
		
	}
}