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
package com.asetune.pcs.sqlcapture;

import org.apache.log4j.Logger;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public abstract class SqlCaptureBrokerAbstract implements ISqlCaptureBroker
{
	private static Logger _logger = Logger.getLogger(SqlCaptureBrokerAbstract.class);
	
	/** Configuration we were initialized with */
	private Configuration _conf;


	@Override
	public void onConnect(DbxConnection conn)
	{
	}

	@Override
	public DbxConnection createConnection()
	throws Exception
	{
		String appName = Version.getAppName()+"-SqlCaptureBroker";
		
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

	// Check the connection for OpenTransaction or other ABNORMALITIES
	// return: true = OK, false = CLOSE CONNECTION 
	@Override
	public boolean checkConnection(DbxConnection conn)
	throws Exception
	{
		boolean inTran =conn.isInTransaction();
		
		if (inTran)
		{
			_logger.error("When checking the SQL Capture DBMS Connection, it was still in a transaction. The Connection will be closed. The DBMS is responsible for clearing/cleaning up the transaction.");
			return false; // CLOSE CONNECTION
		}
		
		return true; // OK
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

	@Override
	public String getSqlPrefix()
	{
		return "/* " + Version.getAppName() + ":SqlCapture */ \n";
	}

	@Override
	public boolean useSqlPrefix()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		boolean useSqlPrefix = conf.getBooleanProperty("cm.sqlPrefix.use", true);
		if (useSqlPrefix)
		{
			if (conf.hasProperty("cm.sqlCapture.sqlPrefix.use"))
			{
				useSqlPrefix = conf.getBooleanProperty("cm.sqlCapture.sqlPrefix.use", true);
			}
		}
		return useSqlPrefix;
	}
}
