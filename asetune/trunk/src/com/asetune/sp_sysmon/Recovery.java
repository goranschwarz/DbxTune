/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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

public class Recovery extends AbstractSysmonType
{
	public Recovery(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Recovery(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Recovery Management";
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

		int fld_NumOfNormalCheckpoints = 0;
		int fld_NumOfFreeCheckpoints   = 0;
		int fld_Total                  = 0;
		int fld_AvgTimePerNormalChkpt  = 0;
		int fld_AvgTimePerFreeChkpt    = 0;
		
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName = (String)row.get(_fieldName_pos);
			groupName = (String)row.get(_groupName_pos);
//			field_id  = ((Number)row.get(_field_id_pos)).intValue();
			value     = ((Number)row.get(_value_pos)).intValue();


			// Total Checkpoints
			if (groupName.equals("access") && (fieldName.equals("normal_database_checkpoints") || fieldName.equals("free_database_checkpoints")) )
				fld_Total += value;

			// # of Normal Checkpoints
			if (groupName.equals("access") && fieldName.equals("normal_database_checkpoints") )
				fld_NumOfNormalCheckpoints += value;

			// # of Free Checkpoints
			if (groupName.equals("housekeeper") && fieldName.equals("free_database_checkpoints") )
				fld_NumOfFreeCheckpoints += value;

			
			
			// Avg Time per Normal Chkpt
			if (groupName.equals("access") && fieldName.equals("time_todo_normal_checkpoints") )
				fld_AvgTimePerNormalChkpt += value;

			// Avg Time per Free Chkpt
			if (groupName.equals("housekeeper") && fieldName.equals("time_todo_free_checkpoints") )
				fld_AvgTimePerFreeChkpt += value;

		}

		addReportHead2("  Checkpoints");
		addReportLnPct("    # of Normal Checkpoints",  fld_NumOfNormalCheckpoints, fld_Total);
		addReportLnPct("    # of Free Checkpoints",    fld_NumOfFreeCheckpoints,   fld_Total);
		addReportLnSum2();
		addReportLnCnt("  Total Checkpoints",          fld_Total);
		addReportLn();
		addReportLnSec("  Avg Time per Normal Chkpt",  fld_AvgTimePerNormalChkpt, fld_NumOfNormalCheckpoints, 5);
		addReportLnSec("  Avg Time per Free Chkpt",    fld_AvgTimePerFreeChkpt,   fld_NumOfFreeCheckpoints,   5);
		
	}
}
