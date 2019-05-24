package com.hit.server_side.connection;
import java.time.LocalDateTime;

public class ServerLogger
{
	private static final char DASH = '>';
	private static final String PREFIX = DASH + " Server: ";
	
	public static void print(String msg) {
		System.out.println(timeStamp() + " " + PREFIX + msg);
	}
	
	private static String timeStamp() {
		LocalDateTime now = LocalDateTime.now();
		int nano = trim(now.getNano(), 3);
		return now.getHour() + ":" + now.getMinute() + ":" + now.getSecond() + ":" + nano;
	}
	
	public static void newLine() {
		System.out.print(timeStamp() + " " + DASH + " ");
	}
	
	public static int trim(int value, int places) {
		return Integer.parseInt(("" + value).substring(0, places));
	}
}