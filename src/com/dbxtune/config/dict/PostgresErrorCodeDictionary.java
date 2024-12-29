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

public class PostgresErrorCodeDictionary
{
	/** Instance variable */
	private static PostgresErrorCodeDictionary _instance = null;

	private HashMap<String, Record> _entries = new HashMap<String, Record>();

	public class Record
	{
		private String _id              = null;
		private String _description     = null;

		public Record(String id, String description)
		{
			_id          = id;
			_description = description;
		}
		
		@Override
		public String toString()
		{
			return StringUtil.left(_id, 50) + " - " + _description;
		}
	}


	public PostgresErrorCodeDictionary()
	{
		init();
	}

	public static PostgresErrorCodeDictionary getInstance()
	{
		if (_instance == null)
			_instance = new PostgresErrorCodeDictionary();
		return _instance;
	}

	/**
	 * Strips out all HTML and return it as a "plain" text
	 * @param name
	 * @return
	 */
	public String getDescriptionPlain(String name)
	{
		if (name == null)
			return "";

		Record rec = _entries.get(name);
		if (rec != null)
			return rec._description;

		// Compose an empty one
		return "";
	}


	public String getDescriptionHtml(String name)
	{
		if (name == null)
			return "";

		String extraInfo = "";
//		String extraInfo = "<br><hr>External Description, from: Paul Randal, www.sqlskills.com<br>"
//				+ "Open in Tooltip Window:   <A HREF='https://www.sqlskills.com/help/waits/"+waitName+"'>https://www.sqlskills.com/help/waits/"+waitName+"</A><br>"
//				+ "Open in External Browser: <A HREF='"+CmToolTipSupplierDefault.OPEN_IN_EXTERNAL_BROWSER+"https://www.sqlskills.com/help/waits/"+waitName+"'>https://www.sqlskills.com/help/waits/"+waitName+"</A><br>"
//				+ "</html>";

		Record rec = _entries.get(name);
		if (rec != null)
		{
			return "<html><b>" + name + "</b> - " + rec._description + " </html>";
		}

		// Compose an empty one
		return "<html><code>" +name + "</code> not found in dictionary."+extraInfo;
	}


	private void set(Record rec)
	{
		if ( _entries.containsKey(rec._id))
			System.out.println("ID '"+rec._id+"' already exists. It will be overwritten.");

		_entries.put(rec._id, rec);
	}

	private void add(String id, String description)
	{
		set(new Record(id, description));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// https://www.postgresql.org/docs/current/errcodes-appendix.html
	//-----------------------------------------------------------------------------------------------------------------
	private void init()
	{
		// Class 00 - Successful Completion
		add("00000", "successful_completion");

		// Class 01 - Warning
		add("01000", "warning");
		add("0100C", "dynamic_result_sets_returned");
		add("01008", "implicit_zero_bit_padding");
		add("01003", "null_value_eliminated_in_set_function");
		add("01007", "privilege_not_granted");
		add("01006", "privilege_not_revoked");
		add("01004", "string_data_right_truncation");
		add("01P01", "deprecated_feature");

		// Class 02 - No Data (this is also a warning Class per the SQL standard)
		add("02000", "no_data");
		add("02001", "no_additional_dynamic_result_sets_returned");

		// Class 03 - SQL Statement Not Yet Complete
		add("03000", "sql_statement_not_yet_complete");

		// Class 08 - Connection Exception
		add("08000", "connection_exception");
		add("08003", "connection_does_not_exist");
		add("08006", "connection_failure");
		add("08001", "sqlclient_unable_to_establish_sqlconnection");
		add("08004", "sqlserver_rejected_establishment_of_sqlconnection");
		add("08007", "transaction_resolution_unknown");
		add("08P01", "protocol_violation");

		// Class 09 - Triggered Action Exception
		add("09000", "triggered_action_exception");

		// Class 0A - Feature Not Supported
		add("0A000", "feature_not_supported");

		// Class 0B - Invalid Transaction Initiation
		add("0B000", "invalid_transaction_initiation");

		// Class 0F - Locator Exception
		add("0F000", "locator_exception");
		add("0F001", "invalid_locator_specification");

		// Class 0L - Invalid Grantor
		add("0L000", "invalid_grantor");
		add("0LP01", "invalid_grant_operation");

		// Class 0P - Invalid Role Specification
		add("0P000", "invalid_role_specification");

		// Class 0Z - Diagnostics Exception
		add("0Z000", "diagnostics_exception");
		add("0Z002", "stacked_diagnostics_accessed_without_active_handler");

		// Class 20 - Case Not Found
		add("20000", "case_not_found");

		// Class 21 - Cardinality Violation
		add("21000", "cardinality_violation");

		// Class 22 - Data Exception
		add("22000", "data_exception");
		add("2202E", "array_subscript_error");
		add("22021", "character_not_in_repertoire");
		add("22008", "datetime_field_overflow");
		add("22012", "division_by_zero");
		add("22005", "error_in_assignment");
		add("2200B", "escape_character_conflict");
		add("22022", "indicator_overflow");
		add("22015", "interval_field_overflow");
		add("2201E", "invalid_argument_for_logarithm");
		add("22014", "invalid_argument_for_ntile_function");
		add("22016", "invalid_argument_for_nth_value_function");
		add("2201F", "invalid_argument_for_power_function");
		add("2201G", "invalid_argument_for_width_bucket_function");
		add("22018", "invalid_character_value_for_cast");
		add("22007", "invalid_datetime_format");
		add("22019", "invalid_escape_character");
		add("2200D", "invalid_escape_octet");
		add("22025", "invalid_escape_sequence");
		add("22P06", "nonstandard_use_of_escape_character");
		add("22010", "invalid_indicator_parameter_value");
		add("22023", "invalid_parameter_value");
		add("22013", "invalid_preceding_or_following_size");
		add("2201B", "invalid_regular_expression");
		add("2201W", "invalid_row_count_in_limit_clause");
		add("2201X", "invalid_row_count_in_result_offset_clause");
		add("2202H", "invalid_tablesample_argument");
		add("2202G", "invalid_tablesample_repeat");
		add("22009", "invalid_time_zone_displacement_value");
		add("2200C", "invalid_use_of_escape_character");
		add("2200G", "most_specific_type_mismatch");
		add("22004", "null_value_not_allowed");
		add("22002", "null_value_no_indicator_parameter");
		add("22003", "numeric_value_out_of_range");
		add("2200H", "sequence_generator_limit_exceeded");
		add("22026", "string_data_length_mismatch");
		add("22001", "string_data_right_truncation");
		add("22011", "substring_error");
		add("22027", "trim_error");
		add("22024", "unterminated_c_string");
		add("2200F", "zero_length_character_string");
		add("22P01", "floating_point_exception");
		add("22P02", "invalid_text_representation");
		add("22P03", "invalid_binary_representation");
		add("22P04", "bad_copy_file_format");
		add("22P05", "untranslatable_character");
		add("2200L", "not_an_xml_document");
		add("2200M", "invalid_xml_document");
		add("2200N", "invalid_xml_content");
		add("2200S", "invalid_xml_comment");
		add("2200T", "invalid_xml_processing_instruction");
		add("22030", "duplicate_json_object_key_value");
		add("22031", "invalid_argument_for_sql_json_datetime_function");
		add("22032", "invalid_json_text");
		add("22033", "invalid_sql_json_subscript");
		add("22034", "more_than_one_sql_json_item");
		add("22035", "no_sql_json_item");
		add("22036", "non_numeric_sql_json_item");
		add("22037", "non_unique_keys_in_a_json_object");
		add("22038", "singleton_sql_json_item_required");
		add("22039", "sql_json_array_not_found");
		add("2203A", "sql_json_member_not_found");
		add("2203B", "sql_json_number_not_found");
		add("2203C", "sql_json_object_not_found");
		add("2203D", "too_many_json_array_elements");
		add("2203E", "too_many_json_object_members");
		add("2203F", "sql_json_scalar_required");
		add("2203G", "sql_json_item_cannot_be_cast_to_target_type");

		// Class 23 - Integrity Constraint Violation
		add("23000", "integrity_constraint_violation");
		add("23001", "restrict_violation");
		add("23502", "not_null_violation");
		add("23503", "foreign_key_violation");
		add("23505", "unique_violation");
		add("23514", "check_violation");
		add("23P01", "exclusion_violation");

		// Class 24 - Invalid Cursor State
		add("24000", "invalid_cursor_state");

		// Class 25 - Invalid Transaction State
		add("25000", "invalid_transaction_state");
		add("25001", "active_sql_transaction");
		add("25002", "branch_transaction_already_active");
		add("25008", "held_cursor_requires_same_isolation_level");
		add("25003", "inappropriate_access_mode_for_branch_transaction");
		add("25004", "inappropriate_isolation_level_for_branch_transaction");
		add("25005", "no_active_sql_transaction_for_branch_transaction");
		add("25006", "read_only_sql_transaction");
		add("25007", "schema_and_data_statement_mixing_not_supported");
		add("25P01", "no_active_sql_transaction");
		add("25P02", "in_failed_sql_transaction");
		add("25P03", "idle_in_transaction_session_timeout");

		// Class 26 - Invalid SQL Statement Name
		add("26000", "invalid_sql_statement_name");

		// Class 27 - Triggered Data Change Violation
		add("27000", "triggered_data_change_violation");

		// Class 28 - Invalid Authorization Specification
		add("28000", "invalid_authorization_specification");
		add("28P01", "invalid_password");

		// Class 2B - Dependent Privilege Descriptors Still Exist
		add("2B000", "dependent_privilege_descriptors_still_exist");
		add("2BP01", "dependent_objects_still_exist");

		// Class 2D - Invalid Transaction Termination
		add("2D000", "invalid_transaction_termination");

		// Class 2F - SQL Routine Exception
		add("2F000", "sql_routine_exception");
		add("2F005", "function_executed_no_return_statement");
		add("2F002", "modifying_sql_data_not_permitted");
		add("2F003", "prohibited_sql_statement_attempted");
		add("2F004", "reading_sql_data_not_permitted");

		// Class 34 - Invalid Cursor Name
		add("34000", "invalid_cursor_name");

		// Class 38 - External Routine Exception
		add("38000", "external_routine_exception");
		add("38001", "containing_sql_not_permitted");
		add("38002", "modifying_sql_data_not_permitted");
		add("38003", "prohibited_sql_statement_attempted");
		add("38004", "reading_sql_data_not_permitted");

		// Class 39 - External Routine Invocation Exception
		add("39000", "external_routine_invocation_exception");
		add("39001", "invalid_sqlstate_returned");
		add("39004", "null_value_not_allowed");
		add("39P01", "trigger_protocol_violated");
		add("39P02", "srf_protocol_violated");
		add("39P03", "event_trigger_protocol_violated");

		// Class 3B - Savepoint Exception
		add("3B000", "savepoint_exception");
		add("3B001", "invalid_savepoint_specification");

		// Class 3D - Invalid Catalog Name
		add("3D000", "invalid_catalog_name");

		// Class 3F - Invalid Schema Name
		add("3F000", "invalid_schema_name");

		// Class 40 - Transaction Rollback
		add("40000", "transaction_rollback");
		add("40002", "transaction_integrity_constraint_violation");
		add("40001", "serialization_failure");
		add("40003", "statement_completion_unknown");
		add("40P01", "deadlock_detected");

		// Class 42 - Syntax Error or Access Rule Violation
		add("42000", "syntax_error_or_access_rule_violation");
		add("42601", "syntax_error");
		add("42501", "insufficient_privilege");
		add("42846", "cannot_coerce");
		add("42803", "grouping_error");
		add("42P20", "windowing_error");
		add("42P19", "invalid_recursion");
		add("42830", "invalid_foreign_key");
		add("42602", "invalid_name");
		add("42622", "name_too_long");
		add("42939", "reserved_name");
		add("42804", "datatype_mismatch");
		add("42P18", "indeterminate_datatype");
		add("42P21", "collation_mismatch");
		add("42P22", "indeterminate_collation");
		add("42809", "wrong_object_type");
		add("428C9", "generated_always");
		add("42703", "undefined_column");
		add("42883", "undefined_function");
		add("42P01", "undefined_table");
		add("42P02", "undefined_parameter");
		add("42704", "undefined_object");
		add("42701", "duplicate_column");
		add("42P03", "duplicate_cursor");
		add("42P04", "duplicate_database");
		add("42723", "duplicate_function");
		add("42P05", "duplicate_prepared_statement");
		add("42P06", "duplicate_schema");
		add("42P07", "duplicate_table");
		add("42712", "duplicate_alias");
		add("42710", "duplicate_object");
		add("42702", "ambiguous_column");
		add("42725", "ambiguous_function");
		add("42P08", "ambiguous_parameter");
		add("42P09", "ambiguous_alias");
		add("42P10", "invalid_column_reference");
		add("42611", "invalid_column_definition");
		add("42P11", "invalid_cursor_definition");
		add("42P12", "invalid_database_definition");
		add("42P13", "invalid_function_definition");
		add("42P14", "invalid_prepared_statement_definition");
		add("42P15", "invalid_schema_definition");
		add("42P16", "invalid_table_definition");
		add("42P17", "invalid_object_definition");

		// Class 44 - WITH CHECK OPTION Violation
		add("44000", "with_check_option_violation");

		// Class 53 - Insufficient Resources
		add("53000", "insufficient_resources");
		add("53100", "disk_full");
		add("53200", "out_of_memory");
		add("53300", "too_many_connections");
		add("53400", "configuration_limit_exceeded");

		// Class 54 - Program Limit Exceeded
		add("54000", "program_limit_exceeded");
		add("54001", "statement_too_complex");
		add("54011", "too_many_columns");
		add("54023", "too_many_arguments");

		// Class 55 - Object Not In Prerequisite State
		add("55000", "object_not_in_prerequisite_state");
		add("55006", "object_in_use");
		add("55P02", "cant_change_runtime_param");
		add("55P03", "lock_not_available");
		add("55P04", "unsafe_new_enum_value_usage");

		// Class 57 - Operator Intervention
		add("57000", "operator_intervention");
		add("57014", "query_canceled");
		add("57P01", "admin_shutdown");
		add("57P02", "crash_shutdown");
		add("57P03", "cannot_connect_now");
		add("57P04", "database_dropped");
		add("57P05", "idle_session_timeout");

		// Class 58 - System Error (errors external to PostgreSQL itself)
		add("58000", "system_error");
		add("58030", "io_error");
		add("58P01", "undefined_file");
		add("58P02", "duplicate_file");

		// Class 72 - Snapshot Failure
		add("72000", "snapshot_too_old");

		// Class F0 - Configuration File Error
		add("F0000", "config_file_error");
		add("F0001", "lock_file_exists");

		// Class HV - Foreign Data Wrapper Error (SQL/MED)
		add("HV000", "fdw_error");
		add("HV005", "fdw_column_name_not_found");
		add("HV002", "fdw_dynamic_parameter_value_needed");
		add("HV010", "fdw_function_sequence_error");
		add("HV021", "fdw_inconsistent_descriptor_information");
		add("HV024", "fdw_invalid_attribute_value");
		add("HV007", "fdw_invalid_column_name");
		add("HV008", "fdw_invalid_column_number");
		add("HV004", "fdw_invalid_data_type");
		add("HV006", "fdw_invalid_data_type_descriptors");
		add("HV091", "fdw_invalid_descriptor_field_identifier");
		add("HV00B", "fdw_invalid_handle");
		add("HV00C", "fdw_invalid_option_index");
		add("HV00D", "fdw_invalid_option_name");
		add("HV090", "fdw_invalid_string_length_or_buffer_length");
		add("HV00A", "fdw_invalid_string_format");
		add("HV009", "fdw_invalid_use_of_null_pointer");
		add("HV014", "fdw_too_many_handles");
		add("HV001", "fdw_out_of_memory");
		add("HV00P", "fdw_no_schemas");
		add("HV00J", "fdw_option_name_not_found");
		add("HV00K", "fdw_reply_handle");
		add("HV00Q", "fdw_schema_not_found");
		add("HV00R", "fdw_table_not_found");
		add("HV00L", "fdw_unable_to_create_execution");
		add("HV00M", "fdw_unable_to_create_reply");
		add("HV00N", "fdw_unable_to_establish_connection");

		// Class P0 - PL/pgSQL Error
		add("P0000", "plpgsql_error");
		add("P0001", "raise_exception");
		add("P0002", "no_data_found");
		add("P0003", "too_many_rows");
		add("P0004", "assert_failure");

		// Class XX - Internal Error
		add("XX000", "internal_error");
		add("XX001", "data_corrupted");
		add("XX002", "index_corrupted");
	}
	
	

//	public static String getWaitEventDescription(String name)
//	{
//		return getInstance().getDescriptionHtml(name);
//	}
//
//	public static String getWaitEventTypeDescription(String name)
//	{
//		if      (name.equals("Activity" )) return "<html><b>Activity </b> - The server process is idle. This event type indicates a process waiting for activity in its main processing loop. wait_event will identify the specific wait point</html>";
//		else if (name.equals("BufferPin")) return "<html><b>BufferPin</b> - The server process is waiting for exclusive access to a data buffer. Buffer pin waits can be protracted if another process holds an open cursor that last read data from the buffer in question</html>";
//		else if (name.equals("Client"   )) return "<html><b>Client   </b> - The server process is waiting for activity on a socket connected to a user application. Thus, the server expects something to happen that is independent of its internal processes. wait_event will identify the specific wait point</html>";
//		else if (name.equals("Extension")) return "<html><b>Extension</b> - The server process is waiting for some condition defined by an extension module.</html>";
//		else if (name.equals("IO"       )) return "<html><b>IO       </b> - The server process is waiting for an I/O operation to complete. wait_event will identify the specific wait point</html>";
//		else if (name.equals("IPC"      )) return "<html><b>IPC      </b> - The server process is waiting for some interaction with another server process. wait_event will identify the specific wait point</html>";
//		else if (name.equals("Lock"     )) return "<html><b>Lock     </b> - The server process is waiting for a heavyweight lock. Heavyweight locks, also known as lock manager locks or simply locks, primarily protect SQL-visible objects such as tables. However, they are also used to ensure mutual exclusion for certain internal operations such as relation extension. wait_event will identify the type of lock awaited</html>";
//		else if (name.equals("LWLock"   )) return "<html><b>LWLock   </b> - The server process is waiting for a lightweight lock. Most such locks protect a particular data structure in shared memory. wait_event will contain a name identifying the purpose of the lightweight lock. (Some locks have specific names; others are part of a group of locks each with a similar purpose.)</html>";
//		else if (name.equals("Timeout"  )) return "<html><b>Timeout  </b> - The server process is waiting for a timeout to expire. wait_event will identify the specific wait point</html>";
//
//		return null;
//	}

}
