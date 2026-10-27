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
package com.dbxtune.cm.db2.gui;

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
 * Row colors for the Db2 'Active Statements' (the same colors, with the same meaning, as in the other DbxTune's, and in DbxCentral)
 * <ul>
 *   <li>ORANGE - MULTI_SAMPLED: the connection was executing the same statement in the previous sample as well</li>
 *   <li>YELLOW - an open unit of work, but the connection is not executing anything (UOWWAIT: it waits for the client, and holds its locks)</li>
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
				if (adapter.getColumnIndex("MULTI_SAMPLED") == -1)
					return false;

				String multiSampled = adapter.getString(adapter.getColumnIndex("MULTI_SAMPLED"));
				return StringUtil.hasValue(multiSampled);
			}
		}, SwingUtils.parseColor(colorStr, Color.ORANGE), null));

		// Mark the row as YELLOW if it has an open unit of work, but is not executing anything (it holds its locks while the client has control)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.holdingLocksWhileIdle");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("UOW_STATE") == -1)
					return false;

				String uowState = adapter.getString(adapter.getColumnIndex("UOW_STATE"));
				return "UOWWAIT".equals(uowState);
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// Mark the row as PINK if this connection is BLOCKED by another connection
		// NOTE: 'BLOCKED_BY_APPL_HANDLE' is 0 when the connection is not blocked
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("BLOCKED_BY_APPL_HANDLE") == -1)
					return false;

				Object blockedBy = adapter.getValue(adapter.getColumnIndex("BLOCKED_BY_APPL_HANDLE"));
				return blockedBy instanceof Number && ((Number) blockedBy).longValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as RED if it blocks other connections from working, and is not blocked itself (it is the root cause)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("BLOCKING_OTHER_APPL_HANDLES") == -1 || adapter.getColumnIndex("BLOCKED_BY_APPL_HANDLE") == -1)
					return false;

				String blockingOthers = adapter.getString(adapter.getColumnIndex("BLOCKING_OTHER_APPL_HANDLES"));
				Object blockedBy      = adapter.getValue (adapter.getColumnIndex("BLOCKED_BY_APPL_HANDLE"));
				boolean isBlocked     = blockedBy instanceof Number && ((Number) blockedBy).longValue() > 0;

				return StringUtil.hasValue(blockingOthers) && ! isBlocked;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}
}
