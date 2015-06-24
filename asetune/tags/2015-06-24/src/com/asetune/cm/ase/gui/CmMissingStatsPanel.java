package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmMissingStatsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmMissingStatsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmMissingStats.CM_NAME;

	public CmMissingStatsPanel(CountersModel cm)
	{
		super(cm);

//		if (cm.getIconFile() != null)
//			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// PINK = NO INDEX in string
		if (conf != null) colorStr = conf.getProperty(getName()+".color.noindex");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.convertColumnIndexToModel(adapter.column);
				if ("colList".equals(adapter.getColumnName(modelCol)))
				{
					String cellVal = adapter.getString();
					if (cellVal != null && cellVal.indexOf("(not indexed!)") >= 0)
						return true;
				}
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));
	}

}
