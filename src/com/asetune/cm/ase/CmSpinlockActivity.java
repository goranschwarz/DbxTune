package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.MonTablesDictionary;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.gui.MainFrame;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpinlockActivity
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSpinlockActivity.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpinlockActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Spinlock Act";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Spinlock Activity based on the MDA Table monSpinlockActivity introduced in ASE 17.7 ESD#2<br>" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 15702;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monSpinlockActivity"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable spinlock monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"Contention"};
	public static final String[] DIFF_COLUMNS     = new String[] {"Grabs", "Spins", "Waits"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	/** Keep a local list of all cache names, used in method doSqlInit(Connection conn) */
	private List<String> _aseCacheNames = new ArrayList<String>();

	
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

		return new CmSpinlockActivity(counterController, guiController);
	}

	public CmSpinlockActivity(ICounterController counterController, IGuiController guiController)
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
	
	private void addTrendGraphs()
	{
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

		pkCols.add("InstanceID");
		pkCols.add("SpinlockName");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
//		String sql = 
//			"select " + InstanceID + "SpinlockName, OwnerPID, LastOwnerPID, Grabs, Spins, Waits, Contention, \n" +
//			"       SpinsPerWait = CASE WHEN Waits > 0 \n" +
//			"                           THEN convert(numeric(12,1), (Spins + 0.0) / (Waits + 0.0) ) \n" +
//			"                           ELSE convert(numeric(12,1), 0.0 ) \n" +
//			"                      END, \n" +
//			"       Description  = convert(varchar(255), '') \n" +
//			"from master..monSpinlockActivity \n";
		String sql = 
			"select Type = convert(varchar(30), ''), \n" +
			"       InstanceID, \n" +
			"       SpinlockName, \n" +
			"       OwnerPID     = max(OwnerPID), \n" +
			"       LastOwnerPID = max(LastOwnerPID), \n" +
			"       Instances    = count(*), \n" +
			"       Grabs        = sum(Grabs), \n" +
			"       Spins        = sum(Spins), \n" +
			"       Waits        = sum(Waits), \n" +
			"       Contention   = avg(Contention), \n" +
			"       SpinsPerWait = CASE WHEN sum(Waits) > 0 \n" +
			"                           THEN convert(numeric(12,1), (sum(Spins) + 0.0) / (sum(Waits) + 0.0) ) \n" +
			"                           ELSE convert(numeric(12,1), 0.0 ) \n" +
			"                       END, \n" +
			"       Description  = convert(varchar(255), '') \n" +
			"from master..monSpinlockActivity \n" +
			"group by InstanceID, SpinlockName \n" +
			"order by SpinlockName \n";


		return sql;
	}

	@Override
	public boolean doSqlInit(Connection conn)
	{
		boolean superRc = super.doSqlInit(conn);

//		String sql = "select cache_name from master..syscacheinfo";
		String sql = "select comment from master.dbo.syscurconfigs where config=19 and value > 0";
		try
		{
			// Clear the cache list
			_aseCacheNames.clear();

			// refresh the list
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			while ( rs.next() )
			{
				String cacheName = rs.getString(1).trim();
				_aseCacheNames.add(cacheName);
				_logger.debug("Added cache name '"+cacheName+"' to the local cache list.");
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException e) 
		{
			_logger.warn("Problem when executing the 'extra init, to populate ASE Cache names' SQL statement: "+sql, e);
		}
		
		return superRc;
	}

	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		MonTablesDictionary mtd = MonTablesDictionary.getInstance();

		long Grabs, Waits, Spins;
		int  pos_Grabs = -1, pos_Waits = -1, pos_Spins = -1, pos_Contention = -1, pos_SpinsPerWait = -1, pos_SpinlockName = -1, pos_Description = -1, pos_Type = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("Grabs"))        pos_Grabs        = colId;
			else if (colName.equals("Waits"))        pos_Waits        = colId;
			else if (colName.equals("Spins"))        pos_Spins        = colId;
			else if (colName.equals("Contention"))   pos_Contention   = colId;
			else if (colName.equals("SpinsPerWait")) pos_SpinsPerWait = colId;
			else if (colName.equals("SpinlockName")) pos_SpinlockName = colId;
			else if (colName.equals("Description"))  pos_Description  = colId;
			else if (colName.equals("Type"))         pos_Type         = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			Grabs = ((Number) diffData.getValueAt(rowId, pos_Grabs)).longValue();
			Waits = ((Number) diffData.getValueAt(rowId, pos_Waits)).longValue();
			Spins = ((Number) diffData.getValueAt(rowId, pos_Spins)).longValue();

			// contention
			if (Grabs > 0)
			{
				BigDecimal contention = new BigDecimal( ((1.0 * (Waits)) / Grabs) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				// Keep only 3 decimals
				// row.set(AvgServ_msId, new Double (AvgServ_ms/1000) );
				diffData.setValueAt(contention, rowId, pos_Contention);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_Contention);

			// spinsPerWait
			if (Waits > 0)
			{
				BigDecimal spinWarning = new BigDecimal( ((1.0 * (Spins)) / Waits) ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

				diffData.setValueAt(spinWarning, rowId, pos_SpinsPerWait);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, pos_SpinsPerWait);

			// set description
			if (mtd != null && pos_SpinlockName >= 0 && pos_Description >= 0)
			{
				Object o = diffData.getValueAt(rowId, pos_SpinlockName);

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

			// set Type
			if (mtd != null && pos_SpinlockName >= 0 && pos_Type >= 0)
			{
				Object o = diffData.getValueAt(rowId, pos_SpinlockName);

				if (o instanceof String)
				{
					String name = (String)o;

					String desc = mtd.getSpinlockType(name, _aseCacheNames);
					if (desc != null)
					{
						newSample.setValueAt(desc, rowId, pos_Type);
						diffData .setValueAt(desc, rowId, pos_Type);
					}
				}
			}
		}
	}
}
