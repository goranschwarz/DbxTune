package com.asetune.pcs.inspection;

import com.asetune.DbxTune;
import com.asetune.Version;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;

public abstract class ObjectLookupInspectorAbstract
implements IObjectLookupInspector
{
	@Override
	public DbxConnection createConnection()
	throws Exception
	{
		boolean hasGui = DbxTune.hasGui();
		if (hasGui)
		{
			return MainFrame.getInstance().getNewConnection(Version.getAppName()+"-ObjInfoLookup");
		}
		else
		{
			// FIXME: implement this is some way...
			throw new Exception("createConnection() not yet implemented for DbxTune when there is NO GUI.");
		}
	}
}
