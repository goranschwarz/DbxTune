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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.config.dict;

import java.lang.invoke.MethodHandles;

import javax.naming.NameNotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.sql.conn.DbxConnection;


public class MonTablesDictionarySqlServer
extends MonTablesDictionaryDefault
{
    /** Log4j logging. */
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void initialize(DbxConnection conn, boolean hasGui)
	{
		// TODO Auto-generated method stub
		super.initialize(conn, hasGui);

		initExtraMonTablesDictionary();
	}

	
//	@Override
//	public void initializeVersionInfo(DbxConnection conn, boolean hasGui)
//	{
//		if (conn == null)
//			return;
//		
//		if (isVersionInfoInitialized())
//			return;
//
//
//		//------------------------------------
//		// - Set the DBMS Servername
//		// - Get what Version the DBMS is of
//		// - SORT order ID and NAME
//		// - Can this possible be a SAP Business Suite System
//
//		try { setDbmsServerName          ( conn.getDbmsServerName()    ); } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsServerName(). Caught: "+ex); }
//		try { setDbmsExecutableVersionStr( conn.getDbmsVersionStr()    ); } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsVersionStr(). Caught: "+ex); }
//		      setDbmsExecutableVersionNum( conn.getDbmsVersionNumber() ); 
//
//		try { setDbmsSortName            (conn.getDbmsSortOrderName());   } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsSortOrderName(). Caught: "+ex); }
//		try { setDbmsSortId              (conn.getDbmsSortOrderId());     } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsSortOrderId().   Caught: "+ex); }
//
//		try { setDbmsCharsetName         (conn.getDbmsCharsetName());     } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsCharsetName(). Caught: "+ex); }
//		try { setDbmsCharsetId           (conn.getDbmsCharsetId());       } catch(SQLException ex) { _logger.warn("initializeVersionInfo() problems when getting getDbmsCharsetId().   Caught: "+ex); }
//
////		//------------------------------------
////		// Can this possible be a SAP Business Suite System
////			setSapSystemInfo(sapSystemInfo);
//		
//		setVersionInfoInitialized(true);
//	}
//

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
	 */
	public static void initExtraMonTablesDictionary()
	{
		// Spinlock
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			if (mtd == null)
				return;
			
			// Add all "known" counter name descriptions
			mtd.addSpinlockDescription("LOCK_HASH",  "LOCK_HASH Contention: "
					+ "Contention on certain lock structure or hash bucket collisions is unavoidable in some cases. "
					+ "Even though the SQL Server engine partitions the majority of lock structures, there are still times when acquiring a lock results in access the same hash bucket. "
					+ "For example, an application the accesses the same row by many threads concurrently (i.e. reference data). "
					+ "This type of problems can be approached by techniques which either scale out this reference data within the database schema or leverage NOLOCK hints when possible");
		}



		//////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////
		// The below code is generated, and grabbed from: 
		// https://msdn.microsoft.com/en-us/library/ms188754.aspx
		//////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////


		// ---------------------------------------------------------------------------------------
		// sysprocesses
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("sysprocesses",  "<p>Contains information about processes that are running on an instance of SQL Server. These processes can be client processes or system processes. To access sysprocesses, you must be in the master database context, or you must use the master.dbo.sysprocesses three-part name.</p>");

			// Column names and description
			mtd.addColumn("sysprocesses", "spid"         , "<html> SQL Server session ID </html>");
			mtd.addColumn("sysprocesses", "kpid"         , "<html> Windows thread ID. </html>");
			mtd.addColumn("sysprocesses", "blocked"      , "<html> ID of the session that is blocking the request. If this column is NULL, the request is not blocked, or the session information of the blocking session is not available (or cannot be identified). "
			                                              + "  <ul>"
			                                              + "    <li>-2 = The blocking resource is owned by an orphaned distributed transaction.</li>"
			                                              + "    <li>-3 = The blocking resource is owned by a deferred recovery transaction.</li>"
			                                              + "    <li>-4 = Session ID of the blocking latch owner could not be determined due to internal latch state transitions.</li>"
			                                              + "  </ul>"
			                                              + "</html>");
			mtd.addColumn("sysprocesses", "waittype"     , "<html> Reserved </html>");
			mtd.addColumn("sysprocesses", "waittime"     , "<html> Current wait time in milliseconds. (0 = Process is not waiting) </html>");
			mtd.addColumn("sysprocesses", "lastwaittype" , "<html> A string indicating the name of the last or current wait type. </html>");
			mtd.addColumn("sysprocesses", "waitresource" , "<html> Textual representation of a lock resource. </html>");
			mtd.addColumn("sysprocesses", "dbid"         , "<html> ID of the database currently being used by the process. </html>");
			mtd.addColumn("sysprocesses", "uid"          , "<html> ID of the user that executed the command. Overflows or returns NULL if the number of users and roles exceeds 32,767. </html>");
			mtd.addColumn("sysprocesses", "cpu"          , "<html> Cumulative CPU time for the process. The entry is updated for all processes, regardless of whether the SET STATISTICS TIME option is ON or OFF. </html>");
			mtd.addColumn("sysprocesses", "physical_io"  , "<html> Cumulative disk reads and writes for the process. </html>");
			mtd.addColumn("sysprocesses", "memusage"     , "<html> Number of pages in the procedure cache that are currently allocated to this process. A negative number indicates that the process is freeing memory allocated by another process. </html>");
			mtd.addColumn("sysprocesses", "login_time"   , "<html> Time at which a client process logged into the server. </html>");
			mtd.addColumn("sysprocesses", "last_batch"   , "<html> Last time a client process executed a remote stored procedure call or an EXECUTE statement. </html>");
			mtd.addColumn("sysprocesses", "ecid"         , "<html> Execution context ID used to uniquely identify the subthreads operating on behalf of a single process. </html>");
			mtd.addColumn("sysprocesses", "open_tran"    , "<html> Number of open transactions for the process. </html>");
			mtd.addColumn("sysprocesses", "status"       , "<html> Process ID status. The possible values are:"
			                                              + "  <ul>"
			                                              + "    <li><b>dormant   </b> = SQL Server is resetting the session.</li>"
			                                              + "    <li><b>running   </b> = The session is running one or more batches. When Multiple Active Result Sets (MARS) is enabled, a session can run multiple batches. For more information, see Using Multiple Active Result Sets (MARS).</li>"
			                                              + "    <li><b>background</b> = The session is running a background task, such as deadlock detection.</li>"
			                                              + "    <li><b>rollback  </b> = The session has a transaction rollback in process.</li>"
			                                              + "    <li><b>pending   </b> = The session is waiting for a worker thread to become available.</li>"
			                                              + "    <li><b>runnable  </b> = The task in the session is in the runnable queue of a scheduler while waiting to get a time quantum.</li>"
			                                              + "    <li><b>spinloop  </b> = The task in the session is waiting for a spinlock to become free.</li>"
			                                              + "    <li><b>suspended </b> = The session is waiting for an event, such as I/O, to complete.</li>"
			                                              + "  </ul>"
			                                              + " </html>");
			mtd.addColumn("sysprocesses", "sid"          , "<html> Globally unique identifier (GUID) for the user. </html>");
			mtd.addColumn("sysprocesses", "hostname"     , "<html> Name of the workstation. </html>");
			mtd.addColumn("sysprocesses", "program_name" , "<html> Name of the application program. </html>");
			mtd.addColumn("sysprocesses", "hostprocess"  , "<html> Workstation process ID number. </html>");
			mtd.addColumn("sysprocesses", "cmd"          , "<html> Command currently being executed. </html>");
			mtd.addColumn("sysprocesses", "nt_domain"    , "<html> Windows domain for the client, if using Windows Authentication, or a trusted connection. </html>");
			mtd.addColumn("sysprocesses", "nt_username"  , "<html> Windows user name for the process, if using Windows Authentication, or a trusted connection. </html>");
			mtd.addColumn("sysprocesses", "net_address"  , "<html> Assigned unique identifier for the network adapter on the workstation of each user. When a user logs in, this identifier is inserted in the net_address column. </html>");
			mtd.addColumn("sysprocesses", "net_library"  , "<html> Column in which the client's network library is stored. Every client process comes in on a network connection. Network connections have a network library associated with them that enables them to make the connection. </html>");
			mtd.addColumn("sysprocesses", "loginame"     , "<html> Login name. </html>");
			mtd.addColumn("sysprocesses", "context_info" , "<html> Data stored in a batch by using the SET CONTEXT_INFO statement. </html>");
			mtd.addColumn("sysprocesses", "sql_handle"   , "<html> Represents the currently executing batch or object.<br><b>Note:</b> This value is derived from the batch or memory address of the object. This value is not calculated by using the SQL Server hash-based algorithm. </html>");
			mtd.addColumn("sysprocesses", "stmt_start"   , "<html> Starting offset of the current SQL statement for the specified sql_handle. </html>");
			mtd.addColumn("sysprocesses", "stmt_end"     , "<html> Ending offset of the current SQL statement for the specified sql_handle.<br> -1 = Current statement runs to the end of the results returned by the fn_get_sql function for the specified sql_handle. </html>");
			mtd.addColumn("sysprocesses", "request_id"   , "<html> ID of request. Used to identify requests running in a specific session. </html>");
			mtd.addColumn("sysprocesses", "page_resource", "<html> An 8-byte hexadecimal representation of the page resource if the waitresource column contains a page. </html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'sysprocesses ' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_databases_cluster
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_databases_cluster",  "<p>Contains one row for each availability database on the instance of SQL Server that is hosting an availability replica for any Always On availability group in the Windows Server Failover Clustering (WSFC) cluster, regardless of whether the local copy database has been joined to the availability group yet.</p>");

			// Column names and description
			mtd.addColumn("availability_databases_cluster", "group_id"           , "<html><p>Unique identifier of the availability group in which the availability group, if any, in which the database is participating.<br><br>NULL = database is not part of an availability replica of in availability group.</p></html>");
			mtd.addColumn("availability_databases_cluster", "group_database_id"  , "<html><p>Unique identifier of the database within the availability group, if any, in which the database is participating. <b>group_database_id</b> is the same for this database on the primary replica and on every secondary replica on which the database has been joined to the availability group.<br><br>NULL = database is not part of an availability replica in any availability group.</p></html>");
			mtd.addColumn("availability_databases_cluster", "database_name"      , "<html><p>Name of the database that was added to the availability group.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'availability_databases_cluster' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_group_listener_ip_addresses
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_group_listener_ip_addresses",  "<p>Returns a row for every IP address that is associated with any Always On availability group listener in the Windows Server Failover Clustering (WSFC) cluster.</p>");

			// Column names and description
			mtd.addColumn("availability_group_listener_ip_addresses", "listener_id"                   , "<html><p>Resource GUID from Windows Server Failover Clustering (WSFC) cluster.</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "ip_address"                    , "<html><p>Configured virtual IP address of the availability group listener. Returns a single IPv4 or IPv6 address.</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "ip_subnet_mask"                , "<html><p>Configured IP subnet mask for the IPv4 address, if any, that is configured for the availability group listener.<br><br>NULL = IPv6 subnet</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "is_dhcp"                       , "<html><p>Whether the IP address is configured by DHCP, one of:"
			                                                                                             + "<ul>"
			                                                                                             + "  <li>0 = IP address is not configured by DHCP.</li>"
			                                                                                             + "  <li>1 = IP address is configured by DHCP</li>"
			                                                                                             + "</ul>"
			                                                                                             +"</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "network_subnet_ip"             , "<html><p>Network subnet IP address that specifies the subnet to which the IP address belongs.</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "network_subnet_prefix_length"  , "<html><p>Network subnet prefix length of the subnet to which the IP address belongs.</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "network_subnet_ipv4_mask"      , "<html><p>Network subnet mask of the subnet to which the IP address belongs. <b>network_subnet_ipv4_mask</b> to specify the DHCP <network_subnet_option> options in a WITH DHCP clause of the CREATE AVAILABILITY GROUP or ALTER AVAILABILITY GROUPTransact-SQL statement.<br><br>NULL = IPv6 subnet</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "state"                         , "<html><p>IP resource ONLINE/OFFLINE state from the WSFC cluster, one of:"
			                                                                                             + "<ul>"
			                                                                                             + "  <li>1 = Online. IP resource is online.</li>"
			                                                                                             + "  <li>0 = Offline. IP resource is offline.</li>"
			                                                                                             + "  <li>2 = Online Pending. IP resource is offline but is being brought online.</li>"
			                                                                                             + "  <li>3 = Failed. IP resource was being brought online but failed.</li>"
			                                                                                             + "</ul>"
			                                                                                             + "</p></html>");
			mtd.addColumn("availability_group_listener_ip_addresses", "state_desc"                    , "<html><p>Description of <b>state</b>, one of:"
			                                                                                             + "<ul>"
			                                                                                             + "  <li>ONLINE</li>"
			                                                                                             + "  <li>OFFLINE</li>"
			                                                                                             + "  <li>ONLINE_PENDING</li>"
			                                                                                             + "  <li>FAILED</li>"
			                                                                                             + "</ul>"
			                                                                                             + "</p></html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems adding 'availability_group_listener_ip_addresses' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_group_listeners
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_group_listeners",  "<p>For each Always On availability group, returns either zero rows indicating that no network name is associated with the availability group, or returns a row for each availability-group listener configuration in the Windows Server Failover Clustering (WSFC) cluster. This view displays the real-time configuration gathered from cluster.</p>");

			// Column names and description
			mtd.addColumn("availability_group_listeners", "group_id"                                  , "<html><p>Availability group ID (group_id) from sys.availability_groups.</p></html>");
			mtd.addColumn("availability_group_listeners", "listener_id"                               , "<html><p>GUID from the cluster resource ID.</p></html>");
			mtd.addColumn("availability_group_listeners", "dns_name"                                  , "<html><p>Configured network name (hostname) of the availability group listener.</p></html>");
			mtd.addColumn("availability_group_listeners", "port"                                      , "<html><p>The TCP port number configured for the availability group listener.<br><br>NULL = Listener was configured outside SQL Server and its port number has not been added to the availability group. To add the port, pleaseuse the MODIFY LISTENER option of the ALTER AVAILABILITY GROUPTransact-SQL statement.</p></html>");
			mtd.addColumn("availability_group_listeners", "is_conformant"                             , "<html><p>Whether this IP configuration is conformant, one of:<br>"
			                                                                                              + "1 = Listener is conformant. Only 'OR' relations exist among its Internet Protocol (IP) addresses. Conformant encompasses every an IP configuration that was created by the CREATE AVAILABILITY GROUPTransact-SQL statement. In addition, if an IP configuration that was created outside of SQL Server, for example by using the WSFC Failover Cluster Manager, but can be modified by the ALTER AVAILABILITY GROUP tsql statement, the IP configuration qualifies as conformant.<br>"
			                                                                                              + "0 = Listener is nonconformant. Typically, this indicates an IP address that could not be configured by using SQL Server commands and, instead, was defined directly in the WSFC cluster.<br>"
			                                                                                              + "</p></html>");
			mtd.addColumn("availability_group_listeners", "ip_configuration_string_from_cluster"      , "<html><p>Cluster IP configuration strings, if any, for this listener. <br>NULL = Listener has no virtual IP addresses.</p></html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems adding 'availability_group_listeners' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_groups
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_groups",  "<p>Returns a row for each availability group for which the local instance of SQL Server hosts an availability replica. Each row contains a cached copy of the availability group metadata.</p>");

			// Column names and description
			mtd.addColumn("availability_groups", "group_id"                         , "<html><p>Unique identifier (GUID) of the availability group.</p></html>");
			mtd.addColumn("availability_groups", "name"                             , "<html><p>Name of the availability group. This is a user-specified name that must be unique within the Windows Server Failover Cluster (WSFC).</p></html>");
			mtd.addColumn("availability_groups", "resource_id"                      , "<html><p>Resource ID for the WSFC cluster resource.</p></html>");
			mtd.addColumn("availability_groups", "resource_group_id"                , "<html><p>Resource Group ID for the WSFC cluster resource group of the availability group.</p></html>");
			mtd.addColumn("availability_groups", "failure_condition_level"          , "<html><p>User-defined failure condition level under which an automatic failover must be triggered, one of the integer values shown in the table immediately below this table.<br><br>The failure-condition levels (1-5) range from the least restrictive, level 1, to the most restrictive, level 5. A given condition level encompasses all of the less restrictive levels. Thus, the strictest condition level, 5, includes the four less restrictive condition levels (1-4), level 4 includes levels 1-3, and so forth.<br><br>To change this value, use the FAILURE_CONDITION_LEVEL option of the ALTER AVAILABILITY GROUPTransact-SQL statement.</p></html>");
			mtd.addColumn("availability_groups", "health_check_timeout"             , "<html><p>Wait time (in milliseconds) for the sp_server_diagnostics system stored procedure to return server-health information, before the server instance is assumed to be slow or not responding. The default value is 30000 milliseconds (30 seconds).<br><br>To change this value, use the HEALTH_CHECK_TIMEOUT option of the ALTER AVAILABILITY GROUPTransact-SQL statement.</p></html>");
			mtd.addColumn("availability_groups", "automated_backup_preference"      , "<html><p>Preferred location for performing backups on the availability databases in this availability group. The following are the possible values and their descriptions.<br><br>"
			                                                                            + "0 : Primary. Backups should always occur on the primary replica.<br>"
			                                                                            + "1 : Secondary only. Performing backups on a secondary replica is preferable.<br>"
			                                                                            + "2 : Prefer Secondary. Performing backups on a secondary replica preferable, but performing backups on the primary replica is acceptable if no secondary replica is available for backup operations. This is the default behavior.<br>"
			                                                                            + "3 : Any Replica. No preference about whether backups are performed on the primary replica or on a secondary replica.<br><br>"
			                                                                            + "For more information, see Active Secondaries: Backup on Secondary Replicas (Always On Availability Groups)."
			                                                                            + "</p></html>");
			mtd.addColumn("availability_groups", "automated_backup_preference_desc" , "<html><p>Description of automated_backup_preference, one of:<br>"
			                                                                            + "PRIMARY<br>"
			                                                                            + "SECONDARY_ONLY<br>"
			                                                                            + "SECONDARY<br>"
			                                                                            + "NONE<br>"
			                                                                            + "</p></html>");
			mtd.addColumn("availability_groups", "version"                          , "<html><p>The version of the availability group metadata stored in the Windows Failover Cluster. This version number is incremented when new features are added.</p></html>");
			mtd.addColumn("availability_groups", "basic_features"                   , "<html><p>Specifies whether this is a Basic availability group. For more information, see Basic Availability Groups (Always On Availability Groups).</p></html>");
			mtd.addColumn("availability_groups", "dtc_support"                      , "<html><p>Specifies whether DTC support has been enabled for this availability group. The DTC_SUPPORT option of CREATE AVAILABILITY GROUP controls this setting.</p></html>");
			mtd.addColumn("availability_groups", "db_failover"                      , "<html><p>Specifies whether the availability group supports failover for database health conditions. The DB_FAILOVER option of CREATE AVAILABILITY GROUP controls this setting.</p></html>");
			mtd.addColumn("availability_groups", "is_distributed"                   , "<html><p>Specifies whether this is a distributed availability group. For more information, see Distributed Availability Groups (Always On Availability Groups).</p></html>");
		}
		catch (NameNotFoundException e) 
		{
			_logger.warn("Problems adding 'availability_groups' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_groups_cluster
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_groups_cluster",  "<p>Returns a row for each Always On availability group in the Windows Server Failover Clustering (WSFC) . Each row contains the availability group metadata from the WSFC cluster.</p>");

			// Column names and description
			mtd.addColumn("availability_groups_cluster", "group_id"                         , "<html><p>Unique identifier (GUID) of the availability group.</p></html>");
			mtd.addColumn("availability_groups_cluster", "name"                             , "<html><p>Name of the availability group. This is a user-specified name that must be unique within the Windows Server Failover Cluster (WSFC).</p></html>");
			mtd.addColumn("availability_groups_cluster", "resource_id"                      , "<html><p>Resource ID for the WSFC cluster resource.</p></html>");
			mtd.addColumn("availability_groups_cluster", "resource_group_id"                , "<html><p>Resource Group ID for the WSFC cluster resource group of the availability group.</p></html>");
			mtd.addColumn("availability_groups_cluster", "failure_condition_level"          , "<html><p>User-defined failure condition level under which an automatic failover must be triggered, one of the following integer values:<br><br>"
			                                                                                    + "1: Specifies that an automatic failover should be initiated when any of the following occurs:<br>"
			                                                                                    + "- The SQL Server service is down.<br>"
			                                                                                    + "- The lease of the availability group for connecting to the WSFC failover cluster expires because no ACK is received from the server instance. For more information, see How It Works: SQL Server Always On Lease Timeout.<br>"
			                                                                                    + "<br>"
			                                                                                    + "2: Specifies that an automatic failover should be initiated when any of the following occurs:<br>"
			                                                                                    + "- The instance of SQL Server does not connect to cluster, and the user-specified health_check_timeout threshold of the availability group is exceeded.<br>"
			                                                                                    + "- The availability replica is in failed state.<br>"
			                                                                                    + "<br>"
			                                                                                    + "3: Specifies that an automatic failover should be initiated on critical SQL Server internal errors, such as orphaned spinlocks, serious write-access violations, or too much dumping. This is the default value.<br>"
			                                                                                    + "<br>"
			                                                                                    + "4: Specifies that an automatic failover should be initiated on moderate SQL Server internal errors, such as a persistent out-of-memory condition in the SQL Server internal resource pool.<br>"
			                                                                                    + "<br>"
			                                                                                    + "5: Specifies that an automatic failover should be initiated on any qualified failure conditions, including:<br>"
			                                                                                    + "- Exhaustion of SQL Engine worker-threads.<br>"
			                                                                                    + "- Detection of an unsolvable deadlock."
			                                                                                    + "<br>"
			                                                                                    + "The failure-condition levels (1-5) range from the least restrictive, level 1, to the most restrictive, level 5. A given condition level encompasses all of the less restrictive levels. Thus, the strictest condition level, 5, includes the four less restrictive condition levels (1-4), level 4 includes levels 1-3, and so forth.<br>"
			                                                                                    + "To change this value, use the FAILURE_CONDITION_LEVEL option of the ALTER AVAILABILITY GROUPTransact-SQL statement."
			                                                                                    + "</p></html>");
			mtd.addColumn("availability_groups_cluster", "health_check_timeout"             , "<html><p>Wait time (in milliseconds) for the sp_server_diagnostics system stored procedure to return server-health information, before the server instance is assumed to be slow or not responding. The default value is 30000 milliseconds (30 seconds).<br><br>To change this value, use the HEALTH_CHECK_TIMEOUT option of ALTER AVAILABILITY GROUPTransact-SQL statement.</p></html>");
			mtd.addColumn("availability_groups_cluster", "automated_backup_preference"      , "<html><p>Preferred location for performing backups on the availability databases in this availability group. One of the following values:<br>br>"
			                                                                                    + "0: Primary. Backups should always occur on the primary replica.<br>"
			                                                                                    + "1: Secondary only. Performing backups on a secondary replica is preferable.<br>"
			                                                                                    + "2: Prefer Secondary. Performing backups on a secondary replica preferable, but performing backups on the primary replica is acceptable if no secondary replica is available for backup operations. This is the default behavior.<br>"
			                                                                                    + "3: Any Replica. No preference about whether backups are performed on the primary replica or on a secondary replica.<br>"
			                                                                                    + "<br>"
			                                                                                    + "For more information, see Active Secondaries: Backup on Secondary Replicas (Always On Availability Groups)."
			                                                                                    + "</p></html>");
			mtd.addColumn("availability_groups_cluster", "automated_backup_preference_desc" , "<html><p>Description of automated_backup_preference, one of:<br><br>"
			                                                                                    + "PRIMARY<br>"
			                                                                                    + "SECONDARY_ONLY<br>"
			                                                                                    + "SECONDARY<br>"
			                                                                                    + "NONE<br>"
			                                                                                    + "</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'availability_groups_cluster' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_read_only_routing_lists
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_read_only_routing_lists",  "<p>Returns a row for the read only routing list of each availability replica in an Always On availability group in the WSFC failover cluster.</p>");

			// Column names and description
			mtd.addColumn("availability_read_only_routing_lists", "replica_id"            , "<html><p>Unique ID of the availability replica that owns the routing list.</p></html>");
			mtd.addColumn("availability_read_only_routing_lists", "routing_priority"      , "<html><p>Priority order for routing (1 is first, 2 is second, and so forth).</p></html>");
			mtd.addColumn("availability_read_only_routing_lists", "read_only_replica_id"  , "<html><p>Unique ID of the availability replica to which a read-only workload will be routed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'availability_read_only_routing_lists' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// availability_replicas
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("availability_replicas",  "<p>Returns a row for each of the availability replicas that belong to any Always On availability group in the WSFC failover cluster.<br><br>If the local server instance is unable to talk to the WSFC failover cluster, for example because the cluster is down or quorum has been lost, only rows for local availability replicas are returned. These rows will contain only the columns of data that are cached locally in metadata.</p>");

			// Column names and description
			mtd.addColumn("availability_replicas", "replica_id"                            , "<html><p>Unique ID of the replica.</p></html>");
			mtd.addColumn("availability_replicas", "group_id"                              , "<html><p>Unique ID of the availability group to which the replica belongs.</p></html>");
			mtd.addColumn("availability_replicas", "replica_metadata_id"                   , "<html><p>ID for the local metadata object for availability replicas in the Database Engine.</p></html>");
			mtd.addColumn("availability_replicas", "replica_server_name"                   , "<html><p>Server name of the instance of SQL Server that is hosting this replica and, for a non-default instance, its instance name.</p></html>");
			mtd.addColumn("availability_replicas", "owner_sid"                             , "<html><p>Security identifier (SID) registered to this server instance for the external owner of this availability replica.<br><br>NULL for non-local availability replicas.</p></html>");
			mtd.addColumn("availability_replicas", "endpoint_url"                          , "<html><p>String representation of the user-specified database mirroring endpoint that is used by connections between primary and secondary replicas for data synchronization. For information about the syntax of endpoint URLs, see Specify the Endpoint URL When Adding or Modifying an Availability Replica (SQL Server).<br><br>NULL = Unable to talk to the WSFC failover cluster.<br><br>To change this endpoint, use the ENDPOINT_URL option of ALTER AVAILABILITY GROUPTransact-SQL statement.</p></html>");
			mtd.addColumn("availability_replicas", "availability_mode"                     , "<html><p>The availability mode of the replica, one of:<br><br>"
			                                                                                   + "0 | Asynchronous commit. The primary replica can commit transactions without waiting for the secondary to write the log to disk.<br>"
			                                                                                   + "1 | Synchronous commit. The primary replica waits to commit a given transaction until the secondary replica has written the transaction to disk.<br>"
			                                                                                   + "4 | Configuration only. The primary replica sends availability group configuration metadata to the replica synchronously. User data is not transmitted to the replica. Available in SQL Server 2017 CU1 and later.<br>"
			                                                                                   + "<br>"
			                                                                                   + "For more information, see Availability Modes (Always On Availability Groups)."
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "availability_mode_desc"                , "<html><p>Description of availability_mode, one of:<br><br>"
			                                                                                   + "ASYNCHRONOUS_COMMIT<br>"
			                                                                                   + "SYNCHRONOUS_COMMIT<br>"
			                                                                                   + "CONFIGURATION_ONLY<br>"
			                                                                                   + "<br>"
			                                                                                   + "To change this the availability mode of an availability replica, use the AVAILABILITY_MODE option of ALTER AVAILABILITY GROUPTransact-SQL statement.<br>"
			                                                                                   + "You cannot change the availability mode of a replica to CONFIGURATION_ONLY. You cannot change a CONFIGURATION_ONLY replica to a secondary or primary replica."
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "failover_mode"                         , "<html><p>The failover mode of the availability replica, one of:<br><br>"
			                                                                                   + "0 | Automatic failover. The replica is a potential target for automatic failovers. Automatic failover is supported only if the availability mode is set to synchronous commit (availability_mode = 1) and the availability replica is currently synchronized.<br>"
			                                                                                   + "1 | Manual failover. A failover to a secondary replica set to manual failover must be manually initiated by the database administrator. The type of failover that is performed will depend on whether the secondary replica is synchronized, as follows:<br>"
			                                                                                   + "If the availability replica is not synchronizing or is still synchronizing, only forced failover (with possible data loss) can occur.<br>"
			                                                                                   + "If the availability mode is set to synchronous commit (availability_mode = 1) and the availability replica is currently synchronized, manual failover without data loss can occur.<br>"
			                                                                                   + "To view a rollup of the database synchronization health of every availability database in an availability replica, use the synchronization_health and synchronization_health_desc columns of the sys.dm_hadr_availability_replica_states dynamic management view. The rollup considers the synchronization state of every availability database and the availability mode of its availability replica.<br>"
			                                                                                   + "<br>"
			                                                                                   + "Note: To view the synchronization health of a given availability database, query the synchronization_state and synchronization_health columns of the sys.dm_hadr_database_replica_states dynamic management view."
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "failover_mode_desc"                    , "<html><p>Description of failover_mode, one of:<br><br>"
			                                                                                   + "MANUAL<br>"
			                                                                                   + "AUTOMATIC<br>"
			                                                                                   + "<br>"
			                                                                                   + "To change the failover mode, use the FAILOVER_MODE option of ALTER AVAILABILITY GROUPTransact-SQL statement."
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "session_timeout"                       , "<html><p>The time-out period, in seconds. The time-out period is the maximum time that the replica waits to receive a message from another replica before considering connection between the primary and secondary replica have failed. Session timeout detects whether secondaries are connected the primary replica.<br><br>"
			                                                                                   + "On detecting a failed connection with a secondary replica, the primary replica considers the secondary replica to be NOT_SYNCHRONIZED. On detecting a failed connection with the primary replica, a secondary replica simply attempts to reconnect.<br><br>"
			                                                                                   + "<b>Note</b>: Session timeouts do not cause automatic failovers.<br><br>"
			                                                                                   + "To change this value, use the SESSION_TIMEOUT option of ALTER AVAILABILITY GROUPTransact-SQL statement."
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "primary_role_allow_connections"        , "<html><p>Whether the availability allows all connections or only read-write connections, one of:<br><br>"
			                                                                                   + "2 = All (default)<br>"
			                                                                                   + "3 = Read write<br>"
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "primary_role_allow_connections_desc"   , "<html><p>Description of primary_role_allow_connections, one of:<br><br>"
			                                                                                   + "ALL<br>"
			                                                                                   + "READ_WRITE<br>"
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "secondary_role_allow_connections"      , "<html><p>Whether an availability replica that is performing the secondary role (that is, a secondary replica) can accept connections from clients, one of:<br><br>"
			                                                                                   + "0 = No. No connections are allowed to the databases in the secondary replica, and the databases are not available for read access. This is the default setting.<br><br>"
			                                                                                   + "1 = Read only. Only read-only connections are allowed to the databases in the secondary replica. All database(s) in the replica are available for read access.<br><br>"
			                                                                                   + "2 = All. All connections are allowed to the databases in the secondary replica for read-only access.<br><br>"
			                                                                                   + "For more information, see Active Secondaries: Readable Secondary Replicas (Always On Availability Groups)."
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "secondary_role_allow_connections_desc" , "<html><p>Description of secondary_role_allow_connections, one of:<br><br>"
			                                                                                   + "NO<br>"
			                                                                                   + "READ_ONLY<br>"
			                                                                                   + "ALL<br>"
			                                                                                   + "</p></html>");
			mtd.addColumn("availability_replicas", "create_date"                           , "<html><p>Date that the replica was created.<br><br>NULL = Replica not on this server instance.</p></html>");
			mtd.addColumn("availability_replicas", "modify_date"                           , "<html><p>Date that the replica was last modified.<br><br>NULL = Replica not on this server instance.</p></html>");
			mtd.addColumn("availability_replicas", "backup_priority"                       , "<html><p>Represents the user-specified priority for performing backups on this replica relative to the other replicas in the same availability group. The value is an integer in the range of 0..100.<br><br>For more information, see Active Secondaries: Backup on Secondary Replicas (Always On Availability Groups).</p></html>");
			mtd.addColumn("availability_replicas", "read_only_routing_url"                 , "<html><p>Connectivity endpoint (URL) of the read only availability replica. For more information, see Configure Read-Only Routing for an Availability Group (SQL Server).</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'availability_replicas' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_auto_page_repair
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_auto_page_repair",  "<p>Returns a row for every automatic page-repair attempt on any availability database on an availability replica that is hosted for any availability group by the server instance. This view contains rows for the latest automatic page-repair attempts on a given primary or secondary database, with a maximum of 100 rows per database. As soon as a database reaches the maximum, the row for its next automatic page-repair attempt replaces one of the existing entries. The following table defines the meaning of the various columns.</p>");

			// Column names and description
			mtd.addColumn("dm_hadr_auto_page_repair", "database_id"      , "<html><p>ID of the database to which this row corresponds.</p></html>");
			mtd.addColumn("dm_hadr_auto_page_repair", "file_id"          , "<html><p>ID of the file in which the page is located.</p></html>");
			mtd.addColumn("dm_hadr_auto_page_repair", "page_id"          , "<html><p>ID of the page in the file.</p></html>");
			mtd.addColumn("dm_hadr_auto_page_repair", "error_type"       , "<html><p>Type of the error. The values can be:</p><p><strong>-</strong>1 = All hardware <a href=\"https://msdn.microsoft.com/en-us/library/aa337267.aspx\">823 errors</a></p><p>1 = <a href=\"https://msdn.microsoft.com/en-us/library/aa337274.aspx\">824 errors</a> other than a bad checksum or a torn page (such as a bad page ID) </p><p>2 = Bad checksum</p><p>3 = Torn page</p></html>");
			mtd.addColumn("dm_hadr_auto_page_repair", "page_status"      , "<html><p>The status of the page-repair attempt:</p><p>2 = Queued for request from partner.</p><p>3 = Request sent to partner.</p><p>4 = Queued for automatic page repair (response received from partner).</p><p>5 = Automatic page repair succeeded and the page should be usable.</p><p>6 = Irreparable. This indicates that an error occurred during page-repair attempt, for example, because the page is also corrupted on the partner, the partner is disconnected, or a network problem occurred. This state is not terminal; if corruption is encountered again on the page, the page will be requested again from the partner.</p></html>");
			mtd.addColumn("dm_hadr_auto_page_repair", "modification_time", "<html><p>Time of last change to the page status.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_auto_page_repair' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_availability_group_states
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_availability_group_states",  "<p>Returns a row for each AlwaysOn availability group that possesses an availability replica on the local instance of SQL Server. Each row displays the states that define the health of a given availability group. </p>");

			// Column names and description
			mtd.addColumn("dm_hadr_availability_group_states", "group_id"                      , "<html><p>Unique identifier of the availability group. </p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "primary_replica"               , "<html><p>Name of the server instance that is hosting the current primary replica.</p><p>NULL = Not the primary replica or unable to communicate with the WSFC failover cluster.</p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "primary_recovery_health"       , "<html><p>Indicates the recovery health of the primary replica, one of:</p><p>0 = In progress</p><p>1 = Online</p><p>NULL</p><p>On secondary replicas the <strong>primary_recovery_health</strong> column is NULL.</p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "primary_recovery_health_desc"  , "<html><p>Description of <strong>primary_replica_health</strong>, one of:</p><p>ONLINE_IN_PROGRESS</p><p>ONLINE</p><p>NULL?</p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "secondary_recovery_health"     , "<html><p>Indicates the recovery health of a secondary replica replica,?one of:</p><p>0 = In progress</p><p>1 = Online</p><p>NULL</p><p>On the primary replica, the <strong>secondary_recovery_health</strong> column is NULL.</p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "secondary_recovery_health_desc", "<html><p>Description of <strong>secondary_recovery_health</strong>, one of:</p><p>ONLINE_IN_PROGRESS</p><p>ONLINE</p><p>NULL</p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "synchronization_health"        , "<html><p>Reflects a rollup of the <strong>synchronization_health</strong> of all availability replicas in the availability group, one of:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>Not healthy. None of the availability replicas have a healthy <strong>synchronization_health</strong> (2 = HEALTHY).</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Partially healthy. The synchronization health of some, but not all, availability replicas is healthy. </p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Healthy. The synchronization health of every availability replica is healthy.</p></td>    </tr>   </tbody>  </table> </div></div><p>For information about replica synchronization health, see the <strong>synchronization_health</strong> column in <a href=\"https://msdn.microsoft.com/en-us/library/ff878537.aspx\">sys.dm_hadr_availability_replica_states (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_hadr_availability_group_states", "synchronization_health_desc"   , "<html><p>Description of <strong>synchronization_health</strong>, one of:</p><p>NOT_HEALTHY</p><p>PARTIALLY_HEALTHY</p><p>HEALTHY</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_availability_group_states' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_availability_replica_cluster_nodes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_availability_replica_cluster_nodes",  "<p>Returns a row for every availability replica (regardless of join state) of the AlwaysOn availability groups in the Windows Server Failover Clustering (WSFC) cluster.</p>");

			// Column names and description
			mtd.addColumn("dm_hadr_availability_replica_cluster_nodes", "group_name"         , "<html><p>Name of the availability group.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_cluster_nodes", "replica_server_name", "<html><p>Name of the instance of SQL Server hosting the replica.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_cluster_nodes", "node_name"          , "<html><p>Name of the cluster node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_availability_replica_cluster_nodes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_availability_replica_cluster_states
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_availability_replica_cluster_states",  "<p>Returns a row for each AlwaysOn availability replica (regardless of its join state) of all AlwaysOn availability groups (regardless of replica location) in the Windows Server Failover Clustering (WSFC) cluster. </p>");

			// Column names and description
			mtd.addColumn("dm_hadr_availability_replica_cluster_states", "replica_id"         , "<html><p>Unique identifier of the availability replica.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_cluster_states", "replica_server_name", "<html><p>Name of the instance of SQL Server hosting the replica.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_cluster_states", "group_id"           , "<html><p>Unique identifier of the availability group.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_cluster_states", "join_state"         , "<html><p>0 = Not joined</p><p>1 = Joined, standalone instance</p><p>2 = Joined, failover cluster instance</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_cluster_states", "join_state_desc"    , "<html><p>NOT_JOINED</p><p>JOINED_STANDALONE_INSTANCE</p><p>JOINED_FAILOVER_CLUSTER_INSTANCE</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_availability_replica_cluster_states' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_availability_replica_states
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_availability_replica_states",  "<p>Returns a row for each local availability replica and a row for each remote availability replica in the same AlwaysOn availability group as a local replica. Each row contains information about the state of a given availability replica.</p>");

			// Column names and description
			mtd.addColumn("dm_hadr_availability_replica_states", "replica_id"                    , "<html><p>Unique identifier of the availability replica.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "group_id"                      , "<html><p>Unique identifier of the availability group.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "is_local"                      , "<html><p>Whether the availability replica is local, one of:</p><p>0 = Indicates a remote secondary replica in an availability group whose primary replica is hosted by the local server instance. This value occurs only on the primary replica location. ?</p><p>1 = Indicates a local availability replica. On secondary replicas, this is the only available value for the availability group to which the replica belongs.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "role"                          , "<html><p>Current AlwaysOn Availability Groups role of a local availability replica or a connected remote availability replica, one of:</p><p>0 = Resolving</p><p>1 = Primary </p><p>2 = Secondary </p><p>For information about AlwaysOn Availability Groups roles, see <a href=\"https://msdn.microsoft.com/en-us/library/ff877884.aspx\">Overview of AlwaysOn Availability Groups (SQL Server)</a>.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "role_desc"                     , "<html><p>Description of <strong>role</strong>, one of:</p><p>RESOLVING</p><p>PRIMARY</p><p>SECONDARY</p><p></p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "operational_state"             , "<html><p>Current operational state of the availability replica, one of: </p><p>0 = Pending failover</p><p>1 = Pending</p><p>2 = Online </p><p>3 = Offline</p><p>4 = Failed</p><p>5 = Failed, no quorum</p><p>NULL = Replica is not local.</p><p>For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ff878537.aspx#RolesAndOperationalStates\">Roles and Operational States</a>, later in this topic. </p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "operational_state_desc"        , "<html><p>Description of <strong>operational_state</strong>, one of:</p><p>PENDING_FAILOVER</p><p>PENDING</p><p>ONLINE</p><p>OFFLINE</p><p>FAILED</p><p>FAILED_NO_QUORUM</p><p>NULL</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "recovery_health"               , "<html><p>Rollup of the <strong>database_state</strong> column of the <a href=\"https://msdn.microsoft.com/en-us/library/ff877972.aspx\">sys.dm_hadr_database_replica_states</a> dynamic management view, one of: </p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>In progress. At least one joined database has a database state other than ONLINE (<strong>database_state</strong> is not 0).</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Online. All the joined databases have a database state of ONLINE (<strong>database_state</strong> is 0).</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>NULL</p></td>     <td data-th=\"Description\"><p><strong>is_local</strong> = 0</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "recovery_health_desc"          , "<html><p>Description of <strong>recovery_health</strong>, one of:</p><p>ONLINE_IN_PROGRESS</p><p>ONLINE</p><p>NULL</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "synchronization_health"        , "<html><p>Reflects a rollup of the database synchronization state (<strong>synchronization_state</strong>)of all joined availability databases (also known as \"database replicas\") and the availability mode of the availability replica (synchronous-commit or asynchronous-commit mode). The rollup will reflect the least healthy accumulated state the databases on the availability replica.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>Not healthy. At least one joined database is in the NOT SYNCHRONIZING state. </p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Partially healthy. Some replicas are not in the target synchronization state: synchronous-commit replicas should be synchronized, and asynchronous-commit replicas should be synchronizing. ?</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Healthy. All replicas are in the target synchronization state: synchronous-commit replicas are synchronized, and asynchronous-commit replicas are synchronizing. ?</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "synchronization_health_desc"   , "<html><p>Description of <strong>synchronization_health</strong>, one of:</p><p>NOT_HEALTHY</p><p>PARTIALLY_HEALTHY</p><p>HEALTHY</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "connected_state"               , "<html><p>Whether a secondary replica is currently connected to the primary replica, one of:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>Disconnected. The response of an availability replica to the DISCONNECTED state depends on its role, as follows:</p>      <ul class=\"unordered\">       <li><p>On the primary replica, if a secondary replica is disconnected, its secondary databases are marked as NOT SYNCHRONIZED on the primary replica, which waits for the secondary to reconnect.</p></li>       <li><p>On a secondary replica, upon detecting that it is disconnected, the secondary replica attempts to reconnect to the primary replica.</p></li>      </ul></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Connected. </p></td>    </tr>   </tbody>  </table> </div></div><p>Each primary replica tracks the connection state for every secondary replica in the same availability group. Secondary replicas track the connection state of only the primary replica.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "connected_state_desc"          , "<html><p>Description of <strong>connection_state</strong>, one of:</p><p>DISCONNECTED</p><p>CONNECTED</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "last_connect_error_number?"    , "<html><p>Number of the last connection error.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "last_connect_error_description", "<html><p>Text of the <strong>last_connect_error_number</strong> message.</p></html>");
			mtd.addColumn("dm_hadr_availability_replica_states", "last_connect_error_timestamp"  , "<html><p>Date and time timestamp indicating when the <strong>last_connect_error_number</strong> error occurred.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_availability_replica_states' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_cluster
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_cluster",  "<p>If the Windows Server Failover Clustering (WSFC) node that hosts an instance of SQL Server that is enabled for AlwaysOn Availability Groups has WSFC quorum, <strong>sys.dm_hadr_cluster</strong> returns a row that exposes the cluster name and information about the quorum. If the WSFC node has no quorum, no row is returned. </p>");

			// Column names and description
			mtd.addColumn("dm_hadr_cluster", "cluster_name"     , "<html><p>Name of the WSFC cluster that hosts the instances of SQL Server that are enabled for AlwaysOn Availability Groups. </p></html>");
			mtd.addColumn("dm_hadr_cluster", "quorum_type"      , "<html><p>Type of quorum used by this WSFC cluster, one of:</p><p>0 = Node Majority. This quorum configuration can sustain failures of half the nodes (rounding up) minus one. For example, on a seven node cluster, this quorum configuration can sustain three node failures.</p><p>1 = Node and Disk Majority. If the disk witness remains on line, this quorum configuration can sustain failures of half the nodes (rounding up). For example, a six node cluster in which the disk witness is online could sustain three node failures. If the disk witness goes offline or fails, this quorum configuration can sustain failures of half the nodes (rounding up) minus one. For example, a six node cluster with a failed disk witness could sustain two (3-1=2) node failures.</p><p>2 = Node and File Share Majority. This quorum configuration works in a similar way to Node and Disk Majority, but uses a file-share witness instead of a disk witness.</p><p>3 = No Majority: Disk Only. If the quorum disk is online, this quorum configuration can sustain failures of all nodes except one. </p></html>");
			mtd.addColumn("dm_hadr_cluster", "quorum_type_desc" , "<html><p>Description of <strong>quorum_type</strong>, one of:</p><p>NODE_MAJORITY</p><p>NODE_AND_DISK_MAJORITY</p><p>NODE_AND_FILE_SHARE_MAJORITY</p><p>NO_MAJORITY:_DISK_ONLY</p></html>");
			mtd.addColumn("dm_hadr_cluster", "quorum_state"     , "<html><p>State of the WSFC quorum, one of:</p><p>0 = Unknown quorum state</p><p>1 = Normal quorum </p><p>2 = Forced quorum </p></html>");
			mtd.addColumn("dm_hadr_cluster", "quorum_state_desc", "<html><p>Description of <strong>quorum_state</strong>, one of:</p><p>UNKNOWN_QUORUM_STATE</p><p>NORMAL_QUORUM</p><p>FORCED_QUORUM</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_cluster' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_cluster_members
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_cluster_members",  "<p>If the WSFC node that hosts a local instance of SQL Server that is enabled for AlwaysOn Availability Groups has WSFC quorum, returns a row for each of the members that constitute the quorum and the state of each of them. This includes of all nodes in the cluster (returned with CLUSTER_ENUM_NODE type by the <strong>Clusterenum</strong> function) and the disk or file-share witness, if any. The row returned for a given member contains information about the state of that member. For example, for a five node cluster with majority node quorum in which one node is down, when <strong>sys.dm_hadr_cluster_members</strong> is queried from a server instance that is that is enabled for AlwaysOn Availability Groups that resides on a node with quorum, <strong>sys.dm_hadr_cluster_members</strong> reflects the state of the down node as \"NODE_DOWN\".</p><p>If the WSFC node has no quorum, no rows are returned. </p><p>Use this dynamic management view to answer the following questions:</p><ul class=\"unordered\"> <li><p>What nodes are currently running on the WSFC cluster?</p></li> <li><p>How many more failures can the WSFC cluster tolerate before losing quorum in a majority-node case?</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_hadr_cluster_members", "member_name"           , "<html><p>Member name, which can be a computer name, a drive letter, or a file share path.</p></html>");
			mtd.addColumn("dm_hadr_cluster_members", "member_type"           , "<html><p>The type of member, one of:</p><p>0 = WSFC node</p><p>1 = Disk witness</p><p>2 = File share witness</p></html>");
			mtd.addColumn("dm_hadr_cluster_members", "member_type_desc"      , "<html><p>Description of <strong>member_type</strong>, one of:</p><p>CLUSTER_NODE</p><p>DISK_WITNESS</p><p>FILE_SHARE_WITNESS</p></html>");
			mtd.addColumn("dm_hadr_cluster_members", "member_state"          , "<html><p>The member state, one of:</p><p>0 = Offline</p><p>1 = Online</p></html>");
			mtd.addColumn("dm_hadr_cluster_members", "member_state_desc"     , "<html><p>Description of <strong>member_state</strong>, one of:</p><p>OFFLINE</p><p>ONLINE</p></html>");
			mtd.addColumn("dm_hadr_cluster_members", "number_of_quorum_votes", "<html><p>Number of quorum votes possessed by this quorum member. For No Majority: Disk Only quorums, this value defaults to 0. For other quorum types, this value defaults to 1. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_cluster_members' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_cluster_networks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_cluster_networks",  "<p>Returns a row for every WSFC cluster member that is participating in an availability group's subnet configuration. You can use this dynamic management view to validate the network virtual IP that is configured for each availability replica. </p><p>Primary key:??<strong>member_name</strong> + <strong>network_subnet_IP</strong> + <strong>network_subnet_prefix_length</strong></p>");

			// Column names and description
			mtd.addColumn("dm_hadr_cluster_networks", "member_name"                 , "<html><p>A computer name of a node in the WSFC cluster. </p></html>");
			mtd.addColumn("dm_hadr_cluster_networks", "network_subnet_ip"           , "<html><p>Network IP address of the subnet to which the computer belongs. This can be an IPv4 or IPv6 address.</p></html>");
			mtd.addColumn("dm_hadr_cluster_networks", "network_subnet_ipv4_mask"    , "<html><p>Network subnet mask that specifies the subnet to which the IP address belongs. <strong>network_subnet_ipv4_mask</strong> to specify the DHCP &lt;network_subnet_option&gt; options in a WITH DHCP clause of the <a href=\"https://msdn.microsoft.com/en-us/library/ff878399.aspx\">CREATE AVAILABILITY GROUP</a> or <a href=\"https://msdn.microsoft.com/en-us/library/ff878601.aspx\">ALTER AVAILABILITY GROUP</a>?Transact-SQL statement.</p><p>NULL = IPv6 subnet.</p></html>");
			mtd.addColumn("dm_hadr_cluster_networks", "network_subnet_prefix_length", "<html><p>Network IP prefix length that specifies the subnet to which the computer belongs. ?</p><p></p></html>");
			mtd.addColumn("dm_hadr_cluster_networks", "is_public"                   , "<html><p>Whether the network is private or public on the WSFC cluster, one of:</p><p>0 = Private</p><p>1 = Public</p></html>");
			mtd.addColumn("dm_hadr_cluster_networks", "is_ipv4"                     , "<html><p>Type of the subnet, one of:</p><p>1 = IPv4</p><p>0 = IPv6</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_cluster_networks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_database_replica_cluster_states
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_database_replica_cluster_states",  "<p>Returns a row containing information intended to provide you with insight into the health of the availability databases in the AlwaysOn availability groups in each AlwaysOn availability group on the Windows Server Failover Clustering (WSFC) cluster. Query <strong>sys.dm_hadr_database_replica_states</strong> to answer the following questions:</p><ul class=\"unordered\"> <li><p>Are all databases in an availability group ready for a failover?</p></li> <li><p>After a forced failover, has a secondary database suspended itself locally and acknowledged its suspended state to the new primary replica?</p></li> <li><p>If the primary replica is currently unavailable, which secondary replica would allow the minimum data loss if it becomes the primary replica? </p></li> <li><p>When the value of the ?<strong>log_reuse_wait_desc</strong> column is \"AVAILABILITY_REPLICA\", which secondary replica in an availability group is holding up log truncation on a given primary database?<span class=\"buildwarning\" id=\"Warn_cffd93a1-0fa3-40c5-bb2c-f873800db252\"><a href=\"https://msdn.microsoft.com/en-us/library/hh213319.aspx#46c288c1-3410-4d68-a027-3bbf33239289\">Bookmark may solve the problem.</a></span></p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "replica_id"                  , "<html><p>Identifier of the availability replica within the availability group.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "group_database_id"           , "<html><p>Identifier of the database within the availability group. This identifier is identical on every replica to which this database is joined.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "database_name"               , "<html><p>Name of a database that belongs to the availability group.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "is_failover_ready"           , "<html><p>Indicates whether the secondary database is synchronized with the corresponding primary database. one of: </p><p>0 = The database is not marked as synchronized in the cluster. The database is not ready for a failover.</p><p>1 = The database is marked as synchronized in the cluster. The database is ready for a failover.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "is_pending_secondary_suspend", "<html><p>Indicates whether, after a forced failover, the database is pending suspension, one of:??</p><p>0 = Any states except for HADR_SYNCHRONIZED_ SUSPENDED.</p><p>1 = HADR_SYNCHRONIZED_ SUSPENDED. When a forced failover completes, each of the secondary databases is set to HADR_SYNCHONIZED_SUSPENDED and remains in this state until the new primary replica receives an acknowledgement from that secondary database to the SUSPEND message. </p><p>NULL = Unknown (no quorum)</p><p></p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "is_database_joined"          , "<html><p>Indicates whether the database on this availability replica has been joined to the availability group, one of:</p><p>0 = Database is not joined to the availability group on this availability replica.</p><p>1 = Database is joined to the availability group on this availability replica.</p><p>NULL = unknown (The availability replica lacks quorum.)</p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "recovery_lsn"                , "<html><p>On the primary replica, the end of the transaction log before the replica writes any new log records after recovery or failover. On the primary replica, the row for a given secondary database will have the value to which the primary replica needs the secondary replica to synchronize to (that is, to revert to and reinitialize to).</p><p>On secondary replicas this value is NULL. Note that each secondary replica will have either the MAX value or a lower value that the primary replica has told the secondary replica to go back to. </p></html>");
			mtd.addColumn("dm_hadr_database_replica_cluster_states", "truncation_lsn"              , "<html><p>The AlwaysOn Availability Groups log truncation value, which may be higher than the local truncation LSN if local log truncation is blocked (such as by a backup operation). ?</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_database_replica_cluster_states' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_database_replica_states
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_database_replica_states",  "<p>Returns a row for each database that is participating in an AlwaysOn availability group for which the local instance of SQL Server is hosting an availability replica. This dynamic management view exposes state information on both the primary and secondary replicas. On a secondary replica, this view returns a row for every secondary database on the server instance. On the primary replica, this view returns a row for each primary database and an additional row for the corresponding secondary database. </p>");

			// Column names and description
			mtd.addColumn("dm_hadr_database_replica_states", "database_id"                , "<html><p>Identifier of the database, unique within an instance of SQL Server. This is the same value as displayed in the <a href=\"https://msdn.microsoft.com/en-us/library/ms178534.aspx\">sys.databases</a> catalog view.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "group_id"                   , "<html><p>Identifier of the availability group to which the database belongs. </p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "replica_id"                 , "<html><p>Identifier of the availability replica within the availability group.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "group_database_id"          , "<html><p>Identifier of the database within the availability group. This identifier is identical on every replica to which this database is joined.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "is_local"                   , "<html><p>Whether the availability database is local, one of:</p><p>0 = The database is not local to the SQL Server instance.</p><p>1 = The database is local to the server instance.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "is_primary_replica"         , "<html><p>Returns 1 if the replica is primary, or 0 if it is a secondary replica.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "synchronization_state"      , "<html><p>Data-movement state, one of: </p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p>Value</p></td>     <td><p>Description</p></td>    </tr>    <tr>     <td><p>0</p></td>     <td><p>Not synchronizing.</p>      <ul class=\"unordered\">       <li><p>For a primary database, indicates that the database is not ready to synchronize its transaction log with the corresponding secondary databases.?</p></li>       <li><p>For a secondary database, indicates that the database has not started log synchronization because of a connection issue, is being suspended, or is going through transition states during startup or a role switch. </p></li>      </ul></td>    </tr>    <tr>     <td><p>1</p></td>     <td><p>Synchronizing.</p>      <ul class=\"unordered\">       <li><p>For a primary database, indicates that the database is ready to accept a scan request from a secondary database.</p></li>       <li><p>For a secondary database, indicates that active data movement is occurring for the database. </p></li>      </ul></td>    </tr>    <tr>     <td><p>2</p></td>     <td><p>Synchronized.</p>      <ul class=\"unordered\">       <li><p>A primary database shows SYNCHRONIZED in place of SYNCHRONIZING.</p></li>       <li><p>A synchronous-commit secondary database shows synchronized when the local cache says the database is failover ready and is synchronizing.</p></li>      </ul></td>    </tr>    <tr>     <td><p>3</p></td>     <td><p>Reverting. Indicates the phase in the undo process when a secondary database is actively getting pages from the primary database.</p>      <div class=\"alert\">       <div class=\"contentTableWrapper\">        <table>         <tbody>          <tr>           <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-caution\" alt=\"System_CAPS_caution\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC1429.jpeg\" title=\"System_CAPS_caution\" xmlns=\"\"></span><span class=\"alertTitle\">Caution </span></th>          </tr>          <tr>           <td><p>When a database on a secondary replica is in the REVERTING state, forcing failover to the secondary replica leaves the database in a state in which it cannot be started as a primary database. Either the database will need to reconnect as a secondary database, or you will need to apply new log records from a log backup.</p></td>          </tr>         </tbody>        </table>       </div>      </div></td>    </tr>    <tr>     <td><p>4</p></td>     <td><p>Initializing. Indicates the phase of undo when the transaction log required for a secondary database to catch up to the undo LSN is being shipped and hardened on a secondary replica. </p>      <div class=\"alert\">       <div class=\"contentTableWrapper\">        <table>         <tbody>          <tr>           <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-caution\" alt=\"System_CAPS_caution\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC1429.jpeg\" title=\"System_CAPS_caution\" xmlns=\"\"></span><span class=\"alertTitle\">Caution </span></th>          </tr>          <tr>           <td><p>When a database on a secondary replica is in the INITIALIZING state, forcing failover to the secondary replica leaves the database in a state in which it be started as a primary database. Either the database will need to reconnect as a secondary database, or you will need to apply new log records from a log backup.</p></td>          </tr>         </tbody>        </table>       </div>      </div></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "synchronization_state_desc" , "<html><p>Description of the data-movement state, one of: </p><p>NOT SYNCHRONIZING</p><p>SYNCHRONIZING</p><p>SYNCHRONIZED</p><p>REVERTING</p><p>INITIALIZING</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "is_commit_participant"      , "<html><p>0 = Transaction commit is not synchronized with respect to this database. </p><p>1 = Transaction commit is synchronized with respect to this database.</p><p>For a database on an asynchronous-commit availability replica, this value is always 0. </p><p>For a database on a synchronous-commit availability replica, this value is accurate only on the primary database. ?</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "synchronization_health"     , "<html><p>Reflects the intersection of the synchronization state of a database that is joined to the availability group on the availability replica and the availability mode of the availability replica (synchronous-commit or asynchronous-commit mode), one of: </p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>Not healthy. The <strong>synchronization_state</strong> of the database is 0 (NOT SYNCHRONIZING). </p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Partially healthy. A database on a synchronous-commit availability replica is considered partially healthy if <strong>synchronization_state</strong> is 1 (SYNCHRONIZING).</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Healthy. A database on an synchronous-commit availability replica is considered healthy if <strong>synchronization_state</strong> is 2 (SYNCHRONIZED), and a database on an asynchronous-commit availability replica is considered healthy if <strong>synchronization_state</strong> is 1 (SYNCHRONIZING).</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "synchronization_health_desc", "<html><p>Description of the <strong>synchronization_health</strong> of the availability database.</p><p>NOT_HEALTHY</p><p>PARTIALLY_HEALTHY</p><p>HEALTHY</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "database_state"             , "<html><p>0 = Online</p><p>1 = Restoring</p><p>2 = Recovering</p><p>3 = Recovery pending</p><p>4 = Suspect</p><p>5 = Emergency</p><p>6 = Offline</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Same as <strong>state</strong> column in <span class=\"database\">sys.databases</span>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "database_state_desc"        , "<html><p>Description of the <strong>database_state</strong> of the availability replica.</p><p>ONLINE</p><p>RESTORING</p><p>RECOVERING</p><p>RECOVERY_PENDING</p><p>SUSPECT</p><p>EMERGENCY</p><p>OFFLINE</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Same as <strong>state</strong> column in <span class=\"database\">sys.databases</span>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "is_suspended"               , "<html><p>Database state, one of: </p><p>0 = Resumed</p><p>1 = Suspended</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "suspend_reason"             , "<html><p>If the database is suspended, the reason for the suspended state, one of:</p><p>0 = User action</p><p>1 = Suspend from partner</p><p>2 = Redo</p><p>3 = Capture</p><p>4 = Apply</p><p>5 = Restart</p><p>6 = Undo</p><p>7 = Revalidation</p><p>8 = Error in the calculation of the secondary-replica synchronization point</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "suspend_reason_desc"        , "<html><p>Description of the database suspended state reason, one of:</p><p>SUSPEND_FROM_USER = A user manually suspended data movement</p><p>SUSPEND_FROM_PARTNER = The database replica is suspended after a forced failover</p><p>SUSPEND_FROM_REDO = An error occurred during the redo phase</p><p>SUSPEND_FROM_APPLY = An error occurred when writing the log to file (see error log)</p><p>SUSPEND_FROM_CAPTURE = An error occurred while capturing log on the primary replica</p><p>SUSPEND_FROM_RESTART = The database replica was suspended before the database was restarted (see error log)</p><p>SUSPEND_FROM_UNDO = An error occurred during the undo phase (see error log)</p><p>SUSPEND_FROM_REVALIDATION = Log change mismatch is detected on reconnection (see error log)</p><p>SUSPEND_FROM_XRF_UPDATE = Unable to find the common log point (see error log)</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "recovery_lsn"               , "<html><p>On the primary replica, the end of the transaction log before the primary database writes any new log records after recovery or failover. For a given secondary database, if this value is less than the current hardened LSN (last_hardened_lsn), recovery_lsn is the value to which this secondary database would need to resynchronize (that is, to revert to and reinitialize to). If this value is greater than or equal to the current hardened LSN, resynchronization would be unnecessary and would not occur. </p><p><strong>recovery_lsn</strong> reflects a log-block ID padded with zeroes. It is not an actual log sequence number (LSN). For information about how this value is derived, see <a href=\"https://msdn.microsoft.com/en-us/library/ff877972.aspx#LSNcolumns\">Understanding the LSN Column Values</a>, later in this topic.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "truncation_lsn"             , "<html><p>On the primary replica, for the primary database, reflects the minimum log truncation LSN across all the corresponding secondary databases. If local log truncation is blocked (such as by a backup operation), this LSN might be higher than the local truncation LSN. ?</p><p>For a given secondary database, reflects the truncation point of that database.</p><p><strong>truncation_lsn</strong> reflects a log-block ID padded with zeroes. It is not an actual log sequence number.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_sent_lsn"              , "<html><p>The log block identifier that indicates the point up to which all log blocks have been sent by the primary. This is the ID of the next log block that will be sent, rather than the ID of the most recently sent log block. </p><p><strong>last_sent_lsn</strong> reflects a log-block ID padded with zeroes, It is not an actual log sequence number.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_sent_time"             , "<html><p>Time when the last log block was sent.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_received_lsn"          , "<html><p>Log block ID identifying the point up to which all log blocks have been received by the secondary replica that hosts this secondary database. </p><p><strong>last_received_lsn</strong> reflects a log-block ID padded with zeroes. It is not an actual log sequence number. </p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_received_time"         , "<html><p>Time when the log block ID in last message received was read on the secondary replica.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_hardened_lsn"          , "<html><p>Start of the Log Block containing the log records of last hardened LSN on a secondary database. ?</p><p>On an asynchronous-commit primary database or on a synchronous-commit database whose current policy is \"delay\", the value is NULL. For other synchronous-commit primary databases, <strong>last_hardened_lsn</strong> indicates the minimum of the hardened LSN across all the secondary databases. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p><strong>last_hardened_lsn</strong> reflects a log-block ID padded with zeroes. It is not an actual log sequence number. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ff877972.aspx#LSNcolumns\">Understanding the LSN Column Values</a>, later in this topic.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_hardened_time"         , "<html><p>On a secondary database, time of the log-block identifier for the last hardened LSN (<strong>last_hardened_lsn</strong>). On a primary database, reflects the time corresponding to minimum hardened LSN.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_redone_lsn"            , "<html><p>Actual log sequence number of the last log record that was redone on the secondary database. <strong>last_redone_lsn</strong> is always less than <strong>last_hardened_lsn</strong>.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_redone_time"           , "<html><p>Time when the last log record was redone on the secondary database.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "log_send_queue_size"        , "<html><p>Amount of log records of the primary database that has not been sent to the secondary databases, in kilobytes (KB).</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "log_send_rate"              , "<html><p>Rate at which log records are being sent to the secondary databases, in kilobytes (KB)/second.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "redo_queue_size"            , "<html><p>Amount of log records in the log files of the secondary replica that has not yet been redone, in kilobytes (KB).</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "redo_rate"                  , "<html><p>Rate at which the log records are being redone on a given secondary database, in kilobytes (KB)/second.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "filestream_send_rate"       , "<html><p>The rate at which the FILESTREAM files are shipped to the secondary replica, in kilobytes (KB)/second.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "end_of_log_lsn"             , "<html><p>Local end of log LSN. Actual LSN corresponding to the last log record in the log cache on the primary and secondary databases. On the primary replica, the secondary rows reflect the end of log LSN from the latest progress messages that the secondary replicas have sent to the primary replica.</p><p><strong>end_of_log_lsn</strong> reflects a log-block ID padded with zeroes. It is not an actual log sequence number. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ff877972.aspx#LSNcolumns\">Understanding the LSN Column Values</a>, later in this topic.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_commit_lsn"            , "<html><p>Actual log sequence number corresponding to the last commit record in the transaction log. </p><p>On the primary database, this corresponds to the last commit record processed. Rows for secondary databases show the log sequence number that the secondary replica has sent to the primary replica.</p><p>On the secondary replica, this is the last commit record that was redone.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "last_commit_time"           , "<html><p>Time corresponding to the last commit record. </p><p>On the secondary database, this time is the same as on the primary database. </p><p>On the primary replica, each secondary database row displays the time that the secondary replica that hosts that secondary database has reported back to the primary replica. The difference in time between the primary-database row and a given secondary-database row represents approximately the recovery time objective (RPO), assuming that the redo process is caught up and that the progress has been reported back to the primary replica by the secondary replica.</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "low_water_mark_for_ghosts"  , "<html><p>A monotonically increasing number for the database indicating a low water mark used by ghost cleanup on the primary database. If this number is not increasing over time, it implies that ghost cleanup might not happen. To decide which ghost rows to clean up, the primary replica uses the minimum value of this column for this database across all availability replicas (including the primary replica).</p></html>");
			mtd.addColumn("dm_hadr_database_replica_states", "secondary_lag_seconds"      , "<html><p>The number of seconds that the secondary replica is behind the primary replica during synchronization.<br>Applies to: SQL Server 2016 (13.x) and later.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_database_replica_states' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_instance_node_map
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_instance_node_map",  "<p>For every instance of SQL Server that hosts an availability replica that is joined to its AlwaysOn availability group, returns the name of the Windows Server Failover Clustering (WSFC) node that hosts the server instance. This dynamic management view has the following uses:</p><ul class=\"unordered\"> <li><p>This dynamic management view is useful for detecting an availability group with multiple availability replicas that are hosted on the same WSFC node, which is an unsupported configuration that could occur after an FCI failover if the availability group is incorrectly configured. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ff929171.aspx\">Failover Clustering and AlwaysOn Availability Groups (SQL Server)</a>.</p></li> <li><p>When multiple SQL Server instances are hosted on the same WSFC node, the Resource DLL uses this dynamic management view to determine the instance of SQL Server to connect to. </p></li></ul><div class=\"contentTableWrapper\"> <table>  <tbody>   <tr>    <td><p><strong></strong></p></td>   </tr>  </tbody> </table></div>");

			// Column names and description
			mtd.addColumn("dm_hadr_instance_node_map", "ag_resource_id", "<html><p>Unique ID of the availability group as a resource in the WSFC cluster.</p></html>");
			mtd.addColumn("dm_hadr_instance_node_map", "instance_name" , "<html><p>Name?<em>server</em>/<em>instance</em>?of a server instance that hosts a replica for the availability group.</p></html>");
			mtd.addColumn("dm_hadr_instance_node_map", "node_name"     , "<html><p>Name of the WSFC cluster node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_instance_node_map' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_hadr_name_id_map
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_hadr_name_id_map",  "<p>Shows the mapping of AlwaysOn availability groups that the current instance of SQL Server has joined to three unique IDs: an availability group ID, a WSFC resource ID, and a WSFC Group ID. The purpose of this mapping is to handle the scenario in which the WSFC resource/group is renamed.</p>");

			// Column names and description
			mtd.addColumn("dm_hadr_name_id_map", "ag_name"       , "<html><p>Name of the availability group. This is a user-specified name that must be unique within the Windows Server Failover Cluster (WSFC) cluster.</p></html>");
			mtd.addColumn("dm_hadr_name_id_map", "ag_id"         , "<html><p>Unique identifier (GUID) of the availability group.</p></html>");
			mtd.addColumn("dm_hadr_name_id_map", "ag_resource_id", "<html><p>Unique ID of the availability group as a resource in the WSFC cluster.</p></html>");
			mtd.addColumn("dm_hadr_name_id_map", "ag_group_id"   , "<html><p>Unique WSFC Group ID of the availability group.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_hadr_name_id_map' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tcp_listener_states
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tcp_listener_states",  "<p>Returns a row containing dynamic-state information for each TCP listener.?</p>");

			// Column names and description
			mtd.addColumn("dm_tcp_listener_states", "listener_id", "<html><p>Listener?s internal ID. Is not nullable.</p><p>Primary key.</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "ip_address" , "<html><p>The listener IP address that is online and currently being listening to. Either IPv4 and IPv6 is allowed. If a listener possesses both types of addresses, they are listed separately. An IPv4 wildcard, is displayed as ?0.0.0.0?. An IPv6 wildcard, is displayed as ?::?. </p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "is_ipv4"    , "<html><p>Type of IP address</p><p>1 = IPv4</p><p>0 = IPv6</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "port"       , "<html><p>The port number on which the listener is listening. Is not nullable.</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "type"       , "<html><p>Listener type, one of:</p><p>0 = Transact-SQL</p><p>1 = Service Broker</p><p>2 = Database mirroring?</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "type_desc"  , "<html><p>Description of the <strong>type</strong>, one of:</p><p>TSQL</p><p>SERVICE_BROKER</p><p>DATABASE_MIRRORING</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "state"      , "<html><p>State of the availability group listener, one of:</p><p>1 = Online. The listener is listening and processing requests.</p><p>2 = Pending restart. the listener is offline, pending a restart.</p><p>If the availability group listener is listening to the same port as the server instance, these two listeners always have the same state.</p><p>Is not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The values in this column come from the <span class=\"literal\">TSD_listener</span> object. The column does not support an offline state because when the <span class=\"literal\">TDS_listener</span> is offline, it cannot be queried for state. </p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tcp_listener_states", "state_desc" , "<html><p>Description of <strong>state</strong>, one of:</p><p>ONLINE</p><p>PENDING_RESTART</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_tcp_listener_states", "start_time" , "<html><p>Timestamp indicating when the listener was started. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tcp_listener_states' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_cdc_log_scan_sessions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_cdc_log_scan_sessions",  "<p>Returns one row for each log scan session in the current database. The last row returned represents the current session. You can use this view to return status information about the current log scan session, or aggregated information about all sessions since the instance of SQL Server was last started.</p>");

			// Column names and description
			mtd.addColumn("dm_cdc_log_scan_sessions", "session_id"           , "<html><p>ID of the session.</p><p>0 = the data returned in this row is an aggregate of all sessions since the instance of SQL Server was last started.?</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "start_time"           , "<html><p>Time the session began.</p><p>When <strong>session_id</strong> = 0, the time aggregated data collection began.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "end_time"             , "<html><p>Time the session ended.</p><p>NULL = session is active.</p><p>When <strong>session_id</strong> = 0, the time the last session ended.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "duration"             , "<html><p>The duration (in seconds) of the session.</p><p>0 = the session does not contain change data capture transactions.</p><p>When <strong>session_id</strong> = 0, the sum of the duration (in seconds) of all sessions with change data capture transactions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "scan_phase"           , "<html><p>The current phase of the session. One of the following:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Phase</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Phase\"><p>1</p></td>     <td data-th=\"Description\"><p>Reading configuration</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>2</p></td>     <td data-th=\"Description\"><p>First scan, building hash table</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>3</p></td>     <td data-th=\"Description\"><p>Second scan</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>4</p></td>     <td data-th=\"Description\"><p>Second scan</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>5</p></td>     <td data-th=\"Description\"><p>Second scan</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>6</p></td>     <td data-th=\"Description\"><p>Schema versioning</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>7</p></td>     <td data-th=\"Description\"><p>Last scan</p></td>    </tr>    <tr>     <td data-th=\"Phase\"><p>8</p></td>     <td data-th=\"Description\"><p>Done</p></td>    </tr>   </tbody>  </table> </div></div><p>When <strong>session_id</strong> = 0, this value is always \"Aggregate\".</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "error_count"          , "<html><p>Number of errors encountered. </p><p>When <strong>session_id</strong> = 0, the total number of errors in all sessions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "start_lsn"            , "<html><p>Starting LSN for the session.</p><p>When <strong>session_id</strong> = 0, the starting LSN for the last session.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "current_lsn"          , "<html><p>Current LSN being scanned.</p><p>When <strong>session_id</strong> = 0, the current LSN is 0.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "end_lsn"              , "<html><p>Ending LSN for the session.</p><p>NULL = session is active. </p><p>When <strong>session_id</strong> = 0, the ending LSN for the last session.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "tran_count"           , "<html><p>Number of change data capture transactions processed. This counter is populated in phase 2. </p><p>When <strong>session_id</strong> = 0, the number of processed transactions in all sessions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "last_commit_lsn"      , "<html><p>LSN of the last commit log record processed.</p><p>When <strong>session_id</strong> = 0, the last commit log record LSN for any session.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "last_commit_time"     , "<html><p>Time the last commit log record was processed.</p><p>When <strong>session_id</strong> = 0, the time the last commit log record for any session.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "log_record_count"     , "<html><p>Number of log records scanned.</p><p>When <strong>session_id</strong> = 0, number of records scanned for all sessions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "schema_change_count"  , "<html><p>Number of data definition language (DDL) operations detected. This counter is populated in phase 6.</p><p>When <strong>session_id</strong> = 0, the number of DDL operations processed in all sessions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "command_count"        , "<html><p>Number of commands processed. </p><p>When <strong>session_id</strong> = 0, the number of commands processed in all sessions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "first_begin_cdc_lsn"  , "<html><p>First LSN that contained change data capture transactions.</p><p>When <strong>session_id</strong> = 0, the first LSN that contained change data capture transactions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "last_commit_cdc_lsn"  , "<html><p>LSN of the last commit log record that contained change data capture transactions.</p><p>When <strong>session_id</strong> = 0, the last commit log record LSN for any session that contained change data capture transactions</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "last_commit_cdc_time" , "<html><p>Time the last commit log record was processed that contained change data capture transactions.</p><p>When <strong>session_id</strong> = 0, the time the last commit log record for any session that contained change data capture transactions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "latency"              , "<html><p>The difference, in seconds, between <strong>end_time</strong> and <strong>last_commit_cdc_time</strong> in the session. This counter is populated at the end of phase 7.</p><p>When <strong>session_id</strong> = 0, the last nonzero latency value recorded by a session.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "empty_scan_count"     , "<html><p>Number of consecutive sessions that contained no change data capture transactions.</p></html>");
			mtd.addColumn("dm_cdc_log_scan_sessions", "failed_sessions_count", "<html><p>Number of sessions that failed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_cdc_log_scan_sessions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_repl_traninfo
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_repl_traninfo",  "<p>Returns information on each replicated or change data capture transaction. </p>");

			// Column names and description
			mtd.addColumn("dm_repl_traninfo", "fp2p_pub_exists"           , "<html><p>If the transaction is in a database published using peer-to-peer transactional replication. If true, the value is 1; otherwise, it is 0.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "db_ver"                    , "<html><p>Database version.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "comp_range_address"        , "<html><p>Defines a partial rollback range that must be skipped.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "textinfo_address"          , "<html><p>In-memory address of the cached text information structure.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "fsinfo_address"            , "<html><p>In-memory address of the cached filestream information structure.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "begin_lsn"                 , "<html><p>Log sequence number (LSN) of the beginning log record for the transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "commit_lsn"                , "<html><p>LSN of commit log record for the transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "dbid"                      , "<html><p>Database ID. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "rows"                      , "<html><p>ID of the replicated command within the transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "xdesid"                    , "<html><p>Transaction ID.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "artcache_table_address"    , "<html><p>In-memory address of the cached article table structure last used for this transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "server"                    , "<html><p>Server name.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "server_len_in_bytes"       , "<html><p>Character length, in bytes, of the server name. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "database"                  , "<html><p>Database name.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "db_len_in_bytes"           , "<html><p>Character length, in bytes, of the database name.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "originator"                , "<html><p>Name of the server where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "originator_len_in_bytes"   , "<html><p>Character length, in bytes, of the server where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "orig_db"                   , "<html><p>Name of the database where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "orig_db_len_in_bytes"      , "<html><p>Character length, in bytes, of the database where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "cmds_in_tran"              , "<html><p>Number of replicated commands in the current transaction, which is used to determine when a logical transaction should be committed.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "is_boundedupdate_singleton", "<html><p>Specifies whether a unique column update affects only a single row.?</p></html>");
			mtd.addColumn("dm_repl_traninfo", "begin_update_lsn"          , "<html><p>LSN used in a unique column update.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "delete_lsn"                , "<html><p>LSN to delete as part of an update.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "last_end_lsn"              , "<html><p>Last LSN in a logical transaction. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "fcomplete"                 , "<html><p>Specifies whether the command is a partial update.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "fcompensated"              , "<html><p>Specifies whether the transaction is involved in a partial rollback.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "fprocessingtext"           , "<html><p>Specifies whether the transaction includes a binary large data type column. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "max_cmds_in_tran"          , "<html><p>Maximum number of commands in a logical transaction, as specified by the Log Reader Agent.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "begin_time"                , "<html><p>Time the transaction began.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "commit_time"               , "<html><p>Time the transaction was committed.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "session_id"                , "<html><p>ID of the change data capture log scan session. This column maps to the <strong>session_id</strong> column in <a href=\"https://msdn.microsoft.com/en-us/library/bb510694.aspx\">sys.dm_cdc_logscan_sessions</a>.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "session_phase"             , "<html><p>Number that indicates the phase the session was in at the time the error occurred. This column maps to the <strong>phase_number</strong> column in <a href=\"https://msdn.microsoft.com/en-us/library/bb500301.aspx\">sys.dm_cdc_errors</a>.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "is_known_cdc_tran"         , "<html><p>Indicates the transaction is tracked by change data capture.</p><p>0 = Transaction replication transaction.</p><p>1 = Change data capture transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "error_count"               , "<html><p>Number of errors encountered.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_repl_traninfo' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_cdc_errors
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_cdc_errors",  "<p>Returns one row for each error encountered during the change data capture log scan session.</p>");

			// Column names and description
			mtd.addColumn("dm_cdc_errors", "session_id"    , "<html><p>ID of the session.</p><p>0 = the error did not occur within a log scan session.</p></html>");
			mtd.addColumn("dm_cdc_errors", "phase_number"  , "<html><p>Number indicating the phase the session was in at the time the error occurred. For a description of each phase, see <a href=\"https://msdn.microsoft.com/en-us/library/bb510694.aspx\">sys.dm_cdc_log_scan_sessions (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_cdc_errors", "entry_time"    , "<html><p>The date and time the error was logged. This value corresponds to the timestamp in the SQL error log.</p></html>");
			mtd.addColumn("dm_cdc_errors", "error_number"  , "<html><p>ID of the error message.</p></html>");
			mtd.addColumn("dm_cdc_errors", "error_severity", "<html><p>Severity level of the message, between 1 and 25.</p></html>");
			mtd.addColumn("dm_cdc_errors", "error_state"   , "<html><p>State number of the error. </p></html>");
			mtd.addColumn("dm_cdc_errors", "error_message" , "<html><p>Message text of the error.</p></html>");
			mtd.addColumn("dm_cdc_errors", "start_lsn"     , "<html><p>Starting LSN value of the rows being processed when the error occurred.</p><p>0 = the error did not occur within a log scan session.</p></html>");
			mtd.addColumn("dm_cdc_errors", "begin_lsn"     , "<html><p>Beginning LSN value of the transaction being processed when the error occurred.</p><p>0 = the error did not occur within a log scan session.</p></html>");
			mtd.addColumn("dm_cdc_errors", "sequence_value", "<html><p>LSN value of the rows being processed when the error occurred.</p><p>0 = the error did not occur within a log scan session.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_cdc_errors' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_commit_table
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_commit_table",  "<p>Displays one row for each transaction that is committed for a table that is tracked by SQL Server change tracking.?The <span class=\"literal\">sys.dm_tran_commit_table management</span> view, which is provided for supportability purposes?and exposes the transaction-related information that change tracking stores in the <span class=\"literal\">sys.syscommittab</span> system table. The <span class=\"literal\">sys.syscommittab</span>?table provides an efficient persistent mapping from a database-specific transaction ID to the transaction's commit log sequence number (LSN) and commit timestamp.?The data that is stored in the <span class=\"literal\">sys.syscommittab</span> table and exposed in this management view is subject to cleanup according to the retention period specified when change tracking was configured.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_commit_table", "commit_ts"  , "<html><p>A monotonically increasing number that serves as a database-specific timestamp for each committed transaction.</p></html>");
			mtd.addColumn("dm_tran_commit_table", "xdes_id"    , "<html><p>A database-specific internal ID for the transaction.</p></html>");
			mtd.addColumn("dm_tran_commit_table", "commit_lbn" , "<html><p>The number of the log block that contains the commit log record for the transaction.</p></html>");
			mtd.addColumn("dm_tran_commit_table", "commit_csn" , "<html><p>The instance-specific commit sequence number for the transaction.</p></html>");
			mtd.addColumn("dm_tran_commit_table", "commit_time", "<html><p>The time when the transaction was committed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_commit_table' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_clr_appdomains
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_clr_appdomains",  "<p>Returns a row for each application domain in the server. Application domain (<strong>AppDomain</strong>) is a construct in the Microsoft?.NET Framework common language runtime (CLR) that is the unit of isolation for an application. You can use this view to understand and troubleshoot CLR integration objects that are executing in Microsoft?SQL Server.</p><p>There are several types of CLR integration managed database objects. For general information about these objects, see <a href=\"https://msdn.microsoft.com/en-us/library/ms131046.aspx\">Building Database Objects with Common Language Runtime (CLR) Integration</a>. Whenever these objects are executed, SQL Server creates an <strong>AppDomain</strong> under which it can load and execute the required code. The isolation level for an <strong>AppDomain</strong> is one <strong>AppDomain</strong> per database per owner. That is, all CLR objects owned by a user are always executed in the same <strong>AppDomain</strong> per-database (if a user registers CLR database objects in different databases, the CLR database objects will run in different application domains). An <strong>AppDomain</strong> is not destroyed after the code finishes execution. Instead, it is cached in memory for future executions. This improves performance.</p>");

			// Column names and description
			mtd.addColumn("dm_clr_appdomains", "appdomain_address"        , "<html><p>Address of the <strong>AppDomain</strong>. All managed database objects owned by a user are always loaded in the same <strong>AppDomain</strong>. You can use this column to look up all the assemblies currently loaded in this <strong>AppDomain</strong> in <strong>sys.dm_clr_loaded_assemblies</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "appdomain_id"             , "<html><p>ID of the <strong>AppDomain</strong>. Each <strong>AppDomain</strong> has a unique ID.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "appdomain_name"           , "<html><p>Name of the <strong>AppDomain </strong>as assigned by SQL Server.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "creation_time"            , "<html><p>Time when the <strong>AppDomain</strong> was created. Because <strong>AppDomains</strong> are cached and reused for better performance, <strong>creation_time</strong> is not necessarily the time when the code was executed.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "db_id"                    , "<html><p>ID of the database in which this <strong>AppDomain</strong> was created. Code stored in two different databases cannot share one <strong>AppDomain</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "user_id"                  , "<html><p>ID of the user whose objects can execute in this <strong>AppDomain</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "state"                    , "<html><p>A descriptor for the current state of the <strong>AppDomain</strong>. An AppDomain can be in different states from creation to deletion. See the Remarks section of this topic for more information.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "strong_refcount"          , "<html><p>Number of strong references to this <strong>AppDomain</strong>. This reflects the number of currently executing batches that use this <strong>AppDomain</strong>. Note that execution of this view will create a <strong>strong refcount</strong>; even if is no code currently executing, <strong>strong_refcount</strong> will have a value of 1.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "weak_refcount"            , "<html><p>Number of weak references to this <strong>AppDomain</strong>. This indicates how many objects inside the <strong>AppDomain</strong> are cached. When you execute a managed database object, SQL Server caches it inside the <strong>AppDomain</strong> for future reuse. This?improves performance.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "cost"                     , "<html><p>Cost of the <strong>AppDomain</strong>. The higher the cost, the more likely this <strong>AppDomain</strong> is to be unloaded under memory pressure. Cost usually depends on how much memory is required to re-create this <strong>AppDomain</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "value"                    , "<html><p>Value of the <strong>AppDomain</strong>. The lower the value, the more likely this <strong>AppDomain</strong> is to be unloaded under memory pressure. Value usually depends on how many connections or batches are using this <strong>AppDomain</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "total_processor_time_ms"  , "<html><p>Total processor time, in milliseconds, used by all threads while executing in the current application domain since the process started. This is equivalent to <strong>System.AppDomain.MonitoringTotalProcessorTime</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "total_allocated_memory_kb", "<html><p>Total size, in kilobytes, of all memory allocations that have been made by the application domain since it was created, without subtracting memory that has been collected. This is equivalent to <strong>System.AppDomain.MonitoringTotalAllocatedMemorySize</strong>.</p></html>");
			mtd.addColumn("dm_clr_appdomains", "survived_memory_kb"       , "<html><p>Number of kilobytes that survived the last full, blocking collection and that are known to be referenced by the current application domain. This is equivalent to <strong>System.AppDomain.MonitoringSurvivedMemorySize</strong>.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_clr_appdomains' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_clr_properties
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_clr_properties",  "<p>Returns a row for each property related to SQL Server common language runtime (CLR) integration, including the version and state of the hosted CLR. The hosted CLR is initialized by running the <a href=\"https://msdn.microsoft.com/en-us/library/ms189524.aspx\">CREATE ASSEMBLY</a>, <a href=\"https://msdn.microsoft.com/en-us/library/ms186711.aspx\">ALTER ASSEMBLY</a>, or <a href=\"https://msdn.microsoft.com/en-us/library/ms177514.aspx\">DROP ASSEMBLY</a> statements, or by executing any CLR routine, type, or trigger. The <strong>sys.dm_clr_properties</strong> view does not specify whether execution of user CLR code has been enabled on the server. Execution of user CLR code is enabled by using the <a href=\"https://msdn.microsoft.com/en-us/library/ms188787.aspx\">sp_configure</a> stored procedure with the <a href=\"https://msdn.microsoft.com/en-us/library/ms175193.aspx\">clr enabled</a> option set to 1.?</p><p>The <strong>sys.dm_clr_properties</strong> view contains the <strong>name</strong> and <strong>value</strong> columns. Each row in this view provides details about a property of the hosted CLR. Use this view to gather information about the hosted CLR, such as the CLR install directory, the CLR version, and the current state of the hosted CLR. This view can help you determine if the CLR integration code is not working because of problems with the CLR installation on the server computer. </p>");

			// Column names and description
			mtd.addColumn("dm_clr_properties", "name" , "<html><p>The name of the property.</p></html>");
			mtd.addColumn("dm_clr_properties", "value", "<html><p>Value of the property.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_clr_properties' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_clr_loaded_assemblies
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_clr_loaded_assemblies",  "<p>Returns a row for each managed user assembly loaded into the server address space. Use this view to understand and troubleshoot CLR integration managed database objects that are executing in Microsoft?SQL Server.</p><p>Assemblies are managed code DLL files that are used to define and deploy managed database objects in SQL Server. Whenever a user executes one of these managed database objects, SQL Server and the CLR load the assembly (and its references) in which the managed database object is defined. The assembly remains loaded in SQL Server to increase performance, so that the managed database objects contained in the assembly can be called in the future with out having to reload the assembly. The assembly is not unloaded until SQL Server comes under memory pressure. For more information about assemblies and CLR integration, see <a href=\"https://msdn.microsoft.com/en-us/library/ms131047.aspx\">CLR Hosted Environment</a>. For more information about managed database objects, see <a href=\"https://msdn.microsoft.com/en-us/library/ms131046.aspx\">Building Database Objects with Common Language Runtime (CLR) Integration</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_clr_loaded_assemblies", "assembly_id"      , "<html><p>ID of the loaded assembly. The <strong>assembly_id</strong> can be used to look up more information about the assembly in the <a href=\"https://msdn.microsoft.com/en-us/library/ms189790.aspx\">sys.assemblies (Transact-SQL)</a> catalog view. Note that the Transact-SQL?<a href=\"https://msdn.microsoft.com/en-us/library/ms189790.aspx\">sys.assemblies</a> catalog shows assemblies in the current database only. The <strong>sqs.dm_clr_loaded_assemblies</strong>?view shows all loaded assemblies on the server.?</p></html>");
			mtd.addColumn("dm_clr_loaded_assemblies", "appdomain_address", "<html><p>Address of the application domain (<strong>AppDomain</strong>) in which the assembly is loaded. All the assemblies owned by a single user are always loaded in the same <strong>AppDomain</strong>. The <strong>appdomain_address</strong> can be used to lookup more information about the <strong>AppDomain</strong> in the <a href=\"https://msdn.microsoft.com/en-us/library/ms187720.aspx\">sys.dm_clr_appdomains</a> view.</p></html>");
			mtd.addColumn("dm_clr_loaded_assemblies", "load_time"        , "<html><p>Time when the assembly was loaded. Note that the assembly remains loaded until SQL Server is under memory pressure and unloads the <strong>AppDomain</strong>. You can monitor <strong>load_time</strong> to understand how frequently SQL Server comes under memory pressure and unloads the <strong>AppDomain</strong>.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_clr_loaded_assemblies' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_clr_tasks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_clr_tasks",  "<p>Returns a row for all common language runtime (CLR) tasks that are currently running. A Transact-SQL batch that contains a reference to a CLR routine creates a separate task for execution of all the managed code in that batch. Multiple statements in the batch that require managed code execution use the same CLR task. The CLR task is responsible for maintaining objects and state pertaining to managed code execution, as well as the transitions between the instance of SQL Server and the common language runtime. </p>");

			// Column names and description
			mtd.addColumn("dm_clr_tasks", "task_address"      , "<html><p>Address of the CLR task. </p></html>");
			mtd.addColumn("dm_clr_tasks", "sos_task_address"  , "<html><p>Address of the underlying Transact-SQL batch task.</p></html>");
			mtd.addColumn("dm_clr_tasks", "appdomain_address" , "<html><p>Address of the application domain in which this task is running.</p></html>");
			mtd.addColumn("dm_clr_tasks", "state"             , "<html><p>Current state of the task.</p></html>");
			mtd.addColumn("dm_clr_tasks", "abort_state"       , "<html><p>State the abort is currently in (if the task was canceled) There are multiple states involved while aborting tasks.</p></html>");
			mtd.addColumn("dm_clr_tasks", "type"              , "<html><p>Task type.</p></html>");
			mtd.addColumn("dm_clr_tasks", "affinity_count"    , "<html><p>Affinity of the task.</p></html>");
			mtd.addColumn("dm_clr_tasks", "forced_yield_count", "<html><p>Number of times the task was forced to yield. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_clr_tasks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_log_stats 
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_log_stats ",  "<p>Returns summary level attributes and information on transaction log files of databases. Use this information for monitoring and diagnostics of transaction log health. Since: SQL Server 2016 SP 2 and later </p>");

			// Column names and description
			mtd.addColumn("dm_db_log_stats ", "database_id"                 , "<html><p> Database ID </p></html>");
			mtd.addColumn("dm_db_log_stats ", "recovery_model"              , "<html><p> Recovery model of the database. Possible values include:<br>SIMPLE<br>BULK_LOGGED<br>FULL<br> </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_min_lsn"                 , "<html><p> Current start log sequence number (LSN) in the transaction log. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_end_lsn"                 , "<html><p> log sequence number (LSN) of the last log record in the transaction log. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "current_vlf_sequence_number" , "<html><p> Current virtual log file (VLF) sequence number at the time of execution. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "current_vlf_size_mb"         , "<html><p> Current virtual log file (VLF) size in MB. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "total_vlf_count"             , "<html><p> Total number of virtual log files (VLFs) in the transaction log. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "total_log_size_mb"           , "<html><p> Total transaction log size in MB. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "active_vlf_count"            , "<html><p> Total number of active virtual log files (VLFs) in the transaction log. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "active_log_size_mb"          , "<html><p> Total active transaction log size in MB. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_truncation_holdup_reason", "<html><p> Log truncation holdup reason. The value is same as log_reuse_wait_desc column of sys.databases. (For more detailed explanations of these values, see The Transaction Log).<br>"
			                                                                                + "Possible values include: <br>"
			                                                                                + " - NOTHING <br>"
			                                                                                + " - CHECKPOINT <br>"
			                                                                                + " - LOG_BACKUP <br>"
			                                                                                + " - ACTIVE_BACKUP_OR_RESTORE <br>"
			                                                                                + " - ACTIVE_TRANSACTION <br>"
			                                                                                + " - DATABASE_MIRRORING <br>"
			                                                                                + " - REPLICATION <br>"
			                                                                                + " - DATABASE_SNAPSHOT_CREATION <br>"
			                                                                                + " - LOG_SCAN <br>"
			                                                                                + " - AVAILABILITY_REPLICA <br>"
			                                                                                + " - OLDEST_PAGE <br>"
			                                                                                + " - XTP_CHECKPOINT <br>"
			                                                                                + " - OTHER TRANSIENT  <br>"
			                                                                                + "</p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_backup_time"             , "<html><p> Last transaction log backup time. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_backup_lsn"              , "<html><p> Last transaction log backup log sequence number (LSN). </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_since_last_log_backup_mb", "<html><p> Log size in MB since last transaction log backup log sequence number (LSN). </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_checkpoint_lsn"          , "<html><p> Last checkpoint log sequence number (LSN). </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_since_last_checkpoint_mb", "<html><p> Log size in MB since last checkpoint log sequence number (LSN). </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_recovery_lsn"            , "<html><p> Recovery log sequence number (LSN) of the database. If log_recovery_lsn occurs before the checkpoint LSN, log_recovery_lsn is the oldest active transaction LSN, otherwise log_recovery_lsn is the checkpoint LSN. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_recovery_size_mb"        , "<html><p> Log size in MB since log recovery log sequence number (LSN). </p></html>");
			mtd.addColumn("dm_db_log_stats ", "recovery_vlf_count"          , "<html><p> Total number of virtual log files (VLFs) to be recovered, if there was failover or server restart. </p></html>");
			mtd.addColumn("dm_db_log_stats ", "log_state"                   , "<html><p> SQL-Server 2019 </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_log_stats ' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_mirroring_connections
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_mirroring_connections",  "<p>Returns a row for each connection established for database mirroring. </p>");

			// Column names and description
			mtd.addColumn("dm_db_mirroring_connections", "connection_id"             , "<html><p>Identifier of the connection. </p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "transport_stream_id"       , "<html><p>Identifier of the SQL Server?Network Interface (SNI) connection used by this connection for TCP/IP communications.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "state"                     , "<html><p>Current state of the connection. Possible values:</p><p>1 = NEW</p><p>2 = CONNECTING</p><p>3 = CONNECTED</p><p>4 = LOGGED_IN</p><p>5 = CLOSED </p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "state_desc"                , "<html><p>Current state of the connection. Possible values:</p><p>NEW</p><p>CONNECTING</p><p>CONNECTED</p><p>LOGGED_IN</p><p>CLOSED </p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "connect_time"              , "<html><p>Date and time at which the connection was opened.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "login_time"                , "<html><p>Date and time at which login for the connection succeeded.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "authentication_method"     , "<html><p>Name of the Windows Authentication method, such as NTLM or KERBEROS. The value comes from Windows. </p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "principal_name"            , "<html><p>Name of the login that was validated for connection permissions. For Windows Authentication, this value is the remote user name. For certificate authentication, this value is the certificate owner.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "remote_user_name"          , "<html><p>Name of the peer user from the other database that is used by Windows Authentication.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "last_activity_time"        , "<html><p>Date and time at which the connection was last used to send or receive information.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "is_accept"                 , "<html><p>Indicates whether the connection originated on the remote side. </p><p>1 = The connection is a request accepted from the remote instance. </p><p>0 = The connection was started by the local instance.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "login_state"               , "<html><p>State of the login process for this connection. Possible values: </p><p>0 = INITIAL</p><p>1 = WAIT LOGIN NEGOTIATE</p><p>2 = ONE ISC</p><p>3 = ONE ASC</p><p>4 = TWO ISC</p><p>5 = TWO ASC</p><p>6 = WAIT ISC Confirm</p><p>7 = WAIT ASC Confirm</p><p>8 = WAIT REJECT</p><p>9 = WAIT PRE-MASTER SECRET</p><p>10 = WAIT VALIDATION</p><p>11 = WAIT ARBITRATION</p><p>12 = ONLINE</p><p>13 = ERROR?</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "login_state_desc"          , "<html><p>Current state of login from the remote computer. Possible values:</p><ul class=\"unordered\"> <li><p>Connection handshake is initializing.</p></li> <li><p>Connection handshake is waiting for Login Negotiate message.</p></li> <li><p>Connection handshake has initialized and sent security context for authentication.</p></li> <li><p>Connection handshake has received and accepted security context for authentication.</p></li> <li><p>Connection handshake has initialized and sent security context for authentication. There is an optional mechanism available for authenticating the peers.</p></li> <li><p>Connection handshake has received and sent accepted security context for authentication. There is an optional mechanism available for authenticating the peers.</p></li> <li><p>Connection handshake is waiting for Initialize Security Context Confirmation message.</p></li> <li><p>Connection handshake is waiting for Accept Security Context Confirmation message.</p></li> <li><p>Connection handshake is waiting for SSPI rejection message for failed authentication.</p></li> <li><p>Connection handshake is waiting for Pre-Master Secret message. </p></li> <li><p>Connection handshake is waiting for Validation message.</p></li> <li><p>Connection handshake is waiting for Arbitration message.</p></li> <li><p>Connection handshake is complete and is online (ready) for message exchange.</p></li> <li><p>Connection is in error.</p></li></ul></html>");
			mtd.addColumn("dm_db_mirroring_connections", "peer_certificate_id"       , "<html><p>The local object ID of the certificate used by the remote instance for authentication. The owner of this certificate must have CONNECT permissions to the database mirroring endpoint.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "encryption_algorithm"      , "<html><p>Encryption algorithm that is used for this connection. NULLABLE. Possible values: </p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value </p></th>     <th><p>Description </p></th>     <th><p>Corresponding DDL option </p></th>    </tr>    <tr>     <td data-th=\"Value \"><p>0</p></td>     <td data-th=\"Description \"><p>NONE </p></td>     <td data-th=\"Corresponding DDL option \"><p>Disabled</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>1</p></td>     <td data-th=\"Description \"><p>RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>{Required | Required algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>2</p></td>     <td data-th=\"Description \"><p>AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required algorithm AES</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>3</p></td>     <td data-th=\"Description \"><p>NONE, RC4</p></td>     <td data-th=\"Corresponding DDL option \"><p>{Supported | Supported algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>4</p></td>     <td data-th=\"Description \"><p>NONE, AES</p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported algorithm RC4</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>5</p></td>     <td data-th=\"Description \"><p>RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>6</p></td>     <td data-th=\"Description \"><p>AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm AES RC4</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>7</p></td>     <td data-th=\"Description \"><p>NONE, RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>8</p></td>     <td data-th=\"Description \"><p>NONE, AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported algorithm AES RC4</p></td>    </tr>   </tbody>  </table> </div></div><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The RC4 algorithm is only supported for backward compatibility. New material can only be encrypted using RC4 or RC4_128 when the database is in compatibility level 90 or 100. (Not recommended.) Use a newer algorithm such as one of the AES algorithms instead. In SQL Server 2012 and higher versions, material encrypted using RC4 or RC4_128 can be decrypted in any compatibility level.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_db_mirroring_connections", "encryption_algorithm_desc" , "<html><p>Textual representation of the encryption algorithm. NULLABLE. Possible Values:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Description </p></th>     <th><p>Corresponding DDL option </p></th>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE </p></td>     <td data-th=\"Corresponding DDL option \"><p>Disabled</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>{Required | Required Algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm AES</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, RC4</p></td>     <td data-th=\"Corresponding DDL option \"><p>{Supported | Supported Algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, AES</p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm RC4</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm AES RC4</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm AES RC4</p></td>    </tr>   </tbody>  </table> </div></div><p></p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "receives_posted"           , "<html><p>Number of asynchronous network receives that have not yet completed for this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "is_receive_flow_controlled", "<html><p>Whether network receives have been postponed due to flow control because the network is busy. </p><p>1 = True</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "sends_posted"              , "<html><p>The number of asynchronous network sends that have not yet completed for this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "is_send_flow_controlled"   , "<html><p>Whether network sends have been postponed due to network flow control because the network is busy. </p><p>1 = True</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "total_bytes_sent"          , "<html><p>Total number of bytes sent by this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "total_bytes_received"      , "<html><p>Total number of bytes received by this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "total_fragments_sent"      , "<html><p>Total number of database mirroring message fragments sent by this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "total_fragments_received"  , "<html><p>Total number of database mirroring message fragments received by this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "total_sends"               , "<html><p>Total number of network send requests issued by this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "total_receives"            , "<html><p>Total number of network receive requests issued by this connection.</p></html>");
			mtd.addColumn("dm_db_mirroring_connections", "peer_arbitration_id"       , "<html><p>Internal identifier for the endpoint. NULLABLE.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_mirroring_connections' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_mirroring_auto_page_repair
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_mirroring_auto_page_repair",  "<p>Returns a row for every automatic page-repair attempt on any mirrored database on the server instance. This view contains rows for the latest automatic page-repair attempts on a given mirrored database, with a maximum of 100 rows per database. As soon as a database reaches the maximum, the row for its next automatic page-repair attempt replaces one of the existing entries. The following table defines the meaning of the various columns.</p>");

			// Column names and description
			mtd.addColumn("dm_db_mirroring_auto_page_repair", "database_id"      , "<html><p>ID of the database to which this row corresponds.</p></html>");
			mtd.addColumn("dm_db_mirroring_auto_page_repair", "file_id"          , "<html><p>ID of the file in which the page is located.</p></html>");
			mtd.addColumn("dm_db_mirroring_auto_page_repair", "page_id"          , "<html><p>ID of the page in the file.</p></html>");
			mtd.addColumn("dm_db_mirroring_auto_page_repair", "error_type"       , "<html><p>Type of the error. The values can be:</p><p><strong>-</strong>1 = All hardware <a href=\"https://msdn.microsoft.com/en-us/library/aa337267.aspx\">823 errors</a></p><p>1 = <a href=\"https://msdn.microsoft.com/en-us/library/aa337274.aspx\">824 errors</a> other than a bad checksum or a torn page (such as a bad page ID) </p><p>2 = Bad checksum</p><p>3 = Torn page</p></html>");
			mtd.addColumn("dm_db_mirroring_auto_page_repair", "page_status"      , "<html><p>The status of the page-repair attempt:</p><p>2 = Queued for request from partner.</p><p>3 = Request sent to partner.</p><p>4 = Queued for automatic page repair (response received from partner).</p><p>5 = Automatic page repair succeeded and the page should be usable.</p><p>6 = Irreparable. This indicates that an error occurred during page-repair attempt, for example, because the page is also corrupted on the partner, the partner is disconnected, or a network problem occurred. This state is not terminal; if corruption is encountered again on the page, the page will be requested again from the partner.</p></html>");
			mtd.addColumn("dm_db_mirroring_auto_page_repair", "modification_time", "<html><p>Time of last change to the page status.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_mirroring_auto_page_repair' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_file_space_usage
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_file_space_usage",  "<p>Returns space usage information for each file in the database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_file_space_usage", "database_id"                        , "<html><p>Database ID.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "file_id"                            , "<html><p>File ID.</p><p><span class=\"literal\">file_id</span> maps to <span class=\"literal\">file_id</span> in <a href=\"https://msdn.microsoft.com/en-us/library/ms190326.aspx\">sys.dm_io_virtual_file_stats</a> and to <span class=\"literal\">fileid</span> in <a href=\"https://msdn.microsoft.com/en-us/library/ms178009.aspx\">sys.sysfiles</a>.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "filegroup_id"                       , "<html><p>Filegroup ID.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_db_file_space_usage", "total_page_count"                   , "<html><p>Total number of pages in the file.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_db_file_space_usage", "allocated_extent_page_count"        , "<html><p>Total number of pages in the allocated extents in the file.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_db_file_space_usage", "unallocated_extent_page_count"      , "<html><p>Total number of pages in the unallocated extents in the file.</p><p>Unused pages in allocated extents are not included.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "version_store_reserved_page_count"  , "<html><p>Total number of pages in the uniform extents allocated for the version store. Version store pages are never allocated from mixed extents. </p><p>IAM pages are not included, because they are always allocated from mixed extents. PFS pages are included if they are allocated from a uniform extent.</p><p>For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms186328.aspx\">sys.dm_tran_version_store (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "user_object_reserved_page_count"    , "<html><p>Total number of pages allocated from uniform extents for user objects in the database. Unused pages from an allocated extent are included in the count.</p><p>IAM pages are not included, because they are always allocated from mixed extents. PFS pages are included if they are allocated from a uniform extent.</p><p>You can use the <span class=\"literal\">total_pages</span> column in the <a href=\"https://msdn.microsoft.com/en-us/library/ms189792.aspx\">sys.allocation_units</a> catalog view to return the reserved page count of each allocation unit in the user object. However, note that the <span class=\"literal\">total_pages</span> column includes IAM pages.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "internal_object_reserved_page_count", "<html><p>Total number of pages in uniform extents allocated for internal objects in the file. Unused pages from an allocated extent are included in the count.</p><p>IAM pages are not included, because they are always allocated from mixed extents. PFS pages are included if they are allocated from a uniform extent.</p><p>There is no catalog view or dynamic management object that returns the page count of each internal object.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "mixed_extent_page_count"            , "<html><p>Total number of allocated and unallocated pages in allocated mixed extents in the file. Mixed extents contain pages allocated to different objects. This count does include all the IAM pages in the file.</p></html>");
			mtd.addColumn("dm_db_file_space_usage", "modified_extent_page_count"         , "<html><p>Total number of pages modified in allocated extents of the file since last full database backup. The modified page count can be used to track amount of differential changes in the database since last full backup, to decide if differential backup is needed.</p>Applies to: SQL Server 2016 (13.x) SP2 and later</html>");
			mtd.addColumn("dm_db_file_space_usage", "pdw_node_id"                        , "<html><p>The identifier for the node that this distribution is on.</p> Applies to: Azure Synapse Analytics, Parallel Data Warehouse</html>");
			mtd.addColumn("dm_db_file_space_usage", "distribution_id"                    , "<html><p>The unique numeric id associated with the distribution.</p>   Applies to: Azure Synapse Analytics, Parallel Data Warehouse</html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_file_space_usage' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_partition_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_partition_stats",  "<p>Returns page and row-count information for every partition in the current database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_partition_stats", "partition_id"                    , "<html><p>ID of the partition. This is unique within a database. This is the same value as the <strong>partition_id</strong> in the <strong>sys.partitions</strong> catalog view</p><p></p></html>");
			mtd.addColumn("dm_db_partition_stats", "object_id"                       , "<html><p>Object ID of the table or indexed view that the partition is part of.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "index_id"                        , "<html><p>ID of the heap or index the partition is part of.</p><p>0 = Heap</p><p>1 = Clustered index. </p><p>&gt; 1 = Nonclustered index</p></html>");
			mtd.addColumn("dm_db_partition_stats", "partition_number"                , "<html><p>1-based partition number within the index or heap.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "in_row_data_page_count"          , "<html><p>Number of pages in use for storing in-row data in this partition. If the partition is part of a heap, the value is the number of data pages in the heap. If the partition is part of an index, the value is the number of pages in the leaf level. (Nonleaf pages in the B-tree are not included in the count.) IAM (Index Allocation Map) pages are not included in either case. Always 0 for an xVelocity memory optimized columnstore index.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "in_row_used_page_count"          , "<html><p>Total number of pages in use to store and manage the in-row data in this partition. This count includes nonleaf B-tree pages, IAM pages, and all pages included in the <strong>in_row_data_page_count</strong> column. Always 0 for a columnstore index.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "in_row_reserved_page_count"      , "<html><p>Total number of pages reserved for storing and managing in-row data in this partition, regardless of whether the pages are in use or not. Always 0 for a columnstore index.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "lob_used_page_count"             , "<html><p>Number of pages in use for storing and managing out-of-row <strong>text</strong>, <strong>ntext</strong>, <strong>image</strong>, <strong>varchar(max)</strong>, <strong>nvarchar(max)</strong>, <strong>varbinary(max)</strong>, and <strong>xml</strong> columns within the partition. IAM pages are included.</p><p>Total number of LOBs used to store and manage columnstore index in the partition.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "lob_reserved_page_count"         , "<html><p>Total number of pages reserved for storing and managing out-of-row <strong>text</strong>, <strong>ntext</strong>, <strong>image</strong>, <strong>varchar(max)</strong>, <strong>nvarchar(max)</strong>, <strong>varbinary(max)</strong>, and <strong>xml</strong> columns within the partition, regardless of whether the pages are in use or not. IAM pages are included.</p><p>Total number of LOBs reserved for storing and managing a columnstore index in the partition.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "row_overflow_used_page_count"    , "<html><p>Number of pages in use for storing and managing row-overflow <strong>varchar</strong>, <strong>nvarchar</strong>, <strong>varbinary</strong>, and <strong>sql_variant</strong> columns within the partition. IAM pages are included.</p><p>Always 0 for a columnstore index.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "row_overflow_reserved_page_count", "<html><p>Total number of pages reserved for storing and managing row-overflow <strong>varchar</strong>, <strong>nvarchar</strong>, <strong>varbinary</strong>, and <strong>sql_variant</strong> columns within the partition, regardless of whether the pages are in use or not. IAM pages are included.</p><p>Always 0 for a columnstore index.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "used_page_count"                 , "<html><p>Total number of pages used for the partition. Computed as <strong>in_row_used_page_count</strong> + <strong>lob_used_page_count</strong> + <strong>row_overflow_used_page_count</strong>.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "reserved_page_count"             , "<html><p>Total number of pages reserved for the partition. Computed as <strong>in_row_reserved_page_count</strong> + <strong>lob_reserved_page_count</strong> + <strong>row_overflow_reserved_page_count</strong>.</p></html>");
			mtd.addColumn("dm_db_partition_stats", "row_count"                       , "<html><p>The approximate number of rows in the partition.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_partition_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_session_space_usage
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_session_space_usage",  "<p>Applies To: Azure SQL Database, SQL Data Warehouse, SQL Server 2014, SQL Server 2016 Preview</p><div class=\"introduction\"> <p>Returns the number of pages allocated and deallocated by each session for the database.</p></div>");

			// Column names and description
			mtd.addColumn("dm_db_session_space_usage", "session_id"                              , "<html><p>Session ID. </p><p><strong>session_id</strong> maps to <strong>session_id</strong> in <a href=\"https://msdn.microsoft.com/en-us/library/ms176013.aspx\">sys.dm_exec_sessions</a>.</p></html>");
			mtd.addColumn("dm_db_session_space_usage", "database_id"                             , "<html><p>Database ID.</p></html>");
			mtd.addColumn("dm_db_session_space_usage", "user_objects_alloc_page_count"           , "<html><p>Number of pages reserved or allocated for user objects by this session. </p></html>");
			mtd.addColumn("dm_db_session_space_usage", "user_objects_dealloc_page_count"         , "<html><p>Number of pages deallocated and no longer reserved for user objects by this session.</p></html>");
			mtd.addColumn("dm_db_session_space_usage", "internal_objects_alloc_page_count"       , "<html><p>Number of pages reserved or allocated for internal objects by this session.</p></html>");
			mtd.addColumn("dm_db_session_space_usage", "internal_objects_dealloc_page_count"     , "<html><p>Number of pages deallocated and no longer reserved for internal objects by this session.</p></html>");
			mtd.addColumn("dm_db_session_space_usage", "user_objects_deferred_dealloc_page_count", "<html><p>Number of pages which have been marked for deferred deallocation.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p> Introduced in service packs for SQL Server 2012 and SQL Server 2014.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_session_space_usage' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_uncontained_entities
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_uncontained_entities",  "<p>Shows any uncontained objects used in the database. Uncontained objects are objects that cross the database boundary in a contained database. This view is accessible from both a contained database and a non-contained database. If <span class=\"database\">sys.dm_db_uncontained_entities</span> is empty, your database does not use any uncontained entities.</p><p>If a module crosses the database boundary more than once, only the first discovered crossing is reported.</p>");

			// Column names and description
			mtd.addColumn("dm_db_uncontained_entities", "Column name"            , "<html><p><strong>Description</strong></p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "class"                  , "<html><p>1 = Object or column (includes modules, XPs, views, synonyms, and tables).</p><p>4 = Database Principal</p><p>5 = Assembly</p><p>6 = Type</p><p>7 = Index (Full-text Index)</p><p>12 = Database DDL Trigger</p><p>19 = Route</p><p>30 = Audit Specification</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "class_desc"             , "<html><p>Description of class of the entity. One of the following to match the class.</p><ul class=\"unordered\"> <li><p>OBJECT_OR_COLUMN</p></li> <li><p>DATABASE_PRINCIPAL</p></li> <li><p>ASSEMBLY</p></li> <li><p>TYPE</p></li> <li><p>INDEX</p></li> <li><p>DATABASE_DDL_TRIGGER</p></li> <li><p>ROUTE</p></li> <li><p>AUDIT_SPECIFICATION</p></li></ul></html>");
			mtd.addColumn("dm_db_uncontained_entities", "major_id"               , "<html><p>ID of the entity. </p><p>If <em>class</em> = 1, then <span class=\"database\">object_id</span></p><p>If <em>class</em> = 4, then <span class=\"database\">sys.database_principals.principal_id</span>.</p><p>If <em>class</em> = 5, then <span class=\"database\">sys.assemblies.assembly_id</span>.</p><p>If <em>class</em> = 6, then <span class=\"database\">sys.types.user_type_id</span>.</p><p>If <em>class</em> = 7, then <span class=\"database\">sys.indexes.index_id</span>.</p><p>If <em>class</em> = 12, then <span class=\"database\">sys.triggers.object_id</span>.</p><p>If <em>class</em> = 19, then <span class=\"database\">sys.routes.route_id</span>.</p><p>If <em>class</em> = 30, then <span class=\"database\">sys. database_audit_specifications.databse_specification_id</span>.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_line_number"  , "<html><p>If the class is a module, returns the line number on which the uncontained use is located. Otherwise the value is null.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_ offset_begin", "<html><p>If the class is a module, indicates, in bytes, beginning with 0, the starting position where uncontained use begins. Otherwise the return value is null. </p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_ offset_end"  , "<html><p>If the class is a module, indicates, in bytes, starting with 0, the ending position of the uncontained use. A value of -1 indicates the end of the module. Otherwise the return value is null.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_type"         , "<html><p>The type of statement.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "feature_ name"          , "<html><p>Returns the external name of the object.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "feature_type_name"      , "<html><p>Returns the type of feature.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_uncontained_entities' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_wait_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_wait_stats",  "<p>Returns information about all the waits encountered by threads that executed during operation. You can use this aggregated view to diagnose performance issues with Azure SQL Database and also with specific queries and batches.</p><p>Specific types of wait times during query execution can indicate bottlenecks or stall points within the query. Similarly, high wait times, or wait counts server wide can indicate bottlenecks or hot spots in interaction query interactions within the server instance. For example, lock waits indicate data contention by queries; page IO latch waits indicate slow IO response times; page latch update waits indicate incorrect file layout.</p>");

			// Column names and description
			mtd.addColumn("dm_db_wait_stats", "wait_type"          , "<html><p>Name of the wait type. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/dn269834.aspx#WaitTypes\">Types of Waits</a>, later in this topic. </p></html>");
			mtd.addColumn("dm_db_wait_stats", "waiting_tasks_count", "<html><p>Number of waits on this wait type. This counter is incremented at the start of each wait. </p></html>");
			mtd.addColumn("dm_db_wait_stats", "wait_time_ms"       , "<html><p>Total wait time for this wait type in milliseconds. This time is inclusive of <span class=\"literal\">signal_wait_time_ms</span>. </p></html>");
			mtd.addColumn("dm_db_wait_stats", "max_wait_time_ms"   , "<html><p>Maximum wait time on this wait type.</p></html>");
			mtd.addColumn("dm_db_wait_stats", "signal_wait_time_ms", "<html><p>Difference between the time that the waiting thread was signaled and when it started running. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_wait_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_operation_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_operation_status",  "<p>Returns information about operations performed on databases in a Azure SQL Database server. </p>");

			// Column names and description
			mtd.addColumn("dm_operation_status", "session_activity_id", "<html><p>ID of the operation. Not null.</p></html>");
			mtd.addColumn("dm_operation_status", "res_type"           , "<html><p>Denotes the type of resource on which the operation is performed. Not null. In the current release, this view tracks operations performed on SQL Database only, and the corresponding integer value is 0.</p></html>");
			mtd.addColumn("dm_operation_status", "resource_type_desc" , "<html><p>Description of the resource type on which the operation is performed. In the current release, this view tracks operations performed on SQL Database only.</p></html>");
			mtd.addColumn("dm_operation_status", "major_resource_id"  , "<html><p>Name of the SQL Database on which the operation is performed. Not Null.</p></html>");
			mtd.addColumn("dm_operation_status", "minor_resource_id"  , "<html><p>For internal use only. Not null.</p></html>");
			mtd.addColumn("dm_operation_status", "operation"          , "<html><p>Operation performed on a SQL Database, such as CREATE or ALTER.</p></html>");
			mtd.addColumn("dm_operation_status", "state"              , "<html><p>The state of the operation.</p><p>0 = Pending<br>1 = In progress<br>2 = Completed<br>3 = Failed<br>4 = Cancelled</p></html>");
			mtd.addColumn("dm_operation_status", "state_desc"         , "<html><p>PENDING = operation is waiting for resource or quota availability.</p><p>IN_PROGRESS = operation has started and is in progress.</p><p>COMPLETED = operation completed successfully.</p><p>FAILED = operation failed. See the <strong>error_desc</strong> column for details.</p><p>CANCELLED = operation stopped at the request of the user.</p></html>");
			mtd.addColumn("dm_operation_status", "percent_complete"   , "<html><p>Percentage of operation that has completed. Values range from 0 to 100. Not null.</p></html>");
			mtd.addColumn("dm_operation_status", "error_code"         , "<html><p>Code indicating the error that occurred during a failed operation. If the value is 0, it indicates that the operation completed successfully.</p></html>");
			mtd.addColumn("dm_operation_status", "error_desc"         , "<html><p>Description of the error that occurred during a failed operation.</p></html>");
			mtd.addColumn("dm_operation_status", "error_severity"     , "<html><p>Severity level of the error that occurred during a failed operation. For more information about error severities, see <a href=\"http://go.microsoft.com/fwlink/?LinkId=251052\">Database Engine Error Severities</a>.</p></html>");
			mtd.addColumn("dm_operation_status", "error_state"        , "<html><p>Reserved for future use. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_operation_status", "start_time"         , "<html><p>Timestamp when the operation started.</p></html>");
			mtd.addColumn("dm_operation_status", "last_modify_time"   , "<html><p>Timestamp when the record was last modified for a long running operation. In case of successfully completed operations, this field displays the timestamp when the operation completed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_operation_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_resource_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_resource_stats",  "<p>Returns CPU, I/O, and memory consumption for an Azure SQL Database database. One row exists for every 15 seconds, even if there is no activity in the database. Historical data is maintained for one hour.</p>");

			// Column names and description
			mtd.addColumn("dm_db_resource_stats", "end_time"             , "<html><p>UTC time indicates the end of the current reporting interval. </p></html>");
			mtd.addColumn("dm_db_resource_stats", "avg_cpu_percent"      , "<html><p>Average compute utilization in percentage of the limit of the service tier.</p></html>");
			mtd.addColumn("dm_db_resource_stats", "avg_data_io_percent"  , "<html><p>Average data I/O utilization in percentage based on the limit of the service tier. </p></html>");
			mtd.addColumn("dm_db_resource_stats", "avg_log_write_percent", "<html><p>Average write resource utilization in percentage of the limit of the service tier. </p></html>");
			mtd.addColumn("dm_db_resource_stats", "avg_memory_percent"   , "<html><p>Average memory utilization in percentage of the limit of the service tier.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_resource_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_fts_index_physical_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_fts_index_physical_stats",  "<p>Returns a row for each full-text or semantic index in each table that has an associated full-text or semantic index.</p>");

			// Column names and description
			mtd.addColumn("dm_db_fts_index_physical_stats", "Column name"                , "<html><p><strong>Description</strong></p></html>");
			mtd.addColumn("dm_db_fts_index_physical_stats", "object_id"                  , "<html><p>Object ID of the table that contains the index.</p></html>");
			mtd.addColumn("dm_db_fts_index_physical_stats", "fulltext_index_page_count"  , "<html><p>Logical size of the extraction in number of index pages.</p></html>");
			mtd.addColumn("dm_db_fts_index_physical_stats", "keyphrase_index_page_count" , "<html><p>Logical size of the extraction in number of index pages.</p></html>");
			mtd.addColumn("dm_db_fts_index_physical_stats", "similarity_index_page_count", "<html><p>Logical size of the extraction in number of index pages.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_fts_index_physical_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_persisted_sku_features
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_persisted_sku_features",  "<p>Some features of the SQL Server Database Engine change the way that Database Engine stores information in the database files. These features are restricted to specific editions of SQL Server. A database that contains these features cannot be moved to an edition of SQL Server that does not support them Use the <span class=\"literal\">sys.dm_db_persisted_sku_features</span> dynamic management view to list all edition-specific features that are enabled in the current database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_persisted_sku_features", "feature_name", "<html><p>External name of the feature that is enabled in the database but not supported on the all the editions of SQL Server. This feature must be removed before the database can be migrated to all available editions of SQL Server.</p></html>");
			mtd.addColumn("dm_db_persisted_sku_features", "feature_id"  , "<html><p>Feature ID that is associated with the feature. Identified for informational purposes only. Not supported. Future compatibility is not guaranteed..</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_persisted_sku_features' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_task_space_usage
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_task_space_usage",  "<p>Returns page allocation and deallocation activity by task for the database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_task_space_usage", "session_id"                         , "<html><p>Session ID.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "request_id"                         , "<html><p>Request ID within the session.</p><p>A request is also called a batch and may contain one or more queries. A session may have multiple requests active at the same time. Each query in the request may start multiple threads (tasks), if a parallel execution plan is used.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "exec_context_id"                    , "<html><p>Execution context ID of the task. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms174963.aspx\">sys.dm_os_tasks (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "database_id"                        , "<html><p>Database ID.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "user_objects_alloc_page_count"      , "<html><p>Number of pages reserved or allocated for user objects by this task.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "user_objects_dealloc_page_count"    , "<html><p>Number of pages deallocated and no longer reserved for user objects by this task.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "internal_objects_alloc_page_count"  , "<html><p>Number of pages reserved or allocated for internal objects by this task.</p></html>");
			mtd.addColumn("dm_db_task_space_usage", "internal_objects_dealloc_page_count", "<html><p>Number of pages deallocated and no longer reserved for internal objects by this task.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_task_space_usage' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_uncontained_entities
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_uncontained_entities",  "<p>Shows any uncontained objects used in the database. Uncontained objects are objects that cross the database boundary in a contained database. This view is accessible from both a contained database and a non-contained database. If <span class=\"database\">sys.dm_db_uncontained_entities</span> is empty, your database does not use any uncontained entities.</p><p>If a module crosses the database boundary more than once, only the first discovered crossing is reported.</p>");

			// Column names and description
			mtd.addColumn("dm_db_uncontained_entities", "Column name"            , "<html><p><strong>Description</strong></p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "class"                  , "<html><p>1 = Object or column (includes modules, XPs, views, synonyms, and tables).</p><p>4 = Database Principal</p><p>5 = Assembly</p><p>6 = Type</p><p>7 = Index (Full-text Index)</p><p>12 = Database DDL Trigger</p><p>19 = Route</p><p>30 = Audit Specification</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "class_desc"             , "<html><p>Description of class of the entity. One of the following to match the class.</p><ul class=\"unordered\"> <li><p>OBJECT_OR_COLUMN</p></li> <li><p>DATABASE_PRINCIPAL</p></li> <li><p>ASSEMBLY</p></li> <li><p>TYPE</p></li> <li><p>INDEX</p></li> <li><p>DATABASE_DDL_TRIGGER</p></li> <li><p>ROUTE</p></li> <li><p>AUDIT_SPECIFICATION</p></li></ul></html>");
			mtd.addColumn("dm_db_uncontained_entities", "major_id"               , "<html><p>ID of the entity. </p><p>If <em>class</em> = 1, then <span class=\"database\">object_id</span></p><p>If <em>class</em> = 4, then <span class=\"database\">sys.database_principals.principal_id</span>.</p><p>If <em>class</em> = 5, then <span class=\"database\">sys.assemblies.assembly_id</span>.</p><p>If <em>class</em> = 6, then <span class=\"database\">sys.types.user_type_id</span>.</p><p>If <em>class</em> = 7, then <span class=\"database\">sys.indexes.index_id</span>.</p><p>If <em>class</em> = 12, then <span class=\"database\">sys.triggers.object_id</span>.</p><p>If <em>class</em> = 19, then <span class=\"database\">sys.routes.route_id</span>.</p><p>If <em>class</em> = 30, then <span class=\"database\">sys. database_audit_specifications.databse_specification_id</span>.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_line_number"  , "<html><p>If the class is a module, returns the line number on which the uncontained use is located. Otherwise the value is null.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_ offset_begin", "<html><p>If the class is a module, indicates, in bytes, beginning with 0, the starting position where uncontained use begins. Otherwise the return value is null. </p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_ offset_end"  , "<html><p>If the class is a module, indicates, in bytes, starting with 0, the ending position of the uncontained use. A value of -1 indicates the end of the module. Otherwise the return value is null.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "statement_type"         , "<html><p>The type of statement.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "feature_ name"          , "<html><p>Returns the external name of the object.</p></html>");
			mtd.addColumn("dm_db_uncontained_entities", "feature_type_name"      , "<html><p>Returns the type of feature.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_uncontained_entities' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_database_copies
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_database_copies",  "<p>Returns information about the database copy.</p>");

			// Column names and description
			mtd.addColumn("dm_database_copies", "database_id"           , "<html><p>The ID of the current database in the <span class=\"code\">sys.databases</span> view.</p></html>");
			mtd.addColumn("dm_database_copies", "start_date"            , "<html><p>The UTC time at a regional SQL Database datacenter when the database copying was initiated.</p></html>");
			mtd.addColumn("dm_database_copies", "modify_date"           , "<html><p>The UTC time at regional SQL Database datacenter when the database copying has completed. The new database is transactionally consistent with the primary database as of this time. The completion information is updated every 5 minutes.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>On a Geo-Replication primary database, modify_date is the UTC time at which the database is transactionally consistent. On a continuous-copy replica database, after seeding completes, this value is the timestamp of the last applied replication operation.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_database_copies", "percent_complete"      , "<html><p>The percentage of bytes that have been copied. Values range from 0 to 100. SQL Database may automatically recover from some errors, such as failover, and restart the database copy. In this case, percent_complete would restart from 0.</p></html>");
			mtd.addColumn("dm_database_copies", "error_code"            , "<html><p>When greater than 0, the code indicating the error that has occurred while copying. Value equals 0 if no errors have occurred. </p></html>");
			mtd.addColumn("dm_database_copies", "error_desc"            , "<html><p>Description of the error that occurred while copying.</p></html>");
			mtd.addColumn("dm_database_copies", "error_severity"        , "<html><p>Returns 16 if the database copy failed.</p></html>");
			mtd.addColumn("dm_database_copies", "error_state"           , "<html><p>Returns 1 if copy failed.</p></html>");
			mtd.addColumn("dm_database_copies", "copy_guid"             , "<html><p>Unique ID of the copy.</p></html>");
			mtd.addColumn("dm_database_copies", "partner_server"        , "<html><p>Name of the linked SQL Database server. </p></html>");
			mtd.addColumn("dm_database_copies", "partner_database"      , "<html><p>Name of the linked database on the linked SQL Database server.</p></html>");
			mtd.addColumn("dm_database_copies", "replication_state"     , "<html><p>The state of continuous-copy replication for this database, one of:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>Pending. Creation of the active secondary database is scheduled but the necessary preparation steps are not yet completed or are temporarily blocked by the seeding quota.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Seeding. The replication target is being seeded and is in a transactionally inconsistent state. Until seeding completes, you cannot connect to the active secondary database and planned termination is disallowed. The only way to cancel the seeding operation is invoking a forced termination on the primary database. </p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Catching up. The active secondary database is currently catching up to the primary database and is in a transactionally consistent state.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>4</p></td>     <td data-th=\"Description\"><p>Terminated. The replication relationship has been terminated..</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_database_copies", "replication_state_desc", "<html><p>Description of replication_state, one of:</p><p>PENDING</p><p>SEEDING</p><p>CATCH_UP</p><p>TERMINATED</p></html>");
			mtd.addColumn("dm_database_copies", "maximum_lag"           , "<html><p> The maximum_lag column returns a value of -1 indicating that the maximum lag value is not set. The value for this column cannot be set or changed</p></html>");
			mtd.addColumn("dm_database_copies", "is_continuous_copy"    , "<html><p>0 = This is a Database Copy operation, not a continuous copy relationship.</p><p>1= This is a continuous copy relationship.</p></html>");
			mtd.addColumn("dm_database_copies", "is_target_role"        , "<html><p>0 =Source/Primary database</p><p>1 = Target/Secondary database</p></html>");
			mtd.addColumn("dm_database_copies", "is_interlink_connected", "<html><p>0 = Disconnected. The continuous copy interlink is disconnected.</p><p>1 = Connected. The continuous copy interlink is connected.</p></html>");
			mtd.addColumn("dm_database_copies", "is_offline_secondary"  , "<html><p>0 = Active Geo-Replication relationship</p><p>1 = Standard Geo-Replication relationship</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_database_copies' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_objects_impacted_on_version_change
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_objects_impacted_on_version_change",  "<p>This database-scoped system view is designed to provide an early warning system to determine objects that will be impacted by a major release upgrade in Azure SQL Database. You can use the view either before or after the upgrade to get a full enumeration of impacted objects. You will need to query this view in each database to get a full accounting across the entire server.</p>");

			// Column names and description
			mtd.addColumn("dm_db_objects_impacted_on_version_change", "class"     , "<html><p>The class of the object which will be impacted:</p><p><strong>1</strong> = constraint</p><p><strong>7</strong> = Indexes and heaps</p></html>");
			mtd.addColumn("dm_db_objects_impacted_on_version_change", "class_desc", "<html><p>Description of the class:</p><p><strong>OBJECT_OR_COLUMN</strong></p><p><strong>INDEX</strong></p></html>");
			mtd.addColumn("dm_db_objects_impacted_on_version_change", "major_id"  , "<html><p>object id of the constraint, or object id of table that contains index or heap.</p></html>");
			mtd.addColumn("dm_db_objects_impacted_on_version_change", "minor_id"  , "<html><p><strong>NULL</strong> for constraints</p><p>Index_id for indexes and heaps</p></html>");
			mtd.addColumn("dm_db_objects_impacted_on_version_change", "dependency", "<html><p>Description of dependency that is causing a constraint or index to be impacted. The same value is also used for warnings generated during upgrade.</p><p>Examples:</p><p><strong>space</strong> (for intrinsic)</p><p><strong>geometry</strong> (for system UDT)</p><p><strong>geography::Parse</strong> (for system UDT method)</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_objects_impacted_on_version_change' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_background_job_queue
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_background_job_queue",  "<p>Returns a row for each query processor job that is scheduled for asynchronous (background) execution.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_background_job_queue", "time_queued" , "<html><p>Time when the job was added to the queue.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "job_id"      , "<html><p>Job identifier.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "database_id" , "<html><p>Database on which the job is to execute.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "object_id1"  , "<html><p>Value depends on the job type. For more information, see the Remarks section. </p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "object_id2"  , "<html><p>Value depends on the job type. For more information, see the Remarks section.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "object_id3"  , "<html><p>Value depends on the job type. For more information, see the Remarks section.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "object_id4"  , "<html><p>Value depends on the job type. For more information, see the Remarks section.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "error_code"  , "<html><p>Error code if the job reinserted due to failure. NULL if suspended, not picked up, or completed.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "request_type", "<html><p>Type of the job request.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "retry_count" , "<html><p>Number of times the job was picked from the queue and reinserted because of lack of resources or other reasons.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "in_progress" , "<html><p>Indicates whether the job has started execution.</p><p>1 = Started</p><p>0 = Still waiting</p></html>");
			mtd.addColumn("dm_exec_background_job_queue", "session_id"  , "<html><p>Session identifier. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_background_job_queue' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_background_job_queue_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_background_job_queue_stats",  "<p>Returns a row that provides aggregate statistics for each query processor job submitted for asynchronous (background) execution. </p>");

			// Column names and description
			mtd.addColumn("dm_exec_background_job_queue_stats", "queue_max_len"                 , "<html><p>Maximum length of the queue.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "enqueued_count"                , "<html><p>Number of requests successfully posted to the queue.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "started_count"                 , "<html><p>Number of requests that started execution.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "ended_count"                   , "<html><p>Number of requests serviced to either success or failure.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "failed_lock_count"             , "<html><p>Number of requests that failed due to lock contention or deadlock.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "failed_other_count"            , "<html><p>Number of requests that failed due to other reasons.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "failed_giveup_count"           , "<html><p>Number of requests that failed because retry limit has been reached.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "enqueue_failed_full_count"     , "<html><p>Number of failed enqueue attempts because the queue is full.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "enqueue_failed_duplicate_count", "<html><p>Number of duplicate enqueue attempts.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "elapsed_avg_ms"                , "<html><p>Average elapsed time of request in milliseconds.</p></html>");
			mtd.addColumn("dm_exec_background_job_queue_stats", "elapsed_max_ms"                , "<html><p>Elapsed time of the longest request in milliseconds.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_background_job_queue_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_cached_plans
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_cached_plans",  "<p>Returns a row for each query plan that is cached by SQL Server for faster query execution. You can use this dynamic management view to find cached query plans, cached query text, the amount of memory taken by cached plans, and the reuse count of the cached plans.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out. In addition, the values in the columns <strong>memory_object_address</strong> and <strong>pool_id</strong> are filtered; the column value is set to NULL.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_cached_plans", "bucketid"             , "<html><p>ID of the hash bucket in which the entry is cached. The value indicates a range from 0 through the hash table size for the type of cache.</p><p>For the SQL Plans and Object Plans caches, the hash table size can be up to 10007 on 32-bit systems and up to 40009 on 64-bit systems. For the Bound Trees cache, the hash table size can be up to 1009 on 32-bit systems and up to 4001 on 64-bit systems. For the Extended Stored Procedures cache the hash table size can be up to 127 on 32-bit and 64-bit systems. </p></html>");
			mtd.addColumn("dm_exec_cached_plans", "refcounts"            , "<html><p>Number of cache objects that are referencing this cache object. <strong>Refcounts</strong> must be at least 1 for an entry to be in the cache.</p></html>");
			mtd.addColumn("dm_exec_cached_plans", "usecounts"            , "<html><p>Number of times the cache object has been looked up. Not incremented when parameterized queries find a plan in the cache. Can be incremented multiple times when using showplan.</p></html>");
			mtd.addColumn("dm_exec_cached_plans", "size_in_bytes"        , "<html><p>Number of bytes consumed by the cache object. </p></html>");
			mtd.addColumn("dm_exec_cached_plans", "memory_object_address", "<html><p>Memory address of the cached entry. This value can be used with <a href=\"https://msdn.microsoft.com/en-us/library/ms179875.aspx\">sys.dm_os_memory_objects</a> to get the memory breakdown of the cached plan and with <a href=\"https://msdn.microsoft.com/en-us/library/ms189488.aspx\">sys.dm_os_memory_cache_entries</a>_entries to obtain the cost of caching the entry.</p></html>");
			mtd.addColumn("dm_exec_cached_plans", "cacheobjtype"         , "<html><p>Type of object in the cache. The value can be one of the following:</p><ul class=\"unordered\"> <li><p>Compiled Plan</p></li> <li><p>Compiled Plan Stub</p></li> <li><p>Parse Tree</p></li> <li><p>Extended Proc</p></li> <li><p>CLR Compiled Func</p></li> <li><p>CLR Compiled Proc</p></li></ul></html>");
			mtd.addColumn("dm_exec_cached_plans", "objtype"              , "<html><p>Type of object. The value can be one of the following:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>Proc</p></td>     <td data-th=\"Description\"><p>Stored procedure</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Prepared</p></td>     <td data-th=\"Description\"><p>Prepared statement</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Adhoc</p></td>     <td data-th=\"Description\"><p>Ad hoc query<span class=\"sup\">1</span></p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>ReplProc</p></td>     <td data-th=\"Description\"><p>Replication-filter-procedure</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Trigger</p></td>     <td data-th=\"Description\"><p>Trigger</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>View</p></td>     <td data-th=\"Description\"><p>View</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Default</p></td>     <td data-th=\"Description\"><p>Default</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>UsrTab</p></td>     <td data-th=\"Description\"><p>User table</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>SysTab</p></td>     <td data-th=\"Description\"><p>System table</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Check</p></td>     <td data-th=\"Description\"><p>CHECK constraint</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Rule</p></td>     <td data-th=\"Description\"><p>Rule</p></td>    </tr>   </tbody>  </table> </div></div><p></p></html>");
			mtd.addColumn("dm_exec_cached_plans", "plan_handle"          , "<html><p>Identifier for the in-memory plan. This identifier is transient and remains constant only while the plan remains in the cache. This value may be used with the following dynamic management functions: </p><p><a href=\"https://msdn.microsoft.com/en-us/library/ms181929.aspx\">sys.dm_exec_sql_text</a></p><p><a href=\"https://msdn.microsoft.com/en-us/library/ms189747.aspx\">sys.dm_exec_query_plan</a></p><p><a href=\"https://msdn.microsoft.com/en-us/library/ms189472.aspx\">sys.dm_exec_plan_attributes</a></p></html>");
			mtd.addColumn("dm_exec_cached_plans", "pool_id"              , "<html><p>The ID of the resource pool against which this plan memory usage is accounted for.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_cached_plans' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_cached_plan_dependent_objects
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_cached_plan_dependent_objects",  "<p>Returns a row for each Transact-SQL execution plan, common language runtime (CLR) execution plan, and cursor associated with a plan. </p>");

			// Column names and description
			mtd.addColumn("dm_exec_cached_plan_dependent_objects", "usecounts"            , "<html><p>Number of times the execution context or cursor has been used.</p><p>Column is not nullable.</p></html>");
			mtd.addColumn("dm_exec_cached_plan_dependent_objects", "memory_object_address", "<html><p>Memory address of the execution context or cursor. </p><p>Column is not nullable.</p></html>");
			mtd.addColumn("dm_exec_cached_plan_dependent_objects", "cacheobjtype"         , "<html><p>Possible values are </p><ul class=\"unordered\"> <li><p>Executable plan</p></li> <li><p>CLR compiled function</p></li> <li><p>CLR compiled procedure</p></li> <li><p>Cursor</p></li></ul><p>Column is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_cached_plan_dependent_objects' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_connections
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_connections",  "<p>Returns information about the connections established to this instance of SQL Server and the details of each connection.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_connections", "session_id"            , "<html><p>Identifies the session associated with this connection. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "most_recent_session_id", "<html><p>Represents the session ID for the most recent request associated with this connection. (SOAP connections can be reused by another session.) Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "connect_time"          , "<html><p>Timestamp when connection was established. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "net_transport"         , "<html><p>Describes the physical transport protocol that is used by this connection. Is not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Always returns <strong>Session</strong> when a connection has multiple active result sets (MARS) enabled.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_connections", "protocol_type"         , "<html><p>Specifies the protocol type of the payload. It currently distinguishes between TDS (TSQL) and SOAP. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "protocol_version"      , "<html><p>Version of the data access protocol associated with this connection. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "endpoint_id"           , "<html><p>An identifier that describes what type of connection it is. This <span class=\"literal\">endpoint_id</span> can be used to query the <span class=\"literal\">sys.endpoints</span> view. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "encrypt_option"        , "<html><p>Boolean value to describe whether encryption is enabled for this connection. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "auth_scheme"           , "<html><p>Specifies SQL Server/Windows Authentication scheme used with this connection. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "node_affinity"         , "<html><p>Identifies the memory node to which this connection has affinity. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "num_reads"             , "<html><p>Number of packet reads that have occurred over this connection. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "num_writes"            , "<html><p>Number of data packet writes that have occurred over this connection. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "last_read"             , "<html><p>Timestamp when last read occurred over this connection. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "last_write"            , "<html><p>Timestamp when last write occurred over this connection. Not Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "net_packet_size"       , "<html><p>Network packet size used for information and data transfer. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "client_net_address"    , "<html><p>Host address of the client connecting to this server. Is nullable.</p><p>Prior to V12 in Azure SQL Database, this column always returns NULL.</p></html>");
			mtd.addColumn("dm_exec_connections", "client_tcp_port"       , "<html><p>Port number on the client computer that is associated with this connection. Is nullable.</p><p>In Azure SQL Database, this column always returns NULL.</p></html>");
			mtd.addColumn("dm_exec_connections", "local_net_address"     , "<html><p>Represents the IP address on the server that this connection targeted. Available only for connections using the TCP transport provider. Is nullable.</p><p>In Azure SQL Database, this column always returns NULL.</p></html>");
			mtd.addColumn("dm_exec_connections", "local_tcp_port"        , "<html><p>Represents the server TCP port that this connection targeted if it were a connection using the TCP transport. Is nullable.</p><p>In Azure SQL Database, this column always returns NULL.</p></html>");
			mtd.addColumn("dm_exec_connections", "connection_id"         , "<html><p>Identifies each connection uniquely. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "parent_connection_id"  , "<html><p>Identifies the primary connection that the MARS session is using. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_connections", "most_recent_sql_handle", "<html><p>The SQL handle of the last request executed on this connection. The <span class=\"literal\">most_recent_sql_handle</span> column is always in sync with the <span class=\"literal\">most_recent_session_id</span> column. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_connections' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_compute_node_errors
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_compute_node_errors",  "<p>Returns errors that occur on PolyBase compute nodes.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_compute_node_errors", "error_id"       , "<html><p>Unique numeric id associated with the error .</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "source"         , "<html><p>Source thread or process description</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "type"           , "<html><p>Type of error.</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "create_time"    , "<html><p>The time of the error occurrence</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "compute_node_id", "<html><p>Identifier of the specific compute node</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "rexecution_id"  , "<html><p>Identifier of the PolyBase query, if any. </p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "spid"           , "<html><p>Identifier of the SQL Server session</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "thread_id"      , "<html><p>Numeric identifier of the thread on which the error occurred.</p></html>");
			mtd.addColumn("dm_exec_compute_node_errors", "details"        , "<html><p>Full description of the details of the error.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_compute_node_errors' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_compute_node_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_compute_node_status",  "<p>Holds additional information about the performance and status of all PolyBase nodes. Lists one row per node.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_compute_node_status", "compute_node_id"   , "<html><p>Unique numeric id associated with the node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "process_id"        , "<html><p></p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "process_name"      , "<html><p>Logical name of the node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "allocated_memory"  , "<html><p>Total allocated memory on this node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "available_memory"  , "<html><p>Total available memory on this node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "process_cpu_usage" , "<html><p>Total process CPU usage, in ticks.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "total_cpu_usage"   , "<html><p>Total CPU usage, in ticks.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "thread_count"      , "<html><p>Total number of threads in use on this node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "handle_count"      , "<html><p>Total number of handles in use on this node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "total_elapsed_time", "<html><p>Total time elapsed since system start or restart.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "is_available"      , "<html><p>Flag indicating whether this node is available.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "sent_time"         , "<html><p>Last time a network package was sent by this</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "received_time"     , "<html><p>Last time a network package was sent by this node.</p></html>");
			mtd.addColumn("dm_exec_compute_node_status", "error_id"          , "<html><p>Unique identifier of the last error that occurred on this node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_compute_node_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_compute_nodes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_compute_nodes",  "<p>Holds information about nodes used with PolyBase data management. It lists one row per node.</p><p>Use this DMV to see the list of all nodes in the scale-out cluster with their role, name and IP address.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_compute_nodes", "compute_node_id", "<html><p>Unique numeric id associated with the node. Key for this view.</p></html>");
			mtd.addColumn("dm_exec_compute_nodes", "type"           , "<html><p>Type of the node.</p></html>");
			mtd.addColumn("dm_exec_compute_nodes", "name"           , "<html><p>Logical name of the node.</p></html>");
			mtd.addColumn("dm_exec_compute_nodes", "address"        , "<html><p>P address of this node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_compute_nodes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_cursors
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_cursors",  "<p>Returns information about the cursors that are open in various databases. </p>");

			// Column names and description
			mtd.addColumn("dm_exec_cursors", "session_id"            , "<html><p>ID of the session that holds this cursor.</p></html>");
			mtd.addColumn("dm_exec_cursors", "cursor_id"             , "<html><p>ID of the cursor object.</p></html>");
			mtd.addColumn("dm_exec_cursors", "name"                  , "<html><p>Name of the cursor as defined by the user.</p></html>");
			mtd.addColumn("dm_exec_cursors", "properties"            , "<html><p>Specifies the properties of the cursor. The values of the following properties are concatenated to form the value of this column:</p><ul class=\"unordered\"> <li><p>Declaration Interface</p></li> <li><p>Cursor Type </p></li> <li><p>Cursor Concurrency</p></li> <li><p>Cursor scope</p></li> <li><p>Cursor nesting level</p></li></ul><p>For example, the value returned in this column might be \"TSQL | Dynamic | Optimistic | Global (0)\".</p></html>");
			mtd.addColumn("dm_exec_cursors", "sql_handle"            , "<html><p>Handle to the text of the batch that declared the cursor.</p></html>");
			mtd.addColumn("dm_exec_cursors", "statement_start_offset", "<html><p>Number of characters into the currently executing batch or stored procedure at which the currently executing statement starts. Can be used together with the <strong>sql_handle</strong>, the <strong>statement_end_offset</strong>, and the <a href=\"https://msdn.microsoft.com/en-us/library/ms181929.aspx\">sys.dm_exec_sql_text</a> dynamic management function to retrieve the currently executing statement for the request.</p></html>");
			mtd.addColumn("dm_exec_cursors", "statement_end_offset"  , "<html><p>Number of characters into the currently executing batch or stored procedure at which the currently executing statement ends. Can be used together with the <strong>sql_handle</strong>, the <strong>statement_start_offset</strong>, and the <strong>sys.dm_exec_sql_text</strong> dynamic management function to retrieve the currently executing statement for the request.</p></html>");
			mtd.addColumn("dm_exec_cursors", "plan_generation_num"   , "<html><p>A sequence number that can be used to distinguish between instances of plans after recompilation.</p></html>");
			mtd.addColumn("dm_exec_cursors", "creation_time"         , "<html><p>Timestamp when this cursor was created.</p></html>");
			mtd.addColumn("dm_exec_cursors", "is_open"               , "<html><p>Specifies whether the cursor is open.</p></html>");
			mtd.addColumn("dm_exec_cursors", "is_async_population"   , "<html><p>Specifies whether the background thread is still asynchronously populating a KEYSET or STATIC cursor.</p></html>");
			mtd.addColumn("dm_exec_cursors", "is_close_on_commit"    , "<html><p>Specifies whether the cursor was declared by using CURSOR_CLOSE_ON_COMMIT. </p><p>1 = Cursor will be closed when the transaction ends.</p></html>");
			mtd.addColumn("dm_exec_cursors", "fetch_status"          , "<html><p>Returns last fetch status of the cursor. This is the last returned @@FETCH_STATUS value.</p></html>");
			mtd.addColumn("dm_exec_cursors", "fetch_buffer_size"     , "<html><p>Returns information about the size of the fetch buffer. </p><p>1 = Transact-SQL cursors. This can be set to a higher value for API cursors.</p></html>");
			mtd.addColumn("dm_exec_cursors", "fetch_buffer_start"    , "<html><p>For FAST_FORWARD and DYNAMIC cursors, it returns 0 if the cursor is not open or if it is positioned before the first row. Otherwise, it returns -1. </p><p>For STATIC and KEYSET cursors, it returns 0 if the cursor is not open, and -1 if the cursor is positioned beyond the last row. </p><p>Otherwise, it returns the row number in which it is positioned.</p></html>");
			mtd.addColumn("dm_exec_cursors", "ansi_position"         , "<html><p>Cursor position within the fetch buffer.</p></html>");
			mtd.addColumn("dm_exec_cursors", "worker_time"           , "<html><p>Time spent, in microseconds, by the workers executing this cursor.</p></html>");
			mtd.addColumn("dm_exec_cursors", "reads"                 , "<html><p>Number of reads performed by the cursor.</p></html>");
			mtd.addColumn("dm_exec_cursors", "writes"                , "<html><p>Number of writes performed by the cursor.</p></html>");
			mtd.addColumn("dm_exec_cursors", "dormant_duration"      , "<html><p>Milliseconds since the last query (open or fetch) on this cursor was started.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_cursors' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_describe_first_result_set
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_describe_first_result_set",  "<p>This dynamic management function takes a Transact-SQL?statement as a parameter and describes the metadata of the?first result set for the statement.</p><p><strong>sys.dm_exec_describe_first_result_set</strong> has the same result set definition as <a href=\"https://msdn.microsoft.com/en-us/library/ff878236.aspx\">sys.dm_exec_describe_first_result_set_for_object (Transact-SQL)</a> and is similar to <a href=\"https://msdn.microsoft.com/en-us/library/ff878602.aspx\">sp_describe_first_result_set (Transact-SQL)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_describe_first_result_set", "is_hidden"                   , "<html><p>Specifies that the column is an extra column added for browsing and informational purposes that does not actually appear in the result set.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "column_ordinal"              , "<html><p>Contains the ordinal position of the column in the result set. Position of the first column will be specified as 1.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "name"                        , "<html><p>Contains the name of the column if a name can be determined. If not, will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_nullable"                 , "<html><p>Contains the following values:</p><ul class=\"unordered\"> <li><p>Value 1 if column allows NULLs.</p></li> <li><p>Value 0 if the column does not allow NULLs. </p></li> <li><p>Value 1 if it cannot be determined that the column allows NULLs.</p></li></ul></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "system_type_id"              , "<html><p>Contains the system_type_id of the column data type as specified in <span class=\"literal\">sys.types</span>. For CLR types, even though the system_type_name column will return NULL, this column will return the value 240.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "system_type_name"            , "<html><p>Contains the name and arguments (such as length, precision, scale), specified for the data type of the column. </p><p>If data type is a user-defined alias type, the underlying system type is specified here. </p><p>If data type is a CLR user-defined type, NULL is returned in this column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "max_length"                  , "<html><p>Maximum length (in bytes) of the column.</p><p>-1 = Column data type is<strong> varchar(max)</strong>, <strong>nvarchar(max)</strong>, <strong>varbinary(max)</strong>, or <strong>xml</strong>.</p><p>For <strong>text</strong> columns, the <strong>max_length</strong> value will be 16 or the value set by <strong>sp_tableoption 'text in row'</strong>.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "precision"                   , "<html><p>Precision of the column if numeric-based. Otherwise returns 0.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "scale"                       , "<html><p>Scale of column if numeric-based. Otherwise returns 0.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "collation_name"              , "<html><p>Name of the collation of the column if character-based. Otherwise returns NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "user_type_id"                , "<html><p>For CLR and alias types, contains the user_type_id of the data type of the column as specified in sys.types. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "user_type_database"          , "<html><p>For CLR and alias types, contains the name of the database in which the type is defined. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "user_type_schema"            , "<html><p>For CLR and alias types, contains the name of the schema in which the type is defined. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "user_type_name"              , "<html><p>For CLR and alias types, contains the name of the type. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "assembly_qualified_type_name", "<html><p>For CLR types, returns the name of the assembly and class defining the type. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "xml_collection_id"           , "<html><p>Contains the xml_collection_id of the data type of the column as specified in sys.columns. This column returns NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "xml_collection_database"     , "<html><p>Contains the database in which the XML schema collection associated with this type is defined. This column returns NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "xml_collection_schema"       , "<html><p>Contains the schema in which the XML schema collection associated with this type is defined. This column returns NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "xml_collection_name"         , "<html><p>Contains the name of the XML schema collection associated with this type. This column returns NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_xml_document"             , "<html><p>Returns 1 if the returned data type is XML and that type is guaranteed to be a complete XML document (including a root node), as opposed to an XML fragment). Otherwise returns 0. </p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_case_sensitive"           , "<html><p>Returns 1 if the column is of a case-sensitive string type. Returns 0 if it is not.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_fixed_length_clr_type"    , "<html><p>Returns 1 if the column is of a fixed-length CLR type. Returns 0 if it is not.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "source_server"               , "<html><p>Name of the originating server (if it originates from a remote server). The name is given as it appears in <span class=\"literal\">sys.servers</span>. Returns NULL if the column originates on the local server or if it cannot be determined which server it originates on. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "source_database"             , "<html><p>Name of the originating database returned by the column in this result. Returns NULL if the database cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "source_schema"               , "<html><p>Name of the originating schema returned by the column in this result. Returns NULL if the schema cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "source_table"                , "<html><p>Name of the originating table returned by the column in this result. Returns NULL if the table cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "source_column"               , "<html><p>Name of the originating column returned by the result column. Returns NULL if the column cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_identity_column"          , "<html><p>Returns 1 if the column is an identity column and 0 if not. Returns NULL if it cannot be determined that the column is an identity column. </p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_part_of_unique_key"       , "<html><p>Returns 1 if the column is part of a unique index (including unique and primary constraints) and 0 if it is not. Returns NULL if it cannot be determined that the column is part of a unique index. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_updateable"               , "<html><p>Returns 1 if the column is updateable and 0 if not. Returns NULL if it cannot be determined that the column is updateable.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_computed_column"          , "<html><p>Returns 1 if the column is a computed column and 0 if not. Returns NULL if it cannot be determined if the column is a computed column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "is_sparse_column_set"        , "<html><p>Returns 1 if the column is a sparse column and 0 if not. Returns NULL if it cannot be determined that the column is a part of a sparse column set.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "ordinal_in_order_by_list"    , "<html><p>ihe position of this column is in ORDER BY list. Returns NULL if the column does not appear in the ORDER BY list, or if the ORDER BY list cannot be uniquely determined.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "order_by_list_length"        , "<html><p>The length of the ORDER BY list. NULL is returned if there is no ORDER BY list or if the ORDER BY list cannot be uniquely determined. Note that this value will be the same for all rows returned by sp_describe_first_result_set.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "order_by_is_descending"      , "<html><p>If the ordinal_in_order_by_list is not NULL, the <strong>order_by_is_descending</strong> column reports the direction of the ORDER BY clause for this column. Otherwise it reports NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "error_number"                , "<html><p>Contains the error number returned by the function. If no error occurred, the column will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "error_severity"              , "<html><p>Contains the severity returned by the function. If no error occurred, the column will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "error_state"                 , "<html><p>Contains the state message. returned by the function. If no error occurred, the column will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "error_message"               , "<html><p>Contains the message returned by the function. If no error occurred, the column will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "error_type"                  , "<html><p>Contains an integer representing the error being returned. Maps to error_type_desc. See the list under remarks.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set", "error_type_desc"             , "<html><p>Contains a short uppercase string representing the error being returned. Maps to error_type. See the list under remarks.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_describe_first_result_set' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_describe_first_result_set_for_object
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_describe_first_result_set_for_object",  "<p>This dynamic management function takes an @object_id as a parameter and describes the first result metadata for the module with that ID. The @object_id specified can be the ID of a Transact-SQL stored procedure or a Transact-SQL trigger. If it is the ID of any other object (such as a view, table, function, or CLR procedure), an error will be specified in the error columns of the result.</p><p><strong>sys.dm_exec_describe_first_result_set_for_object</strong> has the same result set definition as <a href=\"https://msdn.microsoft.com/en-us/library/ff878258.aspx\">sys.dm_exec_describe_first_result_set (Transact-SQL)</a> and is similar to <a href=\"https://msdn.microsoft.com/en-us/library/ff878602.aspx\">sp_describe_first_result_set (Transact-SQL)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_hidden"                   , "<html><p>Specifies whether the column is an extra column added for browsing information purposes that does not actually appear in the result set.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "column_ordinal"              , "<html><p>Contains the ordinal position of the column in the result set. Position of the first column will be specified as 1.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "name"                        , "<html><p>Contains the name of the column if a name can be determined. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_nullable"                 , "<html><p>Contains the value 1 if the column allows NULLs, 0 if the column does not allow NULLs, and 1 if it cannot be determined that the column allows NULLs.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "system_type_id"              , "<html><p>Contains the system_type_id of the data type of the column as specified in <span class=\"literal\">sys.types</span>. For CLR types, even though the system_type_name column will return NULL, this column will return the value 240.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "system_type_name"            , "<html><p>Contains the data type name. Includes arguments (such as length, precision, scale) specified for the data type of the column. If the data type is a user-defined alias type, the underlying system type is specified here. If it is a CLR user-defined type, NULL is returned in this column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "max_length"                  , "<html><p>Maximum length (in bytes) of the column.</p><p>-1 = Column data type is<strong> varchar(max)</strong>, <strong>nvarchar(max)</strong>, <strong>varbinary(max)</strong>, or <strong>xml</strong>.</p><p>For <strong>text</strong> columns, the <strong>max_length</strong> value will be 16 or the value set by <strong>sp_tableoption 'text in row'</strong>.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "precision"                   , "<html><p>Precision of the column if numeric-based. Otherwise returns 0.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "scale"                       , "<html><p>Scale of column if numeric-based. Otherwise returns 0.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "collation_name"              , "<html><p>Name of the collation of the column if character-based. Otherwise returns NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "user_type_id"                , "<html><p>For CLR and alias types, contains the user_type_id of the data type of the column as specified in <span class=\"literal\">sys.types</span>. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "user_type_database"          , "<html><p>For CLR and alias types, contains the name of the database in which the type is defined. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "user_type_schema"            , "<html><p>For CLR and alias types, contains the name of the schema in which the type is defined. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "user_type_name"              , "<html><p>For CLR and alias types, contains the name of the type. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "assembly_qualified_type_name", "<html><p>For CLR types, returns the name of the assembly and class defining the type. Otherwise is NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "xml_collection_id"           , "<html><p>Contains the xml_collection_id of the data type of the column as specified in <span class=\"literal\">sys.columns</span>. This column will return NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "xml_collection_database"     , "<html><p>Contains the database in which the XML schema collection associated with this type is defined. This column will return NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "xml_collection_schema"       , "<html><p>Contains the schema in which the XML schema collection associated with this type is defined. This column will return NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "xml_collection_name"         , "<html><p>Contains the name of the XML schema collection associated with this type. This column will return NULL if the type returned is not associated with an XML schema collection.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_xml_document"             , "<html><p>Returns 1 if the returned data type is XML and that type is guaranteed to be a complete XML document (including a root node), as opposed to an XML fragment). Otherwise returns 0. </p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_case_sensitive"           , "<html><p>Returns 1 if the column is of a case-sensitive string type and 0 if it is not.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_fixed_length_clr_type"    , "<html><p>Returns 1 if the column is of a fixed-length CLR type and 0 if it is not.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "source_server"               , "<html><p>Name of the originating server returned by the column in this result (if it originates from a remote server). The name is given as it appears in <span class=\"literal\">sys.servers</span>. Returns NULL if the column originates on the local server, or if it cannot be determined which server it originates on. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "source_database"             , "<html><p>Name of the originating database returned by the column in this result. Returns NULL if the database cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "source_schema"               , "<html><p>Name of the originating schema returned by the column in this result. Returns NULL if the schema cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "source_table"                , "<html><p>Name of the originating table returned by the column in this result. Returns NULL if the table cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "source_column"               , "<html><p>Name of the originating column returned by the column in this result. Returns NULL if the column cannot be determined. Is only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_identity_column"          , "<html><p>Returns 1 if the column is an identity column and 0 if not. Returns NULL if it cannot be determined that the column is an identity column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_part_of_unique_key"       , "<html><p>Returns 1 if the column is part of a unique index (including unique and primary constraint) and 0 if not. Returns NULL if it cannot be determined that the column is part of a unique index. Only populated if browsing information is requested.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_updateable"               , "<html><p>Returns 1 if the column is updateable and 0 if not. Returns NULL if it cannot be determined that the column is updateable.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_computed_column"          , "<html><p>Returns 1 if the column is a computed column and 0 if not. Returns NULL if it cannot be determined that the column is a computed column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "is_sparse_column_set"        , "<html><p>Returns 1 if the column is a sparse column and 0 if not. Returns NULL if it cannot be determined that the column is a part of a sparse column set.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "ordinal_in_order_by_list"    , "<html><p>Position of this column in ORDER BY list Returns NULL if the column does not appear in the ORDER BY list or if the ORDER BY list cannot be uniquely determined.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "order_by_list_length"        , "<html><p>Length of the ORDER BY list. Returns NULL if there is no ORDER BY list or if the ORDER BY list cannot be uniquely determined. Note that this value will be the same for all rows returned by sp_describe_first_result_set.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "order_by_is_descending"      , "<html><p>If the ordinal_in_order_by_list is not NULL, the <strong>order_by_is_descending</strong> column reports the direction of the ORDER BY clause for this column. Otherwise it reports NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "error_number"                , "<html><p>Contains the error number returned by the function. Contains NULL if no error occurred in the column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "error_severity"              , "<html><p>Contains the severity returned by the function. Contains NULL if no error occurred in the column.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "error_state"                 , "<html><p>Contains the state message returned by the function. If no error occurred. the column will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "error_message"               , "<html><p>Contains the message returned by the function. If no error occurred, the column will contain NULL.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "error_type"                  , "<html><p>Contains an integer representing the error being returned. Maps to error_type_desc. See the list under remarks.</p></html>");
			mtd.addColumn("dm_exec_describe_first_result_set_for_object", "error_type_desc"             , "<html><p>Contains a short uppercase string representing the error being returned. Maps to error_type. See the list under remarks.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_describe_first_result_set_for_object' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_distributed_request_steps
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_distributed_request_steps",  "<p>Holds information about all steps that compose a given PolyBase request or query. It lists one row per query step.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_distributed_request_steps", "execution_id"      , "<html><p>execution_id and step_index make up the key for this view. Unique numeric id associated with the request.</p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "step_index"        , "<html><p>The position of this step in the sequence of steps that make up the request.</p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "operation_type"    , "<html><p>Type of the operation represented by this step.</p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "distribution_type" , "<html><p>Where the step is executing. </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "location_type"     , "<html><p>Where the step is executing.</p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "status"            , "<html><p>Status of this step </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "error_id"          , "<html><p>Unique id of the error associated with this step, if any </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "start_time"        , "<html><p>Time at which the step started execution </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "end_time"          , "<html><p>Time at which this step completed execution, was cancelled, or failed. </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "total_elapsed_time", "<html><p>Total amount of time the query step has been executing, in milliseconds </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "row_count"         , "<html><p>Total number of rows changed or returned by this request </p></html>");
			mtd.addColumn("dm_exec_distributed_request_steps", "command"           , "<html><p>Holds the full text of the command of this step.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_distributed_request_steps' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_distributed_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_distributed_requests",  "<p>Holds information about all requests currently or recently active in PolyBase queries. It lists one row per request/query.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2), Azure SQL Data Warehouse Public Preview.</p><p>Based on session and request ID, a user can then retrieve the actual distributed requests generated to be executed ? via sys.dm_exec_distributed_requests. For example, a query involving regular SQL and external SQL tables will be decomposed into various statements/requests executed across the various compute nodes. To track the distributed steps across all compute nodes, we introduce a ?global? execution ID which can be used to track all operations on the compute nodes associated with one particular request and operator, respectively.?? </p>");

			// Column names and description
			mtd.addColumn("dm_exec_distributed_requests", "sql_handle"        , "<html><p>Key for this view. Unique numeric id associated with the request.</p></html>");
			mtd.addColumn("dm_exec_distributed_requests", "execution_id"      , "<html><p>Unique numeric id associated with the session in which this query was run.</p></html>");
			mtd.addColumn("dm_exec_distributed_requests", "status"            , "<html><p>Current status of the request.</p></html>");
			mtd.addColumn("dm_exec_distributed_requests", "error_id"          , "<html><p>Unique id of the error associated with the request, if any.</p></html>");
			mtd.addColumn("dm_exec_distributed_requests", "start_time"        , "<html><p>Time at which the request execution was started.</p></html>");
			mtd.addColumn("dm_exec_distributed_requests", "end_time"          , "<html><p>Time at which the engine completed compiling the request.</p></html>");
			mtd.addColumn("dm_exec_distributed_requests", "total_elapsed_time", "<html><p>Time elapsed in execution since the request was started, in milliseconds.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_distributed_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_distributed_sql_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_distributed_sql_requests",  "<p>Holds information about all SQL query distributions as part of a SQL step in the query.? This view shows the data for the last 1000 requests; active requests always have the data present in this view.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_distributed_sql_requests", "execution_id"      , "<html><p>execution_id and step_index make up the key for this view. Unique numeric id associated with the request.</p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "step_index"        , "<html><p>Index of the query step this distribution is part of. </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "compute_node_id"   , "<html><p>Type of the operation represented by this step.</p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "distribution_id"   , "<html><p>Where the step is executing. </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "status"            , "<html><p>Status of this step </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "error_id"          , "<html><p>Unique id of the error associated with this step, if any </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "start_time"        , "<html><p>Time at which the step started execution </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "end_time"          , "<html><p>Time at which this step completed execution, was cancelled, or failed. </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "total_elapsed_time", "<html><p>Total amount of time the query step has been executing, in milliseconds </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "row_count"         , "<html><p>Total number of rows changed or returned by this request </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "spid"              , "<html><p>Session id on the SQL Server instance executing the query distribution </p></html>");
			mtd.addColumn("dm_exec_distributed_sql_requests", "command"           , "<html><p>Holds the full text of the command of this step.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_distributed_sql_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_dms_services
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_dms_services",  "<p>Holds information about all of the DMS services running on the PolyBase compute nodes. It lists one row per service instance.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_dms_services", "dms_core_id"    , "<html><p>Unique numeric id associated with the DMS core. Key for this view.</p></html>");
			mtd.addColumn("dm_exec_dms_services", "compute_node_id", "<html><p>ID of the node on which this DMS service is running</p></html>");
			mtd.addColumn("dm_exec_dms_services", "status"         , "<html><p>Current status of the DMS service </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_dms_services' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_dms_workers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_dms_workers",  "<p>Holds information about all workers completing DMS steps.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2), Azure SQL Data Warehouse Public Preview.</p><p>This view shows the data for the last 1000 requests and active requests; active requests always have the data present in this view.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_dms_workers", "execution_id"      , "<html><p>Query that this DMS worker is part of.request_id, step_index, and dms_step_index form the key for this view.</p></html>");
			mtd.addColumn("dm_exec_dms_workers", "step_index"        , "<html><p>Query step this DMS worker is part of.</p></html>");
			mtd.addColumn("dm_exec_dms_workers", "dms_step_index"    , "<html><p>Step in the DMS plan that this worker is running.</p></html>");
			mtd.addColumn("dm_exec_dms_workers", "compute_node_id"   , "<html><p>Node that the worker is running on.</p></html>");
			mtd.addColumn("dm_exec_dms_workers", "distribution_id"   , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "type"              , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "status"            , "<html><p>Status of this step </p></html>");
			mtd.addColumn("dm_exec_dms_workers", "bytes_per_sec"     , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "bytes_processed"   , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "rows_processed"    , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "start_time"        , "<html><p>Time at which the step started execution </p></html>");
			mtd.addColumn("dm_exec_dms_workers", "end_time"          , "<html><p>Time at which this step completed execution, was cancelled, or failed. </p></html>");
			mtd.addColumn("dm_exec_dms_workers", "total_elapsed_time", "<html><p>Total amount of time the query step has been executing, in milliseconds </p></html>");
			mtd.addColumn("dm_exec_dms_workers", "cpu_time"          , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "query_time"        , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "buffers_available" , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "dms_cpid"          , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "sql_spid"          , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "error_id"          , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "source_info"       , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "destination_info"  , "<html><p></p></html>");
			mtd.addColumn("dm_exec_dms_workers", "command"           , "<html><p></p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_dms_workers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_external_operations
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_external_operations",  "<p>Captures information about external PolyBase operations.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_external_operations", "execution_id"   , "<html><p>Unique query identifier associated with PolyBase query?? </p></html>");
			mtd.addColumn("dm_exec_external_operations", "step_index"     , "<html><p>Index of the query step </p></html>");
			mtd.addColumn("dm_exec_external_operations", "operation_ type", "<html><p>Describes a Hadoop operation or other external operation </p></html>");
			mtd.addColumn("dm_exec_external_operations", "operation_ name", "<html><p>Indicates how the status of job in percentage (how much is the input consumed)?? </p></html>");
			mtd.addColumn("dm_exec_external_operations", "map_? progress" , "<html><p>Indicates how the status of a reduce job in percentage, if any? </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_external_operations' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_external_work
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_external_work",  "<p>Returns information about the workload per worker, on each compute node.</p><p>Query sys.dm_exec_external_work to identify the work spun up to communicate with the external data source (e.g. Hadoop or external SQL Server).</p>");

			// Column names and description
			mtd.addColumn("dm_exec_external_work", "execution_id"      , "<html><p>Unique identifier for associated PolyBase query. </p></html>");
			mtd.addColumn("dm_exec_external_work", "step_index"        , "<html><p>The request this worker is performing.</p></html>");
			mtd.addColumn("dm_exec_external_work", "dms_step_index"    , "<html><p>Step in the DMS plan that this worker is executing.</p></html>");
			mtd.addColumn("dm_exec_external_work", "compute_node_id"   , "<html><p>?The node the worker is running on.</p></html>");
			mtd.addColumn("dm_exec_external_work", "type"              , "<html><p>The type of external work.</p></html>");
			mtd.addColumn("dm_exec_external_work", "work_id"           , "<html><p>ID of the actual split.</p></html>");
			mtd.addColumn("dm_exec_external_work", "input_name"        , "<html><p>Name of the input to be read</p></html>");
			mtd.addColumn("dm_exec_external_work", "read_location"     , "<html><p>Offset or read location.</p></html>");
			mtd.addColumn("dm_exec_external_work", "bytes_processed"   , "<html><p>Total bytes processed by this worker.</p></html>");
			mtd.addColumn("dm_exec_external_work", "length"            , "<html><p>Length of the split or HDFS block in case of Hadoop</p></html>");
			mtd.addColumn("dm_exec_external_work", "status"            , "<html><p>Status of the worker</p></html>");
			mtd.addColumn("dm_exec_external_work", "start_time"        , "<html><p>Beginning of the work</p></html>");
			mtd.addColumn("dm_exec_external_work", "end_time"          , "<html><p>End of the work</p></html>");
			mtd.addColumn("dm_exec_external_work", "total_elapsed_time", "<html><p>Total time in milliseconds</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_external_work' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_function_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_function_stats",  "<p>Returns aggregate performance statistics for cached functions. The view returns one row for each cached function plan, and the lifetime of the row is as long as the function remains cached. When a function is removed from the cache, the corresponding row is eliminated from this view. At that time, a Performance Statistics SQL trace event is raised similar to <strong>sys.dm_exec_query_stats</strong>. Returns information about scalar functions, including in-memory functions and CLR scalar functions. Does not return information about table valued functions.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_function_stats", "database_id"         , "<html><p>Database ID in which the function resides.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "object_id"           , "<html><p>Object identification number of the function.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "type"                , "<html><p>Type of the object: FN = Scalar valued functions</p></html>");
			mtd.addColumn("dm_exec_function_stats", "type_desc"           , "<html><p>Description of the object type: SQL_SCALAR_FUNCTION</p></html>");
			mtd.addColumn("dm_exec_function_stats", "sql_handle"          , "<html><p>This can be used to correlate with queries in <strong>sys.dm_exec_query_stats</strong> that were executed from within this function.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "plan_handle"         , "<html><p>Identifier for the in-memory plan. This identifier is transient and remains constant only while the plan remains in the cache. This value may be used with the <strong>sys.dm_exec_cached_plans</strong> dynamic management view.</p><p>Will always be 0x000 when a natively compiled function queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "cached_time"         , "<html><p>Time at which the function was added to the cache.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "last_execution_time" , "<html><p>Last time at which the function was executed.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "execution_count"     , "<html><p>Number of times that the function has been executed since it was last compiled.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "total_worker_time"   , "<html><p>Total amount of CPU time, in microseconds, that was consumed by executions of this function since it was compiled.</p><p>For natively compiled functions, <strong>total_worker_time</strong> may not be accurate if many executions take less than 1 millisecond.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "last_worker_time"    , "<html><p>CPU time, in microseconds, that was consumed the last time the function was executed. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_function_stats", "min_worker_time"     , "<html><p>Minimum CPU time, in microseconds, that this function has ever consumed during a single execution. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_function_stats", "max_worker_time"     , "<html><p>Maximum CPU time, in microseconds, that this function has ever consumed during a single execution. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_function_stats", "total_physical_reads", "<html><p>Total number of physical reads performed by executions of this function since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "last_physical_reads" , "<html><p>Number of physical reads performed the last time the function was executed.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "min_physical_reads"  , "<html><p>Minimum number of physical reads that this function has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "max_physical_reads"  , "<html><p>Maximum number of physical reads that this function has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "total_logical_writes", "<html><p>Total number of logical writes performed by executions of this function since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "last_logical_writes" , "<html><p>Number of the number of buffer pool pages dirtied the last time the plan was executed. If a page is already dirty (modified) no writes are counted.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "min_logical_writes"  , "<html><p>Minimum number of logical writes that this function has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "max_logical_writes"  , "<html><p>Maximum number of logical writes that this function has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "total_logical_reads" , "<html><p>Total number of logical reads performed by executions of this function since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "last_logical_reads"  , "<html><p>Number of logical reads performed the last time the function was executed.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "min_logical_reads"   , "<html><p>Minimum number of logical reads that this function has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "max_logical_reads"   , "<html><p>Maximum number of logical reads that this function has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "total_elapsed_time"  , "<html><p>Total elapsed time, in microseconds, for completed executions of this function.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "last_elapsed_time"   , "<html><p>Elapsed time, in microseconds, for the most recently completed execution of this function.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "min_elapsed_time"    , "<html><p>Minimum elapsed time, in microseconds, for any completed execution of this function.</p></html>");
			mtd.addColumn("dm_exec_function_stats", "max_elapsed_time"    , "<html><p>Maximum elapsed time, in microseconds, for any completed execution of this function.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_function_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_query_memory_grants
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_memory_grants",  "<p>Returns information about the queries that have acquired a memory grant or that still require a memory grant to execute. Queries that do not have to wait on a memory grant will not appear in this view.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out. In addition, the values in the columns <strong>scheduler_id</strong>, <strong>wait_order</strong>, <strong>pool_id</strong>, <strong>group_id</strong> are filtered; the column value is set to NULL.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_query_memory_grants", "session_id"           , "<html><p>ID (SPID) of the session where this query is running.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "request_id"           , "<html><p>ID of the request. Unique in the context of the session.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "scheduler_id"         , "<html><p>ID of the scheduler that is scheduling this query.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "dop"                  , "<html><p>Degree of parallelism of this query.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "request_time"         , "<html><p>Date and time when this query requested the memory grant.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "grant_time"           , "<html><p>Date and time when memory was granted for this query. NULL if memory is not granted yet.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "requested_memory_kb"  , "<html><p>Total requested amount of memory in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "granted_memory_kb"    , "<html><p>Total amount of memory actually granted in kilobytes. Can be NULL if the memory is not granted yet. For a typical situation, this value should be the same as <strong>requested_memory_kb</strong>. For index creation, the server may allow additional on-demand memory beyond initially granted memory.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "required_memory_kb"   , "<html><p>Minimum memory required to run this query in kilobytes. <strong>requested_memory_kb</strong> is the same or larger than this amount.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "used_memory_kb"       , "<html><p>Physical memory used at this moment in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "max_used_memory_kb"   , "<html><p>Maximum physical memory used up to this moment in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "query_cost"           , "<html><p>Estimated query cost.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "timeout_sec"          , "<html><p>Time-out in seconds before this query gives up the memory grant request.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "resource_semaphore_id", "<html><p>Non-unique ID of the resource semaphore on which this query is waiting.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>This ID is unique in versions of SQL Server that are earlier than SQL Server 2008. This change can affect troubleshooting query execution. For more information, see the \"Remarks\" section later in this topic.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "queue_id"             , "<html><p>ID of waiting queue where this query waits for memory grants. NULL if the memory is already granted.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "wait_order"           , "<html><p>Sequential order of waiting queries within the specified <strong>queue_id</strong>. This value can change for a given query if other queries get memory grants or time out. NULL if memory is already granted.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "is_next_candidate"    , "<html><p>Candidate for next memory grant. </p><p>1 = Yes </p><p>0 = No</p><p>NULL = Memory is already granted.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "wait_time_ms"         , "<html><p>Wait time in milliseconds. NULL if the memory is already granted.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "plan_handle"          , "<html><p>Identifier for this query plan. Use <strong>sys.dm_exec_query_plan</strong> to extract the actual XML plan.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "sql_handle"           , "<html><p>Identifier for Transact-SQL text for this query. Use <strong>sys.dm_exec_sql_text</strong> to get the actual Transact-SQL text.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "group_id"             , "<html><p>ID for the workload group where this query is running.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "pool_id"              , "<html><p>ID of the resource pool that this workload group belongs to.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "is_small"             , "<html><p>When set to 1, indicates that this grant uses the small resource semaphore. When set to 0, indicates that a regular semaphore is used.</p></html>");
			mtd.addColumn("dm_exec_query_memory_grants", "ideal_memory_kb"      , "<html><p>Size, in kilobytes (KB), of the memory grant to fit everything into physical memory. This is based on the cardinality estimate.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_memory_grants' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_query_optimizer_info
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_optimizer_info",  "<p></p>");

			// Column names and description
			mtd.addColumn("dm_exec_query_optimizer_info", "name"            , "<html><p>column contains the internal name for the rule. An example is JNtoSM � an implementation rule to transform a logical inner join to a physical sort-merge join operator.</p></html>");
			mtd.addColumn("dm_exec_query_optimizer_info", "promised"        , "<html><p>The promised column shows how many times the rule has been asked to provide a promise value to the optimizer.</p></html>");
			mtd.addColumn("dm_exec_query_optimizer_info", "promise_total"   , "<html><p>The promise_total column is the sum of all the promise values returned.</p></html>");
			mtd.addColumn("dm_exec_query_optimizer_info", "promise_avg"     , "<html><p>The promise_avg column is promise_total divided by promised.</p></html>");
			mtd.addColumn("dm_exec_query_optimizer_info", "built_substitute", "<html><p>The built_substitute column tracks how many times the rule has produced an alternative implementation for a logical group.</p></html>");
			mtd.addColumn("dm_exec_query_optimizer_info", "succeeded"       , "<html><p>The succeeded column tracks the number of times that a rule generated a transformation that was successfully added to the space of valid alternative strategies.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_optimizer_info' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_query_transformation_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_transformation_stats",  "<p>Returns detailed statistics about the operation of the SQL Server query optimizer. You can use this view when tuning a workload to identify query optimization problems or improvements. For example, you can use the total number of optimizations, the elapsed time value, and the final cost value to compare the query optimizations of the current workload and any changes observed during?the tuning process.?Some counters provide data that?is relevant only?for?SQL Server internal diagnostic use. These counters are marked as \"Internal only.\"</p>");

			// Column names and description
			mtd.addColumn("dm_exec_query_transformation_stats", "counter"   , "<html><p>Name of optimizer statistics event.</p></html>");
			mtd.addColumn("dm_exec_query_transformation_stats", "occurrence", "<html><p>Number of occurrences of optimization event for this counter.</p></html>");
			mtd.addColumn("dm_exec_query_transformation_stats", "value"     , "<html><p>Average property value per event occurrence. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_optimizer_info' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_plan_attributes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_plan_attributes",  "<p>Returns one row per plan attribute for the plan specified by the plan handle. You can use this table-valued function to get details about a particular plan, such as the cache key values or the number of current simultaneous executions of the plan. </p>");

			String attributesDetails = ""
				+ "<table>\n"
				+ "<thead>\n"
				+ "<tr><th>Attribute</th><th>Description</th></tr>\n"
				+ "</thead>\n"
				+ "<tbody>\n"
				+ "<tr><td>set_options				</td><td>Indicates the option values that the plan was compiled with.</td></tr>\n"
				+ "<tr><td>objectid					</td><td>One of the main keys used for looking up an object in the cache. This is the object ID stored in sys.objects for database objects (procedures, views, triggers, and so on). For plans of type 'Adhoc' or 'Prepared', it is an internal hash of the batch text.</td></tr>\n"
				+ "<tr><td>dbid						</td><td>Is the ID of the database containing the entity the plan refers to. For ad hoc or prepared plans, it is the database ID from which the batch is executed.</td></tr>\n"
				+ "<tr><td>dbid_execute				</td><td>For system objects stored in the Resource database, the database ID from which the cached plan is executed. For all other cases, it is 0.</td></tr>\n"
				+ "<tr><td>user_id					</td><td>Value of -2 indicates that the batch submitted does not depend on implicit name resolution and can be shared among different users. This is the preferred method. Any other value represents the user ID of the user submitting the query in the database.</td></tr>\n"
				+ "<tr><td>language_id				</td><td>ID of the language of the connection that created the cache object. For more information, see sys.syslanguages (Transact-SQL).</td></tr>\n"
				+ "<tr><td>date_format				</td><td>Date format of the connection that created the cache object. For more information, see SET DATEFORMAT (Transact-SQL).</td></tr>\n"
				+ "<tr><td>date_first				</td><td>Date first value. For more information, see SET DATEFIRST (Transact-SQL).</td></tr>\n"
				+ "<tr><td>compat_level				</td><td>Represents the compatibility level set in the database in whose context the query plan was compiled. The compatibility level returned is the compatibility level of the current database context for adhoc statements, and is unaffected by the query hint QUERY_OPTIMIZER_COMPATIBILITY_LEVEL_n. For statements contained in a stored procedure or function it corresponds to the compatibility level of the database in which the stored procedure or function is created.</td></tr>\n"
				+ "<tr><td>status					</td><td>Internal status bits that are part of the cache lookup key.</td></tr>\n"
				+ "<tr><td>required_cursor_options	</td><td>Cursor options specified by the user such as the cursor type.</td></tr>\n"
				+ "<tr><td>acceptable_cursor_options</td><td>Cursor options that SQL Server may implicitly convert to in order to support the execution of the statement. For example, the user may specify a dynamic cursor, but the query optimizer is permitted to convert this cursor type to a static cursor.</td></tr>\n"
				+ "<tr><td>merge_action_type		</td><td>The type of trigger execution plan used as the result of a MERGE statement.\n"
				+ "  <ul>\n"
				+ "	<li>0 indicates a non-trigger plan, a trigger plan that does not execute as the result of a MERGE statement, or a trigger plan that executes as the result of a MERGE statement that only specifies a DELETE action.</li>\n"
				+ "	<li>1 indicates an INSERT trigger plan that runs as the result of a MERGE statement.</li>\n"
				+ "	<li>2 indicates an UPDATE trigger plan that runs as the result of a MERGE statement.</li>\n"
				+ "	<li>3 indicates a DELETE trigger plan that runs as the result of a MERGE statement containing a corresponding INSERT or UPDATE action.</li>\n"
				+ "  </ul>\n"
				+ "  For nested triggers run by cascading actions, this value is the action of the MERGE statement that caused the cascade.</td></tr>\n"
				+ "<tr><td>is_replication_specific	</td><td>Represents that the session from which this plan was compiled is one that connected to the instance of SQL Server using an undocumented connection property which allows the server to identify the session as one created by replication components, so that the behavior of certain functional aspects of the server are changed according to what such replication component expects.</td></tr>\n"
				+ "<tr><td>optional_spid			</td><td>The connection session_id (spid) becomes part of the cache key in order to reduce the number of re-compiles. This prevents recompilations for a single session's re-use of a plan involving non-dynamically bound temp tables.</td></tr>\n"
				+ "<tr><td>optional_clr_trigger_dbid</td><td>Only populated in the case of a CLR DML trigger. The ID of the database containing the entity. For any other object type, returns zero.</td></tr>\n"
				+ "<tr><td>optional_clr_trigger_objid</td><td>Only populated in the case of a CLR DML trigger. The object ID stored in sys.objects. For any other object type, returns zero.</td></tr>\n"
				+ "<tr><td>parent_plan_handle		</td><td>Always NULL.</td></tr>\n"
				+ "<tr><td>is_azure_user_plan		</td><td>\n"
				+ "  <ul>\n"
				+ "    <li>1 for queries executed in an Azure SQL Database from a session initiated by a user.</li>\n"
				+ "	<li>0 for queries that have been executed from a session not initiated by an end user, but by applications running from within Azure infrastructure that issue queries for other purposes of collecting telemetry or executing administrative tasks. Customers are not charged for resources consumed by queries where is_azure_user_plan = 0.</li>\n"
				+ "  </ul>\n"
				+ "  Azure SQL Database only.</td></tr>\n"
				+ "<tr><td>inuse_exec_context		</td><td>Number of currently executing batches that are using the query plan.</td></tr>\n"
				+ "<tr><td>free_exec_context		</td><td>Number of cached execution contexts for the query plan that are not being currently used.</td></tr>\n"
				+ "<tr><td>hits_exec_context		</td><td>Number of times the execution context was obtained from the plan cache and reused, saving the overhead of recompiling the SQL statement. The value is an aggregate for all batch executions so far.</td></tr>\n"
				+ "<tr><td>misses_exec_context		</td><td>Number of times that an execution context could not be found in the plan cache, resulting in the creation of a new execution context for the batch execution.</td></tr>\n"
				+ "<tr><td>removed_exec_context		</td><td>Number of execution contexts that have been removed because of memory pressure on the cached plan.</td></tr>\n"
				+ "<tr><td>inuse_cursors			</td><td>Number of currently executing batches containing one or more cursors that are using the cached plan.</td></tr>\n"
				+ "<tr><td>free_cursors				</td><td>Number of idle or free cursors for the cached plan.</td></tr>\n"
				+ "<tr><td>hits_cursors				</td><td>Number of times that an inactive cursor was obtained from the cached plan and reused. The value is an aggregate for all batch executions so far.</td></tr>\n"
				+ "<tr><td>misses_cursors			</td><td>Number of times that an inactive cursor could not be found in the cache.</td></tr>\n"
				+ "<tr><td>removed_cursors			</td><td>Number of cursors that have been removed because of memory pressure on the cached plan.</td></tr>\n"
				+ "<tr><td>sql_handle				</td><td>The SQL handle for the batch.</td></tr>\n"
				+ "</tbody>\n"
				+ "</table>\n"
				+ "";

			// Column names and description
			mtd.addColumn("dm_exec_plan_attributes", "attribute"   , "<html><p>Name of the attribute associated with this plan. One of the following:<br>" + attributesDetails +"</html>");
			mtd.addColumn("dm_exec_plan_attributes", "value"       , "<html><p>Value of the attribute that is associated with this plan.</p></html>");
			mtd.addColumn("dm_exec_plan_attributes", "is_cache_key", "<html><p>Indicates whether the attribute is used as part of the cache lookup key for the plan.</p></html>");

			
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_plan_attributes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_procedure_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_procedure_stats",  "<p>Returns aggregate performance statistics for cached stored procedures. The view returns one row for each cached stored procedure plan, and the lifetime of the row is as long as the stored procedure remains cached. When a stored procedure is removed from the cache, the corresponding row is eliminated from this view. At that time, a Performance Statistics SQL trace event is raised similar to <strong>sys.dm_exec_query_stats</strong>.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_procedure_stats", "database_id"         , "<html><p>Database ID in which the stored procedure resides.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "object_id"           , "<html><p>Object identification number of the stored procedure.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "type"                , "<html><p>Type of the object:</p><p> P = SQL stored procedure</p><p> PC = Assembly (CLR) stored procedure</p><p> X = Extended stored procedure</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "type_desc"           , "<html><p>Description of the object type:</p><p>SQL_STORED_PROCEDURE</p><p>CLR_STORED_PROCEDURE</p><p>EXTENDED_STORED_PROCEDURE</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "sql_handle"          , "<html><p>This can be used to correlate with queries in <strong>sys.dm_exec_query_stats</strong> that were executed from within this stored procedure.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "plan_handle"         , "<html><p>Identifier for the in-memory plan. This identifier is transient and remains constant only while the plan remains in the cache. This value may be used with the <strong>sys.dm_exec_cached_plans</strong> dynamic management view.</p><p>Will always be 0x000 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "cached_time"         , "<html><p>Time at which the stored procedure was added to the cache.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "last_execution_time" , "<html><p>Last time at which the stored procedure was executed.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "execution_count"     , "<html><p>Number of times that the stored procedure has been executed since it was last compiled.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "total_worker_time"   , "<html><p>Total amount of CPU time, in microseconds, that was consumed by executions of this stored procedure since it was compiled.</p><p>For natively compiled stored procedures, <strong>total_worker_time</strong> may not be accurate if many executions take less than 1 millisecond.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "last_worker_time"    , "<html><p>CPU time, in microseconds, that was consumed the last time the stored procedure was executed. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "min_worker_time"     , "<html><p>Minimum CPU time, in microseconds, that this stored procedure has ever consumed during a single execution. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "max_worker_time"     , "<html><p>Maximum CPU time, in microseconds, that this stored procedure has ever consumed during a single execution. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "total_physical_reads", "<html><p>Total number of physical reads performed by executions of this stored procedure since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "last_physical_reads" , "<html><p>Number of physical reads performed the last time the stored procedure was executed.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "min_physical_reads"  , "<html><p>Minimum number of physical reads that this stored procedure has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "max_physical_reads"  , "<html><p>Maximum number of physical reads that this stored procedure has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "total_logical_writes", "<html><p>Total number of logical writes performed by executions of this stored procedure since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "last_logical_writes" , "<html><p>Number of the number of buffer pool pages dirtied the last time the plan was executed. If a page is already dirty (modified) no writes are counted.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "min_logical_writes"  , "<html><p>Minimum number of logical writes that this stored procedure has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "max_logical_writes"  , "<html><p>Maximum number of logical writes that this stored procedure has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "total_logical_reads" , "<html><p>Total number of logical reads performed by executions of this stored procedure since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "last_logical_reads"  , "<html><p>Number of logical reads performed the last time the stored procedure was executed.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "min_logical_reads"   , "<html><p>Minimum number of logical reads that this stored procedure has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "max_logical_reads"   , "<html><p>Maximum number of logical reads that this stored procedure has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "total_elapsed_time"  , "<html><p>Total elapsed time, in microseconds, for completed executions of this stored procedure.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "last_elapsed_time"   , "<html><p>Elapsed time, in microseconds, for the most recently completed execution of this stored procedure.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "min_elapsed_time"    , "<html><p>Minimum elapsed time, in microseconds, for any completed execution of this stored procedure.</p></html>");
			mtd.addColumn("dm_exec_procedure_stats", "max_elapsed_time"    , "<html><p>Maximum elapsed time, in microseconds, for any completed execution of this stored procedure.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_procedure_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_query_plan
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_plan",  "<p>Returns the Showplan in XML format for the batch specified by the plan handle. The plan specified by the plan handle can either be cached or currently executing.</p><p>The XML schema for the Showplan is published and available at <a href=\"http://go.microsoft.com/fwlink/?linkid=43100&amp;clcid=0x409\">this Microsoft Web site</a>. It is also available in the directory where SQL Server is installed.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_query_plan", "dbid"      , "<html><p>ID of the context database that was in effect when the Transact-SQL statement corresponding to this plan was compiled. For ad hoc and prepared SQL statements, the ID of the database where the statements were compiled.</p><p>Column is nullable.</p></html>");
			mtd.addColumn("dm_exec_query_plan", "objectid"  , "<html><p>ID of the object (for example, stored procedure or user-defined function) for this query plan. For ad hoc and prepared batches, this column is <strong>null</strong>.</p><p>Column is nullable.</p></html>");
			mtd.addColumn("dm_exec_query_plan", "number"    , "<html><p>Numbered stored procedure integer. For example, a group of procedures for the <strong>orders</strong> application may be named <strong>orderproc;1</strong>, <strong>orderproc;2</strong>, and so on. For ad hoc and prepared batches, this column is <strong>null</strong>.</p><p>Column is nullable.</p></html>");
			mtd.addColumn("dm_exec_query_plan", "encrypted" , "<html><p>Indicates whether the corresponding stored procedure is encrypted.</p><p>0 = not encrypted</p><p>1 = encrypted</p><p>Column is not nullable.</p></html>");
			mtd.addColumn("dm_exec_query_plan", "query_plan", "<html><p>Contains the compile-time Showplan representation of the query execution plan that is specified with <em>plan_handle</em>. The Showplan is in XML format. One plan is generated for each batch that contains, for example ad hoc Transact-SQL statements, stored procedure calls, and user-defined function calls.</p><p>Column is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_plan' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_query_resource_semaphores
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_resource_semaphores",  "<p>Returns the information about the current query-resource semaphore status in SQL Server. <strong>sys.dm_exec_query_resource_semaphores</strong> provides general query-execution memory status and allows you to determine whether the system can access enough memory. This view complements memory information obtained from <a href=\"https://msdn.microsoft.com/en-us/library/ms175019.aspx\">sys.dm_os_memory_clerks</a> to provide a complete picture of server memory status. <strong>sys.dm_exec_query_resource_semaphores</strong> returns one row for the regular resource semaphore and another row for the small-query resource semaphore. There are two requirements for a small-query semaphore:</p><ul class=\"unordered\"> <li><p>The memory grant requested should be less than 5 MB</p></li> <li><p>The query cost should be less than 3 cost units</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_exec_query_resource_semaphores", "resource_semaphore_id", "<html><p>Nonunique ID of the resource semaphore. 0 for the regular resource semaphore and 1 for the small-query resource semaphore. </p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "target_memory_kb"     , "<html><p>Grant usage target in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "max_target_memory_kb" , "<html><p>Maximum potential target in kilobytes. NULL for the small-query resource semaphore.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "total_memory_kb"      , "<html><p>Memory held by the resource semaphore in kilobytes. If the system is under memory pressure or if forced minimum memory is granted frequently, this value can be larger than the <strong>target_memory_kb</strong> or <strong>max_target_memory_kb</strong> values. Total memory is a sum of available and granted memory.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "available_memory_kb"  , "<html><p>Memory available for a new grant in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "granted_memory_kb"    , "<html><p>Total granted memory in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "used_memory_kb"       , "<html><p>Physically used part of granted memory in kilobytes.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "grantee_count"        , "<html><p>Number of active queries that have their grants satisfied.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "waiter_count"         , "<html><p>Number of queries waiting for grants to be satisfied.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "timeout_error_count"  , "<html><p>Total number of time-out errors since server startup. NULL for the small-query resource semaphore.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "forced_grant_count"   , "<html><p>Total number of forced minimum-memory grants since server startup. NULL for the small-query resource semaphore.</p></html>");
			mtd.addColumn("dm_exec_query_resource_semaphores", "pool_id"              , "<html><p>ID of the resource pool to which this resource semaphore belongs.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_resource_semaphores' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_query_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_profiles",  "<p>Monitors real time query progress while the query is in execution. For example, use this DMV to determine which part of the query is running slow. Join this DMV with other system DMVs using the columns identified in the description field. Or, join this DMV with other performance counters (such as Performance Monitor, xperf) by using the timestamp columns.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_query_profiles", "session_id"                 , "<html>Identifies the session in which this query runs. References dm_exec_sessions.session_id.</html>");
			mtd.addColumn("dm_exec_query_profiles", "request_id"                 , "<html>Identifies the target request. References dm_exec_sessions.request_id.</html>");
			mtd.addColumn("dm_exec_query_profiles", "sql_handle"                 , "<html>Is a token that uniquely identifies the batch or stored procedure that the query is part of. References dm_exec_query_stats.sql_handle.</html>");
			mtd.addColumn("dm_exec_query_profiles", "plan_handle"                , "<html>Is a token that uniquely identifies a query execution plan for a batch that has executed and its plan resides in the plan cache, or is currently executing. References dm_exec_query_stats.plan_handle.</html>");
			mtd.addColumn("dm_exec_query_profiles", "physical_operator_name"     , "<html>Physical operator name.</html>");
			mtd.addColumn("dm_exec_query_profiles", "node_id"                    , "<html>Identifies an operator node in the query tree.</html>");
			mtd.addColumn("dm_exec_query_profiles", "thread_id"                  , "<html>Distinguishes the threads (for a parallel query) belonging to the same query operator node.</html>");
			mtd.addColumn("dm_exec_query_profiles", "task_address"               , "<html>Identifies the SQLOS task that this thread is using. References dm_os_tasks.task_address.</html>");
			mtd.addColumn("dm_exec_query_profiles", "row_count"                  , "<html>Number of rows returned by the operator so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "rewind_count"               , "<html>Number of rewinds so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "rebind_count"               , "<html>Number of rebinds so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "end_of_scan_count"          , "<html>Number of end of scans so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "estimate_row_count"         , "<html>Estimated number of rows. It can be useful to compare to estimated_row_count to the actual row_count.</html>");
			mtd.addColumn("dm_exec_query_profiles", "first_active_time"          , "<html>The time, in milliseconds, when the operator was first called.</html>");
			mtd.addColumn("dm_exec_query_profiles", "last_active_time"           , "<html>The time, in milliseconds, when the operator was last called.</html>");
			mtd.addColumn("dm_exec_query_profiles", "open_time"                  , "<html>Timestamp when open (in milliseconds).</html>");
			mtd.addColumn("dm_exec_query_profiles", "first_row_time"             , "<html>Timestamp when first row was opened (in milliseconds).</html>");
			mtd.addColumn("dm_exec_query_profiles", "last_row_time"              , "<html>Timestamp when last row was opened(in milliseconds).</html>");
			mtd.addColumn("dm_exec_query_profiles", "close_time"                 , "<html>Timestamp when close (in milliseconds).</html>");
			mtd.addColumn("dm_exec_query_profiles", "elapsed_time_ms"            , "<html>Total elapsed time (in milliseconds) used by the target node's operations so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "cpu_time_ms"                , "<html>Total CPU time (in milliseconds) use by target node's operations so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "database_id"                , "<html>ID of the database that contains the object on which the reads and writes are being performed.</html>");
			mtd.addColumn("dm_exec_query_profiles", "object_id"                  , "<html>The identifier for the object on which the reads and writes are being performed. References sys.objects.object_id.</html>");
			mtd.addColumn("dm_exec_query_profiles", "index_id"                   , "<html>The index (if any) the rowset is opened against.</html>");
			mtd.addColumn("dm_exec_query_profiles", "scan_count"                 , "<html>Number of table/index scans so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "logical_read_count"         , "<html>Number of logical reads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "physical_read_count"        , "<html>Number of physical reads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "read_ahead_count"           , "<html>Number of read-aheads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "write_page_count"           , "<html>Number of page-writes so far due to spilling.</html>");
			mtd.addColumn("dm_exec_query_profiles", "lob_logical_read_count"     , "<html>Number of LOB logical reads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "lob_physical_read_count"    , "<html>Number of LOB physical reads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "lob_read_ahead_count"       , "<html>Number of LOB read-aheads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "segment_read_count"         , "<html>Number of segment read-aheads so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "segment_skip_count"         , "<html>Number of segments skipped so far.</html>");
			mtd.addColumn("dm_exec_query_profiles", "actual_read_row_count"      , "<html>Number of rows read by an operator before the residual predicate was applied.</html>");
			mtd.addColumn("dm_exec_query_profiles", "estimated_read_row_count"   , "<html>Number of rows estimated to be read by an operator before the residual predicate was applied. <br>Applies to: Beginning with SQL Server 2016 (13.x) SP1. </html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_profiles' to MonTablesDictionary. Caught: " + e, e);
		}

		
		
		// ---------------------------------------------------------------------------------------
		// dm_exec_query_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_query_stats",  "<p>Returns aggregate performance statistics for cached query plans in SQL Server. The view contains one row per query statement within the cached plan, and the lifetime of the rows are tied to the plan itself. When a plan is removed from the cache, the corresponding rows are eliminated from this view. </p>");

			// Column names and description
			mtd.addColumn("dm_exec_query_stats", "sql_handle"            , "<html><p>Is a token that refers to the batch or stored procedure that the query is part of. </p><p><strong>sql_handle</strong>, together with <strong>statement_start_offset</strong> and <strong>statement_end_offset</strong>, can be used to retrieve the SQL text of the query by calling the <strong>sys.dm_exec_sql_text</strong> dynamic management function.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "statement_start_offset", "<html><p>Indicates, in bytes, beginning with 0, the starting position of the query that the row describes within the text of its batch or persisted object.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "statement_end_offset"  , "<html><p>Indicates, in bytes, starting with 0, the ending position of the query that the row describes within the text of its batch or persisted object. For versions before SQL Server 2014, a value of -1 indicates the end of the batch. Trailing comments are no longer include.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "plan_generation_num"   , "<html><p>A sequence number that can be used to distinguish between instances of plans after a recompile.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "plan_handle"           , "<html><p>A token that refers to the compiled plan that the query is part of. This value can be passed to the <a href=\"https://msdn.microsoft.com/en-us/library/ms189747.aspx\">sys.dm_exec_query_plan</a> dynamic management function to obtain the query plan.</p><p>Will always be 0x000 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "creation_time"         , "<html><p>Time at which the plan was compiled.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_execution_time"   , "<html><p>Last time at which the plan started executing.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "execution_count"       , "<html><p>Number of times that the plan has been executed since it was last compiled.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_worker_time"     , "<html><p>Total amount of CPU time, reported in microseconds (but only accurate to milliseconds), that was consumed by executions of this plan since it was compiled.</p><p>For natively compiled stored procedures, <strong>total_worker_time</strong> may not be accurate if many executions take less than 1 millisecond.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_worker_time"      , "<html><p>CPU time, reported in microseconds (but only accurate to milliseconds), that was consumed the last time the plan was executed. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_worker_time"       , "<html><p>Minimum CPU time, reported in microseconds (but only accurate to milliseconds), that this plan has ever consumed during a single execution. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_worker_time"       , "<html><p>Maximum CPU time, reported in microseconds (but only accurate to milliseconds), that this plan has ever consumed during a single execution. <span class=\"sup\">1</span></p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_physical_reads"  , "<html><p>Total number of physical reads performed by executions of this plan since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_physical_reads"   , "<html><p>Number of physical reads performed the last time the plan was executed.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_physical_reads"    , "<html><p>Minimum number of physical reads that this plan has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_physical_reads"    , "<html><p>Maximum number of physical reads that this plan has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_logical_writes"  , "<html><p>Total number of logical writes performed by executions of this plan since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_logical_writes"   , "<html><p>Number of the number of buffer pool pages dirtied the last time the plan was executed. If a page is already dirty (modified) no writes are counted.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_logical_writes"    , "<html><p>Minimum number of logical writes that this plan has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_logical_writes"    , "<html><p>Maximum number of logical writes that this plan has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_logical_reads"   , "<html><p>Total number of logical reads performed by executions of this plan since it was compiled.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_logical_reads"    , "<html><p>Number of logical reads performed the last time the plan was executed.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_logical_reads"     , "<html><p>Minimum number of logical reads that this plan has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_logical_reads"     , "<html><p>Maximum number of logical reads that this plan has ever performed during a single execution.</p><p>Will always be 0 querying a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_clr_time"        , "<html><p>Time, reported in microseconds (but only accurate to milliseconds), consumed inside Microsoft?.NET Framework common language runtime (CLR) objects by executions of this plan since it was compiled. The CLR objects can be stored procedures, functions, triggers, types, and aggregates.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_clr_time"         , "<html><p>Time, reported in microseconds (but only accurate to milliseconds) consumed by execution inside .NET Framework CLR objects during the last execution of this plan. The CLR objects can be stored procedures, functions, triggers, types, and aggregates.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_clr_time"          , "<html><p>Minimum time, reported in microseconds (but only accurate to milliseconds), that this plan has ever consumed inside .NET Framework CLR objects during a single execution. The CLR objects can be stored procedures, functions, triggers, types, and aggregates.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_clr_time"          , "<html><p>Maximum time, reported in microseconds (but only accurate to milliseconds), that this plan has ever consumed inside the .NET Framework CLR during a single execution. The CLR objects can be stored procedures, functions, triggers, types, and aggregates.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_elapsed_time"    , "<html><p>Total elapsed time, reported in microseconds (but only accurate to milliseconds), for completed executions of this plan.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_elapsed_time"     , "<html><p>Elapsed time, reported in microseconds (but only accurate to milliseconds), for the most recently completed execution of this plan.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_elapsed_time"      , "<html><p>Minimum elapsed time, reported in microseconds (but only accurate to milliseconds), for any completed execution of this plan.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_elapsed_time"      , "<html><p>Maximum elapsed time, reported in microseconds (but only accurate to milliseconds), for any completed execution of this plan.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "query_hash"            , "<html><p>Binary hash value calculated on the query and used to identify queries with similar logic. You can use the query hash to determine the aggregate resource usage for queries that differ only by literal values. </p></html>");
			mtd.addColumn("dm_exec_query_stats", "query_plan_hash"       , "<html><p>Binary hash value calculated on the query execution plan and used to identify similar query execution plans. You can use query plan hash to find the cumulative cost of queries with similar execution plans.</p><p>Will always be 0x000 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_rows"            , "<html><p>Total number of rows returned by the query. Cannot be null.</p><p>Will always be 0 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_rows"             , "<html><p>Number of rows returned by the last execution of the query. Cannot be null.</p><p>Will always be 0 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_rows"              , "<html><p>Minimum number of rows returned by the query over the number of times that the plan has been executed since it was last compiled. Cannot be null.</p><p>Will always be 0 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_rows"              , "<html><p>Maximum number of rows returned by the query over the number of times that the plan has been executed since it was last compiled. Cannot be null.</p><p>Will always be 0 when a natively compiled stored procedure queries a memory-optimized table.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "statement_sql_handle"  , "<html><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p><p>Reserved for future use.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "statement_context_id"  , "<html><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p><p>Reserved for future use.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_dop"             , "<html><p>The total sum of degree of parallelism this plan used since it was compiled. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_dop"              , "<html><p>The degree of parallelism when this plan ran last time. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_dop?"              , "<html><p>The minimum degree of parallelism this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_dop"               , "<html><p>The maximum degree of parallelism this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_grant_kb"        , "<html><p>The total amount of reserved memory grant in KB this plan received since it was compiled. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_grant_kb"         , "<html><p>The amount of reserved memory grant in KB when this plan ran last time. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_to_grant_kb"       , "<html><p>The minimum amount of reserved memory grant in KB this plan ever received during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_grant_kb"          , "<html><p>The maximum amount of reserved memory grant in KB this plan ever received during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_used_grant_kb"   , "<html><p>The total amount of reserved memory grant in KB this plan used since it was compiled. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_used_grant_kb"    , "<html><p>The amount of used memory grant in KB when this plan ran last time. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_used_grant_kb"     , "<html><p>The minimum amount of used memory grant in KB this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_used_grant_kb"     , "<html><p>The maximum amount of used memory grant in KB this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_ideal_grant_kb"  , "<html><p>The total amount of ideal memory grant in KB this plan used since it was compiled. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_ideal_grant_kb"   , "<html><p>The amount of ideal memory grant in KB when this plan ran last time. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_ideal_grant_kb"    , "<html><p>The minimum amount of ideal memory grant in KB this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_ideal_grant_kb"    , "<html><p>The maximum amount of ideal memory grant in KB this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_reserved_threads", "<html><p>The total sum of reserved parallel threads this plan ever used since it was compiled.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_reserved_threads" , "<html><p>The number of reserved parallel threads when this plan ran last time. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_reserved_threads"  , "<html><p>The minimum number of reserved parallel threads this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_reserved_threads"  , "<html><p>The maximum number of reserved parallel threads this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "total_used_threads"    , "<html><p>The total sum of reserved parallel threads this plan ever used since it was compiled.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "last_used_threads"     , "<html><p>The number of used parallel threads when this plan ran last time. It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "min_used_threads"      , "<html><p>The minimum number of used parallel threads this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
			mtd.addColumn("dm_exec_query_stats", "max_used_threads"      , "<html><p>The maximum number of reserved parallel threads this plan ever used during one run.? It will always be 0 for querying a memory-optimized table.</p><p><strong>Applies to</strong>: SQL Server 2016 Community Technology Preview 2 (CTP2) through SQL Server 2016.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_query_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_requests",  "<p>Returns information about each request that is executing within SQL Server.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_requests", "session_id"                 , "<html><p>ID of the session to which this request is related. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "request_id"                 , "<html><p>ID of the request. Unique in the context of the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "start_time"                 , "<html><p>Timestamp when the request arrived. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "status"                     , "<html><p>Status of the request. This can be of the following:</p><ul class=\"unordered\"> <li><p>Background</p></li> <li><p>Running</p></li> <li><p>Runnable</p></li> <li><p>Sleeping</p></li> <li><p>Suspended</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "command"                    , "<html><p>Identifies the current type of command that is being processed. Common command types include the following: </p><ul class=\"unordered\"> <li><p>SELECT </p></li> <li><p>INSERT </p></li> <li><p>UPDATE </p></li> <li><p>DELETE </p></li> <li><p>BACKUP LOG </p></li> <li><p>BACKUP DATABASE</p></li> <li><p>DBCC </p></li> <li><p>FOR </p></li></ul><p>The text of the request can be retrieved by using <span class=\"literal\">sys.dm_exec_sql_text</span> with the corresponding <span class=\"literal\">sql_handle</span> for the request. Internal system processes set the command based on the type of task they perform. Tasks can include the following:</p><ul class=\"unordered\"> <li><p>LOCK MONITOR</p></li> <li><p>CHECKPOINTLAZY</p></li> <li><p>WRITER</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "sql_handle"                 , "<html><p>Hash map of the SQL text of the request. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "statement_start_offset"     , "<html><p>Number of characters into the currently executing batch or stored procedure at which the currently executing statement starts. Can be used together with the <span class=\"literal\">sql_handle</span>, the <span class=\"literal\">statement_end_offset</span>, and the <span class=\"literal\">sys.dm_exec_sql_text</span> dynamic management function to retrieve the currently executing statement for the request. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "statement_end_offset"       , "<html><p>Number of characters into the currently executing batch or stored procedure at which the currently executing statement ends. Can be used together with the <span class=\"literal\">sql_handle</span>, the <span class=\"literal\">statement_end_offset</span>, and the <span class=\"literal\">sys.dm_exec_sql_text</span> dynamic management function to retrieve the currently executing statement for the request. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "plan_handle"                , "<html><p>Hash map of the plan for SQL execution. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "database_id"                , "<html><p>ID of the database the request is executing against. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "user_id"                    , "<html><p>ID of the user who submitted the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "connection_id"              , "<html><p>ID of the connection on which the request arrived. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "blocking_session_id"        , "<html><p>ID of the session that is blocking the request. If this column is NULL, the request is not blocked, or the session information of the blocking session is not available (or cannot be identified).</p><p>-2 = The blocking resource is owned by an orphaned distributed transaction.</p><p>-3 = The blocking resource is owned by a deferred recovery transaction.</p><p>-4 = Session ID of the blocking latch owner could not be determined at this time because of internal latch state transitions.</p></html>");
			mtd.addColumn("dm_exec_requests", "wait_type"                  , "<html><p>If the request is currently blocked, this column returns the type of wait. Is nullable.</p><p>For information about types of waits, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179984.aspx\">sys.dm_os_wait_stats (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_exec_requests", "wait_time"                  , "<html><p>If the request is currently blocked, this column returns the duration in milliseconds, of the current wait. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "last_wait_type"             , "<html><p>If this request has previously been blocked, this column returns the type of the last wait. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "wait_resource"              , "<html><p>If the request is currently blocked, this column returns the resource for which the request is currently waiting. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "open_transaction_count"     , "<html><p>Number of transactions that are open for this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "open_resultset_count"       , "<html><p>Number of result sets that are open for this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "transaction_id"             , "<html><p>ID of the transaction in which this request executes. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "context_info"               , "<html><p>CONTEXT_INFO value of the session. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "percent_complete"           , "<html><p>Percentage of work completed for the following commands:</p><ul class=\"unordered\"> <li><p>ALTER INDEX REORGANIZE</p></li> <li><p>AUTO_SHRINK option with ALTER DATABASE</p></li> <li><p>BACKUP DATABASE</p></li> <li><p>DBCC CHECKDB</p></li> <li><p>DBCC CHECKFILEGROUP</p></li> <li><p>DBCC CHECKTABLE</p></li> <li><p>DBCC INDEXDEFRAG</p></li> <li><p>DBCC SHRINKDATABASE</p></li> <li><p>DBCC SHRINKFILE</p></li> <li><p>RECOVERY</p></li> <li><p>RESTORE DATABASE, </p></li> <li><p>ROLLBACK</p></li> <li><p>TDE ENCRYPTION</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "estimated_completion_time"  , "<html><p>Internal only. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "cpu_time"                   , "<html><p>CPU time in milliseconds that is used by the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "total_elapsed_time"         , "<html><p>Total time elapsed in milliseconds since the request arrived. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "scheduler_id"               , "<html><p>ID of the scheduler that is scheduling this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "task_address"               , "<html><p>Memory address allocated to the task that is associated with this request. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "reads"                      , "<html><p>Number of reads performed by this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "writes"                     , "<html><p>Number of writes performed by this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "logical_reads"              , "<html><p>Number of logical reads that have been performed by the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "text_size"                  , "<html><p>TEXTSIZE setting for this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "language"                   , "<html><p>Language setting for the request. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "date_format"                , "<html><p>DATEFORMAT setting for the request. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "date_first"                 , "<html><p>DATEFIRST setting for the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "quoted_identifier"          , "<html><p>1 = QUOTED_IDENTIFIER is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "arithabort"                 , "<html><p>1 = ARITHABORT setting is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "ansi_null_dflt_on"          , "<html><p>1 = ANSI_NULL_DFLT_ON setting is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "ansi_defaults"              , "<html><p>1 = ANSI_DEFAULTS setting is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "ansi_warnings"              , "<html><p>1 = ANSI_WARNINGS setting is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "ansi_padding"               , "<html><p>1 = ANSI_PADDING setting is ON for the request. </p><p>Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "ansi_nulls"                 , "<html><p>1 = ANSI_NULLS setting is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "concat_null_yields_null"    , "<html><p>1 = CONCAT_NULL_YIELDS_NULL setting is ON for the request. Otherwise, it is 0.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "transaction_isolation_level", "<html><p>Isolation level with which the transaction for this request is created. Is not nullable.</p><p>0 = Unspecified</p><p>1 = ReadUncomitted</p><p>2 = ReadCommitted</p><p>3 = Repeatable</p><p>4 = Serializable</p><p>5 = Snapshot</p></html>");
			mtd.addColumn("dm_exec_requests", "lock_timeout"               , "<html><p>Lock time-out period in milliseconds for this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "deadlock_priority"          , "<html><p>DEADLOCK_PRIORITY setting for the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "row_count"                  , "<html><p>Number of rows that have been returned to the client by this request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "prev_error"                 , "<html><p>Last error that occurred during the execution of the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "nest_level"                 , "<html><p>Current nesting level of code that is executing on the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "granted_query_memory"       , "<html><p>Number of pages allocated to the execution of a query on the request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "executing_managed_code"     , "<html><p>Indicates whether a specific request is currently executing common language runtime objects, such as routines, types, and triggers. It is set for the full time a common language runtime object is on the stack, even while running Transact-SQL from within common language runtime. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "group_id"                   , "<html><p>ID of the workload group to which this query belongs. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_requests", "query_hash"                 , "<html><p>Binary hash value calculated on the query and used to identify queries with similar logic. You can use the query hash to determine the aggregate resource usage for queries that differ only by literal values. </p></html>");
			mtd.addColumn("dm_exec_requests", "query_plan_hash"            , "<html><p>Binary hash value calculated on the query execution plan and used to identify similar query execution plans. You can use query plan hash to find the cumulative cost of queries with similar execution plans. </p></html>");
			mtd.addColumn("dm_exec_requests", "statement_sql_handle"       , "<html><p>SQL handle of the individual query.<br> This column is NULL if Query Store is not enabled for the database.</p></html>");
			mtd.addColumn("dm_exec_requests", "statement_context_id"       , "<html><p>The optional foreign key to sys.query_context_settings.<br> This column is NULL if Query Store is not enabled for the database.</p></html>");
			mtd.addColumn("dm_exec_requests", "dop"                        , "<html><p>The degree of parallelism of the query.</p></html>");
			mtd.addColumn("dm_exec_requests", "parallel_worker_count"      , "<html><p>The number of reserved parallel workers if this is a parallel query.</p></html>");
			mtd.addColumn("dm_exec_requests", "external_script_request_id" , "<html><p>The external script request ID associated with the current request.</p></html>");
			mtd.addColumn("dm_exec_requests", "is_resumable"               , "<html><p>Indicates whether the request is a resumable index operation.</p></html>");
			mtd.addColumn("dm_exec_requests", "page_resource"              , "<html><p>An 8-byte hexadecimal representation of the page resource if the wait_resource column contains a page. For more information, see sys.fn_PageResCracker.</p></html>");
			mtd.addColumn("dm_exec_requests", "page_server_reads"          , "<html><p>Number of page server reads performed by this request. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_sessions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_sessions",  "<p>Returns one row per authenticated session on SQL Server. <span class=\"literal\">sys.dm_exec_sessions</span>?is a server-scope view that shows information about all active user connections and internal tasks. This information includes client version, client program name, client login time, login user, current session setting, and more.?Use <span class=\"literal\">sys.dm_exec_sessions</span>?to first view the current system load and to identify a session of interest, and?then?learn more information about that session by using other dynamic management views or dynamic management functions.</p><p>The <span class=\"literal\">sys.dm_exec_connections</span>, <span class=\"literal\">sys.dm_exec_sessions</span>, and <span class=\"literal\">sys.dm_exec_requests</span> dynamic management views map to the <a href=\"https://msdn.microsoft.com/en-us/library/ms179881.aspx\">sys.sysprocesses</a> system table. </p>");

			// Column names and description
			mtd.addColumn("dm_exec_sessions", "session_id"                 , "<html><p>Identifies the session associated with each active primary connection. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "login_time"                 , "<html><p>Time when session was established. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "host_name"                  , "<html><p>Name of the client workstation that is specific to a session. The value is NULL for internal sessions. Is nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-security\" alt=\"System_CAPS_security\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC17938.jpeg\" title=\"System_CAPS_security\" xmlns=\"\"></span><span class=\"alertTitle\"> Security Note </span></th>    </tr>    <tr>     <td><p>The client application provides the workstation name and can provide inaccurate data. Do not rely upon HOST_NAME as a security feature.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "program_name"               , "<html><p>Name of client program that initiated the session. The value is NULL for internal sessions. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "host_process_id"            , "<html><p>Process ID of the client program that initiated the session. The value is NULL for internal sessions. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "client_version"             , "<html><p>TDS protocol version of the interface that is used by the client to connect to the server. The value is NULL for internal sessions. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "client_interface_name"      , "<html><p>Protocol name that is used by the client to connect to the server. The value is NULL for internal sessions. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "security_id"                , "<html><p>Microsoft Windows security ID associated with the login. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "login_name"                 , "<html><p>SQL Server login name under which the session is currently executing. For the original login name that created the session, see <span class=\"literal\">original_login_name</span>. Can be a SQL Server authenticated login?name or a Windows authenticated domain user?name. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "nt_domain"                  , "<html><p>Windows domain for the client if the session is using Windows Authentication or a trusted connection. This value is NULL for internal sessions and non-domain users. Is nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "nt_user_name"               , "<html><p>Windows user name for the client if the session is using Windows Authentication or a trusted connection. This value is NULL for internal sessions and non-domain users. Is nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "status"                     , "<html><p>Status of the session. Possible values:</p><ul class=\"unordered\"> <li><p><strong>Running</strong> - Currently running one or more requests</p></li> <li><p><strong>Sleeping</strong> - Currently running no requests</p></li> <li><p><strong>Dormant</strong> ? Session has been reset because of connection pooling and is now in prelogin state.</p></li> <li><p><strong>Preconnect</strong> - Session is in the Resource Governor classifier.</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "context_info"               , "<html><p>CONTEXT_INFO value for the session. The context information is set by the user by using the <a href=\"https://msdn.microsoft.com/en-us/library/ms187768.aspx\">SET CONTEXT_INFO</a> statement. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "cpu_time"                   , "<html><p>CPU time, in milliseconds, that was used by this session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "memory_usage"               , "<html><p>Number of 8-KB pages of memory used by this session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "total_scheduled_time"       , "<html><p>Total time, in milliseconds, for which the session (requests within) were scheduled for execution. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "total_elapsed_time"         , "<html><p>Time, in milliseconds, since the session was established. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "endpoint_id"                , "<html><p>ID of the Endpoint associated with the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "last_request_start_time"    , "<html><p>Time at which the last request on the session began. This includes the currently executing request. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "last_request_end_time"      , "<html><p>Time of the last completion of a request on the session. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "reads"                      , "<html><p>Number of reads performed, by requests in this session, during this session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "writes"                     , "<html><p>Number of writes performed, by requests in this session, during this session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "logical_reads"              , "<html><p>Number of logical reads that have been performed on the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "is_user_process"            , "<html><p>0 if the session is a system session. Otherwise, it is 1. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "text_size"                  , "<html><p>TEXTSIZE setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "language"                   , "<html><p>LANGUAGE setting for the session. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "date_format"                , "<html><p>DATEFORMAT setting for the session. Is nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "date_first"                 , "<html><p>DATEFIRST setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "quoted_identifier"          , "<html><p>QUOTED_IDENTIFIER setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "arithabort"                 , "<html><p>ARITHABORT setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "ansi_null_dflt_on"          , "<html><p>ANSI_NULL_DFLT_ON setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "ansi_defaults"              , "<html><p>ANSI_DEFAULTS setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "ansi_warnings"              , "<html><p>ANSI_WARNINGS setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "ansi_padding"               , "<html><p>ANSI_PADDING setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "ansi_nulls"                 , "<html><p>ANSI_NULLS setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "concat_null_yields_null"    , "<html><p>CONCAT_NULL_YIELDS_NULL setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "transaction_isolation_level", "<html><p>Transaction isolation level of the session.</p><p>0 = Unspecified</p><p>1 = ReadUncomitted</p><p>2 = ReadCommitted</p><p>3 = Repeatable</p><p>4 = Serializable</p><p>5 = Snapshot</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "lock_timeout"               , "<html><p>LOCK_TIMEOUT setting for the session. The value is in milliseconds. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "deadlock_priority"          , "<html><p>DEADLOCK_PRIORITY setting for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "row_count"                  , "<html><p>Number of rows returned on the session up to this point. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "prev_error"                 , "<html><p>ID of the last error returned on the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_exec_sessions", "original_security_id"       , "<html><p>Microsoft Windows security ID that is associated with the <span class=\"literal\">original_login_name</span>. Is not nullable. </p></html>");
			mtd.addColumn("dm_exec_sessions", "original_login_name"        , "<html><p>SQL Server login name that the client used to create this session. Can be a SQL Server authenticated login name, a Windows authenticated domain user name, or a contained database user. Note that the session could have gone through many implicit or explicit context switches after the initial connection. For example, if <a href=\"https://msdn.microsoft.com/en-us/library/ms181362.aspx\">EXECUTE AS</a> is used. Is not nullable.</p></html>");

			mtd.addColumn("dm_exec_sessions", "last_successful_logon"      , "<html><p>Time of the last successful logon for the <span class=\"literal\">original_login_name</span> before the current session started.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "last_unsuccessful_logon"    , "<html><p>Time of the last unsuccessful logon attempt for the <span class=\"literal\">original_login_name</span> before the current session started.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "unsuccessful_logons"        , "<html><p>Number of unsuccessful logon attempts for the <span class=\"literal\">original_login_name</span> between the <span class=\"literal\">last_successful_logon</span> and <span class=\"literal\">login_time</span>.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "group_id"                   , "<html><p>ID of the workload group to which this session belongs. Is not nullable. </p></html>");
			mtd.addColumn("dm_exec_sessions", "database_id"                , "<html><p>ID of the current database for each session.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "authenticating_database_id" , "<html><p>ID of the database authenticating the principal. For Logins, the value will be 0. For contained database users, the value will be the database ID of the contained database. </p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_exec_sessions", "open_transaction_count"     , "<html><p>Number of open transactions per session.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_sessions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_session_wait_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_session_wait_stats",  "<p>Returns information about all the waits encountered by threads that executed for each session. You can use this view to diagnose performance issues with the SQL Server session and also with specific queries and batches. This view returns session the same information that is aggregated for <a href=\"https://msdn.microsoft.com/en-us/library/ms179984.aspx\">sys.dm_os_wait_stats (Transact-SQL)</a> but provides the <strong>session_id</strong> number as well.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_session_wait_stats", "session_id"         , "<html><p>The id of the session.</p></html>");
			mtd.addColumn("dm_exec_session_wait_stats", "wait_type"          , "<html><p>Name of the wait type. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179984.aspx\">sys.dm_os_wait_stats (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_exec_session_wait_stats", "waiting_tasks_count", "<html><p>Number of waits on this wait type. This counter is incremented at the start of each wait. </p></html>");
			mtd.addColumn("dm_exec_session_wait_stats", "wait_time_ms"       , "<html><p>Total wait time for this wait type in milliseconds. This time is inclusive of <span class=\"literal\">signal_wait_time_ms</span>. </p></html>");
			mtd.addColumn("dm_exec_session_wait_stats", "max_wait_time_ms"   , "<html><p>Maximum wait time on this wait type.</p></html>");
			mtd.addColumn("dm_exec_session_wait_stats", "signal_wait_time_ms", "<html><p>Difference between the time that the waiting thread was signaled and when it started running. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_session_wait_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_sql_text
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_sql_text",  "<p>Returns the text of the SQL batch that is identified by the specified <em>sql_handle</em>. This table-valued function replaces the system function <strong>fn_get_sql</strong>.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_sql_text", "dbid"     , "<html><p>ID of database.</p><p>For ad hoc and prepared SQL statements, the ID of the database where the statements were compiled.</p></html>");
			mtd.addColumn("dm_exec_sql_text", "objectid" , "<html><p>ID of object.</p><p>Is NULL for ad hoc and prepared SQL statements.</p></html>");
			mtd.addColumn("dm_exec_sql_text", "number"   , "<html><p>For a numbered stored procedure, this column returns the number of the stored procedure. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179865.aspx\">sys.numbered_procedures (Transact-SQL)</a>.</p><p>Is NULL for ad hoc and prepared SQL statements.</p></html>");
			mtd.addColumn("dm_exec_sql_text", "encrypted", "<html><p>1 = SQL text is encrypted.</p><p>0 = SQL text is not encrypted.</p></html>");
			mtd.addColumn("dm_exec_sql_text", "text"     , "<html><p>Text of the SQL query.</p><p>Is NULL for encrypted objects.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_sql_text' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_text_query_plan
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_text_query_plan",  "<p>Returns the Showplan in text format for a Transact-SQL batch or for a specific statement within the batch. The query plan specified by the plan handle?can either be cached or currently executing. This table-valued function is similar to <a href=\"https://msdn.microsoft.com/en-us/library/ms189747.aspx\">sys.dm_exec_query_plan (Transact-SQL)</a>, but has the following differences:</p><ul class=\"unordered\"> <li><p>The output of the query plan is returned in text format.</p></li> <li><p>The output of the query plan is not limited in size.</p></li> <li><p>Individual statements within the batch can be specified.</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_exec_text_query_plan", "dbid"      , "<html><p>ID of the context database that was in effect when the Transact-SQL statement corresponding to this plan was compiled. For ad hoc and prepared SQL statements, the ID of the database where the statements were compiled.</p><p>Column is nullable.</p></html>");
			mtd.addColumn("dm_exec_text_query_plan", "objectid"  , "<html><p>ID of the object (for example, stored procedure or user-defined function) for this query plan. For ad hoc and prepared batches, this column is <strong>null</strong>.</p><p>Column is nullable.</p></html>");
			mtd.addColumn("dm_exec_text_query_plan", "number"    , "<html><p>Numbered stored procedure integer. For example, a group of procedures for the <strong>orders</strong> application may be named <strong>orderproc;1</strong>, <strong>orderproc;2</strong>, and so on. For ad hoc and prepared batches, this column is <strong>null</strong>.</p><p>Column is nullable.</p></html>");
			mtd.addColumn("dm_exec_text_query_plan", "encrypted" , "<html><p>Indicates whether the corresponding stored procedure is encrypted.</p><p>0 = not encrypted</p><p>1 = encrypted</p><p>Column is not nullable.</p></html>");
			mtd.addColumn("dm_exec_text_query_plan", "query_plan", "<html><p>Contains the compile-time Showplan representation of the query execution plan that is specified with <em>plan_handle</em>. The Showplan is in text format. One plan is generated for each batch that contains, for example ad hoc Transact-SQL statements, stored procedure calls, and user-defined function calls.</p><p>Column is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_text_query_plan' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_trigger_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_trigger_stats",  "<p>Returns aggregate performance statistics for cached triggers. The view contains one row per trigger, and the lifetime of the row is as long as the trigger remains cached. When a trigger is removed from the cache, the corresponding row is eliminated from this view. At that time, a Performance Statistics SQL trace event is raised similar to <strong>sys.dm_exec_query_stats</strong>.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_trigger_stats", "database_id"         , "<html><p>Database ID in which the trigger resides.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "object_id"           , "<html><p>Object identification number of the trigger.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "type"                , "<html><p>Type of the object:</p><p> TA = Assembly (CLR) trigger</p><p> TR = SQL trigger</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "Type_desc"           , "<html><p>Description of the object type:</p><p> CLR_TRIGGER</p><p> SQL_TRIGGER</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "sql_handle"          , "<html><p>This can be used to correlate with queries in <strong>sys.dm_exec_query_stats</strong> that were executed from within this trigger.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "plan_handle"         , "<html><p>Identifier for the in-memory plan. This identifier is transient and remains constant only while the plan remains in the cache. This value may be used with the <strong>sys.dm_exec_cached_plans</strong> dynamic management view.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "cached_time"         , "<html><p>Time at which the trigger was added to the cache.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "last_execution_time" , "<html><p>Last time at which the trigger was executed.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "execution_count"     , "<html><p>Number of times that the trigger has been executed since it was last compiled.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "total_worker_time"   , "<html><p>Total amount of CPU time, in microseconds, that was consumed by executions of this trigger since it was compiled.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "last_worker_time"    , "<html><p>CPU time, in microseconds, that was consumed the last time the trigger was executed.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "min_worker_time"     , "<html><p>Maximum CPU time, in microseconds, that this trigger has ever consumed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "max_worker_time"     , "<html><p>Maximum CPU time, in microseconds, that this trigger has ever consumed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "total_physical_reads", "<html><p>Total number of physical reads performed by executions of this trigger since it was compiled.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "last_physical_reads" , "<html><p>Number of physical reads performed the last time the trigger was executed.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "min_physical_reads"  , "<html><p>Minimum number of physical reads that this trigger has ever performed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "max_physical_reads"  , "<html><p>Maximum number of physical reads that this trigger has ever performed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "total_logical_writes", "<html><p>Total number of logical writes performed by executions of this trigger since it was compiled.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "last_logical_writes" , "<html><p><strong>total_physical_reads</strong>Number of logical writes performed the last time the trigger was executed.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "min_logical_writes"  , "<html><p>Minimum number of logical writes that this trigger has ever performed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "max_logical_writes"  , "<html><p>Maximum number of logical writes that this trigger has ever performed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "total_logical_reads" , "<html><p>Total number of logical reads performed by executions of this trigger since it was compiled.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "last_logical_reads"  , "<html><p>Number of logical reads performed the last time the trigger was executed.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "min_logical_reads"   , "<html><p>Minimum number of logical reads that this trigger has ever performed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "max_logical_reads"   , "<html><p>Maximum number of logical reads that this trigger has ever performed during a single execution.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "total_elapsed_time"  , "<html><p>Total elapsed time, in microseconds, for completed executions of this trigger.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "last_elapsed_time"   , "<html><p>Elapsed time, in microseconds, for the most recently completed execution of this trigger.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "min_elapsed_time"    , "<html><p>Minimum elapsed time, in microseconds, for any completed execution of this trigger.</p></html>");
			mtd.addColumn("dm_exec_trigger_stats", "max_elapsed_time"    , "<html><p>Maximum elapsed time, in microseconds, for any completed execution of this trigger.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_trigger_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_exec_xml_handles
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_exec_xml_handles",  "<p>Returns information about active handles that have been opened by <strong>sp_xml_preparedocument</strong>.</p>");

			// Column names and description
			mtd.addColumn("dm_exec_xml_handles", "session_id"                            , "<html><p>Session ID of the session that holds this XML document handle.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "document_id"                           , "<html><p>XML document handle ID returned by <strong>sp_xml_preparedocument</strong>.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "namespace_document_id"                 , "<html><p>Internal handle ID used for the associated namespace document that has been passed as the third parameter to <strong>sp_xml_preparedocument</strong>. NULL if there is no namespace document.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "sql_handle"                            , "<html><p>Handle to the text of the SQL code where the handle has been defined.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "statement_start_offset"                , "<html><p>Number of characters into the currently executing batch or stored procedure at which the <strong>sp_xml_preparedocument</strong> call occurs. Can be used together with the <strong>sql_handle</strong>, the <strong>statement_end_offset</strong>, and the <strong>sys.dm_exec_sql_text</strong> dynamic management function to retrieve the currently executing statement for the request.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "statement_end_offset"                  , "<html><p>Number of characters into the currently executing batch or stored procedure at which the <strong>sp_xml_preparedocument</strong> call occurs. Can be used together with the <strong>sql_handle</strong>, the <strong>statement_start_offset</strong>, and the <strong>sys.dm_exec_sql_text</strong> dynamic management function to retrieve the currently executing statement for the request.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "creation_time"                         , "<html><p>Timestamp when <strong>sp_xml_preparedocument</strong> was called.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "original_document_size_bytes"          , "<html><p>Size of the unparsed XML document in bytes.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "original_namespace_document_size_bytes", "<html><p>Size of the unparsed XML namespace document, in bytes. NULL if there is no namespace document.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "num_openxml_calls"                     , "<html><p>Number of OPENXML calls with this document handle.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "row_count"                             , "<html><p>Number of rows returned by all previous OPENXML calls for this document handle.</p></html>");
			mtd.addColumn("dm_exec_xml_handles", "dormant_duration_ms"                   , "<html><p>Milliseconds since the last OPENXML call. If OPENXML has not been called, returns milliseconds since the <strong>sp_xml_preparedocumen</strong>t call. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_exec_xml_handles' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_object_columns
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_object_columns",  "<p>Returns the schema information for all the objects. </p>");

			// Column names and description
			mtd.addColumn("dm_xe_object_columns", "name"               , "<html><p>The name of the column. <span class=\"literal\">name</span> is unique within the object. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "column_id"          , "<html><p>The identifier of the column. <span class=\"literal\">column_id</span> is unique within the object when used with column_type. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "object_name"        , "<html><p>The name of the object to which this column belongs. There is a many-to-one relationship with <span class=\"literal\">sys.dm_xe_objects.id</span>. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "object_package_guid", "<html><p>The GUID of the package that contains the object. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "type_name"          , "<html><p>The name of the type for this column. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "type_package_guid"  , "<html><p>The GUID of the package that contains the column data type. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "column_type"        , "<html><p>Indicates how this column is used. <span class=\"literal\">column_type</span> can be one of the following:</p><ul class=\"unordered\"> <li><p>readonly. The column contains a static value that cannot be changed.</p></li> <li><p>data. The column contains run-time data exposed by the object.</p></li> <li><p>customizable. The column contains a value that can be changed.</p></li></ul><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Changing this value can modify the behavior of the object.</p></td>    </tr>   </tbody>  </table> </div></div><p>Is not nullable. </p></html>");
			mtd.addColumn("dm_xe_object_columns", "column_value"       , "<html><p>Displays static values associated with the object column. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "capabilities"       , "<html><p>A bitmap describing the capabilities of the column. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_object_columns", "capabilities_desc"  , "<html><p>A description of this object column's capabilities. This value can be one of the following:</p><ul class=\"unordered\"> <li><p>Mandatory. The value must be set when binding the parent object to an event session.</p></li> <li><p>NULL</p></li></ul></html>");
			mtd.addColumn("dm_xe_object_columns", "description"        , "<html><p>The description of this object column. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_object_columns' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_objects
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_objects",  "<p>Returns a row for each object that is exposed by an event package. Objects can be one of the following:</p><ul class=\"unordered\"> <li><p>Events. Events indicate points of interest in an execution path. All events contain information about a point of interest.</p></li> <li><p>Actions. Actions are run synchronously when events fire. An action can append run time data to an event.</p></li> <li><p>Targets. Targets consume events, either synchronously on the thread that fires the event or asynchronously on a system-provided thread.</p></li> <li><p>Predicates. Predicate sources retrieve values from event sources for use in comparison operations. Predicate comparisons compare specific data types and return a Boolean value.</p></li> <li><p>Types. Types encapsulate the length and characteristics of the byte collection, which is required in order to interpret the data.</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_xe_objects", "name"             , "<html><p>The name of the object. <span class=\"literal\">name</span> is unique within a package for a specific object type. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_objects", "object_type"      , "<html><p>The type of the object. <span class=\"literal\">object_type</span> is one of the following:</p><p><span class=\"literal\">event</span></p><p><span class=\"literal\">action</span></p><p><span class=\"literal\">target</span></p><p><span class=\"literal\">pred_source</span></p><p><span class=\"literal\">pred_compare</span></p><p><span class=\"literal\">type</span></p><p>Is not nullable. </p></html>");
			mtd.addColumn("dm_xe_objects", "package_guid"     , "<html><p>The GUID for the package that exposes this action. There is a many-to-one relationship with <span class=\"literal\">sys.dm_xe_packages.package_id</span>. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_objects", "description"      , "<html><p>A description of the action. <span class=\"literal\">description</span> is set by the package author. Is not nullable. </p></html>");
			mtd.addColumn("dm_xe_objects", "capabilities"     , "<html><p>A bitmap that describes the capabilities of the object. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_objects", "capabilities_desc", "<html><p>Lists all the capabilities of the object. Is nullable.</p><p><span class=\"label\">Capabilities that apply to all object types</span></p><p> ? <span class=\"label\">Private</span>. The only object available for internal use, and that cannot be accessed via the CREATE/ALTER EVENT SESSION DDL. Audit events and targets fall into this category in addition to a small number of objects used internally.</p><p>===============</p><p><span class=\"label\">Event Capabilities</span></p><p> ? <span class=\"label\">No_block</span>. The event is in a critical code path that cannot block for any reason. Events with this capability may not be added to any event session that specifies NO_EVENT_LOSS.</p><p>===============</p><p><span class=\"label\">Capabilities that apply to all object types</span></p><p> ? <span class=\"label\">Process_whole_buffers</span>. The target consumes buffers of events at a time, rather than event by event.</p><p>? <span class=\"label\">Singleton</span>. Only one instance of the target can exist in a process. Although multiple event sessions can reference the same singleton target there is really only one instance, and that instance will see each unique event only once. This is important if the target is added to multiple sessions that all collect the same event.</p><p>? <span class=\"label\">Synchronous</span>. The target is executed on the thread that is producing the event, before control is returned to the calling code line.</p></html>");
			mtd.addColumn("dm_xe_objects", "type_name"        , "<html><p>The name for <span class=\"literal\">pred_source</span> and <span class=\"literal\">pred_compare</span> objects. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_objects", "type_package_guid", "<html><p>The GUID for the package that exposes the type that this object operates on. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_objects", "type_size"        , "<html><p>The size, in bytes, of the data type. This is only for valid object types. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_objects' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_packages
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_packages",  "<p>Lists all the packages registered with the extended events engine.</p>");

			// Column names and description
			mtd.addColumn("dm_xe_packages", "name"             , "<html><p>The name of package. The description is exposed from the package itself. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_packages", "guid"             , "<html><p>The GUID that identifies the package. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_packages", "description"      , "<html><p>The package description. <span class=\"literal\">description</span><em>?</em>is set by the package author and is not nullable.</p></html>");
			mtd.addColumn("dm_xe_packages", "capabilities"     , "<html><p>Bitmap describing the capabilities of this package. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_packages", "capabilities_desc", "<html><p>A list of all the capabilities possible for this package. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_packages", "module_guid"      , "<html><p>The GUID of the module that exposes this package. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_packages", "module_address"   , "<html><p>The base address where the module containing the package is loaded. A single module may expose several packages. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_packages' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_session_event_actions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_session_event_actions",  "<p>Returns information about event session actions. Actions are executed when events are fired. This management view aggregates statistics about the number of times an action has run, and the total run time of the action.</p>");

			// Column names and description
			mtd.addColumn("dm_xe_session_event_actions", "event_session_address", "<html><p>The memory address of the event session. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_event_actions", "action_name"          , "<html><p>The name of the action. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_event_actions", "action_package_guid"  , "<html><p>The GUID for the package that contains the action. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_event_actions", "event_name"           , "<html><p>The name of the event that the action is bound to. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_event_actions", "event_package_guid"   , "<html><p>The GUID for the package that contains the event. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_session_event_actions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_session_events
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_session_events",  "<p>Returns information about session events. Events are discrete execution points. Predicates can be applied to events to stop them from firing if the event does not contain the required information.</p>");

			// Column names and description
			mtd.addColumn("dm_xe_session_events", "event_session_address", "<html><p>The memory address of the event session. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_events", "event_name"           , "<html><p>The name of the event that an action is bound to. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_events", "event_package_guid"   , "<html><p>The GUID for the package containing the event. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_events", "event_predicate"      , "<html><p>An XML representation of the predicate tree that is applied to the event. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_session_events' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_session_object_columns
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_session_object_columns",  "<p>Shows the configuration values for objects that are bound to a session.</p>");

			// Column names and description
			mtd.addColumn("dm_xe_session_object_columns", "event_session_address", "<html><p>The memory address of the event session. Has a many-to-one relationship with <span class=\"literal\">sys.dm_xe_sessions.address</span>. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_object_columns", "column_name"          , "<html><p>The name of the configuration value. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_object_columns", "column_id"            , "<html><p>The ID of the column. Is unique within the object. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_object_columns", "column_value"         , "<html><p>The configured value of the column. Is nullable.</p></html>");
			mtd.addColumn("dm_xe_session_object_columns", "object_type"          , "<html><p>The type of the object. <span class=\"literal\">object_type</span> is one of:</p><ul class=\"unordered\"> <li><p><span class=\"literal\">event</span></p></li> <li><p><span class=\"literal\">target</span></p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_object_columns", "object_name"          , "<html><p>The name of the object to which this column belongs. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_object_columns", "object_package_guid"  , "<html><p>The GUID of the package that contains the object. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_session_object_columns' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_session_targets
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_session_targets",  "<p>Returns information about session targets.</p>");

			// Column names and description
			mtd.addColumn("dm_xe_session_targets", "event_session_address", "<html><p>The memory address of the event session. Has a many-to-one relationship with <span class=\"literal\">sys.dm_xe_sessions.address</span>. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_targets", "target_name"          , "<html><p>The name of the target within a session. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_targets", "target_package_guid"  , "<html><p>The GUID of the package that contains the target. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_targets", "execution_count"      , "<html><p>The number of times the target has been executed for the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_targets", "execution_duration_ms", "<html><p>The total amount of time, in milliseconds, that the target has been executing. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_session_targets", "target_data"          , "<html><p>The data that the target maintains, such as event aggregation information. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_session_targets' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xe_sessions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xe_sessions",  "<p>Returns information about an active extended events session. This session is a collection of events, actions, and targets.</p>");

			// Column names and description
			mtd.addColumn("dm_xe_sessions", "address"                   , "<html><p>The memory address of the session. <span class=\"literal\">address</span> is unique across the local system. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "name"                      , "<html><p>The name of the session. <span class=\"literal\">name</span> is unique across the local system. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "pending_buffers"           , "<html><p>The number of full buffers that are pending processing. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "total_regular_buffers"     , "<html><p>The total number of regular buffers that are associated with the session. Is not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Regular buffers are used most of the time.?These buffers are of sufficient size to hold many events. Typically, there will be three or more buffers per session. The number of regular buffers is automatically determined by the server, based on the memory partitioning that is set through the MEMORY_PARTITION_MODE option.?The size of the regular buffers is equal to the value of the MAX_MEMORY option (default of 4 MB), divided by the number of buffers. For more information about the MEMORY_PARTITION_MODE and the MAX_MEMORY options, see <a href=\"https://msdn.microsoft.com/en-us/library/bb677289.aspx\">CREATE EVENT SESSION (Transact-SQL)</a>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_xe_sessions", "regular_buffer_size"       , "<html><p>The regular buffer size, in bytes. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "total_large_buffers"       , "<html><p>The total number of large buffers. Is not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Large buffers are used when an event is larger than a regular buffer.?They are set aside explicitly for this purpose.?Large buffers are allocated when the event session starts, and are sized according to the MAX_EVENT_SIZE option. For more information about the MAX_EVENT_SIZE option, see <a href=\"https://msdn.microsoft.com/en-us/library/bb677289.aspx\">CREATE EVENT SESSION (Transact-SQL)</a>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_xe_sessions", "large_buffer_size"         , "<html><p>The large buffer size, in bytes. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "total_buffer_size"         , "<html><p>The total size of the memory buffer that is used to store events for the session, in bytes. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "buffer_policy_flags"       , "<html><p>A bitmap that indicates how session event buffers behave when all the buffers are full and a new event is fired. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "buffer_policy_desc"        , "<html><p>A description that indicates how session event buffers behave when all the buffers are full and a new event is fired. <span class=\"literal\">buffer_policy_desc</span> can be one of the following:</p><ul class=\"unordered\"> <li><p>Drop event</p></li> <li><p>Do not drop events</p></li> <li><p>Drop full buffer</p></li> <li><p>Allocate new buffer</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "flags"                     , "<html><p>A bitmap that indicates the flags that have been set on the session. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "flag_desc"                 , "<html><p>A description of the flags set on the session. <span class=\"literal\">flag_desc</span> can be any combination of the following:</p><ul class=\"unordered\"> <li><p>Flush buffers on close</p></li> <li><p>Dedicated dispatcher</p></li> <li><p>Allow recursive events</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "dropped_event_count"       , "<html><p>The number of events that were dropped when the buffers were full. This value is <strong>0</strong> if the buffer policy is \"Drop full buffer\" or \"Do not drop events\". Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "dropped_buffer_count"      , "<html><p>The number of buffers that were dropped when the buffers were full. This value is <strong>0</strong> if the buffer policy is set to \"Drop event\" or \"Do not drop events\". Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "blocked_event_fire_time"   , "<html><p>The length of time that event firings were blocked when buffers were full. This value is <strong>0</strong> if the buffer policy is \"Drop full buffer\" or \"Drop event\". Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "create_time"               , "<html><p>The time that the session was created. Is not nullable.</p></html>");
			mtd.addColumn("dm_xe_sessions", "largest_event_dropped_size", "<html><p>The size of the largest event that did not fit into the session buffer. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xe_sessions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_filestream_file_io_handles
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_filestream_file_io_handles",  "<p>Displays the file handles that the Namespace Owner (NSO) knows about. Filestream handles that a client got using <strong>OpenSqlFilestream</strong> are displayed by this view. </p>");

			// Column names and description
			mtd.addColumn("dm_filestream_file_io_handles", "handle_context_address"    , "<html><p>Shows the address of the internal NSO structure associated with the client?s handle. Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "creation_request_id"       , "<html><p>Shows a field from the REQ_PRE_CREATE I/O request used to create this handle. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "creation_irp_id"           , "<html><p>Shows a field from the REQ_PRE_CREATE I/O request used to create this handle. Is not nullable</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "handle_id"                 , "<html><p>Shows the unique ID of this handle that is assigned by the driver. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "creation_client_thread_id" , "<html><p>Shows a field from the REQ_PRE_CREATE I/O request used to create this handle. Is nullable. </p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "creation_client_process_id", "<html><p>Shows a field from the REQ_PRE_CREATE I/O request used to create this handle. Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "filestream_transaction_id" , "<html><p>Shows the ID of the transaction associated with the given handle. This is the value returned by the <strong>get_filestream_transaction_context</strong> function. Use this field to join to the <strong>sys.dm_filestream_file_io_requests</strong> view. Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "access_type"               , "<html><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "logical_path"              , "<html><p>Shows the logical pathname of the file that this handle opened. This is the same pathname that is returned by the <strong>.PathName</strong> method of <strong>varbinary</strong>(<strong>max</strong>) filestream. Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_handles", "physical_path"             , "<html><p>Shows the actual NTFS pathname of the file. This is the same pathname returned by the <strong>.PhysicalPathName</strong> method of the <strong>varbinary</strong>(<strong>max</strong>) filestream. It is enabled by trace flag 5556. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_filestream_file_io_handles' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_filestream_file_io_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_filestream_file_io_requests",  "<p>Displays a list of I/O requests being processed by the Namespace Owner (NSO) at the given moment. </p>");

			// Column names and description
			mtd.addColumn("dm_filestream_file_io_requests", "request_context_address"  , "<html><p>Shows the internal address of the NSO memory block that contains the I/O request from the driver. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "current_spid"             , "<html><p>Shows the system process id (SPID) for the current SQL Server?s connection. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "request_type"             , "<html><p>Shows the I/O request packet (IRP) type. The possible request types are REQ_PRE_CREATE, REQ_POST_CREATE, REQ_RESOLVE_VOLUME, REQ_GET_VOLUME_INFO, REQ_GET_LOGICAL_NAME, REQ_GET_PHYSICAL_NAME, REQ_PRE_CLEANUP, REQ_POST_CLEANUP, REQ_CLOSE, REQ_FSCTL, REQ_QUERY_INFO, REQ_SET_INFO, REQ_ENUM_DIRECTORY, REQ_QUERY_SECURITY, and REQ_SET_SECURITY. Is not nullable</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "request_state"            , "<html><p>Shows the state of the I/O request in NSO. Possible values are REQ_STATE_RECEIVED, REQ_STATE_INITIALIZED, REQ_STATE_ENQUEUED, REQ_STATE_PROCESSING, REQ_STATE_FORMATTING_RESPONSE, REQ_STATE_SENDING_RESPONSE, REQ_STATE_COMPLETING, and REQ_STATE_COMPLETED. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "request_id"               , "<html><p>Shows the unique request ID assigned by the driver to this request. Is not nullable. </p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "irp_id"                   , "<html><p>Shows the unique IRP ID. This is useful for identifying all I/O requests related to the given IRP. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "handle_id"                , "<html><p>Indicated the namespace handle ID. This is the NSO specific identifier and is unique across an instance. Is not nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "client_thread_id"         , "<html><p>Shows the client application?s thread ID that originates the request. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-warning\" alt=\"System_CAPS_warning\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC174466.jpeg\" title=\"System_CAPS_warning\" xmlns=\"\"></span><span class=\"alertTitle\">Warning </span></th>    </tr>    <tr>     <td><p>This is meaningful only if the client application is running on the same machine as SQL Server. When the client application is running remotely, the <strong>client_thread_id</strong> shows the thread ID of some system process that works on behalf of the remote client.</p></td>    </tr>   </tbody>  </table> </div></div><p>Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "client_process_id"        , "<html><p>Shows the process ID of the client application if the client application runs on the same machine as SQL Server. For a remote client, this shows the system process ID that is working on behalf of the client application. Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "handle_context_address"   , "<html><p>Shows the address of the internal NSO structure associated with the client?s handle. Is nullable.</p></html>");
			mtd.addColumn("dm_filestream_file_io_requests", "filestream_transaction_id", "<html><p>Shows the ID of the transaction associated with the given handle and all the requests associated with this handle. It is the value returned by the <strong>get_filestream_transaction_context</strong> function. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_filestream_file_io_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_filestream_non_transacted_handles
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_filestream_non_transacted_handles",  "<p>Displays the currently open non-transactional file handles associated with FileTable data. </p><p>This view contains one row per open file handle. Because the data in this view corresponds to the live internal state of the server, the data is constantly changing as handles are opened and closed. This view does not contain historical information. </p>");

			// Column names and description
			mtd.addColumn("dm_filestream_non_transacted_handles", "database_id"               , "<html><p>ID of the database associated with the handle. </p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "object_id"                 , "<html><p>Object ID of the FileTable the handle is associated with.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "handle_id"                 , "<html><p>Unique handle context identifier. Used by the <a href=\"https://msdn.microsoft.com/en-us/library/ff929106.aspx\">sp_kill_filestream_non_transacted_handles (Transact-SQL)</a> stored procedure to kill a specific handle.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "file_object_type"          , "<html><p>Type of the handle. This indicates the level of the hierarchy the handle was opened against, ie. database or item.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "file_object_type_desc"     , "<html><p>?UNDEFINED\", ?SERVER_ROOT\", ?DATABASE_ROOT\", ?TABLE_ROOT\", ?TABLE_ITEM\"</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "correlation_process_id"    , "<html><p>Contains a unique identifier for the process that originated the request.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "correlation_thread_id"     , "<html><p>Contains a unique identifier for the thread that originated the request.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "file_context"              , "<html><p>Pointer to the file object used by this handle.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "state"                     , "<html><p>Current state of the handle. May be active, closed or killed.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "state_desc"                , "<html><p>?ACTIVE\", ?CLOSED\", ?KILLED\"</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "current_workitem_type"     , "<html><p>State this handle is currently being processed by.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "current_workitem_type_desc", "<html><p>?NoSetWorkItemType\", ?FFtPreCreateWorkitem\", ?FFtGetPhysicalFileNameWorkitem\", ?FFtPostCreateWorkitem\", ?FFtPreCleanupWorkitem\", ?FFtPostCleanupWorkitem\", ?FFtPreCloseWorkitem\", ?FFtQueryDirectoryWorkItem\", ?FFtQueryInfoWorkItem\", ?FFtQueryVolumeInfoWorkItem\", ?FFtSetInfoWorkitem\", ?FFtWriteCompletionWorkitem\"</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "fcb_id"                    , "<html><p>FileTable File Control Block ID.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "item_id"                   , "<html><p>The Item ID for a file or directory. May be null for server root handles.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "is_directory"              , "<html><p>Is this a directory.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "item_name"                 , "<html><p>Name of the item.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "opened_file_name"          , "<html><p>Originally requested path to be opened.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "database_directory_name"   , "<html><p>Portion of the opened_file_name that represents the database directory name.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "table_directory_name"      , "<html><p>Portion of the opened_file_name that represents the table directory name.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "remaining_file_name"       , "<html><p>Portion of the opened_file_name that represents the remaining directory name.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "open_time"                 , "<html><p>Time the handle was opened.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "flags"                     , "<html><p>ShareFlagsUpdatedToFcb = 0x1, DeleteOnClose = 0x2, NewFile = 0x4, PostCreateDoneForNewFile = 0x8, StreamFileOverwritten = 0x10, RequestCancelled = 0x20, NewFileCreationRolledBack = 0x40</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "login_id"                  , "<html><p>ID of the principal that opened the handle.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "login_name"                , "<html><p>Name of the principal that opened the handle.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "login_sid"                 , "<html><p>SID of the principal that opened the handle.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "read_access"               , "<html><p>Opened for read access.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "write_access"              , "<html><p>Opened for write access.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "delete_access"             , "<html><p>Opened for delete access.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "share_read"                , "<html><p>Opened with share_read allowed.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "share_write"               , "<html><p>Opened with share_write allowed.</p></html>");
			mtd.addColumn("dm_filestream_non_transacted_handles", "share_delete"              , "<html><p>Opened with share_delete allowed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_filestream_non_transacted_handles' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_active_catalogs
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_active_catalogs",  "<p>Returns information on the full-text catalogs that have some population activity in progress on the server. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_active_catalogs", "database_id"                      , "<html><p>ID of the database that contains the active full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "catalog_id"                       , "<html><p>ID of the active full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "memory_address"                   , "<html><p>Address of memory buffers allocated for the population activity related to this full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "name"                             , "<html><p>Name of the active full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "is_paused"                        , "<html><p>Indicates whether the population of the active full-text catalog has been paused.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "status"                           , "<html><p>Current state of the full-text catalog. One of the following:</p><p>0 = Initializing</p><p>1 = Ready</p><p>2 = Paused</p><p>3 = Temporary error</p><p>4 = Remount needed</p><p>5 = Shutdown</p><p>6 = Quiesced for backup</p><p>7 = Backup is done through catalog</p><p>8 = Catalog is corrupt </p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "status_description"               , "<html><p>Description of current state of the active full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "previous_status"                  , "<html><p>Previous state of the full-text catalog. One of the following:</p><p>0 = Initializing</p><p>1 = Ready</p><p>2 = Paused</p><p>3 = Temporary error</p><p>4 = Remount needed</p><p>5 = Shutdown</p><p>6 = Quiesced for backup</p><p>7 = Backup is done through catalog</p><p>8 = Catalog is corrupt </p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "previous_status_description"      , "<html><p>Description of previous state of the active full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "worker_count"                     , "<html><p>Number of threads currently working on this full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "active_fts_index_count"           , "<html><p>Number of full-text indexes that are being populated.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "auto_population_count"            , "<html><p>Number of tables with an auto population in progress for this full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "manual_population_count"          , "<html><p>Number of tables with manual population in progress for this full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "full_incremental_population_count", "<html><p>Number of tables with a full or incremental population in progress for this full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "row_count_in_thousands"           , "<html><p>Estimated number of rows (in thousands) in all full-text indexes in this full-text catalog.</p></html>");
			mtd.addColumn("dm_fts_active_catalogs", "is_importing"                     , "<html><p>Indicates whether the full-text catalog is being imported:</p><p>1 = The catalog is being imported.</p><p>2 = The catalog is not being imported.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_active_catalogs' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_fdhosts
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_fdhosts",  "<p>Returns information on the current activity of the filter daemon host or hosts on the server instance. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_fdhosts", "fdhost_id"        , "<html><p>ID of the filter daemon host.</p></html>");
			mtd.addColumn("dm_fts_fdhosts", "fdhost_name"      , "<html><p>Name of filter daemon host.</p></html>");
			mtd.addColumn("dm_fts_fdhosts", "fdhost_process_id", "<html><p>Windows process ID of the filter daemon host.</p></html>");
			mtd.addColumn("dm_fts_fdhosts", "fdhost_type"      , "<html><p>Type of document being processed by the filter daemon host, one of: </p><ul class=\"unordered\"> <li><p>Single thread</p></li> <li><p>Multi-thread</p></li> <li><p>Huge document </p></li></ul></html>");
			mtd.addColumn("dm_fts_fdhosts", "max_thread"       , "<html><p>Maximum number of threads in the filter daemon host.</p></html>");
			mtd.addColumn("dm_fts_fdhosts", "batch_count"      , "<html><p>Number of batches that are being processed in the filter daemon host.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_fdhosts' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_index_keywords
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_index_keywords",  "<p>Returns information about the content of a full-text index for the specified table.?</p>");

			// Column names and description
			mtd.addColumn("dm_fts_index_keywords", "keyword"       , "<html><p>The hexadecimal representation of the keyword stored inside the full-text index.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>OxFF represents the special character that indicates the end of a file or dataset.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_keywords", "display_term"  , "<html><p>The human-readable format of the keyword. This format is derived from the hexadecimal format. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The <strong>display_term</strong> value for OxFF is \"END OF FILE.\"</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_keywords", "column_id"     , "<html><p>ID of the column from which the current keyword was full-text indexed.</p></html>");
			mtd.addColumn("dm_fts_index_keywords", "document_count", "<html><p>Number of documents or rows containing the current term.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_index_keywords' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_index_keywords_by_document
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_index_keywords_by_document",  "<p>Returns information about the document-level content of a full-text index associated with the specified table. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_index_keywords_by_document", "keyword"         , "<html><p>The hexadecimal representation of the keyword that is stored inside the full-text index.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>OxFF represents the special character that indicates the end of a file or dataset.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_keywords_by_document", "display_term"    , "<html><p>The human-readable format of the keyword. This format is derived from the internal format that is stored in the full-text index. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>OxFF represents the special character that indicates the end of a file or dataset.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_keywords_by_document", "column_id"       , "<html><p>ID of the column from which the current keyword was full-text indexed. </p></html>");
			mtd.addColumn("dm_fts_index_keywords_by_document", "document_id"     , "<html><p>ID of the document or row from which the current term was full-text indexed. This ID corresponds to the full-text key value of that document or row.</p></html>");
			mtd.addColumn("dm_fts_index_keywords_by_document", "occurrence_count", "<html><p>Number of occurrences of the current keyword in the document or row that is indicated by <strong>document_id</strong>. When <span class=\"literal\">'</span><em>search_property_name</em><span class=\"literal\">'</span> is specified, <span class=\"literal\">occurrence_count</span> displays only the number of occurrences of the current keyword in the specified search property within the document or row.?</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_index_keywords_by_document' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_index_keywords_by_property
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_index_keywords_by_property",  "<p>Returns all property-related content in the full-text index of a given table. This includes all data that belongs to any property registered by the search property list associated with that full-text index. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_index_keywords_by_property", "keyword"     , "<html><p>The hexadecimal representation of the keyword that is stored inside the full-text index.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>OxFF represents the special character that indicates the end of a file or dataset.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_keywords_by_property", "display_term", "<html><p>The human-readable format of the keyword. This format is derived from the internal format that is stored in the full-text index. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>OxFF represents the special character that indicates the end of a file or dataset.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_keywords_by_property", "column_id"   , "<html><p>ID of the column from which the current keyword was full-text indexed. </p></html>");
			mtd.addColumn("dm_fts_index_keywords_by_property", "document_id" , "<html><p>ID of the document or row from which the current term was full-text indexed. This ID corresponds to the full-text key value of that document or row.</p></html>");
			mtd.addColumn("dm_fts_index_keywords_by_property", "property_id" , "<html><p>Internal property ID of the search property within the full-text index of the table that you specified in the OBJECT_ID<span class=\"literal\">('</span><em>table_name</em><span class=\"literal\">')</span> parameter. </p><p>When a given property is added to a search property list, the Full-Text Engine registers the property and assigns it an internal property ID that is specific to that property list. The internal property ID, which is an integer, is unique to a given search property list. If a given property is registered for multiple search property lists, a different internal property ID might be assigned for each search property list. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The internal property ID is distinct from the property integer identifier that is specified when adding the property to the search property list. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ee677637.aspx\">Search Document Properties with Search Property Lists</a>.</p></td>    </tr>   </tbody>  </table> </div></div><p><span class=\"label\">To view the association between property_id and the property name</span></p><ul class=\"unordered\"> <li><p><a href=\"https://msdn.microsoft.com/en-us/library/ee677608.aspx\">sys.registered_search_properties (Transact-SQL)</a></p></li></ul></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_index_keywords_by_property' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_index_population
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_index_population",  "<p>Returns information about the full-text index and semantic key phrase populations currently in progress in SQL Server. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_index_population", "database_id"                       , "<html><p>ID of the database that contains the full-text index being populated.</p></html>");
			mtd.addColumn("dm_fts_index_population", "catalog_id"                        , "<html><p>ID of the full-text catalog that contains this full-text index.</p></html>");
			mtd.addColumn("dm_fts_index_population", "table_id"                          , "<html><p>ID of the table for which the full-text index is being populated. </p></html>");
			mtd.addColumn("dm_fts_index_population", "memory_address"                    , "<html><p>Memory address of the internal data structure that is used to represent an active population. </p></html>");
			mtd.addColumn("dm_fts_index_population", "population_type"                   , "<html><p>Type of population. One of the following: </p><p>1 = Full population</p><p>2 = Incremental timestamp-based population</p><p>3 = Manual update of tracked changes</p><p>4 = Background update of tracked changes.</p></html>");
			mtd.addColumn("dm_fts_index_population", "population_type_description"       , "<html><p>Description for type of population.</p></html>");
			mtd.addColumn("dm_fts_index_population", "is_clustered_index_scan"           , "<html><p>Indicates whether the population involves a scan on the clustered index.</p></html>");
			mtd.addColumn("dm_fts_index_population", "range_count"                       , "<html><p>Number of sub-ranges into which this population has been parallelized.</p></html>");
			mtd.addColumn("dm_fts_index_population", "completed_range_count"             , "<html><p>Number of ranges for which processing is complete.</p></html>");
			mtd.addColumn("dm_fts_index_population", "outstanding_batch_count"           , "<html><p>Current number of outstanding batches for this population. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/cc280742.aspx\">sys.dm_fts_outstanding_batches (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_fts_index_population", "status"                            , "<html><p>Status of this Population. Note: some of the states are transient. One of the following:</p><p>3 = Starting</p><p>5 = Processing normally</p><p>7 = Has stopped processing</p><p>For example, this status occurs when an auto merge is in progress.</p><p>11 = Population aborted</p><p>12 = Processing a semantic similarity extraction</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_index_population", "status_description"                , "<html><p>Description of status of the population.</p></html>");
			mtd.addColumn("dm_fts_index_population", "completion_type"                   , "<html><p>Status of how this population completed.</p></html>");
			mtd.addColumn("dm_fts_index_population", "completion_type_description"       , "<html><p>Description of the completion type.</p></html>");
			mtd.addColumn("dm_fts_index_population", "worker_count"                      , "<html><p>This value is always 0. </p></html>");
			mtd.addColumn("dm_fts_index_population", "queued_population_type"            , "<html><p>Type of the population, based on tracked changes, which will follow the current population, if any.</p></html>");
			mtd.addColumn("dm_fts_index_population", "queued_population_type_description", "<html><p>Description of the population to follow, if any. For example, when CHANGE TRACKING = AUTO and the initial full population is in progress, this column would show \"Auto population.\" </p></html>");
			mtd.addColumn("dm_fts_index_population", "start_time"                        , "<html><p>Time that the population started.</p></html>");
			mtd.addColumn("dm_fts_index_population", "incremental_timestamp"             , "<html><p>Represents the starting timestamp for a full population. For all other population types this value is the last committed checkpoint representing the progress of the populations.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_index_population' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_memory_buffers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_memory_buffers",  "<p>Returns information about memory buffers belonging to a specific memory pool that are used as part of a full-text crawl or a full-text crawl range. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_memory_buffers", "pool_id"       , "<html><p>ID of the allocated memory pool.</p><p>0 = Small buffers</p><p>1 = Large buffers </p></html>");
			mtd.addColumn("dm_fts_memory_buffers", "memory_address", "<html><p>Address of the allocated memory buffer.</p></html>");
			mtd.addColumn("dm_fts_memory_buffers", "name"          , "<html><p>Name of the shared memory buffer for which this allocation was made.</p></html>");
			mtd.addColumn("dm_fts_memory_buffers", "is_free"       , "<html><p>Current state of memory buffer.</p><p>0 = Free </p><p>1 = Busy </p></html>");
			mtd.addColumn("dm_fts_memory_buffers", "row_count"     , "<html><p>Number of rows that this buffer is currently handling.</p></html>");
			mtd.addColumn("dm_fts_memory_buffers", "bytes_used"    , "<html><p>Amount, in bytes, of memory in use in this buffer.</p></html>");
			mtd.addColumn("dm_fts_memory_buffers", "percent_used"  , "<html><p>Percentage of allocated memory used.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_memory_buffers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_memory_pools
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_memory_pools",  "<p>Returns information about the shared memory pools available to the Full-Text Gatherer component for a full-text crawl or a full-text crawl range.</p>");

			// Column names and description
			mtd.addColumn("dm_fts_memory_pools", "pool_id"         , "<html><p>ID of the allocated memory pool. </p><p>0 = Small buffers</p><p>1 = Large buffers</p></html>");
			mtd.addColumn("dm_fts_memory_pools", "buffer_size"     , "<html><p>Size of each allocated buffer in the memory pool.</p></html>");
			mtd.addColumn("dm_fts_memory_pools", "min_buffer_limit", "<html><p>Minimum number of buffers allowed in the memory pool.</p></html>");
			mtd.addColumn("dm_fts_memory_pools", "max_buffer_limit", "<html><p>Maximum number of buffers allowed in the memory pool.</p></html>");
			mtd.addColumn("dm_fts_memory_pools", "buffer_count"    , "<html><p>Current number of shared memory buffers in the memory pool.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_memory_pools' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_outstanding_batches
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_outstanding_batches",  "<p>Returns information about each full-text indexing batch. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_outstanding_batches", "database_id"             , "<html><p>ID of the database</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "catalog_id"              , "<html><p>ID of the full-text catalog</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "table_id"                , "<html><p>ID of the table ID that contains the full-text index</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "batch_id"                , "<html><p>Batch ID</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "memory_address"          , "<html><p>The batch object memory address</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "crawl_memory_address"    , "<html><p>Crawl object memory address (parent object)</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "memregion_memory_address", "<html><p>Memory region memory address of the outbound share memory of the filter daemon host (fdhost.exe) </p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "hr_batch"                , "<html><p>Most recent error code for the batch </p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "is_retry_batch"          , "<html><p>Indicates whether this is a retry batch:</p><p>0 = No</p><p>1 = Yes</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "retry_hints"             , "<html><p>Type of retry needed for the batch:</p><p>0 = No retry</p><p>1 = Multi thread retry</p><p>2 = Single thread retry</p><p>3 = Single and multi thread retry</p><p>5 = Multi thread final retry</p><p>6 = Single thread final retry</p><p>7 = Single and multi thread final retry</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "retry_hints_description" , "<html><p>Description for the type of retry needed:</p><p>NO RETRY</p><p>MULTI THREAD RETRY</p><p>SINGLE THREAD RETRY</p><p>SINGLE AND MULTI THREAD RETRY</p><p>MULTI THREAD FINAL RETRY</p><p>SINGLE THREAD FINAL RETRY</p><p>SINGLE AND MULTI THREAD FINAL RETRY </p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "doc_failed"              , "<html><p>Number of documents that failed in the batch</p></html>");
			mtd.addColumn("dm_fts_outstanding_batches", "batch_timestamp"         , "<html><p>The timestamp value obtained when the batch was created</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_outstanding_batches' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_parser
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_parser",  "<p>Returns the final tokenization result after applying a given <a href=\"https://msdn.microsoft.com/en-us/library/ms142509.aspx\">word breaker</a>, <a href=\"https://msdn.microsoft.com/en-us/library/ms142491.aspx\">thesaurus</a>, and <a href=\"https://msdn.microsoft.com/en-us/library/ms142551.aspx\">stoplist</a> combination to a query string input. The tokenization result is equivalent to the output of the Full-Text Engine for the specified query string. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_parser", "keyword"       , "<html><p>The hexadecimal representation of a given keyword returned by a word breaker. This representation is used to store the keyword in the full-text index. This value is not human-readable, but it is useful for relating a given keyword to output returned by other dynamic management views that return the content of a full-text index, such as <a href=\"https://msdn.microsoft.com/en-us/library/cc280900.aspx\">sys.dm_fts_index_keywords</a> and <a href=\"https://msdn.microsoft.com/en-us/library/cc280607.aspx\">sys.dm_fts_index_keywords_by_document</a>. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>OxFF represents the special character that indicates the end of a file or dataset.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_parser", "group_id"      , "<html><p>Contain an integer value that is useful for differentiating the logical group from which a given term was generated. For example, '<span class=\"code\">Server AND DB OR FORMSOF(THESAURUS, DB)\"</span>' produces the following <span class=\"literal\">group_id</span> values in English:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>group_id </p></th>     <th><p>display_term </p></th>    </tr>    <tr>     <td data-th=\"group_id \"><p>1</p></td>     <td data-th=\"display_term \"><p>Server</p></td>    </tr>    <tr>     <td data-th=\"group_id \"><p>2</p></td>     <td data-th=\"display_term \"><p>DB</p></td>    </tr>    <tr>     <td data-th=\"group_id \"><p>3</p></td>     <td data-th=\"display_term \"><p>DB</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_parser", "phrase_id"     , "<html><p>Contains an integer value that is useful for differentiating the cases in which alternative forms of compound words, such as full-text, are issued by the word breaker. Sometimes, with presence of compound words ('multi-million'), alternative forms are issued by the word breaker. These alternative forms (phrases) need to be differentiated sometimes. </p><p>For example, '<span class=\"code\">multi-million</span>' produces the following <span class=\"literal\">phrase_id</span> values in English:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>phrase_id </p></th>     <th><p>display_term </p></th>    </tr>    <tr>     <td data-th=\"phrase_id \"><p>1</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">multi</span></p></td>    </tr>    <tr>     <td data-th=\"phrase_id \"><p>1</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">million</span></p></td>    </tr>    <tr>     <td data-th=\"phrase_id \"><p>2</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">multimillion</span></p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_parser", "occurrence"    , "<html><p>Indicates the order of each term in the parsing result. For example, for the phrase \"<span class=\"code\">SQL Server query processor</span>\" <span class=\"literal\">occurrence</span> would contain the following <span class=\"literal\">occurrence</span> values for the terms in the phrase, in English: </p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>occurrence </p></th>     <th><p>display_term </p></th>    </tr>    <tr>     <td data-th=\"occurrence \"><p>1</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">SQL</span></p></td>    </tr>    <tr>     <td data-th=\"occurrence \"><p>2</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">Server</span></p></td>    </tr>    <tr>     <td data-th=\"occurrence \"><p>3</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">query</span></p></td>    </tr>    <tr>     <td data-th=\"occurrence \"><p>4</p></td>     <td data-th=\"display_term \"><p><span class=\"code\">processor</span></p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_fts_parser", "special_term"  , "<html><p>Contains information about the characteristics of the term that is being issued by the word breaker, one of:</p><p>Exact match</p><p>Noise word</p><p>End of Sentence</p><p>End of paragraph </p><p>End of Chapter</p></html>");
			mtd.addColumn("dm_fts_parser", "display_term"  , "<html><p>Contains the human-readable form of the keyword. As with the functions designed to access the content of the full-text index, this displayed term might not be identical to the original term due to the denormalization limitation. However, it should be precise enough to help you identify it from the original input.</p></html>");
			mtd.addColumn("dm_fts_parser", "expansion_type", "<html><p>Contains information about the nature of the expansion of a given term, one of:</p><p>0 =Single word case</p><p>2=Inflectional expansion</p><p>4=Thesaurus expansion/replacement</p><p>For example, consider a case in which the thesaurus defines run as an expansion of <span class=\"code\">jog</span>:</p><p><span class=\"code\"> &lt;expansion&gt;</span></p><p><span class=\"code\"> &lt;sub&gt;run&lt;/sub&gt;</span></p><p><span class=\"code\"> &lt;sub&gt;jog&lt;/sub&gt;</span></p><p><span class=\"code\"> &lt;/expansion&gt;</span></p><p>The term <span class=\"code\">FORMSOF (FREETEXT, run)</span> generates the following output:</p><p><span class=\"code\">run</span> with <span class=\"literal\">expansion_type</span>=0</p><p><span class=\"code\">runs</span> with <span class=\"literal\">expansion_type</span>=2</p><p><span class=\"code\">running</span> with <span class=\"literal\">expansion_type</span>=2</p><p><span class=\"code\">ran</span> with <span class=\"literal\">expansion_type</span>=2</p><p><span class=\"code\">jog</span> with <span class=\"literal\">expansion_type</span>=4</p></html>");
			mtd.addColumn("dm_fts_parser", "source_term"   , "<html><p>The term or phrase from which a given term was generated or parsed. For example, a query on the '\"<span class=\"code\">word breakers\" AND stemmers'</span> produces the following <span class=\"literal\">source_term</span> values in English:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>source_term </p></th>     <th><p>display_term </p></th>    </tr>    <tr>     <td data-th=\"source_term \"><p>word breakers</p></td>     <td data-th=\"display_term \"><p>word</p></td>    </tr>    <tr>     <td data-th=\"source_term \"><p>word breakers</p></td>     <td data-th=\"display_term \"><p>breakers</p></td>    </tr>    <tr>     <td data-th=\"source_term \"><p>stemmers</p></td>     <td data-th=\"display_term \"><p>stemmers</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_parser' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_population_ranges
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_population_ranges",  "<p>Returns information about the specific ranges related to a full-text index population currently in progress. </p>");

			// Column names and description
			mtd.addColumn("dm_fts_population_ranges", "memory_address"       , "<html><p>Address of memory buffers allocated for activity related to this subrange of a full-text index population.</p></html>");
			mtd.addColumn("dm_fts_population_ranges", "parent_memory_address", "<html><p>Address of memory buffers representing the parent object of all ranges of population related to a full-text index.</p></html>");
			mtd.addColumn("dm_fts_population_ranges", "is_retry"             , "<html><p>If the value is 1, this subrange is responsible for retrying rows that encountered errors.</p></html>");
			mtd.addColumn("dm_fts_population_ranges", "session_id"           , "<html><p>ID of the session that is currently processing this task.</p></html>");
			mtd.addColumn("dm_fts_population_ranges", "processed_row_count"  , "<html><p>Number of rows that have been processed by this range. Forward progress is persisted and counted every 5 minutes, rather than with every batch commit. </p></html>");
			mtd.addColumn("dm_fts_population_ranges", "error_count"          , "<html><p>Number of rows that have encountered errors by this range. Forward progress is persisted and counted </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_population_ranges' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_fts_semantic_similarity_population
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_fts_semantic_similarity_population",  "<p>Returns one row of status information about the population of the document similarity index for each similarity index in each table that has an associated semantic index.</p><p>The population step follows the extraction step. For status information about the similarity extraction step, see <a href=\"https://msdn.microsoft.com/en-us/library/ms186897.aspx\">sys.dm_fts_index_population (Transact-SQL)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_fts_semantic_similarity_population", "Column name"                , "<html><p><strong>Description</strong></p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "database_id"                , "<html><p>ID of the database that contains the full-text index being populated.</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "catalog_id"                 , "<html><p>ID of the full-text catalog that contains this full-text index.</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "table_id"                   , "<html><p>ID of the table for which the full-text index is being populated. </p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "document_count"             , "<html><p>Number of total documents in the population</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "document_processed_count"   , "<html><p>Number of documents processed since start of this population cycle</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "completion_type"            , "<html><p>Status of how this population completed.</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "completion_type_description", "<html><p>Description of the completion type.</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "worker_count"               , "<html><p>Number of worker threads associated with similarity extraction</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "status"                     , "<html><p>Status of this Population. Note: some of the states are transient. One of the following:</p><p>3 = Starting</p><p>5 = Processing normally</p><p>7 = Has stopped processing</p><p>11 = Population aborted</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "status_description"         , "<html><p>Description of status of the population.</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "start_time"                 , "<html><p>Time that the population started.</p></html>");
			mtd.addColumn("dm_fts_semantic_similarity_population", "incremental_timestamp"      , "<html><p>Represents the starting timestamp for a full population. For all other population types this value is the last committed checkpoint representing the progress of the populations.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_fts_semantic_similarity_population' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// geo_replication_links
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("geo_replication_links",  "<p>Contains a row for each replication link between primary and secondary databases in a geo-replication partnership. This view resides in the logical master database.</p>");

			// Column names and description
			mtd.addColumn("geo_replication_links", "database_id"                      , "<html><p> ID of the current database in the sys.databases view.</p></html>");
			mtd.addColumn("geo_replication_links", "start_date"                       , "<html><p> UTC time at a regional SQL Database datacenter when the database replication was initiated</p></html>");
			mtd.addColumn("geo_replication_links", "modify_date"                      , "<html><p> UTC time at regional SQL Database datacenter when the database geo-replication has completed. The new database is synchronized with the primary database as of this time. .</p></html>");
			mtd.addColumn("geo_replication_links", "link_guid"                        , "<html><p>Unique ID of the geo-replication link. </p></html>");
			mtd.addColumn("geo_replication_links", "partner_server"                   , "<html><p>Name of the logical server containing the geo-replicated database.</p></html>");
			mtd.addColumn("geo_replication_links", "partner_database"                 , "<html><p>Name of the geo-replicated database on the linked logical server.</p></html>");
			mtd.addColumn("geo_replication_links", "replication_state"                , "<html><p>The state of geo-replication for this database, one of:.</p><p>0 = Pending. Creation of the active secondary database is scheduled but the necessary preparation steps are not yet completed.</p><p>1 = Seeding. The geo-replication target is being seeded but the two databases are not yet synchronized. Until seeding completes, you cannot connect to the secondary database. Removing secondary database from the primary will cancel the seeding operation. </p><p>2 = Catch-up. The secondary database is in a transactionally consistent state and is being constantly synchronized with the primary database.</p></html>");
			mtd.addColumn("geo_replication_links", "replication_state_desc"           , "<html><p>PENDING</p><p>SEEDING</p><p>CATCH_UP</p></html>");
			mtd.addColumn("geo_replication_links", "role"                             , "<html><p>Geo-replication role, one of:</p><p>0 = Primary. The database_id refers to the primary database in the geo-replication partnership. </p><p>1 = Secondary. The database_id refers to the primary database in the geo-replication partnership.</p></html>");
			mtd.addColumn("geo_replication_links", "role_desc"                        , "<html><p>PRIMARY</p><p>SECONDARY</p></html>");
			mtd.addColumn("geo_replication_links", "secondary_allow_connections"      , "<html><p>The secondary type, one of:</p><p>0 = No. The secondary database is not accessible until failover.</p><p>1 = All. The secondary database is accessible to any client connection.</p><p>2 = ReadOnly. The secondary database is accessible only to client connections with ApplicationIntent=ReadOnly.</p></html>");
			mtd.addColumn("geo_replication_links", "secondary_allow_connections _desc", "<html><p>No</p><p>All</p><p>Read-Only</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'geo_replication_links' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_operation_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_operation_status",  "<p>Returns information about operations performed on databases in a Azure SQL Database server. </p>");

			// Column names and description
			mtd.addColumn("dm_operation_status", "session_activity_id", "<html><p>ID of the operation. Not null.</p></html>");
			mtd.addColumn("dm_operation_status", "res_type"           , "<html><p>Denotes the type of resource on which the operation is performed. Not null. In the current release, this view tracks operations performed on SQL Database only, and the corresponding integer value is 0.</p></html>");
			mtd.addColumn("dm_operation_status", "resource_type_desc" , "<html><p>Description of the resource type on which the operation is performed. In the current release, this view tracks operations performed on SQL Database only.</p></html>");
			mtd.addColumn("dm_operation_status", "major_resource_id"  , "<html><p>Name of the SQL Database on which the operation is performed. Not Null.</p></html>");
			mtd.addColumn("dm_operation_status", "minor_resource_id"  , "<html><p>For internal use only. Not null.</p></html>");
			mtd.addColumn("dm_operation_status", "operation"          , "<html><p>Operation performed on a SQL Database, such as CREATE or ALTER.</p></html>");
			mtd.addColumn("dm_operation_status", "state"              , "<html><p>The state of the operation.</p><p>0 = Pending<br>1 = In progress<br>2 = Completed<br>3 = Failed<br>4 = Cancelled</p></html>");
			mtd.addColumn("dm_operation_status", "state_desc"         , "<html><p>PENDING = operation is waiting for resource or quota availability.</p><p>IN_PROGRESS = operation has started and is in progress.</p><p>COMPLETED = operation completed successfully.</p><p>FAILED = operation failed. See the <strong>error_desc</strong> column for details.</p><p>CANCELLED = operation stopped at the request of the user.</p></html>");
			mtd.addColumn("dm_operation_status", "percent_complete"   , "<html><p>Percentage of operation that has completed. Values range from 0 to 100. Not null.</p></html>");
			mtd.addColumn("dm_operation_status", "error_code"         , "<html><p>Code indicating the error that occurred during a failed operation. If the value is 0, it indicates that the operation completed successfully.</p></html>");
			mtd.addColumn("dm_operation_status", "error_desc"         , "<html><p>Description of the error that occurred during a failed operation.</p></html>");
			mtd.addColumn("dm_operation_status", "error_severity"     , "<html><p>Severity level of the error that occurred during a failed operation. For more information about error severities, see <a href=\"http://go.microsoft.com/fwlink/?LinkId=251052\">Database Engine Error Severities</a>.</p></html>");
			mtd.addColumn("dm_operation_status", "error_state"        , "<html><p>Reserved for future use. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_operation_status", "start_time"         , "<html><p>Timestamp when the operation started.</p></html>");
			mtd.addColumn("dm_operation_status", "last_modify_time"   , "<html><p>Timestamp when the record was last modified for a long running operation. In case of successfully completed operations, this field displays the timestamp when the operation completed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_operation_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_geo_replication_link_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_geo_replication_link_status",  "<p>Contains a row for each replication link between primary and secondary databases in a geo-replication partnership. This includes both primary and secondary databases. If more than one continuous replication link exists for a given primary database, this table contains a row for each of the relationships. The view is created in all databases, including the logical master. However, querying this view in the logical master returns an empty set. </p>");

			// Column names and description
			mtd.addColumn("dm_geo_replication_link_status", "link_guid"             , "<html><p> Unique ID of the replication link.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "partner_server"        , "<html><p> Name of the logical server containing the linked database.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "partner_database"      , "<html><p> Name of the linked database on the linked logical server.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "last_replication"      , "<html><p>The timestamp of the last transaction?s acknowledgement by the secondary based on the primary database clock. This value is available on the primary database only.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "replication_lag_sec"   , "<html><p>Time difference in seconds between the last_replication value and the timestamp of that transaction?s commit on the primary based on the primary database clock. This value is available on the primary database only.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "replication_state"     , "<html><p>The state of geo-replication for this database, one of:.</p><p>1 = Seeding. The geo-replication target is being seeded but the two databases are not yet synchronized. Until seeding completes, you cannot connect to the secondary database. Removing secondary database from the primary will cancel the seeding operation. </p><p>2 = Catch-up. The secondary database is in a transactionally consistent state and is being constantly synchronized with the primary database.</p><p>4 = Suspended. This is not an active continuous-copy relationship. This state usually indicates that the bandwidth available for the interlink is insufficient for the level of transaction activity on the primary database. However, the continuous-copy relationship is still intact.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "replication_state_desc", "<html><p>PENDING</p><p>SEEDING</p><p>CATCH_UP</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "role"                  , "<html><p>Geo-replication role, one of:</p><p>0 = Primary. The database_id refers to the primary database in the geo-replication partnership. </p><p>1 = Secondary. The database_id refers to the primary database in the geo-replication partnership.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "role_desc"             , "<html><p>PRIMARY</p><p>SECONDARY</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "secondary_type"        , "<html><p>The secondary type, one of:</p><p>-1 = No. The secondary database is not accessible until failover.</p><p> 0 = All. The secondary database is accessible to any client connection.</p></html>");
			mtd.addColumn("dm_geo_replication_link_status", "secondary_type _desc"  , "<html><p>No</p><p>All</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_geo_replication_link_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_continuous_copy_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_continuous_copy_status",  "<p>Returns a row for each user database that is currently engaged in a Geo-replication continuous-copy relationship. If more than one continuous copy relationship is initiated for a given primary database this table contains one row for each active secondary database. </p>");

			// Column names and description
			mtd.addColumn("dm_continuous_copy_status", "copy_guid"             , "<html><p>Unique ID of the replica database.</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "partner_server"        , "<html><p>Name of the linked SQL Database server.</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "partner_database"      , "<html><p>Name of the linked database on the linked SQL Database server.</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "last_replication"      , "<html><p>The timestamp of the last applied replicated transaction.</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "replication_lag_sec"   , "<html><p>Time difference in seconds between the current time and the timestamp of the last successfully committed transaction on the primary database that has not been acknowledged by the active secondary database.</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "replication_state"     , "<html><p>The state of continuous-copy replication for this database, one of:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Seeding. The replication target is being seeded and is in a transactionally inconsistent state. Until seeding completes, you cannot connect to the active secondary database. </p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Catching up. The active secondary database is currently catching up to the primary database and is in a transactionally consistent state.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>3</p></td>     <td data-th=\"Description\"><p>Re-seeding. The active secondary database is being automatically re-seeded because of an unrecoverable replication failure.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>4</p></td>     <td data-th=\"Description\"><p>Suspended. This is not an active continuous-copy relationship. This state usually indicates that the bandwidth available for the interlink is insufficient for the level of transaction activity on the primary database. However, the continuous-copy relationship is still intact. </p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_continuous_copy_status", "replication_state_desc", "<html><p>Description of replication_state, one of: </p><p>SEEDING</p><p>CATCH_UP</p><p>RE_SEEDING</p><p>SUSPENDED</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "is_rpo_limit_reached"  , "<html><p>This is always set to 0</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "is_target_role"        , "<html><p>0 = Source of copy relationship</p><p>1 = Target of copy relationship</p></html>");
			mtd.addColumn("dm_continuous_copy_status", "is_interlink_connected", "<html><p>1 = Interlink is connected.</p><p>0 = Interlink is disconnected.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_continuous_copy_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_column_store_row_group_physical_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_column_store_row_group_physical_stats",  "<p>Provides current rowgroup-level information about all of the columnstore indexes in the current database. </p><p>This extends the catalog view <a href=\"https://msdn.microsoft.com/en-us/library/dn223749.aspx\">sys.column_store_row_groups (Transact-SQL)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "object_id"                          , "<html><p>ID of the underlying table.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "index_id"                           , "<html><p>ID of this columnstore index on <em>object_id</em> table.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "partition_number"                   , "<html><p>ID of the table partition that holds <em>row_group_id</em>. You can use partition_number to join this DMV to sys.partitions.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "row_group_id"                       , "<html><p>ID of this row group. For partitioned tables, this is unique within the partition.</p><p>-1 for an in-memory tail. </p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "delta_store_hobt_id"                , "<html><p>The hobt_id for a row group in the delta store. </p><p>NULL if row group is not in the delta store. </p><p>NULL for tail of an in-memory table.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "state"                              , "<html><p>ID number associated <em>state_description</em>.</p><p>0 = INVISIBLE</p><p>1 = OPEN</p><p>2 = CLOSED</p><p>3 = COMPRESSED</p><p>4 = TOMBSTONE</p><p>COMPRESSED is the only state that applies to in-memory tables.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "state_desc"                         , "<html><p>Description of the row group state:</p><p>INVISIBLE ?A row group that is being built. </p><p>For example: </p><ul class=\"unordered\"> <li><p>A row group in the columnstore is INVISIBLE while the data is being compressed. When the compression is finished a metadata switch changes the state of the columnstore row group from INVISIBLE to COMPRESSED, and the state of the deltastore row group from CLOSED to TOMBSTONE.</p></li> <li><p>A row group </p></li></ul><p>OPEN ? A deltastore row group that is accepting new rows. An open row group is still in rowstore format and has not been compressed to columnstore format.</p><p>CLOSED ? A row group in the delta store that contains the maximum number of rows, and is waiting for the tuple mover process to compress it into the columnstore. </p><p>COMPRESSED ? A row group that is compressed with columnstore compression and stored in the columnstore. </p><p>TOMBSTONE ? A row group that was formerly in the deltastore and is no longer used. </p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "total_rows"                         , "<html><p>Number of rows physical stored in the row group. For compressed row groups, this includes the rows that are marked deleted.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "deleted_rows"                       , "<html><p>Number of rows physically stored in a compressed row group that are marked for deletion. </p><p>0 for row groups that are in the delta store.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "size_in_bytes"                      , "<html><p>Combined size, in bytes, of all the pages in this row group. This size does not include the size required to store metadata or shared dictionaries.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "transition_to_compressed_state"     , "<html><p>Shows how this rowgroup got moved from the deltastore to a compressed state in the columnstore.</p><p>0 - NOT_APPLICABLE</p><p>1 ? INDEX_BUILD</p><p>2 ? TUPLE_MOVER</p><p>3 ? REORG_NORMAL</p><p>4 ? REORG_FORCED</p><p>5 - BULKLOAD</p><p>6 - MERGE</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "transition_to_compressed_state_desc", "<html><p>NOT_APPLICABLE ? the operation does not apply to the deltastore. Or, the rowgroup was compressed prior to upgrading to SQL Server 2016 Community Technology Preview 2 (CTP2) in which case the history is not preserved.</p><p>INDEX_BUILD ? An index create or index rebuild compressed the rowgroup. </p><p>TUPLE_MOVER ? The tuple mover running in the background compressed the rowgroup. This happens after the rowgroup changes state from OPEN to CLOSED.</p><p>REORG_NORMAL ? The reorganization operation, ALTER INDEX ? REORG, moved the CLOSED rowgroup from the deltastore to the columnstore. This occurred before the tuple-mover had time to move the rowgroup.</p><p>REORG_FORCED ? This rowgroup was open in the deltastore and was forced into the columnstore before it had a full number of rows.</p><p>BULKLOAD ? A bulk load operation compressed the rowgroup directly without using the deltastore.</p><p>MERGE ? A merge operation consolidated one or more rowgroups into this rowgroup and then performed the columnstore compression.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "trim_reason"                        , "<html><p>Reason that triggered the COMPRESSED row group to have less than the maximum number of rows.</p><p>0 ? UNKNOWN_UPGRADED_FROM_PREVIOUS_VERSION</p><p>1 - NORMAL</p><p>2 - BULKLOAD</p><p>3 ? REORG </p><p>4 ? DICTIONARY_SIZE</p><p>5 ? MEMORY LIMITAION</p><p>6 ? RESIDUAL ROW GROUP</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "trim_reason_desc"                   , "<html><p>Description of <em>trim_reason</em>.</p><p>0 ? UNKNOWN_UPGRADED_FROM_PREVIOUS_VERSION: Occurred when upgrading from the previous version of SQL Server.</p><p>1 - NORMAL: The row group was not trimmed. The row group was compressed with the maximum of 1,048,476 rows. </p><p>2 ? BULKLOAD: The bulk load batch size limited the number of rows.</p><p>3 ? REORG: The reorganize operation reduced the size by deleting physical rows or merging with another row group.</p><p>4 ? DICTIONARY_SIZE: Dictionary size grew too big to compress all of the rows together.</p><p>5 ? MEMORY LIMITAION: Not enough available memory to compress all the rows together.</p><p>6 ? RESIDUAL ROW GROUP: Rows at the end of a bulk load were less than the maximum rows per row group.</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "has_vertipaq-optimization"          , "<html><p>Vertipaq optimization improves columnstore compression by rearranging the order of the rows in the rowgroup to achieve higher compression. This optimization occurs automatically in most cases. An exception, SQL Server skips Vertipaq optimization when a delta rowgroup moves into the columnstore and there are one or more nonclustered indexes on the columnstore index. In this case Vertipaq optimization is skipped to minimizes changes to the mapping index. </p><p>0 = No</p><p>1 = Yes</p></html>");
			mtd.addColumn("dm_db_column_store_row_group_physical_stats", "creation_time"                      , "<html><p>Clock time for when this rowgroup was created. </p><p>NULL ? for a columnstore index on an in-memory table.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_column_store_row_group_physical_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_index_operational_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_index_operational_stats",  "<p>Returns current lowore-level I/O, locking, latching, and access method activity for each partition of a table or index in the database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_index_operational_stats", "database_id"                       , "<html><p>Database ID.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "object_id"                         , "<html><p>ID of the table or view.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "index_id"                          , "<html><p>ID of the index or heap. </p><p>0 = Heap </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "hobt_id"                           , "<html><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server (SQL Server 2016 Community Technology Preview 2 (CTP2) through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299658\">current version</a>), Azure SQL Database.</p></td>    </tr>   </tbody>  </table> </div></div><p>ID of the data heap or B-tree rowset that tracks internal data for a columnstore index. </p><p>NULL ? this is not an internal columnstore rowset.</p><p>For more details, see <a href=\"https://msdn.microsoft.com/en-us/library/dn917448.aspx\">sys.internal_partitions (Transact-SQL)</a></p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "partition_number"                  , "<html><p>1-based partition number within the index or heap. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "leaf_insert_count"                 , "<html><p>Cumulative count of leaf-level inserts.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "leaf_delete_count"                 , "<html><p>Cumulative count of leaf-level deletes. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "leaf_update_count"                 , "<html><p>Cumulative count of leaf-level updates. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "leaf_ghost_count"                  , "<html><p>Cumulative count of leaf-level rows that are marked as deleted, but not yet removed. These rows are removed by a cleanup thread at set intervals. This value does not include rows that are retained, because of an outstanding snapshot isolation transaction. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "nonleaf_insert_count"              , "<html><p>Cumulative count of inserts above the leaf level. </p><p>0 = Heap or columnstore</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "nonleaf_delete_count"              , "<html><p>Cumulative count of deletes above the leaf level. </p><p>0 = Heap or columnstore</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "nonleaf_update_count"              , "<html><p>Cumulative count of updates above the leaf level. </p><p>0 = Heap or columnstore</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "leaf_allocation_count"             , "<html><p>Cumulative count of leaf-level page allocations in the index or heap.</p><p>For an index, a page allocation corresponds to a page split.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "nonleaf_allocation_count"          , "<html><p>Cumulative count of page allocations caused by page splits above the leaf level. </p><p>0 = Heap or columnstore</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "leaf_page_merge_count"             , "<html><p>Cumulative count of page merges at the leaf level. Always 0 for columnstore index.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "nonleaf_page_merge_count"          , "<html><p>Cumulative count of page merges above the leaf level. </p><p>0 = Heap or columnstore</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "range_scan_count"                  , "<html><p>Cumulative count of range and table scans started on the index or heap.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "singleton_lookup_count"            , "<html><p>Cumulative count of single row retrievals from the index or heap. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "forwarded_fetch_count"             , "<html><p>Count of rows that were fetched through a forwarding record. </p><p>0 = Indexes</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "lob_fetch_in_pages"                , "<html><p>Cumulative count of large object (LOB) pages retrieved from the LOB_DATA allocation unit. These pages contain data that is stored in columns of type <strong>text</strong>, <strong>ntext</strong>, <strong>image</strong>, <strong>varchar(max)</strong>, <strong>nvarchar(max)</strong>, <strong>varbinary(max)</strong>, and <strong>xml</strong>. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms187752.aspx\">Data Types (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "lob_fetch_in_bytes"                , "<html><p>Cumulative count of LOB data bytes retrieved. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "lob_orphan_create_count"           , "<html><p>Cumulative count of orphan LOB values created for bulk operations.</p><p>0 = Nonclustered index</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "lob_orphan_insert_count"           , "<html><p>Cumulative count of orphan LOB values inserted during bulk operations.</p><p>0 = Nonclustered index</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "row_overflow_fetch_in_pages"       , "<html><p>Cumulative count of row-overflow data pages retrieved from the ROW_OVERFLOW_DATA allocation unit.</p><p>These pages contain data stored in columns of type <strong>varchar(n)</strong>, <strong>nvarchar(n)</strong>, <strong>varbinary(n)</strong>, and <strong>sql_variant</strong> that has been pushed off-row. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "row_overflow_fetch_in_bytes"       , "<html><p>Cumulative count of row-overflow data bytes retrieved. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "column_value_push_off_row_count"   , "<html><p>Cumulative count of column values for LOB data and row-overflow data that is pushed off-row to make an inserted or updated row fit within a page. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "column_value_pull_in_row_count"    , "<html><p>Cumulative count of column values for LOB data and row-overflow data that is pulled in-row. This occurs when an update operation frees up space in a record and provides an opportunity to pull in one or more off-row values from the LOB_DATA or ROW_OVERFLOW_DATA allocation units to the IN_ROW_DATA allocation unit. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "row_lock_count"                    , "<html><p>Cumulative number of row locks requested. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "row_lock_wait_count"               , "<html><p>Cumulative number of times the Database Engine waited on a row lock. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "row_lock_wait_in_ms"               , "<html><p>Total number of milliseconds the Database Engine waited on a row lock. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_lock_count"                   , "<html><p>Cumulative number of page locks requested.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_lock_wait_count"              , "<html><p>Cumulative number of times the Database Engine waited on a page lock. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_lock_wait_in_ms"              , "<html><p>Total number of milliseconds the Database Engine waited on a page lock. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "index_lock_promotion_attempt_count", "<html><p>Cumulative number of times the Database Engine tried to escalate locks. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "index_lock_promotion_count"        , "<html><p>Cumulative number of times the Database Engine escalated locks. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_latch_wait_count"             , "<html><p>Cumulative number of times the Database Engine waited, because of latch contention.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_latch_wait_in_ms"             , "<html><p>Cumulative number of milliseconds the Database Engine waited, because of latch contention.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_io_latch_wait_count"          , "<html><p>Cumulative number of times the Database Engine waited on an I/O page latch. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_io_latch_wait_in_ms"          , "<html><p>Cumulative number of milliseconds the Database Engine waited on a page I/O latch. </p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "tree_page_latch_wait_count"        , "<html><p>Subset of <strong>page_latch_wait_count</strong> that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "tree_page_latch_wait_in_ms"        , "<html><p>Subset of <strong>page_latch_wait_in_ms</strong> that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "tree_page_io_latch_wait_count"     , "<html><p>Subset of <strong>page_io_latch_wait_count</strong> that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "tree_page_io_latch_wait_in_ms"     , "<html><p>Subset of <strong>page_io_latch_wait_in_ms</strong> that includes only the upper-level B-tree pages. Always 0 for a heap or columnstore index.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_compression_attempt_count"    , "<html><p>Number of pages that were evaluated for PAGE level compression for specific partitions of a table, index, or indexed view. Includes pages that were not compressed because significant savings could not be achieved. Always 0 for columnstore index.</p></html>");
			mtd.addColumn("dm_db_index_operational_stats", "page_compression_success_count"    , "<html><p>Number of data pages that were compressed by using PAGE compression for specific partitions of a table, index, or indexed view. Always 0 for columnstore index.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_index_operational_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_index_usage_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_index_usage_stats",  "<p>Returns counts of different types of index operations and the time each type of operation was last performed in SQL Server.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out.</p>");

			// Column names and description
			mtd.addColumn("dm_db_index_usage_stats", "database_id"       , "<html><p>ID of the database on which the table or view is defined. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "object_id"         , "<html><p>ID of the table or view on which the index is defined </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "index_id"          , "<html><p>ID of the index.</p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "user_seeks"        , "<html><p>Number of seeks by user queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "user_scans"        , "<html><p>Number of scans by user queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "user_lookups"      , "<html><p>Number of bookmark lookups by user queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "user_updates"      , "<html><p>Number of updates by user queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_user_seek"    , "<html><p>Time of last user seek </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_user_scan"    , "<html><p>Time of last user scan. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_user_lookup"  , "<html><p>Time of last user lookup. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_user_update"  , "<html><p>Time of last user update. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "system_seeks"      , "<html><p>Number of seeks by system queries.</p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "system_scans"      , "<html><p>Number of scans by system queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "system_lookups"    , "<html><p>Number of lookups by system queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "system_updates"    , "<html><p>Number of updates by system queries. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_system_seek"  , "<html><p>Time of last system seek. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_system_scan"  , "<html><p>Time of last system scan. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_system_lookup", "<html><p>Time of last system lookup. </p></html>");
			mtd.addColumn("dm_db_index_usage_stats", "last_system_update", "<html><p>Time of last system update. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_index_usage_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_missing_index_details
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_missing_index_details",  "<p>Returns detailed information about missing indexes, excluding spatial indexes.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out.</p>");

			// Column names and description
			mtd.addColumn("dm_db_missing_index_details", "index_handle"      , "<html><p>Identifies a particular missing index. The identifier is unique across the server. <strong>index_handle</strong> is the key of this table.</p></html>");
			mtd.addColumn("dm_db_missing_index_details", "database_id"       , "<html><p>Identifies the database where the table with the missing index resides.</p></html>");
			mtd.addColumn("dm_db_missing_index_details", "object_id"         , "<html><p>Identifies the table where the index is missing.</p></html>");
			mtd.addColumn("dm_db_missing_index_details", "equality_columns"  , "<html><p>Comma-separated list of columns that contribute to equality predicates of the form:</p><p><em>table.column</em> =<span class=\"code\">?</span><em>constant_value</em></p></html>");
			mtd.addColumn("dm_db_missing_index_details", "inequality_columns", "<html><p>Comma-separated list of columns that contribute to inequality predicates, for example, predicates of the form:</p><p><em>table.column</em> &gt; <em>constant_value</em></p><p>Any comparison operator other than \"=\" expresses inequality.</p></html>");
			mtd.addColumn("dm_db_missing_index_details", "included_columns"  , "<html><p>Comma-separated list of columns needed as covering columns for the query. For more information about covering or included columns, see <a href=\"https://msdn.microsoft.com/en-us/library/ms190806.aspx\">Create Indexes with Included Columns</a>.</p><p>For memory-optimized indexes (both hash and memory-optimized nonclustered), ignore <strong>included_columns</strong>. All columns of the table are included in every memory-optimized index.</p></html>");
			mtd.addColumn("dm_db_missing_index_details", "statement"         , "<html><p>Name of the table where the index is missing.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_missing_index_details' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_missing_index_groups
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_missing_index_groups",  "<p>Returns information about what missing indexes are contained in a specific missing index group, excluding spatial indexes.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out.</p>");

			// Column names and description
			mtd.addColumn("dm_db_missing_index_groups", "index_group_handle", "<html><p>Identifies a missing index group.</p></html>");
			mtd.addColumn("dm_db_missing_index_groups", "index_handle"      , "<html><p>Identifies a missing index that belongs to the group specified by <strong>index_group_handle</strong>.</p><p>An index group contains only one index.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_missing_index_groups' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_index_physical_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_index_physical_stats",  "<p>Returns size and fragmentation information for the data and indexes of the specified table or view in SQL Server. For an index, one row is returned for each level of the B-tree in each partition. For a heap, one row is returned for the IN_ROW_DATA allocation unit of each partition. For large object (LOB) data, one row is returned for the LOB_DATA allocation unit of each partition. If row-overflow data exists in the table, one row is returned for the ROW_OVERFLOW_DATA allocation unit in each partition. Does not return information about xVelocity memory optimized columnstore indexes.</p>");

			// Column names and description
			mtd.addColumn("dm_db_index_physical_stats", "database_id"                        , "<html><p>Database ID of the table or view. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "object_id"                          , "<html><p>Object ID of the table or view that the index is on.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "index_id"                           , "<html><p>Index ID of an index. </p><p>0 = Heap.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "partition_number"                   , "<html><p>1-based partition number within the owning object; a table, view, or index. </p><p>1 = Nonpartitioned index or heap.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "index_type_desc"                    , "<html><p>Description of the index type:</p><p>HEAP</p><p>CLUSTERED INDEX</p><p>NONCLUSTERED INDEX</p><p>PRIMARY XML INDEX</p><p>SPATIAL INDEX?</p><p>XML INDEX </p><p>COLUMNSTORE MAPPING INDEX (internal)</p><p>COLUMNSTORE DELETEBUFFER INDEX (internal)</p><p>COLUMNSTORE DELETEBITMAP INDEX (internal)</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "hobt_id"                            , "<html><p>Heap or B-Tree ID of the index or partition.</p><p>Besides returning the hobt_id of user-defined indexes, this also returns the hobt_id of the internal columnstore indexes. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "alloc_unit_type_desc"               , "<html><p>Description of the allocation unit type:</p><p>IN_ROW_DATA</p><p>LOB_DATA</p><p>ROW_OVERFLOW_DATA</p><p>The LOB_DATA allocation unit contains the data that is stored in columns of type <strong>text</strong>, <strong>ntext</strong>, <strong>image</strong>, <strong>varchar(max)</strong>, <strong>nvarchar(max)</strong>, <strong>varbinary(max)</strong>, and <strong>xml</strong>. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms187752.aspx\">Data Types (Transact-SQL)</a>.</p><p>The ROW_OVERFLOW_DATA allocation unit contains the data that is stored in columns of type <strong>varchar(n)</strong>, <strong>nvarchar(n)</strong>, <strong>varbinary(n)</strong>, and <strong>sql_variant</strong> that have been pushed off-row. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "index_depth"                        , "<html><p>Number of index levels. </p><p>1 = Heap, or LOB_DATA or ROW_OVERFLOW_DATA allocation unit. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "index_level"                        , "<html><p>Current level of the index. </p><p>0 for index leaf levels, heaps, and LOB_DATA or ROW_OVERFLOW_DATA allocation units. </p><p>Greater than 0 for nonleaf index levels. <em>index_level</em> will be the highest at the root level of an index. </p><p>The nonleaf levels of indexes are only processed when <em>mode</em> = DETAILED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "avg_fragmentation_in_percent"       , "<html><p>Logical fragmentation for indexes, or extent fragmentation for heaps in the IN_ROW_DATA allocation unit. </p><p>The value is measured as a percentage and takes into account multiple files. For definitions of logical and extent fragmentation, see Remarks. </p><p>0 for LOB_DATA and ROW_OVERFLOW_DATA allocation units.</p><p>NULL for heaps when <em>mode</em> = SAMPLED.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "fragment_count"                     , "<html><p>Number of fragments in the leaf level of an IN_ROW_DATA allocation unit. For more information about fragments, see Remarks.</p><p>NULL for nonleaf levels of an index, and LOB_DATA or ROW_OVERFLOW_DATA allocation units. </p><p>NULL for heaps when <em>mode</em> = SAMPLED.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "avg_fragment_size_in_pages"         , "<html><p>Average number of pages in one fragment in the leaf level of an IN_ROW_DATA allocation unit. </p><p>NULL for nonleaf levels of an index, and LOB_DATA or ROW_OVERFLOW_DATA allocation units. </p><p>NULL for heaps when <em>mode</em> = SAMPLED.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "page_count"                         , "<html><p>Total number of index or data pages.</p><p>For an index, the total number of index pages in the current level of the b-tree in the IN_ROW_DATA allocation unit.</p><p>For a heap, the total number of data pages in the IN_ROW_DATA allocation unit. </p><p>For LOB_DATA or ROW_OVERFLOW_DATA allocation units, total number of pages in the allocation unit.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "avg_page_space_used_in_percent"     , "<html><p>Average percentage of available data storage space used in all pages.</p><p>For an index, average applies to the current level of the b-tree in the IN_ROW_DATA allocation unit. </p><p>For a heap, the average of all data pages in the IN_ROW_DATA allocation unit.</p><p>For LOB_DATA or ROW_OVERFLOW DATA allocation units, the average of all pages in the allocation unit. </p><p>NULL when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "record_count"                       , "<html><p>Total number of records.</p><p>For an index, total number of records applies to the current level of the b-tree in the IN_ROW_DATA allocation unit. </p><p>For a heap, the total number of records in the IN_ROW_DATA allocation unit.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>For a heap, the number of records returned from this function might not match the number of rows that are returned by running a SELECT COUNT(*) against the heap. This is because a row may contain multiple records. For example, under some update situations, a single heap row may have a forwarding record and a forwarded record as a result of the update operation. Also, most large LOB rows are split into multiple records in LOB_DATA storage.</p></td>    </tr>   </tbody>  </table> </div></div><p>For LOB_DATA or ROW_OVERFLOW_DATA allocation units, the total number of records in the complete allocation unit.</p><p>NULL when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "ghost_record_count"                 , "<html><p>Number of ghost records ready for removal by the ghost cleanup task in the allocation unit. </p><p>0 for nonleaf levels of an index in the IN_ROW_DATA allocation unit.</p><p>NULL when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "version_ghost_record_count"         , "<html><p>Number of ghost records retained by an outstanding snapshot isolation transaction in an allocation unit.</p><p>0 for nonleaf levels of an index in the IN_ROW_DATA allocation unit.</p><p>NULL when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "min_record_size_in_bytes"           , "<html><p>Minimum record size in bytes.</p><p>For an index, minimum record size applies to the current level of the b-tree in the IN_ROW_DATA allocation unit. </p><p>For a heap, the minimum record size in the IN_ROW_DATA allocation unit.</p><p>For LOB_DATA or ROW_OVERFLOW_DATA allocation units, the minimum record size in the complete allocation unit.</p><p>NULL when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "max_record_size_in_bytes"           , "<html><p>Maximum record size in bytes.</p><p>For an index, the maximum record size applies to the current level of the b-tree in the IN_ROW_DATA allocation unit. </p><p>For a heap, the maximum record size in the IN_ROW_DATA allocation unit.</p><p>For LOB_DATA or ROW_OVERFLOW_DATA allocation units, the maximum record size in the complete allocation unit.</p><p>NULL when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "avg_record_size_in_bytes"           , "<html><p>Average record size in bytes.</p><p>For an index, the average record size applies to the current level of the b-tree in the IN_ROW_DATA allocation unit.</p><p>For a heap, the average record size in the IN_ROW_DATA allocation unit.</p><p>For LOB_DATA or ROW_OVERFLOW_DATA allocation units, the average record size in the complete allocation unit.</p><p>NULL when <em>mode</em> = LIMITED.</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "forwarded_record_count"             , "<html><p>Number of records in a heap that have forward pointers to another data location. (This state occurs during an update, when there is not enough room to store the new row in the original location.)</p><p>NULL for any allocation unit other than the IN_ROW_DATA allocation units for a heap. </p><p>NULL for heaps when <em>mode</em> = LIMITED. </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "compressed_page_count"              , "<html><p>The number of compressed pages.</p><ul class=\"unordered\"> <li><p>For heaps, newly allocated pages are not PAGE compressed. A heap is PAGE compressed under two special conditions: when data is bulk imported or when a heap is rebuilt. Typical DML operations that cause page allocations will not be PAGE compressed. Rebuild a heap when the <span class=\"literal\">compressed_page_count</span> value grows larger than the threshold you want.</p></li> <li><p>For tables that have a clustered index, the <span class=\"literal\">compressed_page_count</span> value indicates the effectiveness of PAGE compression.</p></li></ul></html>");
			mtd.addColumn("dm_db_index_physical_stats", "hobt_id"                            , "<html><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server (SQL Server 2016 Community Technology Preview 2 (CTP2) through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299658\">current version</a>), Azure SQL Database.</p></td>    </tr>   </tbody>  </table> </div></div><p>For columnstore indexes only, this is the ID for a rowset that tracks internal columnstore data for a partition. The rowsets are stored as data heaps or binary trees. They have the same index ID as the parent columnstore index. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/dn917448.aspx\">sys.internal_partitions (Transact-SQL)</a>. </p><p>NULL if </p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "column_store_delete_buffer_state"   , "<html><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server (SQL Server 2016 Community Technology Preview 2 (CTP2) through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299658\">current version</a>), Azure SQL Database.</p></td>    </tr>   </tbody>  </table> </div></div><p>0 = NOT_APPLICABLE</p><p>1 = OPEN</p><p>2 = DRAINING</p><p>3 = FLUSHING</p><p>4 = RETIRING</p><p>5 = READY</p></html>");
			mtd.addColumn("dm_db_index_physical_stats", "column_store_delete_buff_state_desc", "<html><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server (SQL Server 2016 Community Technology Preview 2 (CTP2) through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299658\">current version</a>), Azure SQL Database.</p></td>    </tr>   </tbody>  </table> </div></div><p>NOT_APPLICABLE ?the parent index is not a columnstore index.</p><p>OPEN ? deleters and scanners use this.</p><p>DRAINING ? deleters are draining out but scanners still use it.</p><p>FLUSHING ? buffer is closed and rows in the buffer are being written to the delete bitmap.</p><p>RETIRING ? rows in the clooosed delete buffer have been written to the delete bitmap, but the buffer has not been truncated because scanners are still using it. New scanners don?t need to use the retiring buffer because the open buffer is enough.</p><p>READY ? This delete buffer is ready for use.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_index_physical_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_missing_index_columns
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_missing_index_columns",  "<p>Returns information about database table columns that are missing an index, excluding spatial indexes. <strong>sys.dm_db_missing_index_columns</strong> is a dynamic management function.</p>");

			// Column names and description
			mtd.addColumn("dm_db_missing_index_columns", "column_id"   , "<html><p>ID of the column. </p></html>");
			mtd.addColumn("dm_db_missing_index_columns", "column_name" , "<html><p>Name of the table column.</p></html>");
			mtd.addColumn("dm_db_missing_index_columns", "column_usage", "<html><p>How the column is used by the query. Possible values are:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>EQUALITY</p></td>     <td data-th=\"Description\"><p>Column contributes to a predicate that expresses equality, of the form:</p><p><em>table.column</em> =<span class=\"code\">?</span><em>constant_value</em></p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>INEQUALITY</p></td>     <td data-th=\"Description\"><p>Column contributes to a predicate that expresses inequality, for example, a predicate of the form:</p><p><em>table.column</em> &gt; <em>constant_value</em></p><p>Any comparison operator other than \"=\" expresses inequality. </p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>INCLUDE</p></td>     <td data-th=\"Description\"><p>Column is not used to evaluate a predicate, but is used for another reason, for example, to cover a query.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_missing_index_columns' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_missing_index_group_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_missing_index_group_stats",  "<p>Returns summary information about groups of missing indexes, excluding spatial indexes.</p><p>In Azure SQL Database, dynamic management views cannot expose information that would impact database containment or expose information about other databases the user has access to. To avoid exposing this information, every row that contains data that doesn?t belong to the connected tenant is filtered out.</p>");

			// Column names and description
			mtd.addColumn("dm_db_missing_index_group_stats", "group_handle"         , "<html><p>Identifies a group of missing indexes. This identifier is unique across the server. </p><p>The other columns provide information about all queries for which the index in the group is considered missing.</p><p>An index group contains only one index.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "unique_compiles"      , "<html><p>Number of compilations and recompilations that would benefit from this missing index group. Compilations and recompilations of many different queries can contribute to this column value.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "user_seeks"           , "<html><p>Number of seeks caused by user queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "user_scans"           , "<html><p>Number of scans caused by user queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "last_user_seek"       , "<html><p>Date and time of last seek caused by user queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "last_user_scan"       , "<html><p>Date and time of last scan caused by user queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "avg_total_user_cost"  , "<html><p>Average cost of the user queries that could be reduced by the index in the group.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "avg_user_impact"      , "<html><p>Average percentage benefit that user queries could experience if this missing index group was implemented. The value means that the query cost would on average drop by this percentage if this missing index group was implemented.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "system_seeks"         , "<html><p>Number of seeks caused by system queries, such as auto stats queries, that the recommended index in the group could have been used for. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms190721.aspx\">Auto Stats Event Class</a>.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "system_scans"         , "<html><p>Number of scans caused by system queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "last_system_seek"     , "<html><p>Date and time of last system seek caused by system queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "last_system_scan"     , "<html><p>Date and time of last system scan caused by system queries that the recommended index in the group could have been used for.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "avg_total_system_cost", "<html><p>Average cost of the system queries that could be reduced by the index in the group.</p></html>");
			mtd.addColumn("dm_db_missing_index_group_stats", "avg_system_impact"    , "<html><p>Average percentage benefit that system queries could experience if this missing index group was implemented. The value means that the query cost would on average drop by this percentage if this missing index group was implemented.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_missing_index_group_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_io_backup_tapes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_io_backup_tapes",  "<p>Returns the list of tape devices and the status of mount requests for backups. </p>");

			// Column names and description
			mtd.addColumn("dm_io_backup_tapes", "physical_device_name"   , "<html><p>Name of the actual physical device on which a backup can be taken. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "logical_device_name"    , "<html><p>User-specified name for the drive (from <strong>sys.backup_devices</strong>). NULL if no user-specified name is available. Is nullable. </p></html>");
			mtd.addColumn("dm_io_backup_tapes", "status"                 , "<html><p>Status of the tape: </p><p>1 = Open, available for use </p><p>2 = Mount pending </p><p>3 = In use </p><p>4 = Loading</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>While a tape is being loaded (<strong>status = 4</strong>), the media label is not read yet. Columns that copy media-label values, such as <strong>media_sequence_number</strong>, show anticipated values, which may differ from the actual values on the tape. After the label has been read, <strong>status</strong> changes to <strong>3</strong> (in use), and the media-label columns then reflect the actual tape that is loaded. </p></td>    </tr>   </tbody>  </table> </div></div><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "status_desc"            , "<html><p>Description of the tape status: </p><ul class=\"unordered\"> <li><p>AVAILABLE </p></li> <li><p>MOUNT PENDING </p></li> <li><p>IN USE </p></li> <li><p>LOADING MEDIA </p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "mount_request_time"     , "<html><p>Time at which mount was requested. NULL if no mount is pending (<strong>status</strong>?<strong>!=</strong>?<strong>2</strong>). Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "mount_expiration_time"  , "<html><p>Time at which mount request will expire (time-out). NULL if no mount is pending (<strong>status</strong>?<strong>!=</strong>?<strong>2</strong>). Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "database_name"          , "<html><p>Database that is to be backed up onto this device. Is nullable. </p></html>");
			mtd.addColumn("dm_io_backup_tapes", "spid"                   , "<html><p>Session ID. This identifies the user of the tape. Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "command"                , "<html><p>Command that performs the backup. Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "command_desc"           , "<html><p>Description of the command. Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "media_family_id"        , "<html><p>Index of media family (1...<em>n</em>), <em>n</em> is the number of media families in the media set. Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "media_set_name"         , "<html><p>Name of the media set (if any) as specified by the MEDIANAME option when the media set was created). Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "media_set_guid"         , "<html><p>Identifier that uniquely identifies the media set. Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "media_sequence_number"  , "<html><p>Index of volume within a media family (1...<em>n</em>). Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "tape_operation"         , "<html><p>Tape operation that is being performed: </p><p>1 = Read </p><p>2 = Format </p><p>3 = Init </p><p>4 = Append </p><p>Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "tape_operation_desc"    , "<html><p>Tape operation that is being performed: </p><ul class=\"unordered\"> <li><p>READ </p></li> <li><p>FORMAT </p></li> <li><p>INIT </p></li> <li><p>APPEND </p></li></ul><p>Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "mount_request_type"     , "<html><p>Type of the mount request: </p><p>1 = Specific tape. The tape identified by the <strong>media_*</strong> fields is required. </p><p>2 = Next media family. The next media family not yet restored is requested. This is used when restoring from fewer devices than there are media families. </p><p>3 = Continuation tape. The media family is being extended, and a continuation tape is requested. </p><p>Is nullable.</p></html>");
			mtd.addColumn("dm_io_backup_tapes", "mount_request_type_desc", "<html><p>Type of the mount request: </p><ul class=\"unordered\"> <li><p>SPECIFIC TAPE </p></li> <li><p>NEXT MEDIA FAMILY </p></li> <li><p>CONTINUATION VOLUME </p></li></ul><p>Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_io_backup_tapes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_io_pending_io_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_io_pending_io_requests",  "<p>Returns a row for each pending I/O request in SQL Server.</p>");

			// Column names and description
			mtd.addColumn("dm_io_pending_io_requests", "io_completion_request_address", "<html><p>Memory address of the IO request. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_type"                      , "<html><p>Type of pending I/O request. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_pending"                   , "<html><p>Indicates whether the I/O request is pending or has been completed by Windows. An I/O request can still be pending even when Windows has completed the request, but SQL Server has not yet performed a context switch in which it would process the I/O request and remove it from this list. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_completion_routine_address", "<html><p>Internal function to call when the I/O request is completed. Is nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_user_data_address"         , "<html><p>Internal use only. Is nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "scheduler_address"            , "<html><p>Scheduler on which this I/O request was issued. The I/O request will appear on the pending I/O list of the scheduler. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177526.aspx\">sys.dm_os_schedulers (Transact-SQL)</a>. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_handle"                    , "<html><p>File handle of the file that is used in the I/O request. Is nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_offset"                    , "<html><p>Offset of the I/O request. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_pending_io_requests", "io_pending_ms_ticks"          , "<html><p>Internal use only. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_io_pending_io_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_io_cluster_valid_path_names
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_io_cluster_valid_path_names",  "<p>Returns information on all valid shared disks, including clustered shared volumes, for a SQL Server failover cluster instance. If the instance is not clustered, an empty rowset is returned.</p>");

			// Column names and description
			mtd.addColumn("dm_io_cluster_valid_path_names", "path_name"               , "<html><p>Volume mount point or drive path that can be used as a root directory for database and log files. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_cluster_valid_path_names", "cluster_owner_node"      , "<html><p>Current owner of the drive. For cluster shared volumes (CSV), the owner is the node which is hosting the MetaData Server. Is not nullable.</p></html>");
			mtd.addColumn("dm_io_cluster_valid_path_names", "is_cluster_shared_volume", "<html><p>Returns 1 if the drive on which this path is located is a cluster shared volume; otherwise, returns 0.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_io_cluster_valid_path_names' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_io_cluster_shared_drives
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_io_cluster_shared_drives",  "<p>This view returns the drive name of each of the shared drives if the current server instance is a clustered server. If the current server instance is not a clustered instance it returns an empty rowset.</p>");

			// Column names and description
			mtd.addColumn("dm_io_cluster_shared_drives", "DriveName", "<html><p>The name of the drive (the drive letter) that represents an individual disk taking part in the cluster shared disk array. Column is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_io_cluster_shared_drives' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_io_virtual_file_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_io_virtual_file_stats",  "<p>Returns I/O statistics for data and log files. This dynamic management view replaces the <a href=\"https://msdn.microsoft.com/en-us/library/ms187309.aspx\">fn_virtualfilestats</a> function.</p>");

			// Column names and description
			mtd.addColumn("dm_io_virtual_file_stats", "database_id"             , "<html><p>ID of database.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "file_id"                 , "<html><p>ID of file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "sample_ms"               , "<html><p>Number of milliseconds since the computer was started. This column can be used to compare different outputs from this function.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "num_of_reads"            , "<html><p>Number of reads issued on the file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "num_of_bytes_read"       , "<html><p>Total number of bytes read on this file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "io_stall_read_ms"        , "<html><p>Total time, in milliseconds, that the users waited for reads issued on the file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "num_of_writes"           , "<html><p>Number of writes made on this file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "num_of_bytes_written"    , "<html><p>Total number of bytes written to the file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "io_stall_write_ms"       , "<html><p>Total time, in milliseconds, that users waited for writes to be completed on the file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "io_stall"                , "<html><p>Total time, in milliseconds, that users waited for I/O to be completed on the file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "size_on_disk_bytes"      , "<html><p>Number of bytes used on the disk for this file. For sparse files, this number is the actual number of bytes on the disk that are used for database snapshots.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "file_handle"             , "<html><p>Windows file handle for this file.</p></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "io_stall_queued_read_ms" , "<html><p>Total IO latency introduced by IO resource governance for reads. Is not nullable. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/bb934023.aspx\">sys.dm_resource_governor_resource_pools (Transact-SQL)</a>.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_io_virtual_file_stats", "io_stall_queued_write_ms", "<html><p>Total IO latency introduced by IO resource governance for writes. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_io_virtual_file_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_checkpoint_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_checkpoint_stats",  "<p>Returns statistics about the In-Memory OLTP checkpoint operations in the current database. If the database has no In-Memory OLTP objects, returns an empty result set.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "log_to_process_in_bytes"                     , "<html><p>The number of log bytes between the thread's current log sequence number (LSN) and the end-of-log.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "total_log_blocks_processed"                  , "<html><p>Total number of log blocks processed since server startup.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "total_log_records_processed"                 , "<html><p>Total number of log records processed since server startup.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "xtp_log_records_processed"                   , "<html><p>Total number of In-Memory OLTP log records processed since server startup.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "total_wait_time_in_ms"                       , "<html><p>Cumulative wait time in ms.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "waits_for_io"                                , "<html><p>Number of waits for log IO.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "io_wait_time_in_ms"                          , "<html><p>Cumulative time spent waiting on log IO.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "waits_for_new_log"                           , "<html><p>Number of waits for new log to be generated.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "new_log_wait_time_in_ms"                     , "<html><p>Cumulative time spend waiting on new log.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "log_generated_since_last_checkpoint_in_bytes", "<html><p>Amount of log generated since the last In-Memory OLTP checkpoint.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "ms_since_last_checkpoint"                    , "<html><p>Amount of time in milliseconds since the last In-Memory OLTP checkpoint.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "checkpoint_lsn"                              , "<html><p>The recovery log sequence number (LSN) associated with the last completed In-Memory OLTP checkpoint.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "current_lsn"                                 , "<html><p>The LSN of the log record that is currently processing.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "end_of_log_lsn"                              , "<html><p>The LSN of the end of the log.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_stats", "task_address"                                , "<html><p>The address of the SOS_Task. Join to sys.dm_os_tasks to find additional information.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_checkpoint_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_checkpoint_files
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_checkpoint_files",  "<p>Displays information about checkpoint files, including file size, physical location and the transaction ID.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_checkpoint_files", "container_id"                 , "<html><p>The ID of the container (represented as a file with type FILESTREAM in sys.database_files) that the data or delta file is part of. Joins with file_id in <a href=\"https://msdn.microsoft.com/en-us/library/ms174397.aspx\">sys.database_files (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "container_guid"               , "<html><p>The GUID of the container that the data or delta file is part of.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "checkpoint_file_id"           , "<html><p>ID of the data or delta file.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "relative_file_path"           , "<html><p>Path to the data or delta file, relative to the location of the container.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "file_type"                    , "<html><p>0 for data file.</p><p>1 for delta file.</p><p>NULL if the state column is set to 7.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "file_type_desc"               , "<html><p>The type of file: DATA_FILE, DELTA_FILE, or NULL if the state column is set to 7.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "internal_storage_slot"        , "<html><p>The index of the file in the internal storage array. NULL if the state column is not 2 or 3.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "checkpoint_pair_file_id"      , "<html><p>The corresponding data or delta file.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "file_size_in_bytes"           , "<html><p>Size of the file that is used. NULL if the state column is set to 5, 6, or 7.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "file_size_used_in_bytes"      , "<html><p>Used size of the file that is used. NULL if the state column is set to 5, 6, or 7.</p><p>For checkpoint file pairs that are still being populated, this column will be updated after the next checkpoint.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "inserted_row_count"           , "<html><p>Number of rows in the data file.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "deleted_row_count"            , "<html><p>Number of deleted rows in the delta file.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "drop_table_deleted_row_count" , "<html><p>The number of rows in the data files affected by a drop table. Applies to data files when the state column equals 1.</p><p>Shows deleted row counts from dropped table(s). The drop_table_deleted_row_count statistics are compiled after the memory garbage collection of the rows from dropped table(s) is complete and a checkpoint is taken. If you restart SQL Server before the drop tables statistics are reflected in this column, the statistics will be updated as part of recovery. The recovery process does not load rows from dropped tables. Statistics for dropped tables are compiled during the load phase and reported in this column when recovery completes.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "state"                        , "<html><p>0 ? PRECREATED</p><p>1 ? UNDER CONSTRUCTION</p><p>2 - ACTIVE</p><p>3 ? MERGE TARGET</p><p>4 ? MERGED SOURCE</p><p>5 ? REQUIRED FOR BACKUP/HA</p><p>6 ? IN TRANSITION TO TOMBSTONE</p><p>7 ? TOMBSTONE</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "state_desc"                   , "<html><ul class=\"unordered\"> <li><p>PRECREATED ? A small set of data and delta file pairs, also known as checkpoint file pairs (CFPs) are kept pre-allocated to minimize or eliminate any waits to allocate new files as transactions are being executed. These are full sized with data file size of 128MB and delta file size of 8 MB but contain no data. The number of CFPs is computed as the number of logical processors or schedulers (one per core, no maximum) with a minimum of 8. This is a fixed storage overhead in databases with memory-optimized tables.</p></li> <li><p>UNDER CONSTRUCTION ? Set of CFPs that store newly inserted and possibly deleted data rows since the last checkpoint.</p></li> <li><p>ACTIVE - These contain the inserted and deleted rows from previous closed checkpoints. These CFPs contain all required inserted and deleted rows required before applying the active part of the transaction log at the database restart. The size of these CFPs will be approximately 2 times the in-memory size of memory-optimized tables, assuming the merge operation is current with the transactional workload.</p></li> <li><p>MERGE TARGET ? The CFP stores the consolidated data rows from the CFP(s) that were identified by the merge policy. Once the merge is installed, the MERGE TARGET transitions into ACTIVE state.</p></li> <li><p>MERGED SOURCE ? Once the merge operation is installed, the source CFPs are marked as MERGED SOURCE. Note, the merge policy evaluator may identify multiple merges but a CFP can only participate in one merge operation.</p></li> <li><p>REQUIRED FOR BACKUP/HA ? Once the merge has been installed and the MERGE TARGET CFP is part of durable checkpoint, the merge source CFPs transition into this state. CFPs in this state are needed for operational correctness of the database with memory-optimized table. For example, to recover from a durable checkpoint to go back in time. A CFP can be marked for garbage collection once the log truncation point moves beyond its transaction range.</p></li> <li><p>IN TRANSITION TO TOMBSTONE ? These CFPs are not needed by the In-Memory OLTP engine and can they can be garbage collected. This state indicates that these CFPs are waiting for the background thread to transition them to the next state, which is TOMBSTONE.</p></li> <li><p>TOMBSTONE ? These CFPs are waiting to be garbage collected by the filestream garbage collector. (<a href=\"https://msdn.microsoft.com/en-us/library/gg492195.aspx\">sp_filestream_force_garbage_collection (Transact-SQL)</a>)</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "lower_bound_tsn"              , "<html><p>The lower bound of transactions contained in the file. Null if the state column is other than 2, 3, or 4.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "upper_bound_tsn"              , "<html><p>The upper bound of transactions contained in the file. Null if the state column is other than 2, 3, or 4.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "last_backup_page_count"       , "<html><p>Logical page count that is determined at last backup. Applies when the state column is set to 2, 3, 4, or 5. NULL if page count not known.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "delta_watermark_tsn"          , "<html><p>The transaction of the last checkpoint that wrote to this delta file. This is the watermark for the delta file.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "last_checkpoint_recovery_lsn" , "<html><p>Recovery log sequence number of the last checkpoint that still needs the file.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "tombstone_operation_lsn"      , "<html><p>The file will be deleted once the tombstone_operation_lsn falls behind the log truncation log sequence number.</p></html>");
			mtd.addColumn("dm_db_xtp_checkpoint_files", "logical_deletion_log_block_id", "<html><p>Applies only to state 5.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_checkpoint_files' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_gc_cycle_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_gc_cycle_stats",  "<p>Outputs the current state of committed transactions that have deleted one or more rows. The idle garbage collection thread wakes every minute or when the number of committed DML transactions exceeds an internal threshold since the last garbage collection cycle. As part of the garbage collection cycle, it moves the transactions that have committed into one or more queues associated with generations. The transactions that have generated stale versions are grouped in a unit of 16 transactions across 16 generations as follows:</p><ul class=\"unordered\"> <li><p>Generation-0: This stores all transactions that committed earlier than the oldest active transaction. Row versions generated by these transactions are immediately available for garbage collection.</p></li> <li><p>Generations 1-14: Stores transactions with timestamp greater than the oldest active transaction. The row versions cannot be garbage collected. Each generation can hold up to 16 transactions. A total of 224 (14 * 16) transactions can exist in these generations.</p></li> <li><p>Generation 15: The remaining transactions with timestamp greater than the oldest active transaction go to generation 15. Similar to generation-0, there is no limit of number of transactions in generation-15.</p></li></ul><p>When there is memory pressure, the garbage collection thread updates the oldest active transaction hint aggressively, which forces garbage collection.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_gc_cycle_stats", "cycle_id"                       , "<html><p>A unique identifier for the garbage collection cycle.</p></html>");
			mtd.addColumn("dm_db_xtp_gc_cycle_stats", "ticks_at_cycle_start"           , "<html><p>Ticks at the time the cycle started.</p></html>");
			mtd.addColumn("dm_db_xtp_gc_cycle_stats", "ticks_at_cycle_end"             , "<html><p>Ticks at the time the cycle ended.</p></html>");
			mtd.addColumn("dm_db_xtp_gc_cycle_stats", "base_generation"                , "<html><p>The current base generation value in the database. This represents the timestamp of the oldest active transaction used to identify transactions for garbage collection. The oldest active transaction id is updated in the increment of 16. For example, if you have transaction ids as 124, 125, 126 ? 139, the value will be 124. When you add another transaction, for example 140, the value will be 140.</p></html>");
			mtd.addColumn("dm_db_xtp_gc_cycle_stats", "xacts_copied_to_local"          , "<html><p>The number of transactions copied from the transaction pipeline into the database's generation array.</p></html>");
			mtd.addColumn("dm_db_xtp_gc_cycle_stats", "xacts_in_gen_0- xacts_in_gen_15", "<html><p>Number of transactions in each generation.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_gc_cycle_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_hash_index_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_hash_index_stats",  "<p>These statistics are useful for understanding and tuning the bucket counts. It can also be used to detect cases where the index key has many duplicates.</p><p>A large average chain length indicates that many rows are hashed to the same bucket. This could happen because:</p><ul class=\"unordered\"> <li><p>If the number of empty buckets is low or the average and maximum chain lengths are similar, it is likely that the total bucket count is too low. This causes many different index keys to hash to the same bucket.</p></li> <li><p>If the number of empty buckets is high or the maximum chain length is high relative to the average chain length, it is likely that there are many rows with duplicate index key values or there is a skew in the key values. All rows with the same index key value hash to the same bucket, hence there is a long chain length in that bucket.</p></li></ul><p>Long chain lengths can significantly impact the performance of all DML operations on individual rows, including SELECT and INSERT. Short chain lengths along with a high empty bucket count are in indication of a bucket_count that is too high. This decreases the performance of index scans.</p><p><strong>sys.dm_db_xtp_hash_index_stats</strong> scans the entire table. So, if there are large tables in your database, <strong>sys.dm_db_xtp_hash_index_stats</strong> may take a long time run.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_hash_index_stats", "object_id"         , "<html><p>The object ID of parent table.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "xtp_object_id"     , "<html><p>ID of the memory-optimized table.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "index_id"          , "<html><p>The index ID.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "total_bucket_count", "<html><p>The total number of hash buckets in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "empty_bucket_count", "<html><p>The number of empty hash buckets in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "avg_chain_length"  , "<html><p>The average length of the row chains over all the hash buckets in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "max_chain_length"  , "<html><p>The maximum length of the row chains in the hash buckets.</p></html>");
			mtd.addColumn("dm_db_xtp_hash_index_stats", "xtp_object_id"     , "<html><p>The in-memory OLTP object ID that corresponds to the memory-optimized table.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_hash_index_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_index_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_index_stats",  "<p>Contains statistics collected since the last database restart.</p><p>For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/dn133186.aspx\">In-Memory OLTP (In-Memory Optimization)</a> and <a href=\"https://msdn.microsoft.com/en-us/library/dn133166.aspx\">Guidelines for Using Indexes on Memory-Optimized Tables</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_index_stats", "object_id"                               , "<html><p>ID of the object to which this index belongs. </p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "xtp_object_id"                           , "<html><p>Internal ID corresponding to the current version of the object.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Applies to SQL Server 2016 Community Technology Preview 2 (CTP2).</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "index_id"                                , "<html><p>ID of the index. The index_id is unique only within the object.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "scans_started"                           , "<html><p>Number of In-Memory OLTP index scans performed. Every select, insert, update, or delete requires an index scan.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "scans_retries"                           , "<html><p>Number of index scans that needed to be retried,</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "rows_returned"                           , "<html><p>Cumulative number of rows returned since the table was created or the start of SQL Server.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "rows_touched"                            , "<html><p>Cumulative number of rows accessed since the table was created or the start of SQL Server.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "rows_expiring"                           , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "rows_expired"                            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "rows_expired_removed"                    , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phantom_scans_started"                   , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phatom_scans_retries"                    , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phantom_rows_touched"                    , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phantom_expiring_rows_encountered"       , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phantom_expired_rows_encountered"        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phantom_expired_removed_rows_encountered", "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "phantom_expired_rows_removed"            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_index_stats", "object_address"                          , "<html><p>Internal use only.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_index_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_memory_consumers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_memory_consumers",  "<p>Reports the database-level memory consumers in the In-Memory OLTP database engine. The view returns a row for each memory consumer that the database engine uses. Use this DMV to see how the memory is distributed across different internal objects.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_memory_consumers", "memory_consumer_id"       , "<html><p>ID (internal) of the memory consumer.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "memory_consumer_type"     , "<html><p>The type of memory consumer:</p><ul class=\"unordered\"> <li><p>0=Aggregation. (Aggregates memory usage of two or more consumers. It should not be displayed.)</p></li> <li><p>2=VARHEAP (Tracks memory consumption for a variable-length heap.)</p></li> <li><p>3=HASH (Tracks memory consumption for an index.)</p></li> <li><p>5=DB page pool (Tracks memory consumption for a database page pool used for runtime operations. For example, table variables and some serializable scans. There is only one memory consumer of this type per database.)</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "memory_consumer_type_desc", "<html><p>Type of memory consumer: VARHEAP, HASH, or PGPOOL.</p><ul class=\"unordered\"> <li><p>0 ? (It should not be displayed.)</p></li> <li><p>2 - VARHEAP</p></li> <li><p>3 - HASH</p></li> <li><p>5 - PGPOOL</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "memory_consumer_desc"     , "<html><p>Description of the memory consumer instance:</p><ul class=\"unordered\"> <li><p>VARHEAP</p><p>Database heap. Used to allocate user data for a database (rows).</p><p>Database System heap. Used to allocate database data that will be included in memory dumps and do not include user data.</p><p>Range index heap. Private heap used by range index to allocate BW pages.</p></li> <li><p>HASH</p><p>No description since the object_id indicates the table and the index_id the hash index itself.</p></li> <li><p>PGPOOL</p><p>For the database there is only one page pool Database 64K page pool.</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "object_id"                , "<html><p>The object ID to which the allocated memory is attributed. A negative value for system objects.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "xtp_object_id"            , "<html><p>The object ID for the memory-optimized table.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "index_id"                 , "<html><p>The index ID of the consumer (if any). NULL for base tables.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "allocated_bytes"          , "<html><p>Number of bytes reserved for this consumer.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "used_bytes"               , "<html><p>Bytes used by this consumer. Applies only to varheap.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "allocation_count"         , "<html><p>Number of allocations.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "partition_count"          , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "sizeclass_count"          , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "min_sizeclass"            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "max_sizeclass"            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "memory_consumer_address"  , "<html><p>Internal address of the consumer. For internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_memory_consumers", "xtp_object_id"            , "<html><p>The in-memory OLTP object ID that corresponds to the memory-optimized table.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_memory_consumers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_merge_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_merge_requests",  "<p>Tracks database merge requests. The merge request may have been generated by SQL Server or the request could have been made by a user with <a href=\"https://msdn.microsoft.com/en-us/library/dn198330.aspx\">sys.sp_xtp_merge_checkpoint_files (Transact-SQL)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_merge_requests", "request_state"       , "<html><p>Status of the merge request:</p><ul class=\"nobullet\"> <li><p>0 = requested</p></li> <li><p>1 = pending</p></li> <li><p>2= installed</p></li> <li><p>3 = abandoned</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "request_state_desc"  , "<html><p>The current state of the request.</p><ul class=\"nobullet\"> <li><p><strong>Requested</strong> means that a merge request exists.</p></li> <li><p><strong>Pending</strong> means that the merge is being processing.</p></li> <li><p><strong>Installed</strong> means that the merge is complete.</p></li> <li><p><strong>Abandoned</strong> means that the merge could not complete, perhaps due to lack of storage.</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "destination_file_id" , "<html><p>The unique identifier of the destination file for the merge of the Source files.</p></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "lower_bound_tsn"     , "<html><p>The minimum timestamp for the target merge file. The lowest transaction timestamp of all the source files to be merged.</p></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "upper_bound_tsn"     , "<html><p>The maximum timestamp for the target merge file. The highest transaction timestamp of all the source files to be merged.</p></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "collection_tsn"      , "<html><p>The timestamp at which the current row can be collected.</p><p>A row in the Installed state is removed when checkpoint_tsn is greater than collection_tsn.</p><p>A row in the Abandoned state is removed when checkpoint_tsn is less than collection_tsn.</p></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "checkpoint_tsn"      , "<html><p>The time that the checkpoint started.</p><p>Any deletes done by transactions with a timestamp lower than this are accounted for in the new data file. The remaining deletes are moved to the target delta file.</p></html>");
			mtd.addColumn("dm_db_xtp_merge_requests", "sourcenumber_file_id", "<html><p>Up to 16 internal file ids that uniquely identify the source files in the merge.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_merge_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_object_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_object_stats",  "<p>Reports the number rows affected by operations on each of the In-Memory OLTP objects since the last database restart. Statistics are updated when the operation executes, regardless of whether the transaction commits or was rolled back.</p><p>sys.dm_db_xtp_object_stats can help you identify which memory-optimized tables are changing the most. You may decide to remove unused or rarely used indexes on the table, as each index affects performance. If there are hash indexes, you should periodically re-evaluate the bucket-count. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/dn494956.aspx\">Determining the Correct Bucket Count for Hash Indexes</a>.</p><p>sys.dm_db_xtp_object_stats can help you identify which memory-optimized tables incur write-write conflicts, which can affect the performance of your application. For example, if you have transaction retry logic, the same statement may need to be executed more than once. Also, you can use this information to identify the tables (and therefore business logic) that require write-write error handling.</p><p>The view contains a row for each memory optimized table in the database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_object_stats", "object_id"                   , "<html><p>The ID of the object.</p></html>");
			mtd.addColumn("dm_db_xtp_object_stats", "row_insert_attempts"         , "<html><p>The number of rows inserted into the table since the last database restart by both committed and aborted transactions.</p></html>");
			mtd.addColumn("dm_db_xtp_object_stats", "row_update_attempts"         , "<html><p>The number of rows updated in the table since the last database restart by both committed and aborted transactions.</p></html>");
			mtd.addColumn("dm_db_xtp_object_stats", "row_delete_attempts"         , "<html><p>The number of rows deleted from the table since the last database restart by both committed and aborted transactions.</p></html>");
			mtd.addColumn("dm_db_xtp_object_stats", "write_conflicts"             , "<html><p>The number of write conflicts that occurred since the last database restart.</p></html>");
			mtd.addColumn("dm_db_xtp_object_stats", "unique_constraint_violations", "<html><p>The number of unique constraint violations that have occurred since the last database restart.</p></html>");
			mtd.addColumn("dm_db_xtp_object_stats", "object_address"              , "<html><p>Internal use only.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_object_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_nonclustered_index_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_nonclustered_index_stats",  "<p>sys.dm_db_xtp_nonclustered_index_stats includes statistics about operations on nonclustered indexes in memory-optimized tables. sys.dm_db_xtp_nonclustered_index_stats contains one row for each nonclustered index on a memory-optimized table in the current database.</p><p>The statistics reflected in sys.dm_db_xtp_nonclustered_index_stats are collected when the in-memory index structure is created. In-memory index structures are recreated on database restart.</p><p>Use sys.dm_db_xtp_nonclustered_index_stats to understand and monitor index activity during DML operations and when a database is brought online. When a database with a memory-optimized table is restarted, the index is built by inserting one row at a time into memory. The count of page splits, merges, and consolidation can help you understand the work done to build the index when a database is brought online. You can also look at these counts before and after a series of DML operations.</p><p>Large numbers of retries are indicative of concurrency issues; call Microsoft Support.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "object_id"                     , "<html><p>ID of the object.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "xtp_object_id"                 , "<html><p>ID of the memory-optimized table.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "index_id"                      , "<html><p>ID of the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "delta_pages"                   , "<html><p>The total number of delta pages for this index in the tree.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "internal_pages"                , "<html><p>For internal use. The total number of internal pages for this index in the tree.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "leaf_pages"                    , "<html><p>The total number of leaf pages for this index in the tree.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "outstanding_retired_nodes"     , "<html><p>For internal use. The total number of nodes for this index in the internal structures.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_update_count"             , "<html><p>Cumulative number of operations updating a page in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_update_retry_count"       , "<html><p>Cumulative number of retries of an operation updating page in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_consolidation_count"      , "<html><p>Cumulative number of page consolidations in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_consolidation_retry_count", "<html><p>Cumulative number of retries of page consolidation operations.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_split_count"              , "<html><p>Cumulative number of page split operations in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_split_retry_count"        , "<html><p>Cumulative number of retries of page split operations.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "key_split_count"               , "<html><p>Cumulative number of key splits in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "key_split_retry_count"         , "<html><p>Cumulative number of retries of key split operations.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_merge_count"              , "<html><p>Cumulative number of page merge operations in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "page_merge_retry_count"        , "<html><p>Cumulative number of retries of page merge operations.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "key_merge_count"               , "<html><p>Cumulative number of key merge operations in the index.</p></html>");
			mtd.addColumn("dm_db_xtp_nonclustered_index_stats", "key_merge_retry_count"         , "<html><p>Cumulative number of retries of key merge operations.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_nonclustered_index_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_table_memory_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_table_memory_stats",  "<p>Returns memory usage statistics for each In-Memory OLTP table (user and system) in the current database. The system tables have negative object IDs and are used to store run-time information for the In-Memory OLTP engine. Unlike user objects, system tables are internal and only exist in-memory, therefore, they are not visible through catalog views. System tables are used to store information such as meta-data for all data/delta files in storage, merge requests, watermarks for delta files to filter rows, dropped tables, and relevant information for recovery and backups. Given that the In-Memory OLTP engine can have up to 8,192 data and delta file pairs, for large in-memory databases, the memory taken by system tables can be a few megabytes.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_table_memory_stats", "object_id"                      , "<html><p>The object ID of the table. NULL for In-Memory OLTP system tables.</p></html>");
			mtd.addColumn("dm_db_xtp_table_memory_stats", "memory_allocated_for_table_kb"  , "<html><p>Memory allocated for this table.</p></html>");
			mtd.addColumn("dm_db_xtp_table_memory_stats", "memory_used_by_table_kb"        , "<html><p>Memory used by table, including row versions.</p></html>");
			mtd.addColumn("dm_db_xtp_table_memory_stats", "memory_allocated_for_indexes_kb", "<html><p>Memory allocated for indexes on this table.</p></html>");
			mtd.addColumn("dm_db_xtp_table_memory_stats", "memory_used_by_indexes_kb"      , "<html><p>Memory consumed for indexes on this table.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_table_memory_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_xtp_transactions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_xtp_transactions",  "<p>Reports the active transactions in the In-Memory OLTP database engine.</p>");

			// Column names and description
			mtd.addColumn("dm_db_xtp_transactions", "xtp_transaction_id"                   , "<html><p>Internal ID for this transaction in the XTP transaction manager.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "transaction_id"                       , "<html><p>The transaction ID. Joins with the transaction ID in other transaction-related DMVs, such as sys.dm_tran_active_transactions.</p><p>0 for XTP-only transactions, such as transactions started by natively compiled stored procedures.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "session_id"                           , "<html><p>The session identifier of the session that is executing this transaction. Joins with sys.dm_exec_sessions.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "begin_tsn"                            , "<html><p>Begin transaction serial number of the transaction.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "end_tsn"                              , "<html><p>End transaction serial number of the transaction.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "state"                                , "<html><p>The state of the transaction:</p><ul class=\"unordered\"> <li><p>0=ACTIVE</p></li> <li><p>1=COMMITTED</p></li> <li><p>2=ABORTED</p></li> <li><p>3=VALIDATING</p></li></ul></html>");
			mtd.addColumn("dm_db_xtp_transactions", "state_desc"                           , "<html><p>The description of the transaction state.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "result"                               , "<html><p>The result of this transaction. The following are the possible values.</p><p>0 - IN PROGRESS</p><p>1 - SUCCESS</p><p>2 - ERROR</p><p>3 - COMMIT DEPENDENCY</p><p>4 - VALIDATION FAILED (RR)</p><p>5 - VALIDATION FAILED (SR)</p><p>6 - ROLLBACK</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "result_desc"                          , "<html><p>The result of this transaction. The following are the possible values.</p><p>IN PROGRESS</p><p>SUCCESS</p><p>ERROR</p><p>COMMIT DEPENDENCY</p><p>VALIDATION FAILED (RR)</p><p>VALIDATION FAILED (SR)</p><p>ROLLBACK</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "last_error"                           , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "is_speculative"                       , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "is_prepared"                          , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "is_delayed_durability"                , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "memory_address"                       , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "database_address"                     , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "thread_id"                            , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "read_set_row_count"                   , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "write_set_row_count"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "scan_set_count"                       , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "savepoint_garbage_count"              , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "log_bytes_required"                   , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "count_of_allocations"                 , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "allocated_bytes"                      , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "reserved_bytes"                       , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "commit_dependency_count"              , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "commit_dependency_total_attempt_count", "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "scan_area"                            , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "scan_area_desc"                       , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "scan_location"                        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_1_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_2_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_3_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_4_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_5_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_6_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_7_address"                  , "<html><p>Internal use only</p></html>");
			mtd.addColumn("dm_db_xtp_transactions", "dependent_8_address"                  , "<html><p>Internal use only</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_xtp_transactions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xtp_gc_queue_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xtp_gc_queue_stats",  "<p>Outputs information about each garbage collection worker queue on the server, and various statistics about each. There is one queue per logical CPU.</p><p>The main garbage collection thread (the Idle thread) tracks updated, deleted, and inserted rows for all transactions completed since the last invocation of the main garbage collection thread. When the garbage collection thread wakes, it determines if the timestamp of the oldest active transaction has changed. If the oldest active transaction has changed, then the idle thread enqueues work items (in chunks of 16 rows) for transactions whose write sets are no longer needed. For example, if you delete 1,024 rows, you will eventually see 64 garbage collection work items queued, each containing 16 deleted rows. After a user transaction commits, it selects all enqueued items on its scheduler. If there are no enqueued items on its scheduler, the user transaction will search on any queue in the current NUMA node.</p><p>You can determine if garbage collection is freeing memory for deleted rows by executing sys.dm_xtp_gc_queue_stats to see if the enqueued work is being processed. If entries in the current_queue_depth are not being processed or if no new items work items are being added to the current_queue_length, this is an indication that garbage collection is not freeing memory. For example, garbage collection can?t be done if there is a long running transaction.</p>");

			// Column names and description
			mtd.addColumn("dm_xtp_gc_queue_stats", "queue_id"           , "<html><p>The unique identifier of the queue.</p></html>");
			mtd.addColumn("dm_xtp_gc_queue_stats", "total_enqueues"     , "<html><p>The total number of garbage collection work items enqueued to this queue since the server started.</p></html>");
			mtd.addColumn("dm_xtp_gc_queue_stats", "total_dequeues"     , "<html><p>The total number of garbage collection work items dequeued from this queue since the server started.</p></html>");
			mtd.addColumn("dm_xtp_gc_queue_stats", "current_queue_depth", "<html><p>The current number of garbage collection work items present on this queue. This item may imply one or more to be garbage collected.</p></html>");
			mtd.addColumn("dm_xtp_gc_queue_stats", "maximum_queue_depth", "<html><p>The maximum depth this queue has seen.</p></html>");
			mtd.addColumn("dm_xtp_gc_queue_stats", "last_service_ticks" , "<html><p>CPU ticks at the time the queue was last serviced.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xtp_gc_queue_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xtp_gc_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xtp_gc_stats",  "<p>Provides information (the overall statistics) about the current behavior of the In-Memory OLTP garbage-collection process.</p><p>Rows are garbage collected as part of regular transaction processing, or by the main garbage collection thread, which is referred to as the idle worker. When a user transaction commits, it dequeues one work item from the garbage collection queue (<a href=\"https://msdn.microsoft.com/en-us/library/dn268336.aspx\">sys.dm_xtp_gc_queue_stats (Transact-SQL)</a>). Any rows that could be garbage collected but were not accessed by main user transaction are garbage collected by the idle worker, as part of the dusty corner scan (a scan for areas of the index that are less accessed).</p>");

			// Column names and description
			mtd.addColumn("dm_xtp_gc_stats", "rows_examined"               , "<html><p>The number of rows examined by the garbage collection subsystem since the server was started.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "rows_no_sweep_needed"        , "<html><p>The number of rows that were removed without a dusty corner scan.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "rows_first_in_bucket"        , "<html><p>The number of rows examined by garbage collection that were the first row in the hash bucket.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "rows_first_in_bucket_removed", "<html><p>The number of rows examined by garbage collection that were the first row in the hash bucket that have been removed.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "rows_marked_for_unlink"      , "<html><p>The number of rows examined by garbage collection that were already marked as unlinked in their indexes with ref count =0.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "parallel_assist_count"       , "<html><p>The number of rows processed by user transactions.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "idle_worker_count"           , "<html><p>The number of garbage rows processed by the idle worker.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "sweep_scans_started"         , "<html><p>The number of dusty corner scans performed by garbage collection subsystem.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "sweep_scans_retries"         , "<html><p>The number of dusty corner scans performed by the garbage collection subsystem.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "sweep_rows_touched"          , "<html><p>Rows read by dusty corner processing.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "sweep_rows_expiring"         , "<html><p>Expiring rows read by dusty corner processing.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "sweep_rows_expired"          , "<html><p>Expired rows read by dusty corner processing.</p></html>");
			mtd.addColumn("dm_xtp_gc_stats", "sweep_rows_expired_removed"  , "<html><p>Expired rows removed by dusty corner processing.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xtp_gc_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xtp_system_memory_consumers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xtp_system_memory_consumers",  "<p>Reports system level memory consumers for In-Memory OLTP. The memory for these consumers come either from the default pool (when the allocation is in the context of a user thread) or from internal pool (if the allocation is in the context of a system thread).</p>");

			// Column names and description
			mtd.addColumn("dm_xtp_system_memory_consumers", "memory_consumer_id"       , "<html><p>Internal ID for memory consumer.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "memory_consumer_type"     , "<html><p>An integer that represents the type of the memory consumer.</p><ul class=\"unordered\"> <li><p>0 ? It should not be displayed.</p><p>Aggregates memory usage of two or more consumers.</p></li> <li><p>1 ? LOOKASIDE</p><p>Tracks memory consumption for a system lookaside.</p></li> <li><p>2 - VARHEAP</p><p>Tracks memory consumption for a variable-length heap.</p></li> <li><p>4 - IO page pool</p><p>Tracks memory consumption for a system page pool used for IO operations.</p></li></ul></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "memory_consumer_type_desc", "<html><p>The description of the type of memory consumer:</p><ul class=\"unordered\"> <li><p>0 ? It should not be displayed.</p></li> <li><p>1 ? LOOKASIDE</p></li> <li><p>2 - VARHEAP</p></li> <li><p>4 - PGPOOL</p></li></ul></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "memory_consumer_desc"     , "<html><p>Description of the memory consumer instance:</p><ul class=\"unordered\"> <li><p>VARHEAP</p><p>System heap. General purpose. Currently only used to allocate garbage collection work items.</p><p>Lookaside heap. Used by looksides when the number of items contained in the lookaside list reaches a predetermined cap (usually around 5,000 items).</p></li> <li><p>PGPOOL</p><p>For IO system pools there are three different sizes System 4K page pool, System 64K page pool, and System 256K page pool.</p></li></ul></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "lookaside_id"             , "<html><p>The ID of the thread-local, lookaside memory provider.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "pagepool_id"              , "<html><p>The ID of the thread-local, page pool memory provider.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "allocated_bytes"          , "<html><p>Number of bytes reserved for this consumer.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "used_bytes"               , "<html><p>Bytes used by this consumer. Applies only to varheap memory consumers.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "allocation_count"         , "<html><p>Number of allocations.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "partition_count"          , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "sizeclass_count"          , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "min_sizeclass"            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "max_sizeclass"            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_system_memory_consumers", "memory_consumer_address"  , "<html><p>Internal address of the consumer.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xtp_system_memory_consumers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_xtp_transaction_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_xtp_transaction_stats",  "<p>Reports statistics about transactions that have run since the server started.</p>");

			// Column names and description
			mtd.addColumn("dm_xtp_transaction_stats", "total_count"                 , "<html><p>The total number of transactions that have run in the In-Memory OLTP database engine.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "read_only_count"             , "<html><p>The number of read-only transactions.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "total_aborts"                , "<html><p>Total number of transactions that were aborted, either through user or system abort.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "user_aborts"                 , "<html><p>Number of aborts initiated by the system. For example, because of write conflicts, validation failures, or dependency failures.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "validation_failures"         , "<html><p>The number of times a transaction has aborted due to a validation failure.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "dependencies_taken"          , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "dependencies_failed"         , "<html><p>The number of times a transaction aborts because a transaction on which it was dependent aborts.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "savepoint_create"            , "<html><p>The number of savepoints created. A new savepoint is created for every atomic block.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "savepoint_rollbacks"         , "<html><p>The number of rollbacks to a previous savepoint.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "savepoint_refreshes"         , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "log_bytes_written"           , "<html><p>Total number of bytes written to the In-Memory OLTP log records.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "log_IO_count"                , "<html><p>Total number of transactions that require log IO. Only considers transactions on durable tables.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "phantom_scans_started"       , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "phatom_scans_retries"        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "phantom_rows_touched"        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "phantom_rows_expiring"       , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "phantom_rows_expired"        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "phantom_rows_expired_removed", "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "scans_started"               , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "scans_retried"               , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_returned"               , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_touched"                , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_expiring"               , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_expired"                , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_expired_removed"        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_inserted"               , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_updated"                , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "rows_deleted"                , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "write_conflicts"             , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_xtp_transaction_stats", "unique_constraint_violations", "<html><p>Total number of unique constraint violations.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_xtp_transaction_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_sql_referenced_entities
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_sql_referenced_entities",  "<p>Returns one row for each user-defined entity referenced by name in the definition of the specified referencing entity in SQL Server. A dependency between two entities is created when one user-defined entity, called the <em>referenced entity</em>, appears by name in a persisted SQL expression of another user-defined entity, called the <em>referencing entity</em>. For example, if a stored procedure is the specified referencing entity, this function returns all user-defined entities that are referenced in the stored procedure such as tables, views, user-defined types (UDTs), or other stored procedures.</p><p>You can use this dynamic management function to report on the following types of entities referenced by the specified referencing entity:</p><ul class=\"unordered\"> <li><p>Schema-bound entities</p></li> <li><p>Non-schema-bound entities</p></li> <li><p>Cross-database and cross-server entities</p></li> <li><p>Column-level dependencies on schema-bound and non-schema-bound entities</p></li> <li><p>User-defined types (alias and CLR UDT)</p></li> <li><p>XML schema collections</p></li> <li><p>Partition functions</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_sql_referenced_entities", "referencing_minor_id"    , "<html><p>Column ID when the referencing entity is a column; otherwise 0. Is not nullable.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_server_name"  , "<html><p>Name of the server of the referenced entity.</p><p>This column is populated for cross-server dependencies that are made by specifying a valid four-part name. For information about multipart names, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177563.aspx\">Transact-SQL Syntax Conventions (Transact-SQL)</a>.</p><p>NULL for non-schema-bound dependencies for which the entity was referenced without specifying a four-part name.</p><p>NULL for schema-bound entities because they must be in the same database and therefore can only be defined using a two-part (<em>schema.object</em>) name.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_database_name", "<html><p>Name of the database of the referenced entity.</p><p>This column is populated for cross-database or cross-server references that are made by specifying a valid three-part or four-part name.</p><p>NULL for non-schema-bound references when specified using a one-part or two-part name.</p><p>NULL for schema-bound entities because they must be in the same database and therefore can only be defined using a two-part (<em>schema.object</em>) name.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_schema_name"  , "<html><p>Schema in which the referenced entity belongs. </p><p>NULL for non-schema-bound references in which the entity was referenced without specifying the schema name.</p><p>Never NULL for schema-bound references. </p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_entity_name"  , "<html><p>Name of the referenced entity. Is not nullable.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_minor_name"   , "<html><p>Column name when the referenced entity is a column; otherwise NULL. For example, <span class=\"literal\">referenced_minor_name</span> is NULL in the row that lists the referenced entity itself.</p><p>A referenced entity is a column when a column is identified by name in the referencing entity, or when the parent entity is used in a SELECT * statement. </p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_id"           , "<html><p>ID of the referenced entity. When <span class=\"literal\">referenced_minor_id</span> is not 0, <span class=\"literal\">referenced_id</span> is the entity in which the column is defined.</p><p>Always NULL for cross-server references.</p><p>NULL for cross-database references when the ID cannot be determined because the database is offline or the entity cannot be bound.</p><p>NULL for references within the database if the ID cannot be determined. For non-schema-bound references, the ID cannot be resolved in the following cases:</p><ul class=\"unordered\"> <li><p>The referenced entity does not exist in the database.</p></li> <li><p>Name resolution is caller dependent. In this case, <span class=\"literal\">is_caller_dependent</span> is set to 1.</p></li></ul><p>Never NULL for schema-bound references.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_minor_id"     , "<html><p>Column ID when the referenced entity is a column; otherwise, 0. For example, <span class=\"literal\">referenced_minor_is</span> is 0 in the row that lists the referenced entity itself.</p><p>For non-schema-bound references, column dependencies are reported only when all referenced entities can be bound. If any referenced entity cannot be bound, no column-level dependencies are reported and <span class=\"literal\">referenced_minor_id</span> is 0. See Example D.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_class"        , "<html><p>Class of the referenced entity.</p><p>1 = Object or column</p><p>6 = Type</p><p>10 = XML schema collection</p><p>21 = Partition function</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "referenced_class_desc"   , "<html><p>Description of class of referenced entity.</p><p>OBJECT_OR_COLUMN</p><p>TYPE</p><p>XML_SCHEMA_COLLECTION</p><p>PARTITION_FUNCTION</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "is_caller_dependent"     , "<html><p>Indicates schema binding for the referenced entity occurs at run time; therefore, resolution of the entity ID depends on the schema of the caller. This occurs when the referenced entity is a stored procedure, extended stored procedure, or user-defined function called within an EXECUTE statement. </p><p>1 = The referenced entity is caller dependent and is resolved at run time. In this case, <span class=\"literal\">referenced_id</span> is NULL. </p><p>0 = The referenced entity ID is not caller dependent. Always 0 for schema-bound references and for cross-database and cross-server references that explicitly specify a schema name. For example, a reference to an entity in the format <span class=\"code\">EXEC MyDatabase.MySchema.MyProc</span> is not caller dependent. However, a reference in the format <span class=\"code\">EXEC MyDatabase..MyProc</span> is caller dependent.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "is_ambiguous"            , "<html><p>Indicates the reference is ambiguous and can resolve at run time to a user-defined function, a user-defined type (UDT), or an xquery reference to a column of type <strong>xml</strong>. For example, assume the statement <span class=\"code\">SELECT Sales.GetOrder() FROM Sales.MySales</span> is defined in a stored procedure. Until the stored procedure is executed, it is not known whether <span class=\"code\">Sales.GetOrder()</span> is a user-defined function in the <span class=\"code\">Sales</span> schema or column named <span class=\"code\">Sales</span> of type UDT with a method named <span class=\"code\">GetOrder()</span>.</p><p>1 = Reference to a user-defined function or column user-defined type (UDT) method is ambiguous.</p><p>0 = Reference is unambiguous or the entity can be successfully bound when the function is called. </p><p>Always 0 for schema-bound references.</p></html>");
			mtd.addColumn("dm_sql_referenced_entities", "is_selected"             , "<html><p>1 = The object or column is selected.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_sql_referenced_entities", "is_updated"              , "<html><p>1 = The object or column is modified.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_sql_referenced_entities", "is_select_all"           , "<html><p>1 = The object is used in a SELECT * clause (object-level only).</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_sql_referenced_entities", "is_all_columns_found"    , "<html><p>1 = All column dependencies for the object could be found. </p><p>0 = Column dependencies for the object could not be found.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_sql_referenced_entities' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_sql_referencing_entities
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_sql_referencing_entities",  "<p>Returns one row for each entity in the current database that references another user-defined entity by name. A dependency between two entities is created when one entity, called the <em>referenced entity</em>, appears by name in a persisted SQL expression of another entity, called the <em>referencing entity</em>. For example, if a user-defined type (UDT) is specified as the referenced entity, this function returns each user-defined entity that reference that type by name in its definition. The function does not return entities in other databases that may reference the specified entity. This function must be executed in the context of the <span class=\"literal\">master</span> database to return a server-level DDL trigger as a referencing entity.</p><p>You can use this dynamic management function to report on the following types of entities in the current database that reference the specified entity:</p><ul class=\"unordered\"> <li><p>Schema-bound or non-schema-bound entities</p></li> <li><p>Database-level DDL triggers</p></li> <li><p>Server-level DDL triggers</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_sql_referencing_entities", "referencing_schema_name", "<html><p>Schema in which the referencing entity belongs. Is nullable.</p><p>NULL for database-level and server-level DDL triggers.</p></html>");
			mtd.addColumn("dm_sql_referencing_entities", "referencing_entity_name", "<html><p>Name of the referencing entity. Is not nullable.</p></html>");
			mtd.addColumn("dm_sql_referencing_entities", "referencing_id"         , "<html><p>ID of the referencing entity. Is not nullable.</p></html>");
			mtd.addColumn("dm_sql_referencing_entities", "referencing_class"      , "<html><p>Class of the referencing entity. Is not nullable.</p><p>1 = Object</p><p>12 = Database-level DDL trigger</p><p>13 = Server-level DDL trigger</p></html>");
			mtd.addColumn("dm_sql_referencing_entities", "referencing_class_desc" , "<html><p>Description of class of referencing entity.</p><p>OBJECT</p><p>DATABASE_DDL_TRIGGER</p><p>SERVER_DDL_TRIGGER</p></html>");
			mtd.addColumn("dm_sql_referencing_entities", "is_caller_dependent"    , "<html><p>Indicates the resolution of the referenced entity ID occurs at run time because it depends on the schema of the caller. </p><p>1 = The referencing entity has the potential to reference the entity; however, resolution of the referenced entity ID is caller dependent and cannot be determined. This occurs only for non-schema-bound references to a stored procedure, extended stored procedure, or user-defined function called in an EXECUTE statement.</p><p>0 = Referenced entity is not caller dependent.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_sql_referencing_entities' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_stats_properties
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_stats_properties",  "<p>Returns properties of statistics for the specified database object (table or indexed view) in the current SQL Server database.</p>");

			// Column names and description
			mtd.addColumn("dm_db_stats_properties", "object_id"           , "<html><p>ID of the object (table or indexed view) for which to return the properties of the statistics object.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "stats_id"            , "<html><p>ID of the statistics object. Is unique within the table or indexed view. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177623.aspx\">sys.stats (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "last_updated"        , "<html><p>Date and time the statistics object was last updated.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "rows"                , "<html><p>Total number of rows in the table or indexed view when statistics were last updated. If the statistics are filtered or correspond to a filtered index, the number of rows might be less than the number of rows in the table.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "rows_sampled"        , "<html><p>Total number of rows sampled for statistics calculations.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "steps"               , "<html><p>Number of steps in the histogram. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms174384.aspx\">DBCC SHOW_STATISTICS (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "unfiltered_rows"     , "<html><p>Total number of rows in the table before applying the filter expression (for filtered statistics). If statistics are not filtered, <span class=\"literal\">unfiltered_rows</span> is equal to the value returns in the <span class=\"literal\">rows</span> column.</p></html>");
			mtd.addColumn("dm_db_stats_properties", "modification_counter", "<html><p>Total number of modifications for the leading statistics column (the column on which the histogram is built) since the last time statistics were updated.</p><p>This column does not contain information for memory-optimized tables.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_stats_properties' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_qn_subscriptions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_qn_subscriptions",  "<p>Returns information about the active query notifications subscriptions in the server. You can use this view to check for active subscriptions in the server or a specified database, or to check for a specified server principal.</p>");

			// Column names and description
			mtd.addColumn("dm_qn_subscriptions", "id"         , "<html><p>ID of a subscription. </p></html>");
			mtd.addColumn("dm_qn_subscriptions", "database_id", "<html><p>ID of the database in which the notification query was executed. This database stores information related to this subscription. </p></html>");
			mtd.addColumn("dm_qn_subscriptions", "sid"        , "<html><p>Security ID of the server principal that created and owns this subscription. </p></html>");
			mtd.addColumn("dm_qn_subscriptions", "object_id"  , "<html><p>ID of the internal table that stores information about subscription parameters. </p></html>");
			mtd.addColumn("dm_qn_subscriptions", "created"    , "<html><p>Date and time that the subscription was created. </p></html>");
			mtd.addColumn("dm_qn_subscriptions", "timeout"    , "<html><p>Time-out for the subscription in seconds. The notification will be flagged to fire after this time has elapsed. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The actual firing time may be greater than the specified time-out. However, if a change that invalidates the subscription occurs after the specified time-out, but before the subscription is fired, SQL Server ensures that firing occurs at the time that the change was made. </p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_qn_subscriptions", "status"     , "<html><p>Indicates the status of the subscription. See the table under remarks for the list of codes.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_qn_subscriptions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_repl_articles
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_repl_articles",  "<p>Returns information about database objects published as articles in a replication topology.</p>");

			// Column names and description
			mtd.addColumn("dm_repl_articles", "artcache_db_address"     , "<html><p>In-memory address of the cached database structure for the publication database.</p></html>");
			mtd.addColumn("dm_repl_articles", "artcache_table_address"  , "<html><p>In-memory address of the cached table structure for a published table article.</p></html>");
			mtd.addColumn("dm_repl_articles", "artcache_schema_address" , "<html><p>In-memory address of the cached article schema structure for a published table article.</p></html>");
			mtd.addColumn("dm_repl_articles", "artcache_article_address", "<html><p>In-memory address of the cached article structure for a published table article.</p></html>");
			mtd.addColumn("dm_repl_articles", "artid"                   , "<html><p>Uniquely identifies each entry within this table.</p></html>");
			mtd.addColumn("dm_repl_articles", "artfilter"               , "<html><p>ID of the stored procedure used to horizontally filter the article.</p></html>");
			mtd.addColumn("dm_repl_articles", "artobjid"                , "<html><p>ID of the published object. </p></html>");
			mtd.addColumn("dm_repl_articles", "artpubid"                , "<html><p>ID of the publication to which the article belongs.</p></html>");
			mtd.addColumn("dm_repl_articles", "artstatus"               , "<html><p>Bitmask of the article options and status, which can be the bitwise logical OR result of one or more of these values:</p><p><strong>1</strong> = Article is active.</p><p><strong>8</strong> = Include the column name in INSERT statements.</p><p><strong>16</strong> = Use parameterized statements.</p><p><strong>24</strong> = Both include the column name in INSERT statements and use parameterized statements. </p><p>For example, an active article using parameterized statements would have a value of 17 in this column. A value of 0 means that the article is inactive and no additional properties are defined.</p></html>");
			mtd.addColumn("dm_repl_articles", "arttype"                 , "<html><p>Type of article: </p><p><strong>1</strong> = Log-based article.</p><p><strong>3</strong> = Log-based article with manual filter.</p><p><strong>5</strong> = Log-based article with manual view. </p><p><strong>7</strong> = Log-based article with manual filter and manual view.</p><p><strong>8</strong> = Stored procedure execution.</p><p><strong>24</strong> = Serializable stored procedure execution.</p><p><strong>32</strong> = Stored procedure (schema only). </p><p><strong>64</strong> = View (schema only).</p><p><strong>128</strong> = Function (schema only).</p></html>");
			mtd.addColumn("dm_repl_articles", "wszArtdesttable"         , "<html><p>Name of published object at the destination.</p></html>");
			mtd.addColumn("dm_repl_articles", "wszArtdesttableowner"    , "<html><p>Owner of published object at the destination.</p></html>");
			mtd.addColumn("dm_repl_articles", "wszArtinscmd"            , "<html><p>Command or stored procedure used for inserts.</p></html>");
			mtd.addColumn("dm_repl_articles", "cmdTypeIns"              , "<html><p>Call syntax for the insert stored procedure, and can be one of these values.</p><p><strong>1</strong> = CALL</p><p><strong>2</strong> = SQL</p><p><strong>3</strong> = NONE</p><p><strong>7</strong> = UNKNOWN</p></html>");
			mtd.addColumn("dm_repl_articles", "wszArtdelcmd"            , "<html><p>Command or stored procedure used for deletes.</p></html>");
			mtd.addColumn("dm_repl_articles", "cmdTypeDel"              , "<html><p>Call syntax for the delete stored procedure, and can be one of these values.</p><p><strong>0</strong> = XCALL</p><p><strong>1</strong> = CALL</p><p><strong>2</strong> = SQL</p><p><strong>3</strong> = NONE</p><p><strong>7</strong> = UNKNOWN</p></html>");
			mtd.addColumn("dm_repl_articles", "wszArtupdcmd"            , "<html><p>Command or stored procedure used for updates.</p></html>");
			mtd.addColumn("dm_repl_articles", "cmdTypeUpd"              , "<html><p>Call syntax for the update stored procedure, and can be one of these values.</p><p><strong>0</strong> = XCALL</p><p><strong>1</strong> = CALL</p><p><strong>2</strong> = SQL</p><p><strong>3</strong> = NONE</p><p><strong>4</strong> = MCALL</p><p><strong>5</strong> = VCALL</p><p><strong>6</strong> = SCALL</p><p><strong>7</strong> = UNKNOWN</p></html>");
			mtd.addColumn("dm_repl_articles", "wszArtpartialupdcmd"     , "<html><p>Command or stored procedure used for partial updates.</p></html>");
			mtd.addColumn("dm_repl_articles", "cmdTypePartialUpd"       , "<html><p>Call syntax for the partial update stored procedure, and can be one of these values.</p><p><strong>2</strong> = SQL</p></html>");
			mtd.addColumn("dm_repl_articles", "numcol"                  , "<html><p>Number of columns in the partition for a vertically filtered article.</p></html>");
			mtd.addColumn("dm_repl_articles", "artcmdtype"              , "<html><p>Type of command currently being replicated, and can be one of these values. </p><p><strong>1</strong> = INSERT </p><p><strong>2</strong> = DELETE</p><p><strong>3</strong> = UPDATE</p><p><strong>4</strong> = UPDATETEXT</p><p><strong>5</strong> = none </p><p><strong>6</strong> = internal use only</p><p><strong>7</strong> = internal use only</p><p><strong>8</strong> = partial UPDATE</p></html>");
			mtd.addColumn("dm_repl_articles", "artgeninscmd"            , "<html><p>INSERT command template based on the columns included in the article. </p></html>");
			mtd.addColumn("dm_repl_articles", "artgendelcmd"            , "<html><p>DELETE command template, which can include the primary key or the columns included in the article, depending on the call syntax is used. </p></html>");
			mtd.addColumn("dm_repl_articles", "artgenupdcmd"            , "<html><p>UPDATE command template, which can include the primary key, updated columns, or a complete column list depending on the call syntax is used.</p></html>");
			mtd.addColumn("dm_repl_articles", "artpartialupdcmd"        , "<html><p>Partial UPDATE command template, which includes the primary key and updated columns.</p></html>");
			mtd.addColumn("dm_repl_articles", "artupdtxtcmd"            , "<html><p>UPDATETEXT command template, which includes the primary key and updated columns.</p></html>");
			mtd.addColumn("dm_repl_articles", "artgenins2cmd"           , "<html><p>INSERT command template used when reconciling an article during concurrent snapshot processing.</p></html>");
			mtd.addColumn("dm_repl_articles", "artgendel2cmd"           , "<html><p>DELETE command template used when reconciling an article during concurrent snapshot processing.</p></html>");
			mtd.addColumn("dm_repl_articles", "fInReconcile"            , "<html><p>Indicates whether an article is currently being reconciled during concurrent snapshot processing.</p></html>");
			mtd.addColumn("dm_repl_articles", "fPubAllowUpdate"         , "<html><p>Indicates whether the publication allows updating subscription.</p></html>");
			mtd.addColumn("dm_repl_articles", "intPublicationOptions"   , "<html><p>Bitmap that specifies additional publishing options, where the bitwise option values are:</p><p><strong>0x1</strong> - Enabled for peer-to-peer replication.</p><p><strong>0x2</strong> - Publish only local changes.</p><p><strong>0x4</strong> - Enabled for non-SQL?Server Subscribers.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_repl_articles' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_repl_tranhash
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_repl_tranhash",  "<p>Returns information about transactions being replicated in a transactional publication.</p>");

			// Column names and description
			mtd.addColumn("dm_repl_tranhash", "buckets"          , "<html><p>Number of buckets in the hash table.</p></html>");
			mtd.addColumn("dm_repl_tranhash", "hashed_trans"     , "<html><p>Number of committed transactions replicated in the current batch.</p></html>");
			mtd.addColumn("dm_repl_tranhash", "completed_trans"  , "<html><p>Number of transactions competed so far.</p></html>");
			mtd.addColumn("dm_repl_tranhash", "compensated_trans", "<html><p>Number of transactions that contain partial rollbacks.</p></html>");
			mtd.addColumn("dm_repl_tranhash", "first_begin_lsn"  , "<html><p>Earliest begin log sequence number (LSN) in the current batch.</p></html>");
			mtd.addColumn("dm_repl_tranhash", "last_commit_lsn"  , "<html><p>Last commit LSN in the current batch.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_repl_tranhash' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_repl_schemas
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_repl_schemas",  "<p>Returns information about table columns published by replication.</p>");

			// Column names and description
			mtd.addColumn("dm_repl_schemas", "artcache_schema_address", "<html><p>In-memory address of the cached schema structure for the published table article.</p></html>");
			mtd.addColumn("dm_repl_schemas", "tabid"                  , "<html><p>ID of the replicated table. </p></html>");
			mtd.addColumn("dm_repl_schemas", "indexid"                , "<html><p>ID of a clustered index on the published table.</p></html>");
			mtd.addColumn("dm_repl_schemas", "idSch"                  , "<html><p>ID of the table schema.</p></html>");
			mtd.addColumn("dm_repl_schemas", "tabschema"              , "<html><p>Name of the table schema.</p></html>");
			mtd.addColumn("dm_repl_schemas", "ccTabschema"            , "<html><p>Character length of the table schema.</p></html>");
			mtd.addColumn("dm_repl_schemas", "tabname"                , "<html><p>Name of the published table.</p></html>");
			mtd.addColumn("dm_repl_schemas", "ccTabname"              , "<html><p>Character length of the published table name.</p></html>");
			mtd.addColumn("dm_repl_schemas", "rowsetid_delete"        , "<html><p>ID of the deleted row.</p></html>");
			mtd.addColumn("dm_repl_schemas", "rowsetid_insert"        , "<html><p>ID of the inserted row.</p></html>");
			mtd.addColumn("dm_repl_schemas", "num_pk_cols"            , "<html><p>Number of primary key columns.</p></html>");
			mtd.addColumn("dm_repl_schemas", "pcitee"                 , "<html><p>Pointer to the query expression structure used to evaluate computed column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_numtextcols"         , "<html><p>Number of binary large object columns in the replicated table.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_schema_lsn_begin"    , "<html><p>Beginning log sequence number (LSN) of schema version logging.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_schema_lsn_end"      , "<html><p>Ending LSN of schema version logging.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_numcols"             , "<html><p>Number of columns published.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_colid"               , "<html><p>Column identifier at the Publisher.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_awcName"             , "<html><p>Name of the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_ccName"              , "<html><p>Number of characters in the column name.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_pk"                  , "<html><p>Whether the published column is part of a primary key.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_unique"              , "<html><p>Whether the published column is part of a unique index.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_maxlen"              , "<html><p>Maximum length of the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_prec"                , "<html><p>Precision of the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_scale"               , "<html><p>Scale of the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_collatid"            , "<html><p>Collation ID for published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_xvtype"              , "<html><p>Type of the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_offset"              , "<html><p>Offset of the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_bitpos"              , "<html><p>Bit position of the published column, in the byte vector.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_fNullable"           , "<html><p>Specifies whether the published column supports NULL values.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_fAnsiTrim"           , "<html><p>Specifies whether ANSI trim is used on the published column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "re_computed"            , "<html><p>Specifies whether the published column is a computed column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_rowsetid"            , "<html><p>ID of the rowset.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_schema_lsn_begin"    , "<html><p>Beginning LSN of schema version logging. </p></html>");
			mtd.addColumn("dm_repl_schemas", "se_schema_lsn_end"      , "<html><p>Ending LSN of schema version logging.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_numcols"             , "<html><p>Number of columns.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_colid"               , "<html><p>ID of the column at the Subscriber.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_maxlen"              , "<html><p>Maximum length of the column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_prec"                , "<html><p>Precision of the column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_scale"               , "<html><p>Scale of the column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_collatid"            , "<html><p>Collation ID for column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_xvtype"              , "<html><p>Type of the column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_offset"              , "<html><p>Offset of the column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_bitpos"              , "<html><p>Bit position of the column, in the byte vector.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_fNullable"           , "<html><p>Specifies whether the column supports NULL values.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_fAnsiTrim"           , "<html><p>Specifies whether ANSI trim is used on the column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_computed"            , "<html><p>Specifies whether the columnis a computed column.</p></html>");
			mtd.addColumn("dm_repl_schemas", "se_nullBitInLeafRows"   , "<html><p>Specifies whether the column value is NULL.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_repl_schemas' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_repl_traninfo
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_repl_traninfo",  "<p>Returns information on each replicated or change data capture transaction. </p>");

			// Column names and description
			mtd.addColumn("dm_repl_traninfo", "fp2p_pub_exists"           , "<html><p>If the transaction is in a database published using peer-to-peer transactional replication. If true, the value is 1; otherwise, it is 0.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "db_ver"                    , "<html><p>Database version.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "comp_range_address"        , "<html><p>Defines a partial rollback range that must be skipped.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "textinfo_address"          , "<html><p>In-memory address of the cached text information structure.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "fsinfo_address"            , "<html><p>In-memory address of the cached filestream information structure.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "begin_lsn"                 , "<html><p>Log sequence number (LSN) of the beginning log record for the transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "commit_lsn"                , "<html><p>LSN of commit log record for the transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "dbid"                      , "<html><p>Database ID. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "rows"                      , "<html><p>ID of the replicated command within the transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "xdesid"                    , "<html><p>Transaction ID.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "artcache_table_address"    , "<html><p>In-memory address of the cached article table structure last used for this transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "server"                    , "<html><p>Server name.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "server_len_in_bytes"       , "<html><p>Character length, in bytes, of the server name. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "database"                  , "<html><p>Database name.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "db_len_in_bytes"           , "<html><p>Character length, in bytes, of the database name.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "originator"                , "<html><p>Name of the server where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "originator_len_in_bytes"   , "<html><p>Character length, in bytes, of the server where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "orig_db"                   , "<html><p>Name of the database where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "orig_db_len_in_bytes"      , "<html><p>Character length, in bytes, of the database where the transaction originated.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "cmds_in_tran"              , "<html><p>Number of replicated commands in the current transaction, which is used to determine when a logical transaction should be committed.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "is_boundedupdate_singleton", "<html><p>Specifies whether a unique column update affects only a single row.?</p></html>");
			mtd.addColumn("dm_repl_traninfo", "begin_update_lsn"          , "<html><p>LSN used in a unique column update.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "delete_lsn"                , "<html><p>LSN to delete as part of an update.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "last_end_lsn"              , "<html><p>Last LSN in a logical transaction. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "fcomplete"                 , "<html><p>Specifies whether the command is a partial update.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "fcompensated"              , "<html><p>Specifies whether the transaction is involved in a partial rollback.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "fprocessingtext"           , "<html><p>Specifies whether the transaction includes a binary large data type column. </p></html>");
			mtd.addColumn("dm_repl_traninfo", "max_cmds_in_tran"          , "<html><p>Maximum number of commands in a logical transaction, as specified by the Log Reader Agent.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "begin_time"                , "<html><p>Time the transaction began.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "commit_time"               , "<html><p>Time the transaction was committed.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "session_id"                , "<html><p>ID of the change data capture log scan session. This column maps to the <strong>session_id</strong> column in <a href=\"https://msdn.microsoft.com/en-us/library/bb510694.aspx\">sys.dm_cdc_logscan_sessions</a>.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "session_phase"             , "<html><p>Number that indicates the phase the session was in at the time the error occurred. This column maps to the <strong>phase_number</strong> column in <a href=\"https://msdn.microsoft.com/en-us/library/bb500301.aspx\">sys.dm_cdc_errors</a>.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "is_known_cdc_tran"         , "<html><p>Indicates the transaction is tracked by change data capture.</p><p>0 = Transaction replication transaction.</p><p>1 = Change data capture transaction.</p></html>");
			mtd.addColumn("dm_repl_traninfo", "error_count"               , "<html><p>Number of errors encountered.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_repl_traninfo' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_resource_governor_configuration
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_resource_governor_configuration",  "<p>Returns a row that contains the current in-memory configuration state of Resource Governor.</p>");

			// Column names and description
			mtd.addColumn("dm_resource_governor_configuration", "classifier_function_id"       , "<html><p>The ID of the classifier function that is currently used by Resource Governor. Returns a value of 0 if no function is being used. Is not nullable. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>This function is used to classify new requests and uses rules to route these requests to the appropriate workload group. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/bb933866.aspx\">Resource Governor</a>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_configuration", "is_reconfiguration_pending"   , "<html><p>Indicates whether or not changes to a group or pool were made with the ALTER RESOURCE GOVERNOR RECONFIGURE statement but have not been applied to the in-memory configuration. The value returned is one of:</p><ul class=\"unordered\"> <li><p>0 - A reconfiguration statement is not required. </p></li> <li><p>1 - A reconfiguration statement or server restart is required in order for pending configuration changes to be applied.</p></li></ul><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The value returned is always 0 when Resource Governor is disabled.</p></td>    </tr>   </tbody>  </table> </div></div><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_configuration", "max_outstanding_io_per_volume", "<html><p>The maximum number of outstanding I/O per volume.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_resource_governor_configuration' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_resource_governor_resource_pools
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_resource_governor_resource_pools",  "<p>Returns information about the current resource pool state, the current configuration of resource pools, and resource pool statistics.</p>");

			// Column names and description
			mtd.addColumn("dm_resource_governor_resource_pools", "pool_id"                     , "<html><p>The ID of the resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "name"                        , "<html><p>The name of the resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "statistics_start_time"       , "<html><p>The time when statistics was reset for this pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "total_cpu_usage_ms"          , "<html><p>The cumulative CPU usage in milliseconds since the Resource Govenor statistics were reset. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "cache_memory_kb"             , "<html><p>The current total cache memory usage in kilobytes. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "compile_memory_kb"           , "<html><p>The current total stolen memory usage in kilobytes (KB). The majority of this usage would be for compile and optimization, but it can also include other memory users. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "used_memgrant_kb"            , "<html><p>The current total used (stolen) memory from memory grants. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "total_memgrant_count"        , "<html><p>The cumulative count of memory grants in this resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "total_memgrant_timeout_count", "<html><p>The cumulative count of memory grant time-outs in this resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "active_memgrant_count"       , "<html><p>The current count of memory grants. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "active_memgrant_kb"          , "<html><p>The sum, in kilobytes (KB), of current memory grants. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "memgrant_waiter_count"       , "<html><p>The count of queries currently pending on memory grants. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "max_memory_kb"               , "<html><p>The maximum amount of memory, in kilobytes, that the resource pool can have. This is based on the current settings and server state. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "used_memory_kb"              , "<html><p>The amount of memory used, in kilobytes, for the resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "target_memory_kb"            , "<html><p>The target amount of memory, in kilobytes, the resource pool is trying to attain. This is based on the current settings and server state. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "out_of_memory_count"         , "<html><p>The number of failed memory allocations in the pool since the Resource Govenor statistics were reset. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "min_cpu_percent"             , "<html><p>The current configuration for the guaranteed average CPU bandwidth for all requests in the resource pool when there is CPU contention. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "max_cpu_percent"             , "<html><p>The current configuration for the maximum average CPU bandwidth allowed for all requests in the resource pool when there is CPU contention. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "min_memory_percent"          , "<html><p>The current configuration for the guaranteed amount of memory for all requests in the resource pool when there is memory contention. This is not shared with other resource pools. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "max_memory_percent"          , "<html><p>The current configuration for the percentage of total server memory that can be used by requests in this resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "cap_cpu_percent"             , "<html><p>Hard cap on the CPU bandwidth that all requests in the resource pool will receive. Limits the maximum CPU bandwidth level to the specified level. The allowed range for value is from 1 through 100. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "min_iops_per_volume"         , "<html><p>The minimum IO per second (IOPS) per disk volume setting for this Pool. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "max_iops_per_volume"         , "<html><p>The maximum IO per second (IOPS) per disk volume setting for this Pool. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0..</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_io_queued_total"        , "<html><p>The total read IOs enqueued since the Resource Govenor was reset. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_io_issued_total"        , "<html><p>The total read IOs issued since the Resource Govenor statistics were reset. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_io_completed_total"     , "<html><p>The total read IOs completed since the Resource Govenor statistics were reset. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_io_throttled_total"     , "<html><p>The total read IOs throttled since the Resource Govenor statistics were reset. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_bytes_total"            , "<html><p>The total number of bytes read since the Resource Govenor statistics were reset. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_io_stall_total_ms"      , "<html><p>Total time (in milliseconds) between read IO issue and completion. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "read_io_stall_queued_ms"     , "<html><p>Total time (in milliseconds) between read IO arrival and completion. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><p>To determine if the IO setting for the pool is causing latency, subtract <strong>read_io_stall_queued_ms</strong> from <strong>read_io_stall_total_ms</strong>.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_io_queued_total"       , "<html><p>The total write IOs enqueued since the Resource Govenor statistics were reset. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_io_issued_total"       , "<html><p>The total write IOs issued since the Resource Govenor statistics were reset. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_io_completed_total"    , "<html><p>The total write IOs completed since the Resource Govenor statistics were reset. Is not nullable</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_io_throttled_total"    , "<html><p>The total write IOs throttled since the Resource Govenor statistics were reset. Is not nullable</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_bytes_total"           , "<html><p>The total number of bytes written since the Resource Govenor statistics were reset. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_io_stall_total_ms"     , "<html><p>Total time (in milliseconds) between write IO issue and completion. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "write_io_stall_queued_ms"    , "<html><p>Total time (in milliseconds) between write IO arrival and completion. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><p>This is the delay introduced by IO Resource Governance.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "io_issue_violations_total"   , "<html><p>Total IO issue violations. That is, the number of times when the rate of IO issue was lower than the reserved rate. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_resource_pools", "io_issue_delay_total_ms"     , "<html><p>Total time (in milliseconds) between the scheduled issue and actual issue of IO. Is nullable. Null if the resource pool is not governed for IO. That is, the Resource Pool MIN_IOPS_PER_VOLUME and MAX_IOPS_PER_VOLUME settings are 0.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2014 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_resource_governor_resource_pools' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_resource_governor_workload_groups
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_resource_governor_workload_groups",  "<p>Returns workload group statistics and the current in-memory configuration of the workload group. This view can be joined with <span class=\"literal\">sys.dm_resource_governor_resource_pools</span> to get the resource pool name.</p>");

			// Column names and description
			mtd.addColumn("dm_resource_governor_workload_groups", "group_id"                              , "<html><p>ID of the workload group. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "name"                                  , "<html><p>Name of the workload group. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "pool_id"                               , "<html><p>ID of the resource pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "statistics_start_time"                 , "<html><p>Time that statistics collection was reset for the workload group. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_request_count"                   , "<html><p>Cumulative count of completed requests in the workload group. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_queued_request_count"            , "<html><p>Cumulative count of requests queued after the GROUP_MAX_REQUESTS limit was reached. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "active_request_count"                  , "<html><p>Current request count. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "queued_request_count"                  , "<html><p>Current queued request count. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_cpu_limit_violation_count"       , "<html><p>Cumulative count of requests exceeding the CPU limit. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_cpu_usage_ms"                    , "<html><p>Cumulative CPU usage, in milliseconds, by this workload group. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "max_request_cpu_time_ms"               , "<html><p>Maximum CPU usage, in milliseconds, for a single request. Is not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>This is a measured value, unlike <span class=\"literal\">request_max_cpu_time_sec</span>, which is a configurable setting. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/bb934148.aspx\">CPU Threshold Exceeded Event Class</a>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "blocked_task_count"                    , "<html><p>Current count of blocked tasks. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_lock_wait_count"                 , "<html><p>Cumulative count of lock waits that occurred. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_lock_wait_time_ms"               , "<html><p>Cumulative sum of elapsed time, in milliseconds, a lock is held. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_query_optimization_count"        , "<html><p>Cumulative count of query optimizations in this workload group. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_suboptimal_plan_generation_count", "<html><p>Cumulative count of suboptimal plan generations that occurred in this workload group due to memory pressure. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "total_reduced_memgrant_count"          , "<html><p>Cumulative count of memory grants that reached the maximum query size limit. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "max_request_grant_memory_kb"           , "<html><p>Maximum memory grant size, in kilobytes, of a single request since the statistics were reset. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "active_parallel_thread_count"          , "<html><p>Current count of parallel thread usage. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "importance"                            , "<html><p>Current configuration value for the relative importance of a request in this workload group. Importance is one of the following, with Medium being the default:</p><ul class=\"unordered\"> <li><p>Low</p></li> <li><p>Medium</p></li> <li><p>High</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "request_max_memory_grant_percent"      , "<html><p>Current setting for the maximum memory grant, as a percentage, for a single request. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "request_max_cpu_time_sec"              , "<html><p>Current setting for maximum CPU use limit, in seconds, for a single request. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "request_memory_grant_timeout_sec"      , "<html><p>Current setting for memory grant time-out, in seconds, for a single request. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "group_max_requests"                    , "<html><p>Current setting for the maximum number of concurrent requests. Is not nullable.</p></html>");
			mtd.addColumn("dm_resource_governor_workload_groups", "max_dop"                               , "<html><p>Maximum degree of parallelism for the workload group. The default value, 0, uses global settings. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_resource_governor_workload_groups' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_audit_actions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_audit_actions",  "<p>Returns a row for every audit action that can be reported in the audit log and every audit action group that can be configured as part of SQL Server Audit. For more information about?SQL Server Audit, see <a href=\"https://msdn.microsoft.com/en-us/library/cc280386.aspx\">SQL Server Audit (Database Engine)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_audit_actions", "action_id"                  , "<html><p>ID of the audit action. Related to the <strong>action_id</strong> value written to each audit record. Is nullable. NULL for audit groups.</p></html>");
			mtd.addColumn("dm_audit_actions", "action_in_log"              , "<html><p>Indicates whether an action can be written to an audit log. Values are as follows: </p><ul class=\"unordered\"> <li><p>1 = Yes</p></li> <li><p>0 = No</p></li></ul></html>");
			mtd.addColumn("dm_audit_actions", "name"                       , "<html><p>Name of the audit action or action group. Is not nullable.</p></html>");
			mtd.addColumn("dm_audit_actions", "class_desc"                 , "<html><p>The name of the class of the object that the audit action applies to. Can be any one of the Server, Database, or Schema scope objects, but does not include Schema objects. Is not nullable.</p></html>");
			mtd.addColumn("dm_audit_actions", "parent_class_desc"          , "<html><p>Name of the parent class for the object described by <span class=\"literal\">class_desc</span>. Is NULL if the <span class=\"literal\">class_desc</span> is Server. </p></html>");
			mtd.addColumn("dm_audit_actions", "covering_parent_action_name", "<html><p>Name of the audit action or audit group that contains the audit action described in this row. This is used to create a hierarchy of actions and covering actions. Is nullable.</p></html>");
			mtd.addColumn("dm_audit_actions", "configuration_level"        , "<html><p>Indicates that the action or action group specified in this row is configurable at the Group or Action level. Is NULL if the action is not configurable.</p></html>");
			mtd.addColumn("dm_audit_actions", "containing_group_name"      , "<html><p>The name of the audit group that contains the specified action. Is NULL if the value in name is a group.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_audit_actions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_audit_class_type_map
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_audit_class_type_map",  "<p>Returns a table that maps the <span class=\"literal\">class_type</span> field in the audit log to the <span class=\"literal\">class_desc</span> field in <span class=\"literal\">sys.dm_audit_actions</span>. For more information about SQL Server Audit, see <a href=\"https://msdn.microsoft.com/en-us/library/cc280386.aspx\">SQL Server Audit (Database Engine)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_audit_class_type_map", "class_type"          , "<html><p>The class type of the entity that was audited. Maps to the <span class=\"literal\">class_type</span> written to the audit log and returned by the <strong>get_audit_file()</strong> function. Is not nullable.</p></html>");
			mtd.addColumn("dm_audit_class_type_map", "class_type_desc"     , "<html><p>The name for the auditable entity. Is not nullable.</p></html>");
			mtd.addColumn("dm_audit_class_type_map", "securable_class_desc", "<html><p>The securable object that maps to the class_type being audited. Is NULL if the class_type does not map to a securable object. Can be related to <span class=\"literal\">class_desc</span> in <span class=\"literal\">sys.dm_audit_actions</span>. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_audit_class_type_map' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_cryptographic_provider_algorithms
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_cryptographic_provider_algorithms",  "<p>Returns the algorithms supported by an Extensible Key Management (EKM) provider.</p>");

			// Column names and description
			mtd.addColumn("dm_cryptographic_provider_algorithms", "algorithm_id" , "<html><p>Is the identification number of the algorithm.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_algorithms", "algorithm_tag", "<html><p>Is the identification tag of the algorithm.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_algorithms", "key_type"     , "<html><p>Shows the key type. Returns either ASYMMETRIC KEY or SYMMETRIC KEY.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_algorithms", "key_length"   , "<html><p>Indicates the key length in bits.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_cryptographic_provider_algorithms' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_cryptographic_provider_keys
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_cryptographic_provider_keys",  "<p>Returns information about the keys provided by a Extensible Key Management (EKM) provider.</p>");

			// Column names and description
			mtd.addColumn("dm_cryptographic_provider_keys", "key_id"        , "<html><p>Identification number of the key on the provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_keys", "key_name"      , "<html><p>Name of the key on the provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_keys", "key_thumbprint", "<html><p>Thumbprint from the provider of the key.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_keys", "algorithm_id"  , "<html><p>Identification number of the algorithm on the provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_keys", "algorithm_tag" , "<html><p>Tag of the algorithm on the provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_keys", "key_type"      , "<html><p>Type of key on the provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_keys", "key_length"    , "<html><p>Length of the key on the provider.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_cryptographic_provider_keys' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_cryptographic_provider_properties
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_cryptographic_provider_properties",  "<p>Returns information about registered cryptographic providers.</p>");

			// Column names and description
			mtd.addColumn("dm_cryptographic_provider_properties", "provider_id"              , "<html><p>Identification number of the cryptographic provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "guid"                     , "<html><p>Unique provider GUID.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "provider_version"         , "<html><p>Version of the provider in the format '<em>aa.bb.cccc.dd</em>'.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "sqlcrypt_version"         , "<html><p>Major version of the SQL Server Cryptographic API in the format '<em>aa.bb.cccc.dd</em>'.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "friendly_name"            , "<html><p>Name supplied by the provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "authentication_type"      , "<html><p>WINDOWS, BASIC, or OTHER.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "symmetric_key_support"    , "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "symmetric_key_export"     , "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "symmetric_key_import"     , "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "symmetric_key_persistance", "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "asymmetric_key_support"   , "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "asymmetric_key_export"    , "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "symmetric_key_import"     , "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
			mtd.addColumn("dm_cryptographic_provider_properties", "symmetric_key_persistance", "<html><p>0 (not supported)</p><p>1 (supported)</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_cryptographic_provider_properties' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_cryptographic_provider_sessions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_cryptographic_provider_sessions",  "<p> Returns information about open sessions for a cryptographic provider.</p>");

			// Column names and description
			mtd.addColumn("dm_cryptographic_provider_sessions", "provider_id"   , "<html><p>Identification number of the cryptographic provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_sessions", "session_handle", "<html><p>Cryptographic session handle.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_sessions", "identity"      , "<html><p>Identity used to authenticate with the cryptographic provider.</p></html>");
			mtd.addColumn("dm_cryptographic_provider_sessions", "spid"          , "<html><p>Session ID SPID of the connection. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms189535.aspx\">@@SPID (Transact-SQL)</a>.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_cryptographic_provider_sessions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_database_encryption_keys
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_database_encryption_keys",  "<p>Returns information about the encryption state of a database and its associated database encryption keys. For more information about database encryption, see <a href=\"https://msdn.microsoft.com/en-us/library/bb934049.aspx\">Transparent Data Encryption (TDE)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_database_encryption_keys", "database_id"         , "<html><p>ID of the database.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "encryption_state"    , "<html><p>Indicates whether the database is encrypted or not encrypted.</p><p>0 = No database encryption key present, no encryption</p><p>1 = Unencrypted</p><p>2 = Encryption in progress</p><p>3 = Encrypted</p><p>4 = Key change in progress</p><p>5 = Decryption in progress</p><p>6 = Protection change in progress (The certificate or asymmetric key that is encrypting the database encryption key is being changed.)</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "create_date"         , "<html><p>Displays the date the encryption key was created.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "regenerate_date"     , "<html><p>Displays the date the encryption key was regenerated.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "modify_date"         , "<html><p>Displays the date the encryption key was modified.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "set_date"            , "<html><p>Displays the date the encryption key was applied to the database.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "opened_date"         , "<html><p>Shows when the database key was last opened.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "key_algorithm"       , "<html><p>Displays the algorithm that is used for the key.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "key_length"          , "<html><p>Displays the length of the key.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "encryptor_thumbprint", "<html><p>Shows the thumbprint of the encryptor.</p></html>");
			mtd.addColumn("dm_database_encryption_keys", "encryptor_type"      , "<html><p>Describes the encryptor.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server (SQL Server 2012 through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299658\">current version</a>).</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_database_encryption_keys", "percent_complete"    , "<html><p>Percent complete of the database encryption state change. This will be 0 if there is no state change.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_database_encryption_keys' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_server_audit_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_server_audit_status",  "<p>Returns a row for each server audit indicating the current state of the audit. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/cc280386.aspx\">SQL Server Audit (Database Engine)</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_server_audit_status", "audit_id"             , "<html><p>ID of the audit. Maps to the <strong>audit_id</strong> field in the <strong>sys.audits</strong> catalog view.</p></html>");
			mtd.addColumn("dm_server_audit_status", "name"                 , "<html><p>Name of the audit. Same as the <strong>name</strong> field in the <strong>sys.server_audits</strong> catalog view.</p></html>");
			mtd.addColumn("dm_server_audit_status", "status"               , "<html><p>Numeric status of the server audit:</p><p>0 ? Started</p><p>1 ? Failed</p></html>");
			mtd.addColumn("dm_server_audit_status", "status_desc"          , "<html><p>String that shows the status of the server audit:</p><p>- STARTED</p><p>- FAILED</p></html>");
			mtd.addColumn("dm_server_audit_status", "status_time"          , "<html><p>Timestamp in UTC of the last status change for the audit.</p></html>");
			mtd.addColumn("dm_server_audit_status", "event_session_address", "<html><p>Address of the Extended Events session associated with the audit. Related to the <strong>sys.db_xe_sessions.address</strong> catalog view.</p></html>");
			mtd.addColumn("dm_server_audit_status", "audit_file_path"      , "<html><p>Full path and file name of the audit file target that is currently being used. Only populated for file audits.</p></html>");
			mtd.addColumn("dm_server_audit_status", "audit_file_size"      , "<html><p>Approximate size of the audit file, in bytes. Only populated for file audits.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_server_audit_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_server_memory_dumps
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_server_memory_dumps",  "<p>Returns one row for each memory dump file generated by the SQL Server Database Engine. Use this dynamic management view to troubleshoot potential issues. </p>");

			// Column names and description
			mtd.addColumn("dm_server_memory_dumps", "filename"     , "<html><p>Path and name of the memory dump file. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_memory_dumps", "creation_time", "<html><p>Date and time the file was created. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_memory_dumps", "size_in_bytes", "<html><p>Size (in bytes ) of the file. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_server_memory_dumps' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_server_registry
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_server_registry",  "<p>Returns configuration and installation information that is stored in the Windows registry for the current instance of SQL Server. Returns one row per registry key. Use this dynamic management view to return information such as the SQL Server services that are available on the host machine or network configuration values for the instance of SQL Server.</p>");

			// Column names and description
			mtd.addColumn("dm_server_registry", "registry_key", "<html><p>Registry key name. Is nullable.</p></html>");
			mtd.addColumn("dm_server_registry", "value_name"  , "<html><p>Key value name. This is the item shown in the <strong>Name</strong> column of the Registry Editor. Is nullable.</p></html>");
			mtd.addColumn("dm_server_registry", "value_data"  , "<html><p>Value of the key data. This is the value shown in the <strong>Data</strong> column of the Registry Editor for a given entry. Is </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_server_registry' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_server_services
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_server_services",  "<p>Returns information about the SQL Server, Full-Text, and SQL Server Agent services in the current instance of SQL Server. Use this dynamic management view to report status information about these services.</p>");

			// Column names and description
			mtd.addColumn("dm_server_services", "servicename"      , "<html><p>Name of the SQL Server, Full-text, or SQL Server Agent service. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "startup_type"     , "<html><p>Indicates the start mode of the service.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>0</p></td>     <td data-th=\"Description\"><p>Other</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Other</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Automatic</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>3</p></td>     <td data-th=\"Description\"><p>Manual</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>4</p></td>     <td data-th=\"Description\"><p>Disabled</p></td>    </tr>   </tbody>  </table> </div></div><p>Is nullable.</p></html>");
			mtd.addColumn("dm_server_services", "startup_desc"     , "<html><p>Describes the start mode of the service.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>Other</p></td>     <td data-th=\"Description\"><p>Other (boot start)</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Other</p></td>     <td data-th=\"Description\"><p>Other (system start)</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Automatic</p></td>     <td data-th=\"Description\"><p>Auto start</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Manual</p></td>     <td data-th=\"Description\"><p>Demand start</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Disabled</p></td>     <td data-th=\"Description\"><p>Disabled</p></td>    </tr>   </tbody>  </table> </div></div><p>Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "status"           , "<html><p>Indicates the current status of the service.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>1</p></td>     <td data-th=\"Description\"><p>Stopped</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>2</p></td>     <td data-th=\"Description\"><p>Other (start pending)</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>3</p></td>     <td data-th=\"Description\"><p>Other (stop pending)</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>4</p></td>     <td data-th=\"Description\"><p>Running</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>5</p></td>     <td data-th=\"Description\"><p>Other (continue pending)</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>6</p></td>     <td data-th=\"Description\"><p>Other (pause pending)</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>7</p></td>     <td data-th=\"Description\"><p>Paused</p></td>    </tr>   </tbody>  </table> </div></div><p>Is nullable.</p></html>");
			mtd.addColumn("dm_server_services", "status_desc"      , "<html><p>Describes the current status of the service.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>Stopped</p></td>     <td data-th=\"Description\"><p>The service is stopped.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Other(start operation pending)</p></td>     <td data-th=\"Description\"><p>The service is in the process of starting.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Other (stop operation pending)</p></td>     <td data-th=\"Description\"><p>The service is in the process of stopping.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Running</p></td>     <td data-th=\"Description\"><p>The service is running.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Other (continue operations pending)</p></td>     <td data-th=\"Description\"><p>The service is in a pending state.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Other (pause pending)</p></td>     <td data-th=\"Description\"><p>The service is in the process of pausing.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Paused</p></td>     <td data-th=\"Description\"><p>The service is paused.</p></td>    </tr>   </tbody>  </table> </div></div><p>Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "process_id"       , "<html><p>The process ID of the service. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "last_startup_time", "<html><p>The date and time the service was last started. Is nullable.</p></html>");
			mtd.addColumn("dm_server_services", "service_account"  , "<html><p>The account authorized to control the service. This account can start or stop the service, or modify service properties. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "filename"         , "<html><p>The path and filename of the service executable. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "is_clustered"     , "<html><p>Indicates whether the service is installed as a resource of a clustered server. Cannot be null.</p></html>");
			mtd.addColumn("dm_server_services", "cluster_nodename" , "<html><p>The name of the cluster node on which the service is installed. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_server_services' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_broker_activated_tasks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_broker_activated_tasks",  "<p>Returns a row for each stored procedure activated by Service Broker.</p>");

			// Column names and description
			mtd.addColumn("dm_broker_activated_tasks", "spid"          , "<html><p>ID of the session of the activated stored procedure. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_activated_tasks", "database_id"   , "<html><p>ID of the database in which the queue is defined. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_activated_tasks", "queue_id"      , "<html><p>ID of the object of the queue for which the stored procedure was activated. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_activated_tasks", "procedure_name", "<html><p>Name of the activated stored procedure. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_activated_tasks", "execute_as"    , "<html><p>ID of the user that the stored procedure runs as. NULLABLE.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_broker_activated_tasks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_broker_forwarded_messages
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_broker_forwarded_messages",  "<p>Returns a row for each Service Broker message that an instance of SQL Server is in the process of forwarding.</p>");

			// Column names and description
			mtd.addColumn("dm_broker_forwarded_messages", "conversation_id"        , "<html><p>ID of the conversation to which this message belongs. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "is_initiator"           , "<html><p>Indicates whether this message is from the initiator of the conversation. NULLABLE.</p><p>0 = Not from initiator </p><p>1 = From initiator</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "to_service_name"        , "<html><p>Name of the service to which this message is sent. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "to_broker_instance"     , "<html><p>Identifier of the broker that hosts the service to which this message is sent. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "from_service_name"      , "<html><p>Name of the service that this message is from. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "from_broker_instance"   , "<html><p>Identifier of the broker that hosts the service that this message is from. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "adjacent_broker_address", "<html><p>Network address to which this message is being sent. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "message_sequence_number", "<html><p>Sequence number of the message in the dialog box. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "message_fragment_number", "<html><p>If the dialog message is fragmented, this is the fragment number that this transport message contains. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "hops_remaining"         , "<html><p>Number of times the message may be retransmitted before reaching the final destination. Every time the message is forwarded, this number decreases by 1. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "time_to_live"           , "<html><p>Maximum time for the message to remain active. When this reaches 0, the message is discarded. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "time_consumed"          , "<html><p>Time that the message has already been active. Every time the message is forwarded, this number is increased by the time it has taken to forward the message. Not NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_forwarded_messages", "message_id"             , "<html><p>ID of the message. NULLABLE.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_broker_forwarded_messages' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_broker_connections
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_broker_connections",  "<p>Returns a row for each Service Broker network connection. The following table provides more information:</p>");

			// Column names and description
			mtd.addColumn("dm_broker_connections", "connection_id"             , "<html><p>Identifier of the connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "transport_stream_id"       , "<html><p>Identifier of the SQL Server?Network Interface (SNI) connection used by this connection for TCP/IP communications. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "state"                     , "<html><p>Current state of the connection. NULLABLE. Possible values:</p><p>1 = NEW</p><p>2 = CONNECTING</p><p>3 = CONNECTED</p><p>4 = LOGGED_IN</p><p>5 = CLOSED </p></html>");
			mtd.addColumn("dm_broker_connections", "state_desc"                , "<html><p>Current state of the connection. NULLABLE. Possible values:</p><ul class=\"unordered\"> <li><p>NEW</p></li> <li><p>CONNECTING</p></li> <li><p>CONNECTED</p></li> <li><p>LOGGED_IN</p></li> <li><p>CLOSED </p></li></ul><p></p></html>");
			mtd.addColumn("dm_broker_connections", "connect_time"              , "<html><p>Date and time at which the connection was opened. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "login_time"                , "<html><p>Date and time at which login for the connection succeeded. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "authentication_method"     , "<html><p>Name of the Windows Authentication method, such as NTLM or KERBEROS. The value comes from Windows. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "principal_name"            , "<html><p>Name of the login that was validated for connection permissions. For Windows Authentication, this value is the remote user name. For certificate authentication, this value is the certificate owner. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "remote_user_name"          , "<html><p>Name of the peer user from the other database that is used by Windows Authentication. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "last_activity_time"        , "<html><p>Date and time at which the connection was last used to send or receive information. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "is_accept"                 , "<html><p>Indicates whether the connection originated on the remote side. NULLABLE.</p><p>1 = The connection is a request accepted from the remote instance. </p><p>0 = The connection was started by the local instance.</p></html>");
			mtd.addColumn("dm_broker_connections", "login_state"               , "<html><p>State of the login process for this connection. Possible values: </p><p>0 = INITIAL</p><p>1 = WAIT LOGIN NEGOTIATE</p><p>2 = ONE ISC</p><p>3 = ONE ASC</p><p>4 = TWO ISC</p><p>5 = TWO ASC</p><p>6 = WAIT ISC Confirm</p><p>7 = WAIT ASC Confirm</p><p>8 = WAIT REJECT</p><p>9 = WAIT PRE-MASTER SECRET</p><p>10 = WAIT VALIDATION</p><p>11 = WAIT ARBITRATION</p><p>12 = ONLINE</p><p>13 = ERROR?</p></html>");
			mtd.addColumn("dm_broker_connections", "login_state_desc"          , "<html><p>Current state of login from the remote computer. Possible values:</p><ul class=\"unordered\"> <li><p>Connection handshake is initializing.</p></li> <li><p>Connection handshake is waiting for Login Negotiate message.</p></li> <li><p>Connection handshake has initialized and sent security context for authentication.</p></li> <li><p>Connection handshake has received and accepted security context for authentication.</p></li> <li><p>Connection handshake has initialized and sent security context for authentication. There is an optional mechanism available for authenticating the peers.</p></li> <li><p>Connection handshake has received and sent accepted security context for authentication. There is an optional mechanism available for authenticating the peers.</p></li> <li><p>Connection handshake is waiting for Initialize Security Context Confirmation message.</p></li> <li><p>Connection handshake is waiting for Accept Security Context Confirmation message.</p></li> <li><p>Connection handshake is waiting for SSPI rejection message for failed authentication.</p></li> <li><p>Connection handshake is waiting for Pre-Master Secret message. </p></li> <li><p>Connection handshake is waiting for Validation message.</p></li> <li><p>Connection handshake is waiting for Arbitration message.</p></li> <li><p>Connection handshake is complete and is online (ready) for message exchange.</p></li> <li><p>Connection is in error.</p></li></ul></html>");
			mtd.addColumn("dm_broker_connections", "peer_certificate_id"       , "<html><p>The local object ID of the certificate that is used by the remote instance for authentication. The owner of this certificate must have CONNECT permissions to the Service Broker endpoint. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "encryption_algorithm"      , "<html><p>Encryption algorithm that is used for this connection. NULLABLE. Possible values: </p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value </p></th>     <th><p>Description </p></th>     <th><p>Corresponding DDL option </p></th>    </tr>    <tr>     <td data-th=\"Value \"><p>0</p></td>     <td data-th=\"Description \"><p>NONE </p></td>     <td data-th=\"Corresponding DDL option \"><p>Disabled</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>1</p></td>     <td data-th=\"Description \"><p>RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>{Required | Required algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>2</p></td>     <td data-th=\"Description \"><p>AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required algorithm AES</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>3</p></td>     <td data-th=\"Description \"><p>NONE, RC4</p></td>     <td data-th=\"Corresponding DDL option \"><p>{Supported | Supported algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>4</p></td>     <td data-th=\"Description \"><p>NONE, AES</p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported algorithm RC4</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>5</p></td>     <td data-th=\"Description \"><p>RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>6</p></td>     <td data-th=\"Description \"><p>AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm AES RC4</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>7</p></td>     <td data-th=\"Description \"><p>NONE, RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Value \"><p>8</p></td>     <td data-th=\"Description \"><p>NONE, AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported algorithm AES RC4</p></td>    </tr>   </tbody>  </table> </div></div><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>The RC4 algorithm is only supported for backward compatibility. New material can only be encrypted using RC4 or RC4_128 when the database is in compatibility level 90 or 100. (Not recommended.) Use a newer algorithm such as one of the AES algorithms instead. In SQL Server 2012 and later versions, material encrypted using RC4 or RC4_128 can be decrypted in any compatibility level.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_broker_connections", "encryption_algorithm_desc" , "<html><p>Textual representation of the encryption algorithm. NULLABLE. Possible Values:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Description </p></th>     <th><p>Corresponding DDL option </p></th>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE </p></td>     <td data-th=\"Corresponding DDL option \"><p>Disabled</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>{Required | Required Algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm AES</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, RC4</p></td>     <td data-th=\"Corresponding DDL option \"><p>{Supported | Supported Algorithm RC4}</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, AES</p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm RC4</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Required Algorithm AES RC4</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, RC4, AES </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm RC4 AES</p></td>    </tr>    <tr>     <td data-th=\"Description \"><p>NONE, AES, RC4 </p></td>     <td data-th=\"Corresponding DDL option \"><p>Supported Algorithm AES RC4</p></td>    </tr>   </tbody>  </table> </div></div><p></p></html>");
			mtd.addColumn("dm_broker_connections", "receives_posted"           , "<html><p>Number of asynchronous network receives that have not yet completed for this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "is_receive_flow_controlled", "<html><p>Whether network receives have been postponed due to flow control because the network is busy. NULLABLE.</p><p>1 = True</p></html>");
			mtd.addColumn("dm_broker_connections", "sends_posted"              , "<html><p>The number of asynchronous network sends that have not yet completed for this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "is_send_flow_controlled"   , "<html><p>Whether network sends have been postponed due to network flow control because the network is busy. NULLABLE.</p><p>1 = True</p></html>");
			mtd.addColumn("dm_broker_connections", "total_bytes_sent"          , "<html><p>Total number of bytes that were sent by this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "total_bytes_received"      , "<html><p>Total number of bytes that were received by this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "total_fragments_sent"      , "<html><p>Total number of Service Broker message fragments that were sent by this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "total_fragments_received"  , "<html><p>Total number of Service Broker message fragments that were received by this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "total_sends"               , "<html><p>Total number of network send requests that were issued by this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "total_receives"            , "<html><p>Total number of network receive requests that were issued by this connection. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_connections", "peer_arbitration_id"       , "<html><p>Internal identifier for the endpoint. NULLABLE.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_broker_connections' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_broker_queue_monitors
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_broker_queue_monitors",  "<p>Returns a row for each queue monitor in the instance. A queue monitor manages activation for a queue. </p>");

			// Column names and description
			mtd.addColumn("dm_broker_queue_monitors", "database_id"           , "<html><p>Object identifier for the database that contains the queue that the monitor watches. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_queue_monitors", "queue_id"              , "<html><p>Object identifier for the queue that the monitor watches. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_queue_monitors", "state"                 , "<html><p>State of the monitor. NULLABLE. This is one of the following:</p><ul class=\"unordered\"> <li><p>INACTIVE</p></li> <li><p>NOTIFIED</p></li> <li><p>RECEIVES_OCCURRING</p></li></ul></html>");
			mtd.addColumn("dm_broker_queue_monitors", "last_empty_rowset_time", "<html><p>Last time that a RECEIVE from the queue returned an empty result. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_queue_monitors", "last_activated_time"   , "<html><p>Last time that this queue monitor activated a stored procedure. NULLABLE.</p></html>");
			mtd.addColumn("dm_broker_queue_monitors", "tasks_waiting"         , "<html><p>Number of sessions that are currently waiting within a RECEIVE statement for this queue. NULLABLE.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>This number includes any session executing a receive statement, regardless of whether the queue monitor started the session. This is if you use WAITFOR together with RECEIVE. Basically, these tasks are waiting for messages to arrive on the queue.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_broker_queue_monitors' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_objects_disabled_on_compatibility_level_change
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_objects_disabled_on_compatibility_level_change",  "<p>Lists the indexes and constraints that will be disabled as a result of changing compatibility level in SQL Server. Indexes and constraints that contain persisted computed columns whose expressions use spatial UDTs will be disabled after upgrading or changing compatibility level. Use this dynamic management function to determine the impact of a change in compatibility level.</p>");

			// Column names and description
			mtd.addColumn("dm_db_objects_disabled_on_compatibility_level_change", "class"     , "<html><p>1 = constraints</p><p>7 = indexes and heaps</p></html>");
			mtd.addColumn("dm_db_objects_disabled_on_compatibility_level_change", "class_desc", "<html><p>OBJECT or COLUMN for constraints </p><p>INDEX for indexes and heaps</p></html>");
			mtd.addColumn("dm_db_objects_disabled_on_compatibility_level_change", "major_id"  , "<html><p>OBJECT ID of constraints</p><p>OBJECT ID of table that contains indexes and heaps</p></html>");
			mtd.addColumn("dm_db_objects_disabled_on_compatibility_level_change", "minor_id"  , "<html><p>NULL for constraints</p><p>Index_id for indexes and heaps</p></html>");
			mtd.addColumn("dm_db_objects_disabled_on_compatibility_level_change", "dependency", "<html><p>Description of the dependency that is causing the constraint or index to be disabled. The same values are also used in the warnings that are raised during upgrade. Examples include the following:</p><ul class=\"unordered\"> <li><p>\"space\" for an intrinsic</p></li> <li><p>\"geometry\" for a system UDT</p></li> <li><p>\"geography::Parse\" for a method of a system UDT</p></li></ul></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_objects_disabled_on_compatibility_level_change' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_diag_processing_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_diag_processing_stats",  "<p>Displays information related to all internal diagnostic events that could be incorporated into diagnostic sessions defined by the administrator. Query this view to understand the statistics behind the diagnostics and eventing subsystems that drive the population of all the other DMVs. There are a group of queues for each process on each node.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_diag_processing_stats", "pdw_node_id"      , "<html><p>Appliance node this log is from.</p></html>");
			mtd.addColumn("dm_pdw_diag_processing_stats", "process_id"       , "<html><p>Identifier of the process running submitting this statistic.</p></html>");
			mtd.addColumn("dm_pdw_diag_processing_stats", "target_name"      , "<html><p>The name of the queue.</p></html>");
			mtd.addColumn("dm_pdw_diag_processing_stats", "queue_size"       , "<html><p>The number of items in the process queue. The queue size is usually 0. A positive number indicates that the system is under stress and is building backlog of events. A positive count in the other columns means system has become corrupted for that particular queue and any related DMVs.</p></html>");
			mtd.addColumn("dm_pdw_diag_processing_stats", "lost_events_count", "<html><p>The number of events lost.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_diag_processing_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_dms_cores
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_dms_cores",  "<p>Holds information about all DMS services running on the Compute nodes of the appliance. It lists one row per service instance, which is currently one row per node. </p>");

			// Column names and description
			mtd.addColumn("dm_pdw_dms_cores", "dms_core_id", "<html><p>Unique numeric id associated with this DMS core.</p><p>Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_dms_cores", "pdw_node_id", "<html><p>ID of the node on which this DMS service is running.</p></html>");
			mtd.addColumn("dm_pdw_dms_cores", "status"     , "<html><p>Current status of the DMS service.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_dms_cores' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_dms_workers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_dms_workers",  "<p>Holds information about all workers completing DMS steps. </p>");

			// Column names and description
			mtd.addColumn("dm_pdw_dms_workers", "request_id"        , "<html><p>Query that this DMS worker is part of.</p><p><span class=\"literal\">request_id</span>, <span class=\"literal\">step_index</span>, and <span class=\"literal\">dms_step_index</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "step_index"        , "<html><p>Query step this DMS worker is part of.</p><p><span class=\"literal\">request_id</span>, <span class=\"literal\">step_index</span>, and <span class=\"literal\">dms_step_index</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "dms_step_index"    , "<html><p>Step in the DMS plan that this worker is running.</p><p><span class=\"literal\">request_id</span>, <span class=\"literal\">step_index</span>, and <span class=\"literal\">dms_step_index</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "pdw_node_id"       , "<html><p>Node that the worker is running on.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "distribution_id"   , "<html><p>Distribution that the worker is running on, if any.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "type"              , "<html><p>Type of DMS worker thread this entry represents.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "status"            , "<html><p>Status of the DMS worker.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "bytes_per_sec"     , "<html><p>Read or write throughput in the last second.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "bytes_processed"   , "<html><p>Total bytes processed by this worker.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "rows_processed"    , "<html><p>Number of rows read or written for this worker.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "start_time"        , "<html><p>Time at which execution of this worker started.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "end_time"          , "<html><p>Time at which execution ended, failed, or was cancelled.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "total_elapsed_time", "<html><p>Total time spent in execution, in milliseconds.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "cpu_time"          , "<html><p>CPU time consumed by this worker, in milliseconds.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "query_time"        , "<html><p>Period of time before SQL starts returning rows to the thread, in milliseconds.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "buffers_available" , "<html><p>Number of unused buffers. </p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "sql_spid"          , "<html><p>Session id on the SQL Server instance performing the work for this DMS worker.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "dms_cpid"          , "<html><p>Process ID of the actual thread running.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "error_id"          , "<html><p>Unique identifier of the error that occurred during execution of this worker, if any.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "source_info"       , "<html><p>For a reader, specification of the source tables and columns.</p></html>");
			mtd.addColumn("dm_pdw_dms_workers", "destination_info"  , "<html><p>For a writer, specification of the destination tables.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_dms_workers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_errors
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_errors",  "<p>Holds information about all errors encountered during execution of a request or query.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_errors", "error_id"   , "<html><p>Key for this view.</p><p>Unique numeric id associated with the error.</p></html>");
			mtd.addColumn("dm_pdw_errors", "source"     , "<html><p>Information not available.</p></html>");
			mtd.addColumn("dm_pdw_errors", "type"       , "<html><p>Type of error that occurred.</p></html>");
			mtd.addColumn("dm_pdw_errors", "create_time", "<html><p>Time at which the error occurred.</p></html>");
			mtd.addColumn("dm_pdw_errors", "pwd_node_id", "<html><p>Identifier of the specific node involved, if any. For additional information on node ids, see <a href=\"https://msdn.microsoft.com/en-us/library/mt203907.aspx\">sys.dm_pdw_nodes (SQL Data Warehouse)</a>. </p></html>");
			mtd.addColumn("dm_pdw_errors", "session_id" , "<html><p>Identifier of the session involved, if any. For additional information on session ids, see <a href=\"https://msdn.microsoft.com/en-us/library/mt203883.aspx\">sys.dm_pdw_exec_sessions (SQL Data Warehouse)</a>.</p></html>");
			mtd.addColumn("dm_pdw_errors", "request_id" , "<html><p>Identifier of the request involved, if any. For additional information on request ids, see <a href=\"https://msdn.microsoft.com/en-us/library/mt203887.aspx\">sys.dm_pdw_exec_requests (SQL Data Warehouse)</a>.</p></html>");
			mtd.addColumn("dm_pdw_errors", "spid"       , "<html><p>spid of the SQL Server session involved, if any.</p></html>");
			mtd.addColumn("dm_pdw_errors", "thread_id"  , "<html><p>Information not available.</p></html>");
			mtd.addColumn("dm_pdw_errors", "details"    , "<html><p>Holds the full error text description.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_errors' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_exec_connections
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_exec_connections",  "<p>Returns information about the connections established to this instance of SQL Data Warehouse and the details of each connection.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_exec_connections", "session_id"    , "<html><p>Identifies the session associated with this connection. Use <span class=\"code\">SESSION_ID()</span> to return the <span class=\"code\">session_id</span> of the current connection.</p></html>");
			mtd.addColumn("dm_pdw_exec_connections", "connect_time"  , "<html><p>Timestamp when connection was established. Is not nullable.</p></html>");
			mtd.addColumn("dm_pdw_exec_connections", "encrypt_option", "<html><p>Indicates TRUE (connection is encrypted) or FALSE (connection is not enctypred). </p></html>");
			mtd.addColumn("dm_pdw_exec_connections", "auth_scheme"   , "<html><p>Specifies SQL Server/Windows Authentication scheme used with this connection. Is not nullable.</p></html>");
			mtd.addColumn("dm_pdw_exec_connections", "client_id"     , "<html><p>IP address of the client connecting to this server. Is nullable.</p></html>");
			mtd.addColumn("dm_pdw_exec_connections", "sql_spid"      , "<html><p>The server process ID of the connection. Use <span class=\"code\">@@SPID</span> to return the <span class=\"code\">sql_spid</span> of the current connection.For most purposed, use the <span class=\"code\">session_id</span> instead.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_exec_connections' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_exec_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_exec_requests",  "<p>Holds information about all requests currently or recently active in SQL Data Warehouse. It lists one row per request/query.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_exec_requests", "request_id"        , "<html><p>Key for this view. Unique numeric id associated with the request.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "session_id"        , "<html><p>Unique numeric id associated with the session in which this query was run. See <a href=\"https://msdn.microsoft.com/en-us/library/mt203883.aspx\">sys.dm_pdw_exec_sessions (SQL Data Warehouse)</a>.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "status"            , "<html><p>Current status of the request.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "submit_time"       , "<html><p>Time at which the request was submitted for execution.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "start_time"        , "<html><p>Time at which the request execution was started.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "end_compile_time"  , "<html><p>Time at which the engine completed compiling the request.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "end_time"          , "<html><p>Time at which the request execution completed, failed, or was cancelled.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "total_elapsed_time", "<html><p>Time elapsed in execution since the request was started, in milliseconds.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "label"             , "<html><p>Optional label string associated with some SELECT query statements.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "error_id"          , "<html><p>Unique id of the error associated with the request, if any.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "database_id"       , "<html><p>Identifier of database used by explicit context (e.g., USE DB_X).</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "command"           , "<html><p>Holds the full text of the request as submitted by the user.</p></html>");
			mtd.addColumn("dm_pdw_exec_requests", "resource_class"    , "<html><p>The resource class for this request. See related <strong>concurrency_slots_used</strong> in <a href=\"https://msdn.microsoft.com/en-us/library/mt203906.aspx\">sys.dm_pdw_resource_waits (SQL Data Warehouse)</a>.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_exec_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_exec_sessions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_exec_sessions",  "<p>Holds information about all sessions currently or recently open on the appliance. It lists one row per session.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_exec_sessions", "session_id"      , "<html><p>The id of the current query or the last query run (if the session is TERMINATED and the query was executing at time of termination). Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "status"          , "<html><p>For current sessions, identifies whether the session is currently active or idle. For past sessions, the session status may show closed or killed (if the session was forcibly closed).</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "request_id"      , "<html><p>The id of the current query or last query run.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "security_id"     , "<html><p>Security ID of the principal running the session.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "login_name"      , "<html><p>The login name of the principal running the session.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "login_time"      , "<html><p>Date and time at which the user logged in and this session was created. </p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "query_count"     , "<html><p>Captures the number of queries/requests?this session has run since creation.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "is_transactional", "<html><p>Captures whether a session is currently within a transaction or not.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "client_id"       , "<html><p>Captures client information for the session.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "app_name"        , "<html><p>Captures application name information optionally set as part of the connection process.</p></html>");
			mtd.addColumn("dm_pdw_exec_sessions", "sql_spid"        , "<html><p>The id number of the SPID. Use the <span class=\"code\">session_id</span> this session. Use the <span class=\"code\">sql_spid</span> column to join to <strong>sys.dm_pdw_nodes_exec_sessions</strong>.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-warning\" alt=\"System_CAPS_warning\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC174466.jpeg\" title=\"System_CAPS_warning\" xmlns=\"\"></span><span class=\"alertTitle\">Warning </span></th>    </tr>    <tr>     <td><p>This column contains closed SPIDs.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_exec_sessions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_lock_waits
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_lock_waits",  "<p>Holds information about the requests that are waiting for locks.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_lock_waits", "wait_id"     , "<html><p>Position of the request in the waiting list.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "session_id"  , "<html><p>ID of the session in which the wait state occurred.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "type"        , "<html><p>Type of wait this entry represents.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "object_type" , "<html><p>Type of object that is affected by the wait.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "object_name" , "<html><p>Name or GUID of the specified object that was affected by the wait.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "request_id"  , "<html><p>ID of the request on which the wait state occurred.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "request_time", "<html><p>Time at which the lock or resource was requested.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "acquire_time", "<html><p>Time at which the lock or resource was acquired.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "state"       , "<html><p>State of the wait state.</p></html>");
			mtd.addColumn("dm_pdw_lock_waits", "priority"    , "<html><p>Priority of the waiting item.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_lock_waits' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_network_credentials
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_network_credentials",  "<p>Returns a list of all network credentials stored in the SQL Data Warehouse appliance for all target servers. Results are listed for the Control node, and every Compute node.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_network_credentials", "pdw_node_id"       , "<html><p>Unique numeric id associated with the node.</p></html>");
			mtd.addColumn("dm_pdw_network_credentials", "target_server_name", "<html><p>IP address of the target server that SQL Data Warehouse will access by using the username and password credentials. </p></html>");
			mtd.addColumn("dm_pdw_network_credentials", "username"          , "<html><p>Username for which the password is stored.</p></html>");
			mtd.addColumn("dm_pdw_network_credentials", "last_modified"     , "<html><p>Datetime of the last operation that modified the credential.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_network_credentials' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_node_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_node_status",  "<p>Holds additional information (over <a href=\"https://msdn.microsoft.com/en-us/library/mt203907.aspx\">sys.dm_pdw_nodes (SQL Data Warehouse)</a>) about the performance and status of all appliance nodes. It lists one row per node in the appliance.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_node_status", "pdw_node_id"       , "<html><p>Unique numeric id associated with the node. </p><p>Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "process_id"        , "<html><p>Information not available.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "process_name"      , "<html><p>Information not available.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "allocated_memory"  , "<html><p>Total allocated memory on this node.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "available_memory"  , "<html><p>Total available memory on this node.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "process_cpu_usage" , "<html><p>Total process CPU usage, in ticks.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "total_cpu_usage"   , "<html><p>Total CPU usage, in ticks.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "thread_count"      , "<html><p>Total number of threads in use on this node.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "handle_count"      , "<html><p>Total number of handles in use on this node.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "total_elapsed_time", "<html><p>Total time elapsed since system start or restart.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "is_available"      , "<html><p>Flag indicating whether this node is available.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "sent_time"         , "<html><p>Last time a network package was sent by this node.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "received_time"     , "<html><p>Last time a network package was received by this node.</p></html>");
			mtd.addColumn("dm_pdw_node_status", "error_id"          , "<html><p>Unique identifier of the last error that occurred on this node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_node_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_nodes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_nodes",  "<p>Holds information about all of the nodes in Analytics Platform System. It lists one row per node in the appliance.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_nodes", "pdw_node_id", "<html><p>Unique numeric id associated with the node.</p><p>Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_nodes", "type"       , "<html><p>Type of the node.</p></html>");
			mtd.addColumn("dm_pdw_nodes", "name"       , "<html><p>Logical name of the node.</p></html>");
			mtd.addColumn("dm_pdw_nodes", "address"    , "<html><p>IP address of this node.</p></html>");
			mtd.addColumn("dm_pdw_nodes", "is_passive" , "<html><p>Indicates whether the virtual machine running the node is running on the assigned server or has failed over to the spare server.</p></html>");
			mtd.addColumn("dm_pdw_nodes", "region"     , "<html><p>The region where the node is running.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_nodes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_nodes_database_encryption_keys
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_nodes_database_encryption_keys",  "<p>Returns information about the encryption state of a database and its associated database encryption keys. <strong>sys.dm_pdw_nodes_database_encryption_keys</strong> provides this information for each node. For more information about database encryption, see <span class=\"unresolvedLink\">Transparent Data Encryption (SQL Server PDW)</span>.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "database_id"         , "<html><p>ID of the physical database on each node.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "encryption_state"    , "<html><p>Indicates whether the database on this node is encrypted or not encrypted.</p><p>0 = No database encryption key present, no encryption</p><p>1 = Unencrypted</p><p>2 = Encryption in progress</p><p>3 = Encrypted</p><p>4 = Key change in progress</p><p>5 = Decryption in progress</p><p>6 = Protection change in progress (The certificate that is encrypting the database encryption key is being changed.)</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "create_date"         , "<html><p>Displays the date the encryption key was created.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "regenerate_date"     , "<html><p>Displays the date the encryption key was regenerated.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "modify_date"         , "<html><p>Displays the date the encryption key was modified.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "set_date"            , "<html><p>Displays the date the encryption key was applied to the database.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "opened_date"         , "<html><p>Shows when the database key was last opened.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "key_algorithm"       , "<html><p>Displays the algorithm that is used for the key.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "key_length"          , "<html><p>Displays the length of the key.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "encryptor_thumbprint", "<html><p>Shows the thumbprint of the encryptor.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "percent_complete"    , "<html><p>Percent complete of the database encryption state change. This will be 0 if there is no state change.</p></html>");
			mtd.addColumn("dm_pdw_nodes_database_encryption_keys", "node_id"             , "<html><p>Unique numeric id associated with the node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_nodes_database_encryption_keys' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_os_event_logs
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_os_event_logs",  "<p>Holds information regarding the different Windows Event logs on the different nodes.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_os_event_logs", "pdw_node_id"  , "<html><p>Appliance node this log is from.</p><p><span class=\"literal\">pdw_node_id</span> and <span class=\"literal\">log_name</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "log_name"     , "<html><p>Windows event log name.</p><p><span class=\"literal\">pdw_node_id</span> and <span class=\"literal\">log_name</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "log_source"   , "<html><p>Windows event log source name.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "event_id"     , "<html><p>ID of the event. Not unique.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "event_type"   , "<html><p>Type of the event, identifying severity.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "event_message", "<html><p>Details of the event.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "generate_time", "<html><p>Time the event was created.</p></html>");
			mtd.addColumn("dm_pdw_os_event_logs", "write_time"   , "<html><p>Time the event was actually written to the log.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_os_event_logs' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_os_performance_counters
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_os_performance_counters",  "<p>Contains information about Windows performance counters for the nodes in SQL Data Warehouse. </p>");

			// Column names and description
			mtd.addColumn("dm_pdw_os_performance_counters", "pdw_node_id"     , "<html><p>The ID of the node that contains the counter.</p><p>pdw_node_id and counter_name form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_os_performance_counters", "counter_name"    , "<html><p>Name of Windows performance counter.</p></html>");
			mtd.addColumn("dm_pdw_os_performance_counters", "counter_category", "<html><p>Name of Windows performance counter category.</p></html>");
			mtd.addColumn("dm_pdw_os_performance_counters", "instance_name"   , "<html><p>Name of the specific instance of the counter.</p></html>");
			mtd.addColumn("dm_pdw_os_performance_counters", "counter_value"   , "<html><p>Current value of the counter.</p></html>");
			mtd.addColumn("dm_pdw_os_performance_counters", "last_update_time", "<html><p>Timestamp of last time the value was updated.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_os_performance_counters' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_os_threads
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_os_threads",  "<p>Applies To: SQL Server 2014, SQL Server 2016 Preview</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_os_threads", "pdw_node_id"                 , "<html><p>The ID of the affected node.</p><p>pdw_node_id and thread_id form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "thread_id"                   , "<html><p>pdw_node_id and thread_id form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "process_id"                  , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "name"                        , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "priority"                    , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "start_time"                  , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "state"                       , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "wait_reason"                 , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "total_processor_elapsed_time", "<html><p>Total kernel time used by the thread.</p></html>");
			mtd.addColumn("dm_pdw_os_threads", "total_user_elapsed_time"     , "<html><p>Total user time used by the thread</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_os_threads' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_query_stats_xe
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_query_stats_xe",  "<p>This DMV is deprecated and will be removed in a future release. In this release, it returns 0 rows.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_query_stats_xe", "event"          , "<html><p>Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "event_id"       , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "create_time"    , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "session_id"     , "<html><p>The id for the session.</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "cpu"            , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "reads"          , "<html><p>Number of logical reads since the start of the event.</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "writes"         , "<html><p>Number of logical writes since the start of the event.</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "sql_text"       , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "client_app_name", "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "tsql_stack"     , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe", "pdw_node_id"    , "<html><p>Node on which this Xevent instance is running.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_query_stats_xe' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_query_stats_xe_file
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_query_stats_xe_file",  "<p>This DMV is deprecated and will be removed in a future release. In this release, it returns 0 rows.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_query_stats_xe_file", "event"      , "<html><p>Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe_file", "data"       , "<html><p>?</p></html>");
			mtd.addColumn("dm_pdw_query_stats_xe_file", "pdw_node_id", "<html><p>Node on which this Xevent instance is running.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_query_stats_xe_file' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_request_steps
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_request_steps",  "<p>Holds information about all steps that compose a given request or query in SQL Data Warehouse. It lists one row per query step.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_request_steps", "request_id"        , "<html><p><span class=\"literal\">request_id</span> and <span class=\"literal\">step_index</span> make up the key for this view.</p><p>Unique numeric id associated with the request.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "step_index"        , "<html><p><span class=\"literal\">request_id</span> and <span class=\"literal\">step_index</span> make up the key for this view.</p><p>The position of this step in the sequence of steps that make up the request.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "operation_type"    , "<html><p>Type of operation represented by this step.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "distribution_type" , "<html><p>Type of distribution this step will undergo.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "location_type"     , "<html><p>Where the step is running.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "status"            , "<html><p>Status of this step.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "error_id"          , "<html><p>Unique id of the error associated with this step, if any.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "start_time"        , "<html><p>Time at which the step started execution.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "end_time"          , "<html><p>Time at which this step completed execution, was cancelled, or failed.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "total_elapsed_time", "<html><p>Total amount of time the query step has been running, in milliseconds.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "row_count"         , "<html><p>Total number of rows changed or returned by this request.</p></html>");
			mtd.addColumn("dm_pdw_request_steps", "command"           , "<html><p>Holds the full text of the command of this step.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_request_steps' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_resource_waits
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_resource_waits",  "<p>Displays wait information for all resource types in SQL Data Warehouse.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_resource_waits", "wait_id"               , "<html><p>Position of the request in the waiting list.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "session_id"            , "<html><p>ID of the session in which the wait state occurred.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "type"                  , "<html><p>Type of wait this entry represents.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "object_type"           , "<html><p>Type of object that is affected by the wait.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "object_name"           , "<html><p>Name or GUID of the specified object that was affected by the wait.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "request_id"            , "<html><p>ID of the request on which the wait state occurred.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "request_time"          , "<html><p>Time at which the lock or resource was requested.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "acquire_time"          , "<html><p>Time at which the lock or resource was acquired.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "state"                 , "<html><p>State of the wait state.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "priority"              , "<html><p>Priority of the waiting item.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "concurrency_slots_used", "<html><p>Number of concurrency slots (32 max) reserved for this request.</p></html>");
			mtd.addColumn("dm_pdw_resource_waits", "resource_class"        , "<html><p>The resource class for this request.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_resource_waits' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_sql_requests
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_sql_requests",  "<p>Holds information about all SQL Server query distributions as part of a SQL step in the query.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_sql_requests", "request_id"        , "<html><p>Unique identifier of the query to which this SQL query distribution belongs.</p><p><span class=\"literal\">request_id</span>, <span class=\"literal\">step_index</span>, and <span class=\"literal\">distribution_id</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "step_index"        , "<html><p>Index of the query step this distribution is part of.</p><p><span class=\"literal\">request_id</span>, <span class=\"literal\">step_index</span>, and <span class=\"literal\">distribution_id</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "pwd_node_id"       , "<html><p>Unique identifier of the node on which this query distribution is run.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "distribution_id"   , "<html><p>Unique identifier of the distribution on which this query distribution is run.</p><p><span class=\"literal\">request_id</span>, <span class=\"literal\">step_index</span>, and <span class=\"literal\">distribution_id</span> form the key for this view.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "status"            , "<html><p>Current status of the query distribution.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "error_id"          , "<html><p>Unique identifier of the error associated with this query distribution, if any.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "start_time"        , "<html><p>Time at which query distribution started execution.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "end_time"          , "<html><p>Time at which this query distribution completed execution, was cancelled, or failed.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "total_elapsed_time", "<html><p>Represents the time the query distribution has been running, in milliseconds.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "row_count"         , "<html><p>Number of rows changed or read by this query distribution.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "spid"              , "<html><p>Session id on the SQL Server instance running the query distribution.</p></html>");
			mtd.addColumn("dm_pdw_sql_requests", "command"           , "<html><p>Full text of command for this query distribution.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_sql_requests' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_sys_info
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_sys_info",  "<p>Provides a set of appliance-level counters that reflect overall activity on the appliance.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_sys_info", "total_sessions" , "<html><p>Number of sessions currently in the system.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "idle_sessions"  , "<html><p>Number of sessions currently idle.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "active_requests", "<html><p>Number of active requests currently running.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "queued_requests", "<html><p>Number of currently queued requests.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "active_loads"   , "<html><p>Number of loads currently running in the system.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "queued_loads"   , "<html><p>Number of queued loads waiting for execution.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "active_backups" , "<html><p>Number of backups currently running.</p></html>");
			mtd.addColumn("dm_pdw_sys_info", "active_restores", "<html><p>Number of backup restores currently running.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_sys_info' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_wait_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_wait_stats",  "<p>Holds information related to the SQL Server OS state related to instances running on the different nodes. For a list of waits types and their description, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179984%28v=sql.120%29.aspx\">sys.dm_os_wait_stats</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_wait_stats", "pdw_node_id"    , "<html><p>ID of the node this entry refers to.</p></html>");
			mtd.addColumn("dm_pdw_wait_stats", "wait_name"      , "<html><p>Name of the wait type.</p></html>");
			mtd.addColumn("dm_pdw_wait_stats", "max_wait_time"  , "<html><p>Maximum wait time of this wait type.</p></html>");
			mtd.addColumn("dm_pdw_wait_stats", "request_count"  , "<html><p>Number of waits of this wait type outstanding.</p></html>");
			mtd.addColumn("dm_pdw_wait_stats", "signal_time"    , "<html><p>Difference between the time that the waiting thread was signaled and when it started running.</p></html>");
			mtd.addColumn("dm_pdw_wait_stats", "completed_count", "<html><p>Total number of waits of this type completed since the last server restart.</p></html>");
			mtd.addColumn("dm_pdw_wait_stats", "wait_time"      , "<html><p>Total wait time for this wait type in millisecons. Inclusive of signal_time.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_wait_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_pdw_waits
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_pdw_waits",  "<p>Holds information about all wait states encountered during execution of a request or query, including locks, waits on transmission queues, and so on.</p>");

			// Column names and description
			mtd.addColumn("dm_pdw_waits", "wait_id"     , "<html><p>Unique numeric id associated with the wait state.</p><p>Key for this view.</p></html>");
			mtd.addColumn("dm_pdw_waits", "session_id"  , "<html><p>ID of the session on which the wait state occurred.</p></html>");
			mtd.addColumn("dm_pdw_waits", "type"        , "<html><p>Type of wait this entry represents.</p></html>");
			mtd.addColumn("dm_pdw_waits", "object_type" , "<html><p>Type of object that is affected by the wait.</p></html>");
			mtd.addColumn("dm_pdw_waits", "object_name" , "<html><p>Name or GUID of the specified object that was affected by the wait.</p></html>");
			mtd.addColumn("dm_pdw_waits", "request_id"  , "<html><p>ID of the request on which the wait state occurred.</p></html>");
			mtd.addColumn("dm_pdw_waits", "request_time", "<html><p>Time at which the wait state was requested.</p></html>");
			mtd.addColumn("dm_pdw_waits", "acquire_time", "<html><p>Time at which the lock or resource was acquired.</p></html>");
			mtd.addColumn("dm_pdw_waits", "state"       , "<html><p>State of the wait state.</p></html>");
			mtd.addColumn("dm_pdw_waits", "priority"    , "<html><p>Priority of the waiting item.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_pdw_waits' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_buffer_descriptors
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_buffer_descriptors",  "<p>Returns information about all the data pages that are currently in the SQL Server buffer pool. The output of this view can be used to determine the distribution of database pages in the buffer pool according to database, object, or type. In SQL Server 2016, this dynamic management view also returns information about the data pages in the buffer pool extension file. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/dn133176.aspx\">Buffer Pool Extension</a>.</p><p>When a data page is read from disk, the page is copied into the SQL Server buffer pool and cached for reuse. Each cached data page has one buffer descriptor. Buffer descriptors uniquely identify each data page that is currently cached in an instance of SQL Server. <span class=\"literal\">sys.dm_os_buffer_descriptors</span> returns cached pages for all user and system databases. This includes pages that are associated with the <span class=\"literal\">Resource</span> database.</p>");

			// Column names and description
			mtd.addColumn("dm_os_buffer_descriptors", "database_id"        , "<html><p>ID of database associated with the page in the buffer pool. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "file_id"            , "<html><p>ID of the file that stores the persisted image of the page. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "page_id"            , "<html><p>ID of the page within the file. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "page_level"         , "<html><p>Index level of the page. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "allocation_unit_id" , "<html><p>ID of the allocation unit of the page. This value can be used to join <span class=\"literal\">sys.allocation_units</span>. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "page_type"          , "<html><p>Type of the page, such as: Data page or Index page. Is nullable. </p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "row_count"          , "<html><p>Number of rows on the page. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "free_space_in_bytes", "<html><p>Amount of available free space, in bytes, on the page. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "is_modified"        , "<html><p>1 = Page has been modified after it was read from the disk. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "numa_node"          , "<html><p>Nonuniform Memory Access node for the buffer. Is nullable.</p></html>");
			mtd.addColumn("dm_os_buffer_descriptors", "read_microsec"      , "<html><p>The actual time (in microseconds) required to read the page into the buffer. This number is reset when the buffer is reused. Is nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_buffer_descriptors' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_child_instances
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_child_instances",  "<p>Returns a row for each user instance that has been created from the parent server instance. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-important\" alt=\"System_CAPS_important\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC160177.jpeg\" title=\"System_CAPS_important\" xmlns=\"\"></span><span class=\"alertTitle\">Important </span></th>    </tr>    <tr>     <td><p>This feature will be removed in a future version of Microsoft SQL Server. Avoid using this feature in new development work, and plan to modify applications that currently use this feature.</p></td>    </tr>   </tbody>  </table> </div></div><p>The information returned from <strong>sys.dm_os_child_instances</strong> can be used to determine the state of each User Instance (heart_beat) and to obtain the pipe name (instance_pipe_name) that can be used to create a connection to the User Instance using SQL Server Management Studio or SQLCmd. You can only connect to a User Instance after it has been started by an external process, such as a client application. SQL management tools cannot start a User Instance.</p>");

			// Column names and description
			mtd.addColumn("dm_os_child_instances", "owning_principal_name"        , "<html><p>The name of the user that this user instance was created for.</p></html>");
			mtd.addColumn("dm_os_child_instances", "owning_principal_sid"         , "<html><p>SID (Security-Identifier) of the principal who owns this user instance. This matches Windows SID.</p></html>");
			mtd.addColumn("dm_os_child_instances", "owning_principal_sid_binary ?", "<html><p>Binary version of the SID for the user who owns the user Instance</p></html>");
			mtd.addColumn("dm_os_child_instances", "instance_name"                , "<html><p>The name of this user instance.</p></html>");
			mtd.addColumn("dm_os_child_instances", "instance_pipe_name"           , "<html><p>When a user instance is created, a named pipe is created for applications to connect to. This name can be used in a connect string to connect to this user instance.</p></html>");
			mtd.addColumn("dm_os_child_instances", "os_process_id"                , "<html><p>The process number of the Windows process for this user instance.</p></html>");
			mtd.addColumn("dm_os_child_instances", "os_process_creation_date"     , "<html><p>The date and time when this user instance process was last started.</p></html>");
			mtd.addColumn("dm_os_child_instances", "heart_beat"                   , "<html><p>Current state of this user instance; either ALIVE or DEAD.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_child_instances' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_cluster_nodes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_cluster_nodes",  "<p>Returns one row for each node in the failover cluster instance configuration. If the current instance is a failover clustered instance, it returns a list of nodes on which this failover cluster instance (formerly \"virtual server\") has been defined. If the current server instance is not a failover clustered instance, it returns an empty rowset.</p>");

			// Column names and description
			mtd.addColumn("dm_os_cluster_nodes", "NodeName"          , "<html><p>Name of a node in the SQL Server failover cluster instance (virtual server) configuration.</p></html>");
			mtd.addColumn("dm_os_cluster_nodes", "status"            , "<html><p>Status of the node in a SQL Server failover cluster instance. For more information, see <a href=\"http://go.microsoft.com/fwlink/?LinkId=204794\">GetClusterNodeState Function</a>.</p><ul class=\"unordered\"> <li><p>0</p></li> <li><p>1</p></li> <li><p>2</p></li> <li><p>3</p></li> <li><p>-1</p></li></ul></html>");
			mtd.addColumn("dm_os_cluster_nodes", "status_description", "<html><p>Description of the status of the SQL Server failover cluster node.</p><ul class=\"unordered\"> <li><p>0 = up</p></li> <li><p>1 = down</p></li> <li><p>2 = paused</p></li> <li><p>3 = joining</p></li> <li><p>-1 = unknown</p></li></ul></html>");
			mtd.addColumn("dm_os_cluster_nodes", "is_current_owner"  , "<html><p>1 means this node is the current owner of the SQL Server failover cluster resource.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_cluster_nodes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_dispatcher_pools
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_dispatcher_pools",  "<p>Returns information about session dispatcher pools. Dispatcher pools are thread pools used by system components to perform background processing.</p>");

			// Column names and description
			mtd.addColumn("dm_os_dispatcher_pools", "dispatcher_pool_address" , "<html><p>The address of the dispatcher pool. <span class=\"literal\">dispatcher_pool_address</span> is unique. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "type"                    , "<html><p>The type of the dispatcher pool. Is not nullable. There are two types of dispatcher pools:</p><ul class=\"unordered\"> <li><p>DISP_POOL_XE_ENGINE</p></li> <li><p>DISP_POOL_XE_SESSION</p></li></ul></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "name"                    , "<html><p>The name of the dispatcher pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "dispatcher_count"        , "<html><p>The number of active dispatcher threads. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "dispatcher_ideal_count"  , "<html><p>The number of dispatcher threads that the dispatcher pool can grow to use. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "dispatcher_timeout_ms"   , "<html><p>The time, in milliseconds, that a dispatcher will wait for new work before exiting. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "dispatcher_waiting_count", "<html><p>The number of idle dispatcher threads. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_dispatcher_pools", "queue_length"            , "<html><p>The number of work items waiting to be handled by the dispatcher pool. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_dispatcher_pools' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_hosts
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_hosts",  "<p>Returns all the hosts currently registered in an instance of SQL Server. This view also returns the resources that are used by these hosts.?</p>");

			// Column names and description
			mtd.addColumn("dm_os_hosts", "host_address"                , "<html><p>Internal memory address of the host object.</p></html>");
			mtd.addColumn("dm_os_hosts", "type"                        , "<html><p>Type of hosted component. For example, </p><p>SOSHOST_CLIENTID_SERVERSNI= SQL Server Native Interface</p><p>SOSHOST_CLIENTID_SQLOLEDB = SQL Server Native Client OLE DB Provider</p><p>SOSHOST_CLIENTID_MSDART = Microsoft Data Access Run Time</p></html>");
			mtd.addColumn("dm_os_hosts", "name"                        , "<html><p>Name of the host.</p></html>");
			mtd.addColumn("dm_os_hosts", "enqueued_tasks_count"        , "<html><p>Total number of tasks that this host has placed onto queues in SQL Server.</p></html>");
			mtd.addColumn("dm_os_hosts", "active_tasks_count"          , "<html><p>Number of currently running tasks that this host has placed onto queues. </p></html>");
			mtd.addColumn("dm_os_hosts", "completed_ios_count"         , "<html><p>Total number of I/Os issued and completed through this host.</p></html>");
			mtd.addColumn("dm_os_hosts", "completed_ios_in_bytes"      , "<html><p>Total byte count of the I/Os completed through this host.</p></html>");
			mtd.addColumn("dm_os_hosts", "active_ios_count"            , "<html><p>Total number of I/O requests related to this host that are currently waiting to complete.</p></html>");
			mtd.addColumn("dm_os_hosts", "default_memory_clerk_address", "<html><p>Memory address of the memory clerk object associated with this host. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms175019.aspx\">sys.dm_os_memory_clerks (Transact-SQL)</a>.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_hosts' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_latch_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_latch_stats",  "<p>Returns information about all latch waits organized by class.</p>");

			// Column names and description
			mtd.addColumn("dm_os_latch_stats", "latch_class"           , "<html><p>Name of the latch class.</p></html>");
			mtd.addColumn("dm_os_latch_stats", "waiting_requests_count", "<html><p>Number of waits on latches in this class. This counter is incremented at the start of a latch wait. </p></html>");
			mtd.addColumn("dm_os_latch_stats", "wait_time_ms"          , "<html><p>Total wait time, in milliseconds, on latches in this class. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>This column is updated every five minutes during a latch wait and at the end of a latch wait.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_latch_stats", "max_wait_time_ms"      , "<html><p>Maximum time a memory object has waited on this latch. If this value is unusually high, it might indicate an internal deadlock.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_latch_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_loaded_modules
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_loaded_modules",  "<p>Returns a row for each module loaded into the server address space.</p>");

			// Column names and description
			mtd.addColumn("dm_os_loaded_modules", "base_address"   , "<html><p>Address of the module in the process.</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "file_version"   , "<html><p>Version of the file. Appears in the following format:</p><p>x.x:x.x</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "product_version", "<html><p>Version of the product. Appears in the following format:</p><p>x.x:x.x </p></html>");
			mtd.addColumn("dm_os_loaded_modules", "debug"          , "<html><p>1 = Module is a debug version of the loaded module.</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "patched"        , "<html><p>1 = Module has been patched. </p></html>");
			mtd.addColumn("dm_os_loaded_modules", "prerelease"     , "<html><p>1 = Module is a pre-release version of the loaded module. </p></html>");
			mtd.addColumn("dm_os_loaded_modules", "private_build"  , "<html><p>1 = Module is a private build of the loaded module.</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "special_build"  , "<html><p>1 = Module is a special build of the loaded module.</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "language"       , "<html><p>Language of version information of the module.</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "company"        , "<html><p>Name of company that created the module. </p></html>");
			mtd.addColumn("dm_os_loaded_modules", "description"    , "<html><p>Description of the module.</p></html>");
			mtd.addColumn("dm_os_loaded_modules", "name"           , "<html><p>Name of module. Includes the full path of the module.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_loaded_modules' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_brokers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_brokers",  "<p>Allocations that are internal to SQL Server use the SQL Server memory manager. Tracking the difference between process memory counters from <strong>sys.dm_os_process_memory</strong> and internal counters can indicate memory use from external components in the SQL Server memory space.</p><p>Memory brokers fairly distribute memory allocations between various components within SQL Server, based on current and projected usage. Memory brokers do not perform allocations. They only track allocations for computing distribution.</p><p>The following table provides information about memory brokers. </p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_brokers", "pool_id"                 , "<html><p>ID of the resource pool if it is associated with a Resource Governor pool.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "memory_broker_type"      , "<html><p>Type of memory broker. There are currently three types of memory brokers in SQL Server.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p><strong>MEMORYBROKER_FOR_CACHE</strong></p></td>     <td data-th=\"Description\"><p>Memory that is allocated for use by cached objects.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><strong>MEMORYBROKER_FOR_STEAL</strong></p></td>     <td data-th=\"Description\"><p>Memory that is stolen from the buffer pool. This memory is not available for reuse by other components until it is freed by the current owner.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><strong>MEMORYBROKER_FOR_RESERVE</strong></p></td>     <td data-th=\"Description\"><p>Memory reserved for future use by currently executing requests.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_brokers", "allocations_kb"          , "<html><p>Amount of memory, in kilobytes (KB), that has been allocated to this type of broker.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "allocations_kb_per_sec"  , "<html><p>Rate of memory allocations in kilobytes (KB) per second. This value can be negative for memory deallocations.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "predicted_allocations_kb", "<html><p>Predicted amount of allocated memory by the broker. This is based on the memory usage pattern.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "target_allocations_kb"   , "<html><p>Recommended amount of allocated memory, in kilobytes (KB), that is based on current settings and the memory usage pattern. This broker should grow to or shrink to this number.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "future_allocations_kb"   , "<html><p>Projected number of allocations, in kilobytes (KB), that will be done in the next several seconds.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "overall_limit_kb"        , "<html><p>Maximum amount of memory, in kilobytes (KB), that the the broker can allocate.</p></html>");
			mtd.addColumn("dm_os_memory_brokers", "last_notification"       , "<html><p>Memory usage recommendation that is based on the current settings and usage pattern. Valid values are as follows:</p><ul class=\"unordered\"> <li><p>grow</p></li> <li><p>shrink</p></li> <li><p>stable</p></li></ul></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_brokers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_cache_clock_hands
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_cache_clock_hands",  "<p>Returns the status of each hand for a specific cache clock.</p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_cache_clock_hands", "cache_address"           , "<html><p>Address of the cache associated with the clock. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "name"                    , "<html><p>Name of the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "type"                    , "<html><p>Type of cache store. There can be several caches of the same type. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "clock_hand"              , "<html><p>Type of hand. This is one of the following:</p><ul class=\"unordered\"> <li><p>External</p></li> <li><p>Internal</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "clock_status"            , "<html><p>Status of the clock. This is one of the following:</p><ul class=\"unordered\"> <li><p>Suspended</p></li> <li><p>Running</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "rounds_count"            , "<html><p>Number of sweeps made through the cache to remove entries. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "removed_all_rounds_count", "<html><p>Number of entries removed by all sweeps. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "updated_last_round_count", "<html><p>Number of entries updated during the last sweep. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "removed_last_round_count", "<html><p>Number of entries removed during the last sweep. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "last_tick_time"          , "<html><p>Last time, in milliseconds, that the clock hand moved. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "round_start_time"        , "<html><p>Time, in milliseconds, of the previous sweep. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_clock_hands", "last_round_start_time"   , "<html><p>Total time, in milliseconds, taken by the clock to complete the previous round. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_cache_clock_hands' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_cache_counters
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_cache_counters",  "<p>Returns a snapshot of the health of a cache in SQL Server. <strong>sys.dm_os_memory_cache_counters</strong> provides run-time information about the cache entries allocated, their use, and the source of memory for the cache entries. </p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_cache_counters", "cache_address"         , "<html><p>Indicates the address (primary key) of the counters associated with a specific cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "name"                  , "<html><p>Specifies the name of the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "type"                  , "<html><p>Indicates the type of cache that is associated with this entry. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "single_pages_kb"       , "<html><p>Amount, in kilobytes, of the single-page memory allocated. This is the amount of memory allocated by using the single-page allocator. This refers to the 8-KB pages that are taken directly from the buffer pool for this cache. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "pages_kb"              , "<html><p>Specifies the amount, in kilobytes, of the memory allocated in the cache. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "multi_pages_kb"        , "<html><p>Amount, in kilobytes, of the multipage memory allocated. This is the amount of memory allocated by using the multiple-page allocator of the memory node. This memory is allocated outside the buffer pool and takes advantage of the virtual allocator of the memory nodes. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "pages_in_use_kb"       , "<html><p>Specifies the amount, in kilobytes, of the memory that is allocated and in use in the cache. Is nullable. Values for objects of type <span class=\"code\">USERSTORE_&lt;*&gt;</span> are not tracked. NULL is reported for them.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "single_pages_in_use_kb", "<html><p>Amount, in kilobytes, of the single-page memory that is being used. Is nullable. This information is not tracked for objects of type USERSTORE_&lt;*&gt; and these values will be NULL.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "multi_pages_in_use_kb" , "<html><p>Amount, in kilobytes, of the multipage memory that is being used. NULLABLE. This information is not tracked for objects of type USERSTORE_&lt;*&gt;, and these values will be NULL.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "entries_count"         , "<html><p>Indicates the number of entries in the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_counters", "entries_in_use_count"  , "<html><p>Indicates the number of entries in the cache that is being used. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_cache_counters' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_cache_entries
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_cache_entries",  "<p>Returns information about all entries in caches in SQL Server. Use this view to trace cache entries to their associated objects. You can also use this view to obtain statistics on cache entries.</p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_cache_entries", "cache_address"         , "<html><p>Address of the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "name"                  , "<html><p>Name of the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "type"                  , "<html><p>Type of cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "entry_address"         , "<html><p>Address of the descriptor of the cache entry. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "entry_data_address"    , "<html><p>Address of the user data in the cache entry.</p><p>0x00000000 = Entry data address is not available. </p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "in_use_count"          , "<html><p>Number of concurrent users of this cache entry. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "is_dirty"              , "<html><p>Indicates whether this cache entry is marked for removal. 1 = marked for removal. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "disk_ios_count"        , "<html><p>Number of I/Os incurred while this entry was created. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "context_switches_count", "<html><p>Number of context switches incurred while this entry was created. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "original_cost"         , "<html><p>Original cost of the entry. This value is an approximation of the number of I/Os incurred, CPU instruction cost, and the amount of memory consumed by entry. The greater the cost, the lower the chance that the item will be removed from the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "current_cost"          , "<html><p>Current cost of the cache entry. This value is updated during the process of entry purging. Current cost is reset to its original value on entry reuse. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "memory_object_address" , "<html><p>Address of the associated memory object. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "pages_allocated_count" , "<html><p>Number of 8-KB pages to store this cache entry. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "pages_kb"              , "<html><p>Amount of memory in kilobytes (KB) used by this cache entry. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "entry_data"            , "<html><p>Serialized representation of the cached entry. This information is cache store dependant. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_entries", "pool_id"               , "<html><p>Resource pool id associated with entry. Is nullable.</p><p>not katmai</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_cache_entries' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_cache_hash_tables
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_cache_hash_tables",  "<p>Returns a row for each active cache in the instance of SQL Server. </p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_cache_hash_tables", "cache_address"               , "<html><p>Address (primary key) of the cache entry. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "name"                        , "<html><p>Name of the cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "type"                        , "<html><p>Type of cache. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "table_level"                 , "<html><p>Hash table number. A particular cache may have multiple hash tables that correspond to different hash functions. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_count"               , "<html><p>Number of buckets in the hash table. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_in_use_count"        , "<html><p>Number of buckets that are currently being used. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_min_length"          , "<html><p>Minimum number of cache entries in a bucket. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_max_length"          , "<html><p>Maximum number of cache entries in a bucket. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_avg_length"          , "<html><p>Average number of cache entries in each bucket. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_max_length_ever"     , "<html><p>Maximum number of cached entries in a hash bucket for this hash table since the server was started. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "hits_count"                  , "<html><p>Number of cache hits. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "misses_count"                , "<html><p>Number of cache misses. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_avg_scan_hit_length" , "<html><p>Average number of examined entries in a bucket before the searched for an item was found. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_cache_hash_tables", "buckets_avg_scan_miss_length", "<html><p>Average number of examined entries in a bucket before the search ended unsuccessfully. Is not </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_cache_hash_tables' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_clerks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_clerks",  "<p>Returns the set of all memory clerks that are currently active in the instance of SQL Server.?</p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_clerks", "memory_clerk_address"       , "<html><p>Specifies the unique memory address of the memory clerk. This is the primary key column. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "type"                       , "<html><p>Specifies the type of memory clerk. Every clerk has a specific type, such as CLR Clerks MEMORYCLERK_SQLCLR. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "name"                       , "<html><p>Specifies the internally assigned name of this memory clerk. A component can have several memory clerks of a specific type. A component might choose to use specific names to identify memory clerks of the same type. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "memory_node_id"             , "<html><p>Specifies the ID of the memory node. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "single_pages_kb"            , "<html><p></p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_clerks", "pages_kb"                   , "<html><p>Specifies the amount of page memory allocated in kilobytes (KB) for this memory clerk. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_clerks", "multi_pages_kb"             , "<html><p>Amount of multipage memory allocated in KB. This is the amount of memory allocated by using the multiple page allocator of the memory nodes. This memory is allocated outside the buffer pool and takes advantage of the virtual allocator of the memory nodes. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_clerks", "virtual_memory_reserved_kb" , "<html><p>Specifies the amount of virtual memory that is reserved by a memory clerk. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "virtual_memory_committed_kb", "<html><p>Specifies the amount of virtual memory that is committed by a memory clerk. The amount of committed memory should always be less than the amount of reserved memory. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "awe_allocated_kb"           , "<html><p>Specifies the amount of memory in kilobytes (KB) locked in the physical memory and not paged out by the operating system. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "shared_memory_reserved_kb"  , "<html><p>Specifies the amount of shared memory that is reserved by a memory clerk. The amount of memory reserved for use by shared memory and file mapping. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "shared_memory_committed_kb" , "<html><p>Specifies the amount of shared memory that is committed by the memory clerk. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "page_size_in_bytes"         , "<html><p>Specifies the granularity of the page allocation for this memory clerk. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "page_allocator_address"     , "<html><p>Specifies the address of the page allocator. This address is unique for a memory clerk and can be used in <strong>sys.dm_os_memory_objects</strong> to locate memory objects that are bound to this clerk. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_clerks", "host_address"               , "<html><p>Specifies the memory address of the host for this memory clerk. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms187800.aspx\">sys.dm_os_hosts (Transact-SQL)</a>. Components, such as Microsoft?SQL Server?Native Client, access SQL Server memory resources through the host interface. </p><p>0x00000000 = Memory clerk belongs to SQL Server. </p><p>Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_clerks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_nodes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_nodes",  "<p>Allocations that are internal to SQL Server use the SQL Server memory manager. Tracking the difference between process memory counters from <strong>sys.dm_os_process_memory</strong> and internal counters can indicate memory use from external components in the SQL Server memory space.</p><p>Nodes are created per physical NUMA memory nodes. These might be different from the CPU nodes in <strong>sys.dm_os_nodes</strong>.</p><p>No allocations done directly through Windows memory allocations routines are tracked. The following table provides information about memory allocations done only by using SQL Server memory manager interfaces. </p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_nodes", "memory_node_id"                    , "<html><p>Specifies the ID of the memory node. Related to <strong>memory_node_id</strong> of <strong>sys.dm_os_memory_clerks</strong>. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_nodes", "virtual_address_space_reserved_kb" , "<html><p>Indicates the number of virtual address reservations, in kilobytes (KB), which are neither committed nor mapped to physical pages. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_nodes", "virtual_address_space_committed_kb", "<html><p>Specifies the amount of virtual address, in KB, that has been committed or mapped to physical pages. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_nodes", "locked_page_allocations_kb"        , "<html><p>Specifies the amount of physical memory, in KB, that has been locked by SQL Server. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_nodes", "single_pages_kb"                   , "<html><p>Amount of committed memory, in KB, that is allocated by using the single page allocator by threads running on this node. This memory is allocated from the buffer pool. This value indicates the node where allocations request occurred, not the physical location where the allocation request was satisfied.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_nodes", "pages_kb"                          , "<html><p>Specifies the amount of committed memory, in KB, which is allocated from this NUMA node by Memory Manager Page Allocator. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_nodes", "multi_pages_kb"                    , "<html><p>Amount of committed memory, in KB, that is allocated by using the multipage allocator by threads running on this node. This memory is from outside the buffer pool. This value indicates the node where the allocation requests occurred, not the physical location where the allocation request was satisfied.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_nodes", "shared_memory_reserved_kb"         , "<html><p>Specifies the amount of shared memory, in KB, that has been reserved from this node. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_nodes", "shared_memory_committed_kb"        , "<html><p>Specifies the amount of shared memory, in KB, that has been committed on this node. Not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_nodes", "cpu_affinity_mask"                 , "<html><p>Internal use only. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_nodes", "online_scheduler_mask"             , "<html><p>Internal use only. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_nodes", "processor_group"                   , "<html><p>Internal use only. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_nodes", "foreign_committed_kb"              , "<html><p>Specifies the amount of committed memory, in KB, from other memory nodes. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_nodes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_objects
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_objects",  "<p>Returns memory objects that are currently allocated by SQL Server.?You can use <strong>sys.dm_os_memory_objects</strong> to analyze memory use and to identify possible memory leaks.</p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_objects", "memory_object_address"    , "<html><p>Address of the memory object. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "parent_address"           , "<html><p>Address of the parent memory object. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "pages_allocated_count"    , "<html><p>Number of pages that are allocated by this object. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_objects", "pages_in_bytes"           , "<html><p>Amount of memory in bytes that is allocated by this instance of the memory object. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_objects", "creation_options"         , "<html><p>Internal use only. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "bytes_used"               , "<html><p>Internal use only. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "type"                     , "<html><p>Type of memory object. </p><p>This indicates some component that this memory object belongs to, or the function of the memory object. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "name"                     , "<html><p>Internal use only. Nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "memory_node_id"           , "<html><p>ID of a memory node that is being used by this memory object. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "creation_time"            , "<html><p>Internal use only. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "max_pages_allocated_count", "<html><p>Maximum number of pages allocated by this memory object. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_objects", "page_size_in_bytes"       , "<html><p>Size of pages in bytes allocated by this object. Is not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_memory_objects", "max_pages_in_bytes"       , "<html><p>Maximum amount of memory ever used by this memory object. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "page_allocator_address"   , "<html><p>Memory address of page allocator. Is not nullable. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms175019.aspx\">sys.dm_os_memory_clerks (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "creation_stack_address"   , "<html><p>Internal use only. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "sequence_num"             , "<html><p>Internal use only. Is nullable.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "partition_type"           , "<html><p>The type of partition:</p><ul class=\"unordered\"> <li><p>0 - Non-partitionable memory object</p></li> <li><p>1 - Partitionable memory object, currently not partitioned</p></li> <li><p>2 - Partitionable memory object, partitioned by NUMA node. In an environment with a single NUMA node this is equivalent to 1.</p></li> <li><p>3 - Partitionable memory object, partitioned by CPU.</p></li></ul></html>");
			mtd.addColumn("dm_os_memory_objects", "contention_factor"        , "<html><p>A value specifying contention on this memory object, with 0 meaning no contention. The value is updated whenever a specified number of memory allocations were made reflecting contention during that period. Applies only to thread-safe memory objects.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "waiting_tasks_count"      , "<html><p>Number of waits on this memory object. This counter is incremented whenever memory is allocated from this memory object. The increment is the number of tasks currently waiting for access to this memory object. Applies only to thread-safe memory objects. This is a best effort value without a correctness guarantee.</p></html>");
			mtd.addColumn("dm_os_memory_objects", "exclusive_access_count"   , "<html><p>Specifies how often this memory object was accessed exclusively. Applies only to thread-safe memory objects. This is a best effort value without a correctness guarantee.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_objects' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_memory_pools
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_memory_pools",  "<p>Returns a row for each object store in the instance of SQL Server. You can use this view to monitor cache memory use and to identify bad caching behavior </p>");

			// Column names and description
			mtd.addColumn("dm_os_memory_pools", "memory_pool_address"        , "<html><p>Memory address of the entry that represents the memory pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_pools", "pool_id"                    , "<html><p>ID of a specific pool within a set of pools. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_pools", "type"                       , "<html><p>Type of object pool. Is not nullable. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms175019.aspx\">sys.dm_os_memory_clerks (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_os_memory_pools", "name"                       , "<html><p>System-assigned name of this memory object. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_pools", "max_free_entries_count"     , "<html><p>Maximum number of free entries that a pool can have. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_pools", "free_entries_count"         , "<html><p>Number of free entries currently in the pool. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_memory_pools", "removed_in_all_rounds_count", "<html><p>Number of entries removed from the pool since the instance of SQL Server was started. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_memory_pools' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_nodes
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_nodes",  "<p>An internal component named the SQLOS creates node structures that mimic hardware processor locality. These structures can be changed by using soft-NUMA to create custom node layouts. </p><p>The following table provides information about these nodes.</p>");

			// Column names and description
			mtd.addColumn("dm_os_nodes", "node_id"                     , "<html><p>ID of the node.</p></html>");
			mtd.addColumn("dm_os_nodes", "node_state_desc"             , "<html><p>Description of the node state. Values are displayed with the mutually exclusive values first, followed by the combinable values. For example:</p><p>Online, Thread Resources Low, Lazy Preemptive</p><p>There are four mutually exclusive <span class=\"literal\">node_state_desc</span> values:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">ONLINE</span></p></td>     <td data-th=\"Description\"><p>Node is online</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">OFFLINE</span></p></td>     <td data-th=\"Description\"><p>Node is offline</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">IDLE</span></p></td>     <td data-th=\"Description\"><p>Node has no pending work requests, and has entered an idle state.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">IDLE_READY</span></p></td>     <td data-th=\"Description\"><p>Node has no pending work requests, and is ready to enter an idle state.</p></td>    </tr>   </tbody>  </table> </div></div><p>There are three combinable <span class=\"literal\">node_state_desc</span> values:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">DAC</span></p></td>     <td data-th=\"Description\"><p>This node is reserved for the Dedicated Administrative Connection.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">THREAD_RESOURCES_LOW</span></p></td>     <td data-th=\"Description\"><p>No new threads can be created on this node because of a low-memory condition.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p><span class=\"literal\">HOT ADDED</span></p></td>     <td data-th=\"Description\"><p>Indicates the nodes were added in response to a hot add CPU event.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_nodes", "memory_object_address"       , "<html><p>Address of memory object associated with this node. One-to-one relation to <span class=\"literal\">sys.dm_os_memory_objects.memory_object_address</span>.</p></html>");
			mtd.addColumn("dm_os_nodes", "memory_clerk_address"        , "<html><p>Address of memory clerk associated with this node. One-to-one relation to <span class=\"literal\">sys.dm_os_memory_clerks.memory_clerk_address</span>.</p></html>");
			mtd.addColumn("dm_os_nodes", "io_completion_worker_address", "<html><p>Address of worker assigned to IO completion for this node. One-to-one relation to <span class=\"literal\">sys.dm_os_workers.worker_address</span>.</p></html>");
			mtd.addColumn("dm_os_nodes", "memory_node_id"              , "<html><p>ID of the memory node this node belongs to. Many-to-one relation to <span class=\"literal\">sys.dm_os_memory_nodes.memory_node_id</span>.</p></html>");
			mtd.addColumn("dm_os_nodes", "cpu_affinity_mask"           , "<html><p>Bitmap identifying the CPUs this node is associated with.</p></html>");
			mtd.addColumn("dm_os_nodes", "online_scheduler_count"      , "<html><p>Number of online schedulers that aremanaged by this node.</p></html>");
			mtd.addColumn("dm_os_nodes", "idle_scheduler_count"        , "<html><p>Number of online schedulers that have no active workers.</p></html>");
			mtd.addColumn("dm_os_nodes", "active_worker_count"         , "<html><p>Number of workers that are active on all schedulers managed by this node.</p></html>");
			mtd.addColumn("dm_os_nodes", "avg_load_balance"            , "<html><p>Average number of tasks per scheduler on this node.</p></html>");
			mtd.addColumn("dm_os_nodes", "timer_task_affinity_mask"    , "<html><p>Bitmap identifying the schedulers that can have timer tasks assigned to them.</p></html>");
			mtd.addColumn("dm_os_nodes", "permanent_task_affinity_mask", "<html><p>Bitmap identifying the schedulers that can have permanent tasks assigned to them.</p></html>");
			mtd.addColumn("dm_os_nodes", "resource_monitor_state"      , "<html><p>Each node has one resource monitor assigned to it. The resource monitor can be running or idle. A value of 1 indicates running, a value of 0 indicates idle.</p></html>");
			mtd.addColumn("dm_os_nodes", "online_scheduler_mask"       , "<html><p>Identifies the process affinity mask for this node. </p></html>");
			mtd.addColumn("dm_os_nodes", "processor_group"             , "<html><p>Identifies the group of processors for this node.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_nodes' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_performance_counters
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_performance_counters",  "<p>Returns a row per performance counter maintained by the server. For information about each performance counter, see <a href=\"https://msdn.microsoft.com/en-us/library/ms190382.aspx\">Use SQL Server Objects</a>.</p>");

			// Column names and description
			mtd.addColumn("dm_os_performance_counters", "object_name"  , "<html><p>Category to which this counter belongs. </p></html>");
			mtd.addColumn("dm_os_performance_counters", "counter_name" , "<html><p>Name of the counter. </p></html>");
			mtd.addColumn("dm_os_performance_counters", "instance_name", "<html><p>Name of the specific instance of the counter. Often contains the database name. </p></html>");
			mtd.addColumn("dm_os_performance_counters", "cntr_value"   , "<html><p>Current value of the counter. </p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>For per-second counters, this value is cumulative. The rate value must be calculated by sampling the value at discrete time intervals. The difference between any two successive sample values is equal to the rate for the time interval used.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_performance_counters", "cntr_type"    , "<html><p>Type of counter as defined by the Windows performance architecture. See <a href=\"http://msdn2.microsoft.com/library/aa394569.aspx\">WMI Performance Counter Types</a> on MSDN or your Windows Server documentation for more information on performance counter types.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_performance_counters' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_process_memory
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_process_memory",  "<p>Most memory allocations that are attributed to the SQL Server process space are controlled through interfaces that allow for tracking and accounting of those allocations. However, memory allocations might be performed in the SQL Server address space that bypasses internal memory management routines. Values are obtained through calls to the base operating system. They?are not manipulated by methods internal to SQL Server, except when it adjusts for locked or large page allocations.</p><p>All returned values that indicate memory sizes are shown in kilobytes (KB). The column <strong>total_virtual_address_space_reserved_kb</strong> is a duplicate of <strong>virtual_memory_in_bytes</strong> from <strong>sys.dm_os_sys_info</strong>.</p><p>The following table provides a complete picture of the process address space. </p>");

			// Column names and description
			mtd.addColumn("dm_os_process_memory", "physical_memory_in_use_kb"         , "<html><p>Indicates the process working set in KB, as reported by operating system, as well as tracked allocations by using large page APIs. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "large_page_allocations_kb"         , "<html><p>Specifies physical memory allocated by using large page APIs. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "locked_page_allocations_kb"        , "<html><p>Specifies memory pages locked in memory. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "total_virtual_address_space_kb"    , "<html><p>Indicates the total size of the user mode part of the virtual address space. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "virtual_address_space_reserved_kb" , "<html><p>Indicates the total amount of virtual address space reserved by the process. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "virtual_address_space_committed_kb", "<html><p>Indicates the amount of reserved virtual address space that has been committed or mapped to physical pages. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "virtual_address_space_available_kb", "<html><p>Indicates the amount of virtual address space that is currently free. Not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Free regions that are smaller than the allocation granularity can exist. These regions are unavailable for allocations.</p></td>    </tr>   </tbody>  </table> </div></div><p></p></html>");
			mtd.addColumn("dm_os_process_memory", "page_fault_count"                  , "<html><p>Indicates the number of page faults that are incurred by the SQL Server process. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "memory_utilization_percentage"     , "<html><p>Specifies the percentage of committed memory that is in the working set. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "available_commit_limit_kb"         , "<html><p>Indicates the amount of memory that is available to be committed by the process. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "process_physical_memory_low"       , "<html><p>Indicates that the process is responding to low physical memory notification. Not nullable.</p></html>");
			mtd.addColumn("dm_os_process_memory", "process_virtual_memory_low"        , "<html><p>Indicates that low virtual memory condition has been detected. Not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_process_memory' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_schedulers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_schedulers",  "<p>Returns one row per scheduler in SQL Server where each scheduler is mapped to an individual processor. Use this view to monitor the condition of a scheduler or to identify runaway tasks.</p>");

			// Column names and description
			mtd.addColumn("dm_os_schedulers", "scheduler_address"         , "<html><p>Memory address of the scheduler. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "parent_node_id"            , "<html><p>ID of the node that the scheduler belongs to, also known as the parent node. This represents a nonuniform memory access (NUMA) node. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "scheduler_id"              , "<html><p>ID of the scheduler. All schedulers that are used to run regular queries have ID numbers less than 1048576. Those schedulers that have IDs greater than or equal to 1048576 are used internally by SQL Server, such as the dedicated administrator connection scheduler. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "cpu_id"                    , "<html><p>CPU ID assigned to the scheduler.</p><p>Is not nullable.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>255 does not indicate no affinity as it did in SQL Server 2005. See <a href=\"https://msdn.microsoft.com/en-us/library/ms187818.aspx\">sys.dm_os_threads (Transact-SQL)</a> for additional affinity information.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_schedulers", "status"                    , "<html><p>Indicates the status of the scheduler. Can be one of the following values:</p><ul class=\"unordered\"> <li><p>HIDDEN ONLINE</p></li> <li><p>HIDDEN OFFLINE</p></li> <li><p>VISIBLE ONLINE</p></li> <li><p>VISIBLE OFFLINE</p></li> <li><p>VISIBLE ONLINE (DAC)</p></li> <li><p>HOT_ADDED</p></li></ul><p>Is not nullable.</p><p>HIDDEN schedulers are used to process requests that are internal to the Database Engine. VISIBLE schedulers are used to process user requests. </p><p>OFFLINE schedulers map to processors that are offline in the affinity mask and are, therefore, not being used to process any requests. ONLINE schedulers map to processors that are online in the affinity mask and are available to process threads.</p><p>DAC indicates the scheduler is running under a dedicated administrator connection.</p><p>HOT ADDED indicates the schedulers were added in response to a hot add CPU event.</p></html>");
			mtd.addColumn("dm_os_schedulers", "is_online"                 , "<html><p>If SQL Server is configured to use only some of the available processors on the server, this configuration can mean that some schedulers are mapped to processors that are not in the affinity mask. If that is the case, this column returns 0. This value means that the scheduler is not being used to process queries or batches.</p><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "is_idle"                   , "<html><p>1 = Scheduler is idle. No workers are currently running. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "preemptive_switches_count" , "<html><p>Number of times that workers on this scheduler have switched to the preemptive mode. </p><p>To execute code that is outside SQL Server (for example, extended stored procedures and distributed queries), a thread has to execute outside the control of the non-preemptive scheduler. To do this, a worker switches to preemptive mode. </p></html>");
			mtd.addColumn("dm_os_schedulers", "context_switches_count"    , "<html><p>Number of context switches that have occurred on this scheduler. Is not nullable.</p><p>To allow for other workers to run, the current running worker has to relinquish control of the scheduler or switch context.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>If a worker yields the scheduler and puts itself into the runnable queue and then finds no other workers, the worker will select itself. In this case, the <span class=\"literal\">context_switches_count</span> is not updated, but the <span class=\"literal\">yield_count</span> is updated. </p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_schedulers", "idle_switches_count"       , "<html><p>Number of times the scheduler has been waiting for an event while idle. This column is similar to <span class=\"literal\">context_switches_count</span>. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "current_tasks_count"       , "<html><p>Number of current tasks that are associated with this scheduler. This count includes the following:</p><ul class=\"unordered\"> <li><p>Tasks that are waiting for a worker to execute them.</p></li> <li><p>Tasks that are currently waiting or running (in SUSPENDED or RUNNABLE state).</p></li></ul><p>When a task is completed, this count is decremented. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "runnable_tasks_count"      , "<html><p>Number of workers, with tasks assigned to them, that are waiting to be scheduled on the runnable queue. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "current_workers_count"     , "<html><p>Number of workers that are associated with this scheduler. This count includes workers that are not assigned any task. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "active_workers_count"      , "<html><p>Number of workers that are active. An active worker is never preemptive, must have an associated task, and is either running, runnable, or suspended. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "work_queue_count"          , "<html><p>Number of tasks in the pending queue. These tasks are waiting for a worker to pick them up. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "pending_disk_io_count"     , "<html><p>Number of pending I/Os that are waiting to be completed. Each scheduler has a list of pending I/Os that are checked to determine whether they have been completed every time there is a context switch. The count is incremented when the request is inserted. This count is decremented when the request is completed. This number does not indicate the state of the I/Os. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "load_factor"               , "<html><p>Internal value that indicates the perceived load on this scheduler. This value is used to determine whether a new task should be put on this scheduler or another scheduler. This value is useful for debugging purposes when it appears that schedulers are not evenly loaded. The routing decision is made based on the load on the scheduler. SQL Server also uses a load factor of nodes and schedulers to help determine the best location to acquire resources. When a task is enqueued, the load factor is increased. When a task is completed, the load factor is decreased. Using the load factors helps SQL Server OS balance the work load better. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "yield_count"               , "<html><p>Internal value that is used to indicate progress on this scheduler. This value is used by the Scheduler Monitor to determine whether a worker on the scheduler is not yielding to other workers on time. This value does not indicate that the worker or task transitioned to a new worker. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "last_timer_activity"       , "<html><p>In CPU ticks, the last time that the scheduler timer queue was checked by the scheduler. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "failed_to_create_worker"   , "<html><p>Set to 1 if a new worker could not be created on this scheduler. This generally occurs because of memory constraints. Is nullable.</p></html>");
			mtd.addColumn("dm_os_schedulers", "active_worker_address"     , "<html><p>Memory address of the worker that is currently active. Is nullable. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms178626.aspx\">sys.dm_os_workers (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_schedulers", "memory_object_address"     , "<html><p>Memory address of the scheduler memory object. Not NULLABLE.</p></html>");
			mtd.addColumn("dm_os_schedulers", "task_memory_object_address", "<html><p>Memory address of the task memory object. Is not nullable. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179875.aspx\">sys.dm_os_memory_objects (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_schedulers", "quantum_length_us"         , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_schedulers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_stacks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_stacks",  "<p>This dynamic management view is used internally by SQL Server to do the following:</p><ul class=\"unordered\"> <li><p>Keep track of debug data such as outstanding allocations.</p></li> <li><p>Assume or validate logic that is used by SQL Server components in places where the component assumes that a certain call has been made.</p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_os_stacks", "stack_address", "<html><p>Unique address for this stack allocation. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_stacks", "frame_index"  , "<html><p>Each line represents a function call that, when sorted in ascending order by frame index for a particular <strong>stack_address</strong>, returns the full call stack. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_stacks", "frame_address", "<html><p>Address of the function call. Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_stacks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_sys_info
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_sys_info",  "<p>Returns a miscellaneous set of useful information about the computer, and about the resources available to and consumed by SQL Server.</p>");

			// Column names and description
			mtd.addColumn("dm_os_sys_info", "cpu_ticks"                     , "<html><p>Specifies the current CPU tick count. CPU ticks are obtained from the processor's RDTSC counter. It is a monotonically increasing number. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "ms_ticks"                      , "<html><p>Specifies the number of milliseconds since the computer started. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "cpu_count"                     , "<html><p>Specifies the number of logical CPUs on the system. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "hyperthread_ratio"             , "<html><p>Specifies the ratio of the number of logical or physical cores that are exposed by one physical processor package. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "physical_memory_in_bytes"      , "<html><p>Specifies the total amount of physical memory on the machine. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "physical_memory_kb"            , "<html><p>Specifies the total amount of physical memory on the machine. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "virtual_memory_in_bytes"       , "<html><p>Amount of virtual memory available to the process in user mode. This can be used to determine whether SQL Server was started by using a 3-GB switch.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "virtual_memory_kb"             , "<html><p>Specifies the total amount of virtual address space available to the process in user mode. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "bpool_commited"                , "<html><p>Represents the committed memory in kilobytes (KB) in the memory manager. Does not include reserved memory in the memory manager. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "committed_kb"                  , "<html><p>Represents the committed memory in kilobytes (KB) in the memory manager. Does not include reserved memory in the memory manager. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "bpool_commit_target"           , "<html><p>Represents the amount of memory, in kilobytes (KB), that can be consumed by SQL Server memory manager. </p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "committed_target_kb"           , "<html><p>Represents the amount of memory, in kilobytes (KB), that can be consumed by SQL Server memory manager. The target amount is calculated using a variety of inputs like:</p><ul class=\"unordered\"> <li><p>the current state of the system including its load</p></li> <li><p>the memory requested by current processes</p></li> <li><p>the amount of memory installed on the computer</p></li> <li><p>configuration parameters </p></li></ul><p>If <strong>committed_target_kb</strong> is larger than <strong>committed_kb</strong>, the memory manager will try to obtain additional memory. If <strong>committed_target_kb</strong> is smaller than <strong>committed_kb</strong>, the memory manager will try to shrink the amount of memory committed. The <strong>committed_target_kb</strong> always includes stolen and reserved memory. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "bpool_visible"                 , "<html><p>Number of 8-KB buffers in the buffer pool that are directly accessible in the process virtual address space. When not using the Address Windowing Extensions (AWE), when the buffer pool has obtained its memory target (bpool_committed = bpool_commit_target), the value of bpool_visible equals the value of bpool_committed.When using AWE on a 32-bit version of SQL Server, bpool_visible represents the size of the AWE mapping window used to access physical memory allocated by the buffer pool. The size of this mapping window is bound by the process address space and, therefore, the visible amount will be smaller than the committed amount, and can be further reduced by internal components consuming memory for purposes other than database pages. If the value of bpool_visible is too low, you might receive out of memory errors.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2008 R2.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "visible_target_kb"             , "<html><p>Is the same as <strong>committed_target_kb</strong>. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2012 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "stack_size_in_bytes"           , "<html><p>Specifies the size of the call stack for each thread created by SQL Server. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "os_quantum"                    , "<html><p>Represents the Quantum for a non-preemptive task, measured in milliseconds. Quantum (in seconds) = <strong>os_quantum</strong> / CPU clock speed. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "os_error_mode"                 , "<html><p>Specifies the error mode for the SQL Server process. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "os_priority_class"             , "<html><p>Specifies the priority class for the SQL Server process. Nullable.</p><p>32 = Normal (Error log will say SQL Server is starting at normal priority base (=7).)</p><p>128 = High (Error log will say SQL Server is running at high priority base. (=13).)</p><p>For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms188709.aspx\">Configure the priority boost Server Configuration Option</a>.</p></html>");
			mtd.addColumn("dm_os_sys_info", "max_workers_count"             , "<html><p>Represents the maximum number of workers that can be created. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "scheduler_count"               , "<html><p>Represents the number of user schedulers configured in the SQL Server process. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "scheduler_total_count"         , "<html><p>Represents the total number of schedulers in SQL Server. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "deadlock_monitor_serial_number", "<html><p>Specifies the ID of the current deadlock monitor sequence. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "sqlserver_start_time_ms_ticks" , "<html><p>Represents the <strong>ms_tick</strong> number when SQL Server?last started. Compare to the current <span class=\"literal\">ms_ticks</span> column. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "sqlserver_start_time"          , "<html><p>Specifies the date and time SQL Server last started. Not nullable.</p></html>");
			mtd.addColumn("dm_os_sys_info", "affinity_type"                 , "<html><p>Specifies the type of server CPU process affinity currently in use. Not nullable. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ee210585.aspx\">ALTER SERVER CONFIGURATION (Transact-SQL)</a>.</p><p>1 = MANUAL</p><p>2 = AUTO</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "affinity_type_desc"            , "<html><p>Describes the <strong>affinity_type</strong> column. Not nullable.</p><p>MANUAL = affinity has been set for at least one CPU.</p><p>AUTO = SQL Server can freely move threads between CPUs. </p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "process_kernel_time_ms"        , "<html><p>Total time in milliseconds spent by all SQL Server threads in kernel mode. This value can be larger than a single processor clock because it includes the time for all processors on the server. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "process_user_time_ms"          , "<html><p>Total time in milliseconds spent by all SQL Server threads in user mode. This value can be larger than a single processor clock because it includes the time for all processors on the server. Not nullable.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "time_source"                   , "<html><p>Indicates the API that SQL Server is using to retrieve wall clock time. Not nullable.</p><p>0 = QUERY_PERFORMANCE_COUNTER</p><p>1 = MULTIMEDIA_TIMER</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "time_source_desc"              , "<html><p>Describes the <strong>time_source</strong> column. Not nullable.</p><p>QUERY_PERFORMANCE_COUNTER = the <a href=\"http://go.microsoft.com/fwlink/?LinkId=163095\">QueryPerformanceCounter</a> API retrieves wall clock time.</p><p>MULTIMEDIA_TIMER = The <a href=\"http://go.microsoft.com/fwlink/?LinkId=163094\">multimedia timer</a> API that retrieves wall clock time.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "virtual_machine_type"          , "<html><p>Indicates whether SQL Server is running in a virtualized environment. Not nullable.</p><p>0 = NONE</p><p>1 = HYPERVISOR</p><p>2 = OTHER</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_os_sys_info", "virtual_machine_type_desc"     , "<html><p>Describes the <strong>virtual_machine_type</strong> column. Not nullable.</p><p>NONE = SQL Server is not running inside a virtual machine.</p><p>HYPERVISOR = SQL Server is running inside a hypervisor, which implies a hardware-assisted virtualization. When the Hyper_V role is installed, the hypervisor hosts the OS, so an instance running on the host OS is running in the hypervisor.</p><p>OTHER = SQL Server is running inside a virtual machine that does not employ hardware assistant such as Microsoft Virtual PC.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_sys_info' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_sys_memory
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_sys_memory",  "<p>Returns memory information from the operating system.</p><p>SQL Server is bounded by, and responds to, external memory conditions at the operating system level and?the physical limits of the underlying hardware. Determining the overall system state is an important part of evaluating SQL Server memory usage.</p>");

			// Column names and description
			mtd.addColumn("dm_os_sys_memory", "total_physical_memory_kb"       , "<html><p>Total size of physical memory available to the operating system, in kilobytes (KB).</p></html>");
			mtd.addColumn("dm_os_sys_memory", "available_physical_memory_kb"   , "<html><p>Size of physical memory available, in KB.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "total_page_file_kb"             , "<html><p>Size of the commit limit reported by the operating system in KB</p></html>");
			mtd.addColumn("dm_os_sys_memory", "available_page_file_kb"         , "<html><p>Total amount of page file thatis not being used, in KB.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "system_cache_kb"                , "<html><p>Total amount of system cache memory, in KB.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "kernel_paged_pool_kb"           , "<html><p>Total amount of the paged kernel pool, in KB.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "kernel_nonpaged_pool_kb"        , "<html><p>Total amount of the nonpaged kernel pool, in KB.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "system_high_memory_signal_state", "<html><p>State of the system high memory resource notification. A value of 1 indicates the high memory signal has been set by Windows. For more information, see <a href=\"http://go.microsoft.com/fwlink/?LinkId=82427\">CreateMemoryResourceNotification</a> in the MSDN library.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "system_low_memory_signal_state" , "<html><p>State of the system low memory resource notification. A value of 1 indicates the low memory signal has been set by Windows. For more information, see <a href=\"http://go.microsoft.com/fwlink/?LinkId=82427\">CreateMemoryResourceNotification</a> in the MSDN library.</p></html>");
			mtd.addColumn("dm_os_sys_memory", "system_memory_state_desc"       , "<html><p>Description of the memory state.</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Condition</p></th>     <th><p>Value</p></th>    </tr>    <tr>     <td data-th=\"Condition\"><p>system_high_memory_signal_state = 1 </p><p>and </p><p>system_low_memory_signal_state = 0</p></td>     <td data-th=\"Value\"><p>Available physical memory is high</p></td>    </tr>    <tr>     <td data-th=\"Condition\"><p>system_high_memory_signal_state = 0 </p><p>and </p><p>system_low_memory_signal_state = 1 </p></td>     <td data-th=\"Value\"><p>Available physical memory is low</p></td>    </tr>    <tr>     <td data-th=\"Condition\"><p>system_high_memory_signal_state = 0 </p><p>and </p><p>system_low_memory_signal_state = 0</p></td>     <td data-th=\"Value\"><p>Physical memory usage is steady</p></td>    </tr>    <tr>     <td data-th=\"Condition\"><p>system_high_memory_signal_state = 1 </p><p>and </p><p>system_low_memory_signal_state = 1</p></td>     <td data-th=\"Value\"><p>Physical memory state is transitioning</p><p class=\"TextIndented\">The high and low signal should never be on at the same time. However, rapid changes at the operating system level can cause both values to appear to be on to a user mode application. The appearance of both signals being on will be interpreted as a transition state.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_sys_memory' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_tasks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_tasks",  "<p>Returns one row for each task that is active in the instance of SQL Server.</p>");

			// Column names and description
			mtd.addColumn("dm_os_tasks", "task_address"           , "<html><p>Memory address of the object.</p></html>");
			mtd.addColumn("dm_os_tasks", "task_state"             , "<html><p>State of the task. This can be one of the following:</p><p>PENDING: Waiting for a worker thread.</p><p>RUNNABLE: Runnable, but waiting to receive a quantum.</p><p>RUNNING: Currently running on the scheduler.</p><p>SUSPENDED: Has a worker, but is waiting for an event.</p><p>DONE: Completed.</p><p>SPINLOOP: Stuck in a spinlock.</p></html>");
			mtd.addColumn("dm_os_tasks", "context_switches_count" , "<html><p>Number of scheduler context switches that this task has completed.</p></html>");
			mtd.addColumn("dm_os_tasks", "pending_io_count"       , "<html><p>Number of physical I/Os that are performed by this task.</p></html>");
			mtd.addColumn("dm_os_tasks", "pending_io_byte_count"  , "<html><p>Total byte count of I/Os that are performed by this task.</p></html>");
			mtd.addColumn("dm_os_tasks", "pending_io_byte_average", "<html><p>Average byte count of I/Os that are performed by this task.</p></html>");
			mtd.addColumn("dm_os_tasks", "scheduler_id"           , "<html><p>ID of the parent scheduler. This is a handle to the scheduler information for this task. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177526.aspx\">sys.dm_os_schedulers (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_tasks", "session_id"             , "<html><p>ID of the session that is associated with the task.</p></html>");
			mtd.addColumn("dm_os_tasks", "exec_context_id"        , "<html><p>Execution context ID that is associated with the task.</p></html>");
			mtd.addColumn("dm_os_tasks", "request_id"             , "<html><p>ID of the request of the task. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177648.aspx\">sys.dm_exec_requests (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_tasks", "worker_address"         , "<html><p>Memory address of the worker that is running the task.</p><p>NULL = Task is either waiting for a worker to be able to run, or the task has just finished running.</p><p>For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms178626.aspx\">sys.dm_os_workers (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_tasks", "host_address"           , "<html><p>Memory address of the host.</p><p>0 = Hosting was not used to create the task. This helps identify the host that was used to create this task.</p><p>For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms187800.aspx\">sys.dm_os_hosts (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_tasks", "parent_task_address"    , "<html><p>Memory address of the task that is the parent of the object.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_tasks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_threads
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_threads",  "<p>Returns a list of all SQL Server Operating System threads that are running under the SQL Server process.</p>");

			// Column names and description
			mtd.addColumn("dm_os_threads", "thread_address"           , "<html><p>Memory address (Primary Key) of the thread.</p></html>");
			mtd.addColumn("dm_os_threads", "started_by_sqlservr"      , "<html><p>Indicates the thread initiator.</p><p>1 = SQL Server started the thread.</p><p>0 = Another component started the thread, such as an extended stored procedure from within SQL Server.</p></html>");
			mtd.addColumn("dm_os_threads", "os_thread_id"             , "<html><p>ID of the thread that is assigned by the operating system.</p></html>");
			mtd.addColumn("dm_os_threads", "status"                   , "<html><p>Internal status flag.</p></html>");
			mtd.addColumn("dm_os_threads", "instruction_address"      , "<html><p>Address of the instruction that is currently being executed.</p></html>");
			mtd.addColumn("dm_os_threads", "creation_time"            , "<html><p>Time when this thread was created.</p></html>");
			mtd.addColumn("dm_os_threads", "kernel_time"              , "<html><p>Amount of kernel time that is used by this thread.</p></html>");
			mtd.addColumn("dm_os_threads", "usermode_time"            , "<html><p>Amount of user time that is used by this thread.</p></html>");
			mtd.addColumn("dm_os_threads", "stack_base_address"       , "<html><p>Memory address of the highest stack address for this thread.</p></html>");
			mtd.addColumn("dm_os_threads", "stack_end_address"        , "<html><p>Memory address of the lowest stack address of this thread.</p></html>");
			mtd.addColumn("dm_os_threads", "stack_bytes_committed"    , "<html><p>Number of bytes that are committed in the stack.</p></html>");
			mtd.addColumn("dm_os_threads", "stack_bytes_used"         , "<html><p>Number of bytes that are actively being used on the thread.</p></html>");
			mtd.addColumn("dm_os_threads", "affinity"                 , "<html><p>CPU mask on which this thread is running. This depends on the value configured by the <strong>ALTER SERVER CONFIGURATION SET PROCESS AFFINITY</strong> statement. Might be different from the scheduler in case of soft-affinity.</p></html>");
			mtd.addColumn("dm_os_threads", "Priority"                 , "<html><p>Priority value of this thread.</p></html>");
			mtd.addColumn("dm_os_threads", "Locale"                   , "<html><p>Cached locale LCID for the thread.</p></html>");
			mtd.addColumn("dm_os_threads", "Token"                    , "<html><p>Cached impersonation token handle for the thread.</p></html>");
			mtd.addColumn("dm_os_threads", "is_impersonating"         , "<html><p>Indicates whether this thread is using Win32 impersonation.</p><p>1 = The thread is using security credentials that are different from the default of the process. This indicates that the thread is impersonating an entity other than the one that created the process.</p></html>");
			mtd.addColumn("dm_os_threads", "is_waiting_on_loader_lock", "<html><p>Operating system status of whether the thread is waiting on the loader lock. </p></html>");
			mtd.addColumn("dm_os_threads", "fiber_data"               , "<html><p>Current Win32 fiber that is running on the thread. This is only applicable when SQL Server is configured for lightweight pooling.</p></html>");
			mtd.addColumn("dm_os_threads", "thread_handle"            , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_os_threads", "event_handle"             , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_os_threads", "scheduler_address"        , "<html><p>Memory address of the scheduler that is associated with this thread. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177526.aspx\">sys.dm_os_schedulers (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_threads", "worker_address"           , "<html><p>Memory address of the worker that is bound to this thread. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms178626.aspx\">sys.dm_os_workers (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_threads", "fiber_context_address"    , "<html><p>Internal fiber context address. This is only applicable when SQL Server is configured for lightweight pooling.</p></html>");
			mtd.addColumn("dm_os_threads", "self_address"             , "<html><p>Internal consistency pointer.</p></html>");
			mtd.addColumn("dm_os_threads", "processor_group"          , "<html><p>Processor group ID.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 R2 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_threads' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_virtual_address_dump
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_virtual_address_dump",  "<p>Returns information about a range of pages in the virtual address space of the calling process.</p>");

			// Column names and description
			mtd.addColumn("dm_os_virtual_address_dump", "region_base_address"           , "<html><p>Pointer to the base address of the region of pages. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_virtual_address_dump", "region_allocation_base_address", "<html><p>Pointer to the base address of a range of pages allocated by the VirtualAlloc Windows API function. The page pointed to by the BaseAddress member is contained within this allocation range. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_virtual_address_dump", "region_allocation_protection"  , "<html><p>Protection attributes when the region was first allocated. The value is one of the following:</p><ul class=\"unordered\"> <li><p>PAGE_READONLY</p></li> <li><p>PAGE_READWRITE</p></li> <li><p>PAGE_NOACCESS</p></li> <li><p>PAGE_WRITECOPY</p></li> <li><p>PAGE_EXECUTE</p></li> <li><p>PAGE_EXECUTE_READ</p></li> <li><p>PAGE_EXECUTE_READWRITE</p></li> <li><p>PAGE_EXECUTE_WRITECOPY</p></li> <li><p>PAGE_GUARD</p></li> <li><p>PAGE_NOCACHE</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_virtual_address_dump", "region_size_in_bytes"          , "<html><p>Size of the region, in bytes, starting at the base address in which all the pages have the same attributes. Is not nullable.</p></html>");
			mtd.addColumn("dm_os_virtual_address_dump", "region_state"                  , "<html><p>Current state of the region. This is one of the following: </p><ul class=\"unordered\"> <li><p>MEM_COMMIT</p></li> <li><p>MEM_RESERVE</p></li> <li><p>MEM_FREE</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_virtual_address_dump", "region_current_protection"     , "<html><p>Protection attributes. The value is one of the following:</p><ul class=\"unordered\"> <li><p>PAGE_READONLY</p></li> <li><p>PAGE_READWRITE </p></li> <li><p>PAGE_NOACCESS </p></li> <li><p>PAGE_WRITECOPY</p></li> <li><p>PAGE_EXECUTE</p></li> <li><p>PAGE_EXECUTE_READ </p></li> <li><p>PAGE_EXECUTE_READWRITE </p></li> <li><p>PAGE_EXECUTE_WRITECOPY </p></li> <li><p>PAGE_GUARD</p></li> <li><p>PAGE_NOCACHE</p></li></ul><p>Is not nullable.</p></html>");
			mtd.addColumn("dm_os_virtual_address_dump", "region_type"                   , "<html><p>Identifies the types of pages in the region. The value can be one of the following:</p><ul class=\"unordered\"> <li><p>MEM_PRIVATE</p></li> <li><p>MEM_MAPPED</p></li> <li><p>MEM_IMAGE</p></li></ul><p>Is not nullable.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_virtual_address_dump' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_volume_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_volume_stats",  "<p>Returns information about the operating system volume (directory) on which the specified databases and files are stored in SQL Server. Use this dynamic management function to check the attributes of the physical disk drive or return available free space information about the directory. </p>");

			// Column names and description
			mtd.addColumn("dm_os_volume_stats", "Column"                    , "<html><p><strong>Description</strong></p></html>");
			mtd.addColumn("dm_os_volume_stats", "database_id"               , "<html><p>ID of the database. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "file_id"                   , "<html><p>ID of the file. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "volume_mount_point"        , "<html><p>Mount point at which the volume is rooted. Can return an empty string.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "volume_id"                 , "<html><p>Operating system volume ID. Can return an empty string</p></html>");
			mtd.addColumn("dm_os_volume_stats", "logical_volume_name"       , "<html><p>Logical volume name. Can return an empty string</p></html>");
			mtd.addColumn("dm_os_volume_stats", "file_system_type"          , "<html><p>Type of file system volume (for example, NTFS, FAT, RAW). Can return an empty string</p></html>");
			mtd.addColumn("dm_os_volume_stats", "total_bytes"               , "<html><p>Total size in bytes of the volume. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "available_bytes"           , "<html><p>Available free space on the volume. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "supports_compression"      , "<html><p>Indicates if the volume supports operating system compression. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "supports_alternate_streams", "<html><p>Indicates if the volume supports alternate streams. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "supports_sparse_files"     , "<html><p>Indicates if the volume supports sparse files. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "is_read_only"              , "<html><p>Indicates if the volume is currently marked as read only. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_volume_stats", "is_compressed"             , "<html><p>Indicates if this volume is currently compressed. Cannot be null.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_volume_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_wait_stats
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_wait_stats",  "<p>Returns information about all the waits encountered by threads that executed. You can use this aggregated view to diagnose performance issues with SQL Server and also with specific queries and batches. <a href=\"https://msdn.microsoft.com/en-us/library/mt282433.aspx\">sys.dm_exec_session_wait_stats (Transact-SQL)</a> provides similar information by session.</p>");

			// Column names and description
			mtd.addColumn("dm_os_wait_stats", "wait_type"          , "<html><p>Name of the wait type. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179984.aspx#WaitTypes\">Types of Waits</a>, later in this topic. </p></html>");
			mtd.addColumn("dm_os_wait_stats", "waiting_tasks_count", "<html><p>Number of waits on this wait type. This counter is incremented at the start of each wait. </p></html>");
			mtd.addColumn("dm_os_wait_stats", "wait_time_ms"       , "<html><p>Total wait time for this wait type in milliseconds. This time is inclusive of <span class=\"literal\">signal_wait_time_ms</span>. </p></html>");
			mtd.addColumn("dm_os_wait_stats", "max_wait_time_ms"   , "<html><p>Maximum wait time on this wait type.</p></html>");
			mtd.addColumn("dm_os_wait_stats", "signal_wait_time_ms", "<html><p>Difference between the time that the waiting thread was signaled and when it started running. </p></html>");
			mtd.addColumn("dm_os_wait_stats", "wait_xtp_recovery"  , "<html><p>Occurs when database recovery is waiting for recovery of memory-optimized objects to finish.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_wait_stats' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_waiting_tasks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_waiting_tasks",  "<p>Returns information about the wait queue of tasks that are waiting on some resource. </p>");

			// Column names and description
			mtd.addColumn("dm_os_waiting_tasks", "waiting_task_address"    , "<html><p>Address of the waiting task.</p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "session_id"              , "<html><p>ID of the session associated with the task. </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "exec_context_id"         , "<html><p>ID of the execution context associated with the task. </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "wait_duration_ms"        , "<html><p>Total wait time for this wait type, in milliseconds. This time is inclusive of <strong>signal_wait_time</strong>. </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "wait_type"               , "<html><p>Name of the wait type. </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "resource_address"        , "<html><p>Address of the resource for which the task is waiting. </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "blocking_task_address"   , "<html><p>Task that is currently holding this resource </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "blocking_session_id"     , "<html><p>ID of the session that is blocking the request. If this column is NULL, the request is not blocked, or the session information of the blocking session is not available (or cannot be identified).</p><p>-2 = The blocking resource is owned by an orphaned distributed transaction.</p><p>-3 = The blocking resource is owned by a deferred recovery transaction.</p><p>-4 = Session ID of the blocking latch owner could not be determined due to internal latch state transitions.</p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "blocking_exec_context_id", "<html><p>ID of the execution context of the blocking task. </p></html>");
			mtd.addColumn("dm_os_waiting_tasks", "resource_description"    , "<html><p>Description of the resource that is being consumed. For more information, see the list below.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_waiting_tasks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_windows_info
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_windows_info",  "<p>Returns one row that displays Windows operating system version information.</p>");

			// Column names and description
			mtd.addColumn("dm_os_windows_info", "windows_release"           , "<html><p>Microsoft Windows operating system release (version number). Cannot be null. For a list of values and descriptions, see <a href=\"https://msdn.microsoft.com/library/ms724832%28VS.85%29.aspx\">Operating System Version (Windows)</a>. Cannot be null.</p></html>");
			mtd.addColumn("dm_os_windows_info", "windows_service_pack_level", "<html><p>Service pack level of the Windows operating system. Cannot be null. </p></html>");
			mtd.addColumn("dm_os_windows_info", "windows_sku"               , "<html><p>Windows Stock Keeping Unit (SKU) ID. Cannot be null. For a list of SKU IDs and descriptions, see <a href=\"https://msdn.microsoft.com/library/ms724358.aspx\">GetProductInfo Function</a>. Is nullable.</p></html>");
			mtd.addColumn("dm_os_windows_info", "os_language_version"       , "<html><p>Windows locale identifier (LCID) of the operating system. For a list of LCID values and descriptions, see <a href=\"http://go.microsoft.com/fwlink/?LinkId=208080\">Locale IDs Assigned by Microsoft</a>. Cannot be null.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_windows_info' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_os_workers
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_os_workers",  "<p>Returns a row for every worker in the system. </p>");

			// Column names and description
			mtd.addColumn("dm_os_workers", "worker_address"                     , "<html><p>Memory address of the worker. </p></html>");
			mtd.addColumn("dm_os_workers", "status"                             , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_os_workers", "is_preemptive"                      , "<html><p>1 = Worker is running with preemptive scheduling. Any worker that is running external code is run under preemptive scheduling. </p></html>");
			mtd.addColumn("dm_os_workers", "is_fiber"                           , "<html><p>1 = Worker is running with lightweight pooling. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms188787.aspx\">sp_configure (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_workers", "is_sick"                            , "<html><p>1 = Worker is stuck trying to obtain a spin lock. If this bit is set, this might indicate a problem with contention on a frequently accessed object. </p></html>");
			mtd.addColumn("dm_os_workers", "is_in_cc_exception"                 , "<html><p>1 = Worker is currently handling a non-SQL Server exception.</p></html>");
			mtd.addColumn("dm_os_workers", "is_fatal_exception"                 , "<html><p>Specifies whether this worker received a fatal exception. </p></html>");
			mtd.addColumn("dm_os_workers", "is_inside_catch"                    , "<html><p>1 = Worker is currently handling an exception. </p></html>");
			mtd.addColumn("dm_os_workers", "is_in_polling_io_completion_routine", "<html><p>1 = Worker is currently running an I/O completion routine for a pending I/O. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms188762.aspx\">sys.dm_io_pending_io_requests (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_os_workers", "context_switch_count"               , "<html><p>Number of scheduler context switches that are performed by this worker. </p></html>");
			mtd.addColumn("dm_os_workers", "pending_io_count"                   , "<html><p>Number of physical I/Os that are performed by this worker. </p></html>");
			mtd.addColumn("dm_os_workers", "pending_io_byte_count"              , "<html><p>Total number of bytes for all pending physical I/Os for this worker. </p></html>");
			mtd.addColumn("dm_os_workers", "pending_io_byte_average"            , "<html><p>Average number of bytes for physical I/Os for this worker. </p></html>");
			mtd.addColumn("dm_os_workers", "wait_started_ms_ticks"              , "<html><p>Point in time, in <a href=\"https://msdn.microsoft.com/en-us/library/ms175048.aspx\">ms_ticks</a>, when this worker entered the SUSPENDED state. Subtracting this value from <span class=\"literal\">ms_ticks</span> in <a href=\"https://msdn.microsoft.com/en-us/library/ms175048.aspx\">sys.dm_os_sys_info</a> returns the number of milliseconds that the worker has been waiting.</p></html>");
			mtd.addColumn("dm_os_workers", "wait_resumed_ms_ticks"              , "<html><p>Point in time, in <a href=\"https://msdn.microsoft.com/en-us/library/ms175048.aspx\">ms_ticks</a>, when this worker entered the RUNNABLE state. Subtracting this value from <span class=\"literal\">ms_ticks</span> in <a href=\"https://msdn.microsoft.com/en-us/library/ms175048.aspx\">sys.dm_os_sys_info</a> returns the number of milliseconds that the worker has been in the runnable queue.</p></html>");
			mtd.addColumn("dm_os_workers", "task_bound_ms_ticks"                , "<html><p>Point in time, in <a href=\"https://msdn.microsoft.com/en-us/library/ms175048.aspx\">ms_ticks</a>, when a task is bound to this worker.</p></html>");
			mtd.addColumn("dm_os_workers", "worker_created_ms_ticks"            , "<html><p>Point in time, in <a href=\"https://msdn.microsoft.com/en-us/library/ms175048.aspx\">ms_ticks</a>, when a worker is created.</p></html>");
			mtd.addColumn("dm_os_workers", "exception_num"                      , "<html><p>Error number of the last exception that this worker encountered.</p></html>");
			mtd.addColumn("dm_os_workers", "exception_severity"                 , "<html><p>Severity of the last exception that this worker encountered. </p></html>");
			mtd.addColumn("dm_os_workers", "exception_address"                  , "<html><p>Code address that threw the exception </p></html>");
			mtd.addColumn("dm_os_workers", "affinity"                           , "<html><p>The thread affinity of the worker. Matches the affinity of the thread in <a href=\"https://msdn.microsoft.com/en-us/library/ms187818.aspx\">sys.dm_os_threads (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_workers", "state"                              , "<html><p>Worker state. Can be one of the following values: </p><p>INIT = Worker is currently being initialized.</p><p>RUNNING = Worker is currently running either nonpreemptively or preemptively.</p><p>RUNNABLE = The worker is ready to run on the scheduler. </p><p>SUSPENDED = The worker is currently suspended, waiting for an event to send it a signal.</p></html>");
			mtd.addColumn("dm_os_workers", "start_quantum"                      , "<html><p>Time, in milliseconds, at the start of the current run of this worker.</p></html>");
			mtd.addColumn("dm_os_workers", "end_quantum"                        , "<html><p>Time, in milliseconds, at the end of the current run of this worker.</p></html>");
			mtd.addColumn("dm_os_workers", "last_wait_type"                     , "<html><p>Type of last wait. For a list of wait types, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179984.aspx\">sys.dm_os_wait_stats (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_os_workers", "return_code"                        , "<html><p>Return value from last wait. Can be one of the following values:</p><p>0 =SUCCESS</p><p>3 = DEADLOCK</p><p>4 = PREMATURE_WAKEUP</p><p>258 = TIMEOUT</p></html>");
			mtd.addColumn("dm_os_workers", "quantum_used"                       , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_os_workers", "max_quantum"                        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_os_workers", "boost_count"                        , "<html><p>Internal use only.</p></html>");
			mtd.addColumn("dm_os_workers", "tasks_processed_count"              , "<html><p>Number of tasks that this worker processed. </p></html>");
			mtd.addColumn("dm_os_workers", "fiber_address"                      , "<html><p>Memory address of the fiber with which this worker is associated. </p><p>NULL = SQL Server is not configured for lightweight pooling.</p></html>");
			mtd.addColumn("dm_os_workers", "task_address"                       , "<html><p>Memory address of the current task. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms174963.aspx\">sys.dm_os_tasks (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_os_workers", "memory_object_address"              , "<html><p>Memory address of the worker memory object. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms179875.aspx\">sys.dm_os_memory_objects (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_os_workers", "thread_address"                     , "<html><p>Memory address of the thread associated with this worker. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms187818.aspx\">sys.dm_os_threads (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_os_workers", "signal_worker_address"              , "<html><p>Memory address of the worker that last signaled this object. For more information, see . </p></html>");
			mtd.addColumn("dm_os_workers", "scheduler_address"                  , "<html><p>Memory address of the scheduler. For more information, see <a href=\"https://msdn.microsoft.com/en-us/library/ms177526.aspx\">sys.dm_os_schedulers (Transact-SQL)</a>. </p></html>");
			mtd.addColumn("dm_os_workers", "processor_group"                    , "<html><p>Stores the processor group ID that is assigned to this thread.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_os_workers' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_rda_migration_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_rda_migration_status",  "<p>Contains one row for each batch of migrated data from each Stretch-enabled table on the local instance of SQL Server. Batches are identified by their start time and end time.</p><p><strong>sys.dm_db_rda_migration_status</strong> is scoped to the current database context. Make sure you're in the database context of the Stretch-enable tables for which you want to see migration status.</p>");

			// Column names and description
			mtd.addColumn("dm_db_rda_migration_status", "table_id"      , "<html><p>The ID of the table from which rows were migrated.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "database_id"   , "<html><p>The ID of the database from which rows were migrated.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "migrated_rows" , "<html><p>The number of rows migrated in this batch.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "start_time_utc", "<html><p>The UTC time at which the batch started.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "end_time_utc"  , "<html><p>The UTC time at which the batch finished.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "error_number"  , "<html><p>If the batch fails, the error number of the error that occurred; otherwise, null.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "error_severity", "<html><p>If the batch fails, the severity of the error that occurred; otherwise, null.</p></html>");
			mtd.addColumn("dm_db_rda_migration_status", "error_state"   , "<html><p>If the batch fails, the state of the error that occurred; otherwise, null. </p><p>The <strong>error_state</strong> indicates the condition or location where the error occurred.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_rda_migration_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_db_rda_schema_update_status
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_db_rda_schema_update_status",  "<p>Contains one row for each schema update task for the remote data archive of each Stretch-enabled table in the current database. Tasks are identified by their task ids.</p><p><strong>dm_db_rda_schema_update_status</strong> is scoped to the current database context. Make sure you are in the database context of the Stretch-enabled table for which you want to see schema update status.</p>");

			// Column names and description
			mtd.addColumn("dm_db_rda_schema_update_status", "table_id"      , "<html><p>The ID of the local Stretch-enabled table whose remote data archive schema is being updated.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "database_id"   , "<html><p>The ID of the database that contains the local Stretch-enabled table.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "task_id"       , "<html><p>The ID of the remote data archive schema update task.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "task_type"     , "<html><p>The type of the remote data archive schema update task.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "task_type_desc", "<html><p>The description of the type of the remote data archive schema update task.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "task_state"    , "<html><p>The state of the remote data archive schema update task.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "task_state_des", "<html><p>The description of the state of the remote data archive schema update task.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "start_time_utc", "<html><p>The UTC time at which the remote data archive schema update started.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "end_time_utc"  , "<html><p>The UTC time at which the remote data archive schema update finished.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "error_number"  , "<html><p>If the remote data archive schema update fails, the error number of the error that occurred; otherwise, null.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "error_severity", "<html><p>If the remote data archive schema update fails, the severity of the error that occurred; otherwise, null.</p></html>");
			mtd.addColumn("dm_db_rda_schema_update_status", "error_state"   , "<html><p>If the remote data archive schema update fails, the state of the error that occurred; otherwise, null. The error_state indicates the condition or location where the error occurred. </p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_db_rda_schema_update_status' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_active_snapshot_database_transactions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_active_snapshot_database_transactions",  "<p>In a SQL Server instance, this dynamic management view returns a virtual table for all active transactions that generate or potentially access row versions. Transactions are included for one or more of the following conditions:</p><ul class=\"unordered\"> <li><p>When either or both ALLOW_SNAPSHOT_ISOLATION and READ_COMMITTED_SNAPSHOT database options are set to ON:</p>  <ul class=\"unordered\">   <li><p>There is one row for each transaction that is running under snapshot isolation level, or read-committed isolation level that is using row versioning.</p></li>   <li><p>There is one row for each transaction that causes a row version to be created in the current database. For example, the transaction generates a row version by updating or deleting a row in the current database.</p></li>  </ul></li> <li><p>When a trigger is fired, there is one row for the transaction under which the trigger is executing.</p></li> <li><p>When an online indexing procedure is running, there is one row for the transaction that is creating the index.</p></li> <li><p>When Multiple Active Results Sets (MARS) session is enabled, there is one row for each transaction that is accessing row versions.</p></li></ul><p>This dynamic management view does not include system transactions.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "transaction_id"                 , "<html><p>Unique identification number assigned for the transaction. The transaction ID is primarily used to identify the transaction in locking operations.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "transaction_sequence_num"       , "<html><p>Transaction sequence number. This is a unique sequence number that is assigned to a transaction when it starts. Transactions that do not generate version records and do not use snapshot scans will not receive a transaction sequence number. </p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "commit_sequence_num"            , "<html><p>Sequence number that indicates when the transaction finishes (commits or stops). For active transactions, the value is NULL.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "is_snapshot"                    , "<html><p>0 = Is not a snapshot isolation transaction. </p><p>1 = Is a snapshot isolation transaction.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "session_id"                     , "<html><p>ID of the session that started the transaction.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "first_snapshot_sequence_num"    , "<html><p>Lowest transaction sequence number of the transactions that were active when a snapshot was taken. On execution, a snapshot transaction takes a snapshot of all of the active transactions at that time. For nonsnapshot transactions, this column shows 0.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "max_version_chain_traversed"    , "<html><p>Maximum length of the version chain that is traversed to find the transactionally consistent version.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "average_version_chain_traversed", "<html><p>Average number of row versions in the version chains that are traversed.</p></html>");
			mtd.addColumn("dm_tran_active_snapshot_database_transactions", "elapsed_time_seconds"           , "<html><p>Elapsed time since the transaction obtained its transaction sequence number.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_active_snapshot_database_transactions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_current_snapshot
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_current_snapshot",  "<p>Returns a virtual table that displays all active transactions at the time when the current snapshot transaction starts. If the current transaction is not a snapshot transaction, this function returns no rows. <strong>sys.dm_tran_current_snapshot</strong> is similar to <strong>sys.dm_tran_transactions_snapshot</strong>, except that <strong>sys.dm_tran_current_snapshot</strong> returns only the active transactions for the current snapshot transaction.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_current_snapshot", "transaction_sequence_num", "<html><p>Transaction sequence number of the active transaction.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_current_snapshot' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_database_transactions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_database_transactions",  "<p>Returns information about transactions at the database level.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_database_transactions", "transaction_id"                                , "<html><p>ID of the transaction at the instance level, not the database level. It is only unique across all databases within an instance, but not unique across all server instances. </p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_id"                                   , "<html><p>ID of the database associated with the transaction.</p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_begin_time"               , "<html><p>Time at which the database became involved in the transaction. Specifically, it is the time of the first log record in the database for the transaction.</p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_type"                     , "<html><p>1 = Read/write transaction</p><p>2 = Read-only transaction</p><p>3 = System transaction</p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_state"                    , "<html><p>1 = The transaction has not been initialized.</p><p>3 = The transaction has been initialized but has not generated any log records.</p><p>4 = The transaction has generated log records.</p><p>5 = The transaction has been prepared.</p><p>10 = The transaction has been committed.</p><p>11 = The transaction has been rolled back.</p><p>12 = The transaction is being committed. In this state the log record is being generated, but it has not been materialized or persisted.</p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_status"                   , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_status2"                  , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_log_record_count"         , "<html><p>Number of log records generated in the database for the transaction.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_replicate_record_count"   , "<html><p>Number of log records generated in the database for the transaction that will be replicated.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_log_bytes_used"           , "<html><p>Number of bytes used so far in the database log for the transaction.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_log_bytes_reserved"       , "<html><p>Number of bytes reserved for use in the database log for the transaction.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_log_bytes_used_system"    , "<html><p>Number of bytes used so far in the database log for system transactions on behalf of the transaction.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_log_bytes_reserved_system", "<html><p>Number of bytes reserved for use in the database log for system transactions on behalf of the transaction.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_begin_lsn"                , "<html><p>Log sequence number (LSN) of the begin record for the transaction in the database log.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_last_lsn"                 , "<html><p>LSN of the most recently logged record for the transaction in the database log.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_most_recent_savepoint_lsn", "<html><p>LSN of the most recent savepoint for the transaction in the database log.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_commit_lsn"               , "<html><p>LSN of the commit log record for the transaction in the database log.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_last_rollback_lsn"        , "<html><p>LSN that was most recently rolled back to. If no rollback has taken place, the value will be MaxLSN (-1:-1:-1).</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_database_transactions", "database_transaction_next_undo_lsn"            , "<html><p>LSN of the next record to undo.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: SQL Server 2008 through SQL Server 2016.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_database_transactions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_session_transactions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_session_transactions",  "<p>Returns correlation information for associated transactions and sessions.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_session_transactions", "session_id"            , "<html><p>ID of the session under which the transaction is running.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "transaction_id"        , "<html><p>ID of the transaction.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "transaction_descriptor", "<html><p>Transaction identifier used by SQL Server when communicating with the client driver.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "enlist_count"          , "<html><p>Number of active requests in the session working on the transaction.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "is_user_transaction"   , "<html><p>1 = The transaction was initiated by a user request.</p><p>0 = System transaction.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "is_local"              , "<html><p>1 = Local transaction.</p><p>0 = Distributed transaction or an enlisted bound session transaction.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "is_enlisted"           , "<html><p>1 = Enlisted distributed transaction.</p><p>0 = Not an enlisted distributed transaction.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "is_bound"              , "<html><p>1 = The transaction is active on the session via bound sessions. </p><p>0 = The transaction is not active on the session via bound sessions.</p></html>");
			mtd.addColumn("dm_tran_session_transactions", "open_transaction_count", "<html><p>The number of open transactions for each session.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_session_transactions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_transactions_snapshot
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_transactions_snapshot",  "<p>Returns a virtual table for the <strong>sequence_number</strong> of transactions that are active when each snapshot transaction starts. The information that is returned by this view can you help you do the following: </p><ul class=\"unordered\"> <li><p>Find the number of currently active snapshot transactions.</p></li> <li><p>Identify data modifications that are ignored by a particular snapshot transaction. For a transaction that is active when a snapshot transaction starts, all data modifications by that transaction, even after that transaction commits, are ignored by the snapshot transaction. </p></li></ul>");

			// Column names and description
			mtd.addColumn("dm_tran_transactions_snapshot", "transaction_sequence_num", "<html><p>Transaction sequence number (XSN) of a snapshot transaction.</p></html>");
			mtd.addColumn("dm_tran_transactions_snapshot", "snapshot_id"             , "<html><p>Snapshot ID for each Transact-SQL statement started under read-committed using row versioning. This value is used to generate a transactionally consistent view of the database supporting each query that is being run under read-committed using row versioning.</p></html>");
			mtd.addColumn("dm_tran_transactions_snapshot", "snapshot_sequence_num"   , "<html><p>Transaction sequence number of a transaction that was active when the snapshot transaction started.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_transactions_snapshot' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_active_transactions
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_active_transactions",  "<p>Returns information about transactions for the instance of SQL Server.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_active_transactions", "transaction_id"           , "<html><p>ID of the transaction at the instance level, not the database level. It is only unique across all databases within an instance but not unique across all server instances.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "name"                     , "<html><p>Transaction name. This is overwritten if the transaction is marked and the marked name replaces the transaction name.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "transaction_begin_time"   , "<html><p>Time that the transaction started.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "transaction_type"         , "<html><p>Type of transaction.</p><p>1 = Read/write transaction </p><p>2 = Read-only transaction</p><p>3 = System transaction</p><p>4 = Distributed transaction</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "transaction_uow"          , "<html><p>Transaction unit of work (UOW) identifier for distributed transactions. MS DTC uses the UOW identifier to work with the distributed transaction.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "transaction_state"        , "<html><p>0 = The transaction has not been completely initialized yet. </p><p>1 = The transaction has been initialized but has not started. </p><p>2 = The transaction is active. </p><p>3 = The transaction has ended. This is used for read-only transactions. </p><p>4 = The commit process has been initiated on the distributed transaction. This is for distributed transactions only. The distributed transaction is still active but further processing cannot take place.</p><p>5 = The transaction is in a prepared state and waiting resolution. </p><p>6 = The transaction has been committed. </p><p>7 = The transaction is being rolled back. </p><p>8 = The transaction has been rolled back.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "transaction_status"       , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "transaction_status2"      , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "dtc_state"                , "<html><p>1 = ACTIVE</p><p>2 = PREPARED</p><p>3 = COMMITTED</p><p>4 = ABORTED</p><p>5 = RECOVERED</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: Azure SQL Database (Initial release through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299659\">current release</a>).</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_active_transactions", "dtc_status"               , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "dtc_isolation_level"      , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_active_transactions", "filestream_transaction_id", "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p><div> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <td><p><strong>Applies to</strong>: Azure SQL Database (Initial release through <a href=\"http://go.microsoft.com/fwlink/p/?LinkId=299659\">current release</a>).</p></td>    </tr>   </tbody>  </table> </div></div></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_active_transactions' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_current_transaction
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_current_transaction",  "<p>Returns a single row that displays the state information of the transaction in the current session.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_current_transaction", "transaction_id"               , "<html><p>Transaction ID of the current snapshot.</p></html>");
			mtd.addColumn("dm_tran_current_transaction", "transaction_sequence_num"     , "<html><p>Sequence number of the transaction that generates the record version.</p></html>");
			mtd.addColumn("dm_tran_current_transaction", "transaction_is_snapshot"      , "<html><p>Snapshot isolation state. This value is 1 if the transaction is started under snapshot isolation. Otherwise, the value is 0.</p></html>");
			mtd.addColumn("dm_tran_current_transaction", "first_snapshot_sequence_num"  , "<html><p>Lowest transaction sequence number of the transactions that were active when a snapshot was taken. On execution, a snapshot transaction takes a snapshot of all of the active transactions at that time. For nonsnapshot transactions, this column shows 0.</p></html>");
			mtd.addColumn("dm_tran_current_transaction", "last_transaction_sequence_num", "<html><p>Global sequence number. This value represents the last transaction sequence number that was generated by the system.</p></html>");
			mtd.addColumn("dm_tran_current_transaction", "first_useful_sequence_num"    , "<html><p>Global sequence number. This value represents the oldest transaction sequence number of the transaction that has row versions that must be retained in the version store. Row versions that were created by prior transactions can be removed.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_current_transaction' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_locks
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_locks",  "<p>Returns information about currently active lock manager resources in SQL Server 2016. Each row represents a currently active request to the lock manager for a lock that has been granted or is waiting to be granted.</p><p>The columns in the result set are divided into two main groups: resource and request. The resource group describes the resource on which the lock request is being made, and the request group describes the lock request.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_locks", "resource_type"                , "<html><p>Represents the resource type. The value can be one of the following: DATABASE, FILE, OBJECT, PAGE, KEY, EXTENT, RID, APPLICATION, METADATA, HOBT, or ALLOCATION_UNIT.</p></html>");
			mtd.addColumn("dm_tran_locks", "resource_subtype"             , "<html><p>Represents a subtype of <strong>resource_type</strong>. Acquiring a subtype lock without holding a nonsubtyped lock of the parent type is technically valid. Different subtypes do not conflict with each other or with the nonsubtyped parent type. Not all resource types have subtypes.</p></html>");
			mtd.addColumn("dm_tran_locks", "resource_database_id"         , "<html><p>ID of the database under which this resource is scoped. All resources handled by the lock manager are scoped by the database ID.</p></html>");
			mtd.addColumn("dm_tran_locks", "resource_description"         , "<html><p>Description of the resource that contains only information that is not available from other resource columns.</p></html>");
			mtd.addColumn("dm_tran_locks", "resource_associated_entity_id", "<html><p>ID of the entity in a database with which a resource is associated. This can be an object ID, Hobt ID, or an Allocation Unit ID, depending on the resource type.</p></html>");
			mtd.addColumn("dm_tran_locks", "resource_lock_partition"      , "<html><p>ID of the lock partition for a partitioned lock resource. The value for nonpartitioned lock resources is 0.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_mode"                 , "<html><p>Mode of the request. For granted requests, this is the granted mode; for waiting requests, this is the mode being requested.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_type"                 , "<html><p>Request type. The value is LOCK.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_status"               , "<html><p>Current status of this request. Possible values are GRANTED, CONVERT, WAIT, LOW_PRIORITY_CONVERT, LOW_PRIORITY_WAIT, or ABORT_BLOCKERS. For more information about low priority waits and abort blockers, see the <em>low_priority_lock_wait</em> section of <a href=\"https://msdn.microsoft.com/en-us/library/ms188388.aspx\">ALTER INDEX (Transact-SQL)</a>.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_reference_count"      , "<html><p>Returns an approximate number of times the same requestor has requested this resource.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_lifetime"             , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_session_id"           , "<html><p>Session ID that currently owns this request. The owning session ID can change for distributed and bound transactions. A value of -2 indicates that the request belongs to an orphaned distributed transaction. A value of -3 indicates that the request belongs to a deferred recovery transaction, such as, a transaction for which a rollback has been deferred at recovery because the rollback could not be completed successfully.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_exec_context_id"      , "<html><p>Execution context ID of the process that currently owns this request.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_request_id"           , "<html><p>Request ID (batch ID) of the process that currently owns this request. This value will change every time that the active Multiple Active Result Set (MARS) connection for a transaction changes.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_owner_type"           , "<html><p>Entity type that owns the request. Lock manager requests can be owned by a variety of entities. Possible values are: </p><p>TRANSACTION = The request is owned by a transaction. </p><p>CURSOR = The request is owned by a cursor. </p><p>SESSION = The request is owned by a user session.</p><p>SHARED_TRANSACTION_WORKSPACE = The request is owned by the shared part of the transaction workspace.</p><p>EXCLUSIVE_TRANSACTION_WORKSPACE = The request is owned by the exclusive part of the transaction workspace.</p><p>NOTIFICATION_OBJECT = The request is owned by an internal SQL Server component. This component has requested the lock manager to notify it when another component is waiting to take the lock. The FileTable feature is a component that uses this value.</p><div class=\"alert\"> <div class=\"contentTableWrapper\">  <table>   <tbody>    <tr>     <th align=\"left\"><span><img id=\"s-e6f6a65cf14f462597b64ac058dbe1d0-system-media-system-caps-note\" alt=\"System_CAPS_note\" src=\"https://i-msdn.sec.s-msft.com/dynimg/IC101471.jpeg\" title=\"System_CAPS_note\" xmlns=\"\"></span><span class=\"alertTitle\">Note </span></th>    </tr>    <tr>     <td><p>Work spaces are used internally to hold locks for enlisted sessions.</p></td>    </tr>   </tbody>  </table> </div></div><p></p></html>");
			mtd.addColumn("dm_tran_locks", "request_owner_id"             , "<html><p>ID of the specific owner of this request. </p><p>When a transaction is the owner of the request, this value contains the transaction ID.</p><p>When a FileTable is the owner of the request, <strong>request_owner_id</strong> has one of the following values:</p><div> <div class=\"contentTableWrapper\">  <table responsive=\"true\">   <tbody>    <tr>     <th><p>Value</p></th>     <th><p>Description</p></th>    </tr>    <tr>     <td data-th=\"Value\"><p>-4</p></td>     <td data-th=\"Description\"><p>A FileTable has taken a database lock.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>-3</p></td>     <td data-th=\"Description\"><p>A FileTable has taken a table lock.</p></td>    </tr>    <tr>     <td data-th=\"Value\"><p>Other value</p></td>     <td data-th=\"Description\"><p>The value represents a file handle. This value also appears as <strong>fcb_id</strong> in the dynamic management view <a href=\"https://msdn.microsoft.com/en-us/library/ff929168.aspx\">sys.dm_filestream_non_transacted_handles (Transact-SQL)</a>.</p></td>    </tr>   </tbody>  </table> </div></div></html>");
			mtd.addColumn("dm_tran_locks", "request_owner_guid"           , "<html><p>GUID of the specific owner of this request. This value is only used by a distributed transaction where the value corresponds to the MS DTC GUID for that transaction.</p></html>");
			mtd.addColumn("dm_tran_locks", "request_owner_lockspace_id"   , "<html><p>Identified for informational purposes only. Not supported. Future compatibility is not guaranteed. This value represents the lockspace ID of the requestor. The lockspace ID determines whether two requestors are compatible with each other and can be granted locks in modes that would otherwise conflict with one another.</p></html>");
			mtd.addColumn("dm_tran_locks", "lock_owner_address"           , "<html><p>Memory address of the internal data structure that is used to track this request. This column can be joined the with <strong>resource_address</strong> column in <strong>sys.dm_os_waiting_tasks</strong>.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_locks' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_top_version_generators
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_top_version_generators",  "<p>Returns a virtual table for the objects that are producing the most versions in the version store. <strong>sys.dm_tran_top_version_generators</strong> returns the top 256 aggregated record lengths that are grouped by the <strong>database_id</strong> and <strong>rowset_id</strong>. <strong>sys.dm_tran_top_version_generators</strong> retrieves data by querying the <strong>dm_tran_version_store</strong> virtual table. <strong>sys.dm_tran_top_version_generators</strong> is an inefficient view to run because this view queries the version store, and the version store can be very large. We recommend that you use this function to find the largest consumers of the version store.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_top_version_generators", "database_id"                      , "<html><p>Database ID.</p></html>");
			mtd.addColumn("dm_tran_top_version_generators", "rowset_id"                        , "<html><p>Rowset ID.</p></html>");
			mtd.addColumn("dm_tran_top_version_generators", "aggregated_record_length_in_bytes", "<html><p>Sum of the record lengths for each <strong>database_id</strong> and <strong>rowset_id pair</strong> in the version store.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_top_version_generators' to MonTablesDictionary. Caught: " + e, e);
		}



		// ---------------------------------------------------------------------------------------
		// dm_tran_version_store
		// ---------------------------------------------------------------------------------------
		try
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

			// Table name and description
			mtd.addTable("dm_tran_version_store",  "<p>Returns a virtual table that displays all version records in the version store. <strong>sys.dm_tran_version_store</strong> is inefficient to run because it queries the entire version store, and the version store can be very large. </p><p>Each versioned record is stored as binary data together with some tracking or status information. Similar to records in database tables, version-store records are stored in 8192-byte pages. If a record exceeds 8192 bytes, the record will be split across two different records. </p><p>Because the versioned record is stored as binary, there are no problems with different collations from different databases. Use <strong>sys.dm_tran_version_store</strong> to find the previous versions of the rows in binary representation as they exist in the version store.</p>");

			// Column names and description
			mtd.addColumn("dm_tran_version_store", "transaction_sequence_num"          , "<html><p>Sequence number of the transaction that generates the record version.</p></html>");
			mtd.addColumn("dm_tran_version_store", "version_sequence_num"              , "<html><p>Version record sequence number. This value is unique within the version-generating transaction.</p></html>");
			mtd.addColumn("dm_tran_version_store", "database_id"                       , "<html><p>Database ID of the versioned record.</p></html>");
			mtd.addColumn("dm_tran_version_store", "rowset_id"                         , "<html><p>Rowset ID of the record.</p></html>");
			mtd.addColumn("dm_tran_version_store", "status"                            , "<html><p>Indicates whether a versioned record has been split across two records. If the value is 0, the record is stored in one page. If the value is 1, the record is split into two records that are stored on two different pages.</p></html>");
			mtd.addColumn("dm_tran_version_store", "min_length_in_bytes"               , "<html><p>Minimum length of the record in bytes.</p></html>");
			mtd.addColumn("dm_tran_version_store", "record_length_first_part_in_bytes" , "<html><p>Length of the first part of the versioned record in bytes.</p></html>");
			mtd.addColumn("dm_tran_version_store", "record_image_first_part"           , "<html><p>Binary image of the first part of version record.</p></html>");
			mtd.addColumn("dm_tran_version_store", "record_length_second_part_in_bytes", "<html><p>Length of the second part of version record in bytes.</p></html>");
			mtd.addColumn("dm_tran_version_store", "record_image_second_part"          , "<html><p>Binary image of the second part of the version record.</p></html>");
		}
		catch (NameNotFoundException e)
		{
			_logger.warn("Problems adding 'dm_tran_version_store' to MonTablesDictionary. Caught: " + e, e);
		}
	}
}
