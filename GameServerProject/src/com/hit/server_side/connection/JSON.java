package com.hit.server_side.connection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JSON extends JSONObject
{
	private static final long serialVersionUID = 3472688265395105685L;

	/**
	 * This constructor has two roles - create a new JSON object, or convert a string to JSON object.
	 * 
	 * @param str - If constructing a new JSON object - this argument is the message type {"type:*"}.
	 * 				If converting a String to a JSON object - this argument is the string to convert.
	 */
	public JSON(String str) {
		if (str.charAt(0) == '{') initConversion(str);
		else initNew(str);
	}
	
	/**
	 * Initiate a new JSON object.
	 * @param title - The message type
	 */
	private void initNew(String title) {
		put("type", title);
	}
	
	/**
	 * Convert a String to JSON.
	 * @param message - The String to convert
	 */
	private void initConversion(String message) {
		JSONParser parser = new JSONParser();
		try {
			//parse the message into a JSONObject
			JSONObject obj = (JSONObject) parser.parse(message);
			
			//split the rows to an array
			String[] rows = obj.toJSONString().replace("{", "")
											  .replace("}", "")
											  .replace("\"", "")
											  .split(",");
			
			//split each row to key and value and insert both
			for (int i = 0; i < rows.length; i++) {
				String[] keyVal = rows[i].split(":");
				put(keyVal[0], keyVal[1]);
			}
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @see JSONObject.put(Object, Object)
	 */
	@SuppressWarnings("unchecked")
	public Object put(String key, Object value) {
		return super.put(key, value);
	}
	
	/**
	 * @return the type of the message.
	 */
	public String getType() {
		return getString("type");
	}
	
	public void setType(String title) {
		remove("type");
		put("type", title);
	}
	
	public int getInt(String key) {
		return Integer.parseInt(getString(key));
	}
	
	public char getChar(String key) {
		return (char) get(key);
	}
	
	public String getString(String key) {
		return (String) get(key);
	}
	
	public boolean getBoolean(String key) {
		return (boolean) get(key);
	}
	
	@Override
	public String toString() {
		return toJSONString().replace("\\\"", "");
	}
}