/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.pcs.report.senders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;
import com.asetune.utils.JsonUtils;
import com.asetune.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MailHelper
{
	private static Logger _logger = Logger.getLogger(ReportSenderAbstract.class);

	/**
	 * Helper method to get mail addresses
	 * 
	 * @param serverName
	 * @param propKeyTo
	 * @param defaultTo
	 * @return
	 */
	public static List<String> getMailToAddressForServerNameAsList(String serverName, String propKeyTo, String defaultTo)
	{
		String toStr = getMailToAddressForServerName(serverName, propKeyTo, defaultTo);
		return StringUtil.commaStrToList(toStr, true);
	}

	/**
	 * Helper method to get mail addresses
	 * 
	 * @param serverName
	 * @param propKeyTo
	 * @param defaultTo
	 * @return
	 */
	public static String getMailToAddressForServerName(String serverName, String propKeyTo, String defaultTo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		// If the "to" sender is of JSON Content, then we need to parse the JSON and see if any of the entries is enabled for this serverName  
//		String toStr = conf.getProperty(PROPKEY_to, DEFAULT_to);
		String toStr = conf.getProperty(propKeyTo, defaultTo);
		
		if (_logger.isDebugEnabled())
			_logger.debug("getMailToAddressForServerName('"+serverName+"'): "+propKeyTo+"=|"+toStr+"|");

		// Exit early if not properly configured.
		if (StringUtil.isNullOrBlank(toStr))
		{
			_logger.warn("getMailToAddressForServerName('"+serverName+"'): "+propKeyTo+"=|"+toStr+"| is EMPTY... This is a Mandatory property value.");
			return "";
		}

		
//		if (JsonUtils.isPossibleJson(toStr))
		if (JsonUtils.isJsonValid(toStr))
		{
			String jsonStr = toStr;
			int jsonArrayLoopCount = 0;

			try
			{
				JsonArray jsonArr = new JsonParser().parse(jsonStr).getAsJsonArray();
				for (JsonElement jsonElement : jsonArr)
				{
					jsonArrayLoopCount++;
					JsonObject jsonObj = jsonElement.getAsJsonObject();
					
					if (_logger.isDebugEnabled())
						_logger.debug("Checking serverName='" + serverName + "' against the JSON entry: " + jsonObj );

					if (jsonObj.has("serverName") && jsonObj.has("to"))
					{
						String entryServerName = jsonObj.get("serverName").getAsString();
						String entryTo         = jsonObj.get("to"        ).getAsString();
						
						if (_logger.isDebugEnabled())
							_logger.debug("Checking serverName='" + serverName + "' against the JSON entry with: serverNameRegExp='" + entryServerName + "', toStr='" + entryTo + "'.");

						if (StringUtil.hasValue(entryServerName) && StringUtil.hasValue(entryTo))
						{
							// USE REGEXP to check if it matches
							if (serverName.matches(entryServerName))
							{
								if (_logger.isDebugEnabled())
									_logger.debug("MATCH: using mail address to='" + entryTo + "' for serverName='" + serverName + "'.");

								return entryTo;
							}
						}
					}
					else
					{
						_logger.info("getMailToAddressForServerName('"+serverName+"'): Skipping JSON entry '" + jsonObj + "', it dosn't contain members: 'serverName' and 'to'.");
					}
				}
				if (jsonArrayLoopCount == 0)
				{
					_logger.warn("getMailToAddressForServerName('"+serverName+"'): NO JSON Array was found in JSON String '" + jsonStr + "', Skipping this and returning ''. for property '" + propKeyTo + "'.");
					return "";
				}

				_logger.info("getMailToAddressForServerName('"+serverName+"'): No matching entry was found for serverName '" + serverName + "' in JSON '" + jsonStr + "' using propert '" + propKeyTo + "'. Returning ''(blank) as the email recipiant.");
				return "";
			}
			catch(Exception ex)
			{
				_logger.error("getMailToAddressForServerName('"+serverName+"'): Trying to parse the JSON Array String '" + toStr + "', Caught: " + ex, ex);
				return "";
			}
		}
		else
		{
			_logger.debug("getMailToAddressForServerName('"+serverName+"'): NOT a JSON Array, using as 'plain-email-address': "+propKeyTo+"=|"+toStr+"|");
			return toStr;
		}
	}

	/**
	 * Get all mail addresses from entries of the form:
	 * <ul>
	 *     <li><code>email@acme.com</code></li> 
	 *     <li>Or as a JSON String, of the form:<br><code>[ {"serverName":"xxx", "to":"user1@acme.com, user2@acme.com"}, {"serverName":"yyy", "to":"user1@acme.com"}, {"serverName":"zzz", "to":"user3@acme.com"} ]</code></li> 
	 * </ul>
	 *     form: 'email@acme.com' 
	 *     or '[ {"serverName":"xxx", "to":"user1@acme.com, user2@acme.com"}, {"serverName":"yyy", "to":"user1@acme.com"}, {"serverName":"zzz", "to":"user3@acme.com"} ]' 
	 * @param toInputList
	 * @return a list of email addresses or an empty list (never null)
	 */
	public static List<String> getAllMailToAddress(String csvOrJson)
	{
		Set<String> set = new LinkedHashSet<>();

		if (StringUtil.isNullOrBlank(csvOrJson))
			return Collections.emptyList();

//		if (JsonUtils.isPossibleJson(csvOrJson))
		if (JsonUtils.isJsonValid(csvOrJson))
		{
			try
			{
				JsonArray jsonArr = new JsonParser().parse(csvOrJson).getAsJsonArray();
				for (JsonElement jsonElement : jsonArr)
				{
					JsonObject jsonObj = jsonElement.getAsJsonObject();
					
					if (_logger.isDebugEnabled())
						_logger.debug("getAllMailToAddress(): Parsed JSON entry: " + jsonObj );

					if (jsonObj.has("serverName") && jsonObj.has("to"))
					{
						String entryServerName = jsonObj.get("serverName").getAsString();
						String entryTo         = jsonObj.get("to"        ).getAsString();
						
						if (_logger.isDebugEnabled())
							_logger.debug("getAllMailToAddress(): JSON entry with: serverNameRegExp='" + entryServerName + "', toStr='" + entryTo + "'.");

						if (StringUtil.hasValue(entryTo))
						{
							for(String mailAddress : StringUtil.parseCommaStrToSet(entryTo))
								set.add(mailAddress);
						}
					}
					else
					{
						_logger.info("Skipping JSON entry '" + jsonObj + "', it dosn't contain members: 'serverName' and 'to'.");
					}
				}
			}
			catch(Exception ex)
			{
				_logger.error("getAllMailToAddress(): Trying to parse the JSON Array String '" + csvOrJson + "', Caught: " + ex, ex);
			}
		}
		else
		{
			_logger.debug("getAllMailToAddress(): csvOrJson='" + csvOrJson + "', NOT a JSON Array, using as 'plain-email-address' or a CSV of addresses.");

			for(String mailAddress : StringUtil.parseCommaStrToSet(csvOrJson))
				set.add(mailAddress);
		}

		// Return a List of the unique mail addresses
		return new ArrayList<>(set);
	}

}
