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
package com.dbxtune.cm.iq;

import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */

/**
 * The following is a key to understanding the Main IQ I/O and Temporary IQ I/O output codes:
 * I: Input
 * L: Logical pages read (�Finds�)
 * P: Physical pages read
 * O: Output
 * C: Pages created
 * D: Pages dirtied
 * P: Physically written
 * D: Pages destroyed
 * C: Compression ratio
 * @author I063869
 *
 */
public class CmIqStatus
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqStatus.class.getSimpleName();
	public static final String   SHORT_NAME       = "status";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqstatus</h4>" + 
		"Displays a variety of Sybase IQ status information about the current database." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqstatus"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 60; //CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmIqStatus(counterController, guiController);
	}

	public CmIqStatus(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);
		setBackgroundDataPollingEnabled(false, false);
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("sp_iqstatus",  "The sp_iqstatus stored procedure provides a variety of IQ status information.");

			mtd.addColumn("sp_iqstatus", "name",  "<html>Name</html>");
			mtd.addColumn("sp_iqstatus", "value",  "<html>Value</html>");
		}
		catch (NameNotFoundException e) 
		{
		//	_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
			System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from sp_iqstatus()";

		return sql;
	}
}
