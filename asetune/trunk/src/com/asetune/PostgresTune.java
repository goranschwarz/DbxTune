package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesPostgres;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryPostgres;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFramePostgres;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.utils.DbUtils;

public class PostgresTune
extends DbxTune
{
	public final static String APP_NAME = "PostgresTune";

	public PostgresTune(CommandLine cmd) 
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
//		return "POSTGRESTUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "POSTGRESTUNE_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "postgrestune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "postgrestune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "postgrestune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_POSTGRES;
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
		return new MainFramePostgres();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerPostgres(hasGui);
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
		return new CheckForUpdatesPostgres(); 
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
		return null; // null means it's not supported
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionaryPostgres();
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
