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
package com.dbxtune.pcs.report.content.mysql;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.mysql.CmTableSize;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The biggest tables, and how they grew (from CmTableSize). Built on OracleSegmentSize.
 */
public class MySqlTableSize
extends MySqlAbstract
{
	private ResultSetTableModel _shortRstm;

	public MySqlTableSize(DailySummaryReportAbstract reportingInstance)
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

		if (_CmTableSize_SchemaSizeMb != null)
			_CmTableSize_SchemaSizeMb.writeHtmlContent(w, null, null);

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Biggest Tables, and how they grow (order by: FileSizeMb, origin: CmTableSize / INNODB_TABLES, INNODB_TABLESPACES, INNODB_TABLESTATS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmTableSize_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		String schema = getReportingInstance().getDbmsSchemaName();

		if (doTableExist(conn, schema, CmTableSize.CM_NAME + "_" + CmTableSize.GRAPH_NAME_SCHEMA_SIZE_MB))
			_CmTableSize_SchemaSizeMb = createTsLineChart(conn, schema, CmTableSize.CM_NAME, CmTableSize.GRAPH_NAME_SCHEMA_SIZE_MB, -1, true, null, "Size per Schema, in MB (" + CmTableSize.SHORT_NAME + ")");

		// The tables at the LAST sample, and how much the size changed since the FIRST sample
		// Listed: the top ## on size, and the top ## on growth
		// The size: FileSizeMb (file per table), otherwise data + index from the statistics
		String size = "coalesce(l.[FileSizeMb], l.[DataMb] + l.[IndexMb])";
		String sql = ""
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         l.[SchemaName] \n"
			    + "        ,l.[TableName] \n"
			    + "        ,row_number() over (order by " + size + " desc)                                       as [rank_size] \n"
			    + "        ,case when l.[FileSizeMb] > f.[FileSizeMb] \n"   // only the tables that did grow has a place in that list
			    + "              then row_number() over (order by l.[FileSizeMb] - coalesce(f.[FileSizeMb], l.[FileSizeMb]) desc) \n"
			    + "         end                                                                                  as [rank_growth] \n"
			    + "        ,l.[FileSizeMb] \n"
			    + "        ,l.[FileSizeMb] - f.[FileSizeMb]                                                      as [FileSizeMb_change] \n"
			    + "        ,l.[RowsEstimate] \n"
			    + "        ,l.[RowsEstimate] - f.[RowsEstimate]                                                  as [RowsEstimate_change] \n"
			    + "        ,l.[DataMb] \n"
			    + "        ,l.[IndexMb] \n"
			    + "        ,l.[UnusedMb] \n"
			    + "        ,l.[SpaceType] \n"
			    + "        ,l.[RowFormat] \n"
			    + "        ,l.[SchemaTotalMb] \n"
			    + "    from [CmTableSize_abs] l \n"
			    + "    left outer join [CmTableSize_abs] f on  f.[SchemaName]        = l.[SchemaName] \n"
			    + "                                        and f.[TableName]         = l.[TableName] \n"
			    + "                                        and f.[SessionSampleTime] = (select min([SessionSampleTime]) from [CmTableSize_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "    where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmTableSize_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + ") x \n"
			    + "where [rank_size]   <= " + topRows + " \n"
			    + "   or [rank_growth] <= " + topRows + " \n"
			    + "order by [rank_size] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "MySqlTableSize");

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("FileSizeMb");

		_shortRstm.setDescription(
				"The biggest InnoDB tables at the last collector sample, and how much they grew during the period (from the table <code>CmTableSize_abs</code>, which is sampled once an hour).<br>" +
				"A table is listed if it is in the top " + topRows + " on <b>size</b> or on <b>growth</b> (the two <code>rank_</code> columns says where it's placed in each).<br>" +
				"<b>Note</b>: The collector only records the biggest tables (property <code>CmTableSize.sample.topRows</code>). The rows and the data/index sizes are InnoDB's statistics (an estimate), empty for a table that has not been used since the server started.<br>" +
				"The file size is the space the table's file has: rows that are deleted do not make the file smaller (see UnusedMb).<br>" +
				"");

		_shortRstm.setColumnDescription("SchemaName"         , "The schema (database)");
		_shortRstm.setColumnDescription("TableName"          , "The table. A partition is shown as 'table#p#partition'.");
		_shortRstm.setColumnDescription("rank_size"          , "Place in the list when it's sorted on size (1 = biggest)");
		_shortRstm.setColumnDescription("rank_growth"        , "Place in the list when it's sorted on growth (1 = grew most). Empty = the file did not grow during the period.");
		_shortRstm.setColumnDescription("FileSizeMb"         , "The size of the table's file at the last sample, in MB. Empty when the table is not in a file of its own.");
		_shortRstm.setColumnDescription("FileSizeMb_change"  , "How much FileSizeMb changed during the period: last sample - first sample. Empty = the table was not in the first sample (it is new, or it was not among the biggest then).");
		_shortRstm.setColumnDescription("RowsEstimate"       , "Number of rows at the last sample, an estimate");
		_shortRstm.setColumnDescription("RowsEstimate_change", "How much RowsEstimate changed during the period");
		_shortRstm.setColumnDescription("DataMb"             , "The data (the clustered index), in MB");
		_shortRstm.setColumnDescription("IndexMb"            , "The other indexes, in MB");
		_shortRstm.setColumnDescription("UnusedMb"           , "About how much of the file is not used (FileSizeMb - DataMb - IndexMb). Large: OPTIMIZE TABLE gives it back to the file system.");
		_shortRstm.setColumnDescription("SpaceType"          , "Single: the table has its own file. General: in a general tablespace or the system tablespace.");
		_shortRstm.setColumnDescription("RowFormat"          , "Dynamic (the default), Compact, Redundant or Compressed");
		_shortRstm.setColumnDescription("SchemaTotalMb"      , "All tables in the schema, in MB, at the last sample");
	}

	private IReportChart _CmTableSize_SchemaSizeMb; // null = not recorded
}
