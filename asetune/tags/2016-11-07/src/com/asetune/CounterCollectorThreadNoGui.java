package com.asetune;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesDbx.DbxConnectInfo;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CountersModel;
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
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.MandatoryPropertyException;
import com.asetune.utils.Memory;
import com.asetune.utils.PropPropEntry;
import com.asetune.utils.StringUtil;


public class CounterCollectorThreadNoGui
extends CounterCollectorThreadAbstract
{
	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(CounterCollectorThreadNoGui.class);


	public CounterCollectorThreadNoGui(CounterControllerAbstract counterController)
	{
		super(counterController);
	}

	/** a connection to the ASE server to monitor */
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

	/** If no connection can be made to the ASE server, sleep time before retry */
	private int        _sleepOnFailedConnectTime = 60;

	/** Connection information */
	private String     _aseUsername = null;
	private String     _asePassword = null; 
	private String     _aseServer   = null;
//	private String     _aseHostname = null;
//	private String     _asePort     = null;
	private String     _aseHostPortStr = null;

	private String     _sshUsername    = null;
	private String     _sshPassword    = null; 
	private String     _sshHostname    = null;
	private String     _sshPortStr     = null;
	private int        _sshPort        = 22;
	
	private Configuration _storeProps = null;
	
	private boolean       _running = true;

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
		String connPrefix    = "conn.";

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
		Configuration.setSearchOrder( Configuration.PCS );
		String userConfigFile    = Configuration.getInstance(Configuration.USER_CONF) == null ? "not set"                    : Configuration.getInstance(Configuration.USER_CONF).getFilename();
		String userTmpConfigFile = Configuration.getInstance(Configuration.USER_TEMP) == null ? "not set"                    : Configuration.getInstance(Configuration.USER_TEMP).getFilename();
		String pcsConfigFile     = cmTemplateOption                                   != null ? "template="+cmTemplateOption : _storeProps.getFilename();
		_logger.info("Combined Configuration Search Order has been changed to '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'. This means that USER_CONF='"+userConfigFile+"' and USER_TEMP='"+userTmpConfigFile+"' wont be used for as fallback configurations. Only the PCS='"+pcsConfigFile+"' config will be used.");

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
		_aseUsername    = _storeProps.getProperty(connPrefix+"aseUsername");
		_asePassword    = _storeProps.getProperty(connPrefix+"asePassword"); 
		_aseServer      = _storeProps.getProperty(connPrefix+"aseName");
		String aseHosts = _storeProps.getProperty(connPrefix+"aseHost");
		String asePorts = _storeProps.getProperty(connPrefix+"asePort");
		_aseHostPortStr = _storeProps.getProperty(connPrefix+"aseHostPort");

		_sshUsername    = _storeProps.getProperty(connPrefix+"sshUsername");
		_sshPassword    = _storeProps.getProperty(connPrefix+"sshPassword"); 
		_sshHostname    = _storeProps.getProperty(connPrefix+"sshHostname");
		_sshPortStr     = _storeProps.getProperty(connPrefix+"sshPort");
		if (_sshPortStr != null && ! _sshPortStr.equals(""))
			_sshPort = Integer.parseInt(_sshPortStr);
		
		// Override prop values with the Command line parameters if any. 
//		CommandLine cmd = AseTune.getCmdLineParams();
//		if (cmd.hasOption('U'))	_aseUsername = cmd.getOptionValue('U');
//		if (cmd.hasOption('P'))	_asePassword = cmd.getOptionValue('P');
//		if (cmd.hasOption('S'))	_aseServer   = cmd.getOptionValue('S');
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


		
		//-----------------------------------
		// Still no password, read it from the STDIN
		if (_asePassword == null)
		{
			Console cons = System.console();
			if (cons != null)
			{
				System.out.println("-----------------------------------------------------------------------------");
				System.out.println("No password for ASE was specified use command line parameter -P or property '"+connPrefix+"asePassword' in the file '"+_storeProps.getFilename()+"'.");
				System.out.println("Connecting to server '"+_aseServer+"' at '"+_aseHostPortStr+"' with the user name '"+_aseUsername+"'.");
				System.out.println("-----------------------------------------------------------------------------");
				char[] passwd = cons.readPassword("Password: ");
				_asePassword = new String(passwd);
				//System.out.println("Read ASE password from Console '"+_asePassword+"'.");
			}
		}
		// treat null password
		if (_asePassword.equalsIgnoreCase("null"))
			_asePassword = "";

		
		//-----------------------------------
		// read PASSWORD FOR SSH...
		if (_sshHostname != null && _sshPassword == null)
		{
			Console cons = System.console();
			if (cons != null)
			{
				System.out.println("-----------------------------------------------------------------------------");
				System.out.println("No SSH password was specified use command line parameter -p or property '"+connPrefix+"sshPassword' in the file '"+_storeProps.getFilename()+"'.");
				System.out.println("Connecting to host name '"+_sshHostname+"' with the user name '"+_sshUsername+"'.");
				System.out.println("-----------------------------------------------------------------------------");
				char[] passwd = cons.readPassword("Password: ");
				_sshPassword = new String(passwd);
				//System.out.println("Read SSH password from Console '"+_sshPassword+"'.");
			}
		}
		// treat null password
		if (_sshPassword != null && _sshPassword.equalsIgnoreCase("null"))
			_sshPassword = "";

		
		String configStr = 
			"sleepTime='"+_sleepTime+"', " +
			"scriptWaitForNextSample='"+(_scriptWaitForNextSample == null ? "using sleepTime" : "using JavaScript")+"', " +
//			"shutdownAfterXHours='"+_shutdownAfterXHours+"', " +
			"shutdownAfterXHours='"+_shutdownAtTimeStr+"', " +
			"startRecordingAtTime='"+_deferedStartTime+"', " +
			"sleepOnFailedConnectTime='"+_sleepOnFailedConnectTime+"', " +
			"_aseUsername='"+_aseUsername+"', " +
			"_asePassword='*hidden*', " +
			"_aseServer='"+_aseServer+"("+_aseHostPortStr+")', " +
			"_sshUsername='"+_sshUsername+"', " +
			"_sshPassword='*hidden*', " +
			"_sshHostname='"+_sshHostname+"', " +
			"_sshPort='"+_sshPort+"', " +
			".";
		_logger.info("Configuration for NO-GUI sampler: "+configStr);

		if (_scriptWaitForNextSample != null)
			_logger.info("Using Java Script when waiting for next sample period. Script: "+_scriptWaitForNextSample);

		//---------------------------
		// Create all the CM objects, the objects will be added to _CMList
		getCounterController().createCounters(hasGui);
		
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
			if (_sshHostname == null || _sshUsername == null || _sshPassword == null)
				throw new Exception("There are "+activeCountHostMon+" active Performance Counters that are doing Host Monitoring, this is using SSH for communication, but no hostname/user/passwd is given. hostname='"+_sshHostname+"', username='"+_sshUsername+"', password='"+(_sshPassword==null?null:"*has*passwd*")+"'.");
		}
	}

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
		_thread.setName("GetCountersNoGui");
		
		_running = true;
		
		// WAIT for a DEFERRED start
		if (_deferedStartTime != null)
		{
			try { PersistWriterBase.waitForRecordingStartTime(_deferedStartTime, null); }
			catch(InterruptedException ignore) {}
		}

		// If you want to start a new session in the Persistent Storage, just set this to true...
		// This could for instance be used when you connect to a new ASE Server
		boolean startNewPcsSession = false;

		// loop
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

		// remember when we started
		long threadStartTime = System.currentTimeMillis();

		_logger.info("Thread '"+Thread.currentThread().getName()+"' starting...");

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
			if ( ! getCounterController().isMonConnected(true, true))
			{
				_logger.debug("Connecting to ASE server using. user='"+_aseUsername+"', passwd='"+_asePassword+"', hostPortStr='"+_aseHostPortStr+"'. aseServer='"+_aseServer+"'");
				_logger.info( "Connecting to ASE server using. user='"+_aseUsername+"', passwd='"+ "*hidden*" +"', hostPortStr='"+_aseHostPortStr+"'. aseServer='"+_aseServer+"'");

				// get a connection
				try
				{
					// FIXME: this doesn't work for OTHER DBMS than Sybase...
					AseConnectionFactory.setUser(_aseUsername); // Set this just for SendConnectInfo uses it
					AseConnectionFactory.setHostPort(_aseHostPortStr);

					Connection conn = AseConnectionFactory.getConnection(_aseHostPortStr, null, _aseUsername, _asePassword, Version.getAppName()+"-nogui", Version.getVersionStr(), (Properties)null, null);
//					getCounterController().setMonConnection( conn);
					getCounterController().setMonConnection( DbxConnection.createDbxConnection(conn) );
//					getCounterController().getMonConnection().reConnect();

					// CHECK the connection for proper configuration.
					// If failure, go and FIX
					// FIXME: implement the below "set minimal logging options"
					if ( ! AseConnectionUtils.checkForMonitorOptions(getCounterController().getMonConnection(), _aseUsername, false, null) )
					{
						AseConnectionUtils.setBasicAseConfigForMonitoring(getCounterController().getMonConnection());
					}

					// CHECK the connection for proper configuration.
					// The fix did not work, so lets get out of here
					if ( ! AseConnectionUtils.checkForMonitorOptions(getCounterController().getMonConnection(), _aseUsername, false, null) )
					{
						_logger.error("Problems when checking the ASE Server for 'proper monitoring configuration'.");

						// Disconnect, and get out of here...
						getCounterController().closeMonConnection();
						
						// THE LOOP WILL BE FALSE (_running = false)
						_running = false;

						// START AT THE TOP AGAIN
						continue;
					}

					// Do this later, when the MonTablesDictionary is initialized
					//CheckForUpdates.sendConnectInfoNoBlock(ConnectionDialog.TDS_CONN, null);
				}
				catch (SQLException e)
				{
					String msg = AseConnectionUtils.getMessageFromSQLException(e, false); 
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


				if ( ! getCounterController().isMonConnected(true, true) )
				{
					_logger.error("Problems connecting to ASE server. sleeping for "+_sleepOnFailedConnectTime+" seconds before retry...");

					getCounterController().sleep(_sleepTime * 1000);
//					try { Thread.sleep( _sleepTime * 1000 ); }
//					catch (InterruptedException ignore) {}

					// START AT THE TOP AGAIN
					continue;
				}
				
				// initialize Mon Table Dictionary
				mtd = MonTablesDictionaryManager.getInstance();
				if ( ! mtd.isInitialized() )
				{
					mtd.initialize(getCounterController().getMonConnection(), false);
//					CounterControllerAse.initExtraMonTablesDictionary(); // Now done inside: MonTablesDictionaryManager.getInstance().initialize(conn, true);
				}
//				System.out.println("aseServerName() = "+mtd.aseServerName);
//				System.out.println("aseVersionNum() = "+mtd.aseVersionNum);
//				System.out.println("aseVersionStr() = "+mtd.aseVersionStr);
//				System.out.println("aseSortId() = "+mtd.aseSortId);
//				System.out.println("aseSortName() = "+mtd.aseSortName);
				
//				// initialize ASE Config Dictionary
//				IDbmsConfig aseCfg = AseConfig.getInstance();
//				if ( ! aseCfg.isInitialized() )
//				{
//					aseCfg.initialize(getCounterController().getMonConnection(), false, false, null);
//				}
//
////				// initialize ASE Cache Config Dictionary
////				AseCacheConfig aseCacheCfg = AseCacheConfig.getInstance();
////				if ( ! aseCacheCfg.isInitialized() )
////				{
////					aseCacheCfg.initialize(getCounterController().getMonConnection(), false, false, null);
////				}
//
//				// initialize ASE Config Text Dictionary
//				AseConfigText.initializeAll(getCounterController().getMonConnection(), false, false, null);

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
			}

			// HOST Monitoring connection
			if ( ! getCounterController().isHostMonConnected())
			{
				if (_sshHostname != null && _sshUsername != null && _sshPassword != null)
				{
					_logger.info( "Connecting to SSH server using. user='"+_sshUsername+"', passwd='"+ "*hidden*" +"', port='"+_sshPort+"'. hostname='"+_sshHostname+"'");
	
					// get a connection
						try
						{
							SshConnection sshConn = new SshConnection(_sshHostname, _sshPort, _sshUsername, _sshPassword);
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

				// Do various other checks in the system, for instance in ASE do: checkForFullTransLogInMaster()
				getCounterController().checkServerSpecifics();

				// Get some Header information that will be used by the PersistContainer sub system
				PersistContainer.HeaderInfo headerInfo = getCounterController().createPcsHeaderInfo();
				if (headerInfo == null)
					continue;
				
//				PersistContainer pc = new PersistContainer(mainSampleTime, aseServerName, aseHostname);
				PersistContainer pc = new PersistContainer(headerInfo);
				if (startNewPcsSession)
					pc.setStartNewSample(true);
				startNewPcsSession = false;

				// add some statistics on the "main" sample level
				getCounterController().setStatisticsTime(headerInfo._mainSampleTime);


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
						cm.setServerName(      headerInfo._serverName);
						cm.setSampleTimeHead(  headerInfo._mainSampleTime);
						cm.setCounterClearTime(headerInfo._counterClearTime);

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

				
				// POST the container to the Persistent Counter Handler
				// That thread will store the information in any Storage.
				pch.add(pc);

			}
			catch (Throwable t)
			{
				_logger.error(Version.getAppName()+": error in GetCounters loop.", t);
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

			//-----------------------------
			// Do Java Garbage Collection?
			//-----------------------------
			boolean doJavaGcAfterRefresh = Configuration.getCombinedConfiguration().getBooleanProperty(MainFrame.PROPKEY_doJavaGcAfterRefresh, MainFrame.DEFAULT_doJavaGcAfterRefresh);
			if (doJavaGcAfterRefresh)
			{
				getCounterController().setWaitEvent("Doing Java Garbage Collection.");
				System.gc();
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

			// Sleep / wait for next sample
			if (_scriptWaitForNextSample != null)
				scriptWaitForNextSample();
			else
				getCounterController().sleep(sleepTime * 1000);

		} // END: while(_running)

		_logger.info("Thread '"+Thread.currentThread().getName()+"' ending...");

		// so lets stop the Persistent Counter Handler and it's services as well
		if (pch != null)
			pch.stop(true, 10*1000);
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