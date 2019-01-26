package com.asetune.sql.pipe;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.gui.ConnectionProgressCallback;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.ui.autocomplete.SqlObjectName;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class PipeCommandBcp
extends PipeCommandAbstract
{
	private static Logger _logger = Logger.getLogger(PipeCommandBcp.class);

	private String[] _args = null;

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
		String  _db        = null; // extracted from _destTable
		String  _table     = null; // [[dbname.]owner.]tablename
		String  _user      = null;
		String  _passwd    = null;
		String  _server    = null;
		String  _url       = null;
		String  _driver    = null; // not yet used
//		String  _host      = null;
//		String  _port      = null;
		int     _batchSize = 0;
		boolean _slowBcp   = false;
		boolean _createTab = false;
		String  _initStr   = null;
		boolean _truncate  = false;
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
	
	public PipeCommandBcp(String input, String sqlString)
	throws PipeCommandException
	{
		super(input, sqlString);
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
			String params = input.substring(input.indexOf(' ') + 1).trim();

			_args = StringUtil.translateCommandline(params);
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
					if (cmdLine.hasOption('U')) _params._user          = cmdLine.getOptionValue('U');
					if (cmdLine.hasOption('P')) _params._passwd        = cmdLine.getOptionValue('P');
					if (cmdLine.hasOption('S')) _params._server        = cmdLine.getOptionValue('S');
					if (cmdLine.hasOption('u')) _params._url           = cmdLine.getOptionValue('u');
					if (cmdLine.hasOption('D')) _params._db            = cmdLine.getOptionValue('D');
					if (cmdLine.hasOption('b')) try{_params._batchSize = Integer.parseInt(cmdLine.getOptionValue('b'));} catch (NumberFormatException e) {}
					if (cmdLine.hasOption('s')) _params._slowBcp       = true;
					if (cmdLine.hasOption('c')) _params._createTab     = true;
					if (cmdLine.hasOption('i')) _params._initStr       = cmdLine.getOptionValue('i');
					if (cmdLine.hasOption('t')) _params._truncate      = true;

					if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
					{
						String table = cmdLine.getArgList().get(0).toString();
						_params._table = table;
						
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
					}
					System.out.println("BCP Param: _destDb        = '"+ _params._db        + "'.");
					System.out.println("BCP Param: _destTable     = '"+ _params._table     + "'.");
					System.out.println("BCP Param: _destUser      = '"+ _params._user      + "'.");
					System.out.println("BCP Param: _destPasswd    = '"+ _params._passwd    + "'.");
					System.out.println("BCP Param: _destServer    = '"+ _params._server    + "'.");
					System.out.println("BCP Param: _destUrl       = '"+ _params._url       + "'.");
					System.out.println("BCP Param: _destBatchSize = '"+ _params._batchSize + "'.");
					System.out.println("BCP Param: _slowBcp       = '"+ _params._slowBcp   + "'.");
					System.out.println("BCP Param: _createTab     = '"+ _params._createTab + "'.");
					System.out.println("BCP Param: _initStr       = '"+ _params._initStr   + "'.");
					System.out.println("BCP Param: _truncate      = '"+ _params._truncate  + "'.");
				}
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
	
	@Override
	public void doEndPoint(Object input, SqlProgressDialog progress) 
	throws Exception 
	{
		if ( ! (input instanceof ResultSet) )
			throw new Exception("Expected ResultSet as input parameter");
		TransferTable tt = new TransferTable(_params, progress);

		tt.open();
		tt.doTransfer( (ResultSet) input, this );
		tt.close();
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
		options.addOption( "U", "user",          true,  "Username when connecting to server." );
		options.addOption( "P", "passwd",        true,  "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",        true,  "Server to connect to (SERVERNAME|host:port)." );
		options.addOption( "u", "url",           true,  "Destination DB URL (if not ASE and -S)" );
		options.addOption( "D", "dbname",        true,  "Database name in server." );
		options.addOption( "b", "batchSize",     true,  "Batch size" );
		options.addOption( "s", "slowBcp",       false, "Do not set ENABLE_BULK_LOAD when connecting to ASE" );
		options.addOption( "c", "crTable",       false, "Create table if one doesn't exist." );
		options.addOption( "i", "initStr",       true,  "used to do various settings in destination server." );
		options.addOption( "t", "truncateTable", false, "Truncate table before insert." );

		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new PosixParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
//			{
//				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
//				printHelp(options, error);
//			}
			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
			{
				String error = "Missing tablename";
				printHelp(options, error);
			}
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

		sb.append("usage: bcp [[dbname.]owner.]tablename [-U user] [-P passwd] [-S servername|host:port] [-D dbname] [-u url] [-b batchSize] [-s] [-i initStr]\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -U,--user <user>          Username when connecting to server. \n");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd \n");
		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port). \n");
		sb.append("  -D,--dbname <dbname>      Database name in server. \n");
		sb.append("  -u,--url <dest_db_url>    Destination DB URL, if it's not an ASE as destination.\n");
		sb.append("  -b,--batchSize <num>      Batch size. \n");
		sb.append("  -s,--slowBcp              Do not set ENABLE_BULK_LOAD when connecting to ASE.\n");
		sb.append("  -c,--crTable              If table doesn't exist, create a new (based on the ResultSet). -NOT-YET-IMPLEMENTED-\n");
		sb.append("  -i,--initStr <sql stmnt>  Used to do various settings in destination server.\n");
		sb.append("  -t,--truncateTable        truncate table before inserting values.\n");
		sb.append("  \n");
		sb.append("  Note 1: -D,--dbname and -s,--slowBcp is only used if you connects to ASE via the -S,--server switch\n");
		sb.append("  Note 2: if you connect via -u,--url then ordinary JDBC Batch execution will be used.\n");
		sb.append("  \n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private static class TransferTable
	{
		private Connection _conn      = null;
		private CmdParams  _cmdParams = null;
		private SqlProgressDialog _progressDialog = null;
		private String _qic = "\""; // Quoted Identifier Char 

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
			if (StringUtil.hasValue(_cmdParams._server))
			{
    			Properties props = new Properties();
    			
    			if (_cmdParams._slowBcp )
    			{
    				boolean slowBcpDoDynamicPrepare = Configuration.getCombinedConfiguration().getBooleanProperty("PipeCommandBcp.slowBcp.DYNAMIC_PREPARE", true);
    				if (slowBcpDoDynamicPrepare)
    				{
    					_logger.info("Slow BCP has been enabled. Instead of jConnect URL option 'ENABLE_BULK_LOAD=true', lets use 'DYNAMIC_PREPARE=true' (note this can be disabled with property 'PipeCommandBcp.slowBcp.DYNAMIC_PREPARE=false')");
    					props.setProperty("DYNAMIC_PREPARE", "true");
    				}
    			}
    			else
    			{
    				props.setProperty("ENABLE_BULK_LOAD", "true");
    			}

    			String hostPortStr = null;
    			if ( _cmdParams._server.contains(":") )
    				hostPortStr = _cmdParams._server;
    			else
    				hostPortStr = AseConnectionFactory.getIHostPortStr(_cmdParams._server);
    			
    			if (StringUtil.isNullOrBlank(hostPortStr))
    				throw new Exception("Can't find server name information about '"+_cmdParams._server+"', hostPortStr=null. Please try with -S hostname:port");

    			_conn = AseConnectionFactory.getConnection(hostPortStr, _cmdParams._db, _cmdParams._user, _cmdParams._passwd, "sqlw-bcp", Version.getVersionStr(), null, props, (ConnectionProgressCallback)null);

    			if ( ! StringUtil.isNullOrBlank(_cmdParams._db) )
    				AseConnectionUtils.useDbname(_conn, _cmdParams._db);
System.out.println("getDbname(): '" + AseConnectionUtils.getDbname(_conn) + "'");
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
		
				_logger.debug("Try getConnection to driver='"+_cmdParams._driver+"', url='"+_cmdParams._url+"', user='"+_cmdParams._user+"'.");
				_conn = DriverManager.getConnection(_cmdParams._url, props);
			}

			// Print out some destination information
			try
			{
				DatabaseMetaData dbmd = _conn.getMetaData();
				try {_qic = dbmd.getIdentifierQuoteString(); } catch (SQLException ignore) {}
				try {_logger.info("BCP: Connected using driver name '"          + dbmd.getDriverName()             +"'."); } catch (SQLException ignore) {}
				try {_logger.info("BCP: Connected using driver version '"       + dbmd.getDriverVersion()          +"'."); } catch (SQLException ignore) {}
				try {_logger.info("BCP: Connected to destination DBMS Vendor '" + dbmd.getDatabaseProductName()    +"'."); } catch (SQLException ignore) {}
				try {_logger.info("BCP: Connected to destination DBMS Version '"+ dbmd.getDatabaseProductVersion() +"'."); } catch (SQLException ignore) {}
			}
			catch (SQLException ignore) {}

			// Execute the SQL InitString
			if (StringUtil.hasValue(_cmdParams._initStr))
			{
				_logger.info("BCP: executing initialization SQL Stement '"+_cmdParams._initStr+"'.");
				Statement stmnt = _conn.createStatement();
				stmnt.executeUpdate(_cmdParams._initStr);
				stmnt.close();
			}

			// Execute truncate table
			if (_cmdParams._truncate)
			{
				String sql = "";
				// - First try to do:  truncate table ... 
				// - if that fails do: delete from table
				try ( Statement stmnt = _conn.createStatement() )
				{
					sql = "truncate table " + _qic + _cmdParams._table + _qic;
					
					stmnt.executeUpdate(sql);
					_logger.info("BCP: Truncated destination table, using SQL Stement '"+sql+"'.");
				}
				catch(SQLException e)
				{
					_logger.info("Problems with '"+sql+"', trying a normal 'DELETE FROM ...'. Caught: Err="+e.getErrorCode()+", State='"+e.getSQLState()+"', msg='"+e.getMessage()+"'.");
					sql = "DELETE FROM " + _qic + _cmdParams._table + _qic;

					try ( Statement stmnt = _conn.createStatement() )
					{
						stmnt.executeUpdate(sql);
						_logger.info("BCP: Truncated destination table, using SQL Stement '"+sql+"'.");
					}
				}
			}
		}

		public void close()
		throws Exception
		{
			_conn.close();
		}

		public int doTransfer(ResultSet sourceRs, PipeCommandBcp pipeCmd)
		throws Exception
		{
			int sourceNumCols = -1;
			int destNumCols   = -1;
			
			// get RSMD from SOURCE
			ArrayList<String>  sourceColNames   = new ArrayList<String>();
			ArrayList<Integer> sourceSqlTypeInt = new ArrayList<Integer>();
			ResultSetMetaData  sourceRsmd       = sourceRs.getMetaData();

			sourceNumCols = sourceRsmd.getColumnCount();
			for(int c=1; c<sourceNumCols+1; c++)
			{
				sourceColNames  .add(sourceRsmd.getColumnLabel(c));
				sourceSqlTypeInt.add(sourceRsmd.getColumnType(c));
			}

			// Check if the table exists in the destination database
			// If it doesn't exist, try to create it
			if (_cmdParams._createTab)
			{
				SqlObjectName sqlObj = new SqlObjectName(_cmdParams._table, null, null, false);
				DatabaseMetaData md = _conn.getMetaData();
				ResultSet rs = md.getTables(sqlObj.getCatalogNameN(), sqlObj.getSchemaNameN(), sqlObj.getObjectNameN(), null);
				int count = 0;
				while (rs.next())
					count++;
				rs.close();

				System.out.println("Result from: RSMD.getTables(); count="+count);
				if (count == 0)
				{
					// Create a SQL statement like: create table XXX (yyy datatype null/not_null)
					// FIXME
					System.out.println("-NOT-YET-IMPLEMENTED-: Create the destination table.");
				}
			}
			
			// Do dummy SQL to get RSMD from DEST
			String destSql    = "select * from " + _qic + _cmdParams._table + _qic + " where 1=2";
			_logger.info("Investigating destination table, executing SQL Statement: "+destSql);
			if (_progressDialog != null)
				_progressDialog.setState("Checking dest table, SQL: "+destSql);

			Statement destStmt = _conn.createStatement();
			ResultSet destRs = destStmt.executeQuery(destSql);

			// get RSMD from DEST
			ArrayList<String>  destColNames   = new ArrayList<String>();
//			ArrayList<String>  destColType    = new ArrayList<String>();
//			ArrayList<String>  destSqlTypeStr = new ArrayList<String>();
			ArrayList<Integer> destSqlTypeInt = new ArrayList<Integer>();
			ResultSetMetaData  destRsmd       = destRs.getMetaData();

			destNumCols = destRsmd.getColumnCount();
			for(int c=1; c<destNumCols+1; c++)
			{
				destColNames  .add(destRsmd.getColumnLabel(c));
//				destColType   .add(destRsmd.getColumnType(c));
//				destSqlTypeStr.add(destRsmd.getColumnLabel(c));
				destSqlTypeInt.add(destRsmd.getColumnType(c));
			}
			while (destRs.next())
			{
			}
			destRs.close();
			destStmt.close();
			
			// Check if "transfer" will work
			if (sourceNumCols != destNumCols)
			{
				// TODO: should we close the sourceRs or not????
				throw new Exception("Source ResultSet and Destination Table does not have the same column count (source="+sourceNumCols+", dest="+destNumCols+").");
			}
			
			// Make warning if source/destination data types does NOT match
			for (int c=0; c<sourceNumCols; c++)
			{
				int sourceType = sourceSqlTypeInt.get(c);
				int destType   = destSqlTypeInt  .get(c);
				
				if (sourceType != destType)
				{
					String sourceJdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(sourceType);
					String destJdbcTypeStr   = ResultSetTableModel.getColumnJavaSqlTypeName(destType);

					String sourceColName = sourceColNames.get(c);
					String destColName   = destColNames  .get(c);

					String warning = "BCP: Possible column datatype missmatch for column "+(c+1)+". Source column name '"+sourceColName+"', jdbcType '"+sourceJdbcTypeStr+"'. Destination column name '"+destColName+"', jdbcType '"+destJdbcTypeStr+"'. I will still try to do the transfer, hopefully the destination server can/will convert the datatype, so it will work... lets try!"; 
					_logger.warn(warning);
					
					if (pipeCmd._sqlWarnings == null)
						pipeCmd._sqlWarnings = new SQLWarning("Some problems where found during the BCP Operation.");
					pipeCmd._sqlWarnings.setNextWarning(new SQLWarning(warning));
				}
			}
			
			// Build colStr: (col1, col2, col3...)
			String columnStr = " (";
//			for (String colName : sourceColNames)
			for (String colName : destColNames)
				columnStr += colName + ", ";
			columnStr = columnStr.substring(0, columnStr.length()-2);
			columnStr += ")";
	
			// Build: values(?, ?, ?, ?...)
			String valuesStr = " values(";
			for (int i=0; i<destNumCols; i++)
				valuesStr += "?, ";
			valuesStr = valuesStr.substring(0, valuesStr.length()-2);
			valuesStr += ")";
			
			// Build insert SQL
			String insertSql = "insert into " + _qic + _cmdParams._table + _qic + columnStr + valuesStr;
System.out.println("INSERT SQL: "+insertSql);
			_logger.info("BCP INSERT SQL Statement: "+insertSql);

			// Create the Prepared Statement
			PreparedStatement pstmt = _conn.prepareStatement(insertSql);
			
			int totalCount = 0;
			int batchCount = 0;

//			Object[] oa = new Object[sourceNumCols];
			while (sourceRs.next())
			{
				batchCount++;
				totalCount++;

				pipeCmd._rowsSelected++;

				// for each column in source set it to the output
				for (int c=1; c<sourceNumCols+1; c++)
				{
					try
					{
						Object obj = sourceRs.getObject(c);
//						if (obj == null) System.out.println("DATA for column c="+c+", sourceName='"+sourceColNames.get(c-1)+"'. is NULL: sourceRs.getObject(c)");
						if (obj != null)
							pstmt.setObject(c, obj, destSqlTypeInt.get(c-1));
						else
							pstmt.setNull(c, destSqlTypeInt.get(c-1));
					}
					catch (SQLException sqle)
					{
System.out.println("ROW: "+totalCount+" - Problems setting column c="+c+", sourceName='"+sourceColNames.get(c-1)+"', destName='"+destColNames.get(c-1)+"'. Caught: "+sqle);
						throw sqle;
					}
				}

				pstmt.addBatch();
				pipeCmd._rowsInserted++;

				if (_cmdParams._batchSize > 0 && batchCount >= _cmdParams._batchSize )
				{
System.out.println("BATCH SIZE: Executing batch: _batchSize="+_cmdParams._batchSize+", batchCount="+batchCount+", totalCount="+totalCount);
					if (_progressDialog != null)
						_progressDialog.setState("Executing batch insert, at row count "+totalCount);

					batchCount = 0;
					
					pstmt.executeBatch();
				}
				else
				{
					if (_progressDialog != null && ((totalCount % 100) == 0) )
						_progressDialog.setState("Adding row "+totalCount+" to the transfer.");
				}
			}
System.out.println("END OF TRANSFER: Executing batch: _batchSize="+_cmdParams._batchSize+", batchCount="+batchCount+", totalCount="+totalCount);
            if (_progressDialog != null)
            	_progressDialog.setState("Executing final batch insert, at row count "+totalCount);

            pstmt.executeBatch();
			pstmt.close();

//			sourceRs.close();
			
//			if (pipeCmd._sqlWarnings != null)
//				throw pipeCmd._sqlWarnings;

			return totalCount;
		}
	}
}
