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

import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.oracle.CmSegmentStats;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleTopSegments
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm;

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

		if (_CmSegmentStats_SegLogicalReads != null || _CmSegmentStats_SegPhysicalReads != null)
		{
			w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are the 10 segments with most reads, sample by sample.",
					"CmSegmentStats_SegLogicalReads",
					"CmSegmentStats_SegPhysicalReads"
					));
		}

		if (_CmSegmentStats_SegLogicalReads  != null) _CmSegmentStats_SegLogicalReads .writeHtmlContent(w, null, null);
		if (_CmSegmentStats_SegPhysicalReads != null) _CmSegmentStats_SegPhysicalReads.writeHtmlContent(w, null, null);
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
			    + "        ,sum([LOGICAL_READS])                                         as [LOGICAL_READS__sum] \n"
			    + "        ,sum([PHYSICAL_READS])                                        as [PHYSICAL_READS__sum] \n"
			    + "        ,sum([PHYSICAL_READS_DIRECT])                                 as [PHYSICAL_READS_DIRECT__sum] \n"
			    + "        ,sum([SEGMENT_SCANS])                                         as [SEGMENT_SCANS__sum] \n"
			    + "        ,sum([DB_BLOCK_CHANGES])                                      as [DB_BLOCK_CHANGES__sum] \n"
			    + "        ,sum([PHYSICAL_WRITES])                                       as [PHYSICAL_WRITES__sum] \n"
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

		// The graphs: only if they were recorded
		if (doTableExist(conn, schema, CmSegmentStats.CM_NAME + "_" + CmSegmentStats.GRAPH_NAME_LOGICAL_READS))
			_CmSegmentStats_SegLogicalReads  = createTsLineChart(conn, schema, CmSegmentStats.CM_NAME, CmSegmentStats.GRAPH_NAME_LOGICAL_READS,  -1, true, null, "Logical Reads (blocks) per Second, for the Top 10 Segments ("  + CmSegmentStats.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmSegmentStats.CM_NAME + "_" + CmSegmentStats.GRAPH_NAME_PHYSICAL_READS))
			_CmSegmentStats_SegPhysicalReads = createTsLineChart(conn, schema, CmSegmentStats.CM_NAME, CmSegmentStats.GRAPH_NAME_PHYSICAL_READS, -1, true, null, "Physical Reads (blocks) per Second, for the Top 10 Segments (" + CmSegmentStats.SHORT_NAME + ")");
	}

	private IReportChart _CmSegmentStats_SegLogicalReads;  // null = not recorded
	private IReportChart _CmSegmentStats_SegPhysicalReads; // null = not recorded
}
