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

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * The biggest tables, how much they grew during the day, and the tables that can not be used (from CmTableSize). Built on MySqlTableSize / OracleSegmentSize.
 */
public class Db2TableSize
extends Db2Abstract
{
	private ResultSetTableModel _shortRstm; // null = not recorded

	public Db2TableSize(DailySummaryReportAbstract reportingInstance)
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
		return false;
	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		if (_shortRstm == null)
		{
			w.append("<b>The table sizes were not recorded</b> (the table <code>CmTableSize_abs</code> does not exist: the Counter Model <code>CmTableSize</code> is disabled).<br>\n");
			return;
		}

		w.append(getSectionDescriptionHtml(_shortRstm, true));
		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Table Size: the biggest tables, and tables that can not be used (origin: CmTableSize / ADMIN_GET_TAB_INFO)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarm TableNotUsable does that)
	}

	@Override
	public int getTopRowsDefault()
	{
		return 30;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		// No mandatory table: a disabled CM is a text, not a red ERROR
		if ( ! doTableExist(conn, getReportingInstance().getDbmsSchemaName(), "CmTableSize_abs") )
			return;

		// The last sample, and the growth since the first sample in the period (the CM samples once an hour)
		String where = "where 1 = 1 " + getReportPeriodSqlWhere().trim();
		String sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     l.[TABSCHEMA] \n"
			    + "    ,l.[TABNAME] \n"
			    + "    ,l.[PROBLEM] \n"
			    + "    ,l.[TOTAL_MB] \n"
			    + "    ,CAST(l.[TOTAL_MB] - f.[TOTAL_MB] as DECIMAL(15,1))     as [GROWTH_MB] \n"
			    + "    ,l.[DATA_MB] \n"
			    + "    ,l.[INDEX_MB] \n"
			    + "    ,l.[LOB_MB] \n"
			    + "    ,l.[RECLAIMABLE_MB] \n"
			    + "    ,l.[ROW_COUNT_STATS] \n"
			    + "    ,l.[STATS_TIME] \n"
			    + "    ,l.[STATS_ROWS_MODIFIED] \n"
			    + "    ,l.[LASTUSED] \n"
			    + "    ,l.[TBSPACE] \n"
			    + "    ,l.[COMPRESSION] \n"
			    + "from [CmTableSize_abs] l \n"
			    + "left join [CmTableSize_abs] f on f.[TABSCHEMA] = l.[TABSCHEMA] and f.[TABNAME] = l.[TABNAME] \n"
			    + "                             and f.[SessionSampleTime] = (select min([SessionSampleTime]) from [CmTableSize_abs] " + where + ") \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmTableSize_abs] " + where + ") \n"
			    + "order by case when COALESCE(l.[PROBLEM], '') <> '' then 0 else 1 end, l.[TOTAL_MB] desc \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "Db2TableSize");

		_shortRstm.setHighlightSortColumns("TOTAL_MB");

		_shortRstm.setDescription(
				"The biggest tables at the last collector sample (from the table <code>CmTableSize_abs</code>), and the tables that can not be used first (<code>PROBLEM</code>: REORG PENDING, SET INTEGRITY PENDING, LOAD PENDING, NOT AVAILABLE).<br>" +
				"<code>GROWTH_MB</code> is how much it grew during the period. <code>RECLAIMABLE_MB</code> can be given back to the table space with REORG ... RECLAIM EXTENTS.<br>" +
				"");

		_shortRstm.setColumnDescription("TABSCHEMA"          , "The schema");
		_shortRstm.setColumnDescription("TABNAME"            , "The table");
		_shortRstm.setColumnDescription("PROBLEM"            , "Empty if the table can be used, otherwise why it can not");
		_shortRstm.setColumnDescription("TOTAL_MB"           , "Data + indexes + LOB/XML + column-organized, in MB");
		_shortRstm.setColumnDescription("GROWTH_MB"          , "How much it grew during the period, in MB (empty = it was not in the top list at the first sample)");
		_shortRstm.setColumnDescription("DATA_MB"            , "The data, in MB");
		_shortRstm.setColumnDescription("INDEX_MB"           , "The indexes, in MB");
		_shortRstm.setColumnDescription("LOB_MB"             , "LOB, LONG and XML data, in MB");
		_shortRstm.setColumnDescription("RECLAIMABLE_MB"     , "Space that can be given back to the table space, in MB");
		_shortRstm.setColumnDescription("ROW_COUNT_STATS"    , "Rows according to the statistics (-1 = no statistics)");
		_shortRstm.setColumnDescription("STATS_TIME"         , "When the statistics were collected (empty = never)");
		_shortRstm.setColumnDescription("STATS_ROWS_MODIFIED", "Rows changed since the statistics were collected");
		_shortRstm.setColumnDescription("LASTUSED"           , "The date the table was last used");
		_shortRstm.setColumnDescription("TBSPACE"            , "The table space");
		_shortRstm.setColumnDescription("COMPRESSION"        , "N = none, R = row, V = value, B = both");
	}
}
