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
package com.dbxtune.cm.sqlserver.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

public class CmWaitingTasksPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmWaitingTasksPanel.CM_NAME;

	public CmWaitingTasksPanel(CountersModel cm)
	{
		super(cm);

		init();
	}
	
	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// PINK = spid is BLOCKED by some other user
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Number blocking_session_id = (Number) adapter.getValue(adapter.getColumnIndex("blocking_session_id"));
				if ( blocking_session_id != null && blocking_session_id.intValue() != 0 )
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

//		// RED = spid is BLOCKING other spids from running
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number BlockingOtherSpidCount = (Number) adapter.getValue(adapter.getColumnIndex("BlockingOtherSpidCount"));
//				if ( BlockingOtherSpidCount != null && BlockingOtherSpidCount.intValue() > 0 )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.RED), null));

//		// GREEN = running
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.running");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				Number xxx = (Number) adapter.getValue(adapter.getColumnIndex("xxx"));
//				if ( xxx != null && xx.intValue() > 0 )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}
}
