package com.asetune.gui.swing;

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
