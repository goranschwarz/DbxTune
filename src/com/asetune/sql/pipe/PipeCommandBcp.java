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
package com.asetune.sql.pipe;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.ConnectionProfile;
import com.asetune.gui.ConnectionProfileManager;
import com.asetune.gui.ConnectionProgressCallback;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.ResultSetMetaDataCached;
import com.asetune.sql.SqlObjectName;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.ddl.DataTypeNotResolvedException;
import com.asetune.sql.ddl.IDbmsDdlResolver;
import com.asetune.sql.ddl.model.ForeignKey;
import com.asetune.sql.ddl.model.Index;
import com.asetune.sql.ddl.model.Table;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class PipeCommandBcp
extends PipeCommandAbstract
{
	private static Logger _logger = Logger.getLogger(PipeCommandBcp.class);

	private String[] _args = null;

	// If we are transferring data to a Sybase ASE or a Microsoft SQL-Server that has charset UTF8 
	// Then some chars will use more than 1 byte for storage.
	// And some vendors (Sybase ASE[with UTF8] & SQL-Server[with UTF8]) the char/varchar is in BYTES and NOT number of chars
	// While some other vendors still has Number of chars even for chars that are OUTSIDE the 127 bit limit.
	public enum UTF8_destMode
	{
		NONE,  // Do not care to check for UTF-8 chars towards destination 
		CHECK, // Check and warn about UTF-8 String that are longer than the destination allows (if the destination stores bytes in the UTF8 char/varchar columns
		TRUNC  // Truncate UTF-8 String towards the destination, using the same/max length as the destination storage would accept. 
	};
	
	
	private static class CmdParams
	{
		//-----------------------
		// PARAMS for OUT
		//-----------------------
		@SuppressWarnings("unused")
		String _outFilename = null;

		//-----------------------
		// PARAMS for TO SERVER
		//-----------------------
		String  _profile            = null; 
		String  _db                 = null; 
		String  _table              = null; // [owner.]tablename
		String  _user               = null;
		String  _passwd             = null;
		String  _server             = null;
		String  _url                = null;
		String  _driver             = null; // not yet used
		int     _batchSize          = 0;
		boolean _slowBcp            = false;
		boolean _dropTab            = false;
		boolean _createTab          = false;
		boolean _createIndex        = false;
		String  _initStr            = null;
		boolean _truncate           = false;
		boolean _useQuotesOnDestTab = false;
		UTF8_destMode _utf8DestCheckTrunc = UTF8_destMode.NONE;
//		boolean _dryRun             = false;
		boolean _dryRun             = true;
		boolean _debug              = false;

		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			String passwd = _debug ? _passwd : "*secret*";

			sb.append(""  ).append("profile           ".trim()).append("=").append(StringUtil.quotify(_profile           ));
			sb.append(""  ).append("db                ".trim()).append("=").append(StringUtil.quotify(_db                ));
			sb.append(", ").append("table             ".trim()).append("=").append(StringUtil.quotify(_table             ));
			sb.append(", ").append("user              ".trim()).append("=").append(StringUtil.quotify(_user              ));
			sb.append(", ").append("passwd            ".trim()).append("=").append(StringUtil.quotify( passwd            ));
			sb.append(", ").append("server            ".trim()).append("=").append(StringUtil.quotify(_server            ));
			sb.append(", ").append("url               ".trim()).append("=").append(StringUtil.quotify(_url               ));
			sb.append(", ").append("driver            ".trim()).append("=").append(StringUtil.quotify(_driver            ));
			sb.append(", ").append("batchSize         ".trim()).append("=").append(StringUtil.quotify(_batchSize         ));
			sb.append(", ").append("slowBcp           ".trim()).append("=").append(StringUtil.quotify(_slowBcp           ));
			sb.append(", ").append("dropTab           ".trim()).append("=").append(StringUtil.quotify(_dropTab           ));
			sb.append(", ").append("createTab         ".trim()).append("=").append(StringUtil.quotify(_createTab         ));
			sb.append(", ").append("createIndex       ".trim()).append("=").append(StringUtil.quotify(_createIndex       ));
			sb.append(", ").append("initStr           ".trim()).append("=").append(StringUtil.quotify(_initStr           ));
			sb.append(", ").append("truncate          ".trim()).append("=").append(StringUtil.quotify(_truncate          ));
			sb.append(", ").append("useQuotesOnDestTab".trim()).append("=").append(StringUtil.quotify(_useQuotesOnDestTab));
			sb.append(", ").append("utf8DestCheckTrunc".trim()).append("=").append(StringUtil.quotify(_utf8DestCheckTrunc.toString()));
			sb.append(", ").append("dryRun            ".trim()).append("=").append(StringUtil.quotify(_dryRun            ));
			sb.append(", ").append("debug             ".trim()).append("=").append(StringUtil.quotify(_debug             ));
			sb.append(".");

			return sb.toString();
		}
	}
	
	private CmdParams _params = null;
	
	//-----------------------
	// Parameter type to getEndPointResult
	//-----------------------
	public static final String rowsSelected = "rowsSelected";
	public static final String rowsInserted = "rowsInserted";
	public static final String sqlWarnings  = "sqlWarnings";


	private int        _rowsSelected  = 0;
	private int        _rowsInserted  = 0;
	private SQLWarning _sqlWarnings   = null;
	
	public PipeCommandBcp(String input, String sqlString, ConnectionProvider connProvider)
	throws PipeCommandException
	{
		super(input, sqlString, connProvider);
		parse(input);
	}

	/**
	 * ============================================================
	 * bcp (could only be done on a ResultSet)
	 * ============================================================
	 * bcp {   out filename [-t field_terminator] [-r row_terminator]
	 *       | [[dbname.]owner.]tablename [-U user] [-P passwd] [-S servername|host:port] [-b] 
	 *     }
	 * ------------------------------------------------------------
	 * 
	 * @param input
	 * @return
	 * @throws PipeCommandException
	 */
	public void parse(String input)
	throws PipeCommandException
	{
		if (input.startsWith("bcp ") || input.equals("bcp"))
		{
//			String params = input.substring(input.indexOf(' ') + 1).trim();

//			_args = StringUtil.translateCommandline(params);
			_args = StringUtil.translateCommandline(input, true);
//			_args = params.split(" ");
//			for (int i = 0; i < _args.length; i++)
//				_args[i] = _args[i].trim();

			if (_args.length > 1)
			{
				_params = new CmdParams();

				if ("out".equals(_args[0])) // OUTPUT FILE
				{
					_params._outFilename = _args[1];
					// FIXEME: take care about switches: [-t field_terminator] [-r row_terminator]
				}
				else // TO OTHER SERVER
				{
					CommandLine cmdLine = parseCmdLine(_args);
					if (cmdLine.hasOption('T')) _params._table         = cmdLine.getOptionValue('T');
					if (cmdLine.hasOption('p')) _params._profile       = cmdLine.getOptionValue('p');
					if (cmdLine.hasOption('U')) _params._user          = cmdLine.getOptionValue('U');
					if (cmdLine.hasOption('P')) _params._passwd        = cmdLine.getOptionValue('P');
					if (cmdLine.hasOption('S')) _params._server        = cmdLine.getOptionValue('S');
					if (cmdLine.hasOption('u')) _params._url           = cmdLine.getOptionValue('u');
					if (cmdLine.hasOption('D')) _params._db            = cmdLine.getOptionValue('D');
					if (cmdLine.hasOption('b')) try{_params._batchSize = Integer.parseInt(cmdLine.getOptionValue('b'));} catch (NumberFormatException e) {}
					if (cmdLine.hasOption('s')) _params._slowBcp       = true;
					if (cmdLine.hasOption('d')) _params._dropTab       = true;
					if (cmdLine.hasOption('c')) _params._createTab     = true;
					if (cmdLine.hasOption('I')) _params._createIndex   = true;
					if (cmdLine.hasOption('i')) _params._initStr       = cmdLine.getOptionValue('i');
					if (cmdLine.hasOption('t')) _params._truncate      = true;
					if (cmdLine.hasOption('q')) _params._useQuotesOnDestTab = true;
//					if (cmdLine.hasOption('8')) _params._utf8DestCheckTrunc = UTF8_destMode.valueOf(cmdLine.getOptionValue('8'));
//					if (cmdLine.hasOption('X')) _params._dryRun        = true;
					if (cmdLine.hasOption('e')) _params._dryRun        = false;
					if (cmdLine.hasOption('x')) _params._debug         = true;
					if (cmdLine.hasOption('8')) 
					{
						String val = cmdLine.getOptionValue('8').toUpperCase();
						try 
						{
							_params._utf8DestCheckTrunc = UTF8_destMode.valueOf(val);
						} 
						catch (IllegalArgumentException e) 
						{
							String msg = "Problems parsing 'utf8Dest' parameter '"+val+"'. known values: "+StringUtil.toCommaStr(UTF8_destMode.values())+".";
							printHelp(null, msg);
						}
					}

//					// get other params
//					if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
//					{
//						String table = cmdLine.getArgList().get(0).toString();
//						_params._table = table;
						
//						SqlObjectName sqlObj = new SqlObjectName(table, DbUtils.DB_PROD_NAME_SYBASE_ASE, null);

//						_params._table = (String) cmdLine.getArgList().get(0);
//						
//						// if 'dbname.objname.tabname', split off 'dbname.'
//						if (StringUtil.charCount(_params._table, '.') > 1)
//						{
//							_params._table    = _params._table.substring(0, _params._table.indexOf('.'));
//							_params._table = _params._table.substring(_params._table.indexOf('.') + 1);
//							if (_params._table.startsWith("."))
//								_params._table = "dbo" + _params._table;
//						}
//					}
				}
				
				checkParsedParameters(_params);

				if (StringUtil.isNullOrBlank(_params._table))
					printHelp(null, "Missing mandatory parameter -T destinationTableName");
				
				if (_params._debug)
					addDebugMessage("CmdLineSwitches: "+_params);
			}
			else
			{
				printHelp(null, "Please specify some parameters.");
			}
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: bcp");
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
	}

//	private DbxConnection  _leftConn;
//	private ConnectionProp _leftConnProps;
//	public void setConnection(DbxConnection conn)
//	{
//		_leftConn      = conn;
//		_leftConnProps = conn.getConnProp();
//	}

	@Override
	public void doEndPoint(Object input, SqlProgressDialog progress) 
	throws Exception 
	{
		try
		{
			if ( ! (input instanceof ResultSet) )
				throw new Exception("Expected ResultSet as input parameter");
			TransferTable tt = new TransferTable(_params, progress);

			tt.open();
			tt.doTransfer( (ResultSet) input, this );
			tt.close();
		}
		catch(RuntimeException ex)
		{
			_logger.error("Caught RuntimeException, which wasn't expected in PipeCommandBcp.doEndPoint(), just printing the exception for easier debugging.", ex);
			throw ex;
		}
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
		
		if (rowsSelected.equals(type))
		{
			return _rowsSelected;
		}
		else if (rowsInserted.equals(type))
		{
			return _rowsInserted;
		}
		else if (sqlWarnings.equals(type))
		{
			return _sqlWarnings;
		}
		else
		{
			throw new IllegalArgumentException("Input argument/type '"+type+"' is unknown. Known types '"+rowsSelected+"', '"+rowsInserted+"'.");
		}
	}

	@Override 
	public String getConfig()
	{
		return null;
	}
	
	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		options.addOption( "T", "table",         true,  "Destination table name." );
		options.addOption( "p", "profile",       true,  "Profile to get '-U -P -S|-u' from." );
		options.addOption( "U", "user",          true,  "Username when connecting to server." );
		options.addOption( "P", "passwd",        true,  "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",        true,  "Server to connect to (SERVERNAME|host:port)." );
		options.addOption( "u", "url",           true,  "Destination DB URL (if not ASE and -S)" );
		options.addOption( "D", "dbname",        true,  "Database name in server." );
		options.addOption( "b", "batchSize",     true,  "Batch size" );
		options.addOption( "s", "slowBcp",       false, "Do not set ENABLE_BULK_LOAD when connecting to ASE" );
		options.addOption( "d", "dropTable",     false, "Drop table before we start." );
		options.addOption( "c", "crTable",       false, "Create table if one doesn't exist." );
		options.addOption( "I", "crIndex",       false, "Create indexes." );
		options.addOption( "i", "initStr",       true,  "used to do various settings in destination server." );
		options.addOption( "t", "truncateTable", false, "Truncate table before insert." );
		options.addOption( "q", "quoteDestTable",false, "Use Quoted Identifier on the Destination Table." );
		options.addOption( "8", "utf8Dest",      true,  "Do UTF-8 Check/trunc on destination Table column data" );
//		options.addOption( "X", "dryRun",        false, "dryRun." );
		options.addOption( "e", "exec",          false, "dryRun = false." );
		options.addOption( "x", "debug",         false, "Debug mode." );

		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
//			{
//				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
//				printHelp(options, error);
//			}
//			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
//			{
//				String error = "Missing tablename";
//				printHelp(options, error);
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

		sb.append("usage: bcp -T [owner.]tablename [-D dbname] [-p profileName] [-U user] [-P passwd] [-S servername|host:port] [-u url] [-b batchSize] [-s] [-d] [-c] [-i initStr] [I] [-t] [-q] [-e] [-x]\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -T,--table <tabname>        Destination Table name to BCP into, in the form [owner.]tablename. \n");
		sb.append("  -p,--profile <name>         Destination Profile to get '-U -P -S|-u' from. \n");
		sb.append("  -U,--user <user>            Destination Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>        Destination Password when connecting to server. null=noPasswd \n");
		sb.append("  -S,--server <server>        Destination Server to connect to (SERVERNAME|host:port). \n");
		sb.append("  -D,--dbname <dbname>        Destination Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>      Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -b,--batchSize <num>        Batch size. \n");
		sb.append("  -s,--slowBcp                Sybase ASE: Do not set ENABLE_BULK_LOAD when connecting\n");
		sb.append("                              SQL-Server: Do not set useBulkCopyForBatchInsert when connecting\n");
		sb.append("  -d,--dropTable              Drop table before... if --crTable is also enabled, it will be re-created\n");
		sb.append("  -c,--crTable                If table doesn't exist, create a new (based on the ResultSet).\n");
		sb.append("  -I,--crIndex                If --crTable and there is only 1 source table, try to grab indexes from source table and create them at destination table.\n");
		sb.append("  -i,--initStr <sql stmnt>    Used to do various settings in destination server.\n");
		sb.append("  -t,--truncateTable          truncate table before inserting values.\n");
		sb.append("  -q,--quoteDestTable         Use Quoted Identifier on the Destination Table.\n");
		sb.append("  -8,--utf8Dest <check|trunc> Do UTF-8 Length Check/Truncate when applying data on destination Table data (for char, varchar columns.\n");
//		sb.append("  -X,--dryRun                 Dry Run -- Do not make any changes, just print what you are about to do.\n");
		sb.append("  -e,--exec                   Turn Dry Run mode OFF and execute...\n");
		sb.append("  -x,--debug                  Debug mode.\n");
		sb.append("  \n");
		sb.append("  Note 1: -D,--dbname and -s,--slowBcp is only used if you connects to ASE via the -S,--server switch\n");
		sb.append("  Note 2: if you connect via -u,--url then ordinary JDBC Batch execution will be used.\n");
		sb.append("  \n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private class TransferTable
	{
		private DbxConnection _destConn  = null;
		private CmdParams     _cmdParams = null;
		private SqlProgressDialog _progressDialog = null;
//		private String _qic = "\""; // Quoted Identifier Char 
		
		// Hold: 'SRVNAME/dbname' used in various info messages.
		private String _destConnInfo = "";

//		int	_numcols;
//
//		private ArrayList<String>            _type        = new ArrayList<String>();
//		private ArrayList<String>            _sqlTypeStr  = new ArrayList<String>();
//		private ArrayList<Integer>           _sqlTypeInt  = new ArrayList<Integer>();
//		private ArrayList<String>            _cols        = new ArrayList<String>();
//		private ArrayList<Integer>           _displaySize = new ArrayList<Integer>();
//		private ArrayList<ArrayList<Object>> _rows        = new ArrayList<ArrayList<Object>>();
//		private String                       _name        = null;

		public TransferTable(CmdParams params, SqlProgressDialog progressDialog)
		{
			_cmdParams = params;
			_progressDialog = progressDialog;
		}
		
		public void open()
		throws Exception
		{
			if (_cmdParams._dryRun)
			{
				addWarningMessage("###############################################################################");
				addWarningMessage("## You are in DRYRUN MODE, NO changes will be made, I'll just print what I'm about to do/execute. ");
				addWarningMessage("## Turn execution mode ON with: --exec ");
				addWarningMessage("###############################################################################");
			}
			else
			{
				addInfoMessage("In EXECUTION mode... Changes in the destination database will be made.");
			}

			if (StringUtil.hasValue(_cmdParams._server))
			{
    			Properties props = new Properties();
    			
    			if (_cmdParams._slowBcp )
    			{
    				// Sybase ASE
    				if (true)
    				{
        				boolean slowBcpDoDynamicPrepare = Configuration.getCombinedConfiguration().getBooleanProperty("PipeCommandBcp.slowBcp.DYNAMIC_PREPARE", true);
        				if (slowBcpDoDynamicPrepare)
        				{
        					String msg = "Slow BCP has been enabled. Instead of jConnect URL option 'ENABLE_BULK_LOAD=true', lets use 'DYNAMIC_PREPARE=true' (note this can be disabled with property 'PipeCommandBcp.slowBcp.DYNAMIC_PREPARE=false')";
        					_logger.info(msg);
       						addInfoMessage(msg);
        					props.setProperty("DYNAMIC_PREPARE", "true");
        				}
    				}
    			}
    			else
    			{
    				// Sybase ASE
    				if (true)
    				{
    					String msg = "Enable jConnection connection property 'ENABLE_BULK_LOAD' when connecting.";
    					_logger.info(msg);
    					addInfoMessage(msg);
    					props.setProperty("ENABLE_BULK_LOAD", "true");
    				}
    				
    				// Microsoft SQL-Server
//    				if (false)
//    				{
//    					String msg = "Enable SQL-Server JDBC connection property 'useBulkCopyForBatchInsert' when connecting.";
//    					_logger.info(msg);
//    					addInfoMessage(msg);
//        				props.setProperty("useBulkCopyForBatchInsert", "true");
//    				}
    			}

    			String hostPortStr = null;
    			if ( _cmdParams._server.contains(":") )
    				hostPortStr = _cmdParams._server;
    			else
    				hostPortStr = AseConnectionFactory.getIHostPortStr(_cmdParams._server);
    			
    			if (StringUtil.isNullOrBlank(hostPortStr))
    				throw new Exception("Can't find server name information about '"+_cmdParams._server+"', hostPortStr=null. Please try with -S hostname:port");

				if (_cmdParams._debug)
					addDebugMessage("Creating connection to ASE: hostPortStr='"+hostPortStr+"', dbname='"+_cmdParams._db+"', user='"+_cmdParams._user+"', applicationName='sqlw-bcp'.");

				_destConn = DbxConnection.createDbxConnection(AseConnectionFactory.getConnection(hostPortStr, _cmdParams._db, _cmdParams._user, _cmdParams._passwd, "sqlw-bcp", Version.getVersionStr(), null, props, (ConnectionProgressCallback)null));

    			if ( ! StringUtil.isNullOrBlank(_cmdParams._db) )
    				AseConnectionUtils.useDbname(_destConn, _cmdParams._db);
			}
			else
			{
//				throw new Exception("-u|--url option has not yet been implemented.");

				if (StringUtil.hasValue(_cmdParams._driver))
				{
					try { Class.forName(_cmdParams._driver).newInstance(); }
					catch (Exception ignore) {}
				}
				Properties props = new Properties();
				props.put("user", _cmdParams._user);
				props.put("password", _cmdParams._passwd);

				// Special options for: Microsoft SQL-Server
				if (StringUtil.hasValue(_cmdParams._url) && _cmdParams._url.startsWith("jdbc:sqlserver:"))
				{
	    			if ( ! _cmdParams._slowBcp )
	    			{
    					String msg = "Enable SQL-Server JDBC connection property 'useBulkCopyForBatchInsert' when connecting.";
    					_logger.info(msg);
    					addInfoMessage(msg);
        				props.setProperty("useBulkCopyForBatchInsert", "true");
	    			}
				}

				String msg = "Try getConnection to driver='"+_cmdParams._driver+"', url='"+_cmdParams._url+"', user='"+_cmdParams._user+"'.";
				if (_cmdParams._debug)
					addDebugMessage(msg);
				_logger.debug(msg);

				_destConn = DbxConnection.createDbxConnection(DriverManager.getConnection(_cmdParams._url, props));
			}

			// Print out some destination information
			try
			{
				DatabaseMetaData dbmd = _destConn.getMetaData();
				String msg;
//				try {_qic = dbmd.getIdentifierQuoteString(); } catch (SQLException ignore) {}
				try { msg = "Connected to DBMS Server Name '"          + _destConn.getDbmsServerName()    +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected to URL '"                       + dbmd.getURL()                    +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected using driver name '"            + dbmd.getDriverName()             +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected using driver version '"         + dbmd.getDriverVersion()          +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected to destination DBMS Vendor '"   + dbmd.getDatabaseProductName()    +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Connected to destination DBMS Version '"  + dbmd.getDatabaseProductVersion() +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				try { msg = "Current Catalog in the destination srv '" + _destConn.getCatalog()           +"'."; _logger.info(msg); addInfoMessage(msg);} catch (SQLException ignore) {}
				
				_destConnInfo = _destConn.getDbmsServerNameNoThrow() + "/" + _destConn.getCatalog();
			}
			catch (SQLException ignore) {}

			// Execute the SQL InitString
			if (StringUtil.hasValue(_cmdParams._initStr))
			{
				// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
				String sql = _destConn.quotifySqlString(_cmdParams._initStr);

				String msg = "executing initialization SQL Stement '"+sql+"'.";
				addInfoMessage(msg);
				_logger.info(msg);

				_destConn.dbExec(_cmdParams._initStr);
			}

			
			// Get info about DESTINATION Table
			SqlObjectName sqlObj = new SqlObjectName(_destConn, _cmdParams._table);

			DatabaseMetaData md = _destConn.getMetaData();
			ResultSet rs = md.getTables(sqlObj.getCatalogNameOriginNull(), sqlObj.getSchemaNameOriginNull(), sqlObj.getObjectNameOriginNull(), null);
			int destinationTabCount = 0;
			while (rs.next())
				destinationTabCount++;
			rs.close();

			if (_logger.isDebugEnabled())
				_logger.debug("open(): Result from: RSMD.getTables(); count="+destinationTabCount);

			// Execute truncate table
			if (_cmdParams._truncate && destinationTabCount >= 1)
			{
				String tabName = sqlObj.getFullNameUnModified();
				if (_cmdParams._useQuotesOnDestTab)
					tabName = sqlObj.getFullNameOriginQuoted();
				
				if (_cmdParams._dryRun)
				{
					addDryRunMessage("exec SQL at destination[" + _destConnInfo + "]: truncate table " + tabName);
				}
				else
				{
					String sql = "";
					// - First try to do:  truncate table ... 
					// - if that fails do: delete from table
					try ( Statement stmnt = _destConn.createStatement() )
					{
						sql = "truncate table " + tabName;
						
						// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
						sql = _destConn.quotifySqlString(sql);
						
						stmnt.executeUpdate(sql);
						
						String msg = "Truncating destination[" + _destConnInfo + "] table, using SQL Stement '"+sql+"'.";
						addInfoMessage(msg);
						_logger.info(msg);
					}
					catch(SQLException e)
					{
						String msg = "Problems with '"+sql+"', at destination[" + _destConnInfo + "], trying a normal 'DELETE FROM ...'. Caught: Err="+e.getErrorCode()+", State='"+e.getSQLState()+"', msg='"+e.getMessage().trim()+"'.";
						addInfoMessage(msg);
						_logger.info(msg);
						sql = "DELETE FROM " + tabName;

						// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
						sql = _destConn.quotifySqlString(sql);

						try ( Statement stmnt = _destConn.createStatement() )
						{
							stmnt.executeUpdate(sql);
							
							msg = "Truncated destination[" + _destConnInfo + "] table, using SQL Stement '"+sql+"'.";
							addInfoMessage(msg);
							_logger.info(msg);
						}
					}
				}
			}

			// Execute DROP TABLE
			if (_cmdParams._dropTab && destinationTabCount >= 1)
			{
				String tabName = sqlObj.getFullNameUnModified();
				if (_cmdParams._useQuotesOnDestTab)
					tabName = sqlObj.getFullNameOriginQuoted();
				
				if (_cmdParams._dryRun)
				{
					addDryRunMessage("exec SQL at destination[" + _destConnInfo + "]: drop table " + tabName);
				}
				else
				{
					String sql = "";
					// - First try to do:  truncate table ... 
					// - if that fails do: delete from table
					try ( Statement stmnt = _destConn.createStatement() )
					{
						sql = "drop table " + tabName;

						// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
						sql = _destConn.quotifySqlString(sql);
						
						stmnt.executeUpdate(sql);
						
						String msg = "Dropping destination[" + _destConnInfo + "] table, using SQL Stement '"+sql+"'.";
						addInfoMessage(msg);
						_logger.info(msg);
					}
					catch(SQLException ex)
					{
						String msg = "Problems with DROP TABLE using SQL '"+sql+"' at destination[" + _destConnInfo + "]. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
						addErrorMessage(msg);
						_logger.error(msg);
						
						// SHOULD WE THROW the exception here ???
					}
				}
			}
		}

		public void close()
		throws Exception
		{
			if (_destConn != null)
			{
				String msg = "Closing connection to destination[" + _destConnInfo + "], URL='" + _destConn.getMetaData().getURL() + "'.";
				addInfoMessage(msg);
				_logger.info(msg);

				_destConn.close();
			}

			if (_cmdParams._dryRun)
			{
				addWarningMessage("###############################################################################");
				addWarningMessage("## NO CHANGES was done... The 'bcp' command was executed in DRYRUN mode, to execute, please add switch: --exec ");
				addWarningMessage("## Turn execution mode ON with: --exec ");
				addWarningMessage("###############################################################################");
			}
		}

		public int doTransfer(ResultSet sourceRs, PipeCommandBcp pipeCmd)
		throws Exception
		{
			// UTF-8 check/truncate, remember MAX length for each affected column in case of UTF-8 length check
			Map<String, Integer> utf8ColumnIssueMap = new LinkedHashMap<>(); 
			int utf8ColumnIssueCount = 0; // Every time we finds/truncates a UTF-8 issue
			int utf8RowIssueCount    = 0; // Every time we finds/truncates a UTF-8 issue, but on a ROW Level instead on per issue

			if (_progressDialog != null)
				_progressDialog.setState("Reading Source ResultSet MetaData, on large ResulSet this may take time...");

			// Get the origin/source ResultSet MetaData
//			String sourceDbmsVendor = getConnectionProvider().getConnection().getDatabaseProductName();
//			ResultSetMetaDataCached sourceRsmdC = new ResultSetMetaDataCached(sourceRs.getMetaData(), sourceDbmsVendor);
			ResultSetMetaDataCached sourceRsmdC = ResultSetMetaDataCached.createNormalizedRsmd(sourceRs);

			// create a "helper" object for the DESTINATION Table
			SqlObjectName destTableObj = new SqlObjectName(_destConn, _cmdParams._table);
			
			// List of indexes to create "near the end"
			List<String> indexListPossible  = new ArrayList<>();
			List<String> indexListDiscarded = new ArrayList<>();
			List<String> foreignKeyOutList  = new ArrayList<>();
			List<String> foreignKeyInList   = new ArrayList<>();

			// List of foreign key(s) to create or print as information "near the end"
			//List<String> forignKeyList = new ArrayList<>();

			// used by DryRun in the destination table DO not exists (and --crTable is specified)
			String dryRunGuessedInsertSql = "";

			//--------------------------------------------------
			// If it doesn't exist, try to create it
			//--------------------------------------------------
			if (_cmdParams._createTab)
			{
				// Check if the DESTINATION table exists in the destination database
				int destTableCount = 0;
				// Note: the begin/end for variable scope
				{
					if (_progressDialog != null)
						_progressDialog.setState("Checking if destination[" + _destConnInfo + "] table '" + destTableObj.getFullName() + "' exists.");

					DatabaseMetaData md = _destConn.getMetaData();
	    			ResultSet rs = md.getTables(destTableObj.getCatalogNameOriginNull(), destTableObj.getSchemaNameOriginNull(), destTableObj.getObjectNameOriginNull(), null);
	    			while (rs.next())
	    				destTableCount++;
	    			rs.close();
				}

				if (_logger.isDebugEnabled())
					_logger.debug("doTransfer(): Result from: RSMD.getTables(); destTableCount="+destTableCount);
				
				if (destTableCount >= 1)
				{
					addInfoMessage("Destination[" + _destConnInfo + "] table '" + destTableObj.getFullName() + "' already exists. Skipping creating table...");
				}
				else
				{
					String crTabSql = null;

					// Transform Source MetaData to TARGET
//					IDbmsDdlResolver dbmsDdlResolver = _destConn.getDbmsDdlResolver();
//					
//					ResultSetMetaDataCached normalizedSourceRsmdC = dbmsDdlResolver.createNormalizedRsmd(sourceRs); 
//					ResultSetMetaDataCached targetRsmdc           = dbmsDdlResolver.transformToTargetDbms(normalizedSourceRsmdC);
					
					// Transform Source MetaData to TARGET
					IDbmsDdlResolver dbmsDdlResolver = _destConn.getDbmsDdlResolver();
					ResultSetMetaDataCached targetRsmdc = dbmsDdlResolver.transformToTargetDbms(sourceRsmdC); // note the 'sourceRsmdC' is already normalized (done at the "top")
					


					// If the ResultSet has ONE source table... get more info about that table
					if (_logger.isDebugEnabled())
						_logger.debug("DEBUG: targetRsmdc.getSchemaTableNames().size()="+targetRsmdc.getSchemaTableNames(true).size()+", targetRsmdc.getSchemaTableNames()="+targetRsmdc.getSchemaTableNames(true));

					if (targetRsmdc.getSchemaTableNames(true).size() == 1)
					{
						DbxConnection tmpSourceConn = null;
						try
						{
							// Create a new connection to the SOURCE DBMS (if we re-use the one from 'sourceRs.getStatement().getConnection()' things will buffer up at the source...
							// if this isn't done the (Sybase) JDBC driver seems to READ FULLY the LEFT/RIGHT side (and cache the rows)... hence a big delay will happen...
							String msg = "Open a new TEMPORARY Connection to the Source DBMS to get MetaData about the Source table... Reusing the current will/might block the system.";
							_logger.info(msg);
							addInfoMessage(msg);

							tmpSourceConn = getConnectionProvider().getNewConnection("sqlw-bcp-srcMetaData");
							
							String sourceCatName = targetRsmdc.getCatalogNames().isEmpty() ? null : targetRsmdc.getCatalogNames().iterator().next(); 
							String sourceSchName = targetRsmdc.getSchemaNames() .isEmpty() ? null : targetRsmdc.getSchemaNames() .iterator().next(); 
							String sourceTabName = targetRsmdc.getTableNames()  .isEmpty() ? null : targetRsmdc.getTableNames()  .iterator().next(); 

							Table sourceTable = Table.create(tmpSourceConn, sourceCatName, sourceSchName, sourceTabName);

							if (_logger.isDebugEnabled())
								_logger.debug("DEBUG: ONE table in source RS. sourceCatName='"+sourceCatName+"', sourceSchName='"+sourceSchName+"', sourceTabName='"+sourceTabName+"', sourceTable.ColNames="+sourceTable.getColumnNames());

							// If ALL Columns in the input ResultSet and we have found the table in the Source DBMS and that it's all of the columns
							// then: get the "create table..." from the source table.
							if (targetRsmdc.getColumnNames().equals(sourceTable.getColumnNames()))
							{
								try
								{
									crTabSql = dbmsDdlResolver.ddlText(sourceTable);
								}
								catch(DataTypeNotResolvedException ex)
								{
									msg = "Problems when Reverse Engineer 'Create Table' DDL from the Source table (some data type could not be resolved). Skipping this and continuing with DDL Reverse enginering from the Source ResultSet. Caught: " + ex;
									_logger.warn(msg, ex);
									addWarningMessage(msg);
								}
							}
							
							// Loop available indexes in the SOURCE Table
							for (Index index : sourceTable.getIndexes())
							{
								// Set the DESTINATIONS table in the Index entry before we create the DDL
								String indexDdl = dbmsDdlResolver.ddlText(index, true, destTableObj.getSchemaNameNull(), destTableObj.getObjectNameNull()).trim();
								
								if (_logger.isDebugEnabled())
									_logger.debug("DEBUG: Checking index='"+index.getIndexName()+"': with ColNames: "+index.getColumnNames()+", with sourceRsmdC.getColumnNames(): "+sourceRsmdC.getColumnNames());
								
								// if ALL index-columns is part of the input-ResultSetMetaData... then we can add the index to the "create list"
								// else add it to the "discard list" 
								if ( sourceRsmdC.getColumnNames().containsAll( index.getColumnNames() ) )
								{
									indexListPossible.add( indexDdl );

									if (_logger.isDebugEnabled())
										_logger.debug("DEBUG:      +++++++ ADDED index ddl: "+indexDdl);
								}
								else
								{
									indexListDiscarded.add( indexDdl );

									if (_logger.isDebugEnabled())
										_logger.debug("DEBUG:      ------- DISCARDED index ddl: "+indexDdl);
								}
							}
							
							// Loop ForeignKey(s)
							for (ForeignKey fk : sourceTable.getForeignKeysOut())
							{
								String fkDdl = dbmsDdlResolver.ddlTextAlterTable(fk);

								foreignKeyOutList.add(fkDdl);
							}
							for (ForeignKey fk : sourceTable.getForeignKeysIn())
							{
								String fkDdl = dbmsDdlResolver.ddlTextAlterTable(fk);

								foreignKeyInList.add(fkDdl);
							}
						}
						catch (SQLException ex)
						{
							String msg = "Problems when looking up source table. Skipping this and continuing with DDL Reverse enginering from the Source ResultSet. Also no indexes etc will be reversed engineered. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
							_logger.warn(msg);
							addWarningMessage(msg);

							if (tmpSourceConn != null)
							{
								// Now the connection can be closed again...
								msg = "Closing the TEMPORARY Connection to the Source DBMS.";
								_logger.info(msg);
								addInfoMessage(msg);

								// Close the connection
								tmpSourceConn.closeNoThrow();
							}
						}
					}

					// in no DDL for create table, create it using the ResultSet MetaData DDL Resolver for that DBMS Target
					if (crTabSql == null)
					{
						crTabSql = dbmsDdlResolver.ddlTextTable(targetRsmdc, destTableObj.getSchemaNameOriginNull(), destTableObj.getObjectNameOriginNull());
						crTabSql = crTabSql.trim();
					}

					// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
					crTabSql = _destConn.quotifySqlString(crTabSql);

					if (_cmdParams._dryRun)
					{
						addDryRunMessage("Destination[" + _destConnInfo + "] table '" + destTableObj.getFullName() + "' will be created, using the following SQL: \n" + crTabSql);

						String questionMarks = StringUtil.removeLastComma(StringUtil.replicate("?, ", targetRsmdc.getColumnNames().size()));
						dryRunGuessedInsertSql = "INSERT INTO " + destTableObj.getFullName() + " (" + StringUtil.toCommaStr(targetRsmdc.getColumnNames()) + ") values(" + questionMarks + ")";
					}
					else
					{
						addInfoMessage("Destination[" + _destConnInfo + "] table '" + destTableObj.getFullName() + "' will be created, using the following SQL: \n" + crTabSql);

						try (Statement stmnt = _destConn.createStatement())
						{
							stmnt.executeUpdate(crTabSql);
							
							addInfoMessage("CREATED Destination[" + _destConnInfo + "] table '" + destTableObj.getFullName() + "'.");
						}
					}
				} // end: 
			} // end: _createTab


			// Execute DUMMY SQL in Destination to get table/column definition (ResultSet MetaData)
			String tabName = destTableObj.getFullNameUnModified();
			if (_cmdParams._useQuotesOnDestTab)
				tabName = destTableObj.getFullNameOriginQuoted();

			// SQL statement to execute at destination
			String destSql    = "select * from " + tabName + " where 1=2";

			// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
			destSql = _destConn.quotifySqlString(destSql);

			ResultSetMetaDataCached destRsmdC = null;
			try
			{
				String msg = "Investigating destination[" + _destConnInfo + "] table, executing SQL Statement: "+destSql;
				addInfoMessage(msg);
				_logger.info(msg);
				if (_progressDialog != null)
					_progressDialog.setState("Checking dest table, SQL: "+destSql);

				Statement destStmt = _destConn.createStatement();
				ResultSet destRs = destStmt.executeQuery(destSql);

				// get RSMD from DEST
				destRsmdC = new ResultSetMetaDataCached(destRs).createNormalizedRsmd();

				while (destRs.next()) {}
				destRs.close();
				destStmt.close();
			}
			// if the table didn't exist, we will probably end up here in the catch block
			catch(SQLException ex) 
			{
				String msg = "Problems Investigating destination[" + _destConnInfo + "] using SQL='" + destSql + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
				addErrorMessage(msg);
				_logger.error(msg);

				// Print some extra information when in DRY-RUN
				if (_cmdParams._dryRun)
				{
					addDryRunMessage("(guessing a bit here, since table don't exists) INSERT SQL PreparedStatement at Destination[" + _destConnInfo + "]: " + dryRunGuessedInsertSql);

					// info about: Create any index found in the Source table
					if ( ! indexListPossible.isEmpty() )
					{
						for (String ddl : indexListPossible)
						{
							if (_cmdParams._createIndex)
							{
								addDryRunMessage("exec SQL at destination[" + _destConnInfo + "]: " + ddl);
							}
							else
							{
								addDryRunMessage("SUGGESTION_TODO_AT_DESTINATION[" + _destConnInfo + "]: Create INDEX: " + ddl);
							}
						}
					}
					
					if ( ! indexListDiscarded.isEmpty() )
					{
						for (String ddl : indexListDiscarded)
							addDryRunMessage("DISCARDED INDEX: Source Table HAS this index, while destination[" + _destConnInfo + "] are missing some columns: DISCARDED INDEX: " + ddl);
					}

					// Foreign Keys
					if ( ! foreignKeyOutList.isEmpty() )
					{
						for (String ddl : foreignKeyOutList)
							addDryRunMessage("SUGGESTION_TODO_AT_DESTINATION[" + _destConnInfo + "]: Create ForeignKey: " + ddl);
					}
					if ( ! foreignKeyInList.isEmpty() )
					{
						for (String ddl : foreignKeyInList)
							addDryRunMessage("INFO at Source: (-->> incoming fk) Other tables points to this table using: " + ddl);
					}
					
					msg = "NOTE: you may do 'remote SQL execution' with command: \\rsql --profile 'profileName' --dbname dbname --sql 'sql-command-to-execute'";
					addInfoMessage(msg);

					// exec sp_spaceused 'tabname'
					if (_destConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE))
					{
						String destTableName = destTableObj.getFullNameUnModified();
						addDryRunMessage("checking destination[" + _destConnInfo + "]: table size with: sp_spaceused '" + destTableName + "'");
					}
				}
				
				//------------------------------------------------
				// GET OUT OF HERE
				//------------------------------------------------
				return 0;
			} // end: catch
			

			int sourceNumCols = sourceRsmdC.getColumnCount();
			int destNumCols   = destRsmdC  .getColumnCount();

			// Check if "transfer" will work
			if (sourceNumCols != destNumCols)
			{
				String msg = "Source ResultSet and Destination Table does not have the same column count (source="+sourceNumCols+", dest="+destNumCols+").";
				addErrorMessage(msg);
				_logger.error(msg);

				return 0;
				// TODO: should we close the sourceRs or not????
				//throw new Exception("Source ResultSet and Destination Table does not have the same column count (source="+sourceNumCols+", dest="+destNumCols+").");
			}
			
			// Check DataTypes -- Make warning if source/destination data types does NOT match
			for (int c=0; c<sourceNumCols; c++)
			{
				int sqlPos = c + 1;
				
				int sourceType = sourceRsmdC.getColumnType(sqlPos);
				int destType   = destRsmdC  .getColumnType(sqlPos);
				
				if (sourceType != destType)
				{
					String sourceJdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(sourceType);
					String destJdbcTypeStr   = ResultSetTableModel.getColumnJavaSqlTypeName(destType);

					String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
					String destColName   = destRsmdC  .getColumnLabel(sqlPos);

					String warning = "Possible column datatype missmatch for column " + sqlPos + ". "
							+ "Source column name '" + sourceColName + "', jdbcType '" + sourceJdbcTypeStr + "'. "
							+ "Destination column name '"+destColName+"', jdbcType '"+destJdbcTypeStr+"'. "
							+ "I will still try to do the transfer, hopefully the destination server can/will convert the datatype, so it will work... lets try!"; 
					addWarningMessage(warning);
					_logger.warn(warning);
					
					if (pipeCmd._sqlWarnings == null)
						pipeCmd._sqlWarnings = new SQLWarning("Some problems where found during the BCP Operation.");
					pipeCmd._sqlWarnings.setNextWarning(new SQLWarning(warning));
				}
			}
			
			// Build colStr: (col1, col2, col3...)
//			String columnStr = " (" + StringUtil.toCommaStr(destRsmdC.getColumnNames()) + ")";
			String columnStr = " (" + StringUtil.toCommaStrQuoted('[', ']', destRsmdC.getColumnNames()) + ")";
	
			// Build: values(?, ?, ?, ?...)
			String valuesStr = " values(" + StringUtil.removeLastComma(StringUtil.replicate("?, ", destNumCols)) + ")";
			
			// Build insert SQL
			String intoTabName = destTableObj.getFullNameUnModified();
			if (_cmdParams._useQuotesOnDestTab)
				intoTabName = destTableObj.getFullNameOriginQuoted();
			
			int totalCount = 0;
			int batchCount = 0;

			String insertSql = "insert into " + intoTabName + columnStr + valuesStr;

			// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
			insertSql = _destConn.quotifySqlString(insertSql);

			if (_cmdParams._dryRun)
			{
				addDryRunMessage("exec SQL at destination[" + _destConnInfo + "] (Prepared Statement for all rows in source): "+insertSql);
			}
			else
			{
				if (_logger.isDebugEnabled())
					_logger.debug("INSERT SQL: "+insertSql);

				String msg = "INSERT at destination[" + _destConnInfo + "] SQL Statement: "+insertSql;
				addInfoMessage(msg);
				_logger.info(msg);
				
				// Create the Prepared Statement
				PreparedStatement pstmt = _destConn.prepareStatement(insertSql);

				// Loop the SOURCE ResultSet and: setObject(), addBatch(), executeBatch()
				while (sourceRs.next())
				{
					batchCount++;
					totalCount++;

					pipeCmd._rowsSelected++;

					// for each column in source set it to the output
					for (int sqlPos=1; sqlPos<sourceNumCols+1; sqlPos++)
					{
						int colJdbcDataType = destRsmdC.getColumnType(sqlPos);//destSqlTypeInt.get(c-1);
						try
						{
							Object obj = sourceRs.getObject(sqlPos);
//							if (obj == null) System.out.println("DATA for column c="+c+", sourceName='"+sourceColNames.get(c-1)+"'. is NULL: sourceRs.getObject(c)");
							
							// Check for "source DATA String" is TO LONG IN DESTINATION, due to UTF-8 storage in Sybase ASE and MS SQL-Server
							if ( ! UTF8_destMode.NONE.equals(_cmdParams._utf8DestCheckTrunc) )
							{
								// NOTE: NCHAR, NVARCHAR == Should handle UTF-8, LONGVARCHAR etc should be mapped to CLOB or similar
								//       so that leaves us with: CHAR and VARCHAR
								if (obj instanceof String && (colJdbcDataType == Types.CHAR || colJdbcDataType == Types.VARCHAR))
								{
									String colValue = (String) obj;

									int destColumnMaxLength = destRsmdC.getPrecision(sqlPos); // destSqlLength.get(sqlPos-1);
									int utf8Len             = StringUtil.utf8Length(colValue);

									if (utf8Len > destColumnMaxLength)
									{
										utf8ColumnIssueCount++;
										
										String colName = destRsmdC.getColumnLabel(sqlPos); // destColNames.get(sqlPos-1);
										int    strLen  = colValue.length();

										// Remember MAX length for each column
										Integer colMaxVal = utf8ColumnIssueMap.get(colName);
										utf8ColumnIssueMap.put(colName, (colMaxVal == null) ? utf8Len : Math.max(colMaxVal.intValue(), utf8Len));
											
										
										msg = "The columnName='" + colName + "' at row=" + totalCount + ", has a value to long to be inserted at destination. "
												+ "The destColumnMaxLength=" + destColumnMaxLength + ", Str-Length=" + strLen + ", UTF-8-Length=" + utf8Len
												+". THE UTF-8 LENGTH is above destColumnMaxLength. The String (which containes larger UTF8 chars) value is |" + colValue + "|.";

										if ( ! UTF8_destMode.CHECK.equals(_cmdParams._utf8DestCheckTrunc) )
										{
											addErrorMessage(msg);
											_logger.error(msg);
										}
										else
										{
											String newColValue = StringUtil.utf8Truncate(colValue, destColumnMaxLength);
											int    newStrLen   = newColValue.length();
											
											// Set the new value
											obj = newColValue;

											msg += " The value will be truncated to length=" + newStrLen + ", new value=|" + newColValue + "|.";
											addErrorMessage(msg);
											_logger.warn(msg);
										}
									}
								}
							} // end: UTF-8 check/trunc

							//---------------------------------------
							// SET the data or NULL value
							//---------------------------------------
							if (obj != null)
								pstmt.setObject(sqlPos, obj, colJdbcDataType);
							else
								pstmt.setNull(sqlPos, colJdbcDataType);
						}
						catch (SQLException ex)
						{
							String sourceColName = sourceRsmdC.getColumnLabel(sqlPos);
							String destColName   = destRsmdC  .getColumnLabel(sqlPos);

							msg = "ROW: "+totalCount+" - Problems setting column c="+sqlPos+", sourceName='" + sourceColName + "', destName='" + destColName + "'. Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
							addErrorMessage(msg);
							_logger.error(msg);

							// NOTE: Here we THROW (out of method), should we do something "better"
							throw ex;
						}
					}

					pstmt.addBatch();
					pipeCmd._rowsInserted++;

					if (_cmdParams._batchSize > 0 && batchCount >= _cmdParams._batchSize )
					{
						msg = "BATCH SIZE: Executing batch: _batchSize="+_cmdParams._batchSize+", batchCount="+batchCount+", totalCount="+totalCount;
						if (_cmdParams._debug)
							addDebugMessage(msg);

						if (_progressDialog != null)
							_progressDialog.setState("Executing batch insert, at row count " + NumberFormat.getInstance().format(totalCount));

						batchCount = 0;
						
						pstmt.executeBatch();
					}
					else
					{
						if (_progressDialog != null && ((totalCount % 100) == 0) )
							_progressDialog.setState("Adding row " + NumberFormat.getInstance().format(totalCount) + " to the transfer.");
					}
				}
				msg = "END OF TRANSFER to destination[" + _destConnInfo + "]: Executing batch: _batchSize="+_cmdParams._batchSize+", batchCount="+batchCount+", totalCount="+totalCount;
				addInfoMessage(msg);

				if (_progressDialog != null)
					_progressDialog.setState("Executing final batch insert (which might take a bit longer), at row count " + NumberFormat.getInstance().format(totalCount));

				pstmt.executeBatch();
				pstmt.close();

//				sourceRs.close();
				
//				if (pipeCmd._sqlWarnings != null)
//					throw pipeCmd._sqlWarnings;

				msg = "BCP Transferred "+totalCount+" rows to the destination[" + _destConnInfo + "] table '"+tabName+"'.";
				addInfoMessage(msg);
				_logger.info(msg);
				
				
				// CHeck for UTF-8 overflows
				if ( ! utf8ColumnIssueMap.isEmpty() )
				{
					msg = "Summary of UTF-8 length transfer issues to destination[" + _destConnInfo + "], found " + utf8ColumnIssueMap.size() + " columns with issues, on " + utf8RowIssueCount + " rows, total found values are " + utf8ColumnIssueCount + ".";
					addWarningMessage(msg);
					_logger.warn(msg);

					for (Entry<String, Integer> entry : utf8ColumnIssueMap.entrySet())
					{
						String colName    = entry.getKey();
						int    colMaxSize = entry.getValue();
						
						msg = "   - Column '" + colName + "' max UTF-8 length was: " + colMaxSize + " for destination table '" + destTableObj.getFullName() + "'.";
						addWarningMessage(msg);
						_logger.warn(msg);
					}
				}
			} // end: insert/transfer records
			
			
			// Create any index found in the Source table
			if ( ! indexListPossible.isEmpty() )
			{
				for (String ddl : indexListPossible)
				{
					// Translate '[' and ']' chars into DBMS Vendor specific Quoted Chars
					ddl = _destConn.quotifySqlString(ddl);

					if (_cmdParams._createIndex)
					{
						if (_cmdParams._dryRun)
						{
							addDryRunMessage("exec SQL at destination[" + _destConnInfo + "]: " + ddl);
						}
						else
						{
							try (Statement stmnt = _destConn.createStatement())
							{
								String msg = "Create INDEX at destination[" + _destConnInfo + "], executing: " + ddl;

								if (_progressDialog != null)
									_progressDialog.setState(msg);

								addInfoMessage(msg);
								
								stmnt.executeUpdate(ddl);
							}
							catch(SQLException ex)
							{
								String msg = "FAILED: Create INDEX at destination[" + _destConnInfo + "], executing: " + ddl + ", Caught: Err=" + ex.getErrorCode() + ", State='" + ex.getSQLState() + "', msg='" + ex.getMessage().trim() + "'.";
								addErrorMessage(msg);
							}
						}
					}
					else
					{
						String msg = "SUGGESTION_TODO_AT_DESTINATION[" + _destConnInfo + "]: Create INDEX: " + ddl;

						addInfoMessage(msg);
					}
				}

				if ( ! indexListDiscarded.isEmpty() )
				{
					for (String ddl : indexListDiscarded)
						addInfoMessage("DISCARDED INDEX: Source Table HAS this index, while destination[" + _destConnInfo + "] are missing some columns: DISCARDED INDEX: " + ddl);
				}
				
				String msg = "NOTE: you may do 'remote SQL execution' with command: \\rsql --profile 'profileName' --dbname dbname --sql 'sql-command-to-execute'";
				addInfoMessage(msg);
			} // end: indexes
			
			// Foreign Keys
			if ( ! foreignKeyOutList.isEmpty() )
			{
				for (String ddl : foreignKeyOutList)
					addDryRunMessage("SUGGESTION_TODO_AT_DESTINATION[" + _destConnInfo + "]: Create ForeignKey: " + ddl);
			}
			if ( ! foreignKeyInList.isEmpty() )
			{
				for (String ddl : foreignKeyInList)
					addDryRunMessage("INFO at Source: (-->> incoming fk) Other tables points to this table using: " + ddl);
			}
			
			
			{ // Begin/end just to scope the variable
				String msg = "NOTE: you can CHECK for Table or ResultSet DIFFERENCE between two DBMS Servers with command: 'go | diff' or '\\tabdiff ...'";
				addInfoMessage(msg);
			}
			
			// Do some checks on the table
			// -- ASE do: sp_spaceused
			if (_destConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE))
			{
				String destTableName = destTableObj.getFullNameUnModified();
				String sql = "sp_spaceused '" + destTableName + "'";

				if (_cmdParams._dryRun)
				{
					addDryRunMessage("exec SQL at destination[" + _destConnInfo + "]: " + sql);
				}
				else
				{
					try (Statement stmnt = _destConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql); )
					{
						ResultSetMetaData rsmd = rs.getMetaData();
						while(rs.next())
						{
							Map<String, String> colValRow = new LinkedHashMap<>();
							for (int c=1; c<=rsmd.getColumnCount(); c++)
							{
								// add |LABEL:Value| to the map.
								colValRow.put(rsmd.getColumnLabel(c), rs.getString(c));
							}
							String msg = "Results from sp_spaceused: " + StringUtil.toCommaStr(colValRow);
							addInfoMessage(msg);
						}
					}
				}
			}

			return totalCount;
		} // end: method: doTransfer
		
	} // end: class TransferTable
}
