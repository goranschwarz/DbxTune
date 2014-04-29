package com.asetune.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.AseSqlScript;
import com.asetune.utils.Configuration;

public class StressConnect
{
	public String     _host      = "localhost";
	public String     _port      = "15702";
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
	public long       _statConnectTime     = 0;

	public long       _statSleepTime = 10 * 1000;
	
	
	//----------------------------------------------------------------------------------------------------------
	public static void main(String[] args)
	{
		StressConnect stress = new StressConnect(args);
		stress.start();
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
					out.write("ase.host      = localhost\n");
					out.write("ase.port      = 5000\n");
					out.write("#ase.dbname   = tempdb\n");
					out.write("ase.username  = sa\n");
					out.write("ase.password  = sybase\n");
					out.write("#ase.appname  = StressConnect\n");
					out.write("#ase.hostname = someClientName\n");
					out.write("\n");
					out.write("# If we want to exec something in the database, place it in this file\n");
					out.write("#exec.filename = filename.sql\n");
					out.write("\n");
					out.write("# Number of java threads cuncurrently that will try to connect in a loop\n");
					out.write("worker.threads = 10\n");
					out.write("\n");
					out.write("# How many millisecond to each connect thread sleep after a connect attempt\n");
					out.write("worker.sleepAfterConnect = 10\n");
					out.write("\n");
					out.write("# Sleep time between statistic reports\n");
					out.write("stat.sleepTime = 10000\n");
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

		_statSleepTime = config.getIntProperty("stat.sleepTime", (int)_statSleepTime);
		
		
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
		System.out.println("##################################################");
		System.out.println("");
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
		long lastConnectTime     = 0;
		
		while(true)
		{
			if (intervall == 0)
			{
				System.out.println("Sleeping for '"+_statSleepTime+"' ms between Statistics sample.");
				System.out.println("DB Worker sleeps '"+_connSleepTime+"' ms between connect's");
			}

			try { Thread.sleep(_statSleepTime); }
			catch(InterruptedException ignore) {}
			
			long   timeNow    = System.currentTimeMillis();
			String timeNowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeNow));

			// Calculate how long since last stat print
			intervall = timeNow - lastSampleTime;

			long connectAttempts = _statConnectAttempts - lastConnectAttempts;
			long connectSuccess  = _statConnectSuccess  - lastConnectSuccess;
			long connectTime     = _statConnectTime     - lastConnectTime;

			System.out.println("----------------------------- threads='"+_numOfThreads+"', intervall='"+intervall+"' ------ at: " + timeNowStr);
			System.out.println("connectAttempts:             "+connectAttempts);
			System.out.println("connectSuccess:              "+connectSuccess);
			System.out.println("connectTime:                 "+connectTime);
			System.out.println("connectAttempts per second:  "+connectAttempts / (intervall/1000.0));
			System.out.println("connectSuccess  per second:  "+connectSuccess  / (intervall/1000.0));
			System.out.println("connectTime(ms) per connect: "+ ( connectSuccess > 0 ? connectTime/connectSuccess : "no-succesfull-connects"));

			lastSampleTime      = System.currentTimeMillis();
			lastConnectAttempts = _statConnectAttempts;
			lastConnectSuccess  = _statConnectSuccess;
			lastConnectTime     = _statConnectTime;

		}
	}


	//----------------------------------------------------------------------------------------------------------
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
			Connection conn = null;

			try
			{
				_statConnectAttempts++;
				long startTime = System.currentTimeMillis();

				conn = AseConnectionFactory.getConnection(_host, _port, _dbname, _username, _password, _appname, _hostname, _connProps);
				
				_statConnectSuccess++;
				long endTime = System.currentTimeMillis();
				_statConnectTime += (endTime - startTime);

				//List<String> activeRoles = AseConnectionUtils.getActiveRoles(conn);
				String servername = AseConnectionUtils.getAseServername(conn);
				
				// Execute some stuff
				if (_execFile != null)
				{
					AseSqlScript script = new AseSqlScript(conn, 100);
					script.execute(_execFile);
					script.close();
				}
				
				conn.close();
			}
			catch (ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
