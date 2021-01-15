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
package com.asetune.hostmon;

import com.asetune.utils.Configuration;

public class MonitorVmstatAix
extends MonitorVmstat
{
	public MonitorVmstatAix()
	{
		this(-1, null);
	}
	public MonitorVmstatAix(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorVmstatAix";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "vmstat "+getSleepTime();
	}

//	@Override
//	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
//	{
//		HostMonitorMetaData md = new HostMonitorMetaData();
//		md.setTableName(getModuleName());
//
//		//--------------------------------------------------
//		// BELOW is output from AIX (possibly 5.2 but not sure)
//		//--------------------------------------------------
//		md.addDatetimeColumn("sampleTime", 1,  0, true, "Approximately when this record was samples");
//
//		md.addIntColumn("kthr_r",          2,  1, true, "Number of kernel threads placed in run queue.");
//		md.addIntColumn("kthr_b",          3,  2, true, "Number of kernel threads placed in wait queue (awaiting resource, awaiting input/output).");
//
//		md.addIntColumn("memory_avm",      4,  3, true, "Active virtual pages.");
//		md.addIntColumn("memory_free",     5,  4, true, "Size of the free list. Note: A large portion of real memory is utilized as a cache for file system data. It is not unusual for the size of the free list to remain small.");
//
//		md.addIntColumn("page_re",         6,  5, true, "Pager input/output list.");
//		md.addIntColumn("page_pi",         7,  6, true, "Pages paged in from paging space.");
//		md.addIntColumn("page_po",         8,  7, true, "Pages paged out to paging space.");
//		md.addIntColumn("page_fr",         9,  8, true, "Pages freed (page replacement).");
//		md.addIntColumn("page_sr",        10,  9, true, "Pages scanned by page-replacement algorithm.");
//		md.addIntColumn("page_cy",        11, 10, true, "Clock cycles by page-replacement algorithm.");
//
//		md.addIntColumn("faults_in",      12, 11, true, "Device interrupts.");
//		md.addIntColumn("faults_sy",      13, 12, true, "System calls.");
//		md.addIntColumn("faults_cs",      14, 13, true, "Kernel thread context switches.");
//
//		md.addIntColumn("cpu_us",         15, 14, true, "User time.");
//		md.addIntColumn("cpu_sy",         16, 15, true, "System time.");
//		md.addIntColumn("cpu_id",         17, 16, true, "CPU idle time.");
//		md.addIntColumn("cpu_wa",         18, 17, true, "CPU idle time during which the system had outstanding disk/NFS I/O request(s).");
//
//		// Set Percent columns
//		md.setPercentCol("cpu_us");
//		md.setPercentCol("cpu_sy");
//		md.setPercentCol("cpu_id");
//		md.setPercentCol("cpu_wa");
//		
//		// Set column "sampleTime", to a special status, which will contain the  
//		// underlying sample TIME the record was sampled
//		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);
//
//		// What regexp to use to split the input row into individual fields
//		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);
//
//		// Skip the header line
//		md.setSkipRows("memory_avm", "avm");
//
//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());
//
//		return md;
//	}

	@Override
	public HostMonitorMetaData createMetaData(int utilVersion, Configuration utilExtraInfo)
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		//--------------------------------------------------
		// BELOW is output from AIX 7.2
		//--------------------------------------------------
		md.addDatetimeColumn("sampleTime", 1,  0, true, "Approximately when this record was samples");

		md.addIntColumn("kthr_r",          2,  1, true, "Number of kernel threads placed in run queue.");
		md.addIntColumn("kthr_b",          3,  2, true, "Number of kernel threads placed in wait queue (awaiting resource, awaiting input/output).");

		md.addIntColumn("memory_avm",      4,  3, true, "Active virtual pages.");
		md.addIntColumn("memory_free",     5,  4, true, "Size of the free list. Note: A large portion of real memory is utilized as a cache for file system data. It is not unusual for the size of the free list to remain small.");

		md.addIntColumn("page_re",         6,  5, true, "Pager input/output list.");
		md.addIntColumn("page_pi",         7,  6, true, "Pages paged in from paging space.");
		md.addIntColumn("page_po",         8,  7, true, "Pages paged out to paging space.");
		md.addIntColumn("page_fr",         9,  8, true, "Pages freed (page replacement).");
		md.addIntColumn("page_sr",        10,  9, true, "Pages scanned by page-replacement algorithm.");
		md.addIntColumn("page_cy",        11, 10, true, "Clock cycles by page-replacement algorithm.");

		md.addIntColumn("faults_in",      12, 11, true, "Device interrupts.");
		md.addIntColumn("faults_sy",      13, 12, true, "System calls.");
		md.addIntColumn("faults_cs",      14, 13, true, "Kernel thread context switches.");

		md.addIntColumn("cpu_us",         15, 14, true, "User time.");
		md.addIntColumn("cpu_sy",         16, 15, true, "System time.");
		md.addIntColumn("cpu_id",         17, 16, true, "CPU idle time.");
		md.addIntColumn("cpu_wa",         18, 17, true, "CPU idle time during which the system had outstanding disk/NFS I/O request(s).");

		md.addDecColumn("cpu_pc",         19, 18, true, 10,2, "Number of physical processors used. Displayed only if the partition is running with shared processor.");
		md.addDecColumn("cpu_ec",         20, 19, true, 10,1, "The percentage of entitled capacity that is consumed. Displayed only if the partition is running with shared processor. Because the time base over which this data is computed can vary, the entitled capacity percentage can sometimes exceed 100%. This excess is noticeable only with small sampling intervals.");

		// Set Percent columns
		md.setPercentCol("cpu_us");
		md.setPercentCol("cpu_sy");
		md.setPercentCol("cpu_id");
		md.setPercentCol("cpu_wa");

		md.setPercentCol("cpu_ec");

		// Set column "sampleTime", to a special status, which will contain the  
		// underlying sample TIME the record was sampled
		md.setStatusCol("sampleTime", HostMonitorMetaData.STATUS_COL_SAMPLE_TIME);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("memory_avm", "avm");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorVmstat.", Configuration.getCombinedConfiguration());

		_pos_cpu_id = md.getParseColumnArrayPos("cpu_id");
		_pos_cpu_wa = md.getParseColumnArrayPos("cpu_wa");

		return md;
	}

	private int _pos_cpu_id; // Can be '-', which is replaced with 0.0
	private int _pos_cpu_wa; // Can be '-', which is replaced with 0.0
	
	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		if ( preParsed.length == md.getParseColumnCount() )
		{
			// Skip header 
			if (skipRow(md, row, preParsed, type))
				return null;

			// now check the ALLOWED rows
			// If NO allow entries are installed, then true will be returned.
			if ( ! allowRow(md, row, preParsed, type))
				return null;

			if ("-".equals(preParsed[_pos_cpu_id])) preParsed[_pos_cpu_id] = "-1";
			if ("-".equals(preParsed[_pos_cpu_wa])) preParsed[_pos_cpu_wa] = "-1";
			
			return preParsed;
		}
		return null;
	}
}
