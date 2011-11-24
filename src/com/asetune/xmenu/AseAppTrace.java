package com.asetune.xmenu;

import com.asetune.MonTablesDictionary;
import com.asetune.tools.AseAppTraceDialog;

public class AseAppTrace
extends XmenuActionBase 
{

	/**
	 * 
	 */
	public AseAppTrace() 
	{
		super();
	}

	/**
	 */
	public void doWork() 
	{
		String spidStr  = getParamValue(0);
		int    spid     = Integer.parseInt(spidStr);

		String srvVerStr = MonTablesDictionary.getInstance().aseVersionStr;
		String srvName   = MonTablesDictionary.getInstance().aseServerName;

		AseAppTraceDialog apptrace = new AseAppTraceDialog(spid, srvName, srvVerStr);
		apptrace.setVisible(true);
	}
}
