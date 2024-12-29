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
import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.lang.reflect.Field;

import javax.swing.ImageIcon;

public class HiDpiIcon extends ImageIcon
{
	private static final long serialVersionUID = 1L;

	private final ImageIcon	  twoTimesImageIcon;

	public HiDpiIcon(final Image standardImage, Image twoTimesImage)
	{
		super(standardImage);
		this.twoTimesImageIcon = new ImageIcon(twoTimesImage);
	}

	private static boolean isRetina(Component c)
	{
		GraphicsDevice device = c.getGraphicsConfiguration().getDevice();

		try
		{
			Field field = device.getClass().getDeclaredField("scale");

			if ( field == null )
			{
				return false;
			}

			field.setAccessible(true);
			Object scale = field.get(device);

			return scale instanceof Integer && (Integer) scale == 2;
		}
		catch (Exception ignore)
		{
		}
		return false;
	}

	/**
	 * @see javax.swing.ImageIcon#getIconWidth()
	 */
	@Override
	public int getIconWidth()
	{
//		return (int) (super.getIconWidth() / displayScalingfactor);
		return (int) (super.getIconWidth() * 2);
	}

	/**
	 * @see javax.swing.ImageIcon#getIconHeight()
	 */
	@Override
	public int getIconHeight()
	{
//		return (int) (super.getIconHeight() / displayScalingfactor);
		return (int) (super.getIconHeight() * 2);
	}

	@Override
	public synchronized void paintIcon(Component c, Graphics g, int x, int y)
	{
		boolean retina = c != null && isRetina(c);
System.out.println("paintIcon(): retina="+retina);
retina = true;
		if ( !retina )
		{
			super.paintIcon(c, g, x, y);
			return;
		}

		Image image = getImage();
		int width = image.getWidth(c);
		int height = image.getHeight(c);
		final Graphics2D g2d = (Graphics2D) g.create(x, y, width, height);

System.out.println("paintIcon(): image.getWidth="+width+", image.getHeight="+height);
//		g2d.scale(0.5, 0.5);
		g2d.scale(2.0, 2.0);
		g2d.drawImage(twoTimesImageIcon.getImage(), 0, 0, c);
		g2d.setComposite(new Composite()
		{
			@Override
			public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints)
			{
				return null;
			}
		});
		g2d.scale(1, 1);
		g2d.dispose();
	}
}
