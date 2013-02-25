package com.asetune.sql.pipe;

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
	private String _regexpStr  = null;

	public PipeCommand(String cmd)
	throws UnknownPipeCommandException
	{
		if (StringUtil.isNullOrBlank(cmd))
			throw new UnknownPipeCommandException("PipeCommand, cmd can't be null or empty.");
		_cmdStr = cmd;
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
	throws UnknownPipeCommandException
	{
//		_logger.warn("Problems creating the 'go | pipeCommand, continuing without applying this.", e);
//		SwingUtils.showWarnMessage("Problems creating PipeCommand", e.getMessage(), e);

		if (_cmdStr.startsWith("grep ") || _cmdStr.startsWith("egrep "))
		{
			_regexpStr = _cmdStr.substring(_cmdStr.indexOf(' ') + 1).trim();
		}
		else
		{
			throw new UnknownPipeCommandException("PipeCommand, cmd='"+_cmdStr+"' is unknown. Available commands is: grep, egrep");
		}
	}
	
	public String getRegExp()
	{
		return _regexpStr;
	}
}
