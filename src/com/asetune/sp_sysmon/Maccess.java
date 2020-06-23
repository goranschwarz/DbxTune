/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.asetune.sp_sysmon;

import java.util.List;

import com.asetune.cm.CountersModel;

public class Maccess extends AbstractSysmonType
{
	public Maccess(SpSysmon sysmon, CountersModel cm)
	{
		super(sysmon, cm);
	}

	public Maccess(SpSysmon sysmon, long srvVersion, int sampleTimeInMs, List<List<Object>> data, int fieldName_pos, int groupName_pos, int instanceId_pos, int value_pos)
	{
		super(sysmon, srvVersion, sampleTimeInMs, data, fieldName_pos, groupName_pos, instanceId_pos, value_pos);
	}

	@Override
	public String getReportName()
	{
		return "Monitor Access to Executing SQL";
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

		int fld_WaitsOnExecutionPlans    = 0;
		int fld_NumberOfSQLTextOverflows = 0;
		int fld_MaximumSQLTextRequested  = 0;

		for (List<Object> row : getData())
		{
			if (_instanceid_pos > 0)
				instanceid = ((Number)row.get(_instanceid_pos)).intValue();
			fieldName      = (String) row.get(_fieldName_pos);
			groupName      = (String) row.get(_groupName_pos);
//			field_id       = ((Number)row.get(_field_id_pos)).intValue();
			value          = ((Number)row.get(_value_pos)).intValue();

			//----------------------------
			// Memory
			//----------------------------

			// Waits on Execution Plans
			if (groupName.equals("monitor_access") && fieldName.equals("spin_for_plan"))
				fld_WaitsOnExecutionPlans += value;

			// Number of SQL Text Overflows
			if (groupName.equals("monitor_access") && fieldName.equals("sql_mon_txt_size_overflows"))
				fld_NumberOfSQLTextOverflows += value;
			
			// Maximum SQL Text Requested
			if (groupName.equals("monitor_access") && fieldName.equals("sql_mon_txt_reqd_hwm"))
				fld_MaximumSQLTextRequested += value;
			
		}

		//|=============================================================================== 
		//| 
		//|Monitor Access to Executing SQL
		//|-------------------------------
		//|                                  per sec      per xact       count  % of total
		//|                             ------------  ------------  ----------  ---------- 
		//| Waits on Execution Plans            0.0           0.0           0       n/a    
		//| Number of SQL Text Overflows        0.0           0.0           0       n/a    
		//| Maximum SQL Text Requested          n/a           n/a           0       n/a    
		//|  (since beginning of sample)                                                   
		//| 
		//| 
		//| Tuning Recommendations for Monitor Access to Executing SQL                     
		//| ----------------------------------------------------------                     
		//| - Consider decreasing the 'max SQL text monitored' parameter 
		//|   to 4096 (i.e., half way from its current value to Maximum 
		//|   SQL Text Requested).
		//|...
		
		addReportHead("");
		addReportLnCnt(" Waits on Execution Plans",     fld_WaitsOnExecutionPlans);
		addReportLnCnt(" Number of SQL Text Overflows", fld_NumberOfSQLTextOverflows);
		addReportLnCnt(" Maximum SQL Text Requested",   fld_MaximumSQLTextRequested);
		addReportLn   ("  (since beginning of sample)");
		
//		addReportLnNotYetImplemented();
	}
}



//---------------------------------------------------------------------------------------
//sp_sysmon_maccess from ASE 16.0
//---------------------------------------------------------------------------------------
//1> exec sp_helptext 'sp_sysmon_maccess', NULL, NULL, 'showsql,ddlgen'
//use sybsystemprocs
//go
//IF EXISTS (SELECT 1 FROM sysobjects
//           WHERE name = 'sp_sysmon_maccess'
//             AND id = object_id('sp_sysmon_maccess')
//             AND type = 'P')
//	DROP PROCEDURE sp_sysmon_maccess
//go
//
///* This stored procedure produces a report containing a summary of
//** SQL Server monitor access.
//*/
//create or replace procedure sp_sysmon_maccess
//	@NumElapsedMs int,	/* for "per Elapsed second" calculations */
//	@NumXacts int,		/* for per transactions calculations */
//	@Reco   char(1)         /* Flag for recommendations             */
//as
//
///* --------- declare local variables --------- */
//declare @tmp_int int		/* temp var for integer storage */
//declare @tmp_int2 int		/* temp var for integer storage */
//declare @tmp_total int		/* temp var for summing 'total #s' data */
//declare @sum2line char(80)	
//declare @blankline char(1)	/* to print blank line */
//declare @psign char(3)		/* hold a percent sign (%) for print out */
//declare @na_str char(3)		/* holds 'n/a' for 'not applicable' strings */
//declare @rptline char(80)	/* formatted statistics line for print 
//				** statement */
//declare @section char(80)	/* string to delimit sections on printout */
///* ------------- Variables for Tuning Recommendations ------------*/
//declare @recotxt char(80)
//declare @recoline char(80)
//declare @reco_hdr_prn bit
//declare @char_str varchar(30)
//declare @char_trimmed varchar(30)
//
///* --------- Setup Environment --------- */
//set nocount on			/* disable row counts being sent to client */
//
//select @blankline  = " "
//select @psign      = " %%"		/* extra % symbol because '%' is escape char in print statement */
//select @na_str     = "n/a"
//select @sum2line   = "                             ------------  ------------  ----------  ----------"
//select @section = "==============================================================================="
//
///* ======================= Monitor Access to Executing SQL Section =================== */
//if not exists(select *
//                from #tempmonitors
//                where group_name = "monitor_access" and
//                field_name = "spin_for_plan")
//begin
//        print @blankline
//        return 0
//end
//
//print @section
//print @blankline
//print "Monitor Access to Executing SQL"
//print "-------------------------------"
//print "                                  per sec      per xact       count  %% of total"
//print @sum2line
//
//select @tmp_int = value
//  from #tempmonitors
//  where group_name = "monitor_access" and
//        field_name = "spin_for_plan"
//
//select @rptline = " Waits on Execution Plans" +  space(3) +
//			str(@tmp_int / (@NumElapsedMs / 1000.0),12,1) 
//			+ space(2) +
//			str(@tmp_int / convert(real, @NumXacts),12,1) 
//			+ space(2) +
//			str(@tmp_int, 10) + space(7) +
//			@na_str
//print @rptline
//
//select @tmp_int = value
//  from #tempmonitors
//  where group_name = "monitor_access" and
//        field_name = "sql_mon_txt_size_overflows"
//
//select @rptline = " Number of SQL Text Overflows" +
//			str(@tmp_int / (@NumElapsedMs / 1000.0),11,1) 
//			+ space(2) +
//			str(@tmp_int / convert(real, @NumXacts),12,1) 
//			+ space(2) +
//			str(@tmp_int, 10) + space(7) +
//			@na_str
//print @rptline
//
//select @tmp_int = value
//  from #tempmonitors
//  where group_name = "monitor_access" and
//        field_name = "sql_mon_txt_reqd_hwm"
//
//select @rptline = " Maximum SQL Text Requested" +
//			space(10) + @na_str + 
//			space(11) + @na_str + 
//			space(2) + str(@tmp_int, 10) +
//			space(7) + @na_str
//print @rptline
//select @rptline = "  (since beginning of sample)"
//print @rptline
//
//print @blankline
//
//if (@Reco = 'Y')
//begin
//	print @blankline
//        select @recotxt =  " Tuning Recommendations for Monitor Access to Executing SQL"
//        select @recoline = " ----------------------------------------------------------"
//        select @reco_hdr_prn = 0
//
//        select @tmp_total = 0
//        select @tmp_int = 0
//        select @tmp_int2 = 0
//
//	select @tmp_int = convert(integer,value) from #tempconfigures
//                        where name like "max SQL text monitored"
//
//	if (@tmp_int > 0)
//	begin
//		select @tmp_int2 = value from #tempmonitors
//  			where group_name = "monitor_access" and
//			field_name = "sql_mon_txt_reqd_hwm"
//
//		/*
//		** If the high water mark for the sql text monitored
//		** is greater than 'max SQL text monitored' consider
//		** increasing the 'max SQL text monitored' configuration
//		** parameter
//		*/
//		if (@tmp_int2 > @tmp_int)
//         	begin
//                        if (@reco_hdr_prn = 0)
//                        begin
//                                 print @recotxt
//                                 print @recoline
//                                 select @reco_hdr_prn = 1
//                        end
//			select @char_str = str(@tmp_int + ((@tmp_int2 - @tmp_int)/2))
//			select @char_trimmed = ltrim(@char_str)
//			print " - Consider increasing the 'max SQL text monitored' parameter "
//			print "   to at least %1! (i.e., half way from its current value ", 
//				@char_trimmed
//			print "   to Maximum SQL Text Requested)."
//                        print @blankline
//         	end
//		else 
//		/*
//		** If the 'max SQL text monitored' is greater than the
//		** high water mark for sql text monitored
//		** consider decreasing the 'max SQL text monitored' 
//		** configuration parameter
//		*/
//		if (@tmp_int > @tmp_int2)
//	 	begin
//                        if (@reco_hdr_prn = 0)
//                        begin
//                                 print @recotxt
//                                 print @recoline
//                                 select @reco_hdr_prn = 1
//                        end
//			select @char_str = str(@tmp_int2 + ((@tmp_int - @tmp_int2)/2))
//			select @char_trimmed = ltrim(@char_str)
//			print " - Consider decreasing the 'max SQL text monitored' parameter "
//			print "   to %1! (i.e., half way from its current value to Maximum ", 
//				@char_trimmed
//			print "   SQL Text Requested)."
//                        print @blankline
//         	end
//	end
//end
//return 0
//go
//(1 rows affected)
