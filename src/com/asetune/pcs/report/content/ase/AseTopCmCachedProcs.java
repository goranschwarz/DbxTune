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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.ResultSetTableModel.TableStringRenderer;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.SparklineHelper;
import com.asetune.pcs.report.content.SparklineHelper.AggType;
import com.asetune.pcs.report.content.SparklineHelper.DataSource;
import com.asetune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;
import com.asetune.utils.HtmlTableProducer;
import com.asetune.utils.HtmlTableProducer.ColumnCopyDef;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRender;
import com.asetune.utils.HtmlTableProducer.ColumnCopyRow;
import com.asetune.utils.HtmlTableProducer.ColumnStatic;
import com.asetune.utils.StringUtil;

public class AseTopCmCachedProcs extends AseAbstract
{
	private static Logger _logger = Logger.getLogger(AseTopCmCachedProcs.class);

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _ssqlRstm;
	private ResultSetTableModel _sparklinesRstm;
//	private ResultSetTableModel _cmCachedProcsMin;
//	private ResultSetTableModel _cmCachedProcsMax;
//	private Exception           _problem = null;
	private List<String>        _miniChartJsList = new ArrayList<>();

	private Map<Map<String, Object>, SqlCapExecutedSqlEntries> _keyToExecutedSql;
	
	public AseTopCmCachedProcs(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
		}
		else
		{
			if (isFullMessageType())
			{
				// Get a description of this section, and column names
				sb.append(getSectionDescriptionHtml(_shortRstm, true));

//				sb.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
				sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
//				sb.append(toHtmlTable(_shortRstm));

				// Create a default renderer
				TableStringRenderer tableRender = new ReportEntryTableStringRenderer()
				{
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
						if ("txt".equals(colName))
						{
							// Get Actual Executed SQL Text for current 'DBName, ObjectName'
							String dbName   = rstm.getValueAsString (row, "DBName");
							String procName = rstm.getValueAsString (row, "ObjectName");
							
							Map<String, Object> whereColValMap = new LinkedHashMap<>();
							whereColValMap.put("DBName"    , dbName);
							whereColValMap.put("ProcName"  , procName);

							String executedSqlText = getSqlCapExecutedSqlTextAsString(_keyToExecutedSql, whereColValMap);

							// Put the "Actual Executed SQL Text" as a "tooltip"
							return "<div title='Click for Detailes' "
									+ "data-toggle='modal' "
									+ "data-target='#dbx-view-sqltext-dialog' "
									+ "data-objectname='" + (dbName + ".." + procName) + "' "
									+ "data-tooltip=\""   + executedSqlText     + "\" "
									+ ">&#x1F4AC;</div>"; // symbol popup with "..."
						}

						return strVal;
					}
				};
				sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, tableRender));


				if (_ssqlRstm != null)
				{
					sb.append("Statement Cache Entries Count: " + _ssqlRstm.getRowCount() + "<br>\n");
					sb.append(toHtmlTable(_ssqlRstm));
				}
			}

			if (_sparklinesRstm != null)
			{
				sb.append("<br>\n");
				sb.append("<details open> \n");
				sb.append("<summary>Details for above Procedures (click to collapse) </summary> \n");
				
				sb.append("<br>\n");
				sb.append("Details by 'procedure', Row Count: " + _sparklinesRstm.getRowCount() + "\n");
				sb.append(toHtmlTable(_sparklinesRstm));

				sb.append("\n");
				sb.append("</details> \n");
			}
		}
		
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
		return "Top Procedure Calls (order by: CPUTime__sum, origin: CmCachedProcs / monCachedProcedures)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmCachedProcs_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		createTopCmCachedProcs(conn, srvName, pcsSavedConf, localConf);
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
				"Both slow and fast SQL Statements are presented here (ordered by: CPUTime__sum) <br>" +
				"<br>" +
				"This so you can see if there are problems with <i>procedures</i> that falls <i>just below</i> the threshold for 'Slow SQL Statements' <br>" +
				"<br>" +
				"ASE Source table is 'master.dbo.monCachedProcedures' where both StoredProcedures, StatementCache and DynamicSql are displayed. <br>" +
				"PCS Source table is 'CmCachedProcs_diff'. (PCS = Persistent Counter Store) <br>" +
				"<br>" +
				"The report <i>summarizes</i> (min/max/count/sum/avg) all entries/samples from the <i>source_DIFF</i> table. <br>" +
				"Typically the column name <i>postfix</i> will tell you what aggregate function was used. <br>" +
				"");

		// Columns description
		rstm.setColumnDescription("DBName"                , "Database name");
		rstm.setColumnDescription("ObjectName"            , "Procedure Name or '*ss' for StatementCache entries and '*sq' for DynamicPreparedSql");
		rstm.setColumnDescription("Remark"                , "Used by the 'validate' method to check if values looks ok. (for example if 'CPUTime__sum' is above 24H, you will get a WARNING) ");
		rstm.setColumnDescription("PlanID_count"          , "Number of entries this plan has in the period");
		rstm.setColumnDescription("SessionSampleTime_min" , "First entry was sampled at this time");
		rstm.setColumnDescription("SessionSampleTime_max" , "Last entry was sampled at this time");
		rstm.setColumnDescription("newDiffRow_sum"        , "Number of Diff Records that was seen for the first time.");
		rstm.setColumnDescription("CmSampleMs_sum"        , "Number of milliseconds this object has been available for sampling");
		rstm.setColumnDescription("CompileDate_min"       , "First time this object was compiled");
		rstm.setColumnDescription("CompileDate_max"       , "Last time this object was compiled");
		
		rstm.setColumnDescription("MemUsageKB__max"        , "Max Memory used by this object during the report period");
		rstm.setColumnDescription("RequestCntDiff__sum"    , "How many times this object was Requested");
		rstm.setColumnDescription("TempdbRemapCnt__sum"    , "How many times this object was was recompiled due to 'tempdb changes'.");
		rstm.setColumnDescription("ExecutionCount__sum"    , "How many times this object was Executed");
		                                                  
		rstm.setColumnDescription("CPUTime__sum"           , "How much CPUTime did we use during the report period");
		rstm.setColumnDescription("ExecutionTime__sum"     , "How many milliseconds did we spend in execution during the report period");
		rstm.setColumnDescription("PhysicalReads__sum"     , "How many Physical Reads did we do during the report period");
		rstm.setColumnDescription("LogicalReads__sum"      , "How many Logical Reads did we do during the report period");
		rstm.setColumnDescription("PhysicalWrites__sum"    , "How many Physical Writes did we do during the report period");
		rstm.setColumnDescription("PagesWritten__sum"      , "How many PagesWritten (8 pages = 1 physical IO if we did 'extent io' writes) did we do during the report period");
		                                                  
		rstm.setColumnDescription("AvgCPUTime__max"        , "Average CPUTime per execution");
		rstm.setColumnDescription("AvgExecutionTime__max"  , "Average ExecutionTime for this object");
		rstm.setColumnDescription("AvgPhysicalReads__max"  , "Average PhysicalReads per execution");
		rstm.setColumnDescription("AvgLogicalReads__max"   , "Average gLogicalReads per execution");
		rstm.setColumnDescription("AvgPhysicalWrites__max" , "Average PhysicalWrites per execution");
		rstm.setColumnDescription("AvgPagesWritten__max"   , "Average PagesWritten per execution");
	}

	private void createTopCmCachedProcs(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();

		// Get ASE Page Size
		int asePageSize = -1;
		try	{ asePageSize = getAsePageSizeFromMonDdlStorage(conn); }
		catch (SQLException ex) { }
		int asePageSizeDivider = 1024 * 1024 / asePageSize; // 2k->512, 4k->256, 8k=128, 16k=64

		// Get a dummy "metadata" for the table (so we can check what columns exists)
		String dummySql = "select * from [CmCachedProcs_diff] where 1 = 2";
		ResultSetTableModel dummyRstm = executeQuery(conn, dummySql, true, "metadata");

		//  SQL for: only records that has been diff calculations (not first time seen, some ASE Versions has a bug that do not clear counters on reuse)
		String sql_and_skipNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 0 \n"; // This is the "old" way... and used for backward compatibility
		String sql_and_onlyNewOrDiffRateRows = "  and [CmNewDiffRateRow] = 1 \n"; // This is the "old" way... and used for backward compatibility
//		String sql_and_skipAggregatedRows    = "";
		String col_newDiffRow_sum            = " ,sum([CmNewDiffRateRow])     as [newDiffRow_sum] \n";
		if (dummyRstm.hasColumn("CmRowState")) // New column name for 'CmNewDiffRateRow' (which is a bitwise state column)
		{
			// the below will produce for H2:     and  BITAND([CmRowState], 1) = ???   
			//                        for OTHERS: and  ([CmRowState] & 1) = ???
			sql_and_skipNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";
			sql_and_onlyNewOrDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = " + CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW + " \n";

//			sql_and_skipAggregatedRows    = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_AGGREGATED_ROW) + " = 0 \n";

			col_newDiffRow_sum = " ,sum(" + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + ")     as [newDiffRow_sum] \n";
		}
//FIXME; double check the code for "CmNewDiffRateRow and CmRowState"

		// in some versions (ASE 16.0 SP2 I have observed this in) seems to (in some cases) keep the old counter values even if we compile a new PlanID
		// So DO NOT TRUST NEWLY created PlanID's 
		// Although this can create statistical problems:
		//   - if a procedure is *constantly* recompiled (due to "whatever" reason), those procedures will be discarded from below report
		boolean skipNewDiffRateRows    = localConf.getBooleanProperty(this.getClass().getSimpleName()+".skipNewDiffRateRows", false);
		boolean hasSkipNewDiffRateRows = localConf.hasProperty(       this.getClass().getSimpleName()+".skipNewDiffRateRows");

		// try to figure out if we have *new* diff values that exceeds (using column 'ExecutionCount')
		if ( ! hasSkipNewDiffRateRows )
		{
			int executionCountThreshold = 10000;
			String sql = ""
				    + "select count(*) \n"
				    + "from [CmCachedProcs_diff] \n"
				    + "where 1 = 1 \n"
				    + sql_and_onlyNewOrDiffRateRows
				    + "  and [ExecutionCount] > " + executionCountThreshold + " \n"
				    + "  and [ObjectName] NOT like '*ss%' \n" // *ss = Statement Cache
				    + "  and [ObjectName] NOT like '*sq%' \n" // *sq = Dynamic SQL
					+ getReportPeriodSqlWhere()
				    + "";
			sql = conn.quotifySqlString(sql);
			
			int foundCount = 0;
			try ( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql) )
			{
				while (rs.next())
					foundCount = rs.getInt(1);
			}
			catch(SQLException ex)
			{
				_logger.warn("Problems trying to identify if we have any newDiffRate records that seems 'strange'... executed SQL='" + sql + "'.");
			}
			
			if (foundCount > 0)
			{
				skipNewDiffRateRows = true;
				_logger.warn("Found " + foundCount + " records in 'CmCachedProcs_diff' which had 'CmNewDiffRateRow=1' and 'ExecutionCount>" + executionCountThreshold + "'. This means I will EXCLUDE records with 'CmNewDiffRateRow=1'.");
				
				addWarningMessage("Found " + foundCount + " records in 'CmCachedProcs_diff' which had 'CmNewDiffRateRow=1' and 'ExecutionCount>" + executionCountThreshold + "'. This means I will EXCLUDE records with 'CmNewDiffRateRow=1'.");
			}
		}
		if (skipNewDiffRateRows)
			addWarningMessage("Records with the flag 'CmNewDiffRateRow=1' will NOT be part of the Report. This means that the first time a procedure is executed (or recompiled and executed), the first execution counter will NOT be part of the statistics.");
		
		
		//-------------------------------------------------------
		// DIFF columns from: CmCachedProcs
		//-------------------------------------------------------
		//      public static final String[] DIFF_COLUMNS     = new String[] {
		//      		"RequestCntDiff", 
		//      		"TempdbRemapCnt", 
		//      		"ExecutionCount", 
		//      		"CPUTime", 
		//      		"ExecutionTime", 
		//      		"PhysicalReads", 
		//      		"LogicalReads", 
		//      		"PhysicalWrites", 
		//      		"PagesWritten",
		//      		"SnapCodegenTime",    // 16.0 SP2
		//      		"SnapJITTime",        // 16.0 SP2
		//      		"SnapExecutionTime",  // 16.0 SP2
		//      		"SnapExecutionCount"  // 16.0 SP2
		//      	};
		//-------------------------------------------------------
		// Column description from ASE 16.0 recording
		//-------------------------------------------------------
		// RS> Col# Label                JDBC Type Name           Guessed DBMS type Source Table                                    
		// RS> ---- -------------------- ------------------------ ----------------- ------------------------------------------------
		// RS> 1    SessionStartTime     java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 2    SessionSampleTime    java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 3    CmSampleTime         java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 4    CmSampleMs           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 5    CmNewDiffRateRow     java.sql.Types.TINYINT   TINYINT           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 6    PlanID               java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 7    DBName               java.sql.Types.VARCHAR   VARCHAR(30)       PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 8    ObjectName           java.sql.Types.VARCHAR   VARCHAR(255)      PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 9    SSQLID               java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 10   Hashkey              java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 11   ObjectType           java.sql.Types.VARCHAR   VARCHAR(32)       PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 12   Active               java.sql.Types.VARCHAR   VARCHAR(3)        PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 13   MemUsageKB           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 14   CompileDate          java.sql.Types.TIMESTAMP TIMESTAMP         PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 15   CompileAgeInSec      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 16   RequestCnt           java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 17   RequestCntDiff       java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 18   TempdbRemapCnt       java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 19   AvgTempdbRemapTime   java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 20   ExecutionCount       java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 21   AvgCPUTime           java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 22   AvgExecutionTime     java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 23   AvgPhysicalReads     java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 24   AvgLogicalReads      java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 25   AvgPhysicalWrites    java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 26   AvgPagesWritten      java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 27   CPUTime              java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 28   ExecutionTime        java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 29   PhysicalReads        java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 30   LogicalReads         java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 31   PhysicalWrites       java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 32   PagesWritten         java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 33   SnapExecutionCount   java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 34   AvgSnapCodegenTime   java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 35   AvgSnapJITTime       java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 36   AvgSnapExecutionTime java.sql.Types.DECIMAL   DECIMAL(18,1)     PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 37   SnapCodegenTime      java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 38   SnapJITTime          java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 39   SnapExecutionTime    java.sql.Types.INTEGER   INTEGER           PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff
		// RS> 40   Hashkey2             java.sql.Types.BIGINT    BIGINT            PROD_A1_ASE_2022-02-09.PUBLIC.CmCachedProcs_diff		

		// Create Column selects, but only if the column exists in the PCS Table
		String col_MemUsageKB__sum                = !dummyRstm.hasColumnNoCase("MemUsageKB"              ) ? "" : "    ,sum([MemUsageKB])            as [MemUsageKB__sum]         \n"; 
		String col_TempdbRemapCnt__sum            = !dummyRstm.hasColumnNoCase("TempdbRemapCnt"          ) ? "" : "    ,sum([TempdbRemapCnt])        as [TempdbRemapCnt__sum]         \n"; 
		String col_ExecutionCount__sum            = !dummyRstm.hasColumnNoCase("ExecutionCount"          ) ? "" : "    ,sum([ExecutionCount])        as [ExecutionCount__sum]         \n"; 
		String col_CPUTime__sum                   = !dummyRstm.hasColumnNoCase("CPUTime"                 ) ? "" : "    ,sum([CPUTime])               as [CPUTime__sum]                \n"; 
		String col_ExecutionTime__sum             = !dummyRstm.hasColumnNoCase("ExecutionTime"           ) ? "" : "    ,sum([ExecutionTime])         as [ExecutionTime__sum]          \n"; 
		String col_PhysicalReads__sum             = !dummyRstm.hasColumnNoCase("PhysicalReads"           ) ? "" : "    ,sum([PhysicalReads])         as [PhysicalReads__sum]          \n"; 
		String col_LogicalReads__sum              = !dummyRstm.hasColumnNoCase("LogicalReads"            ) ? "" : "    ,sum([LogicalReads])          as [LogicalReads__sum]           \n"; 
		String col_LogicalReadsMb__sum            = !dummyRstm.hasColumnNoCase("LogicalReads"            ) ? "" : "    ,sum([LogicalReads])*1.0/"+asePageSizeDivider+" as [LogicalReadsMb__sum]         \n"; 
		String col_PhysicalWrites__sum            = !dummyRstm.hasColumnNoCase("PhysicalWrites"          ) ? "" : "    ,sum([PhysicalWrites])        as [PhysicalWrites__sum]         \n"; 
		String col_PagesWritten__sum              = !dummyRstm.hasColumnNoCase("PagesWritten"            ) ? "" : "    ,sum([PagesWritten])          as [PagesWritten__sum]           \n"; 
		String col_SnapExecutionCount__sum        = !dummyRstm.hasColumnNoCase("SnapExecutionCount"      ) ? "" : "    ,sum([SnapExecutionCount])    as [SnapExecutionCount__sum]     \n"; 
		String col_SnapCodegenTime__sum           = !dummyRstm.hasColumnNoCase("SnapCodegenTime"         ) ? "" : "    ,sum([SnapCodegenTime])       as [SnapCodegenTime__sum]        \n"; 
		String col_SnapJITTime__sum               = !dummyRstm.hasColumnNoCase("SnapJITTime"             ) ? "" : "    ,sum([SnapJITTime])           as [SnapJITTime__sum]            \n"; 
		String col_SnapExecutionTime__sum         = !dummyRstm.hasColumnNoCase("SnapExecutionTime"       ) ? "" : "    ,sum([SnapExecutionTime])     as [SnapExecutionTime__sum]      \n"; 

		String col_MemUsageKB__avg                = !dummyRstm.hasColumnNoCase("MemUsageKB"              ) ? "" : "    ,cast( sum([MemUsageKB])         * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [MemUsageKB__avg]         \n"; 
		String col_TempdbRemapCnt__avg            = !dummyRstm.hasColumnNoCase("TempdbRemapCnt"          ) ? "" : "    ,cast( sum([TempdbRemapCnt])     * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [TempdbRemapCnt__avg]         \n"; 
//		String col_ExecutionCount__avg            = !dummyRstm.hasColumnNoCase("ExecutionCount"          ) ? "" : "    ,cast( sum([ExecutionCount])     * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [ExecutionCount__avg]         \n"; 
		String col_CPUTime__avg                   = !dummyRstm.hasColumnNoCase("CPUTime"                 ) ? "" : "    ,cast( sum([CPUTime])            * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [CPUTime__avg]                \n"; 
		String col_ExecutionTime__avg             = !dummyRstm.hasColumnNoCase("ExecutionTime"           ) ? "" : "    ,cast( sum([ExecutionTime])      * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [ExecutionTime__avg]          \n"; 
		String col_PhysicalReads__avg             = !dummyRstm.hasColumnNoCase("PhysicalReads"           ) ? "" : "    ,cast( sum([PhysicalReads])      * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [PhysicalReads__avg]          \n"; 
		String col_LogicalReads__avg              = !dummyRstm.hasColumnNoCase("LogicalReads"            ) ? "" : "    ,cast( sum([LogicalReads])       * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [LogicalReads__avg]           \n"; 
		String col_LogicalReadsMb__avg            = !dummyRstm.hasColumnNoCase("LogicalReads"            ) ? "" : "    ,cast( sum([LogicalReads])*1.0/"+asePageSizeDivider+" / nullif(sum([ExecutionCount]), 0) as numeric(19,1)) as [LogicalReadsMb__avg]           \n"; 
		String col_PhysicalWrites__avg            = !dummyRstm.hasColumnNoCase("PhysicalWrites"          ) ? "" : "    ,cast( sum([PhysicalWrites])     * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [PhysicalWrites__avg]         \n"; 
		String col_PagesWritten__avg              = !dummyRstm.hasColumnNoCase("PagesWritten"            ) ? "" : "    ,cast( sum([PagesWritten])       * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [PagesWritten__avg]           \n"; 
		String col_SnapExecutionCount__avg        = !dummyRstm.hasColumnNoCase("SnapExecutionCount"      ) ? "" : "    ,cast( sum([SnapExecutionCount]) * 1.0 / nullif(sum([ExecutionCount]), 0)    as numeric(19,1)) as [SnapExecutionCount__avg]     \n"; 
		String col_SnapCodegenTime__avg           = !dummyRstm.hasColumnNoCase("SnapCodegenTime"         ) ? "" : "    ,cast( sum([SnapCodegenTime])    * 1.0 / nullif(sum([SnapExecutionTime]), 0) as numeric(19,1)) as [SnapCodegenTime__avg]        \n"; 
		String col_SnapJITTime__avg               = !dummyRstm.hasColumnNoCase("SnapJITTime"             ) ? "" : "    ,cast( sum([SnapJITTime])        * 1.0 / nullif(sum([SnapExecutionTime]), 0) as numeric(19,1)) as [SnapJITTime__avg]            \n"; 
		String col_SnapExecutionTime__avg         = !dummyRstm.hasColumnNoCase("SnapExecutionTime"       ) ? "" : "    ,cast( sum([SnapExecutionTime])  * 1.0 / nullif(sum([SnapExecutionTime]), 0) as numeric(19,1)) as [SnapExecutionTime__avg]      \n"; 

		String col_MemUsageKB__chart              = !dummyRstm.hasColumnNoCase("MemUsageKB"              ) ? "" : "    ,cast('' as varchar(512))     as [MemUsageKB__chart]             \n"; 
		String col_TempdbRemapCnt__chart          = !dummyRstm.hasColumnNoCase("TempdbRemapCnt"          ) ? "" : "    ,cast('' as varchar(512))     as [TempdbRemapCnt__chart]         \n"; 
		String col_ExecutionCount__chart          = !dummyRstm.hasColumnNoCase("ExecutionCount"          ) ? "" : "    ,cast('' as varchar(512))     as [ExecutionCount__chart]         \n"; 
		String col_CPUTime__chart                 = !dummyRstm.hasColumnNoCase("CPUTime"                 ) ? "" : "    ,cast('' as varchar(512))     as [CPUTime__chart]                \n"; 
		String col_ExecutionTime__chart           = !dummyRstm.hasColumnNoCase("ExecutionTime"           ) ? "" : "    ,cast('' as varchar(512))     as [ExecutionTime__chart]          \n"; 
		String col_PhysicalReads__chart           = !dummyRstm.hasColumnNoCase("PhysicalReads"           ) ? "" : "    ,cast('' as varchar(512))     as [PhysicalReads__chart]          \n"; 
		String col_LogicalReads__chart            = !dummyRstm.hasColumnNoCase("LogicalReads"            ) ? "" : "    ,cast('' as varchar(512))     as [LogicalReads__chart]           \n"; 
		String col_LogicalReadsMb__chart          = !dummyRstm.hasColumnNoCase("LogicalReads"            ) ? "" : "    ,cast('' as varchar(512))     as [LogicalReadsMb__chart]         \n"; 
		String col_PhysicalWrites__chart          = !dummyRstm.hasColumnNoCase("PhysicalWrites"          ) ? "" : "    ,cast('' as varchar(512))     as [PhysicalWrites__chart]         \n"; 
		String col_PagesWritten__chart            = !dummyRstm.hasColumnNoCase("PagesWritten"            ) ? "" : "    ,cast('' as varchar(512))     as [PagesWritten__chart]           \n"; 
		String col_SnapExecutionCount__chart      = !dummyRstm.hasColumnNoCase("SnapExecutionCount"      ) ? "" : "    ,cast('' as varchar(512))     as [SnapExecutionCount__chart]     \n"; 
		String col_SnapCodegenTime__chart         = !dummyRstm.hasColumnNoCase("SnapCodegenTime"         ) ? "" : "    ,cast('' as varchar(512))     as [SnapCodegenTime__chart]        \n"; 
		String col_SnapJITTime__chart             = !dummyRstm.hasColumnNoCase("SnapJITTime"             ) ? "" : "    ,cast('' as varchar(512))     as [SnapJITTime__chart]            \n"; 
		String col_SnapExecutionTime__chart       = !dummyRstm.hasColumnNoCase("SnapExecutionTime"       ) ? "" : "    ,cast('' as varchar(512))     as [SnapExecutionTime__chart]      \n"; 
		
		
		
		
		
		
//		String ExecutionCount_sum    = !dummyRstm.hasColumnNoCase("ExecutionCount"   ) ? "" : "    ,sum([ExecutionCount])    as [ExecutionCount_sum] \n"   ;
//
//		String CPUTime_sum           = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "" : "    ,sum([CPUTime])           as [CPUTime_sum] \n"          ; 
//		String AvgCPUTime_max        = !dummyRstm.hasColumnNoCase("AvgCPUTime"       ) ? "" : "    ,max([AvgCPUTime])        as [AvgCPUTime_max] \n"       ; 
//		String ExecutionTime_sum     = !dummyRstm.hasColumnNoCase("ExecutionTime"    ) ? "" : "    ,sum([ExecutionTime])     as [ExecutionTime_sum] \n"    ; 
//		String AvgExecutionTime_max  = !dummyRstm.hasColumnNoCase("AvgExecutionTime" ) ? "" : "    ,max([AvgExecutionTime])  as [AvgExecutionTime_max] \n" ; 
//		String PhysicalReads_sum     = !dummyRstm.hasColumnNoCase("PhysicalReads"    ) ? "" : "    ,sum([PhysicalReads])     as [PhysicalReads_sum] \n"    ; 
//		String AvgPhysicalReads_max  = !dummyRstm.hasColumnNoCase("AvgPhysicalReads" ) ? "" : "    ,max([AvgPhysicalReads])  as [AvgPhysicalReads_max] \n" ; 
//		String LogicalReads_sum      = !dummyRstm.hasColumnNoCase("LogicalReads"     ) ? "" : "    ,sum([LogicalReads])      as [LogicalReads_sum] \n"     ; 
//		String AvgLogicalReads_max   = !dummyRstm.hasColumnNoCase("AvgLogicalReads"  ) ? "" : "    ,max([AvgLogicalReads])   as [AvgLogicalReads_max] \n"  ; 
//		String LogicalReadsMb_sum    = !dummyRstm.hasColumnNoCase("LogicalReadsMb"   ) ? "" : "    ,sum([LogicalReads]*1.0)/"+asePageSizeDivider+"      as [LogicalReadsMb_sum] \n"     ; 
//		String AvgLogicalReadsMb_max = !dummyRstm.hasColumnNoCase("AvgLogicalReadsMb") ? "" : "    ,max([AvgLogicalReads*1.0])/"+asePageSizeDivider+"   as [AvgLogicalReadsMb_max] \n"  ; 
//		String PhysicalWrites_sum    = !dummyRstm.hasColumnNoCase("PhysicalWrites"   ) ? "" : "    ,sum([PhysicalWrites])    as [PhysicalWrites_sum] \n"   ; 
//		String AvgPhysicalWrites_max = !dummyRstm.hasColumnNoCase("AvgPhysicalWrites") ? "" : "    ,max([AvgPhysicalWrites]) as [AvgPhysicalWrites_max] \n"; 
//		String PagesWritten_sum      = !dummyRstm.hasColumnNoCase("PagesWritten"     ) ? "" : "    ,sum([PagesWritten])      as [PagesWritten_sum] \n"     ; 
//		String AvgPagesWritten_max   = !dummyRstm.hasColumnNoCase("AvgPagesWritten"  ) ? "" : "    ,max([AvgPagesWritten])   as [AvgPagesWritten_max] \n"  ; 

		String whereFilter           = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "1 = 1 \n" : "([CPUTime] > 0.0 OR [ExecutionTime] > 0.0 OR [LogicalReads] > 0.0) \n";
		String orderBy               = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "order by [RequestCntDiff__sum] desc \n" : "order by [CPUTime__sum] desc \n"; 
		String orderBy_colName       = !dummyRstm.hasColumnNoCase("CPUTime"          ) ? "RequestCntDiff__sum" : "CPUTime__sum"; 

//		String whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : "  and [CmNewDiffRateRow] = 0 \n";
		String whereFilter_skipNewDiffRateRows = !skipNewDiffRateRows ? "" : sql_and_skipNewOrDiffRateRows;

		String ObjectName = "    ,[ObjectName] \n";
		String groupBy    = "group by [DBName], [ObjectName] \n";
		if (dummyRstm.hasColumnNoCase("Hashkey2"))
		{
			ObjectName = "    ,max([ObjectName])        as [ObjectName] \n";
			groupBy    = "group by [DBName], [Hashkey2] \n"; 
		}

		//----------------------
//		FIX // "max" (ExecutionCount, RequestCntDiff)
		//----------------------
		// case when [ExecutionCount] > [RequestCntDiff] then [ExecutionCount] else [RequestCntDiff] end
		// - move a bunch of columns "to the end", so we have it in the same "order" as "top [SQL Capture] SLOW ..."

//		String sql = getCmDiffColumnsAsSqlComment("CmCachedProcs")
//			    + "select top " + topRows + " \n"
//			    + "     [DBName] \n"
//			    + ObjectName
//			    + "    ,cast('' as varchar(10))  as [txt] \n"
//			    + "    ,cast('' as varchar(255)) as [Remark] \n"
//			    + " \n"
//			    + "    ,max([MemUsageKB])        as [MemUsageKB_max] \n"
//
//			    + "    ,cast('' as varchar(512)) as [RequestCntDiff__chart] \n"
//			    + "    ,sum([RequestCntDiff])    as [RequestCntDiff_sum] \n"
//			    + "    ,sum([TempdbRemapCnt])    as [TempdbRemapCnt_sum] \n"
//
//			    + ( StringUtil.hasValue(ExecutionCount_sum) ? "   ,cast('' as varchar(512))     as [ExecutionCount__chart] \n" : "")
//			    + ExecutionCount_sum
//			    + " \n"
//
//			    + ( StringUtil.hasValue(CPUTime_sum) ? "   ,cast('' as varchar(512))     as [CpuTime__chart] \n" : "")
//			    + CPUTime_sum
//			    + AvgCPUTime_max
//			    + ExecutionTime_sum
//			    + AvgExecutionTime_max
//
//			    + ( StringUtil.hasValue(PhysicalReads_sum) ? "   ,cast('' as varchar(512))     as [PhysicalReads__chart] \n" : "")
//			    + PhysicalReads_sum
//			    + AvgPhysicalReads_max
//
//			    + ( StringUtil.hasValue(LogicalReads_sum) ? "   ,cast('' as varchar(512))     as [LogicalReads__chart] \n" : "")
//			    + LogicalReads_sum
//			    + AvgLogicalReads_max
//			    
//			    + ( StringUtil.hasValue(LogicalReadsMb_sum) ? "   ,cast('' as varchar(512))     as [LogicalReadsMb__chart] \n" : "")
//			    + LogicalReadsMb_sum
//			    + AvgLogicalReadsMb_max
//			    
//			    + PhysicalWrites_sum
//			    + AvgPhysicalWrites_max
//
//			    + PagesWritten_sum
//			    + AvgPagesWritten_max
//			    + " \n"
//			    + "    ,min([CompileDate])       as [CompileDate_min] \n"
//			    + "    ,max([CompileDate])       as [CompileDate_max] \n"
//			    + " \n"
//			    + "    ,count(distinct [PlanID]) as [PlanID_count] \n"
//			    + "    ,count(*)                 as [samples_count] \n"
//			    + "    ,min([SessionSampleTime]) as [SessionSampleTime_min] \n"
//			    + "    ,max([SessionSampleTime]) as [SessionSampleTime_max] \n"
//			    + "    ,cast('' as varchar(30))  as [Duration] \n"
//			    + "    ,sum([CmNewDiffRateRow])  as [newDiffRow_sum] \n"
//			    + "    ,sum([CmSampleMs])        as [CmSampleMs_sum] \n"
//			    + " \n"
//			    + "from [CmCachedProcs_diff] \n"
//			    + "where " + whereFilter + " \n"
//			    + whereFilter_skipNewDiffRateRows
//			    + "  and [ObjectName] NOT like '*ss%' -- If we do NOT want statement cache entries.  *ss = Statement Cache, *sq = Dynamic SQL \n"
//			    + "  and [ObjectName] NOT like '*sq%' -- If we do NOT want statement cache entries.  *ss = Statement Cache, *sq = Dynamic SQL \n"
//				+ getReportPeriodSqlWhere()
//			    + groupBy
//			    + " \n"
//			    + orderBy
//			    + "";

		String sql = getCmDiffColumnsAsSqlComment("CmCachedProcs")
			    + "select top " + topRows + " \n"
			    + "     [DBName] \n"
			    + ObjectName
			    + "    ,cast('' as varchar(10))  as [txt] \n"
			    + "    ,cast('' as varchar(255)) as [Remark] \n"
			    + " \n"
//			    + "    ,max([MemUsageKB])        as [MemUsageKB_max] \n"

//			    + "    ,cast('' as varchar(512)) as [RequestCntDiff__chart] \n"
//			    + "    ,sum([RequestCntDiff])    as [RequestCntDiff__sum] \n"
//			    + "    ,sum([TempdbRemapCnt])    as [TempdbRemapCnt__sum] \n"

			    + "\n"
			    + col_ExecutionCount__chart
			    + col_ExecutionCount__sum
			    
			    + "\n"
			    + col_ExecutionTime__chart
			    + col_ExecutionTime__sum
			    + col_ExecutionTime__avg

			    + "\n"
			    + col_CPUTime__chart
			    + col_CPUTime__sum
			    + col_CPUTime__avg

			    + "\n"
			    + col_LogicalReads__chart
			    + col_LogicalReads__sum
			    + col_LogicalReads__avg
			    
			    + "\n"
			    + col_LogicalReadsMb__chart
			    + col_LogicalReadsMb__sum
			    + col_LogicalReadsMb__avg
			    
			    + "\n"
			    + col_PhysicalReads__chart
			    + col_PhysicalReads__sum
			    + col_PhysicalReads__avg
			    
			    + "\n"
			    + col_PhysicalWrites__chart
			    + col_PhysicalWrites__sum
			    + col_PhysicalWrites__avg
			    
			    + "\n"
			    + col_MemUsageKB__chart
			    + col_MemUsageKB__sum
			    + col_MemUsageKB__avg
			    
			    + "\n"
			    + col_TempdbRemapCnt__chart
			    + col_TempdbRemapCnt__sum
			    + col_TempdbRemapCnt__avg
			    
			    + "\n"
			    + col_PagesWritten__chart
			    + col_PagesWritten__sum
			    + col_PagesWritten__avg
			    
			    + "\n"
			    + col_SnapExecutionCount__chart
			    + col_SnapExecutionCount__sum
			    + col_SnapExecutionCount__avg
			    
			    + "\n"
			    + col_SnapCodegenTime__chart
			    + col_SnapCodegenTime__sum
			    + col_SnapCodegenTime__avg
			    
			    + "\n"
			    + col_SnapJITTime__chart
			    + col_SnapJITTime__sum
			    + col_SnapJITTime__avg
			    
			    + "\n"
			    + col_SnapExecutionTime__chart
			    + col_SnapExecutionTime__sum
			    + col_SnapExecutionTime__avg
			    
			    + " \n"
			    + "    ,min([CompileDate])       as [CompileDate_min] \n"
			    + "    ,max([CompileDate])       as [CompileDate_max] \n"
			    + " \n"
			    + "    ,count(distinct [PlanID]) as [PlanID_count] \n"
			    + "    ,count(*)                 as [samples_count] \n"
			    + "    ,min([SessionSampleTime]) as [SessionSampleTime_min] \n"
			    + "    ,max([SessionSampleTime]) as [SessionSampleTime_max] \n"
			    + "    ,cast('' as varchar(30))  as [Duration] \n"
//			    + "    ,sum([CmNewDiffRateRow])  as [newDiffRow_sum] \n"
				+ col_newDiffRow_sum
			    + "    ,sum([CmSampleMs])        as [CmSampleMs_sum] \n"
			    + " \n"
			    + "from [CmCachedProcs_diff] \n"
			    + "where " + whereFilter + " \n"
			    + whereFilter_skipNewDiffRateRows
			    + "  and [ObjectName] NOT like '*ss%' -- If we do NOT want statement cache entries.  *ss = Statement Cache, *sq = Dynamic SQL \n"
			    + "  and [ObjectName] NOT like '*sq%' -- If we do NOT want statement cache entries.  *ss = Statement Cache, *sq = Dynamic SQL \n"
				+ getReportPeriodSqlWhere()
			    + groupBy
			    + " \n"
			    + orderBy
			    + "";
		
		_shortRstm = executeQuery(conn, sql, false, "TopCachedProcedureCalls");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("TopCachedProcedureCalls");
			return;
		}
		else
		{
			// Highlight sort column
			_shortRstm.setHighlightSortColumns(orderBy_colName);

			// Describe the table
			setSectionDescription(_shortRstm);

			// Calculate Duration
			setDurationColumn(_shortRstm, "SessionSampleTime_min", "SessionSampleTime_max", "Duration");

			// Check if rows looks strange (or validate) rows
			// For example if "CPUTime_sum" is above 24 hours (or the sample period), should we mark this entry as "suspect"...
			validateEntries();

			//-----------------------------------------------
			// Mini Chart on "..."
			//-----------------------------------------------
			String whereKeyColumn = "DBName, ObjectName"; 

//			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
//					SparkLineParams.create       (DataSource.CounterModel)
//					.setHtmlChartColumnName      ("RequestCntDiff__chart")
//					.setHtmlWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsTableName            ("CmCachedProcs_diff")
//					.setDbmsSampleTimeColumnName ("SessionSampleTime")
//					.setDbmsDataValueColumnName  ("RequestCntDiff")   
//					.setDbmsWhereKeyColumnName   (whereKeyColumn)
//					.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
//					.validate()));

			if (StringUtil.hasValue(col_ExecutionCount__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("ExecutionCount__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("ExecutionCount")
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_ExecutionTime__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("ExecutionTime__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([ExecutionTime]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_CPUTime__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("CPUTime__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([CPUTime]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1) // MS
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_LogicalReads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("LogicalReads__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([LogicalReads]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_LogicalReadsMb__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("LogicalReadsMb__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([LogicalReads])*1.0/"+asePageSizeDivider+" / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_PhysicalReads__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("PhysicalReads__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([PhysicalReads]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_PhysicalWrites__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("PhysicalWrites__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([PhysicalWrites]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_MemUsageKB__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("MemUsageKB__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([MemUsageKB]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_TempdbRemapCnt__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("TempdbRemapCnt__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([TempdbRemapCnt]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_PagesWritten__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("PagesWritten__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([PagesWritten]) / nullif(sum([ExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}


			// -------------- SNAP counters
			if (StringUtil.hasValue(col_SnapExecutionCount__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SnapExecutionCount__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("SnapExecutionCount")
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_SnapCodegenTime__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SnapCodegenTime__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([SnapCodegenTime]) / nullif(sum([SnapExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_SnapJITTime__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SnapJITTime__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([SnapJITTime]) / nullif(sum([SnapExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}

			if (StringUtil.hasValue(col_SnapExecutionTime__chart))
			{
				_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm, 
						SparkLineParams.create       (DataSource.CounterModel)
						.setHtmlChartColumnName      ("SnapExecutionTime__chart")
						.setHtmlWhereKeyColumnName   (whereKeyColumn)
						.setDbmsTableName            ("CmCachedProcs_diff")
						.setDbmsSampleTimeColumnName ("SessionSampleTime")
						.setDbmsDataValueColumnName  ("sum([SnapExecutionTime]) / nullif(sum([SnapExecutionCount]), 0)").setGroupDataAggregationType(AggType.USER_PROVIDED).setDecimalScale(1)
						.setDbmsWhereKeyColumnName   (whereKeyColumn)
						.setDbmsExtraWhereClause     (whereFilter_skipNewDiffRateRows)
						.validate()));
			}


			
			//--------------------------------------------------------------------------------------
			// get Executed SQL Statements from the SQL Capture ...
			//--------------------------------------------------------------------------------------
			if (_shortRstm.getRowCount() > 0)
			{
				for (int r=0; r<_shortRstm.getRowCount(); r++)
				{
					// Get Actual Executed SQL Text for current 'DBName, ProcName'
					Map<String, Object> whereColValMap = new LinkedHashMap<>();
					whereColValMap.put("DBName"    , _shortRstm.getValueAsString (r, "DBName"));
					whereColValMap.put("ProcName"  , _shortRstm.getValueAsString (r, "ObjectName"));  // 'ProcName' in [MonSqlCapStatements] but 'ObjectName' in RSTM

					_keyToExecutedSql = getSqlCapExecutedSqlText(_keyToExecutedSql, conn, true, whereColValMap);
				}
			}

			// Get SQL-Text for StatementCache entries
			Set<String> stmntCacheObjects = getStatementCacheObjects(_shortRstm, "ObjectName");
			if (stmntCacheObjects != null && ! stmntCacheObjects.isEmpty() )
			{
				try 
				{
					_ssqlRstm = getSqlStatementsFromMonDdlStorage(conn, stmntCacheObjects);
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
				}
			}
			
			
			// Details (sparlines)
			if (true)
			{
				SimpleResultSet srs = new SimpleResultSet();

				srs.addColumn("DBName"     , Types.VARCHAR,       60, 0);
				srs.addColumn("ObjectName" , Types.VARCHAR,       60, 0);
				srs.addColumn("sparklines" , Types.VARCHAR,      512, 0); 

				// Position in the "source" _shortRstm table (values we will fetch)
				int pos_dbname     = _shortRstm.findColumn("DBName");
				int pos_ObjectName = _shortRstm.findColumn("ObjectName");

				ColumnCopyRender msToHMS    = HtmlTableProducer.MS_TO_HMS;
				ColumnCopyRender oneDecimal = HtmlTableProducer.ONE_DECIMAL;
				
				HtmlTableProducer htp = new HtmlTableProducer(_shortRstm, "dsr-sub-table-chart");
				htp.setTableHeaders("Charts at 10 minute interval", "Total;style='text-align:right!important'", "Avg per exec;style='text-align:right!important'", "");
//				                                                        htp.add("req-cnt"    , new ColumnCopyRow().add( new ColumnCopyDef("RequestCntDiff__chart"    ) ).add(new ColumnCopyDef("RequestCntDiff__sum"           ).setColBold()).addEmptyCol()                                                 .addEmptyCol() );
				if (StringUtil.hasValue(col_ExecutionCount__chart    )) htp.add("exec-cnt"   , new ColumnCopyRow().add( new ColumnCopyDef("ExecutionCount__chart"    ) ).add(new ColumnCopyDef("ExecutionCount__sum"           ).setColBold()).addEmptyCol()                                                 .addEmptyCol() );
				if (StringUtil.hasValue(col_ExecutionTime__chart     )) htp.add("exec-time"  , new ColumnCopyRow().add( new ColumnCopyDef("ExecutionTime__chart"     ) ).add(new ColumnCopyDef("ExecutionTime__sum"    ,msToHMS) ).add(new ColumnCopyDef("ExecutionTime__avg"     , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_CPUTime__chart           )) htp.add("cpu-time"   , new ColumnCopyRow().add( new ColumnCopyDef("CPUTime__chart"           ) ).add(new ColumnCopyDef("CPUTime__sum"          ,msToHMS) ).add(new ColumnCopyDef("CPUTime__avg"           , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_LogicalReads__chart      )) htp.add("l-read"     , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReads__chart"      ) ).add(new ColumnCopyDef("LogicalReads__sum"             ) ).add(new ColumnCopyDef("LogicalReads__avg"      , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_LogicalReadsMb__chart    )) htp.add("l-read-mb"  , new ColumnCopyRow().add( new ColumnCopyDef("LogicalReadsMb__chart"    ) ).add(new ColumnCopyDef("LogicalReadsMb__sum"           ) ).add(new ColumnCopyDef("LogicalReadsMb__avg"    , oneDecimal).setColBold()).add(new ColumnStatic("mb"  )) );
				if (StringUtil.hasValue(col_PhysicalReads__chart     )) htp.add("p-read"     , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalReads__chart"     ) ).add(new ColumnCopyDef("PhysicalReads__sum"            ) ).add(new ColumnCopyDef("PhysicalReads__avg"     , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_PhysicalWrites__chart    )) htp.add("p-write"    , new ColumnCopyRow().add( new ColumnCopyDef("PhysicalWrites__chart"    ) ).add(new ColumnCopyDef("PhysicalWrites__sum"           ) ).add(new ColumnCopyDef("PhysicalWrites__avg"    , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_MemUsageKB__chart        )) htp.add("mem-usage"  , new ColumnCopyRow().add( new ColumnCopyDef("MemUsageKB__chart"        ) ).add(new ColumnCopyDef("MemUsageKB__sum"               ) ).add(new ColumnCopyDef("MemUsageKB__avg"        , oneDecimal).setColBold()).add(new ColumnStatic("kb"  )) );
				if (StringUtil.hasValue(col_TempdbRemapCnt__chart    )) htp.add("tmp-remap"  , new ColumnCopyRow().add( new ColumnCopyDef("TempdbRemapCnt__chart"    ) ).add(new ColumnCopyDef("TempdbRemapCnt__sum"           ) ).add(new ColumnCopyDef("TempdbRemapCnt__avg"    , oneDecimal).setColBold()).add(new ColumnStatic("#"   )) );
				if (StringUtil.hasValue(col_PagesWritten__chart      )) htp.add("pgs-write"  , new ColumnCopyRow().add( new ColumnCopyDef("PagesWritten__chart"      ) ).add(new ColumnCopyDef("PagesWritten__sum"             ) ).add(new ColumnCopyDef("PagesWritten__avg"      , oneDecimal).setColBold()).add(new ColumnStatic("pgs" )) );
				if (StringUtil.hasValue(col_SnapExecutionCount__chart)) htp.add("snap-x-cnt" , new ColumnCopyRow().add( new ColumnCopyDef("SnapExecutionCount__chart") ).add(new ColumnCopyDef("SnapExecutionCount__sum"       ).setColBold()).addEmptyCol()                                                 .addEmptyCol() );
				if (StringUtil.hasValue(col_SnapCodegenTime__chart   )) htp.add("snap-cgt"   , new ColumnCopyRow().add( new ColumnCopyDef("SnapCodegenTime__chart"   ) ).add(new ColumnCopyDef("SnapCodegenTime__sum"  ,msToHMS) ).add(new ColumnCopyDef("SnapCodegenTime__avg"   , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_SnapJITTime__chart       )) htp.add("snap-jitime", new ColumnCopyRow().add( new ColumnCopyDef("SnapJITTime__chart"       ) ).add(new ColumnCopyDef("SnapJITTime__sum"      ,msToHMS) ).add(new ColumnCopyDef("SnapJITTime__avg"       , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				if (StringUtil.hasValue(col_SnapExecutionTime__chart )) htp.add("snap-x-time", new ColumnCopyRow().add( new ColumnCopyDef("SnapExecutionTime__chart" ) ).add(new ColumnCopyDef("SnapExecutionTime__sum",msToHMS) ).add(new ColumnCopyDef("SnapExecutionTime__avg" , oneDecimal).setColBold()).add(new ColumnStatic("ms"  )) );
				htp.validate();
				
				// Filter out some rows...
				htp.setRowFilter(new HtmlTableProducer.RowFilter()
				{
					@Override
					public boolean include(ResultSetTableModel rstm, int rstmRow, String rowKey)
					{
						if (StringUtil.equalsAny(rowKey, "p-write"))
						{
							return rstm.hasColumn("PhysicalWrites__sum") && rstm.getValueAsInteger(rstmRow, "PhysicalWrites__sum") > 0;
						}
						
						if (StringUtil.equalsAny(rowKey, "tmp-remap"))
						{
							return rstm.hasColumn("TempdbRemapCnt__sum") && rstm.getValueAsInteger(rstmRow, "TempdbRemapCnt__sum") > 0;
						}
						
						if (StringUtil.equalsAny(rowKey, "pgs-write"))
						{
							return rstm.hasColumn("PagesWritten__sum") && rstm.getValueAsInteger(rstmRow, "PagesWritten__sum") > 0;
						}
						
						if (StringUtil.equalsAny(rowKey, "snap-x-cnt", "snap-cgt", "snap-jitime", "snap-x-time"))
						{
							return rstm.hasColumn("SnapExecutionCount__sum") && rstm.getValueAsInteger(rstmRow, "SnapExecutionCount__sum") > 0;
						}

						return true;
					}
				});

				if (pos_dbname >= 0 && pos_ObjectName >= 0)
				{
					for (int r=0; r<_shortRstm.getRowCount(); r++)
					{
						String     dbname   = _shortRstm.getValueAsString    (r, pos_dbname);
						String     procName = _shortRstm.getValueAsString    (r, pos_ObjectName);
						
						String sparklines = htp.getHtmlTextForRow(r);

						// add record to SimpleResultSet
						srs.addRow(dbname, procName, sparklines);
					}
				}

				try
				{
					// Note the 'srs' is populated when reading above ResultSet from query
					_sparklinesRstm = createResultSetTableModel(srs, "Top Procedures", null, false); // DO NOT TRUNCATE COLUMNS
					srs.close();
				}
				catch (SQLException ex)
				{
					setProblemException(ex);
		
					_sparklinesRstm = ResultSetTableModel.createEmpty("Top Procedures");
					_logger.warn("Problems getting Top Procedures: " + ex);
				}
			} // end: sparkline
		} // end: has data
	} // end: method

	/**
	 * Validate entries... 
	 * for example if some values looks TO BIG (for example "CPUTime_sum" is above 24 hours (or the sample period), then set a "Remark"
	 *             NOTE: If we have *many* engines, then 24H could be OK... or it might be a ASE-MDA counter issue (or some other strange issue)
	 */
	private void validateEntries()
	{
		if (_shortRstm == null)
			return;

		ResultSetTableModel rstm = _shortRstm;

		// Threshold value to be crossed: then make: WARNING
		Long th_CPUTime_sum = 3_600 * 1000 * 24L;  // 24 hours in milliseconds
		
		int pos_CPUTime_sum = rstm.findColumn("CPUTime__sum");
		int pos_remark      = rstm.findColumn("Remark");

		if (pos_CPUTime_sum >= 0 && pos_remark >= 0)
		{
			for (int r=0; r<rstm.getRowCount(); r++)
			{
				Long CPUTime_sum = rstm.getValueAsLong(r, pos_CPUTime_sum);

				if (CPUTime_sum != null)
				{
					if (CPUTime_sum > th_CPUTime_sum)
					{
//						rstm.setValueAtWithOverride("WARNING: CPUTime_sum > 24h", r, pos_remark);
						rstm.setValueAtWithOverride("<font color='red'>WARNING: CPUTime_sum > 24h</font>", r, pos_remark);
					}
				}
			}
		}
	}
}
