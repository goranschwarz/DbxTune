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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class ModalSleep
{
	private final SecondaryLoop _secLoop;
	private final Timer         _timer;
	private       long          _startTime;
	private       long          _stopTime;
	private       boolean       _interrupted = false;

	private int  _maxSleepMs;
	private long _currentWaitedTimeMs;

	/**
	 * Sleep for x milliseconds.
	 * <p>
	 * Example: ModalSleep modalSleep = new ModalSleep(10_000).start();
	 * @param millis
	 */
	public ModalSleep(int millis)
	{
		if ( !EventQueue.isDispatchThread() )
		{
			throw new IllegalStateException("Must be called on EDT");
		}

		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		_secLoop = queue.createSecondaryLoop();

		_timer = new Timer(millis, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_secLoop.exit();
				_stopTime = System.currentTimeMillis();
			}
		});
		_timer.setRepeats(false);
	}

	/** If we want to abort the sleep... called at callBackIntervalMs */
	public interface SleepAbortCallback
	{
		/** Called every MS interval, return true if you don't want to sleep anymore */
		public boolean abortSleepQuestion();
	}
	
	/**
	 * Sleep for x milliseconds.
	 * <p>
	 * And check every 'callBackIntervalMs' using 'callback' if we should continue to sleep or abort the sleep...
	 * 
	 * @param maxSleepMs             The maximum sleep time we want to sleep
	 * @param callbackIntervalMs     Call the 'callback' every X milliseconds to check if we should continue to sleep.
	 * @param sleepAbortCallback     The Callback 
	 */
	public ModalSleep(int maxSleepMs, int callbackIntervalMs, SleepAbortCallback sleepAbortCallback)
	{
		if ( !EventQueue.isDispatchThread() )
		{
			throw new IllegalStateException("Must be called on EDT");
		}

		EventQueue queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		_secLoop = queue.createSecondaryLoop();

		_maxSleepMs = maxSleepMs;
		_timer = new Timer(callbackIntervalMs, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_currentWaitedTimeMs = System.currentTimeMillis() - _startTime;

				boolean exceededTotalSleepTime = _currentWaitedTimeMs > maxSleepMs;
				if (exceededTotalSleepTime)
				{
					interrupt_private(false);					
				}

				boolean abortSleep = sleepAbortCallback.abortSleepQuestion();
				if (abortSleep)
				{
					interrupt_private(true);
				}
			}
		});
		_timer.setRepeats(true);
	}

	/** Starts the sleep (blocks caller logically, but EDT stays alive) */
//	public ModalSleep start()
//	{
//		_startTime = System.currentTimeMillis();
//		_timer.start();
//		_secLoop.enter();
//
//		return this;
//	}
	public void start()
	{
		_startTime = System.currentTimeMillis();
		_timer.start();
		_secLoop.enter();
	}
	
	/** If we set a MAX Sleep time, this would be the value */
	public long getMaxSleepTime()
	{
		return _maxSleepMs;
	}

	/** How many MS have we waited so far (only updated every 'callbackIntervalMs') */
	public long getCurrentWaitedTimeMs()
	{
		return _currentWaitedTimeMs;
	}

	public int getPercentWaitedTime()
	{
		double pct = (getCurrentWaitedTimeMs() * 1.0) / getMaxSleepTime() * 100.0;
		return (int) pct;
	}

	/** How long did we sleep for */
	public long getTotalSleepTime()
	{
		return _stopTime - _startTime;
	}

	/** Interrupts the sleep early */
	private void interrupt_private(boolean interrupted)
	{
		_interrupted = interrupted;
		_timer.stop();
		_secLoop.exit();

		_stopTime = System.currentTimeMillis();
	}

	/** Interrupts the sleep early */
	public void interrupt()
	{
		interrupt_private(true);
	}

	/** Returns true if the sleep was _interrupted */
	public boolean wasInterrupted()
	{
		return _interrupted;
	}




	/**
	 * Sleep without blocking the EDT
	 * <p>
	 * Currently there are no way to interrupt this sleep.<br>
	 * If you need that, please use:
	 * <pre>
	 * ModalSleep modalSleep = new ModalSleep(10_000); or ModalSleep(10_000, 500, with-a-callback-method);
	 * -- Then from "somewhere"... call: modalSleep.interrupt();
	 * </pre>
	 * 
	 * @param millis
	 */
	public static void sleep(final int millis)
	{
		if ( !EventQueue.isDispatchThread() )
		{
			throw new IllegalStateException("ModalWait.sleep must be called on the EDT");
		}

		final EventQueue    queue = Toolkit.getDefaultToolkit().getSystemEventQueue();
		final SecondaryLoop loop  = queue.createSecondaryLoop();

		new javax.swing.Timer(millis, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				loop.exit();
			}
		}).start();

		loop.enter(); // modal-style wait without freezing the UI
	}
}
