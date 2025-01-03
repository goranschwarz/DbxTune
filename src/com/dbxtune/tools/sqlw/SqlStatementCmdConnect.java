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
package com.dbxtune.tools.sqlw;

import java.awt.Component;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.gui.ConnectionProfile;
import com.dbxtune.gui.ConnectionProfileManager;
import com.dbxtune.sql.SqlProgressDialog;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.pipe.PipeCommandException;
import com.dbxtune.utils.StringUtil;

public class SqlStatementCmdConnect 
extends SqlStatementAbstract
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String[] _args = null;
	private String _originCmd = null;
	
//	private String _profileName;

	private static class CmdParams
	{
		String        _profile   = null;
		String        _user      = null;
		String        _passwd    = null;
//		String        _server    = null;
//		String        _db        = null;
		String        _url       = null;
//		String        _driver    = null; // not yet used

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
//			sb.append(", ").append("server        ".trim()).append("=").append(StringUtil.quotify(_server        ));
//			sb.append(", ").append("db            ".trim()).append("=").append(StringUtil.quotify(_db            ));
			sb.append(", ").append("url           ".trim()).append("=").append(StringUtil.quotify(_url           ));
			
			sb.append(", ").append("debug         ".trim()).append("=").append(StringUtil.quotify(_debug         ));
			sb.append(", ").append("trace         ".trim()).append("=").append(StringUtil.quotify(_trace         ));
			sb.append(", ").append("toStdout      ".trim()).append("=").append(StringUtil.quotify(_toStdout      ));

			return sb.toString();
		}
	}
	private CmdParams _params = new CmdParams();

	
	public SqlStatementCmdConnect(QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		super(null, null, null, null, null, null, queryWindow);
	}

	public SqlStatementCmdConnect(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		super(conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		parse(sqlOrigin);
		init();
	}

	
	/**
	 * @return true if we have specified a Connection Profile, using: '-p' or '--profile'
	 */
	public boolean hasConnectionProfile()
	{
		return StringUtil.hasValue(getProfileName());
	}

	/**
	 * @return true if we have specified a URL, using: '-u' or '--url'
	 */
	public boolean hasConnectionUrl()
	{
		return StringUtil.hasValue(getUrl());
	}

	public String getProfileName() { return _params._profile; }
	public String getUrl()         { return _params._url;     }
	public String getUsername()    { return _params._user;    }
	public String getPassword()    { return _params._passwd;  }
	
	private String _overflowText = "";
	public boolean hasOverflowText()
	{
		return StringUtil.hasValue(_overflowText);
	}
	/**
	 * Get "overflow text" from the command<br>
	 * Example:
	 * <pre>
	 * \connect -p 'profile name'
	 * go
	 * select 1  <<<---This is the overflow text---
	 * go        <<<---This is the overflow text---
	 * select 2  <<<---This is the overflow text---
	 * </pre>
	 * @return
	 */
	public String getOverflowText()
	{
		return _overflowText;
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
		input = input.trim();
		_originCmd = input;

		// If this is a "multi-line" input
		// Divide the: \connect -.... "line" as the input command
		// and everything after the \connect row will be placed in "_overflowText"
		// This so we can pick upp potential "trailing" SQL Commands and execute them *after* the connect has happened
		int firstNewLinePos = input.indexOf("\n");
		if (firstNewLinePos != -1)
		{
			input = input.substring(0, firstNewLinePos).trim();
			input = StringUtil.removeSemicolonAtEnd(input).trim();

			_overflowText = _originCmd.substring(firstNewLinePos).trim();

			// Remove first line **after** "\connect" if it's a "go"
			int tmp = _overflowText.indexOf("\n");
			if (tmp != -1)
			{
				String overflowFirstLine = _overflowText.substring(0, tmp).trim();
				if (StringUtil.hasValue(overflowFirstLine) && (overflowFirstLine.equalsIgnoreCase("go") || StringUtils.startsWithIgnoreCase(overflowFirstLine, "go ")))
					_overflowText = _overflowText.substring(tmp).trim();
			}
		}
		String params = input.replace("\\connect", "").trim();

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
//			if (cmdLine.hasOption('S')) _params._server        = cmdLine.getOptionValue('S');
//			if (cmdLine.hasOption('D')) _params._db            = cmdLine.getOptionValue('D');
			if (cmdLine.hasOption('u')) _params._url           = cmdLine.getOptionValue('u');
			if (cmdLine.hasOption('p')) _params._profile       = cmdLine.getOptionValue('p');

			if (cmdLine.hasOption('?'))
				printHelp(null, "You wanted help...");
			
			if ("null".equalsIgnoreCase(_params._passwd))
				_params._passwd = "";
		}
		else
		{
			printHelp(null, "Please specify some parameters.");
		}

		// true  = Get an Exception if not OK
		// false = Implement a "pop-up message" somewhere else... maybe seen better by the user...
		boolean checkParams = false;
		if (checkParams)
		{
			checkParsedParameters(_params);
		}

		if (_params._debug)
			addDebugMessage("CmdLineSwitches: " + _params);
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
			{
				throw new PipeCommandException("Profile not found in the ProfileManager. profile name '" + params._profile + "'.");
			}
			else
			{
			}
		}
		
		if (StringUtil.isNullOrBlank(_params._profile) && StringUtil.isNullOrBlank(_params._url))
			printHelp(null, "Missing mandatory parameter '-p|--profile <profile>' or '-u|--url jdbc:...'");
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
//		options.addOption( "S", "server",          true,    "Server to connect to (SERVERNAME|host:port)." );
//		options.addOption( "D", "dbname",          true,    "Database name in server." );
		options.addOption( "u", "url",             true,    "Destination DB URL (if not ASE and -S)" );

		options.addOption( "x", "debug",           false,   "debug" );
		options.addOption( "X", "trace",           false,   "trace" );
		options.addOption( "Y", "stdout",          false,   "print debug messages to Stdout" );

		try
		{
			_params = new CmdParams();
			
			// create the command line com.dbxtune.parser
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
		sb.append("usage: \\connect ... \n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -U,--user <user>          Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd \n");
//		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port). \n");
//		sb.append("  -D,--dbname <dbname>      Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>    Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -p,--profile <name>       Take user/passwd/url from a connection profile.\n");
		sb.append("\n");
		sb.append("  -x,--debug                Debug, print some extra info \n");
		sb.append("  -X,--trace                Trace, print some extra info (more than debug)\n");
		sb.append("  -Y,--stdout               Print debug/trace messages to stdout as well\n");
		sb.append("  \n");
		sb.append("  \n");
		sb.append("Some extra info:\n");
		sb.append("  - Closes current connection (if we are connected)\n");
		sb.append("  - Opens a new DBMS Connection...\n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- Connect to another DBMS (in the below case a Sybase ASE)\n");
		sb.append("    \\connect -Usa -Psecret -Shost:port -Ddbname -s 'select @@servername, @@version, db_name()'\n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- Connect to JDBC URL (in the below case a H2 DBMS)\n");
		sb.append("    \\connect -Usa -Psecret -u 'jdbc:h2:tcp://dbxtune.acme.com/name_of_the_db;IFEXISTS=TRUE'\n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- Connect to JDBC URL (in the below case a Postgres DBMS)\n");
		sb.append("    \\connect -Upostgres -Psecret -u 'jdbc:postgresql://dev-pg.acme.com:5432/postgres'\n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- Connect to DBMS that you already have in a profile\n");
		sb.append("    \\connect -p 'Profile name' \n");
		sb.append("\n");
		sb.append("Below you find some typical URL for different Vendors: \n");
		sb.append("    Sybase:     jdbc:sybase:Tds:<host>:<port>[/<dbname>]\n");
		sb.append("    SQL Server: jdbc:sqlserver://<host>:1433;databaseName=db;integratedSecurity=true\n");
		sb.append("    Postgres:   jdbc:postgresql://<host>:5432/database\n");
		sb.append("    MySQL:      jdbc:mysql://<host>:3306/<dbname>\n");
		sb.append("    Oracle:     jdbc:oracle:thin:@//<host>:1521/SERVICE\n");
		sb.append("    DB2:        jdbc:db2://<host>:<port>/<dbname>\n");
		sb.append("    H2 - file:  jdbc:h2:file:[<path>]<dbname>;IFEXISTS=TRUE\n");
		sb.append("    H2 - tcp:   jdbc:h2:tcp://<host>[:<port>]/<dbname>;IFEXISTS=TRUE\n");
		sb.append("    HANA:       jdbc:sap://<host>:<port>\n");
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
	{
		return new StatementDummy();
	}

	@Override
	public boolean execute() throws SQLException
	{
		if (_queryWindow == null)
			throw new SQLException("NO _queryWindow instance.");

		//----------------------
		// Disconnect (check if we are connected, and print some messages that we are disconnecting)
		//----------------------
		DbxConnection conn = _queryWindow.getConnection();
		if (conn != null)
		{
			String vendor  = conn.getDatabaseProductName();
			String srvName = conn.getDbmsServerName();
			String url     = conn.getMetaData().getURL();
			
			addInfoMessage("Disconnecting from DBMS Vendor '" + vendor + "', Server Name '" + srvName + "', using URL '" + url + "'.");

			_queryWindow.doDisconnect();
		}

		//----------------------
		// Connect
		//----------------------
		if (hasConnectionProfile())
		{
			_queryWindow.doConnect(getProfileName());
		}
		else if (hasConnectionUrl())
		{
			String jdbcUrl      = getUrl();
			String jdbcUsername = getUsername();
			String jdbcPassword = getPassword();

			_queryWindow.doConnect(jdbcUrl, jdbcUsername, jdbcPassword);
		}
		else
		{
			throw new SQLException("Please specify '--profile' or '--url' when using \\connect ");
		}
		
		
		conn = _queryWindow.getConnection();
		if (conn != null)
		{
			String vendor  = conn.getDatabaseProductName();
			String srvName = conn.getDbmsServerName();
			String url     = conn.getMetaData().getURL();
			
			addInfoMessage("Connected to DBMS Vendor '" + vendor + "', Server Name '" + srvName + "', using URL '" + url + "'.");
		}
		else
		{
			_logger.error("SqlStatementCmdConnect: connection is still NULL after: _queryWindow.doConnect(_profileName);");
		}
		
		return false; // true=We Have A ResultSet, false=No ResultSet
	}
}
