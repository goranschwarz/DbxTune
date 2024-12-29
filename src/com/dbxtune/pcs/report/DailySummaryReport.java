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
package com.dbxtune.pcs.report;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.dbxtune.NormalExitException;
import com.dbxtune.Version;
import com.dbxtune.pcs.MonRecordingInfo;
import com.dbxtune.pcs.PersistReader;
import com.dbxtune.pcs.report.content.DailySummaryReportContent;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.TimeUtils;

public class DailySummaryReport
{
//	public final static String APP_NAME = "DailySummaryReport"; // Daily Summary Report
	public final static String APP_NAME = "dsr";                // Daily Summary Report

	private static Logger _logger = Logger.getLogger(DailySummaryReport.class);


	private InputParams _params;

	private static class InputParams
	{
		public String _h2DbFile;
		public String _ofile;

		public String _username;
		public String _password;
		public String _url;

		public String _beginTimeStr;
		public String _endTimeStr;
		public int    _beginTimeHour;
		public int    _beginTimeMinute;
		public int    _endTimeHour;
		public int    _endTimeMinute;
	}
	
	public DailySummaryReport(CommandLine cmd)
	throws Exception
	{
		Version.setAppName(APP_NAME);

		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		if (cmd.hasOption('x'))
			log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// Check for Mandatory Command Line Switches
		// Also validate Command Line Switches
		_params = getInputParams(cmd);

		// Disable the CONSOLE output
		if ("-".equals(_params._ofile))
		{
			
		}

		// Make the report and print timing
		long startTime = System.currentTimeMillis();
		makeReport();
		_logger.info("Total execution time for this report was: " + TimeUtils.msDiffNowToTimeStr(startTime) + "  (HH:MM:SS.ms) ");
	}
	
	private void makeReport()
	throws Exception
	{
		DbxConnection conn = null;

		String dbxCollector = null;
		String reportSrvName  = null;

//		Configuration conf = Configuration.getCombinedConfiguration();

		try
		{
//			String  jdbcUrl  = conf.getProperty("url.jdbc", "jdbc:h2:tcp://192.168.0.110/GORAN_UB3_DS_2019-05-21");
			String  jdbcUrl  = _params._url;
			String  jdbcUser = _params._username;
			String  jdbcPass = _params._password;

			if (jdbcPass == null)
				jdbcPass = "";
				
			ConnectionProp cp = new ConnectionProp();
			cp.setUrl(jdbcUrl);
			cp.setUsername(jdbcUser);
			cp.setPassword(jdbcPass);
			
			conn = DbxConnection.connect(null, cp);
			
			// - Check if it's a PCS database
			// - get DbxTune TYPE (AseTune, RsTune, SqlServerTune, etc...)
			// - get Server Name
			// - get start/first sample Timestamp
			// - get   end/last  sample Timestamp
			if ( ! PersistReader.isOfflineDb(conn) )
				throw new NormalExitException("This do NOT look like a DbxTune recording... can't continue.");
			
			MonRecordingInfo monRecordingInfo = PersistReader.getMonRecordingInfo(conn, null);
//			MonVersionInfo   monVersionInfo   = PersistReader.getMonVersionInfo(conn, null);
			
			dbxCollector  = monRecordingInfo.getRecDbxAppName();
			reportSrvName = monRecordingInfo.getDbmsServerName();
		}
		catch (Exception ex) 
		{
			throw ex;
//			ex.printStackTrace();
//			System.exit(1);
		}
		

		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_reportClassname, "com.dbxtune.pcs.report.DailySummaryReport" + dbxCollector);
		System.getProperties().setProperty(DailySummaryReportFactory.PROPKEY_senderClassname, "com.dbxtune.pcs.report.senders.ReportSenderNoOp");
		
		IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport();
		if (report == null)
		{
			System.out.println("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
			return;
		}
		

		report.setConnection(conn);
		report.setServerName(reportSrvName);

		if (_params._beginTimeHour > 0 || _params._beginTimeMinute > 0) report.setReportPeriodBeginTime(_params._beginTimeHour, _params._beginTimeMinute);
		if (_params._endTimeHour   > 0 || _params._endTimeMinute   > 0) report.setReportPeriodEndTime  (_params._endTimeHour  , _params._endTimeMinute);

		try
		{
			// Initialize the Report, which also initialized the ReportSender
			report.init();

			// Create & and Send the report
			report.create();
//			report.send();

			// Save the report
//			report.save();

//			// remove/ old reports from the "archive"
//			report.removeOldReports();
			
			// Get Content and the HTML output
			DailySummaryReportContent content = report.getReportContent();
//			String htmlReport = content.getReportAsHtml();
			

			// Compose a filename (which we save the results to)
			String outFilename = _params._ofile;
			if ( StringUtil.isNullOrBlank(outFilename) )
			{
				throw new Exception("Output file must be specified.");
			}
			else if (outFilename.trim().equals("-"))
			{
//				System.out.println(htmlReport);
				content.toPrintStream(System.out);
			}
			else
			{
				String reportBeginDateStr = TimeUtils.getCurrentTimeForFileNameYmd(report.getReportBeginTime().getTime());
				String reportBeginTimeStr = TimeUtils.getCurrentTimeForFileNameHm (report.getReportBeginTime().getTime());
				String reportEndDateStr   = TimeUtils.getCurrentTimeForFileNameYmd(report.getReportEndTime()  .getTime());
				String reportEndTimeStr   = TimeUtils.getCurrentTimeForFileNameHm (report.getReportEndTime()  .getTime());

				// Translate variables in the filename to a "real" filename
				outFilename = outFilename.replace("${tmpDir}"   , System.getProperty("java.io.tmpdir"));
				outFilename = outFilename.replace("${srvName}"  , com.dbxtune.utils.FileUtils.toSafeFileName(reportSrvName));
				outFilename = outFilename.replace("${beginDate}", reportBeginDateStr);
				outFilename = outFilename.replace("${beginTime}", reportBeginTimeStr);
				outFilename = outFilename.replace("${endDate}"  , reportEndDateStr);
				outFilename = outFilename.replace("${endTime}"  , reportEndTimeStr);

				// Write the file
				try
				{
					File f = new File(outFilename);
					_logger.info("------------------------------------------------------------------------------------");
					_logger.info("Saving of DailyReport to file '" + f.getAbsolutePath() + "'.");
					_logger.info("------------------------------------------------------------------------------------");
					
//					FileUtils.write(f, htmlReport, StandardCharsets.UTF_8.name());
					content.saveReportAsFile(f);
				}
				catch (IOException ex)
				{
					_logger.error("Problems writing Daily Report to file '" + outFilename + "'. Caught: "+ex, ex);
				}
			}
			
			_logger.info("Closing DBMS Connection... to url='" + _params._url + "'. conn=" + conn);
			_logger.info("End of Create Dialy Summary Report for server '" + reportSrvName + "', report file was written to '" + outFilename + "'.");

			// Open it in browser
			if (outFilename.endsWith(".html"))
			{
				_logger.info("Open the file in a Web Browser.");
				if (Desktop.isDesktopSupported())
				{
					Desktop desktop = Desktop.getDesktop();
					if ( desktop.isSupported(Desktop.Action.BROWSE) )
					{
						File f = new File(outFilename);
						try
						{
							desktop.browse(f.toURI());
						}
						catch (Exception ex)
						{
							_logger.error("Problems Open the file in a Web Browser. URL '"+f.toURI()+"'.");
						}
					}
				}
			}
		}
		catch(Throwable ex)
		{
			_logger.error("Problems Sending Daily Summary Report. Caught: "+ex, ex);
		}
		
	}
	
	//---------------------------------------------------
	//---------------------------------------------------
	//---------------------------------------------------
	// MAIN
	//---------------------------------------------------
	//---------------------------------------------------
	//---------------------------------------------------
	/**
	 * Print command line options.
	 * @param options
	 */
	private static void printHelp(Options options, String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);

		if (errorStr != null)
		{
			pw.println();
			pw.println(errorStr);
			pw.println();
		}

		pw.println("usage: dsr [-f <h2dbfile>] [-o <filename>]");
		pw.println("           [-U <user>] [-P <passwd>] [-S <url>]");
		pw.println("           [-s <begin-time>] [-e end-time]");
		pw.println("           [-h] [-v] [-x]");
		pw.println("  ");
		pw.println("options:");
		pw.println("  -h,--help                 Usage information.");
		pw.println("  -v,--version              Display "+Version.getAppName()+" and JVM Version.");
		pw.println("  -x,--debug                Enable debug tracing to the log file/output.");
		pw.println("  ");
		pw.println("  -f,--h2dbfile   <filename>  H2 database file, same as: --url jdbc:h2:file:/path/<filename>;IFEXISTS=TRUE");
		pw.println("  -o,--ofile      <filename>  Output filename.");
		pw.println("                              DEFAULT=" + DailySummaryReportDialog.DEFAULT_OUTPUT_FILENAME_SHORT);
		pw.println("                              Use '-' for print to stdout");
		pw.println("  ");
		pw.println("  -U,--user       <user>      Username when connecting to URL.");
		pw.println("  -P,--passwd     <passwd>    Password when connecting to URL. null=noPasswd");
		pw.println("  -S,--url        <url>       Connect to URL");
		pw.println("  ");
		pw.println("  -s,--begin-time <HH[:mm]>   Time to start at. DEFAULT=first sample");
		pw.println("  -e,--end-time   <HH[:mm]>   Time to   end at. DEFAULT=last  sample");
		pw.println("  ");
		pw.flush();
	}

	/**
	 * Build the options com.dbxtune.parser. Has to be synchronized because of the way
	 * Options are constructed.
	 * 
	 * @return an options com.dbxtune.parser.
	 */
	private static synchronized Options buildCommandLineOptions()
	{
		Options options = new Options();

		// create the Options
		options.addOption( "h", "help",        false, "Usage information." );
		options.addOption( "v", "version",     false, "Display "+Version.getAppName()+" and JVM Version." );
		options.addOption( "x", "debug",       false, "Enable debug tracing to the log file/output." );

		options.addOption( "f", "h2dbfile",    true, "Input filename" );
		options.addOption( "o", "ofile",       true, "Output filename" );

		options.addOption( "U", "username",    true, "Username when connecting to URL." );
		options.addOption( "P", "password",    true, "Password when connecting to URL. (null=noPasswd)" );
		options.addOption( "S", "url",         true, "Connect to URL." );

		options.addOption( "b", "begin-time",  true, "Time to start at" );
		options.addOption( "e", "end-time",    true, "Time to end at" );

		return options;
	}
	
	private static InputParams getInputParams(CommandLine cmd)
	throws NormalExitException, FileNotFoundException
	{
		//----------------------------------------
		// Check for mandatory input parameters
		//----------------------------------------
		if ( !cmd.hasOption('U') )
		{
			printHelp(null, "Missing mandatory parameter: plase specify '-U|--username' or '-f|--h2dbfile'");
			throw new NormalExitException();
		}

		if ( !cmd.hasOption('S') && !cmd.hasOption('f') )
		{
			printHelp(null, "Missing mandatory parameter: plase specify '-S|--url' or '-f|--h2dbfile'");
			throw new NormalExitException();
		}


		//----------------------------------------
		// Transfer CmdLine Switches into InputParams object
		//----------------------------------------
		InputParams p = new InputParams();
		
		if (cmd.hasOption('U')) p._username = cmd.getOptionValue('U');
		if (cmd.hasOption('P')) p._password = cmd.getOptionValue('P');
		if (cmd.hasOption('S')) p._url      = cmd.getOptionValue('S');
		
		if (cmd.hasOption('b')) p._beginTimeStr = cmd.getOptionValue('b');
		if (cmd.hasOption('e')) p._endTimeStr   = cmd.getOptionValue('e');

		if (cmd.hasOption('f')) p._h2DbFile  = cmd.getOptionValue('f');
		if (cmd.hasOption('o')) p._ofile     = cmd.getOptionValue('o');

		//----------------------------------------
		// Validate/Check/Fix Content of the CmdLine Switches 
		//----------------------------------------
		
		if (StringUtil.hasValue(p._h2DbFile) && StringUtil.isNullOrBlank(p._url))
		{
			// Check that the file exists
			if ( ! new File(p._h2DbFile).exists() )
			{
				printHelp(null, "The specified H2 Database file '" + p._h2DbFile + "' do not exist");
				throw new NormalExitException();
//				throw new FileNotFoundException(p._h2DbFile);
			}
			String H2DB_FILE_TEMPLATE = "jdbc:h2:file:${filename};IFEXISTS=TRUE";
			String tmpUrl = H2DB_FILE_TEMPLATE.replace("${filename}", p._h2DbFile.replace(".mv.db", ""));
			p._url = tmpUrl;
		}

		// parse: --begin-time
		if (StringUtil.hasValue(p._beginTimeStr))
		{
			String val = p._beginTimeStr;
			
			String[] sa = val.split(":");
			String hourStr   = sa[0];
			String minuteStr = sa.length >= 2 ? sa[1] : "0";
			if (sa.length >= 3)
			{
				printHelp(null, "Parameter '-b|--begin-time' value='" + val + "' has more than one ':'");
				throw new NormalExitException();
			}
			try 
			{ 
				int hour   = Integer.parseInt(hourStr);
				int minute = Integer.parseInt(minuteStr);
				
				if (hour < 0 || hour > 23)
				{
					printHelp(null, "Parameter '-b|--begin-time' hour must be in range 0-23. hour='" + hour + "'");
					throw new NormalExitException();
				}
				if (minute < 0 || minute > 59)
				{
					printHelp(null, "Parameter '-b|--begin-time' minute must be in range 0-59. minute='" + minute + "'");
					throw new NormalExitException();
				}

				// ASSIGN hour/minute value
				p._beginTimeHour   = hour;
				p._beginTimeMinute = minute;
			}
			catch (NumberFormatException ex) 
			{
				printHelp(null, "Parameter '-b|--begin-time' value='" + val + "' hour:minute must be of number. hour='"+hourStr+"', minute='"+minuteStr+"'");
				throw new NormalExitException();
			}
		}

		// parse: --begin-time
		if (StringUtil.hasValue(p._endTimeStr))
		{
			String val = p._endTimeStr;
			
			String[] sa = val.split(":");
			String hourStr   = sa[0];
			String minuteStr = sa.length >= 2 ? sa[1] : "0";
			if (sa.length >= 3)
			{
				printHelp(null, "Parameter '-e|--end-time' value='" + val + "' has more than one ':'");
				throw new NormalExitException();
			}
			try 
			{ 
				int hour   = Integer.parseInt(hourStr);
				int minute = Integer.parseInt(minuteStr);
				
				if (hour < 0 || hour > 23)
				{
					printHelp(null, "Parameter '-e|--end-time' hour must be in range 0-23. hour='" + hour + "'");
					throw new NormalExitException();
				}
				if (minute < 0 || minute > 59)
				{
					printHelp(null, "Parameter '-e|--end-time' minute must be in range 0-59. minute='" + minute + "'");
					throw new NormalExitException();
				}

				// ASSIGN hour/minute value
				p._endTimeHour   = hour;
				p._endTimeMinute = minute;
			}
			catch (NumberFormatException ex) 
			{
				printHelp(null, "Parameter '-e|--end-time' value='" + val + "' hour:minute must be of number. hour='"+hourStr+"', minute='"+minuteStr+"'");
				throw new NormalExitException();
			}
		}

		// --ofile
		if ( ! StringUtil.hasValue(p._ofile) )
		{
			p._ofile = DailySummaryReportDialog.DEFAULT_OUTPUT_FILENAME_SHORT;
		}

		return p;
	}

//	public static parseHourMinute(String param, String val)
//	throws Exception
//	{
//		String[] sa = val.split(":");
//		String hourStr   = sa[0];
//		String minuteStr = sa.length >= 2 ? sa[1] : "0";
//		if (sa.length >= 3)
//		{
//			throw new Exception("Parameter '" +  param + "' value='" + val + "' has more than one ':'");
//		}
//		try 
//		{ 
//			int hour   = Integer.parseInt(hourStr);
//			int minute = Integer.parseInt(minuteStr);
//			
//			if (hour < 0 || hour > 23)
//			{
//				throw new Exception("Parameter '" +  param + "' hour must be in range 0-23. hour='" + hour + "'");
//			}
//			if (minute < 0 || minute > 59)
//			{
//				throw new Exception("Parameter '" +  param + "' minute must be in range 0-59. minute='" + minute + "'");
//			}
//
//			// ASSIGN hour/minute value
//			p._beginTimeHour   = hour;
//			p._beginTimeMinute = minute;
//			
//			LocalTime lt = LocalTime.parse(val);
//			lt.get
//			
//		}
//		catch (NumberFormatException ex) 
//		{
//			throw new Exception("Parameter '" +  param + "' value='" + val + "' hour:minute must be of number. hour='"+hourStr+"', minute='"+minuteStr+"'");
//		}
//	}

	//---------------------------------------------------
	// Command Line Parsing
	//---------------------------------------------------
	private static CommandLine parseCommandLine(String[] args, Options options)
	throws ParseException
	{
		// create the command line com.dbxtune.parser
		CommandLineParser parser = new DefaultParser();	
	
		// parse the command line arguments
		CommandLine cmd = parser.parse( options, args );

		// Validate any mandatory options or dependencies of switches
		

		if (_logger.isDebugEnabled())
		{
			for (Iterator<Option> it=cmd.iterator(); it.hasNext();)
			{
				Option opt = it.next();
				_logger.debug("parseCommandLine: swith='"+opt.getOpt()+"', value='"+opt.getValue()+"'.");
			}
		}

		return cmd;
	}


	/**
	 * Create a Daily Report <br>
	 * Input would be:
	 * <ul> 
	 *   <li>database file name, or a URL</li> 
	 *   <li>Start date/time (optional: if not passed, everything in the database will be included)</li> 
	 *   <li>end date/time (optional: if not passed, everything in the database will be included)</li>
	 * </ul>
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		Version.setAppName(APP_NAME);

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
				System.out.println(Version.getAppName()+" Version: " + Version.getVersionStr() + " JVM: " + System.getProperty("java.version"));
				System.out.println();
			}
			//-------------------------------
			// Check for correct number of cmd line parameters
			//-------------------------------
			else if ( cmd.getArgs() != null && cmd.getArgs().length > 0 )
			{
				String error = "Unknown options: " + StringUtil.toCommaStr(cmd.getArgs());
				printHelp(options, error);
			}
			//-------------------------------
			// Start tool
			//-------------------------------
			else
			{
				new DailySummaryReport(cmd);
				//new LogTailWindow(cmd);
			}
		}
		catch (ParseException pe)
		{
			String error = "Error: " + pe.getMessage();
			printHelp(options, error);
		}
		catch (NormalExitException e)
		{
			// This was probably throws when checking command line parameters
			// do normal exit
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
		}
	}
}
