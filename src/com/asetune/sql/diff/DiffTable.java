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
package com.asetune.sql.diff;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.diff.DiffContext.DiffSide;
import com.asetune.ui.autocomplete.completions.TableInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class DiffTable
{
	private DiffSide _diffSide;
	private DiffContext _context;
	private ResultSet _rs;
	private DbxConnection _lookupConn; // Connection used to get PK Columns etc (not to execute real SQL on)
	private ConnectionProp _connProps; 
	private String _dbmsVendor; 
//	private ResultSetMetaDataCached _rsmd;
	private ResultSetMetaData _rsmd;
	private long      _rowCount;  // Current position in ResultSet, Total row count is known "at the end"  
//	private int       _readReportMod = 1; // start at 1, max value at 100  (in the beginning print report often, then report after every 100 row)
	private long      _expectedRowCount = -1;

	private int       _colCount;
	private List<String>  _pkList          = new ArrayList<>();
	private List<Integer> _pkPos           = new ArrayList<>();
	private List<Integer> _colJdbcDataType = new ArrayList<>();
	private List<String>  _colDbmsDataType = new ArrayList<>();
	private List<String>  _lobColList      = new ArrayList<>();
	
	private List<String>  _colNameList = new ArrayList<>();
	private String    _fullTableName;
	
	private boolean   _hasMoreRows = true;
	private Object[]  _lastRow;
	private Object[]  _lastPk;

	private String    _sqlText;

	public DiffTable(DiffSide side, DiffContext context, ResultSet rs, String sqlText, DbxConnection lookupConn)
	throws DiffException, SQLException
	{
		this(side, context, rs, sqlText, lookupConn, null);
	}
	public DiffTable(DiffSide side, DiffContext context, ResultSet rs, String sqlText, ConnectionProp connProps)
	throws DiffException, SQLException
	{
		this(side, context, rs, sqlText, null, connProps);
	}

	private DiffTable(DiffSide side, DiffContext context, ResultSet rs, String sqlText, DbxConnection lookupConn, ConnectionProp connProps)
	throws DiffException, SQLException
	{
		if (context == null)
			throw new IllegalArgumentException("Sorry context can't be null");
		
		_diffSide   = side;
		_context    = context;
		_rs         = rs;
		_sqlText    = sqlText;
		_lookupConn = lookupConn;
		_connProps  = connProps;
//		_rsmdc = new ResultSetMetaDataCached(_rs.getMetaData(), null, null, false);
		_rsmd       = _rs.getMetaData();
		_colCount   = _rsmd.getColumnCount();
		_rowCount   = 0;
		_expectedRowCount = -1;

		if (_dbmsVendor == null && _lookupConn != null)
		{
			try { _dbmsVendor = _lookupConn.getDatabaseProductName(); }
			catch(SQLException ignore) {}
		}
		
		List<String> pkList = context.getPkColumns();
		boolean generatedPk = false;
		
		// Initialize Column Names
		for (int c=0; c<_colCount; c++)
			_colNameList.add(_rsmd.getColumnLabel(c+1));
		
		// initialize the JDBC DataType array
		for (int c=0; c<_colCount; c++)
			_colJdbcDataType.add(_rsmd.getColumnType(c+1));

		// initialize the DBMS DataType array
		for (int c=0; c<_colCount; c++)
			_colDbmsDataType.add(_rsmd.getColumnTypeName(c+1));
		
		// Initialize LOB Column Names
		for (int c=0; c<_colCount; c++)
		{
			int jdbcType = _colJdbcDataType.get(c);
			if (jdbcType == Types.LONGVARCHAR || jdbcType == Types.LONGNVARCHAR || jdbcType == Types.LONGVARBINARY || jdbcType == Types.CLOB || jdbcType == Types.NCLOB || jdbcType == Types.BLOB)
			{
				String colName = _colNameList.get(c);
				_lobColList.add(colName);
			}
		}
		

		// Get Tablename from ResultSet
		if (_fullTableName == null)
			_fullTableName = getFullTableName(_rs);

		// Try to get PK from the ResultSet
		if (pkList == null)
		{
			pkList = findPrimaryKeyColsForResultSet(rs);
			if (pkList != null)
				generatedPk = true;
		}

		// CHeck if "the other side" (if we are RIGHT side and LEFT side has a PK, use this)
		if (pkList == null && context.getPkGeneratedColumns() != null)
		{
			pkList = context.getPkGeneratedColumns();
			_context.addDebugMessage("Re-Using PK columns from "+ ( DiffSide.LEFT.equals(_diffSide) ? DiffSide.RIGHT : DiffSide.LEFT )
					+". For table '"+_fullTableName+"'. No PK was passed to "+_diffSide+" DiffTable and looking at the ResultSet and Can't find any (PK or unique index) in the ResultSet's MetadaData.");
		}

		// Use ALL columns as a PK (but only basic data types, NOT LOB Columns etc)
		if (pkList == null && context._noPk_generateColumns)
		{
			pkList = buildAllNormalColumnsAsPkFromResultSet();
			if (pkList != null)
			{
				_context.addWarningMessage("Generated PK Columns from ALL Usable Table Columns. For table '"+_fullTableName+"'. NOTE: Only missing rows will be reported (when ALL columns are used). NOTE: Make sure that the ResultSet is SORTED according to the GeneratedPkList="+pkList+" otherwise A LOT of rows will be reported. No PK was passed to "+_diffSide+" DiffTable and looking at the ResultSet and Can't find any (PK or unique index) in the ResultSet's MetadaData.");
				generatedPk = true;
			}
		}
		
		if (pkList == null)
			throw new DiffException("No PK was passed to "+_diffSide+" DiffTable and looking at the ResultSet and Can't find any (Primary Key or unique index) in the ResultSet's MetadaData. "
					+ "For table '"+_fullTableName+"'. "
					+ "Even if you do NOT have a PK, please specify columns (with --keyCols) which you think is \"unique\", and can be used to detect row differences. "
					+ "NOTE: Data MUST be sorted on the those columns. (the diff algorithm is using a 'internal merge join' strategy on PK Cols to figgure out in what order it should read the streaming Results, since data isn't cached/stored in memory before logic is applied)");

		if (generatedPk)
			context.setPkGeneratedColumns(pkList);


		// Initialize the '_pkPos' structure
		_pkList = pkList;
		for (String pkCol : pkList)
		{
//			int pos = _colNameList.indexOf(pkCol);
			int pos = StringUtil.indexOfIgnoreCase(_colNameList, pkCol);
//System.out.println(_diffSide+": lookup pkCol='"+pkCol+"', hasPos="+pos);
			if (pos == -1)
				throw new DiffException("Primary Key Column '"+pkCol+"' was not found in the ResultSet. For table '"+_fullTableName+"'.");

			_pkPos.add(pos);
		}
		_lastPk = new Object[_pkPos.size()];
//System.out.println(_diffSide+": pkList="+pkList+", _pkPos="+_pkPos);
		
//		List<String> tables = new ArrayList<>();
//		for (int c=0; c<_colCount; c++)
//		{
//			// if tabname looks like "cat.sch.tabName", only use the last part (tabName)  
//			String tabName = _rsmd.getTableName(c+1);
//			if (tabName.lastIndexOf(".") != -1)
//				tabName = tabName.substring( tabName.lastIndexOf(".") + 1 );
//
//			if (StringUtil.hasValue(tabName) && !tables.contains(tabName))
//				tables.add(tabName);
//		}
//
//		if (tables.isEmpty())
//			_tableName = "UnknowTableName";
//		else
//			_tableName = tables.get(0);
//
//		if (tables.size() > 1)
//			_context.addWarningMessage("Multiple table names was found in the "+_diffSide+" ResultSet. The DiffTable will use '"+_tableName+"'. All Tables names: "+tables);
	}

	private String getFullTableName(ResultSet rs)
	throws SQLException
	{
		int cols = rs.getMetaData().getColumnCount();
		List<String> tables = new ArrayList<>(); 
		for (int c=1; c<=cols; c++)
		{
			String cat = rs.getMetaData().getCatalogName(c);
			String sch = rs.getMetaData().getSchemaName(c);
			String obj = rs.getMetaData().getTableName(c);

			String fullTabName = SqlObjectName.toString(cat, sch, obj);

			_context.addTraceMessage("getFullTableName[" + getDiffSide() + "]: ColumnNames: col=" + c + ", cat='" + cat + "', sch='" + sch + "', obj='" + obj + "', fullTabName='" + fullTabName + "'.");

			if (StringUtil.hasValue(fullTabName))
			{
				if ( ! tables.contains(fullTabName) )
					tables.add( fullTabName );
			}
		}
		if ( tables.size() >= 1)
		{
			String fullTabName = tables.get(0);

			if ( tables.size() > 1)
				_context.addWarningMessage("getFullTableName["+_diffSide+"]: The ResultSet contained "+tables.size()+" table references, Using the first found table '"+fullTabName+"'. Other Referenced Tables="+tables);

			return fullTabName;
		}
		return null;
	}
	
//	private List<String> findPrimaryKeyColsForResultSet(ResultSet rs)
//	{
//		try
//		{
//			int cols = rs.getMetaData().getColumnCount();
//			List<String> tables = new ArrayList<>(); 
//			for (int c=1; c<=cols; c++)
//			{
//				String cat = rs.getMetaData().getCatalogName(c);
//				String sch = rs.getMetaData().getSchemaName(c);
//				String obj = rs.getMetaData().getTableName(c);
//
//				String fullTabName = SqlObjectName.toString(cat, sch, obj);
//
//				if ( ! tables.contains(fullTabName) )
//					tables.add( fullTabName );
//			}
//			
//			List<String> pkCols = new ArrayList<>(); 
//
//			if ( tables.size() >= 1)
//			{
//				String fullTabName = tables.get(0);
//				
//				if ( tables.size() > 1)
//				{
//					_context.addWarningMessage("Find PkCols["+_diffSide+"]: The ResultSet contained "+tables.size()+" table references, Using PK from first found table '"+fullTabName+"'. Please use --keyCols 'col1,col2...' if this is not correct. Other Referenced Tables="+tables);
////					_context.addWarningMessage("Find PkCols: The ResultSet contained "+tables.size()+" table references, Sorry i can only figgure out the PK Cols if the ResultSet references only 1 table. Referenced Tables="+tables);
//				}
//
//				// Get a connection (if Connection Props are available, use that, and make a new connection)
//				// NOTE: Some JDBC Drivers will read out the origin ResultSet before we can call: dbmd.getPrimaryKeys or dbmd.getIndexInfo
//				//       That is if we reuse the connection from the ResultSet
//				Connection conn = null;
//				boolean createdNewConnectoin = false;
//				if (_connProps != null)
//				{
//					try 
//					{
//						_context.addInfoMessage("Find PkCols["+_diffSide+"]: Creating a new temporary connection to " +_diffSide + " to get PrimaryKey information. Please specify --keyCols to get around this...");
//						conn = DbxConnection.connect(null, _connProps);
//						createdNewConnectoin = true;
//					} 
//					catch (Exception ex) 
//					{
//						_context.addErrorMessage("Find PkCols["+_diffSide+"]: Problems Connecting to " +_diffSide + " to get PrimaryKey information. This will/may slow down the diff process. Please specify --keyCols to get around this problem.");
//					}
//				}
//
//				if (conn == null)
//				{
//					_context.addWarningMessage("Find PkCols["+_diffSide+"]: When getting PK Information, I'll reuse the connection from the ResultSet. This will/may cause performance problems. Please specify --keyCols to get around this problem.");
//					conn = rs.getStatement().getConnection();
//				}
//
//				// GET PrimaryKey or "first unique index"
//				SqlObjectName obj = new SqlObjectName(conn, fullTabName);
//				pkCols = TableInfo.getPkOrFirstUniqueIndex(conn, obj.getCatalogNameNull(), obj.getSchemaNameNull(), obj.getObjectName());
//
//				if (pkCols.isEmpty())
//					_context.addWarningMessage("Find PkCols["+_diffSide+"]: NO Primary Keys (or unique index) was found for the table '"+fullTabName+"'.");
//				else
//					_context.addInfoMessage("Find PkCols["+_diffSide+"]: The following columns "+pkCols+" will be used as a Primary Key Columns for DIFF. TablesInResultSet="+tables);
//
//				if (createdNewConnectoin)
//				{
//					_context.addInfoMessage("Find PkCols["+_diffSide+"]: Closing the temporary connection we just created to get PrimaryKey information. Please specify --keyCols if you do not want this.");
//					try { conn.close(); }
//					catch(SQLException ignore) {}
//				}
//			}
//			else
//			{
//				_context.addWarningMessage("Find PkCols["+_diffSide+"]: The ResultSet contained "+tables.size()+" table references, Sorry i can only figgure out the PK Cols if the ResultSet references only 1 table. Referenced Tables="+tables);
//			}
//			
//			if (pkCols.isEmpty())
//				pkCols = null;
//
//			return pkCols;
//		}
//		catch(SQLException ex)
//		{
//			_context.addErrorMessage("Find PkCols["+_diffSide+"]: Problems trying to get Primary Key Columns from the source ResultSet. Caught: " + ex);
//			//_logger.error("Problems trying to get Primary Key Columns from the source ResultSet", ex);
//			
//			return null;
//		}
//	}



	private List<String> findPrimaryKeyColsForResultSet(ResultSet rs)
	{
		try
		{
			List<String> pkCols = new ArrayList<>(); 

			if (_fullTableName == null)
				_fullTableName = getFullTableName(_rs);
			
			if ( _fullTableName != null )
			{
				// Get a connection (if Connection Props are available, use that, and make a new connection)
				// NOTE: Some JDBC Drivers will read out the origin ResultSet before we can call: dbmd.getPrimaryKeys or dbmd.getIndexInfo
				//       That is if we reuse the connection from the ResultSet
				Connection conn = _lookupConn;
				boolean createdNewConnection = false;
				if (_connProps != null && conn == null)
				{
					try 
					{
						_context.addDebugMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. Creating a new temporary connection to " +_diffSide + " to get PrimaryKey information. Please specify --keyCols to get around this...");
						conn = DbxConnection.connect(_context.getGuiOwnerAsWindow(), _connProps);

						if (_dbmsVendor == null)
						{
							try { _dbmsVendor = conn.getMetaData().getDatabaseProductName(); }
							catch(SQLException ignore) {}
						}

						createdNewConnection = true;
					} 
					catch (Exception ex) 
					{
						_context.addErrorMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. Problems Connecting to " +_diffSide + " to get PrimaryKey information. This will/may slow down the diff process. Please specify --keyCols to get around this problem.");
					}
				}

				if (conn == null)
				{
					_context.addWarningMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. When getting PK Information, I'll reuse the connection from the ResultSet. This will/may cause performance problems. Please specify --keyCols to get around this problem.");
					conn = rs.getStatement().getConnection();
					
					if (_dbmsVendor == null)
					{
						try { _dbmsVendor = conn.getMetaData().getDatabaseProductName(); }
						catch(SQLException ignore) {}
					}
				}

				// GET PrimaryKey or "first unique index"
				SqlObjectName obj = new SqlObjectName(conn, _fullTableName);
				pkCols = TableInfo.getPkOrFirstUniqueIndex(conn, obj.getCatalogNameNull(), obj.getSchemaNameNull(), obj.getObjectName());

				if (pkCols.isEmpty())
					_context.addWarningMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. NO Primary Keys (or unique index) was found.");
				else
					_context.addDebugMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. The following columns "+pkCols+" will be used as a Primary Key Columns for DIFF.");

				if (createdNewConnection)
				{
					_context.addDebugMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. Closing the temporary connection we just created to get PrimaryKey information. Please specify --keyCols if you do not want this.");
					try { conn.close(); }
					catch(SQLException ignore) {}
				}
			}
			else
			{
				_context.addWarningMessage("Find PkCols["+_diffSide+"]: The ResultSet contained NO table references, Sorry i can only figgure out the PK Cols.");
			}
			
			if (pkCols.isEmpty())
				pkCols = null;

			return pkCols;
		}
		catch(SQLException ex)
		{
			_context.addErrorMessage("Find PkCols["+_diffSide+"]: For table '"+_fullTableName+"'. Problems trying to get Primary Key Columns from the source ResultSet. Caught: " + ex);
			//_logger.error("Problems trying to get Primary Key Columns from the source ResultSet", ex);
			
			return null;
		}
	}

	private List<String> buildAllNormalColumnsAsPkFromResultSet()
	{
		List<String> pkCols = new ArrayList<>();

		for (int c=0; c<_colCount; c++)
		{
			String colName = _colNameList.get(c);

			if (isJdbcDataTypeSupportedInPkColumn(c))
				pkCols.add(colName);
		}

		if (pkCols.isEmpty())
			return null;

		return pkCols;
	}

	public DiffSide getDiffSide()
	{
		return _diffSide;
	}

	/** returns full table name. <code>[catalog].schema.table</code> */
	public String getFullTableName()
	{
		return _fullTableName;
	}
	/** returns same as getFullTableName() but stripping off database/catalog name it one exists */
	public String getShortTableName()
	{
		String shortTabName = _fullTableName;
		if (StringUtils.countMatches(shortTabName, '.') >= 2)
		{
			int firstDot = shortTabName.indexOf('.') + 1;
			shortTabName = shortTabName.substring(firstDot);
		}
		
		return shortTabName;
	}
	
	public List<Integer> getJdbcDataTypes()
	{
		return _colJdbcDataType;
	}

	/** DBMS Vendor specific data type. get from ResultSetMetaData.getColumnTypeName() */
	public List<String> getDbmsDataTypes()
	{
		return _colDbmsDataType;
	}


	public List<String> getColumnNames()
	{
		return _colNameList;
	}
	public String getColumnNamesCsv(String startQuoteChar, String endQuoteChar)
	{
		return StringUtil.toCommaStrQuoted(startQuoteChar, endQuoteChar, _colNameList);
	}
	public String getColumnNamesCsv(String startQuoteChar, String endQuoteChar, boolean skipLobColumns)
	{
		List<String> colNames = new ArrayList<>(_colNameList);
		if (skipLobColumns)
			colNames.removeAll(getLobColumnNames());
		
		return StringUtil.toCommaStrQuoted(startQuoteChar, endQuoteChar, colNames);
	}
	
	public List<String> getLobColumnNames()
	{
		return _lobColList;
	}

	public List<String> getPkColumnNames()
	{
		return _pkList;
	}
	public String getPkColumnNamesCsv(String startQuoteChar, String endQuoteChar)
	{
		return StringUtil.toCommaStrQuoted(startQuoteChar, endQuoteChar, _pkList);
	}

	/** Set SQL Text */
	public void setSqlText(String sqlText)
	{
		_sqlText = sqlText == null ? "" : sqlText;
	}

	/** @return Assigned SQL Text. Never null */
	public String getSqlText()
	{
		return _sqlText == null ? "" : _sqlText;
	}
	
	public static Object toTableValue(Object colVal)
	{
		if (colVal == null)
		{
			return ResultSetTableModel.DEFAULT_NULL_REPLACE;
//			return "NULL";
		}
		else
		{
			if (colVal instanceof byte[] )
			{
				String  BINARY_PREFIX  = Configuration.getCombinedConfiguration().getProperty(       ResultSetTableModel.PROPKEY_BINERY_PREFIX,  ResultSetTableModel.DEFAULT_BINERY_PREFIX);
				boolean BINARY_TOUPPER = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetTableModel.PROPKEY_BINARY_TOUPPER, ResultSetTableModel.DEFAULT_BINARY_TOUPPER);
				
				return StringUtil.bytesToHex(BINARY_PREFIX, (byte[])colVal, BINARY_TOUPPER);
			}
			else
			{
				return colVal;
			}
		}
	}

	public String toSqlValues(Object[] row)
	{
		StringBuilder sb = new StringBuilder();

		String suffix = ", ";
		
		for (Object o : row)
		{
			if (o == null)
			{
				sb.append( "NULL" ).append(suffix);
			}
			else
			{
				if (o instanceof byte[] )
				{
					String  BINARY_PREFIX  = Configuration.getCombinedConfiguration().getProperty(       ResultSetTableModel.PROPKEY_BINERY_PREFIX,  ResultSetTableModel.DEFAULT_BINERY_PREFIX);
					boolean BINARY_TOUPPER = Configuration.getCombinedConfiguration().getBooleanProperty(ResultSetTableModel.PROPKEY_BINARY_TOUPPER, ResultSetTableModel.DEFAULT_BINARY_TOUPPER);
					
					sb.append( StringUtil.bytesToHex(BINARY_PREFIX, (byte[])o, BINARY_TOUPPER) ).append(suffix);
				}
				else
				{
					String val = DbUtils.safeStr(o);

					// If the String is to long 16K - 2 quote chars
					// APPEND a WARNING Message that the string is to long and will be truncated

					// NOTE: This is for ASE, which has a 16K limit for string literals
					//       Other DBMS's probably has another limit
					int maxStringLiteral = 16384;
					String warningMsg = "";
					
					// TODO: setting 'maxStringLiteral' for other DBMS Vendors
					//       MOVE this to when we initialize the DiffTab, so we don't have to check the *every* time
					//if (_dbmsVendor != null)
					//{
					//	if      (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_SYBASE_ASE)) maxStringLiteral = 16384;
					//	else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_MSSQL     )) maxStringLiteral = 4000;
					//	else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_ORACLE    )) maxStringLiteral = 4000;
					//	else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_MYSQL     )) maxStringLiteral = 1200; // ????
					//	else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_POSTGRES  )) maxStringLiteral = ####;
					//	else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_DB2       )) maxStringLiteral = 32704;
					//	else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_XXXXXXXXXX)) maxStringLiteral = ####;
					//}

					if (val.length() >= (maxStringLiteral - 2) )
					{
						int valLength = val.length(); 
						warningMsg = "\n/* WARNING SQL String literal length is above (" + maxStringLiteral + " bytes), length = " + valLength + " insert/update will FAIL and/or data will be TRUNCATED, please use option ActionType.SYNC_{LEFT|RIGHT}, which used PreparedStatement to apply the correction. */ \n";
					}
					
					sb.append(warningMsg).append( val ).append(suffix);
				}
			}
		}

		sb.setLength(sb.length() - suffix.length());
		
		return sb.toString();
	}

	public String getPkWhereClause(Object[] row, String leftQuote, String rightQuote)
	{
		StringBuilder sb = new StringBuilder();

		String suffix = " and ";
		
		sb.append(" WHERE ");
		for (int pkPos : _pkPos)
			sb.append(leftQuote).append(_pkList.get(pkPos)).append(rightQuote).append(" = ").append( DbUtils.safeStr(row[pkPos]) ).append(suffix);

		sb.setLength(sb.length() - suffix.length());
		
		return sb.toString();
	}

	public String getUpdateSet(Object[] row, List<Integer> cols, String leftQuote, String rightQuote)
	{
		StringBuilder sb = new StringBuilder();

		String suffix = ", ";
		
		sb.append(" SET ");
		for (int c=0; c<getColumnCount(); c++)
		{
			// Skip PK Columns
			if (_pkPos.contains(c))
				continue;

			// Skip columns that has NOT been updated
			if ( ! cols.contains(c) )
				continue;
			
			sb.append(leftQuote).append(_colNameList.get(c)).append(rightQuote).append(" = ").append( DbUtils.safeStr(row[c]) ).append(suffix);
		}

		sb.setLength(sb.length() - suffix.length());
		sb.append(" ");
		
		return sb.toString();
	}
	
	public boolean isJdbcDataTypeSupportedInPkColumnThrow(String colName)
	throws DiffException
	{
//		return isJdbcDataTypeSupportedInPkColumnThrow( getPkColumnNames().indexOf(colName));
		return isJdbcDataTypeSupportedInPkColumnThrow( StringUtil.indexOfIgnoreCase(getPkColumnNames(), colName) );
	}
	public boolean isJdbcDataTypeSupportedInPkColumnThrow(int colPos)
	throws DiffException
	{
		if ( isJdbcDataTypeSupportedInPkColumn(colPos) )
			return true;
		
		String colName      = getColumnNames().get(colPos);
		int    jdbcType     = getJdbcDataTypes().get(colPos);
		String jdbcTypeName = ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType);
		
		// Not supported: THROW EXCEPTION
		throw new DiffException("JDBC Dataype "+jdbcType+" = '"+jdbcTypeName+"' is NOT Supported for Primary Key columns. ColPos=, ColName='"+colName+"'. For table '"+_fullTableName+"'.");
	}
	
	public boolean isJdbcDataTypeSupportedInPkColumn(String colName)
	{
//		return isJdbcDataTypeSupportedInPkColumn( getPkColumnNames().indexOf(colName));
		return isJdbcDataTypeSupportedInPkColumn( StringUtil.indexOfIgnoreCase(getPkColumnNames(), colName) );
	}
	public boolean isJdbcDataTypeSupportedInPkColumn(int colPos)
	{
		int    jdbcDataType = getJdbcDataTypes().get(colPos);
		String dbmsDataType = getDbmsDataTypes().get(colPos);

		//--------------------------------------------
		// BEGIN: Vendor specific info:
		//--------------------------------------------
		//
		// Sybase ASE (and possibly ASE, and IQ... NOT TESTED)
		//     * text    -> LONGVARCHAR    --> NOT Allowed in PK or ORDER BY
		//     * unitext -> LONGNVARCHAR   --> NOT Allowed in PK or ORDER BY
		//     * image   -> LONGVARBINARY  --> NOT Allowed in PK or ORDER BY
		//
		// SQL-Server
		//     * text        -> LONGVARCHAR    --> NOT Allowed in PK or ORDER BY
		//     * ntext       -> LONGNVARCHAR   --> NOT Allowed in PK or ORDER BY
		//     * image       -> LONGVARBINARY  --> NOT Allowed in PK or ORDER BY
		//
		// Postgres
		//     * text        -> VARCHAR        --> OK in PK or ORDER BY
		//     * bytea       -> BINARY         --> OK in PK or ORDER BY
		//     * bool        -> BIT            --> OK in PK or ORDER BY
		//     * serial      -> INTEGER        --> OK in PK or ORDER BY
		//     * timestamptz -> TIMESTAMP      --> OK in PK or ORDER BY
		//     * oid         -> BIGINT         --> OK in PK or ORDER BY
		//     * uuid        -> OTHER          --> OK in PK or ORDER BY
		//     * json        -> OTHER          --> NOT Allowed in PK or ORDER BY
		//     * interval    -> OTHER          --> OK in PK or ORDER BY
		//
		//--------------------------------------------
		// END: Vendor specific info:
		//--------------------------------------------
		
		boolean allowLongVarChar   = true;
		boolean allowLongNVarChar  = true;
		boolean allowLongVarBinary = true;
		if (_dbmsVendor != null)
		{
			if (DbUtils.isProductName(_dbmsVendor, 
					 DbUtils.DB_PROD_NAME_SYBASE_ASE
					,DbUtils.DB_PROD_NAME_SYBASE_ASA
					,DbUtils.DB_PROD_NAME_SYBASE_IQ
					,DbUtils.DB_PROD_NAME_MSSQL
					,DbUtils.DB_PROD_NAME_MYSQL
			)) {
				allowLongVarChar   = false;
				allowLongNVarChar  = false;
				allowLongVarBinary = false;
			}
		}

		switch (jdbcDataType)
		{
		case Types.BIT                     : return true;
		case Types.TINYINT                 : return true;
		case Types.SMALLINT                : return true;
		case Types.INTEGER                 : return true;
		case Types.BIGINT                  : return true;
		case Types.FLOAT                   : return true;
		case Types.REAL                    : return true;
		case Types.DOUBLE                  : return true;
		case Types.NUMERIC                 : return true;
		case Types.DECIMAL                 : return true;
		case Types.CHAR                    : return true;
		case Types.VARCHAR                 : return true;
		case Types.LONGVARCHAR             : return allowLongVarChar;
		case Types.DATE                    : return true;
		case Types.TIME                    : return true;
		case Types.TIMESTAMP               : return true;
		case Types.BINARY                  : return true;
		case Types.VARBINARY               : return true;
		case Types.LONGVARBINARY           : return allowLongVarBinary;
		case Types.NULL                    : return true;
		case Types.OTHER                   : 
		{
			// The OTHER is a bit specific, and has to be handled for each DBMS Vendor
			if (_dbmsVendor != null && dbmsDataType != null)
			{
				if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_POSTGRES))
				{
					// RS> Col# Label JDBC Type Name           Guessed DBMS type Source Table
					// RS> ---- ----- ------------------------ ----------------- ------------
					// RS> 1    id    java.sql.Types.VARCHAR   text(2147483647)  dummytable1 
					// RS> 2    c1    java.sql.Types.OTHER     uuid              dummytable1 
					// RS> 3    c2    java.sql.Types.TIMESTAMP timestamptz       dummytable1 
					// RS> 4    c3    java.sql.Types.BIT       bool              dummytable1 
					// RS> 5    c4    java.sql.Types.INTEGER   serial            dummytable1 
					// RS> 6    c5    java.sql.Types.OTHER     json              dummytable1 
					// RS> 7    c6    java.sql.Types.OTHER     interval          dummytable1 
					// RS> 8    c7    java.sql.Types.BINARY    bytea(2147483647) dummytable1 
					// RS> 9    c8    java.sql.Types.BIGINT    oid               dummytable1 

					if ("json".equalsIgnoreCase(dbmsDataType)) return false; // NOT ok in PK or ORDER BY
				}
				else if (DbUtils.isProductName(_dbmsVendor, DbUtils.DB_PROD_NAME_ORACLE))
				{
					// FIXME: Oracle has X number of obscure data types, which probably will have to be mapped here
					//        or they will use the SPECIFIC oracle.jdbc.OracleTypes.*
				}
			}
			return true;
		}
	    //------------------------- Since java 1.2 -----------------------------------
		case Types.JAVA_OBJECT             : return false;
		case Types.DISTINCT                : return false;
		case Types.STRUCT                  : return false;
		case Types.ARRAY                   : return false;
		case Types.BLOB                    : return false;
		case Types.CLOB                    : return false;
		case Types.REF                     : return false;
	    //------------------------- Since java 1.4 -----------------------------------
		case Types.DATALINK                : return false;
		case Types.BOOLEAN                 : return true;
	    //------------------------- JDBC 4.0 (Since java 1.6) ------------------------
		case Types.ROWID                   : return false;
		case Types.NCHAR                   : return true;
		case Types.NVARCHAR                : return true;
		case Types.LONGNVARCHAR            : return allowLongNVarChar;
		case Types.NCLOB                   : return false;
		case Types.SQLXML                  : return false;
	    //--------------------------JDBC 4.2 (Since java 1.8) ------------------------
		case Types.REF_CURSOR              : return false;
		case Types.TIME_WITH_TIMEZONE      : return true;
		case Types.TIMESTAMP_WITH_TIMEZONE : return true;
//		case -100:                         return "oracle.jdbc.OracleTypes.TIMESTAMPNS";
//		case -101:                         return "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
//		case -102:                         return "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
//		case -103:                         return "oracle.jdbc.OracleTypes.INTERVALYM";
//		case -104:                         return "oracle.jdbc.OracleTypes.INTERVALDS";
//		case  -10:                         return "oracle.jdbc.OracleTypes.CURSOR";
//		case  -13:                         return "oracle.jdbc.OracleTypes.BFILE";
//		case 2007:                         return "oracle.jdbc.OracleTypes.OPAQUE";
//		case 2008:                         return "oracle.jdbc.OracleTypes.JAVA_STRUCT";
//		case  -14:                         return "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
//		case  100:                         return "oracle.jdbc.OracleTypes.BINARY_FLOAT";
//		case  101:                         return "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
////		case    2:                         return "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
////		case   -2:                         return "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
//		case  999:                         return "oracle.jdbc.OracleTypes.FIXED_CHAR";
		}
		
		return true;
	}
	
	public boolean isJdbcDataTypeSupportedInDiffColumnThrow(int colPos)
	throws DiffException
	{
		if ( isJdbcDataTypeSupportedInDiffColumn(colPos) )
			return true;
		
		String colName      = getColumnNames().get(colPos);
		int    jdbcType     = getJdbcDataTypes().get(colPos);
		String jdbcTypeName = ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType);
		
		// Not supported: THROW EXCEPTION
		throw new DiffException("JDBC Dataype "+jdbcType+" = '"+jdbcTypeName+"' is NOT Supported for Diff columns. ColPos=, ColName='"+colName+"'. For table '"+_fullTableName+"'.");
	}
	
	public boolean isJdbcDataTypeSupportedInDiffColumn(int colPos)
	{
		int jdbcDataType = getJdbcDataTypes().get(colPos);

		switch (jdbcDataType)
		{
		case Types.BIT                     : return true;
		case Types.TINYINT                 : return true;
		case Types.SMALLINT                : return true;
		case Types.INTEGER                 : return true;
		case Types.BIGINT                  : return true;
		case Types.FLOAT                   : return true;
		case Types.REAL                    : return true;
		case Types.DOUBLE                  : return true;
		case Types.NUMERIC                 : return true;
		case Types.DECIMAL                 : return true;
		case Types.CHAR                    : return true;
		case Types.VARCHAR                 : return true;
		case Types.LONGVARCHAR             : return true;
		case Types.DATE                    : return true;
		case Types.TIME                    : return true;
		case Types.TIMESTAMP               : return true;
		case Types.BINARY                  : return true;
		case Types.VARBINARY               : return true;
		case Types.LONGVARBINARY           : return true;
		case Types.NULL                    : return true;
		case Types.OTHER                   : return true;
	    //------------------------- Since java 1.2 -----------------------------------
		case Types.JAVA_OBJECT             : return true;
		case Types.DISTINCT                : return true;
		case Types.STRUCT                  : return true;
		case Types.ARRAY                   : return true;
		case Types.BLOB                    : return true;
		case Types.CLOB                    : return true;
		case Types.REF                     : return true;
	    //------------------------- Since java 1.4 -----------------------------------
		case Types.DATALINK                : return true;
		case Types.BOOLEAN                 : return true;
	    //------------------------- JDBC 4.0 (Since java 1.6) ------------------------
		case Types.ROWID                   : return true;
		case Types.NCHAR                   : return true;
		case Types.NVARCHAR                : return true;
		case Types.LONGNVARCHAR            : return true;
		case Types.NCLOB                   : return true;
		case Types.SQLXML                  : return true;
	    //--------------------------JDBC 4.2 (Since java 1.8) ------------------------
		case Types.REF_CURSOR              : return true;
		case Types.TIME_WITH_TIMEZONE      : return true;
		case Types.TIMESTAMP_WITH_TIMEZONE : return true;
//		case -100:                         return "oracle.jdbc.OracleTypes.TIMESTAMPNS";
//		case -101:                         return "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
//		case -102:                         return "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
//		case -103:                         return "oracle.jdbc.OracleTypes.INTERVALYM";
//		case -104:                         return "oracle.jdbc.OracleTypes.INTERVALDS";
//		case  -10:                         return "oracle.jdbc.OracleTypes.CURSOR";
//		case  -13:                         return "oracle.jdbc.OracleTypes.BFILE";
//		case 2007:                         return "oracle.jdbc.OracleTypes.OPAQUE";
//		case 2008:                         return "oracle.jdbc.OracleTypes.JAVA_STRUCT";
//		case  -14:                         return "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
//		case  100:                         return "oracle.jdbc.OracleTypes.BINARY_FLOAT";
//		case  101:                         return "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
////		case    2:                         return "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
////		case   -2:                         return "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
//		case  999:                         return "oracle.jdbc.OracleTypes.FIXED_CHAR";
		}
		
		return true;
	}


	private Object getDataValue(int colPos)
	throws SQLException
	{
		int jdbcDataType = getJdbcDataTypes().get(colPos - 1); // Array vs JDBC Pos

		switch (jdbcDataType)
		{
		case Types.BIT                     : return _rs.getObject   (colPos);
		case Types.TINYINT                 : return _rs.getObject   (colPos);
		case Types.SMALLINT                : return _rs.getObject   (colPos);
		case Types.INTEGER                 : return _rs.getObject   (colPos);
		case Types.BIGINT                  : return _rs.getObject   (colPos);
		case Types.FLOAT                   : return _rs.getObject   (colPos);
		case Types.REAL                    : return _rs.getObject   (colPos);
		case Types.DOUBLE                  : return _rs.getObject   (colPos);
		case Types.NUMERIC                 : return _rs.getObject   (colPos);
		case Types.DECIMAL                 : return _rs.getObject   (colPos);
		case Types.CHAR                    : return _rs.getString   (colPos);
		case Types.VARCHAR                 : return _rs.getString   (colPos);
		case Types.LONGVARCHAR             : return _rs.getString   (colPos);
		case Types.DATE                    : return _rs.getDate     (colPos);
		case Types.TIME                    : return _rs.getTime     (colPos);
		case Types.TIMESTAMP               : return _rs.getTimestamp(colPos);
		case Types.BINARY                  : return _rs.getBytes    (colPos);
		case Types.VARBINARY               : return _rs.getBytes    (colPos);
		case Types.LONGVARBINARY           : return _rs.getBytes    (colPos);
		case Types.NULL                    : return _rs.getObject   (colPos);
		case Types.OTHER                   : return _rs.getObject   (colPos);
	    //------------------------- Since java 1.2 -----------------------------------
		case Types.JAVA_OBJECT             : return _rs.getObject   (colPos);
		case Types.DISTINCT                : return _rs.getObject   (colPos);
		case Types.STRUCT                  : return _rs.getObject   (colPos);
		case Types.ARRAY                   : return _rs.getObject   (colPos);
		case Types.BLOB                    : return _rs.getBytes    (colPos);
		case Types.CLOB                    : return _rs.getString   (colPos);
		case Types.REF                     : return _rs.getObject   (colPos);
	    //------------------------- Since java 1.4 -----------------------------------
		case Types.DATALINK                : return _rs.getObject   (colPos);
		case Types.BOOLEAN                 : return _rs.getObject   (colPos);
	    //------------------------- JDBC 4.0 (Since java 1.6) ------------------------
		case Types.ROWID                   : return _rs.getString   (colPos);
		case Types.NCHAR                   : return _rs.getString   (colPos);
		case Types.NVARCHAR                : return _rs.getString   (colPos);
		case Types.LONGNVARCHAR            : return _rs.getString   (colPos);
		case Types.NCLOB                   : return _rs.getString   (colPos);
		case Types.SQLXML                  : return _rs.getString   (colPos);
	    //--------------------------JDBC 4.2 (Since java 1.8) ------------------------
		case Types.REF_CURSOR              : return _rs.getObject   (colPos);
//		case Types.TIME_WITH_TIMEZONE      : return _rs.getObject   (colPos);
//		case Types.TIMESTAMP_WITH_TIMEZONE : return _rs.getObject   (colPos);
//		case Types.TIME_WITH_TIMEZONE      : return OffsetTime    .parse( _rs.getString(colPos) );
//		case Types.TIMESTAMP_WITH_TIMEZONE : return OffsetDateTime.parse( _rs.getString(colPos) );
//		case Types.TIME_WITH_TIMEZONE      : return OffsetTime   .parse( _rs.getString(colPos) );
//		case Types.TIMESTAMP_WITH_TIMEZONE : return ZonedDateTime.parse( _rs.getString(colPos) );
		case Types.TIME_WITH_TIMEZONE      : return _rs.getTime     (colPos);
		case Types.TIMESTAMP_WITH_TIMEZONE : return _rs.getTimestamp(colPos);

		//------------------------- VENDOR SPECIFIC TYPES --------------------------- (grabbed from ojdbc7.jar)
//		case -100:                         return "oracle.jdbc.OracleTypes.TIMESTAMPNS";
//		case -101:                         return "oracle.jdbc.OracleTypes.TIMESTAMPTZ";
//		case -102:                         return "oracle.jdbc.OracleTypes.TIMESTAMPLTZ";
//		case -103:                         return "oracle.jdbc.OracleTypes.INTERVALYM";
//		case -104:                         return "oracle.jdbc.OracleTypes.INTERVALDS";
//		case  -10:                         return "oracle.jdbc.OracleTypes.CURSOR";
//		case  -13:                         return "oracle.jdbc.OracleTypes.BFILE";
//		case 2007:                         return "oracle.jdbc.OracleTypes.OPAQUE";
//		case 2008:                         return "oracle.jdbc.OracleTypes.JAVA_STRUCT";
//		case  -14:                         return "oracle.jdbc.OracleTypes.PLSQL_INDEX_TABLE";
//		case  100:                         return "oracle.jdbc.OracleTypes.BINARY_FLOAT";
//		case  101:                         return "oracle.jdbc.OracleTypes.BINARY_DOUBLE";
////		case    2:                         return "oracle.jdbc.OracleTypes.NUMBER";             // same as: java.sql.Types.NUMERIC
////		case   -2:                         return "oracle.jdbc.OracleTypes.RAW";                // same as: java.sql.Types.BINARY
//		case  999:                         return "oracle.jdbc.OracleTypes.FIXED_CHAR";
		}

		// type is not covered above
		return _rs.getObject(colPos);
	}


	/**
	 * Read NEXT row
	 * 
	 * @return The last row, which also can be fetched using <code>getRow()</code>
	 * @throws SQLException
	 */
	public Object[] getNextRow()
	throws SQLException
	{
		if ( ! _hasMoreRows )
			return null;
		
//		// Check for cancellation
//		if ( _context.getProgressDialog() != null && _context.getProgressDialog().isCancelled() )
//		{
//			_context.addWarningMessage("Reading rows was cancelled after " + _rowCount + " rows was read.");
//			return null;
//		}
//
		// Read NEXT row
		_hasMoreRows = _rs.next();
		if ( ! _hasMoreRows )
		{
			_lastRow = null;
			return _lastRow;
		}
		
		Object[] row = new Object[_colCount];
		for (int c=0; c<_colCount; c++)
		{
//			row[c] = _rs.getObject(c+1);
			row[c] = getDataValue(c+1);
		}
		
		_rowCount++;
		_lastRow = row;
		
		// Get the object size of the _lastRow and append it to statistics
		// Possibly use: java.lang.instrument.Instrumentation (see: https://stackoverflow.com/questions/9368764/calculate-size-of-object-in-java)
		// _rowBytesRead += getObjectSize(row);
		
		// construct the PK
		for (int i=0; i<_lastPk.length; i++)
		{
			int rowColPos = _pkPos.get(i);
			_lastPk[i] = _lastRow[rowColPos];
		}
//System.out.println(_diffSide+":at["+_rowCount+"]: pk.length="+_lastPk.length+", PK-Vals: "+StringUtil.toCommaStr(_lastPk));
		
//		if (_context.getProgressDialog() != null)
//		{
//			if ( (_rowCount % _readReportMod) == 0 )
//			{
//				_context.getProgressDialog().setState("Reading row " + _rowCount + " from " + _diffSide);
//
//				// In the beginning report OFTEN, but after 100 record, report only every 100 row read
//				if (_rowCount > 100)
//					_readReportMod =  100;
//			}
//		}

		// Check for Warnings...
		for (SQLWarning sqlw = _rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			_context.addWarningMessage(sqlw.toString());
		_rs.clearWarnings();

		// Return the row we just read
		return _lastRow;
	}

	/** Get the last row fetched using getNextRow() */
	public Object[] getRow()
	{
		return _lastRow;
	}

	/** Get the PrimaryKey columns last row fetched using getNextRow() */
	public Object[] getPk()
	{
		return _lastPk;
	}
	/** Copy the current records of the PK into a new Object[], or in other words a: deep clone */
	public Object[] getPkCopy()
	{
		Object[] tmp = new Object[_lastPk.length];
		System.arraycopy(_lastPk, 0, tmp, 0, _lastPk.length);
		
		return tmp;
	}

	public int getColumnCount()
	{
		return _colCount;
	}

	public long getRowCount()
	{
		return _rowCount;
	}
	
	public long getExpectedRowCount()
	{
		return _expectedRowCount;
	}
	
	public void setExpectedRowCount(long expectedRowCount)
	{
		_expectedRowCount = expectedRowCount;
	}
	
//	public ResultSetMetaDataCached getMetaData()
//	{
//		return _rsmdc;
//	}

	/**
	 * Close the ResultSet silently
	 */
	public void close()
	{
		try { _rs.close(); }
		catch(SQLException ignore) {}
	}

	/**
	 * Send cancel on the Statement that is responsible for the ResultSet
	 */
	public void sendCancel()
	{
		if (_rs == null)
			return;

		// If it's already closed, get out of here
		try
		{
			if (_rs.isClosed())
				return;
		}
		catch (SQLException ignore) 
		{
			return;
		}

		// Cancel the query at the SERVER SIDE
		try
		{
			_rs.getStatement().cancel();
		}
		catch (SQLException ex) 
		{
			_context.addWarningMessage("Canceling " + _diffSide + " ResultSet caught problem... skipping the cancel. ex: "+ex);
		}
	}
}
