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
import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IndexAnalysisHtml;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Indexes that were not used (no index scans) during the period, the biggest first (from CmPgIndexes, and CmPgTables for the writes to the table).
 * <p>
 * Unique / primary key / constraint indexes are counted apart: they stop duplicates also when no query uses them. Built on OracleUnusedIndexes.
 */
public class PostgresUnusedIndexes
extends PostgresAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm; // null = not recorded
	private ResultSetTableModel _sumRstm;
	private int       _minDays;
	private Timestamp _validSinceOldest; // the oldest database (null = not known)
	private Timestamp _newestSample;

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

		// How long the counters are valid: 'idx_scan_since_reset = 0' only means 'not used since valid_since'
		String minDaysProp = "<code>" + IndexAnalysisHtml.PROPKEY_unused_minDays + "=" + _minDays + "</code>";
		if (_validSinceOldest == null || _newestSample == null)
		{
			w.append("<span style='background-color: yellow'><b>Note</b>: The server start and the statistics reset time are not in the recording, so it is not known how long the counters cover.</span><br>\n");
		}
		else
		{
			double days = (_newestSample.getTime() - _validSinceOldest.getTime()) / (24.0 * 3600 * 1000);
			String daysStr = String.format(Locale.US, "%.1f", days);
			w.append("Counters valid since: <b>" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(_validSinceOldest) + "</b> (" + daysStr + " days, the oldest database): the statistics reset of the database, or the server start if that is later. ");
			w.append("Each database has its own: see <code>valid_since</code>.<br>\n");
			if (days < _minDays)
				w.append("<span style='background-color: yellow'><b>Note</b>: The counters only cover " + daysStr + " days: none of the indexes below can be trusted as unused (at least " + _minDays + " days are needed, property " + minDaysProp + ").</span><br>\n");
		}

		// How many, and how much space
		w.append(toHtmlTable(_sumRstm));
		w.append("<br>\n");

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String tagTrAttr(ResultSetTableModel rstm, int row)
			{
				// Gray: used earlier (since the counters started), or the counters are too young to trust 'unused'
				Object validDays = rstm.getValueAsObject(row, "valid_days");
				Object scans     = rstm.getValueAsObject(row, "idx_scan_since_reset");
				if ( ! (validDays instanceof Number) || ((Number)validDays).doubleValue() < _minDays || ! (scans instanceof Number) || ((Number)scans).longValue() != 0 )
					return "style='color:gray;'";
				return super.tagTrAttr(rstm, row);
			}
		}));
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
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		// H2 has no hash join: each joined group-by below is run once per index, without these it is a table scan each time (same indexes as PostgresTopSql)
		List<ReportingIndexEntry> list = new ArrayList<>();
		list.add(new ReportingIndexEntry("CmPgIndexes_diff", "dbname", "schemaname", "relname", "indexrelname"));
		list.add(new ReportingIndexEntry("CmPgTables_diff" , "dbname", "schemaname", "relname"));
		return list;
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
		// Only the last sample of _abs (not the whole period): H2 runs the joined group-by once per row of 'l'
		String lastSample = "(select max([SessionSampleTime]) from [CmPgIndexes_abs] " + where + ")";
		String fromWhere = ""
			    + "from [CmPgIndexes_abs] l \n"
			    + "join (select [dbname], [schemaname], [relname], [indexrelname], sum([idx_scan]) as [scans] \n"
			    + "      from [CmPgIndexes_diff] \n"
			    + "      " + where + " \n"
			    + "      group by [dbname], [schemaname], [relname], [indexrelname] \n"
			    + "     ) d on d.[dbname] = l.[dbname] and d.[schemaname] = l.[schemaname] and d.[relname] = l.[relname] and d.[indexrelname] = l.[indexrelname] \n"
			    + (hasTables ? ""
			    + "left join (select [dbname], [schemaname], [relname], sum([n_tup_ins] + [n_tup_upd] + [n_tup_del]) as [rows_written] \n"
			    + "      from [CmPgTables_diff] \n"
			    + "      " + where + " \n"
			    + "      group by [dbname], [schemaname], [relname] \n"
			    + "     ) t on t.[dbname] = l.[dbname] and t.[schemaname] = l.[schemaname] and t.[relname] = l.[relname] \n" : "")
			    + "where l.[SessionSampleTime] = " + lastSample + " \n"
			    + "  and d.[scans] = 0 \n"
			    + "";

		String constraintExpr = hasSize ? "case when l.[is_constraint] or l.[is_unique] or l.[is_primary] then 1 else 0 end" : "0";

		// When the counters started, per database: the statistics reset, or the server start if that is later (a crash also resets them). Same as the Index Analysis (pgCountersFrom)
		_minDays = Configuration.getCombinedConfiguration().getIntProperty(IndexAnalysisHtml.PROPKEY_unused_minDays, IndexAnalysisHtml.DEFAULT_unused_minDays);
		Timestamp srvStart = null;
		Map<String, Timestamp> statsReset = new LinkedHashMap<>();
		try (Statement stmnt = conn.createStatement())
		{
			try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString("select max([SessionSampleTime]) from [CmPgIndexes_abs] " + where)))
			{
				if (rs.next()) _newestSample = rs.getTimestamp(1);
			}
			if (getRecordedColumns(conn, "CmSummary_abs").contains("start_time"))
				try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString("select max([start_time]) from [CmSummary_abs]")))
				{
					if (rs.next()) srvStart = rs.getTimestamp(1);
				}
			if (getRecordedColumns(conn, "CmPgDatabase_abs").contains("stats_reset"))
				try (ResultSet rs = stmnt.executeQuery(conn.quotifySqlString("select [datname], max([stats_reset]) from [CmPgDatabase_abs] group by [datname]")))
				{
					while (rs.next())
						if (rs.getTimestamp(2) != null)
							statsReset.put(rs.getString(1), rs.getTimestamp(2));
				}
		}
		catch (SQLException ex)
		{
			_logger.info("Could not read the server start or the statistics reset time, continuing without them. Caught: " + ex);
		}

		// As a CASE on the database name (a few literals: no extra join for H2 to run per row)
		String validSinceExpr = "case l.[dbname] \n";
		_validSinceOldest = srvStart;
		for (Map.Entry<String, Timestamp> e : statsReset.entrySet())
		{
			Timestamp from = (srvStart == null || e.getValue().after(srvStart)) ? e.getValue() : srvStart;
			validSinceExpr += "        when '" + e.getKey().replace("'", "''") + "' then timestamp '" + from + "' \n";
			if (_validSinceOldest == null || from.before(_validSinceOldest))
				_validSinceOldest = from;
		}
		validSinceExpr += "        else " + (srvStart == null ? "cast(NULL as timestamp)" : "timestamp '" + srvStart + "'") + " end";
		if (statsReset.isEmpty())
			validSinceExpr = srvStart == null ? "cast(NULL as timestamp)" : "timestamp '" + srvStart + "'";

		String validDaysExpr = "cast(datediff(minute, " + validSinceExpr + ", l.[SessionSampleTime]) / 1440.0 as numeric(8,1))";
		String trustedExpr   = "l.[idx_scan] = 0 and " + validDaysExpr + " >= " + _minDays; // never used since the counters started, and they cover at least minDays

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
			    + "    ," + validSinceExpr + " as [valid_since] \n"
			    + "    ," + validDaysExpr  + " as [valid_days] \n"
			    + (hasLastScan ? "    ,l.[last_idx_scan] \n" : "")
			    + "    ,cast('' as varchar(4000)) as [DropStatement] \n"
			    + "    ,cast('' as varchar(4000)) as [CreateStatement] \n" // the ROLLBACK of the drop
			    + fromWhere
			    + "order by case when " + trustedExpr + " then 0 else 1 end, " + (hasSize ? constraintExpr + ", l.[size_kb] desc, " : "") + (hasTables ? "t.[rows_written] desc, " : "") + "l.[dbname], l.[relname], l.[indexrelname] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "PostgresUnusedIndexes");
		if (hasSize)
			_shortRstm.setHighlightSortColumns("size_mb");

		// DropStatement and CreateStatement (the ROLLBACK of the drop): the CREATE INDEX is pg_get_indexdef(), stored by the collector in the DDL Storage
		int pos_DropStatement   = _shortRstm.findColumn("DropStatement");
		int pos_CreateStatement = _shortRstm.findColumn("CreateStatement");
		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String dbname    = _shortRstm.getValueAsString(r, "dbname");
			String schName   = _shortRstm.getValueAsString(r, "schemaname");
			String table     = _shortRstm.getValueAsString(r, "relname");
			String index     = _shortRstm.getValueAsString(r, "indexrelname");
			String qSchema   = "\"" + schName.replace("\"", "\"\"") + "\"";
			String qTable    = qSchema + ".\"" + table.replace("\"", "\"\"") + "\"";
			String qIndex    = "\"" + index  .replace("\"", "\"\"") + "\"";
			boolean isConstr = hasSize && "true".equalsIgnoreCase(_shortRstm.getValueAsString(r, "is_constraint"));
			boolean isPk     = hasSize && "true".equalsIgnoreCase(_shortRstm.getValueAsString(r, "is_primary"));
			boolean isUnique = hasSize && "true".equalsIgnoreCase(_shortRstm.getValueAsString(r, "is_unique"));

			String indexDef = "";
			try
			{
				Set<PgTableInfo> tiSet = getTableInformationFromMonDdlStorage(conn, dbname, schName, table);
				PgIndexInfo ii = (tiSet == null || tiSet.isEmpty()) ? null : tiSet.iterator().next().getIndexInfoForName(index);
				if (ii != null)
					indexDef = ii.getDdlText();
			}
			catch (SQLException ignore) {}

			String dropStmnt;
			String createStmnt;
			if (isConstr)
			{
				// A primary key / unique / exclusion constraint: DROP INDEX fails, the constraint is dropped (it has the same name as its index)
				dropStmnt = "ALTER TABLE " + qTable + " DROP CONSTRAINT " + qIndex + ";";
				if (StringUtil.isNullOrBlank(indexDef))
					createStmnt = "-- Not in the DDL Storage: save it first with SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = '" + index.replace("'", "''") + "';";
				else if (isPk || isUnique)
					createStmnt = indexDef + "; ALTER TABLE " + qTable + " ADD CONSTRAINT " + qIndex + (isPk ? " PRIMARY KEY" : " UNIQUE") + " USING INDEX " + qIndex + ";";
				else
					createStmnt = "-- An exclusion constraint can't be put back from the index: save it first with SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = '" + index.replace("'", "''") + "';";
			}
			else
			{
				// CONCURRENTLY: no lock on the table (can't be run in a transaction)
				dropStmnt = "DROP INDEX CONCURRENTLY " + qSchema + "." + qIndex + ";";
				if ( ! hasSize ) // an older recording: it is not known if a constraint uses the index
					dropStmnt += " -- if a constraint uses the index (primary key, unique): ALTER TABLE " + qTable + " DROP CONSTRAINT " + qIndex + ";";
				createStmnt = StringUtil.isNullOrBlank(indexDef)
						? "-- Not in the DDL Storage: save it first with SELECT pg_get_indexdef('" + (qSchema + "." + qIndex).replace("'", "''") + "'::regclass);"
						: indexDef.replaceFirst("^CREATE (UNIQUE )?INDEX ", "CREATE $1INDEX CONCURRENTLY ") + ";";
			}
			_shortRstm.setValueAtWithOverride(dropStmnt  , r, pos_DropStatement);
			_shortRstm.setValueAtWithOverride(createStmnt, r, pos_CreateStatement);
		}

		_shortRstm.setDescription(
				"Indexes that had <b>no index scans during the period</b> (from the tables <code>CmPgIndexes_diff</code> and <code>_abs</code>). " +
				"First the ones that can be trusted (no scans since the counters started, at least " + _minDays + " days ago), then the not unique ones, the biggest first. " +
				"The others are gray: used earlier (<code>idx_scan_since_reset</code> &gt; 0) or the counters are too young.<br>" +
				"An index that is not used still costs: every INSERT and UPDATE (that changes an indexed column) has to maintain it, it takes space, and it makes vacuum slower. " +
				"<code>rows_written</code> is what the table got during the period: the index was maintained for all of it, for nothing.<br>" +
				"<b>Before you drop one</b>:" +
				"<ul>" +
				"  <li>The period is short compared with a month or a year: a job that only runs at month end may need it. " +
				"      <code>idx_scan_since_reset</code> = 0 and an old <code>valid_since</code> is a stronger sign" + (hasLastScan ? "; <code>last_idx_scan</code> tells when it was last used" : "") + ".</li>" +
				"  <li>Reads on a <b>standby</b> are not counted here (each server has its own statistics): check the standbys too.</li>" +
				"  <li>A <b>unique</b> index, a primary key or a constraint index stops duplicates also when no query uses it: drop the constraint (if it is not needed), not the index.</li>" +
				"  <li>An index on a foreign key column can be needed even if no query uses it: without it, a DELETE (or key UPDATE) in the parent table scans the child table.</li>" +
				"  <li>Save the <code>CreateStatement</code> first (the rollback, from the DDL Storage), then run the <code>DropStatement</code>. " +
				"      Both must be run connected to the database in <code>dbname</code>. CONCURRENTLY takes no lock on the table, but can't be run in a transaction.</li>" +
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
		_shortRstm.setColumnDescription("valid_since"         , "When the counters of this database started: the statistics reset (pg_stat_database.stats_reset), or the server start if that is later (a crash also resets them)");
		_shortRstm.setColumnDescription("valid_days"          , "Days the counters cover (valid_since to the last sample). Under " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): too short to trust, shown in gray");
		_shortRstm.setColumnDescription("DropStatement"       , "DROP INDEX CONCURRENTLY, or ALTER TABLE ... DROP CONSTRAINT for an index that backs a constraint. Save the CreateStatement first: it is the only way back if the index was needed");
		_shortRstm.setColumnDescription("CreateStatement"     , "The CREATE INDEX that gives the index back (the rollback of the DROP): pg_get_indexdef() from the DDL Storage, and ALTER TABLE ... ADD CONSTRAINT ... USING INDEX for a primary key or unique constraint");
		_shortRstm.setColumnDescription("last_idx_scan"       ,"When the index was last used (Postgres 16 and above; empty = not since the statistics reset)");

		// How many are there, and how much space do they take (all of them, not just the top ##)
		sql = ""
			    + "select \n"
			    + "     count(*) as [unused_indexes] \n"
			    + (hasSize ? ""
			    + "    ,cast(sum(l.[size_kb]) / 1024.0 as numeric(15,1)) as [unused_size_mb] \n"
			    + "    ,sum(" + constraintExpr + ") as [of_which_unique_or_constraint] \n" : "")
			    + "    ,sum(case when l.[idx_scan] = 0 then 1 else 0 end) as [never_used_since_reset] \n"
			    + "    ,sum(case when " + trustedExpr + " then 1 else 0 end) as [trusted_unused] \n"
			    + "    ,(select count(*) from [CmPgIndexes_abs] x where x.[SessionSampleTime] = " + lastSample + ") as [recorded_indexes] \n"
			    + fromWhere
			    + "";

		_sumRstm = executeQuery(conn, sql, true, "PostgresUnusedIndexesSum");
		_sumRstm.setColumnDescription("unused_indexes"               , "Number of indexes without an index scan during the period");
		_sumRstm.setColumnDescription("unused_size_mb"               , "The space they take, in MB");
		_sumRstm.setColumnDescription("of_which_unique_or_constraint", "How many of them are unique, primary keys or back a constraint (they are there to stop duplicates)");
		_sumRstm.setColumnDescription("never_used_since_reset"       , "How many of them were never used since the statistics were reset");
		_sumRstm.setColumnDescription("recorded_indexes"             , "Number of indexes the collector recorded (at the last sample)");
		_sumRstm.setColumnDescription("trusted_unused"               , "How many of them were never used since the counters started, at least " + _minDays + " days ago (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): the ones you can trust");
	}
}
