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
package com.dbxtune.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.dbxtune.central.controllers.ud.chart.UserDefinedChartManager;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class FileWatchTest
{

	public static class FileWatcher extends Thread 
	{
		private AtomicBoolean _running = new AtomicBoolean(true);

		private static List<String> _onChange_SkipKeyPrefix;

		/** Set the: Skip Key Prefix List */
		public void setSkipKeyPrefix(List<String> list)
		{
			_onChange_SkipKeyPrefix = list; 
		}

		/** Add to the: Skip Key Prefix List */
		public void addSkipKeyPrefix(String skipKeyPrefix)
		{
			if (_onChange_SkipKeyPrefix == null)
				_onChange_SkipKeyPrefix = new ArrayList<>();

			// Check for duplicates, the add
			if ( ! _onChange_SkipKeyPrefix.contains(skipKeyPrefix) )
				_onChange_SkipKeyPrefix.add(skipKeyPrefix); 
		}

		/** 
		 * Should the changed entry be KEEPED or SKIPPED
		 *  
		 * @param conf     Configuration object
		 * @param type     1=ADDED, 2=CHANGED, 3=REMOVED
		 * @param key      property name
		 * @param newVal   new value
		 * @param oldVal   old value
		 * 
		 * @return true=KEEP, false=SKIP 
		 */
		public boolean keepFilter(Configuration conf, int type, String key, String oldVal, String newVal)
		{
			if (StringUtil.isNullOrBlank(key))
				return false;

			if (_onChange_SkipKeyPrefix == null)
				return true;
				
			for (String skipKeyPrefix : _onChange_SkipKeyPrefix)
			{
				if (key.startsWith(skipKeyPrefix))
				{
//					System.out.println("Skipping changes COMBINED CONFIG[" + conf.getConfName() + "], due to keepFilter='" + skipKeyPrefix + "' for: propName='" + key + "', type=" + changeTypeToStr(type) + ", newValue='" + newVal + "', oldValue='" + oldVal + "'.");
					System.out.println("Skipping changes COMBINED CONFIG[" + conf.getConfName() + "], due to keepFilter='" + skipKeyPrefix + "' for: propName='" + key + "', type=" + type + ", newValue='" + newVal + "', oldValue='" + oldVal + "'.");
					return false;
				}
			}

			return true;
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
//			List<String> fileNames = _combinedConfig.getFilenames();
			List<String> fileNames = new ArrayList<>(); fileNames.add(System.getProperty("user.home") + "/.profile");

			System.out.println("Starting to listen for changes in Combined Config files: " + fileNames);

			try (WatchService watchService = FileSystems.getDefault().newWatchService()) 
			{
				// If a target file for a symbolic link is changed, we want to get "notified" that the symbolic link-file was changed
				// SO: remember the target file (which gets the change-notification), so we can simulate that the "link" was changed even if it was the "target"
				HashMap<Path, Path> targetFileToSymbolicLinkMap = new HashMap<>();

				// Grab unique directories, and register those directories with the "watch service"
				Set<Path> dirs = new HashSet<>();
				for (String name : fileNames)
				{
					File f = new File(name);
					Path p = f.toPath();
					Path path = p.getParent();

					dirs.add(path);
					
//					if (Files.isSymbolicLink(p))
//					{
//						try {
//							Path targetPath = p.toRealPath();
//							System.out.println("The file '" + p.toAbsolutePath() + "' is a symbolic link that points to '" + targetPath.toAbsolutePath() + "', which we will track for changes.");
//							targetFileToSymbolicLinkMap.put(p.toRealPath(), p);
//						} catch (IOException ex) {
//							System.out.println("Problems looking up real/target name for the symbolic link '" + p.toAbsolutePath() + "', So changes wont be correctly notified for this file.");
//						}
//					}
				}
				if (dirs.isEmpty())
				{
					System.out.println("No Configuration files to listen for, filenames='" + fileNames + "'.");
					return;
				}
				for (Path path : dirs)
				{
					System.out.println("Starts to listen for Configuration file modifications in directory '" + path + "'. All Filenames: " + fileNames);
					path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

					try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) 
					{
						for (Path p : stream) 
						{
							if ( ! Files.isDirectory(p) ) 
							{
								System.out.println(" --- Start: dir contains file: " + p.toAbsolutePath());
								if (Files.isSymbolicLink(p))
								{
//									System.out.println(" --- Start: dir contains file: " + path2.toAbsolutePath());
//									System.out.println("     >>>>>> Which seems to be a symbolic link to path.toAbsolutePath(): " + path2.toAbsolutePath());
//									System.out.println("     >>>>>> Which seems to be a symbolic link to path.toRealPath()    : " + path2.toRealPath());
//									targetFileToSymbolicLinkMap.put(path2.toRealPath(), path2);
									try {
										Path targetPath = p.toRealPath();
										System.out.println("The file '" + p.toAbsolutePath() + "' is a symbolic link that points to '" + targetPath.toAbsolutePath() + "', which we will track for changes.");
										targetFileToSymbolicLinkMap.put(p.toRealPath(), p);
									} catch (IOException ex) {
										System.out.println("Problems looking up real/target name for the symbolic link '" + p.toAbsolutePath() + "', So changes wont be correctly notified for this file.");
									}
								}
							}
						}
					}
					
				}


				while (isRunning()) 
				{
					// Wait for 10 seconds on changes, on timeout, check that we still are in running mode or stop
					WatchKey watchKey = null;;
					try { watchKey = watchService.poll(10, TimeUnit.SECONDS); }
					catch (InterruptedException ignore) {}

					if (watchKey == null) 
						continue; 

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
						else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY)
						{
System.out.println("DEBUG: Configuration.WatchService: Found changes for file '" + fullPath + "'. targetFileToSymbolicLinkMap.containsKey(fullPath) = " + targetFileToSymbolicLinkMap.containsKey(fullPath));
//							if (_logger.isDebugEnabled())
//								System.out.println("WatchService: Found changes for file '" + fullPath + "'. targetFileToSymbolicLinkMap.containsKey(fullPath) = " + targetFileToSymbolicLinkMap.containsKey(fullPath));

							if (targetFileToSymbolicLinkMap.containsKey(fullPath))
							{
								Path symbolicLink = targetFileToSymbolicLinkMap.get(fullPath);
								System.out.println("WatchService: Found changes for file '" + fullPath + "' which HAS a symbolic link pointing towards it. The symbolic link is '" + symbolicLink + "', which will be used to propegate file change.");
								fullPath = symbolicLink;
							}

							String longFilename = fullPath.toString();

							// If the file is any of the files we want to "reload"
							if (fileNames.contains(longFilename))
							{
								onChange(fullPath);
							}

							// Changes to: User Defined Charts
							boolean isUserDefinedContent = longFilename.endsWith(UserDefinedChartManager.USER_DEFINED_FILE_POST);
							if (isUserDefinedContent)
							{
								UserDefinedChartManager udChartManager = UserDefinedChartManager.getInstance();
								udChartManager.onConfigChange(fullPath);
							}
						}

						boolean valid = watchKey.reset();
						if ( ! valid ) 
						{ 
							break; 
						}
					}
				} // end: isRunning
			} 
			catch (Throwable ex) 
			{
				System.out.println("Thread Combined Config files listener has problems. Caught: " + ex);
			}
			System.out.println("Thread Combined Config files listener was stopped.");
		} // end: run()

		public void onChange(Path filename) 
		{
			System.out.println("-------- onChange(): filename=" + filename + ", filename.toAbsolutePath()='" + filename.toAbsolutePath() + "'.");
		}

//		public void onChange(Path filename) 
//		{
//			// Get Configuration instance for this file
//			Configuration curConfig = _combinedConfig.getInstanceForFilename(filename.toString());
//			if (curConfig != null)
//			{
//				String curConfInstanceName = curConfig.getConfName();
//				
//				// skip instance: USER_TEMP
//				if (StringUtil.hasValue(curConfInstanceName) && curConfInstanceName.equals(USER_TEMP))
//					return;
//
//				// file NOT Exists
//				if ( ! filename.toFile().exists() )
//				{
//					System.out.println("Configuration file '" + filename + "' do not exists... will not continue checking for changes.");
//					return;
//				}
//				
//				// Figure out what property keys has been changed/added/deleted
//				// Then call the firePropertyChangeListener(Configuration source, String propertyName, String oldValue, String newValue)
//				Configuration newConfig = new Configuration(filename.toString());
//
//				// No changes has been made... (the file was saved with same values or touched) no need to continue
//				if (curConfig.equals(newConfig))
//				{
//					return;
//				}
//				
//				System.out.println("Found Configuration changes in file '" + filename + "'.");
//
//				// Loop all keys and see if they: are NEW or CHANGED 
//				for (Object keyObj : newConfig.keySet())
//				{
//					String newKey = keyObj.toString();
//					String newVal = newConfig.getProperty(newKey);
//
//					if (curConfig.hasProperty(newKey))
//					{
//						String oldVal = curConfig.getProperty(newKey);
//
//						if ( ! newVal.equals(oldVal) )
//						{
//							if ( keepFilter(curConfig, COMBINED_CONFIG_KEY_CHANGED, newKey, oldVal, newVal) )
//							{
//								// CHANGED CONFIG VALUE
//								curConfig.setProperty(newKey, newVal);
//								
//								if (_logger.isDebugEnabled())
//									System.out.println("Combened Config, CHANGED-VALUE. name='" + curConfInstanceName + "', file='" + filename + "', key='" + newKey + "', oldVal='" + oldVal + "', newVal='" + newVal + "'.");
//
//								fireCombinedConfigPropertyChangeListener(curConfig, COMBINED_CONFIG_KEY_CHANGED, newKey, oldVal, newVal);
//							}
//						}
//					}
//					else
//					{
//						if ( keepFilter(curConfig, COMBINED_CONFIG_KEY_ADDED, newKey, null, newVal) )
//						{
//							// NEW CONFIG VALUE
//							curConfig.setProperty(newKey, newVal);
//
//							if (_logger.isDebugEnabled())
//								System.out.println("Combened Config,     NEW-VALUE. name='" + curConfInstanceName + "', file='" + filename + "', key='" + newKey + "', newVal='" + newVal + "'.");
//
//							fireCombinedConfigPropertyChangeListener(curConfig, COMBINED_CONFIG_KEY_ADDED, newKey, null, newVal);
//						}
//					}
//				}
//
//				// Check if we have DELETED any entries in the NEW file.
//				Set<Object> removedKeySet = new HashSet<>(curConfig.keySet());
//				removedKeySet.removeAll(newConfig.keySet());
//				if ( ! removedKeySet.isEmpty() )
//				{
//					for (Object object : removedKeySet)
//					{
//						String delKey = (String) object;
//						String delVal = curConfig.getProperty(delKey);
//
//						if ( keepFilter(curConfig, COMBINED_CONFIG_KEY_REMOVED, delKey, delVal, null) )
//						{
//							// REMOVED CONFIG KEY
//							curConfig.remove(delKey);
//
////							if (_logger.isDebugEnabled())
//								System.out.println("Combened Config, REMOVED-VALUE. name='" + curConfInstanceName + "', file='" + filename + "', key='" + delKey + "', oldVal='" + delVal + "'.");
//
//							fireCombinedConfigPropertyChangeListener(curConfig, COMBINED_CONFIG_KEY_REMOVED, delKey, delVal, null);
//						}
//					}
//				}
//			}
//		} // end: onChange()
	}
	
	
	public static void main(String[] args)
	{
		try
		{
			FileWatcher fileWatcher = new FileWatcher();
			
			fileWatcher.start();
			fileWatcher.join();
			
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
