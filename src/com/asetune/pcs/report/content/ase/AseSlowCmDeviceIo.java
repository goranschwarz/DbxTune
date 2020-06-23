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
package com.asetune.pcs.report.content.ase;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.os.OsIoStatSlowIo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseSlowCmDeviceIo extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseTopCmCachedProcs.class);

	private ResultSetTableModel _shortRstm;

	public AseSlowCmDeviceIo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMessageText()
	{
		StringBuilder sb = new StringBuilder();

//		if (_shortRstm.getRowCount() == 0)
//		{
//			sb.append("No rows found <br>\n");
//		}
//		else
//		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append("Row Count: ").append(_shortRstm.getRowCount()).append("<br>\n");
			sb.append(_shortRstm.toHtmlTableString("sortable"));
//		}

		if (_CmDeviceIo_IoRW != null)
		{
			sb.append(getDbxCentralLinkWithDescForGraphs(true, "Below are Graphs/Charts with various information that can help you decide how the IO Subsystem is handling the load.",
					"CmDeviceIo_IoRW",
					"CmDeviceIo_SvcTimeRW",
					"CmDeviceIo_SvcTimeR",
					"CmDeviceIo_SvcTimeW"
					));
			
			sb.append(_CmDeviceIo_IoRW             .getHtmlContent(null, null));
			sb.append(_CmDeviceIo_SvcTimeRW_noLimit.getHtmlContent(null, null));
			sb.append(_CmDeviceIo_SvcTimeRW        .getHtmlContent(null, null));
			sb.append(_CmDeviceIo_SvcTimeR         .getHtmlContent(null, null));
			sb.append(_CmDeviceIo_SvcTimeW         .getHtmlContent(null, null));
		}
			
		return sb.toString();
	}

	@Override
	public String getSubject()
	{
		return "Slow Device IO, or Long Wait Time for IO (order by: SampleTime, origin: CmDeviceIo_rate / monDeviceIO)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	public static final String PROPKEY_ABOVE_TOTAL_IOS    = OsIoStatSlowIo.class.getSimpleName()+".above.TotalIOs";
	public static final int    DEFAULT_ABOVE_TOTAL_IOS    = 2;

	public static final String PROPKEY_ABOVE_SERVICE_TIME = OsIoStatSlowIo.class.getSimpleName()+".above.AvgServ_ms";
	public static final int    DEFAULT_ABOVE_SERVICE_TIME = 50;

	int _aboveTotalIos    = DEFAULT_ABOVE_TOTAL_IOS;
	int _aboveServiceTime = DEFAULT_ABOVE_SERVICE_TIME;

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_aboveTotalIos    = localConf.getIntProperty(PROPKEY_ABOVE_TOTAL_IOS,    DEFAULT_ABOVE_TOTAL_IOS);
		_aboveServiceTime = localConf.getIntProperty(PROPKEY_ABOVE_SERVICE_TIME, DEFAULT_ABOVE_SERVICE_TIME);

		 // just to get Column names
		String dummySql = "select * from [CmDeviceIo_rate] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmDeviceIo_rate' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblem(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("slow_CmDeviceIo");
			return;
		}

		String sql = "select * \n"
				+ "from [CmDeviceIo_rate] \n"
				+ "where [AvgServ_ms] > " + _aboveServiceTime + " \n"
				+ "  and [TotalIOs]   > " + _aboveTotalIos    + " \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, false, "slow_CmDeviceIo");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("slow_CmDeviceIo");
			return;
		}
		else
		{
			// 1> select *
			// 2> from "CmDeviceIo_rate"
			// 3> where "AvgServ_ms" > 10
			// 4>   and "TotalIOs"   > 0
			//
			// RS> Col# Label                JDBC Type Name           Guessed DBMS type Source Table                                 
			// RS> ---- -------------------- ------------------------ ----------------- ---------------------------------------------
			// RS> 1    SessionStartTime     java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 2    SessionSampleTime    java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 3    CmSampleTime         java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 4    CmSampleMs           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 5    CmNewDiffRateRow     java.sql.Types.TINYINT   TINYINT           PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 6    LogicalName          java.sql.Types.VARCHAR   VARCHAR(30)       PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 7    TotalIOs             java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 8    Reads                java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 9    ReadsPct             java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 10   APFReads             java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 11   APFReadsPct          java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 12   Writes               java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 13   WritesPct            java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 14   DevSemaphoreRequests java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 15   DevSemaphoreWaits    java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 16   IOTime               java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 17   ReadTime             java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 18   WriteTime            java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 19   AvgServ_ms           java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 20   ReadServiceTimeMs    java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 21   WriteServiceTimeMs   java.sql.Types.DECIMAL   DECIMAL(10,1)     PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 22   DeviceType           java.sql.Types.VARCHAR   VARCHAR(15)       PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// RS> 23   PhysicalName         java.sql.Types.VARCHAR   VARCHAR(128)      PROD_A1_ASE_2019-08-26.PUBLIC.CmDeviceIo_rate
			// +-----------------------+-----------------------+-----------------------+----------+----------------+-----------------+--------+-----+--------+--------+-----------+------+---------+--------------------+-----------------+------+--------+---------+----------+-----------------+------------------+----------+-----------------------------------------------------+
			// |SessionStartTime       |SessionSampleTime      |CmSampleTime           |CmSampleMs|CmNewDiffRateRow|LogicalName      |TotalIOs|Reads|ReadsPct|APFReads|APFReadsPct|Writes|WritesPct|DevSemaphoreRequests|DevSemaphoreWaits|IOTime|ReadTime|WriteTime|AvgServ_ms|ReadServiceTimeMs|WriteServiceTimeMs|DeviceType|PhysicalName                                         |
			// +-----------------------+-----------------------+-----------------------+----------+----------------+-----------------+--------+-----+--------+--------+-----------+------+---------+--------------------+-----------------+------+--------+---------+----------+-----------------+------------------+----------+-----------------------------------------------------+
			// |2019-08-26 00:00:22.053|2019-08-26 01:22:32.44 |2019-08-26 01:22:32.6  |30320     |0               |pml_log_5        |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |1.5   |0.0     |1.5      |11.2      |0.0              |11.2              |File      |/sybdev/devices/log/PROD_A1_ASE.pml_log_5.dev        |
			// |2019-08-26 00:00:22.053|2019-08-26 03:01:11.826|2019-08-26 03:01:12.126|30673     |0               |sybsecurity_log_1|0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |1.2   |0.0     |1.2      |12.7      |0.0              |12.7              |File      |/sybdev/devices/log/PROD_A1_ASE.sybsecurity_log_1.dev|
			// |2019-08-26 00:00:22.053|2019-08-26 03:10:50.516|2019-08-26 03:10:50.84 |30524     |0               |master           |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |1.1   |0.0     |1.1      |11.0      |0.0              |11.0              |File      |/sybdev/devices/data/PROD_A1_ASE.master              |
			// |2019-08-26 00:00:22.053|2019-08-26 10:27:02.18 |2019-08-26 10:27:02.433|30413     |0               |sybsecurity_log_1|0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |1.1   |0.0     |1.1      |10.7      |0.0              |10.7              |File      |/sybdev/devices/log/PROD_A1_ASE.sybsecurity_log_1.dev|
			// |2019-08-26 00:00:22.053|2019-08-26 10:46:19.433|2019-08-26 10:46:19.693|30417     |0               |sybsecurity_log_1|0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |1.4   |0.0     |1.4      |10.8      |0.0              |10.8              |File      |/sybdev/devices/log/PROD_A1_ASE.sybsecurity_log_1.dev|
			// |2019-08-26 00:00:22.053|2019-08-26 10:56:58.99 |2019-08-26 10:56:59.366|30523     |0               |sysprocsdev      |0.2     |0.0  |0.0     |0.0     |0.0        |0.2   |100.0    |0.0                 |0.0              |2.1   |0.0     |2.1      |10.7      |0.0              |10.7              |File      |/sybdev/devices/data/PROD_A1_ASE.sysprocsdev         |
			// |2019-08-26 00:00:22.053|2019-08-26 11:21:51.666|2019-08-26 11:21:51.94 |30427     |0               |b2b_data_1       |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |0.8   |0.0     |0.8      |11.5      |0.0              |11.5              |File      |/sybdev/devices/data/PROD_A1_ASE.b2b_data_1.dev      |
			// |2019-08-26 00:00:22.053|2019-08-26 12:33:54.733|2019-08-26 12:33:54.986|30540     |0               |pml_data_11      |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |0.7   |0.0     |0.7      |10.5      |0.0              |10.5              |File      |/sybdev/devices/data/PROD_A1_ASE.pml_data_11.dev     |
			// |2019-08-26 00:00:22.053|2019-08-26 15:13:19.796|2019-08-26 15:13:20.066|30416     |0               |pml_data_15      |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |0.9   |0.0     |0.9      |13.5      |0.0              |13.5              |File      |/sybdev/devices/data/PROD_A1_ASE.pml_data_15.dev     |
			// |2019-08-26 00:00:22.053|2019-08-26 17:26:12.626|2019-08-26 17:26:12.833|30350     |0               |sysprocsdev      |0.2     |0.0  |0.0     |0.0     |0.0        |0.2   |100.0    |0.0                 |0.0              |2.4   |0.0     |2.4      |10.6      |0.0              |10.6              |File      |/sybdev/devices/data/PROD_A1_ASE.sysprocsdev         |
			// |2019-08-26 00:00:22.053|2019-08-26 17:56:06.17 |2019-08-26 17:56:06.363|30343     |0               |mts_data_1       |0.2     |0.0  |0.0     |0.0     |0.0        |0.2   |100.0    |0.0                 |0.0              |2.0   |0.0     |2.0      |10.3      |0.0              |10.3              |File      |/sybdev/devices/data/PROD_A1_ASE.mts_data_1.dev      |
			// |2019-08-26 00:00:22.053|2019-08-26 19:01:25.633|2019-08-26 19:01:25.81 |30334     |0               |linda_data_1     |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |0.8   |0.0     |0.8      |12.5      |0.0              |12.5              |File      |/sybdev/devices/data/PROD_A1_ASE.linda_data_1.dev    |
			// |2019-08-26 00:00:22.053|2019-08-26 22:01:15.263|2019-08-26 22:01:15.59 |30874     |0               |linda_data_1     |88.7    |0.0  |0.0     |0.0     |0.0        |88.7  |100.0    |0.0                 |0.0              |1124.7|0.0     |1124.7   |12.7      |0.0              |12.7              |File      |/sybdev/devices/data/PROD_A1_ASE.linda_data_1.dev    |
			// |2019-08-26 00:00:22.053|2019-08-26 22:01:15.263|2019-08-26 22:01:15.59 |30874     |0               |sysprocsdev      |0.2     |0.0  |0.0     |0.0     |0.0        |0.2   |100.0    |0.0                 |0.0              |2.1   |0.0     |2.1      |10.7      |0.0              |10.7              |File      |/sybdev/devices/data/PROD_A1_ASE.sysprocsdev         |
			// |2019-08-26 00:00:22.053|2019-08-26 22:02:16.713|2019-08-26 22:02:17.106|30936     |0               |pml_data_5       |0.1     |0.0  |0.0     |0.0     |0.0        |0.1   |100.0    |0.0                 |0.0              |1.4   |0.0     |1.4      |11.0      |0.0              |11.0              |File      |/sybdev/devices/data/PROD_A1_ASE.pml_data_5.dev      |
			// |2019-08-26 00:00:22.053|2019-08-26 22:02:16.713|2019-08-26 22:02:17.106|30936     |0               |pml_log_5        |0.5     |0.0  |0.0     |0.0     |0.0        |0.5   |100.0    |0.0                 |0.0              |5.1   |0.0     |5.1      |10.5      |0.0              |10.5              |File      |/sybdev/devices/log/PROD_A1_ASE.pml_log_5.dev        |
			// +-----------------------+-----------------------+-----------------------+----------+----------------+-----------------+--------+-----+--------+--------+-----------+------+---------+--------------------+-----------------+------+--------+---------+----------+-----------------+------------------+----------+-----------------------------------------------------+
			
			_shortRstm.removeColumnNoCase("SessionStartTime");
			_shortRstm.removeColumnNoCase("SessionSampleTime");
			_shortRstm.removeColumnNoCase("CmNewDiffRateRow");

//			_shortRstm.setColumnOrder("aaa", "bbb", "ccc", "ddd", "eee");

			// Describe the table
			setSectionDescription(_shortRstm);

			
			int maxValue = 10;
			_CmDeviceIo_IoRW              = createChart(conn, "CmDeviceIo", "IoRW",      -1,       null, "Number of Disk Operations (Read+Write), per Second and Device (Disk->Devices)");
			_CmDeviceIo_SvcTimeRW_noLimit = createChart(conn, "CmDeviceIo", "SvcTimeRW", -1,       null, "Device IO Service Time (Read+Write) in Milliseconds, per Device (Disk->Devices) [with NO max value]");
			_CmDeviceIo_SvcTimeRW         = createChart(conn, "CmDeviceIo", "SvcTimeRW", maxValue, null, "Device IO Service Time (Read+Write) in Milliseconds, per Device (Disk->Devices) [with max value=" + maxValue + "]");
			_CmDeviceIo_SvcTimeR          = createChart(conn, "CmDeviceIo", "SvcTimeR",  maxValue, null, "Device IO Service Time (Read) in Milliseconds, per Device (Disk->Devices) [with max value=" + maxValue + "]");
			_CmDeviceIo_SvcTimeW          = createChart(conn, "CmDeviceIo", "SvcTimeW",  maxValue, null, "Device IO Service Time (Write) in Milliseconds, per Device (Disk->Devices) [with max value=" + maxValue + "]");
		}
	}
	private ReportChartObject _CmDeviceIo_IoRW;
	private ReportChartObject _CmDeviceIo_SvcTimeRW_noLimit;
	private ReportChartObject _CmDeviceIo_SvcTimeRW;
	private ReportChartObject _CmDeviceIo_SvcTimeR;
	private ReportChartObject _CmDeviceIo_SvcTimeW;

	
	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Slow Device access, or slow device <i>service time</i> (ordered by: sample time) <br>" +
				"<br>" +
				"What devices (at what time) have undelaying storage <i>problems/issues</i>... manifested as long access time<br>" +
				"<br>" +
				"Thresholds:<br>" +
				"&emsp;&bull; <code>" + PROPKEY_ABOVE_SERVICE_TIME + "</code> = " + _aboveServiceTime + " <br>" +
				"&emsp;&bull; <code>" + PROPKEY_ABOVE_TOTAL_IOS    + "</code> = " + _aboveTotalIos    + " <br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monDeviceIO'. <br>" +
				"PCS Source table is 'CmDeviceIo_rate'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report simply lists what times <i>service times</i> was above a threshold. (FIXME) <br>" +
				"");
	}
}
