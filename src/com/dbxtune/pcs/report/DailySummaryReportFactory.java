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
package com.dbxtune.pcs.report;

import java.lang.invoke.MethodHandles;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.central.DbxTuneCentral;
import com.dbxtune.pcs.report.senders.IReportSender;
import com.dbxtune.pcs.report.senders.ReportSenderToMail;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

public class DailySummaryReportFactory
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	public static final String  PROPKEY_create = "DailySummaryReport.create";
	public static final boolean DEFAULT_create = false;
	
	public static final String  PROPKEY_filter_keep_servername = "DailySummaryReport.filter.keep.serverName";
	public static final String  DEFAULT_filter_keep_servername = "";

	public static final String  PROPKEY_filter_skip_servername = "DailySummaryReport.filter.skip.serverName";
	public static final String  DEFAULT_filter_skip_servername = "";

	public static final String  PROPKEY_reportClassname = "DailySummaryReport.report.classname";
	public static final String  DEFAULT_reportClassname = "com.dbxtune.pcs.report.DailySummaryReport"+Version.getAppName();

	public static final String  PROPKEY_senderClassname = "DailySummaryReport.sender.classname";
	public static final String  DEFAULT_senderClassname = "com.dbxtune.pcs.report.senders.ReportSenderToMail";

	
	public static final String  PROPKEY_save = "DailySummaryReport.save";
	public static final boolean DEFAULT_save = true;
	
	public static final String  PROPKEY_saveDir = "DailySummaryReport.save.dir";
	public static final String  DEFAULT_saveDir = DbxTuneCentral.getAppReportsDir();
	
	public static final String  PROPKEY_removeReportsAfterDays = "DailySummaryReport.remove.after.days";
	public static final int     DEFAULT_removeReportsAfterDays = 60;


	public static final String  PROPKEY_maxTableCellSizeKb = "DailySummaryReport.maxTableCellSizeKb";
	public static final int     DEFAULT_maxTableCellSizeKb = 128;


	public static final String  PROPKEY_reportHtml_headFile = "DailySummaryReport.report.html.headFile";
	public static final String  DEFAULT_reportHtml_headFile = null;

	public static final String  PROPKEY_isReportEntryEnabled = "DailySummaryReport.report.entry.enabled.<ENTRY-NAME>"; // at runtime in each isEnabled() replace '<ENTRY-NAME>' with your class name
	public static final boolean DEFAULT_isReportEntryEnabled = true;
	

	
	/**
	 * Check if the daily summary report is enabled or disabled
	 * 
	 * @return true if it's ENABLED, false if it's NOT Enabled
	 */
	public static boolean isCreateReportEnabled()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		return conf.getBooleanProperty(PROPKEY_create, DEFAULT_create);
	}
	
	/**
	 * Check if the daily summary report is enabled or disabled for this specific server name<br>
	 * Uses regular expresion string in the following properties
	 * <ul>
	 *   <li>DailySummaryReport.filter.keep.serverName</li>
	 *   <li>DailySummaryReport.filter.skip.serverName</li>
	 * </ul>
	 * 
	 * @param serverName
	 * 
	 * @return true if it's ENABLED, false if it's NOT Enabled
	 */
	public static boolean isCreateReportEnabledForServer(String serverName)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		
		String keep_servername_regExp = conf.getProperty(PROPKEY_filter_keep_servername, DEFAULT_filter_keep_servername);
		String skip_servername_regExp = conf.getProperty(PROPKEY_filter_skip_servername, DEFAULT_filter_skip_servername);

		// Below logic is "reused" from: AlarmWriterAbstract.doAlarm(AlarmEvent)
		// The below could have been done with neasted if(keep-db), if(keep-srv), if(!skipDb), if(!skipSrv) doAlarm=true; 
		// Below is more readable, from a variable context point-of-view, but HARDER to understand
		boolean isEnabled = true; // note: this must be set to true at start, otherwise all below rules will be disabled (it "stops" processing at first isEnabled==false)

		// serverName: Keep & Skip rules
		isEnabled = (isEnabled && (StringUtil.isNullOrBlank(keep_servername_regExp) ||   serverName.matches(keep_servername_regExp ))); //     matches the KEEP serverName regexp
		isEnabled = (isEnabled && (StringUtil.isNullOrBlank(skip_servername_regExp) || ! serverName.matches(skip_servername_regExp ))); // NO match in the SKIP serverName regexp
		
		
		// Check the Senders/Writers and see if they have rules that still allow this server
		// Potentially in the future: we might have several "senders/writers", then we need to loop all senders/writers and check...
//hmm... If we still want to Save the ReportFile but NOT sending it via the writer...
//Then we should NOT have this check here...
//		if (isEnabled)
//		{
//			IReportSender reportSender = loadReportSender();
//			if ( ! reportSender.isEnabledForServer(serverName) )
//			{
//				_logger.info("The DailyReportSender's writer/sender named '" + reportSender.getName() + "' is NOT enabled to send reports for serverName '" + serverName + "'. Report will NOT be created for this server...");
//				isEnabled = false;
//			}
//		}
		
		return isEnabled;
	}

	/**
	 * Creates a daily summary report using the class found in property 'DailySummaryReport.classname'
	 * @return
	 */
//	public static IDailySummaryReport createDailySummaryReport(String serverName)
	public static IDailySummaryReport createDailySummaryReport()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String reportClassname = conf.getProperty(PROPKEY_reportClassname, DEFAULT_reportClassname);
		String senderClassname = conf.getProperty(PROPKEY_senderClassname, DEFAULT_senderClassname);
		
		if (reportClassname.startsWith("com.asetune."))
		{
			String oldName = reportClassname;
			String newName = reportClassname.replace("com.asetune.", "com.dbxtune.");;
			reportClassname = newName;
			_logger.warn("You passed an '" + PROPKEY_reportClassname + "' that starts with 'com.asetune...' [" + oldName + "], instead lets use the prefix 'com.dbxtune...' [" + newName + "].");
		}
		
		// Load the REPORT class name
		_logger.info("Creating a Daily Summary Report Using implementation '"+reportClassname+"', with Report Sender '"+senderClassname+"'.");
		IDailySummaryReport reportClass = null;
		try
		{
			Class<?> c = Class.forName( reportClassname );
			reportClass = (IDailySummaryReport) c.newInstance();
		}
		catch (ClassCastException e)
		{
			_logger.error("When trying to load DailySummaryReport class '"+reportClassname+"'. The DailySummaryReport do not seem to follow the interface 'com.dbxtune.pcs.report.IDailySummaryReport'");
		}
		catch (Exception e)
		{
			_logger.error("Tried to load DailySummaryReport class '"+reportClassname+"' failed. Caught: "+e, e);
		}
		
		if (reportClass == null)
		{
			reportClass = new DailySummaryReportDefault();
			_logger.error("DailySummaryReport will be using the Default implementation '"+reportClass.getClass().getName()+"'.");
		}

		
		// Load the SENDER class name
		IReportSender reportSender = loadReportSender();
		
		// Set sender in the report instance
		reportClass.setReportSender(reportSender);
		
		return reportClass;
	}


	private static IReportSender loadReportSender()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String senderClassname = conf.getProperty(PROPKEY_senderClassname, DEFAULT_senderClassname);
		
		if (StringUtil.isNullOrBlank(senderClassname))
			throw new RuntimeException("loadReportSender(): senderClassname='" + senderClassname + "' is null or empty. This is not expected.");

		if (senderClassname.startsWith("com.asetune."))
		{
			String oldName = senderClassname;
			String newName = senderClassname.replace("com.asetune.", "com.dbxtune.");;
			senderClassname = newName;
			_logger.warn("You passed an '" + PROPKEY_senderClassname + "' that starts with 'com.asetune...' [" + oldName + "], instead lets use the prefix 'com.dbxtune...' [" + newName + "].");
		}
		
		
		// Load the SENDER class name
		IReportSender reportSender = null;
		try
		{
			Class<?> c = Class.forName( senderClassname );
			reportSender = (IReportSender) c.newInstance();
		}
		catch (ClassCastException e)
		{
			_logger.error("When trying to load DailySummaryReport Sender class '"+senderClassname+"'. The DailySummaryReport do not seem to follow the interface 'com.dbxtune.pcs.report.IDailySummaryReport'");
		}
		catch (Exception e)
		{
			_logger.error("Tried to load DailySummaryReport Sender class '"+senderClassname+"' failed. Caught: "+e, e);
		}
		
		if (reportSender == null)
		{
			reportSender = new ReportSenderToMail();
			_logger.error("DailySummaryReport Sender will be using the Default implementation '"+reportSender.getClass().getName()+"'.");
		}
		
		return reportSender;
	}
}
