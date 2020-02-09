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
package com.asetune.sql.ddl;

/**
 * "Extension" of java.sql.Types so we can have specialized types
 *
 */
public class ExtendedTypes
{
	/**
	 * DbxTune -- Specify that it's a UUID - Universal Unique IDentifier
	 */
	public static final int DBXTUNE_TYPE_UUID = -690421;
	public static final int DBXTUNE_TYPE_JSON = -690422;


//	//------------------------- ORACLE SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
//	public static final int ORACLE_TYPES_TIMESTAMPNS         = -100;   // return "oracle.jdbc.OracleTypes.TIMESTAMPNS";
//	public static final int ORACLE_TYPES_TIMESTAMPTZ         = -101;   // return "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
//	public static final int ORACLE_TYPES_TIMESTAMPLTZ        = -102;   // return "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
//	public static final int ORACLE_TYPES_INTERVALYM          = -103;   // return "oracle.jdbc.OracleTypes.INTERVALYM";
//	public static final int ORACLE_TYPES_INTERVALDS          = -104;   // return "oracle.jdbc.OracleTypes.INTERVALDS";
//	public static final int ORACLE_TYPES_CURSOR              =  -10;   // return "oracle.jdbc.OracleTypes.CURSOR";
//	public static final int ORACLE_TYPES_BFILE               =  -13;   // return "oracle.jdbc.OracleTypes.BFILE";
//	public static final int ORACLE_TYPES_OPAQUE              = 2007;   // return "oracle.jdbc.OracleTypes.OPAQUE";
//	public static final int ORACLE_TYPES_JAVA_STRUCT         = 2008;   // return "oracle.jdbc.OracleTypes.JAVA_STRUCT";
//	public static final int ORACLE_TYPES_PLSQL_INDEX_TABLE   =  -14;   // return "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
//	public static final int ORACLE_TYPES_BINARY_FLOAT        =  100;   // return "oracle.jdbc.OracleTypes.BINARY_FLOAT";
//	public static final int ORACLE_TYPES_BINARY_DOUBLE       =  101;   // return "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
////	public static final int ORACLE_TYPES_NUMBER              =    2;   // return "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
////	public static final int ORACLE_TYPES_RAW                 =   -2;   // return "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
//	public static final int ORACLE_TYPES_FIXED_CHAR          =  999;   // return "oracle.jdbc.OracleTypes.FIXED_CHAR";
//
//	//------------------------- Microsoft SQL-Server - SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
//    public static final int MSSQLSERVER_TYPES_DATETIMEOFFSET = -155;  // return "microsoft.sql.DATETIMEOFFSET";
//    public static final int MSSQLSERVER_TYPES_STRUCTURED     = -153;  // return "microsoft.sql.STRUCTURED";
//    public static final int MSSQLSERVER_TYPES_DATETIME       = -151;  // return "microsoft.sql.DATETIME";
//    public static final int MSSQLSERVER_TYPES_SMALLDATETIME  = -150;  // return "microsoft.sql.SMALLDATETIME";
//    public static final int MSSQLSERVER_TYPES_MONEY          = -148;  // return "microsoft.sql.MONEY";
//    public static final int MSSQLSERVER_TYPES_SMALLMONEY     = -146;  // return "microsoft.sql.SMALLMONEY";
//    public static final int MSSQLSERVER_TYPES_GUID           = -145;  // return "microsoft.sql.GUID";
//    public static final int MSSQLSERVER_TYPES_SQL_VARIANT    = -156;  // return "microsoft.sql.SQL_VARIANT";
//    public static final int MSSQLSERVER_TYPES_GEOMETRY       = -157;  // return "microsoft.sql.GEOMETRY";
//    public static final int MSSQLSERVER_TYPES_GEOGRAPHY      = -158;  // return "microsoft.sql.GEOGRAPHY";
	
	// Prevent instantiation
	private ExtendedTypes() {}
}
