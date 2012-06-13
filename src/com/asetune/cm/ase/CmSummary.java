package com.asetune.cm.ase;

import java.awt.Component;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import com.asetune.AseTune;
import com.asetune.CounterController;
import com.asetune.ICounterController;
import com.asetune.IGuiController;
import com.asetune.TrendGraphDataPoint;
import com.asetune.cm.CounterSetTemplates;
import com.asetune.cm.CounterSetTemplates.Type;
import com.asetune.cm.CountersModel;
import com.asetune.cm.SamplingCnt;
import com.asetune.cm.ase.gui.CmSummaryPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.TrendGraph;
import com.asetune.utils.AseConnectionUtils;

/**
 * @author Goran Schwarz (goran_schwarz@hotmail.com)
 */
public class CmSummary
extends CountersModel
{
	private static Logger        _logger          = Logger.getLogger(CmSummary.class);
	private static final long    serialVersionUID = 1L;

	public static final String   CM_NAME          = CmSummary.class.getSimpleName();
	public static final String   SHORT_NAME       = "Summary";
	public static final String   HTML_DESC        = 
		"<html>" +
		"Overview of how the system performs." +
		"</html>";

	public static final String   GROUP_NAME       = null;
	public static final String   GUI_ICON_FILE    = "images/"+CM_NAME+".png";

	public static final int      NEED_SRV_VERSION = 0;
	public static final int      NEED_CE_VERSION  = 0;

	public static final String[] MON_TABLES       = new String[] {"monState"};
	public static final String[] NEED_ROLES       = new String[] {"mon_role"};
	public static final String[] NEED_CONFIG      = new String[] {};

	public static final String[] PCT_COLUMNS      = new String[] {};
	public static final String[] DIFF_COLUMNS     = new String[] {
		"LockWaits", "CheckPoints", "NumDeadlocks", "Connections", "Transactions", 
        "cpu_busy", "cpu_io", "cpu_idle", "io_total_read", "io_total_write", 
        "aaConnections", "distinctLogins", 
        "pack_received", "pack_sent", "packet_errors", "total_errors"};

	public static final boolean  NEGATIVE_DIFF_COUNTERS_TO_ZERO = false;
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

		return new CmSummary(counterController, guiController);
	}

	public CmSummary(ICounterController counterController, IGuiController guiController)
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
		
		// THIS IS THE SUMMARY CM, so set this
		counterController.setSummaryCm(this);
		
		addTrendGraphs();
		
		CounterSetTemplates.register(this);
	}


	//------------------------------------------------------------
	// Implementation
	//------------------------------------------------------------
	public static final String GRAPH_NAME_AA_CPU             = "aaCpuGraph";       // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_CPU;
	public static final String GRAPH_NAME_TRANSACTION        = "TransGraph";       // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__TRANSACTION;
	public static final String GRAPH_NAME_CONNECTION         = "ConnectionsGraph"; // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__CONNECTION;
	public static final String GRAPH_NAME_AA_DISK_READ_WRITE = "aaReadWriteGraph"; // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_DISK_READ_WRITE;
	public static final String GRAPH_NAME_AA_NW_PACKET       = "aaPacketGraph";    // String x=GetCounters.CM_GRAPH_NAME__SUMMARY__AA_NW_PACKET;

	private void addTrendGraphs()
	{
		String[] labels_aaCpu       = new String[] { "System+User CPU (@@cpu_busy + @@cpu_io)", "System CPU (@@cpu_io)", "User CPU (@@cpu_busy)" };
		String[] labels_transaction = new String[] { "Transactions" };
		String[] labels_connection  = new String[] { "UserConnections", "distinctLogins", "@@connections" };
		String[] labels_aaDiskRW    = new String[] { "@@total_read", "@@total_write" };
		String[] labels_aaNwPacket  = new String[] { "@@pack_received", "@@pack_sent", "@@packet_errors" };
		
		addTrendGraphData(GRAPH_NAME_AA_CPU,             new TrendGraphDataPoint(GRAPH_NAME_AA_CPU,             labels_aaCpu));
		addTrendGraphData(GRAPH_NAME_TRANSACTION,        new TrendGraphDataPoint(GRAPH_NAME_TRANSACTION,        labels_transaction));
		addTrendGraphData(GRAPH_NAME_CONNECTION,         new TrendGraphDataPoint(GRAPH_NAME_CONNECTION,         labels_connection));
		addTrendGraphData(GRAPH_NAME_AA_DISK_READ_WRITE, new TrendGraphDataPoint(GRAPH_NAME_AA_DISK_READ_WRITE, labels_aaDiskRW));
		addTrendGraphData(GRAPH_NAME_AA_NW_PACKET,       new TrendGraphDataPoint(GRAPH_NAME_AA_NW_PACKET,       labels_aaNwPacket));

		// if GUI
		if (getGuiController() != null && getGuiController().hasGUI())
		{
			// GRAPH
			TrendGraph tg = null;
			tg = new TrendGraph(GRAPH_NAME_AA_CPU,
				"CPU Summary, Global Variables", 	                        // Menu CheckBox text
				"CPU Summary for all Engines (using @@cpu_busy, @@cpu_io)", // Label 
				labels_aaCpu, 
				true, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_TRANSACTION,
				"Transaction per second", 	                                         // Menu CheckBox text
				"Number of Transaction per Second (only in 15.0.3 esd#3 and later)", // Label 
				labels_transaction, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_CONNECTION,
				"Connections/Users in ASE", 	          // Menu CheckBox text
				"Connections/Users connected to the ASE", // Label 
				labels_connection, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_AA_DISK_READ_WRITE,
				"Disk read/write, Global Variables", 	                         // Menu CheckBox text
				"Disk read/write per second, using @@total_read, @@total_write", // Label 
				labels_aaDiskRW, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);

			tg = new TrendGraph(GRAPH_NAME_AA_NW_PACKET,
				"Network Packets received/sent, Global Variables", 	                            // Menu CheckBox text
				"Network Packets received/sent per second, using @@pack_received, @@pack_sent", // Label 
				labels_aaNwPacket, 
				false, // is Percent Graph
				this, 
				false, // visible at start
				-1);  // minimum height
			addTrendGraph(tg.getName(), tg, true);
		}
	}

	@Override
	protected JPanel createGui()
	{
		setTabPanel(null); // Don't think this is necessary, but lets do it anyway...

		CmSummaryPanel summaryPanel = new CmSummaryPanel(this);

		// THIS IS THE SUMMARY CM, so set this
		CounterController.getInstance().setSummaryPanel( summaryPanel );

		// add listener, so that the GUI gets updated when data changes in the CM
		addTableModelListener( summaryPanel );

		return summaryPanel;
	}

	@Override
	public boolean isRefreshable()
	{
		// The SUMMARY should ALWAYS be refreshed
		return true;
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
		return pkCols;
	}

	@Override
	public String getSqlForVersion(Connection conn, int aseVersion, boolean isClusterEnabled)
	{
		String nwAddrInfo = "'no sa_role'";
		if (isRuntimeInitialized())
		{
			if (isRoleActive(AseConnectionUtils.SA_ROLE))
			{
				Component guiOwner = AseTune.hasGUI() ? MainFrame.getInstance() : null;
//				nwAddrInfo = "(select min(convert(varchar(255),address_info)) from syslisteners where address_info not like 'localhost%')";
				nwAddrInfo = "'" + AseConnectionUtils.getListeners(conn, false, true, guiOwner) + "'";
			}
		}
		else
			nwAddrInfo = "'tcp listeners goes here, if we are connected'";

		String cols1, cols2, cols3;
		cols1 = cols2 = cols3 = "";

		// get all the columns for SMP servers
		// if Cluster is enabled, one row for every instance will appear
		//                        so lets try to min, max or sum the rows into 1 row...
		//                        which will "simulate" a SMP environment
		cols1 = "* \n";
		if (isClusterEnabled)
		{
			cols1 = " top 1 \n";
			cols1 += "  LockWaitThreshold   = max(LockWaitThreshold) \n";
			cols1 += ", LockWaits           = sum(LockWaits) \n";
			cols1 += ", DaysRunning         = max(DaysRunning) \n";
			cols1 += ", CheckPoints         = sum(CheckPoints) \n";
			cols1 += ", NumDeadlocks        = sum(NumDeadlocks) \n";
			cols1 += ", DiagnosticDumps     = sum(DiagnosticDumps) \n";
			cols1 += ", Connections         = sum(Connections) \n";
			cols1 += ", MaxRecovery         = avg(MaxRecovery) \n";
			cols1 += ", Transactions        = sum(Transactions) \n";
			cols1 += ", StartDate           = min(StartDate) \n";
			cols1 += ", CountersCleared     = max(CountersCleared) \n";
		}

		cols2 = ", aseVersion         = @@version \n" +
				", atAtServerName     = @@servername \n" +
				", clusterInstanceId  = " + (isClusterEnabled ? "convert(varchar(15),@@instanceid)"     : "'Not Enabled'") + " \n" + 
				", clusterCoordId     = " + (isClusterEnabled ? "convert(varchar(3), @@clustercoordid)" : "'Not Enabled'") + " \n" +
				", timeIsNow          = getdate() \n" +
				", NetworkAddressInfo = " + nwAddrInfo + " \n" +
				", asePageSize        = @@maxpagesize \n" +

				", bootcount          = @@bootcount \n" + // from 12.5.0.3
				", recovery_state     = "+ (aseVersion >= 12510 ? "@@recovery_state" : "'Introduced in ASE 12.5.1'") + " \n" +

				", cpu_busy           = @@cpu_busy \n" +
				", cpu_io             = @@io_busy \n" +
				", cpu_idle           = @@idle \n" +
				", io_total_read      = @@total_read \n" +
				", io_total_write     = @@total_write \n" +

				", aaConnections      = @@connections \n" +
				", distinctLogins     = (select count(distinct suid) from master..sysprocesses) \n" +

				// ------ column 'stat'
				// 32   - Database created with for load option, or crashed while loading database, instructs recovery not to proceed
				// 256  - Database suspect | Not recovered | Cannot be opened or used | Can be dropped only with dbcc dbrepair
				// 1024 - read only
				// 2048 - dbo use only
				// 4096 - single user
				// ------ column 'stat2'
				// 16 - Database is offline
				// 32 - Database is offline until recovery completes
				// model is used during create database... so skip this one to
				", fullTranslogCount  = (select sum(lct_admin('logfull', dbid)) from master..sysdatabases readpast \n" +
				"                        where (status & 32   != 32  ) and (status & 256  != 256 ) \n" +
				"                          and (status & 1024 != 1024) and (status & 2048 != 2048) \n" +
				"                          and (status & 4096 != 4096) \n" +
				"                          and (status2 & 16  != 16  ) and (status2 &  32 != 32  ) \n" +
				"                          and name != 'model' " + (isClusterEnabled ? "and @@instanceid = isnull(instanceid,@@instanceid)" : "") + ") \n" + 

				", pack_received      = @@pack_received \n" +
				", pack_sent          = @@pack_sent \n" +

				", packet_errors      = @@packet_errors \n" +
				", total_errors       = @@total_errors \n" +
				"";

		cols3 = "";

		String fromTable = " from master..monState A \n";

		// if NOT MON_ROLE, revrite the query a bit
		if (isRuntimeInitialized())
		{
			if ( ! isRoleActive(AseConnectionUtils.MON_ROLE))
			{
				cols1     = "dummyColumn = 1 \n";
				fromTable = "";
			}
		}
		
		String sql = "select " + cols1 + cols2 + cols3 + fromTable;
		
		return sql;
	}
	
	/** 
	 * reset some negative counters to 0
	 */
	@Override
	public void localCalculation(SamplingCnt prevSample, SamplingCnt newSample, SamplingCnt diffData)
	{
		SamplingCnt counters = diffData;
		
		if (counters == null)
			return;

		// SUMMARY only have 1 row, so this was simplest way, do not do like this on CM's that has many rows
		// Loop on all diffData rows
		for (int rowId=0; rowId < counters.getRowCount(); rowId++) 
		{
			checkAndSetNc20(counters, rowId, "Transactions");
			checkAndSetNc20(counters, rowId, "io_total_read");
			checkAndSetNc20(counters, rowId, "io_total_write");
			checkAndSetNc20(counters, rowId, "pack_received");
			checkAndSetNc20(counters, rowId, "pack_sent");
			checkAndSetNc20(counters, rowId, "packet_errors");
		}
	}
	private void checkAndSetNc20(SamplingCnt counters, int rowId, String columnName)
	{
		int colId = counters.findColumn(columnName);
		if (colId >= 0)
		{
			Object obj  = counters.getValueAt(rowId, colId);
			if (obj instanceof Number)
			{
				//System.out.println("colId="+colId+", name='"+columnName+"', o="+obj);
				if (((Number)obj).intValue() < 0)
				{
					//System.out.println("colId="+colId+", name='"+columnName+"', setting to Integer(0)");
					counters.setValueAt(new Integer(0), rowId, colId);
				}
			}
		}
	}

	@Override
	public void updateGraphData(TrendGraphDataPoint tgdp)
	{
		int aseVersion = getServerVersion();

		if (GRAPH_NAME_AA_CPU.equals(tgdp.getName()))
		{
			Double cpuUser        = getDiffValueAsDouble(0, "cpu_busy");
			Double cpuSystem      = getDiffValueAsDouble(0, "cpu_io");
			Double cpuIdle        = getDiffValueAsDouble(0, "cpu_idle");
			if (cpuUser != null && cpuSystem != null && cpuIdle != null)
			{
				double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
				double CPUUser   = cpuUser  .doubleValue();
				double CPUSystem = cpuSystem.doubleValue();
//				double CPUIdle   = cpuIdle  .doubleValue();

				BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
				BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

//				_cpuTime_txt          .setText(calcCPUTime      .toString());
//				_cpuUser_txt          .setText(calcUserCPUTime  .toString());
//				_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
//				_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());

				Double[] arr = new Double[3];

				arr[0] = calcCPUTime      .doubleValue();
				arr[1] = calcSystemCPUTime.doubleValue();
				arr[2] = calcUserCPUTime  .doubleValue();
				_logger.debug("updateGraphData(aaCpuGraph): @@cpu_busy+@@cpu_io='"+arr[0]+"', @@cpu_io='"+arr[1]+"', @@cpu_busy='"+arr[2]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		if (GRAPH_NAME_TRANSACTION.equals(tgdp.getName()))
		{
			int tranPos = -1;
			if (getCounterDataAbs() != null)
				tranPos = getCounterDataAbs().findColumn("Transactions");
			
			// if you don't have 'mon_role', the column 'Transactions' is not part of the 
			// resultset even if we are above 15.0.3 ESD#3
			if ( aseVersion < 15033 || tranPos == -1 )
			{
				// disable the transactions graph checkbox...
				TrendGraph tg = getTrendGraph(GRAPH_NAME_TRANSACTION);
				if (tg != null)
				{
					JCheckBoxMenuItem menuItem = tg.getViewMenuItem();
					if (menuItem.isSelected())
						menuItem.doClick();
				}
			}
			else
			{
				Double[] arr = new Double[1];

				arr[0] = this.getRateValueSum("Transactions");
				_logger.debug("updateGraphData(TransGraph): Transactions='"+arr[0]+"'.");

				// Set the values
				tgdp.setDate(this.getTimestamp());
				tgdp.setData(arr);
			}
		}

		if (GRAPH_NAME_CONNECTION.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getAbsValueAsDouble (0, "Connections");
			arr[1] = this.getAbsValueAsDouble (0, "distinctLogins");
			arr[2] = this.getDiffValueAsDouble(0, "aaConnections");
			_logger.debug("updateGraphData(ConnectionsGraph): Connections='"+arr[0]+"', distinctLogins='"+arr[1]+"', aaConnections='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		if (GRAPH_NAME_AA_DISK_READ_WRITE.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[2];

			arr[0] = this.getRateValueAsDouble (0, "io_total_read");
			arr[1] = this.getRateValueAsDouble (0, "io_total_write");
			_logger.debug("updateGraphData(aaReadWriteGraph): io_total_read='"+arr[0]+"', io_total_write='"+arr[1]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}

		if (GRAPH_NAME_AA_NW_PACKET.equals(tgdp.getName()))
		{	
			Double[] arr = new Double[3];

			arr[0] = this.getRateValueAsDouble (0, "pack_received");
			arr[1] = this.getRateValueAsDouble (0, "pack_sent");
			arr[2] = this.getRateValueAsDouble (0, "packet_errors");
			_logger.debug("updateGraphData(aaPacketGraph): packet_errors='"+arr[0]+"', total_errors='"+arr[1]+"', packet_errors='"+arr[2]+"'.");

			// Set the values
			tgdp.setDate(this.getTimestamp());
			tgdp.setData(arr);
		}
	}
}
