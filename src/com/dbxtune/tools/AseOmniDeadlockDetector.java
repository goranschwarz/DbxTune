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
package com.dbxtune.tools;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.AseConnectionUtils;

public class AseOmniDeadlockDetector
extends Thread
{
	private enum ServerSide {Left, Right};
	private enum SpidType {RootCause, Victim};
	
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private boolean    _isRunning = true;
	private int        _sleepTime = 10; // In seconds
	private boolean    _hasDeadlock = false;

	private Connection _leftConn = null;
	private Connection _rightConn = null;

	private String  _leftAseUsername   = "sa";
	private String  _leftAsePassword   = "";
	private String  _leftAseHostname   = null;
	private int     _leftAsePort       = -1;
	private boolean _leftKillRootCause = false;
	
	private String  _rightAseUsername   = "sa";
	private String  _rightAsePassword   = "";
	private String  _rightAseHostname   = null;
	private int     _rightAsePort       = -1;
	private boolean _rightKillRootCause = false;
	
	private class RootCauseEntry
	{
		public int    _spid;
		public String _status;
		
		@Override
		public String toString()
		{
			return "spid="+_spid+", status='"+_status+"'.";
		}
	}
	private class VictimEntry
	{
		public int    _spid;
		public int    _originRemoteSpid;
		public String _status;
		public String _originAse;

		@Override
		public String toString()
		{
			return "spid="+_spid+", originRemoteSpid="+_originRemoteSpid+", status='"+_status+"', originAse='"+_originAse+"'.";
		}
	}
	
	private Connection connect(String host, int port, String username, String password)
	throws SQLException, ClassNotFoundException
	{
		return AseConnectionFactory.getConnection(host, port, null, username, password, "AseOmniDeadlockDetector", null, null);
	}

	public void connectLeft()
	throws SQLException, ClassNotFoundException
	{
		_logger.info("Connecting to left server:  host='"+_leftAseHostname+"', port="+_leftAsePort+", user='"+_leftAseUsername+"'.");
		_leftConn = connect(_leftAseHostname, _leftAsePort, _leftAseUsername, _leftAsePassword);
	}

	public void connectRight()
	throws SQLException, ClassNotFoundException
	{
		_logger.info("Connecting to right server:  host='"+_rightAseHostname+"', port="+_rightAsePort+", user='"+_rightAseUsername+"'.");
		_rightConn = connect(_rightAseHostname, _rightAsePort, _rightAseUsername, _rightAsePassword);
	}

	public void setLeftConnectionProp(String host, int port, String username, String password, boolean killRootCouseSpid)
	{
		_leftAseHostname = host;
		_leftAsePort     = port;
		_leftAseUsername = username;
		_leftAsePassword = password;
		
		_leftKillRootCause = killRootCouseSpid;
	}
	public void setRightConnectionProp(String host, int port, String username, String password, boolean killRootCouseSpid)
	{
		_rightAseHostname = host;
		_rightAsePort     = port;
		_rightAseUsername = username;
		_rightAsePassword = password;
		
		_rightKillRootCause = killRootCouseSpid;
	}
	
	private String execSql(Connection conn, String sql)
	{
		try
		{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			
			ResultSetTableModel tm = new ResultSetTableModel(rs, sql);
			_logger.info("sql: "+sql+"\n"+tm.toTableString());
	
			rs.close();
			stmt.close();
			
			return tm.toTableString();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems executing sql: "+sql, e);
			return "Problems executing sql: "+sql;
		}
	}

	private void printDeadlockInfo(Connection conn, int spid, ServerSide srvSide, SpidType type)
	{
		_logger.info("=====================================================================");
		_logger.info(" printDeadlockInfo, type="+type+", ServerSide="+srvSide+", SPID="+spid+".");
		_logger.info("---------------------------------------------------------------------");

		String res = "";

		//-----------------------
		res = execSql(conn, "select * from master..sysprocesses where spid = " + spid);
		_logger.info("SYSPROCESSES: \n"+res);

		//-----------------------
		res = execSql(conn, "select * from master..syslocks where spid = " + spid);
		_logger.info("SYSLOCKS: \n"+res);
				
		//-----------------------
		res = AseConnectionUtils.dbccSqlText(conn, spid, false);
		_logger.info("DBCC SQL TEXT: \n"+res);

		//-----------------------
		res = AseConnectionUtils.monSqlText(conn, spid, false);
		_logger.info("MON SQL TEXT: \n"+res);

		//-----------------------
		res = AseConnectionUtils.getShowplan(conn, spid, null, false);
		_logger.info("SHOWPLAN: \n"+res);

		//-----------------------
		res = AseConnectionUtils.monProcCallStack(conn, spid, false);
		_logger.info("PROC CALL STACK: \n"+res);

		_logger.info("---------------------------------------------------------------------");
	}

	public boolean hasDeadlock()
	{
		return _hasDeadlock;
	}

	private void killSpid(Connection conn, int spid)
	{
		String sql = "kill "+spid;
	
		try
		{
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			stmt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems executing sql: "+sql, e);
		}
	}
	
	public ArrayList<RootCauseEntry> getRootCause(Connection conn)
	throws SQLException
	{
		_logger.debug("getRootCause(): conn="+conn);

		String sql = 
			"select * \n" +
			"from master..sysprocesses \n" +
			"where blocked = 0 \n" +
			"  and status  = 'remote i/o' \n" +
			"  and spid in (select blocked from master..sysprocesses where blocked > 0) \n";

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		
		ArrayList<RootCauseEntry> list = new ArrayList<RootCauseEntry>();
		while (rs.next())
		{
			RootCauseEntry entry = new RootCauseEntry();

			entry._spid   = rs.getInt   ("spid");
			entry._status = rs.getString("status");

			System.out.println("getRootCause: "+entry);
			list.add(entry);
		}

		rs.close();
		stmt.close();
		
		return list;
	}

	public ArrayList<VictimEntry> getVictim(Connection conn)
	throws SQLException
	{
		_logger.debug("getVictim(): conn="+conn);

		String sql = 
			"select OriginRemoteSpid = substring(program_name, char_length('OmniServer-')+1,99), \n" +
			"       OriginAse = hostname, \n" +
			"* \n" +
			"from master..sysprocesses \n" +
			"where blocked > 0 \n" +
			"  and program_name like 'OmniServer-%' \n";

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);

		ArrayList<VictimEntry> list = new ArrayList<VictimEntry>();
		while (rs.next())
		{
			VictimEntry entry = new VictimEntry();

			entry._spid               = rs.getInt   ("spid");
			entry._originRemoteSpid   = rs.getInt   ("OriginRemoteSpid");
			entry._status             = rs.getString("status");
			entry._originAse          = rs.getString("OriginAse");

			System.out.println("getVictim: "+entry);
			list.add(entry);
		}

		rs.close();
		stmt.close();
		
		return list;
	}
	
	public void check()
	{

		try
		{
			_hasDeadlock = false;

			ArrayList<RootCauseEntry> leftRootCause  = getRootCause(_leftConn);
			ArrayList<VictimEntry>    leftVictim     = getVictim(   _leftConn);

			ArrayList<RootCauseEntry> rightRootCause = getRootCause(_rightConn);
			ArrayList<VictimEntry>    rightVictim    = getVictim(   _rightConn);

			if ( leftRootCause.size() == 0 || rightRootCause.size() == 0 )
				return;

			// CHECK: left -> right
			HashSet<Integer> left = new HashSet<Integer>();
			for (RootCauseEntry rootCauseEntry : leftRootCause)
			{
				for (VictimEntry victimEntry : rightVictim)
				{
					if (rootCauseEntry._spid == victimEntry._originRemoteSpid)
					{
						_hasDeadlock = true;
						left.add(rootCauseEntry._spid);
					}
				}
			}

			// CHECK: right -> left
			HashSet<Integer> right = new HashSet<Integer>();
			for (RootCauseEntry rootCauseEntry : rightRootCause)
			{
				for (VictimEntry victimEntry : leftVictim)
				{
					if (rootCauseEntry._spid == victimEntry._originRemoteSpid)
					{
						_hasDeadlock = true;
						right.add(rootCauseEntry._spid);
					}
				}
			}

			_logger.warn("SPID: Root Cause on left  server: " + left);
			_logger.warn("SPID: Root Cause on right server: " + right);

			//----------------------------
			// PRINT INFO
			for (Integer spid : left)
				printDeadlockInfo(_leftConn, spid, ServerSide.Left, SpidType.RootCause);

			for (Integer spid : right)
				printDeadlockInfo(_rightConn, spid, ServerSide.Right, SpidType.RootCause);

			//----------------------------
			// KILL
			if (_leftKillRootCause)
				for (Integer spid : left)
					killSpid(_leftConn, spid);

			if (_rightKillRootCause)
				for (Integer spid : right)
					killSpid(_rightConn, spid);
		}
		catch (SQLException e)
		{
			_logger.warn("Problems in check()", e);
		}
	}

	@Override
	public void run()
	{
		setName("AseOmniDeadlockDetector");
		_isRunning = true;

		_logger.info("Starting ASE Deadlock Detector.");

		while (_isRunning)
		{
			try
			{
				_logger.info("Checking for Inter Server Deadlock.");
				check();
//				if (hasDeadlock())
//					printDeadlockInfo();
			}
			catch (Throwable t)
			{
				_logger.info("Caught Exception, but I will continue anyway...", t);
			}

			try { Thread.sleep(_sleepTime * 1000); }
			catch(InterruptedException ignore) {}
		}

		_logger.info("ASE Deadlock Detector has now been stopped.");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// Set Log4j Log Level
//		Configurator.setRootLevel(Level.TRACE);

//		String  lHost   = System.getenv("DD_LEFT_HOST");
//		String  lPort   = System.getenv("DD_LEFT_PORT");
//		String  lUser   = System.getenv("DD_LEFT_USER");
//		String  lPasswd = System.getenv("DD_LEFT_PASSWD");
//		boolean lKill   = System.getenv("DD_LEFT_KILL") != null;
//
//		String  rHost   = System.getenv("DD_RIGHT_HOST");
//		String  rPort   = System.getenv("DD_RIGHT_PORT");
//		String  rUser   = System.getenv("DD_RIGHT_USER");
//		String  rPasswd = System.getenv("DD_RIGHT_PASSWD");
//		boolean rKill   = System.getenv("DD_LEFT_KILL") != null;
//
//		if (lHost   == null) {System.out.println("Sorry no env variable 'DD_LEFT_HOST'."); return;}
//		if (lPort   == null) {System.out.println("Sorry no env variable 'DD_LEFT_PORT'."); return;}
//		if (lUser   == null) {System.out.println("Sorry no env variable 'DD_LEFT_USER'."); return;}
//		if (lPasswd == null) {System.out.println("Sorry no env variable 'DD_LEFT_PASSWD'."); return;}
//
//		if (rHost   == null) {System.out.println("Sorry no env variable 'DD_RIGHT_HOST'."); return;}
//		if (rPort   == null) {System.out.println("Sorry no env variable 'DD_RIGHT_PORT'."); return;}
//		if (rUser   == null) {System.out.println("Sorry no env variable 'DD_RIGHT_USER'."); return;}
//		if (rPasswd == null) {System.out.println("Sorry no env variable 'DD_RIGHT_PASSWD'."); return;}

		String  lHost   = "gorans-xp";
		String  lPort   = "12540";
		String  lUser   = "sa";
		String  lPasswd = "";
		boolean lKill   = true;

		String  rHost   = "gorans-xp";
		String  rPort   = "5000";
		String  rUser   = "sa";
		String  rPasswd = "";
		boolean rKill   = false;

		
		AseOmniDeadlockDetector dd = new AseOmniDeadlockDetector();
		
		dd.setLeftConnectionProp (lHost, Integer.parseInt(lPort), lUser, lPasswd, lKill);
		dd.setRightConnectionProp(rHost, Integer.parseInt(rPort), rUser, rPasswd, rKill);

		try
		{
			dd.connectLeft();
			dd.connectRight();
		}
		catch (Exception ex)
		{
			_logger.error("Problems Connecting to servers.", ex);
			return;
		}

		dd.start();
	}
}
