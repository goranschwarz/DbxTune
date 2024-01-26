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
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.alarm.AlarmHelper;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmWhoPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWho
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmWho.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWho.class.getSimpleName();
	public static final String   SHORT_NAME       = "Who";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>What SQL Server SPIDs are doing what.</p>" +
			"<br>" +
			"Table Background colors:" +
			"<ul>" +
			"    <li>YELLOW      - SPID is a System Processes</li>" +
			"    <li>GREEN       - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
			"    <li>LIGHT_GREEN - SPID is Suspended waiting for something, soon it will probably go into running or runnable or finish.</li>" +
			"    <li>PINK        - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
			"    <li>ORANGE      - SPID has an open transaction.</li>" +
			"    <li>RED         - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysprocesses"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"cpu",
		"physical_io",
		"logical_reads",
		"memusage"
		};

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

		return new CmWho(counterController, guiController);
	}

	public CmWho(ICounterController counterController, IGuiController guiController)
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
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_systemThreads       = PROP_PREFIX + ".sample.systemThreads";
	public static final boolean DEFAULT_sample_systemThreads       = false;

	public static final int     COLPOS_is_user_process             = 0;

	private HashMap<Number,Object> _blockingSpids = new HashMap<Number,Object>(); // <(SPID)Integer> <null> indicator that the SPID is BLOCKING some other SPID

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Sample System Threads", PROPKEY_sample_systemThreads , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_systemThreads  , DEFAULT_sample_systemThreads  ), DEFAULT_sample_systemThreads, CmWhoPanel.TOOLTIP_sample_systemThreads ));

		return list;
	}


	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWhoPanel(this);
	}

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
			mtd.addColumn("sysprocesses",  "dupMergeCount", "<html>" +
			                                                       "If more than <b>one</b> row was fetched for this <i>Primary Key</i> (spid, ecid).<br>" +
			                                                       "Then this column will hold number of rows merged into this row. 0=No Merges(only one row for this PK), 1=One Merge accurred(two rows was seen for this PK), etc...<br>" +
			                                                       "This means that the non-diff columns will be from the first row fetched,<br>" +
			                                                       "then all columns which is marked for difference calculation will be a summary of all the rows (so it's basically a SQL SUM(colName) operation)." +
			                                                   "</html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		//	System.out.println("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e); 
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("spid");
		pkCols.add("ecid");

		// NOTE: PK is NOT unique, so therefore 'dupMergeCount' column is added to the SQL Query
		//       when there are Parallel Statements ECID isn't unique enough (sometimes there are 2 ECID's but with different KPID (OS Thread ID)
		
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_systemThreads  = conf.getBooleanProperty(PROPKEY_sample_systemThreads, DEFAULT_sample_systemThreads);

		// Should we sample SYSTEM SPID's
//		String sql_sample_systemThreads = "--and sid != 0x01 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
//		if ( ! sample_systemThreads )
//			sql_sample_systemThreads = "  and sid != 0x01 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";

//		String sql_sample_systemThreads = "--and isnull(es.is_user_process, 0) = 0 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";
//		if ( ! sample_systemThreads )
//			sql_sample_systemThreads = "  and isnull(es.is_user_process, 0) = 0 -- Property: "+PROPKEY_sample_systemThreads+" is "+sample_systemThreads+". \n";


		// Is 'context_info_str' enabled (if it causes any problem, it can be disabled)
		String contextInfoStr = "/*    context_info_str = replace(cast(sp.context_info as varchar(128)),char(0),''), -- " + SqlServerCmUtils.HELPTEXT_howToEnable__context_info_str + " */ \n";
		if (SqlServerCmUtils.isContextInfoStrEnabled())
		{
			// Make the binary 'context_info' into a String
			contextInfoStr = "    context_info_str = replace(cast(sp.context_info as varchar(128)),char(0),''), /* " + SqlServerCmUtils.HELPTEXT_howToDisable__context_info_str + " */ \n";
		}


		String sql = 
			"select /* ${cmCollectorName} */ \n" +
			"    is_user_process = isnull(es.is_user_process, 0), \n" +
			"    sp.spid, \n" +
			"    sp.ecid, \n" +
			"    sp.kpid, \n" +
			"    sp.loginame, \n" +
			"    sp.cmd, \n" +
			"    sp.status, \n" +
			"    sp.blocked, \n" +
			"    BlockingOtherSpids = convert(varchar(512),''), \n" +
			"    sp.open_tran, \n" +
			"    sp.waittype, \n" +
			"    sp.waittime, \n" +
			"    sp.lastwaittype, \n" +
			"    sp.waitresource, \n" +
			"    dbname = db_name(dbid), \n" +
			"    authenticating_dbname = db_name(es.authenticating_database_id), \n" +
			"    sp.uid, \n" +
			"    dupMergeCount = convert(int, 0), \n" + // merge duplicates and increment the count (spid, ecid) do not seem to be unique when there are Parallel Statements
			"    sp.cpu, \n" +
			"    sp.physical_io, \n" +
			"    es.logical_reads, \n" +
			"    sp.memusage, \n" +
			"    sp.login_time, \n" +
			"    login_time_ss = CASE WHEN datediff(day, sp.login_time, getdate()) >= 24 THEN -1 ELSE  datediff(ss, sp.login_time, getdate()) END, \n" +
			"    sp.last_batch, \n" +
//			"    last_batch_ss = datediff(ss, last_batch, getdate()), \n" +
			"    last_batch_ss = CASE WHEN datediff(day, sp.last_batch, getdate()) >= 24 THEN -1 ELSE  datediff(ss, sp.last_batch, getdate()) END, \n" +
			"    sp.hostname, \n" +
			"    sp.program_name, \n" +
			     contextInfoStr +
			"    sp.hostprocess, \n" +
			"    sp.nt_domain, \n" +
			"    sp.nt_username, \n" +
			"    sp.net_address, \n" +
			"    sp.net_library, \n" +
			"    sp.stmt_start, \n" +
			"    sp.stmt_end, \n" +
			"    sp.request_id, \n" +
			"    sp.sql_handle, \n" +
			"    sp.sid, \n" +
			"    sp.context_info \n" +
			"from sys.sysprocesses sp \n" +
			"left outer join sys.dm_exec_sessions es on sp.spid = es.session_id \n" +
			"where 1 = 1 \n" +
//			sql_sample_systemThreads + 
			"";

		return sql;
	}


//	/** 
//	 * Maintain the _blockingSpids Map, which is accessed from the Panel
//	 */
//	@Override
//	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	{
//		// Where are various columns located in the Vector 
//		int pos_BlockingSPID = -1;
//		CounterSample counters = diffData;
//	
//		if (counters == null)
//			return;
//
//		// Reset the blockingSpids Map
//		_blockingSpids.clear();
//		
//		// put the pointer to the Map in the Client Property of the JTable, which should be visible for various places
//		if (getTabPanel() != null)
//			getTabPanel().putTableClientProperty("blockingSpidMap", _blockingSpids);
//
//		// Find column Id's
//		List<String> colNames = counters.getColNames();
//		if (colNames==null) 
//			return;
//
//		for (int colId=0; colId < colNames.size(); colId++) 
//		{
//			String colName = colNames.get(colId);
//			if (colName.equals("blocked"))  pos_BlockingSPID  = colId;
//
//			// Noo need to continue, we got all our columns
//			if (pos_BlockingSPID >= 0)
//				break;
//		}
//
//		if (pos_BlockingSPID < 0)
//		{
//			_logger.debug("Can't find the position for columns ('blocked'="+pos_BlockingSPID+")");
//			return;
//		}
//		
//		// Loop on all diffData rows
//		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
//		{
//			Object o_blockingSpid = counters.getValueAt(rowId, pos_BlockingSPID);
//
//			// Add any blocking SPIDs to the MAP
//			// TODO: for offline recordings it's better to do it in the same way as for 'CmActiveStatements'
//			if (o_blockingSpid instanceof Number)
//			{
//				if (o_blockingSpid != null && ((Number)o_blockingSpid).intValue() != 0 )
//					_blockingSpids.put((Number)o_blockingSpid, null);
//			}
//		}
//	}
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// make: column 'program_name' with value "SQLAgent - TSQL JobStep (Job 0x38AAD6888E5C5E408DE573B0A25EE970 : Step 1)"
		// into:                                  "SQLAgent - TSQL JobStep (Job '<name-of-the-job>' : Step 1 '<name-of-the-step>')
		SqlServerCmUtils.localCalculation_resolveSqlAgentProgramName(newSample);


		int pos_SPID               = newSample.findColumn("spid");
		int pos_BlockingSPID       = newSample.findColumn("blocked");
		int pos_BlockingOtherSpids = newSample.findColumn("BlockingOtherSpids");
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID        = newSample.getValueAt(rowId, pos_SPID);
			
			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStrForSpid(newSample, spid, pos_BlockingSPID, pos_SPID);

				newSample.setValueAt(blockingList, rowId, pos_BlockingOtherSpids);
			}

		}
	}

	private String getBlockingListStrForSpid(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
	{
		StringBuilder sb = new StringBuilder();

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId < rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number BlockingSPID = (Number)o_BlockingSPID;
				if (BlockingSPID.intValue() == spid)
				{
					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
					if (sb.length() == 0)
						sb.append(o_SPID);
					else
						sb.append(", ").append(o_SPID);
				}
			}
		}
		return sb.toString();
	}

	
	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "program_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "loginame");
	}
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "program_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "loginame") );
		
		return list;
	}
}
