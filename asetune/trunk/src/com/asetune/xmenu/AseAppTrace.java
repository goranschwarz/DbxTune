package com.asetune.xmenu;

import com.asetune.config.dict.MonTablesDictionary;
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
	@Override 
	public void doWork() 
	{
		String spidStr  = getParamValue(0);
		int    spid     = Integer.parseInt(spidStr);

		String srvVerStr = MonTablesDictionary.getInstance().getAseExecutableVersionStr();
		String srvName   = MonTablesDictionary.getInstance().getAseServerName();

		AseAppTraceDialog apptrace = new AseAppTraceDialog(spid, srvName, srvVerStr);
		apptrace.setVisible(true);
	}
}
