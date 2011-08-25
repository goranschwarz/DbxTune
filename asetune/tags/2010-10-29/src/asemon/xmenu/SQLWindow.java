/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.xmenu;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;

import asemon.gui.QueryWindow;

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
	public void doWork() 
	{
		
		Connection conn = getConnection();

		String sql = getConfig();
		HashMap paramValues = getParamValues();

		
		for (Iterator it=paramValues.keySet().iterator(); it.hasNext();)
		{
			String pv = (String) it.next();
			sql = sql.replaceAll("\\$\\{"+pv+"\\}", getParamValue(pv));
		}


		QueryWindow qf = new QueryWindow(conn, sql, isCloseConnOnExit());
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
