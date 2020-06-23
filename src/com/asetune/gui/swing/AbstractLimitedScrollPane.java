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
package com.asetune.gui.swing;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * Abstract base class for the {@link HorizontalScrollPane} and
 * {@link VerticalScrollPane}
 */
public abstract class AbstractLimitedScrollPane extends JScrollPane
{
	private static final long serialVersionUID = 1L;

	private class Viewport extends JViewport
	{
		private static final long	serialVersionUID	= 1L;

		public Viewport()
		{
			super();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void setViewSize(Dimension newSize)
		{
			final JScrollPane scrollPane = (JScrollPane) getParent();
			Dimension currentPreferredSize = scrollPane.getPreferredSize();

			fixViewSize(newSize);

			final Dimension expectedPreferredSize = getExpectedPreferredSize();

			if ( isUpdatePreferredSizeNeeded(currentPreferredSize, expectedPreferredSize) )
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						scrollPane.setPreferredSize(expectedPreferredSize);
						scrollPane.validate();
					}
				});
			}

			super.setViewSize(newSize);
		}

	}

	public AbstractLimitedScrollPane()
	{
		super();
	}

	public AbstractLimitedScrollPane(Component view, int vsbPolicy, int hsbPolicy)
	{
		super(view, vsbPolicy, hsbPolicy);
	}

	public AbstractLimitedScrollPane(Component view)
	{
		super(view);
	}

	public AbstractLimitedScrollPane(int vsbPolicy, int hsbPolicy)
	{
		super(vsbPolicy, hsbPolicy);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JViewport createViewport()
	{
		return new Viewport();
	}

	protected abstract boolean isUpdatePreferredSizeNeeded(Dimension currentPreferredSize, Dimension expectedPreferredSize);

	protected abstract void fixViewSize(Dimension newSize);

	public Dimension getExpectedPreferredSize()
	{
		final Dimension expectedPreferredSize = getViewport().getLayout().preferredLayoutSize(getViewport());

		if ( getHorizontalScrollBar().isVisible() )
		{
			expectedPreferredSize.height += getHorizontalScrollBar().getHeight();
		}

		if ( getVerticalScrollBar().isVisible() )
		{
			expectedPreferredSize.width += getVerticalScrollBar().getWidth();
		}
		return expectedPreferredSize;
	}
}
