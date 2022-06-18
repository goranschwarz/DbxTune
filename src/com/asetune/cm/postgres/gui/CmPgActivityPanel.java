/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.postgres.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.cm.CountersModel;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
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

//		// PINK = idle in transaction (aborted)
//		if (conf != null) colorStr = conf.getProperty(getName()+".color.idle_in_transaction");
//		addHighlighter( new ColorHighlighter(new HighlightPredicate()
//		{
//			@Override
//			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
//			{
//				String status = (String) adapter.getValue(adapter.getColumnIndex("state"));
//				if ( status != null && status.equals("idle in transaction (aborted)") )
//					return true;
//				return false;
//			}
//		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as PINK if this SPID is BLOCKED by another thread
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("im_blocking_other_pids") == -1)
					return false;

				String blockingSpid = adapter.getString(adapter.getColumnIndex("im_blocked_by_pids"));
				if (StringUtil.hasValue(blockingSpid))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as RED if blocks other users from working
		if (conf != null) colorStr = conf.getProperty(getName()+".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("im_blocking_other_pids") == -1)
					return false;

				String listOfBlockedSpids = adapter.getString(adapter.getColumnIndex("im_blocking_other_pids"));
				String blockedBySessionId = adapter.getString(adapter.getColumnIndex("im_blocked_by_pids"));

				if ( StringUtil.hasValue(listOfBlockedSpids) && StringUtil.isNullOrBlank(blockedBySessionId))
					return true;
				return false;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}

}
