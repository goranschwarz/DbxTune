/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui;

/**
 * <p>SummaryPanel</p>
 * <p>AseTune : Display main counters and trend graphs </p>
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import com.asetune.AseTune;
import com.asetune.GetCounters;
import com.asetune.MonTablesDictionary;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;




public class SummaryPanel
extends JPanel
implements TableModelListener
{
	static Logger _logger = Logger.getLogger(SummaryPanel.class);

	private static final long serialVersionUID = 7555710440479350306L;


	private ChangeToJTabDialog _focusToBlockingTab = null;
	private ChangeToJTabDialog _focusToDatabasesTab = null;
	private Watermark          _watermark;

	private JPanel           _clusterInfoPanel;
	private JPanel           _serverInfoPanel;
	private JPanel           _dataPanel;
	private JScrollPane      _dataPanelScroll;
	private TrendGraphDashboardPanel _graphPanel;
	private JScrollPane      _graphPanelScroll;
	
	private JLabel           _title_lbl                    = new JLabel();
	private JButton          _trendGraphs_but              = new JButton();

	// CLUSTER INFO PANEL
	private JTextField       _clusterBootTime_txt          = new JTextField();
	private JLabel           _clusterBootTime_lbl          = new JLabel();
	private JTextField       _clusterName_txt              = new JTextField();
	private JLabel           _clusterName_lbl              = new JLabel();
	private JLabel           _clusterInstance_lbl          = new JLabel();
	private JTextField       _clusterInstanceName_txt      = new JTextField();
	private JTextField       _clusterInstanceId_txt        = new JTextField();
	private JLabel           _clusterCoordinator_lbl       = new JLabel();
	private JTextField       _clusterCoordinatorName_txt   = new JTextField();
	private JTextField       _clusterCoordinatorId_txt     = new JTextField();
	private JComboBox        _clusterView_cbx              = new JComboBox(new String[] {"cluster", "instance"});
	private JLabel           _clusterView_lbl              = new JLabel();

	// SERVER INFO PANEL
	private JTextField       _localServerName_txt          = new JTextField();
	private JLabel           _localServerName_lbl          = new JLabel();
	private JTextField       _atAtServerName_txt           = new JTextField();
	private JLabel           _atAtServerName_lbl           = new JLabel();
	private JTextField       _listeners_txt                = new JTextField();
	private JLabel           _listeners_lbl                = new JLabel();
	private JTextField       _aseVersion_txt               = new JTextField();
	private JLabel           _aseVersion_lbl               = new JLabel();
	private JTextField       _lastSampleTime_txt           = new JTextField();
	private JLabel           _lastSampleTime_lbl           = new JLabel();

	private JTextField       _startDate_txt                = new JTextField();
	private JLabel           _startDate_lbl                = new JLabel();
	private JTextField       _daysRunning_txt              = new JTextField();
	private JLabel           _daysRunning_lbl              = new JLabel();
	private JTextField       _countersCleared_txt          = new JTextField();
	private JLabel           _countersCleared_lbl          = new JLabel();
	private JTextField       _checkPoints_txt              = new JTextField();
	private JLabel           _checkPoints_lbl              = new JLabel();
	private JTextField       _numDeadlocks_txt             = new JTextField();
	private JTextField       _numDeadlocksDiff_txt         = new JTextField();
	private JLabel           _numDeadlocks_lbl             = new JLabel();
	private JTextField       _diagnosticDumps_txt          = new JTextField();
	private JLabel           _diagnosticDumps_lbl          = new JLabel();
	private JTextField       _connectionsDiff_txt          = new JTextField();
	private JTextField       _connections_txt              = new JTextField();
	private JLabel           _connections_lbl              = new JLabel();
	private JTextField       _distinctLoginsDiff_txt       = new JTextField();
	private JTextField       _distinctLoginsAbs_txt        = new JTextField();
	private JLabel           _distinctLogins_lbl           = new JLabel();
	private JTextField       _lockWaitThreshold_txt        = new JTextField();
	private JLabel           _lockWaitThreshold_lbl        = new JLabel();
	private JTextField       _lockWaits_txt                = new JTextField();
	private JTextField       _lockWaitsDiff_txt            = new JTextField();
	private JLabel           _lockWaits_lbl                = new JLabel();
	private JTextField       _maxRecovery_txt              = new JTextField();
	private JLabel           _maxRecovery_lbl              = new JLabel();
	private JLabel           _transactions_lbl             = new JLabel();
	private JTextField       _transactions_txt             = new JTextField();
	private JTextField       _transactionsDiff_txt         = new JTextField();
	private JTextField       _transactionsRate_txt         = new JTextField();
	private JLabel           _fullTranslog_lbl             = new JLabel();
	private JTextField       _fullTranslog_txt             = new JTextField();
	
	private JLabel           _bootcount_lbl                = new JLabel();
	private JTextField       _bootcount_txt                = new JTextField();
	private JLabel           _recoveryState_lbl            = new JLabel();
	private JTextField       _recoveryState_txt            = new JTextField();

	private JLabel           _cpuTime_lbl                  = new JLabel();
	private JTextField       _cpuTime_txt                  = new JTextField();
	private JLabel           _cpuUser_lbl                  = new JLabel();
	private JTextField       _cpuUser_txt                  = new JTextField();
	private JLabel           _cpuSystem_lbl                = new JLabel();
	private JTextField       _cpuSystem_txt                = new JTextField();
	private JLabel           _cpuIdle_lbl                  = new JLabel();
	private JTextField       _cpuIdle_txt                  = new JTextField();

	private JLabel           _ioTotalRead_lbl               = new JLabel();
	private JTextField       _ioTotalRead_txt               = new JTextField();
	private JTextField       _ioTotalReadDiff_txt           = new JTextField();
	private JLabel           _ioTotalWrite_lbl              = new JLabel();
	private JTextField       _ioTotalWrite_txt              = new JTextField();
	private JTextField       _ioTotalWriteDiff_txt          = new JTextField();

	private JLabel           _aaConnections_lbl             = new JLabel();
	private JTextField       _aaConnectionsAbs_txt          = new JTextField();
	private JTextField       _aaConnectionsDiff_txt         = new JTextField();
	private JTextField       _aaConnectionsRate_txt         = new JTextField();

	private JLabel           _packReceived_lbl              = new JLabel();
	private JTextField       _packReceived_txt              = new JTextField();
	private JTextField       _packReceivedDiff_txt          = new JTextField();
	private JLabel           _packSent_lbl                  = new JLabel();
	private JTextField       _packSent_txt                  = new JTextField();
	private JTextField       _packSentDiff_txt              = new JTextField();
	private JLabel           _packetErrors_lbl              = new JLabel();
	private JTextField       _packetErrors_txt              = new JTextField();
	private JTextField       _packetErrorsDiff_txt          = new JTextField();
	private JLabel           _totalErrors_lbl               = new JLabel();
	private JTextField       _totalErrors_txt               = new JTextField();
	private JTextField       _totalErrorsDiff_txt           = new JTextField();
	
	// implements singleton pattern
	private static SummaryPanel _instance = null;

	public static final String CM_NAME = "CMsummary";
	public String getName()
	{
		return CM_NAME;
	}

	public static SummaryPanel getInstance()
	{
		if ( _instance == null )
		{
			_instance = new SummaryPanel();
		}
		return _instance;
	}

	public static boolean hasInstance()
	{
		return _instance != null;
	}

	public static void setInstance(SummaryPanel sumPanel)
	{
		_instance = sumPanel;
	}

	
	public SummaryPanel()
	{
		_instance = this;
		try
		{
			initComponents();
		}
		catch (Exception ex)
		{
			_logger.error("Cant create the summary panel", ex);
		}
	}

	private void initComponents() 
	throws Exception
	{
		setLayout(new BorderLayout());
		_graphPanel      = createGraphPanel();
		_dataPanel       = createDataPanel();

		_graphPanelScroll = new JScrollPane(_graphPanel);
		_dataPanelScroll  = new JScrollPane(_dataPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		_graphPanelScroll.getVerticalScrollBar().setUnitIncrement(16);
		_dataPanelScroll.getVerticalScrollBar().setUnitIncrement(16);

		JSplitPane  split  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _dataPanelScroll, _graphPanelScroll);
		add(split, BorderLayout.CENTER);

		// set a Decorator to the panel, that will show text: "Not Connected..." etc
		_watermark = new Watermark(_graphPanelScroll, "Not Connected...");

		// load saved properties
		loadProps();

		// assign actions for the components
		// NOTE: if this is done BEFORE loadProps(), the setting values to clusterInstance combobox
		// and history spinner will trigger the components actions and save the "faulty" default
		// values, which will then be read by the loadProps()... hmmmm...
		initComponentActions();
	}

	private void initComponentActions() 
	throws Exception
	{
		_clusterView_cbx.addActionListener(new ActionListener()
		{
			
			public void actionPerformed(ActionEvent e)
			{
				String choice = (String) _clusterView_cbx.getSelectedItem();
				int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;
				if (choice != null && choice.equalsIgnoreCase("instance"))
					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE;
				else
					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER;

				if (AseTune.getCounterCollector().isMonConnected())
					AseConnectionUtils.setClusterEditionSystemView(AseTune.getCounterCollector().getMonConnection(), systemView);

				saveProps();
			}
		});
	}

	private JPanel createDataPanel() 
	{
		JPanel panel = SwingUtils.createPanel("title", false);
		panel.setLayout(new MigLayout("", "5[grow]5", ""));

		_title_lbl.setFont(new java.awt.Font("Dialog", 1, 16));
		_title_lbl.setText("Summary panel");

		// create new panel
		_clusterInfoPanel = createClusterInfoPanel();
		_serverInfoPanel  = createServerInfoPanel();

		// Fix up the _optionTrendGraphs_but
		TrendGraph.createGraphAccessButton(_trendGraphs_but, CM_NAME);

		panel.add(_title_lbl,           "pushx, growx, left, split");
		panel.add(_trendGraphs_but,     "top, wrap");

		panel.add(_clusterInfoPanel,    "growx, width 275lp, hidemode 3, wrap"); //275 logical pixels
		panel.add(_serverInfoPanel,     "growx, width 275lp,             wrap"); //275 logical pixels

//		panel.setMinimumSize(new Dimension(50, 50));

		return panel;
	}

	private JPanel createClusterInfoPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Cluster Information", true);
		panel.setLayout(new MigLayout("", "[] [grow]", ""));

		String tooltip = "";

		tooltip = "The ASE Cluster was booted/started at this time. (select @@clusterboottime)";
		_clusterBootTime_lbl    .setText("Cluster Boot Time");
		_clusterBootTime_lbl    .setToolTipText(tooltip);
		_clusterBootTime_txt    .setToolTipText(tooltip);
		_clusterBootTime_txt    .setEditable(false);

		tooltip = "The ASE Cluster Name we are connected to. (select @@clustername)";
		_clusterName_lbl        .setText("Cluster Name");
		_clusterName_lbl        .setToolTipText(tooltip);
		_clusterName_txt        .setToolTipText(tooltip);
		_clusterName_txt        .setEditable(false);

		tooltip = "The ASE Cluster Instance Name and ID we are connected to. (select @@instancename, @@instanceid)";
		_clusterInstance_lbl.setText("Instance Name, Id");
		_clusterInstance_lbl.setToolTipText(tooltip);
		_clusterInstanceName_txt.setToolTipText(tooltip);
		_clusterInstanceName_txt.setEditable(false);
		_clusterInstanceId_txt  .setToolTipText(tooltip);
		_clusterInstanceId_txt  .setEditable(false);

		tooltip = "The ASE Cluster Coordinator Name and ID. (select instance_name(@@clustercoordid), @@clustercoordid)";
		_clusterCoordinator_lbl    .setText("Coordinator Name, Id");
		_clusterCoordinator_lbl    .setToolTipText(tooltip);
		_clusterCoordinatorName_txt.setToolTipText(tooltip);
		_clusterCoordinatorName_txt.setEditable(false);
		_clusterCoordinatorId_txt  .setToolTipText(tooltip);
		_clusterCoordinatorId_txt  .setEditable(false);
		
		tooltip = "<html>" +
		              "View counters for the whole Cluster or just the instance we connected to. (set system_view cluster|instance)" +
		              "<ul>" +
		                 "<li>cluster - Show Counters for <b>all instances</b> within the cluster.</li>" +
		                 "<li>instance - Only Counters for the <b>local instance</b> is showed.</li>" +
		              "</ul>" +
		          "</html>";
		_clusterView_lbl        .setText("System View");
		_clusterView_lbl        .setToolTipText(tooltip);
		_clusterView_cbx        .setToolTipText(tooltip);
//		_clusterView_cbx        .setEditable(true);


		
		//--------------------------
		// DO the LAYOUT
		//--------------------------
		panel.add(_clusterBootTime_lbl,       "");
		panel.add(_clusterBootTime_txt,       "growx, wrap");
		
		panel.add(_clusterName_lbl,           "");
		panel.add(_clusterName_txt,           "growx, wrap");
		
		panel.add(_clusterInstance_lbl,       "");
		panel.add(_clusterInstanceName_txt,   "growx, split");
		panel.add(_clusterInstanceId_txt,     "growx, wrap");
		
		panel.add(_clusterCoordinator_lbl,    "");
		panel.add(_clusterCoordinatorName_txt,"growx, split");
		panel.add(_clusterCoordinatorId_txt,  "growx, wrap");

		panel.add(_clusterView_lbl,           "");
		panel.add(_clusterView_cbx,           "growx, wrap");

		// this panel will only be visible when you connect to a Cluster Enabled Server
		panel.setVisible(false);

		return panel;
	}


	private JPanel createServerInfoPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Server Information", true);
		panel.setLayout(new MigLayout("", "[] [grow]", ""));

		String tooltip = "";

		tooltip = "The name we used when "+Version.getAppName()+" connected to the server, meaning name in sql.ini or inetfaces ";
		_localServerName_lbl  .setText("Local server name");
		_localServerName_lbl  .setToolTipText(tooltip);
		_localServerName_txt  .setToolTipText(tooltip);
		_localServerName_txt  .setEditable(false);

		tooltip = "This is the internal server name in the ASE, taken from the global variable @@servername";
		_atAtServerName_lbl   .setText("@@servername");
		_atAtServerName_lbl   .setToolTipText(tooltip);
		_atAtServerName_txt   .setToolTipText(tooltip);
		_atAtServerName_txt   .setEditable(false);

		tooltip = "Hostname that the ASE server has listener services on, this makes it easier to see what physical machine we have connected to.";
		_listeners_lbl        .setText("ASE Port listeners");
		_listeners_lbl        .setToolTipText(tooltip);
		_listeners_txt        .setToolTipText(tooltip);
		_listeners_txt        .setEditable(false);

		tooltip = "The version string taken from @@version";
		_aseVersion_lbl       .setText("ASE Version");
		_aseVersion_lbl       .setToolTipText(tooltip);
		_aseVersion_txt       .setToolTipText(tooltip);
		_aseVersion_txt       .setEditable(false);

		tooltip = "Time of list sample.";
		_lastSampleTime_lbl   .setText("Sample time");
		_lastSampleTime_lbl   .setToolTipText(tooltip);
		_lastSampleTime_txt   .setToolTipText(tooltip);
		_lastSampleTime_txt   .setEditable(false);


		
		
		tooltip = "Date and time that the ASE was started.";
		_startDate_lbl        .setText("Start date");
		_startDate_lbl        .setToolTipText(tooltip);
		_startDate_txt        .setToolTipText(tooltip);
		_startDate_txt        .setEditable(false);

		tooltip = "Number of days that the ASE has been running for.";
		_daysRunning_lbl      .setText("Days running");
		_daysRunning_lbl      .setToolTipText(tooltip);
		_daysRunning_txt      .setToolTipText(tooltip);
		_daysRunning_txt      .setEditable(false);

		tooltip = "Date and time at which the monitor counters were last cleared.";
		_countersCleared_lbl  .setText("Counters clear date");
		_countersCleared_lbl  .setToolTipText(tooltip);
		_countersCleared_txt  .setToolTipText(tooltip);
		_countersCleared_txt  .setEditable(false);

		tooltip = "Whether any checkpoint is currently running.";
		_checkPoints_lbl      .setText("Running checkpoint");
		_checkPoints_lbl      .setToolTipText(tooltip);
		_checkPoints_txt      .setToolTipText(tooltip);
		_checkPoints_txt      .setEditable(false);

		tooltip = "Total number of deadlocks that have occurred.";
		_numDeadlocks_lbl     .setText("Number of deadlock");
		_numDeadlocks_lbl     .setToolTipText(tooltip);
		_numDeadlocks_txt     .setToolTipText(tooltip);
		_numDeadlocks_txt     .setEditable(false);
		_numDeadlocksDiff_txt .setEditable(false);
		_numDeadlocksDiff_txt .setToolTipText("The difference since previous sample.");

		tooltip = "Whether the Sybmon diagnostic utility is performing a shared memory dump.";
		_diagnosticDumps_lbl  .setText("Diagnostics Dumps");
		_diagnosticDumps_lbl  .setToolTipText(tooltip);
		_diagnosticDumps_txt  .setToolTipText(tooltip);
		_diagnosticDumps_txt  .setEditable(false);

		tooltip = "Number of active inbound connections.";
		_connections_lbl      .setText("Connections");
		_connections_lbl      .setToolTipText(tooltip);
		_connections_txt      .setToolTipText(tooltip);
		_connections_txt      .setEditable(false);
		_connectionsDiff_txt  .setEditable(false);
		_connectionsDiff_txt  .setToolTipText("The difference since previous sample.");

		tooltip = "Number of distinct User Names that is logged in to ASE.";
		_distinctLogins_lbl    .setText("Distinct Logins");
		_distinctLogins_lbl    .setToolTipText(tooltip);
		_distinctLoginsAbs_txt .setToolTipText(tooltip);
		_distinctLoginsAbs_txt .setEditable(false);
		_distinctLoginsDiff_txt.setEditable(false);
		_distinctLoginsDiff_txt.setToolTipText("The difference since previous sample.");

		tooltip = "Time (in seconds) that processes must have waited for locks in order to be reported.";
		_lockWaitThreshold_lbl.setText("Lock wait threshold");
		_lockWaitThreshold_lbl.setToolTipText(tooltip);
		_lockWaitThreshold_txt.setToolTipText(tooltip);
		_lockWaitThreshold_txt.setEditable(false);

		tooltip = "Number of processes that have waited longer than LockWaitThreshold seconds.";
		_lockWaits_lbl        .setText("Lock waits");
		_lockWaits_lbl        .setToolTipText(tooltip);
		_lockWaits_txt        .setToolTipText(tooltip);
		_lockWaits_txt        .setEditable(false);
		_lockWaitsDiff_txt    .setEditable(false);
		_lockWaitsDiff_txt    .setToolTipText("The difference since previous sample.");

		tooltip = "The maximum time (in minutes), per database, that ASE uses to complete its recovery procedures in case of a system failure, the current 'Run Value' for the 'recovery interval in minutes' configuration option.";
		_maxRecovery_lbl      .setText("Max recovery");
		_maxRecovery_lbl      .setToolTipText(tooltip);
		_maxRecovery_txt      .setToolTipText(tooltip);
		_maxRecovery_txt      .setEditable(false);
		
		tooltip = "Number of transactions executed (abs, diff, rate), Only available from 15.0.3 CE or 15.5 (not to be trusted in a Cluster Server).";
		_transactions_lbl     .setText("Transactions");
		_transactions_lbl     .setToolTipText(tooltip);
		_transactions_txt     .setToolTipText(tooltip);
		_transactions_txt     .setEditable(false);
		_transactionsDiff_txt .setEditable(false);
		_transactionsDiff_txt .setToolTipText(tooltip);
		_transactionsRate_txt .setEditable(false);
		_transactionsRate_txt .setToolTipText(tooltip);
		
		tooltip = "Number of databases that has a full transaction log, which probably means suspended SPID's.";
		_fullTranslog_lbl.setText("Full Transaction Logs");
		_fullTranslog_lbl.setToolTipText(tooltip);
		_fullTranslog_txt.setToolTipText(tooltip);
		_fullTranslog_txt.setEditable(false);


		
		
		tooltip = "How many times has this ASE been rebooted.";
		_bootcount_lbl      .setText("Boot Count");
		_bootcount_lbl      .setToolTipText(tooltip);
		_bootcount_txt      .setToolTipText(tooltip);
		_bootcount_txt      .setEditable(false);

		tooltip = "If the ASE is in-recovery, this would be the reason.";
		_recoveryState_lbl      .setText("Recovery State");
		_recoveryState_lbl      .setToolTipText(tooltip);
		_recoveryState_txt      .setToolTipText(tooltip);
		_recoveryState_txt      .setEditable(false);

		tooltip = "CPU Time. Global variable @@cpu_busy + @@io_busy.";
//		_cpuTime_lbl      .setText("CPU Usage");
		_cpuTime_lbl      .setText("CPU Time/User/Sys");
		_cpuTime_lbl      .setToolTipText(tooltip);
		_cpuTime_txt      .setToolTipText(tooltip);
		_cpuTime_txt      .setEditable(false);

		tooltip = "CPU Busy. Global variable @@cpu_busy.";
		_cpuUser_lbl      .setText("CPU User");
		_cpuUser_lbl      .setToolTipText(tooltip);
		_cpuUser_txt      .setToolTipText(tooltip);
		_cpuUser_txt      .setEditable(false);

		tooltip = "CPU spent in IO. Global variable @@io_busy.";
		_cpuSystem_lbl      .setText("CPU System");
		_cpuSystem_lbl      .setToolTipText(tooltip);
		_cpuSystem_txt      .setToolTipText(tooltip);
		_cpuSystem_txt      .setEditable(false);

		tooltip = "CPU Idle. Global variable @@idle.";
		_cpuIdle_lbl      .setText("CPU Idle");
		_cpuIdle_lbl      .setToolTipText(tooltip);
		_cpuIdle_txt      .setToolTipText(tooltip);
		_cpuIdle_txt      .setEditable(false);

		tooltip = "Total Read IO's. Global variable @@total_read.";
		_ioTotalRead_lbl      .setText("IO Read");
		_ioTotalRead_lbl      .setToolTipText(tooltip);
		_ioTotalRead_txt      .setToolTipText(tooltip);
		_ioTotalRead_txt      .setEditable(false);
		_ioTotalReadDiff_txt  .setEditable(false);
		_ioTotalReadDiff_txt  .setToolTipText(tooltip);

		tooltip = "Total Write IO's. Global variable @@total_write.";
		_ioTotalWrite_lbl      .setText("IO Write");
		_ioTotalWrite_lbl      .setToolTipText(tooltip);
		_ioTotalWrite_txt      .setToolTipText(tooltip);
		_ioTotalWrite_txt      .setEditable(false);
		_ioTotalWriteDiff_txt  .setEditable(false);
		_ioTotalWriteDiff_txt  .setToolTipText(tooltip);

		tooltip = "Total Connection that was attemped to make to the ASE Server, even those that failes. Global variable @@connections. (abs/diff/rate)";
		_aaConnections_lbl     .setText("Connections Tried");
		_aaConnections_lbl     .setToolTipText(tooltip);
		_aaConnectionsAbs_txt  .setEditable(false);
		_aaConnectionsAbs_txt  .setToolTipText(tooltip);
		_aaConnectionsDiff_txt .setEditable(false);
		_aaConnectionsDiff_txt .setToolTipText(tooltip);
		_aaConnectionsRate_txt .setEditable(false);
		_aaConnectionsRate_txt .setToolTipText(tooltip);

		tooltip = "Total Network Packets Received. Global variable @@pack_received.";
		_packReceived_lbl      .setText("NW Packet Received");
		_packReceived_lbl      .setToolTipText(tooltip);
		_packReceived_txt      .setToolTipText(tooltip);
		_packReceived_txt      .setEditable(false);
		_packReceivedDiff_txt  .setEditable(false);
		_packReceivedDiff_txt  .setToolTipText(tooltip);

		tooltip = "Total Network Packets Sent. Global variable @@pack_sent.";
		_packSent_lbl      .setText("NW Packet Sent");
		_packSent_lbl      .setToolTipText(tooltip);
		_packSent_txt      .setToolTipText(tooltip);
		_packSent_txt      .setEditable(false);
		_packSentDiff_txt  .setEditable(false);
		_packSentDiff_txt  .setToolTipText(tooltip);

		tooltip = "Total Network Packets Errors. Global variable @@packet_errors.";
		_packetErrors_lbl      .setText("NW Packet Errors");
		_packetErrors_lbl      .setToolTipText(tooltip);
		_packetErrors_txt      .setToolTipText(tooltip);
		_packetErrors_txt      .setEditable(false);
		_packetErrorsDiff_txt  .setEditable(false);
		_packetErrorsDiff_txt  .setToolTipText(tooltip);

		tooltip = "Total Errors. Global variable @@total_errors.";
		_totalErrors_lbl      .setText("Total Errors");
		_totalErrors_lbl      .setToolTipText(tooltip);
		_totalErrors_txt      .setToolTipText(tooltip);
		_totalErrors_txt      .setEditable(false);
		_totalErrorsDiff_txt  .setEditable(false);
		_totalErrorsDiff_txt  .setToolTipText(tooltip);

		
		//--------------------------
		// DO the LAYOUT
		//--------------------------
		panel.add(_localServerName_lbl,   "");
		panel.add(_localServerName_txt,   "growx, wrap");
		
		panel.add(_atAtServerName_lbl,    "");
		panel.add(_atAtServerName_txt,    "growx, wrap");
		
		panel.add(_listeners_lbl,         "");
		panel.add(_listeners_txt,         "growx, wrap");
		
		panel.add(_aseVersion_lbl,        "");
		panel.add(_aseVersion_txt,        "growx, wrap");
		
		panel.add(_lastSampleTime_lbl,    "");
		panel.add(_lastSampleTime_txt,    "growx, wrap");
		


		panel.add(_startDate_lbl,         "gapy 20");
		panel.add(_startDate_txt,         "growx, wrap");
		
		panel.add(_daysRunning_lbl,       "");
		panel.add(_daysRunning_txt,       "growx, wrap");
		
		panel.add(_countersCleared_lbl,   "");
		panel.add(_countersCleared_txt,   "growx, wrap");
		
		panel.add(_checkPoints_lbl,       "");
		panel.add(_checkPoints_txt,       "growx, wrap");
		
		panel.add(_numDeadlocks_lbl,      "");
		panel.add(_numDeadlocks_txt,      "growx, split");
		panel.add(_numDeadlocksDiff_txt,  "growx, wrap");
		
		panel.add(_diagnosticDumps_lbl,   "");
		panel.add(_diagnosticDumps_txt,   "growx, wrap");
		
		panel.add(_connections_lbl,       "");
		panel.add(_connections_txt,       "growx, split");
		panel.add(_connectionsDiff_txt,   "growx, wrap");
		
		panel.add(_distinctLogins_lbl,    "");
		panel.add(_distinctLoginsAbs_txt, "growx, split");
		panel.add(_distinctLoginsDiff_txt,"growx, wrap");
		
		panel.add(_lockWaitThreshold_lbl, "");
		panel.add(_lockWaitThreshold_txt, "growx, wrap");
		
		panel.add(_lockWaits_lbl,         "");
		panel.add(_lockWaits_txt,         "growx, split");
		panel.add(_lockWaitsDiff_txt,     "growx, wrap");
		
		panel.add(_maxRecovery_lbl,       "");
		panel.add(_maxRecovery_txt,       "growx, wrap");
		
		panel.add(_transactions_lbl,      "");
		panel.add(_transactions_txt,      "growx, split");
		panel.add(_transactionsDiff_txt,  "growx, split");
		panel.add(_transactionsRate_txt,  "growx, wrap");
		
		panel.add(_fullTranslog_lbl,      "");
		panel.add(_fullTranslog_txt,      "growx, wrap");
		

		
		panel.add(_bootcount_lbl,         "gapy 20");
		panel.add(_bootcount_txt,         "growx, wrap");
		
		panel.add(_recoveryState_lbl,     "");
		panel.add(_recoveryState_txt,     "growx, wrap");
		
//		panel.add(_cpuTime_lbl,           "");
//		panel.add(_cpuTime_txt,           "growx, wrap");
//		panel.add(_cpuUser_lbl,           "");
//		panel.add(_cpuUser_txt,           "growx, wrap");
//		panel.add(_cpuSystem_lbl,         "");
//		panel.add(_cpuSystem_txt,         "growx, wrap");

		panel.add(_cpuTime_lbl,           "");
		panel.add(_cpuTime_txt,           "growx, split");
		panel.add(_cpuUser_txt,           "growx, split");
		panel.add(_cpuSystem_txt,         "growx, wrap");

		panel.add(_cpuIdle_lbl,           "");
		panel.add(_cpuIdle_txt,           "growx, wrap");
		
		panel.add(_ioTotalRead_lbl,       "");
		panel.add(_ioTotalRead_txt,       "growx, split");
		panel.add(_ioTotalReadDiff_txt,   "growx, wrap");
		
		panel.add(_ioTotalWrite_lbl,      "");
		panel.add(_ioTotalWrite_txt,      "growx, split");
		panel.add(_ioTotalWriteDiff_txt,  "growx, wrap");
		
		panel.add(_aaConnections_lbl,     "");
		panel.add(_aaConnectionsAbs_txt,  "growx, split");
		panel.add(_aaConnectionsDiff_txt, "growx, split");
		panel.add(_aaConnectionsRate_txt, "growx, wrap");

		panel.add(_packReceived_lbl,      "");
		panel.add(_packReceived_txt,      "growx, split");
		panel.add(_packReceivedDiff_txt,  "growx, wrap");
		
		panel.add(_packSent_lbl,          "");
		panel.add(_packSent_txt,          "growx, split");
		panel.add(_packSentDiff_txt,      "growx, wrap");
		
		panel.add(_packetErrors_lbl,      "");
		panel.add(_packetErrors_txt,      "growx, split");
		panel.add(_packetErrorsDiff_txt,  "growx, wrap");
		
		panel.add(_totalErrors_lbl,       "");
		panel.add(_totalErrors_txt,       "growx, split");
		panel.add(_totalErrorsDiff_txt,   "growx, wrap");
		
		return panel;
	}
	
	private TrendGraphDashboardPanel createGraphPanel() 
	{
		return new TrendGraphDashboardPanel();
	}

	public TrendGraphDashboardPanel getGraphPanel()
	{
		return _graphPanel;
	}
	
	
	public void clearGraph()
	{
		_graphPanel.clearGraph();
	}

	public void addTrendGraph(TrendGraph tg)
	{
		_graphPanel.add(tg);
	}

	public void setLocalServerName(String name) 
	{ 
		_localServerName_txt.setText(name); 
		_localServerName_txt.setCaretPosition(0);
	}

	public String getLocalServerName() { return _localServerName_txt.getText(); }
	public String getCountersCleared() { return _countersCleared_txt.getText(); }

	
	
	// implementing: TableModelListener
	@SuppressWarnings("unused")
	public void tableChanged(TableModelEvent e)
	{
//		TableModel tm = (TableModel) e.getSource();
		Object source = e.getSource();
		int column    = e.getColumn();
		int firstRow  = e.getFirstRow();
		int lastRow   = e.getLastRow();
		int type      = e.getType();
//		System.out.println("=========TableModelEvent: type="+type+", column="+column+", firstRow="+firstRow+", lastRow="+lastRow);
//		System.out.println("=========TableModelEvent: sourceClass='"+source.getClass().getName()+"', source='"+source+"'.");

		// event: AbstactTableModel.fireTableStructureChanged
		if (column == -1 && firstRow == -1 && lastRow == -1)
		{
		}

		// Do not update values if we are viewing in-memory storage
		if (MainFrame.isInMemoryViewOn())
			return;

		CountersModel cm = GetCounters.getCmByName(CM_NAME);
		if (cm != null && cm.hasAbsData() )
			setSummaryData(cm);
	}

	private void refreshClusterInfo()
	{
		Connection conn = AseTune.getCounterCollector().getMonConnection();

		// Cluster View
		int clusterView = AseConnectionUtils.getClusterEditionSystemView(conn);
		if (clusterView == AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER)  _clusterView_cbx.setSelectedItem("cluster");
		if (clusterView == AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE) _clusterView_cbx.setSelectedItem("instance");

		// The other cluster member fields
		String sql = "select @@clusterboottime, @@clustername, @@instanceid, @@instancename, @@clustercoordid, instance_name(@@clustercoordid)";
		//sql = "select getdate(), 'CE_DS', 1, 'CE_1_DS', 2, 'CE_2_DS'";
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);
			while(rs.next())
			{
				_clusterBootTime_txt       .setText( rs.getString(1) );
				_clusterName_txt           .setText( rs.getString(2) );
				_clusterInstanceId_txt     .setText( rs.getString(3) );
				_clusterInstanceName_txt   .setText( rs.getString(4) );
				_clusterCoordinatorId_txt  .setText( rs.getString(5) );
				_clusterCoordinatorName_txt.setText( rs.getString(6) );
			}
			rs.close();
			stmnt.close();
		}
		catch (SQLException e)
		{
			_logger.warn("Problems when executing sql: "+sql, e);
		}
		
		if ( ! "".equals(_clusterName_txt.getText()) )
			_clusterInfoPanel.setVisible(true);
	}

	public void setSummaryData(CountersModel cm)
	{
		setWatermark();

		String clusterId       = cm.getAbsString (0, "clusterInstanceId");
		String currClusterName = _clusterInstanceName_txt.getText();
		if (clusterId != null && currClusterName.equals(""))
		{
			if (MonTablesDictionary.getInstance() != null && MonTablesDictionary.getInstance().isClusterEnabled)
			{
				refreshClusterInfo();
			}
		}

//		_localServerName_txt  .setText();
		_atAtServerName_txt    .setText(cm.getAbsString (0, "atAtServerName"));
		_listeners_txt         .setText(cm.getAbsString (0, "NetworkAddressInfo"));
		_listeners_txt         .setCaretPosition(0);
		_aseVersion_txt        .setText(cm.getAbsString (0, "aseVersion").replaceFirst("Adaptive Server Enterprise/", ""));
		_aseVersion_txt        .setCaretPosition(0);
		_lastSampleTime_txt    .setText(cm.getAbsString (0, "timeIsNow"));

		_startDate_txt         .setText(cm.getAbsString (0, "StartDate"));
		_daysRunning_txt       .setText(cm.getAbsString (0, "DaysRunning"));
		_countersCleared_txt   .setText(cm.getAbsString (0, "CountersCleared"));
		_checkPoints_txt       .setText(cm.getAbsString (0, "CheckPoints"));
		_numDeadlocks_txt      .setText(cm.getAbsString (0, "NumDeadlocks"));
		_numDeadlocksDiff_txt  .setText(cm.getDiffString(0, "NumDeadlocks"));
		_diagnosticDumps_txt   .setText(cm.getAbsString (0, "DiagnosticDumps"));
		_connections_txt       .setText(cm.getAbsString (0, "Connections"));
		_connectionsDiff_txt   .setText(cm.getDiffString(0, "Connections"));
		_distinctLoginsAbs_txt .setText(cm.getAbsString (0, "distinctLogins"));
		_distinctLoginsDiff_txt.setText(cm.getDiffString(0, "distinctLogins"));
		_lockWaitThreshold_txt .setText(cm.getAbsString (0, "LockWaitThreshold"));
		_lockWaits_txt         .setText(cm.getAbsString (0, "LockWaits"));
		_lockWaitsDiff_txt     .setText(cm.getDiffString(0, "LockWaits"));
		_maxRecovery_txt       .setText(cm.getAbsString (0, "MaxRecovery"));

		if (cm.findColumn("Transactions") >= 0)
		{
			_transactions_txt     .setText(cm.getAbsString (0, "Transactions"));
			_transactionsDiff_txt .setText(cm.getDiffString(0, "Transactions"));
			_transactionsRate_txt .setText(cm.getRateString(0, "Transactions"));
		}
		else
		{
			_transactions_txt     .setText("Not available");
			_transactionsDiff_txt .setText("");
			_transactionsRate_txt .setText("");
		}
		_fullTranslog_txt     .setText(cm.getAbsString (0, "fullTranslogCount"));

		_bootcount_txt        .setText(cm.getAbsString (0, "bootcount"));
		_recoveryState_txt    .setText(cm.getAbsString (0, "recovery_state"));
//		_cpuBusy_txt          .setText(cm.getDiffString(0, "cpu_busy"));
//		_cpuIo_txt            .setText(cm.getDiffString(0, "cpu_io"));
//		_cpuIdle_txt          .setText(cm.getDiffString(0, "cpu_idle"));
		_ioTotalRead_txt      .setText(cm.getAbsString (0, "io_total_read"));
		_ioTotalReadDiff_txt  .setText(cm.getRateString(0, "io_total_read"));
		_ioTotalWrite_txt     .setText(cm.getAbsString (0, "io_total_write"));
		_ioTotalWriteDiff_txt .setText(cm.getRateString(0, "io_total_write"));
		_aaConnectionsAbs_txt .setText(cm.getAbsString (0, "aaConnections"));
		_aaConnectionsDiff_txt.setText(cm.getDiffString(0, "aaConnections"));
		_aaConnectionsRate_txt.setText(cm.getRateString(0, "aaConnections"));
		_packReceived_txt     .setText(cm.getAbsString (0, "pack_received"));
		_packReceivedDiff_txt .setText(cm.getRateString(0, "pack_received"));
		_packSent_txt         .setText(cm.getAbsString (0, "pack_sent"));
		_packSentDiff_txt     .setText(cm.getRateString(0, "pack_sent"));
		_packetErrors_txt     .setText(cm.getAbsString (0, "packet_errors"));
		_packetErrorsDiff_txt .setText(cm.getDiffString(0, "packet_errors"));
		_totalErrors_txt      .setText(cm.getAbsString (0, "total_errors"));
		_totalErrorsDiff_txt  .setText(cm.getDiffString(0, "total_errors"));

		Double cpuUser        = cm.getDiffValueAsDouble(0, "cpu_busy");
		Double cpuSystem      = cm.getDiffValueAsDouble(0, "cpu_io");
		Double cpuIdle        = cm.getDiffValueAsDouble(0, "cpu_idle");
		if (cpuUser != null && cpuSystem != null && cpuIdle != null)
		{
			double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
			double CPUUser   = cpuUser  .doubleValue();
			double CPUSystem = cpuSystem.doubleValue();
			double CPUIdle   = cpuIdle  .doubleValue();

			BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
			BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);

			_cpuTime_txt          .setText(calcCPUTime      .toString());
			_cpuUser_txt          .setText(calcUserCPUTime  .toString());
			_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
			_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());
		}
		
		// Check LOCK WAITS and, do notification
		int lockWaits = 0;
		try { lockWaits = Integer.parseInt(_lockWaits_txt.getText()); }
		catch (NumberFormatException ignore) {}
		_logger.debug("LOCK-WAITS="+lockWaits+", TEXT='"+_lockWaits_txt.getText()+"'.");
		if (lockWaits > 0) // Disabled for the moment
		{
			MainFrame.getInstance().setBlockingLocks(true, lockWaits);

			String toTabName = "Blocking";
			if ( _focusToBlockingTab == null )
				_focusToBlockingTab = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Blocking Locks in the ASE Server", MainFrame.getTabbedPane(), toTabName);
			_focusToBlockingTab.setVisible(true);
		}
		else
			MainFrame.getInstance().setBlockingLocks(false, 0);
		// end: Check LOCK WAITS and, do notification

		// Check FULL LOGS and, do notification
		int fullLogs = 0;
		try { fullLogs = Integer.parseInt(_fullTranslog_txt.getText()); }
		catch (NumberFormatException ignore) {}
		_logger.debug("FULL-LOG="+lockWaits+", TEXT='"+_fullTranslog_txt.getText()+"'.");
		if (fullLogs > 0) // Disabled for the moment
		{
			MainFrame.getInstance().setFullTransactionLog(true, fullLogs);

			String toTabName = "Databases";
			if ( _focusToDatabasesTab == null )
				_focusToDatabasesTab = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Full Database Transaction Logs in the ASE Server", MainFrame.getTabbedPane(), toTabName);
			_focusToDatabasesTab.setVisible(true);
		}
		else
			MainFrame.getInstance().setFullTransactionLog(false, 0);
		// end: Check FULL LOGS and, do notification
	}

	public synchronized void clearSummaryData()
	{
		setWatermark();

		// Cluster info
		_clusterBootTime_txt       .setText("");
		_clusterName_txt           .setText("");
		_clusterInstanceName_txt   .setText("");
		_clusterInstanceId_txt     .setText("");
		_clusterCoordinatorName_txt.setText("");
		_clusterCoordinatorId_txt  .setText("");
//		_clusterView_cbx           .setSelectedItem("cluster"); // do not set this, it will kick off the action event and thus: save the info in the properties file.
		_clusterInfoPanel          .setVisible(false);

		// Server info
		_localServerName_txt   .setText("");

		_atAtServerName_txt    .setText("");
		_listeners_txt         .setText("");
		_aseVersion_txt        .setText("");
		_lastSampleTime_txt    .setText("");

		_startDate_txt         .setText("");
		_daysRunning_txt       .setText("");
		_countersCleared_txt   .setText("");
		_checkPoints_txt       .setText("");
		_numDeadlocks_txt      .setText("");
		_numDeadlocksDiff_txt  .setText("");
		_diagnosticDumps_txt   .setText("");
		_connections_txt       .setText("");
		_connectionsDiff_txt   .setText("");
		_distinctLoginsAbs_txt .setText("");
		_distinctLoginsDiff_txt.setText("");
		_lockWaitThreshold_txt .setText("");
		_lockWaits_txt         .setText("");
		_lockWaitsDiff_txt     .setText("");
		_maxRecovery_txt       .setText("");
		_transactions_txt      .setText("");
		_transactionsDiff_txt  .setText("");
		_transactionsRate_txt  .setText("");

		_bootcount_txt         .setText("");
		_recoveryState_txt     .setText("");
		_cpuTime_txt           .setText("");
		_cpuUser_txt           .setText("");
		_cpuSystem_txt         .setText("");
		_cpuIdle_txt           .setText("");
		_ioTotalRead_txt       .setText("");
		_ioTotalReadDiff_txt   .setText("");
		_ioTotalWrite_txt      .setText("");
		_ioTotalWriteDiff_txt  .setText("");
		_aaConnectionsAbs_txt  .setText("");
		_aaConnectionsDiff_txt .setText("");
		_aaConnectionsRate_txt .setText("");
		_packReceived_txt      .setText("");
		_packReceivedDiff_txt  .setText("");
		_packSent_txt          .setText("");
		_packSentDiff_txt      .setText("");
		_packetErrors_txt      .setText("");
		_packetErrorsDiff_txt  .setText("");
		_totalErrors_txt       .setText("");
		_totalErrorsDiff_txt   .setText("");
	}

	public void saveLayoutProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		conf.setProperty("summaryPanel.serverInfo.width",  _dataPanelScroll.getSize().width);
		conf.setProperty("summaryPanel.serverInfo.height", _dataPanelScroll.getSize().height);

		conf.save();
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		// ASE Cluser Edition: system_view
		if (_clusterView_cbx.getSelectedItem() != null)
		{
			String clusterView = _clusterView_cbx.getSelectedItem().toString();
			conf.setProperty("cluster.system_view", clusterView);
		}

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		// ASE Cluser Edition: system_view
		String clusterView = conf.getProperty("cluster.system_view", "cluster");
		_clusterView_cbx.setSelectedItem(clusterView);
				

		int width   = conf.getIntProperty("summaryPanel.serverInfo.width",  -1);
		int height  = conf.getIntProperty("summaryPanel.serverInfo.height",  -1);
		if (width != -1 && height != -1)
		{
			_dataPanelScroll.setPreferredSize(new Dimension(width, height));
		}
	}

	public int getClusterView()
	{
		String choice = (String) _clusterView_cbx.getSelectedItem();
		int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;
		if (choice != null && choice.equalsIgnoreCase("instance"))
			systemView = AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE;
		else
			systemView = AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER;

		return systemView;
	}

	public void setWatermark()
	{
		if ( MainFrame.isOfflineConnected() )
		{
			String offlineSamplePeriod = MainFrame.getOfflineSamplePeriodText();
			if (offlineSamplePeriod == null)
				setWatermarkText("Choose sample period");
			else
				setWatermarkText(offlineSamplePeriod);
		}
		else if ( ! AseTune.getCounterCollector().isMonConnected() )
		{
			setWatermarkText("Not Connected...");
		}
		else
		{
			setWatermarkText(null);
		}
	}

	public void setWatermarkText(String str)
	{
		_watermark.setWatermarkText(str);
	}

	class Watermark
	extends AbstractComponentDecorator
	{
		public Watermark(JComponent target, String text)
		{
			super(target);
			if (text != null)
				_text = text;
		}

		private String		_text	= "";
		private Graphics2D	g		= null;
		private Rectangle	r		= null;
	
		public void paint(Graphics graphics)
		{
			if (_text == null || _text != null && _text.equals(""))
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 4.0f));
			g.setColor(new Color(128, 128, 128, 128));

			FontMetrics fm = g.getFontMetrics();
			int strWidth = fm.stringWidth(_text);
			int xPos = (r.width - strWidth) / 2;
			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2.0f));

			g.translate(xPos, yPos);
			double theta = -Math.PI / 6;
			g.rotate(theta);
			g.translate(-xPos, -yPos);
	
			g.drawString(_text, xPos, yPos);
//			System.out.println("paint('"+_text+"'): xPos='" + xPos + "', yPos='" + yPos + "', r=" + r + ", g=" + g);
		}
	
		public void setWatermarkText(String text)
		{
			_text = text;
//			System.out.println("setWatermarkText: to '" + _text + "'.");
			repaint();
		}
	}
}
