package com.asetune.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonUtils
{
	private static final Gson gson = new Gson();

	private JsonUtils()
	{
	}

	/**
	 * Loop until we find: Bracket Quote and Colon (max first 255 chars)<br>
	 * Then we can decide it it's possible a JSON<br>
	 * Break if we find any nonWhiteSpaces before the first '{' or '['<br>
	 * <p>
	 * If this returns tru you must also execute isJsonValid to verify that the JSON is a VALID JSON String
	 * 
	 * @param jsonInString
	 * @return
	 */
	public static boolean isPossibleJson(String jsonInString)
	{
		if (StringUtil.isNullOrBlank(jsonInString))
			return false;

		int posBracket = -1; // found '{' or '['
		int posQuote   = -1; // found '"'
		int posColon   = -1; // found ':'
		
		// Loop first 255 characters
		int len = Math.min(255, jsonInString.length());

		// Loop until we find: Bracket Quote and Colon
		// Then we can decide it it's possible a JSON
		// Break if we find any nonWhiteSpaces before the first '{' or '['
		for (int i=0; i<len; i++)
		{
			final char ch = jsonInString.charAt(i);

			if ( posBracket < 0 && (ch == '{' || ch == '[') )
				posBracket = i;
			
			if ( posQuote < 0 && ch == '"' )
				posQuote = i;
			
			if ( posColon < 0 && ch == ':' )
				posColon = i;

			// If NOT Whitespace, and we havn't yet seen a '{' or '[' then it CANT be a JSON
			if (posBracket < 0 && !Character.isWhitespace(ch))
				return false;

			// If we have found Bracket Quote and Colon
			if (posBracket >= 0 && posQuote >= 0 && posColon >= 0)
			{
				return posBracket < posQuote 
					&& posBracket < posQuote
					&& posQuote   < posColon;
			}
		}
		return false;
	}

	/**
	 * Let a JSON Parser decide if it's a VALID JSON String or not
	 * @param jsonInString
	 * @return
	 */
	public static boolean isJsonValid(String jsonInString)
	{
		if (StringUtil.isNullOrBlank(jsonInString))
			return false;

		try
		{
			gson.fromJson(jsonInString, Object.class);
			return true;
		}
		catch (com.google.gson.JsonSyntaxException ex)
		{
			return false;
		}
	}

	/**
	 * Let a JSON Parser to the job of Pretty Print the JSON content
	 * @param jsonStr
	 * @return The Pretty printed JSON String
	 */
	public static String format(String jsonStr)
	{
		JsonParser parser = new JsonParser();
//		JsonObject jsonObj = parser.parse(jsonStr).getAsJsonObject();
		JsonElement jsonObj = parser.parse(jsonStr);

		
		Gson gsonTmp = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gsonTmp.toJson(jsonObj);

		return prettyJson;
	}
}
