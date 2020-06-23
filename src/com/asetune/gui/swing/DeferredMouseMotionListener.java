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
