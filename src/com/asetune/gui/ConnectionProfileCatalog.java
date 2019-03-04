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
package com.asetune.gui;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;

import com.asetune.Version;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class ConnectionProfileCatalog
{
	private String _name;
//	private boolean _expanded;

	// A map containing which product name the expanded state is valid for. <ProductNameString, true|false>
	private Map<String, Boolean> _expandedMap = new LinkedHashMap<>();

	
	public static final ImageIcon ICON = SwingUtils.readImageIcon(Version.class, "images/conn_profile_directory_eclipse_16.png");

	public ConnectionProfileCatalog(String name, boolean expanded)
	{
		_name = name;
		setExpanded(expanded);
	}

	public ConnectionProfileCatalog(String name, String expandedMapStr)
	{
		_name = name;
		_expandedMap = parseExpandedMapFromStr(expandedMapStr);
		
		if (_expandedMap == null)
			_expandedMap = new LinkedHashMap<>();
	}

	public void setName(String name)
	{
		_name = name;
	}

	public String getName()
	{
		return _name;
	}
	
	public ImageIcon getIcon()
	{
		return ICON;
	}

	@Override
	public String toString()
	{
		return getName();
	}

	public boolean isExpanded()
	{
		String key = Version.getAppName();
		
		if (_expandedMap.containsKey(key))
			return _expandedMap.get(key);

		return true;
	}
	
	public void setExpanded(boolean expanded)
	{
		String key = Version.getAppName();
		
		_expandedMap.put(key, expanded);
	}
	
	public String getExpandedMapStr()
	{
		return _expandedMap.toString();
	}

	/**
	 * Parse a Map.stoString() into LinkedHashMap<String, Boolean> String in the form '{key1=true, key2=false}'
	 * 
	 * @param expandEntry   if expanded or not. empty string = true. otherwise it should be "{AseTune=true, RsTune=false, SqlServerTune=false}"
	 * 
	 * @return LinkedHashMap<String, Boolean>
	 */
	private static Map<String, Boolean> parseExpandedMapFromStr(String mapStr)
	{
		if (mapStr == null)
			return new LinkedHashMap<>();

		mapStr = mapStr.trim();
		if (mapStr.startsWith("{") && mapStr.endsWith("}") ) 
		{
			// Remove '{' and '}'
			String tmpStr = mapStr.substring(1);
			tmpStr = tmpStr.substring(0, tmpStr.length()-1);
			
			// use StringUtil.parseCommaStrToMap() to conert it into Map<String, String
			// Then check content and add it to the 'expandedMap'
			Map<String, Boolean> expandedMap = new LinkedHashMap<>();
			for (Entry<String, String> entry : StringUtil.parseCommaStrToMap(tmpStr).entrySet())
			{
				String expandedStr = entry.getValue();
				boolean expanded   = ! expandedStr.equalsIgnoreCase("false"); // ""=true, "true"=true, "unknown"=true, "false"=false, "FALSE"=false

				expandedMap.put(entry.getKey(), expanded);
			}
			return expandedMap;
		}
		else
		{
			// Check if the string looks like "false"
			String  key = Version.getAppName();
			boolean expanded = ! mapStr.equalsIgnoreCase("false"); // ""=true, "true"=true, "unknown"=true, "false"=false, "FALSE"=false
			Map<String, Boolean> expandedMap = new LinkedHashMap<>();

			expandedMap.put(key, expanded);
			
			return expandedMap;
		}
	}
}
