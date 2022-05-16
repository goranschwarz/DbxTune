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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSampleCatalogIteratorPostgres;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgFunctions
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgTables.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgFunctions.class.getSimpleName();
	public static final String   SHORT_NAME       = "Function Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"One row for each tracked function, showing statistics about executions of that function." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_user_functions"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"calls",
			"total_time",
			"self_time"
	};
	
//	RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table          
//	RS> ---- ---------- ---------------------- ----------------- ----------------------
//	RS> 1    funcid     java.sql.Types.BIGINT  oid               pg_stat_user_functions
//	RS> 2    schemaname java.sql.Types.VARCHAR name(2147483647)  pg_stat_user_functions
//	RS> 3    funcname   java.sql.Types.VARCHAR name(2147483647)  pg_stat_user_functions
//	RS> 4    calls      java.sql.Types.BIGINT  int8              pg_stat_user_functions
//	RS> 5    total_time java.sql.Types.DOUBLE  float8            pg_stat_user_functions
//	RS> 6    self_time  java.sql.Types.DOUBLE  float8            pg_stat_user_functions
	
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

		return new CmPgFunctions(counterController, guiController);
	}

	public CmPgFunctions(ICounterController counterController, IGuiController guiController)
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
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmPgFunctionsPanel(this);
//	}


	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("dbname");
		pkCols.add("funcid");

		return pkCols;
	}

	/**
	 * Create a special CounterSample, that will iterate over all databases that we will interrogate
	 */
	@Override
	public CounterSample createCounterSample(String name, boolean negativeDiffCountersToZero, String[] diffColumns, CounterSample prevSample)
	{
		List<String> fallbackDbList = Arrays.asList( new String[]{"postgres"} );
		return new CounterSampleCatalogIteratorPostgres(name, negativeDiffCountersToZero, diffColumns, prevSample, fallbackDbList);
	}

	@Override
	public void close()
	{
		CounterSampleCatalogIteratorPostgres.closeConnPool();
		super.close();
	}
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return "select current_database() as dbname, * from pg_catalog.pg_stat_user_functions";
	}
}
