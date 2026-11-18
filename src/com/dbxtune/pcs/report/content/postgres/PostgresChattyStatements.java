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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmPgStatements;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.NPlusOneParent;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * "Possible N+1 / chatty statements" from <code>CmPgStatements</code> (pg_stat_statements).
 * <p>
 * Lists the statements with the most calls over the day, and for each one tries to find the <b>parent</b>:
 * another statement whose rows per sample follow this statement's calls per sample.
 * See {@link NPlusOneParent#findParent(String, Map, List)}
 */
public class PostgresChattyStatements
extends PostgresAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_roundTripMs = "PostgresChattyStatements.roundTripMs";
	public static final double  DEFAULT_roundTripMs = 0.3;

	private ResultSetTableModel _shortRstm;
	private int _nPlusOneCount = 0;

	public PostgresChattyStatements(DailySummaryReportAbstract reportingInstance)
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
	public String getSubject()
	{
		return "Possible N+1 / Chatty Statements (origin: CmPgStatements / pg_stat_statements)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return _nPlusOneCount > 0;
	}

	@Override
	public int getTopRowsDefault()
	{
		return 20;
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { CmPgStatements.CM_NAME + "_diff" };
	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
		{
			sb.append("No rows found <br>\n");
			return;
		}

		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		if (_nPlusOneCount > 0)
			sb.append("<b>Found " + _nPlusOneCount + " statement(s) that look like N+1</b> (column <code>NPlusOne</code> = YES). <br>\n");
		else
			sb.append("No statement had a parent statement whose rows follow its calls (no N+1 pattern found). <br>\n");

		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");

		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				if ("query".equals(colName) || "parent_query".equals(colName))
				{
					String text = rstm.getValueAsString(row, colName);
					if (StringUtil.isNullOrBlank(text))
						return "";
					return getFormattedSqlAsTooltipDiv(text, "Hover or Click to see formatted SQL Text...", DbUtils.DB_PROD_NAME_POSTGRES);
				}
				if ("queryid".equals(colName) || "parent_queryid".equals(colName))
					return objVal == null ? "" : objVal.toString(); // an id: no thousand separators

				if ("NPlusOne".equals(colName) && "YES".equals(strVal))
					return "<b><font color='red'>YES</font></b>";

				return strVal;
			}
		}));

		if (_nPlusOneCount > 0)
		{
			sb.append("<br>\n");
			sb.append("<b>How to fix N+1:</b> instead of one lookup per row, fetch all rows in one statement: \n");
			sb.append("a JOIN with the parent, a <code>WHERE key = ANY(?)</code> / <code>IN (...)</code> list, or the ORM's eager/batch fetching ");
			sb.append("(for example JPA/Hibernate <code>JOIN FETCH</code> / <code>@BatchSize</code>, Django <code>select_related()</code> / <code>prefetch_related()</code>). <br>\n");
		}
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String tabName = CmPgStatements.CM_NAME + "_diff";

		// The query text can be Dictionary Compressed
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, tabName);
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table '" + tabName + "'.", ex);
		}
		String col_query = hasDictCompCols ? "query$dcc$" : "query";

		// A statement seen for the first time has its counters "since it came into pg_stat_statements" (not "since the previous sample"): skip those rows
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		int    topRows     = getTopRows();
		double roundTripMs = Configuration.getCombinedConfiguration().getDoubleProperty(PROPKEY_roundTripMs, DEFAULT_roundTripMs);

		// The same statement from several users: one row (the counters are summed)
		String sql = getCmDiffColumnsAsSqlComment(CmPgStatements.CM_NAME)
			+ "select top " + topRows + " \n"
			+ "     [datname] \n"
			+ "    ,[queryid] \n"
			+ "    ,cast('' as varchar(10))                                                          as [NPlusOne] \n"
			+ "    ,sum([calls])                                                                     as [calls] \n"
			+ "    ,sum([rows])                                                                      as [rows] \n"
			+ "    ,cast(sum([rows]) * 1.0 / nullif(sum([calls]), 0) as numeric(12,2))               as [rows_per_call] \n"
			+ "    ,max([calls] * 1000.0 / nullif([CmSampleMs], 0))                                  as [calls_per_sec__max] \n"
			+ "    ,cast(sum([total_time]) * 1000.0 / nullif(sum([calls]), 0) as numeric(14,1))      as [avg_time_us] \n"
			+ "    ,cast(sum([shared_blks_hit] + [shared_blks_read]) * 1.0 / nullif(sum([calls]), 0) as numeric(12,1)) as [avg_logical_reads] \n"
			+ "    ,cast(sum([total_time]) as numeric(16,1))                                         as [total_time_ms] \n"
			+ "    ,cast(sum([calls]) * " + roundTripMs + " as numeric(16,1))                       as [est_round_trip_ms] \n"
			+ "    ,count(distinct [usename])                                                        as [user_count] \n"
			+ "    ,count(distinct [SessionSampleTime])                                              as [samples_count] \n"
			+ "    ,min([SessionSampleTime])                                                         as [first_seen] \n"
			+ "    ,max([SessionSampleTime])                                                         as [last_seen] \n"
			+ "    ,max([" + col_query + "])                                                         as [query] \n"
			+ "    ,cast(null as numeric(5,3))                                                       as [parent_correlation] \n"
			+ "    ,cast(null as numeric(12,2))                                                      as [calls_per_parent_row] \n"
			+ "    ,cast(null as numeric(12,1))                                                      as [calls_per_parent_call] \n"
			+ "    ,cast(null as bigint)                                                             as [parent_queryid] \n"
			+ "    ,cast(null as varchar(4000))                                                      as [parent_query] \n"
			+ "from [" + tabName + "] \n"
			+ "where [calls] > 0 \n"
			+ sql_and_skipNewDiffRateRows
			+ getReportPeriodSqlWhere()
			+ "group by [datname], [queryid] \n"
			+ "order by [calls] desc \n"
			;

		_shortRstm = executeQuery(conn, sql, false, "ChattyStatements");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("ChattyStatements");
			return;
		}

		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_shortRstm, conn, null, CmPgStatements.CM_NAME, "query", null);

		findParents(conn, tabName, col_query, hasDictCompCols, sql_and_skipNewDiffRateRows);

		setSectionDescription(_shortRstm, roundTripMs);
	}

	/**
	 * For every row in _shortRstm (the child candidate): find the parent statement, see {@link NPlusOneParent#findParent(String, Map, List)}
	 * <p>
	 * CmPgStatements holds every statement in pg_stat_statements, so the per sample values are only read for the children
	 * and for the statements that can be a parent by their totals (rows close to a child's calls, several rows per call).
	 */
	private void findParents(DbxConnection conn, String tabName, String col_query, boolean hasDictCompCols, String sql_and_skipNewDiffRateRows)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		double minExecPerRow = conf.getDoubleProperty(NPlusOneParent.PROPKEY_minExecPerRow, NPlusOneParent.DEFAULT_minExecPerRow);
		double maxExecPerRow = conf.getDoubleProperty(NPlusOneParent.PROPKEY_maxExecPerRow, NPlusOneParent.DEFAULT_maxExecPerRow);
		double minFanOut     = conf.getDoubleProperty(NPlusOneParent.PROPKEY_minFanOut,     NPlusOneParent.DEFAULT_minFanOut);

		// Children: key = datname:queryid
		Map<String, long[]> childTotals = new LinkedHashMap<>(); // key -> [calls]
		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String key = _shortRstm.getValueAsString(r, "datname") + ":" + _shortRstm.getValueAsString(r, "queryid");
			childTotals.put(key, new long[] { ((Number) _shortRstm.getValueAsObject(r, "calls")).longValue() });
		}

		// Parent candidates, by the totals over the period (the same sums as findParent() checks)
		Set<Long> queryIds = new LinkedHashSet<>();
		for (int r = 0; r < _shortRstm.getRowCount(); r++)
			queryIds.add(((Number) _shortRstm.getValueAsObject(r, "queryid")).longValue());

		String totSql = conn.quotifySqlString(""
			+ "select [datname], [queryid], sum([calls]), sum([rows]) \n"
			+ "from [" + tabName + "] \n"
			+ "where [calls] > 0 \n"
			+ sql_and_skipNewDiffRateRows
			+ getReportPeriodSqlWhere()
			+ "group by [datname], [queryid] \n"
			);
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(totSql))
		{
			while (rs.next())
			{
				String datname = rs.getString(1);
				long   queryid = rs.getLong  (2);
				long   calls   = rs.getLong  (3);
				long   rows    = rs.getLong  (4);
				if (calls <= 0 || rows <= 0)
					continue;

				for (Map.Entry<String, long[]> e : childTotals.entrySet())
				{
					if ( ! e.getKey().startsWith(datname + ":") )
						continue;
					double childCalls = e.getValue()[0];
					double ratio  = childCalls / rows;  // child calls per parent row
					double fanOut = childCalls / calls; // child calls per parent call
					if (ratio >= minExecPerRow && ratio <= maxExecPerRow && fanOut >= minFanOut)
					{
						queryIds.add(queryid);
						break;
					}
				}
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems reading totals per statement from '" + tabName + "'. Skipping parent detection.", ex);
			return;
		}

		// Per sample: calls and rows (summed over users)
		String sql = conn.quotifySqlString(""
			+ "select [SessionSampleTime], [datname], [queryid], sum([calls]), sum([rows]) \n"
			+ "from [" + tabName + "] \n"
			+ "where [calls] > 0 \n"
			+ "  and [queryid] in (" + StringUtil.toCommaStr(queryIds.toArray()) + ") \n"
			+ sql_and_skipNewDiffRateRows
			+ getReportPeriodSqlWhere()
			+ "group by [SessionSampleTime], [datname], [queryid] \n"
			);

		Map<String, NPlusOneParent.Series> seriesMap = new LinkedHashMap<>();
		TreeSet<Timestamp> allSamples = new TreeSet<>();
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				Timestamp ts      = rs.getTimestamp(1);
				String    datname = rs.getString   (2);
				long      queryid = rs.getLong     (3);
				long      calls   = rs.getLong     (4);
				long      rows    = rs.getLong     (5);

				String key = datname + ":" + queryid;
				NPlusOneParent.Series s = seriesMap.get(key);
				if (s == null)
				{
					s = new NPlusOneParent.Series();
					s.dbname = datname;
					seriesMap.put(key, s);
				}
				s.perSample.put(ts, new long[] {calls, rows});
				allSamples.add(ts);
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems reading per sample values from '" + tabName + "'. Skipping parent detection.", ex);
			return;
		}

		// Samples where no candidate was executed still count (as 0) in the correlation: use all samples in the period
		String sampleSql = conn.quotifySqlString(""
			+ "select distinct [SessionSampleTime] \n"
			+ "from [" + tabName + "] \n"
			+ "where 1 = 1 \n"
			+ getReportPeriodSqlWhere()
			);
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sampleSql))
		{
			while (rs.next())
				allSamples.add(rs.getTimestamp(1));
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems reading sample times from '" + tabName + "'. Using only the samples where the statements were executed.", ex);
		}

		List<Timestamp> samples = new ArrayList<>(allSamples);

		int pos_NPlusOne           = _shortRstm.findColumn("NPlusOne");
		int pos_parentCorrelation  = _shortRstm.findColumn("parent_correlation");
		int pos_callsPerParentRow  = _shortRstm.findColumn("calls_per_parent_row");
		int pos_callsPerParentCall = _shortRstm.findColumn("calls_per_parent_call");
		int pos_parentQueryid      = _shortRstm.findColumn("parent_queryid");
		int pos_parentQuery        = _shortRstm.findColumn("parent_query");

		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String childKey = _shortRstm.getValueAsString(r, "datname") + ":" + _shortRstm.getValueAsString(r, "queryid");
			NPlusOneParent.ParentMatch pm = NPlusOneParent.findParent(childKey, seriesMap, samples);
			if (pm == null)
				continue;

			_nPlusOneCount++;
			long parentQueryid = Long.parseLong(pm.parentKey.substring(pm.parentKey.lastIndexOf(':') + 1));
			String parentDbname = pm.parentKey.substring(0, pm.parentKey.lastIndexOf(':'));

			_shortRstm.setValueAtWithOverride("YES"                                                                   , r, pos_NPlusOne);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.corr).setScale(3, RoundingMode.HALF_UP)               , r, pos_parentCorrelation);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentRow ).setScale(2, RoundingMode.HALF_UP)  , r, pos_callsPerParentRow);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentExec).setScale(1, RoundingMode.HALF_UP)  , r, pos_callsPerParentCall);
			_shortRstm.setValueAtWithOverride(parentQueryid                                                           , r, pos_parentQueryid);

			// Parent query text: from the table if the parent is also a row there, else from the recording
			String text = null;
			for (int pr = 0; pr < _shortRstm.getRowCount(); pr++)
			{
				if (pm.parentKey.equals(_shortRstm.getValueAsString(pr, "datname") + ":" + _shortRstm.getValueAsString(pr, "queryid")))
				{
					text = _shortRstm.getValueAsString(pr, "query");
					break;
				}
			}
			if (text == null)
			{
				String textSql = conn.quotifySqlString("select max([" + col_query + "]) from [" + tabName + "] where [queryid] = " + parentQueryid + " and [datname] = '" + parentDbname.replace("'", "''") + "'");
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(textSql))
				{
					while (rs.next())
						text = rs.getString(1);
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems getting query text for parent queryid " + parentQueryid + ".", ex);
				}

				// Dictionary compressed: the value is a hash, look it up
				if (hasDictCompCols && text != null)
				{
					try
					{
						text = DictCompression.getValueForHashId(conn, null, CmPgStatements.CM_NAME, "query", text);
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems getting Dictionary Compressed query text for parent queryid " + parentQueryid + ".", ex);
					}
				}
			}
			if (text != null && text.length() > 4000)
				text = text.substring(0, 4000);
			_shortRstm.setValueAtWithOverride(text, r, pos_parentQuery);
		}
	}

	private void setSectionDescription(ResultSetTableModel rstm, double roundTripMs)
	{
		if (rstm == null)
			return;

		Configuration conf = Configuration.getCombinedConfiguration();

		rstm.setDescription(
				"The statements with the most calls during the report period. <br>" +
				"<br>" +
				"<b>N+1</b> is when the application first runs one <i>parent</i> statement that returns N rows, and then runs one <i>child</i> statement <b>for each row</b>. <br>" +
				"Each child call is fast inside Postgres, but the application pays one network round trip per call (see <code>est_round_trip_ms</code>). <br>" +
				"<br>" +
				"<code>NPlusOne = YES</code> means: another statement's rows per sample follow this statement's calls per sample " +
				"(correlation at or above " + conf.getDoubleProperty(NPlusOneParent.PROPKEY_minCorrelation, NPlusOneParent.DEFAULT_minCorrelation) + "), " +
				"with about one call per parent row, and several calls per parent call (the parent returns many rows). That statement is shown as the <i>parent</i>. <br>" +
				"A child can be the parent of the next level (for example: orders -&gt; order lines -&gt; product names). <br>" +
				"<br>" +
				"Note: CmPgStatements only samples statements with a <code>total_time</code> (since pg_stat_statements was reset) above <code>" + CmPgStatements.PROPKEY_sample_total_time_gt + "</code> ms, " +
				"so a very cheap statement shows up some time after it started. The first sample of a statement is not counted (its counters are not for one sample). <br>" +
				"<br>" +
				"SQL Source table is 'pg_stat_statements'. <br>" +
				"PCS Source table is 'CmPgStatements_diff'. (PCS = Persistent Counter Store) <br>" +
				"");

		rstm.setColumnDescription("NPlusOne"              , "YES if a parent statement was found (rows of the parent follow calls of this statement).");
		rstm.setColumnDescription("queryid"               , "Internal hash code, computed from the statement's parse tree.");
		rstm.setColumnDescription("calls"                 , "Calls during the report period (all users).");
		rstm.setColumnDescription("rows"                  , "Rows retrieved or affected during the report period.");
		rstm.setColumnDescription("rows_per_call"         , "Rows per call. About 1 for a key lookup.");
		rstm.setColumnDescription("calls_per_sec__max"    , "Highest calls per second in one sample (for one user).");
		rstm.setColumnDescription("avg_time_us"           , "Average time per call in microseconds (inside Postgres).");
		rstm.setColumnDescription("avg_logical_reads"     , "Average shared blocks hit + read per call.");
		rstm.setColumnDescription("total_time_ms"         , "Total time in milliseconds (inside Postgres).");
		rstm.setColumnDescription("est_round_trip_ms"     , "Estimated time the application spent on round trips: calls * " + roundTripMs + " ms (property " + PROPKEY_roundTripMs + ").");
		rstm.setColumnDescription("user_count"            , "Number of users that executed this statement.");
		rstm.setColumnDescription("samples_count"         , "Number of samples where this statement was executed.");
		rstm.setColumnDescription("parent_correlation"    , "Correlation between this statement's calls per sample and the parent's rows per sample (1.0 = moves exactly together).");
		rstm.setColumnDescription("calls_per_parent_row"  , "This statement's calls divided by the parent's rows (about 1.0 for N+1).");
		rstm.setColumnDescription("calls_per_parent_call" , "This statement's calls divided by the parent's calls: the 'N' in N+1 (at least " + conf.getDoubleProperty(NPlusOneParent.PROPKEY_minFanOut, NPlusOneParent.DEFAULT_minFanOut) + " to be reported; statements that just run together in the same transaction have 1.0).");
		rstm.setColumnDescription("parent_queryid"        , "queryid of the parent statement.");
		rstm.setColumnDescription("parent_query"          , "Query text of the parent statement.");
	}
}
