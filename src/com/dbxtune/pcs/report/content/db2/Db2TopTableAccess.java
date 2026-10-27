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
package com.dbxtune.pcs.report.content.db2;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The tables that were read and changed most, and the ones that were scanned (from CmMonGetTable / MON_GET_TABLE). Built on MySqlTopTableAccess.
 */
public class Db2TopTableAccess
extends Db2Abstract
{
	private ResultSetTableModel _shortRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();

	public Db2TopTableAccess(DailySummaryReportAbstract reportingInstance)
	{
		super(reportingInstance);
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

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_shortRstm.getRowCount() == 0)
		{
			w.append("No rows found <br>\n");
			return;
		}

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));

		if (isFullMessageType())
		{
			// Write JavaScript code for the mini charts
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Top Table Access: the tables that were read and changed most (order by: ROWS_READ, origin: CmMonGetTable / MON_GET_TABLE)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmMonGetTable_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		// A table that is seen for the first time has it's counters "since the database was activated": do not count those rows
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// A partitioned table (and pureScale) has one row per data partition and member: summed
		String sql = getCmDiffColumnsAsSqlComment("CmMonGetTable")
			    + "select top " + topRows + " \n"
			    + "     [TABSCHEMA] \n"
			    + "    ,[TABNAME] \n"
			    + "    ,max([TAB_TYPE])                                                                  as [TAB_TYPE] \n"
			    + "    ,cast('' as varchar(512))                                                         as [ROWS_READ__chart] \n"
			    + "    ,sum([ROWS_READ])                                                                 as [ROWS_READ] \n"
			    + "    ,cast('' as varchar(512))                                                         as [TABLE_SCANS__chart] \n"
			    + "    ,sum([TABLE_SCANS])                                                               as [TABLE_SCANS] \n"
			    + "    ,CAST( sum([ROWS_READ]) * 1.0 / nullif(sum([TABLE_SCANS]), 0)  as DECIMAL(20,0) ) as [ROWS_READ_PER_SCAN] \n"
			    + "    ,sum([ROWS_INSERTED])                                                             as [ROWS_INSERTED] \n"
			    + "    ,sum([ROWS_UPDATED])                                                              as [ROWS_UPDATED] \n"
			    + "    ,sum([ROWS_DELETED])                                                              as [ROWS_DELETED] \n"
			    + "    ,sum([OVERFLOW_ACCESSES])                                                         as [OVERFLOW_ACCESSES] \n"
			    + "    ,sum([LOCK_WAITS])                                                                as [LOCK_WAITS] \n"
			    + "    ,sum([LOCK_WAIT_TIME])                                                            as [LOCK_WAIT_TIME] \n"
			    + "    ,sum([LOCK_ESCALS])                                                               as [LOCK_ESCALS] \n"
			    + "    ,sum([OBJECT_DATA_L_READS])                                                       as [OBJECT_DATA_L_READS] \n"
			    + "    ,sum([OBJECT_DATA_P_READS])                                                       as [OBJECT_DATA_P_READS] \n"
			    + "    ,count(distinct [SessionSampleTime])                                              as [samples_count] \n"
			    + "from [CmMonGetTable_diff] \n"
			    + "where ([ROWS_READ] > 0 or [ROWS_INSERTED] > 0 or [ROWS_UPDATED] > 0 or [ROWS_DELETED] > 0) \n"
			    + sql_and_skipNewDiffRateRows
			    + getReportPeriodSqlWhere()
			    + "group by [TABSCHEMA], [TABNAME] \n"
			    + "order by sum([ROWS_READ]) desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "Db2TopTableAccess");

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("ROWS_READ");

		_shortRstm.setDescription(
				"The tables that were read most during the period (from the table <code>CmMonGetTable_diff</code>).<br>" +
				"A table with many <code>TABLE_SCANS</code> and a high <code>ROWS_READ_PER_SCAN</code> is read from start to end, often because an index is missing (see the Top SQL section, column ROWS_READ__per_exec).<br>" +
				"<b>Note</b>: The collector only records the most active tables (property <code>CmMonGetTable.sample.topRows</code>), and the first sample where a table is seen is not counted.<br>" +
				"");

		_shortRstm.setColumnDescription("TABSCHEMA"          , "The schema");
		_shortRstm.setColumnDescription("TABNAME"            , "The table");
		_shortRstm.setColumnDescription("TAB_TYPE"           , "USER_TABLE, CATALOG_TABLE, TEMP_TABLE...");
		_shortRstm.setColumnDescription("ROWS_READ__chart"   , "Chart: rows read from the table, in 10 minute chunks\nSo you can see WHEN the table was used.");
		_shortRstm.setColumnDescription("ROWS_READ"          , "Rows read from the table (also the rows that were read, but did not match the WHERE clause)");
		_shortRstm.setColumnDescription("TABLE_SCANS__chart" , "Chart: scans of the whole table, in 10 minute chunks");
		_shortRstm.setColumnDescription("TABLE_SCANS"        , "Scans of the whole table");
		_shortRstm.setColumnDescription("ROWS_READ_PER_SCAN" , "Rows read per table scan: about the size of the table when most reads are scans");
		_shortRstm.setColumnDescription("ROWS_INSERTED"      , "Rows inserted");
		_shortRstm.setColumnDescription("ROWS_UPDATED"       , "Rows updated");
		_shortRstm.setColumnDescription("ROWS_DELETED"       , "Rows deleted");
		_shortRstm.setColumnDescription("OVERFLOW_ACCESSES"  , "Reads of rows that were moved to another page after an UPDATE (every one is an extra read: a REORG fixes it)");
		_shortRstm.setColumnDescription("LOCK_WAITS"         , "Times a connection waited for a lock on the table");
		_shortRstm.setColumnDescription("LOCK_WAIT_TIME"     , "Time waited for locks on the table, in milliseconds");
		_shortRstm.setColumnDescription("LOCK_ESCALS"        , "Lock escalations to a table lock");
		_shortRstm.setColumnDescription("OBJECT_DATA_L_READS", "Data pages read from the buffer pool");
		_shortRstm.setColumnDescription("OBJECT_DATA_P_READS", "Data pages that had to be read from disk");
		_shortRstm.setColumnDescription("samples_count"      , "In how many collector samples the table was used");

		//--------------------------------------------------------------------------------
		// Mini Charts: WHEN during the period
		//--------------------------------------------------------------------------------
		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("ROWS_READ__chart")
				.setHtmlWhereKeyColumnName   ("TABSCHEMA, TABNAME")
				.setDbmsTableName            ("CmMonGetTable_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("ROWS_READ")
				.setDbmsWhereKeyColumnName   ("TABSCHEMA, TABNAME")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Rows read from the table in below period")
				.validate()));

		_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
				SparkLineParams.create       (DataSource.CounterModel)
				.setHtmlChartColumnName      ("TABLE_SCANS__chart")
				.setHtmlWhereKeyColumnName   ("TABSCHEMA, TABNAME")
				.setDbmsTableName            ("CmMonGetTable_diff")
				.setDbmsSampleTimeColumnName ("SessionSampleTime")
				.setDbmsDataValueColumnName  ("TABLE_SCANS")
				.setDbmsWhereKeyColumnName   ("TABSCHEMA, TABNAME")
				.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
				.setSparklineTooltipPostfix  ("Table scans in below period")
				.validate()));
	}
}
