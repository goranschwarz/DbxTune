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

public class Hk extends AbstractSysmonType
{
	public Hk(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Hk(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Housekeeper Task Activity";
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

		int fld_TotalWashes         = 0;
		int fld_Clean               = 0;
		int fld_Dirty               = 0;
		int fld_GarbageCollections  = 0;
		int fld_PagesProcessedInGC  = 0;
		int fld_StatisticsUpdates   = 0;

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

			// Total Washes
			if (groupName.startsWith("buffer_") && fieldName.equals("hk_wash"))
				fld_TotalWashes += value;

			// Clean
			if (groupName.startsWith("buffer_") && fieldName.equals("hk_washclean"))
				fld_Clean += value;

			// Dirty  = "hk_wash" - "hk_washclean"
			//if (groupName.equals("xxx") && fieldName.equals("xxx"))
			//	fld_ += value;

			// Garbage Collections
			if (groupName.equals("housekeeper") && fieldName.equals("hk_gc_wakes"))
				fld_GarbageCollections += value;

			// Pages Processed in GC
			if (groupName.equals("housekeeper") && fieldName.equals("hk_gc_numgoodpages"))
				fld_PagesProcessedInGC += value;

			// Statistics Updates
			if (groupName.equals("housekeeper") && fieldName.equals("hk_stats_wakes"))
				fld_StatisticsUpdates += value;
		}
		
		// Post processing
		fld_Dirty = fld_TotalWashes - fld_Clean;

		
		addReportHead ("Buffer Cache Washes");
		addReportLnPct("  Clean",      fld_Clean, fld_TotalWashes );
		addReportLnPct("  Dirty",      fld_Dirty, fld_TotalWashes);
		addReportLnSum();
		addReportLnCnt("Total Washes", fld_TotalWashes);
		addReportLn();
		addReportHead ("");
		addReportLnCnt("Garbage Collections",     fld_GarbageCollections);
		addReportLnCnt("Pages Processed in GC",   fld_PagesProcessedInGC);
		addReportLn();
		addReportLnCnt("Statistics Updates",      fld_StatisticsUpdates);
	}
}
