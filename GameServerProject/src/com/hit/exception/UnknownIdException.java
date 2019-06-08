package com.hit.exception;

public class UnknownIdException extends Exception
{
	private static final long serialVersionUID = 8537683938621722621L;
	private static final String DEF_MESSAGE = "This game is not recognized by the server.";
	
	private String message;
	
	public UnknownIdException() {}
	
	/**
	 * @param message - The message of the error
	 */
	public UnknownIdException(String message) {
		this.message = new String(message);
	}
	
	@Override
	public String getMessage() {
		return (message != null) ? message : DEF_MESSAGE;
	}
}