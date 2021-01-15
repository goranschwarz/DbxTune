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
package com.asetune.hostmon;

import com.asetune.utils.Configuration;

public class MonitorNwInfoWindows
extends MonitorNwInfo
{
	public MonitorNwInfoWindows()
	{
		this(-1, null);
	}
	public MonitorNwInfoWindows(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return this.getClass().getSimpleName();
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
//		return cmd != null ? cmd : "typeperf -si " + getSleepTime() + " \"\\Network Interface(*)\\*\" \"\\Network Adapter(*)\\*\"";
		return cmd != null ? cmd : "typeperf -si " + getSleepTime() + " \"\\Network Interface(*)\\*\" ";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		// Special MetaData for Perf Counters...
		HostMonitorMetaData md = new HostMonitorMetaDataWindowsTypePerf();

		md.setTableName(getModuleName());

		// It's a *streaming* command
		md.setOsCommandStreaming(true);

		// The initialization on column names etc are done in of first row read, which holds all column names, its a CSV header
		md.setInitializeUsingFirstRow(true);
		
		return md;
	}
}
