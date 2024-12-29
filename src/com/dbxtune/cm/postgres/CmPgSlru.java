/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.postgres;

import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.central.pcs.CentralPersistReader;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.graph.TrendGraphDataPoint;
import com.dbxtune.graph.TrendGraphDataPoint.LabelType;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgSlru
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgWal.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgSlru.class.getSimpleName();
	public static final String   SHORT_NAME       = "SLRU Caches";
	public static final String   HTML_DESC        = 
		"<html>" +
		"PostgreSQL accesses certain on-disk information via SLRU (simple least-recently-used) caches. <br>" +
		"The pg_stat_slru view will contain one row for each tracked SLRU cache, showing statistics about access to cached pages." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_CACHE;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(13);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_slru"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
//			"blks_zeroed",  // Number of blocks zeroed during initializations
			"blks_hit",     // Number of times disk blocks were found already in the SLRU, so that a read was not necessary (this only includes hits in the SLRU, not the operating system's file system cache)
			"blks_read",    // Number of disk blocks read for this SLRU
			"blks_written", // Number of disk blocks written for this SLRU
			"blks_exists",  // Number of blocks checked for existence for this SLRU
			"flushes",      // Number of flushes of dirty data for this SLRU
			"truncates"     // Number of truncates for this SLRU
	};
	
	// RS> Col# Label        JDBC Type Name           Guessed DBMS type Source Table
	// RS> ---- ------------ ------------------------ ----------------- ------------
	// RS> 1    name         java.sql.Types.VARCHAR   text              pg_stat_slru
	// RS> 2    blks_zeroed  java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 3    blks_hit     java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 4    blks_read    java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 5    blks_written java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 6    blks_exists  java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 7    flushes      java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 8    truncates    java.sql.Types.BIGINT    int8              pg_stat_slru
	// RS> 9    stats_reset  java.sql.Types.TIMESTAMP timestamptz       pg_stat_slru

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmPgSlru(counterController, guiController);
	}

	public CmPgSlru(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_HIT            = "SlruHit";
	public static final String GRAPH_NAME_READS          = "SlruReads";
	public static final String GRAPH_NAME_WRITES         = "SlruWrites";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_HIT,
				"Cache Hits [blks_hit] per Second", 	                // Menu CheckBox text
				"Cache Hits [blks_hit] per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(13),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_READS,
				"Cache Reads [blks_read] per Second", 	                // Menu CheckBox text
				"Cache Reads [blks_read] per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(13),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITES,
				"Cache Writes [blks_written] per Second", 	                // Menu CheckBox text
				"Cache Writes [blks_written] per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				null, 
				LabelType.Dynamic, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(13),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//---------------------------------
		if (GRAPH_NAME_HIT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "name");
				dArray[i] = this.getAbsValueAsDouble(i, "blks_hit");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		//---------------------------------
		if (GRAPH_NAME_READS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "name");
				dArray[i] = this.getAbsValueAsDouble(i, "blks_read");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		//---------------------------------
		if (GRAPH_NAME_WRITES.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString       (i, "name");
				dArray[i] = this.getAbsValueAsDouble(i, "blks_written");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// Possibly calculate hit_pct ... 
		return "select * from pg_stat_slru";
	}
}
