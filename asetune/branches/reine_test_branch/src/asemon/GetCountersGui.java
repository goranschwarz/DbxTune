/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

import java.awt.Component;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Iterator;

import org.apache.log4j.Logger;

import asemon.gui.MainFrame;
import asemon.utils.AseConnectionFactory;
import asemon.utils.Memory;

public class GetCountersGui
    extends GetCounters
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(GetCountersGui.class);

	public GetCountersGui()
	{
	}

	public void init()
	throws Exception
	{
//		String propPrefix = "nogui.";
//
//		_props = Asemon.getProps();
//		Configuration saveProps = Asemon.getSaveProps();
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


		if ( Asemon.getSaveProps().getBooleanProperty("conn.onStartup", false) )
		{
//			String srvName = Asemon.getSaveProps().getProperty("conn.serverName");
			String host    = Asemon.getSaveProps().getProperty("conn.hostname");
			String port    = Asemon.getSaveProps().getProperty("conn.port");
			String user    = Asemon.getSaveProps().getProperty("conn.username");
			String passwd  = Asemon.getSaveProps().getProperty("conn.password");

			if (host != null && port != null && user != null && passwd != null)
			{
				AseConnectionFactory.setAppName("asemon");
				AseConnectionFactory.setHost(host);
				AseConnectionFactory.setPort( Integer.parseInt(port) );
				AseConnectionFactory.setUser(user);
				AseConnectionFactory.setPassword(passwd);
				String aseServer = AseConnectionFactory.getServer(); 

				_logger.info("Connecting att startup to host='"+host+"', port='"+port+"', srvName='"+aseServer+"', user='"+user+"'.");
				try
				{
					Connection conn = AseConnectionFactory.getConnection();
					MainFrame.setMonConnection(conn);

					MainFrame.setStatus(MainFrame.ST_CONNECT);

					// Initilize the MonTablesDictionary
					// This will serv as a dictionary for ToolTip
					MonTablesDictionary.getInstance().initialize(MainFrame.getMonConnection());
					GetCounters.initExtraMonTablesDictionary();

					startRefresh();
				}
				catch (Exception e)
				{
					_logger.info("Error when connecting to server on startup. "+e.getMessage());
				}
			}
		}		
	}

	public void run()
	{
		boolean	firstTime	= true;

		// Set the Thread name
		setName("GetCountersGUI");
		_thread = Thread.currentThread();

		// loop
		int loopCounter = 0;

		while (true)
		{
			// Not connected, sleep a little less than the refresh interval
			// this to be more responsive for new connections.
			if ( ! MainFrame.isMonConnected() )
			{
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Not connected to any ASE server. Please connect now!");

				try { Thread.sleep(500); }
				catch (InterruptedException ignore) {}
				
				// START AT THE TOP, wait for a CONNECT
				continue;
			}

			loopCounter++;

			// When 10 MB of memory or less, write some info about that.
			Memory.checkMemoryUsage(10);

			MainFrame.setStatus(MainFrame.ST_MEMORY);

			try
			{
				// Sleep (if not first loop)
				if ( ! firstTime )
				{
					int sleepTime = MainFrame.getRefreshInterval();
					for (int i=sleepTime; i>0; i--)
					{
						if (MainFrame.getStatus(MainFrame.ST_STATUS_FIELD).startsWith("Sleeping for "))
							MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+i+" seconds, waiting for "+getWaitEvent());

						try { Thread.sleep(1000); }
						catch (InterruptedException ignore) 
						{
							// leave the sleep loop
							break;
						}
					} // end: sleep loop
				}
				firstTime = false;
				
				if ( ! refreshCntr )
					continue;


				// Initialize the counters, now when we know what 
				// know we are connected to
				if ( ! initialized )
				{
					initCounters( 
						MainFrame.getMonConnection(),
						MonTablesDictionary.getInstance().aseVersionNum,
						MonTablesDictionary.getInstance().aseMonTablesInstallVersionNum);
				}

				refreshing = true;
				Component comp = MainFrame.getActiveTab();
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing...");



				// Get session/head info
				String    aseServerName    = null;
				Timestamp mainSampleTime   = null;
				Timestamp counterClearTime = null;

				Statement stmt = MainFrame.getMonConnection().createStatement();
				ResultSet rs = stmt.executeQuery("select getdate(), @@servername, CountersCleared from master..monState");
				while (rs.next())
				{
					mainSampleTime   = rs.getTimestamp(1);
					aseServerName    = rs.getString(2);
					counterClearTime = rs.getTimestamp(3);
				}
				rs.close();
				stmt.close();


				//-----------------
				// Update data in tabs
				//-----------------

				// LOOP all CounterModels, and get new data, 
				//   if it should be done
				Iterator iter = _CMList.iterator();
				while (iter.hasNext())
				{
					CountersModel cm = (CountersModel) iter.next();
					
					if (cm == null)
					{
						_logger.info("CountersModel: IS NULL.");
						continue;
					}

					if ( ! cm.isSwingRefreshOK())
						continue;

					if ( ! cm.isActive() )
						continue;

					
					boolean refresh = false;

					if (cm.getName().equals("CMsummary"))
					{
						refresh = true;
					}
					else if ( cm.equalsTabPanel(comp) || cm.hasActiveGraphs() )
					{
						if ( ! cm.isDataPollingPaused() )
						{
							refresh = true;
						}
					}
					else if ( cm.isBackgroundDataPollingEnabled() )
					{
						refresh = true;
					}

					// REFRESH THE COUNTERS NOW
					if (refresh)
					{
						cm.setServerName(aseServerName);
						cm.setSampleTimeHead(mainSampleTime);
						cm.setCounterClearTime(counterClearTime);

						cm.refresh();
					}

					// Post processing if it's the Summary
					if (cm.getName().equals("CMsummary"))
					{
						MainFrame.setSummaryData(cm);
					}
					
					cm.endOfRefresh();
				}

				
				
				//-----------------
				// Update SUMMARY GRAPHS
				//-----------------
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Refreshing... Graphs...");

				_logger.debug("---- Refreshing... Graphs... ----");
				iter = _CMList.iterator();
				while (iter.hasNext())
				{
					CountersModel cm = (CountersModel) iter.next();
					
					if (cm != null)
					{
						// the background thread needs to complete
						cm.waitForSwingDataRefresh();

						if (cm.isSwingRefreshOK())
						{
							// Post processing if it's the Summary
							if ( cm.hasActiveGraphs() )
							{
								cm.updateGraphs();
							}
						}
						else
							_logger.debug(getName()+"cm.isSwingRefreshOK() == FALSE");
							
					}
				}

				refreshing = false;
				setWaitEvent("next sample period...");
				MainFrame.setStatus(MainFrame.ST_STATUS_FIELD, "Sleeping for "+MainFrame.getRefreshInterval()+" seconds.");
			}
//			catch (org.jfree.data.SeriesException jfsex)
//			{
//				//_logger.debug("asemon : error in GetCounters loop. "+jfsex);
//				refreshing = false;
//			}
			catch (Exception e)
			{
				//        System.out.println("asemon : error in GetCounters loop. "+e);
				_logger.error("asemon : error in GetCounters loop.", e);
				refreshing = false;
				//System.exit(1);
			}
		}
	}
}
