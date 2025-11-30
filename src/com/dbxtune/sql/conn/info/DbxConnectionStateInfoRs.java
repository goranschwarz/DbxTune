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
package com.dbxtune.sql.conn.info;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.RsConnection;

public class DbxConnectionStateInfoRs
implements DbxConnectionStateInfo
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public String  _mode    = "";
	public String  _quiesce = "";
	public String  _status  = "";

	private static final String NORMAL_MODE    = "NORMAL";
	private static final String NORMAL_QUIESCE = "FALSE";
	private static final String NORMAL_STATUS  = "HEALTHY";
	
	public DbxConnectionStateInfoRs(DbxConnection conn)
	{
		refresh(conn);
	}
	
	private void refresh(DbxConnection conn)
	{
		String sql = "admin health";

		// Do the work
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				_mode    = rs.getString(1).trim();
				_quiesce = rs.getString(2).trim();
				_status  = rs.getString(3).trim();
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			if (conn instanceof RsConnection)
			{
				if ( ((RsConnection)conn).isInGatewayMode(3) )
				{
					_mode    = "GATEWAY";
					_quiesce = "UNKNOWN";
					_status  = ((RsConnection)conn).getLastGatewaySrvName();
					return;
				}
			}
			_logger.error("Error in DbxConnectionStateInfoRs.refresh()", sqle);
		}
	}

	
	
	
	
	
	@Override
	public boolean isNormalState()
	{
		return ( NORMAL_MODE.equals(_mode) && NORMAL_STATUS.equals(_status));
	}

	@Override
	public String getWaterMarkText()
	{
		if ( isNormalState() )
			return null;

		if ( ! NORMAL_MODE.equals(_mode)   ) return "In mode: "+_mode; 
		if ( "SUSPECT"    .equals(_status) ) return "RS Threads are down: status="+_status;

		return null;
	}

	@Override
	public String getStatusBarText()
	{
		String mode    = "Mode="    + _mode;
		String quiesce = "Quiesce=" + _quiesce;
		String status  = "Status="  + _status;

		if ( ! NORMAL_MODE   .equals(_mode)    ) mode    = "Mode=<b><font color='red'>"     + _mode    + "</font></b>";
		if ( ! NORMAL_QUIESCE.equals(_quiesce) ) quiesce = "Quiesce=<b><font color='blue'>" + _quiesce + "</font></b>";
		if ( ! NORMAL_STATUS .equals(_status)  ) status  = "Status=<b><font color='red'>"   + _status  + "</font></b>";

		String text = "<html>"
				+ mode    + ", "
				+ quiesce + ", "
				+ status  + 
				"</html>";

		return text;
	}

	@Override
	public String getStatusBarToolTipText()
	{
		String tooltip = "<html>" +
				"<table border=0 cellspacing=0 cellpadding=1>" +
				"<tr> <td>Mode:    </td> <td><b>" + _mode    + "</b> </td> </tr>" +
				"<tr> <td>Quiesce: </td> <td><b>" + _quiesce + "</b> </td> </tr>" +
				"<tr> <td>Status:  </td> <td><b>" + _status  + "</b> </td> </tr>" +
				"</table>" +
				"<hr>" + 
				"Various status for the current Replication Server." +
				"</html>";

		return tooltip;
	}
}
