package com.asetune.cm.iq.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.Logger;

import com.asetune.CounterController;
import com.asetune.Version;
import com.asetune.cm.CountersModel;
import com.asetune.cm.iq.CmSummary;
import com.asetune.gui.ISummaryPanel;
import com.asetune.gui.MainFrame;
import com.asetune.gui.ShowCmPropertiesDialog;
import com.asetune.gui.TrendGraph;
import com.asetune.gui.TrendGraphDashboardPanel;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmSummaryPanel
//extends TabularCntrPanel
extends JPanel
implements ISummaryPanel, TableModelListener, GTabbedPane.ShowProperties
{
	private static final Logger  _logger	           = Logger.getLogger(CmSummaryPanel.class);
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSummary.CM_NAME;

	private CountersModel      _cm = null;

//	private ChangeToJTabDialog _focusToBlockingTab = null;
//	private ChangeToJTabDialog _focusToDatabasesTab_fullLog = null;
//	private ChangeToJTabDialog _focusToDatabasesTab_oldestOpenTran = null;
	private Watermark          _watermark;

	private Icon             _icon = null;//SwingUtils.readImageIcon(Version.class, "images/summary_tab.png");

	private JPanel           _clusterInfoPanel;
	private JPanel           _serverInfoPanel;
	private JPanel           _dataPanel;
	private JScrollPane      _dataPanelScroll;
	private TrendGraphDashboardPanel _graphPanel;
//	private JScrollPane      _graphPanelScroll;
	
	private JLabel           _title_lbl                    = new JLabel();
	private JButton          _trendGraphs_but              = new JButton();

	// CLUSTER INFO PANEL
//	private JTextField       _clusterBootTime_txt          = new JTextField();
//	private JLabel           _clusterBootTime_lbl          = new JLabel();
//	private JTextField       _clusterName_txt              = new JTextField();
//	private JLabel           _clusterName_lbl              = new JLabel();
//	private JLabel           _clusterInstance_lbl          = new JLabel();
//	private JTextField       _clusterInstanceName_txt      = new JTextField();
//	private JTextField       _clusterInstanceId_txt        = new JTextField();
//	private JLabel           _clusterCoordinator_lbl       = new JLabel();
//	private JTextField       _clusterCoordinatorName_txt   = new JTextField();
//	private JTextField       _clusterCoordinatorId_txt     = new JTextField();
//	private JComboBox        _clusterView_cbx              = new JComboBox(new String[] {"cluster", "instance"});
//	private JLabel           _clusterView_lbl              = new JLabel();

	// SERVER INFO PANEL
	private JTextField       _localServerName_txt          = new JTextField();
	private JLabel           _localServerName_lbl          = new JLabel();
	private JTextField       _atAtServerName_txt           = new JTextField();
	private JLabel           _atAtServerName_lbl           = new JLabel();
	private JTextField       _listeners_txt                = new JTextField();
	private JLabel           _listeners_lbl                = new JLabel();
	private JTextField       _srvVersion_txt               = new JTextField();
	private JLabel           _srvVersion_lbl               = new JLabel();
	private JTextField       _srvPageSize_txt              = new JTextField();
	private JLabel           _srvPageSize_lbl              = new JLabel();
	private JTextField       _lastSampleTime_txt           = new JTextField();
	private JLabel           _lastSampleTime_lbl           = new JLabel();
	private JTextField       _utcTimeDiff_txt              = new JTextField();
	private JLabel           _utcTimeDiff_lbl              = new JLabel();

//	private JTextField       _startDate_txt                = new JTextField();
//	private JLabel           _startDate_lbl                = new JLabel();
//	private JTextField       _daysRunning_txt              = new JTextField();
//	private JLabel           _daysRunning_lbl              = new JLabel();
//	private JTextField       _countersCleared_txt          = new JTextField();
//	private JLabel           _countersCleared_lbl          = new JLabel();
//	private JTextField       _checkPoints_txt              = new JTextField();
//	private JLabel           _checkPoints_lbl              = new JLabel();
//	private JTextField       _numDeadlocks_txt             = new JTextField();
//	private JTextField       _numDeadlocksDiff_txt         = new JTextField();
//	private JLabel           _numDeadlocks_lbl             = new JLabel();
//	private JTextField       _diagnosticDumps_txt          = new JTextField();
//	private JLabel           _diagnosticDumps_lbl          = new JLabel();
//	private JTextField       _connectionsDiff_txt          = new JTextField();
//	private JTextField       _connections_txt              = new JTextField();
//	private JLabel           _connections_lbl              = new JLabel();
//	private JTextField       _distinctLoginsDiff_txt       = new JTextField();
//	private JTextField       _distinctLoginsAbs_txt        = new JTextField();
//	private JLabel           _distinctLogins_lbl           = new JLabel();
//	private JTextField       _lockWaitThreshold_txt        = new JTextField();
//	private JLabel           _lockWaitThreshold_lbl        = new JLabel();
//	private JTextField       _lockWaits_txt                = new JTextField();
//	private JTextField       _lockWaitsDiff_txt            = new JTextField();
//	private JLabel           _lockWaits_lbl                = new JLabel();
//	private JTextField       _maxRecovery_txt              = new JTextField();
//	private JLabel           _maxRecovery_lbl              = new JLabel();
//	private JLabel           _Transactions_lbl             = new JLabel();
//	private JTextField       _Transactions_Abs_txt         = new JTextField();
//	private JTextField       _Transactions_Diff_txt        = new JTextField();
//	private JTextField       _Transactions_Rate_txt        = new JTextField();
//	private JLabel           _Rollbacks_lbl                = new JLabel();
//	private JTextField       _Rollbacks_Abs_txt            = new JTextField();
//	private JTextField       _Rollbacks_Diff_txt           = new JTextField();
//	private JTextField       _Rollbacks_Rate_txt           = new JTextField();
//	private JLabel           _Selects_lbl                  = new JLabel();
//	private JTextField       _Selects_Abs_txt              = new JTextField();
//	private JTextField       _Selects_Diff_txt             = new JTextField();
//	private JTextField       _Selects_Rate_txt             = new JTextField();
//	private JLabel           _Updates_lbl                  = new JLabel();
//	private JTextField       _Updates_Abs_txt              = new JTextField();
//	private JTextField       _Updates_Diff_txt             = new JTextField();
//	private JTextField       _Updates_Rate_txt             = new JTextField();
//	private JLabel           _Inserts_lbl                  = new JLabel();
//	private JTextField       _Inserts_Abs_txt              = new JTextField();
//	private JTextField       _Inserts_Diff_txt             = new JTextField();
//	private JTextField       _Inserts_Rate_txt             = new JTextField();
//	private JLabel           _Deletes_lbl                  = new JLabel();
//	private JTextField       _Deletes_Abs_txt              = new JTextField();
//	private JTextField       _Deletes_Diff_txt             = new JTextField();
//	private JTextField       _Deletes_Rate_txt             = new JTextField();
//	private JLabel           _Merges_lbl                   = new JLabel();
//	private JTextField       _Merges_Abs_txt               = new JTextField();
//	private JTextField       _Merges_Diff_txt              = new JTextField();
//	private JTextField       _Merges_Rate_txt              = new JTextField();
//	private JLabel           _TableAccesses_lbl            = new JLabel();
//	private JTextField       _TableAccesses_Abs_txt        = new JTextField();
//	private JTextField       _TableAccesses_Diff_txt       = new JTextField();
//	private JTextField       _TableAccesses_Rate_txt       = new JTextField();
//	private JLabel           _IndexAccesses_lbl            = new JLabel();
//	private JTextField       _IndexAccesses_Abs_txt        = new JTextField();
//	private JTextField       _IndexAccesses_Diff_txt       = new JTextField();
//	private JTextField       _IndexAccesses_Rate_txt       = new JTextField();
//	private JLabel           _TempDbObjects_lbl            = new JLabel();
//	private JTextField       _TempDbObjects_Abs_txt        = new JTextField();
//	private JTextField       _TempDbObjects_Diff_txt       = new JTextField();
//	private JTextField       _TempDbObjects_Rate_txt       = new JTextField();
//	private JLabel           _WorkTables_lbl               = new JLabel();
//	private JTextField       _WorkTables_Abs_txt           = new JTextField();
//	private JTextField       _WorkTables_Diff_txt          = new JTextField();
//	private JTextField       _WorkTables_Rate_txt          = new JTextField();
//	private JLabel           _ULCFlushes_lbl               = new JLabel();
//	private JTextField       _ULCFlushes_Abs_txt           = new JTextField();
//	private JTextField       _ULCFlushes_Diff_txt          = new JTextField();
//	private JTextField       _ULCFlushes_Rate_txt          = new JTextField();
//	private JLabel           _ULCFlushFull_lbl             = new JLabel();
//	private JTextField       _ULCFlushFull_Abs_txt         = new JTextField();
//	private JTextField       _ULCFlushFull_Diff_txt        = new JTextField();
//	private JTextField       _ULCFlushFull_Rate_txt        = new JTextField();
//	private JLabel           _ULCKBWritten_lbl             = new JLabel();
//	private JTextField       _ULCKBWritten_Abs_txt         = new JTextField();
//	private JTextField       _ULCKBWritten_Diff_txt        = new JTextField();
//	private JTextField       _ULCKBWritten_Rate_txt        = new JTextField();
//	private JLabel           _PagesRead_lbl                = new JLabel();
//	private JTextField       _PagesRead_Abs_txt            = new JTextField();
//	private JTextField       _PagesRead_Diff_txt           = new JTextField();
//	private JTextField       _PagesRead_Rate_txt           = new JTextField();
//	private JLabel           _PagesWritten_lbl             = new JLabel();
//	private JTextField       _PagesWritten_Abs_txt         = new JTextField();
//	private JTextField       _PagesWritten_Diff_txt        = new JTextField();
//	private JTextField       _PagesWritten_Rate_txt        = new JTextField();
//	private JLabel           _PhysicalReads_lbl            = new JLabel();
//	private JTextField       _PhysicalReads_Abs_txt        = new JTextField();
//	private JTextField       _PhysicalReads_Diff_txt       = new JTextField();
//	private JTextField       _PhysicalReads_Rate_txt       = new JTextField();
//	private JLabel           _PhysicalWrites_lbl           = new JLabel();
//	private JTextField       _PhysicalWrites_Abs_txt       = new JTextField();
//	private JTextField       _PhysicalWrites_Diff_txt      = new JTextField();
//	private JTextField       _PhysicalWrites_Rate_txt      = new JTextField();
//	private JLabel           _LogicalReads_lbl             = new JLabel();
//	private JTextField       _LogicalReads_Abs_txt         = new JTextField();
//	private JTextField       _LogicalReads_Diff_txt        = new JTextField();
//	private JTextField       _LogicalReads_Rate_txt        = new JTextField();
//	private JLabel           _fullTranslog_lbl             = new JLabel();
//	private JTextField       _fullTranslog_txt             = new JTextField();
//	private JLabel           _oldestOpenTran_lbl           = new JLabel();
//	private JTextField       _oldestOpenTran_txt           = new JTextField();
//	
//	private JLabel           _bootcount_lbl                = new JLabel();
//	private JTextField       _bootcount_txt                = new JTextField();
//	private JLabel           _recoveryState_lbl            = new JLabel();
//	private JTextField       _recoveryState_txt            = new JTextField();
//
//	private JLabel           _cpuTime_lbl                  = new JLabel();
//	private JTextField       _cpuTime_txt                  = new JTextField();
//	private JLabel           _cpuUser_lbl                  = new JLabel();
//	private JTextField       _cpuUser_txt                  = new JTextField();
//	private JLabel           _cpuSystem_lbl                = new JLabel();
//	private JTextField       _cpuSystem_txt                = new JTextField();
//	private JLabel           _cpuIdle_lbl                  = new JLabel();
//	private JTextField       _cpuIdle_txt                  = new JTextField();
//
//	private JLabel           _ioTotalRead_lbl               = new JLabel();
//	private JTextField       _ioTotalRead_txt               = new JTextField();
//	private JTextField       _ioTotalReadDiff_txt           = new JTextField();
//	private JLabel           _ioTotalWrite_lbl              = new JLabel();
//	private JTextField       _ioTotalWrite_txt              = new JTextField();
//	private JTextField       _ioTotalWriteDiff_txt          = new JTextField();
//
//	private JLabel           _aaConnections_lbl             = new JLabel();
//	private JTextField       _aaConnectionsAbs_txt          = new JTextField();
//	private JTextField       _aaConnectionsDiff_txt         = new JTextField();
//	private JTextField       _aaConnectionsRate_txt         = new JTextField();
//
//	private JLabel           _packReceived_lbl              = new JLabel();
//	private JTextField       _packReceived_txt              = new JTextField();
//	private JTextField       _packReceivedDiff_txt          = new JTextField();
//	private JLabel           _packSent_lbl                  = new JLabel();
//	private JTextField       _packSent_txt                  = new JTextField();
//	private JTextField       _packSentDiff_txt              = new JTextField();
//	private JLabel           _packetErrors_lbl              = new JLabel();
//	private JTextField       _packetErrors_txt              = new JTextField();
//	private JTextField       _packetErrorsDiff_txt          = new JTextField();
//	private JLabel           _totalErrors_lbl               = new JLabel();
//	private JTextField       _totalErrors_txt               = new JTextField();
//	private JTextField       _totalErrorsDiff_txt           = new JTextField();

	
	
	
	
	
	private JLabel           _ActiveReq_lbl                      = new JLabel();
	private JTextField       _ActiveReq_Abs_txt                  = new JTextField();
	private JTextField       _ActiveReq_Diff_txt                 = new JTextField();
	private JTextField       _ActiveReq_Rate_txt                 = new JTextField();

	private JLabel           _BytesReceived_lbl                  = new JLabel();
	private JTextField       _BytesReceived_Abs_txt              = new JTextField();
	private JTextField       _BytesReceived_Diff_txt             = new JTextField();
	private JTextField       _BytesReceived_Rate_txt             = new JTextField();

	private JLabel           _BytesSent_lbl                      = new JLabel();
	private JTextField       _BytesSent_Abs_txt                  = new JTextField();
	private JTextField       _BytesSent_Diff_txt                 = new JTextField();
	private JTextField       _BytesSent_Rate_txt                 = new JTextField();

	private JLabel           _Chkpt_lbl                          = new JLabel();
	private JTextField       _Chkpt_Abs_txt                      = new JTextField();
	private JTextField       _Chkpt_Diff_txt                     = new JTextField();
	private JTextField       _Chkpt_Rate_txt                     = new JTextField();

	private JLabel           _CurrentCacheSize_Kb_lbl            = new JLabel();
	private JTextField       _CurrentCacheSize_Kb_Abs_txt        = new JTextField();
	private JTextField       _CurrentCacheSize_Kb_Diff_txt       = new JTextField();
	private JTextField       _CurrentCacheSize_Kb_Rate_txt       = new JTextField();

	private JLabel           _LockedHeapPages_lbl                = new JLabel();
	private JTextField       _LockedHeapPages_Abs_txt            = new JTextField();
	private JTextField       _LockedHeapPages_Diff_txt           = new JTextField();
	private JTextField       _LockedHeapPages_Rate_txt           = new JTextField();

	private JLabel           _MainHeapBytes_lbl                  = new JLabel();
	private JTextField       _MainHeapBytes_Abs_txt              = new JTextField();
	private JTextField       _MainHeapBytes_Diff_txt             = new JTextField();
	private JTextField       _MainHeapBytes_Rate_txt             = new JTextField();

	private JLabel           _MainHeapPages_lbl                  = new JLabel();
	private JTextField       _MainHeapPages_Abs_txt              = new JTextField();
	private JTextField       _MainHeapPages_Diff_txt             = new JTextField();
	private JTextField       _MainHeapPages_Rate_txt             = new JTextField();

	private JLabel           _MaxCacheSize_lbl                   = new JLabel();
	private JTextField       _MaxCacheSize_Abs_txt               = new JTextField();
	private JTextField       _MaxCacheSize_Diff_txt              = new JTextField();
	private JTextField       _MaxCacheSize_Rate_txt              = new JTextField();

	private JLabel           _MinCacheSize_lbl                   = new JLabel();
	private JTextField       _MinCacheSize_Abs_txt               = new JTextField();
	private JTextField       _MinCacheSize_Diff_txt              = new JTextField();
	private JTextField       _MinCacheSize_Rate_txt              = new JTextField();

	private JLabel           _MultiPacketsReceived_lbl           = new JLabel();
	private JTextField       _MultiPacketsReceived_Abs_txt       = new JTextField();
	private JTextField       _MultiPacketsReceived_Diff_txt      = new JTextField();
	private JTextField       _MultiPacketsReceived_Rate_txt      = new JTextField();

	private JLabel           _MultiPacketsSent_lbl               = new JLabel();
	private JTextField       _MultiPacketsSent_Abs_txt           = new JTextField();
	private JTextField       _MultiPacketsSent_Diff_txt          = new JTextField();
	private JTextField       _MultiPacketsSent_Rate_txt          = new JTextField();

	private JLabel           _NumProcessorsAvail_lbl             = new JLabel();
	private JTextField       _NumProcessorsAvail_Abs_txt         = new JTextField();
	private JTextField       _NumProcessorsAvail_Diff_txt        = new JTextField();
	private JTextField       _NumProcessorsAvail_Rate_txt        = new JTextField();

	private JLabel           _PacketsReceived_lbl                = new JLabel();
	private JTextField       _PacketsReceived_Abs_txt            = new JTextField();
	private JTextField       _PacketsReceived_Diff_txt           = new JTextField();
	private JTextField       _PacketsReceived_Rate_txt           = new JTextField();

	private JLabel           _PacketsSent_lbl                    = new JLabel();
	private JTextField       _PacketsSent_Abs_txt                = new JTextField();
	private JTextField       _PacketsSent_Diff_txt               = new JTextField();
	private JTextField       _PacketsSent_Rate_txt               = new JTextField();

	private JLabel           _ProcessCPU_lbl                     = new JLabel();
	private JTextField       _ProcessCPU_Abs_txt                 = new JTextField();
	private JTextField       _ProcessCPU_Diff_txt                = new JTextField();
	private JTextField       _ProcessCPU_Rate_txt                = new JTextField();

	private JLabel           _ProcessCPUSystem_lbl               = new JLabel();
	private JTextField       _ProcessCPUSystem_Abs_txt           = new JTextField();
	private JTextField       _ProcessCPUSystem_Diff_txt          = new JTextField();
	private JTextField       _ProcessCPUSystem_Rate_txt          = new JTextField();

	private JLabel           _ProcessCPUUser_lbl                 = new JLabel();
	private JTextField       _ProcessCPUUser_Abs_txt             = new JTextField();
	private JTextField       _ProcessCPUUser_Diff_txt            = new JTextField();
	private JTextField       _ProcessCPUUser_Rate_txt            = new JTextField();

	private JLabel           _threads_lbl                        = new JLabel();
	private JTextField       _threads_Abs_txt                    = new JTextField();
	private JTextField       _threads_Diff_txt                   = new JTextField();
	private JTextField       _threads_Rate_txt                   = new JTextField();
                                                                
	private JLabel           _ConnCount_lbl                      = new JLabel();
	private JTextField       _ConnCount_Abs_txt                  = new JTextField();
	private JTextField       _ConnCount_Diff_txt                 = new JTextField();
	private JTextField       _ConnCount_Rate_txt                 = new JTextField();
                                                                
	private JLabel           _UnschReq_lbl                       = new JLabel();
	private JTextField       _UnschReq_Abs_txt                   = new JTextField();
	private JTextField       _UnschReq_Diff_txt                  = new JTextField();
	private JTextField       _UnschReq_Rate_txt                  = new JTextField();

	private JLabel           _NumLogicalProcessorsUsed_lbl       = new JLabel();
	private JTextField       _NumLogicalProcessorsUsed_Abs_txt   = new JTextField();
	private JTextField       _NumLogicalProcessorsUsed_Diff_txt  = new JTextField();
	private JTextField       _NumLogicalProcessorsUsed_Rate_txt  = new JTextField();

	private JLabel           _NumPhysicalProcessorsUsed_lbl      = new JLabel();
	private JTextField       _NumPhysicalProcessorsUsed_Abs_txt  = new JTextField();
	private JTextField       _NumPhysicalProcessorsUsed_Diff_txt = new JTextField();
	private JTextField       _NumPhysicalProcessorsUsed_Rate_txt = new JTextField();

	private JLabel           _Commit_lbl                         = new JLabel();
	private JTextField       _Commit_Abs_txt                     = new JTextField();
	private JTextField       _Commit_Diff_txt                    = new JTextField();
	private JTextField       _Commit_Rate_txt                    = new JTextField();

	private JLabel           _RequestsReceived_lbl               = new JLabel();
	private JTextField       _RequestsReceived_Abs_txt           = new JTextField();
	private JTextField       _RequestsReceived_Diff_txt          = new JTextField();
	private JTextField       _RequestsReceived_Rate_txt          = new JTextField();

	
	private static final Color NON_CONFIGURED_MONITORING_COLOR = new Color(255, 224, 115);
	private HashMap<String, String> _originToolTip = new HashMap<String, String>(); // <name><msg>

	/** Color to be used when counters is cleared is used */
	private static final Color COUNTERS_CLEARED_COLOR = Color.ORANGE;
	
	private static final JTextField DUMMY_TEXTFIELD = new JTextField();
	private static final Font       RATE_FONT       = new Font(DUMMY_TEXTFIELD.getFont().getFontName(), Font.ITALIC, DUMMY_TEXTFIELD.getFont().getSize());

	@Override
	public String getName()
	{
		return CmSummary.CM_NAME;
	}

	public CmSummaryPanel(CountersModel cm)
	{
		super();

		_cm = cm;
		if (cm.getIconFile() != null)
			setIcon( SwingUtils.readImageIcon(Version.class, cm.getIconFile()) );

		init();
	}
	
	private void init()
	{
		try
		{
			initComponents();
		}
		catch (Exception ex)
		{
			_logger.error("Can't create the summary panel", ex);
		}
	}

	@Override
	public Icon getIcon()
	{
		return _icon;
	}
	public void setIcon(Icon icon)
	{
		_icon = icon;
	}

	@Override
	public String getPanelName()
	{
		return CmSummary.SHORT_NAME;
	}

	@Override
	public String getDescription()
	{
		return CmSummary.HTML_DESC;
	}

	@Override
	public CountersModel getCm()
	{
		return _cm;
	}


	private void initComponents() 
	throws Exception
	{
		setLayout(new BorderLayout());
		_graphPanel      = createGraphPanel();
		_dataPanel       = createDataPanel();

//		_graphPanelScroll = new JScrollPane(_graphPanel);
		_dataPanelScroll  = new JScrollPane(_dataPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
//		_graphPanelScroll.getVerticalScrollBar().setUnitIncrement(16);
		_dataPanelScroll.getVerticalScrollBar().setUnitIncrement(16);

//		JSplitPane  split  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _dataPanelScroll, _graphPanelScroll);
		JSplitPane  split  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _dataPanelScroll, _graphPanel);
		add(split, BorderLayout.CENTER);

		// set a Decorator to the panel, that will show text: "Not Connected..." etc
//		_watermark = new Watermark(_graphPanelScroll, "Not Connected...");
		_watermark = new Watermark(_graphPanel, "Not Connected...");

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
//		_clusterView_cbx.addActionListener(new ActionListener()
//		{
//			
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				String choice = (String) _clusterView_cbx.getSelectedItem();
//				int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;
//				if (choice != null && choice.equalsIgnoreCase("instance"))
//					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE;
//				else
//					systemView = AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER;
//
////				if (AseTune.getCounterCollector().isMonConnected())
////					AseConnectionUtils.setClusterEditionSystemView(AseTune.getCounterCollector().getMonConnection(), systemView);
//				if (CounterController.getInstance().isMonConnected())
//					AseConnectionUtils.setClusterEditionSystemView(CounterController.getInstance().getMonConnection(), systemView);
//
//				saveProps();
//			}
//		});
	}

	private JPanel createDataPanel() 
	{
		JPanel panel = SwingUtils.createPanel("title", false);
		panel.setLayout(new MigLayout("", "5[grow]5", ""));

		_title_lbl.setFont(new java.awt.Font("Dialog", 1, SwingUtils.hiDpiScale(16)));
		_title_lbl.setText("Summary panel");

		// create new panel
		_clusterInfoPanel = createClusterInfoPanel();
		_serverInfoPanel  = createServerInfoPanel();

		// Fix up the _optionTrendGraphs_but
		TrendGraph.createGraphAccessButton(_trendGraphs_but, CmSummary.CM_NAME);

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

		tooltip = "The IQ Cluster was booted/started at this time. (select @@clusterboottime)";
//		_clusterBootTime_lbl    .setText("Cluster Boot Time");
//		_clusterBootTime_lbl    .setToolTipText(tooltip);
//		_clusterBootTime_txt    .setToolTipText(tooltip);
//		_clusterBootTime_txt    .setEditable(false);
//
//		tooltip = "The ASE Cluster Name we are connected to. (select @@clustername)";
//		_clusterName_lbl        .setText("Cluster Name");
//		_clusterName_lbl        .setToolTipText(tooltip);
//		_clusterName_txt        .setToolTipText(tooltip);
//		_clusterName_txt        .setEditable(false);
//
//		tooltip = "The ASE Cluster Instance Name and ID we are connected to. (select @@instancename, @@instanceid)";
//		_clusterInstance_lbl.setText("Instance Name, Id");
//		_clusterInstance_lbl.setToolTipText(tooltip);
//		_clusterInstanceName_txt.setToolTipText(tooltip);
//		_clusterInstanceName_txt.setEditable(false);
//		_clusterInstanceId_txt  .setToolTipText(tooltip);
//		_clusterInstanceId_txt  .setEditable(false);
//
//		tooltip = "The ASE Cluster Coordinator Name and ID. (select instance_name(@@clustercoordid), @@clustercoordid)";
//		_clusterCoordinator_lbl    .setText("Coordinator Name, Id");
//		_clusterCoordinator_lbl    .setToolTipText(tooltip);
//		_clusterCoordinatorName_txt.setToolTipText(tooltip);
//		_clusterCoordinatorName_txt.setEditable(false);
//		_clusterCoordinatorId_txt  .setToolTipText(tooltip);
//		_clusterCoordinatorId_txt  .setEditable(false);
//		
//		tooltip = "<html>" +
//		              "View counters for the whole Cluster or just the instance we connected to. (set system_view cluster|instance)" +
//		              "<ul>" +
//		                 "<li>cluster - Show Counters for <b>all instances</b> within the cluster.</li>" +
//		                 "<li>instance - Only Counters for the <b>local instance</b> is showed.</li>" +
//		              "</ul>" +
//		          "</html>";
//		_clusterView_lbl        .setText("System View");
//		_clusterView_lbl        .setToolTipText(tooltip);
//		_clusterView_cbx        .setToolTipText(tooltip);
////		_clusterView_cbx        .setEditable(true);
//
//
//		
//		//--------------------------
//		// DO the LAYOUT
//		//--------------------------
//		panel.add(_clusterBootTime_lbl,       "");
//		panel.add(_clusterBootTime_txt,       "growx, wrap");
//		
//		panel.add(_clusterName_lbl,           "");
//		panel.add(_clusterName_txt,           "growx, wrap");
//		
//		panel.add(_clusterInstance_lbl,       "");
//		panel.add(_clusterInstanceName_txt,   "growx, split");
//		panel.add(_clusterInstanceId_txt,     "growx, wrap");
//		
//		panel.add(_clusterCoordinator_lbl,    "");
//		panel.add(_clusterCoordinatorName_txt,"growx, split");
//		panel.add(_clusterCoordinatorId_txt,  "growx, wrap");
//
//		panel.add(_clusterView_lbl,           "");
//		panel.add(_clusterView_cbx,           "growx, wrap");

		// this panel will only be visible when you connect to a Cluster Enabled Server
		panel.setVisible(false);

		return panel;
	}


	private JPanel createServerInfoPanel() 
	{
		JPanel panel = new JPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText()
			{
//				CountersModel cm = _cmDisplay; // where is offline data stored when we read from the offline db?
				CountersModel cm = null;
				if (cm == null)	cm = _cm;
//				if (cm == null)	return null;
				
				String sqlRefreshTime = (cm == null) ? "Unavailable" : cm.getSqlRefreshTime() + " ms.";
				String guiRefreshTime = (cm == null) ? "Unavailable" : cm.getGuiRefreshTime() + " ms.";
				String lcRefreshTime  = (cm == null) ? "Unavailable" : cm.getLcRefreshTime() + " ms.";

				return "<html>" +
						"SQL Refresh time: "+sqlRefreshTime+"<br>" +
						"GUI Refresh Time: "+guiRefreshTime+"<br>" +
						"Local Calculation Time: "+lcRefreshTime+"<br>" +
						"</html>";
			}
		};
		// need to set/register the tooltip, otherwise it will grab parents tooltip.
        ToolTipManager.sharedInstance().registerComponent(panel);

		panel.setBorder(BorderFactory.createTitledBorder("Server Information"));

//		JPanel panel = SwingUtils.createPanel("Server Information", true);
		panel.setLayout(new MigLayout("", "[] [grow]", ""));

		String tooltip = "";

		tooltip = "The name we used when "+Version.getAppName()+" connected to the server, meaning name in sql.ini or interfaces ";
//		_localServerName_lbl  .setText("Local server name");
		_localServerName_lbl  .setText("Connection Info");
		_localServerName_lbl  .setToolTipText(tooltip);
		_localServerName_txt  .setToolTipText(tooltip);
		_localServerName_txt  .setEditable(false);

		tooltip = "This is the internal server name in IQ, taken from the global variable @@servername";
		_atAtServerName_lbl   .setText("@@servername");
		_atAtServerName_lbl   .setToolTipText(tooltip);
		_atAtServerName_txt   .setToolTipText(tooltip);
		_atAtServerName_txt   .setEditable(false);

		tooltip = "Hostname that the IQ server has listener services on, this makes it easier to see what physical machine we have connected to.";
		_listeners_lbl        .setText("IQ Port listeners");
		_listeners_lbl        .setToolTipText(tooltip);
		_listeners_txt        .setToolTipText(tooltip);
		_listeners_txt        .setEditable(false);

		tooltip = "The version string taken from @@version";
		_srvVersion_lbl       .setText("IQ Version");
		_srvVersion_lbl       .setToolTipText(tooltip);
		_srvVersion_txt       .setToolTipText(tooltip);
		_srvVersion_txt       .setEditable(false);

		tooltip = "The PageSize is taken from property('PageSize'), which is presented in bytes.";
		_srvPageSize_lbl      .setText("IQ Page Size");
		_srvPageSize_lbl      .setToolTipText(tooltip);
		_srvPageSize_txt      .setToolTipText(tooltip);
		_srvPageSize_txt      .setEditable(false);

		tooltip = "Time of list sample.";
		_lastSampleTime_lbl   .setText("Sample time");
		_lastSampleTime_lbl   .setToolTipText(tooltip);
		_lastSampleTime_txt   .setToolTipText(tooltip);
		_lastSampleTime_txt   .setEditable(false);

		tooltip = "UTC Time Difference in minutes (positive east of UK, negative west of UK).";
		_utcTimeDiff_lbl      .setText("UTC Time Diff");
		_utcTimeDiff_lbl      .setToolTipText(tooltip);
		_utcTimeDiff_txt      .setToolTipText(tooltip);
		_utcTimeDiff_txt      .setEditable(false);


		
		tooltip = "<html>Number of server threads that are currently handling a request. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip      .put    ("ActiveReq", tooltip);
		_ActiveReq_lbl      .setText("ActiveReq");
		_ActiveReq_lbl      .setToolTipText(tooltip);
		_ActiveReq_Abs_txt  .setToolTipText(tooltip);
		_ActiveReq_Abs_txt  .setEditable(false);
		_ActiveReq_Diff_txt .setEditable(false);
		_ActiveReq_Diff_txt .setToolTipText(tooltip);
		_ActiveReq_Rate_txt .setEditable(false);
		_ActiveReq_Rate_txt .setToolTipText(tooltip);
		_ActiveReq_Diff_txt.setForeground(Color.BLUE);
		_ActiveReq_Rate_txt.setForeground(Color.BLUE);
		_ActiveReq_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Number of bytes received during client/server communications. This value is updated for HTTP and HTTPS connections. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip          .put    ("BytesReceived", tooltip);
		_BytesReceived_lbl      .setText("BytesReceived");
		_BytesReceived_lbl      .setToolTipText(tooltip);
		_BytesReceived_Abs_txt  .setToolTipText(tooltip);
		_BytesReceived_Abs_txt  .setEditable(false);
		_BytesReceived_Diff_txt .setEditable(false);
		_BytesReceived_Diff_txt .setToolTipText(tooltip);
		_BytesReceived_Rate_txt .setEditable(false);
		_BytesReceived_Rate_txt .setToolTipText(tooltip);
		_BytesReceived_Diff_txt.setForeground(Color.BLUE);
		_BytesReceived_Rate_txt.setForeground(Color.BLUE);
		_BytesReceived_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Number of bytes sent during client/server communications. This value is updated for HTTP and HTTPS connections. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip      .put    ("BytesSent", tooltip);
		_BytesSent_lbl      .setText("BytesSent");
		_BytesSent_lbl      .setToolTipText(tooltip);
		_BytesSent_Abs_txt  .setToolTipText(tooltip);
		_BytesSent_Abs_txt  .setEditable(false);
		_BytesSent_Diff_txt .setEditable(false);
		_BytesSent_Diff_txt .setToolTipText(tooltip);
		_BytesSent_Rate_txt .setEditable(false);
		_BytesSent_Rate_txt .setToolTipText(tooltip);
		_BytesSent_Diff_txt.setForeground(Color.BLUE);
		_BytesSent_Rate_txt.setForeground(Color.BLUE);
		_BytesSent_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Number of checkpoints that have been performed. . (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip  .put    ("Chkpt", tooltip);
		_Chkpt_lbl      .setText("Chkpt");
		_Chkpt_lbl      .setToolTipText(tooltip);
		_Chkpt_Abs_txt  .setToolTipText(tooltip);
		_Chkpt_Abs_txt  .setEditable(false);
		_Chkpt_Diff_txt .setEditable(false);
		_Chkpt_Diff_txt .setToolTipText(tooltip);
		_Chkpt_Rate_txt .setEditable(false);
		_Chkpt_Rate_txt .setToolTipText(tooltip);
		_Chkpt_Diff_txt.setForeground(Color.BLUE);
		_Chkpt_Rate_txt.setForeground(Color.BLUE);
		_Chkpt_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>current cache size, in kilobytes. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip                .put    ("CurrentCacheSize_Kb", tooltip);
		_CurrentCacheSize_Kb_lbl      .setText("CurrentCacheSize_Kb");
		_CurrentCacheSize_Kb_lbl      .setToolTipText(tooltip);
		_CurrentCacheSize_Kb_Abs_txt  .setToolTipText(tooltip);
		_CurrentCacheSize_Kb_Abs_txt  .setEditable(false);
		_CurrentCacheSize_Kb_Diff_txt .setEditable(false);
		_CurrentCacheSize_Kb_Diff_txt .setToolTipText(tooltip);
		_CurrentCacheSize_Kb_Rate_txt .setEditable(false);
		_CurrentCacheSize_Kb_Rate_txt .setToolTipText(tooltip);
		_CurrentCacheSize_Kb_Diff_txt.setForeground(Color.BLUE);
		_CurrentCacheSize_Kb_Rate_txt.setForeground(Color.BLUE);
		_CurrentCacheSize_Kb_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of heap pages locked in the cache. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip            .put    ("LockedHeapPages", tooltip);
		_LockedHeapPages_lbl      .setText("LockedHeapPages");
		_LockedHeapPages_lbl      .setToolTipText(tooltip);
		_LockedHeapPages_Abs_txt  .setToolTipText(tooltip);
		_LockedHeapPages_Abs_txt  .setEditable(false);
		_LockedHeapPages_Diff_txt .setEditable(false);
		_LockedHeapPages_Diff_txt .setToolTipText(tooltip);
		_LockedHeapPages_Rate_txt .setEditable(false);
		_LockedHeapPages_Rate_txt .setToolTipText(tooltip);
		_LockedHeapPages_Diff_txt.setForeground(Color.BLUE);
		_LockedHeapPages_Rate_txt.setForeground(Color.BLUE);
		_LockedHeapPages_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of bytes used for global server data structures. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip          .put    ("MainHeapBytes", tooltip);
		_MainHeapBytes_lbl      .setText("MainHeapBytes");
		_MainHeapBytes_lbl      .setToolTipText(tooltip);
		_MainHeapBytes_Abs_txt  .setToolTipText(tooltip);
		_MainHeapBytes_Abs_txt  .setEditable(false);
		_MainHeapBytes_Diff_txt .setEditable(false);
		_MainHeapBytes_Diff_txt .setToolTipText(tooltip);
		_MainHeapBytes_Rate_txt .setEditable(false);
		_MainHeapBytes_Rate_txt .setToolTipText(tooltip);
		_MainHeapBytes_Diff_txt.setForeground(Color.BLUE);
		_MainHeapBytes_Rate_txt.setForeground(Color.BLUE);
		_MainHeapBytes_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of pages used for global server data structures. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip          .put    ("MainHeapPages", tooltip);
		_MainHeapPages_lbl      .setText("MainHeapPages");
		_MainHeapPages_lbl      .setToolTipText(tooltip);
		_MainHeapPages_Abs_txt  .setToolTipText(tooltip);
		_MainHeapPages_Abs_txt  .setEditable(false);
		_MainHeapPages_Diff_txt .setEditable(false);
		_MainHeapPages_Diff_txt .setToolTipText(tooltip);
		_MainHeapPages_Rate_txt .setEditable(false);
		_MainHeapPages_Rate_txt .setToolTipText(tooltip);
		_MainHeapPages_Diff_txt.setForeground(Color.BLUE);
		_MainHeapPages_Rate_txt.setForeground(Color.BLUE);
		_MainHeapPages_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>maximum cache size allowed, in kilobytes. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip         .put    ("MaxCacheSize", tooltip);
		_MaxCacheSize_lbl      .setText("MaxCacheSize");
		_MaxCacheSize_lbl      .setToolTipText(tooltip);
		_MaxCacheSize_Abs_txt  .setToolTipText(tooltip);
		_MaxCacheSize_Abs_txt  .setEditable(false);
		_MaxCacheSize_Diff_txt .setEditable(false);
		_MaxCacheSize_Diff_txt .setToolTipText(tooltip);
		_MaxCacheSize_Rate_txt .setEditable(false);
		_MaxCacheSize_Rate_txt .setToolTipText(tooltip);
		_MaxCacheSize_Diff_txt.setForeground(Color.BLUE);
		_MaxCacheSize_Rate_txt.setForeground(Color.BLUE);
		_MaxCacheSize_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>minimum cache size allowed, in kilobytes. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip         .put    ("MinCacheSize", tooltip);
		_MinCacheSize_lbl      .setText("MinCacheSize");
		_MinCacheSize_lbl      .setToolTipText(tooltip);
		_MinCacheSize_Abs_txt  .setToolTipText(tooltip);
		_MinCacheSize_Abs_txt  .setEditable(false);
		_MinCacheSize_Diff_txt .setEditable(false);
		_MinCacheSize_Diff_txt .setToolTipText(tooltip);
		_MinCacheSize_Rate_txt .setEditable(false);
		_MinCacheSize_Rate_txt .setToolTipText(tooltip);
		_MinCacheSize_Diff_txt.setForeground(Color.BLUE);
		_MinCacheSize_Rate_txt.setForeground(Color.BLUE);
		_MinCacheSize_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of multi-packet requests received during client/server communications. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip                 .put    ("MultiPacketsReceived", tooltip);
		_MultiPacketsReceived_lbl      .setText("MultiPacketsReceived");
		_MultiPacketsReceived_lbl      .setToolTipText(tooltip);
		_MultiPacketsReceived_Abs_txt  .setToolTipText(tooltip);
		_MultiPacketsReceived_Abs_txt  .setEditable(false);
		_MultiPacketsReceived_Diff_txt .setEditable(false);
		_MultiPacketsReceived_Diff_txt .setToolTipText(tooltip);
		_MultiPacketsReceived_Rate_txt .setEditable(false);
		_MultiPacketsReceived_Rate_txt .setToolTipText(tooltip);
		_MultiPacketsReceived_Diff_txt.setForeground(Color.BLUE);
		_MultiPacketsReceived_Rate_txt.setForeground(Color.BLUE);
		_MultiPacketsReceived_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of multi-packet responses sent during client/server communications. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip             .put    ("MultiPacketsSent", tooltip);
		_MultiPacketsSent_lbl      .setText("MultiPacketsSent");
		_MultiPacketsSent_lbl      .setToolTipText(tooltip);
		_MultiPacketsSent_Abs_txt  .setToolTipText(tooltip);
		_MultiPacketsSent_Abs_txt  .setEditable(false);
		_MultiPacketsSent_Diff_txt .setEditable(false);
		_MultiPacketsSent_Diff_txt .setToolTipText(tooltip);
		_MultiPacketsSent_Rate_txt .setEditable(false);
		_MultiPacketsSent_Rate_txt .setToolTipText(tooltip);
		_MultiPacketsSent_Diff_txt.setForeground(Color.BLUE);
		_MultiPacketsSent_Rate_txt.setForeground(Color.BLUE);
		_MultiPacketsSent_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>FIXME. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip               .put    ("NumProcessorsAvail", tooltip);
		_NumProcessorsAvail_lbl      .setText("NumProcessorsAvail");
		_NumProcessorsAvail_lbl      .setToolTipText(tooltip);
		_NumProcessorsAvail_Abs_txt  .setToolTipText(tooltip);
		_NumProcessorsAvail_Abs_txt  .setEditable(false);
		_NumProcessorsAvail_Diff_txt .setEditable(false);
		_NumProcessorsAvail_Diff_txt .setToolTipText(tooltip);
		_NumProcessorsAvail_Rate_txt .setEditable(false);
		_NumProcessorsAvail_Rate_txt .setToolTipText(tooltip);
		_NumProcessorsAvail_Diff_txt.setForeground(Color.BLUE);
		_NumProcessorsAvail_Rate_txt.setForeground(Color.BLUE);
		_NumProcessorsAvail_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of client/server communication packets received. This value is not updated for HTTP or HTTPS connections. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip            .put    ("PacketsReceived", tooltip);
		_PacketsReceived_lbl      .setText("PacketsReceived");
		_PacketsReceived_lbl      .setToolTipText(tooltip);
		_PacketsReceived_Abs_txt  .setToolTipText(tooltip);
		_PacketsReceived_Abs_txt  .setEditable(false);
		_PacketsReceived_Diff_txt .setEditable(false);
		_PacketsReceived_Diff_txt .setToolTipText(tooltip);
		_PacketsReceived_Rate_txt .setEditable(false);
		_PacketsReceived_Rate_txt .setToolTipText(tooltip);
		_PacketsReceived_Diff_txt.setForeground(Color.BLUE);
		_PacketsReceived_Rate_txt.setForeground(Color.BLUE);
		_PacketsReceived_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of client/server communication packets sent. This value is not updated for HTTP or HTTPS connections. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip        .put    ("PacketsSent", tooltip);
		_PacketsSent_lbl      .setText("PacketsSent");
		_PacketsSent_lbl      .setToolTipText(tooltip);
		_PacketsSent_Abs_txt  .setToolTipText(tooltip);
		_PacketsSent_Abs_txt  .setEditable(false);
		_PacketsSent_Diff_txt .setEditable(false);
		_PacketsSent_Diff_txt .setToolTipText(tooltip);
		_PacketsSent_Rate_txt .setEditable(false);
		_PacketsSent_Rate_txt .setToolTipText(tooltip);
		_PacketsSent_Diff_txt.setForeground(Color.BLUE);
		_PacketsSent_Rate_txt.setForeground(Color.BLUE);
		_PacketsSent_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Returns CPU usage for the database server process. Values are in seconds. This property is supported on Windows and Unix. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip             .put    ("ProcessCPU", tooltip);
		_ProcessCPU_lbl      .setText("ProcessCPU");
		_ProcessCPU_lbl      .setToolTipText(tooltip);
		_ProcessCPU_Abs_txt  .setToolTipText(tooltip);
		_ProcessCPU_Abs_txt  .setEditable(false);
		_ProcessCPU_Diff_txt .setEditable(false);
		_ProcessCPU_Diff_txt .setToolTipText(tooltip);
		_ProcessCPU_Rate_txt .setEditable(false);
		_ProcessCPU_Rate_txt .setToolTipText(tooltip);
		_ProcessCPU_Diff_txt.setForeground(Color.BLUE);
		_ProcessCPU_Rate_txt.setForeground(Color.BLUE);
		_ProcessCPU_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Returns system CPU usage for the database server process CPU. This is the amount of CPU time that the database server spent inside the operating system kernel. Values are in seconds. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip             .put    ("ProcessCPUSystem", tooltip);
		_ProcessCPUSystem_lbl      .setText("ProcessCPUSystem");
		_ProcessCPUSystem_lbl      .setToolTipText(tooltip);
		_ProcessCPUSystem_Abs_txt  .setToolTipText(tooltip);
		_ProcessCPUSystem_Abs_txt  .setEditable(false);
		_ProcessCPUSystem_Diff_txt .setEditable(false);
		_ProcessCPUSystem_Diff_txt .setToolTipText(tooltip);
		_ProcessCPUSystem_Rate_txt .setEditable(false);
		_ProcessCPUSystem_Rate_txt .setToolTipText(tooltip);
		_ProcessCPUSystem_Diff_txt.setForeground(Color.BLUE);
		_ProcessCPUSystem_Rate_txt.setForeground(Color.BLUE);
		_ProcessCPUSystem_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Returns user CPU usage for the database server process. Values are in seconds. This excludes the amount of CPU time that the database server spent inside the operating system kernel. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip           .put    ("ProcessCPUUser", tooltip);
		_ProcessCPUUser_lbl      .setText("ProcessCPUUser");
		_ProcessCPUUser_lbl      .setToolTipText(tooltip);
		_ProcessCPUUser_Abs_txt  .setToolTipText(tooltip);
		_ProcessCPUUser_Abs_txt  .setEditable(false);
		_ProcessCPUUser_Diff_txt .setEditable(false);
		_ProcessCPUUser_Diff_txt .setToolTipText(tooltip);
		_ProcessCPUUser_Rate_txt .setEditable(false);
		_ProcessCPUUser_Rate_txt .setToolTipText(tooltip);
		_ProcessCPUUser_Diff_txt.setForeground(Color.BLUE);
		_ProcessCPUUser_Rate_txt.setForeground(Color.BLUE);
		_ProcessCPUUser_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>FIXME. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip    .put    ("threads", tooltip);
		_threads_lbl      .setText("threads");
		_threads_lbl      .setToolTipText(tooltip);
		_threads_Abs_txt  .setToolTipText(tooltip);
		_threads_Abs_txt  .setEditable(false);
		_threads_Diff_txt .setEditable(false);
		_threads_Diff_txt .setToolTipText(tooltip);
		_threads_Rate_txt .setEditable(false);
		_threads_Rate_txt .setToolTipText(tooltip);
		_threads_Diff_txt.setForeground(Color.BLUE);
		_threads_Rate_txt.setForeground(Color.BLUE);
		_threads_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>Returns the number of connections to the database. The property value does not include connections used for firing events or other internal operations, but it does include connections used for external environment support. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip      .put    ("ConnCount", tooltip);
		_ConnCount_lbl      .setText("ConnCount");
		_ConnCount_lbl      .setToolTipText(tooltip);
		_ConnCount_Abs_txt  .setToolTipText(tooltip);
		_ConnCount_Abs_txt  .setEditable(false);
		_ConnCount_Diff_txt .setEditable(false);
		_ConnCount_Diff_txt .setToolTipText(tooltip);
		_ConnCount_Rate_txt .setEditable(false);
		_ConnCount_Rate_txt .setToolTipText(tooltip);
		_ConnCount_Diff_txt.setForeground(Color.BLUE);
		_ConnCount_Rate_txt.setForeground(Color.BLUE);
		_ConnCount_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of requests that are currently queued up waiting for an available server thread. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip     .put    ("UnschReq", tooltip);
		_UnschReq_lbl      .setText("UnschReq");
		_UnschReq_lbl      .setToolTipText(tooltip);
		_UnschReq_Abs_txt  .setToolTipText(tooltip);
		_UnschReq_Abs_txt  .setEditable(false);
		_UnschReq_Diff_txt .setEditable(false);
		_UnschReq_Diff_txt .setToolTipText(tooltip);
		_UnschReq_Rate_txt .setEditable(false);
		_UnschReq_Rate_txt .setToolTipText(tooltip);
		_UnschReq_Diff_txt.setForeground(Color.BLUE);
		_UnschReq_Rate_txt.setForeground(Color.BLUE);
		_UnschReq_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of logical processors the database server will use. On Windows, use the -gtc option to change the number of logical processors used. See -gtc server option. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip                     .put    ("NumLogicalProcessorsUsed", tooltip);
		_NumLogicalProcessorsUsed_lbl      .setText("NumLogicalProcessorsUsed");
		_NumLogicalProcessorsUsed_lbl      .setToolTipText(tooltip);
		_NumLogicalProcessorsUsed_Abs_txt  .setToolTipText(tooltip);
		_NumLogicalProcessorsUsed_Abs_txt  .setEditable(false);
		_NumLogicalProcessorsUsed_Diff_txt .setEditable(false);
		_NumLogicalProcessorsUsed_Diff_txt .setToolTipText(tooltip);
		_NumLogicalProcessorsUsed_Rate_txt .setEditable(false);
		_NumLogicalProcessorsUsed_Rate_txt .setToolTipText(tooltip);
		_NumLogicalProcessorsUsed_Diff_txt.setForeground(Color.BLUE);
		_NumLogicalProcessorsUsed_Rate_txt.setForeground(Color.BLUE);
		_NumLogicalProcessorsUsed_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of physical processors the database server will use. The personal server is limited to one processor on some platforms. On Windows, you can use the -gt option to change the number of physical processors used by the network database server. See -gt server option. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip                      .put    ("NumPhysicalProcessorsUsed", tooltip);
		_NumPhysicalProcessorsUsed_lbl      .setText("NumPhysicalProcessorsUsed");
		_NumPhysicalProcessorsUsed_lbl      .setToolTipText(tooltip);
		_NumPhysicalProcessorsUsed_Abs_txt  .setToolTipText(tooltip);
		_NumPhysicalProcessorsUsed_Abs_txt  .setEditable(false);
		_NumPhysicalProcessorsUsed_Diff_txt .setEditable(false);
		_NumPhysicalProcessorsUsed_Diff_txt .setToolTipText(tooltip);
		_NumPhysicalProcessorsUsed_Rate_txt .setEditable(false);
		_NumPhysicalProcessorsUsed_Rate_txt .setToolTipText(tooltip);
		_NumPhysicalProcessorsUsed_Diff_txt.setForeground(Color.BLUE);
		_NumPhysicalProcessorsUsed_Rate_txt.setForeground(Color.BLUE);
		_NumPhysicalProcessorsUsed_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>FIXME. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip   .put    ("Commit", tooltip);
		_Commit_lbl      .setText("Commit");
		_Commit_lbl      .setToolTipText(tooltip);
		_Commit_Abs_txt  .setToolTipText(tooltip);
		_Commit_Abs_txt  .setEditable(false);
		_Commit_Diff_txt .setEditable(false);
		_Commit_Diff_txt .setToolTipText(tooltip);
		_Commit_Rate_txt .setEditable(false);
		_Commit_Rate_txt .setToolTipText(tooltip);
		_Commit_Diff_txt.setForeground(Color.BLUE);
		_Commit_Rate_txt.setForeground(Color.BLUE);
		_Commit_Rate_txt.setFont(RATE_FONT);

		tooltip = "<html>number of client/server communication requests or round trips. It is different from PacketsReceived in that multi-packet requests count as one request, and liveness packets are not included. (abs, <font color=\"blue\">diff, <i>rate</i></font>)</html>";
		_originToolTip             .put    ("RequestsReceived", tooltip);
		_RequestsReceived_lbl      .setText("RequestsReceived");
		_RequestsReceived_lbl      .setToolTipText(tooltip);
		_RequestsReceived_Abs_txt  .setToolTipText(tooltip);
		_RequestsReceived_Abs_txt  .setEditable(false);
		_RequestsReceived_Diff_txt .setEditable(false);
		_RequestsReceived_Diff_txt .setToolTipText(tooltip);
		_RequestsReceived_Rate_txt .setEditable(false);
		_RequestsReceived_Rate_txt .setToolTipText(tooltip);
		_RequestsReceived_Diff_txt.setForeground(Color.BLUE);
		_RequestsReceived_Rate_txt.setForeground(Color.BLUE);
		_RequestsReceived_Rate_txt.setFont(RATE_FONT);

//		tooltip = "FIXME. (abs, diff, rate)";
//		_originToolTip       .put    ("FIXME", tooltip);
//		__lbl      .setText("FIXME");
//		__lbl      .setToolTipText(tooltip);
//		__Abs_txt  .setToolTipText(tooltip);
//		__Abs_txt  .setEditable(false);
//		__Diff_txt .setEditable(false);
//		__Diff_txt .setToolTipText(tooltip);
//		__Rate_txt .setEditable(false);
//		__Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "FIXME. (abs, diff, rate)";
//		_originToolTip       .put    ("FIXME", tooltip);
//		__lbl      .setText("FIXME");
//		__lbl      .setToolTipText(tooltip);
//		__Abs_txt  .setToolTipText(tooltip);
//		__Abs_txt  .setEditable(false);
//		__Diff_txt .setEditable(false);
//		__Diff_txt .setToolTipText(tooltip);
//		__Rate_txt .setEditable(false);
//		__Rate_txt .setToolTipText(tooltip);

		
		
		
//		tooltip = "Date and time that the Server was started.";
//		_startDate_lbl        .setText("Start date");
//		_startDate_lbl        .setToolTipText(tooltip);
//		_startDate_txt        .setToolTipText(tooltip);
//		_startDate_txt        .setEditable(false);
//
//		tooltip = "Number of days that the Server has been running for.";
//		_daysRunning_lbl      .setText("Days running");
//		_daysRunning_lbl      .setToolTipText(tooltip);
//		_daysRunning_txt      .setToolTipText(tooltip);
//		_daysRunning_txt      .setEditable(false);
//
//		tooltip = "Date and time at which the monitor counters were last cleared.";
//		_countersCleared_lbl  .setText("Counters clear date");
//		_countersCleared_lbl  .setToolTipText(tooltip);
//		_countersCleared_txt  .setToolTipText(tooltip);
//		_countersCleared_txt  .setEditable(false);
//
//		tooltip = "Whether any checkpoint is currently running.";
//		_checkPoints_lbl      .setText("Running checkpoint");
//		_checkPoints_lbl      .setToolTipText(tooltip);
//		_checkPoints_txt      .setToolTipText(tooltip);
//		_checkPoints_txt      .setEditable(false);
//
//		tooltip = "Total number of deadlocks that have occurred.";
//		_numDeadlocks_lbl     .setText("Number of deadlock");
//		_numDeadlocks_lbl     .setToolTipText(tooltip);
//		_numDeadlocks_txt     .setToolTipText(tooltip);
//		_numDeadlocks_txt     .setEditable(false);
//		_numDeadlocksDiff_txt .setEditable(false);
//		_numDeadlocksDiff_txt .setToolTipText("The difference since previous sample.");
//
//		tooltip = "Whether the Sybmon diagnostic utility is performing a shared memory dump.";
//		_diagnosticDumps_lbl  .setText("Diagnostics Dumps");
//		_diagnosticDumps_lbl  .setToolTipText(tooltip);
//		_diagnosticDumps_txt  .setToolTipText(tooltip);
//		_diagnosticDumps_txt  .setEditable(false);
//
//		tooltip = "Number of active inbound connections.";
//		_connections_lbl      .setText("Connections");
//		_connections_lbl      .setToolTipText(tooltip);
//		_connections_txt      .setToolTipText(tooltip);
//		_connections_txt      .setEditable(false);
//		_connectionsDiff_txt  .setEditable(false);
//		_connectionsDiff_txt  .setToolTipText("The difference since previous sample.");
//
//		tooltip = "Number of distinct User Names that is logged in to IQ.";
//		_distinctLogins_lbl    .setText("Distinct Logins");
//		_distinctLogins_lbl    .setToolTipText(tooltip);
//		_distinctLoginsAbs_txt .setToolTipText(tooltip);
//		_distinctLoginsAbs_txt .setEditable(false);
//		_distinctLoginsDiff_txt.setEditable(false);
//		_distinctLoginsDiff_txt.setToolTipText("The difference since previous sample.");
//
//		tooltip = "Time (in seconds) that processes must have waited for locks in order to be reported.";
//		_lockWaitThreshold_lbl.setText("Lock wait threshold");
//		_lockWaitThreshold_lbl.setToolTipText(tooltip);
//		_lockWaitThreshold_txt.setToolTipText(tooltip);
//		_lockWaitThreshold_txt.setEditable(false);
//
//		tooltip = "Number of processes that have waited longer than LockWaitThreshold seconds.";
//		_lockWaits_lbl        .setText("Lock waits");
//		_lockWaits_lbl        .setToolTipText(tooltip);
//		_lockWaits_txt        .setToolTipText(tooltip);
//		_lockWaits_txt        .setEditable(false);
//		_lockWaitsDiff_txt    .setEditable(false);
//		_lockWaitsDiff_txt    .setToolTipText("The difference since previous sample.");
//
//		tooltip = "The maximum time (in minutes), per database, that ASE uses to complete its recovery procedures in case of a system failure, the current 'Run Value' for the 'recovery interval in minutes' configuration option.";
//		_maxRecovery_lbl      .setText("Max recovery");
//		_maxRecovery_lbl      .setToolTipText(tooltip);
//		_maxRecovery_txt      .setToolTipText(tooltip);
//		_maxRecovery_txt      .setEditable(false);
//		
//		tooltip = "Number of transactions executed (abs, diff, rate), Only available from 15.0.3 CE or 15.5 (not to be trusted in a Cluster Server).";
//		_originToolTip        .put    ("Transactions", tooltip);
//		_Transactions_lbl     .setText("Transactions");
//		_Transactions_lbl     .setToolTipText(tooltip);
//		_Transactions_Abs_txt .setToolTipText(tooltip);
//		_Transactions_Abs_txt .setEditable(false);
//		_Transactions_Diff_txt.setEditable(false);
//		_Transactions_Diff_txt.setToolTipText(tooltip);
//		_Transactions_Rate_txt.setEditable(false);
//		_Transactions_Rate_txt.setToolTipText(tooltip);
//		
//		tooltip = "Total number of transactions that have been rolled back on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip      .put    ("Rollbacks", tooltip);
//		_Rollbacks_lbl      .setText("Rollbacks");
//		_Rollbacks_lbl      .setToolTipText(tooltip);
//		_Rollbacks_Abs_txt  .setToolTipText(tooltip);
//		_Rollbacks_Abs_txt  .setEditable(false);
//		_Rollbacks_Diff_txt .setEditable(false);
//		_Rollbacks_Diff_txt .setToolTipText(tooltip);
//		_Rollbacks_Rate_txt .setEditable(false);
//		_Rollbacks_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of select operations that have been executed on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip    .put    ("Selects", tooltip);
//		_Selects_lbl      .setText("Selects");
//		_Selects_lbl      .setToolTipText(tooltip);
//		_Selects_Abs_txt  .setToolTipText(tooltip);
//		_Selects_Abs_txt  .setEditable(false);
//		_Selects_Diff_txt .setEditable(false);
//		_Selects_Diff_txt .setToolTipText(tooltip);
//		_Selects_Rate_txt .setEditable(false);
//		_Selects_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of update operations that have been executed on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip    .put    ("Updates", tooltip);
//		_Updates_lbl      .setText("Updates");
//		_Updates_lbl      .setToolTipText(tooltip);
//		_Updates_Abs_txt  .setToolTipText(tooltip);
//		_Updates_Abs_txt  .setEditable(false);
//		_Updates_Diff_txt .setEditable(false);
//		_Updates_Diff_txt .setToolTipText(tooltip);
//		_Updates_Rate_txt .setEditable(false);
//		_Updates_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of insert operations that have been executed on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip    .put    ("Inserts", tooltip);
//		_Inserts_lbl      .setText("Inserts");
//		_Inserts_lbl      .setToolTipText(tooltip);
//		_Inserts_Abs_txt  .setToolTipText(tooltip);
//		_Inserts_Abs_txt  .setEditable(false);
//		_Inserts_Diff_txt .setEditable(false);
//		_Inserts_Diff_txt .setToolTipText(tooltip);
//		_Inserts_Rate_txt .setEditable(false);
//		_Inserts_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of delete operations that have been executed on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip    .put    ("Deletes", tooltip);
//		_Deletes_lbl      .setText("Deletes");
//		_Deletes_lbl      .setToolTipText(tooltip);
//		_Deletes_Abs_txt  .setToolTipText(tooltip);
//		_Deletes_Abs_txt  .setEditable(false);
//		_Deletes_Diff_txt .setEditable(false);
//		_Deletes_Diff_txt .setToolTipText(tooltip);
//		_Deletes_Rate_txt .setEditable(false);
//		_Deletes_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of merge operations that have been executed on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip   .put    ("Merges", tooltip);
//		_Merges_lbl      .setText("Merges");
//		_Merges_lbl      .setToolTipText(tooltip);
//		_Merges_Abs_txt  .setToolTipText(tooltip);
//		_Merges_Abs_txt  .setEditable(false);
//		_Merges_Diff_txt .setEditable(false);
//		_Merges_Diff_txt .setToolTipText(tooltip);
//		_Merges_Rate_txt .setEditable(false);
//		_Merges_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of pages where data was retrieved without an index on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip          .put    ("TableAccesses", tooltip);
//		_TableAccesses_lbl      .setText("TableAccesses");
//		_TableAccesses_lbl      .setToolTipText(tooltip);
//		_TableAccesses_Abs_txt  .setToolTipText(tooltip);
//		_TableAccesses_Abs_txt  .setEditable(false);
//		_TableAccesses_Diff_txt .setEditable(false);
//		_TableAccesses_Diff_txt .setToolTipText(tooltip);
//		_TableAccesses_Rate_txt .setEditable(false);
//		_TableAccesses_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of pages where data was retrieved using an index on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip          .put    ("IndexAccesses", tooltip);
//		_IndexAccesses_lbl      .setText("IndexAccesses");
//		_IndexAccesses_lbl      .setToolTipText(tooltip);
//		_IndexAccesses_Abs_txt  .setToolTipText(tooltip);
//		_IndexAccesses_Abs_txt  .setEditable(false);
//		_IndexAccesses_Diff_txt .setEditable(false);
//		_IndexAccesses_Diff_txt .setToolTipText(tooltip);
//		_IndexAccesses_Rate_txt .setEditable(false);
//		_IndexAccesses_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of temporary tables created on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip          .put    ("TempDbObjects", tooltip);
//		_TempDbObjects_lbl      .setText("TempDbObjects");
//		_TempDbObjects_lbl      .setToolTipText(tooltip);
//		_TempDbObjects_Abs_txt  .setToolTipText(tooltip);
//		_TempDbObjects_Abs_txt  .setEditable(false);
//		_TempDbObjects_Diff_txt .setEditable(false);
//		_TempDbObjects_Diff_txt .setToolTipText(tooltip);
//		_TempDbObjects_Rate_txt .setEditable(false);
//		_TempDbObjects_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of work tables created on the server, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip       .put    ("WorkTables", tooltip);
//		_WorkTables_lbl      .setText("WorkTables");
//		_WorkTables_lbl      .setToolTipText(tooltip);
//		_WorkTables_Abs_txt  .setToolTipText(tooltip);
//		_WorkTables_Abs_txt  .setEditable(false);
//		_WorkTables_Diff_txt .setEditable(false);
//		_WorkTables_Diff_txt .setToolTipText(tooltip);
//		_WorkTables_Rate_txt .setEditable(false);
//		_WorkTables_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total number of times the User Log Cache was flushed, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip       .put    ("ULCFlushes", tooltip);
//		_ULCFlushes_lbl      .setText("ULCFlushes");
//		_ULCFlushes_lbl      .setToolTipText(tooltip);
//		_ULCFlushes_Abs_txt  .setToolTipText(tooltip);
//		_ULCFlushes_Abs_txt  .setEditable(false);
//		_ULCFlushes_Diff_txt .setEditable(false);
//		_ULCFlushes_Diff_txt .setToolTipText(tooltip);
//		_ULCFlushes_Rate_txt .setEditable(false);
//		_ULCFlushes_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of times the User Log Cache was flushed because it was full, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip         .put    ("ULCFlushFull", tooltip);
//		_ULCFlushFull_lbl      .setText("ULCFlushFull");
//		_ULCFlushFull_lbl      .setToolTipText(tooltip);
//		_ULCFlushFull_Abs_txt  .setToolTipText(tooltip);
//		_ULCFlushFull_Abs_txt  .setEditable(false);
//		_ULCFlushFull_Diff_txt .setEditable(false);
//		_ULCFlushFull_Diff_txt .setToolTipText(tooltip);
//		_ULCFlushFull_Rate_txt .setEditable(false);
//		_ULCFlushFull_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of kilobytes written to the user log cache, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip         .put    ("ULCKBWritten", tooltip);
//		_ULCKBWritten_lbl      .setText("ULCKBWritten");
//		_ULCKBWritten_lbl      .setToolTipText(tooltip);
//		_ULCKBWritten_Abs_txt  .setToolTipText(tooltip);
//		_ULCKBWritten_Abs_txt  .setEditable(false);
//		_ULCKBWritten_Diff_txt .setEditable(false);
//		_ULCKBWritten_Diff_txt .setToolTipText(tooltip);
//		_ULCKBWritten_Rate_txt .setEditable(false);
//		_ULCKBWritten_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of pages read on server wide, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip      .put    ("PagesRead", tooltip);
//		_PagesRead_lbl      .setText("PagesRead");
//		_PagesRead_lbl      .setToolTipText(tooltip);
//		_PagesRead_Abs_txt  .setToolTipText(tooltip);
//		_PagesRead_Abs_txt  .setEditable(false);
//		_PagesRead_Diff_txt .setEditable(false);
//		_PagesRead_Diff_txt .setToolTipText(tooltip);
//		_PagesRead_Rate_txt .setEditable(false);
//		_PagesRead_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of pages written on server wide, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip         .put    ("PagesWritten", tooltip);
//		_PagesWritten_lbl      .setText("PagesWritten");
//		_PagesWritten_lbl      .setToolTipText(tooltip);
//		_PagesWritten_Abs_txt  .setToolTipText(tooltip);
//		_PagesWritten_Abs_txt  .setEditable(false);
//		_PagesWritten_Diff_txt .setEditable(false);
//		_PagesWritten_Diff_txt .setToolTipText(tooltip);
//		_PagesWritten_Rate_txt .setEditable(false);
//		_PagesWritten_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of buffers read from the disk, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip          .put    ("PhysicalReads", tooltip);
//		_PhysicalReads_lbl      .setText("PhysicalReads");
//		_PhysicalReads_lbl      .setToolTipText(tooltip);
//		_PhysicalReads_Abs_txt  .setToolTipText(tooltip);
//		_PhysicalReads_Abs_txt  .setEditable(false);
//		_PhysicalReads_Diff_txt .setEditable(false);
//		_PhysicalReads_Diff_txt .setToolTipText(tooltip);
//		_PhysicalReads_Rate_txt .setEditable(false);
//		_PhysicalReads_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of buffers written to the disk, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip           .put    ("PhysicalWrites", tooltip);
//		_PhysicalWrites_lbl      .setText("PhysicalWrites");
//		_PhysicalWrites_lbl      .setToolTipText(tooltip);
//		_PhysicalWrites_Abs_txt  .setToolTipText(tooltip);
//		_PhysicalWrites_Abs_txt  .setEditable(false);
//		_PhysicalWrites_Diff_txt .setEditable(false);
//		_PhysicalWrites_Diff_txt .setToolTipText(tooltip);
//		_PhysicalWrites_Rate_txt .setEditable(false);
//		_PhysicalWrites_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of buffers read from cache, (abs, diff, rate). Only available from ASE 15.7 SP100.";
//		_originToolTip         .put    ("LogicalReads", tooltip);
//		_LogicalReads_lbl      .setText("LogicalReads");
//		_LogicalReads_lbl      .setToolTipText(tooltip);
//		_LogicalReads_Abs_txt  .setToolTipText(tooltip);
//		_LogicalReads_Abs_txt  .setEditable(false);
//		_LogicalReads_Diff_txt .setEditable(false);
//		_LogicalReads_Diff_txt .setToolTipText(tooltip);
//		_LogicalReads_Rate_txt .setEditable(false);
//		_LogicalReads_Rate_txt .setToolTipText(tooltip);
//
//		tooltip = "Number of databases that has a full transaction log, which probably means suspended SPID's.";
//		_fullTranslog_lbl.setText("Full Transaction Logs");
//		_fullTranslog_lbl.setToolTipText(tooltip);
//		_fullTranslog_txt.setToolTipText(tooltip);
//		_fullTranslog_txt.setEditable(false);
//
////		tooltip = "<html>Oldest Open Transaction in any database, presented in seconds.<br>" +
////				"Check Performance Counter '"+CmOpenDatabases.SHORT_NAME+"' for details.<br>" +
////				"<br>" +
////				"<b>Note</b>: if value is -99, this means that you did not have access to the 'master..syslogshold' table.</html>";
//		_oldestOpenTran_lbl.setText("Oldest Open Tran");
//		_oldestOpenTran_lbl.setToolTipText(tooltip);
//		_oldestOpenTran_txt.setToolTipText(tooltip);
//		_oldestOpenTran_txt.setEditable(false);
//
//		
//		
//		tooltip = "How many times has this IQ been rebooted.";
//		_bootcount_lbl      .setText("Boot Count");
//		_bootcount_lbl      .setToolTipText(tooltip);
//		_bootcount_txt      .setToolTipText(tooltip);
//		_bootcount_txt      .setEditable(false);
//
//		tooltip = "If the IQ is in-recovery, this would be the reason.";
//		_recoveryState_lbl      .setText("Recovery State");
//		_recoveryState_lbl      .setToolTipText(tooltip);
//		_recoveryState_txt      .setToolTipText(tooltip);
//		_recoveryState_txt      .setEditable(false);
//
//		tooltip = "CPU Time. Global variable @@cpu_busy + @@io_busy.";
////		_cpuTime_lbl      .setText("CPU Usage");
//		_cpuTime_lbl      .setText("CPU Time/User/Sys");
//		_cpuTime_lbl      .setToolTipText(tooltip);
//		_cpuTime_txt      .setToolTipText(tooltip);
//		_cpuTime_txt      .setEditable(false);
//
//		tooltip = "CPU Busy. Global variable @@cpu_busy.";
//		_cpuUser_lbl      .setText("CPU User");
//		_cpuUser_lbl      .setToolTipText(tooltip);
//		_cpuUser_txt      .setToolTipText(tooltip);
//		_cpuUser_txt      .setEditable(false);
//
//		tooltip = "CPU spent in IO. Global variable @@io_busy.";
//		_cpuSystem_lbl      .setText("CPU System");
//		_cpuSystem_lbl      .setToolTipText(tooltip);
//		_cpuSystem_txt      .setToolTipText(tooltip);
//		_cpuSystem_txt      .setEditable(false);
//
//		tooltip = "CPU Idle. Global variable @@idle.";
//		_cpuIdle_lbl      .setText("CPU Idle");
//		_cpuIdle_lbl      .setToolTipText(tooltip);
//		_cpuIdle_txt      .setToolTipText(tooltip);
//		_cpuIdle_txt      .setEditable(false);
//
//		tooltip = "Total Read IO's. Global variable @@total_read.";
//		_ioTotalRead_lbl      .setText("IO Read");
//		_ioTotalRead_lbl      .setToolTipText(tooltip);
//		_ioTotalRead_txt      .setToolTipText(tooltip);
//		_ioTotalRead_txt      .setEditable(false);
//		_ioTotalReadDiff_txt  .setEditable(false);
//		_ioTotalReadDiff_txt  .setToolTipText(tooltip);
//
//		tooltip = "Total Write IO's. Global variable @@total_write.";
//		_ioTotalWrite_lbl      .setText("IO Write");
//		_ioTotalWrite_lbl      .setToolTipText(tooltip);
//		_ioTotalWrite_txt      .setToolTipText(tooltip);
//		_ioTotalWrite_txt      .setEditable(false);
//		_ioTotalWriteDiff_txt  .setEditable(false);
//		_ioTotalWriteDiff_txt  .setToolTipText(tooltip);
//
//		tooltip = "Total Connection that was attemped to make to the IQ Server, even those that failes. Global variable @@connections. (abs/diff/rate)";
//		_aaConnections_lbl     .setText("Connections Tried");
//		_aaConnections_lbl     .setToolTipText(tooltip);
//		_aaConnectionsAbs_txt  .setEditable(false);
//		_aaConnectionsAbs_txt  .setToolTipText(tooltip);
//		_aaConnectionsDiff_txt .setEditable(false);
//		_aaConnectionsDiff_txt .setToolTipText(tooltip);
//		_aaConnectionsRate_txt .setEditable(false);
//		_aaConnectionsRate_txt .setToolTipText(tooltip);
//
//		tooltip = "Total Network Packets Received. Global variable @@pack_received.";
//		_packReceived_lbl      .setText("NW Packet Received");
//		_packReceived_lbl      .setToolTipText(tooltip);
//		_packReceived_txt      .setToolTipText(tooltip);
//		_packReceived_txt      .setEditable(false);
//		_packReceivedDiff_txt  .setEditable(false);
//		_packReceivedDiff_txt  .setToolTipText(tooltip);
//
//		tooltip = "Total Network Packets Sent. Global variable @@pack_sent.";
//		_packSent_lbl      .setText("NW Packet Sent");
//		_packSent_lbl      .setToolTipText(tooltip);
//		_packSent_txt      .setToolTipText(tooltip);
//		_packSent_txt      .setEditable(false);
//		_packSentDiff_txt  .setEditable(false);
//		_packSentDiff_txt  .setToolTipText(tooltip);
//
//		tooltip = "Total Network Packets Errors. Global variable @@packet_errors.";
//		_packetErrors_lbl      .setText("NW Packet Errors");
//		_packetErrors_lbl      .setToolTipText(tooltip);
//		_packetErrors_txt      .setToolTipText(tooltip);
//		_packetErrors_txt      .setEditable(false);
//		_packetErrorsDiff_txt  .setEditable(false);
//		_packetErrorsDiff_txt  .setToolTipText(tooltip);
//
//		tooltip = "Total Errors. Global variable @@total_errors.";
//		_totalErrors_lbl      .setText("Total Errors");
//		_totalErrors_lbl      .setToolTipText(tooltip);
//		_totalErrors_txt      .setToolTipText(tooltip);
//		_totalErrors_txt      .setEditable(false);
//		_totalErrorsDiff_txt  .setEditable(false);
//		_totalErrorsDiff_txt  .setToolTipText(tooltip);

		
		//--------------------------
		// DO the LAYOUT
		//--------------------------
		panel.add(_localServerName_lbl,     "");
		panel.add(_localServerName_txt,     "growx, wrap");
		
		panel.add(_atAtServerName_lbl,      "");
		panel.add(_atAtServerName_txt,      "growx, wrap");
		
		panel.add(_listeners_lbl,           "");
		panel.add(_listeners_txt,           "growx, wrap");
		
		panel.add(_srvVersion_lbl,          "");
		panel.add(_srvVersion_txt,          "growx, wrap");
		
		panel.add(_srvPageSize_lbl,         "");
		panel.add(_srvPageSize_txt,         "growx, wrap");
		
		panel.add(_lastSampleTime_lbl,      "");
		panel.add(_lastSampleTime_txt,      "growx, wrap");
		
		panel.add(_utcTimeDiff_lbl,         "");
		panel.add(_utcTimeDiff_txt,         "growx, wrap");
		


		
		panel.add(_ActiveReq_lbl,                       "hidemode 3");
		panel.add(_ActiveReq_Abs_txt,                   "hidemode 3, growx, split");
		panel.add(_ActiveReq_Diff_txt,                  "hidemode 3, growx, split");
		panel.add(_ActiveReq_Rate_txt,                  "hidemode 3, growx, wrap");
		
		panel.add(_BytesReceived_lbl,                   "hidemode 3");
		panel.add(_BytesReceived_Abs_txt,               "hidemode 3, growx, split");
		panel.add(_BytesReceived_Diff_txt,              "hidemode 3, growx, split");
		panel.add(_BytesReceived_Rate_txt,              "hidemode 3, growx, wrap");
		
		panel.add(_BytesSent_lbl,                       "hidemode 3");
		panel.add(_BytesSent_Abs_txt,                   "hidemode 3, growx, split");
		panel.add(_BytesSent_Diff_txt,                  "hidemode 3, growx, split");
		panel.add(_BytesSent_Rate_txt,                  "hidemode 3, growx, wrap");
		
		panel.add(_Chkpt_lbl,                           "hidemode 3");
		panel.add(_Chkpt_Abs_txt,                       "hidemode 3, growx, split");
		panel.add(_Chkpt_Diff_txt,                      "hidemode 3, growx, split");
		panel.add(_Chkpt_Rate_txt,                      "hidemode 3, growx, wrap");
		
		panel.add(_CurrentCacheSize_Kb_lbl,             "hidemode 3");
		panel.add(_CurrentCacheSize_Kb_Abs_txt,         "hidemode 3, growx, split");
		panel.add(_CurrentCacheSize_Kb_Diff_txt,        "hidemode 3, growx, split");
		panel.add(_CurrentCacheSize_Kb_Rate_txt,        "hidemode 3, growx, wrap");
		
		panel.add(_LockedHeapPages_lbl,                 "hidemode 3");
		panel.add(_LockedHeapPages_Abs_txt,             "hidemode 3, growx, split");
		panel.add(_LockedHeapPages_Diff_txt,            "hidemode 3, growx, split");
		panel.add(_LockedHeapPages_Rate_txt,            "hidemode 3, growx, wrap");
		                                                
		panel.add(_MainHeapBytes_lbl,                   "hidemode 3");
		panel.add(_MainHeapBytes_Abs_txt,               "hidemode 3, growx, split");
		panel.add(_MainHeapBytes_Diff_txt,              "hidemode 3, growx, split");
		panel.add(_MainHeapBytes_Rate_txt,              "hidemode 3, growx, wrap");
		                                                
		panel.add(_MainHeapPages_lbl,                   "hidemode 3");
		panel.add(_MainHeapPages_Abs_txt,               "hidemode 3, growx, split");
		panel.add(_MainHeapPages_Diff_txt,              "hidemode 3, growx, split");
		panel.add(_MainHeapPages_Rate_txt,              "hidemode 3, growx, wrap");
		                                                
		panel.add(_MaxCacheSize_lbl,                    "hidemode 3");
		panel.add(_MaxCacheSize_Abs_txt,                "hidemode 3, growx, split");
		panel.add(_MaxCacheSize_Diff_txt,               "hidemode 3, growx, split");
		panel.add(_MaxCacheSize_Rate_txt,               "hidemode 3, growx, wrap");
		                                                
		panel.add(_MinCacheSize_lbl,                    "hidemode 3");
		panel.add(_MinCacheSize_Abs_txt,                "hidemode 3, growx, split");
		panel.add(_MinCacheSize_Diff_txt,               "hidemode 3, growx, split");
		panel.add(_MinCacheSize_Rate_txt,               "hidemode 3, growx, wrap");
		
		panel.add(_MultiPacketsReceived_lbl,            "hidemode 3");
		panel.add(_MultiPacketsReceived_Abs_txt,        "hidemode 3, growx, split");
		panel.add(_MultiPacketsReceived_Diff_txt,       "hidemode 3, growx, split");
		panel.add(_MultiPacketsReceived_Rate_txt,       "hidemode 3, growx, wrap");
		
		panel.add(_MultiPacketsSent_lbl,                "hidemode 3");
		panel.add(_MultiPacketsSent_Abs_txt,            "hidemode 3, growx, split");
		panel.add(_MultiPacketsSent_Diff_txt,           "hidemode 3, growx, split");
		panel.add(_MultiPacketsSent_Rate_txt,           "hidemode 3, growx, wrap");
		
		panel.add(_NumProcessorsAvail_lbl,              "hidemode 3");
		panel.add(_NumProcessorsAvail_Abs_txt,          "hidemode 3, growx, split");
		panel.add(_NumProcessorsAvail_Diff_txt,         "hidemode 3, growx, split");
		panel.add(_NumProcessorsAvail_Rate_txt,         "hidemode 3, growx, wrap");
		
		panel.add(_PacketsReceived_lbl,                 "hidemode 3");
		panel.add(_PacketsReceived_Abs_txt,             "hidemode 3, growx, split");
		panel.add(_PacketsReceived_Diff_txt,            "hidemode 3, growx, split");
		panel.add(_PacketsReceived_Rate_txt,            "hidemode 3, growx, wrap");
		
		panel.add(_PacketsSent_lbl,                     "hidemode 3");
		panel.add(_PacketsSent_Abs_txt,                 "hidemode 3, growx, split");
		panel.add(_PacketsSent_Diff_txt,                "hidemode 3, growx, split");
		panel.add(_PacketsSent_Rate_txt,                "hidemode 3, growx, wrap");
		
		panel.add(_ProcessCPU_lbl,                      "hidemode 3");
		panel.add(_ProcessCPU_Abs_txt,                  "hidemode 3, growx, split");
		panel.add(_ProcessCPU_Diff_txt,                 "hidemode 3, growx, split");
		panel.add(_ProcessCPU_Rate_txt,                 "hidemode 3, growx, wrap");
		
		panel.add(_ProcessCPUSystem_lbl,                "hidemode 3");
		panel.add(_ProcessCPUSystem_Abs_txt,            "hidemode 3, growx, split");
		panel.add(_ProcessCPUSystem_Diff_txt,           "hidemode 3, growx, split");
		panel.add(_ProcessCPUSystem_Rate_txt,           "hidemode 3, growx, wrap");
		
		panel.add(_ProcessCPUUser_lbl,                  "hidemode 3");
		panel.add(_ProcessCPUUser_Abs_txt,              "hidemode 3, growx, split");
		panel.add(_ProcessCPUUser_Diff_txt,             "hidemode 3, growx, split");
		panel.add(_ProcessCPUUser_Rate_txt,             "hidemode 3, growx, wrap");
		
		panel.add(_threads_lbl,                         "hidemode 3");
		panel.add(_threads_Abs_txt,                     "hidemode 3, growx, split");
		panel.add(_threads_Diff_txt,                    "hidemode 3, growx, split");
		panel.add(_threads_Rate_txt,                    "hidemode 3, growx, wrap");
		
		panel.add(_ConnCount_lbl,                       "hidemode 3");
		panel.add(_ConnCount_Abs_txt,                   "hidemode 3, growx, split");
		panel.add(_ConnCount_Diff_txt,                  "hidemode 3, growx, split");
		panel.add(_ConnCount_Rate_txt,                  "hidemode 3, growx, wrap");
		
		panel.add(_UnschReq_lbl,                        "hidemode 3");
		panel.add(_UnschReq_Abs_txt,                    "hidemode 3, growx, split");
		panel.add(_UnschReq_Diff_txt,                   "hidemode 3, growx, split");
		panel.add(_UnschReq_Rate_txt,                   "hidemode 3, growx, wrap");
		
		panel.add(_NumLogicalProcessorsUsed_lbl,        "hidemode 3");
		panel.add(_NumLogicalProcessorsUsed_Abs_txt,    "hidemode 3, growx, split");
		panel.add(_NumLogicalProcessorsUsed_Diff_txt,   "hidemode 3, growx, split");
		panel.add(_NumLogicalProcessorsUsed_Rate_txt,   "hidemode 3, growx, wrap");
		
		panel.add(_NumPhysicalProcessorsUsed_lbl,       "hidemode 3");
		panel.add(_NumPhysicalProcessorsUsed_Abs_txt,   "hidemode 3, growx, split");
		panel.add(_NumPhysicalProcessorsUsed_Diff_txt,  "hidemode 3, growx, split");
		panel.add(_NumPhysicalProcessorsUsed_Rate_txt,  "hidemode 3, growx, wrap");
		
		panel.add(_Commit_lbl,                          "hidemode 3");
		panel.add(_Commit_Abs_txt,                      "hidemode 3, growx, split");
		panel.add(_Commit_Diff_txt,                     "hidemode 3, growx, split");
		panel.add(_Commit_Rate_txt,                     "hidemode 3, growx, wrap");
		
		panel.add(_RequestsReceived_lbl,                "hidemode 3");
		panel.add(_RequestsReceived_Abs_txt,            "hidemode 3, growx, split");
		panel.add(_RequestsReceived_Diff_txt,           "hidemode 3, growx, split");
		panel.add(_RequestsReceived_Rate_txt,           "hidemode 3, growx, wrap");
		
		
//		panel.add(_startDate_lbl,           "gapy 20");
//		panel.add(_startDate_txt,           "growx, wrap");
//		
//		panel.add(_daysRunning_lbl,         "");
//		panel.add(_daysRunning_txt,         "growx, wrap");
//		
//		panel.add(_countersCleared_lbl,     "");
//		panel.add(_countersCleared_txt,     "growx, wrap");
//		
//		panel.add(_checkPoints_lbl,         "");
//		panel.add(_checkPoints_txt,         "growx, wrap");
//		
//		panel.add(_numDeadlocks_lbl,        "");
//		panel.add(_numDeadlocks_txt,        "growx, split");
//		panel.add(_numDeadlocksDiff_txt,    "growx, wrap");
//		
//		panel.add(_diagnosticDumps_lbl,     "");
//		panel.add(_diagnosticDumps_txt,     "growx, wrap");
//		
//		panel.add(_connections_lbl,         "");
//		panel.add(_connections_txt,         "growx, split");
//		panel.add(_connectionsDiff_txt,     "growx, wrap");
//		
//		panel.add(_distinctLogins_lbl,      "");
//		panel.add(_distinctLoginsAbs_txt,   "growx, split");
//		panel.add(_distinctLoginsDiff_txt,  "growx, wrap");
//		
//		panel.add(_lockWaitThreshold_lbl,   "");
//		panel.add(_lockWaitThreshold_txt,   "growx, wrap");
//		
//		panel.add(_lockWaits_lbl,           "");
//		panel.add(_lockWaits_txt,           "growx, split");
//		panel.add(_lockWaitsDiff_txt,       "growx, wrap");
//		
//		panel.add(_maxRecovery_lbl,         "");
//		panel.add(_maxRecovery_txt,         "growx, wrap");
//		
//		panel.add(_Transactions_lbl,        "hidemode 3");
//		panel.add(_Transactions_Abs_txt,    "hidemode 3, growx, split");
//		panel.add(_Transactions_Diff_txt,   "hidemode 3, growx, split");
//		panel.add(_Transactions_Rate_txt,   "hidemode 3, growx, wrap");
//		
//		panel.add(_Rollbacks_lbl,           "hidemode 3");
//		panel.add(_Rollbacks_Abs_txt,       "hidemode 3, growx, split");
//		panel.add(_Rollbacks_Diff_txt,      "hidemode 3, growx, split");
//		panel.add(_Rollbacks_Rate_txt,      "hidemode 3, growx, wrap");
//
//		panel.add(_Selects_lbl,             "hidemode 3");
//		panel.add(_Selects_Abs_txt,         "hidemode 3, growx, split");
//		panel.add(_Selects_Diff_txt,        "hidemode 3, growx, split");
//		panel.add(_Selects_Rate_txt,        "hidemode 3, growx, wrap");
//
//		panel.add(_Updates_lbl,             "hidemode 3");
//		panel.add(_Updates_Abs_txt,         "hidemode 3, growx, split");
//		panel.add(_Updates_Diff_txt,        "hidemode 3, growx, split");
//		panel.add(_Updates_Rate_txt,        "hidemode 3, growx, wrap");
//
//		panel.add(_Inserts_lbl,             "hidemode 3");
//		panel.add(_Inserts_Abs_txt,         "hidemode 3, growx, split");
//		panel.add(_Inserts_Diff_txt,        "hidemode 3, growx, split");
//		panel.add(_Inserts_Rate_txt,        "hidemode 3, growx, wrap");
//
//		panel.add(_Deletes_lbl,             "hidemode 3");
//		panel.add(_Deletes_Abs_txt,         "hidemode 3, growx, split");
//		panel.add(_Deletes_Diff_txt,        "hidemode 3, growx, split");
//		panel.add(_Deletes_Rate_txt,        "hidemode 3, growx, wrap");
//
//		panel.add(_Merges_lbl,              "hidemode 3");
//		panel.add(_Merges_Abs_txt,          "hidemode 3, growx, split");
//		panel.add(_Merges_Diff_txt,         "hidemode 3, growx, split");
//		panel.add(_Merges_Rate_txt,         "hidemode 3, growx, wrap");
//
//		panel.add(_TableAccesses_lbl,       "hidemode 3");
//		panel.add(_TableAccesses_Abs_txt,   "hidemode 3, growx, split");
//		panel.add(_TableAccesses_Diff_txt,  "hidemode 3, growx, split");
//		panel.add(_TableAccesses_Rate_txt,  "hidemode 3, growx, wrap");
//
//		panel.add(_IndexAccesses_lbl,       "hidemode 3");
//		panel.add(_IndexAccesses_Abs_txt,   "hidemode 3, growx, split");
//		panel.add(_IndexAccesses_Diff_txt,  "hidemode 3, growx, split");
//		panel.add(_IndexAccesses_Rate_txt,  "hidemode 3, growx, wrap");
//
//		panel.add(_TempDbObjects_lbl,       "hidemode 3");
//		panel.add(_TempDbObjects_Abs_txt,   "hidemode 3, growx, split");
//		panel.add(_TempDbObjects_Diff_txt,  "hidemode 3, growx, split");
//		panel.add(_TempDbObjects_Rate_txt,  "hidemode 3, growx, wrap");
//
//		panel.add(_WorkTables_lbl,          "hidemode 3");
//		panel.add(_WorkTables_Abs_txt,      "hidemode 3, growx, split");
//		panel.add(_WorkTables_Diff_txt,     "hidemode 3, growx, split");
//		panel.add(_WorkTables_Rate_txt,     "hidemode 3, growx, wrap");
//
//		panel.add(_ULCFlushes_lbl,          "hidemode 3");
//		panel.add(_ULCFlushes_Abs_txt,      "hidemode 3, growx, split");
//		panel.add(_ULCFlushes_Diff_txt,     "hidemode 3, growx, split");
//		panel.add(_ULCFlushes_Rate_txt,     "hidemode 3, growx, wrap");
//
//		panel.add(_ULCFlushFull_lbl,        "hidemode 3");
//		panel.add(_ULCFlushFull_Abs_txt,    "hidemode 3, growx, split");
//		panel.add(_ULCFlushFull_Diff_txt,   "hidemode 3, growx, split");
//		panel.add(_ULCFlushFull_Rate_txt,   "hidemode 3, growx, wrap");
//
//		panel.add(_ULCKBWritten_lbl,        "hidemode 3");
//		panel.add(_ULCKBWritten_Abs_txt,    "hidemode 3, growx, split");
//		panel.add(_ULCKBWritten_Diff_txt,   "hidemode 3, growx, split");
//		panel.add(_ULCKBWritten_Rate_txt,   "hidemode 3, growx, wrap");
//
//		panel.add(_PagesRead_lbl,           "hidemode 3");
//		panel.add(_PagesRead_Abs_txt,       "hidemode 3, growx, split");
//		panel.add(_PagesRead_Diff_txt,      "hidemode 3, growx, split");
//		panel.add(_PagesRead_Rate_txt,      "hidemode 3, growx, wrap");
//
//		panel.add(_PagesWritten_lbl,        "hidemode 3");
//		panel.add(_PagesWritten_Abs_txt,    "hidemode 3, growx, split");
//		panel.add(_PagesWritten_Diff_txt,   "hidemode 3, growx, split");
//		panel.add(_PagesWritten_Rate_txt,   "hidemode 3, growx, wrap");
//
//		panel.add(_PhysicalReads_lbl,       "hidemode 3");
//		panel.add(_PhysicalReads_Abs_txt,   "hidemode 3, growx, split");
//		panel.add(_PhysicalReads_Diff_txt,  "hidemode 3, growx, split");
//		panel.add(_PhysicalReads_Rate_txt,  "hidemode 3, growx, wrap");
//
//		panel.add(_PhysicalWrites_lbl,      "hidemode 3");
//		panel.add(_PhysicalWrites_Abs_txt,  "hidemode 3, growx, split");
//		panel.add(_PhysicalWrites_Diff_txt, "hidemode 3, growx, split");
//		panel.add(_PhysicalWrites_Rate_txt, "hidemode 3, growx, wrap");
//
//		panel.add(_LogicalReads_lbl,        "hidemode 3");
//		panel.add(_LogicalReads_Abs_txt,    "hidemode 3, growx, split");
//		panel.add(_LogicalReads_Diff_txt,   "hidemode 3, growx, split");
//		panel.add(_LogicalReads_Rate_txt,   "hidemode 3, growx, wrap");
//
//		panel.add(_fullTranslog_lbl,        "");
//		panel.add(_fullTranslog_txt,        "growx, wrap");
//		
//		panel.add(_oldestOpenTran_lbl,      "");
//		panel.add(_oldestOpenTran_txt,      "growx, wrap");
//		
//
//		
//		panel.add(_bootcount_lbl,           "gapy 20");
//		panel.add(_bootcount_txt,           "growx, wrap");
//		
//		panel.add(_recoveryState_lbl,       "");
//		panel.add(_recoveryState_txt,       "growx, wrap");
//		
////		panel.add(_cpuTime_lbl,             "");
////		panel.add(_cpuTime_txt,             "growx, wrap");
////		panel.add(_cpuUser_lbl,             "");
////		panel.add(_cpuUser_txt,             "growx, wrap");
////		panel.add(_cpuSystem_lbl,           "");
////		panel.add(_cpuSystem_txt,           "growx, wrap");
//
//		panel.add(_cpuTime_lbl,             "");
//		panel.add(_cpuTime_txt,             "growx, split");
//		panel.add(_cpuUser_txt,             "growx, split");
//		panel.add(_cpuSystem_txt,           "growx, wrap");
//
//		panel.add(_cpuIdle_lbl,             "");
//		panel.add(_cpuIdle_txt,             "growx, wrap");
//		
//		panel.add(_ioTotalRead_lbl,         "");
//		panel.add(_ioTotalRead_txt,         "growx, split");
//		panel.add(_ioTotalReadDiff_txt,     "growx, wrap");
//		
//		panel.add(_ioTotalWrite_lbl,        "");
//		panel.add(_ioTotalWrite_txt,        "growx, split");
//		panel.add(_ioTotalWriteDiff_txt,    "growx, wrap");
//		
//		panel.add(_aaConnections_lbl,       "");
//		panel.add(_aaConnectionsAbs_txt,    "growx, split");
//		panel.add(_aaConnectionsDiff_txt,   "growx, split");
//		panel.add(_aaConnectionsRate_txt,   "growx, wrap");
//
//		panel.add(_packReceived_lbl,        "");
//		panel.add(_packReceived_txt,        "growx, split");
//		panel.add(_packReceivedDiff_txt,    "growx, wrap");
//		
//		panel.add(_packSent_lbl,            "");
//		panel.add(_packSent_txt,            "growx, split");
//		panel.add(_packSentDiff_txt,        "growx, wrap");
//		
//		panel.add(_packetErrors_lbl,        "");
//		panel.add(_packetErrors_txt,        "growx, split");
//		panel.add(_packetErrorsDiff_txt,    "growx, wrap");
//		
//		panel.add(_totalErrors_lbl,         "");
//		panel.add(_totalErrors_txt,         "growx, split");
//		panel.add(_totalErrorsDiff_txt,     "growx, wrap");
				
		setComponentProperties();

		return panel;
	}

	@Override
	public void setComponentProperties()
	{
		// SET initial visibility based of config show ABS/DIFF/RATE
		Configuration conf = Configuration.getCombinedConfiguration();
		boolean showAbs  = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showAbs,   MainFrame.DEFAULT_summaryOperations_showAbs);
		boolean showDiff = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showDiff,  MainFrame.DEFAULT_summaryOperations_showDiff);
		boolean showRate = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showRate,  MainFrame.DEFAULT_summaryOperations_showRate);

		_ActiveReq_Abs_txt                  .setVisible(showAbs); 
		_ActiveReq_Diff_txt                 .setVisible(showDiff);
		_ActiveReq_Rate_txt                 .setVisible(showRate);

		_BytesReceived_Abs_txt              .setVisible(showAbs); 
		_BytesReceived_Diff_txt             .setVisible(showDiff);
		_BytesReceived_Rate_txt             .setVisible(showRate);

		_BytesSent_Abs_txt                  .setVisible(showAbs); 
		_BytesSent_Diff_txt                 .setVisible(showDiff);
		_BytesSent_Rate_txt                 .setVisible(showRate);

		_Chkpt_Abs_txt                      .setVisible(showAbs); 
		_Chkpt_Diff_txt                     .setVisible(showDiff);
		_Chkpt_Rate_txt                     .setVisible(showRate);

		_CurrentCacheSize_Kb_Abs_txt        .setVisible(showAbs); 
		_CurrentCacheSize_Kb_Diff_txt       .setVisible(showDiff);
		_CurrentCacheSize_Kb_Rate_txt       .setVisible(showRate);

		_LockedHeapPages_Abs_txt            .setVisible(showAbs); 
		_LockedHeapPages_Diff_txt           .setVisible(showDiff);
		_LockedHeapPages_Rate_txt           .setVisible(showRate);

		_MainHeapBytes_Abs_txt              .setVisible(showAbs); 
		_MainHeapBytes_Diff_txt             .setVisible(showDiff);
		_MainHeapBytes_Rate_txt             .setVisible(showRate);
                                            
		_MainHeapPages_Abs_txt              .setVisible(showAbs); 
		_MainHeapPages_Diff_txt             .setVisible(showDiff);
		_MainHeapPages_Rate_txt             .setVisible(showRate);
                                            
		_MaxCacheSize_Abs_txt               .setVisible(showAbs); 
		_MaxCacheSize_Diff_txt              .setVisible(showDiff);
		_MaxCacheSize_Rate_txt              .setVisible(showRate);

		_MultiPacketsReceived_Abs_txt       .setVisible(showAbs); 
		_MultiPacketsReceived_Diff_txt      .setVisible(showDiff);
		_MultiPacketsReceived_Rate_txt      .setVisible(showRate);

		_MultiPacketsSent_Abs_txt           .setVisible(showAbs); 
		_MultiPacketsSent_Diff_txt          .setVisible(showDiff);
		_MultiPacketsSent_Rate_txt          .setVisible(showRate);
                                          
		_NumProcessorsAvail_Abs_txt         .setVisible(showAbs); 
		_NumProcessorsAvail_Diff_txt        .setVisible(showDiff);
		_NumProcessorsAvail_Rate_txt        .setVisible(showRate);

		_PacketsReceived_Abs_txt            .setVisible(showAbs); 
		_PacketsReceived_Diff_txt           .setVisible(showDiff);
		_PacketsReceived_Rate_txt           .setVisible(showRate);

		_PacketsSent_Abs_txt                .setVisible(showAbs); 
		_PacketsSent_Diff_txt               .setVisible(showDiff);
		_PacketsSent_Rate_txt               .setVisible(showRate);
                                           
		_ProcessCPU_Abs_txt                 .setVisible(showAbs); 
		_ProcessCPU_Diff_txt                .setVisible(showDiff);
		_ProcessCPU_Rate_txt                .setVisible(showRate);

		_ProcessCPUSystem_Abs_txt           .setVisible(showAbs); 
		_ProcessCPUSystem_Diff_txt          .setVisible(showDiff);
		_ProcessCPUSystem_Rate_txt          .setVisible(showRate);
                                            
		_ProcessCPUUser_Abs_txt             .setVisible(showAbs); 
		_ProcessCPUUser_Diff_txt            .setVisible(showDiff);
		_ProcessCPUUser_Rate_txt            .setVisible(showRate);

		_threads_Abs_txt                    .setVisible(showAbs); 
		_threads_Diff_txt                   .setVisible(showDiff);
		_threads_Rate_txt                   .setVisible(showRate);
                                           
		_ConnCount_Abs_txt                  .setVisible(showAbs); 
		_ConnCount_Diff_txt                 .setVisible(showDiff);
		_ConnCount_Rate_txt                 .setVisible(showRate);
                                           
		_UnschReq_Abs_txt                   .setVisible(showAbs); 
		_UnschReq_Diff_txt                  .setVisible(showDiff);
		_UnschReq_Rate_txt                  .setVisible(showRate);

		_NumLogicalProcessorsUsed_Abs_txt   .setVisible(showAbs); 
		_NumLogicalProcessorsUsed_Diff_txt  .setVisible(showDiff);
		_NumLogicalProcessorsUsed_Rate_txt  .setVisible(showRate);

		_NumPhysicalProcessorsUsed_Abs_txt  .setVisible(showAbs); 
		_NumPhysicalProcessorsUsed_Diff_txt .setVisible(showDiff);
		_NumPhysicalProcessorsUsed_Rate_txt .setVisible(showRate);
                                            
		_Commit_Abs_txt                     .setVisible(showAbs); 
		_Commit_Diff_txt                    .setVisible(showDiff);
		_Commit_Rate_txt                    .setVisible(showRate);
                                            
		_RequestsReceived_Abs_txt           .setVisible(showAbs); 
		_RequestsReceived_Diff_txt          .setVisible(showDiff);
		_RequestsReceived_Rate_txt          .setVisible(showRate);

//		_Transactions_Abs_txt   .setVisible(showAbs); 
//		_Transactions_Diff_txt  .setVisible(showDiff);
//		_Transactions_Rate_txt  .setVisible(showRate);
//
//		_Rollbacks_Abs_txt      .setVisible(showAbs);
//		_Rollbacks_Diff_txt     .setVisible(showDiff);
//		_Rollbacks_Rate_txt     .setVisible(showRate);
//
//		_Selects_Abs_txt        .setVisible(showAbs);
//		_Selects_Diff_txt       .setVisible(showDiff);
//		_Selects_Rate_txt       .setVisible(showRate);
//
//		_Updates_Abs_txt        .setVisible(showAbs);
//		_Updates_Diff_txt       .setVisible(showDiff);
//		_Updates_Rate_txt       .setVisible(showRate);
//
//		_Inserts_Abs_txt        .setVisible(showAbs);
//		_Inserts_Diff_txt       .setVisible(showDiff);
//		_Inserts_Rate_txt       .setVisible(showRate);
//
//		_Deletes_Abs_txt        .setVisible(showAbs);
//		_Deletes_Diff_txt       .setVisible(showDiff);
//		_Deletes_Rate_txt       .setVisible(showRate);
//
//		_Merges_Abs_txt         .setVisible(showAbs);
//		_Merges_Diff_txt        .setVisible(showDiff);
//		_Merges_Rate_txt        .setVisible(showRate);
//
//		_TableAccesses_Abs_txt  .setVisible(showAbs);
//		_TableAccesses_Diff_txt .setVisible(showDiff);
//		_TableAccesses_Rate_txt .setVisible(showRate);
//
//		_IndexAccesses_Abs_txt  .setVisible(showAbs);
//		_IndexAccesses_Diff_txt .setVisible(showDiff);
//		_IndexAccesses_Rate_txt .setVisible(showRate);
//
//		_TempDbObjects_Abs_txt  .setVisible(showAbs);
//		_TempDbObjects_Diff_txt .setVisible(showDiff);
//		_TempDbObjects_Rate_txt .setVisible(showRate);
//
//		_WorkTables_Abs_txt     .setVisible(showAbs);
//		_WorkTables_Diff_txt    .setVisible(showDiff);
//		_WorkTables_Rate_txt    .setVisible(showRate);
//
//		_ULCFlushes_Abs_txt     .setVisible(showAbs);
//		_ULCFlushes_Diff_txt    .setVisible(showDiff);
//		_ULCFlushes_Rate_txt    .setVisible(showRate);
//
//		_ULCFlushFull_Abs_txt   .setVisible(showAbs);
//		_ULCFlushFull_Diff_txt  .setVisible(showDiff);
//		_ULCFlushFull_Rate_txt  .setVisible(showRate);
//
//		_ULCKBWritten_Abs_txt   .setVisible(showAbs);
//		_ULCKBWritten_Diff_txt  .setVisible(showDiff);
//		_ULCKBWritten_Rate_txt  .setVisible(showRate);
//
//		_PagesRead_Abs_txt      .setVisible(showAbs);
//		_PagesRead_Diff_txt     .setVisible(showDiff);
//		_PagesRead_Rate_txt     .setVisible(showRate);
//
//		_PagesWritten_Abs_txt   .setVisible(showAbs);
//		_PagesWritten_Diff_txt  .setVisible(showDiff);
//		_PagesWritten_Rate_txt  .setVisible(showRate);
//
//		_PhysicalReads_Abs_txt  .setVisible(showAbs);
//		_PhysicalReads_Diff_txt .setVisible(showDiff);
//		_PhysicalReads_Rate_txt .setVisible(showRate);
//
//		_PhysicalWrites_Abs_txt .setVisible(showAbs);
//		_PhysicalWrites_Diff_txt.setVisible(showDiff);
//		_PhysicalWrites_Rate_txt.setVisible(showRate);
//
//		_LogicalReads_Abs_txt   .setVisible(showAbs);
//		_LogicalReads_Diff_txt  .setVisible(showDiff);
//		_LogicalReads_Rate_txt  .setVisible(showRate);
	}

	private TrendGraphDashboardPanel createGraphPanel() 
	{
		return new TrendGraphDashboardPanel();
	}

	@Override
	public TrendGraphDashboardPanel getGraphPanel()
	{
		return _graphPanel;
	}
	
	
	@Override
	public void clearGraph()
	{
		_graphPanel.clearGraph();
	}

	@Override
	public void addTrendGraph(TrendGraph tg)
	{
		_graphPanel.add(tg);
	}

	@Override
	public void setLocalServerName(String name) 
	{ 
		_localServerName_txt.setText(name); 
		_localServerName_txt.setCaretPosition(0);
	}

	public String getLocalServerName() { return _localServerName_txt.getText(); }
//	public String getCountersCleared() { return _countersCleared_txt.getText(); }

	
	
	// implementing: TableModelListener
	@Override
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

//		CountersModel cm = GetCounters.getInstance().getCmByName(CmSummary.CM_NAME);
//		if (cm != null && cm.hasAbsData() )
//			setSummaryData(cm);
		setSummaryData(_cm, false);
	}

	private void refreshClusterInfo()
	{
//		Connection conn = AseTune.getCounterCollector().getMonConnection();
		Connection conn = CounterController.getInstance().getMonConnection();

//		// Cluster View
//		int clusterView = AseConnectionUtils.getClusterEditionSystemView(conn);
//		if (clusterView == AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER)  _clusterView_cbx.setSelectedItem("cluster");
//		if (clusterView == AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE) _clusterView_cbx.setSelectedItem("instance");
//
//		// The other cluster member fields
//		String sql = "select @@clusterboottime, @@clustername, @@instanceid, @@instancename, @@clustercoordid, instance_name(@@clustercoordid)";
//		//sql = "select getdate(), 'CE_DS', 1, 'CE_1_DS', 2, 'CE_2_DS'";
//		try
//		{
//			Statement stmnt = conn.createStatement();
//			ResultSet rs = stmnt.executeQuery(sql);
//			while(rs.next())
//			{
//				_clusterBootTime_txt       .setText( rs.getString(1) );
//				_clusterName_txt           .setText( rs.getString(2) );
//				_clusterInstanceId_txt     .setText( rs.getString(3) );
//				_clusterInstanceName_txt   .setText( rs.getString(4) );
//				_clusterCoordinatorId_txt  .setText( rs.getString(5) );
//				_clusterCoordinatorName_txt.setText( rs.getString(6) );
//			}
//			rs.close();
//			stmnt.close();
//		}
//		catch (SQLException e)
//		{
//			_logger.warn("Problems when executing sql: "+sql, e);
//		}
//		
//		if ( ! "".equals(_clusterName_txt.getText()) )
//			_clusterInfoPanel.setVisible(true);
	}

	private void setFieldAbsDiffRate(CountersModel cm, String name, JLabel label, JTextField absField, JTextField diffField, JTextField rateField)
	{
		if (cm.isDataInitialized())
		{
			Configuration conf = Configuration.getCombinedConfiguration();
			boolean showAbs  = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showAbs,   MainFrame.DEFAULT_summaryOperations_showAbs);
			boolean showDiff = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showDiff,  MainFrame.DEFAULT_summaryOperations_showDiff);
			boolean showRate = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showRate,  MainFrame.DEFAULT_summaryOperations_showRate);

			// Remember origin tooltip
			if ( ! _originToolTip.containsKey(name) )
				_originToolTip.put(name, absField.getToolTipText());

			if (cm.findColumn(name) >= 0)
			{
//System.out.println("setFieldAbsDiffRate(): cm='"+cm.getName()+"', field='"+name+"', ABS="+cm.getAbsString (0, name)+", DIFF="+cm.getDiffString(0, name)+", RATE="+cm.getRateString(0, name)+".");

				absField .setText(cm.getAbsString (0, name));
				diffField.setText(cm.getDiffString(0, name));
				rateField.setText(cm.getRateString(0, name));
	
				label    .setVisible(true && (showAbs || showDiff || showRate));
				absField .setVisible(true && showAbs);
				diffField.setVisible(true && showDiff);
				rateField.setVisible(true && showRate);

				// Check for non configured monitoring has happened
				boolean hasNonConfiguredMonitoringHappened = (getCm().isInitialized() && getCm().hasNonConfiguredMonitoringHappened());

				// Get BG Color from some other field
				Color bgColor = _atAtServerName_txt.getBackground();
				if (hasNonConfiguredMonitoringHappened)
					bgColor = NON_CONFIGURED_MONITORING_COLOR;

				absField .setBackground(bgColor);
				diffField.setBackground(bgColor);
				rateField.setBackground(bgColor);

//System.out.println("setFieldAbsDiffRate(): cm='"+cm.getName()+"', field='"+name+"', hasNonConfiguredMonitoringHappened="+hasNonConfiguredMonitoringHappened);
				// Set Tooltip
				if (hasNonConfiguredMonitoringHappened)
				{
					String tooltip = "<html><font color=\"red\">" + getCm().getNonConfiguredMonitoringMessage(false) + "</font></html>";
					label    .setToolTipText(tooltip);
					absField .setToolTipText(tooltip);
					diffField.setToolTipText(tooltip);
					rateField.setToolTipText(tooltip);
				}
				else
				{
					String tooltip = _originToolTip.get(name);
					label    .setToolTipText(tooltip);
					absField .setToolTipText(tooltip);
					diffField.setToolTipText(tooltip);
					rateField.setToolTipText(tooltip);
				}
			}
			else
			{
				absField  .setText("Not available");
				diffField .setText("");
				rateField .setText("");
	
				label    .setVisible(false);
				absField .setVisible(false);
				diffField.setVisible(false);
				rateField.setVisible(false);
			}
		}
	}

	@Override
	public void setSummaryData(CountersModel cm, boolean postProcessing)
	{
		setWatermark();

		_atAtServerName_txt    .setText(cm.getAbsString (0, "atAtServerName"));
		_srvVersion_txt        .setText(cm.getAbsString (0, "atAtVersion").replaceFirst("Sybase IQ/", ""));  _srvVersion_txt.setCaretPosition(0);
		_srvPageSize_txt       .setText(cm.getAbsString (0, "PageSize"));
		_lastSampleTime_txt    .setText(cm.getAbsString (0, "timeIsNow"));
		_utcTimeDiff_txt       .setText(cm.findColumn("utcTimeDiff") >= 0 ? cm.getAbsString (0, "utcTimeDiff") : "Not available");
		_listeners_txt         .setText(cm.getAbsString (0, "srvListeners"));  _listeners_txt.setCaretPosition(0);

		setFieldAbsDiffRate(cm, "ActiveReq",                   _ActiveReq_lbl,                   _ActiveReq_Abs_txt,                   _ActiveReq_Diff_txt,                   _ActiveReq_Rate_txt);
		setFieldAbsDiffRate(cm, "BytesReceived",               _BytesReceived_lbl,               _BytesReceived_Abs_txt,               _BytesReceived_Diff_txt,               _BytesReceived_Rate_txt);
		setFieldAbsDiffRate(cm, "BytesSent",                   _BytesSent_lbl,                   _BytesSent_Abs_txt,                   _BytesSent_Diff_txt,                   _BytesSent_Rate_txt);
		setFieldAbsDiffRate(cm, "Chkpt",                       _Chkpt_lbl,                       _Chkpt_Abs_txt,                       _Chkpt_Diff_txt,                       _Chkpt_Rate_txt);
		setFieldAbsDiffRate(cm, "CurrentCacheSize_Kb",         _CurrentCacheSize_Kb_lbl,         _CurrentCacheSize_Kb_Abs_txt,         _CurrentCacheSize_Kb_Diff_txt,         _CurrentCacheSize_Kb_Rate_txt);
		setFieldAbsDiffRate(cm, "LockedHeapPages",             _LockedHeapPages_lbl,             _LockedHeapPages_Abs_txt,             _LockedHeapPages_Diff_txt,             _LockedHeapPages_Rate_txt);
		setFieldAbsDiffRate(cm, "MainHeapBytes",               _MainHeapBytes_lbl,               _MainHeapBytes_Abs_txt,               _MainHeapBytes_Diff_txt,               _MainHeapBytes_Rate_txt);
		setFieldAbsDiffRate(cm, "MainHeapPages",               _MainHeapPages_lbl,               _MainHeapPages_Abs_txt,               _MainHeapPages_Diff_txt,               _MainHeapPages_Rate_txt);
		setFieldAbsDiffRate(cm, "MaxCacheSize",                _MaxCacheSize_lbl,                _MaxCacheSize_Abs_txt,                _MaxCacheSize_Diff_txt,                _MaxCacheSize_Rate_txt);
		setFieldAbsDiffRate(cm, "MinCacheSize",                _MinCacheSize_lbl,                _MinCacheSize_Abs_txt,                _MinCacheSize_Diff_txt,                _MinCacheSize_Rate_txt);
		setFieldAbsDiffRate(cm, "MultiPacketsReceived",        _MultiPacketsReceived_lbl,        _MultiPacketsReceived_Abs_txt,        _MultiPacketsReceived_Diff_txt,        _MultiPacketsReceived_Rate_txt);
		setFieldAbsDiffRate(cm, "MultiPacketsSent",            _MultiPacketsSent_lbl,            _MultiPacketsSent_Abs_txt,            _MultiPacketsSent_Diff_txt,            _MultiPacketsSent_Rate_txt);
		setFieldAbsDiffRate(cm, "NumProcessorsAvail",          _NumProcessorsAvail_lbl,          _NumProcessorsAvail_Abs_txt,          _NumProcessorsAvail_Diff_txt,          _NumProcessorsAvail_Rate_txt);
		setFieldAbsDiffRate(cm, "PacketsReceived",             _PacketsReceived_lbl,             _PacketsReceived_Abs_txt,             _PacketsReceived_Diff_txt,             _PacketsReceived_Rate_txt);
		setFieldAbsDiffRate(cm, "PacketsSent",                 _PacketsSent_lbl,                 _PacketsSent_Abs_txt,                 _PacketsSent_Diff_txt,                 _PacketsSent_Rate_txt);
		setFieldAbsDiffRate(cm, "ProcessCPU",                  _ProcessCPU_lbl,                  _ProcessCPU_Abs_txt,                  _ProcessCPU_Diff_txt,                  _ProcessCPU_Rate_txt);
		setFieldAbsDiffRate(cm, "ProcessCPUSystem",            _ProcessCPUSystem_lbl,            _ProcessCPUSystem_Abs_txt,            _ProcessCPUSystem_Diff_txt,            _ProcessCPUSystem_Rate_txt);
		setFieldAbsDiffRate(cm, "ProcessCPUUser",              _ProcessCPUUser_lbl,              _ProcessCPUUser_Abs_txt,              _ProcessCPUUser_Diff_txt,              _ProcessCPUUser_Rate_txt);
		setFieldAbsDiffRate(cm, "threads",                     _threads_lbl,                     _threads_Abs_txt,                     _threads_Diff_txt,                     _threads_Rate_txt);
		setFieldAbsDiffRate(cm, "ConnCount",                   _ConnCount_lbl,                   _ConnCount_Abs_txt,                   _ConnCount_Diff_txt,                   _ConnCount_Rate_txt);
		setFieldAbsDiffRate(cm, "UnschReq",                    _UnschReq_lbl,                    _UnschReq_Abs_txt,                    _UnschReq_Diff_txt,                    _UnschReq_Rate_txt);
		setFieldAbsDiffRate(cm, "NumLogicalProcessorsUsed",    _NumLogicalProcessorsUsed_lbl,    _NumLogicalProcessorsUsed_Abs_txt,    _NumLogicalProcessorsUsed_Diff_txt,    _NumLogicalProcessorsUsed_Rate_txt);
		setFieldAbsDiffRate(cm, "NumPhysicalProcessorsUsed",   _NumPhysicalProcessorsUsed_lbl,   _NumPhysicalProcessorsUsed_Abs_txt,   _NumPhysicalProcessorsUsed_Diff_txt,   _NumPhysicalProcessorsUsed_Rate_txt);
		setFieldAbsDiffRate(cm, "Commit",                      _Commit_lbl,                      _Commit_Abs_txt,                      _Commit_Diff_txt,                      _Commit_Rate_txt);
		setFieldAbsDiffRate(cm, "RequestsReceived",            _RequestsReceived_lbl,            _RequestsReceived_Abs_txt,            _RequestsReceived_Diff_txt,            _RequestsReceived_Rate_txt);


//		_startDate_txt         .setText(cm.getAbsString (0, "StartDate"));
//		_daysRunning_txt       .setText(cm.getAbsString (0, "DaysRunning"));

//		_cpuTime_txt           .setText(cm.getAbsString (0, "ProcessCPUUser"));
//		_cpuUser_txt           .setText(cm.getAbsString (0, "ProcessCPUUser"));
//		_cpuSystem_txt         .setText(cm.getAbsString (0, "ProcessCPUSystem"));
//
//		_packReceived_txt     .setText(cm.getAbsString (0, "PacketsReceived"));
//		_packReceivedDiff_txt .setText(cm.getRateString(0, "PacketsReceived"));
//		_packSent_txt         .setText(cm.getAbsString (0, "PacketsSent"));
//		_packSentDiff_txt     .setText(cm.getRateString(0, "PacketsSent"));
//
//		setFieldAbsDiffRate(cm, "Commit",   _Transactions_lbl,   _Transactions_Abs_txt,   _Transactions_Diff_txt,   _Transactions_Rate_txt);

//		String clusterId       = cm.getAbsString (0, "clusterInstanceId");
//		String currClusterName = _clusterInstanceName_txt.getText();
//		if (clusterId != null && currClusterName.equals(""))
//		{
//			if (MonTablesDictionary.hasInstance() && MonTablesDictionary.getInstance().isClusterEnabled())
//			{
//				refreshClusterInfo();
//			}
//		}
//
////		_localServerName_txt  .setText();
//		_atAtServerName_txt    .setText(cm.getAbsString (0, "atAtServerName"));
//		_listeners_txt         .setText(cm.getAbsString (0, "NetworkAddressInfo"));
//		_listeners_txt         .setCaretPosition(0);
//		_srvVersion_txt        .setText(cm.getAbsString (0, "aseVersion").replaceFirst("Adaptive Server Enterprise/", ""));
//		_srvVersion_txt        .setCaretPosition(0);
//		_srvPageSize_txt       .setText(cm.getAbsString (0, "asePageSize"));
//		_lastSampleTime_txt    .setText(cm.getAbsString (0, "timeIsNow"));
//		_utcTimeDiff_txt       .setText(cm.findColumn("utcTimeDiff") >= 0 ? cm.getAbsString (0, "utcTimeDiff") : "Not available");
//
//		_startDate_txt         .setText(cm.getAbsString (0, "StartDate"));
//		_daysRunning_txt       .setText(cm.getAbsString (0, "DaysRunning"));
//		_countersCleared_txt   .setText(cm.getAbsString (0, "CountersCleared"));
//		_checkPoints_txt       .setText(cm.getAbsString (0, "CheckPoints"));
//		_numDeadlocks_txt      .setText(cm.getAbsString (0, "NumDeadlocks"));
//		_numDeadlocksDiff_txt  .setText(cm.getDiffString(0, "NumDeadlocks"));
//		_diagnosticDumps_txt   .setText(cm.getAbsString (0, "DiagnosticDumps"));
//		_connections_txt       .setText(cm.getAbsString (0, "Connections"));
//		_connectionsDiff_txt   .setText(cm.getDiffString(0, "Connections"));
//		_distinctLoginsAbs_txt .setText(cm.getAbsString (0, "distinctLogins"));
//		_distinctLoginsDiff_txt.setText(cm.getDiffString(0, "distinctLogins"));
//		_lockWaitThreshold_txt .setText(cm.getAbsString (0, "LockWaitThreshold"));
//		_lockWaits_txt         .setText(cm.getAbsString (0, "LockWaits"));
//		_lockWaitsDiff_txt     .setText(cm.getDiffString(0, "LockWaits"));
//		_maxRecovery_txt       .setText(cm.getAbsString (0, "MaxRecovery"));
//
////		if (cm.findColumn("Transactions") >= 0)
////		{
////			_transactions_txt     .setText(cm.getAbsString (0, "Transactions"));
////			_transactionsDiff_txt .setText(cm.getDiffString(0, "Transactions"));
////			_transactionsRate_txt .setText(cm.getRateString(0, "Transactions"));
////		}
////		else
////		{
////			_transactions_txt     .setText("Not available");
////			_transactionsDiff_txt .setText("");
////			_transactionsRate_txt .setText("");
////		}
//		setFieldAbsDiffRate(cm, "Transactions",   _Transactions_lbl,   _Transactions_Abs_txt,   _Transactions_Diff_txt,   _Transactions_Rate_txt);
//		setFieldAbsDiffRate(cm, "Rollbacks",      _Rollbacks_lbl,      _Rollbacks_Abs_txt,      _Rollbacks_Diff_txt,      _Rollbacks_Rate_txt);
//		setFieldAbsDiffRate(cm, "Selects",        _Selects_lbl,        _Selects_Abs_txt,        _Selects_Diff_txt,        _Selects_Rate_txt);
//		setFieldAbsDiffRate(cm, "Updates",        _Updates_lbl,        _Updates_Abs_txt,        _Updates_Diff_txt,        _Updates_Rate_txt);
//		setFieldAbsDiffRate(cm, "Inserts",        _Inserts_lbl,        _Inserts_Abs_txt,        _Inserts_Diff_txt,        _Inserts_Rate_txt);
//		setFieldAbsDiffRate(cm, "Deletes",        _Deletes_lbl,        _Deletes_Abs_txt,        _Deletes_Diff_txt,        _Deletes_Rate_txt);
//		setFieldAbsDiffRate(cm, "Merges",         _Merges_lbl,         _Merges_Abs_txt,         _Merges_Diff_txt,         _Merges_Rate_txt);
//		setFieldAbsDiffRate(cm, "TableAccesses",  _TableAccesses_lbl,  _TableAccesses_Abs_txt,  _TableAccesses_Diff_txt,  _TableAccesses_Rate_txt);
//		setFieldAbsDiffRate(cm, "IndexAccesses",  _IndexAccesses_lbl,  _IndexAccesses_Abs_txt,  _IndexAccesses_Diff_txt,  _IndexAccesses_Rate_txt);
//		setFieldAbsDiffRate(cm, "TempDbObjects",  _TempDbObjects_lbl,  _TempDbObjects_Abs_txt,  _TempDbObjects_Diff_txt,  _TempDbObjects_Rate_txt);
//		setFieldAbsDiffRate(cm, "WorkTables",     _WorkTables_lbl,     _WorkTables_Abs_txt,     _WorkTables_Diff_txt,     _WorkTables_Rate_txt);
//		setFieldAbsDiffRate(cm, "ULCFlushes",     _ULCFlushes_lbl,     _ULCFlushes_Abs_txt,     _ULCFlushes_Diff_txt,     _ULCFlushes_Rate_txt);
//		setFieldAbsDiffRate(cm, "ULCFlushFull",   _ULCFlushFull_lbl,   _ULCFlushFull_Abs_txt,   _ULCFlushFull_Diff_txt,   _ULCFlushFull_Rate_txt);
//		setFieldAbsDiffRate(cm, "ULCKBWritten",   _ULCKBWritten_lbl,   _ULCKBWritten_Abs_txt,   _ULCKBWritten_Diff_txt,   _ULCKBWritten_Rate_txt);
//		setFieldAbsDiffRate(cm, "PagesRead",      _PagesRead_lbl,      _PagesRead_Abs_txt,      _PagesRead_Diff_txt,      _PagesRead_Rate_txt);
//		setFieldAbsDiffRate(cm, "PagesWritten",   _PagesWritten_lbl,   _PagesWritten_Abs_txt,   _PagesWritten_Diff_txt,   _PagesWritten_Rate_txt);
//		setFieldAbsDiffRate(cm, "PhysicalReads",  _PhysicalReads_lbl,  _PhysicalReads_Abs_txt,  _PhysicalReads_Diff_txt,  _PhysicalReads_Rate_txt);
//		setFieldAbsDiffRate(cm, "PhysicalWrites", _PhysicalWrites_lbl, _PhysicalWrites_Abs_txt, _PhysicalWrites_Diff_txt, _PhysicalWrites_Rate_txt);
//		setFieldAbsDiffRate(cm, "LogicalReads",   _LogicalReads_lbl,   _LogicalReads_Abs_txt,   _LogicalReads_Diff_txt,   _LogicalReads_Rate_txt);
//
//		_fullTranslog_txt     .setText(cm.getAbsString (0, "fullTranslogCount"));
//		_oldestOpenTran_txt   .setText(cm.getAbsString (0, "oldestOpenTranInSec"));
//
//		_bootcount_txt        .setText(cm.getAbsString (0, "bootcount"));
//		_recoveryState_txt    .setText(cm.getAbsString (0, "recovery_state"));
////		_cpuBusy_txt          .setText(cm.getDiffString(0, "cpu_busy"));
////		_cpuIo_txt            .setText(cm.getDiffString(0, "cpu_io"));
////		_cpuIdle_txt          .setText(cm.getDiffString(0, "cpu_idle"));
//		_ioTotalRead_txt      .setText(cm.getAbsString (0, "io_total_read"));
//		_ioTotalReadDiff_txt  .setText(cm.getRateString(0, "io_total_read"));
//		_ioTotalWrite_txt     .setText(cm.getAbsString (0, "io_total_write"));
//		_ioTotalWriteDiff_txt .setText(cm.getRateString(0, "io_total_write"));
//		_aaConnectionsAbs_txt .setText(cm.getAbsString (0, "aaConnections"));
//		_aaConnectionsDiff_txt.setText(cm.getDiffString(0, "aaConnections"));
//		_aaConnectionsRate_txt.setText(cm.getRateString(0, "aaConnections"));
//		_packReceived_txt     .setText(cm.getAbsString (0, "pack_received"));
//		_packReceivedDiff_txt .setText(cm.getRateString(0, "pack_received"));
//		_packSent_txt         .setText(cm.getAbsString (0, "pack_sent"));
//		_packSentDiff_txt     .setText(cm.getRateString(0, "pack_sent"));
//		_packetErrors_txt     .setText(cm.getAbsString (0, "packet_errors"));
//		_packetErrorsDiff_txt .setText(cm.getDiffString(0, "packet_errors"));
//		_totalErrors_txt      .setText(cm.getAbsString (0, "total_errors"));
//		_totalErrorsDiff_txt  .setText(cm.getDiffString(0, "total_errors"));
//
//		Double cpuUser        = cm.getDiffValueAsDouble(0, "cpu_busy");
//		Double cpuSystem      = cm.getDiffValueAsDouble(0, "cpu_io");
//		Double cpuIdle        = cm.getDiffValueAsDouble(0, "cpu_idle");
//		if (cpuUser != null && cpuSystem != null && cpuIdle != null)
//		{
//			double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
//			double CPUUser   = cpuUser  .doubleValue();
//			double CPUSystem = cpuSystem.doubleValue();
//			double CPUIdle   = cpuIdle  .doubleValue();
//
//			// rare cases: java.lang.NumberFormatException: Infinite or NaN
//			// at first calculation below:
//			// BigDecimal calcCPUTime = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//			// Just added more debuging information.
//			try
//			{
//				BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//				BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, BigDecimal.ROUND_HALF_EVEN);
//
//				_cpuTime_txt          .setText(calcCPUTime      .toString());
//				_cpuUser_txt          .setText(calcUserCPUTime  .toString());
//				_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
//				_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());
//			}
//			catch (NumberFormatException e)
//			{
//				_cpuTime_txt          .setText("");
//				_cpuUser_txt          .setText("");
//				_cpuSystem_txt        .setText("");
//				_cpuIdle_txt          .setText("");
//				
//				_logger.warn("Problems calculating CPU usage timings in cm '"+cm.getName()+"'. CPUTime="+CPUTime+", CPUUser="+CPUUser+", CPUSystem="+cpuSystem+", CPUIdle="+cpuIdle+". Setting fields to blank. Caught: "+e);
//			}
//		}
//		
//		//----------------------------------------------
//		// counters clear time: background color
//		//----------------------------------------------
//		if (cm.isCountersCleared())
//			_countersCleared_txt.setBackground(COUNTERS_CLEARED_COLOR);
//		else
//			_countersCleared_txt.setBackground(_atAtServerName_txt.getBackground());
//		// end: counters clear time: background color
//
//		//----------------------------------------------
//		// Check LOCK WAITS and, do notification
//		//----------------------------------------------
//		int lockWaits = 0;
//		try { lockWaits = Integer.parseInt(_lockWaits_txt.getText()); }
//		catch (NumberFormatException ignore) {}
//		_logger.debug("LOCK-WAITS="+lockWaits+", TEXT='"+_lockWaits_txt.getText()+"'.");
//		if (lockWaits > 0) // Disabled for the moment
//		{
//			_lockWaits_txt    .setBackground(Color.RED);
//			_lockWaitsDiff_txt.setBackground(Color.RED);
//
//			MainFrame.getInstance().setBlockingLocks(true, lockWaits);
//
//			String toTabName = "Blocking";
//			if ( _focusToBlockingTab == null )
//				_focusToBlockingTab = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Blocking Locks in the ASE Server", cm.getGuiController().getTabbedPane(), toTabName);
//			_focusToBlockingTab.setVisible(true);
//		}
//		else
//		{
//			_lockWaits_txt    .setBackground(_atAtServerName_txt.getBackground());
//			_lockWaitsDiff_txt.setBackground(_atAtServerName_txt.getBackground());
//
//			MainFrame.getInstance().setBlockingLocks(false, 0);
//		}
//		// end: Check LOCK WAITS and, do notification
//
//		//----------------------------------------------
//		// Check FULL LOGS and, do notification
//		//----------------------------------------------
//		int fullLogs = 0;
//		try { fullLogs = Integer.parseInt(_fullTranslog_txt.getText()); }
//		catch (NumberFormatException ignore) {}
//		_logger.debug("FULL-LOG="+lockWaits+", TEXT='"+_fullTranslog_txt.getText()+"'.");
//		if (fullLogs > 0) // Disabled for the moment
//		{
//			_fullTranslog_txt.setBackground(Color.RED);
//
//			MainFrame.getInstance().setFullTransactionLog(true, fullLogs);
//
//			String toTabName = "Databases";
//			if ( _focusToDatabasesTab_fullLog == null )
//				_focusToDatabasesTab_fullLog = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Full Database Transaction Logs in the ASE Server", cm.getGuiController().getTabbedPane(), toTabName);
//			_focusToDatabasesTab_fullLog.setVisible(true);
//		}
//		else
//		{
//			_fullTranslog_txt.setBackground(_atAtServerName_txt.getBackground());
//
//			MainFrame.getInstance().setFullTransactionLog(false, 0);
//		}
//		// end: Check FULL LOGS and, do notification
//
//		//----------------------------------------------
//		// Check OLDEST OPEN TRANSACTION and, do notification
//		//----------------------------------------------
//		int oldestOpenTranInSec = 0;
//		try { oldestOpenTranInSec = Integer.parseInt(_oldestOpenTran_txt.getText()); }
//		catch (NumberFormatException ignore) {}
//		_logger.debug("OLDEST-OPEN-TRANSACTION="+oldestOpenTranInSec+", TEXT='"+_oldestOpenTran_txt.getText()+"'.");
//		if (oldestOpenTranInSec > 0)
//		{
//			_oldestOpenTran_txt.setBackground(Color.RED);
//
//			MainFrame.getInstance().setOldestOpenTran(true, oldestOpenTranInSec);
//
//			String toTabName = "Databases";
//			if ( _focusToDatabasesTab_oldestOpenTran == null )
//				_focusToDatabasesTab_oldestOpenTran = new ChangeToJTabDialog(MainFrame.getInstance(), "Found A 'long' running Transaction in the ASE Server", cm.getGuiController().getTabbedPane(), toTabName);
//			_focusToDatabasesTab_oldestOpenTran.setVisible(true);
//		}
//		else
//		{
//			_oldestOpenTran_txt.setBackground(_atAtServerName_txt.getBackground());
//
//			MainFrame.getInstance().setOldestOpenTran(false, 0);
//		}
//		// end: Check OLDEST OPEN TRANSACTION and, do notification
	}

	@Override
	public void resetGoToTabSettings(String tabName)
	{
//		if (CmBlocking.SHORT_NAME.equals(tabName))
//		{
//			_focusToBlockingTab = null;
//		}
//
//		if (CmOpenDatabases.SHORT_NAME.equals(tabName))
//		{
//			_focusToDatabasesTab_fullLog        = null;
//			_focusToDatabasesTab_oldestOpenTran = null;
//		}
	}
	
	@Override
	public synchronized void clearSummaryData()
	{
		setWatermark();

		// Cluster info
//		_clusterBootTime_txt       .setText("");
//		_clusterName_txt           .setText("");
//		_clusterInstanceName_txt   .setText("");
//		_clusterInstanceId_txt     .setText("");
//		_clusterCoordinatorName_txt.setText("");
//		_clusterCoordinatorId_txt  .setText("");
////		_clusterView_cbx           .setSelectedItem("cluster"); // do not set this, it will kick off the action event and thus: save the info in the properties file.
//		_clusterInfoPanel          .setVisible(false);

		// Server info
		_localServerName_txt    .setText("");

		_atAtServerName_txt     .setText("");
		_listeners_txt          .setText("");
		_srvVersion_txt         .setText("");
		_srvPageSize_txt        .setText("");
		_lastSampleTime_txt     .setText("");
		_utcTimeDiff_txt        .setText("");

//		_startDate_txt          .setText("");
//		_daysRunning_txt        .setText("");
//		_countersCleared_txt    .setText("");
//		_checkPoints_txt        .setText("");
//		_numDeadlocks_txt       .setText("");
//		_numDeadlocksDiff_txt   .setText("");
//		_diagnosticDumps_txt    .setText("");
//		_connections_txt        .setText("");
//		_connectionsDiff_txt    .setText("");
//		_distinctLoginsAbs_txt  .setText("");
//		_distinctLoginsDiff_txt .setText("");
//		_lockWaitThreshold_txt  .setText("");
//		_lockWaits_txt          .setText("");
//		_lockWaitsDiff_txt      .setText("");
//		_maxRecovery_txt        .setText("");
//
//		_Transactions_Abs_txt   .setText("");
//		_Transactions_Diff_txt  .setText("");
//		_Transactions_Rate_txt  .setText("");
//
//		_Rollbacks_Abs_txt      .setText("");
//		_Rollbacks_Diff_txt     .setText("");
//		_Rollbacks_Rate_txt     .setText("");
//
//		_Selects_Abs_txt        .setText("");
//		_Selects_Diff_txt       .setText("");
//		_Selects_Rate_txt       .setText("");
//
//		_Updates_Abs_txt        .setText("");
//		_Updates_Diff_txt       .setText("");
//		_Updates_Rate_txt       .setText("");
//
//		_Inserts_Abs_txt        .setText("");
//		_Inserts_Diff_txt       .setText("");
//		_Inserts_Rate_txt       .setText("");
//
//		_Deletes_Abs_txt        .setText("");
//		_Deletes_Diff_txt       .setText("");
//		_Deletes_Rate_txt       .setText("");
//
//		_Merges_Abs_txt         .setText("");
//		_Merges_Diff_txt        .setText("");
//		_Merges_Rate_txt        .setText("");
//
//		_TableAccesses_Abs_txt  .setText("");
//		_TableAccesses_Diff_txt .setText("");
//		_TableAccesses_Rate_txt .setText("");
//
//		_IndexAccesses_Abs_txt  .setText("");
//		_IndexAccesses_Diff_txt .setText("");
//		_IndexAccesses_Rate_txt .setText("");
//
//		_TempDbObjects_Abs_txt  .setText("");
//		_TempDbObjects_Diff_txt .setText("");
//		_TempDbObjects_Rate_txt .setText("");
//
//		_WorkTables_Abs_txt     .setText("");
//		_WorkTables_Diff_txt    .setText("");
//		_WorkTables_Rate_txt    .setText("");
//
//		_ULCFlushes_Abs_txt     .setText("");
//		_ULCFlushes_Diff_txt    .setText("");
//		_ULCFlushes_Rate_txt    .setText("");
//
//		_ULCFlushFull_Abs_txt   .setText("");
//		_ULCFlushFull_Diff_txt  .setText("");
//		_ULCFlushFull_Rate_txt  .setText("");
//
//		_ULCKBWritten_Abs_txt   .setText("");
//		_ULCKBWritten_Diff_txt  .setText("");
//		_ULCKBWritten_Rate_txt  .setText("");
//
//		_PagesRead_Abs_txt      .setText("");
//		_PagesRead_Diff_txt     .setText("");
//		_PagesRead_Rate_txt     .setText("");
//
//		_PagesWritten_Abs_txt   .setText("");
//		_PagesWritten_Diff_txt  .setText("");
//		_PagesWritten_Rate_txt  .setText("");
//
//		_PhysicalReads_Abs_txt  .setText("");
//		_PhysicalReads_Diff_txt .setText("");
//		_PhysicalReads_Rate_txt .setText("");
//
//		_PhysicalWrites_Abs_txt .setText("");
//		_PhysicalWrites_Diff_txt.setText("");
//		_PhysicalWrites_Rate_txt.setText("");
//
//		_LogicalReads_Abs_txt   .setText("");
//		_LogicalReads_Diff_txt  .setText("");
//		_LogicalReads_Rate_txt  .setText("");
//
//		_fullTranslog_txt       .setText("");
//		_oldestOpenTran_txt     .setText("");
//		
//		_bootcount_txt          .setText("");
//		_recoveryState_txt      .setText("");
//		_cpuTime_txt            .setText("");
//		_cpuUser_txt            .setText("");
//		_cpuSystem_txt          .setText("");
//		_cpuIdle_txt            .setText("");
//		_ioTotalRead_txt        .setText("");
//		_ioTotalReadDiff_txt    .setText("");
//		_ioTotalWrite_txt       .setText("");
//		_ioTotalWriteDiff_txt   .setText("");
//		_aaConnectionsAbs_txt   .setText("");
//		_aaConnectionsDiff_txt  .setText("");
//		_aaConnectionsRate_txt  .setText("");
//		_packReceived_txt       .setText("");
//		_packReceivedDiff_txt   .setText("");
//		_packSent_txt           .setText("");
//		_packSentDiff_txt       .setText("");
//		_packetErrors_txt       .setText("");
//		_packetErrorsDiff_txt   .setText("");
//		_totalErrors_txt        .setText("");
//		_totalErrorsDiff_txt    .setText("");
	}

	@Override
	public void saveLayoutProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		conf.setLayoutProperty("summaryPanel.serverInfo.width",  _dataPanelScroll.getSize().width);
		conf.setLayoutProperty("summaryPanel.serverInfo.height", _dataPanelScroll.getSize().height);

		conf.save();
	}

	private void saveProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

//		// ASE Cluser Edition: system_view
//		if (_clusterView_cbx.getSelectedItem() != null)
//		{
//			String clusterView = _clusterView_cbx.getSelectedItem().toString();
//			conf.setProperty("cluster.system_view", clusterView);
//		}

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

//		// ASE Cluser Edition: system_view
//		String clusterView = conf.getProperty("cluster.system_view", "cluster");
//		_clusterView_cbx.setSelectedItem(clusterView);

		int width   = conf.getLayoutProperty("summaryPanel.serverInfo.width",  SwingUtils.hiDpiScale(300));
		int height  = conf.getLayoutProperty("summaryPanel.serverInfo.height", SwingUtils.hiDpiScale(5000));
		if (width != -1 && height != -1)
		{
			_dataPanelScroll.setPreferredSize(new Dimension(width, height));
		}
	}

	@Override
	public int getClusterView()
	{
//		String choice = (String) _clusterView_cbx.getSelectedItem();
		int systemView = AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;
//		if (choice != null && choice.equalsIgnoreCase("instance"))
//			systemView = AseConnectionUtils.CE_SYSTEM_VIEW_INSTANCE;
//		else
//			systemView = AseConnectionUtils.CE_SYSTEM_VIEW_CLUSTER;

		return systemView;
	}

	@Override
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
//		else if ( ! AseTune.hasCounterCollector() || ! AseTune.getCounterCollector().isMonConnected() )
//		else if ( ! AseTune.hasCounterCollector() || (_cm != null && ! _cm.isConnected()) )
		else if ( ! CounterController.hasInstance() || (_cm != null && ! _cm.isConnected()) )
		{
			setWatermarkText("Not Connected...");
		}
//		else if ( AseTune.hasCounterCollector() && AseTune.getCounterCollector().getMonDisConnectTime() != null )
		else if ( CounterController.hasInstance() && CounterController.getInstance().getMonDisConnectTime() != null )
		{
			String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm").format(CounterController.getInstance().getMonDisConnectTime());
			setWatermarkText("Disconnect at: \n"+dateStr);
		}
		else
		{
			setWatermarkText(null);
		}
	}

	@Override
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
			if (text == null)
				text = "";
			_textBr = text.split("\n");
		}

		private String[]    _textBr = null; // Break Lines by '\n'
		private Graphics2D	g		= null;
		private Rectangle	r		= null;
	
		@Override
		public void paint(Graphics graphics)
		{
			if (_textBr == null || _textBr != null && _textBr.length < 0)
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
//			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 4.0f));
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 4.0f * SwingUtils.getHiDpiScale() ));
			g.setColor(new Color(128, 128, 128, 128));

			FontMetrics fm = g.getFontMetrics();
			int maxStrWidth  = 0;
			int maxStrHeight = fm.getHeight();

			// get max with for all of the lines
			for (int i=0; i<_textBr.length; i++)
			{
				int CurLineStrWidth  = fm.stringWidth(_textBr[i]);
				maxStrWidth = Math.max(maxStrWidth, CurLineStrWidth);
			}
			int sumTextHeight = maxStrHeight * _textBr.length;
			int xPos = (r.width - maxStrWidth) / 2;
			int yPos = (int) (r.height - ((r.height - sumTextHeight) / 2.0f));
			

			g.translate(xPos, yPos);
			double theta = -Math.PI / 6;
			g.rotate(theta);
			g.translate(-xPos, -yPos);
	
			// Print all the lines
			for (int i=0; i<_textBr.length; i++)
			{
				g.drawString(_textBr[i], xPos, (yPos+(maxStrHeight*i)) );
			}
		}
	
		public void setWatermarkText(String text)
		{
			if (text == null)
				text = "";

			_textBr = text.split("\n");
			_logger.debug("setWatermarkText: to '" + text + "'.");

			repaint();
		}
	}

	/*---------------------------------------------------
	 ** BEGIN: implementing: GTabbedPane.ShowProperties
	 **---------------------------------------------------
	 */
	@Override
	public void showProperties()
	{
//		CountersModel cm = GetCounters.getInstance().getCmByName(CmSummary.CM_NAME);
		ShowCmPropertiesDialog dialog = new ShowCmPropertiesDialog(MainFrame.getInstance(), getIcon(), _cm);
		dialog.setVisible(true);
	}
	/*---------------------------------------------------
	 ** END: implementing: GTabbedPane.ShowProperties
	 **---------------------------------------------------
	 */
}
