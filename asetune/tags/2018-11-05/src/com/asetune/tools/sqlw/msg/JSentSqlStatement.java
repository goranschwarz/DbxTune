package com.asetune.tools.sqlw.msg;

import java.util.Scanner;

import com.asetune.utils.ColorUtils;

public class JSentSqlStatement
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private int _scriptRow;

	private static String createStr(String sql)
	{
		StringBuilder sb = new StringBuilder();
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

	public JSentSqlStatement(String sql, int scriptRow)
	{
		super(createStr(sql), sql);
		_scriptRow = scriptRow;
		
		setForeground(ColorUtils.VERY_DARK_GREEN);
	}
}
