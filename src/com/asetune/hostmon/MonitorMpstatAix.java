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

public class MonitorMpstatAix
extends MonitorMpstat
{
	public MonitorMpstatAix()
	{
		this(-1, null);
	}
	public MonitorMpstatAix(int utilVersion, String utilExtraInfo)
	{
		super(utilVersion, utilExtraInfo);
	}

	@Override
	public String getModuleName()
	{
		return "MonitorMpstatAix";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "mpstat -a "+getSleepTime();
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
//		md.addStrColumn( "cpu",      1,  1, false,    5, "Without the -a option, mpstat reports CPU statistics for a processor ID. With the -a option, mpstat reports SET statistics for a processor set ID.");
//		md.addIntColumn( "samples",  2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");
//
//		md.addStatColumn("min",      3,  2, true, 10,  1, "The minor page faults (page faults with no IO).");
//		md.addStatColumn("maj",      4,  3, true, 10,  1, "The major page faults (page faults with disk IO).");
//		md.addStatColumn("mpcs",     5,  4, true, 10,  1, "The number of mpc send interrupts.");
//		md.addStatColumn("mpcr",     6,  5, true, 10,  1, "The number of mpc receive interrupts.");
////		md.addStatColumn("mpc",      x,  x, true, 10,  1, "The total number of inter-processor calls");
//		md.addStatColumn("dev",      7,  6, true, 10,  1, "The number of device interrupts.");
//		md.addStatColumn("soft",     8,  7, true, 10,  1, "The number of software interrupts.");
//		md.addStatColumn("dec",      9,  8, true, 10,  1, "The number of decrementer interrupts.");
//		md.addStatColumn("ph",      10,  9, true, 10,  1, "The number of phantom interrupts.");
////		md.addStatColumn("int",     xx,  x, true, 10,  1, "The total number of interrupts.");
//		md.addStatColumn("cs",      11, 10, true, 10,  1, "The total number of context switches.");
//		md.addStatColumn("ics",     12, 12, true, 10,  1, "The total number of involuntary context switches.");
//		md.addStatColumn("bound",   13, 12, true, 10,  1, "The total number of threads that are bound.");
//		md.addStatColumn("rq",      14, 13, true, 10,  1, "The run queue size.");
//		md.addStatColumn("push",    15, 14, true, 10,  1, "The number of migrations due to starvation load balancing.");
//		md.addStatColumn("S3pull",  16, 15, true, 10,  1, "The number of migrations outside the scheduling affinity domain 3 due to idle stealing.");
//		md.addStatColumn("S3grd",   17, 16, true, 10,  1, "The number of dispatches from global runqueue, outside the scheduling affinity domain 3.");
////		md.addStatColumn("mig",     xx, xx, true, 10,  1, "The total number of thread migrations (to another logical processor).");
//		md.addStatColumn("S0rd",    18, 17, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 0.");
//		md.addStatColumn("S1rd",    19, 18, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 1.");
//		md.addStatColumn("S2rd",    20, 19, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 2.");
//		md.addStatColumn("S3rd",    21, 20, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 3.");
//		md.addStatColumn("S4rd",    22, 21, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 4.");
//		md.addStatColumn("S5rd",    23, 22, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 5.");
////		md.addStatColumn("lpa",     xx, xx, true, 10,  1, "The logical processor affinity. The percentage of logical processor redispatches within the scheduling affinity domain 3.");
//		md.addStatColumn("sysc",    24, 23, true, 10,  1, "The number of system calls.");
//		md.addStatColumn("us",      25, 24, true, 10,  1, "The percentage of physical processor utilization that occurred while executing at the user level (application).");
//		md.addStatColumn("sy",      26, 25, true, 10,  1, "The percentage of physical processor utilization that occurred while executing at the system level (kernel).");
//		md.addStatColumn("wt",      27, 26, true, 10,  1, "The percentage of time that the logical processor was idle and it did not have an outstanding disk I/O request.");
//		md.addStatColumn("id" ,     28, 27, true, 10,  1, "The percentage of time that the logical processor was idle during which it had an outstanding disk I/O request.");
//		md.addStatColumn("pc",      29, 28, true, 10,  1, "The fraction of physical processor consumed. It is displayed in both shared partition and dedicated partition. For the default flag in the dedicated partition, it is not displayed when both donation and simultaneous multithreading are disabled. The pc of the cpuid U row represents the number of unused physical processors.");
////		md.addStatColumn("ec",      xx, xx, true, 10,  1, "The percentage of entitled capacity consumed by the logical CPU. The %ec of the ALL CPU row represents the percentage of entitled capacity consumed. Because the time base over which this data is computed can vary, the entitled capacity percentage can sometimes exceed 100%. This excess is noticeable only with small sampling intervals. The attribute is displayed only in a shared partition.");
//		md.addStatColumn("ilcs",    30, 29, true, 10,  1, "The number of involuntary logical CPU context switches, displayed only in shared partition. For the -h and -a flags, it is also displayed in dedicated partition.");
//		md.addStatColumn("vlcs",    31, 30, true, 10,  1, "The number of voluntary logical CPU context switches. Displayed only in shared partition. For the -h and -a flags, it is also displayed in dedicated partition.");
////		md.addStatColumn("lcs",     xx, xx, true, 10,  1, "The total number of logical CPU context switches. Displayed only in shared partition or when dedicated partition is donating.");
////		md.addStatColumn("idon",    xx, xx, true, 10,  1, "The percentage of physical processor utilization that occurs while explicitly donating idle cycles. Displayed only in dedicated partition that is donating.");
////		md.addStatColumn("bdon",    xx, xx, true, 10,  1, "The percentage of physical processor utilization that occurs while donating busy cycles. Displayed only in dedicated partition that is donating.");
//		md.addStatColumn("istol",   32, 31, true, 10,  1, "The percentage of physical processor utilization that occurs while the Hypervisor is stealing idle cycles. Displayed only in dedicated partition.");
//		md.addStatColumn("bstol",   33, 32, true, 10,  1, "The percentage of physical processor utilization that occurs while the Hypervisor is stealing busy cycles. Displayed only in dedicated partition.");
////		md.addStatColumn("nsp",     xx, xx, true, 10,  1, "The current average processor speed as a percentage of nominal speed. Displayed only if the hardware uses the SPURR.");
//
//		// Set Percent columns
//		md.setPercentCol("S0rd");
//		md.setPercentCol("S1rd");
//		md.setPercentCol("S2rd");
//		md.setPercentCol("S3rd");
//		md.setPercentCol("S4rd");
//		md.setPercentCol("S5rd");
//
//		md.setPercentCol("us");
//		md.setPercentCol("sy");
//		md.setPercentCol("wt");
//		md.setPercentCol("id");
//
//		md.setPercentCol("istol");
//		md.setPercentCol("bstol");
//
//		// Use "cpu" as the Primary Key, which is used to du summary/average calculations
//		md.setPkCol("cpu");
//
//		// Set column "samples", to a special status, which will contain number of 
//		// underlying samples the summary/average caclulation was based on
//		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);
//
//		// What regexp to use to split the input row into individual fields
//		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);
//
//		// Skip the header line
//		md.setSkipRows("cpu", "cpu");
//
//		// Get SKIP and ALLOW from the Configuration
//		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
//		md.setSkipAndAllowRows("hostmon.MonitorMpstat.", Configuration.getCombinedConfiguration());
//
//		// set positions of some columns, this will be used in parseRow() below, to replace '-' to '0.0'
//		_pos_S0rd = md.getParseColumnArrayPos("S0rd");
//		_pos_S1rd = md.getParseColumnArrayPos("S1rd");
//		_pos_S2rd = md.getParseColumnArrayPos("S2rd");
//		_pos_S3rd = md.getParseColumnArrayPos("S3rd");
//		_pos_S4rd = md.getParseColumnArrayPos("S4rd");
//		_pos_S5rd = md.getParseColumnArrayPos("S5rd");
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
		md.addStrColumn( "cpu",      1,  1, false,    5, "Without the -a option, mpstat reports CPU statistics for a processor ID. With the -a option, mpstat reports SET statistics for a processor set ID.");
		md.addIntColumn( "samples",  2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("min",      3,  2, true, 10,  1, "The minor page faults (page faults with no IO).");
		md.addStatColumn("maj",      4,  3, true, 10,  1, "The major page faults (page faults with disk IO).");
		md.addStatColumn("mpcs",     5,  4, true, 10,  1, "The number of mpc send interrupts.");
		md.addStatColumn("mpcr",     6,  5, true, 10,  1, "The number of mpc receive interrupts.");
//		md.addStatColumn("mpc",      x,  x, true, 10,  1, "The total number of inter-processor calls");
		md.addStatColumn("dev",      7,  6, true, 10,  1, "The number of device interrupts.");
		md.addStatColumn("soft",     8,  7, true, 10,  1, "The number of software interrupts.");
		md.addStatColumn("dec",      9,  8, true, 10,  1, "The number of decrementer interrupts.");
		md.addStatColumn("ph",      10,  9, true, 10,  1, "The number of phantom interrupts.");
//		md.addStatColumn("int",     xx,  x, true, 10,  1, "The total number of interrupts.");
		md.addStatColumn("cs",      11, 10, true, 10,  1, "The total number of context switches.");
		md.addStatColumn("ics",     12, 12, true, 10,  1, "The total number of involuntary context switches.");
		md.addStatColumn("bound",   13, 12, true, 10,  1, "The total number of threads that are bound.");
		md.addStatColumn("rq",      14, 13, true, 10,  1, "The run queue size.");
		md.addStatColumn("push",    15, 14, true, 10,  1, "The number of migrations due to starvation load balancing.");
		md.addStatColumn("S3pull",  16, 15, true, 10,  1, "The number of migrations outside the scheduling affinity domain 3 due to idle stealing.");
		md.addStatColumn("S3grd",   17, 16, true, 10,  1, "The number of dispatches from global runqueue, outside the scheduling affinity domain 3.");
//		md.addStatColumn("mig",     xx, xx, true, 10,  1, "The total number of thread migrations (to another logical processor).");
		md.addStatColumn("S0rd",    18, 17, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 0.");
		md.addStatColumn("S1rd",    19, 18, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 1.");
		md.addStatColumn("S2rd",    20, 19, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 2.");
		md.addStatColumn("S3rd",    21, 20, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 3.");
		md.addStatColumn("S4rd",    22, 21, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 4.");
		md.addStatColumn("S5rd",    23, 22, true, 10,  1, "The percentage of thread redispatches within the scheduling affinity domain 5.");
//		md.addStatColumn("lpa",     xx, xx, true, 10,  1, "The logical processor affinity. The percentage of logical processor redispatches within the scheduling affinity domain 3.");
		md.addStatColumn("sysc",    24, 23, true, 10,  1, "The number of system calls.");
		md.addStatColumn("us",      25, 24, true, 10,  1, "The percentage of physical processor utilization that occurred while executing at the user level (application).");
		md.addStatColumn("sy",      26, 25, true, 10,  1, "The percentage of physical processor utilization that occurred while executing at the system level (kernel).");
		md.addStatColumn("wt",      27, 26, true, 10,  1, "The percentage of time that the logical processor was idle and it did not have an outstanding disk I/O request.");
		md.addStatColumn("id" ,     28, 27, true, 10,  1, "The percentage of time that the logical processor was idle during which it had an outstanding disk I/O request.");
		md.addStatColumn("pc",      29, 28, true, 10,  1, "The fraction of physical processor consumed. It is displayed in both shared partition and dedicated partition. For the default flag in the dedicated partition, it is not displayed when both donation and simultaneous multithreading are disabled. The pc of the cpuid U row represents the number of unused physical processors.");
		md.addStatColumn("ec",      30, 29, true, 10,  1, "The percentage of entitled capacity consumed by the logical CPU. The %ec of the ALL CPU row represents the percentage of entitled capacity consumed. Because the time base over which this data is computed can vary, the entitled capacity percentage can sometimes exceed 100%. This excess is noticeable only with small sampling intervals. The attribute is displayed only in a shared partition.");
		md.addStatColumn("ilcs",    31, 30, true, 10,  1, "The number of involuntary logical CPU context switches, displayed only in shared partition. For the -h and -a flags, it is also displayed in dedicated partition.");
		md.addStatColumn("vlcs",    32, 31, true, 10,  1, "The number of voluntary logical CPU context switches. Displayed only in shared partition. For the -h and -a flags, it is also displayed in dedicated partition.");
//		md.addStatColumn("lcs",     xx, xx, true, 10,  1, "The total number of logical CPU context switches. Displayed only in shared partition or when dedicated partition is donating.");
//		md.addStatColumn("idon",    xx, xx, true, 10,  1, "The percentage of physical processor utilization that occurs while explicitly donating idle cycles. Displayed only in dedicated partition that is donating.");
//		md.addStatColumn("bdon",    xx, xx, true, 10,  1, "The percentage of physical processor utilization that occurs while donating busy cycles. Displayed only in dedicated partition that is donating.");
//		md.addStatColumn("istol",   32, 31, true, 10,  1, "The percentage of physical processor utilization that occurs while the Hypervisor is stealing idle cycles. Displayed only in dedicated partition.");
//		md.addStatColumn("bstol",   33, 32, true, 10,  1, "The percentage of physical processor utilization that occurs while the Hypervisor is stealing busy cycles. Displayed only in dedicated partition.");
		md.addStatColumn("S3hrd",   33, 32, true, 10,  1, "The percentage of local thread dispatches on this logical processor.");
		md.addStatColumn("S4hrd",   34, 33, true, 10,  1, "The percentage of near thread dispatches on this logical processor.");
		md.addStatColumn("S5hrd",   35, 34, true, 10,  1, "The percentage of far thread dispatches on this logical processor.");
		md.addStatColumn("nsp",     36, 35, true, 10,  1, "The current average processor speed as a percentage of nominal speed. Displayed only if the hardware uses the SPURR.");
		
		// Set Percent columns
		md.setPercentCol("S0rd");
		md.setPercentCol("S1rd");
		md.setPercentCol("S2rd");
		md.setPercentCol("S3rd");
		md.setPercentCol("S4rd");
		md.setPercentCol("S5rd");

		md.setPercentCol("S3hrd");
		md.setPercentCol("S4hrd");
		md.setPercentCol("S5hrd");

		md.setPercentCol("us");
		md.setPercentCol("sy");
		md.setPercentCol("wt");
		md.setPercentCol("id");

//		md.setPercentCol("istol");
//		md.setPercentCol("bstol");

		// Use "cpu" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("cpu");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Skip the header line
		md.setSkipRows("cpu", "cpu");

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorMpstat.", Configuration.getCombinedConfiguration());

		// set positions of some columns, this will be used in parseRow() below, to replace '-' to '0.0'
		_pos_S0rd  = md.getParseColumnArrayPos("S0rd");
		_pos_S1rd  = md.getParseColumnArrayPos("S1rd");
		_pos_S2rd  = md.getParseColumnArrayPos("S2rd");
		_pos_S3rd  = md.getParseColumnArrayPos("S3rd");
		_pos_S4rd  = md.getParseColumnArrayPos("S4rd");
		_pos_S5rd  = md.getParseColumnArrayPos("S5rd");

		_pos_S3hrd = md.getParseColumnArrayPos("S3hrd");
		_pos_S4hrd = md.getParseColumnArrayPos("S4hrd");
		_pos_S5hrd = md.getParseColumnArrayPos("S5hrd");

		return md;
	}

	private int _pos_S0rd; // Can be '-', which is replaced with 0.0
	private int _pos_S1rd; // Can be '-', which is replaced with 0.0
	private int _pos_S2rd; // Can be '-', which is replaced with 0.0
	private int _pos_S3rd; // Can be '-', which is replaced with 0.0
	private int _pos_S4rd; // Can be '-', which is replaced with 0.0
	private int _pos_S5rd; // Can be '-', which is replaced with 0.0

	private int _pos_S3hrd; // Can be '-', which is replaced with 0.0
	private int _pos_S4hrd; // Can be '-', which is replaced with 0.0
	private int _pos_S5hrd; // Can be '-', which is replaced with 0.0
	
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

			if ("-".equals(preParsed[_pos_S0rd])) preParsed[_pos_S0rd] = "0.0";
			if ("-".equals(preParsed[_pos_S1rd])) preParsed[_pos_S1rd] = "0.0";
			if ("-".equals(preParsed[_pos_S2rd])) preParsed[_pos_S2rd] = "0.0";
			if ("-".equals(preParsed[_pos_S3rd])) preParsed[_pos_S3rd] = "0.0";
			if ("-".equals(preParsed[_pos_S4rd])) preParsed[_pos_S4rd] = "0.0";
			if ("-".equals(preParsed[_pos_S5rd])) preParsed[_pos_S5rd] = "0.0";

			if ("-".equals(preParsed[_pos_S5rd])) preParsed[_pos_S3hrd] = "0.0";
			if ("-".equals(preParsed[_pos_S5rd])) preParsed[_pos_S4hrd] = "0.0";
			if ("-".equals(preParsed[_pos_S5rd])) preParsed[_pos_S5hrd] = "0.0";
			
			return preParsed;
		}
		return null;
	}
}
