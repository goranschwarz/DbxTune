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
package com.dbxtune.cm.mysql;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoMySql;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmInnoDbMetrics
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmInnoDbMetrics.class.getSimpleName();
	public static final String   SHORT_NAME       = "InnoDB Metrics";
	public static final String   HTML_DESC        =
		"<html>" +
		"InnoDB's own counters (information_schema.INNODB_METRICS), only the ones that are enabled.<br>" +
		"<br>" +
		"<b>VALUE_TYPE</b> tells what <b>count</b> is:<br>" +
		"<ul>" +
		"  <li><b>counter</b> - it only grows, so Diff and Rate shows what happened in the sample (for example: rows read, deadlocks).</li>" +
		"  <li><b>gauge</b> - a value right now (for example: free pages in the buffer pool, history list length). It is shown as it is in Abs, Diff and Rate.</li>" +
		"</ul>" +
		"More counters can be enabled with: <code>SET GLOBAL innodb_monitor_enable = 'name_or_module%'</code><br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "INNODB_METRICS"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"count"};

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmInnoDbMetrics(counterController, guiController);
	}

	public CmInnoDbMetrics(ICounterController counterController, IGuiController guiController)
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
		setBackgroundDataPollingEnabled(true, false);

		setCounterController(counterController);
		setGuiController(guiController);

		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	/** If we can't read the view: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return MySqlCmUtils.checkViewsAreReadable(this, conn, "information_schema.INNODB_METRICS");
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "InnoDB's own counters (information_schema.INNODB_METRICS), only the ones that are enabled.");

			mtd.addColumn(CM_NAME, "name",       "<html>Name of the counter.</html>");
			mtd.addColumn(CM_NAME, "count",      "<html>The value.<br>"
			                                   + "For a <b>counter</b> (VALUE_TYPE): Abs is the value since the counter was enabled (normally since the server started), Diff and Rate is what happened in the sample.<br>"
			                                   + "For a <b>gauge</b> (VALUE_TYPE): the value right now, in Abs, Diff and Rate.</html>");
			mtd.addColumn(CM_NAME, "VALUE_TYPE", "<html><b>counter</b> = it only grows. <b>gauge</b> = a value right now.<br>"
			                                   + "<b>Formula</b>: gauge if 'type' is 'value', or if it is one of: ibuf_size, lock_row_lock_current_waits, os_log_pending_fsyncs, os_log_pending_writes (they are 'status_counter', but holds a value right now).</html>");
			mtd.addColumn(CM_NAME, "subsystem",  "<html>The part of InnoDB the counter belongs to (buffer, lock, transaction...).</html>");
			mtd.addColumn(CM_NAME, "type",       "<html>The type of counter, from INNODB_METRICS: value, status_counter, counter, set_owner, set_member.</html>");
			mtd.addColumn(CM_NAME, "comment",    "<html>What the counter is (from INNODB_METRICS).</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// MariaDB has the column ENABLED (1/0) instead of STATUS ('enabled'/'disabled')
		boolean isMariaDb = (versionInfo instanceof DbmsVersionInfoMySql) && ((DbmsVersionInfoMySql) versionInfo).isMariaDb();
		String  whereEnabled = isMariaDb ? "where ENABLED = 1 \n" : "where status = 'enabled' \n";

		// VALUE_TYPE: 'type' = 'value' is a gauge, and a few of the 'status_counter' rows are also a "value right now"
		String sql = ""
			+ "select /* ${cmCollectorName} */ \n"
			+ "     name \n"
			+ "    ,count \n"
			+ "    ,CASE WHEN type = 'value' \n"
			+ "            OR name IN ('ibuf_size', 'lock_row_lock_current_waits', 'os_log_pending_fsyncs', 'os_log_pending_writes') \n"
			+ "          THEN 'gauge' \n"
			+ "          ELSE 'counter' \n"
			+ "     END as VALUE_TYPE \n"
			+ "    ,subsystem \n"
			+ "    ,type \n"
			+ "    ,comment \n"
			+ "from information_schema.INNODB_METRICS \n"
			+ whereEnabled
			+ "";

		return sql;
	}

	/**
	 * A gauge (VALUE_TYPE = 'gauge') is a value right now, not a counter that only grows.<br>
	 * The framework makes a diff (and a rate) of EVERY row in a DIFF column, so for a gauge we put back the absolute value in diff.
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int pos_type  = diffData.findColumn("VALUE_TYPE");
		int pos_count = diffData.findColumn("count");
		if (pos_type == -1 || pos_count == -1)
			return;

		for (int r = 0; r < diffData.getRowCount(); r++)
		{
			if ( ! "gauge".equals(diffData.getValueAt(r, pos_type)) )
				continue;

			int absRow = newSample.getRowNumberForPkValue(diffData.getPkValue(r));
			if (absRow != -1)
				diffData.setValueAt(newSample.getValueAt(absRow, pos_count), r, pos_count);
		}
	}

	/** Same as in localCalculation(): for a gauge the rate is the absolute value (which is in diff by now) */
	@Override
	public void localCalculationRatePerSec(CounterSample rateData, CounterSample diffData)
	{
		int pos_type  = rateData.findColumn("VALUE_TYPE");
		int pos_count = rateData.findColumn("count");
		if (pos_type == -1 || pos_count == -1)
			return;

		for (int r = 0; r < rateData.getRowCount(); r++)
		{
			if ( ! "gauge".equals(rateData.getValueAt(r, pos_type)) )
				continue;

			// The rate is a BigDecimal with 1 decimal (as computeRatePerSec() makes it)
			int diffRow = diffData.getRowNumberForPkValue(rateData.getPkValue(r));
			if (diffRow != -1 && diffData.getValueAt(diffRow, pos_count) instanceof Number)
				rateData.setValueAt(new BigDecimal( ((Number)diffData.getValueAt(diffRow, pos_count)).doubleValue() ).setScale(1, RoundingMode.HALF_EVEN), r, pos_count);
		}
	}
}
