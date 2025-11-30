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
package com.dbxtune.gui.swing;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JScrollPane;

/**
 * A {@link JScrollPane} that only scrolls vertically. In contrast to a default
 * {@link JScrollPane}, it sets the preferred size of the component to fit its
 * width, instead of just hiding the horizontal scrollbar. When showing the
 * vertical scrollbar, it tries to enlarge its own size to keep the size of the
 * viewport (this works, for example, in the east or west part of a
 * BorderLayout).
 */
public class VerticalScrollPane extends AbstractLimitedScrollPane
{
	private static final long serialVersionUID = 1L;

	public VerticalScrollPane()
	{
		this(VERTICAL_SCROLLBAR_AS_NEEDED);
	}

	public VerticalScrollPane(Component view, int vsbPolicy)
	{
		super(view, vsbPolicy, HORIZONTAL_SCROLLBAR_NEVER);
	}

	public VerticalScrollPane(Component view)
	{
		this(view, VERTICAL_SCROLLBAR_AS_NEEDED);
	}

	public VerticalScrollPane(int vsbPolicy)
	{
		super(vsbPolicy, HORIZONTAL_SCROLLBAR_NEVER);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isUpdatePreferredSizeNeeded(Dimension currentPreferredSize, Dimension expectedPreferredSize)
	{
		return currentPreferredSize.width != expectedPreferredSize.width;
	}

	@Override
	protected void fixViewSize(Dimension newSize)
	{
		newSize.width = getViewportBorderBounds().width;
	}
}
