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
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFramePostgres;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgProgIndex
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmPgProgIndex.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgProgIndex.class.getSimpleName();
	public static final String   SHORT_NAME       = "Index";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Whenever CREATE INDEX or REINDEX is running, the pg_stat_progress_create_index view will contain one row for each backend that is currently creating indexes." +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_PROGRESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(12);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_progress_create_index"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			 "lockers_pct"
			,"blocks_pct"
			,"tuples_pct"
			,"partitions_pct"
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

		return new CmPgProgIndex(counterController, guiController);
	}

	public CmPgProgIndex(ICounterController counterController, IGuiController guiController)
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

// View "pg_catalog.pg_stat_progress_create_index"
// Column              |  Type   | Collation | Nullable | Default 
// --------------------+---------+-----------+----------+---------
// pid                 | integer |           |          | 
// datid               | oid     |           |          | 
// datname             | name    |           |          | 
// relid               | oid     |           |          | 
// index_relid         | oid     |           |          | 
// command             | text    |           |          | 
// phase               | text    |           |          | 
// lockers_total       | bigint  |           |          | 
// lockers_done        | bigint  |           |          | 
// current_locker_pid  | bigint  |           |          | 
// blocks_total        | bigint  |           |          | 
// blocks_done         | bigint  |           |          | 
// tuples_total        | bigint  |           |          | 
// tuples_done         | bigint  |           |          | 
// partitions_total    | bigint  |           |          | 
// partitions_done     | bigint  |           |          |

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return ""
				+ "select \n"
				+ "          pid \n"

				+ "    ,cast(datid         as bigint      )  AS datid         \n"
				+ "    ,cast(relid         as bigint      )  AS relid         \n"
				+ "    ,cast(index_relid   as bigint      )  AS index_relid   \n"

				+ "    ,cast(datname       as varchar(128))  AS datname       \n"
				+ "    ,cast(''            as varchar(128))  AS schema_name   \n"
				+ "    ,cast(''            as varchar(128))  AS relation_name \n"
				+ "    ,cast(''            as varchar(128))  AS index_name    \n"
				
				+ "    ,cast(command       as varchar(128))  AS command \n"
				+ "    ,cast(phase         as varchar(128))  AS phase   \n"
				+ "    ,     current_locker_pid   \n"
				+ "    ,     lockers_total        \n"
				+ "    ,     lockers_done         \n"
				+ "    ,cast(lockers_done    * 1.0 / nullif(lockers_total    ,0) * 100.0 as numeric(9,1))  AS lockers_pct \n"
				+ "    ,     blocks_total         \n"
				+ "    ,     blocks_done          \n"
				+ "    ,cast(blocks_done     * 1.0 / nullif(blocks_total     ,0) * 100.0 as numeric(9,1))  AS blocks_pct \n"
				+ "    ,     tuples_total         \n"
				+ "    ,     tuples_done          \n"
				+ "    ,cast(tuples_done     * 1.0 / nullif(tuples_total     ,0) * 100.0 as numeric(9,1))  AS tuples_pct \n"
				+ "    ,     partitions_total     \n"
				+ "    ,     partitions_done      \n"
				+ "    ,cast(partitions_done * 1.0 / nullif(partitions_total ,0) * 100.0 as numeric(9,1))  AS partitions_pct \n"

				+ "from pg_stat_progress_create_index";

//		return ""
//				+ "select \n"
//				+ "     * \n"
//				+ "    ,cast('' as varchar(128)) AS schema_name \n"
//				+ "    ,cast('' as varchar(128)) AS relation_name \n"
//				+ "    ,cast('' as varchar(128)) AS index_name \n"
//				+ "from pg_stat_progress_create_index";
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		PostgresCmHelper.resolveSchemaAndRelationName(newSample, "datid", "relid",       null, "schema_name", "relation_name");
		PostgresCmHelper.resolveSchemaAndRelationName(newSample, "datid", "index_relid", null, null         , "index_name");

		// Percent Calculations
//		PostgresCmHelper.resolvePercentDone(newSample, "lockers_total"   , "lockers_done"   , "lockers_pct");
//		PostgresCmHelper.resolvePercentDone(newSample, "blocks_total"    , "blocks_done"    , "blocks_pct");
//		PostgresCmHelper.resolvePercentDone(newSample, "tuples_total"    , "tuples_done"    , "tuples_pct");
//		PostgresCmHelper.resolvePercentDone(newSample, "partitions_total", "partitions_done", "partitions_pct");
	}
}
