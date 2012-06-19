package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Recovery extends AbstractSysmonType
{
	public Recovery(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Recovery(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Recovery Management";
	}

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

		addReportHead ("  Checkpoints");
		addReportLnPct("    # of Normal Checkpoints",  fld_NumOfNormalCheckpoints, fld_Total);
		addReportLnPct("    # of Free Checkpoints",    fld_NumOfFreeCheckpoints,   fld_Total);
		addReportLnSum();
		addReportLnCnt("  Total Checkpoints",          fld_Total);
		addReportLn();
		addReportLnSec("  Avg Time per Normal Chkpt",  fld_AvgTimePerNormalChkpt, fld_NumOfNormalCheckpoints, 5);
		addReportLnSec("  Avg Time per Free Chkpt",    fld_AvgTimePerFreeChkpt,   fld_NumOfFreeCheckpoints,   5);
		
	}
}
