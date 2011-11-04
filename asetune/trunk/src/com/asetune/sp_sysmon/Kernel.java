package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Kernel
extends AbstractSysmonType
{

	public Kernel(CountersModel cm)
	{
		super(cm);
	}

	public Kernel(int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportHead()
	{
		return "======================================================================\n" +
		       " Kernel \n" +
		       "----------------------------------------------------------------------\n";
	}

	@Override
	public void calc()
	{
	}
}
