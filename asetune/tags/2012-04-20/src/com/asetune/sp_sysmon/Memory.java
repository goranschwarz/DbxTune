package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Memory 
extends AbstractSysmonType
{
	public Memory(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Memory(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Memory";
	}

	@Override
	public void calc()
	{
		String fieldName  = "";
		String groupName  = "";
		int    instanceid = -1;
		int    field_id   = -1;
		int    value      = 0;

		int fld_PagesAllocated = 0;
		int fld_PagesReleased  = 0;

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

			// Pages Allocated
			if (groupName.equals("memory") && fieldName.equals("mempages_alloced"))
				fld_PagesAllocated += value; // NOT SUM, but += anyway

			// Pages Released
			if (groupName.equals("memory") && fieldName.equals("mempages_freed"))
				fld_PagesReleased += value; // NOT SUM, but += anyway
		}

		addReportHead("Memory Management");
		addReportLnCnt("  Pages Allocated",  fld_PagesAllocated);
		addReportLnCnt("  Pages Released",   fld_PagesReleased);
	}
}
