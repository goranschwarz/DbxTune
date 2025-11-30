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
