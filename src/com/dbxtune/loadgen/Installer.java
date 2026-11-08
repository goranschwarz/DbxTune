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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.Version;
import com.dbxtune.loadgen.scenario.EtlFactSales;
import com.dbxtune.sql.SqlScriptExecutor;
import com.dbxtune.sql.conn.TdsConnection;

/**
 * install / uninstall / status
 */
public class Installer
{
	private static final Logger _logger = LogManager.getLogger();

	public static final String PG_SCHEMA = "dbxt";
	public static final String PG_ROLE   = "dbxt_loadgen_role";

	/** ASE devices created with --ase-device-dir */
	public static final String ASE_DATA_DEV = "dbxt_loadgen_data";
	public static final String ASE_LOG_DEV  = "dbxt_loadgen_log";

	/** Every table and procedure the install creates (used when the database was NOT created by us and only our objects are dropped) */
	public static final String[] TABLES = {"dbxt_loadgen_info", "dbxt_loadgen_runlog", "category", "customer", "product", "warehouse", "stock",
			"orders", "order_line", "payment", "shipment", "audit_log", "counter_table", "session_event",
			"dim_date", "dim_customer", "dim_product", "fact_sales", "etl_watermark"};
	public static final String[] PROCEDURES = {"p_order_status", "p_rbar_price_adjust", "p_temp_report"};

	private static final Pattern LOGIN_NAME_PATTERN = Pattern.compile("^" + LoadGen.LOGIN_PREFIX + "[0-9]+$");

	private final LoadGen _lg;
	private final Vendor  _v;

	public Installer(LoadGen lg)
	{
		_lg = lg;
		_v  = lg.getVendor();
	}

	//------------------------------------------------------------------
	// Size profiles
	//------------------------------------------------------------------
	/** Row counts for one size */
	public static class Size
	{
		public final String name;
		public final int    factor;
		public final int    customers, products, warehouses, categories, orders, auditRows, events;

		Size(String name, int factor)
		{
			this.name       = name;
			this.factor     = factor;
			this.customers  =  20_000 * factor;
			this.products   =   2_000 * factor;
			this.warehouses =      10;
			this.categories =      50;
			this.orders     = 200_000 * factor;
			this.auditRows  = 100_000 * factor;
			this.events     =   5_000 * factor;
		}

		/** Rough data + index size in MB */
		public long estimateMb()
		{
			double lines = orders * 3.0;
			double bytes = customers * 200.0 + products * 250.0 + warehouses * products * 40.0
					+ orders * 100.0 + lines * 40.0 + orders * 0.9 * 50.0 + orders * 0.85 * 50.0
					+ auditRows * 150.0 + events * 2_100.0 + lines * 0.9 * 50.0;
			return (long) (bytes * 1.5 / 1024 / 1024) + 10;
		}

		public static Size fromString(String str)
		{
			switch (str == null ? "small" : str.toLowerCase())
			{
			case "small":  return new Size("small",  1);
			case "medium": return new Size("medium", 10);
			case "large":  return new Size("large",  50);
			case "xl":     return new Size("xl",     200);
			case "xxl":    return new Size("xxl",    1000);
			case "xxxl":   return new Size("xxxl",   5000); // 1 000 000 000 orders: ids are still int (max 2 147 483 647)
			}
			throw new IllegalArgumentException("Unknown size '" + str + "', use: small, medium, large, xl, xxl, xxxl");
		}
	}

	//------------------------------------------------------------------
	// INSTALL
	//------------------------------------------------------------------
	public int install()
	throws Exception
	{
		if (_lg.getCmd().hasOption("logins-only"))
			return installLoginsOnly();

		Size    size        = Size.fromString(_lg.getCmd().getOptionValue("size", "small"));
		int     loginCnt    = Integer.parseInt(_lg.getCmd().getOptionValue("logins", "10"));
		long    seed        = Long.parseLong(_lg.getCmd().getOptionValue("seed", "42"));
		String  db          = _lg.getDbname();
		boolean useExisting = _lg.getCmd().hasOption("use-existing-db");

		String runId = UUID.randomUUID().toString().substring(0, 8);
		Journal journal = new Journal(_lg.getSrvName(), "install", runId);
		_lg.setJournal(journal);

		try (LgSession admin = _lg.connectAdmin(null))
		{
			admin.setQueryTimeout(0);
			String dbmsVersion = admin.getConnection().getMetaData().getDatabaseProductVersion();

			journal.event("header", "kind", "install", "runId", runId, "server", _lg.getSrvName(), "vendor", _v.getName(),
					"host", _lg.getHost(), "port", _lg.getPort(), "dbmsVersion", dbmsVersion, "dbname", db,
					"size", size.name, "seed", seed, "logins", loginCnt, "toolVersion", Version.getVersionStr(),
					"estimatedMb", size.estimateMb(), "useExistingDb", useExisting);

			boolean dbExists = databaseExists(admin, db);
			String  errMsg   = null;
			if (dbExists && !useExisting)
				errMsg = "Database '" + db + "' already exists on '" + _lg.getSrvName() + "'. Run 'uninstall' first, or use --use-existing-db to install into it.";
			else if (!dbExists && useExisting)
				errMsg = "Database '" + db + "' does not exist on '" + _lg.getSrvName() + "' (--use-existing-db). Create it first, or skip --use-existing-db.";
			else if (useExisting)
				errMsg = checkExistingDatabase(db);
			else if (_v == Vendor.ASE)
				errMsg = chooseAseDeviceDir(admin, db, size);
			if (errMsg != null)
			{
				System.out.println("ERROR: " + errMsg);
				journal.event("error", "msg", errMsg);
				return LoadGen.EXIT_NOT_AVAILABLE;
			}

			String estMsg = "Size '" + size.name + "': about " + size.estimateMb() + " MB (data + indexes). "
					+ "Rows: customers=" + size.customers + ", products=" + size.products + ", orders=" + size.orders
					+ " (~" + (size.orders * 3L) + " order lines), audit=" + size.auditRows + ", events=" + size.events;
			_logger.info(estMsg);

			//--- Database
			if (useExisting)
				prepareExistingDatabase(admin, db);
			else
				createDatabase(admin, db, size);

			//--- Logins (+ passwords to the load generator's own password file)
			createLogins(admin, loginCnt, db);
		}

		//--- Tables, data, indexes - in the new database
		try (LgSession s = _lg.connectAdmin(db))
		{
			s.setQueryTimeout(0);
			if (_v == Vendor.POSTGRES)
			{
				s.exec("set role " + PG_ROLE);
				s.exec("set search_path = " + PG_SCHEMA);
			}

			runScript(s, "tables.sql");

			Map<String, String> info = new LinkedHashMap<>();
			info.put("product",        LoadGen.PRODUCT_MARKER);
			info.put("installVersion", String.valueOf(LoadGen.INSTALL_VERSION));
			info.put("toolVersion",    Version.getVersionStr());
			info.put("status",         "FILLING");
			info.put("size",           size.name);
			info.put("seed",           String.valueOf(seed));
			info.put("logins",         String.valueOf(loginCnt));
			info.put("customers",      String.valueOf(size.customers));
			info.put("products",       String.valueOf(size.products));
			info.put("warehouses",     String.valueOf(size.warehouses));
			info.put("categories",     String.valueOf(size.categories));
			info.put("orders",         String.valueOf(size.orders));
			info.put("events",         String.valueOf(size.events));
			info.put("installTs",      Journal.now());
			info.put("installBy",      System.getProperty("user.name"));
			info.put("databaseCreated", String.valueOf(!useExisting)); // false: uninstall drops only our objects, never the database
			for (int i=1; i<=loginCnt; i++)
				info.put("login." + i, LoadGen.LOGIN_PREFIX + i);
			for (Map.Entry<String, String> e : info.entrySet())
				s.update("insert into dbxt_loadgen_info (k, v) values (?, ?)", e.getKey(), e.getValue());

			long start = System.currentTimeMillis();
			new DataFiller(s, size, seed, loginCnt).fill();

			runScript(s, "indexes.sql");
			updateStatistics(s);

			s.update("update dbxt_loadgen_info set v = ? where k = 'status'", "INSTALLED");
			String msg = "Install finished in " + ((System.currentTimeMillis() - start) / 1000) + " s (data, indexes, statistics).";
			_logger.info(msg);
			journal.event("info", "msg", msg);
		}

		// The users need their own grants/aliases in the database (done after the objects exist)
		try (LgSession s = _lg.connectAdmin(db))
		{
			for (int i=1; i<=loginCnt; i++)
				createDbUser(s, LoadGen.LOGIN_PREFIX + i);
		}

		journal.event("summary", "result", "INSTALLED");
		System.out.println("Installed '" + db + "' on '" + _lg.getSrvName() + "'. Journal: " + journal.getFile());
		return LoadGen.EXIT_OK;
	}

	/** Create (or, if they exist, reset) logins dbxt_user_1..N; passwords go to the load generator's own password file */
	private void createLogins(LgSession admin, int loginCnt, String db)
	throws Exception
	{
		Random pwRnd = new java.security.SecureRandom();
		for (int i=1; i<=loginCnt; i++)
		{
			String  login  = LoadGen.LOGIN_PREFIX + i;
			String  pw     = generatePassword(pwRnd);
			boolean exists = loginExists(admin, login);
			if (exists)
				resetLogin(admin, login, pw, db);
			else
				createLogin(admin, login, pw, db);
			_lg.writePassword(login, pw);
			_lg.getJournal().event("object", "action", exists ? "reset" : "create", "objectType", "login", "name", login);
		}
		_logger.info("Created/reset " + loginCnt + " logins, passwords stored in '" + _lg.getPasswdFile() + "' (server key '" + _lg.getSrvName() + "').");
	}

	/**
	 * install --logins-only: (re)create the logins of an existing install, for example after 'uninstall --keep-database'.
	 * The number of logins comes from the install info (or -L).
	 */
	private int installLoginsOnly()
	throws Exception
	{
		String db = _lg.getDbname();
		String runId = UUID.randomUUID().toString().substring(0, 8);
		Journal journal = new Journal(_lg.getSrvName(), "install", runId);
		_lg.setJournal(journal);
		journal.event("header", "kind", "install --logins-only", "runId", runId, "server", _lg.getSrvName(), "vendor", _v.getName(), "dbname", db);

		Map<String, String> info = readInstallInfo(db);
		if (info == null)
		{
			String msg = "No " + LoadGen.PRODUCT_MARKER + " install found in '" + db + "' on '" + _lg.getSrvName() + "'. Do a full install first.";
			System.out.println("ERROR: " + msg);
			journal.event("error", "msg", msg);
			return LoadGen.EXIT_NOT_AVAILABLE;
		}
		int loginCnt = Integer.parseInt(_lg.getCmd().getOptionValue("logins", info.getOrDefault("logins", "10")));

		try (LgSession admin = _lg.connectAdmin(null))
		{
			admin.setQueryTimeout(0);
			createLogins(admin, loginCnt, db);
		}
		try (LgSession s = _lg.connectAdmin(db))
		{
			if (_v == Vendor.POSTGRES)
				s.exec("set search_path = " + PG_SCHEMA);
			for (int i=1; i<=loginCnt; i++)
				createDbUser(s, LoadGen.LOGIN_PREFIX + i);
			s.update("delete from dbxt_loadgen_info where k like 'login.%' or k in ('logins', 'loginsDropped')");
			s.update("insert into dbxt_loadgen_info (k, v) values ('logins', ?)", String.valueOf(loginCnt));
			for (int i=1; i<=loginCnt; i++)
				s.update("insert into dbxt_loadgen_info (k, v) values (?, ?)", "login." + i, LoadGen.LOGIN_PREFIX + i);
		}
		journal.event("summary", "result", "LOGINS_CREATED", "logins", loginCnt);
		System.out.println("Created/reset " + loginCnt + " logins for '" + db + "' on '" + _lg.getSrvName() + "'. Journal: " + journal.getFile());
		return LoadGen.EXIT_OK;
	}

	/** @return the install info (dbxt_loadgen_info) of the database, null if the database or the marker is missing */
	private Map<String, String> readInstallInfo(String db)
	{
		try (LgSession s = _lg.connectAdmin(db))
		{
			if (_v == Vendor.POSTGRES)
				s.exec("set search_path = " + PG_SCHEMA);
			Map<String, String> info = new LinkedHashMap<>();
			for (Object[] row : s.query("select k, v from dbxt_loadgen_info"))
				info.put((String) row[0], (String) row[1]);
			return LoadGen.PRODUCT_MARKER.equals(info.get("product")) ? info : null;
		}
		catch (Exception ex)
		{
			_logger.info("No install info in '" + db + "': " + ex.getMessage());
			return null;
		}
	}

	/**
	 * --use-existing-db: the database must not hold a load generator install, nor tables/procedures with our names.
	 * @return an error message, null if OK
	 */
	private String checkExistingDatabase(String db)
	{
		try (LgSession s = _lg.connectAdmin(db))
		{
			if (_v == Vendor.POSTGRES)
			{
				if (s.queryInt("select count(*) from pg_namespace where nspname = ?", PG_SCHEMA) > 0)
					return "Schema '" + PG_SCHEMA + "' already exists in '" + db + "'. The load generator needs its own schema: drop it, or use another database.";
				return null;
			}
			List<String> found = new ArrayList<>();
			for (String name : TABLES)
				if (s.queryInt("select count(*) from sysobjects where name = ?", name) > 0)
					found.add(name);
			for (String name : PROCEDURES)
				if (s.queryInt("select count(*) from sysobjects where name = ?", name) > 0)
					found.add(name);
			if (!found.isEmpty())
				return "Database '" + db + "' already has objects with load generator names: " + found + ". Uninstall the old install, or use another database.";

			// Show the options it runs with (we do not change them in a database we did not create)
			if (_v == Vendor.SQLSERVER)
				for (Object[] row : s.query("select recovery_model_desc, is_query_store_on from sys.databases where name = ?", db))
					System.out.println("Existing database '" + db + "': recovery model " + row[0] + ", Query Store " + (Boolean.TRUE.equals(row[1]) ? "on" : "off") + " (not changed).");
			if (_v == Vendor.ASE)
			{
				int truncLog = s.queryInt("select count(*) from master..sysdatabases where name = ? and status & 8 = 8", db);
				System.out.println("Existing database '" + db + "': 'trunc log on chkpt' is " + (truncLog > 0 ? "on" : "OFF - the transaction log fills up unless you dump it") + " (not changed).");
			}
			return null;
		}
		catch (Exception ex)
		{
			return "Problems checking the existing database '" + db + "': " + ex.getMessage();
		}
	}

	/** --use-existing-db: what the database needs besides the tables (Postgres: the owner role and the schema) */
	private void prepareExistingDatabase(LgSession admin, String db)
	throws SQLException
	{
		_logger.info("Using the existing database '" + db + "' (its options are not changed).");
		if (_v == Vendor.POSTGRES)
			createPgRoleAndSchema(admin, db);
		_lg.getJournal().event("object", "action", "use-existing", "objectType", "database", "name", db);
	}

	/** Postgres: the role that owns everything, and the schema dbxt */
	private void createPgRoleAndSchema(LgSession admin, String db)
	throws SQLException
	{
		if (admin.queryInt("select count(*) from pg_roles where rolname = ?", PG_ROLE) == 0)
			admin.exec("create role " + PG_ROLE + " nologin");
		admin.exec("grant connect, temporary on database " + db + " to " + PG_ROLE);
		try (LgSession s = _lg.connectAdmin(db))
		{
			s.exec("create schema " + PG_SCHEMA + " authorization " + PG_ROLE);
		}
		catch (Exception ex)
		{
			throw new SQLException("Problems creating schema '" + PG_SCHEMA + "' in '" + db + "'. Caught: " + ex, ex);
		}
		_lg.getJournal().event("object", "action", "create", "objectType", "schema", "name", PG_SCHEMA);
	}

	private static String generatePassword(Random rnd)
	{
		String letters = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
		String chars   = letters + "23456789";
		StringBuilder sb = new StringBuilder();
		sb.append(letters.charAt(rnd.nextInt(letters.length())));
		for (int i=0; i<15; i++)
			sb.append(chars.charAt(rnd.nextInt(chars.length())));
		sb.append(rnd.nextInt(10)); // at least one digit
		return sb.toString();
	}

	private boolean databaseExists(LgSession admin, String db)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER: return admin.queryInt("select count(*) from sys.databases where name = ?", db) > 0;
		case ASE:       return admin.queryInt("select count(*) from master..sysdatabases where name = ?", db) > 0;
		case POSTGRES:  return admin.queryInt("select count(*) from pg_database where datname = ?", db) > 0;
		}
		return false;
	}

	private void createDatabase(LgSession admin, String db, Size size)
	throws SQLException
	{
		_logger.info("Creating database '" + db + "'...");
		switch (_v)
		{
		case SQLSERVER:
			admin.exec("create database [" + db + "]");
			admin.exec("alter database [" + db + "] set recovery simple");
			try { admin.exec("alter database [" + db + "] set query_store = on"); }
			catch (SQLException ex) { _logger.info("Query Store not enabled (needs 2016 or later): " + ex.getMessage()); }
			break;

		case ASE:
			{
				String dataDev = _lg.getCmd().getOptionValue("ase-data-dev", "default");
				String logDev  = _lg.getCmd().getOptionValue("ase-log-dev");
				long   sizeMb  = Long.parseLong(_lg.getCmd().getOptionValue("ase-db-size-mb", String.valueOf(Math.max(300, size.estimateMb() * 2))));
				// Log: measured on ASE 16 (medium): ~56 MB peak during a 15 min high run (long_tran pins the log ~340 s),
				// the fill is kept low by a checkpoint every 10 s (DataFiller). So a fixed size, not one that grows with the data.
				long   logMb   = Long.parseLong(_lg.getCmd().getOptionValue("ase-log-size-mb", "1024"));

				// Own devices: 'disk init' in --ase-device-dir, or in the directories chooseAseDeviceDir() found (dropped again by uninstall)
				if (_aseDataDir != null)
				{
					dataDev = ASE_DATA_DEV;
					logDev  = ASE_LOG_DEV;

					// If the other device files are named after the server (SYBASE.pml_data_1.dev, GORAN_UB3_DS_data1.dat), do the same: "<@@servername><char after it>"
					String filePrefix = "";
					String srvName    = String.valueOf(admin.queryValue("select @@servername")).trim();
					if (!srvName.isEmpty() && !"null".equals(srvName))
					{
						for (Object[] row : admin.query("select phyname from master..sysdevices where cntrltype = 0"))
						{
							String phys = String.valueOf(row[0]).trim();
							String file = phys.substring(Math.max(phys.lastIndexOf('/'), phys.lastIndexOf('\\')) + 1);
							int    p    = file.toLowerCase().indexOf(srvName.toLowerCase());
							if (p >= 0)
							{
								char next = p + srvName.length() < file.length() ? file.charAt(p + srvName.length()) : '.';
								filePrefix = srvName + (next == '.' || next == '_' || next == '-' ? String.valueOf(next) : ".");
								break;
							}
						}
					}

					createAseDevice(admin, dataDev, _aseDataDir + _aseDataSep + filePrefix + dataDev + _aseDevExt, sizeMb);
					createAseDevice(admin, logDev,  _aseLogDir  + _aseLogSep  + filePrefix + logDev  + _aseDevExt, logMb);
				}

				String sql = "create database " + db + " on " + dataDev + " = '" + sizeMb + "M'";
				if (logDev != null)
					sql += " log on " + logDev + " = '" + logMb + "M'";
				_logger.info("ASE: " + sql);
				// Show the server's progress messages while it runs ("CREATE DATABASE: allocating ...", "Processed N allocation unit(s) out of M units ... nn% completed")
				TdsConnection tds = admin.getConnection() instanceof TdsConnection ? (TdsConnection) admin.getConnection() : null;
				if (tds != null)
					tds.setSybMessageHandler(sqle ->
					{
						if (!(sqle instanceof SQLWarning)) // errors: thrown by exec() as usual
							return sqle;
						String msg = sqle.getMessage() == null ? "" : sqle.getMessage().trim();
						if (!msg.isEmpty())
							_logger.info("ASE: " + msg);
						return null;
					});
				try
				{
					admin.exec(sql);
				}
				finally
				{
					if (tds != null)
						tds.restoreSybMessageHandler();
				}
				admin.exec("exec master..sp_dboption " + db + ", 'trunc log on chkpt', true");
				admin.exec("exec master..sp_dboption " + db + ", 'select into/bulkcopy/pllsort', true");
				admin.exec("use " + db);
				admin.exec("checkpoint");
				admin.exec("use master");
			}
			break;

		case POSTGRES:
			admin.exec("create database " + db);
			createPgRoleAndSchema(admin, db);
			break;
		}
		_lg.getJournal().event("object", "action", "create", "objectType", "database", "name", db);
	}

	/** ASE: where createDatabase() puts its own data and log device (null = on --ase-data-dev / --ase-log-dev, or the 'default' device) */
	private String _aseDataDir, _aseLogDir, _aseDataSep = "/", _aseLogSep = "/", _aseDevExt = ".dev";

	/** ASE: a device name (or file name) that says "log": log, logs, xlog, tlog, wal (+ optional dev/device) as a separate word part (pml_log_1, tlog01, sales_xlog, logdev; not syslogs, catalog) */
	private static final Pattern ASE_LOG_DEVICE_NAME = Pattern.compile("(?i)(^|[^a-z])(x|t)?(logs?|wal)(dev(ice)?)?([^a-z]|$)");

	/**
	 * ASE: decide where the database goes, BEFORE anything is created.
	 * <ul>
	 *   <li>--ase-data-dev / --ase-log-dev: on those existing devices (--ase-data-dev default = the default device)</li>
	 *   <li>--ase-device-dir: own devices in that directory</li>
	 *   <li>none of them: own devices next to the existing ones, read from master..sysdevices (disk devices with a file, not raw partitions):
	 *       the data device in the directory that holds most data devices, the log device in the directory that holds most
	 *       log devices (a name with log/logs/xlog/tlog/wal; none: next to the data device). User devices count first;
	 *       master, sysprocsdev, systemdbdev, sybsysdb, sybmgmtdev, sybsecurity and tempdb devices only when there are no others.
	 *       File extension: the most common one among the data devices used.</li>
	 * </ul>
	 * @return null if OK, else what is wrong and what to do (install stops before creating anything)
	 */
	private String chooseAseDeviceDir(LgSession admin, String db, Size size)
	throws SQLException
	{
		if (_lg.getCmd().hasOption("ase-data-dev") || _lg.getCmd().hasOption("ase-log-dev"))
			return null;

		String devDir = _lg.getCmd().getOptionValue("ase-device-dir");
		if (devDir != null)
		{
			_aseDataDir = _aseLogDir = devDir.replaceAll("[/\\\\]+$", "");
			_aseDataSep = _aseLogSep = (devDir.contains("\\") && !devDir.contains("/")) ? "\\" : "/";
			return null;
		}

		// Read the disk devices (cntrltype 0, not dump devices), skip raw partitions: [name, dir, separator, extension, isLog, isSystem]
		List<String>   all     = new ArrayList<>();
		List<Object[]> devices = new ArrayList<>();
		for (Object[] row : admin.query("select name, phyname from master..sysdevices where cntrltype = 0 order by vdevno"))
		{
			String name = String.valueOf(row[0]).trim();
			String phys = String.valueOf(row[1]).trim();
			all.add(name + " = " + phys);
			if (phys.startsWith("/dev/") || phys.startsWith("\\\\.\\")) // raw partition (Unix /dev/..., Windows \\.\...)
				continue;
			int p = Math.max(phys.lastIndexOf('/'), phys.lastIndexOf('\\'));
			if (p <= 0)
				continue;
			String  file     = phys.substring(p + 1);
			String  ext      = file.lastIndexOf('.') > 0 ? file.substring(file.lastIndexOf('.')) : "";
			boolean isLog    = ASE_LOG_DEVICE_NAME.matcher(name).find() || ASE_LOG_DEVICE_NAME.matcher(file).find();
			boolean isSystem = name.matches("(?i)master|sysprocsdev|systemdbdev|sybsysdb|sybmgmtdev|sybsecurity") || name.toLowerCase().contains("tempdb");
			devices.add(new Object[] {name, phys.substring(0, p), String.valueOf(phys.charAt(p)), ext, isLog, isSystem});
		}

		// Data devices: user devices first, then also the system devices, then anything with a file
		List<Object[]> dataDevs = new ArrayList<>();
		for (Object[] d : devices) if (!(Boolean) d[4] && !(Boolean) d[5]) dataDevs.add(d);
		if (dataDevs.isEmpty()) for (Object[] d : devices) if (!(Boolean) d[4]) dataDevs.add(d);
		if (dataDevs.isEmpty()) dataDevs.addAll(devices);

		List<Object[]> logDevs = new ArrayList<>();
		for (Object[] d : devices) if ((Boolean) d[4]) logDevs.add(d);

		if (dataDevs.isEmpty())
		{
			long sizeMb = Math.max(300, size.estimateMb() * 2);
			return "Can't decide where to put the ASE database '" + db + "': no file-based disk devices found in master..sysdevices "
					+ "(only raw partitions or none), so there is no directory to put new devices next to.\n"
					+ "   Devices found: " + (all.isEmpty() ? "(none)" : String.join(", ", all)) + "\n"
					+ "   Do one of these:\n"
					+ "   - give a directory ON THE ASE HOST where the load generator may create its own devices (disk init, dropped again by uninstall):\n"
					+ "        --ase-device-dir /path/on/the/ase/host\n"
					+ "   - or create devices yourself (data ~" + sizeMb + " MB, log ~" + _lg.getCmd().getOptionValue("ase-log-size-mb", "1024") + " MB for size '" + size.name + "') and name them:\n"
					+ "        --ase-data-dev <data device> --ase-log-dev <log device>\n"
					+ "   - or put it on the default device(s):  --ase-data-dev default\n"
					+ "   - or create the database yourself and use:  --use-existing-db\n"
					+ "   See doc/loadgen.md, 'Sybase ASE: where the database goes'.";
		}

		// The directory that holds most of the devices (ties: the first in vdevno order); the extension most used by the data devices
		Map<String, Integer> dataDirCount = new LinkedHashMap<>(), logDirCount = new LinkedHashMap<>(), extCount = new LinkedHashMap<>();
		for (Object[] d : dataDevs) { dataDirCount.merge((String) d[1], 1, Integer::sum); extCount.merge((String) d[3], 1, Integer::sum); }
		for (Object[] d : logDevs)  { logDirCount .merge((String) d[1], 1, Integer::sum); }
		String dataDir = null, logDir = null, ext = null;
		for (Map.Entry<String, Integer> e : dataDirCount.entrySet()) if (dataDir == null || e.getValue() > dataDirCount.get(dataDir)) dataDir = e.getKey();
		for (Map.Entry<String, Integer> e : logDirCount .entrySet()) if (logDir  == null || e.getValue() > logDirCount .get(logDir))  logDir  = e.getKey();
		for (Map.Entry<String, Integer> e : extCount    .entrySet()) if (ext     == null || e.getValue() > extCount    .get(ext))     ext     = e.getKey();
		if (logDir == null)
			logDir = dataDir;

		_aseDataDir = dataDir;
		_aseLogDir  = logDir;
		_aseDevExt  = ext;
		for (Object[] d : devices)
		{
			if (d[1].equals(dataDir)) _aseDataSep = (String) d[2];
			if (d[1].equals(logDir))  _aseLogSep  = (String) d[2];
		}

		String msg = "ASE: no device option given, so the load generator creates its own devices next to the existing ones: "
				+ "data in '" + dataDir + "' (" + dataDirCount.get(dataDir) + " data devices there), "
				+ "log in '" + logDir + "' (" + (logDirCount.isEmpty() ? "no log devices found, next to the data" : logDirCount.get(logDir) + " log devices there") + "), "
				+ "file extension '" + ext + "'. 'uninstall' drops them again (sp_dropdevice ..., dropfile: the files too). "
				+ "To choose yourself: --ase-device-dir <dir>, or --ase-data-dev/--ase-log-dev <existing devices>.";
		_logger.info(msg);
		_lg.getJournal().event("info", "msg", msg, "dataDirs", dataDirCount, "logDirs", logDirCount);
		return null;
	}

	/** ASE: disk init (the device name must not exist) */
	private void createAseDevice(LgSession admin, String name, String physName, long sizeMb)
	throws SQLException
	{
		if (admin.queryInt("select count(*) from master..sysdevices where name = ?", name) > 0)
			throw new SQLException("ASE device '" + name + "' already exists. Run 'uninstall --force' (it drops the load generator devices), or drop it by hand.");
		// directio: like a production device (not via the file system cache); skip_alloc=false: the file is allocated now, not on first write
		String sql = "disk init name = '" + name + "', physname = '" + physName + "', size = '" + sizeMb + "M', skip_alloc = false, directio = true, dsync = false";
		_logger.info("ASE: " + sql);
		try
		{
			admin.exec(sql);
		}
		catch (SQLException ex)
		{
			throw new SQLException(ex.getMessage().trim() + "\n   If the file '" + physName + "' is left from an earlier install (uninstall could not delete it), remove it on the ASE host.", ex.getSQLState(), ex.getErrorCode(), ex);
		}
		_lg.getJournal().event("object", "action", "create", "objectType", "device", "name", name, "physname", physName, "sizeMb", sizeMb);
	}

	/** ASE: drop the load generator devices (if they exist and are not used). The device FILE is left on disk. */
	private void dropAseDevices(LgSession admin)
	throws SQLException
	{
		for (String name : new String[] {ASE_DATA_DEV, ASE_LOG_DEV})
		{
			List<Object[]> rows = admin.query("select phyname from master..sysdevices where name = ?", name);
			if (rows.isEmpty())
				continue;
			String physName = String.valueOf(rows.get(0)[0]).trim();

			// First let ASE remove the file too ('dropfile', ASE 15.7 and later); if that fails, drop only the device
			boolean fileDropped;
			try
			{
				admin.exec("exec master..sp_dropdevice '" + name + "', 'dropfile'");
				fileDropped = true;
			}
			catch (SQLException ex)
			{
				_logger.info("ASE: sp_dropdevice '" + name + "', 'dropfile' failed (" + ex.getMessage().trim() + "), dropping only the device.");
				admin.exec("exec master..sp_dropdevice '" + name + "'");
				fileDropped = false;
			}

			if (fileDropped)
				_logger.info("Dropped ASE device '" + name + "' and its file: " + physName);
			else
				_logger.warn("Dropped ASE device '" + name + "', but ASE did not remove its file. It is still on the ASE host (normally not this machine), remove it by hand: " + physName);
			_lg.getJournal().event("object", "action", "drop", "objectType", "device", "name", name, "physname", physName, "fileDropped", fileDropped);
		}
	}

	private void createLogin(LgSession admin, String login, String pw, String db)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER: admin.exec("create login [" + login + "] with password = '" + pw + "', check_policy = off, default_database = [" + db + "]"); break;
		case ASE:       admin.exec("exec master..sp_addlogin '" + login + "', '" + pw + "', '" + db + "'"); break;
		case POSTGRES:
			admin.exec("create role " + login + " login password '" + pw + "' in role " + PG_ROLE);
			admin.exec("alter role " + login + " set search_path = " + PG_SCHEMA + ", public");
			break;
		}
	}

	private boolean loginExists(LgSession admin, String login)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER: return admin.queryInt("select count(*) from sys.server_principals where name = ?", login) > 0;
		case ASE:       return admin.queryInt("select count(*) from master..syslogins where name = ?", login) > 0;
		case POSTGRES:  return admin.queryInt("select count(*) from pg_roles where rolname = ?", login) > 0;
		}
		return false;
	}

	/** A login with our name is left from earlier (uninstall --keep-logins): new password, default database, role membership */
	private void resetLogin(LgSession admin, String login, String pw, String db)
	throws Exception
	{
		switch (_v)
		{
		case SQLSERVER:
			admin.exec("alter login [" + login + "] with password = '" + pw + "', check_policy = off, default_database = [" + db + "]");
			break;
		case ASE:
			admin.exec("exec master..sp_password '" + _lg.getAdminPassword() + "', '" + pw + "', '" + login + "'");
			admin.exec("exec master..sp_modifylogin '" + login + "', 'defdb', '" + db + "'");
			break;
		case POSTGRES:
			admin.exec("alter role " + login + " login password '" + pw + "'");
			admin.exec("grant " + PG_ROLE + " to " + login);
			admin.exec("alter role " + login + " set search_path = " + PG_SCHEMA + ", public");
			break;
		}
	}

	/** Make the login a user in the load generator database, with owner rights (maintenance scenarios need it) */
	private void createDbUser(LgSession s, String login)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER:
			if (s.queryInt("select count(*) from sys.database_principals where name = ?", login) == 0)
				s.exec("create user [" + login + "] for login [" + login + "]");
			s.exec("alter role db_owner add member [" + login + "]");
			break;
		case ASE:
			if (s.queryInt("select count(*) from sysalternates where suid = suser_id(?)", login) == 0)
				s.exec("exec sp_addalias '" + login + "', 'dbo'");
			break;
		case POSTGRES:
			// Member of PG_ROLE, which owns the schema and every object in it
			break;
		}
	}

	/** Update statistics on all tables (right after the fill) */
	private void updateStatistics(LgSession s)
	throws SQLException
	{
		_logger.info("Updating statistics...");
		if (_v == Vendor.POSTGRES)
		{
			s.exec("analyze");
			return;
		}
		for (String tab : TABLES)
		{
			s.exec(_v == Vendor.ASE ? "update index statistics " + tab : "update statistics " + tab);
		}
	}

	/**
	 * Run a resource script sql/{vendor}_{name}, batch by batch ('go').
	 * Created objects and the '-- FLAW:' comments are written to the journal.
	 */
	private void runScript(LgSession s, String name)
	throws Exception
	{
		String resource = _v.getSqlResourcePrefix() + name;
		_logger.info("Running script '" + resource + "'...");

		Pattern createPat = Pattern.compile("^\\s*create\\s+(table|index|procedure|function)\\s+(\\w+)(\\s+on\\s+(\\w+))?", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
		Pattern flawPat   = Pattern.compile("^\\s*--\\s*FLAW:\\s*(.*)$", Pattern.MULTILINE);

		try (InputStream in = Installer.class.getResourceAsStream(resource))
		{
			if (in == null)
				throw new Exception("Can't find resource '" + resource + "' (next to " + Installer.class.getName() + ")");

			BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			for (String batch = SqlScriptExecutor.readCommand(br); batch != null; batch = SqlScriptExecutor.readCommand(br))
			{
				// Only comments in the batch: skip
				if (batch.replaceAll("(?m)^\\s*--.*$", "").isBlank())
					continue;

				try
				{
					s.exec(batch);
				}
				catch (SQLException ex)
				{
					throw new SQLException("Problems in '" + resource + "' executing:\n" + batch + "\nCaught: " + ex.getMessage(), ex);
				}

				Matcher cm = createPat.matcher(batch);
				if (cm.find())
				{
					Matcher fm = flawPat.matcher(batch);
					_lg.getJournal().event("object", "action", "create", "objectType", cm.group(1).toLowerCase(), "name", cm.group(2),
							"table", cm.group(4), "flaw", fm.find() ? fm.group(1).trim() : null);
				}
			}
		}
	}

	//------------------------------------------------------------------
	// Data fill
	//------------------------------------------------------------------
	/**
	 * Fills the tables with generated data. The same seed gives the same data.
	 * <ul>
	 *   <li>customer 1 owns ~30% of the orders, product 1 is on ~20% of the order lines (skew)</li>
	 *   <li>orders are spread over the last 730 days, order_ts grows with order_id</li>
	 * </ul>
	 */
	private class DataFiller
	{
		private final LgSession _s;
		private final Size      _size;
		private final Random    _rnd;
		private final int       _loginCnt;
		private final double[]  _price;
		private final LocalDateTime _end   = LocalDateTime.now().withNano(0);
		private final LocalDateTime _start = _end.minusDays(730);
		private static final int BATCH = 2000;
		/** a NULL in an INT column (a plain null is sent as a VARCHAR NULL) */
		private final Object NULL_INT = new Object();

		final String[] FIRST   = {"Anna","Erik","Maria","Lars","Karin","Johan","Eva","Per","Sara","Anders","Emma","Nils","Ida","Olof","Lena","Peter","Sofia","Jonas","Elin","Mikael","John","Mary","James","Linda","Robert","Susan","David","Laura","Paul","Kate"};
		final String[] LAST    = {"Smith","Andersson","Johansson","Karlsson","Nilsson","Eriksson","Larsson","Olsson","Persson","Svensson","Gustafsson","Pettersson","Jonsson","Jansson","Hansson","Bengtsson","Lindberg","Jakobsson","Magnusson","Lindstrom","Brown","Jones","Miller","Davis","Wilson","Taylor","Moore","Clark","Lewis","Walker"};
		final String[] CITY    = {"Stockholm","Goteborg","Malmo","Uppsala","Vasteras","Orebro","Linkoping","Helsingborg","Jonkoping","Norrkoping","Lund","Umea","Gavle","Boras","Oslo","Bergen","Copenhagen","Aarhus","Helsinki","Tampere","Berlin","Hamburg","London","Manchester","New York","Boston","Chicago","Paris","Madrid","Rome"};
		final String[] COUNTRY = {"SE","SE","SE","SE","SE","NO","DK","FI","DE","UK","US"};
		final String[] WORDS   = {"red","blue","green","small","large","fast","slow","smart","basic","premium","eco","pro","mini","max","ultra","classic","modern","steel","wood","glass"};

		DataFiller(LgSession s, Size size, long seed, int loginCnt)
		{
			_s        = s;
			_size     = size;
			_rnd      = new Random(seed);
			_loginCnt = loginCnt;
			_price    = new double[size.products + 1];

			// Progress: the work is the estimated bytes per table (as in Size.estimateMb), rows x bytes per row
			double lines = size.orders * 3.0;
			_workTotal = size.customers * 200.0 + size.products * 250.0 + size.warehouses * size.products * 40.0
					+ size.orders * 100.0 + lines * 40.0 + size.orders * 0.9 * 50.0 + size.orders * 0.85 * 50.0
					+ size.auditRows * 150.0 + size.events * 2_100.0 + lines * 0.9 * 50.0;
		}

		private final double _workTotal;
		private double       _workDone;
		private final long   _fillStartMs = System.currentTimeMillis();
		private long         _nextProgressMs = _fillStartMs + 30_000;

		private long _nextLogTruncMs = System.currentTimeMillis() + 10_000;

		/**
		 * ASE: truncate the log every 10 s during the fill. 'trunc log on chkpt' truncates only on the server's own checkpoint
		 * (every ~60-70 s; a manual 'checkpoint' does not truncate), and the fill writes ~9 MB/s (ASE 16, medium: ~490 MB peak without this).
		 */
		private void truncateLogAse(boolean now)
		throws SQLException
		{
			// Only in a database the install created: an existing one may have log dumps, that a truncate would break
			if (_v != Vendor.ASE || _lg.getCmd().hasOption("use-existing-db") || (!now && System.currentTimeMillis() < _nextLogTruncMs))
				return;
			boolean autoCommit = _s.getConnection().getAutoCommit();
			if (!autoCommit)
				_s.getConnection().setAutoCommit(true); // dump transaction is not allowed in a transaction
			_s.exec("dump transaction " + _lg.getDbname() + " with truncate_only");
			if (!autoCommit)
				_s.getConnection().setAutoCommit(false);
			_nextLogTruncMs = System.currentTimeMillis() + 10_000;
		}

		/** Every 30 s: how far the data fill is, and when it (and the install) should be done, extrapolated from the time so far */
		private void progress(double work)
		{
			_workDone += work;
			long now = System.currentTimeMillis();
			if (now < _nextProgressMs || _workDone <= 0)
				return;
			_nextProgressMs = now + 30_000;
			double pct    = Math.min(0.99, _workDone / _workTotal);
			long   fillMs = (long) ((now - _fillStartMs) / pct);
			java.time.format.DateTimeFormatter hms = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");
			LocalDateTime fillEnd    = LocalDateTime.now().plusNanos((fillMs - (now - _fillStartMs)) * 1_000_000L);
			LocalDateTime installEnd = fillEnd.plusNanos(fillMs * 15 / 100 * 1_000_000L); // indexes + statistics: ~15% of the fill time (measured on ASE medium)
			_logger.info(String.format("Install progress: data fill ~%d%% done, fill ready ~%s, install ready ~%s", (int) (pct * 100), fillEnd.format(hms), installEnd.format(hms)));
		}

		private Timestamp ts(LocalDateTime ldt)       { return Timestamp.valueOf(ldt); }
		private Timestamp randomTs()                  { return ts(_start.plusSeconds((long) (_rnd.nextDouble() * 730L * 86400L))); }
		private String    pick(String[] arr)          { return arr[_rnd.nextInt(arr.length)]; }
		private String    text(int minLen, int maxLen)
		{
			int len = minLen + _rnd.nextInt(maxLen - minLen + 1);
			StringBuilder sb = new StringBuilder(len + 10);
			while (sb.length() < len)
				sb.append(pick(WORDS)).append(' ');
			return sb.substring(0, len);
		}

		/** A batched insert: add rows with add(...), it flushes and commits every BATCH rows */
		private class Inserter
		implements AutoCloseable
		{
			final String _table;
			final PreparedStatement _ps;
			final long _total;
			final int  _rowBytes;
			long _rows, _nextLog;

			Inserter(String table, String cols, long expectedRows)
			throws SQLException
			{
				_table = table;
				_total = expectedRows;
				switch (table)
				{
				case "customer":      _rowBytes = 200;   break;
				case "product":       _rowBytes = 250;   break;
				case "orders":        _rowBytes = 100;   break;
				case "payment":       _rowBytes = 50;    break;
				case "shipment":      _rowBytes = 50;    break;
				case "audit_log":     _rowBytes = 150;   break;
				case "session_event": _rowBytes = 2_100; break;
				default:              _rowBytes = 40;    break; // stock, order_line, small tables
				}
				int colCnt = cols.split(",").length;
				String q = "?" + ",?".repeat(colCnt - 1);
				_ps = _s.getConnection().prepareStatement("insert into " + table + " (" + cols + ") values (" + q + ")");
				_nextLog = logStep();
			}

			/** progress every 10% (or every 100000 rows when the total is not known) */
			private long logStep()
			{
				return _total > 0 ? Math.max(_total / 10, BATCH) : 100_000;
			}

			void add(Object... vals)
			throws SQLException
			{
				for (int i=0; i<vals.length; i++)
				{
					if      (vals[i] == null)     _ps.setNull  (i+1, java.sql.Types.VARCHAR);
					else if (vals[i] == NULL_INT) _ps.setNull  (i+1, java.sql.Types.INTEGER);
					else                          _ps.setObject(i+1, vals[i]);
				}
				_ps.addBatch();
				_rows++;
				progress(_rowBytes);
				if (_rows % BATCH == 0)
					flush();
				if (_rows >= _nextLog)
				{
					_logger.info("   " + _table + ": " + _rows + (_total > 0 ? " of " + _total : "") + " rows");
					_nextLog += logStep();
				}
			}

			void flush()
			throws SQLException
			{
				_ps.executeBatch();
				_s.getConnection().commit();
				truncateLogAse(false);
			}

			@Override
			public void close()
			throws SQLException
			{
				flush();
				_ps.close();
			}
		}

		void fill()
		throws SQLException
		{
			_s.getConnection().setAutoCommit(false);
			try
			{
				fillBase();
				fillOrders();
				fillAuditAndEvents();
				_s.getConnection().setAutoCommit(true);
				fillDw();
				fillCounters();
			}
			finally
			{
				_s.rollbackQuietly();
			}
		}

		private void fillBase()
		throws SQLException
		{
			_logger.info("Filling category, warehouse, customer, product, stock...");
			try (Inserter ins = new Inserter("category", "category_id, name, parent_id", _size.categories))
			{
				for (int i=1; i<=_size.categories; i++)
					ins.add(i, "Category " + i, i <= 10 ? NULL_INT : (Object) (1 + (i % 10)));
			}
			try (Inserter ins = new Inserter("warehouse", "warehouse_id, name, city", _size.warehouses))
			{
				for (int i=1; i<=_size.warehouses; i++)
					ins.add(i, "Warehouse " + i, CITY[i % CITY.length]);
			}
			try (Inserter ins = new Inserter("customer", "customer_id, first_name, last_name, email, phone, city, country_code, segment, vip_flag, created_ts, notes", _size.customers))
			{
				for (int i=1; i<=_size.customers; i++)
				{
					String fn = pick(FIRST), ln = (_rnd.nextInt(10) == 0) ? "Smith" : pick(LAST);
					int    seg = _rnd.nextInt(100);
					ins.add(i, fn, ln, (fn + "." + ln + "." + i + "@example.com").toLowerCase(),
							"07" + (10_000_000 + _rnd.nextInt(90_000_000)), pick(CITY), pick(COUNTRY),
							seg < 80 ? "RETAIL" : seg < 95 ? "BUSINESS" : "PARTNER",
							_rnd.nextInt(100) == 0 ? "Y" : null,
							randomTs(), _rnd.nextInt(10) == 0 ? text(20, 200) : null);
				}
			}
			try (Inserter ins = new Inserter("product", "product_id, category_id, name, description, price, status, created_ts", _size.products))
			{
				for (int i=1; i<=_size.products; i++)
				{
					_price[i] = Math.round((1 + _rnd.nextDouble() * 1999) * 100) / 100.0;
					int st = _rnd.nextInt(100);
					ins.add(i, 1 + _rnd.nextInt(_size.categories), "Product " + i + " " + pick(WORDS) + " " + pick(WORDS),
							text(50, 300), BigDecimal.valueOf(_price[i]), st < 99 ? "ACTIVE" : (st == 99 && _rnd.nextBoolean()) ? "RETIRED" : "DRAFT", randomTs());
				}
			}
			try (Inserter ins = new Inserter("stock", "warehouse_id, product_id, qty, reserved, last_upd_ts", (long) _size.warehouses * _size.products))
			{
				Timestamp now = ts(_end);
				for (int w=1; w<=_size.warehouses; w++)
					for (int p=1; p<=_size.products; p++)
						ins.add(w, p, 100_000 + _rnd.nextInt(1000), 0, now);
			}
		}

		private void fillOrders()
		throws SQLException
		{
			_logger.info("Filling orders, order_line, payment, shipment...");
			long spanSec = 730L * 86400L;
			try (Inserter ord  = new Inserter("orders",     "order_id, customer_id, order_ts, status, total_amount, warehouse_id, ship_city, comment_txt", _size.orders);
			     Inserter line = new Inserter("order_line", "order_id, line_no, product_id, qty, price", 0);
			     Inserter pay  = new Inserter("payment",    "payment_id, order_id, pay_ts, amount, method", 0);
			     Inserter shp  = new Inserter("shipment",   "order_id, ship_ts, carrier, status", 0))
			{
				for (int o=1; o<=_size.orders; o++)
				{
					LocalDateTime orderTs = _start.plusSeconds((long) ((double) o / _size.orders * spanSec) - _rnd.nextInt(600));
					int customer = _rnd.nextInt(100) < 30 ? 1 : 2 + _rnd.nextInt(_size.customers - 1);

					double pos = (double) o / _size.orders;   // 0..1, newest last
					String status = pos > 0.98 ? "NEW" : pos > 0.95 ? "PAID" : (_rnd.nextInt(100) < 5 ? "CANCELLED" : "SHIPPED");

					int lines = 1 + _rnd.nextInt(5);
					double total = 0;
					for (int l=1; l<=lines; l++)
					{
						int product = _rnd.nextInt(100) < 20 ? 1 : 1 + _rnd.nextInt(_size.products);
						int qty     = 1 + _rnd.nextInt(5);
						total += qty * _price[product];
						line.add(o, l, product, qty, BigDecimal.valueOf(_price[product]));
					}
					BigDecimal amount = BigDecimal.valueOf(Math.round(total * 100) / 100.0);
					ord.add(o, customer, ts(orderTs), status, amount, 1 + _rnd.nextInt(_size.warehouses), pick(CITY),
							_rnd.nextInt(5) == 0 ? text(10, 150) : null);

					if (!status.equals("NEW") && !status.equals("CANCELLED"))
						pay.add(o, o, ts(orderTs.plusMinutes(1 + _rnd.nextInt(120))), amount, _rnd.nextBoolean() ? "CARD" : _rnd.nextBoolean() ? "INVOICE" : "SWISH");
					if (status.equals("SHIPPED"))
						shp.add(o, ts(orderTs.plusHours(4 + _rnd.nextInt(72))), _rnd.nextBoolean() ? "PostNord" : "DHL", "DELIVERED");
				}
			}
		}

		private void fillAuditAndEvents()
		throws SQLException
		{
			_logger.info("Filling audit_log, session_event...");
			String[] actions = {"NEW_ORDER", "PAYMENT", "LOGIN", "LOGOUT", "SEARCH", "UPDATE_PROFILE"};
			try (Inserter ins = new Inserter("audit_log", "log_ts, login_name, action, object_id, details", _size.auditRows))
			{
				for (int i=1; i<=_size.auditRows; i++)
					ins.add(randomTs(), LoadGen.LOGIN_PREFIX + (1 + _rnd.nextInt(_loginCnt)), pick(actions), _rnd.nextInt(_size.orders), text(30, 200));
			}
			String[] types = {"PAGE_VIEW", "SEARCH", "CART_ADD", "CART_REMOVE", "CHECKOUT"};
			try (Inserter ins = new Inserter("session_event", "event_id, customer_id, event_ts, event_type, payload", _size.events))
			{
				for (int i=1; i<=_size.events; i++)
					ins.add((long) i, 1 + _rnd.nextInt(_size.customers), randomTs(), pick(types), text(1000, 3000));
			}
		}

		private void fillDw()
		throws SQLException
		{
			_logger.info("Filling dim_date, dim_customer, dim_product, fact_sales...");
			_s.getConnection().setAutoCommit(false);
			try (Inserter ins = new Inserter("dim_date", "date_key, cal_date, cal_year, cal_quarter, cal_month, month_name, weekday", 1200))
			{
				String[] months = {"January","February","March","April","May","June","July","August","September","October","November","December"};
				for (LocalDate d = _start.toLocalDate().minusDays(30); d.isBefore(_end.toLocalDate().plusDays(400)); d = d.plusDays(1))
				{
					ins.add(d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth(), Date.valueOf(d), d.getYear(),
							(d.getMonthValue() - 1) / 3 + 1, d.getMonthValue(), months[d.getMonthValue() - 1], d.getDayOfWeek().getValue());
				}
			}
			_s.getConnection().setAutoCommit(true);

			_s.exec("insert into dim_customer (customer_key, segment, country_code, city) select customer_id, segment, country_code, city from customer");
			_s.exec("insert into dim_product (product_key, category_name, product_name) select p.product_id, c.name, p.name from product p join category c on c.category_id = p.category_id");

			// 90% of the orders are already in the fact table, the ETL scenario loads the rest (and new orders)
			int loadTo = (int) (_size.orders * 0.9);
			int chunk  = 100_000;
			for (int from = 0; from < loadTo; from += chunk)
			{
				int to = Math.min(from + chunk, loadTo);
				_s.exec(EtlFactSales.insertSql(_v, from, to));
				progress((to - from) * 3.0 * 0.9 * 50.0);
				truncateLogAse(true); // each chunk is one statement of ~100 MB log: truncate after every one
			}
			_s.update("insert into etl_watermark (name, last_order_id) values ('fact_sales', ?)", loadTo);
		}

		private void fillCounters()
		throws SQLException
		{
			_s.update("insert into counter_table (name, next_val) values ('order',   ?)", (long) _size.orders + 1);
			_s.update("insert into counter_table (name, next_val) values ('payment', ?)", (long) _size.orders + 1);
			_s.update("insert into counter_table (name, next_val) values ('event',   ?)", (long) _size.events + 1);
			_s.update("insert into counter_table (name, next_val) values ('hotspot', 0)");
		}
	}

	//------------------------------------------------------------------
	// UNINSTALL
	//------------------------------------------------------------------
	public int uninstall()
	throws Exception
	{
		boolean force      = _lg.getCmd().hasOption("force");
		boolean keepDb     = _lg.getCmd().hasOption("keep-database");
		boolean keepLogins = _lg.getCmd().hasOption("keep-logins");
		String  db         = _lg.getDbname();

		if (keepDb && keepLogins)
		{
			System.out.println("ERROR: --keep-database and --keep-logins together would drop nothing.");
			return LoadGen.EXIT_USAGE;
		}

		String runId = UUID.randomUUID().toString().substring(0, 8);
		Journal journal = new Journal(_lg.getSrvName(), "uninstall", runId);
		_lg.setJournal(journal);
		boolean dbCreatedByUs = true;

		try (LgSession admin = _lg.connectAdmin(null))
		{
			admin.setQueryTimeout(0);
			journal.event("header", "kind", "uninstall", "runId", runId, "server", _lg.getSrvName(), "vendor", _v.getName(),
					"dbmsVersion", admin.getConnection().getMetaData().getDatabaseProductVersion(), "dbname", db,
					"force", force, "keepDatabase", keepDb, "keepLogins", keepLogins);

			// Which logins: from the marker table, or (--force) all logins named dbxt_user_#
			boolean dbExists = databaseExists(admin, db);
			Map<String, String> info = dbExists ? readInstallInfo(db) : null;
			boolean marker   = info != null;
			List<String> logins = new ArrayList<>();
			if (marker)
				for (Map.Entry<String, String> e : info.entrySet())
					if (e.getKey().startsWith("login."))
						logins.add(e.getValue());

			if (!marker && !force)
			{
				String msg = dbExists
						? "Database '" + db + "' exists but has no " + LoadGen.PRODUCT_MARKER + " marker table. Not touching it. Use --force if it really is a broken load generator install."
						: "Database '" + db + "' does not exist (nothing to uninstall). Use --force to remove left-over " + LoadGen.LOGIN_PREFIX + "# logins.";
				System.out.println("ERROR: " + msg);
				journal.event("error", "msg", msg);
				return LoadGen.EXIT_NOT_AVAILABLE;
			}
			if (force)
			{
				for (String login : listLoadGenLogins(admin))
					if (!logins.contains(login))
						logins.add(login);
			}

			// Did WE create the database? Without a marker (--force) only a database with the default name counts as ours.
			dbCreatedByUs = marker ? !"false".equals(info.get("databaseCreated")) : LoadGen.DEFAULT_DBNAME.equals(db);

			//--- The database (or, in a database we did not create, only our objects)
			if (!keepDb && dbExists)
			{
				if (dbCreatedByUs)
				{
					dropDatabase(admin, db);
					journal.event("object", "action", "drop", "objectType", "database", "name", db);
					_logger.info("Dropped database '" + db + "'.");
					if (_v == Vendor.ASE)
						dropAseDevices(admin);
				}
				else if (marker)
				{
					dropOwnObjects(admin, db, logins);
				}
				else
				{
					_logger.warn("Database '" + db + "' has no marker and is not named '" + LoadGen.DEFAULT_DBNAME + "': not dropping anything in it.");
				}
			}
			else if (!keepDb && _v == Vendor.ASE && force)
			{
				dropAseDevices(admin);
			}

			//--- The logins
			if (!keepLogins)
			{
				// The database stays: remove the users/aliases of the logins first, and note it in the install info
				if (keepDb && marker)
				{
					try (LgSession s = _lg.connectAdmin(db))
					{
						for (String login : logins)
							dropDbUser(s, login);
						if (_v == Vendor.POSTGRES)
							s.exec("set search_path = " + PG_SCHEMA);
						s.update("delete from dbxt_loadgen_info where k like 'login.%' or k = 'loginsDropped'");
						s.update("insert into dbxt_loadgen_info (k, v) values ('loginsDropped', ?)", Journal.now());
					}
				}
				for (String login : logins)
				{
					if (!LOGIN_NAME_PATTERN.matcher(login).matches())
					{
						_logger.warn("Skipping login '" + login + "', it does not look like a load generator login.");
						continue;
					}
					try
					{
						dropLogin(admin, login);
						_lg.removePassword(login);
						journal.event("object", "action", "drop", "objectType", "login", "name", login);
					}
					catch (SQLException ex)
					{
						_logger.warn("Problems dropping login '" + login + "'. Caught: " + ex.getMessage());
						journal.event("error", "msg", "Problems dropping login '" + login + "': " + ex.getMessage());
					}
				}
				_logger.info("Dropped " + logins.size() + " logins.");
			}
			else if (dbCreatedByUs && dbExists && !keepDb)
			{
				// The logins stay, their default database is gone
				for (String login : logins)
				{
					if (_v == Vendor.SQLSERVER) admin.exec("alter login [" + login + "] with default_database = [master]");
					if (_v == Vendor.ASE)       admin.exec("exec master..sp_modifylogin '" + login + "', 'defdb', 'master'");
				}
				_logger.info("Kept " + logins.size() + " logins (default database set to master), their passwords stay in '" + _lg.getPasswdFile() + "'.");
			}

			//--- Postgres: the role that owned the objects
			if (_v == Vendor.POSTGRES && !keepDb && admin.queryInt("select count(*) from pg_roles where rolname = ?", PG_ROLE) > 0)
			{
				if (databaseExists(admin, db))
					admin.exec("revoke connect, temporary on database " + db + " from " + PG_ROLE);
				admin.exec("drop role " + PG_ROLE);
				journal.event("object", "action", "drop", "objectType", "role", "name", PG_ROLE);
			}
		}

		String what = keepDb     ? "the logins (the database is kept)"
		            : keepLogins ? "the database/objects (the logins are kept)"
		            : !dbCreatedByUs ? "the load generator objects and logins (the database was not created by the load generator and is kept)"
		            : "everything";
		journal.event("summary", "result", "UNINSTALLED", "dropped", what);
		System.out.println("Uninstalled " + what + " for '" + db + "' on '" + _lg.getSrvName() + "'. Journal: " + journal.getFile());
		return LoadGen.EXIT_OK;
	}

	/** In a database we did NOT create: drop only our tables, procedures (Postgres: the schema) and the users of our logins */
	private void dropOwnObjects(LgSession admin, String db, List<String> logins)
	throws SQLException
	{
		// Sessions of our logins may hold locks on the tables
		for (String login : logins)
			killSessions(admin, login);

		try (LgSession s = _lg.connectAdmin(db))
		{
			s.setQueryTimeout(0);
			if (_v == Vendor.POSTGRES)
			{
				s.exec("drop schema " + PG_SCHEMA + " cascade");
				_lg.getJournal().event("object", "action", "drop", "objectType", "schema", "name", PG_SCHEMA);
			}
			else
			{
				for (String name : PROCEDURES)
					if (s.queryInt("select count(*) from sysobjects where name = ? and type = 'P'", name) > 0)
					{
						s.exec("drop procedure " + name);
						_lg.getJournal().event("object", "action", "drop", "objectType", "procedure", "name", name);
					}
				for (String name : TABLES)
					if (s.queryInt("select count(*) from sysobjects where name = ? and type = 'U'", name) > 0)
					{
						s.exec("drop table " + name);
						_lg.getJournal().event("object", "action", "drop", "objectType", "table", "name", name);
					}
				for (String login : logins)
					dropDbUser(s, login);
			}
		}
		catch (SQLException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new SQLException("Problems dropping the load generator objects in '" + db + "'. Caught: " + ex, ex);
		}
		_logger.info("Dropped the load generator objects in '" + db + "' (the database itself was not created by the load generator and is kept).");
	}

	/** Remove the database user / alias of a login (SQL Server, ASE). Postgres: nothing, the login is only a member of the owner role */
	private void dropDbUser(LgSession s, String login)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER:
			if (s.queryInt("select count(*) from sys.database_principals where name = ?", login) > 0)
				s.exec("drop user [" + login + "]");
			break;
		case ASE:
			if (s.queryInt("select count(*) from sysalternates where suid = suser_id(?)", login) > 0)
				s.exec("exec sp_dropalias '" + login + "'");
			break;
		case POSTGRES:
			break;
		}
	}

	private void killSessions(LgSession admin, String login)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER:
			for (Object[] row : admin.query("select session_id from sys.dm_exec_sessions where login_name = ?", login))
				admin.exec("kill " + row[0]);
			break;
		case ASE:
			for (Object[] row : admin.query("select spid from master..sysprocesses where suid = suser_id(?)", login))
				admin.exec("kill " + row[0]);
			break;
		case POSTGRES:
			admin.query("select pg_terminate_backend(pid) from pg_stat_activity where usename = ?", login);
			break;
		}
	}

	private List<String> listLoadGenLogins(LgSession admin)
	throws SQLException
	{
		String sql;
		switch (_v)
		{
		case SQLSERVER: sql = "select name    from sys.server_principals where type = 'S' and name like 'dbxt[_]user[_]%'"; break;
		case ASE:       sql = "select name    from master..syslogins where name like 'dbxt[_]user[_]%'"; break;
		default:        sql = "select rolname from pg_roles where rolname like 'dbxt\\_user\\_%'"; break;
		}
		List<String> list = new ArrayList<>();
		for (Object[] row : admin.query(sql))
			if (LOGIN_NAME_PATTERN.matcher((String) row[0]).matches())
				list.add((String) row[0]);
		return list;
	}

	private void dropDatabase(LgSession admin, String db)
	throws SQLException
	{
		switch (_v)
		{
		case SQLSERVER:
			admin.exec("alter database [" + db + "] set single_user with rollback immediate");
			admin.exec("drop database [" + db + "]");
			break;
		case ASE:
			for (Object[] row : admin.query("select spid from master..sysprocesses where dbid = db_id(?) and spid != @@spid", db))
				admin.exec("kill " + row[0]);
			admin.exec("drop database " + db);
			break;
		case POSTGRES:
			admin.query("select pg_terminate_backend(pid) from pg_stat_activity where datname = ? and pid <> pg_backend_pid()", db);
			admin.exec("drop database " + db);
			break;
		}
	}

	private void dropLogin(LgSession admin, String login)
	throws SQLException
	{
		killSessions(admin, login);
		switch (_v)
		{
		case SQLSERVER: admin.exec("drop login [" + login + "]"); break;
		case ASE:       admin.exec("exec master..sp_droplogin '" + login + "'"); break;
		case POSTGRES:  admin.exec("drop role " + login); break;
		}
	}

	//------------------------------------------------------------------
	// STATUS
	//------------------------------------------------------------------
	public int status()
	throws Exception
	{
		LgSession s;
		try
		{
			s = _lg.connectAdmin(_lg.getDbname());
		}
		catch (Exception ex)
		{
			try
			{
				s = _lg.connectUser(LoadGen.LOGIN_PREFIX + "1", LoadGen.APPNAME_PREFIX + ":status", null);
			}
			catch (Exception ex2)
			{
				System.out.println("Can't connect to '" + _lg.getDbname() + "' on '" + _lg.getSrvName() + "' (not installed?).");
				System.out.println("   as admin:           " + ex.getMessage());
				System.out.println("   as " + LoadGen.LOGIN_PREFIX + "1:        " + ex2.getMessage());
				return LoadGen.EXIT_NOT_AVAILABLE;
			}
		}

		try (LgSession ss = s)
		{
			if (_v == Vendor.POSTGRES)
				ss.exec("set search_path = " + PG_SCHEMA);

			System.out.println();
			System.out.println("Server '" + _lg.getSrvName() + "' (" + _v.getName() + " " + ss.getConnection().getMetaData().getDatabaseProductVersion().replace('\n', ' ') + "), database '" + _lg.getDbname() + "'");
			System.out.println();
			System.out.println("Install info:");
			for (Object[] row : ss.query("select k, v from dbxt_loadgen_info where k not like 'login.%' order by k"))
				System.out.println(String.format("   %-16s %s", row[0], row[1]));

			System.out.println();
			System.out.println("Row counts:");
			for (String tab : new String[] {"customer", "product", "stock", "orders", "order_line", "payment", "shipment", "audit_log", "session_event", "fact_sales"})
				System.out.println(String.format(Locale.US, "   %-16s %,d", tab, ((Number) ss.queryValue("select count(*) from " + tab)).longValue()));

			long cust1 = ((Number) ss.queryValue("select count(*) from orders where customer_id = 1")).longValue();
			long all   = ((Number) ss.queryValue("select count(*) from orders")).longValue();
			System.out.println();
			System.out.println(String.format(Locale.US, "Skew: customer 1 has %,d of %,d orders (%.1f%%)", cust1, all, all == 0 ? 0.0 : cust1 * 100.0 / all));

			System.out.println();
			System.out.println("Last runs:");
			String top = _v == Vendor.POSTGRES ? "" : "top 10 ";
			String lim = _v == Vendor.POSTGRES ? " limit 10" : "";
			for (Object[] row : ss.query("select " + top + "run_id, start_ts, end_ts, intensity, threads, exec_count, error_count from dbxt_loadgen_runlog order by start_ts desc" + lim))
				System.out.println(String.format("   %s  %s - %s  intensity=%s threads=%s execs=%s errors=%s", row[0], row[1], row[2], row[3], row[4], row[5], row[6]));
			System.out.println();
		}
		return LoadGen.EXIT_OK;
	}
}
