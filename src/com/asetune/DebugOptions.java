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
package com.asetune;

import com.asetune.utils.Debug;

public class DebugOptions
{
	public static String EDT_HANG = "EDT_HANG";

	private static boolean _doneInit = false;

	/**
	 * Register all known DEBUG options in the Debug object.
	 */
	public static void init()
	{
		if (_doneInit)
			return;

		Debug.addKnownDebug(EDT_HANG, "Install a Swing EDT (Event Dispatch Thread) - hook, that check for deadlocks, traces task that takes to long to execute by the Swing Event Dispatch Thread.");
		
		_doneInit = true;
	}

	static
	{
		init();
	}
}
