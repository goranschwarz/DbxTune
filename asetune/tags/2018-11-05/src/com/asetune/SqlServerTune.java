package com.asetune;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesSqlServer;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.SqlServerConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionarySqlServer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameSqlServer;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.inspection.ObjectLookupInspectorSqlServer;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.utils.DbUtils;

public class SqlServerTune
extends DbxTune
{
	public final static String APP_NAME = "SqlServerTune";

	public SqlServerTune(CommandLine cmd) 
	throws Exception
	{
		super(cmd);
	}

	@Override
	public String getAppName()
	{
		return APP_NAME;
	}

//	@Override
//	public String getAppHomeEnvName()
//	{
//		return "SQLSERVERTUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "SQLSERVERTUNE_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "conf" + File.separatorChar + "dbxtune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "conf" + File.separatorChar + "sqlservertune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "conf" + File.separatorChar + "sqlservertune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_MSSQL;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerSqlServer.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameSqlServer();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerSqlServer(hasGui);
	}

	@Override
	public IObjectLookupInspector createPcsObjectLookupInspector()
	{
		return new ObjectLookupInspectorSqlServer();
	}

	@Override
	public ISqlCaptureBroker createPcsSqlCaptureBroker() 
	{
		return null;
	}
	
	@Override
	public CheckForUpdates createCheckForUpdates()
	{
		return new CheckForUpdatesSqlServer(); 
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
//		return null; // null means it's not supported
		return new SqlServerConfig();
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionarySqlServer();
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
