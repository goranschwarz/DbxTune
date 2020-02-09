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
package com.asetune.tools.sqlw.msg;

import java.sql.SQLException;
import java.sql.SQLWarning;

import org.fife.ui.rtextarea.RTextArea;

import com.asetune.utils.ColorUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.StringUtil;

public class JSQLExceptionMessage
extends JAseMessage
{
	private static final long serialVersionUID = 1L;

	public static final String  PROPKEY_showToolTip_stacktrace = "JSQLExceptionMessage.tooltip.show.stacktrace";
	public static final boolean DEFAULT_showToolTip_stacktrace = true;


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

		if (Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_showToolTip_stacktrace, DEFAULT_showToolTip_stacktrace))
		{
			setToolTipText(StringUtil.stackTraceToString(ex));
		}
	}
}
