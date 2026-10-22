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
package com.dbxtune.pcs.report.content.oracle;

import java.io.IOException;
import java.io.Writer;

import com.dbxtune.cm.oracle.CmRecoveryArea;
import com.dbxtune.cm.oracle.CmTablespaces;
import com.dbxtune.cm.oracle.CmTempSegUsage;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

public class OracleSpaceOverview
extends OracleAbstract
{
	private ResultSetTableModel _tablespaceRstm;
	private ResultSetTableModel _recoveryAreaRstm; // null = no Recovery Area was recorded

	public OracleSpaceOverview(DailySummaryReportAbstract reportingInstance)
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
		// Get a description of this section, and column names
		w.append(getSectionDescriptionHtml(_tablespaceRstm, true));

		w.append("Row Count: " + _tablespaceRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_tablespaceRstm));
		w.append("<br>\n");

		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are the space usage during the day.",
				"CmTablespaces_TsUsedPct",
				"CmTablespaces_TsUsedMb",
				"CmTempSegUsage_TempInUseMb",
				"CmRecoveryArea_FraUsedPct"
				));

		_CmTablespaces_TsUsedPct.writeHtmlContent(w, null, null);
		_CmTablespaces_TsUsedMb .writeHtmlContent(w, null, null);

		if (_CmTempSegUsage_TempInUseMb != null)
			_CmTempSegUsage_TempInUseMb.writeHtmlContent(w, null, "<b>Note</b>: The temporary tablespace is not in the charts above: it's files are 'full' as soon as they have been used once. The chart above is the temp space that really was in use.");

		// Recovery Area: only if the database has one
		if (_recoveryAreaRstm != null)
		{
			w.append("<br>\n");
			w.append(getSectionDescriptionHtml(_recoveryAreaRstm, true));
			w.append(toHtmlTable(_recoveryAreaRstm));
			w.append("<br>\n");

			_CmRecoveryArea_FraUsedPct.writeHtmlContent(w, null, null);
		}
	}

	@Override
	public String getSubject()
	{
		return "Space Usage: Tablespaces and Recovery Area (origin: CmTablespaces, CmRecoveryArea / DBA_DATA_FILES, V$RECOVERY_FILE_DEST)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmTablespaces_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();

		// Tablespaces at the LAST sample, and how much the used space changed since the FIRST sample
		String sql = ""
			    + "select \n"
			    + "     l.[CON_NAME] \n"
			    + "    ,l.[TABLESPACE_NAME] \n"
			    + "    ,l.[CONTENTS] \n"
			    + "    ,l.[STATUS] \n"
			    + "    ,l.[FILE_COUNT] \n"
			    + "    ,l.[AUTOEXT_FILE_COUNT] \n"
			    + "    ,l.[ALLOC_MB] \n"
			    + "    ,l.[MAX_MB] \n"
			    + "    ,l.[USED_MB] \n"
			    + "    ,l.[FREE_MB] \n"
			    + "    ,l.[USED_PCT] \n"
			    + "    ,l.[USED_MB] - f.[USED_MB] as [USED_MB_CHANGE] \n"
			    + "from [CmTablespaces_abs] l \n"
			    + "left outer join [CmTablespaces_abs] f on  f.[CON_ID]            = l.[CON_ID] \n"
			    + "                                      and f.[TABLESPACE_NAME]   = l.[TABLESPACE_NAME] \n"
			    + "                                      and f.[SessionSampleTime] = (select min([SessionSampleTime]) from [CmTablespaces_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmTablespaces_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
			    + "order by l.[USED_PCT] desc \n"
			    + "";

		_tablespaceRstm = executeQuery(conn, sql, true, "OracleTablespaces");

		// Highlight sort column
		_tablespaceRstm.setHighlightSortColumns("USED_PCT");

		_tablespaceRstm.setDescription(
				"The tablespaces at the last collector sample (from the table <code>CmTablespaces_abs</code>), the fullest first.<br>" +
				"<code>USED_PCT</code> is in percent of <code>MAX_MB</code>: how big the tablespace can get when its files autoextend to their max size.<br>" +
				"");

		_tablespaceRstm.setColumnDescription("CON_NAME"          , "Container (PDB) name. Empty when the database is not a Container Database.");
		_tablespaceRstm.setColumnDescription("TABLESPACE_NAME"   , "Name of the tablespace");
		_tablespaceRstm.setColumnDescription("CONTENTS"          , "PERMANENT, UNDO or TEMPORARY");
		_tablespaceRstm.setColumnDescription("STATUS"            , "ONLINE, OFFLINE or READ ONLY");
		_tablespaceRstm.setColumnDescription("FILE_COUNT"        , "Number of files in the tablespace");
		_tablespaceRstm.setColumnDescription("AUTOEXT_FILE_COUNT", "How many of the files that grows automatically (autoextend)");
		_tablespaceRstm.setColumnDescription("ALLOC_MB"          , "Size of the files right now, in MB");
		_tablespaceRstm.setColumnDescription("MAX_MB"            , "How big the tablespace can get, in MB (files that autoextend are counted with their max size)");
		_tablespaceRstm.setColumnDescription("USED_MB"           , "Used space, in MB");
		_tablespaceRstm.setColumnDescription("FREE_MB"           , "Free space up to MAX_MB, in MB");
		_tablespaceRstm.setColumnDescription("USED_PCT"          , "Used space in percent of MAX_MB");
		_tablespaceRstm.setColumnDescription("USED_MB_CHANGE"    , "How much USED_MB changed during the period: last sample - first sample");

		_CmTablespaces_TsUsedPct = createTsLineChart(conn, schema, CmTablespaces.CM_NAME, CmTablespaces.GRAPH_NAME_USED_PCT, 100, true, null, "Tablespace Used in Percent of Max Size (" + CmTablespaces.SHORT_NAME + ")");
		_CmTablespaces_TsUsedMb  = createTsLineChart(conn, schema, CmTablespaces.CM_NAME, CmTablespaces.GRAPH_NAME_USED_MB,  -1,  true, null, "Tablespace Used Space in MB ("             + CmTablespaces.SHORT_NAME + ")");

		// Temp usage: only if it was recorded
		if (doTableExist(conn, schema, CmTempSegUsage.CM_NAME + "_" + CmTempSegUsage.GRAPH_NAME_IN_USE_MB))
			_CmTempSegUsage_TempInUseMb = createTsLineChart(conn, schema, CmTempSegUsage.CM_NAME, CmTempSegUsage.GRAPH_NAME_IN_USE_MB, -1, true, null, "Temporary Tablespace(s), Space that is In Use in MB (" + CmTempSegUsage.SHORT_NAME + ")");

		// Recovery Area: a database without one has no rows for it (and then the graph was never recorded)
		if (doTableExist(conn, schema, CmRecoveryArea.CM_NAME + "_" + CmRecoveryArea.GRAPH_NAME_USED_PCT))
		{
			sql = ""
				    + "select \n"
				    + "     [NAME] \n"
				    + "    ,[LIMIT_MB] \n"
				    + "    ,[USED_MB] \n"
				    + "    ,[RECLAIMABLE_MB] \n"
				    + "    ,[NET_FREE_MB] \n"
				    + "    ,[NET_USED_PCT] \n"
				    + "    ,[NUMBER_OF_FILES] \n"
				    + "    ,[ARCHIVED_LOG_PCT] \n"
				    + "    ,[BACKUP_PCT] \n"
				    + "    ,[FLASHBACK_LOG_PCT] \n"
				    + "    ,[OTHER_PCT] \n"
				    + "from [CmRecoveryArea_abs] \n"
				    + "where [SessionSampleTime] = (select max([SessionSampleTime]) from [CmRecoveryArea_abs] where 1 = 1 " + getReportPeriodSqlWhere().trim() + ") \n"
				    + "";

			_recoveryAreaRstm = executeQuery(conn, sql, true, "OracleRecoveryArea");

			_recoveryAreaRstm.setDescription(
					"The Recovery Area (FRA) at the last collector sample (from the table <code>CmRecoveryArea_abs</code>).<br>" +
					"<code>NET_USED_PCT</code> is what counts: used space minus what Oracle can remove by itself (reclaimable). If the Recovery Area gets full the archiver stops, and then the database stops.<br>" +
					"");

			_recoveryAreaRstm.setColumnDescription("NAME"             , "Location of the Recovery Area (db_recovery_file_dest)");
			_recoveryAreaRstm.setColumnDescription("LIMIT_MB"         , "Max size of the Recovery Area, in MB (db_recovery_file_dest_size)");
			_recoveryAreaRstm.setColumnDescription("USED_MB"          , "Used space, in MB");
			_recoveryAreaRstm.setColumnDescription("RECLAIMABLE_MB"   , "Space that Oracle can free by itself when it needs it (files that are obsolete or already backed up), in MB");
			_recoveryAreaRstm.setColumnDescription("NET_FREE_MB"      , "Free space when the reclaimable space is counted as free, in MB");
			_recoveryAreaRstm.setColumnDescription("NET_USED_PCT"     , "Used space minus reclaimable space, in percent of LIMIT_MB");
			_recoveryAreaRstm.setColumnDescription("NUMBER_OF_FILES"  , "Number of files in the Recovery Area");
			_recoveryAreaRstm.setColumnDescription("ARCHIVED_LOG_PCT" , "Percent of the Recovery Area that is used by archived redo logs");
			_recoveryAreaRstm.setColumnDescription("BACKUP_PCT"       , "Percent of the Recovery Area that is used by backups (backup pieces and image copies)");
			_recoveryAreaRstm.setColumnDescription("FLASHBACK_LOG_PCT", "Percent of the Recovery Area that is used by flashback logs");
			_recoveryAreaRstm.setColumnDescription("OTHER_PCT"        , "Percent of the Recovery Area that is used by other files (control files, online redo logs...)");

			_CmRecoveryArea_FraUsedPct = createTsLineChart(conn, schema, CmRecoveryArea.CM_NAME, CmRecoveryArea.GRAPH_NAME_USED_PCT, 100, false, null, "Recovery Area (FRA), Used Space in Percent (" + CmRecoveryArea.SHORT_NAME + ")");
		}
	}

	private IReportChart _CmTablespaces_TsUsedPct;
	private IReportChart _CmTablespaces_TsUsedMb;
	private IReportChart _CmTempSegUsage_TempInUseMb; // null = not recorded
	private IReportChart _CmRecoveryArea_FraUsedPct;  // null = not recorded
}
