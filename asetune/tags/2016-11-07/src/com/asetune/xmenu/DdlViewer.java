package com.asetune.xmenu;

import com.asetune.gui.MainFrame;

public class DdlViewer
extends XmenuActionBase 
{

	/**
	 * 
	 */
	public DdlViewer() 
	{
		super();
	}

	/**
	 */
	@Override 
	public void doWork() 
	{
		String dbname     = getParamValue(0);
		String objectname = getParamValue(1);

		MainFrame.getInstance().action_openDdlViewer(dbname, objectname);
	}

	@Override 
	public boolean createConnectionOnStart()
	{
		return false;
	}
}
