/*******************************************************************************
DbmsDdlResolverDerbyDbmsDdlResolverDb2 * Copyright (C) 2010-2019 Goran Schwarz
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

public class DbmsDdlResolverOracle 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverOracle(DbxConnection conn)
	{
		super(conn);
	}

	/** 
	 * If you have user defined configured data type mappings, then this will return the <b>prefix</b> for searching in properties.
	 */
	@Override
	public String getPropertiesPrefix()
	{
		return "oracle";
	}


	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		if (entry.getColumnType() == Types.NUMERIC)
		{
			int length = entry.getPrecision();
			int scale  = entry.getScale();
			
			if      ( length ==  0 && scale == -127 ) { entry.setColumnType(Types.INTEGER); entry.setColumnTypeName("int"   ); System.out.println("REMAPPING(ORACLE) index="+entry.getColumnPos()+", columnName=["+entry.getColumnName()+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+entry.getColumnTypeName()+" -------to>>>>>> INTEGER");}
			else if ( length ==  0 && scale ==    0 ) { entry.setColumnType(Types.INTEGER); entry.setColumnTypeName("int"   ); System.out.println("REMAPPING(ORACLE) index="+entry.getColumnPos()+", columnName=["+entry.getColumnName()+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+entry.getColumnTypeName()+" -------to>>>>>> INTEGER");}
			else if ( length == 10 && scale ==    0 ) { entry.setColumnType(Types.INTEGER); entry.setColumnTypeName("int"   ); System.out.println("REMAPPING(ORACLE) index="+entry.getColumnPos()+", columnName=["+entry.getColumnName()+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+entry.getColumnTypeName()+" -------to>>>>>> INTEGER");}
			else if ( length == 19 && scale ==    0 ) { entry.setColumnType(Types.BIGINT ); entry.setColumnTypeName("bigint"); System.out.println("REMAPPING(ORACLE) index="+entry.getColumnPos()+", columnName=["+entry.getColumnName()+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+entry.getColumnTypeName()+" -------to>>>>>> BIGINT");}
			else if ( length == 38 && scale ==    0 ) { entry.setColumnType(Types.BIGINT ); entry.setColumnTypeName("bigint"); System.out.println("REMAPPING(ORACLE) index="+entry.getColumnPos()+", columnName=["+entry.getColumnName()+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+entry.getColumnTypeName()+" -------to>>>>>> BIGINT");}
		}
	}


	/**
	 * Resolve JDBC Types -->> ORACLE 
	 */
	@Override
	public String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale)
	{
		// http://db.apache.org/ddlutils/databases/oracle.html
		// https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT713
		// https://www.techonthenet.com/oracle/datatypes.php

		// Resolve specific data types for this DBMS Vendor
		switch (javaSqlType)
		{
		case java.sql.Types.BIT:                     return "number(1)";
		case java.sql.Types.TINYINT:                 return "number(3)";
		case java.sql.Types.SMALLINT:                return "number(5)";
		case java.sql.Types.INTEGER:                 return "integer";
		case java.sql.Types.BIGINT:                  return "number(19)";			//  - Various pages on Internet says to map BIGINT to number(38,0) 
		                                                                            //    but a 38 number can't fit into a 32-byte-integer, 
		                                                                            //    maybe we shoudl use 19 digits: 64-bit-int-MAX: 9_223_372_036_854_775_807)
		case java.sql.Types.FLOAT:                   return "float";
		case java.sql.Types.REAL:                    return "real";
		case java.sql.Types.DOUBLE:                  return "double precision";
		case java.sql.Types.NUMERIC:                 return "number("+length+","+scale+")";
		case java.sql.Types.DECIMAL:                 return "number("+length+","+scale+")";
		case java.sql.Types.CHAR:                    return "char("+length+")";
		case java.sql.Types.VARCHAR:                 return "varchar2("+length+")";
		case java.sql.Types.LONGVARCHAR:             return "clob";
		case java.sql.Types.DATE:                    return "date";
		case java.sql.Types.TIME:                    return "date";    // TIME is not supported and ALL documents are saying just DATE instead... will that work ????
		case java.sql.Types.TIMESTAMP:               return "timestamp";
		case java.sql.Types.BINARY:                  return "raw("+length+")";
		case java.sql.Types.VARBINARY:               return "raw("+length+")";
		case java.sql.Types.LONGVARBINARY:           return "blob";
		case java.sql.Types.NULL:                    return "blob";
		case java.sql.Types.OTHER:                   return "blob";
		case java.sql.Types.JAVA_OBJECT:             return "blob";
		case java.sql.Types.DISTINCT:                return "blob";
		case java.sql.Types.STRUCT:                  return "blob";
		case java.sql.Types.ARRAY:                   return "blob";
		case java.sql.Types.BLOB:                    return "blob";
		case java.sql.Types.CLOB:                    return "clob";
		case java.sql.Types.REF:                     return "blob";
		case java.sql.Types.DATALINK:                return "blob";
		case java.sql.Types.BOOLEAN:                 return "number(1)";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "urowid";
		case java.sql.Types.NCHAR:                   return "nchar("+length+")";
		case java.sql.Types.NVARCHAR:                return "nvarchar2("+length+")";
		case java.sql.Types.LONGNVARCHAR:            return "nclob";
		case java.sql.Types.NCLOB:                   return "nclob";
		case java.sql.Types.SQLXML:                  return "xmltype";

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "varchar2(40)";                 // Not really supported, should we use varchar(40) instead ??? -->>             hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 40 chars 
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "timestamp with time zone";

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "varchar2(36)";
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
