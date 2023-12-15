/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.cm.ase;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.asetune.CounterControllerAse;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.central.pcs.CentralPersistReader;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.sql.conn.info.DbmsVersionInfo;
import com.asetune.sql.conn.info.DbmsVersionInfoSybaseAse;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmIoQueue
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmIoQueue.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmIoQueue.class.getSimpleName();
	public static final String   SHORT_NAME       = "IO Queue";
	public static final String   HTML_DESC        = 
		"<html>" +
		"<p>How many IO's have the ASE Server done on various segments (UserDb/Tempdb/System) on specific devices.</p>" +
		"For ASE 15.0.2 or so, we will have 'System' segment covered aswell.<br>" +
		"Do not trust the service time <b>too</b> much...<br>" +
		"<br>" +
		CounterControllerAse.TRANLOG_DISK_IO_TOOLTIP +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_DISK;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final long     NEED_SRV_VERSION = 0;
	public static final long     NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monIOQueue"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {"enable monitoring=1"};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {"IOs", "IOTime"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = true;
	public static final boolean  IS_SYSTEM_CM                   = true;
	public static final int      DEFAULT_POSTPONE_TIME          = 0;
	public static final int      DEFAULT_QUERY_TIMEOUT          = CountersModel.DEFAULT_sqlQueryTimeout;

	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.SMALL; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmIoQueue(counterController, guiController);
	}

	public CmIoQueue(ICounterController counterController, IGuiController guiController)
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
	
	public static final String GRAPH_NAME_DEVICE_SERVICE_TIME = "devSvcTime"; //String x=GetCounters.CM_GRAPH_NAME__IO_QUEUE__DEVICE_SERVICE_TIME;

	private void addTrendGraphs()
	{
		addTrendGraph(GRAPH_NAME_DEVICE_SERVICE_TIME,
			"Device IO Service Time", 	              // Menu CheckBox text
			"Device IO Service Time in Milliseconds ("+GROUP_NAME+"->"+SHORT_NAME+")", // Label 
			TrendGraphDataPoint.createGraphProps(TrendGraphDataPoint.Y_AXIS_SCALE_LABELS_MILLISEC, CentralPersistReader.SampleType.MAX_OVER_SAMPLES),
			new String[] { "Max", "Average" }, 
			LabelType.Static,
			TrendGraphDataPoint.Category.DISK,
			false, // is Percent Graph
			true,  // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);  // minimum height

	}

//	@Override
//	protected TabularCntrPanel createGui()
//	{
//		return new CmIoQueuePanel(this);
//	}

	@Override
	public String[] getDependsOnConfigForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		return NEED_CONFIG;
	}

	@Override
	public List<String> getPkForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		List <String> pkCols = new LinkedList<String>();

		if (isClusterEnabled)
			pkCols.add("InstanceID");

		pkCols.add("LogicalName");
		pkCols.add("IOType");

		return pkCols;
	}

	@Override
	public String getSqlForVersion(DbxConnection conn, DbmsVersionInfo versionInfo)
	{
		DbmsVersionInfoSybaseAse aseVersionInfo = (DbmsVersionInfoSybaseAse) versionInfo;
//		long    srvVersion       = aseVersionInfo.getLongVersion();
		boolean isClusterEnabled = aseVersionInfo.isClusterEdition();

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		if (isClusterEnabled)
		{
			cols1 += "InstanceID, ";
		}

		cols1 += "LogicalName, IOType, IOs, IOTime, \n" +
                 "AvgServ_ms = \n" +
                 "CASE \n" +
                 "  WHEN IOs > 0 THEN convert(numeric(18,1), IOTime/convert(numeric(18,0),IOs)) \n" +
                 "  ELSE              convert(numeric(18,1), null) \n" +
                 "END";

		String sql = 
			"select " + cols1 + cols2 + cols3 + "\n" +
			"from master..monIOQueue \n" +
			"order by LogicalName, IOType" + (isClusterEnabled ? ", InstanceID" : "") + "\n";

		return sql;
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_DEVICE_SERVICE_TIME.equals(tgdp.getName()))
		{
			Double[] arr = new Double[2];
			arr[0] = this.getDiffValueMax("AvgServ_ms"); // MAX
			arr[1] = this.getDiffValueAvgGtZero("AvgServ_ms"); // AVG
			
			if (_logger.isDebugEnabled())
			{
				_logger.debug("devSvcTime: MaxServiceTime=" + arr[0] + ", AvgServiceTime=" + arr[1] + ".");
				_logger.debug("updateGraphData(devSvcTime): MaxServiceTime='"+arr[0]+"', AvgServiceTime='"+arr[1]+"'.");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), arr);
		}
	}
	@Override
	public void localCalculation(CounterSample prevSample, CounterSample newSample, CounterSample diffData)
	{
		double AvgServ_ms;
		long   IOs,        IOTime;
		int    IOsId = -1, IOTimeId = -1, AvgServ_msId = -1;

		// Find column Id's
		List<String> colNames = diffData.getColNames();
		if (colNames == null)
			return;
		for (int colId = 0; colId < colNames.size(); colId++)
		{
			String colName = (String) colNames.get(colId);
			if      (colName.equals("IOs"))        IOsId        = colId;
			else if (colName.equals("IOTime"))     IOTimeId     = colId;
			else if (colName.equals("AvgServ_ms")) AvgServ_msId = colId;
		}

		// Loop on all diffData rows
		for (int rowId = 0; rowId < diffData.getRowCount(); rowId++)
		{
			IOs    = ((Number) diffData.getValueAt(rowId, IOsId))   .longValue();
			IOTime = ((Number) diffData.getValueAt(rowId, IOTimeId)).longValue();

			if (IOs != 0)
			{
				AvgServ_ms = IOTime / IOs;
				BigDecimal newVal = new BigDecimal(AvgServ_ms).setScale(1, BigDecimal.ROUND_HALF_EVEN);;
				diffData.setValueAt(newVal, rowId, AvgServ_msId);
			}
			else
				diffData.setValueAt(new BigDecimal(0), rowId, AvgServ_msId);
		}
	}
}
