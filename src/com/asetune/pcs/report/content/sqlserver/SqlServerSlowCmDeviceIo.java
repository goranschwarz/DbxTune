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
package com.asetune.pcs.report.content.sqlserver;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportChartObject;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.pcs.report.content.os.OsIoStatSlowIo;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class SqlServerSlowCmDeviceIo extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlServerSlowCmDeviceIo.class);

	private ResultSetTableModel _shortRstm;

	public SqlServerSlowCmDeviceIo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public String getMsgAsText()
	{
		StringBuilder sb = new StringBuilder();

		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found \n");
		}
		else
		{
			sb.append(getSubject() + " Count: ").append(_shortRstm.getRowCount()).append("\n");
			sb.append(_shortRstm.toAsciiTableString());
		}

		if (hasProblem())
			sb.append(getProblem());
		
		return sb.toString();
	}

	@Override
	public String getMsgAsHtml()
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

		if (hasProblem())
			sb.append("<pre>").append(getProblem()).append("</pre> \n");

		sb.append("\n<br>");

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
	public void create(DbxConnection conn, String srvName, Configuration conf)
	{
		_aboveTotalIos    = conf.getIntProperty(PROPKEY_ABOVE_TOTAL_IOS,    DEFAULT_ABOVE_TOTAL_IOS);
		_aboveServiceTime = conf.getIntProperty(PROPKEY_ABOVE_SERVICE_TIME, DEFAULT_ABOVE_SERVICE_TIME);

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
				"SqlServer Source table is 'dm_io_virtual_file_stats'. <br>" +
				"PCS Source table is 'CmDeviceIo_rate'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report simply lists what times <i>service times</i> was above a threshold. (" + _aboveServiceTime + ") <br>" +
				"");
	}
}
