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

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMemoryClerks
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTempdbUsage.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemoryClerks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>Information about the memory clerks.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_memory_clerks"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"SizeMb_diff"
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

		return new CmMemoryClerks(counterController, guiController);
	}

	public CmMemoryClerks(ICounterController counterController, IGuiController guiController)
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

	public static final String GRAPH_NAME_MEMORY_CLERKS    = "MemoryClerks";

	private void addTrendGraphs()
	{
//		//-----
//		addTrendGraph(GRAPH_NAME_TEMPDB_USAGE_SUM_FULL,
//			"Tempdb Usage by Object Type, in MB, All", // Menu CheckBox text
//			"Tempdb Usage by Object Type, in MB, All ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
//			new String[] { "Version Store", "Internal Objects", "User Objects", "Mixed Extents", "Allocated Space", "Free Space", "Total Space" }, 
//			LabelType.Static,
//			TrendGraphDataPoint.Category.SPACE,
//			false,  // is Percent Graph
//			true,  // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height
//
//		//-----
//		addTrendGraph(GRAPH_NAME_TEMPDB_USAGE_SUM_SMALL,
//			"Tempdb Usage by Object Type, in MB, Subset", // Menu CheckBox text
//			"Tempdb Usage by Object Type, in MB, Subset ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
//			TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MB,
//			new String[] { "Version Store", "Internal Objects", "User Objects", "Mixed Extents", "Allocated Space" }, 
//			LabelType.Static,
//			TrendGraphDataPoint.Category.SPACE,
//			false,  // is Percent Graph
//			true,  // visible at start
//			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
//			-1);   // minimum height
	}

//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//		// -----------------------------------------------------------------------------------------
//		if (GRAPH_NAME_TEMPDB_USAGE_SUM_FULL.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[7];
//
//			arr[0] = this.findColumn("version_store_reserved_page_count")   == -1 ? 0.0d : round1(this.getAbsValueSum("version_store_reserved_page_count")  / 128.0d);
//			arr[1] = this.findColumn("internal_object_reserved_page_count") == -1 ? 0.0d : round1(this.getAbsValueSum("internal_object_reserved_page_count")/ 128.0d);
//			arr[2] = this.findColumn("user_object_reserved_page_count")     == -1 ? 0.0d : round1(this.getAbsValueSum("user_object_reserved_page_count")    / 128.0d);
//			arr[3] = this.findColumn("mixed_extent_page_count")             == -1 ? 0.0d : round1(this.getAbsValueSum("mixed_extent_page_count")            / 128.0d);
////			arr[?] = this.findColumn("modified_extent_page_count")          == -1 ? 0.0d : round1(this.getAbsValueSum("modified_extent_page_count")         / 128.0d);
//
//			arr[4] = this.findColumn("allocated_extent_page_count")         == -1 ? 0.0d : round1(this.getAbsValueSum("allocated_extent_page_count")        / 128.0d);
//			arr[5] = this.findColumn("unallocated_extent_page_count")       == -1 ? 0.0d : round1(this.getAbsValueSum("unallocated_extent_page_count")      / 128.0d);
//			arr[6] = this.findColumn("total_page_count")                    == -1 ? 0.0d : round1(this.getAbsValueSum("total_page_count")                   / 128.0d);
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}
//
//		// -----------------------------------------------------------------------------------------
//		if (GRAPH_NAME_TEMPDB_USAGE_SUM_SMALL.equals(tgdp.getName()))
//		{
//			Double[] arr = new Double[5];
//
//			arr[0] = this.findColumn("version_store_reserved_page_count")   == -1 ? 0.0d : round1(this.getAbsValueSum("version_store_reserved_page_count")  / 128.0d);
//			arr[1] = this.findColumn("internal_object_reserved_page_count") == -1 ? 0.0d : round1(this.getAbsValueSum("internal_object_reserved_page_count")/ 128.0d);
//			arr[2] = this.findColumn("user_object_reserved_page_count")     == -1 ? 0.0d : round1(this.getAbsValueSum("user_object_reserved_page_count")    / 128.0d);
//			arr[3] = this.findColumn("mixed_extent_page_count")             == -1 ? 0.0d : round1(this.getAbsValueSum("mixed_extent_page_count")            / 128.0d);
////			arr[?] = this.findColumn("modified_extent_page_count")          == -1 ? 0.0d : round1(this.getAbsValueSum("modified_extent_page_count")         / 128.0d);
//
//			arr[4] = this.findColumn("allocated_extent_page_count")         == -1 ? 0.0d : round1(this.getAbsValueSum("allocated_extent_page_count")        / 128.0d);
//
//			// Set the values
//			tgdp.setDataPoint(this.getTimestamp(), arr);
//		}
//	}



//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmMemoryUsagePanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("type");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String sql = ""
			    + "SELECT /* ${cmCollectorName} */ \n"
			    + "     [type] \n"
			    + "    ,count(*) as [InstanceCount] \n"
			    + "    ,cast(SUM([pages_kb]) / 1024.0 as numeric(12,1)) AS [SizeMb] \n"
			    + "    ,cast(SUM([pages_kb]) / 1024.0 as numeric(12,1)) AS [SizeMb_diff] \n"
			    + "    ,cast(AVG([pages_kb]) / 1024.0 as numeric(12,1)) AS [AvgMb] \n"
			    + "    ,cast(MAX([pages_kb]) / 1024.0 as numeric(12,1)) AS [MaxMb] \n"
			    + "    ,cast(MIN([pages_kb]) / 1024.0 as numeric(12,1)) AS [MinMb] \n"
			    + "FROM sys.dm_os_memory_clerks \n"
			    + "GROUP BY [type] \n"
			    + "ORDER BY SUM([pages_kb]) DESC \n"
			    + "";

		return sql;
	}
}
