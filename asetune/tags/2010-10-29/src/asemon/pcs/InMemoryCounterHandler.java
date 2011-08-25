/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import org.apache.log4j.Logger;

import asemon.utils.Configuration;

public class InMemoryCounterHandler 
implements Runnable
{
	private static Logger _logger          = Logger.getLogger(InMemoryCounterHandler.class);

	
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
	private int         _saveTimeInSec       = 10 * 60;
	
	/** A list of PersistContainer, New entries are added at "right" side. 
	 * Then it follows the "graphs" and "slider" numbering... */
	private LinkedList  _list                = new LinkedList();

	/** */
	private BlockingQueue  _containerQueue = new LinkedBlockingQueue();
	
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

		_warnQueueSizeThresh = _props.getIntProperty("InMemoryCounterHandler.warnQueueSizeThresh", _warnQueueSizeThresh);

		int hist = Configuration.getInstance(Configuration.TEMP).getIntProperty("InMemoryCounterHandler.history", -1);
		if (hist == -1)
		{
			_logger.info("Cant find property 'InMemoryCounterHandler.history', using default '"+_saveTimeInSec+"'.");
		}
		else
		{
			setHistoryLengthInSeconds(hist);
		}

		_logger.info("Saving Counters in memory for '"+_saveTimeInSec+"' seconds.");

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
		_saveTimeInSec = seconds;

		Configuration conf = Configuration.getInstance(Configuration.TEMP);
		conf.setProperty("InMemoryCounterHandler.history", seconds);
		conf.save();
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
			PersistContainer pc  = (PersistContainer) _list.get(i);

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
		return (PersistContainer) _list.get(pos);
	}
	
	public synchronized Timestamp getTs(int pos)
	{
		PersistContainer pc = (PersistContainer) _list.get(pos);
		return pc == null ? null : pc.getSampleTime();
	}
	
	public synchronized PersistContainer get(Timestamp ts)
	{
		int index = indexOf(ts);
		if (index >= 0)
			return get(index);
		return null;
	}
	
	/** Get from the "right" side of the list, this is the Most Recently Added, which is the newest entry */
	public PersistContainer getRight()
	{
		return getNewest();
	}
	/** Get from the "right" side of the list, this is the Most Recently Added, which is the newest entry */
	public synchronized PersistContainer getNewest()
	{
		return (PersistContainer) _list.getLast();
	}

	
	/** Get from the "left" side of the list, this is the oldest entry */
	public PersistContainer getLeft()
	{
		return getOldest();
	}
	/** Get from the "left" side of the list, this is the oldest entry */
	public synchronized PersistContainer getOldest()
	{
		return (PersistContainer) _list.getFirst();
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

			PersistContainer last = (PersistContainer) _list.getLast();

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
			Timestamp expireTs = new Timestamp( cont.getSampleTime().getTime() - (_saveTimeInSec*1000) );
			for(int i=0; i<1000; i++)
			{
				if (_list.size() == 0)
					break;

				PersistContainer oldest = (PersistContainer) _list.getFirst();
				if (_logger.isTraceEnabled())
					_logger.trace("_saveTimeInSec="+_saveTimeInSec+", oldest.ts='"+oldest.getSampleTime()+"', exireTs='"+expireTs+"'. last.getSampleTime().before(expireTs)="+oldest.getSampleTime().before(expireTs));
				if (oldest.getSampleTime().before( expireTs ) )
				{
					_logger.debug("Removing oldest entry: "+oldest.getSampleTime());
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
				PersistContainer cont = (PersistContainer)_containerQueue.take();

				// Make sure the container isn't empty.
				if (cont == null)                     continue;
				if (cont._counterObjects == null)	  continue;
				if (cont._counterObjects.size() <= 0) continue;

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
