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
package com.dbxtune.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.dbxtune.NormalExitException;
import com.dbxtune.Version;
import com.dbxtune.utils.OpenSslAesUtil.DecryptionException;

public class DbxPasswordUpgrade
{
	
	public static final boolean DEFAULT_DO_BACKUP = true;
	public static final boolean DEFAULT_CONTINUE_ON_FAILURE  = true;

	private static String   _filename;
	private static String[] _passwords;
	private static boolean  _doBackup          = DEFAULT_DO_BACKUP;
	private static boolean  _continueOnFailure = DEFAULT_CONTINUE_ON_FAILURE;

	private void doWork() 
	throws IOException, DecryptionException
	{
		OpenSslAesUtil.upgradePasswdFile(_filename, _passwords, _doBackup, _continueOnFailure);
	}

	private void init(CommandLine cmd)
	throws Exception
	{
		// Read some parameters
		if (cmd.hasOption('F')) _filename    = cmd.getOptionValue('F');
		if (cmd.hasOption('P')) _passwords   = cmd.getOptionValues('P'); // Multiple passwords is possible

		if (cmd.hasOption('b')) _doBackup           = StringUtil.parseBoolean(cmd.getOptionValue('b'));
		if (cmd.hasOption('c')) _continueOnFailure  = StringUtil.parseBoolean(cmd.getOptionValue('c'));

		// Create a simple "Log4j Console Logger"
		LoggingConsole.init();
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

		pw.println("  ");
		pw.println("usage: " + Version.getAppName() + " [-h] [-v] [-x] ");
		pw.println("              [-P password1] [-P password2] [-P password3]");
		pw.println("              [-F passwdFile]");
		pw.println("              [-b true|false]  -c true|false");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -P,--password <passwd>      File encryption password. DEFAULT=null (useDefaultPassword)");
		pw.println("                              NOTE: You can specify this many times to test with multiple passwords");
		pw.println("  -F,--file <filename>        The file that holds the encrypted passwords: DEFAULT=${HOME}/.passwd.enc");
		pw.println("  -b,--backup <true|false>    Do make a backup file if change the file. DEFAULT=" + _doBackup);
		pw.println("  -c,--continue-on-failure <true|false>");
		pw.println("                              If we failed on one entry, continue anyway. DEFAULT=" + _continueOnFailure);
		pw.println("                              NOTE: That entry will simply be commented out.");
		pw.println("  ");
		pw.println("  -h,--help                   Usage information.");
		pw.println("  -v,--version                Display " + Version.getAppName() + " and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>      Debug options: a comma separated string");
		pw.println("                              To get available option, do -x list");
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
		options.addOption( Option.builder("h").longOpt("help"          ).hasArg(false).build() );
		options.addOption( Option.builder("v").longOpt("version"       ).hasArg(false).build() );
		options.addOption( Option.builder("x").longOpt("debug"         ).hasArg(true ).build() );

		options.addOption( Option.builder("F").longOpt("file"          ).hasArg(true ).build() );
		options.addOption( Option.builder("P").longOpt("password"      ).hasArg(true ).build() );

		options.addOption( Option.builder("b").longOpt("backup"             ).hasArg(true).build() );
		options.addOption( Option.builder("c").longOpt("continue-on-failure").hasArg(true).build() );

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

//		if (_logger.isDebugEnabled())
//		{
//			for (Iterator<Option> it=cmd.iterator(); it.hasNext();)
//			{
//				Option opt = it.next();
//				_logger.debug("parseCommandLine: swith='" + opt.getOpt() + "', value='" + opt.getValue() + "'.");
//			}
//		}
		if (cmd.hasOption('x'))
		{
			for (Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				System.out.println("parseCommandLine: swith='" + opt.getOpt() + "', value='" + opt.getValue() + "'.");
			}
		}

		return cmd;
	}

	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	public static void main(String[] args)
	{
		Version.setAppName("dbxPasswordUpgrade");
		
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
				DbxPasswordUpgrade dbxPassword = new DbxPasswordUpgrade();

				// Initialize some things
				dbxPassword.init(cmd);

				// DO THE WORK
				dbxPassword.doWork();
//				System.out.println(">>>>>>>>>>>>>>>> doWork(); is commented out <<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
//				System.out.println("_passwords=" + StringUtil.toCommaStrQuoted(_passwords));
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
}
