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
package com.asetune.gui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.SplashScreen;
import java.io.File;

public class SplashWindow
{
	private static final String[] SPLASHES = { "splash.jpg", "splash2.jpg" };
	private static int _textX = 10, _textWClear = 250;
	private static int _barX  = 2,  _barWClear  = 250;
	private static int _textH = 10, _barH = 9, _barW = 3;
	private static int _barRightMargin = 100;
	private static int _textRightMargin = 0;
	private static int _textYpos = 40;
	private static int _numOfBars = 100;
	
	private static int _expectedProgressCalls = 100;

	private static int _textY, _barY;
	private static int _barPos = 0;

	private static SplashScreen _splash = null;
	private static Graphics2D   _graph  = null;

	private static long _lastProgressCall = 0;

	public static void init()
	{
		init(true);
	}
	public static void init(boolean printProblems)
	{
		init(printProblems, 100, 500);
	}
	/**
	 * Initialize the Splash Screen/Window
	 * @param printProblems print to stdout if <code>SplashScreen.getSplashScreen()</code> failed.
	 * @param expectedProgressCalls Number of expected steps/calls to <code>drawSplashProgress(text)</code>, this calculates this how many bars we should draw for a call to <code>drawSplashProgress(text)</code>
	 * @param bgProgressUpdateTime If we want the progress bar to grow even if no calls has been made to <code>drawSplashProgress(text)</code>, this would be the time between background paints.
	 */
	public static void init(boolean printProblems, int expectedProgressCalls, final long bgProgressUpdateTime)
	{
		_expectedProgressCalls = expectedProgressCalls;

		_splash = SplashScreen.getSplashScreen();
		if ( _splash == null)
		{
			if (printProblems)
				System.out.println("No splash image specified on the command line.");
			return;
		}

		// compute base positions for text and progress bar
		Dimension splashSize = _splash.getSize();
		_textY = splashSize.height - _textYpos;
		_barY = splashSize.height - _barH - 2; // at the bottom

		// Calculate how many bars we should display, and the clear region
		_numOfBars = (splashSize.width - _barX - _barRightMargin) / (_barW + 1);
		_barWClear = splashSize.width - _barX - _barRightMargin + 1;
//		System.out.println("NUM_BUBBLES="+numOfBars);

		// Calculate text clear region
		_textWClear = splashSize.width - _textX - _textRightMargin + 1;

		_graph = _splash.createGraphics();
		//drawSplashUrl(splash.getImageURL());

		// kick off a progress bar update every now and then...
		if (bgProgressUpdateTime > 0)
		{
			Runnable updProgressBar = new Runnable()
			{
				@Override
				public void run()
				{
					long sleepTime = bgProgressUpdateTime;
					Thread.currentThread().setName("SplashWindow:UpdProgressBar");
					try
					{
						while (_splash.isVisible())
						{
							Thread.sleep(sleepTime);
		
							if (System.currentTimeMillis() - _lastProgressCall > sleepTime)
							{
								_expectedProgressCalls++;
								drawProgress(null);
							}
						}
					}
					catch (IllegalStateException ignore) {/*ignore*/}
					catch (InterruptedException ignore) {/*ignore*/}
				}
			};
			new Thread(updProgressBar).start();
		}
	}

	public static boolean isOk()
	{
		return (_splash != null) ? true : false;
	}

	public static void setExpectedProgressCalls(int num)
	{
		_expectedProgressCalls = num;
	}

	public static void close()
	{
		try
		{
			if (_splash == null)
				_splash = SplashScreen.getSplashScreen();
	
			if (_splash != null)
			{
				_splash.close();
				_splash = null;
			}
		}
		catch (HeadlessException e)
		{
		}
		catch (UnsupportedOperationException e)
		{
		}
		catch (Throwable t)
		{
		}
	}

	public static void drawProgress(String msg)
	{
		if (_splash == null)
			return;
		if ( ! _splash.isVisible() )
			return;

		_lastProgressCall = System.currentTimeMillis();

		// clear what we don't need from previous state
		_graph.setComposite(AlphaComposite.Clear);

		// Clear text field
		if (msg != null)
			_graph.fillRect(_textX, _textY, _textWClear, _textH + 2);

		// Clear Progress bar field
		if ( _barPos == 0 )
			_graph.fillRect(_barX, _barY, _barWClear, _barH);

		// draw new state
		_graph.setPaintMode();

		// draw message
		if (msg != null)
		{
			_graph.setColor(Color.BLACK);
			_graph.drawString(msg, _textX, _textY + _textH);
		}

		// draw progress bar
		_graph.setColor(Color.LIGHT_GRAY);
		
//		System.out.println("XXX="+numOfBars/expectedProgressCalls);
		int barCount = Math.max(1, _numOfBars/_expectedProgressCalls);
		for (int bc=0; bc<barCount; bc++)
		{
			_graph.fillRect(_barX + _barPos * (_barW + 1), _barY, _barW, _barH);
			_barPos++;
		}

		// show changes
		_splash.update();
//		_barPos = (_barPos + 1) % _numOfBars;
		if (_barPos >= _numOfBars)
			_barPos = 0;
	}

	public static void changeImage(int i)
	{
		if (_splash == null)
			return;

		try
		{
			_splash.setImageURL(new File(SPLASHES[(i / 10) % 2]).toURI().toURL());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void drawTopRight(String str)
	{
		if (_splash == null)
			return;

		FontMetrics fm = _graph.getFontMetrics();
		int strWidth   = fm.stringWidth(str);
		int strHeight  = fm.getHeight();

		// Place it at right top.
		Dimension splashSize = _splash.getSize();
		int verX = splashSize.width - strWidth - 7;
		int verY = strHeight;

		_graph.setPaintMode();
		_graph.setColor(Color.LIGHT_GRAY);
		_graph.drawString(str, verX, verY);
		_splash.update();
	}

	public static void drawTopLeft(String str)
	{
		if (_splash == null)
			return;

		FontMetrics fm = _graph.getFontMetrics();
		int strHeight  = fm.getHeight();

		// Place it at right top.
		int verX = 7;
		int verY = strHeight;

		_graph.setPaintMode();
		_graph.setColor(Color.LIGHT_GRAY);
		_graph.drawString(str, verX, verY);
		_splash.update();
	}

	// Test it using: C:\projects\asetune\classes>java -splash:../lib/asetune_splash.jpg -cp . com.asetune.gui.SplashWindow
	public static void main(String args[]) throws Exception
	{
		SplashWindow.init(false, 200, 500);
//		SplashWindow.init();
		for (int i = 0; i < 100; i++)
		{
			SplashWindow.drawProgress("Progress step number " + i);
			Thread.sleep(700);

			if ( i > 0 && i % 5 == 0 )
				SplashWindow.drawProgress("extra drawProg after number " + 1);
				
			// change the splash image from time to time
//			if ( i > 0 && i % 10 == 0 )
//			{
//				test.changeSplash(i);
//			}
		}

		SplashWindow.close();
	}
}
