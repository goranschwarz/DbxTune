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
package com.dbxtune.cache;

import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.dbxtune.cache.DbmsObjectIdCache.ObjectInfo;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.StringUtil;

public class DbmsObjectIdCacheUtils
{
	private static Logger _logger = Logger.getLogger(DbmsObjectIdCacheUtils.class);

	/**
	 * Helper to "fill in" various columns in a CounterModel <br>
	 * Lookup columns <code>database_id, object_id [, index_id]</code> and translate/fill-in columns <code>DatabaseName, SchemaName, TableName, IndexName</code>
	 * 
	 * Example:
	 * <pre> 
	 * DbmsObjectIdCacheUtils.localCalculation_DbmsObjectIdFiller(cm, newSample, 
	 *     "database_id", "object_id", "index_id",
	 *     "DbName", "SchemaName", "TableName", "IndexName")
	 * </pre>
	 *                                                    
	 * @param cm
	 * @param newSample
	 * 
	 * @param dbId_colname            Name of the column that holds "database_id"      -- mandatory
	 * @param objectId_colname        Name of the column that holds "object_id"        -- mandatory
	 * @param indexId_colname         Name of the column that holds "index_id"         -- optional
	 * 
	 * @param dbname_colname          Name of the column that holds "DbName"           -- optional
	 * @param schemaName_colname      Name of the column that holds "SchemaName"       -- optional
	 * @param tableName_colname       Name of the column that holds "TableName"        -- optional
	 * @param indexName_colname       Name of the column that holds "IndexName"        -- optional
	 */
	public static void localCalculation_DbmsObjectIdFiller(CountersModel cm, CounterSample newSample,
			String dbId_colname, String objectId_colname, String indexId_colname, 
			String dbname_colname, String schemaName_colname, String tableName_colname, String indexName_colname)
	{
		// Resolve "database_id", "object_id", "index_id" to real names 
		if (DbmsObjectIdCache.hasInstance() && DbmsObjectIdCache.getInstance().isBulkLoadOnStartEnabled())
		{
			int database_id_pos = StringUtil.isNullOrBlank(dbId_colname      ) ? -2 : newSample.findColumn(dbId_colname);
			int object_id_pos   = StringUtil.isNullOrBlank(objectId_colname  ) ? -2 : newSample.findColumn(objectId_colname);
			int index_id_pos    = StringUtil.isNullOrBlank(indexId_colname   ) ? -2 : newSample.findColumn(indexId_colname);

			int dbname_pos      = StringUtil.isNullOrBlank(dbname_colname    ) ? -2 : newSample.findColumn(dbname_colname);
			int schemaName_pos  = StringUtil.isNullOrBlank(schemaName_colname) ? -2 : newSample.findColumn(schemaName_colname);
			int tableName_pos   = StringUtil.isNullOrBlank(tableName_colname ) ? -2 : newSample.findColumn(tableName_colname);
			int indexName_pos   = StringUtil.isNullOrBlank(indexName_colname ) ? -2 : newSample.findColumn(indexName_colname);

			if (database_id_pos == -2 || object_id_pos == -2)
			{
				throw new IllegalArgumentException("Missing mandatory columns in cm='" + cm.getName() + "', for: localCalculation_DbmsObjectIdFiller: " 
						+ dbId_colname       + "_pos=" + database_id_pos + ", "
						+ objectId_colname   + "_pos=" + object_id_pos   + ".");
			}

			if (database_id_pos == -1 || object_id_pos == -1 || index_id_pos == -1 || dbname_pos == -1 || schemaName_pos == -1 || tableName_pos == -1 || indexName_pos == -1)
			{
				throw new IllegalArgumentException("Missing translatable columns in cm='" + cm.getName() + "', for: localCalculation_DbmsObjectIdFiller: " 
						+ dbId_colname       + "_pos=" + database_id_pos + ", "
						+ objectId_colname   + "_pos=" + object_id_pos   + ", "
						+ indexId_colname    + "_pos=" + index_id_pos    + ", "
						+ dbname_colname     + "_pos=" + dbname_pos      + ", " 
						+ schemaName_colname + "_pos=" + schemaName_pos  + ", " 
						+ tableName_colname  + "_pos=" + tableName_pos   + ", "
						+ indexName_colname  + "_pos=" + indexName_pos   + ".");
			}

			// Get the DBMS ID Cache Instance
			DbmsObjectIdCache idc = DbmsObjectIdCache.getInstance();

			// Loop on all counter rows
			for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
			{
				// Do the lookup
				Integer database_id = newSample.getValueAsInteger(rowId, database_id_pos);
				Integer object_id   = newSample.getValueAsInteger(rowId, object_id_pos);
				Integer index_id    = index_id_pos < 0 ? null : newSample.getValueAsInteger(rowId, index_id_pos);

				String dbName_val     = idc.getDBName(database_id);;
				String schemaName_val = null;
				String tableName_val  = null;
				String indexName_val  = null;

				try
				{
					ObjectInfo objInfo = idc.getByObjectId(database_id, object_id);
					if (objInfo != null)
					{
						schemaName_val  = objInfo.getSchemaName();
						tableName_val   = objInfo.getObjectName();
						indexName_val   = index_id_pos < 0 ? null : objInfo.getIndexName(index_id);
					}

					if (dbname_pos     >= 0) newSample.setValueAt(dbName_val    , rowId, dbname_pos);
					if (schemaName_pos >= 0) newSample.setValueAt(schemaName_val, rowId, schemaName_pos);
					if (tableName_pos  >= 0) newSample.setValueAt(tableName_val , rowId, tableName_pos);
					if (indexName_pos  >= 0) newSample.setValueAt(indexName_val , rowId, indexName_pos);

//System.out.println(cm.getName() + ": localCalculation: Resolved-DbmsObjectIdCache -- database_id=" + database_id + ", object_id=" + object_id + ", index_id=" + index_id + "  --->>>  DbName='" + dbName_val + "', SchemaName='" + schemaName_val + "', TableName='" + tableName_val + "', IndexName='" + indexName_val + "'.");
				}
				catch (TimeoutException ex)
				{
					_logger.warn("Timeout exception in localCalculation() when getting id's to names. look up on: database_id=" + database_id + ", object_id=" + object_id + ", index_id=" + index_id + ".", ex);
				}
			}
		}
//		else
//		{
//			_logger.info("Skipping BULK load of ObjectId's at localCalculation_DbmsObjectIdFiller(), isBulkLoadOnStartEnabled() was NOT enabled.");
//		}
	}

}
