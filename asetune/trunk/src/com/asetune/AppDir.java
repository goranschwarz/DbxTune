package com.asetune;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.asetune.utils.Configuration;
import com.asetune.utils.PlatformUtils;
import com.asetune.utils.StringUtil;

public class AppDir
{
	private static char sep = File.separatorChar;

	
	
	/** Get the directory where user settings are stored. More or less the user/temporary configuration files */
	public static String getAppStoreDir()
	{
//		return System.getProperty("user.home") + sep + ".asetune";
		return System.getProperty("user.home") + sep + ".dbxtune";
	}
	
	

	public static List<String> checkCreateAppDir(String dir, PrintStream ps)
	{
		if (dir == null)
			dir = getAppStoreDir();
		
		List<String> logList = new LinkedList<>();

		// If it's "the new app directory" and the old exists...
		// Then try to move/merge the old to the new structure
		boolean upgardeFromAsetuneToDbxtune = false;
		if (dir.endsWith(".dbxtune"))
		{
			File oldDir = new File(System.getProperty("user.home") + File.separator + ".asetune");
			if (oldDir.exists())
			{
				log(ps, logList, "Found old application directory '"+oldDir+"'. This will be upgraded to '"+dir+"'.");
				upgardeFromAsetuneToDbxtune = true;
			}
		}

		boolean doDbxTuneDirExistsAtStart = new File(dir).exists();
		if (doDbxTuneDirExistsAtStart)
			log(ps, logList, "INFO: The application directory '"+dir+"' already exists. So I will probably do nothing in here.");
		
		// To simulate a windows box during development, simply set: isWindows = false
		boolean isWindows = PlatformUtils.getCurrentPlattform() == PlatformUtils.Platform_WIN;
		//isWindows = false; // To simulate a Unix/Linux/Mac box during development, simply set: isWindows = false

		String dbxHome = Configuration.getCombinedConfiguration().getProperty("DBXTUNE_HOME");
		if (StringUtil.hasValue(dbxHome))
		{
			// Check if DBXTUNE_HOME looks like 'asetune_2018-06-28'
			// if it does try to check if a soft link '0' exists... if so try to use that instead
			if (dbxHome.matches(".*_[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"))
			{
				File dbxHomeParent = new File(dbxHome).getParentFile();
				
				File zeroLinkFile = new File(dbxHomeParent, "0");
				if (zeroLinkFile.exists())
				{
					log(ps, logList, "Found that 'DBXTUNE_HOME' is not a soft link. DBXTUNE_HOME='"+dbxHome+"', BUT the parent directory has a '0' soft link. I'm going to use that instead.");
					dbxHome = zeroLinkFile.toString();
					log(ps, logList, "Setting DBXTUNE_HOME='"+dbxHome+"' during the application dir creation/upgrade.");
				}
			}
		}
		String dbxHomeRes = dbxHome + sep + "resources" + sep + "dbxcentral" + sep + "scripts" + sep;
		String dbxHomeBin = dbxHome + sep + "bin"       + sep;
		

		File baseLogCreated = null;
		File baseConfCreated = null;

		// Create "${HOME}/.dbxtune"
		mkdir(dir, null, ps, logList, "- to hold various files for "+Version.getAppName());
		
		// Create "${HOME}/.dbxtune/log", conf
		baseLogCreated  = mkdir(dir, "log",  ps, logList, "- where log files are stored.");
		baseConfCreated = mkdir(dir, "conf", ps, logList, "- where properties/configuration files are located...");
		
		// Create "${HOME}/.dbxtune/dbxc
		File dbxcCreated = null;
		if (isWindows)
		{
			// Nothing "yet" for Windows... dont even create the directories
			// If the directories do not exists, then we know that in the future and can add/copy files when we have any implementation for Windows
			dbxcCreated = null;
		}
		else
		{
			dbxcCreated = mkdir(dir, "dbxc", ps, logList, "- where user/localized DbxCentral files are located...");
		}

		// If the 'dbxc' directory was created
		if (dbxcCreated != null)
		{
			File dbxcBin  = null; 
			File dbxcLog  = null; 
			File dbxcConf = null; 
			File dbxcData = null; 

			// Create "${HOME}/.dbxtune/dbxc/": bin, log, conf, data
			dbxcBin  = mkdir(dir, "dbxc" + sep + "bin",  ps, logList, "- DbxCentral local start files.");
			dbxcLog  = mkdir(dir, "dbxc" + sep + "log",  ps, logList, "- DbxCentral log files.");
			dbxcConf = mkdir(dir, "dbxc" + sep + "conf", ps, logList, "- DbxCentral local configuration files.");
			dbxcData = mkdir(dir, "dbxc" + sep + "data", ps, logList, "- DbxCentral database recording files. (NOTE: make a soft-link to location which has enough storage.)");

			if (isWindows)
			{
				// Nothing "yet" for Windows... dont even create the directories
				// If the directories do not exists, then we know that in the future and can add/copy files when we have any implementation for Windows
			}
			else
			{
				if (StringUtil.isNullOrBlank(dbxHome))
				{
					log(ps, logList, "ERROR: Can't get environment DBXTUNE_HOME. Can't copy files from '${DBXTUNE_HOME}/resource/' to '"+dbxcCreated+"'.");
				}
				else
				{
					// Also create a bunch of files in 'dbxc', for an easy start
					if (dbxcBin != null)
					{
						String srcDir = dbxHomeRes;

						if (isWindows)
						{
							log(ps, logList, "DbxCentral for Windows does not have any starter files for the moment (not yet implemented for Windows).");
						}
						else
						{
							// Maybe we should create a Symbolic Link instead...
							//copyFileToDir(srcDir + "list_ALL.sh",               dbxcBin, ps, logList, "");
							//copyFileToDir(srcDir + "start_ALL.sh",              dbxcBin, ps, logList, "");
							//copyFileToDir(srcDir + "stop_ALL.sh",               dbxcBin, ps, logList, "");
							//copyFileToDir(srcDir + "dbxPassword.sh",            dbxcBin, ps, logList, "");

							// It's probably better to make symbilic links from: ${HOME}./dbxtune/dbxc/bin/xxx.sh to ${DBXTUNE_HOME}/bin/xxx.sh 
							String dbxcBinStr = dbxcBin.toString() + File.separatorChar;
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
						String srcDir = dbxHomeRes+"conf"+sep;

						copyFileToDir(srcDir + "SERVER_LIST",                    dbxcConf, ps, logList, "- What Servers should be started/listed/stopped by 'dbxc_{start|list|stop}_ALL.sh'");
						copyFileToDir(srcDir + "DBX_CENTRAL.conf",               dbxcConf, ps, logList, "- Config file for DbxCentral Server");
						copyFileToDir(srcDir + "AlarmEventOverride.example.txt", dbxcConf, ps, logList, "- just an example file.");

						copyFileToDir(srcDir + "ase.GENERIC.conf",               dbxcConf, ps, logList, "- Example/template Config file for Sybase ASE");
						copyFileToDir(srcDir + "rs.GENERIC.conf",                dbxcConf, ps, logList, "- Example/template Config file for Sybase Replication Server");
						copyFileToDir(srcDir + "mysql.GENERIC.conf",             dbxcConf, ps, logList, "- Example/template Config file for MySQL");
						copyFileToDir(srcDir + "postgres.GENERIC.conf",          dbxcConf, ps, logList, "- Example/template Config file for Postgres");
						copyFileToDir(srcDir + "sqlserver.GENERIC.conf",         dbxcConf, ps, logList, "- Example/template Config file for Microsoft SQL-Server");
						
					}

					// Copy some files to ${DBXTUNE_HOME} if it has a '0' soft-link / pointer
					if (dbxHome.endsWith(sep+"0") || dbxHome.endsWith(sep+"0"+sep))
					{
						String srcDir = dbxHomeRes;
						File destPath = new File(dbxHome);

						copyFileToDir(srcDir + "xtract_install_dbxtune.sh", destPath, ps, logList, "- Helper script to install new 'public/beta/in-development' DbxTune Software");
					}
				}
			}
		}

		// Upgrade/move files from ~/.asetune -> ~/.dbxtune
		if (upgardeFromAsetuneToDbxtune)
		{
			log(ps, logList, "========================================================================");
			log(ps, logList, "BEGIN: upgrade of application directory. (~/.asetune -> ~/.dbxtune)");
			log(ps, logList, "------------------------------------------------------------------------");

			String ts        = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
			File newDir      = new File(dir); //conf or Log. make mkdir return File on OK
			File oldDir      = new File(System.getProperty("user.home") + File.separator + ".asetune");
			File backupDir   = new File(System.getProperty("user.home") + File.separator + ".asetune.BACKUP_"+ts);

			String oldDirStr = System.getProperty("user.home") + File.separator + ".asetune";

			if (doDbxTuneDirExistsAtStart)
				log(ps, logList, "WARNING: The new application directory '"+newDir+"' already exists, we might run into problems when upgrading because of that.");
				
			log(ps, logList, "ACTION: Taking a backup of the old application directory '" + oldDir + "' to '" + backupDir + "', so you can move files manually if things went wrong.");
			try { 
				FileUtils.copyDirectory(oldDir, backupDir); 
			} catch(IOException ex) {
				log(ps, logList, "ERROR: When taking a backup of the old application directory '" + oldDir + "' to '" + backupDir + "'. Continuing anyway... Caught: "+ex);
			}


			log(ps, logList, "Upgrade/move files from ${HOME}/.asetune -> ${HOME}/.dbxtune  (from '"+oldDir+"', to '"+newDir+"')");

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
//						Configuration conf = new Configuration(f.toString());
//						boolean changed = false;
//						for (String key : conf.getKeys())
//						{
//							String val = conf.getProperty(key, "");
//							if (val.startsWith(oldDirStr))
//							{
//								changed = true;
//								String newVal = val.replace(oldDirStr, dir);
//								conf.setProperty(key, newVal);
//								
//								log(ps, logList, "  * Fixing property key in file '" + f + "'. key='"+key+"', from='"+val+"', to='"+newVal+"'.");
//							}
//						}
//						if (changed)
//							conf.save(true);
//						conf.close();
						
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
									String newVal = val.replace(oldDirStr, dir);
									props.setProperty(key, newVal);
									
									log(ps, logList, "  * Fixing property key in file '" + f + "'. key='"+key+"', from='"+val+"', to='"+newVal+"'.");
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
							log(ps, logList, "ERROR: When Fixing property key in file '" + f + "'. Continuing anyway... Caught: "+ex);
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
						log(ps, logList, "ERROR: When Moving: file '" + f + "', to directory '" + toDir + "'. Continuing anyway... Caught: "+ex);
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
							xmlContent = xmlContent.replace(oldDirStr, dir);
							
							log(ps, logList, "  * Found and fixed content in 'ConnectionProfiles.xml'.  Changed ${HOME}/.asetune -> ${HOME}/.dbxtune");

							FileUtils.write(connProfileFile, xmlContent);
						}
					}
				}
				catch(IOException ex)
				{
					log(ps, logList, "ERROR: When Fixing: file '" + connProfileFile + "', ${HOME}/.asetune -> ${HOME}/.dbxtune . Continuing anyway... Caught: "+ex);
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
//							boolean rename = f.renameTo(toDir);
//							if (!rename) 
//							{
//								FileUtils.copyDirectory( f, toDir );
//								FileUtils.deleteDirectory( f );
//					        }
						} catch(IOException ex) {
							log(ps, logList, "ERROR: When Moving: directory '" + f + "', to directory '" + toDir + "'. Continuing anyway... Caught: "+ex);
						}
					}
					else
					{
						log(ps, logList, "  * Moving: file '" + f + "', to directory '" + toDir + "'.");
						try { 
							FileUtils.moveFileToDirectory(f, newDir, true);
						} catch(IOException ex) {
							log(ps, logList, "ERROR: When Moving: file '" + f + "', to directory '" + toDir + "'. Continuing anyway... Caught: "+ex);
						}
					}
				}
			}
			log(ps, logList, "Done moving files/directories.");

			// Create some extra directory (in a upgrade it will probably already be there)
			mkdir(dir, "info",          ps, logList, "- where process information files are located...");


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
				File keepDir = new File(System.getProperty("user.home") + File.separator + ".asetune.UPGRADE_NOT_EMPTY_"+ts);
				log(ps, logList, "Keeping (but renaming) old application directory '" + oldDir + "', the directory still had " + files.length + " files in it, so instaed of removing it, it will be ranamed to '"+keepDir+"'.");
				
				oldDir.renameTo(keepDir);
			}

			log(ps, logList, "========================================================================");
			log(ps, logList, "END: upgrade of application directory. (~/.asetune -> ~/.dbxtune)");
			log(ps, logList, "------------------------------------------------------------------------");
		} // end: upgardeFromAsetuneToDbxtune
		
		
		// COPY Environment file
		// This has to be done AFTER moving all files in the UPGRADE false, otherwise (the move in the upgrade might fail)
		// copyFileToDir() will "not copy the file" if the file already exists in the destination 
		if (dbxcCreated != null)
		{
			if (isWindows)
			{
				String srcDir = dbxHomeRes;
				File destPath = new File(dir);
				
				copyFileToDir(srcDir + "DBXTUNE.env.bat", destPath, ps, logList, "- Environment file that will be sources by various start scripts.");
				copyFileToDir(srcDir + "sql.ini",         destPath, ps, logList, "- Sybase Directory/Name Services (like the 'hosts' file for ASE Servers).");
			}
			else
			{
				String srcDir = dbxHomeRes;
				File destPath = new File(dir);
				
				copyFileToDir(srcDir + "DBXTUNE.env", destPath, ps, logList, "- Environment file that will be sources by various start scripts.");
				copyFileToDir(srcDir + "interfaces",  destPath, ps, logList, "- Sybase Directory/Name Services (like the 'hosts' file for ASE Servers).");
			}
		}

		// Create some extra directory (in a upgrade it will probably already be there, and not created, thats why it's 'at-the-end')
		mkdir(dir, "jdbc_drivers",                           ps, logList, "- where you can put JDBC Drivers that are not part of the normal DbxTune distribution.");
		mkdir(dir, "saved_files",                            ps, logList, "- save various files, most probably SQL files used by SQL Window ('sqlw.sh' or 'sqlw.bat').");
		mkdir(dir, "resources",                              ps, logList, "- other various resources.");
		mkdir(dir, "resources" + sep + "alarm-handler-src",  ps, logList, "- User defined Alarms handling source code, can be placed here. Note: specify this with env var ${DBXTUNE_UD_ALARM_SOURCE_DIR}");

		// Create a special LOG file for the upgrade...
//		if ( upgardeFromAsetuneToDbxtune && ! logList.isEmpty() )
		if ( logList.size() > 1 ) // 1 entry will always be in there: "INFO: The application directory '"+dir+"' already exists. So I will probably do nothing in here."
		{
			String ts = new SimpleDateFormat("yyyy-MM-dd_HHmmss").format(new Date());
			File f = new File(dir + sep + "log" + sep + "AppDirCreateOrUpgrade."+ts+".log");
			try
			{
				FileUtils.writeLines(f, logList);
				log(ps, logList, "Also writing the 'application dir upgrade' information to log file '"+f+"'.");
			}
			catch(IOException ex)
			{
				log(ps, logList, "ERROR: Problems writing the 'application dir create/upgrade' information to log file '"+f+"'. Caught: "+ex);
			}
		}

		return logList;
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

		logList.add(msg);
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
				log(ps, logList, "Creating directory '"+f+"' "+comment);
				return f;
			}
			else
			{
				log(ps, logList, "WARNING: Creating directory '"+f+"' FAILED.");
			}
		}
		return null;
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
		if (StringUtil.isNullOrBlank(srcFile)) throw new RuntimeException("srcFile can't be null or blank.");
		if (destDir == null)                   throw new RuntimeException("destDir can't be null or blank.");
		
		File src  = new File(srcFile);
		
		try
		{
	        File destFile = new File(destDir, src.getName());
	        if (destFile.exists())
	        {
				log(ps, logList, "  * SKIPPING copy '"+srcFile+"', reason: File '"+destFile+"' already exists in the destination directory.");
				return false;
	        }

			FileUtils.copyFileToDirectory(src, destDir);
			log(ps, logList, "  * Copy file '"+src+"' to directory '"+destDir+"' succeeded. "+comment);

			if (destFile.toString().endsWith(".sh"))
			{
				log(ps, logList, "    ** chmod 755, to file '"+destFile+"'.");
				
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
					log(ps, logList, "WARNING: chmod 755 to file '"+destFile+"' FAILED. Continuing anyway. Caught: "+ex);
				}
			}
			return true;
		}
		catch(IOException ex)
		{
			log(ps, logList, "ERROR: Copy file '"+src+"' to directory '"+destDir+"' failed. Caught: "+ex);
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
			log(ps, logList, "  * SKIPPING Create Symbolic Link, reason: File '"+symLinkPath+"' already exists.");
			return false;
		}

		try
		{
			Files.createSymbolicLink(symLinkPath, existingFilePath);
			log(ps, logList, "  * Create Symbolic Link from '"+symLinkName+"' to '"+existingFile+"' succeeded. "+comment);
			return true;
		}
		catch(Exception ex)
		{
			log(ps, logList, "ERROR: Create Symbolic Link from '"+symLinkName+"' to '"+existingFile+"' failed. Caught: "+ex);
			return false;
		}
	}
}
