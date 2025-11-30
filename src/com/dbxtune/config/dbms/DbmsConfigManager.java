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
package com.dbxtune.config.dbms;

import java.util.LinkedHashMap;
import java.util.Map;


public class DbmsConfigManager
{
	/** Instance variable */
	private static IDbmsConfig _instance = null;

	private static Map<String, IDbmsConfigText> _textInstances = new LinkedHashMap<String, IDbmsConfigText>();
	
	/** check if we got an instance or not */
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	/** Get a instance of the class */
	public static IDbmsConfig getInstance()
	{
		return _instance;
	}

	/** Get a instance of the class */
	public static void setInstance(IDbmsConfig dbmsConfig)
	{
		_instance = dbmsConfig;
	}
}
