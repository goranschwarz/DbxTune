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
public class CmPgProgCopy
extends CountersModel
{
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgProgCopy.class.getSimpleName();
	public static final String   SHORT_NAME       = "Copy";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Whenever COPY is running, the pg_stat_progress_copy view will contain one row for each backend that is currently running a COPY command." +
		"</html>";

	public static final String   GROUP_NAME       = MainFramePostgres.TCP_GROUP_PROGRESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_stat_progress_copy"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {
			"bytes_pct"
	};
	public static final String[] DIFF_COLUMNS     = new String[] {
			"bytes_processed",
			"tuples_processed",
			"tuples_excluded"
	};
	
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

		return new CmPgProgCopy(counterController, guiController);
	}

	public CmPgProgCopy(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("pid");
		pkCols.add("datid");
		pkCols.add("relid");

		return pkCols;
	}

// View "pg_catalog.pg_stat_progress_copy"
// Column            |  Type   | Collation | Nullable | Default 
// ------------------+---------+-----------+----------+---------
// pid               | integer |           |          | 
// datid             | oid     |           |          | 
// datname           | name    |           |          | 
// relid             | oid     |           |          | 
// command           | text    |           |          | 
// type              | text    |           |          | 
// bytes_processed   | bigint  |           |          | 
// bytes_total       | bigint  |           |          | 
// tuples_processed  | bigint  |           |          | 
// tuples_excluded   | bigint  |           |          |

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return ""
				+ "select \n"
				+ "          pid \n"

				+ "    ,cast(datid    as bigint      )  AS datid         \n"
				+ "    ,cast(relid    as bigint      )  AS relid         \n"

				+ "    ,cast(datname  as varchar(128))  AS datname       \n"
				+ "    ,cast(''       as varchar(128))  AS schema_name   \n"
				+ "    ,cast(''       as varchar(128))  AS relation_name \n"
				
				+ "    ,cast(command  as varchar(128))  AS command \n"
				+ "    ,cast(type     as varchar(128))  AS type    \n"
				
				+ "    ,     bytes_total       \n"
				+ "    ,     bytes_processed   \n"
				+ "    ,cast(bytes_processed * 1.0 / nullif(bytes_total, 0) * 100.0 as numeric(9,1))  AS bytes_pct \n"
				+ "    ,     tuples_processed  \n"
				+ "    ,     tuples_excluded   \n"

				+ "from pg_stat_progress_copy";

//		return ""
//				+ "select \n"
//				+ "     * \n"
//				+ "    ,cast('' as varchar(128)) AS schema_name \n"
//				+ "    ,cast('' as varchar(128)) AS relation_name \n"
//				+ "from pg_stat_progress_copy";
	}
	
	@Override
	public void localCalculation(CounterSample newSample)
	{
		PostgresCmHelper.resolveSchemaAndRelationName(newSample, "datid", "relid", null, "schema_name", "relation_name");
	}
}
