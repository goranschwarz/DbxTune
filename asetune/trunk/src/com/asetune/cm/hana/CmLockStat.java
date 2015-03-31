package com.asetune.cm.hana;

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
public class CmLockStat
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmLockStat.class.getSimpleName();
	public static final String   SHORT_NAME       = "RW Locks";
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
		"EXCLUSIVE_LOCK_COUNT", 			//	BIGINT	Counter	Count of exclusive lock calls
		"EXCLUSIVE_WAIT_COUNT", 			//	BIGINT	Counter	Count of blocking exclusive lock calls
		"EXCLUSIVE_CAS_COLLISION_COUNT", 	//	BIGINT	Counter	Collision count on atomic operation on exclusive lock
//		"EXCLUSIVE_COLLISION_RATE", 		//	DOUBLE	Percent	Collision rate on exclusive lock in percent
//		"LAST_EXCLUSIVE_WAIT_TIME", 		//	BIGINT	Microsecond	Time of blocking exclusive lock calls (last)
//		"MAX_EXCLUSIVE_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking exclusive lock calls (max)
//		"MIN_EXCLUSIVE_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking exclusive lock calls (min)
		"SUM_EXCLUSIVE_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking exclusive lock calls (total)
//		"AVG_EXCLUSIVE_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking exclusive lock calls (avg)
		"INTENT_LOCK_COUNT", 				//	BIGINT	Counter	Count of intent lock calls
		"INTENT_WAIT_COUNT", 				//	BIGINT	Counter	Count of blocking intent lock calls
		"INTENT_CAS_COLLISION_COUNT", 		//	BIGINT	Counter	Collision count on atomic operation on intent lock
		"INTENT_TIMEOUT_COUNT", 			//	BIGINT	Counter	Count of timed out intent lock calls
//		"INTENT_COLLISION_RATE", 			//	DOUBLE	Percent	Collision rate on intent lock in percent
//		"LAST_INTENT_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking intent lock calls (last)
//		"MAX_INTENT_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking intent lock calls (max)
//		"MIN_INTENT_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking intent lock calls (min)
		"SUM_INTENT_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking intent lock calls (total)
//		"AVG_INTENT_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking intent lock calls (avg)
		"SHARED_LOCK_COUNT", 				//	BIGINT	Counter	Count of shared lock calls
		"SHARED_WAIT_COUNT", 				//	BIGINT	Counter	Count of blocking shared lock calls
		"SHARED_CAS_COLLISION_COUNT", 		//	BIGINT	Counter	Collision count on atomic operation on shared lock
		"SHARED_TIMEOUT_COUNT", 			//	BIGINT	Counter	Count of timed out shared lock calls
//		"SHARED_COLLISION_RATE", 			//	DOUBLE	Percent	Collision rate on shared lock in percent
//		"LAST_SHARED_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking shared lock calls (last)
//		"MAX_SHARED_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking shared lock calls (max)
//		"MIN_SHARED_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking shared lock calls (min)
		"SUM_SHARED_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking shared lock calls (total)
//		"AVG_SHARED_WAIT_TIME", 			//	BIGINT	Microsecond	Time of blocking shared lock calls (avg)
//		"COLLISION_RATE", 					//	DOUBLE	Percent	Global collision rate
		"CREATE_COUNT", 					//	BIGINT	Counter	Count of read/write lock creation (for shared statistics only)
		"DESTROY_COUNT", 					//	BIGINT	Counter	Count of read/write lock destruction (for shared statistics only) 
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

		return new CmLockStat(counterController, guiController);
	}

	public CmLockStat(ICounterController counterController, IGuiController guiController)
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

		pkCols.add("HOST");
		pkCols.add("PORT");
		pkCols.add("VOLUME_ID");
		pkCols.add("STATISTICS_ID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = "select * from M_READWRITELOCKS";

		return sql;
	}
}
