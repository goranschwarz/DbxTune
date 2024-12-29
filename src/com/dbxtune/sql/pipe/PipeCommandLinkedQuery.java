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
package com.dbxtune.sql.pipe;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.dbxtune.sql.SqlProgressDialog;
import com.dbxtune.utils.ConnectionProvider;
import com.dbxtune.utils.StringUtil;

/**
 * NOTE: THIS IS NOT YET IMPLEMENTED
 * <p>
 * Execute a new query based on the row you are currently at<br>
 * When you change row, the query in the <i>linked query window</i> will automatically be refreshed.<br>
 * <br>
 * Example (ForeignKey lookup: 'order' and 'order_entry' table):
 * <pre>
 * select * from order
 * go | lq -q 'select * from order_entry where order_id = ${order_id}'
 * </pre>
 * More than one '-q' parameters should be possible, one <i>linked query window</i> should be opened for every '-q' <br>
 * <br>
 * Several levels of <i>Linked Queries</i> should be possible (but not yet determined how to implement this).<br>
 * This means several levels of ForeignKey links <br>
 * <br>
 *  
 */
public class PipeCommandLinkedQuery
extends PipeCommandAbstract
{
	private String[] _args = null;

	private static class CmdParams
	{
		String  _query = null;
		boolean _debug = false;
		
		@Override
		public String toString()
		{
			StringBuilder sb = new StringBuilder();

			sb.append(""  ).append("query".trim()).append("=").append(StringUtil.quotify(_query));
			sb.append(", ").append("debug".trim()).append("=").append(StringUtil.quotify(_debug));
			sb.append(".");

			return sb.toString();
		}
	}
	
	private CmdParams _params = null;

	
	public String        getCmdLineParams()        { return _params.toString();            }
	                                                                                       
	public String        getQuery()                { return _params._query;                }
	public boolean       isDebugEnabled()          { return _params._debug;                }

	public PipeCommandLinkedQuery(String input, String sqlString, ConnectionProvider connProvider)
	throws PipeCommandException
	{
		super(input, sqlString, connProvider);
		parse(input);
	}

	public void parse(String input)
	throws PipeCommandException
	{
		if (    input.startsWith("lq ")          || input.equals("lq") 
		     || input.startsWith("linkedquery ") || input.equals("linkedquery")
		   )
		{
			_args = StringUtil.translateCommandline(input, true);

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('q')) _params._query                = cmdLine.getOptionValue('q');
			if (cmdLine.hasOption('x')) _params._debug                = true;
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: lq or linkedquery");
		}
	}

	@Override
	public String getConfig()
	{
		if (_params == null)
			return "linkedquery: ...";
		
		return _params.toString();
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

		// Switches       short long Option              hasParam Description (not really used)
		//                ----- ------------------------ -------- ------------------------------------------
		options.addOption( "q", "query",                 true,    "SQL Text to be executed in linked query window" );
		options.addOption( "x", "debug",                 false,   "debug" );

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
		sb.append("usage: lq or linkedquery [-q] [-x]\n");
		sb.append("\n");
		sb.append("options: \n");
		sb.append("  -q,--query                SQL Text to be executed in linked query window \n");
		sb.append("\n");
		sb.append("  -x,--debug                Debug, print some extra info \n");
		sb.append("  \n");
		sb.append("  \n");
		
		
		throw new PipeCommandException(sb.toString());
	}


	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------
	//-----------------------------------------------------------------------------------------


	@Override
	public void doPipe(Object input) throws Exception
	{
		throw new Exception("LinkedQuery is NOT-YET-IMPLEMENTED");
	}

	@Override
	public void doEndPoint(Object input, SqlProgressDialog progress) 
	throws Exception 
	{
		throw new Exception("LinkedQuery is NOT-YET-IMPLEMENTED");
	}
}
