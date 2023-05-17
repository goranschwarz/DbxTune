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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Comparator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.central.DbxCentralStatistics;
import com.asetune.central.DbxCentralStatistics.ServerEntry;
import com.asetune.central.check.ReceiverAlarmCheck;
import com.asetune.central.controllers.CentralPcsReceiverController;
import com.asetune.central.controllers.ChartBroadcastWebSocket;
import com.asetune.central.pcs.DbxTuneSample.CmEntry;
import com.asetune.central.pcs.DbxTuneSample.MissingFieldException;
import com.asetune.pcs.PersistWriterToDbxCentral;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

/**
 * Read JSON files posted by any Collector that writes to a directory instead of using the default HTTP CentralPcsReceiverController 
 * <p>
 * How it works:
 * <ul>
 *   <li>Start a watch dog for new '*.json' files creation in directory: <code>${tmpDir}/DbxTune/DbxCentral/file_receiver</code> </li>
 *   <li>At start: read all existing files in the above directory (if files has been created while we were not running) </li>
 *   <li>for each file: Parse the file and send it to Central PCS handler, which will write the information to ALL installed writers.</li>
 * </ul>
 *
 */
public class CentralPcsDirectoryReceiver
{
	private static Logger _logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

	private static ReceiverDirectoryWatcher _receiveDirWatcher;

	//----------------------------------------------------------------
	// Configuration Properties
	//----------------------------------------------------------------
	public static final String  PROPKEY_storageDir = "CentralPcsDirectoryReceiver.storage.dir";
	public static final String  DEFAULT_storageDir = PersistWriterToDbxCentral.DEFAULT_writerTypeFileDir; // "${tmpDir}/DbxTune/DbxCentral/file_receiver";

	public static final String  PROPKEY_forceStart = "CentralPcsDirectoryReceiver.forceStart";
	public static final boolean DEFAULT_forceStart = false;



	public static String getResolvedStorageDir()
	{
		// Set System property 'tmpDir', which will be used to replace '${tmpDir}' in the fetched value
//		System.setProperty("tmpDir", System.getProperty("java.io.tmpdir"));
//		String dirName = conf.getProperty(PROPKEY_writerTypeFileDir, DEFAULT_writerTypeFileDir);

		// Using RAW so we do NOT replace ${tmpDir} with blank... 
		String dirName = Configuration.getCombinedConfiguration().getPropertyRaw(PROPKEY_storageDir, DEFAULT_storageDir);

		// replace '${tmpDir}'
		dirName = dirName.replace("${tmpDir}", System.getProperty("java.io.tmpdir"));

		return dirName;
	}

		
	public synchronized static void startWatcher(String dirName)
	{
		if (_receiveDirWatcher == null)
		{
			_logger.info("Starting: Central PCS Directory Receiver.");

			_receiveDirWatcher = new ReceiverDirectoryWatcher(dirName);
			_receiveDirWatcher.setName("ReceiverDirectoryWatcher::"+dirName);
			_receiveDirWatcher.setDaemon(true);
			_receiveDirWatcher.start();
		}
		else
		{
			_logger.info("Central PCS Directory Receiver has already been started.");
		}
	}

	public static void stopWatcher()
	{
		if (_receiveDirWatcher != null)
		{
			_logger.info("Stopping: Central PCS Directory Receiver.");

			_receiveDirWatcher.setRunning(false);

			// Wake up the "pool" that is probably in sleep waiting for data changes
			_receiveDirWatcher.interrupt();
		}
	}
	
	
	public static class ReceiverDirectoryWatcher 
	extends Thread 
	{
		private AtomicBoolean _running = new AtomicBoolean(true);
		private String        _receiveDir;

		public ReceiverDirectoryWatcher()
		{
			this(null);
		}
		public ReceiverDirectoryWatcher(String dirName)
		{
			_receiveDir = dirName;
			if (_receiveDir == null)
				_receiveDir = getResolvedStorageDir();
		}

		/** Check if the thread is running */
		public boolean isRunning() 
		{ 
			return _running.get(); 
		}
		
		/** set to false if you want to stop the thread */
		public void setRunning(boolean to) 
		{ 
			_running.set(to); 
		}

		@Override
		public void run() 
		{
			_running.set(true); 

			_logger.info("Starting to listen for NEW files in Receive directory '" + _receiveDir + "'.");
			
			// If Receive Directory do NOT exist, create it
			File receiveDir = new File(_receiveDir);
			if ( ! receiveDir.exists() )
			{
				if ( receiveDir.mkdirs() )
				{
					_logger.info("Created the receive directory '" + receiveDir + "' since it did not exist.");
				}
				else
				{
					String msg = "The receive directory '" + receiveDir + "' did NOT exists, and I FAILED to create it... This WILL lead to issue, please create it manually.";
					_logger.error(msg);
					//throw new RuntimeException(msg);
				}
			}

			try (WatchService watchService = FileSystems.getDefault().newWatchService()) 
			{
				Path path = Paths.get(_receiveDir);
				
				// Register WHAT types we want to see
				path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
			//	path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
			//	path.register(watchService, StandardWatchEventKinds.ENTRY_DELETE);

				// Get sub-directories, and register those directories with the "watch service"
				// This will be used if we create different directories for each "server name", which I havn't planed!
				boolean watchSubDirs = false; // Always FALSE for now... please test before use/enable this...
				if (watchSubDirs)
				{
					for (Path entry : Paths.get(_receiveDir))
					{
						if (entry.toFile().isDirectory())
						{
							_logger.info("Starts to listen for NEW files in Receive SUB Directory '" + entry + "'.");
							entry.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
						}
					}
				}

				// When STARTING we should pick up all existing JSON files
				Set<Path> existingJsonFiles = Files.list(Paths.get(_receiveDir))
						.map(Path::toFile)
						.filter(f -> f.getName().toLowerCase().endsWith(".json"))
						.sorted(Comparator.comparing(File::lastModified)) // by "lastModified" OR should we sort by FILENAME (since the name reflects the time, and in that way we can influence in what order we read them)
						.map(File::toPath)
						.collect(Collectors.toSet());

				if ( ! existingJsonFiles.isEmpty() )
				{
					_logger.info("At startup: Processing " + existingJsonFiles.size() + " existing files in Receive dir '" + _receiveDir + "'. " + existingJsonFiles);

					int maxCount = existingJsonFiles.size();
					int atCount = 0;
					for (Path entry : existingJsonFiles)
					{
						atCount++;
						if (isRunning())
						{
							_logger.info("At startup: Processing existing file [" + atCount + " of " + maxCount + "] '" + entry + "'. ");
							onNewFile(entry);
						}
					}
				}
				
				while (isRunning()) 
				{
					// Wait for 10 seconds on changes, on timeout, check that we still are in running mode or stop
					WatchKey watchKey = null;;
					try { watchKey = watchService.poll(10, TimeUnit.SECONDS); }
					catch (InterruptedException ignore) {}
//					WatchKey watchKey = watchService.take();

					if (watchKey == null)
					{
						_logger.debug("After waiting for a new file, but watchKey was NULL. Most likely a 'timeout' when waiting, starting at top and waiting for new files.");
						continue; 
					}

					// Prevent receiving TWO separate ENTRY_MODIFIED events. (file-modified, timestamp-update)
					// Instead receive ONE ENTRY_MODIFIED event with count() is 2.
					Thread.sleep(100);

					// Loop received events
					for (WatchEvent<?> event : watchKey.pollEvents()) 
					{
						WatchEvent.Kind<?> kind = event.kind();

						@SuppressWarnings("unchecked")
						WatchEvent<Path> ev = (WatchEvent<Path>) event;

						Path dir = (Path)watchKey.watchable();
						Path filename = ev.context();
						Path fullPath = dir.resolve(filename);

						if (kind == StandardWatchEventKinds.OVERFLOW) 
						{
							Thread.yield();
							continue;
						} 
						else if (kind == StandardWatchEventKinds.ENTRY_CREATE)
						{
							String longFilename = fullPath.toString();
							
							// If the file is any of the files we want to "reload"
							if (longFilename.toLowerCase().endsWith(".json"))
							{
								// If we get any Runtime exceptions in here, lets catch them and continue with next file
								try
								{
									onNewFile(fullPath);
								}
								catch (Throwable ex)
								{
									_logger.error("Problems when processing file '" + filename + "'. Skipping this and continuing with next file. Caught: " + ex, ex);
								}
							}
							else
							{
								if (_logger.isDebugEnabled())
									_logger.debug("SKIPPED (not a: *.json): Received notification ENTRY_CREATE for file '" + filename + "'.");
							}
						}
						else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) 
						{ 
							/* NOT HANDLED, or actually this will never happen since we havn't yet registered for MODIFY */ 
						}
						else if (kind == StandardWatchEventKinds.ENTRY_DELETE) 
						{ 
							/* NOT HANDLED, or actually this will never happen since we havn't yet registered for DELETE */ 
						}

						boolean valid = watchKey.reset();
						if ( ! valid ) 
						{ 
							//break; 
						}
					}
				} // end: isRunning
			} 
			catch (Throwable ex) 
			{
				_logger.error("Central PCS Directory Receiver had problem. Caught: " + ex, ex);
			}
			_logger.info("Central PCS Directory Receiver was STOPPED.");
		} // end: run()

		public void onNewFile(Path filename) 
		{
			// file NOT Exists (possibly already deleted)
			if ( ! filename.toFile().exists() )
			{
				return;
			}

			// QUESTION: Should we lock the file, before processing ???
			//           Since we are not reading from many instances, I don't see it as necessary
			
			if (_logger.isDebugEnabled())
				_logger.debug("Processing file '" + filename + "'.");

			try
			{
				// Get the JSON PayLoad from the incoming file
				String jsonPayload = new String(Files.readAllBytes(filename), StandardCharsets.UTF_8);

				// Parse the JSON String and send it to Central PCS
				CentralPcsReceiverController.parseJsonAndSaveToCentralPcs(jsonPayload);

				// Removing the file
				filename.toFile().delete();
				if (_logger.isDebugEnabled())
					_logger.debug("Removed file '" + filename + "'.");
			}
			catch(IOException ex) 
			{
				_logger.error("Problem reading the incoming JSON message for DbxTuneSample from file '" + filename + "'. NOTE: The file will NOT be deleted", ex); 
				return;
			}
			catch(MissingFieldException ex) 
			{
				_logger.error("Problem parsing incoming JSON message for DbxTuneSample from file '" + filename + "'. NOTE: The file will be MOVED with a prefix of '.FAILED'", ex); 

				// Rename file (so it wont be picked up again on a restart)
				filename.toFile().renameTo( new File(filename + ".FAILED") );
				return;
			}

		} // end: onNewFile()

	} // end: static class


	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
	// Some simple code when developing/testing WatchService
	//------------------------------------------------------------------------------------
	//------------------------------------------------------------------------------------
//	public static void main(String[] args) 
//	{
//		try
//		{
//			WatchService watchService = FileSystems.getDefault().newWatchService();
//
////			Path path = Paths.get(System.getProperty("user.home"));
//			Path path = Paths.get(System.getProperty("java.io.tmpdir"));
//			
//			System.out.println("Listening on: " + path);
//
//			path.register(watchService,
//					StandardWatchEventKinds.ENTRY_CREATE,
//					StandardWatchEventKinds.ENTRY_DELETE,
//					StandardWatchEventKinds.ENTRY_MODIFY);
//
//			WatchKey key;
//			while ((key = watchService.take()) != null) 
//			{
//				for (WatchEvent<?> event : key.pollEvents()) 
//				{
//					System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");
//				}
//				key.reset();
//			}
//		}
//		catch (Exception ex)
//		{
//			ex.printStackTrace();
//		}
//	}

	public static void main(String[] args) 
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

//		String dirName = System.getProperty("java.io.tmpdir");
//		startWatcher(dirName);
		startWatcher(null);

		// Sleep for X seconds, before we "stop"
		try { Thread.sleep(300 * 1000); } catch(InterruptedException ignore) {}
		
		stopWatcher();

		// Sleep for 1 seconds to let any eventual messages be printed
		try { Thread.sleep(1 * 1000); } catch(InterruptedException ignore) {}
	}
}