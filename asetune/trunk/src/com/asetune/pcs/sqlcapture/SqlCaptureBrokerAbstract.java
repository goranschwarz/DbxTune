package com.asetune.pcs.sqlcapture;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;

public abstract class SqlCaptureBrokerAbstract implements ISqlCaptureBroker
{

	@Override
	public void onConnect(DbxConnection conn)
	{
	}

	@Override
	public DbxConnection createConnection()
	throws Exception
	{
		boolean hasGui = DbxTune.hasGui();
		if (hasGui)
		{
			return MainFrame.getInstance().getNewConnection(Version.getAppName()+"-SqlCaptureBroker");
		}
		else
		{
			// FIXME: implement this is some way...
			throw new Exception("createConnection() not yet implemented for DbxTune when there is NO GUI.");
		}
	}
}
