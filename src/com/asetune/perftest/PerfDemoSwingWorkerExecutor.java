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
package com.asetune.perftest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

/**
 * Usage: SwingWorkerExecutor.getInstance().execute(swingWorker);
 * <pre>
 * // Create a background worker thread
 * SwingWorker swingWorker = new SwingWorker, Void>() 
 * {
 *     // This method executes on the background worker thread
 *     protected doInBackground() throws Exception 
 *     {
 *         compute result;
 *         return result;
 *     }
 *     
 *     // This method executes on the UI thread
 *     protected void done() 
 *     {
 *         result = get();
 *     }
 * };
 * 
 * // Submit to the executor
 * SwingWorkerExecutor.getInstance().execute(swingWorker);
 * </pre>
 * 
 */
public class PerfDemoSwingWorkerExecutor
{
	private static final int MAX_WORKER_THREAD = 50;

	private static final PerfDemoSwingWorkerExecutor executor = new PerfDemoSwingWorkerExecutor();

	// Thread pool for worker thread execution
	private ExecutorService workerThreadPool = Executors.newFixedThreadPool(MAX_WORKER_THREAD);

	/**
	 * Private constructor required for the singleton pattern.
	 */
	private PerfDemoSwingWorkerExecutor()
	{
	}

	/**
	 * Returns the singleton instance.
	 * 
	 * @return SwingWorkerExecutor - Singleton.
	 */
	public static PerfDemoSwingWorkerExecutor getInstance()
	{
		return executor;
	}

	/**
	 * Adds the SwingWorker to the thread pool for execution.
	 * 
	 * @param worker  - The SwingWorker thread to execute.
	 * 
	 */
	@SuppressWarnings("rawtypes")
	public void execute(SwingWorker worker)
	{
		workerThreadPool.submit(worker);
	}
}
