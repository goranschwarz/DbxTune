package com.asetune.sql.conn;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
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

import org.apache.log4j.Logger;

public class DbxPreparedStatement implements PreparedStatement
{
	private static Logger _logger = Logger.getLogger(DbxPreparedStatement.class);

	public static DbxPreparedStatement create(PreparedStatement pstmnt)
	{
		if (pstmnt == null)
			throw new IllegalArgumentException("create(): pstmnt can't be null");

		return new DbxPreparedStatement(pstmnt);
	}

	public DbxPreparedStatement(PreparedStatement pstmnt)
	{
		_pstmnt = pstmnt;
	}


	@Override
	public String toString()
	{
		return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[_pstmnt=" + _pstmnt + "]";
	}

	//#################################################################################
	//#################################################################################
	//### BEGIN: delegated methods
	//#################################################################################
	//#################################################################################
	protected PreparedStatement _pstmnt;

	@Override
	public ResultSet executeQuery(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxPreparedStatement.executeQuery(String): sql='"+sql+"'.");
		
		return _pstmnt.executeQuery(sql);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return _pstmnt.unwrap(iface);
	}

	@Override
	public ResultSet executeQuery() throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxPreparedStatement.executeQuery(): none");
		
		return _pstmnt.executeQuery();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxPreparedStatement.executeUpdate(String): sql='"+sql+"'.");
		
		return _pstmnt.executeUpdate(sql);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return _pstmnt.isWrapperFor(iface);
	}

	@Override
	public int executeUpdate() throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxPreparedStatement.executeUpdate(): none");
		
		return _pstmnt.executeUpdate();
	}

	@Override
	public void close() throws SQLException
	{
		_pstmnt.close();
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException
	{
		_pstmnt.setNull(parameterIndex, sqlType);
	}

	@Override
	public int getMaxFieldSize() throws SQLException
	{
		return _pstmnt.getMaxFieldSize();
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException
	{
		_pstmnt.setBoolean(parameterIndex, x);
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException
	{
		_pstmnt.setMaxFieldSize(max);
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException
	{
		_pstmnt.setByte(parameterIndex, x);
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException
	{
		_pstmnt.setShort(parameterIndex, x);
	}

	@Override
	public int getMaxRows() throws SQLException
	{
		return _pstmnt.getMaxRows();
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException
	{
		_pstmnt.setInt(parameterIndex, x);
	}

	@Override
	public void setMaxRows(int max) throws SQLException
	{
		_pstmnt.setMaxRows(max);
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException
	{
		_pstmnt.setLong(parameterIndex, x);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		_pstmnt.setEscapeProcessing(enable);
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException
	{
		_pstmnt.setFloat(parameterIndex, x);
	}

	@Override
	public int getQueryTimeout() throws SQLException
	{
		return _pstmnt.getQueryTimeout();
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException
	{
		_pstmnt.setDouble(parameterIndex, x);
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException
	{
		_pstmnt.setQueryTimeout(seconds);
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
	{
		_pstmnt.setBigDecimal(parameterIndex, x);
	}

	@Override
	public void cancel() throws SQLException
	{
		_pstmnt.cancel();
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException
	{
		_pstmnt.setString(parameterIndex, x);
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return _pstmnt.getWarnings();
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException
	{
		_pstmnt.setBytes(parameterIndex, x);
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		_pstmnt.clearWarnings();
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException
	{
		_pstmnt.setDate(parameterIndex, x);
	}

	@Override
	public void setCursorName(String name) throws SQLException
	{
		_pstmnt.setCursorName(name);
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException
	{
		_pstmnt.setTime(parameterIndex, x);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
	{
		_pstmnt.setTimestamp(parameterIndex, x);
	}

	@Override
	public boolean execute(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxPreparedStatement.execute(String): sql='"+sql+"'.");
		
		return _pstmnt.execute(sql);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		_pstmnt.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public ResultSet getResultSet() throws SQLException
	{
		return _pstmnt.getResultSet();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		_pstmnt.setUnicodeStream(parameterIndex, x, length);
	}

	@Override
	public int getUpdateCount() throws SQLException
	{
		return _pstmnt.getUpdateCount();
	}

	@Override
	public boolean getMoreResults() throws SQLException
	{
		return _pstmnt.getMoreResults();
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
	{
		_pstmnt.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException
	{
		_pstmnt.setFetchDirection(direction);
	}

	@Override
	public void clearParameters() throws SQLException
	{
		_pstmnt.clearParameters();
	}

	@Override
	public int getFetchDirection() throws SQLException
	{
		return _pstmnt.getFetchDirection();
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
	{
		_pstmnt.setObject(parameterIndex, x, targetSqlType);
	}

	@Override
	public void setFetchSize(int rows) throws SQLException
	{
		_pstmnt.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException
	{
		return _pstmnt.getFetchSize();
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException
	{
		_pstmnt.setObject(parameterIndex, x);
	}

	@Override
	public int getResultSetConcurrency() throws SQLException
	{
		return _pstmnt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetType() throws SQLException
	{
		return _pstmnt.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException
	{
		_pstmnt.addBatch(sql);
	}

	@Override
	public void clearBatch() throws SQLException
	{
		_pstmnt.clearBatch();
	}

	@Override
	public boolean execute() throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxPreparedStatement.execute(): none");
		
		return _pstmnt.execute();
	}

	@Override
	public int[] executeBatch() throws SQLException
	{
		return _pstmnt.executeBatch();
	}

	@Override
	public void addBatch() throws SQLException
	{
		_pstmnt.addBatch();
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
	{
		_pstmnt.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException
	{
		_pstmnt.setRef(parameterIndex, x);
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return _pstmnt.getConnection();
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException
	{
		_pstmnt.setBlob(parameterIndex, x);
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException
	{
		_pstmnt.setClob(parameterIndex, x);
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException
	{
		return _pstmnt.getMoreResults(current);
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException
	{
		_pstmnt.setArray(parameterIndex, x);
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException
	{
		return _pstmnt.getMetaData();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException
	{
		return _pstmnt.getGeneratedKeys();
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
	{
		_pstmnt.setDate(parameterIndex, x, cal);
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _pstmnt.executeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
	{
		_pstmnt.setTime(parameterIndex, x, cal);
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		return _pstmnt.executeUpdate(sql, columnIndexes);
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
	{
		_pstmnt.setTimestamp(parameterIndex, x, cal);
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException
	{
		_pstmnt.setNull(parameterIndex, sqlType, typeName);
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException
	{
		return _pstmnt.executeUpdate(sql, columnNames);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _pstmnt.execute(sql, autoGeneratedKeys);
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException
	{
		_pstmnt.setURL(parameterIndex, x);
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException
	{
		return _pstmnt.getParameterMetaData();
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException
	{
		_pstmnt.setRowId(parameterIndex, x);
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException
	{
		return _pstmnt.execute(sql, columnIndexes);
	}

	@Override
	public void setNString(int parameterIndex, String value) throws SQLException
	{
		_pstmnt.setNString(parameterIndex, value);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException
	{
		_pstmnt.setNCharacterStream(parameterIndex, value, length);
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException
	{
		return _pstmnt.execute(sql, columnNames);
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException
	{
		_pstmnt.setNClob(parameterIndex, value);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
	{
		_pstmnt.setClob(parameterIndex, reader, length);
	}

	@Override
	public int getResultSetHoldability() throws SQLException
	{
		return _pstmnt.getResultSetHoldability();
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException
	{
		_pstmnt.setBlob(parameterIndex, inputStream, length);
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return _pstmnt.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException
	{
		_pstmnt.setPoolable(poolable);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException
	{
		_pstmnt.setNClob(parameterIndex, reader, length);
	}

	@Override
	public boolean isPoolable() throws SQLException
	{
		return _pstmnt.isPoolable();
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException
	{
		_pstmnt.setSQLXML(parameterIndex, xmlObject);
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException
	{
		_pstmnt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException
	{
		_pstmnt.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException
	{
		_pstmnt.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException
	{
		_pstmnt.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException
	{
		_pstmnt.setAsciiStream(parameterIndex, x);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException
	{
		_pstmnt.setBinaryStream(parameterIndex, x);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException
	{
		_pstmnt.setCharacterStream(parameterIndex, reader);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException
	{
		_pstmnt.setNCharacterStream(parameterIndex, value);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException
	{
		_pstmnt.setClob(parameterIndex, reader);
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException
	{
		_pstmnt.setBlob(parameterIndex, inputStream);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException
	{
		_pstmnt.setNClob(parameterIndex, reader);
	}

	//#######################################################
	//############################# JDBC 4.1
	//#######################################################

	@Override
	public void closeOnCompletion() throws SQLException
	{
		_pstmnt.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException
	{
		return _pstmnt.isCloseOnCompletion();
	}


}
