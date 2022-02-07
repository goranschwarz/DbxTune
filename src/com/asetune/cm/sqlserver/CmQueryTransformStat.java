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
public class CmQueryTransformStat
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmQueryTransformStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "Query Transformation";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Query optimization is a recursive process that starts at the root of a logical operator tree, and ends by producing a physical representation suitable for execution.</p>" +
		"<p>The space of possible plans is explored by applying rules that result in a logical transformation of some part of the current plan, or a conversion of that part to a particular physical implementation.</p>" +
		"<p>The optimizer does not try to match every available rule to every part of every query, in every possible combination. That sort of exhaustive approach would guarantee the best plan possible, but you would not like the compilation times, or the memory usage!</p>" +
		"<p>To find a good plan quickly, the optimizer uses a number of techniques. I plan to cover these in some detail in future posts, but two of these tricks are immediately relevant to the DMV:</p>" +
		"<ul>" +
		"  <li>Every logical operator contains code to describe all the rules that are capable of matching with it. This saves the optimizer from trying rules which have no chance of producing a lower-cost plan.</li>" +
		"  <li>Every rule contains code to compute a value to indicate how promising the rule is in the current context. A rule has a higher promise value if it has a high potential to reduce the cost of the overall plan."
		+ "<br>"
		+ "<br>"
		+ "In general, commonly-used optimizations (like pushing a predicate) have a high ‘promise’ value. More specialised rules, like those that match indexed views may have a lower promise value."
		+ "</li>" +
		"</ul>" +
		"<p>When faced with several possible rule choices, the optimizer uses promise values as part of its strategy. This helps reduce compilation time, while still pursuing the most promising transformations.</p>" +
		"</html>";
	// for desc maybe use: https://www.sql.kiwi/2010/07/inside-the-optimiser-constructing-a-plan-part-3.html

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_query_transformation_stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"promised",
		"promise_total",
		"built_substitute",
		"succeeded"
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

		return new CmQueryTransformStat(counterController, guiController);
	}

	public CmQueryTransformStat(ICounterController counterController, IGuiController guiController)
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
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_exec_query_optimizer_info = "dm_exec_query_transformation_stats";
		
		if (isAzure)
			dm_exec_query_optimizer_info = "dm_pdw_nodes_exec_query_transformation_stats"; // Just guessed here...
		
		String sql = "select *    /* ${cmCollectorName} */ \n" 
		           + "from sys." + dm_exec_query_optimizer_info;

		return sql;
	}
}
