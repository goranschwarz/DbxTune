package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates2;
import com.asetune.check.CheckForUpdates2Iq;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameIq;
import com.asetune.utils.DbUtils;

public class IqTune
extends DbxTune
{
	public final static String APP_NAME = "IqTune";

	public IqTune(CommandLine cmd) 
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
//		return "IQTUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "IQTUNE_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "iqtune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "iqtune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "iqtune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_SYBASE_IQ;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerIq.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameIq();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerIq(hasGui);
	}

	@Override
	public CheckForUpdates2 createCheckForUpdates()
	{
		return new CheckForUpdates2Iq(); 
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
