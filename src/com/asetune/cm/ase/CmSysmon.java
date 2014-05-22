package com.asetune.cm.ase;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmSysmonPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSysmon
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSysmon.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSysmon.class.getSimpleName();
	public static final String   SHORT_NAME       = "Sysmon Raw";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>Just grabs the raw counters used in sp_sysmon.</p>" +
		"NOTE: reuses data from 'Spinlock Sum', so this needs to be running as well.<br>" +
		"For the moment consider this as <b>very experimental</b>." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysmonitors"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"value"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 300;
	public static final int      DEFAULT_QUERY_TIMEOUT          = 30;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.ALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmSysmon(counterController, guiController);
	}

	public CmSysmon(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		addDependsOnCm(CmSpinlockSum.CM_NAME); // CMspinlockSum must have been executed before this cm

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSysmonPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("instanceid");

		pkCols.add("field_name");
		pkCols.add("group_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String optGoalPlan = "";
//		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
		if (aseVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		String discardSpinlocks = "group_name not in ('spinlock_p_0', 'spinlock_w_0', 'spinlock_s_0')";
//		if (aseVersion >= 15700)
//		if (aseVersion >= 1570000)
		if (aseVersion >= Ver.ver(15,7))
			discardSpinlocks = "group_name not in ('spinlock_p', 'spinlock_w', 'spinlock_s')";
		
		String sql =   
			"SELECT \n" +
			(isClusterEnabled ? "instanceid, \n" : "") +
			"  field_name = convert(varchar(100),field_name), \n" +
			"  group_name = convert(varchar(30),group_name), \n" +
			"  field_id, \n" +
			"  value, \n" +
			"  description  = convert(varchar(255), description) \n" +
			"FROM master..sysmonitors \n" +
			"WHERE " + discardSpinlocks + " \n" +
			"  AND value != 0 \n" +
//			"  AND value > 100 \n" +
			"  OR  group_name = 'config' \n" +
			"ORDER BY group_name, field_name" + (isClusterEnabled ? ", instanceid" : "") + "\n" +
			optGoalPlan;

		
		// ASE Devices
		String vdevno = "d.vdevno";
		if (aseVersion < Ver.ver(15,0))
			vdevno = "convert(int, d.low/power(2,24))";

		String sqlDevices = 
			"\n" +
			"------- Append device information \n" +
			"select \n" +
			"  field_name  = d.name, \n" +
			"  group_name  = 'ase-device-info', \n" +
			"  field_id    = "+vdevno+", \n" +
			"  value       = "+vdevno+", \n" +
			"  description = d.phyname \n" +
			"from master.dbo.sysdevices d \n" +
			"where d.cntrltype = 0 \n" +
			"";

		// @@kernelmode
		String kernelmode = "convert(varchar(255), 'process')";
		if (aseVersion >= Ver.ver(15,7))
			kernelmode = "@@kernelmode";

		String sqlGlobalVariableKernelmode =
			"\n" +
			"------- Append some global variables information \n" +
			"select \n" +
			"  field_name  = convert(varchar(100), '@@kernelmode'), \n" +
			"  group_name  = 'ase-global-var', \n" +
			"  field_id    = convert(int, 0), \n" +
			"  value       = convert(int, 0), \n" +
			"  description = "+kernelmode+" \n" +
			"";
				
		return 
			sql + 
			sqlDevices + 
			sqlGlobalVariableKernelmode;
	}

	

/*
 * below: try to figure out if isCountersCleared is valid or not
 * 
 * we might be able to figure out if "isCountersCleared" is on this counter set...
 *     DBCC monitor('clear', 'spinlock_s', 'on') -- done by: sp_sysmon, spinmon, and CmSpinlockSum(if reset is enabled)
 *     DBCC monitor('clear', 'all',        'on') -- done by: sp_sysmon
 *     
 * it might be done by checking if taking a known counter that is *constantly* updated:
 * Candidates for: DBCC monitor('clear', 'all', 'on')
 *     group_name='access', field_name='mon_lesubst' or group_name='access', field_name='scan_copy_bytes' or group_name='access', field_name='scan_getnext'
 *
 * Candidates for: DBCC monitor('clear', 'spinlock_s', 'on')
 *     group_name='spinlock_s', field_name='default data cache' 
 *     
 * is *lower" in the newSample, then we know it's a reset
 */


//------------------------------------
// Below is some way of converting the signed int to "unsigned int" but within a "long" instead of int
// By doing this a negative diff result always means it's a counter "reset" 
// Below code isn't used for the moment, but lets save it for the moment...
//------------------------------------
// idea: bump up negative counters to "unsigned int", which makes resets detection easier
//       but lets just do it for ONE predictive counter...
//------------------------------------
//	private long _oldSample_unsigned_kernel_wakeupCalls = 0;
//	private long _newSample_unsigned_kernel_wakeupCalls = 0;
//
//	private void detectCounterResetHelper(SamplingCnt oldSample, SamplingCnt newSample)
//	{
//		// PK: [instanceid,] field_name, group_name
//		String key = "wakeup_calls:kernel"; // TODO: if ASE-CE we should try to get in "instance" as the first part of the key
//
//		Object oldVal = oldSample.getRowValue(key, "value");
//		Object newVal = newSample.getRowValue(key, "value");
//
//		if (oldVal != null && oldVal instanceof Number)
//		{
//			int value = ((Number)oldVal).intValue();
//			_oldSample_unsigned_kernel_wakeupCalls = (value < 0) ? Integer.MAX_VALUE + (value - Integer.MIN_VALUE) : value;
//		}		
//
//		if (newVal != null && newVal instanceof Number)
//		{
//			int value = ((Number)newVal).intValue();
//			_newSample_unsigned_kernel_wakeupCalls = (value < 0) ? Integer.MAX_VALUE + (value - Integer.MIN_VALUE) : value;
//		}
//System.out.println(getName()+":computeDiffCnt->detectCounterResetHelper PK='"+key+"', _oldSample_unsigned_kernel_wakeupCalls='"+_oldSample_unsigned_kernel_wakeupCalls+"', _newSample_unsigned_kernel_wakeupCalls='"+_newSample_unsigned_kernel_wakeupCalls+"'.");
//	}
	
	/**
	 * if counters has been reset outside, shorten the sample interval
	 */
	@Override
	public SamplingCnt computeDiffCnt(SamplingCnt oldSample, SamplingCnt newSample, List<Integer> deletedRows, List<String> pkCols, boolean[] isDiffCol, boolean isCountersCleared)
	{
//		detectCounterResetHelper(oldSample, newSample);

		// Sanity check if "isCountersCleared" is valid...
		// get a known value we know will increment all the time from old/new value and compare
		if (isCountersCleared)
		{
			// PK: [instanceid,] field_name, group_name
			String key = "wakeup_calls:kernel"; // TODO: if ASE-CE we should try to get in "instance" as the first part of the key

			Object oldVal = oldSample.getRowValue(key, "value");
			Object newVal = newSample.getRowValue(key, "value");

			if (oldVal != null && newVal != null)
			{
				if (oldVal instanceof Number && newVal instanceof Number)
				{
					// Note this is "signed int" can be negative... Then the below wont work
					// ... but the key "wakeup_calls:kernel" isn't *heavily* incremented, so it will hopefully NOT WRAP and go to negative
					// so lets do a ordinary difference calculation like we do in diffColumnValue(...)
					// if the diff calc is negative, then it's a reset.
					int diffColVal = ((Number)newVal).intValue() - ((Number)oldVal).intValue();
					isCountersCleared =  diffColVal < 0;

					// Vague way of finding out negative counter followed by a reset
					if ( ((Number)newVal).intValue() >= 0 && ((Number)oldVal).intValue() < -10000)
						isCountersCleared =  true;

					// should we set/reset the CM isCountersCleared or not...
					//setIsCountersCleared(isCountersCleared);

					_logger.debug(getName()+":computeDiffCnt(): CountersClearedCheck(after) PK='"+key+"', oldVal='"+oldVal+"', newVal='"+newVal+"', isCountersCleared="+isCountersCleared+".");
				}
			}
// If we use detectCounterResetHelper(...) this code is relevant, and can be used instead of above code
//			// This will work here since "value" is converted to a unsigned int
//			isCountersCleared =  _newSample_unsigned_kernel_wakeupCalls < _oldSample_unsigned_kernel_wakeupCalls;
//
//			_logger.debug(getName()+":computeDiffCnt(): CountersClearedCheck(after) PK='wakeup_calls:kernel', _oldSample_unsigned_kernel_wakeupCalls='"+_oldSample_unsigned_kernel_wakeupCalls+"', _newSample_unsigned_kernel_wakeupCalls='"+_newSample_unsigned_kernel_wakeupCalls+"', isCountersCleared="+isCountersCleared+".");
		}

		// Let super do all the work
		SamplingCnt diff = super.computeDiffCnt(oldSample, newSample, deletedRows, pkCols, isDiffCol, isCountersCleared);
		
		// Adjust interval (make it shorter) if counters has been cleared
		if (isCountersCleared && getCounterClearTime() != null)
		{
			long interval = getSampleTime().getTime() - getCounterClearTime().getTime();
			newSample.setSampleInterval(interval);
			diff     .setSampleInterval(interval);
			//rate will grab the interval from diff
	
			setSampleInterval(interval);
		}

		return diff;
	}

	/**
	 * if counters has been reset outside, just return the new value...<br>
	 * No need to handle special case with "wrapped" unsigned integers in here (like we do in CmSpinlockSum) since we don't upgrade signed integers to higher data types. 
	 */
	@Override
	protected Number diffColumnValue(Number prevColVal, Number newColVal, boolean negativeDiffCountersToZero, String counterSetName, String colName, boolean isCountersCleared)
	{
		// If counters has been cleared, simply return the new value... no need to do diff calculation, and if we do they will be wrong...
		if (isCountersCleared)
			return newColVal;

		return super.diffColumnValue(prevColVal, newColVal, negativeDiffCountersToZero, counterSetName, colName, isCountersCleared);
	}
}
