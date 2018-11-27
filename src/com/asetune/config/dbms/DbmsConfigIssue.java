package com.asetune.config.dbms;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import com.asetune.utils.Configuration;

public class DbmsConfigIssue
{
	public enum Severity
	{
		WARNING,
		ERROR
	};
	
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
		return Configuration.getCombinedConfiguration().getBooleanProperty(getDiscardPropKey(), false);
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
