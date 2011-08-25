/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon;

import java.io.Console;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import asemon.check.CheckForUpdates;
import asemon.cm.CountersModel;
import asemon.gui.SummaryPanel;
import asemon.pcs.PersistContainer;
import asemon.pcs.PersistentCounterHandler;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.MandatoryPropertyException;
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
//	private Connection _monConn     = null;

	/** sleep time between samples */
	private int        _sleepTime   = 60;

	/** If no connection can be made to the ASE server, sleep time before retry */
	private int        _sleepOnFailedConnectTime = 60;

	/** Connection information */
	private String     _aseUsername = null;
	private String     _asePassword = null; 
	private String     _aseServer   = null;
//	private String     _aseHostname = null;
//	private String     _asePort     = null;
	private String     _aseHostPortStr = null;

	private Configuration _storeProps = null;
	
	private boolean       _running = true;

	public GetCountersNoGui()
	{
		super.setInstance(this);
	}

	public static boolean checkValidCmShortcuts(String options)
	{
		if ( ! (new File(options)).exists() )
		{
			if (options.indexOf(',') >= 0)
				return true;

			if (    options.equals("small")
				 || options.equals("medium")
				 || options.equals("large")
				 || options.equals("all")
			   )
			{
				return true;
			}
		}
		return false;
	}

	public void init()
	throws Exception
	{
		String offlinePrefix = "offline.";
//		String CMpropPrefix  = "CM.";
		String connPrefix    = "conn.";

		_storeProps = Configuration.getInstance(Configuration.PCS);
		if ( _storeProps == null || (_storeProps != null && _storeProps.size() == 0) )
		{
			throw new Exception("No persistent storage configuration was found. Persistent storage are configured via Command Line Switch '-noGui'");
		}
		
		// WRITE init message, jupp a little late, but I wanted to grab the _name
		_logger.info("Initializing the NO-GUI sampler component. Using config file '"+_storeProps.getFilename()+"'.");

		// PROPERTY: sleepTime
		_sleepTime = _storeProps.getIntMandatoryProperty(offlinePrefix + "sampleTime");


		
		// PROPERTY: sleepOnFailedConnectTime
//		_sleepOnFailedConnectTime = _props.getIntProperty(offlinePrefix+"sleepOnFailedConnectTime", _sleepOnFailedConnectTime);

		
		// PROPERTY: username, password, server
		_aseUsername    = _storeProps.getProperty(connPrefix+"aseUsername");
		_asePassword    = _storeProps.getProperty(connPrefix+"asePassword"); 
		_aseServer      = _storeProps.getProperty(connPrefix+"aseName");
		String aseHosts = _storeProps.getProperty(connPrefix+"aseHost");
		String asePorts = _storeProps.getProperty(connPrefix+"asePort");
		_aseHostPortStr = _storeProps.getProperty(connPrefix+"aseHostPort");

		// Override prop values with the Command line parameters if any. 
//		CommandLine cmd = Asemon.getCmdLineParams();
//		if (cmd.hasOption('U'))	_aseUsername = cmd.getOptionValue('U');
//		if (cmd.hasOption('P'))	_asePassword = cmd.getOptionValue('P');
//		if (cmd.hasOption('S'))	_aseServer   = cmd.getOptionValue('S');
//		
//		// -s, --sampleTime: Offline: time between samples.
//		if (cmd.hasOption('s'))
//		{
//			try 
//			{
//				_sleepTime = Integer.parseInt( cmd.getOptionValue('s'));
//			}
//			catch(NumberFormatException ignore)
//			{
//				_logger.info("The command line parameter -s|--sampleTime was not a number. Setting sample time to "+_sleepTime);
//			}
//		}

		if (_aseUsername == null) throw new Exception("No ASE User has been specified. Not by commandLine parameters '-U', or by property 'conn.aseUsername' in the file '"+_storeProps.getFilename()+"'.");
		if (_aseServer == null)   throw new Exception("No ASE Server has been specified. Not by commandLine parameters '-S', or by property 'conn.aseName' in the file '"+_storeProps.getFilename()+"'.");

		// If aseHostPort wasn't found, use aseHost & asePort
		if (_aseHostPortStr == null && aseHosts != null && asePorts != null)
			_aseHostPortStr = AseConnectionFactory.toHostPortStr(aseHosts, asePorts);
		
		// If aseServerName is specified in "server name" format (not in host:port)
		// Then go and grab host:port from the local interfaces file.
		// Meaning serverName overrides host/port specifications.
		// BUT if the serverName can't be found in the local interfaces file then use 
		// the specified properties aseHost + asePort or aseHostPort
		String aseServerStr = _aseServer;
		if ( aseServerStr.indexOf(":") == -1 )
		{
			aseServerStr = AseConnectionFactory.getIHostPortStr(_aseServer);
			if (aseServerStr == null)
			{
				_logger.info("Can't resolve or find ASE Server named '"+_aseServer+"' in the interfaces/sql.ini file. Fallback on 'aseHostPort', which is '"+_aseHostPortStr+"'.");
				throw new Exception("Can't resolve or find ASE Server named '"+_aseServer+"' in the interfaces/sql.ini file. Fallback on 'aseHostPort', which is '"+_aseHostPortStr+"'.");
				//aseServerStr = _aseHostPortStr;
			}
		}
		else
		{
			if (_aseHostPortStr != null)
				aseServerStr = _aseHostPortStr;
		}

		// Check input if it looks like: host:port[, host2:port2[, hostN:portN]]
		if ( ! AseConnectionFactory.isHostPortStrValid(aseServerStr) )
		{
			String error = AseConnectionFactory.isHostPortStrValidReason(aseServerStr);
			throw new Exception("The ASE Server connection specification '"+aseServerStr+"' is in a faulty format. The format should be 'hostname:port[,hostname2:port2[,hostnameN:portN]]', error='"+error+"'.");
		}
		_aseHostPortStr = aseServerStr;

		// check that aseHostPort or aseHost,asePort are set
		if (_aseHostPortStr == null && (aseHosts == null || asePorts == null))
			throw new MandatoryPropertyException("If the properties '"+connPrefix+"aseName' or '"+connPrefix+"aseHostPort' or cmdLine switch -S, is not specified. Then '"+connPrefix+"aseHost' and '"+connPrefix+"asePort' must be specified.");


		
		// Still no password, read it from the STDIN
		if (_asePassword == null)
		{
			Console cons = System.console();
			if (cons != null)
			{
				System.out.println("No password was specified use command line parameter -P or property '"+connPrefix+"asePassword' in the file '"+_storeProps.getFilename()+"'.");
				System.out.println("Connecting to server '"+_aseServer+"' at '"+_aseHostPortStr+"' with the user name '"+_aseUsername+"'.");
				char[] passwd = cons.readPassword("Password: ");
				_asePassword = new String(passwd);
				//System.out.println("Read password from Console '"+_asePassword+"'.");
			}
		}
		
		// treat null password
		if (_asePassword.equalsIgnoreCase("null"))
			_asePassword = "";
		
		
		String configStr = "sleepTime='"+_sleepTime+"', sleepOnFailedConnectTime='"+_sleepOnFailedConnectTime+"', _aseUsername='"+_aseUsername+"', _asePassword='*hidden*', _aseServer='"+_aseServer+"("+_aseHostPortStr+")'.";
		_logger.info("Configuration for NO-GUI sampler: "+configStr);
		
		
		// Create all the CM objects, the objects will be added to _CMList
		this.createCounters();

		// SPECIAL things for some offline CMD LINE parameters
		String cmOptions = _storeProps.getProperty("cmdLine.cmOptions");
		if (cmOptions != null)
		{
			List<String> activeCmList = new ArrayList<String>();

			if (cmOptions.equalsIgnoreCase("small"))
			{
				activeCmList.add(SummaryPanel.CM_NAME);
				activeCmList.add("CMcacheActivity");
				activeCmList.add("CMengineActivity");
				activeCmList.add("CMioQueueActivity");
				activeCmList.add("CMioQueueSumAct");
				_logger.info("Found --noGui small: enabling cm's named "+activeCmList);
			}
			else if (cmOptions.equalsIgnoreCase("medium"))
			{
				activeCmList.add(SummaryPanel.CM_NAME);
				activeCmList.add("CMobjActivity");
				activeCmList.add("CMprocActivity");
				activeCmList.add("CMdbActivity");
				activeCmList.add("CMTmpdbActivity");
				activeCmList.add("CMsysWaitActivity");
				activeCmList.add("CMengineActivity");
				activeCmList.add("CMSysLoad");
				activeCmList.add("CMcacheActivity");
				activeCmList.add("CMpoolActivity");
				activeCmList.add("CMdeviceActivity");
				activeCmList.add("CMioQueueSumAct");
				activeCmList.add("CMioQueueActivity");
//				activeCmList.add("CMspinlockSum:300"); // 5 minute
//				activeCmList.add("CMsysmon:300");      // 5 minute
//				activeCmList.add("CMcachedProcs");
				activeCmList.add("CMprocCache");
				activeCmList.add("CMprocCallStack");
//				activeCmList.add("CMcachedObjects:600"); / 10 minute
//				activeCmList.add("CMerrolog");
//				activeCmList.add("CMdeadlock");
//				activeCmList.add("CMpCacheModuleUsage");
//				activeCmList.add("CMpCacheMemoryUsage");
//				activeCmList.add("CMstatementCache");
//				activeCmList.add("CMstmntCacheDetails");
//				activeCmList.add("CMActiveObjects");
				activeCmList.add("CMActiveStatements");
//				activeCmList.add("CMblocking");
//				activeCmList.add("CMmissingStats");
				activeCmList.add("CMspMonitorConfig:3600"); // 1 Hour

				_logger.info("Found --noGui medium: enabling cm's named "+activeCmList);
			}
			else if (cmOptions.equalsIgnoreCase("large"))
			{
				activeCmList.add(SummaryPanel.CM_NAME);
				activeCmList.add("CMobjActivity");
				activeCmList.add("CMprocActivity");
				activeCmList.add("CMdbActivity");
				activeCmList.add("CMTmpdbActivity");
				activeCmList.add("CMsysWaitActivity");
				activeCmList.add("CMengineActivity");
				activeCmList.add("CMSysLoad");
				activeCmList.add("CMcacheActivity");
				activeCmList.add("CMpoolActivity");
				activeCmList.add("CMdeviceActivity");
				activeCmList.add("CMioQueueSumAct");
				activeCmList.add("CMioQueueActivity");
//				activeCmList.add("CMspinlockSum:300"); // 5 minute
//				activeCmList.add("CMsysmon:300");      // 5 minute
//				activeCmList.add("CMcachedProcs");
				activeCmList.add("CMprocCache");
				activeCmList.add("CMprocCallStack");
//				activeCmList.add("CMcachedObjects:600"); / 10 minute
//				activeCmList.add("CMerrolog");
//				activeCmList.add("CMdeadlock");
				activeCmList.add("CMpCacheModuleUsage");
				activeCmList.add("CMpCacheMemoryUsage");
				activeCmList.add("CMstatementCache");
				activeCmList.add("CMstmntCacheDetails");
				activeCmList.add("CMActiveObjects");
				activeCmList.add("CMActiveStatements");
				activeCmList.add("CMblocking");
				activeCmList.add("CMmissingStats");
				activeCmList.add("CMspMonitorConfig:3600"); // 1 Hour

				_logger.info("Found --noGui large: enabling cm's named "+activeCmList);
			}
			else if (cmOptions.equalsIgnoreCase("all"))
			{
				for (CountersModel cm : _CMList)
				{
					String addStr = null;
					if (cm == null)
						continue;

					if      ("CMspinlockSum"    .equals(cm.getName())) addStr = cm.getName() + ":300"; // 5 minute
					else if ("CMsysmon"         .equals(cm.getName())) addStr = cm.getName() + ":300"; // 5 minute
					else if ("CMcachedObjects"  .equals(cm.getName())) addStr = cm.getName() + ":600"; // 10 minute
					else if ("CMerrolog"        .equals(cm.getName())) continue; // do not save
					else if ("CMdeadlock"       .equals(cm.getName())) continue; // do not save
					else if ("CMspMonitorConfig".equals(cm.getName())) addStr = cm.getName() + ":3600"; // 1 Hour
					else addStr = cm.getName();

					activeCmList.add(addStr);
				}
				_logger.info("Found --noGui all: enabling cm's named "+activeCmList);
			}
			else
			{
				// option can look like: cm1,cm2,cm3:postponeTime,cm4,cm5
				String[] sa = cmOptions.split(",");
				for (String str : sa)
					activeCmList.add(str);

				_logger.info("Found --noGui aListOfCmNames: enabling cm's named "+activeCmList);
			}

			// FIRST disable things, then enable them again later
			for (CountersModel cm : _CMList)
			{
				cm.setActive(false, "Inactivated by offline config");
				cm.setPersistCounters(true, false);
				cm.setPostponeTime(0, false);
			}
			
			// NOW, lets look at the list and enable various CM's
			for (String cmNameStr : activeCmList)
			{
				String cmName       = cmNameStr;
				int    postponeTime = 0;

				// cmName can look like: cm1,cm2,cm3:postponeTime,cm4,cm5
				String[] optStr = cmNameStr.split(":");
				if (optStr.length > 1)
				{
					try
					{
						postponeTime = Integer.parseInt(optStr[1]);
						cmName       = optStr[0];
					}
					catch (NumberFormatException ignore)
					{
						_logger.error("Can't read postpone time from the string '"+optStr[1]+"', since it's not a number. Full cmName/option was '"+optStr+"'.");
					}
				}

				CountersModel cm = getCmByName(cmName);
				if (cm != null)
				{
					cm.setActive(true, null);
					cm.setPersistCounters(true, false);
					cm.setPostponeTime(postponeTime, false);
				}
				else
				{
					_logger.warn("CM named '"+cmName+"' can't be found in the list of available CM's.");
				}
			}

			// Write info about what was enabled/disabled
			int activeCount = 0;
			for (CountersModel cm : _CMList)
			{
				if ( cm.isActive() )
				{
					activeCount++;
					_logger.info(">Enabled  CM named '"+cm.getName()+"'.");
				}
				else 
					_logger.info(" DISABLED CM named '"+cm.getName()+"'.");
			}
			_logger.info("Setting "+activeCount+" CM's in active-sampling-state. The CMList contained "+_CMList.size()+" entries.");
			if (activeCount == 0)
				throw new Exception("Can't find any CM's to sample. Check the command line option '-n cmNames'.");
		}
		//-------------------------------------------------------
		// We have a property file
		//-------------------------------------------------------
		else
		{
			//-----------------
			// LOOP all CounterModels, and remove if config indicates it should not be sampled, 
			//-----------------
			int activeCount = 0;
			for (CountersModel cm : _CMList)
			{
				String persistCountersKey = cm.getName() + "." + CountersModel.PROP_persistCounters;

				if (cm != null)
				{
					if ( _storeProps.getBooleanProperty(persistCountersKey, false) == false )
					{
						cm.setActive(false, "Inactivated by offline config");

						_logger.info("DISABLING CM named '"+cm.getName()+"'.");
					}
					else
					{
						activeCount++;
						//The offline props file indicates this CM should be sampled, persist it.
						cm.setPersistCounters(true);

						_logger.info("Enabling CM named '"+cm.getName()+"'.");
					}
				}
			}
			_logger.info("Setting "+activeCount+" CM's in active-sampling-state. The CMList contained "+_CMList.size()+" entries.");
			if (activeCount == 0)
			{
				throw new Exception("Can't find any CM's to sample. Check the file '"+_storeProps.getFilename()+"', for the any keys ending with '.sample' and mark them as 'true'.");
			}
		}
	}

	public void run()
	{
		// Set the Thread name
		_thread = Thread.currentThread();
		_thread.setName("GetCountersNoGui");

		// loop
		int loopCounter = 0;

		MonTablesDictionary mtd = null;
		
		Timestamp sessionStartTime = null;

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
//			if (_monConn != null)
//			{
//				try
//				{
//					if ( _monConn.isClosed() )
//					{
//						try {_monConn.close();}
//						catch(SQLException ignore) {}
//						finally {_monConn = null;}
//					}
//				}
//				catch (SQLException ignore) {}
//			}

			// CONNECT (initial or reconnect)
//			if (_monConn == null)
			if ( ! isMonConnected(true, true))
			{
				sessionStartTime = null;
				
				_logger.debug("Connecting to ASE server using. user='"+_aseUsername+"', passwd='"+_asePassword+"', hostPortStr='"+_aseHostPortStr+"'. aseServer='"+_aseServer+"'");
				_logger.info( "Connecting to ASE server using. user='"+_aseUsername+"', passwd='"+ "*hidden*" +"', hostPortStr='"+_aseHostPortStr+"'. aseServer='"+_aseServer+"'");

				// get a connection
				try
				{
					setMonConnection(AseConnectionFactory.getConnection(_aseHostPortStr, null, _aseUsername, _asePassword, Version.getAppName()+"-nogui", Version.getVersionStr(), (Properties)null, null));
					
					AseConnectionFactory.setHostPort(_aseHostPortStr);

					// CHECK the connection for proper configuration.
					// If failure, go and FIX
					// FIXME: implement the below "set minimal logging options"
					if ( ! AseConnectionUtils.checkForMonitorOptions(getMonConnection(), _aseUsername, false, null) )
					{
						AseConnectionUtils.setBasicAseMonitoring(getMonConnection());
					}

					// CHECK the connection for proper configuration.
					// The fix did not work, so lets get out of here
					if ( ! AseConnectionUtils.checkForMonitorOptions(getMonConnection(), _aseUsername, false, null) )
					{
						_logger.error("Problems when checking the ASE Server for 'proper monitoring configuration'.");

						// Disconnect, and get out of here...
						closeMonConnection();
						
						// THE LOOP WILL BE FALSE (_running = false)
						_running = false;

						// START AT THE TOP AGAIN
						continue;
					}

					CheckForUpdates.sendConnectInfoNoBlock();
				}
				catch (SQLException e)
				{
					String msg = AseConnectionUtils.getMessageFromSQLException(e); 
					_logger.error("Problems when connecting to a ASE Server. "+msg);

					// JZ00L: Login failed
					if (e.getSQLState().equals("JZ00L"))
					{
						// THE LOOP WILL BE FALSE (_running = false)
						_running = false;

						_logger.error("Faulty PASSWORD when connecting to the server '"+_aseServer+"' at '"+_aseHostPortStr+"', with user '"+_aseUsername+"', I cant recover from this... exiting...");

						// GET OUT OF THE LOOP, causing us to EXIT
						break;
					}
				}
				catch (Exception e)
				{
					_logger.error("Problems when connecting to a ASE Server. "+e);
				}


				if ( ! isMonConnected(true, true) )
				{
					_logger.error("Problems connecting to ASE server. sleeping for "+_sleepOnFailedConnectTime+" seconds before retry...");
					
					try { Thread.sleep( _sleepTime * 1000 ); }
					catch (InterruptedException ignore) {}

					// START AT THE TOP AGAIN
					continue;
				}
				
				// initialize Mon Table Dictionary
				mtd = MonTablesDictionary.getInstance();
				if ( ! mtd.isInitialized() )
				{
					mtd.initialize(getMonConnection());
					GetCounters.initExtraMonTablesDictionary();
				}
				
				// initialize ASE Config Dictionary
				AseConfig aseCfg = AseConfig.getInstance();
				if ( ! aseCfg.isInitialized() )
				{
					aseCfg.initialize(getMonConnection(), false, null);
				}

				// initialize ASE Cache Config Dictionary
				AseCacheConfig aseCacheCfg = AseCacheConfig.getInstance();
				if ( ! aseCacheCfg.isInitialized() )
				{
					aseCacheCfg.initialize(getMonConnection(), false, null);
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

				Statement stmt = getMonConnection().createStatement();
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
				if ( ! isInitialized() )
				{
					initCounters( getMonConnection(), mtd.aseVersionNum, mtd.isClusterEnabled, mtd.montablesVersionNum);
				}

				if (_CMList == null || (_CMList != null && _CMList.size() == 0))
				{
					_logger.error("The list of known CM's is either null or empty, can't continue...");
					continue;
				}

				//----------------------
				// In some versions we need to check if the transaction log is full to some reasons
				// If it is full it will be truncated.
				//----------------------
				checkForFullTransLogInMaster(getMonConnection());


				// SET a session id and call startSession()
				if (sessionStartTime == null)
				{
					sessionStartTime = mainSampleTime;
					if (pch != null)
					{
						PersistContainer tmpPc = new PersistContainer(sessionStartTime, mainSampleTime, aseServerName);
//						Iterator iter = _CMList.iterator();
//						while (iter.hasNext())
//						{
//							CountersModel cm = (CountersModel) iter.next();
						for (CountersModel cm : _CMList)
						{
							if (cm == null) continue;
							if ( ! cm.isActive() ) continue;
							tmpPc.add(cm);
						}
						pch.startSession(tmpPc);
					}
				}

				// PCS
				PersistContainer pc = new PersistContainer(sessionStartTime, mainSampleTime, aseServerName);

				
				//-----------------
				// LOOP all CounterModels, and get new data, 
				//   if it should be done
				//-----------------
//				Iterator iter = _CMList.iterator();
//				while (iter.hasNext())
//				{
//					CountersModel cm = (CountersModel) iter.next();
				for (CountersModel cm : _CMList)
				{
					if (cm != null && cm.isRefreshable())
					{
						cm.setServerName(aseServerName);
						cm.setSampleTimeHead(mainSampleTime);
						cm.setCounterClearTime(counterClearTime);

						try
						{
							cm.refresh();
//							cm.refresh(getMonConnection());
	
							// move this into cm.refresh()
//							cm.setValidSampleData( (cm.getRowCount() > 0) ); 

							// Add the CM to the container, which will 
							// be posted to persister thread later.
							pc.add(cm);
						}
						catch (Exception ex)
						{
							// log the stack trace for all others than the SQLException
							if (ex instanceof SQLException)
								_logger.warn("Problem when refreshing cm '"+cm.getName()+"'. Caught: " + ex);
							else
								_logger.warn("Problem when refreshing cm '"+cm.getName()+"'. Caught: " + ex, ex);

							cm.setSampleException(ex);

							// move this into cm.refresh()
//							cm.setValidSampleData(false); 
						}
						
						cm.endOfRefresh();

					} // END: isRefreshable

				} // END: LOOP CM's

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
		_logger.info("Thread '"+Thread.currentThread().getName()+"' ending...");

		// so lets stop the Persistent Counter Handler and it's services as well
		if (pch != null)
			pch.stop();
	}
}
