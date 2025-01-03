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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.pcs;

import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.cm.CountersModel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.TimeUtils;


public class InMemoryCounterHandler 
implements Runnable
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public  static final String PROPKEY_HISTORY_SIZE_IN_SECONDS      = "InMemoryCounterHandler.history";
	public  static final int    DEFAULT_HISTORY_SIZE_IN_SECONDS      = 10 * 60;
	
	private static final String PROPKEY_QUEUE_SIZE_WARNING_THRESHOLD = "InMemoryCounterHandler.warnQueueSizeThresh";
	private static final int    DEFAULT_QUEUE_SIZE_WARNING_THRESHOLD = 2;
	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	// implements singleton pattern
	private static InMemoryCounterHandler _instance = null;

	private boolean _initialized = false;
	private boolean _running     = false;

	private Thread  _thread      = null;
	
	/** Configuration we were initialized with */
	private Configuration _props;
	
	private int         _warnQueueSizeThresh = 2;
	private int         _saveTimeInSec       = DEFAULT_HISTORY_SIZE_IN_SECONDS;
	
	/** A list of PersistContainer, New entries are added at "right" side. 
	 * Then it follows the "graphs" and "slider" numbering... */
	private LinkedList<PersistContainer> _list = new LinkedList<PersistContainer>();

	/** */
	private BlockingQueue<PersistContainer>  _containerQueue = new LinkedBlockingQueue<PersistContainer>();
	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public InMemoryCounterHandler()
	throws Exception
	{
	}

	public InMemoryCounterHandler(Configuration props)
	throws Exception
	{
		init(props);
	}

	/** Initialize various member of the class */
	public synchronized void init(Configuration props)
	throws Exception
	{
		_props = props; 

		_logger.info("Initializing the In-Memory Counter Handler functionality.");

		_warnQueueSizeThresh = _props.getIntProperty(PROPKEY_QUEUE_SIZE_WARNING_THRESHOLD, DEFAULT_QUEUE_SIZE_WARNING_THRESHOLD);

		int hist = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_HISTORY_SIZE_IN_SECONDS, -1);
		if (hist == -1)
		{
			_logger.info("Can't find property '"+PROPKEY_HISTORY_SIZE_IN_SECONDS+"', using default '"+DEFAULT_HISTORY_SIZE_IN_SECONDS+"'.");
			hist = DEFAULT_HISTORY_SIZE_IN_SECONDS;
		}
		else
		{
			setHistoryLengthInSeconds(hist);
		}

		_logger.info("In memory history will be '"+_saveTimeInSec+"' seconds, (Minutes:Seconds = "+TimeUtils.msToTimeStr("%MM:%SS", _saveTimeInSec*1000)+").");

		_initialized = true;
	}

	/*---------------------------------------------------
	** Listener stuff
	**---------------------------------------------------
	*/
	EventListenerList   _listenerList  = new EventListenerList();

	/** Add any listeners that want to see changes */
	public void addChangeListener(ChangeListener l)
	{
		_listenerList.add(ChangeListener.class, l);
	}

	/** Remove the listener */
	public void removeChangeListener(ChangeListener l)
	{
		_listenerList.remove(ChangeListener.class, l);
	}

	/** Kicked off when new entries are added */
	protected void fireStateChanged()
	{
		Object aobj[] = _listenerList.getListenerList();
		for (int i = aobj.length - 2; i >= 0; i -= 2)
		{
			if (aobj[i] == ChangeListener.class)
			{
				ChangeEvent changeEvent = new ChangeEvent(this);
				((ChangeListener) aobj[i + 1]).stateChanged(changeEvent);
			}
		}
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	
	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static InMemoryCounterHandler getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(InMemoryCounterHandler inst)
	{
		_instance = inst;
	}

	public void setHistoryLengthInSeconds(int seconds)
	{
//		if (hasInstance())
//			getInstance()._saveTimeInSec = seconds;
		_saveTimeInSec = seconds;

		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
		conf.setProperty(PROPKEY_HISTORY_SIZE_IN_SECONDS, seconds);
		conf.save();
		
		_logger.info("Setting new save time for in memory history to '"+seconds+"' seconds, (Minutes:Seconds = "+TimeUtils.msToTimeStr("%MM:%SS", seconds*1000)+").");
	}
	public void setHistoryLengthInMinutes(int minutes)
	{
		setHistoryLengthInSeconds( minutes * 60 );
	}

	/** How many entries are in the In-Memory storage */
	public synchronized int getSize()
	{
		return _list.size();
	}

	/** Called by the Counter Sampler, which adds a Sample Session */
	public void add(PersistContainer cont)
	{
		int qsize = _containerQueue.size();
		if (qsize > _warnQueueSizeThresh)
		{
			_logger.warn("The 'in box' queue to In Memory Counter Handler has "+qsize+" entries. The handler might not keep in pace.");
		}

		_containerQueue.add(cont);
	}

	public synchronized int indexOf(PersistContainer pc)
	{
		return _list.indexOf(pc);
	}
	public synchronized int indexOf(Timestamp ts)
	{
		if (ts == null)
			return -1;

		// The time stamp might not be exact... 
		// values are added newest entry at the end "right side"
		// the timestamp need to be between HEAD sample time + the sampleInterval
		for (int i=0; i<(_list.size()-1); i++)
		{
			PersistContainer pc  = _list.get(i);

			// the timestamp need to be between HEAD sample time + the sampleInterval
			if ( pc.equalsApprox(ts) )
			{
				return i;
			}
		}
		return -1;
	}

	public synchronized PersistContainer get(int pos)
	{
		return _list.get(pos);
	}
	
	public synchronized Timestamp getTs(int pos)
	{
		PersistContainer pc = _list.get(pos);
		return pc == null ? null : pc.getMainSampleTime();
	}
	
	public synchronized PersistContainer get(Timestamp ts)
	{
		int index = indexOf(ts);
		if (index >= 0)
			return get(index);
		return null;
	}
	
	public CountersModel getCmForSample(String name, Timestamp offlineSampleTime)
	{
		PersistContainer pc = get(offlineSampleTime);
		if (pc == null)
			return null;

		return pc.getCm(name);
	}

	
	/** Get from the "right" side of the list, this is the Most Recently Added, which is the newest entry */
	public PersistContainer getRight()
	{
		return getNewest();
	}
	/** Get from the "right" side of the list, this is the Most Recently Added, which is the newest entry */
	public synchronized PersistContainer getNewest()
	{
		return _list.getLast();
	}

	
	/** Get from the "left" side of the list, this is the oldest entry */
	public PersistContainer getLeft()
	{
		return getOldest();
	}
	/** Get from the "left" side of the list, this is the oldest entry */
	public synchronized PersistContainer getOldest()
	{
		return _list.getFirst();
	}
	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The Persistent Counter Handler module has NOT yet been initialized.");
		}
	}

	/**
	 * Try to clear the in memory counters
	 * @param keepLastEntry
	 */
	public void clear(boolean keepLastEntry)
	{
		// catch all runtime errors that might come
		try 
		{
			if (_list == null)   return;
			if (_list.isEmpty()) return;

			PersistContainer last = _list.getLast();

			_list.clear();
			if (keepLastEntry)
				_list.addLast(last);

			if (_logger.isDebugEnabled())
			{
				_logger.debug("The in-memory history list has "+_list.size()+" entries.");
			}
			
			// notify listeners...
			fireStateChanged();
		}
		catch (Throwable t)
		{
			_logger.error("The In Memory Counter Handler got runtime error.", t);
		}
	}

	private synchronized void consume(PersistContainer cont, long prevConsumeTimeMs)
	{
		// catch all runtime errors that might come
		try 
		{
			// Add entry
			_list.addLast(cont);

			// Remove last entry if it's to old
			Timestamp expireTs = new Timestamp( cont.getMainSampleTime().getTime() - (_saveTimeInSec*1000) );
			for(int i=0; i<1000; i++)
			{
				if (_list.size() == 0)
					break;

				PersistContainer oldest = _list.getFirst();
				if (_logger.isTraceEnabled())
					_logger.trace("_saveTimeInSec="+_saveTimeInSec+", oldest.ts='"+oldest.getMainSampleTime()+"', exireTs='"+expireTs+"'. last.getSampleTime().before(expireTs)="+oldest.getMainSampleTime().before(expireTs));
				if (oldest.getMainSampleTime().before( expireTs ) )
				{
					_logger.debug("Removing oldest entry: "+oldest.getMainSampleTime());
					_list.removeFirst();
				}
				else
					break;
			}
			
			if (_logger.isDebugEnabled())
			{
				_logger.debug("The in-memory history list has "+_list.size()+" entries.");
			}

			// notify listeners...
			fireStateChanged();
		}
		catch (Throwable t)
		{
			_logger.error("The In Memory Counter Handler got runtime error.", t);
		}
	}
	
	@Override
	public void run()
	{
		_logger.info("Starting a thread for the module '"+_thread.getName()+"'.");

		isInitialized();

		_running = true;
		long prevConsumeTimeMs = 0;

		while(_running)
		{
			//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
			//try { Thread.sleep(5 * 1000); }
			//catch (InterruptedException ignore) {}
			
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+_thread.getName()+"', waiting on queue...");

			try 
			{
				PersistContainer cont = _containerQueue.take();

				// Make sure the container isn't empty.
				if (cont == null || (cont != null && cont.isEmpty()) )
					continue;

				// Go and store or consume the in-data/container
				long startTime = System.currentTimeMillis();
				consume( cont, prevConsumeTimeMs );
				long stopTime = System.currentTimeMillis();

				prevConsumeTimeMs = stopTime-startTime;
				_logger.debug("It took "+prevConsumeTimeMs+" ms to persist the above information.");
				
			} 
			catch (InterruptedException ex) 
			{
				_running = false;
			}
		}

		_logger.info("Thread '"+_thread.getName()+"' was stopped.");
	}

	public boolean isRunning()
	{
		return _running;
	}

	public void start()
	{
		isInitialized();

		_thread = new Thread(this);
		_thread.setName("InMemoryCounterHandler");
		_thread.setDaemon(true);
		_thread.start();
	}

	public void stop()
	{
		_running = false;
		if (_thread != null)
		{
			_thread.interrupt();
			_thread = null;
		}
	}

	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
	//// ---- TEST CODE ---- TEST CODE ---- TEST CODE ---- TEST CODE ----
	//////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////
//	public static void main(String[] args) 
//	{
//	}
}
