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
package com.dbxtune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.sql.conn.info.DbmsVersionInfoSybaseRs;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfo;
import com.dbxtune.sql.conn.info.DbxConnectionStateInfoRs;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.Ver;

public class RsConnection
extends TdsConnection
{
	private static Logger _logger = Logger.getLogger(RsConnection.class);

	private String _lastGatewayServer = "";
	
	public RsConnection(Connection conn)
	{
		super(conn);
		Ver.majorVersion_mustBeTenOrAbove = true;
//System.out.println("constructor::RsConnection(conn): conn="+conn);
	}

	@Override
	public DbmsVersionInfo createDbmsVersionInfo()
	{
		return new DbmsVersionInfoSybaseRs(this);
	}

	@Override
	public DbxConnectionStateInfo refreshConnectionStateInfo()
	{
		DbxConnectionStateInfo csi = new DbxConnectionStateInfoRs(this);
		setConnectionStateInfo(csi);
		return csi;
	}

	@Override
	public boolean isInTransaction() throws SQLException
	{
		return false; // FIXME: Don't know how to check this, so lets assume FALSE
	}

	@Override
	public long getDbmsVersionNumber()
	{
		return AseConnectionUtils.getRsVersionNumber(this);
	}

	@Override
	public boolean isDbmsClusterEnabled()
	{
		return false;
	}

	/**
	 * Check if we are connected in "gateway mode" (connect rssd) or similar
	 * @return 
	 */
	public void closeGatewayMode()
	{
		String sql =  "disconnect";
		try
		{
			Statement stmnt = createStatement();

			stmnt.setQueryTimeout(10);
			stmnt.executeUpdate(sql);
			stmnt.close();

			_lastGatewayServer = "";
		}
		catch (SQLException ex)
		{
			_logger.warn("closeGatewayMode() had problems. sql='"+sql+"'.", ex);
		}
	}
	/**
	 * Note: Updated by isInGatewayMode()...
	 * @return
	 */
	public String getLastGatewaySrvName()
	{
		return _lastGatewayServer;
	}
	/**
	 * Check if we are connected in "gateway mode" (connect rssd) or similar
	 * @return 
	 */
	public boolean isInGatewayMode()
	{
		return isInGatewayMode(3);
	}
	/**
	 * Check if we are connected in "gateway mode" (connect rssd) or similar
	 * @param timeout
	 * @return 
	 */
	public boolean isInGatewayMode(int timeout)
	{
		_lastGatewayServer = "";
		
		String sql =  "show server"; // Note show server do not return a ResultSet in sends the output as "messages"
		try
		{
			Statement stmnt = createStatement();

			stmnt.setQueryTimeout(timeout);
			stmnt.executeUpdate(sql);

			// read Msg...
			SQLWarning sqlw = stmnt.getWarnings();
			String msg = "";
			while (sqlw != null)
			{
				msg += sqlw.getMessage();
				msg = msg.trim();

				sqlw = sqlw.getNextWarning();
			}
			_lastGatewayServer = msg;

			stmnt.close();

			// true = connection is alive, NOT Closed
			return true;
		}
		catch (SQLException ex)
		{
			// If RS Says 'Incorrect syntax', then we are not connected in Gateway-mode
			//-----------------------------------------------------------
			// Msg 2056, Level 12, State 0:
			// Server 'PROD_REP':
			// Line 1, character 1: Incorrect syntax with 'show'.
			//-----------------------------------------------------------
			if (ex.getErrorCode() == 2056)
				return false;
			
			_logger.warn("isInGatewayMode("+timeout+") had problems. sql='"+sql+"'.", ex);
			return false;
		}
	}

	@Override
	public boolean isValid(int timeout) throws SQLException
	{
//System.out.println("RsConnection.isValid("+timeout+"): was called");
		if (timeout < 0)
			throw new SQLException("The passed timeout value of '"+timeout+"' must be 0 or above.");

		String sql =  "admin echo, 'DbxConnection-Rs-isValid("+timeout+")'";
		
		// If we KNOW that we are in gateway mode... issue a simple SQL Statement, so we don't get a error message:
		//         Msg 2812, Level 16, State 5:
		//         Server 'XXXXXXX', Line 1, Status 0, TranState 3:
		//         Stored procedure 'admin' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
		if (StringUtil.hasValue(_lastGatewayServer))
			sql = "select 1";
		
		try
		{
			Statement stmnt = createStatement();

			stmnt.setQueryTimeout(timeout);
			ResultSet rs = stmnt.executeQuery(sql);

			while (rs.next())
			{
				rs.getString(1);
			}
			rs.close();
			stmnt.close();

			// true = connection is alive, NOT Closed
			return true;
		}
		catch (SQLException ex)
		{
			// connection is already closed...
			if ( "JZ0C0".equals(ex.getSQLState()) )
			{
				return false;
			}

			// Check if we are in "gateway mode" ... "connect to rssd" has been issued 
			if (isInGatewayMode(timeout))
				return true;
			
			_logger.warn("isValid("+timeout+") had problems. sql='"+sql+"'.", ex);
			return false;
		}
	}

	@Override
	protected int getDbmsSessionId_impl() throws SQLException
	{
//		return -1;

		// There is no command to get current spid (that I know of)
		// So lets try using 'admin who', checking for 'USER' which is 'Active' right now

		String sql = "admin who";
		//-------------------------------------------------------------------------------
		// admin who 
		// go filter "where Name='USER' and State = 'Active'"
		//-------------------------------------------------------------------------------
		// Filter information: visibleRows=1, actualRows=67, filterText='where Name='USER' and State = 'Active''.
		// +----+----+------+----+
		// |Spid|Name|State |Info|
		// +----+----+------+----+
		// | 230|USER|Active|sa  |
		// +----+----+------+----+
		// Rows 1
		// (67 rows affected)
		
		int spid = -1;
		try (Statement stmnt = _conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
//			go filter "where Name='USER' and State = 'Active'"

			while(rs.next())
			{
				String name  = rs.getString(2);
				String state = rs.getString(3);
				
				if ("USER".equals(name) && "Active".equals(state))
					spid = rs.getInt(1);
			}
		}
		
		return spid;
	}
}
