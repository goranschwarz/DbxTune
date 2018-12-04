package com.asetune.cm.sqlserver;

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
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpinlocks
extends CountersModel
{
//	private static Logger        _logger          = Logger.getLogger(CmSpinlocks.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpinlocks.class.getSimpleName();
	public static final String   SHORT_NAME       = "Spinlock Info";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>FIXME</p>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"dm_os_spinlock_stats"};
	public static final String[] NEED_ROLES       = new String[] {};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {"ContentionPct"};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"collisions",
		"spins",
		"sleep_time",
		"backoffs"
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

		return new CmSpinlocks(counterController, guiController);
	}

	public CmSpinlocks(ICounterController counterController, IGuiController guiController)
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

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmRaSysmonPanel(this);
//	}

	@Override
	public void addMonTableDictForVersion(Connection conn, int srvVersion, boolean isClusterEnabled)
	{
		try 
		{
			MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();
			mtd.addTable("dm_os_spinlock_stats",  "Spinlocks.");

			mtd.addColumn("dm_os_spinlock_stats", "name",                "<html>The name of the spinlock</html>");
			mtd.addColumn("dm_os_spinlock_stats", "spins",               "<html>The number of times that threads were blocked by a spinlock when trying to access a protected data structure</html>");
			mtd.addColumn("dm_os_spinlock_stats", "collisions",          "<html>The number of times threads spinned in a loop trying to obtain the spinlock</html>");
			mtd.addColumn("dm_os_spinlock_stats", "ContentionPct",       "<html>Calculates the percentage of the spinlock contention for this specific spinlock.<br> <b>Formula</b>: (collisions/spins)*100</html>");
			mtd.addColumn("dm_os_spinlock_stats", "spins_per_collision", "<html>Ratio between spins and collisions</html>");
			mtd.addColumn("dm_os_spinlock_stats", "sleep_time",          "<html>The time that threads were sleeping because of a backoff</html>");
			mtd.addColumn("dm_os_spinlock_stats", "backoffs",            "<html>The number of times that threads were backed-off to allow other threads to continue on the CPU</html>");
			mtd.addColumn("dm_os_spinlock_stats", "Description",         "<html>Description of this specific spinlock</html>");
			
		}
		catch (NameNotFoundException e) {/*ignore*/}
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

		pkCols.add("name");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String sql = 
			"select \n" +
			"    s.name, \n" +
			"    s.spins, \n" +
			"    s.collisions, \n" +
			"    s.spins_per_collision, \n" + 
			"    ContentionPct = CASE WHEN s.spins > 0 THEN convert(numeric(5,1), ((s.collisions*1.0)/(s.spins*1.0))*100.0) ELSE convert(numeric(5,1), 0.0) END, \n" +
			"    s.sleep_time, \n" +
			"    s.backoffs, \n" +
			"    Description = convert(varchar(1024), '') \n" +
			"from sys.dm_os_spinlock_stats s \n" +
			"order by s.spins_per_collision desc \n" +
			"";

		return sql;
	}



	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

//		long Grabs, Waits, Spins;
//		int  pos_Grabs = -1, pos_Waits = -1, pos_Spins = -1, pos_Contention = -1, pos_SpinsPerWait = -1, pos_SpinlockName = -1, pos_Description = -1, pos_Type = -1;
		long collisions, spins;
		int  pos_collisions = -1, pos_ContentionPct = -1, pos_spins = -1, pos_spins_per_collision = -1, pos_name = -1, pos_Description = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("collisions"))          pos_collisions          = colId;
			else if (colName.equals("spins"))               pos_spins               = colId;
			else if (colName.equals("ContentionPct"))       pos_ContentionPct       = colId;
			else if (colName.equals("spins_per_collision")) pos_spins_per_collision = colId;
			else if (colName.equals("name"))                pos_name                = colId;
			else if (colName.equals("Description"))         pos_Description         = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			collisions = ((Number) diffData.getValueAt(rowId, pos_collisions)).longValue();
			spins      = ((Number) diffData.getValueAt(rowId, pos_spins     )).longValue();

			//---------------------------
			// contention
			if (spins > 0)
			{
				BigDecimal contention = new BigDecimal( ((1.0 * (collisions)) / spins) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(contention, rowId, pos_ContentionPct);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_ContentionPct);

			//---------------------------
			// spins_per_collision
			if (collisions > 0)
			{
				BigDecimal spins_per_collision = new BigDecimal( ((1.0 * (spins)) / collisions) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(spins_per_collision, rowId, pos_spins_per_collision);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_spins_per_collision);

			//---------------------------
			// set description
			if (mtd != null && pos_name >= 0 && pos_Description >= 0)
			{
				Object o = diffData.getValueAt(rowId, pos_name);

				if (o instanceof String)
				{
					String name = (String)o;

					String desc = mtd.getSpinlockDescription(name);
					if (desc != null)
					{
						newSample.setValueAt(desc, rowId, pos_Description);
						diffData .setValueAt(desc, rowId, pos_Description);
					}
				}
			}
		}
	}
	
//	@Override
//	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
//	{
//		String tooltip = CmSpinlockSum.getToolTipTextOnTableCell(this, e, colName, cellValue, modelRow, modelCol);
//
//		// If nothing was found, call super
//		if (tooltip == null)
//			tooltip = super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);
//
//		return tooltip;
//	}
}