package com.hit.control;
import com.hit.server.HandleRequest;

import javaNK.util.networking.JSON;
import javaNK.util.networking.Protocol;

public class ClientIdentity
{
	private String name;
	private String avatarID;
	private Protocol protocol;
	private HandleRequest handler;
	private boolean isSociopath;
	
	/**
	 * @param name - Client's in-game name
	 * @param avatarID - The ID of the client's avatar
	 * @param prot - Protocol that's used to communicate with the client
	 * @param handler - The private serving thread of the client
	 * @param singlePlayer - True if the client wants to play against the computer
	 */
	public ClientIdentity(String name, String avatarID, Protocol prot, HandleRequest handler, boolean singlePlayer) {
		this.name = new String(name);
		this.avatarID = avatarID;
		this.protocol = prot;
		this.handler = handler;
		this.isSociopath = singlePlayer;
	}
	
	@Override
	public boolean equals(Object obj) {
		ClientIdentity other;
		
		if (!(obj instanceof ClientIdentity)) return false;
		else {
			other = (ClientIdentity) obj;
			return name.equals(other.name)
				&& protocol.getRemotePort() == other.getProtocol().getRemotePort();
		}
	}
	
	/**
	 * @return the client's name.
	 */
	public String getName() { return name; }
	
	/**
	 * @param n - The new client's name
	 */
	public void setName(String n) { name = new String(n); }
	
	/**
	 * @return the client's avatar ID.
	 */
	public String getAvatarID() { return avatarID; }
	
	/**
	 * @return the protocol that's used to communicate with the client.
	 */
	public Protocol getProtocol() { return protocol; }
	
	/**
	 * @param prot - The new protocol to communicate with the client
	 */
	public void setProtocol(Protocol prot) { protocol = prot; }
	
	/**
	 * @return the HandleRequest object that's assigned to serve the client. 
	 */
	public HandleRequest getHandler() { return handler; }
	
	/**
	 * @param hr - The new handler for the client to use
	 */
	public void setHandler(HandleRequest hr) { handler = hr; }
	
	/**
	 * @return true if the client wants to play against the computer.
	 */
	public boolean isSociopath() { return isSociopath; }
	
	/**
	 * Generate a JSON message, containing critical information about this client.
	 * 
	 * @param key - The type of the JSON message
	 * @return a JSON message with the client's information.
	 */
	public JSON generateJSONObject(String key) {
		JSON id = new JSON(key);
		id.put("name", name);
		id.put("avatar", avatarID);
		
		return id;
	}
	
	@Override
	public String toString() {
		return "[name: " + name + ", avatar: " + avatarID + ", port: " + protocol.getRemotePort() + "]";
	}
}