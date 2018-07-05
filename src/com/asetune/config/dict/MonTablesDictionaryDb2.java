/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dict;

import com.asetune.sql.conn.DbxConnection;


public class MonTablesDictionaryDb2
extends MonTablesDictionaryDefault
{
    /** Log4j logging. */
//	private static Logger _logger = Logger.getLogger(MonTablesDictionaryDb2.class);

	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		// TODO Auto-generated method stub
		super.initialize(conn, hasGui);

		initExtraMonTablesDictionary();
	}

	
	/**
	 * NO, do not save MonTableDictionary in PCS
	 */
	@Override
	public boolean isSaveMonTablesDictionaryInPcsEnabled()
	{
		return false;
	}
	
	/**
	 * Add some information to the MonTablesDictionary<br>
	 * This will serv as a dictionary for ToolTip
	 */
	public static void initExtraMonTablesDictionary()
	{
//		try
//		{
//			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
//			
//			if (mtd == null)
//				return;
//
//			// NOTE: the below information on all MON_* tables are copied from: http://xxxx
//
//			//---------------------------------------------------------------------------------------------------------------
//			// tablename
//			//---------------------------------------------------------------------------------------------------------------
//			mtd.addTable("t1",            "describeme");
//
//			mtd.addColumn("t1", "c1",            "describeme");
//			mtd.addColumn("t1", "c2",            "describeme");
//
//			
//
//			//---------------------------------------------------------------------------------------------------------------
//			// tablename
//			//---------------------------------------------------------------------------------------------------------------
//			mtd.addTable("t2",            "describeme");
//
//			mtd.addColumn("t2", "c1",            "describeme");
//			mtd.addColumn("t2", "c2",            "describeme");
//
//		
//		}
//		catch (NameNotFoundException e)
//		{
//			/* ignore */
//		}
	}
}
