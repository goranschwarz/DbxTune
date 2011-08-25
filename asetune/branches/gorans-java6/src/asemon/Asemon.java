/**
 */
package asemon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;

import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import asemon.check.CheckForUpdates;
import asemon.gui.GuiLogAppender;
import asemon.gui.MainFrame;
import asemon.gui.SplashWindow;
import asemon.utils.AseConnectionFactory;
import asemon.utils.Configuration;
import asemon.utils.JavaVersion;
import asemon.utils.Logging;
import asemon.utils.StringUtil;
import asemon.utils.SwingExceptionHandler;
import asemon.utils.SwingUtils;

public class Asemon
{
	private static Logger _logger = Logger.getLogger(Asemon.class);

//	private boolean	     packFrame	= false;
//	public static String	java_version;

	/** This can be either GUI or NO-GUI "collector" */
	private static GetCounters   getCnt          = null;

	//static long mainThreadId = -1;

	private static boolean _gui = true;
//	private static CommandLine _cmdLine = null;
	
	public Asemon(CommandLine cmd)
	throws Exception
	{

		// Kick it off
		init(cmd);
	}

	/**
	 * Initialize and start AseMon
	 * @param propFile
	 * @param savePropFile
	 * @throws Exception
	 */
	public void init(CommandLine cmd)
	throws Exception
	{
//		_cmdLine = cmd;

		// Create store dir if it did not exists.
		File asemonStoreDir = new File(Version.APP_STORE_DIR);
		if ( ! asemonStoreDir.exists() )
		{
			if (asemonStoreDir.mkdir())
				System.out.println("Creating directory '"+asemonStoreDir+"' to hold various files for AseMon.");
		}

		
		// -----------------------------------------------------------------
		// CHECK/SETUP information from the CommandLine switches
		// -----------------------------------------------------------------
		final String CONFIG_FILE_NAME     = System.getProperty("CONFIG_FILE_NAME",     "asemon.properties");
		final String TMP_CONFIG_FILE_NAME = System.getProperty("TMP_CONFIG_FILE_NAME", "asemon.save.properties");
		final String ASEMON_HOME          = System.getProperty("ASEMON_HOME");
		
		String defaultPropsFile    = (ASEMON_HOME           != null) ? ASEMON_HOME           + "/" + CONFIG_FILE_NAME     : CONFIG_FILE_NAME;
		String defaultTmpPropsFile = (Version.APP_STORE_DIR != null) ? Version.APP_STORE_DIR + "/" + TMP_CONFIG_FILE_NAME : TMP_CONFIG_FILE_NAME;

		// Compose MAIN CONFIG file (first USER_HOME then ASEMON_HOME)
		String filename = Version.APP_STORE_DIR + "/" + CONFIG_FILE_NAME;
		if ( (new File(filename)).exists() )
			defaultPropsFile = filename;

		String propFile        = cmd.getOptionValue("config",    defaultPropsFile);
		String tmpPropFile     = cmd.getOptionValue("tmpConfig", defaultTmpPropsFile);
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
			System.out.println(" Asemon needs a runtime JVM 1.6 or higher.");
			System.out.println(" java.version = " + System.getProperty("java.version"));
			System.out.println(" which is parsed into the number: " + JavaVersion.getVersion());
			System.out.println("---------------------------------------------------------------");
			System.out.println("");
			throw new Exception("Asemon needs a runtime JVM 1.6 or higher.");
		}

		// The SAVE Properties...
		Configuration asemonSaveProps = new Configuration(tmpPropFile);
		Configuration.setInstance(Configuration.TEMP, asemonSaveProps);

		// Get the "OTHER" properties that has to do with LOGGING etc...
		Configuration asemonProps = new Configuration(propFile);
		Configuration.setInstance(Configuration.CONF, asemonProps);

		// The Store Properties...
		Configuration storeConfigProps = new Configuration();
		Configuration.setInstance(Configuration.PCS, storeConfigProps);

		// Put command line properties into the Configuration
		if ( _gui )
		{
			if (cmd.hasOption('U'))	asemonProps.setProperty("cmdLine.aseUsername", cmd.getOptionValue('U'));
			if (cmd.hasOption('P'))	asemonProps.setProperty("cmdLine.asePassword", cmd.getOptionValue('P'));
			if (cmd.hasOption('S'))	asemonProps.setProperty("cmdLine.aseServer",   cmd.getOptionValue('S'));

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
	
			// -s, --sampleTime: Offline: time between samples.
			if (cmd.hasOption('s'))	storeConfigProps.setProperty("offline.sampleTime", cmd.getOptionValue('s'));

			// -d --h2dbname: Offline: dbname to store offline samples.
			if (cmd.hasOption('d'))
			{
				String opt = cmd.getOptionValue('d');
				_logger.info("Command Line Option '-d|--h2dbname' was specified, dbname to use is '"+opt+"'.");
	
				storeConfigProps.setProperty("PersistWriterJdbc.jdbcDriver",           "org.h2.Driver");
				storeConfigProps.setProperty("PersistWriterJdbc.jdbcPassword",         "");
				storeConfigProps.setProperty("PersistWriterJdbc.jdbcUrl",              "jdbc:h2:file:"+opt);
				storeConfigProps.setProperty("PersistWriterJdbc.jdbcUsername",         "sa");
				storeConfigProps.setProperty("PersistWriterJdbc.startH2NetworkServer", true);
	
				storeConfigProps.setProperty("PersistentCounterHandler.WriterClass", "asemon.pcs.PersistWriterJdbc");
			}
		}

		
//		// Check if we are in NO-GUI mode
//		_gui = System.getProperty("asemon.gui", "true").trim().equalsIgnoreCase("true");

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
			httpProxyHost = asemonSaveProps.getProperty("http.proxyHost");
		if (httpProxyPort == null)
			httpProxyPort = asemonSaveProps.getProperty("http.proxyPort");

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
			ToolTipManager.sharedInstance().setDismissDelay(120*1000); // 2 minutes
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
		
		_logger.info("Using configuration file '"+propFile+"'.");
		_logger.info("Storing temporary configurations in file '"+tmpPropFile+"'.");


		// check if sufficient memory has been configured.
		// [FIXME] what is appropriate default value here.
		String needMemInMBStr = "32";
		int maxConfigMemInMB = (int) Runtime.getRuntime().maxMemory() / 1024 / 1024; // jdk 1.4 or higher
		int needMemInMB = Integer.parseInt( asemonProps.getProperty("minMemoryLimitInMB", needMemInMBStr) );
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

			_logger.info("This TEMPORARY DEVELOPMENT VERSION will NOT work after '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'.");
			if ( System.currentTimeMillis() > Version.DEV_VERSION_EXPIRE_DATE.getTime() )
			{
				String msg = "This TEMPORARY DEVELOPMENT VERSION has expired. The trial period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'. A new version can be downloaded here 'http://www.asemon.se'";
				_logger.error(msg);
				Exception ex = new Exception(msg);
				if (_gui)
				{
					SwingUtils.showErrorMessage("AseMon - This TEMPORARY DEVELOPMENT VERSION has expired", 
							"This TEMPORARY DEVELOPMENT VERSION has expired.\n\n" +
							"The trial period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"'. \n" +
							"A new version can be downloaded here 'http://www.asemon.se'",
							ex);
				}
				throw ex;
			}
		}

		if ( ! _gui )
		{
			_logger.info("Starting asemon in NO-GUI mode, counters will be sampled into a database.");

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
			_logger.info("Starting asemon in GUI mode.");

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
//		formatter.printHelp(pw, 80, "asemon", "options:", options, 2, 4, 
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

		pw.println("usage: asemon [-c <cfgFile>] [-t <filename>] [-h] [-v] ");
		pw.println("              [-U <user>] [-P <passwd>] [-S <server>]");
		pw.println("              [-n <cfgFile|cmNames>] [-s <seconds>] [-d <dbname>] ");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -c,--config <cfgName>     The Main Config file");
		pw.println("  -t,--tmpConfig <filename> Config file where temporary stuff are stored.");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display AseMon and JVM Version.");
		pw.println("  ");
		pw.println("  -U,--user <user>          Username when connecting to server.");
		pw.println("  -P,--passwd <passwd>      Password when connecting to server. (null=noPasswd)");
		pw.println("  -S,--server <server>      Server to connect to.");
		pw.println("  ");
		pw.println("  Switches for offline mode:");
		pw.println("  -n,--noGui <cfgFile|cmNames> Do not start with GUI.");
		pw.println("                            instead collect counters to a database.");
		pw.println("                            cfgFile = <a config file for offline sample>");
		pw.println("                                      which can be generated with the wizard.");
		pw.println("                            cmNames = <small|medium|large|all>.");
		pw.println("                                      or a comma separated list on CMNames.");
		pw.println("  -s,--sampleTime <seconds> time between samples.");
		pw.println("  -d,--h2dbname <dbname>    H2 dbname/file to store offline samples.");
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
		options.addOption( "c", "config",      true,  "The Main Config file" );
		options.addOption( "t", "tmpConfig",   true,  "Config file where temporary stuff are stored." );
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display AseMon and JVM Version." );

		options.addOption( "U", "user",        true, "Username when connecting to server." );
		options.addOption( "P", "passwd",      true, "Password when connecting to server. (null=noPasswd)" );
		options.addOption( "S", "server",      true, "Server to connect to." );

		options.addOption( "n", "noGui",       true,  "Do not start with GUI, instead collect counters to a database <a config file for offline sample>." );
		options.addOption( "s", "sampleTime",  true, "Offline: time between samples." );
		options.addOption( "d", "h2dbname",    true, "Offline: H2 dbname/file to store offline samples." );
//		options.addOption( "m", "cmNames",     true, "Offline: names of CM's to sample, use 'all' for everything." );

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

		return cmd;
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

			if ( cmd.hasOption("help") )
			{
				printHelp(options, "The option '--help' was passed.");
			}
			else if ( cmd.hasOption("version") )
			{
				System.out.println();
				System.out.println(Version.getAppName()+" Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			else
			{
				new Asemon(cmd);
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
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
