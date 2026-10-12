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
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
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
import com.dbxtune.utils.MathUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIoStatFile
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIoStatFile.class.getSimpleName();
	public static final String   SHORT_NAME       = "IO by File";
	public static final String   HTML_DESC        =
		"<html>" +
		"Disk I/O by file: one row for every data file and temp file, and one row for every other type of file (control file, log file, archive log, backups...).<br>" +
		"<br>" +
		"'Small' is single block requests, 'large' is multi block requests.<br>" +
		"The <code>*_AVG_MS</code> columns are the average time for a request in milliseconds (in 'diff' and 'rate': for the sample). This is where a slow disk shows:" +
		"<ul>" +
		"  <li><code>SMALL_READ_AVG_MS</code>  - a single block read from a data file. On spinning disks 5-10 ms is normal, on flash below 1 ms.</li>" +
		"  <li><code>SMALL_WRITE_AVG_MS</code> - for the 'Log File' row this is what a commit waits for.</li>" +
		"</ul>" +
		"<b>Note</b>: The times are only measured when the parameter <code>timed_statistics</code> is true (which is the default).<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {CM_NAME, "V$IOSTAT_FILE"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"TOTAL_MEGABYTES",
		"TOTAL_REQS",
		"SMALL_READ_MEGABYTES",
		"SMALL_WRITE_MEGABYTES",
		"LARGE_READ_MEGABYTES",
		"LARGE_WRITE_MEGABYTES",
		"SMALL_READ_REQS",
		"SMALL_WRITE_REQS",
		"SMALL_SYNC_READ_REQS",
		"LARGE_READ_REQS",
		"LARGE_WRITE_REQS",
		"SMALL_READ_SERVICETIME",
		"SMALL_WRITE_SERVICETIME",
		"SMALL_SYNC_READ_LATENCY",
		"LARGE_READ_SERVICETIME",
		"LARGE_WRITE_SERVICETIME",
		"RETRIES_ON_ERROR"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmIoStatFile(counterController, guiController);
	}

	public CmIoStatFile(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_MB_PER_SEC = "IoFileMbPerSec";
	public static final String GRAPH_NAME_AVG_MS     = "IoFileAvgMs";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_MB_PER_SEC,
			"IO by File Type, MB per Second", 	                // Menu CheckBox text
			"Disk IO in MB per Second, by File Type: Data File, Log File, Temp File... (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above
			-1);   // minimum height

		addTrendGraph(GRAPH_NAME_AVG_MS,
			"IO by File Type, Average ms per Request", 	                // Menu CheckBox text
			"Disk IO Average Milliseconds per Request in the sample, by File Type (" + SHORT_NAME + ")", // Graph Label
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			null,
			LabelType.Dynamic,
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

		pkCols.add("FILE_NO");
		pkCols.add("FILETYPE_ID");

		return pkCols;
	}

	/** If we can't read the views: disable the CM, and tell what to do about it */
	@Override
	public boolean checkDependsOnOther(DbxConnection conn)
	{
		return OracleCmUtils.checkViewsAreReadable(this, conn, "V$IOSTAT_FILE", "V$DATAFILE", "V$TEMPFILE");
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// One row for every data file and temp file (FILE_NO is the file number), and one row for every other file type (FILE_NO = 0)
		// FILE_NAME: for data files (FILETYPE_ID 2) from V$DATAFILE, for temp files (FILETYPE_ID 6) from V$TEMPFILE
		// The *_AVG_MS columns are since the instance was started here: for DIFF and RATE they are calculated for the sample in localCalculation()
		// This looks the same in all versions
		// Note: CAST(... as NUMBER(#,3)) is needed to keep decimals (a NUMBER without precision is treated as a BIGINT)
		String sql = ""
			+ "select \n"
			+ "     f.FILE_NO \n"
			+ "    ,f.FILETYPE_ID \n"
			+ "    ,f.FILETYPE_NAME \n"
			+ "    ,case when f.FILETYPE_ID = 2 then d.NAME when f.FILETYPE_ID = 6 then t.NAME end                               as FILE_NAME \n"
			+ "    ,f.SMALL_READ_MEGABYTES + f.SMALL_WRITE_MEGABYTES + f.LARGE_READ_MEGABYTES + f.LARGE_WRITE_MEGABYTES          as TOTAL_MEGABYTES \n"
			+ "    ,f.SMALL_READ_REQS      + f.SMALL_WRITE_REQS      + f.LARGE_READ_REQS      + f.LARGE_WRITE_REQS               as TOTAL_REQS \n"
			+ "    ,CAST(f.SMALL_READ_SERVICETIME  / NULLIF(f.SMALL_READ_REQS,  0) as NUMBER(18,3))                              as SMALL_READ_AVG_MS \n"
			+ "    ,CAST(f.SMALL_WRITE_SERVICETIME / NULLIF(f.SMALL_WRITE_REQS, 0) as NUMBER(18,3))                              as SMALL_WRITE_AVG_MS \n"
			+ "    ,CAST(f.LARGE_READ_SERVICETIME  / NULLIF(f.LARGE_READ_REQS,  0) as NUMBER(18,3))                              as LARGE_READ_AVG_MS \n"
			+ "    ,CAST(f.LARGE_WRITE_SERVICETIME / NULLIF(f.LARGE_WRITE_REQS, 0) as NUMBER(18,3))                              as LARGE_WRITE_AVG_MS \n"
			+ "    ,f.SMALL_READ_MEGABYTES \n"
			+ "    ,f.SMALL_WRITE_MEGABYTES \n"
			+ "    ,f.LARGE_READ_MEGABYTES \n"
			+ "    ,f.LARGE_WRITE_MEGABYTES \n"
			+ "    ,f.SMALL_READ_REQS \n"
			+ "    ,f.SMALL_WRITE_REQS \n"
			+ "    ,f.SMALL_SYNC_READ_REQS \n"
			+ "    ,f.LARGE_READ_REQS \n"
			+ "    ,f.LARGE_WRITE_REQS \n"
			+ "    ,f.SMALL_READ_SERVICETIME \n"
			+ "    ,f.SMALL_WRITE_SERVICETIME \n"
			+ "    ,f.SMALL_SYNC_READ_LATENCY \n"
			+ "    ,f.LARGE_READ_SERVICETIME \n"
			+ "    ,f.LARGE_WRITE_SERVICETIME \n"
			+ "    ,f.ASYNCH_IO \n"
			+ "    ,f.ACCESS_METHOD \n"
			+ "    ,f.RETRIES_ON_ERROR \n"
			+ "from V$IOSTAT_FILE f \n"
			+ "left join V$DATAFILE d on d.FILE# = f.FILE_NO and f.FILETYPE_ID = 2 \n"
			+ "left join V$TEMPFILE t on t.FILE# = f.FILE_NO and f.FILETYPE_ID = 6 \n"
			+ "order by f.FILETYPE_ID, f.FILE_NO \n"
			+ "";

		return sql;
	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			mtd.addTable(CM_NAME, "Disk I/O by file: one row for every data file and temp file, and one row for every other type of file.");

			mtd.addColumn(CM_NAME, "FILE_NO",                 "<html>File number for a data file or temp file. 0 for the other file types (the row is all files of that type together).</html>");
			mtd.addColumn(CM_NAME, "FILETYPE_ID",             "<html>Oracle's id for the file type.</html>");
			mtd.addColumn(CM_NAME, "FILETYPE_NAME",           "<html>Type of file: Data File, Temp File, Log File, Control File, Archive Log, Data File Backup, Flashback Log, Other...</html>");
			mtd.addColumn(CM_NAME, "FILE_NAME",               "<html>Name of the data file or temp file. NULL for the other file types.</html>");
			mtd.addColumn(CM_NAME, "TOTAL_MEGABYTES",         "<html>All I/O in MB.<br><b>Formula</b>: SMALL_READ_MEGABYTES + SMALL_WRITE_MEGABYTES + LARGE_READ_MEGABYTES + LARGE_WRITE_MEGABYTES</html>");
			mtd.addColumn(CM_NAME, "TOTAL_REQS",              "<html>All I/O requests.<br><b>Formula</b>: SMALL_READ_REQS + SMALL_WRITE_REQS + LARGE_READ_REQS + LARGE_WRITE_REQS</html>");
			mtd.addColumn(CM_NAME, "SMALL_READ_AVG_MS",       "<html>Average milliseconds for a single block read. In 'abs': since the instance was started. In 'diff' and 'rate': in the sample.<br><b>Formula</b>: SMALL_READ_SERVICETIME / SMALL_READ_REQS</html>");
			mtd.addColumn(CM_NAME, "SMALL_WRITE_AVG_MS",      "<html>Average milliseconds for a single block write.<br><b>Formula</b>: SMALL_WRITE_SERVICETIME / SMALL_WRITE_REQS</html>");
			mtd.addColumn(CM_NAME, "LARGE_READ_AVG_MS",       "<html>Average milliseconds for a multi block read.<br><b>Formula</b>: LARGE_READ_SERVICETIME / LARGE_READ_REQS</html>");
			mtd.addColumn(CM_NAME, "LARGE_WRITE_AVG_MS",      "<html>Average milliseconds for a multi block write.<br><b>Formula</b>: LARGE_WRITE_SERVICETIME / LARGE_WRITE_REQS</html>");
			mtd.addColumn(CM_NAME, "SMALL_SYNC_READ_REQS",    "<html>Number of synchronous single block read requests (the session waits for the read).</html>");
			mtd.addColumn(CM_NAME, "SMALL_SYNC_READ_LATENCY", "<html>Total time for the synchronous single block reads, in milliseconds.</html>");
			mtd.addColumn(CM_NAME, "ASYNCH_IO",               "<html>Is asynchronous I/O used for the file: ASYNC_ON or ASYNC_OFF.</html>");
			mtd.addColumn(CM_NAME, "ACCESS_METHOD",           "<html>How the file is accessed, for example: OS_LIB (the operating system), ODM_LIB, ASM_MANAGED, DNFS_LIB (Direct NFS).</html>");
			mtd.addColumn(CM_NAME, "RETRIES_ON_ERROR",        "<html>Number of read retries after an error. Above 0 is a sign of problems with the storage.</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems in cm='" + CM_NAME + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}

	// {requests, total time, average} for the 4 kinds of requests
	private static final String[][] AVG_MS_COLUMNS = new String[][] {
		{"SMALL_READ_REQS",  "SMALL_READ_SERVICETIME",  "SMALL_READ_AVG_MS"},
		{"SMALL_WRITE_REQS", "SMALL_WRITE_SERVICETIME", "SMALL_WRITE_AVG_MS"},
		{"LARGE_READ_REQS",  "LARGE_READ_SERVICETIME",  "LARGE_READ_AVG_MS"},
		{"LARGE_WRITE_REQS", "LARGE_WRITE_SERVICETIME", "LARGE_WRITE_AVG_MS"}
	};

	/**
	 * The *_AVG_MS columns: The SQL gives the average since the instance was started.<br>
	 * For DIFF (and RATE, which gets it from DIFF) we want the average for the requests that happened in this sample
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		for (String[] cols : AVG_MS_COLUMNS)
		{
			int pos_reqs  = diffData.findColumn(cols[0]);
			int pos_time  = diffData.findColumn(cols[1]);
			int pos_avgMs = diffData.findColumn(cols[2]);

			if (pos_reqs == -1 || pos_time == -1 || pos_avgMs == -1)
			{
				_logger.warn(getName() + ": localCalculation(), Problems finding one of the columns: " + cols[0] + ", " + cols[1] + ", " + cols[2] + ".");
				return;
			}

			for (int rowId=0; rowId < diffData.getRowCount(); rowId++)
			{
				long reqs = diffData.getValueAsLong(rowId, pos_reqs);
				long time = diffData.getValueAsLong(rowId, pos_time);

				BigDecimal avgMs = (reqs <= 0) ? new BigDecimal(0) : MathUtils.roundToBigDecimal(time * 1.0 / reqs, 3);

				diffData.setValueAt(avgMs, rowId, pos_avgMs);
			}
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (this.size() == 0 || ! this.hasDiffData() || ! this.hasRateData())
			return;

		// Both graphs: one "line" for every file type that has done any IO since the instance was started (all files of the type together)
		if (GRAPH_NAME_MB_PER_SEC.equals(tgdp.getName()) || GRAPH_NAME_AVG_MS.equals(tgdp.getName()))
		{
			LinkedHashMap<String, Double> mbPerSec = new LinkedHashMap<String, Double>();
			LinkedHashMap<String, Double> reqs     = new LinkedHashMap<String, Double>();
			LinkedHashMap<String, Double> timeMs   = new LinkedHashMap<String, Double>();

			for (int r = 0; r < this.size(); r++)
			{
				if (this.getAbsValueAsDouble(r, "TOTAL_REQS", 0d) <= 0)
					continue;

				String fileType = this.getAbsString(r, "FILETYPE_NAME");

				Double rowMbPerSec = this.getRateValueAsDouble(r, "TOTAL_MEGABYTES");
				double rowReqs     = this.getDiffValueAsDouble(r, "TOTAL_REQS", 0d);
				double rowTimeMs   = this.getDiffValueAsDouble(r, "SMALL_READ_SERVICETIME", 0d) + this.getDiffValueAsDouble(r, "SMALL_WRITE_SERVICETIME", 0d)
				                   + this.getDiffValueAsDouble(r, "LARGE_READ_SERVICETIME", 0d) + this.getDiffValueAsDouble(r, "LARGE_WRITE_SERVICETIME", 0d);

				mbPerSec.merge(fileType, (rowMbPerSec == null || rowMbPerSec < 0) ? 0d : rowMbPerSec, Double::sum);
				reqs    .merge(fileType, Math.max(0, rowReqs),   Double::sum);
				timeMs  .merge(fileType, Math.max(0, rowTimeMs), Double::sum);
			}

			if (mbPerSec.isEmpty())
				return;

			if (GRAPH_NAME_MB_PER_SEC.equals(tgdp.getName()))
			{
				tgdp.setData(this.getTimestamp(), mbPerSec);
			}
			else
			{
				// Average ms per request in the sample (0 if there was no requests)
				LinkedHashMap<String, Double> avgMs = new LinkedHashMap<String, Double>();
				for (String fileType : reqs.keySet())
					avgMs.put(fileType, reqs.get(fileType) <= 0 ? 0d : MathUtils.round(timeMs.get(fileType) / reqs.get(fileType), 3));

				tgdp.setData(this.getTimestamp(), avgMs);
			}
		}
	}
}
