package com.asetune.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.Timer;

public class DeferredResizeListener
implements ComponentListener, ActionListener
{
	private final static int DEFAULT_TIME = 500;

	ComponentEvent _lastEvent;
	Timer          _timer;

	private int _deferredTime = DEFAULT_TIME;

	public DeferredResizeListener()
	{
		this(DEFAULT_TIME);
	}

	public DeferredResizeListener(int deferredTime)
	{
		_deferredTime = deferredTime;
		_timer = new Timer(_deferredTime, this);
	}

	/**
	 * Kicked of Xms after last resized event has been sent.
	 * @param e The last received ComponentEvent
	 */
	public void deferredResize(ComponentEvent e)
	{
	}

	
	//-----------------------------------------------------------------------------
	// BEGIN: implement ComponentListener
	//-----------------------------------------------------------------------------
	@Override
	public void componentResized(ComponentEvent e)
	{
		_lastEvent = e;
		
		if ( ! _timer.isRunning() )
			_timer.start();
		else
			_timer.restart();
	}

	@Override
	public void componentMoved(ComponentEvent e)
	{
	}

	@Override
	public void componentShown(ComponentEvent e)
	{
	}

	@Override
	public void componentHidden(ComponentEvent e)
	{
	}
	//-----------------------------------------------------------------------------
	// END: implement ComponentListener
	//-----------------------------------------------------------------------------

	
	//-----------------------------------------------------------------------------
	// BEGIN: implement ActionListener
	//-----------------------------------------------------------------------------
	@Override
	public void actionPerformed(ActionEvent e)
	{
		_timer.stop();
		deferredResize(_lastEvent);
	}
	//-----------------------------------------------------------------------------
	// END: implement ActionListener
	//-----------------------------------------------------------------------------
}
