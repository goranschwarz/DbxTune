package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class TaskMgmt
extends AbstractSysmonType
{
	public TaskMgmt(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public TaskMgmt(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Task Management";
	}

	@SuppressWarnings("unused")
	@Override
	public void calc()
	{
		/*
		* Context Switch Due to Task Yields (Voluntary)
		* Context Switch Due to Cache Search Misses resulting in a read
		* Context Switch due to exceedint the 'i/o batch size' config limit.
			In other words, we started I/O batch and now we are waiting for them to complete before starting the next batch.  
			The server works in batches to avoid flooding the I/O subsystem.  
			The size of the batch is tuneable via config parameter 'io batch size'.
		* Context Switch Due to Disk Writes 
		* Context Switch Due to DB Lock Contention
		* Context Switch Due to Address Lock Contention 
		* Context Switch Due to Latch Contention 
		* Context Switch Due to Physical lock transition.
		* Context Switch Due to Logical Lock Transition.
		* Context Switch Due to Object Lock Transition.
		* Context Switch Due to Blocking on Log Semaphore 
		* Context Switch Due to Blocking on PLC lock
		* Context Switch Due to Group Commit Sleeps 
		* Context Switch Due to Last Log Page Writes 
		* Context Switch Due to Modify Conflicts 
			In other words, a task wants to perform an operation on a page  (i.e. write it), 
			but can't because another task is in the middle of modifying it.
		* Context Switch Due to Disk Device Contention 
		* Context Switch Due to Network Packets Received
		* Context Switch Due to Network Packets Sent
		* Context Switch Due to CIPC Thread Sleeps.
		* Context Switch Due to Network services
		* Context Switch Due to Other Causes
		*/
		

		String fieldName  = "";
		String groupName  = "";
		int    instanceid = -1;
		int    field_id   = -1;
		int    value      = 0;

		int NumEngines       = _sysmon.getNumEngines();
		int NumXacts         = _sysmon.getNumXacts();

		int NumTaskSwitch    = 0; // Total Number of Task Context Switches across all engines
		int IgnoreTaskSwitch = 0; // Total Number of Task Context Switches which can be ignored
		int KnownTaskSwitch  = 0; // Count of Number of Task Context Switches by Known Causes
		int IgnoreTaskYields = 0; // Total Number of Task Yields which can be ignored

//		int tmpNumEngines = 0;

		// Get some basics
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			// tmpNumEngines, NumEngines
//			if (groupName.equals("config") && fieldName.equals("cg_cmaxonline"))
//				tmpNumEngines += value;
//			if (groupName.startsWith("engine_") && fieldName.equals("clock_ticks") && value > 0)
//				NumEngines++;

			// NumXacts
			if (groupName.equals("access") && fieldName.equals("xacts"))
				NumXacts += value;
			
			// NumTaskSwitch
			if (groupName.startsWith("engine_") && fieldName.equals("context_switches"))
				NumTaskSwitch += value;
			
			// IgnoreTaskSwitch
			if (groupName.equals("lock") && (fieldName.equals("lock_gc_yields") || fieldName.equals("daemon_context_switches")))
				IgnoreTaskSwitch += value;
			if (groupName.equals("bcmt") && (fieldName.equals("bcmt_pri_sleeps") || fieldName.equals("bcmt_sec_sleeps")))
				IgnoreTaskSwitch += value;

			// IgnoreTaskYields
			if (groupName.equals("bcmt") && fieldName.equals("bcmt_pri_sleeps"))
				IgnoreTaskYields += value;						
		}
		NumTaskSwitch = NumTaskSwitch - IgnoreTaskSwitch - IgnoreTaskYields;

		// 
		int fld_ConnectionsOpened         = 0;
		
		int[] fld_EngineCtxSwitchArray = new int[NumEngines];
		int fld_VoluntaryYields           = 0;
		int fld_CacheSearchMisses         = 0;
		int fld_ExceedingIoBatchSize      = 0;
		int fld_SystemDiskWrites          = 0;
		int fld_LogicalLockContention     = 0;
		int fld_AddressLockContention     = 0;
		int fld_LatchContention           = 0;
		int fld_PhysicalLockTransition    = 0;
		int fld_LogicalLockTransition     = 0;
		int fld_ObjectLockTransition      = 0;
		int fld_LogSemaphoreContention    = 0;
		int fld_PlcLockContention         = 0;
		int fld_GroupCommitSleeps         = 0;
		int fld_LastLogPageWrites         = 0;
		int fld_ModifyConflicts           = 0;
		int fld_IoDeviceContention        = 0;
		int fld_NetworkPacketReceived     = 0;
		int fld_NetworkPacketSent         = 0;
		int fld_InterconnectMessageSleeps = 0;
		int fld_NetworkServices           = 0;
		int fld_OtherCauses               = 0;

		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();


			// Connections Opened
			if (groupName.equals("kernel") && fieldName.equals("processes_created"))
				fld_ConnectionsOpened += value; // NOT SUM, but += anyway

			
			// Task Context Switches by Engine
			if (groupName.startsWith("engine_") && fieldName.equals("context_switches"))
			{
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < fld_EngineCtxSwitchArray.length)
					fld_EngineCtxSwitchArray[engineNum] += value;
			}

			// Context Switch Due to Task Yields (Voluntary)
			if (groupName.equals("kernel") && fieldName.equals("yields"))
				fld_VoluntaryYields += value; // NOT SUM, but += anyway

			// Context Switch Due to Cache Search Misses resulting in a read
			if (groupName.startsWith("buffer_") && fieldName.equals("bufread_read_waits"))
				fld_CacheSearchMisses += value; // SUM

			// Context Switch due to exceedint the 'i/o batch size' config limit.
			//     In other words, we started I/O batch and now we are waiting
			//     for them to complete before starting the next batch.  The server 
			//     works in batches to avoid flooding the I/O subsystem.  The size 
			//     of the batch is tuneable via config parameter 'io batch size'.
			if (groupName.startsWith("buffer_") && fieldName.equals("my_start_waits_periobatch"))
				fld_ExceedingIoBatchSize += value; // SUM

			// Context Switch Due to Disk Writes 
			if (groupName.equals("lock") 
					&& (    fieldName.equals("write_waits")
						 || fieldName.equals("hk_write_waits")
						 || fieldName.equals("restart_io_waits")
						 || fieldName.equals("my_start_waits_log")
						 || fieldName.equals("my_start_waits_non-log")
						 || fieldName.equals("my_other_waits_non-log")
					))
				fld_SystemDiskWrites += value; // SUM

			//  Context Switch Due to DB Lock Contention
			if (groupName.equals("lock") && fieldName.startsWith("waited_") && ! fieldName.endsWith("_ADDR") )
				fld_LogicalLockContention += value; // SUM

			// Context Switch Due to Address Lock Contention
			if (groupName.equals("lock") && fieldName.startsWith("waited_") && fieldName.endsWith("_ADDR") )
				fld_AddressLockContention += value; // SUM

			// Context Switch Due to Latch Contention 
			if (groupName.equals("latch") && fieldName.startsWith("waited_") && fieldName.endsWith("_LATCH") )
				fld_LatchContention += value; // SUM

			// Context Switch Due to Physical lock transition. 
			if (groupName.equals("lock") && fieldName.equals("physical_lock_context_switches") )
				fld_PhysicalLockTransition += value; // SUM

			// Context Switch Due to Logical Lock Transition. 
			if (groupName.equals("lock") && fieldName.equals("logical_lock_context_switches") )
				fld_LogicalLockTransition += value; // SUM

			// Context Switch Due to Object Lock Transition 
			if (groupName.equals("lock") && fieldName.equals("ocm_context_switches") )
				fld_ObjectLockTransition += value; // SUM

			// Context Switch Due to Blocking on Log Semaphore
			if (groupName.equals("xls") && fieldName.equals("log_lock_waited") )
				fld_LogSemaphoreContention += value; // NOT SUM, but += anyway

			// Context Switch Due to Blocking on PLC lock
			if (groupName.equals("xls") && fieldName.equals("plc_lock_waits") )
				fld_PlcLockContention += value; // NOT SUM, but += anyway

			// Context Switch Due to Group Commit Sleeps 
			if (groupName.startsWith("buffer_") && (fieldName.equals("my_other_waits_log") || fieldName.equals("log_lastpage_pending_io_sleeps")) )
				fld_GroupCommitSleeps += value; // SUM

			// Context Switch Due to Last Log Page Writes 
			if (groupName.startsWith("buffer_") && fieldName.equals("last_log_page_writes") )
				fld_LastLogPageWrites += value; // SUM

			// Context Switch Due to Modify Conflicts 
			//     In other words, a task wants to perform an operation on a page 
			//     (i.e. write it), but can't because another task is in the middle 
			//     of modifying it.
			if (groupName.startsWith("buffer_") 
					&& (    fieldName.equals("changing_state_waits")
						 || fieldName.equals("bufwrite_changing_waits")
						 || fieldName.equals("bufpredirty_write_waits")
						 || fieldName.equals("bufpredirty_changing_waits")
						 || fieldName.equals("bufnewpage_changing_waits")
						 || fieldName.equals("ind_bufguess_changing_waits")
						 || fieldName.equals("ind_bufguess_writing_waits")
					))
				fld_ModifyConflicts += value; // SUM

			// Context Switch Due to Disk Device Contention  
			if (groupName.startsWith("disk_") && fieldName.equals("p_sleeps") )
				fld_IoDeviceContention += value; // SUM

			// Context Switch Due to Network Packets Received
			if (groupName.equals("network") && fieldName.equals("network_read_sleeps") )
				fld_NetworkPacketReceived += value; // NOT SUM, but += anyway

			// Context Switch Due to Network Packets Sent
			if (groupName.equals("network") && fieldName.equals("network_send_sleeps") )
				fld_NetworkPacketSent += value; // SUM

			// Context Switch Due to CIPC Thread Sleeps.
			if (groupName.equals("kernel") && fieldName.equals("cipc_context_switches") )
				fld_InterconnectMessageSleeps += value; // SUM

			// Context Switch Due to Network services  
			if (groupName.startsWith("network") && fieldName.equals("nserver_sleeps") )
				fld_NetworkServices += value; // SUM
		}
		KnownTaskSwitch = 0
			+ fld_VoluntaryYields
			+ fld_CacheSearchMisses
			+ fld_ExceedingIoBatchSize
			+ fld_SystemDiskWrites
			+ fld_LogicalLockContention
			+ fld_AddressLockContention
			+ fld_LatchContention
			+ fld_PhysicalLockTransition
			+ fld_LogicalLockTransition
			+ fld_ObjectLockTransition
			+ fld_LogSemaphoreContention
			+ fld_PlcLockContention
			+ fld_GroupCommitSleeps
			+ fld_LastLogPageWrites
			+ fld_ModifyConflicts
			+ fld_IoDeviceContention
			+ fld_NetworkPacketReceived
			+ fld_NetworkPacketSent
			+ fld_InterconnectMessageSleeps
			+ fld_NetworkServices
			+ fld_OtherCauses;

		// Context Switch Due to Other Causes
		fld_OtherCauses = NumTaskSwitch - KnownTaskSwitch;

//		System.out.println();
//		System.out.println("tmpNumEngines    = "+tmpNumEngines);
//		System.out.println("NumEngines       = "+_NumEngines);
//		System.out.println("NumTaskSwitch    = "+NumTaskSwitch);
//		System.out.println("IgnoreTaskSwitch = "+IgnoreTaskSwitch);
//		System.out.println("KnownTaskSwitch  = "+KnownTaskSwitch);
//		System.out.println("IgnoreTaskYields = "+IgnoreTaskYields);
//		System.out.println();
//		System.out.println("NumXacts         = "+_NumXacts);
//		System.out.println("NumElapsedMs     = "+_NumElapsedMs);
//		System.out.println();


		addReportHead("Task Management");

		addReportLnCnt("  Connections Opened", fld_ConnectionsOpened);
		addReportLn("");

		addReportLn("  Task Context Switches by Engine");
		int fld_EngineCtxSwitchAllEngines = 0;
		for (int i=0; i<fld_EngineCtxSwitchArray.length; i++)
		{
			fld_EngineCtxSwitchAllEngines += fld_EngineCtxSwitchArray[i];
			addReportLnPct("    Engine "+i, fld_EngineCtxSwitchArray[i], NumTaskSwitch);
		}
		if (fld_EngineCtxSwitchArray.length > 1)
		{
			addReportLnSum();
			addReportLnCnt("    Total Task Context Switches", fld_EngineCtxSwitchAllEngines);
		}
		addReportLn("");

		addReportLn("  Task Context Switches Due To:");
		addReportLnPct("    Voluntary Yields",            fld_VoluntaryYields          , NumTaskSwitch);
		addReportLnPct("    Cache Search Misses",         fld_CacheSearchMisses        , NumTaskSwitch);
		addReportLnPct("    Exceeding I/O batch size",    fld_ExceedingIoBatchSize     , NumTaskSwitch);
		addReportLnPct("    System Disk Writes",          fld_SystemDiskWrites         , NumTaskSwitch);
		addReportLnPct("    Logical Lock Contention",     fld_LogicalLockContention    , NumTaskSwitch);
		addReportLnPct("    Address Lock Contention",     fld_AddressLockContention    , NumTaskSwitch);
		addReportLnPct("    Latch Contention",            fld_LatchContention          , NumTaskSwitch);
		addReportLnPct("    Physical Lock Transition",    fld_PhysicalLockTransition   , NumTaskSwitch);
		addReportLnPct("    Logical Lock Transition",     fld_LogicalLockTransition    , NumTaskSwitch);
		addReportLnPct("    Object Lock Transition",      fld_ObjectLockTransition     , NumTaskSwitch);
		addReportLnPct("    Log Semaphore Contention",    fld_LogSemaphoreContention   , NumTaskSwitch);
		addReportLnPct("    PLC Lock Contention",         fld_PlcLockContention        , NumTaskSwitch);
		addReportLnPct("    Group Commit Sleeps",         fld_GroupCommitSleeps        , NumTaskSwitch);
		addReportLnPct("    Last Log Page Writes",        fld_LastLogPageWrites        , NumTaskSwitch);
		addReportLnPct("    Modify Conflicts",            fld_LastLogPageWrites        , NumTaskSwitch);
		addReportLnPct("    I/O Device Contention",       fld_IoDeviceContention       , NumTaskSwitch);
		addReportLnPct("    Network Packet Received",     fld_NetworkPacketReceived    , NumTaskSwitch);
		addReportLnPct("    Network Packet Sent",         fld_NetworkPacketSent        , NumTaskSwitch);
		addReportLnPct("    Interconnect Message Sleeps", fld_InterconnectMessageSleeps, NumTaskSwitch);
		addReportLnPct("    Network services",            fld_NetworkServices          , NumTaskSwitch);
		addReportLnPct("    Other Causes",                fld_OtherCauses              , NumTaskSwitch);
	}
}
