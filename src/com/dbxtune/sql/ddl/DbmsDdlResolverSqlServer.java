/*******************************************************************************
DbmsDdlResolverRsDaDbmsDdlResolverRaxDbmsDdlResolverDerbyDbmsDdlResolverDb2 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.sql.ddl;

import java.sql.Types;

import com.dbxtune.sql.ResultSetMetaDataCached.Entry;
import com.dbxtune.sql.conn.DbxConnection;

public class DbmsDdlResolverSqlServer 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverSqlServer(DbxConnection conn)
	{
		super(conn);
	}

	/** 
	 * If you have user defined configured data type mappings, then this will return the <b>prefix</b> for searching in properties.
	 */
	@Override
	public String getPropertiesPrefix()
	{
		return "sqlserver";
	}


//	/**
//	 * For Sybase ASE and SQL-Server escaping square brackets inside a column name or an identifier is done using<br>
//	 * <code>[Bring-The-[AAAA]]-Game]</code> is equal to <code>Bring-The-[AAAA]-Game</code> <br>
//	 * so all ']' should be escaped as ']]'... but starting brackets '[' should NOT be touched
//	 * <p>
//	 * The passed value of name is WITHOUT the surrounding brackets, for the above example the input value is <code>Bring-The-[AAAA]-Game</code>
//	 * <p>
//	 * <b>Example:</b> <code>escapeQuotedIdentifier("Bring-The-[AAAA]-Game")</code><br>
//	 * <b>Returns:</b> <code>"Bring-The-[AAAA]]-Game"</code> 
//	 */
//	@Override
//	public String escapeQuotedIdentifier(String name)
//	{
//		if (name.indexOf(']') != -1)
//			name = name.replace("]", "]]");
//
//		return name;
//	}

	@Override
	public boolean skipCreateSchemaWithName(String schemaName)
	{
		if (schemaName == null)
			return true;

		// for Sybase ASE and Microsoft SQL-Server
		if (schemaName.equalsIgnoreCase("dbo")) return true;
		if (schemaName.equalsIgnoreCase("sys")) return true;

		// for H2 and "others"
//		if (schemaName.equalsIgnoreCase("PUBLIC"            )) return true;
//		if (schemaName.equalsIgnoreCase("INFORMATION_SCHEMA")) return true;

		// Everything else... create the schema
		return false;
	}

	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		// https://docs.microsoft.com/en-us/sql/t-sql/data-types/uniqueidentifier-transact-sql
		if ("uniqueidentifier".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(ExtendedTypes.DBXTUNE_TYPE_UUID);
//			entry.setColumnType(Types.CHAR);
//			entry.setColumnTypeName("char");
//			entry.setPrecision(36);             // XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
		}
		
		switch (entry.getColumnType())
		{
	    case -155: // "microsoft.sql.DATETIMEOFFSET";
			entry.setColumnType(Types.TIMESTAMP_WITH_TIMEZONE);
	    	break;

	    case -153: // "microsoft.sql.STRUCTURED";
	    	//??????????????????????????????????????????????
	    	break;

	    case -151: // "microsoft.sql.DATETIME";
			entry.setColumnType(Types.TIMESTAMP);
	    	break;
	    
	    case -150: // "microsoft.sql.SMALLDATETIME";
			entry.setColumnType(Types.TIMESTAMP);
	    	break;
	    
	    case -148: // "microsoft.sql.MONEY";
			entry.setColumnType(Types.DECIMAL);
			entry.setPrecision(19);
			entry.setScale(4);
	    	break;
	    
	    case -146: // "microsoft.sql.SMALLMONEY";
			entry.setColumnType(Types.DECIMAL);
			entry.setPrecision(10);
			entry.setScale(4);
	    	break;
	    
	    case -145: // "microsoft.sql.GUID";
			entry.setColumnType(Types.CHAR);
			entry.setColumnTypeName("char");
			entry.setPrecision(36);             // XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX
	    	break;
	    
	    case -156: // "microsoft.sql.SQL_VARIANT";
			entry.setColumnType(Types.VARBINARY);
			entry.setColumnTypeName("varbinary");
			entry.setPrecision(8000);
	    	break;
	    
	    case -157: // "microsoft.sql.GEOMETRY";
	    	//??????????????????????????????????????????????
	    	break;
	    
	    case -158: // "microsoft.sql.GEOGRAPHY";
	    	//??????????????????????????????????????????????
			break;
		}

		// NOT HERE: This will instead be translated by the "Target" as it "hopefully" has Types.XML
//		if ("xml".equalsIgnoreCase(entry.getColumnTypeName()))
//		{
//			entry.setColumnType(Types.LONGVARCHAR);
//			entry.setColumnTypeName("text");
//		}
	}


	// Private helper methods if: *varchar is above max storage, then return: *varchar(max)
	private String varcharFix (int len) { return (len > 8000) ? "varchar(max)"  : "varchar("  + len + ")"; }
	private String nvarcharFix(int len) { return (len > 4000) ? "nvarchar(max)" : "nvarchar(" + len + ")"; }

	/**
	 * Resolve JDBC Types -->> Microsoft SQL-Server 
	 */
	@Override
	public String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale)
	{
		// Resolve specific data types for this DBMS Vendor
		switch (javaSqlType)
		{
		case java.sql.Types.BIT:                     return "bit";
		case java.sql.Types.TINYINT:                 return "tinyint";
		case java.sql.Types.SMALLINT:                return "smallint";
		case java.sql.Types.INTEGER:                 return "int";
		case java.sql.Types.BIGINT:                  return "bigint";
		case java.sql.Types.FLOAT:                   return "float";
		case java.sql.Types.REAL:                    return "real";
		case java.sql.Types.DOUBLE:                  return "double precision";
		case java.sql.Types.NUMERIC:                 return "numeric("+length+","+scale+")";
		case java.sql.Types.DECIMAL:                 return "decimal("+length+","+scale+")";
		case java.sql.Types.CHAR:                    return "char("+length+")";
		case java.sql.Types.VARCHAR:                 return varcharFix(length);           // if ABOVE 8000 -> varchar(max)
		case java.sql.Types.LONGVARCHAR:             return "varchar(max)";
		case java.sql.Types.DATE:                    return "date";
		case java.sql.Types.TIME:                    return "time";
		case java.sql.Types.TIMESTAMP:               return "datetime";
		case java.sql.Types.BINARY:                  return "binary("+length+")";
		case java.sql.Types.VARBINARY:               return "varbinary("+length+")";
		case java.sql.Types.LONGVARBINARY:           return "image";
		case java.sql.Types.NULL:                    return "image";                      // Not really supported just use 'image'
		case java.sql.Types.OTHER:                   return "image";                      // Not really supported just use 'image'
		case java.sql.Types.JAVA_OBJECT:             return "image";                      // Not really supported just use 'image'
		case java.sql.Types.DISTINCT:                return "image";                      // Not really supported just use 'image'
		case java.sql.Types.STRUCT:                  return "image";                      // Not really supported just use 'image'
		case java.sql.Types.ARRAY:                   return "image";                      // Not really supported just use 'image'
		case java.sql.Types.BLOB:                    return "image";
		case java.sql.Types.CLOB:                    return "varchar(max)";
		case java.sql.Types.REF:                     return "image";                      // Not really supported just use 'image'
		case java.sql.Types.DATALINK:                return "image";                      // Not really supported just use 'image'
		case java.sql.Types.BOOLEAN:                 return "bit";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "varchar(20)";                 // Just guessing here... from https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1846
		case java.sql.Types.NCHAR:                   return "nchar("+length+")";
		case java.sql.Types.NVARCHAR:                return nvarcharFix(length);          // if ABOVE 4000 -> nvarchar(max)
		case java.sql.Types.LONGNVARCHAR:            return "nvarchar(max)";
		case java.sql.Types.NCLOB:                   return "nvarchar(max)";
		case java.sql.Types.SQLXML:                  return "xml";

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "varchar(40)";                 // Not really supported, should we use varchar(40) instead ??? -->>             hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 40 chars 
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "datetimeoffset";

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "uniqueidentifier";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "nvarchar(max)";

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
