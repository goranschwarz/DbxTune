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
