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

import java.io.InputStream;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
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
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class PreparedStatementCache
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static HashMap<Connection, PreparedStatementCacheEntries> _psceMap = new HashMap<Connection, PreparedStatementCacheEntries>();

	/**
	 * 
	 * @param conn
	 * @param sql
	 * @return a Prepared Statement
	 * @throws SQLException
	 */
	public static PreparedStatement getPreparedStatement(Connection conn, String sql)
	throws SQLException
	{
		PreparedStatementCacheEntries psce = _psceMap.get(conn);
		if (psce == null)
		{
			psce = new PreparedStatementCacheEntries(conn);
			_psceMap.put(conn, psce);
		}

		return psce.getPreparedStatement(sql);
	}

	public static void remove(Connection conn, PreparedStatementWrapper psw)
	{
		PreparedStatementCacheEntries psce = _psceMap.get(conn);
		if (psce != null)
			psce.remove(psw);
	}
	
	/**
	 * @author gorans
	 */
	private static class PreparedStatementCacheEntries
	{
		private Connection _conn = null;
		private HashMap<String, PreparedStatement> _pstmntMap = null;
	
		public PreparedStatementCacheEntries(Connection conn)
		{
			setConnection(conn);
		}
	
		public void remove(PreparedStatementWrapper psw)
		{
			String removeKey = null;
			for (Map.Entry<String,PreparedStatement> entry : _pstmntMap.entrySet()) 
			{
				String            key = entry.getKey();
				PreparedStatement val = entry.getValue();

				if (psw.equals(val))
				{
					removeKey = key;
					break;
				}
			}
			if (removeKey != null)
				_pstmntMap.remove(removeKey);
		}

		public void setConnection(Connection conn)
		{
			// Create a new cache map, when new connection
			_pstmntMap = new HashMap<String, PreparedStatement>();
	
			_conn = conn;
		}
	
		public Connection getConnection()
		{
			return _conn;
		}
	
		public PreparedStatement getPreparedStatement(String sql)
		throws SQLException
		{
			PreparedStatement pstmnt = _pstmntMap.get(sql);
			if (pstmnt == null)
			{
				if (_logger.isDebugEnabled())
					_logger.debug("GENERATING a new PreparedStatement for conn='"+getConnection()+"', with SQL: "+sql);
	
				Connection conn = getConnection();
//				pstmnt = conn.prepareStatement(sql);
				pstmnt = new PreparedStatementWrapper( conn, conn.prepareStatement(sql) );

				_pstmntMap.put(sql, pstmnt);
			}
			return pstmnt;
		}
	}

	private static class PreparedStatementWrapper 
	implements PreparedStatement
	{
		//-----------------------------------------------------------
		// Local code
		//-----------------------------------------------------------
		private Connection _conn = null;
		private PreparedStatement _ps = null;
		public PreparedStatementWrapper(Connection conn, PreparedStatement ps)
		{
			_conn = conn;
			_ps   = ps;
		}

		public void close(boolean forceClose) throws SQLException
		{
			if ( ! forceClose)
				return;

			_ps.close();

			PreparedStatementCache.remove(_conn, this);
		}

		private static boolean _forceClose = false;
		@SuppressWarnings("unused")
		public static void setForceClose(boolean forceClose)
		{
			_forceClose = forceClose;
		}
		@SuppressWarnings("unused")
		public static boolean getForceClose()
		{
			return _forceClose;
		}

		//-----------------------------------------------------------
		// implementing/delegating PreparedStatement
		//-----------------------------------------------------------
		@Override
		public void addBatch(String sql) throws SQLException
		{
			_ps.addBatch(sql);
		}

		@Override
		public void cancel() throws SQLException
		{
			_ps.cancel();
		}

		@Override
		public void clearBatch() throws SQLException
		{
			_ps.clearBatch();
		}

		@Override
		public void clearWarnings() throws SQLException
		{
			_ps.clearWarnings();
		}

		@Override
		public void close() throws SQLException
		{
			close(_forceClose);
		}

		@Override
		public boolean execute(String sql) throws SQLException
		{
			return _ps.execute(sql);
		}

		@Override
		public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
		{
			return _ps.execute(sql, autoGeneratedKeys);
		}

		@Override
		public boolean execute(String sql, int[] columnIndexes) throws SQLException
		{
			return _ps.execute(sql, columnIndexes);
		}

		@Override
		public boolean execute(String sql, String[] columnNames) throws SQLException
		{
			return _ps.execute(sql, columnNames);
		}

		@Override
		public int[] executeBatch() throws SQLException
		{
			return _ps.executeBatch();
		}

		@Override
		public ResultSet executeQuery(String sql) throws SQLException
		{
			return _ps.executeQuery(sql);
		}

		@Override
		public int executeUpdate(String sql) throws SQLException
		{
			return _ps.executeUpdate(sql);
		}

		@Override
		public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
		{
			return _ps.executeUpdate(sql, autoGeneratedKeys);
		}

		@Override
		public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
		{
			return _ps.executeUpdate(sql, columnIndexes);
		}

		@Override
		public int executeUpdate(String sql, String[] columnNames) throws SQLException
		{
			return _ps.executeUpdate(sql, columnNames);
		}

		@Override
		public Connection getConnection() throws SQLException
		{
			return _ps.getConnection();
		}

		@Override
		public int getFetchDirection() throws SQLException
		{
			return _ps.getFetchDirection();
		}

		@Override
		public int getFetchSize() throws SQLException
		{
			return _ps.getFetchSize();
		}

		@Override
		public ResultSet getGeneratedKeys() throws SQLException
		{
			return _ps.getGeneratedKeys();
		}

		@Override
		public int getMaxFieldSize() throws SQLException
		{
			return _ps.getMaxFieldSize();
		}

		@Override
		public int getMaxRows() throws SQLException
		{
			return _ps.getMaxRows();
		}

		@Override
		public boolean getMoreResults() throws SQLException
		{
			return _ps.getMoreResults();
		}

		@Override
		public boolean getMoreResults(int current) throws SQLException
		{
			return _ps.getMoreResults(current);
		}

		@Override
		public int getQueryTimeout() throws SQLException
		{
			return _ps.getQueryTimeout();
		}

		@Override
		public ResultSet getResultSet() throws SQLException
		{
			return _ps.getResultSet();
		}

		@Override
		public int getResultSetConcurrency() throws SQLException
		{
			return _ps.getResultSetConcurrency();
		}

		@Override
		public int getResultSetHoldability() throws SQLException
		{
			return _ps.getResultSetHoldability();
		}

		@Override
		public int getResultSetType() throws SQLException
		{
			return _ps.getResultSetType();
		}

		@Override
		public int getUpdateCount() throws SQLException
		{
			return _ps.getUpdateCount();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException
		{
			return _ps.getWarnings();
		}

		@Override
		public boolean isClosed() throws SQLException
		{
			return _ps.isClosed();
		}

		@Override
		public boolean isPoolable() throws SQLException
		{
			return _ps.isPoolable();
		}

		@Override
		public void setCursorName(String name) throws SQLException
		{
			_ps.setCursorName(name);
		}

		@Override
		public void setEscapeProcessing(boolean enable) throws SQLException
		{
			_ps.setEscapeProcessing(enable);
		}

		@Override
		public void setFetchDirection(int direction) throws SQLException
		{
			_ps.setFetchDirection(direction);
		}

		@Override
		public void setFetchSize(int rows) throws SQLException
		{
			_ps.setFetchSize(rows);
		}

		@Override
		public void setMaxFieldSize(int max) throws SQLException
		{
			_ps.setMaxFieldSize(max);
		}

		@Override
		public void setMaxRows(int max) throws SQLException
		{
			_ps.setMaxRows(max);
		}

		@Override
		public void setPoolable(boolean poolable) throws SQLException
		{
			_ps.setPoolable(poolable);
		}

		@Override
		public void setQueryTimeout(int seconds) throws SQLException
		{
			_ps.setQueryTimeout(seconds);
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) throws SQLException
		{
			return _ps.isWrapperFor(iface);
		}

		@Override
		public <T> T unwrap(Class<T> iface) throws SQLException
		{
			return _ps.unwrap(iface);
		}

		@Override
		public void addBatch() throws SQLException
		{
			_ps.addBatch();
		}

		@Override
		public void clearParameters() throws SQLException
		{
			_ps.clearParameters();
		}

		@Override
		public boolean execute() throws SQLException
		{
			return _ps.execute();
		}

		@Override
		public ResultSet executeQuery() throws SQLException
		{
			return _ps.executeQuery();
		}

		@Override
		public int executeUpdate() throws SQLException
		{
			return _ps.executeUpdate();
		}

		@Override
		public ResultSetMetaData getMetaData() throws SQLException
		{
			return _ps.getMetaData();
		}

		@Override
		public ParameterMetaData getParameterMetaData() throws SQLException
		{
			return _ps.getParameterMetaData();
		}

		@Override
		public void setArray(int parameterIndex, Array x) throws SQLException
		{
			_ps.setArray(parameterIndex, x);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException
		{
			_ps.setAsciiStream(parameterIndex, x);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException
		{
			_ps.setAsciiStream(parameterIndex, x, length);
		}

		@Override
		public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException
		{
			_ps.setAsciiStream(parameterIndex, x, length);
		}

		@Override
		public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException
		{
			_ps.setBigDecimal(parameterIndex, x);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException
		{
			_ps.setBinaryStream(parameterIndex, x);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException
		{
			_ps.setBinaryStream(parameterIndex, x, length);
		}

		@Override
		public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException
		{
			_ps.setBinaryStream(parameterIndex, x, length);
		}

		@Override
		public void setBlob(int parameterIndex, Blob x) throws SQLException
		{
			_ps.setBlob(parameterIndex, x);
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException
		{
			_ps.setBlob(parameterIndex, inputStream);
		}

		@Override
		public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException
		{
			_ps.setBlob(parameterIndex, inputStream, length);
		}

		@Override
		public void setBoolean(int parameterIndex, boolean x) throws SQLException
		{
			_ps.setBoolean(parameterIndex, x);
		}

		@Override
		public void setByte(int parameterIndex, byte x) throws SQLException
		{
			_ps.setByte(parameterIndex, x);
		}

		@Override
		public void setBytes(int parameterIndex, byte[] x) throws SQLException
		{
			_ps.setBytes(parameterIndex, x);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException
		{
			_ps.setCharacterStream(parameterIndex, reader);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException
		{
			_ps.setCharacterStream(parameterIndex, reader, length);
		}

		@Override
		public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException
		{
			_ps.setCharacterStream(parameterIndex, reader, length);
		}

		@Override
		public void setClob(int parameterIndex, Clob x) throws SQLException
		{
			_ps.setClob(parameterIndex, x);
		}

		@Override
		public void setClob(int parameterIndex, Reader reader) throws SQLException
		{
			_ps.setClob(parameterIndex, reader);
		}

		@Override
		public void setClob(int parameterIndex, Reader reader, long length) throws SQLException
		{
			_ps.setClob(parameterIndex, reader, length);
		}

		@Override
		public void setDate(int parameterIndex, Date x) throws SQLException
		{
			_ps.setDate(parameterIndex, x);
		}

		@Override
		public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException
		{
			_ps.setDate(parameterIndex, x, cal);
		}

		@Override
		public void setDouble(int parameterIndex, double x) throws SQLException
		{
			_ps.setDouble(parameterIndex, x);
		}

		@Override
		public void setFloat(int parameterIndex, float x) throws SQLException
		{
			_ps.setFloat(parameterIndex, x);
		}

		@Override
		public void setInt(int parameterIndex, int x) throws SQLException
		{
			_ps.setInt(parameterIndex, x);
		}

		@Override
		public void setLong(int parameterIndex, long x) throws SQLException
		{
			_ps.setLong(parameterIndex, x);
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException
		{
			_ps.setNCharacterStream(parameterIndex, value);
		}

		@Override
		public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException
		{
			_ps.setNCharacterStream(parameterIndex, value, length);
		}

		@Override
		public void setNClob(int parameterIndex, NClob value) throws SQLException
		{
			_ps.setNClob(parameterIndex, value);
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader) throws SQLException
		{
			_ps.setNClob(parameterIndex, reader);
		}

		@Override
		public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException
		{
			_ps.setNClob(parameterIndex, reader, length);
		}

		@Override
		public void setNString(int parameterIndex, String value) throws SQLException
		{
			_ps.setNString(parameterIndex, value);
		}

		@Override
		public void setNull(int parameterIndex, int sqlType) throws SQLException
		{
			_ps.setNull(parameterIndex, sqlType);
		}

		@Override
		public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException
		{
			_ps.setNull(parameterIndex, sqlType, typeName);
		}

		@Override
		public void setObject(int parameterIndex, Object x) throws SQLException
		{
			_ps.setObject(parameterIndex, x);
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException
		{
			_ps.setObject(parameterIndex, x, targetSqlType);
		}

		@Override
		public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException
		{
			_ps.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		}

		@Override
		public void setRef(int parameterIndex, Ref x) throws SQLException
		{
			_ps.setRef(parameterIndex, x);
		}

		@Override
		public void setRowId(int parameterIndex, RowId x) throws SQLException
		{
			_ps.setRowId(parameterIndex, x);
		}

		@Override
		public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException
		{
			_ps.setSQLXML(parameterIndex, xmlObject);
		}

		@Override
		public void setShort(int parameterIndex, short x) throws SQLException
		{
			_ps.setShort(parameterIndex, x);
		}

		@Override
		public void setString(int parameterIndex, String x) throws SQLException
		{
			_ps.setString(parameterIndex, x);
		}

		@Override
		public void setTime(int parameterIndex, Time x) throws SQLException
		{
			_ps.setTime(parameterIndex, x);
		}

		@Override
		public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException
		{
			_ps.setTime(parameterIndex, x, cal);
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException
		{
			_ps.setTimestamp(parameterIndex, x);
		}

		@Override
		public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException
		{
			_ps.setTimestamp(parameterIndex, x, cal);
		}

		@Override
		public void setURL(int parameterIndex, URL x) throws SQLException
		{
			_ps.setURL(parameterIndex, x);
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException
		{
			_ps.setUnicodeStream(parameterIndex, x, length);
		}

		//#######################################################
		//############################# JDBC 4.1
		//#######################################################

		@Override
		public void closeOnCompletion() throws SQLException
		{
			_ps.closeOnCompletion();
		}

		@Override
		public boolean isCloseOnCompletion() throws SQLException
		{
			return _ps.isCloseOnCompletion();
		}
		
	}
}
