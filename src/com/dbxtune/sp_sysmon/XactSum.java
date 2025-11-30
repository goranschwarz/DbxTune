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
package com.dbxtune.sp_sysmon;

import java.util.List;

import com.dbxtune.cm.CountersModel;

public class XactSum
extends AbstractSysmonType
{
	public XactSum(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public XactSum(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Transaction Profile";
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

		int fld_CommittedXacts             = _sysmon.getNumXacts();
		
		int fld_NumXactOps                   = 0;

		// gorans calc
//		int fld_acess__xacts                       = 0;
//		int fld_acess__non_tempdb_internal_xacts   = 0;
//		int fld_acess__tempdb_internal_xacts       = 0;
//
//		int fld_xact__tm_xact_pcommit_opti_scans   = 0;
//		int fld_xact__tm_xact_rollback_locals      = 0;
//		int fld_xact__tm_xact_prepare_readonly     = 0;
//		int fld_xact__tm_xact_prepare_requests     = 0;
//		int fld_xact__tm_xact_commit_externals     = 0;
//		int fld_xact__tm_xact_commit_internals     = 0;
//		int fld_xact__tm_xact_commit_locals        = 0;
//		int fld_xact__tm_xact_subs                 = 0;
//		int fld_xact__tm_xact_begin_internals      = 0;
//		int fld_xact__tm_xact_begin_externals      = 0;
//		int fld_xact__tm_xact_begin_locals         = 0;
//		int fld_xact__tmpplc_flush_endxact         = 0;

		
		// ins
		int fld_TotalRowsInserted            = 0;
		int fld_InsFl_APLHeapTable           = 0;
		int fld_InsFl_APLClusteredTable      = 0;
		int fld_InsFl_DataOnlyLockTable      = 0;
		int fld_InsFl_FastBulkInsert         = 0;
		int fld_InsMl_APLHeapTable           = 0;
		int fld_InsMl_APLClusteredTable      = 0;
		int fld_InsMl_DataOnlyLockTable      = 0;

		// upd
		int fld_TotalRowsUpdated             = 0;
		int fld_UpdFl_APLDeferred            = 0;
		int fld_UpdFl_APLDirectInplace       = 0;
		int fld_UpdFl_APLDirectCheap         = 0;
		int fld_UpdFl_APLDirectExpensive     = 0;
		int fld_UpdFl_DOLDeferred            = 0;
		int fld_UpdFl_DOLDirect              = 0;
		int fld_UpdMl_APLDirectInplace       = 0;
		int fld_UpdMl_APLDirectCheap         = 0;
		int fld_UpdMl_APLDirectExpensive     = 0;
		int fld_UpdMl_DOLDirect              = 0;

		// upd dol
		int fld_TotalDOLRowsUpdated          = 0;
		int fld_DolUpdFl_DOLReplace          = 0;
		int fld_DolUpdFl_DOLShrink           = 0;
		int fld_DolUpdFl_DOLCheapExpand      = 0;
		int fld_DolUpdFl_DOLExpensiveExpand  = 0;
		int fld_DolUpdFl_DOLExpandAndForward = 0;
		int fld_DolUpdFl_DOLFwdRowReturned   = 0;
		int fld_DolUpdMl_DOLReplace          = 0;
		int fld_DolUpdMl_DOLShrink           = 0;
		int fld_DolUpdMl_DOLCheapExpand      = 0;
		int fld_DolUpdMl_DOLExpensiveExpand  = 0;
		int fld_DolUpdMl_DOLExpandAndForward = 0;
		int fld_DolUpdMl_DOLFwdRowReturned   = 0;

		// del
		int fld_TotalRowsDeleted             = 0;
		int fld_DelFl_APLDeferred            = 0;
		int fld_DelFl_APLDirect              = 0;
		int fld_DelFl_DOL                    = 0;
		int fld_DelMl_APLDirect              = 0;
		int fld_DelMl_DOL                    = 0;

		int fld_TotalRowsAffected            = 0;

		
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//----------------------------
			// Transaction Profile
			//----------------------------

			// xxx
			if (    groupName.equals("access") 
					&& (    fieldName.equals("ncinsert")
						 || fieldName.equals("cinsert")
						 || fieldName.equals("deferred_update")
						 || fieldName.equals("direct_inplace_update")
						 || fieldName.equals("direct_notinplace_update")
						 || fieldName.equals("direct_expensive_update")
						 || fieldName.equals("delete")
						 || fieldName.equals("bulk_fast_insert")
						 || fieldName.equals("mldml_ncinsert")
						 || fieldName.equals("mldml_cinsert")
						 || fieldName.equals("mldml_direct_inplace_update")
						 || fieldName.equals("mldml_direct_notinplace_update")
						 || fieldName.equals("mldml_direct_expensive_update")
						 || fieldName.equals("mldml_delete")
					   )
				 || groupName.equals("dolaccess")
				    && (    fieldName.equals("dolinsert")
						 || fieldName.equals("dolupdates")
						 || fieldName.equals("doldelete_total")
						 || fieldName.equals("mldml_dolinsert")
						 || fieldName.equals("mldml_dolupdates")
						 || fieldName.equals("mldml_doldelete_total")
					   )
				)
				fld_NumXactOps += value;

			//--------------------------------------------
			// INSERT
			//--------------------------------------------
			// INS Total
			if (    groupName.equals("access") 
					&& (    fieldName.equals("ncinsert")
						 || fieldName.equals("cinsert")
						 || fieldName.equals("bulk_fast_insert")
						 || fieldName.equals("mldml_ncinsert")
						 || fieldName.equals("mldml_cinsert")
					   )
				 || groupName.equals("dolaccess")
				    && (    fieldName.equals("dolinsert")
						 || fieldName.equals("mldml_dolinsert")
					   )
				)
				fld_TotalRowsInserted += value;

			// Fully Logged: APL Heap Table
			if (groupName.equals("access") && fieldName.equals("ncinsert") )
				fld_InsFl_APLHeapTable += value;

			// Fully Logged: APL Clustered Table
			if (groupName.equals("access") && fieldName.equals("cinsert") )
				fld_InsFl_APLClusteredTable += value;

			// Fully Logged: Data Only Lock Table
			if (groupName.equals("dolaccess") && fieldName.equals("dolinsert") )
				fld_InsFl_DataOnlyLockTable += value;

			// Fully Logged: Fast Bulk Insert
			if (groupName.equals("access") && fieldName.equals("bulk_fast_insert") )
				fld_InsFl_FastBulkInsert += value;

			// Minimally Logged: APL Heap Table
			if (groupName.equals("access") && fieldName.equals("mldml_ncinsert") )
				fld_InsMl_APLHeapTable += value;

			// Minimally Logged: APL Clustered Table
			if (groupName.equals("access") && fieldName.equals("mldml_cinsert") )
				fld_InsMl_APLClusteredTable += value;

			// Minimally Logged: Data Only Lock Table
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_dolinsert") )
				fld_InsMl_DataOnlyLockTable += value;


			//--------------------------------------------
			// UPDATE
			//--------------------------------------------
			// UPD Total
			if (    groupName.equals("access") 
					&& (    fieldName.equals("deferred_update")
						 || fieldName.equals("direct_inplace_update")
						 || fieldName.equals("direct_notinplace_update")
						 || fieldName.equals("direct_expensive_update")
						 || fieldName.equals("mldml_deferred_update")
						 || fieldName.equals("mldml_direct_inplace_update")
						 || fieldName.equals("mldml_direct_notinplace_update")
						 || fieldName.equals("mldml_direct_expensive_update")
					   )
				 || groupName.equals("dolaccess")
				    && (    fieldName.equals("dolupdates")
						 || fieldName.equals("mldml_dolupdates")
					   )
				)
				fld_TotalRowsUpdated += value;
			
			// Fully Logged: APL Deferred
			if (groupName.equals("access") && fieldName.equals("deferred_update") )
				fld_UpdFl_APLDeferred += value;

			// Fully Logged: APL Direct In-place
			if (groupName.equals("access") && fieldName.equals("direct_inplace_update") )
				fld_UpdFl_APLDirectInplace += value;

			// Fully Logged: APL Direct Cheap
			if (groupName.equals("access") && fieldName.equals("direct_notinplace_update") )
				fld_UpdFl_APLDirectCheap += value;

			// Fully Logged: APL Direct Expensive
			if (groupName.equals("access") && fieldName.equals("direct_expensive_update") )
				fld_UpdFl_APLDirectExpensive += value;

			// Fully Logged: DOL Deferred
			if (groupName.equals("dolaccess") && fieldName.equals("dolupdate_deferred") )
				fld_UpdFl_DOLDeferred += value;

			// Fully Logged: DOL Direct
			if (groupName.equals("dolaccess") && fieldName.equals("dolupdates") )
				fld_UpdFl_DOLDirect += value;

			// Minimally Logged: APL Direct In-place
			if (groupName.equals("access") && fieldName.equals("mldml_direct_inplace_update") )
				fld_UpdMl_APLDirectInplace += value;

			// Minimally Logged: APL Direct Cheap
			if (groupName.equals("access") && fieldName.equals("mldml_direct_notinplace_update") )
				fld_UpdMl_APLDirectCheap += value;

			// Minimally Logged: APL Direct Expensive
			if (groupName.equals("access") && fieldName.equals("mldml_direct_expensive_update") )
				fld_UpdMl_APLDirectExpensive += value;

			// Minimally Logged: DOL Direct
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_dolupdates") )
				fld_UpdMl_DOLDirect += value;


			//--------------------------------------------
			// Data Only Locked Updates
			//--------------------------------------------

			// TOTAL
			if (groupName.equals("dolaccess") 
					&& (    fieldName.equals("dolupdate_replace")
					     || fieldName.equals("mldml_dolupdate_shrink")
					     || fieldName.equals("dolupdate_expand_incfs")
					     || fieldName.equals("mldml_dolupdate_expand_shift")
					     || fieldName.equals("dolupdate_expand_after_gc")
					     || fieldName.equals("mldml_dolupdate_forward_firstlvl")
					     || fieldName.equals("dolupdate_forward_secondlvl")
					     || fieldName.equals("mldml_dolupdate_migrate_rowhome")
					     || fieldName.equals("mldml_dolupdate_replace")
					     || fieldName.equals("mldml_dolupdate_shrink")
					     || fieldName.equals("mldml_dolupdate_expand_incfs")
					     || fieldName.equals("mldml_dolupdate_expand_shift")
					     || fieldName.equals("mldml_dolupdate_expand_after_gc")
					     || fieldName.equals("mldml_dolupdate_forward_firstlvl")
					     || fieldName.equals("mldml_dolupdate_forward_secondlvl")
					     || fieldName.equals("mldml_dolupdate_migrate_rowhome")
					))
				fld_TotalDOLRowsUpdated += value;

			// Fully Logged: DOL Replace
			if (groupName.equals("dolaccess") && fieldName.equals("dolupdate_replace") )
				fld_DolUpdFl_DOLReplace += value;

			// Fully Logged: DOL Shrink
			if (groupName.equals("dolaccess") && fieldName.equals("dolupdate_shrink") )
				fld_DolUpdFl_DOLShrink += value;

			// Fully Logged: DOL Cheap Expand
			if (groupName.equals("dolaccess") && fieldName.equals("dolupdate_expand_incfs") )
				fld_DolUpdFl_DOLCheapExpand += value;

			// Fully Logged: DOL Expensive Expand
			if (groupName.equals("dolaccess") && (fieldName.equals("dolupdate_expand_shift") || fieldName.equals("dolupdate_expand_after_gc")) )
				fld_DolUpdFl_DOLExpensiveExpand += value;

			// Fully Logged: DOL Expand & Forward
			if (groupName.equals("dolaccess") && (fieldName.equals("dolupdate_forward_firstlvl") || fieldName.equals("dolupdate_forward_secondlvl")) )
				fld_DolUpdFl_DOLExpandAndForward += value;

			// Fully Logged: DOL Fwd Row Returned
			if (groupName.equals("dolaccess") && fieldName.equals("dolupdate_migrate_rowhome") )
				fld_DolUpdFl_DOLFwdRowReturned += value;

			// Minimally Logged: DOL Replace
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_dolupdate_replace") )
				fld_DolUpdMl_DOLReplace += value;

			// Minimally Logged: DOL Shrink
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_dolupdate_shrink") )
				fld_DolUpdMl_DOLShrink += value;

			// Minimally Logged: DOL Cheap Expand
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_dolupdate_expand_incfs") )
				fld_DolUpdMl_DOLCheapExpand += value;

			// Minimally Logged: DOL Expensive Expand
			if (groupName.equals("dolaccess") && (fieldName.equals("dmldml_olupdate_expand_shift") || fieldName.equals("mldml_dolupdate_expand_after_gc")) )
				fld_DolUpdMl_DOLExpensiveExpand += value;

			// Minimally Logged: DOL Expand & Forward
			if (groupName.equals("dolaccess") && (fieldName.equals("mldml_dolupdate_forward_firstlvl") || fieldName.equals("mldml_dolupdate_forward_secondlvl")) )
				fld_DolUpdMl_DOLExpandAndForward += value;

			// Minimally Logged: DOL Fwd Row Returned
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_dolupdate_migrate_rowhome") )
				fld_DolUpdMl_DOLFwdRowReturned += value;


			//--------------------------------------------
			// DELETES
			//--------------------------------------------

			// TOTAL
			if (    groupName.equals("access") 
					&& (    fieldName.equals("delete")
						 || fieldName.equals("mldml_delete")
					   )
				 || groupName.equals("dolaccess")
				    && (    fieldName.equals("doldelete_total")
						 || fieldName.equals("mldml_doldelete_total")
					   )
				)
				fld_TotalRowsDeleted += value;

			// Fully Logged: APL Deferred
			if (groupName.equals("access") && fieldName.equals("delete_deferred") )
				fld_DelFl_APLDeferred += value;

			// Fully Logged: APL Direct
			if (groupName.equals("access") && fieldName.equals("delete") )
				fld_DelFl_APLDirect += value;

			// Fully Logged: DOL
			if (groupName.equals("dolaccess") && fieldName.equals("doldelete_total") )
				fld_DelFl_DOL += value;

			// Minimally Logged: APL Direct
			if (groupName.equals("access") && fieldName.equals("mldml_delete") )
				fld_DelMl_APLDirect += value;

			// Minimally Logged: DOL
			if (groupName.equals("dolaccess") && fieldName.equals("mldml_doldelete_total") )
				fld_DelMl_DOL += value;

		}
		fld_TotalRowsAffected = fld_TotalRowsInserted
		                      + fld_TotalRowsUpdated
		                      + fld_TotalDOLRowsUpdated
		                      + fld_TotalRowsDeleted;

		addReportHead("Transaction Summary");
		addReportLnCnt("  Committed Xacts",          fld_CommittedXacts);
		addReportLn   ("");
		addReportHead("Transaction Detail");
		addReportLn   ("  Inserts");
		addReportLn   ("    Fully Logged");
		addReportLnPct("      APL Heap Table",       fld_InsFl_APLHeapTable     , fld_TotalRowsInserted);
		addReportLnPct("      APL Clustered Table",  fld_InsFl_APLClusteredTable, fld_TotalRowsInserted);
		addReportLnPct("      Data Only Lock Table", fld_InsFl_DataOnlyLockTable, fld_TotalRowsInserted);
		addReportLnPct("      Fast Bulk Insert",     fld_InsFl_FastBulkInsert   , fld_TotalRowsInserted);
		addReportLn   ("    Minimally Logged");
		addReportLnPct("      APL Heap Table",       fld_InsMl_APLHeapTable     , fld_TotalRowsInserted);
		addReportLnPct("      APL Clustered Table",  fld_InsMl_APLClusteredTable, fld_TotalRowsInserted);
		addReportLnPct("      Data Only Lock Table", fld_InsMl_DataOnlyLockTable, fld_TotalRowsInserted);
		addReportLnPct("  Total Rows Inserted",      fld_TotalRowsInserted      , fld_TotalRowsAffected);
		addReportLnPct(" -Total Rows Inserted",      fld_TotalRowsInserted      , fld_NumXactOps);
		addReportLn   ("");
		addReportLn   ("  Updates");
		addReportLn   ("    Fully Logged");
		addReportLnPct("      APL Deferred",         fld_UpdFl_APLDeferred       , fld_TotalRowsUpdated);
		addReportLnPct("      APL Direct In-place",  fld_UpdFl_APLDirectInplace  , fld_TotalRowsUpdated);
		addReportLnPct("      APL Direct Cheap",     fld_UpdFl_APLDirectCheap    , fld_TotalRowsUpdated);
		addReportLnPct("      APL Direct Expensive", fld_UpdFl_APLDirectExpensive, fld_TotalRowsUpdated);
		addReportLnPct("      DOL Deferred",         fld_UpdFl_DOLDeferred       , fld_TotalRowsUpdated);
		addReportLnPct("      DOL Direct",           fld_UpdFl_DOLDirect         , fld_TotalRowsUpdated);
		addReportLn   ("    Minimally Logged");
		addReportLnPct("      APL Direct In-place",  fld_UpdMl_APLDirectInplace  , fld_TotalRowsUpdated);
		addReportLnPct("      APL Direct Cheap",     fld_UpdMl_APLDirectCheap    , fld_TotalRowsUpdated);
		addReportLnPct("      APL Direct Expensive", fld_UpdMl_APLDirectExpensive, fld_TotalRowsUpdated);
		addReportLnPct("      DOL Direct",           fld_UpdMl_DOLDirect         , fld_TotalRowsUpdated);
		addReportLnPct("  Total Rows Updated",       fld_TotalRowsUpdated        , fld_TotalRowsAffected);
		addReportLnPct(" -Total Rows Updated",       fld_TotalRowsUpdated        , fld_NumXactOps);
		addReportLn   ("");
		addReportLn   ("  Data Only Locked Updates");
		addReportLn   ("    Fully Logged");
		addReportLnPct("      DOL Replace",          fld_DolUpdFl_DOLReplace         , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Shrink",           fld_DolUpdFl_DOLShrink          , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Cheap Expand",     fld_DolUpdFl_DOLCheapExpand     , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Expensive Expand", fld_DolUpdFl_DOLExpensiveExpand , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Expand & Forward", fld_DolUpdFl_DOLExpandAndForward, fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Fwd Row Returned", fld_DolUpdFl_DOLFwdRowReturned  , fld_TotalDOLRowsUpdated);
		addReportLn   ("    Minimally Logged");
		addReportLnPct("      DOL Replace",          fld_DolUpdMl_DOLReplace         , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Shrink",           fld_DolUpdMl_DOLShrink          , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Cheap Expand",     fld_DolUpdMl_DOLCheapExpand     , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Expensive Expand", fld_DolUpdMl_DOLExpensiveExpand , fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Expand & Forward", fld_DolUpdMl_DOLExpandAndForward, fld_TotalDOLRowsUpdated);
		addReportLnPct("      DOL Fwd Row Returned", fld_DolUpdMl_DOLFwdRowReturned  , fld_TotalDOLRowsUpdated);
		addReportLnPct("  Total DOL Rows Updated",   fld_TotalDOLRowsUpdated         , fld_TotalRowsAffected);
		addReportLnPct(" -Total DOL Rows Updated",   fld_TotalDOLRowsUpdated         , fld_NumXactOps);
		addReportLn   ("");
		addReportLn   ("  Deletes");
		addReportLn   ("    Fully Logged");
		addReportLnPct("      APL Deferred",         fld_DelFl_APLDeferred           , fld_TotalRowsDeleted);
		addReportLnPct("      APL Direct",           fld_DelFl_APLDirect             , fld_TotalRowsDeleted);
		addReportLnPct("      DOL",                  fld_DelFl_DOL                   , fld_TotalRowsDeleted);
		addReportLn   ("    Minimally Logged");
		addReportLnPct("      APL Direct",           fld_DelMl_APLDirect             , fld_TotalRowsDeleted);
		addReportLnPct("      DOL",                  fld_DelMl_DOL                   , fld_TotalRowsDeleted);
		addReportLnPct("  Total Rows Deleted",       fld_TotalRowsDeleted            , fld_TotalRowsAffected);
		addReportLnPct(" -Total Rows Deleted",       fld_TotalRowsDeleted            , fld_NumXactOps);
		addReportLn   ("");
		addReportLnCnt("  Total Rows Affected",      fld_TotalRowsAffected);
		addReportLnCnt(" -Total Rows Affected",      fld_NumXactOps);
	}
}
