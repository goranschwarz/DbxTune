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

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCache.ObjectInfo;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.postgres.gui.CmPgLocksPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPgLocks
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmPgLocks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPgLocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Locks";
	public static final String   HTML_DESC        = 
		"<html>" +
		"All locks in the Postgres Instance. (from pg_locks)" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_OBJECT_ACCESS;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = Ver.ver(14);
	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"pg_locks"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
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

		return new CmPgLocks(counterController, guiController);
	}

	public CmPgLocks(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

//		addDependsOnCm(CmPgTables.CM_NAME); // CmPgTables "must" have been executed before this cm, otherwise dbname, schema_name and table_name wont be set
		
		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmPgLocksPanel(this);
	}
	
	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return null;
//		List <String> pkCols = new LinkedList<String>();
//
////		pkCols.add("pk");
//
//		return pkCols;
	}

	// instead of the below, make CmSummary request a refresh
//	@Override
//	public boolean isRefreshable()
//	{
//		boolean superVal = super.isRefreshable();
//		int blockingLockCount = CmSummary.getBlockingLockCount();
//		
//		// Can/should we set the reason why we should NOT refresh
//		if (blockingLockCount <= 0)
//		{
//			if (getTabPanel() != null)
//				getTabPanel().setWatermarkText("Skipping refresh of 'CmPgLocks'. CmSummary says there is NO blockings, so there is no need to refresh.");
//
//			_logger.info("Skipping refresh of 'CmPgLocks'. CmSummary.getBlockingLockCount() == " + blockingLockCount);
//
//			return false;
//		}
//		
//		return superVal;
//	}
	
	/**
	 * get/fill in 'dbname', 'schema_name' and 'schema_name' from ObjectIdCache basen on the 'database:id' and 'relation:id'
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Exit early - If the cache isn't available
		if ( ! DbmsObjectIdCache.hasInstance() )
		{
			_logger.info("localCalculation(): No DbmsObjectIdCache available, can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		int database_pos      = newSample.findColumn("database");
		int relation_pos      = newSample.findColumn("relation");

		int dbname_pos        = newSample.findColumn("dbname");
		int schema_name_pos   = newSample.findColumn("schema_name");
		int relation_name_pos = newSample.findColumn("relation_name");
		int relation_type_pos = newSample.findColumn("relation_type");

		// No need to continue if we havn't got the columns we need
		if (database_pos == -1 || relation_pos == -1 || dbname_pos == -1 || schema_name_pos == -1 || relation_name_pos == -1 || relation_type_pos == -1)
		{
			_logger.info("localCalculation(): Desired columns not available (database_pos="+database_pos+", relation_pos="+relation_pos+", dbname_pos="+dbname_pos+", schema_name_pos="+schema_name_pos+", relation_name_pos="+relation_name_pos+", relation_type_pos="+relation_type_pos+"), can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		// Loop on all rows
		for (int rowId = 0; rowId < newSample.getRowCount(); rowId++)
		{
			Long database = newSample.getValueAsLong(rowId, database_pos);
			Long relation = newSample.getValueAsLong(rowId, relation_pos);

//System.out.println(">>>>>>>>>> CmPgLocks.localCalculation(): row="+rowId+", database="+database+", relation="+relation);
			if (database != null && database > 0)
			{
				DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
				String dbname        = cache.getDBName(database);
				String schema_name   = null;
				String relation_name = null;
				String relation_type = null;
				
				if (relation != null && relation > 0)
				{
					try
					{
						ObjectInfo objInfo = cache.getByObjectId(database, relation);
						if (objInfo != null)
						{
							schema_name   = objInfo.getSchemaName();
							relation_name = objInfo.getObjectName();
							relation_type = objInfo.getObjectTypeStr();
						}
					}
					catch (TimeoutException timoutEx)
					{
						// nothing to do... 
					}
//if (relation_name == null)
//{
//	System.out.println("        -- ######################################################################### NOT FOUND RECORD: row="+rowId+", database="+database+", relation="+relation+" :::: dbname='"+dbname+"', schema_name='"+schema_name+"', relation_name='"+relation_name+"'.");
//}

//if (relation_name != null)
//{
//	System.out.println("        ++ ######################################################################### FOUND RECORD: dbname='"+dbname+"', schema_name='"+schema_name+"', relation_name='"+relation_name+"'.");
////	System.out.println(cache.debugPrintAllObject());
//}

				}

//System.out.println("        ++ CmPgLocks.localCalculation(): row="+rowId+", database="+database+", relation="+relation+", dbname='"+dbname+"', schema_name='"+schema_name+"', relation_name='"+relation_name+"'.");

				if (dbname        != null) newSample.setValueAt(dbname       , rowId, dbname_pos);
				if (schema_name   != null) newSample.setValueAt(schema_name  , rowId, schema_name_pos);
				if (relation_name != null) newSample.setValueAt(relation_name, rowId, relation_name_pos);
				if (relation_type != null) newSample.setValueAt(relation_type, rowId, relation_type_pos);
			}
		}
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		// ----- Postgres Version: 14
		String waitstart        = "";
		String waitstart_is_sec = "";
		if (versionInfo.getLongVersion() >= Ver.ver(14))
		{
			waitstart         = "    ,waitstart \n";
			waitstart_is_sec  = "    ,CAST( COALESCE( EXTRACT('epoch' FROM clock_timestamp()) - EXTRACT('epoch' FROM waitstart), -1) as numeric(12,1)) AS waitstart_is_sec \n"; 
		}

		// Build SQL
		String sql = ""
			    + "select \n"
			    + "     CAST(null               as varchar(128)) AS dbname \n"              // NOTE: This will be resolved in localCalculation
			    + "    ,CAST(null               as varchar(128)) AS schema_name \n"         // NOTE: This will be resolved in localCalculation
			    + "    ,CAST(null               as varchar(128)) AS relation_name \n"       // NOTE: This will be resolved in localCalculation
			    + "    ,CAST(null               as varchar(30) ) AS relation_type \n"       // NOTE: This will be resolved in localCalculation

			    + "    ,CAST(locktype           as varchar(128)) AS locktype \n"            // java.sql.Types.VARCHAR   text              pg_locks \n"
			    + "    ,CAST(mode               as varchar(128)) AS mode \n"                // java.sql.Types.VARCHAR   text              pg_locks \n"
			    + "    ,page \n"                                                            // java.sql.Types.INTEGER   int4              pg_locks \n"
			    + "    ,tuple \n"                                                           // java.sql.Types.SMALLINT  int2              pg_locks \n"
			    + "    ,CAST(virtualxid         as varchar(128)) AS virtualxid \n"          // java.sql.Types.VARCHAR   text              pg_locks \n"
			    + "    ,CAST(transactionid      as varchar(128)) AS transactionid \n"       // java.sql.Types.OTHER     xid               pg_locks \n"
			    + "    ,classid \n"                                                         // java.sql.Types.BIGINT    oid               pg_locks \n"
			    + "    ,objid \n"                                                           // java.sql.Types.BIGINT    oid               pg_locks \n"
			    + "    ,objsubid \n"                                                        // java.sql.Types.SMALLINT  int2              pg_locks \n"
			    + "    ,CAST(virtualtransaction as varchar(128)) AS virtualtransaction \n"  // java.sql.Types.VARCHAR   text              pg_locks \n"
			    + "    ,pid \n"                                                             // java.sql.Types.INTEGER   int4              pg_locks \n"
			    + "    ,granted \n"                                                         // java.sql.Types.BIT       bool              pg_locks \n"
			    + "    ,fastpath \n"                                                        // java.sql.Types.BIT       bool              pg_locks \n"
			    + waitstart              /* only in 14 and above*/                          // java.sql.Types.TIMESTAMP timestamptz       pg_locks \n"
			    + waitstart_is_sec
			    + "    ,database \n"                                                        // java.sql.Types.BIGINT    oid               pg_locks \n"
			    + "    ,relation \n"                                                        // java.sql.Types.BIGINT    oid               pg_locks \n"
			    + "from pg_locks \n"
			    + "where 1=1 \n"
			    + "  and pid != pg_backend_pid() \n"
			    + "";
		
		return sql;
	}

	private void addTrendGraphs()
	{
	}
}
