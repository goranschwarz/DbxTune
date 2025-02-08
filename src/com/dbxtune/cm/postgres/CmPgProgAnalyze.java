/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.postgres;

import java.util.LinkedList;
import java.util.List;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.MainFramePostgres;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgProgAnalyze
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgProgAnalyze.class.getSimpleName();
	public static final String   SHORT_NAME       = "Analyze";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Whenever ANALYZE is running, the pg_stat_progress_analyze view will contain a row for each backend that is currently running that command." +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_PROGRESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(13);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_progress_analyze"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			 "sample_blks_pct"
			,"ext_stats_pct"
			,"child_tables_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {};
	
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

		return new CmPgProgAnalyze(counterController, guiController);
	}

	public CmPgProgAnalyze(ICounterController counterController, IGuiController guiController)
	{
		super(counterController, guiController,
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
//		return new CmPgProgAnalyzePanel(this);
//	}


	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		List <String> pkCols = new LinkedList<String>();

//		pkCols.add("dbname");
//		pkCols.add("funcid");

		return pkCols;
	}

// View "pg_catalog.pg_stat_progress_analyze"
// Column                     |  Type   | Collation | Nullable | Default 
// ---------------------------+---------+-----------+----------+---------
// pid                        | integer |           |          | 
// datid                      | oid     |           |          | 
// datname                    | name    |           |          | 
// relid                      | oid     |           |          | 
// phase                      | text    |           |          | 
// sample_blks_total          | bigint  |           |          | 
// sample_blks_scanned        | bigint  |           |          | 
// ext_stats_total            | bigint  |           |          | 
// ext_stats_computed         | bigint  |           |          | 
// child_tables_total         | bigint  |           |          | 
// child_tables_done          | bigint  |           |          | 
// current_child_table_relid  | oid     |           |          |
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return ""
				+ "select \n"
				+ "          pid \n"

				+ "    ,cast(datid                         as bigint      )  AS datid   \n"
				+ "    ,cast(relid                         as bigint      )  AS relid   \n"
				+ "    ,cast(current_child_table_relid     as bigint      )  AS current_child_table_relid \n"

				+ "    ,cast(datname                       as varchar(128))  AS datname \n"
				+ "    ,cast(''                            as varchar(128))  AS schema_name   \n"
				+ "    ,cast(''                            as varchar(128))  AS relation_name \n"
				+ "    ,cast(''                            as varchar(128))  AS current_child_table_name \n"
				
				+ "    ,cast(phase                         as varchar(128))  AS phase   \n"
				+ "    ,     sample_blks_total    \n"
				+ "    ,     sample_blks_scanned  \n"
				+ "    ,cast(sample_blks_scanned * 1.0 / nullif(sample_blks_total  ,0) * 100.0 as numeric(9,1))  AS sample_blks_pct \n"
				+ "    ,     ext_stats_total      \n"
				+ "    ,     ext_stats_computed   \n"
				+ "    ,cast(ext_stats_computed  * 1.0 / nullif(ext_stats_total    ,0) * 100.0 as numeric(9,1))  AS ext_stats_pct \n"
				+ "    ,     child_tables_total   \n"
				+ "    ,     child_tables_done    \n"
				+ "    ,cast(child_tables_done   * 1.0 / nullif(child_tables_total ,0) * 100.0 as numeric(9,1))  AS child_tables_pct \n"

				+ "from pg_stat_progress_analyze";
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		PostgresCmHelper.resolveSchemaAndRelationName(newSample, "datid", "relid"                    , null, "schema_name", "relation_name");
		PostgresCmHelper.resolveSchemaAndRelationName(newSample, "datid", "current_child_table_relid", null, null         , "current_child_table_name");
	}
}
