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
package com.asetune.test;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.asetune.AppDir;
import com.asetune.pcs.report.DailySummaryReportFactory;
import com.asetune.pcs.report.IDailySummaryReport;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.ssh.SshConnection;
import com.asetune.ssh.SshTunnelInfo;
import com.asetune.ssh.SshTunnelManager;
import com.asetune.utils.Configuration;

public class DailySummaryReportTest
{
	private static Logger _logger = Logger.getLogger(DailySummaryReportTest.class);

	
	
	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		//log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		log4jProps.setProperty("log4j.rootLogger", "DEBUG, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		String propFile = AppDir.getAppStoreDir(true) + "DailySummaryReportTest.props";
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
		

		String srvName = conf.getProperty("report.srvName", "DUMMY_SERVER");
		report.setConnection(conn);
		report.setServerName(srvName);
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
		}
		catch(Exception ex)
		{
			_logger.error("Problems Sending Daily Summary Report. Caught: "+ex, ex);
		}
	}
}
