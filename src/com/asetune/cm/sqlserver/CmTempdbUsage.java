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
package com.asetune.cm.sqlserver;

import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmTempdbSpidUsagePanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSqlServer;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTempdbUsage
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTempdbUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTempdbUsage.class.getSimpleName();
	public static final String   SHORT_NAME       = "Tempdb Usage";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>How much is used in the tempdb.</p>" +
//			"<br>" +
//			"<b>Tip:</b><br>" +
//			"Sort by 'BatchIdDiff', will give you the one that executes the most SQL Batches.<br>" +
//			"Or check 'WaitEventDesc' to find out when the SPID is waiting for." +
//			"<br><br>" +
//			"Table Background colors:" +
//			"<ul>" +
//			"    <li>YELLOW - SPID is a System Processes</li>" +
//			"    <li>GREEN  - SPID is Executing(running) or are in the Run Queue Awaiting a time slot to Execute (runnable)</li>" +
//			"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
//			"    <li>ORANGE - SPID has an open transaction.</li>" +
//			"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
//			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_db_file_space_usage"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"usedPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"total_MB",
		"allocated_extent_MB",
		"unallocated_extent_MB",
		"version_store_reserved_MB",
		"user_object_reserved_MB",
		"internal_object_reserved_MB",
		"mixed_extent_MB",
		"modified_extent_MB",

		"total_page_count",
		"allocated_extent_page_count",
		"unallocated_extent_page_count",
		"version_store_reserved_page_count",
		"user_object_reserved_page_count",
		"internal_object_reserved_page_count",
		"mixed_extent_page_count",
		"modified_extent_page_count"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmTempdbUsage(counterController, guiController);
	}

	public CmTempdbUsage(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String GRAPH_NAME_TEMPDB_USAGE_SUM_FULL    = "TempdbUsageFull";
	public static final String GRAPH_NAME_TEMPDB_USAGE_SUM_SMALL   = "TempdbUsageSmall";

	private void addTrendGraphs()
	{
		//-----
		addTrendGraph(GRAPH_NAME_TEMPDB_USAGE_SUM_FULL,
			"Tempdb Usage by Object Type, in MB, All", // Menu CheckBox text
			"Tempdb Usage by Object Type, in MB, All ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.AUTO, -1),
			new String[] { "Version Store", "Internal Objects", "User Objects", "Mixed Extents", "Allocated Space", "Free Space", "Total Space" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		//-----
		addTrendGraph(GRAPH_NAME_TEMPDB_USAGE_SUM_SMALL,
			"Tempdb Usage by Object Type, in MB, Subset", // Menu CheckBox text
			"Tempdb Usage by Object Type, in MB, Subset ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB, CentralPersistReader.SampleType.AUTO, -1),
			new String[] { "Version Store", "Internal Objects", "User Objects", "Mixed Extents", "Allocated Space" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.SPACE,
			false,  // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TEMPDB_USAGE_SUM_FULL.equals(tgdp.getName()))
		{
			Double[] arr = new Double[7];

			arr[0] = this.findColumn("version_store_reserved_page_count")   == -1 ? 0.0d : round1(this.getAbsValueSum("version_store_reserved_page_count")  / 128.0d);
			arr[1] = this.findColumn("internal_object_reserved_page_count") == -1 ? 0.0d : round1(this.getAbsValueSum("internal_object_reserved_page_count")/ 128.0d);
			arr[2] = this.findColumn("user_object_reserved_page_count")     == -1 ? 0.0d : round1(this.getAbsValueSum("user_object_reserved_page_count")    / 128.0d);
			arr[3] = this.findColumn("mixed_extent_page_count")             == -1 ? 0.0d : round1(this.getAbsValueSum("mixed_extent_page_count")            / 128.0d);
//			arr[?] = this.findColumn("modified_extent_page_count")          == -1 ? 0.0d : round1(this.getAbsValueSum("modified_extent_page_count")         / 128.0d);

			arr[4] = this.findColumn("allocated_extent_page_count")         == -1 ? 0.0d : round1(this.getAbsValueSum("allocated_extent_page_count")        / 128.0d);
			arr[5] = this.findColumn("unallocated_extent_page_count")       == -1 ? 0.0d : round1(this.getAbsValueSum("unallocated_extent_page_count")      / 128.0d);
			arr[6] = this.findColumn("total_page_count")                    == -1 ? 0.0d : round1(this.getAbsValueSum("total_page_count")                   / 128.0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		// -----------------------------------------------------------------------------------------
		if (GRAPH_NAME_TEMPDB_USAGE_SUM_SMALL.equals(tgdp.getName()))
		{
			Double[] arr = new Double[5];

			arr[0] = this.findColumn("version_store_reserved_page_count")   == -1 ? 0.0d : round1(this.getAbsValueSum("version_store_reserved_page_count")  / 128.0d);
			arr[1] = this.findColumn("internal_object_reserved_page_count") == -1 ? 0.0d : round1(this.getAbsValueSum("internal_object_reserved_page_count")/ 128.0d);
			arr[2] = this.findColumn("user_object_reserved_page_count")     == -1 ? 0.0d : round1(this.getAbsValueSum("user_object_reserved_page_count")    / 128.0d);
			arr[3] = this.findColumn("mixed_extent_page_count")             == -1 ? 0.0d : round1(this.getAbsValueSum("mixed_extent_page_count")            / 128.0d);
//			arr[?] = this.findColumn("modified_extent_page_count")          == -1 ? 0.0d : round1(this.getAbsValueSum("modified_extent_page_count")         / 128.0d);

			arr[4] = this.findColumn("allocated_extent_page_count")         == -1 ? 0.0d : round1(this.getAbsValueSum("allocated_extent_page_count")        / 128.0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

	}



//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmTempdbUsagePanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			mtd.addTable("dm_db_file_space_usage",  "");

			mtd.addColumn("dm_db_file_space_usage",  "total_MB"                   ,     "<html>Same as 'total_page_count'                    but as MB. <br><b>Algorithm</b>: total_page_count                    / 128.0 </html>");
			mtd.addColumn("dm_db_file_space_usage",  "allocated_extent_MB"        ,     "<html>Same as 'allocated_extent_page_count'         but as MB. <br><b>Algorithm</b>: allocated_extent_page_count         / 128.0</html>");
			mtd.addColumn("dm_db_file_space_usage",  "modified_extent_MB"         ,     "<html>Same as 'modified_extent_page_count'          but as MB. <br><b>Algorithm</b>: modified_extent_page_count          / 128.0</html>");
			mtd.addColumn("dm_db_file_space_usage",  "unallocated_extent_MB"      ,     "<html>Same as 'unallocated_extent_page_count'       but as MB. <br><b>Algorithm</b>: unallocated_extent_page_count       / 128.0</html>");
			mtd.addColumn("dm_db_file_space_usage",  "version_store_reserved_MB"  ,     "<html>Same as 'version_store_reserved_page_count'   but as MB. <br><b>Algorithm</b>: version_store_reserved_page_count   / 128.0</html>");
			mtd.addColumn("dm_db_file_space_usage",  "user_object_reserved_MB"    ,     "<html>Same as 'user_object_reserved_page_count'     but as MB. <br><b>Algorithm</b>: user_object_reserved_page_count     / 128.0</html>");
			mtd.addColumn("dm_db_file_space_usage",  "internal_object_reserved_MB",     "<html>Same as 'internal_object_reserved_page_count' but as MB. <br><b>Algorithm</b>: internal_object_reserved_page_count / 128.0</html>");
			mtd.addColumn("dm_db_file_space_usage",  "mixed_extent_MB"            ,     "<html>Same as 'mixed_extent_page_count'             but as MB. <br><b>Algorithm</b>: mixed_extent_page_count             / 128.0</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("file_id");
		
		if (srvVersion >= Ver.ver(2012))
			pkCols.add("filegroup_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSqlServer ssVersionInfo = (DbmsVersionInfoSqlServer) versionInfo;
		long srvVersion = ssVersionInfo.getLongVersion();

		String dm_db_file_space_usage = "dm_db_file_space_usage";
		
		if (ssVersionInfo.isAzureSynapseAnalytics())
			dm_db_file_space_usage = "dm_pdw_nodes_db_file_space_usage";

		String filegroup_id                  = "";
		String usedPct                       = "";
		String total_MB_abs                  = "";
		String total_MB                      = "";
		String allocated_extent_MB           = "";
		String total_page_count_abs          = "";
		String total_page_count              = "";
		String allocated_extent_page_count   = "";
		String unallocated_extent_MB         = ", unallocated_extent_MB       = convert(numeric(12,1), unallocated_extent_page_count       / 128.0) \n";
		String unallocated_extent_page_count = ", unallocated_extent_page_count \n";

		if (srvVersion >= Ver.ver(2012))
		{
			filegroup_id                  = ", filegroup_id \n";
			
			usedPct                       = ", usedPct = convert(numeric(6,2), ((allocated_extent_page_count*1.0) / (total_page_count*1.0)) * 100.0) \n";
			total_MB_abs                  = ", total_MB_abs                = convert(numeric(12,1), total_page_count                    / 128.0) \n";
			total_MB                      = ", total_MB                    = convert(numeric(12,1), total_page_count                    / 128.0) \n";
			allocated_extent_MB           = ", allocated_extent_MB         = convert(numeric(12,1), allocated_extent_page_count         / 128.0) \n";
			unallocated_extent_MB         = "";

			total_page_count_abs          = ", total_page_count_abs = total_page_count \n";
			total_page_count              = ", total_page_count \n";
			allocated_extent_page_count   = ", allocated_extent_page_count \n";
			unallocated_extent_page_count = "";
		}
		
		String modified_extent_MB         = "";
		String modified_extent_page_count = "";
		if (srvVersion >= Ver.ver(2016,0,0, 2))
		{
			modified_extent_MB            = ", modified_extent_MB          = convert(numeric(12,1), modified_extent_page_count          / 128.0) \n";
			modified_extent_page_count    = ", modified_extent_page_count \n";
		}
		
		// FIXME: THIS CAN BE IMPROVED: Just a placeholder to do something
		String sql = 
			"SELECT /* ${cmCollectorName} */ \n"
			+ "  database_id \n"
			+ ", file_id \n"
			+ filegroup_id
			
			+ usedPct
			+ total_MB_abs
			+ total_MB
			+ allocated_extent_MB
			+ unallocated_extent_MB // only show if below SQL-Server 2012
			+ ", version_store_reserved_MB   = convert(numeric(12,1), version_store_reserved_page_count   / 128.0) \n"
			+ ", user_object_reserved_MB     = convert(numeric(12,1), user_object_reserved_page_count     / 128.0) \n"
			+ ", internal_object_reserved_MB = convert(numeric(12,1), internal_object_reserved_page_count / 128.0) \n"
			+ ", mixed_extent_MB             = convert(numeric(12,1), mixed_extent_page_count             / 128.0) \n"
			+ modified_extent_MB

			+ total_page_count_abs
			+ total_page_count
			+ allocated_extent_page_count
			+ unallocated_extent_page_count // only show if below SQL-Server 2012
			+ ", version_store_reserved_page_count \n"
			+ ", user_object_reserved_page_count \n"
			+ ", internal_object_reserved_page_count \n"
			+ ", mixed_extent_page_count \n"
			+ modified_extent_page_count

			+ "FROM tempdb.sys." + dm_db_file_space_usage + " u \n"
			+ "WHERE u.database_id = 2 \n"
			;

		return sql;
	}
}
