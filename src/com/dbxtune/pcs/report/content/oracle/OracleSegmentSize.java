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

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleSegmentSize
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm;

	public OracleSegmentSize(DailySummaryReportAbstract reportingInstance)
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
	}

	@Override
	public String getSubject()
	{
		return "Biggest Tables and Indexes, and how they grow (order by: SIZE_MB, origin: CmSegmentSize / DBA_SEGMENTS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmSegmentSize_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		int topRows = getTopRows();

		// The segments at the LAST sample, and how much the size changed since the FIRST sample
		// Listed: the top ## on size, and the top ## on growth
		String sql = ""
			    + "select * \n"
			    + "from ( \n"
			    + "    select \n"
			    + "         l.[CON_NAME] \n"
			    + "        ,l.[OWNER] \n"
			    + "        ,l.[SEGMENT_NAME] \n"
			    + "        ,l.[PARTITION_NAME] \n"
			    + "        ,l.[SEGMENT_TYPE] \n"
			    + "        ,l.[TABLE_NAME] \n"
			    + "        ,l.[LOB_COLUMN] \n"
			    + "        ,l.[TABLESPACE_NAME] \n"
			    + "        ,row_number() over (order by l.[SIZE_MB] desc)                                        as [rank_size] \n"
			    + "        ,case when l.[SIZE_MB] > f.[SIZE_MB] \n"   // only the segments that did grow has a place in that list
			    + "              then row_number() over (order by l.[SIZE_MB] - coalesce(f.[SIZE_MB], l.[SIZE_MB]) desc) \n"
			    + "         end                                                                                  as [rank_growth] \n"
			    + "        ,l.[SIZE_MB] \n"
			    + "        ,l.[SIZE_MB] - f.[SIZE_MB]                                                            as [SIZE_MB_CHANGE] \n"
			    + "        ,l.[EXTENTS] \n"
			    + "    from [CmSegmentSize_abs] l \n"
			    + "    left outer join [CmSegmentSize_abs] f on  f.[CON_ID]            = l.[CON_ID] \n"
			    + "                                          and f.[OWNER]             = l.[OWNER] \n"
			    + "                                          and f.[SEGMENT_NAME]      = l.[SEGMENT_NAME] \n"
			    + "                                          and f.[PARTITION_NAME]    = l.[PARTITION_NAME] \n"
			    + "                                          and f.[SEGMENT_TYPE]      = l.[SEGMENT_TYPE] \n"
			    + "                                          and f.[SessionSampleTime] = (select min([SessionSampleTime]) from [CmSegmentSize_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "    where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmSegmentSize_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + ") x \n"
			    + "where [rank_size]   <= " + topRows + " \n"
			    + "   or [rank_growth] <= " + topRows + " \n"
			    + "order by [rank_size] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleSegmentSize");

		if (_shortRstm.getRowCount() == 0)
			return;

		// Highlight sort column
		_shortRstm.setHighlightSortColumns("SIZE_MB");

		_shortRstm.setDescription(
				"The biggest segments at the last collector sample, and how much they grew during the period (from the table <code>CmSegmentSize_abs</code>, which is sampled once an hour).<br>" +
				"A segment is listed if it is in the top " + topRows + " on <b>size</b> or on <b>growth</b> (the two <code>rank_</code> columns says where it's placed in each).<br>" +
				"<code>TABLE_NAME</code> is the table that the segment belongs to, also for an index and for a LOB (a LOB segment has a name made by Oracle).<br>" +
				"<b>Note</b>: The collector only records the biggest segments (property <code>CmSegmentSize.sample.topRows</code>). The size is the space that is allocated: rows that are deleted does not make a segment smaller.<br>" +
				"");

		_shortRstm.setColumnDescription("CON_NAME"       , "Container (PDB) name. Empty when the database is not a Container Database, or when the collector is connected to a PDB.");
		_shortRstm.setColumnDescription("OWNER"          , "The schema that owns the segment");
		_shortRstm.setColumnDescription("SEGMENT_NAME"   , "Name of the segment: the name of the table or the index. A LOB has a name made by Oracle, see TABLE_NAME and LOB_COLUMN.");
		_shortRstm.setColumnDescription("PARTITION_NAME" , "Name of the partition, empty if it is not a partition");
		_shortRstm.setColumnDescription("SEGMENT_TYPE"   , "TABLE, INDEX, TABLE PARTITION, INDEX PARTITION, LOBSEGMENT, LOBINDEX, LOB PARTITION, CLUSTER...");
		_shortRstm.setColumnDescription("TABLE_NAME"     , "The table that the segment belongs to: the table itself, the table of an index, or the table of a LOB");
		_shortRstm.setColumnDescription("LOB_COLUMN"     , "For a LOB: the column in TABLE_NAME that holds it");
		_shortRstm.setColumnDescription("TABLESPACE_NAME", "The tablespace that the segment is in");
		_shortRstm.setColumnDescription("rank_size"      , "Place in the list when it's sorted on size (1 = biggest)");
		_shortRstm.setColumnDescription("rank_growth"    , "Place in the list when it's sorted on growth (1 = grew most). Empty = the segment did not grow during the period.");
		_shortRstm.setColumnDescription("SIZE_MB"        , "Space allocated to the segment at the last sample, in MB");
		_shortRstm.setColumnDescription("SIZE_MB_CHANGE" , "How much SIZE_MB changed during the period: last sample - first sample. Empty = the segment was not in the first sample (it is new, or it was not among the biggest then).");
		_shortRstm.setColumnDescription("EXTENTS"        , "Number of extents that the segment has");
	}
}
