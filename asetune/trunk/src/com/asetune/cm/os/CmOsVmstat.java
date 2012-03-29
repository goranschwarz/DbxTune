package com.asetune.cm.os;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsVmstatPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

public class CmOsVmstat
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsVmstat.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_VMSTAT;
	public static final String   CM_NAME          = CmOsVmstat.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS CPU(vmstat)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'vmstat' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmOsVmstat(counterController, guiController);
	}

	public CmOsVmstat(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, true);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsVmstatPanel(this);
	}
}
