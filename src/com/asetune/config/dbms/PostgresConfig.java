/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.config.dbms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.asetune.PostgresTune;
import com.asetune.config.dbms.DbmsConfigIssue.Severity;
import com.asetune.pcs.MonRecordingInfo;
import com.asetune.pcs.PersistWriterJdbc;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.PostgresUtils;
import com.asetune.utils.StringUtil;
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
	public static final String CURENT_VALUE_IN_MB   = "ValueInMb";
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
//	private static final int COL_SQLTYPE     = 3;
	private static final int COL_JDBC_TYPE   = 3;
	private static final int COL_JDBC_LENGTH = 4;
	private static final int COL_JDBC_SCALE  = 5;
	private static final int COL_TOOLTIP     = 6;
	private static Object[][] COLUMN_HEADERS = 
	{
	//   ColumnName,           Editable, JTable type,   JDBC Type      JDBC Length JDBC Scale /* SQL Datatype,    */ Tooltip
	//   --------------------- --------- -------------- -------------- ----------- ---------- /* ---------------  */ --------------------------------------------
		{NON_DEFAULT,          false,    Boolean.class, Types.BOOLEAN, -1,         -1,        /* "bit",           */ "True if the value is not configured to the default value."},
		{SECTION_NAME,         true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Configuration Group"},
		{CONFIG_NAME,          true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Run-time configuration parameter name."},
		{CONFIG_TYPE,          true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "What type of parameter is this."},
		{CURENT_VALUE,         true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "Value of the configuration."},
		{CURENT_VALUE_IN_MB,   true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Value of the configuration as MB. Formula: if (8kb) CurrentValue/128; if (kb) CurrentValue/1024; "},
                                                                                                                  
		{CONTEXT,              true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ CONTEXT_TOOLTIP},
		{VARTYPE,              true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Parameter type (bool, enum, integer, real, or string)"},
		{SOURCE,               true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Source of the current parameter value"},
		{MIN_VALUE,            true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Minimum value of the configuration"},
		{MAX_VALUE,            true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Maximum value of the configuration"},
		{ENUMVALS,             true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Allowed values of an enum parameter"},
		{BOOT_VAL,             true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Parameter value assumed at server startup if the parameter is not otherwise set"},
		{RESET_VAL,            true,     String .class, Types.VARCHAR, 128,        -1,        /* "varchar(128)",  */ "Value that RESET would reset the parameter to in the current session"},
                                                                                                                  
		{DESCRIPTION,          true,     String .class, Types.VARCHAR, 1024,       -1,        /* "varchar(1024)", */ "Description of the configuration."},
		{EXTRA_DESCRIPTION,    true,     String .class, Types.VARCHAR, 1024,       -1,        /* "varchar(1024)", */ "Extra Description of the configuration."},
                                                                                                                  
		{SOURCE_FILE,          true,     String .class, Types.VARCHAR, 255,        -1,        /* "varchar(255)",  */ "Configuration file the current value was set in (null for values set from sources other than configuration files, or when examined by a non-superuser); helpful when using include directives in configuration files"},
		{SOURCE_LINE,          true,     Integer.class, Types.INTEGER, -1,         -1,        /* "int",           */ "Line number within the configuration file the current value was set at (null for values set from sources other than configuration files, or when examined by a non-superuser)"}
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
		"from [" + PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "] \n" +
		"where [SessionStartTime] = SESSION_START_TIME \n";

	private static String GET_CONFIG_OFFLINE_MAX_SESSION_SQL = 
		" (select max([SessionStartTime]) " +
		"  from ["+PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false) + "]" +
		" ) ";


//	@Override
//	public String getSqlForDiff(boolean isOffline)
//	{
//		if (isOffline)
//		{
//			String tabName = PersistWriterJdbc.getTableName(null, PersistWriterJdbc.SESSION_DBMS_CONFIG, null, false);
//
//			return 
//					"select \n" +
//					"     [name] \n" +
//					"    ,[setting] \n" +
//					"from [" + tabName + "] \n" +
//					"where [SessionStartTime] = (select max([SessionStartTime]) from [" + tabName + "]) \n" +
//					"order by 1 \n" +
//					"";
//		}
//		else
//		{
//			return
//					"select \n" +
//					"       name \n" +
//					"      ,setting \n" +
//					"from pg_catalog.pg_settings \n" +
//					"order by 1 \n" +
//					"";
//		}
//	}
		
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
	implements IDbmsConfigEntry
	{
		@Override public String  getConfigKey()   { return configName; }
		@Override public String  getConfigValue() { return configValue; }
		@Override public String  getSectionName() { return sectionName; }
		@Override public boolean isPending()      { return false; }
		@Override public boolean isNonDefault()   { return isNonDefault; }
		
		/** configuration has been changed by any user */
		public boolean isNonDefault;

		/** sqlCol='name', Name of the configuration */
		public String  configName;

		/** sqlCol='setting', representation of the configuration */
		public String  configValue;

		/** sqlCol='unit', What type of parameter is this. second, millisecond etc... */
		public String  configType;

		/** Config value recalculated as MB */
		public String configValueMb;

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

	@Override
	public Map<String, ? extends IDbmsConfigEntry> getDbmsConfigMap()
	{
		return _configMap;
	}

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
		
		try { setDbmsServerName(conn.getDbmsServerName()); } catch (SQLException ex) { setDbmsServerName(ex.getMessage()); };
		try { setDbmsVersionStr(conn.getDbmsVersionStr()); } catch (SQLException ex) { setDbmsVersionStr(ex.getMessage()); };

		try { setLastUsedUrl( conn.getMetaData().getURL() ); } catch(SQLException ignore) { }
		setLastUsedConnProp(conn.getConnPropOrDefault());


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

					// 1> select 
					// 2>        name 
					// 3>       ,setting 
					// 4>       ,unit 
					// 5>       ,category 
					// 6>       ,short_desc 
					// 7>       ,extra_desc 
					// 8>       ,context 
					// 9>       ,vartype 
					// 10>       ,source 
					// 11>       ,min_val 
					// 12>       ,max_val 
					// 13>       ,enumvals 
					// 14>       ,boot_val 
					// 15>       ,reset_val 
					// 16>       ,sourcefile 
					// 17>       ,sourceline 
					// 18> from pg_catalog.pg_settings
					
					// RS> Col# Label      JDBC Type Name         Guessed DBMS type Source Table
					// RS> ---- ---------- ---------------------- ----------------- ------------
					// RS> 1    name       java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 2    setting    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 3    unit       java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 4    category   java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 5    short_desc java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 6    extra_desc java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 7    context    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 8    vartype    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 9    source     java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 10   min_val    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 11   max_val    java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 12   enumvals   java.sql.Types.ARRAY   _text             pg_settings 
					// RS> 13   boot_val   java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 14   reset_val  java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 15   sourcefile java.sql.Types.VARCHAR text(2147483647)  pg_settings 
					// RS> 16   sourceline java.sql.Types.INTEGER int4              pg_settings 

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

					// where Source not in('default', 'override', 'session', 'client')
					if (    "default" .equals(entry.source) 
					     || "override".equals(entry.source)
					     || "session" .equals(entry.source)
					     || "client"  .equals(entry.source)
					   )
					{
						entry.isNonDefault = false;
					}
					
//					// if the "Source File" for configuration is empty
//					if (entry.isNonDefault && StringUtil.isNullOrBlank(entry.source))
//						entry.isNonDefault = false;

					// Add
					_configMap .put(entry.configName, entry);
					_configList.add(entry);

					// Add it to the section list, if not already there
					if ( ! _configSectionList.contains(entry.sectionName) )
						_configSectionList.add(entry.sectionName);
					
					// Add or descriptions or make special calculations...
					enrichOnlineEntry(entry);
				}
				rs.close();
			}
			else // OFFLINE GET CONFIG
			{
				// For OFFLINE mode: override some the values previously fetched the connection object, with values from the Recorded Database
				MonRecordingInfo recordingInfo = new MonRecordingInfo(conn, null);
				setOfflineRecordingInfo(recordingInfo);
				setDbmsServerName(recordingInfo.getDbmsServerName());
				setDbmsVersionStr(recordingInfo.getDbmsVersionStr());

				// Check if this is correct TYPE
				String expectedType = PostgresTune.APP_NAME;
				String readType     = recordingInfo.getRecDbxAppName();
				if ( ! expectedType.equals(readType) )
				{
					throw new WrongRecordingVendorContent(expectedType, readType);
				}


				sql = GET_CONFIG_OFFLINE_SQL;

				String tsStr = "";
				if (ts == null)
					tsStr = GET_CONFIG_OFFLINE_MAX_SESSION_SQL;
				else
					tsStr = "'" + ts + "'";

				sql = sql.replace("SESSION_START_TIME", tsStr);

				// replace all '[' and ']' into DBMS Vendor Specific Chars
				sql = conn.quotifySqlString(sql);

				// Then execute the Real query
				ResultSet rs = stmt.executeQuery(sql);
				ResultSetMetaData rsmd = rs.getMetaData();

				// Store all column names/labels in a List, for DBMS portability use "lower-case" name
				List<String> columns = new ArrayList<>();
				int colCount = rsmd.getColumnCount();
				for (int c=0; c<colCount; c++)
					columns.add(rsmd.getColumnLabel(c).toLowerCase());

				while ( rs.next() )
				{
					PostgresConfigEntry entry = new PostgresConfigEntry();

					// NOTE: same order as in COLUMN_HEADERS
					_timestamp               = rs.getTimestamp(1);
//					entry.isNonDefault       = rs.getBoolean(2);

//					entry.configName         = rs.getString (3);
//					entry.configValue        = rs.getString (4);
//					entry.configValueMb      = rs.getString (4); // This is a column that might NOT been stored in previous releases...
//					entry.configType         = rs.getString (5);
//					entry.sectionName        = rs.getString (6);
//					entry.description        = rs.getString (7);
//					entry.extraDescription   = rs.getString (8);
//
//					entry.context            = rs.getString (9);
//					entry.vartype            = rs.getString (10);
//					entry.source             = rs.getString (11);
//					entry.min_val            = rs.getString (12);
//					entry.max_val            = rs.getString (13);
//					entry.enumvals           = rs.getString (14);
//					entry.boot_val           = rs.getString (15);
//					entry.reset_val          = rs.getString (16);
//					entry.sourcefile         = rs.getString (17);
//					entry.sourceline         = rs.getInt    (18);

					// All columns may not be stored in the offline database 
					// Only get the columns that are stored...
					// for DBMS portability use "lower-case" name
					if (columns.contains(NON_DEFAULT       .toLowerCase())) entry.isNonDefault       = rs.getBoolean( 1 + columns.indexOf(NON_DEFAULT       .toLowerCase()) );
					if (columns.contains(SECTION_NAME      .toLowerCase())) entry.sectionName        = rs.getString ( 1 + columns.indexOf(SECTION_NAME      .toLowerCase()) );
					if (columns.contains(CONFIG_NAME       .toLowerCase())) entry.configName         = rs.getString ( 1 + columns.indexOf(CONFIG_NAME       .toLowerCase()) );
					if (columns.contains(CONFIG_TYPE       .toLowerCase())) entry.configType         = rs.getString ( 1 + columns.indexOf(CONFIG_TYPE       .toLowerCase()) );
					if (columns.contains(CURENT_VALUE      .toLowerCase())) entry.configValue        = rs.getString ( 1 + columns.indexOf(CURENT_VALUE      .toLowerCase()) );
					if (columns.contains(CURENT_VALUE_IN_MB.toLowerCase())) entry.configValueMb      = rs.getString ( 1 + columns.indexOf(CURENT_VALUE_IN_MB.toLowerCase()) );
					if (columns.contains(DESCRIPTION       .toLowerCase())) entry.description        = rs.getString ( 1 + columns.indexOf(DESCRIPTION       .toLowerCase()) );
					if (columns.contains(EXTRA_DESCRIPTION .toLowerCase())) entry.extraDescription   = rs.getString ( 1 + columns.indexOf(EXTRA_DESCRIPTION .toLowerCase()) );

					if (columns.contains(CONTEXT           .toLowerCase())) entry.context            = rs.getString ( 1 + columns.indexOf(CONTEXT           .toLowerCase()) );
					if (columns.contains(VARTYPE           .toLowerCase())) entry.vartype            = rs.getString ( 1 + columns.indexOf(VARTYPE           .toLowerCase()) );
					if (columns.contains(SOURCE            .toLowerCase())) entry.source             = rs.getString ( 1 + columns.indexOf(SOURCE            .toLowerCase()) );
					if (columns.contains(MIN_VALUE         .toLowerCase())) entry.min_val            = rs.getString ( 1 + columns.indexOf(MIN_VALUE         .toLowerCase()) );
					if (columns.contains(MAX_VALUE         .toLowerCase())) entry.max_val            = rs.getString ( 1 + columns.indexOf(MAX_VALUE         .toLowerCase()) );
					if (columns.contains(ENUMVALS          .toLowerCase())) entry.enumvals           = rs.getString ( 1 + columns.indexOf(ENUMVALS          .toLowerCase()) );
					if (columns.contains(BOOT_VAL          .toLowerCase())) entry.boot_val           = rs.getString ( 1 + columns.indexOf(BOOT_VAL          .toLowerCase()) );
					if (columns.contains(RESET_VAL         .toLowerCase())) entry.reset_val          = rs.getString ( 1 + columns.indexOf(RESET_VAL         .toLowerCase()) );
					if (columns.contains(SOURCE_FILE       .toLowerCase())) entry.sourcefile         = rs.getString ( 1 + columns.indexOf(SOURCE_FILE       .toLowerCase()) );
					if (columns.contains(SOURCE_LINE       .toLowerCase())) entry.sourceline         = rs.getInt    ( 1 + columns.indexOf(SOURCE_LINE       .toLowerCase()) );

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

		if ( _offline )
		{
			// Load saved Configuration Issues from the offline database
			getOfflineConfigIssues(conn);
		}
		else
		{
			// Check if we got any strange in the configuration
			// in case it does: report that...
			checkConfig(conn);
		}
		
		// notify change
		fireTableDataChanged();
	}


	@Override
	public void checkConfig(DbxConnection conn)
	{
		if (_configMap == null)
			return;

		PostgresConfigEntry entry = null;
		String cfgName;

		String    srvName    = "-UNKNOWN-";
		Timestamp srvRestart = null;
		try { srvName    = conn.getDbmsServerName();          } catch (SQLException ex) { _logger.info("Problems getting Postgres instance name. ex="+ex);}
		try { srvRestart = PostgresUtils.getStartDate(conn);  } catch (SQLException ex) { _logger.info("Problems getting Postgres start date. ex="+ex);}

		
		//-------------------------------------------------------
		cfgName = "synchronous_commit";
		entry = _configMap.get(cfgName);
		if (entry != null)
		{
			if ("off".equalsIgnoreCase(entry.configValue))
			{
				String key = "DbmsConfigIssue." + srvName + ".pg_settings." + cfgToPropName(cfgName) + ".off";

				DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, cfgName, Severity.WARNING, 
						"The Server Level Configuration '" + cfgName + "' is 'OFF'. You may loose data in case of a power failure.", 
						"Fix this using: Set configuration '" + cfgName + "' to '{on|remote_apply|remote_write|local}'.");

				DbmsConfigManager.getInstance().addConfigIssue(issue);
			}
		}

		//-------------------------------------------------------
		cfgName = "shared_buffers"; // default value '???' but if it's less than 25% of the memory on the HW. 
		// This extension might be useful to get OS Memory --->>>> https://github.com/EnterpriseDB/system_stats
		entry = _configMap.get(cfgName);
		if (entry != null)
		{
			double mb_configured = StringUtil.parseDouble(entry.configValueMb, -1);
			double mb_threshold  = 128;
			
			if (mb_configured != -1 && mb_configured <= mb_threshold)
			{
				String key = "DbmsConfigIssue." + srvName + ".pg_settings." + cfgToPropName(cfgName) + ".is.lt." + mb_threshold + "mb";

				DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, cfgName, Severity.INFO, 
						"The Server Level Configuration '" + cfgName + "' is less-or-equal than " + mb_threshold + " MB (" + mb_configured + "). This is a memory area where (data/index) Pages are cached in memory. The OS probably has a LOT more memory (I guess you have more than 1GB memory), see https://www.postgresql.org/docs/current/runtime-config-resource.html", 
						"Fix this using: Set configuration '" + cfgName + "' to '25-40%' of the hosts memory. But leave some for the OS and FileSystem Caches etc.");

				DbmsConfigManager.getInstance().addConfigIssue(issue);
			}
		}

		
		//-------------------------------------------------------
		cfgName = "effective_cache_size"; // default value '4G' should be approc 75% of physical memory (or: shared_buffers + filesystem-cache) 
		// This extension might be useful to get OS Memory --->>>> https://github.com/EnterpriseDB/system_stats
		entry = _configMap.get(cfgName);
		if (entry != null)
		{
			double mb_configured = StringUtil.parseDouble(entry.configValueMb, -1);
			double mb_threshold  = 4096;
			
			if (mb_configured != -1 && mb_configured <= mb_threshold)
			{
				String key = "DbmsConfigIssue." + srvName + ".pg_settings." + cfgToPropName(cfgName) + ".is.lt." + mb_threshold + "mb";

				DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, cfgName, Severity.INFO, 
						"The Server Level Configuration '" + cfgName + "' is less-or-equal than " + mb_threshold + " MB (" + mb_configured + "). This is a optimzer only parameter, it do NOT allocate memory. see https://www.postgresql.org/docs/current/runtime-config-query.html", 
						"Fix this using: Set configuration '" + cfgName + "' to '75%' of the hosts memory. Or shared_buffers + os-filesystem-cache.");

				DbmsConfigManager.getInstance().addConfigIssue(issue);
			}
		}

		
		//-------------------------------------------------------
		cfgName = "shared_preload_libraries"; // pg_stat_statements is good to have *especially* if you are using PostgresTune
		entry = _configMap.get(cfgName);
		if (entry != null)
		{
			if ( ! entry.configValue.contains("pg_stat_statements") )
			{
				String key = "DbmsConfigIssue." + srvName + ".pg_settings." + cfgToPropName(cfgName) + ".not_available";

				DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, cfgName, Severity.INFO, 
						"The Server Level Configuration '" + cfgName + "' does NOT contain 'pg_stat_statements'. This is an important option for PostgresTune, it's used to see what SQL Statements are expensive.", 
						"Fix this using: 'CREATE EXTENSION pg_stat_statements', for more details check: https://www.postgresql.org/docs/current/static/pgstatstatements.html");

				DbmsConfigManager.getInstance().addConfigIssue(issue);
			}
		}
		

		//-------------------------------------------------------
		cfgName = "random_page_cost"; // if the default of 4.0, and you have fast disks (probably SSD), then 1.1 would be a better choice  
		entry = _configMap.get(cfgName);
		if (entry != null)
		{
			double cfg_val = StringUtil.parseDouble(entry.configValue, -1);
			double cfg_def = 4.0;
			
			if (cfg_val != -1 && cfg_val == cfg_def)
			{
				String key = "DbmsConfigIssue." + srvName + ".pg_settings." + cfgToPropName(cfgName) + ".is_default_configured";

				DbmsConfigIssue issue = new DbmsConfigIssue(srvRestart, key, cfgName, Severity.INFO, 
						"The Server Level Configuration '" + cfgName + "' is default configured (" + cfg_def + "). If you are using fast disk (SSD), my suggestion is 1.1, otherwise Postgres in some cases falls back on doing table scans, instead of index lookups.",
						"Fix this using: Set configuration '" + cfgName + "' to '1.1'. for more details check: https://www.postgresql.org/docs/current/runtime-config-query.html");

				DbmsConfigManager.getInstance().addConfigIssue(issue);
			}
		}

		//-------------------------------------------------------
		// TODO in the future
		cfgName = "min_parallel_table_scan_size"; // is probably set to low  
		cfgName = "min_parallel_index_scan_size"; // is probably set to low  

	}


	/**
	 * Set or enrich some fields in the Config Entry
	 * @param entry
	 */
	private void enrichOnlineEntry(PostgresConfigEntry entry)
	{
		if (entry == null)
			return;

		if (entry.configName == null)
			return;

		//-----------------------------------------------------------------
		// ADD More descriptions for a specific CONFIG_NAME
		//
		if ("shared_buffers".equals(entry.configName))
		{
			entry.extraDescription = StringUtil.isNullOrBlank(entry.extraDescription) ? "" : entry.extraDescription + " ";
			
			entry.extraDescription += "NOTE: This should typically be sized to 25-40% of available memory (Postgres do also relay on the OS FS Cache, so to big means 'double buffering').";
		}

		if ("effective_cache_size".equals(entry.configName))
		{
			entry.extraDescription = StringUtil.isNullOrBlank(entry.extraDescription) ? "" : entry.extraDescription + " ";
			
			entry.extraDescription += "NOTE: NOT a physical memory allocation, instead it's an indicator/hint to the planner. This should typically be sized to 50-75% of available memory. Used by the query planer for determine wheather data may or may not fit in memory.";
		}

		if ("work_mem".equals(entry.configName))
		{
			entry.extraDescription = StringUtil.isNullOrBlank(entry.extraDescription) ? "" : entry.extraDescription + " ";
			
			entry.extraDescription += "NOTE: Increase based on temp file activity to cover the size of generating temp files by ORDER BY, DISTINCT, JOIN, and hash tables. WARNING: Be CAREFUL - Memory is allocating per-operation. If to big the system (or OS) may start to memory swap. This can also be set on a session level by a connection that needs a larger work_mem";
		}

		if ("maintenance_work_mem".equals(entry.configName))
		{
			entry.extraDescription = StringUtil.isNullOrBlank(entry.extraDescription) ? "" : entry.extraDescription + " ";
			
			entry.extraDescription += "NOTE: 10% of memory up to 1GB. Used for maintenance ops such as VACUUM, CREATE INDEX and ALTER TABLE ADD FOREIGN KEY.";
		}

		if ("max_connections".equals(entry.configName))
		{
			entry.extraDescription = StringUtil.isNullOrBlank(entry.extraDescription) ? "" : entry.extraDescription + " ";
			
			entry.extraDescription += "NOTE: each connection creates an OS Process, which usually takes approx 10MB";
		}

		
		//-----------------------------------------------------------------
		// Translate Memory Used into a "readable" column... like "UsedMemoryMB", "UsedMemoryKB"
		//
		//-- FIXME: add the column and calculate like: 
		//                8kB * ### -->> ##.# MB
		//                 kB * ### -->> ##.# MB
		if ("8kB".equalsIgnoreCase(entry.configType))
		{
			try 
			{ 
				int intVal = Integer.parseInt(entry.configValue);
				if (intVal > 0)
				{
					BigDecimal inMb = new BigDecimal(intVal / 128.0).setScale(1, RoundingMode.HALF_EVEN);

					entry.configValueMb = "" + inMb; 
				}
			} 
			catch (NumberFormatException ignore) {}
		}
		if ("kB".equalsIgnoreCase(entry.configType))
		{
			try 
			{
				int intVal = Integer.parseInt(entry.configValue);
				if (intVal > 0)
				{
					BigDecimal inMb = new BigDecimal(intVal / 1024.0).setScale(1, RoundingMode.HALF_EVEN);

					entry.configValueMb = "" + inMb; 
				}
			} 
			catch (NumberFormatException ignore) {}
		}
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
		case 0:  return e.isNonDefault;
		case 1:  return e.sectionName;
		case 2:  return e.configName;
		case 3:  return e.configType;
		case 4:  return e.configValue;
		case 5:  return e.configValueMb;

		case 6:  return e.context;
		case 7:  return e.vartype;
		case 8:  return e.source;
		case 9:  return e.min_val;
		case 10:  return e.max_val;
		case 11: return e.enumvals;
		case 12: return e.boot_val;
		case 13: return e.reset_val;

		case 14: return e.description;
		case 15: return e.extraDescription;

		case 16: return e.sourcefile;
		case 17: return e.sourceline;
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

//	@Override
//	public String getSqlDataType(String colName)
//	{
//		for (int i=0; i<COLUMN_HEADERS.length; i++)
//		{
//			if (COLUMN_HEADERS[i][COL_NAME].equals(colName))
//				return (String)COLUMN_HEADERS[i][COL_SQLTYPE];
//		}
//		return "";
//	}
//	@Override
//	public String getSqlDataType(int colIndex)
//	{
//		return (String)COLUMN_HEADERS[colIndex][COL_SQLTYPE];
//	}
	@Override
	public String getSqlDataType(DbxConnection conn, int colIndex)
	{
		int jdbcType = (int)COLUMN_HEADERS[colIndex][COL_JDBC_TYPE];
		int length   = (int)COLUMN_HEADERS[colIndex][COL_JDBC_LENGTH];
		int scale    = (int)COLUMN_HEADERS[colIndex][COL_JDBC_SCALE];
		
		return conn.getDbmsDataTypeResolver().dataTypeResolverToTarget(jdbcType, length, scale);
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
		sb.append("<b>Description:  </b>").append(getValueAt(mrow, findColumn(PostgresConfig.DESCRIPTION)))       .append("<br>");
		sb.append("<b>Extra Descr:  </b>").append(getValueAt(mrow, findColumn(PostgresConfig.EXTRA_DESCRIPTION))) .append("<br>");
		sb.append("<b>Section Name: </b>").append(getValueAt(mrow, findColumn(PostgresConfig.SECTION_NAME)))      .append("<br>");
		sb.append("<b>Config Name:  </b>").append(getValueAt(mrow, findColumn(PostgresConfig.CONFIG_NAME)))       .append("<br>");
		sb.append("<b>Run Value:    </b>").append(getValueAt(mrow, findColumn(PostgresConfig.CURENT_VALUE)))      .append("<br>");
		if (findColumn(PostgresConfig.CURENT_VALUE_IN_MB) != -1)
			sb.append("<b>Run Value MB: </b>").append(getValueAt(mrow, findColumn(PostgresConfig.CURENT_VALUE_IN_MB))).append("<br>");

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
