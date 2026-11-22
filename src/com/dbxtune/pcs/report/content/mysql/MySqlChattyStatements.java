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
package com.dbxtune.pcs.report.content.mysql;

import java.io.IOException;
import java.io.Writer;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
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

import com.dbxtune.cm.mysql.CmChattyStatements;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.NPlusOneParent;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

/**
 * "Possible N+1 / chatty statements" from <code>CmChattyStatements</code> (MySQL performance_schema.events_statements_summary_by_digest).
 * <p>
 * Lists the statements with the most executions over the day, and for each one tries to find the <b>parent</b>:
 * another statement (in the same schema) whose rows per sample follow this statement's executions per sample.
 * See {@link NPlusOneParent#findParent(String, Map, List)}
 */
public class MySqlChattyStatements
extends MySqlAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private int _nPlusOneCount = 0;

	public MySqlChattyStatements(DailySummaryReportAbstract reportingInstance)
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
		return "Possible N+1 / Chatty Statements (origin: CmChattyStatements / events_statements_summary_by_digest)";
	}

	@Override
	public IssueLevel getIssueLevel()
	{
		// N+1 is a design finding (fix it in the application), not an incident: FYI
		return _nPlusOneCount > 0 ? IssueLevel.FYI : IssueLevel.NONE;
	}

	@Override
	public String getIssueReason()
	{
		return _nPlusOneCount + " statement(s) that look like N+1";
	}

	@Override
	public int getTopRowsDefault()
	{
		return 20;
	}

	// MySQL's own SQL (and DbxTune's): CmChattyStatements.IS_SYSTEM_STMT = 'Y' (only tables in mysql, sys, performance_schema, information_schema). It can be "N+1", but it is nothing a DBA can change
	public static final String  PROPKEY_skipSystemStmt = "MySqlChattyStatements.nplusone.skip.systemStmt";
	public static final boolean DEFAULT_skipSystemStmt = true;

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
				if ("STMT_TEXT".equals(colName) || "QUERY_SAMPLE_TEXT".equals(colName) || "ParentStmtText".equals(colName))
				{
					String text = rstm.getValueAsString(row, colName);
					if (StringUtil.isNullOrBlank(text))
						return "";
					return getFormattedSqlAsTooltipDiv(text, "Hover or Click to see formatted SQL Text...", DbUtils.DB_PROD_NAME_MYSQL, 120); // the start of the SQL, the full SQL as tooltip + popup
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
			sb.append("(for example JPA/Hibernate <code>JOIN FETCH</code> / <code>@BatchSize</code>). <br>\n");
		}
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String tabName = CmChattyStatements.CM_NAME + "_abs";

		// STMT_TEXT and QUERY_SAMPLE_TEXT can be Dictionary Compressed
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, tabName);
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table '" + tabName + "'.", ex);
		}
		String col_StmtText   = hasDictCompCols ? "STMT_TEXT$dcc$"         : "STMT_TEXT";
		String col_SampleText = hasDictCompCols ? "QUERY_SAMPLE_TEXT$dcc$" : "QUERY_SAMPLE_TEXT";

		int topRows = getTopRows();

		String sql = ""
			+ "select top " + topRows + " \n"
			+ "     [SCHEMA_NAME] \n"
			+ "    ,[DIGEST] \n"
			+ "    ,max([IS_SYSTEM_STMT])                                                       as [IS_SYSTEM_STMT] \n"
			+ "    ,cast('' as varchar(10))                                                     as [NPlusOne] \n"
			+ "    ,sum([ExecCount])                                                            as [ExecCount] \n"
			+ "    ,sum([RowCount])                                                             as [RowCount] \n"
			+ "    ,cast(sum([RowCount]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,2))  as [RowsPerExec] \n"
			+ "    ,max([ExecPerSec])                                                           as [ExecPerSec__max] \n"
			+ "    ,cast(sum([ElapsedMs]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(14,3)) as [AvgElapsedMs] \n"
			+ "    ,cast(sum([RowsExamined]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,1)) as [AvgRowsExamined] \n"
			+ "    ,cast(sum([ElapsedMs]) as numeric(16,1))                                     as [ElapsedMs] \n"
			+ "    ,cast(sum([EstRoundTripMs]) as numeric(16,1))                                as [EstRoundTripMs] \n"
			+ "    ,count(*)                                                                    as [SampleCount] \n"
			+ "    ,min([SessionSampleTime])                                                    as [FirstSeen] \n"
			+ "    ,max([SessionSampleTime])                                                    as [LastSeen] \n"
			+ "    ,max([" + col_StmtText + "])                                                 as [STMT_TEXT] \n"
			+ "    ,max([" + col_SampleText + "])                                               as [QUERY_SAMPLE_TEXT] \n"
			+ "    ,cast(null as numeric(5,3))                                                  as [ParentCorrelation] \n"
			+ "    ,cast(null as numeric(12,2))                                                 as [ExecPerParentRow] \n"
			+ "    ,cast(null as numeric(12,1))                                                 as [ExecPerParentExec] \n"
			+ "    ,cast(null as varchar(64))                                                   as [ParentDigest] \n"
			+ "    ,cast(null as varchar(4000))                                                 as [ParentStmtText] \n"
			+ "from [" + tabName + "] \n"
			+ "where [ExecCount] > 0 \n"
			+ getReportPeriodSqlWhere()
			+ "group by [SCHEMA_NAME], [DIGEST] \n"
			+ "order by [ExecCount] desc \n"
			;

		_shortRstm = executeQuery(conn, sql, false, "ChattyStatements");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("ChattyStatements");
			return;
		}

		if (hasDictCompCols)
		{
			updateDictionaryCompressedColumn(_shortRstm, conn, null, CmChattyStatements.CM_NAME, "STMT_TEXT",         null);
			updateDictionaryCompressedColumn(_shortRstm, conn, null, CmChattyStatements.CM_NAME, "QUERY_SAMPLE_TEXT", null);
		}

		findParents(conn, tabName, col_StmtText, hasDictCompCols);

		setSectionDescription(_shortRstm);
	}

	/**
	 * For every row in _shortRstm (the child candidate): find the parent statement, see {@link NPlusOneParent#findParent(String, Map, List)}
	 * <br>The key is <code>SCHEMA_NAME:DIGEST</code>, and the parent must be in the same schema
	 */
	private void findParents(DbxConnection conn, String tabName, String col_StmtText, boolean hasDictCompCols)
	{
		// Read all samples (the table only holds the top rows per sample, so it's small)
		String sql = conn.quotifySqlString(""
			+ "select [SessionSampleTime], [SCHEMA_NAME], [DIGEST], [ExecCount], [RowCount], [IS_SYSTEM_STMT] \n"
			+ "from [" + tabName + "] \n"
			+ "where 1 = 1 \n"
			+ getReportPeriodSqlWhere()
			);

		boolean skipSystemStmt = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_skipSystemStmt, DEFAULT_skipSystemStmt);

		Map<String, NPlusOneParent.Series> seriesMap = new LinkedHashMap<>();
		TreeSet<Timestamp> allSamples = new TreeSet<>();
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while (rs.next())
			{
				Timestamp ts         = rs.getTimestamp(1);
				String    schemaName = rs.getString   (2);
				String    digest     = rs.getString   (3);
				long      execCount  = rs.getLong     (4);
				long      rowCount   = rs.getLong     (5);
				String    isSystem   = rs.getString   (6);

				// MySQL's own SQL: neither a child nor a parent
				if (skipSystemStmt && "Y".equals(isSystem))
					continue;

				String key = schemaName + ":" + digest;
				NPlusOneParent.Series s = seriesMap.get(key);
				if (s == null)
				{
					s = new NPlusOneParent.Series();
					s.dbname = schemaName;
					seriesMap.put(key, s);
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
		int pos_ParentDigest      = _shortRstm.findColumn("ParentDigest");
		int pos_ParentStmtText    = _shortRstm.findColumn("ParentStmtText");

		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String childKey = _shortRstm.getValueAsString(r, "SCHEMA_NAME") + ":" + _shortRstm.getValueAsString(r, "DIGEST");
			NPlusOneParent.ParentMatch pm = NPlusOneParent.findParent(childKey, seriesMap, samples);
			if (pm == null)
				continue;

			_nPlusOneCount++;
			// The digest is hex (no ':'), a schema name can have one: split at the last ':'
			int    pos          = pm.parentKey.lastIndexOf(':');
			String parentSchema = pm.parentKey.substring(0, pos);
			String parentDigest = pm.parentKey.substring(pos + 1);

			_shortRstm.setValueAtWithOverride("YES"                                                                   , r, pos_NPlusOne);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.corr).setScale(3, RoundingMode.HALF_UP)               , r, pos_ParentCorrelation);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentRow ).setScale(2, RoundingMode.HALF_UP)  , r, pos_ExecPerParentRow);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentExec).setScale(1, RoundingMode.HALF_UP)  , r, pos_ExecPerParentExec);

			// Text of the parent: is the parent also in the table? Then reuse it
			String  text  = null;
			boolean found = false;
			for (int pr = 0; pr < _shortRstm.getRowCount(); pr++)
			{
				if (    parentSchema.equals(_shortRstm.getValueAsString(pr, "SCHEMA_NAME"))
				     && parentDigest.equals(_shortRstm.getValueAsString(pr, "DIGEST")) )
				{
					text  = _shortRstm.getValueAsString(pr, "STMT_TEXT");
					found = true;
					break;
				}
			}

			if ( ! found )
			{
				String textSql = conn.quotifySqlString("select max([" + col_StmtText + "]) from [" + tabName + "] where [SCHEMA_NAME] = ? and [DIGEST] = ?");
				try (PreparedStatement pstmnt = conn.prepareStatement(textSql))
				{
					pstmnt.setString(1, parentSchema);
					pstmnt.setString(2, parentDigest);
					try (ResultSet rs = pstmnt.executeQuery())
					{
						while (rs.next())
							text = rs.getString(1);
					}
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems getting the statement text for parent '" + pm.parentKey + "'.", ex);
				}

				// Dictionary compressed: the value is a hash, look it up
				if (hasDictCompCols && text != null)
				{
					try
					{
						text = DictCompression.getValueForHashId(conn, null, CmChattyStatements.CM_NAME, "STMT_TEXT", text);
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems getting Dictionary Compressed statement text for parent '" + pm.parentKey + "'.", ex);
					}
				}
			}

			if (text != null && text.length() > 4000)
				text = text.substring(0, 4000);
			_shortRstm.setValueAtWithOverride(parentDigest, r, pos_ParentDigest);
			_shortRstm.setValueAtWithOverride(text,         r, pos_ParentStmtText);
		}
	}

	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;

		Configuration conf = Configuration.getCombinedConfiguration();
		rstm.setDescription(
				"Cheap statements (few rows examined per execution) that are executed very often. <br>" +
				"<br>" +
				"<b>N+1</b> is when the application first runs one <i>parent</i> statement that returns N rows, and then runs one <i>child</i> statement <b>for each row</b>. <br>" +
				"Each child call is fast inside MySQL, but the application pays one network round trip per call (see <code>EstRoundTripMs</code>). <br>" +
				"<br>" +
				"<code>NPlusOne = YES</code> means: another statement's rows per sample follow this statement's executions per sample " +
				"(correlation at or above " + conf.getDoubleProperty(NPlusOneParent.PROPKEY_minCorrelation, NPlusOneParent.DEFAULT_minCorrelation) + "), " +
				"with about one execution per parent row, and several executions per parent execution (the parent returns many rows). That statement is shown as the <i>parent</i>. <br>" +
				"A child can be the parent of the next level (for example: orders -&gt; order lines -&gt; product names). <br>" +
				"<br>" +
				"Statements with <code>IS_SYSTEM_STMT = Y</code> (MySQL's own SQL, or DbxTune's: only tables in mysql, sys, performance_schema or information_schema) are listed, but not taken as N+1" +
				(conf.getBooleanProperty(PROPKEY_skipSystemStmt, DEFAULT_skipSystemStmt) ? "" : " (<b>switched off</b>: they ARE taken as N+1 now)") +
				" (property <code>" + PROPKEY_skipSystemStmt + "</code>). <br>" +
				"<br>" +
				"SQL Source table is 'performance_schema.events_statements_summary_by_digest' (per schema and digest, so literal SQL is folded together). <br>" +
				"PCS Source table is 'CmChattyStatements_abs'. (PCS = Persistent Counter Store) <br>" +
				"");

		rstm.setColumnDescription("NPlusOne"           , "YES if a parent statement was found (rows of the parent follow executions of this statement).");
		rstm.setColumnDescription("SCHEMA_NAME"        , "The default schema of the session when the statement ran.");
		rstm.setColumnDescription("DIGEST"             , "The same statement with different literal values has the same digest. The same as 'Digest' in the 'Statements' tab.");
		rstm.setColumnDescription("IS_SYSTEM_STMT"     , "Y = MySQL's own SQL (or DbxTune's): listed, but not taken as N+1.");
		rstm.setColumnDescription("ExecCount"          , "Executions during the report period (in the samples where this statement was in the top list).");
		rstm.setColumnDescription("RowCount"           , "Rows sent to the client during the report period (SUM_ROWS_SENT).");
		rstm.setColumnDescription("RowsPerExec"        , "Rows sent per execution. About 1 for a key lookup.");
		rstm.setColumnDescription("ExecPerSec__max"    , "Highest executions per second in one sample.");
		rstm.setColumnDescription("AvgElapsedMs"       , "Average execution time per execution in milliseconds (inside MySQL).");
		rstm.setColumnDescription("AvgRowsExamined"    , "Average rows examined per execution.");
		rstm.setColumnDescription("ElapsedMs"          , "Total execution time in milliseconds (inside MySQL).");
		rstm.setColumnDescription("EstRoundTripMs"     , "Estimated time the application spent on round trips: executions * " + CmChattyStatements.PROPKEY_roundTripMs + ".");
		rstm.setColumnDescription("SampleCount"        , "Number of samples where this statement was in the top list.");
		rstm.setColumnDescription("STMT_TEXT"          , "The normalized statement (DIGEST_TEXT).");
		rstm.setColumnDescription("QUERY_SAMPLE_TEXT"  , "One example of the statement, with its values.");
		rstm.setColumnDescription("ParentCorrelation"  , "Correlation between this statement's executions per sample and the parent's rows per sample (1.0 = moves exactly together).");
		rstm.setColumnDescription("ExecPerParentRow"   , "This statement's executions divided by the parent's rows (about 1.0 for N+1).");
		rstm.setColumnDescription("ExecPerParentExec"  , "This statement's executions divided by the parent's executions: the 'N' in N+1 (at least " + conf.getDoubleProperty(NPlusOneParent.PROPKEY_minFanOut, NPlusOneParent.DEFAULT_minFanOut) + " to be reported; statements that just run together in the same transaction have 1.0).");
		rstm.setColumnDescription("ParentDigest"       , "DIGEST of the parent statement (in the same schema).");
		rstm.setColumnDescription("ParentStmtText"     , "Text of the parent statement.");
	}
}
