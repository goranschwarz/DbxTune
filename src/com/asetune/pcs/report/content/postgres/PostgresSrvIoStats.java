/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;

import com.asetune.cm.postgres.CmPgIo;
import com.asetune.pcs.report.DailySummaryReportAbstract;
import com.asetune.pcs.report.content.IReportChart;
import com.asetune.pcs.report.content.ase.AseAbstract;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.Configuration;

public class PostgresSrvIoStats 
extends AseAbstract
{
//	private static Logger _logger = Logger.getLogger(AseWaitStats.class);

	public PostgresSrvIoStats(DailySummaryReportAbstract reportingInstance)
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

//	@Override
//	public void writeShortMessageText(Writer w)
//	throws IOException
//	{
//		writeMessageText(w);
//	}

	@Override
	public void writeMessageText(Writer w, MessageType messageType)
	throws IOException
	{
		w.append("IO Statistics for the full day (origin: CmPgIo / pg_stat_io) <br>");
		w.append("See what backend systems are causing IO's and if they are using the buffer pools/caches in a good manner<br>");
		w.append("<br>");
		w.append("Postgres Source table is 'pg_stat_io'. <br>");
		w.append("PCS Source table is 'CmPgIo_diff'. (PCS = Persistent Counter Store) <br>");

//		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are IO Statistics on the Server level.",
//				"CmPgIo_SrvWaitTypeMs",
//				"CmPgIo_SrvWaitMs"
//				));

		if (_CmPgIo_IoCacheHitNormalPct != null) _CmPgIo_IoCacheHitNormalPct.writeHtmlContent(w, null, null);
		if (_CmPgIo_IoCacheHitAllPct    != null) _CmPgIo_IoCacheHitAllPct   .writeHtmlContent(w, null, null);

		if (_CmPgIo_IoHits              != null) _CmPgIo_IoHits             .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoEvections         != null) _CmPgIo_IoEvections        .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoReuses            != null) _CmPgIo_IoReuses           .writeHtmlContent(w, null, null);

		if (_CmPgIo_IoReads             != null) _CmPgIo_IoReads            .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoReadTime          != null) _CmPgIo_IoReadTime         .writeHtmlContent(w, null, null);

		if (_CmPgIo_IoWrites            != null) _CmPgIo_IoWrites           .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoWriteTime         != null) _CmPgIo_IoWriteTime        .writeHtmlContent(w, null, null);

		if (_CmPgIo_IoWritebacks        != null) _CmPgIo_IoWritebacks       .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoWritebackTime     != null) _CmPgIo_IoWritebackTime    .writeHtmlContent(w, null, null);

		if (_CmPgIo_IoExtends           != null) _CmPgIo_IoExtends          .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoExtendTime        != null) _CmPgIo_IoExtendTime       .writeHtmlContent(w, null, null);

		if (_CmPgIo_IoFsyncs            != null) _CmPgIo_IoFsyncs           .writeHtmlContent(w, null, null);
		if (_CmPgIo_IoFsyncTime         != null) _CmPgIo_IoFsyncTime        .writeHtmlContent(w, null, null);
		
		// NOTE: We could also add a normal HTML Table with "sparklines" (mini graphs) in each of the value fields...
	}

	@Override
	public String getSubject()
	{
		return "IO Statistics for the full day (origin: CmPgIo / pg_stat_io)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmPgIo_diff" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{

		String schema = getReportingInstance().getDbmsSchemaName();
		boolean sort = true;

		_CmPgIo_IoCacheHitNormalPct = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_CACHE_HIT_NORMAL_PCT,  -1, sort, null, "Buffer 'Cache Hit' By 'Client Table Access' in Percent ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoCacheHitAllPct    = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_CACHE_HIT_ALL_PCT,     -1, sort, null, "Buffer 'Cache Hit' By 'backend_type:object:context' in Percent ("+CmPgIo.SHORT_NAME+")");

		_CmPgIo_IoHits              = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_HITS,                  -1, sort, null, "Buffer 'Hits' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoEvections         = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_EVICTIONS,             -1, sort, null, "Buffer 'Evictions' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoReuses            = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_REUSES,                -1, sort, null, "Buffer 'Reuses' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");

		_CmPgIo_IoReads             = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_READS,                 -1, sort, null, "IO 'Reads' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoReadTime          = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_READ_TIME,             -1, sort, null, "IO 'Read Time' in ms By 'backend_type:object:context' per Operation ("+CmPgIo.SHORT_NAME+")");

		_CmPgIo_IoWrites            = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_WRITES,                -1, sort, null, "IO 'Writes' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoWriteTime         = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_WRITE_TIME,            -1, sort, null, "IO 'Write Time' in ms By 'backend_type:object:context' per Operation ("+CmPgIo.SHORT_NAME+")");

		_CmPgIo_IoWritebacks        = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_WRITEBACKS,            -1, sort, null, "IO 'Writebacks' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoWritebackTime     = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_WRITEBACK_TIME,        -1, sort, null, "IO 'Writeback Time' in ms By 'backend_type:object:context' per Operation ("+CmPgIo.SHORT_NAME+")");

		_CmPgIo_IoExtends           = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_EXTENDS,               -1, sort, null, "IO 'Extends' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoExtendTime        = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_EXTEND_TIME,           -1, sort, null, "IO 'Extend Time' in ms By 'backend_type:object:context' per Operation ("+CmPgIo.SHORT_NAME+")");

		_CmPgIo_IoFsyncs            = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_FSYNCS,                -1, sort, null, "IO 'FSyncs' By 'backend_type:object:context' per Second ("+CmPgIo.SHORT_NAME+")");
		_CmPgIo_IoFsyncTime         = createTsLineChart(conn, schema, CmPgIo.CM_NAME, CmPgIo.GRAPH_NAME_FSYNC_TIME,            -1, sort, null, "IO 'FSync Time' in ms By 'backend_type:object:context' per Operation ("+CmPgIo.SHORT_NAME+")");
	}

	private IReportChart _CmPgIo_IoCacheHitNormalPct;
	private IReportChart _CmPgIo_IoCacheHitAllPct;

	private IReportChart _CmPgIo_IoHits         ;
	private IReportChart _CmPgIo_IoEvections    ;
	private IReportChart _CmPgIo_IoReuses       ;
	
	private IReportChart _CmPgIo_IoReads        ;
	private IReportChart _CmPgIo_IoReadTime     ;
	
	private IReportChart _CmPgIo_IoWrites       ;
	private IReportChart _CmPgIo_IoWriteTime    ;
	
	private IReportChart _CmPgIo_IoWritebacks   ;
	private IReportChart _CmPgIo_IoWritebackTime;
	
	private IReportChart _CmPgIo_IoExtends      ;
	private IReportChart _CmPgIo_IoExtendTime   ;
	
	private IReportChart _CmPgIo_IoFsyncs       ;
	private IReportChart _CmPgIo_IoFsyncTime    ;
}
