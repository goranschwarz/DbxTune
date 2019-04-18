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
package com.asetune.tools.sqlw;

import java.awt.Component;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.asetune.gui.ConnectionProfile;
import com.asetune.gui.ConnectionProfile.ConnProfileEntry;
import com.asetune.gui.ConnectionProfile.JdbcEntry;
import com.asetune.gui.ConnectionProfile.TdsEntry;
import com.asetune.gui.ConnectionProfileManager;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.StringUtil;

public class SqlStatementCmdRemoteSql 
extends SqlStatementAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlStatementCmdRemoteSql.class);

	private String[] _args = null;
	private String _originCmd = null;

	// if -p profileName is given... then store the profile's Connection Properties in this one... 
	private ConnectionProfile _rightConnectionProfile = null;

	private DbxConnection _rightConn = null;
	private Statement     _rightStmnt = null;

	private static class CmdParams
	{
		String        _profile   = null;
		String        _user      = null;
		String        _passwd    = null;
		String        _server    = null;
		String        _db        = null;
		String        _url       = null;
//		String        _driver    = null; // not yet used

		String        _initStr        = null;
		String        _sql            = null;

		boolean       _debug           = false;  // addDebugMessage()
		boolean       _trace           = false;  // addTraceMessage()
		boolean       _toStdout        = false;  // if debug/trace is enabled, also print the messages to STDOUT as soon as they happen (may be easier to debug in that way)
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			String passwd = _debug ? _passwd : "*secret*";
			
			sb.append(", ").append("profile       ".trim()).append("=").append(StringUtil.quotify(_profile       ));
			sb.append(", ").append("user          ".trim()).append("=").append(StringUtil.quotify(_user          ));
			sb.append(", ").append("passwd        ".trim()).append("=").append(StringUtil.quotify( passwd        ));
			sb.append(", ").append("server        ".trim()).append("=").append(StringUtil.quotify(_server        ));
			sb.append(", ").append("db            ".trim()).append("=").append(StringUtil.quotify(_db            ));
			sb.append(", ").append("url           ".trim()).append("=").append(StringUtil.quotify(_url           ));
			
			sb.append(", ").append("initStr       ".trim()).append("=").append(StringUtil.quotify(_initStr       ));
			sb.append(", ").append("sql           ".trim()).append("=").append(StringUtil.quotify(_sql           ));

			sb.append(", ").append("debug         ".trim()).append("=").append(StringUtil.quotify(_debug         ));
			sb.append(", ").append("trace         ".trim()).append("=").append(StringUtil.quotify(_trace         ));
			sb.append(", ").append("toStdout      ".trim()).append("=").append(StringUtil.quotify(_toStdout      ));

			return sb.toString();
		}
	}
	private CmdParams _params = new CmdParams();
	

	public SqlStatementCmdRemoteSql(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		super(conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		parse(sqlOrigin);
		init();
	}

	/**
	 * ============================================================
	 * see printHelp() for usage
	 * ------------------------------------------------------------
	 * 
	 * @param input
	 * @return
	 * @throws PipeCommandException
	 */
	public void parse(String input)
	throws SQLException, PipeCommandException
	{
		_originCmd = input;
		String params = input.replace("\\rsql", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);

			if (cmdLine.hasOption('x')) _params._debug           = true;
			if (cmdLine.hasOption('X')) _params._trace           = true;
			if (cmdLine.hasOption('Y')) _params._toStdout        = true;
			
			if (cmdLine.hasOption('U')) _params._user          = cmdLine.getOptionValue('U');
			if (cmdLine.hasOption('P')) _params._passwd        = cmdLine.getOptionValue('P');
			if (cmdLine.hasOption('S')) _params._server        = cmdLine.getOptionValue('S');
			if (cmdLine.hasOption('D')) _params._db            = cmdLine.getOptionValue('D');
			if (cmdLine.hasOption('u')) _params._url           = cmdLine.getOptionValue('u');
			if (cmdLine.hasOption('p')) _params._profile       = cmdLine.getOptionValue('p');

			if (cmdLine.hasOption('s')) _params._sql           = cmdLine.getOptionValue('s');

			if (cmdLine.hasOption('?'))
				printHelp(null, "You wanted help...");
		}
		else
		{
			printHelp(null, "Please specify some parameters.");
		}

		checkParsedParameters(_params);

		if (_params._debug)
			addDebugMessage("CmdLineSwitches: "+_params);
	}

	/**
	 * Check for mandatory parameters etc...
	 * 
	 * @param params
	 * @throws PipeCommandException
	 */
	private void checkParsedParameters(CmdParams params)
	throws PipeCommandException
	{
		// Passed Password == CURRENT ::: get it from the current connection
		if (StringUtil.hasValue(params._passwd) && params._passwd.equals("CURRENT"))
		{
			// hmmm... how can we do this??? (get the DbxConnection and if it has ConnectionProperties, we can extract it from there)
			// FIXME: lets implement that later...
		}

		// -p ::: Get user/passwd/server from the Profile
		if (StringUtil.hasValue(params._profile) && ConnectionProfileManager.hasInstance())
		{
			ConnectionProfile cp = ConnectionProfileManager.getInstance().getProfile(params._profile);
			if (cp == null)
				throw new PipeCommandException("Profile not found in the ProfileManager. profile name '"+params._profile+"'.");
			else
			{
				_rightConnectionProfile = cp;
				
				params._user   = cp.getDbUserName();
				params._passwd = cp.getDbPassword();
				String serverOrUrlStr = cp.getDbServerOrUrl();
				if (serverOrUrlStr != null)
				{
					if (serverOrUrlStr.startsWith("jdbc:"))
						params._url = serverOrUrlStr;
					else
						params._server = serverOrUrlStr;
				}
			}
		}
		
		if (StringUtil.isNullOrBlank(_params._server) && StringUtil.isNullOrBlank(_params._url))
			printHelp(null, "Missing mandatory parameter '-p|--profile <profile>' or '-S|--server <srvName>'.");

		if (StringUtil.isNullOrBlank(_params._sql))
			printHelp(null, "Missing mandatory parameter '-s|--sql <text>'");
	}

	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// Switches       short long Option        hasParam Description (not really used)
		//                ----- ------------------ -------- ------------------------------------------
		options.addOption( "p", "profile",         true,    "Profile to get '-U -P -S|-u' from." );
		options.addOption( "U", "user",            true,    "Username when connecting to server." );
		options.addOption( "P", "passwd",          true,    "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",          true,    "Server to connect to (SERVERNAME|host:port)." );
		options.addOption( "D", "dbname",          true,    "Database name in server." );
		options.addOption( "u", "url",             true,    "Destination DB URL (if not ASE and -S)" );

		options.addOption( "s", "sql",             true,    "sql");
		options.addOption( "i", "initSql",         true,    "");

		options.addOption( "x", "debug",           false,   "debug" );
		options.addOption( "X", "trace",           false,   "trace" );
		options.addOption( "Y", "stdout",          false,   "print debug messages to Stdout" );

		try
		{
			_params = new CmdParams();
			
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

			if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
//			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
//			{
//				if (cmd.hasOption('x'))
//					; // if option 'x' we don't need any parameters
//				else
//				{
//					String error = "Missing string to use for 'graph' or 'chart' command.";
//					printHelp(options, error);
//				}
//			}
			if ( cmd.getArgs() != null && cmd.getArgs().length > 1 )
			{
				String error = "To many options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			return cmd;
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
			return null;
		}	
	}
	private static void printHelp(Options options, String errorStr)
	throws PipeCommandException
	{
		StringBuilder sb = new StringBuilder();

		if (StringUtil.hasValue(errorStr))
		{
			sb.append("\n");
			sb.append(errorStr).append("\n");
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("usage: \\rsql ... \n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -U,--user <user>          Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd \n");
		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port). \n");
		sb.append("  -D,--dbname <dbname>      Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>    Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -p,--profile <name>       Take user/passwd/url from a connection profile.\n");
		sb.append("\n");
		sb.append("  -s,--sql                  SQL Statement to execute on remote connection.\n");
		sb.append("  -i,--initSql              SQL Statement to initialize the connection.\n");
		sb.append("\n");
		sb.append("  -x,--debug                Debug, print some extra info \n");
		sb.append("  -X,--trace                Trace, print some extra info (more than debug)\n");
		sb.append("  -Y,--stdout               Print debug/trace messages to stdout as well\n");
		sb.append("  \n");
		sb.append("  \n");
		sb.append("Some extra info:\n");
		sb.append("  FIXME: describe what we do in here\n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- Check the version of another ASE (in the below case a Sybase ASE)\n");
		sb.append("    \\rsql -Usa -Psecret -Shost:port -Ddbname -s 'select @@servername, @@version, db_name()'\n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- Same as above but with a profile\n");
		sb.append("    \\rsql -p 'PROD_1B_DS' -Ddbname -s 'select @@servername, @@version, db_name()'\n");
		sb.append("\n");
		sb.append("\n");

		throw new PipeCommandException(sb.toString());
	}
	
	private void init()
	throws SQLException
	{
		if (_params._debug)    { setMessageDebugLevel(1);  }
		if (_params._trace)    { setMessageDebugLevel(2);  }
		if (_params._toStdout) { setMessageToStdout(true); }
		
//		System.out.println("SqlStatementCmdDdlGen.init(): _sqlOrigin = " + _sqlOrigin);
	}

	@Override
	public Statement getStatement()
	throws SQLException
	{
		try
		{
			// Connect to RIGHT hand side
			if (_progress != null) _progress.setState("Connecting to Remote DBMS");
			if (_params._debug)     addDebugMessage(  "Connecting to Remote DBMS");
			_rightConn = getRightConnection();

			// Create Statement to RIGHT hand side
			if (_progress != null) _progress.setState("Create Statement at Remote Connection");
			if (_params._debug)     addDebugMessage(  "Create Statement at Remote Connection");
			_rightStmnt = _rightConn.createStatement();
			
			return _rightStmnt;
		}
		catch (Exception ex)
		{
			throw new SQLException("Problems connecting to remote DBMS, Caught: "+ex, ex);
		}
	}

	@Override
	public boolean execute() throws SQLException
	{
		String sql = _params._sql;

		if (_progress != null) _progress.setState("Executing SQL at Remote DBMS: "+sql);
		if (_params._debug)     addDebugMessage(  "Executing SQL at Remote DBMS: "+sql);

		return _rightStmnt.execute(sql);
	}

	@Override
	public void close()
	{
		if (_rightStmnt != null)
		{
			addDebugMessage("Closing Remote Statement.");
			
			try { _rightStmnt.close(); }
			catch (SQLException ignore) {}
		}
		
		// close RIGHT Connection
		if (_rightConn != null)
		{
			addDebugMessage("Closing Remote Connection.");
			
			try { _rightConn.close(); }
			catch (SQLException ignore) {}
		}
	}
	

	private DbxConnection getRightConnection()
	throws Exception
	{
		DbxConnection conn = null;
		ConnectionProp cp = new ConnectionProp();
		cp.setAppName("sqlw-rsql");
		
		if (_rightConnectionProfile == null)
		{
			cp.setDbname(_params._db);
			cp.setPassword(_params._passwd);
			cp.setServer(_params._server);
//			cp.setSshTunnelInfo(_params.);
			cp.setUrl(_params._url);
//			cp.setUrlOptions(urlOptions);
			cp.setUsername(_params._user);
		}
		else
		{
			ConnProfileEntry profileEntry = _rightConnectionProfile.getEntry();
			
			if (profileEntry instanceof TdsEntry)
			{
				TdsEntry entry = (TdsEntry) profileEntry;
				
				cp.setDbname       (entry._tdsDbname);
				cp.setPassword     (entry._tdsPassword);
				cp.setServer       (entry._tdsServer);
				cp.setSshTunnelInfo(entry._tdsShhTunnelUse ? entry._tdsShhTunnelInfo : null);
				cp.setUrl          (entry._tdsUseUrl ? entry._tdsUseUrlStr : null);
				cp.setUrlOptions   (entry._tdsUrlOptions);
				cp.setUsername     (entry._tdsUsername);
			}
			else if (profileEntry instanceof JdbcEntry)
			{
				JdbcEntry entry = (JdbcEntry) profileEntry;
				
//				cp.setDbname       (entry._jdbcDbname);
				cp.setPassword     (entry._jdbcPassword);
//				cp.setServer       (entry._jdbcServer);
				cp.setSshTunnelInfo(entry._jdbcShhTunnelUse ? entry._jdbcShhTunnelInfo : null);
				cp.setUrl          (entry._jdbcUrl);
				cp.setUrlOptions   (entry._jdbcUrlOptions);
				cp.setUsername     (entry._jdbcUsername);
			}
		}
		
		
		if (StringUtil.hasValue(_params._server))
		{
			String hostPortStr = null;
			if ( _params._server.contains(":") )
				hostPortStr = _params._server;
			else
				hostPortStr = AseConnectionFactory.getIHostPortStr(_params._server);

			if (StringUtil.isNullOrBlank(hostPortStr))
				throw new Exception("Can't find server name information about '"+_params._server+"', hostPortStr=null. Please try with -S hostname:port");

			_params._url = "jdbc:sybase:Tds:" + hostPortStr;

			if ( ! StringUtil.isNullOrBlank(_params._db) )
				_params._url += "/" + _params._db;
			cp.setUrl(_params._url);
		}


		// Try to connect
		if (isDebugEnabled())
			addDebugMessage("Try getConnection to: " + cp);
		
		// Make the connection
		conn = DbxConnection.connect(getGuiOwnerAsWindow(), cp);
		
		// Change catalog... (but do not bail out on error)
		if ( ! StringUtil.isNullOrBlank(_params._db) )
		{
			try { conn.setCatalog(_params._db); }
			catch(SQLException ex) { addErrorMessage("Changing database/catalog to '" + _params._db + "' was not successful. Caught: " + ex); }
		}
		
		// Print out some destination information
		try
		{
			DatabaseMetaData dbmd = conn.getMetaData();
			String msg;

			try { msg = "Connected to URL '"                       + dbmd.getURL()                    +"'."; if (_params._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
			try { msg = "Connected using driver name '"            + dbmd.getDriverName()             +"'."; if (_params._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
			try { msg = "Connected using driver version '"         + dbmd.getDriverVersion()          +"'."; if (_params._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
			try { msg = "Connected to destination DBMS Vendor '"   + dbmd.getDatabaseProductName()    +"'."; if (_params._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
			try { msg = "Connected to destination DBMS Version '"  + dbmd.getDatabaseProductVersion() +"'."; if (_params._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
			try { msg = "Current Catalog in the destination srv '" + conn.getCatalog()                +"'."; if (_params._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
		}
		catch (SQLException ignore) {}

		// Execute the SQL InitString
		if (StringUtil.hasValue(_params._initStr))
		{
			String msg = "executing initialization SQL Stement '"+_params._initStr+"'.";
			addDebugMessage(msg);

			Statement stmnt = conn.createStatement();
			stmnt.executeUpdate(_params._initStr);
			stmnt.close();
		}

		return conn;
	}

}
