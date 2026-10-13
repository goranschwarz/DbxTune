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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.oracle.CmSegmentStats;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.inspection.ObjectLookupInspectorOracle;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.pcs.report.content.SparklineHelper;
import com.dbxtune.pcs.report.content.SparklineHelper.DataSource;
import com.dbxtune.pcs.report.content.SparklineHelper.SparkLineParams;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class OracleTopSegments
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm;
	private List<String>        _miniChartJsList = new ArrayList<>();
	private String              _tableInfoHtml   = "";

	public OracleTopSegments(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm.getRowCount() == 0)
		{
			w.append("No rows found <br>\n");
			return;
		}

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "&emsp;&emsp; To change number of <i>top</i> records, set property <code>" + getTopRowsPropertyName() + "=##</code><br>\n");
		w.append(toHtmlTable(_shortRstm));

		// What the tables look like: columns, statistics, sizes, and every index with how it is used (the same block as under a statement in "Top SQL Statements")
		if (isFullMessageType() && StringUtil.hasValue(_tableInfoHtml))
		{
			boolean detailsOpen = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_TABLE_INFO_DETAILS_OPEN, DEFAULT_TABLE_INFO_DETAILS_OPEN);

			w.append("<br>\n");
			w.append("<details class='dsr-details-tableinfo'" + (detailsOpen ? " open" : "") + "> \n");
			w.append("<summary>Show/Hide Table information for the tables above (and for the tables of the indexes above)</summary> \n");
			w.append(_tableInfoHtml);
			w.append("</details> \n");
			w.append("<br>\n");
		}

		if (_CmSegmentStats_SegLogicalReads != null || _CmSegmentStats_SegPhysicalReads != null)
		{
			w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are the 10 segments with most reads, sample by sample.",
					"CmSegmentStats_SegLogicalReads",
					"CmSegmentStats_SegPhysicalReads"
					));
		}

		if (_CmSegmentStats_SegLogicalReads  != null) _CmSegmentStats_SegLogicalReads .writeHtmlContent(w, null, null);
		if (_CmSegmentStats_SegPhysicalReads != null) _CmSegmentStats_SegPhysicalReads.writeHtmlContent(w, null, null);

		// Write JavaScript code for the mini charts
		if (isFullMessageType())
		{
			for (String str : _miniChartJsList)
				w.append(str);
		}
	}

	@Override
	public String getSubject()
	{
		return "Top Tables and Indexes (order by: LOGICAL_READS, origin: CmSegmentStats / V$SEGSTAT)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSegmentStats_diff" };
	}

	@Override
	public List<ReportingIndexEntry> getReportingIndexes()
	{
		List<ReportingIndexEntry> list = new ArrayList<>();

		// For the "Table Information": how the tables and their indexes were used during the recording (see OracleAbstract.readSegmentActivity)
		list.add(new ReportingIndexEntry("CmSegmentStats_diff", "OWNER", "OBJECT_NAME"));
		list.add(new ReportingIndexEntry("CmSegmentStats_abs" , "OWNER", "OBJECT_NAME", "SessionSampleTime"));

		return list;
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema  = getReportingInstance().getDbmsSchemaName();
		int    topRows = getTopRows();

		// A segment that is seen for the first time has it's counters "since the instance was started" (and not "since the previous sample"): do not count those rows
		String sql_and_skipNewDiffRateRows = "  and " + conn.toBitAnd("[CmRowState]", CountersModel.ROW_STATE__IS_DIFF_OR_RATE_ROW) + " = 0 \n";

		// The segments that are in the top ## on: logical reads, physical reads, block changes or row lock waits
		String sql = getCmDiffColumnsAsSqlComment("CmSegmentStats")
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         max([CON_NAME])                                              as [CON_NAME] \n"
			    + "        ,[CON_ID] \n"
			    + "        ,[OWNER] \n"
			    + "        ,[OBJECT_NAME] \n"
			    + "        ,[SUBOBJECT_NAME] \n"
			    + "        ,max([OBJECT_TYPE])                                           as [OBJECT_TYPE] \n"
			    + "        ,max([TABLESPACE_NAME])                                       as [TABLESPACE_NAME] \n"
			    + " \n"
			    + "        ,row_number() over (order by sum([LOGICAL_READS])    desc)    as [rank_logical_reads] \n"
			    + "        ,row_number() over (order by sum([PHYSICAL_READS])   desc)    as [rank_physical_reads] \n"
			    + "        ,row_number() over (order by sum([DB_BLOCK_CHANGES]) desc)    as [rank_block_changes] \n"
			    + "        ,row_number() over (order by sum([ROW_LOCK_WAITS])   desc)    as [rank_row_lock_waits] \n"
			    + " \n"
			    + "        ,cast('' as varchar(512))                                     as [LOGICAL_READS__chart] \n"
			    + "        ,sum([LOGICAL_READS])                                         as [LOGICAL_READS__sum] \n"
			    + "        ,cast('' as varchar(512))                                     as [PHYSICAL_READS__chart] \n"
			    + "        ,sum([PHYSICAL_READS])                                        as [PHYSICAL_READS__sum] \n"
			    + "        ,sum([PHYSICAL_READS_DIRECT])                                 as [PHYSICAL_READS_DIRECT__sum] \n"
			    + "        ,cast('' as varchar(512))                                     as [SEGMENT_SCANS__chart] \n"
			    + "        ,sum([SEGMENT_SCANS])                                         as [SEGMENT_SCANS__sum] \n"
			    + "        ,cast('' as varchar(512))                                     as [DB_BLOCK_CHANGES__chart] \n"
			    + "        ,sum([DB_BLOCK_CHANGES])                                      as [DB_BLOCK_CHANGES__sum] \n"
			    + "        ,sum([PHYSICAL_WRITES])                                       as [PHYSICAL_WRITES__sum] \n"
			    + "        ,cast('' as varchar(512))                                     as [ROW_LOCK_WAITS__chart] \n"
			    + "        ,sum([ROW_LOCK_WAITS])                                        as [ROW_LOCK_WAITS__sum] \n"
			    + "        ,sum([ITL_WAITS])                                             as [ITL_WAITS__sum] \n"
			    + "        ,sum([BUFFER_BUSY_WAITS])                                     as [BUFFER_BUSY_WAITS__sum] \n"
			    + " \n"
			    + "        ,count(distinct [SessionSampleTime])                          as [samples_count] \n"
			    + "    from [CmSegmentStats_diff] \n"
			    + "    where [OBJECT_NAME] is not null \n"   // not the dropped objects and the temporary segments
			    + "  " + sql_and_skipNewDiffRateRows
			    + "  " + getReportPeriodSqlWhere()
			    + "    group by [CON_ID], [OWNER], [OBJECT_NAME], [SUBOBJECT_NAME] \n"
			    + ") x \n"
			    + "where  ([rank_logical_reads]  <= " + topRows + " and [LOGICAL_READS__sum]    > 0) \n"
			    + "   or  ([rank_physical_reads] <= " + topRows + " and [PHYSICAL_READS__sum]   > 0) \n"
			    + "   or  ([rank_block_changes]  <= " + topRows + " and [DB_BLOCK_CHANGES__sum] > 0) \n"
			    + "   or  ([rank_row_lock_waits] <= " + topRows + " and [ROW_LOCK_WAITS__sum]   > 0) \n"
			    + "order by [rank_logical_reads] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleTopSegments");

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("LOGICAL_READS__sum");

		_shortRstm.setDescription(
				"The tables and indexes that were used most during the period (from the table <code>CmSegmentStats_diff</code>).<br>" +
				"A segment is listed if it is in the top " + topRows + " on <b>logical reads</b>, <b>physical reads</b>, <b>block changes</b> or <b>row lock waits</b> (the four <code>rank_</code> columns says where it's placed in each).<br>" +
				"What to look for: a table with many <code>SEGMENT_SCANS</code> and <code>PHYSICAL_READS</code> (it is scanned, and it does not fit in memory), and tables with <code>ROW_LOCK_WAITS</code> (sessions that wait for each other).<br>" +
				"<b>Note</b>: The collector only records the segments with most reads and writes since the instance was started (property <code>CmSegmentStats.sample.topRows</code>), " +
				"and the first sample where a segment is seen is not counted. Dropped objects and temporary segments are not listed.<br>" +
				"");

		_shortRstm.setColumnDescription("CON_NAME"                  , "Container (PDB) name. Empty when the database is not a Container Database, or when the collector is connected to a PDB.");
		_shortRstm.setColumnDescription("CON_ID"                    , "Container ID. 0 when the database is not a Container Database, or when the collector is connected to a PDB.");
		_shortRstm.setColumnDescription("LOGICAL_READS__chart"      , "Chart: logical reads, in 10 minute chunks\nSo you can see WHEN the segment was read.");
		_shortRstm.setColumnDescription("PHYSICAL_READS__chart"     , "Chart: physical reads, in 10 minute chunks");
		_shortRstm.setColumnDescription("SEGMENT_SCANS__chart"      , "Chart: number of full scans, in 10 minute chunks");
		_shortRstm.setColumnDescription("DB_BLOCK_CHANGES__chart"   , "Chart: block changes, in 10 minute chunks\nSo you can see WHEN the segment was changed.");
		_shortRstm.setColumnDescription("ROW_LOCK_WAITS__chart"     , "Chart: row lock waits, in 10 minute chunks\nSo you can see WHEN sessions waited for each other.");
		_shortRstm.setColumnDescription("OWNER"                     , "The schema that owns the object");
		_shortRstm.setColumnDescription("OBJECT_NAME"               , "Name of the table, index, LOB or cluster");
		_shortRstm.setColumnDescription("SUBOBJECT_NAME"            , "Name of the partition, empty if it is not a partition");
		_shortRstm.setColumnDescription("OBJECT_TYPE"               , "TABLE, INDEX, TABLE PARTITION, INDEX PARTITION, LOB, CLUSTER (a segment shared by several tables, like SYS.C_OBJ#)...");
		_shortRstm.setColumnDescription("TABLESPACE_NAME"           , "The tablespace that the segment is in");
		_shortRstm.setColumnDescription("rank_logical_reads"        , "Place in the list when it's sorted on logical reads (1 = most)");
		_shortRstm.setColumnDescription("rank_physical_reads"       , "Place in the list when it's sorted on physical reads (1 = most)");
		_shortRstm.setColumnDescription("rank_block_changes"        , "Place in the list when it's sorted on block changes (1 = most)");
		_shortRstm.setColumnDescription("rank_row_lock_waits"       , "Place in the list when it's sorted on row lock waits (1 = most)");
		_shortRstm.setColumnDescription("LOGICAL_READS__sum"        , "Blocks read from the segment: from memory (the buffer cache) or from disk. This is what costs CPU.");
		_shortRstm.setColumnDescription("PHYSICAL_READS__sum"       , "Blocks read from disk");
		_shortRstm.setColumnDescription("PHYSICAL_READS_DIRECT__sum", "Blocks read from disk that did not go through the buffer cache (big scans, parallel query). They are also in PHYSICAL_READS.");
		_shortRstm.setColumnDescription("SEGMENT_SCANS__sum"        , "Number of times the whole segment was scanned (full table scan, or index fast full scan)");
		_shortRstm.setColumnDescription("DB_BLOCK_CHANGES__sum"     , "Changes made to blocks of the segment (INSERT, UPDATE, DELETE). This is what makes redo.");
		_shortRstm.setColumnDescription("PHYSICAL_WRITES__sum"      , "Blocks written to disk");
		_shortRstm.setColumnDescription("ROW_LOCK_WAITS__sum"       , "Number of times a session had to wait for a row that was locked by another session");
		_shortRstm.setColumnDescription("ITL_WAITS__sum"            , "Number of times a session had to wait for a transaction slot in a block (fix: a higher INITRANS)");
		_shortRstm.setColumnDescription("BUFFER_BUSY_WAITS__sum"    , "Number of times a session had to wait for a block that another session was reading in or changing");
		_shortRstm.setColumnDescription("samples_count"             , "In how many collector samples the segment was recorded");

		//--------------------------------------------------------------------------------
		// Mini Charts: WHEN during the period. One per row and measure (a NULL partition name is OK: the SQL then says "is null")
		//--------------------------------------------------------------------------------
		for (String measure : new String[] { "LOGICAL_READS", "PHYSICAL_READS", "SEGMENT_SCANS", "DB_BLOCK_CHANGES", "ROW_LOCK_WAITS" })
		{
			_miniChartJsList.add(SparklineHelper.createSparkline(conn, this, _shortRstm,
					SparkLineParams.create       (DataSource.CounterModel)
					.setHtmlChartColumnName      (measure + "__chart")
					.setHtmlWhereKeyColumnName   ("CON_ID, OWNER, OBJECT_NAME, SUBOBJECT_NAME")
					.setDbmsTableName            ("CmSegmentStats_diff")
					.setDbmsSampleTimeColumnName ("SessionSampleTime")
					.setDbmsDataValueColumnName  (measure)
					.setDbmsWhereKeyColumnName   ("CON_ID, OWNER, OBJECT_NAME, SUBOBJECT_NAME")
					.setDbmsExtraWhereClause     (sql_and_skipNewDiffRateRows.trim())
					.setSparklineTooltipPostfix  ("'" + measure + "' in below period")
					.validate()));
		}

		//--------------------------------------------------------------------------------
		// Table Information: what the tables above (and the tables of the indexes above) look like, from the DDL Storage
		// (CmSegmentStats asks for a lookup of the most used segments, see CmSegmentStats.sendDdlDetailsRequest)
		// One lookup per container: in a recording from CDB$ROOT the segments can be in different containers
		//--------------------------------------------------------------------------------
		Map<String, Set<String>> namesPerContainer = new LinkedHashMap<>();
		for (int r = 0; r < _shortRstm.getRowCount(); r++)
		{
			String type = StringUtil.nullToValue(_shortRstm.getValueAsString(r, "OBJECT_TYPE"), "");
			String name = _shortRstm.getValueAsString(r, "OWNER") + "." + _shortRstm.getValueAsString(r, "OBJECT_NAME");

			// Not a LOB or a CLUSTER, and not Oracle's own objects (they are not looked up)
			if ( ! (type.startsWith("TABLE") || type.startsWith("INDEX")) || ObjectLookupInspectorOracle.isDictionaryObject(name) )
				continue;

			namesPerContainer.computeIfAbsent(StringUtil.nullToValue(_shortRstm.getValueAsString(r, "CON_NAME"), ""), k -> new LinkedHashSet<>()).add(name);
		}
		for (Entry<String, Set<String>> entry : namesPerContainer.entrySet())
			_tableInfoHtml += getDbmsTableInfoAsHtmlTable(conn, entry.getKey(), entry.getValue(), true, "dsr-sub-table-tableinfo");

		// The graphs: only if they were recorded
		if (doTableExist(conn, schema, CmSegmentStats.CM_NAME + "_" + CmSegmentStats.GRAPH_NAME_LOGICAL_READS))
			_CmSegmentStats_SegLogicalReads  = createTsLineChart(conn, schema, CmSegmentStats.CM_NAME, CmSegmentStats.GRAPH_NAME_LOGICAL_READS,  -1, true, null, "Logical Reads (blocks) per Second, for the Top 10 Segments ("  + CmSegmentStats.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmSegmentStats.CM_NAME + "_" + CmSegmentStats.GRAPH_NAME_PHYSICAL_READS))
			_CmSegmentStats_SegPhysicalReads = createTsLineChart(conn, schema, CmSegmentStats.CM_NAME, CmSegmentStats.GRAPH_NAME_PHYSICAL_READS, -1, true, null, "Physical Reads (blocks) per Second, for the Top 10 Segments (" + CmSegmentStats.SHORT_NAME + ")");
	}

	private IReportChart _CmSegmentStats_SegLogicalReads;  // null = not recorded
	private IReportChart _CmSegmentStats_SegPhysicalReads; // null = not recorded
}
