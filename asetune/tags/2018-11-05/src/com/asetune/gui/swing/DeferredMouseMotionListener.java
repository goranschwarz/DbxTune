package com.asetune.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.Timer;

public abstract class DeferredMouseMotionListener
implements MouseMotionListener, ActionListener
{
	private final static int     DEFAULT_TIME    = 100;
	private final static boolean DEFAULT_RESTART = true;

	private MouseEvent _lastEvent;
	private Timer      _timer;

	private boolean    _doRestart    = DEFAULT_RESTART;
	private int        _deferredTime = DEFAULT_TIME;

	public DeferredMouseMotionListener()
	{
		this(DEFAULT_TIME, DEFAULT_RESTART);
	}

	public DeferredMouseMotionListener(int deferredTime, boolean doRestart)
	{
		_deferredTime = deferredTime;
		_doRestart    = doRestart;
		_timer = new Timer(_deferredTime, this);
	}

	/**
	 * Kicked of Xms after last mouseMoved event has been sent.
	 * @param e The last received MouseEvent
	 */
	public abstract void deferredMouseMoved(MouseEvent e);

	
	//-----------------------------------------------------------------------------
	// BEGIN: implement MouseMotionListener
	//-----------------------------------------------------------------------------
	@Override
	public void mouseDragged(MouseEvent e)
	{
		// EMPTY
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		_lastEvent = e;
		
		if ( ! _timer.isRunning() )
			_timer.start();
		else if (_doRestart)
			_timer.restart();
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
		deferredMouseMoved(_lastEvent);
	}
	//-----------------------------------------------------------------------------
	// END: implement ActionListener
	//-----------------------------------------------------------------------------
}
