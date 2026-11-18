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
package com.dbxtune.pcs.report.content.sqlserver;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IndexAnalysisHtml;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;

public class SqlServerUnusedIndexes
extends SqlServerAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sumRstm;
	private int _totalUnusedIndexCount;
	private int _trustedUnusedIndexCount;  // the counters cover at least _minDays
	private int _minDays;
	private Timestamp _srvStartDate;       // null = not recorded
	private Timestamp _newestSample;

	public SqlServerUnusedIndexes(DailySummaryReportAbstract reportingInstance)
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

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//	}

	@Override
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// How long the counters are valid: dm_db_index_usage_stats is in memory, so 'no reads' only means 'not read since valid_since'
		String minDaysProp = "<code>" + IndexAnalysisHtml.PROPKEY_unused_minDays + "=" + _minDays + "</code>";
		if (_srvStartDate == null || _newestSample == null)
		{
			sb.append("<span style='background-color: yellow'><b>Note</b>: The SQL Server start time is not in the recording (CmSummary), so it is not known how long the counters cover.</span><br>\n");
		}
		else
		{
			double srvDays = (_newestSample.getTime() - _srvStartDate.getTime()) / (24.0 * 3600 * 1000);
			String srvDaysStr = String.format(Locale.US, "%.1f", srvDays);
			sb.append("Counters valid since: <b>" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(_srvStartDate) + "</b> (" + srvDaysStr + " days), when SQL Server was started. ");
			sb.append("For a table that was created later, the counters start at <code>valid_since</code> (create_date).<br>\n");
			if (srvDays < _minDays)
				sb.append("<span style='background-color: yellow'><b>Note</b>: SQL Server has only been running " + srvDaysStr + " days: none of the indexes below can be trusted as unused (at least " + _minDays + " days are needed, property " + minDaysProp + ").</span><br>\n");
		}

		// How many, and how much space
		sb.append(toHtmlTable(_sumRstm));
		if (_trustedUnusedIndexCount < _totalUnusedIndexCount)
			sb.append("The ones that are not trusted (counters for less than " + _minDays + " days, property " + minDaysProp + ") are last in the list, in gray.<br>\n");
		sb.append("<br>\n");

		sb.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		sb.append(_shortRstm.toHtmlTableString("sortable", true, true, null, new ReportEntryTableStringRenderer()
		{
			@Override
			public String tagTrAttr(ResultSetTableModel rstm, int row)
			{
				// Gray: the counters are too young to trust 'unused'
				Object validDays = rstm.getValueAsObject(row, "valid_days");
				if ( ! (validDays instanceof Number) || ((Number)validDays).doubleValue() < _minDays )
					return "style='color:gray;'";
				return super.tagTrAttr(rstm, row);
			}
		}));
	}

	@Override
	public String getSubject()
	{
		return "Top Unused Indexes (origin: CmIndexUnused)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmIndexUnused_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows();
		_minDays = Configuration.getCombinedConfiguration().getIntProperty(IndexAnalysisHtml.PROPKEY_unused_minDays, IndexAnalysisHtml.DEFAULT_unused_minDays);

		// When was SQL Server started (dm_db_index_usage_stats is in memory: the counters start there)
		boolean hasStartDate = DbUtils.getColumnNamesNoThrow(conn, null, "CmSummary_abs").contains("StartDate");
		String  srvStartSql  = hasStartDate ? "(select max([StartDate]) from [CmSummary_abs])" : "cast(NULL as timestamp)";
		String  sql          = "select " + srvStartSql + ", (select max([SessionSampleTime]) from [CmIndexUnused_abs])";

		sql = conn.quotifySqlString(sql);
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_srvStartDate = rs.getTimestamp(1);
				_newestSample = rs.getTimestamp(2);
			}
		}
		catch(SQLException ignore) {}

		// The counters of an index start when SQL Server was started, or when the table was created (if later). Unknown start time = NULL (not trusted)
		boolean hasCreateDate  = DbUtils.getColumnNamesNoThrow(conn, null, "CmIndexUnused_abs").contains("create_date");
		// coalesce: greatest() is NULL if one of them is NULL (H2 2.5)
		String  validSinceExpr = hasCreateDate ? "case when " + srvStartSql + " is null then NULL else greatest(" + srvStartSql + ", coalesce(x.[create_date], " + srvStartSql + ")) end" : srvStartSql;
		String  validDaysExpr  = "cast(datediff(minute, " + validSinceExpr + ", x.[SessionSampleTime]) / 1440.0 as numeric(8,1))";

		// How many are there, how much space and writes do they cost, and how many can be trusted (all of them, not just the top ##)
		sql = ""
			    + "select \n"
			    + "     count(*) as [unused_indexes] \n"
			    + "    ,cast(sum(x.[IndexSizeMB]) as numeric(15,1)) as [unused_size_mb] \n"
			    + "    ,sum(x.[user_updates]) as [rows_written] \n"
			    + "    ,sum(case when " + validDaysExpr + " >= " + _minDays + " then 1 else 0 end) as [trusted_unused] \n"
			    + "from [CmIndexUnused_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUnused_abs]) \n"
			    + "";

		_sumRstm = executeQuery(conn, sql, true, "SqlServerUnusedIndexesSum");
		_totalUnusedIndexCount   = _sumRstm.getRowCount() == 0 ? 0 : _sumRstm.getValueAsInteger(0, "unused_indexes", true, 0);
		_trustedUnusedIndexCount = _sumRstm.getRowCount() == 0 ? 0 : _sumRstm.getValueAsInteger(0, "trusted_unused", true, 0);
		_sumRstm.setColumnDescription("unused_indexes", "Number of nonclustered indexes (not primary key, not unique constraint) with no seeks, scans or lookups since valid_since");
		_sumRstm.setColumnDescription("unused_size_mb", "The space they take, in MB");
		_sumRstm.setColumnDescription("rows_written"  , "Updates (user_updates: insert/update/delete) to them since valid_since: maintained for nothing");
		_sumRstm.setColumnDescription("trusted_unused", "How many of them have counters for at least " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): the ones you can trust");

		sql = ""
			    + "select top " + topRows + " \n"
			    + "     x.* \n"
			    + "    ," + validSinceExpr + " as [valid_since] \n"
			    + "    ," + validDaysExpr  + " as [valid_days] \n"
			    + "from [CmIndexUnused_abs] x \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmIndexUnused_abs]) \n"
			    + "order by case when " + validDaysExpr + " >= " + _minDays + " then 0 else 1 end, [user_updates] desc \n" // the ones that can be trusted first
			    + "";
		
		_shortRstm = executeQuery(conn, sql, true, "CmIndexUnused_abs");

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("user_updates");

		// Remove some columns which we don't really need
		_shortRstm.removeColumnNoCase("SessionStartTime");
		_shortRstm.removeColumnNoCase("SessionSampleTime");
//		_shortRstm.removeColumnNoCase("CmSampleTime");
		_shortRstm.removeColumnNoCase("CmSampleMs");
		_shortRstm.removeColumnNoCase("CmNewDiffRateRow");  // This was changed into "CmRowState"
		_shortRstm.removeColumnNoCase("CmRowState");

		// CreateStatement (the ROLLBACK of the drop): the collector's text has 'DATA_COMPRESSION = ?', so use the DDL Storage, same as the Index Analysis rollback
		int pos_CreateStatement = _shortRstm.findColumn("CreateStatement");
		if (pos_CreateStatement >= 0)
		{
			for (int r = 0; r < _shortRstm.getRowCount(); r++)
			{
				String dbName    = _shortRstm.getValueAsString(r, "DbName");
				String schema    = _shortRstm.getValueAsString(r, "SchemaName");
				String tabName   = _shortRstm.getValueAsString(r, "TableName");
				String indexName = _shortRstm.getValueAsString(r, "IndexName");

				String ddl = "";
				try
				{
					Set<SqlServerTableInfo> tiSet = getTableInformationFromMonDdlStorage(conn, dbName, schema, tabName);
					if (tiSet != null && ! tiSet.isEmpty())
						ddl = getIndexRestoreDdl(tiSet.iterator().next(), indexName);
				}
				catch (SQLException ignore) {}

				// Not in the DDL Storage: the collector's text
				if (StringUtil.isNullOrBlank(ddl))
					ddl = StringUtil.nullToValue(_shortRstm.getValueAsString(r, "CreateStatement"), "");

				// Compression not known ('?') or not the same for all partitions ('MIXED'): leave it out (= NONE), so it can be run, and say so
				if (ddl.contains("DATA_COMPRESSION = ?, ") || ddl.contains("DATA_COMPRESSION = MIXED, "))
					ddl = ddl.replace("DATA_COMPRESSION = ?, ", "").replace("DATA_COMPRESSION = MIXED, ", "") + " /* DATA_COMPRESSION was not known: add it if the index was compressed */";

				// The stored DDL names the table without the database: run it in that database
				if (StringUtil.hasValue(ddl) && ! ddl.contains("[" + dbName + "]."))
					ddl = "USE [" + dbName + "]; " + ddl;

				_shortRstm.setValueAtWithOverride(ddl, r, pos_CreateStatement);
			}
		}

		// Describe the table
		setSectionDescription(_shortRstm);
	}

	/**
	 * Set descriptions for the table, and the columns
	 */
	private void setSectionDescription(ResultSetTableModel rstm)
	{
		if (rstm == null)
			return;
		
		// Section description
		rstm.setDescription(
				"Information from last collector sample from the table <code>CmIndexUnused_abs</code><br>" +
				"The counters (sys.dm_db_index_usage_stats) are kept in memory: they start when SQL Server is started, and again when the database is taken offline, detached, restored or auto-closed " +
				"(on older SQL Server 2012/2014 builds also when the index is rebuilt). " +
				"So <i>no reads</i> only means <i>not read since <code>valid_since</code></i>. " +
				"A job that only runs at month end or year end may still need the index.<br>" +
				"");
		rstm.setColumnDescription("DropStatement"  , "The DROP INDEX. Save the CreateStatement first: it is the only way back if the index was needed");
		rstm.setColumnDescription("CreateStatement", "The CREATE INDEX that gives the index back (the rollback of the DROP): from the DDL Storage, same as the Index Analysis rollback (options, filegroup). Not in the DDL Storage: the collector's text, without DATA_COMPRESSION");
		rstm.setColumnDescription("valid_since", "When the counters of this index started: when SQL Server was started, or when the table was created (create_date) if that is later");
		rstm.setColumnDescription("valid_days" , "Days the counters cover (valid_since to the last sample). Under " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): too short to trust, shown in gray");
	}
}

