package com.asetune;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.h2.util.Profiler;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.asetune.cm.CountersModel;
import com.asetune.cm.LostConnectionException;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.WaitForExecDialog;
import com.asetune.gui.swing.WaitForExecDialog.BgExecutor;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.Memory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;
import com.asetune.utils.TimeUtils;

public class CounterCollectorThreadGui 
extends CounterCollectorThreadAbstract
{
	private static Logger _logger = Logger.getLogger(CounterCollectorThreadGui.class);

	public static final String THREAD_NAME = "CounterCollectorThreadGui";

	private long _lastJavaGcWasDoneAt = System.currentTimeMillis();

	/** means if we have ever been connected to any server, which then means we can do reconnect if we lost the connection */
	private boolean _canDoReconnect = false; 

	
	public CounterCollectorThreadGui(CounterControllerAbstract counterController)
	{
		super(counterController);
	}
	
	
	@Override
	public void init(boolean hasGui)
	throws Exception
	{
		Configuration conf    = Configuration.getCombinedConfiguration();

		boolean doProfile = Configuration.getCombinedConfiguration().getBooleanProperty("init.createCounters.profile", false);
		//doProfile = true;
		if (doProfile)
		{
			long start = System.currentTimeMillis();
			Profiler prof = new Profiler();
			prof.startCollecting();

			// Create all the CM objects, the objects will be added to _CMList
			getCounterController().createCounters(hasGui);

			prof.stopCollecting();
			long time = System.currentTimeMillis() - start;
			if (time > 5*1000)
			{
				String profStr = prof.getTop(3);
				_logger.info("createCounters(): time=" + time + ", profStr:\n" + profStr);
			}
		}
		else
		{
			long start = System.currentTimeMillis();
			
			// Create all the CM objects, the objects will be added to _CMList
			getCounterController().createCounters(hasGui);

			long time = System.currentTimeMillis() - start;
			_logger.info("Creating ALL CM Objects took " + TimeUtils.msToTimeStr("%MM:%SS.%ms", time) + " time format(MM:SS.ms)");
		}
			
		
		// Connect ON-STARTUP, use the ConnectionDialog for this, so we do not have
		// to duplicate the logic for this in here as well...
		// WARNING: if logic in ConnectionDialog changes, we might be in trouble
		// Override prop values with the Command line parameters if any. 
//		CommandLine cmd = AseTune.getCmdLineParams();
		String cmdLineUsername = null, cmdLinePassword = null, cmdLineServer = null;
		cmdLineUsername = conf.getProperty("cmdLine.dbmsUsername");
		cmdLinePassword = conf.getProperty("cmdLine.dbmsPassword");
		cmdLineServer   = conf.getProperty("cmdLine.dbmsServer");

		if (cmdLineUsername != null || cmdLinePassword != null || cmdLineServer != null)
		{
			String ppeStr = ConnectionDialog.PROPKEY_CONNECT_ON_STARTUP + 
				"={dbmsUsername="+ conf.getProperty("cmdLine.dbmsUsername") + 
				",dbmsPassword=" + conf.getProperty("cmdLine.dbmsPassword") + 
				",dbmsServer="   + conf.getProperty("cmdLine.dbmsServer") + 
				",sshUsername=" + conf.getProperty("cmdLine.sshUsername") + 
				",sshPassword=" + conf.getProperty("cmdLine.sshPassword") +
				",sshHostname=" + conf.getProperty("cmdLine.sshHostname") +
				",sshPort="     + conf.getProperty("cmdLine.sshPort") +
				"}";
			MainFrame mf = MainFrame.getInstance();
			if (mf != null)
			{
//				mf.action_connect(new ActionEvent(this, 1, ConnectionDialog.PROPKEY_CONNECT_ON_STARTUP +":"+cmdLineUsername+":"+cmdLinePassword+":"+cmdLineServer));
				mf.action_connect(new ActionEvent(this, 1, ppeStr));
			}
		}
		else if (hasGui)
		{
			if ( conf.getBooleanProperty(ConnectionDialog.PROPKEY_CONNECT_ON_STARTUP, ConnectionDialog.DEFAULT_CONNECT_ON_STARTUP) )
			{
				// Use the below info just to write the message of what we THINK the
				// ConnectionDialog will connect us to...
				String host    = conf.getProperty("conn.hostname");
				String port    = conf.getProperty("conn.port");
				String user    = conf.getProperty("conn.username");
				String aseServer = AseConnectionFactory.getServer();
				_logger.info("Connecting ON-STARTUP to host='"+host+"', port='"+port+"', srvName='"+aseServer+"', user='"+user+"'. This by using a non visible ConnectionDialog.");
	
				final MainFrame mf = MainFrame.getInstance();
				if (mf != null)
				{
//					mf.action_connect(new ActionEvent(this, 1, ConnectionDialog.PROPKEY_CONNECT_ON_STARTUP));
					
					// Make/Show the Connection dialog after the main window has been loaded
					Timer deferedAction = new Timer(250, new ActionListener() 
					{
						@Override
						public void actionPerformed(ActionEvent evt) 
						{
							mf.action_connect(new ActionEvent(CounterCollectorThreadGui.this, 1, ConnectionDialog.PROPKEY_CONNECT_ON_STARTUP));
						}    
					});
					deferedAction.setInitialDelay(250);
					deferedAction.setRepeats(false);
					deferedAction.start();
				}
			}
		}
	}

	@Override
	public void run()
	{
		boolean	  firstLoopAfterConnect = true;
//		boolean   canDoReconnect        = false; // means if we have ever been connected to any server, which then means we can do reconnect if we lost the connection
		int       reconnectProblemsSleeptSeconds = 0;

		// If you want to start a new session in the Persistent Storage, just set this to true...
		// This could for instance be used when you connect to a new ASE Server
		boolean startNewPcsSession = false;

		// Set the Thread name
//		setName("CounterCollectorThreadGui");
		setName(THREAD_NAME);

		_thread = Thread.currentThread();

		_running = true;
		_logger.info("Starting GetCounters GUI collector");

		// loop
		int  loopCounter = 0;
		long cmLastRefreshTime = 0; // number of milliseconds used to refresh data last time

		//---------------------------
		// START the InMemory Counter Handler
		//---------------------------
		InMemoryCounterHandler imch = null;
		try
		{
			imch = new InMemoryCounterHandler();
//			imch.init( Configuration.getInstance(Configuration.CONF) );
			imch.init( Configuration.getCombinedConfiguration() );
			InMemoryCounterHandler.setInstance(imch);

			// Register listeners
			imch.addChangeListener(MainFrame.getInstance());

			// Start it
			imch.start();
			
			// Start a watchdog for connection (issues isConnected() all the time...)
			if (getCounterController().shouldWeStart_connectionWatchDog())
				startIsMonConnectedWatchDog();
		}
		catch (Exception e)
		{
			_logger.error("Problems initializing InMemoryCounterHandler,", e);
		}

		_logger.info("Thread '"+Thread.currentThread().getName()+"' starting...");

		//---------------------------
		// NOW LOOP
		//---------------------------
		_lastJavaGcWasDoneAt = System.currentTimeMillis();
		while (_running)
		{
			MainFrame.getInstance().setStatus(MainFrame.ST_MEMORY);

			// are we in offline read mode
			if ( MainFrame.isOfflineConnected() )
			{
				MainFrame.getInstance().setStatus(MainFrame.ST_OFFLINE_CONNECT);
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Offline read mode, use the offline window to navigate.");

				getCounterController().sleep(500);
//				try { Thread.sleep(500); }
//				catch (InterruptedException ignore) {}

				// START AT THE TOP, wait for a CONNECT
				continue;
			}
			// NOT CONNECTED, sleep a little less than the refresh interval
			// this to be more responsive for new connections.
			else if ( ! getCounterController().isMonConnected() )
			{
//System.out.println("XXXXXX: -not-connected- threadName="+Thread.currentThread().getName());
				firstLoopAfterConnect = true;
				if ( ! _canDoReconnect )
					startNewPcsSession = true;

				MainFrame.getInstance().setStatus(MainFrame.ST_DISCONNECT);
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Not connected to any server. Please connect now!");

				getCounterController().sleep(500);

				// maybe do reconnect if the connection has been lost
				if (_canDoReconnect)
				{
					Configuration tmpConf = Configuration.getCombinedConfiguration();

					boolean optReconnectOnFailure = tmpConf.getBooleanProperty(ConnectionDialog.PROPKEY_RECONNECT_ON_FAILURE, ConnectionDialog.DEFAULT_RECONNECT_ON_FAILURE);
					if ( optReconnectOnFailure )
					{
						// Give up after X number of reconnects
						if (reconnectProblemsSleeptSeconds < 3600*1000) // Try for 1 hour
						{
							MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Trying to Re-connect to the monitored server.");

							// NOTE: we might have to do:
							//   mf.action_connect(new ActionEvent(this, 1, ConnectionDialog.PROPKEY_CONNECT_ON_STARTUP));
							// But for now just try to grab a connection and continue, revisit this later
							try
							{
//								Connection conn = AseConnectionFactory.getConnection("master", Version.getAppName(), Version.getVersionStr());
//								getCounterController().setMonConnection(conn);
//								getCounterController().getMonConnection().reConnect(MainFrame.getInstance());
								getCounterController().getMonConnection().reConnect(null);

//								String str = AseConnectionFactory.getServer() + " (" +
//								             AseConnectionFactory.getHost()   + ":" +
//								             AseConnectionFactory.getPort()   + ")";
//								String str = AseConnectionFactory.getServer() + " (" + AseConnectionFactory.getHostPortStr() + ")";

								String str = getCounterController().getMonConnection().getDbmsServerName();

								_logger.info("Re-connected to monitored server '"+str+"' after a 'lost connection'.");
								reconnectProblemsSleeptSeconds = 0;

								MainFrame.getInstance().setStatus(MainFrame.ST_CONNECT);
							}
							catch (Exception e)
							{
								_logger.warn("Problem when re-connecting to monitored server. Caught: "+e);
								_logger.debug("Problem when re-connecting to monitored server. Caught: "+e, e);
								MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Re-connect FAILED, I will soon try again.");

								// Send ALARM
								sendAlarmServerIsDown(null);
								
								// On connect failure sleep for a little longer
								int sleepTime = 5000;
								getCounterController().sleep(sleepTime);
								reconnectProblemsSleeptSeconds += sleepTime;
							}
						}
					}
				} // end: reconnect

				// START AT THE TOP, wait for a CONNECT
				continue;
			}

			// set a flag that we have been connected, which means that we can try to do
			// re-connect if we lost the connection
			if (getCounterController().isMonConnected())
			{
				_canDoReconnect = true;
			}

			// When 130 MB of memory or less, enable Java Garbage Collect after each Sample
			if (Memory.getMemoryLeftInMB() <= MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB)
			{
				ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_LOW_ON_MEMORY);
				MainFrame.getInstance().actionPerformed(doGcEvent);
			}

			// When 30 MB of memory or less, write some info about that.
			// and call some handler to act on low memory.
			if (Memory.checkMemoryUsage(MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB))
			{
				ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_OUT_OF_MEMORY);
				MainFrame.getInstance().actionPerformed(doGcEvent);
			}

			loopCounter++;
			try
			{
				if (firstLoopAfterConnect)
					loopCounter = 0;
				
//System.out.println("XXXXXXXXXXXXXXXXX: firstLoopAfterConnect="+firstLoopAfterConnect+", loopCounter="+loopCounter);
				// Sleep (if not first loop)
				if ( ! firstLoopAfterConnect )
				{
					boolean doJavaGcAfterXMinutes       = Configuration.getCombinedConfiguration().getBooleanProperty(MainFrame.PROPKEY_doJavaGcAfterXMinutes,       MainFrame.DEFAULT_doJavaGcAfterXMinutes);
					boolean doJavaGcAfterRefresh        = Configuration.getCombinedConfiguration().getBooleanProperty(MainFrame.PROPKEY_doJavaGcAfterRefresh,        MainFrame.DEFAULT_doJavaGcAfterRefresh);
					boolean doJavaGcAfterRefreshShowGui = Configuration.getCombinedConfiguration().getBooleanProperty(MainFrame.PROPKEY_doJavaGcAfterRefreshShowGui, MainFrame.DEFAULT_doJavaGcAfterRefreshShowGui);

					int doJavaGcAfterXMinutesValue      = Configuration.getCombinedConfiguration().getIntProperty(    MainFrame.PROPKEY_doJavaGcAfterXMinutesValue,  MainFrame.DEFAULT_doJavaGcAfterXMinutesValue);
					
					// If the GUI is NOT active, no need to show a progress window...
					// The progress window makes the "current" window to behave strange
					if ( ! MainFrame.getInstance().isActive() )
						doJavaGcAfterRefreshShowGui = false;
						
					int sleepTime = MainFrame.getRefreshInterval();
					getCounterController().setDefaultSleepTimeInSec(sleepTime);

					// First loop, we might want to be more responsive (not sleeping as long as we normally do)
					// This so we get some info at the graphs...
					if (loopCounter <= 3 && cmLastRefreshTime < 5000)
					{
						int shorterSleepTime = 5;
						if (sleepTime > shorterSleepTime)
						{
							_logger.info("Setting initial sleep time from "+sleepTime+" seconds to "+shorterSleepTime+" seconds the first 3 times we refresh data, this so graphs initially has some representive value.");
							sleepTime = shorterSleepTime;
						}
					}

					// If previous CHECK has DEMAND checks, lets sleep for a shorter while
					// This so we can catch data if the CM's are not yet initialized and has 2 samples (has diff data)
					sleepTime = getCounterController().getCmDemandRefreshSleepTime(sleepTime, getCounterController().getLastRefreshTimeInMs());
					
					// SLEEP in a loop
					for (int i=sleepTime; i>0; i--)
					{						
						boolean doJavaGc = false;
						if (doJavaGcAfterRefresh && (i == sleepTime-1)) // sleep first second before try do GC
							doJavaGc = true;

						if (doJavaGcAfterXMinutes)
						{
							long lastJavaGcTimeDiff = System.currentTimeMillis() - _lastJavaGcWasDoneAt;
							if (lastJavaGcTimeDiff > (doJavaGcAfterXMinutesValue*60*1000) )
								doJavaGc = true;

							if (_logger.isDebugEnabled())
								_logger.debug("lastJavaGcTimeDiff="+lastJavaGcTimeDiff+", Timeout is "+doJavaGcAfterXMinutesValue*60*1000+", doJavaGc="+doJavaGc);
						}

						// Do Java Garbage Collection?
						if (doJavaGc)
						{
							getCounterController().setWaitEvent("Doing Java Garbage Collection.");
							MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Doing Java Garbage Collection.");
							
							if (doJavaGcAfterRefreshShowGui)
							{
								WaitForExecDialog execWait = new WaitForExecDialog(MainFrame.getInstance(), "Forcing Java Garbage Collection.");
								execWait.setState("Note: This can be disabled from Menu->View->Preferences.");
								BgExecutor doWork = new BgExecutor(execWait)
								{
									@Override
									public Object doWork()
									{
										// just sleep 10ms, so the GUI will have a chance to become visible
										try {Thread.sleep(10);}
										catch(InterruptedException ignore) {}
	
										long gcStartAt = System.currentTimeMillis();
										System.gc();
										_lastJavaGcWasDoneAt = System.currentTimeMillis();

										_logger.debug("Just called: System.gc() and took "+(_lastJavaGcWasDoneAt - gcStartAt)+" ms. GUI progress was DISPLAYED.");
										return null;
									}
								};
								execWait.execAndWait(doWork, 200); // pupup if exec time is more than 200ms
							}
							else
							{
								long gcStartAt = System.currentTimeMillis();
								System.gc();
								_lastJavaGcWasDoneAt = System.currentTimeMillis();

								_logger.debug("Just called: System.gc() and took "+(_lastJavaGcWasDoneAt - gcStartAt)+" ms. NO gui progress...");
							}

							getCounterController().setWaitEvent("next sample period...");
							MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+i+" seconds, waiting for "+getCounterController().getWaitEvent());
						}

						if (MainFrame.getStatus(MainFrame.ST_STATUS_FIELD).startsWith("Sleeping for "))
							MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+i+" seconds, waiting for "+getCounterController().getWaitEvent());

						// Update Watermarks on all the CM tabs
//						for (CountersModel cm : _CMList)
						for (CountersModel cm : getCounterController().getCmList())
						{
							if (cm == null)
								continue;

							TabularCntrPanel tcp = cm.getTabPanel();
							if ( tcp != null)
							{
								tcp.setWatermark();
								tcp.checkLocalComponents();
							}
						}

						// Now SLEEP, return true on success, false on interrupted.
						if (MainFrame.isSamplingPaused()) // just stop update 'next sample is in x seconds', and use label 'Paused'
							break; // Sleep loop
						else
						{
							boolean ok = getCounterController().sleep(1000);
							if (!ok)
								break; // Sleep loop
						}

						MainFrame.getInstance().setStatus(MainFrame.ST_MEMORY);
						
						if ( ! getCounterController().isMonConnected() ) // Do not continue in here if we have hit disconnect button
							break; // no need to sleep if not connected...

					} // end: sleep loop
				}
				firstLoopAfterConnect = false;


				// Are we PAUSED, just sleep here
				while (MainFrame.isSamplingPaused() && MainFrame.isForcedRefresh()==false)
				{
					MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "PAUSED the data sampling. Press |> to continue... (or F5 / 'refresh' button to the left)");
					MainFrame.getInstance().setStatus(MainFrame.ST_MEMORY);
					getCounterController().sleep(1000); // changed to 1000 so that memory usage is updated more often
//					sleep(10000);
//					try { Thread.sleep(10000); }
//					catch (InterruptedException ignore)	{}

					// DUMMY to: TEST OUT-OF-MEMORY
					//ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_OUT_OF_MEMORY);
					//MainFrame.getInstance().actionPerformed(doGcEvent);
				}
				// Reset any forced refresh request
				MainFrame.setForcedRefresh(false);


				if ( ! getCounterController().isRefreshEnabled() )
					continue;


				// Initialize the counters, now when we know what
				// know we are connected to
				if ( ! getCounterController().isInitialized() )
				{
					MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Initializing all counters...");

					getCounterController().initCounters(
						getCounterController().getMonConnection(),
						true,
						MonTablesDictionaryManager.getInstance().getDbmsExecutableVersionNum(),
						MonTablesDictionaryManager.getInstance().isClusterEnabled(),
						MonTablesDictionaryManager.getInstance().getDbmsMonTableVersion());

					// emulate a slow INIT time...
					//try { Thread.sleep(7000); }
					//catch (InterruptedException ignore) {}

					MainFrame.getInstance().setStatus(MainFrame.ST_CONNECT);
				}

				// Have we reached the STOP/DISCONNECT Time
//				if (GetCounters.getInstance().getMonDisConnectTime() != null)
				if (CounterController.getInstance().getMonDisConnectTime() != null)
				{
					long now      = System.currentTimeMillis();
//					Date stopTime = GetCounters.getInstance().getMonDisConnectTime();
					Date stopTime = CounterController.getInstance().getMonDisConnectTime();

					if ( now > stopTime.getTime())
					{
//						String startDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(GetCounters.getInstance().getMonConnectionTime());
						String startDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(CounterController.getInstance().getMonConnectionTime());
						String stopDateStr  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(stopTime);
						String nowStr       = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(now));

//						long collectionTime = now - GetCounters.getInstance().getMonConnectionTime().getTime();
						long collectionTime = now - CounterController.getInstance().getMonConnectionTime().getTime();

						_logger.info("Disconnect from ASE. Stop time was set to '"+stopDateStr+"'. It was started at '"+startDateStr+"'.");
						MainFrame.getInstance().action_disconnect();

						SwingUtils.showInfoMessage(MainFrame.getInstance(), "Disconnected", 
								"<html>" +
								"<h1>Intentional Disconnect from ASE.</h1>" +
								"<br>" +
								"Connection was started at: <b>"+startDateStr+"</b> <br>" +
								"Stop/disconnect time was set to: <b>"+stopDateStr+"</b> <br>" +
								"<br>" +
								"Collection time was: "+TimeUtils.msToTimeStr("<b>%HH</b> Hours and <b>%MM</b> Minutes", collectionTime)+" <br>" +
								"<br>" +
								"Time is now: <b>"+nowStr+"</b> <br>" +
								"</html>");
						break;
					}
				}

				getCounterController().setInRefresh(true);

				getCounterController().setDefaultSleepTimeInSec(MainFrame.getRefreshInterval());

				//Component comp = MainFrame.getActiveTab();
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing...");

				// Set the CM's to be in normal refresh state
				for (CountersModel cm : getCounterController().getCmList())
				{
					if (cm == null)
						continue;

					cm.setState(CountersModel.State.NORMAL);
				}

				// Get the CounterStorage if we have any
				PersistentCounterHandler pcs = PersistentCounterHandler.getInstance();

				// Do various other checks in the system, for instance in:
				//    - ASE do: checkForFullTransLogInMaster()
				//    - RS  do: checkIfWeAreInGatewayMode
				getCounterController().checkServerSpecifics();

				// Get some Header information that will be used by the PersistContainer sub system
				PersistContainer.HeaderInfo headerInfo = getCounterController().createPcsHeaderInfo();
				if (headerInfo == null)
					continue;

				// If there is a ServerAlias, apply that... This is used by the DbxCentral for an alternate schema/servername
//				if (StringUtil.hasValue(_dbmsServerAlias))
//					headerInfo.setServerNameAlias(_dbmsServerAlias);
				
				
				// PCS
				PersistContainer pc = null;
				if (pcs != null || imch != null)
				{
//					pc = new PersistContainer(mainSampleTime, aseServerName, aseHostname);
					pc = new PersistContainer(headerInfo);
					if (startNewPcsSession)
						pc.setStartNewSample(true);
					startNewPcsSession = false;
				}
				
				// add some statistics on the "main" sample level
				getCounterController().setStatisticsTime(headerInfo.getMainSampleTime());

				// Set SERVERNAME to where we are currently connected 
				if ( StringUtil.isNullOrBlank(headerInfo.getServerNameOrAlias()) )
					_logger.warn("DBMS Server Name is null. Please set the server in ICounterController method: createPcsHeaderInfo()");
				else
					System.setProperty("SERVERNAME", DbxTune.stripSrvName(headerInfo.getServerNameOrAlias()));
				
				//-----------------
				// Update data in tabs
				//-----------------

				// Clear the demand refresh list
				getCounterController().clearCmDemandRefreshList();

				// Keep a list of all the CM's that are refreshed during this loop
				// This one will be passed to doPostRefresh()
				LinkedHashMap<String, CountersModel> refreshedCms = new LinkedHashMap<String, CountersModel>();

//System.out.println("main: ----------------------------------------------------------------------------------------------");
				// LOOP all CounterModels, and get new data,
				//   if it should be done
				long cmRefreshStartTime = System.currentTimeMillis();
				for (CountersModel cm : getCounterController().getCmList())
				{
					if (cm == null)
					{
						_logger.info("CountersModel: IS NULL.");
						continue;
					}

					if ( cm.getTabPanel() != null)
					{
						if (pcs == null)
							cm.getTabPanel().enableOptionPersistCounters(false);
						else
							cm.getTabPanel().enableOptionPersistCounters(true);
					}

					//-------------------------------------
					// SHOULD WE REFRESH OR NOT
					//-------------------------------------
					if ( cm.isRefreshable() )
					{
						if (headerInfo != null)
						{
							cm.setServerName(      headerInfo.getServerNameOrAlias()); // or should we just use getServerName()
							cm.setSampleTimeHead(  headerInfo.getMainSampleTime());
							cm.setCounterClearTime(headerInfo.getCounterClearTime());
						}

						try
						{
//System.out.println("############################## main: do-refresh: "+cm.getDisplayName());
							MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing... "+cm.getDisplayName());
							cm.setSampleException(null);
							cm.refresh();
							
							// Add it to the list of refreshed cm's
							refreshedCms.put(cm.getName(), cm);

							// move this into cm.refresh()
							//cm.setValidSampleData( (cm.getRowCount() > 0) ); 
	
							// Add the CM to the container, which will
							// be posted to persister thread later.
							if ( (pc != null && cm.isPersistCountersEnabled()) || imch != null )
							{
								pc.add(cm);
							}
						}
						catch (LostConnectionException ex)
						{
							cm.setSampleException(ex);

							// Try to re-connect, otherwise we might "cancel" some ongoing alarms (due to the fact that we do 'end-of-scan' at the end of the loop)
							_logger.info("Try reconnect. When refreshing the data for cm '"+cm.getName()+"', we got 'LostConnectionException'.");
							DbxConnection conn = getCounterController().getMonConnection();
							if (conn != null)
							{
								try
								{
									conn.close();
									conn.reConnect(null);
									_logger.info("Succeeded: reconnect. continuing to refresh data for next CM.");
								}
								catch(Exception reconnectEx)
								{
									_logger.error("Problem when reconnecting. Caught: "+reconnectEx);
								}
							}
							// If we got an exception, go and check if we are still connected
							if ( ! getCounterController().isMonConnected(true, true) ) // forceConnectionCheck=true, closeConnOnFailure=true
							{
								_logger.warn("Breaking check loop, due to 'not-connected' (after trying to re-connect). Next check loop will do new connection. When refreshing the data for cm '"+getName()+"', we Caught an Exception and we are no longer connected to the monitored server.");
								break; // break: LOOP CM's
							}
						}
						catch (Exception ex)
						{
							cm.setSampleException(ex);

							// move this into cm.refresh()
							//cm.setValidSampleData(false); 
						}
					}
//					else
//					{
//						// move this into cm.shouldRefresh()
//						cm.setValidSampleData(false); 
//					}

					// NOTE: Post processing if it's the Summary is now done by listeners, 
					//       which was setup in GetCounters when adding/creating CMsummary

					cm.endOfRefresh();

				} // END: LOOP all CounterModels, and get new data
				
				cmLastRefreshTime = System.currentTimeMillis() - cmRefreshStartTime;


				//---------------------------------------------------
				// POSTPROCESSING -  Refresh handling
				//---------------------------------------------------
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Post Refreshing...");

				_logger.debug("---- Do POST Refreshing...");
				for (CountersModel cm : getCounterController().getCmList())
				{
					if ( cm == null )
						continue;

					MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Post Refreshing... "+cm.getDisplayName());

					cm.doPostRefresh(refreshedCms);
				}

				
				//-----------------
				// Update SUMMARY GRAPHS
				//-----------------
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing... Graphs...");

				_logger.debug("---- Refreshing... Graphs... ----");
				for (CountersModel cm : getCounterController().getCmList())
				{
					// Post processing if it's the Summary
					if ( cm != null && cm.hasActiveGraphs() )
					{
						// FIXME: maybe do this in the EDT (AWT-EventQueue-0)
						//cm.updateGraphs();
						
						// Execute cm.updateGraphs in the EventDispatcher Thread (if not already there)
						final CountersModel fcm = cm;
						Runnable doWork = new Runnable()
						{
							@Override
							public void run()
							{
								fcm.updateGraphs();
							}
						};
						if ( ! SwingUtilities.isEventDispatchThread() )
							SwingUtilities.invokeLater(doWork);
						else
							doWork.run();
					}
				}

				//-----------------
				// AlarmHandler: end-of-scan: Cancel any alarms that has NOT been repeated
				//-----------------
				if (AlarmHandler.hasInstance())
				{
					AlarmHandler ah = AlarmHandler.getInstance();

					ah.endOfScan();           // This is synchronous operation (if we want to stuff Alarms in the PersistContainer before it's called/sent)
//					ah.addEndOfScanToQueue(); // This is async operation

					// Wait for the above end-of-schan to be executed, but wait for max 100ms
					// Note: This will block the Event Dispatch Thread from updating the GUI in 100ms
//					long waitTime = AlarmHandler.getInstance().waitForQueueEndOfScan(100); 
//
//					if (_logger.isDebugEnabled())
//						_logger.debug("waitForQueueEndOfScan(): waitTime="+waitTime);

					// Add Active alarms to the Persist Container.
					pc.addActiveAlarms(ah.getAlarmList());
					
					// Add Alarm events that has happened in this sample. (RASIE/RE-RAISE/CANCEL)
					if ( AlarmWriterToPcsJdbc.hasInstance() )
					{
						List<AlarmEventWrapper> alarmEvents = AlarmWriterToPcsJdbc.getInstance().getList();
						pc.addAlarmEvents(alarmEvents);
						// Note: AlarmWriterToPcsJdbc.getInstance().clear(); is done in PersistContainerHandler after each container entry is handled
					}
				}

				//-----------------
				// POST the container to the Persistent Counter Handler
				// That thread will store the information in any Storage.
				//-----------------
				if (pcs != null)
					pcs.add(pc);

				// POST/Add to the history queue
				if (imch != null)
					imch.add(pc);


				// NO Longer in refresh mode
				getCounterController().setInRefresh(false);

				//----------------------------------------------------
				// Post checking: should we reconnect to the server
				//----------------------------------------------------
				DbxConnection conn = getCounterController().getMonConnection();
				if (conn != null && conn.isConnectionMarked(DbxConnection.MarkTypes.MarkForReConnect))
				{
					_logger.warn("The Connection to the monitored DBMS has been marked for 're-connect'. So lets close the connection, and open a new one.");
					try
					{
						conn.clearConnectionMark(DbxConnection.MarkTypes.MarkForReConnect);
						conn.close();
						conn.reConnect( MainFrame.hasInstance() ? MainFrame.getInstance() : null );
					}
					catch (Exception ex)
					{
						_logger.error("Problems during 're-connect' after a sample is finished. Caught: "+ex);
					}
				}

				getCounterController().setWaitEvent("next sample period...");
				MainFrame.getInstance().setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+MainFrame.getRefreshInterval()+" seconds.");
			}
			catch (Exception e)
			{
				// System.out.println(Version.getAppName()+" : error in GetCounters loop. "+e);
				_logger.error(Version.getAppName()+" : error in GetCounters loop ("+e.getMessage()+").", e);
			}
			finally
			{
				getCounterController().setInRefresh(false);
			}
		} // END: while(_running)

		_logger.info("Thread '"+Thread.currentThread().getName()+"' ending...");
	}

	/**
	 * This will hopefully help, the GUI a bit, meaning it wont "freeze" for short periods<br>
	 * The isMonConnected will just return, with the "cached" value...
	 */
	private Thread _isMonConnectedWatchDog = null;
	private void startIsMonConnectedWatchDog()
	{
//		boolean startIsMonConnectedWatchDog = Configuration.getCombinedConfiguration().getBooleanProperty("startIsMonConnectedWatchDog", true);
//		if (startIsMonConnectedWatchDog)
//		{
//		}
//		}
//		else
//		{
//			_logger.info("NO START OF 'isMonConnectedWatchDog' thread.");
//		}
		_isMonConnectedWatchDog = new Thread()
		{
			@Override
			public void run() 
			{
				_logger.info("Starting up 'is monitor connected' background thread...");
				
				while(true)
				{
					try
					{
						// If the monitor is "in refresh", we don't need/want to check
						// It might cause blocking things if _conn.isClosed() is called
						// but right now, I'm doing 'select 1' in my own isClosed(conn) check...
						if ( ! getCounterController().isRefreshing() )
						{
							getCounterController().isMonConnected(true, true); // forceConnectionCheck, closeConnOnFailure
//								_isMonConnectedWatchDogLastCheck = System.currentTimeMillis();
						}

						Thread.sleep(1000);
					}
					catch(Throwable t)
					{
						_logger.info("isMonConnectedWatchDog: caught: "+t);
					}
				}
			};
		};
		
		_isMonConnectedWatchDog.setName("isMonConnectedWatchDog");
		_isMonConnectedWatchDog.setDaemon(true);

		_isMonConnectedWatchDog.start();
	}

	/** called when GetCounters.closeMonConnection() */
	@Override
	public void cleanupMonConnection()
	{
		super.cleanupMonConnection();
		_canDoReconnect = false;
	}
	
}
