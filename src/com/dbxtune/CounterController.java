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
package com.dbxtune;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ISummaryPanel;

public class CounterController 
//implements ICounterController
{
	public static final String PROPKEY_startRecordingAtTime = "offline.startRecordingAtTime";

	private static ICounterController _instance = null;

	/** 
	 * have we got set a singleton object to be used. (set with setInstance() ) 
	 */
	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	/** 
	 * Get the singleton object
	 * @throws RuntimeException if no singleton has been set. set with setInstance(), check with hasInstance() 
	 */
	public static ICounterController getInstance()
	{
		if (_instance == null)
			throw new RuntimeException("No CounterController instance exists.");
		return _instance;
	}

	/**
	 * Set a specific GetCounter object to be used as the singleton.
	 * @param cnt
	 */
	public static void setInstance(ICounterController counterController)
	{
		_instance = counterController;
	}

	public static CountersModel getSummaryCm()
	{
		return getInstance().getSummaryCm();
	}

	public static String getSummaryCmName()
	{
		return getInstance().getSummaryCmName();
	}

	public static ISummaryPanel getSummaryPanel()
	{
		return getInstance().getSummaryPanel();
	}

//	@Override
//	public void addCm(CountersModel cm)
//	{
//		getInstance().addCm(cm);
//	}
//
//	@Override
//	public void setMonConnection(Connection conn)
//	{
//		getInstance().setMonConnection(conn);
//	}
//
//	@Override
//	public Connection getMonConnection()
//	{
//	}
//
//	@Override
//	public boolean isMonConnected()
//	{
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public boolean isMonConnected(boolean forceConnectionCheck, boolean closeConnOnFailure)
//	{
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public void closeMonConnection()
//	{
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public void cleanupMonConnection()
//	{
//		// TODO Auto-generated method stub
//
//	}
//
//	@Override
//	public CountersModel getCmByName(String name)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public CountersModel getCmByDisplayName(String name)
//	{
//		// TODO Auto-generated method stub
//		return null;
//	}

}
