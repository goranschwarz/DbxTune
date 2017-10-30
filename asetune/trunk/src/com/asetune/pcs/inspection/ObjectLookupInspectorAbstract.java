package com.asetune.pcs.inspection;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public abstract class ObjectLookupInspectorAbstract
implements IObjectLookupInspector
{
	/** Configuration we were initialized with */
	private Configuration _conf;

	@Override
	public DbxConnection createConnection()
	throws Exception
	{
		String appName = Version.getAppName()+"-ObjInfoLookup";
		
		boolean hasGui = DbxTune.hasGui();
		if (hasGui)
		{
			return MainFrame.getInstance().getNewConnection(appName);
		}
		else
		{
			// FIXME: implement this is some way...
//			throw new Exception("createConnection() not yet implemented for DbxTune when there is NO GUI.");
			return DbxConnection.connect(null, appName); 
		}
	}

	@Override
	public void init(Configuration conf)
	{
		_conf = conf;
	}

	@Override
	public Configuration getConfiguration()
	{
		return _conf;
	}
	
	@Override
	public String getProperty(String propName, String defaultValue)
	{
		Configuration conf = getConfiguration(); 
		if (conf == null)
			return defaultValue;
		
		try {                 return conf.getMandatoryProperty(propName); }
		catch(Exception ex) { return Configuration.getCombinedConfiguration().getProperty(propName, defaultValue); }
	}

	@Override
	public boolean getBooleanProperty(String propName, boolean defaultValue)
	{
		Configuration conf = getConfiguration(); 
		if (conf == null)
			return defaultValue;
		
		try {                 return conf.getBooleanMandatoryProperty(propName); }
		catch(Exception ex) { return Configuration.getCombinedConfiguration().getBooleanProperty(propName, defaultValue); }
	}

	@Override
	public int getIntProperty(String propName, int defaultValue)
	{
		Configuration conf = getConfiguration(); 
		if (conf == null)
			return defaultValue;
		
		try {                 return conf.getIntMandatoryProperty(propName); }
		catch(Exception ex) { return Configuration.getCombinedConfiguration().getIntProperty(propName, defaultValue); }
	}

	@Override
	public long getLongProperty(String propName, long defaultValue)
	{
		Configuration conf = getConfiguration(); 
		if (conf == null)
			return defaultValue;
		
		try {                 return conf.getLongMandatoryProperty(propName); }
		catch(Exception ex) { return Configuration.getCombinedConfiguration().getLongProperty(propName, defaultValue); }
	}
}
