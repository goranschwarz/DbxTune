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
