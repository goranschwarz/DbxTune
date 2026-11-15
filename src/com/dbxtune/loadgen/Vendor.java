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

/**
 * The DBMS vendors the load generator can drive.
 * <p>
 * Only the connection basics live here. Vendor differences in SQL are written in-line where the SQL is used.
 */
public enum Vendor
{
	SQLSERVER("sqlserver", 1433, "sa",       "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
	ASE      ("ase",       5000, "sa",       "com.sybase.jdbc42.jdbc.SybDriver"),
	POSTGRES ("postgres",  5432, "postgres", "org.postgresql.Driver"),
	ORACLE   ("oracle",    1521, "system",   "oracle.jdbc.OracleDriver"),
	MYSQL    ("mysql",     3306, "root",     "com.mysql.cj.jdbc.Driver"),
	DB2      ("db2",      50000, "db2inst1", "com.ibm.db2.jcc.DB2Driver");

	private final String _name;
	private final int    _defaultPort;
	private final String _defaultAdminUser;
	private final String _driverClass;

	Vendor(String name, int defaultPort, String defaultAdminUser, String driverClass)
	{
		_name             = name;
		_defaultPort      = defaultPort;
		_defaultAdminUser = defaultAdminUser;
		_driverClass      = driverClass;
	}

	public String getName()             { return _name; }
	public int    getDefaultPort()      { return _defaultPort; }
	public String getDefaultAdminUser() { return _defaultAdminUser; }
	public String getDriverClass()      { return _driverClass; }

	/** The resource prefix of the install scripts, for example "sql/sqlserver_" */
	public String getSqlResourcePrefix() { return "sql/" + _name + "_"; }

	/**
	 * Build a JDBC URL
	 * @param host
	 * @param port
	 * @param dbname  null = the server's default database (postgres: the 'postgres' database). Oracle: the SERVICE name (the PDB), not the schema.
	 *                DB2: the DATABASE from -S host:port/DBNAME, not the schema
	 */
	public String getUrl(String host, int port, String dbname)
	{
		switch (this)
		{
		case SQLSERVER: return "jdbc:sqlserver://" + host + ":" + port + (dbname == null ? "" : ";databaseName=" + dbname) + ";encrypt=true;trustServerCertificate=true";
		case ASE:       return "jdbc:sybase:Tds:"  + host + ":" + port + (dbname == null ? "" : "/" + dbname);
		case POSTGRES:  return "jdbc:postgresql://" + host + ":" + port + "/" + (dbname == null ? "postgres" : dbname);
		case ORACLE:    return "jdbc:oracle:thin:@//" + host + ":" + port + "/" + dbname;
		case MYSQL:     return "jdbc:mysql://" + host + ":" + port + "/" + (dbname == null ? "" : dbname);
		case DB2:       return "jdbc:db2://" + host + ":" + port + "/" + dbname;
		}
		throw new IllegalStateException("Unknown vendor " + this);
	}

	/** SQL that returns the server's id for this session (spid / backend pid / SID) */
	public String getSessionIdSql()
	{
		switch (this)
		{
		case POSTGRES: return "select pg_backend_pid()";
		case ORACLE:   return "select to_number(sys_context('USERENV', 'SID')) from dual";
		case MYSQL:    return "select connection_id()";                                         // = performance_schema.threads.PROCESSLIST_ID
		case DB2:      return "select mon_get_application_handle() from sysibm.sysdummy1";      // MON_GET_CONNECTION.APPLICATION_HANDLE
		default:       return "select @@spid";
		}
	}

	public static Vendor fromString(String str)
	{
		if (str == null)
			return null;
		String s = str.trim().toLowerCase();
		for (Vendor v : values())
			if (v._name.equals(s))
				return v;
		if (s.equals("mssql") || s.equals("sqlsrv"))        return SQLSERVER;
		if (s.equals("sybase") || s.equals("syb"))          return ASE;
		if (s.equals("pg") || s.equals("postgresql"))       return POSTGRES;
		if (s.equals("ora"))                                return ORACLE;
		if (s.equals("my"))                                 return MYSQL;
		if (s.equals("db2luw") || s.equals("luw"))          return DB2;
		return null;
	}
}
