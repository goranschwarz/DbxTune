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
package com.dbxtune.pcs.report.content.sqlserver;

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
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.sqlserver.CmChattyStatements;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.NPlusOneParent;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * "Possible N+1 / chatty statements" from <code>CmChattyStatements</code>.
 * <p>
 * Lists the statements with the most executions over the day, and for each one tries to find the <b>parent</b>:
 * another statement whose rows per sample follow this statement's executions per sample
 * (high correlation, and about one execution per parent row).
 */
public class SqlServerChattyStatements
extends SqlServerAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private int _nPlusOneCount = 0;

	public SqlServerChattyStatements(DailySummaryReportAbstract reportingInstance)
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
		return "Possible N+1 / Chatty Statements (origin: CmChattyStatements/dm_exec_query_stats)";
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
		return new String[] { CmChattyStatements.CM_NAME + "_abs" };
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
			sb.append("No statement had a parent statement whose rows follow its executions (no N+1 pattern found). <br>\n");

		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");

		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				if ("SqlText".equals(colName) || "ParentSqlText".equals(colName))
				{
					String text = rstm.getValueAsString(row, colName);
					if (StringUtil.isNullOrBlank(text))
						return "";
					return getFormattedSqlAsTooltipDiv(text, "Hover or Click to see formatted SQL Text...", DbUtils.DB_PROD_NAME_MSSQL);
				}
				if ("NPlusOne".equals(colName) && "YES".equals(strVal))
					return "<b><font color='red'>YES</font></b>";

				return strVal;
			}
		}));

		if (_nPlusOneCount > 0)
		{
			sb.append("<br>\n");
			sb.append("<b>How to fix N+1:</b> instead of one lookup per row, fetch all rows in one statement: \n");
			sb.append("a JOIN with the parent, a <code>WHERE key IN (...)</code> list, or the ORM's eager/batch fetching ");
			sb.append("(for example JPA/Hibernate <code>JOIN FETCH</code> / <code>@BatchSize</code>, Entity Framework <code>Include()</code>). <br>\n");
		}
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String tabName = CmChattyStatements.CM_NAME + "_abs";

		// SqlText can be Dictionary Compressed
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, tabName);
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table '" + tabName + "'.", ex);
		}
		String col_SqlText = hasDictCompCols ? "SqlText$dcc$" : "SqlText";

		int topRows = getTopRows();

		String sql = ""
			+ "select top " + topRows + " \n"
			+ "     [dbname] \n"
			+ "    ,[query_hash] \n"
			+ "    ,cast('' as varchar(10))                                                      as [NPlusOne] \n"
			+ "    ,sum([ExecCount])                                                            as [ExecCount] \n"
			+ "    ,sum([RowCount])                                                             as [RowCount] \n"
			+ "    ,cast(sum([RowCount]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,2))  as [RowsPerExec] \n"
			+ "    ,max([ExecPerSec])                                                           as [ExecPerSec__max] \n"
			+ "    ,cast(sum([ElapsedMs]) * 1000.0 / nullif(sum([ExecCount]), 0) as numeric(14,1)) as [AvgElapsedUs] \n"
			+ "    ,cast(sum([LogicalReads]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,1)) as [AvgLogicalReads] \n"
			+ "    ,cast(sum([ElapsedMs]) as numeric(16,1))                                     as [ElapsedMs] \n"
			+ "    ,cast(sum([EstRoundTripMs]) as numeric(16,1))                                as [EstRoundTripMs] \n"
			+ "    ,max([PlanCount])                                                            as [PlanCount__max] \n"
			+ "    ,count(*)                                                                    as [SampleCount] \n"
			+ "    ,min([SessionSampleTime])                                                    as [FirstSeen] \n"
			+ "    ,max([SessionSampleTime])                                                    as [LastSeen] \n"
			+ "    ,max([" + col_SqlText + "])                                                  as [SqlText] \n"
			+ "    ,cast(null as numeric(5,3))                                                  as [ParentCorrelation] \n"
			+ "    ,cast(null as numeric(12,2))                                                 as [ExecPerParentRow] \n"
			+ "    ,cast(null as numeric(12,1))                                                 as [ExecPerParentExec] \n"
			+ "    ,cast(null as varchar(200))                                                  as [ParentQueryHash] \n"
			+ "    ,cast(null as varchar(4000))                                                 as [ParentSqlText] \n"
			+ "from [" + tabName + "] \n"
			+ "where [ExecCount] > 0 \n"
			+ getReportPeriodSqlWhere()
			+ "group by [dbname], [query_hash] \n"
			+ "order by [ExecCount] desc \n"
			;

		_shortRstm = executeQuery(conn, sql, false, "ChattyStatements");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("ChattyStatements");
			return;
		}

		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_shortRstm, conn, null, CmChattyStatements.CM_NAME, "SqlText", null);

		findParents(conn, tabName, col_SqlText, hasDictCompCols);

		setSectionDescription(_shortRstm);
	}

	/**
	 * For every row in _shortRstm (the child candidate): find the parent statement, see {@link NPlusOneParent#findParent(String, Map, List)}
	 */
	private void findParents(DbxConnection conn, String tabName, String col_SqlText, boolean hasDictCompCols)
	{
		// Read all samples (the table only holds the top rows per sample, so it's small)
		String sql = conn.quotifySqlString(""
			+ "select [SessionSampleTime], [dbname], [query_hash], [ExecCount], [RowCount] \n"
			+ "from [" + tabName + "] \n"
			+ "where 1 = 1 \n"
			+ getReportPeriodSqlWhere()
			);

		Map<String, NPlusOneParent.Series> seriesMap = new LinkedHashMap<>();
		TreeSet<Timestamp> allSamples = new TreeSet<>();
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				Timestamp ts        = rs.getTimestamp(1);
				String    dbname    = rs.getString   (2);
				String    queryHash = rs.getString   (3);
				long      execCount = rs.getLong     (4);
				long      rowCount  = rs.getLong     (5);

				NPlusOneParent.Series s = seriesMap.get(queryHash);
				if (s == null)
				{
					s = new NPlusOneParent.Series();
					s.dbname = dbname;
					seriesMap.put(queryHash, s);
				}
				s.perSample.put(ts, new long[] {execCount, rowCount});
				allSamples.add(ts);
			}
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems reading per sample values from '" + tabName + "'. Skipping parent detection.", ex);
			return;
		}

		List<Timestamp> samples = new ArrayList<>(allSamples);

		int pos_NPlusOne          = _shortRstm.findColumn("NPlusOne");
		int pos_ParentCorrelation = _shortRstm.findColumn("ParentCorrelation");
		int pos_ExecPerParentRow  = _shortRstm.findColumn("ExecPerParentRow");
		int pos_ExecPerParentExec = _shortRstm.findColumn("ExecPerParentExec");
		int pos_ParentQueryHash   = _shortRstm.findColumn("ParentQueryHash");

		Map<String, Integer> parentRowsToLookup = new LinkedHashMap<>(); // parentQueryHash -> row in _shortRstm

		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String childHash = _shortRstm.getValueAsString(r, "query_hash");
			NPlusOneParent.ParentMatch pm = NPlusOneParent.findParent(childHash, seriesMap, samples);
			if (pm != null)
			{
				_nPlusOneCount++;
				_shortRstm.setValueAtWithOverride("YES"                                                                   , r, pos_NPlusOne);
				_shortRstm.setValueAtWithOverride(new BigDecimal(pm.corr).setScale(3, RoundingMode.HALF_UP)               , r, pos_ParentCorrelation);
				_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentRow ).setScale(2, RoundingMode.HALF_UP)  , r, pos_ExecPerParentRow);
				_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentExec).setScale(1, RoundingMode.HALF_UP)  , r, pos_ExecPerParentExec);
				_shortRstm.setValueAtWithOverride(pm.parentKey                                                            , r, pos_ParentQueryHash);
				parentRowsToLookup.put(pm.parentKey + "|" + r, r);
			}
		}

		// Get SQL Text for the parents
		if ( ! parentRowsToLookup.isEmpty() )
		{
			int pos_ParentSqlText = _shortRstm.findColumn("ParentSqlText");
			for (Map.Entry<String, Integer> e : parentRowsToLookup.entrySet())
			{
				String parentHash = e.getKey().substring(0, e.getKey().lastIndexOf('|'));
				int    row        = e.getValue();

				// Is the parent also in the table? Then reuse the text
				String text = null;
				for (int r = 0; r < _shortRstm.getRowCount(); r++)
				{
					if (parentHash.equals(_shortRstm.getValueAsString(r, "query_hash")))
					{
						text = _shortRstm.getValueAsString(r, "SqlText");
						break;
					}
				}

				if (text == null)
				{
					String textSql = conn.quotifySqlString("select max([" + col_SqlText + "]) from [" + tabName + "] where [query_hash] = '" + parentHash.replace("'", "''") + "'");
					try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(textSql))
					{
						while (rs.next())
							text = rs.getString(1);
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems getting SQL Text for parent query_hash '" + parentHash + "'.", ex);
					}

					// Dictionary compressed: the value is a hash, look it up
					if (hasDictCompCols && text != null)
					{
						try
						{
							text = DictCompression.getValueForHashId(conn, null, CmChattyStatements.CM_NAME, "SqlText", text);
						}
						catch (SQLException ex)
						{
							_logger.warn("Problems getting Dictionary Compressed SQL Text for parent query_hash '" + parentHash + "'.", ex);
						}
					}
				}

				if (text != null && text.length() > 4000)
					text = text.substring(0, 4000);
				_shortRstm.setValueAtWithOverride(text, row, pos_ParentSqlText);
			}
		}
	}

	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;

		rstm.setDescription(
				"Cheap statements that are executed very often (the <i>Query Stat</i> sections only show statements with more than 100 logical reads). <br>" +
				"<br>" +
				"<b>N+1</b> is when the application first runs one <i>parent</i> statement that returns N rows, and then runs one <i>child</i> statement <b>for each row</b>. <br>" +
				"Each child call is fast inside SQL Server, but the application pays one network round trip per call (see <code>EstRoundTripMs</code>). <br>" +
				"<br>" +
				"<code>NPlusOne = YES</code> means: another statement's rows per sample follow this statement's executions per sample " +
				"(correlation at or above " + Configuration.getCombinedConfiguration().getDoubleProperty(NPlusOneParent.PROPKEY_minCorrelation, NPlusOneParent.DEFAULT_minCorrelation) + "), " +
				"with about one execution per parent row, and several executions per parent execution (the parent returns many rows). That statement is shown as the <i>parent</i>. <br>" +
				"A child can be the parent of the next level (for example: orders -&gt; order lines -&gt; product names). <br>" +
				"<br>" +
				"SQL Source table is 'sys.dm_exec_query_stats'. <br>" +
				"PCS Source table is 'CmChattyStatements_abs'. (PCS = Persistent Counter Store) <br>" +
				"");

		rstm.setColumnDescription("NPlusOne"          , "YES if a parent statement was found (rows of the parent follow executions of this statement).");
		rstm.setColumnDescription("ExecCount"         , "Executions during the report period (in the samples where this statement was in the top list).");
		rstm.setColumnDescription("RowCount"          , "Rows returned during the report period.");
		rstm.setColumnDescription("RowsPerExec"       , "Rows per execution. About 1 for a key lookup.");
		rstm.setColumnDescription("ExecPerSec__max"   , "Highest executions per second in one sample.");
		rstm.setColumnDescription("AvgElapsedUs"      , "Average elapsed time per execution in microseconds (inside SQL Server).");
		rstm.setColumnDescription("AvgLogicalReads"   , "Average logical reads per execution.");
		rstm.setColumnDescription("ElapsedMs"         , "Total elapsed time in milliseconds (inside SQL Server).");
		rstm.setColumnDescription("EstRoundTripMs"    , "Estimated time the application spent on round trips: executions * " + CmChattyStatements.PROPKEY_roundTripMs + ".");
		rstm.setColumnDescription("PlanCount__max"    , "Highest number of plans with this query_hash in one sample. Many plans = literal SQL (not parameterized).");
		rstm.setColumnDescription("SampleCount"       , "Number of samples where this statement was in the top list.");
		rstm.setColumnDescription("ParentCorrelation" , "Correlation between this statement's executions per sample and the parent's rows per sample (1.0 = moves exactly together).");
		rstm.setColumnDescription("ExecPerParentRow"  , "This statement's executions divided by the parent's rows (about 1.0 for N+1).");
		rstm.setColumnDescription("ExecPerParentExec" , "This statement's executions divided by the parent's executions: the 'N' in N+1 (at least " + Configuration.getCombinedConfiguration().getDoubleProperty(NPlusOneParent.PROPKEY_minFanOut, NPlusOneParent.DEFAULT_minFanOut) + " to be reported; statements that just run together in the same transaction have 1.0).");
		rstm.setColumnDescription("ParentQueryHash"   , "query_hash of the parent statement.");
		rstm.setColumnDescription("ParentSqlText"     , "SQL Text of the parent statement.");

	}
}
