package com.asetune.tools.sqlw.msg;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.asetune.utils.ColorUtils;

public class JOracleMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;
	
	private static String createMsg(String owner, String name, String type, int sequence, int line, int position, String text, String attribute, int message_number)
	{
		return "Oracle SHOW ERRORS\n"
				+ "Type:           " + type + "\n"
				+ "Owner:          " + owner + "\n"
				+ "Name:           " + name + "\n"
				+ "sequence:       " + sequence + "\n"
				+ "line:           " + line + "\n"
				+ "position:       " + position + "\n"
				+ "attribute:      " + attribute + "\n"
				+ "message_number: " + message_number + "\n"
				+ text
				;
	}

	public JOracleMessage(String owner, String name, String type, int sequence, int line, int position, String text, String attribute, int message_number, 
			int scriptRow, String originSql, RSyntaxTextArea textArea)
	{
		super(createMsg(owner, name, type, sequence, line, position, text, attribute, message_number), 
				message_number, text, 16, scriptRow, position, originSql, textArea);

		setForeground(ColorUtils.DARK_RED);
	}
}
