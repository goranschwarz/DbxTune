package com.asetune.hostmon;

import com.asetune.Version;
import com.asetune.utils.Configuration;

public class MonitorIoAix 
extends MonitorIo
{
	public String getModuleName()
	{
		return "MonitorIoAix";
	}

	@Override
	public String getCommand()
	{
		String cmd = super.getCommand();
		return cmd != null ? cmd : "iostat -Dl "+getSleepTime();

		// -R = Specifies that the reset of min* and max* values should happen at each interval. The default is to reset the values once when iostat is started. The -R flag can be specified only with the -D flag.
		//return cmd != null ? cmd : "iostat -DRl "+getSleepTime(); 
	}

	@Override
	public HostMonitorMetaData createMetaData()
	{
		HostMonitorMetaData md = new HostMonitorMetaData();
		md.setTableName(getModuleName());

		md.addStrColumn( "Disks",       1,  1, false,   30, "Disk device name");
		md.addIntColumn( "samples",     2,  0, true,        "Number of 'sub' sample entries of iostat this value is based on");

		md.addStatColumn("X_tm_act",    3,  2, true, 7,  1, "Indicates the percentage of time the physical disk or tape was active (bandwidth utilization for the drive).");
		md.addStatColumn("X_bps",       4,  3, true, 7,  1, "Indicates the amount of data transferred (read or written) per second to the drive. Note: in "+Version.getAppName()+" this is always presented in KB per second");
		md.addStatColumn("X_tps",       5,  4, true, 7,  1, "Indicates the number of transfers per second that were issued to the physical disk or tape. A transfer is an I/O request to the physical disk or tape. Multiple logical requests can be combined into a single I/O request to the disk. A transfer is of indeterminate size.");
		md.addStatColumn("X_bread",     6,  5, true, 7,  1, "Indicates the amount of data read per second, from the drive. Note: in "+Version.getAppName()+" this is always presented in KB per second");
		md.addStatColumn("X_bwrtn",     7,  6, true, 7,  1, "Indicates the amount of data written per second, to the drive. Note: in "+Version.getAppName()+" this is always presented in KB per second");

		md.addStatColumn("R_rps",       8,  7, true, 7,  1, "Indicates the number of read transfers per second.");
		md.addStatColumn("R_avgserv",   9,  8, true, 7,  1, "Indicates the average service time per read transfer. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("R_minserv",  10,  9, true, 7,  1, "Indicates the minimum read service time. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("R_maxserv",  11, 10, true, 7,  1, "Indicates the maximum read service time. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("R_timeouts", 12, 11, true, 7,  1, "Indicates the number of read timeouts per second.");
		md.addStatColumn("R_fails",    13, 12, true, 7,  1, "Indicates the number of failed read requests per second.");
		
		md.addStatColumn("W_wps",      14, 13, true, 7,  1, "Indicates the number of write transfers per second.");
		md.addStatColumn("W_avgserv",  15, 14, true, 7,  1, "Indicates the average service time per write transfer. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("W_minserv",  16, 15, true, 7,  1, "Indicates the minimum write service time. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("W_maxserv",  17, 16, true, 7,  1, "Indicates the maximum write service time. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("W_timeouts", 18, 17, true, 7,  1, "Indicates the number of write timeouts per second.");
		md.addStatColumn("W_fails",    19, 18, true, 7,  1, "Indicates the number of failed write requests per second.");

		md.addStatColumn("Q_avgtime",  20, 19, true, 7,  1, "Indicates the average time spent by a transfer request in the wait queue. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("Q_mintime",  21, 20, true, 7,  1, "Indicates the minimum time spent by a transfer request in the wait queue. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("Q_maxtime",  22, 21, true, 7,  1, "Indicates the maximum time spent by a transfer request in the wait queue. Note: in "+Version.getAppName()+" this is always presented in in milliseconds.");
		md.addStatColumn("Q_avgwqsz",  23, 22, true, 7,  1, "Indicates the average wait queue size.");
		md.addStatColumn("Q_avgsqsz",  24, 23, true, 7,  1, "Indicates the average service queue size.");
		md.addStatColumn("Q_sqfull",   25, 24, true, 7,  1, "Indicates the number of times the service queue becomes full (that is, the disk is not accepting any more service requests) per second.");

		// Use "Disks" as the Primary Key, which is used to du summary/average calculations
		md.setPkCol("Disks");

		// Set column "samples", to a special status, which will contain number of 
		// underlying samples the summary/average caclulation was based on
		md.setStatusCol("samples",    HostMonitorMetaData.STATUS_COL_SUB_SAMPLE);

		// Set Percent columns
		md.setPercentCol("X_tm_act");
		
		// What regexp to use to split the input row into individual fields
		md.setParseRegexp(HostMonitorMetaData.REGEXP_IS_SPACE);

		// Get SKIP and ALLOW from the Configuration
		md.setSkipAndAllowRows(null, Configuration.getCombinedConfiguration());
		md.setSkipAndAllowRows("hostmon.MonitorIo.", Configuration.getCombinedConfiguration());

		// set positions of some columns, this will be used in parseRow() below, to replace 'suffixes' into real values
		_pos_X_bps     = md.getParseColumnArrayPos("X_bps");
		_pos_X_bread   = md.getParseColumnArrayPos("X_bread");
		_pos_X_bwrtn   = md.getParseColumnArrayPos("X_bwrtn");

		_pos_R_avgserv = md.getParseColumnArrayPos("R_avgserv");
		_pos_R_minserv = md.getParseColumnArrayPos("R_minserv");
		_pos_R_maxserv = md.getParseColumnArrayPos("R_maxserv");

		_pos_W_avgserv = md.getParseColumnArrayPos("W_avgserv");
		_pos_W_minserv = md.getParseColumnArrayPos("W_minserv");
		_pos_W_maxserv = md.getParseColumnArrayPos("W_maxserv");

		_pos_Q_avgtime = md.getParseColumnArrayPos("Q_avgtime");
		_pos_Q_mintime = md.getParseColumnArrayPos("Q_mintime");
		_pos_Q_maxtime = md.getParseColumnArrayPos("Q_maxtime");

		return md;
	}

	private boolean _skipEmptyBpsSamples = true;

	private int _pos_X_bps;    // Can be suffixed with: K, M, G, T : which indicated the size in KB etc
	private int _pos_X_bread;  // Can be suffixed with: K, M, G, T : which indicated the size in KB etc
	private int _pos_X_bwrtn;  // Can be suffixed with: K, M, G, T : which indicated the size in KB etc
	
	private int _pos_R_avgserv; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	private int _pos_R_minserv; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	private int _pos_R_maxserv; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	
	private int _pos_W_avgserv; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	private int _pos_W_minserv; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	private int _pos_W_maxserv; // Can be suffixed with: S, H : which indicated the time in Sec or Hour

	private int _pos_Q_avgtime; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	private int _pos_Q_mintime; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	private int _pos_Q_maxtime; // Can be suffixed with: S, H : which indicated the time in Sec or Hour
	
	@Override
	public String[] parseRow(HostMonitorMetaData md, String row, String[] preParsed, int type)
	{
		// skip rows that are of the faulty size
		if ( preParsed.length == md.getParseColumnCount() )
		{
			// Skip header 
			if (skipRow(md, row, preParsed, type))
				return null;

			// now check the ALLOWED rows
			// If NO allow entries are installed, then true will be returned.
			if ( ! allowRow(md, row, preParsed, type))
				return null;

			if (_skipEmptyBpsSamples)
			{
				if ("0.0".equals(preParsed[_pos_X_bps]))
					return null;
			}

			// some columns can end with K, M, G, T : which indicated the size in KB etc
			preParsed[_pos_X_bps]   = recalcSuffixesValues(preParsed[_pos_X_bps]);
			preParsed[_pos_X_bread] = recalcSuffixesValues(preParsed[_pos_X_bread]);
			preParsed[_pos_X_bwrtn] = recalcSuffixesValues(preParsed[_pos_X_bwrtn]);
	
			preParsed[_pos_R_avgserv] = recalcSuffixesValues(preParsed[_pos_R_avgserv]);
			preParsed[_pos_R_minserv] = recalcSuffixesValues(preParsed[_pos_R_minserv]);
			preParsed[_pos_R_maxserv] = recalcSuffixesValues(preParsed[_pos_R_maxserv]);
	
			preParsed[_pos_W_avgserv] = recalcSuffixesValues(preParsed[_pos_W_avgserv]);
			preParsed[_pos_W_minserv] = recalcSuffixesValues(preParsed[_pos_W_minserv]);
			preParsed[_pos_W_maxserv] = recalcSuffixesValues(preParsed[_pos_W_maxserv]);
	
			preParsed[_pos_Q_avgtime] = recalcSuffixesValues(preParsed[_pos_Q_avgtime]);
			preParsed[_pos_Q_mintime] = recalcSuffixesValues(preParsed[_pos_Q_mintime]);
			preParsed[_pos_Q_maxtime] = recalcSuffixesValues(preParsed[_pos_Q_maxtime]);
	
			return preParsed;
		}
		return null;
	}

	/**
	 * Recalculate suffix characters into a FIXED value<br>
	 * The fixed value for M, G, T is in KB<br>
	 * The fixed value for S, H is in Milliseconds<br>
	 * @param str
	 * @return
	 */
	private String recalcSuffixesValues(String str)
	{
		String lastChar = str.substring(str.length()-1);    // Just the suffixes
		String numStr   = str.substring(0, str.length()-1); // The string without the suffix

		if      ("K".equals(lastChar)) return numStr; // just return the value without the K 
		else if ("M".equals(lastChar)) return Double.toString( Double.parseDouble(numStr) * 1000); // 1 000 Kb
		else if ("G".equals(lastChar)) return Double.toString( Double.parseDouble(numStr) * 1000000); // 1 000 000 Kb
		else if ("T".equals(lastChar)) return Double.toString( Double.parseDouble(numStr) * 1000000000); // 1 000 000 000 Kb
		else if ("S".equals(lastChar)) return Double.toString( Double.parseDouble(numStr) * 1000); // 1000 ms is 1 second
		else if ("H".equals(lastChar)) return Double.toString( Double.parseDouble(numStr) * 1000 * 3699); // 1000 ms is 1 second and 3600 seconds is 1 hour
		else return str;
	}

	
}
