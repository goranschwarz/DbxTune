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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.ICounterController.DbmsOption;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmOpenTransactionsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.PcsColumnOptions;
import com.asetune.pcs.PcsColumnOptions.ColumnType;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Configuration;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.SqlServerUtils.LockRecord;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOpenTransactions
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmOpenTransactions.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmOpenTransactions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Open Transactions";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Who has open transactions on the server</p>" +
		"<br><br>" +
		"Table Background colors:" +
		"<ul>" +
		"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
		"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
		"</ul>" +
		"</html>";

//	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"CmOpenTransactions"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmOpenTransactions(counterController, guiController);
	}

	public CmOpenTransactions(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                      = CM_NAME;

	public static final String  PROPKEY_sample_spidLocks          = PROP_PREFIX + ".sample.spidLocks";
	public static final boolean DEFAULT_sample_spidLocks          = true;
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOpenTransactionsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public Map<String, PcsColumnOptions> getPcsColumnOptions()
	{
		Map<String, PcsColumnOptions> map = super.getPcsColumnOptions();

		// No settings in the super, create one, and set it at super
		if (map == null)
		{
			map = new HashMap<>();
			map.put("LastSqlText", new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));
			map.put("LastPlan"   , new PcsColumnOptions(ColumnType.DICTIONARY_COMPRESSION));

			// Set the map in the super
			setPcsColumnOptions(map);
		}

		return map;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		List <String> pkCols = new LinkedList<String>();
//
//		pkCols.add("session_id");
//
//		return pkCols;
		return null;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;

		String dm_tran_database_transactions = "sys.dm_tran_database_transactions";
		String dm_tran_session_transactions  = "sys.dm_tran_session_transactions";
		String dm_exec_sessions              = "sys.dm_exec_sessions";
		String dm_exec_connections           = "sys.dm_exec_connections";
		String dm_exec_requests              = "sys.dm_exec_requests";
		String dm_exec_sql_text              = "sys.dm_exec_sql_text";
		String dm_exec_query_plan            = "sys.dm_exec_query_plan";
		
		// get Actual-Query-Plan instead of Estimated-QueryPlan
		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
		{
			dm_exec_query_plan = "sys.dm_exec_query_plan_stats";
		}

		if (ssVersionInfo.isAzureSynapseAnalytics())
		{
			dm_tran_database_transactions = "sys.dm_pdw_nodes_tran_database_transactions";
			dm_tran_session_transactions  = "sys.dm_pdw_nodes_tran_session_transactions";
			dm_exec_sessions              = "sys.dm_pdw_nodes_exec_sessions";
			dm_exec_connections           = "sys.dm_pdw_exec_connections";
			dm_exec_requests              = "sys.dm_exec_requests";                          // Same name in Azure ???
			dm_exec_sql_text              = "sys.dm_exec_sql_text";                          // Same name in Azure ???
			dm_exec_query_plan            = "sys.dm_exec_query_plan";                        // Same name in Azure ???
		}

		
		String sql = ""
				+ "-------------------------------------------------------------------------------------------------------\n"
				+ "-- Origin SQL from: https://www.sqlskills.com/blogs/paul/script-open-transactions-with-text-and-plans/ \n"
				+ "-- Just reformated the code and added a bunch of columns \n"
				+ "-------------------------------------------------------------------------------------------------------\n"
				+ "SELECT /* ${cmCollectorName} */ \n"
				+ "     [s_tst].[session_id] \n"
				+ "    ,db_name(s_tdt.database_id)                       AS [Database] \n"
				+ "    ,[s_es].[status]                                   AS [Status] \n"
				+ "    ,[s_es].[login_name]                               AS [LoginName] \n"
				+ "    ,[s_es].[login_time]                               AS [LoginTime] \n"
				+ "    ,[s_es].[host_name]                                AS [HostName] \n"
				+ "    ,[s_es].[host_process_id]                          AS [HostPID] \n"
				+ "    ,[s_es].[program_name]                             AS [ProgramName] \n"
				+ "    ,convert(bit,0)                                    AS [HasSpidLocks]\n"
				+ "    ,convert(int,-1)                                   AS [SpidLockCount] \n"
				+ "    ,[s_es].[last_request_start_time]                  AS [LastRequestStartTime] \n"
				+ "    ,[s_tdt].[database_transaction_begin_time]         AS [TranBeginTime] \n"
				+ "    ,[s_tdt].[database_transaction_log_record_count]   AS [TranLogRecordCount] \n"
				+ "    ,[s_tdt].[database_transaction_log_bytes_used]     AS [LogBytesUsed] \n"
				+ "    ,[s_tdt].[database_transaction_log_bytes_reserved] AS [LogBytesReserved] \n"
				+ "    ,[s_er].[status]                                   AS [ExecStatus] \n"
				+ "    ,[s_er].[command]                                  AS [ExecCommand] \n"
				+ "    ,[s_er].[blocking_session_id]                      AS [BlockedBySpid] \n"
				+ "    ,(SELECT count(*) FROM " + dm_exec_requests + " x where x.blocking_session_id = s_tst.session_id) AS [BlockingOtherSpidCount] \n"
				+ "    ,[s_er].[wait_type]                                AS [WaitType] \n"
				+ "    ,[s_er].[wait_time]                                AS [WaitTimeInMs] \n"
				+ "    ,[s_er].[last_wait_type]                           AS [LastWaitType] \n"
				+ "    ,[s_est].text                                      AS [LastSqlText] \n"
				+ "    ,[s_eqp].[query_plan]                              AS [LastPlan] \n"
				+ "    ,convert(varchar(max),null)                        AS [SpidLocks] \n"
				+ "FROM            " + dm_tran_database_transactions + " [s_tdt] \n"
				+ "JOIN            " + dm_tran_session_transactions + "  [s_tst]  ON [s_tst].[transaction_id] = [s_tdt].[transaction_id] \n"
				+ "JOIN            " + dm_exec_sessions + "              [s_es]   ON [s_es] .[session_id]     = [s_tst].[session_id] \n"
				+ "JOIN            " + dm_exec_connections + "           [s_ec]   ON [s_ec] .[session_id]     = [s_tst].[session_id] \n"
				+ "LEFT OUTER JOIN " + dm_exec_requests + "              [s_er]   ON [s_er] .[session_id]     = [s_tst].[session_id] \n"
				+ "OUTER APPLY     " + dm_exec_sql_text + " ([s_ec].[most_recent_sql_handle]) AS [s_est] \n"
				+ "OUTER APPLY     " + dm_exec_query_plan + " ([s_er].[plan_handle])          AS [s_eqp] \n"
				+ "WHERE [s_tdt].[database_transaction_begin_time] IS NOT NULL -- Time at which the database became involved in the transaction. \n"
				+ "ORDER BY [TranBeginTime] ASC                                -- Specifically, it is the time of the first log record in the database for the transaction. \n"
				+ "";

		return sql;
	}
	
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get SPID Locks"           , PROPKEY_sample_spidLocks       , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_spidLocks       , DEFAULT_sample_spidLocks      ), DEFAULT_sample_spidLocks      , "Do 'select <i>someCols</i> from syslockinfo where spid = ?' on every row in the table. This will help us to diagnose what the current SQL statement is locking."));

		return list;
	}

	
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for some columns of type bit/Boolean
		String colName = getColumnName(columnIndex);

		if ("HasSpidLocks".equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
	}

	@Override
	public void localCalculation(CounterSample newSample)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		boolean getSpidLocks      = conf == null ? false: conf.getBooleanProperty(PROPKEY_sample_spidLocks,      DEFAULT_sample_spidLocks);

		int pos_session_id    = newSample.findColumn("session_id"); 
		int pos_SpidLocks     = newSample.findColumn("SpidLocks"); 
		int pos_HasSpidLocks  = newSample.findColumn("HasSpidLocks"); 
		int pos_SpidLockCount = newSample.findColumn("SpidLockCount");

		if (pos_session_id < 0 || pos_SpidLocks < 0 || pos_HasSpidLocks < 0 || pos_SpidLockCount < 0)
		{
			_logger.debug("Can't find the position for columns ('session_id'="+pos_session_id+", 'SpidLocks'="+pos_SpidLocks+", 'HasSpidLocks'="+pos_HasSpidLocks+", SpidLockCount="+pos_SpidLockCount+")");
			return;
		}
		
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID = newSample.getValueAt(rowId, pos_session_id);
			
			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				String spidLocks       = "This was disabled";
				int    spidLockCount   = -1;

				if (getSpidLocks)
				{
					List<LockRecord> lockList = null;
					try
					{
						lockList = SqlServerUtils.getLockSummaryForSpid(getCounterController().getMonConnection(), spid);
						spidLocks = SqlServerUtils.getLockSummaryForSpid(lockList, true, false);
						if (spidLocks == null)
							spidLocks = "No Locks found";
					}
					catch (TimeoutException ex)
					{
						spidLocks = "Timeout - when getting lock information";
					}
					
					spidLockCount = 0;
					if (lockList != null)
					{
						for (LockRecord lockRecord : lockList)
						{
							spidLockCount += lockRecord._lockCount;
						}
					}
				}

				boolean b;

				// SPID Locks
				b = !"This was disabled".equals(spidLocks) && !"No Locks found".equals(spidLocks) && !"Timeout - when getting lock information".equals(spidLocks);
				newSample.setValueAt(new Boolean(b), rowId, pos_HasSpidLocks);
				newSample.setValueAt(spidLocks,      rowId, pos_SpidLocks);
				newSample.setValueAt(spidLockCount,  rowId, pos_SpidLockCount);
	
			} // end: SPID is a number

		} // end: loop rows

	} // end: method
}
