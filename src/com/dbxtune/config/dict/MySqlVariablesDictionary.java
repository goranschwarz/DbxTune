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
package com.dbxtune.config.dict;

import java.util.HashMap;

import com.dbxtune.utils.StringUtil;

public class MySqlVariablesDictionary
{
	/** Instance variable */
	private static MySqlVariablesDictionary _instance = null;

	private HashMap<String, DescriptionRecord> _descriptionMap = new HashMap<String, DescriptionRecord>();

	public class DescriptionRecord
	{
		private String _key             = null;
		private String _description     = null;

		public DescriptionRecord(String key, String description)
		{
			_key         = key;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return StringUtil.left(_key, 50) + " - " + _description;
		}
	}


	public MySqlVariablesDictionary()
	{
		init();
	}

	public static MySqlVariablesDictionary getInstance()
	{
		if (_instance == null)
			_instance = new MySqlVariablesDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param waitName
	 * @return
	 */
	public String getDescriptionPlain(String waitName)
	{
		DescriptionRecord rec = _descriptionMap.get(waitName);
		if (rec != null)
			return StringUtil.stripHtml(rec._description);

		// Compose an empty one
		return "";
//		return "WaitName '"+waitName+"' not found in dictionary.";
	}


	public String getDescriptionHtml(String waitName)
	{
		DescriptionRecord rec = _descriptionMap.get(waitName);
		if (rec != null)
			return rec._description;

		// Compose an empty one
		return "<html><code>"+waitName+"</code> not found in dictionary.</html>";
	}


	private void set(DescriptionRecord rec)
	{
		if ( _descriptionMap.containsKey(rec._key))
			System.out.println("Key '"+rec._key+"' already exists. It will be overwritten.");

		_descriptionMap.put(rec._key, rec);
	}

	private void add(String key, String description)
	{
		set(new DescriptionRecord(key, description));
	}

	private void init()
	{
		add("Aborted_clients",                      "<html> The number of connections that were aborted because the client died without closing the connection properly. See Section B.5.2.10, 'Communication Errors and Aborted Connections'. </html> ");
		add("Aborted_connects",                     "<html> The number of failed attempts to connect to the MySQL server. See Section B.5.2.10, 'Communication Errors and Aborted Connections'. <br>" +
		                                                "For additional connection-related information, check the Connection_errors_xxx status variables and the host_cache table.<br>" +
		                                                "As of MySQL 5.7.3, Aborted_connects is not visible in the embedded server because for that server it is not updated and is not meaningful.<br>" +
		                                                "</html> ");
		add("Binlog_cache_disk_use",                "<html> The number of transactions that used the temporary binary log cache but that exceeded the value of binlog_cache_size and used a temporary file to store statements from the transaction. <br>" + 
		                                                "The number of nontransactional statements that caused the binary log transaction cache to be written to disk is tracked separately in theBinlog_stmt_cache_disk_use status variable. <br>" +
		                                                "</html> ");
		add("Binlog_cache_use",                     "<html> The number of transactions that used the binary log cache. </html> ");
		add("Binlog_stmt_cache_disk_use",           "<html> The number of nontransaction statements that used the binary log statement cache but that exceeded the value of binlog_stmt_cache_size and used a temporary file to store those statements. </html> ");
		add("Binlog_stmt_cache_use",                "<html> The number of nontransactional statements that used the binary log statement cache. </html> ");
		add("Bytes_received",                       "<html> The number of bytes received from all clients. </html> ");
		add("Bytes_sent",                           "<html> The number of bytes sent to all clients. </html> ");
		add("Com_xxx",                              "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Com_stmt_prepare",                     "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Com_stmt_execute",                     "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Com_stmt_fetch",                       "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Com_stmt_send_long_data",              "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Com_stmt_reset",                       "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Com_stmt_close",                       "<html> The Com_xxx statement counter variables indicate the number of times each xxx statement has been executed. There is one status variable for each type of statement. For example, Com_delete and Com_update count DELETE and UPDATE statements, respectively. Com_delete_multi andCom_update_multi are similar but apply to DELETE and UPDATE statements that use multiple-table syntax. <br>" +
		                                                "If a query result is returned from query cache, the server increments the Qcache_hits status variable, not Com_select. See Section 8.10.3.4, 'Query Cache Status and Maintenance'. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate these statement-counting status variables to other such variables. <br>" +
		                                                "All of the Com_stmt_xxx variables are increased even if a prepared statement argument is unknown or an error occurred during execution. In other words, their values correspond to the number of requests issued, not to the number of requests successfully completed. <br>" +
		                                                "The Com_stmt_xxx status variables are as follows: <br>" +
		                                                "<ul>" +
		                                                "<li>Com_stmt_prepare</li>" +
		                                                "<li>Com_stmt_execute</li>" +
		                                                "<li>Com_stmt_fetch</li>" +
		                                                "<li>Com_stmt_send_long_data</li>" +
		                                                "<li>Com_stmt_reset</li>" +
		                                                "<li>Com_stmt_close</li>" +
		                                                "</ul>" +
		                                                "Those variables stand for prepared statement commands. Their names refer to the COM_xxx command set used in the network layer. In other words, their values increase whenever prepared statement API calls such as mysql_stmt_prepare(), mysql_stmt_execute(), and so forth are executed. However, Com_stmt_prepare, Com_stmt_execute and Com_stmt_close also increase for PREPARE, EXECUTE, or DEALLOCATE PREPARE, respectively. Additionally, the values of the older statement counter variables Com_prepare_sql, Com_execute_sql, and Com_dealloc_sql increase for the PREPARE,EXECUTE, and DEALLOCATE PREPARE statements. Com_stmt_fetch stands for the total number of network round-trips issued when fetching from cursors. <br>" +
		                                                "Com_stmt_reprepare indicates the number of times statements were automatically reprepared by the server after metadata changes to tables or views referred to by the statement. A reprepare operation increments Com_stmt_reprepare, and also Com_stmt_prepare. <br>" +
		                                                "Com_explain_other indicates the number of EXPLAIN FOR CONNECTION statements executed. See Section 8.8.4, 'Obtaining Execution Plan Information for a Named Connection'. It was introduced in MySQL 5.7.2. <br>" +
		                                                "Com_change_repl_filter indicates the number of CHANGE REPLICATION FILTER statements executed. It was introduced in MySQL 5.7.3. <br>" +
		                                                "</html> ");
		add("Compression",                          "<html> Whether the client connection uses compression in the client/server protocol. </html> ");
		add("Connection_errors_accept",             "<html> The number of errors that occurred during calls to accept() on the listening port. <br>" +
		                                                "These variables provide information about errors that occur during the client connection process. They are global only and represent error counts aggregated across connections from all hosts. These variables track errors not accounted for by the host cache (see Section 8.12.5.2, 'DNS Lookup Optimization and the Host Cache'), such as errors that are not associated with TCP connections, occur very early in the connection process (even before an IP address is known), or are not specific to any particular IP address (such as out-of-memory conditions). <br>" +
		                                                "As of MySQL 5.7.3, the Connection_errors_xxx status variables are not visible in the embedded server because for that server they are not updated and are not meaningful. <br>" +
		                                                "</html> ");
		add("Connection_errors_internal",           "<html> The number of connections refused due to internal errors in the server, such as failure to start a new thread or an out-of-memory condition. <br>" +
		                                                "These variables provide information about errors that occur during the client connection process. They are global only and represent error counts aggregated across connections from all hosts. These variables track errors not accounted for by the host cache (see Section 8.12.5.2, 'DNS Lookup Optimization and the Host Cache'), such as errors that are not associated with TCP connections, occur very early in the connection process (even before an IP address is known), or are not specific to any particular IP address (such as out-of-memory conditions). <br>" +
		                                                "As of MySQL 5.7.3, the Connection_errors_xxx status variables are not visible in the embedded server because for that server they are not updated and are not meaningful. <br>" +
		                                                "</html> ");
		add("Connection_errors_max_connections",    "<html> The number of connections refused because the server max_connections limit was reached. <br>" +
		                                                "These variables provide information about errors that occur during the client connection process. They are global only and represent error counts aggregated across connections from all hosts. These variables track errors not accounted for by the host cache (see Section 8.12.5.2, 'DNS Lookup Optimization and the Host Cache'), such as errors that are not associated with TCP connections, occur very early in the connection process (even before an IP address is known), or are not specific to any particular IP address (such as out-of-memory conditions). <br>" +
		                                                "As of MySQL 5.7.3, the Connection_errors_xxx status variables are not visible in the embedded server because for that server they are not updated and are not meaningful. <br>" +
		                                                "</html> ");
		add("Connection_errors_peer_address",       "<html> The number of errors that occurred while searching for connecting client IP addresses. <br>" +
		                                                "These variables provide information about errors that occur during the client connection process. They are global only and represent error counts aggregated across connections from all hosts. These variables track errors not accounted for by the host cache (see Section 8.12.5.2, 'DNS Lookup Optimization and the Host Cache'), such as errors that are not associated with TCP connections, occur very early in the connection process (even before an IP address is known), or are not specific to any particular IP address (such as out-of-memory conditions). <br>" +
		                                                "As of MySQL 5.7.3, the Connection_errors_xxx status variables are not visible in the embedded server because for that server they are not updated and are not meaningful. <br>" +
		                                                "</html> ");
		add("Connection_errors_select",             "<html> The number of errors that occurred during calls to select() or poll() on the listening port. (Failure of this operation does not necessarily means a client connection was rejected.) <br>" +
		                                                "These variables provide information about errors that occur during the client connection process. They are global only and represent error counts aggregated across connections from all hosts. These variables track errors not accounted for by the host cache (see Section 8.12.5.2, 'DNS Lookup Optimization and the Host Cache'), such as errors that are not associated with TCP connections, occur very early in the connection process (even before an IP address is known), or are not specific to any particular IP address (such as out-of-memory conditions). <br>" +
		                                                "As of MySQL 5.7.3, the Connection_errors_xxx status variables are not visible in the embedded server because for that server they are not updated and are not meaningful. <br>" +
		                                                "</html> ");
		add("Connection_errors_tcpwrap",            "<html> The number of connections refused by the libwrap library. <br>" +
		                                                "These variables provide information about errors that occur during the client connection process. They are global only and represent error counts aggregated across connections from all hosts. These variables track errors not accounted for by the host cache (see Section 8.12.5.2, 'DNS Lookup Optimization and the Host Cache'), such as errors that are not associated with TCP connections, occur very early in the connection process (even before an IP address is known), or are not specific to any particular IP address (such as out-of-memory conditions). <br>" +
		                                                "As of MySQL 5.7.3, the Connection_errors_xxx status variables are not visible in the embedded server because for that server they are not updated and are not meaningful. <br>" +
		                                                "</html> ");
		add("Connections",                          "<html> The number of connection attempts (successful or not) to the MySQL server. </html> ");
		add("Created_tmp_disk_tables",              "<html> The number of internal on-disk temporary tables created by the server while executing statements. <br>" +
		                                                "If an internal temporary table is created initially as an in-memory table but becomes too large, MySQL automatically converts it to an on-disk table. The maximum size for in-memory temporary tables is the minimum of the tmp_table_size and max_heap_table_size values. If Created_tmp_disk_tables is large, you may want to increase the tmp_table_size or max_heap_table_size value to lessen the likelihood that internal temporary tables in memory will be converted to on-disk tables. <br>" +
		                                                "You can compare the number of internal on-disk temporary tables created to the total number of internal temporary tables created by comparing the values of the Created_tmp_disk_tables and Created_tmp_tables variables. <br>" +
		                                                "See also Section 8.4.4, 'Internal Temporary Table Use in MySQL'. <br>" +
		                                                "</html> ");
		add("Created_tmp_files",                    "<html> How many temporary files mysqld has created. </html> ");
		add("Created_tmp_tables",                   "<html> The number of internal temporary tables created by the server while executing statements. <br>" +
		                                                "You can compare the number of internal on-disk temporary tables created to the total number of internal temporary tables created by comparing the values of the Created_tmp_disk_tables and Created_tmp_tables variables. <br>" +
		                                                "See also Section 8.4.4, 'Internal Temporary Table Use in MySQL'. <br>" +
		                                                "Each invocation of the SHOW STATUS statement uses an internal temporary table and increments the global Created_tmp_tables value. <br>" +
		                                                "</html> ");
		add("Delayed_errors",                       "<html> This status variable is deprecated (because DELAYED inserts are not supported), and will be removed in a future release. </html> ");
		add("Delayed_insert_threads",               "<html> This status variable is deprecated (because DELAYED inserts are not supported), and will be removed in a future release. </html> ");
		add("Delayed_writes",                       "<html> This status variable is deprecated (because DELAYED inserts are not supported), and will be removed in a future release. </html> ");
		add("Flush_commands",                       "<html> The number of times the server flushes tables, whether because a user executed a FLUSH TABLES statement or due to internal server operation. It is also incremented by receipt of a COM_REFRESH packet. This is in contrast to Com_flush, which indicates how many FLUSH statements have been executed, whether FLUSH TABLES, FLUSH LOGS, and so forth. </html> ");
		add("Handler_commit",                       "<html> The number of internal COMMIT statements. </html> ");
		add("Handler_delete",                       "<html> The number of times that rows have been deleted from tables. </html> ");
		add("Handler_external_lock",                "<html> The server increments this variable for each call to its external_lock() function, which generally occurs at the beginning and end of access to a table instance. There might be differences among storage engines. This variable can be used, for example, to discover for a statement that accesses a partitioned table how many partitions were pruned before locking occurred: Check how much the counter increased for the statement, subtract 2 (2 calls for the table itself), then divide by 2 to get the number of partitions locked. </html> ");
		add("Handler_mrr_init",                     "<html> The number of times the server uses a storage engine's own Multi-Range Read implementation for table access. </html> ");
		add("Handler_prepare",                      "<html> A counter for the prepare phase of two-phase commit operations. </html> ");
		add("Handler_read_first",                   "<html> The number of times the first entry in an index was read. If this value is high, it suggests that the server is doing a lot of full index scans; for example, SELECT col1 FROM foo, assuming that col1 is indexed. </html> ");
		add("Handler_read_key",                     "<html> The number of requests to read a row based on a key. If this value is high, it is a good indication that your tables are properly indexed for your queries. </html> ");
		add("Handler_read_last",                    "<html> The number of requests to read the last key in an index. With ORDER BY, the server will issue a first-key request followed by several next-key requests, whereas with ORDER BY DESC, the server will issue a last-key request followed by several previous-key requests. </html> ");
		add("Handler_read_next",                    "<html> The number of requests to read the next row in key order. This value is incremented if you are querying an index column with a range constraint or if you are doing an index scan. </html> ");
		add("Handler_read_prev",                    "<html> The number of requests to read the previous row in key order. This read method is mainly used to optimize ORDER BY ... DESC. </html> ");
		add("Handler_read_rnd",                     "<html> The number of requests to read a row based on a fixed position. This value is high if you are doing a lot of queries that require sorting of the result. You probably have a lot of queries that require MySQL to scan entire tables or you have joins that do not use keys properly. </html> ");
		add("Handler_read_rnd_next",                "<html> The number of requests to read the next row in the data file. This value is high if you are doing a lot of table scans. Generally this suggests that your tables are not properly indexed or that your queries are not written to take advantage of the indexes you have. </html> ");
		add("Handler_rollback",                     "<html> The number of requests for a storage engine to perform a rollback operation. </html> ");
		add("Handler_savepoint",                    "<html> The number of requests for a storage engine to place a savepoint. </html> ");
		add("Handler_savepoint_rollback",           "<html> The number of requests for a storage engine to roll back to a savepoint. </html> ");
		add("Handler_update",                       "<html> The number of requests to update a row in a table. </html> ");
		add("Handler_write",                        "<html> The number of requests to insert a row in a table. </html> ");
		add("Innodb_available_undo_logs",           "<html> The total number of available InnoDB rollback segments. Supplements the innodb_rollback_segments system variable, which defines the number of active rollback segments. <br>" +
		                                                "One rollback segment always resides in the system tablespace, and 32 rollback segments are reserved for use by temporary tables and are hosted in the temporary tablespace (ibtmp1). See Section 14.4.12.1, 'Temporary Table Undo Logs'. <br>" +
		                                                "If you initiate a MySQL instance with 32 or fewer rollback segments, InnoDB still assigns one rollback segment to the system tablespace and 32 rollback segments to the temporary tablespace. In this case, Innodb_available_undo_logs reports 33 available rollback segments even though the instance was initialized with a lesser innodb_rollback_segments value. <br>" +
		                                                "<b>Note</b>: The Innodb_available_undo_logs status variable is deprecated as of MySQL 5.7.19 and will be removed in a future release. <br>" +
		                                                "</html> ");
		add("Innodb_buffer_pool_dump_status",       "<html> The progress of an operation to record the pages held in the InnoDB buffer pool, triggered by the setting of innodb_buffer_pool_dump_at_shutdownor innodb_buffer_pool_dump_now. <br>" +
		                                                "For related information and examples, see Section 14.6.3.8, 'Saving and Restoring the Buffer Pool State'. <br>" +
		                                                "</html> ");
		add("Innodb_buffer_pool_load_status",       "<html> The progress of an operation to warm up the InnoDB buffer pool by reading in a set of pages corresponding to an earlier point in time, triggered by the setting of innodb_buffer_pool_load_at_startup or innodb_buffer_pool_load_now. If the operation introduces too much overhead, you can cancel it by setting innodb_buffer_pool_load_abort. <br>" +
		                                                "For related information and examples, see Section 14.6.3.8, 'Saving and Restoring the Buffer Pool State'. <br>" +
		                                                "</html> ");
		add("Innodb_buffer_pool_bytes_data",        "<html> The total number of bytes in the InnoDB buffer pool containing data. The number includes both dirty and clean pages. For more accurate memory usage calculations than with Innodb_buffer_pool_pages_data, when compressed tables cause the buffer pool to hold pages of different sizes. </html> ");
		add("Innodb_buffer_pool_pages_data",        "<html> The number of pages in the InnoDB buffer pool containing data. The number includes both dirty and clean pages. When using compressed tables, the reported Innodb_buffer_pool_pages_data value may be larger than Innodb_buffer_pool_pages_total (Bug #59550). </html> ");
		add("Innodb_buffer_pool_bytes_dirty",       "<html> The total current number of bytes held in dirty pages in the InnoDB buffer pool. For more accurate memory usage calculations than withInnodb_buffer_pool_pages_dirty, when compressed tables cause the buffer pool to hold pages of different sizes. </html> ");
		add("Innodb_buffer_pool_pages_dirty",       "<html> The current number of dirty pages in the InnoDB buffer pool. </html> ");
		add("Innodb_buffer_pool_pages_flushed",     "<html> The number of requests to flush pages from the InnoDB buffer pool. </html> ");
		add("Innodb_buffer_pool_pages_free",        "<html> The number of free pages in the InnoDB buffer pool. </html> ");
		add("Innodb_buffer_pool_pages_latched",     "<html> The number of latched pages in the InnoDB buffer pool. These are pages currently being read or written, or that cannot be flushed or removed for some other reason. Calculation of this variable is expensive, so it is available only when the UNIV_DEBUG system is defined at server build time. </html> ");
		add("Innodb_buffer_pool_pages_misc",        "<html> The number of pages in the InnoDB buffer pool that are busy because they have been allocated for administrative overhead, such as row locks or theadaptive hash index. This value can also be calculated as Innodb_buffer_pool_pages_total - Innodb_buffer_pool_pages_free -Innodb_buffer_pool_pages_data. When using compressed tables, Innodb_buffer_pool_pages_misc may report an out-of-bounds value (Bug #59550). </html> ");
		add("Innodb_buffer_pool_pages_total",       "<html> The total size of the InnoDB buffer pool, in pages. When using compressed tables, the reported Innodb_buffer_pool_pages_data value may be larger than Innodb_buffer_pool_pages_total (Bug #59550) </html> ");
		add("Innodb_buffer_pool_read_ahead",        "<html> The number of pages read into the InnoDB buffer pool by the read-ahead background thread. </html> ");
		add("Innodb_buffer_pool_read_ahead_evicted","<html> The number of pages read into the InnoDB buffer pool by the read-ahead background thread that were subsequently evicted without having been accessed by queries. </html> ");
		add("Innodb_buffer_pool_read_ahead_rnd",    "<html> The number of 'random' read-aheads initiated by InnoDB. This happens when a query scans a large portion of a table but in random order. </html> ");
		add("Innodb_buffer_pool_read_requests",     "<html> The number of logical read requests. </html> ");
		add("Innodb_buffer_pool_reads",             "<html> The number of logical reads that InnoDB could not satisfy from the buffer pool, and had to read directly from disk. </html> ");
		add("Innodb_buffer_pool_resize_status",     "<html> The status of an operation to resize the InnoDB buffer pool dynamically, triggered by setting the innodb_buffer_pool_size parameter dynamically. As of MySQL 5.7.5, the innodb_buffer_pool_size parameter is dynamic, which allows you to resize the buffer pool without restarting the server. SeeConfiguring InnoDB Buffer Pool Size Online for related information. </html> ");
		add("Innodb_buffer_pool_wait_free",         "<html> Normally, writes to the InnoDB buffer pool happen in the background. When InnoDB needs to read or create a page and no clean pages are available, InnoDB flushes some dirty pages first and waits for that operation to finish. This counter counts instances of these waits. If innodb_buffer_pool_sizehas been set properly, this value should be small. </html> ");
		add("Innodb_buffer_pool_write_requests",    "<html> The number of writes done to the InnoDB buffer pool. </html> ");
		add("Innodb_data_fsyncs",                   "<html> The number of fsync() operations so far. The frequency of fsync() calls is influenced by the setting of the innodb_flush_method configuration option. </html> ");
		add("Innodb_data_pending_fsyncs",           "<html> The current number of pending fsync() operations. The frequency of fsync() calls is influenced by the setting of the innodb_flush_methodconfiguration option. </html> ");
		add("Innodb_data_pending_reads",            "<html> The current number of pending reads. </html> ");
		add("Innodb_data_pending_writes",           "<html> The current number of pending writes. </html> ");
		add("Innodb_data_read",                     "<html> The amount of data read since the server was started (in bytes). </html> ");
		add("Innodb_data_reads",                    "<html> The total number of data reads (OS file reads). </html> ");
		add("Innodb_data_writes",                   "<html> The total number of data writes. </html> ");
		add("Innodb_data_written",                  "<html> The amount of data written so far, in bytes. </html> ");
		add("Innodb_dblwr_pages_written",           "<html> The number of pages that have been written to the doublewrite buffer. See Section 14.12.1, 'InnoDB Disk I/O'. </html> ");
		add("Innodb_dblwr_writes",                  "<html> The number of doublewrite operations that have been performed. See Section 14.12.1, 'InnoDB Disk I/O'. </html> ");
		add("Innodb_have_atomic_builtins",          "<html> Indicates whether the server was built with atomic instructions. </html> ");
		add("Innodb_log_waits",                     "<html> The number of times that the log buffer was too small and a wait was required for it to be flushed before continuing. </html> ");
		add("Innodb_log_write_requests",            "<html> The number of write requests for the InnoDB redo log. </html> ");
		add("Innodb_log_writes",                    "<html> The number of physical writes to the InnoDB redo log file. </html> ");
		add("Innodb_num_open_files",                "<html> The number of files InnoDB currently holds open. </html> ");
		add("Innodb_os_log_fsyncs",                 "<html> The number of fsync() writes done to the InnoDB redo log files. </html> ");
		add("Innodb_os_log_pending_fsyncs",         "<html> The number of pending fsync() operations for the InnoDB redo log files. </html> ");
		add("Innodb_os_log_pending_writes",         "<html> The number of pending writes to the InnoDB redo log files. </html> ");
		add("Innodb_os_log_written",                "<html> The number of bytes written to the InnoDB redo log files. </html> ");
		add("Innodb_page_size",                     "<html> InnoDB page size (default 16KB). Many values are counted in pages; the page size enables them to be easily converted to bytes. </html> ");
		add("Innodb_pages_created",                 "<html> The number of pages created by operations on InnoDB tables. </html> ");
		add("Innodb_pages_read",                    "<html> The number of pages read from the InnoDB buffer pool by operations on InnoDB tables. </html> ");
		add("Innodb_pages_written",                 "<html> The number of pages written by operations on InnoDB tables. </html> ");
		add("Innodb_row_lock_current_waits",        "<html> The number of row locks currently being waited for by operations on InnoDB tables. </html> ");
		add("Innodb_row_lock_time",                 "<html> The total time spent in acquiring row locks for InnoDB tables, in milliseconds. </html> ");
		add("Innodb_row_lock_time_avg",             "<html> The average time to acquire a row lock for InnoDB tables, in milliseconds. </html> ");
		add("Innodb_row_lock_time_max",             "<html> The maximum time to acquire a row lock for InnoDB tables, in milliseconds. </html> ");
		add("Innodb_row_lock_waits",                "<html> The number of times operations on InnoDB tables had to wait for a row lock. </html> ");
		add("Innodb_rows_deleted",                  "<html> The number of rows deleted from InnoDB tables. </html> ");
		add("Innodb_rows_inserted",                 "<html> The number of rows inserted into InnoDB tables. </html> ");
		add("Innodb_rows_read",                     "<html> The number of rows read from InnoDB tables. </html> ");
		add("Innodb_rows_updated",                  "<html> The number of rows updated in InnoDB tables. </html> ");
		add("Innodb_truncated_status_writes",       "<html> The number of times output from the SHOW ENGINE INNODB STATUS statement has been truncated. </html> ");
		add("Key_blocks_not_flushed",               "<html> The number of key blocks in the MyISAM key cache that have changed but have not yet been flushed to disk. </html> ");
		add("Key_blocks_unused",                    "<html> The number of unused blocks in the MyISAM key cache. You can use this value to determine how much of the key cache is in use; see the discussion ofkey_buffer_size in Section 5.1.5, 'Server System Variables'. </html> ");
		add("Key_blocks_used",                      "<html> The number of used blocks in the MyISAM key cache. This value is a high-water mark that indicates the maximum number of blocks that have ever been in use at one time. </html> ");
		add("Key_read_requests",                    "<html> The number of requests to read a key block from the MyISAM key cache. </html> ");
		add("Key_reads",                            "<html> The number of physical reads of a key block from disk into the MyISAM key cache. If Key_reads is large, then your key_buffer_size value is probably too small. The cache miss rate can be calculated as Key_reads/Key_read_requests. </html> ");
		add("Key_write_requests",                   "<html> The number of requests to write a key block to the MyISAM key cache. </html> ");
		add("Key_writes",                           "<html> The number of physical writes of a key block from the MyISAM key cache to disk. </html> ");
		add("Last_query_cost",                      "<html> The total cost of the last compiled query as computed by the query optimizer. This is useful for comparing the cost of different query plans for the same query. The default value of 0 means that no query has been compiled yet. The default value is 0. Last_query_cost has session scope. <br>" +
		                                                "The Last_query_cost value can be computed accurately only for simple 'flat' queries, not complex queries such as those with subqueries or UNION. For the latter, the value is set to 0. <br>" +
		                                                "</html> ");
		add("Last_query_partial_plans",             "<html> The number of iterations the query optimizer made in execution plan construction for the previous query. Last_query_cost has session scope. </html> ");
		add("Locked_connects",                      "<html> The number of attempts to connect to locked user accounts. For information about account locking and unlocking, see Section 6.3.11, 'User Account Locking'. <br>" +
		                                                "This variable was added in MySQL 5.7.6. <br>" +
		                                                "</html> ");
		add("Max_execution_time_exceeded",          "<html> The number of SELECT statements for which the execution timeout was exceeded. </html> ");
		add("Max_execution_time_set",               "<html> The number of SELECT statements for which a nonzero execution timeout was set. This includes statements that include a nonzeroMAX_EXECUTION_TIME optimizer hint, and statements that include no such hint but execute while the timeout indicated by the max_execution_timesystem variable is nonzero. </html> ");
		add("Max_execution_time_set_failed",        "<html> The number of SELECT statements for which the attempt to set an execution timeout failed. </html> ");
		add("Max_statement_time_exceeded",          "<html> This variable was renamed to Max_execution_time_exceeded in MySQL 5.7.8. </html> ");
		add("Max_statement_time_set",               "<html> This variable was renamed to Max_execution_time_set in MySQL 5.7.8. </html> ");
		add("Max_statement_time_set_failed",        "<html> This variable was renamed to Max_execution_time_set_failed in MySQL 5.7.8. </html> ");
		add("Max_used_connections",                 "<html> The maximum number of connections that have been in use simultaneously since the server started. </html> ");
		add("Max_used_connections_time",            "<html> The time at which Max_used_connections reached its current value. This variable was added in MySQL 5.7.5. </html> ");
		add("Not_flushed_delayed_rows",             "<html> This status variable is deprecated (because DELAYED inserts are not supported), and will be removed in a future release. </html> ");
		add("mecab_charset",                        "<html> The character set currently used by the MeCab full-text parser plugin. For related information, see Section 12.9.9, 'MeCab Full-Text Parser Plugin'. </html> ");
		add("Ongoing_anonymous_transaction_count",  "<html> Shows the number of ongoing transactions which have been marked as anonymous. This can be used to ensure that no further transactions are waiting to be processed. This variable was added in MySQL 5.7.6. </html> ");
		add("Ongoing_anonymous_gtid_violating_transaction_count", "<html> This status variable is only available in debug builds. Shows the number of ongoing transactions which use gtid_next=ANONYMOUS and that violate GTID consistency. </html> ");
		add("Ongoing_automatic_gtid_violating_transaction_count", "<html> This status variable is only available in debug builds. Shows the number of ongoing transactions which use gtid_next=AUTOMATIC and that violate GTID consistency. </html> ");
		add("Open_files",                           "<html> The number of files that are open. This count includes regular files opened by the server. It does not include other types of files such as sockets or pipes. Also, the count does not include files that storage engines open using their own internal functions rather than asking the server level to do so. </html> ");
		add("Open_streams",                         "<html> The number of streams that are open (used mainly for logging). </html> ");
		add("Open_table_definitions",               "<html> The number of cached .frm files. </html> ");
		add("Open_tables",                          "<html> The number of tables that are open. </html> ");
		add("Opened_files",                         "<html> The number of files that have been opened with my_open() (a mysys library function). Parts of the server that open files without using this function do not increment the count. </html> ");
		add("Opened_table_definitions",             "<html> The number of .frm files that have been cached. </html> ");
		add("Opened_tables",                        "<html> The number of tables that have been opened. If Opened_tables is big, your table_open_cache value is probably too small. </html> ");
		add("Performance_schema_xxx",               "<html> Performance Schema status variables are listed in Section 25.15, 'Performance Schema Status Variables'. These variables provide information about instrumentation that could not be loaded or created due to memory constraints. </html> ");
		add("Prepared_stmt_count",                  "<html> The current number of prepared statements. (The maximum number of statements is given by the max_prepared_stmt_count system variable.) </html> ");
		add("Qcache_free_blocks",                   "<html> The number of free memory blocks in the query cache. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_free_blocks. <br>" +
		                                                "</html> ");
		add("Qcache_free_memory",                   "<html> The amount of free memory for the query cache. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_free_memory. <br>" +
		                                                "</html> ");
		add("Qcache_hits",                          "<html> The number of query cache hits. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate this statement-counting status variable to other such variables. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_hits. <br>" +
		                                                "</html> ");
		add("Qcache_inserts",                       "<html> The number of queries added to the query cache. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_inserts. <br>" +
		                                                "</html> ");
		add("Qcache_lowmem_prunes",                 "<html> The number of queries that were deleted from the query cache because of low memory. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_lowmem_prunes. <br>" +
		                                                "</html> ");
		add("Qcache_not_cached",                    "<html> The number of noncached queries (not cacheable, or not cached due to the query_cache_type setting). <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_not_cached. <br>" +
		                                                "</html> ");
		add("Qcache_queries_in_cache",              "<html> The number of queries registered in the query cache. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_queries_in_cache. <br>" +
		                                                "</html> ");
		add("Qcache_total_blocks",                  "<html> The total number of blocks in the query cache. <br>" +
		                                                "<b>Note</b>: The query cache is deprecated as of MySQL 5.7.20, and is removed in MySQL 8.0. Deprecation includes Qcache_total_blocks. <br>" +
		                                                "</html> ");
		add("Queries",                              "<html> The number of statements executed by the server. This variable includes statements executed within stored programs, unlike the Questions variable. It does not count COM_PING or COM_STATISTICS commands. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate this statement-counting status variable to other such variables. <br>" +
		                                                "</html> ");
		add("Questions",                            "<html> The number of statements executed by the server. This includes only statements sent to the server by clients and not statements executed within stored programs, unlike the Queries variable. This variable does not count COM_PING, COM_STATISTICS, COM_STMT_PREPARE, COM_STMT_CLOSE, orCOM_STMT_RESET commands. <br>" +
		                                                "The discussion at the beginning of this section indicates how to relate this statement-counting status variable to other such variables. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_clients",         "<html> The number of semisynchronous slaves. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_net_avg_wait_time","<html> The average time in microseconds the master waited for a slave reply. This variable is deprecated, always 0, and will be removed in a future version. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_net_wait_time",   "<html> The total time in microseconds the master waited for slave replies. This variable is deprecated, always 0, and will be removed in a future version. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_net_waits",       "<html> The total number of times the master waited for slave replies. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_no_times",        "<html> The number of times the master turned off semisynchronous replication. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_no_tx",           "<html> The number of commits that were not acknowledged successfully by a slave. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_status",          "<html> Whether semisynchronous replication currently is operational on the master. The value is ON if the plugin has been enabled and a commit acknowledgment has occurred. It is OFF if the plugin is not enabled or the master has fallen back to asynchronous replication due to commit acknowledgment timeout. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_timefunc_failures","<html> The number of times the master failed when calling time functions such as gettimeofday(). <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_tx_avg_wait_time","<html> The average time in microseconds the master waited for each transaction. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_tx_wait_time",    "<html> The total time in microseconds the master waited for transactions. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_tx_waits",        "<html> The total number of times the master waited for transactions. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_wait_pos_backtraverse", "<html> The total number of times the master waited for an event with binary coordinates lower than events waited for previously. This can occur when the order in which transactions start waiting for a reply is different from the order in which their binary log events are written. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_wait_sessions",   "<html> The number of sessions currently waiting for slave replies. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_master_yes_tx",          "<html> The number of commits that were acknowledged successfully by a slave. <br>" +
		                                                "This variable is available only if the master-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rpl_semi_sync_slave_status",           "<html> Whether semisynchronous replication currently is operational on the slave. This is ON if the plugin has been enabled and the slave I/O thread is running,OFF otherwise. <br>" +
		                                                "This variable is available only if the slave-side semisynchronous replication plugin is installed. <br>" +
		                                                "</html> ");
		add("Rsa_public_key",                       "<html> This variable is available if MySQL was using OpenSSL (see Section 6.4.4, 'OpenSSL Versus yaSSL'). Its value is the RSA public key value used by thesha256_password authentication plugin. The value is nonempty only if the server successfully initializes the private and public keys in the files named by the sha256_password_private_key_path and sha256_password_public_key_path system variables. The value of Rsa_public_key comes from the latter file. <br>" +
		                                                "For information about sha256_password, see Section 6.5.1.4, 'SHA-256 Pluggable Authentication'. <br>" +
		                                                "</html> ");
		add("Select_full_join",                     "<html> The number of joins that perform table scans because they do not use indexes. If this value is not 0, you should carefully check the indexes of your tables. </html> ");
		add("Select_full_range_join",               "<html> The number of joins that used a range search on a reference table. </html> ");
		add("Select_range",                         "<html> The number of joins that used ranges on the first table. This is normally not a critical issue even if the value is quite large. </html> ");
		add("Select_range_check",                   "<html> The number of joins without keys that check for key usage after each row. If this is not 0, you should carefully check the indexes of your tables. </html> ");
		add("Select_scan",                          "<html> The number of joins that did a full scan of the first table. </html> ");
		add("Slave_heartbeat_period",               "<html> Shows the replication heartbeat interval (in seconds) on a replication slave. <br>" +
		                                                "This variable is affected by the value of the show_compatibility_56 system variable. For details, see Effect of show_compatibility_56 on Slave Status Variables. <br>" +
		                                                "<b>Note</b>: This variable only shows the status of the default replication channel. To monitor any replication channel, use the HEARTBEAT_INTERVAL column in the replication_connection_status table for the replication channel.Slave_heartbeat_period is deprecated and will be removed in a future MySQL release. <br>" +
		                                                "</html> ");
		add("Slave_last_heartbeat",                 "<html> Shows when the most recent heartbeat signal was received by a replication slave, as a TIMESTAMP value. <br>" +
		                                                "This variable is affected by the value of the show_compatibility_56 system variable. For details, see Effect of show_compatibility_56 on Slave Status Variables. <br>" +
		                                                "<b>Note</b>: This variable only shows the status of the default replication channel. To monitor any replication channel, use the LAST_HEARTBEAT_TIMESTAMP column in the replication_connection_status table for the replication channel.Slave_last_heartbeat is deprecated and will be removed in a future MySQL release. <br>" +
		                                                "</html> ");
		add("Slave_open_temp_tables",               "<html> The number of temporary tables that the slave SQL thread currently has open. If the value is greater than zero, it is not safe to shut down the slave; seeSection 16.4.1.30, 'Replication and Temporary Tables'. This variable reports the total count of open temporary tables for all replication channels. <br> " +
		                                                "This counter increments with each replication heartbeat received by a replication slave since the last time that the slave was restarted or reset, or a CHANGE MASTER TO statement was issued. <br>" +
		                                                "This variable is affected by the value of the show_compatibility_56 system variable. For details, see Effect of show_compatibility_56 on Slave Status Variables. <br>" +
		                                                "<b>Note</b>: This variable only shows the status of the default replication channel. To monitor any replication channel, use the COUNT_RECEIVED_HEARTBEATS column in the replication_connection_status table for the replication channel.Slave_received_heartbeats is deprecated and will be removed in a future MySQL release. <br>" +
		                                                "</html> ");
		add("Slave_retried_transactions",           "<html> The total number of times since startup that the replication slave SQL thread has retried transactions. <br>" +
		                                                "This variable is affected by the value of the show_compatibility_56 system variable. For details, see Effect of show_compatibility_56 on Slave Status Variables. <br>" +
		                                                "<b>Note</b>: This variable only shows the status of the default replication channel. To monitor any replication channel, use the COUNT_TRANSACTIONS_RETRIES column in the replication_applier_status table for the replication channel.Slave_retried_transactions is deprecated and will be removed in a future MySQL release. <br>" +
		                                                "</html> ");
		add("Slave_rows_last_search_algorithm_used","<html> The search algorithm that was most recently used by this slave to locate rows for row-based replication. The result shows whether the slave used indexes, a table scan, or hashing as the search algorithm for the last transaction executed on any channel. <br>" +
		                                                "The method used depends on the setting for the slave_rows_search_algorithms system variable, and the keys that are available on the relevant table. <br>" +
		                                                "</html> ");
		add("Slave_running",                        "<html> This is ON if this server is a replication slave that is connected to a replication master, and both the I/O and SQL threads are running; otherwise, it is OFF. <br>" +
		                                                "This variable is affected by the value of the show_compatibility_56 system variable. For details, see Effect of show_compatibility_56 on Slave Status Variables. <br>" +
		                                                "<b>Note</b>: This variable only shows the status of the default replication channel. To monitor any replication channel, use the SERVICE_STATEcolumn in the replication_applier_status or replication_connection_status tables of the replication channel.Slave_running is deprecated and will be removed in a future MySQL release. <br>" +
		                                                "</html> ");
		add("Slow_launch_threads",                  "<html> The number of threads that have taken more than slow_launch_time seconds to create. <br>" +
		                                                "This variable is not meaningful in the embedded server (libmysqld) and as of MySQL 5.7.2 is no longer visible within the embedded server. <br>" +
		                                                "</html> ");
		add("Slow_queries",                         "<html> The number of queries that have taken more than long_query_time seconds. This counter increments regardless of whether the slow query log is enabled. For information about that log, see Section 5.4.5, 'The Slow Query Log'. </html> ");
		add("Sort_merge_passes",                    "<html> The number of merge passes that the sort algorithm has had to do. If this value is large, you should consider increasing the value of the sort_buffer_size system variable. </html> ");
		add("Sort_range",                           "<html> The number of sorts that were done using ranges. </html> ");
		add("Sort_rows",                            "<html> The number of sorted rows. </html> ");
		add("Sort_scan",                            "<html> The number of sorts that were done by scanning the table. </html> ");
		add("Ssl_accept_renegotiates",              "<html> The number of negotiates needed to establish the connection. </html> ");
		add("Ssl_accepts",                          "<html> The number of accepted SSL connections. </html> ");
		add("Ssl_callback_cache_hits",              "<html> The number of callback cache hits. </html> ");
		add("Ssl_cipher",                           "<html> The current encryption cipher (empty for unencrypted connections). </html> ");
		add("Ssl_cipher_list",                      "<html> The list of possible SSL ciphers (empty for non-SSL connections). </html> ");
		add("Ssl_client_connects",                  "<html> The number of SSL connection attempts to an SSL-enabled master. </html> ");
		add("Ssl_connect_renegotiates",             "<html> The number of negotiates needed to establish the connection to an SSL-enabled master. </html> ");
		add("Ssl_ctx_verify_depth",                 "<html> The SSL context verification depth (how many certificates in the chain are tested). </html> ");
		add("Ssl_ctx_verify_mode",                  "<html> The SSL context verification mode. </html> ");
		add("Ssl_default_timeout",                  "<html> The default SSL timeout. </html> ");
		add("Ssl_finished_accepts",                 "<html> The number of successful SSL connections to the server. </html> ");
		add("Ssl_finished_connects",                "<html> The number of successful slave connections to an SSL-enabled master. </html> ");
		add("Ssl_server_not_after",                 "<html> The last date for which the SSL certificate is valid. To check SSL certificate expiration information, use this statement: <br> " +
		                                                "<pre> " +
		                                                "mysql> SHOW STATUS LIKE 'Ssl_server_not%'; \n" +
		                                                "+-----------------------+--------------------------+ \n" +
		                                                "| Variable_name         | Value                    | \n" +
		                                                "+-----------------------+--------------------------+ \n" +
		                                                "| Ssl_server_not_after  | Apr 28 14:16:39 2025 GMT | \n" +
		                                                "| Ssl_server_not_before | May  1 14:16:39 2015 GMT | \n" +
		                                                "+-----------------------+--------------------------+ \n" +
		                                                "</pre>" +
		                                                "</html> ");
		add("Ssl_server_not_before",                "<html> The first date for which the SSL certificate is valid. </html> ");
		add("Ssl_session_cache_hits",               "<html> The number of SSL session cache hits. </html> ");
		add("Ssl_session_cache_misses",             "<html> The number of SSL session cache misses. </html> ");
		add("Ssl_session_cache_mode",               "<html> The SSL session cache mode. </html> ");
		add("Ssl_session_cache_overflows",          "<html> The number of SSL session cache overflows. </html> ");
		add("Ssl_session_cache_size",               "<html> The SSL session cache size. </html> ");
		add("Ssl_session_cache_timeouts",           "<html> The number of SSL session cache timeouts. </html> ");
		add("Ssl_sessions_reused",                  "<html> How many SSL connections were reused from the cache. </html> ");
		add("Ssl_used_session_cache_entries",       "<html> How many SSL session cache entries were used. </html> ");
		add("Ssl_verify_depth",                     "<html> The verification depth for replication SSL connections. </html> ");
		add("Ssl_verify_mode",                      "<html> The verification mode used by the server for a connection that uses SSL. The value is a bitmask; bits are defined in the openssl/ssl.h header file: <br> " +
		                                                "<pre>" +
		                                                "# define SSL_VERIFY_NONE                 0x00 \n" +
		                                                "# define SSL_VERIFY_PEER                 0x01 \n" +
		                                                "# define SSL_VERIFY_FAIL_IF_NO_PEER_CERT 0x02 \n" +
		                                                "# define SSL_VERIFY_CLIENT_ONCE          0x04 \n" +
		                                                "</pre> " +
		                                                "SSL_VERIFY_PEER indicates that the server asks for a client certificate. If the client supplies one, the server performs verification and proceeds only if verification is successful. SSL_VERIFY_CLIENT_ONCE indicates that a request for the client certificate will be done only in the initial handshake. <br> " +
		                                                "</html> ");
		add("Ssl_version",                          "<html> The SSL protocol version of the connection; for example, TLSv1. If the connection is not encrypted, the value is empty. </html> ");
		add("Table_locks_immediate",                "<html> The number of times that a request for a table lock could be granted immediately. </html> ");
		add("Table_locks_waited",                   "<html> The number of times that a request for a table lock could not be granted immediately and a wait was needed. If this is high and you have performance problems, you should first optimize your queries, and then either split your table or tables or use replication. </html> ");
		add("Table_open_cache_hits",                "<html> The number of hits for open tables cache lookups. </html> ");
		add("Table_open_cache_misses",              "<html> The number of misses for open tables cache lookups. </html> ");
		add("Table_open_cache_overflows",           "<html> The number of overflows for the open tables cache. This is the number of times, after a table is opened or closed, a cache instance has an unused entry and the size of the instance is larger than table_open_cache / table_open_cache_instances. </html> ");
		add("Tc_log_max_pages_used",                "<html> For the memory-mapped implementation of the log that is used by mysqld when it acts as the transaction coordinator for recovery of internal XA transactions, this variable indicates the largest number of pages used for the log since the server started. If the product of Tc_log_max_pages_used andTc_log_page_size is always significantly less than the log size, the size is larger than necessary and can be reduced. (The size is set by the --log-tc-size option. This variable is unused: It is unneeded for binary log-based recovery, and the memory-mapped recovery log method is not used unless the number of storage engines that are capable of two-phase commit and that support XA transactions is greater than one. (InnoDB is the only applicable engine.) </html> ");
		add("Tc_log_page_size",                     "<html> The page size used for the memory-mapped implementation of the XA recovery log. The default value is determined using getpagesize(). This variable is unused for the same reasons as described for Tc_log_max_pages_used. </html> ");
		add("Tc_log_page_waits",                    "<html> For the memory-mapped implementation of the recovery log, this variable increments each time the server was not able to commit a transaction and had to wait for a free page in the log. If this value is large, you might want to increase the log size (with the --log-tc-size option). For binary log-based recovery, this variable increments each time the binary log cannot be closed because there are two-phase commits in progress. (The close operation waits until all such transactions are finished.) </html> ");
		add("Threads_cached",                       "<html> The number of threads in the thread cache. <br>" +
		                                                "This variable is not meaningful in the embedded server (libmysqld) and as of MySQL 5.7.2 is no longer visible within the embedded server. <br>" +
		                                                "</html> ");
		add("Threads_connected",                    "<html> The number of currently open connections. </html> ");
		add("Threads_created",                      "<html> The number of threads created to handle connections. If Threads_created is big, you may want to increase the thread_cache_size value. The cache miss rate can be calculated as Threads_created/Connections. </html> ");
		add("Threads_running",                      "<html> The number of threads that are not sleeping. </html> ");
		add("Uptime",                               "<html> The number of seconds that the server has been up. </html> ");
		add("Uptime_since_flush_status",            "<html> The number of seconds since the most recent FLUSH STATUS statement. </html> ");

	}
}
