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
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import com.dbxtune.gui.ConnectionProfile;
import com.dbxtune.gui.ConnectionProfileManager;
import com.dbxtune.sql.SqlProgressDialog;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.TdsConnection;
import com.dbxtune.sql.diff.DiffContext;
import com.dbxtune.sql.diff.DiffContext.DiffSide;
import com.dbxtune.sql.diff.DiffSink;
import com.dbxtune.sql.diff.DiffTable;
import com.dbxtune.sql.diff.actions.DiffTableModel;
import com.dbxtune.sql.diff.actions.GenerateSqlText;
import com.dbxtune.sql.pipe.PipeCommandDiff.ActionType;
import com.dbxtune.sql.pipe.PipeCommandException;
import com.dbxtune.tools.sqlw.msg.JPipeMessage;
import com.dbxtune.tools.sqlw.msg.JTableResultSet;
import com.dbxtune.tools.sqlw.msg.Message;
import com.dbxtune.utils.DbUtils;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;

public class SqlStatementCmdTabDiff 
extends SqlStatementAbstract
{
//	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	private String[] _args = null;
	private String _originCmd = null;

//	String    _leftQuery       = null;
//	String    _rightQuery      = null;

	private static class CmdParams
	{
		String     _profile   = null;
		String     _user      = null;
		String     _passwd    = null;
		String     _server    = null;
		String     _db        = null;
		String     _url       = null;
//		String     _driver    = null; // not yet used

		List<String>  _keyCols     = null;

		String    _initStr         = null;
//		String    _query           = null;
		String    _leftTable       = null;
		String    _rightTable      = null;
		String    _whereClause     = null;

		int       _leftFetchSize   = -1;
		int       _rightFetchSize  = -1;

//And for other DBMSs... supply a switch --no-xxx-in-where-clauses-bla-bla-bla
//		boolean   _dbmsDoNotUseLongDatatypesInGeneratedPkOrOrderBy = false;
//FIXME: implement the below
//		String    _rawTabDiffCmdSwitches = null;

		boolean    _skipLobCols    = false;
		List<String> _diffColumns  = null;
		
		ActionType _action         = null; 
		String     _actionOutFile  = null; 
		String     _goString       = "\\ngo"; 
		String     _execBeforeSync = null;
		String     _execAfterSync  = null;
		boolean    _execSyncInTran = true;

		boolean   _doPreRowCount   = true;  // addDebugMessage()

		boolean   _debug           = false;  // addDebugMessage()
		boolean   _trace           = false;  // addTraceMessage()
		boolean   _toStdout        = false;  // if debug/trace is enabled, also print the messages to STDOUT as soon as they happen (may be easier to debug in that way)
		
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
			sb.append(", ").append("doPreRowCount ".trim()).append("=").append(StringUtil.quotify(_doPreRowCount ));
			sb.append(", ").append("debug         ".trim()).append("=").append(StringUtil.quotify(_debug         ));
			sb.append(", ").append("trace         ".trim()).append("=").append(StringUtil.quotify(_trace         ));
			sb.append(", ").append("toStdout      ".trim()).append("=").append(StringUtil.quotify(_toStdout      ));
			sb.append(", ").append("keyCols       ".trim()).append("=").append(StringUtil.quotify(_keyCols       ));
//			sb.append(", ").append("query         ".trim()).append("=").append(StringUtil.quotify(_query         ));
			sb.append(", ").append("leftTable     ".trim()).append("=").append(StringUtil.quotify(_leftTable     ));
			sb.append(", ").append("rightTable    ".trim()).append("=").append(StringUtil.quotify(_rightTable    ));
			sb.append(", ").append("whereClause   ".trim()).append("=").append(StringUtil.quotify(_whereClause   ));
			sb.append(", ").append("diffColumns   ".trim()).append("=").append(StringUtil.quotify(_diffColumns   ));
			sb.append(", ").append("skipLobCols   ".trim()).append("=").append(StringUtil.quotify(_skipLobCols   ));
			sb.append(", ").append("action        ".trim()).append("=").append(StringUtil.quotify(_action        ));
			sb.append(", ").append("actionOutFile ".trim()).append("=").append(StringUtil.quotify(_actionOutFile ));
			sb.append(", ").append("goString      ".trim()).append("=").append(StringUtil.quotify(_goString      ));
			sb.append(", ").append("execBeforeSync".trim()).append("=").append(StringUtil.quotify(_execBeforeSync));
			sb.append(", ").append("execAfterSync ".trim()).append("=").append(StringUtil.quotify(_execAfterSync ));
			sb.append(", ").append("execSyncInTran".trim()).append("=").append(StringUtil.quotify(_execSyncInTran));

			return sb.toString();
		}
	}
	private CmdParams _params = new CmdParams();

	// if -p profileName is given... then store the profile's Connection Properties in this one... 
	private ConnectionProfile _rightConnectionProfile = null;


	public String        getCmdLineParams()   { return _params.toString();       }
	
//	public String        getQuery()           { return _params._query;           }
	public List<String>  getKeyCols()         { return _params._keyCols;         }
//	public boolean       isDebugEnabled()     { return _params._debug;           }




	public SqlStatementCmdTabDiff(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
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
//		String params = input.substring(input.indexOf(' ') + 1).trim();
		String params = input.replace("\\tabdiff", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
//			if (cmdLine.hasOption('q')) _params._query           = cmdLine.getOptionValue('q');
			if (cmdLine.hasOption('l')) _params._leftTable       = cmdLine.getOptionValue('l');
			if (cmdLine.hasOption('r')) _params._rightTable      = cmdLine.getOptionValue('r');
			if (cmdLine.hasOption('w')) _params._whereClause     = cmdLine.getOptionValue('w');
			if (cmdLine.hasOption('k')) _params._keyCols         = StringUtil.commaStrToList(cmdLine.getOptionValue('k'));
			if (cmdLine.hasOption('n')) _params._doPreRowCount   = false;
			if (cmdLine.hasOption('x')) _params._debug           = true;
			if (cmdLine.hasOption('X')) _params._trace           = true;
			if (cmdLine.hasOption('Y')) _params._toStdout        = true;
			if (cmdLine.hasOption('U')) _params._user            = cmdLine.getOptionValue('U');
			if (cmdLine.hasOption('P')) _params._passwd          = cmdLine.getOptionValue('P');
			if (cmdLine.hasOption('S')) _params._server          = cmdLine.getOptionValue('S');
			if (cmdLine.hasOption('D')) _params._db              = cmdLine.getOptionValue('D');
			if (cmdLine.hasOption('u')) _params._url             = cmdLine.getOptionValue('u');
			if (cmdLine.hasOption('p')) _params._profile         = cmdLine.getOptionValue('p');
			if (cmdLine.hasOption('f')) _params._leftFetchSize   = StringUtil.parseInt(cmdLine.getOptionValue('f'), -1);
			if (cmdLine.hasOption('F')) _params._rightFetchSize  = StringUtil.parseInt(cmdLine.getOptionValue('F'), -1);
			if (cmdLine.hasOption('A')) _params._action          = ActionType.fromString(cmdLine.getOptionValue('A'));
			if (cmdLine.hasOption('o')) _params._actionOutFile   = cmdLine.getOptionValue('o');
			if (cmdLine.hasOption('g')) _params._goString        = cmdLine.getOptionValue('g');
			if (cmdLine.hasOption('L')) _params._skipLobCols     = true;
			if (cmdLine.hasOption('c')) _params._diffColumns     = StringUtil.commaStrToList(cmdLine.getOptionValue('c'));;

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
				
//				params._user   = cp.getDbUserName();
//				params._passwd = cp.getDbPassword();
//				String serverOrUrlStr = cp.getDbServerOrUrl();
//				if (serverOrUrlStr != null)
//				{
//					if (serverOrUrlStr.startsWith("jdbc:"))
//						params._url = serverOrUrlStr;
//					else
//						params._server = serverOrUrlStr;
//				}
			}
		}
		
		if (StringUtil.isNullOrBlank(_params._server) && _rightConnectionProfile == null)
			printHelp(null, "Missing mandatory parameter '-p|--profile <profile>' or '-S|--server <srvName>'.");

		if (StringUtil.isNullOrBlank(_params._leftTable))
			printHelp(null, "Missing mandatory parameter '--left tableName'.");

		if (StringUtil.isNullOrBlank(_params._rightTable))
			_params._rightTable = _params._leftTable;

		// if --leftFetchSize is specified but not --rightFetchSize: then set rightFetchSize to same value as leftFetchSize
		if (_params._leftFetchSize >= 0 && _params._rightFetchSize == -1)
			_params._rightFetchSize = _params._leftFetchSize;
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
//		options.addOption( "q", "query",           true,    "SQL Query towards destination" );
		options.addOption( "l", "left",            true,    "Table name on the LEFT side" );
		options.addOption( "r", "right",           true,    "Table name on the RIGHT side" );
		options.addOption( "w", "where",           true,    "" );
		options.addOption( "c", "diffCols",        true,    "" );
		options.addOption( "k", "keyCols",         true,    "" );
		options.addOption( "n", "noRowCount",      false,   "" );
		options.addOption( "f", "leftFetchSize",   true,    "" );
		options.addOption( "F", "rightFetchSize",  true,    "" );
		options.addOption( "A", "action",          true,    "what action to do with difference" );
		options.addOption( "o", "actionOutFile",   true,    "Write actions to file" );
		options.addOption( "g", "go",              true,    "go string" );
		options.addOption( "L", "skipLobCols",     false,   "" );
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

		if (errorStr != null)
		{
			sb.append("\n");
			sb.append(errorStr);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("usage: \\tabdiff ... \n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -U,--user <user>          Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd \n");
		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port). \n");
		sb.append("  -D,--dbname <dbname>      Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>    Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -p,--profile <name>       Take user/passwd/url from a connection profile.\n");
		sb.append("\n");
//		sb.append("  -q,--query <sq-query>     SQL Statement to exexute towards destination. (default same as source) \n");
		sb.append("  -l,--left <tableName>     Table name on the LEFT side. \n");
		sb.append("  -r,--right <tableName>    Table name on the RIGHT side. (default same as '--left') \n");
		sb.append("  -w,--where <str>          Append a 'where clause' to just compare some rows. \n");
		sb.append("  -c,--diffCols <c1,c2...>  Comma separated list of columns to do diff on (default all columns) \n");
		sb.append("  -k,--keyCols <c1,c2...>   Comma separated list of KEY columns to use: ColNames or ColPos (pos starts at 0) \n");
		sb.append("  -n,--noRowCount           Disable 'get row count' SQL in PRE Execution. \n");
		sb.append("  -f,--leftFetchSize <num>  Statement.setFetchSize(###), if above 0, the select will also be done in tran (default=-1)\n");
		sb.append("  -F,--rightFetchSize <num> Statement.setFetchSize(###), if above 0, the select will also be done in tran (default=same as --leftFetchSize)\n");
		sb.append("  -A,--action <name>        Action when differance. "+StringUtil.toCommaStr(ActionType.values())+" (default: "+ActionType.TABLE+") \n");
		sb.append("  -o,--actionOutFile <name> Write the action out put to a file. \n");
		sb.append("  -g,--go <termStr>         Use this as a command execution string. (default=\\ngo)\n");
		sb.append("  -L,--skipLobCols          Skip LOB columns in the select list. (that is LONG* types adn *LOB) \n");
		sb.append("  -x,--debug                Debug, print some extra info \n");
		sb.append("  -X,--trace                Trace, print some extra info (more than debug)\n");
		sb.append("  -Y,--stdout               Print debug/trace messages to stdout as well\n");
		sb.append("  \n");
		sb.append("  \n");
		sb.append("Some extra info:\n");
		sb.append("  - The data for left/right hand side is read as a stream, therefore the data needs to be \n");
		sb.append("    sorted (at the DBMS side) on the --keyCols (normally not a problem if you have a Primary Key) \n");
		sb.append("  - If you do not specify --keyCols, I will try to get the PK or First Unique Index from the DBMS \n");
		sb.append("    in case that fails, or there are no PK or Unique Indexes, you have to specify --keyCols manually. \n");
		sb.append("    Note: --keyCols should be a list of columns *you* considder to be \"unique\" or want to compare on. \n");
		sb.append("    Note: Data MUST be sorted on the --keyCols columns. \n");
		sb.append("  - Diff Algorithm: \n");
		sb.append("    The diff algorithm is using 'kind-of: internal merge join' strategy on --keyCols to figgure out \n");
		sb.append("    in what order it should read the next row from (left or right ResultSet) \n");
		sb.append("     - When it can't find the current rows PK Columns on the other side: The full row will be reported.\n");
		sb.append("     - When we have a PK Value \"match\": all columns will be checked for value differences.\n");
		sb.append("  - For Postgres *FetchSize will automatically be changed to 1000, to disable this, use: -f 0 -F 0 \n");
		sb.append("    This because Postgres fetches ALL records into the client before reading first row. \n");
		sb.append("    With big tables, this means Out-Of-Memoy. If you want to do this for other DBMS with the same \n");
		sb.append("    behaviour, please use the --leftFetchSize #### and --rightFetchSize #### \n");
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- check difference on the whole table with another server (in the below case a Sybase ASE)\n");
		sb.append("    \\tabdiff -l tablename -Usa -Psecret -Shost:port -Ddbname \n");
		sb.append("\n");
		sb.append("  or: \n");
		sb.append("    -- Just a subset of the table: some rows... \n");
		sb.append("    \\tabdiff -l tablename -r schema2.tablename --where \"country = 'sweden'\" --profile 'profile name' \n");
		sb.append("\n");
		sb.append("If you want even more flexibility... take a look at the 'diff' where you can do even more, for example: \n");
		sb.append("    -- Do aggregate and compare that... Connect using a Connection Profile \n");
		sb.append("    select type, count(*) as cnt from sysobjects group by type order by type \n");
		sb.append("    go | diff -p 'profile name' -Ddbname --keyCols type\n");
		sb.append("\n");
		sb.append("\n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private void init()
	throws SQLException
	{
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
		// If this is a jConnect connection, RESET current message handler (which mean they will be thrown)
		DbxConnection leftConn = _conn;
		if (leftConn instanceof TdsConnection)
			((TdsConnection)leftConn).setSybMessageHandler(null);
		
		try
		{
			return localExecute();
		}
		catch(SQLException ex)
		{
//ex.printStackTrace();
			throw ex;
		}
		catch(Exception ex)
		{
//ex.printStackTrace();
			throw new SQLException(ex.getMessage(), ex);
		}
		finally
		{
			// If this is a jConnect connection, RESTORE any previously installed message handler
			if (leftConn instanceof TdsConnection)
				((TdsConnection)leftConn).restoreSybMessageHandler();
		}
	}

	private boolean localExecute() throws Exception
	{
		long execStartTime = System.currentTimeMillis();
		
		DbxConnection leftConn = _conn;
		
		DiffContext context = new DiffContext();

		context.setDiffColumns(_params._diffColumns);
		context.setGuiOwner(_guiOwner); // Handle to the SQL Window Component
		context.setProgressDialog(_progress);

		if (_params._debug)    { context.setMessageDebugLevel(1);  setMessageDebugLevel(1);  }
		if (_params._trace)    { context.setMessageDebugLevel(2);  setMessageDebugLevel(2);  }
		if (_params._toStdout) { context.setMessageToStdout(true); setMessageToStdout(true); }
//context.setMessageDebugLevel(1);  setMessageDebugLevel(1);
////context.setMessageDebugLevel(2);  setMessageDebugLevel(2);
//context.setMessageToStdout(true); setMessageToStdout(true);

		DiffTableModel diffTableModel = null;

		DbxConnection rightConn = null;
		
		// NOTE: Postgres (and possibly other DBMS's) tries to read ALL records in the ResultSet into memory when executing a query
		//       This is possibly a way to get around this...
		//       https://jdbc.postgresql.org/documentation/head/query.html#fetchsize-example

		boolean leftConnDoFetchInTransaction  = false;
		boolean rightConnDoFetchInTransaction = false;

		// Get current state of Auto Commit so we can restore it later
		boolean leftConnOriginAutoCommit  = true;
		boolean rightConnOriginAutoCommit = true;

		String leftExtraInfoMsg  = "";
		String rightExtraInfoMsg = "";


		try
		{
			// Connect to RIGHT hand side
			if (_progress != null) _progress.setState("Connecting to RIGHT hand side DBMS");
			if (_params._debug)     addDebugMessage(  "Connecting to RIGHT hand side DBMS");

			// Get a connection to the Right Hand Side DBMS
			rightConn = getRightConnection(_rightConnectionProfile, _params._user, _params._passwd, _params._server, _params._db, _params._url, _params._initStr, _params._debug);
			

			ConnectionProp leftConnProps  = leftConn  == null ? null : leftConn .getConnProp();
			ConnectionProp rightConnProps = rightConn == null ? null : rightConn.getConnProp();

			// Build a WHERE Clause, from the input parameter (if it was specified)
			String whereClause = "";
			if (StringUtil.hasValue(_params._whereClause))
				whereClause = " where " + _params._whereClause;


			// PRE Query on LEFT and RIGHT to see if table exists
			// and also to collect PK info etc
			String leftPreQuery  = "select * from " + _params._leftTable  + " where 1 > 100 -- always FALSE, just to get JDBC MetaData"; // NOTE: where 1 = 2 sometimes returned faulty MetaData (wring database name for Sybase ASE)
			String rightPreQuery = "select * from " + _params._rightTable + " where 1 > 100 -- always FALSE, just to get JDBC MetaData"; // NOTE: where 1 = 2 sometimes returned faulty MetaData (wring database name for Sybase ASE)


			//----------------------------------------------------
			// PRE LEFT - check if table exists
			if (_progress != null) _progress.setState("Executing PRE SQL at LEFT hand side DBMS to get MetaData, SQL: "+leftPreQuery);
			if (_params._debug)     addDebugMessage(  "Executing PRE SQL at LEFT hand side DBMS to get MetaData, SQL: "+leftPreQuery);

			ResultSet leftPreRs = null;
			try 
			{
				Statement leftPreStmt = leftConn.createStatement();
				leftPreRs   = leftPreStmt.executeQuery(leftPreQuery);
			}
			catch (SQLException ex)
			{
				addErrorMessage("Problems executing PRE-SQL on LEFT hand side. SQL='"+leftPreQuery+"'. Caught: "+ex);
				throw ex;
			}
			

			//----------------------------------------------------
			// PRE RIGHT - check if table exists
			if (_progress != null) _progress.setState("Executing PRE SQL at RIGHT hand side DBMS to get MetaData, SQL: "+rightPreQuery);
			if (_params._debug)     addDebugMessage(  "Executing PRE SQL at RIGHT hand side DBMS to get MetaData, SQL: "+rightPreQuery);

			ResultSet rightPreRs = null;
			try 
			{
				Statement rightPreStmt = rightConn.createStatement();
				rightPreRs = rightPreStmt.executeQuery(rightPreQuery);
			}
			catch (SQLException ex)
			{
				addErrorMessage("Problems executing PRE-SQL on RIGHT hand side. SQL='"+rightPreQuery+"'. Caught: "+ex);
				throw ex;
			}


			context.setPkColumns(_params._keyCols);
			DiffTable leftPreDt  = new DiffTable(DiffSide.LEFT,  context, leftPreRs,  leftPreQuery,  leftConn);//leftConnProps);
			DiffTable rightPreDt = new DiffTable(DiffSide.RIGHT, context, rightPreRs, rightPreQuery, rightConn);//rightConnProps);

			// Transfer messages that was generated during PRE Stage
			if (context.hasMessages())
			{
				addMessages(context.getMessages());
				context.clearMessages();
			}



			//----------------------------------------------------------------------
			// NOW CONSTRUCT SQL TEXT for the REAL SQL Query on the LEFT and RIGHT
			boolean skipLobColumns = _params._skipLobCols;
			String leftQuery  = "select " + leftPreDt .getColumnNamesCsv(leftConn .getLeftQuote(), leftConn .getRightQuote(), skipLobColumns) + " from " + _params._leftTable  + whereClause + " order by " + leftPreDt .getPkColumnNamesCsv(leftConn .getLeftQuote(), leftConn .getRightQuote());
			String rightQuery = "select " + rightPreDt.getColumnNamesCsv(rightConn.getLeftQuote(), rightConn.getRightQuote(), skipLobColumns) + " from " + _params._rightTable + whereClause + " order by " + rightPreDt.getPkColumnNamesCsv(rightConn.getLeftQuote(), rightConn.getRightQuote());
			
			if (skipLobColumns)
			{
				int cnt = leftPreDt .getLobColumnNames().size() + rightPreDt .getLobColumnNames().size();
				if (cnt > 0)
				{
					addInfoMessage("Skipping LOB columns on LEFT  SIDE: " + leftPreDt .getLobColumnNames());
					addInfoMessage("Skipping LOB columns on RIGHT SIDE: " + rightPreDt.getLobColumnNames());
				}
			}

			// SET the PK found in the PreQuery (or from the _params._keyCols, which was specified earlier)
			addDebugMessage("Setting/Using PK Columns in the context to use: "+leftPreDt.getPkColumnNames());
			context.setPkColumns(leftPreDt.getPkColumnNames());

			leftPreDt .close();
			rightPreDt.close();


			
			
			String leftPreCountQuery  = "select count(*) from " + _params._leftTable  + whereClause;
			String rightPreCountQuery = "select count(*) from " + _params._rightTable + whereClause;
			long   leftPreCount       = -1;
			long   rightPreCount      = -1;

			//----------------------------------------------------
			// PRE LEFT - Row Count
			if (_params._doPreRowCount)
			{
				if (_progress != null) _progress.setState("Executing PRE SQL at LEFT hand side DBMS to get Row Count, SQL: "+leftPreCountQuery);
				if (_params._debug)     addDebugMessage(  "Executing PRE SQL at LEFT hand side DBMS to get Row Count, SQL: "+leftPreCountQuery);

				try 
				{
					Statement tmpStmnt = leftConn.createStatement();
					ResultSet tmpRs    = tmpStmnt.executeQuery(leftPreCountQuery);
					while (tmpRs.next())
						leftPreCount = tmpRs.getLong(1);
					tmpRs.close();
				}
				catch (SQLException ex)
				{
					addErrorMessage("Problems executing PRE-SQL Row Count on LEFT hand side. SQL='"+leftPreCountQuery+"'. Caught: "+ex);
					throw ex;
				}
			}
			
			//----------------------------------------------------
			// PRE RIGHT - Row Count
			if (_params._doPreRowCount)
			{
				if (_progress != null) _progress.setState("Executing PRE SQL at RIGHT hand side DBMS to get Row Count, SQL: "+rightPreCountQuery);
				if (_params._debug)     addDebugMessage(  "Executing PRE SQL at RIGHT hand side DBMS to get Row Count, SQL: "+rightPreCountQuery);

				try 
				{
					Statement tmpStmnt = leftConn.createStatement();
					ResultSet tmpRs    = tmpStmnt.executeQuery(rightPreCountQuery);
					while (tmpRs.next())
						rightPreCount = tmpRs.getLong(1);
					tmpRs.close();
				}
				catch (SQLException ex)
				{
					addErrorMessage("Problems executing PRE-SQL Row Count on RIGHT hand side. SQL='"+rightPreCountQuery+"'. Caught: "+ex);
					throw ex;
				}
			}
			

			// NOTE: Postgres (and possibly other DBMS's) tries to read ALL records in the ResultSet into memory when executing a query
			//       This is possibly a way to get around this...
			//       https://jdbc.postgresql.org/documentation/head/query.html#fetchsize-example
			leftConnOriginAutoCommit  = leftConn .getAutoCommit();
			rightConnOriginAutoCommit = rightConn.getAutoCommit();

			if ((_params._leftFetchSize < 0 && leftConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_POSTGRES)) || _params._leftFetchSize > 0)
			{
				leftConnDoFetchInTransaction = true;
				_params._leftFetchSize       = 1_000;
				leftConn.setAutoCommit(false); // Start a transaction

				addDebugMessage("LEFT Connection is '"+leftConn.getDatabaseProductName()+"', FetchSize will be set to "+_params._leftFetchSize+" and we will start a Transaction where the diff is made (row fetch).");
				leftExtraInfoMsg += " DbmsVendor='"+leftConn.getDatabaseProductName()+"',FetchSize="+_params._leftFetchSize+",FetchInTran=true";
			}

			if ((_params._rightFetchSize < 0 && rightConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_POSTGRES)) || _params._rightFetchSize > 0)
			{
				rightConnDoFetchInTransaction = true;
				_params._rightFetchSize       = 1_000;
				rightConn.setAutoCommit(false); // Start a transaction

				addDebugMessage("RIGHT Connection is '"+rightConn.getDatabaseProductName()+"', FetchSize will be set to "+_params._rightFetchSize+" and we will start a Transaction where the diff is made (row fetch).");
				rightExtraInfoMsg += " DbmsVendor='"+rightConn.getDatabaseProductName()+"',FetchSize="+_params._leftFetchSize+",FetchInTran=true";
			}


			// Execute query LEFT SIDE
			if (_progress != null) _progress.setState("Executing SQL at LEFT hand side DBMS, SQL: "+leftQuery);
			if (_params._debug)     addDebugMessage(  "Executing SQL at LEFT hand side DBMS, SQL: "+leftQuery);

			// Execute SQL at LEFT Hand side
			ResultSet leftRs = null; // FIXME: close this and the Statement "somewhere"
			try
			{
				Statement leftStmt = leftConn.createStatement();
				if (_params._leftFetchSize > 0)
					leftStmt.setFetchSize(_params._leftFetchSize);
				leftRs = leftStmt.executeQuery(leftQuery);
			}
			catch (SQLException ex)
			{
				addErrorMessage("Problems executing on LEFT hand side. SQL='"+leftQuery+"'. Caught: "+ex);
				throw ex;
			}


			// Execute query RIGHT SIDE
			if (_progress != null) _progress.setState("Executing SQL at RIGHT hand side DBMS, SQL: "+rightQuery);
			if (_params._debug)     addDebugMessage(  "Executing SQL at RIGHT hand side DBMS, SQL: "+rightQuery);

			// Execute SQL at RIGHT Hand side
			ResultSet rightRs = null; // FIXME: close this and the Statement "somewhere"
			try
			{
				Statement rightStmt = rightConn.createStatement();
				if (_params._rightFetchSize > 0)
					rightStmt.setFetchSize(_params._rightFetchSize);
				rightRs = rightStmt.executeQuery(rightQuery);
			}
			catch (SQLException ex)
			{
				addErrorMessage("Problems executing on RIGHT hand side. SQL='"+rightQuery+"'. Caught: "+ex);
				throw ex;
			}


			if (_progress != null) _progress.setState("Initializing and validating DIFF Engine...");
			if (_params._debug)     addDebugMessage  ("Initializing and validating DIFF Engine...");


			// Set the REAL RESULT SETS
			context.setDiffTable(DiffSide.LEFT,  leftRs,  leftQuery,  leftConnProps);
			context.setDiffTable(DiffSide.RIGHT, rightRs, rightQuery, rightConnProps);

			// Set Expected Row Count
			context.getDiffTable(DiffSide.LEFT) .setExpectedRowCount(leftPreCount);
			context.getDiffTable(DiffSide.RIGHT).setExpectedRowCount(rightPreCount);
			
			// VALIDATE the DiffTables
			context.validate();

			if (_progress != null) _progress.setState("Do DIFF Logic...");
			if (_params._debug)     addDebugMessage  ("Do DIFF Logic...");

			//----------------------
			// DIFF LOGIC
			int diffCount = context.doDiff();

			//----------------------
			// RESTORE AutoCommit...
			if (leftConnDoFetchInTransaction)  leftConn .setAutoCommit(leftConnOriginAutoCommit);
			if (rightConnDoFetchInTransaction) rightConn.setAutoCommit(rightConnOriginAutoCommit);


			if (_progress != null) _progress.setState("Done: Total Diff Count = " + diffCount);
			if (_params._debug)     addDebugMessage  ("Done: Total Diff Count = " + diffCount);

			// Transfer messages that was generated during "diff" to "this" message system
			if (context.hasMessages())
				addMessages(context.getMessages());

			// Check results
//			String tabInfo = "[" + context.getLeftDt().getFullTableName() + ", " + context.getRightDt().getFullTableName() + "]";
			String tabInfo = "[" + context.getLeftDt().getShortTableName() + "]";

			String leftDbmsInfo  = "";
			String rightDbmsInfo = "";
			try { leftDbmsInfo  = leftConn .getDbmsServerName(); } catch(SQLException ex) {}
			try { rightDbmsInfo = rightConn.getDbmsServerName(); } catch(SQLException ex) {}

			// How long did this take.
			String execTimeStr = TimeUtils.msDiffNowToTimeStr(execStartTime);

			if (diffCount == 0)
			{
				addInfoMessage("OK - " + tabInfo + " Left and Right ResultSet has NO difference \n"
						+ "           Left:  RowCount=" + context.getLeftDt() .getRowCount() + ", ColCount=" + context.getLeftDt() .getColumnCount() + ", PkCols=" + context.getLeftDt() .getPkColumnNames() + ", TabName='" + context.getLeftDt() .getFullTableName() + "', DbmsInfo='" + leftDbmsInfo  + "'." + leftExtraInfoMsg  + "\n"
						+ "           Right: RowCount=" + context.getRightDt().getRowCount() + ", ColCount=" + context.getRightDt().getColumnCount() + ", pkCols=" + context.getRightDt().getPkColumnNames() + ", TabName='" + context.getRightDt().getFullTableName() + "', DbmsInfo='" + rightDbmsInfo + "'." + rightExtraInfoMsg + "\n"
						+ "           ExecTime: " + execTimeStr
						);
			}
			else
			{
				DiffSink diffSink = context.getSink();
				String msg 
						= "        Total Diff Count = "     + diffSink.getCurrentDiffCount() 
						                  + ", Left Missing Rows = "  + diffSink.getLeftMissingRows() .size()
						                  + ", Right Missing Rows = " + diffSink.getRightMissingRows().size()
						                  + ", Column Diff Rows = "   + diffSink.getDiffColumnValues().size() + ".\n"
						+ "           Left:  RowCount=" + context.getLeftDt() .getRowCount() + ", ColCount=" + context.getLeftDt() .getColumnCount() + ", PkCols=" + context.getLeftDt() .getPkColumnNames() + ", TabName='" + context.getLeftDt() .getFullTableName() + "', DbmsInfo='" + leftDbmsInfo  + "'." + leftExtraInfoMsg  + "\n"
						+ "           Right: RowCount=" + context.getRightDt().getRowCount() + ", ColCount=" + context.getRightDt().getColumnCount() + ", pkCols=" + context.getRightDt().getPkColumnNames() + ", TabName='" + context.getRightDt().getFullTableName() + "', DbmsInfo='" + rightDbmsInfo + "'." + rightExtraInfoMsg + "\n"
						+ "           ExecTime: " + execTimeStr + "\n"
						+ "        NOTE: You can turn on debugging '-x' to see what SQL that is issued on left/right hand side."
						;

				addErrorMessage(tabInfo + " Left and Right ResultSet is DIFFERENT. \n" + msg);
			}
			

			// has DIFFERENCE, do some extra stuff
			if (diffCount != 0)
			{
				// Actions to be taken on the DIFF Results
				if (ActionType.TABLE.equals(_params._action) || _params._action == null)
				{
					diffTableModel = new DiffTableModel(context);

					// Write to OUTPUT File
					if (StringUtil.hasValue(_params._actionOutFile))
					{
						File f = new File(_params._actionOutFile);
						addInfoMessage("Saving "+_params._action+" output to file: "+f);

						//String tableStr = ""; SwingUtils.tableToString(_diffTableModel); // FIXME: strip out ALL HTML tags
						String tableStr = SwingUtils.tableToHtmlString(diffTableModel);
						FileUtils.write(f, tableStr, StandardCharsets.UTF_8);
					}
				}
				else if (ActionType.SQL_LEFT.equals(_params._action))
				{
					GenerateSqlText sqlGen = new GenerateSqlText(context, leftConn);
					List<String> dmlList = sqlGen.getSql(DiffSide.LEFT, _params._goString);

					for (String dml : dmlList)
						addPlainMessage(dml);

					// Write to OUTPUT File
					if (StringUtil.hasValue(_params._actionOutFile))
					{
						File f = new File(_params._actionOutFile);
						addInfoMessage("Saving "+_params._action+" output to file: "+f);

						FileUtils.writeLines(f, dmlList);
					}
				}
				else if (ActionType.SQL_RIGHT.equals(_params._action))
				{
					GenerateSqlText sqlGen = new GenerateSqlText(context, leftConn);
					List<String> dmlList = sqlGen.getSql(DiffSide.RIGHT, _params._goString);
					
					for (String dml : dmlList)
						addPlainMessage(dml);

					// Write to OUTPUT File
					if (StringUtil.hasValue(_params._actionOutFile))
					{
						File f = new File(_params._actionOutFile);
						addInfoMessage("Saving "+_params._action+" output to file: "+f);

						FileUtils.writeLines(f, dmlList);
					}
				}
				else if (ActionType.SYNC_LEFT.equals(_params._action))
				{
					addErrorMessage("SYNC_LEFT is not yet implemented, instead a difference table will be generated.");
					diffTableModel = new DiffTableModel(context);
				}
				else if (ActionType.SYNC_RIGHT.equals(_params._action))
				{
					addErrorMessage("SYNC_RIGHT is not yet implemented, instead a difference table will be generated.");
					diffTableModel = new DiffTableModel(context);
				}
			} // end: has DIFFERENCE
		}
		finally
		{
			for (Message pmsg : getMessages())
				_resultCompList.add( new JPipeMessage(pmsg, _originCmd) );
			clearMessages();
			
			if (diffTableModel != null)
			{
				_resultCompList.add(new JTableResultSet(diffTableModel));
			}

			//----------------------
			// RESTORE AutoCommit...
			if (leftConn  != null && leftConnDoFetchInTransaction)  leftConn .setAutoCommit(leftConnOriginAutoCommit);
			if (rightConn != null && rightConnDoFetchInTransaction) rightConn.setAutoCommit(rightConnOriginAutoCommit);

			// close RIGHT Connection
			if (rightConn != null)
				rightConn.close();
		}

		return false; // true=We Have A ResultSet, false=No ResultSet
	}
}
