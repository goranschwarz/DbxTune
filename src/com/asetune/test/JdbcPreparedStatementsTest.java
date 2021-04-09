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
package com.asetune.test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Properties;

public class JdbcPreparedStatementsTest
{
	private Connection _conn = null;
	private HashMap<String, PreparedStatement> _pstmntMap = null;

	public JdbcPreparedStatementsTest()
	{
	}

	/**
	 * Simply connect to dataserver
	 * @return
	 * @throws Exception
	 */
	public Connection connect(String jdbcDriver, String jdbcUrl, String user, String passwd, Properties jdbcProps )
	throws ClassNotFoundException, SQLException
	{
		Class.forName(jdbcDriver);

		Properties props = new Properties(jdbcProps);
		if (props.getProperty("user")            == null) props.put("user",            user);
		if (props.getProperty("password")        == null) props.put("password",        passwd);
//		if (props.getProperty("APPLICATIONNAME") == null) props.put("APPLICATIONNAME", _appname);
//		if (props.getProperty("HOSTNAME")        == null) props.put("HOSTNAME",        hostname);
		
		Connection conn = null;
		conn = DriverManager.getConnection(jdbcUrl, props);

		DatabaseMetaData dbmd = conn.getMetaData();
		if (dbmd != null)
		{
			System.out.println("JDBC driver version: " + dbmd.getDriverVersion());
		}

		// make strings unaware of quotation ""
		// meaning SQL statements like: print "some string" will work...
		try
		{
			if (conn.getAutoCommit() == false)
			{
				//_logger.info("Autocommit was turned 'off'. I will turn this to 'on' for this connection.");
				conn.setAutoCommit(true);
			}

			// If this is not set to 'off', things like (print "any string") wont work
			conn.createStatement().execute("set quoted_identifier off");
		}
		catch (SQLException sqle)
		{
			String errStr = "";
			while (sqle != null)
			{
				errStr += sqle.getMessage() + " ";
				sqle = sqle.getNextException();
			}
			System.out.println("Failed to execute 'set quoted_identifier off' when connecting. Problem: "+errStr);
		}

		// Create a new cache map, when new connection
		_pstmntMap = new HashMap<String, PreparedStatement>();

		_conn = conn;
		return conn;
	}

	/**
	 * Close connection to dataserver
	 */
	public void close()
	{
		try
		{
			if (_conn != null)
				_conn.close();
		}
		catch(SQLException ignore) {}
		
		_conn = null;
	}

	/**
	 * Print some information about Dynamic SQL Statistics
	 */
	public void printDynamicSqlStat()
	{
		String sql = 
			"select SPID=convert(int,substring(ObjectName,2,6)), StmntId=convert(int,substring(ObjectName,8,8)), MemUsageKB \n" +
			"into #xxx \n" +
			"from master..monCachedProcedures  \n" +
			"where ObjectName like '*%' \n" +
			"  and ObjectName not like '*ss%' \n" +
			" \n" +
			"select SPID, NumOfDynSqlStat = count(*), MaxStmnId=max(StmntId), SumKbPerSpid=sum(MemUsageKB) \n" +
			"from #xxx \n" +
			"group by SPID \n" +
			"order by 2 desc \n" +
			" \n" +
			"drop table #xxx \n";

		try
		{
			Connection conn = getConnection();
			Statement stmnt = conn.createStatement();

			ResultSet rs = stmnt.executeQuery(sql);

			System.out.format("SPID   NumOfDynSqlStat MaxStmnId SumKbPerSpid\n");
			System.out.format("------ --------------- --------- ------------\n");
			int rows = 0;
			while (rs.next())
			{
				rows++;
				int SPID            = rs.getInt(1);
				int NumOfDynSqlStat = rs.getInt(2);
				int MaxStmnId       = rs.getInt(3);
				int SumKbPerSpid    = rs.getInt(4);

				System.out.format("%6d %15d %9d %12d\n", 
					SPID, NumOfDynSqlStat, MaxStmnId, SumKbPerSpid);
			}
			if (rows == 0)
				System.out.format("---- no rows ----\n");
			
//			ResultSetTableModel tm = new ResultSetTableModel(rs);
//			String tabOut = tm.toTableString();
//			System.out.println(tabOut);

			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Problems printDynamicSqlStat(). sql="+sql);
			e.printStackTrace();
		}
	}

	public int getSpid()
	{
		int spid = -1;
		String sql = "select @@spid";

		try
		{
			Connection conn = getConnection();
			Statement stmnt = conn.createStatement();

			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				spid = rs.getInt(1);
			}

			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Problems getSpid(). sql="+sql);
			e.printStackTrace();
		}
		
		return spid;
	}

	public String getsrvVersion()
	{
		String version = "";
		String sql = "select @@version";

		try
		{
			Connection conn = getConnection();
			Statement stmnt = conn.createStatement();

			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
			{
				version = rs.getString(1);
			}

			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			System.err.println("Problems getsrvVersion(). sql="+sql);
			e.printStackTrace();
		}
		
		return version;
	}

	public Connection getConnection()
	{
		return _conn;
	}

	private PreparedStatement getPreparedStatement(String sql)
	throws SQLException
	{
		PreparedStatement pstmnt = _pstmntMap.get(sql);
		if (pstmnt == null)
		{
			System.out.println("DEBUG: GENERATING a new PreparedStatement for SQL: "+sql);
			Connection conn = getConnection();
			pstmnt = conn.prepareStatement(sql);

			_pstmntMap.put(sql, pstmnt);
		}
		return pstmnt;
	}

	public Timestamp getCrDateReuse(String tabname)
	{
		String sql = 
			"select crdate \n" +
			"from sysobjects \n" +
			"where name = ? \n";

		try
		{
			PreparedStatement pstmnt = getPreparedStatement(sql);
			pstmnt.setString(1, tabname);

			Timestamp crdate = null;

			ResultSet rs = pstmnt.executeQuery();
			while (rs.next())
			{
				crdate = rs.getTimestamp(1);
			}
			rs.close();
			
			return crdate;
		}
		catch (SQLException e)
		{
			System.err.println("Problems loading 'crdate' for table '"+tabname+"'. sql="+sql);
			e.printStackTrace();
			return null;
		}
	}

	public Timestamp getCrDate(String tabname, boolean closePrepStmnt)
	{
		String sql = 
			"select crdate \n" +
			"from sysobjects \n" +
			"where name = ? \n";

		try
		{
			Connection conn = getConnection();
			PreparedStatement pstmnt = conn.prepareStatement(sql);
			pstmnt.setString(1, tabname);

			Timestamp crdate = null;

			ResultSet rs = pstmnt.executeQuery();
			while (rs.next())
			{
				crdate = rs.getTimestamp(1);
			}
			rs.close();
			if (closePrepStmnt)
				pstmnt.close();
			
			return crdate;
		}
		catch (SQLException e)
		{
			System.err.println("Problems loading 'crdate' for table '"+tabname+"'. sql="+sql);
			e.printStackTrace();
			return null;
		}
	}

	private static void test(JdbcPreparedStatementsTest jt, int testCase, int subCase, int num, Properties jdbcProps, String jdbcDriver, String jdbcUrl, String jdbcUser, String jdbcPasswd)
	throws Exception
	{
		boolean closePrepStmnt      = true;
		boolean closePrepStmnt50Pct = false;

		if (subCase == 1) closePrepStmnt      = true;
		if (subCase == 2) closePrepStmnt      = false;
		if (subCase == 3) closePrepStmnt50Pct = true;

		System.out.println();
		System.out.println("############ TEST "+testCase+":"+subCase+" ############## ---BEGIN---");
		System.out.println("PROPS="+jdbcProps);

		jt.connect(jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd, jdbcProps);
		System.out.println("ASE Version used in this test is: "+jt.getsrvVersion());
		System.out.println("SPID used in this test is: "+jt.getSpid());
		if (testCase == 3)
		{
			System.out.println("RUNNING getCrDateReuse()");
		}

		long startTime = System.currentTimeMillis();
		for (int i=0; i<num; i++)
		{
//			if (closePrepStmnt50Pct)
//				System.out.println("jt.getCrDate(\"syscomments\", "+(i % 2 == 0) );

//			if (closePrepStmnt50Pct)
//				jt.getCrDate("syscomments", i % 2 == 0);

			if (testCase == 3)
			{
				jt.getCrDateReuse("syscomments");
			}
			else
			{
				if (closePrepStmnt50Pct)
				{
					if (i < (num/2))
						jt.getCrDate("syscomments", true);
					else
						jt.getCrDate("syscomments", false);
				}
				else
					jt.getCrDate("syscomments", closePrepStmnt);
			}
		}
		long stopTime = System.currentTimeMillis();
		System.out.println("Exec time for "+num+" iterations: "+(stopTime-startTime)+" ms. closePrepStmnt="+closePrepStmnt+", closePrepStmnt50Pct="+closePrepStmnt50Pct);
		
		jt.printDynamicSqlStat();
		jt.close();
		System.out.println("                    ############## ---END---");
	}

/*
c: 
cd C:\projects\asetune
java -cp classes;lib/jconn3.jar com.asetune.test.JdbcPreparedStatementsTest ston60238837a 15702 sa sybase
 */
	public static void main(String[] args)
	{
		System.out.println("Usage: hostname port user passwd");
		String host = "localhost";
		String port = "5000";
		String user = "sa";
		String pawd = "";
		
		if (args.length > 0) host = args[0];
		if (args.length > 1) port = args[1];
		if (args.length > 2) user = args[2];
		if (args.length > 3) pawd = args[3];

		System.out.println("host = '"+host+"'");
		System.out.println("port = '"+port+"'");
		System.out.println("user = '"+user+"'");
		System.out.println("pawd = '"+pawd+"'");
		
		String jdbcDriver    = "com.sybase.jdbc42.jdbc.SybDriver";
		String jdbcUrl       = "jdbc:sybase:Tds:"+host+":"+port;
		String jdbcUser      = user;
		String jdbcPasswd    = pawd;
		Properties jdbcProps = null;
		JdbcPreparedStatementsTest jt = new JdbcPreparedStatementsTest();
		try
		{
			int num = 10000;
			int testNum = 0;

			System.out.println("----------- TEST: START");
			
			testNum++;
			jdbcProps = new Properties();
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			testNum++;
			jdbcProps = new Properties();
			jdbcProps.put("DYNAMIC_PREPARE", "true");
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			// Reuse of the LW Procedure, created by DYNAMIC_PREPARE
			testNum++;
			jdbcProps = new Properties();
			jdbcProps.put("DYNAMIC_PREPARE", "true");
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			testNum++;
			jdbcProps = new Properties();
			jdbcProps.put("DYNAMIC_PREPARE", "false");
			jdbcProps.put("SQLINITSTRING",   "set statement_cache on");
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			testNum++;
			jdbcProps = new Properties();
			jdbcProps.put("DYNAMIC_PREPARE", "false");
			jdbcProps.put("SQLINITSTRING",   "set statement_cache off");
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			testNum++;
			jdbcProps = new Properties();
			jdbcProps.put("LITERAL_PARAMS", "true");
			jdbcProps.put("SQLINITSTRING",   "set statement_cache on");
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			testNum++;
			jdbcProps = new Properties();
			jdbcProps.put("LITERAL_PARAMS", "true");
			jdbcProps.put("SQLINITSTRING",  "set statement_cache off");
			test(jt, testNum, 1, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 2, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);
			test(jt, testNum, 3, num, jdbcProps, jdbcDriver, jdbcUrl, jdbcUser, jdbcPasswd);

			System.out.println("----------- TEST: END");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
