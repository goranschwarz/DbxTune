/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.central;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.HtmlEmail;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;

import com.asetune.AppDir;
import com.asetune.DbxTune;
import com.asetune.NormalExitException;
import com.asetune.Version;
import com.asetune.alarm.AlarmHandler;
import com.asetune.alarm.writers.AlarmWriterToMail;
import com.asetune.central.check.ReceiverAlarmCheck;
import com.asetune.central.cleanup.CentralDailyReportSender;
import com.asetune.central.cleanup.CentralH2Defrag;
import com.asetune.central.cleanup.CentralPcsJdbcCleaner;
import com.asetune.central.cleanup.DataDirectoryCleaner;
import com.asetune.central.pcs.CentralPcsWriterHandler;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.central.pcs.CentralPersistWriterJdbc;
import com.asetune.central.pcs.DbxCentralRealm;
import com.asetune.central.pcs.H2WriterStatCronTask;
import com.asetune.check.CheckForUpdates;
import com.asetune.check.CheckForUpdatesDbxCentral;
import com.asetune.gui.GuiLogAppender;
import com.asetune.pcs.PersistReader;
import com.asetune.pcs.report.senders.ReportSenderToMail;
import com.asetune.utils.Configuration;
import com.asetune.utils.CronUtils;
import com.asetune.utils.Debug;
import com.asetune.utils.JavaUtils;
import com.asetune.utils.Logging;
import com.asetune.utils.Memory;
import com.asetune.utils.NetUtils;
import com.asetune.utils.ShutdownHandler;
import com.asetune.utils.StringUtil;

import it.sauronsoftware.cron4j.Scheduler;

public class DbxTuneCentral
{
//	private final static Logger _logger = LoggerFactory.getLogger(CentralPcsReceiverController.class);
	private static Logger _logger = Logger.getLogger(DbxTuneCentral.class);

	public static final String PROPKEY_WEB_PORT = "DbxTuneCentral.web.port";
	public static final int    DEFAULT_WEB_PORT = 8080;
	
//	public static String getAppName()           { return "DbxTuneCentral"; }
	public static String getAppHomeEnvName()    { return "DBXTUNE_HOME"; }
	public static String getAppSaveDirEnvName() { return "DBXTUNE_SAVE_DIR"; }

	private static org.h2.tools.Server _h2TcpServer = null;
	private static org.h2.tools.Server _h2WebServer = null;
	private static org.h2.tools.Server _h2PgServer  = null;
	
    private static Server _server = null;

    private static Scheduler _scheduler = null;

//	public static void main(String[] args) 
//	{
//		SpringApplication.run(DbxTuneCentralApplication.class, args);
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

		pw.println("usage: "+Version.getAppName()+" [-h] [-v] [-x]");
		pw.println("              [-C <cfgfile>] [-L <logfile>] [-H <dirname>] [-R <dirname>] [-D <key=val>]");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
		pw.println("                            To get available option, do -x list");
		pw.println("  -a,--createAppDir         Create application dir (~/.dbxtune) and exit.");
		pw.println("  ");
		pw.println("  -C,--config  <filename>   System Config file.");
		pw.println("  -L,--logfile <filename>   Name of the logfile where application logging is saved.");
		pw.println("  -H,--homedir <dirname>    HOME Directory, where all personal files are stored.");
		pw.println("  -R,--savedir <dirname>    DBXTUNE_SAVE_DIR, where H2 Database recordings are stored.");
		pw.println("  -D,--javaSystemProp <k=v> set Java System Property, same as java -Dkey=value");
		pw.println("  ");
		pw.flush();

//		pw.println("usage: "+Version.getAppName()+" [-C <cfgFile>] [-c <cfgFile>] [-t <filename>] [-h] [-v] ");
//		pw.println("              [-U <user>]   [-P <passwd>]    [-S <server>]");
//		pw.println("              [-u <ssUser>] [-p <sshPasswd>] [-s <sshHostname>] ");
//		pw.println("              [-L <logfile>] [-H <dirname>] [-R <dirname>] [-D <key=val>]");
//		pw.println("              [-n <cfgFile|cmNames>] [-r] [-l] [-i <seconds>] [-f <hours>]");
//		pw.println("              [-d <dbname>] [-T <H2|ASE|ASE>] ");
//		pw.println("  ");
//		pw.println("options:");
//		pw.println("  -C,--config <cfgName>     System Config file");
//		pw.println("  -c,--userConfig <cfgName> User Config file, overrides values in System cfg.");
//		pw.println("  -t,--tmpConfig <filename> Config file where temporary stuff are stored.");
//		pw.println("  -h,--help                 Usage information.");
//		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
//		pw.println("  -x,--debug <dbg1,dbg2>    Debug options: a comma separated string");
//		pw.println("                            To get available option, do -x list");
//		pw.println("  ");
//		pw.println("  -U,--user <user>          Username when connecting to server.");
//		pw.println("  -P,--passwd <passwd>      Password when connecting to server. null=noPasswd");
//		pw.println("  -S,--server <server>      Server to connect to.");
//		pw.println("  ");
//		pw.println("  -u,--sshUser <user>       SSH Username, used by Host Monitoring subsystem.");
//		pw.println("  -p,--sshPasswd <passwd>   SSH Password, used by Host Monitoring subsystem.");
//		pw.println("  -s,--sshServer <host>     SSH Hostname, used by Host Monitoring subsystem.");
//		pw.println("  ");
//		pw.println("  -L,--logfile <filename>   Name of the logfile where application logging is saved.");
//		pw.println("  -H,--homedir <dirname>    HOME Directory, where all personal files are stored.");
//		pw.println("  -R,--savedir <dirname>    DBXTUNE_SAVE_DIR, where H2 Database recordings are stored.");
//		pw.println("  -D,--javaSystemProp <k=v> set Java System Property, same as java -Dkey=value");
//		pw.println("  ");
//		pw.println("  Switches for offline mode:");
//		pw.println("  -n,--noGui <cfgFile|cmNames> Do not start with GUI.");
//		pw.println("                            instead collect counters to a database.");
//		pw.println("                            cfgFile = <a config file for offline sample>");
//		pw.println("                                      which can be generated with the wizard.");
//		pw.println("                            cmNames = <small|medium|large|all>.");
//		pw.println("                                      or a comma separated list on CMNames.");
//		pw.println("                            You can also combine a template and add/remove cm's.");
//		pw.println("                            Example: -n medium,-CmDeviceIo,+CmSpinlockSum");
//		pw.println("  -r,--reconfigure          If the monitored ASE is not properly configured,");
//		pw.println("                            then try to configure it using predefined values.");
//		pw.println("  -l,--listcm               List cm's that can be used in '-n' switch.");
//		pw.println("  -i,--interval <seconds>   sample Interval, time between samples.");
//		pw.println("  -e,--enable   <hh[:mm]>   enable/start the recording at Hour(00-23) Minute(00-59)");
//		pw.println("  -f,--finish   <hh[:mm]>   shutdown/stop the no-gui service after # hours.");
//		pw.println("  -T,--dbtype <H2|ASE|ASA>  type of database to use as offline store.");
//		pw.println("  -d,--dbname <connSpec>    Connection specification to store offline data.");
//		pw.println("                            Depends on -T, see below for more info.");
//		pw.println("");
//		pw.println("If you specify '-T and -d':");
//		pw.println("  -T H2  -d h2dbfile|default");
//		pw.println("  -T ASE -d hostname:port:dbname:user:passwd");
//		pw.println("  -T ASA -d hostname:port:dbname:user:passwd");
//		pw.println("  Default connection specifications for different dbtypes:");
//		pw.println("     H2:  'h2dbfile' file is mandatory.");
//		pw.println("          Use: -d default, will set h2dbfile to '${<DBXTUNE>_SAVE_DIR}/${SERVERNAME}_${DATE}'");
//		pw.println("     ASE: 'hostname:port:dbname:user:passwd' is all mandatory.");
//		pw.println("     ASA: port=2638, dbname='', user='DBA', passwd='SQL'");
//		pw.println("  If only '-d' is given, the default value for '-T' is 'H2'.");
//		pw.println("");
//		pw.flush();
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

		// create the Options
		options.addOption( Option.builder("C").longOpt("config"        ).hasArg(true ).build() );
//		options.addOption( Option.builder("c").longOpt("userConfig"    ).hasArg(true ).build() );
//		options.addOption( Option.builder("t").longOpt("tmpConfig"     ).hasArg(true ).build() );
		options.addOption( Option.builder("h").longOpt("help"          ).hasArg(false).build() );
		options.addOption( Option.builder("v").longOpt("version"       ).hasArg(false).build() );
		options.addOption( Option.builder("x").longOpt("debug"         ).hasArg(true ).build() );

		options.addOption( Option.builder("a").longOpt("createAppDir"  ).hasArg(false).build() );

//		options.addOption( Option.builder("U").longOpt("user"          ).hasArg(true ).build() );
//		options.addOption( Option.builder("P").longOpt("passwd"        ).hasArg(true ).build() );
//		options.addOption( Option.builder("S").longOpt("server"        ).hasArg(true ).build() );
//
//		options.addOption( Option.builder("u").longOpt("sshUser"       ).hasArg(true ).build() );
//		options.addOption( Option.builder("p").longOpt("sshPasswd"     ).hasArg(true ).build() );
//		options.addOption( Option.builder("s").longOpt("sshServer"     ).hasArg(true ).build() );

		options.addOption( Option.builder("L").longOpt("logfile"       ).hasArg(true ).build() );
		options.addOption( Option.builder("H").longOpt("homedir"       ).hasArg(true ).build() );
		options.addOption( Option.builder("R").longOpt("savedir"       ).hasArg(true ).build() );
		options.addOption( Option.builder("D").longOpt("javaSystemProp").hasArgs().valueSeparator('=').build() ); // NOTE the hasArgs() instead of hasArg() *** the 's' at the end of hasArg<s>() does the trick...

//		options.addOption( Option.builder("n").longOpt("noGui"         ).hasArg(true ).build() );
//		options.addOption( Option.builder("r").longOpt("reconfigure"   ).hasArg(false).build() );
//		options.addOption( Option.builder("l").longOpt("listcm"        ).hasArg(false).build() );
//		options.addOption( Option.builder("i").longOpt("interval"      ).hasArg(true ).build() );
//		options.addOption( Option.builder("T").longOpt("dbtype"        ).hasArg(true ).build() );
//		options.addOption( Option.builder("d").longOpt("dbname"        ).hasArg(true ).build() );
//		options.addOption( Option.builder("f").longOpt("finish"        ).hasArg(true ).build() );
//		options.addOption( Option.builder("e").longOpt("enable"        ).hasArg(true ).build() );

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

	public static String getConfigFileName()  { return "dbxTuneCentral.properties"; }
//	public static String getConfigFileName()  { return "conf" + File.separatorChar + "dbxTuneCentral.properties"; }

	public static void init(CommandLine cmd)
	throws Exception
	{
		// Create store dir if it did not exists.
		List<String> crAppDirLog = AppDir.checkCreateAppDir( null, System.out );

		
		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME", getConfigFileName());

		final String APP_HOME              = System.getProperty(getAppHomeEnvName());

		String defaultPropsFile = (APP_HOME != null) ? APP_HOME + File.separator + CONFIG_FILE_NAME : CONFIG_FILE_NAME;

		String propFile = cmd.getOptionValue("config", defaultPropsFile);

		if ( cmd.hasOption("homedir") )
			System.setProperty("user.home", cmd.getOptionValue("homedir"));

		if ( cmd.hasOption("savedir") )
			System.setProperty("DBXTUNE_SAVE_DIR", cmd.getOptionValue("savedir"));
		
		// Take all Environment variables and add them as System Properties
		// But: Do NOT overwrite already set System Properties with Environment variables
		StringUtil.setEnvironmentVariablesToSystemProperties(false, false);  

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
		
		// Should we set this to headless... so that we throws "headless exception" if accessing GUI parts
		System.setProperty("java.awt.headless", "true");

		
		// Check if we have a specific LOG filename before we initialize the logger...
		String logFilename = null;
		if (cmd.hasOption('L'))
		{
			String opt = cmd.getOptionValue('L');
			logFilename = opt;
		}
		else
		{
			logFilename = getAppLogDir() + File.separatorChar + "log" + File.separatorChar + Version.getAppName()+".log";
		}

//System.setProperty("log.console.debug", "true");
		// Initialize the log file
//		String propFile = null;
		Logging.init(null, propFile, logFilename);

		// Always register the "gui" LogAppender, which will also send errors to 'dbxtune.com'
		// Calling this would make GuiLogAppender, to register itself in log4j.
		GuiLogAppender.getInstance();

		// Print out the memory configuration
		// And the JVM info
		_logger.info("Starting "+Version.getAppName()+", version "+Version.getVersionStr()+", build "+Version.getBuildStr());
//		_logger.info("GUI mode "+_gui);
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
//		_logger.info("User configuration file is '"+userPropFile+"'.");
//		_logger.info("Storing temporary configurations in file '"+tmpPropFile+"'.");
		_logger.info("Combined Configuration Search Order '"+StringUtil.toCommaStr(Configuration.getSearchOrder())+"'.");

		if (crAppDirLog != null && !crAppDirLog.isEmpty())
		{
			_logger.info("Below messages was created earlier by 'check/create application directory'.");
			for (String msg : crAppDirLog)
				_logger.info(msg);
		}

		
		// If the config file exists, read it, if not create empty
		if ( (new File(propFile)).exists() )
		{
			_logger.info("Using configuration file '"+propFile+"'.");
			Configuration appProps = new Configuration(propFile);
			Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);
		}
		else
		{
			_logger.info("The configuration file '"+propFile+"', did not exists, starting with an empty configuration.");
			Configuration appProps = new Configuration();
			Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);
		}


		_logger.info("getHomeDir(): "+getAppHomeDir());
		_logger.info("getDataDir(): "+getAppDataDir());
		_logger.info("getConfDir(): "+getAppConfDir());
		_logger.info("getLogDir():  "+getAppLogDir());
		_logger.info("getInfoDir(): "+getAppInfoDir());
		_logger.info("getWebDir():  "+getAppWebDir());
		

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
				//_hasDevVersionExpired = true;

				String msg = "This DEVELOPMENT VERSION has expired. (version='"+Version.getVersionStr()+"', buildStr='"+Version.getBuildStr()+"'). The \"time to live\" period ended at '"+df.format(Version.DEV_VERSION_EXPIRE_DATE)+"', and the current date is '"+df.format(new Date())+"'. A new version can be downloaded here 'http://www.dbxtune.com'";
				_logger.error(msg);
				throw new Exception(msg);
			}
		}

		// Install a "CheckForUpdate" instance
		CheckForUpdates.setInstance( new CheckForUpdatesDbxCentral() );

		//---------------------------------
		// Go and check for updates, before continuing (timeout 10 seconds)
		// This has to be done before: startCentralPcs()
		//---------------------------------
		_logger.info("Checking for new release...");
//		CheckForUpdates.blockCheck(10*1000);
		CheckForUpdates.getInstance().checkForUpdateBlock(10*1000);
		if (CheckForUpdates.getInstance().hasUpgrade())
		{
			sendDbxTuneUpdateMail();
		}

		// Start the Persistant Counter Service
		startCentralPcs();

		// Start the Alarm Handler
		startAlarmHandler();

		// Start Receiver Alarm Checker
		startReceiverAlarmChecker();
		
		// Start the Web Server
		startWebServer();

		// Start scheduler
		startScheduler();

		// 
		// Create a file, that will be deleted when the process ends.
		// This file will hold various configuration about the NO-GUI process
		// This is used by DBXTUNE Central...
		//
		boolean writeDbxTuneServiceFile = true;
		if (writeDbxTuneServiceFile)
			writeDbxTuneServiceFile();
		
		
		// Create a statistics object that will be used by various subsystems to
		// track how many times we doe stuff...
		// This will later on be sent to dbxtune.com, just as we do with checking for updates etc.
		DbxCentralStatistics.getInstance(); // Not really needed, but lets do it anyway

//Fix the below to work better
//- Implement a CheckForUpdate for the DbxCentral instance
//DONE: - Check for new release should: mail (using {sendReport|sendAlarm}ToMail) properties (if we can find any)
//DONE: - Shutdown hook, should be in current shutdown
//- A scheduler task that sends in Usage behaviour at HH:mm every day (or when doing "dailyReport")
//- The ServerSide of "checkForUpdate" probably needs some new table(s) to handle the DbxCentral Statistics

// Statistics: shutdown reason
		
		//---------------------------------
		// Install shutdown hook -- SEND Counter Usage - add it as FIRST (index: 0)
//		ShutdownHandler.addShutdownHandler(0, new ShutdownHandler.Shutdownable()
//		{
//			@Override
//			public List<String> systemShutdown()
//			{
////System.out.println("----Start Shutdown Hook: sendCounterUsageInfo");
//				_logger.debug("----Start Shutdown Hook: sendCounterUsageInfo");
//
//				DbxCentralStatistics stat = DbxCentralStatistics.getInstance();
//
//				stat.setShutdownReason  ( ShutdownHandler.getShutdownReason()   );
//				stat.setRestartSpecified( ShutdownHandler.wasRestartSpecified() );
//				
//				// Send the statistics object to dbxtune.com
//				CheckForUpdates.getInstance().sendCounterUsageInfo(true, stat);
//				
//				_logger.debug("----End Shutdown Hook: sendCounterUsageInfo");
////System.out.println("----End Shutdown Hook: sendCounterUsageInfo");
//				
//				return null;
//			}
//		});
	}
	
	private static void sendDbxTuneUpdateMail()
	{
		CheckForUpdates cfu = CheckForUpdates.getInstance();
		Configuration conf = Configuration.getCombinedConfiguration();

		_logger.info(Version.getAppName() + " new Upgrade is Available. New version is '"+cfu.getNewAppVersionStr()+"' and can be downloaded here '"+cfu.getDownloadUrl()+"'.");
		
		String msgSubject = Version.getAppName() + " new Upgrade is Available";
		String msgBody = "<html>" +
				Version.getAppName() + " new Upgrade is Available. <br>" + 
				"New version is '"+cfu.getNewAppVersionStr()+"'.<br>" +
				"And can be downloaded here: <a href='"+cfu.getDownloadUrl()+"'>"+cfu.getDownloadUrl()+"</a><br>" +
				"<hr>" +
				"Whats new: <a href='"+cfu.getWhatsNewUrl()+"'>"+cfu.getWhatsNewUrl()+"</a>" +
				"</html>";
		
		String  smtpHostname       = conf.getProperty       (AlarmWriterToMail.PROPKEY_smtpHostname,           AlarmWriterToMail.DEFAULT_smtpHostname);
		String  toCsv              = conf.getProperty       (AlarmWriterToMail.PROPKEY_to,                     AlarmWriterToMail.DEFAULT_to);
		String  ccCsv              = conf.getProperty       (AlarmWriterToMail.PROPKEY_cc,                     AlarmWriterToMail.DEFAULT_cc);
		String  from               = conf.getProperty       (AlarmWriterToMail.PROPKEY_from,                   AlarmWriterToMail.DEFAULT_from);

		String  username           = conf.getProperty       (AlarmWriterToMail.PROPKEY_username,               AlarmWriterToMail.DEFAULT_username);
		String  password           = conf.getProperty       (AlarmWriterToMail.PROPKEY_password,               AlarmWriterToMail.DEFAULT_password);
		int     smtpPort           = conf.getIntProperty    (AlarmWriterToMail.PROPKEY_smtpPort,               AlarmWriterToMail.DEFAULT_smtpPort);
		int     sslPort            = conf.getIntProperty    (AlarmWriterToMail.PROPKEY_sslPort,                AlarmWriterToMail.DEFAULT_sslPort);
		boolean useSsl             = conf.getBooleanProperty(AlarmWriterToMail.PROPKEY_useSsl,                 AlarmWriterToMail.DEFAULT_useSsl);
		int     smtpConnectTimeout = conf.getIntProperty    (AlarmWriterToMail.PROPKEY_connectionTimeout,      AlarmWriterToMail.DEFAULT_connectionTimeout);
		
		if (StringUtil.isNullOrBlank(smtpHostname))
		{
			smtpHostname       = conf.getProperty       (ReportSenderToMail.PROPKEY_smtpHostname,           ReportSenderToMail.DEFAULT_smtpHostname);
			toCsv              = conf.getProperty       (ReportSenderToMail.PROPKEY_to,                     ReportSenderToMail.DEFAULT_to);
			ccCsv              = conf.getProperty       (ReportSenderToMail.PROPKEY_cc,                     ReportSenderToMail.DEFAULT_cc);
			from               = conf.getProperty       (ReportSenderToMail.PROPKEY_from,                   ReportSenderToMail.DEFAULT_from);

			username           = conf.getProperty       (ReportSenderToMail.PROPKEY_username,               ReportSenderToMail.DEFAULT_username);
			password           = conf.getProperty       (ReportSenderToMail.PROPKEY_password,               ReportSenderToMail.DEFAULT_password);
			smtpPort           = conf.getIntProperty    (ReportSenderToMail.PROPKEY_smtpPort,               ReportSenderToMail.DEFAULT_smtpPort);
			sslPort            = conf.getIntProperty    (ReportSenderToMail.PROPKEY_sslPort,                ReportSenderToMail.DEFAULT_sslPort);
			useSsl             = conf.getBooleanProperty(ReportSenderToMail.PROPKEY_useSsl,                 ReportSenderToMail.DEFAULT_useSsl);
			smtpConnectTimeout = conf.getIntProperty    (ReportSenderToMail.PROPKEY_connectionTimeout,      ReportSenderToMail.DEFAULT_connectionTimeout);
		}

		if (StringUtil.hasValue(smtpHostname) && StringUtil.hasValue(toCsv) && StringUtil.hasValue(from))
		{
			List<String> toList = StringUtil.commaStrToList(toCsv);

			List<String> ccList = new ArrayList<>();
			if (StringUtil.hasValue(ccCsv))
				ccList = StringUtil.commaStrToList(ccCsv);
			
			try
			{
				HtmlEmail email = new HtmlEmail();

				email.setHostName(smtpHostname);

				// Charset
				//email.setCharset(StandardCharsets.UTF_8.name());
				
				// Connection timeout
				if (smtpConnectTimeout >= 0)
					email.setSocketConnectionTimeout(smtpConnectTimeout);

				// SMTP PORT
				if (smtpPort >= 0)
					email.setSmtpPort(smtpPort);

				// USE SSL
				if (useSsl)
					email.setSSLOnConnect(useSsl);

				// SSL PORT
				if (useSsl && sslPort >= 0)
					email.setSslSmtpPort(sslPort+""); // Hmm why is this a String parameter?

				// AUTHENTICATION
				if (StringUtil.hasValue(username))
					email.setAuthentication(username, password);
				
				// add TO and CC
				for (String to : toList)
					email.addTo(to);

				for (String cc : ccList)
					email.addCc(cc);

				// FROM & SUBJECT
				email.setFrom(from);
				email.setSubject(msgSubject);

				// CONTENT HTML or PLAIN
				if (StringUtils.startsWithIgnoreCase(msgBody.trim(), "<html>"))
					email.setHtmlMsg(msgBody);
				else
					email.setTextMsg(msgBody);
//				email.setHtmlMsg(msgBodyHtml);
//				email.setTextMsg(msgBodyText);
				
//				System.out.println("About to send the following message: \n"+msgBody);
				if (_logger.isDebugEnabled())
				{
					_logger.debug("About to send the following message: \n"+msgBody);
				}

				// SEND
				email.send();

				_logger.info("Sent mail message: host='"+smtpHostname+"', to='"+toCsv+"', cc='"+ccCsv+"', subject='"+msgSubject+"'.");
			}
			catch (Exception ex)
			{
				_logger.error("Problems sending mail (host='"+smtpHostname+"', to='"+toCsv+"', cc='"+ccCsv+"', subject='"+msgSubject+"').", ex);
			}
		}
		else // not enough params to send mail.
		{
			if (StringUtil.hasValue(smtpHostname) && StringUtil.hasValue(toCsv) && StringUtil.hasValue(from))
			_logger.info(Version.getAppName() + " new Upgrade is Available. New version is '"+cfu.getNewAppVersionStr()+"' and can be downloaded here '"+cfu.getDownloadUrl()+"'.");
			_logger.info("Not enough email parameters to send mail. One of the following parameters are blank: smtpHostname='"+smtpHostname+"', to='"+toCsv+"', from='"+from+"'. Tried to be retrived from 'AlarmWriterToMail.*', or 'ReportSenderToMail.*'.");
		}
	}

	private static void sendCounterUsageInfo()
	{
		DbxCentralStatistics stat = DbxCentralStatistics.getInstance();

		stat.setShutdownReason  ( ShutdownHandler.getShutdownReason()   );
		stat.setRestartSpecified( ShutdownHandler.wasRestartSpecified() );
		
		// Send the statistics object to dbxtune.com
		CheckForUpdates.getInstance().sendCounterUsageInfo(true, stat);
	}

	private static void close()
	{
		try
		{
			stopWebServer();
			stopReceiverAlarmChecker();     // Do not send any "missing data from xxxTune collector"
			stopAlarmHandler();             // No more Alarms will be sent
			stopScheduler();                // No more "cron" jobs
			stopCentralPcs();               // if it's a H2 DB, it might take time to stop (if defrag/compress is requested)

			sendCounterUsageInfo();
		}
		catch(Exception ex)
		{
			_logger.warn("Problems when closing the system down.", ex);
		}
	}

	/** where is application started */
	public static String getAppHomeDir()
	{
		String dbx = System.getProperty("DBXTUNE_HOME", ".");
		String val = System.getProperty("DBXTUNE_CENTRAL_HOME", dbx);
	//	       val = System.getProperty("DBXTUNE_CENTRAL_BASE", val);
		
		return val;
	}
	/** Where is DbxTune Central collector Configuration directory located */
	public static String getAppConfDir()
	{
//		String dbx = System.getProperty("DBXTUNE_CONF_DIR", getAppHomeDir() + File.separator + "conf");
		String dbx = System.getProperty("DBXTUNE_CONF_DIR", AppDir.getAppStoreDir() + File.separator + "dbxc" + File.separator + "conf");
		String val = System.getProperty("DBXTUNE_CENTRAL_CONF_DIR", dbx);
		
		return val;
	}
	/** Where is DbxTune Central collector "data" or "save" directory located */
	public static String getAppDataDir()
	{
//		String dbx = System.getProperty("DBXTUNE_SAVE_DIR", getAppHomeDir() + File.separator + "data");
		String dbx = System.getProperty("DBXTUNE_SAVE_DIR", AppDir.getAppStoreDir() + File.separator + "dbxc" + File.separator + "data");
		String val = System.getProperty("DBXTUNE_CENTRAL_SAVE_DIR", dbx);
		       val = System.getProperty("DBXTUNE_DATA_DIR", val);
		       val = System.getProperty("DBXTUNE_CENTRAL_DATA_DIR", val);
		
		return val;
	}
	/** Where is DbxTune Central collector "log" directory located */
	public static String getAppLogDir()
	{
//		String dbx = System.getProperty("DBXTUNE_LOG_DIR", getAppHomeDir() + File.separator + "log");
		String dbx = System.getProperty("DBXTUNE_LOG_DIR", AppDir.getAppStoreDir() + File.separator + "dbxc" + File.separator + "log");
		String val = System.getProperty("DBXTUNE_CENTRAL_LOG_DIR", dbx);
		
		return val;
	}
	/** Where is DbxTune Central collector "info files" directory located */
	public static String getAppInfoDir()
	{
		String dbx = System.getProperty("DBXTUNE_INFO_DIR", AppDir.getAppStoreDir() + File.separator + "dbxc" + File.separator + "info");
		String val = System.getProperty("DBXTUNE_CENTRAL_INFO_DIR", dbx);
		
		return val;
	}
	/** Where is DbxTune Central Web "content" directory located */
	public static String getAppWebDir()
	{
		String val = System.getProperty("DBXTUNE_CENTRAL_WEB_DIR", getAppHomeDir() + File.separator + "resources/WebContent");
		
		return val;
	}
	/** Where is DbxTune Central collector "reports" directory located */
	public static String getAppReportsDir()
	{
//		String dbx = System.getProperty("DBXTUNE_REPORTS_DIR", getAppHomeDir() + File.separator + "reports");
		String dbx = System.getProperty("DBXTUNE_REPORTS_DIR", AppDir.getAppStoreDir() + File.separator + "dbxc" + File.separator + "reports");
		String val = System.getProperty("DBXTUNE_CENTRAL_REPORTS_DIR", dbx);
		
		return val;
	}

	/**
	 *  Add a shudown hook so we can shutdown in a gracefull way on "kill"
	 * 
	 * @return The installed Thread object, in case you want to call <code>Runtime.getRuntime().removeShutdownHook(hook)</code>
	 */
	private static void installShutdownHandler()
	{
		_logger.info("Installing shutdown hook, which will help us stop the system gracefully if we are killed.");

		// tell H2 to NOT run it's shutdown hook: THE SYSTEM property did NOT WORK
		//System.setProperty("h2.dbCloseOnExit", "false");
		//System.setProperty("h2.DB_CLOSE_ON_EXIT", "FALSE");

		// Setting this property so that CentralPersistenWriter can append DB_CLOSE_ON_EXIT on the URL when connectiong to H2
		System.setProperty("dbxtune.isShutdownHookInstalled", "true");

		ShutdownHandler.addShutdownHandler( new ShutdownHandler.Shutdownable()
		{
			@Override
			public List<String> systemShutdown()
			{
				// Let the main thread continue, which will do close()...
				ShutdownHandler.shutdown("shutdown-handler");

				//close();
				
				// wait for the "main" thread to terminate...
				return Arrays.asList( new String[]{"main"} );
			}
		});
	}
//		Thread shutdownHook = new Thread("ShudtownHook-"+Version.getAppName()) 
//		{
//			@Override
//			public void run() 
//			{
//				_logger.info("Shutdown-hook: Starting to do 'shutdown'...");
//				
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("--------------------------------------------------");
//					for (Thread th : Thread.getAllStackTraces().keySet()) 
//						_logger.debug("1-shutdownHook: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");
//				}
//
//				// Signal the main thread that it should "continue" and do shutdown
//				ShutdownHandler.shutdown("shutdown-hook");
//				
//				// This is probably not needed, since close() is also called from the "main" thread after WaitforShutdown.shutdown() has continued
//				close();
//
//				// Code below this point can probably be removed it's just for "extras"
//				
////				_logger.info("Shutdown-hook: Interrupting threads...");
////				Set<Thread> runningThreads   = Thread.getAllStackTraces().keySet();
////				Set<Thread> interruptThreads = new LinkedHashSet<>();
////				for (Thread th : runningThreads) 
////				{
////					if (    th != Thread.currentThread()
////					     && ! th.isDaemon() 
////					     && th.getClass().getName().startsWith("com.asetune")) 
////					{
////						_logger.info("Shutdown-hook: Interrupting thread '"+th.getName()+"', at class '"+th.getClass()+"'.");
////						interruptThreads.add(th);
////						th.interrupt();
////					}
////				}
//				
//				if (_logger.isDebugEnabled())
//				{
//					_logger.debug("--------------------------------------------------");
//					for (Thread th : Thread.getAllStackTraces().keySet()) 
//						_logger.debug("2-shutdownHook: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");
//				}
//
////				// Wait for interupted threads
////				for (Thread th : interruptThreads) 
////				{
////					try 
////					{
////						if (th.isInterrupted()) 
////						{
////							_logger.info("Shutdown-hook: Waiting for thread '"+th.getName()+"' to terminate");
////							th.join();
////						}
////					} 
////					catch (InterruptedException ex) 
////					{
////						_logger.info("Shutdown-hook: The shutdown was interrupted");
////					}
////				}
////
////				if (_logger.isDebugEnabled())
////				{
////					_logger.debug("--------------------------------------------------");
////					for (Thread th : Thread.getAllStackTraces().keySet()) 
////						_logger.debug("3-shutdownHook: isDaemon="+th.isDaemon()+", threadName=|"+th.getName()+"|, th.getClass().getName()=|"+th.getClass().getName()+"|.");
////				}
////
////				_logger.info("Shutdown-hook: Shutdown finished. (interrupted "+interruptThreads.size()+" threads.");
//
//				_logger.info("Shutdown-hook: Shutdown finished.");
//			}
//		};
//		Runtime.getRuntime().addShutdownHook(shutdownHook);
//		return shutdownHook;
//	}
	
	private static void writeDbxTuneServiceFile() 
	throws IOException
	{
		// file location: ${HOME}/.asetune/info/${dbmsSrvName}.dbxtune
		String centralServiceInfoFile = getAppInfoDir() + File.separator + ".dbxtune_central";
		File f = new File(centralServiceInfoFile);

		_logger.info("Creating DbxTune - Central Service information file '" + f.getAbsolutePath() + "'.");

		// Create the directory structure (if it's not there)
		if (f.getParentFile() != null)
			f.getParentFile().mkdirs();

		// Create the file
		f.createNewFile();
		
		// Mark it as to be removed when the JVM ends.
		f.deleteOnExit();


		// Create the configuration
		Configuration conf = new Configuration(centralServiceInfoFile);
//		Configuration.setInstance(DBXTUNE_CENTRAL_INFO_CONFIG, conf);
		
		// Set some configuartion in the file
		conf.setProperty("dbxtune.central.app.name",     Version.getAppName());
		conf.setProperty("dbxtune.central.startTime",    new Timestamp(System.currentTimeMillis())+"" );
		conf.setProperty("dbxtune.central.pid",          JavaUtils.getProcessId("-1"));
//		conf.setProperty("dbxtune.central.log.file",     logFilename);
//		conf.setProperty("dbxtune.central.config.file",  noGuiConfigFile);

		// Save it; with override
		conf.save(true);
	}

	private static void startCentralPcs() 
	throws Exception
	{
//		CentralPersistWriterJdbc jdbcWriter = new CentralPersistWriterJdbc();
//		jdbcWriter.init( conf );
//		
//		// This will probably be moved to: PersistWriterHandler or similar, that will be responsible for writing data...
//		// The CentralPersistWriterBase->CentralPersistWriterJdbc should only be the implementation for how to do it...
//		CentralPersistWriterBase.setInstance(jdbcWriter);

//		Configuration conf = new Configuration();
//		conf.setProperty(CentralPcsWriterHandler.PROPKEY_WriterClass, "com.asetune.central.pcs.CentralPersistWriterJdbc");
//			
//		conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_DRIVER,   "org.h2.Driver");
//		conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL,      "jdbc:h2:file:${DBXTUNE_SAVE_DIR}/DBXTUNE_CENTRAL_DB");
//		conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME, "sa");
//		conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD, "");
		
		Configuration conf = Configuration.getInstance(Configuration.SYSTEM_CONF);
		if (conf == null)
			throw new Exception("Can't get configuration 'SYSTEM_CONF'.");
		
		// If no properties is set... set some defaults...
		if ( ! conf.hasProperty(CentralPcsWriterHandler.PROPKEY_WriterClass  ) ) conf.setProperty(CentralPcsWriterHandler.PROPKEY_WriterClass  , "com.asetune.central.pcs.CentralPersistWriterJdbc");
		
		if ( ! conf.hasProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_DRIVER  ) ) conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_DRIVER  , "org.h2.Driver");
		if ( ! conf.hasProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL     ) ) conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL     , "jdbc:h2:file:${DBXTUNE_SAVE_DIR}/DBXTUNE_CENTRAL_DB");
		if ( ! conf.hasProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME) ) conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME, "sa");
		if ( ! conf.hasProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD) ) conf.setProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD, "");

		
		//---------------------------
		// START the Persistent Storage thread
		//---------------------------
		CentralPcsWriterHandler pcsw = null;
		try
		{
			pcsw = new CentralPcsWriterHandler();
			pcsw.init( conf );
			pcsw.start();
			
			CentralPcsWriterHandler.setInstance(pcsw);
		}
		catch (Exception e)
		{
			throw e;
//			_logger.error("Problems initializing PersistentCounterHandler,", e);
//			return;
		}
		if ( ! pcsw.hasWriters() )
		{
			throw new Exception("No writers installed to the PersistentCounterHandler, this is NO-GUI... So I do not see the need for me to start.");
//			_logger.error("No writers installed to the PersistentCounterHandler, this is NO-GUI... So I do not see the need for me to start.");
//			return;
		}

		
		//---------------------------
		// START the Persistent Reader
		//---------------------------
		if ( ! PersistReader.hasInstance() )
		{
			CentralPersistReader reader = new CentralPersistReader();
//			CentralPersistReader reader = new CentralPersistReader(getOfflineConnection());

			reader.setJdbcDriver(conf.getMandatoryProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_DRIVER));
			reader.setJdbcUrl   (conf.getMandatoryProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_URL));
			reader.setJdbcUser  (conf.getMandatoryProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_USERNAME));
			reader.setJdbcPasswd(conf.getMandatoryProperty(CentralPersistWriterJdbc.PROPKEY_JDBC_PASSWORD));

			reader.init();
			
			CentralPersistReader.setInstance(reader);
		}
		
		//
		// Start H2 tcp/pg/web services...
		//
//		if ( _jdbcDriver.equals("org.h2.Driver") && _startH2NetworkServer )
		if ( true )
		{
			try
			{
				boolean writeDbxTuneServiceFile = false;
//				String baseDir = StringUtil.getEnvVariableValue("DBXTUNE_SAVE_DIR");
				String baseDir = getAppDataDir();

				List<String> tcpSwitches = new ArrayList<>();
				List<String> webSwitches = new ArrayList<>();
				List<String> pgSwitches  = new ArrayList<>();

				boolean startTcpServer = Configuration.getCombinedConfiguration().getBooleanProperty("h2.tcp.startServer", true);
				boolean startWebServer = Configuration.getCombinedConfiguration().getBooleanProperty("h2.web.startServer", true);
				boolean startPgServer  = Configuration.getCombinedConfiguration().getBooleanProperty("h2.pg.startServer",  true);

				int     tcpBasePortNumber  = Configuration.getCombinedConfiguration().getIntProperty("h2.tcp.port", 9092);
				int     webBasePortNumber  = Configuration.getCombinedConfiguration().getIntProperty("h2.web.port", 8082);
				int     pgBasePortNumber   = Configuration.getCombinedConfiguration().getIntProperty("h2.pg.port",  5435);

				tcpBasePortNumber = NetUtils.getFirstFreeLocalPortNumber(tcpBasePortNumber);
				webBasePortNumber = NetUtils.getFirstFreeLocalPortNumber(webBasePortNumber);
				pgBasePortNumber  = NetUtils.getFirstFreeLocalPortNumber(pgBasePortNumber);

				// If we couldn't get a port number... do not start that specific service
				if (tcpBasePortNumber == -1) { startTcpServer = false; _logger.warn("Could not get a valid port for H2 TCP Network Service, which wont be started."); }
				if (webBasePortNumber == -1) { startWebServer = false; _logger.warn("Could not get a valid port for H2 WEB Network Service, which wont be started."); }
				if (pgBasePortNumber  == -1) { startPgServer  = false; _logger.warn("Could not get a valid port for H2 Postgres Network Service, which wont be started."); }

				//-------------------------------------------
				// Switches to TCP server
				tcpSwitches.add("-tcpDaemon");         // Start the service thread as a daemon
				tcpSwitches.add("-tcpAllowOthers");    // Allow other that the localhost to connect
				tcpSwitches.add("-tcpPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
				tcpSwitches.add(""+tcpBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
				tcpSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
				if (StringUtil.hasValue(baseDir))
				{
					tcpSwitches.add("-baseDir");
					tcpSwitches.add(baseDir);
					
					writeDbxTuneServiceFile = true;
				}
				
				//-------------------------------------------
				// Switches to WEB server
				webSwitches.add("-webDaemon");         // Start the service thread as a daemon
				webSwitches.add("-webAllowOthers");    // Allow other that the localhost to connect
				webSwitches.add("-webPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
				webSwitches.add(""+webBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
				webSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
				if (StringUtil.hasValue(baseDir))
				{
					webSwitches.add("-baseDir");
					webSwitches.add(baseDir);
				}

				//-------------------------------------------
				// Switches to POSTGRES server
				pgSwitches.add("-pgDaemon");         // Start the service thread as a daemon
				pgSwitches.add("-pgAllowOthers");    // Allow other that the localhost to connect
				pgSwitches.add("-pgPort");           // Try this port as a base, if it's bussy, H2 will grab "next" available
				pgSwitches.add(""+pgBasePortNumber); // Try this port as a base, if it's bussy, H2 will grab "next" available
				pgSwitches.add("-ifExists");          // If the database file DO NOT exists, DO NOT CREATE one
				if (StringUtil.hasValue(baseDir))
				{
					pgSwitches.add("-baseDir");
					pgSwitches.add(baseDir);
				}
				
				
				//java -cp ${H2_JAR} org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort ${portStart} -ifExists -baseDir ${baseDir} &

				
				if (startTcpServer)
				{
					_logger.info("Starting a H2 TCP server. Switches: "+tcpSwitches);
					_h2TcpServer = org.h2.tools.Server.createTcpServer(tcpSwitches.toArray(new String[0]));
					_h2TcpServer.start();
		
		//			_logger.info("H2 TCP server, listening on port='"+h2TcpServer.getPort()+"', url='"+h2TcpServer.getURL()+"', service='"+h2TcpServer.getService()+"'.");
					_logger.info("H2 TCP server, url='"+_h2TcpServer.getURL()+"', Status='"+_h2TcpServer.getStatus()+"'.");
				}
	
				if (startWebServer)
				{
					try
					{
						_logger.info("Starting a H2 WEB server. Switches: "+webSwitches);
						_h2WebServer = org.h2.tools.Server.createWebServer(webSwitches.toArray(new String[0]));
						_h2WebServer.start();

						_logger.info("H2 WEB server, url='"+_h2WebServer.getURL()+"', Status='"+_h2WebServer.getStatus()+"'.");
					}
					catch (Exception e)
					{
						_logger.info("H2 WEB server, failed to start, but I will continue anyway... Caught: "+e);
					}
				}

				if (startPgServer)
				{
					try
					{
						_logger.info("Starting a H2 Postgres server. Switches: "+pgSwitches);
						_h2PgServer = org.h2.tools.Server.createPgServer(pgSwitches.toArray(new String[0]));
						_h2PgServer.start();
		
						_logger.info("H2 Postgres server, url='"+_h2PgServer.getURL()+"', Status='"+_h2PgServer.getStatus()+"'.");
					}
					catch (Exception e)
					{
						_logger.info("H2 Postgres server, failed to start, but I will continue anyway... Caught: "+e);
					}
				}
				
//				if (writeDbxTuneServiceFile)
//				{
//					Configuration conf = Configuration.getInstance(DbxTune.DBXTUNE_NOGUI_INFO_CONFIG);
//					H2UrlHelper urlHelper = new H2UrlHelper(_lastUsedUrl);
//
//					conf.setProperty("pcs.last.url", _lastUsedUrl);
//					
//					if (_h2TcpServer != null)
//					{
//						conf.setProperty("pcs.h2.tcp.port" ,_h2TcpServer.getPort());
//						conf.setProperty("pcs.h2.tcp.url"  ,_h2TcpServer.getURL());
//						conf.setProperty("pcs.h2.jdbc.url" ,"jdbc:h2:" + _h2TcpServer.getURL() + "/" + urlHelper.getFile().getName() );
//					}
//					if (_h2WebServer != null)
//					{
//						conf.setProperty("pcs.h2.web.port", _h2WebServer.getPort());
//						conf.setProperty("pcs.h2.web.url",  _h2WebServer.getURL());
//					}
//					if (_h2PgServer != null)
//					{
//						conf.setProperty("pcs.h2.pg.port", _h2PgServer.getPort());
//						conf.setProperty("pcs.h2.pg.url",  _h2PgServer.getURL());
//					}
//					
//					conf.save(true);
//				}
			}
			catch (SQLException e) 
			{
				_logger.warn("Problem starting H2 network service", e);
			}
		}
	}

	private static void stopCentralPcs() 
	throws Exception
	{
		if (CentralPcsWriterHandler.hasInstance())
		{
			CentralPcsWriterHandler.getInstance().stop(true, 10*1000);
		}

		if (_h2TcpServer != null)
			_h2TcpServer.stop();

		if (_h2WebServer != null)
			_h2WebServer.stop();

		if (_h2PgServer != null)
			_h2PgServer.stop();

	}

	private static void startScheduler()
	throws Exception
	{
		_scheduler = new Scheduler();
		
		//--------------------------------------------
		// H2 Database File Cleanup - Scheduling Task
		//--------------------------------------------
		boolean h2DbFileCleanupStart = Configuration.getCombinedConfiguration().getBooleanProperty(DataDirectoryCleaner.PROPKEY_start, DataDirectoryCleaner.DEFAULT_start);
		if (h2DbFileCleanupStart)
		{
			File logFile = Logging.getBaseLogFile("_" + DataDirectoryCleaner.class.getSimpleName() + ".log");
			if (logFile != null)
			{
				String pattern = Configuration.getCombinedConfiguration().getProperty(DataDirectoryCleaner.PROPKEY_LOG_FILE_PATTERN, DataDirectoryCleaner.DEFAULT_LOG_FILE_PATTERN);
				PatternLayout layout = new PatternLayout(pattern);
				_logger.info("Adding separate log file for '"+DataDirectoryCleaner.EXTRA_LOG_NAME+"' using file '"+logFile.getAbsolutePath()+"' with pattern '"+pattern+"'.");
				
				// Create a Rolling Log File
				RollingFileAppender appender = new RollingFileAppender(layout, logFile.getAbsolutePath(), true);
				appender.setMaxFileSize("10MB");
				appender.setMaxBackupIndex(3);
				appender.setName(DataDirectoryCleaner.EXTRA_LOG_NAME);

				// Only log messages from 'DataDirectoryCleaner' in this appender
				appender.addFilter(new Filter()
				{
					@Override
					public int decide(LoggingEvent event)
					{
						if (event.getLogger().getName().equals(DataDirectoryCleaner.class.getName()))
							return Filter.NEUTRAL;

						return Filter.DENY;
					}
				});

				// Add the appender
				Logger.getRootLogger().addAppender(appender);
			}

			String cron  = Configuration.getCombinedConfiguration().getProperty(DataDirectoryCleaner.PROPKEY_cron,  DataDirectoryCleaner.DEFAULT_cron);
			_logger.info("Adding 'Data Directory Cleanup' scheduling with cron entry '"+cron+"', human readable '"+CronUtils.getCronExpressionDescription(cron)+"'.");
			_scheduler.schedule(cron, new DataDirectoryCleaner());
		}

		//--------------------------------------------
		// H2 Database Writer Statistics - Scheduling Task
		//--------------------------------------------
		boolean h2WriterStatisticsStart = Configuration.getCombinedConfiguration().getBooleanProperty(H2WriterStatCronTask.PROPKEY_start, H2WriterStatCronTask.DEFAULT_start);
		if (h2WriterStatisticsStart)
		{
			File logFile = Logging.getBaseLogFile("_" + H2WriterStatCronTask.class.getSimpleName() + ".log");
			if (logFile != null)
			{
				String pattern = Configuration.getCombinedConfiguration().getProperty(H2WriterStatCronTask.PROPKEY_LOG_FILE_PATTERN, H2WriterStatCronTask.DEFAULT_LOG_FILE_PATTERN);
				PatternLayout layout = new PatternLayout(pattern);
				_logger.info("Adding separate log file for '"+H2WriterStatCronTask.EXTRA_LOG_NAME+"' using file '"+logFile.getAbsolutePath()+"' with pattern '"+pattern+"'.");
				
				// Create a Rolling Log File
				RollingFileAppender appender = new RollingFileAppender(layout, logFile.getAbsolutePath(), true);
				appender.setMaxFileSize("10MB");
				appender.setMaxBackupIndex(3);
				appender.setName(H2WriterStatCronTask.EXTRA_LOG_NAME);

				// Only log messages from 'H2WriterStatCronTask' in this appender
				appender.addFilter(new Filter()
				{
					@Override
					public int decide(LoggingEvent event)
					{
						if (event.getLogger().getName().equals(H2WriterStatCronTask.class.getName()))
							return Filter.NEUTRAL;

						return Filter.DENY;
					}
				});

				// Add the appender
				Logger.getRootLogger().addAppender(appender);
			}

			String cron  = Configuration.getCombinedConfiguration().getProperty(H2WriterStatCronTask.PROPKEY_cron,  H2WriterStatCronTask.DEFAULT_cron);
			_logger.info("Adding 'H2 Writer File Size Statistics' scheduling with cron entry '"+cron+"', human readable '"+CronUtils.getCronExpressionDescription(cron)+"'.");
			_scheduler.schedule(cron, new H2WriterStatCronTask());
		}

		//--------------------------------------------
		// Central PCS Database Data/Content Cleanup - Scheduling Task
		//--------------------------------------------
		boolean centralPcsDataCleanupStart = Configuration.getCombinedConfiguration().getBooleanProperty(CentralPcsJdbcCleaner.PROPKEY_start, CentralPcsJdbcCleaner.DEFAULT_start);
		if (centralPcsDataCleanupStart)
		{
			File logFile = Logging.getBaseLogFile("_" + CentralPcsJdbcCleaner.class.getSimpleName() + ".log");
			if (logFile != null)
			{
				String pattern = Configuration.getCombinedConfiguration().getProperty(CentralPcsJdbcCleaner.PROPKEY_LOG_FILE_PATTERN, CentralPcsJdbcCleaner.DEFAULT_LOG_FILE_PATTERN);
				PatternLayout layout = new PatternLayout(pattern);
				_logger.info("Adding separate log file for '"+CentralPcsJdbcCleaner.EXTRA_LOG_NAME+"' using file '"+logFile.getAbsolutePath()+"' with pattern '"+pattern+"'.");
				
				// Create a Rolling Log File
				RollingFileAppender appender = new RollingFileAppender(layout, logFile.getAbsolutePath(), true);
				appender.setMaxFileSize("10MB");
				appender.setMaxBackupIndex(3);
				appender.setName(CentralPcsJdbcCleaner.EXTRA_LOG_NAME);

				// Only log messages from 'CentralPcsJdbcCleaner' in this appender
				appender.addFilter(new Filter()
				{
					@Override
					public int decide(LoggingEvent event)
					{
						if (event.getLogger().getName().equals(CentralPcsJdbcCleaner.class.getName()))
							return Filter.NEUTRAL;

						return Filter.DENY;
					}
				});

				// Add the appender
				Logger.getRootLogger().addAppender(appender);
			}

			String cron  = Configuration.getCombinedConfiguration().getProperty(CentralPcsJdbcCleaner.PROPKEY_cron,  CentralPcsJdbcCleaner.DEFAULT_cron);
			_logger.info("Adding 'Central PCS Data Retention/Cleanup' scheduling with cron entry '"+cron+"', human readable '"+CronUtils.getCronExpressionDescription(cron)+"'.");
			_scheduler.schedule(cron, new CentralPcsJdbcCleaner());
		}
		
		//--------------------------------------------
		// Central H2 Database DEFRAG - Scheduling Task
		//--------------------------------------------
		boolean centralH2DefragStart = Configuration.getCombinedConfiguration().getBooleanProperty(CentralH2Defrag.PROPKEY_start, CentralH2Defrag.DEFAULT_start);
		if (centralH2DefragStart)
		{
			File logFile = Logging.getBaseLogFile("_" + CentralH2Defrag.class.getSimpleName() + ".log");
			if (logFile != null)
			{
				String pattern = Configuration.getCombinedConfiguration().getProperty(CentralH2Defrag.PROPKEY_LOG_FILE_PATTERN, CentralH2Defrag.DEFAULT_LOG_FILE_PATTERN);
				PatternLayout layout = new PatternLayout(pattern);
				_logger.info("Adding separate log file for '"+CentralH2Defrag.EXTRA_LOG_NAME+"' using file '"+logFile.getAbsolutePath()+"' with pattern '"+pattern+"'.");
				
				// Create a Rolling Log File
				RollingFileAppender appender = new RollingFileAppender(layout, logFile.getAbsolutePath(), true);
				appender.setMaxFileSize("10MB");
				appender.setMaxBackupIndex(3);
				appender.setName(CentralH2Defrag.EXTRA_LOG_NAME);

				// Only log messages from 'CentralH2Defrag' in this appender
				appender.addFilter(new Filter()
				{
					@Override
					public int decide(LoggingEvent event)
					{
//						if (    event.getLogger().getName().equals(CentralH2Defrag.class.getName()) 
//						     || event.getLogger().getName().equals(CentralPersistWriterJdbc.class.getName()) 
//						   )
//						{
//							return Filter.NEUTRAL;
//						}

						// log all in: CentralH2Defrag
						if (event.getLogger().getName().equals(CentralH2Defrag.class.getName()))
							return Filter.NEUTRAL;

						// log only "some" (message has H2 in the message) in: CentralPersistWriterJdbc
						if (event.getLogger().getName().equals(CentralPersistWriterJdbc.class.getName()))
						{
//							if (event.getThreadName().equals("xxxx"))
//								return Filter.NEUTRAL;
								
							String msg = "" + event.getMessage();

							if (msg.indexOf(" H2 ") != -1)
								return Filter.NEUTRAL;
						}

						return Filter.DENY;
					}
				});

				// Add the appender
				Logger.getRootLogger().addAppender(appender);
			}

			String cron  = Configuration.getCombinedConfiguration().getProperty(CentralH2Defrag.PROPKEY_cron,  CentralH2Defrag.DEFAULT_cron);
			_logger.info("Adding 'H2 Database Defrag' scheduling with cron entry '"+cron+"', human readable '"+CronUtils.getCronExpressionDescription(cron)+"'.");
			_scheduler.schedule(cron, new CentralH2Defrag());
		}

		//--------------------------------------------
		// Central Daily Summary REPORT
		//--------------------------------------------
		boolean centralDailyReportStart = Configuration.getCombinedConfiguration().getBooleanProperty(CentralDailyReportSender.PROPKEY_start, CentralDailyReportSender.DEFAULT_start);
		if (centralDailyReportStart)
		{
			File logFile = Logging.getBaseLogFile("_" + CentralDailyReportSender.class.getSimpleName() + ".log");
			if (logFile != null)
			{
				String pattern = Configuration.getCombinedConfiguration().getProperty(CentralDailyReportSender.PROPKEY_LOG_FILE_PATTERN, CentralDailyReportSender.DEFAULT_LOG_FILE_PATTERN);
				PatternLayout layout = new PatternLayout(pattern);
				_logger.info("Adding separate log file for '"+CentralDailyReportSender.EXTRA_LOG_NAME+"' using file '"+logFile.getAbsolutePath()+"' with pattern '"+pattern+"'.");
				
				// Create a Rolling Log File
				RollingFileAppender appender = new RollingFileAppender(layout, logFile.getAbsolutePath(), true);
				appender.setMaxFileSize("10MB");
				appender.setMaxBackupIndex(3);
				appender.setName(CentralDailyReportSender.EXTRA_LOG_NAME);

				// Only log messages from 'CentralDailyReportSender' in this appender
				appender.addFilter(new Filter()
				{
					@Override
					public int decide(LoggingEvent event)
					{
						if (event.getLogger().getName().equals(CentralDailyReportSender.class.getName()))
							return Filter.NEUTRAL;

						return Filter.DENY;
					}
				});

				// Add the appender
				Logger.getRootLogger().addAppender(appender);
			}

			String cron  = Configuration.getCombinedConfiguration().getProperty(CentralDailyReportSender.PROPKEY_cron,  CentralDailyReportSender.DEFAULT_cron);
			_logger.info("Adding 'Central Daily Summary Report' scheduling with cron entry '"+cron+"', human readable '"+CronUtils.getCronExpressionDescription(cron)+"'.");
			_scheduler.schedule(cron, new CentralDailyReportSender());
		}
		
		
		//--------------------------------------------
		// Start the scheduler
		//--------------------------------------------
		_logger.info("Starting scheduler thread.");
		//_scheduler.setName("cron4j-sched"); // this would have been nice...
		_scheduler.setDaemon(true);
		_scheduler.start();
	}

	public static void stopScheduler() 
	throws Exception
	{
		_logger.info("Stopping scheduler thread.");
		_scheduler.stop();
	}

	private static void startWebServer()
	throws Exception
	{
		File webDir = new File( getAppHomeDir() + File.separatorChar + "resources" + File.separatorChar + "WebContent");
		if ( ! webDir.exists() )
		{
			throw new Exception("The WEB Content directory '"+webDir+"' can't be found... Check env ASETUNE_HOME and/or DBXTUNE_CENTRAL_HOME");
		}
		
		startWebServerJetty();
//		startWebServerTomcat();
	}

	public static void stopWebServer() 
	throws Exception
	{
		if (_server == null)
			return;
		
		_server.stop();
	}

	private static void startWebServerJetty()
	throws Exception
	{
		System.out.println("jetty.home = '"+System.getProperty("jetty.home")+"'.");
		
		// Start the webserver
//		if (true)
//		{
//			int port = 8080;
//			Server server = new Server();
//
//			ServerConnector http = new ServerConnector(server);
////			http.setHost("localhost");
//			http.setPort(port);
//			http.setIdleTimeout(30000);
//
//			// Set the connector
//			server.addConnector(http);
//			
//			ContextHandler context = null;
//			HandlerCollection handlers = new HandlerCollection();
//
//			// Add a single handler on context "/connect-offline"
//			context = new ContextHandler("/xxx");
//			context.setHandler( new DbxTuneGuiHttpConnectOfflineHandler() );
//			handlers.addHandler(context);
//
////			context = new ContextHandler("/api/pcs/receive");
////			context.setHandler( new CentralPcsReceiverController() );
////			handlers.addHandler(context);
//
//			ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.SESSIONS);
//			sch.addServlet(CentralPcsReceiverController.class, "/api/pcs/receive");
//			sch.addServlet(DefaultServlet.class, "/*");
//			handlers.addHandler(sch);
//
//			ResourceHandler rh = new ResourceHandler();
////			rh.setDirectoriesListed(true);
//			rh.setWelcomeFiles(new String[]{ "index.html" });
//			rh.setResourceBase("www");
//			handlers.addHandler(rh);
//
//			rh = new ResourceHandler();
//			rh.setDirectoriesListed(true);
////			rh.setWelcomeFiles(new String[]{ "index.html" });
//			rh.setResourceBase("log");
//			handlers.addHandler(rh);
//
//			handlers.addHandler(new DefaultHandler());
//
//			server.setHandler( handlers );
//
//			_logger.info("Starting local Web server at port "+port+".");
//			server.start();
//		}

		// https://examples.javacodegeeks.com/enterprise-java/jetty/jetty-web-xml-configuration-example/
		if (true)
		{
//			Server server = new Server(8080);
			int port = Configuration.getCombinedConfiguration().getIntProperty(PROPKEY_WEB_PORT, DEFAULT_WEB_PORT);
			_server = new Server(port);
	   
			// Handler for multiple web apps
			HandlerCollection handlers = new HandlerCollection();
	 
//			// Creating the first web application context
//			WebAppContext webapp1 = new WebAppContext();
//			webapp1.setResourceBase("C:\\projects\\AseTune\\resources\\WebContent");
//			webapp1.setContextPath("/");
////			webapp1.setDefaultsDescriptor("src/main/webdefault/webdefault.xml");
//			handlers.addHandler(webapp1);

			String webDir = getAppWebDir();
			WebAppContext webapp1 = new WebAppContext();
			webapp1.setDescriptor(webDir+"/WEB-INF/web.xml");
			webapp1.setResourceBase(webDir);
			webapp1.setContextPath("/");
			webapp1.getInitParams().put("org.eclipse.jetty.servlet.Default.useFileMappedBuffer", "false");
			webapp1.setParentLoaderPriority(true);

//	        // The below 4 lines is to get JSP going????
//	        webapp1.setAttribute("org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern",".*/[^/]*jstl.*\\.jar$");
//	        org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(_server);
//	        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration", "org.eclipse.jetty.plus.webapp.PlusConfiguration");
//	        classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

			handlers.addHandler(webapp1);

//			// Creating the second web application context
//			WebAppContext webapp2 = new WebAppContext();
//			webapp2.setResourceBase("src/main/webapp2");
//			webapp2.setContextPath("/webapp2");
//			webapp2.setDefaultsDescriptor("src/main/webdefault/webdefault.xml");
//			handlers.addHandler(webapp2);


			// Creating the LoginService for the realm
			DbxCentralRealm loginService = new DbxCentralRealm("DbxTuneCentralRealm");

//			// Creating the LoginService for the realm
//			HashLoginService loginService = new HashLoginService("DbxTuneCentralRealm");
//
//			// Setting the realm configuration there the users, passwords and roles reside
//			String userFile = Configuration.getCombinedConfiguration().getProperty("realm.users.file", webDir+"/dbxtune_central_users.txt");
			// The above file should look like:
			// admin: admin,admin,user
			//user1: user1pass,user

//			loginService.setConfig(userFile);
//			_logger.info("Web Autentication. Using property 'realm.users.file', which is set to '"+userFile+"'.");
////			loginService.setConfig("/projects/AseTune/resources/WebContent/dbxtune_central_users.txt");
////			Map<String, UserIdentity> userMap = new HashMap<>();
////			userMap.put("xxx", new UserIdentity()
////			loginService.setUsers();

			// Appending the loginService to the Server
			_server.addBean(loginService);

			// Adding the handlers to the server
			_server.setHandler(handlers);
	 
			// Starting the Server
			_server.start();
			_logger.info("Started 'Jetty' as Web server.");

			_logger.info("Web server 'Jetty' configuration. \n" + _server.dump());
//			_server.dumpStdErr();
			
			//server.join();
		}
		
		// https://wiki.eclipse.org/Jetty/Tutorial/Embedding_Jetty
		//TEST THE ABOVE
		
//		if (false)
//		{
//			Server server = new Server(8080);
//			HandlerList handlers = new HandlerList();
//			ResourceHandler resourceHandler = new ResourceHandler();
//			resourceHandler.setBaseResource(Resource.newResource("."));
//			handlers.setHandlers(new Handler[] { resourceHandler, new DefaultHandler() });
//			server.setHandler(handlers);
//			server.start();
//		}
	}
	
//	private static void startWebServerTomcat()
//	throws Exception
//	{
////		System.setProperty("DBXTUNE_CENTRAL_HOME", "C:/tmp/DbxTuneCentral/maxm");
////		System.setProperty("catalina.home", new File("./").getAbsolutePath());
//		
//		String webappDirLocation = "resources/WebContent/";
//		Tomcat tomcat = new Tomcat();
//
//		// The port that we should run on can be set into an environment
//		// variable
//		// Look for that variable and default to 8080 if it isn't there.
//		String webPort = System.getenv("PORT");
//		if ( webPort == null || webPort.isEmpty() )
//		{
//			webPort = "8080";
//		}
//		
////		UserDatabaseRealm realm = new UserDatabaseRealm();
////		realm.setRealmPath("C:\\Users\\gorans\\workspace\\TomcatTest\\WebContent\\config\\tomcat-users.xml");
//		
//		System.out.println("tomcat.getHost().getCatalinaHome()='" + tomcat.getHost().getCatalinaHome() + "'");
////		tomcat.getHost().setRealm(realm);
//
////		MemoryRealm realm = new MemoryRealm();
////		UserDatabase ud = (UserDatabase) new InitialContext().lookup("java:comp/env/UserDatabase");
////		ud.createRole("admin", "description");
////		ud.createUser("username", "password", "fullname");
////		ud.createUser("admin", "admin", "fullname");
//
//		
//		tomcat.setPort(Integer.valueOf(webPort));
//
//		System.out.println("configuring app with basedir: " + new File("./" + webappDirLocation).getAbsolutePath());
//		StandardContext ctx = (StandardContext) tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
//
////		File fileDir1 = new File("www");
////		String baseDir1 = fileDir1.getAbsolutePath();
////		System.out.println("Adding context2: "+baseDir1+", exists="+fileDir1.exists());
////		Context ctx1 = tomcat.addContext("/", baseDir1);
//
////		tomcat.addWebapp("/maxm",   new File("C:\\tmp\\DbxTuneCentral\\maxm").getAbsolutePath());
////		tomcat.addWebapp("/config", new File("C:\\tmp\\DbxTuneCentral\\maxm\\config").getAbsolutePath());
//
//		File fileDir2 = new File("C:\\tmp\\DbxTuneCentral\\maxm\\log");
//		String baseDir2 = fileDir2.getAbsolutePath();
//		System.out.println("Adding context2: "+baseDir2+", exists="+fileDir2.exists());
////		Context ctx2 = tomcat.addContext("/ctx2", baseDir2);
//		StandardContext ctx2 = (StandardContext) tomcat.addWebapp("/log", baseDir2);
//		
//		
////		tomcat.addContext("/log", "C:\\tmp\\DbxTuneCentral\\maxm\\log");
////		tomcat.addContext("/log/*", new File("C:\\tmp\\DbxTuneCentral\\maxm\\log").getAbsolutePath());
//		
////		String srvletName;
////		
////		srvletName = DummyServlet.class.getName();
////		Tomcat.addServlet(ctx, srvletName, new DummyServlet());
////		ctx.addServletMappingDecoded("/xxx", srvletName);
////		
////		srvletName = OverviewServlet.class.getName();
////		Tomcat.addServlet(ctx, srvletName, new OverviewServlet());
////		ctx.addServletMappingDecoded("/overview", srvletName);
////
////		srvletName = AlarmLogServlet.class.getName();
////		Tomcat.addServlet(ctx, srvletName, new AlarmLogServlet());
////		ctx.addServletMappingDecoded("/alarmLog", srvletName);
////
//////		srvletName = HTMLManagerServlet.class.getName();
//////		Tomcat.addServlet(ctx, srvletName, new HTMLManagerServlet());
//////		ctx.addServletMappingDecoded("/manager", srvletName);
//
//		HashMap<String, HttpServlet> map = new HashMap<>();
//		
////		map.put("/xxx",          new DummyServlet());
////		map.put("/overview",     new OverviewServlet());
////		map.put("/alarmLog",     new AlarmLogServlet());
////		map.put("/admin",        new AdminServlet());
////		map.put("/api/receiver", new PcsReceiverServlet());
////		map.put("/tomcat",       new HTMLManagerServlet());
//
//		for (Entry<String, HttpServlet> entry : map.entrySet())
//		{
//			String mapping    = entry.getKey();
//			String srvletName = entry.getValue().getClass().getName();
//
//			System.out.println("Adding servlet mapping '"+mapping+"' using the class/servletName '"+srvletName+"'.");
//			
//			Tomcat.addServlet(ctx, srvletName,entry.getValue());
//			ctx.addServletMappingDecoded(mapping, srvletName);
//		}
//		
//		
////		srvletName = "LogFiles";
////		DefaultServlet defaultServlet = new DefaultServlet()
////		{
//////			protected String pathPrefix = "C:\\tmp\\DbxTuneCentral\\maxm\\log";
////			protected String pathPrefix = "C:\\tmp\\DbxTuneCentral\\maxm";
////			@Override
////			public void init(ServletConfig config) throws ServletException
////			{
////				System.out.println("init(config)...............................");
////				super.init(config);
////				listings = true;
////				
////				if ( config.getInitParameter("pathPrefix") != null )
////				{
////					pathPrefix = config.getInitParameter("pathPrefix");
////				}
////				System.out.println("pathPrefix='"+pathPrefix+"'.");
////			}
////			@Override
////			protected String getRelativePath(HttpServletRequest req)
////			{
////				System.out.println("getRelativePath(): "+ pathPrefix + super.getRelativePath(req));
////				return pathPrefix + super.getRelativePath(req);
////			}
////		};
////		Tomcat.addServlet(ctx, srvletName, defaultServlet);
////		ctx.addServletMappingDecoded("/log/*", srvletName);
//				
//		tomcat.start();
////		tomcat.getServer().await();
//	}
	
	public static void startAlarmHandler()
	throws Exception
	{
		//--------------------------------------------------------
		// ALARM Handling
		//--------------------------------------------------------
//		boolean enableAlarmHandler = Configuration.getCombinedConfiguration().getBooleanProperty(AlarmHandler.PROPKEY_enable, AlarmHandler.DEFAULT_enable);
//		boolean enableAlarmHandler = false; // FIXME: implement/check how often we receive info from each collector, and alarm if it's been to long...
		boolean enableAlarmHandler = true; // FIXME: implement/check how often we receive info from each collector, and alarm if it's been to long...
		if (enableAlarmHandler)
		{
			//--------------------------------------------------------
			// Alarm Handler
			//--------------------------------------------------------
//System.setProperty("AlarmHandler.WriterClass",                 "com.asetune.alarm.writers.AlarmWriterToStdout");
//System.setProperty("AlarmHandler.WriterClass",                 "com.asetune.alarm.writers.AlarmWriterToStdout, com.asetune.alarm.writers.AlarmWriterToFile");
//System.setProperty("AlarmWriterToFile.alarms.active.filename", "c:\\tmp\\DbxCentral_alarm_active.log");
//System.setProperty("AlarmWriterToFile.alarms.log.filename",    "c:\\tmp\\DbxCentral_alarm.log");

			try
			{
				Configuration conf = Configuration.getCombinedConfiguration();
				
				// Initialize the alarm handler
				AlarmHandler ah = new AlarmHandler();
				AlarmHandler.setInstance(ah); // Set this before init() if it throws an exception and we are in GUI more, we still want to fix the error...
				ah.init(conf, false, true, true); // createTableModelWriter=false, createPcsWriter=true, createToApplicationLog=true
				ah.start();
				
//				String alarmWriters = conf.getProperty(AlarmHandler.PROPKEY_WriterClass);
//				if (StringUtil.hasValue(alarmWriters))
//				{
//					//--------------------------------------------------------
//					// User Defined Alarm Handler
//					// Compiling some dynamic java classes
//					//--------------------------------------------------------
////					UserDefinedAlarmHandler udah = new UserDefinedAlarmHandler();
////					udah.init(conf);
////					UserDefinedAlarmHandler.setInstance(udah);
//
//					// Initialize the alarm handler
//					AlarmHandler ah = new AlarmHandler();
//					AlarmHandler.setInstance(ah); // Set this before init() if it throws an exception and we are in GUI more, we still want to fix the error...
//					ah.init(conf, false, true, true); // createTableModelWriter=false, createPcsWriter=true, createToApplicationLog=true
//					ah.start();
//				}
//				else
//				{
//					_logger.warn("No 'Alarm Writers' was found in the current configuration '"+conf.getFilename()+"'. Alarm Handler will NOT be enabled. To enable the AlarmHandler, please specify any Alarm Writer classes using the configuration key '"+AlarmHandler.PROPKEY_WriterClass+"'.");
//				}
			}
			catch (Exception ex)
			{
				_logger.error("Problems Initializing the Alarm Handler Module", ex);
				throw ex;
			}
		}
	}
	public static void stopAlarmHandler()
	{
		if (AlarmHandler.hasInstance())
		{
			AlarmHandler.getInstance().shutdown();
		}
	}

	public static void startReceiverAlarmChecker()
	throws Exception
	{
		try
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			ReceiverAlarmCheck checker = new ReceiverAlarmCheck();
			
			checker.init(conf);
			checker.start();

			ReceiverAlarmCheck.setInstance(checker);
		}
		catch (Exception ex)
		{
			_logger.error("Problems Initializing the Receiver Alarm Check Module", ex);
			throw ex;
		}
	}
	public static void stopReceiverAlarmChecker() 
	{
		if (ReceiverAlarmCheck.hasInstance())
		{
			ReceiverAlarmCheck.getInstance().shutdown();
		}
	}



	public static final String APP_NAME = "DbxTuneCentral";
	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
		Version.setAppName(APP_NAME);
		DbxTune.setStartTime();

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
//				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				String error = "Unknown options: " + Arrays.toString(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start DbxTune, GUI/NOGUI will be determined later on.
			//-------------------------------
			else
			{
				// Install a shutdown hook.
				installShutdownHandler();
				
				// Initialize some things
				init(cmd);

				// WAIT Here for "anyone" to signal "stop" to the system...
				//   - Servlet request "shutdonw" or "restart" ca be used
				//   - The installed Shutdown Hook can signal "shutdown" on Ctrl-C or kill <pid>
				// Then we can stop the components
				ShutdownHandler.waitforShutdown();

				// Stop various parts of the system
				close();
				
				_logger.info("The server has now been STOPPED.");
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
			System.exit(1);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters in the underlying DbxTune initialization: init(cmd)
			// do normal exit
			System.exit(1);
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

		// Was the shutdown in restart...
		if (ShutdownHandler.wasRestartSpecified())
		{
			System.exit(ShutdownHandler.RESTART_EXIT_CODE);
		}
	}
}
