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
package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

public class DbxStatement implements Statement
{
	private static Logger _logger = Logger.getLogger(DbxStatement.class);

	public static DbxStatement create(Statement stmnt)
	{
		if (stmnt == null)
			throw new IllegalArgumentException("create(): stmnt can't be null");

		return new DbxStatement(stmnt);
	}

	public DbxStatement(Statement stmnt)
	{
		_stmnt = stmnt;
	}

	@Override
	public String toString()
	{
		return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[_stmnt=" + _stmnt + "]";
	}

	//#################################################################################
	//#################################################################################
	//### BEGIN: delegated methods
	//#################################################################################
	//#################################################################################
	protected Statement _stmnt;

	@Override
	public ResultSet executeQuery(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxStatement.executeQuery(String): sql='"+sql+"'.");
		
		return _stmnt.executeQuery(sql);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		return _stmnt.unwrap(iface);
	}

	@Override
	public int executeUpdate(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxStatement.executeUpdate(String): sql='"+sql+"'.");
		
		return _stmnt.executeUpdate(sql);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return _stmnt.isWrapperFor(iface);
	}

	@Override
	public void close() throws SQLException
	{
		_stmnt.close();
	}

	@Override
	public int getMaxFieldSize() throws SQLException
	{
		return _stmnt.getMaxFieldSize();
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException
	{
		_stmnt.setMaxFieldSize(max);
	}

	@Override
	public int getMaxRows() throws SQLException
	{
		return _stmnt.getMaxRows();
	}

	@Override
	public void setMaxRows(int max) throws SQLException
	{
		_stmnt.setMaxRows(max);
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		_stmnt.setEscapeProcessing(enable);
	}

	@Override
	public int getQueryTimeout() throws SQLException
	{
		return _stmnt.getQueryTimeout();
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException
	{
		_stmnt.setQueryTimeout(seconds);
	}

	@Override
	public void cancel() throws SQLException
	{
		_stmnt.cancel();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		return _stmnt.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		_stmnt.clearWarnings();
	}

	@Override
	public void setCursorName(String name) throws SQLException
	{
		_stmnt.setCursorName(name);
	}

	@Override
	public boolean execute(String sql) throws SQLException
	{
		if (_logger.isDebugEnabled())
			_logger.debug("DbxStatement.execute(String): sql='"+sql+"'.");
		
		return _stmnt.execute(sql);
	}

	@Override
	public ResultSet getResultSet() throws SQLException
	{
		return _stmnt.getResultSet();
	}

	@Override
	public int getUpdateCount() throws SQLException
	{
		return _stmnt.getUpdateCount();
	}

	@Override
	public boolean getMoreResults() throws SQLException
	{
		return _stmnt.getMoreResults();
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException
	{
		_stmnt.setFetchDirection(direction);
	}

	@Override
	public int getFetchDirection() throws SQLException
	{
		return _stmnt.getFetchDirection();
	}

	@Override
	public void setFetchSize(int rows) throws SQLException
	{
		_stmnt.setFetchSize(rows);
	}

	@Override
	public int getFetchSize() throws SQLException
	{
		return _stmnt.getFetchSize();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException
	{
		return _stmnt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetType() throws SQLException
	{
		return _stmnt.getResultSetType();
	}

	@Override
	public void addBatch(String sql) throws SQLException
	{
		_stmnt.addBatch(sql);
	}

	@Override
	public void clearBatch() throws SQLException
	{
		_stmnt.clearBatch();
	}

	@Override
	public int[] executeBatch() throws SQLException
	{
		return _stmnt.executeBatch();
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		return _stmnt.getConnection();
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException
	{
		return _stmnt.getMoreResults(current);
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException
	{
		return _stmnt.getGeneratedKeys();
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _stmnt.executeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		return _stmnt.executeUpdate(sql, columnIndexes);
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException
	{
		return _stmnt.executeUpdate(sql, columnNames);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
	{
		return _stmnt.execute(sql, autoGeneratedKeys);
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException
	{
		return _stmnt.execute(sql, columnIndexes);
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException
	{
		return _stmnt.execute(sql, columnNames);
	}

	@Override
	public int getResultSetHoldability() throws SQLException
	{
		return _stmnt.getResultSetHoldability();
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		return _stmnt.isClosed();
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException
	{
		_stmnt.setPoolable(poolable);
	}

	@Override
	public boolean isPoolable() throws SQLException
	{
		return _stmnt.isPoolable();
	}

	//#######################################################
	//############################# JDBC 4.1
	//#######################################################

	@Override
	public void closeOnCompletion() throws SQLException
	{
		_stmnt.closeOnCompletion();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException
	{
		return _stmnt.isCloseOnCompletion();
	}

}
