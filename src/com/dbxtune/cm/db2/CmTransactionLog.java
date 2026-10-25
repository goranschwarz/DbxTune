/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cm.db2;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.events.AlarmDescriptionProviderBase;
import com.dbxtune.alarm.events.AlarmEvent;
import com.dbxtune.alarm.events.AlarmEventLowLogFreeSpace;
import com.dbxtune.alarm.events.db2.AlarmEventDb2LogArchiveFailed;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CmSettingsHelper.IntegerInputValidator;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MathUtils;

/**
 * The transaction log of the database: how full it is, who holds the oldest transaction, and if log archiving works.
 * <p>
 * A full transaction log (SQL0964C) stops all writes in the database, and it is one of the most common Db2 outages.
 *
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTransactionLog
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTransactionLog.class.getSimpleName();
	public static final String   SHORT_NAME       = "Transaction Log";
	public static final String   HTML_DESC        =
		"<html>" +
		"The <b>transaction log</b> of the database (one row per member).<br>" +
		"<br>" +
		"<b>LOG_USED_PCT</b> is how much of the log (primary + secondary log files) is used by transactions that are not yet finished. " +
		"When it reaches 100%, all changes in the database fail with <b>SQL0964C</b> (the transaction log is full).<br>" +
		"The log can only be reused when the <b>oldest</b> transaction is finished: the <code>OLDEST_XACT_*</code> columns show who holds it. " +
		"A forgotten open transaction (an application that never commits) can fill the log, even if it changed only one row.<br>" +
		"<br>" +
		"With archive logging (LOGARCHMETH1/2) the filled log files are copied to the archive. <b>ARCHIVE_METHOD1_STATUS</b> tells if that works.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "MON_GET_TRANSACTION_LOG", "MON_GET_UNIT_OF_WORK"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LOG_WRITE_MB",
		"LOG_READ_MB",
		"LOG_WRITE_TIME",
		"NUM_LOG_WRITE_IO",
		"LOG_READ_TIME",
		"NUM_LOG_READ_IO",
		"NUM_LOG_BUFFER_FULL",
		"NUM_LOG_PART_PAGE_IO",
		"LOG_HADR_WAIT_TIME",
		"LOG_HADR_WAITS_TOTAL"
	};

	// The counters starts from 0 when the database is activated again
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmTransactionLog(counterController, guiController);
	}

	public CmTransactionLog(ICounterController counterController, IGuiController guiController)
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

		setCounterController(counterController);
		setGuiController(guiController);

		addTrendGraphs();

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_LOG_USED_PCT   = "LogUsedPct";
	public static final String GRAPH_NAME_LOG_WRITE_MB   = "LogWriteMb";
	public static final String GRAPH_NAME_LOG_WRITE_TIME = "LogWriteTime";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LOG_USED_PCT,
			"Transaction Log Used in Percent",                                // Menu CheckBox text
			"Transaction Log Used in Percent (" + SHORT_NAME + ")",           // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_PERCENT, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Log Used Pct" },
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			true,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOG_WRITE_MB,
			"Transaction Log Written and Read, MB per Second",                // Menu CheckBox text
			"Transaction Log Written and Read, MB per Second (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.AUTO),
			new String[] { "Log Written MB/sec", "Log Read MB/sec" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_LOG_WRITE_TIME,
			"Transaction Log Write Time, Average ms per Write",               // Menu CheckBox text
			"Transaction Log Write Time, Average ms per Write (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.AUTO),
			new String[] { "Avg ms per Log Write" },
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();
		pkCols.add("MEMBER");
		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// NOTE: TOTAL_LOG_AVAILABLE is -1 with "infinite logging" (LOGSECOND = -1), then LOG_USED_PCT and LOG_AVAILABLE_MB are NULL
		// NOTE: LOG_WRITES and LOG_READS are in 4 KB pages
		// NOTE: The oldest transaction: APPLID_HOLDING_OLDEST_XACT is an application handle (not an application id), joined with MON_GET_UNIT_OF_WORK for who it is
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     l.MEMBER \n"
			+ "    ,CURRENT SERVER                                                                                  as DB_NAME \n"
			+ "    ,CAST(CASE WHEN l.TOTAL_LOG_AVAILABLE >= 0 AND (l.TOTAL_LOG_USED + l.TOTAL_LOG_AVAILABLE) > 0 \n"
			+ "               THEN l.TOTAL_LOG_USED * 100.0 / (l.TOTAL_LOG_USED + l.TOTAL_LOG_AVAILABLE) END AS DECIMAL(5,1)) as LOG_USED_PCT \n"
			+ "    ,CAST(l.TOTAL_LOG_USED / 1048576.0 AS DECIMAL(15,1))                                             as LOG_USED_MB \n"
			+ "    ,CAST(CASE WHEN l.TOTAL_LOG_AVAILABLE >= 0 THEN l.TOTAL_LOG_AVAILABLE / 1048576.0 END AS DECIMAL(15,1))                     as LOG_AVAILABLE_MB \n"
			+ "    ,CAST(CASE WHEN l.TOTAL_LOG_AVAILABLE >= 0 THEN (l.TOTAL_LOG_USED + l.TOTAL_LOG_AVAILABLE) / 1048576.0 END AS DECIMAL(15,1)) as LOG_TOTAL_MB \n"
			+ "    ,CAST(l.TOT_LOG_USED_TOP / 1048576.0 AS DECIMAL(15,1))                                           as LOG_USED_TOP_MB \n"
			+ "    ,CAST(l.SEC_LOG_USED_TOP / 1048576.0 AS DECIMAL(15,1))                                           as SEC_LOG_USED_TOP_MB \n"
			+ "    ,l.SEC_LOGS_ALLOCATED \n"
			+ "    ,l.APPLID_HOLDING_OLDEST_XACT                                                                     as OLDEST_XACT_APPL_HANDLE \n"
			+ "    ,u.SESSION_AUTH_ID                                                                                as OLDEST_XACT_AUTH_ID \n"
			// NOTE: APPLICATION_NAME from MON_GET_CONNECTION: MON_GET_UNIT_OF_WORK has it first in 11.5 (the WHERE gives NULL when nobody holds the log)
			+ "    ,(select c.APPLICATION_NAME from table(MON_GET_CONNECTION(u.APPLICATION_HANDLE, l.MEMBER)) c where c.APPLICATION_HANDLE = u.APPLICATION_HANDLE fetch first 1 rows only) as OLDEST_XACT_APPL_NAME \n"
			+ "    ,u.CLIENT_HOSTNAME                                                                                as OLDEST_XACT_CLIENT_HOST \n"
			+ "    ,u.WORKLOAD_OCCURRENCE_STATE                                                                      as OLDEST_XACT_STATE \n"
			+ "    ,u.UOW_START_TIME                                                                                 as OLDEST_XACT_START_TIME \n"
			+ "    ,SECONDS_BETWEEN(CURRENT TIMESTAMP, u.UOW_START_TIME)                                              as OLDEST_XACT_AGE_SEC \n"
			+ "    ,CAST(u.UOW_LOG_SPACE_USED / 1048576.0 AS DECIMAL(15,1))                                          as OLDEST_XACT_LOG_USED_MB \n"
			+ "    ,CAST(l.LOG_HELD_BY_DIRTY_PAGES / 1048576.0 AS DECIMAL(15,1))                                    as LOG_HELD_BY_DIRTY_PAGES_MB \n"
			+ "    ,CAST(l.LOG_TO_REDO_FOR_RECOVERY / 1048576.0 AS DECIMAL(15,1))                                   as LOG_TO_REDO_FOR_RECOVERY_MB \n"
			+ "    ,l.FIRST_ACTIVE_LOG \n"
			+ "    ,l.CURRENT_ACTIVE_LOG \n"
			+ "    ,l.LAST_ACTIVE_LOG \n"
			+ "    ,l.CURRENT_ARCHIVE_LOG \n"
			+ "    ,(select CAST(c.VALUE AS VARCHAR(256)) from SYSIBMADM.DBCFG c where c.NAME = 'logarchmeth1' and c.MEMBER = l.MEMBER) as LOGARCHMETH1 \n"
			+ "    ,l.ARCHIVE_METHOD1_STATUS \n"
			+ "    ,l.METHOD1_NEXT_LOG_TO_ARCHIVE \n"
			+ "    ,l.METHOD1_FIRST_FAILURE \n"
			+ "    ,(select CAST(c.VALUE AS VARCHAR(256)) from SYSIBMADM.DBCFG c where c.NAME = 'logarchmeth2' and c.MEMBER = l.MEMBER) as LOGARCHMETH2 \n"
			+ "    ,l.ARCHIVE_METHOD2_STATUS \n"
			+ "    ,l.METHOD2_NEXT_LOG_TO_ARCHIVE \n"
			+ "    ,l.METHOD2_FIRST_FAILURE \n"
			+ "    ,CAST(l.LOG_WRITES * 4096 / 1048576.0 AS DECIMAL(19,1))                                         as LOG_WRITE_MB \n"
			+ "    ,CAST(l.LOG_READS  * 4096 / 1048576.0 AS DECIMAL(19,1))                                         as LOG_READ_MB \n"
			+ "    ,l.LOG_WRITE_TIME \n"
			+ "    ,l.NUM_LOG_WRITE_IO \n"
			+ "    ,CAST(CASE WHEN l.NUM_LOG_WRITE_IO > 0 THEN l.LOG_WRITE_TIME * 1.0 / l.NUM_LOG_WRITE_IO END AS DECIMAL(12,3)) as AVG_LOG_WRITE_MS \n"
			+ "    ,l.LOG_READ_TIME \n"
			+ "    ,l.NUM_LOG_READ_IO \n"
			+ "    ,l.NUM_LOG_BUFFER_FULL \n"
			+ "    ,l.NUM_LOG_PART_PAGE_IO \n"
			+ "    ,l.LOG_HADR_WAIT_TIME \n"
			+ "    ,l.LOG_HADR_WAITS_TOTAL \n"
			+ "    ,l.NUM_INDOUBT_TRANS \n"
			+ "from table(MON_GET_TRANSACTION_LOG(-2)) l \n"
			+ "left join table(MON_GET_UNIT_OF_WORK(NULL, -2)) u on u.APPLICATION_HANDLE = l.APPLID_HOLDING_OLDEST_XACT and u.MEMBER = l.MEMBER \n"
			+ "order by l.MEMBER \n"
			+ "";

		return sql;
	}

	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return Db2CmUtils.checkFunctionsAreExecutable(this, conn, "MON_GET_TRANSACTION_LOG(-2)", "MON_GET_UNIT_OF_WORK(NULL, -2)", "MON_GET_CONNECTION(NULL, -2)");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable(CM_NAME, "The transaction log of the database: how full it is, who holds the oldest transaction, and if log archiving works.");

			mtd.addColumn(CM_NAME, "MEMBER",                      "<html>The database member (0 if this is not pureScale or DPF).</html>");
			mtd.addColumn(CM_NAME, "DB_NAME",                     "<html>The database.</html>");
			mtd.addColumn(CM_NAME, "LOG_USED_PCT",                "<html>How much of the log is used by transactions that are not finished, in percent of all log space (primary + secondary log files).<br>"
			                                                    + "<b>Formula</b>: TOTAL_LOG_USED / (TOTAL_LOG_USED + TOTAL_LOG_AVAILABLE) * 100<br>"
			                                                    + "At 100% all changes fail with SQL0964C (the transaction log is full). The log can only be reused when the oldest transaction (OLDEST_XACT_*) has finished.<br>"
			                                                    + "NULL with infinite logging (LOGSECOND = -1).<br>"
			                                                    + "<b>Note</b>: A single transaction can also be stopped earlier by MAX_LOG (percent of the primary log one transaction may use) or NUM_LOG_SPAN (how many log files it may span).<br>"
			                                                    + "<b>Alarm</b>: LogUsedPct</html>");
			mtd.addColumn(CM_NAME, "LOG_USED_MB",                 "<html>Log space used by transactions that are not finished, in MB. (TOTAL_LOG_USED)</html>");
			mtd.addColumn(CM_NAME, "LOG_AVAILABLE_MB",            "<html>Log space that is still available, in MB. NULL with infinite logging. (TOTAL_LOG_AVAILABLE)</html>");
			mtd.addColumn(CM_NAME, "LOG_TOTAL_MB",                "<html>All log space (primary + secondary log files), in MB. NULL with infinite logging.<br><b>Formula</b>: LOG_USED_MB + LOG_AVAILABLE_MB</html>");
			mtd.addColumn(CM_NAME, "LOG_USED_TOP_MB",             "<html>The most log space that has been used at the same time, since the database was activated, in MB. (TOT_LOG_USED_TOP)</html>");
			mtd.addColumn(CM_NAME, "SEC_LOG_USED_TOP_MB",         "<html>The most <b>secondary</b> log space that has been used at the same time, since the database was activated, in MB. (SEC_LOG_USED_TOP)<br>Above 0 means that the primary log files were not enough at some point.</html>");
			mtd.addColumn(CM_NAME, "SEC_LOGS_ALLOCATED",          "<html>Number of secondary log files that are allocated right now.</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_APPL_HANDLE",     "<html>The application (handle) that holds the <b>oldest transaction</b> that has written to the log. The log files from this transaction and on can not be reused until it is finished.<br>NULL if there is no open transaction that has written to the log. (APPLID_HOLDING_OLDEST_XACT)</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_AUTH_ID",         "<html>The user of the application that holds the oldest transaction. (MON_GET_UNIT_OF_WORK.SESSION_AUTH_ID)</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_APPL_NAME",       "<html>The name of the application that holds the oldest transaction.</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_CLIENT_HOST",     "<html>The client host of the application that holds the oldest transaction.</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_STATE",           "<html>What the application that holds the oldest transaction does right now (WORKLOAD_OCCURRENCE_STATE).<br><b>UOWWAIT</b> = it waits for the client: the transaction is open, but nothing runs (a forgotten commit?).<br><b>UOWEXEC</b> = it runs a statement.</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_START_TIME",      "<html>When the oldest transaction started (UOW_START_TIME).</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_AGE_SEC",         "<html>How old the oldest transaction is, in seconds.</html>");
			mtd.addColumn(CM_NAME, "OLDEST_XACT_LOG_USED_MB",     "<html>Log space written by the oldest transaction itself, in MB (UOW_LOG_SPACE_USED). It can be small, while it still keeps all the log written after it from being reused.</html>");
			mtd.addColumn(CM_NAME, "LOG_HELD_BY_DIRTY_PAGES_MB",  "<html>Log space that can't be reused because changed pages in the buffer pools are not yet written to disk, in MB. (LOG_HELD_BY_DIRTY_PAGES)</html>");
			mtd.addColumn(CM_NAME, "LOG_TO_REDO_FOR_RECOVERY_MB", "<html>Log that must be redone if the database crashed right now (crash recovery), in MB. (LOG_TO_REDO_FOR_RECOVERY)</html>");
			mtd.addColumn(CM_NAME, "FIRST_ACTIVE_LOG",            "<html>Number of the first active log file (S0000xxx.LOG).</html>");
			mtd.addColumn(CM_NAME, "CURRENT_ACTIVE_LOG",          "<html>Number of the log file Db2 writes to right now.</html>");
			mtd.addColumn(CM_NAME, "LAST_ACTIVE_LOG",             "<html>Number of the last active log file.</html>");
			mtd.addColumn(CM_NAME, "CURRENT_ARCHIVE_LOG",         "<html>Number of the log file that is being archived right now (NULL if none).</html>");
			mtd.addColumn(CM_NAME, "LOGARCHMETH1",                "<html>Where log files are archived (database configuration LOGARCHMETH1). <b>OFF</b> = circular logging: no archive, no online backup and no roll forward.</html>");
			mtd.addColumn(CM_NAME, "ARCHIVE_METHOD1_STATUS",      "<html>Status of the last archive attempt with LOGARCHMETH1: <b>1</b> = it worked, <b>0</b> = it failed (the logs wait in the active log path until it works). NULL = no attempt since the database was activated.<br><b>Note</b>: It is also 0 when LOGARCHMETH1 is OFF.<br><b>Alarm</b>: ArchiveFailed</html>");
			mtd.addColumn(CM_NAME, "METHOD1_NEXT_LOG_TO_ARCHIVE", "<html>The next log file number to archive with LOGARCHMETH1.</html>");
			mtd.addColumn(CM_NAME, "METHOD1_FIRST_FAILURE",       "<html>The first log file number that LOGARCHMETH1 failed to archive (when ARCHIVE_METHOD1_STATUS is 0).</html>");
			mtd.addColumn(CM_NAME, "LOGARCHMETH2",                "<html>A second archive target (database configuration LOGARCHMETH2), OFF if not used.</html>");
			mtd.addColumn(CM_NAME, "ARCHIVE_METHOD2_STATUS",      "<html>Status of the last archive attempt with LOGARCHMETH2: <b>1</b> = it worked, <b>0</b> = it failed. NULL = no attempt since the database was activated.<br><b>Note</b>: It is also 0 when LOGARCHMETH2 is OFF (not used), so the alarm only looks at it when LOGARCHMETH2 is set.<br><b>Alarm</b>: ArchiveFailed</html>");
			mtd.addColumn(CM_NAME, "METHOD2_NEXT_LOG_TO_ARCHIVE", "<html>The next log file number to archive with LOGARCHMETH2.</html>");
			mtd.addColumn(CM_NAME, "METHOD2_FIRST_FAILURE",       "<html>The first log file number that LOGARCHMETH2 failed to archive (when ARCHIVE_METHOD2_STATUS is 0).</html>");
			mtd.addColumn(CM_NAME, "LOG_WRITE_MB",                "<html>Log written, in MB. (LOG_WRITES, which is in 4 KB pages)</html>");
			mtd.addColumn(CM_NAME, "LOG_READ_MB",                 "<html>Log read, in MB (rollbacks, HADR, currently committed). (LOG_READS, which is in 4 KB pages)</html>");
			mtd.addColumn(CM_NAME, "LOG_WRITE_TIME",              "<html>Time spent writing the log, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "NUM_LOG_WRITE_IO",            "<html>Number of write I/Os to the log.</html>");
			mtd.addColumn(CM_NAME, "AVG_LOG_WRITE_MS",            "<html>Average time per log write I/O, in milliseconds. A slow log disk makes every commit slow.<br><b>Formula</b>: LOG_WRITE_TIME / NUM_LOG_WRITE_IO (in the Diff and Rate view: for this sample only)</html>");
			mtd.addColumn(CM_NAME, "LOG_READ_TIME",               "<html>Time spent reading the log, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "NUM_LOG_READ_IO",             "<html>Number of read I/Os from the log.</html>");
			mtd.addColumn(CM_NAME, "NUM_LOG_BUFFER_FULL",         "<html>Number of times the log buffer was full, so the writer had to wait. If this grows, think about a bigger LOGBUFSZ.</html>");
			mtd.addColumn(CM_NAME, "NUM_LOG_PART_PAGE_IO",        "<html>Number of log writes of a page that was only partly filled (commits that happen often write partial pages).</html>");
			mtd.addColumn(CM_NAME, "LOG_HADR_WAIT_TIME",          "<html>Time the log writer waited for the HADR standby, in milliseconds (0 without HADR).</html>");
			mtd.addColumn(CM_NAME, "LOG_HADR_WAITS_TOTAL",        "<html>Number of times the log writer waited for the HADR standby.</html>");
			mtd.addColumn(CM_NAME, "NUM_INDOUBT_TRANS",           "<html>Number of indoubt (two-phase commit) transactions. They hold the log until they are resolved.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	/** AVG_LOG_WRITE_MS: in the diff (and rate) data, the average for this sample only */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_writeTime = diffData.findColumn("LOG_WRITE_TIME");
		int pos_writeIo   = diffData.findColumn("NUM_LOG_WRITE_IO");
		int pos_avgMs     = diffData.findColumn("AVG_LOG_WRITE_MS");

		if (pos_writeTime == -1 || pos_writeIo == -1 || pos_avgMs == -1)
		{
			_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: LOG_WRITE_TIME, NUM_LOG_WRITE_IO, AVG_LOG_WRITE_MS.");
			return;
		}

		for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
		{
			long   writeIo   = diffData.getValueAsLong  (rowId, pos_writeIo  , 0L);
			double writeTime = diffData.getValueAsDouble(rowId, pos_writeTime, 0d);

			BigDecimal avgMs = (writeIo <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(writeTime / writeIo, 3);

			diffData.setValueAt(avgMs, rowId, pos_avgMs);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LOG_USED_PCT.equals(tgdp.getName()))
		{
			// The highest of the members (one row if this is not pureScale or DPF)
			Double max = null;
			for (int r = 0; r < this.getAbsRowCount(); r++)
			{
				Double val = this.getAbsValueAsDouble(r, "LOG_USED_PCT");
				if (val != null && (max == null || val > max))
					max = val;
			}

			Double[] arr = new Double[1];
			arr[0] = max == null ? 0d : max;
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LOG_WRITE_MB.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			Double writeMb = this.getRateValueSum("LOG_WRITE_MB");
			Double readMb  = this.getRateValueSum("LOG_READ_MB");
			arr[0] = writeMb == null ? 0d : writeMb;
			arr[1] = readMb  == null ? 0d : readMb;
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_LOG_WRITE_TIME.equals(tgdp.getName()))
		{
			// Average for this sample: sum of the time / sum of the I/Os (over the members)
			Double iosD  = this.getDiffValueSum("NUM_LOG_WRITE_IO");
			Double timeD = this.getDiffValueSum("LOG_WRITE_TIME");
			double ios   = iosD  == null ? 0 : iosD;
			double time  = timeD == null ? 0 : timeD;

			Double[] arr = new Double[1];
			arr[0] = ios <= 0 ? 0d : MathUtils.round(time / ios, 3);
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}

	@Override
	public void sendAlarmRequest()
	{
		if ( ! hasAbsData() )
			return;

		if ( ! AlarmHandler.hasInstance() )
			return;

		boolean debugPrint = Configuration.getCombinedConfiguration().getBooleanProperty("sendAlarmRequest.debug", _logger.isDebugEnabled());

		CountersModel cm   = this;
		Configuration conf = Configuration.getCombinedConfiguration();

		for (int r=0; r<cm.getAbsRowCount(); r++)
		{
			String dbname = cm.getAbsString(r, "DB_NAME");

			//-------------------------------------------------------
			// LogUsedPct
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("LogUsedPct"))
			{
				int    threshold  = conf.getIntProperty(PROPKEY_alarm_LogUsedPct, DEFAULT_alarm_LogUsedPct);
				Double logUsedPct = cm.getAbsValueAsDouble(r, "LOG_USED_PCT");

				if (debugPrint || _logger.isDebugEnabled())
					System.out.println("##### sendAlarmRequest(" + cm.getName() + "): LogUsedPct: dbname='" + dbname + "', threshold=" + threshold + ", LOG_USED_PCT='" + logUsedPct + "'.");

				// NULL = infinite logging
				if (logUsedPct != null && logUsedPct > threshold)
				{
					Double logAvailableMb = cm.getAbsValueAsDouble(r, "LOG_AVAILABLE_MB");
					int    freeMb         = logAvailableMb == null ? 0 : logAvailableMb.intValue();

					AlarmEvent ae = new AlarmEventLowLogFreeSpace(cm, dbname, freeMb, logUsedPct, (double) threshold);

					// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
					ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), threshold)
					{
						@Override
						public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
						{
							int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
							String label = "Transaction log of database '" + dbname + "'";

							if (rowId != -1)
							{
								// Always: (RAISE, RE-RAISE, CANCEL) - The row has who holds the oldest transaction
								String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
								String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);

								extendedDescHtml += "<br><br>" + cm.getGraphDataHistoryAsHtmlImage(GRAPH_NAME_LOG_USED_PCT);

								alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

								// Set CANCEL message (values that was found AFTER last raise/re-raise event)
								if (AlarmPhase.CANCEL.equals(phase))
								{
									String cancelMsg = getAlarmCancelText(label + ": used", toAlarmCancelValue(cm.getAbsValueAsDouble(rowId, "LOG_USED_PCT")), "%");
									alarmEvent.setCancelDescription(cancelMsg);
								}
							}
							else
							{
								if (AlarmPhase.CANCEL.equals(phase))
									alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
							}
						}
					});

					// Information about how to disable this alarm
					ae.createAlarmOptionsMessage(this, "LogUsedPct");

					AlarmHandler.getInstance().addAlarm(ae);
				}
			} // end: LogUsedPct

			//-------------------------------------------------------
			// ArchiveFailed (LOGARCHMETH1 and LOGARCHMETH2)
			//-------------------------------------------------------
			if (isSystemAlarmsForColumnEnabledAndInTimeRange("ArchiveFailed") && conf.getBooleanProperty(PROPKEY_alarm_ArchiveFailed, DEFAULT_alarm_ArchiveFailed))
			{
				for (int method = 1; method <= 2; method++)
				{
					// 1 = the last archive attempt worked, 0 = it failed, NULL = no attempt since the database was activated
					// NOTE: A method that is OFF also says 0 (tested 11.5.8: LOGARCHMETH2=OFF gave ARCHIVE_METHOD2_STATUS=0), so only look at configured methods
					Integer status     = cm.getAbsValueAsInteger(r, "ARCHIVE_METHOD" + method + "_STATUS");
					String  archTarget = cm.getAbsString(r, "LOGARCHMETH" + method);
					boolean isUsed     = archTarget != null && ! archTarget.trim().isEmpty() && ! "OFF".equalsIgnoreCase(archTarget.trim());

					if (debugPrint || _logger.isDebugEnabled())
						System.out.println("##### sendAlarmRequest(" + cm.getName() + "): ArchiveFailed: dbname='" + dbname + "', LOGARCHMETH" + method + "='" + archTarget + "', ARCHIVE_METHOD" + method + "_STATUS='" + status + "'.");

					if (isUsed && status != null && status == 0)
					{
						final int  archMethod     = method;
						Long       firstFailedLog = cm.getAbsValueAsLong(r, "METHOD" + method + "_FIRST_FAILURE");
						Long       currentLog     = cm.getAbsValueAsLong(r, "CURRENT_ACTIVE_LOG");

						AlarmEvent ae = new AlarmEventDb2LogArchiveFailed(cm, dbname, method, archTarget, firstFailedLog, currentLog);

						// The below is called from AlarmHandler on: Raise, RE-RAISE & CANCEL
						ae.setAlarmDescriptionProvider(this, new AlarmDescriptionProviderBase(cm.getPk(), cm.getAbsPkValue(r), null)
						{
							@Override
							public void setValues(CountersModel cm, AlarmEvent alarmEvent, AlarmPhase phase)
							{
								int    rowId = cm.getAbsRowIdForPkValue(getRowPk());
								String label = "Log archiving with LOGARCHMETH" + archMethod + " in database '" + dbname + "'";

								if (rowId != -1)
								{
									// Always: (RAISE, RE-RAISE, CANCEL)
									String extendedDescText = cm.toTextTableString(DATA_ABS, rowId);
									String extendedDescHtml = cm.toHtmlTableString(DATA_ABS, rowId, true, false, false);
									alarmEvent.setExtendedDescription(extendedDescText, extendedDescHtml);

									// Set CANCEL message (values that was found AFTER last raise/re-raise event)
									if (AlarmPhase.CANCEL.equals(phase))
										alarmEvent.setCancelDescription(label + " works again (next log to archive: " + cm.getAbsString(rowId, "METHOD" + archMethod + "_NEXT_LOG_TO_ARCHIVE") + ").");
								}
								else
								{
									if (AlarmPhase.CANCEL.equals(phase))
										alarmEvent.setCancelDescription(getAlarmCancelTextGone(label));
								}
							}
						});

						// Information about how to disable this alarm
						ae.createAlarmOptionsMessage(this, "ArchiveFailed");

						AlarmHandler.getInstance().addAlarm(ae);
					}
				}
			} // end: ArchiveFailed
		} // end: loop-rows
	}

	@Override
	public boolean isGraphDataHistoryEnabled(String name)
	{
		// ENABLED for the following graphs
		if (GRAPH_NAME_LOG_USED_PCT.equals(name)) return true;

		// default: DISABLED
		return false;
	}

	public static final String  PROPKEY_alarm_LogUsedPct    = CM_NAME + ".alarm.system.if.LOG_USED_PCT.gt";
	public static final int     DEFAULT_alarm_LogUsedPct    = 80;

	public static final String  PROPKEY_alarm_ArchiveFailed = CM_NAME + ".alarm.system.if.ARCHIVE_METHOD_STATUS.isFailing";
	public static final boolean DEFAULT_alarm_ArchiveFailed = true;

	@Override
	public List<CmSettingsHelper> getLocalAlarmSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		CmSettingsHelper.Type isAlarmSwitch = CmSettingsHelper.Type.IS_ALARM_SWITCH;

		list.add(new CmSettingsHelper("LogUsedPct"   , isAlarmSwitch, PROPKEY_alarm_LogUsedPct   , Integer.class, conf.getIntProperty    (PROPKEY_alarm_LogUsedPct   , DEFAULT_alarm_LogUsedPct   ), DEFAULT_alarm_LogUsedPct   , "If 'LOG_USED_PCT' is greater than ## percent then send 'AlarmEventLowLogFreeSpace'. The alarm tells who holds the oldest transaction (at 100% all changes fail with SQL0964C).", new IntegerInputValidator()));
		list.add(new CmSettingsHelper("ArchiveFailed", isAlarmSwitch, PROPKEY_alarm_ArchiveFailed, Boolean.class, conf.getBooleanProperty(PROPKEY_alarm_ArchiveFailed, DEFAULT_alarm_ArchiveFailed), DEFAULT_alarm_ArchiveFailed, "If 'ARCHIVE_METHOD1_STATUS' or 'ARCHIVE_METHOD2_STATUS' is 0 (the last archive attempt failed) then send 'AlarmEventDb2LogArchiveFailed'." ));

		return list;
	}
}
