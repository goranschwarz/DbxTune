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

import com.dbxtune.cm.db2.CmMemory;
import com.dbxtune.cm.db2.CmMonGetBufferPool;
import com.dbxtune.cm.db2.CmTableSpaceAll;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * What the instance used its memory for, and how well the buffer pools worked, during the day (from CmMemory, CmMonGetBufferPool and CmTableSpaceAll). Built on MySqlMemory.
 */
public class Db2Memory
extends Db2Abstract
{
	public Db2Memory(DailySummaryReportAbstract reportingInstance)
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
		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is what the instance used its memory for, and how many reads the buffer pools had to do from disk, during the day.",
				"CmMemory_InstMemPct",
				"CmMemory_MemSetMb",
				"CmMemory_MemPoolMb",
				"CmMonGetBufferPool_BpHitPct",
				"CmMonGetBufferPool_BpPhysReads",
				"CmTableSpaceAll_TbspPhysReads",
				"CmTableSpaceAll_TbspAvgReadMs"
				));

		if (_CmMemory_InstMemPct != null) _CmMemory_InstMemPct.writeHtmlContent(w, null, "<b>Note</b>: When the instance reaches INSTANCE_MEMORY, memory requests fail (SQL0973N, SQL1218N...).");
		if (_CmMemory_MemSetMb   != null) _CmMemory_MemSetMb  .writeHtmlContent(w, null, null);

		if (_CmMonGetBufferPool_BpHitPct    != null) _CmMonGetBufferPool_BpHitPct   .writeHtmlContent(w, null, "<b>Note</b>: Below about 95-99% means that many reads had to go to disk: the buffer pool may be too small for the data that is used.");
		if (_CmMonGetBufferPool_BpPhysReads != null) _CmMonGetBufferPool_BpPhysReads.writeHtmlContent(w, null, null);

		if (isFullMessageType())
		{
			if (_CmMemory_MemPoolMb             != null) _CmMemory_MemPoolMb            .writeHtmlContent(w, null, null);
			if (_CmTableSpaceAll_TbspPhysReads  != null) _CmTableSpaceAll_TbspPhysReads .writeHtmlContent(w, null, null);
			if (_CmTableSpaceAll_TbspAvgReadMs  != null) _CmTableSpaceAll_TbspAvgReadMs .writeHtmlContent(w, null, "<b>Note</b>: More than about 10 ms per read is a slow disk (or a busy one).");
		}
	}

	@Override
	public String getSubject()
	{
		return "Memory and Buffer Pools of the full day (origin: CmMemory, CmMonGetBufferPool, CmTableSpaceAll / MON_GET_MEMORY_SET, MON_GET_MEMORY_POOL, MON_GET_BUFFERPOOL, MON_GET_TABLESPACE)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();

		// Each one only if it was recorded (a CM can be disabled, or not have the privilege)
		if (doTableExist(conn, schema, CmMemory.CM_NAME + "_" + CmMemory.GRAPH_NAME_INST_PCT))
			_CmMemory_InstMemPct = createTsLineChart(conn, schema, CmMemory.CM_NAME, CmMemory.GRAPH_NAME_INST_PCT, 100, false, null, "Instance Memory Used in Percent of INSTANCE_MEMORY (" + CmMemory.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmMemory.CM_NAME + "_" + CmMemory.GRAPH_NAME_SET_MB))
			_CmMemory_MemSetMb = createTsLineChart(conn, schema, CmMemory.CM_NAME, CmMemory.GRAPH_NAME_SET_MB, -1, true, null, "Memory per Memory Set, Committed MB (" + CmMemory.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmMemory.CM_NAME + "_" + CmMemory.GRAPH_NAME_POOL_MB))
			_CmMemory_MemPoolMb = createTsLineChart(conn, schema, CmMemory.CM_NAME, CmMemory.GRAPH_NAME_POOL_MB, -1, true, null, "Memory per Memory Pool Type, Used MB, Top 15 (" + CmMemory.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmMonGetBufferPool.CM_NAME + "_" + CmMonGetBufferPool.GRAPH_NAME_HIT_PCT))
			_CmMonGetBufferPool_BpHitPct = createTsLineChart(conn, schema, CmMonGetBufferPool.CM_NAME, CmMonGetBufferPool.GRAPH_NAME_HIT_PCT, 100, true, null, "Buffer Pool Hit Ratio in Percent, per Buffer Pool (" + CmMonGetBufferPool.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmMonGetBufferPool.CM_NAME + "_" + CmMonGetBufferPool.GRAPH_NAME_P_READS))
			_CmMonGetBufferPool_BpPhysReads = createTsLineChart(conn, schema, CmMonGetBufferPool.CM_NAME, CmMonGetBufferPool.GRAPH_NAME_P_READS, -1, true, null, "Buffer Pool Physical Reads per Second, per Buffer Pool (" + CmMonGetBufferPool.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmTableSpaceAll.CM_NAME + "_" + CmTableSpaceAll.GRAPH_NAME_P_READS))
			_CmTableSpaceAll_TbspPhysReads = createTsLineChart(conn, schema, CmTableSpaceAll.CM_NAME, CmTableSpaceAll.GRAPH_NAME_P_READS, -1, true, null, "Table Space Physical Reads per Second, per Table Space (" + CmTableSpaceAll.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmTableSpaceAll.CM_NAME + "_" + CmTableSpaceAll.GRAPH_NAME_AVG_READ_MS))
			_CmTableSpaceAll_TbspAvgReadMs = createTsLineChart(conn, schema, CmTableSpaceAll.CM_NAME, CmTableSpaceAll.GRAPH_NAME_AVG_READ_MS, -1, true, null, "Table Space Read Time, Average ms per Physical Read, per Table Space (" + CmTableSpaceAll.SHORT_NAME + ")");
	}

	private IReportChart _CmMemory_InstMemPct;            // null = not recorded
	private IReportChart _CmMemory_MemSetMb;              // null = not recorded
	private IReportChart _CmMemory_MemPoolMb;             // null = not recorded
	private IReportChart _CmMonGetBufferPool_BpHitPct;    // null = not recorded
	private IReportChart _CmMonGetBufferPool_BpPhysReads; // null = not recorded
	private IReportChart _CmTableSpaceAll_TbspPhysReads;  // null = not recorded
	private IReportChart _CmTableSpaceAll_TbspAvgReadMs;  // null = not recorded
}
