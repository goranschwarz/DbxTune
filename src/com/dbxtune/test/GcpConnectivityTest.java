/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import com.dbxtune.cloud.CloudConfig;
import com.dbxtune.cloud.GcpCredentialsHelper;
import com.dbxtune.hostmon.HostMonitorConnection;
import com.dbxtune.hostmon.HostMonitorConnectionCloud;
import com.dbxtune.hostmon.HostMonitorConnectionSsh;
import com.dbxtune.ssh.SshConnection;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;

/**
 * Command line test of the two connections a collector makes: JDBC to the DBMS, and the Host Monitor connection
 * (GCP Cloud SQL / SSH). It uses the same property names as a collector <code>.conf</code> file, and the same helpers
 * as the collector (GCP IAM token as the JDBC password, CloudConfig resolving), so a pass here means the collector can connect.
 * <p>
 * Run it with: <code>dbxtune.sh gcpconntest /path/to/file.props</code> (no file: prints an example properties file)
 */
public class GcpConnectivityTest
{
	private static final String EXAMPLE_PROPS = ""
		+ "#--------------------------------------------------------------------------\n"
		+ "# GcpConnectivityTest - example properties file\n"
		+ "# Same property names as a collector .conf file (so you can also pass the collector's own .conf)\n"
		+ "#--------------------------------------------------------------------------\n"
		+ "\n"
		+ "#--- JDBC: full URL\n"
		+ "conn.url = jdbc:postgresql://10.20.30.40:5432/postgres?sslmode=require\n"
		+ "#--- or: host:port (then the URL is jdbc:postgresql://<hostPort>/postgres)\n"
		+ "#conn.dbmsHostPort = 10.20.30.40:5432\n"
		+ "\n"
		+ "#--- GCP Cloud SQL IAM login: the Service Account as user, and NO password\n"
		+ "#    (an OAuth2 token is fetched: from the VM's attached Service Account, or GOOGLE_APPLICATION_CREDENTIALS)\n"
		+ "conn.dbmsUsername = dbxtune-monitor@my-prod-project.iam\n"
		+ "#    or use a key file:  conn.dbmsPassword = gcp-iam:/etc/dbxtune/gcp-dbxtune-sa.json\n"
		+ "#    or a SQL login:     conn.dbmsUsername = dbxtune  +  conn.dbmsPassword = secret\n"
		+ "#conn.dbmsPassword = \n"
		+ "\n"
		+ "#--- Extra SQL to run (optional), example: does the user have the pg_monitor role\n"
		+ "#test.sql = select current_user, pg_has_role('pg_monitor', 'member') as has_pg_monitor, version()\n"
		+ "\n"
		+ "#--- Host Monitor: GCP Cloud Monitoring + Cloud Logging\n"
		+ "conn.sshUsername = gcp\n"
		+ "#    The Cloud SQL instance: <instance>  or  <project>:<instance>  or  <project>:<region>:<instance>\n"
		+ "conn.sshHostname = pg-orders-01\n"
		+ "#    Key file (only when NOT on a GCP VM with an attached Service Account)\n"
		+ "#conn.sshKeyFile = /etc/dbxtune/gcp-dbxtune-sa.json\n"
		+ "#    Or explicit (optional if resolved from the above)\n"
		+ "#conn.hostmon.cloud.project         = my-prod-project\n"
		+ "#conn.hostmon.cloud.instance        = pg-orders-01\n"
		+ "#conn.hostmon.cloud.credentialsFile = /etc/dbxtune/gcp-dbxtune-sa.json\n"
		+ "\n"
		+ "#--- Host Monitor: SSH (instead of GCP)\n"
		+ "#conn.sshUsername = dbxtune\n"
		+ "#conn.sshHostname = dbhost.acme.com\n"
		+ "#conn.sshPort     = 22\n"
		+ "#conn.sshPassword = secret\n"
		+ "#conn.sshKeyFile  = /home/dbxtune/.ssh/id_rsa\n"
		;

	public static void main(String[] args)
	{
		if (args.length == 0 || ! new File(args[0]).exists())
		{
			if (args.length > 0)
				System.out.println("ERROR: The file '" + args[0] + "' does not exist.\n");
			System.out.println("Usage: dbxtune.sh gcpconntest <file.props>");
			System.out.println("       Tests the JDBC connection and the Host Monitor connection (GCP Cloud SQL or SSH).");
			System.out.println("");
			System.out.println("Example properties file:");
			System.out.println("");
			System.out.println(EXAMPLE_PROPS);
			System.exit(1);
		}

		// Show INFO messages from the GCP helpers (which token/credentials file was used, what was resolved)
		Configurator.setRootLevel(Level.INFO);

		// The GCP helpers read some properties from the Combined Configuration (conn.dbms.cloud.iamAuth, ...)
		Configuration conf = new Configuration(args[0]);
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf);
		Configuration.setSearchOrder(Configuration.SYSTEM_CONF);

		boolean jdbcOk    = testJdbc(conf);
		Boolean hostMonOk = testHostMon(conf);

		System.out.println("");
		System.out.println("==========================================================================");
		System.out.println(" JDBC:         " + (jdbcOk ? "OK" : "FAILED"));
		System.out.println(" Host Monitor: " + (hostMonOk == null ? "not configured" : hostMonOk ? "OK" : "FAILED"));
		System.out.println("==========================================================================");

		System.exit(jdbcOk && (hostMonOk == null || hostMonOk) ? 0 : 1);
	}

	private static boolean testJdbc(Configuration conf)
	{
		System.out.println("");
		System.out.println("==========================================================================");
		System.out.println(" JDBC Connection");
		System.out.println("==========================================================================");

		String url      = conf.getProperty("conn.url", "");
		String hostPort = conf.getProperty("conn.dbmsHostPort", "");
		String user     = conf.getProperty("conn.dbmsUsername", "");
		String passwd   = conf.getProperty("conn.dbmsPassword", ""); // empty + IAM user = GCP IAM login (as the collector)
		String testSql  = conf.getProperty("test.sql", "");

		if (StringUtil.isNullOrBlank(url) && StringUtil.hasValue(hostPort))
			url = "jdbc:postgresql://" + hostPort + "/postgres";

		if (StringUtil.isNullOrBlank(url))
		{
			System.out.println("ERROR: No 'conn.url' or 'conn.dbmsHostPort' in the properties file.");
			return false;
		}

		System.out.println("URL:  " + url);
		System.out.println("User: " + user);

		Properties props = new Properties();
		props.put("user",     user);
		props.put("password", passwd);

		try
		{
			// The same as DbxConnection.connect(): an IAM user (or password 'gcp-iam[:keyFile]') gets an OAuth2 token as the password
			boolean iam = GcpCredentialsHelper.applyGcpIamPostgresAuth(url, props, null, true);
			System.out.println("Auth: " + (iam ? "GCP IAM (OAuth2 access token), as user '" + props.getProperty("user") + "'" : "password"));

			long startTime = System.currentTimeMillis();
			try (Connection conn = DriverManager.getConnection(url, props))
			{
				System.out.println("Connected in " + (System.currentTimeMillis() - startTime) + " ms.");

				DatabaseMetaData dbmd = conn.getMetaData();
				System.out.println("JDBC Driver:  " + dbmd.getDriverName() + " " + dbmd.getDriverVersion());
				System.out.println("DBMS Product: " + dbmd.getDatabaseProductName() + " " + dbmd.getDatabaseProductVersion());
				System.out.println("DBMS User:    " + dbmd.getUserName());

				System.out.println("");
				System.out.println("Databases:");
				try (ResultSet rs = dbmd.getCatalogs())
				{
					while (rs.next())
						System.out.println("    " + rs.getString(1));
				}

				if (StringUtil.hasValue(testSql))
				{
					System.out.println("");
					System.out.println("test.sql: " + testSql);
					try (Statement stmnt = conn.createStatement(); ResultSet rs = stmnt.executeQuery(testSql))
					{
						ResultSetMetaData rsmd = rs.getMetaData();
						int row = 0;
						while (rs.next())
						{
							row++;
							System.out.println("  --- row " + row);
							for (int c = 1; c <= rsmd.getColumnCount(); c++)
								System.out.println("    " + rsmd.getColumnLabel(c) + " = " + rs.getString(c));
						}
					}
				}
			}
			return true;
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: JDBC connection failed: " + ex);
			ex.printStackTrace(System.out);
			return false;
		}
	}

	/** @return null if no Host Monitor is configured */
	private static Boolean testHostMon(Configuration conf)
	{
		String sshUser    = conf.getProperty("conn.sshUsername");
		String sshPasswd  = conf.getProperty("conn.sshPassword");
		String sshKeyFile = conf.getProperty("conn.sshKeyFile");
		String sshHost    = conf.getProperty("conn.sshHostname");
		int    sshPort    = conf.getIntProperty("conn.sshPort", 22);

		// The same as the collector (CounterCollectorThreadNoGui): Cloud first, then SSH
		CloudConfig cloudConfig = CloudConfig.fromConfiguration(conf, sshHost, sshUser, sshKeyFile);

		if (cloudConfig == null && (StringUtil.isNullOrBlank(sshHost) || StringUtil.isNullOrBlank(sshUser)))
			return null;

		System.out.println("");
		System.out.println("==========================================================================");
		System.out.println(" Host Monitor Connection (" + (cloudConfig != null ? "Cloud: " + cloudConfig.getProvider() : "SSH") + ")");
		System.out.println("==========================================================================");

		HostMonitorConnection hostMonConn = null;
		try
		{
			if (cloudConfig != null)
			{
				HostMonitorConnectionCloud cloudConn = HostMonitorConnectionCloud.create(cloudConfig);
				hostMonConn = cloudConn;

				// Printed after create(): it may resolve the project from the credentials file / VM metadata
				System.out.println("Cloud Config: " + cloudConfig);

				cloudConn.connect(); // Cloud Monitoring
				System.out.println("");
				System.out.println(cloudConn.testCloudConnection()); // Cloud Monitoring + Cloud Logging
			}
			else
			{
				System.out.println("Host: " + sshHost + ":" + sshPort + ", User: " + sshUser + ", KeyFile: " + sshKeyFile);

				SshConnection sshConn = new SshConnection(sshHost, sshPort, sshUser, sshPasswd, sshKeyFile);
				sshConn.connect();
				hostMonConn = new HostMonitorConnectionSsh(sshConn);

				System.out.println("Connected.");
				System.out.println("OS Name:    " + hostMonConn.getOsName());
				System.out.println("OS Cores:   " + hostMonConn.getOsCoreCount());
			}
			return true;
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: Host Monitor connection failed: " + ex);
			ex.printStackTrace(System.out);
			return false;
		}
		finally
		{
			if (hostMonConn != null)
				hostMonConn.closeConnection();
		}
	}
}
