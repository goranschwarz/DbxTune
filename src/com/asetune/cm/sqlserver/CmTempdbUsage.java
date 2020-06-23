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
import com.asetune.cm.sqlserver.gui.CmTempdbSpidUsagePanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

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
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
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

	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmTempdbSpidUsagePanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("database_id");
		pkCols.add("file_id");
		pkCols.add("filegroup_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
		String dm_db_file_space_usage = "dm_db_file_space_usage";
		
		if (isAzure)
			dm_db_file_space_usage = "dm_pdw_nodes_db_file_space_usage";

		// FIXME: THIS CAN BE IMPROVED: Just a placeholder to do something
		String sql = 
			"SELECT * \n" +
			"FROM tempdb.sys."+dm_db_file_space_usage+" u \n" +
			"WHERE u.database_id = 2 \n"
			;

		return sql;
	}
}
