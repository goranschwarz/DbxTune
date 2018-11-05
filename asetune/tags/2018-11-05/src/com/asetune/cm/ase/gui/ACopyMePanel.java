package com.asetune.cm.ase.gui;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;

public class ACopyMePanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(ACopyMePanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = ACopyMe.CM_NAME;

	public ACopyMePanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
//		Configuration conf = Configuration.getCombinedConfiguration();
//		String colorStr = null;

	}

}
