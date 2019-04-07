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
package com.asetune.sql.pipe;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.swing.table.TableModel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.ConnectionProfile;
import com.asetune.gui.ConnectionProfile.ConnProfileEntry;
import com.asetune.gui.ConnectionProfile.JdbcEntry;
import com.asetune.gui.ConnectionProfile.TdsEntry;
import com.asetune.gui.ConnectionProfileManager;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.diff.DiffContext;
import com.asetune.sql.diff.DiffContext.DiffSide;
import com.asetune.sql.diff.DiffSink;
import com.asetune.sql.diff.actions.DiffTableModel;
import com.asetune.sql.diff.actions.GenerateSqlText;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

/**
 * This one should be able to parse a bunch of things.<br>
 * and also several pipes... cmd | cmd | cmd
 * <p>
 * 
 * ============================================================
 * diff
 * ============================================================
 * ------------------------------------------------------------
 * 
 */
public class PipeCommandDiff
extends PipeCommandAbstract
{
	private static Logger _logger = Logger.getLogger(PipeCommandDiff.class);
	
	private String[] _args = null;

	public enum ActionType
	{
		TABLE,               // as a GUI Table
		SQL_LEFT,            // SQL Text that can be used to FIX the LEFT side of a DIFF
		SQL_RIGHT,           // SQL Text that can be used to FIX the RIGHT side of a DIFF
		SYNC_LEFT,           // execute SQL on the LEFT side to become in sync with the RIGHT side
		SYNC_RIGHT;           // execute SQL on the RIGHT side to become in sync with the LEFT side
		
		/** parse the value */
		public static ActionType fromString(String text)
		{
			for (ActionType type : ActionType.values()) 
			{
				// check for upper/lower: 
				if (type.name().equalsIgnoreCase(text))
					return type;

				// check for camelCase: 'valXxxYyy', 'valxxxyyy'
				if (type.name().replace("_", "").equalsIgnoreCase(text))
					return type;
			}

			throw new IllegalArgumentException("Unknown ActionType '" + text + "' found, possible values: "+StringUtil.toCommaStr(ActionType.values()));
		}
	};

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
		String    _query           = null;

		ActionType _action         = null; 
		String     _actionOutFile  = null; 
		String     _execBeforeSync = null;
		String     _execAfterSync  = null;
		boolean    _execSyncInTran = true;

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
			sb.append(", ").append("debug         ".trim()).append("=").append(StringUtil.quotify(_debug         ));
			sb.append(", ").append("trace         ".trim()).append("=").append(StringUtil.quotify(_trace         ));
			sb.append(", ").append("toStdout      ".trim()).append("=").append(StringUtil.quotify(_toStdout      ));
			sb.append(", ").append("keyCols       ".trim()).append("=").append(StringUtil.quotify(_keyCols       ));
			sb.append(", ").append("query         ".trim()).append("=").append(StringUtil.quotify(_query         ));
			sb.append(", ").append("action        ".trim()).append("=").append(StringUtil.quotify(_action        ));
			sb.append(", ").append("actionOutFile ".trim()).append("=").append(StringUtil.quotify(_actionOutFile ));
			sb.append(", ").append("execBeforeSync".trim()).append("=").append(StringUtil.quotify(_execBeforeSync));
			sb.append(", ").append("execAfterSync ".trim()).append("=").append(StringUtil.quotify(_execAfterSync ));
			sb.append(", ").append("execSyncInTran".trim()).append("=").append(StringUtil.quotify(_execSyncInTran));

			return sb.toString();
		}
	}
	
	private CmdParams _params = null;
	
	// if -p profileName is given... then store the profile's Connection Properties in this one... 
	private ConnectionProfile _rightConnectionProfile = null;

	
	public String        getCmdLineParams()   { return _params.toString();       }
	
	public String        getQuery()           { return _params._query;           }
	public List<String>  getKeyCols()         { return _params._keyCols;         }
	public boolean       isDebugEnabled()     { return _params._debug;           }




	//-----------------------
	// Parameter type to getEndPointResult
	//-----------------------
//	public static final String rowsSource = "rowsSource";
//	public static final String rowsTarget = "rowsTarget";
//
//	public static final String rstmSource = "rstmSource";
//	public static final String rstmTarget = "rstmTarget";
//	public static final String rstmDiff   = "rstmDiff";


//	private int        _rowsSource  = 0;
//	private int        _rowsTarget  = 0;







	public PipeCommandDiff(String input, String sqlString)
	throws PipeCommandException
	{
		super(input, sqlString);
		parse(input);
	}

	public void parse(String input)
	throws PipeCommandException
	{
		if ( input.startsWith("diff ") || input.equals("diff") )
		{
			_args = StringUtil.translateCommandline(input, true);

			if (_args.length > 1)
			{
				CommandLine cmdLine = parseCmdLine(_args);
				if (cmdLine.hasOption('q')) _params._query           = cmdLine.getOptionValue('q');
				if (cmdLine.hasOption('k')) _params._keyCols         = StringUtil.commaStrToList(cmdLine.getOptionValue('k'));
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
				if (cmdLine.hasOption('o')) _params._actionOutFile = cmdLine.getOptionValue('o');
			}
			else
			{
				printHelp(null, "Please specify some parameters.");
			}
			
			checkParsedParameters(_params);

			if (_params._debug)
				addDebugMessage("CmdLineSwitches: "+_params);
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: diff");
		}
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

	}

//	private CommandLine parseCmdLine(String args)
//	throws PipeCommandException
//	{
//		return parseCmdLine(StringUtil.translateCommandline(args));
////		return parseCmdLine(args.split(" "));
//	}
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
		options.addOption( "q", "query",           true,    "SQL Query towards destination" );
		options.addOption( "k", "keyCols",         true,    "" );
		options.addOption( "A", "action",          true,    "what action to do with difference" );
		options.addOption( "o", "actionOutFile",   true,    "Write actions to file" );
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

//		sb.append("\n");
//		sb.append("usage: graph or chart [-d] [-t <type>] [-p] [-3] [-k <csv>] [-v <csv>] \n");
//		sb.append("                      [-n <name>] [-l <label>] [-L <label>] [-c] [-r <regEx>]\n");
//		sb.append("                      [-w <width>] [-h <height>] [-D] [-S] [-W] [-x]\n");
//		sb.append("\n");
//		sb.append("options: \n");
//		sb.append("  -d,--data                 Also add table data to the output \n");
//		sb.append("  -t,--type        <type>   What type of graph do you want to produce. \n");
//		sb.append("                   auto      - Try to figgure out what you want (default)\n");
//		sb.append("                   bar       - bar graph. \n");
//		sb.append("                   sbar      - stacked bar graph. \n");
//		sb.append("                   area      - area graph. \n");
//		sb.append("                   sarea     - stacked area graph. \n");
//		sb.append("                   line      - line chart. \n");
//		sb.append("                   pie       - pie chart. \n");
//		sb.append("                   ts        - time series data. \n");
//		sb.append("  -p,--pivot                Turn the columns into rows or vice verse (based on graph type)\n");
//		sb.append("  -3,--3d                   If possible use 3D graphs/charts. \n");
//		sb.append("  -k,--keyCols              Comma separated list of KEY columns to use: ColNames or ColPos (pos starts at 0) \n");
//		sb.append("  -v,--valCols              Comma separated list of VALUE columns to use ColNames or ColPos (pos starts at 0) \n");
//		sb.append("  -n,--name          name   Name of the graph. (printed on top) \n");
//		sb.append("  -l,--labelCategory name   Label for Categories \n");
//		sb.append("  -L,--labelValue    name   Label for Values \n");
//		sb.append("  -c,--str2num              Try to convert String Columns to numbers. \n");
//		sb.append("  -r,--removeRegEx   str    In combination with '-c', remove some strings column content using a RegEx \n");
//		sb.append("                             - example to remove KB or KB from columns: go | graph -c -r '(KB|MB)'\n");
//		sb.append("\n");
//		sb.append("  -w,--width         spec   Width  of the graph/chart \n");
//		sb.append("  -h,--height        spec   Height of the graph/chart \n");
//		sb.append("\n");
//		sb.append("  -D,--showDataValues       Show Data Values in graphs (easier to see data values)\n");
//		sb.append("  -S,--showShapes           Show Shapes/boxes on data points (easier see data points in smaller datasets) \n");
//		sb.append("  -W,--window               Open Graph/Chart in it's own Windows. \n");
//		sb.append("  -x,--debug                Debug, print some extra info \n");
//		sb.append("  \n");
//		sb.append("  \n");

		sb.append("\n");
		sb.append("usage: diff [-q <sql-query>] [-k <csv>] \n");
		sb.append("            [-x]\n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -U,--user <user>          Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd \n");
		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port). \n");
		sb.append("  -D,--dbname <dbname>      Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>    Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -p,--profile <name>       Take user/passwd/url from a connection profile.\n");
		sb.append("\n");
		sb.append("  -q,--query <sq-query>     SQL Statement to exexute towards destination. (default same as source) \n");
		sb.append("  -k,--keyCols <c1,c2...>   Comma separated list of KEY columns to use: ColNames or ColPos (pos starts at 0) \n");
		sb.append("  -A,--action <name>        Action when differance. "+StringUtil.toCommaStr(ActionType.values())+" (default: "+ActionType.TABLE+") \n");
		sb.append("  -o,--actionOutFile <name> Write the action out put to a file. \n");
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
		sb.append("\n");
		sb.append("Example: \n");
		sb.append("    -- check difference on the whole table with another server \n");
		sb.append("    select * from t1 \n");
		sb.append("    go | diff -Usa -Psecret -Shost:port -Ddbname \n");
		sb.append("\n");
		sb.append("  or: \n");
		sb.append("    -- Just a subset of the table: some columns and some rows... \n");
		sb.append("    select id, c1, c2, c3 from t1 where city = 'stockholm' \n");
		sb.append("    go | diff -Usa -Psecret --url 'jdbc:postgresql://192.168.0.110:5432/dbname' \n");
		sb.append("\n");
		sb.append("  or: \n");
		sb.append("    -- Do aggregate and compare that... Connect using a Connection Profile \n");
		sb.append("    select type, count(*) as cnt from sysobjects group by type order by type \n");
		sb.append("    go | diff -p 'profile name' -Ddbname --keyCols type\n");
		sb.append("\n");
		sb.append("\n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	

//	private List<String> findPrimaryKeyColsForResultSet(ResultSet rs)
//	{
//		try
//		{
//			int cols = rs.getMetaData().getColumnCount();
//			Set<String> tables = new HashSet<>(); 
//			for (int c=1; c<=cols; c++)
//			{
//				String cat = rs.getMetaData().getCatalogName(c);
//				String sch = rs.getMetaData().getSchemaName(c);
//				String obj = rs.getMetaData().getTableName(c);
//				
//				tables.add( SqlObjectName.toString(cat, sch, obj) );
//			}
//			
//			List<String> pkCols = new ArrayList<>(); 
//
//			if ( tables.size() == 1)
//			{
//				// there is no: Set.get(0);
//				// So iterate (on the SINGLE row) was simplest
//				for (String entry : tables)
//				{
//					Connection conn = rs.getStatement().getConnection();
//					SqlObjectName obj = new SqlObjectName(conn, entry);
//
//					pkCols = TableInfo.getPkOrFirstUniqueIndex(conn, obj.getCatalogNameNull(), obj.getSchemaNameNull(), obj.getObjectName());
//
//					if (pkCols.isEmpty())
//						addWarningMessage("Find PkCols: NO Primary Keys (or unique index) was found for the table '"+entry+"'.");
//					else
//						addInfoMessage("Find PkCols: The following columns "+pkCols+" will be used as a Primary Key Columns for DIFF. TablesInResultSet="+tables);
//				}
//			}
//			else
//			{
//				addWarningMessage("Find PkCols: The ResultSet contained "+tables.size()+" table references, Sorry i can only figgure out the PK Cols if the ResultSet references only 1 table. Referenced Tables="+tables);
//			}
//			
//			if (pkCols.isEmpty())
//				pkCols = null;
//
//			return pkCols;
//		}
//		catch(SQLException ex)
//		{
//			addErrorMessage("Find PkCols: Problems trying to get Primary Key Columns from the source ResultSet. Caught: " + ex);
//			_logger.error("Problems trying to get Primary Key Columns from the source ResultSet", ex);
//			
//			return null;
//		}
//	}


//	@Override
//	public void doEndPoint(Object input, SqlProgressDialog progress) 
//	throws Exception 
//	{
//		if ( ! (input instanceof ResultSet) || (input instanceof ResultSetTableModel) )
//			throw new Exception("Expected ResultSet or ResultSetTableModel as input parameter");
//
//		// Get keys from the source ResultSet
//		if (_params._keyCols == null && input instanceof ResultSet)
//		{
//			addInfoMessage("No Primary Key Columns was specified using the command line parameter -k or --keyCols ... Trying to get that information from the ResultSet.");
//			_params._keyCols = findPrimaryKeyColsForResultSet((ResultSet) input);
//
//			if (_params._keyCols == null)
//				addInfoMessage("Still NO Primary Key Columns... The diff will use 'simple row by row' comparison.");
//
//		}
//		
//		ResultSetDiff diff = new ResultSetDiff(_params, progress);
//
//		// if it's a ResultSet, then transform it into a ResultSetTableModel
//		ResultSetTableModel rstm = null;
//		if ( input instanceof ResultSetTableModel )
//		{
//			rstm = (ResultSetTableModel) input;
//		}
//		else
//		{
//			rstm = new ResultSetTableModel((ResultSet)input, false, "sqlw-diff-source-rstm", -1, -1, false, null, progress);
//		}
//
//		// Do the work
//		diff.open();
//		diff.doWork( rstm, this );
//		diff.close();
//	}

	@Override
	public void doEndPoint(Object input, SqlProgressDialog progress) 
	throws Exception 
	{
		if ( ! (input instanceof ResultSet) )
			throw new Exception("Expected ResultSet or as input parameter");

		ResultSet leftRs = (ResultSet) input;

		DbxConnection leftConn = _leftConn;
		
		
		DiffContext context = new DiffContext();

		context.setGuiOwner(getGuiOwner()); // Handle to the SQL Window Component
		context.setProgressDialog(progress);

		if (_params._debug)    context.setMessageDebugLevel(1);
		if (_params._trace)    context.setMessageDebugLevel(2);
		if (_params._toStdout) context.setMessageToStdout(true);

		// Connect to RIGHT hand side
		if (progress != null)
			progress.setState("Connecting to RIGHT hand side DBMS");

		DbxConnection rightConn = getRightConnection();

		// Get the Query, which will be sent to the RIGHT Hand Side
		String destSql = _params._query;
		if (StringUtil.isNullOrBlank(destSql))
			destSql = getSqlString();

		if (progress != null) progress.setState("Executing SQL at RIGHT hand side DBMS, SQL: "+destSql);
		if (_params._debug)   addDebugMessage(  "Executing SQL at target: " + destSql);

		// Execute SQL at RIGHT Hand side
		Statement targetStmt = rightConn.createStatement();
		ResultSet rightRs = targetStmt.executeQuery(destSql);

		if (progress != null) progress.setState("Initializing and validating DIFF Engine...");
		if (_params._debug)   addDebugMessage  ("Initializing and validating DIFF Engine...");

		ConnectionProp leftConnProps  = leftConn  == null ? null : leftConn .getConnProp();
		ConnectionProp rightConnProps = rightConn == null ? null : rightConn.getConnProp();
//System.out.println("LEFT:  connProps: " + leftConnProps);
//System.out.println("RIGHT: connProps: " + rightConnProps);
		
		context.setPkColumns(_params._keyCols);
		context.setDiffTable(DiffSide.LEFT,  leftRs,  leftConnProps);
		context.setDiffTable(DiffSide.RIGHT, rightRs, rightConnProps);

		context.validate();

		if (progress != null) progress.setState("Do DIFF Logic...");
		if (_params._debug)   addDebugMessage  ("Do DIFF Logic...");

		//----------------------
		// DIFF LOGIC
		int diffCount = context.doDiff();

		if (progress != null) progress.setState("Done: Total Diff Count = " + diffCount);
		if (_params._debug)   addDebugMessage  ("Done: Total Diff Count = " + diffCount);

		// Transfer messages that was generated during "diff" to "this" message system
		if (context.hasMessages())
			addMessages(context.getMessages());

		// Check results
//		String tabInfo = "[" + context.getLeftDt().getFullTableName() + ", " + context.getRightDt().getFullTableName() + "]";
		String tabInfo = "[" + context.getLeftDt().getShortTableName() + "]";
		if (diffCount == 0)
		{
			addInfoMessage("OK - " + tabInfo + " Left and Right ResultSet has NO difference \n"
					+ "           Left:  RowCount=" + context.getLeftDt() .getRowCount() + ", ColCount=" + context.getLeftDt() .getColumnCount() + ", PkCols=" + context.getLeftDt() .getPkColumnNames() + ", TabName='" + context.getLeftDt() .getFullTableName() + "'.\n"
					+ "           Right: RowCount=" + context.getRightDt().getRowCount() + ", ColCount=" + context.getRightDt().getColumnCount() + ", pkCols=" + context.getRightDt().getPkColumnNames() + ", TabName='" + context.getRightDt().getFullTableName() + "'."
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
					+ "           Left:  RowCount=" + context.getLeftDt() .getRowCount() + ", ColCount=" + context.getLeftDt() .getColumnCount() + ", PkCols=" + context.getLeftDt() .getPkColumnNames() + ", TabName='" + context.getLeftDt() .getFullTableName() + "'.\n"
					+ "           Right: RowCount=" + context.getRightDt().getRowCount() + ", ColCount=" + context.getRightDt().getColumnCount() + ", pkCols=" + context.getRightDt().getPkColumnNames() + ", TabName='" + context.getRightDt().getFullTableName() + "'.\n"
					+ "        NOTE: You can turn on debugging '-x' to see what SQL that is issued on left/right hand side."
					;

			addErrorMessage(tabInfo + "Left and Right ResultSet is DIFFERENT. \n" + msg);
		}


		// has DIFFERENCE, do some extra stuff
		if (diffCount != 0)
		{
			// Actions to be taken on the DIFF Results
			if (ActionType.TABLE.equals(_params._action) || _params._action == null)
			{
				_diffTableModel = new DiffTableModel(context);

				// Write to OUTPUT File
				if (StringUtil.hasValue(_params._actionOutFile))
				{
					File f = new File(_params._actionOutFile);
					addInfoMessage("Saving "+_params._action+" output to file: "+f);

					//String tableStr = ""; SwingUtils.tableToString(_diffTableModel); // FIXME: strip out ALL HTML tags
					String tableStr = SwingUtils.tableToHtmlString(_diffTableModel);
					FileUtils.write(f, tableStr, StandardCharsets.UTF_8);
				}
			}
			else if (ActionType.SQL_LEFT.equals(_params._action))
			{
				GenerateSqlText sqlGen = new GenerateSqlText(context, leftConn);
				List<String> dmlList = sqlGen.getSql(DiffSide.LEFT);

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
				List<String> dmlList = sqlGen.getSql(DiffSide.RIGHT);
				
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
				_diffTableModel = new DiffTableModel(context);
			}
			else if (ActionType.SYNC_RIGHT.equals(_params._action))
			{
				addErrorMessage("SYNC_RIGHT is not yet implemented, instead a difference table will be generated.");
				_diffTableModel = new DiffTableModel(context);
			}
		} // end: has DIFFERENCE

		// close RIGHT Connection
		rightConn.close();
	}

	private DbxConnection getRightConnection()
	throws Exception
	{
		DbxConnection conn = null;
		ConnectionProp cp = new ConnectionProp();
		cp.setAppName("sqlw-diff");
		
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
		
		
//		if (StringUtil.hasValue(_params._server))
//		{
//			Properties props = new Properties();
//			
//			String hostPortStr = null;
//			if ( _params._server.contains(":") )
//				hostPortStr = _params._server;
//			else
//				hostPortStr = AseConnectionFactory.getIHostPortStr(_params._server);
//
//			if (StringUtil.isNullOrBlank(hostPortStr))
//				throw new Exception("Can't find server name information about '"+_params._server+"', hostPortStr=null. Please try with -S hostname:port");
//
//			if (_params._debug)
//				addDebugMessage("Creating connection to ASE: hostPortStr='"+hostPortStr+"', dbname='"+_params._db+"', user='"+_params._user+"', applicationName='sqlw-diff'.");
//
//			conn = DbxConnection.createDbxConnection(AseConnectionFactory.getConnection(hostPortStr, _params._db, _params._user, _params._passwd, "sqlw-diff", Version.getVersionStr(), null, props, (ConnectionProgressCallback)null));
//
//			if ( ! StringUtil.isNullOrBlank(_params._db) )
//				AseConnectionUtils.useDbname(conn, _params._db);
//		}
//		else
//		{
////			throw new Exception("-u|--url option has not yet been implemented.");
//			
//			if (StringUtil.hasValue(_params._driver))
//			{
//				try { Class.forName(_params._driver).newInstance(); }
//				catch (Exception ignore) {}
//			}
//			Properties props = new Properties();
//			props.put("user", _params._user);
//			props.put("password", _params._passwd);
//	
//			String msg = "Try getConnection to driver='"+_params._driver+"', url='"+_params._url+"', user='"+_params._user+"'.";
//			addDebugMessage(msg);
//			_logger.debug(msg);
//			conn = DbxConnection.createDbxConnection(DriverManager.getConnection(_params._url, props));
//		}

		if (StringUtil.hasValue(_params._server))
		{
//			Properties props = new Properties();
			
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

//			if (_params._debug)
//				addDebugMessage("Creating connection to ASE: hostPortStr='"+hostPortStr+"', dbname='"+_params._db+"', user='"+_params._user+"', applicationName='sqlw-diff'.");
//
//			conn = DbxConnection.createDbxConnection(AseConnectionFactory.getConnection(hostPortStr, _params._db, _params._user, _params._passwd, "sqlw-diff", Version.getVersionStr(), null, props, (ConnectionProgressCallback)null));
//
//			if ( ! StringUtil.isNullOrBlank(_params._db) )
//				AseConnectionUtils.useDbname(conn, _params._db);
		}
		else
		{
//			throw new Exception("-u|--url option has not yet been implemented.");
			
//			if (StringUtil.hasValue(_params._driver))
//			{
//				try { Class.forName(_params._driver).newInstance(); }
//				catch (Exception ignore) {}
//			}
//			Properties props = new Properties();
//			props.put("user", _params._user);
//			props.put("password", _params._passwd);
//	
//			String msg = "Try getConnection to driver='"+_params._driver+"', url='"+_params._url+"', user='"+_params._user+"'.";
//			addDebugMessage(msg);
//			_logger.debug(msg);
//			conn = DbxConnection.createDbxConnection(DriverManager.getConnection(_params._url, props));
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

	/**
	 * Get 'rowsSelected' from the select statement or 'rowsInserted' of the INSERT command. 
	 * @return an integer of the desired type
	 */
	@Override
	public Object getEndPointResult(String type)
	{
		if (type == null)
			throw new IllegalArgumentException("Input argument/type cant be null.");
		
//		if (rowsSelected.equals(type))
//		{
//			return _rowsSelected;
//		}
//		else if (rowsInserted.equals(type))
//		{
//			return _rowsInserted;
//		}
//		else if (sqlWarnings.equals(type))
//		{
//			return _sqlWarnings;
//		}
//		else
//		{
//			throw new IllegalArgumentException("Input argument/type '"+type+"' is unknown. Known types '"+rowsSelected+"', '"+rowsInserted+"'.");
//		}

		return null;
	}

	@Override 
	public String getConfig()
	{
		return _params == null ? null : _params.toString();
	}

	
	
//	//--------------------------------------------------------------------------------------------
//	// WORKER CLASS
//	//--------------------------------------------------------------------------------------------
//	private class ResultSetDiff
//	{
//		private DbxConnection _conn      = null;
//		private CmdParams     _cmdParams = null;
//		private SqlProgressDialog _progressDialog = null;
////		private String _qic = "\""; // Quoted Identifier Char 
//
////		int	_numcols;
////
////		private ArrayList<String>            _type        = new ArrayList<String>();
////		private ArrayList<String>            _sqlTypeStr  = new ArrayList<String>();
////		private ArrayList<Integer>           _sqlTypeInt  = new ArrayList<Integer>();
////		private ArrayList<String>            _cols        = new ArrayList<String>();
////		private ArrayList<Integer>           _displaySize = new ArrayList<Integer>();
////		private ArrayList<ArrayList<Object>> _rows        = new ArrayList<ArrayList<Object>>();
////		private String                       _name        = null;
//
//		public ResultSetDiff(CmdParams params, SqlProgressDialog progressDialog)
//		{
//			_cmdParams = params;
//			_progressDialog = progressDialog;
//		}
//		
//		public void open()
//		throws Exception
//		{
//			if (StringUtil.hasValue(_cmdParams._server))
//			{
//				Properties props = new Properties();
//				
//				String hostPortStr = null;
//				if ( _cmdParams._server.contains(":") )
//					hostPortStr = _cmdParams._server;
//				else
//					hostPortStr = AseConnectionFactory.getIHostPortStr(_cmdParams._server);
//
//				if (StringUtil.isNullOrBlank(hostPortStr))
//					throw new Exception("Can't find server name information about '"+_cmdParams._server+"', hostPortStr=null. Please try with -S hostname:port");
//
//				if (_cmdParams._debug)
//					addDebugMessage("Creating connection to ASE: hostPortStr='"+hostPortStr+"', dbname='"+_cmdParams._db+"', user='"+_cmdParams._user+"', applicationName='sqlw-diff'.");
//
//				_conn = DbxConnection.createDbxConnection(AseConnectionFactory.getConnection(hostPortStr, _cmdParams._db, _cmdParams._user, _cmdParams._passwd, "sqlw-diff", Version.getVersionStr(), null, props, (ConnectionProgressCallback)null));
//
//				if ( ! StringUtil.isNullOrBlank(_cmdParams._db) )
//					AseConnectionUtils.useDbname(_conn, _cmdParams._db);
//			}
//			else
//			{
////				throw new Exception("-u|--url option has not yet been implemented.");
//				
//				if (StringUtil.hasValue(_cmdParams._driver))
//				{
//					try { Class.forName(_cmdParams._driver).newInstance(); }
//					catch (Exception ignore) {}
//				}
//				Properties props = new Properties();
//				props.put("user", _cmdParams._user);
//				props.put("password", _cmdParams._passwd);
//		
//				String msg = "Try getConnection to driver='"+_cmdParams._driver+"', url='"+_cmdParams._url+"', user='"+_cmdParams._user+"'.";
//				addDebugMessage(msg);
//				_logger.debug(msg);
//				_conn = DbxConnection.createDbxConnection(DriverManager.getConnection(_cmdParams._url, props));
//			}
//
//			// Print out some destination information
//			try
//			{
//				DatabaseMetaData dbmd = _conn.getMetaData();
//				String msg;
//
//				try { msg = "Connected to URL '"                       + dbmd.getURL()                    +"'."; _logger.info(msg); if (_cmdParams._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
//				try { msg = "Connected using driver name '"            + dbmd.getDriverName()             +"'."; _logger.info(msg); if (_cmdParams._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
//				try { msg = "Connected using driver version '"         + dbmd.getDriverVersion()          +"'."; _logger.info(msg); if (_cmdParams._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
//				try { msg = "Connected to destination DBMS Vendor '"   + dbmd.getDatabaseProductName()    +"'."; _logger.info(msg); if (_cmdParams._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
//				try { msg = "Connected to destination DBMS Version '"  + dbmd.getDatabaseProductVersion() +"'."; _logger.info(msg); if (_cmdParams._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
//				try { msg = "Current Catalog in the destination srv '" + _conn.getCatalog()               +"'."; _logger.info(msg); if (_cmdParams._debug) addDebugMessage(msg);} catch (SQLException ignore) {}
//			}
//			catch (SQLException ignore) {}
//
//			// Execute the SQL InitString
//			if (StringUtil.hasValue(_cmdParams._initStr))
//			{
//				String msg = "executing initialization SQL Stement '"+_cmdParams._initStr+"'.";
//				addDebugMessage(msg);
//				_logger.info(msg);
//				Statement stmnt = _conn.createStatement();
//				stmnt.executeUpdate(_cmdParams._initStr);
//				stmnt.close();
//			}
//		}
//
//		public void close()
//		throws Exception
//		{
//			if (_conn != null)
//			{
//				String msg = "Closing connection to '" + _conn.getMetaData().getURL() + "'.";
//				if (_cmdParams._debug)
//					addDebugMessage(msg);
//				_logger.info(msg);
//
//				_conn.close();
//			}
//		}
//
//
//		// Check the below to borrow some ideas from:
//		// - C:\projects\DbxTune\src\com\asetune\cm\CounterSample.java
//		// - https://github.com/paulfitz/coopy
//		// - C:\projects\tmp\Metaqa.java
//		
//		public int doWork(ResultSetTableModel sourceRstm, PipeCommandDiff pipeCmd)
//		throws Exception
//		{
//			int sourceNumCols = -1;
//			int destNumCols   = -1;
//			
//			// Do dummy SQL to get RSMD from DEST
//			String destSql    = _cmdParams._query;
//			if (StringUtil.isNullOrBlank(destSql))
//			{
//				destSql = pipeCmd.getSqlString();
//			}
//
//			if (_progressDialog != null)
//				_progressDialog.setState("Getting data from DIFF-Target, SQL: "+destSql);
//
//			if (_cmdParams._debug)
//				addDebugMessage("Executing SQL at target: " + destSql);
//
//			Statement targetStmt = _conn.createStatement();
//			ResultSet targetRs = targetStmt.executeQuery(destSql);
//
////			ResultSetTableModel targetRstm = new ResultSetTableModel(targetRs, false, "sqlw-diff-target-rstm", -1, -1, false, this, _progressDialog);
//			ResultSetTableModel targetRstm = new ResultSetTableModel(targetRs, false, "sqlw-diff-target-rstm", -1, -1, false, null, _progressDialog);
//			
//			if (_progressDialog != null)
//				_progressDialog.setState("Do DIFF Logic...");
//
//
//			_rowsSource = sourceRstm.getRowCount();
//			_rowsTarget = targetRstm.getRowCount();
//
//			if (_params._debug)
//			{
//				addDebugMessage("sourceRowc="+sourceRstm.getRowCount()+", colCount="+sourceRstm.getColumnCount());
//				addDebugMessage("targetRowc="+targetRstm.getRowCount()+", colCount="+targetRstm.getColumnCount());
//			}
//			
//			try
//			{
//				boolean diffOk = diff(sourceRstm, targetRstm);
//				if (diffOk)
//				{
//					addInfoMessage("OK - Source and Target ResultSet looks the same");
//				}
//				else
//				{
//					addErrorMessage("Source and Target ResultSet do NOT has the same content.");
//				}
//			}
//			catch (Exception ex)
//			{
//				addErrorMessage("Exception: "+ex);
//				_logger.error("Some problems when doing diff...", ex);
//			}
//			
////			// Check if "transfer" will work
////			if (sourceNumCols != destNumCols)
////			{
////				// TODO: should we close the sourceRs or not????
////				throw new Exception("Source ResultSet and Destination Table does not have the same column count (source="+sourceNumCols+", dest="+destNumCols+").");
////			}
////			
////			// Make warning if source/destination data types does NOT match
////			for (int c=0; c<sourceNumCols; c++)
////			{
////				int sourceType = sourceSqlTypeInt.get(c);
////				int destType   = destSqlTypeInt  .get(c);
////				
////				if (sourceType != destType)
////				{
////					String sourceJdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(sourceType);
////					String destJdbcTypeStr   = ResultSetTableModel.getColumnJavaSqlTypeName(destType);
////
////					String sourceColName = sourceColNames.get(c);
////					String destColName   = destColNames  .get(c);
////
////					String warning = "Possible column datatype missmatch for column "+(c+1)+". Source column name '"+sourceColName+"', jdbcType '"+sourceJdbcTypeStr+"'. Destination column name '"+destColName+"', jdbcType '"+destJdbcTypeStr+"'. I will still try to do the transfer, hopefully the destination server can/will convert the datatype, so it will work... lets try!"; 
////					_logger.warn(warning);
////					
////					if (pipeCmd._sqlWarnings == null)
////						pipeCmd._sqlWarnings = new SQLWarning("Some problems where found during the DIFF Operation.");
////					pipeCmd._sqlWarnings.setNextWarning(new SQLWarning(warning));
////				}
////			}
//			
//			return 999;
//		}
//
//		private boolean diff(ResultSetTableModel source, ResultSetTableModel target) 
//		throws Exception
//		{
//			boolean diffIsOk = true;
//			
////			ResultSetTableModel diff = new ResultSetTableModel(rs, name)
//			int sourceRowc = source.getRowCount();
//			int sourceColc = source.getColumnCount();
//
//			int targetRowc = target.getRowCount();
//			int targetColc = target.getColumnCount();
//
//			if (sourceColc != targetColc)
//			{
//				throw new Exception("Diff can't even start. COLUMN COUNT IS DIFFERENT. ColumnCount=[source="+sourceColc+",target="+targetColc+"], RowCount=[source="+sourceRowc+",target="+targetRowc+"].");
//			}
//			
//			for (int r=0; r<sourceRowc; r++)
//			{
//				if ( r >= targetRowc)
//					break;
//				
//				for (int c=0; c<sourceColc; c++)
//				{
//					Object sourceColVal = source.getValueAsObject(r, c);
//					Object targetColVal = target.getValueAsObject(r, c);
//					
//					if (isColumnValueEqual(sourceColVal, targetColVal))
//						continue;
//					
//					if (_cmdParams._debug)
//						addDebugMessage("DIFFERENT: row="+r+",col="+c+": source['"+source.getColumnName(c)+"'].Val=["+sourceColVal+"], target['"+target.getColumnName(c)+"'].Val=["+targetColVal+"]");
//					diffIsOk = false;
//				}
//			}
//			
//			if (sourceRowc < targetRowc)
//			{
//				if (_cmdParams._debug)
//					addDebugMessage("EXTRA TARGET ROWS: ");
//
//				for (int r=sourceRowc-1; r<targetRowc; r++)
//				{
//					if (_cmdParams._debug)
//						addDebugMessage("Target Row["+r+"]: is missing in source. Target row: " + target.toStringRow(r));
//					diffIsOk = false;
//				}
//			}
//			
//			return diffIsOk;
//		}
//		
//		private boolean isColumnValueEqual(Object source, Object target)
//		{
//			if (source == null && target == null)
//				return true;
//			
//			if (source != null && source.equals(target))
//				return true;
//			
//			return false;
//		}
//	}



	private DiffTableModel _diffTableModel;
	public boolean hasDiffTableMode()
	{
		return _diffTableModel != null;
	}
	public TableModel getDiffTableMode()
	{
		return _diffTableModel;
	}

	private DbxConnection  _leftConn;
	private ConnectionProp _leftConnProps;
	public void setConnection(DbxConnection conn)
	{
		_leftConn      = conn;
		_leftConnProps = conn.getConnProp();
	}
}
