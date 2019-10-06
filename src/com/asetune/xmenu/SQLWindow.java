/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.util.HashMap;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.tools.WindowType;
import com.asetune.tools.sqlw.QueryWindow;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;


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
	 * Decide if the Caller should create a new connection for us or just pass the connection the caller was using.<br>
	 * Lets ALWAYS just REUSE the callers connection, if we need a new connection we will create one ourself from the ConnectionProvider.
	 * 
	 */
	@Override 
	public boolean createConnectionOnStart()
	{
		return false;
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
//		Connection conn = getConnection();
		DbxConnection conn = getConnection();

		String sql = getConfig();
		HashMap<String,String> paramValues = getParamValues();

		
		// if we have aliases (1 parameter with multiple "names") for a parameter, then the FIRST name in the alias list will be used as a parameter name
		for (String pv : paramValues.keySet())
		{
//			sql = sql.replaceAll("\\$\\{"+pv+"\\}", getParamValue(pv));
			sql = sql.replace("${"+pv+"}", getParamValue(pv));
			
			// System.out.println("xxxxxxxxxxxxxxxx: key='"+pv+"', value='"+getParamValue(pv)+"'.");
		}

		// NOTE:
		// if the tag: <getOwner(DBNameStr,ObjectNameStr)> exists, then lookup and grab the first 
		// owner we find for a objects, if there are more than one table, with different owners, 
		// just grab the first one.
		if (sql.indexOf("<getOwner(") >= 0)
		{
			String dbname  = null;
			String objname = null;

			int startPos = sql.indexOf("<getOwner(") + "<getOwner(".length();
			int endPos   = sql.indexOf(")>");
			if (endPos >= 0)
			{
				String tagData = sql.substring(startPos, endPos);
//System.out.println("tagData='"+tagData+"'.");
				String sa[] = tagData.split(",");
				if (sa.length < 2)
				{
					// to few parameters... what to do?
				}
				else
				{
					dbname  = sa[0].trim();
					objname = sa[1].trim();
	
					String owner = AseConnectionUtils.getObjectOwner(conn, dbname, objname);
					
					// finally replace "<getOwner(...)>" with the object owner
					sql = sql.replaceAll("<getOwner(.*)>", owner);
				}
			}
			else
			{
				// Can't find the end... what to do?
			}
		}
		
		sql = modifySql(sql);

		// TODO: figure out if this was called from SQLWindow or from AseTune (or possible from what JTable sub type)
		//       if called from AseTune   open new Window
		//       if called from SQLWindow execute the statement in current window
		// can we use a stacktrace to see from where it's called?
		// or do we need to add some parameter ???
		ConnectionProvider connProvider = getConnectionProvider();
		if (connProvider instanceof QueryWindow)
		{
			QueryWindow qw = (QueryWindow)connProvider;
			qw.displayQueryResults(sql, 0, false, true);
		}
		else
		{
			boolean closeConnOnExit = true;
			DbxConnection newConn = getConnectionProvider().getNewConnection("QueryWindow");
//			QueryWindow qf = new QueryWindow(conn, sql, null, isCloseConnOnExit(), WindowType.JFRAME, getConfiguration());
			QueryWindow qf = new QueryWindow(newConn, sql, null, closeConnOnExit, WindowType.JFRAME, getConfiguration());
			qf.openTheWindow();
			
		}
//		else if (connProvider instanceof MainFrame)
//		{
//			QueryWindow qf = new QueryWindow(conn, sql, null, isCloseConnOnExit(), WindowType.JFRAME, getConfiguration());
//			qf.openTheWindow();
//		}
//		else
//		{
//			SwingUtils.showInfoMessage(null, "Sorry not from here", "This functionality is not available from the Component '"+connProvider.getClass().getSimpleName()+"'.");
//			return;
//		}


//				"print '11111111'\n" +
//				"exec sp_whoisw2\n" +
//				"select \"ServerName\" = @@servername\n" +
//				"select \"Current Date\" = getdate()\n" +
//				"print '222222222'\n" +
//				"select * from master..sysdatabases\n" +
//				"print '333333333'\n" +
//				"");

		
	}

	/** 
	 * Hook to modify SQL before execution, implemented by subclasses... 
	 * 
	 * @param the SQL that is about to be executed
	 * @return the new and modified SQL statement
	 */
	protected String modifySql(String sql)
	{
		return sql;
	}
	
	/**
	 * Get Properties/Configuration passed to QueryWindow Object
	 * @return
	 */
	protected Configuration getConfiguration()
	{
		return null;
	}
}
