/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class ClickListener extends MouseAdapter 
implements ActionListener
{
	// 500 is the default on Windows, so if the value isn't found, use that
	private final static int clickInterval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") == null ? 500 : (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");

	MouseEvent lastEvent;
	Timer      timer;

	public ClickListener()
	{
		this(clickInterval);
	}

	public ClickListener(int delay)
	{
		timer = new Timer(delay, this);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (SwingUtilities.isLeftMouseButton(e)) 
		{
			if ( e.getClickCount() > 2 )
				return;
			
			MouseEvent prev = lastEvent;
			lastEvent = e;
			
			if ( timer.isRunning() )
			{
				// Check that the "double click" click is in close proximity with the previous click
				if (prev != null)
				{
					double distance = prev.getPoint().distance( e.getPoint() );
					//System.out.println("distance="+distance);
					if (distance > 10)
					{
						// restart the timer, or make it a single click...
						timer.restart();
						return;
					}
				}

				timer.stop();
				doubleClick(lastEvent);
			}
			else
			{
				timer.restart();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		timer.stop();
		singleClick(lastEvent);
	}

	public void singleClick(MouseEvent e)
	{
	}

	public void doubleClick(MouseEvent e)
	{
	}

	public static void main(String[] args)
	{
		JFrame frame = new JFrame("Double Click Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addMouseListener(new ClickListener()
		{
			@Override
			public void singleClick(MouseEvent e)
			{
				System.out.println("single");
			}

			@Override
			public void doubleClick(MouseEvent e)
			{
				System.out.println("double");
			}
		});
		frame.setSize(200, 200);
		frame.setVisible(true);
	}
}
