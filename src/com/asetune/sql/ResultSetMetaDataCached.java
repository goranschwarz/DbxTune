package com.asetune.sql;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.DbUtils;

/**
 * A class that implements ResultSetMetaData by making a copy of all the data from another ResultSetMetaData object. 
 */
public class ResultSetMetaDataCached implements ResultSetMetaData, java.io.Serializable, Cloneable
{
	private static final long   serialVersionUID = 1L;
	private static final Logger	_logger	= Logger.getLogger(ResultSetMetaDataCached.class);

	/**
	 * A user preference: If set true, this class will convert column names
	 * upper case for you. It doesn't affect any of the other strings.
	 */
	private boolean  _upcaseColumnNames;

	private int      _columnCount;
	private boolean  _autoIncrement[];
	private boolean  _caseSensitive[];
	private boolean  _searchable[];
	private boolean  _currency[];
	private int      _nullable[];
	private boolean  _signed[];
	private int      _columnDisplaySize[];
	private String   _columnLabel[];
	private String   _columnName[];
	private String   _schemaName[];
	private int      _precision[];
	private int      _scale[];
	private String   _tableName[];
	private String   _catalogName[];
	private int      _columnType[];
	private String   _columnTypeName[];
	private boolean  _readOnly[];
	private boolean  _writable[];
	private boolean  _definitelyWritable[];
	private String   _columnClassName[];

	private String   _originDbms;

	/**
	 * Creates a new cached resultset metadata object with no columns. After
	 * construction, columns can be added one at a time by calling
	 * {@link #addColumn(boolean, boolean, boolean, boolean, int, boolean, int, String, String, String, int, int, String, String, int, String, boolean, boolean, boolean, String)}
	 * .
	 */
	public ResultSetMetaDataCached()
	{
		_logger.debug("Creating new CachedResultSetMetaData");
		_columnCount = 0;
		createArrays(_columnCount);
	}

	/**
	 * Works like the two-argument constructor with upcaseColumnNames set to false.
	 */
	public ResultSetMetaDataCached(ResultSetMetaData source, String originDbms) throws SQLException
	{
		this(source, originDbms, false);
	}

	/**
	 * Creates a copy of the given ResultSetMetaData so that you can use it
	 * without a live reference to the source result set or database.
	 */
	public ResultSetMetaDataCached(ResultSetMetaData source, String originDbms, boolean upcaseColumnNames) throws SQLException
	{
		_logger.debug("Creating new CachedResultSetMetaData from origin Dbms '"+originDbms+"'.");
		_originDbms        = originDbms;
		_upcaseColumnNames = upcaseColumnNames;
		_columnCount       = source.getColumnCount();
		createArrays(_columnCount);
		populate(source);
	}

	protected void createArrays(int columnCount)
	{
		_autoIncrement      = new boolean[columnCount];
		_caseSensitive      = new boolean[columnCount];
		_searchable         = new boolean[columnCount];
		_currency           = new boolean[columnCount];
		_nullable           = new int    [columnCount];
		_signed             = new boolean[columnCount];
		_columnDisplaySize  = new int    [columnCount];
		_columnLabel        = new String [columnCount];
		_columnName         = new String [columnCount];
		_schemaName         = new String [columnCount];
		_precision          = new int    [columnCount];
		_scale              = new int    [columnCount];
		_tableName          = new String [columnCount];
		_catalogName        = new String [columnCount];
		_columnType         = new int    [columnCount];
		_columnTypeName     = new String [columnCount];
		_readOnly           = new boolean[columnCount];
		_writable           = new boolean[columnCount];
		_definitelyWritable = new boolean[columnCount];
		_columnClassName    = new String [columnCount];
	}

	protected void populate(ResultSetMetaData source) throws SQLException
	{
		for (int i = 0; i < source.getColumnCount(); i++)
		{
			_autoIncrement     [i] = source.isAutoIncrement     (i + 1);
			_caseSensitive     [i] = source.isCaseSensitive     (i + 1);
			_searchable        [i] = source.isSearchable        (i + 1);
			_currency          [i] = source.isCurrency          (i + 1);
			_nullable          [i] = source.isNullable          (i + 1);
			_signed            [i] = source.isSigned            (i + 1);
			_columnDisplaySize [i] = source.getColumnDisplaySize(i + 1);
			_columnLabel       [i] = source.getColumnLabel      (i + 1);
			_columnName        [i] = source.getColumnName       (i + 1);
			_schemaName        [i] = source.getSchemaName       (i + 1);
			_precision         [i] = source.getPrecision        (i + 1);
			_scale             [i] = source.getScale            (i + 1);
			_tableName         [i] = source.getTableName        (i + 1);
			_catalogName       [i] = source.getCatalogName      (i + 1);
			_columnType        [i] = source.getColumnType       (i + 1);
			_columnTypeName    [i] = source.getColumnTypeName   (i + 1);
			_readOnly          [i] = source.isReadOnly          (i + 1);
			_writable          [i] = source.isWritable          (i + 1);
			_definitelyWritable[i] = source.isDefinitelyWritable(i + 1);
			_columnClassName   [i] = source.getColumnClassName  (i + 1);

			if ( _upcaseColumnNames && _columnName[i] != null )
				_columnName[i] = _columnName[i].toUpperCase();
			
			remap(i);
		}
	}

	/**
	 * TODO 1: get a "remap" instance from "somewhere" which should be responsible for this based on the DBMS instance  
	 * TODO 2: The PCS Writer should also have some kind of remap functionality if we want to store it as something different... but have a look at this later...  
	 * @param index
	 */
	protected void remap(int index)
	{
		// If we do not have a PCS do not bather to transform data types
		if ( PersistentCounterHandler.getInstance() == null)
			return;

		//------------------------------------------------------------------------
		// SOURCE: Sybase ASE
		//------------------------------------------------------------------------
		if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_SYBASE_ASE))
		{
			if ("unsigned smallint".equalsIgnoreCase(_columnTypeName[index]))
			{
				_columnType    [index] = Types.INTEGER;
				_columnTypeName[index] = "int";
			}
			
			if ("unsigned int".equalsIgnoreCase(_columnTypeName[index]))
			{
				_columnType    [index] = Types.BIGINT;
				_columnTypeName[index] = "bigint";
			}
			
//			if ("unsigned bigint".equalsIgnoreCase(_columnTypeName[index]))
//			{
//				_columnType    [index] = Types.BIGINT;
//				_columnTypeName[index] = "bigint";
//			}
		}
		//------------------------------------------------------------------------
		// SOURCE: Microsoft SQL-Server
		//------------------------------------------------------------------------
		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_MSSQL))
		{
			if ("uniqueidentifier".equalsIgnoreCase(_columnTypeName[index]))
			{
				_columnType    [index] = Types.CHAR;
				_columnTypeName[index] = "char";
			}

			if ("xml".equalsIgnoreCase(_columnTypeName[index]))
			{
				_columnType    [index] = Types.LONGVARCHAR;
				_columnTypeName[index] = "text";
			}
		}
		//------------------------------------------------------------------------
		// SOURCE: Oracle
		//------------------------------------------------------------------------
		else if (DbUtils.isProductName(_originDbms, DbUtils.DB_PROD_NAME_ORACLE))
		{
			// We need to remap some of those strange numeric -127 or whatever they are...
			if (_columnType[index] == Types.NUMERIC && (_scale[index] == 0 || _scale[index] == -127) && (_precision[index] <= 9 || _precision[index] == 38))
			{
				_columnType    [index] = Types.INTEGER;
				_columnTypeName[index] = "int";
			}
		}
	}

	/**
	 * Adds a new column to the metadata. Feel free to supply whatever values
	 * you want for the various properties. They don't matter to internal
	 * CachedRowSet code.
	 *
	 * <p>
	 * I am aware that this method signature sucks, but what would you have me
	 * do? Implement setters for each property?
	 */
	public int addColumn(boolean autoIncrement, boolean caseSensitive, boolean searchable, boolean currency, int nullable, boolean signed, int columnDisplaySize, String columnLabel, String columnName, String schemaName, int precision, int scale, String tableName, String catalogName, int columnType, String columnTypeName, boolean readOnly, boolean writable, boolean definitelyWritable, String columnClassName) throws SQLException
	{

		ResultSetMetaData copy = null;
		try
		{
			copy = (ResultSetMetaData) this.clone();
		}
		catch (CloneNotSupportedException e)
		{
			throw new IllegalStateException("Couldn't clone this instance");
		}
		createArrays(_columnCount + 1);
		populate(copy);

		_autoIncrement     [_columnCount] = autoIncrement;
		_caseSensitive     [_columnCount] = caseSensitive;
		_searchable        [_columnCount] = searchable;
		_currency          [_columnCount] = currency;
		_nullable          [_columnCount] = nullable;
		_signed            [_columnCount] = signed;
		_columnDisplaySize [_columnCount] = columnDisplaySize;
		_columnLabel       [_columnCount] = columnLabel;
		_columnName        [_columnCount] = columnName;
		_schemaName        [_columnCount] = schemaName;
		_precision         [_columnCount] = precision;
		_scale             [_columnCount] = scale;
		_tableName         [_columnCount] = tableName;
		_catalogName       [_columnCount] = catalogName;
		_columnType        [_columnCount] = columnType;
		_columnTypeName    [_columnCount] = columnTypeName;
		_readOnly          [_columnCount] = readOnly;
		_writable          [_columnCount] = writable;
		_definitelyWritable[_columnCount] = definitelyWritable;
		_columnClassName   [_columnCount] = columnClassName;

		if ( _upcaseColumnNames && _columnName[_columnCount] != null )
			_columnName[_columnCount] = _columnName[_columnCount].toUpperCase();

		_columnCount += 1;
		return _columnCount;
	}

	// ==========================================
	// RESULT SET META DATA INTERFACE
	// ==========================================
	@Override
	public int getColumnCount() throws SQLException
	{
		return _columnCount;
	}

	@Override
	public boolean isAutoIncrement(int column) throws SQLException
	{
		return _autoIncrement[column - 1];
	}

	@Override
	public boolean isCaseSensitive(int column) throws SQLException
	{
		return _caseSensitive[column - 1];
	}

	@Override
	public boolean isSearchable(int column) throws SQLException
	{
		return _searchable[column - 1];
	}

	@Override
	public boolean isCurrency(int column) throws SQLException
	{
		return _currency[column - 1];
	}

	@Override
	public int isNullable(int column) throws SQLException
	{
		return _nullable[column - 1];
	}

	@Override
	public boolean isSigned(int column) throws SQLException
	{
		return _signed[column - 1];
	}

	@Override
	public int getColumnDisplaySize(int column) throws SQLException
	{
		return _columnDisplaySize[column - 1];
	}

	@Override
	public String getColumnLabel(int column) throws SQLException
	{
		return _columnLabel[column - 1];
	}

	@Override
	public String getColumnName(int column) throws SQLException
	{
		return _columnName[column - 1];
	}

	@Override
	public String getSchemaName(int column) throws SQLException
	{
		return _schemaName[column - 1];
	}

	@Override
	public int getPrecision(int column) throws SQLException
	{
		return _precision[column - 1];
	}

	@Override
	public int getScale(int column) throws SQLException
	{
		return _scale[column - 1];
	}

	@Override
	public String getTableName(int column) throws SQLException
	{
		return _tableName[column - 1];
	}

	@Override
	public String getCatalogName(int column) throws SQLException
	{
		return _catalogName[column - 1];
	}

	@Override
	public int getColumnType(int column) throws SQLException
	{
		return _columnType[column - 1];
	}

	@Override
	public String getColumnTypeName(int column) throws SQLException
	{
		return _columnTypeName[column - 1];
	}

	@Override
	public boolean isReadOnly(int column) throws SQLException
	{
		return _readOnly[column - 1];
	}

	@Override
	public boolean isWritable(int column) throws SQLException
	{
		return _writable[column - 1];
	}

	@Override
	public boolean isDefinitelyWritable(int column) throws SQLException
	{
		return _definitelyWritable[column - 1];
	}

	@Override
	public String getColumnClassName(int column) throws SQLException
	{
		return _columnClassName[column - 1];
	}

	@Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException
	{
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException
	{
		throw new UnsupportedOperationException("Currently it is only possible to wrap JDBC 3.");
	}
}
