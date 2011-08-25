/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Iterator;

import org.apache.log4j.Logger;

import asemon.pcs.PersistContainer;
import asemon.pcs.PersistentCounterHandler;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.Memory;

/**
 * FIXME: This has NOT yet been developed...
 * @author gorans
 */
public class GetCountersNoGui
    extends GetCounters
{
	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(GetCountersNoGui.class);

	/** a connection to the ASE server to monitor */
	private Connection _monConn     = null;

	/** sleep time between samples */
	private int        _sleepTime   = 60;

	/** If no connection can be made to the ASE server, sleep time before retry */
	private int        _sleepOnFailedConnectTime = 60;

	/** Connection information */
	private String     _aseUsername = null;
	private String     _asePassword = null; 
	private String     _aseServer   = null;
	private String     _aseHostname = null;
	private String     _asePort     = null;

//	private Configuration _props      = null;
	private Configuration _storeProps = null;
	
	private boolean       _running = true;

	public GetCountersNoGui()
	{
	}

	public void init()
	throws Exception
	{
		String offlinePrefix = "offline.";
		String CMpropPrefix  = "CM.";
		String connPrefix    = "conn.";

		//_props = Asemon.getProps();
		//Configuration saveProps = Asemon.getSaveProps();
		_storeProps = Asemon.getStoreProps();

//		if ( _props == null )
//		{
//			throw new Exception("No configuration was initiated.");
//		}
		if ( _storeProps.size() == 0 )
		{
			throw new Exception("No persistent storage configuration was found. Persistent storage are configured via -Dasemon.store.config");
		}
		
		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the NO-GUI sampler component.");

		// PROPERTY: sleepTime
		String sleepTimeStr = _storeProps.getMandatoryProperty(offlinePrefix + "sampleTime");

		if (sleepTimeStr == null)
			sleepTimeStr = System.getProperty("asemon.nogui.sleepTime");
		
//		if (sleepTimeStr == null)
//			sleepTimeStr = _props.getProperty("nogui.sleepTime");

//		if (sleepTimeStr == null && saveProps != null)
//			sleepTimeStr = saveProps.getProperty("nogui.sleepTime");

		if (sleepTimeStr != null)
			_sleepTime = Integer.parseInt(sleepTimeStr);
			

		
		// PROPERTY: sleepOnFailedConnectTime
//		_sleepOnFailedConnectTime = _props.getIntProperty(offlinePrefix+"sleepOnFailedConnectTime", _sleepOnFailedConnectTime);

		
		// PROPERTY: username, password, server
		_aseUsername  = _storeProps.getMandatoryProperty(connPrefix+"aseUsername");
		_asePassword  = _storeProps.getMandatoryProperty(connPrefix+"asePassword"); 
		_aseHostname  = _storeProps.getMandatoryProperty(connPrefix+"aseHost");
		_asePort      = _storeProps.getMandatoryProperty(connPrefix+"asePort");
		_aseServer    = _storeProps.getMandatoryProperty(connPrefix+"aseName");

		// treat null password
		if (_asePassword.equalsIgnoreCase("null"))
			_asePassword = "";
		
		// Get host/port from interfaces file if not specified in the form HOST:PORT
		String aseServerUrl = _aseServer;
		if ( aseServerUrl.indexOf(":") == -1 )
		{
			aseServerUrl = AseConnectionFactory.resolvInterfaceEntry(_aseServer);
			if (aseServerUrl == null)
			{
				_logger.info("Can't resolv or find ASE Server named '"+_aseServer+"' in the interfaces/sql.ini file.");
//				throw new Exception("Can't resolv or find ASE Server named '"+_aseServer+"' in the interfaces/sql.ini file.");
				aseServerUrl = _aseHostname + ":" + _asePort;
			}
		}
		else
		{
			aseServerUrl = _aseHostname + ":" + _asePort;
		}

		// host:port parsing
		String[] aseServerUrlArray = aseServerUrl.split(":");
		if (aseServerUrlArray.length != 2)
		{
			throw new Exception("The ASE Server connection specification '"+aseServerUrl+"' is in a faulty format. The format should be 'hostname:port'.");
		}
		_aseHostname = aseServerUrlArray[0];
		_asePort     = aseServerUrlArray[1];

		String configStr = "sleepTime='"+_sleepTime+"', sleepOnFailedConnectTime='"+_sleepOnFailedConnectTime+"', _aseUsername='"+_aseUsername+"', _asePassword='"+_asePassword+"', _aseServer='"+_aseServer+"("+_aseHostname+":"+_asePort+")'.";
		_logger.info("Configuration for NO-GUI sampler: "+configStr);
		
		
		// Create all the CM objects, the objects will be added to _CMList
		this.createCounters();

		//-----------------
		// LOOP all CounterModels, and remove if config indicates it should not be sampled, 
		//-----------------
		Iterator iter = _CMList.iterator();
		while (iter.hasNext())
		{
			CountersModel cm = (CountersModel) iter.next();
			String key = CMpropPrefix + cm.getName() + ".sample";
			if (cm != null)
			{
				if ( _storeProps.getBooleanProperty(key, false) == false )
				{
					cm.setActive(false, "Inactivated by offline config");
				}
			}
		}
	}

	public void run()
	{
		// Set the Thread name
		setName("GetCountersNoGui");
		_thread = Thread.currentThread();

		// loop
		int loopCounter = 0;

		MonTablesDictionary mtd = null;
		

		//---------------------------
		// START the Persistent Storage thread
		//---------------------------
		PersistentCounterHandler pch = null;
		try
		{
			pch = new PersistentCounterHandler();
			pch.init( _storeProps );
			pch.start();
		}
		catch (Exception e)
		{
			_logger.error("Problems initializing PersistentCounterHandler,", e);
			return;
		}
		if ( ! pch.hasWriters() )
		{
			_logger.error("No writers installed to the PersistentCounterHandler, this is NO-GUI... So I do not see the need for me to start.");
			return;
		}

		//---------------------------
		// NOW LOOP
		//---------------------------
		while (_running)
		{
			// Check if current MONITOR-ASE connection is lost
			if (_monConn != null)
			{
				try
				{
					if ( _monConn.isClosed() )
					{
						try {_monConn.close();}
						catch(SQLException ignore) {}
						finally {_monConn = null;}
					}
				}
				catch (SQLException ignore) {}
			}

			// CONNECT (initial or reconnect)
			if (_monConn == null)
			{
				_logger.debug("Connecting to ASE server using. user='"+_aseUsername+"', passwd='"+_asePassword+"', host='"+_aseHostname+"', port='"+_asePort+"'. aseServer='"+_aseServer+"'");
				_logger.info( "Connecting to ASE server using. user='"+_aseUsername+"', host='"+_aseHostname+"', port='"+_asePort+"'. aseServer='"+_aseServer+"'");

//				_monConn = OpenConnectionDlg.openConnection(null, Version.getAppName()+"-nogui", _aseUsername, _asePassword, _aseHostname, _asePort, false);

				// get a connection
				try
				{
					_monConn = AseConnectionFactory.getConnection(_aseHostname, Integer.parseInt(_asePort), null, _aseUsername, _asePassword, Version.getAppName()+"-nogui");
					
					AseConnectionFactory.setHost(_aseHostname);

					// CHECK the connection for proper configuration.
					if ( ! AseConnectionUtils.checkForMonitorOptions(_monConn, _aseUsername, false, null) )
					{
						_logger.error("Problems when checking the ASE Server for 'proper monitoring configuration'.");

						// Disconnect, and get out of here...
						_monConn.close();
						_monConn = null;
						
						// THE LOOP WILL BE FALSE (_running = false)
						_running = false;

						// START AT THE TOP AGAIN
						continue;
					}
				}
				catch (SQLException e)
				{
					String msg = AseConnectionUtils.getMessageFromSQLException(e); 
					_logger.error("Problems when connecting to a ASE Server. "+msg);
				}
				catch (Exception e)
				{
					_logger.error("Problems when connecting to a ASE Server. "+e);
				}


				if (_monConn == null)
				{
					_logger.error("Problems connecting to ASE server. sleeping for "+_sleepOnFailedConnectTime+" seconds before retry...");
					
					try { Thread.sleep( _sleepTime * 1000 ); }
					catch (InterruptedException ignore) {}

					// START AT THE TOP AGAIN
					continue;
				}
				
				mtd = MonTablesDictionary.getInstance();
				if ( ! mtd.isInitialized() )
				{
					mtd.initialize(_monConn);
					GetCounters.initExtraMonTablesDictionary();
				}
			}
				
			loopCounter++;
			
			// When 10 MB of memory or less, write some info about that.
			Memory.checkMemoryUsage(10);

			try
			{
				String    aseServerName    = null;
				Timestamp mainSampleTime   = null;
				Timestamp counterClearTime = null;

				Statement stmt = _monConn.createStatement();
				ResultSet rs = stmt.executeQuery("select getdate(), @@servername, CountersCleared from master..monState");
				while (rs.next())
				{
					mainSampleTime   = rs.getTimestamp(1);
					aseServerName    = rs.getString(2);
					counterClearTime = rs.getTimestamp(3);
				}
				rs.close();
				stmt.close();
				
				// Initialize the counters, now when we know what 
				// release we are connected to
				if ( ! initialized )
				{
					initCounters( _monConn, mtd.aseVersionNum, mtd.aseMonTablesInstallVersionNum);
				}

				PersistContainer pc = new PersistContainer(mainSampleTime, aseServerName);

				//-----------------
				// LOOP all CounterModels, and get new data, 
				//   if it should be done
				//-----------------
				Iterator iter = _CMList.iterator();
				while (iter.hasNext())
				{
					CountersModel cm = (CountersModel) iter.next();
					
					if (cm != null)
					{
						if ( ! cm.isActive() )
							continue;

						if ( cm.isPersistCountersEnabled() )
						{
							cm.setServerName(aseServerName);
							cm.setSampleTimeHead(mainSampleTime);
							cm.setCounterClearTime(counterClearTime);

							cm.refresh(_monConn);
							
							// Add the CM to the container, which will 
							// be posted to persister thread later.
							pc.add(cm);
							
							cm.endOfRefresh();
						}
					}
				}

				// POST the container to the Persistent Counter Handler
				// That thread will store the information in any Storage.
				pch.add(pc);
				
			}
			catch (Throwable t)
			{
				_logger.error(Version.getAppName()+": error in GetCounters loop.", t);
			}

			//-----------------------------
			// Sleep
			//-----------------------------
			if (_logger.isDebugEnabled())
			{
				setWaitEvent("next sample period...");
				_logger.debug("Sleeping for "+_sleepTime+" seconds. Waiting for " + getWaitEvent() );
			}

			// Sleep (if not first loop)
			try { Thread.sleep( _sleepTime * 1000 ); }
			catch (InterruptedException ignore) {}
		}
	}
}
