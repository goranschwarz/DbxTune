package com.asetune.sql.conn.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.asetune.sql.conn.DbxConnection;

public class DbxConnectionStateInfoRs
implements DbxConnectionStateInfo
{
	private static Logger _logger = Logger.getLogger(DbxConnectionStateInfoRs.class);

	public String  _mode    = "";
	public String  _quiesce = "";
	public String  _status  = "";

	private static final String NORMAL_MODE    = "NORMAL";
	private static final String NORMAL_QUIESCE = "FALSE";
	private static final String NORMAL_STATUS  = "HEALTHY";
	
	public DbxConnectionStateInfoRs(DbxConnection conn)
	{
		refresh(conn);
	}
	
	private void refresh(DbxConnection conn)
	{
		String sql = "admin health";

		// Do the work
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				_mode    = rs.getString(1).trim();
				_quiesce = rs.getString(2).trim();
				_status  = rs.getString(3).trim();
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException sqle)
		{
			_logger.error("Error in DbxConnectionStateInfoRs.refresh()", sqle);
		}
	}

	
	
	
	
	
	@Override
	public boolean isNormalState()
	{
		return ( NORMAL_MODE.equals(_mode) && NORMAL_STATUS.equals(_status));
	}

	@Override
	public String getWaterMarkText()
	{
		if ( isNormalState() )
			return null;

		if ( ! NORMAL_MODE.equals(_mode)   ) return "In mode: "+_mode; 
		if ( "SUSPECT"    .equals(_status) ) return "RS Threads are down: status="+_status;

		return null;
	}

	@Override
	public String getStatusBarText()
	{
		String mode    = "Mode="    + _mode;
		String quiesce = "Quiesce=" + _quiesce;
		String status  = "Status="  + _status;

		if ( ! NORMAL_MODE   .equals(_mode)    ) mode    = "Mode=<b><font color=\"red\">"     + _mode    + "</font></b>";
		if ( ! NORMAL_QUIESCE.equals(_quiesce) ) quiesce = "Quiesce=<b><font color=\"blue\">" + _quiesce + "</font></b>";
		if ( ! NORMAL_STATUS .equals(_status)  ) status  = "Status=<b><font color=\"red\">"   + _status  + "</font></b>";

		String text = "<html>"
				+ mode    + ", "
				+ quiesce + ", "
				+ status  + 
				"</html>";

		return text;
	}

	@Override
	public String getStatusBarToolTipText()
	{
		String tooltip = "<html>" +
				"<table border=0 cellspacing=0 cellpadding=1>" +
				"<tr> <td>Mode:    </td> <td><b>" + _mode    + "</b> </td> </tr>" +
				"<tr> <td>Quiesce: </td> <td><b>" + _quiesce + "</b> </td> </tr>" +
				"<tr> <td>Status:  </td> <td><b>" + _status  + "</b> </td> </tr>" +
				"</table>" +
				"<hr>" + 
				"Various status for the current Replication Server." +
				"</html>";

		return tooltip;
	}
}
