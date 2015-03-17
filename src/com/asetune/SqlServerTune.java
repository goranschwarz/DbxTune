package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates2;
import com.asetune.check.CheckForUpdates2SqlServer;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameSqlServer;
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

	@Override
	public String getAppHomeEnvName()
	{
		return "SQLSERVERTUNE_HOME";
	}

	@Override
	public String getConfigFileName()
	{
		return "sqlservertune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "sqlservertune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "sqlservertune.save.properties";
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
	public CheckForUpdates2 createCheckForUpdates()
	{
		return new CheckForUpdates2SqlServer(); 
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
