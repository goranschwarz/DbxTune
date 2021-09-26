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
import java.util.ArrayList;
import java.util.List;

import com.asetune.cm.os.CmOsIostat;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.ReportEntryAbstract;
import com.asetune.pcs.report.content.ase.SparklineHelper;
import com.asetune.pcs.report.content.ase.SparklineHelper.AggType;
import com.asetune.pcs.report.content.ase.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.ase.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class OsIoStatOverview extends ReportEntryAbstract
{
//	private static Logger _logger = Logger.getLogger(OsIoStatIo.class);

	private ResultSetTableModel _shortRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public OsIoStatOverview(DailySummaryReportAbstract reportingInstance)
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
	public boolean hasShortMessageText()
	{
		return true;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
		writeMessageText(w);
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));

		// Write JavaScript code for CPU SparkLine
		for (String str : _miniChartJsList)
		{
			sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "OS 'iostat' Overview (order by: name, origin: CmOsIoStat_abs / os-cmd:iostat)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	public static final String PROPKEY_SKIP_DEVICE_NAMES  = OsIoStatOverview.class.getSimpleName()+".skip.device.names";
//	public static final String DEFAULT_SKIP_DEVICE_NAMES  = "";
	public static final String DEFAULT_SKIP_DEVICE_NAMES  = "sd";

	private String _skipDeviceNames  = DEFAULT_SKIP_DEVICE_NAMES;

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmOsIostat_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		_skipDeviceNames  = localConf.getProperty   (PROPKEY_SKIP_DEVICE_NAMES,  DEFAULT_SKIP_DEVICE_NAMES);

		 // just to get Column names
		String dummySql = "select * from [CmOsIostat_abs] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmOsIostat_abs' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("OsIoStat");
			return;
		}

		// Create Column selects, but only if the column exists in the PCS Table
//		String col_r_await__avg  = !dummyRstm.hasColumnNoCase("r_await") ? "" : "    ,avg([r_await]) as [r_await__avg] \n"; 
//		String col_w_await__avg  = !dummyRstm.hasColumnNoCase("w_await") ? "" : "    ,avg([w_await]) as [r_await__avg] \n"; 

		String sql_skipDeviceNames = "";
		if (StringUtil.hasValue(_skipDeviceNames))
		{
			List<String> skipList = StringUtil.parseCommaStrToList(_skipDeviceNames);
			for (String str : skipList)
			{
				str = str.trim();

				if ( ! str.endsWith("%") )
					str = str + "%";

				sql_skipDeviceNames = "  and [device] not like '" + str + "' \n";
			}
		}

		// Create Column selects, but only if the column exists in the PCS Table
		String r_await__avg = !dummyRstm.hasColumnNoCase("r_await") ? "" : "    ,cast(avg([r_await])         as numeric(10,1)) as [r_await__avg] \n";
		String w_await__avg = !dummyRstm.hasColumnNoCase("w_await") ? "" : "    ,cast(avg([w_await])         as numeric(10,1)) as [w_await__avg] \n";

		String sql = ""
			    + "select \n"
			    + "     [device] \n"
			    + "    ,cast(avg([rrqmPerSec])      as numeric(10,1)) as [rrqmPerSec__avg] \n"
			    + "    ,cast(avg([wrqmPerSec])      as numeric(10,1)) as [wrqmPerSec__avg] \n"
			    + "    ,cast('' as varchar(512))                      as [readsPerSec__chart] \n"
			    + "    ,cast(avg([readsPerSec])     as numeric(10,1)) as [readsPerSec__avg] \n"
			    + "    ,cast('' as varchar(512))                      as [writesPerSec__chart] \n"
			    + "    ,cast(avg([writesPerSec])    as numeric(10,1)) as [writesPerSec__avg] \n"
			    + "    ,cast(avg([kbReadPerSec])    as numeric(10,1)) as [kbReadPerSec__avg] \n"
			    + "    ,cast(avg([kbWritePerSec])   as numeric(10,1)) as [kbWritePerSec__avg] \n"
			    + "    ,cast(avg([avgReadKbPerIo])  as numeric(10,1)) as [avgReadKbPerIo__avg] \n"
			    + "    ,cast(avg([avgWriteKbPerIo]) as numeric(10,1)) as [avgWriteKbPerIo__avg] \n"
			    + "    ,cast(avg([avgrq-sz])        as numeric(10,1)) as [avgrq-sz__avg] \n"
			    + "    ,cast('' as varchar(512))                      as [avgqu-sz__chart] \n"
			    + "    ,cast(avg([avgqu-sz])        as numeric(10,1)) as [avgqu-sz__avg] \n"
			    + "    ,cast(avg([await])           as numeric(10,1)) as [await__avg] \n"
			    + r_await__avg
			    + w_await__avg
			    + "    ,cast('' as varchar(512))                      as [svctm__chart] \n"
			    + "    ,cast(avg([svctm])           as numeric(10,1)) as [svctm__avg] \n"
			    + "    ,cast('' as varchar(512))                      as [utilPct__chart] \n"
			    + "    ,cast(avg([utilPct])         as numeric(10,1)) as [utilPct__avg] \n"
			    + "    ,max([deviceDescription])                      as [deviceDescription] \n"
				+ "from [CmOsIostat_abs] \n"
				+ "where 1=1 \n"
				+ getReportPeriodSqlWhere()
				+ sql_skipDeviceNames
			    + "group by [device] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "OsIoStat");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("OsIoStat");
			return;
		}
		else
		{
			// Describe the table
			setSectionDescription(_shortRstm);

			// Mini Chart on
			String whereKeyColumn = "device"; 
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("readsPerSec__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmOsIostat_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("readsPerSec").setGroupDataAggregationType(AggType.AVG)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("AVG 'readsPerSec' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("writesPerSec__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmOsIostat_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("writesPerSec").setGroupDataAggregationType(AggType.AVG)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("AVG 'writesPerSec' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("avgqu-sz__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmOsIostat_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("avgqu-sz").setGroupDataAggregationType(AggType.MAX).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'avgqu-sz' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("svctm__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmOsIostat_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("svctm").setGroupDataAggregationType(AggType.MAX).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'svctm' in below period")
					.validate()));

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("utilPct__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmOsIostat_abs")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("utilPct").setGroupDataAggregationType(AggType.AVG).setDecimalScale(1)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("AVG 'utilPct' in below period")
					.validate()));
		}
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Disk access (measured by OS Command <code>iostat</code>), ordered by: name <br>" +
				"<br>" +
				"Restrictions:<br>" +
					"&emsp;&bull; <code>" + PROPKEY_SKIP_DEVICE_NAMES + "</code> = " + _skipDeviceNames + " <br>" +
				"<br>" +
				"OS Source command is 'iostat'. <br>" +
				"PCS Source table is 'CmOsIostat_abs'. (PCS = Persistent Counter Store) <br>" +
				"");
	}
}
