package com.asetune.sp_sysmon;

import java.sql.Timestamp;
import java.util.List;

import com.asetune.cm.CountersModel;
import com.asetune.utils.TimeUtils;

public class SpSysmon
{
	private int _NumEngines       = 0;
	private int _NumXacts         = 0;
	private int _NumElapsedMs     = 0;
	
	public void setNumEngines(int numEngines)     { _NumEngines   = numEngines; }
	public void setNumXacts(int numXacts)         { _NumXacts     = numXacts; }
	public void setNumElapsedMs(int numElapsedMs) { _NumElapsedMs = numElapsedMs; }
	
	public int getNumEngines()   { return _NumEngines; }
	public int getNumXacts()     { return _NumXacts; }
	public int getNumElapsedMs() { return _NumElapsedMs; }

	public void setAseVersionStr   (String    aseVersionStr)    { _aseVersionStr    = aseVersionStr; }
	public void setAseServerNameStr(String    aseServerNameStr) { _aseServerNameStr = aseServerNameStr; }
	public void setRunDate         (Timestamp runDate)          { _runDate          = runDate; }
	public void setSampleStartTime (Timestamp sampleStartTime)  { _sampleStartTime  = sampleStartTime; }
	public void setSampleEndTime   (Timestamp sampleEndTime)    { _sampleEndTime    = sampleEndTime; }
	public void setSampleInterval  (int       sampleInterval)   { _sampleInterval   = sampleInterval; }
	public void setSampleMode      (String    sampleMode)       { _sampleMode       = sampleMode; }
	public void setCounterClearTime(Timestamp counterClearTime) { _counterClearTime = counterClearTime; }

	public String    getAseVersionStr()    { return _aseVersionStr; }
	public String    getAseServerNameStr() { return _aseServerNameStr; }
	public Timestamp getRunDate()          { return _runDate; }
	public Timestamp getSampleStartTime()  { return _sampleStartTime; }
	public Timestamp getSampleEndTime()    { return _sampleEndTime; }
	public int       getSampleInterval()   { return _sampleInterval; }
	public String    getSampleMode()       { return _sampleMode; }
	public Timestamp getCounterClearTime() { return _counterClearTime; }
	
	private String    _aseVersionStr    = "";
	private String    _aseServerNameStr = "";
	private Timestamp _runDate          = null;
	private Timestamp _sampleStartTime  = null;
	private Timestamp _sampleEndTime    = null;
	private int       _sampleInterval   = 0;
	private String    _sampleMode       = "";
	private Timestamp _counterClearTime = null;

	public String getReportHead()
	{
		StringBuilder sb = new StringBuilder();
		
		String interval = TimeUtils.msToTimeStr(_sampleInterval);
		
		sb.append("===============================================================================\n");
		sb.append("      Sybase Adaptive Server Enterprise System Performance Report\n");
		sb.append("===============================================================================\n");
		sb.append("\n");
		sb.append("Server Version:        ").append(_aseVersionStr)   .append("\n");
		sb.append("Server Name:           ").append(_aseServerNameStr).append("\n");
		sb.append("Run Date:              ").append(_runDate)         .append("\n");
		sb.append("Sampling Started at:   ").append(_sampleStartTime) .append("\n");
		sb.append("Sampling Ended at:     ").append(_sampleEndTime)   .append("\n");
		sb.append("Sample Interval:       ").append(interval)         .append("\n");
		sb.append("Sample Mode:           ").append(_sampleMode)      .append("\n");
		sb.append("Counters Last Cleared: ").append(_counterClearTime).append("\n");
		
		return sb.toString();
	}

	private AbstractSysmonType basicCalc;
	private AbstractSysmonType kernel;
	private AbstractSysmonType wpm; // NOT YET IMPLEMENTED
	private AbstractSysmonType parallel; // NOT YET IMPLEMENTED
	private AbstractSysmonType taskmgmt;
	private AbstractSysmonType appmgmt; // NOT YET IMPLEMENTED
	private AbstractSysmonType esp; // NOT YET IMPLEMENTED
	private AbstractSysmonType hk;
	private AbstractSysmonType maccess; // NOT YET IMPLEMENTED
	private AbstractSysmonType xactsum;
	private AbstractSysmonType xactmgmt;
	private AbstractSysmonType index;
	private AbstractSysmonType mdcache; // NOT YET IMPLEMENTED
	private AbstractSysmonType locks; // NOT YET IMPLEMENTED
	private AbstractSysmonType dcache; // NOT YET IMPLEMENTED
	private AbstractSysmonType pcache;
	private AbstractSysmonType memory;
	private AbstractSysmonType recovery;
	private AbstractSysmonType diskio; // NOT YET IMPLEMENTED
	private AbstractSysmonType netio; // NOT YET IMPLEMENTED
	private AbstractSysmonType repagent; // NOT YET IMPLEMENTED
	
	public SpSysmon(CountersModel cm)
	{
		basicCalc = new BasicCalc(this, cm);
		kernel    = new Kernel   (this, cm);
		wpm       = new Wpm      (this, cm);
		parallel  = new Parallel (this, cm);
		taskmgmt  = new TaskMgmt (this, cm);
		appmgmt   = new AppMgmt  (this, cm);
		esp       = new Esp      (this, cm);
		hk        = new Hk       (this, cm);
		maccess   = new Maccess  (this, cm);
		xactsum   = new XactSum  (this, cm);
		xactmgmt  = new XactMgmt (this, cm);
		index     = new Index    (this, cm);
		mdcache   = new MdCache  (this, cm);
		locks     = new Locks    (this, cm);
		dcache    = new DataCache(this, cm);
		pcache    = new ProcCache(this, cm);
		memory    = new Memory   (this, cm);
		recovery  = new Recovery (this, cm);
		diskio    = new DiskIo   (this, cm);
		netio     = new NetIo    (this, cm);
		repagent  = new RepAgent (this, cm);
	}

	public SpSysmon(int aseVersion, int sampleTimeInMs, List<List<Object>> data, 
			int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		basicCalc = new BasicCalc(this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		kernel    = new Kernel   (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		wpm       = new Wpm      (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		parallel  = new Parallel (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		taskmgmt  = new TaskMgmt (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		appmgmt   = new AppMgmt  (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		esp       = new Esp      (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		hk        = new Hk       (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		maccess   = new Maccess  (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		xactsum   = new XactSum  (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		xactmgmt  = new XactMgmt (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		index     = new Index    (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		mdcache   = new MdCache  (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		locks     = new Locks    (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		dcache    = new DataCache(this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		pcache    = new ProcCache(this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		memory    = new Memory   (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		recovery  = new Recovery (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		diskio    = new DiskIo   (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		netio     = new NetIo    (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
		repagent  = new RepAgent (this, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	public void calc()
	{
		basicCalc.calc();
		kernel   .calc();
		wpm      .calc();
		parallel .calc();
		taskmgmt .calc();
		appmgmt  .calc();
		esp      .calc();
		hk       .calc();
		maccess  .calc();
		xactsum  .calc();
		xactmgmt .calc();
		index    .calc();
		mdcache  .calc();
		locks    .calc();
		dcache   .calc();
		pcache   .calc();
		memory   .calc();
		recovery .calc();
		diskio   .calc();
		netio    .calc();
		repagent .calc();
	}

	public void printReport()
	{
	}

	public String getReportText()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(getReportHead());

		sb.append(kernel  .getReport());
		sb.append(wpm     .getReport());
		sb.append(parallel.getReport());
		sb.append(taskmgmt.getReport());
		sb.append(appmgmt .getReport());
		sb.append(esp     .getReport());
		sb.append(hk      .getReport());		
		sb.append(maccess .getReport());
		sb.append(xactsum .getReport());
		sb.append(xactmgmt.getReport());
		sb.append(index   .getReport());
		sb.append(mdcache .getReport());
		sb.append(locks   .getReport());
		sb.append(dcache  .getReport());
		sb.append(pcache  .getReport());
		sb.append(memory  .getReport());
		sb.append(recovery.getReport());
		sb.append(diskio  .getReport());
		sb.append(netio   .getReport());
		sb.append(repagent.getReport());

		return sb.toString();
	}
}
