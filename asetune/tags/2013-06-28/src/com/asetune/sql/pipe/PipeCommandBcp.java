package com.asetune.sql.pipe;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.asetune.gui.ConnectionProgressCallback;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.StringUtil;

public class PipeCommandBcp
extends PipeCommandAbstract
{
	private String[] _args = null;

	//-----------------------
	// PARAMS for OUT
	//-----------------------
	private String _outFilename = null;

	//-----------------------
	// PARAMS for TO SERVER
	//-----------------------
	private String _destDb        = null; // extracted from _destTable
	private String _destTable     = null; // [[dbname.]owner.]tablename
	private String _destUser      = null;
	private String _destPasswd    = null;
	private String _destServer    = null;
//	private String _destHost      = null;
//	private String _destport      = null;
	private int    _destBatchSize = 0;
	
	public PipeCommandBcp(String input)
	throws PipeCommandException
	{
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
		if (input.startsWith("bcp "))
		{
			String params = input.substring(input.indexOf(' ') + 1).trim();
			_args = params.split(" ");
			for (int i = 0; i < _args.length; i++)
				_args[i] = _args[i].trim();

			if (_args.length > 1)
			{
				if ("out".equals(_args[0])) // OUTPUT FILE
				{
					_outFilename = _args[1];
					// FIXEME: take care about switches: [-t field_terminator] [-r row_terminator]
				}
				else // TO OTHER SERVER
				{
//					for (int i=0; i<_args.length; i++)
//					{
//					}
					CommandLine cmdLine = parseCmdLine(_args);
					if (cmdLine.hasOption('U')) _destUser      = cmdLine.getOptionValue('U');
					if (cmdLine.hasOption('P')) _destPasswd    = cmdLine.getOptionValue('P');
					if (cmdLine.hasOption('S')) _destServer    = cmdLine.getOptionValue('S');
					if (cmdLine.hasOption('D')) _destDb        = cmdLine.getOptionValue('D');
					if (cmdLine.hasOption('b')) try{_destBatchSize = Integer.parseInt(cmdLine.getOptionValue('b'));} catch (NumberFormatException e) {}

					if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
					{
						_destTable = (String) cmdLine.getArgList().get(0);
						
						// if 'dbname.objname.tabname', split off 'dbname.'
						if (StringUtil.charCount(_destTable, '.') > 1)
						{
							_destDb    = _destTable.substring(0, _destTable.indexOf('.'));
							_destTable = _destTable.substring(_destTable.indexOf('.') + 1);
							if (_destTable.startsWith("."))
								_destTable = "dbo" + _destTable;
						}
					}
					System.out.println("BCP: _destDb        = '"+ _destDb        + "'.");
					System.out.println("BCP: _destTable     = '"+ _destTable     + "'.");
					System.out.println("BCP: _destUser      = '"+ _destUser      + "'.");
					System.out.println("BCP: _destPasswd    = '"+ _destPasswd    + "'.");
					System.out.println("BCP: _destServer    = '"+ _destServer    + "'.");
					System.out.println("BCP: _destBatchSize = '"+ _destBatchSize + "'.");
				}
			}
			else
			{
				throw new PipeCommandException("PipeCommand, cmd='"+input+"'. Usage: bcp { out filename [-t field_terminator] [-r row_terminator] | [[dbname.]owner.]tablename [-U user] [-P passwd] [-S servername|host:port] [-b] }");
			}
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: bcp");
		}
	}
	
	@Override
	public void doEndPoint(Object input) 
	throws Exception 
	{
		if ( ! (input instanceof ResultSet) )
			throw new Exception("Expected ResultSet as input parameter");
		TransferTable tt = new TransferTable(_destDb, _destTable, _destUser, _destPasswd, _destServer, _destBatchSize);
		
		tt.open();
		tt.doTransfer( (ResultSet) input );
		tt.close();
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
		options.addOption( "U", "user",        true, "Username when connecting to server." );
		options.addOption( "P", "passwd",      true, "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",      true, "Server to connect to (SERVERNAME|host:port)." );
		options.addOption( "D", "dbname",      true, "Database name in server." );
		options.addOption( "b", "batchSize",   true, "Batch size" );

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

		if (errorStr != null)
		{
			sb.append("\n");
			sb.append(errorStr);
			sb.append("\n");
		}

		sb.append("usage: bcp [[dbname.]owner.]tablename [-U user] [-P passwd] [-S servername|host:port] [-b]");
		sb.append("  ");
		sb.append("options:");
		sb.append("  -U,--user <user>          Username when connecting to server.");
		sb.append("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd");
		sb.append("  -S,--server <server>      Server to connect to (SERVERNAME|host:port).");
		sb.append("  -D,--dbname <dbname>      Database name in server.");
		sb.append("  -b,--batchSize <num>      Batch size.");
		sb.append("  ");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private static class TransferTable
	{
		private Connection _conn = null;
		private String _db        = null; // extracted from _destTable
		private String _table     = null; // [[dbname.]owner.]tablename
		private String _user      = null;
		private String _passwd    = null;
		private String _server    = null;
//		private String _host      = null;
//		private String _port      = null;
		private int    _batchSize = 0;
		
//		int	_numcols;
//
//		private ArrayList<String>            _type        = new ArrayList<String>();
//		private ArrayList<String>            _sqlTypeStr  = new ArrayList<String>();
//		private ArrayList<Integer>           _sqlTypeInt  = new ArrayList<Integer>();
//		private ArrayList<String>            _cols        = new ArrayList<String>();
//		private ArrayList<Integer>           _displaySize = new ArrayList<Integer>();
//		private ArrayList<ArrayList<Object>> _rows        = new ArrayList<ArrayList<Object>>();
//		private String                       _name        = null;

		public TransferTable(String db, String table, String user, String passwd, String server, int batchSize)
		{
			_db        = db;
			_table     = table;
			_user      = user;
			_passwd    = passwd;
			_server    = server;
			_batchSize = batchSize;
		}
		
		public void open()
		throws Exception
		{
			Properties props = new Properties();
			props.setProperty("ENABLE_BULK_LOAD", "true");
			String hostPortStr = AseConnectionFactory.getIHostPortStr(_server);
			_conn = AseConnectionFactory.getConnection(hostPortStr, _db, _user, _passwd, "sqlw-bcp", null, props, (ConnectionProgressCallback)null);

			if ( ! StringUtil.isNullOrBlank(_db) )
				AseConnectionUtils.useDbname(_conn, _db);
System.out.println("getDbname(): '" + AseConnectionUtils.getDbname(_conn) + "'");
		}

		public void close()
		throws Exception
		{
			_conn.close();
		}

		public int doTransfer(ResultSet sourceRs)
		throws Exception
		{
			int sourceNumCols = -1;
			int destNumCols   = -1;
			
			// get RSMD from SOURCE
			ArrayList<String> sourceColNames = new ArrayList<String>();
			ArrayList<Integer> sourceSqlTypeInt = new ArrayList<Integer>();
			ResultSetMetaData sourceRsmd = sourceRs.getMetaData();
			sourceNumCols = sourceRsmd.getColumnCount();
			for(int c=1; c<sourceNumCols+1; c++)
			{
				sourceColNames  .add(sourceRsmd.getColumnLabel(c));
				sourceSqlTypeInt.add(sourceRsmd.getColumnType(c));
			}

			// Do dummy SQL to get RSMD from DEST
			String destSql    = "select * from "+_table+" where 1=2";
			Statement destStmt = _conn.createStatement();
			ResultSet destRs = destStmt.executeQuery(destSql);

			// get RSMD from DEST
			ArrayList<String>  destColNames   = new ArrayList<String>();
//			ArrayList<String>  destColType    = new ArrayList<String>();
//			ArrayList<String>  destSqlTypeStr = new ArrayList<String>();
			ArrayList<Integer> destSqlTypeInt = new ArrayList<Integer>();
			ResultSetMetaData destRsmd = destRs.getMetaData();
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
			
			// Build colStr: (col1, col2, col3...)
			String columnStr = " (";
			for (String colName : sourceColNames)
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
			String insertSql = "insert into " + _table + columnStr + valuesStr;
System.out.println("INSERT SQL: "+insertSql);

			// Create the Prepared Statement
			PreparedStatement pstmt = _conn.prepareStatement(insertSql);
			
			int totalCount = 0;
			int batchCount = 0;

//			Object[] oa = new Object[sourceNumCols];
			while (sourceRs.next())
			{
				batchCount++;
				totalCount++;

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

				if (_batchSize > 0 && batchCount >= _batchSize )
				{
System.out.println("BATCH SIZE: Executing batch: _batchSize="+_batchSize+", batchCount="+batchCount+", totalCount="+totalCount);
					batchCount = 0;
					
					pstmt.executeBatch();
				}
			}
System.out.println("END OF TRANSFER: Executing batch: _batchSize="+_batchSize+", batchCount="+batchCount+", totalCount="+totalCount);
			pstmt.executeBatch();
			pstmt.close();

//			sourceRs.close();
			
			return totalCount;
		}
	}
}
