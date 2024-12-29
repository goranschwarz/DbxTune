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

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;

public class FileWatchService
{
	public static void main(String[] args)
	{
		try
		{
			System.out.println("args.length=" + args.length);
			for (int i = 0; i < args.length; i++)
				System.out.println("args[" + i + "]=|" + args[i] + "|");

			String dirName = args.length > 0 ? args[1] : System.getProperty("user.home");
			Path dirPath = Paths.get(dirName);

			System.out.println("Listing all files in directory: " + dirPath);

			// If a target file for a symbolic link is changed, we want to get "notified" that the symbolic link-file was changed
			// SO: remember the target file (which gets the change-notification), so we can simulate that the "link" was changed even if it was the "target"
			HashMap<Path, Path> targetFileToSymbolicLinkMap = new HashMap<>();
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) 
			{
				for (Path path : stream) 
				{
					if ( ! Files.isDirectory(path) ) 
					{
						System.out.println(" --- Start: dir contains file: " + path.toAbsolutePath());
						if (Files.isSymbolicLink(path))
						{
							System.out.println(" --- Start: dir contains file: " + path.toAbsolutePath());
							System.out.println("     >>>>>> Which seems to be a symbolic link to path.toAbsolutePath(): " + path.toAbsolutePath());
							System.out.println("     >>>>>> Which seems to be a symbolic link to path.toRealPath()    : " + path.toRealPath());
							targetFileToSymbolicLinkMap.put(path.toRealPath(), path);
						}
					}
				}
			}
			
			System.out.println("Watching directory '" + dirPath + "' for changes: CREATE, DELETE, MODIFY.");

			WatchService watchService = FileSystems.getDefault().newWatchService();

			dirPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

			WatchKey key;
			while ((key = watchService.take()) != null)
			{
				for (WatchEvent<?> event : key.pollEvents())
				{
					System.out.println("Event kind:" + event.kind() + ". File affected: " + event.context() + ".");

					Object ctx = event.context();
					System.out.println("---- ctx=" + ctx.getClass().getName());
					
					Path fn = (Path) event.context();
//					Path dir = (Path)watchKey.watchable();
//					Path fullPath = dir.resolve(filename);
					System.out.println("---- fn='" + fn + "', fn.toAbsolutePath()='" + fn.toAbsolutePath() + "', Files.isSymbolicLink(fn)=" + Files.isSymbolicLink(fn));

					Path realName = targetFileToSymbolicLinkMap.get(fn);
					if (realName != null)
					{
						System.out.println("---- The Changed file points to a SymLink '" + realName + "', so we should notify for that as well.");
					}
				}
				key.reset();
			}
			
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

}
