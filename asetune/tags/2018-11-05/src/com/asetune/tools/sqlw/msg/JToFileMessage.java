package com.asetune.tools.sqlw.msg;

import com.asetune.sql.pipe.PipeCommand;
import com.asetune.utils.ColorUtils;

public class JToFileMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createStr(String message, PipeCommand pipeCmd)
	{
		String mainText = "The TOFILE command '"+pipeCmd.getCmd().getCmdStr()+"' Completed successfully.\n";

		return mainText + message;
	}

	public JToFileMessage(String message, PipeCommand pipeCmd, String sql)
	{
		super(createStr(message, pipeCmd), sql);

		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
}
