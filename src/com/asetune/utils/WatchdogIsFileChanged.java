package com.asetune.utils;

import java.io.File;

import org.apache.log4j.Logger;

public class WatchdogIsFileChanged
{
	private static Logger _logger = Logger.getLogger(WatchdogIsFileChanged.class);

	public interface WatchdogIsFileChangedChecker
	{
//		public void checkIfCurrentFileIsUpdated();
		public void fileHasChanged(File file, long savedLastModifiedTime);
	}

	public final static int WATCHDOG_IS_FILE_CHANGED_SLEEP_TIME_MS = 5000; 

	private Thread    _watchdogIsFileChanged = null;
	
	private int     _sleepTimeMs = WATCHDOG_IS_FILE_CHANGED_SLEEP_TIME_MS;
	private boolean _running = true;
	private boolean _paused  = false;

	private WatchdogIsFileChangedChecker _checker = null; 

	private File _currentFile             = null;
	private long _currentFileLastModified = 0;


	/**
	 * Create a WatchDog, later do: start(), setFile()
	 * @param checker interface that will be called when a file has been changed
	 * @param sleepTimeInMs sleep time between checks
	 */
	public WatchdogIsFileChanged(WatchdogIsFileChangedChecker checker, int sleepTimeInMs)
	{
		_checker     = checker;
		_sleepTimeMs = sleepTimeInMs;
	}
	
	/**
	 * Set a file to be checked for modification.<br>
	 * This can also be used to reset the modification date if the local application pressed 'save'...
	 * 
	 * @param fileFullPath name of the file to monitor
	 */
	public void setFile(String fileFullPath)
	{
		setFile(new File(fileFullPath));
	}
	/**
	 * Set a file to be checked for modification.<br>
	 * This can also be used to reset the modification date if the local application pressed 'save'...
	 * 
	 * @param f
	 */
	public void setFile(File f)
	{
		_logger.debug("WATCHDOG: setFile(): BEGIN: f="+f);

		try
		{
			// Pause the checker while setting the new file.
			// otherwise we might get into some race condition...
			setPaused(true);

			if (f.exists())
			{
				_currentFileLastModified = f.lastModified();
				_currentFile             = f;
			}
			else
			{
				_currentFileLastModified = 0;
				_currentFile             = null;
			}
		}
		finally
		{
			setPaused(false);
		}

		_logger.debug("WATCHDOG: setFile():   END: f="+f);
	}

	/** 
	 * Start the check thread
	 */
	public void start()
	{
		_watchdogIsFileChanged = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				_logger.info("WatchdogIsFileChanged has been Started.");
				while (_running)
				{
					if ( ! _paused )
					{
						try 
						{
							_logger.debug("WATCHDOG: run(): CHECKING FILE: _currentFile="+_currentFile);

							if (_currentFile != null)
							{
								if (_currentFile.lastModified() > _currentFileLastModified)
								{
									_logger.debug("WATCHDOG: run(): FILE HAS CHANGED: _currentFile="+_currentFile);
									
									_checker.fileHasChanged(_currentFile, _currentFileLastModified);
									
									// The above interface call might itself call setFile(), which might set _currentile to null
									if (_currentFile != null)
										_currentFileLastModified = _currentFile.lastModified();
								}
							}
//							_checker.checkIfCurrentFileIsUpdated();
						}
						catch (Throwable t)
						{
							_logger.warn("WatchdogIsFileChanged had problems when checking file, but continuing. Caught: "+t, t);
						}
					}
	
					try { Thread.sleep(_sleepTimeMs); }
					catch (InterruptedException ignore) { /*ignore */ }
				}
				_logger.info("WatchdogIsFileChanged has been Stopped.");
			}
		});
		_watchdogIsFileChanged.setName("WatchdogIsFileChanged");
		_watchdogIsFileChanged.setDaemon(true);
		_watchdogIsFileChanged.start();
	}
	
	/** 
	 * Stop the check thread
	 */
	public void shutdown()
	{
		_running = false;
		_watchdogIsFileChanged.interrupt();
	}

	/** 
	 * Pause or Continue the check thread<br>
	 * Typically called if you don't want the WatchDog to check file changes while not active in a window
	 */
	public void setPaused(boolean toStatus)
	{
		_paused = toStatus;
		
		// if not paused, make a check immediately
		if ( _paused == false )
			checkNow();
	}

	/**
	 * If the WatchDog is sleeping, you can interrupt the sleep and initiate a check using this method
	 */
	public void checkNow()
	{
		_watchdogIsFileChanged.interrupt();
	}
}
