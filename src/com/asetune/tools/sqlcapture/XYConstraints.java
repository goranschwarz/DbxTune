package com.asetune.tools.sqlcapture;

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
