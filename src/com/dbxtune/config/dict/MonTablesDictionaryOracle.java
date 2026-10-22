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
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
package com.dbxtune.config.dict;

import java.lang.invoke.MethodHandles;

import javax.naming.NameNotFoundException;

import com.dbxtune.sql.conn.DbxConnection;


public class MonTablesDictionaryOracle
extends MonTablesDictionaryDefault
{
	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		super.initialize(conn, hasGui);

		initExtraMonTablesDictionary();
	}


	/**
	 * NO, do not save MonTableDictionary in PCS
	 */
	@Override
	public boolean isSaveMonTablesDictionaryInPcsEnabled()
	{
		return false;
	}

	/**
	 * Add some information to the MonTablesDictionary<br>
	 * This will serv as a dictionary for ToolTip
	 * <p>
	 * Oracle has no "dictionary" with descriptions for the V$ views, so the descriptions for the views we use is maintained here.<br>
	 * Columns that are "calculated" by a CM is described in the CM itself, see: addMonTableDictForVersion()
	 */
	public static void initExtraMonTablesDictionary()
	{
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			if (mtd == null)
				return;

			//---------------------------------------------------------------------------------------------------------------
			// DBA_TABLESPACES
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("DBA_TABLESPACES",  "Describes all tablespaces in the database.");

			mtd.addColumn("DBA_TABLESPACES", "TABLESPACE_NAME",  "Name of the tablespace");
			mtd.addColumn("DBA_TABLESPACES", "BLOCK_SIZE",       "Tablespace block size (in bytes)");
			mtd.addColumn("DBA_TABLESPACES", "STATUS",           "Tablespace status: ONLINE, OFFLINE or READ ONLY");
			mtd.addColumn("DBA_TABLESPACES", "CONTENTS",         "Tablespace contents: PERMANENT, TEMPORARY or UNDO");
			mtd.addColumn("DBA_TABLESPACES", "BIGFILE",          "Indicates whether the tablespace is a bigfile tablespace (YES) or a smallfile tablespace (NO). A bigfile tablespace has exactly ONE data file.");

			//---------------------------------------------------------------------------------------------------------------
			// DBA_TABLESPACE_USAGE_METRICS
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("DBA_TABLESPACE_USAGE_METRICS",  "Describes tablespace usage metrics for all types of tablespaces, including permanent, temporary, and undo tablespaces.");

			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "TABLESPACE_NAME",  "Name of the tablespace");
			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "USED_SPACE",       "Used space, in database blocks");
			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "TABLESPACE_SIZE",  "Tablespace size, in database blocks. If a data file is AUTOEXTENSIBLE this is the maximum size it can grow to.");
			mtd.addColumn("DBA_TABLESPACE_USAGE_METRICS", "USED_PERCENT",     "Percentage of used space, as a function of the maximum possible tablespace size");

			//---------------------------------------------------------------------------------------------------------------
			// V$SYSTEM_EVENT
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("V$SYSTEM_EVENT",  "Total waits for an event, for the whole instance (since it was started).");

			mtd.addColumn("V$SYSTEM_EVENT", "EVENT_ID",             "Identifier of the wait event");
			mtd.addColumn("V$SYSTEM_EVENT", "WAIT_CLASS",           "<html>Name of the class the wait event belongs to:<ul>"
			                                                            + "<li><b>User I/O</b>       - Waiting for disk IO done by the session itself (reading data blocks)</li>"
			                                                            + "<li><b>System I/O</b>     - Waiting for disk IO done by background processes (writing data blocks, redo log, control file)</li>"
			                                                            + "<li><b>Commit</b>         - Waiting for the redo log to be written at COMMIT (the event: log file sync)</li>"
			                                                            + "<li><b>Concurrency</b>    - Waiting for internal database resources (latches, buffer busy, library cache locks)</li>"
			                                                            + "<li><b>Application</b>    - Waiting for locks caused by the application (row locks, table locks)</li>"
			                                                            + "<li><b>Configuration</b>  - Waiting caused by inadequate configuration (too small redo log files, shared pool...)</li>"
			                                                            + "<li><b>Network</b>        - Waiting for data to be sent over the network</li>"
			                                                            + "<li><b>Administrative</b> - Waiting for a DBA command to finish (for example an index rebuild)</li>"
			                                                            + "<li><b>Scheduler</b>      - Waiting due to Resource Manager</li>"
			                                                            + "<li><b>Cluster</b>        - Waiting for Real Application Cluster resources</li>"
			                                                            + "<li><b>Queueing</b>       - Delays in a pipelined environment (for example Streams)</li>"
			                                                            + "<li><b>Other</b>          - Waits that should not normally occur</li>"
			                                                            + "<li><b>Idle</b>           - The session is inactive, and waits for work (not included here)</li>"
			                                                            + "</ul></html>");
			mtd.addColumn("V$SYSTEM_EVENT", "EVENT",                "Name of the wait event");
			mtd.addColumn("V$SYSTEM_EVENT", "TOTAL_WAITS",          "Total number of waits for the event");
			mtd.addColumn("V$SYSTEM_EVENT", "TOTAL_TIMEOUTS",       "Total number of timeouts for the event");
			mtd.addColumn("V$SYSTEM_EVENT", "TIME_WAITED_MICRO",    "Total amount of time waited for the event (in microseconds)");
			mtd.addColumn("V$SYSTEM_EVENT", "TOTAL_WAITS_FG",       "Total number of waits for the event, from foreground sessions (the users)");
			mtd.addColumn("V$SYSTEM_EVENT", "TOTAL_TIMEOUTS_FG",    "Total number of timeouts for the event, from foreground sessions (the users)");
			mtd.addColumn("V$SYSTEM_EVENT", "TIME_WAITED_MICRO_FG", "Total amount of time waited for the event (in microseconds), from foreground sessions (the users)");

			//---------------------------------------------------------------------------------------------------------------
			// V$SYSSTAT
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("V$SYSSTAT",  "System statistics, for the whole instance (since it was started).");

			mtd.addColumn("V$SYSSTAT", "NAME",     "Name of the statistic");
			mtd.addColumn("V$SYSSTAT", "VALUE",    "Value of the statistic. What unit it is in depends on the statistic (count, bytes, blocks, centiseconds...)");
			mtd.addColumn("V$SYSSTAT", "CLASS",    "A number representing one or more statistics classes (a bitmap): 1=User, 2=Redo, 4=Enqueue, 8=Cache, 16=OS, 32=Real Application Clusters, 64=SQL, 128=Debug");
			mtd.addColumn("V$SYSSTAT", "STAT_ID",  "Identifier of the statistic (the same in all versions, which STATISTIC# is not)");

			//---------------------------------------------------------------------------------------------------------------
			// V$SYS_TIME_MODEL
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("V$SYS_TIME_MODEL",  "Accumulated time for various operations, for the whole instance (since it was started).");

			mtd.addColumn("V$SYS_TIME_MODEL", "STAT_NAME",  "Name of the statistic");
			mtd.addColumn("V$SYS_TIME_MODEL", "VALUE",      "Amount of time (in microseconds) that the instance has spent in this operation");
			mtd.addColumn("V$SYS_TIME_MODEL", "STAT_ID",    "Identifier of the statistic");

			//---------------------------------------------------------------------------------------------------------------
			// V$SESSION
			//---------------------------------------------------------------------------------------------------------------
			mtd.addTable("V$SESSION",  "Session information for each current session.");

			mtd.addColumn("V$SESSION", "SID",                     "Session identifier");
			mtd.addColumn("V$SESSION", "SERIAL#",                 "Session serial number. Used to uniquely identify a session's objects. Guarantees that session-level commands are applied to the correct session objects if the session ends and another session begins with the same session ID");
			mtd.addColumn("V$SESSION", "USERNAME",                "Oracle username. NULL for the background processes");
			mtd.addColumn("V$SESSION", "SCHEMANAME",              "Schema user name (the current schema, which can be changed with ALTER SESSION SET CURRENT_SCHEMA)");
			mtd.addColumn("V$SESSION", "STATUS",                  "Status of the session: ACTIVE (currently executing SQL), INACTIVE (waiting for the client), KILLED (marked to be killed), CACHED (temporarily cached for use by Oracle XA), SNIPED (inactive, and has exceeded a resource limit)");
			mtd.addColumn("V$SESSION", "TYPE",                    "Session type: USER or BACKGROUND");
			mtd.addColumn("V$SESSION", "SERVER",                  "Server type: DEDICATED, SHARED, PSEUDO, POOLED or NONE");
			mtd.addColumn("V$SESSION", "OSUSER",                  "Operating system client user name");
			mtd.addColumn("V$SESSION", "MACHINE",                 "Operating system machine name (of the client)");
			mtd.addColumn("V$SESSION", "PROGRAM",                 "Operating system program name (of the client)");
			mtd.addColumn("V$SESSION", "MODULE",                  "Name of the currently executing module, as set by the application by calling DBMS_APPLICATION_INFO.SET_MODULE");
			mtd.addColumn("V$SESSION", "ACTION",                  "Name of the currently executing action, as set by the application by calling DBMS_APPLICATION_INFO.SET_ACTION");
			mtd.addColumn("V$SESSION", "CLIENT_INFO",             "Information set by the application by calling DBMS_APPLICATION_INFO.SET_CLIENT_INFO");
			mtd.addColumn("V$SESSION", "CLIENT_IDENTIFIER",       "Client identifier of the session (the end user, for applications that uses a connection pool)");
			mtd.addColumn("V$SESSION", "SERVICE_NAME",            "Service name of the session (what the client connected to)");
			mtd.addColumn("V$SESSION", "LOGON_TIME",              "Time of logon");
			mtd.addColumn("V$SESSION", "LAST_CALL_ET",            "If the session STATUS is currently ACTIVE, then the value represents the elapsed time (in seconds) since the session has become active. If the session STATUS is currently INACTIVE, then the value represents the elapsed time (in seconds) since the session has become inactive.");
			mtd.addColumn("V$SESSION", "SQL_ID",                  "SQL identifier of the SQL statement that is currently being executed. NULL if the session is not executing anything");
			mtd.addColumn("V$SESSION", "SQL_CHILD_NUMBER",        "Child number of the SQL statement that is currently being executed. Together with SQL_ID it identifies a cursor (and its execution plan) in V$SQL");
			mtd.addColumn("V$SESSION", "SQL_EXEC_START",          "Time when the execution of the SQL currently executed by this session started");
			mtd.addColumn("V$SESSION", "PREV_SQL_ID",             "SQL identifier of the last SQL statement executed");
			mtd.addColumn("V$SESSION", "BLOCKING_SESSION",        "Session identifier of the blocking session (the one that holds what this session waits for). Empty if the session is not blocked");
			mtd.addColumn("V$SESSION", "FINAL_BLOCKING_SESSION",  "Session identifier of the final blocking session: the session at the END of a chain of blocked sessions (A waits for B that waits for C, then C is the final blocker)");
			mtd.addColumn("V$SESSION", "WAIT_CLASS",              "Name of the class of the wait event (User I/O, Concurrency, Application, Idle...)");
			mtd.addColumn("V$SESSION", "EVENT",                   "Resource or event for which the session is waiting. If STATE is not WAITING, this is the LAST event the session waited for");
			mtd.addColumn("V$SESSION", "STATE",                   "Wait state: WAITING (the session is currently waiting), WAITED UNKNOWN TIME, WAITED SHORT TIME or WAITED KNOWN TIME (the session is NOT waiting, it is on CPU)");
			mtd.addColumn("V$SESSION", "RESOURCE_CONSUMER_GROUP", "Name of the session's current resource consumer group (Resource Manager)");
		}
		catch (NameNotFoundException e)
		{
			System.out.println("Problems in cm='" + MethodHandles.lookup().lookupClass() + "', adding addMonTableDictForVersion. Caught: " + e);
		}
	}
}
