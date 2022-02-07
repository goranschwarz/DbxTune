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
public class CmPlanCacheHistory
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPlanCacheHistory.class.getSimpleName();
	public static final String   SHORT_NAME       = "Plan Cache History";
	public static final String   HTML_DESC        = 
		"<html> \n" +
		"<p>How much Query Plan History does SQL Server have available?</p> \n" +
		"Looking at the top ## plans in the cache doesn't do me much good if: \n" +
		"<ul> \n" +
		"    <li>Someone restarted the server recently</li> \n" +
		"    <li>Someone ran DBCC FREEPROCCACHE</li> \n" +
		"    <li>Somebody's addicted to rebuilding indexes and updating stats (which invalidates plans for all affected objects)</li> \n" +
		"    <li>The server's under extreme memory pressure</li> \n" +
		"    <li>The developers <i>aren't parameterizing their queries</i></li> \n" +
		"    <li>The app has an <i>old version of NHibernate with the parameterization bug</i></li> \n" +
		"    <li>The .NET app calls <i>Parameters.Add without setting the parameter size</i></li> \n" +
//		"    <li>The Java app gets <code>CONVERT_IMPLICIT</code> when call <i><code>stmnt.setString()</code> on columns that has VARCHAR... Workaround: set connection parameter <code>sendStringParametersAsUnicode=false</code></i></li> \n" +
		"</ul> \n" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"plan_count_diff",
		"exec_count_diff"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

	@Override public int     getDefaultPostponeTime()                 { return 3600; } // 1 Hour
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

		return new CmPlanCacheHistory(counterController, guiController);
	}

	public CmPlanCacheHistory(ICounterController counterController, IGuiController guiController)
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
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		return null;
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("creation_date");
		pkCols.add("creation_hour");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_exec_query_stats = "dm_exec_query_stats";
		
//		if (isAzure)
//			dm_exec_query_stats = "dm_exec_query_stats";

		// FIXME: possibly we can check for
		//          * Spill to disk
		//			* CONVERT_IMPLICIT 
		//			* memory grant issues
		//			* etc, etc
		//        by "checking" the plans for various warnings/errors
		//        and add each "warning" as a specific column to the below SQL Statement
		
		String sql = ""
			    + "/* Below SQL is from -- https://www.brentozar.com/archive/2018/07/tsql2sday-how-much-plan-cache-history-do-you-have/ */ \n"
			    + "SELECT TOP 50 /* ${cmCollectorName} */ \n"
			    + "     creation_date   = CAST(creation_time AS date) \n"
			    + "    ,creation_hour   = CASE \n"
			    + "                           WHEN CAST(creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
			    + "                           ELSE DATEPART(hh, creation_time) \n"
			    + "                       END \n"

			    + "    ,plan_count      = SUM(1) \n"
			    + "    ,exec_count      = SUM(execution_count) \n"

			    + "    ,plan_count_diff = SUM(1) \n"
			    + "    ,exec_count_diff = SUM(execution_count) \n"

			    + "FROM sys." + dm_exec_query_stats + " \n"
			    + "GROUP BY CAST(creation_time AS date), \n"
			    + "         CASE \n"
			    + "             WHEN CAST(creation_time AS date) <> CAST(GETDATE() AS date) THEN -1 \n"
			    + "             ELSE DATEPART(hh, creation_time) \n"
			    + "         END \n"
			    + "ORDER BY 1 DESC, 2 DESC \n"
			    + "";

		return sql;
	}
}
