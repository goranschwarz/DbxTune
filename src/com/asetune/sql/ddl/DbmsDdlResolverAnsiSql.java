/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

public class DbmsDdlResolverAnsiSql 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverAnsiSql(DbxConnection conn)
	{
		super(conn);
	}


	/** 
	 * If you have user defined configured data type mappings, then this will return the <b>prefix</b> for searching in properties.
	 */
	@Override
	public String getPropertiesPrefix()
	{
		return "ansi";
	}


	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		// NOTHING TO DO HERE
	}
	
	/**
	 * Resolve JDBC Types -->> use as close to ANSI SQL as possible 
	 */
	@Override
	public String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale)
	{
		// from where:
		// -------------------------------------------------------------------------
		// ISO/IEC CD 9075-2:2013(E) ---  Information technology - Database languages - SQL - Part 2: Foundation (SQL/Foundation) Ed 5
		//   http://jtc1sc32.org/doc/N2301-2350/32N2311T-text_for_ballot-CD_9075-2.pdf
		//
		//   --------------------------------
		//   -- BEGIN: from above document --
		//   --------------------------------
		//   SQL defines predefined data types named by the following <key word>s:
		//       CHARACTER
		//       CHARACTER VARYING
		//       CHARACTER LARGE OBJECT
		//       BINARY
		//       BINARY VARYING
		//       BINARY LARGE OBJECT
		//       NUMERIC
		//       DECIMAL
		//       SMALLINT
		//       INTEGER
		//       BIGINT
		//       FLOAT
		//       REAL
		//       DOUBLE PRECISION
		//       BOOLEAN
		//       DATE
		//       TIME
		//       TIMESTAMP
		//       INTERVAL
		//   These names are used in the type designators that constitute the type precedence lists specified in Subclause 9.7, "Type precedence list determination".
        //   
		//   For reference purposes:
		//       * The data types CHARACTER, CHARACTER VARYING, and CHARACTER LARGE OBJECT are collectively referred to as character string types and the values of character string types are known as character strings.
		//       * The data types BINARY, BINARY VARYING, and BINARY LARGE OBJECT are referred to as binary string types and the values of binary string types are referred to as binary strings.
		//       * The data types CHARACTER LARGE OBJECT and BINARY LARGE OBJECT are collectively referred to as large object string types and the values of large object string types are referred to as large object strings.
		//       * Character string types and binary string types are collectively referred to as string types and values of string types are referred to as strings.
		//       * The data types NUMERIC, DECIMAL, SMALLINT, INTEGER, and BIGINT are collectively referred to as exact numeric types.
		//       * The data types FLOAT, REAL, and DOUBLE PRECISION are collectively referred to as approximate numeric types.
		//       * Exact numeric types and approximate numeric types are collectively referred to as numeric types. Values of numeric types are referred to as numbers.
		//       * The data types TIME WITHOUT TIME ZONE and TIME WITH TIME ZONE are collectively referred to as time types (or, for emphasis, as time with or without time zone).
		//       * The data types TIMESTAMP WITHOUT TIME ZONE and TIMESTAMP WITH TIME ZONE are collectively referred to as timestamp types (or, for emphasis, as timestamp with or without time zone).
		//       * The data types DATE, TIME, and TIMESTAMP are collectively referred to as datetime types.
		//       * Values of datetime types are referred to as datetimes.
		//       * The data type INTERVAL is referred to as an interval type. Values of interval types are called intervals.
        //   
		//   Each data type has an associated data type descriptor; the contents of a data type descriptor are determined by
		//   the specific data type that it describes. A data type descriptor includes an identification of the data type and all
		//   information needed to characterize a value of that data type.
		//   Subclause 6.1, "<data type>", describes the semantic properties of each data type.
		//   --------------------------------
		//   -- END: from above document   --
		//   --------------------------------
		//
		// -------------------------------------------------------------------------
		// Some Other parts might be interesting to look at:
		//   https://docs.microsoft.com/en-us/office/client-developer/access/desktop-database-reference/equivalent-ansi-sql-data-types
		//   https://www.cs.mun.ca/java-api-1.5/guide/jdbc/getstart/mapping.html
		//
		// -------------------------------------------------------------------------

		// Resolve specific data types for this DBMS Vendor
		switch (javaSqlType)
		{
		case java.sql.Types.BIT:                     return "SMALLINT";                      // ---- TINYINT is as close as we can get
		case java.sql.Types.TINYINT:                 return "SMALLINT";                      // ---- TINYINT is as close as we can get
		case java.sql.Types.SMALLINT:                return "SMALLINT";                      // ANSI
		case java.sql.Types.INTEGER:                 return "INTEGER";                       // ANSI
		case java.sql.Types.BIGINT:                  return "BIGINT";                        // ANSI
		case java.sql.Types.FLOAT:                   return "FLOAT";                         // ANSI
		case java.sql.Types.REAL:                    return "REAL";                          // ANSI
		case java.sql.Types.DOUBLE:                  return "DOUBLE PRECISION";              // ANSI
		case java.sql.Types.NUMERIC:                 return "NUMERIC("+length+","+scale+")"; // ANSI
		case java.sql.Types.DECIMAL:                 return "DECIMAL("+length+","+scale+")"; // ANSI
		case java.sql.Types.CHAR:                    return "CHARACTER("+length+")";         // ANSI has 'CHARACTER'          but we could have mapped it to 'CHAR'
		case java.sql.Types.VARCHAR:                 return "CHARACTER VARYING("+length+")"; // ANSI has 'CHARACTER VARYING'  but we could have mapped it to 'VARCHAR'
		case java.sql.Types.LONGVARCHAR:             return "CHARACTER LARGE OBJECT";        // ANSI
		case java.sql.Types.DATE:                    return "DATE";                          // ANSI
		case java.sql.Types.TIME:                    return "TIME";                          // ANSI
		case java.sql.Types.TIMESTAMP:               return "TIMESTAMP";                     // ANSI
		case java.sql.Types.BINARY:                  return "BINARY("+length+")";            // ANSI
		case java.sql.Types.VARBINARY:               return "BINARY VARYING("+length+")";    // ANSI
		case java.sql.Types.LONGVARBINARY:           return "BINARY LARGE OBJECT";           // ANSI has 'BINARY LARGE OBJECT' but we could have mapped it to 'BLOB'
		case java.sql.Types.NULL:                    return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.OTHER:                   return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.JAVA_OBJECT:             return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.DISTINCT:                return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.STRUCT:                  return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.ARRAY:                   return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.BLOB:                    return "BINARY LARGE OBJECT";           // ANSI has 'BINARY LARGE OBJECT'    but we could have mapped it to 'BLOB'
		case java.sql.Types.CLOB:                    return "CHARACTER LARGE OBJECT";        // ANSI has 'CHARACTER LARGE OBJECT' but we could have mapped it to 'CLOB'
		case java.sql.Types.REF:                     return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.DATALINK:                return "BINARY LARGE OBJECT";           // ---- NOT part of ANSI - map to BLOB
		case java.sql.Types.BOOLEAN:                 return "BOOLEAN";                       // ANSI

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "CHARACTER VARYING(20)";                  // Just guessing here... from https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1846
		case java.sql.Types.NCHAR:                   return "NATIONAL CHARACTER("+length+")";         // ANSI has 'NATIONAL CHARACTER'         but we could have mapped it to 'NCHAR'
		case java.sql.Types.NVARCHAR:                return "NATIONAL CHARACTER VARYING("+length+")"; // ANSI has 'NATIONAL CHARACTER VARYING' but we could have mapped it to 'NVARCHAR'
		case java.sql.Types.LONGNVARCHAR:            return "NATIONAL CHARACTER LARGE OBJECT";        // ANSI
		case java.sql.Types.NCLOB:                   return "NATIONAL CHARACTER LARGE OBJECT";        // ANSI
		case java.sql.Types.SQLXML:                  return "NATIONAL CHARACTER LARGE OBJECT";        // ---- NOT part of ANSI - map to NCLOB

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "TIME WITH TIME ZONE";                    // ANSI -- but probably not implemented by many DBMS vendors
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "TIMESTAMP WITH TIME ZONE";               // ANSI -- but probably not implemented by many DBMS vendors

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "CHARACTER VARYING(36)";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "CHARACTER LARGE OBJECT";

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
