package com.asetune.gui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class ListSelectionListenerDeferred 
implements ListSelectionListener, ActionListener
{
	private final static int DEFAULT_TIME = 500;

	ListSelectionEvent _lastEvent;
	Timer              _timer;

	private int _deferredTime = DEFAULT_TIME;

	/**
	 * This will kick off the deferredValueChanged() after 500 ms
	 */
	public ListSelectionListenerDeferred()
	{
		this(DEFAULT_TIME);
	}

	/**
	 * This will kick off the deferredValueChanged() after your <code>deferredTime</code>
	 * @param deferredTime
	 */
	public ListSelectionListenerDeferred(int deferredTime)
	{
		_deferredTime = deferredTime;
		_timer = new Timer(_deferredTime, this);
	}

	/**
	 * Kicked of Xms after last resized event has been sent.
	 * @param e The last received ComponentEvent
	 */
	abstract public void deferredValueChanged(ListSelectionEvent event);

	//-----------------------------------------------------------------------------
	// BEGIN: implement ListSelectionListener
	//-----------------------------------------------------------------------------
	@Override
	public void valueChanged(ListSelectionEvent event)
	{
		_lastEvent = event;

		if ( ! _timer.isRunning() )
			_timer.start();
		else
			_timer.restart();
	}
	//-----------------------------------------------------------------------------
	// END: implement ListSelectionListener
	//-----------------------------------------------------------------------------

	//-----------------------------------------------------------------------------
	// BEGIN: implement ActionListener
	//-----------------------------------------------------------------------------
	@Override
	public void actionPerformed(ActionEvent e)
	{
		_timer.stop();
		deferredValueChanged(_lastEvent);
	}
	//-----------------------------------------------------------------------------
	// END: implement ActionListener
	//-----------------------------------------------------------------------------
}
