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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ase.SparklineHelper.AggType;
import com.asetune.pcs.report.content.ase.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.ase.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class AseCmDeviceIo extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseTopCmCachedProcs.class);

	private ResultSetTableModel _shortRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public AseCmDeviceIo(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
	}

	@Override
	public boolean hasShortMessageText()
	{
		return false;
	}

	@Override
	public void writeShortMessageText(Writer w)
	throws IOException
	{
	}

	@Override
	public void writeMessageText(Writer sb)
	throws IOException
	{
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
		return "Device IO, (order by: Name, origin: CmDeviceIo_diff / monDeviceIO)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmDeviceIo_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		 // just to get Column names
		String dummySql = "select * from [CmDeviceIo_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, false, "metadata");
		if (dummyRstm == null)
		{
			String msg = "Table 'CmDeviceIo_diff' did not exist. So Performance Counters for this hasn't been sampled during this period.";

			//addMessage(msg);
			setProblemException(new Exception(msg));

			_shortRstm = ResultSetTableModel.createEmpty("CmDeviceIo");
			return;
		}

		String sql = ""
			    + "select \n"
			    + "     [LogicalName] \n"
			    + "    ,sum([TotalIOs])                    as [TotalIOs__sum] \n"
			    + "    ,CASE WHEN sum([TotalIOs]) = 0 THEN NULL ELSE cast(sum([Reads])    * 1.0 / sum([TotalIOs]) * 100.0 as numeric(10,1)) END as [ReadsPct] \n"
			    + "    ,CASE WHEN sum([TotalIOs]) = 0 THEN NULL ELSE cast(sum([APFReads]) * 1.0 / sum([TotalIOs]) * 100.0 as numeric(10,1)) END as [APFReadsPct] \n"
			    + "    ,CASE WHEN sum([TotalIOs]) = 0 THEN NULL ELSE cast(sum([Writes])   * 1.0 / sum([TotalIOs]) * 100.0 as numeric(10,1)) END as [WritesPct] \n"
			    + "    ,sum([Reads])                       as [Reads__sum] \n"
			    + "    ,sum([APFReads])                    as [APFReads__sum] \n"
			    + "    ,sum([Writes])                      as [Writes__sum] \n"
			    + "    ,cast('' as varchar(512))           as [Reads__chart] \n"
			    + "    ,cast('' as varchar(512))           as [APFReads__chart] \n"
			    + "    ,cast('' as varchar(512))           as [Writes__chart] \n"
			    + "    ,sum([DevSemaphoreRequests])        as [DevSemaphoreRequests__sum] \n"
			    + "    ,sum([DevSemaphoreWaits])           as [DevSemaphoreWaits__sum] \n"
			    + "    ,sum([IOTime])                      as [IOTime__sum] \n"
			    + "    ,sum([ReadTime])                    as [ReadTime__sum] \n"
			    + "    ,sum([WriteTime])                   as [WriteTime__sum] \n"
			    + "    ,CASE WHEN sum([IOTime])    = 0 THEN NULL ELSE cast(sum([TotalIOs]) * 1.0 / sum([IOTime])    as numeric(10,1)) END as [TotalServiceTimeMs__avg] \n"
			    + "    ,CASE WHEN sum([ReadTime])  = 0 THEN NULL ELSE cast(sum([Reads])    * 1.0 / sum([ReadTime])  as numeric(10,1)) END as [ReadServiceTimeMs__avg] \n"
			    + "    ,CASE WHEN sum([WriteTime]) = 0 THEN NULL ELSE cast(sum([Writes])   * 1.0 / sum([WriteTime]) as numeric(10,1)) END as [WriteServiceTimeMs__avg] \n"
			    + "    ,max([AvgServ_ms])                  as [TotalServiceTimeMs__max] \n"
			    + "    ,max([ReadServiceTimeMs])           as [ReadServiceTimeMs__max] \n"
			    + "    ,max([WriteServiceTimeMs])          as [WriteServiceTimeMs__max] \n"
			    + "    ,cast('' as varchar(512))           as [TotalServiceTimeMs__chart] \n"
			    + "    ,cast('' as varchar(512))           as [ReadServiceTimeMs__chart] \n"
			    + "    ,cast('' as varchar(512))           as [WriteServiceTimeMs__chart] \n"
			    + "    ,max([DeviceType])                  as [DeviceType__max] \n"
			    + "    ,max([PhysicalName])                as [PhysicalName__max] \n"
//			    + " \n"
//			    + "    ,max([SessionStartTime])            as [SessionStartTime__max] \n"
//			    + "    ,max([SessionSampleTime])           as [SessionSampleTime__max] \n"
//			    + "    ,max([CmSampleTime])                as [CmSampleTime__max] \n"
//			    + "    ,max([CmSampleMs])                  as [CmSampleMs__max] \n"
//			    + "    ,sum([CmNewDiffRateRow])            as [CmNewDiffRateRow__sum] \n"
			    + "from [CmDeviceIo_diff] \n"
			    + "group by [LogicalName] \n"
			    + "order by 1 \n"
			    + "";
		
		_shortRstm = executeQuery(conn, sql, false, "CmDeviceIo");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("CmDeviceIo");
			return;
		}
		else
		{
			// Describe the table
			setSectionDescription(_shortRstm);
			
			// Mini Chart on "ExecCount"
			String whereKeyColumn = "LogicalName"; 

			// Mini Chart on "Physical Reads"
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("Reads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmDeviceIo_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("Reads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of Reads in below period")
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("APFReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmDeviceIo_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("APFReads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of APFReads in below period")
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("Writes__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmDeviceIo_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("Writes")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of Writes in below period")
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("TotalServiceTimeMs__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmDeviceIo_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("AvgServ_ms").setGroupDataAggregationType(AggType.MAX)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'AvgServ_ms' in below period")
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("ReadServiceTimeMs__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmDeviceIo_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("ReadServiceTimeMs").setGroupDataAggregationType(AggType.MAX)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'ReadServiceTimeMs' in below period")
					.validate()));
			
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      ("WriteServiceTimeMs__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("CmDeviceIo_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  ("WriteServiceTimeMs").setGroupDataAggregationType(AggType.MAX)
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("MAX 'WriteServiceTimeMs' in below period")
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
				"Device access, or device <i>service time</i> (ordered by: LogicalName) <br>" +
				"<br>" +
//				"What devices (at what time) have undelaying storage <i>problems/issues</i>... manifested as long access time<br>" +
//				"<br>" +
				"ASE Source table is 'master.dbo.monDeviceIO'. <br>" +
				"PCS Source table is 'CmDeviceIo_diff'. (PCS = Persistent Counter Store) <br>" +
				"");
	}
}
