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

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.db2.CmChattyStatements;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.NPlusOneParent;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * "Possible N+1 / chatty statements" from <code>CmChattyStatements</code> (Db2 MON_GET_PKG_CACHE_STMT).
 * <p>
 * Lists the statements with the most executions over the day, and for each one tries to find the <b>parent</b>:
 * another statement whose rows per sample follow this statement's executions per sample.
 * See {@link NPlusOneParent#findParent(String, Map, List)}
 */
public class Db2ChattyStatements
extends Db2Abstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private int _nPlusOneCount = 0;

	public Db2ChattyStatements(DailySummaryReportAbstract reportingInstance)
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
		return "Possible N+1 / Chatty Statements (origin: CmChattyStatements / MON_GET_PKG_CACHE_STMT)";
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

	// Db2's own SQL (and DbxTune's): CmChattyStatements.IS_SYSTEM_STMT = 'Y' (SYS* package, or only Db2's own tables). It can be "N+1", but it is nothing a DBA can change
	public static final String  PROPKEY_skipSystemStmt = "Db2ChattyStatements.nplusone.skip.systemStmt";
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
				if ("STMT_TEXT".equals(colName) || "ParentStmtText".equals(colName))
				{
					String text = rstm.getValueAsString(row, colName);
					if (StringUtil.isNullOrBlank(text))
						return "";
					String shortText = text.length() > 120 ? text.substring(0, 120) + "..." : text;
					return "<code title=\"" + StringEscapeUtils.escapeHtml4(text) + "\">" + StringEscapeUtils.escapeHtml4(shortText) + "</code>";
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

		// STMT_TEXT can be Dictionary Compressed
		boolean hasDictCompCols = false;
		try {
			hasDictCompCols = DictCompression.hasCompressedColumnNames(conn, null, tabName);
		} catch (SQLException ex) {
			_logger.error("Problems checking for Dictionary Compressed Columns in table '" + tabName + "'.", ex);
		}
		String col_StmtText = hasDictCompCols ? "STMT_TEXT$dcc$" : "STMT_TEXT";

		int topRows = getTopRows();

		String sql = ""
			+ "select top " + topRows + " \n"
			+ "     [MEMBER] \n"
			+ "    ,[STMTID] \n"
			+ "    ,[SEMANTIC_ENV_ID] \n"
			+ "    ,max([EXECUTABLE_ID])                                                        as [EXECUTABLE_ID] \n"
			+ "    ,max([SECTION_TYPE])                                                         as [SECTION_TYPE] \n"
			+ "    ,max([PACKAGE_SCHEMA])                                                       as [PACKAGE_SCHEMA] \n"
			+ "    ,max([IS_SYSTEM_STMT])                                                       as [IS_SYSTEM_STMT] \n"
			+ "    ,cast('' as varchar(10))                                                     as [NPlusOne] \n"
			+ "    ,sum([ExecCount])                                                            as [ExecCount] \n"
			+ "    ,sum([RowCount])                                                             as [RowCount] \n"
			+ "    ,cast(sum([RowCount]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,2))  as [RowsPerExec] \n"
			+ "    ,max([ExecPerSec])                                                           as [ExecPerSec__max] \n"
			+ "    ,cast(sum([ElapsedMs]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(14,3)) as [AvgElapsedMs] \n"
			+ "    ,cast(sum([LogicalReads]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,1)) as [AvgLogicalReads] \n"
			+ "    ,cast(sum([ElapsedMs]) as numeric(16,1))                                     as [ElapsedMs] \n"
			+ "    ,cast(sum([EstRoundTripMs]) as numeric(16,1))                                as [EstRoundTripMs] \n"
			+ "    ,max([ExecIdCount])                                                          as [ExecIdCount__max] \n"
			+ "    ,count(*)                                                                    as [SampleCount] \n"
			+ "    ,min([SessionSampleTime])                                                    as [FirstSeen] \n"
			+ "    ,max([SessionSampleTime])                                                    as [LastSeen] \n"
			+ "    ,max([" + col_StmtText + "])                                                 as [STMT_TEXT] \n"
			+ "    ,cast(null as numeric(5,3))                                                  as [ParentCorrelation] \n"
			+ "    ,cast(null as numeric(12,2))                                                 as [ExecPerParentRow] \n"
			+ "    ,cast(null as numeric(12,1))                                                 as [ExecPerParentExec] \n"
			+ "    ,cast(null as varchar(64))                                                   as [ParentExecutableId] \n"
			+ "    ,cast(null as varchar(4000))                                                 as [ParentStmtText] \n"
			+ "from [" + tabName + "] \n"
			+ "where [ExecCount] > 0 \n"
			+ getReportPeriodSqlWhere()
			+ "group by [MEMBER], [STMTID], [SEMANTIC_ENV_ID] \n"
			+ "order by [ExecCount] desc \n"
			;

		_shortRstm = executeQuery(conn, sql, false, "ChattyStatements");
		if (_shortRstm == null)
		{
			_shortRstm = ResultSetTableModel.createEmpty("ChattyStatements");
			return;
		}

		if (hasDictCompCols)
			updateDictionaryCompressedColumn(_shortRstm, conn, null, CmChattyStatements.CM_NAME, "STMT_TEXT", null);

		findParents(conn, tabName, col_StmtText, hasDictCompCols);

		setSectionDescription(_shortRstm);
	}

	/**
	 * For every row in _shortRstm (the child candidate): find the parent statement, see {@link NPlusOneParent#findParent(String, Map, List)}
	 * <br>The key is <code>MEMBER:STMTID:SEMANTIC_ENV_ID</code>, and the parent must be on the same member
	 */
	private void findParents(DbxConnection conn, String tabName, String col_StmtText, boolean hasDictCompCols)
	{
		// Read all samples (the table only holds the top rows per sample, so it's small)
		String sql = conn.quotifySqlString(""
			+ "select [SessionSampleTime], [MEMBER], [STMTID], [SEMANTIC_ENV_ID], [ExecCount], [RowCount], [IS_SYSTEM_STMT] \n"
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
				Timestamp ts        = rs.getTimestamp(1);
				int       member    = rs.getInt      (2);
				String    stmtId    = rs.getString   (3);
				long      semEnvId  = rs.getLong     (4);
				long      execCount = rs.getLong     (5);
				long      rowCount  = rs.getLong     (6);
				String    isSystem  = rs.getString   (7);

				// Db2's own SQL: neither a child nor a parent
				if (skipSystemStmt && "Y".equals(isSystem))
					continue;

				String key = member + ":" + stmtId + ":" + semEnvId;
				NPlusOneParent.Series s = seriesMap.get(key);
				if (s == null)
				{
					s = new NPlusOneParent.Series();
					s.dbname = "member=" + member;
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

		int pos_NPlusOne           = _shortRstm.findColumn("NPlusOne");
		int pos_ParentCorrelation  = _shortRstm.findColumn("ParentCorrelation");
		int pos_ExecPerParentRow   = _shortRstm.findColumn("ExecPerParentRow");
		int pos_ExecPerParentExec  = _shortRstm.findColumn("ExecPerParentExec");
		int pos_ParentExecutableId = _shortRstm.findColumn("ParentExecutableId");
		int pos_ParentStmtText     = _shortRstm.findColumn("ParentStmtText");

		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String childKey = _shortRstm.getValueAsString(r, "MEMBER") + ":" + _shortRstm.getValueAsString(r, "STMTID") + ":" + _shortRstm.getValueAsString(r, "SEMANTIC_ENV_ID");
			NPlusOneParent.ParentMatch pm = NPlusOneParent.findParent(childKey, seriesMap, samples);
			if (pm == null)
				continue;

			_nPlusOneCount++;
			String[] parentKey = pm.parentKey.split(":");
			int    parentMember   = Integer.parseInt(parentKey[0]);
			String parentStmtId   = parentKey[1];
			long   parentSemEnvId = Long.parseLong(parentKey[2]);

			_shortRstm.setValueAtWithOverride("YES"                                                                   , r, pos_NPlusOne);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.corr).setScale(3, RoundingMode.HALF_UP)               , r, pos_ParentCorrelation);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentRow ).setScale(2, RoundingMode.HALF_UP)  , r, pos_ExecPerParentRow);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentExec).setScale(1, RoundingMode.HALF_UP)  , r, pos_ExecPerParentExec);

			// EXECUTABLE_ID and text of the parent: is the parent also in the table? Then reuse them
			String execId = null;
			String text   = null;
			for (int pr = 0; pr < _shortRstm.getRowCount(); pr++)
			{
				if (    parentKey[0].equals(_shortRstm.getValueAsString(pr, "MEMBER"))
				     && parentKey[1].equals(_shortRstm.getValueAsString(pr, "STMTID"))
				     && parentKey[2].equals(_shortRstm.getValueAsString(pr, "SEMANTIC_ENV_ID")) )
				{
					execId = _shortRstm.getValueAsString(pr, "EXECUTABLE_ID");
					text   = _shortRstm.getValueAsString(pr, "STMT_TEXT");
					break;
				}
			}

			if (execId == null)
			{
				String textSql = conn.quotifySqlString("select max([EXECUTABLE_ID]), max([" + col_StmtText + "]) from [" + tabName + "] where [MEMBER] = ? and [STMTID] = ? and [SEMANTIC_ENV_ID] = ?");
				try (PreparedStatement pstmnt = conn.prepareStatement(textSql))
				{
					pstmnt.setInt   (1, parentMember);
					pstmnt.setString(2, parentStmtId);
					pstmnt.setLong  (3, parentSemEnvId);
					try (ResultSet rs = pstmnt.executeQuery())
					{
						while (rs.next())
						{
							execId = rs.getString(1);
							text   = rs.getString(2);
						}
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
			_shortRstm.setValueAtWithOverride(execId, r, pos_ParentExecutableId);
			_shortRstm.setValueAtWithOverride(text,   r, pos_ParentStmtText);
		}
	}

	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;

		rstm.setDescription(
				"Cheap statements (few logical reads per execution) that are executed very often. <br>" +
				"<br>" +
				"<b>N+1</b> is when the application first runs one <i>parent</i> statement that returns N rows, and then runs one <i>child</i> statement <b>for each row</b>. <br>" +
				"Each child call is fast inside Db2, but the application pays one network round trip per call (see <code>EstRoundTripMs</code>). <br>" +
				"<br>" +
				"<code>NPlusOne = YES</code> means: another statement's rows per sample follow this statement's executions per sample " +
				"(correlation at or above " + Configuration.getCombinedConfiguration().getDoubleProperty(NPlusOneParent.PROPKEY_minCorrelation, NPlusOneParent.DEFAULT_minCorrelation) + "), " +
				"with about one execution per parent row, and several executions per parent execution (the parent returns many rows). That statement is shown as the <i>parent</i>. <br>" +
				"A child can be the parent of the next level (for example: orders -&gt; order lines -&gt; product names). <br>" +
				"<br>" +
				"Statements with <code>IS_SYSTEM_STMT = Y</code> (Db2's own SQL, or DbxTune's: a package in a SYS* schema, or only Db2's own tables like the catalog or MON_GET_*) are listed, but not taken as N+1" +
				(Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_skipSystemStmt, DEFAULT_skipSystemStmt) ? "" : " (<b>switched off</b>: they ARE taken as N+1 now)") +
				" (property <code>" + PROPKEY_skipSystemStmt + "</code>). <br>" +
				"<br>" +
				"SQL Source table is 'MON_GET_PKG_CACHE_STMT' (grouped by STMTID, so literal SQL is folded together). <br>" +
				"PCS Source table is 'CmChattyStatements_abs'. (PCS = Persistent Counter Store) <br>" +
				"");

		rstm.setColumnDescription("NPlusOne"           , "YES if a parent statement was found (rows of the parent follow executions of this statement).");
		rstm.setColumnDescription("MEMBER"             , "The database member (0 if this is not pureScale or DPF).");
		rstm.setColumnDescription("STMTID"             , "The same statement with different literal values has the same STMTID.");
		rstm.setColumnDescription("SEMANTIC_ENV_ID"    , "The default schema and function path the statement was compiled with.");
		rstm.setColumnDescription("EXECUTABLE_ID"      , "A package cache entry with this STMTID (with parameter markers there is only one). The same id as in the 'Statements' tab.");
		rstm.setColumnDescription("SECTION_TYPE"       , "D = dynamic SQL, S = static SQL.");
		rstm.setColumnDescription("PACKAGE_SCHEMA"     , "The schema of the package (static SQL). NULL for dynamic SQL.");
		rstm.setColumnDescription("IS_SYSTEM_STMT"     , "Y = Db2's own SQL (or DbxTune's): listed, but not taken as N+1.");
		rstm.setColumnDescription("ExecCount"          , "Executions during the report period (in the samples where this statement was in the top list).");
		rstm.setColumnDescription("RowCount"           , "Rows returned during the report period (ROWS_RETURNED).");
		rstm.setColumnDescription("RowsPerExec"        , "Rows returned per execution. About 1 for a key lookup.");
		rstm.setColumnDescription("ExecPerSec__max"    , "Highest executions per second in one sample.");
		rstm.setColumnDescription("AvgElapsedMs"       , "Average execution time per execution in milliseconds (inside Db2).");
		rstm.setColumnDescription("AvgLogicalReads"    , "Average buffer pool logical reads per execution.");
		rstm.setColumnDescription("ElapsedMs"          , "Total execution time in milliseconds (inside Db2).");
		rstm.setColumnDescription("EstRoundTripMs"     , "Estimated time the application spent on round trips: executions * " + CmChattyStatements.PROPKEY_roundTripMs + ".");
		rstm.setColumnDescription("ExecIdCount__max"   , "Highest number of package cache entries with this STMTID in one sample. Many of them = literal SQL (no parameter markers).");
		rstm.setColumnDescription("SampleCount"        , "Number of samples where this statement was in the top list.");
		rstm.setColumnDescription("ParentCorrelation"  , "Correlation between this statement's executions per sample and the parent's rows per sample (1.0 = moves exactly together).");
		rstm.setColumnDescription("ExecPerParentRow"   , "This statement's executions divided by the parent's rows (about 1.0 for N+1).");
		rstm.setColumnDescription("ExecPerParentExec"  , "This statement's executions divided by the parent's executions: the 'N' in N+1 (at least " + Configuration.getCombinedConfiguration().getDoubleProperty(NPlusOneParent.PROPKEY_minFanOut, NPlusOneParent.DEFAULT_minFanOut) + " to be reported; statements that just run together in the same transaction have 1.0).");
		rstm.setColumnDescription("ParentExecutableId" , "EXECUTABLE_ID of the parent statement.");
		rstm.setColumnDescription("ParentStmtText"     , "Text of the parent statement.");
	}
}
