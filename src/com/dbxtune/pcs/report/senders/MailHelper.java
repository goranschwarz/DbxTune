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
package com.dbxtune.pcs.report.senders;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventDummy;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.JsonUtils;
import com.dbxtune.utils.StringUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MailHelper
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static void main(String[] args)
	{
		// Set Log4j Log Level
		Configurator.setRootLevel(Level.TRACE);

		List<String> list;

		// test-1
		System.setProperty("xxx.test.1", "test1@acme.com");
		list = getMailToAddressForServerNameAsList("", null, "xxx.test.1", "test1");
		System.out.println("test.1: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test1@acme.com"})) ? "OK" : "---- FAIL ----");

		
		// test-2
		System.setProperty("xxx.test.2", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test2.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv2#, #to#:#test2.srv2@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", null, "xxx.test.2", "test2");
		System.out.println("test.2: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test2.srv1@json.acme.com"})) ? "OK" : "---- FAIL ----");

		
		// test-3
		AlarmEvent ae = new AlarmEventDummy("test", "test", "test", AlarmEvent.Category.OTHER, AlarmEvent.Severity.INFO, AlarmEvent.ServiceState.UP, 0, null, null, null, 0);
		System.setProperty("xxx.test.3", ("[ "
				+ "{#serverName#:#srv1#, #alarmName#:#Dummy#, #to#:#test3.onlyDummyAlarm.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv1#,                      #to#:#test3.allAlarms.srv1@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", ae, "xxx.test.3", "test3");
		System.out.println("test.3: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test3.onlyDummyAlarm.srv1@json.acme.com", "test3.allAlarms.srv1@json.acme.com"})) ? "OK" : "---- FAIL ----");

		
		// test-4 (regex)
		ae = new AlarmEventDummy("test", "test", "test", AlarmEvent.Category.OTHER, AlarmEvent.Severity.INFO, AlarmEvent.ServiceState.UP, 0, null, null, null, 0);
		System.setProperty("xxx.test.4", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test4.onlyDummyAlarm.srv1@json.acme.com#, #alarmName#:#(Dummy1|Dummy2|Dummy)# }, "
				+ "{#serverName#:#srv1#, #to#:#test4.allAlarms.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv1#, #to#:#test4.noMatch.srv1@json.acme.com#, #alarmName#:#(Dummy1|Dummy2)# } "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", ae, "xxx.test.4", "test4");
		System.out.println("test.4: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test4.onlyDummyAlarm.srv1@json.acme.com", "test4.allAlarms.srv1@json.acme.com"})) ? "OK" : "---- FAIL ----");

		
		// test-5 -- FALLBACK
		System.setProperty("xxx.test.5", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test5.srv1@json.acme.com#}, "
				+ "{#serverName#:#srv2#, #to#:#test5.srv2@json.acme.com#}, "
				+ "{#fallback#:true, #to#:#test5.srvFallback1@json.acme.com, test5.srvFallback2@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("unknownSrv", null, "xxx.test.5", "test5");
		System.out.println("test.5: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test5.srvFallback1@json.acme.com", "test5.srvFallback2@json.acme.com"})) ? "OK" : "---- FAIL ----");


		// test-6 -- simple but with 2 mail addresses (and a 'fallback', which should NOT be used)
		System.setProperty("xxx.test.6", ("[ "
				+ "{#serverName#:#srv1#, #to#:#test6.srv1.m1@json.acme.com, test6.srv1.m2@json.acme.com#}, "
				+ "{#serverName#:#srv2#, #to#:#test6.srv2@json.acme.com#}, "
				+ "{#fallback#:true, #to#:#test6.srvFallback1@json.acme.com#} "
				+ "]").replace('#', '"'));
		list = getMailToAddressForServerNameAsList("srv1", null, "xxx.test.6", "test6");
		System.out.println("test.6: " + list);
		System.out.println( list.equals(Arrays.asList(new String[]{"test6.srv1.m1@json.acme.com", "test6.srv1.m2@json.acme.com"})) ? "OK" : "---- FAIL ----");
	}

	/**
	 * Helper method to get mail addresses
	 * 
	 * @param serverName
	 * @param propKeyTo
	 * @param defaultTo
	 * @return
	 */
	public static List<String> getMailToAddressForServerNameAsList(String serverName, AlarmEvent alarmEvent, String propKeyTo, String defaultTo)
	{
		String toStr = getMailToAddressForServerName(serverName, alarmEvent, propKeyTo, defaultTo);
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
	public static String getMailToAddressForServerName(String serverName, AlarmEvent alarmEvent, String propKeyTo, String defaultTo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		// If the "to" sender is of JSON Content, then we need to parse the JSON and see if any of the entries is enabled for this serverName  
//		String toStr = conf.getProperty(PROPKEY_to, DEFAULT_to);
		String toStr = conf.getProperty(propKeyTo, defaultTo);
		
		if (_logger.isDebugEnabled())
			_logger.debug("getMailToAddressForServerName('" + serverName + "'): " + propKeyTo + "=|" + toStr + "|");

		// Exit early if not properly configured.
		if (StringUtil.isNullOrBlank(toStr))
		{
			throw new RuntimeException("getMailToAddressForServerName('" + serverName + "'): " + propKeyTo + "=|" + toStr + "| is EMPTY... This is a Mandatory property value.");
			//_logger.warn("getMailToAddressForServerName('" + serverName + "'): " + propKeyTo + "=|" + toStr + "| is EMPTY... This is a Mandatory property value.");
			//return "";
		}

		
//		if (JsonUtils.isPossibleJson(toStr))
		if (JsonUtils.isJsonValid(toStr))
		{
			Set<String> returnSet = new LinkedHashSet<>();
			Set<String> fallbackSet = new LinkedHashSet<>();
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
						_logger.debug("json[" + jsonArrayLoopCount + "] Checking step-1: serverName='" + serverName + "' against the JSON entry: " + jsonObj );

					if (jsonObj.has("serverName") && jsonObj.has("to"))
					{
						String entryServerName = jsonObj.get("serverName").getAsString();
						String entryTo         = jsonObj.get("to"        ).getAsString();

						// If we have 'alarmName', then get it
						String entryAlarmName  = !jsonObj.has("alarmName") ? null : jsonObj.get("alarmName").getAsString();
						
						if (_logger.isDebugEnabled())
							_logger.debug("json[" + jsonArrayLoopCount + "] Checking step-2: serverName='" + serverName + "' against the JSON entry with: serverNameRegExp='" + entryServerName + "', alarmNameRegExp='" + entryAlarmName + "', toStr='" + entryTo + "'.");

						if (StringUtil.hasValue(entryServerName) && StringUtil.hasValue(entryTo))
						{
							// USE REGEXP to check if it matches
							if (serverName.matches(entryServerName))
							{
								if (_logger.isDebugEnabled())
									_logger.debug("json[" + jsonArrayLoopCount + "] MATCH-start: using mail address to='" + entryTo + "' for serverName='" + serverName + "'.");

								boolean match = true;
								String alarmName = alarmEvent == null ? "" : alarmEvent.getClass().getSimpleName();
								
								// Check optional restriction 'AlarmName'
								// Which can be long or short name: 'AlarmEvenXxx' or 'Xxx'
								if (StringUtil.hasValue(entryAlarmName) && StringUtil.hasValue(alarmName))
								{
									// Remove any "fullname" prefix
									if (alarmName.startsWith("AlarmEvent"))
										alarmName = alarmName.substring("AlarmEvent".length());

									if (entryAlarmName.contains("AlarmEvent"))
										entryAlarmName = entryAlarmName.replace("AlarmEvent", "");

									if ( ! alarmName.matches(entryAlarmName) )
									{
										if (_logger.isDebugEnabled())
											_logger.debug("json[" + jsonArrayLoopCount + "] DISABLE-MATCH-DUE-TO: NOT MATCHING: alarmName='" + alarmName + "', entryAlarmNameRegEx='" + entryAlarmName + "'.");

										match = false;
									}
								}

								if (match)
								{
									if (_logger.isDebugEnabled())
										_logger.debug("json[" + jsonArrayLoopCount + "] MATCH-end: using mail address to='" + entryTo + "' for serverName='" + serverName + "', alarmName='" + alarmName + "'.");

									returnSet.addAll( StringUtil.parseCommaStrToSet(entryTo) );
								}
								else
								{
									if (_logger.isDebugEnabled())
										_logger.debug("json[" + jsonArrayLoopCount + "] NO-MATCH-end: using mail address to='" + entryTo + "' for serverName='" + serverName + "', alarmName='" + alarmName + "'.");
								}
							}
						}
					}
					else if (jsonObj.has("fallback") && jsonObj.has("to")) // NOTE: This section is new 2023-09-20 and has NOT YET been tested
					{
						String entryFallback   = jsonObj.get("fallback").getAsString();
						String entryTo         = jsonObj.get("to"      ).getAsString();

						if ("true".equalsIgnoreCase(entryFallback))
						{
							if (StringUtil.hasValue(entryTo))
							{
								if (_logger.isDebugEnabled())
									_logger.debug("json[" + jsonArrayLoopCount + "] FALLBACK entry found: adding mail address to='" + entryTo + "'.");

								fallbackSet.addAll( StringUtil.parseCommaStrToSet(entryTo) );
							}
						}
						else
						{
							_logger.info("json[" + jsonArrayLoopCount + "] getMailToAddressForServerName('"+serverName+"'): Skipping 'fallback' entry, since it contains '" + entryFallback + "', which is considdered as 'false'.");
						}
					}
					else
					{
						_logger.info("json[" + jsonArrayLoopCount + "] getMailToAddressForServerName('"+serverName+"'): Skipping JSON entry '" + jsonObj + "', it dosn't contain members: ('serverName' and 'to') or ('fallback' and 'to').");
					}
				} // end: JSON loop objects

				if (jsonArrayLoopCount == 0)
				{
					_logger.warn("getMailToAddressForServerName('"+serverName+"'): NO JSON Array was found in JSON String '" + jsonStr + "', Skipping this and returning ''. for property '" + propKeyTo + "'.");
					return "";
				}
				else if ( ! returnSet.isEmpty() )
				{
					return StringUtil.toCommaStr(returnSet);
				}
				else if ( ! fallbackSet.isEmpty() )
				{
					_logger.info("getMailToAddressForServerName('"+serverName+"'): No matching 'serverName' entry was found for serverName '" + serverName + "' but a 'fallback' entry was found and used. to=" + fallbackSet);
					return StringUtil.toCommaStr(fallbackSet);
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
