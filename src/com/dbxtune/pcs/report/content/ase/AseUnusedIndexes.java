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
package com.dbxtune.pcs.report.content.ase;

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

public class AseUnusedIndexes
extends AseAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private ResultSetTableModel _shortRstm;
	private ResultSetTableModel _sumRstm;
	private int _totalUnusedIndexCount;
	private int _trustedUnusedIndexCount;  // the counters cover at least _minDays
	private int _minDays;
	private Timestamp _srvStartDate;       // null = not recorded
	private Timestamp _newestSample;

	public AseUnusedIndexes(DailySummaryReportAbstract reportingInstance)
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
	public void writeMessageText(Writer sb, MessageType messageType)
	throws IOException
	{
		// Get a description of this section, and column names
		sb.append(getSectionDescriptionHtml(_shortRstm, true));

		// How long the counters are valid: they are in memory, so 'UsedCount = 0' only means 'not used since valid_since'
		String minDaysProp = "<code>" + IndexAnalysisHtml.PROPKEY_unused_minDays + "=" + _minDays + "</code>";
		if (_srvStartDate == null || _newestSample == null)
		{
			sb.append("<span style='background-color: yellow'><b>Note</b>: The ASE start time is not in the recording (CmSummary), so it is not known how long the counters cover.</span><br>\n");
		}
		else
		{
			double srvDays = (_newestSample.getTime() - _srvStartDate.getTime()) / (24.0 * 3600 * 1000);
			String srvDaysStr = String.format(Locale.US, "%.1f", srvDays);
			sb.append("Counters valid since: <b>" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(_srvStartDate) + "</b> (" + srvDaysStr + " days), when ASE was started. ");
			sb.append("For a table that was read into the metadata cache later, the counters start at <code>valid_since</code> (ObjectCacheDate).<br>\n");
			if (srvDays < _minDays)
				sb.append("<span style='background-color: yellow'><b>Note</b>: ASE has only been running " + srvDaysStr + " days: none of the indexes below can be trusted as unused (at least " + _minDays + " days are needed, property " + minDaysProp + ").</span><br>\n");
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

			@Override
			public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
			{
				// The scripts are several lines ('go' on a line of its own)
				if ("DropStatement".equals(colName) || "CreateStatement".equals(colName))
					return strVal == null ? null : strVal.replace("\n", "<br>");
				return super.cellValue(rstm, row, col, colName, objVal, strVal);
			}
		}));
	}

	@Override
	public String getSubject()
	{
		return "Top Unused Indexes, ordered by 'RowsInsUpdDel' (origin: CmObjectActivity)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}


	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmObjectActivity_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
//		int topRows = localConf.getIntProperty(this.getClass().getSimpleName()+".top", 20);
		int topRows = getTopRows() * 2;
		_minDays = Configuration.getCombinedConfiguration().getIntProperty(IndexAnalysisHtml.PROPKEY_unused_minDays, IndexAnalysisHtml.DEFAULT_unused_minDays);

		String sql = "";

		// When was ASE started (the counters in monOpenObjectActivity are in memory: they start there)
		boolean hasStartDate = DbUtils.getColumnNamesNoThrow(conn, null, "CmSummary_abs").contains("StartDate");
		String  srvStartSql  = hasStartDate ? "(select max([StartDate]) from [CmSummary_abs])" : "cast(NULL as timestamp)";
		sql = "select " + srvStartSql + ", (select max([SessionSampleTime]) from [CmObjectActivity_abs])";

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

		// The counters of an index start when its table was read into the metadata cache (ObjectCacheDate, ASE 15.7 and above), else when ASE was started
		boolean hasCacheDate   = DbUtils.getColumnNamesNoThrow(conn, null, "CmObjectActivity_abs").contains("ObjectCacheDate");
		String  validSinceExpr = hasCacheDate ? "coalesce([ObjectCacheDate], " + srvStartSql + ")" : srvStartSql;
		String  validDaysExpr  = "cast(datediff(minute, " + validSinceExpr + ", [SessionSampleTime]) / 1440.0 as numeric(8,1))";

		// How many are there, how much space and writes do they cost, and how many can be trusted (all of them, not just the top ##)
		sql = ""
			    + "select \n"
			    + "     count(*) as [unused_indexes] \n"
			    + "    ,cast(sum(case when [UsageInMb] >= 0 then [UsageInMb] else 0 end) as numeric(15,1)) as [unused_size_mb] \n"
			    + "    ,sum([RowsInsUpdDel]) as [rows_written] \n"
			    + "    ,sum(case when " + validDaysExpr + " >= " + _minDays + " then 1 else 0 end) as [trusted_unused] \n"
			    + "    ,(select count(*) from [CmObjectActivity_abs] x where x.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmObjectActivity_abs]) and x.[IndexID] between 1 and 254) as [recorded_indexes] \n"
			    + "from [CmObjectActivity_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmObjectActivity_abs]) \n"
			    + "  and [IndexID] between 1 and 254 \n" // 255 = the text/image (LOB) storage of the table, not an index
//			    + "  and [OptSelectCount] = 0 \n"
			    + "  and [UsedCount]      = 0 \n"
//			    + "  and [Operations]     = 0 \n"
			    + "";

		_sumRstm = executeQuery(conn, sql, true, "AseUnusedIndexesSum");
		_totalUnusedIndexCount   = _sumRstm.getRowCount() == 0 ? 0 : _sumRstm.getValueAsInteger(0, "unused_indexes", true, 0);
		_trustedUnusedIndexCount = _sumRstm.getRowCount() == 0 ? 0 : _sumRstm.getValueAsInteger(0, "trusted_unused", true, 0);
		_sumRstm.setColumnDescription("unused_indexes"  , "Number of indexes with UsedCount = 0 at the last sample");
		_sumRstm.setColumnDescription("unused_size_mb"  , "The space they take, in MB (indexes without a known size are not counted)");
		_sumRstm.setColumnDescription("rows_written"    , "Rows inserted + updated + deleted (RowsInsUpdDel) in them since valid_since: maintained for nothing");
		_sumRstm.setColumnDescription("trusted_unused"  , "How many of them have counters for at least " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): the ones you can trust");
		_sumRstm.setColumnDescription("recorded_indexes", "Number of indexes the collector recorded at the last sample (monOpenObjectActivity only has the objects in the metadata cache)");

		// get TOP # unused indexes
		sql = ""
			    + "select top " + topRows + " \n"
			    + "     [DBName] \n"
			    + "    ,[ObjectName] \n"
			    + "    ,NULL as [SchemaName] \n"
			    + "    ,[IndexName] \n"
			    + "    ,[IndexID] \n"
			    + "    ,NULL as [CreateDate] \n"
			    + "    ," + validSinceExpr + " as [valid_since] \n"
			    + "    ," + validDaysExpr  + " as [valid_days] \n"
			    + "    ,[RowsInsUpdDel] \n"
			    + "    ,[OptSelectCount] \n"
			    + "    ,[UsedCount] \n"
			    + "    ,[Operations] \n"
			    + " \n"
			    + "    ,[UsageInMb]        as [IndexSizeMB] -- if: -1 get from DDL Store \n"
			    + "    ,[TabRowCount]      as [TabRowCount] -- if: -1 get from DDL Store \n"
			    + "    ,cast(-1 as bigint) as [TableSizeMB] \n"
//			    + "    ,'DROP INDEX [' || [ObjectName] || '].[' || [IndexName] || ']' as [DropStatement] \n"
			    + "    ,'' as [DropStatement] \n"
			    + "    ,'' as [CreateStatement] \n" // the ROLLBACK of the drop
			    + " \n"
			    + "from [CmObjectActivity_abs] \n"
			    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmObjectActivity_abs]) \n"
			    + "  and [IndexID] between 1 and 254 \n" // 255 = the text/image (LOB) storage of the table, not an index
//			    + "  and [OptSelectCount] = 0 \n"
			    + "  and [UsedCount]      = 0 \n"
//			    + "  and [Operations]     = 0 \n"
			    + "order by case when " + validDaysExpr + " >= " + _minDays + " then 0 else 1 end, [RowsInsUpdDel] desc \n" // the ones that can be trusted first
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "CmObjectActivity_abs");

		// Go and get some information from the DDL Storage
		if ( ! _shortRstm.isEmpty() )
		{
			int pos_SchemaName    = _shortRstm.findColumn("SchemaName");
			int pos_CreateDate    = _shortRstm.findColumn("CreateDate");
			int pos_IndexSizeMB   = _shortRstm.findColumn("IndexSizeMB");
			int pos_TabRowCount   = _shortRstm.findColumn("TabRowCount");
			int pos_TableSizeMB   = _shortRstm.findColumn("TableSizeMB");
			int pos_DropStatement   = _shortRstm.findColumn("DropStatement");
			int pos_CreateStatement = _shortRstm.findColumn("CreateStatement");

			for (int r=0; r<_shortRstm.getRowCount(); r++)
			{
				String DBName     = _shortRstm.getValueAsString(r, "DBName");
				String ObjectName = _shortRstm.getValueAsString(r, "ObjectName");
				String IndexName  = _shortRstm.getValueAsString(r, "IndexName");

				String problem = "";
				Set<AseTableInfo> aseTableInfoSet = null;
				try	{ aseTableInfoSet = getTableInformationFromMonDdlStorage(conn, DBName, null, ObjectName); }
				catch (SQLException ex) { problem = "ERROR: Mon DDL Storage lookup, Msg: " + ex.getMessage(); }

				if (aseTableInfoSet != null && !aseTableInfoSet.isEmpty())
				{
					AseTableInfo aseTableInfo = aseTableInfoSet.iterator().next(); // Get first entry from iterator (simulating: list.get(0))

					// DROP INDEX only takes 'table.index' (no database or owner): 'use' the database, and 'setuser' to the table owner when it is not dbo
					String owner     = aseTableInfo.getSchemaName();
					boolean isDbo    = StringUtil.isNullOrBlank(owner) || "dbo".equals(owner);
					String dropStmnt = ""
							+ "use " + DBName + "\ngo\n"
							+ (isDbo ? "" : "setuser '" + owner + "'\ngo\n")
							+ "drop index " + ObjectName + "." + IndexName + "\ngo\n"
							+ (isDbo ? "" : "setuser\ngo\n");
					String createStmnt = getIndexRestoreDdl(aseTableInfo, IndexName);
					if (StringUtil.isNullOrBlank(createStmnt))
						createStmnt = "INFO: Index wasn't found in DDL Storage: save it with sp_helpindex before the drop";
					else
						createStmnt = ""
							+ "use " + DBName + "\ngo\n"
							+ (isDbo ? "" : "setuser '" + owner + "'\ngo\n")
							+ createStmnt + "\ngo\n"
							+ (isDbo ? "" : "setuser\ngo\n");

					// Set: CreateDate, SchemaName, TabRowCount, TableSizeMB
					_shortRstm.setValueAtWithOverride(aseTableInfo.getSchemaName(), r, pos_SchemaName);
					_shortRstm.setValueAtWithOverride(aseTableInfo.getRowTotal()  , r, pos_TabRowCount);
					_shortRstm.setValueAtWithOverride(aseTableInfo.getSizeMb()    , r, pos_TableSizeMB);
					_shortRstm.setValueAtWithOverride(dropStmnt                   , r, pos_DropStatement);
					_shortRstm.setValueAtWithOverride(createStmnt                 , r, pos_CreateStatement);

					// Set: IndexSizeMB
					AseIndexInfo indexInfo = aseTableInfo.getIndexInfoForName(IndexName);
					if (indexInfo != null)
					{
						_shortRstm.setValueAtWithOverride(indexInfo.getCreationDate(), r, pos_CreateDate);
						_shortRstm.setValueAtWithOverride(indexInfo.getSizeMb()      , r, pos_IndexSizeMB);
					}
				}
				else
				{
					if (StringUtil.isNullOrBlank(problem))
						problem = "INFO: Object wasn't found in DDL Storage";
					
					_shortRstm.setValueAtWithOverride(problem, r, pos_DropStatement);
					_shortRstm.setValueAtWithOverride("No CREATE INDEX to roll back with: save it with sp_helpindex before a drop", r, pos_CreateStatement);
				}
			}
		}
		
		// Highlight sort column
		_shortRstm.setHighlightSortColumns("RowsInsUpdDel");

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
				"Information from last collector sample from the table <code>CmObjectActivity_abs</code><br>" +
				"The counters (monOpenObjectActivity) are kept in memory: they start when ASE is started, and again when the table is read into the metadata cache. " +
				"So <code>UsedCount = 0</code> only means <i>not used since <code>valid_since</code></i>. " +
				"A job that only runs at month end or year end may still need the index.<br>" +
				"");
		rstm.setColumnDescription("DropStatement"  , "The DROP INDEX. Save the CreateStatement first: it is the only way back if the index was needed");
		rstm.setColumnDescription("CreateStatement", "The CREATE INDEX that gives the index back (the rollback of the DROP): keys, options and segment, from the DDL Storage (sp_help) captured by the collector");
		rstm.setColumnDescription("valid_since","When the counters of this index started: when the table was read into the metadata cache (ObjectCacheDate, ASE 15.7 and above), else when ASE was started");
		rstm.setColumnDescription("valid_days" , "Days the counters cover (valid_since to the last sample). Under " + _minDays + " days (property " + IndexAnalysisHtml.PROPKEY_unused_minDays + "): too short to trust, shown in gray");
	}
}

