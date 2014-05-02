package com.asetune.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.JTable;

import org.apache.log4j.PropertyConfigurator;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.Ver;
import com.sybase.jdbc4.jdbc.SybDriver;
import com.sybase.jdbcx.SybMessageHandler;

public class StressConnect
{
	public String     _host      = "localhost";
	public String     _port      = "5000";
	public String     _dbname    = "tempdb";
	public String     _username  = "sa";
	public String     _password  = "sybase";
	public String     _appname   = "StressConnect";
	public String     _hostname  = null;
	public String     _execFile  = null;
	public Properties _connProps = null;

	public int        _numOfThreads = 10;

	public boolean    _running   = true;
	public long       _connSleepTime = 10;

	public long       _statConnectAttempts = 0;
	public long       _statConnectSuccess  = 0;
	public long       _statConnectFailed   = 0;
	public long       _statConnectTime     = 0;

	public long       _statSleepTime         = 10 * 1000;
	public boolean    _statDoSpinMon         = false;
	public int        _statDoSpinMonRows     = 15;
	public String     _statDoSpinMonUsername = "sa";
	public String     _statDoSpinMonPassword = "sybase";
	public Connection _statDoSpinMonConn     = null;
	public int        _statSrvVersion        = 0;
	
//	private DbMessageHandler _msgHandler = new DbMessageHandler();
	
	//----------------------------------------------------------------------------------------------------------
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		try
		{
			initJdbcDriver();
			StressConnect stress = new StressConnect(args);
			stress.firstTestConnect();
			stress.statisticsConnect(); // only connects if it's enabled
			stress.start();
		}
		catch(Exception e)
		{
			System.out.println("Problems when setting up or executing StressConnect.");
			e.printStackTrace();
		}
	}

	public StressConnect(String[] args)
	{
		String configFile = null;
		
		if (args.length == 0)
		{
			System.out.println("Usage: StressConnect propFile");
			configFile = "StressConnect.props";
			File propFile = new File(configFile);
			if ( ! propFile.exists() )
			{
				try
				{
					// Create file 	
					BufferedWriter out = new BufferedWriter(new FileWriter(propFile));
					out.write("# ASE Connection information\n");
					out.write("ase.host      = "+_host+"\n");
					out.write("ase.port      = "+_port+"\n");
					out.write("#ase.dbname   = tempdb\n");
					out.write("ase.username  = "+_username+"\n");
					out.write("ase.password  = "+_password+"\n");
					out.write("#ase.appname  = "+_appname+"\n");
					out.write("#ase.hostname = someClientName\n");
					out.write("\n");
					out.write("# If we want to exec something in the database, place it in this file\n");
					out.write("#exec.filename = filename.sql\n");
					out.write("\n");
					out.write("# Number of java threads cuncurrently that will try to connect in a loop\n");
					out.write("worker.threads = "+_numOfThreads+"\n");
					out.write("\n");
					out.write("# How many millisecond to each connect thread sleep after a connect attempt\n");
					out.write("worker.sleepAfterConnect = "+_connSleepTime+"\n");
					out.write("\n");
					out.write("# Sleep time between statistic reports\n");
					out.write("stat.sleepTime = "+_statSleepTime+"\n");
					out.write("\n");
					out.write("# Get Spinlock Monitoring in the statistics report\n");
					out.write("stat.doSpinMon = "+_statDoSpinMon+"\n");
					out.write("\n");
					out.write("# get only top # rows from spinmon\n");
					out.write("stat.doSpinMon.top = "+_statDoSpinMonRows+"\n");
					out.write("\n");
					out.write("# Username and password for user to get spinmon statistics (must have sa_role)\n");
					out.write("stat.doSpinMon.username = "+_statDoSpinMonUsername+"\n");
					out.write("stat.doSpinMon.password = "+_statDoSpinMonPassword+"\n");
					out.write("\n");

					out.close();
					
					System.out.println("Created a properties file '"+propFile.getCanonicalPath()+"', which will be used for configuration.");
					System.out.println("");
					System.out.println("Please edit the file with server host, port, username & password...");
					System.out.println("");
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				System.exit(1);
			}
			else
			{
				try { System.out.println("Using default properties file '"+propFile.getCanonicalPath()+"'."); }
				catch (IOException e) { e.printStackTrace(); }
			}
		}
		else
		{
			System.out.println("Using properties file '"+configFile+"' for configuration.");
		}
		
		Configuration config = new Configuration(configFile);
		Configuration.setInstance("MAIN", config);
		
		_host     = config.getProperty("ase.host",     _host);
		_port     = config.getProperty("ase.port",     "5000");
		_dbname   = config.getProperty("ase.dbname",   null);
		_username = config.getProperty("ase.username", "sa");
		_password = config.getProperty("ase.password", "");
		_appname  = config.getProperty("ase.appname",  _appname);
		_hostname = config.getProperty("ase.hostname", _hostname);
		
		_execFile = config.getProperty("exec.filename", _execFile);

		_numOfThreads  = config.getIntProperty("worker.threads", _numOfThreads);
		_connSleepTime = config.getIntProperty("worker.sleepAfterConnect", (int)_connSleepTime);

		_statSleepTime         = config.getIntProperty    ("stat.sleepTime",     (int)_statSleepTime);

		_statDoSpinMon         = config.getBooleanProperty("stat.doSpinMon",          _statDoSpinMon);
		_statDoSpinMonRows     = config.getIntProperty    ("stat.doSpinMon.top",      _statDoSpinMonRows);
		_statDoSpinMonUsername = config.getProperty       ("stat.doSpinMon.username", _statDoSpinMonUsername);
		_statDoSpinMonPassword = config.getProperty       ("stat.doSpinMon.password", _statDoSpinMonPassword);
		
		
		System.out.println("############# CONFIGURATION ######################");
		System.out.println("ase.host     = "+_host);
		System.out.println("ase.port     = "+_port);
		System.out.println("ase.dbname   = "+_dbname);
		System.out.println("ase.username = "+_username);
		System.out.println("ase.password = "+_password);
		System.out.println("ase.appname  = "+_appname);
		System.out.println("ase.hostname = "+_hostname);
		System.out.println("--------------------------------------------------");
		System.out.println("exec.filename = "+_execFile);
		System.out.println("--------------------------------------------------");
		System.out.println("worker.threads           = " + _numOfThreads);
		System.out.println("worker.sleepAfterConnect = " + _connSleepTime);
		System.out.println("--------------------------------------------------");
		System.out.println("stat.sleepTime           = " + _statSleepTime);
		System.out.println("stat.doSpinMon           = " + _statDoSpinMon);
		System.out.println("stat.doSpinMon.top       = " + _statDoSpinMonRows);
		System.out.println("##################################################");
		System.out.println("");
	}
	

	public void firstTestConnect()
	throws Exception
	{
		System.out.println("Trying to do initial Test Login to '"+_host+":"+_port+"' with user '"+_username+"'.");
		
		//Connection conn = AseConnectionFactory.getConnection(_host, _port, _dbname, _username, _password, _appname, _hostname, _connProps);
		Connection conn = jdbcConnect(_host, _port, _dbname, _username, _password, _appname, _hostname, _connProps);


		// Get active roles and print those
		List<String> activeRoles = AseConnectionUtils.getActiveRoles(conn);
		
		System.out.println("Initial Test Login to '"+_host+":"+_port+"' with user '"+_username+"' SUCCEEDED.");
		System.out.println("Active roles for the user is: "+StringUtil.toCommaStr(activeRoles));
	}

	public void statisticsConnect()
	throws Exception
	{
		// Only connect if this is enabled
		if ( ! _statDoSpinMon )
			return;

		System.out.println("Statistics: Connecting to '"+_host+":"+_port+"' with user '"+_statDoSpinMonUsername+"'.");
		
//		Connection conn = AseConnectionFactory.getConnection(_host, _port, _dbname, _statDoSpinMonUsername, _statDoSpinMonPassword, "StressConnStatistics", _hostname, _connProps);
		Connection conn = jdbcConnect(_host, _port, _dbname, _statDoSpinMonUsername, _statDoSpinMonPassword, "StressConnStatistics", _hostname, _connProps);

		// Get active roles and print those
		List<String> activeRoles = AseConnectionUtils.getActiveRoles(conn);
		if ( ! activeRoles.contains("sa_role") )
		{
			throw new Exception("Statistics: user '"+_statDoSpinMonUsername+"', must have 'sa_role'. list of current roles: "+StringUtil.toCommaStr(activeRoles));
		}
		
		// Get server version, spinmon use different queries
		_statSrvVersion = AseConnectionUtils.getAseVersionNumber(conn);
		
		System.out.println("Statistics: Connecting to '"+_host+":"+_port+"' with user '"+_statDoSpinMonUsername+"' SUCCEEDED.");
		_statDoSpinMonConn = conn;
	}

	public void start()
	{
		// Start of the clients
		for (int i=0; i<_numOfThreads; i++)
		{
			DbClient dbc = new DbClient();
			dbc.start();
		}

		// Monitor and print statistics
		long intervall      = 0;

		long lastSampleTime      = System.currentTimeMillis();
		long lastConnectAttempts = 0;
		long lastConnectSuccess  = 0;
		long lastConnectFailed   = 0;
		long lastConnectTime     = 0;
		
		int  lastAtAtConnections = 0;
		if (_statDoSpinMonConn != null)
			lastAtAtConnections = statGetAtAtConnections();

		while(true)
		{
			if (intervall == 0)
			{
				System.out.println("Sleeping for '"+_statSleepTime+"' ms between Statistics sample.");
				System.out.println("DB Worker sleeps '"+_connSleepTime+"' ms between connect's");
			}

			if (_statDoSpinMon)
				spinMonReset();

			////////////////////////////////////////////////////////
			// SLEEP // SLEEP // SLEEP // SLEEP // SLEEP // SLEEP //   
			////////////////////////////////////////////////////////
			try { Thread.sleep(_statSleepTime); }
			catch(InterruptedException ignore) {}


			if (_statDoSpinMon)
				spinMonPopulate();

			long   timeNow    = System.currentTimeMillis();
			String timeNowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeNow));

			// Calculate how long since last stat print
			intervall = timeNow - lastSampleTime;

			long connectAttempts = _statConnectAttempts - lastConnectAttempts;
			long connectSuccess  = _statConnectSuccess  - lastConnectSuccess;
			long connectFailed   = _statConnectFailed   - lastConnectFailed;
			long connectTime     = _statConnectTime     - lastConnectTime;
			
			int  atAtConnectionsDiff = 0;
			if (_statDoSpinMonConn != null)
			{
				int atAtConnectionsNow = statGetAtAtConnections();

				atAtConnectionsDiff = atAtConnectionsNow - lastAtAtConnections;
				lastAtAtConnections = atAtConnectionsNow;
			}

			lastSampleTime      = System.currentTimeMillis();
			lastConnectAttempts = _statConnectAttempts;
			lastConnectSuccess  = _statConnectSuccess;
			lastConnectFailed   = _statConnectFailed;
			lastConnectTime     = _statConnectTime;


			// Print statistics
			System.out.println("----------------------------- threads='"+_numOfThreads+"', intervall='"+intervall+"' ------ at: " + timeNowStr);
			System.out.println("connectAttempts:             "+connectAttempts);
			System.out.println("connectSuccess:              "+connectSuccess);
			System.out.println("connectFailed:               "+connectFailed);
			System.out.println("@@connections diff:          "+atAtConnectionsDiff);
			System.out.println("connectTime:                 "+connectTime);
			System.out.println("connectAttempts per second:  "+connectAttempts     / (intervall/1000.0));
			System.out.println("connectSuccess  per second:  "+connectSuccess      / (intervall/1000.0));
			System.out.println("connectFailed   per second:  "+connectFailed       / (intervall/1000.0));
			System.out.println("@@connections   per second:  "+atAtConnectionsDiff / (intervall/1000.0));
			System.out.println("connectTime(ms) per connect: "+ ( connectSuccess > 0 ? connectTime/connectSuccess : "no-succesfull-connects"));

			if (_statDoSpinMon)
			{
				String spinMonStr = spinMonCalculate(_statDoSpinMonRows);
				System.out.println("SPINMON output: (first "+_statDoSpinMonRows+" rows)");
				System.out.println(spinMonStr);
			}
		}
	}


	private int statGetAtAtConnections()
	{
		String sql = "select @@connections";
		int connections = 0;
		
		try
		{
			Statement stmnt = _statDoSpinMonConn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while (rs.next())
				connections = rs.getInt(1);

			stmnt.close();
			rs.close();
		}
		catch (SQLException e)
		{
			System.out.println("ERROR: statGetAtAtConnections() caught: "+e);
		}
		return connections;
	}

	private void spinMonReset()
	{
		String sql =
			"dbcc monitor('clear', 'all', 'on') \n" +
			"dbcc monitor('clear', 'spinlock_s', 'on') \n";
		
		try
		{
//			System.out.println("spinMonReset() SQL: \n"+sql);
			Statement stmnt = _statDoSpinMonConn.createStatement();
			stmnt.executeUpdate(sql);
			stmnt.close();
		}
		catch (SQLException e)
		{
			_statDoSpinMon = false;
			System.out.println("ERROR: spinMonReset() caught: "+e);
			System.out.println("ERROR: spinMonReset() turning spinmon OFF");
		}
	}
	private void spinMonPopulate()
	{
		String sql = 
			"dbcc monitor('sample', 'spinlock_s', 'off') \n" +
			"dbcc monitor('sample', 'all', 'off') \n" +
			"dbcc monitor('select', 'spinlock_s', 'on') \n" +
			"dbcc monitor('select', 'all', 'on') \n";

		try
		{
//			System.out.println("spinMonPopulate() SQL: \n"+sql);
			Statement stmnt = _statDoSpinMonConn.createStatement();
			stmnt.executeUpdate(sql);
			stmnt.close();
		}
		catch (SQLException e)
		{
			_statDoSpinMon = false;
			System.out.println("ERROR: spinMonPopulate() caught: "+e);
			System.out.println("ERROR: spinMonPopulate() turning spinmon OFF");
		}
	}
	private String spinMonCalculate(int firstXrows)
	{
		String spinP = "spinlock_p_0";
		String spinW = "spinlock_w_0";
		String spinS = "spinlock_s_0";
		
		if ( _statSrvVersion >= Ver.ver(15, 7) )
		{
			spinP = "spinlock_p";
			spinW = "spinlock_w";
			spinS = "spinlock_s";
		}
		
		String sql = 
			"/* \n" +
			"** The spinlocks are displayed as 'name::id'. name is the \n" +
			"** name passed into ulinitspinlock(). For single instance \n" +
			"** spinlocks id will be 0, for array spinlocks id corresponds \n" +
			"** to the order the spinlocks were intialised in, with 0 being the first. \n" +
			"*/ \n" +
			" \n" +
			"/* Get the number of transactions */ \n" +
			"declare @xacts float \n" +
			"select @xacts = value from sysmonitors \n" +
			"	where group_name = 'access' and field_name='xacts' \n" +
			" \n" +
			"if @xacts = 0 \n" +
			"begin \n" +
			"	select @xacts = 1 /* avoid divide by zero errors */ \n" +
			"end \n" +
			" \n" +
//			"select @xacts 'Number of xacts' \n" +
			"print 'Number of xacts = %1!', @xacts\n" +
			" \n" +
			"print '' \n" +
			"print 'Spinlocks with contention - ordered by percent contention' \n" +
			"print '' \n" +
			" \n" +
			"select  \n" +
			"	rtrim(P.field_name) + '::' + convert(char(5), P.field_id - (select min(field_id) from sysmonitors where field_name = P.field_name)) as spinlock, \n" +
			"	P.value as grabs,  \n" +
			"	W.value as waits,  \n" +
			"	(100 * W.value)/P.value as wait_percent, \n" +
			"	S.value / W.value as spins_per_wait, S.value as total_spins, \n" +
			"	P.value / @xacts as grabs_per_xact \n" +
			"from sysmonitors P, sysmonitors W, sysmonitors S \n" +
			"where P.group_name = '"+spinP+"' \n" +
			"  and W.group_name = '"+spinW+"' \n" +
			"  and S.group_name = '"+spinS+"' \n" +
			"  and P.field_id = W.field_id \n" +
			"  and P.field_id = S.field_id \n" +
			"  and W.field_id =  S.field_id \n" +
			"  and W.value != 0 \n" +
			"order by wait_percent desc \n" +
//			"compute sum(P.value), sum(P.value / @xacts) \n" +
			" \n" +
			" \n" +
			" \n" +
//			"print '' \n" +
//			"print 'Spinlocks with no contention - ordered by number of grabs' \n" +
//			"print '' \n" +
//			" \n" +
//			"select  \n" +
//			"	rtrim(P.field_name) + '::' + convert(char(5), P.field_id - (select min(field_id) from sysmonitors where field_name = P.field_name)) as spinlock, \n" +
//			"	P.value grabs, \n" +
//			"	P.value / @xacts as grabs_per_xact \n" +
//			"from sysmonitors P, sysmonitors W \n" +
//			"where P.group_name = '"+spinP+"' \n" +
//			"  and W.group_name = '"+spinW+"' \n" +
//			"  and P.field_id = W.field_id \n" +
//			"  and P.value > 1 	/* one because getting the stats gets the spinlock */ \n" +
//			"  and W.value = 0 \n" +
//			"order by grabs desc \n" +
//			"compute sum(P.value), sum(P.value / @xacts) \n" +
			"";

		try
		{
//			System.out.println("spinMonCalculate() SQL: \n"+sql);
			Statement stmnt = _statDoSpinMonConn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			ResultSetTableModel rstm = new ResultSetTableModel(rs, false, "SpinMon", firstXrows, false, null, null);
//			ResultSetTableModel rstm = new ResultSetTableModel(rs, "SpinMon");
			stmnt.close();
			rs.close();

//			return rstm.toTableString();
			return SwingUtils.tableToString(new JTable(rstm));
		}
		catch (SQLException e)
		{
			_statDoSpinMon = false;
			System.out.println("ERROR: spinMonCalculate() caught: "+e);
			System.out.println("ERROR: spinMonCalculate() turning spinmon OFF");
		}
		return "NO RESULTS FROM spinMonCalculate(): _statDoSpinMon="+_statDoSpinMon;
	}

	//----------------------------------------------------------------------------------------------------------
	private static void initJdbcDriver() 
	throws SQLException
	{
//		System.out.println("initJdbcDriver(): BEFORE: CurrentLoadedDrivers:");
//		Enumeration<Driver> drivers = DriverManager.getDrivers();
//		while (drivers.hasMoreElements())
//		{
//			Driver driver = (Driver) drivers.nextElement();
//			System.out.println("        - driver toString  = '"+driver.toString()+"'\n" +
//			                   "                 className = '"+driver.getClass().getName()+"'.");
//		}
		
		System.out.println("initJdbcDriver(): Creating new SybDriver - with UserDefined MessageHandler");
		SybDriver sybDriver = new SybDriver();
		sybDriver.setSybMessageHandler( new DbMessageHandler() );
		
		// Dow we nned to register this one... dont think so, but here is the code anyway
		//DriverManager.registerDriver(sybDriver);
		
//		System.out.println("initJdbcDriver(): AFTER: CurrentLoadedDrivers:");
//		drivers = DriverManager.getDrivers();
//		while (drivers.hasMoreElements())
//		{
//			Driver driver = (Driver) drivers.nextElement();
//			System.out.println("        - driver toString  = '"+driver.toString()+"'\n" +
//			                   "                 className = '"+driver.getClass().getName()+"'.");
//		}
	}

	private static Connection jdbcConnect(String host, String port, String dbname, String username, String password, String appname, String hostname, Properties connProps)
	throws SQLException
	{
		if (connProps == null)
			connProps = new Properties();

		if (username != null) connProps.put("user",            username);
		if (password != null) connProps.put("password",        password);
		if (dbname   != null) connProps.put("DATABASE",        dbname);
		if (appname  != null) connProps.put("APPLICATIONNAME", appname);
		if (hostname != null) connProps.put("HOSTNAME",        hostname);

		Connection conn = DriverManager.getConnection("jdbc:sybase:Tds:"+host+":"+port, connProps);
		return conn;
	}

	private static class DbMessageHandler
	implements SybMessageHandler
	{
		@Override
		public SQLException messageHandler(SQLException sqle)
		{
			// Write out Print messages
			if (sqle.getErrorCode() == 0)
				System.out.println("SybMessageHandler: " + StringUtil.removeLastNewLine(sqle.getMessage()) );

			//System.out.println("SybMessageHandler: ThreadId="+Thread.currentThread().getId() + ": Msg="+sqle.getErrorCode()+", SqlState="+sqle.getSQLState()+", Desc="+StringUtil.removeLastNewLine(sqle.getMessage()));
			// If in shutdown mode, UPGRADE SQLWarning to SQLException 
			// Msg 6002, A SHUTDOWN command is already in progress. Please log off.
			if (sqle.getErrorCode() == 6002)
				return new SQLException(StringUtil.removeLastNewLine(sqle.getMessage()), sqle.getSQLState(), sqle.getErrorCode(), sqle);

			return sqle;
		}
	}

	/**
	 * WORKER CLASS THAT DOES THE JOB<br>
	 * It will loop and do connect/close
	 */
	private class DbClient
	extends Thread
	{
		@Override
		public void run()
		{
			while (_running)
			{
				connectAndClose();
				
				if (_connSleepTime > 0)
				{
    				try { Thread.sleep(_connSleepTime); }
    				catch(InterruptedException ignore) {}
				}
			}
		}
		
		private void connectAndClose()
		{
			try
			{
				_statConnectAttempts++;
				long startTime = System.currentTimeMillis();

				//Connection conn = AseConnectionFactory.getConnection(_host, _port, _dbname, _username, _password, _appname, _hostname, _connProps);
				Connection conn = jdbcConnect(_host, _port, _dbname, _username, _password, _appname, _hostname, _connProps);

				// Install a message handler, that will "upgrade" SQLWarning->SQLException for "SHUTDOWN in progress"...
//				((SybConnection)conn).setSybMessageHandler(_msgHandler);

				_statConnectSuccess++;
				long endTime = System.currentTimeMillis();
				_statConnectTime += (endTime - startTime);

				//List<String> activeRoles = AseConnectionUtils.getActiveRoles(conn);
				//String servername = AseConnectionUtils.getAseServername(conn);

				// Dummy statement
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select getdate()");
				while (rs.next())
				{
					@SuppressWarnings("unused")
					String xxx = rs.getString(1);
				}
				rs.close();
				stmt.close();

				
				// Execute some stuff
				if (_execFile != null)
				{
					AseSqlScript script = new AseSqlScript(conn, 100);
					script.execute(_execFile);
					script.close();
				}
				
				conn.close();
			}
//			catch (ClassNotFoundException e)
//			{
//				_statConnectFailed++;
//
//				e.printStackTrace();
//			}
			catch (SQLException e)
			{
				_statConnectFailed++;

				//e.printStackTrace();
				System.out.println("ThreadId="+Thread.currentThread().getId() + ": PROBLEMS in connectAndClose(), Caught: "+e);
			}
		}
	}
}
