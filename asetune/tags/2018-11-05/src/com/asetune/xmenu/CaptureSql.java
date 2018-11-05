/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.xmenu;

import com.asetune.tools.sqlcapture.ProcessDetailFrame;

public class CaptureSql
extends XmenuActionBase 
{
	/**
	 * 
	 */
	public CaptureSql() 
	{
		super();
	}

	/* (non-Javadoc)
	 * @see com.sybase.jisql.xmenu.XmenuActionBase#doWork()
	 */
	@Override 
	public void doWork() 
	{
//		String kpidStr  = getParamValue(0);
//		int    kpid     = Integer.parseInt(kpidStr);
//
//		new ProcessDetailFrame(kpid);

		String spidStr  = getParamValue(0);
		int    spid     = Integer.parseInt(spidStr);

		new ProcessDetailFrame(getConnectionProvider(), spid, -1);
	}
}
