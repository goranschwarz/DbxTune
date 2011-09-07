/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune;

import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;

import com.asetune.cm.CountersModel;
import com.asetune.gui.ConnectionDialog;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.pcs.InMemoryCounterHandler;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.Memory;


public class GetCountersGui
    extends GetCounters
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(GetCountersGui.class);

	public GetCountersGui()
	{
		super.setInstance(this);
	}

	public void init()
	throws Exception
	{
//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
//		Configuration conf    = Configuration.getInstance(Configuration.CONF);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		Configuration conf    = Configuration.getCombinedConfiguration();
//		String propPrefix = "nogui.";
//
//		_props = AseTune.getProps();
//		Configuration saveProps = AseTune.getSaveProps();
//
//		if ( _props == null )
//		{
//			throw new Exception("No configuration was initiated.");
//		}
//
//		// WRITE init message, jupp a little late, but I wanted to grab the _name
//		_logger.info("Initializing the NO-GUI sampler component.");

		// Create all the CM objects, the objects will be added to _CMList
		this.createCounters();

		// Connect ON-STARTUP, use the ConnectionDialog for this, so we do not have
		// to duplicate the logic for this in here as well...
		// WARNING: if logic in ConnectionDialog changes, we might be in trouble
		// Override prop values with the Command line parameters if any. 
//		CommandLine cmd = AseTune.getCmdLineParams();
		String cmdLineUsername = null, cmdLinePassword = null, cmdLineServer = null;
		cmdLineUsername = conf.getProperty("cmdLine.aseUsername");
		cmdLinePassword = conf.getProperty("cmdLine.asePassword");
		cmdLineServer   = conf.getProperty("cmdLine.aseServer");

		if (cmdLineUsername != null || cmdLinePassword != null || cmdLineServer != null)
		{
			String ppeStr = ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP + 
				"={aseUsername="+ conf.getProperty("cmdLine.aseUsername") + 
				",asePassword=" + conf.getProperty("cmdLine.asePassword") + 
				",aseServer="   + conf.getProperty("cmdLine.aseServer") + 
				",sshUsername=" + conf.getProperty("cmdLine.sshUsername") + 
				",sshPassword=" + conf.getProperty("cmdLine.sshPassword") +
				",sshHostname=" + conf.getProperty("cmdLine.sshHostname") +
				",sshPort="     + conf.getProperty("cmdLine.sshPort") +
				"}";
			MainFrame mf = MainFrame.getInstance();
			if (mf != null)
			{
//				mf.action_connect(new ActionEvent(this, 1, ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP +":"+cmdLineUsername+":"+cmdLinePassword+":"+cmdLineServer));
				mf.action_connect(new ActionEvent(this, 1, ppeStr));
			}
		}
		else
		{
			if ( tmpConf.getBooleanProperty(ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP, false) )
			{
				// Use the below info just to write the message of what we THINK the
				// ConnectionDialog will connect us to...
				String host    = tmpConf.getProperty("conn.hostname");
				String port    = tmpConf.getProperty("conn.port");
				String user    = tmpConf.getProperty("conn.username");
				String aseServer = AseConnectionFactory.getServer();
				_logger.info("Connecting ON-STARTUP to host='"+host+"', port='"+port+"', srvName='"+aseServer+"', user='"+user+"'. This by using a non visible ConnectionDialog.");
	
				MainFrame mf = MainFrame.getInstance();
				if (mf != null)
				{
					mf.action_connect(new ActionEvent(this, 1, ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP));
				}
			}
		}
	}

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
	public void setMonConnection(Connection conn)
	{
		super.setMonConnection(conn);
		MainFrame.setStatus(MainFrame.ST_CONNECT);
	}

	public void run()
	{
		boolean	  firstLoopAfterConnect = true;
		boolean   canDoReconnect        = false; // means if we have ever been connected to any server, which then means we can do reconnect if we lost the connection
		int       reconnectProblems     = 0;

		// If you want to start a new session in the Persistent Storage, just set this to true...
		// This could for instance be used when you connect to a new ASE Server
		boolean startNewPcsSession = false;

		// Set the Thread name
		setName("GetCountersGUI");
		_thread = Thread.currentThread();

		// loop
		int loopCounter = 0;

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
		}
		catch (Exception e)
		{
			_logger.error("Problems initializing InMemoryCounterHandler,", e);
		}

		//---------------------------
		// NOW LOOP
		//---------------------------
		while (true)
		{
			MainFrame.setStatus(MainFrame.ST_MEMORY);

			// are we in offline read mode
			if ( MainFrame.isOfflineConnected() )
			{
				MainFrame.setStatus(MainFrame.ST_OFFLINE_CONNECT);
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Offline read mode, use the offline window to navigate.");

				try { Thread.sleep(500); }
				catch (InterruptedException ignore) {}

				// START AT THE TOP, wait for a CONNECT
				continue;
			}
			// NOT CONNECTED, sleep a little less than the refresh interval
			// this to be more responsive for new connections.
			else if ( ! isMonConnected() )
			{
				firstLoopAfterConnect = true;
				if ( ! canDoReconnect )
					startNewPcsSession = true;

				MainFrame.setStatus(MainFrame.ST_DISCONNECT);
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Not connected to any server. Please connect now!");

				try { Thread.sleep(500); }
				catch (InterruptedException ignore) {}

				// maybe do reconnect if the connection has been lost
				if (canDoReconnect)
				{
//					Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
					Configuration tmpConf = Configuration.getCombinedConfiguration();

					// null: means that reset() has ben called, which is done on a disconnect
					Map<String, List<String>> aseHostPortMap = AseConnectionFactory.getHostPortMap();
					boolean optReconnectOnFailure = tmpConf.getBooleanProperty(ConnectionDialog.CONF_OPTION_RECONNECT_ON_FAILURE, false);
					if ( optReconnectOnFailure && aseHostPortMap != null )
					{
						// Give up after X number of reconnects
						if (reconnectProblems < 100)
						{
							// NOTE: we might have to do:
							//   mf.action_connect(new ActionEvent(this, 1, ConnectionDialog.CONF_OPTION_CONNECT_ON_STARTUP));
							// But for now just try to grab a connection and continue, revisit this later
							try
							{
								Connection conn = AseConnectionFactory.getConnection("master", Version.getAppName(), Version.getVersionStr());
								setMonConnection(conn);

//								String str = AseConnectionFactory.getServer() + " (" +
//								             AseConnectionFactory.getHost()   + ":" +
//								             AseConnectionFactory.getPort()   + ")";
								String str = AseConnectionFactory.getServer() + " (" + AseConnectionFactory.getHostPortStr() + ")";

								_logger.info("Re-connected to monitored server '"+str+"' after a 'lost connection'.");
								reconnectProblems = 0;
							}
							catch (Exception e)
							{
								reconnectProblems++;
								_logger.warn("Problem when re-connecting to monitored server. Caught: "+e);
								_logger.debug("Problem when re-connecting to monitored server. Caught: "+e, e);
								MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Re-connect FAILED, I will soon try again.");

								// On connect failure sleep for a little longer
								try { Thread.sleep(5000); }
								catch (InterruptedException ignore) {}
							}
						}
					}
				} // end: reconnect

				// START AT THE TOP, wait for a CONNECT
				continue;
			}

			// set a flag that we have been connected, which means that we can try to do
			// re-connect if we lost the connection
			if (isMonConnected())
			{
				canDoReconnect = true;
			}

			loopCounter++;

			// When 10 MB of memory or less, write some info about that.
			// and call some handler to act on low memory.
			if (Memory.checkMemoryUsage(10))
			{
				ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_OUT_OF_MEMORY);
				MainFrame.getInstance().actionPerformed(doGcEvent);
			}

			try
			{
				// Sleep (if not first loop)
				if ( ! firstLoopAfterConnect )
				{
					int sleepTime = MainFrame.getRefreshInterval();
					for (int i=sleepTime; i>0; i--)
					{
						if (MainFrame.getStatus(MainFrame.ST_STATUS_FIELD).startsWith("Sleeping for "))
							MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+i+" seconds, waiting for "+getWaitEvent());

						// Update Watermarks on all the CM tabs
						for (CountersModel cm : _CMList)
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

						// Now SLEEP
						try { Thread.sleep(1000); }
						catch (InterruptedException ignore)
						{
							// leave the sleep loop
							break;
						}
					} // end: sleep loop
				}
				firstLoopAfterConnect = false;


				// Are we PAUSED, just sleep here
				while (MainFrame.isSamplingPaused())
				{
					MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "PAUSED the data sampling. Press |> to continue...");
					try { Thread.sleep(10000); }
					catch (InterruptedException ignore)	{}
				}


				if ( ! isRefreshEnabled() )
					continue;


				// Initialize the counters, now when we know what
				// know we are connected to
				if ( ! isInitialized() )
				{
					MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Initializing all counters...");

					initCounters(
						getMonConnection(),
						true,
						MonTablesDictionary.getInstance().aseVersionNum,
						MonTablesDictionary.getInstance().isClusterEnabled,
						MonTablesDictionary.getInstance().montablesVersionNum);

					// emulate a slow INIT time...
					//try { Thread.sleep(7000); }
					//catch (InterruptedException ignore) {}

					MainFrame.setStatus(MainFrame.ST_CONNECT);
				}

				
				//----------------------
				// In some versions we need to check if the transaction log is full to some reasons
				// If it is full it will be truncated.
				//----------------------
				checkForFullTransLogInMaster(getMonConnection());

				setRefreshingCounters(true);
				//Component comp = MainFrame.getActiveTab();
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing...");


				// Get the CounterStorage if we have any
				PersistentCounterHandler pcs = PersistentCounterHandler.getInstance();

				// Get session/head info
				String    aseServerName    = null;
				String    aseHostname      = null;
				Timestamp mainSampleTime   = null;
				Timestamp counterClearTime = null;

				Statement stmt = getMonConnection().createStatement();
				String sql = "select getdate(), @@servername, 'ONLY_ABOVE_ASE_1502', CountersCleared from master..monState";
				if (MonTablesDictionary.getInstance().aseVersionNum >= 15020)
					sql =    "select getdate(), @@servername, asehostname(),         CountersCleared from master..monState";
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next())
				{
					mainSampleTime   = rs.getTimestamp(1);
					aseServerName    = rs.getString(2);
					aseHostname      = rs.getString(3);
					counterClearTime = rs.getTimestamp(4);
				}
				rs.close();
				stmt.close();

				// PCS
				PersistContainer pc = null;
				if (pcs != null || imch != null)
				{
					pc = new PersistContainer(mainSampleTime, aseServerName, aseHostname);
					if (startNewPcsSession)
						pc.setStartNewSample(true);
					startNewPcsSession = false;
				}

				//-----------------
				// Update data in tabs
				//-----------------

				// LOOP all CounterModels, and get new data,
				//   if it should be done
				for (CountersModel cm : _CMList)
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
						cm.setServerName(aseServerName);
						cm.setSampleTimeHead(mainSampleTime);
						cm.setCounterClearTime(counterClearTime);

						try
						{
							MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing... "+cm.getDisplayName());
							cm.setSampleException(null);
							cm.refresh();

							// move this into cm.refresh()
							//cm.setValidSampleData( (cm.getRowCount() > 0) ); 
	
							// Add the CM to the container, which will
							// be posted to persister thread later.
							if ( (pc != null && cm.isPersistCountersEnabled()) || imch != null )
							{
								pc.add(cm);
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

				
				// POST the container to the Persistent Counter Handler
				// That thread will store the information in any Storage.
				if (pcs != null)
					pcs.add(pc);

				// POST/Add to the history queue
				if (imch != null)
					imch.add(pc);


				//-----------------
				// Update SUMMARY GRAPHS
				//-----------------
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing... Graphs...");

				_logger.debug("---- Refreshing... Graphs... ----");
				for (CountersModel cm : _CMList)
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

				setRefreshingCounters(false);
				setWaitEvent("next sample period...");
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+MainFrame.getRefreshInterval()+" seconds.");
			}
			catch (Exception e)
			{
				//        System.out.println(Version.getAppName()+" : error in GetCounters loop. "+e);
				_logger.error(Version.getAppName()+" : error in GetCounters loop ("+e.getMessage()+").", e);
				setRefreshingCounters(false);
				//System.exit(1);
			}
		}
	}
}
