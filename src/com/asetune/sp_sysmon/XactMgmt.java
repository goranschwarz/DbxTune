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
package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class XactMgmt
extends AbstractSysmonType
{
	public XactMgmt(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public XactMgmt(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Transaction Management";
	}

	@SuppressWarnings("unused")
	@Override
	public void calc()
	{
		String fieldName  = "";
		String groupName  = "";
		int    instanceid = -1;
		int    field_id   = -1;
		int    value      = 0;

		int fld_ULC_Flushes_to_Xact_Log___Total_ULC_Flushes                               = 0;
		int fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_End_Transaction      = 0;
		int fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Change_of_Database   = 0;
		int fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Unpin                = 0;
		int fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Log_markers          = 0;
		int fld_ULC_Flushes_to_Xact_Log___Fully_Logged_DMLs___by_Full_ULC                 = 0;
		int fld_ULC_Flushes_to_Xact_Log___Fully_Logged_DMLs___by_Single_Log_Record        = 0;
		int fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Full_ULC             = 0;
		int fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Single_Log_Record    = 0;
		int fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Start_of_Sub_Command = 0;
		int fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_End_of_Sub_Command   = 0;

		int fld_ULC_Flushes_Skipped___Total_ULC_Flushes_Skips                             = 0;
		int fld_ULC_Flushes_Skipped___Fully_Logged_DMLs___by_ULC_Discards                 = 0;
		int fld_ULC_Flushes_Skipped___Minimally_Logged_DMLs___by_ULC_Discards             = 0;

		int fld_ULC_Log_Records___Total_ULC_Log_Records                                   = 0;
		int fld_ULC_Log_Records___Fully_Logged_DMLs                                       = 0;
		int fld_ULC_Log_Records___Minimally_Logged_DMLs                                   = 0;

		int fld_Max_ULC_Size_During_Sample___Fully_Logged_DMLs                            = 0;
		int fld_Max_ULC_Size_During_Sample___Minimally_Logged_DMLs                        = 0;

		int fld_ML_DMLs_Sub_Command_Scans___Total_Sub_Command_Scans                       = 0;
		int fld_ML_DMLs_Sub_Command_Scans___ULC_Scans                                     = 0;
		int fld_ML_DMLs_Sub_Command_Scans___Syslogs_Scans                                 = 0;

		int fld_ML_DMLs_ULC_Efficiency___Total_ML_DML_Sub_Commands                        = 0;
		int fld_ML_DMLs_ULC_Efficiency___Discarded_Sub_Commands                           = 0;
		int fld_ML_DMLs_ULC_Efficiency___Logged_Sub_Commands                              = 0;

		int fld_ULC_Semaphore_Requests___Total_ULC_Semaphore_Req                          = 0;
		int fld_ULC_Semaphore_Requests___Granted                                          = 0;  // = "plc_lock_calls" - "plc_lock_waits"
		int fld_ULC_Semaphore_Requests___Waited                                           = 0;

		int fld_Log_Semaphore_Requests___Total_Log_Semaphore_Req                          = 0;
		int fld_Log_Semaphore_Requests___Granted                                          = 0;
		int fld_Log_Semaphore_Requests___Local_Waited                                     = 0;
		int fld_Log_Semaphore_Requests___Global_Waited                                    = 0;

		int fld_Transaction_Log_Writes                                                    = 0;
		int fld_Transaction_Log_Alloc                                                     = 0;
		int fld_Avg_Num_Writes_per_Log_Page                                               = 0;   // =  "log_page_writes" / "log_page_allocations" (if "log_page_allocations" > 0)


		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//-------------------------------------------------------------------
			// ULC Flushes to Xact Log
			//-------------------------------------------------------------------

			// @tmp_total -------------------------------------------------------
			//----- ULC Flushes to Xact Log - Total ULC Flushes
			if (    groupName.equals("xls") 
			    && (fieldName.startsWith("plc_flush_") || fieldName.startsWith("mldml_plc_flush_")) 
			    && ! fieldName.equals("plc_flush_discard") 
			    && ! fieldName.equals("mldml_plc_discard") 
			   )
				fld_ULC_Flushes_to_Xact_Log___Total_ULC_Flushes += value;

			//----- ULC Flushes to Xact Log - Any Logging Mode DMLs - by End Transaction
			if (groupName.equals("xls") && fieldName.equals("plc_flush_endxact"))
				fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_End_Transaction += value;

			//----- ULC Flushes to Xact Log - Any Logging Mode DMLs - by Change of Database
			if (groupName.equals("xls") && fieldName.equals("plc_flush_xdeschange"))
				fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Change_of_Database += value;

			//----- ULC Flushes to Xact Log - Any Logging Mode DMLs - by Unpin
			if (groupName.equals("xls") && fieldName.equals("plc_flush_unpin"))
				fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Unpin += value;

			//----- ULC Flushes to Xact Log - Any Logging Mode DMLs - by Log markers
			if (groupName.equals("xls") && fieldName.equals("plc_flush_pmscan"))
				fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Log_markers += value;

			//----- ULC Flushes to Xact Log - Fully Logged DMLs - by Full ULC
			if (groupName.equals("xls") && fieldName.equals("plc_flush_full"))
				fld_ULC_Flushes_to_Xact_Log___Fully_Logged_DMLs___by_Full_ULC += value;

			//----- ULC Flushes to Xact Log - Fully Logged DMLs - by Single Log Record
			if (groupName.equals("xls") && fieldName.equals("plc_flush_slr_xact"))
				fld_ULC_Flushes_to_Xact_Log___Fully_Logged_DMLs___by_Single_Log_Record += value;

			//----- ULC Flushes to Xact Log - Minimally Logged DMLs - by Full ULC
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_flush_full"))
				fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Full_ULC += value;

			//----- ULC Flushes to Xact Log - Minimally Logged DMLs - by Single Log Record
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_flush_slr_xact"))
				fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Single_Log_Record += value;

			//----- ULC Flushes to Xact Log - Minimally Logged DMLs - by Start of Sub-Command
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_flush_beginsubcmd"))
				fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Start_of_Sub_Command += value;

			//----- ULC Flushes to Xact Log - Minimally Logged DMLs - by End of Sub-Command
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_flush_endsubcmd"))
				fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_End_of_Sub_Command += value;

			
			// @tmp_total -------------------------------------------------------
			//----- ULC Flushes Skipped - Total ULC Flushes Skips
			if (groupName.equals("xls")  && (    fieldName.equals("plc_flush_discard") 
			                                  || fieldName.equals("mldml_plc_discard")) )
				fld_ULC_Flushes_Skipped___Total_ULC_Flushes_Skips += value;

			//----- ULC Flushes Skipped - Fully Logged DMLs - by ULC Discards
			if (groupName.equals("xls") && fieldName.equals("plc_flush_discard"))
				fld_ULC_Flushes_Skipped___Fully_Logged_DMLs___by_ULC_Discards += value;

			//----- ULC Flushes Skipped - Minimally Logged DMLs - by ULC Discards
			if (groupName.equals("xls") && fieldName.equals("mldml_subcmd_discard"))
				fld_ULC_Flushes_Skipped___Minimally_Logged_DMLs___by_ULC_Discards += value;

			
			// @tmp_total -------------------------------------------------------
			//----- ULC Log Records - Total ULC Log Records
			if (groupName.equals("xls")  && (    fieldName.equals("plc_logrecs") 
			                                  || fieldName.equals("mldml_plc_logrecs")) )
				fld_ULC_Log_Records___Total_ULC_Log_Records += value;

			//----- ULC Log Records - Fully Logged DMLs
			if (groupName.equals("xls") && fieldName.equals("plc_logrecs"))
				fld_ULC_Log_Records___Fully_Logged_DMLs += value;

			//----- ULC Log Records - Minimally Logged DMLs
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_logrecs"))
				fld_ULC_Log_Records___Minimally_Logged_DMLs += value;

			
			// -------------------------------------------------------
			//----- Max ULC Size During Sample - Fully Logged DMLs
			if (groupName.equals("xls") && fieldName.equals("plc_maxused"))
				fld_Max_ULC_Size_During_Sample___Fully_Logged_DMLs += value;

			//----- Max ULC Size During Sample - Minimally Logged DMLs
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_maxused"))
				fld_Max_ULC_Size_During_Sample___Minimally_Logged_DMLs += value;


			// @tmp_total -------------------------------------------------------
			//----- ML-DMLs Sub-Command Scans - Total Sub-Command Scans
			if (groupName.equals("xls")  && (    fieldName.equals("mldml_subcmd_plc_scan") 
			                                  || fieldName.equals("mldml_subcmd_syslogs_scan")) )
				fld_ML_DMLs_Sub_Command_Scans___Total_Sub_Command_Scans += value;

			//----- ML-DMLs Sub-Command Scans - ULC Scans
			if (groupName.equals("xls") && fieldName.equals("mldml_subcmd_plc_scan"))
				fld_ML_DMLs_Sub_Command_Scans___ULC_Scans += value;

			//----- ML-DMLs Sub-Command Scans - Syslogs Scans
			if (groupName.equals("xls") && fieldName.equals("mldml_subcmd_syslogs_scan"))
				fld_ML_DMLs_Sub_Command_Scans___Syslogs_Scans += value;


			
			// @tmp_total = @discarded_subcmd + @logged_subcmd -------------------------------------------------------
			//----- ML-DMLs ULC Efficiency - Total ML-DML Sub-Commands = @discarded_subcmd + @logged_subcmd
			if (groupName.equals("xls") && (    fieldName.equals("mldml_subcmd_discard") 
			                                 || fieldName.equals("mldml_plc_flush_endsubcmd")) )
				fld_ML_DMLs_ULC_Efficiency___Total_ML_DML_Sub_Commands += value;

			//----- ML-DMLs ULC Efficiency - Discarded Sub-Commands = @discarded_subcmd
			if (groupName.equals("xls") && fieldName.equals("mldml_subcmd_discard"))
				fld_ML_DMLs_ULC_Efficiency___Discarded_Sub_Commands += value;

			//----- ML-DMLs ULC Efficiency - Logged Sub-Commands = @logged_subcmd
			if (groupName.equals("xls") && fieldName.equals("mldml_plc_flush_endsubcmd"))
				fld_ML_DMLs_ULC_Efficiency___Logged_Sub_Commands += value;


			
			// @tmp_total -------------------------------------------------------
			//----- ULC Semaphore Requests - Total ULC Semaphore Req
			if (groupName.equals("xls") && fieldName.equals("plc_lock_calls"))
				fld_ULC_Semaphore_Requests___Total_ULC_Semaphore_Req += value;

			//----- ULC Semaphore Requests - Granted  = "plc_lock_calls" - "plc_lock_waits"
			//	fld_xxx += value;
			//doAfterLoop
			//fld_ULC_Semaphore_Requests___Granted = fld_ULC_Semaphore_Requests___Total_ULC_Semaphore_Req - fld_ULC_Semaphore_Requests___Waited;
 
			//----- ULC Semaphore Requests - Waited
			if (groupName.equals("xls") && fieldName.equals("plc_lock_waits"))
				fld_ULC_Semaphore_Requests___Waited += value;


			
			// @tmp_total -------------------------------------------------------
			//----- Log Semaphore Requests - Total Log Semaphore Req
			if (      groupName.equals("xls")  
			     && (    fieldName.equals("log_lock_granted") 
			          || fieldName.equals("log_lock_waited") 
			          || fieldName.equals("log_objectlock_needwait")
			        ) 
			   )
				fld_Log_Semaphore_Requests___Total_Log_Semaphore_Req += value;

			//----- Log Semaphore Requests - Granted
			if (groupName.equals("xls") && fieldName.equals("log_lock_granted"))
				fld_Log_Semaphore_Requests___Granted += value;

			//----- Log Semaphore Requests - Local Waited
			if (groupName.equals("xls") && fieldName.equals("log_lock_waited"))
				fld_Log_Semaphore_Requests___Local_Waited += value;

			//----- Log Semaphore Requests - Global Waited
			if (groupName.equals("xls") && fieldName.equals("log_objectlock_needwait"))
				fld_Log_Semaphore_Requests___Global_Waited += value;


			
			
//			//----- ???  - not used in the proc...
//			if (groupName.startsWith("buffer_") && fieldName.equals("last_log_page_writes"))
//				fld_ += value;

			//----- Transaction Log Writes
			if (groupName.startsWith("buffer_") && fieldName.equals("log_page_writes"))
				fld_Transaction_Log_Writes += value;

			//----- Transaction Log Alloc
			if (groupName.equals("access") && fieldName.equals("log_page_allocations"))
				fld_Transaction_Log_Alloc += value;

			//----- Avg # Writes per Log Page  =  "log_page_writes" / "log_page_allocations" (if "log_page_allocations" > 0)
			//	fld_xxx += value;
			//done in method: addReportLnScDiv()
			//fld_Avg_Num_Writes_per_Log_Page = fld_Transaction_Log_Writes / fld_Transaction_Log_Alloc;

		}
		
		// do some post prrocessing of some counters
		fld_ULC_Semaphore_Requests___Granted = fld_ULC_Semaphore_Requests___Total_ULC_Semaphore_Req - fld_ULC_Semaphore_Requests___Waited;
		

		// ADD info
		int divideBy = fld_ULC_Flushes_to_Xact_Log___Total_ULC_Flushes;
		addReportHead ("ULC Flushes to Xact Log");
		addReportLn   ("  Any Logging Mode DMLs");
		addReportLnPct("    by End Transaction",      fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_End_Transaction,    divideBy);
		addReportLnPct("    by Change of Database",   fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Change_of_Database, divideBy);
		addReportLnPct("    by Unpin",                fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Unpin,              divideBy);
		addReportLnPct("    by Log markers",          fld_ULC_Flushes_to_Xact_Log___Any_Logging_Mode_DMLs___by_Log_markers,        divideBy);
		addReportLn   ("  Fully Logged DMLs");
		addReportLnPct("    by Full ULC",             fld_ULC_Flushes_to_Xact_Log___Fully_Logged_DMLs___by_Full_ULC,               divideBy);
		addReportLnPct("    by Single Log Record",    fld_ULC_Flushes_to_Xact_Log___Fully_Logged_DMLs___by_Single_Log_Record,      divideBy);
		addReportLn   ("  Minimally Logged DMLs");
		addReportLnPct("    by Full ULC",             fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Full_ULC,             divideBy);
		addReportLnPct("    by Single Log Record",    fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Single_Log_Record,    divideBy);
		addReportLnPct("    by Start of Sub-Command", fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_Start_of_Sub_Command, divideBy);
		addReportLnPct("    by End of Sub-Command",   fld_ULC_Flushes_to_Xact_Log___Minimally_Logged_DMLs___by_End_of_Sub_Command,   divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total ULC Flushes",         divideBy);

		divideBy = fld_ULC_Flushes_Skipped___Total_ULC_Flushes_Skips;
		addReportLn();
		addReportHead ("ULC Flushes Skipped");
		addReportLn   ("  Fully Logged DMLs");
		addReportLnPct("    by ULC Discards",         fld_ULC_Flushes_Skipped___Fully_Logged_DMLs___by_ULC_Discards,     divideBy);
		addReportLn   ("  Minimally Logged DMLs");
		addReportLnPct("    by ULC Discards",         fld_ULC_Flushes_Skipped___Minimally_Logged_DMLs___by_ULC_Discards, divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("  Total ULC Flushes Skips",   divideBy);

		divideBy = fld_ULC_Log_Records___Total_ULC_Log_Records;
		addReportLn();
		addReportHead ("ULC Log Records");
		addReportLnPct("Fully Logged DMLs",           fld_ULC_Log_Records___Fully_Logged_DMLs,     divideBy);
		addReportLnPct("Minimally Logged DMLs",       fld_ULC_Log_Records___Minimally_Logged_DMLs, divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("Total ULC Log Records",       divideBy);

		// divideBy = xx; // not used in this sub-section
		addReportLn();
		addReportLn   ("Max ULC Size During Sample");
		addReportLn   ("--------------------------");
		addReportLnSC ("Fully Logged DMLs",           fld_Max_ULC_Size_During_Sample___Fully_Logged_DMLs);
		addReportLnSC ("Minimally Logged DMLs",       fld_Max_ULC_Size_During_Sample___Minimally_Logged_DMLs);

		divideBy = fld_ML_DMLs_Sub_Command_Scans___Total_Sub_Command_Scans;
		addReportLn();
		addReportHead ("ML-DMLs Sub-Command Scans");
		addReportLnPct("  ULC Scans",                 fld_ML_DMLs_Sub_Command_Scans___ULC_Scans,     divideBy);
		addReportLnPct("  Syslogs Scans",             fld_ML_DMLs_Sub_Command_Scans___Syslogs_Scans, divideBy);
		addReportLnCnt("Total Sub-Command Scans",     divideBy);

		divideBy = fld_ML_DMLs_ULC_Efficiency___Total_ML_DML_Sub_Commands;
		addReportLn();
		addReportHead ("ML-DMLs ULC Efficiency");
		addReportLnPct("  Discarded Sub-Commands",    fld_ML_DMLs_ULC_Efficiency___Discarded_Sub_Commands, divideBy);
		addReportLnPct("  Logged Sub-Commands",       fld_ML_DMLs_ULC_Efficiency___Logged_Sub_Commands,    divideBy);
		addReportLnCnt("Total ML-DML Sub-Commands",   divideBy);

		divideBy = fld_ULC_Semaphore_Requests___Total_ULC_Semaphore_Req;
		addReportLn();
		addReportLn   ("ULC Semaphore Requests");
		addReportLnPct("  Granted",                   fld_ULC_Semaphore_Requests___Granted, divideBy);
		addReportLnPct("  Waited",                    fld_ULC_Semaphore_Requests___Waited,  divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("Total ULC Semaphore Req",     divideBy);

		divideBy = fld_Log_Semaphore_Requests___Total_Log_Semaphore_Req;
		addReportLn();
		addReportLn   ("Log Semaphore Requests");
		addReportLnPct("  Granted",                   fld_Log_Semaphore_Requests___Granted,       divideBy);
		addReportLnPct("  Local Waited",              fld_Log_Semaphore_Requests___Local_Waited,  divideBy);
		addReportLnPct("  Global Waited",             fld_Log_Semaphore_Requests___Global_Waited, divideBy);
		addReportLnSum(); // -------------------------  ------------  ------------  ----------
		addReportLnCnt("Total Log Semaphore Req",     divideBy);

		addReportLn();
		addReportLnCnt("Transaction Log Writes",      fld_Transaction_Log_Writes);
		addReportLnCnt("Transaction Log Alloc",       fld_Transaction_Log_Alloc);
		addReportLnScD("Avg # Writes per Log Page",   fld_Transaction_Log_Writes, fld_Transaction_Log_Alloc, 5); // 5 = num of decimals


//		  Tuning Recommendations for Transaction Management
//		  -------------------------------------------------
//		  - Consider increasing the 'user log cache size'
//		    configuration parameter.
	}
}


//
// ASE 15.7.0: sp_sysmon output
//===============================================================================
//
//	Transaction Management
//	----------------------
//
//	  ULC Flushes to Xact Log         per sec      per xact       count  % of total
//	  -------------------------  ------------  ------------  ----------  ----------
//	  Any Logging Mode DMLs
//	    by End Transaction                0.5           1.8          14       9.8 %
//	    by Change of Database             0.0           0.1           1       0.7 %
//	    by Unpin                          0.0           0.0           0       0.0 %
//	    by Log markers                    0.1           0.3           2       1.4 %
//
//	  Fully Logged DMLs
//	    by Full ULC                       4.2          15.8         126      88.1 %
//	    by Single Log Record              0.0           0.0           0       0.0 %
//
//	  Minimally Logged DMLs
//	    by Full ULC                       0.0           0.0           0       0.0 %
//	    by Single Log Record              0.0           0.0           0       0.0 %
//	    by Start of Sub-Command           0.0           0.0           0       0.0 %
//	    by End of Sub-Command             0.0           0.0           0       0.0 %
//	  -------------------------  ------------  ------------  ----------
//	  Total ULC Flushes                   4.8          17.9         143
//
//	  ULC Flushes Skipped             per sec      per xact       count  % of total
//	  -------------------------  ------------  ------------  ----------  ----------
//	  Fully Logged DMLs
//	    by ULC Discards                   0.1           0.4           3     100.0 %
//	  Minimally Logged DMLs
//	    by ULC Discards                   0.0           0.0           0       0.0 %
//	  -------------------------  ------------  ------------  ----------
//	  Total ULC Flushes Skips             0.1           0.4           3
//
//	  ULC Log Records                 per sec      per xact       count  % of total
//	  -------------------------  ------------  ------------  ----------  ----------
//	  Fully Logged DMLs                 367.9        1379.6       11037       100.0
//	  Minimally Logged DMLs               0.0           0.0           0         0.0
//	  -------------------------  ------------  ------------  ----------
//	  Total ULC Log Records             367.9        1379.6       11037
//
//	  Max ULC Size During Sample
//	  --------------------------
//	  Fully Logged DMLs                   n/a           n/a           0       n/a
//	  Minimally Logged DMLs               n/a           n/a           0       n/a
//
//	  ML-DMLs Sub-Command Scans       per sec      per xact       count  % of total
//	  -------------------------  ------------  ------------  ----------  ----------
//	  Total Sub-Command Scans             0.0           0.0           0       n/a
//
//	  ML-DMLs ULC Efficiency          per sec      per xact       count  % of total
//	  -------------------------  ------------  ------------  ----------  ----------
//	  Total ML-DML Sub-Commands           0.0           0.0           0       n/a
//
//	  ULC Semaphore Requests
//	    Granted                         370.6        1389.6       11117     100.0 %
//	    Waited                            0.0           0.0           0       0.0 %
//	  -------------------------  ------------  ------------  ----------
//	  Total ULC Semaphore Req           370.6        1389.6       11117
//
//	  Log Semaphore Requests
//	    Granted                           9.0          33.9         271     100.0 %
//	    Local Waited                      0.0           0.0           0       0.0 %
//	    Global Waited                     0.0           0.0           0       0.0 %
//	  -------------------------  ------------  ------------  ----------
//	  Total Log Semaphore Req             9.0          33.9         271
//
//	  Transaction Log Writes              0.2           0.9           7       n/a
//	  Transaction Log Alloc               8.6          32.1         257       n/a
//	  Avg # Writes per Log Page           n/a           n/a     0.02724       n/a
//
//	  Tuning Recommendations for Transaction Management
//	  -------------------------------------------------
//	  - Consider increasing the 'user log cache size'
//	    configuration parameter.

