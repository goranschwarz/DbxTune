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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.utils.StringUtil;

public class PipeCommandToFile
extends PipeCommandAbstract
{
	private static Logger _logger = Logger.getLogger(PipeCommandToFile.class);

	private String[] _args = null;

	private static class CmdParams
	{
		//-----------------------
		// PARAMS
		//-----------------------
		boolean _useRfc4180    = false; // see: http://tools.ietf.org/html/rfc4180    or   https://commons.apache.org/proper/commons-csv/
		boolean _append        = false;
		boolean _overwrite     = false;
		boolean _header        = false;
		String  _fieldTerm     = "\t";
		String  _rowTerm       = "\n";
		String  _nullValue     = "<NULL>";
		String  _charset       = "UTF-8";
		boolean _trimValues    = false;

		boolean _queryInfo     = false;
		boolean _rsInfo        = false;
		boolean _noGuiQuestion = false;

		String  _outfile       = null;

		String  _fieldTermReadable; // set in parse()
		String  _rowTermReadable;   // set in parse()
	}
	
	private CmdParams _params = null;
	
	//-----------------------
	// Parameter type to getEndPointResult
	//-----------------------
	public static final String rowsSelected = "rowsSelected";
	public static final String rowsWritten  = "rowsWritten";
	public static final String message      = "message";

	private int        _rowsSelected = 0;
	private int        _rowsWritten  = 0;
	private String     _message      = "";
	
	public PipeCommandToFile(String input, String sqlString)
	throws PipeCommandException
	{
		super(input, sqlString);
		parse(input);
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
	throws PipeCommandException
	{
		if (input.startsWith("tofile ") || input.equals("tofile"))
		{
			String params = input.substring(input.indexOf(' ') + 1).trim();

			_args = StringUtil.translateCommandline(params);

			if (_args.length >= 1)
			{
				_params = new CmdParams();

				CommandLine cmdLine = parseCmdLine(_args);
				if (cmdLine.hasOption('a')) _params._append        = true;
				if (cmdLine.hasOption('o')) _params._overwrite     = true;
				if (cmdLine.hasOption('h')) _params._header        = true;
				if (cmdLine.hasOption('f')) _params._fieldTerm     = cmdLine.getOptionValue('f');
				if (cmdLine.hasOption('r')) _params._rowTerm       = cmdLine.getOptionValue('r');
				if (cmdLine.hasOption('c')) _params._charset       = cmdLine.getOptionValue('c');
				if (cmdLine.hasOption('t')) _params._trimValues    = true;
				if (cmdLine.hasOption('q')) _params._queryInfo     = true;
				if (cmdLine.hasOption('i')) _params._rsInfo        = true;
				if (cmdLine.hasOption('n')) _params._noGuiQuestion = true;
				if (cmdLine.hasOption('R')) _params._useRfc4180    = true;
				if (cmdLine.hasOption('N')) _params._nullValue     = cmdLine.getOptionValue('N');

				if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
				{
					String table = cmdLine.getArgList().get(0).toString();
					_params._outfile = table;
				}
				else if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 0 )
				{
					printHelp(null, "You need to specify an output file");
				}
				else
				{
					printHelp(null, "You can only specify 1 output file");
				}

				if (StringUtil.hasValue(_params._outfile) && "tofile".equals(_params._outfile))
				{
					printHelp(null, "You need to specify an output file");
				}
			}
			else
			{
				printHelp(null, "Please specify some parameters.");
			}
			
			if (_params._useRfc4180)
			{
				_params._fieldTerm = ",";
				_params._rowTerm   = "\r\n";
			}
			
			if (_params._nullValue.trim().equalsIgnoreCase("none"))
			{
				_params._nullValue = "";
			}
			
			_params._fieldTermReadable = StringUtil.escapeControlChars(_params._fieldTerm);
			_params._rowTermReadable   = StringUtil.escapeControlChars(_params._rowTerm);

			System.out.println("TOFILE Param: _append        = '"+ _params._append            + "'.");
			System.out.println("TOFILE Param: _overwrite     = '"+ _params._overwrite         + "'.");
			System.out.println("TOFILE Param: _fieldTerm     = '"+ _params._fieldTermReadable + "'.");
			System.out.println("TOFILE Param: _rowTerm       = '"+ _params._rowTermReadable   + "'.");
			System.out.println("TOFILE Param: _nullValue     = '"+ _params._nullValue         + "'.");
			System.out.println("TOFILE Param: _charset       = '"+ _params._charset           + "'.");
			System.out.println("TOFILE Param: _trimValues    = '"+ _params._trimValues        + "'.");
			System.out.println("TOFILE Param: _queryInfo     = '"+ _params._queryInfo         + "'.");
			System.out.println("TOFILE Param: _rsInfo        = '"+ _params._rsInfo            + "'.");
			System.out.println("TOFILE Param: _noGuiQuestion = '"+ _params._noGuiQuestion     + "'.");
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: tofile");
		}
	}
	
	@Override
	public void doEndPoint(Object input, SqlProgressDialog progress) 
	throws Exception 
	{
		if ( ! (input instanceof ResultSet) )
			throw new Exception("Expected ResultSet as input parameter");
		
		TransferTable tt = new TransferTable(_params, progress);

		try
		{
			tt.open();
			tt.doTransfer( (ResultSet) input, this );
		}
		finally
		{
			// Always try to close the transfer,even on exceptions
			tt.close();
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
		
		if      (rowsSelected.equals(type))   return _rowsSelected;
		else if (rowsWritten .equals(type))   return _rowsWritten;
		else if (message     .equals(type))   return _message;
		else
			throw new IllegalArgumentException("Input argument/type '"+type+"' is unknown. Known types '"+rowsSelected+"', '"+rowsWritten+"'.");
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
		options.addOption( "a", "append",           false, "Append to file" );
		options.addOption( "o", "overwrite",        false, "If file already exists overwrite it" );
		options.addOption( "h", "header",           false, "Write header/column names       DEFAULT=false" );
		options.addOption( "f", "field_terminator", true,  "Character(s) between fields     DEFAULT=\\t" );
		options.addOption( "r", "row_terminator",   true,  "Character(s) to terminate a row DEFAULT=\\n" );
		options.addOption( "c", "charset",          true,  "Java Characterset name          DEFAULT=UTF8" );
		options.addOption( "t", "trim",             false, "Remove leading/trailing spaces  DEFAULT=false" );
		options.addOption( "q", "query",            false, "Print Query at the top of the file" );
		options.addOption( "i", "rsinfo",           false, "Print JDBC ResultSet info in the file" );
		options.addOption( "n", "noguiquestion",    false, "Do not show GUI questions for file overwrite" );
		options.addOption( "R", "rfc4180",          false, "Use RFC 4180 to write file" );
		options.addOption( "N", "nullValue",        true,  "If null write this value.       DEFAULT=<NULL>" );

		try
		{
			// create the command line com.asetune.parser
//			CommandLineParser parser = new PosixParser();
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
			{
				String error = "You need to specify an output file";
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
		sb.append("usage: tofile [-a] [-o] [-h] [-f <str>] [-r <str>] [-c <charset>] [-q] [-i] [-n] [-R] filename\n");
		sb.append("   \n");
		sb.append("description: \n");
		sb.append("  Write the ResultSet directly to a file instead of presenting all records in the GUI.\n");
		sb.append("  This could be used to export (a lot of rows) to plain files.\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -a,--append                  Append to file.\n");
		sb.append("  -o,--overwrite               If file already exists overwrite it.\n");
		sb.append("  -h,--header                  Write header/column names                      DEFAULT=false\n");
		sb.append("  -f,--field_terminator <str>  Character(s) between fields                    DEFAULT=\\t\n");
		sb.append("  -r,--row_terminator <str>    Character(s) to terminate a row                DEFAULT=\\n\n");
		sb.append("  -c,--charset <name>          Java Characterset name                         DEFAULT=UTF8\n");
		sb.append("  -t,--trim                    Trim or Remove leading/trailing spaces         DEFAULT=false\n");
		sb.append("  -q,--query                   Print Query at the top of the file\n");
		sb.append("  -i,--rsinfo                  Print JDBC ResultSet info in the file\n");
		sb.append("  -n,--noguiquestion           Do not show GUI questions for file overwrite\n");
		sb.append("  -R,--rfc4180                 Use RFC 4180 to write file.                    DEFAULT=false\n");
		sb.append("                                   see: http://tools.ietf.org/html/rfc4180 \n");
		sb.append("                                   Basically: embeds newline and quotes within a quoted string. \n");
		sb.append("                                   This sets -f to ',' and -r to '\\r\\n'\n");
		sb.append("  -N,--nullValue               If value is NULL in the db, write this value.  DEFAULT='<NULL>'\n");
		sb.append("                               you want it to be '' (blank), plase use -N none\n");
		sb.append("  \n");
		
		throw new PipeCommandException(sb.toString());
	}
	
	private static class TransferTable
	{
		private File           _outfile  = null;
		private BufferedWriter _writer   = null;

		private CmdParams  _cmdParams = null;
		private SqlProgressDialog _progressDialog = null;

		public TransferTable(CmdParams params, SqlProgressDialog progressDialog)
		{
			_cmdParams = params;
			_progressDialog = progressDialog;
		}
		
		public void open()
		throws Exception
		{
			_outfile = new File(_cmdParams._outfile);

			// If file exists, we may want to save it as another file
			if (_outfile.exists()  &&  ! _cmdParams._append  &&  ! _cmdParams._overwrite  &&  ! _cmdParams._noGuiQuestion)
//			if (_outfile.exists()  &&  ! _cmdParams._append  &&  ! _cmdParams._overwrite)
			{
				String htmlMsg = "<html>"
						+ "<h2>File already exists!</h2>"
						+ "<code>"+_outfile+"</code><br>"
						+ "<br>"
						+ "Tip: you can use <code>-o</code> or <code>--overwrite</code> option<br>"
						+ "Tip: you can use <code>-a</code> or <code>--append</code> option<br>"
						+ "</html>";

				Object[] buttons = {"Overwrite", "Append to current file", "Choose a new file", "Cancel"};
				int answer = JOptionPane.showOptionDialog( _progressDialog == null ? null : _progressDialog.getOwner(), 
						htmlMsg,
						"File already exists!", 
						JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE,
						null,
						buttons,
						buttons[0]);
				//----------------------------
				// Overwrite
				//----------------------------
				if (answer == 0) 
				{
					_cmdParams._overwrite = true;
				}
				//----------------------------
				// Overwrite
				//----------------------------
				else if (answer == 1) 
				{
					_cmdParams._append = true;
				}
				//----------------------------
				// Choose a new file
				//----------------------------
				else if (answer == 2)
				{
					final JFileChooser fc = new JFileChooser(_outfile);
					fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
					fc.setApproveButtonText("Save To File");
					fc.setDialogTitle("File already exists, choose another one");
					fc.setSelectedFile(_outfile);
					
					int returnValue = fc.showOpenDialog(_progressDialog == null ? null : _progressDialog.getOwner());
					if (returnValue == JFileChooser.APPROVE_OPTION) 
					{
						_outfile = fc.getSelectedFile();
						System.out.println(_outfile+"");
					}
				}
				//----------------------------
				// CANCEL
				//----------------------------
				else if (answer == 3)
				{
					throw new Exception("Cancel was pressed during 'File already exists dialog'. filename: "+_outfile);
				}
			}

			if (_outfile.exists()  &&  ! _cmdParams._append  &&  ! _cmdParams._overwrite)
				throw new Exception("File already exists, please use -o|--overwrite or -a|--append flag. filename='"+_outfile+"'.");

			try
			{
    			_writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(_outfile, _cmdParams._append), _cmdParams._charset));
			}
			catch (UnsupportedEncodingException e)
			{
				Map<String, Charset> charsetMap = Charset.availableCharsets();
				String avCharsetStr = StringUtil.toCommaStr(charsetMap.keySet());

				throw new Exception("The supplied charset '"+_cmdParams._charset+"' wasn't supported. List of available charsets: "+avCharsetStr, e);
			}
			
			_logger.info("Opened the file '"+_outfile+"' to write data. Options: append="+_cmdParams._append+", overwrite="+_cmdParams._overwrite+", header="+_cmdParams._header+", fieldTerm='"+_cmdParams._fieldTermReadable+"', rowTerm='"+_cmdParams._rowTermReadable+"', charset='"+_cmdParams._charset+"', queryInfo='"+_cmdParams._queryInfo+"', rsInfo='"+_cmdParams._rsInfo+"', noGuiQuestion='"+_cmdParams._noGuiQuestion+"'.");
		}

		public void close()
		throws Exception
		{
			if (_writer != null)
			{
				_writer.flush();
				_writer.close();
			}
		}

		public int doTransfer(ResultSet sourceRs, PipeCommandToFile pipeCmd)
		throws Exception
		{
			int sourceNumCols = -1;
			
			// get RSMD from SOURCE
			ArrayList<String>  sourceColNames   = new ArrayList<String>();
			ResultSetMetaData  sourceRsmd       = sourceRs.getMetaData();

			// Print QUERY INFO
			if (_cmdParams._queryInfo)
			{
				_writer.write("--BEGIN-QUERY-INFO--\n");
				_writer.write(pipeCmd.getSqlString());
				_writer.write("--END-QUERY-INFO--\n");
				_writer.write("\n");
			}

			// Print RESULT SET INFO
			if (_cmdParams._rsInfo)
			{
				String rsInfo = ResultSetTableModel.getResultSetInfo(sourceRsmd);

				_writer.write("--BEGIN-RESULTSET-INFO--\n");
				_writer.write( rsInfo );
				if ( ! rsInfo.endsWith("\n"))
					_writer.write("\n");
				_writer.write("--END-RESULTSET-INFO--\n");
				_writer.write("\n");
			}

			// Print HEADERS
			sourceNumCols = sourceRsmd.getColumnCount();
			for(int c=1; c<sourceNumCols+1; c++)
			{
				String colName = sourceRsmd.getColumnLabel(c);
				sourceColNames.add(colName);
				
				if (_cmdParams._header)
				{
					// Write columns header
					_writer.write(colName);
					
					// Write field or row terminator
					if (c < sourceNumCols)
						_writer.write(_cmdParams._fieldTerm);
					else
						_writer.write(_cmdParams._rowTerm);
				}
			}

			int totalCount = 0;

			// READ all rows from the ResultSet
			while (sourceRs.next())
			{
				totalCount++;
				pipeCmd._rowsSelected++;

				// for each column in source set it to the output
				for (int c=1; c<sourceNumCols+1; c++)
				{
					try
					{
						String data = sourceRs.getString(c);
						
						// Trim the data
						if (data != null && _cmdParams._trimValues)
							data = data.trim();

						// Replace null with "some" value
						if (data == null)
							data = _cmdParams._nullValue;

						// Write columns data
						if ( _cmdParams._useRfc4180 )
							_writer.write(StringUtil.toRfc4180String(data));
						else
							_writer.write(data);
						
						// Write field or row terminator
						if (c < sourceNumCols)
							_writer.write(_cmdParams._fieldTerm);
						else
							_writer.write(_cmdParams._rowTerm);
					}
					catch (SQLException sqle)
					{
System.out.println("ROW: "+totalCount+" - Problems reading row "+totalCount+", column c="+c+", sourceName='"+sourceColNames.get(c-1)+"'. Caught: "+sqle);
						throw sqle;
					}
				}

				pipeCmd._rowsWritten++;

				if (_progressDialog != null && ((totalCount % 100) == 0) )
					_progressDialog.setState("Written "+totalCount+" rows to the output file.");
			}
			sourceRs.close();

			pipeCmd._message = "Successfully wrote "+totalCount+" row(s) to file '"+_outfile+"'.\n" + 
			"Using options: append="+_cmdParams._append+", overwrite="+_cmdParams._overwrite+", header="+_cmdParams._header+", fieldTerm='"+_cmdParams._fieldTermReadable+"', rowTerm='"+_cmdParams._rowTermReadable+"', charset='"+_cmdParams._charset+"', trim="+_cmdParams._trimValues+", queryInfo='"+_cmdParams._queryInfo+"', rsInfo='"+_cmdParams._rsInfo+"', noGuiQuestion='"+_cmdParams._noGuiQuestion+"'.";

			return totalCount;
		}
	}
}
