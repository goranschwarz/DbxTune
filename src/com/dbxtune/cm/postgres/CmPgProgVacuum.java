/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.lang.invoke.MethodHandles;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class CmPgProgVacuum
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgProgVacuum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Vacuum";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Whenever VACUUM is running, the pg_stat_progress_vacuum view will contain one row for each backend (including autovacuum worker processes) that is currently vacuuming. <br>" +
		"Progress for VACUUM FULL commands is reported via pg_stat_progress_cluster because both VACUUM FULL and CLUSTER rewrite the table, while regular VACUUM only modifies it in place." +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_PROGRESS;
	public static final String   GUI_ICON_FILE    = "images/" + CM_NAME + ".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(9,6);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_progress_vacuum"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			"heap_blks_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {};
	
	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

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
			guiController.splashWindowProgress("Loading: Counter Model '" + CM_NAME + "'");

		return new CmPgProgVacuum(counterController, guiController);
	}

	public CmPgProgVacuum(ICounterController counterController, IGuiController guiController)
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

// View "pg_catalog.pg_stat_progress_vacuum"
// Column              |  Type   | Collation | Nullable | Default 
// --------------------+---------+-----------+----------+---------
// pid                 | integer |           |          | 
// datid               | oid     |           |          | 
// datname             | name    |           |          | 
// relid               | oid     |           |          | 
// phase               | text    |           |          | 
// heap_blks_total     | bigint  |           |          | 
// heap_blks_scanned   | bigint  |           |          | 
// heap_blks_vacuumed  | bigint  |           |          | 
// index_vacuum_count  | bigint  |           |          | 
// max_dead_tuples     | bigint  |           |          | 
// num_dead_tuples     | bigint  |           |          |
	
	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo == null ? 0 : versionInfo.getLongVersion();
		String sql = getSql(srvVersion);

		// Dry-run the newer SQL (a fork may report a version, but not have the columns): fall back to the SQL before 17
		if (srvVersion >= Ver.ver(17) && conn != null && ! PostgresCmUtils.canExecute(conn, "select * from (" + sql + ") x where 1=2"))
		{
			_logger.warn(CM_NAME + ": The SQL for version " + Ver.versionNumToStr(srvVersion) + " failed, the SQL for Postgres 16 is used (and it may fail at every sample).");
			sql = getSql(Ver.ver(16));
		}
		return sql;
	}

	/**
	 * 17: max_dead_tuples / num_dead_tuples was replaced by max_dead_tuple_bytes / dead_tuple_bytes / num_dead_item_ids, indexes_total / indexes_processed was added<br>
	 * 18: delay_time (needs 'track_cost_delay_timing')<br>
	 * 19: mode, started_by<br>
	 * The old columns stay (history): num_dead_tuples is num_dead_item_ids (each dead item is a dead tuple), max_dead_tuples is NULL (17 has a memory limit in bytes instead).
	 */
	private String getSql(long srvVersion)
	{
		String deadTuples = ""
				+ "    ,     max_dead_tuples      \n"
				+ "    ,     num_dead_tuples      \n";
		if (srvVersion >= Ver.ver(17))
		{
			deadTuples = ""
				+ "    ,cast(NULL as bigint)              AS max_dead_tuples       \n"
				+ "    ,     num_dead_item_ids            AS num_dead_tuples       \n"
				+ "    ,     max_dead_tuple_bytes         \n"
				+ "    ,     dead_tuple_bytes             \n"
				+ "    ,     indexes_total                \n"
				+ "    ,     indexes_processed            \n";
		}
		if (srvVersion >= Ver.ver(18))
			deadTuples += "    ,cast(delay_time as numeric(15,1)) AS delay_time_ms         \n";
		if (srvVersion >= Ver.ver(19))
			deadTuples += "    ,cast(mode       as varchar(30))   AS mode                  \n"
			            + "    ,cast(started_by as varchar(30))   AS started_by            \n";

		return ""
				+ "select \n"
				+ "          pid \n"

				+ "    ,cast(datid        as bigint      )  AS datid         \n"
				+ "    ,cast(relid        as bigint      )  AS relid         \n"

				+ "    ,cast(datname      as varchar(128))  AS datname       \n"
				+ "    ,cast(''           as varchar(128))  AS schema_name   \n"
				+ "    ,cast(''           as varchar(128))  AS relation_name \n"
				+ "    ,cast(''           as varchar(128))  AS index_name    \n"
				
				+ "    ,cast(phase        as varchar(128))  AS phase   \n"
				+ "    ,     index_vacuum_count   \n"
				+ "    ,     heap_blks_total      \n"
				+ "    ,     heap_blks_scanned    \n"
				+ "    ,cast(heap_blks_scanned * 1.0 / nullif(heap_blks_total, 0) * 100.0 as numeric(9,1))  AS heap_blks_pct \n"
				+ "    ,     heap_blks_vacuumed   \n"
				+ deadTuples

				+ "from pg_stat_progress_vacuum";

//		return ""
//				+ "select \n"
//				+ "     * \n"
//				+ "    ,cast('' as varchar(128)) AS schema_name \n"
//				+ "    ,cast('' as varchar(128)) AS relation_name \n"
//				+ "from pg_stat_progress_vacuum";
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		PostgresCmHelper.resolveSchemaAndRelationName(newSample, "datid", "relid", null, "schema_name", "relation_name");
	}
}
