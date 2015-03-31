package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates2;
import com.asetune.check.CheckForUpdates2Hana;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameHana;
import com.asetune.utils.DbUtils;

public class HanaTune
extends DbxTune
{
	public final static String APP_NAME = "HanaTune";

	public HanaTune(CommandLine cmd) 
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
//		return "HANATUNE_HOME";
//	}
//
//	@Override
//	public String getAppSaveDirEnvName()
//	{
//		return "HANATUNE_SAVE_DIR";
//	}

	@Override
	public String getConfigFileName()
	{
		return "hanatune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "hanatune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "hanatune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_HANA;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerHana.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameHana();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerHana(hasGui);
	}

	@Override
	public CheckForUpdates2 createCheckForUpdates()
	{
		return new CheckForUpdates2Hana(); 
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}

}
