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

	/** The scenario the next statements belong to (statistics), also shown to the DBMS with {@link #setOperation(String)} */
	public LgSession setScenario(String name)       { _scenario     = name; setOperation(name); return this; }
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
		sql = tagInside(sql);
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
		sql = tagInside(sql);
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
		sql = tagInside(sql);
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

	private String _operation;      // the last operation set
	private String _operationShown; // the value the DBMS shows for it

	/**
	 * Tell the DBMS what this session is doing now, in a field a monitor (DbxTune) can see for every session:
	 * <ul>
	 *   <li>SQL Server: <code>set context_info</code> (sys.dm_exec_sessions/requests.context_info, SqlServerTune column context_info_str)</li>
	 *   <li>ASE: <code>set clientname</code> (sysprocesses.clientname, monProcessLookup.ClientName; max 30 chars)</li>
	 *   <li>Postgres: <code>application_name</code> = appName:operation (pg_stat_activity.application_name; max 63 chars)</li>
	 *   <li>Oracle: <code>DBMS_APPLICATION_INFO</code>: MODULE = the app name, ACTION = the operation, CLIENT_INFO = "dbxt:&lt;operation&gt;"
	 *       (V$SESSION.MODULE/ACTION/CLIENT_INFO; max 48/32/64 chars). PROGRAM is the app name (JDBC property v$session.program at connect).</li>
	 *   <li>MySQL: the user variable <code>@dbxt_operation</code> (performance_schema.user_variables_by_thread). program_name is the app name (connectionAttributes at connect).</li>
	 *   <li>DB2: client accounting info (MON_GET_CONNECTION.CLIENT_ACCTNG). CLIENT_APPLNAME is the app name, CLIENT_USERID the login (JCC properties at connect).</li>
	 * </ul>
	 * The value is "dbxt:&lt;operation&gt;" (Postgres: the app name + ":&lt;operation&gt;"). Only sent when it changes;
	 * it is not counted in the statistics, and a failure is only logged (debug).
	 */
	public void setOperation(String operation)
	{
		if (operation == null || operation.equals(_operation) || _closed)
			return;
		String sql, shown;
		switch (_lg.getVendor())
		{
		case SQLSERVER:
		{
			shown = ("dbxt:" + operation);
			byte[] b = shown.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
			StringBuilder hex = new StringBuilder("set context_info 0x");
			for (int i=0; i<Math.min(b.length, 128); i++)
				hex.append(String.format("%02x", b[i]));
			sql = hex.toString();
			break;
		}
		case ASE:
		{
			String v = ("dbxt:" + operation).replace("'", "");
			shown = v.length() > 30 ? v.substring(0, 30) : v;
			sql = "set clientname '" + shown + "'";
			break;
		}
		case POSTGRES:
		{
			// The app name may already end with the scenario (problem sessions: dbxt-loadgen:blocking), don't repeat it
			String last = _appName.substring(_appName.lastIndexOf(':') + 1);
			String v = (operation.equals(last)             ? _appName
			         :  operation.startsWith(last + ":")   ? _appName + operation.substring(last.length())
			         :                                       _appName + ":" + operation).replace("'", "");
			shown = v.length() > 63 ? v.substring(0, 63) : v;
			sql = "set application_name = '" + shown + "'";
			break;
		}
		case ORACLE:
		{
			// Binds, not literals: one cursor for all values (literals would be a hard parse per new operation)
			String v = "dbxt:" + operation;
			shown = v.length() > 64 ? v.substring(0, 64) : v;
			try (CallableStatement cs = _conn.prepareCall("begin dbms_application_info.set_module(?, ?); dbms_application_info.set_client_info(?); end;"))
			{
				cs.setString(1, _appName.length() > 48 ? _appName.substring(0, 48) : _appName);
				cs.setString(2, operation.length() > 32 ? operation.substring(0, 32) : operation);
				cs.setString(3, shown);
				cs.execute();
				_operation      = operation;
				_operationShown = shown;
			}
			catch (SQLException ex)
			{
				_logger.debug("setOperation('" + operation + "') failed on " + getId() + ": " + ex.getMessage());
			}
			return;
		}
		case MYSQL:
		{
			// No settable "client info" field: a user variable, visible to a monitor in performance_schema.user_variables_by_thread
			shown = ("dbxt:" + operation).replace("'", "");
			sql = "set @dbxt_operation = '" + shown + "'";
			break;
		}
		case DB2:
		{
			// JCC sends client info with the next request (no round trip of its own): MON_GET_CONNECTION.CLIENT_ACCTNG
			shown = "dbxt:" + operation;
			try
			{
				_conn.setClientInfo("ClientAccountingInformation", shown);
				_operation      = operation;
				_operationShown = shown;
			}
			catch (SQLException ex)
			{
				_logger.debug("setOperation('" + operation + "') failed on " + getId() + ": " + ex.getMessage());
			}
			return;
		}
		default:
			return;
		}
		try (Statement stmt = _conn.createStatement())
		{
			stmt.execute(sql);
			_operation      = operation;
			_operationShown = shown;
		}
		catch (SQLException ex)
		{
			_logger.debug("setOperation('" + operation + "') failed on " + getId() + ": " + ex.getMessage());
		}
	}
	/** The value the DBMS shows for the current operation (context_info, clientname, application_name, client_info), null if none */
	public String getOperationShown() { return _operationShown; }

	/**
	 * Move a leading scenario tag (<code>/&#42; dbxt:x &#42;/ select ...</code>) to after the first word (<code>select /&#42; dbxt:x &#42;/ ...</code>).
	 * SQL Server keeps only the statement text in dm_exec_query_stats / Query Store, which starts AFTER a leading comment,
	 * so a tag in front is lost there (seen 2026-11-07). Inside the statement it is kept on all vendors.
	 */
	static String tagInside(String sql)
	{
		if (sql == null || !sql.startsWith("/* dbxt:"))
			return sql;
		int end = sql.indexOf("*/");
		if (end < 0)
			return sql;
		String tag  = sql.substring(0, end + 2);
		String rest = sql.substring(end + 2).stripLeading();
		int    sp   = 0;
		while (sp < rest.length() && Character.isLetter(rest.charAt(sp)))
			sp++;
		if (sp == 0) // not a keyword first (e.g. "{call ...}"): leave it as it is
			return sql;
		return rest.substring(0, sp) + " " + tag + rest.substring(sp);
	}

	/** A plain statement without parameters (literal SQL, set options, DDL), all results are read and thrown away */
	public int exec(String sql)
	throws SQLException
	{
		sql = tagInside(sql);
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
