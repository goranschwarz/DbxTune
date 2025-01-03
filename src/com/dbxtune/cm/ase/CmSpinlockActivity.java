/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
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
package com.dbxtune.cm.ase;

import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.ase.gui.CmSpinlockActivityPanel;
import com.dbxtune.config.dict.MonTablesDictionary;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.sql.conn.DbxConnection;
import com.dbxtune.sql.conn.info.DbmsVersionInfo;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.Ver;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSpinlockActivity
extends CountersModel
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSpinlockActivity.class.getSimpleName();
	public static final String   SHORT_NAME       = "Spinlock Act";
//	public static final String   HTML_DESC        = 
//		"<html>" +
//		"Spinlock Activity based on the MDA Table monSpinlockActivity introduced in ASE 15.7 ESD#2<br>" +
//		"</html>";
	public static final String   HTML_DESC        = CmSpinlockSum.HTML_DESC.replaceFirst("<CODE>master.dbo.sysmonitors</CODE>", "<CODE>master.dbo.monSpinlockActivity</CODE> introduced in ASE 15.7 ESD#2.");

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_SERVER;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	public static final long     NEED_SRV_VERSION = 15702;
//	public static final long     NEED_SRV_VERSION = 1570020;
	public static final long     NEED_SRV_VERSION = Ver.ver(15,7,0,2);
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monSpinlockActivity"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1", "enable spinlock monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {"Contention"};
	public static final String[] DIFF_COLUMNS     = new String[] {"Grabs", "Spins", "Waits"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	/** Keep a local list of all cache names, used in method doSqlInit(DbxConnection conn) */
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
	private static final String  PROP_PREFIX                       = CM_NAME;

	public static final String  PROPKEY_sample_SpinlockSlotID      = PROP_PREFIX + ".sample.SpinlockSlotID";
	public static final boolean DEFAULT_sample_SpinlockSlotID      = true;

	
	private void addTrendGraphs()
	{
	}

	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmSpinlockActivityPanel(this);
	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

		List <String> pkCols = new LinkedList<String>();

		pkCols.add("InstanceID");
		pkCols.add("SpinlockName");
		
		if (srvVersion >= Ver.ver(15,7,0,100))
			pkCols.add("SpinlockSlotID");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		long srvVersion = versionInfo.getLongVersion();

//		String sql = 
//			"select " + InstanceID + "SpinlockName, OwnerPID, LastOwnerPID, Grabs, Spins, Waits, Contention, \n" +
//			"       SpinsPerWait = CASE WHEN Waits > 0 \n" +
//			"                           THEN convert(numeric(12,1), (Spins + 0.0) / (Waits + 0.0) ) \n" +
//			"                           ELSE convert(numeric(12,1), 0.0 ) \n" +
//			"                      END, \n" +
//			"       Description  = convert(varchar(255), '') \n" +
//			"from master..monSpinlockActivity \n";

		// in 15.7.0 ESD#2 the SpinlockSlotID was not available, then we need to do SUM/AVG/MAX/MIN and GROUP BY to get 1 row for each Spinlock 
		String sql = 
			"select Type = convert(varchar(30), ''), \n" +
			"       InstanceID, \n" +
			"       SpinlockName, \n" +
			"       Instances    = count(*), \n" +
//			((srvVersion >= 1570100) ? "       SpinlockSlotID = convert(int, 0), \n" : "") + // in 15.7 SP100, add SpinlockSlotID as dummy to be consistent with below full statement
			((srvVersion >= Ver.ver(15,7,0,100)) ? "       SpinlockSlotID = convert(int, 0), \n" : "") + // in 15.7 SP100, add SpinlockSlotID as dummy to be consistent with below full statement
			"       OwnerPID     = max(OwnerPID), \n" +
			"       LastOwnerPID = max(LastOwnerPID), \n" +
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
			"order by SpinlockName \n" +
			"";

		// in 15.7.0 SP100 the SpinlockSlotID was introduced... but we can keep/toggle with the GROUPED select above if we ONLY WANT ONE row for each SpinlockName...
		boolean showSpinlockSlotID = Configuration.getCombinedConfiguration().getBooleanProperty(PROPKEY_sample_SpinlockSlotID, DEFAULT_sample_SpinlockSlotID);
//		if (srvVersion >= 1570100 && showSpinlockSlotID)
		if (srvVersion >= Ver.ver(15,7,0,100) && showSpinlockSlotID)
		{
			sql = 
				"select Type = convert(varchar(30), ''), \n" +
				"       InstanceID, \n" +
				"       SpinlockName, \n" +
				"       Instances    = convert(int, 1), \n" +
				"       SpinlockSlotID, \n" +
				"       OwnerPID, \n" +
				"       LastOwnerPID, \n" +
				"       Grabs, \n" +
				"       Spins, \n" +
				"       Waits, \n" +
				"       Contention, \n" +
				"       SpinsPerWait = CASE WHEN Waits > 0 \n" +
				"                           THEN convert(numeric(12,1), (Spins + 0.0) / (Waits + 0.0) ) \n" +
				"                           ELSE convert(numeric(12,1), 0.0 ) \n" +
				"                       END, \n" +
				"       Description  = convert(varchar(255), '') \n" +
				"from master..monSpinlockActivity \n" +
				"order by SpinlockName, SpinlockSlotID\n" +
				"";
		}

		return sql;
	}

	@Override
	public boolean doSqlInit(DbxConnection conn)
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

	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("View Individual Spinlock Instances", PROPKEY_sample_SpinlockSlotID , Boolean.class, conf.getBooleanProperty(PROPKEY_sample_SpinlockSlotID , DEFAULT_sample_SpinlockSlotID ), DEFAULT_sample_SpinlockSlotID, CmSpinlockActivityPanel.TOOLTIP_sample_SpinlockSlotID ));

		return list;
	}


	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		MonTablesDictionary mtd = MonTablesDictionaryManager.getInstance();

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
	
	@Override
	public String getToolTipTextOnTableCell(MouseEvent e, String colName, Object cellValue, int modelRow, int modelCol) 
	{
		String tooltip = CmSpinlockSum.getToolTipTextOnTableCell(this, e, colName, cellValue, modelRow, modelCol);

		// If nothing was found, call super
		if (tooltip == null)
			tooltip = super.getToolTipTextOnTableCell(e, colName, cellValue, modelRow, modelCol);

		return tooltip;
	}
}
