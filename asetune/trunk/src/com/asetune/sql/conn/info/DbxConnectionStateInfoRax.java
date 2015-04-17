package com.asetune.sql.conn.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.StringUtil;

public class DbxConnectionStateInfoRax
implements DbxConnectionStateInfo
{
	private static Logger _logger = Logger.getLogger(DbxConnectionStateInfoRax.class);

	public String  _state  = "";
	public String  _action = "";

	private static final String NORMAL_STATE = "REPLICATING";
	
	public DbxConnectionStateInfoRax(DbxConnection conn)
	{
		refresh(conn);
	}
	
	private void refresh(DbxConnection conn)
	{
		String sql = "ra_status";

		// Do the work
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				_state  = rs.getString(1).trim();
				_action = rs.getString(2).trim();
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			_logger.error("Error in DbxConnectionStateInfoRax.refresh()", sqle);
		}
	}

	
	
	
	
	
	@Override
	public boolean isNormalState()
	{
		if ( StringUtil.isNullOrBlank(_state) ) return true; 
		if ( NORMAL_STATE.equals(_state) ) return true; 
		return false;
	}

	@Override
	public String getWaterMarkText()
	{
		if ( isNormalState() )
			return null;

		String str = "NOTE: in state '"+_state+"'\n"
		           + _action;
		return str;
	}

	@Override
	public String getStatusBarText()
	{
		String state  = "State="   + _state;
		String action = "Action="  + _action;

		if ( ! isNormalState() )
			state = "State=<b><font color=\"red\">" + _state + "</font></b>";

		String text = "<html>"
				+ state  + ", "
				+ action + 
				"</html>";

		return text;
	}

	@Override
	public String getStatusBarToolTipText()
	{
		String tooltip = "<html>" +
				"<table border=0 cellspacing=0 cellpadding=1>" +
				"<tr> <td>State:  </td> <td><b>" + _state  + "</b> </td> </tr>" +
				"<tr> <td>Action: </td> <td><b>" + _action + "</b> </td> </tr>" +
				"</table>" +
				"<hr>" + 
				"Various status for the current connection." +
				"</html>";

		return tooltip;
	}
}
