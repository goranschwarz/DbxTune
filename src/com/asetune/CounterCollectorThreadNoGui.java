/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.events.AlarmEvent;
import com.asetune.alarm.events.ase.AlarmEventAseLicensExpiration;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc;
import com.asetune.alarm.writers.AlarmWriterToPcsJdbc.AlarmEventWrapper;
import com.asetune.cache.XmlPlanCache;
import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesDbx.DbxConnectInfo;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CountersModel;
import com.asetune.cm.LostConnectionException;
import com.asetune.config.dbms.DbmsConfigManager;
import com.asetune.config.dbms.DbmsConfigTextManager;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.gui.MainFrame;
import com.asetune.pcs.PersistContainer;
import com.asetune.pcs.PersistWriterBase;
import com.asetune.pcs.PersistentCounterHandler;
import com.asetune.pcs.inspection.IObjectLookupInspector;
import com.asetune.pcs.sqlcapture.ISqlCaptureBroker;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.ssh.SshConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseLicensInfo;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.HeartbeatMonitor;
import com.asetune.utils.MandatoryPropertyException;
import com.asetune.utils.Memory;
import com.asetune.utils.MemoryWarningSystem;
import com.asetune.utils.PropPropEntry;
import com.asetune.utils.ShutdownHandler;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;


public class CounterCollectorThreadNoGui
extends CounterCollectorThreadAbstract
implements Memory.MemoryListener
{
	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(CounterCollectorThreadNoGui.class);

	public static final String THREAD_NAME = "GetCountersNoGui";


	public CounterCollectorThreadNoGui(CounterControllerAbstract counterController)
	{
		super(counterController);
	}

	
	/** a connection to the DBMS server to monitor */
//	private Connection _monConn     = null;

	/** sleep time between samples */
	private int        _sleepTime   = 60;
	private String     _scriptWaitForNextSample = null;

	/** if above 0, then shutdown the service after X hours */
//	private int        _shutdownAfterXHours = 0;
	private String     _shutdownAtTimeStr   = null;
	private Date       _shutdownAtTime      = null;

	/** if != null, then delay the start until this time HHMM */
	private String     _deferedStartTime = null;

	/** If no connection can be made to the DBMS server, sleep time before retry */
	private int        _sleepOnFailedConnectTime = 60;

	/** Connection information */
	private String     _dbmsUsername    = null;
	private String     _dbmsPassword    = null; 
	private String     _dbmsServer      = null;
	private String     _dbmsServerAlias = null;
//	private String     _dbmsHostname    = null;
//	private String     _dbmsPort        = null;
	private String     _dbmsHostPortStr = null;
	private String     _jdbcUrlOptions  = null;

	private String     _sshUsername     = null;
	private String     _sshPassword     = null; 
	private String     _sshKeyFile      = null;
	private String     _sshHostname     = null;
	private String     _sshPortStr      = null;
	private int        _sshPort         = 22;
	
	private Configuration _storeProps   = null;
	
	private boolean       _running      = true;

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

	@Override
	public void init(boolean hasGui)
	throws Exception
	{
		String offlinePrefix = "offline.";
//		String CMpropPrefix  = "CM.";
//		String connPrefix    = "conn.";

		_storeProps = Configuration.getInstance(Configuration.PCS);
		if ( _storeProps == null || (_storeProps != null && _storeProps.size() == 0) )
		{
			throw new Exception("No persistent storage configuration was found. Persistent storage are configured via Command Line Switch '-noGui'");
		}
		
		// WRITE init message, jupp a little late, but I wanted to grab the _name
		String cmTemplateOption = _storeProps.getProperty("cmdLine.cmOptions");
		if (cmTemplateOption != null)
			_logger.info("Initializing the NO-GUI sampler component. Using command line template '"+cmTemplateOption+"'.");
		else
			_logger.info("Initializing the NO-GUI sampler component. Using config file '"+_storeProps.getFilename()+"'.");


		// Reset the search order to be "just" Configuration.PCS
		// should we do this or not (or ONLY when using props file, an NOT for templates)
//		Configuration.setSearchOrder( Configuration.PCS );
//		String userConfigFile    = Configuration.getInstance(Configuration.USER_CONF) == null ? "not set"                    : Configuration.getInstance(Configuration.USER_CONF).getFilename();
//		String userTmpConfigFile = Configuration.getInstance(Configuration.USER_TEMP) == null ? "not set"                    : Configuration.getInstance(Configuration.USER_TEMP).getFilename();
//		String pcsConfigFile     = cmTemplateOption                                   != null ? "template="+cmTemplateOption : _storeProps.getFilename();
//		_logger.info("Combined Configuration Search Order has been changed to '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'. This means that USER_CONF='"+userConfigFile+"' and USER_TEMP='"+userTmpConfigFile+"' wont be used for as fallback configurations. Only the PCS='"+pcsConfigFile+"' config will be used.");

		_logger.info("NO-GUI Init - Combined Configuration Search Order is '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");
		_logger.info("              PCS         Using Configuration file '"+(Configuration.getInstance(Configuration.PCS)         == null ? "-no-instance-" : Configuration.getInstance(Configuration.PCS        ).getFilename())+"'.");
		_logger.info("              USER_TEMP   Using Configuration file '"+(Configuration.getInstance(Configuration.USER_TEMP)   == null ? "-no-instance-" : Configuration.getInstance(Configuration.USER_TEMP  ).getFilename())+"'.");
		_logger.info("              USER_CONF   Using Configuration file '"+(Configuration.getInstance(Configuration.USER_CONF)   == null ? "-no-instance-" : Configuration.getInstance(Configuration.USER_CONF  ).getFilename())+"'.");
		_logger.info("              SYSTEM_CONF Using Configuration file '"+(Configuration.getInstance(Configuration.SYSTEM_CONF) == null ? "-no-instance-" : Configuration.getInstance(Configuration.SYSTEM_CONF).getFilename())+"'.");

		// PROPERTY: sleepTime
		_sleepTime               = _storeProps.getIntMandatoryProperty(offlinePrefix + "sampleTime");
		_scriptWaitForNextSample = _storeProps.getProperty(            offlinePrefix + "script.waitForNextSample");

		// PROPERTY: shutdownAfterXHours
		_deferedStartTime = _storeProps.getProperty(CounterController.PROPKEY_startRecordingAtTime);
		try {
			PersistWriterBase.getRecordingStartTime(_deferedStartTime);
		} catch (Exception e) {
			throw new Exception("Deferred start time '"+CounterController.PROPKEY_startRecordingAtTime+"' is faulty configured, Caught: "+e.getMessage());
		}

		// PROPERTY: shutdownAfterXHours
//		_shutdownAfterXHours = _storeProps.getIntProperty(offlinePrefix + "shutdownAfterXHours", _shutdownAfterXHours);
		_shutdownAtTimeStr = _storeProps.getProperty(offlinePrefix + "shutdownAfterXHours");
		if (_shutdownAtTimeStr != null)
		{
			Date startTime = PersistWriterBase.getRecordingStartTime(_deferedStartTime);
			_shutdownAtTime = PersistWriterBase.getRecordingStopTime(startTime, _shutdownAtTimeStr);
		}

		// PROPERTY: sleepOnFailedConnectTime
		_sleepOnFailedConnectTime = _storeProps.getIntProperty(offlinePrefix + "sleepOnFailedConnectTime", _sleepOnFailedConnectTime);

		
		// PROPERTY: username, password, server
		_dbmsUsername    = _storeProps.getProperty("conn.dbmsUsername");
		_dbmsPassword    = _storeProps.getProperty("conn.dbmsPassword"); 
		_dbmsServer      = _storeProps.getProperty("conn.dbmsName");
		_dbmsServerAlias = _storeProps.getProperty("conn.dbmsServerAlias");
		String dbmsHosts = _storeProps.getProperty("conn.dbmsHost");
		String dbmsPorts = _storeProps.getProperty("conn.dbmsPort");
		_dbmsHostPortStr = _storeProps.getProperty("conn.dbmsHostPort");
		_jdbcUrlOptions  = _storeProps.getProperty("conn.jdbcUrlOptions");

		_sshUsername     = _storeProps.getProperty("conn.sshUsername");
		_sshPassword     = _storeProps.getProperty("conn.sshPassword"); 
		_sshKeyFile      = _storeProps.getProperty("conn.sshKeyFile"); 
		_sshHostname     = _storeProps.getProperty("conn.sshHostname");
		_sshPortStr      = _storeProps.getProperty("conn.sshPort");
		if (_sshPortStr != null && ! _sshPortStr.equals(""))
			_sshPort = Integer.parseInt(_sshPortStr);

		// typically faulty initialized may be: "null:-1", then unset the value
		if (_dbmsHostPortStr != null && _dbmsHostPortStr.equals("null:-1"))
			_dbmsHostPortStr = null;
		
		
		// Override prop values with the Command line parameters if any. 
//		CommandLine cmd = AseTune.getCmdLineParams();
//		if (cmd.hasOption('U'))	_dbmsUsername = cmd.getOptionValue('U');
//		if (cmd.hasOption('P'))	_dbmsPassword = cmd.getOptionValue('P');
//		if (cmd.hasOption('S'))	_dbmsServer   = cmd.getOptionValue('S');
//		
//		// -i, --interval: Offline: time between samples.
//		if (cmd.hasOption('i'))
//		{
//			try 
//			{
//				_sleepTime = Integer.parseInt( cmd.getOptionValue('i'));
//			}
//			catch(NumberFormatException ignore)
//			{
//				_logger.info("The command line parameter -i|--interval was not a number. Setting sample time to "+_sleepTime);
//			}
//		}

		if (_dbmsUsername == null) throw new Exception("No DBMS User has been specified. Not by commandLine parameters '-U', or by property 'conn.dbmsUsername' in the file '"+_storeProps.getFilename()+"'.");
		if (_dbmsServer == null)   throw new Exception("No DBMS Server has been specified. Not by commandLine parameters '-S', or by property 'conn.dbmsName' in the file '"+_storeProps.getFilename()+"'.");

		
		// TODO: maybe break this out into the counterCollector, so we can adapt to other DBMS types
		if (    "AseTune".equalsIgnoreCase(Version.getAppName())
			 || "IqTune" .equalsIgnoreCase(Version.getAppName())
			 || "RsTune" .equalsIgnoreCase(Version.getAppName())
			 || "RaxTune".equalsIgnoreCase(Version.getAppName())
		   )
		{
			// If aseHostPort wasn't found, use aseHost & asePort
			if (_dbmsHostPortStr == null && dbmsHosts != null && dbmsPorts != null)
				_dbmsHostPortStr = AseConnectionFactory.toHostPortStr(dbmsHosts, dbmsPorts);
			
			// If aseServerName is specified in "server name" format (not in host:port)
			// Then go and grab host:port from the local interfaces file.
			// Meaning serverName overrides host/port specifications.
			// BUT if the serverName can't be found in the local interfaces file then use 
			// the specified properties aseHost + asePort or aseHostPort
			String aseServerStr = _dbmsServer;
			if ( aseServerStr.indexOf(":") == -1 )
			{
				aseServerStr = AseConnectionFactory.getIHostPortStr(_dbmsServer);
				if (aseServerStr == null)
				{
					_logger.info("Can't resolve or find ASE Server named '"+_dbmsServer+"' in the interfaces/sql.ini file. Fallback on 'aseHostPort', which is '"+_dbmsHostPortStr+"'.");
					throw new Exception("Can't resolve or find ASE Server named '"+_dbmsServer+"' in the interfaces/sql.ini file. Fallback on 'aseHostPort', which is '"+_dbmsHostPortStr+"'.");
					//aseServerStr = _dbmsHostPortStr;
				}
			}
			else
			{
				if (_dbmsHostPortStr != null)
					aseServerStr = _dbmsHostPortStr;
			}

			// Check input if it looks like: host:port[, host2:port2[, hostN:portN]]
			if ( ! AseConnectionFactory.isHostPortStrValid(aseServerStr) )
			{
				String error = AseConnectionFactory.isHostPortStrValidReason(aseServerStr);
				throw new Exception("The ASE Server connection specification '"+aseServerStr+"' is in a faulty format. The format should be 'hostname:port[,hostname2:port2[,hostnameN:portN]]', error='"+error+"'.");
			}
			_dbmsHostPortStr = aseServerStr;

			// check that aseHostPort or aseHost,asePort are set
			if (_dbmsHostPortStr == null && (dbmsHosts == null || dbmsPorts == null))
				throw new MandatoryPropertyException("If the properties 'conn.dbmsName' or 'conn.dbmsHostPort' or cmdLine switch -S, is not specified. Then 'conn.dbmsHost' and 'conn.dbmsPort' must be specified.");
		}
		else
		{
			// If aseHostPort wasn't found, use aseHost & asePort
			if (_dbmsHostPortStr == null && dbmsHosts != null && dbmsPorts != null)
				_dbmsHostPortStr = dbmsHosts + ":" + dbmsPorts;

			if (_dbmsHostPortStr == null)
				_dbmsHostPortStr = _dbmsServer;
		}

		
		//-----------------------------------
		// Still no password, read it from the STDIN
		if (_dbmsPassword == null)
		{
			Console cons = System.console();
			if (cons != null)
			{
				System.out.println("-----------------------------------------------------------------------------");
				System.out.println("No password for DBMS was specified use command line parameter -P or property 'conn.dbmsPassword' in the file '"+_storeProps.getFilename()+"'.");
				System.out.println("Connecting to server '"+_dbmsServer+"' at '"+_dbmsHostPortStr+"' with the user name '"+_dbmsUsername+"'.");
				System.out.println("-----------------------------------------------------------------------------");
				char[] passwd = cons.readPassword("Password: ");
				_dbmsPassword = new String(passwd);
				//System.out.println("Read DBMS password from Console '"+_dbmsPassword+"'.");
			}
		}
		// if we started in background... stdin is not available
		if (_dbmsPassword == null)
		{
			throw new MandatoryPropertyException("No Password for the DBMS could be retrived for: Server '"+_dbmsServer+"' at '"+_dbmsHostPortStr+"' with the user name '"+_dbmsUsername+"'.");
		}
		// treat "null" password, and set it to blank
		if (_dbmsPassword.equalsIgnoreCase("null"))
			_dbmsPassword = "";

		
		//-----------------------------------
		// read PASSWORD FOR SSH...
//		if (_sshHostname != null && _sshPassword == null)
		if (StringUtil.hasValue(_sshHostname) && StringUtil.hasValue(_sshUsername) && _sshPassword == null && _sshKeyFile == null)
		{
			Console cons = System.console();
			if (cons != null)
			{
				System.out.println("-----------------------------------------------------------------------------");
				System.out.println("No SSH password was specified use command line parameter -p or property 'conn.sshPassword' in the file '"+_storeProps.getFilename()+"'.");
				System.out.println("Connecting to host name '"+_sshHostname+"' with the user name '"+_sshUsername+"'.");
				System.out.println("-----------------------------------------------------------------------------");
				char[] passwd = cons.readPassword("Password: ");
				_sshPassword = new String(passwd);
				//System.out.println("Read SSH password from Console '"+_sshPassword+"'.");
			}

			// if we started in background... stdin is not available
			if (_sshPassword == null)
			{
				throw new MandatoryPropertyException("No Password for SSH could be retrived.");
			}
		}
		// treat "null" password, and set it to blank
		if (_sshPassword != null && _sshPassword.equalsIgnoreCase("null"))
			_sshPassword = "";

		getCounterController().setDefaultSleepTimeInSec(_sleepTime);

		String configStr = 
			"sleepTime='"+_sleepTime+"', " +
			"scriptWaitForNextSample='"+(_scriptWaitForNextSample == null ? "using sleepTime" : "using JavaScript")+"', " +
//			"shutdownAfterXHours='"+_shutdownAfterXHours+"', " +
			"shutdownAfterXHours='"+_shutdownAtTimeStr+"', " +
			"startRecordingAtTime='"+_deferedStartTime+"', " +
			"sleepOnFailedConnectTime='"+_sleepOnFailedConnectTime+"', " +
			"_dbmsUsername='"+_dbmsUsername+"', " +
			"_dbmsPassword='*hidden*', " +
			"_dbmsServer='"+_dbmsServer+"("+_dbmsHostPortStr+")', " +
			"_sshUsername='"+_sshUsername+"', " +
			"_sshPassword='*hidden*', " +
			"_sshKeyFile='"+_sshKeyFile+"', " +
			"_sshHostname='"+_sshHostname+"', " +
			"_sshPort='"+_sshPort+"', " +
			".";
		_logger.info("Configuration for NO-GUI sampler: "+configStr);

		if (_scriptWaitForNextSample != null)
			_logger.info("Using Java Script when waiting for next sample period. Script: "+_scriptWaitForNextSample);

		// Setting internal "system property" variable 'SERVERNAME' to "specified server name" or the "hostName.portNum" 
		// This might be used by the AlarmWriterToFile or similar
		if (StringUtil.hasValue(_dbmsServer) || StringUtil.hasValue(_dbmsHostPortStr))
		{
			String servername = null;
			
			if (StringUtil.hasValue(_dbmsServer))
				servername = _dbmsServer;
			else
				servername = _dbmsHostPortStr;

			if (StringUtil.hasValue(servername))
			{
				// change any ':' to '.'   ... due to windows can't handle ':' to good in files
				// I choosed char '.' because '-' is probably in any hostnames, and '_' is usualy within any ASE-Servername
				// So if we want to have some logic when looking at a filename, then '.' made "best sence"
				//servername = servername.replace(':', '.');
				servername = DbxTune.stripSrvName(servername);

				// Note this is also set later on when we got a connection... which means that it may change...
				System.setProperty("SERVERNAME", servername);
			}
		}
		
		
		//---------------------------
		// Create all the CM objects, the objects will be added to _CMList
		long start = System.currentTimeMillis();
		
		getCounterController().createCounters(hasGui);

		long time = System.currentTimeMillis() - start;
		_logger.info("Creating ALL CM Objects took " + TimeUtils.msToTimeStr("%MM:%SS.%ms", time) + " time format(MM:SS.ms)");
		


		// SPECIAL things for some offline CMD LINE parameters
		String cmOptions = _storeProps.getProperty("cmdLine.cmOptions");
		if (cmOptions != null)
		{
			// Get a list of CM names, that should be used
			// This is based on cmdLine options, (could be template or a CommaSep string of cm's)
			List<String> activeCmList = buildActiveCmList(cmOptions);

			// FIRST disable things, then enable them again later
			for (CountersModel cm : getCounterController().getCmList())
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

				// Get CM: first try the "short" name, example "CMprocCallStack"
				//          then try the "long"  name, example "Procedure Call Stack"
				CountersModel cm = getCounterController().getCmByName(cmName);
				if (cm == null)
					cm = getCounterController().getCmByDisplayName(cmName);
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
			for (CountersModel cm : getCounterController().getCmList())
			{
				if ( cm.isActive() )
				{
					activeCount++;
					_logger.info(">Enabled  CM named "+StringUtil.left("'"+cm.getName()+"',", 20+3)+" postpone "+StringUtil.left("'"+cm.getPostponeTime()+"',", 5+3)+"Tab Name '"+cm.getDisplayName()+"'.");
				}
				else 
					_logger.info(" DISABLED CM named "+StringUtil.left("'"+cm.getName()+"',", 20+3)+" postpone "+StringUtil.left("'"+cm.getPostponeTime()+"',", 5+3)+"Tab Name '"+cm.getDisplayName()+"'.");
			}
			_logger.info("Setting "+activeCount+" CM's in active-sampling-state. The CMList contained "+getCounterController().getCmList().size()+" entries.");
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
			for (CountersModel cm : getCounterController().getCmList())
			{
				String persistCountersKey = cm.getName() + "." + CountersModel.PROPKEY_persistCounters;

				if (cm != null)
				{
					if ( _storeProps.getBooleanProperty(persistCountersKey, false) == false )
					{
						cm.setActive(false, "Inactivated by offline config");

						_logger.info(" DISABLED CM named "+StringUtil.left("'"+cm.getName()+"',", 20+3)+" postpone "+StringUtil.left("'"+cm.getPostponeTime()+"',", 5+3)+"Tab Name '"+cm.getDisplayName()+"'.");
					}
					else
					{
						activeCount++;
						//The offline props file indicates this CM should be sampled, persist it.
						cm.setPersistCounters(true, false);

						_logger.info(">Enabled  CM named "+StringUtil.left("'"+cm.getName()+"',", 20+3)+" postpone "+StringUtil.left("'"+cm.getPostponeTime()+"',", 5+3)+"Tab Name '"+cm.getDisplayName()+"'.");
					}
				}
			}
			_logger.info("Setting "+activeCount+" CM's in active-sampling-state. The CMList contained "+getCounterController().getCmList().size()+" entries.");
			if (activeCount == 0)
			{
				throw new Exception("Can't find any CM's to sample. Check the file '"+_storeProps.getFilename()+"', for the any keys ending with '.sample' and mark them as 'true'.");
			}
		}

		
		// Check for enabled Host Monitored CM's
		// if ANY we need host/user/passwd for SSH
		int activeCountHostMon = 0;
		for (CountersModel cm : getCounterController().getCmList())
		{
			if ( cm.isActive() )
				if (cm instanceof CounterModelHostMonitor)
					activeCountHostMon++;
		}
		if (activeCountHostMon > 0)
		{
			if (_sshHostname == null || _sshUsername == null || (_sshPassword == null && _sshKeyFile == null) )
				throw new Exception("There are "+activeCountHostMon+" active Performance Counters that are doing Host Monitoring, this is using SSH for communication, but no hostname/user/passwd is given. hostname='"+_sshHostname+"', username='"+_sshUsername+"', password='"+(_sshPassword==null?null:"*has*passwd*")+"', keyFile='"+_sshKeyFile+"'.");
		}
		
		// Add a "low memory" listener... so we can cleanup some stuff...
		Memory.addMemoryListener(this);

		// Add another memory listener...
		MemoryWarningSystem mws = new MemoryWarningSystem();
		MemoryWarningSystem.setPercentageUsageThreshold(0.8);
		
		mws.addListener(new MemoryWarningSystem.Listener()
		{
			@Override
			public void memoryUsageLow(long usedMemory, long maxMemory)
			{
				double percentageUsed    = (((double) usedMemory) / maxMemory) * 100.0;
				String percentageUsedStr = String.format("%.1f",percentageUsed);
				long freeMem = maxMemory - usedMemory;
				_logger.warn("Low on memory usage ? ... percentageUsed="+percentageUsedStr+", maxMemoryMb="+(maxMemory/1024/1024)+", usedMemoryMb="+(usedMemory/1024/1024)+", freeMemoryMb="+(freeMem/1024/1024));
			}
		});
		
		//---------------------------------
		// Install shutdown hook, that will STOP the collector (and SHUTDOWN H2)
		// This needs to be done before we start any connections to H2
		_logger.info("Installing shutdown hook, which will help us stop the system gracefully if we are killed.");

		// Setting this property so that CentralPersistenWriter can append DB_CLOSE_ON_EXIT on the URL when connectiong to H2
		System.setProperty("dbxtune.isShutdownHookInstalled", "true");

		// Add a shutdown handler (called when we recive ctrl-c or kill -15 */
		ShutdownHandler.addShutdownHandler(new ShutdownHandler.Shutdownable()
		{
			@Override
			public List<String> systemShutdown()
			{
				shutdown();
				
				return Arrays.asList(new String[]{THREAD_NAME});
			}
		});

//		// Setting this property so that CentralPersistenWriter can append DB_CLOSE_ON_EXIT on the URL when connectiong to H2
//		System.setProperty("dbxtune.isShutdownHookInstalled", "true");
//
//		Thread noGuiShutdownHook = new Thread("ShutdownHook-"+Version.getAppName()+"-NoGui")
//		{
//			@Override
//			public void run()
//			{
//				_logger.info("Shutdown-hook: Starting to do 'shutdown'...");
//
//				// DEBUG: write some thread info
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("--------------------------------------------------");
//					for (Thread th : Thread.getAllStackTraces().keySet()) 
//						_logger.debug("1-shutdownHook: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");
//				}
//
//				//----------------------------------------------------------------------------------
//				// Signal the collector to stop, and at the end it will stop the PCS if we got one
//				//----------------------------------------------------------------------------------
//				shutdown();
//
//				
//				// Wait for thread "GetCountersNoGui" has terminated
//				String waitForThreadName = THREAD_NAME;
//				long sleepTime   = 500;
//				long maxWaitTime = 40*1000; // 40 sec
//				long startTime   = System.currentTimeMillis();
//				while (true) // break on: notFound or  timeout
//				{
//					Thread waitForThread = null;
//					for (Thread th : Thread.getAllStackTraces().keySet())
//					{
//						if (waitForThreadName.equals(th.getName()))
//							waitForThread = th;
//					}
//
//					// The thread was found: So WAIT
//					if (waitForThread != null)
//					{
//						// But dont wait invane... onnor the timeout
//						if (TimeUtils.msDiffNow(startTime) > maxWaitTime)
//						{
//							_logger.warn("Shutdown-hook: Waited for thread '"+waitForThreadName+"' to terminate. maxWaitTime="+maxWaitTime+" ms has been expired. STOP WAITING.");
//							_logger.warn("Shutdown-hook: Here is a stacktrace of the thread '"+waitForThreadName+"' we are waiting for:" 
//									+ StringUtil.stackTraceToString(waitForThread.getStackTrace()));
//
//							_logger.warn("Shutdown-hook: For completeness, lets stacktrace all other threads that are not part of the 'system' Thread Group.");
//							for (Thread th : Thread.getAllStackTraces().keySet()) 
//							{
//								ThreadGroup thg = th.getThreadGroup();
//								boolean isSystem = thg == null ? false : "system".equalsIgnoreCase(thg.getName());
//								String  thgName  = thg == null ? "null" : thg.getName();
//								String  thName   = th.getName();
//
//								// Do not print System threads or the wait thread, which we already has printed
//								if (isSystem || waitForThreadName.equals(thName) || "DestroyJavaVM".equals(thName) || th == Thread.currentThread())
//									continue;
//								
//								_logger.info("Shutdown-hook: Stacktrace for threadName='"+th.getName()+"', isDaemon="+th.isDaemon()+", threadGroupName='"+thgName+"':"
//									+ StringUtil.stackTraceToString(th.getStackTrace()));
//							}
//							break; // ON TIMEOUT: get out of the while(true)
//						}
//
//						_logger.info("Shutdown-hook: Still waiting for thread '"+waitForThreadName+"' to terminate... sleepTime="+sleepTime+", TotalWaitTime="+TimeUtils.msDiffNow(startTime)+", maxWaitTime="+maxWaitTime);
//
//						// Sleep for X ms
//						try { Thread.sleep(sleepTime); }
//						catch (InterruptedException ignore) {}
//					}
//					else
//					{
//						break;  // ON NOT-FOUND: get out of the while(true)
//					}
//				}
//
////TODO 1: ssh connection ()
////    sybase@gorans-ub2:~/asetune$ ssh gorans@gorans-ub2
////    The authenticity of host 'gorans-ub2 (192.168.0.110)' can't be established.
////>>  ECDSA key fingerprint is SHA256:SvrXUiR6K9XhHVK4R5u14/pcbVSQOU0qCe2aNop7LVk.
////>>  Are you sure you want to continue connecting (yes/no)? yes
////>>  Warning: Permanently added 'gorans-ub2,192.168.0.110' (ECDSA) to the list of known hosts.
////    gorans@gorans-ub2's password:
////    Welcome to Ubuntu 16.04.3 LTS (GNU/Linux 4.4.0-53-generic x86_64)
////
////TODO 2: HostMonitoring... 
////Also check if the host is "local" then we dont need the SSH (but that might be harder to implement, mabe borrow something from com.asetune.utils.FileTail.FileTail.java)
//
//
//// TEST THE ABOVE CODE;
//// ALSO SQL Window:
////	- check H2 reconnect (not working)
////	- possibly: getServerName() and set that in the window title.
//
//				// DEBUG: write some thread info
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("--------------------------------------------------");
//					for (Thread th : Thread.getAllStackTraces().keySet()) 
//						_logger.debug("2-shutdownHook: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");
//				}
//
//				_logger.info("Shutdown-hook: Shutdown finished.");
//			}
//		};
//		Runtime.getRuntime().addShutdownHook(noGuiShutdownHook);

	}
	
	/*---------------------------------------------------
	** BEGIN: implementing Memory.MemoryListener
	**---------------------------------------------------
	*/
	@Override
	public void outOfMemoryHandler()
	{
		_logger.info("called: outOfMemoryHandler() - OUT_OF_MEMORY");
//		ActionEvent doGcEvent = new ActionEvent(this, 0, MainFrame.ACTION_OUT_OF_MEMORY);
//		actionPerformed(doGcEvent);
		
		
		// XML Plan Cache
		if (XmlPlanCache.hasInstance())
		{
			XmlPlanCache.getInstance().outOfMemoryHandler();
		}

		// Persistent Counter Storage
		if (PersistentCounterHandler.hasInstance())
		{
			PersistentCounterHandler.getInstance().outOfMemoryHandler();
		}

		System.gc();
	}

	@Override
	public void memoryConsumption(int memoryLeftInMB)
	{
		// When 150 MB of memory or less, enable Java Garbage Collect after each Sample
		if (memoryLeftInMB <= MEMORY_LOW_ON_MEMORY_THRESHOLD_IN_MB)
		{
			_logger.info("called: memoryConsumption(memoryLeftInMB=" + memoryLeftInMB + ") - LOW_ON_MEMORY");
			
			// XML Plan Cache
			if (XmlPlanCache.hasInstance())
			{
				XmlPlanCache.getInstance().lowOnMemoryHandler();
			}

			// Persistent Counter Storage
			if (PersistentCounterHandler.hasInstance())
			{
				PersistentCounterHandler.getInstance().lowOnMemoryHandler();
			}

			System.gc();
		}

		// When 30 MB of memory or less, write some info about that.
		// and call some handler to act on low memory.
		if (memoryLeftInMB <= MEMORY_OUT_OF_MEMORY_THRESHOLD_IN_MB)
		{
			outOfMemoryHandler();
		}
	}
	/*---------------------------------------------------
	** END: implementing Memory.MemoryListener
	**---------------------------------------------------
	*/	

	/**
	 * Return a list of entries that specifies what CM's that should be used
	 * @param cmOptions
	 * @return
	 */
	private List<String> buildActiveCmList(String cmOptions)
	throws Exception
	{
		List<String> activeCmList = new ArrayList<String>();

//		activeCmList.add(SummaryPanel.CM_NAME);
		activeCmList.add(CounterController.getSummaryCmName());

		PropPropEntry ppe = null;
		if      (cmOptions.equalsIgnoreCase("small"))  ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_SMALL;
		else if (cmOptions.equalsIgnoreCase("medium")) ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_MEDIUM;
		else if (cmOptions.equalsIgnoreCase("large"))  ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_LARGE;
		else if (cmOptions.equalsIgnoreCase("all,"))   ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_ALL;
//		else if (cmOptions.equalsIgnoreCase("all"))
//		{
//			ppe = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_ALL;
//			for (CountersModel cm : _CMList)
//			{
//				if (cm == null)
//					continue;
//
//				if ( ! cm.isSystemCm() )
//				{
//					ppe.put(cm.getDisplayName(), "storePcs", "true");
//					ppe.put(cm.getDisplayName(), "postpone", cm.getPostponeTime()+"");
//				}
//			}
//		}

		// template AND add/remove individual CM's from the template
		PropPropEntry ppe2 = null;
		if      (cmOptions.startsWith("small,"))  ppe2 = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_SMALL;
		else if (cmOptions.startsWith("medium,")) ppe2 = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_MEDIUM;
		else if (cmOptions.startsWith("large,"))  ppe2 = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_LARGE;
		else if (cmOptions.startsWith("all,"))    ppe2 = CounterSetTemplates.SYSTEM_TEMPLATE_PCS_ON_ALL;

		
		//----------------------------------------
		// Some sort of TEMPLATE was found
		if (ppe != null)
		{
			for (String name : ppe.keySet())
			{
				boolean storePcs = ppe.getBooleanProperty(name, "storePcs", false);
				int     postpone = ppe.getIntProperty    (name, "postpone", 0);
				if (storePcs)
				{
					if (postpone <= 0)
						activeCmList.add(name);
					else
						activeCmList.add(name+":"+postpone);
				}
			}
			_logger.info("Found --noGui '"+cmOptions+"': enabling cm's named "+activeCmList);
		}
		//----------------------------------------
		// Some sort of TEMPLATE was found WITH add/remove individual CM's from the template
		else if (ppe2 != null)
		{
			// First get what CM's to be ADDED/REMOVED from the template
			ArrayList<String> addCmList    = new ArrayList<String>();
			ArrayList<String> removeCmList = new ArrayList<String>();
			String templateName = "unknown";
			String[] sa = cmOptions.split(",");
			for (String str : sa)
			{
				str = str.trim();
				if (str.equals("small") || str.equals("medium") || str.equals("large") || str.equals("all"))
				{
					templateName = str;
					continue;
				}

				String modifier = str.substring(0,1);
				String cmName   = str.substring(1);

				if (CounterSetTemplates.getShortName(cmName) == null && CounterSetTemplates.getLongName(cmName) == null)
				{
					throw new Exception("Unknown name '"+cmName+"', This wasn't found in the template '"+templateName+"' or any other template.");
				}

				if ("+".equals(modifier))
				{
					addCmList.add(cmName);
					_logger.info("Adding sampling of CM '"+cmName+"', to the template '"+templateName+"' for this session.");
				}
				else if ("-".equals(modifier))
				{
					if (CounterSetTemplates.getShortName(cmName) != null) removeCmList.add(CounterSetTemplates.getShortName(cmName));
					if (CounterSetTemplates.getLongName (cmName) != null) removeCmList.add(CounterSetTemplates.getLongName (cmName));
					_logger.info("Removing sampling of CM '"+cmName+"', from the template '"+templateName+"' for this session.");
				}
				else
				{
					throw new Exception("Unknown option '"+str+"', when add/remove CM's in template '"+templateName+"'. First char in modifier must be '+' or '-' to add/remove the CM from the template.");
//					_logger.warn("Unknown option '"+str+"', when add/remove CM's in template '"+templateName+"'. First char in modifier must be '+' or '-' to add/remove the CM from the template. This will be discarded.");
				}
			}

			// Then add all CM's in template to activeCmList
			for (String name : ppe2.keySet())
			{
				// But not in the remove list
				if (removeCmList.contains(name))
					continue;

				boolean storePcs = ppe2.getBooleanProperty(name, "storePcs", false);
				int     postpone = ppe2.getIntProperty    (name, "postpone", 0);
				if (storePcs)
				{
					if (postpone <= 0)
						activeCmList.add(name);
					else
						activeCmList.add(name+":"+postpone);
				}
			}

			// Finally add the ones in the ADD List
			for (String str : addCmList)
				activeCmList.add(str);

			_logger.info("Found --noGui '"+cmOptions+"': enabling cm's named "+activeCmList);
		}
		//----------------------------------------
		// No template just a list of CM's
		else
		{
			// option can look like: cm1,cm2,cm3:postponeTime,cm4,cm5
			String[] sa = cmOptions.split(",");
			for (String str : sa)
				activeCmList.add(str);

			_logger.info("Found --noGui aListOfCmNames: enabling cm's named "+activeCmList);
		}

		return activeCmList;
	}


	
	@Override
	public void shutdown()
	{
		_logger.info("Stopping the collector thread.");
		_running = false;
		if (_thread != null)
			_thread.interrupt();
	}

	@Override
	public void run()
	{
		// Set the Thread name
		_thread = Thread.currentThread();
		_thread.setName(THREAD_NAME);
		
		_running = true;
		
		// WAIT for a DEFERRED start
		if (_deferedStartTime != null)
		{
			try { PersistWriterBase.waitForRecordingStartTime(_deferedStartTime, null); }
			catch(InterruptedException ignore) {}
		}

		// If you want to start a new session in the Persistent Storage, just set this to true...
		// This could for instance be used when you connect to a new DBMS Server
		boolean startNewPcsSession = false;

		// Get the JVM parameters, find out: -XX:HeapDumpPath=C:\Users\gorans\.dbxtune\data
		// which is later used to remove older "*.hprof" dump files
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmStartArguments = runtimeMxBean.getInputArguments();
		File heapDumpPath = null;        // The path where: -XX:HeapDumpPath
		long heapDumpPathLastCheck = 0;  // used later: not to check this on every loop, but instead every X hour
		for (String arg : jvmStartArguments)
		{
			if (arg.startsWith("-XX:HeapDumpPath="))
			{
				String[] sa = arg.split("=");
				if (sa.length >= 2)
				{
					heapDumpPath = new File(sa[1]);
					if ( ! heapDumpPath.exists() )
						heapDumpPath = null;
				}
			}
		}

		// loop
		@SuppressWarnings("unused")
		int loopCounter = 0;

		MonTablesDictionary mtd = null;
		
		//---------------------------
		// START the Persistent Storage thread
		//---------------------------
		PersistentCounterHandler pch = null;
		try
		{
			IObjectLookupInspector oli = DbxTune.getInstance().createPcsObjectLookupInspector();
			ISqlCaptureBroker      scb = DbxTune.getInstance().createPcsSqlCaptureBroker();

			pch = new PersistentCounterHandler(oli, scb);
			pch.init( _storeProps );
			pch.start();
			
			PersistentCounterHandler.setInstance(pch);
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
		// START Heart Beat Monitor thread
		// If we do not PING this monitor it will start to do stacktraces of all threads...
		// This so we can debug what's happening on the system. 
		// This is used if the collector thread get "stuck" or "freezes" for some reason
		//---------------------------
		HeartbeatMonitor.setRestartEnabled(true);
		HeartbeatMonitor.setRestartTime(30*60); // After 30 minutes of "no heartbeat" -->> restart the system by doing: System.exit(8)
		
		HeartbeatMonitor.setAlarmTime(_sleepTime * 4); // Dump threads if no Heartbeat has been issued for a while (4 times the sleep time)
		HeartbeatMonitor.setSleepTime(_sleepTime / 2); // check every now and then... (half the sleep time seems reasonable)
		HeartbeatMonitor.start();


		// remember when we started
		long threadStartTime = System.currentTimeMillis();

		_logger.info("Thread '"+Thread.currentThread().getName()+"' starting...");

		//---------------------------
		// NOW LOOP
		//---------------------------
		while (_running)
		{
			// notify the heartbeat that we are still running...
			// This is also done right before we go to sleep (waiting for next data collection)
			HeartbeatMonitor.doHeartbeat();
			
			// Check if current MONITOR-DBMS connection is lost
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
			if ( ! getCounterController().isMonConnected(true, true))
			{
				_logger.debug("Connecting to DBMS server using. user='"+_dbmsUsername+"', passwd='"+_dbmsPassword+"', hostPortStr='"+_dbmsHostPortStr+"'. dbmsServer='"+_dbmsServer+"'");
				_logger.info( "Connecting to DBMS server using. user='"+_dbmsUsername+"', passwd='"+ "*hidden*" +"', hostPortStr='"+_dbmsHostPortStr+"'. dbmsServer='"+_dbmsServer+"'");

//				// get a connection
//				try
//				{
//					// FIXME: this doesn't work for OTHER DBMS than Sybase...
//					AseConnectionFactory.setUser(_dbmsUsername); // Set this just for SendConnectInfo uses it
//					AseConnectionFactory.setHostPort(_dbmsHostPortStr);
//
//					Connection conn = AseConnectionFactory.getConnection(_dbmsHostPortStr, null, _dbmsUsername, _dbmsPassword, Version.getAppName()+"-nogui", Version.getVersionStr(), null, (Properties)null, null);
////					getCounterController().setMonConnection( conn);
//					getCounterController().setMonConnection( DbxConnection.createDbxConnection(conn) );
////					getCounterController().getMonConnection().reConnect();
//
//					// set the connection props so it can be reused...
//					// FIXME: This is very ASE Specific right now... it needs to be more generic for DbxTune (sqlServerTune, oracleTune, etc)
////					ConnectionProp cp = new ConnectionProp();
//					cp.setLoginTimeout ( 20 );
//					cp.setDriverClass  ( AseConnectionFactory.getDriver() );
//					cp.setUrl          ( AseConnectionFactory.getUrlTemplateBase() + AseConnectionFactory.getHostPortStr() );
////					cp.setUrlOptions   ( tdsUrlOptions );
//					cp.setUsername     ( _dbmsUsername );
//					cp.setPassword     ( _dbmsPassword );
//					cp.setAppName      ( Version.getAppName() );
//					cp.setAppVersion   ( Version.getVersionStr() );
////					cp.setHostPort     ( hosts, ports );
////					cp.setSshTunnelInfo( sshTunnelInfo );
//
//					DbxConnection.setDefaultConnProp(cp);
//					
//					// ASE: 
//					// XML Plan Cache... maybe it's not the perfect place to initialize this...
//					XmlPlanCache.setInstance( new XmlPlanCacheAse( new ConnectionProvider()
//					{
//						@Override
//						public DbxConnection getNewConnection(String appname)
//						{
//							try 
//							{
//								return DbxConnection.connect(null, appname);
//							} 
//							catch(Exception e) 
//							{
//								_logger.error("Problems getting a new connection. Caught: "+e, e);
//								return null;
//							}
//						}
//						
//						@Override
//						public DbxConnection getConnection()
//						{
//							return getCounterController().getMonConnection();
//						}
//					}) );
//
//					
//					// CHECK the connection for proper configuration.
//					// If failure, go and FIX
//					// FIXME: implement the below "set minimal logging options"
//					if ( ! AseConnectionUtils.checkForMonitorOptions(getCounterController().getMonConnection(), _dbmsUsername, false, null) )
//					{
//						AseConnectionUtils.setBasicAseConfigForMonitoring(getCounterController().getMonConnection());
//					}
//
//					// CHECK the connection for proper configuration.
//					// The fix did not work, so lets get out of here
//					if ( ! AseConnectionUtils.checkForMonitorOptions(getCounterController().getMonConnection(), _dbmsUsername, false, null) )
//					{
//						_logger.error("Problems when checking the ASE Server for 'proper monitoring configuration'.");
//
//						// Disconnect, and get out of here...
//						getCounterController().closeMonConnection();
//						
//						// THE LOOP WILL BE FALSE (_running = false)
//						_running = false;
//
//						// START AT THE TOP AGAIN
//						continue;
//					}
//
//					// Do this later, when the MonTablesDictionary is initialized
//					//CheckForUpdates.sendConnectInfoNoBlock(ConnectionDialog.TDS_CONN, null);
//				}
//				catch (SQLException e)
//				{
//					String msg = AseConnectionUtils.getMessageFromSQLException(e, false); 
//					_logger.error("Problems when connecting to a ASE Server. "+msg);
//
//					// JZ00L: Login failed
//					if (e.getSQLState().equals("JZ00L"))
//					{
//						// THE LOOP WILL BE FALSE (_running = false)
//						_running = false;
//
//						_logger.error("Faulty PASSWORD when connecting to the server '"+_dbmsServer+"' at '"+_dbmsHostPortStr+"', with user '"+_dbmsUsername+"', I cant recover from this... exiting...");
//
//						// GET OUT OF THE LOOP, causing us to EXIT
//						break;
//					}
//				}
//				catch (Exception e)
//				{
//					_logger.error("Problems when connecting to a ASE Server. "+e);
//				}

				// get a connection
				SQLException connectException = null;
				String       connectInfoMsg   = null;
				try
				{
					// Should we try to do a "re-connect"...
//					if ( DbxConnection.hasDefaultConnProp() )
//					{
//					}
						
					if (System.getProperty("nogui.password.print", "false").equalsIgnoreCase("true"))
						System.out.println("#### DEBUG ####: Connecting to DBMS server using. user='"+_dbmsUsername+"', passwd='"+_dbmsPassword+"', hostPortStr='"+_dbmsHostPortStr+"'. dbmsServer='"+_dbmsServer+"'");

					// Make a connection using any specific implementation for the installed counter controller
					DbxConnection conn = getCounterController().noGuiConnect(_dbmsUsername, _dbmsPassword, _dbmsServer, _dbmsHostPortStr, _jdbcUrlOptions);

					// Set the connection to be used
					//getCounterController().setMonConnection( DbxConnection.createDbxConnection(conn) );
					getCounterController().setMonConnection(conn);

					// Check "stuff"
					if ( ! DbxConnection.hasDefaultConnProp() )
						_logger.warn("No Default Connection Properties was specified...");

					// Special thing for AlrmWriters, set SERVERNAME
					try {
						String dbmsServerName = conn.getDbmsServerName();
						if (StringUtil.hasValue(dbmsServerName))
							System.setProperty("SERVERNAME", DbxTune.stripSrvName(dbmsServerName));
					} catch (Exception ignore) {}
				}
				catch (SQLException ex)
				{
					// connection failed, and we should retry
					// Do nothing here, later on in the code will will (send alarms), and start at the top again
					connectException = ex;
					connectInfoMsg   = "Username='"+_dbmsUsername+"', Password='"+ "*secret*"    +"', Server='"+_dbmsServer+"', HostPortStr='"+_dbmsHostPortStr+"', UrlOptions='"+_jdbcUrlOptions+"'. Caught: "+ex;
					
					// But at least, log the exception...
					_logger.info ("Problems connecting to DBMS, retry  will be done later. Username='"+_dbmsUsername+"', Password='"+ "*secret*"    +"', Server='"+_dbmsServer+"', HostPortStr='"+_dbmsHostPortStr+"', UrlOptions='"+_jdbcUrlOptions+"'. Caught: "+ex);
					_logger.debug("Problems connecting to DBMS, retry  will be done later. Username='"+_dbmsUsername+"', Password='"+ _dbmsPassword +"', Server='"+_dbmsServer+"', HostPortStr='"+_dbmsHostPortStr+"', UrlOptions='"+_jdbcUrlOptions+"'. Caught: "+ex);
				}
				catch (Exception ex)
				{
					// Disconnect, and get out of here...
					getCounterController().closeMonConnection();
					
					// THE LOOP WILL BE FALSE (_running = false)
					_running = false;

					// GET OUT OF THE LOOP, causing us to EXIT
					break;
				}


				if ( ! getCounterController().isMonConnected(true, true) )
				{
					_logger.error("Problems connecting to DBMS server. sleeping for "+_sleepOnFailedConnectTime+" seconds before retry...");

					// Send ALARM: Server is down (note this will also issue and endOfScan in the AlarmHandler)
					String fallbackSrvName = _dbmsHostPortStr;
					if (StringUtil.hasValue(_dbmsServer))      fallbackSrvName = _dbmsServer;
					if (StringUtil.hasValue(_dbmsServerAlias)) fallbackSrvName = _dbmsServerAlias;
					
					sendAlarmServerIsDown(fallbackSrvName, connectException, connectInfoMsg);

					// Sleep a short while
					getCounterController().sleep(_sleepOnFailedConnectTime * 1000);
					
					// START AT THE TOP AGAIN
					continue;
				}
				
				// initialize Mon Table Dictionary
				mtd = MonTablesDictionaryManager.getInstance();
				if ( ! mtd.isInitialized() )
				{
					mtd.initialize(getCounterController().getMonConnection(), false);
				}
				
				try
				{
					// initialize DBMS Config Dictionary
					if (DbmsConfigManager.hasInstance())
					{
						IDbmsConfig dbmsCfg = DbmsConfigManager.getInstance();
						if ( ! dbmsCfg.isInitialized() )
							dbmsCfg.initialize(getCounterController().getMonConnection(), false, false, null);
					}
					
					// initialize DBMS Config Text Dictionary
					if (DbmsConfigTextManager.hasInstances())
						DbmsConfigTextManager.initializeAll(getCounterController().getMonConnection(), false, false, null);
				}
				catch(SQLException ex) 
				{
					_logger.info("Initialization of the DBMS Configuration did not succeed. Caught: "+ex); 
				}

				// for ASE only
				// Check if we have License issues... send alarm...
				DbxConnection conn = getCounterController().getMonConnection();
				if (conn != null && conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_SYBASE_ASE))
				{
//					String gracePeriodWarning   = AseConnectionUtils.getAseGracePeriodWarning(conn);
					String gracePeriodWarning   = AseLicensInfo.getAseGracePeriodWarning(conn);
					if (StringUtil.hasValue(gracePeriodWarning))
					{
						_logger.warn(gracePeriodWarning);
						
						if (AlarmHandler.hasInstance())
						{
							AlarmEvent alarmEvent = new AlarmEventAseLicensExpiration(conn.getDbmsServerNameNoThrow(), gracePeriodWarning);
							AlarmHandler.getInstance().addAlarm(alarmEvent);
						}
					}
				}
				
			} // end: not connected

			// HOST Monitoring connection
			if ( ! getCounterController().isHostMonConnected() )
			{
				if (_sshHostname != null && _sshUsername != null && (_sshPassword != null || _sshKeyFile != null))
				{
					_logger.info( "Connecting to SSH server using. user='"+_sshUsername+"', passwd='"+ "*hidden*" +"', port='"+_sshPort+"'. hostname='"+_sshHostname+"', keyFile='"+_sshKeyFile+"'.");
					if (System.getProperty("nogui.password.print", "false").equalsIgnoreCase("true"))
						System.out.println("#### DEBUG ####: Connecting to SSH server using. user='"+_sshUsername+"', passwd='"+ _sshPassword +"', port='"+_sshPort+"', hostname='"+_sshHostname+"', keyFile='"+_sshKeyFile+"'.");
	
					// get a connection
					try
					{
						SshConnection sshConn = new SshConnection(_sshHostname, _sshPort, _sshUsername, _sshPassword, _sshKeyFile);
						sshConn.connect();
						getCounterController().setHostMonConnection(sshConn);
					}
					catch (IOException e)
					{
						_logger.error("Host Monitoring: Failed to connect to SSH hostname='"+_sshHostname+"', user='"+_sshUsername+"'.", e);
					}
				}
			}
				
			loopCounter++;
			
			// When 10 MB of memory or less, write some info about that.
			Memory.checkMemoryUsage(10);

			// Set the CM's to be in normal refresh state
			for (CountersModel cm : getCounterController().getCmList())
			{
				if (cm == null)
					continue;

				cm.setState(CountersModel.State.NORMAL);
			}

			try
			{
				getCounterController().setInRefresh(true);

				// Initialize the counters, now when we know what 
				// release we are connected to
				if ( ! getCounterController().isInitialized() )
				{
					DbxConnection xconn = getCounterController().getMonConnection();
					
					getCounterController().initCounters( 
							getCounterController().getMonConnection(), 
							false, 
							mtd.getDbmsExecutableVersionNum(), 
							mtd.isClusterEnabled(), 
							mtd.getDbmsMonTableVersion());

					// Hopefully this is a better place to send connect info
//					CheckForUpdates.sendConnectInfoNoBlock(ConnectionDialog.TDS_CONN, null);
//					CheckForUpdates.getInstance().sendConnectInfoNoBlock(ConnectionDialog.TDS_CONN, null);

					DbxConnectInfo ci = new DbxConnectInfo(xconn, true);
//					ci.setSshTunnelInfo(fixme-if-this-is-used);
					CheckForUpdates.getInstance().sendConnectInfoNoBlock(ci);
				}

				if (getCounterController().getCmList() == null || (getCounterController().getCmList() != null && getCounterController().getCmList().size() == 0))
				{
					_logger.error("The list of known CM's is either null or empty, can't continue...");
					continue;
				}

				// Do various other checks in the system, for instance in DBMS do: checkForFullTransLogInMaster()
				getCounterController().checkServerSpecifics();

				// Get some Header information that will be used by the PersistContainer sub system
				PersistContainer.HeaderInfo headerInfo = getCounterController().createPcsHeaderInfo();
				if (headerInfo == null)
				{
					_logger.warn("No 'header information' object could be created... starting at top of the while loop.");
					continue;
				}

				// Save header info, so we can send AlarmEvents to DbxCentral (in case of connectivity issues) and ruse the servername and other fields.
				_lastKnownHeaderInfo = headerInfo;

				// If there is a ServerAlias, apply that... This is used by the DbxCentral for an alternate schema/servername
				if (StringUtil.hasValue(_dbmsServerAlias))
					headerInfo.setServerNameAlias(_dbmsServerAlias);

				
				// PCS
				PersistContainer pc = new PersistContainer(headerInfo);
				if (startNewPcsSession)
					pc.setStartNewSample(true);
				startNewPcsSession = false;

				// add some statistics on the "main" sample level
				getCounterController().setStatisticsTime(headerInfo.getMainSampleTime());

				// Set SERVERNAME to where we are currently connected 
				if ( StringUtil.isNullOrBlank(headerInfo.getServerNameOrAlias()) )
					_logger.warn("DBMS Server Name is null. Please set the server in ICounterController method: createPcsHeaderInfo()");
				else
					System.setProperty("SERVERNAME", DbxTune.stripSrvName(headerInfo.getServerNameOrAlias()));
				

				// Keep a list of all the CM's that are refreshed during this loop
				// This one will be passed to doPostRefresh()
				LinkedHashMap<String, CountersModel> refreshedCms = new LinkedHashMap<String, CountersModel>();

				// Clear the demand refresh list
				getCounterController().clearCmDemandRefreshList();

				//-----------------
				// LOOP all CounterModels, and get new data, 
				//   if it should be done
				//-----------------
				for (CountersModel cm : getCounterController().getCmList())
				{
					if (cm != null && cm.isRefreshable())
					{
						cm.setServerName(      headerInfo.getServerNameOrAlias()); // or should we just use getServerName()
						cm.setSampleTimeHead(  headerInfo.getMainSampleTime());
						cm.setCounterClearTime(headerInfo.getCounterClearTime());

						try
						{
							cm.setSampleException(null);
							cm.refresh();
//							cm.refresh(getMonConnection());
	
							// Add it to the list of refreshed cm's
							refreshedCms.put(cm.getName(), cm);

							// move this into cm.refresh()
//							cm.setValidSampleData( (cm.getRowCount() > 0) ); 

							// Add the CM to the container, which will 
							// be posted to persister thread later.
							pc.add(cm);
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


				//-----------------
				// POST Refresh handling
				//-----------------
				_logger.debug("---- Do POST Refreshing...");
				for (CountersModel cm : getCounterController().getCmList())
				{
					if ( cm == null )
						continue;

					cm.doPostRefresh(refreshedCms);
				}

				
				//-----------------
				// AlarmHandler: end-of-scan: Cancel any alarms that has not been repeated
				//-----------------
				if (AlarmHandler.hasInstance())
				{
					// Get the Alarm Handler
					AlarmHandler ah = AlarmHandler.getInstance();
					
					// Generate a DummyAlarm if the file '/tmp/${SRVNAME}.dummyAlarm.deleteme exists' exists.
					ah.checkSendDummyAlarm(pc.getServerNameOrAlias());

					ah.endOfScan();           // This is synchronous operation (if we want to stuff Alarms in the PersistContainer before it's called/sent)
//					ah.addEndOfScanToQueue(); // This is async operation

					// Add any alarms to the Persist Container.
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
				pch.add(pc);

			}
			catch (Throwable t)
			{
				_logger.error(Version.getAppName()+": error in GetCounters loop.", t);

				if (t instanceof OutOfMemoryError)
				{
					_logger.error(Version.getAppName()+": in GetCounters loop, caught 'OutOfMemoryError'. Calling: Memory.fireOutOfMemory(), which hopefully will release some memory.");
					Memory.fireOutOfMemory();
				}
			}
			finally
			{
				getCounterController().setInRefresh(false);
			}

			//-----------------------------
			// Time to exit ??
			//-----------------------------
//			if (_shutdownAfterXHours > 0)
//			{
//				long runningForXHours = (System.currentTimeMillis() - threadStartTime) / 1000 / 60 / 60;
//				//long runningForXHours = (System.currentTimeMillis() - threadStartTime) / 1000 / 60; // use this for minutes instead... debugging
//
//				if ( runningForXHours >= _shutdownAfterXHours)
//				{
//					String startDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(threadStartTime));
//
//					_logger.info("Shutting down the 'no-gui' service after "+runningForXHours+" hours of service. It was started at '"+startDateStr+"'.");
//					break;
//				}
//			}
			if (_shutdownAtTime != null)
			{
				long now = System.currentTimeMillis();

				if ( now > _shutdownAtTime.getTime())
				{
					String startDateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(threadStartTime));

					_logger.info("Shutting down the 'no-gui' service. Stop time was set to '"+_shutdownAtTime+"'. It was started at '"+startDateStr+"'.");
					break;
				}
			}

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
					conn.reConnect(null);
				}
				catch (Exception ex)
				{
					_logger.error("Problems during 're-connect' after a sample is finished. Caught: "+ex);
				}
			}

			//-----------------------------
			// Do Java Garbage Collection?
			//-----------------------------
			boolean doJavaGcAfterRefresh = Configuration.getCombinedConfiguration().getBooleanProperty(MainFrame.PROPKEY_doJavaGcAfterRefresh, MainFrame.DEFAULT_doJavaGcAfterRefresh);
			if (doJavaGcAfterRefresh)
			{
				getCounterController().setWaitEvent("Doing Java Garbage Collection.");
				System.gc();
			}

			//-----------------------------
			// Remove old debug files: for -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/home/sybase/.dbxtune
			//   - java_pid${dpif}.hprof
			//-----------------------------
			if (heapDumpPath != null)
			{
				long timeSinceLastCheck = System.currentTimeMillis() - heapDumpPathLastCheck;
				if (timeSinceLastCheck > TimeUnit.HOURS.toMillis(1)) // Only check this once an hour
				{
					_logger.info("Checking for Java HPROF file(s) at directory '" + heapDumpPath + "'. heapDumpPath.exists=" + heapDumpPath.exists());

					if (heapDumpPath.exists())
					{
						try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(heapDumpPath.toPath(), "*.hprof"))
						{
							int fileCount             = 0;
							int fileMatchCount        = 0;
							int fileNoMatchCount      = 0;
							int fileDeleteCount       = 0;
							int fileNotOldEnoughCount = 0;
							
							for (Path path : directoryStream)
							{
								fileCount++;
								_logger.debug("HPROF file was found. now checking filename='" + path.getFileName() + "', fullFilename='" + path + "'.");
								
								// Note: Use toString() here otherwise we will use Path.endsWith(), which is NOT the same as we want to do...
								if (path.getFileName().toString().toLowerCase().endsWith(".hprof")) // Just to be "safe" since we are about to delete files
								{
									fileMatchCount++;

									long ageInMs = System.currentTimeMillis() - path.toFile().lastModified();
									if (ageInMs > TimeUnit.DAYS.toMillis(7))
									{
										fileDeleteCount++;
										_logger.info("Deleting old HPROF file, wich is older than 7 days (lastMod='" + (new Timestamp(path.toFile().lastModified())) + "', sizeInMb=" + (path.toFile().length()/1024/1024) + ", filename='" + path + "').");
										path.toFile().delete();
									}
									else
									{
										fileNotOldEnoughCount++;
										_logger.info("DEBUG: HPROF file was found, but not yet older than 7 days, this can be manually removed if you like. (lastMod='" + (new Timestamp(path.toFile().lastModified())) + "', sizeInMb=" + (path.toFile().length()/1024/1024) + ", filename='" + path + "').");
									}
								}
								else
								{
									fileNoMatchCount++;
									_logger.debug("this is NOT a HPROF file, skipping this file. filename='" + path.getFileName() + "', fullFilename='" + path + "'.");
								}

								_logger.info("Checking for Java HPROF file(s) is complete. fileCount=" + fileCount + ", matchCount=" + fileMatchCount + ", noMatchCount=" + fileNoMatchCount + ", deleteCount=" + fileDeleteCount + ", notOldEnoughCount=" + fileNotOldEnoughCount + ".");
							}
						}
						catch (IOException ex)
						{
							_logger.error("Problems reading HPROF HeapDumpPath '" + heapDumpPath + "' when checking/cleaning-up older '*.hprof' files.", ex);
						}
					}
					
					heapDumpPathLastCheck = System.currentTimeMillis();
				}
			}
			

			// If previous CHECK has DEMAND checks, lets sleep for a shorter while
			// This so we can catch data if the CM's are not yet initialized and has 2 samples (has diff data)
			int sleepTime = getCounterController().getCmDemandRefreshSleepTime(_sleepTime, getCounterController().getLastRefreshTimeInMs());

			//-----------------------------
			// Sleep
			//-----------------------------
			if (_logger.isDebugEnabled())
			{
				getCounterController().setWaitEvent("next sample period...");
				_logger.debug("Sleeping for "+sleepTime+" seconds. Waiting for " + getCounterController().getWaitEvent() );
			}

			// notify the heartbeat that we are still running...
			HeartbeatMonitor.doHeartbeat();

			// Sleep / wait for next sample
			if (_scriptWaitForNextSample != null)
				scriptWaitForNextSample();
			else
				getCounterController().sleep(sleepTime * 1000);

		} // END: while(_running)

		// so lets stop the Persistent Counter Handler and it's services as well
		if (pch != null)
		{
			int maxWaitTimeInMs = 10 * 1000;
			_logger.info("Stopping the PCS Thread (and it's sub threads). maxWaitTimeInMs="+maxWaitTimeInMs);
			pch.stop(true, maxWaitTimeInMs);
		}

		_logger.info("Thread '"+Thread.currentThread().getName()+"' ending, this should lead to a server STOP.");
		
//		_logger.info("DUMMY WHICH SHOULD BE REMOVED... ONLY FOR TESTING PURPOSES OF ShutdownHook TIMEOUT... sleeping for 99 sec...");
//		try { Thread.sleep(99 * 1000); }
//		catch(InterruptedException ex) { ex.printStackTrace(); }
	}

	/**
	 * User defined exit, to wait for next "data refresh" to happen
	 * <p>  
	 * Call Java Script every X milliseconds to determine if we should continue and sample data, or if we should wait.
	 * <p>
	 * The java script returns number of milliseconds until next check.<br>
	 * If the Java script returns 0 or less, then it's time for next check.<br>
	 * <p>
	 * So "sleep" can be done in the Java Script Code, or return a number, which means the sleep is done by the Java Code.
	 * This might help to create a "simpler" Java Script, since sleep is not supported by Java Script...
	 * 
	 */
	private void scriptWaitForNextSample()
	{
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

		// Set initial start time for the sleep
		engine.put("waitStartedAtTime", System.currentTimeMillis());

		try
		{
			while (true)
			{
				String script = _scriptWaitForNextSample;

				// Run the JavaScript
				Object rc = engine.eval(script);
				
				// Nothing was returned, use fall back and do sleep
				if (rc == null)
					throw new ScriptException("scriptWaitForNextSample(): NO return code from the java script code.");

				// If JavaScript returns a number: 0=no more wait, >0=sleep for this amount of ms and call script again.
				if (rc instanceof Number)
				{
					// No more wait, if it's 0 or below
					int waitTime = ((Number) rc).intValue();
					if (waitTime <= 0)
						return;
					else
					{
						// Wait for X milliseconds, returned from the JavaScript, and try again.
						getCounterController().sleep(waitTime);
						continue;
					}
				}

				// If we got here, the script did NOT return a number
				throw new ScriptException("scriptWaitForNextSample(): unknown return code '"+rc+"' of type '"+rc.getClass().getName()+"'.");
			}
		}
		catch (ScriptException e)
		{
			_logger.warn("JavaScript problems with 'WaitForNextSample' JavaScriptCode '"+_scriptWaitForNextSample+"'. Falling back to 'sleep("+_sleepTime+")' Caught: "+e);
			getCounterController().sleep(_sleepTime * 1000);
		}
	}
}
