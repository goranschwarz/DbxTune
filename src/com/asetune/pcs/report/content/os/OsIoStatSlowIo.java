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
package com.asetune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.asetune.cm.os.CmOsIostat;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class OsIoStatSlowIo extends OsAbstract
{
//	private static Logger _logger = Logger.getLogger(OsIoStatSlowIo.class);

	private ResultSetTableModel _shortRstm;

	public OsIoStatSlowIo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean isEnabled()
	{
		// If super is DISABLED, no need to continue
		boolean isEnabled = super.isEnabled();
		if ( ! isEnabled )
			return isEnabled;

		// NOT For Windows
//		String dbmsVerStr = getReportingInstance().getDbmsVersionStr();
//		if (StringUtil.hasValue(dbmsVerStr))
//		{
//			if (dbmsVerStr.indexOf("Windows") != -1)
//			{
//				setDisabledReason("This DBMS is running on Windows, wich is not supported by this report.");
//				return false;
//			}
//		}
		return true;
	}
	
	@Override
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));

		// If table "CmOsIostat_abs" do not exists
		// the below _CmOsIostat_xxxx objects will be null... early exit in create()
		if (_CmOsIostat_IoWait_noLimit != null)
		{
			sb.append(getDbxCentralLinkWithDescForGraphs(true, "Below are Graphs/Charts with various information that can help you decide how the IO Subsystem is handling the load.",
					"CmOsIostat_IoWait",
					"CmOsIostat_IoReadWait",
					"CmOsIostat_IoWriteWait",
					"CmOsIostat_IoServiceTime",
					"CmOsIostat_IoReadOp",
					"CmOsIostat_IoWriteOp"
					));

			_CmOsIostat_IoWait_noLimit.writeHtmlContent(sb, null, null);
			_CmOsIostat_IoWait        .writeHtmlContent(sb, null, null);
			_CmOsIostat_IoReadWait    .writeHtmlContent(sb, null, null);
			_CmOsIostat_IoWriteWait   .writeHtmlContent(sb, null, null);
			_CmOsIostat_IoServiceTime .writeHtmlContent(sb, null, null);
			_CmOsIostat_IoReadOp      .writeHtmlContent(sb, null, null);
			_CmOsIostat_IoWriteOp     .writeHtmlContent(sb, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "OS 'iostat' Long Wait Time for IO (order by: SampleTime, origin: CmOsIoStat_abs / os-cmd:iostat)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	public static final String PROPKEY_ABOVE_TOTAL_IOS    = OsIoStatSlowIo.class.getSimpleName()+".above.total_ios";
	public static final int    DEFAULT_ABOVE_TOTAL_IOS    = 2;

	public static final String PROPKEY_ABOVE_SERVICE_TIME = OsIoStatSlowIo.class.getSimpleName()+".above.await";
	public static final int    DEFAULT_ABOVE_SERVICE_TIME = 50;

	public static final String PROPKEY_SKIP_DEVICE_NAMES  = OsIoStatSlowIo.class.getSimpleName()+".skip.device.names";
//	public static final String DEFAULT_SKIP_DEVICE_NAMES  = "";
	public static final String DEFAULT_SKIP_DEVICE_NAMES  = "sd";

	private int    _aboveTotalIos    = DEFAULT_ABOVE_TOTAL_IOS;
	private int    _aboveServiceTime = DEFAULT_ABOVE_SERVICE_TIME;
	private String _skipDeviceNames  = DEFAULT_SKIP_DEVICE_NAMES;

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmOsIostat_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_aboveTotalIos    = localConf.getIntProperty(PROPKEY_ABOVE_TOTAL_IOS,    DEFAULT_ABOVE_TOTAL_IOS);
		_aboveServiceTime = localConf.getIntProperty(PROPKEY_ABOVE_SERVICE_TIME, DEFAULT_ABOVE_SERVICE_TIME);
		_skipDeviceNames  = localConf.getProperty   (PROPKEY_SKIP_DEVICE_NAMES,  DEFAULT_SKIP_DEVICE_NAMES);

		String schemaPrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
		
//		if ( ! DbUtils.checkIfTableExistsNoThrow(conn, cat, schema, tableName) )
//		{
//		}

		 // just to get Column names
		String dummySql = "select * from " + schemaPrefix + "[CmOsIostat_abs] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmOsIostat_abs' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("OsIoStat_slow");
			return;
		}

		// TODO: Build a TimeSeries chart with all devices
		//       select * from CmOsIostat_IoWait
		//       also: Remove columnNames (deviceName) which DO NOT MATCH: PROPKEY_SKIP_DEVICE_NAMES
		//       max value: _aboveServiceTime / 2
		// possibly also: CmOsIostat_IoReadOp, CmOsIostat_IoWriteOp
		//
		// check: https://www.boraji.com/jfreechart-time-series-chart-example
		//        and: ChartUtilities.saveChartAsPNG(); or ChartUtilities.writeChartAsPNG();
		
		// Create Column selects, but only if the column exists in the PCS Table
//		String Inserts_sum             = !dummyRstm.hasColumnNoCase("Inserts"            ) ? "" : "    ,sum([Inserts])                                        as [Inserts_sum] -- 16.0 \n"; 
//		String Updates_sum             = !dummyRstm.hasColumnNoCase("Updates"            ) ? "" : "    ,sum([Updates])                                        as [Updates_sum] -- 16.0 \n"; 
//		String Deletes_sum             = !dummyRstm.hasColumnNoCase("Deletes"            ) ? "" : "    ,sum([Deletes])                                        as [Deletes_sum] -- 16.0 \n"; 
//		String Scans_sum               = !dummyRstm.hasColumnNoCase("Scans"              ) ? "" : "    ,sum([Scans])                                          as [Scans_sum]   -- 16.0 \n"; 

		String sql = "";

		if ( isWindows() )
		{
			String sql_skipDeviceNames = "";
			if (StringUtil.hasValue(_skipDeviceNames))
			{
				List<String> skipList = StringUtil.parseCommaStrToList(_skipDeviceNames);
				for (String str : skipList)
				{
					if ( ! str.endsWith("%") )
						str += "%";

					sql_skipDeviceNames = "  and [Instance] not like '" + str + "' \n";
				}
			}

			sql = "select [Avg. Disk sec/Transfer] * 1000.0 AS [ServiceTimeInMs], * \n"
					+ "from " + schemaPrefix + "[CmOsIostat_abs] \n"
					+ "where [Avg. Disk sec/Transfer] * 1000.0 > " + _aboveServiceTime + " \n"
					+ "  and ([Disk Reads/sec] > " + _aboveTotalIos + " or [Disk Writes/sec] > " + _aboveTotalIos + ") \n"
					+ getReportPeriodSqlWhere()
					+ sql_skipDeviceNames
				    + "  and [Instance] != '_Total' \n"
				    + "";
		}
		else
		{
			String sql_skipDeviceNames = "";
			if (StringUtil.hasValue(_skipDeviceNames))
			{
				List<String> skipList = StringUtil.parseCommaStrToList(_skipDeviceNames);
				for (String str : skipList)
				{
					if ( ! str.endsWith("%") )
						str += "%";

					sql_skipDeviceNames = "  and [device] not like '" + str + "' \n";
				}
			}


			// Build WHERE clause for: "await"  OR  "r_await" + "w_await"
			String and_aboveServiceTime = "";
			if (dummyRstm.hasColumnNoCase("await"))
			{
				and_aboveServiceTime = "  and [await] > " + _aboveServiceTime + " \n";
			}
			else
			{
				and_aboveServiceTime = "  and ([r_await] + [w_await]) > " + _aboveServiceTime + " \n";
			}
			

			sql = "select * \n"
					+ "from " + schemaPrefix + "[CmOsIostat_abs] \n"
					+ "where 1 = 1 \n"
					+ and_aboveServiceTime
					+ "  and ([readsPerSec] > " + _aboveTotalIos + " or [writesPerSec] > " + _aboveTotalIos + ") \n"
					+ getReportPeriodSqlWhere()
					+ sql_skipDeviceNames
				    + "";
		}

		_shortRstm = executeQuery(conn, sql, false, "OsIoStat_slow");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("OsIoStat_slow");
			return;
		}
		else
		{
			// 1> select *
			// 2> from "CmOsIostat_abs"
			// 3> where "await" > 3
			// 4>   and ("readsPerSec" > 1 or "writesPerSec" > 1)
			//
			// RS> Col# Label             JDBC Type Name           Guessed DBMS type Source Table                                
			// RS> ---- ----------------- ------------------------ ----------------- --------------------------------------------
			// RS> 1    SessionStartTime  java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 2    SessionSampleTime java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 3    CmSampleTime      java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 4    CmSampleMs        java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 5    CmNewDiffRateRow  java.sql.Types.TINYINT   TINYINT           PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 6    device            java.sql.Types.VARCHAR   VARCHAR(30)       PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 7    samples           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 8    rrqmPerSec        java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 9    wrqmPerSec        java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 10   readsPerSec       java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 11   writesPerSec      java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 12   kbReadPerSec      java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 13   kbWritePerSec     java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 14   avgReadKbPerIo    java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 15   avgWriteKbPerIo   java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 16   avgrq-sz          java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 17   avgqu-sz          java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 18   await             java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 19   r_await           java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 20   w_await           java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 21   svctm             java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 22   utilPct           java.sql.Types.DECIMAL   DECIMAL(5,1)      PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// RS> 23   deviceDescription java.sql.Types.VARCHAR   VARCHAR(255)      PROD_A1_ASE_2019-08-26.PUBLIC.CmOsIostat_abs
			// +-----------------------+-----------------------+-----------------------+----------+----------------+------+-------+----------+----------+-----------+------------+------------+-------------+--------------+---------------+--------+--------+-----+-------+-------+-----+-------+-----------------+
			// |SessionStartTime       |SessionSampleTime      |CmSampleTime           |CmSampleMs|CmNewDiffRateRow|device|samples|rrqmPerSec|wrqmPerSec|readsPerSec|writesPerSec|kbReadPerSec|kbWritePerSec|avgReadKbPerIo|avgWriteKbPerIo|avgrq-sz|avgqu-sz|await|r_await|w_await|svctm|utilPct|deviceDescription|
			// +-----------------------+-----------------------+-----------------------+----------+----------------+------+-------+----------+----------+-----------+------------+------------+-------------+--------------+---------------+--------+--------+-----+-------+-------+-----+-------+-----------------+
			// |2019-08-26 00:00:22.053|2019-08-26 01:10:20.583|2019-08-26 01:10:20.76 |0         |0               |dm-0  |3      |0.0       |0.0       |0.0        |136.0       |0.0         |33878.0      |0.0           |249.1          |184.3   |2.2     |5.5  |0.0    |5.5    |0.1  |1.8    |(NULL)           |
			// |2019-08-26 00:00:22.053|2019-08-26 17:00:21.96 |2019-08-26 17:00:22.322|0         |0               |dm-0  |4      |0.0       |0.0       |0.0        |75.4        |0.0         |18895.5      |0.0           |250.6          |141.4   |1.2     |3.9  |0.0    |3.9    |0.0  |1.0    |(NULL)           |
			// |2019-08-26 00:00:22.053|2019-08-26 17:05:25.996|2019-08-26 17:05:26.223|0         |0               |dm-0  |5      |0.0       |0.0       |0.0        |767.4       |0.0         |97788.8      |0.0           |127.4          |60.5    |15.8    |4.4  |0.0    |4.4    |0.1  |5.1    |(NULL)           |
			// |2019-08-26 00:00:22.053|2019-08-26 19:03:27.196|2019-08-26 19:03:27.402|0         |0               |dm-0  |2      |0.0       |0.0       |0.0        |484.5       |0.0         |11070.0      |0.0           |22.8           |135.8   |2.4     |3.8  |0.0    |3.8    |0.1  |1.4    |(NULL)           |
			// |2019-08-26 00:00:22.053|2019-08-26 21:36:24.413|2019-08-26 21:36:24.613|0         |0               |dm-0  |4      |0.0       |0.0       |0.0        |1.6         |0.0         |13.5         |0.0           |8.4            |17.0    |0.0     |3.7  |0.0    |3.7    |0.8  |0.3    |(NULL)           |
			// +-----------------------+-----------------------+-----------------------+----------+----------------+------+-------+----------+----------+-----------+------------+------------+-------------+--------------+---------------+--------+--------+-----+-------+-------+-----+-------+-----------------+
			
			_shortRstm.removeColumnNoCase("SessionStartTime");
			_shortRstm.removeColumnNoCase("SessionSampleTime");
			_shortRstm.removeColumnNoCase("CmNewDiffRateRow");  // This was changed into "CmRowState"
			_shortRstm.removeColumnNoCase("CmRowState");


//			_shortRstm.setColumnOrder("aaa", "bbb", "ccc", "ddd", "eee");
			
			// Describe the table
			setSectionDescription(_shortRstm);

			String schema = getReportingInstance().getDbmsSchemaName();

			int maxValue = 10;
			_CmOsIostat_IoWait_noLimit = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_WaitTime,      -1,       false, _skipDeviceNames, "iostat: Wait Time(await) per Device in ms (Host Monitor->OS Disk Stat(iostat)) [with NO max value]");
			_CmOsIostat_IoWait         = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_WaitTime,      maxValue, false, _skipDeviceNames, "iostat: Wait Time(await) per Device in ms (Host Monitor->OS Disk Stat(iostat)) [with max value=" + maxValue + "]");
			_CmOsIostat_IoReadWait     = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_ReadWaitTime,  maxValue, false, _skipDeviceNames, "iostat: Read wait Time(r_await) per Device in ms (Host Monitor->OS Disk Stat(iostat)) [with max value=" + maxValue + "]");
			_CmOsIostat_IoWriteWait    = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_WriteWaitTime, maxValue, false, _skipDeviceNames, "iostat: Write wait Time(w_await) per Device in ms (Host Monitor->OS Disk Stat(iostat)) [with max value=" + maxValue + "]");

			_CmOsIostat_IoReadOp       = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_ReadOp,       -1,        false, _skipDeviceNames, "iostat: Read Operations(readsPerSec) per Device & sec (Host Monitor->OS Disk Stat(iostat))");
			_CmOsIostat_IoWriteOp      = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_WriteOp,      -1,        false, _skipDeviceNames, "iostat: Write Operations(writesPerSec) per Device & sec (Host Monitor->OS Disk Stat(iostat))");

			_CmOsIostat_IoServiceTime  = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_ServiceTime,   -1,       false, _skipDeviceNames, "iostat: Service Time(svctm) per Device in ms (Host Monitor->OS Disk Stat(iostat)) [with NO max value]");
			_CmOsIostat_IoServiceTime  = createTsLineChart(conn, schema, CmOsIostat.CM_NAME, CmOsIostat.GRAPH_NAME_ServiceTime,   maxValue, false, _skipDeviceNames, "iostat: Service Time(svctm) per Device in ms (Host Monitor->OS Disk Stat(iostat)) [with max value=" + maxValue + "]");
		}
	}

	private IReportChart _CmOsIostat_IoWait_noLimit;
	private IReportChart _CmOsIostat_IoWait;
	private IReportChart _CmOsIostat_IoReadWait;
	private IReportChart _CmOsIostat_IoWriteWait;
	private IReportChart _CmOsIostat_IoServiceTime;
	
	private IReportChart _CmOsIostat_IoReadOp;
	private IReportChart _CmOsIostat_IoWriteOp;
	
	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Slow Disk access (measured by OS Command <code>iostat</code>), or slow disk <i>service time</i> (ordered by: sample time) <br>" +
				"<br>" +
				"What disk/devices (at what time) have undelaying storage <i>problems/issues</i>... manifested as long access time<br>" +
				"<br>" +
				"Thresholds:<br>" +
					"&emsp;&bull; <code>" + PROPKEY_ABOVE_SERVICE_TIME + "</code> = " + _aboveServiceTime + " <br>" +
					"&emsp;&bull; <code>" + PROPKEY_ABOVE_TOTAL_IOS    + "</code> = " + _aboveTotalIos    + " <br>" +
				"<br>" +
				"Restrictions:<br>" +
					"&emsp;&bull; <code>" + PROPKEY_SKIP_DEVICE_NAMES + "</code> = " + _skipDeviceNames + " <br>" +
				"<br>" +
				"OS Source command is 'iostat'. <br>" +
				"PCS Source table is 'CmOsIostat_abs'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report simply lists what times <i>service times</i> was above a threshold. (FIXME) <br>" +
				"");
	}
}
