/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune;

import java.io.File;

import org.apache.commons.cli.CommandLine;

import com.dbxtune.check.CheckForUpdates;
import com.dbxtune.check.CheckForUpdatesMySql;
import com.dbxtune.config.dbms.IDbmsConfig;
import com.dbxtune.config.dbms.MySqlConfig;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryMySql;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.MainFrameMySql;
import com.dbxtune.pcs.inspection.IObjectLookupInspector;
import com.dbxtune.pcs.sqlcapture.ISqlCaptureBroker;
import com.dbxtune.sql.DbmsVersionHelperSimple;
import com.dbxtune.sql.IDbmsVersionHelper;
import com.dbxtune.utils.DbUtils;

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
		return "conf" + File.separatorChar + "dbxtune.properties";
	}

	@Override
	public String getUserConfigFileName()
	{
		return "conf" + File.separatorChar + "mysqltune.user.properties";
	}

	@Override
	public String getSaveConfigFileName()
	{
		return "conf" + File.separatorChar + "mysqltune.save.properties";
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
		return CounterControllerMySql.NUMBER_OF_PERFORMANCE_COUNTERS;
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
	public IDbmsVersionHelper createDbmsVersionHelper()
	{
		return new DbmsVersionHelperSimple();
	}

	@Override
	public IDbmsConfig createDbmsConfig()
	{
		//return null; // null means it's not supported
		return new MySqlConfig();
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
