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
package com.asetune.sql.conn.info;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;

public class DbxConnectionStateInfoGenericJdbc
implements DbxConnectionStateInfo
{
	private static Logger _logger = Logger.getLogger(DbxConnectionStateInfoGenericJdbc.class);

	public String  _catalog        = "";
	public boolean _autocommit     = true;
	public int     _isolationLevel = -1;
	public boolean _inTransaction  = false;

	protected String isolationLevelToString(int isolation)
	{
		switch (isolation)
		{
			case Connection.TRANSACTION_READ_UNCOMMITTED: return "0=READ_UNCOMMITTED";
			case Connection.TRANSACTION_READ_COMMITTED:   return "1=READ_COMMITTED";
			case Connection.TRANSACTION_REPEATABLE_READ:  return "2=REPEATABLE_READ";
			case Connection.TRANSACTION_SERIALIZABLE:     return "3=SERIALIZABLE";
			case Connection.TRANSACTION_NONE:             return "NONE";

			default:
				return "TRANSACTION_ISOLATION_UNKNOWN_STATE("+isolation+")";
		}
	}

	public String getCatalog()
	{
		return _catalog;
	}

	public boolean getAutoCommit()
	{
		return _autocommit;
	}

	public boolean isInTransaction()
	{
		return _inTransaction;
	}

	public String getIsolationLevelStr()
	{
		return isolationLevelToString(_isolationLevel);
	}

	
	public DbxConnectionStateInfoGenericJdbc(DbxConnection conn)
	{
		refresh(conn);
	}
	
	private void refresh(DbxConnection conn)
	{
		// Do the work
		try { _catalog        = conn.getCatalog();              } catch (SQLException sqle) { _logger.error("Error in DbxConnectionStateInfoGenericJdbc.refresh(getCatalog)"             , sqle); }
		try { _autocommit     = conn.getAutoCommit();           } catch (SQLException sqle) { _logger.error("Error in DbxConnectionStateInfoGenericJdbc.refresh(getAutoCommit)"          , sqle); }
		try { _isolationLevel = conn.getTransactionIsolation(); } catch (SQLException sqle) { _logger.error("Error in DbxConnectionStateInfoGenericJdbc.refresh(getTransactionIsolation)", sqle); }
		try { _inTransaction  = conn.isInTransaction();         } catch (SQLException sqle) { _logger.error("Error in DbxConnectionStateInfoGenericJdbc.refresh(isInTransaction)"        , sqle); }
	}

	
	@Override
	public boolean isNormalState()
	{
		return ! isInTransaction();
	}

	@Override
	public String getWaterMarkText()
	{
		if ( isInTransaction() )
		{
			String str = "NOTE: You are currently in a TRANSACTION!\n"
			           + "Don't forget to commit or rollback!";
			return str;
		}
		return null;
	}

	@Override
	public String getStatusBarText()
	{
		String catalog    = "cat="        + _catalog;
		String isolation  = "Isolation="  + getIsolationLevelStr();
		String autocommit = "AutoCommit=" + getAutoCommit();

		if ( ! getAutoCommit() )
			autocommit = "AutoCommit=<b><font color=\"red\">" + getAutoCommit() + "</font></b>";

		String text = "ac="+getAutoCommit();
		if ( ! getAutoCommit() )
		{
			text = "<html>"
				+ autocommit + ", "
				+ catalog    + ", " 
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
				"<tr> <td>Current Catalog: </td> <td><b>" + getCatalog()           + "</b> </td> </tr>" +
				"<tr> <td>AutoCommit:      </td> <td><b>" + getAutoCommit()        + "</b> </td> </tr>" +
				"<tr> <td>Isolation Level: </td> <td><b>" + getIsolationLevelStr() + "</b> </td> </tr>" +
				"</table>" +
				"<hr>" + 
				"Various status for the current connection. Are we in a transaction or not." +
				"</html>";

		return tooltip;
	}
}
