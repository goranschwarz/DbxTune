/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
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






	public static class MissingFieldException
	extends Exception
	{
		private static final long serialVersionUID = 1L;

		public MissingFieldException(String msg)
		{
			super(msg);
		}

		public MissingFieldException(String msg, Exception ex)
		{
			super(msg, ex);
		}
	}
	
	/**
	 * Get Node from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A String
	 * @throws MissingFieldException  When the field name is not found
	 */
	public static JsonNode getNode(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		return n;
	}

	/**
	 * Check if the field Exists.
	 * @param node
	 * @param fieldName  name of the field(s) you checking for... It may be more than one
	 * @return 
	 */
	public static boolean doFieldExist(JsonNode node, String... fieldNames)
	{
		for (String fieldName : fieldNames)
		{
			if (node.get(fieldName) == null)
				return false;
		}
		return true;
	}

	/**
	 * Get String from a node 
	 * @param node           The node
	 * @param fieldName      The fields name
	 * @param defaultValue   Default value if field is not found
	 * @return               A String
	 */
	public static String getString(JsonNode node, String fieldName, String defaultValue)
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
//		if (n.isNull())
//			return null;
		if (n.isNull())
			return defaultValue;
		return n.asText();
	}

	/**
	 * Get String from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A String
	 * @throws MissingFieldException  When the field name is not found
	 */
	public static String getString(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		if (n.isNull())
			return null;
		return n.asText();
	}

	/**
	 * Get String from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A String
	 * @throws MissingFieldException  When the field name is not found
	 */
	public static String getStringAny(JsonNode node, String... fieldNames)
	throws MissingFieldException
	{
		for (String entry : fieldNames)
		{
			if ( ! node.has(entry) )
				continue;

			JsonNode n = node.get(entry);
			if (n.isNull())
				return null;
			return n.asText();
		}
		throw new MissingFieldException("Expecting any fields '" + StringUtil.toCommaStr(fieldNames) + "' but none of them was found.");
	}

	/**
	 * Get String from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A String
	 * @throws MissingFieldException  When the field name is not found
	 */
	public static String getStringAny(String defaultVal, JsonNode node, String... fieldNames)
//	throws MissingFieldException
	{
		for (String entry : fieldNames)
		{
			if ( ! node.has(entry) )
				continue;

			JsonNode n = node.get(entry);
			if (n.isNull())
				return null;
			return n.asText();
		}
		return defaultVal;
	}

	/**
	 * Get String LIST from a node 
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   A List of Strings
	 * @throws MissingFieldException  When the field name is not found
	 */
	public static List<String> getStringList(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return null;
//			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		if (n.isNull())
			return null;
		
		if ( ! n.isArray() )
			return StringUtil.parseCommaStrToList(n.asText());

		List<String> list = new ArrayList<>();
		for(JsonNode jsonNode : n) 
		{
			list.add(jsonNode.asText());
		}
		return list;
	}

	/**
	 * Get int value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an int
	 */
	public static int getInt(JsonNode node, String fieldName, int defaultValue)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		return n.asInt();
	}

	/**
	 * Get int value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @return                   an int
	 * @throws MissingFieldException  When the field name is not found
	 */
	public static int getInt(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		return n.asInt();
	}

	/**
	 * Get boolean value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an boolean value
	 */
	public static boolean getBoolean(JsonNode node, String fieldName, boolean defaultValue)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		return n.asBoolean();
	}

	/**
	 * Get boolean value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @throws MissingFieldException  When the field name is not found
	 * @return                   an boolean value
	 */
	public static boolean getBoolean(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		return n.asBoolean();
	}

	/**
	 * Get double value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an double value
	 */
	public static double getDouble(JsonNode node, String fieldName, double defaultValue)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			return defaultValue;
		return n.asDouble();
	}

	/**
	 * Get double value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @throws MissingFieldException  When the field name is not found
	 * @return                   an double value
	 */
	public static double getDouble(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		return n.asDouble();
	}

	/**
	 * Get Timestamp value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an Timestamp value
	 */
	public static Timestamp getTimestampAny(Timestamp defaultValue, JsonNode node, String... fieldNames)
//	throws MissingFieldException
	{
		for (String entry : fieldNames)
		{
			if ( ! node.has(entry) )
				continue;

			JsonNode n = node.get(entry);
			if (n.isNull())
				return defaultValue;

			try
			{
				return getTimestamp(node, entry);
			}
			catch (Exception e) 
			{
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Get Timestamp value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @param defaultValue       Default value if field is not found
	 * @return                   an Timestamp value
	 */
	public static Timestamp getTimestamp(JsonNode node, String fieldName, Timestamp defaultValue)
	throws MissingFieldException
	{
		try
		{
			return getTimestamp(node, fieldName);
		}
		catch (Exception e) 
		{
			return defaultValue;
		}
	}

	/**
	 * Get Timestamp value from a node
	 * @param node               The node
	 * @param fieldName          The fields name
	 * @throws MissingFieldException  When the field name is not found
	 * @return                   an Timestamp value
	 */
	public static Timestamp getTimestamp(JsonNode node, String fieldName)
	throws MissingFieldException
	{
		JsonNode n = node.get(fieldName);
		if (n == null)
			throw new MissingFieldException("Expecting field '"+fieldName+"' which was not found.");
		
		if (n.isNull())
			return null;
		
		String str = n.asText();
		Timestamp ts = null;
		Exception ex = null;

		// if String(iso-8601), then convert it into a Timestamp 
		try 
		{ 
			ts = TimeUtils.parseToTimestampIso8601(str); 
			return ts;
		}
		catch (ParseException pe) 
		{ 
			ex = pe;
		}

		// if String(yyyy-MM-dd hh:mm:ss.SSS), then convert it into a Timestamp 
		try 
		{ 
			ts = TimeUtils.parseToTimestamp(str); 
			return ts;
		}
		catch (ParseException pe) 
		{ 
			ex = pe;
		}

		// if Long value, then convert it into a Timestamp
		try 
		{ 
			long time = Long.parseLong(str);
			return new Timestamp(time);
		}
		catch (NumberFormatException nfe) 
		{ 
			ex = nfe;
		}
		

		throw new MissingFieldException("Problems parsing the Timestamp for field '"+fieldName+"' using value '"+str+"'. Caught: "+ex, ex); 
	}

	/**
	 * create a Object based on what type the desired field is of
	 * @param node               The node
	 */
	public static Object createObjectFromNodeType(JsonNode node)
	{
		if (node == null)
			return null;

		if (node.isTextual()) return node.asText();
		if (node.isInt())     return node.asInt();
		if (node.isBoolean()) return node.asBoolean();

		if (node.isLong())       return node.asLong();
		if (node.isDouble())     return node.asDouble();
		if (node.isShort())      return Short.valueOf(node.asText());
		if (node.isFloat())      return Float.valueOf(node.asText());
		if (node.isBigDecimal()) return new BigDecimal(node.asText());
		if (node.isBigInteger()) return new BigInteger(node.asText());
//		if (node.isBinary())     return node.as);

		return null;
	}
	
	/**
	 * Parse out multiple JSON Strings from the input value, and return them as separate strings in a list.
	 * <p>
	 * This extract every string "chunk" between curly braces: {...}<br>
	 * It may be JSON or "anything else
	 * <p>
	 * <ul>
	 *    <li>End Curly braces inside double quotes is ignored</li>
	 *    <li>Escaped Curly braces is ignored</li>
	 *    <li>Escaped Double Quotes is ignored</li>
	 * </ul>
	 * 
	 *
	 * @param input    The text to extract JSON information from. 
	 * @return         A List of JSON text "chunks" that was extracted!
	 */
	// This was copied from: https://stackoverflow.com/questions/40991519/extract-json-string-from-mixed-string-with-java
	// FIXED: It doesn't look that efficient, but lets fix that later... (I switched temp from 'String' to 'StringBuilder', which made it **much** faster)
	// FIXED: It probably do NOT handle escaped '{' like >>>{"name":"some \} embedded string"}<<<
	// FIXED: It probably do NOT handle '{' within Strings like >>>{"name":"some } embedded string"}<<<
	public static List<String> getJsonObjectsFromString(String input)
	{
		List<Character> stack = new ArrayList<Character>();
		List<String>    jsons = new ArrayList<String>();
		StringBuilder   temp  = new StringBuilder();

		boolean inQuote = false; // toggled every time we see " (double quote char)
		char prevChar = ' ';

		// For debug
		int pos=0;
		int row=1;
		int col=0;

		for (char thisChar : input.toCharArray())
		{
			if (thisChar == '\n') { row++; col=0; }
			pos++;
			col++;
//System.out.println("row=" + row + ", col=" + col + ", pos=" + pos + ", char=|" + (thisChar=='\n'?"<<<\\n>>>":thisChar) + "|, inQuote=" + inQuote + ", temp.length=" + temp.length() + ", stack.size="+stack.size());

			if ( stack.isEmpty() && thisChar == '{' )
			{
//System.out.println(">>>--->>> row=" + row + ", col=" + col + ", pos=" + pos + ", char=|" + thisChar + "|inQuote=" + inQuote + ", , FOUND START {... stack.size=" + stack.size());
				stack.add(thisChar);
				temp.append(thisChar);
			}
			else if ( ! stack.isEmpty() )
			{
				// add char to buffer
				temp.append(thisChar);

				// Flip "inQuote" 
				if (thisChar == '"')
				{
					if ( prevChar != '\\') // If not escaped
					{
						inQuote = ! inQuote;
					}
				}
				
				if ( ! inQuote )
				{
					if ( stack.get(stack.size() - 1).equals('{') && thisChar == '}')
					{
						if (prevChar == '\\' || inQuote)
						{
							// Do nothing
						}
						else
						{
							stack.remove(stack.size() - 1);
//System.out.println("   CLOSE '}' row=" + row + ", col=" + col + ", pos=" + pos + ", char=|" + thisChar + "|, inQuote=" + inQuote + ", stack.size=" + stack.size());
							if ( stack.isEmpty() )
							{
								jsons.add(temp.toString());
//System.out.println("<<<---<<< row=" + row + ", col=" + col + ", pos=" + pos + ", char=|" + thisChar + "|, inQuote=" + inQuote + ", FOUND END JSON jsonList.size=" + jsons.size() + ", strSize=" + temp.toString().length());
								temp = new StringBuilder();
							}
						}
					}
					else if ( thisChar == '{' || thisChar == '}' )
					{
						stack.add(thisChar);
//System.out.println("   START-OR-CLOSE '" + thisChar + "' row=" + row + ", col=" + col + ", pos=" + pos + ", char=|" + thisChar + "|, inQuote=" + inQuote + ", stack.size=" + stack.size());
					}
				} //end: !inQuote
			}
			else if ( temp.length() > 0 && stack.isEmpty() )
			{
//System.out.println("---END OBJ... row=" + row + ", col=" + col + ", pos=" + pos + ", char=|" + thisChar + "|, FOUND END JSON jsonList.size=" + jsons.size() + ", strSize=" + temp.toString().length());
				jsons.add(temp.toString());
				temp = new StringBuilder();
			}
			prevChar = thisChar;
		}
//		for (String json : jsons)
//		{
//			System.out.println("#########################################");
//			System.out.println(json);
//		}

		return jsons;
	}

//	private static void test(boolean replaceHashWithQuotes, String input, String[] expected)
//	{
//		if (replaceHashWithQuotes)
//		{
//			input = input.replace('#', '"');
//
//			for (int i=0; i<expected.length; i++)
//				expected[i] = expected[i].replace('#', '"');
//		}
//		
//		List<String> outputList = getJsonFromString(input);
//		if (outputList.size() != expected.length)
//		{
//			System.out.println("WRONG SIZE: expectedSize=" + expected.length + ", actual=" + outputList.size() );
//			return;
//		}
//		
//		for (int i=0; i<expected.length; i++)
//		{
//			if ( ! expected[i].equals(outputList.get(i)) )
//			{
//				System.out.println("WRONG VALUE: expectedVal=|" + expected[i] + "|, actual=|" + outputList.get(i) + "|");
//				return;
//			}
//		}
//		
//		System.out.println("SUCCESS: size()=" + outputList.size());
//		System.out.println("         input     =|" + input + "|.");
//		System.out.println("         outputList=" + StringUtil.toCommaStrQuoted('|', outputList));
//		
//	}
//	
	public static void main(String[] args)
	{
//		System.out.println("--------------------------");
//		test(true, "{#name#:#value#}"                                                   , new String[] {"{#name#:#value#}"});
//		test(true, "{#name#:#val\\#ue#}"                                                , new String[] {"{#name#:#val\\#ue#}"});
//		test(true, "-123-{#name#:#value#}-456-{aaa=bbb}-321-"                           , new String[] {"{#name#:#value#}"                           , "{aaa=bbb}"});
//		test(true, "-123-{#name#:#some } embedded in a string#}-456-{aaa=bbb}-321-"     , new String[] {"{#name#:#some } embedded in a string#}"     , "{aaa=bbb}"});
//		test(true, "-123-{#name#:#some escaped \\} embedded string#}-456-{aaa=bbb}-321-", new String[] {"{#name#:#some escaped \\} embedded string#}", "{aaa=bbb}"});
//		test(true, "-123-{#name#:some escaped \\} right curlyBrace}-456-{aaa=bbb}-321-" , new String[] {"{#name#:some escaped \\} right curlyBrace}" , "{aaa=bbb}"});

		try
		{
			String content = FileUtils.readFile("C:\\tmp\\QQQQQQQQQQQQQQQQQQQQQQ.json", StandardCharsets.UTF_8.toString());
//			System.out.println("Content=|"+content+"|.");
			System.out.println("Content=|"+content.length()+"| bytes.");
			System.out.println("Content=|"+content.length()/1024+"| KB.");

			long startTime = System.currentTimeMillis();
			List<String> list = getJsonObjectsFromString(content);
			System.out.println("ExecTimeMs=" + TimeUtils.msDiffNow(startTime) + ", for list.size()=" + list.size());

			for (String str : list)
			{
				System.out.println("Length=" + str.length());
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
