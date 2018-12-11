package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class NetIo extends AbstractSysmonType
{
	public NetIo(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public NetIo(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Network I/O Management";
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
		String description= "";

		int      NumEngines        = _sysmon.getNumEngines();
		String   kernelMode        =  "not-initialized";

		int tmp_total = 0;

		int total_packets_received = 0;
		int total_bytes_received   = 0;

		int total_packets_sent     = 0;
		int total_bytes_sent       = 0;
		
		int fld_NetworkIOsDelayed  = 0;

		int[] engine_clock_ticks         = new int[NumEngines];
		int[] engine_no_packets_received = new int[NumEngines];
		int[] engine_no_bytes_received   = new int[NumEngines];
		int[] engine_no_packets_sent     = new int[NumEngines];
		int[] engine_no_bytes_sent       = new int[NumEngines];
		int   engSum_no_packets_received = 0;
		int   engSum_no_bytes_received   = 0;
		int   engSum_no_packets_sent     = 0;
		int   engSum_no_bytes_sent       = 0;
		

		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName      = (String) row.get(_fieldName_pos);
			groupName      = (String) row.get(_groupName_pos);
//			field_id       = ((Number)row.get(_field_id_pos)).intValue();
			value          = ((Number)row.get(_value_pos)).intValue();
			description    = (String) row.get(_description_pos);

			// @@kernelmode
			if (groupName.equals("ase-global-var") && fieldName.equals("@@kernelmode"))
				kernelMode = description;

			// tmp_total - 'Total Network I/O Requests'
			if (groupName.equals("kernel") && fieldName.equals("ksalloc_calls"))
				tmp_total += value; // NOT SUM, but += anyway

			// 'Network I/Os Delayed'
			if (groupName.equals("kernel") && fieldName.equals("ksalloc_sleeps"))
				fld_NetworkIOsDelayed += value; // NOT SUM, but += anyway


			// total_packets_received
			if (groupName.equals("network") && fieldName.equals("total_packets_received"))
				total_packets_received += value; // NOT SUM, but += anyway

			// total_bytes_received
			if (groupName.equals("network") && fieldName.equals("total_bytes_received"))
				total_bytes_received += value; // NOT SUM, but += anyway

			// total_packets_sent
			if (groupName.equals("network") && fieldName.equals("total_packets_sent"))
				total_packets_sent += value; // NOT SUM, but += anyway

			// total_packets_received
			if (groupName.equals("network") && fieldName.equals("total_bytes_sent"))
				total_bytes_sent += value; // NOT SUM, but += anyway

		
			// engine - clock_ticks
			if (groupName.startsWith("engine_") && fieldName.equals("clock_ticks"))
			{
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < engine_clock_ticks.length)
					engine_clock_ticks[engineNum] += value;
			}

			// engine - no_packets_received
			if (groupName.startsWith("engine_") && fieldName.equals("no_packets_received"))
			{
				engSum_no_packets_received += value;
				
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < engine_no_packets_received.length)
					engine_no_packets_received[engineNum] += value;
			}

			// engine - no_bytes_received
			if (groupName.startsWith("engine_") && fieldName.equals("no_bytes_received"))
			{
				engSum_no_bytes_received += value;
				
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < engine_no_bytes_received.length)
					engine_no_bytes_received[engineNum] += value;
			}

			// engine - no_packets_sent
			if (groupName.startsWith("engine_") && fieldName.equals("no_packets_sent"))
			{
				engSum_no_packets_sent += value;
				
				String engineNumStr = groupName.substring("engine_".length());
				int engineNum = Integer.parseInt(engineNumStr);
				if (engineNum < engine_no_packets_sent.length)
					engine_no_packets_sent[engineNum] += value;
			}

            // engine - no_bytes_sent
            if (groupName.startsWith("engine_") && fieldName.equals("no_bytes_sent"))
            {
				engSum_no_bytes_sent += value;

				String engineNumStr = groupName.substring("engine_".length());
            	int engineNum = Integer.parseInt(engineNumStr);
            	if (engineNum < engine_no_bytes_sent.length)
            		engine_no_bytes_sent[engineNum] += value;
            }

		} // end: extract data

		
		
		//-------------------------------
		// Print the report
		//-------------------------------
		
		// OUTPUT EXAMPLE from sp_sysmon:
		//|=============================================================================== 
		//| 
		//|Network I/O Management
		//|----------------------
		//| 
		//|  Network I/O Requests            per sec      per xact       count  % of total
		//|  -------------------------  ------------  ------------  ----------  ---------- 
		//|  Total Network I/O Requests          0.0           0.0           0       n/a   
		//| 
		//| 
		//|  Network Receive Activity        per sec      per xact       count
		//|  -------------------------  ------------  ------------  ----------             
		//|  Total TDS Packets Rec'd             0.0           0.0           0             
		//|  Total Bytes Rec'd                   0.0           0.0           0             
		//| 
		//|  Network Send Activity           per sec      per xact       count
		//|  -------------------------  ------------  ------------  ----------             
		//|  Total TDS Packets Sent              0.0           0.0           0       n/a   
		//|  Total Bytes Sent                    0.0           0.0           0       n/a   
		//|......
//		addReportLn("DEBUG: kernelMode='"+kernelMode+"'.");
		
		addReportHead2("  Network I/O Requests");
		addReportLnCnt("  Total Network I/O Requests",  tmp_total);
		if (tmp_total > 0)
			addReportLnPct("  Network I/Os Delayed",  fld_NetworkIOsDelayed, tmp_total);
		addReportLn   ();
		addReportLn   ();

		// PROCESS MODE -- print on all the engines
		if ("process".equals(kernelMode) || "not-initialized".equals(kernelMode))
		{
			addReportHead2("  Total TDS Packets Received");
			for (int e=0; e<NumEngines; e++)
				if (engine_clock_ticks[e] > 0)
					addReportLnPct("    Engine "+e, engine_no_packets_received[e], engSum_no_packets_received);
			addReportLnSum2();
			addReportLnCntSum("  Total TDS Packets Rec'd", engSum_no_packets_received);
			addReportLn   ();
			addReportLn   ();
			addReportHead2("  Total Bytes Received");
			for (int e=0; e<NumEngines; e++)
				if (engine_clock_ticks[e] > 0)
					addReportLnPct("    Engine "+e, engine_no_bytes_received[e], engSum_no_bytes_received);
			addReportLnSum2();
			addReportLnCntSum("  Total Bytes Rec'd", engSum_no_bytes_received);
			addReportLn   ();
			addReportLn   ();
			if (engSum_no_bytes_received > 0)
				addReportLnSC("   Avg Bytes Rec'd per Packet", engSum_no_bytes_received / engSum_no_packets_received);
			addReportLn   ();
			addReportLn   ("  -----------------------------------------------------------------------------");
			addReportLn   ();

		
			addReportHead2("  Total TDS Packets Sent");
			for (int e=0; e<NumEngines; e++)
				if (engine_clock_ticks[e] > 0)
					addReportLnPct("    Engine "+e, engine_no_packets_sent[e], engSum_no_packets_sent);
			addReportLnSum2();
			addReportLnCntSum("  Total TDS Packets Sent", engSum_no_packets_sent);
			addReportLn   ();
			addReportLn   ();
			addReportHead2("  Total Bytes Sent");
			for (int e=0; e<NumEngines; e++)
				if (engine_clock_ticks[e] > 0)
					addReportLnPct("    Engine "+e, engine_no_bytes_sent[e], engSum_no_bytes_sent);
			addReportLnSum2();
			addReportLnCntSum("  Total Bytes Sent", engSum_no_bytes_sent);
			addReportLn   ();
			addReportLn   ();
			if (engSum_no_bytes_sent > 0)
				addReportLnSC("   Avg Bytes Sent per Packet", engSum_no_bytes_sent / engSum_no_packets_sent);
			addReportLn   ();
		}
		else // THREADED MODE IS MORE COMPACT
		{
			addReportHead2("  Network Receive Activity");
			addReportLnCnt("  Total TDS Packets Rec'd",  total_packets_received);
			addReportLnCnt("  Total Bytes Rec'd",        total_bytes_received);
			addReportLn   ();

			addReportHead2("  Network Send Activit");
			addReportLnCnt("  Total TDS Packets Sent",  total_packets_sent);
			addReportLnCnt("  Total Bytes Sent",        total_bytes_sent);
		}

		addReportLn   ();
	}
}








//---------------------------------------------------------------------------------------
//sp_sysmon_netio from ASE 16.0
//---------------------------------------------------------------------------------------
//1> exec sp_helptext 'sp_sysmon_netio', NULL, NULL, 'showsql,ddlgen'
//use sybsystemprocs
//go
//IF EXISTS (SELECT 1 FROM sysobjects
//           WHERE name = 'sp_sysmon_netio'
//             AND id = object_id('sp_sysmon_netio')
//             AND type = 'P')
//	DROP PROCEDURE sp_sysmon_netio
//go
//
///* This stored procedure produces a report containing a summary of
//** network activity.
//*/
//create or replace procedure sp_sysmon_netio
//	@NumEngines tinyint,	/* number of engines online */
//	@NumElapsedMs int,	/* for "per Elapsed second" calculations */
//	@NumXacts int		/* for per transactions calculations */
//as
//
///* --------- declare local variables --------- */
//declare @i smallint		/* loop index to iterate through multi-group 
//				** counters (engine, disk, & buffer) */
//declare @tmp_grp varchar(25)	/* temp var for build group_names 
//				** ie. engine_N, disk_N */
//declare @tmp_int int		/* temp var for integer storage */
//declare @tmp_int2 int		/* temp var for integer storage */
//declare @tmp_total int		/* temp var for summing 'total #s' data */
//declare @sum1line char(80)	/* string to delimit total lines without 
//				** percent calc on printout */	
//declare @sum2line char(80)	/* string to delimit total lines without 
//				** percent calc on printout */	
//declare @subsection char(80)	/* delimit disk sections */
//declare @blankline char(1)	/* to print blank line */
//declare @psign char(3)		/* hold a percent sign (%) for print out */
//declare @na_str char(3)		/* holds 'n/a' for 'not applicable' strings */
//declare @rptline char(80)	/* formatted stats line for print statement */
//declare @section char(80)	/* string to delimit sections on printout */
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
//print "Network I/O Management"
//print "----------------------"
//print @blankline
//
//select @tmp_total = value
//  from #tempmonitors
//  where group_name = "kernel" and
//		field_name = "ksalloc_calls"
//
//print "  Network I/O Requests            per sec      per xact       count  %% of total"
//print @sum1line
//
//select @rptline = "  Total Network I/O Requests " + 
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10) + space(7) +
//			@na_str
//print @rptline
//
//if @tmp_total != 0
//  begin
//	select @tmp_int = value
//	  from #tempmonitors
//	  where group_name = "kernel" and
//			field_name = "ksalloc_sleeps"
//
//	select @rptline = "  Network I/Os Delayed" + space(7) +
//				str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//				space(2) +
//				str(@tmp_int / convert(real, @NumXacts),12,1) +
//				space(2) +
//				str(@tmp_int, 10) + space(5) +
//				str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//	print @rptline
//  end
//print @blankline
//print @blankline
//
///****************************************************
//**						
//**	Process mode version of report is per engine
//**
//*****************************************************/
//
//if @@kernelmode = "proces"
//begin
//
//print "  Total TDS Packets Received      per sec      per xact       count  %% of total"
//print @sum1line
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "no_packets_received"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total TDS Packets Rec'd             0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
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
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "no_packets_received"
//  
//			select @rptline = "    Engine " + convert(char(4), @i) + 
//					space(14) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_int, 10) + space(5) +
//					str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//			print @rptline
//		end
//
//		select @i = @i + 1
//	  end	/* while loop */
//
//	print @sum2line
//
//	select @rptline = "  Total TDS Packets Rec'd" + space(4) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end
//print @blankline
//print @blankline
//
///* 
//** save total packets rec'd for avg bytes / pkt calc 
//*/ 
//select @tmp_int2 = @tmp_total	
//
//print "  Total Bytes Received            per sec      per xact       count  %% of total"
//print @sum1line
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "no_bytes_received"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total Bytes Rec'd                   0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
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
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "no_bytes_received"
//  
//			select @rptline = "    Engine " + convert(char(4), @i) + 
//					space(14) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_int, 10) + space(5) +
//					str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//			print @rptline
//		end
//
//		select @i = @i + 1
//	  end	/* while loop */
//
//	print @sum2line
//
//	select @rptline = "  Total Bytes Rec'd" + space(10) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end	/* else */
//print @blankline
//print @blankline
//
//if @tmp_int2 != 0		/* Avoid divide by zero. */
//  begin
//	select @rptline = "   Avg Bytes Rec'd per Packet" + space(10) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_total / @tmp_int2, 10) + space(7) +
//				@na_str
//	print @rptline
//	print @blankline
//  end
//print @subsection
//print @blankline
//
//print "  Total TDS Packets Sent          per sec      per xact       count  %% of total"
//print @sum1line
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "no_packets_sent"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total TDS Packets Sent              0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
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
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "no_packets_sent"
//  
//			select @rptline = "    Engine " + convert(char(4), @i) + 
//					space(14) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_int, 10) + space(5) +
//					str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//			print @rptline
//		end
//
//		select @i = @i + 1
//	  end	/* while loop */
//
//	print @sum2line
//
//	select @rptline = "  Total TDS Packets Sent" + space(5) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end	/* else */
//print @blankline
//print @blankline
//
///* save total packets sent for avg bytes / pkt calc */ 
//select @tmp_int2 = @tmp_total
//
//print "  Total Bytes Sent                per sec      per xact       count  %% of total"
//print @sum1line
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name like "engine_%" and
//		field_name = "no_bytes_sent"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total Bytes Sent                    0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
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
//			  from #tempmonitors
//			  where group_name = @tmp_grp and
//					field_name = "no_bytes_sent"
//  
//			select @rptline = "    Engine " + convert(char(4), @i) + 
//					space(14) +
//					str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) +
//					space(2) +
//					str(@tmp_int / convert(real, @NumXacts),12,1) +
//					space(2) +
//					str(@tmp_int, 10) + space(5) +
//					str(100.0 * @tmp_int / @tmp_total,5,1) + @psign
//			print @rptline
//		end
//
//		select @i = @i + 1
//	  end	/* while loop */
//
//	print @sum2line
//
//	select @rptline = "  Total Bytes Sent" + space(11) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end	/* else */
//print @blankline
//print @blankline
//
//if @tmp_int2 != 0 		/* Avoid divide by zero. */
//  begin
//	select @rptline = "  Avg Bytes Sent per Packet" + space(11) +
//					@na_str + space(11) +
//					@na_str + space(2) +
//					str(@tmp_total / @tmp_int2, 10) + 
//					space(7) +
//					@na_str
//	print @rptline
//	print @blankline
//  end
//
//end	/* process mode */
//else
//
///****************************************************
//**						
//** Threaded mode version is a more consolidated report
//**
//*****************************************************/
//begin
//
//print "  Network Receive Activity        per sec      per xact       count"
//print @sum2line
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name = "network" and
//		field_name = "total_packets_received"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total TDS Packets Rec'd             0.0           0.0           0"
//  	print @rptline
// end
//else
//  begin
//	select @rptline = "  Total TDS Packets Rec'd" + space(4) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end
///* 
//** save total packets rec'd for avg bytes / pkt calc 
//*/ 
//select @tmp_int2 = @tmp_total	
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name = "network" and
//		field_name = "total_bytes_received"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total Bytes Rec'd                   0.0           0.0           0"
//  	print @rptline
// end
//else
//  begin
//	select @rptline = "  Total Bytes Rec'd" + space(10) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end	/* else */
//
//if @tmp_int2 != 0		/* Avoid divide by zero. */
//  begin
//	select @rptline = "  Avg Bytes Rec'd per Packet" + space(10) +
//				@na_str + space(11) +
//				@na_str + space(2) +
//				str(@tmp_total / @tmp_int2, 10)
//	print @rptline
//  end
//  
//print @blankline
//
//print "  Network Send Activity           per sec      per xact       count"
//print @sum2line
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name = "network" and
//		field_name = "total_packets_sent"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total TDS Packets Sent              0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
//  begin
//	select @rptline = "  Total TDS Packets Sent" + space(5) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end	/* else */
//
///* save total packets sent for avg bytes / pkt calc */ 
//select @tmp_int2 = @tmp_total
//
//select @tmp_total = SUM(value)
//  from #tempmonitors
//  where group_name = "network" and
//		field_name = "total_bytes_sent"
//
//if @tmp_total = 0
// begin
//	select @rptline = "  Total Bytes Sent                    0.0           0.0           0       n/a"
//  	print @rptline
// end
//else
//  begin
//	select @rptline = "  Total Bytes Sent" + space(11) +
//			str(@tmp_total / (@NumElapsedMs / 1000.0),12,1) + 
//			space(2) +
//			str(@tmp_total / convert(real, @NumXacts),12,1) + 
//			space(2) +
//			str(@tmp_total, 10)
//	print @rptline
//  end	/* else */
//
//if @tmp_int2 != 0 		/* Avoid divide by zero. */
//  begin
//	select @rptline = "  Avg Bytes Sent per Packet" + space(11) +
//					@na_str + space(11) +
//					@na_str + space(2) +
//					str(@tmp_total / @tmp_int2, 10)
//	print @rptline
//  end
//print @blankline  
//end 	/* threaded mode */
//
//return 0
//go
//(1 rows affected)

