package com.asetune.cm.postgres.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmPgActivityPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmPgActivityPanel.class);
	private static final long    serialVersionUID      = 1L;

	public CmPgActivityPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// WHITE = idle : do nothing

		// GREEN = active
		if (conf != null) colorStr = conf.getProperty(getName()+".color.active");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
//				if ( status != null && (status.startsWith("running") || status.startsWith("runnable")) )
//					return true;
				if ( status != null && status.equals("active") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

		// YELLOW = idle in transaction
		if (conf != null) colorStr = conf.getProperty(getName()+".color.idle_in_transaction");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
				if ( status != null && status.equals("idle in transaction") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// PINK = idle in transaction (aborted)
		if (conf != null) colorStr = conf.getProperty(getName()+".color.idle_in_transaction");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
				if ( status != null && status.equals("idle in transaction (aborted)") )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));
	}

}
