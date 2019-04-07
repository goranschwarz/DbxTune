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

import com.asetune.gui.ConnectionProfile;
import com.asetune.gui.ConnectionProfile.ConnProfileEntry;
import com.asetune.gui.ConnectionProfile.JdbcEntry;
import com.asetune.gui.ConnectionProfile.TdsEntry;
import com.asetune.gui.ConnectionProfileManager;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.diff.DiffContext;
import com.asetune.sql.diff.DiffException;
import com.asetune.sql.pipe.PipeCommandDiff.ActionType;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.StringUtil;

public class SqlStatementCmdDbDiff 
extends SqlStatementAbstract
{
//	private static Logger _logger = Logger.getLogger(SqlStatementCmdDbDiff.class);

	private String[] _args = null;
	private String _originCmd = null;
	
	private String _generatedCommands = null;

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

		String        _leftCatFilter  = null;
		String        _leftSchFilter  = null;
		String        _leftTabFilter  = "%";
		List<String>  _leftSkipList   = new ArrayList<>();

		String        _rightCatFilter = null;
		String        _rightSchFilter = null;
		String        _rightTabFilter = "%";
		List<String>  _rightSkipList  = new ArrayList<>();

		boolean       _autoSkip       = false;

		ActionType    _action         = null; 
		String        _actionOutFile  = null; 
		String        _execBeforeSync = null;
		String        _execAfterSync  = null;
		boolean       _execSyncInTran = true;
		boolean       _dryRun         = false;

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
			sb.append(", ").append("debug         ".trim()).append("=").append(StringUtil.quotify(_debug         ));
			sb.append(", ").append("trace         ".trim()).append("=").append(StringUtil.quotify(_trace         ));
			sb.append(", ").append("toStdout      ".trim()).append("=").append(StringUtil.quotify(_toStdout      ));

			sb.append(", ").append("leftCatFilter ".trim()).append("=").append(StringUtil.quotify(_leftCatFilter ));
			sb.append(", ").append("leftSchFilter ".trim()).append("=").append(StringUtil.quotify(_leftSchFilter ));
			sb.append(", ").append("leftTabFilter ".trim()).append("=").append(StringUtil.quotify(_leftTabFilter ));
			sb.append(", ").append("leftSkipList  ".trim()).append("=").append(StringUtil.quotify(_leftSkipList  ));
			sb.append(", ").append("rightCatFilter".trim()).append("=").append(StringUtil.quotify(_rightCatFilter));
			sb.append(", ").append("rightSchFilter".trim()).append("=").append(StringUtil.quotify(_rightSchFilter));
			sb.append(", ").append("rightTabFilter".trim()).append("=").append(StringUtil.quotify(_rightTabFilter));
			sb.append(", ").append("rightSkipList ".trim()).append("=").append(StringUtil.quotify(_rightSkipList ));

			sb.append(", ").append("autoSkip      ".trim()).append("=").append(StringUtil.quotify(_autoSkip      ));
			
			sb.append(", ").append("action        ".trim()).append("=").append(StringUtil.quotify(_action        ));
			sb.append(", ").append("actionOutFile ".trim()).append("=").append(StringUtil.quotify(_actionOutFile ));
			sb.append(", ").append("execBeforeSync".trim()).append("=").append(StringUtil.quotify(_execBeforeSync));
			sb.append(", ").append("execAfterSync ".trim()).append("=").append(StringUtil.quotify(_execAfterSync ));
			sb.append(", ").append("execSyncInTran".trim()).append("=").append(StringUtil.quotify(_execSyncInTran));
			sb.append(", ").append("_dryRun       ".trim()).append("=").append(StringUtil.quotify(_dryRun        ));
			
			return sb.toString();
		}

		public String createTabDiffSwitches(String left, String right)
		throws DiffException
		{
			StringBuilder sb = new StringBuilder();

			if (_profile != null)
			{
				if (_profile        != null)   sb.append("--profile '").append(_profile).append("' ");
			}
			else
			{
				if (_user           != null)   sb.append("-U '")       .append(_user   ).append("' ");
				if (_passwd         != null)   sb.append("-P '")       .append(_passwd ).append("' ");
				if (_server         != null)   sb.append("-S '")       .append(_server ).append("' ");
				if (_url            != null)   sb.append("--url '")    .append(_url    ).append("' ");
			}
			
//			if (_initStr        != null)   sb.append("--??? '")    .append(_initStr).append("' ");
//			if (_execBeforeSync != null)   sb.append("-? '")       .append(_execBeforeSync).append("' ");
//			if (_execAfterSync  != null)   sb.append("-? '")       .append(_execAfterSync).append("' ");
//			if (_execSyncInTran != null)   sb.append("-? '")       .append(_execSyncInTran).append("' ");
//			if (_actionOutFile  != null)   sb.append("-? '")       .append(_actionOutFile).append("' ");

			if (_db             != null)   sb.append("-D '")       .append(_db     ).append("' ");
			if (_action         != null)   sb.append("-A '")       .append(_action ).append("' ");
			if (_actionOutFile  != null)   
			{
				boolean hasLeft  = _actionOutFile.indexOf("${leftTabName}")  != -1; 
				boolean hasRight = _actionOutFile.indexOf("${rightTabName}") != -1; 

				if (hasLeft || hasRight)
				{
					String outFile = _actionOutFile; 
					outFile = outFile.replace("${leftTabName}",  left);
					outFile = outFile.replace("${rightTabName}", right);

					sb.append("--actionOutFile '").append(outFile).append("' ");
				}
				else
				{
					throw new DiffException("ERROR: You have specified -a or --actionOutFile '"+_actionOutFile+"' but you have NOT specified either '${leftTabName}' or '${rightTabName}' in the String. This needs to be done.");
				}
			}
			if (_debug                 )   sb.append("-x ");
			if (_trace                 )   sb.append("-X ");
			if (_toStdout              )   sb.append("-Z ");

			return sb.toString();
		}
	}
	private CmdParams _params = new CmdParams();

	// if -p profileName is given... then store the profile's Connection Properties in this one... 
	private ConnectionProfile _rightConnectionProfile = null;


	public String        getCmdLineParams()   { return _params.toString();       }
	
//	public String        getQuery()           { return _params._query;           }
//	public List<String>  getKeyCols()         { return _params._keyCols;         }
//	public boolean       isDebugEnabled()     { return _params._debug;           }




	public SqlStatementCmdDbDiff(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner)
	throws SQLException, PipeCommandException
	{
		super(conn, sqlOrigin, dbProductName, resultCompList, progress, owner);
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
		String params = input.replace("\\dbdiff", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('c')) _params._leftCatFilter   = cmdLine.getOptionValue('c');
			if (cmdLine.hasOption('o')) _params._leftSchFilter   = cmdLine.getOptionValue('o');
			if (cmdLine.hasOption('t')) _params._leftTabFilter   = cmdLine.getOptionValue('t');
			if (cmdLine.hasOption('f')) _params._leftSkipList    = StringUtil.parseCommaStrToList(cmdLine.getOptionValue('f'));
			
			if (cmdLine.hasOption('C')) _params._rightCatFilter  = cmdLine.getOptionValue('C');
			if (cmdLine.hasOption('O')) _params._rightSchFilter  = cmdLine.getOptionValue('O');
			if (cmdLine.hasOption('T')) _params._rightTabFilter  = cmdLine.getOptionValue('T');
			if (cmdLine.hasOption('F')) _params._rightSkipList   = StringUtil.parseCommaStrToList(cmdLine.getOptionValue('F'));

			if (cmdLine.hasOption('s')) _params._autoSkip        = true;
			if (cmdLine.hasOption('d')) _params._dryRun          = true;

			if (cmdLine.hasOption('x')) _params._debug           = true;
			if (cmdLine.hasOption('X')) _params._trace           = true;
			if (cmdLine.hasOption('Y')) _params._toStdout        = true;
			
			if (cmdLine.hasOption('U')) _params._user          = cmdLine.getOptionValue('U');
			if (cmdLine.hasOption('P')) _params._passwd        = cmdLine.getOptionValue('P');
			if (cmdLine.hasOption('S')) _params._server        = cmdLine.getOptionValue('S');
			if (cmdLine.hasOption('D')) _params._db            = cmdLine.getOptionValue('D');
			if (cmdLine.hasOption('u')) _params._url           = cmdLine.getOptionValue('u');
			if (cmdLine.hasOption('p')) _params._profile       = cmdLine.getOptionValue('p');
			if (cmdLine.hasOption('A')) _params._action        = ActionType.fromString(cmdLine.getOptionValue('A'));
			if (cmdLine.hasOption('a')) _params._actionOutFile = cmdLine.getOptionValue('a');

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
			printHelp(null, "Missing mandatory parameter '--profile <profile>' or '--server <srvName>'.");


		// Some default values (if something is specified on the LEFT side and nothing is specified on the RIGHT side... 
		// use the LEFT hand sides values  
		if (_params._rightCatFilter == null && _params._leftCatFilter != null)
			_params._rightCatFilter = _params._leftCatFilter;

		if (_params._rightSchFilter == null && _params._leftSchFilter != null)
			_params._rightSchFilter = _params._leftSchFilter;

		if (_params._rightTabFilter.equals("%") && !_params._leftTabFilter.equals("%"))
			_params._rightTabFilter = _params._leftTabFilter;

		if (_params._rightSkipList.isEmpty() && !_params._leftSkipList.isEmpty())
			_params._rightSkipList = _params._leftSkipList;
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

		options.addOption( "c", "leftCatFilter",   true,    "" );
		options.addOption( "o", "leftSchFilter",   true,    "" );
		options.addOption( "t", "leftTabFilter",   true,    "" );
		options.addOption( "f", "leftSkipList",    true,    "" );

		options.addOption( "C", "rightCatFilter",  true,    "" );
		options.addOption( "O", "rightSchFilter",  true,    "" );
		options.addOption( "T", "rightTabFilter",  true,    "" );
		options.addOption( "F", "rightSkipList",   true,    "" );

		options.addOption( "s", "autoSkip",        false,   "Automatically Skip all tables that are NOT on both sides (shortcut for -f|-F)" );
		options.addOption( "d", "dryRun",          false,   "Do not execute the \\tabdiff" );

		options.addOption( "A", "action",          true,    "what action to do with difference" );
		options.addOption( "a", "actionOutFile",   true,    "Write actions to file" );
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

		if (errorStr != null)
		{
			sb.append("\n");
			sb.append(errorStr);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append("usage: \\dbdiff ... \n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -U,--user <user>          Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd \n");
		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port). \n");
		sb.append("  -D,--dbname <dbname>      Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>    Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -p,--profile <name>       Take user/passwd/url from a connection profile.\n");
		sb.append("\n");
		sb.append("  -c,--leftCatFilter <val>  Filter for left jdbc: getMetaData().getTables(*cat*, sch, tab). (default=<currentCatalog>)\n");
		sb.append("  -o,--leftSchFilter <val>  Filter for left jdbc: getMetaData().getTables(cat, *sch*, tab). (default=null)\n");
		sb.append("  -t,--leftTabFilter <val>  Filter for left jdbc: getMetaData().getTables(cat, sch, *tab*). (default=\"%\")\n");
		sb.append("  -f,--leftSkipList <list>  List of entries to SKIP from the LEFT side (can be a regex). \n");
		sb.append("\n");
		sb.append("  -C,--rightCatFilter <val> Filter for right jdbc: getMetaData().getTables(*cat*, sch, tab). (default=<currentCatalog>)\n");
		sb.append("  -O,--rightSchFilter <val> Filter for right jdbc: getMetaData().getTables(cat, *sch*, tab). (default=null)\n");
		sb.append("  -T,--rightTabFilter <val> Filter for right jdbc: getMetaData().getTables(cat, sch, *tab*). (default=\"%\")\n");
		sb.append("  -F,--rightSkipList <list> List of entries to SKIP from the LEFT side (can be a regex). \n");
		sb.append("\n");
		sb.append("  -s,--autoSkip             Automatically Skip all tables that are NOT on both sides (shortcut for -f|-F)\n");
		sb.append("  -d,--dryRun               Do not execute the generated '\\tabdiff' commands, only print what to execute.\n");
		sb.append("\n");
		sb.append("  -A,--action <name>        Action when differance. "+StringUtil.toCommaStr(ActionType.values())+" (default: "+ActionType.TABLE+") \n");
		sb.append("  -a,--actionOutFile <name> Write the action out put to a file. \n");
		sb.append("  -x,--debug                Debug, print some extra info \n");
		sb.append("  -X,--trace                Trace, print some extra info (more than debug)\n");
		sb.append("  -Y,--stdout               Print debug/trace messages to stdout as well\n");
		sb.append("  \n");
		sb.append("  \n");
		sb.append("Some extra info:\n");
		sb.append("  FIXME: describe what we do in here\n");
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
	}

	@Override
	public Statement getStatement()
	{
		return new StatementDummy();
	}

	@Override
	public boolean execute() throws SQLException
	{
		try
		{
			return localExecute();
		}
		catch(SQLException ex)
		{
			throw ex;
		}
		catch(Exception ex)
		{
			throw new SQLException(ex.getMessage(), ex);
		}
	}

	private boolean localExecute() throws Exception
	{
		DbxConnection leftConn = _conn;
		
		DiffContext context = new DiffContext();

		context.setGuiOwner(_guiOwner); // Handle to the SQL Window Component
		context.setProgressDialog(_progress);

		if (_params._debug)    { context.setMessageDebugLevel(1);  setMessageDebugLevel(1);  }
		if (_params._trace)    { context.setMessageDebugLevel(2);  setMessageDebugLevel(2);  }
		if (_params._toStdout) { context.setMessageToStdout(true); setMessageToStdout(true); }
//context.setMessageDebugLevel(1);  setMessageDebugLevel(1);
////context.setMessageDebugLevel(2);  setMessageDebugLevel(2);
//context.setMessageToStdout(true); setMessageToStdout(true);


		// Connect to RIGHT hand side
		if (_progress != null) _progress.setState("Connecting to RIGHT hand side DBMS");
		if (_params._debug)     addDebugMessage(  "Connecting to RIGHT hand side DBMS");

		DbxConnection rightConn = getRightConnection();
		

//		ConnectionProp leftConnProps  = leftConn  == null ? null : leftConn .getConnProp();
//		ConnectionProp rightConnProps = rightConn == null ? null : rightConn.getConnProp();


		// LEFT
		if (_progress != null) _progress.setState("Executing PRE SQL at LEFT hand side DBMS to get MetaData");
		if (_params._debug)     addDebugMessage(  "Executing PRE SQL at LEFT hand side DBMS to get MetaData");

//		List<SqlObjectName> leftTables = new ArrayList<>();
		List<String> leftTables = new ArrayList<>();
		String leftCurCat = null;
		try  { leftCurCat = leftConn.getCatalog(); if (StringUtil.isNullOrBlank(leftCurCat)) leftCurCat = null; } catch (SQLException ignore) {}
		if (_params._leftCatFilter != null) 
			leftCurCat = _params._leftCatFilter;
		ResultSet leftPreRs = leftConn.getMetaData().getTables(leftCurCat, _params._leftSchFilter, _params._leftTabFilter, new String[] {"TABLE"});
//		ResultSetTableModel leftPreRstm = new ResultSetTableModel(leftPreRs, "leftPreRs");
		while(leftPreRs.next())
		{
			String cat = leftPreRs.getString(1); // 1 TABLE_CAT   String => table catalog (may be null)
			String sch = leftPreRs.getString(2); // 2 TABLE_SCHEM String => table schema (may be null)
			String tab = leftPreRs.getString(3); // 3 TABLE_NAME  String => table name
//System.out.println("LEFT : cat='"+cat+"', sch='"+sch+"', tab='"+tab+"'.");

//			leftTables.add(new SqlObjectName(leftConn, SqlObjectName.toString(cat, sch, tab)));
//			String tabName = SqlObjectName.toString(cat, sch, tab);
			String tabName = toCatOrSchAndTable(cat, sch, tab);
			boolean skip = false;
			for (String regex : _params._leftSkipList)
			{
				if (tabName.matches(regex))
					skip = true;
			}
			if (skip)
				continue;
			leftTables.add(tabName);
		}
		leftPreRs.close();
		leftTables.sort(String.CASE_INSENSITIVE_ORDER);

		
		// RIGHT
		if (_progress != null) _progress.setState("Executing PRE SQL at RIGHT hand side DBMS to get MetaData");
		if (_params._debug)     addDebugMessage(  "Executing PRE SQL at RIGHT hand side DBMS to get MetaData");

//		List<SqlObjectName> rightTables = new ArrayList<>();
		List<String> rightTables = new ArrayList<>();
		String rightCurCat = null;
		try  { rightCurCat = rightConn.getCatalog(); if (StringUtil.isNullOrBlank(rightCurCat)) rightCurCat = null; } catch (SQLException ignore) {}
		if (_params._rightCatFilter != null) 
			rightCurCat = _params._rightCatFilter;
		ResultSet rightPreRs = rightConn.getMetaData().getTables(rightCurCat, _params._rightSchFilter, _params._rightTabFilter, new String[] {"TABLE"});
//		ResultSetTableModel rightPreRstm = new ResultSetTableModel(rightPreRs, "rightPreRs");
		while(rightPreRs.next())
		{
			String cat = rightPreRs.getString(1); // 1 TABLE_CAT   String => table catalog (may be null)
			String sch = rightPreRs.getString(2); // 2 TABLE_SCHEM String => table schema (may be null)
			String tab = rightPreRs.getString(3); // 3 TABLE_NAME  String => table name
//System.out.println("RIGHT: cat='"+cat+"', sch='"+sch+"', tab='"+tab+"'.");
			
//			rightTables.add(new SqlObjectName(rightConn, SqlObjectName.toString(cat, sch, tab)));
//			String tabName = SqlObjectName.toString(cat, sch, tab);
			String tabName = toCatOrSchAndTable(cat, sch, tab);
			boolean skip = false;
			for (String regex : _params._rightSkipList)
			{
				if (tabName.matches(regex))
					skip = true;
			}
			if (skip)
				continue;
			rightTables.add(tabName);
		}
		rightPreRs.close();
		rightTables.sort(String.CASE_INSENSITIVE_ORDER);

		List<String> onlyOnLeftSide  = new ArrayList<>(leftTables);  onlyOnLeftSide .removeAll(rightTables);
		List<String> onlyOnRightSide = new ArrayList<>(rightTables); onlyOnRightSide.removeAll(leftTables);
		
//System.out.println("LEFT  TABLES:");
//for (String sqlObj : leftTables)
//	System.out.println("    " + sqlObj);
//	
//System.out.println("RIGHT TABLES:");
//for (String sqlObj : rightTables)
//	System.out.println("    " + sqlObj);
//
//
//System.out.println("ONLY ON LEFT  TABLES:");
//for (String sqlObj : onlyOnLeftSide)
//	System.out.println("   <<< " + sqlObj);
//	
//System.out.println("ONLY ON RIGHT TABLES:");
//for (String sqlObj : onlyOnRightSide)
//	System.out.println("   >>> " + sqlObj);

		// close RIGHT Connection
		rightConn.close();

		if (!onlyOnLeftSide.isEmpty() && !onlyOnRightSide.isEmpty())
		{
			if ( _params._autoSkip )
			{
				// print DEBUG what tables we are SKIPPING
				for (String name : onlyOnLeftSide)
					addInfoMessage("Skipping LEFT side table '"+name+"'. Due to --autoSkip");

				for (String name : onlyOnRightSide)
					addInfoMessage("Skipping RIGHT side table '"+name+"'. Due to --autoSkip");

				// remove onlyOnLeftSide and onlyOnRightSide from the list we want to do
				leftTables .removeAll(onlyOnLeftSide);
				rightTables.removeAll(onlyOnRightSide);
			}
			else
			{
				throw new DiffException("Tables do not match on LEFT/RIGHT hand side. Please use --leftSkipList, --rightSkipList or --autoSkip. Tables only on LEFT side "+onlyOnLeftSide+", Tables only on on RIGHT side "+onlyOnRightSide);
			}
		}
		
		if (leftTables.size() != rightTables.size())
		{
			throw new DiffException("Tables COUNT do not match on LEFT/RIGHT hand side. TableCount left="+leftTables.size()+", right="+rightTables.size()+". Tables only on LEFT side "+onlyOnLeftSide+", Tables only on on RIGHT side "+onlyOnRightSide);
		}
		
		if (leftTables.isEmpty())
		{
			addInfoMessage("No tables was found... (after removing tables that was only found on left/right hand side).");
			return false;
		}

		int leftMaxLen  = 0;
		int rightMaxLen = 0;
		for (String name : leftTables)  leftMaxLen  = Math.max(leftMaxLen , name.length());
		for (String name : rightTables) rightMaxLen = Math.max(rightMaxLen, name.length());
			
		// Create \tabdif commands
		String cmdTerminator = ";";
//		String cmdTerminator = "\ngo";
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<leftTables.size(); i++)
		{
			String left  = leftTables .get(i);
			String right = rightTables.get(i);

			String tabDiffSwitches = _params.createTabDiffSwitches(left, right);
			
			// format the output for readability
			left  = StringUtil.left("'" + left  + "'", leftMaxLen +2);
			right = StringUtil.left("'" + right + "'", rightMaxLen+2);
			
			sb.append("\\tabdiff ").append("--left ").append(left).append(" --right ").append(right).append(" ").append(tabDiffSwitches).append(cmdTerminator).append("\n");
		}
		String tabDiffCommands = sb.toString();
		
		// Put \tabdiff commands in the output.
		addInfoMessage("The following '\\tabdiff' commands will be executed.\n\n" + tabDiffCommands);
		
		// execute the \tabdif commands
		// The execution is actually done by the caller, since this acted as a "preprocessor" to generate commands...
		if (_params._dryRun)
		{
			addInfoMessage("--dryRun was specified. The above commands would have been executed. You can copy (and change them) then execute manually OR remove the --dryRun");
		}
		else
		{
			_generatedCommands = tabDiffCommands;
			addInfoMessage("End of messages from '\\dbdiff'. The ABOVE Commands are now executed.");
		}

		return false; // true=We Have A ResultSet, false=No ResultSet
	}

	@Override
	public String getPostExecSqlCommands()
	{
		return _generatedCommands;
	}
	
	
	/**
	 * Return a String catName|schName.tabname
	 * <p>
	 * If Schema has value that will be used.<br>
	 * If Schema is null Catalog will be used (if that isn't null as well)
	 * 
	 * @param cat
	 * @param sch
	 * @param tab
	 * @return
	 */
	private String toCatOrSchAndTable(String cat, String sch, String tab)
	{
		StringBuilder sb = new StringBuilder();
		
		if ( ! StringUtil.hasValue(sch) )
		{
			if (StringUtil.hasValue(cat))
				sch = cat;
		}

		// addd: schema and table to output
		if (StringUtil.hasValue(sch)) 
			sb.append(sch).append(".");

		sb.append(tab);

		return sb.toString();
	}


	private DbxConnection getRightConnection()
	throws Exception
	{
		DbxConnection conn = null;
		ConnectionProp cp = new ConnectionProp();
		cp.setAppName("sqlw-tabdiff");
		
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
