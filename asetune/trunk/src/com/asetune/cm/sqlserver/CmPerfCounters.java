package com.asetune.cm.sqlserver;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmPerfCounters
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmPerfCounters.class.getSimpleName();
	public static final String   SHORT_NAME       = "Perf Counters";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"cntr_value"
		};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;;

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

		return new CmPerfCounters(counterController, guiController);
	}

	public CmPerfCounters(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
				DIFF_COLUMNS, PCT_COLUMNS, MON_TABLES, 
				NEED_ROLES, NEED_CONFIG, NEED_SRV_VERSION, NEED_CE_VERSION, 
				NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setShowClearTime(false);

		setCounterController(counterController);
		setGuiController(guiController);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	
	private void addTrendGraphs()
	{
	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("object_name");
		pkCols.add("counter_name");
		pkCols.add("instance_name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			  "select *, \n"
			+ "       CASE \n"
			+ "          WHEN cntr_type = 65792      THEN convert(varchar(30), 'PERF_COUNTER_LARGE_RAWCOUNT') \n" // provides the last observed value for the counter; for this type of counter, the values in cntr_value can be used directly, making this the most easily usable type
			+ "          WHEN cntr_type = 272696576  THEN convert(varchar(30), 'PERF_COUNTER_BULK_COUNT')     \n" // provides the average number of operations per second. Two readings of cntr_value will be required for this counter type, in order to get the per second averages
			+ "          WHEN cntr_type = 537003264  THEN convert(varchar(30), 'PERF_LARGE_RAW_FRACTION')     \n" // used in conjunction with PERF_LARGE_RAW_BASE to calculate ratio values, such as the cache hit ratio
			+ "          WHEN cntr_type = 1073874176 THEN convert(varchar(30), 'PERF_AVERAGE_BULK')           \n" // used to calculate an average number of operations completed during a time interval; like PERF_LARGE_RAW_FRACTION, it uses PERF_LARGE_RAW_BASE to do the calculation
			+ "          WHEN cntr_type = 1073939712 THEN convert(varchar(30), 'PERF_LARGE_RAW_BASE')         \n" // used in the translation of PERF_LARGE_RAW_FRACTION and PERF_AVERAGE_BULK values to readable output; should not be displayed alone.
			+ "          ELSE convert(varchar(30), cntr_type) \n"
			+ "       END as cntr_type_name \n"
			+ "from sys.dm_os_performance_counters";

		return sql;
	}
}
