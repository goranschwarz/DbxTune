package com.asetune.cm.sqlserver;

import java.awt.event.MouseEvent;
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
import com.asetune.cm.sqlserver.gui.CmWaitStatsPanel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.config.dict.MonTablesDictionaryManager;
import com.asetune.config.dict.SqlServerWaitTypeDictionary;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmWaitStats
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmServiceMemory.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmWaitStats.class.getSimpleName();
	public static final String   SHORT_NAME       = "Wait Stats";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_wait_stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"waiting_tasks_count",
		"wait_time_ms",
		"signal_wait_time_ms"
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

		return new CmWaitStats(counterController, guiController);
	}

	public CmWaitStats(ICounterController counterController, IGuiController guiController)
	{
		super(counterController,
				CM_NAME, GROUP_NAME, /*sql*/null, /*pkList*/null, 
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

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmWaitStatsPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		List <String> pkCols = new LinkedList<String>();

		pkCols.add("wait_type");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select \n" +
			"    wait_type, \n" +
			"    waiting_tasks_count, \n" +
			"    wait_time_ms, \n" +
			"    max_wait_time_ms, \n" +
			"    signal_wait_time_ms, \n" +
			"    WaitTimePerWait = CASE WHEN waiting_tasks_count > 0 \n" +
			"                           THEN convert(numeric(18,3), (wait_time_ms + 0.0) / waiting_tasks_count) \n" +
			"                           ELSE convert(numeric(18,3), 0.0) \n" +
			"                      END, \n" +
			"    Description = convert(varchar(1500), '') \n" +
			"from sys.dm_os_wait_stats \n" +
			"";

		return sql;
	}






	@Override
	public void addMonTableDictForVersion(Connection conn, long srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addColumn("dm_os_wait_stats", "WaitTimePerWait", "<html>" +
			                                                   "Wait time in ms per wait. <br>" +
			                                                   "<b>formula</b>: diff.wait_time_ms / diff.waiting_tasks_count<br>" +
			                                                "</html>");
		}
		catch (NameNotFoundException e) {/*ignore*/}
	}



	/** 
	 * Fill in the Description column with data from
	 * SqlServerWaitNameDictionary.. transforms a wait_type -> text description
	 */
	@Override
	public void localCalculation(CounterSample newSample)
	{
		// Where are various columns located in the Vector 
		int pos_wait_type = -1, pos_Description = -1;
	
		SqlServerWaitTypeDictionary wd = SqlServerWaitTypeDictionary.getInstance();
		if (wd == null)
			return;

		if (newSample == null)
			return;

		// Find column Id's
		List<String> colNames = newSample.getColNames();
		if (colNames==null) 
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);

			if      (colName.equals("wait_type"))   pos_wait_type   = colId;
			else if (colName.equals("Description")) pos_Description = colId;
		}

		// Loop on all counter rows
		for (int rowId=0; rowId < newSample.getRowCount(); rowId++) 
		{
			Object o_wait_type  = newSample.getValueAt(rowId, pos_wait_type);

			if (o_wait_type instanceof String)
			{
				String desc = wd.getDescriptionPlain( (String)o_wait_type );

				if (desc != null)
					newSample.setValueAt(desc, rowId, pos_Description);
			}
		}
	}

	/** 
	 * Compute the WaitTimePerWait for diff values
	 */
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		long WaitTime,        Waits;
		int  WaitTimeId = -1, WaitsId = -1;

		double calcWaitTimePerWait;
		int WaitTimePerWaitId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;

		for (int colId=0; colId < colNames.size(); colId++) 
		{
			String colName = colNames.get(colId);
			
			if      (colName.equals("WaitTimePerWait"))     WaitTimePerWaitId = colId;
			else if (colName.equals("wait_time_ms"))        WaitTimeId        = colId;
			else if (colName.equals("waiting_tasks_count")) WaitsId           = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			WaitTime = ((Number)diffData.getValueAt(rowId, WaitTimeId)).longValue();
			Waits    = ((Number)diffData.getValueAt(rowId, WaitsId   )).longValue();

			if (Waits > 0)
			{
				calcWaitTimePerWait = WaitTime / (Waits * 1.0);

				BigDecimal newVal = new BigDecimal(calcWaitTimePerWait).setScale(3, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, WaitTimePerWaitId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, WaitTimePerWaitId);
		}
	}

	
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		if ("wait_type".equals(colName))
		{
			return cellValue == null ? null : SqlServerWaitTypeDictionary.getInstance().getDescriptionHtml(cellValue.toString());
		}

		if ("Description".equals(colName) )
		{
			int pos_wait_type = findColumn("wait_type");
			if (pos_wait_type >= 0)
			{
				Object cellVal = getValueAt(modelRow, pos_wait_type);
				if (cellVal instanceof String)
				{
					return SqlServerWaitTypeDictionary.getInstance().getDescriptionHtml((String) cellVal);
				}
			}
		}
		
		return super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
	}
}
