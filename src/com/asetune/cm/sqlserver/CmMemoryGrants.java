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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.ICounterController.DbmsOption;
import com.asetune.IGuiController;
import com.asetune.cm.CmSettingsHelper;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.sqlserver.gui.CmMemoryGrantsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SqlServerUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmMemoryGrants
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmMemoryGrants.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmMemoryGrants.class.getSimpleName();
	public static final String   SHORT_NAME       = "Memory Grants";
	public static final String   HTML_DESC        = 
		"<html>" +
			"<p>Information about SPID's that requests memory grants.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_memory_grants"};
	public static final String[] NEED_ROLES       = new String[] {"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {};

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

		return new CmMemoryGrants(counterController, guiController);
	}

	public CmMemoryGrants(ICounterController counterController, IGuiController guiController)
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

	public static final String  PROPKEY_sample_sqlText             = PROP_PREFIX + ".sample.sqlText";
	public static final boolean DEFAULT_sample_sqlText             = true;

	public static final String  PROPKEY_sample_queryPlan           = PROP_PREFIX + ".sample.queryPlan";
	public static final boolean DEFAULT_sample_queryPlan           = true;

	public static final String  PROPKEY_sample_liveQueryPlan       = PROP_PREFIX + ".sample.liveQueryPlan";
	public static final boolean DEFAULT_sample_liveQueryPlan       = true;

//	public static final String  PROPKEY_init_liveQueryPlan         = PROP_PREFIX + ".init.liveQueryPlan";
//	public static final boolean DEFAULT_init_liveQueryPlan         = false;

	
	private void addTrendGraphs()
	{
	}

//	@Override
//	public void updateGraphData(TrendGraphDataPoint tgdp)
//	{
//	}



	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmMemoryGrantsPanel(this);
	}

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Get SQL Text",         PROPKEY_sample_sqlText      , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_sqlText      , DEFAULT_sample_sqlText      ), true, "Also get SQL Text"  ));
		list.add(new CmSettingsHelper("Get Query Plan",       PROPKEY_sample_queryPlan    , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_queryPlan    , DEFAULT_sample_queryPlan    ), true, "Also get queryplan" ));
		list.add(new CmSettingsHelper("Get Live Query Plan",  PROPKEY_sample_liveQueryPlan, Boolean.class, conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan, DEFAULT_sample_liveQueryPlan), true, "Also get LIVE queryplan" ));
//		list.add(new CmSettingsHelper("Init Live Query Plan", PROPKEY_init_liveQueryPlan  , Boolean.class, conf.getBooleanProperty(PROPKEY_init_liveQueryPlan  , DEFAULT_init_liveQueryPlan)  , true, "Initialize get LIVE queryplan using 'dbcc traceon(2451, -1) with no_infomsgs' when this CM starts." ));

		return list;
	}

	
	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("session_id");

//		return pkCols;
		return null;
	}

//	@Override
//	public String getSqlInitForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
//	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		boolean getLiveQueryPlan  = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan,  DEFAULT_sample_liveQueryPlan);
//		boolean initLiveQueryPlan = conf == null ? true : conf.getBooleanProperty(PROPKEY_init_liveQueryPlan,    DEFAULT_init_liveQueryPlan);
//
//		if (srvVersion >= Ver.ver(2019) && getLiveQueryPlan && initLiveQueryPlan)
//		{
//			return "dbcc traceon(2451, -1) with no_infomsgs";
//		}
//		
//		return null;
//	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_exec_query_plan = "dm_exec_query_plan";

		// get Actual-Query-Plan instead of Estimated-QueryPlan
		if (isDbmsOptionEnabled(DbmsOption.SQL_SERVER__LAST_QUERY_PLAN_STATS))
		{
			dm_exec_query_plan = "dm_exec_query_plan_stats";
		}

		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getSqltext        = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_sqlText,        DEFAULT_sample_sqlText);
		boolean getQueryPlan      = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_queryPlan,      DEFAULT_sample_queryPlan);
//		boolean getLiveQueryPlan  = conf == null ? true : conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan,  DEFAULT_sample_liveQueryPlan);


		String sql = ""
				+ "SELECT \n"
				+ "     mg.* \n"
				+ (getSqltext   ? "    ,st.[TEXT] AS [sql_text] \n" : "    ,cast('' as varchar(max)) as [sql_text]   \n")
				+ (getQueryPlan ? "    ,qp.[query_plan] \n"         : "    ,cast('' as varchar(max)) as [query_plan] \n")
				+ "    ,cast(null as varchar(max)) as [live_query_plan] \n"
				+ "FROM sys.dm_exec_query_memory_grants mg \n"
			    + (getSqltext   ? "OUTER APPLY sys.dm_exec_sql_text  (mg.plan_handle) AS st \n"         : "")
			    + (getQueryPlan ? "OUTER APPLY sys." + dm_exec_query_plan + "(mg.plan_handle) AS qp \n" : "")
//				+ "WHERE mg.session_id != @@spid \n"
//				+ "ORDER BY mg.required_memory_kb DESC \n"
				+ "";

		return sql;
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean getLiveQueryPlan  = conf.getBooleanProperty(PROPKEY_sample_liveQueryPlan,  DEFAULT_sample_liveQueryPlan);

		if ( ! getLiveQueryPlan )
			return;

		// Find column Id's
		int pos_spid          = newSample.findColumn("session_id");
		int pos_liveQueryPlan = newSample.findColumn("live_query_plan");

		if (pos_spid < 0 || pos_liveQueryPlan < 0)
		{
			_logger.info("Cant find column 'session_id'="+pos_spid+" or 'live_query_plan'="+pos_liveQueryPlan + ". Skipping fetch/update of 'live_query_plan'.");
			return;
		}

		// Loop on all rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_SPID        = newSample.getValueAt(rowId, pos_spid);
			
			if (o_SPID instanceof Number)
			{
				int spid = ((Number)o_SPID).intValue();
				
				if (getLiveQueryPlan)  
				{
					if (getServerVersion() >= Ver.ver(2016,0,0, 1)) // 2016 SP1
					{
						String liveQueryPlan = SqlServerUtils.getLiveQueryPlanNoThrow(getCounterController().getMonConnection(), spid);

						if (StringUtil.isNullOrBlank(liveQueryPlan))
							liveQueryPlan = "-not-found-";

						newSample.setValueAt(liveQueryPlan, rowId, pos_liveQueryPlan);
					}
				}
			}
		}
	}
}
