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
package com.asetune.cm.postgres;

import java.sql.Types;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.NumberUtils;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgWal
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgWal.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgWal.class.getSimpleName();
	public static final String   SHORT_NAME       = "Wal Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row only, containing data about WAL activity of the cluster.<br> <i>(WAL = Write Ahead Log, or the Transaction log)</i>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_wal"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"wal_records",
			"wal_fpi",
			"wal_bytes",
			"wal_buffers_full",
			"wal_write",
			"wal_sync",
			"wal_write_time",
			"wal_sync_time"
	};
	
	// RS> Col# Label            JDBC Type Name           Guessed DBMS type Source Table
	// RS> ---- ---------------- ------------------------ ----------------- ------------
	// RS> 1    wal_records      java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 2    wal_fpi          java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 3    wal_bytes        java.sql.Types.NUMERIC   numeric(0,0)      pg_stat_wal 
	// RS> 4    wal_buffers_full java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 5    wal_write        java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 6    wal_sync         java.sql.Types.BIGINT    int8              pg_stat_wal 
	// RS> 7    wal_write_time   java.sql.Types.DOUBLE    float8            pg_stat_wal 
	// RS> 8    wal_sync_time    java.sql.Types.DOUBLE    float8            pg_stat_wal 
	// RS> 9    stats_reset      java.sql.Types.TIMESTAMP timestamptz       pg_stat_wal 	

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

		return new CmPgWal(counterController, guiController);
	}

	public CmPgWal(ICounterController counterController, IGuiController guiController)
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
	public static final String GRAPH_NAME_RECORDS     = "Records";
	public static final String GRAPH_NAME_WRITE_COUNT = "WCount";
	public static final String GRAPH_NAME_WRITE_TIME  = "WTime";
	public static final String GRAPH_NAME_WAL_KB      = "WalKb";
	public static final String GRAPH_NAME_FULL_COUNT  = "FullCount";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_RECORDS,
				"WAL Records per Second", 	                // Menu CheckBox text
				"WAL Records per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"wal_records"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_COUNT,
				"WAL Writes per Second", 	                // Menu CheckBox text
				"WAL Writes per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"wal_write", "wal_sync"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WRITE_TIME,
				"WAL Write Time in ms", 	                // Menu CheckBox text
				"WAL Write Time in ms ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"wal_write_time", "wal_sync_time"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_WAL_KB,
				"WAL KB Written per Second", 	                // Menu CheckBox text
				"WAL KB Written per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_KB,
				new String[] {"wal_kb"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_FULL_COUNT,
				"WAL Buffer was Full Writes per Second", 	                // Menu CheckBox text
				"WAL Buffer was Full Writes per Second ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"wal_buffers_full"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.DISK,
				false, // is Percent Graph
				false, // visible at start
				Ver.ver(14),     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		//---------------------------------
		if (GRAPH_NAME_RECORDS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "wal_records", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WRITE_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble(0, "wal_write", 0d);
			arr[1] = this.getRateValueAsDouble(0, "wal_sync", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WRITE_TIME.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble(0, "wal_write_time", 0d);
			arr[1] = this.getRateValueAsDouble(0, "wal_sync_time", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_WAL_KB.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = NumberUtils.round(this.getRateValueAsDouble(0, "wal_bytes", 0d) / 1024.0, 1);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		//---------------------------------
		if (GRAPH_NAME_FULL_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = this.getRateValueAsDouble(0, "wal_buffers_full", 0d);

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("pk");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// possibly add: wal_write_time / wal_write AS wal_write_time_per_write   (it also needs to be maintained for DIFF level)
		// possibly add: wal_sync_time  / wal_sync  AS wal_sync_time_per_write    (it also needs to be maintained for DIFF level) 
		return "select * from pg_stat_wal";

//		String sql = ""
//			    + "select \n"
//			    + "     wal_records \n"
//			    + "    ,wal_fpi \n"
//			    + "    ,CAST(wal_bytes as numeric(38,0)) AS wal_bytes /* need to cast, pg uses: numeric(0,0) */ \n"
//			    + "    ,wal_buffers_full \n"
//			    + "    ,wal_write \n"
//			    + "    ,wal_sync \n"
//			    + "    ,wal_write_time \n"
//			    + "    ,wal_sync_time \n"
//			    + "    ,stats_reset \n"
//			    + "from pg_stat_wal \n"
//			    + "";
//		
//		return sql;
	}

	@Override
	public ResultSetMetaDataCached modifyResultSetMetaData(ResultSetMetaDataCached rsmdc)
	{
		for (Entry entry : rsmdc.getEntries())
		{
			if (    "wal_bytes".equals(entry.getColumnLabel()) 
			     && entry.getColumnType() == Types.NUMERIC 
			     && entry.getPrecision() == 0 
			     && entry.getScale() == 0
			   ) 
			{
				entry.setPrecision(38);
				entry.setScale(0);

				_logger.info("modifyResultSetMetaData: Cm='" + getName() + "', columnName='" + entry.getColumnLabel() + "', changing data type PRECISION from 0 to 38");
			}
		}
		
		return rsmdc;
	}

}
