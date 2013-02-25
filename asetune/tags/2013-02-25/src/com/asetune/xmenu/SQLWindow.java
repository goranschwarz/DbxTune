/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import java.sql.Connection;
import java.util.HashMap;

import com.asetune.tools.QueryWindow;
import com.asetune.tools.WindowType;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;


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

		QueryWindow qf = new QueryWindow(conn, sql, null, isCloseConnOnExit(), WindowType.JFRAME, getConfiguration());
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
