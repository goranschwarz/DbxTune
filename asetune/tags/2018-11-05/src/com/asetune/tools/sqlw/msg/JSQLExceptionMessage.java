package com.asetune.tools.sqlw.msg;

import java.sql.SQLException;
import java.sql.SQLWarning;

import org.fife.ui.rtextarea.RTextArea;

import com.asetune.utils.ColorUtils;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class JSQLExceptionMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	private static String createMsg(SQLException ex, String productName)
	{
		String prodName = productName; 
		if (DbUtils.DB_PROD_NAME_HANA.equals(productName)) prodName = "HANA";

		String sqlExceptionsText = ex.getMessage();
//		SQLException xxx = ex;
//		while (xxx != null)
//		{
//			System.out.println("XXXX:" + prodName + ": ErrorCode "+xxx.getErrorCode()+", SQLState "+xxx.getSQLState()+", ExceptionClass: " + xxx.getClass().getName() + "\n");
//			xxx = xxx.getNextException();
//		}
//		System.out.println("JSQLExceptionMessage: prodName='"+prodName+"'.");
//		ex.printStackTrace();

		String sqlWarningsText = "";
		if (ex instanceof SQLWarning)
		{
			int w = 0;
			SQLWarning sqlw = (SQLWarning)ex;
			while (sqlw != null)
			{
				//System.out.println("SQLWarning: "+sqlw);
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
				sqlWarningsText = "\nBelow is the full SQLWarning chain, there are "+w+" Warnings:\n" + sqlWarningsText;
		}
		
		String text = sqlExceptionsText;
		if (StringUtil.hasValue(sqlWarningsText))
		{
			if ( ! text.endsWith("\n") )
				text += "\n";
			text += sqlWarningsText;
		}

		return prodName + ": ErrorCode "+ex.getErrorCode()+", SQLState "+ex.getSQLState()+", ExceptionClass: " + ex.getClass().getName() + "\n"
//			+ "("+Version.getAppName()+": The SQL Batch was aborted due to a thrown SQLException)\n"
			+ text;
	}
	public JSQLExceptionMessage(SQLException ex, String productName, int line, int col, String originSql, String objectText, RTextArea sqlTextArea)
	{
		super(createMsg(ex, productName), ex.getErrorCode(), ex.getMessage(), -1, line, col, originSql, sqlTextArea);
		setObjectText(objectText);
		setForeground(ColorUtils.RED);
	}
}
