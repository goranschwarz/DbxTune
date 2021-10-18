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
package com.asetune.utils;

import java.util.HashMap;

public class MovingAverageCounterManager
{
	//---------------------------------------------------
	// BEGIN: Instance variables and methods
	//---------------------------------------------------
	private static HashMap<String, MovingAverageCounter> _map = new HashMap<>(); 

	public static MovingAverageCounter getInstance(String name, int minutes)
	{
		String key = MovingAverageCounter.createName(name, minutes);
		
		// Get object, if not found create one
		MovingAverageCounter val = _map.get(key);
		if (val == null)
		{
			val = new MovingAverageCounter(name, minutes);
			_map.put(key, val);
		}
		return val;
	}
	//---------------------------------------------------
	// END: Instance variables and methods
	//---------------------------------------------------
	
	
//	//---------------------------------------------------
//	// BEGIN: class variables and methods
//	//---------------------------------------------------
//	public static class MovingAverageCounterEntry
//	{
//		private String _name;
//		private int    _minutes;
//
//		public MovingAverageCounterEntry(String name, int minutes)
//		{
//			_name    = name;
//			_minutes = minutes;
//		}
//	}
//
//	
//	//---------------------------------------------------
//	// END: class variables and methods
//	//---------------------------------------------------
}
