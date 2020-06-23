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

import java.sql.Types;

import com.asetune.sql.ResultSetMetaDataCached.Entry;
import com.asetune.sql.conn.DbxConnection;

public abstract class DbmsDdlResolverTds 
extends DbmsDdlResolverAbstract
{

	public DbmsDdlResolverTds(DbxConnection conn)
	{
		super(conn);
	}

	@Override
	public boolean supportsIfNotExists()
	{
		return false;
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

		// Everything else... create the schema
		return false;
	}


	@Override
	public void dbmsVendorDataTypeResolverForSource(Entry entry)
	{
		// Should we FIX the size of precision if it's ZERO for some data types ???
		if (entry.getPrecision() == 0)
		{
			// Some vendors 'Sybase', for instance has 'precision=0', while the length is in 'columnDisplaySize' ... for CHAR/VARCHAR
			// Sybase fucks it up even greater
			// -- CHAR/VARCHAR     is OK in Types... but Precision is ZERO and "length" is really in columnDisplaySize
			// -- NCHAR/NVARCHAR   is returned as Types.LONGVARCHAR, with a 'columnDisplaySize'*3    so nchar(99)     is 297
			// -- BINARY/VARBINARY is OK in Types... but 'columnDisplaySize'*2                       so varbinary(99) is 198
			
			switch (entry.getColumnType())
			{

			// Set Precision using ColumnDisplaySize
			case Types.CHAR:
			case Types.VARCHAR:
//			case Types.LONGVARCHAR:
				entry.setPrecision( entry.getColumnDisplaySize() );
				break;

			// Set Precision using ColumnDisplaySize / 3   (display size seems to be 3 for every "character")
			case Types.NCHAR:
			case Types.NVARCHAR:
//			case Types.LONGNVARCHAR:
				entry.setPrecision( entry.getColumnDisplaySize() / 3 );
				break;

			// Set Precision using ColumnDisplaySize / 2  (display size is obviously 2 for every BYTE value)
			case Types.BINARY:
			case Types.VARBINARY:
//			case Types.LONGVARBINARY:
				entry.setPrecision( entry.getColumnDisplaySize() / 2 );
				break;
			}
		}

		// Sybase UNICHAR is really like a nchar  
		if ("unichar".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NCHAR);
			entry.setColumnTypeName("nchar");
		}

		// Sybase UNICHAR is really like a nchar  
		if ("univarchar".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NVARCHAR);
			entry.setColumnTypeName("nvarchar");
		}

		// Sybase NCHAR is MetaData: char and Types.LONGVARCHAR
		if (entry.getColumnType() == Types.LONGVARCHAR && "char".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NCHAR);
			entry.setColumnTypeName("nchar");
			entry.setPrecision( entry.getColumnDisplaySize() / 3 );
		}

		// Sybase NVARCHAR is MetaData: varchar and Types.LONGVARCHAR
		if (entry.getColumnType() == Types.LONGVARCHAR && "varchar".equalsIgnoreCase(entry.getColumnTypeName()))
		{
			entry.setColumnType(Types.NVARCHAR);
			entry.setColumnTypeName("nvarchar");
			entry.setPrecision( entry.getColumnDisplaySize() / 3 );
		}

		if ("unsigned smallint".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.SMALLINT && !entry.isSigned()) )
		{
			entry.setColumnType(Types.INTEGER);
			entry.setColumnTypeName("int");
			entry.setSigned(true);
		}
		
		if ("unsigned int".equalsIgnoreCase(entry.getColumnTypeName()) || (entry.getColumnType() == Types.INTEGER && !entry.isSigned()) )
		{
			entry.setColumnType(Types.BIGINT);
			entry.setColumnTypeName("bigint");
			entry.setSigned(true);
		}
	}


	/**
	 * Resolve JDBC Types -->> Sybase: ASE, ASA, RAX, RepServer, DA, DRA 
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
		case java.sql.Types.VARCHAR:                 return "varchar("+length+")";
		case java.sql.Types.LONGVARCHAR:             return "text";
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
		case java.sql.Types.CLOB:                    return "text";
		case java.sql.Types.REF:                     return "image";                      // Not really supported just use 'image'
		case java.sql.Types.DATALINK:                return "image";                      // Not really supported just use 'image'
		case java.sql.Types.BOOLEAN:                 return "bit";

		//------------------------- JDBC 4.0 (java 1.6) -----------------------------------
		case java.sql.Types.ROWID:                   return "varchar(20)";                 // Just guessing here... from https://docs.oracle.com/cd/B28359_01/server.111/b28318/datatype.htm#CNCPT1846
		case java.sql.Types.NCHAR:                   return "unichar("+length+")";
		case java.sql.Types.NVARCHAR:                return "univarchar("+length+")";
		case java.sql.Types.LONGNVARCHAR:            return "unitext";
		case java.sql.Types.NCLOB:                   return "unitext";
		case java.sql.Types.SQLXML:                  return "unitext";                     // NOTE: if Charset UTF-8 is used then 'text' otherwise use 'unitext'

		//------------------------- JDBC 4.2 (java 1.8) -----------------------------------
		case java.sql.Types.REF_CURSOR:              return null;
		case java.sql.Types.TIME_WITH_TIMEZONE:      return "varchar(40)";                 // Not really supported, should we use varchar(40) instead ??? -->>             hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 40 chars 
		case java.sql.Types.TIMESTAMP_WITH_TIMEZONE: return "varchar(50)";                 // Not really supported, should we use varchar(50) instead ??? -->>  YYYY-MM-DD hh:mm:ss.123456789 {+##|Europe/Stockholm} // approx 50 chars

		//------------------------- DBXTUNE SPECIFIC TYPES -- Not located anywhere else ---------------------------
		case ExtendedTypes.DBXTUNE_TYPE_UUID:        return "varchar(36)";
		case ExtendedTypes.DBXTUNE_TYPE_JSON:        return "text";

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
