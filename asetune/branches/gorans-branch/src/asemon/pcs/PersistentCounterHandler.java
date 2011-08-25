/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.pcs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import asemon.cm.CountersModel;
import asemon.utils.Configuration;

public class PersistentCounterHandler 
implements Runnable
{
	private static Logger _logger          = Logger.getLogger(PersistentCounterHandler.class);

	
	/*---------------------------------------------------
	** Constants
	**---------------------------------------------------
	*/

	/*---------------------------------------------------
	** class members
	**---------------------------------------------------
	*/

	// implements singleton pattern
	private static PersistentCounterHandler _instance = null;

	private boolean _initialized = false;
	private boolean _running     = false;

	private Thread  _thread      = null;
	
	/** Configuration we were initialized with */
	private Configuration _props;
	
	/** a list of installed Writers */
	private List _writerClasses = new LinkedList();

	private int  _warnQueueSizeThresh = 2;
	/** */
	private BlockingQueue  _containerQueue = new LinkedBlockingQueue();
	
	/*---------------------------------------------------
	** Constructors
	**---------------------------------------------------
	*/
	public PersistentCounterHandler()
	throws Exception
	{
	}

	public PersistentCounterHandler(Configuration props)
	throws Exception
	{
		init(props);
	}

	/** Initialize various member of the class */
	public synchronized void init(Configuration props)
	throws Exception
	{
		_props = props; 
		
		_logger.info("Initializing the Persistent Counter Handler functionality.");

		_warnQueueSizeThresh = _props.getIntProperty("PersistentCounterHandler.warnQueueSizeThresh", _warnQueueSizeThresh);

		// property: alarm.handleAlarmEventClass
		// NOTE: this could be a comma ',' separated list
		String writerClasses = _props.getProperty("PersistentCounterHandler.WriterClass");
		if (writerClasses == null)
		{
//			throw new Exception("The property 'PersistentCounterHandler.WriterClass' is mandatory for the PersistentCounterHandler module. It should contain one or several classes that implemets the IPersistWriter interface. If you have more than one writer, specify them as a comma separated list.");
			_logger.info("No counters will be persisted. The property 'PersistentCounterHandler.WriterClass' is not found in configuration for the PersistentCounterHandler module. It should contain one or several classes that implemets the IPersistWriter interface. If you have more than one writer, specify them as a comma separated list.");
		}
		else
		{
			String[] writerClassArray =  writerClasses.split(",");
			for (int i=0; i<writerClassArray.length; i++)
			{
				writerClassArray[i] = writerClassArray[i].trim();
				String writerClassName = writerClassArray[i];
				IPersistWriter writerClass;
	
				_logger.debug("Instantiating and Initializing WriterClass='"+writerClassName+"'.");
				try
				{
					Class c = Class.forName( writerClassName );
					writerClass = (IPersistWriter) c.newInstance();
					_writerClasses.add( writerClass );
				}
				catch (ClassCastException e)
				{
					throw new ClassCastException("When trying to load writerWriter class '"+writerClassName+"'. The writerWriter do not seem to follow the interface 'asemon.pcs.IPersistWriter'");
				}
				catch (ClassNotFoundException e)
				{
					throw new ClassNotFoundException("Tried to load writerWriter class '"+writerClassName+"'.", e);
				}
	
				// Now initialize the User Defined AlarmWriter
				writerClass.init(_props);
			}
			if (_writerClasses.size() == 0)
			{
				_logger.warn("No Persistent Counter Writers has been installed, NO counters will be saved.");
			}
		}

		_initialized = true;
	}

	/*---------------------------------------------------
	** Methods
	**---------------------------------------------------
	*/
	
	//////////////////////////////////////////////
	//// Instance
	//////////////////////////////////////////////
	public static PersistentCounterHandler getInstance()
	{
		return _instance;
	}

	public static boolean hasInstance()
	{
		return (_instance != null);
	}

	public static void setInstance(PersistentCounterHandler inst)
	{
		_instance = inst;
	}

	
	//////////////////////////////////////////////
	//// xxx
	//////////////////////////////////////////////
	
	public void add(PersistContainer cont)
	{
		if (_writerClasses.size() == 0)
			return;

		int qsize = _containerQueue.size();
		if (qsize > _warnQueueSizeThresh)
		{
			_logger.warn("The persistent queue has "+qsize+" entries. The persistent writer might not keep in pace.");
		}

		_containerQueue.add(cont);
	}
	
	private void isInitialized()
	{
		if ( ! _initialized )
		{
			throw new RuntimeException("The Persistent Counter Handler module has NOT yet been initialized.");
		}
	}
	

	private void consume(PersistContainer cont, long prevConsumeTimeMs)
	{
		Iterator writerIter = _writerClasses.iterator();
		while (writerIter.hasNext()) 
		{
			IPersistWriter pw = (IPersistWriter) writerIter.next();

			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
				_logger.info("Persisting Counters using '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', sampleTime='"+cont.getSampleTime()+"'. Previous persist took "+prevConsumeTimeMs+" ms.");

				// BEGIN-OF-SAMPLE If we want to do anything in here
				pw.beginOfSample();

				
				// CREATE-DDL
				Iterator cmIter = cont._counterObjects.iterator();
				while (cmIter.hasNext()) 
				{
					CountersModel cm = (CountersModel) cmIter.next();
					
					// only call saveDdl() the first time...
					if ( ! pw.isDdlCreated(cm) )
					{
						if (pw.saveDdl(cm))
						{
							pw.markDdlAsCreated(cm);
						}
					}
				}

				// SAVE-SAMPLE
				// In here we can "do it all" 
				// or use: beginOfSample(), saveDdl(), saveCounters(), endOfSample()
				pw.saveSample(cont);

				
				// SAVE-COUNTERS
				cmIter = cont._counterObjects.iterator();
				while (cmIter.hasNext()) 
				{
					CountersModel cm = (CountersModel) cmIter.next();
					
					pw.saveCounters(cm);
				}

				
				// END-OF-SAMPLE If we want to do anything in here
				pw.endOfSample();
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error when calling the method endOfScan() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
			}
		}
	}
	
	/** When we start a new session, lets call this method to get some 
	 * idea what we are about to sample. 
	 * @param cont a PersistContainer filled with <b>all</b> the available
	 *             CounterModels we could sample.
	 */
	public void startSession(PersistContainer cont)
	{
		Iterator writerIter = _writerClasses.iterator();
		while (writerIter.hasNext()) 
		{
			IPersistWriter pw = (IPersistWriter) writerIter.next();

			// CALL THE installed Writer
			// AND catch all runtime errors that might come
			try 
			{
				_logger.info("Starting Counters Storage Session '"+pw.getName()+"' for sessionStartTime='"+cont.getSessionStartTime()+"', server='"+cont.getServerName()+"'.");

				pw.startSession(cont);
			}
			catch (Throwable t)
			{
				_logger.error("The Persistent Writer got runtime error when calling the method startSession() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
			}
		}
	}

	public void run()
	{
		String threadName = _thread.getName();
		_logger.info("Starting a thread for the module '"+threadName+"'.");

		isInitialized();

		_running = true;
		long prevConsumeTimeMs = 0;

		while(_running)
		{
			//_logger.info("Thread '"+_thread.getName()+"', SLEEPS...");
			//try { Thread.sleep(5 * 1000); }
			//catch (InterruptedException ignore) {}
			
			if (_logger.isDebugEnabled())
				_logger.debug("Thread '"+threadName+"', waiting on queue...");

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

		_logger.info("Emptying the queue for module '"+threadName+"', which had "+_containerQueue.size()+" entries.");
		_containerQueue.clear();

		_logger.info("Thread '"+threadName+"' was stopped.");
	}

	public boolean isRunning()
	{
		return _running;
	}

	public void start()
	{
		if (_writerClasses.size() == 0)
		{
			_logger.warn("No Persistent Counter Writers has been installed, The service thread will NOT be started and NO counters will be saved.");
			return;
		}

		isInitialized();

		_thread = new Thread(this);
		_thread.setName("PersistentCounterHandler");
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

		// Close the connections to the datastore.
		Iterator writerIter = _writerClasses.iterator();
		while (writerIter.hasNext()) 
		{
			IPersistWriter pw = (IPersistWriter) writerIter.next();
			pw.close();
		}
	}

	public boolean hasWriters()
	{
		return (_writerClasses.size() > 0);
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
