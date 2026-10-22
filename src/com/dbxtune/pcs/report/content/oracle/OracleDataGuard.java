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

import com.dbxtune.cm.oracle.CmDataGuard;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Data Guard: how far behind the standby databases were.
 * <p>
 * NOTE: like CmDataGuard this has NOT been tested against a real standby database, only with made up rows.
 */
public class OracleDataGuard
extends OracleAbstract
{
	private ResultSetTableModel _shortRstm; // null = Data Guard was not recorded

	public OracleDataGuard(DailySummaryReportAbstract reportingInstance)
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
		if (_shortRstm == null || _shortRstm.getRowCount() == 0)
		{
			w.append("<b>Data Guard was not recorded.</b><br>\n");
			w.append("The database does not send redo to a standby database (it has no archive destination with TARGET=STANDBY).<br>\n");
			return;
		}

		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_shortRstm, true));

		w.append("Row Count: " + _shortRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_shortRstm));

		if (_CmDataGuard_DgLagSec != null || _CmDataGuard_DgSeqGap != null)
		{
			w.append(getDbxCentralLinkWithDescForGraphs(false, "Below is how far behind the standby databases were during the day.",
					"CmDataGuard_DgLagSec",
					"CmDataGuard_DgSeqGap"
					));
		}

		if (_CmDataGuard_DgLagSec != null) _CmDataGuard_DgLagSec.writeHtmlContent(w, null, null);
		if (_CmDataGuard_DgSeqGap != null) _CmDataGuard_DgSeqGap.writeHtmlContent(w, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Data Guard: Standby Lag (origin: CmDataGuard / V$ARCHIVE_DEST_STATUS, V$ARCHIVED_LOG)";
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

		// No mandatory table: a database without a standby is the normal case (and something the reader should be told), not a red ERROR
		if ( ! doTableExist(conn, schema, "CmDataGuard_abs") )
			return;

		// The standbys at the LAST sample, with the worst values of the period
		String sql = ""
			    + "select \n"
			    + "     l.[SEEN_FROM] \n"
			    + "    ,l.[DB_UNIQUE_NAME] \n"
			    + "    ,l.[DEST_NAME] \n"
			    + "    ,l.[STATUS] \n"
			    + "    ,l.[GAP_STATUS] \n"
			    + "    ,l.[PRIMARY_SEQ] \n"
			    + "    ,l.[SHIPPED_SEQ] \n"
			    + "    ,l.[APPLIED_SEQ] \n"
			    + "    ,l.[TRANSPORT_LAG_SEC] \n"
			    + "    ,l.[APPLY_LAG_SEC] \n"
			    + "    ,m.[TRANSPORT_LAG_SEC__max] \n"
			    + "    ,m.[APPLY_LAG_SEC__max] \n"
			    + "    ,m.[TRANSPORT_SEQ_GAP__max] \n"
			    + "    ,m.[APPLY_SEQ_GAP__max] \n"
			    + "    ,m.[samples_not_valid] \n"
			    + "    ,m.[samples_count] \n"
			    + "    ,l.[STANDBY_TYPE] \n"
			    + "    ,l.[DATABASE_MODE] \n"
			    + "    ,l.[RECOVERY_MODE] \n"
			    + "    ,l.[PROTECTION_MODE] \n"
			    + "    ,l.[TRANSMIT_MODE] \n"
			    + "    ,l.[DELAY_MINS] \n"
			    + "    ,l.[DESTINATION] \n"
			    + "    ,l.[ERROR] \n"
			    + "from [CmDataGuard_abs] l \n"
			    + "join (select \n"
			    + "           [DEST_ID] \n"
			    + "          ,max([TRANSPORT_LAG_SEC])                                  as [TRANSPORT_LAG_SEC__max] \n"
			    + "          ,max([APPLY_LAG_SEC])                                      as [APPLY_LAG_SEC__max] \n"
			    + "          ,max([TRANSPORT_SEQ_GAP])                                  as [TRANSPORT_SEQ_GAP__max] \n"
			    + "          ,max([APPLY_SEQ_GAP])                                      as [APPLY_SEQ_GAP__max] \n"
			    + "          ,sum(case when [STATUS] <> 'VALID' then 1 else 0 end)      as [samples_not_valid] \n"
			    + "          ,count(*)                                                  as [samples_count] \n"
			    + "      from [CmDataGuard_abs] \n"
			    + "      where 1 = 1 " + getReportPeriodSqlWhere().trim() + " \n"
			    + "      group by [DEST_ID] \n"
			    + "     ) m on m.[DEST_ID] = l.[DEST_ID] \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmDataGuard_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "order by l.[DEST_ID] \n"
			    + "";

		_shortRstm = executeQuery(conn, sql, true, "OracleDataGuard");

		_shortRstm.setDescription(
				"<font color='red'><b>Not tested against a real standby database</b></font>: check the numbers against your standby before you trust them.<br>" +
				"The standby databases at the last collector sample, and the worst values of the period (from the table <code>CmDataGuard_abs</code>).<br>" +
				"Counted on the primary, in whole log files: a standby that has received and applied every complete log is shown as 0 (it can still be in the middle of the log that is being written).<br>" +
				"");

		_shortRstm.setColumnDescription("SEEN_FROM"             , "PRIMARY = what the primary knows about the standby. STANDBY = the collector was connected to the standby (Oracle's own numbers, V$DATAGUARD_STATS).");
		_shortRstm.setColumnDescription("DB_UNIQUE_NAME"        , "The DB_UNIQUE_NAME of the standby database");
		_shortRstm.setColumnDescription("DEST_NAME"             , "The archive destination: LOG_ARCHIVE_DEST_n");
		_shortRstm.setColumnDescription("STATUS"                , "State of the destination at the last sample: VALID, ERROR, DISABLED, DEFERRED...");
		_shortRstm.setColumnDescription("GAP_STATUS"            , "Is any redo missing at the standby: NO GAP, RESOLVABLE GAP, UNRESOLVABLE GAP...");
		_shortRstm.setColumnDescription("PRIMARY_SEQ"           , "The last log sequence that was complete on the primary, at the last sample");
		_shortRstm.setColumnDescription("SHIPPED_SEQ"           , "The last log sequence that the standby had received. Empty = the primary did not know.");
		_shortRstm.setColumnDescription("APPLIED_SEQ"           , "The last log sequence that the standby had applied. Empty = the primary did not know.");
		_shortRstm.setColumnDescription("TRANSPORT_LAG_SEC"     , "At the last sample: how old the newest redo was that the standby had received, in seconds");
		_shortRstm.setColumnDescription("APPLY_LAG_SEC"         , "At the last sample: how old the newest redo was that the standby had applied, in seconds");
		_shortRstm.setColumnDescription("TRANSPORT_LAG_SEC__max", "The highest transport lag during the period, in seconds");
		_shortRstm.setColumnDescription("APPLY_LAG_SEC__max"    , "The highest apply lag during the period, in seconds");
		_shortRstm.setColumnDescription("TRANSPORT_SEQ_GAP__max", "The highest number of complete logs that the standby had not received, during the period");
		_shortRstm.setColumnDescription("APPLY_SEQ_GAP__max"    , "The highest number of complete logs that the standby had not applied, during the period");
		_shortRstm.setColumnDescription("samples_not_valid"     , "In how many collector samples the destination was in another state than VALID");
		_shortRstm.setColumnDescription("samples_count"         , "Number of collector samples in the period");
		_shortRstm.setColumnDescription("STANDBY_TYPE"          , "PHYSICAL, LOGICAL, SNAPSHOT...");
		_shortRstm.setColumnDescription("DATABASE_MODE"         , "The state of the standby database: MOUNTED-STANDBY, OPEN_READ-ONLY...");
		_shortRstm.setColumnDescription("RECOVERY_MODE"         , "What the apply does: IDLE (not applying), MANAGED, MANAGED REAL TIME APPLY...");
		_shortRstm.setColumnDescription("PROTECTION_MODE"       , "MAXIMUM PERFORMANCE, MAXIMUM AVAILABILITY or MAXIMUM PROTECTION");
		_shortRstm.setColumnDescription("TRANSMIT_MODE"         , "SYNCHRONOUS (a commit waits for the standby) or ASYNCHRONOUS");
		_shortRstm.setColumnDescription("DELAY_MINS"            , "Minutes that the standby waits before it applies a log, on purpose");
		_shortRstm.setColumnDescription("DESTINATION"           , "The service name that the redo is sent to");
		_shortRstm.setColumnDescription("ERROR"                 , "The error message of the destination at the last sample, if any");

		// The graphs: only if they were recorded
		if (doTableExist(conn, schema, CmDataGuard.CM_NAME + "_" + CmDataGuard.GRAPH_NAME_LAG_SEC))
			_CmDataGuard_DgLagSec = createTsLineChart(conn, schema, CmDataGuard.CM_NAME, CmDataGuard.GRAPH_NAME_LAG_SEC, -1, true, null, "Data Guard, Transport and Apply Lag in Seconds, per Standby (" + CmDataGuard.SHORT_NAME + ")");

		if (doTableExist(conn, schema, CmDataGuard.CM_NAME + "_" + CmDataGuard.GRAPH_NAME_SEQ_GAP))
			_CmDataGuard_DgSeqGap = createTsLineChart(conn, schema, CmDataGuard.CM_NAME, CmDataGuard.GRAPH_NAME_SEQ_GAP, -1, true, null, "Data Guard, Log Sequences not Received and not Applied, per Standby (" + CmDataGuard.SHORT_NAME + ")");
	}

	private IReportChart _CmDataGuard_DgLagSec; // null = not recorded
	private IReportChart _CmDataGuard_DgSeqGap; // null = not recorded
}
