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
package com.dbxtune.pcs.report.content.ase;

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

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.ase.CmChattyStatements;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.DictCompression;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.NPlusOneParent;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * "Possible N+1 / chatty statements" from <code>CmChattyStatements</code> (ASE statement cache, monCachedStatement).
 * <p>
 * Lists the statements with the most executions over the day, and for each one tries to find the <b>parent</b>:
 * another statement whose rows per sample follow this statement's executions per sample.
 * See {@link NPlusOneParent#findParent(String, Map, List)}
 */
public class AseChattyStatements
extends AseAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private int _nPlusOneCount = 0;

	public AseChattyStatements(DailySummaryReportAbstract reportingInstance)
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
		return "Possible N+1 / Chatty Statements (origin: CmChattyStatements / monCachedStatement)";
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
			+ "     [DBName] \n"
			+ "    ,[DBID] \n"
			+ "    ,cast([Hashkey] as varchar(20))                                              as [Hashkey] \n" // text: no digit grouping in the report
			+ "    ,cast('' as varchar(10))                                                     as [NPlusOne] \n"
			+ "    ,sum([ExecCount])                                                            as [ExecCount] \n"
			+ "    ,sum([RowCount])                                                             as [RowCount] \n"
			+ "    ,cast(sum([RowCount]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,2))  as [RowsPerExec] \n"
			+ "    ,max([ExecPerSec])                                                           as [ExecPerSec__max] \n"
			+ "    ,cast(sum([ElapsedMs]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(14,3)) as [AvgElapsedMs] \n"
			+ "    ,cast(sum([LogicalReads]) * 1.0 / nullif(sum([ExecCount]), 0) as numeric(12,1)) as [AvgLogicalReads] \n"
			+ "    ,sum([ElapsedMs])                                                            as [ElapsedMs] \n"
			+ "    ,cast(sum([EstRoundTripMs]) as numeric(16,1))                                as [EstRoundTripMs] \n"
			+ "    ,max([EntryCount])                                                           as [EntryCount__max] \n"
			+ "    ,count(*)                                                                    as [SampleCount] \n"
			+ "    ,min([SessionSampleTime])                                                    as [FirstSeen] \n"
			+ "    ,max([SessionSampleTime])                                                    as [LastSeen] \n"
			+ "    ,max([" + col_SqlText + "])                                                  as [SqlText] \n"
			+ "    ,cast(null as numeric(5,3))                                                  as [ParentCorrelation] \n"
			+ "    ,cast(null as numeric(12,2))                                                 as [ExecPerParentRow] \n"
			+ "    ,cast(null as numeric(12,1))                                                 as [ExecPerParentExec] \n"
			+ "    ,cast(null as varchar(20))                                                   as [ParentHashkey] \n"
			+ "    ,cast(null as varchar(4000))                                                 as [ParentSqlText] \n"
			+ "from [" + tabName + "] \n"
			+ "where [ExecCount] > 0 \n"
			+ getReportPeriodSqlWhere()
			+ "group by [DBName], [DBID], [Hashkey] \n"
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
	 * <br>The key is <code>DBID:Hashkey</code>
	 */
	private void findParents(DbxConnection conn, String tabName, String col_SqlText, boolean hasDictCompCols)
	{
		// Read all samples (the table only holds the top rows per sample, so it's small)
		String sql = conn.quotifySqlString(""
			+ "select [SessionSampleTime], [DBName], [DBID], [Hashkey], [ExecCount], [RowCount] \n"
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
				int       dbid      = rs.getInt      (3);
				int       hashkey   = rs.getInt      (4);
				long      execCount = rs.getLong     (5);
				long      rowCount  = rs.getLong     (6);

				String key = dbid + ":" + hashkey;
				NPlusOneParent.Series s = seriesMap.get(key);
				if (s == null)
				{
					s = new NPlusOneParent.Series();
					s.dbname = dbname != null ? dbname : "dbid=" + dbid;
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
		int pos_ParentHashkey     = _shortRstm.findColumn("ParentHashkey");
		int pos_ParentSqlText     = _shortRstm.findColumn("ParentSqlText");

		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String childKey = _shortRstm.getValueAsString(r, "DBID") + ":" + _shortRstm.getValueAsString(r, "Hashkey");
			NPlusOneParent.ParentMatch pm = NPlusOneParent.findParent(childKey, seriesMap, samples);
			if (pm == null)
				continue;

			_nPlusOneCount++;
			String parentDbid    = pm.parentKey.substring(0, pm.parentKey.indexOf(':'));
			String parentHashkey = pm.parentKey.substring(pm.parentKey.indexOf(':') + 1);

			_shortRstm.setValueAtWithOverride("YES"                                                                   , r, pos_NPlusOne);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.corr).setScale(3, RoundingMode.HALF_UP)               , r, pos_ParentCorrelation);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentRow ).setScale(2, RoundingMode.HALF_UP)  , r, pos_ExecPerParentRow);
			_shortRstm.setValueAtWithOverride(new BigDecimal(pm.execPerParentExec).setScale(1, RoundingMode.HALF_UP)  , r, pos_ExecPerParentExec);
			_shortRstm.setValueAtWithOverride(parentHashkey                                                           , r, pos_ParentHashkey);

			// SQL Text of the parent: is the parent also in the table? Then reuse the text
			String text = null;
			for (int pr = 0; pr < _shortRstm.getRowCount(); pr++)
			{
				if (parentDbid.equals(_shortRstm.getValueAsString(pr, "DBID")) && parentHashkey.equals(_shortRstm.getValueAsString(pr, "Hashkey")))
				{
					text = _shortRstm.getValueAsString(pr, "SqlText");
					break;
				}
			}

			if (text == null)
			{
				String textSql = conn.quotifySqlString("select max([" + col_SqlText + "]) from [" + tabName + "] where [DBID] = " + Integer.parseInt(parentDbid) + " and [Hashkey] = " + Integer.parseInt(parentHashkey));
				try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(textSql))
				{
					while (rs.next())
						text = rs.getString(1);
				}
				catch (SQLException ex)
				{
					_logger.warn("Problems getting SQL Text for parent '" + pm.parentKey + "'.", ex);
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
						_logger.warn("Problems getting Dictionary Compressed SQL Text for parent '" + pm.parentKey + "'.", ex);
					}
				}
			}

			if (text != null && text.length() > 4000)
				text = text.substring(0, 4000);
			_shortRstm.setValueAtWithOverride(text, r, pos_ParentSqlText);
		}
	}

	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;

		rstm.setDescription(
				"Cheap statements from the statement cache that are executed very often. <br>" +
				"<br>" +
				"<b>N+1</b> is when the application first runs one <i>parent</i> statement that returns N rows, and then runs one <i>child</i> statement <b>for each row</b>. <br>" +
				"Each child call is fast inside ASE, but the application pays one network round trip per call (see <code>EstRoundTripMs</code>). <br>" +
				"<br>" +
				"<code>NPlusOne = YES</code> means: another statement's rows per sample follow this statement's executions per sample " +
				"(correlation at or above " + Configuration.getCombinedConfiguration().getDoubleProperty(NPlusOneParent.PROPKEY_minCorrelation, NPlusOneParent.DEFAULT_minCorrelation) + "), " +
				"with about one execution per parent row, and several executions per parent execution (the parent returns many rows). That statement is shown as the <i>parent</i>. <br>" +
				"A child can be the parent of the next level (for example: orders -&gt; order lines -&gt; product names). <br>" +
				"ASE has no 'rows returned' counter per statement: the rows are estimated as <code>AvgQualifyingReadRows</code> * executions. <br>" +
				"<br>" +
				"SQL Source table is 'master..monCachedStatement' (needs 'enable stmt cache monitoring'; prepared statements are only there when 'streamlined dynamic SQL' is 1). <br>" +
				"PCS Source table is 'CmChattyStatements_abs'. (PCS = Persistent Counter Store) <br>" +
				"");

		rstm.setColumnDescription("NPlusOne"          , "YES if a parent statement was found (rows of the parent follow executions of this statement).");
		rstm.setColumnDescription("Hashkey"           , "The hashkey over the statement's text (the statement cache has one entry per login, they are grouped on this).");
		rstm.setColumnDescription("ExecCount"         , "Executions during the report period (in the samples where this statement was in the top list).");
		rstm.setColumnDescription("RowCount"          , "Estimated rows during the report period (AvgQualifyingReadRows * executions).");
		rstm.setColumnDescription("RowsPerExec"       , "Estimated rows per execution. About 1 for a key lookup.");
		rstm.setColumnDescription("ExecPerSec__max"   , "Highest executions per second in one sample.");
		rstm.setColumnDescription("AvgElapsedMs"      , "Average elapsed time per execution in milliseconds (inside ASE, measured in whole ms).");
		rstm.setColumnDescription("AvgLogicalReads"   , "Average logical reads per execution.");
		rstm.setColumnDescription("ElapsedMs"         , "Total elapsed time in milliseconds (inside ASE).");
		rstm.setColumnDescription("EstRoundTripMs"    , "Estimated time the application spent on round trips: executions * " + CmChattyStatements.PROPKEY_roundTripMs + ".");
		rstm.setColumnDescription("EntryCount__max"   , "Highest number of statement cache entries with this Hashkey in one sample (one per login).");
		rstm.setColumnDescription("SampleCount"       , "Number of samples where this statement was in the top list.");
		rstm.setColumnDescription("ParentCorrelation" , "Correlation between this statement's executions per sample and the parent's rows per sample (1.0 = moves exactly together).");
		rstm.setColumnDescription("ExecPerParentRow"  , "This statement's executions divided by the parent's rows (about 1.0 for N+1).");
		rstm.setColumnDescription("ExecPerParentExec" , "This statement's executions divided by the parent's executions: the 'N' in N+1 (at least " + Configuration.getCombinedConfiguration().getDoubleProperty(NPlusOneParent.PROPKEY_minFanOut, NPlusOneParent.DEFAULT_minFanOut) + " to be reported; statements that just run together in the same transaction have 1.0).");
		rstm.setColumnDescription("ParentHashkey"     , "Hashkey of the parent statement.");
		rstm.setColumnDescription("ParentSqlText"     , "SQL Text of the parent statement.");
	}
}
