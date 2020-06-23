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
package com.asetune.utils;

import java.awt.Color;

/**
 */
@SuppressWarnings("serial")
public class ColorUtils extends Color
{
	public ColorUtils(int r, int g, int b)
	{
		super(r, g, b);
	}

	public static final Color DEBUG              = Color.GRAY;
	public static final Color INFO               = Color.BLACK;
	public static final Color WARNING            = new Color(239, 169, 64);    // DARK YELLOW/ORANGE
	public static final Color ERROR              = Color.RED;

	/** A very dark red color. */
	public static final Color VERY_DARK_RED      = new Color(0x80, 0x00, 0x00);

	/** A dark red color. */
	public static final Color DARK_RED           = new Color(0xc0, 0x00, 0x00);

	/** A light red color. */
	public static final Color LIGHT_RED          = new Color(0xFF, 0x40, 0x40);

	/** A very light red color. */
	public static final Color VERY_LIGHT_RED     = new Color(0xFF, 0x80, 0x80);

	/** A very dark yellow color. */
	public static final Color VERY_DARK_YELLOW   = new Color(0x80, 0x80, 0x00);

	/** A dark yellow color. */
	public static final Color DARK_YELLOW        = new Color(0xC0, 0xC0, 0x00);

	/** A light yellow color. */
	public static final Color LIGHT_YELLOW       = new Color(0xFF, 0xFF, 0x40);

	/** A very light yellow color. */
	public static final Color VERY_LIGHT_YELLOW  = new Color(0xFF, 0xFF, 0x80);

	/** A very dark green color. */
	public static final Color VERY_DARK_GREEN    = new Color(0x00, 0x80, 0x00);

	/** A dark green color. */
	public static final Color DARK_GREEN         = new Color(0x00, 0xC0, 0x00);

	/** A light green color. */
	public static final Color LIGHT_GREEN        = new Color(0x40, 0xFF, 0x40);

	/** A very light green color. */
	public static final Color VERY_LIGHT_GREEN   = new Color(0x80, 0xFF, 0x80);

	/** A very dark cyan color. */
	public static final Color VERY_DARK_CYAN     = new Color(0x00, 0x80, 0x80);

	/** A dark cyan color. */
	public static final Color DARK_CYAN          = new Color(0x00, 0xC0, 0xC0);

	/** A light cyan color. */
	public static final Color LIGHT_CYAN         = new Color(0x40, 0xFF, 0xFF);

	/** Aa very light cyan color. */
	public static final Color VERY_LIGHT_CYAN    = new Color(0x80, 0xFF, 0xFF);

	/** A very dark blue color. */
	public static final Color VERY_DARK_BLUE     = new Color(0x00, 0x00, 0x80);

	/** A dark blue color. */
	public static final Color DARK_BLUE          = new Color(0x00, 0x00, 0xC0);

	/** A light blue color. */
	public static final Color LIGHT_BLUE         = new Color(0x40, 0x40, 0xFF);

	/** A very light blue color. */
	public static final Color VERY_LIGHT_BLUE    = new Color(0x80, 0x80, 0xFF);

	/** A very dark magenta/purple color. */
	public static final Color VERY_DARK_MAGENTA  = new Color(0x80, 0x00, 0x80);

	/** A dark magenta color. */
	public static final Color DARK_MAGENTA       = new Color(0xC0, 0x00, 0xC0);

	/** A light magenta color. */
	public static final Color LIGHT_MAGENTA      = new Color(0xFF, 0x40, 0xFF);

	/** A very light magenta color. */
	public static final Color VERY_LIGHT_MAGENTA = new Color(0xFF, 0x80, 0xFF);
}
