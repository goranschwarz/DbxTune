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
package com.asetune.tools.sqlw;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.asetune.sql.SqlObjectName;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.utils.FileUtils;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

public class SqlStatementCmdLoadFile 
extends SqlStatementAbstract
{
	private static Logger _logger = Logger.getLogger(SqlStatementCmdLoadFile.class);

	public static final String  DEFAULT_NULL_STRING       = "<NULL>";
	public static final String  DEFAULT_FIELD_TERM_STRING = ",";
	public static final String  DEFAULT_ROW_TERM_STRING   = "\\n";

	public static final int     DEFAULT_TRAN_BATCH_SIZE   = 0;
	public static final int     DEFAULT_SEND_BATCH_SIZE   = 1000;
	
	public static final boolean DEFAULT_TRUNCATE_TABLE    = false;
	public static final boolean DEFAULT_NOHEADER          = false;
	public static final boolean DEFAULT_SKIP_PROBLEM_ROWS = false;
	public static final boolean DEFAULT_CHECK_AND_STOP    = false;
	
	private String[] _args = null;

	private static class CmdParams
	{
		//-----------------------
		// PARAMS
		//-----------------------
		boolean _preview       = false;

		boolean _truncate      = DEFAULT_TRUNCATE_TABLE;
//		boolean _overwrite     = false;
		boolean _noheader      = DEFAULT_NOHEADER;
		String  _fieldTerm     = DEFAULT_FIELD_TERM_STRING;
		String  _rowTerm       = DEFAULT_ROW_TERM_STRING;
		String  _nullValue     = DEFAULT_NULL_STRING;
		String  _charset       = null;//"UTF-8";

		boolean _skipProblemRows  = DEFAULT_SKIP_PROBLEM_ROWS;
		boolean _listJavaCharSets = false;
		boolean _checkAndStop     = DEFAULT_CHECK_AND_STOP;

//		boolean _queryInfo     = false;
//		boolean _rsInfo        = false;
//		boolean _noGuiQuestion = false;

//		String  _outfile       = null;
		String  _filename      = null;
		String  _tablename     = null;
		
		int     _tranBatchSize = DEFAULT_TRAN_BATCH_SIZE;
		int     _sendBatchSize = DEFAULT_SEND_BATCH_SIZE;

		String  _fieldTermReadable; // set in parse()
		String  _rowTermReadable;   // set in parse()
	}
	private CmdParams _params = new CmdParams();
	

	private int     _rowsInserted  = 0;
	private boolean _validRowCount = false;
	private boolean _cancelled     = false;
//	private String  _tabname       = "";
//	private String  _filename      = "";
	private List<String>  _tabColumns  = null;
	private List<Integer> _tabDatatypeInt = null;
	private List<String>  _tabDatatypeStr = null;
	private Map<Integer, Integer> _f2cMap = null; // Field 2 Column Map

//	private List<String> _fileColumns = null;
	private Map<String, Integer> _fileColumnMap = null;
	private List<List<Object>> _filePreview = null;

	
	public SqlStatementCmdLoadFile(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
	throws SQLException, PipeCommandException
	{
		super(conn, sqlOrigin, dbProductName, resultCompList, progress, owner, queryWindow);
		parse(sqlOrigin);
		init();
	}

	/**
	 * ============================================================
	 * tofile (could only be done on a ResultSet)
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
//	throws PipeCommandException
	{
//		String params = input.substring(input.indexOf(' ') + 1).trim();
		String params = input.replace("\\loadfile", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('l')) _params._listJavaCharSets = true;
			if (cmdLine.hasOption('s')) _params._skipProblemRows  = true;
			if (cmdLine.hasOption('T')) _params._tablename     = cmdLine.getOptionValue('T');
			if (cmdLine.hasOption('t')) _params._truncate      = true;
			if (cmdLine.hasOption('n')) _params._noheader      = true;
			if (cmdLine.hasOption('f')) _params._fieldTerm     = cmdLine.getOptionValue('f');
			if (cmdLine.hasOption('r')) _params._rowTerm       = cmdLine.getOptionValue('r');
			if (cmdLine.hasOption('c')) _params._charset       = cmdLine.getOptionValue('c');
			if (cmdLine.hasOption('C')) _params._checkAndStop  = true;
//			if (cmdLine.hasOption('q')) _params._queryInfo     = true;
//			if (cmdLine.hasOption('i')) _params._rsInfo        = true;
//			if (cmdLine.hasOption('n')) _params._noGuiQuestion = true;
			if (cmdLine.hasOption('b')) _params._sendBatchSize = Integer.parseInt( cmdLine.getOptionValue('b') );
			if (cmdLine.hasOption('B')) _params._tranBatchSize = Integer.parseInt( cmdLine.getOptionValue('B') );
			if (cmdLine.hasOption('N')) _params._nullValue     = cmdLine.getOptionValue('N');
			if (cmdLine.hasOption('P')) _params._preview       = true;

			if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
			{
				String table = cmdLine.getArgList().get(0).toString();
				_params._filename = table;
			}
			else if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 0 )
			{
				printHelp(null, "You need to specify an input file");
			}
			else
			{
				printHelp(null, "You can only specify 1 input file");
			}

			if (StringUtil.isNullOrBlank(_params._filename))
			{
				printHelp(null, "You need to specify an output file");
			}
		}
		else
		{
			printHelp(null, "Please specify some parameters.");
		}
		
		_params._fieldTermReadable = StringUtil.escapeControlChars(_params._fieldTerm);
		_params._rowTermReadable   = StringUtil.escapeControlChars(_params._rowTerm);

//		System.out.println("TOFILE Param: _append        = '"+ _params._append            + "'.");
//		System.out.println("TOFILE Param: _overwrite     = '"+ _params._overwrite         + "'.");
//		System.out.println("TOFILE Param: _fieldTerm     = '"+ _params._fieldTermReadable + "'.");
//		System.out.println("TOFILE Param: _rowTerm       = '"+ _params._rowTermReadable   + "'.");
//		System.out.println("TOFILE Param: _nullValue     = '"+ _params._nullValue         + "'.");
//		System.out.println("TOFILE Param: _charset       = '"+ _params._charset           + "'.");
//		System.out.println("TOFILE Param: _queryInfo     = '"+ _params._queryInfo         + "'.");
//		System.out.println("TOFILE Param: _rsInfo        = '"+ _params._rsInfo            + "'.");
//		System.out.println("TOFILE Param: _noGuiQuestion = '"+ _params._noGuiQuestion     + "'.");

		// List java char sets
		if (_params._listJavaCharSets)
		{
			StringBuilder sb = new StringBuilder();

			Map<String, Charset> charSets = Charset.availableCharsets();
			Iterator<String> it = charSets.keySet().iterator();
			while (it.hasNext())
			{
				String csName = it.next();
				sb.append(csName);
				Iterator<String> aliases = charSets.get(csName).aliases().iterator();
				if ( aliases.hasNext() )
					sb.append(": ");
				while (aliases.hasNext())
				{
					sb.append(aliases.next());
					if ( aliases.hasNext() )
						sb.append(", ");
				}
				sb.append('\n');
			}

			throw new SQLException("LOADFILE: Available java charsets: \n" + sb.toString());
		}
	}

	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		options.addOption( "l", "listJavaCharSets", false, "fixme" );
		options.addOption( "s", "skipProblemRows",  false, "fixme" );
		options.addOption( "T", "tablename",        true,  "fixme" );
		options.addOption( "t", "truncateTable",    false, "fixme" );
		options.addOption( "n", "noHeader",         false, "fixme" );
		options.addOption( "f", "field_terminator", true,  "fixme" );
		options.addOption( "r", "row_terminator",   true,  "fixme" );
		options.addOption( "c", "charset",          true,  "fixme" );
		options.addOption( "B", "batchSize",        true,  "fixme" );
		options.addOption( "b", "sendBatchSize",    true,  "fixme" );
		options.addOption( "C", "checkAndStop",     false, "fixme" );
		options.addOption( "m", "columnMapping",    true,  "fixme" );
		options.addOption( "N", "nullValue",        true,  "fixme" );
		options.addOption( "P", "preview",          false, "fixme" );


		try
		{
			// create the command line com.asetune.parser
//			CommandLineParser parser = new PosixParser();
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
			{
				String error = "You need to specify an filename";
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

//		sb.append("usage: bcp [[dbname.]owner.]tablename [-U user] [-P passwd] [-S servername|host:port] [-D dbname] [-u url] [-b batchSize] [-s] [-i initStr]\n");
		sb.append("usage: loadfile [options] -T tablename filename\n");
		sb.append("   \n");
		sb.append("description: \n");
		sb.append("  Reads a file and insert data into a table.\n");
		sb.append("  This is used to import (a lot of rows) from plain files.\n");
		sb.append("  The file can be in CSV (Comma Separated Value) format\n");
//		sb.append("  or some other of the known file formats (see -t)\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -T,--tablename               Name of the table to insert into.\n");
		sb.append("  -l,--listJavaCharSets        List Available Java Character sets.\n");
		sb.append("  -s,--skipProblemRows         If insert to a specific row has problems\n");
		sb.append("                               simply skip this row and continue with next one.\n");
		sb.append("                               Note: this will set batchsize=0 and sendbatchsize=1\n");
		sb.append("  -n,--noHeader                The input file doesnt have column header DEFAULT=false\n");
		sb.append("  -f,--field_terminator <str>  Character between fields        DEFAULT=,\n");
		sb.append("  -r,--row_terminator <str>    Character(s) to terminate a row DEFAULT=\\n\n");
		sb.append("  -N,--nullValue <str>         NULL Value representation       DEFAULT=<NULL>\n");
		sb.append("  -c,--charset <name>          File content Characterset name  DEFAULT=guessed by the content\n");
		sb.append("  -C,--checkAndStop            Try to add first record, but then rollback\n");
		sb.append("                               Note: this can be used to check if it will work.\n");
		sb.append("  -B,--batchSize               Commit every X record           DEFAULT=0, All in One tran\n");
		sb.append("  -b,--sendBatchSize           Send batch of records           DEFAULT=1000\n");
//		sb.append("  -n,--noGuiQuestion           Do not show GUI questions for file overwrite\n");
		sb.append("  -t,--truncateTable           Truncate or delete records from the table at start.\n");
//		sb.append("  -t,--type                    What type of file is this\n");
//		sb.append("                                 * \n");
		sb.append("  -m,--columnMapping <str>     Redirect columns in file to table column\n");
		sb.append("                               Example: '1>c2, 2>c1, 3>c3, 4>c4'\n");
		sb.append("                               If file has field names they can be specified...\n");
		sb.append("                               Example: 'fname>c2, lname>c1, phone>c3, address>c4'\n");
		sb.append("  -P,--preview                 Preview the file, read first 10 rows, then quit.\n");
		sb.append("  \n");
//withCommentMarker               - Sets the comment start marker of the format to the specified character. Note that the comment start character is only recognized at the start of a line.
//withDelimiter                   - Sets the delimiter of the format to the specified character.
//withEscape                      - Sets the escape character of the format to the specified character.
//withHeader                      - Sets the header of the format. The header can either be parsed automatically from the input file with:
//                                      CSVFormat format = aformat.withHeader();
//                                      or specified manually with:
//                                      CSVFormat format = aformat.withHeader(metaData);
//withHeaderComments              - ** only for printing ** for examlple: withHeaderComments("Generated by Apache Commons CSV 1.1.", new Date());
//withAllowMissingColumnNames     - the missing column names behavior, true to allow missing column names in the header line, false to cause an IllegalArgumentException to be thrown.
//withIgnoreEmptyLines            - the empty line skipping behavior, true to ignore the empty lines between the records, false to translate empty lines to empty records.
//withIgnoreSurroundingSpaces     - Sets the trimming behavior of the format.
//withNullString                  - Performs conversions to and from null for strings on input and output. 
//                                  * Reading: Converts strings equal to the given nullString to null when reading records.
//                                  * Writing: Writes null as the given nullString when writing records.
//withQuote                       - Sets the quoteChar of the format to the specified character.
//withQuoteMode                   - Sets the **output** quote policy of the format to the specified value.
//withRecordSeparator             - Sets the record separator of the format to the specified String. 
//                                  Note: This setting is only used during printing and does not affect parsing. Parsing currently only works for inputs with '\n', '\r' and "\r\n" 
//withSkipHeaderRecord            - Sets whether to skip the header record.

		throw new PipeCommandException(sb.toString());
	}
	
	private void init()
	throws SQLException
	{
		System.out.println("SqlStatementCmdLoadFile.init(): _sqlOrigin = " + _sqlOrigin);
//		System.out.println("SqlStatementCmdLoadFile.init(): _params    = " + _params);
		
		// Check input parameters
//		List<String> list = StringUtil.splitOnCharAllowQuotes(_sqlOrigin, ' ', true, true, true);
//		list.remove(0);
//		if (list.size() != 2)
//		{
//			throw new SQLException("Usage: loadfile tablename filename\nThe command currently has "+list.size()+" arguments, while expecting 2\nArgument(s): "+list);
//		}

//		_tabname  = list.get(0);
//		_filename = list.get(1);
//System.out.println("_tabname =|"+_tabname+"|.");
//System.out.println("_filename=|"+_filename+"|.");

		// Check if the file exists
		File f = new File(_params._filename);
		if ( ! f.exists() )
			throw new SQLException("loadfile: File '"+_params._filename+"' doesn't exists.");

		if (StringUtil.isNullOrBlank(_params._charset))
		{
			_params._charset = FileUtils.getFileEncoding(f);
//			addResultMessage("No charset was specified, interrogation of the file guessed it was of charset '"+_params._charset+"'. If this is faulty please use the -c or --charset flag. \nNote: Available charsets: "+Charset.availableCharsets().keySet());
			addResultMessage("No charset was specified, interrogation of the file guessed it was of charset '"+_params._charset+"'. If this is faulty please use the -c or --charset flag. \nNote: To get available charsets, plase specify -l or --listJavaCharSets");
		}
System.out.println("fileEncoding=|"+_params._charset+"|.");

		// Check if the table exists
//		SqlObjectName sqlObj = new SqlObjectName(_params._tablename, _conn.getDatabaseProductName(), null, false);
		SqlObjectName sqlObj = new SqlObjectName(_conn, _params._tablename);
		DatabaseMetaData md = _conn.getMetaData();
		ResultSet rs = md.getColumns(sqlObj.getCatalogNameNull(), sqlObj.getSchemaNameNull(), sqlObj.getObjectNameNull(), "%");
		int count = 0;
		_tabColumns     = new ArrayList<>();
		_tabDatatypeInt = new ArrayList<>();
		_tabDatatypeStr = new ArrayList<>();
		while (rs.next())
		{
			// 1. TABLE_CAT String => table catalog (may be null) 
			// 2.TABLE_SCHEM String => table schema (may be null) 
			// 3.TABLE_NAME String => table name 
			// 4.COLUMN_NAME String => column name 
			// 5.DATA_TYPE int => SQL type from java.sql.Types 
			// 6.TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified 
			// 7.COLUMN_SIZE int => column size. 
			// ...

			_tabColumns    .add(rs.getString(4));
			_tabDatatypeInt.add(rs.getInt   (5));
			_tabDatatypeStr.add(rs.getString(6));
		}
		rs.close();

		System.out.println("Result from: RSMD.getTables(); count="+count);
		if (_tabColumns.size() == 0)
		{
			throw new SQLException("Table '"+_params._tablename+"' doesnt exists.");
			// Create a SQL statement like: create table XXX (yyy datatype null/not_null)
			// FIXME
			//System.out.println("-NOT-YET-IMPLEMENTED-: Create the destination table.");
		}

		// Take a preview at the data in the file
		int fileColCount = 0;
		try
		{
			CSVFormat format = _params._noheader 
					? CSVFormat.DEFAULT.withIgnoreEmptyLines() 
					: CSVFormat.DEFAULT.withHeader().withIgnoreEmptyLines();
//			CSVFormat format = CSVFormat.DEFAULT.withHeader().withIgnoreEmptyLines();
//			if (_params._noheader)
//			{
//System.out.println("Setting CSVFormat: format.withSkipHeaderRecord()");
//				format = format.withSkipHeaderRecord();
//			}
//System.out.println("CSVFormat: format="+format.toString());
			CSVParser parser = CSVParser.parse(new File(_params._filename), Charset.forName(_params._charset), format);
			
			// Create a list which would hold first X rows, so we can view content.
			_filePreview = new ArrayList<List<Object>>();

			// Copy the header map, if there is one.
			_fileColumnMap = parser.getHeaderMap();

			// Loop first rows and add it 
			int rowCount=0;
			int previewCount=10;
			for (CSVRecord record : parser)
			{
				rowCount++;
				List<Object> row = new ArrayList<Object>();
				_filePreview.add(row);
				
				fileColCount = record.size(); 
				for (int c=0; c<fileColCount; c++)
				{
					String fValue = record.get(c);
					if ( fValue != null && fValue.equals(_params._nullValue) )
						fValue = null;
					
					row.add(fValue);
				}
				if (rowCount >= previewCount)
					break;
			}
			parser.close();

			// if there wasn't any header, create some dummy field headers: f1, f2, f3
			if (_fileColumnMap == null)
			{
				_fileColumnMap = new LinkedHashMap<String, Integer>();
				for (int c=0; c<fileColCount; c++)
					_fileColumnMap.put("f"+(c+1), c);
			}
		}
		catch (IOException e)
		{
			throw new SQLException("Problems Reading file '"+_params._filename+"', caught: "+e, e);
		}

		System.out.println("File  Columns: "+_fileColumnMap.keySet());
		System.out.println("Table Columns: "+_tabColumns);

		List<String> fileColList = new ArrayList<String>(_fileColumnMap.keySet());
		String tableString = StringUtil.toTableString(fileColList, _filePreview);
		System.out.println("File preview:");
		System.out.println(tableString);

//fixme: check column numbers in table/file
//fixme: check column names   in table/file
//fixme: check column types   in table/File .class.. probably not do-able, unless we *guess* quite a bit
//fixme: check column allow NULL in table/File... lets use first 10 rows.. to guess with...
//
		if (fileColCount != _tabColumns.size())
		{
			String msg = "ERROR: The file and table do not have the same number of columns. \n" +
					"TableColumnCount="+_tabColumns.size()+", FileColumnCount="+fileColCount+"\n"+
					"\n" +
					"Table columns: " + _tabColumns + "\n" +
					"\n" +
					"File headers:  " + fileColList + "\n" +
					"\n" +
					"File preview, of first 10 rows: \n" +
					tableString;
			throw new SQLException(msg);
		}

		if (_params._preview)
		{
			String msg = "STOP: Preview of the file was specified.\n" +
					"TableColumnCount="+_tabColumns.size()+", FileColumnCount="+fileColCount+"\n"+
					"\n" +
					"Table columns: " + _tabColumns + "\n" +
					"\n" +
					"File headers:  " + fileColList + "\n" +
					"\n" +
					"File preview, of first 10 rows: \n" +
					tableString;
			throw new SQLException(msg);
		}

//		DatabaseMetaData dbmd = _conn.getMetaData();
//		SqlObjectName sqlo = new SqlObjectName(_tabname, _conn.getDatabaseProductName(), "\"", _conn.getMetaData().getdbStoresUpperCaseIdentifiers)
//		DbUtils.checkIfTableExistsNoThrow(_conn, cat, schema, tableName);
//		dbmd.getTables(catalog, schemaPattern, tableNamePattern, types)
		
		// check if the table has appropriate number of columns

//		int c=0;
//		for (String str : list)
//		{
//			System.out.println(c+"=|"+str+"|.");
//			c++;
//		}


//		try
//		{
//    		Reader in = new FileReader(_filename);
////    		Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(in);
//    		Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(in);
//    		int row=0;
//			for (CSVRecord record : records)
//			{
//				row++;
//				System.out.println("CSVRecord["+row+"]: "+record.toString());
////				String lastName = record.get("Last Name");
////				String firstName = record.get("First Name");
//			}
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//		}
	}

	@Override
	public Statement getStatement()
	{
		return new StatementDummy()
		{
			@Override
			public int getUpdateCount() throws SQLException
			{
				// NOTE: last call to this should return -1, otherwise the result loop expects to continue to loop
				if ( ! _validRowCount )
					return -1;

				_validRowCount = false;
				return _rowsInserted;
			}
			
			@Override
			public void cancel() throws SQLException
			{
				_cancelled = true;
			}
		};
	}

	@Override
	public boolean execute() throws SQLException
	{
//		if ("".equals("xxx"))
//			throw new SQLException("Sorry not yet implemented.");
		
		setProgressState("Load CSV file '"+_params._filename+"'.");

//		System.out.println("NOTE: here we should implement the LOADFILE command...");
//
//		for (int i=3500; i>0; i--)
//		{
//			try { Thread.sleep(1); }
//			catch (InterruptedException e) { e.printStackTrace(); }
//			
////			if (_progress != null && _progress.isCancelled())
//			if (_cancelled)
//				break;
//			
//			if (_progress != null)
//			{
//				_progress.setState("at row "+i);
//			}
//			_rowsInserted++;
//		}
//		
//		_validRowCount = true;
////		_rowsInserted = 33;

		
		List<String> headerRow = _tabColumns;
		String questionmarks = StringUtils.repeat("?,", headerRow.size());
		questionmarks = (String) questionmarks.subSequence(0, questionmarks.length() - 1);

		String query = SQL_INSERT.replaceFirst(TABLE_REGEX, _params._tablename);
		query = query.replaceFirst(KEYS_REGEX, StringUtils.join(headerRow, ","));
		query = query.replaceFirst(VALUES_REGEX, questionmarks);

		System.out.println("INSERT STATEMENT: " + query);
		
		PreparedStatement ps = null;
		Reader reader = null;
		CSVRecord lastRecord = null;

		boolean startAutoCommitState = _conn.getAutoCommit();

		try
		{
			if ( _params._truncate )
			{
				setProgressState("Trying to truncate table");
				_logger.info("LOADFILE: truncating table '"+_params._tablename+"'.");
				addResultMessage("truncating table '"+_params._tablename+"'.");

				String sql = "TRUNCATE TABLE " + _params._tablename;
				// delete data from table before loading csv
				try
				{
					_conn.createStatement().execute(sql);
				}
				catch(SQLException e)
				{
					_logger.info("Problems with '"+sql+"', trying a normal 'DELETE FROM ...'. Caught: Err="+e.getErrorCode()+", State='"+e.getSQLState()+"', msg='"+e.getMessage()+"'.");
					sql = "DELETE FROM " + _params._tablename;
					_conn.createStatement().execute(sql);
				}
			}

			setProgressState("Using: "+query);

			_conn.setAutoCommit(false);
			ps = _conn.prepareStatement(query);

			int sendBatchSize = _params._sendBatchSize;
			if (_params._skipProblemRows)
				sendBatchSize = 1;

			List<CSVRecord> skippedRecordsList = new LinkedList<CSVRecord>();
			int count = 0;
			Date date = null;

			_logger.info("LOADFILE: filename='"+_params._filename+"', charset='"+_params._charset+"', tablename='"+_params._tablename+"'.");
			addResultMessage("filename='"+_params._filename+"', charset='"+_params._charset+"', tablename='"+_params._tablename+"'.");
//			reader = new FileReader(_filename);
//			reader = new FileInputStream
//			reader = new BufferedReader( new InputStreamReader( new FileInputStream(_filename), _params._charset ) );
			
//			Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);
//			CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180);
//			CSVFormat format = CSVFormat.RFC4180;
//			CSVFormat format = CSVFormat.EXCEL;
//			CSVFormat format = CSVFormat.DEFAULT.withSkipHeaderRecord(false).withDelimiter(',').withIgnoreEmptyLines(true).withIgnoreSurroundingSpaces(true);
			//withSkipHeaderRecord(true);
//			CSVFormat format = CSVFormat.DEFAULT.withHeader().withIgnoreEmptyLines();
			CSVFormat format = _params._noheader 
					? CSVFormat.DEFAULT.withIgnoreEmptyLines() 
					: CSVFormat.DEFAULT.withHeader().withIgnoreEmptyLines();
			
			CSVParser parser = CSVParser.parse(new File(_params._filename), Charset.forName(_params._charset), format);
//			Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(reader);
System.out.println("LOADFILE: table columns: "+_tabColumns);
System.out.println("LOADFILE: file fields: "+parser.getHeaderMap());
System.out.println("LOADFILE: parser format: "+format.toString());
addResultMessage("parser format: "+format.toString());
			int row=0;
			long startTime = System.currentTimeMillis();
			for (CSVRecord record : parser)
			{
				row++;
				lastRecord = record;

//				System.out.println("CSVRecord["+row+"]: "+record.toString());
//				String lastName = record.get("Last Name");
//				String firstName = record.get("First Name");

				if (_cancelled)
					throw new SQLException("Execution was canceled, rolling back changes.");

//System.out.println("CSVRecord["+row+"]: "+record.toString());
				setProgressState("at row "+record.getRecordNumber());

				for (int c=0; c<record.size(); c++)
				{
					int fpos = c;
					int cpos = c+1;

					// get column<->field mapping if there is any
					if (_f2cMap != null)
						fpos = _f2cMap.get(cpos);

//					String fValue = record.get(fpos);
//					if (fValue != null && fValue.equals(_params._nullValue))
//						fValue = null; // wonder if this works, or if we need to use: ps.setNull(cpos, Types.VARCHAR); 
//					ps.setString(cpos, fValue);

//FIXME: how do I set -N <BLANK>
//OR: can we do MetaData.ifNullColumn... then if fValue==null or fValue=="" -->> this means ps.setNull()...
					String fValue = record.get(fpos);
//					if ( fValue == null || (fValue != null && fValue.equals(_params._nullValue)) )
//						ps.setNull(cpos, Types.VARCHAR);
//					else
//						ps.setString(cpos, fValue);

					int dtInt = _tabDatatypeInt.get(cpos-1);
					if ( fValue == null || (fValue != null && fValue.equals(_params._nullValue)) )
						ps.setNull(cpos, dtInt);
					else
						ps.setObject(cpos, fValue, dtInt);
				}

				ps.addBatch();
				_rowsInserted++;

				count++;
				if ( ! _params._skipProblemRows )
				{
					if ( (count % sendBatchSize) == 0 )
					{
						setProgressState("Executing batch at "+record.getRecordNumber());
						ps.executeBatch();
					}
				}
				else
				{
					setProgressState("Executing record "+record.getRecordNumber()+" in \"safe\" mode.");
					try
					{
						ps.executeBatch();
					}
					catch (SQLException e)
					{
						skippedRecordsList.add(record);
						_logger.warn("LOADFILE: Skipping record due to load problem. Record '"+record+"', caught problem: "+e);
						addResultMessage("Skipping one record due to load problem. Record '"+record+"', caught problem: "+e);
						_rowsInserted--;
					}
				}

				if (_params._checkAndStop)
				{
					throw new CheckAndStopException();
				}
			}
			setProgressState("Executing last batch.");
			ps.executeBatch(); // insert remaining records

			setProgressState("Committing records.");
			_conn.commit();
			long execTime = System.currentTimeMillis() - startTime;
			
			String skipInfo = "";
			if (skippedRecordsList.size() > 0)
				skipInfo = "Skipped "+skippedRecordsList.size()+" records due to issues. ";
			
			BigDecimal rowsPerSec = new BigDecimal(String.valueOf(_rowsInserted*1000.0/execTime)).setScale(1, BigDecimal.ROUND_HALF_UP);
			addResultMessage("Added "+_rowsInserted+" rows to table '"+_params._tablename+"'. "+skipInfo+"Using time "+TimeUtils.msToTimeStr("%?HH[:]%MM:%SS.%ms", execTime)+". Which is "+rowsPerSec+" records per second.");
		}
		catch (CheckAndStopException e)
		{
			_conn.rollback();
			addResultMessage("Stopped after FIRST Record, reason: flag '--checkAndStop' was specified.");
		}
		catch (SQLException e)
		{
			_conn.rollback();
			e.printStackTrace();
//			throw new Exception("Error occured while loading data from file to database." + e.getMessage());
			if (lastRecord != null)
				addResultMessage("Last read record was line "+lastRecord.getRecordNumber()+", and contained: "+lastRecord);
			throw e;
		}
		catch (Exception e)
		{
			_conn.rollback();
			e.printStackTrace();
			throw new SQLException("Error occured while loading data from file to database." + e.getMessage());
		}
		finally
		{
			if ( null != ps )
				ps.close();
//			if ( null != con )
//				con.close();

			if (reader != null)
				try { reader.close(); } catch(IOException e) {}
			
			// Get back to the AutoCommit state we used prior to this call 
			if (startAutoCommitState)
				_conn.setAutoCommit(true);
		}
		
		_validRowCount = true;
		return false;
	}

	private static final String	SQL_INSERT	 = "INSERT INTO ${table}(${keys}) VALUES(${values})";
	private static final String	TABLE_REGEX	 = "\\$\\{table\\}";
	private static final String	KEYS_REGEX	 = "\\$\\{keys\\}";
	private static final String	VALUES_REGEX = "\\$\\{values\\}";

	private static class CheckAndStopException
	extends SQLException
	{
		private static final long serialVersionUID = 1L;
	}


	/**
	 * Read X number of records from file.
	 * 
	 * @param filename
	 * @param encoding
	 * @param rows
	 * @param noheader
	 * @param nullStr
	 * @return
	 * @throws IOException
	 */
	public static PreviewObject readFirstRows(String filename, String encoding, int rows, boolean noheader, String nullStr)
	throws IOException
	{
		// Take a preview at the data in the file
		int fileColCount = 0;

		CSVFormat format = noheader 
				? CSVFormat.DEFAULT.withIgnoreEmptyLines() 
				: CSVFormat.DEFAULT.withHeader().withIgnoreEmptyLines();
		
//		CSVFormat format = new CSVFormat(COMMA, DOUBLE_QUOTE_CHAR, null, null, null, false, true, CRLF,
//	            null, null, null, false, false, false, false, false);

		CSVParser parser = CSVParser.parse(new File(filename), Charset.forName(encoding), format);
		
		// Create a list which would hold first X rows, so we can view content.
		List<List<Object>> filePreview = new ArrayList<>();

		// Copy the header map, if there is one.
		Map<String, Integer> fileColumnMap = parser.getHeaderMap();

		// Loop first rows and add it 
		int rowCount=0;
		int previewCount=rows;
		for (CSVRecord record : parser)
		{
			rowCount++;
			List<Object> row = new ArrayList<Object>();
			filePreview.add(row);
			
			fileColCount = record.size(); 
			for (int c=0; c<fileColCount; c++)
			{
				String fValue = record.get(c);
				if ( fValue != null && fValue.equals(nullStr) )
					fValue = null;
				
				row.add(fValue);
			}
			if (rowCount >= previewCount)
				break;
		}
		parser.close();

		// if there wasn't any header, create some dummy field headers: f1, f2, f3
		if (fileColumnMap == null)
		{
			fileColumnMap = new LinkedHashMap<String, Integer>();
			for (int c=0; c<fileColCount; c++)
				fileColumnMap.put("f"+(c+1), c);
		}
		
		//List<String> fileColList = new ArrayList<String>(fileColumnMap.keySet());
		//String tableString = StringUtil.toTableString(fileColList, filePreview);
		//
		//return tableString;
		
		List<String> fileColList = new ArrayList<String>(fileColumnMap.keySet());
		
		return new PreviewObject(fileColList, filePreview);
	}
	
	public static class PreviewObject
	{
		List<String>       _fileColList; 
		List<List<Object>> _filePreviewEntries;
		
		public PreviewObject(List<String> fileColList, List<List<Object>> filePreviewEntries)
		{
			_fileColList        = fileColList;
			_filePreviewEntries = filePreviewEntries;
		}

		public String toTableString()
		{
			String tableString = StringUtil.toTableString(_fileColList, _filePreviewEntries);
			return tableString;
		}
	}

//	public static class CSVLoader
//	{
//
//		private static final String	SQL_INSERT	 = "INSERT INTO ${table}(${keys}) VALUES(${values})";
//		private static final String	TABLE_REGEX	 = "\\$\\{table\\}";
//		private static final String	KEYS_REGEX	 = "\\$\\{keys\\}";
//		private static final String	VALUES_REGEX = "\\$\\{values\\}";
//
//		private Connection			connection;
//		private char				seprator;
//
//		/**
//		 * Public constructor to build CSVLoader object with Connection details.
//		 * The connection is closed on success or failure.
//		 * 
//		 * @param connection
//		 */
//		public CSVLoader(Connection connection)
//		{
//			this.connection = connection;
//			// Set default separator
//			this.seprator = ',';
//		}
//
//		/**
//		 * Parse CSV file using OpenCSV library and load in given database
//		 * table.
//		 * 
//		 * @param csvFile
//		 *            Input CSV file
//		 * @param tableName
//		 *            Database table name to import data
//		 * @param truncateBeforeLoad
//		 *            Truncate the table before inserting new records.
//		 * @throws Exception
//		 */
//		public void loadCSV(String csvFile, String tableName, boolean truncateBeforeLoad) throws Exception
//		{
//
//			
//			CSVReader csvReader = null;
//			if ( null == this.connection )
//			{
//				throw new Exception("Not a valid connection.");
//			}
//			try
//			{
//
//				csvReader = new CSVReader(new FileReader(csvFile), this.seprator);
//
//			}
//			catch (Exception e)
//			{
//				e.printStackTrace();
//				throw new Exception("Error occured while executing file. " + e.getMessage());
//			}
//
//			String[] headerRow = csvReader.readNext();
//
//			if ( null == headerRow )
//			{
//				throw new FileNotFoundException("No columns defined in given CSV file." + "Please check the CSV file format.");
//			}
//
//			String questionmarks = StringUtils.repeat("?,", headerRow.length);
//			questionmarks = (String) questionmarks.subSequence(0, questionmarks.length() - 1);
//
//			String query = SQL_INSERT.replaceFirst(TABLE_REGEX, tableName);
//			query = query.replaceFirst(KEYS_REGEX, StringUtils.join(headerRow, ","));
//			query = query.replaceFirst(VALUES_REGEX, questionmarks);
//
//			System.out.println("Query: " + query);
//
//			String[] nextLine;
//			Connection con = null;
//			PreparedStatement ps = null;
//			try
//			{
//				con = this.connection;
//				con.setAutoCommit(false);
//				ps = con.prepareStatement(query);
//
//				if ( truncateBeforeLoad )
//				{
//					// delete data from table before loading csv
//					con.createStatement().execute("DELETE FROM " + tableName);
//				}
//
//				final int batchSize = 1000;
//				int count = 0;
//				Date date = null;
//				while ((nextLine = csvReader.readNext()) != null)
//				{
//
//					if ( null != nextLine )
//					{
//						int index = 1;
//						for (String string : nextLine)
//						{
//							date = DateUtil.convertToDate(string);
//							if ( null != date )
//							{
//								ps.setDate(index++, new java.sql.Date(date.getTime()));
//							}
//							else
//							{
//								ps.setString(index++, string);
//							}
//						}
//						ps.addBatch();
//					}
//					if ( ++count % batchSize == 0 )
//					{
//						ps.executeBatch();
//					}
//				}
//				ps.executeBatch(); // insert remaining records
//				con.commit();
//			}
//			catch (Exception e)
//			{
//				con.rollback();
//				e.printStackTrace();
//				throw new Exception("Error occured while loading data from file to database." + e.getMessage());
//			}
//			finally
//			{
//				if ( null != ps )
//					ps.close();
//				if ( null != con )
//					con.close();
//
//				csvReader.close();
//			}
//		}
//
//		public char getSeprator()
//		{
//			return seprator;
//		}
//
//		public void setSeprator(char seprator)
//		{
//			this.seprator = seprator;
//		}
//
//	}
}
