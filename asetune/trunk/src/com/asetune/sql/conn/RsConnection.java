package com.asetune.sql.conn;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.info.DbxConnectionStateInfo;
import com.asetune.sql.conn.info.DbxConnectionStateInfoRs;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.SqlUtils;
import com.asetune.utils.Ver;
import com.sybase.jdbcx.EedInfo;

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
	public int getDbmsVersionNumber()
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
}
