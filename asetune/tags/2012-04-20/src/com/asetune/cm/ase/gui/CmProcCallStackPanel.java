package com.asetune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

public class CmProcCallStackPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmProcCallStackPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmProcCallStack.CM_NAME;

	public CmProcCallStackPanel(CountersModel cm)
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

		// Mark the row as GREEN if it's the EXECUTING procedure
		if (conf != null) colorStr = conf.getProperty(getName()+".color.executing");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String maxContextID = adapter.getString(adapter.getColumnIndex("MaxContextID"));
				String contextID    = adapter.getString(adapter.getColumnIndex("ContextID"));
				if (maxContextID != null) maxContextID = maxContextID.trim();
				if (contextID    != null) contextID    = contextID   .trim();
				if ( maxContextID != null && contextID != null && maxContextID.equals(contextID))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.GREEN), null));

		// Procedure ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("stored procedure"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_procedure.png")));
					
		// Trigger ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("trigger"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_trigger.png")));

		// View ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("view"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_view.png")));
	}

}
