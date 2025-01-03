/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.ase.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.IconHighlighter;

import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

public class CmProcCallStackPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmProcCallStack.CM_NAME;

	public CmProcCallStackPanel(CountersModel cm)
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

		// Mark the row as GREEN if it's the EXECUTING procedure
		if (conf != null) colorStr = conf.getProperty(getName()+".color.executing");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
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
					
		// Trigger ICON
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

		// View ICON
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
	}

}
