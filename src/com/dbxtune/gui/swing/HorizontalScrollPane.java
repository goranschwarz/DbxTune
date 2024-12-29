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
 * A {@link JScrollPane} that only scrolls horizontally. In contrast to a
 * default {@link JScrollPane}, it sets the preferred size of the component to
 * fit its height, instead of just hiding the vertical scrollbar. When showing
 * the horizontal scrollbar, it tries to enlarge its own size to keep the size
 * of the viewport (this works, for example, in the north or south part of a
 * BorderLayout).
 */
public class HorizontalScrollPane extends AbstractLimitedScrollPane
{
	private static final long serialVersionUID = 1L;

	public HorizontalScrollPane()
	{
		this(HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	public HorizontalScrollPane(Component view, int hsbPolicy)
	{
		super(view, VERTICAL_SCROLLBAR_NEVER, hsbPolicy);
	}

	public HorizontalScrollPane(Component view)
	{
		this(view, HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	public HorizontalScrollPane(int hsbPolicy)
	{
		super(VERTICAL_SCROLLBAR_NEVER, hsbPolicy);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isUpdatePreferredSizeNeeded(Dimension currentPreferredSize, Dimension expectedPreferredSize)
	{
		return currentPreferredSize.height != expectedPreferredSize.height;
	}

	@Override
	protected void fixViewSize(Dimension newSize)
	{
		newSize.height = getViewportBorderBounds().height;
	}
}
