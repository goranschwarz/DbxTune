package com.asetune.xmenu;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.asetune.utils.SwingUtils;

public class RsLogFirstTran
extends XmenuActionBase 
{
//	private static Logger _logger = Logger.getLogger(RsResume.class);

	/**
	 * 
	 */
	public RsLogFirstTran() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		final Connection conn = getConnection();
//System.out.println("doWork(): conn="+conn);

//		String rcl     = getConfig();
		String nameCol = getParamValue(0);
		String infoCol = getParamValue(1);

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

		// Parse the INFO Columns 
		if ("DSI".equals(nameCol) || "DSI EXEC".equals(nameCol))
		{
			String dsdb = infoCol.substring(infoCol.indexOf(" ")).trim().replace(".", ", ");
			
			String rcl = "sysadmin log_first_tran, "+dsdb;

			try
			{
				Statement stmnt = conn.createStatement();
				stmnt.executeUpdate(rcl);
				stmnt.close();

				String htmlMsg = 
						"<html>"
						+ "Please check the Replication Servers errorlog, where the information is recorded.<br>"
						+ "Or check the RSSD procedure: exec rs_helpexception #, v"
						+ "</html>";
				
				SwingUtils.showInfoMessage(getOwner(), "Check the errorlog or the exceptions log", htmlMsg);
			}
			catch (SQLException ex)
			{
				SwingUtils.showErrorMessage(getOwner(), "Problem", "Problems executing '"+rcl+"'.", ex);
			}
		}
		else
		{
			SwingUtils.showInfoMessage(getOwner(), "Not supported", "Resume for '"+nameCol+"' is not supported, only 'DSI' and 'DSI EXEC' threads are possible.");
			return;
		}
	}
}
