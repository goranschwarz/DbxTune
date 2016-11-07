package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.naming.NameNotFoundException;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmTempdbActivity
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmTempdbActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmTempdbActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Temp Db";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Provides statistics for all local temporary databases.<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final int      NEED_SRV_VERSION = 15500;
//	public static final int      NEED_SRV_VERSION = 1550000;
	public static final int      NEED_SRV_VERSION = Ver.ver(15,5);
//	public static final int      NEED_CE_VERSION  = 15020;
//	public static final int      NEED_CE_VERSION  = 1502000;
	public static final int      NEED_CE_VERSION  = Ver.ver(15,0,2);

	public static final String[] MON_TABLES       = new String[] {"monTempdbActivity"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "object lockwait timing=1", "per object statistics active=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"AppendLogContPct", "LockContPct", "CatLockContPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"AppendLogRequests", "AppendLogWaits", "LogicalReads", "PhysicalReads", 
		"APFReads", "PagesRead", "PhysicalWrites", "PagesWritten", "LockRequests", 
		"LockWaits", "CatLockRequests", "CatLockWaits", "AssignedCnt", "SharableTabCnt"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.MEDIUM; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmTempdbActivity(counterController, guiController);
	}

	public CmTempdbActivity(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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
	
	public static final String GRAPH_NAME_LOGSEMAPHORE_CONT = "LogSemapContGraph"; //GetCounters.CM_GRAPH_NAME__TEMPDB_ACTIVITY__LOGSEMAPHORE_CONT;

	private void addTrendGraphs()
	{
		String[] labels = new String[] { "runtime-replaced" };
		
		addTrendGraphData(GRAPH_NAME_LOGSEMAPHORE_CONT, new TrendGraphDataPoint(GRAPH_NAME_LOGSEMAPHORE_CONT, labels));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = new TrendGraph(GRAPH_NAME_LOGSEMAPHORE_CONT,
				"TempDB Transaction Log Semaphore Contention", 	          // Menu CheckBox text
				"TempDB Transaction Log Semaphore Contention in Percent", // Label 
				labels, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("monTempdbActivity", "AppendLogContPct",  "<html>" +
			                                                             "Log Semaphore Contention in percent.<br> " +
			                                                             "<b>Formula</b>: Pct = (AppendLogWaits / AppendLogRequests) * 100<br>" +
			                                                        "</html>");
			mtd.addColumn("monTempdbActivity", "LockContPct",       "<html>" +
			                                                             "Lock Contention in percent.<br> " +
			                                                             "<b>Formula</b>: Pct = (LockWaits / LockRequests) * 100<br>" +
			                                                        "</html>");
			mtd.addColumn("monTempdbActivity", "CatLockContPct",    "<html>" +
			                                                             "Catalog Lock Contention in percent.<br> " +
			                                                             "<b>Formula</b>: Pct = (CatLockWaits / CatLockRequests) * 100<br>" +
			                                                        "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}

	@Override
	public List<String> getPkForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("DBID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		cols1 += "DBID, DBName, \n" +
		         "SharableTabCnt, \n" +
		         "AppendLogRequests, AppendLogWaits, \n" +
		         "AppendLogContPct = CASE \n" +
		         "                       WHEN AppendLogRequests > 0 \n" +
		         "                       THEN convert(numeric(10,2), ((AppendLogWaits+0.0)/AppendLogRequests)*100.0) \n" +
		         "                       ELSE convert(numeric(10,2), 0.0) \n" +
		         "                   END, \n" +
		         "LogicalReads, PhysicalReads, APFReads, \n" +
		         "PagesRead, PhysicalWrites, PagesWritten, \n" +
		         "LockRequests, LockWaits, \n" +
		         "LockContPct      = CASE \n" +
		         "                       WHEN LockRequests > 0 \n" +
		         "                       THEN convert(numeric(10,2), ((LockWaits+0.0)/LockRequests)*100.0) \n" +
		         "                       ELSE convert(numeric(10,2), 0.0) \n" +
		         "                   END, \n" +
		         "CatLockRequests, CatLockWaits, \n" +
		         "CatLockContPct   = CASE \n" +
		         "                       WHEN CatLockRequests > 0 \n" +
		         "                       THEN convert(numeric(10,2), ((CatLockWaits+0.0)/CatLockRequests)*100.0) \n" +
		         "                       ELSE convert(numeric(10,2), 0.0) \n" +
		         "                   END, \n" +
		         "AssignedCnt ";
		cols2 += "";
		cols3 += "";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monTempdbActivity \n" +
			"order by DBName \n";

		return sql;
	}

	/** 
	 * Compute the AppendLogContPct for DIFF values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		int AppendLogRequests,          AppendLogWaits;
		int AppendLogRequests_pos = -1, AppendLogWaits_pos = -1;
		int AppendLogContPct_pos = -1;

		int LockRequests,               LockWaits;
		int LockRequests_pos = -1,      LockWaits_pos = -1;
		int LockContPct_pos = -1;

		int CatLockRequests,            CatLockWaits;
		int CatLockRequests_pos = -1,   CatLockWaits_pos = -1;
		int CatLockContPct_pos = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			if      (colName.equals("AppendLogContPct"))  AppendLogContPct_pos  = colId;
			else if (colName.equals("AppendLogRequests")) AppendLogRequests_pos = colId;
			else if (colName.equals("AppendLogWaits"))    AppendLogWaits_pos    = colId;
			else if (colName.equals("LockContPct"))       LockContPct_pos       = colId;
			else if (colName.equals("LockRequests"))      LockRequests_pos      = colId;
			else if (colName.equals("LockWaits"))         LockWaits_pos         = colId;
			else if (colName.equals("CatLockContPct"))    CatLockContPct_pos    = colId;
			else if (colName.equals("CatLockRequests"))   CatLockRequests_pos   = colId;
			else if (colName.equals("CatLockWaits"))      CatLockWaits_pos      = colId;
		}

		int colPos;
		// Loop on all DIFF DATA rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			AppendLogRequests = ((Number)diffData.getValueAt(rowId, AppendLogRequests_pos)).intValue();
			AppendLogWaits    = ((Number)diffData.getValueAt(rowId, AppendLogWaits_pos   )).intValue();

			LockRequests      = ((Number)diffData.getValueAt(rowId, LockRequests_pos     )).intValue();
			LockWaits         = ((Number)diffData.getValueAt(rowId, LockWaits_pos        )).intValue();

			CatLockRequests   = ((Number)diffData.getValueAt(rowId, CatLockRequests_pos  )).intValue();
			CatLockWaits      = ((Number)diffData.getValueAt(rowId, CatLockWaits_pos     )).intValue();

			//------------------------------
			// CALC: AppendLogContPct_pos
			// int totIo = Reads + APFReads + Writes;
			colPos = AppendLogContPct_pos;
			if (AppendLogRequests > 0)
			{
				double calc = ((AppendLogWaits + 0.0) / AppendLogRequests) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, colPos);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);

			//------------------------------
			// CALC: LockContPct
			colPos = LockContPct_pos;
			if (LockRequests > 0)
			{
				double calc = ((LockWaits+0.0) / (LockRequests+0.0)) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, colPos);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);

			//------------------------------
			// CALC: CatLockContPct
			colPos = CatLockContPct_pos;
			if (CatLockRequests > 0)
			{
				double calc = ((CatLockWaits+0.0) / (CatLockRequests+0.0)) * 100.0;

				BigDecimal newVal = new BigDecimal(calc).setScale(2, BigDecimal.ROUND_HALF_EVEN);
				diffData.setValueAt(newVal, rowId, colPos);
			}
			else
				diffData.setValueAt(new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_EVEN), rowId, colPos);
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_LOGSEMAPHORE_CONT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every database
			Double[] dArray = new Double[this.size()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getAbsString        (i, "DBName");
				dArray[i] = this.getDiffValueAsDouble(i, "AppendLogContPct");
			}

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setLabel(lArray);
			tgdp.setData(dArray);
		}
	}
}
