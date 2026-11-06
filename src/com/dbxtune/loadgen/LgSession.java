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
package com.dbxtune.loadgen;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;

/**
 * One database connection used by the load generator.
 * <p>
 * Every statement goes through {@link #query}, {@link #update} or {@link #exec}, which time it and record it
 * in the {@link Stats} (if any) under the current scenario name. That is what the journal's per-minute rows are built from.
 */
public class LgSession
implements AutoCloseable
{
	private static final Logger _logger = LogManager.getLogger();

	/** Wrap a String parameter in this to send it as NVARCHAR (SQL Server: forces an implicit conversion against a VARCHAR column) */
	public static class NString
	{
		final String _val;
		public NString(String val) { _val = val; }
	}

	private final LoadGen       _lg;
	private final String        _login;
	private final String        _appName;
	private final DbxConnection _conn;
	private final int           _spid;
	private final long          _connectTime = System.currentTimeMillis();
	private final Stats         _stats;

	private volatile Statement  _current;
	private volatile boolean    _cancelled;
	private volatile boolean    _closed;
	private String              _scenario     = "-";
	private int                 _queryTimeout = 60;

	LgSession(LoadGen lg, DbxConnection conn, String login, String appName, Stats stats)
	throws SQLException
	{
		_lg      = lg;
		_conn    = conn;
		_login   = login;
		_appName = appName;
		_stats   = stats;

		int spid = -1;
		try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(lg.getVendor().getSessionIdSql()))
		{
			if (rs.next())
				spid = rs.getInt(1);
		}
		_spid = spid;

		if (lg.getJournal() != null)
			lg.getJournal().event("session", "event", "connect", "login", login, "appName", appName, "spid", _spid);
	}

	public DbxConnection getConnection()   { return _conn; }
	public String        getLogin()        { return _login; }
	public String        getAppName()      { return _appName; }
	public int           getSpid()         { return _spid; }
	public Vendor        getVendor()       { return _lg.getVendor(); }
	public String        getScenario()     { return _scenario; }
	public boolean       isCancelled()     { return _cancelled; }

	public LgSession setScenario(String name)       { _scenario     = name; return this; }
	public LgSession setQueryTimeout(int seconds)   { _queryTimeout = seconds; return this; }

	/** "login/spid", used in log messages and the journal */
	public String getId() { return _login + "/" + _spid; }

	//------------------------------------------------------------------
	// Transactions
	//------------------------------------------------------------------
	public void begin()    throws SQLException { _conn.setAutoCommit(false); }
	public void commit()   throws SQLException { _conn.commit();   _conn.setAutoCommit(true); }
	public void rollbackQuietly()
	{
		try { if (!_conn.getAutoCommit()) { _conn.rollback(); _conn.setAutoCommit(true); } }
		catch (SQLException ignore) {}
	}

	//------------------------------------------------------------------
	// Statements
	//------------------------------------------------------------------
	/** SELECT with parameters, all rows returned */
	public List<Object[]> query(String sql, Object... params)
	throws SQLException
	{
		long start = System.currentTimeMillis();
		List<Object[]> rows = new ArrayList<>();
		try (PreparedStatement ps = _conn.prepareStatement(sql))
		{
			prepare(ps, params);
			try (ResultSet rs = ps.executeQuery())
			{
				int cols = rs.getMetaData().getColumnCount();
				while (rs.next())
				{
					Object[] row = new Object[cols];
					for (int c=0; c<cols; c++)
						row[c] = rs.getObject(c+1);
					rows.add(row);
				}
			}
			record(sql, start, rows.size(), null);
			return rows;
		}
		catch (SQLException ex)
		{
			record(sql, start, 0, ex);
			throw ex;
		}
		finally
		{
			_current = null;
		}
	}

	/** SELECT returning one value (first column of the first row), null if no rows */
	public Object queryValue(String sql, Object... params)
	throws SQLException
	{
		List<Object[]> rows = query(sql, params);
		return rows.isEmpty() ? null : rows.get(0)[0];
	}

	/** first column as an int (0 when null) */
	public int queryInt(String sql, Object... params)
	throws SQLException
	{
		Object o = queryValue(sql, params);
		return o == null ? 0 : ((Number) o).intValue();
	}

	/** INSERT/UPDATE/DELETE with parameters, returns the row count */
	public int update(String sql, Object... params)
	throws SQLException
	{
		long start = System.currentTimeMillis();
		try (PreparedStatement ps = _conn.prepareStatement(sql))
		{
			prepare(ps, params);
			int cnt = ps.executeUpdate();
			record(sql, start, cnt, null);
			return cnt;
		}
		catch (SQLException ex)
		{
			record(sql, start, 0, ex);
			throw ex;
		}
		finally
		{
			_current = null;
		}
	}

	/**
	 * Call a procedure (or anything returning several results), read and throw away all results.
	 * @return total number of rows read + rows affected
	 */
	public int call(String sql, Object... params)
	throws SQLException
	{
		long start = System.currentTimeMillis();
		try (CallableStatement cs = _conn.prepareCall(sql))
		{
			prepare(cs, params);
			int rows = consume(cs, cs.execute());
			record(sql, start, rows, null);
			return rows;
		}
		catch (SQLException ex)
		{
			record(sql, start, 0, ex);
			throw ex;
		}
		finally
		{
			_current = null;
		}
	}

	/** A plain statement without parameters (literal SQL, set options, DDL), all results are read and thrown away */
	public int exec(String sql)
	throws SQLException
	{
		long start = System.currentTimeMillis();
		try (Statement stmt = _conn.createStatement())
		{
			_current = stmt;
			if (_queryTimeout > 0)
				stmt.setQueryTimeout(_queryTimeout);
			int rows = consume(stmt, stmt.execute(sql));
			record(sql, start, rows, null);
			return rows;
		}
		catch (SQLException ex)
		{
			record(sql, start, 0, ex);
			throw ex;
		}
		finally
		{
			_current = null;
		}
	}

	private int consume(Statement stmt, boolean hasRs)
	throws SQLException
	{
		int rows = 0;
		while (true)
		{
			if (hasRs)
			{
				try (ResultSet rs = stmt.getResultSet())
				{
					while (rs.next())
						rows++;
				}
			}
			else
			{
				int cnt = stmt.getUpdateCount();
				if (cnt == -1)
					break;
				rows += cnt;
			}
			hasRs = stmt.getMoreResults();
		}
		return rows;
	}

	private void prepare(PreparedStatement ps, Object... params)
	throws SQLException
	{
		_current = ps;
		if (_queryTimeout > 0)
			ps.setQueryTimeout(_queryTimeout);
		for (int i=0; i<params.length; i++)
		{
			Object p = params[i];
			if      (p instanceof NString) ps.setNString(i+1, ((NString) p)._val);
			else if (p instanceof String)  ps.setString (i+1, (String) p);
			else                           ps.setObject (i+1, p);
		}
	}

	private void record(String sql, long startTime, int rows, SQLException ex)
	{
		if (_stats != null)
			_stats.recordStatement(_scenario, sql, System.currentTimeMillis() - startTime, rows, ex, _spid);
	}

	//------------------------------------------------------------------
	// Cancel / Close
	//------------------------------------------------------------------
	/** Called by the watchdog (another thread): cancel what is running and close the connection */
	public void cancelAndClose()
	{
		_cancelled = true;
		Statement stmt = _current;
		if (stmt != null)
		{
			try { stmt.cancel(); }
			catch (Throwable ignore) {}
		}
		_closed = true; // no rollback here: the connection is busy in another thread
		try { _conn.close(); }
		catch (Throwable ignore) {}
		if (_lg.getJournal() != null)
			_lg.getJournal().event("session", "event", "disconnect", "login", _login, "appName", _appName, "spid", _spid, "connectedMs", System.currentTimeMillis() - _connectTime, "by", "watchdog");
	}

	/**
	 * Has close() been called? Safe from any thread: it does NOT ask the driver
	 * (jConnect's isClosed() sends a query, which hangs while the connection is blocked in another thread).
	 */
	public boolean isClosed()
	{
		return _closed;
	}

	/** Is the connection dead (closed by us, or lost)? Asks the driver: call it only from the thread that uses the session. */
	public boolean isConnectionLost()
	{
		if (_closed)
			return true;
		try { return _conn.isClosed(); }
		catch (SQLException ex) { return true; }
	}

	@Override
	public void close()
	{
		if (_closed)
			return;
		_closed = true;
		try
		{
			{
				rollbackQuietly();
				_conn.close();
				if (_lg.getJournal() != null)
					_lg.getJournal().event("session", "event", "disconnect", "login", _login, "appName", _appName, "spid", _spid, "connectedMs", System.currentTimeMillis() - _connectTime);
			}
		}
		catch (Throwable ex)
		{
			_logger.debug("Problems closing session " + getId() + ". Caught: " + ex);
		}
	}
}
