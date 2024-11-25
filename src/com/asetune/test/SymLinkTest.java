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
package com.asetune.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import com.asetune.utils.StringUtil;

/**
 * Checking if Symbolic links works... especially on WINDOWS <br>
 * Because on Windows it seems that we need: Administrator Authority 
 */
public class SymLinkTest
{
	private static char sep = File.separatorChar;

	public static void main(String[] args)
	{
		String home = System.getProperty("user.home");
		List<String> logList = new LinkedList<>();

		String dbxHomeRes = home + sep + "resources" + sep + "dbxcentral" + sep + "scripts" + sep;
		String dbxHomeBin = home + sep + "bin"       + sep;

		File dbxcCreated = mkdir(home, "xxx_dbxc",                    System.out, logList, "- where user/localized DbxCentral files are located...");

		File dbxcBin     = mkdir(home, "xxx_dbxc" + sep + "bin",      System.out, logList, "- bin");
//		File dbxcBin2    = mkdir(home, "xxx_dbxc" + sep + "bin2",     System.out, logList, "- bin2");
		
		String dbxcBinStr  = dbxcBin    .toString() + File.separatorChar;
		String dbxcBin2Str = dbxcCreated.toString() + File.separatorChar + "bin2";

		File xxx_bat = new File(dbxcBinStr + "xxx.bat");
		try
		{
			xxx_bat.createNewFile();
			log(System.out, logList, "  * Creating file '" + xxx_bat + "'.");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		createSymbolicLink(dbxcBin2Str ,           dbxcBinStr,  System.out, logList, "- Soft link bin -> bin2");
		createSymbolicLink(dbxcBinStr + "zzz.bat", dbxcBinStr + "xxx.bat",  System.out, logList, "- yyy.bat -> zzz.bat");
		
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
}
