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
package com.asetune.config.dbms;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import com.asetune.utils.Configuration;

public class DbmsConfigIssue
{
	public enum Severity
	{
		INFO    ("Information"),
		WARNING ("Warning"),
		ERROR   ("Error");

		private final String _displayName;
		private Severity(String displayName)
		{
			_displayName = displayName;
		}
		@Override
		public String toString()
		{
			return _displayName;
		}
	};
	
	private boolean _isOfflineEntryDiscarded = false;
	private boolean _isOfflineEntry = false;
	public void setOfflineEntry() { _isOfflineEntry = true; }
	public void setOfflineEntryDiscarded(boolean b) { _isOfflineEntryDiscarded = b; }
	
	private Timestamp _srvRestart;
	private String    _propKey;
	private String    _configName;
	private Severity  _severity;
	private String    _description;
	private String    _resolution;
	
	public DbmsConfigIssue(Timestamp srvRestart, String propKey, String configName, Severity severity, String description, String resolution)
	{
		_srvRestart  = srvRestart;
		_propKey     = propKey;
		_configName  = configName;
		_severity    = severity;
		_description = description;
		_resolution  = resolution;
	}
	
	public Timestamp getSrvRestartTs() { return _srvRestart; }
	
	public String   getPropKey()     { return _propKey; }
	public String   getConfigName()  { return _configName; }
	public Severity getSeverity()    { return _severity; }
	public String   getDescription() { return _description; }
	public String   getResolution()  { return _resolution; }

//	public void setPropKey     (String propKey)     { _propKey     = propKey; }
//	public void setConfigName  (String configName)  { _configName  = configName; }
	public void setSeverity    (Severity severity)  { _severity    = severity; }
	public void setDescription (String description) { _description = description; }
	public void setResolution  (String resolution)  { _resolution  = resolution; }

	public String getDiscardPropKey()
	{
		String srvRestartTime = "";
		if (_srvRestart != null)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd.HHmmss");
			srvRestartTime = "." + sdf.format(_srvRestart);
		}
		return this.getPropKey() + srvRestartTime + ".discard";
	}
	
	public boolean isDiscarded()
	{
		if (_isOfflineEntry)
		{
			return _isOfflineEntryDiscarded;
		}
		else
		{
			return Configuration.getCombinedConfiguration().getBooleanProperty(getDiscardPropKey(), false);
		}
	}

	public void setDiscarded(boolean discard)
	{
		Configuration tmpCfg = Configuration.getInstance(Configuration.USER_TEMP);
		if (tmpCfg == null)
			return;

		tmpCfg.setProperty(getDiscardPropKey(), discard);
		tmpCfg.save();
	}
}
