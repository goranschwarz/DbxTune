package com.asetune.xmenu;

import java.sql.Connection;

import com.asetune.tools.WindowType;
import com.asetune.tools.sqlw.RsDumpQueueDialog;
import com.asetune.utils.SwingUtils;

public class RsQueueContent
extends XmenuActionBase 
{
//	private static Logger _logger = Logger.getLogger(RsQueueContent.class);

	/**
	 * 
	 */
	public RsQueueContent() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		
		final Connection conn = getConnection();

		String nameCol = getParamValue(0);
		String infoCol = getParamValue(1);

//		SwingUtils.showInfoMessage(null, "Not yet implemented", "Not yet implemented.");

//		+----+---------+---------+-------------------------------+
//		|Spid|Name     |State    |Info                           |
//		+----+---------+---------+-------------------------------+
//		|    |DSI EXEC |Suspended|122(1) GORAN_1_DS.wsdb1        |
//		|    |DSI      |Suspended|122 GORAN_1_DS.wsdb1           |
//		|    |REP AGENT|Down     |GORAN_2_DS.wsdb2               |
//		|    |DSI EXEC |Down     |116(1) REP_CONNECTOR_1.dest1   |
//		|    |DSI EXEC |Down     |118(1) REP_CONNECTOR_HPUX.dest1|
//		|    |DSI EXEC |Down     |115(1) XE.XE                   |
//		+----+---------+---------+-------------------------------+

		String dsdb      = null;
		int    dbid      = -1;
		int    queueType = -1;
		// Parse the INFO Columns 
		if ("DSI".equals(nameCol))
		{
			try 
			{ 
				dbid = Integer.parseInt( infoCol.substring(0, infoCol.indexOf(" ")).trim() ); 
			}
			catch (NumberFormatException nfe) {}

			dsdb = infoCol.substring(infoCol.indexOf(" ")).trim();
			queueType = 0;
		}
		else if ("DSI EXEC".equals(nameCol))
		{
			try 
			{ 
				//  grab first word "122(1)" from "122(1) GORAN_1_DS.wsdb1"
				// remove ()
				String word = infoCol.substring(0, infoCol.indexOf(" ")).trim(); 
				word = word.replace('(', ' '); 
				word = word.replace(')', ' '); 

				String dbidStr = word.substring(0, word.indexOf(" ")).trim(); 
//				String qInOut  = word.substring(infoCol.indexOf(" ")).trim(); 

				dbid      = Integer.parseInt( dbidStr ); 
//				queueType = Integer.parseInt( qInOut ); 
				queueType = 0;
			}
			catch (NumberFormatException nfe) {}

			dsdb = infoCol.substring(infoCol.indexOf(" ")).trim();
		}
		else if ("REP AGENT".equals(nameCol))
		{
			dsdb = infoCol;
			queueType = 1;
		}
//		else if ("RSI".equals(nameCol))
//		{
//			queueType = 0;
//		}
		else
		{
			SwingUtils.showInfoMessage(null, "Not yet implemented", "Dump Queue for '"+nameCol+"' can't be done or not yet implemented.");
			return;
		}
		
		RsDumpQueueDialog dumpQueueDialog = new RsDumpQueueDialog(conn, WindowType.JFRAME);
		boolean show = false;
		if (dsdb != null)
			show = dumpQueueDialog.setDsDbName(dsdb);
		if (dbid >= 0)
			show = dumpQueueDialog.setDbId(dbid);
		if (queueType >= 0)
			dumpQueueDialog.setQueueType(queueType);

		if (show)
			dumpQueueDialog.readQueue();
			
		dumpQueueDialog.setVisible(true);
	}
}
