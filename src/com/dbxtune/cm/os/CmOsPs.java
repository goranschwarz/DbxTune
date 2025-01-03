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
package com.dbxtune.cm.os;

import java.util.ArrayList;
import java.util.List;

import com.dbxtune.CounterController;
import com.dbxtune.ICounterController;
import com.dbxtune.IGuiController;
import com.dbxtune.cm.CmSettingsHelper;
import com.dbxtune.cm.CounterModelHostMonitor;
import com.dbxtune.cm.CounterSetTemplates;
import com.dbxtune.cm.CounterSetTemplates.Type;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.os.gui.CmOsPsPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.TabularCntrPanel;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

public class CmOsPs
extends CounterModelHostMonitor
{
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_PS;
	public static final String   CM_NAME          = CmOsPs.class.getSimpleName();
	public static final String   SHORT_NAME       = "OS Top Process(ps)";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'ps ... | top ##' on the Operating System" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmOsPs(counterController, guiController);
	}

	public CmOsPs(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, NEGATIVE_DIFF_COUNTERS_TO_ZERO, IS_SYSTEM_CM, DEFAULT_POSTPONE_TIME);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);
		
		// Normally for HostMonitor is ABS
//		setDataSource(DATA_RATE, false);
		setDataSource(DATA_ABS, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}

	public static final String  PROPKEY_top = "MonitorPs.top";
	public static final int     DEFAULT_top = 30;
	
	/** Used by the: Create 'Offline Session' Wizard */
	@Override
	public List<CmSettingsHelper> getLocalSettings()
	{
		Configuration conf = Configuration.getCombinedConfiguration();
		List<CmSettingsHelper> list = new ArrayList<>();
		
		list.add(new CmSettingsHelper("Top Rows", PROPKEY_top, Integer.class, conf.getIntProperty(PROPKEY_top, DEFAULT_top), DEFAULT_top, "Number of top rows."));

		return list;
	}


	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsPsPanel(this);
	}

	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	private void addTrendGraphs()
	{
	}

	/**
	 * Get a HTML Table representation of the ABS/RATE values from CmOsPs
	 * 
	 * @return A HTML Table String (if CmOsPs is not found, then "" will be returned)
	 */
	public static String getCmOsPs_asHtmlTable()
	{
		String retStr = "";
		
		CountersModel cmOsPs = CounterController.getInstance().getCmByName(CmOsPs.CM_NAME);
		if (cmOsPs != null)
		{
			if (cmOsPs.hasAbsData())
			{
				retStr += "<b>ABS Top Processes: (" + cmOsPs.getAbsRowCount() + " records)</b><br>";
				retStr += SwingUtils.tableToHtmlString(cmOsPs.getCounterDataAbs()) + "<br>";

				if (cmOsPs.hasRateData())
				{
					retStr += "<b>RATE Top Processes: (" + cmOsPs.getRateRowCount() + " records)</b><br>";
					retStr += SwingUtils.tableToHtmlString(cmOsPs.getCounterDataRate()) + "<br>";
				}
			}
			else
			{
				if (cmOsPs.isActive())
					retStr += "<b>Top Processes: (CmOsPs does NOT contain any data)</b><br>";
				else
					retStr += "<b>Top Processes: (CmOsPs is NOT enabled)</b><br>";
			}
		}
		
		return retStr;
	}
	
	
	
//	@Override
//	public void localCalculation(OsTable newSample)
//	{
////		System.out.println("localCalculation(OsTable thisSample): newSample.getColumnCount()="+newSample.getColumnCount()+", "+newSample.getColNames());
//
//		int sizeKB_pos      = newSample.findColumn("Size-KB");
//		int usedKB_pos      = newSample.findColumn("Used-KB");
//		int availableKB_pos = newSample.findColumn("Available-KB");
//
//		int sizeMB_pos      = newSample.findColumn("Size-MB");
//		int usedMB_pos      = newSample.findColumn("Used-MB");
//		int availableMB_pos = newSample.findColumn("Available-MB");
//
//		int usedPct_pos     = newSample.findColumn("UsedPct");
//
//		if (sizeKB_pos == -1 || usedKB_pos == -1 || availableKB_pos == -1 || sizeMB_pos == -1 || usedMB_pos == -1 || availableMB_pos == -1 || usedPct_pos == -1)
//		{
//			_logger.warn("Column position not available. sizeKB_pos="+sizeKB_pos+", usedKB_pos="+usedKB_pos+", availableKB_pos="+availableKB_pos+", sizeMB_pos="+sizeMB_pos+", usedMB_pos="+usedMB_pos+", availableMB_pos="+availableMB_pos+", usedPct_pos="+usedPct_pos+".");
//			return;
//		}
//		
//		for (int r=0; r<newSample.getRowCount(); r++)
//		{
//			Number sizeKB_num      = (Number) newSample.getValueAt(r, sizeKB_pos);
//			Number usedKB_num      = (Number) newSample.getValueAt(r, usedKB_pos);
//			Number availableKB_num = (Number) newSample.getValueAt(r, availableKB_pos);
//
//			if (sizeKB_num      != null) newSample.setValueAt(Integer.valueOf(sizeKB_num     .intValue()/1024), r, sizeMB_pos);
//			if (usedKB_num      != null) newSample.setValueAt(Integer.valueOf(usedKB_num     .intValue()/1024), r, usedMB_pos);
//			if (availableKB_num != null) newSample.setValueAt(Integer.valueOf(availableKB_num.intValue()/1024), r, availableMB_pos);
//
//			// Calculate the Pct value with a higher (scale=1) resolution than df
//			if (sizeKB_num != null && usedKB_num != null && availableKB_num != null)
//			{
//				if (sizeKB_num.intValue() > 0)
//				{
////					double pct = usedKB_num.doubleValue() / sizeKB_num.doubleValue() * 100.0;
//					double pct = 100.0 - (availableKB_num.doubleValue() / sizeKB_num.doubleValue() * 100.0);
//					if (pct <= 0)
//						pct = 0;
//					if (pct > 100)
//						pct = 100;
//
//					BigDecimal bd =  new BigDecimal( pct ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//					newSample.setValueAt(bd, r, usedPct_pos);
//				}
//			}
//		}
//	}
}
