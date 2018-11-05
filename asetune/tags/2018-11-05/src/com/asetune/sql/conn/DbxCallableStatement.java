package com.asetune.sql.conn;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

import org.apache.log4j.Logger;

public class DbxCallableStatement implements CallableStatement
{
	private static Logger _logger = Logger.getLogger(DbxCallableStatement.class);

	public static DbxCallableStatement create(CallableStatement cstmnt)
	{
		if (cstmnt == null)
			throw new IllegalArgumentException("create(): cstmnt can't be null");

		return new DbxCallableStatement(cstmnt);
	}

	public DbxCallableStatement(CallableStatement cstmnt)
	{
		_cstmnt = cstmnt;
	}



	//#################################################################################
	//#################################################################################
	//### BEGIN: delegated methods
	//#################################################################################
	//#################################################################################
	protected CallableStatement _cstmnt;

	@Override
	public ResultSet executeQuery(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxCallableStatement.executeQuery(String): sql='"+sql+"'.");

		return _cstmnt.executeQuery(sql);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return _cstmnt.unwrap(iface);
	}

	@Override
	public ResultSet executeQuery() throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxCallableStatement.executeQuery(): none");

		return _cstmnt.executeQuery();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxCallableStatement.executeUpdate(String): sql='"+sql+"'.");
		
		return _cstmnt.executeUpdate(sql);
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException
	{
		_cstmnt.registerOutParameter(parameterIndex, sqlType);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return _cstmnt.isWrapperFor(iface);
	}

	@Override
	public int executeUpdate() throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxCallableStatement.executeUpdate(): none");
		
		return _cstmnt.executeUpdate();
	}

	@Override
	public void close() throws SQLException
	{
		_cstmnt.close();
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException
	{
		_cstmnt.setNull(parameterIndex, sqlType);
	}

	@Override
	public int getMaxFieldSize() throws SQLException
	{
		return _cstmnt.getMaxFieldSize();
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException
	{
		_cstmnt.registerOutParameter(parameterIndex, sqlType, scale);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException
	{
		_cstmnt.setBoolean(parameterIndex, x);
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException
	{
		_cstmnt.setMaxFieldSize(max);
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException
	{
		_cstmnt.setByte(parameterIndex, x);
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException
	{
		_cstmnt.setShort(parameterIndex, x);
	}

	@Override
	public int getMaxRows() throws SQLException
	{
		return _cstmnt.getMaxRows();
	}

	@Override
	public boolean wasNull() throws SQLException
	{
		return _cstmnt.wasNull();
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException
	{
		_cstmnt.setInt(parameterIndex, x);
	}

	@Override
	public void setMaxRows(int max) throws SQLException
	{
		_cstmnt.setMaxRows(max);
	}

	@Override
	public String getString(int parameterIndex) throws SQLException
	{
		return _cstmnt.getString(parameterIndex);
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException
	{
		_cstmnt.setLong(parameterIndex, x);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		_cstmnt.setEscapeProcessing(enable);
	}

	@Override
	public boolean getBoolean(int parameterIndex) throws SQLException
	{
		return _cstmnt.getBoolean(parameterIndex);
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException
	{
		_cstmnt.setFloat(parameterIndex, x);
	}

	@Override
	public int getQueryTimeout() throws SQLException
	{
		return _cstmnt.getQueryTimeout();
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException
	{
		_cstmnt.setDouble(parameterIndex, x);
	}

	@Override
	public byte getByte(int parameterIndex) throws SQLException
	{
		return _cstmnt.getByte(parameterIndex);
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException
	{
		_cstmnt.setQueryTimeout(seconds);
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
	{
		_cstmnt.setBigDecimal(parameterIndex, x);
	}

	@Override
	public short getShort(int parameterIndex) throws SQLException
	{
		return _cstmnt.getShort(parameterIndex);
	}

	@Override
	public void cancel() throws SQLException
	{
		_cstmnt.cancel();
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException
	{
		_cstmnt.setString(parameterIndex, x);
	}

	@Override
	public int getInt(int parameterIndex) throws SQLException
	{
		return _cstmnt.getInt(parameterIndex);
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return _cstmnt.getWarnings();
	}

	@Override
	public long getLong(int parameterIndex) throws SQLException
	{
		return _cstmnt.getLong(parameterIndex);
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException
	{
		_cstmnt.setBytes(parameterIndex, x);
	}

	@Override
	public float getFloat(int parameterIndex) throws SQLException
	{
		return _cstmnt.getFloat(parameterIndex);
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		_cstmnt.clearWarnings();
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException
	{
		_cstmnt.setDate(parameterIndex, x);
	}

	@Override
	public void setCursorName(String name) throws SQLException
	{
		_cstmnt.setCursorName(name);
	}

	@Override
	public double getDouble(int parameterIndex) throws SQLException
	{
		return _cstmnt.getDouble(parameterIndex);
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException
	{
		_cstmnt.setTime(parameterIndex, x);
	}

	@SuppressWarnings("deprecation")
	@Override
	public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException
	{
		return _cstmnt.getBigDecimal(parameterIndex, scale);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
	{
		_cstmnt.setTimestamp(parameterIndex, x);
	}

	@Override
	public boolean execute(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxCallableStatement.execute(String): sql='"+sql+"'.");
		
		return _cstmnt.execute(sql);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		_cstmnt.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public byte[] getBytes(int parameterIndex) throws SQLException
	{
		return _cstmnt.getBytes(parameterIndex);
	}

	@Override
	public Date getDate(int parameterIndex) throws SQLException
	{
		return _cstmnt.getDate(parameterIndex);
	}

	@Override
	public ResultSet getResultSet() throws SQLException
	{
		return _cstmnt.getResultSet();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		_cstmnt.setUnicodeStream(parameterIndex, x, length);
	}

	@Override
	public Time getTime(int parameterIndex) throws SQLException
	{
		return _cstmnt.getTime(parameterIndex);
	}

	@Override
	public int getUpdateCount() throws SQLException
	{
		return _cstmnt.getUpdateCount();
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex) throws SQLException
	{
		return _cstmnt.getTimestamp(parameterIndex);
	}

	@Override
	public boolean getMoreResults() throws SQLException
	{
		return _cstmnt.getMoreResults();
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		_cstmnt.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public Object getObject(int parameterIndex) throws SQLException
	{
		return _cstmnt.getObject(parameterIndex);
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException
	{
		_cstmnt.setFetchDirection(direction);
	}

	@Override
	public void clearParameters() throws SQLException
	{
		_cstmnt.clearParameters();
	}

	@Override
	public BigDecimal getBigDecimal(int parameterIndex) throws SQLException
	{
		return _cstmnt.getBigDecimal(parameterIndex);
	}

	@Override
	public int getFetchDirection() throws SQLException
	{
		return _cstmnt.getFetchDirection();
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
	{
		_cstmnt.setObject(parameterIndex, x, targetSqlType);
	}

	@Override
	public Object getObject(int parameterIndex, Map<String, Class<?>> map) throws SQLException
	{
		return _cstmnt.getObject(parameterIndex, map);
	}

	@Override
	public void setFetchSize(int rows) throws SQLException
	{
		_cstmnt.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException
	{
		return _cstmnt.getFetchSize();
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException
	{
		_cstmnt.setObject(parameterIndex, x);
	}

	@Override
	public Ref getRef(int parameterIndex) throws SQLException
	{
		return _cstmnt.getRef(parameterIndex);
	}

	@Override
	public int getResultSetConcurrency() throws SQLException
	{
		return _cstmnt.getResultSetConcurrency();
	}

	@Override
	public Blob getBlob(int parameterIndex) throws SQLException
	{
		return _cstmnt.getBlob(parameterIndex);
	}

	@Override
	public int getResultSetType() throws SQLException
	{
		return _cstmnt.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException
	{
		_cstmnt.addBatch(sql);
	}

	@Override
	public Clob getClob(int parameterIndex) throws SQLException
	{
		return _cstmnt.getClob(parameterIndex);
	}

	@Override
	public void clearBatch() throws SQLException
	{
		_cstmnt.clearBatch();
	}

	@Override
	public boolean execute() throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxCallableStatement.execute(): none");
		
		return _cstmnt.execute();
	}

	@Override
	public Array getArray(int parameterIndex) throws SQLException
	{
		return _cstmnt.getArray(parameterIndex);
	}

	@Override
	public int[] executeBatch() throws SQLException
	{
		return _cstmnt.executeBatch();
	}

	@Override
	public Date getDate(int parameterIndex, Calendar cal) throws SQLException
	{
		return _cstmnt.getDate(parameterIndex, cal);
	}

	@Override
	public void addBatch() throws SQLException
	{
		_cstmnt.addBatch();
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
	{
		_cstmnt.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public Time getTime(int parameterIndex, Calendar cal) throws SQLException
	{
		return _cstmnt.getTime(parameterIndex, cal);
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException
	{
		_cstmnt.setRef(parameterIndex, x);
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return _cstmnt.getConnection();
	}

	@Override
	public Timestamp getTimestamp(int parameterIndex, Calendar cal) throws SQLException
	{
		return _cstmnt.getTimestamp(parameterIndex, cal);
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException
	{
		_cstmnt.setBlob(parameterIndex, x);
	}

	@Override
	public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException
	{
		_cstmnt.registerOutParameter(parameterIndex, sqlType, typeName);
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException
	{
		_cstmnt.setClob(parameterIndex, x);
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException
	{
		return _cstmnt.getMoreResults(current);
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException
	{
		_cstmnt.setArray(parameterIndex, x);
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException
	{
		return _cstmnt.getMetaData();
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType) throws SQLException
	{
		_cstmnt.registerOutParameter(parameterName, sqlType);
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException
	{
		return _cstmnt.getGeneratedKeys();
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
	{
		_cstmnt.setDate(parameterIndex, x, cal);
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _cstmnt.executeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
	{
		_cstmnt.setTime(parameterIndex, x, cal);
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException
	{
		_cstmnt.registerOutParameter(parameterName, sqlType, scale);
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		return _cstmnt.executeUpdate(sql, columnIndexes);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
	{
		_cstmnt.setTimestamp(parameterIndex, x, cal);
	}

	@Override
	public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException
	{
		_cstmnt.registerOutParameter(parameterName, sqlType, typeName);
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException
	{
		_cstmnt.setNull(parameterIndex, sqlType, typeName);
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException
	{
		return _cstmnt.executeUpdate(sql, columnNames);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _cstmnt.execute(sql, autoGeneratedKeys);
	}

	@Override
	public URL getURL(int parameterIndex) throws SQLException
	{
		return _cstmnt.getURL(parameterIndex);
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException
	{
		_cstmnt.setURL(parameterIndex, x);
	}

	@Override
	public void setURL(String parameterName, URL val) throws SQLException
	{
		_cstmnt.setURL(parameterName, val);
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException
	{
		return _cstmnt.getParameterMetaData();
	}

	@Override
	public void setNull(String parameterName, int sqlType) throws SQLException
	{
		_cstmnt.setNull(parameterName, sqlType);
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException
	{
		_cstmnt.setRowId(parameterIndex, x);
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException
	{
		return _cstmnt.execute(sql, columnIndexes);
	}

	@Override
	public void setBoolean(String parameterName, boolean x) throws SQLException
	{
		_cstmnt.setBoolean(parameterName, x);
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException
	{
		_cstmnt.setNString(parameterIndex, value);
	}

	@Override
	public void setByte(String parameterName, byte x) throws SQLException
	{
		_cstmnt.setByte(parameterName, x);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException
	{
		_cstmnt.setNCharacterStream(parameterIndex, value, length);
	}

	@Override
	public void setShort(String parameterName, short x) throws SQLException
	{
		_cstmnt.setShort(parameterName, x);
	}

	@Override
	public void setInt(String parameterName, int x) throws SQLException
	{
		_cstmnt.setInt(parameterName, x);
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException
	{
		return _cstmnt.execute(sql, columnNames);
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException
	{
		_cstmnt.setNClob(parameterIndex, value);
	}

	@Override
	public void setLong(String parameterName, long x) throws SQLException
	{
		_cstmnt.setLong(parameterName, x);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
	{
		_cstmnt.setClob(parameterIndex, reader, length);
	}

	@Override
	public void setFloat(String parameterName, float x) throws SQLException
	{
		_cstmnt.setFloat(parameterName, x);
	}

	@Override
	public void setDouble(String parameterName, double x) throws SQLException
	{
		_cstmnt.setDouble(parameterName, x);
	}

	@Override
	public int getResultSetHoldability() throws SQLException
	{
		return _cstmnt.getResultSetHoldability();
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException
	{
		_cstmnt.setBlob(parameterIndex, inputStream, length);
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return _cstmnt.isClosed();
	}

	@Override
	public void setBigDecimal(String parameterName, BigDecimal x) throws SQLException
	{
		_cstmnt.setBigDecimal(parameterName, x);
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException
	{
		_cstmnt.setPoolable(poolable);
	}

	@Override
	public void setString(String parameterName, String x) throws SQLException
	{
		_cstmnt.setString(parameterName, x);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException
	{
		_cstmnt.setNClob(parameterIndex, reader, length);
	}

	@Override
	public boolean isPoolable() throws SQLException
	{
		return _cstmnt.isPoolable();
	}

	@Override
	public void setBytes(String parameterName, byte[] x) throws SQLException
	{
		_cstmnt.setBytes(parameterName, x);
	}

	@Override
	public void setDate(String parameterName, Date x) throws SQLException
	{
		_cstmnt.setDate(parameterName, x);
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException
	{
		_cstmnt.setSQLXML(parameterIndex, xmlObject);
	}

	@Override
	public void setTime(String parameterName, Time x) throws SQLException
	{
		_cstmnt.setTime(parameterName, x);
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException
	{
		_cstmnt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x) throws SQLException
	{
		_cstmnt.setTimestamp(parameterName, x);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, int length) throws SQLException
	{
		_cstmnt.setAsciiStream(parameterName, x, length);
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, int length) throws SQLException
	{
		_cstmnt.setBinaryStream(parameterName, x, length);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException
	{
		_cstmnt.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType, int scale) throws SQLException
	{
		_cstmnt.setObject(parameterName, x, targetSqlType, scale);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException
	{
		_cstmnt.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException
	{
		_cstmnt.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setObject(String parameterName, Object x, int targetSqlType) throws SQLException
	{
		_cstmnt.setObject(parameterName, x, targetSqlType);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException
	{
		_cstmnt.setAsciiStream(parameterIndex, x);
	}

	@Override
	public void setObject(String parameterName, Object x) throws SQLException
	{
		_cstmnt.setObject(parameterName, x);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException
	{
		_cstmnt.setBinaryStream(parameterIndex, x);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException
	{
		_cstmnt.setCharacterStream(parameterName, reader, length);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException
	{
		_cstmnt.setCharacterStream(parameterIndex, reader);
	}

	@Override
	public void setDate(String parameterName, Date x, Calendar cal) throws SQLException
	{
		_cstmnt.setDate(parameterName, x, cal);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException
	{
		_cstmnt.setNCharacterStream(parameterIndex, value);
	}

	@Override
	public void setTime(String parameterName, Time x, Calendar cal) throws SQLException
	{
		_cstmnt.setTime(parameterName, x, cal);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException
	{
		_cstmnt.setClob(parameterIndex, reader);
	}

	@Override
	public void setTimestamp(String parameterName, Timestamp x, Calendar cal) throws SQLException
	{
		_cstmnt.setTimestamp(parameterName, x, cal);
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException
	{
		_cstmnt.setBlob(parameterIndex, inputStream);
	}

	@Override
	public void setNull(String parameterName, int sqlType, String typeName) throws SQLException
	{
		_cstmnt.setNull(parameterName, sqlType, typeName);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException
	{
		_cstmnt.setNClob(parameterIndex, reader);
	}

	@Override
	public String getString(String parameterName) throws SQLException
	{
		return _cstmnt.getString(parameterName);
	}

	@Override
	public boolean getBoolean(String parameterName) throws SQLException
	{
		return _cstmnt.getBoolean(parameterName);
	}

	@Override
	public byte getByte(String parameterName) throws SQLException
	{
		return _cstmnt.getByte(parameterName);
	}

	@Override
	public short getShort(String parameterName) throws SQLException
	{
		return _cstmnt.getShort(parameterName);
	}

	@Override
	public int getInt(String parameterName) throws SQLException
	{
		return _cstmnt.getInt(parameterName);
	}

	@Override
	public long getLong(String parameterName) throws SQLException
	{
		return _cstmnt.getLong(parameterName);
	}

	@Override
	public float getFloat(String parameterName) throws SQLException
	{
		return _cstmnt.getFloat(parameterName);
	}

	@Override
	public double getDouble(String parameterName) throws SQLException
	{
		return _cstmnt.getDouble(parameterName);
	}

	@Override
	public byte[] getBytes(String parameterName) throws SQLException
	{
		return _cstmnt.getBytes(parameterName);
	}

	@Override
	public Date getDate(String parameterName) throws SQLException
	{
		return _cstmnt.getDate(parameterName);
	}

	@Override
	public Time getTime(String parameterName) throws SQLException
	{
		return _cstmnt.getTime(parameterName);
	}

	@Override
	public Timestamp getTimestamp(String parameterName) throws SQLException
	{
		return _cstmnt.getTimestamp(parameterName);
	}

	@Override
	public Object getObject(String parameterName) throws SQLException
	{
		return _cstmnt.getObject(parameterName);
	}

	@Override
	public BigDecimal getBigDecimal(String parameterName) throws SQLException
	{
		return _cstmnt.getBigDecimal(parameterName);
	}

	@Override
	public Object getObject(String parameterName, Map<String, Class<?>> map) throws SQLException
	{
		return _cstmnt.getObject(parameterName, map);
	}

	@Override
	public Ref getRef(String parameterName) throws SQLException
	{
		return _cstmnt.getRef(parameterName);
	}

	@Override
	public Blob getBlob(String parameterName) throws SQLException
	{
		return _cstmnt.getBlob(parameterName);
	}

	@Override
	public Clob getClob(String parameterName) throws SQLException
	{
		return _cstmnt.getClob(parameterName);
	}

	@Override
	public Array getArray(String parameterName) throws SQLException
	{
		return _cstmnt.getArray(parameterName);
	}

	@Override
	public Date getDate(String parameterName, Calendar cal) throws SQLException
	{
		return _cstmnt.getDate(parameterName, cal);
	}

	@Override
	public Time getTime(String parameterName, Calendar cal) throws SQLException
	{
		return _cstmnt.getTime(parameterName, cal);
	}

	@Override
	public Timestamp getTimestamp(String parameterName, Calendar cal) throws SQLException
	{
		return _cstmnt.getTimestamp(parameterName, cal);
	}

	@Override
	public URL getURL(String parameterName) throws SQLException
	{
		return _cstmnt.getURL(parameterName);
	}

	@Override
	public RowId getRowId(int parameterIndex) throws SQLException
	{
		return _cstmnt.getRowId(parameterIndex);
	}

	@Override
	public RowId getRowId(String parameterName) throws SQLException
	{
		return _cstmnt.getRowId(parameterName);
	}

	@Override
	public void setRowId(String parameterName, RowId x) throws SQLException
	{
		_cstmnt.setRowId(parameterName, x);
	}

	@Override
	public void setNString(String parameterName, String value) throws SQLException
	{
		_cstmnt.setNString(parameterName, value);
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException
	{
		_cstmnt.setNCharacterStream(parameterName, value, length);
	}

	@Override
	public void setNClob(String parameterName, NClob value) throws SQLException
	{
		_cstmnt.setNClob(parameterName, value);
	}

	@Override
	public void setClob(String parameterName, Reader reader, long length) throws SQLException
	{
		_cstmnt.setClob(parameterName, reader, length);
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException
	{
		_cstmnt.setBlob(parameterName, inputStream, length);
	}

	@Override
	public void setNClob(String parameterName, Reader reader, long length) throws SQLException
	{
		_cstmnt.setNClob(parameterName, reader, length);
	}

	@Override
	public NClob getNClob(int parameterIndex) throws SQLException
	{
		return _cstmnt.getNClob(parameterIndex);
	}

	@Override
	public NClob getNClob(String parameterName) throws SQLException
	{
		return _cstmnt.getNClob(parameterName);
	}

	@Override
	public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException
	{
		_cstmnt.setSQLXML(parameterName, xmlObject);
	}

	@Override
	public SQLXML getSQLXML(int parameterIndex) throws SQLException
	{
		return _cstmnt.getSQLXML(parameterIndex);
	}

	@Override
	public SQLXML getSQLXML(String parameterName) throws SQLException
	{
		return _cstmnt.getSQLXML(parameterName);
	}

	@Override
	public String getNString(int parameterIndex) throws SQLException
	{
		return _cstmnt.getNString(parameterIndex);
	}

	@Override
	public String getNString(String parameterName) throws SQLException
	{
		return _cstmnt.getNString(parameterName);
	}

	@Override
	public Reader getNCharacterStream(int parameterIndex) throws SQLException
	{
		return _cstmnt.getNCharacterStream(parameterIndex);
	}

	@Override
	public Reader getNCharacterStream(String parameterName) throws SQLException
	{
		return _cstmnt.getNCharacterStream(parameterName);
	}

	@Override
	public Reader getCharacterStream(int parameterIndex) throws SQLException
	{
		return _cstmnt.getCharacterStream(parameterIndex);
	}

	@Override
	public Reader getCharacterStream(String parameterName) throws SQLException
	{
		return _cstmnt.getCharacterStream(parameterName);
	}

	@Override
	public void setBlob(String parameterName, Blob x) throws SQLException
	{
		_cstmnt.setBlob(parameterName, x);
	}

	@Override
	public void setClob(String parameterName, Clob x) throws SQLException
	{
		_cstmnt.setClob(parameterName, x);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException
	{
		_cstmnt.setAsciiStream(parameterName, x, length);
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException
	{
		_cstmnt.setBinaryStream(parameterName, x, length);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException
	{
		_cstmnt.setCharacterStream(parameterName, reader, length);
	}

	@Override
	public void setAsciiStream(String parameterName, InputStream x) throws SQLException
	{
		_cstmnt.setAsciiStream(parameterName, x);
	}

	@Override
	public void setBinaryStream(String parameterName, InputStream x) throws SQLException
	{
		_cstmnt.setBinaryStream(parameterName, x);
	}

	@Override
	public void setCharacterStream(String parameterName, Reader reader) throws SQLException
	{
		_cstmnt.setCharacterStream(parameterName, reader);
	}

	@Override
	public void setNCharacterStream(String parameterName, Reader value) throws SQLException
	{
		_cstmnt.setNCharacterStream(parameterName, value);
	}

	@Override
	public void setClob(String parameterName, Reader reader) throws SQLException
	{
		_cstmnt.setClob(parameterName, reader);
	}

	@Override
	public void setBlob(String parameterName, InputStream inputStream) throws SQLException
	{
		_cstmnt.setBlob(parameterName, inputStream);
	}

	@Override
	public void setNClob(String parameterName, Reader reader) throws SQLException
	{
		_cstmnt.setNClob(parameterName, reader);
	}

	//#######################################################
	//############################# JDBC 4.1
	//#######################################################

	@Override
	public void closeOnCompletion() throws SQLException
	{
		_cstmnt.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException
	{
		return _cstmnt.isCloseOnCompletion();
	}

	@Override
	public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException
	{
		return _cstmnt.getObject(parameterIndex, type);
	}

	@Override
	public <T> T getObject(String parameterName, Class<T> type) throws SQLException
	{
		return _cstmnt.getObject(parameterName, type);
	}

}
