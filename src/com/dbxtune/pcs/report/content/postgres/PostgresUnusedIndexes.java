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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Indexes that were not used (no index scans) during the period, the biggest first (from CmPgIndexes, and CmPgTables for the writes to the table).
 * <p>
 * Unique / primary key / constraint indexes are counted apart: they stop duplicates also when no query uses them. Built on OracleUnusedIndexes.
 */
public class PostgresUnusedIndexes
extends PostgresAbstract
{
	private ResultSetTableModel _shortRstm; // null = not recorded
	private ResultSetTableModel _sumRstm;

	public PostgresUnusedIndexes(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm == null)
		{
			w.append("<b>Index usage was not recorded</b> (the table <code>CmPgIndexes_diff</code> does not exist).<br>\n");
			return;
		}

		if (_shortRstm.getRowCount() == 0)
		{
			w.append("<b>No unused indexes were found</b>: every recorded index was used (scanned) during the period.<br>\n");
			return;
		}

		w.append(getSectionDescriptionHtml(_shortRstm, true));

		// How many, and how much space
		w.append(toHtmlTable(_sumRstm));
		w.append("<br>\n");

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));
	}

	@Override
	public String getSubject()
	{
		return "Indexes that are Not Used (order by: size_mb, origin: CmPgIndexes / pg_stat_user_indexes)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public int getTopRowsDefault()
	{
		return 30;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		// No mandatory table: a text, not a red ERROR
		if ( ! doTableExist(conn, schema, "CmPgIndexes_diff") )
			return;

		Set<String> idxCols = getRecordedColumns(conn, "CmPgIndexes_abs");
		boolean hasSize     = idxCols.contains("size_kb");       // added 2026-11 (older recordings: no size, no uniqueness)
		boolean hasLastScan = idxCols.contains("last_idx_scan"); // Postgres 16 and above
		boolean hasTables   = getRecordedColumns(conn, "CmPgTables_diff").contains("n_tup_ins");

		// The unused indexes: no index scan in the whole period. Their values at the last sample.
		String fromWhere = ""
			    + "from [CmPgIndexes_abs] l \n"
			    + "join (select [dbname], [relid], [indexrelid], sum([idx_scan]) as [scans], max([SessionSampleTime]) as [last_ts] \n"
			    + "      from [CmPgIndexes_diff] \n"
			    + "      " + where + " \n"
			    + "      group by [dbname], [relid], [indexrelid] \n"
			    + "     ) d on d.[dbname] = l.[dbname] and d.[relid] = l.[relid] and d.[indexrelid] = l.[indexrelid] and d.[last_ts] = l.[SessionSampleTime] \n"
			    + (hasTables ? ""
			    + "left join (select [dbname], [schemaname], [relname], sum([n_tup_ins] + [n_tup_upd] + [n_tup_del]) as [rows_written] \n"
			    + "      from [CmPgTables_diff] \n"
			    + "      " + where + " \n"
			    + "      group by [dbname], [schemaname], [relname] \n"
			    + "     ) t on t.[dbname] = l.[dbname] and t.[schemaname] = l.[schemaname] and t.[relname] = l.[relname] \n" : "")
			    + "where d.[scans] = 0 \n"
			    + "";

		String constraintExpr = hasSize ? "case when l.[is_constraint] or l.[is_unique] or l.[is_primary] then 1 else 0 end" : "0";

		String sql = ""
			    + "select top " + getTopRows() + " \n"
			    + "     l.[dbname] \n"
			    + "    ,l.[schemaname] \n"
			    + "    ,l.[relname] \n"
			    + "    ,l.[indexrelname] \n"
			    + (hasSize ? ""
			    + "    ,cast(l.[size_kb] / 1024.0 as numeric(12,1)) as [size_mb] \n"
			    + "    ,l.[is_unique] \n"
			    + "    ,l.[is_primary] \n"
			    + "    ,l.[is_constraint] \n" : "")
			    + (hasTables   ? "    ,t.[rows_written] \n" : "")
			    + "    ,l.[idx_scan] as [idx_scan_since_reset] \n"
			    + (hasLastScan ? "    ,l.[last_idx_scan] \n" : "")
			    + fromWhere
			    + "order by " + (hasSize ? constraintExpr + ", l.[size_kb] desc, " : "") + (hasTables ? "t.[rows_written] desc, " : "") + "l.[dbname], l.[relname], l.[indexrelname] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "PostgresUnusedIndexes");
		if (hasSize)
			_shortRstm.setHighlightSortColumns("size_mb");

		_shortRstm.setDescription(
				"Indexes that had <b>no index scans during the period</b> (from the tables <code>CmPgIndexes_diff</code> and <code>_abs</code>). The ones that are not unique first, the biggest first.<br>" +
				"An index that is not used still costs: every INSERT and UPDATE (that changes an indexed column) has to maintain it, it takes space, and it makes vacuum slower. " +
				"<code>rows_written</code> is what the table got during the period: the index was maintained for all of it, for nothing.<br>" +
				"<b>Before you drop one</b>:" +
				"<ul>" +
				"  <li>The period is short compared with a month or a year: a job that only runs at month end may need it. " +
				"      <code>idx_scan_since_reset</code> = 0 and an old statistics reset (below) is a stronger sign" + (hasLastScan ? "; <code>last_idx_scan</code> tells when it was last used" : "") + ".</li>" +
				"  <li>Reads on a <b>standby</b> are not counted here (each server has its own statistics): check the standbys too.</li>" +
				"  <li>A <b>unique</b> index, a primary key or a constraint index stops duplicates also when no query uses it: drop the constraint (if it is not needed), not the index.</li>" +
				"  <li>An index on a foreign key column can be needed even if no query uses it: without it, a DELETE (or key UPDATE) in the parent table scans the child table.</li>" +
				"  <li>Save the definition first: <code>SELECT pg_get_indexdef('schema.index_name'::regclass);</code> (that is the CREATE INDEX to roll back), " +
				"      then <code>DROP INDEX CONCURRENTLY schema.index_name;</code> (no lock on the table).</li>" +
				"</ul>" +
				"");
		_shortRstm.setColumnDescription("dbname"              , "The database");
		_shortRstm.setColumnDescription("schemaname"          , "The schema");
		_shortRstm.setColumnDescription("relname"             , "The table");
		_shortRstm.setColumnDescription("indexrelname"        , "The index");
		_shortRstm.setColumnDescription("size_mb"             , "Size of the index, in MB (at the last sample)");
		_shortRstm.setColumnDescription("is_unique"           , "A unique index: it stops duplicates, also when no query uses it");
		_shortRstm.setColumnDescription("is_primary"          , "The index of the primary key");
		_shortRstm.setColumnDescription("is_constraint"       , "The index backs a constraint (primary key, unique or exclusion): drop the constraint, not the index");
		_shortRstm.setColumnDescription("rows_written"        , "Rows inserted + updated + deleted in the table during the period (the index was maintained for them)");
		_shortRstm.setColumnDescription("idx_scan_since_reset", "Index scans since the statistics were reset (0 = never used since then)");
		_shortRstm.setColumnDescription("last_idx_scan"       , "When the index was last used (Postgres 16 and above; empty = not since the statistics reset)");

		// How many are there, and how much space do they take (all of them, not just the top ##)
		sql = ""
			    + "select \n"
			    + "     count(*) as [unused_indexes] \n"
			    + (hasSize ? ""
			    + "    ,cast(sum(l.[size_kb]) / 1024.0 as numeric(15,1)) as [unused_size_mb] \n"
			    + "    ,sum(" + constraintExpr + ") as [of_which_unique_or_constraint] \n" : "")
			    + "    ,sum(case when l.[idx_scan] = 0 then 1 else 0 end) as [never_used_since_reset] \n"
			    + "    ,(select count(distinct x.[indexrelid] || '.' || x.[dbname]) from [CmPgIndexes_abs] x " + where + ") as [recorded_indexes] \n"
			    + "    ,(select min([stats_reset]) from [CmPgDatabase_abs] " + where + ") as [statistics_since] \n"
			    + fromWhere
			    + "";

		_sumRstm = executeQuery(conn, sql, true, "PostgresUnusedIndexesSum");
		_sumRstm.setColumnDescription("unused_indexes"               , "Number of indexes without an index scan during the period");
		_sumRstm.setColumnDescription("unused_size_mb"               , "The space they take, in MB");
		_sumRstm.setColumnDescription("of_which_unique_or_constraint", "How many of them are unique, primary keys or back a constraint (they are there to stop duplicates)");
		_sumRstm.setColumnDescription("never_used_since_reset"       , "How many of them were never used since the statistics were reset");
		_sumRstm.setColumnDescription("recorded_indexes"             , "Number of indexes the collector recorded");
		_sumRstm.setColumnDescription("statistics_since"             , "The oldest statistics reset of the databases (pg_stat_database.stats_reset): 'since reset' counts from here. Empty = never reset (since the cluster was created, or a crash: a crash also resets them)");
	}
}
