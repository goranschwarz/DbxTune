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
package com.asetune.cm.sqlserver;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCache.ObjectInfo;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.swing.ColumnHeaderPropsEntry;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStPlanStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmActiveStatementsPlanStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStPlanStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Stmnts Plan Stat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Monitors real time query progress while the query is in execution.<br>" +
		"<br>" +
		"Show information about the Operators in a Execution Plan.<br>" +
		"What operators are exututing right now, and how far have they come.<br>" +
		"How many rows is expected and how many have it processed.<br>" +
		"<br>" +
		"It must be a really <b>slow</b> query to see whats happening.<br>" +
		"<br>" +
		"<b>NOTE: Just testing this for the moment, I dont think it gives a lot of value so it might be <i>scrapped</i>.</b>" +
//		"Table (cell) Background colors:" +
//		"<ul>" +
//		"    <li>ORANGE - (in column 'properties') Cursor is declared as GLOBAL (a better way is: <code>DECLARE <i>CURSOR_NAME</i> cursor <b>STATIC LOCAL</b> for...</code>).</li>" +
//		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(2016,0,0, 1); // 2016 SP1
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_profiles"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			 "row_count"
			,"rewind_count"
			,"rebind_count"
//			,"end_of_scan_count"
//			,"estimate_row_count"
			,"scan_count"
			,"logical_read_count"
			,"physical_read_count"
			,"read_ahead_count"
			,"write_page_count"
			,"lob_logical_read_count"
			,"lob_physical_read_count"
			,"lob_read_ahead_count"
			,"segment_read_count"
			,"segment_skip_count"
//			,"actual_read_row_count"
//			,"estimated_read_row_count"		
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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

		return new CmActiveStPlanStats(counterController, guiController);
	}

	public CmActiveStPlanStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

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
//		return new CmActiveStatementsPlanStatsPanel(this);
//	}

	@Override
	public boolean checkDependsOnVersion(DbxConnection conn)
	{
		DbmsVersionInfoSqlServer versionInfo = (DbmsVersionInfoSqlServer) conn.getDbmsVersionInfo();
		if (versionInfo.isAzureDb() || versionInfo.isAzureSynapseAnalytics())
			return true;

		return super.checkDependsOnVersion(conn);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
		pkCols.add("request_id");
		pkCols.add("node_id");
		pkCols.add("thread_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("index_name",    21));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("object_name",   21));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("schema_name",   21));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("database_name", 21)); // before "database_id" (or close)

		addPreferredColumnOrder(new ColumnHeaderPropsEntry("plan_handle",   ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN));
		addPreferredColumnOrder(new ColumnHeaderPropsEntry("sql_handle",    ColumnHeaderPropsEntry.AS_LAST_VIEW_COLUMN));

		String sql = ""
			    + "select /* ${cmCollectorName} */ \n"
				+ "     * \n"
				+ "    ,cast('' as varchar(128)) AS database_name \n"
				+ "    ,cast('' as varchar(128)) AS schema_name \n"
				+ "    ,cast('' as varchar(128)) AS object_name \n"
				+ "    ,cast('' as varchar(128)) AS index_name \n"
			    + "from sys.dm_exec_query_profiles \n"
			    + "where session_id != @@spid \n"
			    + "";

		return sql;
	}

	/**
	 * Fill in database, schema, object and index NAMES
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		if ( ! DbmsObjectIdCache.hasInstance() )
			return;

		DbmsObjectIdCache objectIdCache = DbmsObjectIdCache.getInstance();
		

		int database_id_pos   = newSample.findColumn("database_id");
		int object_id_pos     = newSample.findColumn("object_id");
		int index_id_pos      = newSample.findColumn("index_id");
		
		int database_name_pos = newSample.findColumn("database_name");
		int schema_name_pos   = newSample.findColumn("schema_name");
		int object_name_pos   = newSample.findColumn("object_name");
		int index_name_pos    = newSample.findColumn("index_name");

		// Check that all "pos" are found

		// Loop rows and fill in:
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_database_id = newSample.getValueAsObject(rowId, database_id_pos);
			Object o_object_id   = newSample.getValueAsObject(rowId, object_id_pos);
			Object o_index_id    = newSample.getValueAsObject(rowId, index_id_pos);
			
			if (   o_database_id != null && o_database_id instanceof Number 
				&& o_object_id   != null && o_object_id   instanceof Number)
			{
				int dbid     = ((Number)o_database_id).intValue();
				int objectId = ((Number)o_object_id).intValue();

				if (dbid == 0 && objectId == 0)
					continue;
				try
				{
					ObjectInfo oi = objectIdCache.getByObjectId(dbid, objectId);

					if (oi != null)
					{
						newSample.setValueAt(oi.getDBName()    , rowId, database_name_pos);
						newSample.setValueAt(oi.getSchemaName(), rowId, schema_name_pos);
						newSample.setValueAt(oi.getObjectName(), rowId, object_name_pos);
						
						if (o_index_id != null && o_index_id instanceof Number)
						{
							int indexId = ((Number)o_index_id).intValue();
						//	if (indexId != 0)
								newSample.setValueAt(oi.getIndexName(indexId), rowId, index_name_pos);
						}
					}
				}
				catch (TimeoutException ex) 
				{
					newSample.setValueAt("-timeout-", rowId, database_name_pos);
					newSample.setValueAt("-timeout-", rowId, schema_name_pos);
					newSample.setValueAt("-timeout-", rowId, object_name_pos);
				}
				
			}
		}
	}
}
