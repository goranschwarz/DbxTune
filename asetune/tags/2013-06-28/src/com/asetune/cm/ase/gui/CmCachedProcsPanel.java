package com.asetune.cm.ase.gui;

import java.awt.Component;

import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;

import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.SwingUtils;

public class CmCachedProcsPanel
extends TabularCntrPanel
{
//	private static final Logger  _logger	           = Logger.getLogger(CmCachedProcsPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmCachedProcsPanel.CM_NAME;

	public CmCachedProcsPanel(CountersModel cm)
	{
		super(cm);

		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		// PROCEDURE ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
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
					
		// TRIGGER ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
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

		// VIEW ICON
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
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

		// DEFAULT VALUE icon
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("default value spec"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_default_value.png")));

		// RULE icon
		addHighlighter( new IconHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				int modelCol = adapter.getColumnIndex("ObjectType");
				if (modelCol == adapter.convertColumnIndexToModel(adapter.column))
				{
					String objectType = adapter.getString(modelCol);
					if (objectType != null)
						objectType = objectType.trim();
					if ( objectType.startsWith("rule"))
						return true;
				}
				return false;
			}
		}, SwingUtils.readImageIcon(Version.class, "images/highlighter_rule.png")));
	}
}
