/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dbms;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.SwingUtils;


public class PostgresConfig
extends DbmsConfigAbstract 
{
//	FIXME...
	private static final long serialVersionUID = 1L;

	/** Log4j logging. */
	private static Logger _logger = Logger.getLogger(PostgresConfig.class);

	private boolean   _offline   = false;
	private boolean   _hasGui    = false;
	private Timestamp _timestamp = null;

	private static final String CONTEXT_TOOLTIP = 
			  "<html>"
			+ "There are several possible values of <code>context</code>. In order of decreasing difficulty of changing the setting, they are:"
			+ "<ul>"
			+ "  <li>internal   - These settings cannot be changed directly; they reflect internally determined values. Some of them may be adjustable by rebuilding the server with different configuration options, or by changing options supplied to initdb.</li>"
			+ "  <li>postmaster - These settings can only be applied when the server starts, so any change requires restarting the server. Values for these settings are typically stored in the postgresql.conf file, or passed on the command line when starting the server. Of course, settings with any of the lower context types can also be set at server start time.</li>"
			+ "  <li>sighup     - Changes to these settings can be made in postgresql.conf without restarting the server. Send a SIGHUP signal to the postmaster to cause it to re-read postgresql.conf and apply the changes. The postmaster will also forward the SIGHUP signal to its child processes so that they all pick up the new value.</li>"
			+ "  <li>backend    - Changes to these settings can be made in postgresql.conf without restarting the server; they can also be set for a particular session in the connection request packet (for example, via libpq's PGOPTIONS environment variable). However, these settings never change in a session after it is started. If you change them in postgresql.conf, send a SIGHUP signal to the postmaster to cause it to re-read postgresql.conf. The new values will only affect subsequently-launched sessions.</li>"
			+ "  <li>superuser  - These settings can be set from postgresql.conf, or within a session via the SET command; but only superusers can change them via SET. Changes in postgresql.conf will affect existing sessions only if no session-local value has been established with SET.</li>"
			+ "  <li>user       - These settings can be set from postgresql.conf, or within a session via the SET command. Any user is allowed to change his session-local value. Changes in postgresql.conf will affect existing sessions only if no session-local value has been established with SET.</li>"
			+ "</ul>"
			+ "</html>";

	
//	1> select x.name, x.setting, x.unit, x.category, x.short_desc, x.extra_desc, x.context, x.vartype, x.source, x.min_val, x.max_val, x.enumvals, x.boot_val, x.reset_val
//	2> from pg_catalog.pg_settings x
//	RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table
//	RS> ---- ---------- ---------------------- ----------------- ------------
//	RS> 1    name       java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 2    setting    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 3    unit       java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 4    category   java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 5    short_desc java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 6    extra_desc java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 7    context    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 8    vartype    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 9    source     java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 10   min_val    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 11   max_val    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 12   enumvals   java.sql.Types.ARRAY   _text             pg_settings 
//	RS> 13   boot_val   java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	RS> 14   reset_val  java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//	+-----------------------------------+------------------+------+-----------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------+-------+--------------------+---------+------------+------------------------------------------------------------------------------+-----------------+------------------------------------------------------------------------------+
//	|name                               |setting           |unit  |category                                                         |short_desc                                                                                                                   |extra_desc                                                                                                                                                                                                                                                                                                                                                       |context   |vartype|source              |min_val  |max_val     |enumvals                                                                      |boot_val         |reset_val                                                                     |
//	+-----------------------------------+------------------+------+-----------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------+-------+--------------------+---------+------------+------------------------------------------------------------------------------+-----------------+------------------------------------------------------------------------------+
//	|allow_system_table_mods            |off               |(NULL)|Developer Options                                                |Allows modifications of the structure of system tables.                                                                      |(NULL)                                                                                                                                                                                                                                                                                                                                                           |postmaster|bool   |default             |(NULL)   |(NULL)      |(NULL)                                                                        |off              |off                                                                           |
//	|application_name                   |                  |(NULL)|Reporting and Logging / What to Log                              |Sets the application name to be reported in statistics and logs.                                                             |(NULL)                                                                                                                                                                                                                                                                                                                                                           |user      |string |default             |(NULL)   |(NULL)      |(NULL)                                                                        |                 |                                                                              |
//	|archive_command                    |(disabled)        |(NULL)|Write-Ahead Log / Archiving                                      |Sets the shell command that will be called to archive a WAL file.                                                            |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |string |configuration file  |(NULL)   |(NULL)      |(NULL)                                                                        |                 |test ! -f /var/lib/pgsql/9.3/archive/%f && cp %p /var/lib/pgsql/9.3/archive/%f|
//	|archive_mode                       |off               |(NULL)|Write-Ahead Log / Archiving                                      |Allows archiving of WAL files using archive_command.                                                                         |(NULL)                                                                                                                                                                                                                                                                                                                                                           |postmaster|bool   |configuration file  |(NULL)   |(NULL)      |(NULL)                                                                        |off              |off                                                                           |
//	|archive_timeout                    |0                 |s     |Write-Ahead Log / Archiving                                      |Forces a switch to the next xlog file if a new file has not been started within N seconds.                                   |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |0        |1073741823  |(NULL)                                                                        |0                |0                                                                             |
//	|array_nulls                        |on                |(NULL)|Version and Platform Compatibility / Previous PostgreSQL Versions|Enable input of NULL elements in arrays.                                                                                     |When turned on, unquoted NULL in an array input value means a null value; otherwise it is taken literally.                                                                                                                                                                                                                                                       |user      |bool   |default             |(NULL)   |(NULL)      |(NULL)                                                                        |on               |on                                                                            |
//	|authentication_timeout             |60                |s     |Connections and Authentication / Security and Authentication     |Sets the maximum allowed time to complete client authentication.                                                             |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |1        |600         |(NULL)                                                                        |60               |60                                                                            |
//	|autovacuum                         |on                |(NULL)|Autovacuum                                                       |Starts the autovacuum subprocess.                                                                                            |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |bool   |default             |(NULL)   |(NULL)      |(NULL)                                                                        |on               |on                                                                            |
//	|autovacuum_analyze_scale_factor    |0.1               |(NULL)|Autovacuum                                                       |Number of tuple inserts, updates, or deletes prior to analyze as a fraction of reltuples.                                    |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |real   |default             |0        |100         |(NULL)                                                                        |0.1              |0.1                                                                           |
//	|autovacuum_naptime                 |60                |s     |Autovacuum                                                       |Time to sleep between autovacuum runs.                                                                                       |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |1        |2147483     |(NULL)                                                                        |60               |60                                                                            |
//	|autovacuum_vacuum_cost_delay       |20                |ms    |Autovacuum                                                       |Vacuum cost delay in milliseconds, for autovacuum.                                                                           |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |-1       |100         |(NULL)                                                                        |20               |20                                                                            |
//	+-----------------------------------+------------------+------+-----------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------+-------+--------------------+---------+------------+------------------------------------------------------------------------------+-----------------+------------------------------------------------------------------------------+
//	Rows 228
	
//	1> ra_config
//	RS> Col# Label          JDBC Type Name         Guessed DBMS type
//	RS> ---- -------------- ---------------------- -----------------
//	RS> 1    Parameter Name java.sql.Types.CHAR    char(31)         
//	RS> 2    Parameter Type java.sql.Types.CHAR    char(9)          
//	RS> 3    Current Value  java.sql.Types.CHAR    char(78)         
//	RS> 4    Pending Value  java.sql.Types.CHAR    char(1)          
//	RS> 5    Default Value  java.sql.Types.CHAR    char(74)         
//	RS> 6    Legal Values   java.sql.Types.CHAR    char(38)         
//	RS> 7    Category       java.sql.Types.CHAR    char(25)         
//	RS> 8    Restart        java.sql.Types.INTEGER int              
//	RS> 9    Description    java.sql.Types.CHAR    char(252)        
//	+---------------------+--------------+-------------+-------------+----------------+---------------+--------+-------+---------------------------------------------------------------+
//	|Parameter Name       |Parameter Type|Current Value|Pending Value|Default Value   |Legal Values   |Category|Restart|Description                                                    |
//	+---------------------+--------------+-------------+-------------+----------------+---------------+--------+-------+---------------------------------------------------------------+
//	|admin_port           |int           |20500        |(NULL)       |10000           |range: 1,65535 |Admin   |1      |The client socket port number.                                 |
//	|admin_port_chunk_size|int           |1024         |(NULL)       |1024            |range: 1, 65535|Admin   |0      |The client socket port stream chunk size.                      |
//	|asa_password         |password      |******       |(NULL)       |N/A             |N/A            |ASA     |0      |ASA database user password.                                    |
//	|asm_password         |password      |******       |(NULL)       |<not_configured>|N/A            |PDS User|0      |Specifies the user password used when connecting to Oracle ASM.|
//	+---------------------+--------------+-------------+-------------+----------------+---------------+--------+-------+---------------------------------------------------------------+
//	(139 rows affected)
	
//	public static final String NON_DEFAULT          = "NonDefault";
//	public static final String CONFIG_NAME          = "ParameterName";
//	public static final String CONFIG_TYPE          = "ParameterType";
//	public static final String CURENT_VALUE         = "CurrentValue";
//	public static final String PENDING              = "Pending";
//	public static final String PENDING_VALUE        = "PendingValue";
//	public static final String DEFAULT_VALUE        = "DefaultValue";
//	public static final String LEGAL_VALUES         = "LegalValues";
//	public static final String SECTION_NAME         = "Category";
//	public static final String RESTART_IS_REQ       = "Restart";
//	public static final String DESCRIPTION          = "Description";

	public static final String NON_DEFAULT          = "NonDefault";
	public static final String SECTION_NAME         = "Category";
	public static final String CONFIG_NAME          = "ParameterName";
	public static final String CONFIG_TYPE          = "ParameterType";
	public static final String CURENT_VALUE         = "CurrentValue";
//	public static final String PENDING              = "Pending";
//	public static final String PENDING_VALUE        = "PendingValue";
//	public static final String DEFAULT_VALUE        = "DefaultValue";
//	public static final String LEGAL_VALUES         = "LegalValues";
//	public static final String RESTART_IS_REQ       = "Restart";
	public static final String DESCRIPTION          = "Description";
	public static final String EXTRA_DESCRIPTION    = "ExtraDescription";

	public static final String CONTEXT              = "Context";
	public static final String VARTYPE              = "VarType";
	public static final String SOURCE               = "Source";
	public static final String MIN_VALUE            = "MinValue";
	public static final String MAX_VALUE            = "MaxValue";
	public static final String ENUMVALS             = "EnumVals";
	public static final String BOOT_VAL             = "BootVal";
	public static final String RESET_VAL            = "ResetVal";
	public static final String SOURCE_FILE          = "SourceFile";
	public static final String SOURCE_LINE          = "SourceLine";
	
	private static final int COL_NAME        = 0;
	private static final int COL_EDITABLE    = 1;
	private static final int COL_CLASS       = 2;
	private static final int COL_SQLTYPE     = 3;
	private static final int COL_TOOLTIP     = 4;
	private static Object[][] COLUMN_HEADERS = 
	{
	//   ColumnName,           Editable, JTable type,   SQL Datatype,   Tooltip
	//   --------------------- --------- -------------- --------------- --------------------------------------------
//		{NON_DEFAULT,          false,    Boolean.class, "bit",          "True if the value is not configured to the default value."},
//		{SECTION_NAME,         true,     String .class, "varchar(60)",  "Configuration Group"},
//		{CONFIG_NAME,          true,     String .class, "varchar(60)",  "Name of the configuration, same as the name in ra_config."},
//		{CONFIG_TYPE,          true,     String .class, "varchar(30)",  "What type of parameter is this."},
//		{CURENT_VALUE,         false,    String .class, "varchar(255)", "Value of the configuration."},
//		{PENDING,              false,    Boolean.class, "bit",          "The Configuration has not yet taken effect, probably needs restart to take effect."},
//		{PENDING_VALUE,        false,    String .class, "varchar(255)", "The value which will be configured on next restart, if PENDING is true."},
//		{DEFAULT_VALUE,        false,    String .class, "varchar(100)", "The default configuration value."},
//		{RESTART_IS_REQ,       false,    Boolean.class, "bit",          "ASE needs to be rebooted for the configuration to take effect."},
//		{LEGAL_VALUES,         true,     String .class, "varchar(60)",  "What legal values can this configuration hold"},
//		{DESCRIPTION,          true,     String .class, "varchar(255)", "Description of the configuration."}

		{NON_DEFAULT,          false,    Boolean.class, "bit",           "True if the value is not configured to the default value."},
		{SECTION_NAME,         true,     String .class, "varchar(128)",  "Configuration Group"},
		{CONFIG_NAME,          true,     String .class, "varchar(128)",  "Run-time configuration parameter name."},
		{CONFIG_TYPE,          true,     String .class, "varchar(128)",  "What type of parameter is this."},
		{CURENT_VALUE,         false,    String .class, "varchar(255)",  "Value of the configuration."},
//		{PENDING,              false,    Boolean.class, "bit",           "The Configuration has not yet taken effect, probably needs restart to take effect."},
//		{PENDING_VALUE,        false,    String .class, "varchar(255)",  "The value which will be configured on next restart, if PENDING is true."},
//		{DEFAULT_VALUE,        false,    String .class, "varchar(100)",  "The default configuration value."},
//		{RESTART_IS_REQ,       false,    Boolean.class, "bit",           "ASE needs to be rebooted for the configuration to take effect."},
//		{LEGAL_VALUES,         true,     String .class, "varchar(60)",   "What legal values can this configuration hold"},

		{CONTEXT,              false,    String .class, "varchar(128)",  CONTEXT_TOOLTIP},
		{VARTYPE,              false,    String .class, "varchar(128)",  "Parameter type (bool, enum, integer, real, or string)"},
		{SOURCE,               false,    String .class, "varchar(128)",  "Source of the current parameter value"},
		{MIN_VALUE,            false,    String .class, "varchar(128)",  "Minimum value of the configuration"},
		{MAX_VALUE,            false,    String .class, "varchar(128)",  "Maximum value of the configuration"},
		{ENUMVALS,             false,    String .class, "varchar(128)",  "Allowed values of an enum parameter"},
		{BOOT_VAL,             false,    String .class, "varchar(128)",  "Parameter value assumed at server startup if the parameter is not otherwise set"},
		{RESET_VAL,            false,    String .class, "varchar(128)",  "Value that RESET would reset the parameter to in the current session"},

		{DESCRIPTION,          true,     String .class, "varchar(255)",  "Description of the configuration."},
		{EXTRA_DESCRIPTION,    true,     String .class, "varchar(1024)", "Extra Description of the configuration."},

		{SOURCE_FILE,          true,     String .class, "varchar(255)",  "Configuration file the current value was set in (null for values set from sources other than configuration files, or when examined by a non-superuser); helpful when using include directives in configuration files"},
		{SOURCE_LINE,          true,     Integer.class, "int",           "Line number within the configuration file the current value was set at (null for values set from sources other than configuration files, or when examined by a non-superuser)"}
	};

	private static String GET_CONFIG_ONLINE_SQL = 
		"select \n" +
		"       name \n" +
		"      ,setting \n" +
		"      ,unit \n" +
		"      ,category \n" +
		"      ,short_desc \n" +
		"      ,extra_desc \n" +
		"      ,context \n" +
		"      ,vartype \n" +
		"      ,source \n" +
		"      ,min_val \n" +
		"      ,max_val \n" +
		"      ,enumvals \n" +
		"      ,boot_val \n" +
		"      ,reset_val \n" +
		"      ,sourcefile \n" +
		"      ,sourceline \n" +
		"from pg_catalog.pg_settings";

	private static String GET_CONFIG_OFFLINE_SQL = 
		"select * " +
		"from " + PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_DBMS_CONFIG, null, true) + " \n" +
		"where \"SessionStartTime\" = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max(\"SessionStartTime\") " +
		"  from "+PersistWriterJdbc.getTableName(PersistWriterJdbc.SESSION_DBMS_CONFIG, null, true) + 
		" ) ";


	/** hash table */
	private HashMap<String,PostgresConfigEntry> _configMap  = null;
	private ArrayList<PostgresConfigEntry>      _configList = null;

	private ArrayList<String>              _configSectionList = null;

//	public class PostgresConfigEntry
//	{
//		/** configuration has been changed by any user */
//		public boolean isNonDefault;
//
//		/** Different sections in the configuration */
//		public String  sectionName;
//
//		/** Name of the configuration */
//		public String  configName;
//
//		/** What type of parameter is this. int, password, etc... */
//		public String  configType;
//
//		/** integer representation of the configuration */
//		public String  configValue;
//
//		/** true if the configuration value is set, but we need to restart the ASE server to enable the configuration */
//		public boolean isPending;
//
//		/** integer representation of the pending configuration */
//		public String  pendingValue;
//
//		/** String representation of the default configuration */
//		public String  defaultValue;
//
//		/** true if the server needs to be restarted to enable the configuration */
//		public boolean requiresRestart;
//
//		/** What legal values can this configuration hold */
//		public String  legalValues;
//
//		/** description of the configuration option */
//		public String  description;
//	}

//	+-----------------------------------+------------------+------+-----------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------+-------+--------------------+---------+------------+------------------------------------------------------------------------------+-----------------+------------------------------------------------------------------------------+
//	|name                               |setting           |unit  |category                                                         |short_desc                                                                                                                   |extra_desc                                                                                                                                                                                                                                                                                                                                                       |context   |vartype|source              |min_val  |max_val     |enumvals                                                                      |boot_val         |reset_val                                                                     |
//	+-----------------------------------+------------------+------+-----------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------+-------+--------------------+---------+------------+------------------------------------------------------------------------------+-----------------+------------------------------------------------------------------------------+
//	|allow_system_table_mods            |off               |(NULL)|Developer Options                                                |Allows modifications of the structure of system tables.                                                                      |(NULL)                                                                                                                                                                                                                                                                                                                                                           |postmaster|bool   |default             |(NULL)   |(NULL)      |(NULL)                                                                        |off              |off                                                                           |
//	|application_name                   |                  |(NULL)|Reporting and Logging / What to Log                              |Sets the application name to be reported in statistics and logs.                                                             |(NULL)                                                                                                                                                                                                                                                                                                                                                           |user      |string |default             |(NULL)   |(NULL)      |(NULL)                                                                        |                 |                                                                              |
//	|archive_command                    |(disabled)        |(NULL)|Write-Ahead Log / Archiving                                      |Sets the shell command that will be called to archive a WAL file.                                                            |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |string |configuration file  |(NULL)   |(NULL)      |(NULL)                                                                        |                 |test ! -f /var/lib/pgsql/9.3/archive/%f && cp %p /var/lib/pgsql/9.3/archive/%f|
//	|archive_mode                       |off               |(NULL)|Write-Ahead Log / Archiving                                      |Allows archiving of WAL files using archive_command.                                                                         |(NULL)                                                                                                                                                                                                                                                                                                                                                           |postmaster|bool   |configuration file  |(NULL)   |(NULL)      |(NULL)                                                                        |off              |off                                                                           |
//	|archive_timeout                    |0                 |s     |Write-Ahead Log / Archiving                                      |Forces a switch to the next xlog file if a new file has not been started within N seconds.                                   |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |0        |1073741823  |(NULL)                                                                        |0                |0                                                                             |
//	|array_nulls                        |on                |(NULL)|Version and Platform Compatibility / Previous PostgreSQL Versions|Enable input of NULL elements in arrays.                                                                                     |When turned on, unquoted NULL in an array input value means a null value; otherwise it is taken literally.                                                                                                                                                                                                                                                       |user      |bool   |default             |(NULL)   |(NULL)      |(NULL)                                                                        |on               |on                                                                            |
//	|authentication_timeout             |60                |s     |Connections and Authentication / Security and Authentication     |Sets the maximum allowed time to complete client authentication.                                                             |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |1        |600         |(NULL)                                                                        |60               |60                                                                            |
//	|autovacuum                         |on                |(NULL)|Autovacuum                                                       |Starts the autovacuum subprocess.                                                                                            |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |bool   |default             |(NULL)   |(NULL)      |(NULL)                                                                        |on               |on                                                                            |
//	|autovacuum_analyze_scale_factor    |0.1               |(NULL)|Autovacuum                                                       |Number of tuple inserts, updates, or deletes prior to analyze as a fraction of reltuples.                                    |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |real   |default             |0        |100         |(NULL)                                                                        |0.1              |0.1                                                                           |
//	|autovacuum_naptime                 |60                |s     |Autovacuum                                                       |Time to sleep between autovacuum runs.                                                                                       |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |1        |2147483     |(NULL)                                                                        |60               |60                                                                            |
//	|autovacuum_vacuum_cost_delay       |20                |ms    |Autovacuum                                                       |Vacuum cost delay in milliseconds, for autovacuum.                                                                           |(NULL)                                                                                                                                                                                                                                                                                                                                                           |sighup    |integer|default             |-1       |100         |(NULL)                                                                        |20               |20                                                                            |
//	+-----------------------------------+------------------+------+-----------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------+----------+-------+--------------------+---------+------------+------------------------------------------------------------------------------+-----------------+------------------------------------------------------------------------------+
//	Rows 228

	public class PostgresConfigEntry
	{
		/** configuration has been changed by any user */
		public boolean isNonDefault;

		/** sqlCol='name', Name of the configuration */
		public String  configName;

		/** sqlCol='setting', representation of the configuration */
		public String  configValue;

		/** sqlCol='unit', What type of parameter is this. second, millisecond etc... */
		public String  configType;

		/** sqlCol='category', Different sections in the configuration */
		public String  sectionName;

//		/** sqlCol='', true if the configuration value is set, but we need to restart the ASE server to enable the configuration */
//		public boolean isPending;

//		/** sqlCol='', integer representation of the pending configuration */
//		public String  pendingValue;

		/** sqlCol='', String representation of the default configuration */
		public String  defaultValue;

//		/** sqlCol='', true if the server needs to be restarted to enable the configuration */
//		public boolean requiresRestart;

//		/** sqlCol='', What legal values can this configuration hold */
//		public String  legalValues;

		/** sqlCol='short_desc', description of the configuration option */
		public String  description;

		/** sqlCol='extra_desc', Extra description of the configuration option */
		public String  extraDescription;

		
		/** sqlCol='context', xxx */
		public String  context;

		/** sqlCol='vartype', xxx */
		public String  vartype;

		/** sqlCol='source', xxx */
		public String  source;

		/** sqlCol='min_val', xxx */
		public String  min_val;

		/** sqlCol='max_val', xxx */
		public String  max_val;

		/** sqlCol='enumvals', xxx */
		public String  enumvals;

		/** sqlCol='boot_val', xxx */
		public String  boot_val;

		/** sqlCol='reset_val', xxx */
		public String  reset_val;

		/** sqlCol='sourcefile', xxx */
		public String  sourcefile;

		/** sqlCol='sourceline', xxx */
		public int  sourceline;
	}

	public PostgresConfig()
	{
//		DbmsConfigTextManager.clear();
//		RaxConfigText.createAndRegisterAllInstances();
	}

	/**
	 * Reset ALL configurations, this so we can get new ones later<br>
	 * Most possible called from disconnect() or similar
	 */
	@Override
	public void reset()
	{
		_configMap = null;
		super.reset();
	}

//	/** get the Map */
//	public Map<String,PostgresConfigEntry> getRaxConfigMap()
//	{
//		return _configMap;
//	}

	/** check if the RaxConfig is initialized or not */
	@Override
	public boolean isInitialized()
	{
		return _configMap != null;
	}

	/**
	 * Initialize 
	 * @param conn
	 */
	@Override
	public void initialize(DbxConnection conn, boolean hasGui, boolean offline, Timestamp ts)
	{
		_hasGui  = hasGui;
		_offline = offline;
		refresh(conn, ts);
	}

	/**
	 * refresh 
	 * @param conn
	 */
	@Override
	public void refresh(DbxConnection conn, Timestamp ts)
	{
		if (conn == null)
			return;

		reset();
		
		_configMap         = new HashMap<String,PostgresConfigEntry>();
		_configList        = new ArrayList<PostgresConfigEntry>();
		_configSectionList = new ArrayList<String>();
		
		String sql = "";
		try
		{
			// ONLINE GET CONFIG
			Statement stmt = conn.createStatement();
			stmt.setQueryTimeout(10);

			if ( ! _offline )
			{
				sql = GET_CONFIG_ONLINE_SQL;

				// First simple get a date
				ResultSet rs = stmt.executeQuery("select localtimestamp");
				while ( rs.next() )
					_timestamp = rs.getTimestamp(1);
				rs.close();

				// Then execute the Real query
				rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
//System.out.println("RaxConfig.refresh(): read row...");
					PostgresConfigEntry entry = new PostgresConfigEntry();

//					1> select x.name, x.setting, x.unit, x.category, x.short_desc, x.extra_desc, x.context, x.vartype, x.source, x.min_val, x.max_val, x.enumvals, x.boot_val, x.reset_val
//					2> from pg_catalog.pg_settings x
//					RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table
//					RS> ---- ---------- ---------------------- ----------------- ------------
//					RS> 1    name       java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 2    setting    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 3    unit       java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 4    category   java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 5    short_desc java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 6    extra_desc java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 7    context    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 8    vartype    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 9    source     java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 10   min_val    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 11   max_val    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 12   enumvals   java.sql.Types.ARRAY   _text             pg_settings 
//					RS> 13   boot_val   java.sql.Types.VARCHAR text(2147483647)  pg_settings 
//					RS> 14   reset_val  java.sql.Types.VARCHAR text(2147483647)  pg_settings 

					//entry.isNonDefault     = Set this below: configValue != defaultValue
					entry.configName         = rs.getString (1);
					entry.configValue        = rs.getString (2);
					entry.configType         = rs.getString (3);
					entry.sectionName        = rs.getString (4);
					entry.description        = rs.getString (5);
					entry.extraDescription   = rs.getString (6);

					entry.context            = rs.getString (7);
					entry.vartype            = rs.getString (8);
					entry.source             = rs.getString (9);
					entry.min_val            = rs.getString (10);
					entry.max_val            = rs.getString (11);
					entry.enumvals           = rs.getString (12);
					entry.boot_val           = rs.getString (13);
					entry.reset_val          = rs.getString (14);
					entry.sourcefile         = rs.getString (15);
					entry.sourceline         = rs.getInt    (16);

//					entry.defaultValue = entry.reset_val;
					entry.defaultValue = entry.boot_val;
					
//					// fix 'used memory' column
//					if (entry.configValue == null)
//						entry.isNonDefault = entry.configValue != entry.defaultValue;
//					else
//						entry.isNonDefault = ! entry.configValue.equals(entry.defaultValue);

					// DEFAULT Value???
//					entry.isNonDefault = ! "default".equals(entry.source);
					if (entry.configValue == null)
						entry.isNonDefault = entry.configValue != entry.defaultValue;
					else
						entry.isNonDefault = ! entry.configValue.equals(entry.defaultValue);

					if ("default".equals(entry.source))
						entry.isNonDefault = false;
					
					// Add
					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
				}
				rs.close();
			}
			else // OFFLINE GET CONFIG
			{
				sql = GET_CONFIG_OFFLINE_SQL;

				String tsStr = "";
				if (ts == null)
					tsStr = GET_CONFIG_OFFLINE_MAX_SESSION_SQL;
				else
					tsStr = "'" + ts + "'";

				sql = sql.replace("SESSION_START_TIME", tsStr);

				// Then execute the Real query
				ResultSet rs = stmt.executeQuery(sql);
				while ( rs.next() )
				{
					PostgresConfigEntry entry = new PostgresConfigEntry();

					// NOTE: same order as in COLUMN_HEADERS
					_timestamp               = rs.getTimestamp(1);
					entry.isNonDefault       = rs.getBoolean(2);

					entry.configName         = rs.getString (3);
					entry.configValue        = rs.getString (4);
					entry.configType         = rs.getString (5);
					entry.sectionName        = rs.getString (6);
					entry.description        = rs.getString (7);
					entry.extraDescription   = rs.getString (8);

					entry.context            = rs.getString (9);
					entry.vartype            = rs.getString (10);
					entry.source             = rs.getString (11);
					entry.min_val            = rs.getString (12);
					entry.max_val            = rs.getString (13);
					entry.enumvals           = rs.getString (14);
					entry.boot_val           = rs.getString (15);
					entry.reset_val          = rs.getString (16);
					entry.sourcefile         = rs.getString (17);
					entry.sourceline         = rs.getInt    (18);

//					entry.defaultValue = entry.reset_val;
					entry.defaultValue = entry.boot_val;

					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
				}
				rs.close();
				
			}
		}
		catch (SQLException ex)
		{
			_logger.error("RaxConfig:initialize:sql='"+sql+"'", ex);
			if (_hasGui)
				SwingUtils.showErrorMessage("RaxConfig - Initialize", "SQL Exception: "+ex.getMessage()+"\n\nThis was found when executing SQL statement:\n\n"+sql, ex);
			_configMap = null;
			_configList = null;
			_configSectionList = null;
			return;
		}

		// Check if we got any strange in the configuration
		// in case it does: report that...
		if ( ! _offline )
		{
			checkConfig(conn);
		}
		
		// notify change
		fireTableDataChanged();
	}

	/**
	 * Get description for the configuration name
	 * @param colfigName
	 * @return the description
	 */
	@Override
	public String getDescription(String configName)
	{
		if (_configMap == null)
			return null;

		PostgresConfigEntry e = _configMap.get(configName);
		
		return e == null ? null : e.description;
	}

//	/**
//	 * Get run value
//	 * @param colfigName
//	 * @return the description
//	 */
//	@Override
//	public int getRunValue(String configName)
//	{
//		if (_configMap == null)
//			return -1;
//
//		PostgresConfigEntry e = _configMap.get(configName);
//		
//		return e == null ? -1 : e.configValue;
//		return -99;
//	}

	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
	@Override
	public List<String> getSectionList()
	{
		return _configSectionList;
	}


	/**
	 * Get Section list, which is a list of all config sections
	 * @return List<String> of all sections
	 */
	@Override
	public Timestamp getTimestamp()
	{
		return _timestamp;
	}


	//--------------------------------------------------------------------------------
	// BEGIN implement: AbstractTableModel
	//--------------------------------------------------------------------------------
	@Override
	public Class<?> getColumnClass(int columnIndex)
	{
		return (Class<?>)COLUMN_HEADERS[columnIndex][COL_CLASS];
	}

	@Override
	public int getColumnCount()
	{
		return COLUMN_HEADERS.length;
	}

	@Override
	public String getColumnName(int columnIndex)
	{
		return (String)COLUMN_HEADERS[columnIndex][COL_NAME];
	}

	@Override
	public int getRowCount()
	{
		return _configList == null ? 0 : _configList.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex)
	{
		PostgresConfigEntry e = _configList.get(rowIndex);
		if (e == null)
			return null;

		switch (columnIndex)
		{
//		case 0:  return e.isNonDefault;
//		case 1:  return e.sectionName;
//		case 2:  return e.configName;
//		case 3:  return e.configType;
//		case 4:  return e.configValue;
//		case 5:  return e.isPending;
//		case 6:  return e.pendingValue;
//		case 7:  return e.defaultValue;
//		case 8:  return e.requiresRestart; 
//		case 9:  return e.legalValues; 
//		case 10: return e.description;

		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.configType;
		case 4:  return e.configValue;

		case 5:  return e.context;
		case 6:  return e.vartype;
		case 7:  return e.source;
		case 8:  return e.min_val;
		case 9:  return e.max_val;
		case 10: return e.enumvals;
		case 11: return e.boot_val;
		case 12: return e.reset_val;

		case 13: return e.description;
		case 14: return e.extraDescription;

		case 15: return e.sourcefile;
		case 16: return e.sourceline;
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex)
	{
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		return (Boolean) COLUMN_HEADERS[columnIndex][COL_EDITABLE];
	}

	
	@Override
	public int findColumn(String columnName) 
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++) 
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(columnName)) 
				return i;
		}
		return -1;
	}
	//--------------------------------------------------------------------------------
	// END implement: AbstractTableModel
	//--------------------------------------------------------------------------------

	@Override
	public String getColumnToolTip(String colName)
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++)
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
				return (String)COLUMN_HEADERS[i][COL_TOOLTIP];
		}
		return "";
	}

	@Override
	public String getSqlDataType(String colName)
	{
		for (int i=0; i<COLUMN_HEADERS.length; i++)
		{
			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
				return (String)COLUMN_HEADERS[i][COL_SQLTYPE];
		}
		return "";
	}
	@Override
	public String getSqlDataType(int colIndex)
	{
		return (String)COLUMN_HEADERS[colIndex][COL_SQLTYPE];
	}

	@Override
	public String getTabLabel()
	{
		return "Postgres Config";
	}

	@Override
	public String getCellToolTip(int mrow, int mcol)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(PostgresConfig.DESCRIPTION)))      .append("<br>");
		sb.append("<b>Extra Descr:  </b>").append(getValueAt(mrow, findColumn(PostgresConfig.EXTRA_DESCRIPTION))).append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(PostgresConfig.SECTION_NAME)))     .append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(PostgresConfig.CONFIG_NAME)))      .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(PostgresConfig.CURENT_VALUE)))     .append("<br>");
//		if (Boolean.TRUE.equals(getValueAt(mrow, findColumn(PostgresConfig.PENDING))))
//		{
//			sb.append("<br>");
//			sb.append("<b>NOTE: Postgres Needs to be rebooted for this option to take effect.</b>").append("<br>");
//		}
		sb.append("</html>");
		return sb.toString();
	}

	@Override
	public String getColName_pending()
	{
		return null;
//		return PENDING;
	}

	@Override
	public String getColName_sectionName()
	{
		return SECTION_NAME;
	}

	@Override
	public String getColName_configName()
	{
		return CONFIG_NAME;
	}

	@Override
	public String getColName_nonDefault()
	{
		return NON_DEFAULT;
	}
}
