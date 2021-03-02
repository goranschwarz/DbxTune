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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.log4j.Logger;

import com.asetune.utils.SwingUtils;


public class Screenshot
{
	private static Logger _logger = Logger.getLogger(Screenshot.class);
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param window
	 * @param dir           Directory where to store the file
	 * @param baseFilename  name of the file, without the .JPG file ending
	 * @param doTimeStamp   add a "_YYYY-MM-DD.HH-MM-SS" to the end of base filename
	 * @param extraInfo     Write some extra info in the top right corner of the screen shot
	 * @return filename on success, null of failure
	 */
//	public static String windowScreenshot(Window window, String dir, String baseFilename, boolean doTimeStamp, String extraInfo)
	public static String windowScreenshot(Component window, String dir, String baseFilename, boolean doTimeStamp, String extraInfo)
	{
		String fileSeparator = System.getProperty("file.separator");
		String filename      = baseFilename;

		// Get a directory where to store the file
		if (dir == null)
		{
			dir = System.getProperty("user.home");
		}
		if ( ! dir.endsWith(fileSeparator))
			dir += fileSeparator;

		if (doTimeStamp)
		{
			Date timeNow = new Date(System.currentTimeMillis());
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss").format(timeNow);
			
			filename += "_" + timeStamp;
		}
		filename += ".jpg";

		filename = dir + filename;

		if (windowScreenshot(window, filename, extraInfo))
			return filename;
		else
			return null;
	}

	
//	public static boolean windowScreenshot(Window window, String filename, String extraInfo)
	public static boolean windowScreenshot(Component window, String filename, String extraInfo)
	{
		if ( window == null )
			throw new IllegalArgumentException("Window can't be null");
		if ( filename == null )
			throw new IllegalArgumentException("Filename can't be null");

		// Get File
		File file = new File(filename);
		_logger.info("Saving screen capture to file '"+file+"'.");
		if ( file.exists() )
		{
			_logger.info("The file '"+file+"' exists and will be overwritten.");
			file.delete();
		}

		// Get Writer
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
		ImageWriter writer = writers.next();
		if ( writer == null )
		{
			_logger.error("Problems creating a writer for file '"+file+"'.");
			return false;
		}

		// Get Image
		BufferedImage bi = capture(window);

		// Add "watermark"
		if (extraInfo != null)
		{
			Graphics2D g2d = (Graphics2D) bi.getGraphics();
			g2d.setColor(Color.GRAY);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			g2d.setFont(new Font("Arial", Font.BOLD, SwingUtils.hiDpiScale(20)));

			FontMetrics fontMetrics = g2d.getFontMetrics();
			Rectangle2D rect = fontMetrics.getStringBounds(extraInfo, g2d);

//			int posX = (bi.getWidth() - (int) rect.getWidth()) / 2;
//			int posY = (bi.getHeight() - (int) rect.getHeight()) / 2;
			int posX = bi.getWidth()  - (int) rect.getWidth() - 10;
			int posY = 3 + (int) rect.getHeight(); //bi.getHeight() - (int) rect.getHeight();

			g2d.drawString(extraInfo, posX, posY);

			//Free graphic resources
			g2d.dispose();		
		}

		// Save the file
		try
		{
			ImageOutputStream ios = ImageIO.createImageOutputStream(file);
			writer.setOutput(ios);
			writer.write(bi);
			ios.flush();
			ios.close();
		}
		catch (IOException e)
		{
			_logger.error("Problems writing screen capture to file '"+file+"'.", e);
			return false;
		}
		return true;
	}

	/**
	 */
//	protected static BufferedImage capture(Window window)
	protected static BufferedImage capture(Component comp)
	{
		BufferedImage img = new BufferedImage(comp.getWidth(), comp.getHeight(), BufferedImage.TYPE_INT_RGB);
		comp.paintAll(img.createGraphics());
		return img;
	}
}
