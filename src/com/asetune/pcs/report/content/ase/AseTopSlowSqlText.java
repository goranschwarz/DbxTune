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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.gui.ModelMissmatchException;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.DictCompression;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.ReportEntryAbstract.ReportEntryTableStringRenderer;
import com.asetune.pcs.report.content.ase.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;

public class AseTopSlowSqlText extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopSlowSqlText.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _longRstm;
//	private Exception           _problem = null;
	private ResultSetTableModel _skippedDsrRows;
	private List<String>        _miniChartJsList = new ArrayList<>();
	
	public AseTopSlowSqlText(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			// Get a description of this section, and column names
			sb.append(getSectionDescriptionHtml(_shortRstm, true));

			sb.append(createSkippedEntriesReport(_skippedDsrRows));

//			sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
			sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
//			sb.append(_shortRstm.toHtmlTableString("sortable"));
			sb.append(toHtmlTable(_shortRstm));

			if (_longRstm != null)
			{
				sb.append("<br>\n");
				sb.append("SQL Text by JavaSqlHashCodeShort: " + _longRstm.getRowCount() + "<br>\n");
				sb.append("Tip: To format the SQL text below you can use any online formatting tool, like <a href='https://poorsql.com/'>Poor Man's T-SQL Formatter</a><br>\n");
//				sb.append(_longRstm.toHtmlTablesVerticalString("sortable"));
//				sb.append(_longRstm.toHtmlTableString("sortable"));

				// Create a default renderer
				TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
						if ("SQLText".equals(colName))
						{
						//	return "<xmp style='width:100%; max-height:400px; overflow:auto'>" + strVal + "</xmp>";
							return "<xmp>" + strVal + "</xmp>";
						}
						return strVal;
					}
				};
				sb.append(_longRstm.toHtmlTableString("sortable", true, true, null, tableRender));
			}
		}
		
		// Write JavaScript code for CPU SparkLine
		for (String str : _miniChartJsList)
		{
			sb.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Top [SQL Captured] SLOW SQL Text/Language (order by: CpuTime__sum, origin: monSysStatement) [with gt: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"]";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
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
				"Top [SQL Captured] Slow SQL Text/Language are presented here (ordered by: CpuTime__sum) <br>" +
				"<br>" +
				"Thresholds: with GreaterThan: execTime="+_statement_gt_execTime+", logicalReads="+_statement_gt_logicalReads+", physicalReads="+_statement_gt_physicalReads+"<br>" +
				"Thresholds: having CpuTime__sum &gt;= 1000<br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monSysStatement', which is a <i>ring buffer</i>, if the buffer is small, then we will be missing entries. <br>" +
				"PCS Source table is 'MonSqlCapStatements'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>MonSqlCapStatements</i> table grouped by 'JavaSqlHashCodeShort'. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"SQL Text will be displayed in a separate table below the <i>summary</i> table.<br>" +
				"");

		// Columns description
		rstm.setColumnDescription("JavaSqlHashCodeShort"       , "A Java calculation SQLText.hashCode() for the SQL Text, but only the first ### characters. (Note: this might not bee 100% accurate, but it's something... Note: -1 = Means that The SQLText couldn't be retrived/saved to the PCS.)");
//		rstm.setColumnDescription("JavaSqlHashCode"            , "A Java calculation SQLText.hashCode() for the SQL Text (Note: this might not bee 100% accurate, but it's something... Note: -1 = Means that The SQLText couldn't be retrived/saved to the PCS.)");
		rstm.setColumnDescription("ExecCount"                  , "Number of entries for this 'JavaSqlHashCodeShort' in the report period");
		rstm.setColumnDescription("ExecCount__chart"           , "A Mini Chart of when the executions was happening (grouped in 10 minute spans)");
		rstm.setColumnDescription("SkipThis"                   , "Send a 'skip' request to DbxCentral, this means that in future reports this entry will be excluded.");
                                                              
		rstm.setColumnDescription("StartTime_min"              , "First entry was sampled for this JavaSqlHashCodeShort");
		rstm.setColumnDescription("EndTime_max"                , "Last entry was sampled for this JavaSqlHashCodeShort");
		rstm.setColumnDescription("Duration"                   , "Start/end time presented as HH:MM:SS, so we can see if this JavaSqlHashCodeShort is just for a short time or if it spans over a long period of time.");
                                                              
		rstm.setColumnDescription("Elapsed_ms__avg"            , "Average Time it took to execute this Statement during the report period (Elapsed_ms__sum/records)   ");
		rstm.setColumnDescription("CpuTime__avg"               , "Average CpuTime this Statement used            during the report period (CpuTime__sum/records)      ");
		rstm.setColumnDescription("WaitTime__avg"              , "Average avgWaitTime this Statement waited      during the report period (WaitTime__sum/records)     ");
		rstm.setColumnDescription("MemUsageKB__avg"            , "Average MemUsageKB this Statement used         during the report period (MemUsageKB__sum/records)   ");
		rstm.setColumnDescription("PhysicalReads__avg"         , "Average PhysicalReads this Statement used      during the report period (PhysicalReads__sum/records)");
		rstm.setColumnDescription("LogicalReads__avg"          , "Average LogicalReads this Statement used       during the report period (LogicalReads__sum/records) ");
		rstm.setColumnDescription("RowsAffected__avg"          , "Average RowsAffected this Statement did        during the report period (RowsAffected__sum/records) ");
                                                              
		rstm.setColumnDescription("Elapsed_ms__sum"            , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("CpuTime__sum"               , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("WaitTime__sum"              , "How much WaitTime did we use during the report period");
		rstm.setColumnDescription("MemUsageKB__sum"            , "How much MemUsageKB did we use during the report period");
		rstm.setColumnDescription("PhysicalReads__sum"         , "How much PhysicalReads did we use during the report period");
		rstm.setColumnDescription("LogicalReads__sum"          , "How much LogicalReads did we use during the report period");
		rstm.setColumnDescription("RowsAffected__sum"          , "How many RowsAffected did did this Statement do during the report period");

		rstm.setColumnDescription("LogicalReadsPerRowsAffected", "How Many LogicalReads per RowsAffected did this Statement do during the report period (Algorithm: sumLogicalReads/sumRowsAffected)");
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "MonSqlCapStatements" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// Set: _statement_gt_* variables
		getSlowQueryThresholds(conn);
		
		int topRows          = getTopRows();
		int havingSumCpuTime = 1000; // 1 second
		int dsrSkipCount     = getDsrSkipCount();
		
		String sql = "-- source table: MonSqlCapStatements \n"
			    + "select top " + (topRows + dsrSkipCount) + " \n"
//			    + "    [JavaSqlHashCode] \n"
			    + "    [JavaSqlHashCodeShort] \n"
			    + "   ,count(*)                     as [ExecCount] \n"
			    + "   ,cast('' as varchar(512))     as [ExecCount__chart] \n"
			    + " \n"
			    + "   ,cast('' as varchar(512))     as [SkipThis] \n"
			    + "	  ,min([StartTime])             as [StartTime_min] \n"
			    + "	  ,max([EndTime])               as [EndTime_max] \n"
			    + "	  ,cast('' as varchar(30))      as [Duration] \n"
			    + " \n"
			    + "   ,sum([Elapsed_ms])            as [Elapsed_ms__sum] \n"
			    + "   ,avg([Elapsed_ms])            as [Elapsed_ms__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [CpuTime__chart] \n"
			    + "   ,sum([CpuTime])               as [CpuTime__sum] \n"
			    + "   ,avg([CpuTime])               as [CpuTime__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [WaitTime__chart] \n"
			    + "   ,sum([WaitTime])              as [WaitTime__sum] \n"
			    + "   ,avg([WaitTime])              as [WaitTime__avg] \n"
			    
			    + "   ,sum([MemUsageKB])            as [MemUsageKB__sum] \n"
			    + "   ,avg([MemUsageKB])            as [MemUsageKB__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [PhysicalReads__chart] \n"
			    + "   ,sum([PhysicalReads])         as [PhysicalReads__sum] \n"
			    + "   ,avg([PhysicalReads])         as [PhysicalReads__avg] \n"
			    
			    + "   ,cast('' as varchar(512))     as [LogicalReads__chart] \n"
			    + "   ,sum([LogicalReads])          as [LogicalReads__sum] \n"
			    + "   ,avg([LogicalReads])          as [LogicalReads__avg] \n"
			    
			    + "   ,sum([RowsAffected])          as [RowsAffected__sum] \n"
			    + "   ,avg([RowsAffected])          as [RowsAffected__avg] \n"
			    
//			    + "   ,sum([QueryOptimizationTime]) as [QueryOptimizationTime__sum] \n"
//			    + "   ,avg([QueryOptimizationTime]) as [QueryOptimizationTime__avg] \n"

//			    + "   ,sum([PagesModified])         as [PagesModified__sum] \n"
//			    + "   ,avg([PagesModified])         as [PagesModified__avg] \n"

//			    + "   ,sum([PacketsSent])           as [PacketsSent__sum] \n"
//			    + "   ,avg([PacketsSent])           as [PacketsSent__avg] \n"

//			    + "   ,sum([PacketsReceived])       as [PacketsReceived__sum] \n"
//			    + "   ,avg([PacketsReceived])       as [PacketsReceived__avg] \n"
			    + " \n"

//				+ "   ,(sum([LogicalReads])*1.0) / (coalesce(sum([RowsAffected]),1)*1.0) as [LogicalReadsPerRowsAffected] \n"
				+ "   ,-9999999.0 as [LogicalReadsPerRowsAffected] \n"
				
			    + "from [MonSqlCapStatements] \n"
//			    + "where [ProcName] is NULL \n"
			    + "where [ProcedureID] = 0 \n"
				+ getReportPeriodSqlWhere("StartTime")
//			    + "group by [JavaSqlHashCode] \n"
			    + "group by [JavaSqlHashCodeShort] \n"
			    + "having [CpuTime__sum] >= " + havingSumCpuTime + " \n"
//			    + "order by [records] desc \n"
//			    + "order by [LogicalReads__sum] desc \n"
			    + "order by [CpuTime__sum] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, false, "Top SQL");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("Top SQL");
			return;
		}
		else
		{
			// Describe the table
			setSectionDescription(_shortRstm);

			// set duration
			setDurationColumn(_shortRstm, "StartTime_min", "EndTime_max", "Duration");

			// Fill in a column with a "skip link" to DbxCentral
//			setSkipEntriesUrl(_shortRstm, "SkipThis", "JavaSqlHashCode", null);
			setSkipEntriesUrl(_shortRstm, "SkipThis", "JavaSqlHashCodeShort", null);
						
			// Do some calculations (which was hard to do in a PORTABLE SQL Way)
			int pos_sumLogicalReads             = _shortRstm.findColumn("LogicalReads__sum");
			int pos_sumRowsAffected             = _shortRstm.findColumn("RowsAffected__sum");
			int pos_LogicalReadsPerRowsAffected = _shortRstm.findColumn("LogicalReadsPerRowsAffected");

			if (pos_sumLogicalReads >= 0 && pos_sumRowsAffected >= 0 && pos_LogicalReadsPerRowsAffected >= 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					//------------------------------------------------
					// set "LogicalReadsPerRowsAffected"
					long sumLogicalReads = _shortRstm.getValueAsLong(r, pos_sumLogicalReads);
					long sumRowsAffected = _shortRstm.getValueAsLong(r, pos_sumRowsAffected);

					BigDecimal calc = new BigDecimal(-1);
					if (sumRowsAffected > 0)
						calc = new BigDecimal( (sumLogicalReads*1.0) / (sumRowsAffected*1.0) ).setScale(2, RoundingMode.HALF_EVEN);
					
					_shortRstm.setValueAtWithOverride(calc, r, pos_LogicalReadsPerRowsAffected);
				}
			}


			// Remove anything from the *SKIP* entries
			_skippedDsrRows = removeSkippedEntries(_shortRstm, topRows, getDsrSkipEntries());

			// Mini Chart on "ExecCount"
			// Get data for: SparkLine - small chart values ... this will do:
			//  -- fill in the data cell with: <span class='aClassName' values='v1, v2, v2, v3...'>Mini Chart Here</span>
			//  -- return JavaScript Code to initialize the Spark line
			String whereKeyColumn = "JavaSqlHashCodeShort"; 

			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create()
					.setHtmlChartColumnName      ("ExecCount__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("1") // not actually a column name, but will be used as: sum(1)   
					.setDbmsDataValueColumnNameIsExpression(true) // do NOT quotify the 'dbmsDataValueColumnName' 
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of execution in below period")
					.validate()));

			// Mini Chart on "CPU Time"
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create()
					.setHtmlChartColumnName      ("CpuTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("CpuTime")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("CPU Time in ms")
					.validate()));

			// Mini Chart on "Wait Time"
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create()
					.setHtmlChartColumnName      ("WaitTime__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("WaitTime")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Wait Time in ms")
					.validate()));

			// Mini Chart on "Physical Reads"
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create()
					.setHtmlChartColumnName      ("PhysicalReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("PhysicalReads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of Physical Reads in below period")
					.validate()));

			// Mini Chart on "Logical Reads"
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
					SparkLineParams.create()
					.setHtmlChartColumnName      ("LogicalReads__chart")
					.setHtmlWhereKeyColumnName   (whereKeyColumn)
					.setDbmsTableName            ("MonSqlCapStatements")
					.setDbmsSampleTimeColumnName ("StartTime")
					.setDbmsDataValueColumnName  ("LogicalReads")   
					.setDbmsWhereKeyColumnName   (whereKeyColumn)
					.setSparklineTooltipPostfix  ("Number of Logical Reads in below period")
					.validate()));


			String sqlTextTable = "MonSqlCapStatements";
			if (DbUtils.checkIfTableExistsNoThrow(conn, null, null, "MonSqlCapSqlText"))
				sqlTextTable = "MonSqlCapSqlText";

			// Check if table "MonSqlCapStatements or MonSqlCapSqlText" has Dictionary Compressed Columns (any columns ends with "$dcc$")
			boolean hasDictCompCols = false;
			try {
				hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, sqlTextTable);
			} catch (SQLException ex) {
				_logger.error("Problems checking for Dictionary Compressed Columns in table '" + sqlTextTable + "'.", ex);
			}

			// Get SQL Text
			if (_shortRstm.getRowCount() > 0)
			{
				// For each record... try to get the SQL Text based on the JavaSqlHashCode
				int pos_JavaSqlHashCode = _shortRstm.findColumn("JavaSqlHashCodeShort");
				if (pos_JavaSqlHashCode != -1)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						int JavaSqlHashCode = _shortRstm.getValueAsInteger(r, pos_JavaSqlHashCode);
						
						if (JavaSqlHashCode != -1)
						{
							String col_SQLText = "[SQLText]";
							if (hasDictCompCols)
								col_SQLText = DictCompression.getRewriteForColumnName(sqlTextTable, "SQLText$dcc$").replace(" AS [SQLText]", "");

//							sql = ""
//								    + "select distinct [JavaSqlHashCode], [ServerLogin], cast('' as varchar(512)) as [SkipThis], " + col_SQLText + " as [SQLText] \n"
//								    + "from [" + sqlTextTable + "] \n"
//								    + "where [JavaSqlHashCode] = " + JavaSqlHashCode + " \n"
//								    + "";
							sql = ""
								    + "select top 1 \n"
								    + "    [JavaSqlHashCodeShort] \n"
								    + "   ,cast('' as varchar(512)) as [SkipThis] \n"
								    + "   ,cast('' as varchar(512)) as [CpuTime] \n"
								    + "   ," + col_SQLText + " as [SQLText] \n"
								    + "from [" + sqlTextTable + "] \n"
								    + "where [JavaSqlHashCodeShort] = " + JavaSqlHashCode + " \n"
								    + "";
							
							sql = conn.quotifySqlString(sql);
							try ( Statement stmnt = conn.createStatement() )
							{
								// Unlimited execution time
								stmnt.setQueryTimeout(0);
								try ( ResultSet rs = stmnt.executeQuery(sql) )
								{
//									ResultSetTableModel rstm = new ResultSetTableModel(rs, "SqlDetails");
									ResultSetTableModel rstm = createResultSetTableModel(rs, "SqlDetails", sql);
									
									if (_longRstm == null)
										_longRstm = rstm;
									else
										_longRstm.add(rstm);
	
									// Fill in a column with a "skip link" to DbxCentral
//									setSkipEntriesUrl(_longRstm, "SkipThis", "JavaSqlHashCode", "SQLText");
									setSkipEntriesUrl(_longRstm, "SkipThis", "JavaSqlHashCodeShort", "SQLText");
									
									if (_logger.isDebugEnabled())
										_logger.debug("SqlDetails.getRowCount()="+ rstm.getRowCount());
								}
							}
							catch(SQLException ex)
							{
								setProblemException(ex);
	
//								_logger.warn("Problems getting SQL by JavaSqlHashCode = "+JavaSqlHashCode+": " + ex);
								_logger.warn("Problems getting SQL by JavaSqlHashCodeShort = "+JavaSqlHashCode+": " + ex);
							} 
							catch(ModelMissmatchException ex)
							{
								setProblemException(ex);
	
//								_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by JavaSqlHashCode = "+JavaSqlHashCode+": " + ex);
								_logger.warn("Problems (merging into previous ResultSetTableModel) when getting SQL by JavaSqlHashCodeShort = "+JavaSqlHashCode+": " + ex);
							} 
						}
					}

					// Mini Chart on "CPU Time"
					// COPY Cell data from the "details" table
					_longRstm.copyCellContentFrom(_shortRstm, "JavaSqlHashCodeShort", "CpuTime__chart",   "JavaSqlHashCodeShort", "CpuTime");
				}
			}
		}
	}
}
