/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesDb2;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryDb2;
import com.asetune.gui.MainFrame;
import com.asetune.gui.MainFrameDb2;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.sql.DbmsVersionHelperSybase;
import com.asetune.sql.IDbmsVersionHelper;
import com.asetune.utils.DbUtils;

public class Db2Tune
extends DbxTune
{
	public final static String APP_NAME = "Db2Tune";

	public Db2Tune(CommandLine cmd) 
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
	public String getConfigFileName()
	{
		return "conf" + File.separatorChar + "dbxtune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "conf" + File.separatorChar + "db2tune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "conf" + File.separatorChar + "db2tune.save.properties";
	}

	@Override
	public String getSupportedProductName()
	{
		return DbUtils.DB_PROD_NAME_DB2_LUW;
	}

	@Override
	public int getSplashShreenSteps()
	{
		// TODO Auto-generated method stub
		return CounterControllerDb2.NUMBER_OF_PERFORMANCE_COUNTERS;
	}

	@Override
	public MainFrame createGuiMainFrame()
	{
		return new MainFrameDb2();
	}

	@Override
	public ICounterController createCounterController(boolean hasGui)
	{
		return new CounterControllerDb2(hasGui);
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
		return new CheckForUpdatesDb2(); 
	}

	@Override
	public IDbmsVersionHelper createDbmsVersionHelper()
	{
		return new DbmsVersionHelperSybase();
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
		return null; // null means it's not supported
//		return new Db2Config();
	}

	@Override
	public MonTablesDictionary createMonTablesDictionary()
	{
		return new MonTablesDictionaryDb2();
	}

	public static void main(String[] args)
	{
		DbxTune.main(args);
	}
}
