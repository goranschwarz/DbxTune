package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesAse;
import com.asetune.config.dbms.AseConfig;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryAse;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameAse;
import com.asetune.utils.DbUtils;

public class AseTune
extends DbxTune
{
	public final static String APP_NAME = "AseTune";

	public AseTune(CommandLine cmd) 
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
//		return "ASETUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "ASETUNE_SAVE_DIR";
//	}
//
	@Override
	public String getConfigFileName()
	{
		return "asetune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "asetune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "asetune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_SYBASE_ASE;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
//		return 111;
		return CounterControllerAse.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameAse();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerAse(hasGui);
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}

	@Override
	public CheckForUpdates createCheckForUpdates()
	{
		return new CheckForUpdatesAse(); 
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
		return new AseConfig();
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionaryAse();
	}
}