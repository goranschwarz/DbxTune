/*******************************************************************************
DbmsDdlResolverDb2 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.sql.ddl;

import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;

public class DbmsDdlResolverDb2 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverDb2(DbxConnection conn)
	{
		super(conn);
	}

	/** 
	 * If you have user defined configured data type mappings, then this will return the <b>prefix</b> for searching in properties.
	 */
	@Override
	public String getPropertiesPrefix()
	{
		return "db2";
	}


	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		// NOTHING TO DO HERE
	}


	/**
	 * Resolve JDBC Types -->> DB2 
	 */
	@Override
	public String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale)
	{
		// https://www.ibm.com/support/knowledgecenter/es/SSEPGG_9.7.0/com.ibm.db2.luw.sql.ref.doc/doc/r0000927.html
		// https://www.ibm.com/support/knowledgecenter/SSEPGG_9.7.0/com.ibm.db2.luw.apdv.plsql.doc/doc/c0053846.html
		// https://db.apache.org/ddlutils/databases/db2.html

		// Resolve specific data types for this DBMS Vendor
		switch (javaSqlType)
		{
		case java.sql.Types.BIT:                     return "boolean";
		case java.sql.Types.TINYINT:                 return "smallint";  // DB2 do not have tinyint, so emulate with smallint or possibly ???
		case java.sql.Types.SMALLINT:                return "smallint";
		case java.sql.Types.INTEGER:                 return "int";
		case java.sql.Types.BIGINT:                  return "bigint";
		case java.sql.Types.FLOAT:                   return "float";
		case java.sql.Types.REAL:                    return "real";
		case java.sql.Types.DOUBLE:                  return "double";
		case java.sql.Types.NUMERIC:                 return "numeric("+length+","+scale+")";
		case java.sql.Types.DECIMAL:                 return "decimal("+length+","+scale+")";
		case java.sql.Types.CHAR:                    return "char("+length+")";
		case java.sql.Types.VARCHAR:                 return "varchar("+length+")";
		case java.sql.Types.LONGVARCHAR:             return "clob";
		case java.sql.Types.DATE:                    return "date";
		case java.sql.Types.TIME:                    return "time";
		case java.sql.Types.TIMESTAMP:               return "timestamp";
		case java.sql.Types.BINARY:                  return "blob("+length+")";
		case java.sql.Types.VARBINARY:               return "blob("+length+")";
		case java.sql.Types.LONGVARBINARY:           return "blob";
		case java.sql.Types.NULL:                    return "long varchar for bit data"; // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.OTHER:                   return "blob";                      // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.JAVA_OBJECT:             return "blob";                      // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.DISTINCT:                return "distinct";                  // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.STRUCT:                  return "blob";                      // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.ARRAY:                   return "blob";                      // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.BLOB:                    return "blob";
		case java.sql.Types.CLOB:                    return "clob";
		case java.sql.Types.REF:                     return "ref";                       // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.DATALINK:                return "datalink";                  // https://db.apache.org/ddlutils/databases/db2.html
		case java.sql.Types.BOOLEAN:                 return "boolean";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "varchar(20)";                 // Just guessing here... from https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1846
		case java.sql.Types.NCHAR:                   return "nchar("+length+")";
		case java.sql.Types.NVARCHAR:                return "nvarchar("+length+")";
		case java.sql.Types.LONGNVARCHAR:            return "nclob";
		case java.sql.Types.NCLOB:                   return "nclob";
		case java.sql.Types.SQLXML:                  return "xml";

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "varchar(40)";                 // Not really supported, should we use varchar(40) instead ??? -->>             hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 40 chars 
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "varchar(50)";                 // Not really supported, should we use varchar(50) instead ??? -->>  YYYY-MM-DD hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 50 chars

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "varchar(36)";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "nclob";

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
		case -100:                                   return null;    // oracle.jdbc.OracleTypes.TIMESTAMPNS
		case -101:                                   return null;    // oracle.jdbc.OracleTypes.TIMESTAMPTZ
		case -102:                                   return null;    // oracle.jdbc.OracleTypes.TIMESTAMPLTZ
		case -103:                                   return null;    // oracle.jdbc.OracleTypes.INTERVALYM
		case -104:                                   return null;    // oracle.jdbc.OracleTypes.INTERVALDS
		case  -10:                                   return null;    // oracle.jdbc.OracleTypes.CURSOR
		case  -13:                                   return null;    // oracle.jdbc.OracleTypes.BFILE
		case 2007:                                   return null;    // oracle.jdbc.OracleTypes.OPAQUE
		case 2008:                                   return null;    // oracle.jdbc.OracleTypes.JAVA_STRUCT
		case  -14:                                   return null;    // oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE
		case  100:                                   return null;    // oracle.jdbc.OracleTypes.BINARY_FLOAT
		case  101:                                   return null;    // oracle.jdbc.OracleTypes.BINARY_DOUBLE
//		case    2:                                   return null;    // oracle.jdbc.OracleTypes.NUMBER             // same as: java.sql.Types.NUMERIC
//		case   -2:                                   return null;    // oracle.jdbc.OracleTypes.RAW                // same as: java.sql.Types.BINARY
		case  999:                                   return null;    // oracle.jdbc.OracleTypes.FIXED_CHAR

		default:
			return null;
		}
	}
}
