package com.asetune;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates2;
import com.asetune.check.CheckForUpdates2Oracle;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameOracle;
import com.asetune.utils.DbUtils;

public class OracleTune
extends DbxTune
{
	public final static String APP_NAME = "OracleTune";

	public OracleTune(CommandLine cmd) 
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
		return "ORACLETUNE_HOME";
	}

	@Override
	public String getConfigFileName()
	{
		return "oracletune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "oracletune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "oracletune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_ORACLE;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerOracle.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameOracle();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerOracle(hasGui);
	}

	@Override
	public CheckForUpdates2 createCheckForUpdates()
	{
		return new CheckForUpdates2Oracle(); 
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
