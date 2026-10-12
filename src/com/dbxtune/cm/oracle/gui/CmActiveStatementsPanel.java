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
package com.dbxtune.cm.oracle.gui;

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
 * Row colors for the Oracle 'Active Statements' (the same colors, with the same meaning, as in AseTune, SqlServerTune and PostgresTune, and in DbxCentral)
 * <ul>
 *   <li>ORANGE - MultiSampled: the session was executing the same statement in the previous sample as well</li>
 *   <li>YELLOW - an open transaction, but the session is not executing anything (it waits for the client, and holds its locks)</li>
 *   <li>PINK   - blocked by another session</li>
 *   <li>RED    - blocks other sessions, and is not blocked itself</li>
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

		// Mark the row as ORANGE if the session was executing the same statement in the previous sample
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
				if (adapter.getColumnIndex("HasOpenTxn") == -1 || adapter.getColumnIndex("STATUS") == -1)
					return false;

				String hasOpenTxn = adapter.getString(adapter.getColumnIndex("HasOpenTxn"));
				String status     = adapter.getString(adapter.getColumnIndex("STATUS"));
				return "Y".equals(hasOpenTxn) && ! "ACTIVE".equals(status);
			}
		}, SwingUtils.parseColor(colorStr, Color.YELLOW), null));

		// Mark the row as PINK if this session is BLOCKED by another session
		// NOTE: 'BlockedBySid' is 0 when the session is not blocked (a NULL number is read as 0, and there is no SID 0)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.blocked");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("BlockedBySid") == -1)
					return false;

				Object blockedBySid = adapter.getValue(adapter.getColumnIndex("BlockedBySid"));
				return blockedBySid instanceof Number && ((Number) blockedBySid).longValue() > 0;
			}
		}, SwingUtils.parseColor(colorStr, Color.PINK), null));

		// Mark the row as RED if it blocks other sessions from working, and is not blocked itself (it is the root cause)
		if (conf != null) colorStr = conf.getProperty(getName() + ".color.blocking");
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				if (adapter.getColumnIndex("BlockingOtherSids") == -1 || adapter.getColumnIndex("BlockedBySid") == -1)
					return false;

				String blockingOtherSids = adapter.getString(adapter.getColumnIndex("BlockingOtherSids"));
				Object blockedBySid      = adapter.getValue (adapter.getColumnIndex("BlockedBySid"));
				boolean isBlocked        = blockedBySid instanceof Number && ((Number) blockedBySid).longValue() > 0;

				return StringUtil.hasValue(blockingOtherSids) && ! isBlocked;
			}
		}, SwingUtils.parseColor(colorStr, Color.RED), null));
	}
}
