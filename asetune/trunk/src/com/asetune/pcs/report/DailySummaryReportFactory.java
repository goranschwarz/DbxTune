package com.asetune.pcs.report;

import org.apache.log4j.Logger;

import com.asetune.Version;
import com.asetune.pcs.report.senders.IReportSender;
import com.asetune.pcs.report.senders.ReportSenderToMail;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;

public class DailySummaryReportFactory
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportFactory.class);

	public static final String  PROPKEY_create = "DailySummaryReport.create";
	public static final boolean DEFAULT_create = false;
	
	public static final String  PROPKEY_filter_keep_servername = "DailySummaryReport.filter.keep.serverName";
	public static final String  DEFAULT_filter_keep_servername = "";

	public static final String  PROPKEY_filter_skip_servername = "DailySummaryReport.filter.skip.serverName";
	public static final String  DEFAULT_filter_skip_servername = "";

	public static final String  PROPKEY_reportClassname = "DailySummaryReport.report.classname";
	public static final String  DEFAULT_reportClassname = "com.asetune.pcs.report.DailySummaryReport"+Version.getAppName();

	public static final String  PROPKEY_senderClassname = "DailySummaryReport.sender.classname";
	public static final String  DEFAULT_senderClassname = "com.asetune.pcs.report.senders.ReportSenderToMail";

	
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

		// servername: Keep & Skip rules
		isEnabled = (isEnabled && (StringUtil.isNullOrBlank(keep_servername_regExp) ||   serverName.matches(keep_servername_regExp ))); //     matches the KEEP serverName regexp
		isEnabled = (isEnabled && (StringUtil.isNullOrBlank(skip_servername_regExp) || ! serverName.matches(skip_servername_regExp ))); // NO match in the SKIP serverName regexp
		
		return isEnabled;
	}

	/**
	 * Creates a daily summary report using the class found in property 'DailySummaryReport.classname'
	 * @return
	 */
	public static IDailySummaryReport createDailySummaryReport()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		String reportClassname = conf.getProperty(PROPKEY_reportClassname, DEFAULT_reportClassname);
		String senderClassname = conf.getProperty(PROPKEY_senderClassname, DEFAULT_senderClassname);
		
		
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
			_logger.error("When trying to load DailySummaryReport class '"+reportClassname+"'. The DailySummaryReport do not seem to follow the interface 'com.asetune.pcs.report.IDailySummaryReport'");
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
		IReportSender reportSender = null;
		try
		{
			Class<?> c = Class.forName( senderClassname );
			reportSender = (IReportSender) c.newInstance();
		}
		catch (ClassCastException e)
		{
			_logger.error("When trying to load DailySummaryReport Sender class '"+senderClassname+"'. The DailySummaryReport do not seem to follow the interface 'com.asetune.pcs.report.IDailySummaryReport'");
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
		
		// Set sender in the report instance
		reportClass.setReportSender(reportSender);
		
		return reportClass;
	}
}