/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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


import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.ResultSetMetaDataCached;
import com.dbxtune.sql.ResultSetMetaDataCached.Entry;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.ddl.model.ForeignKey;
import com.dbxtune.sql.ddl.model.Index;
import com.dbxtune.sql.ddl.model.IndexColumn;
import com.dbxtune.sql.ddl.model.Schema;
import com.dbxtune.sql.ddl.model.Table;
import com.dbxtune.sql.ddl.model.TableColumn;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public abstract class DbmsDdlResolverAbstract 
implements IDbmsDataTypeResolver, IDbmsDdlResolver
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private DbxConnection _conn;
	
	
	public DbmsDdlResolverAbstract(DbxConnection conn)
	{
		_conn = conn;
	}

	protected DbxConnection getConnection()
	{
		return _conn;
	}
	
	@Override
	public ResultSetMetaDataCached createNormalizedRsmd(ResultSet originRs)
	throws SQLException
	{
		return ResultSetMetaDataCached.createNormalizedRsmd(originRs);
	}

	@Override
	public ResultSetMetaDataCached createNormalizedRsmd(ResultSetMetaDataCached originRsmd)
	{
		return ResultSetMetaDataCached.createNormalizedRsmd(originRsmd);
	}

	/**
	 * Transforms a Normalized ResultSet into a TRANSLATED ResultSetMetaDataCached for this DbxConnection DBMS type
	 * 
	 * @param sourceRs
	 * @return
	 * @throws SQLException
	 */
	@Override
	public ResultSetMetaDataCached transformToTargetDbms(ResultSet originRs) throws SQLException
	{
		ResultSetMetaDataCached normalizedRsmdc  = ResultSetMetaDataCached.createNormalizedRsmd(originRs);
		ResultSetMetaDataCached transformedRsmdc = transformToTargetDbms(normalizedRsmdc);
		
		return transformedRsmdc;
	}
	/**
	 * Transforms a Source ResultSetMetaDataCached into a TRANSLATED ResultSetMetaDataCached for this DbxConnection DBMS type
	 * 
	 * @param normalizedRsmdc
	 * @return
	 * @throws SQLException
	 */
	@Override
	public ResultSetMetaDataCached transformToTargetDbms(ResultSetMetaDataCached normalizedRsmdc)
	{
		ResultSetMetaDataCached targetRsmdc = ResultSetMetaDataCached.transformToTargetDbms(normalizedRsmdc);
		
		return targetRsmdc;
	}

	@Override
	public String dataTypeResolverToTarget(TableColumn column)
	{
//		int jdbcSqlType = column.getColumnDataType().getJavaSqlType().getVendorTypeNumber();
//		int length      = column.getSize();
//		int scale       = column.getDecimalDigits();
		int jdbcSqlType = column.getColumnJdbcType();
		int length      = column.getColumnLength();
		int scale       = column.getColumnScale();
		
		// use: dataTypeResolver(int javaSqlType, int len, int scale)
		String resType  = dataTypeResolverToTarget(jdbcSqlType, length, scale);

		// Check that we have a type to return
		if (resType == null)
		{
//			throw new DataTypeNotResolvedException(jdbcSqlType, length, scale, "Data Type wasn't resolved. jdbcSqlType=" + jdbcSqlType + ", jdbcSqlTypeStr='" + DataTypeNotResolvedException.getJdbcTypeAsString(jdbcSqlType) + "', length=" + length + ", scale=" + scale + ".");
//			resType = column.getColumnDataType().getDatabaseSpecificTypeName();
			resType = column.getColumnDbmsDataType();
			_logger.warn("Data Type wasn't resolved (returning origin data type '" + resType + "'). jdbcSqlType=" + jdbcSqlType + ", jdbcSqlTypeStr='" + DataTypeNotResolvedException.getJdbcTypeAsString(jdbcSqlType) + "', length=" + length + ", scale=" + scale + ".");
		}

		return resType;
	}

//	/**
//	 * Checks for "strange" data types that is needed to be resolved before we do any real action/decision
//	 * <p>
//	 * This would be a typical place to handle Oracle data types of numeric(36.,0), which really is a INTEGER
//	 *  
//	 * @param javaSqlType
//	 * @param length
//	 * @param scale
//	 * @return
//	 */
//	public int resolvePreCheck(int javaSqlType, int length, int scale)
//	{
//		//------------------------------------------------------------------------
//		// SOURCE: Oracle
//		//------------------------------------------------------------------------
////		if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_ORACLE))
////		{
//			// We need to remap some of those strange numeric
//			//  - Like in Oracle 'NUMBER(0,-127)' is mapped to INTEGER  ??????
//			//  - Like in Oracle 'NUMBER(38,0)'   is mapped to INTEGER  ??????
//			//  - but at the same time various pages on Internet says to map BIGINT to number(38,0)
//			//    and number(38,0) can't fit into a 32-byte-integer, so lets assume BIGINT which is a 64-bit-integer (which at least can hold 19 digits: 64-bit-int-MAX: 9_223_372_036_854_775_807
//			//
//			// some of the below mapping could be found at: https://docs.oracle.com/cd/B10501_01/server.920/a96544/apb.htm
//			//
//			if (javaSqlType == Types.NUMERIC)
//			{
//				if ( length ==  0 && scale == -127 ) return Types.INTEGER;
//				if ( length ==  0 && scale ==    0 ) return Types.INTEGER;
//				if ( length == 10 && scale ==    0 ) return Types.INTEGER;
//				if ( length == 19 && scale ==    0 ) return Types.BIGINT;
//				if ( length == 38 && scale ==    0 ) return Types.BIGINT;
//			}
////		}
//
//		return javaSqlType;
//	}

	
	/**
	 * Get DBMS Specific database types
	 * <p>
	 * A good start can be: http://users.atw.hu/sqlnut/sqlnut2-chp-2-sect-3.html
	 * 
	 * @param javaSqlType
	 * @param length
	 * @param scale
	 * @return
	 */
	public abstract String dbmsVendorDataTypeResolverForTarget(int javaSqlType, int length, int scale);

	/**
	 * Normalize DBMS Specific database types, for example, like:
	 * <ul>
	 *   <li>Oracles various Numbers into java.sql.Types.INTEGER</li>
	 *   <li>Bump up 'unsigned' integer into java.sql.Types.BIGINT</li>
	 * </ul>
	 * <p>
	 * 
	 * @param entry   NOTE: The entry members can be changed
	 */
	public abstract void dbmsVendorDataTypeResolverForSource(Entry entry);
	
	/**
	 * If you have user defined configured data type mappings, then this will return the <b>prefix</b> for searching in properties. 
	 */
	public abstract String getPropertiesPrefix();

	
	/**
	 * 
	 * @param entry
	 * @return
	 */
	public boolean getUserDefinedDataTypeForSource(Entry entry)
	{
		return false;
//		String prefix = getPropertiesPrefix();
//		if (StringUtil.isNullOrBlank(prefix))
//			return null;
//
//		// Get value from Configuration, example: dbms.ddl.resolver.datatype.sybase.ase.java.sql.Types.NUMERIC = numeric(${length}, ${scale})
//		String jdbcSqlTypeStr = DataTypeNotResolvedException.getJdbcTypeAsString(javaSqlType);
//		String val = Configuration.getCombinedConfiguration().getPropertyRaw("dbms.ddl.resolver.datatype." + prefix + "." + jdbcSqlTypeStr, null);
//
//		// Replace "${length}" or "${scale}" in template if they exists.  
//		if (StringUtil.hasValue(val))
//		{
//			if (val.indexOf("${length}") != -1) val = val.replace("${length}", Integer.toString(length));
//			if (val.indexOf("${scale}" ) != -1) val = val.replace("${scale}" , Integer.toString(length));
//		}
//		
//		return val;
	}

	
	/**
	 * 
	 * @param javaSqlType
	 * @param length
	 * @param scale
	 * @return
	 */
	public String getUserDefinedDataTypeForTarget(int javaSqlType, int length, int scale)
	{
		String prefix = getPropertiesPrefix();
		if (StringUtil.isNullOrBlank(prefix))
			return null;

		// Get value from Configuration, example: dbms.ddl.resolver.datatype.sybase.ase.java.sql.Types.NUMERIC = numeric(${length}, ${scale})
		String jdbcSqlTypeStr = DataTypeNotResolvedException.getJdbcTypeAsString(javaSqlType);
		String val = Configuration.getCombinedConfiguration().getPropertyRaw("dbms.ddl.resolver.datatype." + prefix + "." + jdbcSqlTypeStr, null);

		// Replace "${length}" or "${scale}" in template if they exists.  
		if (StringUtil.hasValue(val))
		{
			if (val.indexOf("${length}") != -1) val = val.replace("${length}", Integer.toString(length));
			if (val.indexOf("${scale}" ) != -1) val = val.replace("${scale}" , Integer.toString(length));
		}
		
		return val;
	}

	/**
	 * Based on the JDBC "java.sql.Types" get a DBMS specific data type String.
	 * <p>
	 * NOTE 1: the method <code>getUserDefinedDataType()</code> picks up any User Specified data type definition (which is available in the CombinedConfiguration object) <br>  
	 * NOTE 2: the method <code>dbmsVendorDataTypeResolverForTarget()</code> in any DBMS subclass will dictate what to use <br>  
	 */
	@Override
	public String dataTypeResolverToTarget(int jdbcSqlType, int length, int scale)
	{
//		// do some pre-processing, which might be to: translate some strange behavior, like Oracle has for INTEGER etc...
//		javaSqlType = resolvePreCheck(javaSqlType, length, scale);

		// check/get User Specified data types
		String userDatatype = getUserDefinedDataTypeForTarget(jdbcSqlType, length, scale);
		if (userDatatype != null)
			return userDatatype;
		
		// Get DBMS Specific database types
		String datatype = dbmsVendorDataTypeResolverForTarget(jdbcSqlType, length, scale);

		// Check that we have a type to return
		if (datatype == null)
		{
			_logger.warn("Data Type wasn't resolved (throwing DataTypeNotResolvedException). jdbcSqlType=" + jdbcSqlType + ", jdbcSqlTypeStr='" + DataTypeNotResolvedException.getJdbcTypeAsString(jdbcSqlType) + "', length=" + length + ", scale=" + scale + ".");
			throw new DataTypeNotResolvedException(jdbcSqlType, length, scale, "Data Type wasn't resolved. jdbcSqlType=" + jdbcSqlType + ", jdbcSqlTypeStr='" + DataTypeNotResolvedException.getJdbcTypeAsString(jdbcSqlType) + "', length=" + length + ", scale=" + scale + ".");
		}

		// done
		return datatype;
	}

	/**
	 * 
	 */
	@Override
	public String dataTypeResolverToTarget(Entry entry)
	{
		int javaSqlType = entry.getColumnType();
//		int length      = entry.getPrecision();
//		int length      = Math.max(entry.getColumnDisplaySize(), entry.getPrecision());   // or should it be: entry.getPrecision() > 0 ? entry.getPrecision() : entry.getColumnDisplaySize();
//		int length      = entry.getPrecision() > 0 ? entry.getPrecision() : entry.getColumnDisplaySize();
		int length      = entry.getPrecision();
		int scale       = entry.getScale();

		if (length <= 0)
		{
			int newLength = entry.getColumnDisplaySize();

			if (DbmsDdlResolverAbstract.shouldHaveNonZeroPrecisionForDataType(javaSqlType))
			{
				String msg = "dataTypeResolverToTarget(): Inproper Column Precision for: TableName='" + entry.getTableName() + "', ColumnName='" + entry.getColumnLabel() + "' has a Precision of " + length + ", and a Scale of " + scale + ". The PCS will use a Presision of " + newLength + " instead.";
				_logger.info(msg, new RuntimeException(msg));
			}

			length = newLength;
		}
		
		String targetDataType = dataTypeResolverToTarget(javaSqlType, length, scale);
		
		entry.setColumnResolvedTypeName(targetDataType);
		
		return targetDataType;
	}
	
	/**
	 * 
	 */
	@Override
	public void dataTypeResolverForSource(Entry entry)
	{
		// check/get User Specified data types
		if (getUserDefinedDataTypeForSource(entry))
			return;
		
		// normalize DBMS Specific database types using any of the subclass implementation
		dbmsVendorDataTypeResolverForSource(entry);
		
		// Check for "strange" things
		checkSourceDataTypes(entry);
	}
	
	/**
	 * Check for "strange" things and warn
	 * @param entry
	 */
	private void checkSourceDataTypes(Entry entry)
	{
		String msg = null;

		if (entry.getPrecision() == 0)
		{
			switch (entry.getColumnType())
			{

			// ----------------------------------
			case Types.CHAR:
			case Types.VARCHAR:
				msg = "DbmsDdlResolverAbstract.dataTypeResolverForSource().checkSourceDataTypes(): TabName='" + entry.getTableName() + "', Column='" + entry.getColumnLabel() + "', jdbcType='" + DataTypeNotResolvedException.getJdbcTypeAsString(entry.getColumnType())+ "'. has getPrecision()=" + entry.getPrecision() + ". This seems to be unlikely, please cast the data in SQL to use a specific length.";
				_logger.warn(msg, new RuntimeException(msg));
				break;

				// ----------------------------------
			case Types.NCHAR:
			case Types.NVARCHAR:
//			case Types.LONGNVARCHAR:
				msg = "DbmsDdlResolverAbstract.dataTypeResolverForSource().checkSourceDataTypes(): TabName='" + entry.getTableName() + "',Column='" + entry.getColumnLabel() + "', jdbcType='" + DataTypeNotResolvedException.getJdbcTypeAsString(entry.getColumnType())+ "'. has getPrecision()=" + entry.getPrecision() + ". This seems to be unlikely, please cast the data in SQL to use a specific length.";
				_logger.warn(msg, new RuntimeException(msg));
				break;

			// ----------------------------------
			case Types.BINARY:
			case Types.VARBINARY:
//			case Types.LONGVARBINARY:
				msg = "DbmsDdlResolverAbstract.dataTypeResolverForSource().checkSourceDataTypes(): TabName='" + entry.getTableName() + "',Column='" + entry.getColumnLabel() + "', jdbcType='" + DataTypeNotResolvedException.getJdbcTypeAsString(entry.getColumnType())+ "'. has getPrecision()=" + entry.getPrecision() + ". This seems to be unlikely, please cast the data in SQL to use a specific length.";
				_logger.warn(msg, new RuntimeException(msg));
				break;

			// ----------------------------------
			case Types.DECIMAL:
			case Types.NUMERIC:
				msg = "DbmsDdlResolverAbstract.dataTypeResolverForSource().checkSourceDataTypes(): TabName='" + entry.getTableName() + "',Column='" + entry.getColumnLabel() + "', jdbcType='" + DataTypeNotResolvedException.getJdbcTypeAsString(entry.getColumnType())+ "'. has getPrecision()=" + entry.getPrecision() + ", getScale()=" + entry.getScale() + ". This seems to be unlikely, please cast the data in SQL to use a specific length.";
				_logger.warn(msg, new RuntimeException(msg));
				break;
			}
		}
	}

	/**
	 * Used by PersistWriterBase and "this" before writing INFO messages about missing precisions 
	 * @param jdbcType   The integer value of: java.sql.Types
	 */
	public static boolean shouldHaveNonZeroPrecisionForDataType(int jdbcType)
	{
		switch (jdbcType)
		{

		// ----------------------------------
		case Types.CHAR:
		case Types.VARCHAR:
			return true;

		// ----------------------------------
		case Types.NCHAR:
		case Types.NVARCHAR:
//		case Types.LONGNVARCHAR:
			return true;

		// ----------------------------------
		case Types.BINARY:
		case Types.VARBINARY:
//		case Types.LONGVARBINARY:
			return true;

		// ----------------------------------
		case Types.DECIMAL:
		case Types.NUMERIC:
			return true;
		}
		
		return false;
	}

	
//	@Override
//	public void dataTypeResolverSource(Entry entry)
//	{
//		//------------------------------------------------------------------------
//		// SOURCE: Sybase ASE
//		//------------------------------------------------------------------------
//		if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_SYBASE_ASE))
//		{
//			if ("unsigned smallint".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.INTEGER;
//				_columnTypeName[index] = "int";
//			}
//			
//			if ("unsigned int".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
//			
////			if ("unsigned bigint".equalsIgnoreCase(_columnTypeName[index]))
////			{
////				_columnType    [index] = Types.BIGINT;
////				_columnTypeName[index] = "bigint";
////			}
//		}
//		//------------------------------------------------------------------------
//		// SOURCE: Microsoft SQL-Server
//		//------------------------------------------------------------------------
//		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_MSSQL))
//		{
//			if ("uniqueidentifier".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.CHAR;
//				_columnTypeName[index] = "char";
//			}
//
//			if ("xml".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.LONGVARCHAR;
//				_columnTypeName[index] = "text";
//			}
//		}
//		//------------------------------------------------------------------------
//		// SOURCE: Oracle
//		//------------------------------------------------------------------------
//		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_ORACLE))
//		{
//			if (_columnType[index] == Types.NUMERIC)
//			{
//				int length = _precision[index];
//				int scale  = _scale    [index];
//				
//				if      ( length ==  0 && scale == -127 ) { _columnType[index] =  Types.INTEGER; _columnTypeName[index] = "int";    System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> INTEGER");}
//				else if ( length ==  0 && scale ==    0 ) { _columnType[index] =  Types.INTEGER; _columnTypeName[index] = "int";    System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> INTEGER");}
//				else if ( length == 10 && scale ==    0 ) { _columnType[index] =  Types.INTEGER; _columnTypeName[index] = "int";    System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> INTEGER");}
//				else if ( length == 19 && scale ==    0 ) { _columnType[index] =  Types.BIGINT;  _columnTypeName[index] = "bigint"; System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> BIGINT");}
//				else if ( length == 38 && scale ==    0 ) { _columnType[index] =  Types.BIGINT;  _columnTypeName[index] = "bigint"; System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> BIGINT");}
//			}
//
////			// We need to remap some of those strange numeric -127 or whatever they are...
////			if (_columnType[index] == Types.NUMERIC && (_scale[index] == 0 || _scale[index] == -127) && (_precision[index] <= 9 || _precision[index] == 38))
////			{
////System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+_precision[index]+", _scale="+_scale[index]+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> Types.BIGINT");
//////				_columnType    [index] = Types.INTEGER;
//////				_columnTypeName[index] = "int";
////				_columnType    [index] = Types.BIGINT;
////				_columnTypeName[index] = "bigint";
////			}
//		}
//		//------------------------------------------------------------------------
//		// SOURCE: Postgres (which has olot of stange datatypes... it might be better to look at "JDBC datatypes when creating destination tables")
//		//------------------------------------------------------------------------
//		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_POSTGRES))
//		{
//			String dt = _columnTypeName[index];
//
//			if ("oid".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
//
//			else if ("int2".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.SMALLINT;
//				_columnTypeName[index] = "smallint";
//			}
//
//			else if ("int4".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.INTEGER;
//				_columnTypeName[index] = "int";
//			}
//
//			else if ("int8".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
//
//			else if ("name".equalsIgnoreCase(dt) && _columnDisplaySize[index] == 2147483647)
//			{
//				_columnType    [index] = Types.VARCHAR;
//				_columnTypeName[index] = "varchar";
//				_columnDisplaySize[index] = 256;
//			}
//
//			else if ("float8".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.DOUBLE;
//				_columnTypeName[index] = "double";
//			}
//
//			else if ("float4".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.REAL;
//				_columnTypeName[index] = "real";
//			}
//
//			else if ("timestamptz".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.TIMESTAMP;
//				_columnTypeName[index] = "timestamp";
//			}
//
//			else if ("xid".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.VARCHAR;
//				_columnTypeName[index] = "varchar";
//				_columnDisplaySize[index] = 30;
//			}
//
//			else if ("inet".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.VARCHAR;
//				_columnTypeName[index] = "varchar";
//				_columnDisplaySize[index] = 30;
//			}
//
//			else if ("bool".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.BOOLEAN;
//				_columnTypeName[index] = "bit";
//			}
//
//			else if ("xml".equalsIgnoreCase(dt)) // not sure about this one
//			{
//				_columnType    [index] = Types.LONGVARCHAR;
//				_columnTypeName[index] = "text";
//			}
//		}
//	}



//	/**
//	 * TODO 1: get a "remap" instance from "somewhere" which should be responsible for this based on the DBMS instance  
//	 * TODO 2: The PCS Writer should also have some kind of remap functionality if we want to store it as something different... but have a look at this later...  
//	 * @param index
//	 */
//	protected void remap(int index)
//	{
////		if (_dataTypeResolver == null)
////			return;
//
//		// If we do not have a PCS do not bather to transform data types
////		if ( ! PersistentCounterHandler.hasInstance() )
////			return;
//
//		FIXME: Maybe use DbmsDdlResolver ... and have a resolvePreCheck() -->> sourceDataTypeResolver()
//
//		//------------------------------------------------------------------------
//		// SOURCE: Sybase ASE
//		//------------------------------------------------------------------------
//		if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_SYBASE_ASE))
//		{
//			if ("unsigned smallint".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.INTEGER;
//				_columnTypeName[index] = "int";
//			}
//			
//			if ("unsigned int".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
//			
////			if ("unsigned bigint".equalsIgnoreCase(_columnTypeName[index]))
////			{
////				_columnType    [index] = Types.BIGINT;
////				_columnTypeName[index] = "bigint";
////			}
//		}
//		//------------------------------------------------------------------------
//		// SOURCE: Microsoft SQL-Server
//		//------------------------------------------------------------------------
//		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_MSSQL))
//		{
//			if ("uniqueidentifier".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.CHAR;
//				_columnTypeName[index] = "char";
//			}
//
//			if ("xml".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.LONGVARCHAR;
//				_columnTypeName[index] = "text";
//			}
//		}
//		//------------------------------------------------------------------------
//		// SOURCE: Oracle
//		//------------------------------------------------------------------------
//		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_ORACLE))
//		{
//			if (_columnType[index] == Types.NUMERIC)
//			{
//				int length = _precision[index];
//				int scale  = _scale    [index];
//				
//				if      ( length ==  0 && scale == -127 ) { _columnType[index] =  Types.INTEGER; _columnTypeName[index] = "int";    System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> INTEGER");}
//				else if ( length ==  0 && scale ==    0 ) { _columnType[index] =  Types.INTEGER; _columnTypeName[index] = "int";    System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> INTEGER");}
//				else if ( length == 10 && scale ==    0 ) { _columnType[index] =  Types.INTEGER; _columnTypeName[index] = "int";    System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> INTEGER");}
//				else if ( length == 19 && scale ==    0 ) { _columnType[index] =  Types.BIGINT;  _columnTypeName[index] = "bigint"; System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> BIGINT");}
//				else if ( length == 38 && scale ==    0 ) { _columnType[index] =  Types.BIGINT;  _columnTypeName[index] = "bigint"; System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+length+", _scale="+scale+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> BIGINT");}
//			}
//
////			// We need to remap some of those strange numeric -127 or whatever they are...
////			if (_columnType[index] == Types.NUMERIC && (_scale[index] == 0 || _scale[index] == -127) && (_precision[index] <= 9 || _precision[index] == 38))
////			{
////System.out.println("REMAPPING(ORACLE) index="+index+", columnName=["+_columnName[index]+"]: Types.NUMERIC, _precision="+_precision[index]+", _scale="+_scale[index]+", _columnTypeName="+_columnTypeName[index]+" -------to>>>>>> Types.BIGINT");
//////				_columnType    [index] = Types.INTEGER;
//////				_columnTypeName[index] = "int";
////				_columnType    [index] = Types.BIGINT;
////				_columnTypeName[index] = "bigint";
////			}
//		}
//		//------------------------------------------------------------------------
//		// SOURCE: Postgres (which has olot of stange datatypes... it might be better to look at "JDBC datatypes when creating destination tables")
//		//------------------------------------------------------------------------
//		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_POSTGRES))
//		{
//			String dt = _columnTypeName[index];
//
//			if ("oid".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
//
//			else if ("int2".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.SMALLINT;
//				_columnTypeName[index] = "smallint";
//			}
//
//			else if ("int4".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.INTEGER;
//				_columnTypeName[index] = "int";
//			}
//
//			else if ("int8".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
//
//			else if ("name".equalsIgnoreCase(dt) && _columnDisplaySize[index] == 2147483647)
//			{
//				_columnType    [index] = Types.VARCHAR;
//				_columnTypeName[index] = "varchar";
//				_columnDisplaySize[index] = 256;
//			}
//
//			else if ("float8".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.DOUBLE;
//				_columnTypeName[index] = "double";
//			}
//
//			else if ("float4".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.REAL;
//				_columnTypeName[index] = "real";
//			}
//
//			else if ("timestamptz".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.TIMESTAMP;
//				_columnTypeName[index] = "timestamp";
//			}
//
//			else if ("xid".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.VARCHAR;
//				_columnTypeName[index] = "varchar";
//				_columnDisplaySize[index] = 30;
//			}
//
//			else if ("inet".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.VARCHAR;
//				_columnTypeName[index] = "varchar";
//				_columnDisplaySize[index] = 30;
//			}
//
//			else if ("bool".equalsIgnoreCase(dt))
//			{
//				_columnType    [index] = Types.BOOLEAN;
//				_columnTypeName[index] = "bit";
//			}
//
//			else if ("xml".equalsIgnoreCase(dt)) // not sure about this one
//			{
//				_columnType    [index] = Types.LONGVARCHAR;
//				_columnTypeName[index] = "text";
//			}
//		}
//	}
	
	@Override
	public boolean supportsIfNotExists()
	{
		return false;
	}

	@Override
	public boolean skipCreateSchemaWithName(String schemaName)
	{
		if (schemaName == null)
			return true;

		// for "others" that isn't specified/overridden
		if (schemaName.equalsIgnoreCase("PUBLIC"            )) return true;
		if (schemaName.equalsIgnoreCase("INFORMATION_SCHEMA")) return true;

		// Everything else... create the schema
		return false;
	}

//	@Override
//	public String escapeQuotedIdentifier(String name)
//	{
//		return name;
//	}

	
//	 * For Sybase ASE and SQL-Server escaping square brackets inside a column name or an identifier is done using<br>
//	 * <code>[Bring-The-[AAAA]]-Game]</code> is equal to <code>Bring-The-[AAAA]-Game</code> <br>
//	 * so all ']' should be escaped as ']]'... but starting brackets '[' should NOT be touched
//	 * <p>
//	 * The passed value of name is WITHOUT the surrounding brackets, for the above example the input value is <code>Bring-The-[AAAA]-Game</code>
//	 * <p>
//	 * <b>Example:</b> <code>escapeQuotedIdentifier("Bring-The-[AAAA]-Game")</code><br>
//	 * <b>Returns:</b> <code>"Bring-The-[AAAA]]-Game"</code> 
	/**
	 * Since the generated/intermediate code always has '[' and ']' around identifiers...<br>
	 * Then if the name contains and ']' wee need to escape them to ']]'
	 * <p>
	 * Then the DbxConnection should use <code>dbxConn.quotifySqlString(sql)</code> to <b>translate</b> to the destination DBMS Quoted Identifier Char  
	 * 
	 */
	@Override
	public String escapeQuotedIdentifier(String name)
	{
		if (name.indexOf(']') != -1)
			name = name.replace("]", "]]");

		return name;
	}












	/** 
	 * SCHEMA
	 * 
	 * <pre>
	 * CREATE SCHEMA [IF NOT EXISTS] schemaName);
	 * </pre>
	 * 
	 * <p>
	 * <b>Note:</b> all identifiers is surrounded with a starting '[' and ending ']' character, which will be translated into proper DBMS Quote Chars using <code>DbxConnection.quotifySqlString(sql)</code>
	 * 
	 */
	@Override
	public String ddlText(Schema schema)
	{
		// If we should NOT create DDL for specific schemas
		if (skipCreateSchemaWithName(schema.getSchemaName()))
			return "";

		StringBuilder sb = new StringBuilder();
		
		String ifNotExists = supportsIfNotExists() ? "IF NOT EXISTS " : "" ;
		String leftQuote   = "[";
		String rightQuote  = "]";
		
		// CREATE SCHEMA [IF NOT EXISTS] schemaName
		sb.append("CREATE SCHEMA ").append(ifNotExists).append(leftQuote).append(escapeQuotedIdentifier(schema.getSchemaName())).append(rightQuote).append(" \n");
		sb.append("\n");

		return sb.toString();
	}

	/** 
	 * <pre>
	 * CREATE [UNIQUE] INDEX [IF NOT EXISTS] indexName ON schemaName.tableName(c1, c2, c3...)
	 * </pre>
	 * 
	 * <p>
	 * <b>Note:</b> all identifiers is surrounded with a starting '[' and ending ']' character, which will be translated into proper DBMS Quote Chars using <code>DbxConnection.quotifySqlString(sql)</code>
	 * 
	 */
	@Override
	public String ddlText(Index index, boolean pkAsConstraint)
	{
		return ddlText(index, pkAsConstraint, null, null);
	}
	@Override
	public String ddlText(Index index, boolean pkAsConstraint, String inSchemaName, String inTableName)
	{
		StringBuilder sb = new StringBuilder();
		
		String ifNotExists = supportsIfNotExists() ? "IF NOT EXISTS " : "" ;
		String schemaName  = StringUtil.hasValue(inSchemaName) ? inSchemaName : index.getParent().getSchemaName();
		String tableName   = StringUtil.hasValue(inTableName)  ? inTableName  : index.getParent().getTableName();
		String indexName   = index.getIndexName();
		String leftQuote   = "[";
		String rightQuote  = "]";
		String unique      = index.isUnique() ? "UNIQUE " : "";
		
		boolean hasInTable = StringUtil.hasValue(inTableName);

		sb.append("CREATE ").append(unique).append("INDEX ").append(ifNotExists); // CREATE [UNIQUE] INDEX [IF NOT EXISTS]
		sb.append(leftQuote).append(escapeQuotedIdentifier(indexName)).append(rightQuote); // indexName
		sb.append(" ON "); // ON [schema.]tabname
		if (StringUtil.hasValue(schemaName))
		{
			sb.append(leftQuote).append(escapeQuotedIdentifier(schemaName)).append(rightQuote);
			sb.append(".");
		}
		sb.append(leftQuote).append(escapeQuotedIdentifier(tableName)).append(rightQuote); 
		sb.append(" ("); // ( col1, col2, col3 )

		String comma = "";
		for (final IndexColumn ic : index.getIndexColumns())
		{
			sb.append(comma).append(leftQuote).append(escapeQuotedIdentifier(ic.getColumnName())).append(rightQuote);
			if (ic.isDescending())
				sb.append(" DESC");
			comma = ", ";
		}
		sb.append(") \n");
		// sb.append("\n"); // NOTE: This is done at the end... if we get there (not exiting due to below PK check)

		if (index.getParent().hasPk() && !hasInTable)
		{
			if (index.isPrimaryKey())
			{
				StringBuilder sb2 = new StringBuilder();
				sb2.append("-- ########################################\n");
				sb2.append("-- ## Skipping below index... it's already created using a PrimaryKey specification on the table level \n");
				sb2.append("-- ").append(sb.toString());
				sb2.append("-- ########################################\n");
				return sb2.toString();
			}
		}

		if (pkAsConstraint && index.isPrimaryKey())
		{
			sb.setLength(0);
			// "ALTER TABLE [schema.]tableName ADD CONSTRAINT contraintName PRIMARY KEY (colName1, colName2...)"
			sb.append("ALTER TABLE ");
			if (StringUtil.hasValue(schemaName))
			{
				sb.append(leftQuote).append(escapeQuotedIdentifier(schemaName)).append(rightQuote);
				sb.append(".");
			}
			sb.append(leftQuote).append(escapeQuotedIdentifier(tableName)).append(rightQuote); 
			sb.append(" ADD CONSTRAINT ").append(leftQuote).append(escapeQuotedIdentifier(indexName)).append(rightQuote); 
			sb.append(" PRIMARY KEY ("); 

			comma = "";
			for (final IndexColumn ic : index.getIndexColumns())
			{
				sb.append(comma).append(leftQuote).append(escapeQuotedIdentifier(ic.getColumnName())).append(rightQuote);
				comma = ", ";
			}
			sb.append(") \n");
		}

		
		sb.append("\n");
		return sb.toString();
	}

	/** 
	 * <pre>
	 * CREATE TABLE [IF NOT EXISTS] schemaName.tableName
	 * (
	 *      c1        dbmsDataType      [NOT] NULL    -- jdbcTypeName  {jdbcTypeNumber=#}    
	 *    , c2        dbmsDataType      [NOT] NULL    -- jdbcTypeName  {jdbcTypeNumber=#}
	 *    , c3        dbmsDataType      [NOT] NULL    -- jdbcTypeName  {jdbcTypeNumber=#}
	 *    [, PRIMARY KEY(c1, c2...)]
	 * )
	 * </pre>
	 * 
	 * <p>
	 * <b>Note:</b> all identifiers is surrounded with a starting '[' and ending ']' character, which will be translated into proper DBMS Quote Chars using <code>DbxConnection.quotifySqlString(sql)</code>
	 * 
	 */
	@Override
	public String ddlText(Table table)
	{
		return ddlText(table, null, null);
	}
	/** 
	 * <pre>
	 * CREATE TABLE [IF NOT EXISTS] schemaName.tableName
	 * (
	 *      c1        dbmsDataType      [NOT] NULL    -- jdbcTypeName  {jdbcTypeNumber=#}    
	 *    , c2        dbmsDataType      [NOT] NULL    -- jdbcTypeName  {jdbcTypeNumber=#}
	 *    , c3        dbmsDataType      [NOT] NULL    -- jdbcTypeName  {jdbcTypeNumber=#}
	 *    [, PRIMARY KEY(c1, c2...)]
	 * )
	 * </pre>
	 * 
	 * <p>
	 * <b>Note:</b> all identifiers is surrounded with a starting '[' and ending ']' character, which will be translated into proper DBMS Quote Chars using <code>DbxConnection.quotifySqlString(sql)</code>
	 * 
	 */
	@Override
	public String ddlText(Table table, String inSchemaName, String inTableName)
	{
		StringBuilder sb = new StringBuilder();
		
		String ifNotExists = supportsIfNotExists() ? "IF NOT EXISTS " : "";
		String schemaName  = StringUtil.hasValue(inSchemaName) ? inSchemaName : table.getSchemaName();
		String tableName   = StringUtil.hasValue(inTableName ) ? inTableName  : table.getTableName();
		String leftQuote   = "[";
		String rightQuote  = "]";
		
		sb.append("CREATE TABLE ").append(ifNotExists);
		if (StringUtil.hasValue(schemaName))
		{
			sb.append(leftQuote).append(escapeQuotedIdentifier(schemaName)).append(rightQuote);
			sb.append(".");
		}
		sb.append(leftQuote).append(escapeQuotedIdentifier(tableName)).append(rightQuote).append(" \n");
		sb.append("( \n");


		// Prepare for "pretty" output, get MAX column name length
		int maxColNameLen = 30;
		for (final TableColumn tc : table.getColumns())
		{
			String fullColName = leftQuote + escapeQuotedIdentifier(tc.getColumnLabel()) + rightQuote;
			maxColNameLen = Math.max(maxColNameLen, fullColName.length());
		}

		// column spec: colname datatype [not] null ...
		String comma = " ";
		for (final TableColumn tc : table.getColumns())
		{
//			System.out.println("COLUMN["+column.getOrdinalPosition()+"]: getVendorTypeNumber="+column.getType().getJavaSqlType().getVendorTypeNumber() + ", getName="+column.getType().getJavaSqlType().getName()+", getVendor="+column.getType().getJavaSqlType().getVendor()+", JavaSqlTypeGroup="+column.getType().getJavaSqlType().getJavaSqlTypeGroup());
//			System.out.println("   " + comma + "[" + column.getName() + "]     " + column.getColumnDataType().getDatabaseSpecificTypeName() + (column.isNullable() ? "    NULL" : " NOT NULL") );

			String colDataType = dataTypeResolverToTarget(tc);
			sb.append( 
					String.format("   %s %-"+maxColNameLen+"s %-30s %s     -- %-25s {jdbcTypeNumber=%d}\n", 
							comma,
							leftQuote + escapeQuotedIdentifier(tc.getColumnLabel()) + rightQuote,
							colDataType, // column.getColumnDataType().getDatabaseSpecificTypeName(),
							tc.isColumnNullable() ? "    NULL" : "NOT NULL",
							tc.getColumnJdbcTypeStr(),
							tc.getColumnJdbcType()
					));
			comma = ",";
		}
		// primary key(...)
		if (table.hasPk())
		{
			sb.append("   , PRIMARY KEY(");

			comma = "";
			for (final String columnName : table.getPkColumns())
			{
				sb.append(comma).append(leftQuote).append(escapeQuotedIdentifier(columnName)).append(rightQuote);
				comma = ", ";
			}
			sb.append(") \n");
		}
		
		sb.append(") \n");
		sb.append("\n");

		return sb.toString();
	}


	/**
	 * Build alter table for foreign keys
	 * <pre>
	 *    ALTER TABLE [schema.]tableName                      
	 *    ADD CONSTRAINT fkName                               
	 *    FOREIGN KEY (colName1, colName2...)                 
	 *    REFERENCES [schema.]tableName(colName1, colName2...)
	 * </pre>
	 */
	@Override
	public String ddlTextAlterTable(ForeignKey fk)
	{
		return ddlTextAlterTable(fk, null, null);
	}
	/**
	 * Build alter table for foreign keys
	 * <pre>
	 *    ALTER TABLE [schema.]tableName                      
	 *    ADD CONSTRAINT fkName                               
	 *    FOREIGN KEY (colName1, colName2...)                 
	 *    REFERENCES [schema.]tableName(colName1, colName2...)
	 * </pre>
	 */
	@Override
	public String ddlTextAlterTable(ForeignKey fk, String inSchemaName, String inTableName)
	{
		String schemaName  = StringUtil.hasValue(inSchemaName) ? inSchemaName : fk.getSchemaName() ;
		String tableName   = StringUtil.hasValue(inTableName ) ? inTableName  : fk.getTableName()  ;
		String fkName      = fk.getForeignKeyName();

		String fkSchemaName  = fk.getDestSchemaName();
		String fkTableName   = fk.getDestTableName();

		String leftQuote   = "[";
		String rightQuote  = "]";

		String comma = "";
		
		StringBuilder sb = new StringBuilder();
		
		// ALTER TABLE [schema.]tableName
		// ADD CONSTRAINT fkName
		// FOREIGN KEY (colName1, colName2...) 
		// REFERENCES [schema.]tableName(colName1, colName2...)


		// ---- ALTER TABLE [schema.]tableName
		sb.append("ALTER TABLE ");
		if (StringUtil.hasValue(schemaName))
		{
			sb.append(leftQuote).append(escapeQuotedIdentifier(schemaName)).append(rightQuote);
			sb.append(".");
		}
		sb.append(leftQuote).append(escapeQuotedIdentifier(tableName)).append(rightQuote); 
		
		// ---- ADD CONSTRAINT fkName
		// ---- FOREIGN KEY (colName1, colName2...) 
		sb.append(" ADD CONSTRAINT ").append(leftQuote).append(escapeQuotedIdentifier(fkName)).append(rightQuote); 
		sb.append(" FOREIGN KEY (");
		comma = "";
		for (final String colName : fk.getColumnNames())
		{
			sb.append(comma).append(leftQuote).append(escapeQuotedIdentifier(colName)).append(rightQuote);
			comma = ", ";
		}
		sb.append(") ");
		
		// ---- REFERENCES [schema.]tableName(colName1, colName2...)
		sb.append(" REFERENCES ");
		if (StringUtil.hasValue(fkSchemaName))
		{
			sb.append(leftQuote).append(escapeQuotedIdentifier(fkSchemaName)).append(rightQuote);
			sb.append(".");
		}
		sb.append(leftQuote).append(escapeQuotedIdentifier(fkTableName)).append(rightQuote); 
		sb.append(" (");
		comma = "";
		for (final String colName : fk.getDestColumnNames())
		{
			sb.append(comma).append(leftQuote).append(escapeQuotedIdentifier(colName)).append(rightQuote);
			comma = ", ";
		}
		sb.append(") ");

		sb.append(" -- ");
		sb.append(" ON UPDATE ").append(fk.getUpdateRuleText());
		sb.append(" ON DELETE ").append(fk.getDeleteRuleText());
		
		sb.append(" \n");

		return sb.toString();
	}

	@Override
	public String ddlTextTable(ResultSetMetaDataCached rsmdc)
	{
		return ddlTextTable(rsmdc, null, null);
	}
	@Override
	public String ddlTextTable(ResultSetMetaDataCached rsmdc, String inSchemaName, String inTableName)
	{
		if (StringUtil.isNullOrBlank(inSchemaName))
		{
			// Maybe we should get a SET of schemas, and use first/all
			inSchemaName = rsmdc.getSchemaName(1);
		}
		if (StringUtil.isNullOrBlank(inTableName))
		{
			// Maybe we should get a SET of tables, and use first/all
			inTableName = rsmdc.getTableName(1);
		}
		
		StringBuilder sb = new StringBuilder();
		
		String ifNotExists = supportsIfNotExists() ? "IF NOT EXISTS " : "";
		String schemaName  = inSchemaName;
		String tableName   = inTableName;
		String leftQuote   = "[";
		String rightQuote  = "]";
		
		sb.append("CREATE TABLE ").append(ifNotExists);
		if (StringUtil.hasValue(schemaName))
		{
			sb.append(leftQuote).append(escapeQuotedIdentifier(schemaName)).append(rightQuote);
			sb.append(".");
		}
		sb.append(leftQuote).append(escapeQuotedIdentifier(tableName)).append(rightQuote).append(" \n");
		sb.append("( \n");


		// Prepare for "pretty" output, get MAX column name length
		int maxColNameLen = 30;
		for (String colName : rsmdc.getColumnNames() )
		{
			String fullColName = leftQuote + escapeQuotedIdentifier(colName) + rightQuote;
			maxColNameLen = Math.max(maxColNameLen, fullColName.length());
			
		}

		// column spec: colname datatype [not] null ...
		String comma = " ";
		for (Entry entry : rsmdc.getEntries())
		{
			String colDataType = dataTypeResolverToTarget(entry);
			sb.append( 
					String.format("   %s %-"+maxColNameLen+"s %-30s %s     -- %-25s {jdbcTypeNumber=%d}\n", 
							comma,
							leftQuote + escapeQuotedIdentifier(entry.getColumnLabel()) + rightQuote,
							colDataType, // column.getColumnDataType().getDatabaseSpecificTypeName(),
							entry.isNullable() ? "    NULL" : "NOT NULL",
							entry.getColumnJdbcTypeStr(),
							entry.getColumnJdbcType()
					));
			comma = ",";
		}
		
		sb.append(") \n");
		sb.append("\n");

		return sb.toString();
	}
}
