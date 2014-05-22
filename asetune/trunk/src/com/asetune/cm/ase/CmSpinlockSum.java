package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CmSybMessageHandler;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmSpinlockSumPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.utils.Configuration;
import com.asetune.utils.StringUtil;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpinlockSum
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSpinlockSum.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpinlockSum.class.getSimpleName();
	public static final String   SHORT_NAME       = "Spinlock Sum";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>What spinlocks do we have contention on.</p>" +
		"This could be a bit heavy to use when there is a 'low' refresh interval.<br>" +
		"For the moment consider this as <b>experimental</b>." +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"sysmonitors"};
	public static final String[] NEED_ROLES       = new String[] {"sa_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"contention"};
	public static final String[] DIFF_COLUMNS     = new String[] {"grabs", "waits", "spins"};

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

		return new CmSpinlockSum(counterController, guiController);
	}

	public CmSpinlockSum(ICounterController counterController, IGuiController guiController)
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

		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_resetAfter          = PROP_PREFIX + ".sample.reset.after";
	public static final boolean DEFAULT_sample_resetAfter          = false;

	public static final String  PROPKEY_sample_fglockspins          = PROP_PREFIX + ".sample.fglockspins";
	public static final boolean DEFAULT_sample_fglockspins          = false;

	@Override
	protected void registerDefaultValues()
	{
		super.registerDefaultValues();

		Configuration.registerDefaultValue(PROPKEY_sample_resetAfter,  DEFAULT_sample_resetAfter);
		Configuration.registerDefaultValue(PROPKEY_sample_fglockspins, DEFAULT_sample_fglockspins);
	}

	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpinlockSumPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	protected CmSybMessageHandler createSybMessageHandler()
	{
		CmSybMessageHandler msgHandler = super.createSybMessageHandler();

		// in some ASE 15.5 ESD#4 versions message 'spin_type, spin_name' is thrown...
		// this in conjunction when dbcc traceon(3604) has been enabled.
		// I dont know why this is happening, most likly a bug...
		// So just discard this message
		msgHandler.addDiscardMsgStr("spin_type, spin_name");

		return msgHandler;
	}


	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("instanceid");

		pkCols.add("spinName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean sample_resetAfter = conf.getBooleanProperty(PROPKEY_sample_resetAfter, DEFAULT_sample_resetAfter);

		// sum(int) may cause: "Arithmetic overflow occurred"
		// max int is: 2147483647, so if we sum several rows we may overflow the integer
		// so on pre 15.0 use numeric instead, over 15.0 use bigint
		String datatype    = "numeric(19,0)"; // 19.0 is unsigned bigint, so lets use that... 
		String optGoalPlan = "";

//		if (aseVersion >= 15000)
//		if (aseVersion >= 1500000)
		if (aseVersion >= Ver.ver(15,0))
		{
			datatype    = "bigint";
		}
//		if (aseVersion >= 15020)
//		if (aseVersion >= 1502000)
		if (aseVersion >= Ver.ver(15,0,2))
		{
			optGoalPlan = "plan '(use optgoal allrows_dss)' \n";
		}

		/*
		 * Retrieve the spinlocks. There are three spinlock counters collected by dbcc monitor:
		 *	- spinlock_p_0 -> Spinlock "grabs" - as in attempted grabs for the spinlock - includes waits
		 *	- spinlock_w_0 -> Spinlock "waits" - usually a good sign of contention
		 *	- spinlock_s_0 -> Spinlock "spins" - this is the CPU spins that drives up CPU utilization
		 *	                  The higher the spin count, the more CPU usage and the more serious
		 *	                  the performance impact of the spinlock on other processes not waiting
		 */
		String preDropTmpTables =
			"\n " +
			"/*------ drop tempdb objects if we failed doing that in previous execution -------*/ \n" +
			"if ((select object_id('#spin_names'))   is not null) drop table #spin_names \n" +
			"if ((select object_id('#sysmonitorsP')) is not null) drop table #sysmonitorsP \n" +
			"if ((select object_id('#sysmonitorsW')) is not null) drop table #sysmonitorsW \n" +
			"if ((select object_id('#sysmonitorsS')) is not null) drop table #sysmonitorsS \n" +
			"go \n" +
			"\n";
		
		String sqlCreateTmpTabCache = 
			"\n " +
			"/*------ temp table to hold 'cache names' and other 'named' stuff -------*/ \n" +
			"create table #spin_names     \n " +
			"(                            \n " +
			"  spin_name  varchar(50),    \n " +
			"  spin_type  varchar(20),    \n " +
			"  spin_desc  varchar(100),   \n " +
			"  start_id   int,            \n " +
			"  instanceid tinyint         /* if ASE SMP, this will always be value 1 */\n " +
			")                            \n " +
			"\n" +
			"/*------ DATA CACHES -------*/ \n" +
			"insert into #spin_names(spin_name, spin_type, spin_desc, start_id, instanceid) \n" +
			"select comment, 'CACHELET', 'Data cache, spinlock instance', 0, " + (isClusterEnabled ? "instanceid" : "1") + " \n" + 
			"  from master..syscurconfigs \n" +
			" where config=19 and value > 0 \n" +
			"\n" +
			"/*------ SPINLOCK INSTANCES, for other stuff -------*/ \n" +
			"insert into #spin_names(spin_name, spin_type, spin_desc, start_id, instanceid) \n" +
			"select 'Kernel->erunqspinlock', 'KERNEL-INST', 'Engine Runqueue, spinlock instance', 0, " + (isClusterEnabled ? "instanceid" : "1") + " \n" +
			"  from master..syscurconfigs \n" + 
			" where config=19 and value > 0 \n" +
			"   and comment = 'default data cache' /* if ASE CE, then this will produce number of Cluster Instances, ASE SMP = 1 row */ \n" + 
			"\n";

		String sqlUpdateTmpTabCache = 
			"/*------ SET start_id -------*/ \n" +
			"update #spin_names \n" +
			"   set start_id = isnull((select min(M.field_id) \n" +
			"                          from #sysmonitorsP M\n" +
			"                          where #spin_names.spin_name  = M.field_name \n" +
			"                            and #spin_names.instanceid = " + (isClusterEnabled ? "M.instanceid" : "1") + ") \n" +
			"                  ,start_id) \n";

		String sqlDropTmpTabCache = 
			"\n" +
			"drop table #spin_names \n";


		String instanceid = ""; // If cluster, we need to use column 'instanceid' as well
		if (isClusterEnabled) 
			instanceid = ", instanceid";


		// If ASE-CE, we might want to exclude some fields, for example 'fglockspins' has more than 100.000 records...
		String restrictTmpSysmonitorsWhere = "";
		if (isClusterEnabled)
		{
			String disregardFields = "";
			if (conf.getBooleanProperty(PROPKEY_sample_fglockspins, DEFAULT_sample_fglockspins) == false)
				disregardFields += "'fglockspins', ";
			
			if ( ! disregardFields.equals(""))
			{
				disregardFields = StringUtil.removeLastComma(disregardFields);
				restrictTmpSysmonitorsWhere = " and field_name not in("+disregardFields+")";
			}
		}

		// if 12.5.x: group_name = 'spinlock_p_0' or 'spinlock_w_0' or 'spinlock_s_0'  
		// if 15.7.x: group_name = 'spinlock_p'   or 'spinlock_w'   or 'spinlock_s'  
		String spinPostfix = "_0";
		if (aseVersion >= Ver.ver(15,7))
			spinPostfix = "";
		
		String sqlCreateTmpSysmonitors = 
			"/*------ Copy 'spinlock_[p|w|s]' rows to local tempdb, this reduces IO in joins below -------*/ \n" +
			"/*------ Deal with overflow by bumping up to a higher datatype: and adding the negative delta on top of Integer.MAX_VALUE -------*/ \n" +
			"declare @int_max "+datatype+"    set @int_max =  2147483647 \n" +
			"declare @int_min "+datatype+"    set @int_min = -2147483648 \n" +
			"select field_name=convert(varchar(79),field_name), field_id, value = CASE WHEN (value < 0) THEN @int_max + (value - @int_min) ELSE convert("+datatype+", value) END "+instanceid+" into #sysmonitorsP FROM master..sysmonitors WHERE group_name = 'spinlock_p"+spinPostfix+"' "+restrictTmpSysmonitorsWhere+" \n" +
			"select field_name=convert(varchar(79),field_name), field_id, value = CASE WHEN (value < 0) THEN @int_max + (value - @int_min) ELSE convert("+datatype+", value) END "+instanceid+" into #sysmonitorsW FROM master..sysmonitors WHERE group_name = 'spinlock_w"+spinPostfix+"' "+restrictTmpSysmonitorsWhere+" \n" +
			"select field_name=convert(varchar(79),field_name), field_id, value = CASE WHEN (value < 0) THEN @int_max + (value - @int_min) ELSE convert("+datatype+", value) END "+instanceid+" into #sysmonitorsS FROM master..sysmonitors WHERE group_name = 'spinlock_s"+spinPostfix+"' "+restrictTmpSysmonitorsWhere+" \n";
		sqlCreateTmpSysmonitors += 
			"-- A 'go' here will make the second batch optimize better \n" +
			"go \n";
		
		String sqlDropTmpTabPWS   = 
			"\n" +
			"drop table #sysmonitorsP \n"+
			"drop table #sysmonitorsW \n" +
			"drop table #sysmonitorsS \n";
		
		String sqlResetCountersAfterSample = "";
		if (sample_resetAfter)
		{
			sqlResetCountersAfterSample = 
				" \n" +
				"/*------ RESET THE in-memory monitor counters (just for spinlocks) -------*/ \n" +
				"DBCC monitor('clear', 'spinlock_s', 'on') \n";
		}

		String sqlSampleSpins =
			"/*------ SAMPLE THE in-memory monitor counters into table master..sysmonitors -------*/ \n" +
			"DBCC monitor('sample', 'all',        'on') \n" +
			"DBCC monitor('sample', 'spinlock_s', 'on') \n" +
			"\n" +
			sqlCreateTmpSysmonitors +
			sqlResetCountersAfterSample +
			" \n" +
			sqlUpdateTmpTabCache +
			" \n" +
			"/*------ get data: for all spinlocks -------*/ \n" +
			"SELECT \n" +
			"  type         = \n" +
			"    CASE \n" +
			"      WHEN P.field_name like 'Dbtable->%'  THEN convert(varchar(20), 'DBTABLE')  \n" +
			"      WHEN P.field_name like 'Dbt->%'      THEN convert(varchar(20), 'DBTABLE')  \n" +
			"      WHEN P.field_name like 'Dbtable.%'   THEN convert(varchar(20), 'DBTABLE')  \n" +
			"      WHEN P.field_name like 'Resource->%' THEN convert(varchar(20), 'RESOURCE') \n" +
			"      WHEN P.field_name like 'Kernel->%'   THEN convert(varchar(20), 'KERNEL')   \n" +
			"      WHEN P.field_name in (select spin_name from #spin_names where spin_type = 'CACHELET')   \n" +
			"                                           THEN convert(varchar(20), 'CACHE') \n" +
			"      ELSE convert(varchar(20), 'OTHER')  \n" +
			"    END, \n" +
			(isClusterEnabled ? "P.instanceid, \n" : "") +
			"  spinName     = convert(varchar(50), P.field_name), \n" +
			"  instances    = count(P.field_id), \n" +
			"  grabs        = sum(convert("+datatype+", P.value)), \n" +
			"  waits        = sum(convert("+datatype+", W.value)), \n" +
			"  spins        = sum(convert("+datatype+", S.value)), \n" +
			"  contention   = convert(numeric(4,1), null), \n" +
			"  spinsPerWait = convert(numeric(12,1), null), \n" +
			"  description  = convert(varchar(100), '') \n" +
			"FROM #sysmonitorsP P, #sysmonitorsW W, #sysmonitorsS S \n" +
			"WHERE P.field_id   = W.field_id   \n" +
			"  AND P.field_name = W.field_name \n" +
			"  AND P.field_id   = S.field_id   \n" +
			"  AND P.field_name = S.field_name \n";
		if (isClusterEnabled) 
		{
			sqlSampleSpins +=
			"  AND P.instanceid = W.instanceid \n" +
			"  AND P.instanceid = S.instanceid \n" +
			"GROUP BY P.instanceid, P.field_name \n" +
			"ORDER BY 3, 2 \n" +
			optGoalPlan;
		}
		else 
		{
			sqlSampleSpins +=
			"GROUP BY P.field_name \n" +
			"ORDER BY 2 \n" +
			optGoalPlan;
		}

		//---------------------------------------------
		// For CACHES and other FINER GRANULARITY spinlocks
		// do the calculation on EACH Cache partition or spinlock instance
		//---------------------------------------------
		String sqlForCaches =
			"\n" +
			"/*------ get data: For some selected spinlocks with multiple instances -------*/ \n" +
			"SELECT \n" + 
			"  type         = N.spin_type, \n" +
			(isClusterEnabled ? "P.instanceid, \n" : "") +
			"  spinName     = convert(varchar(50), convert(varchar(40),P.field_name) + ' # ' + convert(varchar(5), P.field_id-N.start_id)), \n" +
			"  instances    = convert(int,1), \n" +
			"  grabs        = convert("+datatype+", P.value), \n" +
			"  waits        = convert("+datatype+", W.value), \n" +
			"  spins        = convert("+datatype+", S.value), \n" +
			"  contention   = convert(numeric(4,1), null), \n" +
			"  spinsPerWait = convert(numeric(12,1), null), \n" +
			"  description  = N.spin_desc \n" +
			"FROM #sysmonitorsP P, #sysmonitorsW W, #sysmonitorsS S, #spin_names N \n" +
			"WHERE P.field_id   = W.field_id   \n" +
			"  AND P.field_name = W.field_name \n" +
			"  AND P.field_id   = S.field_id   \n" +
			"  AND P.field_name = S.field_name \n" +
		    "  AND P.field_name = N.spin_name \n";
		if (isClusterEnabled) 
		{
			sqlForCaches +=
			"  AND P.instanceid = W.instanceid \n" +
			"  AND P.instanceid = S.instanceid \n" +
		    "  AND P.instanceid = N.instanceid \n" +
			"ORDER BY 3, 2 \n" +
			optGoalPlan;
		}
		else 
		{
			sqlForCaches +=
			"  AND N.instanceid = 1 \n" +
			"ORDER BY 2 \n" +
			optGoalPlan;
		}

		String sql = preDropTmpTables + sqlCreateTmpTabCache + sqlSampleSpins + sqlForCaches + sqlDropTmpTabCache + sqlDropTmpTabPWS;

		return sql;
	}

	@Override
	public String getSqlInitForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		//---------------------------------------------
		// SQL INIT (executed first time only)
		// AFTER 12.5.2 traceon(8399) is no longer needed
		//---------------------------------------------
		String sqlInit = "DBCC traceon(3604) \n";

//		if (aseVersion < 12520)
//		if (aseVersion < 1252000)
		if (aseVersion < Ver.ver(12,5,2))
			sqlInit += "DBCC traceon(8399) \n";

//		if (aseVersion >= 15020 || (aseVersion >= 12541 && aseVersion < 15000) )
//		if (aseVersion >= 1502000 || (aseVersion >= 1254010 && aseVersion < 1500000) )
		if (aseVersion >= Ver.ver(15,0,2) || (aseVersion >= Ver.ver(12,5,4,1) && aseVersion < Ver.ver(15,0)) )
		{
			sqlInit = "set switch on 3604 with no_info \n";
		}

		sqlInit += "DBCC monitor('select', 'all',        'on') \n" +
		           "DBCC monitor('select', 'spinlock_s', 'on') \n";

		return sqlInit;
	}
	@Override
	public String getSqlCloseForVersion(Connection conn, int srvVersion, boolean isClusterEnabled) 
	{
		String sqlClose = 
			"--DBCC monitor('select', 'all',        'off') \n" +
			"--DBCC monitor('select', 'spinlock_s', 'off') \n";
		return sqlClose;
	};

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public Configuration getLocalConfiguration()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		Configuration lc = new Configuration();

		lc.setProperty(PROPKEY_sample_resetAfter,  conf.getBooleanProperty(PROPKEY_sample_resetAfter,  DEFAULT_sample_resetAfter));
		
		if (isClusterEnabled())
		{
		lc.setProperty(PROPKEY_sample_fglockspins, conf.getBooleanProperty(PROPKEY_sample_fglockspins, DEFAULT_sample_fglockspins));
		}
		
		return lc;
	}

	@Override
	public String getLocalConfigurationDescription(String propName)
	{
		if (propName.equals(PROPKEY_sample_resetAfter))  return CmSpinlockSumPanel.TOOLTIP_sample_resetAfter;
		if (propName.equals(PROPKEY_sample_fglockspins)) return CmSpinlockSumPanel.TOOLTIP_sample_fglockspins;
	
		return "";
	}
	@Override
	public String getLocalConfigurationDataType(String propName)
	{
		if (propName.equals(PROPKEY_sample_resetAfter))  return Boolean.class.getSimpleName();
		if (propName.equals(PROPKEY_sample_fglockspins)) return Boolean.class.getSimpleName();

		return "";
	}

	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		MonTablesDictionary mtd = MonTablesDictionary.getInstance();

		long grabs,        waits,        spins;
		int  grabsId = -1, waitsId = -1, spinsId = -1, contentionId = -1, spinsPerWaitId = -1;
		int  pos_name = -1,  pos_desc = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("grabs"))        grabsId        = colId;
			else if (colName.equals("waits"))        waitsId        = colId;
			else if (colName.equals("spins"))        spinsId        = colId;
			else if (colName.equals("contention"))   contentionId   = colId;
			else if (colName.equals("spinsPerWait")) spinsPerWaitId = colId;
			else if (colName.equals("spinName"))     pos_name       = colId;
			else if (colName.equals("description"))  pos_desc       = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			grabs = ((Number) diffData.getValueAt(rowId, grabsId)).longValue();
			waits = ((Number) diffData.getValueAt(rowId, waitsId)).longValue();
			spins = ((Number) diffData.getValueAt(rowId, spinsId)).longValue();

			// contention
			if (grabs > 0)
			{
				BigDecimal contention = new BigDecimal( ((1.0 * (waits)) / grabs) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				// Keep only 3 decimals
				// row.set(AvgServ_msId, new Double (AvgServ_ms/1000) );
				diffData.setValueAt(contention, rowId, contentionId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, contentionId);

			// spinsPerWait
			if (waits > 0)
			{
				BigDecimal spinWarning = new BigDecimal( ((1.0 * (spins)) / waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(spinWarning, rowId, spinsPerWaitId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, spinsPerWaitId);

			// set description
			if (mtd != null && pos_name >= 0 && pos_desc >= 0)
			{
				Object o = diffData.getValueAt(rowId, pos_name);

				if (o instanceof String)
				{
					String name = (String)o;

					String desc = mtd.getSpinlockDescription(name);
					if (desc != null)
					{
						newSample.setValueAt(desc, rowId, pos_desc);
						diffData .setValueAt(desc, rowId, pos_desc);
					}
				}
			}
		}
	}

	/**
	 * if counters has been reset outside, shorten the sample interval
	 */
	@Override
	public SamplingCnt computeDiffCnt(SamplingCnt oldSample, SamplingCnt newSample, List<Integer> deletedRows, List<String> pkCols, boolean[] isDiffCol, boolean isCountersCleared)
	{
		// Sanity check if "isCountersCleared" is valid...
		// get a known value we know will increment all the time from old/new value and compare
		if (isCountersCleared)
		{
			// PK: spinName
			String key = "default data cache"; // TODO: if ASE-CE we should try to get in "instance" as the first part of the key

			Object oldVal = oldSample.getRowValue(key, "grabs");
			Object newVal = newSample.getRowValue(key, "grabs");

			if (oldVal != null && newVal != null)
			{
				if (oldVal instanceof Number && newVal instanceof Number)
				{
					// This will work here since "value" is converted to a unsigned int
					isCountersCleared =  ((Number)newVal).longValue() < ((Number)oldVal).longValue();
					
					// should we set/reset the CM isCountersCleared or not...
					//setIsCountersCleared(isCountersCleared);

					_logger.debug(getName()+":computeDiffCnt(): CountersClearedCheck(after) PK='"+key+"', oldVal='"+oldVal+"', newVal='"+newVal+"', isCountersCleared="+isCountersCleared+".");
				}
			}
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
	 * called by CounterModel.computeDiffCnt() to calculate a specific "cell" 
	 * <p>
	 * in ASE 12.5.x datatype will be numeric(19,0), which in java becomes BigDecimal<br>
	 * in ASE >= 15  datatype will be bigint,        which in java becomes Long<br>
	 * <br>
	 * So lets do special calculation on them
	 */
	@Override
	protected Number diffColumnValue(Number prevColVal, Number newColVal, boolean negativeDiffCountersToZero, String counterSetName, String colName, boolean isCountersCleared)
	{
		Number diffColVal = null;

		// If counters has been cleared, simply return the new value... no need to do diff calculation, and if we do they will be wrong...
		if (isCountersCleared)
			return newColVal;

		// ASE 12.5.x data
		if (newColVal instanceof BigDecimal)
		{
			diffColVal = new BigDecimal(newColVal.doubleValue() - prevColVal.doubleValue());
			if (diffColVal.doubleValue() < 0)
			{
				// Do special stuff for diff counters on CmSpinlockSum and ASE is 12.5.x, then counters will be delivered as numeric(19,0)
				// but is really signed int, then we need to check for wrapped signed int values
				// prevColVal is "near" UNSIGNED-INT-MAX and newColVal is "near" 0
				// Then do special calculation: (UNSIGNED-INT-MAX - prevColVal) + newColVal + 1      (+1 to handle passing value 0)
				// NOTE: we might also want to check COUNTER-RESET-DATE (if it has been done since last sample, then we can't trust the counters)
				if ( ! isCountersCleared )
				{
					Number beforeReCalc = diffColVal;
//					int  threshold      = 10000000;    // 10 000 000
					long maxUnsignedInt = 4294967295L; // 4 294 967 295

//					if (prevColVal.doubleValue() > (maxUnsignedInt - threshold) && newColVal.doubleValue() < threshold)
						diffColVal = new BigDecimal((maxUnsignedInt - prevColVal.doubleValue()) + newColVal.doubleValue() + 1);
					_logger.debug("diffColumnValue(): CM='"+counterSetName+"', BigDecimal(ASE-numeric) : CmSpinlockSum(colName='"+colName+"', isCountersCleared="+isCountersCleared+"):  AFTER: do special calc. newColVal.doubleValue()='"+newColVal.doubleValue()+"', prevColVal.doubleValue()='"+prevColVal.doubleValue()+"', beforeReCalc.doubleValue()='"+beforeReCalc.doubleValue()+"', diffColVal.doubleValue()='"+diffColVal.doubleValue()+"'.");
				}

				if (diffColVal.doubleValue() < 0)
					if (negativeDiffCountersToZero)
						diffColVal = new BigDecimal(0);
			}
		}
		// ASE >= 15.x data
		else if (newColVal instanceof Long)
		{
			diffColVal = new Long(newColVal.longValue() - prevColVal.longValue());
			if (diffColVal.longValue() < 0)
			{
				// Do special stuff for diff counters on CmSpinlockSum and ASE is above 15.x, then counters will be delivered as bigint
				// but is really signed int, then we need to check for wrapped signed int values
				// prevColVal is "near" UNSIGNED-INT-MAX and newColVal is "near" 0
				// Then do special calculation: (UNSIGNED-INT-MAX - prevColVal) + newColVal + 1      (+1 to handle passing value 0)
				// NOTE: we might also want to check COUNTER-RESET-DATE (if it has been done since last sample, then we can't trust the counters)
				if ( ! isCountersCleared )
				{
					Number beforeReCalc = diffColVal;
//					int  threshold      = 10000000;    // 10 000 000
					long maxUnsignedInt = 4294967295L; // 4 294 967 295
					
//					if (prevColVal.longValue() > (maxUnsignedInt - threshold) && newColVal.longValue() < threshold)
						diffColVal = new Long((maxUnsignedInt - prevColVal.longValue()) + newColVal.longValue() + 1);
					_logger.debug("diffColumnValue(): CM='"+counterSetName+"', Long(ASE-bigint) : CmSpinlockSum(colName='"+colName+"', isCountersCleared="+isCountersCleared+"):  AFTER: do special calc. newColVal.longValue()='"+newColVal.longValue()+"', prevColVal.longValue()='"+prevColVal.longValue()+"', beforeReCalc.longValue()='"+beforeReCalc.longValue()+"', diffColVal.longValue()='"+diffColVal.longValue()+"'.");
				}

				if (diffColVal.longValue() < 0)
					if (negativeDiffCountersToZero)
						diffColVal = new Long(0);
			}
		}
		else
		{
			diffColVal = super.diffColumnValue(prevColVal, newColVal, negativeDiffCountersToZero, counterSetName, colName, isCountersCleared);
		}

		return diffColVal;
	}
}
