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
package com.dbxtune.pcs.report.content.postgres;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.dbxtune.cm.postgres.CmPgArchiver;
import com.dbxtune.cm.postgres.CmPgBgWriter;
import com.dbxtune.cm.postgres.CmPgWal;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * WAL, checkpoints and archiving: checkpoints per hour (requested = forced by the WAL volume, timed = by checkpoint_timeout), WAL MB per hour,
 * the size of pg_wal, and the archiver. With advice on max_wal_size and wal_buffers.
 * <p>
 * From CmPgBgWriter (checkpoints; 17+ from pg_stat_checkpointer), CmPgWal (14+) and CmPgArchiver. Built on Db2TransactionLog.
 */
public class PostgresWalCheckpoints
extends PostgresAbstract
{
	private ResultSetTableModel _hourRstm;    // null = not recorded
	private ResultSetTableModel _walDirRstm;  // null = not recorded (before 14, or an old recording)
	private ResultSetTableModel _archiveRstm; // null = not recorded, or archive_mode is off
	private String              _archiveText;
	private String              _walDirVerdict; // null = pg_wal stayed normal
	private List<String>        _verdicts = new ArrayList<>();

	public PostgresWalCheckpoints(DailySummaryReportAbstract reportingInstance)
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
		if (_hourRstm == null)
		{
			w.append("<b>Checkpoints were not recorded</b>: CmPgBgWriter has no checkpoint columns in this recording (it is from an older PostgresTune, which did not read pg_stat_checkpointer on Postgres 17 and above).<br>\n");
		}
		else
		{
			w.append(getSectionDescriptionHtml(_hourRstm, true));
			w.append(toHtmlTable(_hourRstm));
			for (String v : _verdicts)
				w.append("<p><b>Verdict</b>: " + v + "</p>\n");

			if (_CmPgBgWriter_CheckpointCount != null)
			{
				w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are the checkpoints during the day (requested = forced by the WAL volume, timed = by checkpoint_timeout).",
						"CmPgBgWriter_" + CmPgBgWriter.GRAPH_NAME_CHECKPOINT_COUNT,
						"CmPgWal_"      + CmPgWal.GRAPH_NAME_WAL_KB
						));
				_CmPgBgWriter_CheckpointCount.writeHtmlContent(w, null, null);
			}
			if (_CmPgWal_WalKb != null)
				_CmPgWal_WalKb.writeHtmlContent(w, null, null);
		}

		if (_walDirRstm != null)
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_walDirRstm, true));
			w.append(toHtmlTable(_walDirRstm));
			if (_walDirVerdict != null)
				w.append("<p><b>Verdict</b>: " + _walDirVerdict + "</p>\n");
			if (isFullMessageType() && _CmPgWal_WalSizeMb != null)
				_CmPgWal_WalSizeMb.writeHtmlContent(w, null, "<b>Note</b>: pg_wal grows when the archiver fails, a replication slot keeps WAL, or max_wal_size / wal_keep_size are big.");
		}

		w.append("<br>\n");
		if (_archiveText != null)
			w.append("<p>" + _archiveText + "</p>\n");
		if (_archiveRstm != null)
		{
			w.append(getSectionDescriptionHtml(_archiveRstm, true));
			w.append(toHtmlTable(_archiveRstm));
			if (isFullMessageType() && _CmPgArchiver_ArchivedCount != null)
				_CmPgArchiver_ArchivedCount.writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "WAL, Checkpoints and Archiving (origin: CmPgBgWriter, CmPgWal, CmPgArchiver)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	/** Requested checkpoints above this percent of all checkpoints (and at least 3) gives the max_wal_size advice: as the CheckpointsRequested alarm */
	private double getRequestedPct()
	{
		return Configuration.getCombinedConfiguration().getDoubleProperty(getClass().getSimpleName() + ".requested.pct", 20.0);
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();
		String hour   = "formatdatetime([SessionSampleTime], 'yyyy-MM-dd HH:00')";

		boolean hasWal = getRecordedColumns(conn, "CmPgWal_diff").contains("wal_bytes");

		//-------------------------------------------------------------
		// Checkpoints (and WAL) per hour
		//-------------------------------------------------------------
		if (getRecordedColumns(conn, "CmPgBgWriter_diff").contains("checkpoints_req"))
		{
			String sql = ""
				    + "select \n"
				    + "     b.[hour] \n"
				    + "    ,b.[checkpoints_timed] \n"
				    + "    ,b.[checkpoints_req] \n"
				    + "    ,cast(100.0 * b.[checkpoints_req] / nullif(b.[checkpoints_timed] + b.[checkpoints_req], 0) as numeric(5,1)) as [requested_pct] \n"
				    + "    ,b.[checkpoint_write_sec] \n"
				    + "    ,b.[checkpoint_sync_sec] \n"
				    + (hasWal ? ""
				    + "    ,w.[wal_mb] \n"
				    + "    ,w.[wal_fpi_pct] \n"
				    + "    ,w.[wal_buffers_full] \n" : "")
				    + "from (select \n"
				    + "           " + hour + " as [hour] \n"
				    + "          ,sum([checkpoints_timed]) as [checkpoints_timed] \n"
				    + "          ,sum([checkpoints_req])   as [checkpoints_req] \n"
				    + "          ,cast(sum([checkpoint_write_time]) / 1000.0 as numeric(12,1)) as [checkpoint_write_sec] \n"
				    + "          ,cast(sum([checkpoint_sync_time])  / 1000.0 as numeric(12,1)) as [checkpoint_sync_sec] \n"
				    + "      from [CmPgBgWriter_diff] \n"
				    + "      " + where + " \n"
				    + "      group by " + hour + " \n"
				    + "     ) b \n"
				    + (hasWal ? ""
				    + "left join (select \n"
				    + "           " + hour + " as [hour] \n"
				    + "          ,cast(sum([wal_bytes]) / 1048576.0 as numeric(12,1)) as [wal_mb] \n"
				    + "          ,cast(100.0 * sum([wal_fpi]) / nullif(sum([wal_records]), 0) as numeric(5,1)) as [wal_fpi_pct] \n"
				    + "          ,sum([wal_buffers_full]) as [wal_buffers_full] \n"
				    + "      from [CmPgWal_diff] \n"
				    + "      " + where + " \n"
				    + "      group by " + hour + " \n"
				    + "     ) w on w.[hour] = b.[hour] \n" : "")
				    + "order by b.[hour] \n"
				    + "";

			_hourRstm = executeQuery(conn, sql, true, "PostgresWalCheckpointsHour");

			long   maxWalSizeMb   = getServerSettingLong(conn, "max_wal_size",       1024);
			long   ckptTimeoutSec = getServerSettingLong(conn, "checkpoint_timeout", 300);

			_hourRstm.setDescription(
					"Checkpoints and WAL per hour (from the tables <code>CmPgBgWriter_diff</code> and <code>CmPgWal_diff</code>).<br>" +
					"A checkpoint writes all changed pages to disk. <i>Timed</i> checkpoints come every <code>checkpoint_timeout</code> (now " + ckptTimeoutSec + " s); " +
					"<i>requested</i> ones are forced when the WAL since the last checkpoint reaches about <code>max_wal_size</code> (now " + maxWalSizeMb + " MB), or by a CHECKPOINT command / a backup. " +
					"Many requested checkpoints mean more disk writes and more full-page images in the WAL (<code>wal_fpi_pct</code>).<br>" +
					"");
			_hourRstm.setColumnDescription("hour"                , "The hour");
			_hourRstm.setColumnDescription("checkpoints_timed"   , "Checkpoints started by checkpoint_timeout");
			_hourRstm.setColumnDescription("checkpoints_req"     , "Requested checkpoints: forced by the WAL volume (max_wal_size), a CHECKPOINT command, a base backup, ...");
			_hourRstm.setColumnDescription("requested_pct"       , "Requested checkpoints in percent of all checkpoints");
			_hourRstm.setColumnDescription("checkpoint_write_sec", "Seconds the checkpoints spent writing pages (spread over checkpoint_completion_target)");
			_hourRstm.setColumnDescription("checkpoint_sync_sec" , "Seconds the checkpoints spent in fsync (a slow disk shows here)");
			_hourRstm.setColumnDescription("wal_mb"              , "WAL written during the hour, in MB (Postgres 14 and above)");
			_hourRstm.setColumnDescription("wal_fpi_pct"         , "Full-page images in percent of the WAL records: the first change of a page after a checkpoint writes the whole page to the WAL");
			_hourRstm.setColumnDescription("wal_buffers_full"    , "Times the WAL buffers were full, so a session had to write the WAL itself");

			// The verdicts
			long   sumTimed = 0, sumReq = 0, sumBufFull = 0;
			double maxWalMbHour = 0;
			for (int r = 0; r < _hourRstm.getRowCount(); r++)
			{
				sumTimed += _hourRstm.getValueAsLong(r, "checkpoints_timed", true, 0L);
				sumReq   += _hourRstm.getValueAsLong(r, "checkpoints_req",   true, 0L);
				if (hasWal)
				{
					sumBufFull += _hourRstm.getValueAsLong(r, "wal_buffers_full", true, 0L);
					Double mb = _hourRstm.getValueAsDouble(r, "wal_mb");
					maxWalMbHour = Math.max(maxWalMbHour, mb == null ? 0 : mb);
				}
			}
			double reqPct = sumTimed + sumReq == 0 ? 0 : 100.0 * sumReq / (sumTimed + sumReq);
			if (sumReq >= 3 && reqPct > getRequestedPct())
			{
				String txt = String.format("%.0f", reqPct) + "% of the checkpoints (" + sumReq + " of " + (sumTimed + sumReq) + ") were requested, not timed. "
						+ "If they were forced by the WAL volume (not by CHECKPOINT commands or backups), <code>max_wal_size</code> (now " + maxWalSizeMb + " MB) is too small for the write load.";
				if (maxWalMbHour > 0)
				{
					long walPerTimeoutMb = Math.round(maxWalMbHour * ckptTimeoutSec / 3600.0);
					long suggestMb       = Math.max(maxWalSizeMb * 2, (long) Math.ceil(walPerTimeoutMb * 1.5 / 1024.0) * 1024);
					txt += " The busiest hour wrote " + Math.round(maxWalMbHour) + " MB of WAL: about " + walPerTimeoutMb + " MB per checkpoint_timeout. "
						+ "<i>Fix</i>: <code>ALTER SYSTEM SET max_wal_size = '" + suggestMb + "MB'; SELECT pg_reload_conf();</code> (the disk of pg_wal must have room for it).";
				}
				_verdicts.add(txt);
			}
			if (sumBufFull > 0)
			{
				long walBuffersPages = getServerSettingLong(conn, "wal_buffers", -1);
				String now = walBuffersPages > 0 ? (walBuffersPages * 8 / 1024) + " MB" : "unknown";
				_verdicts.add("The WAL buffers were full " + sumBufFull + " times: sessions had to write the WAL themselves (slower commits). "
						+ "<code>wal_buffers</code> is now " + now + ". <i>Fix</i>: <code>ALTER SYSTEM SET wal_buffers = '64MB';</code> (needs a restart).");
			}

			_CmPgBgWriter_CheckpointCount = chart(conn, schema, CmPgBgWriter.CM_NAME, CmPgBgWriter.GRAPH_NAME_CHECKPOINT_COUNT, "Checkpoints per Second, Timed and Requested (" + CmPgBgWriter.SHORT_NAME + ")");
			if (hasWal)
				_CmPgWal_WalKb = chart(conn, schema, CmPgWal.CM_NAME, CmPgWal.GRAPH_NAME_WAL_KB, "WAL KB Written per Second (" + CmPgWal.SHORT_NAME + ")");
		}

		//-------------------------------------------------------------
		// The size of pg_wal
		//-------------------------------------------------------------
		if (getRecordedColumns(conn, "CmPgWal_abs").contains("pg_waldir_total_size_mb"))
		{
			String sql = ""
				    + "select \n"
				    + "     (select max([pg_waldir_total_size_mb]) from [CmPgWal_abs] " + where + ") as [pg_waldir_size_mb__max] \n"
				    + "    ,(select min([SessionSampleTime]) from [CmPgWal_abs] x " + where + " and x.[pg_waldir_total_size_mb] = (select max([pg_waldir_total_size_mb]) from [CmPgWal_abs] " + where + ")) as [pg_waldir_size_mb__max_time] \n"
				    + "    ,(select min([pg_waldir_total_size_mb]) from [CmPgWal_abs] " + where + ") as [pg_waldir_size_mb__min] \n"
				    + "    ,l.[pg_waldir_total_size_mb] as [pg_waldir_size_mb__last] \n"
				    + "    ,l.[oldest_wal_file_ts] \n"
				    + "from [CmPgWal_abs] l \n"
				    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgWal_abs] " + where + ") \n"
				    + "";

			_walDirRstm = executeQuery(conn, sql, true, "PostgresWalCheckpointsWalDir");
			if (_walDirRstm.getRowCount() == 0)
			{
				_walDirRstm = null;
			}
			else
			{
				_walDirRstm.setDescription(
						"The size of the pg_wal directory (from the table <code>CmPgWal_abs</code>). Normally it stays around <code>max_wal_size</code>. " +
						"It grows (until the disk is full) when the archiver fails, or a replication slot keeps the WAL (see 'Replication / HA').<br>" +
						"");
				_walDirRstm.setColumnDescription("pg_waldir_size_mb__max"     , "The largest pg_wal was during the period, in MB");
				_walDirRstm.setColumnDescription("pg_waldir_size_mb__max_time", "When it was the largest");
				_walDirRstm.setColumnDescription("pg_waldir_size_mb__min"     , "The smallest it was, in MB");
				_walDirRstm.setColumnDescription("pg_waldir_size_mb__last"    , "The size at the last sample, in MB");
				_walDirRstm.setColumnDescription("oldest_wal_file_ts"         , "The oldest WAL file in pg_wal at the last sample (old files = something keeps them)");

				// pg_wal much bigger than max_wal_size (+ wal_keep_size): something keeps the WAL
				Double maxMb      = _walDirRstm.getValueAsDouble(0, "pg_waldir_size_mb__max");
				long   maxWalMb   = getServerSettingLong(conn, "max_wal_size",  1024);
				long   walKeepMb  = getServerSettingLong(conn, "wal_keep_size", 0);
				if (maxMb != null && maxMb > 1.5 * (maxWalMb + walKeepMb))
				{
					_walDirVerdict = "pg_wal grew to " + Math.round(maxMb) + " MB, " + String.format("%.1f", maxMb / (maxWalMb + walKeepMb)) + " times max_wal_size + wal_keep_size (" + maxWalMb + " + " + walKeepMb + " MB). "
							+ "Something keeps the WAL: a replication slot whose consumer is behind or gone (see 'Replication / HA', the slots' <code>lag_mb</code>), "
							+ "or a failing archiver (below). If it goes on, the disk fills up and the server stops. Limit what a slot may keep with <code>max_slot_wal_keep_size</code>.";
				}

				_CmPgWal_WalSizeMb = chart(conn, schema, CmPgWal.CM_NAME, CmPgWal.GRAPH_NAME_WAL_SIZE_MB, "pg_wal Directory Size in MB (" + CmPgWal.SHORT_NAME + ")");
			}
		}

		//-------------------------------------------------------------
		// Archiving
		//-------------------------------------------------------------
		String archiveMode = getServerSetting(conn, "archive_mode");
		if ("off".equalsIgnoreCase(archiveMode))
		{
			_archiveText = "<b>WAL archiving is off</b> (archive_mode = off): no point-in-time recovery from archived WAL, unless the backups are taken some other way (for example pg_basebackup with streamed WAL, or a backup tool that streams the WAL).";
		}
		else if (getRecordedColumns(conn, "CmPgArchiver_diff").contains("failed_count"))
		{
			String sql = ""
				    + "select \n"
				    + "     (select sum([archived_count]) from [CmPgArchiver_diff] " + where + ") as [archived_count__sum] \n"
				    + "    ,(select sum([failed_count])   from [CmPgArchiver_diff] " + where + ") as [failed_count__sum] \n"
				    + "    ,l.[last_archived_wal] \n"
				    + "    ,l.[last_archived_time] \n"
				    + "    ,l.[last_failed_wal] \n"
				    + "    ,l.[last_failed_time] \n"
				    + "from [CmPgArchiver_abs] l \n"
				    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmPgArchiver_abs] " + where + ") \n"
				    + "";

			_archiveRstm = executeQuery(conn, sql, true, "PostgresWalCheckpointsArchiver");
			_archiveText = "WAL archiving: archive_mode = " + (archiveMode == null ? "unknown" : archiveMode) + ".";

			_archiveRstm.setDescription(
					"The WAL archiver (from the tables <code>CmPgArchiver_diff</code> and <code>_abs</code>). " +
					"Look for: <code>failed_count__sum</code> &gt; 0 (archive_command / archive_library failed: the WAL stays in pg_wal until it works, and the backups can't be restored to after that point), " +
					"and an old <code>last_archived_time</code> on a busy server.<br>" +
					"");
			_archiveRstm.setColumnDescription("archived_count__sum", "WAL files archived during the period");
			_archiveRstm.setColumnDescription("failed_count__sum"  , "Failed archive attempts during the period (each failed file is retried)");
			_archiveRstm.setColumnDescription("last_archived_wal"  , "The last archived WAL file (at the last sample)");
			_archiveRstm.setColumnDescription("last_archived_time" , "When it was archived");
			_archiveRstm.setColumnDescription("last_failed_wal"    , "The last WAL file that failed (the error is in the error log)");
			_archiveRstm.setColumnDescription("last_failed_time"   , "When it failed (it may be before the period)");

			_CmPgArchiver_ArchivedCount = chart(conn, schema, CmPgArchiver.CM_NAME, CmPgArchiver.GRAPH_NAME_ARCHIVED_COUNT, "Archived WAL Files (" + CmPgArchiver.SHORT_NAME + ")");
		}
	}

	/** A line chart, or null when the graph was not recorded */
	private IReportChart chart(DbxConnection conn, String schema, String cmName, String graphName, String title)
	{
		if ( ! doTableExist(conn, schema, cmName + "_" + graphName) )
			return null;
		return createTsLineChart(conn, schema, cmName, graphName, -1, false, null, title);
	}

	private IReportChart _CmPgBgWriter_CheckpointCount;
	private IReportChart _CmPgWal_WalKb;
	private IReportChart _CmPgWal_WalSizeMb;
	private IReportChart _CmPgArchiver_ArchivedCount;
}
