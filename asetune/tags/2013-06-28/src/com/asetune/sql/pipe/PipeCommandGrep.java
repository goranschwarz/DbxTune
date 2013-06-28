package com.asetune.sql.pipe;

public class PipeCommandGrep
extends PipeCommandAbstract
{
	private String _grepStr = null;

	public PipeCommandGrep(String input)
	throws PipeCommandException
	{
		parse(input);
	}

	public void parse(String input)
	throws PipeCommandException
	{
		if (input.startsWith("grep ") || input.startsWith("egrep "))
		{
			String params = input.substring(input.indexOf(' ') + 1).trim();
			// TODO parse for cmdline switches to grep: like -v etc
			_grepStr = params;
		}
		else
		{
			throw new PipeCommandException("PipeCommand, cmd='"+input+"' is unknown. Available commands is: grep, egrep");
		}
	}

	@Override
	public String getConfig()
	{
		return _grepStr;
	}
}
