/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.sql.Connection;
import java.util.HashMap;

import com.asetune.tools.QueryWindow;


/**
 * @author gorans
 */
public class SQLWindow 
extends XmenuActionBase 
{

	/**
	 * 
	 */
	public SQLWindow() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		
		Connection conn = getConnection();

		String sql = getConfig();
		HashMap<String,String> paramValues = getParamValues();

		
		for (String pv : paramValues.keySet())
		{
			sql = sql.replaceAll("\\$\\{"+pv+"\\}", getParamValue(pv));
		}


		QueryWindow qf = new QueryWindow(conn, sql, isCloseConnOnExit(), QueryWindow.WindowType.JFRAME);
		qf.openTheWindow();

//				"print '11111111'\n" +
//				"exec sp_whoisw2\n" +
//				"select \"ServerName\" = @@servername\n" +
//				"select \"Current Date\" = getdate()\n" +
//				"print '222222222'\n" +
//				"select * from master..sysdatabases\n" +
//				"print '333333333'\n" +
//				"");

		
	}
}
