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
 * sp_iqlocks procedure
 * Shows information about locks in the database, for both the IQ store and the catalog store
 * 
 * @author I063869
 *
 */
public class CmIqLocks
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIqLocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "locks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<h4>sp_iqlocks</h4>"
		+ "Shows information about locks in the database, for both the IQ store and the catalog store" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sp_iqlocks"};
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

		return new CmIqLocks(counterController, guiController);
	}

	public CmIqLocks(ICounterController counterController, IGuiController guiController)
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
			mtd.addTable("sp_iqlocks",  "Shows information about locks in the database, for both the IQ store and the catalog store.");

			mtd.addColumn("sp_iqlocks", "conn_name",  "<html>The name of the current connection</html>");
			mtd.addColumn("sp_iqlocks", "conn_id",  "<html>Connection ID that has the lock.</html>");
			mtd.addColumn("sp_iqlocks", "user_id",  "<html>User associated with this connection ID.</html>");
			mtd.addColumn("sp_iqlocks", "table_type",  "<html>The type of table. This type is either BASE for a table, GLBTMP for global temporary table, or MVIEW for a materialized view. " +
			                                    "<br/>Materialized views are only supported for SQL Anywhere tables in the IQ catalog store.</html>");
			mtd.addColumn("sp_iqlocks", "creator",  "<html>The owner of the table.</html>");
			mtd.addColumn("sp_iqlocks", "table_name",  "<html>Table on which the lock is held.</html>");
			mtd.addColumn("sp_iqlocks", "index_id",  "<html>The index ID or NULL.</html>");		
			mtd.addColumn("sp_iqlocks", "lock_class",  "<html>String of characters indicating the type of lock:"
					+ "<ul><li>S � share."
					+ "<li>SW � share and write."
					+ "<li>EW � exclusive and write."
					+ "<li>E � exclusive."
					+ "<li>P � phantom."
					+ "<li>A � antiphantom."
					+ "<li>W � write.</ul>"
					+ "All locks listed have one of S, E, EW, or SW, and may also have P, A, or both. Phantom and antiphantom locks also have a qualifier of T or *:"
					+ "<ul><li>T � the lock is with respect to a sequential scan."
					+ "<li>* � the lock is with respect to all scans."
					+ "<li>nnn � Index number; the lock is with respect to a particular index."
					+ "Sybase IQ obtains a share lock before a write lock. If a connection has exclusive lock, share lock does not appear. For write locks, if a connection has all-exclusive, share, and write locks, it is EW."
					+ "</html>");
			mtd.addColumn("sp_iqlocks", "lock_duration",  "<html>The duration of the lock. One of Transaction, Position, or Connection.</html>");
			mtd.addColumn("sp_iqlocks", "lock_type",  "<html>Value identifying the lock (dependent on the lock class)</html>");
			mtd.addColumn("sp_iqlocks", "row_identifier",  "<html>The identifier for the row or NULL.</html>");
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
//		pkCols.add("conn_id");
//
//		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from sp_iqlocks()";

		return sql;
	}
}
