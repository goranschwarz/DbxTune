package com.asetune;

import org.apache.log4j.Logger;

public abstract class CounterCollectorThreadAbstract
extends Thread
{
	private static Logger _logger = Logger.getLogger(CounterCollectorThreadAbstract.class);

	protected Thread   _thread  = null;
	protected boolean  _running = false;

	protected ICounterController _counterController;
	public CounterCollectorThreadAbstract(ICounterController counterController)
	{
		_counterController = counterController;
//		_thread = this;
	}
	
	public ICounterController getCounterController()
	{
		return _counterController;
	}

//	public boolean isRunning()
//	{
//		return _running;
//	}
//	public void setRunning(boolean toValue)
//	{
//		_running = toValue;
//	}

	public void shutdown()
	{
		_logger.info("Stopping the collector thread.");
		_running = false;
		if (_thread != null)
			_thread.interrupt();
	}

	public abstract void init(boolean hasGui) throws Exception;

	@Override
	public abstract void run();

	/** NOTE: this SHOULD be called when GetCounters.closeMonConnection() */
	public void cleanupMonConnection()
	{
	}

	public void doInterrupt()
	{
		if (_thread != null)
		{
			_logger.debug("Sending 'interrupt' to the thread '"+_thread.getName()+"', this was done by thread '"+Thread.currentThread().getName()+"'.");
			_thread.interrupt();
		}
	}
}
