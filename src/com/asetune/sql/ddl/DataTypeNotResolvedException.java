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

public class DataTypeNotResolvedException 
extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	private int _jdbcType;
	
	public DataTypeNotResolvedException(int jdbcType, int length, int scale, String msg)
	{
		super(msg);

		_jdbcType = jdbcType;
	}

	/**
	 * Get the JDBC "java.sql.Types.*" number
	 * @return
	 */
	public int getJdbcType()
	{
		return _jdbcType;
	}

	
	/**
	 * Get the JDBC "java.sql.Types.*" to a String
	 * 
	 * @return A String looking like: <code>java.sql.Types.NAME_OF_THE_JDBC_TYPE</code>
	 */
	public String getJdbcTypeAsString()
	{
		return getJdbcTypeAsString(_jdbcType);
	}

	/**
	 * Translate a JDBC "java.sql.Types.*" to a String
	 * 
	 * @param jdbcType
	 * @return A String looking like: <code>java.sql.Types.NAME_OF_THE_JDBC_TYPE</code>
	 */
	public static String getJdbcTypeAsString(int jdbcType)
	{
		switch (jdbcType)
		{
		case java.sql.Types.BIT:                     return "java.sql.Types.BIT";
		case java.sql.Types.TINYINT:                 return "java.sql.Types.TINYINT";
		case java.sql.Types.SMALLINT:                return "java.sql.Types.SMALLINT";
		case java.sql.Types.INTEGER:                 return "java.sql.Types.INTEGER";
		case java.sql.Types.BIGINT:                  return "java.sql.Types.BIGINT";
		case java.sql.Types.FLOAT:                   return "java.sql.Types.FLOAT";
		case java.sql.Types.REAL:                    return "java.sql.Types.REAL";
		case java.sql.Types.DOUBLE:                  return "java.sql.Types.DOUBLE";
		case java.sql.Types.NUMERIC:                 return "java.sql.Types.NUMERIC";
		case java.sql.Types.DECIMAL:                 return "java.sql.Types.DECIMAL";
		case java.sql.Types.CHAR:                    return "java.sql.Types.CHAR";
		case java.sql.Types.VARCHAR:                 return "java.sql.Types.VARCHAR";
		case java.sql.Types.LONGVARCHAR:             return "java.sql.Types.LONGVARCHAR";
		case java.sql.Types.DATE:                    return "java.sql.Types.DATE";
		case java.sql.Types.TIME:                    return "java.sql.Types.TIME";
		case java.sql.Types.TIMESTAMP:               return "java.sql.Types.TIMESTAMP";
		case java.sql.Types.BINARY:                  return "java.sql.Types.BINARY";
		case java.sql.Types.VARBINARY:               return "java.sql.Types.VARBINARY";
		case java.sql.Types.LONGVARBINARY:           return "java.sql.Types.LONGVARBINARY";
		case java.sql.Types.NULL:                    return "java.sql.Types.NULL";
		case java.sql.Types.OTHER:                   return "java.sql.Types.OTHER";
		case java.sql.Types.JAVA_OBJECT:             return "java.sql.Types.JAVA_OBJECT";
		case java.sql.Types.DISTINCT:                return "java.sql.Types.DISTINCT";
		case java.sql.Types.STRUCT:                  return "java.sql.Types.STRUCT";
		case java.sql.Types.ARRAY:                   return "java.sql.Types.ARRAY";
		case java.sql.Types.BLOB:                    return "java.sql.Types.BLOB";
		case java.sql.Types.CLOB:                    return "java.sql.Types.CLOB";
		case java.sql.Types.REF:                     return "java.sql.Types.REF";
		case java.sql.Types.DATALINK:                return "java.sql.Types.DATALINK";
		case java.sql.Types.BOOLEAN:                 return "java.sql.Types.BOOLEAN";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "java.sql.Types.ROWID";
		case java.sql.Types.NCHAR:                   return "java.sql.Types.NCHAR";
		case java.sql.Types.NVARCHAR:                return "java.sql.Types.NVARCHAR";
		case java.sql.Types.LONGNVARCHAR:            return "java.sql.Types.LONGNVARCHAR";
		case java.sql.Types.NCLOB:                   return "java.sql.Types.NCLOB";
		case java.sql.Types.SQLXML:                  return "java.sql.Types.SQLXML";

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return "java.sql.Types.REF_CURSOR";
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "java.sql.Types.TIME_WITH_TIMEZONE";
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "java.sql.Types.TIMESTAMP_WITH_TIMEZONE";
		

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "dbxtune.ExtendedTypes.UUID";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "dbxtune.ExtendedTypes.JSON";

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
		case -100:                                   return "oracle.jdbc.OracleTypes.TIMESTAMPNS";
		case -101:                                   return "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
		case -102:                                   return "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
		case -103:                                   return "oracle.jdbc.OracleTypes.INTERVALYM";
		case -104:                                   return "oracle.jdbc.OracleTypes.INTERVALDS";
		case  -10:                                   return "oracle.jdbc.OracleTypes.CURSOR";
		case  -13:                                   return "oracle.jdbc.OracleTypes.BFILE";
		case 2007:                                   return "oracle.jdbc.OracleTypes.OPAQUE";
		case 2008:                                   return "oracle.jdbc.OracleTypes.JAVA_STRUCT";
		case  -14:                                   return "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
		case  100:                                   return "oracle.jdbc.OracleTypes.BINARY_FLOAT";
		case  101:                                   return "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
//		case    2:                                   return "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
//		case   -2:                                   return "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
		case  999:                                   return "oracle.jdbc.OracleTypes.FIXED_CHAR";

	    case -155:                                   return "microsoft.sql.DATETIMEOFFSET";
	    case -153:                                   return "microsoft.sql.STRUCTURED";
	    case -151:                                   return "microsoft.sql.DATETIME";
	    case -150:                                   return "microsoft.sql.SMALLDATETIME";
	    case -148:                                   return "microsoft.sql.MONEY";
	    case -146:                                   return "microsoft.sql.SMALLMONEY";
	    case -145:                                   return "microsoft.sql.GUID";
	    case -156:                                   return "microsoft.sql.SQL_VARIANT";
	    case -157:                                   return "microsoft.sql.GEOMETRY";
	    case -158:                                   return "microsoft.sql.GEOGRAPHY";
		
		//------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return "unknown-jdbc-datatype("+jdbcType+")";
		}
		
	}
}
