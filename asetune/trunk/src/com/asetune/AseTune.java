/**
 */
package com.asetune;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import com.asetune.check.CheckForUpdates;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.gui.GuiLogAppender;
import com.asetune.gui.MainFrame;
import com.asetune.gui.SplashWindow;
import com.asetune.gui.swing.debug.EventDispatchThreadHangMonitor;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.Configuration;
import com.asetune.utils.Debug;
import com.asetune.utils.JavaVersion;
import com.asetune.utils.Logging;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingExceptionHandler;
import com.asetune.utils.SwingUtils;


public class AseTune
{
	private static Logger _logger = Logger.getLogger(AseTune.class);

//	private boolean	     packFrame	= false;
//	public static String	java_version;

	/** This can be either GUI or NO-GUI "collector" */
	private static GetCounters   getCnt          = null;

	//static long mainThreadId = -1;

	private static boolean _gui = true;
//	private static CommandLine _cmdLine = null;
	
	public AseTune(CommandLine cmd)
	throws Exception
	{
		// Kick it off
		init(cmd);
	}

	private static boolean _hasDevVersionExpired = false;
	public static boolean hasDevVersionExpired()
	{
		return _hasDevVersionExpired;
	}

	/**
	 * Initialize and start AseTune
	 * @param propFile
	 * @param savePropFile
	 * @throws Exception
	 */
	public void init(CommandLine cmd)
	throws Exception
	{
//		_cmdLine = cmd;

		// Create store dir if it did not exists.
		File appStoreDir = new File(Version.APP_STORE_DIR);
		if ( ! appStoreDir.exists() )
		{
			if (appStoreDir.mkdir())
				System.out.println("Creating directory '"+appStoreDir+"' to hold various files for "+Version.getAppName());
		}

		
		// -----------------------------------------------------------------
		// CHECK/SETUP information from the CommandLine switches
		// -----------------------------------------------------------------
		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME",      "asetune.properties");
		final String USER_CONFIG_FILE_NAME = System.getProperty("USER_CONFIG_FILE_NAME", "asetune.user.properties");
		final String TMP_CONFIG_FILE_NAME  = System.getProperty("TMP_CONFIG_FILE_NAME",  "asetune.save.properties");
		final String ASETUNE_HOME          = System.getProperty("ASETUNE_HOME");
		
		String defaultPropsFile     = (ASETUNE_HOME          != null) ? ASETUNE_HOME          + "/" + CONFIG_FILE_NAME      : CONFIG_FILE_NAME;
		String defaultUserPropsFile = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + "/" + USER_CONFIG_FILE_NAME : USER_CONFIG_FILE_NAME;
		String defaultTmpPropsFile  = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + "/" + TMP_CONFIG_FILE_NAME  : TMP_CONFIG_FILE_NAME;

		// Compose MAIN CONFIG file (first USER_HOME then ASETUNE_HOME)
		String filename = Version.APP_STORE_DIR + "/" + CONFIG_FILE_NAME;
		if ( (new File(filename)).exists() )
			defaultPropsFile = filename;

		String propFile        = cmd.getOptionValue("config",     defaultPropsFile);
		String userPropFile    = cmd.getOptionValue("userConfig", defaultUserPropsFile);
		String tmpPropFile     = cmd.getOptionValue("tmpConfig",  defaultTmpPropsFile);
		String noGuiConfigFile = cmd.getOptionValue("noGui");

		// Check if the configuration file exists
		if ( ! (new File(propFile)).exists() )
			throw new FileNotFoundException("The configuration file '"+propFile+"' doesn't exists.");

		// Are we in GUI mode or not
		_gui = true;
		if (cmd.hasOption("noGui"))
			_gui = false;
		System.setProperty("application.gui", Boolean.toString(_gui)); // used in AseConnectionFactory

		// -----------------------------------------------------------------
		// CHECK JAVA JVM VERSION
		// -----------------------------------------------------------------
		int javaVersionInt = JavaVersion.getVersion();
		if (   javaVersionInt != JavaVersion.VERSION_NOTFOUND 
		    && javaVersionInt <  JavaVersion.VERSION_1_6
		   )
		{
			System.out.println("");
			System.out.println("===============================================================");
			System.out.println(" "+Version.getAppName()+" needs a runtime JVM 1.6 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception(Version.getAppName()+" needs a runtime JVM 1.6 or higher.");
		}

		// The SAVE Properties...
		Configuration appSaveProps = new Configuration(tmpPropFile);
		Configuration.setInstance(Configuration.USER_TEMP, appSaveProps);

		// Get the USER properties that could override CONF
		Configuration appUserProps = new Configuration(userPropFile);
		Configuration.setInstance(Configuration.USER_CONF, appUserProps);

		// Get the "OTHER" properties that has to do with LOGGING etc...
		Configuration appProps = new Configuration(propFile);
		Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);

		// The Store Properties...
		Configuration storeConfigProps = new Configuration();
		Configuration.setInstance(Configuration.PCS, storeConfigProps);

		// Set the Configuration search order when using the: Configuration.getCombinedConfiguration()
		Configuration.setSearchOrder(
			Configuration.USER_TEMP,    // First
			Configuration.USER_CONF,    // second
			Configuration.SYSTEM_CONF); // Third
		
		//-------------------------------
		// LIST CM
		//-------------------------------
		if (cmd.hasOption('l'))
		{
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
			
			throw new NormalExitException("Done after -l switch.");
		}

		//-------------------------------
		// DEBUG switches
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

		// Put command line properties into the Configuration
		if ( _gui )
		{
			if (cmd.hasOption('U'))	appProps.setProperty("cmdLine.aseUsername", cmd.getOptionValue('U'));
			if (cmd.hasOption('P'))	appProps.setProperty("cmdLine.asePassword", cmd.getOptionValue('P'));
			if (cmd.hasOption('S'))	appProps.setProperty("cmdLine.aseServer",   cmd.getOptionValue('S'));

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

			if (cmd.hasOption('u'))	storeConfigProps.setProperty("cmdLine.sshUsername", cmd.getOptionValue('u'));
			if (cmd.hasOption('p'))	storeConfigProps.setProperty("cmdLine.sshPassword", cmd.getOptionValue('p'));
			if (cmd.hasOption('s'))	
			{
				storeConfigProps.setProperty("cmdLine.sshHostname", cmd.getOptionValue('s'));
				storeConfigProps.setProperty("cmdLine.sshPort", 22);

				String cmdLineHostname = cmd.getOptionValue('s');
				if (cmdLineHostname.indexOf(":") >= 0)
				{
					String[] sa = cmdLineHostname.split(":");
					storeConfigProps.setProperty("cmdLine.sshHostname", sa[0]);
					storeConfigProps.setProperty("cmdLine.sshPort",     Integer.parseInt(sa[1]));
				}
			}
		}
		else
		{
			// Check if the configuration file exists
			// or if it's appropriate options...
			if ( (new File(noGuiConfigFile)).exists() )
			{
				storeConfigProps.load(noGuiConfigFile);
			}
			else
			{
				if (GetCountersNoGui.checkValidCmShortcuts(noGuiConfigFile))
					storeConfigProps.setProperty("cmdLine.cmOptions", noGuiConfigFile);
				else
					throw new FileNotFoundException("The noGuiConfig file '"+noGuiConfigFile+"' doesn't exists.");
			}

			if (cmd.hasOption('U'))	storeConfigProps.setProperty("conn.aseUsername", cmd.getOptionValue('U'));
			if (cmd.hasOption('P'))	storeConfigProps.setProperty("conn.asePassword", cmd.getOptionValue('P'));
			if (cmd.hasOption('S'))	storeConfigProps.setProperty("conn.aseName",     cmd.getOptionValue('S'));
	
			if (cmd.hasOption('u'))	storeConfigProps.setProperty("conn.sshUsername", cmd.getOptionValue('u'));
			if (cmd.hasOption('p'))	storeConfigProps.setProperty("conn.sshPassword", cmd.getOptionValue('p'));
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
				storeConfigProps.setProperty("offline.shutdownAfterXHours", cmd.getOptionValue('f'));
			}

			// -D --dbtype: Offline: Type of database storage H2 or ASE or ASA
			// values for -D is handled within the -d section
			if (cmd.hasOption('D'))
			{
				if ( ! cmd.hasOption('d') )
				{
					throw new Exception("If you use switch '-D|--dbtype' you must also use switch '-d|--dbname'.");
				}
			}

			// -d --dbname: Offline: dbname to store offline samples.
			if (cmd.hasOption('d'))
			{
				String opt = cmd.getOptionValue('d');
				_logger.info("Command Line Option '-d|--dbname' was specified, dbname to use is '"+opt+"'.");

				String opt_D = "H2";
				if (cmd.hasOption('D'))
				{
					opt_D = cmd.getOptionValue('D').toUpperCase();
					_logger.info("Command Line Option '-D|--dbtype' was specified, type to use is '"+opt_D+"'.");
				}
				else
				{
					opt_D = "H2";
					_logger.info("Command Line Option '-D|--dbtype' was NOT specified, using the default 'H2'.");
				}
				
				if (opt_D.equals("H2"))
				{
					String jdbcDriver = "org.h2.Driver";
					String jdbcUrl    = "jdbc:h2:file:"+opt;
					String jdbcUser   = "sa";
					String jdbcPasswd = "";

					storeConfigProps.setProperty("PersistWriterJdbc.jdbcDriver", jdbcDriver);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcUrl",    jdbcUrl);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcUser",   jdbcUser);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcPasswd", jdbcPasswd);
					storeConfigProps.setProperty("PersistWriterJdbc.startH2NetworkServer", true);
		
					storeConfigProps.setProperty("PersistentCounterHandler.WriterClass", "asetune.pcs.PersistWriterJdbc");

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

					storeConfigProps.setProperty("PersistWriterJdbc.jdbcDriver", jdbcDriver);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcUrl",    jdbcUrl);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcUser",   jdbcUser);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcPasswd", jdbcPasswd);
		
					storeConfigProps.setProperty("PersistentCounterHandler.WriterClass", "asetune.pcs.PersistWriterJdbc");

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

					storeConfigProps.setProperty("PersistWriterJdbc.jdbcDriver", jdbcDriver);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcUrl",    jdbcUrl);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcUser",   jdbcUser);
					storeConfigProps.setProperty("PersistWriterJdbc.jdbcPasswd", jdbcPasswd);
		
					storeConfigProps.setProperty("PersistentCounterHandler.WriterClass", "asetune.pcs.PersistWriterJdbc");

					_logger.info("PCS: using jdbcDriver='"+jdbcDriver+"', jdbcUrl='"+jdbcUrl+"', jdbcUser='"+jdbcUser+"', jdbcPasswd='*secret*', dbname='"+asaDbname+"'.");
				}
				else
				{
					throw new Exception("Unknown -D,--dbtype value of '"+opt+"' was specified, known values 'H2|ASE|ASA'.");
				}
			}
		}

		
//		// Check if we are in NO-GUI mode
//		_gui = System.getProperty("asetune.gui", "true").trim().equalsIgnoreCase("true");

		// Show a SPLASH SCREEN
		if (_gui)
		{
			// How many steps do we have, NOTE: needs to be updated if you add steps
			SplashWindow.init(false, 33, 1000);
			SplashWindow.drawTopRight("Version: " + Version.getVersionStr());
		}
		else
		{
			SplashWindow.close();
		}
		
		// Setup HARDCODED, configuration for LOG4J, if not found in config file
		if (_gui)
			Logging.init(null, propFile);
		else
			Logging.init("nogui.", propFile);

		if (_gui && !SplashWindow.isOk())
			_logger.info("Splash screen could not be displayed.");

		SplashWindow.drawProgress("Initializing.");

		//--------------------------------------------------------------------
		// Set some SYSTEM properties
		// - Mainly for H2 database specific settings
		// - But any system setting could be set here
		System.setProperty("h2.lobInDatabase", "true"); // H2 database version 1.2.134, can store BLOBS inside the database using this system property


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

		SplashWindow.drawProgress("Initializing..");

		// Initialize this early...
		// If it's initalized after any connect attempt we might have
		// problems connecting to various destinations.
		CheckForUpdates.init();


		SplashWindow.drawProgress("Initializing...");
		if (_gui)
		{
			// Calling this would make GuiLogAppender, to register itself in log4j.
			GuiLogAppender.getInstance();

			// How long should a ToolTip be displayed... 
			// this is especially good for WaitEventID tooltip help, which could be much text to read. 
//			ToolTipManager.sharedInstance().setDismissDelay(120*1000); // 2 minutes
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
		_logger.info("Running on Operating System Name:  "+System.getProperty("os.name"));
		_logger.info("Running on Operating System Version:  "+System.getProperty("os.version"));
		_logger.info("Running on Operating System Architecture:  "+System.getProperty("os.arch"));
		_logger.info("The application was started by the username:  "+System.getProperty("user.name"));
		_logger.info("The application was started in the directory:   "+System.getProperty("user.dir"));
		
		_logger.info("System configuration file is '"+propFile+"'.");
		_logger.info("User configuration file is '"+userPropFile+"'.");
		_logger.info("Storing temporary configurations in file '"+tmpPropFile+"'.");
		_logger.info("Combined Configuration Search Order '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");

		// check if sufficient memory has been configured.
		// [FIXME] what is appropriate default value here.
		String needMemInMBStr = "32";
		int maxConfigMemInMB = (int) Runtime.getRuntime().maxMemory() / 1024 / 1024; // jdk 1.4 or higher
		int needMemInMB = Integer.parseInt( appProps.getProperty("minMemoryLimitInMB", needMemInMBStr) );
		if (maxConfigMemInMB < needMemInMB)
		{
			String message = "I need at least "+needMemInMB+" MB to start this process. Maximum memory limit is now configured to "+maxConfigMemInMB+" MB. Specify this at the JVM startup using the -Xmx###m flag. ### is the upper limit (in MB) that this JVM could use.";
			_logger.error(message);
			throw new Exception(message);
		}

		//-------------------------------------------------------------------------
		// HARDCODE a STOP date when this "DEVELOPMENT VERSION" will STOP working
		//-------------------------------------------------------------------------
		if (Version.IS_DEVELOPMENT_VERSION)
		{
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

			_logger.info("This DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', then you will have to download a later version..");
			if ( System.currentTimeMillis() > Version.DEV_VERSION_EXPIRE_DATE.getTime() )
			{
				_hasDevVersionExpired = true;

				String msg = "This DEVELOPMENT VERSION has expired. The \"time to live\" period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'. A new version can be downloaded here 'http://www.asetune.com'";
				_logger.error(msg);
				Exception ex = new Exception(msg);
				if (_gui)
				{
					SwingUtils.showErrorMessage(Version.getAppName()+" - This DEVELOPMENT VERSION has expired", 
							"This DEVELOPMENT VERSION has expired.\n" +
							"\n" +
							"The \"time to live\" period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'. \n" +
							"A new version can be downloaded here 'http://www.asetune.com'\n" +
							"\n" +
							"The application will still start\n" +
							"But it can only read data from Persistent Counter Storage, also called 'offline databases'.\n" +
							"\n" +
							"But I strongly encourage you to download the latest release, " +
							"This 'start' option was implemented as a fail-safe mechanism, and " +
							"should only be used if the new "+Version.getAppName()+" version is not capable of reading the " +
							"old database. In 99.9% the new "+Version.getAppName()+" version will be able to read " +
							"Performance Counters stored by a earlier "+Version.getAppName()+" release.",
							ex);
				}
				else
				{
					// NO-GUI mode is NOT allowed to start if it's a OLD DEVELOPMENT Version 
					throw ex;
				}
			}
		}

		if ( ! _gui )
		{
			_logger.info("Starting "+Version.getAppName()+" in NO-GUI mode, counters will be sampled into a database.");

			// Create and Start the "collector" thread
			getCnt = new GetCountersNoGui();
			getCnt.init();
			getCnt.start();

			//---------------------------------
			// Go and check for updates aswell.
			//---------------------------------
			_logger.info("Checking for new release...");
			CheckForUpdates.noBlockCheck(null, false, false);
		}
		else
		{
			_logger.info("Starting "+Version.getAppName()+" in GUI mode.");
			
			// Install a "special" EventQueue, which monitors deadlocks, and other "long" and time 
			// consuming operations on the EDT (Event Dispatch Thread)
			// A WARN message will be written to the error log starting with 'Swing EDT-DEBUG - Hang: '
			if (Debug.hasDebug(DebugOptions.EDT_HANG))
			{
				_logger.info("Installing a Swing EDT (Event Dispatch Thread) - Hang Monitor, which will write information about long running EDT operations to the "+Version.getAppName()+" log.");
				EventDispatchThreadHangMonitor.initMonitoring();
			}

			// Construct a starter that will be passed to the Swing Event Dispatcher Thread
	        Runnable runGui = new Runnable() 
			{
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

					// Set the Exception handler for the AWT/Swing Event Dispatch Thread
					// This will simply write the information to Log4J...
					SwingExceptionHandler.register();

					// Create the GUI
					SplashWindow.drawProgress("Loading: Main Frame...");
					MainFrame frame = new MainFrame();

					// Create and Start the "collector" thread
					SplashWindow.drawProgress("Loading: Counter Models...");
					try
					{
						getCnt = new GetCountersGui();
						getCnt.init();
						getCnt.start();
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
					CheckForUpdates.noBlockCheck(frame, false, true);

					// FINALLY SHOW THE WINDOW
					SplashWindow.drawProgress("Loading Main Window...");
					frame.setVisible(true);
				}
			};
			SplashWindow.drawProgress("Invoking Swing Event Dispatcher Thread...");
			SwingUtilities.invokeLater(runGui);

		} // end: gui code
	}

	/**
	 * Get the <code>CommandLine</code> parameters
	 * @return CommandLine
	 */
//	public static CommandLine getCmdLineParams()
//	{
//		return _cmdLine;
//	}

	/**
	 * Get the <code>GetCounters</code> object
	 * @return
	 */
	public static GetCounters getCounterCollector()
	{
		if (getCnt == null)
			throw new RuntimeException("No Counter Collector has not yet been assigned.");
		return getCnt;
	}

	/**
	 * Do we have GUI mode enabled
	 * @return
	 */
	public static boolean hasGUI()
	{
		return _gui;
	}

//	private static void printHelp_OLD(Options options, String errorStr)
//	{
//		PrintWriter pw = new PrintWriter(System.out);
//
//		if (errorStr != null)
//		{
//			pw.println();
//			pw.println(errorStr);
//			pw.println();
//		}
//
//		HelpFormatter formatter = new HelpFormatter();
//
//		formatter.printHelp(pw, 80, "asetune", "options:", options, 2, 4, 
//				null, // footer
//				true);
//
//		pw.println();
//		pw.flush();
//	}

	/**
	 * Print command line options.
	 * @param options
	 */
	private static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: asetune [-C <cfgFile>] [-c <cfgFile>] [-t <filename>] [-h] [-v] ");
		pw.println("               [-U <user>]   [-P <passwd>]    [-S <server>]");
		pw.println("               [-u <ssUser>] [-p <sshPasswd>] [-s <sshHostname>]");
		pw.println("               [-n <cfgFile|cmNames>] [-r] [-l] [-i <seconds>] [-f <hours>]");
		pw.println("               [-d <dbname>] [-D <H2|ASE|ASE>] ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -C,--config <cfgName>     System Config file");
		pw.println("  -c,--userConfig <cfgName> User Config file, overrides values in System cfg.");
		pw.println("  -t,--tmpConfig <filename> Config file where temporary stuff are stored.");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
		pw.println("                            To get available option, do -x list");
		pw.println("  ");
		pw.println("  -U,--user <user>          Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd");
		pw.println("  -S,--server <server>      Server to connect to.");
		pw.println("  ");
		pw.println("  -u,--sshUser <user>       SSH Username, used by Host Monitoring subsystem.");
		pw.println("  -p,--sshPasswd <passwd>   SSH Password, used by Host Monitoring subsystem.");
		pw.println("  -s,--sshServer <host>     SSH Hostname, used by Host Monitoring subsystem.");
		pw.println("  ");
		pw.println("  Switches for offline mode:");
		pw.println("  -n,--noGui <cfgFile|cmNames> Do not start with GUI.");
		pw.println("                            instead collect counters to a database.");
		pw.println("                            cfgFile = <a config file for offline sample>");
		pw.println("                                      which can be generated with the wizard.");
		pw.println("                            cmNames = <small|medium|large|all>.");
		pw.println("                                      or a comma separated list on CMNames.");
		pw.println("  -r,--reconfigure          If the monitored ASE is not properly configured,");
		pw.println("                            then try to configure it using predefined values.");
		pw.println("  -l,--listcm               List cm's that can be used in '-n' switch.");
		pw.println("  -i,--interval <seconds>   sample Interval, time between samples.");
		pw.println("  -f,--finish   <hours>     shutdown/stop the no-gui service after # hours.");
		pw.println("  -D,--dbtype <H2|ASE|ASE>  type of database to use as offline store.");
		pw.println("  -d,--dbname <connSpec>    Connection specification to store offline data.");
		pw.println("                            Depends on -D, see below for more info.");
		pw.println("");
		pw.println("If you specify '-D and -d':");
		pw.println("  -D H2  -d h2dbfile");
		pw.println("  -D ASE -d hostname:port:dbname:user:passwd");
		pw.println("  -D ASA -d hostname:port:dbname:user:passwd");
		pw.println("  Default connection specifications for different dbtypes:");
		pw.println("     H2:  'h2dbfile' file is mandatory");
		pw.println("     ASE: 'hostname:port:dbname:user:passwd' is all mandatory.");
		pw.println("     ASA: port=2638, dbname='', user='DBA', passwd='SQL'");
		pw.println("  If only '-d' is given, the default value for '-D' is 'H2'.");
		pw.println("");
		pw.flush();
	}

	/**
	 * Build the options parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options parser.
	 */
	private static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( "C", "config",      true,  "The System Config file" );
		options.addOption( "c", "userConfig",  true,  "The User Config file where, overrides values in System Config." );
		options.addOption( "t", "tmpConfig",   true,  "Config file where temporary stuff are stored." );
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display "+Version.getAppName()+" and JVM Version." );
		options.addOption( "x", "debug",       true,  "Debug options: a comma separated string dbg1,dbg2,dbg3" );

		options.addOption( "U", "user",        true, "Username when connecting to server." );
		options.addOption( "P", "passwd",      true, "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",      true, "Server to connect to." );

		options.addOption( "u", "sshUser",     true, "SSH Username when connecting to server for Host Monitoring." );
		options.addOption( "p", "sshPasswd",   true, "SSH Password when connecting to server for Host Monitoring." );
		options.addOption( "s", "sshServer",   true, "SSH Hostname to connect to." );

		options.addOption( "n", "noGui",       true, "Do not start with GUI, instead collect counters to a database <a config file for offline sample>." );
		options.addOption( "r", "reconfigure", false,"Offline: if monitored ASE is not properly configured, try to configure it." );
		options.addOption( "l", "listcm",      false,"Offline: List cm's that can be used in '-n' switch." );
		options.addOption( "i", "interval",    true, "Offline: Sample Interval, time between samples." );
		options.addOption( "D", "dbtype",      true, "Offline: {H2|ASE|ASA} database type default is H2." );
		options.addOption( "d", "dbname",      true, "Offline: dbname/file to store offline samples." );
		options.addOption( "f", "finish",      true, "Offline: Shutdown the NO-GUI service after # hours" );

//		options.addOption(OptionBuilder.withLongOpt("define").withDescription("define a system property").hasArg(true).withArgName("name=value").create('D'));
//		options.addOption(OptionBuilder.hasArg(false).withDescription("usage information").withLongOpt("help").create('h'));
//		options.addOption(OptionBuilder.hasArg(false).withDescription("debug mode will print out full stack traces").withLongOpt("debug").create('d'));
//		options.addOption(OptionBuilder.hasArg(false).withDescription("display the Groovy and JVM versions").withLongOpt("version").create('v'));
//		options.addOption(OptionBuilder.withArgName("charset").hasArg().withDescription("specify the encoding of the files").withLongOpt("encoding").create('c'));
//		options.addOption(OptionBuilder.withArgName("script").hasArg().withDescription("specify a command line script").create('e'));
//		options.addOption(OptionBuilder.withArgName("extension").hasOptionalArg().withDescription("modify files in place; create backup if extension is given (e.g. \'.bak\')").create('i'));
//		options.addOption(OptionBuilder.hasArg(false).withDescription("process files line by line using implicit 'line' variable").create('n'));
//		options.addOption(OptionBuilder.hasArg(false).withDescription("process files line by line and print result (see also -n)").create('p'));
//		options.addOption(OptionBuilder.withArgName("port").hasOptionalArg().withDescription("listen on a port and process inbound lines").create('l'));
//		options.addOption(OptionBuilder.withArgName("splitPattern").hasOptionalArg().withDescription("split lines using splitPattern (default '\\s') using implicit 'split' variable").withLongOpt("autosplit").create('a'));

		return options;
	}


	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line parser
		CommandLineParser parser = new PosixParser();	
	
		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		// Validate any mandatory options or dependencies of switches
		
		// validate that -N has been specified if -noGui
//		if( cmd.hasOption( "noGui" ) ) 
//		{
//			if ( ! cmd.hasOption("noGuiConfig") )
//				throw new ParseException("You have specified -noGui but not any -noGuiConfig option.");
//		}

		if (_logger.isDebugEnabled())
		{
			for (@SuppressWarnings("unchecked") Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}

	private static void printTemplate(String templateName)
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
	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
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
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start AseTune, GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				new AseTune(cmd);
			}
		}
		catch (ParseException pe)
		{
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
		}
	}
}

class NormalExitException
extends Exception
{
	private static final long serialVersionUID = 1L;
	public NormalExitException()
	{
		super();
	}
	public NormalExitException(String msg)
	{
		super(msg);
	}
}
