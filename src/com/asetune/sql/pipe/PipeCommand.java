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

import java.util.ArrayList;

import com.asetune.utils.StringUtil;

/**
 * Used to pipe output from a SQL batch to "something"
 * <p>
 * This needs a lot more work before it's complete<br>
 * But the idea it to restrict 
 * 
 * @author gorans
 */
public class PipeCommand
{
	private String _cmdStr     = null;
	private String _sqlStr     = null;
//	private String _paramStr   = null;
	private ArrayList<IPipeCommand> _pipeList = new ArrayList<IPipeCommand>();
	private IPipeCommand _cmd = null; // tmp solution

	public PipeCommand(String cmd, String sqlString)
	throws PipeCommandException
	{
		if (StringUtil.isNullOrBlank(cmd))
			throw new PipeCommandException("PipeCommand, cmd can't be null or empty.");
		_cmdStr = cmd;
		_sqlStr = sqlString;
		parse();
	}
	
	/**
	 * This one should be able to parse a bunch of things.<br>
	 * and also several pipes... cmd | cmd | cmd
	 * <p>
	 * 
	 * ============================================================
	 * grep or egrep
	 * ============================================================
	 * [rs[:colname|all]|msg|all]{grep|egrep} [-v] stringToGrepFor
	 * 
	 * rs[:colname|all]  = do the command on a result set. (:onOnlyOneSpecificColumnName, or all columns) DEFAULT = 'all'
	 * msg               = do the command on a AseMessage
	 * all               = do the command on 'all' the output from the sql batch
	 * Default: *rs*
	 * ------------------------------------------------------------
	 * 
	 * ============================================================
	 * bcp (could only be done on a ResultSet)
	 * ============================================================
	 * bcp { out filename | [[dbname.]owner.]tablename [-U user] [-P passwd] [-S servername|host:port] [-b] }
	 * ------------------------------------------------------------
	 */
	private void parse()
	throws PipeCommandException
	{
//		_logger.warn("Problems creating the 'go | pipeCommand, continuing without applying this.", e);
//		SwingUtils.showWarnMessage("Problems creating PipeCommand", e.getMessage(), e);

		if (    _cmdStr.startsWith("grep ")  || _cmdStr.equals("grep") 
		     || _cmdStr.startsWith("egrep ") || _cmdStr.equals("egrep")
		   )
		{
			//_paramStr = _cmdStr.substring(_cmdStr.indexOf(' ') + 1).trim();
			IPipeCommand cmd = new PipeCommandGrep(_cmdStr, _sqlStr);
			_pipeList.add(cmd);
			_cmd = cmd;
			// for the moment this doesn't support MULTIPLE commands in the pipe
		}
		else if (   _cmdStr.startsWith("convert ") || _cmdStr.equals("convert")
		         || _cmdStr.startsWith("iconv ")   || _cmdStr.equals("iconv")
		        )
		{
			IPipeCommand cmd = new PipeCommandConvert(_cmdStr, _sqlStr);
			_pipeList.add(cmd);
			_cmd = cmd;
			// for the moment this doesn't support MULTIPLE commands in the pipe
		}
		else if (_cmdStr.startsWith("bcp ") || _cmdStr.equals("bcp"))
		{
//			_paramStr = _cmdStr.substring(_cmdStr.indexOf(' ') + 1).trim();
			IPipeCommand cmd = new PipeCommandBcp(_cmdStr, _sqlStr);
			_pipeList.add(cmd);
			_cmd = cmd;
			// for the moment this doesn't support MULTIPLE commands in the pipe
		}
		else if (_cmdStr.startsWith("tofile ") || _cmdStr.equals("tofile"))
		{
			IPipeCommand cmd = new PipeCommandToFile(_cmdStr, _sqlStr);
			_pipeList.add(cmd);
			_cmd = cmd;
			// for the moment this doesn't support MULTIPLE commands in the pipe
		}
		else if (   _cmdStr.startsWith("graph ") || _cmdStr.equals("graph")
		         || _cmdStr.startsWith("chart ") || _cmdStr.equals("chart")
		        )
		{
			IPipeCommand cmd = new PipeCommandGraph(_cmdStr, _sqlStr);
			_pipeList.add(cmd);
			_cmd = cmd;
			// for the moment this doesn't support MULTIPLE commands in the pipe
		}
		else if ( _cmdStr.startsWith("diff ") || _cmdStr.equals("diff") )
		{
			IPipeCommand cmd = new PipeCommandDiff(_cmdStr, _sqlStr);
			_pipeList.add(cmd);
			_cmd = cmd;
			// for the moment this doesn't support MULTIPLE commands in the pipe
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+_cmdStr+"' is unknown. Available commands is: grep, egrep, bcp, tofile, convert, iconv, graph, chart, diff");
		}
	}
	
//	public String getRegExp()
//	{
//		if (_cmd instanceof PipeCommandGrep)
//			return _cmd.getConfig();
//		throw new RuntimeException("No grep command...");
//	}

	/**
	 * TODO: this is a temp workaround, to be deleted
	 * If the pipe containes a BCP command
	 * @return when it containes bcp command
	 */
	public boolean isBcp()
	{
		return (_cmd instanceof PipeCommandBcp);
	}

	/**
	 * TODO: this is a temp workaround, to be deleted
	 * If the pipe containes a GREP command
	 * @return when it containes GREP command
	 */
	public boolean isGrep()
	{
		return (_cmd instanceof PipeCommandGrep);
	}

	public IPipeCommand getCmd()
	{
		return _cmd;
	}

//	public String getCmdStr()
//	{
//		return _cmdStr;
//	}
}
