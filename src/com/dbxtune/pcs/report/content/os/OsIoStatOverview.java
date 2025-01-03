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
package com.dbxtune.pcs.report.content.os;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.AggType;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class OsIoStatOverview extends OsAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

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
	public boolean hasMinimalMessageText()
	{
		return false;
	}

	@Override
	public boolean hasShortMessageText()
	{
		return true;
	}

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		sb.append(toHtmlTable(_shortRstm));

		// Write JavaScript code for CPU SparkLine
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
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

		String schema       = getReportingInstance().getDbmsSchemaName();
		String schemaPrefix = getReportingInstance().getDbmsSchemaNameSqlPrefix();
		
		 // just to get Column names
		String dummySql = "select * from " + schemaPrefix + "[CmOsIostat_abs] where 1 = 2";
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

		if ( isWindows() )
		{
			String sql_skipDeviceNames = "";
			if (StringUtil.hasValue(_skipDeviceNames))
			{
				List<String> skipList = StringUtil.parseCommaStrToList(_skipDeviceNames);
				for (String str : skipList)
				{
					str = str.trim();

					if ( ! str.endsWith("%") )
						str = str + "%";

					sql_skipDeviceNames = "  and [Instance] not like '" + str + "' \n";
				}
			}

			String sql = ""
				    + "select \n"
				    + "     [Instance] \n"

				    + "    ,cast('' as varchar(512))                                      as [iosPerSec__chart] \n"
				    + "    ,cast(avg([Disk Transfers/sec])              as numeric(10,1)) as [iosPerSec__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [readsPerSec__chart] \n"
				    + "    ,cast(avg([Disk Reads/sec])                  as numeric(10,1)) as [readsPerSec__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [writesPerSec__chart] \n"
				    + "    ,cast(avg([Disk Writes/sec])                 as numeric(10,1)) as [writesPerSec__avg] \n"

				    + "    ,cast(avg([Disk Read Bytes/sec] /1024.0)     as numeric(10,1)) as [kbReadPerSec__avg] \n"
				    + "    ,cast(avg([Disk Write Bytes/sec]/1024.0)     as numeric(10,1)) as [kbWritePerSec__avg] \n"

//				    + "    ,cast(avg([Disk Read Bytes/sec] /1024.0])/avg([Disk Reads/sec])  as numeric(10,1)) as [avgReadKbPerIo__avg] \n"
//				    + "    ,cast(avg([Disk Write Bytes/sec]/1024.0])/avg([Disk Writes/sec]) as numeric(10,1)) as [avgWriteKbPerIo__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [diskQueueLength__avg__chart] \n"
				    + "    ,cast(avg([Avg. Disk Queue Length])          as numeric(10,1)) as [diskQueueLength__avg] \n"

//				    + "    ,cast('' as varchar(512))                                      as [diskReadQueueLength__avg__chart] \n"
//				    + "    ,cast(avg([Avg. Disk Read Queue Length])     as numeric(10,1)) as [diskReadQueueLength__avg] \n"
//
//				    + "    ,cast('' as varchar(512))                                      as [diskWriteQueueLength__avg__chart] \n"
//				    + "    ,cast(avg([Avg. Disk Write Queue Length])    as numeric(10,1)) as [diskWriteQueueLength__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [svctm__chart] \n"
				    + "    ,cast(avg([Avg. Disk sec/Transfer]*1000.0)   as numeric(10,1)) as [svctm__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [readSvctm__chart] \n"
				    + "    ,cast(avg([Avg. Disk sec/Read]*1000.0)       as numeric(10,1)) as [readSvctm__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [writeSvctm__chart] \n"
				    + "    ,cast(avg([Avg. Disk sec/Write]*1000.0)      as numeric(10,1)) as [writeSvctm__avg] \n"

				    + "    ,cast('' as varchar(512))                                      as [utilPct__chart] \n"
				    + "    ,cast(avg(100.0-[% Idle Time])               as numeric(10,1)) as [utilPct__avg] \n"

//				    + "    ,max([deviceDescription])                                      as [deviceDescription] \n"
					+ "from " + schemaPrefix + "[CmOsIostat_abs] \n"
					+ "where 1=1 \n"
					+ getReportPeriodSqlWhere()
					+ sql_skipDeviceNames
//				    + "  and [Instance] != '_Total' \n"
				    + "group by [Instance] \n"
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
				String whereKeyColumn = "Instance"; 
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("iosPerSec__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("Disk Transfers/sec").setGroupDataAggregationType(AggType.AVG)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("AVG 'iosPerSec' (Disk Transfers/sec) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("readsPerSec__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("Disk Reads/sec").setGroupDataAggregationType(AggType.AVG)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("AVG 'readsPerSec' (Disk Reads/sec) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("writesPerSec__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("Disk Writes/sec").setGroupDataAggregationType(AggType.AVG)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("AVG 'writesPerSec' (Disk Writes/sec) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("diskQueueLength__avg__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("Avg. Disk Queue Length").setGroupDataAggregationType(AggType.MAX).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("MAX 'diskQueueLength' (Avg. Disk Queue Length) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("svctm__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("[Avg. Disk sec/Transfer] * 1000.0").setGroupDataAggregationType(AggType.MAX).setDbmsDataValueColumnNameIsExpression(true).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("MAX 'svctm' (Avg. Disk sec/Transfer * 1000.0) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("readSvctm__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("[Avg. Disk sec/Read] * 1000.0").setGroupDataAggregationType(AggType.MAX).setDbmsDataValueColumnNameIsExpression(true).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("MAX 'read svctm' (Avg. Disk sec/Read * 1000.0) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("writeSvctm__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("[Avg. Disk sec/Write] * 1000.0").setGroupDataAggregationType(AggType.MAX).setDbmsDataValueColumnNameIsExpression(true).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("MAX 'write svctm' (Avg. Disk sec/Write * 1000.0) in below period")
						.validate()));

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("utilPct__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("100.0 - [% Idle Time]").setGroupDataAggregationType(AggType.AVG).setDbmsDataValueColumnNameIsExpression(true).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("AVG 'utilPct' (100.0 - % Idle Time) in below period")
						.validate()));
			}
			
		}
		else // ALL OTHERS: Linux, Unix
		{
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
			String await__avg      = !dummyRstm.hasColumnNoCase("await")    ? "" : "    ,cast(avg([await])           as numeric(10,1)) as [await__avg] \n";
			String r_await__avg    = !dummyRstm.hasColumnNoCase("r_await")  ? "" : "    ,cast(avg([r_await])         as numeric(10,1)) as [r_await__avg] \n";
			String w_await__avg    = !dummyRstm.hasColumnNoCase("w_await")  ? "" : "    ,cast(avg([w_await])         as numeric(10,1)) as [w_await__avg] \n";

			String avgrq_sz__avg   = !dummyRstm.hasColumnNoCase("avgrq-sz") ? "" : "    ,cast(avg([avgrq-sz])        as numeric(10,1)) as [avgrq-sz__avg] \n";

			String avgqu_sz__chart = !dummyRstm.hasColumnNoCase("avgqu-sz") ? "" : "    ,cast('' as varchar(512))                      as [avgqu-sz__chart] \n";
			String avgqu_sz__avg   = !dummyRstm.hasColumnNoCase("avgqu-sz") ? "" : "    ,cast(avg([avgqu-sz])        as numeric(10,1)) as [avgqu-sz__avg] \n";

			String svctm__chart    = !dummyRstm.hasColumnNoCase("svctm")    ? "" : "    ,cast('' as varchar(512))                      as [svctm__chart] \n";
			String svctm__avg      = !dummyRstm.hasColumnNoCase("svctm")    ? "" : "    ,cast(avg([svctm])           as numeric(10,1)) as [svctm__avg] \n";
			
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
				    + avgrq_sz__avg
				    + avgqu_sz__chart
				    + avgqu_sz__avg
				    + await__avg
				    + r_await__avg
				    + w_await__avg
				    + svctm__chart
				    + svctm__avg
				    + "    ,cast('' as varchar(512))                      as [utilPct__chart] \n"
				    + "    ,cast(avg([utilPct])         as numeric(10,1)) as [utilPct__avg] \n"
				    + "    ,max([deviceDescription])                      as [deviceDescription] \n"
					+ "from " + schemaPrefix + "[CmOsIostat_abs] \n"
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

//LOOK-AT; // we are potentially getting  java.lang.StackOverflowError  due to "to many time entries" in below CTE ... possibly add a "time factor table instead" or *lower* Reporting Period
         // Possibly check CTE "entries" limitations in H2
//java.lang.StackOverflowError
//at org.h2.command.Parser.parseQueryExpressionBody(Parser.java:2612)
//at org.h2.command.Parser.parseQueryExpressionBodyAndEndOfQuery(Parser.java:2605)
//at org.h2.command.Parser.parseQueryPrimary(Parser.java:2756)
//at org.h2.command.Parser.parseQueryTerm(Parser.java:2633)
//at org.h2.command.Parser.parseQueryExpressionBody(Parser.java:2612)
//at org.h2.command.Parser.parseQueryExpressionBodyAndEndOfQuery(Parser.java:2605)
//at org.h2.command.Parser.parseQueryPrimary(Parser.java:2756)
//at org.h2.command.Parser.parseQueryTerm(Parser.java:2633)
//at org.h2.command.Parser.parseQueryExpressionBody(Parser.java:2612)
//at org.h2.command.Parser.parseQueryExpressionBodyAndEndOfQuery(Parser.java:2605)
//at org.h2.command.Parser.parseQueryPrimary(Parser.java:2756)

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("readsPerSec__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
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
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("writesPerSec").setGroupDataAggregationType(AggType.AVG)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("AVG 'writesPerSec' in below period")
						.validate()));

				if (StringUtil.hasValue(avgqu_sz__chart))
				{
					_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("avgqu-sz__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("avgqu-sz").setGroupDataAggregationType(AggType.MAX).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("MAX 'avgqu-sz' in below period")
						.validate()));
				}

				if (StringUtil.hasValue(svctm__chart))
				{
					_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("svctm__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("svctm").setGroupDataAggregationType(AggType.MAX).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("MAX 'svctm' in below period")
						.validate()));
				}

				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("utilPct__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsSchemaName           (schema)
						.setDbmsTableName            ("CmOsIostat_abs")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("utilPct").setGroupDataAggregationType(AggType.AVG).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setSparklineTooltipPostfix  ("AVG 'utilPct' in below period")
						.validate()));
			}
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
