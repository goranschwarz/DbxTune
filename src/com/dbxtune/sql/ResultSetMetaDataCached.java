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
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.h2.tools.SimpleResultSet;

import com.dbxtune.RsTune;
import com.dbxtune.Version;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.ddl.DataTypeNotResolvedException;
import com.dbxtune.sql.ddl.IDbmsDataTypeResolver;
import com.dbxtune.utils.StringUtil;

/**
 * A class that implements ResultSetMetaData by making a copy of all the data from another ResultSetMetaData object. 
 */
public class ResultSetMetaDataCached implements ResultSetMetaData, java.io.Serializable, Cloneable
{
	private static final long   serialVersionUID = 1L;
	private static final Logger	_logger	= Logger.getLogger(ResultSetMetaDataCached.class);

	public enum State
	{
		/** When the ResultSetMetaDataCached has just been created, possibly from a ResultSetMetaData */
		ORIGIN, 
		
		/** When the ResultSetMetaDataCached has just been normalized, Data Types may have been changed etc by the <code>IDbmsDataTypeResolver.dataTypeResolverSource()</code>. */
		NORMALIZED, 

		/** When the ResultSetMetaDataCached has just been transformed, to a specific DBMS Vendors Capabilities <code>IDbmsDataTypeResolver.dataTypeResolverTarget()</code>. */
		TRANSLATED
	};
	
	private State _state = State.ORIGIN;
	
	public State   getState() { return _state; }
	public boolean isStateOrigin()     { return State.ORIGIN    .equals(_state); }
	public boolean isStateNormalized() { return State.NORMALIZED.equals(_state); }
	public boolean isStateTranslated() { return State.TRANSLATED.equals(_state); }
	

	/**
	 * A user preference: If set true, this class will convert column names
	 * upper case for you. It doesn't affect any of the other strings.
	 */
	private boolean     _upcaseColumnNames;

	private List<Entry> _entries;
	
//	private DbmsDataTypeResolver _dataTypeResolver;
	
	private String   _originDbms;

	public String getDatabaseProductName() { return _originDbms; };
	
	/** Entry with all data */
	public static class Entry
	{
		private boolean  _autoIncrement;
		private boolean  _caseSensitive;
		private boolean  _searchable;
		private boolean  _currency;
		private int      _nullable;
		private boolean  _signed;
		private int      _columnDisplaySize;
		private String   _columnLabel;
		private String   _columnName;
		private String   _schemaName;
		private int      _precision;
		private int      _scale;
		private String   _tableName;
		private String   _catalogName;
		private int      _columnType;
		private String   _columnTypeName;
		private boolean  _readOnly;
		private boolean  _writable;
		private boolean  _definitelyWritable;
		private String   _columnClassName;

		// Not standard
		private int      _columnPos;
		private String   _columnResolvedTypeName;
//		private boolean  _isAltered;

		private int      _changeBitMap = 0;

		private boolean  _fakedColumn = false;  // if a column has been added, but it's NOT part of the ResultSet
		private Object   _fakedColumnDefaultValue = null;

		
		public static int CHANGED_autoIncrement      = 1;      
		public static int CHANGED_caseSensitive      = 2;      
		public static int CHANGED_searchable         = 4;      
		public static int CHANGED_currency           = 8;      
		public static int CHANGED_nullable           = 16;     
		public static int CHANGED_signed             = 32;     
		public static int CHANGED_columnDisplaySize  = 64;     
		public static int CHANGED_columnLabel        = 128;    
		public static int CHANGED_columnName         = 256;    
		public static int CHANGED_schemaName         = 512;    
		public static int CHANGED_precision          = 1024;   
		public static int CHANGED_scale              = 2048;   
		public static int CHANGED_tableName          = 4096;   
		public static int CHANGED_catalogName        = 8192;   
		public static int CHANGED_columnType         = 16384;  
		public static int CHANGED_columnTypeName     = 32768;  
		public static int CHANGED_readOnly           = 65536;  
		public static int CHANGED_writable           = 131072; 
		public static int CHANGED_definitelyWritable = 262144; 
		public static int CHANGED_columnClassName    = 524288; 

		public boolean isAutoIncrement     () { return _autoIncrement     ; }
		public boolean isCaseSensitive     () { return _caseSensitive     ; }
		public boolean isSearchable        () { return _searchable        ; }
		public boolean isCurrency          () { return _currency          ; }
		public boolean isNullable          () { return _nullable != ResultSetMetaData.columnNoNulls; }
		public boolean isSigned            () { return _signed            ; }
		public int     getColumnDisplaySize() { return _columnDisplaySize ; }
		public String  getColumnLabel      () { return _columnLabel       ; }
		public String  getColumnName       () { return _columnName        ; }
		public String  getSchemaName       () { return _schemaName        ; }
		public int     getPrecision        () { return _precision         ; }
		public int     getScale            () { return _scale             ; }
		public String  getTableName        () { return _tableName         ; }
		public String  getCatalogName      () { return _catalogName       ; }
		public int     getColumnType       () { return _columnType        ; }
		public String  getColumnTypeName   () { return _columnTypeName    ; }
		public boolean isReadOnly          () { return _readOnly          ; }
		public boolean isWritable          () { return _writable          ; }
		public boolean isDefinitelyWritable() { return _definitelyWritable; }
		public String  getColumnClassName  () { return _columnClassName   ; }

		public int     getColumnPos        () { return _columnPos         ; }
		public String  setColumnResolvedTypeName() { return _columnResolvedTypeName    ; }
//		public boolean isAltered           () { return _isAltered         ; }
		public boolean wasChanged          () { return _changeBitMap != 0 ; }

		public boolean wasChanged(int changeBitMap)
		{
			return (changeBitMap & _changeBitMap) != 0;
		}
		public int     getColumnJdbcType        () { return getColumnType(); }
		public String  getColumnJdbcTypeStr     () { return DataTypeNotResolvedException.getJdbcTypeAsString(_columnType); }
//		public String  getColumnTypeStr         () { return getColumnTypeStr(); } // ERROR: This calls itself
		public String  getColumnResolvedTypeName() { return _columnResolvedTypeName; }



		public void setAutoIncrement     (boolean autoIncrement     ) { _autoIncrement      = autoIncrement     ; _changeBitMap |= CHANGED_autoIncrement     ; }
		public void setCaseSensitive     (boolean caseSensitive     ) { _caseSensitive      = caseSensitive     ; _changeBitMap |= CHANGED_caseSensitive     ; }
		public void setSearchable        (boolean searchable        ) { _searchable         = searchable        ; _changeBitMap |= CHANGED_searchable        ; }
		public void setCurrency          (boolean currency          ) { _currency           = currency          ; _changeBitMap |= CHANGED_currency          ; }
		public void setNullable          (int     nullable          ) { _nullable           = nullable          ; _changeBitMap |= CHANGED_nullable          ; }
		public void setSigned            (boolean signed            ) { _signed             = signed            ; _changeBitMap |= CHANGED_signed            ; }
		public void setColumnDisplaySize (int     columnDisplaySize ) { _columnDisplaySize  = columnDisplaySize ; _changeBitMap |= CHANGED_columnDisplaySize ; }
		public void setColumnLabel       (String  columnLabel       ) { _columnLabel        = columnLabel       ; _changeBitMap |= CHANGED_columnLabel       ; }
		public void setColumnName        (String  columnName        ) { _columnName         = columnName        ; _changeBitMap |= CHANGED_columnName        ; }
		public void setSchemaName        (String  schemaName        ) { _schemaName         = schemaName        ; _changeBitMap |= CHANGED_schemaName        ; }
		public void setPrecision         (int     precision         ) { _precision          = precision         ; _changeBitMap |= CHANGED_precision         ; }
		public void setScale             (int     scale             ) { _scale              = scale             ; _changeBitMap |= CHANGED_scale             ; }
		public void setTableName         (String  tableName         ) { _tableName          = tableName         ; _changeBitMap |= CHANGED_tableName         ; }
		public void setCatalogName       (String  catalogName       ) { _catalogName        = catalogName       ; _changeBitMap |= CHANGED_catalogName       ; }
		public void setColumnType        (int     columnType        ) { _columnType         = columnType        ; _changeBitMap |= CHANGED_columnType        ; }
		public void setColumnTypeName    (String  columnTypeName    ) { _columnTypeName     = columnTypeName    ; _changeBitMap |= CHANGED_columnTypeName    ; }
		public void setReadOnly          (boolean readOnly          ) { _readOnly           = readOnly          ; _changeBitMap |= CHANGED_readOnly          ; }
		public void setWritable          (boolean writable          ) { _writable           = writable          ; _changeBitMap |= CHANGED_writable          ; }
		public void setDefinitelyWritable(boolean definitelyWritable) { _definitelyWritable = definitelyWritable; _changeBitMap |= CHANGED_definitelyWritable; }
		public void setColumnClassName   (String  columnClassName   ) { _columnClassName    = columnClassName   ; _changeBitMap |= CHANGED_columnClassName   ; }

//		public void setColumnPos         (int     pos               ) { _columnPos          = pos; }
		public void setColumnResolvedTypeName(String resolvedTypeName){ _columnResolvedTypeName = resolvedTypeName ; }
//		public void setAltered               (boolean altered)        { _isAltered              = altered; }


		public void    setFakedColumn(boolean fakedColumn) { _fakedColumn = fakedColumn; }
		public boolean isFakedColumn()              { return _fakedColumn; }

		public void   setFakedColumnDefaultValue(Object defaultValue) { _fakedColumnDefaultValue = defaultValue; }
		public Object getFakedColumnDefaultValue()             { return _fakedColumnDefaultValue; }
	}

	/**
	 * Creates a new cached result set metadata object with no columns. After
	 * construction, columns can be added one at a time by calling
	 * {@link #addColumn(boolean, boolean, boolean, boolean, int, boolean, int, String, String, String, int, int, String, String, int, String, boolean, boolean, boolean, String)}
	 * .
	 */
	public ResultSetMetaDataCached()
	{
		_logger.debug("Creating new CachedResultSetMetaData");
		_entries = new ArrayList<ResultSetMetaDataCached.Entry>();
	}

//	/**
//	 * Works like the two-argument constructor with upcaseColumnNames set to false.
//	 */
//	public ResultSetMetaDataCached(ResultSetMetaData source, DbmsDataTypeResolver dataTypeResolver, String originDbms) throws SQLException
//	{
//		this(source, dataTypeResolver, originDbms, false);
//	}
//
//	/**
//	 * Creates a copy of the given ResultSetMetaData so that you can use it
//	 * without a live reference to the source result set or database.
//	 */
//	public ResultSetMetaDataCached(ResultSetMetaData source, DbmsDataTypeResolver dataTypeResolver, String originDbms, boolean upcaseColumnNames) throws SQLException
//	{
//		_logger.debug("Creating new CachedResultSetMetaData from origin Dbms '"+originDbms+"'.");
////		_dataTypeResolver  = dataTypeResolver;
//		_originDbms        = originDbms;
//		_upcaseColumnNames = upcaseColumnNames;
//
//		_entries = new ArrayList<ResultSetMetaDataCached.Entry>(source.getColumnCount());
//		
//		populate(source);
//	}

	/**
	 * Creates a copy of the given ResultSetMetaData (from a Source DBMS) so that you can use it without a live reference to the source result set or database.
	 * 
	 * @param source               ResultSet MetaData for a SOURCE DBMS (which later can be translated to any Target, using: <code>createNormalizedRsmd(productName)</code>
	 * @param originDbms           The name of the Source DBMS
	 * 
	 * @throws SQLException
	 */
	public ResultSetMetaDataCached(ResultSetMetaData source, String originDbms) 
	throws SQLException
	{
		this(source, originDbms, false);
	}

	/**
	 * Creates a copy of the given ResultSetMetaData (from a Source DBMS) so that you can use it without a live reference to the source result set or database.
	 * 
	 * @param source               ResultSet MetaData for a SOURCE DBMS (which later can be translated to any Target, using: <code>createNormalizedRsmd(productName)</code>
	 * @param originDbms           The name of the Source DBMS
	 * @param upcaseColumnNames    Should the column names be translated to UPPERCASE
	 * 
	 * @throws SQLException
	 */
	public ResultSetMetaDataCached(ResultSetMetaData source, String originDbms, boolean upcaseColumnNames) 
	throws SQLException
	{
		_logger.debug("Creating new CachedResultSetMetaData from origin Dbms '" + originDbms + "'.");
//		_dataTypeResolver  = dataTypeResolver;
		_originDbms        = originDbms;
		_upcaseColumnNames = upcaseColumnNames;

		_entries = new ArrayList<ResultSetMetaDataCached.Entry>(source.getColumnCount());
		
		populate(source);
	}

	/**
	 * Creates a copy of the given ResultSetMetaData (from a Source DBMS) so that you can use it without a live reference to the source result set or database.
	 * 
	 * @param sourceRs           ResultSet from a Source DBMS (the DBMS Vendors name will be taken from the underlying Connection of the ResultSet)
	 * @throws SQLException
	 */
	public ResultSetMetaDataCached(ResultSet sourceRs) throws SQLException
	{
		this(sourceRs, null);
	}
	/**
	 * Creates a copy of the given ResultSetMetaData (from a Source DBMS) so that you can use it without a live reference to the source result set or database.
	 * 
	 * @param sourceRs           ResultSet from a Source DBMS (the DBMS Vendors name will be taken from the underlying Connection of the ResultSet)
	 * @param sourceRsmd         Can be null (if it is null, we simply call: <code>sourceRs.getMetaData()</code> to grab it.
	 * @throws SQLException
	 */
	public ResultSetMetaDataCached(ResultSet sourceRs, ResultSetMetaData sourceRsmd) throws SQLException
	{
		if (sourceRs.getStatement() != null)
		{
			try
			{
				Connection conn = sourceRs.getStatement().getConnection();
				_originDbms = conn.getMetaData().getDatabaseProductName();
			}
			catch (SQLException ignore) {}
		}
		
		if (sourceRsmd == null)
			sourceRsmd = sourceRs.getMetaData();
		
		_logger.debug("Creating new CachedResultSetMetaData from origin Dbms '"+_originDbms+"'.");

		_entries = new ArrayList<ResultSetMetaDataCached.Entry>(sourceRsmd.getColumnCount());

		populate(sourceRsmd);
	}

	/**
	 * Copy another ResultSetMetaDataCached
	 * @param sourceRsmd
	 */
	public ResultSetMetaDataCached(ResultSetMetaDataCached sourceRsmd)
	{
		_entries = new ArrayList<ResultSetMetaDataCached.Entry>(sourceRsmd.getColumnCount());
		
		try 
		{ 
			populate(sourceRsmd);
		}
		catch (SQLException ignore)
		{
			// ResultSetMetaDataCached do NOT throw SQLException on populate() only if we have ResultSetMetaData
		}
	}

	/** In here we can do some "sanity" checking before we add records */
	private void addEntry(Entry entry)
	{
		_entries.add(entry);
	}

	protected void handlePopulateMetaDataException(String methodName, int colNum, SQLException sqle)
	{
		// Do a bit less logging for RsTune
		if (RsTune.APP_NAME.equals(Version.getAppName()))
		{
			if ( StringUtil.equalsAny(methodName, "isCaseSensitive", "isSearchable", "getColumnTypeName"))
				/* Skip some methods... */;
			else
				_logger.info("Problems reading ResultSetMetaData." + methodName + "(" + colNum + "). Caught: " + sqle);
		}
		else
		{
			_logger.info("Problems reading ResultSetMetaData." + methodName + "(" + colNum + "). Caught: " + sqle);
		}
	}

	protected void populate(ResultSetMetaData source) 
	throws SQLException
	{
		for (int i = 0; i < source.getColumnCount(); i++)
		{
			Entry entry = new Entry();
			int col = i + 1;

			// NOTE: source.isSearchable(), and source.getColumnTypeName() seems to be "heavy" or blocks the if we have a LARGE ResultSet (at least for Sybase ASE)
			//       check if there is something we can do about that???
			//       maybe NOT get them if not needed. (but if we do ResultSet MetaData translations with IDbmsDataTypeResolver and possibly IDbmsDdlResolver getColumnTypeName is used)
			try { entry._autoIncrement      = source.isAutoIncrement     (col); } catch(SQLException sqle) { entry._autoIncrement      = false;                                         handlePopulateMetaDataException("isAutoIncrement"     , col, sqle); }
			try { entry._caseSensitive      = source.isCaseSensitive     (col); } catch(SQLException sqle) { entry._caseSensitive      = false;                                         handlePopulateMetaDataException("isCaseSensitive"     , col, sqle); }
			try { entry._searchable         = source.isSearchable        (col); } catch(SQLException sqle) { entry._searchable         = false;                                         handlePopulateMetaDataException("isSearchable"        , col, sqle); }
			try { entry._currency           = source.isCurrency          (col); } catch(SQLException sqle) { entry._currency           = false;                                         handlePopulateMetaDataException("isCurrency"          , col, sqle); }
			try { entry._nullable           = source.isNullable          (col); } catch(SQLException sqle) { entry._nullable           = ResultSetMetaData.columnNullable;              handlePopulateMetaDataException("isNullable"          , col, sqle); }
			try { entry._signed             = source.isSigned            (col); } catch(SQLException sqle) { entry._signed             = false;                                         handlePopulateMetaDataException("isSigned"            , col, sqle); }
			try { entry._columnDisplaySize  = source.getColumnDisplaySize(col); } catch(SQLException sqle) { entry._columnDisplaySize  = 30;                                            handlePopulateMetaDataException("getColumnDisplaySize", col, sqle); }
			try { entry._columnLabel        = source.getColumnLabel      (col); } catch(SQLException sqle) { entry._columnLabel        = "unknown";                                     handlePopulateMetaDataException("getColumnLabel"      , col, sqle); }
			try { entry._columnName         = source.getColumnName       (col); } catch(SQLException sqle) { entry._columnName         = "unknown";                                     handlePopulateMetaDataException("getColumnName"       , col, sqle); }
			try { entry._schemaName         = source.getSchemaName       (col); } catch(SQLException sqle) { entry._schemaName         = "";                                            handlePopulateMetaDataException("getSchemaName"       , col, sqle); }
			try { entry._precision          = source.getPrecision        (col); } catch(SQLException sqle) { entry._precision          = 0;                                             handlePopulateMetaDataException("getPrecision"        , col, sqle); }
			try { entry._scale              = source.getScale            (col); } catch(SQLException sqle) { entry._scale              = 0;                                             handlePopulateMetaDataException("getScale"            , col, sqle); }
			try { entry._tableName          = source.getTableName        (col); } catch(SQLException sqle) { entry._tableName          = "";                                            handlePopulateMetaDataException("getTableName"        , col, sqle); }
			try { entry._catalogName        = source.getCatalogName      (col); } catch(SQLException sqle) { entry._catalogName        = "";                                            handlePopulateMetaDataException("getCatalogName"      , col, sqle); }
			try { entry._columnType         = source.getColumnType       (col); } catch(SQLException sqle) { entry._columnType         = Types.OTHER;                                   handlePopulateMetaDataException("getColumnType"       , col, sqle); }
			try { entry._columnTypeName     = source.getColumnTypeName   (col); } catch(SQLException sqle) { entry._columnTypeName     = getSqlDatatypeFromJdbcType(entry._columnType); handlePopulateMetaDataException("getColumnTypeName"   , col, sqle); }
			try { entry._readOnly           = source.isReadOnly          (col); } catch(SQLException sqle) { entry._readOnly           = true;                                          handlePopulateMetaDataException("isReadOnly"          , col, sqle); }
			try { entry._writable           = source.isWritable          (col); } catch(SQLException sqle) { entry._writable           = false;                                         handlePopulateMetaDataException("isWritable"          , col, sqle); }
			try { entry._definitelyWritable = source.isDefinitelyWritable(col); } catch(SQLException sqle) { entry._definitelyWritable = false;                                         handlePopulateMetaDataException("isDefinitelyWritable", col, sqle); }
			try { entry._columnClassName    = source.getColumnClassName  (col); } catch(SQLException sqle) { entry._columnClassName    = "unknown";                                     handlePopulateMetaDataException("getColumnClassName"  , col, sqle); }

			if (source instanceof ResultSetMetaDataCached)
			{
				ResultSetMetaDataCached rsmdc = (ResultSetMetaDataCached) source;
				
				entry._fakedColumn             = rsmdc.isFakedColumn(col);
				entry._fakedColumnDefaultValue = rsmdc.getFakedColumnDefaultValue(col);
			}

			entry._columnPos = col;

			if ( _upcaseColumnNames && entry._columnName != null )
				entry._columnName = entry._columnName.toUpperCase();
			
			addEntry(entry);

//			remap(i);
		}
	}
	protected String getSqlDatatypeFromJdbcType(int jdbcType)
	{
		switch (jdbcType)
		{
		case java.sql.Types.BIT:           return "bit";
		case java.sql.Types.TINYINT:       return "tinyint";
		case java.sql.Types.SMALLINT:      return "smallint";
		case java.sql.Types.INTEGER:       return "int";
		case java.sql.Types.BIGINT:        return "bigint";
		case java.sql.Types.FLOAT:         return "float";
		case java.sql.Types.REAL:          return "real";
		case java.sql.Types.DOUBLE:        return "float";
		case java.sql.Types.NUMERIC:       return "numeric";
		case java.sql.Types.DECIMAL:       return "numeric";
		case java.sql.Types.CHAR:          return "char";
		case java.sql.Types.VARCHAR:       return "varchar";
		case java.sql.Types.LONGVARCHAR:   return "text";
		case java.sql.Types.DATE:          return "date";
		case java.sql.Types.TIME:          return "time";
		case java.sql.Types.TIMESTAMP:     return "datetime";
		case java.sql.Types.BINARY:        return "binary";
		case java.sql.Types.VARBINARY:     return "varbinary";
		case java.sql.Types.LONGVARBINARY: return "image";
//		case java.sql.Types.NULL:          return "";
//		case java.sql.Types.OTHER:         return "";
//		case java.sql.Types.JAVA_OBJECT:   return "";
//		case java.sql.Types.DISTINCT:      return "";
//		case java.sql.Types.STRUCT:        return "";
//		case java.sql.Types.ARRAY:         return "";
		case java.sql.Types.BLOB:          return "image";
		case java.sql.Types.CLOB:          return "text";
//		case java.sql.Types.REF:           return "";
//		case java.sql.Types.DATALINK:      return "";
		case java.sql.Types.BOOLEAN:       return "bit";

		//------------------------- JDBC 4.0 -----------------------------------
//		case java.sql.Types.ROWID:         return "";
		case java.sql.Types.NCHAR:         return "char";
		case java.sql.Types.NVARCHAR:      return "varchar";
		case java.sql.Types.LONGNVARCHAR:  return "text";
		case java.sql.Types.NCLOB:         return "text";
//		case java.sql.Types.SQLXML:        return "";

		//------------------------- UNHANDLED TYPES  ---------------------------
		default:
			return "unknown";
		}
	}


	/**
	 * Creates a new ResultSetMetaDataCached and normalized/transformed data types into ...
	 * 
	 * @param originRs  ResultSet
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public static ResultSetMetaDataCached createNormalizedRsmd(ResultSet originRs)
	throws SQLException
	{
		ResultSetMetaDataCached rsmdc = new ResultSetMetaDataCached(originRs);
		return createNormalizedRsmd(rsmdc, rsmdc.getDatabaseProductName());
	}

	/**
	 * Creates a new ResultSetMetaDataCached and normalized/transformed data types into ...
	 * 
	 * @param ResultSetMetaData  ResultSetMetaData
	 * @param dbmsProductName    The vendor we are normalizing for
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public static ResultSetMetaDataCached createNormalizedRsmd(ResultSetMetaData originRsmd, String dbmsProductName)
	throws SQLException
	{
		ResultSetMetaDataCached rsmdc = new ResultSetMetaDataCached(originRsmd, dbmsProductName);
		return createNormalizedRsmd(rsmdc, rsmdc.getDatabaseProductName());
	}

	/**
	 * Creates a new ResultSetMetaDataCached and normalized/transformed data types into ...
	 * 
	 * @param originRsmd   an origin ResultSet MetaData Cache that was created but not normalized.
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public static ResultSetMetaDataCached createNormalizedRsmd(ResultSetMetaDataCached originRsmd)
	{
		return createNormalizedRsmd(originRsmd, originRsmd.getDatabaseProductName());
	}
	/**
	 * Creates a new ResultSetMetaDataCached and normalized/transformed data types into ...
	 * 
	 * @param originRsmd   an origin ResultSet MetaData Cache that was created but not normalized.
	 * @param productName  Source DBMS Vendor names
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public static ResultSetMetaDataCached createNormalizedRsmd(ResultSetMetaDataCached originRsmd, String dbmsProductName)
	{
		ResultSetMetaDataCached newRsmd = new ResultSetMetaDataCached(originRsmd);
		
		newRsmd._state      = State.NORMALIZED;
		newRsmd._originDbms = dbmsProductName;
		
		IDbmsDataTypeResolver resolver = DbxConnection.createDbmsDataTypeResolver(dbmsProductName);
		for (int i=0; i<newRsmd.getColumnCount(); i++)
		{
			Entry entry = newRsmd._entries.get(i);
			
			// possibly change the passed entry
			resolver.dataTypeResolverForSource( entry );
		}
		
		return newRsmd;
	}

	/**
	 * Creates a new ResultSetMetaDataCached and normalized/transformed data types into ...
	 * 
	 * @param productName  Source DBMS Vendor names
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public ResultSetMetaDataCached createNormalizedRsmd(String dbmsProductName)
	{
		return createNormalizedRsmd(this, dbmsProductName);
	}

	/**
	 * Creates a new ResultSetMetaDataCached and normalized/transformed data types into ...<br>
	 * Simply calls: <code>createNormalizedRsmd(this, this.getDatabaseProductName())</code>
	 * 
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public ResultSetMetaDataCached createNormalizedRsmd()
	{
		return createNormalizedRsmd(this, this.getDatabaseProductName());
	}



	/**
	 * Creates a new ResultSetMetaDataCached and transform the data types into the desired DBMS Vendor
	 * 
	 * @param sourceRsmd   A ResultSet MetaData (that has been normalized from the source using <code>createNormalizedRsmd(...)</code>)  
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public static ResultSetMetaDataCached transformToTargetDbms(ResultSetMetaDataCached normalizedRsmdc)
	{
		return transformToTargetDbms(normalizedRsmdc, normalizedRsmdc.getDatabaseProductName());
	}

	/**
	 * Creates a new ResultSetMetaDataCached and transform the data types into the desired DBMS Vendor
	 * 
	 * @param sourceRsmd   A ResultSet MetaData (that has been normalized from the source using <code>createNormalizedRsmd(...)</code>)  
	 * @param productName  Target DBMS Vendor names
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public static ResultSetMetaDataCached transformToTargetDbms(ResultSetMetaDataCached sourceRsmd, String dbmsProductName)
	{
		if (sourceRsmd.isStateOrigin())
		{
			_logger.warn("WARNING: transformToTargetDbms(ResultSetMetaDataCached sourceRsmd.dbmsProdName='"+sourceRsmd.getDatabaseProductName()+"', String dbmsProductName='"+dbmsProductName+"'): sourceRsmd is NOT YET NORMALIZED at the Source DBMS. I'll try to do that FIRST.");
			sourceRsmd = sourceRsmd.createNormalizedRsmd();
		}

		if (sourceRsmd.isStateNormalized())
		{
			// OK: This is what we expected
		}

		// No need to translate it again...
		if (sourceRsmd.isStateTranslated())
		{
			if (dbmsProductName != null && dbmsProductName.equals(sourceRsmd.getDatabaseProductName()))
				return sourceRsmd;
		}
		
		ResultSetMetaDataCached newRsmd = new ResultSetMetaDataCached(sourceRsmd);

		newRsmd._state      = State.TRANSLATED;
		newRsmd._originDbms = dbmsProductName;
		
		IDbmsDataTypeResolver resolver = DbxConnection.createDbmsDataTypeResolver(dbmsProductName);
		for (int i=0; i<newRsmd.getColumnCount(); i++)
		{
			Entry entry = newRsmd._entries.get(i);
			
			// possibly changes the passed entry
			resolver.dataTypeResolverToTarget( entry );
		}
		
		return newRsmd;
	}

	/**
	 * Creates a new ResultSetMetaDataCached and transform the data types into the desired DBMS Vendor
	 * 
	 * @param productName  Target DBMS Vendor names
	 * @return a new transformed ResultSetMetaDataCached
	 */
	public ResultSetMetaDataCached transformToTargetDbms(String dbmsProductName)
	{
		return transformToTargetDbms(this, dbmsProductName);
	}

	
	
	
	
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

	/**
	 * Adds a new column to the metadata. Feel free to supply whatever values
	 * you want for the various properties. They don't matter to internal
	 * CachedRowSet code.
	 *
	 * <p>
	 * I am aware that this method signature sucks, but what would you have me
	 * do? Implement setters for each property?
	 */
	public int addColumn(boolean autoIncrement, boolean caseSensitive, boolean searchable, boolean currency, int nullable, boolean signed, int columnDisplaySize, String columnLabel, String columnName, String schemaName, int precision, int scale, String tableName, String catalogName, int columnType, String columnTypeName, boolean readOnly, boolean writable, boolean definitelyWritable, String columnClassName) 
	throws SQLException
	{
		Entry entry = new Entry();

		entry._autoIncrement      = autoIncrement     ; 
		entry._caseSensitive      = caseSensitive     ; 
		entry._searchable         = searchable        ; 
		entry._currency           = currency          ; 
		entry._nullable           = nullable          ; 
		entry._signed             = signed            ; 
		entry._columnDisplaySize  = columnDisplaySize ; 
		entry._columnLabel        = columnLabel       ; 
		entry._columnName         = columnName        ; 
		entry._schemaName         = schemaName        ; 
		entry._precision          = precision         ; 
		entry._scale              = scale             ; 
		entry._tableName          = tableName         ; 
		entry._catalogName        = catalogName       ; 
		entry._columnType         = columnType        ; 
		entry._columnTypeName     = columnTypeName    ; 
		entry._readOnly           = readOnly          ; 
		entry._writable           = writable          ; 
		entry._definitelyWritable = definitelyWritable; 
		entry._columnClassName    = columnClassName   ; 

		entry._columnPos = _entries.size() + 1;

		if ( _upcaseColumnNames && entry._columnName != null )
			entry._columnName = entry._columnName.toUpperCase();
		
		addEntry(entry);

		return _entries.size();
	}

	/**
	 * Adds a new column to the metadata. 
	 */
	public int addColumn(String columnName, int columnType, String columnTypeName, boolean nullable, String columnClassName, int columnDisplaySize) 
	throws SQLException
	{
		return addColumn(columnName, columnType, columnTypeName, nullable, columnClassName, columnDisplaySize, columnDisplaySize, -1);
	}
	/**
	 * Adds a new column to the metadata. 
	 */
	public int addColumn(String columnName, int columnType, String columnTypeName, boolean nullable, String columnClassName, int columnDisplaySize, int precision, int scale) 
	throws SQLException
	{
		return addColumn(
				false,             // autoIncrement
				false,             // caseSensitive
				false,             // searchable
				false,             // currency
				(nullable ? ResultSetMetaData.columnNullable : ResultSetMetaData.columnNoNulls), // nullable
				false,             // signed
				columnDisplaySize, // columnDisplaySize
				columnName,        // columnLabel
				columnName,        // columnName
				"",                // schemaName
				precision,         // precision
				scale,             // scale
				"",                // tableName 
				"",                // catalogName 
				columnType,        // columnType
				columnTypeName,    // columnTypeName 
				true,              // readOnly
				false,             // writable
				false,             // definitelyWritable
				columnClassName);  // columnClassName
	}

	// ==========================================
	// Extended methods
	// ==========================================
	/** returns the position (with index start at 1) */
	public int findColumn(String colName)
	{
		for (int i=0; i<_entries.size(); i++)
		{
			Entry e = _entries.get(i);
			if (colName.equalsIgnoreCase(e.getColumnLabel()))
				return i + 1;
		}
		return -1;
	}

//	/** (with index start at 1) */
//	public void setColumnDisplaySize(int column, int newLen)
//	{
//		_entries.get( column - 1 )._columnDisplaySize = newLen;
//	}

	/**
	 * Get internal entry with details.
	 * @param column  Column Position (with index start at 1) 
	 * @return
	 */
	public Entry getEntry(int column)
	{
		return _entries.get(column - 1);
	}

	/**
	 * Get internal entry with details.
	 * @param colName  Column Name 
	 * @return the Entry if found, null if NOT Found
	 */
	public Entry getEntry(String colName)
	{
		int pos = findColumn(colName);
		if (pos == -1)
			return null;
		
		return getEntry(pos);
	}

	public List<Entry> getEntries()
	{
		return _entries;
	}
	
	
	/**
	 * Get column names (actually LABELS) in a List
	 * @return
	 */
	public List<String> getColumnNames()
	{
		List<String> list = new ArrayList<>();

		for (Entry entry : _entries)
			list.add(entry.getColumnLabel());

		return list;
	}

	/**
	 * Get unique catalog names in a Set
	 * @return
	 */
	public Set<String> getCatalogNames()
	{
		Set<String> set = new HashSet<>();

		for (Entry entry : _entries)
			if (StringUtil.hasValue(entry.getCatalogName()))
				set.add(entry.getCatalogName());

		return set;
	}

	/**
	 * Get unique table names in a Set
	 * @return
	 */
	public Set<String> getTableNames()
	{
		Set<String> set = new HashSet<>();

		for (Entry entry : _entries)
			if (StringUtil.hasValue(entry.getTableName()))
				set.add(entry.getTableName());

		return set;
	}

	/**
	 * Get unique schema names in a Set
	 * @return
	 */
	public Set<String> getSchemaNames()
	{
		Set<String> set = new HashSet<>();

		for (Entry entry : _entries)
			if (StringUtil.hasValue(entry.getSchemaName()))
				set.add(entry.getSchemaName());

		return set;
	}
	
	/**
	 * Get unique [schema.]table names in a Set<br>
	 * If schema name isn't set, then the schemaName and the '.' will be left out
	 * 
	 * @param skipEmpty  if both schema and table name is blank... do NOT add that entry to the output
	 * @return
	 */
	public Set<String> getSchemaTableNames(boolean skipEmpty)
	{
		Set<String> set = new HashSet<>();

		for (Entry entry : _entries)
		{
			String schemaName = entry.getTableName();
			String tableName  = entry.getSchemaName();

			// Skip empty ones
			if (skipEmpty && StringUtil.isNullOrBlank(schemaName) && StringUtil.isNullOrBlank(tableName))
				continue;

			if (StringUtil.hasValue(schemaName))
				set.add(schemaName + "." + tableName);
			else
				set.add(tableName);
		}

		return set;
	}
	
	public boolean isFakedColumn (int column)               { return _entries.get( column - 1 ).isFakedColumn(); }
	public void    setFakedColumn(int column, boolean fakedColumn) { _entries.get( column - 1 ).setFakedColumn( fakedColumn ); }

	public Object getFakedColumnDefaultValue(int column)               { return _entries.get( column - 1 ).getFakedColumnDefaultValue(); }
	public void   setFakedColumnDefaultValue(int column, Object defaultValue) { _entries.get( column - 1 ).setFakedColumnDefaultValue( defaultValue ); }


	public void setAutoIncrement     (int column, boolean autoIncrement     ) { _entries.get( column - 1 ).setAutoIncrement      ( autoIncrement     ); }
	public void setCaseSensitive     (int column, boolean caseSensitive     ) { _entries.get( column - 1 ).setCaseSensitive      ( caseSensitive     ); }
	public void setSearchable        (int column, boolean searchable        ) { _entries.get( column - 1 ).setSearchable         ( searchable        ); }
	public void setCurrency          (int column, boolean currency          ) { _entries.get( column - 1 ).setCurrency           ( currency          ); }
	public void setNullable          (int column, int     nullable          ) { _entries.get( column - 1 ).setNullable           ( nullable          ); }
	public void setSigned            (int column, boolean signed            ) { _entries.get( column - 1 ).setSigned             ( signed            ); }
	public void setColumnDisplaySize (int column, int     columnDisplaySize ) { _entries.get( column - 1 ).setColumnDisplaySize  ( columnDisplaySize ); }
	public void setColumnLabel       (int column, String  columnLabel       ) { _entries.get( column - 1 ).setColumnLabel        ( columnLabel       ); }
	public void setColumnName        (int column, String  columnName        ) { _entries.get( column - 1 ).setColumnName         ( columnName        ); }
	public void setSchemaName        (int column, String  schemaName        ) { _entries.get( column - 1 ).setSchemaName         ( schemaName        ); }
	public void setPrecision         (int column, int     precision         ) { _entries.get( column - 1 ).setPrecision          ( precision         ); }
	public void setScale             (int column, int     scale             ) { _entries.get( column - 1 ).setScale              ( scale             ); }
	public void setTableName         (int column, String  tableName         ) { _entries.get( column - 1 ).setTableName          ( tableName         ); }
	public void setCatalogName       (int column, String  catalogName       ) { _entries.get( column - 1 ).setCatalogName        ( catalogName       ); }
	public void setColumnType        (int column, int     columnType        ) { _entries.get( column - 1 ).setColumnType         ( columnType        ); }
	public void setColumnTypeName    (int column, String  columnTypeName    ) { _entries.get( column - 1 ).setColumnTypeName     ( columnTypeName    ); }
	public void setReadOnly          (int column, boolean readOnly          ) { _entries.get( column - 1 ).setReadOnly           ( readOnly          ); }
	public void setWritable          (int column, boolean writable          ) { _entries.get( column - 1 ).setWritable           ( writable          ); }
	public void setDefinitelyWritable(int column, boolean definitelyWritable) { _entries.get( column - 1 ).setDefinitelyWritable ( definitelyWritable); }
	public void setColumnClassName   (int column, String  columnClassName   ) { _entries.get( column - 1 ).setColumnClassName    ( columnClassName   ); }
	
	// ==========================================
	// RESULT SET META DATA INTERFACE
	// ==========================================
	@Override public int     getColumnCount()                 { return _entries.size(); }
	@Override public boolean isAutoIncrement     (int column) { return _entries.get( column - 1 ).isAutoIncrement     (); }
	@Override public boolean isCaseSensitive     (int column) { return _entries.get( column - 1 ).isCaseSensitive     (); }
	@Override public boolean isSearchable        (int column) { return _entries.get( column - 1 ).isSearchable        (); }
	@Override public boolean isCurrency          (int column) { return _entries.get( column - 1 ).isCurrency          (); }
	@Override public int     isNullable          (int column) { return _entries.get( column - 1 ).isNullable          () ? ResultSetMetaData.columnNullable : ResultSetMetaData.columnNoNulls; }
	          public boolean isNullableBoolean   (int column) { return _entries.get( column - 1 ).isNullable          (); }
	@Override public boolean isSigned            (int column) { return _entries.get( column - 1 ).isSigned            (); }
	@Override public int     getColumnDisplaySize(int column) { return _entries.get( column - 1 ).getColumnDisplaySize(); }
	@Override public String  getColumnLabel      (int column) { return _entries.get( column - 1 ).getColumnLabel      (); }
	@Override public String  getColumnName       (int column) { return _entries.get( column - 1 ).getColumnName       (); }
	@Override public String  getSchemaName       (int column) { return _entries.get( column - 1 ).getSchemaName       (); }
	@Override public int     getPrecision        (int column) { return _entries.get( column - 1 ).getPrecision        (); }
	@Override public int     getScale            (int column) { return _entries.get( column - 1 ).getScale            (); }
	@Override public String  getTableName        (int column) { return _entries.get( column - 1 ).getTableName        (); }
	@Override public String  getCatalogName      (int column) { return _entries.get( column - 1 ).getCatalogName      (); }
	@Override public int     getColumnType       (int column) { return _entries.get( column - 1 ).getColumnType       (); }
	@Override public String  getColumnTypeName   (int column) { return _entries.get( column - 1 ).getColumnTypeName   (); }
	@Override public boolean isReadOnly          (int column) { return _entries.get( column - 1 ).isReadOnly          (); }
	@Override public boolean isWritable          (int column) { return _entries.get( column - 1 ).isWritable          (); }
	@Override public boolean isDefinitelyWritable(int column) { return _entries.get( column - 1 ).isDefinitelyWritable(); }
	@Override public String  getColumnClassName  (int column) { return _entries.get( column - 1 ).getColumnClassName  (); }
	
	          // Below is NOT part of the interface
	          public String  getColumnTypeStr        (int column) { return DataTypeNotResolvedException.getJdbcTypeAsString(_entries.get( column - 1 )._columnType); }
	          public Object getColumnResolvedTypeName(int column) { return _entries.get( column - 1 )._columnResolvedTypeName; }


	@Override 
	public boolean isWrapperFor(Class<?> arg0) 
	{
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3."); 
	}
	
	@Override 
	public <T> T unwrap(Class<T> arg0) 
	{ 
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3."); 
	}


	// ==========================================
	// And some convenient methods
	// ==========================================
	public int getPrecision(String colName, int def)
	{
		int precision = -1;

		Entry entry = getEntry(colName);
		if (entry != null)
		{
			precision = entry.getPrecision();
			if (precision <= 0)
				precision = def;

			return precision;
		}

		return def;
	}

	
	/**
	 * Like DatabaseMetaData.getColumns(...) but instead of using that it simply does a <code>SELECT * FROM [catalog].[schema].[tabname] WHERE 1=2</code> to get a ResultSetMetaData object.
	 * <br>
	 * Note: 
	 * <ul>
	 *    <li>the '[' and ']' chars will be replaced with the Quoted Identifier Character for the DBMS Vendor</li>
	 *    <li>If catalog parameter is <b>not</b> specified, it will be removed from the above SELECT Statement.</li>
	 *    <li>If schema parameter is <b>not</b> specified, it will be removed from the above SELECT Statement.</li>
	 *    <li>Special Case: If catalog parameter <b>is</b> specified and schema parameter is <b>not</b> specified, An empty/default schema name will be added hoping the DBMS will resolve it to "default" schema name. SQL will be <code>SELECT * FROM [catalog]..[tabname] WHERE 1=2</code></li>	
	 * </ul> 
	 * 
	 * @param conn         Connection to DBMS
	 * @param catalog      Name of the Catalog/database, null or "" and we wont use it
	 * @param schema       Name of the schema, null or "" and we wont use it
	 * @param table        Name of the table to get info from
	 * @return
	 */
	public static ResultSetMetaDataCached getMetaData(DbxConnection conn, String catalog, String schema, String table)
	{
		String catName = StringUtil.isNullOrBlank(catalog) ? "" : "[" + catalog + "].";
		String schName = StringUtil.isNullOrBlank(schema)  ? "" : "[" + schema  + "].";
		String tabName = "[" + table + "]";

		// special case: If we have 'catalog' but NOT 'schema', then add "empty" schema and hope it will resolve to "default" schema name
		// For Sybase and SQL Server it will work (the default is dbo), But for "other" DBMS Vendors, I don't know 
		if (StringUtil.hasValue(catName) && StringUtil.isNullOrBlank(schema))
			schName = ".";
		
		String sql = conn.quotifySqlString("SELECT * FROM " + catName + schName + tabName + " WHERE 1 = 2");
		
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			ResultSetMetaDataCached rsmdc = new ResultSetMetaDataCached(rs);

			// Read the RS... it should have 0 rows... but anyway...
			while (rs.next())
				;

			return rsmdc;
		}
		catch (SQLException ex)
		{
			_logger.warn("Problems getting ResultSetMetaData for catalog=" + catalog + ", schema=" + schema + ", table=" + table + ". Used SQL=|" + sql + "|.", ex);
			return null;
		}
	}
	
	
	
	
	public String debugPrint()
	{
		try
		{
			ResultSetTableModel rstm = new ResultSetTableModel(toResultSet(), "debugPrint");
			return rstm.toAsciiTableString();
		}
		catch (SQLException ex)
		{
			return "" + ex;
		}
	}
	
	
	public ResultSet toResultSet()
	{
		SimpleResultSet rs = new SimpleResultSet();
		rs.addColumn("autoIncrement",          Types.BIT,        0, 0);
		rs.addColumn("caseSensitive",          Types.BIT,        0, 0);
		rs.addColumn("searchable",             Types.BIT,        0, 0);
		rs.addColumn("currency",               Types.BIT,        0, 0);
		rs.addColumn("nullable",               Types.INTEGER,    0, 0);
		rs.addColumn("signed",                 Types.BIT,        0, 0);
		rs.addColumn("columnDisplaySize",      Types.INTEGER,   80, 0);
		rs.addColumn("columnLabel",            Types.VARCHAR,   80, 0);
		rs.addColumn("columnName",             Types.VARCHAR,   80, 0);
		rs.addColumn("schemaName",             Types.VARCHAR,   80, 0);
		rs.addColumn("precision",              Types.INTEGER,    0, 0);
		rs.addColumn("scale",                  Types.INTEGER,    0, 0);
		rs.addColumn("tableName",              Types.VARCHAR,   80, 0);
		rs.addColumn("catalogName",            Types.VARCHAR,   80, 0);
		rs.addColumn("columnType",             Types.INTEGER,    0, 0);
		rs.addColumn("columnTypeName",         Types.VARCHAR,   80, 0);
		rs.addColumn("readOnly",               Types.BIT,        0, 0);
		rs.addColumn("writable",               Types.BIT,        0, 0);
		rs.addColumn("definitelyWritable",     Types.BIT,        0, 0);
		rs.addColumn("columnClassName",        Types.VARCHAR,   80, 0);
		rs.addColumn("columnPos",              Types.INTEGER,    0, 0);
		rs.addColumn("columnResolvedTypeName", Types.VARCHAR,   80, 0);
		rs.addColumn("changeBitMap",           Types.INTEGER,    0, 0);

		for (Entry entry : _entries)
		{
			rs.addRow(
					 entry._autoIncrement
					,entry._caseSensitive
					,entry._searchable
					,entry._currency
					,entry._nullable
					,entry._signed
					,entry._columnDisplaySize
					,entry._columnLabel
					,entry._columnName
					,entry._schemaName
					,entry._precision
					,entry._scale
					,entry._tableName
					,entry._catalogName
					,entry._columnType
					,entry._columnTypeName
					,entry._readOnly
					,entry._writable
					,entry._definitelyWritable
					,entry._columnClassName
					,entry._columnPos
					,entry._columnResolvedTypeName
					,entry._changeBitMap
			);
		}

		return rs;
	}
}
