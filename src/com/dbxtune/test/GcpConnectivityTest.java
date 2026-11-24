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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

	private static void printUsage(String errorMsg)
	{
		if (errorMsg != null)
			System.out.println("ERROR: " + errorMsg + "\n");
		System.out.println("Usage: dbxtune.sh gcpconntest [file.props] [-S host[:port][/dbname]] [-U user] [-P passwd] [-A alias] [-u gcp|sshUser] [-s project:instance|sshHost[:port]] [-k keyFile] [-p sshPasswd]");
		System.out.println("       Tests the JDBC connection and the Host Monitor connection (GCP Cloud SQL or SSH).");
		System.out.println("       The switches are the same as when starting the collector (postgrestune.sh), and they override the properties file.");
		System.out.println("");
		System.out.println("Example, the same switches as the collector in SERVER_LIST:");
		System.out.println("  dbxtune.sh gcpconntest -S pg-host.acme.com:5432 -U dbxtune-monitor@my-sa-project.iam -u gcp -s my-db-project:my-instance -A MY_SRV");
		System.out.println("");
		System.out.println("Example properties file:");
		System.out.println("");
		System.out.println(EXAMPLE_PROPS);
	}

	public static void main(String[] args)
	{
		// The same switches as the collector (DbxTune.java): -S -U -P -A for the DBMS, -u -p -s -k for the Host Monitor
		Options options = new Options();
		options.addOption( Option.builder("S").longOpt("server"     ).hasArg(true).build() );
		options.addOption( Option.builder("U").longOpt("user"       ).hasArg(true).build() );
		options.addOption( Option.builder("P").longOpt("passwd"     ).hasArg(true).build() );
		options.addOption( Option.builder("A").longOpt("serverAlias").hasArg(true).build() );
		options.addOption( Option.builder("u").longOpt("sshUser"    ).hasArg(true).build() );
		options.addOption( Option.builder("p").longOpt("sshPasswd"  ).hasArg(true).build() );
		options.addOption( Option.builder("s").longOpt("sshServer"  ).hasArg(true).build() );
		options.addOption( Option.builder("k").longOpt("sshKeyFile" ).hasArg(true).build() );

		CommandLine cmd;
		try
		{
			cmd = new DefaultParser().parse(options, args);
		}
		catch (ParseException ex)
		{
			printUsage(ex.getMessage());
			System.exit(1);
			return;
		}

		if (cmd.getArgList().size() > 1)
		{
			printUsage("Only one properties file can be given, got: " + cmd.getArgList());
			System.exit(1);
		}

		String propFile = cmd.getArgList().isEmpty() ? null : cmd.getArgList().get(0);
		if (propFile != null && ! new File(propFile).exists())
		{
			printUsage("The file '" + propFile + "' does not exist.");
			System.exit(1);
		}
		if (propFile == null && ! cmd.hasOption('S'))
		{
			printUsage(null);
			System.exit(1);
		}

		// Show INFO messages from the GCP helpers (which token/credentials file was used, what was resolved)
		Configurator.setRootLevel(Level.INFO);

		// The GCP helpers read some properties from the Combined Configuration (conn.dbms.cloud.iamAuth, ...)
		Configuration conf = propFile != null ? new Configuration(propFile) : new Configuration();
		Configuration.setInstance(Configuration.SYSTEM_CONF, conf);
		Configuration.setSearchOrder(Configuration.SYSTEM_CONF);

		// The switches into the same properties as the collector does it (DbxTune.java)
		if (cmd.hasOption('S')) conf.setProperty("conn.dbmsHostPort",    cmd.getOptionValue('S'));
		if (cmd.hasOption('U')) conf.setProperty("conn.dbmsUsername",    cmd.getOptionValue('U'));
		if (cmd.hasOption('P')) conf.setProperty("conn.dbmsPassword",    cmd.getOptionValue('P'));
		if (cmd.hasOption('A')) conf.setProperty("conn.dbmsServerAlias", cmd.getOptionValue('A'));
		if (cmd.hasOption('u')) conf.setProperty("conn.sshUsername",     cmd.getOptionValue('u'));
		if (cmd.hasOption('p')) conf.setProperty("conn.sshPassword",     cmd.getOptionValue('p'));
		if (cmd.hasOption('k')) conf.setProperty("conn.sshKeyFile",      cmd.getOptionValue('k'));
		if (cmd.hasOption('s'))
		{
			// 'host:22' is host + port, other 'a:b' (like 'project:instance') is kept as it is
			String   s  = cmd.getOptionValue('s');
			String[] sa = s.split(":");
			boolean  hostAndPort = sa.length == 2 && StringUtil.isInteger(sa[1]);
			conf.setProperty("conn.sshHostname", hostAndPort ? sa[0] : s);
			if (hostAndPort)
				conf.setProperty("conn.sshPort", sa[1]);
		}
		// Like the collector: a Host Monitor user, but no host -> take the host from the DBMS host
		if ( ! conf.hasProperty("conn.sshHostname") && conf.hasProperty("conn.sshUsername") && conf.hasProperty("conn.dbmsHostPort") )
		{
			String sshHostname = conf.getProperty("conn.dbmsHostPort").split("[:/]")[0];
			conf.setProperty("conn.sshHostname", sshHostname);
			System.out.println("NOTE: No Host Monitor host (-s) was given, taking it from the DBMS host (-S), as the collector does: '" + sshHostname + "'.");
		}

		printCollectorCommandLine(conf);

		boolean jdbcOk    = testJdbc(conf);
		Boolean hostMonOk = testHostMon(conf);

		System.out.println("");
		System.out.println("==========================================================================");
		System.out.println(" JDBC:         " + (jdbcOk ? "OK" : "FAILED"));
		System.out.println(" Host Monitor: " + (hostMonOk == null ? "not configured" : hostMonOk ? "OK" : "FAILED"));
		System.out.println("==========================================================================");

		System.exit(jdbcOk && (hostMonOk == null || hostMonOk) ? 0 : 1);
	}

	/** Print how the collector would be started with the same settings (for SERVER_LIST, where the .conf is the shared GENERIC one) */
	private static void printCollectorCommandLine(Configuration conf)
	{
		String url      = conf.getProperty("conn.url", "");
		String hostPort = conf.getProperty("conn.dbmsHostPort", "");
		String urlOpts  = "";
		if (StringUtil.isNullOrBlank(hostPort) && url.startsWith("jdbc:postgresql://"))
		{
			// jdbc:postgresql://host:port/db?a=b&c=d  -->  -S host:port/db  -O "a=b;c=d"
			String rest = url.substring("jdbc:postgresql://".length());
			int    q    = rest.indexOf('?');
			hostPort = q < 0 ? rest : rest.substring(0, q);
			urlOpts  = q < 0 ? ""   : rest.substring(q + 1).replace('&', ';');
		}

		String passwd   = conf.getProperty("conn.dbmsPassword", "");
		String sshPw    = conf.getProperty("conn.sshPassword", "");
		String sshUser  = conf.getProperty("conn.sshUsername", "");
		String sshHost  = conf.getProperty("conn.sshHostname", "");
		String sshPort  = conf.getProperty("conn.sshPort", "22");
		String keyFile  = conf.getProperty("conn.sshKeyFile", "");

		StringBuilder sb = new StringBuilder("postgrestune.sh -n <collector.conf>");
		if (StringUtil.hasValue(hostPort)) sb.append(" -S ").append(hostPort);
		if (StringUtil.hasValue(urlOpts))  sb.append(" -O \"").append(urlOpts).append("\"");
		sb.append(" -U ").append(conf.getProperty("conn.dbmsUsername", "<user>"));
		if (StringUtil.hasValue(passwd))   sb.append(" -P ").append(passwd.regionMatches(true, 0, "gcp-iam", 0, 7) ? passwd : "'***'");
		if (conf.hasProperty("conn.dbmsServerAlias")) sb.append(" -A ").append(conf.getProperty("conn.dbmsServerAlias"));

		// Host Monitor: for a Cloud instance '-s' gets what was resolved (project:instance), so it works without any 'conn.hostmon.cloud.*' in the .conf
		CloudConfig cloudConfig = CloudConfig.fromConfiguration(conf, sshHost, sshUser, keyFile);
		if (cloudConfig != null)
		{
			sb.append(" -u ").append(StringUtil.hasValue(sshUser) ? sshUser : cloudConfig.getProvider().name().toLowerCase());
			String region = StringUtil.hasValue(cloudConfig.getRegion()) ? cloudConfig.getRegion() + ":" : "";
			sb.append(" -s ").append(cloudConfig.getProjectId()).append(":").append(region).append(cloudConfig.getInstanceId());
			if (StringUtil.hasValue(cloudConfig.getCredentialsFile())) sb.append(" -k ").append(cloudConfig.getCredentialsFile());
		}
		else if (StringUtil.hasValue(sshUser))
		{
			sb.append(" -u ").append(sshUser);
			if (StringUtil.hasValue(sshHost)) sb.append(" -s ").append(sshHost).append("22".equals(sshPort) ? "" : ":" + sshPort);
			if (StringUtil.hasValue(sshPw))   sb.append(" -p '***'");
			if (StringUtil.hasValue(keyFile)) sb.append(" -k ").append(keyFile);
		}

		System.out.println("");
		System.out.println("==========================================================================");
		System.out.println(" Collector command line with the same settings");
		System.out.println("==========================================================================");
		System.out.println(sb);
		System.out.println("(In SERVER_LIST add: -L <logDir> --savedir <dataDir>, and use <SRVNAME> for -S)");
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

		// The same URL as the collector (CounterControllerAbstract.noGuiConnect): '/postgres' if no database was given
		if (StringUtil.isNullOrBlank(url) && StringUtil.hasValue(hostPort))
			url = "jdbc:postgresql://" + hostPort + (hostPort.contains("/") ? "" : "/postgres");

		if (StringUtil.isNullOrBlank(url))
		{
			System.out.println("ERROR: No '-S', 'conn.url' or 'conn.dbmsHostPort' was given.");
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
