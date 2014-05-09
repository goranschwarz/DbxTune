package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class ProcCache
extends AbstractSysmonType
{
	public ProcCache(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public ProcCache(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Procedure Cache";
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

		int fld_ProcedureRequests                             = 0;
		int fld_ProcedureReadsFromDisk                        = 0;
		int fld_ProcedureWritesToDisk                         = 0;
		int fld_ProcedureRemovals                             = 0;
		int fld_ProcedureRecompilations                       = 0;

		int fld_RecompilationsRequests_ExecutionPhase         = 0;
		int fld_RecompilationsRequests_CompilationPhase       = 0;
		int fld_RecompilationsRequests_ExecuteCursorExecution = 0;
		int fld_RecompilationsRequests_RedefinitionPhase      = 0;

		int fld_RecompilationReasons_TableMissing             = 0;
		int fld_RecompilationReasons_TemporaryTableMissing    = 0;
		int fld_RecompilationReasons_SchemaChange             = 0;
		int fld_RecompilationReasons_IndexChange              = 0;
		int fld_RecompilationReasons_IsolationLevelChange     = 0;
		int fld_RecompilationReasons_PermissionsChange        = 0;
		int fld_RecompilationReasons_CursorPermissionsChange  = 0;

		int fld_SqlStatementCache_StatementsCached            = 0;
		int fld_SqlStatementCache_StatementsFoundInCache      = 0;
		int fld_SqlStatementCache_StatementsNotFound          = 0;
		int fld_SqlStatementCache_StatementsDropped           = 0;
		int fld_SqlStatementCache_StatementsRestored          = 0;
		int fld_SqlStatementCache_StatementsNotCached         = 0;

		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//----------------------------
			// Procedure Cache Management
			//----------------------------

			// Procedure Requests
			if (groupName.equals("procmgr") && fieldName.equals("procedure_requests"))
				fld_ProcedureRequests += value; // NOT SUM, but += anyway

			// Procedure Reads from Disk
			if (groupName.equals("procmgr") && fieldName.equals("procedure_reads"))
				fld_ProcedureReadsFromDisk += value; // NOT SUM, but += anyway

			// Procedure Writes to Disk
			if (groupName.equals("procmgr") && fieldName.equals("procedure_writes"))
				fld_ProcedureWritesToDisk += value; // NOT SUM, but += anyway

			// Procedure Removals
			if (groupName.equals("procmgr") && fieldName.equals("procedure_removes"))
				fld_ProcedureRemovals += value; // NOT SUM, but += anyway

			// Procedure Recompilations
			if (groupName.equals("procmgr") && (   fieldName.equals("proc_recompilation_exec")
			                                    || fieldName.equals("proc_recompilation_comp") 
			                                    || fieldName.equals("proc_recompilation_exec_curs") 
			                                    || fieldName.equals("proc_recompilation_redef")) )
				fld_ProcedureRecompilations += value; // SUM

						
			//----------------------------
			// Recompilations Requests
			//----------------------------
			// Recompilations Requests: Execution Phase
			if (groupName.equals("procmgr") && fieldName.equals("proc_recompilation_exec"))
				fld_RecompilationsRequests_ExecutionPhase += value; // NOT SUM, but += anyway

			// Recompilations Requests: Compilation Phase
			if (groupName.equals("procmgr") && fieldName.equals("proc_recompilation_comp"))
				fld_RecompilationsRequests_CompilationPhase += value; // NOT SUM, but += anyway

			// Recompilations Requests: Execute Cursor Execution
			if (groupName.equals("procmgr") && fieldName.equals("proc_recompilation_exec_curs"))
				fld_RecompilationsRequests_ExecuteCursorExecution += value; // NOT SUM, but += anyway

			// Recompilations Requests: Redefinition Phase
			if (groupName.equals("procmgr") && fieldName.equals("proc_recompilation_redef"))
				fld_RecompilationsRequests_RedefinitionPhase += value; // NOT SUM, but += anyway


			//----------------------------
			// Recompilation Reasons
			//----------------------------
			// Recompilation Reasons: Table Missing
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_tabmissing"))
				fld_RecompilationReasons_TableMissing += value; // NOT SUM, but += anyway

			// Recompilation Reasons: Temporary Table Missing
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_tempmissing"))
				fld_RecompilationReasons_TemporaryTableMissing += value; // NOT SUM, but += anyway

			// Recompilation Reasons: Schema Change
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_schemacount"))
				fld_RecompilationReasons_SchemaChange += value; // NOT SUM, but += anyway

			// Recompilation Reasons: Index Change
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_idxchange"))
				fld_RecompilationReasons_IndexChange += value; // NOT SUM, but += anyway

			// Recompilation Reasons: Isolation Level Change
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_isolevel"))
				fld_RecompilationReasons_IsolationLevelChange += value; // NOT SUM, but += anyway

			// Recompilation Reasons: Permissions Change
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_permissions"))
				fld_RecompilationReasons_PermissionsChange += value; // NOT SUM, but += anyway

			// Recompilation Reasons: Cursor Permissions Change
			if (groupName.equals("procmgr") && fieldName.equals("proc_recomp_cursor_perm"))
				fld_RecompilationReasons_CursorPermissionsChange += value; // NOT SUM, but += anyway


			//----------------------------
			// SQL Statement Cache:
			//----------------------------
			// SQL Statement Cache: Statements Cached
			if (groupName.equals("procmgr") && fieldName.equals("proc_ssql_procs_available"))
				fld_SqlStatementCache_StatementsCached += value; // NOT SUM, but += anyway

			// SQL Statement Cache: Statements Found in Cache
			if (groupName.equals("procmgr") && fieldName.equals("proc_ssql_found"))
				fld_SqlStatementCache_StatementsFoundInCache += value; // NOT SUM, but += anyway

			// SQL Statement Cache: Statements Not Found
			if (groupName.equals("procmgr") && fieldName.equals("proc_ssql_not_found"))
				fld_SqlStatementCache_StatementsNotFound += value; // NOT SUM, but += anyway

			// SQL Statement Cache: Statements Dropped
			if (groupName.equals("procmgr") && fieldName.equals("proc_ssql_dropped"))
				fld_SqlStatementCache_StatementsDropped += value; // NOT SUM, but += anyway

			// SQL Statement Cache: Statements Restored
			if (groupName.equals("procmgr") && fieldName.equals("proc_ssql_restored"))
				fld_SqlStatementCache_StatementsRestored += value; // NOT SUM, but += anyway

			// SQL Statement Cache: Statements Not Cached
			if (groupName.equals("procmgr") && fieldName.equals("proc_ssql_notcached"))
				fld_SqlStatementCache_StatementsNotCached += value; // NOT SUM, but += anyway
		}

		addReportHead("Procedure Cache Management");
		addReportLnCnt("  Procedure Requests",        fld_ProcedureRequests);
		addReportLnPct("  Procedure Reads from Disk", fld_ProcedureReadsFromDisk, fld_ProcedureRequests);
		addReportLnPct("  Procedure Writes to Disk",  fld_ProcedureWritesToDisk,  fld_ProcedureRequests);
		addReportLnCnt("  Procedure Removals",        fld_ProcedureRemovals);
		addReportLnCnt("  Procedure Recompilations",  fld_ProcedureRecompilations);

		addReportLn   ("  Recompilations Requests:");
		addReportLnCnt("    Execution Phase",          fld_RecompilationsRequests_ExecutionPhase);
		addReportLnCnt("    Compilation Phase",        fld_RecompilationsRequests_CompilationPhase);
		addReportLnCnt("    Execute Cursor Execution", fld_RecompilationsRequests_ExecuteCursorExecution);
		addReportLnCnt("    Redefinition Phase",       fld_RecompilationsRequests_RedefinitionPhase);

		addReportLn   ("  Recompilation Reasons:");
		addReportLnCnt("    Table Missing",            fld_RecompilationReasons_TableMissing);
		addReportLnCnt("    Temporary Table Missing",  fld_RecompilationReasons_TemporaryTableMissing);
		addReportLnCnt("    Schema Change",            fld_RecompilationReasons_SchemaChange);
		addReportLnCnt("    Index Change",             fld_RecompilationReasons_IndexChange);
		addReportLnCnt("    Isolation Level Change",   fld_RecompilationReasons_IsolationLevelChange);
		addReportLnCnt("    Permissions Change",       fld_RecompilationReasons_PermissionsChange);
		addReportLnCnt("    Cursor Permissions Change",fld_RecompilationReasons_CursorPermissionsChange);

		addReportLn   ("  SQL Statement Cache:");
		addReportLnCnt("    Statements Cached",        fld_SqlStatementCache_StatementsCached);
		addReportLnCnt("    Statements Found in Cache",fld_SqlStatementCache_StatementsFoundInCache);
		addReportLnCnt("    Statements Not Found",     fld_SqlStatementCache_StatementsNotFound);
		addReportLnCnt("    Statements Dropped",       fld_SqlStatementCache_StatementsDropped);
		addReportLnCnt("    Statements Restored",      fld_SqlStatementCache_StatementsRestored);
		addReportLnCnt("    Statements Not Cached",    fld_SqlStatementCache_StatementsNotCached);
	}
//	print "Procedure Cache Management        per sec      per xact       count  %% of total"
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "procedure_requests"
//	select @rptline = "  Procedure Requests" + space(9) +
//
//		select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "procedure_reads"
//		select @rptline = "  Procedure Reads from Disk" + space(2) +
//
//		select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "procedure_writes"
//		select @rptline = "  Procedure Writes to Disk" + space(3) +
//
//		select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "procedure_removes"
//		select @rptline = "  Procedure Removals" + space(9) +
//
//		select @tmp_reco_total = sum(value) from #tempmonitors where group_name = "procmgr" and field_name in ("proc_recompilation_exec", "proc_recompilation_comp", "proc_recompilation_exec_curs", "proc_recompilation_redef")
//		select @rptline = "  Procedure Recompilations" + space(3) +
//
//
//			print "  Recompilations Requests:"
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recompilation_exec"
//			select @rptline = "    Execution Phase" + space(10) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recompilation_comp"
//			select @rptline = "    Compilation Phase" + space(8) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recompilation_exec_curs"
//			select @rptline = "    Execute Cursor Execution" + space(1) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recompilation_redef"
//			select @rptline = "    Redefinition Phase" + space(7) +
//
//			print "  Recompilation Reasons:"
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_tabmissing"
//			select @rptline = "    Table Missing" + space(12) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_tempmissing"
//			select @rptline = "    Temporary Table Missing" + space(2) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_schemacount"
//			select @rptline = "    Schema Change" + space(12) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_idxchange"
//			select @rptline = "    Index Change" + space(13) +
//
//			select @tmp_int = value  from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_isolevel"
//			select @rptline = "    Isolation Level Change" + space(3) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_permissions"
//			select @rptline = "    Permissions Change" + space(7) +
//
//			select @tmp_int = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_recomp_cursor_perm"
//			select @rptline = "    Cursor Permissions Change" +
//
//	print "  SQL Statement Cache:"
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_ssql_procs_available"
//	select @rptline = "    Statements Cached" + space(8) +
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_ssql_found"
//	select @rptline = "    Statements Found in Cache" +
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_ssql_not_found"
//	select @rptline = "    Statements Not Found" + space(5) +
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_ssql_dropped"
//	select @rptline = "    Statements Dropped" + space(7) +
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_ssql_restored"
//	select @rptline = "    Statements Restored" + space(6) +
//
//	select @tmp_total = value from #tempmonitors where group_name = "procmgr" and field_name = "proc_ssql_notcached"
//	select @rptline = "    Statements Not Cached" + space(4) +

}
