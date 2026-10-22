/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
 *
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune,
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 *
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.cm.mysql.gui;

import java.awt.Color;
import java.awt.Component;

import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

/**
 * Row colors for the MySQL 'Active Statements' (the same colors, with the same meaning, as in AseTune, SqlServerTune, PostgresTune and OracleTune, and in DbxCentral)
 * <ul>
 *   <li>ORANGE - MultiSampled: the connection was executing the same statement in the previous sample as well</li>
 *   <li>YELLOW - an open transaction, but the connection is not executing anything (Command is Sleep: it waits for the client, and holds its locks)</li>
 *   <li>PINK   - blocked by another connection</li>
 *   <li>RED    - blocks other connections, and is not blocked itself</li>
 * </ul>
 * The last one that matches wins, so they are added in that order.
 */
public class CmActiveStatementsPanel
extends TabularCntrPanel
{
	private static final long    serialVersionUID      = 1L;

	public CmActiveStatementsPanel(CountersModel cm)
	{
		super(cm);

		init();
	}

	private void init()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		String colorStr = null;

		// Mark the row as ORANGE if the connection was executing the same statement in the previous sample
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.multiSampled");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("MultiSampled") == -1)
					return false;

				String multiSampled = adapter.getString(adapter.getColumnIndex("MultiSampled"));
				return StringUtil.hasValue(multiSampled);
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// Mark the row as YELLOW if it has an open transaction, but is not executing anything (it holds its locks while the client has control)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.holdingLocksWhileIdle");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("HasOpenTxn") == -1 || adapter.getColumnIndex("Command") == -1)
					return false;

				String hasOpenTxn = adapter.getString(adapter.getColumnIndex("HasOpenTxn"));
				String command    = adapter.getString(adapter.getColumnIndex("Command"));
				return "Y".equals(hasOpenTxn) && "Sleep".equals(command);
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// Mark the row as PINK if this connection is BLOCKED by another connection
		// NOTE: 'BlockedByPid' is 0 when the connection is not blocked (there is no processlist id 0)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("BlockedByPid") == -1)
					return false;

				Object blockedByPid = adapter.getValue(adapter.getColumnIndex("BlockedByPid"));
				return blockedByPid instanceof Number && ((Number) blockedByPid).longValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as RED if it blocks other connections from working, and is not blocked itself (it is the root cause)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("BlockingOtherPids") == -1 || adapter.getColumnIndex("BlockedByPid") == -1)
					return false;

				String blockingOtherPids = adapter.getString(adapter.getColumnIndex("BlockingOtherPids"));
				Object blockedByPid      = adapter.getValue (adapter.getColumnIndex("BlockedByPid"));
				boolean isBlocked        = blockedByPid instanceof Number && ((Number) blockedByPid).longValue() > 0;

				return StringUtil.hasValue(blockingOtherPids) && ! isBlocked;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}
}
