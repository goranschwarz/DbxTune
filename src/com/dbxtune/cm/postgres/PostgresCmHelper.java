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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cache.DbmsObjectIdCache;
import com.dbxtune.cache.DbmsObjectIdCache.ObjectInfo;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.MathUtils;
import com.dbxtune.utils.StringUtil;

public class PostgresCmHelper
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * get/fill in 'dbname', 'schema_name' and 'schema_name' from ObjectIdCache based on the 'database:id' and 'relation:id'
	 * 
	 * @param cs                 Counter Sample object to "fix"
	 * @param dbid_colName              Name of the column name holding a 'Database ID', mandatory parameter
	 * @param relid_colName             Name of the column name holding a 'Relation ID', mandatory parameter
	 * @param dbname_colName            Name of the column name to "store" the 'Database Name', (if null, this wont be done)
	 * @param schemaName_colName        Name of the column name to "store" the 'Schema Name',   (if null, this wont be done)
	 * @param relationName_colName      Name of the column name to "store" the 'Table Name',    (if null, this wont be done)
	 */
	public static void resolveSchemaAndRelationName(CounterSample cs, String dbid_colName, String relid_colName, 
			String dbname_colName, String schemaName_colName, String relationName_colName)
	{
		// Exit early - If the cache isn't available
		if ( ! DbmsObjectIdCache.hasInstance() )
		{
			_logger.info("resolveSchemaAndRelationName(): No DbmsObjectIdCache available, can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		// Check input parameters
		if (cs == null)                                throw new RuntimeException("Input parameter 'counterSample' cant be null.");
		if (StringUtil.isNullOrBlank("dbid_colName"))  throw new RuntimeException("Input parameter 'dbid_colName' cant be null or empty.");
		if (StringUtil.isNullOrBlank("relid_colName")) throw new RuntimeException("Input parameter 'dbid_colName' cant be null or empty.");


		int dbid_pos          = cs.findColumn(dbid_colName, true);
		int relid_pos         = cs.findColumn(relid_colName);

		int dbname_pos        = cs.findColumn(dbname_colName);
		int schema_name_pos   = cs.findColumn(schemaName_colName);
		int relation_name_pos = cs.findColumn(relationName_colName);

		// No need to continue if we havn't got the columns we need
		if (dbid_pos == -1 || relid_pos == -1)
		{
			_logger.info("resolveSchemaAndRelationName(): Desired columns not available (database_pos=" + dbid_pos + ", relation_pos=" + relid_pos + "), can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		// No need to continue if we havn't got the columns we need
		if (dbname_pos == -1 && schema_name_pos == -1 && relation_name_pos == -1)
		{
			_logger.info("resolveSchemaAndRelationName(): all destiantion table are -1. Skipping the lookup (dbname_pos=" + dbname_pos + ", schema_name_pos=" + schema_name_pos + ", relation_name_pos=" + relation_name_pos + "), can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		// Loop on all rows
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			Long r_dbid  = cs.getValueAsLong(rowId, dbid_pos);
			Long r_relid = cs.getValueAsLong(rowId, relid_pos);

//System.out.println(">>>>>>>>>> resolveSchemaAndRelationName(): row="+rowId+", r_dbid="+r_dbid+", r_relid="+r_relid);
			if (r_dbid != null && r_dbid > 0)
			{
				DbmsObjectIdCache cache = DbmsObjectIdCache.getInstance();
				String dbname        = cache.getDBName(r_dbid);
				String schema_name   = null;
				String relation_name = null;
				
				if (r_relid != null && r_relid > 0)
				{
					try
					{
						ObjectInfo objInfo = cache.getByObjectId(r_dbid, r_relid);
						if (objInfo != null)
						{
							schema_name   = objInfo.getSchemaName();
							relation_name = objInfo.getObjectName();
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

				if (dbname_pos        != -1 && dbname        != null) cs.setValueAt(dbname       , rowId, dbname_pos);
				if (schema_name_pos   != -1 && schema_name   != null) cs.setValueAt(schema_name  , rowId, schema_name_pos);
				if (relation_name_pos != -1 && relation_name != null) cs.setValueAt(relation_name, rowId, relation_name_pos);
			}
		}
	}

	/**
	 * Calculate and set a percent value.
	 * 
	 * @param cs                   Counter Sample object to "fix"
	 * @param totalCount_colName   Name of the column name holding a 'total' value to complete, mandatory parameter
	 * @param doneCount_colName    Name of the column name holding a 'done' value done so far, mandatory parameter
	 * @param percent_colName      Name of the column name to "store" the 'percent' value, mandatory parameter
	 */
	public static void resolvePercentDone(CounterSample cs, String totalCount_colName, String doneCount_colName, String percent_colName)
	{
		// Check input parameters
		if (cs == null)                                throw new RuntimeException("Input parameter 'counterSample' cant be null.");
		if (StringUtil.isNullOrBlank("dbid_colName"))  throw new RuntimeException("Input parameter 'dbid_colName' cant be null or empty.");
		if (StringUtil.isNullOrBlank("relid_colName")) throw new RuntimeException("Input parameter 'dbid_colName' cant be null or empty.");


		int totalCount_pos = cs.findColumn(totalCount_colName);
		int doneCount_pos  = cs.findColumn(doneCount_colName);
		int percent_pos    = cs.findColumn(percent_colName);

		// No need to continue if we havn't got the columns we need
		if (totalCount_pos == -1 || doneCount_pos == -1 || percent_pos == -1)
		{
			_logger.info("resolvePercentDone(): Desired columns not available (totalCount_pos=" + totalCount_pos + ", doneCount_pos=" + doneCount_pos + ", percent_pos=" + percent_pos + ").");
			return;
		}

		// Loop on all rows
		for (int rowId = 0; rowId < cs.getRowCount(); rowId++)
		{
			Double r_totalCount = cs.getValueAsDouble(rowId, totalCount_pos, 0d);
			Double r_doneCount  = cs.getValueAsDouble(rowId, doneCount_pos , 0d);

			// Do the calculation
//			BigDecimal r_percentDone = r_totalCount <= 0 ? new BigDecimal(0) : MathUtils.roundToBigDecimal(r_doneCount / r_totalCount * 100.0, 1);
			BigDecimal r_percentDone = null;
			if (r_totalCount > 0)
				r_percentDone = MathUtils.roundToBigDecimal(r_doneCount / r_totalCount * 100.0, 1);
			
			//System.out.println(">>>>>>>>>> resolvePercentDone(): row="+rowId+", r_totalCount="+r_totalCount+", r_doneCount="+r_doneCount+", r_percentDone="+r_percentDone);

			cs.setValueAt(r_percentDone, rowId, percent_pos);
		}
	}








	public static final String  PROPKEY_pgStatements_createExtension     = "CmPgStatements.if.missing.pg_stat_statements.createExtension";
	public static final boolean DEFAULT_pgStatements_createExtension     = true;

	/** When the last AUTO-FIX found that 'pg_stat_statements' is not in 'shared_preload_libraries' (0 = never) */
	private static long _pgStatements_notLoadedTime = 0;

	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since SIX is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	public static boolean pgStatements_beforeRefreshGetData(CountersModel cm, DbxConnection conn) throws Exception
	{
		String sql = "SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'pg_stat_statements' AND c.relkind = 'v' ";
		
		// Check if 'pg_stat_statements' exists... 
		// and possibly create it...
		boolean exists = false;
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			while(rs.next())
				exists = true;
		}
		
		// The last AUTO-FIX found that the library is not loaded: that needs a restart of Postgres, so do not try (and log) again at every refresh, only once an hour
		if ( ! exists && System.currentTimeMillis() - _pgStatements_notLoadedTime < 3_600_000 )
			return false;

		if ( ! exists )
		{
			_logger.warn("When checking Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "' The table 'pg_stat_statements' do not exists.");

			_logger.warn("Possible solution for 'pg_stat_statements': (1: check that file 'PG_INST_DIR/lib/pg_stat_statements.so' exists, if not install postgres contrib package), (2: check that config 'shared_preload_libraries' contains 'pg_stat_statements'), (3: optional: configure 'pg_stat_statements.max=10000', 'pg_stat_statements.track=all' in the config file and restart postgres), (4: execute: CREATE EXTENSION pg_stat_statements), (5: verify with: SELECT * FROM pg_stat_statements). or look at https://www.postgresql.org/docs/current/static/pgstatstatements.html");

			boolean createExtension = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_pgStatements_createExtension, DEFAULT_pgStatements_createExtension);
			if (createExtension)
			{
				_logger.info("AUTO-FIX: The table 'pg_stat_statements' did not exists... Trying to create it, this will simply work if you are authorized. to DISABLE the 'CREATE EXTENSION pg_stat_statements' set config '" + PROPKEY_pgStatements_createExtension + "=false'.");

				// We are often inside the refresh transaction (AutoCommit=false): a failing statement aborts the whole transaction,
				// and every following SQL in this refresh fails with "current transaction is aborted". So: rollback to a savepoint on failure.
				Savepoint savepoint = conn.getAutoCommit() ? null : conn.setSavepoint();
				try (Statement stmnt = conn.createStatement())
				{
					_logger.info("AUTO-FIX: Executing: CREATE EXTENSION pg_stat_statements");

					stmnt.executeUpdate("CREATE EXTENSION pg_stat_statements");

					_logger.info("AUTO-FIX: Success executing: CREATE EXTENSION pg_stat_statements");

					// Check if the table is accessible after 'CREATE EXTENSION pg_stat_statements'
					// NOTE: read a row ('where 1=2' never calls the view's function, so it works even when the library is NOT in 'shared_preload_libraries')
					try( Statement stmnt2 = conn.createStatement(); ResultSet rs = stmnt2.executeQuery("SELECT 1 FROM pg_stat_statements limit 1"); )
					{
						while(rs.next())
							;
						exists = true;
					}
					catch (SQLException exSelChk)
					{
						exists = false;
						_logger.warn("AUTO-FIX: FAILED when checking table 'pg_stat_statements' after 'CREATE EXTENSION pg_stat_statements' (is 'pg_stat_statements' in 'shared_preload_libraries'?). The extension is removed again, and the next try is in 1 hour. Caught: " + exSelChk);
					_pgStatements_notLoadedTime = System.currentTimeMillis();
						// Remove the extension we just created (it can't be used): rollback to the savepoint, or DROP it when AutoCommit is on
						if (savepoint != null)
							conn.rollback(savepoint);
						else
						{
							try (Statement stmnt3 = conn.createStatement()) { stmnt3.executeUpdate("DROP EXTENSION IF EXISTS pg_stat_statements"); }
							catch (SQLException exDrop) { _logger.warn("AUTO-FIX: Problems removing the extension 'pg_stat_statements' again. Caught: " + exDrop); }
						}

						// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
						//System.setProperty(PROPKEY_pgStatements_createExtension, "false"); 
					}
				}
				catch(SQLException exCrExt)
				{
					exists = false;
					_logger.warn("AUTO-FIX: FAILED when executing 'CREATE EXTENSION pg_stat_statements'. Caught: " + exCrExt);
					if (savepoint != null)
						conn.rollback(savepoint);

					// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
					//System.setProperty(PROPKEY_pgStatements_createExtension, "false"); 
				}
			}
		}
		
		return exists;
	}

	public static boolean pgStatements_checkDependsOnOther(CountersModel cm, DbxConnection conn)
	{
		boolean isOk = false;
		String  message = "";
		
		// Check if the table exists, since it's optional and needs to be installed
		try
		{
			isOk = cm.beforeRefreshGetData(conn);
		}
		catch (Exception ex)
		{
			isOk = false;
			_logger.warn("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "' The table 'pg_stat_statements' do not exists. This is an optional component. Caught: " + ex);
			message = ex.getMessage();
		}

		// The view can exist while the library is NOT loaded (CREATE EXTENSION works without 'shared_preload_libraries'),
		// then every read fails with: ERROR: pg_stat_statements must be loaded via "shared_preload_libraries" (SqlState 55000)
		if (isOk && ! PostgresCmUtils.canExecute(conn, "select 1 from pg_stat_statements limit 1"))
		{
			isOk = false;
			message = "The view 'pg_stat_statements' exists, but it can't be read (the library is probably not in 'shared_preload_libraries').";
		}

		if (isOk)
		{
			_logger.info("Check dependencies SUCCESS: Table 'pg_stat_statements' exists when checking Counters Model '" + cm.getName() + "'.");
			return true;
		}

		return PostgresCmUtils.disable(cm,
				"The extension 'pg_stat_statements' is not installed (or not loaded)." + (StringUtil.hasValue(message) ? " " + message : ""),
				"add 'pg_stat_statements' to the setting 'shared_preload_libraries' (append it to the current list) and restart Postgres, then: CREATE EXTENSION pg_stat_statements; "
				+ "See DbmsSetup/postgres.txt and https://www.postgresql.org/docs/current/pgstatstatements.html");
	}





	public static final String  PROPKEY_pgWaitSampling_createExtension     = "CmPgWaitSampling.if.missing.pg_wait_sampling_profile.createExtension";
	public static final boolean DEFAULT_pgWaitSampling_createExtension     = true; 

	/**
	 * Called before <code>refreshGetData(conn)</code> where we can make various checks
	 * <p>
	 * Note: this is a special case since SIX is recreating the Postgres Server 
	 *       (every now and then) during the day/night...
	 *       We need to check/create the extension before polling data from it!
	 */
	public static boolean pgWaitSampling_beforeRefreshGetData(CountersModel cm, DbxConnection conn) throws Exception
	{
//		String sql = "SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'pg_wait_sampling_profile' AND c.relkind = 'v' ";
		String sql = "SELECT 1 FROM pg_catalog.pg_class c WHERE c.relname = 'pg_wait_sampling_profile' ";
		
		// Check if 'pg_stat_statements' exists... 
		// and possibly create it...
		boolean exists = false;
		try( Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
		{
			while(rs.next())
				exists = true;
		}
		
		if ( ! exists )
		{
			_logger.warn("When checking Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "' The table 'pg_wait_sampling_profile' do not exists.");

			_logger.warn("Possible solution for 'pg_wait_sampling_profile': "
					+ "(1: check that file 'PG_INST_DIR/lib/pg_wait_sampling.so' exists, if not install postgres package 'pg_wait_sampling_##' where ## is Postgres version, example: dnf/yum install -y pg_wait_sampling_16), "
					+ "(2: check that config 'shared_preload_libraries' contains 'pg_wait_sampling' (append it to the current list), and restart postgres), "
					+ "(3: optional: the 'pg_wait_sampling.*' settings, see https://github.com/postgrespro/pg_wait_sampling), "
					+ "(4: execute: CREATE EXTENSION pg_wait_sampling), "
					+ "(5: verify with: SELECT * FROM pg_wait_sampling_profile). "
					+ "or look at https://github.com/postgrespro/pg_wait_sampling and DbmsSetup/postgres.txt");

			boolean createExtension = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_pgWaitSampling_createExtension, DEFAULT_pgWaitSampling_createExtension);
			if (createExtension)
			{
				_logger.info("AUTO-FIX: The table 'pg_wait_sampling_profile' did not exists... Trying to create it, this will simply work if you are authorized. to DISABLE the 'CREATE EXTENSION pg_wait_sampling' set config '" + PROPKEY_pgWaitSampling_createExtension + "=false'.");

				// We are often inside the refresh transaction (AutoCommit=false): a failing statement aborts the whole transaction,
				// and every following SQL in this refresh fails with "current transaction is aborted". So: rollback to a savepoint on failure.
				Savepoint savepoint = conn.getAutoCommit() ? null : conn.setSavepoint();
				try (Statement stmnt = conn.createStatement())
				{
					_logger.info("AUTO-FIX: Executing: CREATE EXTENSION pg_wait_sampling");

					stmnt.executeUpdate("CREATE EXTENSION pg_wait_sampling");

					_logger.info("AUTO-FIX: Success executing: CREATE EXTENSION pg_wait_sampling");

					// Check if the table is accessible after 'CREATE EXTENSION pg_wait_sampling'
					// NOTE: read a row ('where 1=2' never calls the view's function, so it works even when the library is NOT in 'shared_preload_libraries')
					try( Statement stmnt2 = conn.createStatement(); ResultSet rs = stmnt2.executeQuery("SELECT 1 FROM pg_wait_sampling_profile limit 1"); )
					{
						while(rs.next())
							;
						exists = true;
					}
					catch (SQLException exSelChk)
					{
						exists = false;
						_logger.warn("AUTO-FIX: FAILED when checking table 'pg_wait_sampling_profile' after 'CREATE EXTENSION pg_wait_sampling' (is 'pg_wait_sampling' in 'shared_preload_libraries'?). The extension is removed again. Caught: " + exSelChk);
						// Remove the extension we just created (it can't be used): rollback to the savepoint, or DROP it when AutoCommit is on
						if (savepoint != null)
							conn.rollback(savepoint);
						else
						{
							try (Statement stmnt3 = conn.createStatement()) { stmnt3.executeUpdate("DROP EXTENSION IF EXISTS pg_wait_sampling"); }
							catch (SQLException exDrop) { _logger.warn("AUTO-FIX: Problems removing the extension 'pg_wait_sampling' again. Caught: " + exDrop); }
						}
						
						// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
						//System.setProperty(PROPKEY_pgWaitSampling_createExtension, "false"); 
					}
				}
				catch(SQLException exCrExt)
				{
					exists = false;
					_logger.warn("AUTO-FIX: FAILED when executing 'CREATE EXTENSION pg_wait_sampling'. Caught: " + exCrExt);
					if (savepoint != null)
						conn.rollback(savepoint);

					// Possibly turn off 'AUTO-FIX' since it failed... until next restart (hence the System.setProperty())
					//System.setProperty(PROPKEY_pgWaitSampling_createExtension, "false"); 
				}
			}
		}
		
		return exists;
	}

	public static boolean pgWaitSampling_checkDependsOnOther(CountersModel cm, DbxConnection conn)
	{
		boolean isOk = false;
		String  message = "";

		// Check if the table exists, since it's optional and needs to be installed
		try
		{
			isOk = cm.beforeRefreshGetData(conn);
		}
		catch (Exception ex)
		{
			isOk = false;
			_logger.warn("When trying to initialize Counters Model '" + cm.getName() + "', named '" + cm.getDisplayName() + "' The table 'pg_wait_sampling_profile' do not exists. This is an optional component. Caught: " + ex);
			message = ex.getMessage();
		}

		if (isOk)
		{
			_logger.info("Check dependencies SUCCESS: Table 'pg_wait_sampling_profile' exists when checking Counters Model '" + cm.getName() + "'.");
			return true;
		}
		
		return PostgresCmUtils.disable(cm,
				"The extension 'pg_wait_sampling' is not installed (or not loaded). It is an optional third party extension." + (StringUtil.hasValue(message) ? " " + message : ""),
				"install the package (for example: dnf install pg_wait_sampling_## or apt install postgresql-##-pg-wait-sampling, where ## is the Postgres major version), "
				+ "add 'pg_wait_sampling' to the setting 'shared_preload_libraries' (append it to the current list) and restart Postgres, then: CREATE EXTENSION pg_wait_sampling; "
				+ "See DbmsSetup/postgres.txt and https://github.com/postgrespro/pg_wait_sampling");
	}

}
