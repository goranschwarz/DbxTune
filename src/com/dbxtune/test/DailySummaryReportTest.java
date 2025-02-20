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
package com.dbxtune.test;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AppDir;
import com.dbxtune.pcs.report.DailySummaryReportFactory;
import com.dbxtune.pcs.report.IDailySummaryReport;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.ssh.SshTunnelInfo;
import com.dbxtune.ssh.SshTunnelManager;
import com.dbxtune.utils.Configuration;

public class DailySummaryReportTest
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	
	
	public static void main(String[] args)
	{
		String propFile = AppDir.getAppStoreDir(true) + "DailySummaryReportTest.props";

		try (InputStream input = new FileInputStream(propFile)) 
		{
			System.out.println("Loading LOG4J properties from file '" + propFile + "'.");

//			Properties log4jProps = new Properties();
//			log4jProps.load(input);
//			PropertyConfigurator.configure(log4jProps);
		}
		catch (IOException ex)
		{
			System.out.println("Problems Loading properties file '" + propFile + "'. Caught: " + ex);

			System.out.println("Using static configuration.");
			
			// Set Log4j Log Level
//			Configurator.setRootLevel(Level.TRACE);
		}

		System.out.println("PROPFILE: '"+propFile+"'.");
		Configuration conf = new Configuration(propFile);
		Configuration.setInstance(Configuration.USER_TEMP, conf);

		conf.setProperty(DailySummaryReportFactory.PROPKEY_create, true+"");

		// The below values are in: DailySummaryReportTest.props
//		conf.setProperty(ReportSenderToMail.PROPKEY_smtpHostname, "smtp.gmail.com");
//		conf.setProperty(ReportSenderToMail.PROPKEY_useSsl,       "true");
//		conf.setProperty(ReportSenderToMail.PROPKEY_smtpPort,     "465");
//		conf.setProperty(ReportSenderToMail.PROPKEY_from,         "dbxtune@xxx.org");
//		conf.setProperty(ReportSenderToMail.PROPKEY_username,     "xxx.yyyy@gmail.com");
//		conf.setProperty(ReportSenderToMail.PROPKEY_password,     "somepasswd");
//		conf.setProperty(ReportSenderToMail.PROPKEY_to,           "aaaa.bbbb@acme.com");

		String srvName = conf.getProperty("report.srvName", "DUMMY_SERVER");

		
		if ( ! DailySummaryReportFactory.isCreateReportEnabled() )
		{
			System.out.println("Daily Summary Report is NOT Enabled, this can be enabled using property '"+DailySummaryReportFactory.PROPKEY_create+"=true'.");
			return;
		}
		
		IDailySummaryReport report = DailySummaryReportFactory.createDailySummaryReport();
		if (report == null)
		{
			System.out.println("Daily Summary Report: create did not pass a valid report instance, skipping report creation.");
			return;
		}
		
		DbxConnection conn = null;
		try
		{
			String  urlJdbc         = conf.getProperty       ("url.jdbc",           "jdbc:h2:tcp://192.168.0.110/GORAN_UB3_DS_2019-05-21");
			String  urlSshTunnel    = conf.getProperty       ("url.ssh.tunnel",     "jdbc:h2:tcp://localhost/GORAN_UB3_DS_2019-05-21");
			boolean urlSshTunnelUse = conf.getBooleanProperty("url.ssh.tunnel.use", false);
			
			ConnectionProp cp = new ConnectionProp();
			cp.setUrl(urlJdbc);
			cp.setUsername("sa");
			cp.setPassword("");
			
			if (urlSshTunnelUse)
			{
				SshTunnelInfo sshTi = new SshTunnelInfo();
				sshTi.setSshHost    (conf.getProperty   ("SshTunnelInfo.hostname",      "dummy.com"));
				sshTi.setSshUsername(conf.getProperty   ("SshTunnelInfo.username",      "someuser"));
				sshTi.setSshPassword(conf.getProperty   ("SshTunnelInfo.password",      "somepasswd"));
				sshTi.setSshPort    (conf.getIntProperty("SshTunnelInfo.port",          22));
				sshTi.setDestHost   (conf.getProperty   ("SshTunnelInfo.dest.hostname", "192.168.0.110"));
				sshTi.setDestPort   (conf.getIntProperty("SshTunnelInfo.dest.port",     9092));
				sshTi.setLocalPort  (conf.getIntProperty("SshTunnelInfo.local.port",    9092));
				cp.setSshTunnelInfo(sshTi);

				SshConnection sshConn = new SshConnection(sshTi.getSshHost(), sshTi.getSshPort(), sshTi.getSshUsername(), sshTi.getSshPassword(), sshTi.getSshKeyFile());
				sshConn.connect();
				
				SshTunnelManager tm = SshTunnelManager.getInstance();
				tm.setupTunnel("someUniqueName", sshTi);

				cp.setUrl(urlSshTunnel);
			}
			
			conn = DbxConnection.connect(null, cp);
		}
		catch (Exception ex) 
		{
			ex.printStackTrace();
//			System.exit(1);
		}
		

		report.setConnection(conn);
		report.setServerName(srvName);
		
		if (conf.hasProperty("dbms.schema.name"))
			report.setDbmsSchemaName(conf.getProperty("dbms.schema.name"));

		// Set Reporting Period
		setReportingPeriod(report);
		
		try
		{
			// Initialize the Report, which also initialized the ReportSender
			report.init();

			// Create & and Send the report
			report.create();
			report.send();

			// Save the report
			report.save();

			// remove/ old reports from the "archive"
			report.removeOldReports();
			
			// Open in browser
			boolean openInBrowser = conf.getBooleanProperty("DailySummaryReport.openInBrowser", false);
			if (openInBrowser)
			{
				openInBrowser(report);
			}
		}
		catch(Throwable ex)
		{
			_logger.error("Problems Sending Daily Summary Report. Caught: "+ex, ex);
		}
	}
	
	private static void setReportingPeriod(IDailySummaryReport report)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean setReportingPeriod = conf.getBooleanProperty("dsr.reporting.period.set", false);
		if (setReportingPeriod)
		{
			int days            = conf.getIntProperty("dsr.reporting.period.days"        , 6);
			int beginTimeHour   = conf.getIntProperty("dsr.reporting.period.start.hour"  , 0);
			int beginTimeMinute = conf.getIntProperty("dsr.reporting.period.start.minute", 0);
			int endTimeHour     = conf.getIntProperty("dsr.reporting.period.end.hour"    , 23);
			int endTimeMinute   = conf.getIntProperty("dsr.reporting.period.end.minute"  , 59);

			LocalDateTime now = LocalDateTime.now();
			
			Timestamp beginTs = Timestamp.valueOf(now.withHour(beginTimeHour).withMinute(beginTimeMinute).withSecond(0).withNano(0).minusDays(days));
			Timestamp endTs   = Timestamp.valueOf(now.withHour(endTimeHour).withMinute(endTimeMinute).withSecond(59).withNano(999_999_999));

			report.setReportPeriodBeginTime(beginTs);
			report.setReportPeriodEndTime(endTs);
		}
	}

	private static void openInBrowser(IDailySummaryReport report)
	{
		File reportFile = report.getReportFile();
		if (reportFile == null)
			return;

		if (Desktop.isDesktopSupported())
		{
			Desktop desktop = Desktop.getDesktop();
			if ( desktop.isSupported(Desktop.Action.BROWSE) )
			{
				try
				{
//					desktop.browse(rr.getUrl().toURI());
					desktop.browse(reportFile.toURI());
				}
				catch (Exception ex)
				{
					_logger.error("Problems when open the URL '"+reportFile+"'. Caught: "+ex, ex);
				}
			}
		}
	}
}
