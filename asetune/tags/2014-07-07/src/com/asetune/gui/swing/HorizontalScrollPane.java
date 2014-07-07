package com.asetune.gui.swing;

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
