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

import java.io.Serializable;

public class XYConstraints
    implements Cloneable, Serializable
{
    private static final long serialVersionUID = 7982419486259717184L;

    public XYConstraints()
	{
		this(0, 0, 0, 0);
	}

	public XYConstraints(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(int y)
	{
		this.y = y;
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
	public int hashCode()
	{
		return x ^ y * 37 ^ width * 43 ^ height * 47;
	}

	@Override
	public boolean equals(Object that)
	{
		if (that instanceof XYConstraints)
		{
			XYConstraints other = (XYConstraints) that;
			return other.x == x && other.y == y && other.width == width && other.height == height;
		}
		else
		{
			return false;
		}
	}

	@Override
	public Object clone()
	{
		return new XYConstraints(x, y, width, height);
	}

	@Override
	public String toString()
	{
		return String.valueOf(String.valueOf(new StringBuffer("XYConstraints[").append(x).append(",").append(y).append(",").append(width).append(",").append(height).append("]")));
	}

	int	x;
	int	y;
	int	width;
	int	height;
}
