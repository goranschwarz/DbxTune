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

import asemon.CountersModel;
import asemon.utils.Configuration;

/**
 * TODO: make it a service thread with a Queue so that it wont "block" 
 *       the caller while any of the AlarmWriter does it's job
 *       
 * @author Goran Schwarz
 */
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
//	public void raise(AlarmEvent alarmEvent)
//	{
//		isInitialized();
//		if (isSendAlarmsDisabled())
//		{
//			_logger.debug("AlarmHandler.raise() is DISABLED. isSendAlarmsDisabled()=true.");
//			return;
//		}
//
//		// Add alarms to "last check list"
//		// This so we can cancel alarms later on...
//		if ( ! _alarmContLast.contains(alarmEvent) )
//		{
//			_alarmContLast.add(alarmEvent);
//		}
//
//		// Check if the alarm already has been raised
//		// Then we do NOT need to signal a new alarm...
//		if ( _alarmContSaved.contains(alarmEvent) )
//		{
//			_logger.debug("The AlarmEvent has already been raised: " + alarmEvent);
//			return;
//		}
//
//		// Save the alarms
//		_alarmContSaved.add(alarmEvent);
//		saveAlarms(_alarmContSaved, _serializedFileName);
//
//		// raise the alarms in all the alarm writers
//		Iterator iter = _alarmClasses.iterator();
//		while (iter.hasNext()) 
//		{
//			IAlarmWriter aw = (IAlarmWriter) iter.next();
//
//			// CALL THE installed AlarmWriter
//			// AND catch all rutime errors that might come
//			try 
//			{
//				aw.raise(alarmEvent);
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The AlarmHandler got runtime error when calling the method raise() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
//			}
//		}
//	}
//
//	/**
//	 * This methos should only be executed at the end of a check loop.
//	 */
//	public void restoredAlarms()
//	{
//		// Call restoredAlarms() for all writers
//		Iterator writerIter = _alarmClasses.iterator();
//		while (writerIter.hasNext()) 
//		{
//			IAlarmWriter aw = (IAlarmWriter) writerIter.next();
//
//			_logger.debug("Calling restoredAlarms() event in AlarmWriter named='"+aw.getName()+"'.");
//
//			// CALL THE installed AlarmWriter
//			// AND catch all rutime errors that might come
//			try 
//			{
//				aw.restoredAlarms( getAlarmList() );
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The AlarmHandler got runtime error when calling the method restoredAlarms() in AlarmWriter named '"+aw.getName()+"'. Continuing with next AlarmWriter...", t);
//			}
//		}
//	}
	
//	/**
//	 */
//	public void beginOfSample()
//	{
//		// Call endOfSample() for all writers
//		Iterator writerIter = _writerClasses.iterator();
//		while (writerIter.hasNext()) 
//		{
//			IPersistWriter pw = (IPersistWriter) writerIter.next();
//
//			_logger.debug("Sending end-of-sample event to Persistent Writer named='"+pw.getName()+"'.");
//
//			// CALL THE installed Writer
//			// AND catch all runtime errors that might come
//			try 
//			{
//				pw.beginOfSample();
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The Persistent Writer got runtime error when calling the method endOfScan() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
//			}
//		}
//	}
//	
//	
//	/**
//	 */
//	public void endOfSample()
//	{
//		// Call endOfSample() for all writers
//		Iterator writerIter = _writerClasses.iterator();
//		while (writerIter.hasNext()) 
//		{
//			IPersistWriter pw = (IPersistWriter) writerIter.next();
//
//			_logger.debug("Sending end-of-sample event to Persistent Writer named='"+pw.getName()+"'.");
//
//			// CALL THE installed Writer
//			// AND catch all runtime errors that might come
//			try 
//			{
//				pw.endOfSample();
//			}
//			catch (Throwable t)
//			{
//				_logger.error("The Persistent Writer got runtime error when calling the method endOfScan() in Persistent Writer named '"+pw.getName()+"'. Continuing with next Writer...", t);
//			}
//		}
//	}
	
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
				_logger.info("Persisting Counters using '"+pw.getName()+"' for sampleTime='"+cont.getSampleTime()+"', server='"+cont.getServerName()+"'. Previous persist took "+prevConsumeTimeMs+" ms.");

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

				// SAVE-SESSION
				// In here we can "do it all" 
				// or use: beginOfSample(), saveDdl(), saveCounters(), endOfSample()
				pw.saveSession(cont);

				
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
