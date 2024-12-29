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

import com.dbxtune.utils.Configuration;

/**
 * This needs SSH Server on the Windows Server.
 * <p>
 * Install instructions for OpenSSH on Windows Server 2019 & Windows 10:
 * https://docs.microsoft.com/en-us/windows-server/administration/openssh/openssh_install_firstuse
 * <p>
 * And possibly also add a user/login (from DOS)<br>
 * <code>net user /add dbxtune secretPasswd</code>
 * <p>
 * Allow user to get perf counters (from DOS)<br>
 * <code>net localgroup "Performance Log Users" dbxtune /add</code> -- For 'performance counter' access
 * <code>net localgroup "Distributed COM Users" dbxtune /add</code> -- For 'gwmi win32_logicaldisk' access
 * <p>
 * Or simply<br>
 * <code>net localgroup administrators dbxtune /add</code> -- For 'local admin' access
 * <p>
 * Note: The user has to logout and login again for the above 'net localgroup ...' is affected...<br>
 * 
 */
public class MonitorMpstatWindows
extends MonitorMpstat
{
	public MonitorMpstatWindows()
	{
		this(-1, null);
	}
	public MonitorMpstatWindows(int utilVersion, String utilExtraInfo)
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
		return cmd != null ? cmd : "typeperf -si " + getSleepTime() + " \"\\Processor(*)\\*\"";
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
