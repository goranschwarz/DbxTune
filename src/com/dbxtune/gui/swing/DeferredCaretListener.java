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

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public class DeferredCaretListener
implements CaretListener, ActionListener
{
	// 500 is the default on Windows, so if the value isn't found, use that
	private final static int clickInterval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval") == null ? 500 : (Integer) Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");

	CaretEvent lastEvent;
	Timer      timer;

	public DeferredCaretListener()
	{
		this(clickInterval);
	}

	public DeferredCaretListener(int delay)
	{
		timer = new Timer(delay, this);
		timer.stop();
	}

	@Override
	public void caretUpdate(CaretEvent e)
	{
		lastEvent = e;

		if ( timer.isRunning() )
		{
			timer.restart();
		}
		else
		{
			timer.start();
			startMove(e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		timer.stop();
		stopMove(lastEvent);
	}

	public void startMove(CaretEvent e)
	{
//System.out.println("startMove(): e="+e);
	}

	public void stopMove(CaretEvent e)
	{
//System.out.println("stopMove(): e="+e);
		// If we have a selection
		// Search for the selected selection, normally this is done when "double click"
		int dot  = e.getDot();
		int mark = e.getMark();
		if (dot != mark)
		{
			Object source = e.getSource();
//System.out.println("stopMove(): dot != mark, source="+source);
			if (source instanceof JTextArea)
			{
				JTextArea ta = (JTextArea) source;
				String selectedText = ta.getSelectedText();
				if (selectedText != null)
				{
//System.out.println("call: stopMoveWithSelection(): e="+e+", selectedText='"+selectedText+"'.");
					stopMoveWithSelection(e, selectedText);
				}
			}
		}
	}

	public void stopMoveWithSelection(CaretEvent e, String selectedText)
	{
	}
}
