package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesMySql;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryMySql;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameMySql;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.utils.DbUtils;

public class MySqlTune
extends DbxTune
{
	public final static String APP_NAME = "MySqlTune";

	public MySqlTune(CommandLine cmd) 
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
//		return "MYSQLTUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "MYSQLTUNE_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "dbxtune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "mysqltune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "mysqltune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_MYSQL;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerPostgres.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameMySql();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerMySql(hasGui);
	}

	@Override
	public IObjectLookupInspector createPcsObjectLookupInspector()
	{
		return null;
	}

	@Override
	public ISqlCaptureBroker createPcsSqlCaptureBroker() 
	{
		return null;
	}
	
	@Override
	public CheckForUpdates createCheckForUpdates()
	{
		return new CheckForUpdatesMySql(); 
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
		return null; // null means it's not supported
//		return new MySqlConfig();
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionaryMySql();
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
