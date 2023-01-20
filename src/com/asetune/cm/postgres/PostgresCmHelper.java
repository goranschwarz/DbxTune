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
package com.asetune.cm.postgres;

import java.math.BigDecimal;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.asetune.cache.DbmsObjectIdCache;
import com.asetune.cache.DbmsObjectIdCache.ObjectInfo;
import com.asetune.cm.CounterSample;
import com.asetune.utils.MathUtils;
import com.asetune.utils.StringUtil;

public class PostgresCmHelper
{
	private static Logger _logger = Logger.getLogger(PostgresCmHelper.class);

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
			_logger.info("resolveSchemaAndRelationName(): Desired columns not available (database_pos="+dbid_pos+", relation_pos="+relid_pos+"), can't resolv 'database:id' and 'relation:id' into real names.");
			return;
		}

		// No need to continue if we havn't got the columns we need
		if (dbname_pos == -1 && schema_name_pos == -1 && relation_name_pos == -1)
		{
			_logger.info("resolveSchemaAndRelationName(): all destiantion table are -1. Skipping the lookup (dbname_pos="+dbname_pos+", schema_name_pos="+schema_name_pos+", relation_name_pos="+relation_name_pos+"), can't resolv 'database:id' and 'relation:id' into real names.");
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
			_logger.info("resolvePercentDone(): Desired columns not available (totalCount_pos="+totalCount_pos+", doneCount_pos="+doneCount_pos+", percent_pos="+percent_pos+").");
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
}
