package com.trap.swallow.server;

@SuppressWarnings("serial")
public class SwallowException extends Exception {
	private String serverMessage;
	
	public SwallowException(String message, String serverMessage, Throwable t) {
		super(message, t);
		this.serverMessage = serverMessage;
	}
	public String getServerMessage() {
		return serverMessage;
	}

}
