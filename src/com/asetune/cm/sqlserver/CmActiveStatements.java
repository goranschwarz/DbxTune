/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmActiveStatementsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.SqlServerWaitTypeDictionary;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.XmlFormatter;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmActiveStatements
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmActiveStatements.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmActiveStatements.class.getSimpleName();
	public static final String   SHORT_NAME       = "Active Statements";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_sessions", "dm_exec_requests", "dm_exec_connections", "dm_exec_sql_text", "dm_exec_query_plan"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"cpu_time",
		"reads",
		"logical_reads",
		"writes"
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

		return new CmActiveStatements(counterController, guiController);
	}

	public CmActiveStatements(ICounterController counterController, IGuiController guiController)
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmActiveStatementsPanel(this);
	}

	@Override
	public void initLocalToolTipTextOnTableColumnHeader()
	{
		setLocalToolTipTextOnTableColumnHeader("monSource",                 "<html>"
		                                                                  + "  <ul> "
		                                                                  + "    <li>ACTIVE  = session_id's that are currently executing some SQL Statement.</li> "
		                                                                  + "    <li>BLOCKER = session_id's that are blocking others from working. <b>Note:</b> sessions that are <i>sleeping/inactive</i> will still displayed</li> "
		                                                                  + "  </ul>"
		                                                                  + "  session_id's can be in both the 'ACTIVE' and 'BLOCKER' section. That is if the session_id is both blocking and currently executing a statement.<br>"
		                                                                  + "  If the session_id is <b>only</b> displayed in the 'BLOCKER' section, it probably means that the client has started a transaction, but currently is at the client side code and doing <i>something</i>, meaning client side logic.<br>"
		                                                                  + "</html>");
		setLocalToolTipTextOnTableColumnHeader("multiSampled",              "<html>The session_id is still executing the <b>same</b> SQL Statement as it did in the previous sample.</html>");
		setLocalToolTipTextOnTableColumnHeader("ImBlockedBySessionId",      "<html>This session_id is blocked by some other session_id</html>");
		setLocalToolTipTextOnTableColumnHeader("ImBlockingOtherSessionIds", "<html>This session_id is <b>blocking</b> other session_id's, This is a list of This session_id is blocked by some other session_id's which this This session_id is blocked by some other session_id is blocking.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasSqlText",                "<html>Checkbox to indicate that 'lastKnownSql' column has a value<br><b>Note:</b> Hower over this cell to see the SQL Statement.</html>");
		setLocalToolTipTextOnTableColumnHeader("HasQueryplan",              "<html>Checkbox to indicate that 'query_plan' column has a value<br><b>Note:</b> Hower over this cell to see the Query plan.</html>");
		setLocalToolTipTextOnTableColumnHeader("ExecTimeInMs",              "<html>How many milliseconds has this session_id been executing the current SQL Statement</html>");
		setLocalToolTipTextOnTableColumnHeader("UsefullExecTime",           "<html>More or less same thing as the column 'ExecTimeInMs' but it subtracts the 'wait_time'...<br>The idea is to display <i>time</i> used on something <i>usefull</i>, e.g. Not in sleep mode.</html>");
		setLocalToolTipTextOnTableColumnHeader("lastKnownSql",              "<html>"
		                                                                  + "  Last SQL Statement executed by this session_id.<br>"
		                                                                  + "  If 'monSource' is in status:<br>"
		                                                                  + "  <ul> "
		                                                                  + "    <li>ACTIVE  = SQL Statement that is currently executing.</li> "
		                                                                  + "    <li>BLOCKER = The <b>last</b> SQL Statement that was executed by this session_id. <br>"
		                                                                  + "        <b>Note:</b> This might <b>not</b> be the SQL Statement that caused the blocking...<br>"
		                                                                  + "        Meaning the session_id still holds lock(s) that an earlier issued SQL Statement (in the same transaction) is responsible for.</li> "
		                                                                  + "  </ul>"
		                                                                  + "  session_id's can be in both the 'ACTIVE' and 'BLOCKER' section. That is if the session_id is both blocking and currently executing a statement.<br>"
		                                                                  + "  If the session_id is <b>only</b> displayed in the 'BLOCKER' section, it probably means that the client has started a transaction, but currently is at the client side code and doing <i>something</i>, meaning client side logic is in play.<br>"
		                                                                  + "</html>");
		setLocalToolTipTextOnTableColumnHeader("query_plan",                "<html>Query Plan for the SQL-Statement that is currently executing.<br><b>Note:</b> Only valid for the 'ACTIVE' sessions.</html>");
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("session_id");
		pkCols.add("monSource");     // the "spid" can be in both ACTIVE and BLOCKER section

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql1 =
			"SELECT  \n" +
			"    monSource    = convert(varchar(10), 'ACTIVE'), \n" +
			"    multiSampled = convert(varchar(10), ''), \n" +
			"    des.login_name, \n" +
			"    des.session_id, \n" +
			"    ImBlockedBySessionId = der.blocking_session_id, \n" +
			"    ImBlockingOtherSessionIds = convert(varchar(512), ''), \n" +
			"    des.status, \n" +
			"    der.command, \n" +
			"    des.[HOST_NAME], \n" +
			"    HasSqlText   = convert(bit,0), \n" +
			"    HasQueryplan = convert(bit,0), \n" +
//			"    DB_NAME(der.database_id) AS database_name, \n" +
			"    (select db.name from sys.databases db where db.database_id = der.database_id) AS database_name, \n" +
			"    des.cpu_time, \n" +
			"    des.reads, \n" +
			"    des.logical_reads, \n" +
			"    des.writes, \n" +
//			"    dec.last_write , \n" +
			"    der.start_time, \n" +
			"    ExecTimeInMs    = CASE WHEN datediff(day, der.start_time, getdate()) >= 24 THEN -1 ELSE  datediff(ms, der.start_time, getdate()) END, \n" +               // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"    UsefullExecTime = CASE WHEN datediff(day, der.start_time, getdate()) >= 24 THEN -1 ELSE (datediff(ms, der.start_time, getdate()) - der.wait_time) END, \n" + // protect from: Msg 535: Difference of two datetime fields caused overflow at runtime. above 24 days or so, the MS difference is overflowned
			"    des.[program_name], \n" +
			"    der.wait_type, \n" +
			"    der.wait_time, \n" +
			"    der.last_wait_type, \n" +
			"    der.wait_resource, \n" +
			"    CASE des.transaction_isolation_level \n" +
			"        WHEN 0 THEN 'Unspecified' \n" +
			"        WHEN 1 THEN 'ReadUncommitted' \n" +
			"        WHEN 2 THEN 'ReadCommitted' \n" +
			"        WHEN 3 THEN 'Repeatable' \n" +
			"        WHEN 4 THEN 'Serializable' \n" +
			"        WHEN 5 THEN 'Snapshot' \n" +
			"    END AS transaction_isolation_level, \n" +
//			"    OBJECT_NAME(dest.objectid, der.database_id) AS OBJECT_NAME, \n" +
			"    SUBSTRING(dest.text, der.statement_start_offset / 2,  \n" +
			"        ( CASE WHEN der.statement_end_offset = -1  \n" +
			"               THEN DATALENGTH(dest.text)  \n" +
			"               ELSE der.statement_end_offset  \n" +
			"          END - der.statement_start_offset ) / 2) AS [lastKnownSql], \n" +
			"    deqp.query_plan \n" +
			"FROM sys.dm_exec_sessions des \n" +
			"LEFT JOIN sys.dm_exec_requests der ON des.session_id = der.session_id \n" +
//			"LEFT JOIN sys.dm_exec_connections dec ON des.session_id = dec.session_id \n" +
			"CROSS APPLY sys.dm_exec_sql_text(der.sql_handle) dest \n" +
			"CROSS APPLY sys.dm_exec_query_plan(der.plan_handle) deqp \n" +
			"WHERE des.session_id != @@spid";

		String sql2 = 
			"SELECT  \n" +
			"    monSource    = convert(varchar(10), 'BLOCKER'),  \n" +
			"    multiSampled = convert(varchar(10), ''),  \n" +
			"    p1.loginame, --des.login_name \n" +
			"    p1.spid, --des.session_id, \n" +
			"    ImBlockedBySessionId = p1.blocked, --der.blocking_session_id, \n" +
			"    ImBlockingOtherSessionIds = convert(varchar(512), ''),  \n" +
			"    p1.status, --des.status, \n" +
			"    p1.cmd, --der.command, \n" +
			"    p1.hostname, --des.[HOST_NAME] \n" +
			"    HasSqlText   = convert(bit,0),  \n" +
			"    HasQueryplan = convert(bit,0),  \n" +
//			"    DB_NAME(p1.dbid) AS database_name,  \n" +
			"    (select db.name from sys.databases db where db.database_id = p1.dbid) AS database_name, \n" +
			"    p1.cpu, --des.cpu_time, \n" +
			"    999999, --des.reads ,  \n" +
			"    999999, --des.logical_reads ,  \n" +
			"    999999, --des.writes ,  \n" +
			"    p1.last_batch, --der.start_time,  \n" +
			"    ExecTimeInMs    = CASE WHEN datediff(day, p1.last_batch, getdate()) >= 24 THEN -1 ELSE  datediff(ms, p1.last_batch, getdate()) END,  \n" +
			"    UsefullExecTime = CASE WHEN datediff(day, p1.last_batch, getdate()) >= 24 THEN -1 ELSE (datediff(ms, p1.last_batch, getdate()) - p1.waittime) END,  \n" +
			"    p1.program_name, --des.[program_name] ,  \n" +
			"    p1.waittype, --der.wait_type ,  \n" +
			"    p1.waittime, --der.wait_time ,  \n" +
			"    p1.waittype, --der.last_wait_type ,  \n" +
			"    p1.waitresource, --der.wait_resource ,  \n" +
			"    'unknown', \n" +
			"--    CASE des.transaction_isolation_level  \n" +
			"--        WHEN 0 THEN 'Unspecified'  \n" +
			"--        WHEN 1 THEN 'ReadUncommitted'  \n" +
			"--        WHEN 2 THEN 'ReadCommitted'  \n" +
			"--        WHEN 3 THEN 'Repeatable'  \n" +
			"--        WHEN 4 THEN 'Serializable'  \n" +
			"--        WHEN 5 THEN 'Snapshot'  \n" +
			"--    END AS transaction_isolation_level ,  \n" +
//			"    'unknown', --OBJECT_NAME(dest.objectid, der.database_id) AS OBJECT_NAME ,  \n" +
			"--    SUBSTRING(dest.text, der.statement_start_offset / 2,   \n" +
			"--        ( CASE WHEN der.statement_end_offset = -1   \n" +
			"--               THEN DATALENGTH(dest.text)   \n" +
			"--               ELSE der.statement_end_offset   \n" +
			"--          END - der.statement_start_offset ) / 2) AS [lastKnownSql] ,  \n" +
			"    dest.text, \n" +
			"    '' --deqp.query_plan  \n" +
			"FROM sys.sysprocesses p1 \n" +
			"CROSS APPLY sys.dm_exec_sql_text(p1.sql_handle) dest  \n" +
			"WHERE p1.spid in (select p2.blocked from sys.sysprocesses p2 where p2.blocked > 0) \n" + 
			"";
			

		return 
			sql1 +
			"\n" +
			"UNION ALL \n" +
			"\n" +
			sql2;
	}




	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("lastKnownSql".equals(colName))
		{
			return cellValue == null ? null : toHtmlString( cellValue.toString() );
		}

		if ("query_plan".equals(colName))
		{
			if (cellValue == null)
				return null;
			
			String formatedXml = new XmlFormatter().format(cellValue.toString());
			return toHtmlString( formatedXml );
		}

		// 'HasSqlText' STUFF
		if ("HasSqlText".equals(colName))
		{
			// Find 'MonSqlText' column, is so get it and set it as the tool tip
			int pos_MonSqlText = findColumn("lastKnownSql");
			if (pos_MonSqlText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_MonSqlText);
				if (cellVal instanceof String)
				{
					return toHtmlString((String) cellVal);
					//return (String) cellVal;
				}
			}
		}

		// 'HasQueryplan' STUFF
		if ("HasQueryplan".equals(colName))
		{
			// Find 'ShowPlanText' column, is so get it and set it as the tool tip
			int pos_ShowPlanText = findColumn("query_plan");
			if (pos_ShowPlanText > 0)
			{
				Object cellVal = getValueAt(modelRow, pos_ShowPlanText);
				if (cellVal instanceof String)
					return (String) cellVal;
			}
		}
		
		if ("wait_type".equals(colName) || "last_wait_type".equals(colName))
		{
			if (cellValue == null)
				return null;
			
			return SqlServerWaitTypeDictionary.getInstance().getDescriptionHtml((String)cellValue);
		}

		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
	/** add HTML around the string, and translate line breaks into <br> */
	private String toHtmlString(String in)
	{
		if (in.indexOf("<html>")>=0 || in.indexOf("<HTML>")>=0)
			return in;

		String str = StringUtil.makeApproxLineBreak(in, 150, 10, "\n");
		str = str.replace("<","&lt;").replace(">","&gt;");
		str = str.replaceAll("\\n", "<br>");
		
		return "<html><pre>" + str + "</pre></html>";
	}

	
	
	
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get Query Plan", getName()+".sample.showplan"   , Boolean.class, conf.getBooleanProperty(getName()+".sample.showplan"   , true ), true, "Also get queryplan" ));
		list.add(new CmSettingsHelper("Get SQL Text",   getName()+".sample.monSqltext" , Boolean.class, conf.getBooleanProperty(getName()+".sample.monSqltext" , true ), true, "Also get SQL Text"  ));

		return list;
	}

	
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		// use CHECKBOX for some columns of type bit/Boolean
		String colName = getColumnName(columnIndex);

		if      ("HasSqlText"     .equals(colName)) return Boolean.class;
		else if ("HasQueryplan"   .equals(colName)) return Boolean.class;
		else return super.getColumnClass(columnIndex);
	}

	/** 
	 * Fill in the WaitEventDesc column with data from
	 * MonTableDictionary.. transforms a WaitEventId -> text description
	 * This so we do not have to do a subselect in the query that gets data
	 * doing it this way, means better performance, since the values are cached locally in memory
	 * Also do post lookups of dbcc sqltext, sp_showplan, dbcc stacktrace
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
//	public void localCalculation(CounterSample newSample)
	{
//		long startTime = System.currentTimeMillis();

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getShowplan       = conf == null ? true : conf.getBooleanProperty(getName()+".sample.showplan",       true);
		boolean getMonSqltext     = conf == null ? true : conf.getBooleanProperty(getName()+".sample.monSqltext",     true);
//		boolean getDbccSqltext    = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccSqltext",    false);
//		boolean getProcCallStack  = conf == null ? true : conf.getBooleanProperty(getName()+".sample.procCallStack",  true);
//		boolean getDbccStacktrace = conf == null ? false: conf.getBooleanProperty(getName()+".sample.dbccStacktrace", false);

		// Where are various columns located in the Vector 
		int pos_SPID = -1;
//		int pos_WaitEventID        = -1, pos_WaitEventDesc  = -1, pos_WaitClassDesc = -1;
		int pos_HasShowPlan        = -1, pos_ShowPlanText   = -1;
		int pos_HasMonSqlText      = -1, pos_MonSqlText     = -1;
//		int pos_HasDbccSqlText     = -1, pos_DbccSqlText    = -1;
//		int pos_HasProcCallStack   = -1, pos_ProcCallStack  = -1;
//		int pos_HasStacktrace      = -1, pos_DbccStacktrace = -1;
		int pos_BlockingOtherSpids = -1, pos_BlockingSPID   = -1;
		int pos_multiSampled       = -1;
		int pos_StartTime          = -1;
//		int waitEventID = 0;
//		String waitEventDesc = "";
//		String waitClassDesc = "";
		CounterSample counters = diffData;
//		CounterSample counters = newSample;

		if ( ! MonTablesDictionaryManager.hasInstance() )
			return;
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

		if (counters == null)
			return;

		// Find column Id's
		List<String> colNames = counters.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (false) ; // Dummy on first if, to make "move" easier 
//			else if (colName.equals("WaitEventID"))                pos_WaitEventID        = colId;
//			else if (colName.equals("WaitEventDesc"))              pos_WaitEventDesc      = colId;
//			else if (colName.equals("WaitClassDesc"))              pos_WaitClassDesc      = colId;
//			else if (colName.equals("SPID"))                       pos_SPID               = colId;
			else if (colName.equals("session_id"))                 pos_SPID               = colId;
			else if (colName.equals("HasQueryplan"))               pos_HasShowPlan        = colId;
			else if (colName.equals("query_plan"))                 pos_ShowPlanText       = colId;
			else if (colName.equals("HasSqlText"))                 pos_HasMonSqlText      = colId;
			else if (colName.equals("lastKnownSql"))               pos_MonSqlText         = colId;
//			else if (colName.equals("HasDbccSqlText"))             pos_HasDbccSqlText     = colId;
//			else if (colName.equals("DbccSqlText"))                pos_DbccSqlText        = colId;
//			else if (colName.equals("HasProcCallStack"))           pos_HasProcCallStack   = colId;
//			else if (colName.equals("ProcCallStack"))              pos_ProcCallStack      = colId;
//			else if (colName.equals("HasStacktrace"))              pos_HasStacktrace      = colId;
//			else if (colName.equals("DbccStacktrace"))             pos_DbccStacktrace     = colId;
			else if (colName.equals("ImBlockingOtherSessionIds"))  pos_BlockingOtherSpids = colId;
			else if (colName.equals("ImBlockedBySessionId"))       pos_BlockingSPID       = colId;
			else if (colName.equals("multiSampled"))               pos_multiSampled       = colId;
//			else if (colName.equals("StartTime"))                  pos_StartTime          = colId;
			else if (colName.equals("start_time"))                 pos_StartTime          = colId;

//			// Noo need to continue, we got all our columns
//			if (    pos_WaitEventID        >= 0 && pos_WaitEventDesc  >= 0 
//			     && pos_WaitClassDesc      >= 0 && pos_SPID >= 0 
//			     && pos_HasShowPlan        >= 0 && pos_ShowPlanText   >= 0 
//			     && pos_HasMonSqlText      >= 0 && pos_MonSqlText     >= 0 
//			     && pos_HasDbccSqlText     >= 0 && pos_DbccSqlText    >= 0 
//			     && pos_HasProcCallStack   >= 0 && pos_ProcCallStack  >= 0 
//			     && pos_HasStacktrace      >= 0 && pos_DbccStacktrace >= 0 
//			     && pos_BlockingOtherSpids >= 0 && pos_BlockingSPID   >= 0
//			     && pos_multiSampled       >= 0
//			     && pos_StartTime          >= 0
//			   )
//				break;
		}

//		if (pos_WaitEventID < 0 || pos_WaitEventDesc < 0 || pos_WaitClassDesc < 0)
//		{
//			_logger.debug("Can't find the position for columns ('WaitEventID'="+pos_WaitEventID+", 'WaitEventDesc'="+pos_WaitEventDesc+", 'WaitClassDesc'="+pos_WaitClassDesc+")");
//			return;
//		}
		
		if (pos_SPID < 0 || pos_HasShowPlan < 0 || pos_ShowPlanText < 0)
		{
System.out.println("Can't find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
			_logger.debug("Can't find the position for columns ('SPID'="+pos_SPID+", 'HasShowPlan'="+pos_HasShowPlan+", 'ShowPlanText'="+pos_ShowPlanText+")");
			return;
		}

//		if (pos_HasDbccSqlText < 0 || pos_DbccSqlText < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasDbccSqlText'="+pos_HasDbccSqlText+", 'DbccSqlText'="+pos_DbccSqlText+")");
//			return;
//		}
//
//		if (pos_HasProcCallStack < 0 || pos_ProcCallStack < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasProcCallStack'="+pos_HasProcCallStack+", 'ProcCallStack'="+pos_ProcCallStack+")");
//			return;
//		}

		if (pos_HasMonSqlText < 0 || pos_MonSqlText < 0)
		{
System.out.println("Can't find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
			_logger.debug("Can't find the position for columns (''HasMonSqlText'="+pos_HasMonSqlText+", 'MonSqlText'="+pos_MonSqlText+")");
			return;
		}

//		if (pos_HasStacktrace < 0 || pos_DbccStacktrace < 0)
//		{
//			_logger.debug("Can't find the position for columns ('HasShowplan'="+pos_HasStacktrace+", 'DbccStacktrace'="+pos_DbccStacktrace+")");
//			return;
//		}
		
		if (pos_BlockingOtherSpids < 0 || pos_BlockingSPID < 0)
		{
System.out.println("Can't find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
			_logger.debug("Can't find the position for columns ('BlockingOtherSpids'="+pos_BlockingOtherSpids+", 'BlockingSPID'="+pos_BlockingSPID+")");
			return;
		}
		
		if (pos_multiSampled < 0)
		{
System.out.println("Can't find the position for columns ('multiSampled'="+pos_multiSampled+")");
			_logger.debug("Can't find the position for columns ('multiSampled'="+pos_multiSampled+")");
			return;
		}
		
		if (pos_StartTime < 0)
		{
System.out.println("Can't find the position for columns ('StartTime'="+pos_StartTime+")");
			_logger.debug("Can't find the position for columns ('StartTime'="+pos_StartTime+")");
			return;
		}
		
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			String thisRowPk = counters.getPkValue(rowId);
			int prevPkRowId = (prevSample == null) ? -1 : prevSample.getRowNumberForPkValue(thisRowPk);
			boolean prevPkExists = prevPkRowId >= 0;

//			Object o_waitEventId = counters.getValueAt(rowId, pos_WaitEventID);
			Object o_SPID        = counters.getValueAt(rowId, pos_SPID);
System.out.println("xxx: rowId="+rowId+", thisRowPk='"+thisRowPk+"', prevPkRowId="+prevPkRowId+", prevPkExists="+prevPkExists+", o_SPID="+o_SPID);

			if (prevPkExists)
			{
				Object o_this_StartTime = counters  .getValueAt(rowId,       pos_StartTime);
				Object o_prev_StartTime = prevSample.getValueAt(prevPkRowId, pos_StartTime);

				if (o_this_StartTime instanceof Timestamp && o_prev_StartTime instanceof Timestamp)
				{
					if (o_this_StartTime.equals(o_prev_StartTime))
						counters.setValueAt("YES", rowId, pos_multiSampled);
				}
			}

//			if (o_waitEventId instanceof Number)
//			{
//				waitEventID	  = ((Number)o_waitEventId).intValue();
//
//				if (mtd.hasWaitEventDescription(waitEventID))
//				{
//					waitEventDesc = mtd.getWaitEventDescription(waitEventID);
//					waitClassDesc = mtd.getWaitEventClassDescription(waitEventID);
//				}
//				else
//				{
//					waitEventDesc = "";
//					waitClassDesc = "";
//				}
//
//				//row.set( pos_WaitEventDesc, waitEventDesc);
//				counters.setValueAt(waitEventDesc, rowId, pos_WaitEventDesc);
//				counters.setValueAt(waitClassDesc, rowId, pos_WaitClassDesc);
//			}

			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();

//				String monSqlText    = "Not properly configured (need 'SQL batch capture' & 'max SQL text monitored').";
//				String dbccSqlText   = "User does not have: sa_role";
//				String procCallStack = "User does not have: sa_role";
//				String showplan      = "User does not have: sa_role";
//				String stacktrace    = "User does not have: sa_role";
//
//				if (getMonitorConfig("SQL batch capture") > 0 && getMonitorConfig("max SQL text monitored") > 0)
//				{
//					// monProcessSQLText; needs 'enable monitoring', 'SQL batch capture' and 'max SQL text monitored' configuration parameters for this monitoring table to collect data.
//					if (getMonSqltext)
//						monSqlText  = AseConnectionUtils.monSqlText(getCounterController().getMonConnection(), spid, true);
//					else
//						monSqlText = "This was disabled";
//					if (monSqlText == null)
//						monSqlText = "Not Available";
//				}
//				if (isServerRoleOrPermissionActive(AseConnectionUtils.SA_ROLE))
//				{
//					if (getDbccSqltext)
//						dbccSqlText  = AseConnectionUtils.dbccSqlText(getCounterController().getMonConnection(), spid, true);
//					else
//						dbccSqlText = "This was disabled";
//					if (dbccSqlText == null)
//						dbccSqlText = "Not Available";
//
//					if (getProcCallStack)
//						procCallStack  = AseConnectionUtils.monProcCallStack(getCounterController().getMonConnection(), spid, true);
//					else
//						procCallStack = "This was disabled";
//					if (procCallStack == null)
//						procCallStack = "Not Available";
//
//					if (getShowplan)
//						showplan = AseConnectionUtils.getShowplan(getCounterController().getMonConnection(), spid, "Showplan:", true);
//					else
//						showplan = "This was disabled";
//					if (showplan == null)
//						showplan = "Not Available";
//
//					if (getDbccStacktrace)
//						stacktrace = AseConnectionUtils.dbccStacktrace(getCounterController().getMonConnection(), spid, true, waitEventID);
//					else
//						stacktrace = "This was disabled";
//					if (stacktrace == null)
//						stacktrace = "Not Available";
//				}
//				boolean b = true;
//				b = !"This was disabled".equals(monSqlText)    && !"Not Available".equals(monSqlText)    && !monSqlText   .startsWith("Not properly configured");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);
//				counters.setValueAt(monSqlText,     rowId, pos_MonSqlText);
//
//				b = !"This was disabled".equals(dbccSqlText)   && !"Not Available".equals(dbccSqlText)   && !dbccSqlText  .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasDbccSqlText);
//				counters.setValueAt(dbccSqlText,    rowId, pos_DbccSqlText);
//
//				b = !"This was disabled".equals(procCallStack) && !"Not Available".equals(procCallStack) && !procCallStack.startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasProcCallStack);
//				counters.setValueAt(procCallStack,  rowId, pos_ProcCallStack);
//
//				b = !"This was disabled".equals(showplan)      && !"Not Available".equals(showplan)      && !showplan     .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);
//				counters.setValueAt(showplan,       rowId, pos_ShowPlanText);
//
//				b = !"This was disabled".equals(stacktrace)    && !"Not Available".equals(stacktrace)    && !stacktrace   .startsWith("User does not have");
//				counters.setValueAt(new Boolean(b), rowId, pos_HasStacktrace);
//				counters.setValueAt(stacktrace,     rowId, pos_DbccStacktrace);

				boolean b;
				Object obj;

				// SQL-Text check box
				obj = counters.getValueAt(rowId, pos_MonSqlText);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(new Boolean(b), rowId, pos_HasMonSqlText);

				// QueryPlan check box
				obj = counters.getValueAt(rowId, pos_ShowPlanText);
				b = (obj != null && obj instanceof String && StringUtil.hasValue((String)obj)); 
				counters.setValueAt(new Boolean(b), rowId, pos_HasShowPlan);


				// Get LIST of SPID's that I'm blocking
				String blockingList = getBlockingListStr(counters, spid, pos_BlockingSPID, pos_SPID);

				// This could be used to test that PCS.store() will truncate string size to the tables storage size
				//blockingList += "'1:aaa:0', '1:bbb:0', '1:ccc:0', '1:ddd:0', '1:eee:0', '1:fff:0', '1:ggg:0', '1:hhh:0', '1:iii:0', '1:jjj:0', '1:kkk:0', '1:lll:0', '1:mmm:0', '1:nnn:0', '1:ooo:0', '1:ppp:0', '1:qqq:0', '1:rrr:0', '1:sss:0', '1:ttt:0', '1:uuu:0', '1:vvv:0', '1:wwww:0', '1:xxx:0', '1:yyy:0', '1:zzz:0' -end-";

				counters.setValueAt(blockingList, rowId, pos_BlockingOtherSpids);
			}
		}
	}
	
//	@Override
//	protected Object clone() throws CloneNotSupportedException
//	{
//		// TODO Auto-generated method stub
//		return super.clone();
//	}

//	private String getBlockingListStr(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
//	{
//		StringBuilder sb = new StringBuilder();
//
//		// Loop on all diffData rows
//		int rows = counters.getRowCount();
//		for (int rowId=0; rowId < rows; rowId++)
//		{
//			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
//			if (o_BlockingSPID instanceof Number)
//			{
//				Number thisRow = (Number)o_BlockingSPID;
//				if (thisRow.intValue() == spid)
//				{
//					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
//					if (sb.length() == 0)
//						sb.append(o_SPID);
//					else
//						sb.append(", ").append(o_SPID);
//				}
//			}
//		}
//		return sb.toString();
//	}
	private String getBlockingListStr(CounterSample counters, int spid, int pos_BlockingSPID, int pos_SPID)
	{
		Set<Integer> spidSet = null;

		// Loop on all diffData rows
		int rows = counters.getRowCount();
		for (int rowId=0; rowId<rows; rowId++)
		{
			Object o_BlockingSPID = counters.getValueAt(rowId, pos_BlockingSPID);
			if (o_BlockingSPID instanceof Number)
			{
				Number thisRow = (Number)o_BlockingSPID;
				if (thisRow.intValue() == spid)
				{
					if (spidSet == null)
						spidSet = new LinkedHashSet<Integer>();

					Object o_SPID = counters.getValueAt(rowId, pos_SPID);
					if (o_SPID instanceof Number)
						spidSet.add( ((Number)o_SPID).intValue() );
				}
			}
		}
		if (spidSet == null)
			return "";
		return StringUtil.toCommaStr(spidSet);
	}

//	/** 
//	 * Get number of rows to save/request ddl information for 
//	 */
//	@Override
//	public int getMaxNumOfDdlsToPersist()
//	{
//		return Integer.MAX_VALUE; // Basically ALL Rows
//	}
//
//	/** 
//	 * Get Column names to where DBName and ObjectName is called, this must always return at least a array with 2 strings. 
//	 */
//	@Override
//	public String[] getDdlDetailsColNames()
//	{
//		String[] sa = {"dbname", "procname"};
//		return sa;
//	}
	
}
