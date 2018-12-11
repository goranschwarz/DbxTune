package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class BasicCalc extends AbstractSysmonType
{
	public BasicCalc(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public BasicCalc(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Basic";
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

		int tmpNumEngines = 0;
		int NumEngines    = 0;
		int NumXacts      = 0;


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
			if (groupName.equals("config") && fieldName.equals("cg_cmaxonline"))
				tmpNumEngines += value;
			if (groupName.startsWith("engine_") && fieldName.equals("clock_ticks") && value > 0)
				NumEngines++;

			// NumXacts
			if (groupName.equals("access") && fieldName.equals("xacts"))
				NumXacts += value;
		}


		// Set the basics in the top object
		_sysmon.setNumXacts(NumXacts);
		_sysmon.setNumEngines(NumEngines);
		
		// no report is made on this
//		addReportHead("Memory Management");
//		addReportLnCnt("  Pages Allocated",  fld_PagesAllocated);
//		addReportLnCnt("  Pages Released",   fld_PagesReleased);
	}
}
