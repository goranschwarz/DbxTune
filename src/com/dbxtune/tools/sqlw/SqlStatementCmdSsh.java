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
package com.dbxtune.tools.sqlw;

import java.awt.Component;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.dbxtune.gui.SqlTextDialog;
import com.dbxtune.gui.swing.PromptForPassword;
import com.dbxtune.sql.JdbcUrlParser;
import com.dbxtune.sql.SqlProgressDialog;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.pipe.PipeCommandException;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.tools.sqlw.msg.JAseMessage;
import com.dbxtune.tools.sqlw.msg.JSshOutput;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

public class SqlStatementCmdSsh 
extends SqlStatementAbstract
{
	private static Logger _logger = Logger.getLogger(SqlStatementCmdSsh.class);

	private String[] _args = null;
	private String _originCmd = null;

	private static class CmdParams
	{
		//-----------------------
		// PARAMS
		//-----------------------
		String  _loginName       = System.getProperty("user.name");
		String  _hostName        = null;
		int     _portNumber      = 22;
		String  _identity        = null;
		String  _password        = SshConnection.PROMPT_FOR_PASSWORD;
		
		String  _cmd             = null;

		boolean _openWindow      = false;
	}
	private CmdParams _params = new CmdParams();
	

	public SqlStatementCmdSsh(DbxConnection conn, String sqlOrigin, String dbProductName, ArrayList<JComponent> resultCompList, SqlProgressDialog progress, Component owner, QueryWindow queryWindow)
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
		String params = input.replace("\\ssh", "").trim();

		_args = StringUtil.translateCommandline(params, false);

		if (_args.length >= 1)
		{
			_params = new CmdParams();

			CommandLine cmdLine = parseCmdLine(_args);
			if (cmdLine.hasOption('l')) _params._loginName   =                     cmdLine.getOptionValue('l');
			if (cmdLine.hasOption('h')) _params._hostName    =                     cmdLine.getOptionValue('h');
			if (cmdLine.hasOption('p')) _params._portNumber  = StringUtil.parseInt(cmdLine.getOptionValue('p'), 22);
			if (cmdLine.hasOption('i')) _params._identity    =                     cmdLine.getOptionValue('i');

			if (cmdLine.hasOption('w')) _params._openWindow  = true;

			if (cmdLine.hasOption('?'))
				printHelp(null, "You wanted help...");

			if ( cmdLine.getArgs() != null && (cmdLine.getArgs().length == 1 || cmdLine.getArgs().length == 2) )
			{
				if (cmdLine.getArgs().length == 1)
				{
					String hostname = JdbcUrlParser.parse(_conn.getMetaData().getURL()).getHost();
					_params._hostName = hostname;
					_params._cmd = cmdLine.getArgList().get(0).toString();
				}
				else
				{
					String str = cmdLine.getArgList().get(0).toString();
					if (str.indexOf('@') >= 0)
					{
						String[] sa = str.split("@");
						_params._loginName = sa[0];
						_params._hostName  = sa[1];
					}
					else
					{
						_params._hostName  = str;
					}
					_params._cmd = cmdLine.getArgList().get(1).toString();
				}
			}
			else if ( cmdLine.getArgs() != null && cmdLine.getArgs().length == 0 )
			{
				printHelp(null, "You need to specify a command");
			}
//			else
//			{
//				printHelp(null, "You can only specify 1 input file");
//			}

			if (StringUtil.isNullOrBlank(_params._cmd))
			{
				printHelp(null, "You need to specify a cmd");
			}
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
		options.addOption( "l", "login",            true, "fixme" );
		options.addOption( "h", "host",             true, "fixme" );
		options.addOption( "p", "port",             true, "fixme" );
		options.addOption( "i", "identity",         true,  "fixme" );

		options.addOption( "w", "openWindow",       false, "fixme" );
		options.addOption( "?", "help",             false, "fixme" );


		try
		{
			// create the command line com.dbxtune.parser
			CommandLineParser parser = new DefaultParser();

			// parse the command line arguments
			CommandLine cmd = parser.parse( options, args );

//			if ( cmd.getArgs() != null && cmd.getArgs().length == 0 )
//			{
//				String error = "You need to specify an output file";
//				printHelp(options, error);
//			}
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

		sb.append("usage: ssh [user@hostname] [options] cmd\n");
		sb.append("   \n");
		sb.append("description: \n");
		sb.append("  Execute a command on a remote server).\n");
		sb.append("  The output is presented in the output panel.\n");
		sb.append("   \n");
		sb.append("Note: \n");
		sb.append("  This is not a normal tty session.\n");
		sb.append("  So everything is not available.\n");
		sb.append("   \n");
		sb.append("options: \n");
		sb.append("  -l,--login <name>           Username to login with.\n");
		sb.append("  -h,--host <name>            Hostname to connect to.\n");
		sb.append("  -p,--port <number>          Port number to connect to.\n");
		sb.append("  -i,--identity <filename>    Private Key File.\n");
		sb.append("                                                                          \n");
		sb.append("  -w,--openWindow              Open a Window with the result. Nomally it goes to output window.\n");
		sb.append("  -?,--help                    Print this text.\n");
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
		SshConnection sshConn = null;
		
		String savedPasswd = PromptForPassword.getSavedPassword(_params._hostName, _params._loginName);
		if (savedPasswd != null)
			_params._password = savedPasswd;

		try
		{
			setProgressState("Connecting to host '"+_params._hostName+"' as '"+_params._loginName+"' using SSH.");
			sshConn = new SshConnection(_params._hostName, _params._portNumber, _params._loginName, _params._password, _params._identity);
//			sshConn.setWaitForDialog(this.);
			sshConn.setGuiOwner(_owner);
			sshConn.connect();
				
			setProgressState("Executing cmd: "+_params._cmd);
			String retStr = sshConn.execCommandOutputAsStr(_params._cmd);
			
			if (StringUtil.hasValue(retStr))
			{
				_resultCompList.add(new JAseMessage(_params._cmd, _originCmd));

				if (_params._openWindow)
				{
    				SqlTextDialog dialog = new SqlTextDialog(null, retStr);
    				dialog.setVisible(true);
				}
				else
				{
					JSshOutput output = new JSshOutput(_params._cmd, retStr);
					_resultCompList.add(output);
				}
			}

		}
		catch (Throwable ex)
		{
//			if (ddlgen != null)
//				_resultCompList.add(new JAseMessage(_params._cmd, _originCmd));

			_logger.warn("Problems when Executing SSH Command: cmd="+_params._cmd+", Caught="+ex, ex);
			SwingUtils.showErrorMessage(null, "Problems Executing SSH Command", 
					"<html>Problems when Executing SSH Command:<br>"
					+ "cmd="+_params._cmd+"<br>"
					+ "<br>"
					+ ex
					+ "</html>", ex);
		}
		
		if (sshConn != null)
		{
			setProgressState("Closing SSH Connection");
			sshConn.close();
		}

		setProgressState("Done");
		
		return false;
	}
}
