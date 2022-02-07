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
import com.asetune.cm.sqlserver.gui.CmExecCursorsPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmExecCursors
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmExecCursors.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmExecCursors.class.getSimpleName();
	public static final String   SHORT_NAME       = "Cursors";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Show information about the cursors that are open in various databases.</p>" +
		"<br>" +
		"Table (cell) Background colors:" +
		"<ul>" +
		"    <li>ORANGE - (in column 'properties') Cursor is declared as GLOBAL (a better way is: <code>DECLARE <i>CURSOR_NAME</i> cursor <b>STATIC LOCAL</b> for...</code>).</li>" +
		"</ul>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_exec_cursors"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"fetch_buffer_start",
		"worker_time",
		"reads", 
		"writes",
		"dormant_duration"
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

		return new CmExecCursors(counterController, guiController);
	}

	public CmExecCursors(ICounterController counterController, IGuiController guiController)
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmExecCursorsPanel(this);
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

		pkCols.add("session_id");
		pkCols.add("cursor_id");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isAzure)
	{
//		String dm_exec_cursors = "dm_exec_cursors";
//		
//		if (isAzure)
//			dm_exec_cursors = "dm_exec_cursors";  // SAME NAME IN AZURE ????
//
//		String sql = "select * from sys." + dm_exec_cursors + "(0)";

		String sql = ""
			    + "select /* ${cmCollectorName} */ \n" 
				+ "     c.* \n"
			    + "    ,isnull(txt.dbid, -1)     AS dbid \n"
			    + "    ,isnull(txt.objectid, -1) AS objectid \n"
			    + "    ,substring(txt.text, (c.statement_start_offset/2)+1, \n"
			    + "        ((CASE c.statement_end_offset WHEN -1 THEN datalength(txt.text) \n"
			    + "                                      ELSE c.statement_end_offset \n"
			    + "          END - c.statement_start_offset)/2) + 1) AS exec_sql_text \n"
			    + "    ,txt.text as full_sql_text \n"
			    + "from sys.dm_exec_cursors(0) c \n"
			    + "outer apply sys.dm_exec_sql_text(sql_handle) txt \n"
			    + "";

		return sql;
	}
}
