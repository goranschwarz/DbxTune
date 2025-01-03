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
package com.dbxtune.cm.mysql;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHelper;
import com.dbxtune.cm.CmSettingsHelper;
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
public class CmSysSession
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysSession.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sessions";
	public static final String   HTML_DESC        = 
		"<html>"
		+ "<h4>Sessions</h4>"
		+ "Simply select * from sys.`x$session`"
		+ "</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"x$session"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"-dummy-culomn-that-will-never-exists-"
//			, "time"                   // The time in seconds that the thread has been in its current state.
//			, "statement_latency"      // How long the statement has been executing. <br> This column was added in MySQL 5.7.9.
//			, "lock_latency"           // The time spent waiting for locks by the current statement.
//			, "rows_examined"          // The number of rows read from storage engines by the current statement.
//			, "rows_sent"              // The number of rows returned by the current statement.
//			, "rows_affected"          // The number of rows affected by the current statement.
//			, "tmp_tables"             // The number of internal in-memory temporary tables created by the current statement.
//			, "tmp_disk_tables"        // The number of internal on-disk temporary tables created by the current statement.
//			, "last_statement_latency" // How long the last statement executed.
//			, "current_memory"         // The number of bytes allocated by the thread.
//			, "trx_latency"            // The wait time of the current transaction for the thread. <br> This column was added in MySQL 5.7.9.
			};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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

		return new CmSysSession(counterController, guiController);
	}

	public CmSysSession(ICounterController counterController, IGuiController guiController)
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
		setBackgroundDataPollingEnabled(true, false);
		
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
//		return new CmGlobalStatusPanel(this);
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
			// https://dev.mysql.com/doc/refman/5.7/en/innodb-buffer-pool-stats-table.html
			
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("x$session",  "These views summarize processlist information. They provide more complete information than the SHOW PROCESSLIST statement and the INFORMATION_SCHEMA PROCESSLIST table, and are also nonblocking. By default, rows are sorted by descending process time and descending wait time.");

			mtd.addColumn("x$session", "thd_id",                 "<html>The thread ID.</html>");
			mtd.addColumn("x$session", "conn_id",                "<html>The connection ID.</html>");
			mtd.addColumn("x$session", "user",                   "<html>The thread user or thread name.</html>");
			mtd.addColumn("x$session", "db",                     "<html>The default database for the thread, or NULL if there is none.</html>");
			mtd.addColumn("x$session", "command",                "<html>For foreground threads, the type of command the thread is executing on behalf of the client, or Sleep if the session is idle.</html>");
			mtd.addColumn("x$session", "state",                  "<html>An action, event, or state that indicates what the thread is doing.</html>");
			mtd.addColumn("x$session", "time",                   "<html>The time in seconds that the thread has been in its current state.</html>");
			mtd.addColumn("x$session", "current_statement",      "<html>The statement the thread is executing, or NULL if it is not executing any statement.</html>");
			mtd.addColumn("x$session", "statement_latency",      "<html>How long the statement has been executing. <br> This column was added in MySQL 5.7.9.</html>");
			mtd.addColumn("x$session", "progress",               "<html>The percentage of work completed for stages that support progress reporting. See Section 26.3, �sys Schema Progress Reporting�. <br> This column was added in MySQL 5.7.9.</html>");
			mtd.addColumn("x$session", "lock_latency",           "<html>The time spent waiting for locks by the current statement.</html>");
			mtd.addColumn("x$session", "rows_examined",          "<html>The number of rows read from storage engines by the current statement.</html>");
			mtd.addColumn("x$session", "rows_sent",              "<html>The number of rows returned by the current statement.</html>");
			mtd.addColumn("x$session", "rows_affected",          "<html>The number of rows affected by the current statement.</html>");
			mtd.addColumn("x$session", "tmp_tables",             "<html>The number of internal in-memory temporary tables created by the current statement.</html>");
			mtd.addColumn("x$session", "tmp_disk_tables",        "<html>The number of internal on-disk temporary tables created by the current statement.</html>");
			mtd.addColumn("x$session", "full_scan",              "<html>The number of full table scans performed by the current statement.</html>");
			mtd.addColumn("x$session", "last_statement",         "<html>The last statement executed by the thread, if there is no currently executing statement or wait.</html>");
			mtd.addColumn("x$session", "last_statement_latency", "<html>How long the last statement executed.</html>");
			mtd.addColumn("x$session", "current_memory",         "<html>The number of bytes allocated by the thread.</html>");
			mtd.addColumn("x$session", "last_wait",              "<html>The name of the most recent wait event for the thread.</html>");
			mtd.addColumn("x$session", "last_wait_latency",      "<html>The wait time of the most recent wait event for the thread.</html>");
			mtd.addColumn("x$session", "source",                 "<html>The source file and line number containing the instrumented code that produced the event.</html>");
			mtd.addColumn("x$session", "trx_latency",            "<html>The wait time of the current transaction for the thread. <br> This column was added in MySQL 5.7.9.</html>");
			mtd.addColumn("x$session", "trx_state",              "<html>The state for the current transaction for the thread. <br> This column was added in MySQL 5.7.9.</html>");
			mtd.addColumn("x$session", "trx_autocommit",         "<html>Whether autocommit mode was enabled when the current transaction started. <br> This column was added in MySQL 5.7.9.</html>");
			mtd.addColumn("x$session", "pid",                    "<html>The client process ID.<br> This column was added in MySQL 5.7.9.</html>");
			mtd.addColumn("x$session", "program_name",           "<html>The client program name.</html>");
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
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("thd_id");
		pkCols.add("conn_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		String sql = "select * from sys.`x$session`";

		return sql;
	}

	
	@Override
	public void sendAlarmRequest()
	{
		AlarmHelper.sendAlarmRequestForColumn(this, "program_name");
		AlarmHelper.sendAlarmRequestForColumn(this, "user");
	}
	
	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "program_name") );
		list.addAll( AlarmHelper.getLocalAlarmSettingsForColumn(this, "user") );
		
		return list;
	}
}
