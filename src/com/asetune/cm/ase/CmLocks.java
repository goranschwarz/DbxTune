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
package com.asetune.cm.ase;

import java.util.ArrayList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.ase.gui.CmLocksPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmLocks
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmLocks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmLocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Locks";
	public static final String   HTML_DESC        = 
		"<html>" +
			"normal Locks in the system." +
			"<br><br>" +
//			"Background colors:" +
//			"<ul>" +
//			"    <li>PINK   - SPID is Blocked by some other SPID that holds a Lock on a database object Table, Page or Row. This is the Lock Victim.</li>" +
//			"    <li>RED    - SPID is Blocking other SPID's from running, this SPID is Responsible or the Root Cause of a Blocking Lock.</li>" +
//			"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monLocks"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmLocks(counterController, guiController);
	}

	/**
	 * Constructor
	 */
	public CmLocks(ICounterController counterController, IGuiController guiController)
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
		
//		CounterSetTemplates.register(this);
	}
	
	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_extraWhereClause   = PROP_PREFIX + ".sample.extraWhereClause";
	public static final String  DEFAULT_sample_extraWhereClause   = "";

	public static final String GRAPH_NAME_LOCK_COUNT = "LockCountGraph";

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_LOCK_COUNT,
				"Lock Count", 	                           // Menu CheckBox text
				"Number of Current Locks Held in the Server ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
				TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_NORMAL, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
				new String[] { "Lock Count" }, 
				LabelType.Static,
				TrendGraphDataPoint.Category.LOCK,
				false, // is Percent Graph
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);   // minimum height

	}

	
	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);
	}
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Extra Where Clause", PROPKEY_sample_extraWhereClause , String.class, conf.getProperty(PROPKEY_sample_extraWhereClause , DEFAULT_sample_extraWhereClause ), DEFAULT_sample_extraWhereClause, CmLocksPanel.TOOLTIP_sample_extraWhereClause ));

		return list;
	}


	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmLocksPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		String cols1 = "";
		String cols2 = "";
		String cols3 = "";

		Configuration conf = Configuration.getCombinedConfiguration();
		String  sample_extraWhereClause = conf.getProperty(PROPKEY_sample_extraWhereClause, DEFAULT_sample_extraWhereClause);

		// Do we have extra where clauses
		String sql_sample_extraWhereClause = "  -- Extra where clauses will go here. (it will look like: AND the_extra_where_clause) \n";
		if ( ! StringUtil.isNullOrBlank(sample_extraWhereClause) )
			sql_sample_extraWhereClause = "  and " + sample_extraWhereClause + "\n";

		cols1 = "SPID, KPID, DBID, ParentSPID, LockID, Context, \n" +
				"ObjectID, DBName=db_name(DBID), ObjectName=object_name(ObjectID, DBID), \n" +
				"LockState, LockType, LockLevel, ";
		cols2 = "";
		cols3 = "WaitTime, PageNumber, RowNumber";

		if (srvVersion >= Ver.ver(15,0,0,2))
		{
			cols2 = "BlockedState, BlockedBy, ";  //
		}

		if (srvVersion >= Ver.ver(15,0,2))
		{
			cols3 += ", SourceCodeID";  //
		}

		if (srvVersion >= Ver.ver(16,0))
		{
			cols3 += ", PartitionID";  //
		}
		
		String sql =
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monLocks L \n" +
			"where 1 = 1 \n" +
			sql_sample_extraWhereClause;

		return sql;
	}

	
	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LOCK_COUNT.equals(tgdp.getName()))
		{
			Double[] arr = new Double[1];

			arr[0] = new Double( this.getCounterDataAbs().getRowCount() );
			
			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	/** Normal updateGraphData() skips update if no rows in table... but we still want to update */
	@Override
	public void updateGraphData()
	{
		for (TrendGraphDataPoint tgdp : getTrendGraphData().values()) 
			updateGraphData(tgdp);
	}
}
