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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.oracle;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CmSettingsHelper;
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
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmUndoStat
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmUndoStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "Undo";
	public static final String   HTML_DESC        =
		"<html>" +
		"How much undo that is generated and kept, in 10 minute periods (the newest period first, it is still being filled).<br>" +
		"<br>" +
		"Undo is the 'before image' of changed data: it is needed to roll back, and by queries to see the data as it was when they started.<br>" +
		"<ul>" +
		"  <li><code>ACTIVE_MB</code>    - undo of transactions that are not finished. It can not be reused.</li>" +
		"  <li><code>UNEXPIRED_MB</code> - undo of finished transactions, kept for queries (for <code>TUNED_UNDORETENTION</code> seconds).</li>" +
		"  <li><code>EXPIRED_MB</code>   - undo that is older than that. It is reused first.</li>" +
		"</ul>" +
		"If a query runs longer than the undo is kept (<code>MAXQUERYLEN</code> above <code>TUNED_UNDORETENTION</code>) it can fail with 'ORA-01555: snapshot too old', counted in <code>SSOLDERRCNT</code>.<br>" +
		"<code>NOSPACEERRCNT</code> counts transactions that failed because the undo tablespace was full (ORA-30036).<br>" +
		"Both errors also send an alarm from the tab 'Alert Log'.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$UNDOSTAT"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmUndoStat(counterController, guiController);
	}

	public CmUndoStat(ICounterController counterController, IGuiController guiController)
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
	public static final String  PROPKEY_sample_hours = CM_NAME + ".sample.hours";
	public static final int     DEFAULT_sample_hours = 1;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_hours, DEFAULT_sample_hours);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();

		list.add(new CmSettingsHelper("Show Periods for the last ## Hours", PROPKEY_sample_hours, Integer.class, conf.getIntProperty(PROPKEY_sample_hours, DEFAULT_sample_hours), DEFAULT_sample_hours, "Show the 10 minute periods of V$UNDOSTAT that started in the last ## hours. (Oracle keeps 4 days)" ));

		return list;
	}

	public static final String GRAPH_NAME_UNDO_MB       = "UndoMb";
	public static final String GRAPH_NAME_RETENTION_SEC = "UndoRetentionSec";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_UNDO_MB,
			"Undo, Active/Unexpired/Expired in MB", 	                // Menu CheckBox text
			"Undo Space in MB: Active (not finished transactions), Unexpired (kept for queries), Expired (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Active MB", "Unexpired MB", "Expired MB"},
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_RETENTION_SEC,
			"Undo, Retention and Longest Query in Seconds", 	                // Menu CheckBox text
			"Undo Retention (how long undo is kept) and the Longest Query, in Seconds (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_SECONDS, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] {"Tuned Undo Retention (sec)", "Longest Query (sec)"},
			LabelType.Static,
			TrendGraphDataPoint.Category.OTHER,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		// CON_ID is always 0 in a non-CDB (and in 11.2). With 'local undo' (12.2 and above) every container has it's own undo
		pkCols.add("CON_ID");
		pkCols.add("BEGIN_TIME");
		pkCols.add("UNDOTSN");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$UNDOSTAT", "V$PARAMETER");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// 11.2 and above
		String sql = getSql("CAST(0 as NUMBER(10))");

		// 12.1 and above: V$UNDOSTAT has CON_ID
		if (versionInfo.getLongVersion() >= Ver.ver(12,1))
		{
			String sqlCon = getSql("u.CON_ID");

			// The SQL for 12.1 and above can NOT be tested on all versions: so "dry-run" it, and if it fails use the 11.2 SQL (which works on all versions)
			// conn is null when we just want to "view" the SQL (the CM Properties dialog)
			if (conn == null)
			{
				sql = sqlCon;
			}
			else
			{
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select * from (" + sqlCon + ") where 1=2"))
				{
					sql = sqlCon;
				}
				catch (SQLException ex)
				{
					_logger.warn(CM_NAME + ": The SQL with CON_ID (version " + Ver.versionNumToStr(versionInfo.getLongVersion()) + ") failed, CON_ID will be 0. Caught: " + ex.getMessage().trim());
				}
			}
		}

		return sql;
	}

	private static String getSql(String CON_ID)
	{
		int hours = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_sample_hours, DEFAULT_sample_hours);

		// One row for every 10 minute period (the newest is the one that is being filled right now)
		// The view counts in blocks: the undo tablespace has the block size of the database
		// Note: CAST(... as NUMBER(#,1)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		return ""
			+ "select \n"
			+ "     " + CON_ID + " as CON_ID \n"
			+ "    ,u.BEGIN_TIME \n"
			+ "    ,u.END_TIME \n"
			+ "    ,u.UNDOTSN \n"
			+ "    ,CAST(u.UNDOBLKS      * p.BLOCK_SIZE / 1048576 as NUMBER(18,1)) as UNDO_MB \n"
			+ "    ,u.TXNCOUNT \n"
			+ "    ,u.MAXCONCURRENCY \n"
			+ "    ,u.MAXQUERYLEN \n"
			+ "    ,u.MAXQUERYID \n"
			+ "    ,u.TUNED_UNDORETENTION \n"
			+ "    ,CAST(u.ACTIVEBLKS    * p.BLOCK_SIZE / 1048576 as NUMBER(18,1)) as ACTIVE_MB \n"
			+ "    ,CAST(u.UNEXPIREDBLKS * p.BLOCK_SIZE / 1048576 as NUMBER(18,1)) as UNEXPIRED_MB \n"
			+ "    ,CAST(u.EXPIREDBLKS   * p.BLOCK_SIZE / 1048576 as NUMBER(18,1)) as EXPIRED_MB \n"
			+ "    ,u.SSOLDERRCNT \n"
			+ "    ,u.NOSPACEERRCNT \n"
			+ "    ,u.UNXPSTEALCNT \n"
			+ "from V$UNDOSTAT u \n"
			+ "cross join (select TO_NUMBER(VALUE) as BLOCK_SIZE from V$PARAMETER where NAME = 'db_block_size') p \n"
			+ "where u.BEGIN_TIME > SYSDATE - " + hours + " / 24 \n"
			+ "order by u.BEGIN_TIME desc \n"
			+ "";
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "How much undo that is generated and kept, in 10 minute periods (the newest period first).");

			mtd.addColumn(CM_NAME, "CON_ID",              "<html>Container ID. 0 = This is not a Container Database (or the version is below 12.1).</html>");
			mtd.addColumn(CM_NAME, "BEGIN_TIME",          "<html>Start of the 10 minute period.</html>");
			mtd.addColumn(CM_NAME, "END_TIME",            "<html>End of the period. For the newest period: the time of the latest update.</html>");
			mtd.addColumn(CM_NAME, "UNDOTSN",             "<html>Number of the undo tablespace that was active in the period.</html>");
			mtd.addColumn(CM_NAME, "UNDO_MB",             "<html>Undo generated in the period, in MB.<br><b>Formula</b>: UNDOBLKS * db_block_size / 1024 / 1024</html>");
			mtd.addColumn(CM_NAME, "TXNCOUNT",            "<html>Number of transactions in the period.</html>");
			mtd.addColumn(CM_NAME, "MAXCONCURRENCY",      "<html>Highest number of transactions that was running at the same time in the period.</html>");
			mtd.addColumn(CM_NAME, "MAXQUERYLEN",         "<html>The longest running query in the period, in seconds. If this is above TUNED_UNDORETENTION, that query risks 'ORA-01555: snapshot too old'.</html>");
			mtd.addColumn(CM_NAME, "MAXQUERYID",          "<html>SQL_ID of the longest running query in the period.</html>");
			mtd.addColumn(CM_NAME, "TUNED_UNDORETENTION", "<html>How many seconds Oracle keeps undo of finished transactions (it is tuned automatically, from the longest query and the free space in the undo tablespace).</html>");
			mtd.addColumn(CM_NAME, "ACTIVE_MB",           "<html>Undo of transactions that are not finished, in MB. It can not be reused.</html>");
			mtd.addColumn(CM_NAME, "UNEXPIRED_MB",        "<html>Undo of finished transactions that is still kept for queries, in MB.</html>");
			mtd.addColumn(CM_NAME, "EXPIRED_MB",          "<html>Undo that is older than the retention, in MB. It is reused first.</html>");
			mtd.addColumn(CM_NAME, "SSOLDERRCNT",         "<html>Number of 'ORA-01555: snapshot too old' errors in the period.</html>");
			mtd.addColumn(CM_NAME, "NOSPACEERRCNT",       "<html>Number of times a transaction failed because the undo tablespace was full (ORA-30036) in the period.</html>");
			mtd.addColumn(CM_NAME, "UNXPSTEALCNT",        "<html>Number of times Oracle had to take UNEXPIRED undo from another transaction to get space. Above 0 means: the undo tablespace is too small for the wanted retention.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0)
			return;

		// Both graphs use the NEWEST period (summed over the containers, if there are more than one)
		Timestamp newest = null;
		for (int r = 0; r < this.size(); r++)
		{
			Timestamp beginTime = this.getAbsValueAsTimestamp(r, "BEGIN_TIME");
			if (beginTime != null && (newest == null || beginTime.after(newest)))
				newest = beginTime;
		}
		if (newest == null)
			return;

		String[] colNames = null;
		if (GRAPH_NAME_UNDO_MB      .equals(tgdp.getName())) colNames = new String[] {"ACTIVE_MB", "UNEXPIRED_MB", "EXPIRED_MB"};
		if (GRAPH_NAME_RETENTION_SEC.equals(tgdp.getName())) colNames = new String[] {"TUNED_UNDORETENTION", "MAXQUERYLEN"};

		if (colNames != null)
		{
			boolean isSum = GRAPH_NAME_UNDO_MB.equals(tgdp.getName()); // MB: the sum of the containers, Seconds: the highest value
			Double[] arr = new Double[colNames.length];
			for (int i = 0; i < arr.length; i++)
			{
				double val = 0;
				for (int r = 0; r < this.size(); r++)
				{
					if ( ! newest.equals(this.getAbsValueAsTimestamp(r, "BEGIN_TIME")) )
						continue;

					double rowVal = this.getAbsValueAsDouble(r, colNames[i], 0d);
					val = isSum ? val + rowVal : Math.max(val, rowVal);
				}
				arr[i] = val;
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
