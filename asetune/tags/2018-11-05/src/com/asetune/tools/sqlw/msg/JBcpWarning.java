package com.asetune.tools.sqlw.msg;

import java.sql.SQLWarning;

import com.asetune.sql.pipe.PipeCommand;
import com.asetune.utils.ColorUtils;

public class JBcpWarning
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createStr(SQLWarning sqlw, PipeCommand pipeCmd)
	{
		String mainText = "The BCP command '"+pipeCmd.getCmd().getCmdStr()+"' had some Warning messages.\n";
		String sqlWarningsText = "";

		int w = 0;
		while (sqlw != null)
		{
			String wmsg = sqlw.getMessage();
			
			sqlWarningsText += "SQLWarning("+w+"): " + wmsg;
			if ( ! sqlWarningsText.endsWith("\n") )
				sqlWarningsText += "\n";
				
			sqlw = sqlw.getNextWarning();
			if (w == 0 && sqlw == null)
				break;
			w++;
		}
		if (w > 1) // If we had a Warning Chain... add the chain, else "reset" the warnings...
			sqlWarningsText = "Below is the full SQLWarning chain, there are "+w+" Warnings:\n" + sqlWarningsText;

		return mainText + sqlWarningsText;
	}

	public JBcpWarning(SQLWarning bcpSqlWarning, PipeCommand pipeCmd, String sql)
	{
		super(createStr(bcpSqlWarning, pipeCmd), sql);

		setForeground(ColorUtils.VERY_DARK_YELLOW);
	}
}
