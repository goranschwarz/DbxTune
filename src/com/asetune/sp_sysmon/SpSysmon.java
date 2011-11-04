package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class SpSysmon
{
	private AbstractSysmonType kernel;
//	private AbstractSysmonType wpm;
//	private AbstractSysmonType parallel;
	private AbstractSysmonType taskmgmt;
//	private AbstractSysmonType appmgmt;
//	private AbstractSysmonType esp;
//	private AbstractSysmonType hk;
//	private AbstractSysmonType maccess;
//	private AbstractSysmonType xactsum;
//	private AbstractSysmonType xactmgmt;
//	private AbstractSysmonType index;
//	private AbstractSysmonType mdcache;
//	private AbstractSysmonType locks;
//	private AbstractSysmonType dcache;
	private AbstractSysmonType pcache;
//	private AbstractSysmonType memory;
//	private AbstractSysmonType recovery;
//	private AbstractSysmonType diskio;
//	private AbstractSysmonType netio;
//	private AbstractSysmonType repagent;
	
	public SpSysmon(CountersModel cm)
	{
		kernel   = new Kernel   (cm);
//		wpm      = new Wpm      (cm);
//		parallel = new Parallel (cm);
		taskmgmt = new TaskMgmt (cm);
//		appmgmt  = new AppMgmt  (cm);
//		esp      = new Esp      (cm);
//		hk       = new Hk       (cm);
//		maccess  = new Maccess  (cm);
//		xactsum  = new XactSum  (cm);
//		xactmgmt = new XactMgmt (cm);
//		index    = new Index    (cm);
//		mdcache  = new MdCache  (cm);
//		locks    = new Locks    (cm);
//		dcache   = new DataCache(cm);
		pcache   = new ProcCache(cm);
//		memory   = new Memory   (cm);
//		recovery = new Recovery (cm);
//		diskio   = new DiskIo   (cm);
//		netio    = new NetIo    (cm);
//		repagent = new RepAgent (cm);
	}

	public SpSysmon(int aseVersion, int sampleTimeInMs, List<List<Object>> data, 
			int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		kernel   = new Kernel   (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		wpm      = new Wpm      (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		parallel = new Parallel (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		taskmgmt = new TaskMgmt (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		appmgmt  = new AppMgmt  (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		esp      = new Esp      (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		hk       = new Hk       (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		maccess  = new Maccess  (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		xactsum  = new XactSum  (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		xactmgmt = new XactMgmt (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		index    = new Index    (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		mdcache  = new MdCache  (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		locks    = new Locks    (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		dcache   = new DataCache(aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		pcache   = new ProcCache(aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		memory   = new Memory   (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		recovery = new Recovery (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		diskio   = new DiskIo   (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		netio    = new NetIo    (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
//		repagent = new RepAgent (aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	public void calc()
	{
		kernel  .calc();
//		wpm     .calc();
//		parallel.calc();
		taskmgmt.calc();
//		appmgmt .calc();
//		esp     .calc();
//		hk      .calc();
//		maccess .calc();
//		xactsum .calc();
//		xactmgmt.calc();
//		index   .calc();
//		mdcache .calc();
//		locks   .calc();
//		dcache  .calc();
		pcache  .calc();
//		memory  .calc();
//		recovery.calc();
//		diskio  .calc();
//		netio   .calc();
//		repagent.calc();
	}

	public void printReport()
	{
	}

	public String getReportText()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(kernel  .getReport());
		sb.append(taskmgmt.getReport());
		sb.append(pcache  .getReport());

		return sb.toString();
	}
}
