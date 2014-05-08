package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class DiskIo extends AbstractSysmonType
{
	public DiskIo(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public DiskIo(SpSysmon sysmon, int aseVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, aseVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Disk I/O Management";
	}

//	@Override
//	public void calc()
//	{
////		String fieldName  = "";
////		String groupName  = "";
////		int    instanceid = -1;
////		int    field_id   = -1;
////		int    value      = 0;
////
////		int fld_xxx = 0;
////		int fld_yyy = 0;
////
////		for (List<Object> row : getData())
////		{
////			if (_instanceid_pos > 0)
////				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
////			fieldName = (String)row.get(_fieldName_pos);
////			groupName = (String)row.get(_groupName_pos);
//////			field_id  = ((Number)row.get(_field_id_pos)).intValue();
////			value     = ((Number)row.get(_value_pos)).intValue();
////
////			//----------------------------
////			// Memory
////			//----------------------------
////
////			// Pages Allocated
////			if (groupName.equals("group") && fieldName.equals("xxx"))
////				fld_xxx += value;
////
////			// Pages Released
////			if (groupName.equals("group") && fieldName.equals("yyy"))
////				fld_yyy += value;
////		}
////
////		addReportHead("Whatever Header");
////		addReportLnCnt("  Counter X",  fld_xxx);
////		addReportLnCnt("  Counter Y",  fld_yyy);
//		
//		addReportLnNotYetImplemented();
//	}

	@Override
	public void calc()
	{
		String fieldName  = "";
		String groupName  = "";
		int    instanceid = -1;
		int    field_id   = -1;
		int    value      = 0;
		String description= "";

		int NumEngines       = _sysmon.getNumEngines();
		int NumXacts         = _sysmon.getNumXacts();

		int NumDisks         = 0;

		// Get some basics
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName      = (String) row.get(_fieldName_pos);
			groupName      = (String) row.get(_groupName_pos);
//			field_id       = ((Number)row.get(_field_id_pos)).intValue();
			value          = ((Number)row.get(_value_pos)).intValue();
			description    = (String) row.get(_description_pos);

			// NumDisks
			if (groupName.startsWith("disk_") && fieldName.equals("sysdb_time"))
			{
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				NumDisks = Math.max(NumDisks, diskNum);
			}
		}
		
		// extra information inserted in CmSysmon: for the ability to read from a recording etc
		String[] disk_physicalName = new String[NumDisks];
		String[] disk_logicalName  = new String[NumDisks];
		String   kernelMode        =  "not-initialized";

		// 
		int   fld_max_outstanding_AIOs_server     = 0;
		int[] fld_max_outstanding_AIOs_engine     = new int[NumEngines];
		int[] fld_clock_ticks                     = new int[NumEngines];
		int[] fld_total_dpoll_completed_aios      = new int[NumEngines];

		int fld_IOsDelayedBy_DiskIOStructures     = 0;
		int fld_IOsDelayedBy_ServerConfigLimit    = 0;
		int fld_IOsDelayedBy_EngineConfigLimit    = 0;
		int fld_IOsDelayedBy_OperatingSystemLimit = 0;

		int fld_TotalRequestedDiskIOs             = 0;

		int tmp_total                             = 0;
		int tmp_total_async                       = 0;
		int tmp_total_sync                        = 0;
		int tmp_total_ios                         = 0;

		// Counters based on each disk/device
		int[] disk_total_reads_writes             = new int[NumDisks];
		int[] disk_apf_physical_reads             = new int[NumDisks];
		int[] disk_total_reads                    = new int[NumDisks];
		int[] disk_total_writes                   = new int[NumDisks];
		int[] disk_p_hits                         = new int[NumDisks];
		int[] disk_p_misses                       = new int[NumDisks];

		
		//-------------------------------
		// loop data and sore in above integers and arrays
		// later on print out the report
		//-------------------------------
		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName      = (String) row.get(_fieldName_pos);
			groupName      = (String) row.get(_groupName_pos);
			field_id       = ((Number)row.get(_field_id_pos)).intValue();
			value          = ((Number)row.get(_value_pos)).intValue();
			description    = (String) row.get(_description_pos);


			// @@kernelmode
			if (groupName.equals("ase-global-var") && fieldName.equals("@@kernelmode"))
				kernelMode = description;

			// extra device information
			if (groupName.equals("ase-device-info"))
			{
				if (field_id < disk_logicalName.length)
					disk_logicalName[field_id] = fieldName;

				if (field_id < disk_physicalName.length)
					disk_physicalName[field_id] = description;
			}

			
			
			// Max Outstanding I/Os - Server
			if (groupName.equals("kernel") && fieldName.equals("max_outstanding_AIOs_server"))
				fld_max_outstanding_AIOs_server += value; // NOT SUM, but += anyway


			// Max Outstanding I/Os - Engine
			if (groupName.startsWith("engine_") && fieldName.equals("max_outstanding_AIOs_engine"))
			{
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < fld_max_outstanding_AIOs_engine.length)
					fld_max_outstanding_AIOs_engine[engineNum] += value;
			}

			// Max Outstanding I/Os - clock_ticks
			if (groupName.startsWith("engine_") && fieldName.equals("clock_ticks"))
			{
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < fld_clock_ticks.length)
					fld_clock_ticks[engineNum] += value;
			}

			// I/Os Delayed by - Disk I/O Structures
			if (groupName.equals("kernel") && fieldName.equals("udalloc_sleeps"))
				fld_IOsDelayedBy_DiskIOStructures += value; // NOT SUM, but += anyway

			// I/Os Delayed by - Server Config Limit
			if (groupName.startsWith("engine_") && fieldName.equals("AIOs_delayed_due_to_server_limit"))
				fld_IOsDelayedBy_ServerConfigLimit += value; // SUM

			// I/Os Delayed by - Engine Config Limit
			if (groupName.startsWith("engine_") && fieldName.equals("AIOs_delayed_due_to_engine_limit"))
				fld_IOsDelayedBy_EngineConfigLimit += value; // SUM

			// I/Os Delayed by - Operating System Limit
			if (groupName.startsWith("engine_") && fieldName.equals("AIOs_delayed_due_to_os_limit"))
				fld_IOsDelayedBy_ServerConfigLimit += value; // SUM


			
			// Total Requested Disk I/Os
			if (groupName.equals("kernel") && fieldName.equals("udalloc_calls"))
				fld_TotalRequestedDiskIOs += value; // NOT SUM, but += anyway

			
			
			// tmp_total_async
			if (groupName.startsWith("engine_") && fieldName.equals("total_dpoll_completed_aios"))
				tmp_total_async += value; // SUM

			// tmp_total_sync
			if (groupName.equals("kernel") && fieldName.equals("total_sync_completed_ios"))
				tmp_total_sync += value; // NOT SUM, but += anyway
			

			// PROCESS: Asynchronous I/O's - Total Completed I/Os
			if (groupName.startsWith("engine_") && fieldName.equals("total_dpoll_completed_aios"))
			{
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < fld_max_outstanding_AIOs_engine.length)
					fld_total_dpoll_completed_aios[engineNum] += value;
			}



			//--------------------------------
			// Device activity
			//--------------------------------
			if ( groupName.startsWith("disk_") && (fieldName.equals("total_reads") || fieldName.equals("total_writes")) )
			{
				// get total summary for all disk
				tmp_total += value; // SUM

				// get total summary for individual disk device
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				if (diskNum < disk_total_reads_writes.length)
					disk_total_reads_writes[diskNum] += value;
			}
	
			// total_reads
			if ( groupName.startsWith("disk_") && fieldName.equals("total_reads") )
			{
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				if (diskNum < disk_total_reads.length)
					disk_total_reads[diskNum] += value;
			}

			// total_writes
			if ( groupName.startsWith("disk_") && fieldName.equals("total_writes") )
			{
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				if (diskNum < disk_total_writes.length)
					disk_total_writes[diskNum] += value;
			}

			// apf_physical_reads
			if ( groupName.startsWith("disk_") && fieldName.equals("apf_physical_reads") )
			{
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				if (diskNum < disk_apf_physical_reads.length)
					disk_apf_physical_reads[diskNum] += value;
			}
			
			// p_hits
			if ( groupName.startsWith("disk_") && fieldName.equals("p_hits") )
			{
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				if (diskNum < disk_p_hits.length)
					disk_p_hits[diskNum] += value;
			}
			
			// p_misses
			if ( groupName.startsWith("disk_") && fieldName.equals("p_misses") )
			{
				String diskNumStr = groupName.substring("disk_".length());
				int diskNum = Integer.parseInt(diskNumStr);
				if (diskNum < disk_p_misses.length)
					disk_p_misses[diskNum] += value;
			}
		} // end: loop data and extract
		
		tmp_total_ios = tmp_total_async + tmp_total_sync;

		
		
		
		//-------------------------------
		// Print the report
		//-------------------------------
		
		// OUTPUT EXAMPLE from sp_sysmon:
		//|=============================================================================== 
		//| 
		//|Disk I/O Management
		//|-------------------
		//| 
		//|  Max Outstanding I/Os            per sec      per xact       count  % of total
		//|  -------------------------  ------------  ------------  ----------  ---------- 
		//|    Server                            n/a           n/a           8       n/a   
		//|    Engine 0                          n/a           n/a           0       n/a   
		//|    Engine 1                          n/a           n/a           0       n/a   
		//|    Engine 2                          n/a           n/a           0       n/a   
		//|    Engine 3                          n/a           n/a           0       n/a   
		//| 
		//| 
		//|  I/Os Delayed by
		//|    Disk I/O Structures               n/a           n/a           0       n/a   
		//|    Server Config Limit               n/a           n/a           0       n/a   
		//|    Engine Config Limit               n/a           n/a           0       n/a   
		//|    Operating System Limit            n/a           n/a           0       n/a   
		//| 
		//| 
		//|  Total Requested Disk I/Os           0.4           0.6           7             
		//| 
		//|  Completed Disk I/O's
		//|    Asynchronous I/O's
		//|      Total Completed I/Os            0.4           0.6           7     100.0 %
		//|    Synchronous I/O's
		//|      Total Completed I/Os            0.0           0.0           0       n/a   
		//|  -------------------------  ------------  ------------  ----------             
		//|  Total Completed I/Os                0.4           0.6           7             
		//| 
		//| 
		//|  Device Activity Detail
		//|  ----------------------
		//| 
		//|  Device:                                                                       
		//|    C:\Sybase\devices\GORAN_15072_DS.data1                                      
		//|    data1                         per sec      per xact       count  % of total
		//|  -------------------------  ------------  ------------  ----------  ---------- 
		//|  Total I/Os                          0.0           0.0           0       n/a   
		//|  -------------------------  ------------  ------------  ----------  ---------- 
		//|  Total I/Os                          0.0           0.0           0       0.0 %
		//| 
		//| 
		//|  ----------------------------------------------------------------------------- 
		//| 
		//|  Device:                                                                       
		//|    C:\Sybase\devices\GORAN_15072_DS.master                                     
		//|    master                        per sec      per xact       count  % of total
		//|  -------------------------  ------------  ------------  ----------  ---------- 
		//|    Reads                                                                       
		//|      APF                             0.0           0.0           0       0.0 %
		//|      Non-APF                         0.0           0.0           0       0.0 %
		//|    Writes                            0.3           0.5           6     100.0 %
		//|  -------------------------  ------------  ------------  ----------  ---------- 
		//|  Total I/Os                          0.3           0.5           6      85.7 %
		//| 
		//| 
		//|  ----------------------------------------------------------------------------- 
		//|......

//		addReportLn("Disk I/O Management");
//		addReportLn("-------------------");
//		addReportLn("");
//		addReportLn("DEBUG: kernelMode='"+kernelMode+"'.");

		addReportHead2("  Max Outstanding I/Os");
		addReportLnSC("    Server", fld_max_outstanding_AIOs_server);
		for (int i=0; i<NumEngines; i++)
		{
			if (fld_clock_ticks[i] > 0)
				addReportLnSC("    Engine "+i, fld_max_outstanding_AIOs_engine[i]);
		}
		addReportLn("");
		addReportLn("");

		addReportLn  ("  I/Os Delayed by");
		addReportLnSC("    Disk I/O Structures",    fld_IOsDelayedBy_DiskIOStructures);
		addReportLnSC("    Server Config Limit",    fld_IOsDelayedBy_ServerConfigLimit);
		addReportLnSC("    Engine Config Limit",    fld_IOsDelayedBy_EngineConfigLimit);
		addReportLnSC("    Operating System Limit", fld_IOsDelayedBy_OperatingSystemLimit);
		addReportLn("");
		addReportLn("");

		addReportLnCnt("  Total Requested Disk I/Os", fld_TotalRequestedDiskIOs);		
		addReportLn("");

		addReportLn   ("  Completed Disk I/O's");
		if (tmp_total_ios == 0)
		{
			addReportLn("      Total Completed I/Os              0.0         0.0           0       n/a");
		}
		else
		{
			addReportLn   ("    Asynchronous I/O's");
    		if ("process".equals(kernelMode) || "not-initialized".equals(kernelMode))
    		{
    			for (int i=0; i<NumEngines; i++)
    			{
    				if (fld_clock_ticks[0] > 0)
    					addReportLnPct("      Engine "+i, fld_total_dpoll_completed_aios[i], tmp_total_async);
    			}
    		}
    		else
    		{
    			addReportLnPct("      Total Completed I/Os", tmp_total_async, tmp_total_ios);
    		}
		}
		addReportLn   ("    Synchronous I/O's");
		addReportLnCnt("      Total Completed I/Os", tmp_total_sync);
		addReportLnSum2();
		addReportLnCnt("  Total Completed I/Os", tmp_total_ios);		
		addReportLn("");
		addReportLn("");
		addReportLn("  Device Activity Detail");
		addReportLn("  ----------------------");
		addReportLn("");
		if (tmp_total == 0)
		{
			addReportLn("    No Disk I/O in Given Sample Period");
			addReportLn("");
		}
		else
		{
			for (int i=0; i<NumDisks; i++)
			{
				if (disk_physicalName[i] == null)
					continue;

				addReportLn   ("  Device:");
				addReportLn   ("    " + disk_physicalName[i]);
				addReportHead2("    " + disk_logicalName[i]);

				int deviceTotalIOs = disk_total_reads_writes[i];
				if (deviceTotalIOs > 0)
				{
					if (disk_apf_physical_reads[i] == 0)
						addReportLnPct("    Reads", disk_total_reads[i], deviceTotalIOs);
					else
					{
						int apfReads    = disk_apf_physical_reads[i];
						int nonApfReads = disk_total_reads[i] - disk_apf_physical_reads[i];
						
						addReportLn   ("    Reads");
						addReportLnPct("      APF",     apfReads,    deviceTotalIOs);
						addReportLnPct("      Non-APF", nonApfReads, deviceTotalIOs);
					}
					addReportLnPct("    Writes", disk_total_writes[i], deviceTotalIOs);
    				addReportLnSum2();
				}
   				addReportLnPct("  Total I/Os", deviceTotalIOs, tmp_total);

				int p_hits_misses = disk_p_hits[i] + disk_p_misses[i]; 
				if ( p_hits_misses > 0 )
				{
					addReportLnPct("  Mirror Semaphore Granted", disk_p_hits[i],   p_hits_misses);
					addReportLnPct("  Mirror Semaphore Waited",  disk_p_misses[i], p_hits_misses);
				}
				addReportLn("");
				addReportLn("  -----------------------------------------------------------------------------");
				addReportLn("");
			}
		} // end: report individual disk devices
		
	} // end: method

} // end: class

//---------------------------------------------------------------------------------------
// sp_sysmon_diskio from ASE 16.0
//---------------------------------------------------------------------------------------
//1> exec sp_helptext 'sp_sysmon_diskio', NULL, NULL, 'showsql,ddlgen'
//use sybsystemprocs
//go
//IF EXISTS (SELECT 1 FROM sysobjects
//           WHERE name = 'dbo.sp_sysmon_diskio'
//             AND id = object_id('dbo.sp_sysmon_diskio')
//             AND type = 'P')
//	DROP PROCEDURE dbo.sp_sysmon_diskio
//go
//
///* This stored procedure produces a report containing a summary of
//** of disk activity.
//*/
//create or replace procedure sp_sysmon_diskio
//	@NumEngines tinyint,	/* number of engines online */
//	@NumElapsedMs int,	/* for "per Elapsed second" calculations */
//	@NumXacts int,		/* for per transactions calculations */
//	@Reco	char(1)		/* Flag for recommendations */
//as
//
///* --------- declare local variables --------- */
//declare @SybDiskName varchar(265) /* cursor var for logical disk name - 
//				** sysdevices.name */
//declare @PhyDiskName varchar(127) /* cursor var for physical disk name - 
//				  ** sysdevices.phyname */
//
//declare @i smallint		/* loop index to iterate through multi-group
//				**  counters (engine, disk, & buffer) */
//declare @tmp_grp varchar(25)	/* temp var for build group_name's - ie., 
//				** engine_N, disk_N */
//declare @tmp_int int		/* temp var for integer storage */
//declare @tmp_int2 int		/* temp var for integer storage */
//declare @tmp_int3 int           /* temp var for integer storage */
//declare @tmp_int4 int           /* temp var for integer storage */
//declare @tmp_total int          /* temp var for summing 'total #s' data */
//declare @tmp_total_async int    /* temp var for summing total #s of
//                                ** asynchronous IOs completed */
//declare @tmp_total_sync int     /* temp var for summing total #s of synchronous
//                                ** IOs completed */
//declare @tmp_total_ios int      /* temp var for summing total #s of IOs
//                                ** completed.
//                                ** @tmp_total_ios = @tmp_total_async + 
//				** @tmp_total_sync
//                                */
//declare @sum1line char(80)	/* string to delimit total lines without 
//				** percent calc on printout */	
//declare @sum2line char(80)	/* string to delimit total lines without 
//				** percent calc on printout */	
//declare @subsection char(80)	/* delimit disk sections */
//declare @blankline char(1)	/* to print blank line */
//declare @psign char(3)		/* hold a percent sign (%) for print out */
//declare @na_str char(3)		/* holds 'n/a' for 'not applicable' strings */
//declare @rptline char(80)	/* formatted stats line for print statement */
//declare @section char(80)       /* string to delimit sections on printout */
//
///* ------------- Variables for Tuning Recommendations ------------*/
//declare @recotxt char(80)       /* Header for tuning recommendation */
//declare @recoline char(80)      /* to underline recotxt */
//declare @reco_hdr_prn bit       /* to indicate if the recotxt is already printed */
//declare @reco_total_diskio int
//declare @reco_diskio_struct_delay int
//declare @reco_maxaio_server int
//declare @reco_maxaio_engine int
//declare @reco_aio_os_limit int
//
///* --------- Setup Environment --------- */
//set nocount on			/* disable row counts being sent to client */
//
//select @sum1line   = "  -------------------------  ------------  ------------  ----------  ----------"
//select @sum2line   = "  -------------------------  ------------  ------------  ----------"
//select @subsection = "  -----------------------------------------------------------------------------"
//select @blankline  = " "
//select @psign      = " %%"		/* extra % symbol because '%' is escape char in print statement */
//select @na_str     = "n/a"
//select @section = "==============================================================================="
//
//print @section
//print @blankline
//print "Disk I/O Management"
//print "-------------------"
//print @blankline
//print "  Max Outstanding I/Os            per sec      per xact       count  %% of total"
//print @sum1line
//
//select @tmp_int = value
//  from #tempmonitors
//  where group_name = "kernel" and
//		field_name = "max_outstanding_AIOs_server"
//
//select @rptline = "    Server" + space(28) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_int, 10) + space(7) +
//				@na_str
//print @rptline
//
//select @i = 0
//while @i < @NumEngines		/* for each engine */
//  begin
//	/* build group_name string */
//	select @tmp_grp = "engine_" + convert(varchar(4), @i)
// 
//	/* If an engine's clock_ticks counter is 0, this means this engine
//	** is in offline status. We should skip this engine when priting
//	** the statistic information.
//	*/
//	if (select value
//		from #tempmonitors where field_name="clock_ticks"
//			and group_name=@tmp_grp) > 0
//	begin
//		select @tmp_int = value
//	  	from #tempmonitors
//	  	where group_name = @tmp_grp and
//          	  field_name = "max_outstanding_AIOs_engine"
//  
//		select @rptline = "    Engine " + convert(char(4), @i) + space(23) +
//						@na_str + space(11) +
//						@na_str + space(2) +
//						str(@tmp_int, 10) + space(7) +
//						@na_str
//		print @rptline
//	end
//
//	select @i = @i + 1
//  end
//
//print @blankline
//print @blankline
//print "  I/Os Delayed by"
//
//select @tmp_int = value, @reco_diskio_struct_delay = value
//  from #tempmonitors
//  where group_name = "kernel" and
//		field_name = "udalloc_sleeps"
//
//select @rptline = "    Disk I/O Structures" + space(15) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_int, 10) + space(7) +
//				@na_str
//print @rptline
//
//select @tmp_int = SUM(value), @reco_maxaio_server = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "AIOs_delayed_due_to_server_limit"
//
//select @rptline = "    Server Config Limit" + space(15) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_int, 10) + space(7) +
//				@na_str
//print @rptline
//
//select @tmp_int = SUM(value), @reco_maxaio_engine = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "AIOs_delayed_due_to_engine_limit"
//
//select @rptline = "    Engine Config Limit" + space(15) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_int, 10) + space(7) +
//				@na_str
//print @rptline
//
//select @tmp_int = SUM(value), @reco_aio_os_limit = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "AIOs_delayed_due_to_os_limit"
//
//select @rptline = "    Operating System Limit" + space(12) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_int, 10) + space(7) +
//				@na_str
//print @rptline
//
//print @blankline
//print @blankline
//
//select @tmp_int = value , @reco_total_diskio = value
//  from #tempmonitors
//  where group_name = "kernel" and
//		field_name = "udalloc_calls"
//
//select @rptline = "  Total Requested Disk I/Os" + space(2) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(2) +
//				str(@tmp_int, 10) 
//print @rptline
//print @blankline
//print "  Completed Disk I/O's"
//
//select @tmp_total_async = isnull(SUM(value), 0)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "total_dpoll_completed_aios"
//
//select @tmp_total_sync = value
//  from #tempmonitors
//  where group_name like "kernel" and
//		field_name = "total_sync_completed_ios"
//
//select @tmp_total_ios = @tmp_total_async + @tmp_total_sync
//
//print "    Asynchronous I/O's"
//if @tmp_total_async = 0
// begin
//	select @rptline = "      Total Completed I/Os            0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
//begin
//  if @@kernelmode = "process"
//  begin
//	select @i = 0
//	while @i < @NumEngines		/* for each engine */
//	  begin
//		/* build group_name string */
//		select @tmp_grp = "engine_" + convert(varchar(4), @i)
//  
//		/* If an engine's clock_ticks counter is 0, this means this engine
//		** is in offline status. We should skip this engine when priting
//		** the statistic information.
//		*/
//		if (select value
//			from #tempmonitors where field_name="clock_ticks"
//				and group_name=@tmp_grp) > 0
//		begin
//			select @tmp_int = value
//		  	from #tempmonitors
//		  	where group_name = @tmp_grp and
//					field_name = "total_dpoll_completed_aios"
//  
//			select @rptline = "      Engine " + convert(char(4), @i) + 
//					space(12) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_int, 10) + space(5) +
//					str(100.0 * @tmp_int / @tmp_total_async,5,1) + @psign
//			print @rptline
//		end
//
//		select @i = @i + 1
//	  end	/* while loop */
//  end	/* else */
//  else
//  begin	/* threaded kernel */
//	select @rptline = "      Total Completed I/Os" +
//		space(3) +
//		str(@tmp_total_async / (@NumElapsedMs / 1000.0),12,1) +
//		space(2) +
//		str(@tmp_total_async / convert(real, @NumXacts),12,1) +
//		space(2) +
//		str(@tmp_total_async, 10) + space(5) +
//		str(100.0 * @tmp_total_async / @tmp_total_ios,5,1) + @psign
//	print @rptline
//  end
//end
//
//print "    Synchronous I/O's"
//if @tmp_total_sync = 0
// begin
//	select @rptline = "      Total Completed I/Os            0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
//  begin
//		select @rptline = "      Total Completed I/Os   " +
//				str(@tmp_total_sync / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_total_sync / convert(real, @NumXacts),12,1) +
//				space(2) +
//				str(@tmp_total_sync, 10) + space(5) +
//				str(100.0,5,1) + @psign
//		print @rptline
//  end	/* else */
//
//print @sum2line
//
//select @rptline = "  Total Completed I/Os" + space(7) +
//			str(@tmp_total_ios / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total_ios / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total_ios, 10)
//print @rptline
//print @blankline
//print @blankline
//print "  Device Activity Detail"
//print "  ----------------------"
//print @blankline
//
///* get total number of I/Os to all devices to calc each device's percentage */
//select @tmp_total = isnull(SUM(value), 0)
//  from #tempmonitors
//  where group_name like "disk_%" and
//		(field_name = "total_reads" or field_name = "total_writes")
//
//if @tmp_total = 0
//  begin
//	print "    No Disk I/O in Given Sample Period"
//	print @blankline
//  end
//else
//  begin
//        declare disk_info cursor for
//                select name, phyname,group_name 
//                  from #devicemap
//                  order by phyname
//                  for read only
//
//	open disk_info
//	fetch disk_info into @SybDiskName, @PhyDiskName, @tmp_grp
//
//	while (@@sqlstatus = 0)
//	  begin
//
//		select @rptline = "  Device:"
//		print @rptline
//		select @rptline = space(4) + substring(@PhyDiskName, 1, 76)
//		print @rptline
//		select @rptline = space(4) + convert(char(25), 
//			substring(@SybDiskName, 1, 25)) + 
//			"     per sec      per xact       count  %% of total"
//		print @rptline
//
//		print @sum1line
//
//		select @tmp_int2 = isnull(SUM(value), 0)
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//				(field_name = "total_reads" or 
//					field_name = "total_writes")
//
//		if @tmp_int2 = 0
// 		begin
//			select @rptline = "  Total I/Os                          0.0           0.0           0       n/a"
//  			print @rptline
// 		end
//		else
//		  begin
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "total_reads"
//			if not exists(select * 
//					from #tempmonitors
//					where group_name = @tmp_grp and 
//					field_name="apf_physical_reads")
//
//			begin /*{ begin to check existence of apf counters*/
//	                       select @rptline = "    Reads" + space(20) +
//                                str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//                                space(2) +
//                                str(@tmp_int / convert(real, @NumXacts),12,1) +
//                                space(2) +
//                                str(@tmp_int, 10) + space(5) +
//                                str(100.0 * @tmp_int / @tmp_int2,5,1) + @psign
//       		                 print @rptline
//			end /*} end for apf counter check*/
//			
//			else
//			begin	/*{begin in case apf counters exist*/
//                        	select @tmp_int3 = value
//                          	from #tempmonitors
//                          	where group_name = @tmp_grp and
//                                        field_name = "apf_physical_reads"
//
//                        	select @tmp_int4 = @tmp_int - @tmp_int3
//
//                        	select @rptline = "    Reads"
//                        	print @rptline
//
//                        	select @rptline = "      APF" + space(20) +
//                                str(@tmp_int3 / (@NumElapsedMs / 1000.0),12,1) +
//                                space(2) +
//                                str(@tmp_int3 / convert(real, @NumXacts),12,1) +
//                                space(2) +
//                                str(@tmp_int3, 10) + space(5) +
//                                str(100.0 * @tmp_int3 / @tmp_int2,5,1) + @psign
//                        	print @rptline
//
//                        	select @rptline = "      Non-APF" + space(16) +
//                                str(@tmp_int4 / (@NumElapsedMs / 1000.0),12,1) +
//                                space(2) +
//                                str(@tmp_int4 / convert(real, @NumXacts),12,1) +
//                                space(2) +
//                                str(@tmp_int4, 10) + space(5) +
//                                str(100.0 * @tmp_int4 / @tmp_int2,5,1) + @psign
//                        	print @rptline
//			end /*} end in case where apf counters exist*/
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "total_writes"
//
//			select @rptline = "    Writes" + space(19) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(2) +
//				str(@tmp_int, 10) + space(5) +
//				str(100.0 * @tmp_int / @tmp_int2,5,1) + @psign
//			print @rptline
//
//		  end	/* else @tmp_int2 != 0 */
//
//		print @sum1line
//
//		select @rptline = "  Total I/Os" + space(17) +
//			str(@tmp_int2 / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_int2 / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_int2, 10) + space(5) +
//			str(100.0 * @tmp_int2 / @tmp_total,5,1) + @psign
//		print @rptline
//		print @blankline
//
//		select @tmp_int2 = isnull(SUM(value), 0)
//		  from #tempmonitors
//		  where group_name = @tmp_grp and
//			(field_name = "p_hits" or field_name = "p_misses")
//
//		if @tmp_int2 != 0
//		  begin
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "p_hits"
//
//			select @rptline = "  Mirror Semaphore Granted" + 
//				space(3) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(2) +
//				str(@tmp_int, 10) + space(5) +
//				str(100.0 * @tmp_int / @tmp_int2,5,1) + @psign
//			print @rptline
//
//			select @tmp_int = value
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "p_misses"
//
//			select @rptline = "  Mirror Semaphore Waited" + 
//				space(4) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(2) +
//				str(@tmp_int, 10) + space(5) +
//				str(100.0 * @tmp_int / @tmp_int2,5,1) + @psign
//			print @rptline
//		  end	/* else @tmp_int2 != 0 */ 
//
//		print @blankline
//		print @subsection
//		print @blankline
//
//		fetch disk_info into @SybDiskName, @PhyDiskName, @tmp_grp
//
//	  end	/* while @@sqlstatus */
//
//	close disk_info 
//	deallocate cursor disk_info
//
//  end	/* else @tmp_total != 0 */
//
//print @blankline
//
//if (@Reco = 'Y' and @reco_total_diskio != 0)
// begin
//	select @recotxt = "  Tuning Recommendations for Disk I/O Management"
//	select @recoline = "  ----------------------------------------------"
//	select @reco_hdr_prn = 0
//
//	select @reco_diskio_struct_delay = convert(int, 
//		(100.0 * ((1.0 *@reco_diskio_struct_delay)/@reco_total_diskio)))
//	select @reco_maxaio_server = convert(int,
//		(100.0 * ((1.0 *@reco_maxaio_server)/@reco_total_diskio)))
//	select @reco_maxaio_engine = convert(int,
//		(100.0 * ((1.0 *@reco_maxaio_engine)/@reco_total_diskio)))
//	select @reco_aio_os_limit = convert(int,
//		(100.0 * ((1.0 *@reco_aio_os_limit)/@reco_total_diskio)))
//
//	/*
//	** If the % of I/O's delayed on account of
//	** number of disk I/O structures is > 5%
//	** consider increasing the number of disk i/o
//	** structures
//	*/
//	if @reco_diskio_struct_delay > 5
//	begin
//                if (@reco_hdr_prn = 0)
//                begin
//                        print @recotxt
//                        print @recoline
//                        select @reco_hdr_prn = 1
//                end
//		
//		print "  - Consider increasing the 'disk i/o structures'"
//		print "    configuration parameter."
//		print @blankline
//		select @reco_hdr_prn = 1
//	end
//
//	/*
//	** If the % of the number of I/O's delayed on account of
//	** the server limit is > 5
//	** consider increasing the 'max async I/Os per server'
//	** configuration parameter 
//	*/
//	if @reco_maxaio_server > 5
//	begin
//                if (@reco_hdr_prn = 0)
//                begin
//                        print @recotxt
//                        print @recoline
//                        select @reco_hdr_prn = 1
//                end
//
//		print "  - Consider increasing the 'max async I/Os per server'"
//		print "    configuration parameter."
//		print @blankline
//		select @reco_hdr_prn = 1
//	end
//
//	/*
//	** If the % of the number of I/O's delayed on account of
//	** the engine limit is > 5
//	** consider increasing 'max async I/Os per engine'
//	*/
//	if @reco_maxaio_engine > 5
//	begin
//                if (@reco_hdr_prn = 0)
//                begin
//                        print @recotxt
//                        print @recoline
//                        select @reco_hdr_prn = 1
//                end
//
//		print "  - Consider increasing the 'max async I/Os per engine'"
//		print "    configuration parameter."
//		print @blankline
//		select @reco_hdr_prn = 1
//	end
//
//	/*
//	** If the % of the number of I/O's delayed on account of
//	** operating system parameters governing async I/O
//	** is greater than 5, then consider increasing that
//	** parameter.
//	*/
//	if @reco_aio_os_limit > 5
//	begin
//                if (@reco_hdr_prn = 0)
//                begin
//                        print @recotxt
//                        print @recoline
//                        select @reco_hdr_prn = 1
//                end
//
//		print "  - Consider increasing the operating system parameter"
//		print "    governing the number of asynchronous I/O's."
//		print @blankline
//		select @reco_hdr_prn = 1
//	end
//
// end
//
//print @blankline
//
//return 0
//go
