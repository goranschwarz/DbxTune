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
package com.dbxtune.hostmon;

import java.util.HashMap;
import java.util.Map;

import com.dbxtune.hostmon.WindowsTypePerfCsvReader.CounterColumnRewrite;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class MonitorMeminfoWindows
extends MonitorIo
{
	public MonitorMeminfoWindows()
	{
		this(-1, null);
	}
	public MonitorMeminfoWindows(int utilVersion, String utilExtraInfo)
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
		if (StringUtil.hasValue(cmd))
			return cmd;
		
		return "typeperf -si " + getSleepTime() + " \"\\Paging File(_Total)\\*\" \"\\Memory\\*\" ";
//		return "typeperf -si " + getSleepTime() + " \"\\Memory\\*\" ";
	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		// Special MetaData for Perf Counters...
		HostMonitorMetaDataWindowsTypePerf md = new HostMonitorMetaDataWindowsTypePerf();

		md.setTableName(getModuleName());

		// It's a *streaming* command
		md.setOsCommandStreaming(true);

		// The initialization on column names etc are done in of first row read, which holds all column names, its a CSV header
		md.setInitializeUsingFirstRow(true);
		
		// "merge" in the "Page File(_Total)" counters into the "Memory" instance
		Map<CounterColumnRewrite, CounterColumnRewrite> rewriteRules = new HashMap<>();
		rewriteRules.put(new CounterColumnRewrite("Paging File(_Total)", "% Usage")     , new CounterColumnRewrite("Memory", "Paging File(_Total) - % Usage"));
		rewriteRules.put(new CounterColumnRewrite("Paging File(_Total)", "% Usage Peak"), new CounterColumnRewrite("Memory", "Paging File(_Total) - % Usage Peak"));
		
		md.setInitializeUsingFirstRowRewriteRules(rewriteRules);
		
		return md;
	}
}
