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
package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.utils.Configuration;

public class DbxDatabaseMetaDataSqlServer 
extends DbxDatabaseMetaData
{
	private static Logger _logger = Logger.getLogger(ConnectionProp.class);
	
	public static final String  PROPKEY_useOriginImpementation_getIndexInfo = "DbxDatabaseMetaDataSqlServer.useOriginImpementation.getIndexInfo";
	public static final boolean DEFAULT_useOriginImpementation_getIndexInfo = false;

	public DbxDatabaseMetaDataSqlServer(DatabaseMetaData dbmd)
	{
		super(dbmd);
	}

	/**
	 * For SQL Server we override this, since the MS JDBC id NOT picking up COLUMN STORE INDEXES
	 * <p>
	 * Not sure if this works at 100% but at least it's start...
	 */
	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table, boolean unique, boolean approximate) 
	throws SQLException
	{
		boolean useOriginImpementation = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_useOriginImpementation_getIndexInfo, DEFAULT_useOriginImpementation_getIndexInfo);

		// Use the ORIGINAL Implementation
		if (useOriginImpementation)
			return super.getIndexInfo(catalog, schema, table, unique, approximate);

		// Below is: Special implementation that also shows COLUMNSTORE indexes
		// And hopefully also the correct CARDINALITY & PAGES for partitioned tables/indexes
		Connection conn = _dbmd.getConnection();

		if (_logger.isDebugEnabled())
			_logger.debug("DbxDatabaseMetaDataSqlServer.getIndexInfo(catalog='" + catalog + "', schema='" + schema + "', table='" + table + "', unique=" + unique + ", approximate=" + approximate + "): Called");

		String objectname = table;
		if (schema != null && !schema.isEmpty())
			objectname = schema + "." + table;
		if (catalog != null && !catalog.isEmpty())
			objectname = catalog + "." + objectname;
		
		String dbname = "";
		if (catalog != null && !catalog.isEmpty())
			dbname = catalog + ".";

		String onlyUnique = "";
		if (unique)
			onlyUnique = "  AND ix.is_unique = 1 \n";

		String sql = ""
			    + "DECLARE @objectname nvarchar(512) = '" + objectname + "' \n"
			    + "DECLARE @dbname     nvarchar(128) = PARSENAME(@objectname, 3) \n"
			    + "DECLARE @schname    nvarchar(128) = PARSENAME(@objectname, 2) \n"
			    + "DECLARE @tabname    nvarchar(128) = PARSENAME(@objectname, 1) \n"
			    + " \n"
	    	    + "SELECT \n"
	    	    + "     TABLE_CAT        = COALESCE(@dbname, db_name()) \n"
	    	    + "    ,TABLE_SCHEM      = COALESCE(@schname, 'dbo') \n"
	    	    + "    ,TABLE_NAME       = @tabname \n"
	    	    + "    ,NON_UNIQUE       = NULL \n"
	    	    + "    ,INDEX_QUALIFIER  = NULL \n"
	    	    + "    ,INDEX_NAME       = NULL \n"
	    	    + "    ,TYPE             = 0 \n"
	    	    + "    ,ORDINAL_POSITION = NULL \n"
	    	    + "    ,COLUMN_NAME      = NULL \n"
	    	    + "    ,ASC_OR_DESC      = NULL \n"
				+ "    ,CARDINALITY      = SUM(p.rows) \n"
				+ "    ,PAGES            = SUM(a.total_pages) \n"  // Number of PAGES for the whole table or just the "data"
				+ "    ,FILTER_CONDITION = NULL \n"
				+ "FROM " + dbname + "sys.partitions p \n"  // note: sys.dm_db_partition_stats requires: VIEW DATABASE PERFORMANCE STATE
				+ "INNER JOIN sys.allocation_units a ON p.partition_id = a.container_id \n"
				+ "WHERE p.index_id <= 1 \n"
				+ "  AND p.object_id = OBJECT_ID(@objectname) \n"
	    	    + " \n"
	    	    + "UNION ALL \n"
	    	    + ""
			    + "SELECT \n"
			    + "     TABLE_CAT        = COALESCE(@dbname, db_name()) \n"
			    + "    ,TABLE_SCHEM      = COALESCE(@schname, 'dbo') \n"
			    + "    ,TABLE_NAME       = @tabname \n"
			    + "    ,NON_UNIQUE       = CASE ix.is_unique WHEN 1 THEN 0 ELSE 1 END \n"
			    + "    ,INDEX_QUALIFIER  = @tabname \n"
			    + "    ,INDEX_NAME       = ix.name \n"
			    + "    ,TYPE             = CASE WHEN ix.type_desc like 'CLUSTERED%' THEN 1 ELSE 3 END \n"
			    + "    ,ORDINAL_POSITION = ic.index_column_id \n"
			    + "    ,COLUMN_NAME      = c.name \n"
			    + "    ,ASC_OR_DESC      = CASE WHEN ic.is_descending_key = 1 THEN 'D' ELSE 'A' END \n"
			    + "    ,CARDINALITY      = (SELECT SUM(xp.rows) \n"
			    + "                         FROM " + dbname + "sys.partitions xp \n"
			    + "                         WHERE ix.object_id = xp.object_id \n"
			    + "                           AND ix.index_id  = xp.index_id\n"
			    + "                        ) \n"
			    + "    ,PAGES            = (SELECT SUM(xa.total_pages) \n"
			    + "                         FROM       " + dbname + "sys.partitions xp \n"
			    + "                         INNER JOIN " + dbname + "sys.allocation_units xa ON xp.partition_id = xa.container_id \n"
			    + "                         WHERE xp.object_id = ix.object_id \n"
			    + "                           AND xp.index_id  = ix.index_id \n"
			    + "                        ) \n"
			    + "    ,FILTER_CONDITION = ix.filter_definition \n"
			    + "FROM      " + dbname + "sys.indexes ix \n"
			    + "LEFT JOIN " + dbname + "sys.partitions                p ON ix.object_id =  p.object_id AND ix.index_id  =  p.index_id \n"
			    + "LEFT JOIN " + dbname + "sys.index_columns            ic ON ix.object_id = ic.object_id AND ix.index_id  = ic.index_id \n"
			    + "LEFT JOIN " + dbname + "sys.columns                   c ON ic.object_id =  c.object_id AND ic.column_id =  c.column_id \n"
			    + "WHERE 1 = 1 \n"
			    + "  AND ix.object_id = OBJECT_ID(@objectname) \n"
			    + onlyUnique
			    + "  AND  p.partition_number = 1 /** For partitioned tables we only care about 1 partition, for getting PAGE/ROW count use subselects in the select list... **/ \n"
			    + "  AND ( \n"
			    + "          /* FOR 'ordinary' indexes (NON COLUMNSTORE INDEXES) we only want: ic.key_ordinal = 1 and NOT included columns */ \n"
			    + "          (ix.type_desc NOT LIKE '%COLUMNSTORE%' AND ic.key_ordinal = 1 AND ic.is_included_column = 0) \n"
			    + "       OR \n"
			    + "          /* FOR COLUMNSTORE INDEXES we only want ic.key_ordinal = 0 and ic.is_included_column = 1 */ \n"
			    + "          (ix.type_desc     LIKE '%COLUMNSTORE%' AND ic.key_ordinal = 0 AND ic.is_included_column = 1) \n"
			    + "      ) \n"
			    + "ORDER BY NON_UNIQUE, TYPE, INDEX_NAME, ORDINAL_POSITION \n"
			    + "";

		if (_logger.isDebugEnabled())
			_logger.debug("DbxDatabaseMetaDataSqlServer.getIndexInfo: SQL=\n|" + sql + "|");
		
		Statement stmnt = conn.createStatement();
		stmnt.setQueryTimeout(10);
	
		ResultSet rs = stmnt.executeQuery(sql);
		return rs;		
	}
}
