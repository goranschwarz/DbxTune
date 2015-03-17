package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates2;
import com.asetune.check.CheckForUpdates2Rs;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameRs;
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

	@Override
	public String getAppHomeEnvName()
	{
		return "RSTUNE_HOME";
	}

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
	public CheckForUpdates2 createCheckForUpdates()
	{
		return new CheckForUpdates2Rs(); 
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
