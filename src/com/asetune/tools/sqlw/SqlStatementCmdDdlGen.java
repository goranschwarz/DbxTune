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
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.asetune.gui.SqlTextDialog;
import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.tools.ddlgen.DdlGen;
import com.asetune.tools.ddlgen.DdlGen.Type;
import com.asetune.tools.sqlw.msg.JAseMessage;
import com.asetune.tools.sqlw.msg.JDdlGenOutput;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

public class SqlStatementCmdDdlGen 
extends SqlStatementAbstract
{
	private static Logger _logger = Logger.getLogger(SqlStatementCmdDdlGen.class);

	private String[] _args = null;
	private String _originCmd = null;

	private static class CmdParams
	{
		//-----------------------
		// PARAMS
		//-----------------------
		String  _dbname          = null;
		String  _tabname         = null;
		String  _viewname        = null;
		String  _procname        = null;
		
		boolean _useJdbcMetaData     = false;
		boolean _openWindow          = false;
		String  _saveToFile          = null;
		boolean _saveToFileOverwrite = false;
		String  _extraParams         = null;
		String  _rawParams           = null;
	}
	private CmdParams _params = new CmdParams();
	

	public SqlStatementCmdDdlGen(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
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
	{
		_originCmd = input;
//		String params = input.substring(input.indexOf(' ') + 1).trim();
		String params = input.replaceFirst("\\ddlgen", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('d')) _params._dbname      = cmdLine.getOptionValue('d');
			if (cmdLine.hasOption('t')) _params._tabname     = cmdLine.getOptionValue('t');
			if (cmdLine.hasOption('v')) _params._viewname    = cmdLine.getOptionValue('v');
			if (cmdLine.hasOption('p')) _params._procname    = cmdLine.getOptionValue('p');

			if (cmdLine.hasOption('M')) _params._useJdbcMetaData     = true;
			if (cmdLine.hasOption('w')) _params._openWindow          = true;
			if (cmdLine.hasOption('o')) _params._saveToFile          = cmdLine.getOptionValue('o');
			if (cmdLine.hasOption('O')) _params._saveToFileOverwrite = true;
			if (cmdLine.hasOption('x')) _params._extraParams         = cmdLine.getOptionValue('x');
			if (cmdLine.hasOption('X')) _params._rawParams           = cmdLine.getOptionValue('X');

			if (cmdLine.hasOption('h'))
				printHelp(null, "You wanted help...");

//			if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 1 )
//			{
//				String table = cmdLine.getArgList().get(0).toString();
//				_params._filename = table;
//			}
//			else if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 0 )
//			{
//				printHelp(null, "You need to specify an input file");
//			}
//			else
//			{
//				printHelp(null, "You can only specify 1 input file");
//			}
//
//			if (StringUtil.isNullOrBlank(_params._filename))
//			{
//				printHelp(null, "You need to specify an output file");
//			}
		}
		else
		{
			printHelp(null, "Please specify some parameters.");
		}
	}

	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		options.addOption( "d", "dbname",           true, "fixme" );
		options.addOption( "t", "table",            true, "fixme" );
		options.addOption( "v", "view",             true,  "fixme" );
		options.addOption( "p", "proc",             true, "fixme" );

		options.addOption( "M", "jdbcMetaData",     false, "fixme" );
		options.addOption( "x", "extraParams",      true,  "fixme" );
		options.addOption( "X", "rawParams",        true,  "fixme" );
		options.addOption( "w", "openWindow",       false, "fixme" );
		options.addOption( "o", "outputFile",       true,  "fixme" );
		options.addOption( "O", "overwrite",        false, "fixme" );
		options.addOption( "h", "help",             false, "fixme" );


		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
//			{
//				String error = "You need to specify an output file";
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

		sb.append("usage: ddlgen [options] \n");
		sb.append("   \n");
		sb.append("description: \n");
		sb.append("  Reverse engineer DDL (Data Definition Language).\n");
		sb.append("  Get DDL for the whole database or just some specific objects.\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -d,--dbname <name>           Name of the database to get DDL for.\n");
		sb.append("  -t,--table <name>            Name of the table to get DDL for.\n");
		sb.append("  -v,--view <name>             Name of the view to get DDL for.\n");
		sb.append("  -p,--proc <name>             Name of the procedure to get DDL for.\n");
		sb.append("                                                                          \n");
		sb.append("  -M,--jdbcMetaData            Use JDBC MetaData instance to generate DDL.\n");
		sb.append("  -x,--extraParams <params>    Send extra params to the DdlGen implemeter.\n");
		sb.append("  -X,--rawParams <params>      Send the following parameters to DdlGen implemeter (it has to decide what to do with it).\n");
		sb.append("                               Note: in -X you can use some variables/templates\n");
		sb.append("                               Example: -X '-U${username} -P${password} -S${hostport} -TC -N%'\n");
		sb.append("  -w,--openWindow              Open a Window with the DDL. Nomally it goes to output window.\n");
		sb.append("  -o,--outputFile              Save to this file. Nomally it goes to output window.\n");
		sb.append("  -O,--overwrite               If the 'outputFile' exists, overwrite the file.\n");
		sb.append("  -h,--help                    Print this text.\n");
		sb.append("  \n");

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
		setProgressState("Creating DDL Object(s)");
		DdlGen ddlgen = null;

		// Get default database
		String dbname = _conn.getCatalog();
		if (StringUtil.hasValue(_params._dbname))
			dbname = _params._dbname;

		try
		{
			ddlgen = DdlGen.create(_conn, _params._useJdbcMetaData, false);
			ddlgen.setDefaultDbname(dbname);

			DdlGen.Type type = Type.DB;
			String      name = dbname;
			if (StringUtil.hasValue(_params._tabname ))  { type = Type.TABLE;      name = _params._tabname; }
			if (StringUtil.hasValue(_params._viewname))  { type = Type.VIEW;       name = _params._viewname; }
			if (StringUtil.hasValue(_params._procname))  { type = Type.PROCEDURE;  name = _params._procname; }
			if (StringUtil.hasValue(_params._rawParams)) { type = Type.RAW_PARAMS; name = _params._rawParams; }
			
			if (StringUtil.hasValue(_params._extraParams))
				ddlgen.setExtraParams(_params._extraParams);
				
			setProgressState("<html>Generating DDL Objects...: <br><code>"+(ddlgen==null?"null":ddlgen.getCommandForType(type, name))+"</code></html>");
			
			// Generate the DDL
			String retStr = ddlgen.getDdlForType(type, name);
			
			if (StringUtil.hasValue(retStr))
			{
				_resultCompList.add(new JAseMessage(ddlgen.getUsedCommand(), _originCmd));

				// Save the output to file
				if (StringUtil.hasValue(_params._saveToFile))
				{
					File f = new File(_params._saveToFile);

					// If file exists and overwrite is enabled, delete the file.
					if (f.exists() && _params._saveToFileOverwrite)
					{
						f.delete();
						_resultCompList.add(new JAseMessage("Deleting the File '" + f.getAbsolutePath() + "'.", _originCmd));
					}

					// Write the file
					if (f.exists())
					{
						_resultCompList.add(new JAseMessage("ERROR: File '" + f.getAbsolutePath() + "' already exists, skipping write.", _originCmd));
					}
					else
					{
						FileUtils.write(f, retStr, StandardCharsets.UTF_8);
						_resultCompList.add(new JAseMessage("Content written to File '" + f.getAbsolutePath() + "'.", _originCmd));
					}
				}
				else
				{
					if (_params._openWindow)
					{
	    				SqlTextDialog dialog = new SqlTextDialog(null, retStr);
	    				dialog.setVisible(true);
					}
					else
					{
						JDdlGenOutput output = new JDdlGenOutput(ddlgen.getUsedCommand(), retStr);
						_resultCompList.add(output);
					}
				}
			}

		}
		catch (Throwable ex)
		{
			if (ddlgen != null)
				_resultCompList.add(new JAseMessage(ddlgen.getUsedCommand(), _originCmd));

			_logger.warn("Problems when generating DDL Statements: args="+(ddlgen==null?"null":ddlgen.getUsedCommand())+", Caught="+ex, ex);
			SwingUtils.showErrorMessage(null, "Problems generating DDL", 
					"<html>Problems when generating DDL Statements:<br>"
					+ "args="+(ddlgen==null?"null":ddlgen.getUsedCommand())+"<br>"
					+ "<br>"
					+ ex
					+ "</html>", ex);
		}

		setProgressState("Done");
		
		return false;
	}
}
