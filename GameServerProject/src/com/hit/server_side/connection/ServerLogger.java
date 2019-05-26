package com.hit.server_side.connection;
import java.time.LocalDateTime;

public class ServerLogger
{
	private static final char DASH = '>';
	private static final String PREFIX = "Server: ";
	
	public static void print(String msg) {
		String period = (msg.charAt(msg.length() - 1) != '.') ? "." : "";
		System.out.println(timeStamp() + " " + DASH + " " + PREFIX + msg + period);
	}
	
	public static void newLine() {
		System.out.print(timeStamp() + " " + DASH + " ");
	}
	
	public static void error(String msg) {
		newLine();
		String period = (msg.charAt(msg.length() - 1) != '.') ? "." : "";
		System.err.println(PREFIX + msg + period);
	}
	
	private static String timeStamp() {
		LocalDateTime now = LocalDateTime.now();
		int nano = trim(now.getNano(), 3);
		return now.getHour() + ":" + now.getMinute() + ":" + now.getSecond() + ":" + nano;
	}
	
	private static int trim(int value, int places) {
		return Integer.parseInt(("" + value).substring(0, places));
	}
}