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
package com.dbxtune.loadgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.AppDir;
import com.dbxtune.Version;
import com.dbxtune.loadgen.scenario.Scenario;
import com.dbxtune.sql.conn.ConnectionProp;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.utils.AseConnectionFactory;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Logging;
import com.dbxtune.utils.OpenSslAesUtil;
import com.dbxtune.utils.StringUtil;

/**
 * DbxLoadGen - a load generator for SQL Server, Sybase ASE and Postgres (more vendors later).
 * <p>
 * Used to put "dummy traffic" on test servers, so the DbxTune collectors and DbxCentral have something to show,
 * and to find bugs. Every run writes a journal of what was done (see {@link Journal}).
 * <pre>
 * loadgen install   -t sqlserver -S host[:port] -U sa [-P pw] --size small
 * loadgen run       -t sqlserver -S host[:port] --duration 15m-45m --intensity low
 * loadgen status    -t sqlserver -S host[:port]
 * loadgen uninstall -t sqlserver -S host[:port] -U sa
 * loadgen journal   [--name srvName]
 * loadgen scenarios
 * </pre>
 */
public class LoadGen
{
	private static final Logger _logger = LogManager.getLogger();

	public static final String APP_NAME        = "DbxLoadGen";
	public static final String PRODUCT_MARKER  = "DbxLoadGen";
	public static final String DEFAULT_DBNAME  = "dbxt_loadgen";
	public static final String LOGIN_PREFIX    = "dbxt_user_";
	public static final String APPNAME_PREFIX  = "dbxt-loadgen";
	public static final int    INSTALL_VERSION = 1;

	public static final int EXIT_OK            = 0;
	public static final int EXIT_USAGE         = 1;
	public static final int EXIT_NOT_AVAILABLE = 2;   // can't connect, not installed, already installed...
	public static final int EXIT_FAILED        = 3;

	private Vendor     _vendor;
	private String     _host;
	private int        _port;
	private String     _srvName;
	private String     _adminUser;
	private String     _adminPassword;
	private String     _dbname = DEFAULT_DBNAME;
	private String     _passwdFile;
	private Properties _props = new Properties();
	private CommandLine _cmd;
	private Journal    _journal;

	public Vendor      getVendor()     { return _vendor; }
	public String      getSrvName()    { return _srvName; }
	public String      getHost()       { return _host; }
	public int         getPort()       { return _port; }
	public String      getDbname()     { return _dbname; }
	public String      getAdminUser()  { return _adminUser; }
	public String      getPasswdFile() { return _passwdFile; }
	public CommandLine getCmd()        { return _cmd; }
	public Journal     getJournal()    { return _journal; }
	public void        setJournal(Journal j) { _journal = j; }

	//------------------------------------------------------------------
	// Properties: built-in defaults (in the code) < ${DBXTUNE_HOME}/conf/loadgen.properties < ~/.dbxtune/loadgen/loadgen.properties < -c file
	//------------------------------------------------------------------
	public String getProp(String key, String defaultValue)
	{
		String val = _props.getProperty(key);
		return StringUtil.hasValue(val) ? val.trim() : defaultValue;
	}
	public int getIntProp(String key, int defaultValue)
	{
		return StringUtil.parseInt(getProp(key, null), defaultValue);
	}
	public boolean getBoolProp(String key, boolean defaultValue)
	{
		String val = getProp(key, null);
		return val == null ? defaultValue : val.equalsIgnoreCase("true");
	}

	/** A scenario setting: scenario.{name}.{key} */
	public int getScenarioIntProp(Scenario sc, String key, int defaultValue)
	{
		return getIntProp("scenario." + sc.getName() + "." + key, defaultValue);
	}

	private void loadProps(String file, boolean mustExist)
	throws Exception
	{
		if (file == null)
			return;
		File f = new File(file);
		if (!f.exists())
		{
			if (mustExist)
				throw new Exception("Config file '" + file + "' does not exist.");
			return;
		}
		try (InputStream in = new FileInputStream(f))
		{
			_props.load(in);
			_logger.info("Loaded config file: " + f.getAbsolutePath());
		}
	}

	//------------------------------------------------------------------
	// Passwords: an OWN password file, never the normal DbxTune one
	//------------------------------------------------------------------
	/** ~/.dbxtune/loadgen/.loadgen_passwd.enc */
	public static String getDefaultPasswdFile()
	{
		return AppDir.getDbxUserHomeDir(true) + "loadgen" + File.separator + ".loadgen_passwd.enc";
	}

	/** Passwords already read from the file (user -> password): the file is read once per user, not at every connect */
	private final java.util.Map<String, String> _passwdCache = new java.util.concurrent.ConcurrentHashMap<>();

	/** @return the password from the load generator's password file, null if not found */
	public String readPassword(String user)
	{
		String cached = _passwdCache.get(user);
		if (cached != null)
			return cached;
		try
		{
			if (!new File(_passwdFile).exists())
				return null;
			String pw = OpenSslAesUtil.readPasswdFromFile(user, _srvName, _passwdFile);
			if (pw != null)
				_passwdCache.put(user, pw);
			return pw;
		}
		catch (Exception ex)
		{
			_logger.warn("Problems reading password for user '" + user + "', server '" + _srvName + "' from '" + _passwdFile + "'. Caught: " + ex);
			return null;
		}
	}

	public void writePassword(String user, String password)
	throws Exception
	{
		File f = new File(_passwdFile);
		f.getParentFile().mkdirs();
		if (!f.exists())
			f.createNewFile();
		OpenSslAesUtil.writePasswdToFile(password, user, _srvName, _passwdFile);
		_passwdCache.put(user, password);
	}

	public void removePassword(String user)
	{
		_passwdCache.remove(user);
		try
		{
			if (new File(_passwdFile).exists())
				OpenSslAesUtil.removePasswdFromFile(user, _srvName, _passwdFile);
		}
		catch (Exception ex)
		{
			_logger.warn("Problems removing password for user '" + user + "' from '" + _passwdFile + "'. Caught: " + ex);
		}
	}

	/** The admin password: -P, else the load generator's password file */
	public String getAdminPassword()
	throws Exception
	{
		if (_adminPassword != null)
			return _adminPassword;
		String pw = readPassword(_adminUser);
		if (pw == null)
			throw new Exception("No password for admin user '" + _adminUser + "' on server '" + _srvName + "'. Use -P, or store it in the load generator's own password file with:\n"
					+ "   dbxPassword.sh  set -U " + _adminUser + " -S " + _srvName + " -f " + _passwdFile + " -P <password>   (Linux)\n"
					+ "   dbxPassword.bat set -U " + _adminUser + " -S " + _srvName + " -F " + _passwdFile + " -P <password>   (Windows)");
		return pw;
	}

	//------------------------------------------------------------------
	// Connections
	//------------------------------------------------------------------
	/** Connect as the admin user (sa/postgres) */
	public LgSession connectAdmin(String dbname)
	throws Exception
	{
		return connect(_adminUser, getAdminPassword(), dbname, APPNAME_PREFIX + ":admin", null);
	}

	/** Connect as one of the dbxt_user_# logins, the password comes from the load generator's password file */
	public LgSession connectUser(String login, String appName, Stats stats)
	throws Exception
	{
		String pw = readPassword(login);
		if (pw == null)
			throw new Exception("No password for '" + login + "' on server '" + _srvName + "' in '" + _passwdFile + "'. Was 'install' done from this machine/user?");
		return connect(login, pw, _dbname, appName, stats);
	}

	public LgSession connect(String user, String password, String dbname, String appName, Stats stats)
	throws SQLException
	{
		ConnectionProp cp = new ConnectionProp();
		cp.setDriverClass(_vendor.getDriverClass());
		cp.setUrl(_vendor.getUrl(_host, _port, dbname));
		cp.setUsername(user);
		cp.setPassword(password);
		cp.setAppName(appName);
		cp.setLoginTimeout(20);

		switch (_vendor)
		{
		case SQLSERVER:
			cp.setUrlOption("sendStringParametersAsUnicode", "false"); // VARCHAR parameters; NVARCHAR only where a scenario asks for it (LgSession.NString)
			cp.setUrlOption("useBulkCopyForBatchInsert", "true");
			cp.setUrlOption("trustServerCertificate", "true");
			break;
		case ASE:
			cp.setUrlOption("ENCRYPT_PASSWORD", "true");
			cp.setUrlOption("APPLICATIONNAME", appName);
			cp.setUrlOption("DYNAMIC_PREPARE", "true");
			break;
		case POSTGRES:
			cp.setUrlOption("ApplicationName", appName);
			cp.setUrlOption("reWriteBatchedInserts", "true");
			break;
		}

		try
		{
			DbxConnection conn = DbxConnection.connect(null, cp);
			return new LgSession(this, conn, user, appName, stats);
		}
		catch (SQLException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new SQLException("Problems connecting to '" + cp.getUrl() + "' as '" + user + "'. Caught: " + ex, ex);
		}
	}

	//------------------------------------------------------------------
	// Command line
	//------------------------------------------------------------------
	private static Options buildCommandLineOptions()
	{
		Options o = new Options();
		o.addOption(Option.builder("h").longOpt("help"             ).hasArg(false).build());
		o.addOption(Option.builder("t").longOpt("dbms"             ).hasArg(true ).build());
		o.addOption(Option.builder("S").longOpt("server"           ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("name"               ).hasArg(true ).build()); // no -n: the launchers treat -n as "no GUI collector"
		o.addOption(Option.builder("U").longOpt("user"             ).hasArg(true ).build());
		o.addOption(Option.builder("P").longOpt("password"         ).hasArg(true ).build());
		o.addOption(Option.builder("D").longOpt("dbname"           ).hasArg(true ).build());
		o.addOption(Option.builder("c").longOpt("config"           ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("passwd-file"        ).hasArg(true ).build());

		o.addOption(Option.builder("s").longOpt("size"             ).hasArg(true ).build());
		o.addOption(Option.builder("L").longOpt("logins"           ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("seed"               ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("ase-data-dev"       ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("ase-log-dev"        ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("ase-db-size-mb"     ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("ase-device-dir"     ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("force"              ).hasArg(false).build());
		o.addOption(Option.builder( ).longOpt("use-existing-db"    ).hasArg(false).build());
		o.addOption(Option.builder( ).longOpt("logins-only"        ).hasArg(false).build());
		o.addOption(Option.builder( ).longOpt("keep-database"      ).hasArg(false).build());
		o.addOption(Option.builder( ).longOpt("keep-logins"        ).hasArg(false).build());

		o.addOption(Option.builder("d").longOpt("duration"         ).hasArg(true ).build());
		o.addOption(Option.builder("i").longOpt("intensity"        ).hasArg(true ).build());
		o.addOption(Option.builder("T").longOpt("threads"          ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("scenarios"          ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("exclude"            ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("no-problems"        ).hasArg(false).build());
		o.addOption(Option.builder( ).longOpt("journal-keep-days"  ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("file"               ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("run-id"             ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("central"            ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("central-srv"        ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("central-token"      ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("grace"              ).hasArg(true ).build());
		o.addOption(Option.builder( ).longOpt("no-rows"            ).hasArg(false).build());

		o.addOption(Option.builder("x").longOpt("debug"            ).hasArg(false).build());
		return o;
	}

	private static void printHelp(String errorStr)
	{
		PrintWriter pw = new PrintWriter(System.out);
		if (errorStr != null)
		{
			pw.println();
			pw.println("ERROR: " + errorStr);
		}
		pw.println();
		pw.println("usage: loadgen <mode> [options]");
		pw.println();
		pw.println("modes:");
		pw.println("  install     Create logins, database, tables and data (needs sa/sysadmin/superuser)");
		pw.println("  run         Run the workload for a while (uses the " + LOGIN_PREFIX + "# logins)");
		pw.println("  status      Is it installed? Sizes, row counts, last runs");
		pw.println("  uninstall   Drop the database and the logins (needs sa/sysadmin/superuser)");
		pw.println("  journal     Print a journal file (default: the last one)");
		pw.println("  compare     What did DbxTune see of a run? Journal vs DbxCentral alarms + graphs -> <journal>.compare.md");
		pw.println("  scenarios   List the scenarios");
		pw.println();
		pw.println("connection:");
		pw.println("  -t,--dbms <sqlserver|ase|postgres>  DBMS vendor");
		pw.println("  -S,--server <host[:port]>           Server (default port per vendor). ASE: also a server name from the interfaces file");
		pw.println("  --name <name>                       Logical server name: password file key, journal dir");
		pw.println("                                      (default: host, or host_port when a port is given)");
		pw.println("  -U,--user <admin>                   Admin user for install/uninstall/status (default: sa / postgres)");
		pw.println("  -P,--password <pw>                  Admin password (default: from the load generator's password file)");
		pw.println("  -D,--dbname <name>                  Database (default: " + DEFAULT_DBNAME + ")");
		pw.println("  --passwd-file <file>                Password file (default: " + getDefaultPasswdFile() + ")");
		pw.println("  -c,--config <file>                  Extra properties (scenario weights etc, see conf/loadgen.properties)");
		pw.println();
		pw.println("install/uninstall:");
		pw.println("  -s,--size <small|medium|large|xl|xxl|xxxl>  Data size, approx: small ~180 MB (default), medium ~1700 MB, large ~8500 MB,");
		pw.println("                                      xl ~34000 MB, xxl ~170000 MB, xxxl ~850000 MB");
		pw.println("  -L,--logins <n>                     Number of " + LOGIN_PREFIX + "# logins (default 10)");
		pw.println("  --seed <n>                          Random seed for the data (default 42)");
		pw.println("  --ase-data-dev / --ase-log-dev <dev>  ASE: existing devices for the database ('--ase-data-dev default' = the default device)");
		pw.println("                                      none of the --ase-*-dev/dir options: own devices next to the existing ones");
		pw.println("                                      (data where most data devices are, log where most log devices are)");
		pw.println("  --ase-db-size-mb <mb>               ASE database size (default: from --size)");
		pw.println("  --ase-device-dir <dir>              ASE: create own devices in this directory ON THE ASE HOST (disk init),");
		pw.println("                                      uninstall drops them and their files (sp_dropdevice ..., dropfile)");
		pw.println("  --use-existing-db                   install: into a database you created (-D name); its options are not changed,");
		pw.println("                                      and uninstall then drops only the load generator objects, not the database");
		pw.println("  --logins-only                       install: (re)create the " + LOGIN_PREFIX + "# logins of an existing install");
		pw.println("  --keep-database                     uninstall: drop only the logins (get them back with install --logins-only)");
		pw.println("  --keep-logins                       uninstall: drop only the database (or our objects), keep the logins + passwords");
		pw.println("  --force                             uninstall: also without the marker table (only " + DEFAULT_DBNAME + " / " + LOGIN_PREFIX + "#)");
		pw.println();
		pw.println("run:");
		pw.println("  -d,--duration <15m|15m-45m|1h|90s>  How long (a range = random within it). Default 15m");
		pw.println("  -i,--intensity <low|medium|high>    How hard (threads and think time). Default low");
		pw.println("  -T,--threads <n>                    OLTP threads (overrides the intensity's number)");
		pw.println("  --scenarios <a,b,c>                 Run only these scenarios");
		pw.println("  --exclude <a,b,c>                   Do not run these scenarios");
		pw.println("  --no-problems                       No problem scenarios (blocking, deadlocks...)");
		pw.println("  --journal-keep-days <n>             Remove journal files older than this (default 30)");
		pw.println();
		pw.println("journal:");
		pw.println("  --file <file>                       The journal file to print (default: the last one, for --name if given)");
		pw.println();
		pw.println("compare:");
		pw.println("  --central <url>                     DbxCentral, e.g. http://dbxc-host:8080 (or central.url in loadgen.properties)");
		pw.println("  --central-srv <name>                DbxCentral's name for the server, e.g. GS-2-WIN__SS_2025 (or central.srv.<--name>)");
		pw.println("  --central-token <token>             Only if DbxCentral requires a login: DbxTune.central.registration.token");
		pw.println("  --file <journal> | --run-id <id>    Which run (default: the newest run journal, for --name / -S if given)");
		pw.println("  --grace <time>                      How long after a problem an alarm still counts as SEEN (default 120s)");
		pw.println("  --no-rows                           Skip the row checks (CM rows per sample): only alarms + graphs, faster");
		pw.println();
		pw.println("examples (cron):");
		pw.println("  loadgen.sh install -t sqlserver -S gs-1-mssql -U sa --size small");
		pw.println("  loadgen.sh run     -t sqlserver -S gs-1-mssql --duration 15m-45m --intensity low");
		pw.println("  loadgen.sh compare --name gs-1-mssql --central http://localhost:8080 --central-srv GS-1-MSSQL");
		pw.println();
		pw.flush();
	}

	/** Parse a duration "15m", "90s", "1h", "15m-45m" (random in the range) into seconds */
	public static int parseDurationSec(String str, java.util.Random rnd)
	{
		String s = str.trim().toLowerCase();
		if (s.contains("-"))
		{
			String[] sa = s.split("-");
			int min = parseDurationSec(sa[0], rnd);
			int max = parseDurationSec(sa[1], rnd);
			if (max < min)
				throw new IllegalArgumentException("Duration range '" + str + "': max is less than min");
			return min + rnd.nextInt(max - min + 1);
		}
		int mult = 60;
		if      (s.endsWith("s")) { mult = 1;    s = s.substring(0, s.length()-1); }
		else if (s.endsWith("m")) { mult = 60;   s = s.substring(0, s.length()-1); }
		else if (s.endsWith("h")) { mult = 3600; s = s.substring(0, s.length()-1); }
		return Integer.parseInt(s.trim()) * mult;
	}

	private void init(CommandLine cmd)
	throws Exception
	{
		_cmd = cmd;

		_vendor = Vendor.fromString(cmd.getOptionValue("dbms"));
		if (_vendor == null)
			throw new ParseException("-t/--dbms is required: sqlserver, ase or postgres");

		String server = cmd.getOptionValue("server");
		if (StringUtil.isNullOrBlank(server))
			throw new ParseException("-S/--server is required: host[:port] (ASE: or a server name in the interfaces file)");

		_host = server;
		_port = _vendor.getDefaultPort();
		if (server.contains(":"))
		{
			_host = server.substring(0, server.indexOf(':'));
			_port = Integer.parseInt(server.substring(server.indexOf(':') + 1));
		}
		else if (_vendor == Vendor.ASE)
		{
			// An ASE server name (GORAN_UB3_DS): host:port from the interfaces file ($SYBASE/interfaces or %SYBASE%\ini\sql.ini, else the DbxTune private one).
			// Not found: use it as a host name with the default port.
			String hostPort = AseConnectionFactory.getIFirstPort(server) > 0 ? AseConnectionFactory.getIHostPortStr(server) : null; // host1:port1[,host2:port2]: use the first
			if (StringUtil.hasValue(hostPort))
			{
				String first = hostPort.split(",")[0].trim();
				_host = first.substring(0, first.lastIndexOf(':'));
				_port = Integer.parseInt(first.substring(first.lastIndexOf(':') + 1));
				_logger.info("ASE server '" + server + "' resolved to " + _host + ":" + _port + " from the interfaces file '" + AseConnectionFactory.getIFileName() + "'.");
			}
		}

		// ':' can't be in the password file's server field
		_srvName = cmd.getOptionValue("name", server.contains(":") ? _host + "_" + _port : server); // ASE: the server name, not the resolved host

		_adminUser     = cmd.getOptionValue("user", _vendor.getDefaultAdminUser());
		_adminPassword = cmd.getOptionValue("password");
		_dbname        = cmd.getOptionValue("dbname", DEFAULT_DBNAME);
		_passwdFile    = cmd.getOptionValue("passwd-file", getDefaultPasswdFile());

		loadAllProps(cmd);
	}

	/** ${DBXTUNE_HOME}/conf/loadgen.properties < ~/.dbxtune/loadgen/loadgen.properties < -c file */
	private void loadAllProps(CommandLine cmd)
	throws Exception
	{
		String dbxHome = System.getProperty("DBXTUNE_HOME", System.getenv("DBXTUNE_HOME"));
		if (dbxHome != null)
			loadProps(dbxHome + File.separator + "conf" + File.separator + "loadgen.properties", false);
		loadProps(AppDir.getDbxUserHomeDir(true) + "loadgen" + File.separator + "loadgen.properties", false);
		loadProps(cmd.getOptionValue("config"), true);
	}

	//------------------------------------------------------------------
	// MAIN
	//------------------------------------------------------------------
	public static void main(String[] args)
	{
		Version.setAppName(APP_NAME);
		System.exit( doMain(args) );
	}

	static int doMain(String[] args)
	{
		CommandLine cmd;
		try
		{
			cmd = new DefaultParser().parse(buildCommandLineOptions(), args);
		}
		catch (ParseException ex)
		{
			printHelp(ex.getMessage());
			return EXIT_USAGE;
		}

		if (cmd.hasOption("help") || cmd.getArgList().isEmpty())
		{
			printHelp(null);
			return cmd.hasOption("help") ? EXIT_OK : EXIT_USAGE;
		}
		if (cmd.getArgList().size() > 1)
		{
			printHelp("Only one mode at a time, got: " + cmd.getArgList());
			return EXIT_USAGE;
		}

		String mode = cmd.getArgList().get(0).toLowerCase();

		// Modes that need no server
		if (mode.equals("scenarios"))
		{
			Scenario.printList(System.out);
			return EXIT_OK;
		}
		if (mode.equals("journal"))
		{
			try
			{
				File f = cmd.hasOption("file") ? new File(cmd.getOptionValue("file")) : Journal.findLast(cmd.getOptionValue("name"), null);
				if (f == null)
				{
					System.out.println("No journal files found in " + Journal.getJournalDir());
					return EXIT_NOT_AVAILABLE;
				}
				Journal.print(f, System.out);
				return EXIT_OK;
			}
			catch (Exception ex)
			{
				System.out.println("ERROR: " + ex);
				return EXIT_FAILED;
			}
		}

		if (mode.equals("compare"))
		{
			try
			{
				// No DBMS connection: the journal + DbxCentral. Server dir: --name, else from -S like the other modes, else all servers
				LoadGen lg = new LoadGen();
				lg._cmd = cmd;
				lg.loadAllProps(cmd);
				String server = cmd.getOptionValue("server");
				String name   = cmd.getOptionValue("name", server == null ? null : server.replace(':', '_'));
				File f = null;
				if (cmd.hasOption("file"))
					f = new File(cmd.getOptionValue("file"));
				else if (cmd.hasOption("run-id"))
				{
					File[] dirs = Journal.getJournalDir().listFiles(File::isDirectory);
					for (File d : dirs == null ? new File[0] : dirs)
					{
						File[] hit = d.listFiles((dd, n) -> n.endsWith("_run_" + cmd.getOptionValue("run-id") + ".jsonl"));
						if (hit != null && hit.length > 0) f = hit[0];
					}
				}
				else
					f = Journal.findLast(name, "run");
				if (f == null || !f.exists())
				{
					System.out.println("ERROR: no run journal found (" + (cmd.hasOption("file") ? cmd.getOptionValue("file") : cmd.hasOption("run-id") ? "run id " + cmd.getOptionValue("run-id") : "newest for " + (name == null ? "any server" : name)) + ") in " + Journal.getJournalDir());
					return EXIT_NOT_AVAILABLE;
				}
				return new Compare(lg).compare(f);
			}
			catch (Exception ex)
			{
				System.out.println("ERROR: " + ex);
				ex.printStackTrace();
				return EXIT_FAILED;
			}
		}

		LoadGen lg = new LoadGen();
		try
		{
			lg.init(cmd);
		}
		catch (ParseException ex)
		{
			printHelp(ex.getMessage());
			return EXIT_USAGE;
		}
		catch (Exception ex)
		{
			System.out.println("ERROR: " + ex.getMessage());
			return EXIT_USAGE;
		}

		// Log file: ~/.dbxtune/log/DbxLoadGen.{srvName}.log
		String logFile = AppDir.getAppLogDir() + File.separator + APP_NAME + "." + lg._srvName + ".log";
		Logging.init(null, (String) null, logFile);
		if (cmd.hasOption("debug"))
			org.apache.logging.log4j.core.config.Configurator.setLevel("com.dbxtune.loadgen", org.apache.logging.log4j.Level.DEBUG);

		// DbxConnection reads the combined DbxTune configuration: give it empty ones (otherwise a WARN per missing config)
		for (String confName : new String[] {Configuration.SYSTEM_CONF, Configuration.USER_CONF, Configuration.USER_TEMP})
			if (!Configuration.hasInstance(confName))
				Configuration.setInstance(confName, new Configuration());

		_logger.info("Starting " + APP_NAME + " (DbxTune " + Version.getVersionStr() + "), mode=" + mode + ", vendor=" + lg._vendor.getName() + ", server=" + lg._host + ":" + lg._port + ", name=" + lg._srvName + ", dbname=" + lg._dbname);

		try
		{
			switch (mode)
			{
			case "install":   return new Installer(lg).install();
			case "uninstall": return new Installer(lg).uninstall();
			case "status":    return new Installer(lg).status();
			case "run":       return new RunEngine(lg).run();
			default:
				printHelp("Unknown mode '" + mode + "'");
				return EXIT_USAGE;
			}
		}
		catch (Exception ex)
		{
			_logger.error("Mode '" + mode + "' failed. Caught: " + ex, ex);
			if (lg._journal != null)
				lg._journal.event("error", "msg", "Mode '" + mode + "' failed: " + ex);
			return EXIT_FAILED;
		}
		finally
		{
			if (lg._journal != null)
				lg._journal.close();
		}
	}
}
