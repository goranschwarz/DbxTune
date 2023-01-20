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

import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgBgWriter
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgBgWriter.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgBgWriter.class.getSimpleName();
	public static final String   SHORT_NAME       = "BG Writer";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row only, showing statistics about the background writer process's activity." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_bgwriter"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"checkpoints_timed",
			"checkpoints_req",
			"checkpoint_write_time",
			"checkpoint_sync_time",
			"buffers_checkpoint",
			"buffers_clean",
			"maxwritten_clean",
			"buffers_backend",
			"buffers_backend_fsync",
			"buffers_alloc"
	};
	
//	RS> Col# Label                 JDBC Type Name           Guessed DBMS type Source Table    
//	RS> ---- --------------------- ------------------------ ----------------- ----------------
//	RS> 1    checkpoints_timed     java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 2    checkpoints_req       java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 3    checkpoint_write_time java.sql.Types.DOUBLE    float8            pg_stat_bgwriter
//	RS> 4    checkpoint_sync_time  java.sql.Types.DOUBLE    float8            pg_stat_bgwriter
//	RS> 5    buffers_checkpoint    java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 6    buffers_clean         java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 7    maxwritten_clean      java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 8    buffers_backend       java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 9    buffers_backend_fsync java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 10   buffers_alloc         java.sql.Types.BIGINT    int8              pg_stat_bgwriter
//	RS> 11   stats_reset           java.sql.Types.TIMESTAMP timestamptz       pg_stat_bgwriter	
	
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

		return new CmPgBgWriter(counterController, guiController);
	}

	public CmPgBgWriter(ICounterController counterController, IGuiController guiController)
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

//TODO; Possibly add some graphs... see: https://github.com/postgrespro/mamonsu

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_BUFFER           = "Buffer";
	public static final String GRAPH_NAME_BUFFER_EVENTS    = "BufferEvents";
	public static final String GRAPH_NAME_CHECKPOINT_COUNT = "CheckpointCount";
	public static final String GRAPH_NAME_CHECKPOINT_TIME  = "CheckpointTime";
	
//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmActiveStatementsPanel(this);
//	}
	
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
//		return "select *, 1 as PK from pg_catalog.pg_stat_bgwriter";
		return "select * from pg_stat_bgwriter";
	}

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_BUFFER,
				"Buffers", 	                // Menu CheckBox text
				"Buffers ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"Written During Checkpoints [buffers_checkpoint]", "Written [buffers_clean]", "Written Directly by a Backend [buffers_backend]", "Allocated [buffers_alloc]"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_BUFFER_EVENTS,
				"Buffer Events", 	                // Menu CheckBox text
				"Buffer Events ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"Number of bgwriter Stopped by Max Write Count [maxwritten_clean]", "Times a Backend Execute Its Own Fsync [buffers_backend_fsync]"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CHECKPOINT_COUNT,
				"Checkpoints Count", 	                // Menu CheckBox text
				"Checkpoints Count ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL,
				new String[] {"By Timeout [checkpoints_timed]", "By WAL [checkpoints_req]"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

		addTrendGraph(GRAPH_NAME_CHECKPOINT_TIME,
				"Checkpoints Time in ms", 	                // Menu CheckBox text
				"Checkpoints Time in ms ("+SHORT_NAME+")", // Graph Label 
				TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC,
				new String[] {"Sync Time [checkpoint_sync_time]", "Write Time [checkpoint_write_time]"}, 
				LabelType.Static, 
				TrendGraphDataPoint.Category.OTHER,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_BUFFER.equals(tgdp.getName()))
		{
			Double[] arr = new Double[4];

			arr[0] = this.getDiffValueAsDouble(0, "buffers_checkpoint");
			arr[1] = this.getDiffValueAsDouble(0, "buffers_clean");
			arr[2] = this.getDiffValueAsDouble(0, "buffers_backend");
			arr[3] = this.getDiffValueAsDouble(0, "buffers_alloc");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_BUFFER_EVENTS.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getDiffValueAsDouble(0, "maxwritten_clean");
			arr[1] = this.getDiffValueAsDouble(0, "buffers_backend_fsync");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_CHECKPOINT_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getDiffValueAsDouble(0, "checkpoints_timed");
			arr[1] = this.getDiffValueAsDouble(0, "checkpoints_req");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}

		if (GRAPH_NAME_CHECKPOINT_TIME.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];

			arr[0] = this.getDiffValueAsDouble(0, "checkpoint_sync_time");
			arr[1] = this.getDiffValueAsDouble(0, "checkpoint_write_time");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
}
