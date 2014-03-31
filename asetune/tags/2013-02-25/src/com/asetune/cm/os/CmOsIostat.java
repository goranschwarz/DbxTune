package com.asetune.cm.os;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsIostatPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmOsIostat
extends CounterModelHostMonitor
{
//	private static Logger        _logger          = Logger.getLogger(CmOsIostat.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_IOSTAT;
	public static final String   CM_NAME          = CmOsIostat.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Disk Stat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'iostat' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
//	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmOsIostat(counterController, guiController);
	}

	public CmOsIostat(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, true);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsIostatPanel(this);
	}
}