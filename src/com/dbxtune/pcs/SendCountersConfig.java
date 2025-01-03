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
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

//------------------------------------------------------------------------------
// PRIVATE HELPER CLASS
//------------------------------------------------------------------------------
public class SendCountersConfig
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private boolean             _sendAll = false;
	private Map<String, String> _includeMap;
	private Set<String>         _excludeSet;

	@Override
	public String toString()
	{
//		return super.toString() + ": sendAll=" + _sendAll + ", includeMap=" + _includeMap + ", excludeSet=" + _excludeSet;
		return "sendAll=" + _sendAll + ", includeMap=" + _includeMap + ", excludeSet=" + _excludeSet;
	}
	
	public enum FilterType 
	{
		ABS, 
		DIFF, 
		RATE, 
//		META_DATA, 
		META_DATA_JDBC, 
		META_DATA_CM
	};

	public SendCountersConfig(boolean sendAll)
	{
		_sendAll = sendAll;
	}

	public SendCountersConfig(Configuration conf, String propName, String defaultValue)
	throws Exception
	{
		if (conf == null)
			conf = Configuration.getCombinedConfiguration();
			
conf = Configuration.getCombinedConfiguration();
		String propVal = conf.getProperty(propName, defaultValue);

		// parse the CSV Property into a temporary map
		Map<String, String> tmpMap = StringUtil.parseCommaStrToMap(propVal);
//System.out.println(">>>> SendCountersConfig(propName=|"+propName+"|, propVal=|"+propVal+"|): tmpMap="+tmpMap);

		if (tmpMap.isEmpty())
		{
			_sendAll = false;
		}
		else
		{
			// loop ALL entries, add them to _map
			for (Entry<String, String> entry : tmpMap.entrySet())
			{
				String key = entry.getKey();
				String val = entry.getValue();
				
				// Look for "", "none"
				if ( "".equalsIgnoreCase(key) || "none".equalsIgnoreCase(key) )
				{
					_sendAll = false;
				}
				// Look for "*", "all"
				else if ( "*".equalsIgnoreCase(key) || "all".equalsIgnoreCase(key) )
				{
					_sendAll = true;
					// or possibly ADD ALL entries from: CounterController.getInstance().getCmList();
				}
				else
				{
					// Check for "negative" or "exclusion" entry
					boolean exlusionEntry = false;
					if (key.startsWith("!"))
					{
						exlusionEntry = true;
						key = key.substring(1).trim(); // remove the "!"
					}

					if ("true".equalsIgnoreCase(val))
						val = "adrc"; // By default do not send type 'j' jdbcMetaData

					if ("false".equalsIgnoreCase(val))
						exlusionEntry = true;

					String cmName = key;

					// Check if the name exists (if any CM exists with that name)
					CountersModel cm = CounterController.getInstance().getCmByName(cmName);
					if (cm == null)
					{
						_logger.error("JSON-SendCountersConfig: The CounterModel '" + cmName + "' does not exists. This entry will be discarded!");
						continue;
					}

					if (exlusionEntry)
					{
						if (_excludeSet == null)
							_excludeSet = new LinkedHashSet<>();

						_excludeSet.add(cmName);
					}
					else
					{
						// Do only allow 'adrjc' chars... 
						// * a=AbsCounters
						// * d=DiffCounters
						// * r=RateCounters 
						// -old-* m=JdbcMetaData 
						// * j=JdbcMetaData 
						// * c=CounterModelMetaData 
						String addType = "";
						if (StringUtil.hasValue(val))
						{
							String unsupportedChar = "";
							for (int i=0; i<val.length(); i++)
							{
								char ch = val.charAt(i);
								if (ch == 'a' || ch == 'A' || ch == 'd' || ch == 'D' || ch == 'r' || ch == 'R' || ch == 'j' || ch == 'J' || ch == 'c' || ch == 'C')
								{
									addType += Character.toLowerCase(ch);
								}
								else if (ch == 'm' || ch == 'M') // for backward compatibility ('m' is no more supported, and you should use 'j' or 'c')
								{
									_logger.warn("JSON-SendCountersConfig: Depricated value for property '" + key + "'. Found depricated char(s) 'm'. Plase use: 'j'=jdbcMetaData and/or 'c'=counterModelMetaData. For now 'm' is translated into 'c' as backward compatibility.");
									addType += 'c'; // if 'm' (metadata) is specified, change it into the new part 'c' (CounterModelMetaData), but leave exclude 'j' (JdbcMetaData)
								}
								else
								{
									unsupportedChar += ch;
									_logger.error("JSON-SendCountersConfig: Inproper value for property '" + key + "'. Found unsupported char(s) '" + unsupportedChar + "', which simply will be skipped. Accepted values are 'adrjc' where: 'a'=AbsoluteCounters, 'd'=DiffCounters, 'r'=RateCounters, 'j'=jdbcMetaData, 'c'=counterModelMetaData");
								}
							}
						}

						// Create the mpa if it doesnt exist
						if (_includeMap == null)
							_includeMap = new LinkedHashMap<>();

						// finally ADD the entry to the _includeMap
						_includeMap.put(cmName, addType);
					}
				} // end: CM Entry 
			} // end: loop tmpMap
		} // end: tmpMap has-values

		// Should we have a second way to enable this via another property (that starts with the CM-NAME)
		// Check for CM Properties, named: <CmName>.PersistWriterToHttpJson.send.{counters|graphs}
		boolean checkEnableViaCmName = true;
		if (checkEnableViaCmName)
		{
			for (CountersModel cm : CounterController.getInstance().getCmList())
			{
//				public static final String  PROPKEY_sendCounters      = "PersistWriterToHttpJson.{KEY}.send.counters";
//				public static final String  PROPKEY_sendGraphs        = "PersistWriterToHttpJson.{KEY}.send.graphs";
				
				String cmName = cm.getName();
				String key = null;
				if (propName.endsWith(".send.counters")) key = cmName + ".PersistWriterToHttpJson.send.counters";
				if (propName.endsWith(".send.graphs"  )) key = cmName + ".PersistWriterToHttpJson.send.graphs";

//SOMEWHERE; CmActiveStatements should not be hardcoded...
//SOMEWHERE; also add property to JSON "cmSendCountersList" or similar

				String val = conf.getProperty(key);
				if (val != null)
				{
					// do work
					// - check if the CM is EXPLICITLY defined, then do NOT override!

					boolean doApply = true;
					if (_excludeSet != null && _excludeSet.contains(cmName))    doApply = false;
					if (_includeMap != null && _includeMap.containsKey(cmName)) doApply = false;

					if ( ! doApply )
					{
						_logger.info("JSON-SendCountersConfig: NOT Applying setting from property '" + key + "', value '" + val + "'. This due to a higer priority setting '" + propName + "', value '" + propVal + "' has already been applied.");
					}
					else
					{
						boolean exlusionEntry = false;

						if ("true".equalsIgnoreCase(val))
							val = "adrc"; // By default do not send type 'j' jdbcMetaData

						if ("false".equalsIgnoreCase(val))
							exlusionEntry = true;

						if (exlusionEntry)
						{
							if (_excludeSet == null)
								_excludeSet = new LinkedHashSet<>();

							_excludeSet.add(cmName);
						}
						else
						{
							// Do only allow 'adrm' chars... 
							// * a=AbsCounters
							// * d=DiffCounters
							// * r=RateCounters 
							// -old-* m=JdbcMetaData 
							// * j=JdbcMetaData 
							// * c=CounterModelMetaData 
							String addType = "";
							if (StringUtil.hasValue(val))
							{
								String unsupportedChar = "";
								for (int i=0; i<val.length(); i++)
								{
									char ch = val.charAt(i);
									if (ch == 'a' || ch == 'A' || ch == 'd' || ch == 'D' || ch == 'r' || ch == 'R' || ch == 'j' || ch == 'J' || ch == 'c' || ch == 'C')
									{
										addType += Character.toLowerCase(ch);
									}
									else if (ch == 'm' || ch == 'M') // for backward compatibility ('m' is no more supported, and you should use 'j' or 'c')
									{
										_logger.warn("JSON-SendCountersConfig: Depricated value for property '" + key + "'. Found depricated char(s) 'm'. Plase use: 'j'=jdbcMetaData and/or 'c'=counterModelMetaData. For now 'm' is translated into 'c' as backward compatibility.");
										addType += 'c'; // if 'm' (metadata) is specified, change it into the new part 'c' (CounterModelMetaData), but leave exclude 'j' (JdbcMetaData)
									}
									else
									{
										unsupportedChar += ch;
										_logger.error("JSON-SendCountersConfig: Inproper value for property '" + key + "'. Found unsupported char(s) '" + unsupportedChar + "', which simply will be skipped. Accepted values are 'adrjc' where: 'a'=AbsoluteCounters, 'd'=DiffCounters, 'r'=RateCounters, 'j'=jdbcMetaData, 'c'=counterModelMetaData");
									}
								}
							}

							// Create the Map if it doesn't exist
							if (_includeMap == null)
								_includeMap = new LinkedHashMap<>();

							// finally ADD the entry to the _includeMap
							_includeMap.put(cmName, addType);
						}
					} // end: doApply
				} // end: hasValue
			} // end: loop CmList	
		} // end: checkEnableViaCmName

	} // end: constructor

	public boolean isSendAll() 
	{ 
		return _sendAll; 
	}

	public boolean isEnabled            (String name) { return isEnabled(name, null); }
//	public boolean isMetaDataEnabled    (String name) { return isEnabled(name, FilterType.META_DATA); }
	public boolean isMetaDataEnabled    (String name) { return isMetaDataJdbcEnabled(name) || isMetaDataCmEnabled(name); }
	public boolean isMetaDataJdbcEnabled(String name) { return isEnabled(name, FilterType.META_DATA_JDBC); }
	public boolean isMetaDataCmEnabled  (String name) { return isEnabled(name, FilterType.META_DATA_CM); }
	public boolean isAbsEnabled         (String name) { return isEnabled(name, FilterType.ABS); }
	public boolean isDiffEnabled        (String name) { return isEnabled(name, FilterType.DIFF); }
	public boolean isRateEnabled        (String name) { return isEnabled(name, FilterType.RATE); }

	private boolean isEnabled(String name, FilterType type) 
	{
//		if (type == null)      throw new IllegalArgumentException("type can't be null.");
//		if (type.length() > 1) throw new IllegalArgumentException("type can't be more than 1 character, you passed '" + type + "'.");
//		if ( ! ("a".equalsIgnoreCase(type) || "d".equalsIgnoreCase(type) || "r".equalsIgnoreCase(type)) )
//			throw new IllegalArgumentException("type Can only be 'a', 'd' or 'r'. you passed '" + type + "'.");

		if (isSendAll())
			return true;

		if (_excludeSet != null && _excludeSet.contains(name))
		{
			return false;
		}

		if (_includeMap != null && _includeMap.containsKey(name))
		{
			String adr = _includeMap.get(name);
			if (StringUtil.isNullOrBlank(adr))
				return true;

			if (type == null)
				return true;
				
			String typeStr = "";
			if (FilterType.ABS           .equals(type)) typeStr = "a";
			if (FilterType.DIFF          .equals(type)) typeStr = "d";
			if (FilterType.RATE          .equals(type)) typeStr = "r";
//			if (FilterType.META_DATA     .equals(type)) typeStr = "m";
			if (FilterType.META_DATA_JDBC.equals(type)) typeStr = "j";
			if (FilterType.META_DATA_CM  .equals(type)) typeStr = "c";

			return adr.contains(typeStr);
		}
		else
		{
			return false;
		}
	}
}
