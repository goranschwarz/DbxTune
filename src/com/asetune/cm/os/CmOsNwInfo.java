/*******************************************************************************
 * Copyright (C) 2010-2019 Goran Schwarz
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
package com.asetune.cm.os;

import java.math.BigDecimal;

import org.apache.log4j.Logger;

import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.cm.CounterModelHostMonitor;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.os.gui.CmOsNwInfoPanel;
import com.asetune.graph.TrendGraphDataPoint;
import com.asetune.graph.TrendGraphDataPoint.LabelType;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.hostmon.OsTable;

public class CmOsNwInfo
extends CounterModelHostMonitor
{
	private static Logger        _logger          = Logger.getLogger(CmOsNwInfo.class);
	private static final long    serialVersionUID = 1L;

	public static final int      CM_TYPE          = CounterModelHostMonitor.HOSTMON_NWINFO;
	public static final String   CM_NAME          = CmOsNwInfo.class.getSimpleName();
	public static final String   SHORT_NAME       = "Network Stat";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Executes: 'cat /proc/net/dev' on the Operating System (Linux only)" +
		"</html>";

	public static final String   GROUP_NAME       = MainFrame.TCP_GROUP_HOST_MONITOR;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

//	@Override public int     getDefaultPostponeTime()                 { return DEFAULT_POSTPONE_TIME; }
//	@Override public int     getDefaultQueryTimeout()                 { return DEFAULT_QUERY_TIMEOUT; }
//	@Override public boolean getDefaultIsNegativeDiffCountersToZero() { return NEGATIVE_DIFF_COUNTERS_TO_ZERO; }
	@Override public Type    getTemplateLevel()                       { return Type.LARGE; }

	/**
	 * FACTORY  method to create the object
	 */
	public static CountersModel create(ICounterController counterController, IGuiController guiController)
	{
		if (guiController != null && guiController.hasGUI())
			guiController.splashWindowProgress("Loading: Counter Model '"+CM_NAME+"'");

		return new CmOsNwInfo(counterController, guiController);
	}

	public CmOsNwInfo(ICounterController counterController, IGuiController guiController)
	{
		super(CM_NAME, GROUP_NAME, CM_TYPE, null, true);

		setDisplayName(SHORT_NAME);
		setDescription(HTML_DESC);

		setIconFile(GUI_ICON_FILE);

		setCounterController(counterController);
		setGuiController(guiController);

		// Normally for HostMonitor is ABS
		setDataSource(DATA_RATE, false);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_RECV_BANDWIDTH_KB    = "RecvKb";
	public static final String GRAPH_NAME_RECV_BANDWIDTH_MBIT  = "RecvMbit";
	public static final String GRAPH_NAME_RECV_PACKETS         = "RecvPck";
	public static final String GRAPH_NAME_TRANS_BANDWIDTH_KB   = "TransKb";
	public static final String GRAPH_NAME_TRANS_BANDWIDTH_MBIT = "TransMbit";
	public static final String GRAPH_NAME_TRANS_PACKETS        = "TransPck";
	public static final String GRAPH_NAME_ALL_BANDWIDTH_MBIT    = "AllMbit";

	private void addTrendGraphs()
	{
		// GRAPH
		addTrendGraph(GRAPH_NAME_RECV_BANDWIDTH_KB,
			"Network Received KB", 	                                           // Menu CheckBox text
			"Network Received KB per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_RECV_BANDWIDTH_MBIT,
			"Network Received Mbit", 	                                         // Menu CheckBox text
			"Network Received Mbit per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_RECV_PACKETS,
			"Network Received Packets", 	                                        // Menu CheckBox text
			"Network Received Packets per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_TRANS_BANDWIDTH_KB,
			"Network Transmitted in KB", 	                                         // Menu CheckBox text
			"Network Transmitted in KB per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_TRANS_BANDWIDTH_MBIT,
			"Network Transmitted in Mbit", 	                                         // Menu CheckBox text
			"Network Transmitted in Mbit per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",  // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height

		// GRAPH
		addTrendGraph(GRAPH_NAME_TRANS_PACKETS,
			"Network Transmitted Packets", 	                                           // Menu CheckBox text
			"Network Transmitted Packets per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			null, 
			LabelType.Dynamic,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
		
		// GRAPH
		addTrendGraph(GRAPH_NAME_ALL_BANDWIDTH_MBIT,
			"Network Received/Transmitted all NIC in Mbit", 	                                        // Menu CheckBox text
			"Network Received/Transmitted all NIC in Mbit per Sec ("+GROUP_NAME+"->"+SHORT_NAME+")",    // Label 
			new String[] {"Received", "Transmitted"}, 
			LabelType.Static,
			TrendGraphDataPoint.Category.NETWORK,
			false, // is Percent Graph
			false, // visible at start
			0,     // graph is valid from Server Version. 0 = All Versions; >0 = Valid from this version and above 
			-1);   // minimum height
	}
	
	@Override
	protected TabularCntrPanel createGui()
	{
		return new CmOsNwInfoPanel(this);
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		if (GRAPH_NAME_RECV_BANDWIDTH_KB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "interface");
				dArray[i] = this.getRateValueAsDouble(i, "r_KB");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_RECV_BANDWIDTH_MBIT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "interface");
				dArray[i] = this.getRateValueAsDouble(i, "r_Mbit");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_RECV_PACKETS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "interface");
				dArray[i] = this.getRateValueAsDouble(i, "r_packets");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_TRANS_BANDWIDTH_KB.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "interface");
				dArray[i] = this.getRateValueAsDouble(i, "t_KB");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_TRANS_BANDWIDTH_MBIT.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "interface");
				dArray[i] = this.getRateValueAsDouble(i, "t_Mbit");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_TRANS_PACKETS.equals(tgdp.getName()))
		{
			// Write 1 "line" for every device
			Double[] dArray = new Double[this.getRowCount()];
			String[] lArray = new String[dArray.length];
			for (int i = 0; i < dArray.length; i++)
			{
				lArray[i] = this.getRateString       (i, "interface");
				dArray[i] = this.getRateValueAsDouble(i, "t_packets");
			}

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), lArray, dArray);
		}

		if (GRAPH_NAME_ALL_BANDWIDTH_MBIT.equals(tgdp.getName()))
		{
			Double[] dArray = new Double[2];
			dArray[0] = this.getRateValueSum("r_Mbit");
			dArray[1] = this.getRateValueSum("t_Mbit");

			// Set the values
			tgdp.setDataPoint(this.getTimestamp(), dArray);
		}

	}

	@Override
	public void localCalculation(OsTable osSampleTable)
	{
//		System.out.println(getName()+ ": localCalculation(OsTable osSampleTable) " 
//				+ "rowcount="   + osSampleTable.getRowCount()
//				+ ", colCount=" + osSampleTable.getColumnCount()
//				+ ", ColNames=" + osSampleTable.getColNames()
//				+ " !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

		// Check/get column position
		int pos_r_bytes      = osSampleTable.findColumn("r_bytes");
		int pos_r_kb         = osSampleTable.findColumn("r_KB");
		int pos_r_mb         = osSampleTable.findColumn("r_MB");
		int pos_r_mbit       = osSampleTable.findColumn("r_Mbit");
		int pos_t_bytes      = osSampleTable.findColumn("t_bytes");
		int pos_t_kb         = osSampleTable.findColumn("t_KB");
		int pos_t_mb         = osSampleTable.findColumn("t_MB");
		int pos_t_mbit       = osSampleTable.findColumn("t_Mbit");

		int pos_r_bPerPacket = osSampleTable.findColumn("r_bPerPacket");
		int pos_r_packets    = osSampleTable.findColumn("r_packets");
		int pos_t_bPerPacket = osSampleTable.findColumn("t_bPerPacket");
		int pos_t_packets    = osSampleTable.findColumn("t_packets");
		
		
		if (    pos_r_bytes < 0 
		     || pos_r_kb    < 0
		     || pos_r_mb    < 0
		     || pos_r_mbit  < 0
		     || pos_t_bytes < 0
		     || pos_t_kb    < 0
		     || pos_t_mb    < 0
		     || pos_t_mbit  < 0

		     || pos_r_bPerPacket < 0
		     || pos_r_packets    < 0
		     || pos_t_bPerPacket < 0
		     || pos_t_packets    < 0
		   )
		{
			_logger.warn(getName()      + ".localCalculation(OsTable) could not find all desired columns"
				+ ". pos_r_bytes="      + pos_r_bytes
				+ ", pos_r_kb="         + pos_r_kb
				+ ", pos_r_mb="         + pos_r_mb
				+ ", pos_r_mbit="       + pos_r_mbit
				+ ", pos_r_bPerPacket=" + pos_r_bPerPacket
				+ ", pos_r_packets="    + pos_r_packets

				+ ", pos_t_bytes="      + pos_t_bytes
				+ ", pos_t_kb="         + pos_t_kb
				+ ", pos_t_mb="         + pos_t_mb
				+ ", pos_t_mbit="       + pos_t_mbit
				+ ", pos_t_bPerPacket=" + pos_t_bPerPacket
				+ ", pos_t_packets="    + pos_t_packets
				);
			return;
		}

		// Loop all rows and calculate KB and MB
		for (int r=0; r<osSampleTable.getRowCount(); r++)
		{
			// get bytes for Receive and Transmit
			Number r_bytes   = (Number) osSampleTable.getValueAt(r, pos_r_bytes);
			Number t_bytes   = (Number) osSampleTable.getValueAt(r, pos_t_bytes);
			Number r_packets = (Number) osSampleTable.getValueAt(r, pos_r_packets);
			Number t_packets = (Number) osSampleTable.getValueAt(r, pos_t_packets);

			// convert to KB and MB
			BigDecimal r_kb   = new BigDecimal( r_bytes.doubleValue() / 1024 )          .setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal r_mb   = new BigDecimal( r_bytes.doubleValue() / 1024 / 1024)    .setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal r_mbit = new BigDecimal( r_bytes.doubleValue() / 1024 / 1024 * 8).setScale(1, BigDecimal.ROUND_HALF_EVEN);

			BigDecimal t_kb   = new BigDecimal( t_bytes.doubleValue() / 1024 )          .setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal t_mb   = new BigDecimal( t_bytes.doubleValue() / 1024 / 1024)    .setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal t_mbit = new BigDecimal( t_bytes.doubleValue() / 1024 / 1024 * 8).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			
			// Calc bytesPerPackage
			int r_bPerPacket = 0;
			if (r_packets.doubleValue() > 0)
				r_bPerPacket = ((Number) (r_bytes.doubleValue() / r_packets.doubleValue()) ).intValue();

			int t_bPerPacket = 0;
			if (t_packets.doubleValue() > 0)
				t_bPerPacket = ((Number) (t_bytes.doubleValue() / t_packets.doubleValue()) ).intValue();

			// set values
			osSampleTable.setValueAt(r_kb,   r, pos_r_kb);
			osSampleTable.setValueAt(r_mb,   r, pos_r_mb);
			osSampleTable.setValueAt(r_mbit, r, pos_r_mbit);

			osSampleTable.setValueAt(t_kb,   r, pos_t_kb);
			osSampleTable.setValueAt(t_mb,   r, pos_t_mb);
			osSampleTable.setValueAt(t_mbit, r, pos_t_mbit);

			osSampleTable.setValueAt(r_bPerPacket, r, pos_r_bPerPacket);
			osSampleTable.setValueAt(t_bPerPacket, r, pos_t_bPerPacket);
		}
	}
	
	/**
	 * Calculate the difference values for Bytes Per Pakckets
	 */
	@Override
	public void localCalculation(OsTable prevSample, OsTable thisSample, OsTable diffdata)
	{
		// Check/get column position
		int pos_r_bytes      = diffdata.findColumn("r_bytes");
		int pos_r_bPerPacket = diffdata.findColumn("r_bPerPacket");
		int pos_r_packets    = diffdata.findColumn("r_packets");

		int pos_t_bytes      = diffdata.findColumn("t_bytes");
		int pos_t_bPerPacket = diffdata.findColumn("t_bPerPacket");
		int pos_t_packets    = diffdata.findColumn("t_packets");
		
		
		if (    pos_r_bytes      < 0 
		     || pos_r_packets    < 0
		     || pos_r_bPerPacket < 0

		     || pos_t_bytes      < 0
		     || pos_t_packets    < 0
		     || pos_t_bPerPacket < 0
		   )
		{
			_logger.warn(getName()      + ".localCalculation(diff) could not find all desired columns"
				+ ". pos_r_bytes="      + pos_r_bytes
				+ ", pos_r_packets="    + pos_r_packets
				+ ", pos_r_bPerPacket=" + pos_r_bPerPacket

				+ ", pos_t_bytes="      + pos_t_bytes
				+ ", pos_t_packets="    + pos_t_packets
				+ ", pos_t_bPerPacket=" + pos_t_bPerPacket
				);
			return;
		}

		// Loop all rows and calculate KB and MB
		for (int r=0; r<diffdata.getRowCount(); r++)
		{
			// get bytes for Receive and Transmit
			Number r_bytes   = (Number) diffdata.getValueAt(r, pos_r_bytes);
			Number t_bytes   = (Number) diffdata.getValueAt(r, pos_t_bytes);
			Number r_packets = (Number) diffdata.getValueAt(r, pos_r_packets);
			Number t_packets = (Number) diffdata.getValueAt(r, pos_t_packets);

			// Calc bytesPerPackage
			int r_bPerPacket = 0;
			if (r_packets.doubleValue() > 0)
				r_bPerPacket = ((Number) (r_bytes.doubleValue() / r_packets.doubleValue()) ).intValue();

			int t_bPerPacket = 0;
			if (t_packets.doubleValue() > 0)
				t_bPerPacket = ((Number) (t_bytes.doubleValue() / t_packets.doubleValue()) ).intValue();

			// set values
			diffdata.setValueAt(r_bPerPacket, r, pos_r_bPerPacket);
			diffdata.setValueAt(t_bPerPacket, r, pos_t_bPerPacket);
		}
	}
}
