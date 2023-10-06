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
package com.asetune.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

import com.asetune.NormalExitException;
import com.asetune.Version;
import com.asetune.utils.OpenSslAesUtil.DecryptionException;

public class DbxPassword
{
//	private static Logger  _logger = Logger.getLogger(DbxPassword.class);
	
//	public static final boolean DEFAULT_DO_BACKUP = true;

	private CommandType _command;
	private String   _filename;
	private String   _username;
	private String   _password;
	private String   _serverName;
//	private String[] _keyPassPhrase;
	private String   _keyPassPhrase;
	private int      _encVersion;
//	private boolean  _doBackup          = DEFAULT_DO_BACKUP;
	private String   _outputFile;

	public enum CommandType
	{
		GET,
		SET,
		REMOVE,
		LIST
	};
	
	private void doWork() 
	throws IOException, DecryptionException
	{
		boolean listFileContenet = false;

//		OpenSslAesUtil.upgradePasswdFile(_filename, _passswords, _doBackup, _continueOnFailure);
		if (CommandType.GET.equals(_command))
		{
			String cleartextPasswd = OpenSslAesUtil.readPasswdFromFile(_username, _serverName, _filename, _keyPassPhrase);

			if (StringUtil.isNullOrBlank(_outputFile))
			{
				String serverNameTmp = _serverName == null ? "" : _serverName;
				System.out.println();
				System.out.println("==============================================================================");
				System.out.println("-- Cleartext Password for user '" + _username + "' and server '" + serverNameTmp + "'.");
				System.out.println("------------------------------------------------------------------------------");
				String outputStr = "DBX_PASSWORD: " + cleartextPasswd;
				System.out.println(outputStr);
				System.out.println("------------------------------------------------------------------------------");
				System.out.println();
			}
			else
			{
				String outputStr = "export DBX_PASSWORD=" + cleartextPasswd + "\n";
				if (PlatformUtils.isWindows())
				{
					outputStr = "set DBX_PASSWORD=" + cleartextPasswd + "\r\n";
				}
				
				// Write to file
				File of = new File(_outputFile);
				FileUtils.writeStringToFile(of, outputStr, Charset.defaultCharset());

				String serverNameTmp = _serverName == null ? "" : _serverName;
				System.out.println();
				System.out.println("==============================================================================");
				System.out.println("-- Cleartext Password for user '" + _username + "' and server '" + serverNameTmp + "'.");
				System.out.println("-- was written to file '" + of.getAbsolutePath() + "'.");
				System.out.println("------------------------------------------------------------------------------");
				System.out.println();
			}
		}
		else if (CommandType.SET.equals(_command))
		{
			OpenSslAesUtil.writePasswdToFile(_password, _username, _serverName, _filename, _keyPassPhrase);
			listFileContenet = true;

//			String outputStr = "DBX_PASSWORD_ENCRYPTED: " + enctextPasswd;
//			System.out.println(outputStr);
		}
		else if (CommandType.REMOVE.equals(_command))
		{
			OpenSslAesUtil.removePasswdFromFile(_username, _serverName, _filename);
			listFileContenet = true;
		}
		else if (CommandType.LIST.equals(_command))
		{
			listFileContenet = true;
		}

		if (listFileContenet)
		{
			System.out.println();
			System.out.println("==============================================================================");
			System.out.println("-- File: " + _filename);
			System.out.println("------------------------------------------------------------------------------");
			String fileContent = OpenSslAesUtil.readPasswdFile(_filename);
			System.out.println(fileContent);
			System.out.println("------------------------------------------------------------------------------");
			System.out.println();
		}
	}

	private void init(CommandLine cmd)
	throws Exception
	{
		if ( cmd.getArgs() == null || (cmd.getArgs() != null && cmd.getArgs().length == 0) )
		{
			String error = "Please specify a command: 'get|set|list'";
			printHelp(null, error);
		}
		if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
		{
			for (String command : cmd.getArgs())
			{
				if      ("get"   .equalsIgnoreCase(command)) _command = CommandType.GET;
				else if ("set"   .equalsIgnoreCase(command)) _command = CommandType.SET;
				else if ("remove".equalsIgnoreCase(command)) _command = CommandType.REMOVE;
				else if ("list"  .equalsIgnoreCase(command)) _command = CommandType.LIST;
				else
				{
					String error = "Unknown option: '" + command + "'. The known commands are: 'get|set|list'";
					printHelp(null, error);
				}
			}
		}
		
		// Read some parameters
		if (cmd.hasOption('F')) _filename      = cmd.getOptionValue('F');
		if (cmd.hasOption('U')) _username      = cmd.getOptionValue('U');
		if (cmd.hasOption('P')) _password      = cmd.getOptionValue('P');
		if (cmd.hasOption('S')) _serverName    = cmd.getOptionValue('S');
		if (cmd.hasOption('V')) _encVersion    = StringUtil.parseInt(cmd.getOptionValue('V'), OpenSslAesUtil.DEFAULT_VERSION);
//		if (cmd.hasOption('k')) _keyPassPhrase = cmd.getOptionValues('k');
		if (cmd.hasOption('k')) _keyPassPhrase = cmd.getOptionValue('k');
		if (cmd.hasOption('o')) _outputFile    = cmd.getOptionValue('o');

//		if (cmd.hasOption('b')) _doBackup           = StringUtil.parseBoolean(cmd.getOptionValue('b'));
//		if (cmd.hasOption('c')) _continueOnFailure  = StringUtil.parseBoolean(cmd.getOptionValue('c'));

		if (StringUtil.isNullOrBlank(_filename))
		{
			_filename = OpenSslAesUtil.getPasswordFilename();
		}

		// Check flags depending on the command type
		if (CommandType.GET.equals(_command))
		{
			if (StringUtil.isNullOrBlank(_username))
			{
				String error = "ERROR: '-U username'  MUST be specified for 'get' operation";
				printHelp(null, error);
				throw new NormalExitException();
			}
		}
		else if (CommandType.SET.equals(_command))
		{
			if (StringUtil.isNullOrBlank(_username))
			{
				String error = "ERROR: '-U username'  MUST be specified for 'set' operation";
				printHelp(null, error);
				throw new NormalExitException();
			}

			if (StringUtil.isNullOrBlank(_password))
			{
				String error = "ERROR: '-P password'  MUST be specified for 'set' operation";
				printHelp(null, error);
				throw new NormalExitException();
			}
		}
		else if (CommandType.REMOVE.equals(_command))
		{
			if (StringUtil.isNullOrBlank(_username))
			{
				String error = "ERROR: '-U username'  MUST be specified for 'remove' operation";
				printHelp(null, error);
				throw new NormalExitException();
			}
		}
		else if (CommandType.LIST.equals(_command))
		{
		}
		
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
		pw.println("              [-U username]");
		pw.println("              [-P password]");
		pw.println("              [-S servername]");
		pw.println("              [-k key-pass-phrase]");
		pw.println("              [-F passwdFile]");
		pw.println("              [-o output-file]");
		pw.println("              [-V enc-version]");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -U,--username <username>      Username to encrypt/decrypt password for");
		pw.println("  -P,--password <passwd>        Password to encrypt");
		pw.println("  -S,--servername <srvname>     Servername to encrypt/decrypt password for");
		pw.println("  -k,--key-pass-phrase <passwd> Password phrase when encrypt/decrypt. DEFAULT=" + OpenSslAesUtil.getDefaultPassPhrase());
		pw.println("  -F,--file <filename>          The file that holds the encrypted passwords: DEFAULT=" + OpenSslAesUtil.getPasswordFilename());
		pw.println("  -o,--output-file <fullpath>   if 'get' write the decryptet password to a file. DEFAULT=''");
		pw.println("                                this can be used to 'source' the file and include it in local env.");
		pw.println("  -V,--enc-version <1|2>        Version of encryption to use. DEFAULT=" + OpenSslAesUtil.DEFAULT_VERSION);
		pw.println("  ");
		pw.println("  -h,--help                     Usage information.");
		pw.println("  -v,--version                  Display " + Version.getAppName() + " and JVM Version.");
		pw.println("  -x,--debug <dbg1,dbg2>        Debug options: a comma separated string");
		pw.println("                                To get available option, do -x list");
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
		options.addOption( Option.builder("h").longOpt("help"           ).hasArg(false).build() );
		options.addOption( Option.builder("v").longOpt("version"        ).hasArg(false).build() );
		options.addOption( Option.builder("x").longOpt("debug"          ).hasArg(true ).build() );

		options.addOption( Option.builder("U").longOpt("username"       ).hasArg(true ).build() );
		options.addOption( Option.builder("P").longOpt("password"       ).hasArg(true ).build() );
		options.addOption( Option.builder("S").longOpt("servername"     ).hasArg(true ).build() );

		options.addOption( Option.builder("k").longOpt("key-pass-phrase").hasArg(true ).build() );
		options.addOption( Option.builder("F").longOpt("file"           ).hasArg(true ).build() );
		options.addOption( Option.builder("o").longOpt("output-file"    ).hasArg(true ).build() );
		options.addOption( Option.builder("V").longOpt("enc-version"    ).hasArg(true ).build() );
		
//		options.addOption( Option.builder("b").longOpt("backup"             ).hasArg(true).build() );
//		options.addOption( Option.builder("c").longOpt("continue-on-failure").hasArg(true).build() );

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
		Version.setAppName("dbxPassword");
		
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
//			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
//			{
////				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
//				String error = "Unknown options: " + Arrays.toString(cmd.getArgs());
//				printHelp(options, error);
//			}
			//-------------------------------
			// Start 
			//-------------------------------
			else
			{
				DbxPassword dbxPassword = new DbxPassword();

				// Initialize some things
				dbxPassword.init(cmd);

				// DO THE WORK
				dbxPassword.doWork();

//				// Use: auto-close
//				try( H2CentralDbCopy3 dbCopy = new H2CentralDbCopy3() )
//				{
//					// Initialize some things
//					dbCopy.init(cmd);
//
//					// Connect to SOURCE/Target
//					dbCopy.connect();
//
//					// check some stuff before we: go-to-work
//					dbCopy.preCheck();
//					if (dbCopy._sourceDbxCentralDbVersion <= 0)
//					{
//						throw new Exception("The SOURCE database does not look like a DbxCentral database.");
//					}
//					if (dbCopy._targetDbxCentralDbVersion >= 0)
//					{
//						if (dbCopy._sourceDbxCentralDbVersion != dbCopy._targetDbxCentralDbVersion)
//						{
//							throw new Exception("The SOURCE and TARGET DbxCentral database versions are not the same, this is not supported... sourceDbVersion=" + dbCopy._sourceDbxCentralDbVersion + ", targetDbVersion=" + dbCopy._targetDbxCentralDbVersion);
//						}
//					}
//
//					// DO THE WORK
//					dbCopy.doWork();
//				}
//
//				_logger.info("End of processing in '" + H2CentralDbCopy3.class.getSimpleName() + "'.");
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
