/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonUtils
{
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
				return posBracket < posQuote && posQuote < posColon;
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
			JsonElement elem = new JsonParser().parse(jsonInString);
			//System.out.println("isJsonValid(): isJsonArray="+elem.isJsonArray()+", isJsonObject()="+elem.isJsonObject() + ". input=|"+jsonInString+"|.");
			return elem.isJsonArray() || elem.isJsonObject();
		}
		catch (Exception ex)
		{
			return false;
		}
	}
	public static Exception isJsonValid_returnException(String jsonInString)
	{
		if (StringUtil.isNullOrBlank(jsonInString))
			return null;

		try
		{
			JsonElement elem = new JsonParser().parse(jsonInString);
			//System.out.println("isJsonValid(): isJsonArray="+elem.isJsonArray()+", isJsonObject()="+elem.isJsonObject() + ". input=|"+jsonInString+"|.");
			if (elem.isJsonArray() || elem.isJsonObject())
				return null;
			
			return new Exception("Passed JSON Parse, but element is NOT a 'elem.isJsonArray()' or 'elem.isJsonObject()'.");
		}
		catch (Exception ex)
		{
			return ex;
		}
	}

	public static String format(String jsonStr)
	{
		return format(jsonStr, false);
	}
	/**
	 * Let a JSON Parser to the job of Pretty Print the JSON content
	 * @param jsonStr
	 * @return The Pretty printed JSON String
	 */
	public static String format(String jsonStr, boolean asHtml)
	{
		JsonParser parser = new JsonParser();
//		JsonObject jsonObj = parser.parse(jsonStr).getAsJsonObject();
		JsonElement jsonObj = parser.parse(jsonStr);

		
		Gson gsonTmp = new GsonBuilder().setPrettyPrinting().create();
		String prettyJson = gsonTmp.toJson(jsonObj);

		if (asHtml)
			return "<html><pre>\n" + prettyJson + "\n</pre></html>"; // possibly use <xmp> instead of <pre>
		else
			return prettyJson;
	}
}
