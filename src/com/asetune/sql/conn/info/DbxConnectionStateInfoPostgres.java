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
package com.asetune.sql.conn.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;

public class DbxConnectionStateInfoPostgres
extends DbxConnectionStateInfoGenericJdbc
{
	private static Logger _logger = Logger.getLogger(DbxConnectionStateInfoPostgres.class);

	public String _sessionUser    = "";
	public String _currentUser    = "";
	public String _currentCatalog = "";
	public String _currentSchema  = "";
	public int    _backendPid     = -1;

	public int    _lockCount      = -1;
//	public List<LockRecord> _lockList = new ArrayList<LockRecord>();

	public DbxConnectionStateInfoPostgres(DbxConnection conn)
	{
		super(conn);
		refresh(conn);
	}

	private void refresh(DbxConnection conn)
	{
		String sql = "select session_user, current_user, current_catalog, current_schema, pg_backend_pid()";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_sessionUser    = rs.getString(1);
				_currentUser    = rs.getString(2);
				_currentCatalog = rs.getString(3);
				_currentSchema  = rs.getString(4);
				_backendPid     = rs.getInt   (5);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("Error in refresh() problems executing sql='"+sql+"'.", ex);
		}

		
		_lockCount = 0;
//		_lockList.clear();

		sql = "SELECT count(*) FROM pg_locks WHERE pid=pg_backend_pid() AND locktype='transactionid' AND mode='ExclusiveLock' AND granted";
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
			{
				_lockCount = rs.getInt(1);
			}
		}
		catch (SQLException ex)
		{
			_logger.error("Error in refresh() problems executing sql='"+sql+"'.", ex);
		}

	}

	@Override
	public String getWaterMarkText()
	{
		String str = null;

		if (_lockCount >= 1)
		{
			str = "You are in a TRANSACTION (AutoCommit=false)\n"
				+ "And you are holding " + _lockCount + " locks in the server\n"
				+ "Don't forget to commit or rollback!";
		}
		
		return str;
	}

	@Override
	public String getStatusBarText()
	{
		boolean autocommitBool = getAutoCommit();
		
		String bpid        = "bpid=<b>"        + _backendPid       + "</b>";
//		String login       = "login=<b>"       + _sessionUser      + "</b>";
//		String user        = "user=<b>"        + _currentUser      + "</b>";
		String cat         = "cat=<b>"         + _currentCatalog   + "</b>";
		String schema      = "schema=<b>"      + _currentSchema    + "</b>";
		String ac          = "ac=<b>"          + autocommitBool    + "</b>";

		String loginUser   = "";
		if (_sessionUser.equals(_currentUser))
			loginUser = "user=<b>" + _currentUser + "</b>";
		else
			loginUser = "login=<b>" + _sessionUser + "</b>, user=<b>" + _currentUser + "</b>";
			
		
//		String catalog    = "cat="        + _currentCatalog;
		String isolation  = "Isolation="  + getIsolationLevelStr();
		String autocommit = "AutoCommit=" + autocommitBool;

		if ( ! autocommitBool )
			autocommit = "AutoCommit=<b><font color='red'>" + autocommitBool + "</font></b>";

		// status: Normal state
		String text = "<html>" 
				+ bpid      + ", " 
				+ cat       + ", " 
				+ schema    + ", " 
				+ loginUser + ", " 
				+ ac        +
				"</html>";
//		String text = "ac="+getAutoCommit();

		// status: "problem" state
		if ( ! autocommitBool )
		{
			text = "<html>"
				+ autocommit + ", "
				+ bpid       + ", " 
				+ cat        + ", " 
				+ schema     + ", " 
				+ loginUser  + ", " 
				+ isolation  + 
				"</html>";
		}

		return text;
	}

	@Override
	public String getStatusBarToolTipText()
	{
		String tooltip = "<html>" +
				"<table border=0 cellspacing=0 cellpadding=1>" +
				"<tr> <td>Backend PID:     </td> <td><b>" + _backendPid            + "</b> </td> </tr>" +
				"<tr> <td>Session User:    </td> <td><b>" + _sessionUser           + "</b> </td> </tr>" +
				"<tr> <td>Current User:    </td> <td><b>" + _currentUser           + "</b> </td> </tr>" +
				"<tr> <td>Current Catalog: </td> <td><b>" + _currentCatalog        + "</b> </td> </tr>" +
				"<tr> <td>Current Schema:  </td> <td><b>" + _currentSchema         + "</b> </td> </tr>" +
				"<tr> <td>AutoCommit:      </td> <td><b>" + getAutoCommit()        + "</b> </td> </tr>" +
				"<tr> <td>Isolation Level: </td> <td><b>" + getIsolationLevelStr() + "</b> </td> </tr>" +
				"</table>" +
				"<hr>" + 
				"Various status for the current connection. Are we in a transaction or not." +
				"</html>";

		return tooltip;
	}
}
