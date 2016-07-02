package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesRax;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.RaxConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryDefault;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameRax;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.utils.DbUtils;

public class RaxTune
extends DbxTune
{
	public final static String APP_NAME = "RaxTune";

	public RaxTune(CommandLine cmd) 
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
//		return "RAXTUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "RAX_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "raxtune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "raxtune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "raxtune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_SYBASE_RAX;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerRax.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameRax();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerRax(hasGui);
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
	public IDbmsConfig createDbmsConfig()
	{
		return new RaxConfig();
	}

	@Override
	public CheckForUpdates createCheckForUpdates()
	{
		return new CheckForUpdatesRax(); 
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionaryDefault();
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
