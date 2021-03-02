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
package com.asetune.hostmon;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;

public class MonitorVmstatWindows 
extends HostMonitor
{
//	private static Logger _logger = Logger.getLogger(MonitorVmstatWindows.class);

	public MonitorVmstatWindows()
	{
		this(-1, null);
	}
	public MonitorVmstatWindows(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorVmstatWindows";
	}

	@Override
	public String getCommand()
	{
//		String cmd = super.getCommand();
//		return cmd != null ? cmd : "vmstat "+getSleepTime();
		return "echo not-yet-implemented-on-Windows";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfoConf)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn ("Dummy",            1, 1, false, 100,   "Dummy");
                                                 
		md.setOsCommandStreaming(false);
		
		return md;
	}

}
