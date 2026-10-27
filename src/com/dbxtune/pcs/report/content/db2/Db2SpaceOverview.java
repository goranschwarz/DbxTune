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

import com.dbxtune.cm.db2.CmStoragePaths;
import com.dbxtune.cm.db2.CmTableSpace;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.pcs.report.DailySummaryReportAbstract;
import com.dbxtune.pcs.report.content.IReportChart;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;

/**
 * Table spaces and the file systems of the storage paths at the last sample, and how they grew during the day (from CmTableSpace and CmStoragePaths). Built on OracleSpaceOverview.
 */
public class Db2SpaceOverview
extends Db2Abstract
{
	private ResultSetTableModel _tablespaceRstm;
	private ResultSetTableModel _storagePathRstm; // null = not recorded

	public Db2SpaceOverview(DailySummaryReportAbstract reportingInstance)
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
		// The file systems first: an automatic storage table space is only "full" when its file system is
		if (_storagePathRstm != null)
		{
			w.append(getSectionDescriptionHtml(_storagePathRstm, true));
			w.append("Row Count: " + _storagePathRstm.getRowCount() + "<br>\n");
			w.append(toHtmlTable(_storagePathRstm));
			w.append("<br>\n");
		}

		w.append(getSectionDescriptionHtml(_tablespaceRstm, true));
		w.append("Row Count: " + _tablespaceRstm.getRowCount() + "<br>\n");
		w.append(toHtmlTable(_tablespaceRstm));
		w.append("<br>\n");

		w.append(getDbxCentralLinkWithDescForGraphs(false, "Below are the space usage during the day.",
				"CmStoragePaths_FsUsedPct",
				"CmStoragePaths_FsFreeMb",
				"CmTableSpace_TbspUsedMb"
				));

		if (_CmStoragePaths_FsUsedPct != null) _CmStoragePaths_FsUsedPct.writeHtmlContent(w, null, null);
		if (_CmStoragePaths_FsFreeMb  != null) _CmStoragePaths_FsFreeMb .writeHtmlContent(w, null, null);
		_CmTableSpace_TbspUsedMb.writeHtmlContent(w, null, null);
	}

	@Override
	public String getSubject()
	{
		return "Space Usage: Table Spaces and Storage Paths (origin: CmTableSpace, CmStoragePaths / MON_GET_TABLESPACE, ADMIN_GET_STORAGE_PATHS)";
	}

	@Override
	public boolean hasIssueToReport()
	{
		return false; // even if we found entries, do NOT indicate this as a Problem or Issue (the alarms does that)
	}

	@Override
	public String[] getMandatoryTables()
	{
		return new String[] { "CmTableSpace_abs" };
	}

	@Override
	public void create(DbxConnection conn, String srvName, Configuration pcsSavedConf, Configuration localConf)
	{
		String schema = getReportingInstance().getDbmsSchemaName();
		String where  = "where 1 = 1 " + getReportPeriodSqlWhere().trim();

		// Table spaces at the LAST sample, and how much the used space changed since the FIRST sample
		// The ones that can NOT grow by themselves first (they are the ones that gets full)
		String sql = ""
			    + "select \n"
			    + "     l.[TBSP_NAME] \n"
			    + "    ,l.[TBSP_CONTENT_TYPE] \n"
			    + "    ,l.[TBSP_STATE] \n"
			    + "    ,l.[CAN_GROW] \n"
			    + "    ,l.[STORAGE_GROUP_NAME] \n"
			    + "    ,l.[TOTAL_MB] \n"
			    + "    ,l.[USED_MB] \n"
			    + "    ,l.[FREE_MB] \n"
			    + "    ,l.[USED_PCT] \n"
			    + "    ,l.[MAX_MB] \n"
			    + "    ,l.[USED_PCT_OF_MAX] \n"
			    + "    ,l.[USED_MB] - f.[USED_MB]   as [USED_MB_CHANGE] \n"
			    + "    ,l.[TOTAL_MB] - f.[TOTAL_MB] as [TOTAL_MB_CHANGE] \n"
			    + "    ,l.[HWM_MB] \n"
			    + "    ,l.[TBSP_LAST_RESIZE_TIME] \n"
			    + "from [CmTableSpace_abs] l \n"
			    + "left outer join [CmTableSpace_abs] f on  f.[TBSP_ID]           = l.[TBSP_ID] \n"
			    + "                                     and f.[MEMBER]            = l.[MEMBER] \n"
			    + "                                     and f.[SessionSampleTime] = (select min([SessionSampleTime]) from [CmTableSpace_abs] " + where + ") \n"
			    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmTableSpace_abs] " + where + ") \n"
			    + "order by case when l.[CAN_GROW] in ('NO', 'NO_MAX_REACHED', 'YES_TO_MAX') then 0 else 1 end, l.[USED_PCT] desc \n"
			    + "";

		_tablespaceRstm = executeQuery(conn, sql, true, "Db2Tablespaces");

		_tablespaceRstm.setHighlightSortColumns("USED_PCT");

		_tablespaceRstm.setDescription(
				"The table spaces at the last collector sample (from the table <code>CmTableSpace_abs</code>). The ones that can't grow by themselves first, then the fullest.<br>" +
				"<code>USED_PCT</code> only matters when <code>CAN_GROW</code> is NO: a table space that can grow allocates more when it needs it (then look at the file systems above, or <code>USED_PCT_OF_MAX</code>).<br>" +
				"");

		_tablespaceRstm.setColumnDescription("TBSP_NAME"            , "The table space");
		_tablespaceRstm.setColumnDescription("TBSP_CONTENT_TYPE"    , "ANY / LARGE = data, SYSTEMP = system temporary, USRTEMP = user temporary");
		_tablespaceRstm.setColumnDescription("TBSP_STATE"           , "NORMAL, or for example BACKUP_PENDING, ROLLFORWARD_PENDING, OFFLINE");
		_tablespaceRstm.setColumnDescription("CAN_GROW"             , "YES = automatic storage or AUTORESIZE. YES_TO_MAX = up to MAXSIZE. FILESYSTEM = SMS. NO = full means inserts fail (SQL0289N). NO_MAX_REACHED = at MAXSIZE");
		_tablespaceRstm.setColumnDescription("STORAGE_GROUP_NAME"   , "The storage group (automatic storage)");
		_tablespaceRstm.setColumnDescription("TOTAL_MB"             , "Allocated size, in MB");
		_tablespaceRstm.setColumnDescription("USED_MB"              , "Used, in MB");
		_tablespaceRstm.setColumnDescription("FREE_MB"              , "Free in the allocated size, in MB");
		_tablespaceRstm.setColumnDescription("USED_PCT"             , "Used in percent of the allocated (usable) size");
		_tablespaceRstm.setColumnDescription("MAX_MB"               , "MAXSIZE, in MB (empty = no limit)");
		_tablespaceRstm.setColumnDescription("USED_PCT_OF_MAX"      , "Used in percent of MAXSIZE");
		_tablespaceRstm.setColumnDescription("USED_MB_CHANGE"       , "How much USED_MB changed during the period: last sample - first sample");
		_tablespaceRstm.setColumnDescription("TOTAL_MB_CHANGE"      , "How much the table space grew during the period: last sample - first sample");
		_tablespaceRstm.setColumnDescription("HWM_MB"               , "The high water mark, in MB (a DMS table space can't shrink below it without a REORG / LOWER HIGH WATER MARK)");
		_tablespaceRstm.setColumnDescription("TBSP_LAST_RESIZE_TIME", "When the table space was last resized");

		_CmTableSpace_TbspUsedMb = createTsLineChart(conn, schema, CmTableSpace.CM_NAME, CmTableSpace.GRAPH_NAME_USED_MB, -1, true, null, "Table Space Used MB, per Table Space (" + CmTableSpace.SHORT_NAME + ")");

		// Storage paths and containers: only if they were recorded
		if (doTableExist(conn, schema, "CmStoragePaths_abs"))
		{
			sql = ""
				    + "select \n"
				    + "     l.[PATH_TYPE] \n"
				    + "    ,l.[OWNER_NAME] \n"
				    + "    ,l.[PATH] \n"
				    + "    ,l.[STATE] \n"
				    + "    ,l.[FS_TOTAL_MB] \n"
				    + "    ,l.[FS_FREE_MB] \n"
				    + "    ,l.[FS_USED_PCT] \n"
				    + "    ,m.[FS_USED_PCT__max] \n"
				    + "    ,l.[FS_FREE_MB] - f.[FS_FREE_MB] as [FS_FREE_MB_CHANGE] \n"
				    + "    ,l.[PATH_FREE_MB] \n"
				    + "    ,l.[FS_ID] \n"
				    + "from [CmStoragePaths_abs] l \n"
				    + "join (select [PATH_TYPE], [PATH], [DBPARTITIONNUM], max([FS_USED_PCT]) as [FS_USED_PCT__max] \n"
				    + "      from [CmStoragePaths_abs] \n"
				    + "      " + where + " \n"
				    + "      group by [PATH_TYPE], [PATH], [DBPARTITIONNUM] \n"
				    + "     ) m on m.[PATH_TYPE] = l.[PATH_TYPE] and m.[PATH] = l.[PATH] and m.[DBPARTITIONNUM] = l.[DBPARTITIONNUM] \n"
				    + "left outer join [CmStoragePaths_abs] f on  f.[PATH_TYPE]         = l.[PATH_TYPE] \n"
				    + "                                       and f.[PATH]              = l.[PATH] \n"
				    + "                                       and f.[DBPARTITIONNUM]    = l.[DBPARTITIONNUM] \n"
				    + "                                       and f.[SessionSampleTime] = (select min([SessionSampleTime]) from [CmStoragePaths_abs] " + where + ") \n"
				    + "where l.[SessionSampleTime] = (select max([SessionSampleTime]) from [CmStoragePaths_abs] " + where + ") \n"
				    + "order by l.[FS_USED_PCT] desc \n"
				    + "";

			_storagePathRstm = executeQuery(conn, sql, true, "Db2StoragePaths");

			_storagePathRstm.setHighlightSortColumns("FS_USED_PCT");

			_storagePathRstm.setDescription(
					"The file systems of the storage paths (automatic storage) and of the containers of the other table spaces, at the last collector sample (from the table <code>CmStoragePaths_abs</code>). The fullest first.<br>" +
					"An automatic storage table space never looks full by itself: it is full when the file system of its storage paths is.<br>" +
					"");

			_storagePathRstm.setColumnDescription("PATH_TYPE"        , "STORAGE PATH (a storage group's path) or CONTAINER (a table space that is not automatic storage)");
			_storagePathRstm.setColumnDescription("OWNER_NAME"       , "The storage group, or the table space");
			_storagePathRstm.setColumnDescription("PATH"             , "The path");
			_storagePathRstm.setColumnDescription("STATE"            , "IN_USE, NOT_IN_USE, DROP_PENDING; or for a container ACCESSIBLE / NOT_ACCESSIBLE");
			_storagePathRstm.setColumnDescription("FS_TOTAL_MB"      , "The size of the file system, in MB");
			_storagePathRstm.setColumnDescription("FS_FREE_MB"       , "Free in the file system, in MB");
			_storagePathRstm.setColumnDescription("FS_USED_PCT"      , "The file system used in percent, at the last sample");
			_storagePathRstm.setColumnDescription("FS_USED_PCT__max" , "The fullest the file system was during the period, in percent");
			_storagePathRstm.setColumnDescription("FS_FREE_MB_CHANGE", "How much the free space changed during the period: last sample - first sample (negative = it got fuller)");
			_storagePathRstm.setColumnDescription("PATH_FREE_MB"     , "For a storage path: the free space Db2 can use in it, in MB");
			_storagePathRstm.setColumnDescription("FS_ID"            , "The file system id: paths with the same id share the same file system");

			if (doTableExist(conn, schema, CmStoragePaths.CM_NAME + "_" + CmStoragePaths.GRAPH_NAME_FS_USED_PCT))
				_CmStoragePaths_FsUsedPct = createTsLineChart(conn, schema, CmStoragePaths.CM_NAME, CmStoragePaths.GRAPH_NAME_FS_USED_PCT, 100, true, null, "File System Used in Percent, per Storage Path or Container (" + CmStoragePaths.SHORT_NAME + ")");

			if (doTableExist(conn, schema, CmStoragePaths.CM_NAME + "_" + CmStoragePaths.GRAPH_NAME_FS_FREE_MB))
				_CmStoragePaths_FsFreeMb = createTsLineChart(conn, schema, CmStoragePaths.CM_NAME, CmStoragePaths.GRAPH_NAME_FS_FREE_MB, -1, true, null, "File System Free Space in MB, per Storage Path or Container (" + CmStoragePaths.SHORT_NAME + ")");
		}
	}

	private IReportChart _CmTableSpace_TbspUsedMb;
	private IReportChart _CmStoragePaths_FsUsedPct; // null = not recorded
	private IReportChart _CmStoragePaths_FsFreeMb;  // null = not recorded
}
