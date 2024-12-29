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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.dbxtune.utils.StringUtil;

public class ProcessBuilderTester
{

	public static void main(String[] args)
	{
		try
		{
			execCommandOutputAsStr(args);
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}	
	
	public static String execCommandOutputAsStr(String[] cmd) throws Exception
	{
		// Create ProcessBuilder
		ProcessBuilder pb = new ProcessBuilder(cmd);
//		pb.inheritIO();
		pb.redirectErrorStream(true);

		System.out.println("execCommandOutputAsStr(): pb.command()=[" + StringUtil.toCommaStrQuoted("|", pb.command()) + "]");
		
		// Change environment, this could be usable if the 'dbxtune.sql.pretty.print.cmd' is a shell script or a bat file
//		Map<String, String> env = pb.environment();
		
		// Start
		Process process = pb.start();

		// Get output, into the output variable
		InputStream stdout = process.getInputStream();
		String output = IOUtils.toString(stdout, "UTF-8");
		if (StringUtil.hasValue(output))
			output = output.trim();

System.out.println("execCommandOutputAsStr(cmd=|"+cmd+"|): <<<<<<<< returned=|" + output + "|");
		return output;
	}
	
}
