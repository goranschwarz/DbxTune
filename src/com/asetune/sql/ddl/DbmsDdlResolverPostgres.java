/*******************************************************************************
DbmsDdlResolverDerbyDbmsDdlResolverDb2 * Copyright (C) 2010-2020 Goran Schwarz
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

import java.sql.Types;

import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;

public class DbmsDdlResolverPostgres 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverPostgres(DbxConnection conn)
	{
		super(conn);
	}

	/** 
	 * If you have user defined configured data type mappings, then this will return the <b>prefix</b> for searching in properties.
	 */
	@Override
	public String getPropertiesPrefix()
	{
		return "postgres";
	}


	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		String dt = entry.getColumnTypeName();

		if (false)
		{
			// Dummy to ...
		}

//		else if ("oid".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.BIGINT);
//			entry.setColumnTypeName("bigint");
//		}
//		
//		else if ("int2".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.SMALLINT);
//			entry.setColumnTypeName("smallint");
//		}
//
//		else if ("int4".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.INTEGER);
//			entry.setColumnTypeName("int");
//		}
//
//		else if ("int8".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.BIGINT);
//			entry.setColumnTypeName("bigint");
//		}

		else if ("name".equalsIgnoreCase(dt) && entry.getColumnDisplaySize() == 2147483647)
		{
			entry.setColumnType(Types.VARCHAR);
			entry.setColumnTypeName("varchar");
			entry.setPrecision(255);
			entry.setColumnDisplaySize(255);
		}

		else if ("uuid".equalsIgnoreCase(dt) && entry.getColumnDisplaySize() == 2147483647)
		{
			entry.setColumnType(ExtendedTypes.DBXTUNE_TYPE_UUID);
//			entry.setColumnType(Types.VARCHAR);
//			entry.setPrecision(36);
//			entry.setColumnDisplaySize(36);
		}

		else if ( ("json".equalsIgnoreCase(dt) || "jsonb".equalsIgnoreCase(dt)) && entry.getColumnDisplaySize() == 2147483647)
		{
			entry.setColumnType(ExtendedTypes.DBXTUNE_TYPE_JSON);
//			entry.setColumnTypeName("varchar");
//			entry.setPrecision(36);
//			entry.setColumnDisplaySize(36);
		}

//		else if ("float8".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.DOUBLE);
//			entry.setColumnTypeName("double");
//		}
//
//		else if ("float4".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.REAL);
//			entry.setColumnTypeName("real");
//		}
//
//		else if ("timestamptz".equalsIgnoreCase(dt))
//		{
//			entry.setColumnType(Types.TIMESTAMP);
//			entry.setColumnTypeName("timestamp");
//
//			// or
//			//entry.setColumnType(Types.TIMESTAMP_WITH_TIMEZONE);
//			//entry.setColumnTypeName("timestamp with time zone");
//		}

		else if ("xid".equalsIgnoreCase(dt))
		{
			entry.setColumnType(Types.VARCHAR);
			entry.setColumnTypeName("varchar");
			entry.setPrecision(30);
			entry.setColumnDisplaySize(30);
		}

		// Network Address Types -- https://www.postgresql.org/docs/9.1/datatype-net-types.html
		else if ("inet".equalsIgnoreCase(dt) || "cidr".equalsIgnoreCase(dt) || "macaddr".equalsIgnoreCase(dt))
		{
			entry.setColumnType(Types.VARCHAR);
			entry.setColumnTypeName("varchar");
			entry.setPrecision(30);
			entry.setColumnDisplaySize(30);
		}

//		else if ("bool".equalsIgnoreCase(dt))
//		{
//			// is this needed
//			entry.setColumnType(Types.BOOLEAN);
//			entry.setColumnTypeName("bit");
//		}

//		// NOT HERE: This will instead be translated by the "Target" as it "hopefully" has Types.XML
//		else if ("xml".equalsIgnoreCase(dt)) // not sure about this one
//		{
//			entry.setColumnType(Types.LONGVARCHAR);
//			entry.setColumnTypeName("text");
//		}
	}

//	@Override
//	public void dbmsVendorDataTypeResolverForSource(Entry entry)
//	{
//		String dt = _columnTypeName[index];
//
//		if ("oid".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.BIGINT;
//			_columnTypeName[index] = "bigint";
//		}
//
//		else if ("int2".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.SMALLINT;
//			_columnTypeName[index] = "smallint";
//		}
//
//		else if ("int4".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.INTEGER;
//			_columnTypeName[index] = "int";
//		}
//
//		else if ("int8".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.BIGINT;
//			_columnTypeName[index] = "bigint";
//		}
//
//		else if ("name".equalsIgnoreCase(dt) && _columnDisplaySize[index] == 2147483647)
//		{
//			_columnType    [index] = Types.VARCHAR;
//			_columnTypeName[index] = "varchar";
//			_columnDisplaySize[index] = 256;
//		}
//
//		else if ("float8".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.DOUBLE;
//			_columnTypeName[index] = "double";
//		}
//
//		else if ("float4".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.REAL;
//			_columnTypeName[index] = "real";
//		}
//
//		else if ("timestamptz".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.TIMESTAMP;
//			_columnTypeName[index] = "timestamp";
//		}
//
//		else if ("xid".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.VARCHAR;
//			_columnTypeName[index] = "varchar";
//			_columnDisplaySize[index] = 30;
//		}
//
//		else if ("inet".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.VARCHAR;
//			_columnTypeName[index] = "varchar";
//			_columnDisplaySize[index] = 30;
//		}
//
//		else if ("bool".equalsIgnoreCase(dt))
//		{
//			_columnType    [index] = Types.BOOLEAN;
//			_columnTypeName[index] = "bit";
//		}
//
//		else if ("xml".equalsIgnoreCase(dt)) // not sure about this one
//		{
//			_columnType    [index] = Types.LONGVARCHAR;
//			_columnTypeName[index] = "text";
//		}
//	}


	/**
	 * Resolve JDBC Types -->> Postgres 
	 */
	@Override
	public String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale)
	{
		// https://www.postgresql.org/docs/9.5/datatype.html
		// https://www.postgresqltutorial.com/postgresql-data-types/

		// Resolve specific data types for this DBMS Vendor
		switch (javaSqlType)
		{
		case java.sql.Types.BIT:                     return "boolean";
		case java.sql.Types.TINYINT:                 return "smallint";  // Postgres do not have tinyint, so emulate with smallint or possibly ???
		case java.sql.Types.SMALLINT:                return "smallint";
		case java.sql.Types.INTEGER:                 return "int";
		case java.sql.Types.BIGINT:                  return "bigint";
		case java.sql.Types.FLOAT:                   return "float8";
		case java.sql.Types.REAL:                    return "real";
		case java.sql.Types.DOUBLE:                  return "float8";
		case java.sql.Types.NUMERIC:                 return "numeric("+length+","+scale+")";
		case java.sql.Types.DECIMAL:                 return "decimal("+length+","+scale+")";
		case java.sql.Types.CHAR:                    return "char("+length+")";
		case java.sql.Types.VARCHAR:                 return "varchar("+length+")";
		case java.sql.Types.LONGVARCHAR:             return "text";
		case java.sql.Types.DATE:                    return "date";
		case java.sql.Types.TIME:                    return "time";
		case java.sql.Types.TIMESTAMP:               return "timestamp";
		case java.sql.Types.BINARY:                  return "bytea";
		case java.sql.Types.VARBINARY:               return "bytea";
		case java.sql.Types.LONGVARBINARY:           return "bytea";
		case java.sql.Types.NULL:                    return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.OTHER:                   return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.JAVA_OBJECT:             return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.DISTINCT:                return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.STRUCT:                  return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.ARRAY:                   return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.BLOB:                    return "bytea";
		case java.sql.Types.CLOB:                    return "text";
		case java.sql.Types.REF:                     return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.DATALINK:                return "bytea";                      // Not really supported just use 'bytea'
		case java.sql.Types.BOOLEAN:                 return "boolean";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "varchar(20)";                 // Just guessing here... from https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1846
		case java.sql.Types.NCHAR:                   return "char("+length+")";
		case java.sql.Types.NVARCHAR:                return "varchar("+length+")";
		case java.sql.Types.LONGNVARCHAR:            return "text";
		case java.sql.Types.NCLOB:                   return "text";
		case java.sql.Types.SQLXML:                  return "xml";

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "timetz";
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "timestamptz";

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "uuid";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "json";

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
