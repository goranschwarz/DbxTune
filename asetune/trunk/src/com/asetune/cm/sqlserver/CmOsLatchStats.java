/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.SqlServerLatchClassDictionary;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOsLatchStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmOsLatchStats.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmOsLatchStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Latch Stats";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Show information about LATCH, from table 'sys.dm_os_latch_stats'.</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_latch_stats"};
	public static final String[] NEED_ROLES       = new String[] {};//{"VIEW SERVER STATE"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"waiting_requests_count", 
		"wait_time_ms"
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

		return new CmOsLatchStats(counterController, guiController);
	}

	public CmOsLatchStats(ICounterController counterController, IGuiController guiController)
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
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("latch_class");

		return pkCols;
	}

//	RS> Col# Label                  JDBC Type Name          Guessed DBMS type Source Table
//	RS> ---- ---------------------- ----------------------- ----------------- ------------
//	RS> 1    latch_class            java.sql.Types.NVARCHAR nvarchar(60)      -none-      
//	RS> 2    waiting_requests_count java.sql.Types.BIGINT   bigint            -none-      
//	RS> 3    wait_time_ms           java.sql.Types.BIGINT   bigint            -none-      
//	RS> 4    max_wait_time_ms       java.sql.Types.BIGINT   bigint            -none-      

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
				  "select "
				+ "    latch_class, \n"
				+ "    waiting_requests_count, \n"
				+ "    wait_time_ms, \n"
				+ "    max_wait_time_ms, \n"
				+ "    waitTimePerCount = CASE WHEN waiting_requests_count > 0 \n"
				+ "                            THEN convert(numeric(12,1), (wait_time_ms*1.0) / waiting_requests_count) \n"
				+ "                            ELSE 0 \n"
				+ "                       END, \n"
				+ "    Description = convert(varchar(1500), '') \n"
				+ "from sys.dm_os_latch_stats \n"
				+ "";

		return sql;
	}

	/** 
	 * Fill in the Description column with data from
	 * SqlServerWaitNameDictionary.. transforms a wait_type -> text description
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Where are various columns located in the Vector 
		int pos_latch_class = -1, pos_Description = -1;
	
		SqlServerLatchClassDictionary dict = SqlServerLatchClassDictionary.getInstance();
		if (dict == null)
			return;

		if (newSample == null)
			return;

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);

			if      (colName.equals("latch_class")) pos_latch_class = colId;
			else if (colName.equals("Description")) pos_Description = colId;
		}

		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_wait_type  = newSample.getValueAt(rowId, pos_latch_class);

			if (o_wait_type instanceof String)
			{
				String desc = dict.getDescriptionPlain( (String)o_wait_type );

				if (desc != null)
					newSample.setValueAt(desc, rowId, pos_Description);
			}
		}
	}
}
