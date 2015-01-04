package com.asetune.tools.sqlw.msg;

import java.util.Scanner;

import com.asetune.utils.ColorUtils;

public class JSkipSendSqlStatement
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createStr(String sql)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("-----------------------------------------------------------------------------------------------------------\n");
		sb.append("The below SQL Statement looks like it only consist of comments... This will NOT be sent to the server.     \n");
		sb.append("Note: If you still want to SEND it, the behaiviour can be changed under: Options -> Send empty SQL Batches \n");
		sb.append("-----------------------------------------------------------------------------------------------------------\n");
		Scanner scanner = new Scanner(sql);
		int row = 0;
		while (scanner.hasNextLine()) 
		{
			String str = scanner.nextLine();
			row++;
			sb.append(row).append("> ").append(str);
			if (scanner.hasNextLine())
				sb.append("\n");
		}
		return sb.toString();
	}

	public JSkipSendSqlStatement(String sql)
	{
		super(createStr(sql), sql);
		
		setForeground(ColorUtils.VERY_DARK_YELLOW);
	}
}
