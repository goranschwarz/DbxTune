/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.awt.EventQueue;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;

public final class ModalWait<T>
{
	private final SecondaryLoop _loop;
	private T                   _result;

	public interface Callback<T>
	{
		void run(ModalWait<T> wait);
	}

	public ModalWait()
	{
		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		this._loop = queue.createSecondaryLoop();
	}

	public T callAndWait(Callback<T> cb)
	{
		if ( ! EventQueue.isDispatchThread() )
		{
			throw new IllegalStateException("Must be on EDT");
		}

		cb.run(this); // pass instance so code can call complete(value)
		_loop.enter(); // nested event _loop
		return _result;
	}

	public void complete(T value)
	{
		this._result = value;
		_loop.exit();
	}
}
