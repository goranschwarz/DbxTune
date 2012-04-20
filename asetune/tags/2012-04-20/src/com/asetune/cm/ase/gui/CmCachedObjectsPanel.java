package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmCachedObjectsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmCachedObjectsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmCachedObjects.CM_NAME;

	public CmCachedObjectsPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// ORANGE = Index id > 0
		if (conf != null) colorStr = conf.getProperty(getName()+".color.index");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number indexId = (Number) adapter.getValue(adapter.getColumnIndex("IndexID"));
				if ( indexId != null && indexId.intValue() > 0)
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));
	}

}
