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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.asetune.sql.SqlProgressDialog;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.pipe.PipeCommandException;
import com.asetune.tools.sqlw.msg.JAseMessage;
import com.asetune.tools.sqlw.msg.JSetOutput;
import com.asetune.utils.StringUtil;

public class SqlStatementCmdSet 
extends SqlStatementAbstract
{
	private static Logger _logger = Logger.getLogger(SqlStatementCmdSet.class);

	public static final String PROPKEY_PREFIX = "SQLWindow.variable.";
	
	private static LinkedHashMap<String, String> _variableMap = new LinkedHashMap<>();

	private String[] _args = null;
	private String _originCmd = null;

	private static class CmdParams
	{
		//-----------------------
		// PARAMS
		//-----------------------
		boolean _unset           = false;
		boolean _printVariables  = false;

		String  _variable        = null;
		String  _value           = null;
	}
	private CmdParams _params = new CmdParams();
	

	public SqlStatementCmdSet(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
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
		String params = input.replace("\\set", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('u')) _params._unset = true;

			if (cmdLine.hasOption('?'))
				printHelp(null, "You wanted help...");

			if ( cmdLine.getArgs() != null && (cmdLine.getArgs().length == 1 ) )
			{
				if (cmdLine.getArgs().length == 1)
				{
					String str = cmdLine.getArgList().get(0).toString();
					String[] sa = str.split("=");
					if (sa.length >= 1)
						_params._variable = sa[0]; 
					if (sa.length >= 2)
						_params._value = sa[1]; 
					if (sa.length >= 3)
						_logger.warn("Received more values than expected, skipping the rest of the values starting from '"+sa[2]+"'.");
				}
			}
			else if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 0 )
			{
				_params._printVariables = true;
			}
		}
		else
		{
			_params._printVariables = true;
		}
	}

	private CommandLine parseCmdLine(String[] args)
	throws PipeCommandException
	{
		Options options = new Options();

		// create the Options
		options.addOption( "u", "unset",  false, "fixme" );
		options.addOption( "?", "help",   false, "fixme" );


		try
		{
			// create the command line com.asetune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

			if ( cmd.getArgs() != null && cmd.getArgs().length > 2 )
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

		sb.append("usage: set [-u] [var=value]\n");
		sb.append("   \n");
		sb.append("description: \n");
		sb.append("  Set a variable that can be used later in the command text.\n");
		sb.append("  If you just do '\\set' a list of variables will be displayed.\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -u,--unset    Unset the variable.\n");
		sb.append("  -?,--help     Print this text.\n");
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
		if (_params._printVariables)
		{
			JSetOutput output = new JSetOutput(_originCmd, _variableMap, "cmd: \\set list variable(s)");
			_resultCompList.add(output);
		}
		else
		{
			if (_params._unset)
			{
				String prev = _variableMap.remove(_params._variable);

				if (prev != null)
				{
    				JSetOutput output = new JSetOutput(_originCmd, null, "cmd: \\set removed variable '"+_params._variable+"' succeeded.");
    				_resultCompList.add(output);
				}
				else
				{
    				JSetOutput output = new JSetOutput(_originCmd, null, "cmd: \\set removed variable '"+_params._variable+"' was NOT FOUND.");
    				_resultCompList.add(output);
				}
			}
			else
			{
				String prev = _variableMap.put(_params._variable, _params._value);

				JSetOutput output = new JSetOutput(_originCmd, null, "cmd: \\set assigned variable '"+_params._variable+"' to value '"+_params._value+"', previous value was '"+prev+"'.");
				_resultCompList.add(output);
			}
		}
		
		return false;
	}

	/**
	 * This searches and replaces ${varName} into a real value<br>
	 * If no variables has been set or if we cant find any '${' in the input, then it simply does nothing.
	 * 
	 * @param val               The string to replace variables in
	 * @param skipList          If the 'val' starts with any of the strings in this array: Then do NOT substitute variables
	 * @param resultCompList    Put error messages in this list (if this is null, message will be written using System.out.println
	 * @return
	 */
	public static String substituteVariables(String val, String[] skipList, List<JComponent> resultCompList)
	{
		// Below is grabbed from: StringUtil.envVariableSubstitution(val)
		
		if (val == null)
			return val;

		if (_variableMap == null)
			return val;
		
		// Check if the input string; if it's part of the skip list
		if (skipList != null)
		{
			for (String skip : skipList)
			{
				if (val.startsWith(skip))
				{
					// Only print if we can find a variable in the input text
					Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}"); // or maybe: "\\$\\{[A-Za-z0-9_]+\\}"
					if (compiledRegex.matcher(val).find())
					{
    					String msg = "NOTE: substituteVariables(): The input string starts with '"+skip+"', which is part of the 'skip list'... Skipping variable substitution of this statement before execution.";
    					if (resultCompList == null)
    						System.out.println(msg);
    					else
    						resultCompList.add(new JAseMessage(msg, val));
					}

					// Return original String
					return val;
				}
			}
		}
		
//		if (_variableMap.isEmpty())
//			return val;
		
		// Nothing to substitute
		if (val.indexOf("${") < 0)
			return val;

		
		// Extract Environment variables
		// search for ${ENV_NAME}
		Pattern compiledRegex = Pattern.compile("\\$\\{.*\\}"); // or maybe: "\\$\\{[A-Za-z0-9_]+\\}"
		while( compiledRegex.matcher(val).find() )
		{
			String defVal     = "";
			String envVal     = null;
			String envName    = val.substring( val.indexOf("${")+2, val.indexOf("}") );
			String envNameTxt = envName;

			// Has it got a default value if not set... ${varname:-defValue}
			int colonPos = envName.indexOf(":");
			if (colonPos >= 0)
			{
				String varName = envName.substring(0, colonPos);
				String rest    = envName.substring(colonPos + 1);

				// '-' use default value if it do not exist
				// '=' set variable to value if it do not exist
				if (rest.startsWith("-") || rest.startsWith("="))
				{
					envName = varName;
					defVal  = rest.substring(1);

					// Set the value if it do not exists
					if (rest.startsWith("="))
					{
						if ( ! _variableMap.containsKey(envName) )
							_variableMap.put(envName, defVal);
					}
				}
			}

			envVal = _variableMap.get(envName);
			if (envVal == null)
			{
				envVal = defVal;

				String msg = "substituteVariables(): variable '"+envName+"' has not been set. replacing the variable with '"+envVal+"'. Please set the value using \\set varname=value.";
				if (resultCompList == null)
					System.out.println(msg);
				else
					resultCompList.add(new JAseMessage(msg, val));
			}


			// Backslashes does not work that good in replaceFirst()...
			// So change them to / instead...
			envVal = envVal.replace('\\', '/');

			// NOW substitute the ENV VARIABLE with a real value...
			val = val.replaceFirst("\\$\\{"+envNameTxt+"\\}", envVal);
		}

		return val;
	}
}
