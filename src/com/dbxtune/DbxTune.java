/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune;

import java.awt.GraphicsEnvironment;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandler;

import com.dbxtune.alarm.AlarmHandler;
import com.dbxtune.alarm.UserDefinedAlarmHandler;
import com.dbxtune.alarm.writers.AlarmWriterToFile;
import com.dbxtune.central.controllers.DbxTuneGuiHttpConnectOfflineHandler;
import com.dbxtune.check.CheckForUpdates;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.config.dbms.DbmsConfigManager;
import com.dbxtune.config.dbms.IDbmsConfig;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.GuiLogAppender;
import com.dbxtune.gui.Log4jTableModel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.SplashWindow;
import com.dbxtune.gui.swing.EventQueueProxy;
import com.dbxtune.gui.swing.debug.EventDispatchThreadHangMonitor;
import com.dbxtune.mgt.NoGuiManagementServer;
import com.dbxtune.pcs.PersistWriterBase;
import com.dbxtune.pcs.PersistWriterJdbc;
import com.dbxtune.pcs.PersistentCounterHandler;
import com.dbxtune.pcs.inspection.IObjectLookupInspector;
import com.dbxtune.pcs.sqlcapture.ISqlCaptureBroker;
import com.dbxtune.sql.IDbmsVersionHelper;
import com.dbxtune.tools.tailw.LogTailWindow;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Debug;
import com.dbxtune.utils.Encrypter;
import com.dbxtune.utils.JavaUtils;
import com.dbxtune.utils.JavaVersion;
import com.dbxtune.utils.Logging;
import com.dbxtune.utils.Memory;
import com.dbxtune.utils.OpenSslAesUtil;
import com.dbxtune.utils.OpenSslAesUtil.DecryptionException;
import com.dbxtune.utils.ShutdownHandler;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingExceptionHandler;
import com.dbxtune.utils.SwingUtils;
import com.dbxtune.utils.TimeUtils;
import com.dbxtune.utils.Ver;

/**
 * NOT YET EVEN CLOSE TO BE FINNISHED
 * 
 * This is supposed to ba a base class that AseTune, IqTune, RsTune, HanaTune, MsSqlTune 
 * can build upon, and just implement a "main" method
 * 
 * @author Goran Schwarz
 */
public abstract class DbxTune
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static String _mainClassName = "unknown"; // this is set in main() by looking at the stack trace, by getting the last entry";

	public static final String DBXTUNE_NOGUI_INFO_CONFIG = "DBXTUNE_NOGUI_INFO_CONFIG";

	/** This can be either GUI or NO-GUI "collector" */
//	private static ICounterController _counterController = null;

	private static boolean _gui = true;

	public DbxTune(CommandLine cmd)
	throws Exception
	{
		// Set this early, some initialization blocks might use DbxTune.getInstance()
		_instance = this;

		// Kick it off
		init(cmd);
	}
	private static DbxTune _instance = null;
	public static DbxTune getInstance()
	{
		if (_instance == null)
			throw new RuntimeException("DbxTune is not yet initialized.");

		return _instance;
	}

	private static boolean _hasDevVersionExpired = false;
	public static boolean hasDevVersionExpired()
	{
		return _hasDevVersionExpired;
	}

	// Start time
	private static long _startTime = System.currentTimeMillis();
	public static long getStartTime()
	{
		return _startTime;
	}
	public static void setStartTime()
	{
		_startTime = System.currentTimeMillis();
	}

	/**
	 * What is the implementors application name.
	 * @return
	 */
	public static String getAppNameCmd()
	{
		return _mainClassName;
	}
	
	/**
	 * Strip of some part of a servername<br>
	 * For SqlServer (which has default port 1433), it will do the following<br>
	 * <pre>
	 * 192.168.0.1         -> 192-168-0-1 
	 * 192.168.0.1:1234    -> 192-168-0-1_1234
	 * 192.168.0.1:1433    -> 192-168-0-1
	 * gorans              -> gorans
	 * gorans:1234         -> gorans_1234
	 * gorans:1433         -> gorans
	 * gorans.xxx.com      -> gorans
	 * gorans.xxx.com:1234 -> gorans_1234
	 * gorans.xxx.com:1433 -> gorans
	 * </pre>
	 * Other default port numbers
	 * <pre>
	 * SqlServerTune : 1433
	 * PostgresTune  : 5432
	 * MySqlTune     : 3306
	 * OracleTune    : 1521
	 * Db2Tune       : 50000
	 * HanaTune      : 30015
	 * </pre>
	 * @param srvName
	 * @return
	 */
	public static String stripSrvName(String srvName)
	{
		if (srvName == null)
			return null;

		// Should we strip off the "last part" of any host name:  "host1.acme.com:1234" -> "host1:1234" or shoudl we keep it 
		if (srvName.indexOf('.') >= 0 || srvName.indexOf(':') >= 0)
		{
			String beforeColon = srvName;
			String afterColon  = "";
			int firstColon = srvName.indexOf(':');
			if (firstColon >= 0)
			{
				beforeColon = srvName.substring(0, firstColon);
				afterColon  = srvName.substring(firstColon + 1);
			}

			// get only the hostname: "host1.acme.com" -> "host1"
			int firstDot = srvName.indexOf('.');
			if (firstDot >= 0)
			{
				String validIpAddressRegex = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$";
				//String validHostnameRegex = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";

				if (beforeColon.matches(validIpAddressRegex))
				{
					// Replace all '.'
					beforeColon = beforeColon.replace('.', '-');
				}
				else
				{
					beforeColon = srvName.substring(0, firstDot);
				}
			}

			// If the portnumber is the default... strip it out
//			if ("AseTune"      .equalsIgnoreCase(Version.getAppName()) && "5000" .equals(afterColon)) afterColon = "";
			if ("SqlServerTune".equalsIgnoreCase(Version.getAppName()) && "1433" .equals(afterColon)) afterColon = "";
			if ("PostgresTune" .equalsIgnoreCase(Version.getAppName()) && "5432" .equals(afterColon)) afterColon = "";
			if ("MySqlTune"    .equalsIgnoreCase(Version.getAppName()) && "3306" .equals(afterColon)) afterColon = "";
			if ("OracleTune"   .equalsIgnoreCase(Version.getAppName()) && "1521" .equals(afterColon)) afterColon = "";
			if ("Db2Tune"      .equalsIgnoreCase(Version.getAppName()) && "50000".equals(afterColon)) afterColon = "";
			if ("HanaTune"     .equalsIgnoreCase(Version.getAppName()) && "30015".equals(afterColon)) afterColon = "";
			
			// Now set servername
			srvName = beforeColon;
			if (StringUtil.hasValue(afterColon))
				srvName = beforeColon + ":" + afterColon;
		}
		
		// Windows do not handle ':' characters in filenames that well
		if (srvName.indexOf(':') >= 0)
		{
//			// note: we do not yet have a log system, so use: System.out.println
//			System.out.println("Replacing characters in the LOG filename. ");
			
			srvName = srvName.replace(':', '_');
		}


		return srvName;
	}
//	public static void main(String[] args)
//	{
//		Version.setAppName("SqlServerTune");
//		
//		System.out.println("stripSrvName()='"+stripSrvName("192.168.0.1")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("192.168.0.1:1234")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("192.168.0.1:1433")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("gorans")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("gorans:1234")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("gorans:1433")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("gorans.xxx.com")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("gorans.xxx.com:1234")+"'");
//		System.out.println("stripSrvName()='"+stripSrvName("gorans.xxx.com:1433")+"'");
//	}

	/**
	 * What port number should the DbxTune GUI application use (for listening on local web calls/requests to connect to any db recording that is hosted on other server)
	 * @param appname
	 * @return
	 */
	public static int getGuiWebPort(String appname)
	{
		if ("AseTune"      .equalsIgnoreCase(appname)) return 6904;
		if ("IqTune"       .equalsIgnoreCase(appname)) return 6905;
		if ("RsTune"       .equalsIgnoreCase(appname)) return 6906;
		if ("RaxTune"      .equalsIgnoreCase(appname)) return 6907;
		if ("SqlServerTune".equalsIgnoreCase(appname)) return 6908;
		if ("PostgresTune" .equalsIgnoreCase(appname)) return 6909;
		if ("MySqlTune"    .equalsIgnoreCase(appname)) return 6910;
		if ("OracleTune"   .equalsIgnoreCase(appname)) return 6911;
		if ("Db2Tune"      .equalsIgnoreCase(appname)) return 6912;
		if ("HanaTune"     .equalsIgnoreCase(appname)) return 6913;

		return 6903;
	}

	
	public abstract String getAppName();
//	public abstract String getAppHomeEnvName();
//	public abstract String getAppSaveDirEnvName();
	public String getAppHomeEnvName()    { return "DBXTUNE_HOME"; }
	public String getAppSaveDirEnvName() { return "DBXTUNE_SAVE_DIR"; }


	public abstract String getConfigFileName();
	public abstract String getUserConfigFileName();
	public abstract String getSaveConfigFileName();
	
	public abstract int getSplashShreenSteps();

	public abstract String getSupportedProductName();
	public abstract MainFrame createGuiMainFrame();
	public abstract ICounterController createCounterController(boolean hasGui);
	public abstract IObjectLookupInspector createPcsObjectLookupInspector();
	public abstract ISqlCaptureBroker createPcsSqlCaptureBroker();

	public abstract CheckForUpdates createCheckForUpdates();
	public abstract IDbmsVersionHelper createDbmsVersionHelper();
	public abstract IDbmsConfig createDbmsConfig();
	public abstract MonTablesDictionary createMonTablesDictionary();

	public static boolean hasGui() 
	{
		if (GraphicsEnvironment.isHeadless()) 
			return false;
		
		return _gui; 
	} 
	/**
	 * Initialize and start 
	 * 
	 * @param cmd
	 * @throws Exception
	 */
	public void init(CommandLine cmd)
	throws Exception
	{
		Version.setAppName(getAppName());
		
//final org.h2.util.Profiler profiler = new org.h2.util.Profiler();
//profiler.startCollecting();

//		_cmdLine = cmd;

		// Create store dir if it did not exists.
		List<String> crAppDirLog = AppDir.checkCreateAppDir( null, System.out );


		// Are we in GUI mode or not
		_gui = true;
		if (cmd.hasOption("noGui"))
//		if ( ! hasGui() )
		{
			_gui = false;
			// Should we set this to headless... so that we throws "headless exception" if accessing GUI parts
			System.setProperty("java.awt.headless", "true");
		}
		Configuration.setGui(_gui);
//		System.setProperty(Configuration.HAS_GUI, Boolean.toString(_gui)); // used in AseConnectionFactory


		// -----------------------------------------------------------------
		// CHECK/SETUP information from the CommandLine switches
		// -----------------------------------------------------------------
//		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME",      "dbxtune.properties");
//		final String USER_CONFIG_FILE_NAME = System.getProperty("USER_CONFIG_FILE_NAME", "dbxtune.user.properties");
//		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  "iqtune.save.properties");
//		final String IQTUNE_HOME           = System.getProperty("IQTUNE_HOME");
		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME",      getConfigFileName());
		final String USER_CONFIG_FILE_NAME = System.getProperty("USER_CONFIG_FILE_NAME", getUserConfigFileName());
		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  getSaveConfigFileName());
		final String APP_HOME              = System.getProperty(getAppHomeEnvName());

		String defaultPropsFile     = (APP_HOME                != null) ? APP_HOME                + File.separator + CONFIG_FILE_NAME      : CONFIG_FILE_NAME;
		String defaultUserPropsFile = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() + File.separator + USER_CONFIG_FILE_NAME : USER_CONFIG_FILE_NAME;
		String defaultTmpPropsFile  = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() + File.separator + TMP_CONFIG_FILE_NAME  : TMP_CONFIG_FILE_NAME;
		String defaultTailPropsFile = LogTailWindow.getDefaultPropFile();

		// Compose MAIN CONFIG file (first USER_HOME then IQTUNE_HOME)
		String filename = AppDir.getAppStoreDir() + File.separator + CONFIG_FILE_NAME;
		if ( (new File(filename)).exists() )
			defaultPropsFile = filename;

		// Set default values for NO-GUI mode
		if ( ! _gui )
		{
			defaultUserPropsFile = null; // no file, just keep temp settings in-memory
			defaultTmpPropsFile  = null; // no file, just keep temp settings in-memory
			
		}

		String propFile        = cmd.getOptionValue("config",     defaultPropsFile);
		String userPropFile    = cmd.getOptionValue("userConfig", defaultUserPropsFile);
		String tmpPropFile     = cmd.getOptionValue("tmpConfig",  defaultTmpPropsFile);
		String tailPropFile    = cmd.getOptionValue("tailConfig", defaultTailPropsFile);
		String noGuiConfigFile = cmd.getOptionValue("noGui");

		// Check if the configuration file exists
		if ( ! (new File(propFile)).exists() )
			throw new FileNotFoundException("The configuration file '"+propFile+"' doesn't exists.");

		// -----------------------------------------------------------------
		// CHECK JAVA JVM VERSION
		// -----------------------------------------------------------------
		int javaVersionInt = JavaVersion.getVersion();
		if (   javaVersionInt != JavaVersion.VERSION_NOTFOUND
		    && javaVersionInt <  JavaVersion.VERSION_7
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime Java 7 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime Java 7 or higher.");
		}

		// The SAVE Properties for shared Tail
		Configuration tailSaveProps = new Configuration(tailPropFile);
		Configuration.setInstance(Configuration.TAIL_TEMP, tailSaveProps);

		// The SAVE Properties...
		Configuration appSaveProps = null;
		if (_gui)
		{
			appSaveProps = StringUtil.hasValue(tmpPropFile) ? new Configuration(tmpPropFile) : new Configuration();
			Configuration.setInstance(Configuration.USER_TEMP, appSaveProps);
		}
		else
		{
			appSaveProps = new Configuration();
			Configuration.setInstance(Configuration.USER_TEMP, appSaveProps);

			if (StringUtil.hasValue(tmpPropFile))
			{
				System.out.println("");
				System.out.println("===============================================================");
				System.out.println(" WARNING: you have specified cmdline option --tmpConfig "+tmpPropFile);
				System.out.println(" This will NOT be used in NU-GUI mode.");
				System.out.println(" A new 'USER_TEMP' object will be created on every startup.");
				System.out.println("---------------------------------------------------------------");
				System.out.println("");
			}
		}

		// Get the USER properties that could override CONF
		Configuration appUserProps = StringUtil.hasValue(userPropFile) ? new Configuration(userPropFile) : new Configuration();
		Configuration.setInstance(Configuration.USER_CONF, appUserProps);

		// Get the "OTHER" properties that has to do with LOGGING etc...
		Configuration appProps = new Configuration(propFile);
		Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);

		// The Store Properties...
		Configuration storeConfigProps = new Configuration();
		Configuration.setInstance(Configuration.PCS, storeConfigProps);

		// Set the Configuration search order when using the: Configuration.getCombinedConfiguration()
		if (_gui)
		{
			Configuration.setSearchOrder(
					Configuration.TAIL_TEMP,    // First
					Configuration.USER_TEMP,    // Second
					Configuration.USER_CONF,    // Third
					Configuration.SYSTEM_CONF); // Forth
		}
		else
		{
			Configuration.setSearchOrder(
					Configuration.USER_TEMP,    // First   (this must be first, otherwise "overrides" like 'timeout'->changeSqlBehaviour wont work)
					Configuration.PCS,          // Second
					Configuration.USER_CONF,    // Third
					Configuration.SYSTEM_CONF); // Forth
		}

		
		
		//-------------------------------
		// Set system properties
		//-------------------------------
		if (cmd.hasOption('D'))
		{
			Properties javaProps = cmd.getOptionProperties("D");

			for (String key : javaProps.stringPropertyNames())
			{
				String val = javaProps.getProperty(key);
				System.setProperty(key, val);

				boolean debug = true;
				if (debug)
					System.out.println("   SETTING SYSTEM PROPERTY: key=|"+key+"|, val=|"+val+"|.");
			}

		}
		
		// Take all Environment variables and add them as System Properties
		// But: Do NOT overwrite already set System Properties with Environment variables
		StringUtil.setEnvironmentVariablesToSystemProperties(false, false);
		
		//-------------------------------
		// LIST CM and exit
		//-------------------------------
		if (cmd.hasOption('l'))
		{
			// Create a dummy Counter Controller...
//			GetCountersNoOp counters = new GetCountersNoOp();
//			CounterController.setInstance(counters);
			CounterControllerNoOp counters = new CounterControllerNoOp(_gui);
			CounterController.setInstance(counters);
			counters.init();

			Map<String,String> shortToLongCmMap = CounterSetTemplates.getShortToLongMap();

			System.out.println();
			System.out.println("======================================================================");
			System.out.println("Here is a list of all available Performance Counters:");
			System.out.println("Short name           Long Name");
			System.out.println("-------------------- ------------------------------------------------------");
			for (Map.Entry<String,String> entry : shortToLongCmMap.entrySet())
			{
				String shortName = entry.getKey();
				String longName  = entry.getValue();

				System.out.println(StringUtil.left(shortName, 20) + " " + longName);
			}
			System.out.println();

			printTemplate("small");
			printTemplate("medium");
			printTemplate("large");
			printTemplate("all");

			// drop the dummy controller
			CounterController.setInstance(null);

			throw new NormalExitException("Done after -l switch.");
		}

		//-------------------------------
		// SHOW DEBUG switches and exit
		//-------------------------------
		DebugOptions.init();
		if (cmd.hasOption('x'))
		{
			String cmdLineDebug = cmd.getOptionValue('x');
			String[] sa = cmdLineDebug.split(",");
			for (int i=0; i<sa.length; i++)
			{
				String str = sa[i].trim();

				if (str.equalsIgnoreCase("list"))
				{
					System.out.println();
					System.out.println(" Option          Description");
					System.out.println(" --------------- -------------------------------------------------------------");
					for (Map.Entry<String,String> entry : Debug.getKnownDebugs().entrySet())
					{
						String debugOption = entry.getKey();
						String description = entry.getValue();

						System.out.println(" "+StringUtil.left(debugOption, 15, true) + " " + description);
					}
					System.out.println();
					// Get of of here if it was a list option
					throw new NormalExitException("List of debug options");
				}
				else
				{
					// add debug option
					Debug.addDebug(str);
				}
			}
		}


		
		
//		// Check if we are in NO-GUI mode
//		_gui = System.getProperty("dbxtune.gui", "true").trim().equalsIgnoreCase("true");

		// Show a SPLASH SCREEN
		if (_gui)
		{
			// How many steps do we have, NOTE: needs to be updated if you add steps
//			SplashWindow.init(false, GetCounters.NUMBER_OF_PERFORMANCE_COUNTERS, 1000);
			SplashWindow.init(false, getSplashShreenSteps(), 5000);
			SplashWindow.drawTopRight("Version: " + Version.getVersionStr());
		}
		else
		{
			SplashWindow.close();
		}



		// Both below is used later in the code (dbmsSrv* for NO-GUI to set the INFO file)
		String dbmsSrvName        = null;
		String dbmsSrvAliasName   = null;
		String dbmsSrvOrAliasName = null;
		String logFilename        = null; // in GUI mode it will be NULL & Logging.init() will assign a default name...
		
		// Check if we have a specific LOG filename before we initialize the logger...
		if (cmd.hasOption('L'))
		{
			String opt = cmd.getOptionValue('L');
			//check/fix opt Str
			logFilename = opt;
		}

		// If NO-GUI mode, try tof figgure out:
		//  - dbms server name or alias to use (for info and log file)
		//  - if 'logFilename' is only a directory... then add server/alias name as the logFilename
		if ( ! _gui )
		{
			// first: get servername from the propfile
			//String srvName = storeConfigProps.getProperty("conn.dbmsName");
			String tmpSrvName = storeConfigProps.getProperty("conn.dbmsName");

			// Override the name if we got the servernmae from the command line params
			if (cmd.hasOption('S'))
			{
				tmpSrvName = cmd.getOptionValue('S');

				// dbmsSrv*Name is used later
				dbmsSrvName        = tmpSrvName;
				dbmsSrvOrAliasName = tmpSrvName;
			}

			if (cmd.hasOption('A'))
			{
				tmpSrvName = cmd.getOptionValue('A');
				
				// dbmsSrv*Name is used later
				dbmsSrvAliasName   = tmpSrvName;
				dbmsSrvOrAliasName = tmpSrvName;
			}

			// Strip off "bad" characters from the server name (since it will be used in filenames etc)
			tmpSrvName = stripSrvName(tmpSrvName);

			// If we don't have a log file or log file directory...
			// Can we grab the "log directory" from ENVironment variable: DBXTUNE_CENTRAL_BASE, which normally is: ${HOME}/.dbxtune/dbxc
			if ( StringUtil.isNullOrBlank(logFilename) )
			{
				// Configuration also contains ENV variables
				String dbxCentralBase = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_CENTRAL_BASE");

				// If not found in getCombinedConfiguration, try another way...
				if (StringUtil.isNullOrBlank(dbxCentralBase)) 
					dbxCentralBase = StringUtil.getEnvVariableValue("DBXTUNE_CENTRAL_BASE");

				if (StringUtil.hasValue(dbxCentralBase))
				{
					logFilename = dbxCentralBase + File.separator + "log";
					System.out.println("INFO: No log file (or directory) was specified ('-L' or '--logfile'), but the environment variable 'DBXTUNE_CENTRAL_BASE' was found using that as a base. The log file directory will be '" + logFilename + "'.");
				}
			}
			
			// Investigate if logFilename is ONLY a DIRECTORY ... then add "srvName"
			if (StringUtil.hasValue(logFilename) && tmpSrvName != null)
			{
				File tmpFile = new File(logFilename);
				if (tmpFile.isDirectory())
				{
					logFilename += File.separator + tmpSrvName + ".log";

					// No log file is yet available, so we need to use 'System.out.println' to print stuff
					System.out.println("INFO: The logfile '" + tmpFile + "' specified by '-L' or '--logfile' is a directory, and no log file was specified. Constructing a log file using the switches '-S,--server' or '-A,--serverAlias' to fill in the server name. new logFilename is '" + logFilename + "'.");
				}
			}
			
			// NO Logfile was specified, maybe try to set the filename to AppName.nogui.dbmsSrvName.log
			if ( tmpSrvName != null && StringUtil.isNullOrBlank(logFilename) )
			{
				logFilename = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() : System.getProperty("user.home");
				if ( logFilename != null && ! (logFilename.endsWith("/") || logFilename.endsWith("\\")) )
					logFilename += File.separator;

				logFilename += "log" + File.separator;

				logFilename += Version.getAppName()+".nogui."+tmpSrvName+".log";
			}
		}



		// Setup HARDCODED, configuration for LOG4J, if not found in config file
		if (_gui)
			Logging.init(null, propFile, logFilename);
		else
			Logging.init("nogui.", propFile, logFilename);

		if (_gui && !SplashWindow.isOk())
			_logger.info("Splash screen could not be displayed.");

		SplashWindow.drawProgress("Initializing.");



		//--------------------------------------------------------------------
		// BEGIN: Set some SYSTEM properties
		// - Mainly for H2 database specific settings
		// - But any system setting could be set here
		//--------------------------------------------------------------------
		// H2 settings: http://code.google.com/p/h2database/source/browse/trunk/h2/src/main/org/h2/constant/SysProperties.java
		//--------------------------------------------------------------------
		System.setProperty("h2.lobInDatabase", "true");  // H2 database version 1.2.134, can store BLOBS inside the database using this system property

		//--------------------------------------------------------------------
		// Other setting below here
		//--------------------------------------------------------------------
		// END: Set some SYSTEM properties
		//--------------------------------------------------------------------

		// Check for System/localStored proxy settings
		String httpProxyHost = System.getProperty("http.proxyHost");
		String httpProxyPort = System.getProperty("http.proxyPort");

		if (httpProxyHost == null)
			httpProxyHost = appSaveProps.getProperty("http.proxyHost");
		if (httpProxyPort == null)
			httpProxyPort = appSaveProps.getProperty("http.proxyPort");

		if (httpProxyHost != null)
			System.setProperty("http.proxyHost", httpProxyHost);
		if (httpProxyPort != null)
			System.setProperty("http.proxyPort", httpProxyPort);

		_logger.debug("Using proxy settings: http.proxyHost='"+httpProxyHost+"', http.proxyPort='"+httpProxyPort+"'.");
		if (httpProxyHost != null)
			_logger.info("Using Java Properties for HTTP Proxy settings: http.proxyHost='"+httpProxyHost+"', http.proxyPort='"+httpProxyPort+"'.");

		SplashWindow.drawProgress("Initializing..");

		// Initialize this early...
		// If it's initialized after any connect attempt we might have
		// problems connecting to various destinations.
//		CheckForUpdates.init();
		// Create a thread that does this...
		// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
		// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
		Thread checkForUpdatesThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
//				CheckForUpdates.init();
				CheckForUpdates.setInstance( createCheckForUpdates() );
			}
		}, "checkForUpdatesThread");
//		checkForUpdatesThread.setDaemon(true);
//		checkForUpdatesThread.start();
		long cfuInitStartTime = System.currentTimeMillis();
		checkForUpdatesThread.run();
		_logger.info("Init of CheckForUpdate took '"+(System.currentTimeMillis()-cfuInitStartTime)+"' ms.");

		SplashWindow.drawProgress("Initializing...");
		if (_gui)
		{
			// Calling this would make GuiLogAppender, to register itself in log4j.
//			GuiLogAppender.getInstance();

			// How long should a ToolTip be displayed...
			// this is especially good for WaitEventID tooltip help, which could be much text to read.
//			ToolTipManager.sharedInstance().setDismissDelay(120*1000); // 2 minutes
		}
		// Always register the "gui" LogAppender, which will also send errors to 'dbxtune.com'
		// Calling this would make GuiLogAppender, to register itself in log4j.
		GuiLogAppender.getInstance();
		
		// if we are in NO-GUI mode, set some special things for the log appender
		if ( ! _gui )
		{
			Log4jTableModel tm = GuiLogAppender.getTableModel();
			if (tm != null)
			{
				tm.setMaxRecords(1000);
				tm.setNoGuiMode(true);
			}
		}

		// Check registered LOG4J appenders
		// NOTE: this should NOT be here for production
		//Enumeration en = Logger.getRootLogger().getAllAppenders();
		//while (en.hasMoreElements())
		//{
		//	Appender a = (Appender) en.nextElement();
		//	System.out.println("Appender="+a);
		//	System.out.println("Appender.getName="+a.getName());
		//}

		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		List<String> jvmStartArguments = runtimeMxBean.getInputArguments();
		_logger.info("JVM Command Line Argument List: " + jvmStartArguments);
		
		SplashWindow.drawProgress("Starting "+Version.getAppName()+", version "+Version.getVersionStr()+", build "+Version.getBuildStr());

		// Print out the memory configuration
		// And the JVM info
		_logger.info("Starting "+Version.getAppName()+", version "+Version.getVersionStr()+", build "+Version.getBuildStr());
		_logger.info("GUI mode "+_gui);
		_logger.info("Debug Options enabled: "+Debug.getDebugsString());

		_logger.info("Using Java Runtime Environment Version: "+System.getProperty("java.version"));
//		_logger.info("Using Java Runtime Environment Vendor: "+System.getProperty("java.vendor"));
//		_logger.info("Using Java Vendor URL: "+System.getProperty("java.vendor.url"));
//		_logger.info("Using Java VM Specification Version: "+System.getProperty("java.vm.specification.version"));
//		_logger.info("Using Java VM Specification Vendor:  "+System.getProperty("java.vm.specification.vendor"));
//		_logger.info("Using Java VM Specification Name:    "+System.getProperty("java.vm.specification.name"));
		_logger.info("Using Java VM Implementation  Version: "+System.getProperty("java.vm.version"));
		_logger.info("Using Java VM Implementation  Vendor:  "+System.getProperty("java.vm.vendor"));
		_logger.info("Using Java VM Implementation  Name:    "+System.getProperty("java.vm.name"));
		_logger.info("Using Java VM Home:    "+System.getProperty("java.home"));
		_logger.info("Java class format version number: " +System.getProperty("java.class.version"));
		_logger.info("Java class path: " +System.getProperty("java.class.path"));
		_logger.info("List of paths to search when loading libraries: " +System.getProperty("java.library.path"));
		_logger.info("Name of JIT compiler to use: " +System.getProperty("java.compiler"));
		_logger.info("Path of extension directory or directories: " +System.getProperty("java.ext.dirs"));

		_logger.info("Maximum memory is set to:  "+Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB. this could be changed with  -Xmx###m (where ### is number of MB)"); // jdk 1.4 or higher
		_logger.info("Total Physical Memory on this machine:  "+ Memory.getTotalPhysicalMemorySizeInMB() + " MB.");
		_logger.info("Free Physical Memory on this machine:  "+ Memory.getFreePhysicalMemorySizeInMB() + " MB.");
		_logger.info("Running on Operating System Name:  "+System.getProperty("os.name"));
		_logger.info("Running on Operating System Version:  "+System.getProperty("os.version"));
		_logger.info("Running on Operating System Architecture:  "+System.getProperty("os.arch"));
		_logger.info("The application was started by the username:  "+System.getProperty("user.name"));
		_logger.info("The application was started in the directory:   "+System.getProperty("user.dir"));
		_logger.info("The user '"+System.getProperty("user.name")+"' home directory:   "+System.getProperty("user.home"));

		_logger.info("System configuration file is '"+propFile+"'.");
		_logger.info("User configuration file is '"+userPropFile+"'.");
		_logger.info("Storing temporary configurations in file '"+tmpPropFile+"'.");
		_logger.info("Combined Configuration Search Order '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");
		_logger.info("Combined Configuration Search Order, With file names: "+StringUtil.toCommaStr(Configuration.getSearchOrder(true)));

		if (crAppDirLog != null && !crAppDirLog.isEmpty())
		{
			_logger.info("Below messages was created earlier by 'check/create application directory'.");
			for (String msg : crAppDirLog)
				_logger.info(msg);
		}

		// check if sufficient memory has been configured.
		// [FIXME] what is appropriate default value here.
		String needMemInMBStr = "32";
		int maxConfigMemInMB = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024); // jdk 1.4 or higher
		int needMemInMB = Integer.parseInt( appProps.getProperty("minMemoryLimitInMB", needMemInMBStr) );
		if (maxConfigMemInMB < needMemInMB)
		{
			String message = "I need at least "+needMemInMB+" MB to start this process. Maximum memory limit is now configured to "+maxConfigMemInMB+" MB. Specify this at the JVM startup using the -Xmx###m flag. ### is the upper limit (in MB) that this JVM could use.";
			_logger.error(message);
			throw new Exception(message);
		}

		// Put command line properties into the Configuration
		if ( _gui ) // GUI MODE
		{
			if (cmd.hasOption('U'))	appProps.setProperty("cmdLine.dbmsUsername", cmd.getOptionValue('U'));
			if (cmd.hasOption('P'))	appProps.setProperty("cmdLine.dbmsPassword", cmd.getOptionValue('P'), true);
			if (cmd.hasOption('S'))	appProps.setProperty("cmdLine.dbmsServer",   cmd.getOptionValue('S'));

			// Check servername
			if (cmd.hasOption('S'))
			{
				String cmdLineServer = cmd.getOptionValue('S');
				if (cmdLineServer.indexOf(":") >= 0)
				{
					if ( ! AseConnectionFactory.isHostPortStrValid(cmdLineServer) )
						throw new Exception("Problems with command line parameter -S"+cmdLineServer+"; "+AseConnectionFactory.isHostPortStrValidReason(cmdLineServer));
				}
				else
				{
					if (AseConnectionFactory.resolvInterfaceEntry(cmdLineServer) == null)
						throw new Exception("Server '"+cmdLineServer+"' is not found in the file '"+AseConnectionFactory.getIFileName()+"'.");
				}
			}

			if (cmd.hasOption('u'))	appProps.setProperty("cmdLine.sshUsername", cmd.getOptionValue('u'));
			if (cmd.hasOption('p'))	appProps.setProperty("cmdLine.sshPassword", cmd.getOptionValue('p'), true);
			if (cmd.hasOption('s'))
			{
				appProps.setProperty("cmdLine.sshHostname", cmd.getOptionValue('s'));
				appProps.setProperty("cmdLine.sshPort", 22);

				String cmdLineHostname = cmd.getOptionValue('s');
				if (cmdLineHostname.indexOf(":") >= 0)
				{
					String[] sa = cmdLineHostname.split(":");
					appProps.setProperty("cmdLine.sshHostname", sa[0]);
					appProps.setProperty("cmdLine.sshPort",     Integer.parseInt(sa[1]));
				}
			}
		}
		else // NO-GUI MODE
		{
			// Check if the configuration file exists
			// or if it's appropriate options...
			if ( (new File(noGuiConfigFile)).exists() )
			{
				storeConfigProps.load(noGuiConfigFile);
			}
			else
			{
//				if (GetCountersNoGui.checkValidCmShortcuts(noGuiConfigFile))
				if (CounterCollectorThreadNoGui.checkValidCmShortcuts(noGuiConfigFile))
					storeConfigProps.setProperty("cmdLine.cmOptions", noGuiConfigFile);
				else
					throw new FileNotFoundException("The noGuiConfig file '"+noGuiConfigFile+"' doesn't exists.");
			}

			//-------------------------------------------------
			// ASE CONNECTION
			if (cmd.hasOption('U'))	storeConfigProps.setProperty("conn.dbmsUsername",    cmd.getOptionValue('U'));
			if (cmd.hasOption('P'))	storeConfigProps.setProperty("conn.dbmsPassword",    cmd.getOptionValue('P'), true);
			if (cmd.hasOption('S'))	storeConfigProps.setProperty("conn.dbmsName",        cmd.getOptionValue('S'));
			if (cmd.hasOption('A'))	storeConfigProps.setProperty("conn.dbmsServerAlias", cmd.getOptionValue('A'));
			if (cmd.hasOption('N'))	storeConfigProps.setProperty("conn.dbmsDisplayName", cmd.getOptionValue('N'));
			if (cmd.hasOption('O'))	storeConfigProps.setProperty("conn.jdbcUrlOptions",  cmd.getOptionValue('O'));

			// Check servername
			boolean plainAseServerName = false;
			String dbmsCmdLineSwitchHostname = null;
			if (cmd.hasOption('S'))
			{
				String cmdLineServer = cmd.getOptionValue('S');

				// Only SYBASE/TDS srvTypes is for the moment validated
				if (    "AseTune".equalsIgnoreCase(Version.getAppName()) 
				     || "IqTune" .equalsIgnoreCase(Version.getAppName())
				     || "RsTune" .equalsIgnoreCase(Version.getAppName())
				     || "RaxTune".equalsIgnoreCase(Version.getAppName())
				   )
				{
					if (cmdLineServer.indexOf(":") != -1) // HAS ":" is cmdLineServer 
					{
						if ( ! AseConnectionFactory.isHostPortStrValid(cmdLineServer) )
							throw new Exception("Problems with command line parameter -S"+cmdLineServer+"; "+AseConnectionFactory.isHostPortStrValidReason(cmdLineServer));
						
						dbmsCmdLineSwitchHostname = cmdLineServer.substring(0, cmdLineServer.indexOf(":"));

						storeConfigProps.setProperty("conn.dbmsHostPort", cmdLineServer);
					}
					else
					{
						if (AseConnectionFactory.resolvInterfaceEntry(cmdLineServer) == null)
							throw new Exception("Server '"+cmdLineServer+"' is not found in the file '"+AseConnectionFactory.getIFileName()+"'.");

						String hostPort = AseConnectionFactory.getIHostPortStr(cmdLineServer);
//						appProps.setProperty("cmdLine.aseServer",   hostPort);
//						storeConfigProps.setProperty("conn.dbmsName",   hostPort);
						storeConfigProps.setProperty("conn.dbmsHostPort",   hostPort);
						_logger.info("Resolved the Command Line Switch -S '"+cmdLineServer+"'. To host:port '"+hostPort+"'.");
						
						plainAseServerName = true;
					}
				}
				else if ("XxxTune".equalsIgnoreCase(Version.getAppName()))
				{
				}
				else // "unhandled" server types
				{
//					JdbcUrlParser jdbcUrlParser = JdbcUrlParser.parse(cmdLineServer);
//System.out.println("jdbcUrlParser='"+jdbcUrlParser+"'");
//					
//					dbmsCmdLineSwitchHostname = jdbcUrlParser.getHost();
//					storeConfigProps.setProperty("conn.dbmsHostPort", jdbcUrlParser.getHostPortStr());
					
					dbmsCmdLineSwitchHostname = cmdLineServer;
					storeConfigProps.setProperty("conn.dbmsHostPort", cmdLineServer);

					if (cmdLineServer.indexOf(":") != -1) // HAS ":" is cmdLineServer 
					{
						dbmsCmdLineSwitchHostname = cmdLineServer.substring(0, cmdLineServer.indexOf(":"));
						storeConfigProps.setProperty("conn.dbmsHostPort", cmdLineServer);
					}
				}
			}

//System.out.println("####################### DBMS");
//System.out.println("DbxTune: storeConfigProps.getProperty(conn.dbmsUsername) = " + storeConfigProps.getProperty("conn.dbmsUsername"));
//System.out.println("DbxTune: storeConfigProps.getProperty(conn.dbmsPassword) = " + storeConfigProps.getProperty("conn.dbmsPassword"));
//System.out.println("DbxTune: storeConfigProps.getProperty(conn.dbmsName)     = " + storeConfigProps.getProperty("conn.dbmsName"));
//System.out.println("---> DbxTune: storeConfigProps.hasProperty(conn.dbmsPassword) = " + storeConfigProps.hasProperty("conn.dbmsPassword"));
			// If no password, try to grab a password from the file '~/.passwd.enc'
			if ( ! storeConfigProps.hasProperty("conn.dbmsPassword") )
			{
				String defaultNullValue = null;
//				String aseUser   = cmd.getOptionValue('U', "sa");
				String aseUser   = storeConfigProps.getProperty("conn.dbmsUsername", "sa");
				String aseServer = plainAseServerName ? cmd.getOptionValue('S', defaultNullValue) : dbmsCmdLineSwitchHostname;

				// if we have an alias/alternate server name
				if (cmd.hasOption('A'))
					aseServer = cmd.getOptionValue('A');

//FIXME; rewrite a bunch of this... this is to "r�rigt" and it's also a bit of duplicate in CounterCollectorThreadNoGui

				try
				{
					//_logger.info("Reading password for DBMS server name '" + aseServer + "' from file '" + OpenSslAesUtil.getPasswordFilename() + "'.");

					// Note: generate a passwd in linux: echo 'thePasswd' | openssl enc -aes-128-cbc -a -salt -pass:sybase
					String asePasswd = OpenSslAesUtil.readPasswdFromFile(aseUser, aseServer);
					
					if (asePasswd != null)
					{
						_logger.info("No DBMS password was specified. But the password '******', for user '"+aseUser+"', DBMS Server '"+aseServer+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

						if (_logger.isDebugEnabled())
							_logger.info("No DBMS password was specified. But the password '"+asePasswd+"', for user '"+aseUser+"', DBMS Server '"+aseServer+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

						if (System.getProperty("nogui.password.print", "false").equalsIgnoreCase("true"))
							System.out.println("#### DEBUG ####: No DBMS password was specified. But the password '"+asePasswd+"', for user '"+aseUser+"', DBMS Server '"+aseServer+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

						storeConfigProps.setProperty("conn.dbmsPassword", asePasswd, true); // should we encrypt the passwd or not
					}
					else
						_logger.warn("No DBMS password was specified. and NO entry, for user '"+aseUser+"', DBMS Server '"+aseServer+"' was found in the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");
				}
				catch(DecryptionException ex)
				{
					_logger.warn("Problems decrypting the password, for user '"+aseUser+"', DBMS Server '"+aseServer+"'. Probably a bad passphrase for the encrypted passwd. Caught: "+ex);
				}
				catch(FileNotFoundException ex)
				{
					_logger.warn("The password file '"+OpenSslAesUtil.getPasswordFilename()+"' didn't exists.");
				}
				catch(IOException ex)
				{
					_logger.error("Problems reading the password file "+OpenSslAesUtil.getPasswordFilename()+"'. Caught: "+ex);
				}
			}
			

			//-------------------------------------------------
			// HOST MONITORING
			if (cmd.hasOption("localHostMon"))	         storeConfigProps.setProperty("conn.hostmon.local", "true");
			if (cmd.hasOption("localHostMonWrapperCmd")) storeConfigProps.setProperty("conn.hostmon.local.wrapper.cmd", cmd.getOptionValue("localHostMonWrapperCmd"));

			//-------------------------------------------------
			// SSH CONNECTION
			if (cmd.hasOption('u'))	storeConfigProps.setProperty("conn.sshUsername", cmd.getOptionValue('u'));
			if (cmd.hasOption('p'))	storeConfigProps.setProperty("conn.sshPassword", cmd.getOptionValue('p'), true);
			if (cmd.hasOption('k'))	storeConfigProps.setProperty("conn.sshKeyFile",  cmd.getOptionValue('k'));
			if (cmd.hasOption('s'))
			{
				storeConfigProps.setProperty("conn.sshHostname", cmd.getOptionValue('s'));
				storeConfigProps.setProperty("conn.sshPort", 22);

				String cmdLineHostname = cmd.getOptionValue('s');
				if (cmdLineHostname.indexOf(":") >= 0)
				{
					String[] sa = cmdLineHostname.split(":");
					storeConfigProps.setProperty("conn.sshHostname", sa[0]);
					storeConfigProps.setProperty("conn.sshPort",     Integer.parseInt(sa[1]));
				}
			}
			// If we havn't got any SSH-Server (property or switch), then try to get it from the DBMS specification
			if ( ! storeConfigProps.hasProperty("conn.sshHostname") )
			{
				// only when we have a SSH-User name... otherwise we wont do SSH Connect
				if (storeConfigProps.hasProperty("conn.sshUsername"))
				{
					String dbmsHostPort = storeConfigProps.getProperty("conn.dbmsHostPort", null);
					if (StringUtil.hasValue(dbmsHostPort))
					{
						String sa[] = dbmsHostPort.split(":");
						String sshHostname = sa[0];
						storeConfigProps.setProperty("conn.sshHostname", sshHostname);
						
						_logger.info("No SSH Hostname was specified. Resolving this from the DBMS connection to be '"+sshHostname+"'. If this is NOT Correct, please specify it with the -s switch or the property 'conn.sshHostname'.");
					}
				}
			}

//System.out.println("####################### SSH");
//System.out.println("DbxTune: storeConfigProps.getProperty(conn.sshUsername) = " + storeConfigProps.getProperty("conn.sshUsername"));
//System.out.println("DbxTune: storeConfigProps.getProperty(conn.sshPassword) = " + storeConfigProps.getProperty("conn.sshPassword"));
//System.out.println("DbxTune: storeConfigProps.getProperty(conn.sshHostname) = " + storeConfigProps.getProperty("conn.sshHostname"));
//System.out.println("---> DbxTune: storeConfigProps.hasProperty(conn.sshPassword) = " + storeConfigProps.hasProperty("conn.sshPassword"));
			// If no password, try to grab a password from the file '~/.passwd.enc'
			if ( ! storeConfigProps.hasProperty("conn.sshPassword"))
			{
				try 
				{
					String sshUser   = storeConfigProps.getProperty("conn.sshUsername", "sybase");
					String sshServer = storeConfigProps.getProperty("conn.sshHostname", null);
					
					String sshUserName      = sshUser;
					String sshUserLookupKey = sshUser;
					if (sshUser.contains(":"))
					{
						sshUserName      = StringUtils.substringBefore(sshUser, ":");
						sshUserLookupKey = StringUtils.substringAfter (sshUser, ":");
						
						storeConfigProps.setProperty("conn.sshUsername", sshUserName);
						storeConfigProps.setProperty("conn.sshUsername.lookupKey", sshUserLookupKey);
						
						_logger.info("SSH User name contained 'key-lookup' method. sshUserName='" + sshUserName + "', sshUserLookupKey='" + sshUserLookupKey + "'.");
					}

					// Note: generate a passwd in linux:  echo 'thePasswd' | openssl enc -aes-128-cbc -a -salt -pass:sybase
					// Note: generate in RHEL 8 or above: echo 'thePasswd' | openssl enc -base64 -aes-256-cbc -pbkdf2 -iter 100000 -k sybase
					String sshPasswd = OpenSslAesUtil.readPasswdFromFile(sshUserLookupKey, sshServer);
					
					if (sshPasswd != null)
					{
						_logger.info("No SSH password was specified. But the password '******', for user '"+sshUser+"', SSH Server '"+sshServer+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

						if (_logger.isDebugEnabled())
							_logger.debug("No SSH password was specified. But the password '"+sshPasswd+"', for user '"+sshUser+"', SSH Server '"+sshServer+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

						if (System.getProperty("nogui.password.print", "false").equalsIgnoreCase("true"))
							System.out.println("#### DEBUG ####: No SSH password was specified. But the password '"+sshPasswd+"', for user '"+sshUser+"', SSH Server '"+sshServer+"' was grabbed from the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");

						storeConfigProps.setProperty("conn.sshPassword", sshPasswd, true); // should we encrypt the passwd or not
					}
					else
						_logger.info("No SSH password was specified and NO entry, for user '"+sshUser+"', SSH Server '"+sshServer+"' was found in the file '"+OpenSslAesUtil.getPasswordFilename()+"'.");
				}
				catch(FileNotFoundException ex)
				{
					_logger.info("The password file '"+OpenSslAesUtil.getPasswordFilename()+"' didn't exists.");
				}
				catch(IOException ex)
				{
					_logger.error("Problems reading the password file "+OpenSslAesUtil.getPasswordFilename()+"'. Caught: "+ex);
				}
			}


			//-------------------------------------------------
			// Other flags

			// -r, --reconfigure: Offline: if monitored ASE is not properly configured, try to configure it.
			if (cmd.hasOption('r'))
			{
				storeConfigProps.setProperty("offline.configuration.fix", true);
			}

			// -i, --interval: Offline: time between samples.
			if (cmd.hasOption('i'))
			{
				storeConfigProps.setProperty("offline.sampleTime", cmd.getOptionValue('i'));
			}

			// -f, --finish: Offline: shutdown/stop the no-gui service after # hours.
			if (cmd.hasOption('f'))
			{
				String recordingStopTime = cmd.getOptionValue('e');
				try {
					PersistWriterBase.getRecordingStopTime(null,recordingStopTime);
				} catch (Exception e) {
					throw new Exception("Switch '-f|--finish' "+e.getMessage());
				}
				storeConfigProps.setProperty("offline.shutdownAfterXHours", cmd.getOptionValue('f'));
			}

			// -e, --enable: Offline: enable/start the recording at a specific time
			if (cmd.hasOption('e'))
			{
				String recordingStartTime = cmd.getOptionValue('e');
				try {
					PersistWriterBase.getRecordingStartTime(recordingStartTime);
				} catch (Exception e) {
					throw new Exception("Switch '-e|--enable' "+e.getMessage());
				}
				storeConfigProps.setProperty(CounterController.PROPKEY_startRecordingAtTime, recordingStartTime);
			}


			// -T --dbtype: Offline: Type of database storage H2 or ASE or ASA
			// values for -T is handled within the -d section
			if (cmd.hasOption('T'))
			{
				if ( ! cmd.hasOption('d') )
				{
					throw new Exception("If you use switch '-T|--dbtype' you must also use switch '-d|--dbname'.");
				}
			}

			// -d --dbname: Offline: dbname to store offline samples.
			if (cmd.hasOption('d'))
			{
				String opt = cmd.getOptionValue('d');
				_logger.info("Command Line Option '-d|--dbname' was specified, dbname to use is '"+opt+"'.");

				String opt_D = "H2";
				if (cmd.hasOption('T'))
				{
					opt_D = cmd.getOptionValue('T').toUpperCase();
					_logger.info("Command Line Option '-T|--dbtype' was specified, type to use is '"+opt_D+"'.");
				}
				else
				{
					opt_D = "H2";
					_logger.info("Command Line Option '-T|--dbtype' was NOT specified, using the default 'H2'.");
				}

				if (opt_D.equals("H2"))
				{
					String envNameSaveDir    = getAppSaveDirEnvName();  // DBXTUNE_SAVE_DIR

					String jdbcDriver = "org.h2.Driver";
					String jdbcUrl    = "jdbc:h2:file:"+opt;
					String jdbcUser   = "sa";
					String jdbcPasswd = "";

					if ("default".equalsIgnoreCase(opt))
						jdbcUrl = "jdbc:h2:file:${"+envNameSaveDir+"}/${SERVERNAME}_${DATE}";

					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcDriver,           jdbcDriver);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUrl,              jdbcUrl);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUsername,         jdbcUser);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcPassword,         jdbcPasswd, true);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_startH2NetworkServer, true);

					storeConfigProps.setProperty(PersistentCounterHandler.PROPKEY_WriterClass, "com.dbxtune.pcs.PersistWriterJdbc");

					_logger.info("PCS: using jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"', jdbcUser='"+jdbcUser+"', jdbcPasswd='*secret*', startH2NetworkServer=true.");
				}
				else if (opt_D.equals("ASE"))
				{
					String[] strArr = opt.split(":");
					if (strArr.length != 5)
						throw new Exception("Wrong format of '-d|-dbname' for type 'ASE', this should look like 'hostname:port:dbname:user:passwd' ");
					String aseHost   = strArr[0];
					String asePort   = strArr[1];
					String aseDbname = strArr[2];
					String aseUser   = strArr[3];
					String asePasswd = strArr[4];

					if (asePasswd.trim().equalsIgnoreCase("null"))
						asePasswd = "";

					String urlOptions =
						"?APPLICATIONNAME="+Version.getAppName()+"-Writer" +
						"&HOSTNAME=" + Version.VERSION_STRING +
						"&DYNAMIC_PREPARE=true" +
						"&SQLINITSTRING=set statement_cache off" +
					//	"&ENABLE_BULK_LOAD=true" +
						"";

					String jdbcDriver = AseConnectionFactory.getDriver();
					String jdbcUrl    = "jdbc:sybase:Tds:"+aseHost+":"+asePort+"/"+aseDbname+urlOptions;
					String jdbcUser   = aseUser;
					String jdbcPasswd = asePasswd;

					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcDriver,   jdbcDriver);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUrl,      jdbcUrl);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUsername, jdbcUser);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcPassword, jdbcPasswd, true);

					storeConfigProps.setProperty(PersistentCounterHandler.PROPKEY_WriterClass, "com.dbxtune.pcs.PersistWriterJdbc");

					_logger.info("PCS: using jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"', jdbcUser='"+jdbcUser+"', jdbcPasswd='*secret*', dbname='"+aseDbname+"'.");
				}
				else if (opt_D.equals("ASA"))
				{
					String[] strArr = opt.split(":");
//					if (strArr.length != 5)
//						throw new Exception("Wrong format of '-d|-dbname' for type 'ASE', this should look like 'hostname:port:dbname:user:passwd' ");
					String asaHost   = (strArr.length >= 0+1) ? strArr[0] : "localhost";
					String asaPort   = (strArr.length >= 1+1) ? strArr[1] : "2638";
					String asaDbname = (strArr.length >= 2+1) ? strArr[2] : "";
					String asaUser   = (strArr.length >= 3+1) ? strArr[3] : "dba";
					String asaPasswd = (strArr.length >= 4+1) ? strArr[4] : "sql";

					if (asaPasswd.trim().equalsIgnoreCase("null"))
						asaPasswd = "";

					if ( ! asaDbname.equals("") )
						asaDbname = "/" + asaDbname;


					String jdbcDriver = AseConnectionFactory.getDriver();
					String jdbcUrl    = "jdbc:sybase:Tds:"+asaHost+":"+asaPort+asaDbname;
					String jdbcUser   = asaUser;
					String jdbcPasswd = asaPasswd;

					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcDriver,   jdbcDriver);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUrl,      jdbcUrl);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcUsername, jdbcUser);
					storeConfigProps.setProperty(PersistWriterJdbc.PROPKEY_jdbcPassword, jdbcPasswd, true);

					storeConfigProps.setProperty(PersistentCounterHandler.PROPKEY_WriterClass, "com.dbxtune.pcs.PersistWriterJdbc");

					_logger.info("PCS: using jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"', jdbcUser='"+jdbcUser+"', jdbcPasswd='*secret*', dbname='"+asaDbname+"'.");
				}
				else
				{
					throw new Exception("Unknown -T,--dbtype value of '"+opt+"' was specified, known values 'H2|ASE|ASA'.");
				}
			}
		}


		//-------------------------------------------------------------------------
		// HARDCODE a STOP date when this "DEVELOPMENT VERSION" will STOP working
		//-------------------------------------------------------------------------
		boolean enforceOldDbxTuneVersionsCheck = System.getProperty("_ENFORCE_OLD_DBXTUNE_VERSIONS_CHECK_", "true").equalsIgnoreCase("true");
		if (Version.IS_DEVELOPMENT_VERSION && enforceOldDbxTuneVersionsCheck)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

			_logger.info("This DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', then you will have to download a later version..");
			if ( System.currentTimeMillis() > Version.DEV_VERSION_EXPIRE_DATE.getTime() )
			{
				_hasDevVersionExpired = true;

				String msg = "This DEVELOPMENT VERSION has expired. (version='"+Version.getVersionStr()+"', buildStr='"+Version.getBuildStr()+"'). The \"time to live\" period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', and the current date is '"+df.format(new Date())+"'. A new version can be downloaded here 'http://www.dbxtune.com'";
				_logger.error(msg);
				Exception ex = new Exception(msg);
				if (_gui)
				{
					try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
					catch (Exception e) { _logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e); }

					SwingUtils.showErrorMessage(Version.getAppName()+" - This DEVELOPMENT VERSION has expired",
						"<html>" +
							"<h2>This DEVELOPMENT VERSION has expired.</h2>" +
							"Current Version is '"+Version.getVersionStr()+"', with the build string '"+Version.getBuildStr()+"'.<br>" +
							"<br>" +
							"The <i>time to live</i> period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'. <br>" +
							"And the current date is '"+df.format(new Date())+"'.<br>" +
							"<br>" +
							"A new version can be downloaded at <A HREF=\"http://www.dbxtune.com\">http://www.dbxtune.com</A><br>" +
							"<br>" +
							Version.getAppName() + " will still be started, but in <b>restricted mode</b><br>" +
							"It can only read data from <i>Persistent Counter Storage</i>, also called <i>'offline databases'</i>.<br>" +
							"<br>" +
							"But I strongly encourage you to download the latest release!<br>" +
							"<br>" +
							"This 'start' option was implemented as a fail-safe mechanism, and " +
							"should only be used if the new "+Version.getAppName()+" version is not capable of reading the " +
							"old database. In 99.9% the new "+Version.getAppName()+" version will be able to read " +
							"Performance Counters stored by a earlier "+Version.getAppName()+" release." +
						"</html>",
						ex);
				}
				else
				{
					// NO-GUI mode is NOT allowed to start if it's a OLD DEVELOPMENT Version
					throw ex;
				}
			}
		}

boolean startEvenIfGui_justToTestTheService = "gorans2".equals(StringUtil.getHostname());
if (_gui && startEvenIfGui_justToTestTheService)
{
	(new Exception("REMOVE THIS, we should NOT start a Management Server in GUI Mode...")).printStackTrace();

	NoGuiManagementServer noGuiMngmntSrv = new NoGuiManagementServer();
	NoGuiManagementServer.setInstance(noGuiMngmntSrv);
	noGuiMngmntSrv.startServer();
}
		// 
		// Create a file, that will be deleted when the process ends.
		// This file will hold various configuration about the NO-GUI process
		// This is used by DBXTUNE Central...
		//
		if ( ! _gui )
		{
			// Start a HTTP Server so DbxTune Central can "talk" to the NOGUI instance
			NoGuiManagementServer noGuiMngmntSrv = new NoGuiManagementServer();
			NoGuiManagementServer.setInstance(noGuiMngmntSrv);
			noGuiMngmntSrv.startServer();

			// Write a "Service Info File" -- Can be used by DbxCentral lookup stuff 
			boolean writeDbxTuneServiceFile = true;
			if (writeDbxTuneServiceFile)
			{
				// file location: ${HOME}/.dbxtune/info/${dbmsSrvName}.dbxtune
//				String noGuiServiceInfoFile = AppDir.getAppStoreDir() + File.separator + "info" + File.separator + dbmsSrvName + ".dbxtune";
				String noGuiServiceInfoFile = AppDir.getAppStoreDir() + File.separator + "info" + File.separator + dbmsSrvOrAliasName + ".dbxtune";
				File f = new File(noGuiServiceInfoFile);

				_logger.info("Creating DbxTune - NOGUI Service information file '" + f.getAbsolutePath() + "'.");

				// Create the directory structure (if it's not there)
				if (f.getParentFile() != null)
					f.getParentFile().mkdirs();

				// Create the file
				f.createNewFile();
				
				// Mark it as to be removed when the JVM ends.
				f.deleteOnExit();


				// Create the configuration
				Configuration conf = new Configuration(noGuiServiceInfoFile);
				Configuration.setInstance(DBXTUNE_NOGUI_INFO_CONFIG, conf);
				
				// Set some configuartion in the file
				conf.setProperty("dbxtune.app.name",            Version.getAppName());
				conf.setProperty("dbxtune.startTime",           new Timestamp(System.currentTimeMillis())+"" );
				conf.setProperty("dbxtune.pid",                 JavaUtils.getProcessId("-1"));
				conf.setProperty("dbxtune.log.file",            logFilename);
				conf.setProperty("dbxtune.config.file",         noGuiConfigFile);
				conf.setProperty("dbxtune.dbms.srvName",        dbmsSrvName);
				conf.setProperty("dbxtune.dbms.srvAliasName",   dbmsSrvAliasName == null ? "" : dbmsSrvAliasName); // on null: set it to ""
				conf.setProperty("dbxtune.dbms.srvOrAliasName", dbmsSrvOrAliasName);
				conf.setProperty("dbxtune.refresh.rate",        storeConfigProps.getProperty("offline.sampleTime", ""));
				
				conf.setProperty("dbxtune.management.host",     noGuiMngmntSrv.getListenerHost());
				conf.setProperty("dbxtune.management.port",     noGuiMngmntSrv.getPort());
				conf.setProperty("dbxtune.management.info",     noGuiMngmntSrv.getExInfo());

				// AlarmWriteToFile ACTIVE/LOG properties (so the DBXCENTRAL OverviewServlet can pick it up)
				String wtoFileActiveFilename = Configuration.getCombinedConfiguration().getPropertyRaw(AlarmWriterToFile.PROPKEY_activeFilename);
				String wtoFileLogFilename    = Configuration.getCombinedConfiguration().getPropertyRaw(AlarmWriterToFile.PROPKEY_logFilename);
				if (StringUtil.hasValue(wtoFileActiveFilename))	conf.setProperty(AlarmWriterToFile.PROPKEY_activeFilename, wtoFileActiveFilename);
				if (StringUtil.hasValue(wtoFileActiveFilename))	conf.setProperty(AlarmWriterToFile.PROPKEY_logFilename,    wtoFileLogFilename);
				
				// Save it; with override
				conf.save(true);
				
				// Later of, for instance in the PCS JDBC Writer will write information on where a recording is stored etc.

			} // end: writeDbxTuneServiceFile

		}
		else
		{
			// Start a HTTP Server (on the client host) so DbxTune Central web content - can "talk" to the local GUI instance
			// This can be used for example to issue a "connect to - recorded database" that is managed by 'DbxTune Central' 
			boolean startHttpSrv = true;
//			boolean startHttpSrv = false;
			if (startHttpSrv)
			{
				// TODO: implement this
				
				// The port number must be "fixed", so if you have started several dbxtune GUI clients on your PC, it's only the "first" that will listen on the port
				// when a command is received on the port, a popup must be displayed - so that the user can accept or decline the command issued on the web page
				
				// How this will work.
				// - you connect to the DbxTune Central server (a web server), That will present high level info...
				//   If you want to drill down, you need to start dbxTune GUI (which will listen on port 9999) 9999 is just an example
				// - DbxTune Central will present a HTML "link" that points to URL "http://localhost:9999/some-command/param"
				//   When you click the link, the web browser will try to access that URL (which is your DbxTune GUI instance)
				//   The GUI instance can then "do" what the web browser instructed it to do (for instance: connect to an offline recording on url 'jdbc:h2:tcp://dbxtune-central.acme.com:19092/PROD_ASE_2017-11-09'
				//   The GUI should show a popup so the user can accept/decline the command issued by the web browser
				
				// http://www.eclipse.org/jetty/documentation/9.4.x/embedding-jetty.html
				
				// Or we may do this by "drag and drop" --- But lets explore this...
				
				try
				{
					int port = Configuration.getCombinedConfiguration().getIntProperty("DbxTune.gui.http.listener.port", getGuiWebPort(Version.getAppName()));
					Server server = new Server();

					ServerConnector http = new ServerConnector(server);
			        http.setHost("127.0.0.1"); // localhost
			        http.setPort(port);
			        http.setIdleTimeout(30000);

			        // Set the connector
			        server.addConnector(http);
					
					// Add a single handler on context "/connect-offline"
					ContextHandler context = new ContextHandler();
					context.setContextPath( "/connect-offline" );
					context.setHandler( new DbxTuneGuiHttpConnectOfflineHandler() );

					server.setHandler( context );

					_logger.info("Starting local Web server at port "+port+".");
					server.start();
				}
				catch(Exception ex)
				{
					_logger.warn ("Starting local Web server faild, but I will continue anyway...");
					_logger.debug("Starting local Web server faild, but I will continue anyway...", ex);
				}
			}
		}


		
		
		// Create a DBMS Configuration object, note: createDbmsConfig() might return null, then the DBMS Config isn't supported...
		IDbmsConfig dbmsConfig = createDbmsConfig();
		DbmsConfigManager.setInstance(dbmsConfig);

		// Create a MonTableDictionary object
		MonTablesDictionary monTableDict = createMonTablesDictionary();
		MonTablesDictionaryManager.setInstance(monTableDict);


		//--------------------------------------------------------
		// ALARM Handling
		boolean enableAlarmHandler = Configuration.getCombinedConfiguration().getBooleanProperty(AlarmHandler.PROPKEY_enable, AlarmHandler.DEFAULT_enable);
		if (enableAlarmHandler)
		{
			//--------------------------------------------------------
			// Alarm Queue Handler (This will SOON be replaced with the AlarmHandler, below)
			//--------------------------------------------------------
//			AlarmQueueHandler aqh = new AlarmQueueHandler();
//			aqh.init(Configuration.getCombinedConfiguration());
//			aqh.start();
//			AlarmQueueHandler.setInstance(aqh);

			//--------------------------------------------------------
			// Alarm Handler
			//--------------------------------------------------------
//System.setProperty("AlarmHandler.WriterClass",                 "com.dbxtune.alarm.writers.AlarmWriterToStdout, com.dbxtune.alarm.writers.AlarmWriterToFile");
//System.setProperty("AlarmWriterToFile.alarms.active.filename", "c:\\tmp\\AseTune_alarm_active.log");
//System.setProperty("AlarmWriterToFile.alarms.log.filename",    "c:\\tmp\\AseTune_alarm.log");

			try
			{
				Configuration conf = Configuration.getCombinedConfiguration();
				if ( ! _gui )
					conf = Configuration.getInstance(Configuration.PCS);
				
				String alarmWriters = conf.getProperty(AlarmHandler.PROPKEY_WriterClass);
				if (StringUtil.hasValue(alarmWriters) || _gui)
				{
					//--------------------------------------------------------
					// User Defined Alarm Handler
					// Compiling some dynamic java classes
					//--------------------------------------------------------
					UserDefinedAlarmHandler udah = new UserDefinedAlarmHandler();
					udah.init(conf);
					UserDefinedAlarmHandler.setInstance(udah);

					// Initialize the alarm handler
					AlarmHandler ah = new AlarmHandler(AlarmHandler.DEFAULT_INSTANCE);
					AlarmHandler.setInstance(AlarmHandler.DEFAULT_INSTANCE, ah); // Set this before init() if it throws an exception and we are in GUI more, we still want to fix the error...
					ah.init(conf, _gui, true, true);
					ah.start();
					
					
					// In NI-GUI, you can simulate any DummyAlarm, let us know how...
					if ( ! _gui )
						_logger.info("To test the alarm subsystem, you can generate an alarm called 'AlarmEventDummy' by creating the file '"+ah.getDymmyAlarmFileName(null)+"'. The file will be deleted after the alarm is raised.");
				}
				else
				{
					_logger.warn("No 'Alarm Writers' was found in the current configuration '"+conf.getFilename()+"'. Alarm Handler will NOT be enabled. To enable the AlarmHandler, please specify any Alarm Writer classes using the configuration key '"+AlarmHandler.PROPKEY_WriterClass+"'.");
				}
			}
			catch (Exception ex)
			{
				_logger.error("Problems Initializing the Alarm Handler Module", ex);

				if ( ! _gui )
					throw ex;
				else
				{
					try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
					catch (Exception e) { _logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e); }

					String htmlMsg = 
							"<html>"
							+ "Propblems Initializing the Alarm Handler Module<br>"
							+ "<br>"
							+ "The Alarm Module or any of the Alarm Writers might not work as expected<br>"
							+ "Please look at the errolog and fix the problem.<br>"
							+ "<b>Then restart "+Version.getAppName()+"</b><br>"
							+ "<br>"
							+ "However the "+Version.getAppName()+" will start after you dismiss this message."
							+ "</html>";
					SwingUtils.showErrorMessage("Propblems Initializing the Alarm Handler Module", htmlMsg, ex);
				}
			}
		}
		

		
		if ( ! _gui )
		{
			_logger.info("Starting "+Version.getAppName()+" in NO-GUI mode, counters will be sampled into a database.");

			//---------------------------------
			// Go and check for updates, before continuing (timeout 10 seconds)
			//---------------------------------
			_logger.info("Checking for new release...");
//			CheckForUpdates.blockCheck(10*1000);
			CheckForUpdates.getInstance().checkForUpdateBlock(10*1000);

			// Version Helper
			IDbmsVersionHelper versionHelper = createDbmsVersionHelper();
			Ver.setDbmsVersionHelper(versionHelper);
			
			
			//---------------------------------
			// Install shutdown hook, that will STOP the collector (and SHUTDOWN H2)
			// This needs to be done before we start any connections to H2
			// NOTE: This is done in CounterCollectorThreadNoGui.init()
			
			//---------------------------------
			// Create and Start the "collector" thread
//			_counterController = new GetCountersNoGui();
			ICounterController counterController = createCounterController(_gui);
			CounterController.setInstance(counterController);
			counterController.setSupportedProductName(getSupportedProductName());
			counterController.init();
			counterController.start();

			//---------------------------------
			// Install shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				@Override
				public void run()
				{
					_logger.debug("----Start Shutdown Hook");
//					CheckForUpdates.sendCounterUsageInfo(true);
					CheckForUpdates.getInstance().sendCounterUsageInfo(true);
					_logger.debug("----End Shutdown Hook");
				}
			});

			// Start OutOfMemory check/thread
			// Listeners has to be attached, which is done in CounterCollectorThreadNoGui
			Memory.start();
			
			// Start watching configuration files for CHANGES (ONLY IN NO-GUI mode)
			Configuration.startCombinedConfigurationFileWatcher();
			Configuration.addCombinedConfigPropertyChangeListener(new PropertyChangeListener()
			{
				@Override
				public void propertyChange(PropertyChangeEvent evt)
				{
					String instName = "";
					if (evt.getSource() instanceof Configuration)
						instName = ((Configuration)evt.getSource()).getConfName();

					String propName = evt.getPropertyName();
					Object type     = evt.getPropagationId();
					Object newValue = evt.getNewValue();
					Object oldValue = evt.getOldValue();
					
					// If "Password", don't print the passwords
					if (propName != null && propName.indexOf("assword") >= 0)
					{
						newValue = "*secret*";
						oldValue = "*secret*";
					}
					_logger.info("COMBINED CONFIG CHANGE[" + instName + "]: propName='" + propName + "', type=" + Configuration.changeTypeToStr(type) + ", newValue='" + newValue + "', oldValue='" + oldValue + "'.");
				}
			});
			// do not care about keys starting with: "conn." 
			Configuration.addCombinedConfigurationFileWatcher_SkipKeyPrefix("conn.");

			String appStartupTime = TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - DbxTune.getStartTime());
			_logger.info("Application startup time "+appStartupTime+" (MM:SS.ms)");
		}
		else
		{
			_logger.info("Starting "+Version.getAppName()+" in GUI mode.");

			// Install a "special" EventQueue, which monitors deadlocks, and other "long" and time
			// consuming operations on the EDT (Event Dispatch Thread)
			// A WARN message will be written to the error log starting with 'Swing EDT-DEBUG - Hang: '
//			boolean useEdtHang = Version.getVersionStr().endsWith(".dev");
			boolean useEdtHang = System.getProperty("user.name").equals("goran");
			if (Debug.hasDebug(DebugOptions.EDT_HANG) || useEdtHang)
			{
				_logger.info("Installing a Swing EDT (Event Dispatch Thread) - Hang Monitor, which will write information about long running EDT operations to the "+Version.getAppName()+" log.");
				EventDispatchThreadHangMonitor.initMonitoring();
//				RepaintManager.setCurrentManager(new CheckThreadViolationRepaintManager());
			}

			// Do a dummy encryption, this will hopefully speedup, so that the connection dialog wont hang for a long time during initialization
			SplashWindow.drawProgress("Initializing: encryption package.");
			long initStartTime=System.currentTimeMillis();
			Encrypter propEncrypter = new Encrypter("someDummyStringToInitialize");
			String encrypedValue = propEncrypter.encrypt("TheDummyValueToEncrypt... this is just a dummy string...");
			propEncrypter.decrypt(encrypedValue); // Don't care about the result...
			_logger.info("Initializing 'encrypt/decrypt' package took: " + (System.currentTimeMillis() - initStartTime) + " ms.");

			// Construct a starter that will be passed to the Swing Event Dispatcher Thread
	        Runnable runGui = new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//						UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); // Metal
//						UIManager.setLookAndFeel("com.sun.java.swing.plaf.mac.MacLookAndFeel"); // Mac
//						UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"); // Windows
//						UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel"); // GNOME - Linux
//						UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel"); // MOTIF

						_logger.info("Using Look And Feel named '"+UIManager.getLookAndFeel().getName()+"', classname='"+UIManager.getLookAndFeel().getClass().getName()+"', toString='"+UIManager.getLookAndFeel()+"'.");
					}
					catch (Exception e)
					{
						_logger.warn("Problem setting the Look And Feel to 'getSystemLookAndFeelClassName()'.", e);
					}

//					try
//					{
//						// Workaround for error in Java (with modules): Unhandled Execption in SWING EventDispatchThread when dispatching an event. Caught: java.lang.IllegalAccessError: class com.jidesoft.plaf.LookAndFeelFactory (in unnamed module @0x2145b572) cannot access class com.sun.java.swing.plaf.windows.WindowsLookAndFeel (in module java.desktop) because module java.desktop does not export com.sun.java.swing.plaf.windows to unnamed module @0x2145b572
//						// Note: This is only for "Jide components"
//						// The below did NOT work... trying to use jvm switch '-Djide.defaultStyle=1' instead
//						LookAndFeelFactory.installJideExtension(LookAndFeelFactory.EXTENSION_STYLE_VSNET);
//					}
//					catch (Exception e)
//					{
//						_logger.warn("Problem setting the Look And Feel for JIDE Components. LookAndFeelFactory.installJideExtension(LookAndFeelFactory.EXTENSION_STYLE_VSNET);", e);
//					}
					
					// Disable save properties during startup, lets see if this can make the startup faster.
					Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
					if (tmpConf != null)
						tmpConf.setSaveEnable(false);

					// Set the Exception handler for the AWT/Swing Event Dispatch Thread
					// This will simply write the information to Log4J...
					SwingExceptionHandler.register();

					// Install our own EventQueue handler, to handle/log strange exceptions in the event dispatch thread
					// More or less the same thing as the above, we will have to see what handler we will use at-the-end-of-the-day
					EventQueueProxy.install();

//					if (SwingUtils.getHiDpiScale() > 1.0)
//					{
//						Font labelFont = UIManager.getFont("Label.font");
//						//Font textareaFont = UIManager.getFont("TextArea.font");
//						UIManager.put("TextArea.font", labelFont);
//					}

					// Create the GUI
					SplashWindow.drawProgress("Loading: Main Frame...");
//					final MainFrame frame = new MainFrame();
					final MainFrame frame = createGuiMainFrame();
					Configuration.setGuiWindow(frame);

					// Version Helper
					IDbmsVersionHelper versionHelper = createDbmsVersionHelper();
					Ver.setDbmsVersionHelper(versionHelper);
					
					// Create and Start the "collector" thread
					SplashWindow.drawProgress("Loading: Counter Models...");
					try
					{
//						_counterController = new GetCountersGui();
						ICounterController counterController = createCounterController(_gui);
						CounterController.setInstance(counterController);
						counterController.setSupportedProductName(getSupportedProductName());
						counterController.init();
						counterController.start();
					}
					catch (Exception e)
					{
						_logger.error("Problems when initializing/starting the GetCounterGui thread.", e);
						return;
					}

					// Load JTabbedPane: tab-order and visibility
					frame.loadTabOrderAndVisibility();

					//---------------------------------
					// Go and check for updates as well.
					//---------------------------------
					SplashWindow.drawProgress("Checking for new release...");
					_logger.info("Checking for new release...");
					//CheckForUpdates.noBlockCheck(frame, false, false);
//					CheckForUpdates.noBlockCheck(frame, false, true);
					// Create a thread that does this...
					// Apparently the noBlockCheckSqlWindow() hits problems when it accesses the CheckForUpdates, which uses ProxyVole
					// My guess is that ProxyVole want's to unpack it's DDL, which takes time...
					Thread checkForUpdatesThread = new Thread(new Runnable()
					{
						@Override
						public void run()
						{
//							CheckForUpdates.noBlockCheck(frame, false, true);
							CheckForUpdates.getInstance().checkForUpdateNoBlock(frame, false, true);
						}
					}, "checkForUpdatesThread");
					checkForUpdatesThread.setDaemon(true);
					checkForUpdatesThread.start();

					// Start OutOfMemory check/thread
					// Listeners has to be attached, which is done for example in MainFrame
					Memory.start();

					// Enable the save properties again.
					if (tmpConf != null)
					{
						tmpConf.setSaveEnable(true);
						tmpConf.save();
					}

//System.out.println(profiler.getTop(4));
//profiler.stopCollecting();

					// Remove this, it's a test
					boolean startConfigFileChangeListener = false;
					if (startConfigFileChangeListener)
					{
						Configuration.startCombinedConfigurationFileWatcher();
						Configuration.addCombinedConfigPropertyChangeListener(new PropertyChangeListener()
						{
							@Override
							public void propertyChange(PropertyChangeEvent evt)
							{
								String instName = "";
								if (evt.getSource() instanceof Configuration)
									instName = ((Configuration)evt.getSource()).getConfName();

								String propName = evt.getPropertyName();
								Object type     = evt.getPropagationId();
								Object newValue = evt.getNewValue();
								Object oldValue = evt.getOldValue();
								
								// If "Password", don't print the passwords
								if (propName != null && propName.indexOf("assword") >= 0)
								{
									newValue = "*secret*";
									oldValue = "*secret*";
								}
								_logger.info("COMBINED CONFIG CHANGE[" + instName + "]: propName='" + propName + "', type=" + Configuration.changeTypeToStr(type) + ", newValue='" + newValue + "', oldValue='" + oldValue + "'.");
							}
						});
					}

					String appStartupTime = TimeUtils.msToTimeStr("%MM:%SS.%ms", System.currentTimeMillis() - DbxTune.getStartTime());
					_logger.info("Application startup time "+appStartupTime+" (MM:SS.ms)");

					// FINALLY SHOW THE WINDOW
					SplashWindow.drawProgress("Loading Main Window...");
					frame.setVisible(true);
				}
			};
			
//			// Dummy profiler, lets use H2's built in profiler tool
//	        Thread runProfiler = new Thread()
//			{
//				public void run()
//				{
//					Profiler profiler = new Profiler();
//					profiler.interval = 1;
//					profiler.startCollecting();
//					
//					while(true)
//					{
//						try { Thread.sleep(5000); }
//						catch (InterruptedException e) {}
//
//						// application code
//						System.out.println(profiler.getTop(3));
//					}
//				}
//			};
//			runProfiler.setName("DbxTune-Profiler");
//			runProfiler.setDaemon(true);
//			runProfiler.start();
			
			SplashWindow.drawProgress("Invoking Swing Event Dispatcher Thread...");
			SwingUtilities.invokeLater(runGui);

		} // end: gui code
	}

//	/**
//	 * Get the <code>GetCounters</code> object
//	 * @return
//	 */
//	public static ICounterController getCounterCollector()
//	{
//		if (_counterController == null)
//			throw new RuntimeException("No Counter Collector has not yet been assigned.");
//		return _counterController;
//	}
//
//	/**
//	 * has any counter collector been assigned yet
//	 * @return
//	 */
//	public static boolean hasCounterCollector()
//	{
//		return _counterController != null;
//	}
//
//	/**
//	 * Do we have GUI mode enabled
//	 * @return
//	 */
//	public static boolean hasGUI()
//	{
//		return _gui;
//	}


	/**
	 * Print command line options.
	 * @param options
	 */
	public static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: "+getAppNameCmd()+" [-C <cfgFile>] [-c <cfgFile>] [-t <filename>] [-h] [-v] [-a]");
		pw.println("              [-U <user>]   [-P <passwd>]    [-S <server>] [-A <alias>] [-O <urlOptions>]");
		pw.println("              [-u <ssUser>] [-p <sshPasswd>] [-s <sshHostname>] [-k <keyFile>]");
		pw.println("              [-L <logfile>] [-H <dirname>] [-R <dirname>] [-D <key=val>]");
		pw.println("              [-n <cfgFile|cmNames>] [-r] [-l] [-i <seconds>] [-f <hours>]");
		pw.println("              [-d <dbname>] [-T <H2|ASE|ASE>] ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -C,--config <cfgName>        System Config file");
		pw.println("  -c,--userConfig <cfgName>    User Config file, overrides values in System cfg.");
		pw.println("  -t,--tmpConfig <filename>    Config file where temporary stuff are stored.");
		pw.println("  -h,--help                    Usage information.");
		pw.println("  -v,--version                 Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>       Debug options: a comma separated string");
		pw.println("                               To get available option, do -x list");
		pw.println("  -a,--createAppDir            Create application dir (~/.dbxtune) and exit.");
		pw.println("  ");                          
		pw.println("  -U,--user <user>             Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>         Password when connecting to server. null=noPasswd");
		pw.println("  -S,--server <server>         Server to connect to.");
		pw.println("  -A,--serverAlias <name>      Server Alias (used by PCS and DbxCentral) for alternate name.");
		pw.println("                               note: <SRVNAME> will be replaced with the DBMS Instance Name");
		pw.println("  -N,--displayName <name>      Display Name (used by DbxCentral) to label buttons at start page.");
		pw.println("                               note: <SRVNAME> will be replaced with the DBMS Instance Name");
		pw.println("  -O,--urlOptions <server>     jdbc Url options/properties example: 'key1=val;key2=val'");
		pw.println("  ");
		pw.println("  -u,--sshUser <user>          SSH Username, used by Host Monitoring subsystem.");
		pw.println("                                   Note: can be specified as 'user:lookupKey' which can be used");
		pw.println("                                   if password are stored in ~/.passwd.enc and the user/key ");
		pw.println("                                   is not the same as the username.");
		pw.println("  -p,--sshPasswd <passwd>      SSH Password, used by Host Monitoring subsystem.");
		pw.println("  -k,--sshKeyFile <file>       SSH Private Key File, used by Host Monitoring subsystem.");
		pw.println("  -s,--sshServer <host>        SSH Hostname, used by Host Monitoring subsystem.");
		pw.println("  --localHostMon               Execute the host mon commands on local machine (no SSH)");
		pw.println("  --localHostMonWrapperCmd <cmd>  Use a 'wrapper' command to execute the local command");
		pw.println("                               - This may be used to switch to another user");
		pw.println("                               - Or wrap a 'ssh' execution if the above --ssh* isn't working.");
		pw.println("                               Following vars is available: ${hostMonCmd} ");
		pw.println("                                      ${sshUser} ${sshPasswd} ${sshKeyFile} ${sshServer}");
		pw.println("  ");
		pw.println("  -L,--logfile <filename>      Name of the logfile where application logging is saved.");
		pw.println("                               if the filename is only a directory, the -S or -A will be");
		pw.println("                               used as the filename in that directory.");
		pw.println("  -H,--homedir <dirname>       HOME Directory, where all personal files are stored.");
		pw.println("  -R,--savedir <dirname>       DBXTUNE_SAVE_DIR, where H2 Database recordings are stored.");
		pw.println("  -D,--javaSystemProp <k=v>    Set Java System Property, same as java -Dkey=value");
		pw.println("  ");
		pw.println("  Switches for offline mode:");
		pw.println("  -n,--noGui <cfgFile|cmNames> Do not start with GUI.");
		pw.println("                               instead collect counters to a database.");
		pw.println("                               cfgFile = <a config file for offline sample>");
		pw.println("                                         which can be generated with the wizard.");
		pw.println("                               cmNames = <small|medium|large|all>.");
		pw.println("                                         or a comma separated list on CMNames.");
		pw.println("                               You can also combine a template and add/remove cm's.");
		pw.println("                               Example: -n medium,-CmDeviceIo,+CmSpinlockSum");
		pw.println("  -r,--reconfigure             If the monitored ASE is not properly configured,");
		pw.println("                               then try to configure it using predefined values.");
		pw.println("  -l,--listcm                  List cm's that can be used in '-n' switch.");
		pw.println("  -i,--interval <seconds>      sample Interval, time between samples.");
		pw.println("  -e,--enable   <hh[:mm]>      enable/start the recording at Hour(00-23) Minute(00-59)");
		pw.println("  -f,--finish   <hh[:mm]>      shutdown/stop the no-gui service after # hours.");
		pw.println("  -T,--dbtype <H2|ASE|ASA>     type of database to use as offline store.");
		pw.println("  -d,--dbname <connSpec>       Connection specification to store offline data.");
		pw.println("                               Depends on -T, see below for more info.");
		pw.println("");
		pw.println("If you specify '-T and -d':");
		pw.println("  -T H2  -d h2dbfile|default");
		pw.println("  -T ASE -d hostname:port:dbname:user:passwd");
		pw.println("  -T ASA -d hostname:port:dbname:user:passwd");
		pw.println("  NOTE: Only -T H2 is supported for the moment (ASE, and ASA can still be used, but may *not* work as expected)");
		pw.println("  Default connection specifications for different dbtypes:");
		pw.println("     H2:  'h2dbfile' file is mandatory.");
		pw.println("          Use: -d default, will set h2dbfile to '${<DBXTUNE>_SAVE_DIR}/${SERVERNAME}_${DATE}'");
		pw.println("     ASE: 'hostname:port:dbname:user:passwd' is all mandatory.");
		pw.println("     ASA: port=2638, dbname='', user='DBA', passwd='SQL'");
		pw.println("  If only '-d' is given, the default value for '-T' is 'H2'.");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options parser. Has to be synchronized because of the way
	 * Options are constructed.
	 *
	 * @return an options parser.
	 */
	public static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

//		// create the Options
//		options.addOption( "C", "config",         true,  "The System Config file" );
//		options.addOption( "c", "userConfig",     true,  "The User Config file where, overrides values in System Config." );
//		options.addOption( "t", "tmpConfig",      true,  "Config file where temporary stuff are stored." );
//		options.addOption( "h", "help",           false, "Usage information." );
//		options.addOption( "v", "version",        false, "Display "+Version.getAppName()+" and JVM Version." );
//		options.addOption( "x", "debug",          true,  "Debug options: a comma separated string dbg1,dbg2,dbg3" );
//                                                  
//		options.addOption( "U", "user",           true, "Username when connecting to server." );
//		options.addOption( "P", "passwd",         true, "Password when connecting to server. (null=noPasswd)" );
//		options.addOption( "S", "server",         true, "Server to connect to." );
//                                                  
//		options.addOption( "u", "sshUser",        true, "SSH Username when connecting to server for Host Monitoring." );
//		options.addOption( "p", "sshPasswd",      true, "SSH Password when connecting to server for Host Monitoring." );
//		options.addOption( "s", "sshServer",      true, "SSH Hostname to connect to." );
//                                                  
//		options.addOption( "L", "logfile",        true, "Name of the logfile." );
//		options.addOption( "H", "homedir",        true, "HOME Directory, where all personal files are stored." );
//		options.addOption( "R", "savedir",        true, "DBXTUNE_SAVE_DIR, where H2 Database recordings are stored." );
////		options.addOption( "D", "javaSystemProp", true, "set Java System Property, same as java -DpropName=val, example -DpropName=val" );
//		options.addOption( Option.builder("D").longOpt("javaSystemProp").hasArg().valueSeparator('=').build() );
//
//		options.addOption( "n", "noGui",          true, "Do not start with GUI, instead collect counters to a database <a config file for offline sample>." );
//		options.addOption( "r", "reconfigure",    false,"Offline: if monitored ASE is not properly configured, try to configure it." );
//		options.addOption( "l", "listcm",         false,"Offline: List cm's that can be used in '-n' switch." );
//		options.addOption( "i", "interval",       true, "Offline: Sample Interval, time between samples." );
//		options.addOption( "T", "dbtype",         true, "Offline: {H2|ASE|ASA} database type default is H2." );
//		options.addOption( "d", "dbname",         true, "Offline: dbname/file to store offline samples." );
//		options.addOption( "f", "finish",         true, "Offline: Shutdown the NO-GUI service after # hours" );
//		options.addOption( "e", "enable",         true, "Offline: enable/start the recording at Hour Minute" );

		// create the Options
		options.addOption( Option.builder("C").longOpt("config"        ).hasArg(true ).build() );
		options.addOption( Option.builder("c").longOpt("userConfig"    ).hasArg(true ).build() );
		options.addOption( Option.builder("t").longOpt("tmpConfig"     ).hasArg(true ).build() );
		options.addOption( Option.builder("h").longOpt("help"          ).hasArg(false).build() );
		options.addOption( Option.builder("v").longOpt("version"       ).hasArg(false).build() );
		options.addOption( Option.builder("x").longOpt("debug"         ).hasArg(true ).build() );

		options.addOption( Option.builder("a").longOpt("createAppDir"  ).hasArg(false).build() );

		options.addOption( Option.builder("U").longOpt("user"          ).hasArg(true ).build() );
		options.addOption( Option.builder("P").longOpt("passwd"        ).hasArg(true ).build() );
		options.addOption( Option.builder("S").longOpt("server"        ).hasArg(true ).build() );
		options.addOption( Option.builder("A").longOpt("serverAlias"   ).hasArg(true ).build() );
		options.addOption( Option.builder("N").longOpt("displayName"   ).hasArg(true ).build() );
		options.addOption( Option.builder("O").longOpt("urlOptions"    ).hasArg(true ).build() );

		options.addOption( Option.builder("u").longOpt("sshUser"       ).hasArg(true ).build() );
		options.addOption( Option.builder("p").longOpt("sshPasswd"     ).hasArg(true ).build() );
		options.addOption( Option.builder("k").longOpt("sshKeyFile"    ).hasArg(true ).build() );
		options.addOption( Option.builder("s").longOpt("sshServer"     ).hasArg(true ).build() );
		options.addOption( Option.builder("localHostMon"               ).hasArg(false).build() );
		options.addOption( Option.builder("localHostMonWrapperCmd"     ).hasArg(true ).build() );

		options.addOption( Option.builder("L").longOpt("logfile"       ).hasArg(true ).build() );
		options.addOption( Option.builder("H").longOpt("homedir"       ).hasArg(true ).build() );
		options.addOption( Option.builder("R").longOpt("savedir"       ).hasArg(true ).build() );
		options.addOption( Option.builder("D").longOpt("javaSystemProp").hasArgs().valueSeparator('=').build() ); // NOTE the hasArgs() instead of hasArg() *** the 's' at the end of hasArg<s>() does the trick...

		options.addOption( Option.builder("n").longOpt("noGui"         ).hasArg(true ).build() );
		options.addOption( Option.builder("r").longOpt("reconfigure"   ).hasArg(false).build() );
		options.addOption( Option.builder("l").longOpt("listcm"        ).hasArg(false).build() );
		options.addOption( Option.builder("i").longOpt("interval"      ).hasArg(true ).build() );
		options.addOption( Option.builder("T").longOpt("dbtype"        ).hasArg(true ).build() );
		options.addOption( Option.builder("d").longOpt("dbname"        ).hasArg(true ).build() );
		options.addOption( Option.builder("f").longOpt("finish"        ).hasArg(true ).build() );
		options.addOption( Option.builder("e").longOpt("enable"        ).hasArg(true ).build() );

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	public static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line parser
		CommandLineParser parser = new DefaultParser();

		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		if (_logger.isDebugEnabled())
		{
			for (Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}

	public static void printTemplate(String templateName)
	throws Exception
	{
		List<String> lst = CounterSetTemplates.getTemplateList(templateName);

		System.out.println("======================================================================");
		System.out.println("Template name '"+templateName+"', contains the following counters.");
		System.out.println("Short name           Postpone Long Name");
		System.out.println("-------------------- -------- ----------------------------------------------");

		// NOW, lets look at the list and write some info
		for (String cmNameStr : lst)
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
				catch (NumberFormatException ignore) {/* ignore */}
			}

			// Get CM: first try the "short" name, example "CMprocCallStack"
			//          then try the "long"  name, example "Procedure Call Stack"
			String name;
			name = CounterSetTemplates.getShortName(cmName);
			String shortName = (name == null) ? cmName : name;

			name = CounterSetTemplates.getLongName(cmName);
			String longName = (name == null) ? cmName : name;

			System.out.println(StringUtil.left(shortName, 20) + " " + StringUtil.left(postponeTime+"", 8) + " " + longName);
		}
		System.out.println();

	}

	public static void main_testCmdLineOptions(String[] args)
	{
		try
		{
			Options options = new Options();

			options.addOption( Option.builder("D").longOpt("javaSystemProp").hasArgs().valueSeparator('=').build() ); // NOTE the hasArgs() instead of hasArg() *** the 's' at the end of hasArg<s>()

//			String[] args = { "-Dxxxxxxxx=111" };
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

//			String propertyName = cmd.getOptionValues("D")[0]; // will be "key"
//			String propertyValue = cmd.getOptionValues("D")[1]; // will be "value"
//			System.out.println("propertyName=|"+propertyName+"|");
//			System.out.println("propertyValue=|"+propertyValue+"|");

			if (cmd.hasOption('D'))
			{
				Properties javaProps = cmd.getOptionProperties("D");
				System.setProperties(javaProps);
				
				for (Object key : javaProps.keySet())
				{
					Object val = javaProps.get(key);
					System.out.println("SYSTEM: key=|"+key+"|, val=|"+val+"|.");
				}
			}

			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
		// Get MainClass
		// This will be used later to instantiate the correct product
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StackTraceElement main = stack[stack.length - 1];
		_mainClassName = main.getClassName().toLowerCase();
		int lastDot = _mainClassName.lastIndexOf('.');
		if (lastDot != -1)
			_mainClassName = _mainClassName.substring(lastDot+1);


		// if we want to print input arguments
		//System.setProperty("dbxtune.print.input.args", "true");
		if ( System.getProperty("dbxtune.print.input.args") != null )
		{
			for (int i=0; i<args.length; i++)
			{
				System.out.println("TRACE-INPUT-ARGS["+i+"] = |"+args[i]+"|");
			}
		}


		Options options = buildCommandLineOptions();
		try
		{
			CommandLine cmd = parseCommandLine(args, options);

			//-------------------------------
			// HELP
			//-------------------------------
			if ( cmd.hasOption("help") )
			{
				printHelp(options, "The option '--help' was passed.");
			}
			//-------------------------------
			// VERSION
			//-------------------------------
			else if ( cmd.hasOption("version") )
			{
				System.out.println();
				System.out.println(Version.getAppName()+" Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			//-------------------------------
			// CREATE APP DIR
			//-------------------------------
			else if ( cmd.hasOption("createAppDir") )
			{
				// Create store dir if it did not exists.
				AppDir.checkCreateAppDir( null, System.out );
			}
			//-------------------------------
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start DbxTune, GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				if ( cmd.hasOption("homedir") )
					System.setProperty("user.home", cmd.getOptionValue("homedir"));

				if ( cmd.hasOption("savedir") )
					System.setProperty("DBXTUNE_SAVE_DIR", cmd.getOptionValue("savedir"));
				
				// Note: _instance is also set in the DbxTune constructor... This is if any of the subinitializers use DbxTune.getIntance()
				if      ("AseTune"      .equalsIgnoreCase(_mainClassName)) _instance = new AseTune      (cmd);
				else if ("IqTune"       .equalsIgnoreCase(_mainClassName)) _instance = new IqTune       (cmd);
				else if ("RsTune"       .equalsIgnoreCase(_mainClassName)) _instance = new RsTune       (cmd);
				else if ("RaxTune"      .equalsIgnoreCase(_mainClassName)) _instance = new RaxTune      (cmd);
				else if ("HanaTune"     .equalsIgnoreCase(_mainClassName)) _instance = new HanaTune     (cmd);
				else if ("SqlServerTune".equalsIgnoreCase(_mainClassName)) _instance = new SqlServerTune(cmd);
				else if ("OracleTune"   .equalsIgnoreCase(_mainClassName)) _instance = new OracleTune   (cmd);
				else if ("PostgresTune" .equalsIgnoreCase(_mainClassName)) _instance = new PostgresTune (cmd);
				else if ("MySqlTune"    .equalsIgnoreCase(_mainClassName)) _instance = new MySqlTune    (cmd);
				else if ("Db2Tune"      .equalsIgnoreCase(_mainClassName)) _instance = new Db2Tune      (cmd);
				else
				{
					throw new Exception("Unknown Implementor of type '"+_mainClassName+"'.");
				}
			}
		}
		catch (ParseException pe)
		{
//pe.printStackTrace();
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters
			// do normal exit
		}
		catch (Exception e)
		{
			System.out.println();
			System.out.println("Error: " + e.getMessage());
			System.out.println();
			System.out.println("Printing a stacktrace, where the error occurred.");
			System.out.println("--------------------------------------------------------------------");
			e.printStackTrace();
			System.out.println("--------------------------------------------------------------------");
			System.exit(1);
		}

		// Did anyone set that we requested a restart, then exit with 8  ( or laying 8 : a lemniscate = infinity symbol)
		if (ShutdownHandler.wasRestartSpecified())
		{
			System.exit(ShutdownHandler.RESTART_EXIT_CODE);
		}
	}
}

//LOOK AT:
//	* tooltip (focusable) tooltip and normal tooltip... maybe create your own normal-tooltip manager, that handles both normal and focusable tooltip.
//	  or at least... focusable tooltip should be a singleton so we can "cancel" other tooltip that might still be "up"
//	  and thats also the reason why we would want to group the two together...

