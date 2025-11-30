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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.JavaUtils;
import com.dbxtune.utils.PlatformUtils;
import com.dbxtune.utils.StringUtil;

public class AppDir
{
	private static char sep = File.separatorChar;

	

//	getAppStoreDir
	/** Get the directory where user settings are stored. More or less the user/temporary configuration files */
	public static String getDbxUserHomeDir()
	{
		return getDbxUserHomeDir(false);
	}

	/**
	 * Get the directory where user settings are stored. More or less the user/temporary configuration files
	 * @param appendSeparator  Append File.separatorChar at the end
	 * @return
	 */
	public static String getDbxUserHomeDir(boolean appendSeparator)
	{
//		return System.getProperty("user.home") + sep + ".dbxtune" + (appendSeparator ? sep : "");
		
		String dbxUserHome = System.getProperty("user.home") + sep + ".dbxtune";
		dbxUserHome = System.getProperty("DBXTUNE_USER_HOME", dbxUserHome);

		if (appendSeparator)
			dbxUserHome += sep;
		
		return dbxUserHome;
	}
	
	/** Where is DbxTune Collector directories located */
	public static String getAppDataDir() { return System.getProperty("DBXTUNE_SAVE_DIR", getDbxUserHomeDir(true) + "data"); }
	public static String getAppLogDir()  { return System.getProperty("DBXTUNE_LOG_DIR" , getDbxUserHomeDir(true) + "log"); }
	public static String getAppConfDir() { return System.getProperty("DBXTUNE_CONF_DIR", getDbxUserHomeDir(true) + "conf"); }
	public static String getAppInfoDir() { return System.getProperty("DBXTUNE_INFO_DIR", getDbxUserHomeDir(true) + "info"); }

	public static List<String> checkCreateAppDir(String dbxUserHome, PrintStream ps, CommandLine cmd)
//	throws Exception
	{
		if (dbxUserHome == null)
			dbxUserHome = getDbxUserHomeDir();
		
		List<String> logList = new LinkedList<>();

		// If it's "the new app directory" and the old exists...
		// Then try to move/merge the old to the new structure
		boolean upgradeFromAsetuneToDbxtune = false;
		if (dbxUserHome.endsWith(".dbxtune"))
		{
			File oldDir = new File(System.getProperty("user.home") + File.separator + ".asetune");
			if (oldDir.exists())
			{
				log(ps, logList, "Found old application directory '" + oldDir + "'. This will be upgraded to '" + dbxUserHome + "'.");
				upgradeFromAsetuneToDbxtune = true;
			}
		}
		
//		TODO; // On Windows: Create stacktrace and see if it's called from "DbxCentral"... 
//		      // Check if we can create Symbolic Links: if NOT -- Error out with instructions

		
		// Get MainClass -- Or who started the App
		// This will be used to check if we call it from >>>> DbxTuneCentral <<<< 
		boolean isDbxCentral = JavaUtils.isMainStartClass("DbxTuneCentral");

		// Check if '$HOME/.dbxtune' exists
		boolean doDbxTuneDirExistsAtStart = new File(dbxUserHome).exists();
		if (doDbxTuneDirExistsAtStart)
			log(ps, logList, "INFO: The application directory '" + dbxUserHome + "' already exists. So I will probably do nothing in here.");
		
		// To simulate a windows box during development, simply set: isWindows = false
		boolean isWindows = PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN;
		//isWindows = false; // To simulate a Unix/Linux/Mac box during development, simply set: isWindows = false

		String dbxHome = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_HOME");
		if (StringUtil.hasValue(dbxHome))
		{
			// Check if DBXTUNE_HOME looks like 'dbxtune_2018-06-28'
			// if it does try to check if a soft link '0' exists... if so try to use that instead
			if (dbxHome.matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
			{
				File dbxHomeParent = new File(dbxHome).getParentFile();
				
				File zeroLinkFile = new File(dbxHomeParent, "0");
				if (zeroLinkFile.exists())
				{
					log(ps, logList, "Found that 'DBXTUNE_HOME' is not a soft link. DBXTUNE_HOME='" + dbxHome + "', BUT the parent directory has a '0' soft link. I'm going to use that instead.");
					dbxHome = zeroLinkFile.toString();
					log(ps, logList, "Setting DBXTUNE_HOME='" + dbxHome + "' during the application dir creation/upgrade.");
				}
			}
		}
		String dbxHomeBin                       = dbxHome + sep + "bin"       + sep;
		String dbxHomeResourceBin               = dbxHome + sep + "resources" + sep + "bin" + sep;
		String dbxHomeResourceDbxcentralScripts = dbxHome + sep + "resources" + sep + "dbxcentral" + sep + "scripts" + sep;
		

		File baseLogCreated = null;
		File baseConfCreated = null;
		File baseDataCreated = null;

		// Create "${HOME}/.dbxtune"
		mkdir(dbxUserHome, null, ps, logList, "- to hold various files for " + Version.getAppName());
		
		// Create "${HOME}/.dbxtune/log", conf
		baseLogCreated  = mkdir(dbxUserHome, "log",  ps, logList, "- where log files are stored.");
		baseConfCreated = mkdir(dbxUserHome, "conf", ps, logList, "- where properties/configuration files are located...");
		baseDataCreated = mkdir(dbxUserHome, "data", ps, logList, "- where local recording files could be located...");

		// Create "${HOME}/.dbxtune/dbxc
		File dbxcCreated = null;
		File dbxcReports = null; 
		if (isWindows)
		{
			// Nothing "yet" for Windows... dont even create the directories
			// If the directories do not exists, then we know that in the future and can add/copy files when we have any implementation for Windows
			dbxcCreated = null;
			
			// BUT if it's DbxCentral... Then we NEED to do A LOT MORE
			if (isDbxCentral)
			{
				boolean hasDbxCentralDir = new File(dbxUserHome + sep + "dbxc").exists();
				boolean startedWith_createAppDir = cmd == null ? false : cmd.hasOption("createAppDir");

				if ( ! hasDbxCentralDir )
				{
					// First CHECK if we can do symbolic links
					// if NOT: we need to follow the installation requirements -- https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_0_install_windows.md
					if (startedWith_createAppDir)
					{
					}
					if ( ! testCreateSymbolicLink(dbxUserHome, ps) )
					{
						System.out.println();
						System.out.println("-------------------------------------------------------------------------------------------------------------------------");
						System.out.println(">> Can't create a 'dummy' Symbolic Link. When installing DbxCentral directory structure we NEED to create symbolic links.");
						System.out.println(">> Follow the Installation Requirements at: https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_0_install_windows.md");
						System.out.println("-------------------------------------------------------------------------------------------------------------------------");
						System.out.println();
						throw new RuntimeException("Can't create a 'dummy' Symbolic Link. When installing DbxCentral we NEED to create symbolic links. Follow the Installation Requirements at: https://github.com/goranschwarz/DbxTune/blob/master/README_dbxcentral_0_install_windows.md");
					}

					dbxcCreated = mkdir(dbxUserHome, "dbxc",                    ps, logList, "- where user/localized DbxCentral files are located...");
					
					// Do this up here (if it's version 2019-06-xx since the 'dbxc' dir already exists... and wont be created in below logic
					dbxcReports = mkdir(dbxUserHome, "dbxc" + sep + "reports",  ps, logList, "- DbxCentral reports created by 'Daily Report', etc.");
				}
			}
		}
		else
		{
			dbxcCreated = mkdir(dbxUserHome, "dbxc",                    ps, logList, "- where user/localized DbxCentral files are located...");
			
			// Do this up here (if it's version 2019-06-xx since the 'dbxc' dir already exists... and wont be created in below logic
			dbxcReports = mkdir(dbxUserHome, "dbxc" + sep + "reports",  ps, logList, "- DbxCentral reports created by 'Daily Report', etc.");
		}

		// If the 'dbxc' directory was created
		if (dbxcCreated != null)
		{
			File dbxcBin     = null; 
			File dbxcLog     = null; 
			File dbxcConf    = null; 
			File dbxcData    = null; 

			// Create "${HOME}/.dbxtune/dbxc/": bin, log, conf, data
			dbxcBin     = mkdir(dbxUserHome, "dbxc" + sep + "bin",      ps, logList, "- DbxCentral local start files.");
			dbxcLog     = mkdir(dbxUserHome, "dbxc" + sep + "log",      ps, logList, "- DbxCentral log files.");
			dbxcConf    = mkdir(dbxUserHome, "dbxc" + sep + "conf",     ps, logList, "- DbxCentral local configuration files.");
			dbxcConf    = mkdir(dbxUserHome, "dbxc" + sep + "info",     ps, logList, "- DbxCentral local info files.");
//			dbxcReports = mkdir(dir, "dbxc" + sep + "reports",  ps, logList, "- DbxCentral reports created by 'Daily Report', etc.");
			dbxcData    = mkdir(dbxUserHome, "dbxc" + sep + "data",     ps, logList, "- DbxCentral database recording files. (NOTE: make a soft-link to location which has enough storage.)");

			if (StringUtil.isNullOrBlank(dbxHome))
			{
				log(ps, logList, "ERROR: Can't get environment DBXTUNE_HOME. Can't copy files from '${DBXTUNE_HOME}/resource/' to '" + dbxcCreated + "'.");
			}
			else
			{
				// Also create a bunch of files in 'dbxc', for an easy start
				if (dbxcBin != null)
				{
					String srcDir = dbxHomeResourceDbxcentralScripts;
					String dbxcBinStr = dbxcBin.toString() + File.separatorChar;

					if (isWindows)
					{
//						log(ps, logList, "DbxCentral for Windows does not have any starter files for the moment (not yet implemented for Windows).");
						
//FIXME: The Below files do not yet exist
						createSymbolicLink(dbxcBinStr + "list_ALL.ps1",         dbxHomeBin + "dbxc_list_ALL.ps1",  ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						createSymbolicLink(dbxcBinStr + "start_ALL.ps1",        dbxHomeBin + "dbxc_start_ALL.ps1", ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						createSymbolicLink(dbxcBinStr + "stop_ALL.ps1",         dbxHomeBin + "dbxc_stop_ALL.ps1",  ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						createSymbolicLink(dbxcBinStr + "dbxPassword.bat",      dbxHomeBin + "dbxPassword.bat",    ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
//						createSymbolicLink(dbxcBinStr + "list_ALL.bat",         dbxHomeBin + "dbxc_list_ALL.bat",  ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
//						createSymbolicLink(dbxcBinStr + "start_ALL.bat",        dbxHomeBin + "dbxc_start_ALL.bat", ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
//						createSymbolicLink(dbxcBinStr + "stop_ALL.bat",         dbxHomeBin + "dbxc_stop_ALL.bat",  ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
//						createSymbolicLink(dbxcBinStr + "dbxPassword.bat",      dbxHomeBin + "dbxPassword.bat",    ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
//						
//						
						copyFileToDir(srcDir + "start_dbxcentral.bat",       dbxcBin, ps, logList, "- Change this for customer specializations to DbxCentral Server.");
						copyFileToDir(srcDir + "start_asetune.bat",          dbxcBin, ps, logList, "- Change this for customer specializations to AseTune Collectors.");
						copyFileToDir(srcDir + "start_rstune.bat",           dbxcBin, ps, logList, "- Change this for customer specializations to RsTune Collectors.");
						copyFileToDir(srcDir + "start_sqlservertune.bat",    dbxcBin, ps, logList, "- Change this for customer specializations to SqlServerTune Collectors.");
						copyFileToDir(srcDir + "start_postgrestune.bat",     dbxcBin, ps, logList, "- Change this for customer specializations to PostgresTune Collectors.");
						copyFileToDir(srcDir + "start_mysqltune.bat",        dbxcBin, ps, logList, "- Change this for customer specializations to MySqlTune Collectors.");

						// OR: should we create a LINK here instead?
						copyFileToDir(dbxHomeResourceBin + "nssm.exe",       dbxcBin, ps, logList, "- Non Sucking Service Manager, to create services.");
//						copyFileToDir(?????? + "create_dbxc_service.bat",    dbxcBin, ps, logList, "- Create services for DbxCentral/DbxTune.");
					}
					else
					{
						// It's probably better to make symbilic links from: ${HOME}./dbxtune/dbxc/bin/xxx.sh to ${DBXTUNE_HOME}/bin/xxx.sh 
						createSymbolicLink(dbxcBinStr + "list_ALL.sh",         dbxHomeBin + "dbxc_list_ALL.sh",  ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						createSymbolicLink(dbxcBinStr + "start_ALL.sh",        dbxHomeBin + "dbxc_start_ALL.sh", ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						createSymbolicLink(dbxcBinStr + "stop_ALL.sh",         dbxHomeBin + "dbxc_stop_ALL.sh",  ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						createSymbolicLink(dbxcBinStr + "dbxPassword.sh",      dbxHomeBin + "dbxPassword.sh",    ps, logList, "- Soft link to the DBXTUNE_HOME software install, instead of copy. Easier for new SW releases.");
						
						
						copyFileToDir(srcDir + "start_dbxcentral.sh",       dbxcBin, ps, logList, "- Change this for customer specializations to DbxCentral Server.");
						copyFileToDir(srcDir + "start_asetune.sh",          dbxcBin, ps, logList, "- Change this for customer specializations to AseTune Collectors.");
						copyFileToDir(srcDir + "start_rstune.sh",           dbxcBin, ps, logList, "- Change this for customer specializations to RsTune Collectors.");
						copyFileToDir(srcDir + "start_sqlservertune.sh",    dbxcBin, ps, logList, "- Change this for customer specializations to SqlServerTune Collectors.");
						copyFileToDir(srcDir + "start_postgrestune.sh",     dbxcBin, ps, logList, "- Change this for customer specializations to PostgresTune Collectors.");
						copyFileToDir(srcDir + "start_mysqltune.sh",        dbxcBin, ps, logList, "- Change this for customer specializations to MySqlTune Collectors.");
					}
				}

				if (dbxcConf != null)
				{
					String srcDir = dbxHomeResourceDbxcentralScripts + "conf" + sep;

					if (isWindows)
					{
						copyFileToDir(srcDir + "SERVER_LIST.windows", "SERVER_LIST", dbxcConf, ps, logList, false, "- What Servers should be started/listed/stopped by 'dbxc_{start|list|stop}_ALL.bat'");
					}
					else
					{
						copyFileToDir(srcDir + "SERVER_LIST",                    dbxcConf, ps, logList, "- What Servers should be started/listed/stopped by 'dbxc_{start|list|stop}_ALL.sh'");
					}

					copyFileToDir(srcDir + "DBX_CENTRAL.conf",               dbxcConf, ps, logList, "- Config file for DbxCentral Server");
					copyFileToDir(srcDir + "AlarmEventOverride.example.txt", dbxcConf, ps, logList, "- just an example file.");

					copyFileToDir(srcDir + "ase.GENERIC.conf",               dbxcConf, ps, logList, "- Example/template Config file for Sybase ASE");
					copyFileToDir(srcDir + "rs.GENERIC.conf",                dbxcConf, ps, logList, "- Example/template Config file for Sybase Replication Server");
					copyFileToDir(srcDir + "mysql.GENERIC.conf",             dbxcConf, ps, logList, "- Example/template Config file for MySQL");
					copyFileToDir(srcDir + "postgres.GENERIC.conf",          dbxcConf, ps, logList, "- Example/template Config file for Postgres");
					copyFileToDir(srcDir + "sqlserver.GENERIC.conf",         dbxcConf, ps, logList, "- Example/template Config file for Microsoft SQL-Server");
				}

				// Copy some files to ${DBXTUNE_HOME} if it has a '0' soft-link / pointer
				if (dbxHome.endsWith(sep + "0") || dbxHome.endsWith(sep + "0" + sep))
				{
					String srcDir = dbxHomeResourceDbxcentralScripts;
					File destPath = new File(dbxHome);

					if (isWindows)
					{
//FIXME: The Below files do not yet exist
//						copyFileToDir(srcDir + "xtract_install_dbxtune.bat"     , destPath, ps, logList, "- Helper script to install new 'public/beta/in-development' DbxTune Software");
//						copyFileToDir(srcDir + "x_stop_xtract_install_start.bat", destPath, ps, logList, "- Helper script to install new 'public/beta/in-development' DbxTune Software");
					}
					else
					{
						copyFileToDir(srcDir + "xtract_install_dbxtune.sh"     , destPath, ps, logList, "- Helper script to install new 'public/beta/in-development' DbxTune Software");
						copyFileToDir(srcDir + "x_stop_xtract_install_start.sh", destPath, ps, logList, "- Helper script to install new 'public/beta/in-development' DbxTune Software");
					}
				}
			}
		} // end: 'dbxc' directory was created

		// Upgrade/move files from ~/.asetune -> ~/.dbxtune
		if (upgradeFromAsetuneToDbxtune)
		{
			upgradeFromAsetuneToDbxtune(ps, logList, isWindows, dbxUserHome, baseConfCreated, baseLogCreated);
		}
		
		
		// COPY Environment file
		// This has to be done AFTER moving all files in the UPGRADE false, otherwise (the move in the upgrade might fail)
		// copyFileToDir() will "not copy the file" if the file already exists in the destination 
		if (dbxcCreated != null)
		{
			if (isWindows)
			{
				String srcDir = dbxHomeResourceDbxcentralScripts;
				File destPath = new File(dbxUserHome);
				
				copyFileToDir(srcDir + "DBXTUNE.env.bat", destPath, ps, logList, "- Environment file that will be sources by various start scripts.");
				copyFileToDir(srcDir + "sql.ini",         destPath, ps, logList, "- Sybase Directory/Name Services (like the 'hosts' file for ASE Servers).");
				createEmtyFile("DBXTUNE.env.bat.v1"     , destPath, ps, logList, "- Empty Environment file V1 to indicate that no upgrade needs to be done.");
			}
			else
			{
				String srcDir = dbxHomeResourceDbxcentralScripts;
				File destPath = new File(dbxUserHome);
				
				copyFileToDir(srcDir + "DBXTUNE.env", destPath, ps, logList, "- Environment file that will be sources by various start scripts.");
				copyFileToDir(srcDir + "interfaces",  destPath, ps, logList, "- Sybase Directory/Name Services (like the 'hosts' file for ASE Servers).");
				createEmtyFile("DBXTUNE.env.v1",      destPath, ps, logList, "- Empty Environment file V1 to indicate that no upgrade needs to be done.");
			}
		}
		else
		{
			boolean checkEnvFile = true;
			if (checkEnvFile)
			{
				// This will upgrade DBXTUNE.env (and create a DBXTUNE.env.V1) to indicate that we already have done the upgrade. 
				checkAndUpgradeDbxtuneEnvFile(ps, logList, isWindows, dbxHome, dbxUserHome, dbxHomeResourceDbxcentralScripts);
			}
			
		}


		// Create some extra directory (in a upgrade it will probably already be there, and not created, thats why it's 'at-the-end')
		mkdir(dbxUserHome, "jdbc_drivers",                           ps, logList, "- where you can put JDBC Drivers that are not part of the normal DbxTune distribution.");
		mkdir(dbxUserHome, "saved_files",                            ps, logList, "- save various files, most probably SQL files used by SQL Window ('sqlw.sh' or 'sqlw.bat').");
		mkdir(dbxUserHome, "resources",                              ps, logList, "- other various resources.");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src",  ps, logList, "- User defined Alarms handling source code, can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");

		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "asetune",       ps, logList, "- User defined Alarms handling source code, for AseTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "db2tune",       ps, logList, "- User defined Alarms handling source code, for Db2Tune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "hanatune",      ps, logList, "- User defined Alarms handling source code, for HanaTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "iqtune",        ps, logList, "- User defined Alarms handling source code, for IqTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "mysqltune",     ps, logList, "- User defined Alarms handling source code, for MySqlTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "oracletune",    ps, logList, "- User defined Alarms handling source code, for OracleTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "postgrestune",  ps, logList, "- User defined Alarms handling source code, for PostgresTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "raxtune",       ps, logList, "- User defined Alarms handling source code, for RaxTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "rstune",        ps, logList, "- User defined Alarms handling source code, for RsTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		mkdir(dbxUserHome, "resources" + sep + "alarm-handler-src" + sep + "sqlservertune", ps, logList, "- User defined Alarms handling source code, for SqlServerTune can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");
		
		// Create a special LOG file for the upgrade...
//		if ( upgradeFromAsetuneToDbxtune && ! logList.isEmpty() )
		if ( logList.size() > 1 ) // 1 entry will always be in there: "INFO: The application directory '" + dir + "' already exists. So I will probably do nothing in here."
		{
			String ts = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
			File f = new File(dbxUserHome + sep + "log" + sep + "AppDirCreateOrUpgrade." + ts + ".log");
			try
			{
				FileUtils.writeLines(f, logList);
				log(ps, logList, "Also writing the 'application dir upgrade' information to log file '" + f + "'.");
			}
			catch(IOException ex)
			{
				log(ps, logList, "ERROR: Problems writing the 'application dir create/upgrade' information to log file '" + f + "'. Caught: " + ex);
			}
		}

		return logList;
	} // end: checkCreateAppDir()


	/**
	 * upgradeFromAsetuneToDbxtune
	 */
	public static void upgradeFromAsetuneToDbxtune(PrintStream ps, List<String> logList, boolean isWindows, String dbxUserHome, File baseConfCreated, File baseLogCreated)
	{
		log(ps, logList, "========================================================================");
		log(ps, logList, "BEGIN: upgrade of application directory. (~/.asetune -> ~/.dbxtune)");
		log(ps, logList, "------------------------------------------------------------------------");

		String ts        = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
		File newDir      = new File(dbxUserHome); //conf or Log. make mkdir return File on OK
		File oldDir      = new File(System.getProperty("user.home") + File.separator + ".asetune");
		File backupDir   = new File(System.getProperty("user.home") + File.separator + ".asetune.BACKUP_" + ts);

		boolean doDbxTuneDirExistsAtStart = new File(dbxUserHome).exists();
		
		String oldDirStr = System.getProperty("user.home") + File.separator + ".asetune";

		if (doDbxTuneDirExistsAtStart)
			log(ps, logList, "WARNING: The new application directory '" + newDir + "' already exists, we might run into problems when upgrading because of that.");
			
		log(ps, logList, "ACTION: Taking a backup of the old application directory '" + oldDir + "' to '" + backupDir + "', so you can move files manually if things went wrong.");
		try { 
			FileUtils.copyDirectory(oldDir, backupDir); 
		} catch(IOException ex) {
			log(ps, logList, "ERROR: When taking a backup of the old application directory '" + oldDir + "' to '" + backupDir + "'. Continuing anyway... Caught: " + ex);
		}


		log(ps, logList, "Upgrade/move files from ${HOME}/.asetune -> ${HOME}/.dbxtune  (from '" + oldDir + "', to '" + newDir + "')");

		log(ps, logList, "ACTION: Moving 'properties' and 'log' files.");

		// MOVE some files to specific directories
		// *.save.properties -> conf
		// *.user.properties -> conf
		// *.log*            -> log
		FileFilter fileFilter = new WildcardFileFilter(new String[] {"*.save.properties", "*.user.properties", "*.log", "*.log.*"});
		File[] files = oldDir.listFiles( fileFilter );
		for (int i=0; i<files.length; i++) 
		{
			File f = files[i];

			File toDir = null;
			if (f.toString().endsWith(".properties")) 
			{
				toDir = baseConfCreated;
				
				// FIXING property values from '~/.asetune' -> '~/.dbxtune' 
				String propFileName = f.toString();
				if (propFileName.endsWith("save.properties"))
				{
//					Configuration conf = new Configuration(f.toString());
//					boolean changed = false;
//					for (String key : conf.getKeys())
//					{
//						String val = conf.getProperty(key, "");
//						if (val.startsWith(oldDirStr))
//						{
//							changed = true;
//							String newVal = val.replace(oldDirStr, dir);
//							conf.setProperty(key, newVal);
//							
//							log(ps, logList, "  * Fixing property key in file '" + f + "'. key='" + key + "', from='" + val + "', to='" + newVal + "'.");
//						}
//					}
//					if (changed)
//						conf.save(true);
//					conf.close();
					
					try
					{
						Properties props = new Properties();
						FileInputStream fis = FileUtils.openInputStream(f);
						props.load(fis);
						boolean changed = false;
						for (Object objKey : props.keySet())
						{
							String key = objKey.toString();
							String val = props.getProperty(key, "");
							if (val.startsWith(oldDirStr))
							{
								changed = true;
								String newVal = val.replace(oldDirStr, dbxUserHome);
								props.setProperty(key, newVal);
								
								log(ps, logList, "  * Fixing property key in file '" + f + "'. key='" + key + "', from='" + val + "', to='" + newVal + "'.");
							}
						}
						if (changed)
						{
							FileOutputStream fos = FileUtils.openOutputStream(f);
							props.save(fos, "Changed by application upgrade");
							fos.close();
						}
						fis.close();
					}
					catch(IOException ex)
					{
						log(ps, logList, "ERROR: When Fixing property key in file '" + f + "'. Continuing anyway... Caught: " + ex);
					}
				}
			}
			else 
				toDir = baseLogCreated;
			
			if (toDir != null)
			{
				log(ps, logList, "  * Moving: file '" + f + "', to directory '" + toDir + "'.");
				try { 
					FileUtils.moveFileToDirectory(f, toDir, false);
				} catch(IOException ex) {
					log(ps, logList, "ERROR: When Moving: file '" + f + "', to directory '" + toDir + "'. Continuing anyway... Caught: " + ex);
				}
			}
		}
		
		// Fixing: ConnectionProfiles.xml
		File connProfileFile = new File(oldDirStr + sep + "ConnectionProfiles.xml");
		if (connProfileFile.exists())
		{
			log(ps, logList, "ACTION: Checking/fixing 'ConnectionProfiles.xml' and changing ${HOME}/.asetune -> ${HOME}/.dbxtune");
			
			try
			{
				String xmlContent = FileUtils.readFileToString(connProfileFile);
				if (StringUtil.hasValue(xmlContent))
				{
					if ( xmlContent.indexOf(oldDirStr) != -1)
					{
						xmlContent = xmlContent.replace(oldDirStr, dbxUserHome);
						
						log(ps, logList, "  * Found and fixed content in 'ConnectionProfiles.xml'.  Changed ${HOME}/.asetune -> ${HOME}/.dbxtune");

						FileUtils.write(connProfileFile, xmlContent);
					}
				}
			}
			catch(IOException ex)
			{
				log(ps, logList, "ERROR: When Fixing: file '" + connProfileFile + "', ${HOME}/.asetune -> ${HOME}/.dbxtune . Continuing anyway... Caught: " + ex);
			}
		}

		log(ps, logList, "ACTION: Moving all other files/directories");

		// MOVE REST of the files 
		files = oldDir.listFiles();
		for (int i=0; i<files.length; i++) 
		{
			File f = files[i];

			File toDir = newDir;
			if (toDir != null)
			{
				if (f.isDirectory())
				{
					log(ps, logList, "  * Moving: directory '" + f + "', to directory '" + toDir + "'.");
					try { 
						FileUtils.moveDirectoryToDirectory(f, newDir, true);
//						boolean rename = f.renameTo(toDir);
//						if (!rename) 
//						{
//							FileUtils.copyDirectory( f, toDir );
//							FileUtils.deleteDirectory( f );
//				        }
					} catch(IOException ex) {
						log(ps, logList, "ERROR: When Moving: directory '" + f + "', to directory '" + toDir + "'. Continuing anyway... Caught: " + ex);
					}
				}
				else
				{
					log(ps, logList, "  * Moving: file '" + f + "', to directory '" + toDir + "'.");
					try { 
						FileUtils.moveFileToDirectory(f, newDir, true);
					} catch(IOException ex) {
						log(ps, logList, "ERROR: When Moving: file '" + f + "', to directory '" + toDir + "'. Continuing anyway... Caught: " + ex);
					}
				}
			}
		}
		log(ps, logList, "Done moving files/directories.");

		// Create some extra directory (in a upgrade it will probably already be there)
		mkdir(dbxUserHome, "info",          ps, logList, "- where process information files are located...");


		//----------------------------------------------------------------------------------
		// AT THE END: remove the original ".asetune" directory, if it's empty
		//----------------------------------------------------------------------------------
		files = oldDir.listFiles();
		if (files.length == 0)
		{
			log(ps, logList, "ACTION: Removing old application directory '" + oldDir + "' (all the files has been moved). ");
			oldDir.delete();
		}
		else
		{
			File keepDir = new File(System.getProperty("user.home") + File.separator + ".asetune.UPGRADE_NOT_EMPTY_" + ts);
			log(ps, logList, "Keeping (but renaming) old application directory '" + oldDir + "', the directory still had " + files.length + " files in it, so instead of removing it, it will be ranamed to '" + keepDir + "'.");
			
			oldDir.renameTo(keepDir);
		}

		log(ps, logList, "========================================================================");
		log(ps, logList, "END: upgrade of application directory. (~/.asetune -> ~/.dbxtune)");
		log(ps, logList, "------------------------------------------------------------------------");
	} // end: upgradeFromAsetuneToDbxtune

//	public static void main(String[] args)
//	{
//		boolean isWindows = false;
//		List<String> logList = new LinkedList<>();
//		PrintStream ps = System.out;
//
//		String dbxHome = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_HOME", "c:/projects/DbxTune");
//		String dbxHomeResourceDbxcentralScripts = dbxHome + sep + "resources" + sep + "dbxcentral" + sep + "scripts" + sep;
//
//		String dbxUserHome = getDbxUserHomeDir();
//		
//		checkAndUpgradeDbxtuneEnvFile(ps, logList, isWindows, dbxHome, dbxUserHome, dbxHomeResourceDbxcentralScripts);
//	}

	/**
	 * checkAndUpgradeDbxtuneEnvFile
	 */
	public static void checkAndUpgradeDbxtuneEnvFile(PrintStream ps, List<String> logList, boolean isWindows, String dbxHome, String dbxUserHome, String dbxHomeResourceDbxcentralScripts)
	{
		// if it's unchanged (from the OLD version), then copy a new one!
		// if it has been CHANGED by the user... Can we "patch" it in some way ???
		String srcDir = dbxHomeResourceDbxcentralScripts;

		String envFileName = (isWindows ? "DBXTUNE.env.bat" : "DBXTUNE.env");
		
		Path systEnvFileNameV1 = Paths.get(srcDir      + sep + envFileName + ".v1");  // Version 1 of this file in resources/dbxcentral (used to check for changes)
		Path systEnvFileName   = Paths.get(srcDir      + sep + envFileName);          // This is CURRENT/LATEST System Version in resources/dbxcentral
		Path userEnvFileNameV1 = Paths.get(dbxUserHome + sep + envFileName + ".v1");  // ORIGIN if exists: The file has already been upgraded, NO Need to check again
		Path userEnvFileName   = Paths.get(dbxUserHome + sep + envFileName);          // Name of the env file (located at users '~/.dbxtune' dir)

		// Check if file exists
		if (Files.isRegularFile(userEnvFileNameV1) && Files.isRegularFile(userEnvFileName))
		{
System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX: Skipping env file check/migration. The file '" + userEnvFileNameV1 + "' already exists.");
			// No need to do upgrade checks on: DBXTUNE.env
			// It has already been done.
//			log(ps, logList, "Skipping env file check/migration. The file '" + userEnvFileNameV1 + "' already exists.");
		}
		else
		{
			try
			{
				// get content of the SYSTEM ORIGINAL VERSION 1 of the 'DBXTUNE.env' 'DBXTUNE.env.bat' file
				String systEnvFileContentV1 = Files.readString(systEnvFileNameV1).trim();

				// get content of the USERS CURRENT 'DBXTUNE.env' 'DBXTUNE.env.bat' file
				String userEnvFileContent = Files.readString(userEnvFileName).trim();

				File dbxUserHomeFile = new File(dbxUserHome);

				if (userEnvFileContent.equals(systEnvFileContentV1))
				{
					// Copy OLD ENV file to 'DBXTUNE.env.V1' (so we know that this upgrade has already been done)
					// Then: Simply copy the new ENV File
					log(ps, logList, "Installing a NEW Environent file '" + envFileName + "'. NO Changes from the original file was detected.");
					copyFileToDir(userEnvFileName,      userEnvFileNameV1, dbxUserHomeFile, ps, logList, true, "- Copy Current/Old User Environment file '" + userEnvFileName + "' to '" + userEnvFileNameV1 + "'. Next step is to replace it with a new version.");
					copyFileToDir(systEnvFileName.toString(),              dbxUserHomeFile, ps, logList, true, "- NEW Environment file '" + userEnvFileName + "' that will be sources by various start scripts.");
System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> TODO: [no-changes-to-env-file]... COPY " + envFileName + " to " + dbxUserHomeFile);
				}
				else
				{
System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> TODO: [CHANGES-TO-ENV-FILE] ---upgrade file--- " + envFileName);
					// What should we do here?
					//  - Read the current/old ENV file and check for DBXTUNE variables...
					//  - If any has "non" default values...
					//     * Append to the NEW Environment File
					//     * If we can't write to the file: Just LOG info about what is needed to be done.
					//  - Copy OLD ENV file to 'DBXTUNE.env.V1' (so we know that this upgrade has already been done)
					//  - Copy the new ENV File

					List<String> envList = new ArrayList<>();
					for (String line : userEnvFileContent.split("\n"))
					{
						line = line.trim();

						// Skip comments
						if (line.startsWith("rem ")) continue;
						if (line.startsWith("#")) continue;

						String regex = ".*(export)[ ]+(DBXTUNE_|SYBASE).*"; // "[ ]+" == one or several spaces after export 
						if (isWindows)
							regex = ".*(set|SET)[ ]+[\\\"]?(DBXTUNE_|SYBASE).*"; // "[ ]+" == one or several spaces after export, "[\"]?" == Possibly a '"' before the ENV VAR (this is for DOS)

						// get ENV variables
						if (line.matches(regex))
						{
							String keyWord = isWindows ? "set " : "export ";
							int startPos = StringUtil.indexOfIgnoreCase(line, keyWord);
							if (startPos >= 0)
							{
								startPos += keyWord.length();
								line = line.substring(startPos).trim();

								// WINDOWS: Remove any trailing/ending quotes (")   yes it may look like: set "ENV_NAME=some value"
								if (isWindows)
								{
									if (line.startsWith("\"") && line.endsWith("\"") )
									{
										line = line.substring(1);
										line = line.substring(0, line.length()-1);
									}
								}
								
							}
							envList.add(line);
						}
					}
					Map<String, String> changedEnvMap = new LinkedHashMap<>();
					for (String str : envList)
					{
						String key = StringUtils.substringBefore(str, "=").trim();
						String val = StringUtils.substringAfter (str, "=").trim();
						System.out.println("ENV: |" + str + "|, key=|"+key+"|, val=|"+val+"|.");
						
						if (isWindows)
						{
							if (StringUtil.equalsAny(key, "DBXTUNE_CENTRAL_BASE", "DBXTUNE_USER_HOME", "SYBASE", "DBXTUNE_SAVE_DIR", "DBXTUNE_LOG_DIR", "DBXTUNE_CONF_DIR", "DBXTUNE_INFO_DIR", "DBXTUNE_REPORTS_DIR"))
							{
								if      ("DBXTUNE_CENTRAL_BASE".equals(key) && ! StringUtils.equalsAny(val, "%systemdrive%%homepath%\\.dbxtune", "%USERPROFILE%\\dbxtune\0") ) changedEnvMap.put(key, val);
								else if ("DBXTUNE_USER_HOME"   .equals(key) && ! StringUtils.equalsAny(val, "%systemdrive%%homepath%\\.dbxtune", "%USERPROFILE%\\dbxtune\0" )) changedEnvMap.put(key, val);
								else if ("SYBASE"              .equals(key) && ! StringUtils.equalsAny(val, "%DBXTUNE_USER_HOME%"               )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_SAVE_DIR"    .equals(key) && ! StringUtils.equalsAny(val, "%DBXTUNE_USER_HOME%\\dbxc\\data"   )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_LOG_DIR"     .equals(key) && ! StringUtils.equalsAny(val, "%DBXTUNE_USER_HOME%\\dbxc\\log"    )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_CONF_DIR"    .equals(key) && ! StringUtils.equalsAny(val, "%DBXTUNE_USER_HOME%\\dbxc\\conf"   )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_INFO_DIR"    .equals(key) && ! StringUtils.equalsAny(val, "%DBXTUNE_USER_HOME%\\dbxc\\info"   )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_REPORTS_DIR" .equals(key) && ! StringUtils.equalsAny(val, "%DBXTUNE_USER_HOME%\\dbxc\\reports")) changedEnvMap.put(key, val);
							}
							else 
							{
								// Other "DBXTUNE_*" variables
								changedEnvMap.put(key, val);
							}
						}
						else
						{
							if (StringUtil.equalsAny(key, "DBXTUNE_HOME", "DBXTUNE_CENTRAL_BASE", "DBXTUNE_USER_HOME", "SYBASE", "DBXTUNE_SAVE_DIR", "DBXTUNE_LOG_DIR", "DBXTUNE_CONF_DIR", "DBXTUNE_INFO_DIR", "DBXTUNE_REPORTS_DIR"))
							{
								if      ("DBXTUNE_HOME"        .equals(key) && ! StringUtils.equalsAny(val, "${HOME}/dbxtune/0"              )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_CENTRAL_BASE".equals(key) && ! StringUtils.equalsAny(val, "${HOME}/.dbxtune/dbxc"          )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_USER_HOME"   .equals(key) && ! StringUtils.equalsAny(val, "${HOME}/.dbxtune"               )) changedEnvMap.put(key, val);
								else if ("SYBASE"              .equals(key) && ! StringUtils.equalsAny(val, "${SYBASE:-${DBXTUNE_USER_HOME}}")) changedEnvMap.put(key, val);
								else if ("DBXTUNE_SAVE_DIR"    .equals(key) && ! StringUtils.equalsAny(val, "${DBXTUNE_CENTRAL_BASE}/data"   )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_LOG_DIR"     .equals(key) && ! StringUtils.equalsAny(val, "${DBXTUNE_CENTRAL_BASE}/log"    )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_CONF_DIR"    .equals(key) && ! StringUtils.equalsAny(val, "${DBXTUNE_CENTRAL_BASE}/conf"   )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_INFO_DIR"    .equals(key) && ! StringUtils.equalsAny(val, "${DBXTUNE_CENTRAL_BASE}/info"   )) changedEnvMap.put(key, val);
								else if ("DBXTUNE_REPORTS_DIR" .equals(key) && ! StringUtils.equalsAny(val, "${DBXTUNE_CENTRAL_BASE}/reports")) changedEnvMap.put(key, val);
							}
							else 
							{
								// Other "DBXTUNE_*" variables
								changedEnvMap.put(key, val);
							}
						}
					}
					
					// REMOVE any values that looks like DEFAULTS
					if ( ! changedEnvMap.isEmpty() )
					{
						// The current/old system ENV File contains a fault with an extra "set " in the ENV Name, so just remove this... 
						changedEnvMap.entrySet().removeIf(e -> "set DBXTUNE_CENTRAL_BASE".equals(e.getKey()) );
						changedEnvMap.entrySet().removeIf(e -> "set DBXTUNE_USER_HOME"   .equals(e.getKey()) );

						// entries with "%ENVNAME%" is Winows entries... and "${ENVNAME}" or "$ENVNAME" is Linux/Unix entries...
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_HOME"               .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%USERPROFILE%\\dbxtune\\0",       "%systemdrive%%homepath%\\dbxtune",  "${HOME}/dbxtune/0", "$HOME/dbxtune/0"));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_USER_HOME"          .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%USERPROFILE%\\.dbxtune"  ,       "%systemdrive%%homepath%\\.dbxtune", "${HOME}/.dbxtune" , "$HOME/.dbxtune"));
                                                                                            
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_SAVE_DIR"           .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_USER_HOME%\\data",       "${DBXTUNE_USER_HOME}/data"   , "$DBXTUNE_USER_HOME/data"   ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_LOG_DIR"            .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_USER_HOME%\\log",        "${DBXTUNE_USER_HOME}/log"    , "$DBXTUNE_USER_HOME/log"    ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CONF_DIR"           .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_USER_HOME%\\conf",       "${DBXTUNE_USER_HOME}/conf"   , "$DBXTUNE_USER_HOME/conf"   ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_INFO_DIR"           .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_USER_HOME%\\info",       "${DBXTUNE_USER_HOME}/info"   , "$DBXTUNE_USER_HOME/info"   ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_REPORTS_DIR"        .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_USER_HOME%\\reports",    "${DBXTUNE_USER_HOME}/reports", "$DBXTUNE_USER_HOME/reports"));

						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CENTRAL_BASE"       .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_HOME%\\dbxc", "set %DBXTUNE_HOME%\\dbxc",            "${DBXTUNE_HOME}/dbxc", "${HOME}/dbxtune/dbxc", "$DBXTUNE_HOME/dbxc", "$HOME/dbxtune/dbxc"));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CENTRAL_SAVE_DIR"   .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_CENTRAL_BASE%\\data",    "${DBXTUNE_CENTRAL_BASE}/data"   , "$DBXTUNE_CENTRAL_BASE/data"   ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CENTRAL_LOG_DIR"    .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_CENTRAL_BASE%\\log",     "${DBXTUNE_CENTRAL_BASE}/log"    , "$DBXTUNE_CENTRAL_BASE/log"    ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CENTRAL_CONF_DIR"   .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_CENTRAL_BASE%\\conf",    "${DBXTUNE_CENTRAL_BASE}/conf"   , "$DBXTUNE_CENTRAL_BASE/conf"   ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CENTRAL_INFO_DIR"   .equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_CENTRAL_BASE%\\info",    "${DBXTUNE_CENTRAL_BASE}/info"   , "$DBXTUNE_CENTRAL_BASE/info"   ));
						changedEnvMap.entrySet().removeIf(e -> "DBXTUNE_CENTRAL_REPORTS_DIR".equals(e.getKey()) && StringUtils.equalsAny(e.getValue(), "%DBXTUNE_CENTRAL_BASE%\\reports", "${DBXTUNE_CENTRAL_BASE}/reports", "$DBXTUNE_CENTRAL_BASE/reports"));
					}

					// COPY the files
					log(ps, logList, "Installing a NEW Environent file '" + envFileName + "'. CHANGES from the original file WAS DETECTED. Trying to add the old settings to the NEW Env File (see messages below).");
					copyFileToDir(userEnvFileName,      userEnvFileNameV1, dbxUserHomeFile, ps, logList, true, "- Copy Current/Old User Environment file '" + userEnvFileName + "' to '" + userEnvFileNameV1 + "'. Next step is to replace it with a new version.");
					copyFileToDir(systEnvFileName.toString(),              dbxUserHomeFile, ps, logList, true, "- NEW Environment file '" + userEnvFileName + "' that will be sources by various start scripts.");
					
					// APPEND the NON Default environment variables to the newly copied: "DBXTUNE.env" or "DBXTUNE.env.bat"
					if ( ! changedEnvMap.isEmpty() )
					{
						String setKeyword = isWindows ? "set " : "export ";
						try
						{
							// Open file in APPEND mode
							FileWriter fw = new FileWriter(userEnvFileName.toString(), true);
							BufferedWriter bw = new BufferedWriter(fw);

							bw.newLine();
							if (isWindows)
							{
								bw.write("rem --------------------------------------------------------------------------------------------"); bw.newLine();
								bw.write("rem -- Below values was found in the old '" + envFileName + "' file. and added by a upgrade step"); bw.newLine();
								bw.write("rem --------------------------------------------------------------------------------------------"); bw.newLine();

								// Write the Changed ENV variable AT the end of the new file (which will override)
								for (Entry<String, String> entry : changedEnvMap.entrySet())
								{
									bw.write("set \"" + entry.getKey() + "=" + entry.getValue() + "\"");
									bw.newLine();

									log(ps, logList, "Appending Environment Variable '" + entry.getKey() +"' with value '" + entry.getValue() + "' to the END of then new ENV file '" + userEnvFileName + "'.");
								}
							}
							else
							{
								bw.write("##------------------------------------------------------------------------------------------"); bw.newLine();
								bw.write("## Below values was found in the old '" + envFileName + "' file. and added by a upgrade step"); bw.newLine();
								bw.write("##------------------------------------------------------------------------------------------"); bw.newLine();

								// Write the Changed ENV variable AT the end of the new file (which will override)
								for (Entry<String, String> entry : changedEnvMap.entrySet())
								{
									bw.write("export " + entry.getKey() + "=" + entry.getValue() );
									bw.newLine();

									log(ps, logList, "Appending Environment Variable '" + entry.getKey() +"' with value '" + entry.getValue() + "' to the END of then new ENV file '" + userEnvFileName + "'.");
								}
							}

							bw.close();
						}
						catch (IOException ex)
						{
							log(ps, logList, "Problems appending Changed Environment Variables to the newly created '" + userEnvFileName + "' file.");
							for (Entry<String, String> entry : changedEnvMap.entrySet())
								log(ps, logList, "MANUALLY ADD THE FOLLOWING ENTRIES: " + setKeyword + entry.getKey() + "=" + entry.getValue());
						}
					}
				}
			}
			catch (IOException ex)
			{
				log(ps, logList, "Skipping env file check/migration. Caught: " + ex + "\n" + StringUtil.exceptionToString(ex));
			}
		}
	}


	/**
	 * Test if we can create a "symbolic link" in "java.io.tmpdir" <br>
	 *  - 1: to a File <br>
	 *  - 2: to a Directory <br>
	 *  
	 * @param dir    Directory where to create them... (if null 'System.getProperty("java.io.tmpdir")' is used) 
	 * @param ps     Print stream
	 * 
	 * @return true of SUCCESS, false on FAILURE
	 */
	/**
	 * 
	 * @param dir
	 * @param ps
	 * @return
	 */
	private static boolean testCreateSymbolicLink(String dir, PrintStream ps)
	{
		boolean success = false;
		
		if (dir == null)
			dir = System.getProperty("java.io.tmpdir");

		String baseName = "testCreateSymbolicLink__";
		
		String fileUuid = UUID.randomUUID().toString();
		Path testFileName = Paths.get(dir + sep + baseName + "file_"      + fileUuid);
		Path testFileLink = Paths.get(dir + sep + baseName + "file_link_" + fileUuid);
		
		String dirUuid = UUID.randomUUID().toString();
		Path testDirName  = Paths.get(dir + sep + baseName + "dir_"       + dirUuid);
		Path testDirLink  = Paths.get(dir + sep + baseName + "dir_link_"  + dirUuid);

		try
		{
			ps.println("INFO: testCreateSymbolicLink - Creating a dummy File    '" + testFileName + "'.");
			Files.createFile(testFileName);

			ps.println("INFO: testCreateSymbolicLink - Creating a symbolic link '" + testFileLink + "' that points to File '" + testFileName + "'.");
			Files.createSymbolicLink(testFileLink, testFileName);

			ps.println("INFO: testCreateSymbolicLink - Creating a dummy Directory '" + testDirName + "'.");
			Files.createDirectories(testDirName);

			ps.println("INFO: testCreateSymbolicLink - Creating a symbolic link   '" + testDirLink + "' that points to Directory '" + testDirName + "'.");
			Files.createSymbolicLink(testDirLink, testDirName);

			success = true;
		}
		catch(IOException ex)
		{
			ps.println("ERROR: Problems when testing to create symbolic links (to File and Directory), Caught: " + ex);
			success = false;
		}
		finally
		{
			// Remove the above temporary files
			try
			{
				if (Files.deleteIfExists(testFileLink)) ps.println("INFO: testCreateSymbolicLink - Removed dummy File symbolic link '" + testFileLink + "'.");
				if (Files.deleteIfExists(testFileName)) ps.println("INFO: testCreateSymbolicLink - Removed dummy File '" + testFileName + "'.");
				
				if (Files.deleteIfExists(testDirLink)) ps.println("INFO: testCreateSymbolicLink - Removed dummy Directory symbolic link '" + testDirLink + "'.");
				if (Files.deleteIfExists(testDirName)) ps.println("INFO: testCreateSymbolicLink - Removed dummy Directory '" + testDirName + "'.");
			}
			catch(IOException ex)
			{
				ps.println("Problems cleaning up the temporary file when testing to create symbolic links (to File and Directory), Caught: " + ex);
			}
		}

		return success;
	}

	/**
	 * Print information to the PrintStream and stuff it in a linked list
	 * @param ps
	 * @param logList
	 * @param msg
	 */
	private static void log(PrintStream ps, List<String> logList, String msg)
	{
		if (ps != null)
			ps.println(msg);

		if (logList != null)
			logList.add(msg);
	}

	/**
	 * Check if directory exists
	 * 
	 * @param dir
	 * @param ps
	 */
	private static boolean dirExists(String baseDir, String subDir)
	{
		if (StringUtil.isNullOrBlank(baseDir))
			throw new RuntimeException("baseDir can't be null or blank.");
		
		String dir = baseDir;
		if (StringUtil.hasValue(subDir)) 
			dir = baseDir + sep + subDir;
		
		File f = new File(dir);
		return f.exists();
	}
	
	/**
	 * Create directory
	 * 
	 * @param dir
	 * @param ps
	 * @param isAppStoreDir
	 * @return returns File on OK, else NULL (if dir already exists, also NULL)
	 */
	private static File mkdir(String baseDir, String subDir, PrintStream ps, List<String> logList, String comment)
	{
		if (StringUtil.isNullOrBlank(baseDir))
			throw new RuntimeException("baseDir can't be null or blank.");
		
		String dir = baseDir;
		if (StringUtil.hasValue(subDir)) 
			dir = baseDir + sep + subDir;
		
		File f = new File(dir);
		if ( ! f.exists() )
		{
			if (f.mkdir())
			{
				log(ps, logList, "Creating directory '" + f + "' " + comment);
				return f;
			}
			else
			{
				log(ps, logList, "WARNING: Creating directory '" + f + "' FAILED.");
			}
		}
		return null;
	}
	
	/**
	 * createEmtyFile
	 * 
	 * @param srcFile
	 * @param destDir
	 * @param ps
	 * @param logList
	 * @param comment
	 * @return
	 */
	private static boolean createEmtyFile(String filename, File destDir, PrintStream ps, List<String> logList, String comment)
	{
		// Specify the directory and file name
		Path dirPath = destDir.toPath();
		Path filePath = dirPath.resolve(filename);

		try 
		{
			// Ensure the directory exists
			if (!Files.exists(dirPath))
				Files.createDirectories(dirPath);

			// Create the file
			Files.createFile(filePath);
			log(ps, logList, "Empty file created succeeded '" + filePath.toAbsolutePath() + "'. " + comment);
			
			return true;
		} 
		catch (IOException ex) 
		{
			log(ps, logList, "createEmtyFile(): Error creating file, caught: " + ex);
			return true;
		}
	}

	/**
	 * Copy file
	 * 
	 * @param srcFile
	 * @param destDir
	 * @param ps
	 * @param logList
	 * @param comment
	 * @return
	 */
	private static boolean copyFileToDir(String srcFile, File destDir, PrintStream ps, List<String> logList, String comment)
	{
		return copyFileToDir(srcFile, null, destDir, ps, logList, false, comment);
	}

	/**
	 * Copy file
	 * 
	 * @param srcFile
	 * @param destDir
	 * @param ps
	 * @param logList
	 * @param overwriteExistingFile
	 * @param comment
	 * @return
	 */
	private static boolean copyFileToDir(String srcFile, File destDir, PrintStream ps, List<String> logList, boolean overwriteIfExists, String comment)
	{
		return copyFileToDir(srcFile, null, destDir, ps, logList, overwriteIfExists, comment);
	}

//	private static boolean copyFileToDir(Path srcFile, Path destDir, PrintStream ps, List<String> logList, boolean overwriteIfExists, String comment)
//	{
//		return copyFileToDir(srcFile.toString(), null, destDir.toFile(), ps, logList, overwriteIfExists, comment);
//	}
	private static boolean copyFileToDir(Path srcFilePath, Path destFilePath, File destDir, PrintStream ps, List<String> logList, boolean overwriteIfExists, String comment)
	{
		return copyFileToDir(srcFilePath.toString(), destFilePath.toString(), destDir, ps, logList, overwriteIfExists, comment);
	}
	/**
	 * Copy file
	 * 
	 * @param srcFileStr
	 * @param destFileStr
	 * @param destDir
	 * @param ps
	 * @param logList
	 * @param comment
	 * @return
	 */
	private static boolean copyFileToDir(String srcFileStr, String destFileStr, File destDir, PrintStream ps, List<String> logList, boolean overwriteIfExists, String comment)
	{
		if (StringUtil.isNullOrBlank(srcFileStr )) throw new RuntimeException("srcFileStr can't be null or blank.");
//		if (StringUtil.isNullOrBlank(destFileStr)) throw new RuntimeException("destFileStr can't be null or blank.");
		if (destDir == null)                       throw new RuntimeException("destDir can't be null or blank.");
		
		File srcFile  = new File(srcFileStr);
		
System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>> copyFileToDir: srcFileStr=|" + srcFileStr + "|, destFileStr=|" + destFileStr + "|, destDir=|" + destDir + "|.");
		try
		{
	        File destFile = new File(destDir, srcFile.getName());
	        if (destFileStr == null)
	        	destFileStr = destFile.getName();        	
	        else
				destFile = new File(destDir, new File(destFileStr).getName());

	        if (destFile.exists() && destFile.isFile() && !overwriteIfExists)
	        {
				log(ps, logList, "  * SKIPPING copy '" + srcFileStr + "', reason: File '" + destFile + "' already exists in the destination directory.");
				return false;
	        }

//			FileUtils.copyFileToDirectory(srcFile, destDir);
			FileUtils.copyFile(srcFile, destFile);
			log(ps, logList, "  * Copy file '" + srcFile + "' as '" + destFileStr + "' to directory '" + destDir + "' succeeded. " + comment);

			if (destFile.toString().endsWith(".sh"))
			{
				log(ps, logList, "    ** chmod 755, to file '" + destFile + "'.");
				
				//using PosixFilePermission to set file permissions 755
				Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();

				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				perms.add(PosixFilePermission.OWNER_EXECUTE);

				perms.add(PosixFilePermission.GROUP_READ);
				perms.add(PosixFilePermission.GROUP_EXECUTE);

				perms.add(PosixFilePermission.OTHERS_READ);
				perms.add(PosixFilePermission.OTHERS_EXECUTE);
				
				try
				{
					Files.setPosixFilePermissions(destFile.toPath(), perms);
				}
				catch(IOException ex)
				{
					log(ps, logList, "WARNING: chmod 755 to file '" + destFile + "' FAILED. Continuing anyway. Caught: " + StringUtil.stackTraceToString(ex));
				}
			}
			return true;
		}
		catch(IOException ex)
		{
			log(ps, logList, "ERROR: Copy file '" + srcFile + "' to directory '" + destDir + "' failed. Caught: " + StringUtil.stackTraceToString(ex));
			return false;
		}
	}


	/**
	 * do: ln -s fysicalName linkName
	 * 
	 * @param symLinkName    Name of the link
	 * @param existingFile   Name of the existing file the link should "point" to
	 * @param ps
	 * @param logList
	 * @param comment
	 * @return
	 */
	private static boolean createSymbolicLink(String symLinkName, String existingFile, PrintStream ps, List<String> logList, String comment)
	{
		if (StringUtil.isNullOrBlank(existingFile)) throw new RuntimeException("existingFile can't be null or blank.");
		if (StringUtil.isNullOrBlank(symLinkName))  throw new RuntimeException("symLinkName can't be null or blank.");

		Path existingFilePath = Paths.get(existingFile);
		Path symLinkPath      = Paths.get(symLinkName);

		if (symLinkPath.toFile().exists())
		{
			log(ps, logList, "  * SKIPPING Create Symbolic Link, reason: File '" + symLinkPath + "' already exists.");
			return false;
		}

		try
		{
			Files.createSymbolicLink(symLinkPath, existingFilePath);
			log(ps, logList, "  * Create Symbolic Link from '" + symLinkName + "' to '" + existingFile + "' succeeded. " + comment);
			return true;
		}
		catch(Exception ex)
		{
			log(ps, logList, "ERROR: Create Symbolic Link from '" + symLinkName + "' to '" + existingFile + "' failed. Caught: " + StringUtil.stackTraceToString(ex));
			return false;
		}
	}
}
