package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Kernel
extends AbstractSysmonType
{

	public Kernel(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Kernel(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Kernel";
	}

	@Override
	public void calc()
	{
	}
}
