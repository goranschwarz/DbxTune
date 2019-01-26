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
package com.asetune.tools.sqlw;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

public class StatementDummy implements Statement
{

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException
	{
		throw new SQLException("Not implemented, method: unwrap()");
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException
	{
		return false;
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException
	{
		throw new SQLException("Not implemented, method: executeQuery(String sql)");
	}

	@Override
	public int executeUpdate(String sql) throws SQLException
	{
		throw new SQLException("Not implemented, method: executeUpdate(String sql)");
	}

	@Override
	public void close() throws SQLException
	{
	}

	@Override
	public int getMaxFieldSize() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int getMaxRows() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMaxRows(int max) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int getQueryTimeout() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void cancel() throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public SQLWarning getWarnings() throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clearWarnings() throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void setCursorName(String name) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public boolean execute(String sql) throws SQLException
	{
		throw new SQLException("Not implemented, method: execute(String sql)");
//		// TODO Auto-generated method stub
//		return false;
	}

	@Override
	public ResultSet getResultSet() throws SQLException
	{
		throw new SQLException("Not implemented, method: getResultSet()");
//		// TODO Auto-generated method stub
//		return null;
	}

	@Override
	public int getUpdateCount() throws SQLException
	{
		return -1;
	}

	@Override
	public boolean getMoreResults() throws SQLException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int getFetchDirection() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setFetchSize(int rows) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int getFetchSize() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getResultSetConcurrency() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getResultSetType() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addBatch(String sql) throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void clearBatch() throws SQLException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public int[] executeBatch() throws SQLException
	{
		throw new SQLException("Not implemented, method: executeBatch()");
//		// TODO Auto-generated method stub
//		return null;
	}

	@Override
	public Connection getConnection() throws SQLException
	{
		throw new SQLException("Not implemented, method: getConnection()");
//		// TODO Auto-generated method stub
//		return null;
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException
	{
		throw new SQLException("Not implemented, method: executeUpdate(String sql, int autoGeneratedKeys)");
//		// TODO Auto-generated method stub
//		return 0;
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes) throws SQLException
	{
		throw new SQLException("Not implemented, method: executeUpdate(String sql, int[] columnIndexes)");
//		// TODO Auto-generated method stub
//		return 0;
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames) throws SQLException
	{
		throw new SQLException("Not implemented, method: executeUpdate(String sql, String[] columnNames)");
//		// TODO Auto-generated method stub
//		return 0;
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys) throws SQLException
	{
		throw new SQLException("Not implemented, method: execute(String sql, int autoGeneratedKeys)");
//		// TODO Auto-generated method stub
//		return false;
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException
	{
		throw new SQLException("Not implemented, method: execute(String sql, int[] columnIndexes)");
//		// TODO Auto-generated method stub
//		return false;
	}

	@Override
	public boolean execute(String sql, String[] columnNames) throws SQLException
	{
		throw new SQLException("Not implemented, method: execute(String sql, String[] columnNames)");
//		// TODO Auto-generated method stub
//		return false;
	}

	@Override
	public int getResultSetHoldability() throws SQLException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() throws SQLException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException
	{
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isPoolable() throws SQLException
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void closeOnCompletion() throws SQLException
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException
	{
		// TODO Auto-generated method stub
		return false;
	}

}
