package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Locks extends AbstractSysmonType
{
	public Locks(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Locks(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Lock Management";
	}

	@Override
	public void calc()
	{
//		String fieldName  = "";
//		String groupName  = "";
//		int    instanceid = -1;
//		int    field_id   = -1;
//		int    value      = 0;
//
//		int fld_xxx = 0;
//		int fld_yyy = 0;
//
//		for (List<Object> row : getData())
//		{
//			if (_instanceid_pos > 0)
//				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
//			fieldName = (String)row.get(_fieldName_pos);
//			groupName = (String)row.get(_groupName_pos);
////			field_id  = ((Number)row.get(_field_id_pos)).intValue();
//			value     = ((Number)row.get(_value_pos)).intValue();
//
//			//----------------------------
//			// Memory
//			//----------------------------
//
//			// Pages Allocated
//			if (groupName.equals("group") && fieldName.equals("xxx"))
//				fld_xxx += value;
//
//			// Pages Released
//			if (groupName.equals("group") && fieldName.equals("yyy"))
//				fld_yyy += value;
//		}
//
//		addReportHead("Whatever Header");
//		addReportLnCnt("  Counter X",  fld_xxx);
//		addReportLnCnt("  Counter Y",  fld_yyy);
		
		addReportLnNotYetImplemented();
	}
}
