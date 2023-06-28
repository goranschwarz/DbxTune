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
package com.asetune.central.pcs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.asetune.AppDir;
import com.asetune.NormalExitException;
import com.asetune.Version;
import com.asetune.gui.ResultSetTableModel;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.ddl.DbmsDdlUtils;
import com.asetune.sql.ddl.DbmsDdlUtils.DdlType;
//import com.asetune.sql.ddl.SchemaCrawlerUtils;
//import com.asetune.sql.ddl.SchemaCrawlerUtils.DdlType;
import com.asetune.sql.ddl.model.Catalog;
import com.asetune.sql.ddl.model.ICreateFilter;
import com.asetune.sql.ddl.model.Schema;
import com.asetune.sql.ddl.model.Table;
import com.asetune.utils.Configuration;
import com.asetune.utils.DbUtils;
import com.asetune.utils.Debug;
import com.asetune.utils.H2UrlHelper;
import com.asetune.utils.Logging;
import com.asetune.utils.Memory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.TimeUtils;

//import schemacrawler.schema.Catalog;
//import schemacrawler.schema.Schema;
//import schemacrawler.schema.Table;
//import schemacrawler.schemacrawler.SchemaCrawlerException;
//import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
//import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;

/**
 * Uggly hack to copy one H2 database to another.
 * <p>
 * 
 * The idea is to use this to "recover" from a corrupt database.
 * <ul>
 *   <li>Connect to source and target database</li>
 *   <li>from source: get DDL Statements (create schema, create table, create index, etc)</li>
 *   <li>from source: get table list</li>
 *   <li>transfer each table. In case of failure, skip that table and move on to next</li>
 * </ul>
 * 
 * @author gorans
 *
 */
public class H2CentralDbCopy3
implements AutoCloseable
{
	private static final Logger _logger = Logger.getLogger(H2CentralDbCopy3.class);

//	private static char q = '"';
	
	public enum Type
	{
		Source, Target
	};
	
	public static final int     DEFAULT_DML_BATCH_SIZE = 25000;
	public static final boolean DEFAULT_DML_USE_MERGE  = false;
	
	boolean _ddlPrintExec  = false;
//	boolean _ddlPrintExec  = true;
	boolean _ddlSkipErrors = true;

	int     _dmlBatchSize  = DEFAULT_DML_BATCH_SIZE;
	boolean _dmlUseMerge   = DEFAULT_DML_USE_MERGE; // default=false: auto enabled if target H2 db file exists 
	
	List<DbTable> _tableList = new ArrayList<>();
//	List<String> _schemaList = new ArrayList<>();

	Process _migrationH2Srv;
	String _sourceH2Jar;
	File   _sourceH2JarFile;

	String _sourceUser;
	String _sourcePasswd;
	String _sourceUrl;
	File   _sourceH2File;
	DbxConnection _sourceConn;
	Catalog       _sourceCatalog;

	int    _sourceDbxCentralDbVersion = -1; 

	
	String _targetUser;
	String _targetPasswd;
	String _targetUrl;
	File   _targetH2File;
	DbxConnection _targetConn;
	Catalog       _targetCatalog;
//	List<DbTable> _targetPreTableList = new ArrayList<>(); // not really used... The MAIN is sourceTableList
	
	int    _targetDbxCentralDbVersion = -1; 

	public void setDefaults()
	{
		String DBXTUNE_SAVE_DIR = System.getProperty("DBXTUNE_SAVE_DIR");
		if (DBXTUNE_SAVE_DIR == null)
			throw new RuntimeException("System property 'DBXTUNE_SAVE_DIR' is NOT set.");
		
		String sourceDbName = DBXTUNE_SAVE_DIR + File.separatorChar + "DBXTUNE_CENTRAL_DB";
		String targetDbName = sourceDbName + "_NEW";

		String sourceH2UrlOp = ";DATABASE_TO_UPPER=false;MAX_COMPACT_TIME=60000;COMPRESS=TRUE;WRITE_DELAY=30000;IFEXISTS=TRUE";
		String targetH2UrlOp = ";CASE_INSENSITIVE_IDENTIFIERS=true;COMPRESS=TRUE;WRITE_DELAY=30000;REUSE_SPACE=FALSE";
		
		// SOURCE
		if (StringUtil.isNullOrBlank(_sourceUser  )) _sourceUser   = "sa";
		if (StringUtil.isNullOrBlank(_sourcePasswd)) _sourcePasswd = "";

		if (StringUtil.isNullOrBlank(_sourceUrl))
		{
			_sourceUrl = "jdbc:h2:file:" + sourceDbName + sourceH2UrlOp;
			
			if (StringUtil.hasValue(_sourceH2Jar))
			{
				_sourceUrl = "jdbc:h2:tcp://localhost/" + sourceDbName + sourceH2UrlOp;
			}
		}
		else
		{
			// Looks like a "plain file"
			if ( ! _sourceUrl.startsWith("jdbc:") )
			{ 
				String dbname = _sourceUrl; 
				if (dbname.endsWith(".mv.db")) 
					dbname = dbname.substring(0, dbname.length()-".mv.db".length()); 
				
				targetDbName = dbname + "_NEW";
				
				_sourceUrl = "jdbc:h2:file:" + dbname + sourceH2UrlOp;

				if (StringUtil.hasValue(_sourceH2Jar))
				{
					_sourceUrl = "jdbc:h2:tcp://localhost/" + dbname + sourceH2UrlOp;
				}
			}
		}

		// set H2 Database FILE
		if (StringUtil.hasValue(_sourceUrl) && _sourceUrl.startsWith("jdbc:h2:file:"))
		{
			_sourceH2File = new H2UrlHelper(_sourceUrl).getDbFile(true);
		}
		
		// TARGET
		if (StringUtil.isNullOrBlank(_targetUser  )) _targetUser   = _sourceUser;
		if (StringUtil.isNullOrBlank(_targetPasswd)) _targetPasswd = _sourcePasswd;
		
		if (StringUtil.isNullOrBlank(_targetUrl))
		{
			_targetUrl = "jdbc:h2:file:" + targetDbName + targetH2UrlOp;
		}
		else
		{
			// Looks like a "plain file"
			if ( ! _targetUrl.startsWith("jdbc:") )
			{ 
				String dbname = _targetUrl; 
				if (dbname.endsWith(".mv.db")) 
					dbname = dbname.substring(0, dbname.length() - ".mv.db".length()); 
				_targetUrl = "jdbc:h2:file:" + dbname + targetH2UrlOp;
			}
		}

		// set H2 Database FILE
		if (StringUtil.hasValue(_targetUrl) && _targetUrl.startsWith("jdbc:h2:file:"))
		{
			_targetH2File = new H2UrlHelper(_targetUrl).getDbFile(true);
			if (_targetH2File.exists())
			{
				_logger.info("The TARGET H2 database file already EXISTS, Enabling 'dmlUseMerge=true' for the transfer. Target H2 Database File='" + _targetH2File + "'.");
				_dmlUseMerge = true;
			}
		}
	}
	

	
	public static String getConfigFileName()  { return "H2CentralDbCopy.properties"; }

	public void init(CommandLine cmd)
	throws Exception
	{
		final String CONFIG_FILE_NAME      = System.getProperty("CONFIG_FILE_NAME", getConfigFileName());

//		final String APP_HOME              = System.getProperty(getAppHomeEnvName());
//
//		String defaultPropsFile = (APP_HOME != null) ? APP_HOME + File.separator + CONFIG_FILE_NAME : CONFIG_FILE_NAME;

		String propFile = cmd.getOptionValue("config", getConfigFileName());

		if ( cmd.hasOption("homedir") )
			System.setProperty("user.home", cmd.getOptionValue("homedir"));

		if ( cmd.hasOption("savedir") )
			System.setProperty("DBXTUNE_SAVE_DIR", cmd.getOptionValue("savedir"));
		else
		{
			String dbDir = System.getProperty("user.home") + File.separatorChar + ".dbxtune" + File.separatorChar + "dbxc" + File.separatorChar + "data";
			System.setProperty("DBXTUNE_SAVE_DIR", dbDir);
		}
		
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
					System.out.println("   SETTING SYSTEM PROPERTY: key=|" + key + "|, val=|" + val + "|.");
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
			// Decide path to the log file.
			String logPath = (AppDir.getAppStoreDir() != null) ? AppDir.getAppStoreDir() : System.getProperty("user.home");
			if ( logPath != null && ! (logPath.endsWith("/") || logPath.endsWith("\\")) )
				logPath += File.separatorChar;
			logPath += "log" + File.separatorChar;

//			logFilename = getAppLogDir() + File.separatorChar + "log" + File.separatorChar + Version.getAppName()+".log";
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
			String ts = sdf.format(new Date());
			logFilename = logPath + Version.getAppName()+"." + ts + ".log";
		}

		
		// Read some parameters
		if (cmd.hasOption('U')) _sourceUser   = cmd.getOptionValue('U');
		if (cmd.hasOption('P')) _sourcePasswd = cmd.getOptionValue('P');
		if (cmd.hasOption('S')) _sourceUrl    = cmd.getOptionValue('S');
		if (cmd.hasOption('J')) _sourceH2Jar  = cmd.getOptionValue('J');
		if (cmd.hasOption('u')) _targetUser   = cmd.getOptionValue('u');
		if (cmd.hasOption('p')) _targetPasswd = cmd.getOptionValue('p');
		if (cmd.hasOption('s')) _targetUrl    = cmd.getOptionValue('s');

		if (cmd.hasOption('b')) _dmlBatchSize = StringUtil.parseInt(cmd.getOptionValue('b'), DEFAULT_DML_BATCH_SIZE);
		if (cmd.hasOption('m')) _dmlUseMerge  = true;

		// Construct some defaults for NON Specified values
		setDefaults();

		// If we should NOT execute, print report and get out of here 
		if ( ! cmd.hasOption('e') )
		{
			printExecutionReport();
			throw new NormalExitException();
		}

		
		
		
		
//System.setProperty("log.console.debug", "true");
		// Initialize the log file
//		String propFile = null;
		Logging.init(null, propFile, logFilename);

		
		// Print out the memory configuration
		// And the JVM info
		_logger.info("Starting " + Version.getAppName() + ", version " + Version.getVersionStr() + ", build "+Version.getBuildStr());
		_logger.info("Debug Options enabled: "                          + Debug.getDebugsString());

		_logger.info("Using Java Runtime Environment Version: "         + System.getProperty("java.version"));
		_logger.info("Using Java VM Implementation  Version: "          + System.getProperty("java.vm.version"));
		_logger.info("Using Java VM Implementation  Vendor:  "          + System.getProperty("java.vm.vendor"));
		_logger.info("Using Java VM Implementation  Name:    "          + System.getProperty("java.vm.name"));
		_logger.info("Using Java VM Home:    "                          + System.getProperty("java.home"));
		_logger.info("Java class format version number: "               + System.getProperty("java.class.version"));
		_logger.info("Java class path: "                                + System.getProperty("java.class.path"));
		_logger.info("List of paths to search when loading libraries: " + System.getProperty("java.library.path"));
		_logger.info("Name of JIT compiler to use: "                    + System.getProperty("java.compiler"));
		_logger.info("Path of extension directory or directories: "     + System.getProperty("java.ext.dirs"));

		_logger.info("Maximum memory is set to:  "                      + Runtime.getRuntime().maxMemory() / 1024 / 1024 + " MB. this could be changed with  -Xmx###m (where ### is number of MB)"); // jdk 1.4 or higher
		_logger.info("Total Physical Memory on this machine:  "         + Memory.getTotalPhysicalMemorySizeInMB() + " MB.");
		_logger.info("Free Physical Memory on this machine:  "          + Memory.getFreePhysicalMemorySizeInMB() + " MB.");
		_logger.info("Running on Operating System Name:  "              + System.getProperty("os.name"));
		_logger.info("Running on Operating System Version:  "           + System.getProperty("os.version"));
		_logger.info("Running on Operating System Architecture:  "      + System.getProperty("os.arch"));
		_logger.info("The application was started by the username:  "   + System.getProperty("user.name"));
		_logger.info("The application was started in the directory:   " + System.getProperty("user.dir"));
		_logger.info("The user '" + System.getProperty("user.name") + "' home directory:   " + System.getProperty("user.home"));

		_logger.info("System configuration file is '" + propFile + "'.");
//		_logger.info("User configuration file is '" + userPropFile + "'.");
//		_logger.info("Storing temporary configurations in file '" + tmpPropFile + "'.");
		_logger.info("Combined Configuration Search Order '" + StringUtil.toCommaStr(Configuration.getSearchOrder()) + "'.");

		// If the config file exists, read it, if not create empty
		if ( (new File(propFile)).exists() )
		{
			_logger.info("Using configuration file '" + propFile + "'.");
			Configuration appProps = new Configuration(propFile);
			Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);
		}
		else
		{
			_logger.info("The configuration file '" + propFile + "', did not exists, starting with an empty configuration.");
			Configuration appProps = new Configuration();
			Configuration.setInstance(Configuration.SYSTEM_CONF, appProps);
		}
		_logger.info("----------------------------------------------------------------------------------------");


		// Start a separate JVM with an OLD H2 version (instead of fiddling with class loaders)
		//   - Get the CLASSPATH and change it to use "_sourceH2Jar" the specified JAR 
		//   - modify URL (if it points to a FILE, to use a TCP)
		//   - Start a SUB Processes (remember the PID so we can kill it later, if we can't do a normal shutdown)
		if (StringUtil.hasValue(_sourceH2Jar))
		{
			_logger.info("You specified a H2 JAR to 'migration/upgrade' an old H2 database from.");
			_logger.info(" *** Starting a separate JVM to hold the old H2 database.");

			startOldH2TcpServer();

			_logger.info("----------------------------------------------------------------------------------------");
		}
	}
	
	private void startOldH2TcpServer() 
	throws IOException
	{
		String systemClassPath = System.getProperty("java.class.path");
		systemClassPath = systemClassPath.replace(File.pathSeparatorChar, ',');

		List<String> systemClassPathList = StringUtil.parseCommaStrToList(systemClassPath, true);
		List<String> newSystemClassPathList = new ArrayList<>();
		for (String entry : systemClassPathList)
		{
			String tmpFilename = FilenameUtils.getName(entry);
			if (tmpFilename.startsWith("h2-") && tmpFilename.endsWith(".jar"))
			{
				newSystemClassPathList.add(_sourceH2Jar);
				_logger.info("Replacing migration CLASSPATH entry from '" + entry + "' to '" + _sourceH2Jar + "'.");
			}
			else
			{
				newSystemClassPathList.add(entry);
			}
		}
		
//		Map<String, String> systemEnvMap = System.getenv(); 
		
		// java org.h2.tools.Server -tcp -tcpAllowOthers -ifExists
//		List<String> cmd = Arrays.asList(new String[]{"java", "org.h2.tools.Server", "-tcp", "-tcpAllowOthers", "-ifExists"});
		
//		final ProcessBuilder pb = new ProcessBuilder("java", "org.h2.tools.Server", "-tcp", "-tcpAllowOthers", "-ifExists");
		final ProcessBuilder pb = new ProcessBuilder("java", "org.h2.tools.Server", "-tcp", "-ifExists");
//		pb.inheritIO();
		pb.redirectErrorStream(true);

		Map<String, String> pbEnv = pb.environment();
		pbEnv.put("CLASSPATH", StringUtil.toCommaStr(newSystemClassPathList, File.pathSeparatorChar + ""));

		
		_logger.info("About to start, H2 Migration Server (JVM). Using the following comand: " + pb.command());
		_logger.info("  *** Command:     " + pb.command());
		_logger.info("  *** CWD:         " + pb.directory());
//		_logger.info("  *** CLASSPATH:   " + newSystemClassPathList);
		_logger.info("  *** Environment: ");
		for (Entry<String, String> e : pb.environment().entrySet())
		{
			_logger.info("      * " + StringUtil.left(e.getKey(), 30) + " = " + e.getValue());
			
		}
		_logger.info("  *** CLASSPATH:");
		List<String> tmpClasspathList = StringUtil.parseCommaStrToList(pb.environment().get("CLASSPATH").replace(File.pathSeparatorChar, ','), true);
		for (String e : tmpClasspathList)
		{
			_logger.info("      * " + e);
		}

		_logger.info("Starting H2 Migration Server (JVM). Using the following comand: " + pb.command());
		_migrationH2Srv = pb.start();
		
		// If we want to read output of the OS Command, and print it to the errorlog...
		//pb.inheritIO(); <<<---- Comment out the above
		// If we don't read the stream(s) from Process, it may simply not "start" or start slowly
		Thread osOutputReaderThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				BufferedReader bReader = new BufferedReader(new InputStreamReader(_migrationH2Srv.getInputStream()));
				String line;
				try
				{
					while ((line = bReader.readLine()) != null) 
					{
						_logger.info(Thread.currentThread().getName() + " -- OUTPUT >>>>>>>>>: " + line);
					}
				}
				catch (IOException ex)
				{
					_logger.error("Problems reading 'H2 Migration Server (JVM) OUTPUT'.", ex);
				}
				_logger.error("STOPPED: thread: " + Thread.currentThread().getName());
			}
		});
		osOutputReaderThread.setDaemon(true);
		osOutputReaderThread.setName("OS-H2 Migration Server (JVM)");
		osOutputReaderThread.start();
	}

	private void stopOldH2TcpServer()
	{
		if (_migrationH2Srv != null)
		{
			// Copy to a dummy variable and reset (so we don't TRY stop it several times)
			Process p = _migrationH2Srv;
			_migrationH2Srv = null;

			_logger.info("Stopping H2 Migration Server (JVM).");
			p.destroy();

			_logger.info("Waiting for H2 Migration Server (JVM) to STOP.");
			try 
			{ 
				p.wait(); 
				_logger.info("Stopped for H2 Migration Server.");
			}
			catch (Exception ex) 
			{
				_logger.info("While waiting for H2 Migration Server, to STOP, we caught exception, skipping this and continuing. Caught:" + ex);
			}
		}
	}
	
	public void doWork()
	throws Exception
	{
		long startTime = System.currentTimeMillis();

		// get table list from SOURCE
		// get approx rowcount
		_tableList    = getTables(_sourceConn);
		List<DbTable> targetPreTableList = getTables(_targetConn);
		
		// Count records in the target table... 
		// and SET that in the SOURCE.targetPreRowCount
		// which will be used at a later stage
		if (targetPreTableList.size() > 0)
		{
//			for (DbTable dbt : targetPreTableList)
//			{
//				dbt.targetPreRowCount = getTargetTableRowCount(dbt);
//			}
			for (DbTable dbt : targetPreTableList)
			{
				dbt.targetPreRowCount = _targetConn.getRowCountEstimate(dbt.catalog, dbt.schema, dbt.name);
			}

			// try to find the entry in the SOURCE LIST and update 'targetPreRowCount'
			for (DbTable dbt : targetPreTableList)
			{
				DbTable sourceDbt = null;
				for (DbTable sDbt : _tableList)
				{
					if (sDbt.schema.equals(dbt.schema) && sDbt.name.equals(dbt.name))
					{
						sourceDbt = sDbt;
						break;
					}
				}
				
				if (sourceDbt != null)
					sourceDbt.targetPreRowCount = dbt.targetPreRowCount;
			}
		}

		// Get schema names from target (used in fixDbxCentralMetaDataTablesAtTarget())
//		if (targetPreTableList.size() > 0)
//		{
//			for (DbTable dbt : targetPreTableList)
//			{
//				if ( ! dbCopyParams.schemaList.contains(dbt.schema) )
//					dbCopyParams.schemaList.add(dbt.schema);
//			}
//		}
//		dbCopyParams.schemaList.remove("PUBLIC");

    
		// do work
		transferDdl();
		transferData();
		
		//
		boolean hasErrors = printErrorReport();
		
		//
		if (hasErrors || _dmlUseMerge)
			fixDbxCentralMetaDataTablesAtTarget();

		// Shutdown H2 and 
//		ifH2_doShutdownDefrag(_targetConn);
		
		// close everything
		//close(); // done in outer Auto-close
		
		String execTimeStr  = TimeUtils.msToTimeStrDHMS( TimeUtils.msDiffNow(startTime) );
		String startTimeStr = TimeUtils.toString(startTime);
		String stopTimeStr  = TimeUtils.toString(System.currentTimeMillis());

		_logger.info("");
		_logger.info("==============================================================");
		_logger.info(" Execution Time");
		_logger.info("==============================================================");
		_logger.info(" Start Time: " + startTimeStr);
		_logger.info("   End Time: " + stopTimeStr);
		_logger.info(" Total Time: " + execTimeStr);
		_logger.info("==============================================================");
		_logger.info("");
	}
	
	private void printExecutionReport()
	{
		System.out.println("");
		System.out.println("-------------------------------------------------------------");
		System.out.println("User Input:");
		System.out.println("-------------------------------------------------------------");
		if (StringUtil.hasValue(_sourceH2Jar))
		{
			System.out.println("  Source JAR    : '" + _sourceH2Jar + "'");
			System.out.println("");

			// Check if the JAR exists
			if (StringUtil.hasValue(_sourceH2Jar))
			{
				File f = new File(_sourceH2Jar);
				if ( ! f.exists() )
				{
					System.out.println("  ###########################################################");
					System.out.println("  #### ERROR #### ERROR #### ERROR #### ERROR #### ERROR ####");
					System.out.println("  ###########################################################");
					System.out.println("  >>>>> The specified H2 JAR file '" + _sourceH2Jar + "' DO NOT EXISTS! <<<<<");
					System.out.println("");
				}
			}
		}
		System.out.println("  Source User   : '" + _sourceUser   + "'");
		System.out.println("  Source Passwd : '" + _sourcePasswd + "'");
		System.out.println("  Source URL    : '" + _sourceUrl    + "'");
		if (_sourceUrl.startsWith("jdbc:h2:file:"))
			System.out.println("  H2 File Exists: " + (_sourceH2File == null ? false : _sourceH2File.exists()) + ", name='" + _sourceH2File + "'");
		System.out.println("");
		System.out.println("  Target User   : '" + _targetUser   + "'");
		System.out.println("  Target Passwd : '" + _targetPasswd + "'");
		System.out.println("  Target URL    : '" + _targetUrl    + "'");   
		if (_targetUrl.startsWith("jdbc:h2:file:"))
			System.out.println("  H2 File Exists: " + (_targetH2File == null ? false : _targetH2File.exists()) + ", name='" + _targetH2File + "'");
		System.out.println("");
		System.out.println("  Options:");
		System.out.println("    ddlPrintExec  : " + _ddlPrintExec );
		System.out.println("    ddlSkipErrors : " + _ddlSkipErrors);
		System.out.println("    dmlBatchSize  : " + _dmlBatchSize );
		System.out.println("    dmlUseMerge   : " + _dmlUseMerge  );
		System.out.println("-------------------------------------------------------------");
		System.out.println("NOTE: For the moment this only works for H2 database");
		System.out.println(" - to execute specify '-e' or '--exec' switch.");
		System.out.println(" - or specify '-h' to get help on switches.");
		System.out.println("Exiting...");
		System.out.println("");
	}

	/**
	 * Also checks if there was any errors...
	 * @param dbcp
	 * @return true on errors, or false on NO-Errors
	 */
	private boolean printErrorReport()
	{
		_logger.info("");

		int errorCount = 0;
		for (DbTable dbt : _tableList)
		{
			if ( dbt.hasProblems() )
				errorCount++;
		}
		
		if (errorCount == 0)
		{
			_logger.info("--------------------------------------------------");
			_logger.info("No errors was found during the transfer");
			_logger.info("--------------------------------------------------");
			return false;
		}
		else
		{
			_logger.error("##############################################################");
			_logger.error(errorCount + " errors was found during the transfer");
			_logger.error("##############################################################");
			for (DbTable dbt : _tableList)
			{
				// Check messages
				for (String msg : dbt.errorMsgList)
				{
					String msg1 = String.format("Transfer Error on: schema=%s, name=%-40.40s : %s",
						dbt.schema, 
						dbt.name, 
						msg.trim());

					_logger.error(msg1);
				}
				
				// Check Exceptions
				if (dbt.exception != null)
				{
					String msg1 = String.format("Transfer Error on: schema=%s, name=%-40.40s : %s",
							dbt.schema, 
							dbt.name, 
							"EXCEPTION: " + dbt.exception);

					_logger.error(msg1, dbt.exception);
				}
			}
			_logger.error("##############################################################");
			return true;
		}
	}

	
	// maybe use some parts of:
	//     https://github.com/swissmanu/JDBCCopier/blob/master/src/me/alabor/jdbccopier/database/Database.java
	//     https://github.com/vijayan007/data-copier/tree/master/data-copier/src/main/java/com/github/vijayan007
	private void connect()
	throws Exception
	{
		_sourceConn = connect(Type.Source, _sourceUrl, _sourceUser, _sourcePasswd);
		_targetConn = connect(Type.Target, _targetUrl, _targetUser, _targetPasswd);

	}
	
	private DbxConnection connect(Type type, String url, String user, String passwd)
	throws Exception
	{
		ConnectionProp cp = new ConnectionProp();
		cp.setUsername(user);
		cp.setPassword(passwd);
		cp.setUrl(url);
		cp.setAppName(H2CentralDbCopy3.class.getSimpleName() + ": "+type);
		
		_logger.info("Connecting to '" + type + "', with user '" + user + "', using url '" + url + "'.");
//		return DbxConnection.createDbxConnection( DriverManager.getConnection(url, user, passwd) );
		DbxConnection conn = DbxConnection.connect(null, cp);
		
		// print the Version
		String dbmsVendor  = conn.getDatabaseProductName();
//		String dbmsVersion = conn.getDatabaseProductVersion();
//		String dbInfo      = getH2DbInfo(conn);
//		_logger.info("Connected to DBMS Product '" + dbmsVendor + "', using Version '" + dbmsVersion + "', H2 Info '" + dbInfo + "'.");

		if (DbUtils.isProductName(dbmsVendor, DbUtils.DB_PROD_NAME_H2))
		{
			String dbInfo      = getH2DbInfo(conn);
			_logger.info("Connected to H2 Info '" + dbInfo + "'.");
		}

		return conn;
	}
	
	private String getH2DbInfo(DbxConnection conn) 
	throws SQLException
	{
		try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery("select SESSION_ID(), DATABASE_PATH(), H2VERSION()") )
		{
			while (rs.next())
			{
				String sid   = rs.getString(1);
				String path  = rs.getString(2);
				String h2ver = rs.getString(3);
//				if (path == null)
//					path = "-H2-UNKNOWN-DATABASE_PATH-";

				return "H2VERSION()=" + h2ver + ", SESSION_ID()=" + sid + ", DATABASE_PATH()=" + path;
			}
		}
		return "-UNKNOWN-";
	}

	private void preCheck()
	{
		String sql;
		
		// Get SOURCE DbxCentral DB Version
		_sourceDbxCentralDbVersion = -1;
		sql = _sourceConn.quotifySqlString("select [DbVersion] from [PUBLIC].[DbxCentralVersionInfo] where [ProductString] = 'DbxTuneCentral'");
		try (Statement stmnt = _sourceConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_sourceDbxCentralDbVersion = rs.getInt(1);
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting SOURCE DbxCentral DbVersion using sql='" + sql + "'. caught: " + ex);
		}
		_logger.info("SOURCE DbxCentral DbVersion = " + _sourceDbxCentralDbVersion);

		// Get TARGET DbxCentral DB Version
		_targetDbxCentralDbVersion = -1;
		sql = _sourceConn.quotifySqlString("select [DbVersion] from [PUBLIC].[DbxCentralVersionInfo] where [ProductString] = 'DbxTuneCentral'");
		try (Statement stmnt = _sourceConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
		{
			while(rs.next())
				_targetDbxCentralDbVersion = rs.getInt(1);
		}
		catch(SQLException ex)
		{
			//_logger.info("Problems getting TARGET DbxCentral DbVersion using sql='" + sql + "'. caught: " + ex);
		}
		if (_targetDbxCentralDbVersion >= 0)
		{
			_logger.info("TARGET DbxCentral DbVersion = " + _targetDbxCentralDbVersion);
		
			if (_sourceDbxCentralDbVersion != _targetDbxCentralDbVersion)
			{
			}
		}
		
		
	}

//	private void transferDdl()
//	throws Exception
//	{
//		if ( ! _sourceConn.isDatabaseProduct(DbUtils.DB_PROD_NAME_H2) )
//			throw new Exception("Sorry for the moment the SOURCE database must be '" + DbUtils.DB_PROD_NAME_H2 + "'. you are connected to '" + _sourceConn.getDatabaseProductName() + "'.");
//		
//		_logger.info("Extracting DDL information from SOURCE: " + _sourceConn.getDbmsServerName());
//		File tmpFile = File.createTempFile("h2.sourcedb.ddl.", ".sql");
//		Script.process(_sourceConn, tmpFile.toString(), "nodata nopasswords nosettings", "");
//		
//		String ddlContent = FileUtils.readFileToString(tmpFile, Charset.defaultCharset());
//		
//		// change: 
//		ddlContent = ddlContent.replace("CREATE CACHED TABLE", "CREATE TABLE IF NOT EXISTS ");
////		ddlContent = ddlContent.replaceAll("(SELECTIVITY \\d+)", ""); // remove SELECTIVITY
//		
//		// change: 
//		ddlContent = ddlContent.replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS ");
//		
////		System.out.println("------------------------------------");
////		System.out.println("------------- DDL BEGIN ------------");
////		System.out.println("------------------------------------");
////		System.out.println(ddlContent);
////		System.out.println("------------------------------------");
////		System.out.println("------------- DDL END --------------");
////		System.out.println("------------------------------------");
//		FileUtils.write(tmpFile, ddlContent, Charset.defaultCharset());
//
//		
//		_logger.info("Applying DDL information to TARGET: " + _targetConn.getDbmsServerName());
//		FileReader reader = new FileReader(tmpFile);
//		
//		executeH2Script(_targetConn, reader, _ddlSkipErrors, _ddlPrintExec);
//	}
	private void transferDdl()
	throws Exception
	{
		_logger.info("Extracting DDL information from SOURCE '" + _sourceConn.getDatabaseProductNameNoThhrow(null) + "' : " + _sourceConn.getDbmsServerName());
		_logger.info("Transforming DDL information to TARGET '" + _targetConn.getDatabaseProductNameNoThhrow(null) + "' : " + _targetConn.getDbmsServerName());


		Map<DdlType, List<String>> ddlMap = DbmsDdlUtils.getDdlFor(_sourceCatalog, _targetConn);
		
		List<String> ddlSchemas = ddlMap.get(DdlType.SCHEMA);
		List<String> ddlTables  = ddlMap.get(DdlType.TABLE);
		List<String> ddlIndexes = ddlMap.get(DdlType.INDEX);

		List<String> ddlList    = new ArrayList<>();
		ddlList.addAll(ddlSchemas);
		ddlList.addAll(ddlTables);
		ddlList.addAll(ddlIndexes);
		
		_logger.info("Applying DDL " + ddlList.size() + " information to TARGET: "+_targetConn.getDbmsServerName());

		for (String ddl : ddlList)
		{
			ddl = _targetConn.quotifySqlString(ddl);
			
			executeDdl(_targetConn, ddl, _ddlSkipErrors, _ddlPrintExec);
		}
		
//		AseSqlScriptReader sr = new AseSqlScriptReader(ddlContent, false, "go");
//		script = new AseSqlScript(conn, getSqlTimeout(), getKeepDbmsState(), getDiscardDbmsErrorList()); 
//		executeH2Script(_targetConn, reader, _ddlSkipErrors, _ddlPrintExec);

	}

	public boolean executeDdl(DbxConnection conn, String sql, boolean skipErrors, boolean printExecution) 
	throws SQLException
	{
		if (StringUtil.isNullOrBlank(sql))
			return true;

		try
		{
			SQLWarning sqlw  = null;
			Statement stmnt  = conn.createStatement();
			ResultSet  rs    = null;
			int rowsAffected = 0;

//			if (_queryTimeout > 0)
//				stmnt.setQueryTimeout(_queryTimeout);

			if (_logger.isDebugEnabled()) 
				_logger.debug("EXECUTING: " + sql);
			
			if (printExecution)
				_logger.info("---- EXECUTING: -------------------------------------------------------------\n" + sql);

//			stmnt.executeUpdate(sql);
			boolean hasRs = stmnt.execute(sql);

			// iterate through each result set
			do
			{
				if(hasRs)
				{
					// Get next resultset to work with
					rs = stmnt.getResultSet();

					// Convert the ResultSet into a TableModel, which fits on a JTable
					ResultSetTableModel tm = new ResultSetTableModel(rs, true, sql, sql);

					// Write ResultSet Content as a "string table"
					_logger.info("executeDdl: produced a ResultSet\n" + tm.toTableString());
//					_resultCompList.add(tm);

					// Check for warnings
					// If warnings found, add them to the LIST
					for (sqlw = rs.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
					{
						_logger.trace("--In loop, sqlw: " + sqlw);
						//compList.add(new JAseMessage(sqlw.getMessage()));
					}

					// Close it
					rs.close();
				}
				else
				{
					// Treat update/row count(s)
					rowsAffected = stmnt.getUpdateCount();
					if (rowsAffected >= 0)
					{
//						rso.add(rowsAffected);
					}
				}

				// Check if we have more resultsets
				hasRs = stmnt.getMoreResults();

				_logger.trace( "--hasRs=" + hasRs + ", rowsAffected=" + rowsAffected );
			}
			while (hasRs || rowsAffected != -1);

			// Check for warnings
			for (sqlw = stmnt.getWarnings(); sqlw != null; sqlw = sqlw.getNextWarning())
			{
				_logger.trace("====After read RS loop, sqlw: " + sqlw);
				//compList.add(new JAseMessage(sqlw.getMessage()));
			}

			// Close the statement
			stmnt.close();
		}
		catch(SQLWarning w)
		{
			_logger.warn("Problems when executing sql: " + sql, w);
//			PluginSupport.LogInfoMessage(sqlwarning.getMessage(), MessageText.formatSQLExceptionDetails(sqlwarning));
		}
		catch (SQLException ex)
		{
			if (skipErrors)
			{
				String msg = ex.getMessage();
//				System.out.println("MSG=|" + msg + "|.");

				Pattern pattern = Pattern.compile("Constraint .* already exists");
				Matcher matcher = pattern.matcher(msg);

				if ( ! matcher.find() )
					_logger.info("SKIPPING ERROR '" + ex + "': when executing sql: " + sql);
			}
			else
			{
				throw ex;
			}
			return false;
		}
		return true;
	}

	private void executeDdl(DbxConnection conn, String sql)
	throws SQLException
	{
		try (Statement stmnt = conn.createStatement())
		{
			stmnt.executeUpdate(sql);
		}
	}

//	public ResultSet executeH2Script(Connection conn, Reader reader, boolean skipErrors, boolean printExecution) 
//	throws SQLException
//	{
//		// can not close the statement because we return a result set from it
//		Statement stmnt = conn.createStatement();
//		ResultSet rs = null;
//		ScriptReader r = new ScriptReader(reader);
//		while (true)
//		{
//			String sql = r.readStatement();
//			if ( sql == null )
//			{
//				break;
//			}
//			if ( sql.trim().length() == 0 )
//			{
//				continue;
//			}
//			try
//			{
//				if (printExecution)
//					_logger.info("ExecuteH2Script: " + sql);
//
//    			boolean resultSet = stmnt.execute(sql);
//    			if ( resultSet )
//    			{
//    				if ( rs != null )
//    				{
//    					rs.close();
//    					rs = null;
//    				}
//    				rs = stmnt.getResultSet();
//    			}
//			}
//			catch (SQLException ex)
//			{
//				if (skipErrors)
//				{
//					String msg = ex.getMessage();
////					System.out.println("MSG=|" + msg + "|.");
//
//					Pattern pattern = Pattern.compile("Constraint .* already exists");
//					Matcher matcher = pattern.matcher(msg);
//
//					if ( ! matcher.find() )
//						_logger.info("SKIPPING ERROR '" + ex + "': when executing sql: " + sql);
//				}
//				else
//				{
//					r.close();
//					throw ex;
//				}
//			}
//		}
//		r.close();
//		return rs;
//	}
	
	private void transferData()
	throws Exception
	{
		_logger.info("Copy DATA table by table from SOURCE: " + _sourceConn.getDbmsServerName() + " TO: " + _targetConn.getDbmsServerName());
		long transferStartTime = System.currentTimeMillis();
		
		// Stats: how much are we about todo
		long totalSourceTableCount       = 0;
		long totalSourceRowCountEstimate = 0;
		long totalDoneTableCount         = 0;
		long totalDoneRowCount           = 0;
		for (DbTable dbt : _tableList)
		{
			totalSourceTableCount++;
			totalSourceRowCountEstimate += dbt.sourceRowCountEstimate;
		}
		
		for (DbTable dbt : _tableList)
		{
//			if (dbt.name.startsWith("Dbx"))
//			{
//				_logger.info("");
//				String msg1 = String.format("Skipping: schema=%s, name=%-40.40s   ... This is a DbxCentral Metadata table, which will be handled later.", dbt.schema, dbt.name);
//				_logger.info(msg1);
//				continue;
//			}
			
			_logger.info("");
			String msg1 = String.format("Transfer: schema=%s, name=%-40.40s, estRows=%9d, dmlBatchSize=%d, dmlUseMerge=%b",
					dbt.schema, 
					dbt.name, 
					dbt.sourceRowCountEstimate,
					_dmlBatchSize,
					_dmlUseMerge);
			_logger.info(msg1);

			
			boolean createTargetTable = false;
			int     batchSize         = _dmlBatchSize;
			
			try
			{
				long startTime = System.currentTimeMillis();
				
				/////////////////////////////////////
				// DO TRANSFER of one table
				/////////////////////////////////////
				transferTable(dbt, createTargetTable, batchSize);

				totalDoneTableCount++;
				totalDoneRowCount += dbt.sourceRowCountEstimate;
				
				// Check various things...
				// NOTE: We should probably do special checks for DbxCentral SYSTEM Tables (or tables starting with 'Dbx')
				boolean ok = true;
				if (dbt.sourceReadCount != dbt.targetWriteCount)
				{
					ok = false;
					String status = "SUSPECT WRITE-COUNT";
					String stMsg  = "sourceReadCount[" + dbt.sourceReadCount + "] != targetWriteCount[" + dbt.targetWriteCount + "], ";
					
					//                   format("Transfer: ...",
					String msg2 = String.format("          %s - %s", status, stMsg);
					_logger.warn(msg2);

					dbt.errorType = ErrorType.SUSPECT_WRITE_COUNT;
					dbt.errorMsgList.add(msg2);
				}

				long targetRowCount = dbt.targetPostRowCount;
				if (dbt.targetPreRowCount >= 0)
					targetRowCount = dbt.targetPostRowCount - dbt.targetPreRowCount;
				
				if (dbt.sourceReadCount != targetRowCount)
				{
					ok = false;
					String status = "SUSPECT: ROW-COUNT";
					String stMsg  = "sourceReadCount[" + dbt.sourceReadCount + "] != targetRowCount[" + targetRowCount + "] (targetPreRowCount=" + dbt.targetPreRowCount + ", targetPostRowCount=" + dbt.targetPostRowCount + "), ";

					String guessedMergeCountStr = "";
					if (_dmlUseMerge)
					{
						long guessedMergeCount = dbt.sourceReadCount - targetRowCount;
						guessedMergeCountStr = "Guessing we MERGED " + guessedMergeCount + " rows from source into target (using PK=FIXME), which means that " + guessedMergeCount + " rows in the target was overwritten with data from the source, based on: guessedMergeCount = sourceReadCount - (targetPostRowCount - targetPreRowCount)";
					}
					
					//                   format("Transfer: ...",
					String msg2 = String.format("          %s - %s %s", status, stMsg, guessedMergeCountStr);
					_logger.warn(msg2);
					
					dbt.errorType = ErrorType.SUSPECT_ROW_COUNT;
					dbt.errorMsgList.add(msg2);
				}
				if (ok)
				{
					dbt.errorType = ErrorType.OK; // OK
					
//					int totalRowPctDone = (int) ( ((totalDoneRowCount  *1.0) / (totalSourceRowCountEstimate*1.0)) * 100.0 );
//					int totalTabPctDone = (int) ( ((totalDoneTableCount*1.0) / (totalDoneTableCount        *1.0)) * 100.0 );

//					double totalRowPctDone = MathUtils.round( ((totalDoneRowCount  *1.0d) / (totalSourceRowCountEstimate*1.0d)) * 100.0d, 1);
					double totalRowPctDone = ((totalDoneRowCount  *1.0d) / (totalSourceRowCountEstimate*1.0d)) * 100.0d;

					// We could try to estimate "END" time, since we have 'totalRowPctDone' here...
					long totalExecTime = TimeUtils.msDiffNow(transferStartTime);
					String estEndTimeStr = "";
					if (totalExecTime > 10 * 1000)
					{
						long estMsToEnd = (long) (totalExecTime * (100.0 / totalRowPctDone)) - totalExecTime;
						//long estEndInMs = transferStartTime + estMsToEnd;
						
						String estEndTimeHMS = "EstTimeLeft=" + TimeUtils.msToTimeStrDHMS(estMsToEnd); // HH:MM:SS
						// estEndTimeHMS.length: 20 = (12+8)   "EstTimeLeft=".length=12   "HH:MM:SS".length=8

						String estEndTime = TimeUtils.toString( System.currentTimeMillis() + estMsToEnd ).substring("yyyy-MM-dd ".length());
						estEndTime = estEndTime.substring(0, "HH:MM:SS".length());

						estEndTimeStr = estEndTimeHMS + " @ " + estEndTime;
						// Length: 31 == 20 + 3 + 8
					}

					
					String execTime = TimeUtils.msDiffNowToTimeStr(startTime);
					
					String spaces = StringUtil.replicate(" ", dbt.schema.length());

					//    format("Transfer: ...",
					String msg2 = String.format("          OK        %-31s %s   -->>  target rows: %9d, execTime: %s, totalPctDone: %.1f%%",
							estEndTimeStr,
							spaces,
							dbt.targetPostRowCount,
							execTime,
							totalRowPctDone);
					_logger.info(msg2);
//					_logger.info("          OK  " + spaces + "                                                 target rows: %8d" + dbt.targetRowCount + ", execTime: " + execTime + ", totalPctDone: " + totalRowPctDone + "%");
				}
			}
			catch (SourceException ex)
			{
				dbt.errorType = ErrorType.HAS_EXCEPTION;
				dbt.exception = ex;
				
				_logger.error("SOURCE Problems when transfer: schema='" + dbt.schema + "', name='" + dbt.name + ". ex=" + ex, ex);
				_logger.error("SOURCE Problems when transfer: schema='" + dbt.schema + "', name='" + dbt.name + ". When it FAILED: sourceEstRows=" + dbt.sourceRowCountEstimate + ", targetWriteCount=" + dbt.targetWriteCount + ", targetRowCount=" + dbt.targetPostRowCount);
				
				// check if connection is alive
				// - maybe reconnect
				if ( ! _sourceConn.isValid(1000) )
				{
					_logger.info("SOURCE: Connection is not valid anymore, trying to re-connect.");
					_sourceConn.reConnect(null);
					_logger.info("SOURCE: re-connect succeeded... SKIPPING transfer of this table (schema='" + dbt.schema + "', name='" + dbt.name + ") and continuing with next table.");
				}
			}
			catch (TargetException ex)
			{
				dbt.errorType = ErrorType.HAS_EXCEPTION;
				dbt.exception = ex;
				
				_logger.error("TARGET Problems when transfer: schema='" + dbt.schema + "', name='" + dbt.name + ". (SKIPPING and continuing with next table) ex=" + ex, ex);
				_logger.error("TARGET Problems when transfer: schema='" + dbt.schema + "', name='" + dbt.name + ". When it FAILED: sourceEstRows=" + dbt.sourceRowCountEstimate + ", targetWriteCount=" + dbt.targetWriteCount + ", targetRowCount=" + dbt.targetPostRowCount);
			}
			catch (Exception ex)
			{
				dbt.errorType = ErrorType.HAS_EXCEPTION;
				dbt.exception = ex;
				
				_logger.error("Unhandled Problems when transfer: schema='" + dbt.schema + "', name='" + dbt.name + ". (SKIPPING and continuing with next table) ex=" + ex, ex);
				_logger.error("Unhandled Problems when transfer: schema='" + dbt.schema + "', name='" + dbt.name + ". When it FAILED: sourceEstRows=" + dbt.sourceRowCountEstimate + ", targetWriteCount=" + dbt.targetWriteCount + ", targetRowCount=" + dbt.targetPostRowCount);
			}
		}
		
		_logger.info("Data Transfer took: " + TimeUtils.msToTimeStrDHMS( TimeUtils.msDiffNow(transferStartTime) ));

		// loop table list
		//	- select *
		//	- batch apply (in 1000) 
	}
	
	/**
	 * A Graph Counter table must 
	 * <ul>
	 *    <li>contain a '_' somewhere</li>
	 *    <li>have more that three columns</li>
	 *    <li>First column must be 'SessionStartTime'</li>
	 *    <li>Second column must be 'SessionSampleTime'</li>
	 *    <li>Third column must be 'CmSampleTime'</li>
	 * </ul>
	 * @param tabname
	 * @param colList
	 * @return
	 */
	private boolean isGraphCounterTable(String tabname, List<String> colList)
	{
		return     tabname.indexOf("_") > 0
				&& colList.size() > 3 
				&& "SessionStartTime".equals(colList.get(0)) 
				&& "SessionSampleTime".equals(colList.get(1)) 
				&& "CmSampleTime".equals(colList.get(2));
	}
	
	private void transferTable(DbTable dbt, boolean createTargetTable, int batchSize)
	throws Exception
	{
		// are we in Source or Target code
		Type atType = Type.Source;

		try
		{
			String sourceSql = _sourceConn.quotifySqlString("select * from [" + dbt.schema + "].[" + dbt.name + "]");
			String targetSql = _targetConn.quotifySqlString("select * from [" + dbt.schema + "].[" + dbt.name + "] where 1=2");

			atType = Type.Source;
			try (Statement stmnt = _sourceConn.createStatement(); ResultSet sourceRs = stmnt.executeQuery(sourceSql))
			{
				int sourceNumCols = -1;
				int targetNumCols   = -1;
				
				// get RSMD from SOURCE
				ArrayList<String>  sourceColNames   = new ArrayList<String>();
				ArrayList<Integer> sourceSqlTypeInt = new ArrayList<Integer>();
				ResultSetMetaData  sourceRsmd       = sourceRs.getMetaData();

				sourceNumCols = sourceRsmd.getColumnCount();
				for(int c=1; c<sourceNumCols+1; c++)
				{
					sourceColNames  .add(sourceRsmd.getColumnLabel(c));
					sourceSqlTypeInt.add(sourceRsmd.getColumnType(c));
				}

				// Check if the table exists in the target database
				// If it doesn't exist, try to create it
				if (createTargetTable)
				{
					atType = Type.Target;

					DatabaseMetaData md = _targetConn.getMetaData();
					ResultSet rs = md.getTables(null, dbt.schema, dbt.name, null);
					int count = 0;
					while (rs.next())
						count++;
					rs.close();

					System.out.println("Result from: RSMD.getTables(); count=" + count);
					if (count == 0)
					{
						// Create a SQL statement like: create table XXX (yyy datatype null/not_null)
						// FIXME
						System.out.println("-NOT-YET-IMPLEMENTED-: Create the target table.");
					}
				}
				
				// Do dummy SQL to get RSMD from TARGET
//				_logger.info("Investigating target table, executing SQL Statement: " + targetSql);

				atType = Type.Target;
				
				Statement targetStmt = _targetConn.createStatement();
				ResultSet targetRs   = targetStmt.executeQuery(targetSql);

				// get RSMD from TARGET
				ArrayList<String>  targetColNames   = new ArrayList<String>();
//				ArrayList<String>  targetColType    = new ArrayList<String>();
//				ArrayList<String>  targetSqlTypeStr = new ArrayList<String>();
				ArrayList<Integer> targetSqlTypeInt = new ArrayList<Integer>();
				ResultSetMetaData  targetRsmd       = targetRs.getMetaData();

				targetNumCols = targetRsmd.getColumnCount();
				for(int c=1; c<targetNumCols+1; c++)
				{
					targetColNames  .add(targetRsmd.getColumnLabel(c));
//					targetColType   .add(targetRsmd.getColumnType(c));
//					targetSqlTypeStr.add(targetRsmd.getColumnLabel(c));
					targetSqlTypeInt.add(targetRsmd.getColumnType(c));
				}
				while (targetRs.next())
				{
				}
				targetRs.close();
				targetStmt.close();
				
				// Check if "transfer" will work
				if (sourceNumCols != targetNumCols)
				{
					// If it's a Graph Counter Table
					if (isGraphCounterTable(dbt.name, targetColNames))
					{
						// If source contains MORE column... alter the table in the Target...
						if (sourceNumCols > targetNumCols)
						{
							List<String> missingCols = new ArrayList<>(sourceColNames);
							missingCols.removeAll(targetColNames);
							
							for (String col : missingCols)
							{
								String sql = _targetConn.quotifySqlString("alter table [" + dbt.schema + "].[" + dbt.name + "] add column [" + col + "] number(16,2) null"); // NOTE: 'not null' is not supported at alter
								_logger.info("FIXING TARGET Table '" + dbt.schema + "." + dbt.name + "' is missing the column '" + col + "'. Adding it using sql: " + sql);

								executeDdl(_targetConn, sql);
							}
							//////////////////////////////////////////////////
							// Refresh the MetatData for the TARGET TABLE
							//////////////////////////////////////////////////
							targetStmt = _targetConn.createStatement();
							targetRs   = targetStmt.executeQuery(targetSql);

							// get RSMD from TARGET
							targetColNames   = new ArrayList<String>();
							targetSqlTypeInt = new ArrayList<Integer>();
							targetRsmd       = targetRs.getMetaData();

							targetNumCols = targetRsmd.getColumnCount();
							for(int c=1; c<targetNumCols+1; c++)
							{
								targetColNames  .add(targetRsmd.getColumnLabel(c));
								targetSqlTypeInt.add(targetRsmd.getColumnType(c));
							}
							while (targetRs.next())
							{
							}
							targetRs.close();
							targetStmt.close();
						}
					}

					if (sourceNumCols != targetNumCols)
					{
						// TODO: should we close the sourceRs or not????
						throw new Exception("Source ResultSet and Target Table does not have the same column count (source=" + sourceNumCols + ", target=" + targetNumCols + ").");
					}
				}
				
				// Make warning if source/target data types does NOT match
				for (int c=0; c<sourceNumCols; c++)
				{
					int sourceType = sourceSqlTypeInt.get(c);
					int targetType = targetSqlTypeInt.get(c);
					
					if (sourceType != targetType)
					{
						// and of course some exceptions from this rule. (NUMERIC & DECIMAL is "same" in most DBMS's)
						if ( (sourceType == Types.NUMERIC && targetType == Types.DECIMAL) || (targetType == Types.NUMERIC && sourceType == Types.DECIMAL) )
							continue;
						
						String sourceJdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(sourceType);
						String targetJdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(targetType);

						String sourceColName = sourceColNames.get(c);
						String targetColName = targetColNames.get(c);

						String warning = "Metadata missmatch: Possible column datatype missmatch for column " + (c+1) + ". Source column name '" + sourceColName + "', jdbcType '" + sourceJdbcTypeStr + "'. Target column name '" + targetColName + "', jdbcType '" + targetJdbcTypeStr + "'. I will still try to do the transfer, hopefully the target server can/will convert the datatype, so it will work... lets try!"; 
						_logger.warn(warning);
					}
				}
				
				
				//-----------------------------------------------------
				// Create a target SQL-DML Statement to execute on the TARGET
				//-----------------------------------------------------
				// Build colStr: (col1, col2, col3...)
				String columnStr = " (";
//				for (String colName : sourceColNames)
				for (String colName : targetColNames)
					columnStr += "[" + colName.replace("]", "]]") + "], ";  // If column name like like 'default data cache[1-pg]' we need do some escaping 'default data cache[1-pg]]' so the end-result will be '[default data cache[1-pg]]]'
				columnStr = columnStr.substring(0, columnStr.length()-2);
				columnStr += ")";
		
				// Build: values(?, ?, ?, ?...)
				String valuesStr = " values(";
				for (int i=0; i<targetNumCols; i++)
					valuesStr += "?, ";
				valuesStr = valuesStr.substring(0, valuesStr.length()-2);
				valuesStr += ")";
				
				// Build insert SQL
				String targetDml;
				boolean doMerge = _dmlUseMerge;
				if (doMerge)
				{
					String mergeKeys = "";
					// looks Like Graph Table
					if (isGraphCounterTable(dbt.name, targetColNames))
						mergeKeys = " KEY([SessionStartTime], [SessionSampleTime], [CmSampleTime]) ";

					targetDml = "merge into [" + dbt.schema + "].[" + dbt.name + "]" + columnStr + mergeKeys + valuesStr;

					// If it's a DbxCentral DICTIONARY Table... then write a special MERGE Statement for some tables (that is "summary" tables, pk=SessionStartTime) 
					boolean doDbxTables = false;
					if (doDbxTables && dbt.name.startsWith("Dbx"))
					{
						// Create a copy of  the sourceColNames so we can manipulate it
						ArrayList<String> soucerColCopy = new ArrayList<>(sourceColNames);
						
//						NOTE: For this to work we also need som "mapper" from SrcCol -> pos, that we can use when doing setObject(colPos, ...)
//						Or it migt be simpler to COUNT record at the end... as a "post work"

						// loop schemas:
						// select count("SessionStartTime")  as "NumOfSamples"
						//        , max("SessionSampleTime") as "LastSampleTime"
						// from "DEV_ASE"."DbxSessionSamples"
						// group by "SessionStartTime"

						if ("PUBLIC".equals(dbt.schema))
						{
							// DbxCentralGraphProfiles - pk(ProductString, UserName, ProfileName)    -- CREATE CACHED TABLE PUBLIC."DbxCentralGraphProfiles"(     "ProductString" VARCHAR(30) NOT NULL,     "UserName" VARCHAR(30) NOT NULL,     "ProfileName" VARCHAR(30) NOT NULL,     "ProfileDescription" TEXT NOT NULL,     "ProfileValue" TEXT NOT NULL,     "ProfileUrlOptions" VARCHAR(1024) )
							// DbxCentralVersionInfo   - pk(ProductString)                           -- CREATE CACHED TABLE PUBLIC."DbxCentralVersionInfo"  (     "ProductString" VARCHAR(30) NOT NULL,     "VersionString" VARCHAR(30) NOT NULL,     "BuildString" VARCHAR(30) NOT NULL,     "DbVersion" INT NOT NULL,     "DbProductName" VARCHAR(30) NOT NULL )
							// DbxCentralSessions      - pk(SessionStartTime)                        -- CREATE CACHED TABLE PUBLIC."DbxCentralSessions"     (     "SessionStartTime" DATETIME NOT NULL,     "Status" INT NOT NULL,     "ServerName" VARCHAR(30) NOT NULL,     "OnHostname" VARCHAR(30) NOT NULL,     "ProductString" VARCHAR(30) NOT NULL,     "VersionString" VARCHAR(30) NOT NULL,     "BuildString" VARCHAR(30) NOT NULL,     "CollectorHostname" VARCHAR(30) NOT NULL,     "CollectorSampleInterval" INT NOT NULL,     "CollectorCurrentUrl" VARCHAR(80),     "CollectorInfoFile" VARCHAR(256),     "NumOfSamples" INT NOT NULL,     "LastSampleTime" DATETIME )

							// "SessionStartTime"        DATETIME      NOT NULL,     PRIMARY KEY
							// "Status"                  INT           NOT NULL,     
							// "ServerName"              VARCHAR(30)   NOT NULL,     
							// "OnHostname"              VARCHAR(30)   NOT NULL,     
							// "ProductString"           VARCHAR(30)   NOT NULL,     
							// "VersionString"           VARCHAR(30)   NOT NULL,     
							// "BuildString"             VARCHAR(30)   NOT NULL,     
							// "CollectorHostname"       VARCHAR(30)   NOT NULL,     
							// "CollectorSampleInterval" INT           NOT NULL,     
							// "CollectorCurrentUrl"     VARCHAR(80)       NULL,     
							// "CollectorInfoFile"       VARCHAR(256)      NULL,     
							// "NumOfSamples"            INT           NOT NULL,     - This fields needs to be special merged (SOURCE + TARGET)
							// "LastSampleTime"          DATETIME          NULL      - Probably MAX value of them
							if (dbt.name.startsWith("DbxCentralSessions"))
							{
								// remove PK Columns from soucerColCopy
								soucerColCopy.remove("SessionStartTime");

								// remove columns that we handle manually (special treatment) from soucerColCopy
								soucerColCopy.remove("NumOfSamples");
								soucerColCopy.remove("LastSampleTime");
								
								StringBuilder sb = new StringBuilder();
								sb.append("MERGE into [PUBLIC].[DbxCentralSessions] T \n");
								sb.append("USING ( select \n");
								sb.append("               ? as [SessionStartTime] \n"); // PK
								sb.append("             , ? as [NumOfSamples] \n");
								sb.append("             , ? as [LastSampleTime] \n");
								for (Object colName : soucerColCopy)
								{
									sb.append("            , ? as [" + colName + "] \n");
								}
								sb.append("      ) S \n");
								sb.append("ON (T.[SessionStartTime] = S.[SessionStartTime]) \n");
								sb.append("\n");
								sb.append("WHEN MATCHED THEN \n");
								sb.append("UPDATE set \n");
								sb.append("           T.[NumOfSamples]   = T.[NumOfSamples] + S.[NumOfSamples] \n");
								sb.append("         , T.[LastSampleTime] = CASE WHEN S.[LastSampleTime] >= T.[LastSampleTime] THEN S.[LastSampleTime] ELSE T.[LastSampleTime] END \n"); // do some kind of: max(S.LastSampleTime, T.LastSampleTime)
								for (Object colName : soucerColCopy)
								{
									sb.append("         , T.[" + colName + "] = S.[" + colName + "] \n");
								}
								sb.append("\n");
								sb.append("WHEN NOT MATCHED THEN \n");
								sb.append("INSERT \n");
								sb.append("( \n");
								sb.append("     [SessionStartTime] \n"); // PK
								sb.append("   , [NumOfSamples] \n");
								sb.append("   , [LastSampleTime] \n");
								for (Object colName : soucerColCopy)
								{
									sb.append("    , [" + colName + "] \n");
								}
								sb.append(") \n");
								sb.append("VALUES \n");
								sb.append("( \n");
								sb.append("     S.[SessinStartTime] \n"); // PK
								sb.append("   , S.[NumOfSamples] \n");
								sb.append("   , S.[LastSampleTime] \n");
								for (Object colName : soucerColCopy)
								{
									sb.append("   , S.[" + colName + "] \n");
								}
								sb.append(") \n");

								// For simplicity/readability, I used '#' as the quoted identifier...
								// Now change the Quoted Identifier to the character used to do DBMS Specific Quote char
//								targetDml = sb.toString().replace('#', q);
								
//								\prep merge into t1 T 
//								using (select cast(? as int) as id, cast(? as number(16,2)) as c1, cast(? as number(16,2)) as c2, cast(? as number(16,2)) as c3) S 
//								on (T.id = S.id) 
//								WHEN     MATCHED THEN update set T.c1 = T.c1 + S.c1
//								                               , T.c2 = T.c2 + S.c2
//								                               , T.c3 = T.c3 + S.c3 
//								WHEN NOT MATCHED THEN insert (id, c1,c2,c3) values(S.id, S.c1, S.c2, S.c3) 
//								:( int = 6, numeric = 1.1, numeric = 2.2, numeric = 3.3 )
								
							}
						}
						else
						{
							// DbxGraphProperties        - pk(SessionStartTime, GraphName)                  -- CREATE CACHED TABLE PROD_B1_ASE."DbxGraphProperties"      (     "SessionStartTime" DATETIME NOT NULL,     "CmName" VARCHAR(30) NOT NULL,     "GraphName" VARCHAR(30) NOT NULL,     "TableName" VARCHAR(128) NOT NULL,     "GraphLabel" VARCHAR(255) NOT NULL,     "GraphCategory" VARCHAR(30) NOT NULL,     "isPercentGraph" INT NOT NULL,     "visibleAtStart" INT NOT NULL,     "initialOrder" INT NOT NULL )
							// DbxSessionSampleDetailes  - pk(SessionSampleTime, CmName, SessionStartTime)  -- CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleDetailes"(     "SessionStartTime" DATETIME NOT NULL,     "SessionSampleTime" DATETIME NOT NULL,     "CmName" VARCHAR(30) NOT NULL,     "type" INT,     "graphRecvCount" INT,     "absRecvRows" INT,     "diffRecvRows" INT,     "rateRecvRows" INT,     "graphSaveCount" INT,     "absSaveRows" INT,     "diffSaveRows" INT,     "rateSaveRows" INT,     "sqlRefreshTime" INT,     "guiRefreshTime" INT,     "lcRefreshTime" INT,     "nonCfgMonHappened" INT,     "nonCfgMonMissingParams" VARCHAR(100),     "nonCfgMonMessages" VARCHAR(1024),     "isCountersCleared" INT,     "hasValidSampleData" INT,     "exceptionMsg" VARCHAR(1024),     "exceptionFullText" TEXT )
							// DbxSessionSamples         - pk(SessionSampleTime, SessionStartTime)          -- CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSamples"       (     "SessionStartTime" DATETIME NOT NULL,     "SessionSampleTime" DATETIME NOT NULL )
							// DbxSessionSampleSum       - pk(SessionStartTime, CmName)                     -- CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleSum"     (     "SessionStartTime" DATETIME NOT NULL,     "CmName" VARCHAR(30) NOT NULL,     "graphSamples" INT,     "absSamples" INT,     "diffSamples" INT,     "rateSamples" INT )
							// DbxAlarmActive            - pk(                   alarmClass, serviceType, serviceName, serviceInfo, extraInfo)
							// DbxAlarmHistory           - pk(eventTime, action, alarmClass, serviceType, serviceName, serviceInfo, extraInfo)

						
							// ------- DbxGraphProperties        - pk(SessionStartTime, GraphName)                  -- 
							// CREATE CACHED TABLE PROD_B1_ASE."DbxGraphProperties"      (     
							// 	*	"SessionStartTime" DATETIME     NOT NULL,     
							// 		"CmName"           VARCHAR(30)  NOT NULL,     
							// 	*	"GraphName"        VARCHAR(30)  NOT NULL,     
							// 		"TableName"        VARCHAR(128) NOT NULL,     
							// 		"GraphLabel"       VARCHAR(255) NOT NULL,     
							// 		"GraphCategory"    VARCHAR(30)  NOT NULL,     
							// 		"isPercentGraph"   INT          NOT NULL,     
							// 		"visibleAtStart"   INT          NOT NULL,     
							// 		"initialOrder"     INT          NOT NULL )
							// ------- DbxSessionSampleDetailes  - pk(SessionSampleTime, CmName, SessionStartTime)  -- 
							// CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleDetailes"(     
							// 	*	"SessionStartTime"       DATETIME    NOT NULL,     
							// 	*	"SessionSampleTime"      DATETIME    NOT NULL,     
							// 	*	"CmName"                 VARCHAR(30) NOT NULL,     
							// 		"type"                   INT,     
							// 		"graphRecvCount"         INT,     
							// 		"absRecvRows"            INT,     
							// 		"diffRecvRows"           INT,     
							// 		"rateRecvRows"           INT,     
							// 		"graphSaveCount"         INT,     
							// 		"absSaveRows"            INT,    
							// 		"diffSaveRows"           INT,     
							// 		"rateSaveRows"           INT,     
							// 		"sqlRefreshTime"         INT,     
							// 		"guiRefreshTime"         INT,     
							// 		"lcRefreshTime"          INT,     
							// 		"nonCfgMonHappened"      INT,     
							// 		"nonCfgMonMissingParams" VARCHAR(100),     
							// 		"nonCfgMonMessages"      VARCHAR(1024),     
							// 		"isCountersCleared"      INT,     
							// 		"hasValidSampleData"     INT,     
							// 		"exceptionMsg"           VARCHAR(1024),     
							// 		"exceptionFullText"      TEXT )
							// ------- DbxSessionSamples         - pk(SessionSampleTime, SessionStartTime)          -- 
							// CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSamples"       (     
							// 	*	"SessionStartTime"  DATETIME NOT NULL,     
							// 	*	"SessionSampleTime" DATETIME NOT NULL )
							// ------- DbxSessionSampleSum       - pk(SessionStartTime, CmName)                     -- 
							// CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleSum"     (     
							// 	*	"SessionStartTime" DATETIME NOT NULL,     
							// 	*	"CmName" VARCHAR(30) NOT NULL,     
							// 		"graphSamples" INT,     
							// 		"absSamples" INT,     
							// 		"diffSamples" INT,     
							// 		"rateSamples" INT )
							// ------- DbxAlarmActive            - pk(                   alarmClass, serviceType, serviceName, serviceInfo, extraInfo)
							// ------- DbxAlarmHistory           - pk(eventTime, action, alarmClass, serviceType, serviceName, serviceInfo, extraInfo)
						
						}
					}
				}
				else
				{
					targetDml = "insert into [" + dbt.schema + "].[" + dbt.name + "]" + columnStr + valuesStr;
				}
				targetDml = _targetConn.quotifySqlString(targetDml);
				_logger.debug("TARGET SQL Statement: " + targetDml);


				// Create the Prepared Statement
				atType = Type.Target;
				PreparedStatement pstmt = _targetConn.prepareStatement(targetDml);
				
				int totalCount = 0;
				int batchCount = 0;

//				long dataTransferStartTime   = System.currentTimeMillis();
//				long dataTransferLastMsgTime = System.currentTimeMillis();
				TimeUtils.timeExpiredSetStartTime("dataTransferLastMsgTime");
				
				atType = Type.Source;
				while (sourceRs.next())
				{
					batchCount++;
					totalCount++;
					
					dbt.sourceReadCount++;

					// for each column in source set it to the output
					for (int c=1; c<sourceNumCols+1; c++)
					{
						try
						{
							atType = Type.Source;
							Object obj = sourceRs.getObject(c);
//							if (obj == null) System.out.println("DATA for column c=" + c + ", sourceName='" + sourceColNames.get(c-1) + "'. is NULL: sourceRs.getObject(c)");

							atType = Type.Target;
							if (obj != null)
								pstmt.setObject(c, obj, targetSqlTypeInt.get(c-1));
							else
								pstmt.setNull(c, targetSqlTypeInt.get(c-1));
						}
						catch (SQLException sqle)
						{
							_logger.warn("ROW: " + totalCount + " - Problems setting column c=" + c + ", sourceName='" + sourceColNames.get(c-1) + "', targetName='" + targetColNames.get(c-1) + "'. Caught: " + sqle);
							throw sqle;
						}
					}

					atType = Type.Target;
					pstmt.addBatch();

					dbt.targetWriteCount++;

					if (batchSize > 0 && batchCount >= batchSize )
					{
	//System.out.println("BATCH SIZE: Executing batch: batchSize=" + batchSize + ", batchCount=" + batchCount + ", totalCount=" + totalCount);
						batchCount = 0;
						
						pstmt.executeBatch();
					}
					

					// print some information if it takes TO LONG...
					if (TimeUtils.timeExpired("dataTransferLastMsgTime", 5*1000))
					{
						int pctDone = (int) ( ((dbt.targetWriteCount*1.0) / (dbt.sourceRowCountEstimate*1.0)) * 100.0 );
						//    format("Transfer: schema=%s, name=%-40.40s, estRows=%8d, dmlBatchSize=%d, dmlUseMerge=%b",
						_logger.info("          Done " + (pctDone < 0 ? "-unknown-" : pctDone) + "%, sourceRowCountEstimate=" + dbt.sourceRowCountEstimate + ", targetWriteCount=" + dbt.targetWriteCount);
					}
				}
	//System.out.println("END OF TRANSFER: Executing batch: batchSize=" + batchSize + ", batchCount=" + batchCount + ", totalCount=" + totalCount);

				atType = Type.Target;
	            pstmt.executeBatch();
				pstmt.close();

				
//				ResultSetMetaData rsmd = rs.getMetaData();
//				List<String> colNames = new
//				int cols = rsmd.getColumnCount();
//				while (rs.next())
//				{
//					String insert = "insert into "  + q+dbt.schema+q +  "." + q+dbt.name+q + 
//				}
			}
		}
		catch (SQLException ex)
		{
			if (Type.Source.equals(atType))
			{
				throw new SourceException(ex);
			}
			if (Type.Target.equals(atType))
			{
				throw new TargetException(ex);
			}
		}
		finally
		{
			// Count records in the target table
			dbt.targetPostRowCount = getTargetTableRowCount(dbt);
		}
	}

	private long getTargetTableRowCount(DbTable dbt)
	{
		long count = -1;
		String sqlRowCount = _targetConn.quotifySqlString("select count(*) from [" + dbt.schema + "].[" + dbt.name + "]");
		try (Statement stmnt = _targetConn.createStatement(); ResultSet rs = stmnt.executeQuery(sqlRowCount))
		{
			while(rs.next())
			{
				count = rs.getInt(1);
				//System.out.println("getTargetTableRowCount - COUNT[" + count + "]     - SQL: " + sqlRowCount);
			}
		}
		catch(SQLException ex)
		{
			_logger.warn("Problems getting TARGET ROW-COUNT using sql='" + sqlRowCount + "'. caught: " + ex);
		}
		return count;
	}
	

//	private void transferDbxCentralMetaDataTables()
//	{
//		for (DbTable dbt : _tableList)
//		{
//			if (dbt.name.startsWith("Dbx"))
//			{
//				_logger.info("");
//				String msg1 = String.format("Transfer: schema=%s, name=%-40.40s, estRows=%9d, dmlBatchSize=%d, dmlUseMerge=%b",
//						dbt.schema, 
//						dbt.name, 
//						dbt.sourceRowCountEstimate,
//						_dmlBatchSize,
//						_dmlUseMerge);
//				_logger.info(msg1);
//
//				
//			}
//		}
//	}
	
	
	private class FixDbxCentralSessions
	{
		Timestamp SessionStartTime = null;
		int       NumOfSamples     = -1;
		Timestamp LastSampleTime   = null;
	}
	private void fixDbxCentralMetaDataTablesAtTarget()
	{
		// For now:
		// - only tables with PrimaryKey 'SessionStartTime' will be affected
		//   this is becase in some cases we have "overlapping" values
		//   (overlaping = if DbxCentral DB is re-created-due-to-issues and the collectors has NOT been stopped, then we get SAME SessionStartTime in the two different DBXC DB's we are merging, and the "summary" fields will have to be "merged" (a sum of both fields)
		
//		select * from "PUBLIC"."DbxCentralGraphProfiles"  -- on merge: no issues
//		select * from "PUBLIC"."DbxCentralSessions"       -- on merge: FIX: NumOfSamples, LastSampleTime (if row is found in target)
//		                                                  -- overlaps if DBXC DB is re-created-due-to-issues and the collectors has NOT been stopped (then we get SAME SessionStartTime in the two different DBXC DB's we are merging)
//		select * from "PUBLIC"."DbxCentralVersionInfo"    -- on merge: no issues
//
//		select * from "DEV_ASE"."DbxGraphProperties"      -- on merge: no issues (same issue as "PUBLIC"."DbxCentralSessions")
//		select * from "DEV_ASE"."DbxSessionSampleDetailes" where "SessionStartTime" = '2018-09-01 18:23:52.896' -- on merge: FIX: alot... or delete this table
//		select * from "DEV_ASE"."DbxSessionSamples"       -- on merge: no issues
//		select * from "DEV_ASE"."DbxSessionSampleSum"     -- on merge: (same issue as "PUBLIC"."DbxCentralSessions")

		// Get unique set of schemas (except PUBLIC)
		List<String> schemaNames = new ArrayList<>();
		for (DbTable dbt : _tableList)
		{
			if ( ! schemaNames.contains(dbt.schema) && ! dbt.schema.equalsIgnoreCase("PUBLIC"))
				schemaNames.add(dbt.schema);
		}

		//------------------------------------------------------
		// PUBLIC.DbxCentral* tables
		//------------------------------------------------------
		// DbxCentralGraphProfiles - pk(ProductString, UserName, ProfileName)    -- CREATE CACHED TABLE PUBLIC."DbxCentralGraphProfiles"(     "ProductString" VARCHAR(30) NOT NULL,     "UserName" VARCHAR(30) NOT NULL,     "ProfileName" VARCHAR(30) NOT NULL,     "ProfileDescription" TEXT NOT NULL,     "ProfileValue" TEXT NOT NULL,     "ProfileUrlOptions" VARCHAR(1024) )
		// DbxCentralVersionInfo   - pk(ProductString)                           -- CREATE CACHED TABLE PUBLIC."DbxCentralVersionInfo"  (     "ProductString" VARCHAR(30) NOT NULL,     "VersionString" VARCHAR(30) NOT NULL,     "BuildString" VARCHAR(30) NOT NULL,     "DbVersion" INT NOT NULL,     "DbProductName" VARCHAR(30) NOT NULL )
		// DbxCentralSessions      - pk(SessionStartTime)                        -- CREATE CACHED TABLE PUBLIC."DbxCentralSessions"     (     "SessionStartTime" DATETIME NOT NULL,     "Status" INT NOT NULL,     "ServerName" VARCHAR(30) NOT NULL,     "OnHostname" VARCHAR(30) NOT NULL,     "ProductString" VARCHAR(30) NOT NULL,     "VersionString" VARCHAR(30) NOT NULL,     "BuildString" VARCHAR(30) NOT NULL,     "CollectorHostname" VARCHAR(30) NOT NULL,     "CollectorSampleInterval" INT NOT NULL,     "CollectorCurrentUrl" VARCHAR(80),     "CollectorInfoFile" VARCHAR(256),     "NumOfSamples" INT NOT NULL,     "LastSampleTime" DATETIME )

		// DbxCentralGraphProfiles - pk(ProductString, UserName, ProfileName)    
		//      CREATE CACHED TABLE PUBLIC."DbxCentralGraphProfiles"(     
		//       * "ProductString"      VARCHAR(30)   NOT NULL,     
		//       * "UserName"           VARCHAR(30)   NOT NULL,     
		//       * "ProfileName"        VARCHAR(30)   NOT NULL,     
		//         "ProfileDescription" TEXT          NOT NULL,     
		//         "ProfileValue"       TEXT          NOT NULL,     
		//         "ProfileUrlOptions"  VARCHAR(1024)     NULL
		//      )
		// DbxCentralVersionInfo   - pk(ProductString)                           -- 
		//      CREATE CACHED TABLE PUBLIC."DbxCentralVersionInfo"  (     
		//       * "ProductString" VARCHAR(30) NOT NULL,     
		//         "VersionString" VARCHAR(30) NOT NULL,     
		//         "BuildString"   VARCHAR(30) NOT NULL,     
		//         "DbVersion"     INT         NOT NULL,     
		//         "DbProductName" VARCHAR(30) NOT NULL 
		//      )
		// DbxCentralSessions      - pk(SessionStartTime)                        -- 
		//      CREATE CACHED TABLE PUBLIC."DbxCentralSessions"     (     
		//       * "SessionStartTime" DATETIME NOT NULL,     
		//         "Status"                   INT          NOT NULL,     
		//         "ServerName"               VARCHAR(30)  NOT NULL,     
		//         "OnHostname"               VARCHAR(30)  NOT NULL,     
		//         "ProductString"            VARCHAR(30)  NOT NULL,     
		//         "VersionString"            VARCHAR(30)  NOT NULL,     
		//         "BuildString"              VARCHAR(30)  NOT NULL,     
		//         "CollectorHostname"        VARCHAR(30)  NOT NULL,     
		//         "CollectorSampleInterval"  INT          NOT NULL,     
		//         "CollectorCurrentUrl"      VARCHAR(80)      NULL,     
		//         "CollectorInfoFile"        VARCHAR(256)     NULL,     
		//         "NumOfSamples"             INT          NOT NULL,     
		//         "LastSampleTime"           DATETIME         NULL 
		//      )

		_logger.info("");
		_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='PUBLIC', table='DbxCentralGraphProfiles'.");
		_logger.info("                                      SourceRowCount=" + getDbtFor("PUBLIC", "DbxCentralGraphProfiles").sourceReadCount);
		_logger.info("                                      TargetRowCount=" + getDbtFor("PUBLIC", "DbxCentralGraphProfiles").targetPostRowCount);
		if (getDbtFor("PUBLIC", "DbxCentralGraphProfiles").sourceReadCount <= 0)
			_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
		_logger.info("                                      NOT-FIX-NEEDED-OR-IMPLEMETED");

		_logger.info("");
		_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='PUBLIC', table='DbxCentralVersionInfo'.");
		_logger.info("                                      SourceRowCount=" + getDbtFor("PUBLIC", "DbxCentralVersionInfo").sourceReadCount);
		_logger.info("                                      TargetRowCount=" + getDbtFor("PUBLIC", "DbxCentralVersionInfo").targetPostRowCount);
		if (getDbtFor("PUBLIC", "DbxCentralVersionInfo").sourceReadCount <= 0)
			_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
		_logger.info("                                      NOT-FIX-NEEDED-OR-IMPLEMETED");

		
		_logger.info("");
		_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='PUBLIC', table='DbxCentralSessions'.");
		_logger.info("                                      SourceRowCount=" + getDbtFor("PUBLIC", "DbxCentralSessions").sourceReadCount);
		_logger.info("                                      TargetRowCount=" + getDbtFor("PUBLIC", "DbxCentralSessions").targetPostRowCount);
		if (getDbtFor("PUBLIC", "DbxCentralSessions").sourceReadCount <= 0)
			_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
		_logger.info("                                      fixing values for 'NumOfSamples', 'LastSampleTime'. foreach: 'SessionStartTime' & 'ServerName' record. from table 'DbxCentralSessions' in each schema/server.");
		for (String schema : schemaNames)
		{
			String sql = "select   [SessionStartTime] \n"
			           + "       , count([SessionStartTime])  as [NumOfSamples] \n"
			           + "       , max([SessionSampleTime]) as [LastSampleTime] \n"
			           + "from ["+schema+"].[DbxSessionSamples] \n"
			           + "group by [SessionStartTime]";
			sql = _targetConn.quotifySqlString(sql);
//			sql = sql.replace('#', q); // replace # with the DBMS Quoted Identifier Char

			List<FixDbxCentralSessions> list = new ArrayList<>();
			try (Statement stmnt = _targetConn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
			{
				while(rs.next())
				{
					FixDbxCentralSessions record = new FixDbxCentralSessions();
					
					record.SessionStartTime = rs.getTimestamp(1);
					record.NumOfSamples     = rs.getInt(2);
					record.LastSampleTime   = rs.getTimestamp(3);

					list.add(record);
				}
			}
			catch(SQLException ex)
			{
				_logger.error("Problems getting 'NumOfSamples' and 'LastSampleTime' from schema='" + schema + "', table='DbxSessionSamples'. caught: " + ex);
			}
			
			// Update 'NumOfSamples' and 'LastSampleTime'
			sql = "update [PUBLIC].[DbxCentralSessions] set [NumOfSamples] = ?, [LastSampleTime] = ? where [SessionStartTime] = ? and [ServerName] = ?";
			sql = _targetConn.quotifySqlString(sql);
//			sql = sql.replace('#', q); // replace # with the DBMS Quoted Identifier Char

			for (FixDbxCentralSessions e : list)
			{
				try (PreparedStatement stmnt = _targetConn.prepareStatement(sql))
				{
					stmnt.setInt      (1, e.NumOfSamples);
					stmnt.setTimestamp(2, e.LastSampleTime);
					stmnt.setTimestamp(3, e.SessionStartTime);
					stmnt.setString   (4, schema);
					
//					_logger.info("  - Updating PUBLIC.DbxCentralSessions: NumOfSamples=" + e.NumOfSamples + ", LastSampleTime='" + e.LastSampleTime + "' for ServerName='" + schema + "' and SessionStartTime='" + e.SessionStartTime + "'.");
					_logger.info("  - Updating PUBLIC.DbxCentralSessions: for ServerName=" + StringUtil.left(schema,30) + " and SessionStartTime=" + StringUtil.left(e.SessionStartTime + "",23) + "  with NumOfSamples=" + StringUtil.left(e.NumOfSamples + "",6) + ", LastSampleTime=" + e.LastSampleTime + "'.");
					int updCount = stmnt.executeUpdate();
					
					if (updCount != 1)
						_logger.error("Rowcount when updating PUBLIC.DbxCentralSessions was not 1. serverName='" + schema + "', SessionStartTime='" + e.SessionStartTime + "'. rowcount=" + updCount);
				}
				catch(SQLException ex)
				{
					_logger.error("Problems updating 'NumOfSamples' and 'LastSampleTime' for PUBLIC.DbxCentralSessions, serverName='" + schema + "', SessionStartTime='" + e.SessionStartTime + "'. caught: " + ex);
				}
			}
		}

		// DbxGraphProperties        - pk(SessionStartTime, GraphName)                  -- CREATE CACHED TABLE PROD_B1_ASE."DbxGraphProperties"      (     "SessionStartTime" DATETIME NOT NULL,     "CmName" VARCHAR(30) NOT NULL,     "GraphName" VARCHAR(30) NOT NULL,     "TableName" VARCHAR(128) NOT NULL,     "GraphLabel" VARCHAR(255) NOT NULL,     "GraphCategory" VARCHAR(30) NOT NULL,     "isPercentGraph" INT NOT NULL,     "visibleAtStart" INT NOT NULL,     "initialOrder" INT NOT NULL )
		// DbxSessionSampleDetailes  - pk(SessionSampleTime, CmName, SessionStartTime)  -- CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleDetailes"(     "SessionStartTime" DATETIME NOT NULL,     "SessionSampleTime" DATETIME NOT NULL,     "CmName" VARCHAR(30) NOT NULL,     "type" INT,     "graphRecvCount" INT,     "absRecvRows" INT,     "diffRecvRows" INT,     "rateRecvRows" INT,     "graphSaveCount" INT,     "absSaveRows" INT,     "diffSaveRows" INT,     "rateSaveRows" INT,     "sqlRefreshTime" INT,     "guiRefreshTime" INT,     "lcRefreshTime" INT,     "nonCfgMonHappened" INT,     "nonCfgMonMissingParams" VARCHAR(100),     "nonCfgMonMessages" VARCHAR(1024),     "isCountersCleared" INT,     "hasValidSampleData" INT,     "exceptionMsg" VARCHAR(1024),     "exceptionFullText" TEXT )
		// DbxSessionSamples         - pk(SessionSampleTime, SessionStartTime)          -- CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSamples"       (     "SessionStartTime" DATETIME NOT NULL,     "SessionSampleTime" DATETIME NOT NULL )
		// DbxSessionSampleSum       - pk(SessionStartTime, CmName)                     -- CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleSum"     (     "SessionStartTime" DATETIME NOT NULL,     "CmName" VARCHAR(30) NOT NULL,     "graphSamples" INT,     "absSamples" INT,     "diffSamples" INT,     "rateSamples" INT )
		// DbxAlarmActive            - pk(                   alarmClass, serviceType, serviceName, serviceInfo, extraInfo)
		// DbxAlarmHistory           - pk(eventTime, action, alarmClass, serviceType, serviceName, serviceInfo, extraInfo)

	
		// ------- DbxGraphProperties        - pk(SessionStartTime, GraphName)                  -- 
		// CREATE CACHED TABLE PROD_B1_ASE."DbxGraphProperties"      (     
		// 	*	"SessionStartTime" DATETIME     NOT NULL,     
		// 		"CmName"           VARCHAR(30)  NOT NULL,     
		// 	*	"GraphName"        VARCHAR(30)  NOT NULL,     
		// 		"TableName"        VARCHAR(128) NOT NULL,     
		// 		"GraphLabel"       VARCHAR(255) NOT NULL,     
		// 		"GraphCategory"    VARCHAR(30)  NOT NULL,     
		// 		"isPercentGraph"   INT          NOT NULL,     
		// 		"visibleAtStart"   INT          NOT NULL,     
		// 		"initialOrder"     INT          NOT NULL )
		// ------- DbxSessionSampleDetailes  - pk(SessionSampleTime, CmName, SessionStartTime)  -- 
		// CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleDetailes"(     
		// 	*	"SessionStartTime"       DATETIME    NOT NULL,     
		// 	*	"SessionSampleTime"      DATETIME    NOT NULL,     
		// 	*	"CmName"                 VARCHAR(30) NOT NULL,     
		// 		"type"                   INT,     
		// 		"graphRecvCount"         INT,     
		// 		"absRecvRows"            INT,     
		// 		"diffRecvRows"           INT,     
		// 		"rateRecvRows"           INT,     
		// 		"graphSaveCount"         INT,     
		// 		"absSaveRows"            INT,    
		// 		"diffSaveRows"           INT,     
		// 		"rateSaveRows"           INT,     
		// 		"sqlRefreshTime"         INT,     
		// 		"guiRefreshTime"         INT,     
		// 		"lcRefreshTime"          INT,     
		// 		"nonCfgMonHappened"      INT,     
		// 		"nonCfgMonMissingParams" VARCHAR(100),     
		// 		"nonCfgMonMessages"      VARCHAR(1024),     
		// 		"isCountersCleared"      INT,     
		// 		"hasValidSampleData"     INT,     
		// 		"exceptionMsg"           VARCHAR(1024),     
		// 		"exceptionFullText"      TEXT )
		// ------- DbxSessionSamples         - pk(SessionSampleTime, SessionStartTime)          -- 
		// CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSamples"       (     
		// 	*	"SessionStartTime"  DATETIME NOT NULL,     
		// 	*	"SessionSampleTime" DATETIME NOT NULL )
		// ------- DbxSessionSampleSum       - pk(SessionStartTime, CmName)                     -- 
		// CREATE CACHED TABLE PROD_B1_ASE."DbxSessionSampleSum"     (     
		// 	*	"SessionStartTime" DATETIME NOT NULL,     
		// 	*	"CmName" VARCHAR(30) NOT NULL,     
		// 		"graphSamples" INT,     
		// 		"absSamples" INT,     
		// 		"diffSamples" INT,     
		// 		"rateSamples" INT )
		// ------- DbxAlarmActive            - pk(                   alarmClass, serviceType, serviceName, serviceInfo, extraInfo)
		// ------- DbxAlarmHistory           - pk(eventTime, action, alarmClass, serviceType, serviceName, serviceInfo, extraInfo)

		// fix individual schemas
		for (String schema : schemaNames)
		{
//			_logger.info("fixDbxCentralMetaDataTablesAtTarget - NOT-YET-IMPLEMENTED - schema='" + schema + "'. (DbxGraphProperties, DbxSessionSampleDetailes, DbxSessionSamples, DbxSessionSampleSum)");
			
			_logger.info("");
			_logger.info("");
			_logger.info("fixDbxCentralMetaDataTablesAtTarget ------------------ SCHEMA='" + schema + "'.");
			_logger.info("");
			_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='" + schema + "', table='DbxGraphProperties'.");
			_logger.info("                                      SourceRowCount=" + getDbtFor(schema, "DbxGraphProperties").sourceReadCount);
			_logger.info("                                      TargetRowCount=" + getDbtFor(schema, "DbxGraphProperties").targetPostRowCount);
			if (getDbtFor(schema, "DbxGraphProperties").sourceReadCount <= 0)
				_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
			_logger.info("                                      NOT-FIX-NEEDED-OR-IMPLEMETED");

			_logger.info("");
			_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='" + schema + "', table='DbxSessionSampleDetailes'.");
			_logger.info("                                      SourceRowCount=" + getDbtFor(schema, "DbxSessionSampleDetailes").sourceReadCount);
			_logger.info("                                      TargetRowCount=" + getDbtFor(schema, "DbxSessionSampleDetailes").targetPostRowCount);
			if (getDbtFor(schema, "DbxGraphProperties").sourceReadCount <= 0)
				_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
			_logger.info("                                      NOT-FIX-NEEDED-OR-IMPLEMETED");

			_logger.info("");
			_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='" + schema + "', table='DbxSessionSamples'.");
			_logger.info("                                      SourceRowCount=" + getDbtFor(schema, "DbxSessionSamples").sourceReadCount);
			_logger.info("                                      TargetRowCount=" + getDbtFor(schema, "DbxSessionSamples").targetPostRowCount);
			if (getDbtFor(schema, "DbxGraphProperties").sourceReadCount <= 0)
				_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
			_logger.info("                                      NOT-FIX-NEEDED-OR-IMPLEMETED");

			_logger.info("");
			_logger.info("fixDbxCentralMetaDataTablesAtTarget - schema='" + schema + "', table='DbxSessionSampleSum'.");
			_logger.info("                                      SourceRowCount=" + getDbtFor(schema, "DbxSessionSampleSum").sourceReadCount);
			_logger.info("                                      TargetRowCount=" + getDbtFor(schema, "DbxSessionSampleSum").targetPostRowCount);
			if (getDbtFor(schema, "DbxGraphProperties").sourceReadCount <= 0)
				_logger.info("                                      Problems reading the records from the SOURCE, Could NOT be reconstructed");
			_logger.info("                                      NOT-FIX-NEEDED-OR-IMPLEMETED");

			// I think the only one we need to look at is: DbxSessionSampleSum, and fixing 'graphSamples'
			// and to do that we need to:
			// - get tables starting with CmName like 'CmActiveStatements_*'
			// - count records for 'SessionStartTime'  (select SessionStartTime, count(*) as graphSamples from ${schema}.${tabName} group by SessionStartTime)
			// - get MAX value of count(*)
		}
	}

	private DbTable getDbtFor(String schema, String name)
	{
		for (DbTable dbt : _tableList)
		{
			if (dbt.schema.equals(schema) && dbt.name.equals(name))
				return dbt;
		}
		String msg = "Can't find schema='" + schema + "' and table='" + name + "' in the DbTable List.";
		_logger.error(msg, new RuntimeException(msg)); // the Exception to get a stacktrace so we know where it happened
		return new DbTable();
	}

	private void ifH2_doShutdownDefrag(DbxConnection conn)
	{
		if (conn != null)
		{
			if (DbUtils.isProductName(conn.getDatabaseProductNameNoThhrow("unknown"), DbUtils.DB_PROD_NAME_H2))
			{
				String sql = "SHUTDOWN DEFRAG";
				try
				{
					_logger.info("");
					_logger.info("Executing '" + sql + "' on '" + conn.getDbmsServerNameNoThrow() + "'.");
					
					long startTime = System.currentTimeMillis();
					executeDdl(conn, sql);
					
					_logger.info("Shutdown exec time '" + TimeUtils.msDiffNowToTimeStr(startTime) + "' to execute '" + sql + "'." );
				}
				catch(SQLException ex)
				{
					_logger.warn("Problems when execution '" + sql + "' on '" + conn.getDbmsServerNameNoThrow() + "'.", ex);
				}
			}
		}
	}
	
	@Override
	public void close()
	{
		if (_sourceConn != null)
		{
			_logger.info("Closing Connection to '" + Type.Source + "'.");
			_sourceConn.closeNoThrow();
			_sourceConn = null;
		}
		
		if (_targetConn != null)
		{
			_logger.info("Closing Connection to '" + Type.Target + "'.");
			_targetConn.closeNoThrow();
			_targetConn = null;
		}
		
		if (_migrationH2Srv != null)
		{
			stopOldH2TcpServer();
		}
	}

	// H2: select TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE, SQL, ROW_COUNT_ESTIMATE from INFORMATION_SCHEMA.TABLES where TABLE_TYPE = 'TABLE'
	public List<DbTable> getTables(DbxConnection conn)
	throws SQLException
	{
		try
		{
			List<DbTable> list = new ArrayList<>();
			
			_logger.info("Extracting Catalog information from: " + conn.getDbmsServerName());

			// do NOT include some schemas
			ICreateFilter createFilter = new ICreateFilter()
			{
				@Override
				public boolean includeSchema(DbxConnection conn, String catName, String schemaName)
				{
					if ("pg_catalog".equalsIgnoreCase(schemaName))
					{
						_logger.info("Skipping SCHEMA '" + schemaName + "' for connnection '" + conn.getDbmsServerNameNoThrow() + "'.");
						return false;
					}
					if ("INFORMATION_SCHEMA".equalsIgnoreCase(schemaName))
					{
						_logger.info("Skipping SCHEMA '" + schemaName + "' for connnection '" + conn.getDbmsServerNameNoThrow() + "'.");
						return false;
					}

					return true;
				}
			};
			
			// Get info from current catalog
			Catalog catalog = Catalog.create(conn, null, createFilter);

			
			boolean isSourceConn = true;
			if (conn == _sourceConn)
			{
				_sourceCatalog = catalog;
				isSourceConn = true;
			}
			else if (conn == _targetConn)
			{
				_targetCatalog = catalog;
				isSourceConn = false;
			}
			else
			{
				throw new RuntimeException("The passed Connection is not the '_sourceConn' or the '_targetConn'... not sure how to proceed...");
			}

//			catalog.getDatabaseInfo();
			for (final Schema schema : catalog.getSchemas())
			{
//				System.out.println("---------------------- SCHEMA: " + schema);
				for (final Table table : schema.getTables())
				{
					if ( ! (table instanceof Table) )
						continue;

					DbTable dbt = new DbTable();
//					dbt.catalog                = catalog.getName();
					dbt.catalog                = schema.getCatalogName();
					dbt.schema                 = table.getSchemaName();
					dbt.name                   = table.getTableName();
					dbt.type                   = "TABLE";
					dbt.ddl                    = null;
					dbt.sourceRowCountEstimate = -1;
					
//					dbt.sourceRowCountEstimate = conn.getRowCountEstimate(dbt.catalog, dbt.schema, dbt.name);
					dbt.sourceRowCountEstimate = conn.getRowCountEstimate(null, dbt.schema, dbt.name);
//System.out.println((isSourceConn ? "SOURCE" : "DEST") + ": cat='" + dbt.catalog + "', schema='" + dbt.schema + "', tab='" + dbt.name + "', rowCountEstimate=" + dbt.sourceRowCountEstimate);

					list.add(dbt);
				}
			}

			return list;
		}
		catch(SQLException ex)
		{
			throw new SQLException("Exception when getting Database Objects from DBMS.", ex);
		}
		
//		try
//		{
//			List<DbTable> list = new ArrayList<>();
//			
//			_logger.info("Extracting Catalog information from: "+_sourceConn.getDbmsServerName());
//
//			// FIXME: if H2 ... then map away all objects in "pg_catalog" schema
//			// Create the options
//			SchemaCrawlerOptionsBuilder optionsBuilder = SchemaCrawlerOptionsBuilder.builder();
//
//			// Set what details are required in the schema - this affects the time taken to crawl the schema
//			optionsBuilder.withSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
//
//			if (conn == _sourceConn && DbUtils.isProductName(_sourceConn.getDatabaseProductNameNoThhrow("xxx"), DbUtils.DB_PROD_NAME_H2))
//			{
//				_logger.info("Source Connections seems to be a H2 database... excluding schema: 'pg_catalog'");
//				
////				optionsBuilder.includeSchemas(schemaFullName -> !schemaFullName.endsWith("pg_catalog"));
//				optionsBuilder.includeSchemas(schemaName -> !schemaName.endsWith(".pg_catalog"));
//			}
//
//			boolean isSourceConn = true;
//			Catalog catalog = SchemaCrawlerUtils.getCatalogObjects(conn, optionsBuilder.toOptions());
//			if (conn == _sourceConn)
//			{
//				_sourceCatalog = catalog;
//				isSourceConn = true;
//			}
//			else if (conn == _targetConn)
//			{
//				_targetCatalog = catalog;
//				isSourceConn = false;
//			}
//			else
//			{
//				throw new RuntimeException("The passed Connection is not the '_sourceConn' or the '_targetConn'... not sure how to proceed...");
//			}
//
//			catalog.getDatabaseInfo();
//			for (final Schema schema : catalog.getSchemas())
//			{
////				System.out.println("---------------------- SCHEMA: " + schema);
//				for (final Table table : catalog.getTables(schema))
//				{
//					if ( ! (table instanceof Table) )
//						continue;
//
//					DbTable dbt = new DbTable();
////					dbt.catalog                = catalog.getName();
//					dbt.catalog                = schema.getCatalogName();
//					dbt.schema                 = table.getSchema().getName();
//					dbt.name                   = table.getName();
//					dbt.type                   = "TABLE";
//					dbt.ddl                    = null;
//					dbt.sourceRowCountEstimate = -1;
//					
////					dbt.sourceRowCountEstimate = conn.getRowCountEstimate(dbt.catalog, dbt.schema, dbt.name);
//					dbt.sourceRowCountEstimate = conn.getRowCountEstimate(null, dbt.schema, dbt.name);
//System.out.println((isSourceConn ? "SOURCE" : "DEST") + ": cat='" + dbt.catalog + "', schema='" + dbt.schema + "', tab='" + dbt.name + "', rowCountEstimate=" + dbt.sourceRowCountEstimate);
//
//					list.add(dbt);
//				}
//			}
//
//			return list;
//		}
//		catch(SchemaCrawlerException ex)
//		{
//			throw new SQLException("Exception when getting Database Objects from DBMS.", ex);
//		}



//		if (conn.isDatabaseProduct(DbUtils.DB_PROD_NAME_H2))
//		{
//			String sql = "select TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE, SQL, ROW_COUNT_ESTIMATE \n"
//					+ "from INFORMATION_SCHEMA.TABLES \n"
//					+ "where TABLE_TYPE = 'TABLE' \n"
//					+ "order by 1,2,3 \n";
//
//			List<DbTable> list = new ArrayList<>();
//			
//			try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(sql))
//			{
//				while (rs.next())
//				{
//					DbTable dbt = new DbTable();
//					dbt.catalog                = rs.getString(1);
//					dbt.schema                 = rs.getString(2);
//					dbt.name                   = rs.getString(3);
//					dbt.type                   = rs.getString(4);
//					dbt.ddl                    = rs.getString(5);
//					dbt.sourceRowCountEstimate = rs.getLong  (6);
//					
//					list.add(dbt);
//				}
//			}
//			return list;
//		}
//		else
//		{
//			// GET LIST FROM JDBC Meta Data
//			
//			_logger.info("getTables(): will return empty list for DBMS Vendor '" + conn.getDatabaseProductName() + "'.");
//			
//			List<DbTable> list = new ArrayList<>();
//			
//			DatabaseMetaData dbmd = conn.getMetaData();
//			String   catalog          = null;  // a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a catalog; null means that the catalog name should not be used to narrow the search
//			String   schemaPattern    = null;  // a schema name pattern; must match the schema name as it is stored in the database; "" retrieves those without a schema; null means that the schema name should not be used to narrow the search
//			String   tableNamePattern = "%";   // a table name pattern; must match the table name as it is stored in the database
//			String[] types = new String[] {"TABLE"};
//
//			try (ResultSet rs = dbmd.getTables(catalog, schemaPattern, tableNamePattern, types))
//			{
//				// ResultSet output accrding to: https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html
//				// 1:  TABLE_CAT String => table catalog (may be null)
//				// 2:  TABLE_SCHEM String => table schema (may be null)
//				// 3:  TABLE_NAME String => table name
//				// 4:  TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
//				// 5:  REMARKS String => explanatory comment on the table
//				// 6:  TYPE_CAT String => the types catalog (may be null)
//				// 7:  TYPE_SCHEM String => the types schema (may be null)
//				// 8:  TYPE_NAME String => type name (may be null)
//				// 9:  SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
//				// 10: REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)
//				
//				while (rs.next())
//				{
//					DbTable dbt = new DbTable();
//					dbt.catalog                = rs.getString(1);
//					dbt.schema                 = rs.getString(2);
//					dbt.name                   = rs.getString(3);
//					dbt.type                   = rs.getString(4);
//					dbt.ddl                    = "";
//					dbt.sourceRowCountEstimate = -1;
//
//					list.add(dbt);
//				}
//			}
//			
//			// Get column spec for all tables (and possibly the row count)
//			for (DbTable dbt : list)
//			{
//			}
//			
//			return list;
//		}
	}
	
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

		pw.println("usage: " + Version.getAppName() + " [-h] [-v] [-x] [-e]");
		pw.println("              [-U <srcUser>]  [-P <srcPasswd>]  [-S <srcUrl>]");
		pw.println("              [-u <destUser>] [-p <destPasswd>] [-s <destUrl>]");
		pw.println("              [-C <cfgfile>] [-L <logfile>] [-H <dirname>] [-R <dirname>] [-D <key=val>]");
		pw.println("              [-b]");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                   Usage information.");
		pw.println("  -v,--version                Display " + Version.getAppName() + " and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>      Debug options: a comma separated string");
		pw.println("                              To get available option, do -x list");
		pw.println("  ");
		pw.println("  -e,--exec                   Execute... if not specified: it will only list parameters");
		pw.println("  ");
		pw.println("  -U,--fromUser <user>        Source Username when connecting to server.");
		pw.println("  -P,--fromPasswd <passwd>    Source Password when connecting to server. null=noPasswd");
		pw.println("  -S,--fromUrl <url>          Source URL");
		pw.println("  -J,--fromOldH2Jar <jarFile> Upgrade/Migrate old H2 database (from old JAR file)");
		pw.println("                              This will start a separate process, with a H2 tcpSrv to host old H2 DB");
		pw.println("  ");
		pw.println("  -u,--toUser <user>          Destination Username when connecting to server.");
		pw.println("  -p,--toPasswd <passwd>      Destination Password when connecting to server. null=noPasswd");
		pw.println("  -s,--toUrl <url>            Destination URL");
		pw.println("  ");
		pw.println("  -b,--dmlBatchSize <num>     How often should we commit in the target db");
		pw.println("  -m,--dmlUseMerge            If we should use SQL 'merge' instead of 'insert'.");
		pw.println("                              Note: if H2 target db exists, this is auto-enabled ");
		pw.println("  ");
		pw.println("  -C,--config  <filename>     System Config file.");
		pw.println("  -L,--logfile <filename>     Name of the logfile where application logging is saved.");
		pw.println("  -H,--homedir <dirname>      HOME Directory, where all personal files are stored.");
		pw.println("  -R,--savedir <dirname>      DBXTUNE_SAVE_DIR, where H2 Database recordings are stored.");
		pw.println("  -D,--javaSystemProp <k=v>   Set Java System Property, same as java -Dkey=value");
		pw.println("  ");
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

		// create the Options
		options.addOption( Option.builder("C").longOpt("config"        ).hasArg(true ).build() );
		options.addOption( Option.builder("h").longOpt("help"          ).hasArg(false).build() );
		options.addOption( Option.builder("v").longOpt("version"       ).hasArg(false).build() );
		options.addOption( Option.builder("x").longOpt("debug"         ).hasArg(true ).build() );

		options.addOption( Option.builder("e").longOpt("exec"          ).hasArg(false).build() );
		
		options.addOption( Option.builder("U").longOpt("fromUser"      ).hasArg(true ).build() );
		options.addOption( Option.builder("P").longOpt("fromPasswd"    ).hasArg(true ).build() );
		options.addOption( Option.builder("S").longOpt("fromUrl"       ).hasArg(true ).build() );
		options.addOption( Option.builder("J").longOpt("fromOldH2Jar"  ).hasArg(true ).build() );

		options.addOption( Option.builder("u").longOpt("toUser"        ).hasArg(true ).build() );
		options.addOption( Option.builder("p").longOpt("toPasswd"      ).hasArg(true ).build() );
		options.addOption( Option.builder("s").longOpt("toUrl"         ).hasArg(true ).build() );

		options.addOption( Option.builder("b").longOpt("dmlBatchSize"  ).hasArg(true ).build() );
		options.addOption( Option.builder("m").longOpt("dmlUseMerge"   ).hasArg(false).build() );

		options.addOption( Option.builder("L").longOpt("logfile"       ).hasArg(true ).build() );
		options.addOption( Option.builder("H").longOpt("homedir"       ).hasArg(true ).build() );
		options.addOption( Option.builder("R").longOpt("savedir"       ).hasArg(true ).build() );
		options.addOption( Option.builder("D").longOpt("javaSystemProp").hasArgs().valueSeparator('=').build() ); // NOTE the hasArgs() instead of hasArg() *** the 's' at the end of hasArg<s>() does the trick...

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
				_logger.debug("parseCommandLine: swith='" + opt.getOpt() + "', value='" + opt.getValue() + "'.");
			}
		}

		return cmd;
	}

	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
		Version.setAppName("DbxCentralDbCopy");
		
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
				System.out.println(Version.getAppName() + " Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
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
			// Start 
			//-------------------------------
			else
			{
				// Use: auto-close
				try( H2CentralDbCopy3 dbCopy = new H2CentralDbCopy3() )
				{
					// Initialize some things
					dbCopy.init(cmd);

					// Connect to SOURCE/Target
					dbCopy.connect();

					// check some stuff before we: go-to-work
					dbCopy.preCheck();
					if (dbCopy._sourceDbxCentralDbVersion <= 0)
					{
						throw new Exception("The SOURCE database does not look like a DbxCentral database.");
					}
					if (dbCopy._targetDbxCentralDbVersion >= 0)
					{
						if (dbCopy._sourceDbxCentralDbVersion != dbCopy._targetDbxCentralDbVersion)
						{
							throw new Exception("The SOURCE and TARGET DbxCentral database versions are not the same, this is not supported... sourceDbVersion=" + dbCopy._sourceDbxCentralDbVersion + ", targetDbVersion=" + dbCopy._targetDbxCentralDbVersion);
						}
					}

					// DO THE WORK
					dbCopy.doWork();
				}

				_logger.info("End of processing in '" + H2CentralDbCopy3.class.getSimpleName() + "'.");
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
	}



	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	// Local helper classes
	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////
	public enum ErrorType
	{
		OK, 
		SUSPECT_WRITE_COUNT, 
		SUSPECT_ROW_COUNT, 
		HAS_EXCEPTION
	};
	
	private static class DbTable
	{
		String catalog;
		String schema;
		String name;
		String type;
		
		String ddl;

		// Source specific fields
		long sourceRowCountEstimate = 0;
		long sourceReadCount        = 0;
		
		// Target specific fields
		long targetWriteCount   = 0;
		long targetPostRowCount = -1;

		long targetPreRowCount  = -1;
		
		// error information
		ErrorType errorType = ErrorType.OK;
		List<String> errorMsgList = new ArrayList<>();
		
		Exception exception = null;
		
		public boolean hasProblems()
		{
			if ( ! ErrorType.OK.equals(errorType) )
				return true;
			
			if ( ! errorMsgList.isEmpty() )
				return true;

			if ( exception != null )
				return true;

			return false;
		}
	}
//	private static class DbTableColumn
//	{
//		DbTable dbTable;
//	}
//	private static class DbTableIndex
//	{
//		DbTable dbTable;
//	}

	
	private static class SourceException extends Exception
	{
		private static final long serialVersionUID = 1L;

		public SourceException(SQLException ex)
		{
			super(ex);
		}
	}

	private static class TargetException extends Exception
	{
		private static final long serialVersionUID = 1L;
		
		public TargetException(SQLException ex)
		{
			super(ex);
		}
	}
	
}
