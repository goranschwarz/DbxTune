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
package com.dbxtune.tools.sqlcapture;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.Hashtable;

public class XYLayout
    implements LayoutManager2, Serializable
{

	public XYLayout()
	{
		info = new Hashtable<Component, Object>();
	}

	public XYLayout(int width, int height)
	{
		info = new Hashtable<Component, Object>();
		this.width = width;
		this.height = height;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	@Override
	public String toString()
	{
		return String.valueOf(String.valueOf(new StringBuffer("XYLayout[width=").append(width).append(",height=").append(height).append("]")));
	}

	@Override
	public void addLayoutComponent(String s, Component component1)
	{
	}

	@Override
	public void removeLayoutComponent(Component component)
	{
		info.remove(component);
	}

	@Override
	public Dimension preferredLayoutSize(Container target)
	{
		return getLayoutSize(target, true);
	}

	@Override
	public Dimension minimumLayoutSize(Container target)
	{
		return getLayoutSize(target, false);
	}

	@Override
	public void layoutContainer(Container target)
	{
		Insets insets = target.getInsets();
		int count = target.getComponentCount();
		for (int i = 0; i < count; i++)
		{
			Component component = target.getComponent(i);
			if (component.isVisible())
			{
				Rectangle r = getComponentBounds(component, true);
				component.setBounds(insets.left + r.x, insets.top + r.y, r.width, r.height);
			}
		}

	}

	@Override
	public void addLayoutComponent(Component component, Object constraints)
	{
		if (constraints instanceof XYConstraints)
			info.put(component, constraints);
	}

	@Override
	public Dimension maximumLayoutSize(Container target)
	{
		return new Dimension(0x7fffffff, 0x7fffffff);
	}

	@Override
	public float getLayoutAlignmentX(Container target)
	{
		return 0.5F;
	}

	@Override
	public float getLayoutAlignmentY(Container target)
	{
		return 0.5F;
	}

	@Override
	public void invalidateLayout(Container container)
	{
	}

	Rectangle getComponentBounds(Component component, boolean doPreferred)
	{
		XYConstraints constraints = (XYConstraints) info.get(component);
		if (constraints == null)
			constraints = defaultConstraints;
		Rectangle r = new Rectangle(constraints.x, constraints.y, constraints.width, constraints.height);
		if (r.width <= 0 || r.height <= 0)
		{
			Dimension d = doPreferred ? component.getPreferredSize() : component.getMinimumSize();
			if (r.width <= 0)
				r.width = d.width;
			if (r.height <= 0)
				r.height = d.height;
		}
		return r;
	}

	Dimension getLayoutSize(Container target, boolean doPreferred)
	{
		Dimension dim = new Dimension(0, 0);
		if (width <= 0 || height <= 0)
		{
			int count = target.getComponentCount();
			for (int i = 0; i < count; i++)
			{
				Component component = target.getComponent(i);
				if (component.isVisible())
				{
					Rectangle r = getComponentBounds(component, doPreferred);
					dim.width = Math.max(dim.width, r.x + r.width);
					dim.height = Math.max(dim.height, r.y + r.height);
				}
			}

		}
		if (width > 0)
			dim.width = width;
		if (height > 0)
			dim.height = height;
		Insets insets = target.getInsets();
		dim.width += insets.left + insets.right;
		dim.height += insets.top + insets.bottom;
		return dim;
	}

	private static final long	serialVersionUID	= 200L;
	int	                       width;
	int	                       height;
	Hashtable<Component, Object> info;
	static final XYConstraints	defaultConstraints	= new XYConstraints();

}
