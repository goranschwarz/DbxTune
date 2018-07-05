package com.asetune.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public abstract class DeferredChangeListener
implements ChangeListener, ActionListener
{
	private final static int     DEFAULT_TIME    = 50;
	private final static boolean DEFAULT_RESTART = true;

	private ChangeEvent _lastEvent;
	private Timer       _timer;

	private boolean     _doRestart    = DEFAULT_RESTART;
	private int         _deferredTime = DEFAULT_TIME;

	public DeferredChangeListener()
	{
		this(DEFAULT_TIME, DEFAULT_RESTART);
	}

	public DeferredChangeListener(int deferredTime, boolean doRestart)
	{
		_deferredTime = deferredTime;
		_doRestart    = doRestart;
		_timer = new Timer(_deferredTime, this);
	}

	/**
	 * Kicked of Xms after last ChangeEvent event has been sent.
	 * @param e The last received ChangeEvent
	 */
	public abstract void deferredStateChanged(ChangeEvent e);

	
	//-----------------------------------------------------------------------------
	// BEGIN: implement MouseMotionListener
	//-----------------------------------------------------------------------------
	@Override
	public void stateChanged(ChangeEvent e)
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
		deferredStateChanged(_lastEvent);
	}
	//-----------------------------------------------------------------------------
	// END: implement ActionListener
	//-----------------------------------------------------------------------------
}
