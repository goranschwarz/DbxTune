package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesRs;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dbms.RsConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryRs;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameRs;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.inspection.ObjectLookupInspectorAse;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.utils.DbUtils;

public class RsTune
extends DbxTune
{
	public final static String APP_NAME = "RsTune";

	public RsTune(CommandLine cmd) 
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
//		return "RSTUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "RSTUNE_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "rstune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "rstune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "rstune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_SYBASE_RS;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerRs.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameRs();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerRs(hasGui);
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
		return new CheckForUpdatesRs(); 
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
		return new RsConfig();
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionaryRs();
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
