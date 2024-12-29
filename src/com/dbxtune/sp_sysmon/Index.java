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

public class Index extends AbstractSysmonType
{
	public Index(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Index(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Index Management";
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

		int fld_NcIndexMaint_Ins_Upd_Requiring_Maint          = 0;
		int fld_NcIndexMaint_Ins_Upd_Num_of_NC_Ndx_Maint      = 0;

		int fld_NcIndexMaint_Deletes_Requiring_Maint          = 0;
		int fld_NcIndexMaint_Deletes_Num_of_NC_Ndx_Maint      = 0;

		int fld_NcIndexMaint_RID_Upd_from_Clust_Split         = 0;
		int fld_NcIndexMaint_RID_Upd_Num_of_NC_Ndx_Maint      = 0;

		int fld_NcIndexMaint_Upd_Del_DOL_Req_Maint            = 0;
		int fld_NcIndexMaint_Upd_Del_Num_of_DOL_Ndx_Maint     = 0;

		int fld_PageSplits                                    = 0;
		int fld_PageSplits_Retries                            = 0;
		int fld_PageSplits_Deadlocks                          = 0;
		int fld_PageSplits_Add_Index_Level                    = 0;

		int fld_PageShrinks                                   = 0;
		int fld_PageShrinks_Deadlocks                         = 0;
		int fld_PageShrinks_Deadlock_Retries_Exceeded         = 0;

		int fld_IndexScans_Ascending_Scans                    = 0;
		int fld_IndexScans_DOL_Ascending_Scans                = 0;
		int fld_IndexScans_Descending_Scans                   = 0;
		int fld_IndexScans_DOL_Descending_Scans               = 0;
		int fld_IndexScans_Total_Scans                        = 0;

		
		
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();

			//----------------------------
			// Memory
			//----------------------------

			//---------------------------------------------
			// Nonclustered Maintenance - Ins/Upd Requiring Maint
			if (groupName.equals("access") && fieldName.equals("ncupdate"))
				fld_NcIndexMaint_Ins_Upd_Requiring_Maint += value;

			// Nonclustered Maintenance - Ins/Upd Requiring Maint - # of NC Ndx Maint
			if (groupName.equals("access") && fieldName.equals("ncupdate_indexes"))
				fld_NcIndexMaint_Ins_Upd_Num_of_NC_Ndx_Maint += value;

			
			//---------------------------------------------
			// Nonclustered Maintenance - Deletes Requiring Maint
			if (groupName.equals("access") && fieldName.equals("ncdelete"))
				fld_NcIndexMaint_Deletes_Requiring_Maint += value;

			// Nonclustered Maintenance - Deletes Requiring Maint - # of NC Ndx Maint
			if (groupName.equals("access") && fieldName.equals("ncdelete_indexes"))
				fld_NcIndexMaint_Deletes_Num_of_NC_Ndx_Maint += value;

			
			//---------------------------------------------
			// Nonclustered Maintenance - RID Upd from Clust Split
			if (groupName.equals("access") && fieldName.equals("ncrid_update"))
				fld_NcIndexMaint_RID_Upd_from_Clust_Split += value;

			// Nonclustered Maintenance - RID Upd from Clust Split - # of NC Ndx Maint
			if (groupName.equals("access") && fieldName.equals("ncrid_update_indexes"))
				fld_NcIndexMaint_RID_Upd_Num_of_NC_Ndx_Maint += value;
			
			
			//---------------------------------------------
			// Nonclustered Maintenance - Upd/Del DOL Req Maint
			if (groupName.equals("access") && fieldName.equals("dolncdelete"))
				fld_NcIndexMaint_Upd_Del_DOL_Req_Maint += value;

			// Nonclustered Maintenance - Upd/Del DOL Req Maint - # of DOL Ndx Maint
			if (groupName.equals("access") && fieldName.equals("dolncdelete_indexes"))
				fld_NcIndexMaint_Upd_Del_Num_of_DOL_Ndx_Maint += value;

			
			//---------------------------------------------
			// Page Splits
			if (    ( groupName.equals("access") && ( fieldName.equals("split_index")        || fieldName.equals("split_root")) )
			     || ( groupName.equals("btree")  && ( fieldName.equals("bt_leafsplit_count") || fieldName.equals("bt_noleafsplit_count")) )
			   )
				fld_PageSplits += value;

			// Page Splits - Retries
			if (groupName.equals("access") && fieldName.equals("split_index_retry"))
				fld_PageSplits_Retries += value;

			// Page Splits - Deadlocks
			if (groupName.equals("access") && fieldName.equals("split_index_deadlock"))
				fld_PageSplits_Deadlocks += value;

			// Page Splits - Add Index Level
			if (    ( groupName.equals("access") && fieldName.equals("add_ind_level") )
				 || ( groupName.equals("btree")  && fieldName.equals("bt_add_ind_level") )
			   )
				fld_PageSplits_Add_Index_Level += value;


			//---------------------------------------------
			// Page Shrinks
			if (    ( groupName.equals("access") && fieldName.startsWith("shrink") )
				 || ( groupName.equals("btree")  && (   fieldName.equals("bt_shrink_bylastdel") 
			                                         || fieldName.equals("bt_shrink_byscan") 
			                                         || fieldName.equals("bt_shrink_nonleaf")
			                                        ) 
			        )
			   )
				fld_PageShrinks += value;

			// Page Shrinks - Deadlocks
			if (groupName.equals("access") && (    fieldName.equals("am_split_shrink_LOSTP") 
			                                    || fieldName.equals("am_split_shrink_WDP")
			                                    || fieldName.equals("am_split_shrink_NWFP")
			                                    || fieldName.equals("am_split_shrink_WDC")
			                                    || fieldName.equals("am_split_shrink_NWFC")
			                                    || fieldName.equals("am_split_shrink_WDNXT")
			                                    || fieldName.equals("am_split_shrink_NWFNX")
			                                    || fieldName.equals("am_split_shrink_WDPRV")
			                                    || fieldName.equals("am_split_shrink_NWFPRV")
			                                  )
			   )
				fld_PageShrinks_Deadlocks += value;

			// Page Shrinks - Deadlock Retries Exceeded
			if (groupName.equals("access") && fieldName.equals("split_shrink_retries_exceeded"))
				fld_PageShrinks_Deadlock_Retries_Exceeded += value;


			//---------------------------------------------
			// Index Scans
			if (    ( groupName.equals("access") && ( fieldName.equals("forward_scans")    || fieldName.equals("backward_scans")) )
				 || ( groupName.equals("btree")  && ( fieldName.equals("bt_forward_scans") || fieldName.equals("bt_backward_scans")) )
			   )
				fld_IndexScans_Ascending_Scans += value;

			// Index Scans - Ascending Scans
			if (groupName.equals("access") && fieldName.equals("forward_scans"))
				fld_IndexScans_DOL_Ascending_Scans += value;

			// Index Scans - DOL Ascending Scans
			if (groupName.equals("btree") && fieldName.equals("bt_forward_scans"))
				fld_IndexScans_Descending_Scans += value;

			// Index Scans - Descending Scans
			if (groupName.equals("access") && fieldName.equals("backward_scans"))
				fld_IndexScans_DOL_Descending_Scans += value;

			// Index Scans - DOL Descending Scans
			if (groupName.equals("btree") && fieldName.equals("bt_backward_scans"))
				fld_IndexScans_Total_Scans += value;
		}
		

		addReportHead2("  Nonclustered Maintenance");
		addReportLnCnt("    Ins/Upd Requiring Maint",   fld_NcIndexMaint_Ins_Upd_Requiring_Maint);
		addReportLnCnt("      # of NC Ndx Maint",       fld_NcIndexMaint_Ins_Upd_Num_of_NC_Ndx_Maint);
		addReportLnScD("      Avg NC Ndx Maint / Op",   fld_NcIndexMaint_Ins_Upd_Num_of_NC_Ndx_Maint, fld_NcIndexMaint_Ins_Upd_Requiring_Maint, 5);
		addReportLn();
		addReportLnCnt("    Deletes Requiring Maint",   fld_NcIndexMaint_Deletes_Requiring_Maint);
		addReportLnCnt("      # of NC Ndx Maint",       fld_NcIndexMaint_Deletes_Num_of_NC_Ndx_Maint);
		addReportLnScD("      Avg NC Ndx Maint / Op",   fld_NcIndexMaint_Deletes_Num_of_NC_Ndx_Maint, fld_NcIndexMaint_Deletes_Requiring_Maint, 5);
		addReportLn();
		addReportLnCnt("    RID Upd from Clust Split ", fld_NcIndexMaint_RID_Upd_from_Clust_Split);
		addReportLnCnt("      # of NC Ndx Maint",       fld_NcIndexMaint_RID_Upd_Num_of_NC_Ndx_Maint);
		addReportLnScD("      Avg NC Ndx Maint / Op",   fld_NcIndexMaint_RID_Upd_Num_of_NC_Ndx_Maint, fld_NcIndexMaint_RID_Upd_from_Clust_Split, 5);
		addReportLn();
		addReportLnCnt("    Upd/Del DOL Req Maint",     fld_NcIndexMaint_Upd_Del_DOL_Req_Maint);
		addReportLnCnt("      # of DOL Ndx Maint",      fld_NcIndexMaint_Upd_Del_Num_of_DOL_Ndx_Maint);
		addReportLnScD("      Avg DOL Ndx Maint / Op",  fld_NcIndexMaint_Upd_Del_Num_of_DOL_Ndx_Maint, fld_NcIndexMaint_Upd_Del_DOL_Req_Maint, 5);
		addReportLn();
		addReportLnCnt("  Page Splits",                 fld_PageSplits);
		addReportLnCnt("    Retries",                   fld_PageSplits_Retries);
		addReportLnCnt("    Deadlocks",                 fld_PageSplits_Deadlocks);
		addReportLnCnt("    Add Index Level",           fld_PageSplits_Add_Index_Level);
		addReportLn();
		addReportLnCnt("  Page Shrinks",                fld_PageShrinks);
		addReportLnCnt("    Deadlocks",                 fld_PageShrinks_Deadlocks);
		addReportLnCnt("    Deadlock Retries Exceeded", fld_PageShrinks_Deadlock_Retries_Exceeded);
		addReportLn();
		addReportHead2("  Index Scans");
		addReportLnPct("    Ascending Scans",           fld_IndexScans_Ascending_Scans,      fld_IndexScans_Total_Scans);
		addReportLnPct("    DOL Ascending Scans",       fld_IndexScans_DOL_Ascending_Scans,  fld_IndexScans_Total_Scans);
		addReportLnPct("    Descending Scans",          fld_IndexScans_Descending_Scans,     fld_IndexScans_Total_Scans);
		addReportLnPct("    DOL Descending Scans",      fld_IndexScans_DOL_Descending_Scans, fld_IndexScans_Total_Scans);
		addReportLnSum2();
		addReportLnCnt("    Total Scans",               fld_IndexScans_Total_Scans);
	}
}
