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
package com.dbxtune.cm.sqlserver.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.sqlserver.CmActiveStatements;
import com.dbxtune.cm.sqlserver.CmSummary;
import com.dbxtune.gui.ChangeToJTabDialog;
import com.dbxtune.gui.ISummaryPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.ResultSetTableModel.TableStringRenderer;
import com.dbxtune.gui.ShowCmPropertiesDialog;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.gui.TrendGraphDashboardPanel;
import com.dbxtune.gui.swing.AbstractComponentDecorator;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.gui.swing.GTextField;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.StringUtil;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class CmSummaryPanel
//extends TabularCntrPanel
extends JPanel
implements ISummaryPanel, TableModelListener, GTabbedPane.ShowProperties
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID      = 1L;

//	private static final String  PROP_PREFIX           = CmSummary.CM_NAME;

	private CountersModel      _cm = null;

	private ChangeToJTabDialog _focusToBlockingTab = null;
	private ChangeToJTabDialog _focusToDatabasesTab_fullLog = null;
	private ChangeToJTabDialog _focusToDatabasesTab_oldestOpenTran = null;
	private Watermark          _watermark;

	private Icon             _icon = null;//SwingUtils.readImageIcon(Version.class, "images/summary_tab.png");

	private JPanel           _serverInfoPanel;
	private JPanel           _dataPanel;
	private JScrollPane      _dataPanelScroll;
	private TrendGraphDashboardPanel _graphPanel;
//	private JScrollPane      _graphPanelScroll;
	
	// Simple Local TextField class that displays the content as the tool tip 
	private static class LocalTextField
	extends JTextField
	{
		private static final long serialVersionUID = 1L;

		@Override
		public String getToolTipText(MouseEvent event)
		{
			String ott = getToolTipText();
			String txt = getText();
			if (StringUtil.isNullOrBlank(txt))
				return ott;
			txt = txt.replace("\n", "<br>");
			return "<html>" + ott + "<hr><pre>" + StringEscapeUtils.escapeHtml4(txt) + "</pre></html>";
		}
	}
	
	private JLabel           _title_lbl                                 = new JLabel();
	private JButton          _trendGraphs_but                           = new JButton();
                                                                        
	// SERVER INFO PANEL                                                
	private JTextField       _localServerName_txt                       = new LocalTextField();
	private JLabel           _localServerName_lbl                       = new JLabel();
	private JTextField       _atAtServerName_txt                        = new JTextField();
	private JLabel           _atAtServerName_lbl                        = new JLabel();
	private JTextField       _listeners_txt                             = new LocalTextField();
	private JLabel           _listeners_lbl                             = new JLabel();
	private JTextField       _onHostName_txt                            = new JTextField();
	private JLabel           _onHostName_lbl                            = new JLabel();
	private JTextField       _srvVersion_txt                            = new LocalTextField();
	private JLabel           _srvVersion_lbl                            = new JLabel();
	private JTextField       _asePageSize_txt                           = new JTextField();
	private JLabel           _asePageSize_lbl                           = new JLabel();
	private JTextField       _lastSampleTime_txt                        = new JTextField();
	private JLabel           _lastSampleTime_lbl                        = new JLabel();
	private JTextField       _utcTimeDiff_txt                           = new JTextField();
	private JLabel           _utcTimeDiff_lbl                           = new JLabel();
                                                                        
	private JTextField       _startDate_txt                             = new JTextField();
	private JLabel           _startDate_lbl                             = new JLabel();
	private JTextField       _daysRunning_txt                           = new JTextField();
	private JLabel           _daysRunning_lbl                           = new JLabel();
                                                                        
	private JTextField       _connectionsDiff_txt                       = new JTextField();
	private JTextField       _connections_txt                           = new JTextField();
	private JLabel           _connections_lbl                           = new JLabel();
	private JTextField       _distinctLoginsDiff_txt                    = new JTextField();
	private JTextField       _distinctLoginsAbs_txt                     = new JTextField();
	private JLabel           _distinctLogins_lbl                        = new JLabel();
	private JTextField       _lockWaitThreshold_txt                     = new JTextField();
	private JLabel           _lockWaitThreshold_lbl                     = new JLabel();
	private JTextField       _lockWaits_txt                             = new JTextField();
	private JTextField       _lockWaitsDiff_txt                         = new JTextField();
	private JLabel           _lockWaits_lbl                             = new JLabel();
	private JTextField       _rootBlockerSpids_txt                      = new JTextField();
	private JLabel           _rootBlockerSpids_lbl                      = new JLabel();
	private JTextField       _deadlockCount_txt                         = new JTextField();
	private JTextField       _deadlockCountDiff_txt                     = new JTextField();
	private JLabel           _deadlockCount_lbl                         = new JLabel();
	private JLabel           _fullTranslog_lbl                          = new JLabel();
	private JTextField       _fullTranslog_txt                          = new JTextField();
	                                                                    
	private JLabel           _tempdbUsageMbAll_lbl                      = new JLabel();
	private JTextField       _tempdbUsageMbAllAbs_txt                   = new JTextField();
	private JTextField       _tempdbUsageMbAllDiff_txt                  = new JTextField();
                                                                        
	private JLabel           _tempdbUsageMbUser_lbl                     = new JLabel();
	private JTextField       _tempdbUsageMbUserAbs_txt                  = new JTextField();
	private JTextField       _tempdbUsageMbUserDiff_txt                 = new JTextField();
                                                                        
	private JLabel           _tempdbUsageMbInternal_lbl                 = new JLabel();
	private JTextField       _tempdbUsageMbInternalAbs_txt              = new JTextField();
	private JTextField       _tempdbUsageMbInternalDiff_txt             = new JTextField();
                                                                        
	private JLabel           _oldestOpenTranBeginTime_lbl               = new JLabel();
	private JTextField       _oldestOpenTranBeginTime_txt               = new JTextField();
	private JLabel           _oldestOpenTranId_lbl                      = new JLabel();
	private JTextField       _oldestOpenTranId_txt                      = new JTextField();
	private JLabel           _oldestOpenTranSpid_lbl                    = new JLabel();
	private JTextField       _oldestOpenTranSpid_txt                    = new JTextField();
	private JLabel           _oldestOpenTranName_lbl                    = new JLabel();
	private JTextField       _oldestOpenTranName_txt                    = new JTextField();
	private JLabel           _oldestOpenTranDbname_lbl                  = new JLabel();
	private JTextField       _oldestOpenTranDbname_txt                  = new JTextField();
	private JLabel           _oldestOpenTranWaitType_lbl                = new JLabel();
	private JTextField       _oldestOpenTranWaitType_txt                = new JTextField();
	private JLabel           _oldestOpenTranCmd_lbl                     = new JLabel();
	private JTextField       _oldestOpenTranCmd_txt                     = new JTextField();
	private JLabel           _oldestOpenTranLoginName_lbl               = new JLabel();
	private JTextField       _oldestOpenTranLoginName_txt               = new JTextField();
	private JLabel           _oldestOpenTranTempdbUsageMb_lbl           = new JLabel();
	private JTextField       _oldestOpenTranTempdbUsageMbAll_txt        = new JTextField();
	private JTextField       _oldestOpenTranTempdbUsageMbUser_txt       = new JTextField();
	private JTextField       _oldestOpenTranTempdbUsageMbInternal_txt   = new JTextField();
	private JLabel           _oldestOpenTranSec_lbl                     = new JLabel();
	private JTextField       _oldestOpenTranSec_txt                     = new JTextField();
	private JLabel           _oldestOpenTranThreshold_lbl               = new JLabel();
	private JTextField       _oldestOpenTranThreshold_txt               = new JTextField();
	private JLabel           _maxSqlExecTimeInSec_lbl                   = new JLabel();
	private JTextField       _maxSqlExecTimeInSec_txt                   = new JTextField();
	private JLabel           _suspectPageCount_lbl                      = new JLabel();
	private GTextField       _suspectPageCount_txt                      = new GTextField();
	private JLabel           _suspectPageErrors_lbl                     = new JLabel();
	private GTextField       _suspectPageErrors_txt                     = new GTextField();

	private JLabel           _osMemMb_lbl                               = new JLabel();
	private JTextField       _osMem_total_os_memory_mb_txt              = new JTextField();
	private JTextField       _osMem_available_os_memory_mb_txt          = new JTextField();
	private JCheckBox        _osMem_system_high_memory_signal_state_chk = new JCheckBox();
	private JCheckBox        _osMem_system_low_memory_signal_state_chk  = new JCheckBox();

	private JLabel           _memXxxServerMb_lbl                        = new JLabel();
	private JTextField       _memTargetServerMb_txt                     = new JTextField();
	private JTextField       _memTotalServerMb_txt                      = new JTextField();
	private JTextField       _memTargetVsTotalMb_txt                    = new JTextField();
                                                                        
	private JLabel           _memYyyServerMb_lbl                        = new JLabel();
	private JTextField       _memUsedByServer_txt                       = new JTextField();
	private JTextField       _memLockedPagesUsedByServer_txt            = new JTextField();
	private JTextField       _memUtilizationPct_txt                     = new JTextField();
	private JCheckBox        _memProcessPhysicalMemoryLow_chk           = new JCheckBox();
	private JCheckBox        _memProcessVirtualMemoryLow_chk            = new JCheckBox();
                                                                        
	private JLabel           _memDatabaseCacheMemoryMb_lbl              = new JLabel();
	private JTextField       _memDatabaseCacheMemoryMb_abs_txt          = new JTextField();
	private JTextField       _memDatabaseCacheMemoryMb_diff_txt         = new JTextField();

	private JLabel           _memGrantedWorkspaceMemoryMb_lbl           = new JLabel();
	private JTextField       _memGrantedWorkspaceMemoryMb_abs_txt       = new JTextField();
	private JTextField       _memGrantedWorkspaceMemoryMb_diff_txt      = new JTextField();

	private JLabel           _memStolenServerMemoryMb_lbl               = new JLabel();
	private JTextField       _memStolenServerMemoryMb_abs_txt           = new JTextField();
	private JTextField       _memStolenServerMemoryMb_diff_txt          = new JTextField();

	// Worker Threads
	private JLabel           _wt_maxWorkers_lbl                         = new JLabel();
	private JTextField       _wt_maxWorkers_abs_txt                     = new JTextField();

	private JLabel           _wt_usedWorkers_lbl                        = new JLabel();
	private JTextField       _wt_usedWorkers_abs_txt                    = new JTextField();
	private JTextField       _wt_usedWorkers_diff_txt                   = new JTextField();

	private JLabel           _wt_availableWorkers_lbl                   = new JLabel();
	private JTextField       _wt_availableWorkers_abs_txt               = new JTextField();
	private JTextField       _wt_availableWorkers_diff_txt              = new JTextField();

	private JLabel           _wt_workersWaitingForCPU_lbl               = new JLabel();
	private JTextField       _wt_workersWaitingForCPU_abs_txt           = new JTextField();
	private JTextField       _wt_workersWaitingForCPU_diff_txt          = new JTextField();

	private JLabel           _wt_requestsWaitingForWorkers_lbl          = new JLabel();
	private JTextField       _wt_requestsWaitingForWorkers_abs_txt      = new JTextField();
	private JTextField       _wt_requestsWaitingForWorkers_diff_txt     = new JTextField();

	private JLabel           _wt_allocatedWorkers_lbl                   = new JLabel();
	private JTextField       _wt_allocatedWorkers_abs_txt               = new JTextField();
	private JTextField       _wt_allocatedWorkers_diff_txt              = new JTextField();

	// CPU... (@@variables)
	private JLabel           _cpuTime_lbl                               = new JLabel();
	private JTextField       _cpuTime_txt                               = new JTextField();
	private JLabel           _cpuUser_lbl                               = new JLabel();
	private JTextField       _cpuUser_txt                               = new JTextField();
	private JLabel           _cpuSystem_lbl                             = new JLabel();
	private JTextField       _cpuSystem_txt                             = new JTextField();
	private JLabel           _cpuIdle_lbl                               = new JLabel();
	private JTextField       _cpuIdle_txt                               = new JTextField();
                                                                        
	private JLabel           _ioTotalRead_lbl                           = new JLabel();
	private JTextField       _ioTotalRead_txt                           = new JTextField();
	private JTextField       _ioTotalReadDiff_txt                       = new JTextField();
	private JLabel           _ioTotalWrite_lbl                          = new JLabel();
	private JTextField       _ioTotalWrite_txt                          = new JTextField();
	private JTextField       _ioTotalWriteDiff_txt                      = new JTextField();
                                                                        
	private JLabel           _aaConnections_lbl                         = new JLabel();
	private JTextField       _aaConnectionsAbs_txt                      = new JTextField();
	private JTextField       _aaConnectionsDiff_txt                     = new JTextField();
	private JTextField       _aaConnectionsRate_txt                     = new JTextField();
                                                                        
	private JLabel           _packReceived_lbl                          = new JLabel();
	private JTextField       _packReceived_txt                          = new JTextField();
	private JTextField       _packReceivedDiff_txt                      = new JTextField();
	private JLabel           _packSent_lbl                              = new JLabel();
	private JTextField       _packSent_txt                              = new JTextField();
	private JTextField       _packSentDiff_txt                          = new JTextField();
	private JLabel           _packetErrors_lbl                          = new JLabel();
	private JTextField       _packetErrors_txt                          = new JTextField();
	private JTextField       _packetErrorsDiff_txt                      = new JTextField();
	private JLabel           _totalErrors_lbl                           = new JLabel();
	private JTextField       _totalErrors_txt                           = new JTextField();
	private JTextField       _totalErrorsDiff_txt                       = new JTextField();

	private JLabel           _sqlAgentStatus_lbl                        = new JLabel();
	private JTextField       _sqlAgentStatus_txt                        = new JTextField();
	
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
	}

	private JPanel createDataPanel() 
	{
		JPanel panel = SwingUtils.createPanel("title", false);
		panel.setLayout(new MigLayout("", "5[grow]5", ""));

		_title_lbl.setFont(new java.awt.Font("Dialog", 1, SwingUtils.hiDpiScale(16)));
		_title_lbl.setText("Summary panel");

		// create new panel
		_serverInfoPanel  = createServerInfoPanel();

		// Fix up the _optionTrendGraphs_but
		TrendGraph.createGraphAccessButton(_trendGraphs_but, CmSummary.CM_NAME);

		panel.add(_title_lbl,           "pushx, growx, left, split");
		panel.add(_trendGraphs_but,     "top, wrap");

		panel.add(_serverInfoPanel,     "growx, width 275lp,             wrap"); //275 logical pixels

//		panel.setMinimumSize(new Dimension(50, 50));

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

		tooltip = "This is the internal server name in the SQL-Server, taken from the global variable @@servername";
		_atAtServerName_lbl   .setText("@@servername");
		_atAtServerName_lbl   .setToolTipText(tooltip);
		_atAtServerName_txt   .setToolTipText(tooltip);
		_atAtServerName_txt   .setEditable(false);

		tooltip = "All ports that the SQL-Server has listener services on.";
		_listeners_lbl        .setText("SRV Port listeners");
		_listeners_lbl        .setToolTipText(tooltip);
		_listeners_txt        .setToolTipText(tooltip);
		_listeners_txt        .setEditable(false);

		tooltip = "Hostname that the SQL-Server server has listener services on, this makes it easier to see what physical machine we have connected to.";
		_onHostName_lbl       .setText("SRV On Hostname");
		_onHostName_lbl       .setToolTipText(tooltip);
		_onHostName_txt       .setToolTipText(tooltip);
		_onHostName_txt       .setEditable(false);

		tooltip = "The version string taken from @@version";
		_srvVersion_lbl       .setText("SRV Version");
		_srvVersion_lbl       .setToolTipText(tooltip);
		_srvVersion_txt       .setToolTipText(tooltip);
		_srvVersion_txt       .setEditable(false);

		tooltip = "The PageSize is taken from @@maxpagesize, which is presented in bytes.";
		_asePageSize_lbl      .setText("SRV Page Size");
		_asePageSize_lbl      .setToolTipText(tooltip);
		_asePageSize_txt      .setToolTipText(tooltip);
		_asePageSize_txt      .setEditable(false);

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


		
		
		tooltip = "Date and time that the SQL-Server was started.";
		_startDate_lbl        .setText("Start date");
		_startDate_lbl        .setToolTipText(tooltip);
		_startDate_txt        .setToolTipText(tooltip);
		_startDate_txt        .setEditable(false);

		tooltip = "Number of days that the SQL-Server has been running for.";
		_daysRunning_lbl      .setText("Days running");
		_daysRunning_lbl      .setToolTipText(tooltip);
		_daysRunning_txt      .setToolTipText(tooltip);
		_daysRunning_txt      .setEditable(false);


		//-----------------------------------------------------------------------------------
		tooltip = "Number of active inbound connections.";
		_connections_lbl      .setText("Connections");
		_connections_lbl      .setToolTipText(tooltip);
		_connections_txt      .setToolTipText(tooltip);
		_connections_txt      .setEditable(false);
		_connectionsDiff_txt  .setEditable(false);
		_connectionsDiff_txt  .setToolTipText("The difference since previous sample.");
		_connectionsDiff_txt.setForeground(Color.BLUE);

		tooltip = "Number of distinct User Names that is logged in to SQL-Server.";
		_distinctLogins_lbl    .setText("Distinct Logins");
		_distinctLogins_lbl    .setToolTipText(tooltip);
		_distinctLoginsAbs_txt .setToolTipText(tooltip);
		_distinctLoginsAbs_txt .setEditable(false);
		_distinctLoginsDiff_txt.setEditable(false);
		_distinctLoginsDiff_txt.setToolTipText("The difference since previous sample.");
		_distinctLoginsDiff_txt.setForeground(Color.BLUE);

		tooltip = "Time (in milliseconds) that processes must have waited for locks in order to be reported.";
		_lockWaitThreshold_lbl.setText("Lock wait threshold");
		_lockWaitThreshold_lbl.setToolTipText(tooltip);
		_lockWaitThreshold_txt.setToolTipText(tooltip);
		_lockWaitThreshold_txt.setEditable(false);

		tooltip = "Number of processes that have waited longer than LockWaitThreshold.";
		_lockWaits_lbl        .setText("Lock waits");
		_lockWaits_lbl        .setToolTipText(tooltip);
		_lockWaits_txt        .setToolTipText(tooltip);
		_lockWaits_txt        .setEditable(false);
		_lockWaitsDiff_txt    .setEditable(false);
		_lockWaitsDiff_txt    .setToolTipText("The difference since previous sample.");
		_lockWaitsDiff_txt.setForeground(Color.BLUE);
		
		tooltip = "SPID's that are 'root blocker'... SPID's that are BLOCKING other SPID's, but at the same time not blocked by any other SPID.";
		_rootBlockerSpids_lbl.setText("Root Blockers");
		_rootBlockerSpids_lbl.setToolTipText(tooltip);
		_rootBlockerSpids_txt.setToolTipText(tooltip);
		_rootBlockerSpids_txt.setEditable(false);

		tooltip = "<html>Number Deadlocks on this instance.<br>"
				+ "If you have a deadlock <i>problem</i>, please run 'sp_blitzLock' from Brent Ozar's package 'First Responder Kit'...<br>"
				+ "<br>"
				+ "To find that package and how to use it; simply Google 'sp_blitzLock'.<br>"
				+ "It will give you <i>details</i> and <i>summary</i> of what tables/SQL/procs that is responsible for the deadlocks.</html>";
		_deadlockCount_lbl        .setText("Deadlocks");
		_deadlockCount_lbl        .setToolTipText(tooltip);
		_deadlockCount_txt        .setToolTipText(tooltip);
		_deadlockCount_txt        .setEditable(false);
		_deadlockCountDiff_txt    .setEditable(false);
		_deadlockCountDiff_txt    .setToolTipText("The difference since previous sample.");
		_deadlockCountDiff_txt.setForeground(Color.BLUE);
		

		//-----------------------------------------------------------------------------------
		tooltip = "Number of databases that has a full transaction log, which probably means suspended SPID's.";
		_fullTranslog_lbl.setText("Full Transaction Logs");
		_fullTranslog_lbl.setToolTipText(tooltip);
		_fullTranslog_txt.setToolTipText(tooltip);
		_fullTranslog_txt.setEditable(false);

		tooltip = "<html>How many MB is used by all SPID's tempdb.<br>LOCAL_TEXT</html>";
		_tempdbUsageMbAll_lbl        .setText("tempdb usage, Mb (all)");
		_tempdbUsageMbAll_lbl        .setToolTipText(tooltip.replace("LOCAL_TEXT", ""));
		_tempdbUsageMbAllAbs_txt     .setToolTipText(tooltip.replace("LOCAL_TEXT", "All tempdb space used (both 'user' and 'internal' objects)."));
		_tempdbUsageMbAllAbs_txt     .setEditable(false);
		_tempdbUsageMbAllDiff_txt    .setToolTipText(tooltip.replace("LOCAL_TEXT", "<b>Diff Calculated</b>. <br>All tempdb space used (both 'user' and 'internal' objects)."));
		_tempdbUsageMbAllDiff_txt    .setEditable(false);
		_tempdbUsageMbAllDiff_txt    .setForeground(Color.BLUE);

		tooltip = "<html>How many MB is used by all SPID's tempdb.<br>LOCAL_TEXT</html>";
		_tempdbUsageMbUser_lbl        .setText("tempdb usage, Mb (usr)");
		_tempdbUsageMbUser_lbl        .setToolTipText(tooltip.replace("LOCAL_TEXT", ""));
		_tempdbUsageMbUserAbs_txt     .setToolTipText(tooltip.replace("LOCAL_TEXT", "User Objects, most possibly temp tables."));
		_tempdbUsageMbUserAbs_txt     .setEditable(false);
		_tempdbUsageMbUserDiff_txt    .setToolTipText(tooltip.replace("LOCAL_TEXT", "<b>Diff Calculated</b>. <br>User Objects, most possibly temp tables."));
		_tempdbUsageMbUserDiff_txt    .setEditable(false);
		_tempdbUsageMbUserDiff_txt    .setForeground(Color.BLUE);

		tooltip = "<html>How many MB is used by all SPID's tempdb.<br>LOCAL_TEXT</html>";
		_tempdbUsageMbInternal_lbl        .setText("tempdb usage, Mb (int)");
		_tempdbUsageMbInternal_lbl        .setToolTipText(tooltip.replace("LOCAL_TEXT", ""));
		_tempdbUsageMbInternalAbs_txt     .setToolTipText(tooltip.replace("LOCAL_TEXT", "All tempdb space used (both 'user' and 'internal' objects)."));
		_tempdbUsageMbInternalAbs_txt     .setEditable(false);
		_tempdbUsageMbInternalDiff_txt    .setToolTipText(tooltip.replace("LOCAL_TEXT", "<b>Diff Calculated</b>. <br>Internal Objects, most possibly work tables etc (implicitly usage by the SQL Server engine)."));
		_tempdbUsageMbInternalDiff_txt    .setEditable(false);
		_tempdbUsageMbInternalDiff_txt    .setForeground(Color.BLUE);

		tooltip = "Oldest Open Transaction Time (empty if no open transactions).";
		_oldestOpenTranBeginTime_lbl.setText("Oldest Open Tran Time");
		_oldestOpenTranBeginTime_lbl.setToolTipText(tooltip);
		_oldestOpenTranBeginTime_txt.setToolTipText(tooltip);
		_oldestOpenTranBeginTime_txt.setEditable(false);

		tooltip = "Oldest Open Transaction ID (empty if no open transactions).";
		_oldestOpenTranId_lbl.setText("Oldest Open Tran ID");
		_oldestOpenTranId_lbl.setToolTipText(tooltip);
		_oldestOpenTranId_txt.setToolTipText(tooltip);
		_oldestOpenTranId_txt.setEditable(false);

		tooltip = "Oldest Open Transaction SPID (empty if no open transactions).";
		_oldestOpenTranSpid_lbl.setText("Oldest Open Tran SPID");
		_oldestOpenTranSpid_lbl.setToolTipText(tooltip);
		_oldestOpenTranSpid_txt.setToolTipText(tooltip);
		_oldestOpenTranSpid_txt.setEditable(false);

		tooltip = "Oldest Open Transaction Name (empty if no open transactions).";
		_oldestOpenTranName_lbl.setText("Oldest Open Tran Name");
		_oldestOpenTranName_lbl.setToolTipText(tooltip);
		_oldestOpenTranName_txt.setToolTipText(tooltip);
		_oldestOpenTranName_txt.setEditable(false);

		tooltip = "Database name which has the oldest open transaction (empty if no open transactions).";
		_oldestOpenTranDbname_lbl.setText("Oldest Open Tran DB");
		_oldestOpenTranDbname_lbl.setToolTipText(tooltip);
		_oldestOpenTranDbname_txt.setToolTipText(tooltip);
		_oldestOpenTranDbname_txt.setEditable(false);

		tooltip = "What the 'Oldest Open Tran SPID' is waiting for (empty if no open transactions).";
		_oldestOpenTranWaitType_lbl.setText("Oldest Open Tran Wait");
		_oldestOpenTranWaitType_lbl.setToolTipText(tooltip);
		_oldestOpenTranWaitType_txt.setToolTipText(tooltip);
		_oldestOpenTranWaitType_txt.setEditable(false);

		tooltip = "What the 'Oldest Open Tran SPID' status/command (empty if no open transactions).";
		_oldestOpenTranCmd_lbl.setText("Oldest Open Tran Cmd");
		_oldestOpenTranCmd_lbl.setToolTipText(tooltip);
		_oldestOpenTranCmd_txt.setToolTipText(tooltip);
		_oldestOpenTranCmd_txt.setEditable(false);

		tooltip = "What the 'Oldest Open Tran SPID' Login Name (empty if no open transactions).";
		_oldestOpenTranLoginName_lbl.setText("Oldest Open Tran Login");
		_oldestOpenTranLoginName_lbl.setToolTipText(tooltip);
		_oldestOpenTranLoginName_txt.setToolTipText(tooltip);
		_oldestOpenTranLoginName_txt.setEditable(false);

		tooltip = "<html>How many MB in tempdb does the 'Oldest Open Tran SPID' hold (empty if no open transactions).<br>LOCAL_TEXT</html>";
		_oldestOpenTranTempdbUsageMb_lbl.setText("Oldest Open Tran TmpMb");
		_oldestOpenTranTempdbUsageMb_lbl.setToolTipText(tooltip.replace("LOCAL_TEXT", ""));
		_oldestOpenTranTempdbUsageMbAll_txt     .setToolTipText(tooltip.replace("LOCAL_TEXT", "All tempdb space used (both 'user' and 'internal' objects)."));
		_oldestOpenTranTempdbUsageMbAll_txt     .setEditable(false);
		_oldestOpenTranTempdbUsageMbUser_txt    .setToolTipText(tooltip.replace("LOCAL_TEXT", "User Objects, most possibly temp tables."));
		_oldestOpenTranTempdbUsageMbUser_txt    .setEditable(false);
		_oldestOpenTranTempdbUsageMbInternal_txt.setToolTipText(tooltip.replace("LOCAL_TEXT", "Internal Objects, most possibly work tables etc (implicity usaged by the SQL Server engine)."));
		_oldestOpenTranTempdbUsageMbInternal_txt.setEditable(false);

		tooltip = "Oldest Open Transaction in any database, presented in seconds.";
		_oldestOpenTranSec_lbl.setText("Oldest Open Tran Sec");
		_oldestOpenTranSec_lbl.setToolTipText(tooltip);
		_oldestOpenTranSec_txt.setToolTipText(tooltip);
		_oldestOpenTranSec_txt.setEditable(false);

		tooltip = "After this amount of seconds, and 'alarm' will be raised...";
		_oldestOpenTranThreshold_lbl.setText("Open Tran Threshold");
		_oldestOpenTranThreshold_lbl.setToolTipText(tooltip);
		_oldestOpenTranThreshold_txt.setToolTipText(tooltip);
		_oldestOpenTranThreshold_txt.setEditable(false);

		tooltip = "Max Active SQL Execution Time In Seconds in this SQL-Server";
		_maxSqlExecTimeInSec_lbl.setText("Max SQL Exec in Sec");
		_maxSqlExecTimeInSec_lbl.setToolTipText(tooltip);
		_maxSqlExecTimeInSec_txt.setToolTipText(tooltip);
		_maxSqlExecTimeInSec_txt.setEditable(false);

		tooltip = "Number of rows in table: msdb.dbo.suspect_pages";  // Note: if you change this, also change in: setSummaryData()
		_suspectPageCount_lbl.setText("Suspect Page Count");
		_suspectPageCount_lbl.setToolTipText(tooltip);
		_suspectPageCount_txt.setToolTipText(tooltip);
		_suspectPageCount_txt.setEditable(false);

		tooltip = "Number of sum(error_count) in table: msdb.dbo.suspect_pages";  // Note: if you change this, also change in : setSummaryData()
		_suspectPageErrors_lbl.setText("Errors");
		_suspectPageErrors_lbl.setToolTipText(tooltip);
		_suspectPageErrors_txt.setToolTipText(tooltip);
		_suspectPageErrors_txt.setEditable(false);
		
		
		
		//-----------------------------------------------------------------------------------

		tooltip = "";
		_osMemMb_lbl                              .setText("OS Memory in MB");
		_osMemMb_lbl                              .setToolTipText("<html>Operating System Memory Information<ul>"
				+ "<li>1: dm_os_sys_memory.total_physical_memory_kb/1024     -- Total size of physical memory available to the operating system </li> "
				+ "<li>2: dm_os_sys_memory.available_physical_memory_kb/1024 -- Size of physical memory available in the operating system </li> "
				+ "<li>4: dm_os_sys_memory.system_high_memory_signal_state   -- State of the system high memory resource notification. A value of 1 indicates the high memory signal has been set by the OS </li> "
				+ "<li>5: dm_os_sys_memory.system_low_memory_signal_state    -- State of the system low memory resource notification. A value of 1 indicates the low memory signal has been set by the OS </li> "
				+ "</ul></html>");

		_osMem_total_os_memory_mb_txt             .setToolTipText("<html>Total size of physical memory available to the operating system (total memory on the machine)</html>");
		_osMem_available_os_memory_mb_txt         .setToolTipText("<html>Size of physical memory free/available in the operating system</html>");
		_osMem_system_high_memory_signal_state_chk.setToolTipText("<html>State of the system high memory resource notification. A value of 1 indicates the high memory signal has been set by the OS</html>");
		_osMem_system_low_memory_signal_state_chk .setToolTipText("<html>State of the system low memory resource notification. A value of 1 indicates the low memory signal has been set by the OS</html>");

		_osMem_total_os_memory_mb_txt             .setEditable(false);
		_osMem_available_os_memory_mb_txt         .setEditable(false);
		_osMem_system_high_memory_signal_state_chk.setEnabled (false);
		_osMem_system_low_memory_signal_state_chk .setEnabled (false);

		_osMem_system_high_memory_signal_state_chk.setBorder(new EmptyBorder(new Insets(1,1,1,1)));
		_osMem_system_low_memory_signal_state_chk .setBorder(new EmptyBorder(new Insets(1,1,1,1)));
		
		
		tooltip = "";
		_memXxxServerMb_lbl.setText("Target & Total Memory");
		_memXxxServerMb_lbl.setToolTipText("<html><ul> "
				+ "<li>1: Target Server Memory MB</li> "
				+ "<li>2: Total Server Memory MB</li> "
				+ "<li>3: Target - Total (not-yet-used) MB</li> "
				+ "</ul></html>");

		_memTargetServerMb_txt .setToolTipText("<html><b>Target Server Memory MB</b> - The amount of memory that SQL Server is willing (potential) to allocate to the SQL Server under its current load.</html>");
		_memTotalServerMb_txt  .setToolTipText("<html><b>Total Server Memory MB</b> - Current amount of memory <b>currently</b> assigned/allocated to SQL Server</html>");
		_memTargetVsTotalMb_txt.setToolTipText("<html><b>Target - Total (not-yet-used) MB</b> - Simply the two field on the left: Target - Total ... Basically how much memory until we reach the MAX Memory</html>");
		
		_memTargetServerMb_txt .setEditable(false);
		_memTotalServerMb_txt  .setEditable(false);
		_memTargetVsTotalMb_txt.setEditable(false);

		
		_memYyyServerMb_lbl.setText("Memory Used");
		_memYyyServerMb_lbl.setToolTipText("<html><ul> "
				+ "<li>1: dm_os_process_memory.physical_memory_in_use_kb/1024  -- the process working set in KB, as reported by operating system, as well as tracked allocations by using large page APIs </li> "
				+ "<li>2: dm_os_process_memory.locked_page_allocations_kb/1024 -- Specifies memory pages locked in memory</li> "
				+ "<li>3: dm_os_process_memory.memory_utilization_percentage   -- Specifies the percentage of committed memory that is in the working set</li> "
				+ "<li>4: dm_os_process_memory.process_physical_memory_low     -- Indicates that the process is responding to low physical memory notification</li> "
				+ "<li>5: dm_os_process_memory.process_virtual_memory_low      -- Indicates that low virtual memory condition has been detected</li> "
				+ "</ul></html>");

		_memUsedByServer_txt            .setToolTipText("<html><b>Memory Used by SQL Server in MB</b>       - dm_os_process_memory.physical_memory_in_use_kb</html>");
		_memLockedPagesUsedByServer_txt .setToolTipText("<html><b>Locked Pages Used by SQL Server in MB</b> - dm_os_process_memory.locked_page_allocations_kb</html>");
		_memUtilizationPct_txt          .setToolTipText("<html><b>Memory Utilization Percentage</b>         - dm_os_process_memory.memory_utilization_percentage</html>");
		_memProcessPhysicalMemoryLow_chk.setToolTipText("<html><b>process_physical_memory_low</b>           - dm_os_process_memory.process_physical_memory_low -- indicates that you are under memory pressure -- Indicates that the process is responding to low physical memory notification</html>");
		_memProcessVirtualMemoryLow_chk .setToolTipText("<html><b>process_virtual_memory_low</b>            - dm_os_process_memory.process_virtual_memory_low -- indicates that you are under memory pressure -- Indicates that low virtual memory condition has been detected</html>");

		_memUsedByServer_txt            .setEditable(false);
		_memLockedPagesUsedByServer_txt .setEditable(false);
		_memUtilizationPct_txt          .setEditable(false);
		_memProcessPhysicalMemoryLow_chk.setEnabled (false);
		_memProcessVirtualMemoryLow_chk .setEnabled (false);
		
		_memProcessPhysicalMemoryLow_chk.setBorder(new EmptyBorder(new Insets(1,1,1,1)));
		_memProcessVirtualMemoryLow_chk .setBorder(new EmptyBorder(new Insets(1,1,1,1)));
		
		
		tooltip = "<html><REPLACE>"
				+ "MB used by the Buffer Pool (Data) Cache to hold data pages in memory. This is hopefully where most of the memory should be...<br>"
				+ "<b>Origin</b>: SELECT cntr_value/1024.0 FROM sys.<b>dm_os_performance_counters</b> WHERE counter_name = '<b>Database Cache Memory (KB)</b>'<br>"
				+ "</html>";
		_memDatabaseCacheMemoryMb_lbl      .setText("Buffer Pool/Cache MB");
		_memDatabaseCacheMemoryMb_lbl      .setToolTipText(tooltip.replace("<REPLACE>", ""));
		_memDatabaseCacheMemoryMb_abs_txt  .setToolTipText(tooltip.replace("<REPLACE>", ""));
		_memDatabaseCacheMemoryMb_abs_txt  .setEditable(false);
		_memDatabaseCacheMemoryMb_diff_txt .setToolTipText(tooltip.replace("<REPLACE>", "DIFF caculate value for: "));
		_memDatabaseCacheMemoryMb_diff_txt .setEditable(false);
		_memDatabaseCacheMemoryMb_diff_txt .setForeground(Color.BLUE);

		tooltip = "<html><REPLACE>"
				+ "MB currently granted to executing processes, such as hash, sort, bulk copy, and index creation operations <i>(just another name for 'memory grants')</i><br>"
				+ "<b>Origin</b>: SELECT cntr_value/1024.0 FROM sys.<b>dm_os_performance_counters</b> WHERE counter_name = '<b>Granted Workspace Memory (KB)</b>'<br>"
				+ "</html>";
		_memGrantedWorkspaceMemoryMb_lbl      .setText("Granted Workspace MB");
		_memGrantedWorkspaceMemoryMb_lbl      .setToolTipText(tooltip.replace("<REPLACE>", ""));
		_memGrantedWorkspaceMemoryMb_abs_txt  .setToolTipText(tooltip.replace("<REPLACE>", ""));
		_memGrantedWorkspaceMemoryMb_abs_txt  .setEditable(false);
		_memGrantedWorkspaceMemoryMb_diff_txt .setToolTipText(tooltip.replace("<REPLACE>", "DIFF caculate value for: "));
		_memGrantedWorkspaceMemoryMb_diff_txt .setEditable(false);
		_memGrantedWorkspaceMemoryMb_diff_txt .setForeground(Color.BLUE);

		tooltip = "<html><REPLACE>"
				+ "MB that are stolen/borrowed from the Buffer Pool/Data Cache to be used by something else... Typically 'Workspace Memory', which is another name for 'memory grants', which really is just 'work memory' for <i>sort</i>, <i>hash tables</i>, etc...)<br>"
				+ "<b>Origin</b>: SELECT cntr_value/1024.0 FROM sys.<b>dm_os_performance_counters</b> WHERE counter_name = '<b>Stolen Server Memory (KB)</b>'<br>"
				+ "</html>";
		_memStolenServerMemoryMb_lbl      .setText("Stolen Server Memory MB");
		_memStolenServerMemoryMb_lbl      .setToolTipText(tooltip.replace("<REPLACE>", ""));
		_memStolenServerMemoryMb_abs_txt  .setToolTipText(tooltip.replace("<REPLACE>", ""));
		_memStolenServerMemoryMb_abs_txt  .setEditable(false);
		_memStolenServerMemoryMb_diff_txt .setToolTipText(tooltip.replace("<REPLACE>", "DIFF caculate value for: "));
		_memStolenServerMemoryMb_diff_txt .setEditable(false);
		_memStolenServerMemoryMb_diff_txt .setForeground(Color.BLUE);


		// Worker Threads
		tooltip = "Max Worker Threads that this SQL Server can use/schedule.";
		_wt_maxWorkers_lbl                     .setText("Max Worker Threads");
		_wt_maxWorkers_lbl                     .setToolTipText(tooltip);
		_wt_maxWorkers_abs_txt                 .setToolTipText(tooltip);
		_wt_maxWorkers_abs_txt                 .setEditable(false);

		tooltip = "Currently Used Worker threads";
		_wt_usedWorkers_lbl                    .setText("Used Workers");
		_wt_usedWorkers_lbl                    .setToolTipText(tooltip);
		_wt_usedWorkers_abs_txt                .setToolTipText(tooltip);
		_wt_usedWorkers_abs_txt                .setEditable(false);
		_wt_usedWorkers_diff_txt               .setEditable(false);
		_wt_usedWorkers_diff_txt               .setToolTipText(tooltip);
		_wt_usedWorkers_diff_txt               .setForeground(Color.BLUE);

		tooltip = "Available Worker Threads. If this is LOW we will be in trouble... If zero then the Server will have serious issues sceduling ANY work.";
		_wt_availableWorkers_lbl               .setText("Available Workers");
		_wt_availableWorkers_lbl               .setToolTipText(tooltip);
		_wt_availableWorkers_abs_txt           .setToolTipText(tooltip);
		_wt_availableWorkers_abs_txt           .setEditable(false);
		_wt_availableWorkers_diff_txt          .setEditable(false);
		_wt_availableWorkers_diff_txt          .setToolTipText(tooltip);
		_wt_availableWorkers_diff_txt          .setForeground(Color.BLUE);

		tooltip = "Number of Workers that wait for CPU resources (waiting the be scheduled, in status 'runnable')";
		_wt_workersWaitingForCPU_lbl           .setText("Workers Waiting for CPU");
//		_wt_workersWaitingForCPU_lbl           .setText("Workers Waits for CPU");
		_wt_workersWaitingForCPU_lbl           .setToolTipText(tooltip);
		_wt_workersWaitingForCPU_abs_txt       .setToolTipText(tooltip);
		_wt_workersWaitingForCPU_abs_txt       .setEditable(false);
		_wt_workersWaitingForCPU_diff_txt      .setEditable(false);
		_wt_workersWaitingForCPU_diff_txt      .setToolTipText(tooltip);
		_wt_workersWaitingForCPU_diff_txt      .setForeground(Color.BLUE);

		tooltip = "Tasks/Requests that are waiting for any worker to be available (Meaning: Thread Pool Starvation... wait_type 'THREADPOOL')";
//		_wt_requestsWaitingForWorkers_lbl      .setText("Requests Waiting For Workers");
		_wt_requestsWaitingForWorkers_lbl      .setText("Tasks Waiting for Worker");
		_wt_requestsWaitingForWorkers_lbl      .setToolTipText(tooltip);
		_wt_requestsWaitingForWorkers_abs_txt  .setToolTipText(tooltip);
		_wt_requestsWaitingForWorkers_abs_txt  .setEditable(false);
		_wt_requestsWaitingForWorkers_diff_txt .setEditable(false);
		_wt_requestsWaitingForWorkers_diff_txt .setToolTipText(tooltip);
		_wt_requestsWaitingForWorkers_diff_txt .setForeground(Color.BLUE);

		tooltip = "How many of the Worker Threads has been allocated and assigned to any scheduler. If this is lower than 'Max Worker Threads', it only means that the server has not yet allocated a OS Thread for that Worker...";
		_wt_allocatedWorkers_lbl               .setText("Allocated Workers");
		_wt_allocatedWorkers_lbl               .setToolTipText(tooltip);
		_wt_allocatedWorkers_abs_txt           .setToolTipText(tooltip);
		_wt_allocatedWorkers_abs_txt           .setEditable(false);
		_wt_allocatedWorkers_diff_txt          .setEditable(false);
		_wt_allocatedWorkers_diff_txt          .setToolTipText(tooltip);
		_wt_allocatedWorkers_diff_txt          .setForeground(Color.BLUE);

		

		// CPU...
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
		_ioTotalReadDiff_txt.setForeground(Color.BLUE);

		tooltip = "Total Write IO's. Global variable @@total_write.";
		_ioTotalWrite_lbl      .setText("IO Write");
		_ioTotalWrite_lbl      .setToolTipText(tooltip);
		_ioTotalWrite_txt      .setToolTipText(tooltip);
		_ioTotalWrite_txt      .setEditable(false);
		_ioTotalWriteDiff_txt  .setEditable(false);
		_ioTotalWriteDiff_txt  .setToolTipText(tooltip);
		_ioTotalWriteDiff_txt.setForeground(Color.BLUE);

		tooltip = "<html>Total Connection that was attemped to make to the SQL-Server Server, even those that failes. Global variable @@connections. (abs, <font color='blue'>diff, <i>rate</i></font>)</html>";
		_aaConnections_lbl     .setText("Connections Tried");
		_aaConnections_lbl     .setToolTipText(tooltip);
		_aaConnectionsAbs_txt  .setEditable(false);
		_aaConnectionsAbs_txt  .setToolTipText(tooltip);
		_aaConnectionsDiff_txt .setEditable(false);
		_aaConnectionsDiff_txt .setToolTipText(tooltip);
		_aaConnectionsRate_txt .setEditable(false);
		_aaConnectionsRate_txt .setToolTipText(tooltip);
		_aaConnectionsDiff_txt.setForeground(Color.BLUE);
		_aaConnectionsRate_txt.setForeground(Color.BLUE);
		_aaConnectionsRate_txt.setFont(RATE_FONT);

		tooltip = "Total Network Packets Received. Global variable @@pack_received.";
		_packReceived_lbl      .setText("NW Packet Received");
		_packReceived_lbl      .setToolTipText(tooltip);
		_packReceived_txt      .setToolTipText(tooltip);
		_packReceived_txt      .setEditable(false);
		_packReceivedDiff_txt  .setEditable(false);
		_packReceivedDiff_txt  .setToolTipText(tooltip);
		_packReceivedDiff_txt.setForeground(Color.BLUE);

		tooltip = "Total Network Packets Sent. Global variable @@pack_sent.";
		_packSent_lbl      .setText("NW Packet Sent");
		_packSent_lbl      .setToolTipText(tooltip);
		_packSent_txt      .setToolTipText(tooltip);
		_packSent_txt      .setEditable(false);
		_packSentDiff_txt  .setEditable(false);
		_packSentDiff_txt  .setToolTipText(tooltip);
		_packSentDiff_txt.setForeground(Color.BLUE);

		tooltip = "Total Network Packets Errors. Global variable @@packet_errors.";
		_packetErrors_lbl      .setText("NW Packet Errors");
		_packetErrors_lbl      .setToolTipText(tooltip);
		_packetErrors_txt      .setToolTipText(tooltip);
		_packetErrors_txt      .setEditable(false);
		_packetErrorsDiff_txt  .setEditable(false);
		_packetErrorsDiff_txt  .setToolTipText(tooltip);
		_packetErrorsDiff_txt.setForeground(Color.BLUE);

		tooltip = "Total Errors. Global variable @@total_errors.";
		_totalErrors_lbl      .setText("Total Errors");
		_totalErrors_lbl      .setToolTipText(tooltip);
		_totalErrors_txt      .setToolTipText(tooltip);
		_totalErrors_txt      .setEditable(false);
		_totalErrorsDiff_txt  .setEditable(false);
		_totalErrorsDiff_txt  .setToolTipText(tooltip);
		_totalErrorsDiff_txt.setForeground(Color.BLUE);

		tooltip = "SQL Agent/Scheduler status (from sys.dm_server_services)";
		_sqlAgentStatus_lbl      .setText("SQL Agent Status");
		_sqlAgentStatus_lbl      .setToolTipText(tooltip);
		_sqlAgentStatus_txt      .setToolTipText(tooltip);
		_sqlAgentStatus_txt      .setEditable(false);

		
		//--------------------------
		// DO the LAYOUT
		//--------------------------
		panel.add(_localServerName_lbl,                       "");
		panel.add(_localServerName_txt,                       "growx, wrap");
		                                                      
		panel.add(_atAtServerName_lbl,                        "");
		panel.add(_atAtServerName_txt,                        "growx, wrap");
		                                                      
		panel.add(_listeners_lbl,                             "");
		panel.add(_listeners_txt,                             "growx, wrap");
		                                                      
		panel.add(_onHostName_lbl,                            "");
		panel.add(_onHostName_txt,                            "growx, wrap");
		                                                      
		panel.add(_srvVersion_lbl,                            "");
		panel.add(_srvVersion_txt,                            "growx, wrap");
		                                                      
		panel.add(_asePageSize_lbl,                           "");
		panel.add(_asePageSize_txt,                           "growx, wrap");
		                                                      
		panel.add(_lastSampleTime_lbl,                        "");
		panel.add(_lastSampleTime_txt,                        "growx, wrap");
		                                                      
		panel.add(_utcTimeDiff_lbl,                           "");
		panel.add(_utcTimeDiff_txt,                           "growx, wrap 20");
		//---------------------------------------------------------------- small space


		panel.add(_startDate_lbl,                             "");
		panel.add(_startDate_txt,                             "growx, wrap");
		                                                      
		panel.add(_daysRunning_lbl,                           "");
		panel.add(_daysRunning_txt,                           "growx, wrap");
		                                                      
                                                              
		panel.add(_connections_lbl,                           "");
		panel.add(_connections_txt,                           "growx, split");
		panel.add(_connectionsDiff_txt,                       "growx, wrap");
		                                                      
		panel.add(_distinctLogins_lbl,                        "");
		panel.add(_distinctLoginsAbs_txt,                     "growx, split");
		panel.add(_distinctLoginsDiff_txt,                    "growx, wrap");
		                                                      
		panel.add(_lockWaitThreshold_lbl,                     "");
		panel.add(_lockWaitThreshold_txt,                     "growx, wrap");
		                                                      
		panel.add(_lockWaits_lbl,                             "");
		panel.add(_lockWaits_txt,                             "growx, split");
		panel.add(_lockWaitsDiff_txt,                         "growx, wrap");
		                                                      
		panel.add(_rootBlockerSpids_lbl,                      "");
		panel.add(_rootBlockerSpids_txt,                      "growx, wrap");
                                                              
		panel.add(_deadlockCount_lbl,                         "");
		panel.add(_deadlockCount_txt,                         "growx, split");
		panel.add(_deadlockCountDiff_txt,                     "growx, wrap");
		                                                      
		panel.add(_fullTranslog_lbl,                          "");
		panel.add(_fullTranslog_txt,                          "growx, wrap");
                                                              
		panel.add(_maxSqlExecTimeInSec_lbl,                   "");
		panel.add(_maxSqlExecTimeInSec_txt,                   "growx, wrap");
                                                              
		panel.add(_suspectPageCount_lbl,                      "");
		panel.add(_suspectPageCount_txt,                      "growx, split");
		panel.add(_suspectPageErrors_lbl,                     "");
		panel.add(_suspectPageErrors_txt,                     "growx, wrap");
                                                              
		panel.add(_tempdbUsageMbAll_lbl,                      "");
		panel.add(_tempdbUsageMbAllAbs_txt,                   "growx, split");
		panel.add(_tempdbUsageMbAllDiff_txt,                  "growx, wrap");
		                                                      
		panel.add(_tempdbUsageMbUser_lbl,                     "");
		panel.add(_tempdbUsageMbUserAbs_txt,                  "growx, split");
		panel.add(_tempdbUsageMbUserDiff_txt,                 "growx, wrap");
		                                                      
		panel.add(_tempdbUsageMbInternal_lbl,                 "");
		panel.add(_tempdbUsageMbInternalAbs_txt,              "growx, split");
		panel.add(_tempdbUsageMbInternalDiff_txt,             "growx, wrap 20");
		//---------------------------------------------------------------- small space
		
		panel.add(_oldestOpenTranBeginTime_lbl,               "");
		panel.add(_oldestOpenTranBeginTime_txt,               "growx, wrap");
                                                              
		panel.add(_oldestOpenTranId_lbl,                      "");
		panel.add(_oldestOpenTranId_txt,                      "growx, wrap");
                                                              
		panel.add(_oldestOpenTranSpid_lbl,                    "");
		panel.add(_oldestOpenTranSpid_txt,                    "growx, wrap");
                                                              
		panel.add(_oldestOpenTranName_lbl,                    "");
		panel.add(_oldestOpenTranName_txt,                    "growx, wrap");
                                                              
		panel.add(_oldestOpenTranDbname_lbl,                  "");
		panel.add(_oldestOpenTranDbname_txt,                  "growx, wrap");
                                                              
		panel.add(_oldestOpenTranWaitType_lbl,                "");
		panel.add(_oldestOpenTranWaitType_txt,                "growx, wrap");
                                                              
		panel.add(_oldestOpenTranCmd_lbl,                     "");
		panel.add(_oldestOpenTranCmd_txt,                     "growx, wrap");
                                                              
		panel.add(_oldestOpenTranLoginName_lbl,               "");
		panel.add(_oldestOpenTranLoginName_txt,               "growx, wrap");

		panel.add(_oldestOpenTranTempdbUsageMb_lbl,           "");
		panel.add(_oldestOpenTranTempdbUsageMbAll_txt,        "growx, gapright 2, split");
		panel.add(_oldestOpenTranTempdbUsageMbUser_txt,       "growx, gapright 2");
		panel.add(_oldestOpenTranTempdbUsageMbInternal_txt,   "growx, wrap");
                                                              
		panel.add(_oldestOpenTranSec_lbl,                     "");
		panel.add(_oldestOpenTranSec_txt,                     "growx, wrap");
                                                              
		panel.add(_oldestOpenTranThreshold_lbl,               "");
		panel.add(_oldestOpenTranThreshold_txt,               "growx, wrap 20");
		//---------------------------------------------------------------- small space

		panel.add(_osMemMb_lbl,                               "");
		panel.add(_osMem_total_os_memory_mb_txt,              "growx, gapright 2, split");
		panel.add(_osMem_available_os_memory_mb_txt,          "growx, gapright 2");
		panel.add(_osMem_system_high_memory_signal_state_chk, "gapright 2");
		panel.add(_osMem_system_low_memory_signal_state_chk,  "gapright 0, wrap");

		panel.add(_memXxxServerMb_lbl,                        "");
		panel.add(_memTargetServerMb_txt,                     "growx, gapright 2, split");
		panel.add(_memTotalServerMb_txt,                      "growx, gapright 2");
		panel.add(_memTargetVsTotalMb_txt,                    "growx, wrap");
                                                              
		panel.add(_memYyyServerMb_lbl,                        "");
		panel.add(_memUsedByServer_txt,                       "growx, gapright 2, split");
		panel.add(_memLockedPagesUsedByServer_txt,            "growx, gapright 2");
		panel.add(_memUtilizationPct_txt,                     "growx, gapright 2");
		panel.add(_memProcessPhysicalMemoryLow_chk,           "gapright 2");
		panel.add(_memProcessVirtualMemoryLow_chk,            "gapright 0, wrap");

		panel.add(_memDatabaseCacheMemoryMb_lbl,              "");
		panel.add(_memDatabaseCacheMemoryMb_abs_txt,          "growx, gapright 2, split");
		panel.add(_memDatabaseCacheMemoryMb_diff_txt,         "growx, gapright 2, wrap");

		panel.add(_memGrantedWorkspaceMemoryMb_lbl,           "");
		panel.add(_memGrantedWorkspaceMemoryMb_abs_txt,       "growx, gapright 2, split");
		panel.add(_memGrantedWorkspaceMemoryMb_diff_txt,      "growx, gapright 2, wrap");

		panel.add(_memStolenServerMemoryMb_lbl,               "");
		panel.add(_memStolenServerMemoryMb_abs_txt,           "growx, gapright 2, split");
		panel.add(_memStolenServerMemoryMb_diff_txt,          "growx, gapright 2, wrap 20");


		// Worker Threads
		panel.add(_wt_maxWorkers_lbl,                         "");
		panel.add(_wt_maxWorkers_abs_txt,                     "growx, wrap");

		panel.add(_wt_usedWorkers_lbl,                        "");
		panel.add(_wt_usedWorkers_abs_txt,                    "growx, gapright 2, split");
		panel.add(_wt_usedWorkers_diff_txt,                   "growx, gapright 2, wrap");

		panel.add(_wt_availableWorkers_lbl,                   "");
		panel.add(_wt_availableWorkers_abs_txt,               "growx, gapright 2, split");
		panel.add(_wt_availableWorkers_diff_txt,              "growx, gapright 2, wrap");

		panel.add(_wt_workersWaitingForCPU_lbl,               "");
		panel.add(_wt_workersWaitingForCPU_abs_txt,           "growx, gapright 2, split");
		panel.add(_wt_workersWaitingForCPU_diff_txt,          "growx, gapright 2, wrap");

		panel.add(_wt_requestsWaitingForWorkers_lbl,          "");
		panel.add(_wt_requestsWaitingForWorkers_abs_txt,      "growx, gapright 2, split");
		panel.add(_wt_requestsWaitingForWorkers_diff_txt,     "growx, gapright 2, wrap");

		panel.add(_wt_allocatedWorkers_lbl,                   "");
		panel.add(_wt_allocatedWorkers_abs_txt,               "growx, gapright 2, split");
		panel.add(_wt_allocatedWorkers_diff_txt,              "growx, gapright 2, wrap 20");


		// CPU...
		panel.add(_cpuTime_lbl,                               "");
		panel.add(_cpuTime_txt,                               "growx, gapright 2, split");
		panel.add(_cpuUser_txt,                               "growx, gapright 2");
		panel.add(_cpuSystem_txt,                             "growx, wrap");
                                                              
		panel.add(_cpuIdle_lbl,                               "");
		panel.add(_cpuIdle_txt,                               "growx, wrap");
		                                                      
		panel.add(_ioTotalRead_lbl,                           "");
		panel.add(_ioTotalRead_txt,                           "growx, split");
		panel.add(_ioTotalReadDiff_txt,                       "growx, wrap");
		                                                      
		panel.add(_ioTotalWrite_lbl,                          "");
		panel.add(_ioTotalWrite_txt,                          "growx, split");
		panel.add(_ioTotalWriteDiff_txt,                      "growx, wrap");
		                                                      
		panel.add(_aaConnections_lbl,                         "");
		panel.add(_aaConnectionsAbs_txt,                      "growx, gapright 2, split");
		panel.add(_aaConnectionsDiff_txt,                     "growx, gapright 2");
		panel.add(_aaConnectionsRate_txt,                     "growx, wrap");
                                                              
		panel.add(_packReceived_lbl,                          "");
		panel.add(_packReceived_txt,                          "growx, split");
		panel.add(_packReceivedDiff_txt,                      "growx, wrap");
		                                                      
		panel.add(_packSent_lbl,                              "");
		panel.add(_packSent_txt,                              "growx, split");
		panel.add(_packSentDiff_txt,                          "growx, wrap");
		                                                      
		panel.add(_packetErrors_lbl,                          "");
		panel.add(_packetErrors_txt,                          "growx, split");
		panel.add(_packetErrorsDiff_txt,                      "growx, wrap");
		                                                      
		panel.add(_totalErrors_lbl,                           "");
		panel.add(_totalErrors_txt,                           "growx, split");
		panel.add(_totalErrorsDiff_txt,                       "growx, wrap 20");
		
		// SQL Services Status
		panel.add(_sqlAgentStatus_lbl,                        "");
		panel.add(_sqlAgentStatus_txt,                        "growx, wrap");

		setComponentProperties();

		return panel;
	}

//	@Override
//	public void setComponentProperties()
//	{
//		// SET initial visibility based of config show ABS/DIFF/RATE
//		Configuration conf = Configuration.getCombinedConfiguration();
//		boolean showAbs  = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showAbs,   MainFrame.DEFAULT_summaryOperations_showAbs);
//		boolean showDiff = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showDiff,  MainFrame.DEFAULT_summaryOperations_showDiff);
//		boolean showRate = conf.getBooleanProperty(MainFrame.PROPKEY_summaryOperations_showRate,  MainFrame.DEFAULT_summaryOperations_showRate);
//
//	}

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
	public String getCountersCleared() { return ""; }

	
	
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

	@Override
	public void setSummaryData(CountersModel cm, boolean postProcessing)
	{
		setWatermark();

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
//		_localServerName_txt  .setText();
		_atAtServerName_txt                     .setText(cm.getAbsString (0, "atAtServerName"));
		_listeners_txt                          .setText(cm.getAbsString (0, "NetworkAddressInfo")); _listeners_txt.setCaretPosition(0);
		_onHostName_txt                         .setText(cm.getAbsString (0, "OnHostName"));
		_srvVersion_txt                         .setText(cm.getAbsString (0, "srvVersion").replaceFirst("Microsoft SQL Server ", "")); _srvVersion_txt.setCaretPosition(0);
		_asePageSize_txt                        .setText(cm.getAbsString (0, "srvPageSize"));
		_lastSampleTime_txt                     .setText(cm.getAbsString (0, "timeIsNow"));
		_utcTimeDiff_txt                        .setText(cm.findColumn("utcTimeDiff") >= 0 ? cm.getAbsString (0, "utcTimeDiff") : "Not available");

		_startDate_txt                          .setText(cm.getAbsString (0, "StartDate"));
		_daysRunning_txt                        .setText(cm.getAbsString (0, "DaysRunning"));

		_connections_txt                        .setText(cm.getAbsString (0, "Connections"));
		_connectionsDiff_txt                    .setText(cm.getDiffString(0, "Connections"));
		_distinctLoginsAbs_txt                  .setText(cm.getAbsString (0, "distinctLogins"));
		_distinctLoginsDiff_txt                 .setText(cm.getDiffString(0, "distinctLogins"));
		_lockWaitThreshold_txt                  .setText(cm.getAbsString (0, "LockWaitThreshold"));
		_lockWaits_txt                          .setText(cm.getAbsString (0, "LockWaits"));
		_lockWaitsDiff_txt                      .setText(cm.getDiffString(0, "LockWaits"));
		_rootBlockerSpids_txt                   .setText(cm.getAbsString (0, "RootBlockerSpids"));  _rootBlockerSpids_txt.setCaretPosition(0);
		_deadlockCount_txt                      .setText(cm.getAbsString (0, "deadlockCount"));
		_deadlockCountDiff_txt                  .setText(cm.getDiffString(0, "deadlockCount"));
		_fullTranslog_txt                       .setText(cm.getAbsString (0, "fullTranslogCount"));
		
		_tempdbUsageMbAllAbs_txt		        .setText(cm.getAbsString (0, "tempdbUsageMbAll"));
		_tempdbUsageMbUserAbs_txt		        .setText(cm.getAbsString (0, "tempdbUsageMbUser"));
		_tempdbUsageMbInternalAbs_txt		    .setText(cm.getAbsString (0, "tempdbUsageMbInternal"));
		_tempdbUsageMbAllDiff_txt		        .setText(cm.getDiffString(0, "tempdbUsageMbAll"));
		_tempdbUsageMbUserDiff_txt		        .setText(cm.getDiffString(0, "tempdbUsageMbUser"));
		_tempdbUsageMbInternalDiff_txt		    .setText(cm.getDiffString(0, "tempdbUsageMbInternal"));
		
		_oldestOpenTranBeginTime_txt            .setText(cm.getAbsString (0, "oldestOpenTranBeginTime"));
		_oldestOpenTranId_txt                   .setText(cm.getAbsString (0, "oldestOpenTranId"));
		_oldestOpenTranSpid_txt                 .setText(cm.getAbsString (0, "oldestOpenTranSpid"));
		_oldestOpenTranName_txt                 .setText(cm.getAbsString (0, "oldestOpenTranName"));
		_oldestOpenTranDbname_txt               .setText(cm.getAbsString (0, "oldestOpenTranDbname"));
		_oldestOpenTranWaitType_txt             .setText(cm.getAbsString (0, "oldestOpenTranWaitType"));
		_oldestOpenTranCmd_txt                  .setText(cm.getAbsString (0, "oldestOpenTranCmd"));
		_oldestOpenTranLoginName_txt            .setText(cm.getAbsString (0, "oldestOpenTranLoginName"));
		_oldestOpenTranTempdbUsageMbAll_txt     .setText(cm.getAbsString (0, "oldestOpenTranTempdbUsageMbAll"));
		_oldestOpenTranTempdbUsageMbUser_txt    .setText(cm.getAbsString (0, "oldestOpenTranTempdbUsageMbUser"));
		_oldestOpenTranTempdbUsageMbInternal_txt.setText(cm.getAbsString (0, "oldestOpenTranTempdbUsageMbInternal"));

		_oldestOpenTranSec_txt                  .setText(cm.getAbsString (0, "oldestOpenTranInSec"));
		_oldestOpenTranThreshold_txt            .setText(cm.getAbsString (0, "oldestOpenTranInSecThreshold"));
		_maxSqlExecTimeInSec_txt                .setText(cm.getAbsString (0, "maxSqlExecTimeInSec"));
		_suspectPageCount_txt                   .setText(cm.getAbsString (0, "suspectPageCount"));
		_suspectPageErrors_txt                  .setText(cm.getAbsString (0, "suspectPageErrors"));

		// Fix some integer "null" values into blanks
		if (_oldestOpenTranId_txt  .getText().trim().equals("0")) _oldestOpenTranId_txt.setText("");
		if (_oldestOpenTranSpid_txt.getText().trim().equals("0")) _oldestOpenTranSpid_txt.setText("");

// print some extra info if current user is "goran"
if (StringUtil.hasValue(_oldestOpenTranId_txt.getText()) && "goran".equals(System.getProperty("user.name")))
{
	System.out.println("");
	System.out.println("#################################################: " + new Timestamp(System.currentTimeMillis()));
	System.out.println("CmSummary: _oldestOpenTranBeginTime             = '" + _oldestOpenTranBeginTime_txt            .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranId                    = '" + _oldestOpenTranId_txt                   .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranSpid                  = '" + _oldestOpenTranSpid_txt                 .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranName                  = '" + _oldestOpenTranName_txt                 .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranDbname                = '" + _oldestOpenTranDbname_txt               .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranWaitType              = '" + _oldestOpenTranWaitType_txt             .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranCmd                   = '" + _oldestOpenTranCmd_txt                  .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranLoginName             = '" + _oldestOpenTranLoginName_txt            .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranTempdbUsageMbAll      = '" + _oldestOpenTranTempdbUsageMbAll_txt     .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranTempdbUsageMbUser     = '" + _oldestOpenTranTempdbUsageMbUser_txt    .getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranTempdbUsageMbInternal = '" + _oldestOpenTranTempdbUsageMbInternal_txt.getText() + "'.");
	System.out.println("CmSummary: _oldestOpenTranSec                   = '" + _oldestOpenTranSec_txt                  .getText() + "'.");
	System.out.println("");
}

		_osMem_total_os_memory_mb_txt             .setText(cm.getAbsString (0, "total_os_memory_mb"));
		_osMem_available_os_memory_mb_txt         .setText(cm.getAbsString (0, "available_physical_memory_mb"));
		_osMem_system_high_memory_signal_state_chk.setSelected("true".equalsIgnoreCase(cm.getAbsString (0, "system_high_memory_signal_state")));
		_osMem_system_low_memory_signal_state_chk .setSelected("true".equalsIgnoreCase(cm.getAbsString (0, "system_low_memory_signal_state")));

		_memTargetServerMb_txt                    .setText(cm.getAbsString (0, "Target_Server_Memory_MB"));
		_memTotalServerMb_txt                     .setText(cm.getAbsString (0, "Total_Server_Memory_MB"));
		_memTargetVsTotalMb_txt                   .setText(cm.getAbsString (0, "TargetVsTotal_diff_MB"));
                                                  
		_memUsedByServer_txt                      .setText(cm.getAbsString (0, "memory_used_by_sqlserver_MB"));
		_memLockedPagesUsedByServer_txt           .setText(cm.getAbsString (0, "locked_pages_used_by_sqlserver_MB"));
		_memUtilizationPct_txt                    .setText(cm.getAbsString (0, "process_memory_utilization_pct"));
		_memProcessPhysicalMemoryLow_chk          .setSelected("true".equalsIgnoreCase(cm.getAbsString (0, "process_physical_memory_low")));
		_memProcessVirtualMemoryLow_chk           .setSelected("true".equalsIgnoreCase(cm.getAbsString (0, "process_virtual_memory_low")));

		_memDatabaseCacheMemoryMb_abs_txt         .setText(cm.getAbsString (0, "databaseCacheMemoryMb"));
		_memDatabaseCacheMemoryMb_diff_txt        .setText(cm.getDiffString(0, "databaseCacheMemoryMb"));
		
		_memGrantedWorkspaceMemoryMb_abs_txt      .setText(cm.getAbsString (0, "grantedWorkspaceMemoryMb"));
		_memGrantedWorkspaceMemoryMb_diff_txt     .setText(cm.getDiffString(0, "grantedWorkspaceMemoryMb"));
		
		_memStolenServerMemoryMb_abs_txt          .setText(cm.getAbsString (0, "stolenServerMemoryMb"));
		_memStolenServerMemoryMb_diff_txt         .setText(cm.getDiffString(0, "stolenServerMemoryMb"));

		// Workers
		_wt_maxWorkers_abs_txt                    .setText(cm.getAbsString (0, "maxWorkers"));

		_wt_usedWorkers_abs_txt                   .setText(cm.getAbsString (0, "usedWorkers"));
		_wt_usedWorkers_diff_txt                  .setText(cm.getDiffString(0, "usedWorkers"));

		_wt_availableWorkers_abs_txt              .setText(cm.getAbsString (0, "availableWorkers"));
		_wt_availableWorkers_diff_txt             .setText(cm.getDiffString(0, "availableWorkers"));

		_wt_workersWaitingForCPU_abs_txt          .setText(cm.getAbsString (0, "workersWaitingForCPU"));
		_wt_workersWaitingForCPU_diff_txt         .setText(cm.getDiffString(0, "workersWaitingForCPU"));

		_wt_requestsWaitingForWorkers_abs_txt     .setText(cm.getAbsString (0, "requestsWaitingForWorkers"));
		_wt_requestsWaitingForWorkers_diff_txt    .setText(cm.getDiffString(0, "requestsWaitingForWorkers"));

		_wt_allocatedWorkers_abs_txt              .setText(cm.getAbsString (0, "allocatedWorkers"));
		_wt_allocatedWorkers_diff_txt             .setText(cm.getDiffString(0, "allocatedWorkers"));


		// CPU...
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

		_sqlAgentStatus_txt   .setText(cm.getAbsString (0, "sql_agent_status")); _sqlAgentStatus_txt.setCaretPosition(0);
		
		Double cpuUser        = cm.getDiffValueAsDouble(0, "cpu_busy");
		Double cpuSystem      = cm.getDiffValueAsDouble(0, "cpu_io");
		Double cpuIdle        = cm.getDiffValueAsDouble(0, "cpu_idle");
		if (cpuUser != null && cpuSystem != null && cpuIdle != null)
		{
			double CPUTime   = cpuUser  .doubleValue() + cpuSystem.doubleValue() + cpuIdle.doubleValue();
			double CPUUser   = cpuUser  .doubleValue();
			double CPUSystem = cpuSystem.doubleValue();
			double CPUIdle   = cpuIdle  .doubleValue();

			// rare cases: java.lang.NumberFormatException: Infinite or NaN
			// at first calculation below:
			// BigDecimal calcCPUTime = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
			// Just added more debuging information.
			try
			{
				BigDecimal calcCPUTime       = new BigDecimal( ((1.0 * (CPUUser + CPUSystem)) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
				BigDecimal calcUserCPUTime   = new BigDecimal( ((1.0 * (CPUUser            )) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
				BigDecimal calcSystemCPUTime = new BigDecimal( ((1.0 * (CPUSystem          )) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);
				BigDecimal calcIdleCPUTime   = new BigDecimal( ((1.0 * (CPUIdle            )) / CPUTime) * 100 ).setScale(1, RoundingMode.HALF_EVEN);

				_cpuTime_txt          .setText(calcCPUTime      .toString());
				_cpuUser_txt          .setText(calcUserCPUTime  .toString());
				_cpuSystem_txt        .setText(calcSystemCPUTime.toString());
				_cpuIdle_txt          .setText(calcIdleCPUTime  .toString());
			}
			catch (NumberFormatException e)
			{
				_cpuTime_txt          .setText("");
				_cpuUser_txt          .setText("");
				_cpuSystem_txt        .setText("");
				_cpuIdle_txt          .setText("");
				
				_logger.warn("Problems calculating CPU usage timings in cm '"+cm.getName()+"'. CPUTime="+CPUTime+", CPUUser="+CPUUser+", CPUSystem="+cpuSystem+", CPUIdle="+cpuIdle+". Setting fields to blank. Caught: "+e);
			}
		}
		
//		//----------------------------------------------
//		// counters clear time: background color
//		//----------------------------------------------
//		if (cm.isCountersCleared())
//			_countersCleared_txt.setBackground(COUNTERS_CLEARED_COLOR);
//		else
//			_countersCleared_txt.setBackground(_atAtServerName_txt.getBackground());
//		// end: counters clear time: background color
//
		//----------------------------------------------
		// Check SUSPECT PAGES and, do notification
		//----------------------------------------------
		Integer suspectPageCount = cm.getAbsValueAsInteger(0, "suspectPageCount", false, -1);
		if (suspectPageCount > 0)
		{
			ResultSetTableModel rstm = (cm instanceof CmSummary) ? ((CmSummary)cm).get_lastSuspectPage_rstm() : null;
			
			if (rstm != null)
			{
//				String errTooltip = "<html>" + rstm.toHtmlTableString("sortable") + "</html>";
				String errTooltip = rstm.toHtmlTableString("sortable", true, true, null, new TableStringRenderer()
				{
					@Override
					public String tagTableAttr(ResultSetTableModel rstm)
					{
						// TODO Auto-generated method stub
						return "border='1'";
					}
					@Override
					public String cellValue(ResultSetTableModel rstm, int row, int col, String colName, Object objVal, String strVal)
					{
						if ("object_name".equalsIgnoreCase(colName))
							return "<b>" + strVal + "</b>";
						return strVal;
					}
				});
				errTooltip = "<html>" + errTooltip + "</html>";

				_suspectPageCount_txt .setToolTipText(errTooltip);
				_suspectPageErrors_txt.setToolTipText(errTooltip);
			}

			_suspectPageCount_txt .setBackground(Color.RED);
			_suspectPageErrors_txt.setBackground(Color.RED);
		}
		else
		{
			_suspectPageCount_txt .setToolTipText("Number of rows in table: msdb.dbo.suspect_pages");
			_suspectPageErrors_txt.setToolTipText("Number of sum(error_count) in table: msdb.dbo.suspect_pages");

			_suspectPageCount_txt .setBackground(_atAtServerName_txt.getBackground());
			_suspectPageErrors_txt.setBackground(_atAtServerName_txt.getBackground());
		}

		
		
		//----------------------------------------------
		// Check LOCK WAITS and, do notification
		//----------------------------------------------
		int lockWaits          = StringUtil.parseInt(_lockWaits_txt.getText(), 0);
//		int lockWaitsThreshold = StringUtil.parseInt(_lockWaitThreshold_txt.getText(), 0);
		_logger.debug("LOCK-WAITS="+lockWaits+", TEXT='"+_lockWaits_txt.getText()+"'.");
		if (lockWaits > 0)
		{
			_lockWaits_txt       .setBackground(Color.RED);
			_lockWaitsDiff_txt   .setBackground(Color.RED);
			_rootBlockerSpids_txt.setBackground(Color.RED);

			boolean isVisibleInPrevSample = MainFrame.getInstance().hasBlockingLocks();
			MainFrame.getInstance().setBlockingLocks(true, lockWaits);

			String toTabName = "Active Statements";
			if ( _focusToBlockingTab == null )
				_focusToBlockingTab = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Blocking Locks in the SQL-Server", cm.getGuiController().getTabbedPane(), toTabName);

			if ( ! isVisibleInPrevSample )
				_focusToBlockingTab.setVisible(true);
		}
		else
		{
			_lockWaits_txt       .setBackground(_atAtServerName_txt.getBackground());
			_lockWaitsDiff_txt   .setBackground(_atAtServerName_txt.getBackground());
			_rootBlockerSpids_txt.setBackground(_atAtServerName_txt.getBackground());

			MainFrame.getInstance().setBlockingLocks(false, 0);
		}
		// end: Check LOCK WAITS and, do notification

		
		
		//----------------------------------------------
		// Check DEADLOCK and, do notification
		//----------------------------------------------
		int deadlockCount = StringUtil.parseInt(_deadlockCountDiff_txt.getText(), 0);
		_logger.debug("DEADLOCK-COUNT-DIFF="+deadlockCount+", TEXT='"+_deadlockCountDiff_txt.getText()+"'.");
		if (deadlockCount > 0)
		{
			_deadlockCount_txt       .setBackground(Color.RED);
			_deadlockCountDiff_txt   .setBackground(Color.RED);

//			boolean isVisibleInPrevSample = MainFrame.getInstance().hasDeadlocks();
//			MainFrame.getInstance().setDeadlocks(true, deadlockCount);

//			String toTabName = "Active Statements";
//			if ( _focusToBlockingTab == null )
//				_focusToBlockingTab = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Blocking Locks in the SQL-Server", cm.getGuiController().getTabbedPane(), toTabName);
//
//			if ( ! isVisibleInPrevSample )
//				_focusToBlockingTab.setVisible(true);
		}
		else
		{
			_deadlockCount_txt       .setBackground(_atAtServerName_txt.getBackground());
			_deadlockCountDiff_txt   .setBackground(_atAtServerName_txt.getBackground());

//			MainFrame.getInstance().setDeadlocks(false, 0);
		}
		// end: Check DEADLOCKS and, do notification


		
		//----------------------------------------------
		// Check FULL LOGS and, do notification
		//----------------------------------------------
		int fullLogs = 0;
		try { fullLogs = Integer.parseInt(_fullTranslog_txt.getText()); }
		catch (NumberFormatException ignore) {}
		_logger.debug("FULL-LOG="+fullLogs+", TEXT='"+_fullTranslog_txt.getText()+"'.");
		if (fullLogs > 0)
		{
			_fullTranslog_txt.setBackground(Color.RED);

			boolean isVisibleInPrevSample = MainFrame.getInstance().hasFullTransactionLog();
			MainFrame.getInstance().setFullTransactionLog(true, fullLogs);

			String toTabName = "Databases";
			if ( _focusToDatabasesTab_fullLog == null )
				_focusToDatabasesTab_fullLog = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Full Database Transaction Logs in the SQL-Server", cm.getGuiController().getTabbedPane(), toTabName);
			
			if ( ! isVisibleInPrevSample )
				_focusToDatabasesTab_fullLog.setVisible(true);
		}
		else
		{
			_fullTranslog_txt.setBackground(_atAtServerName_txt.getBackground());

			MainFrame.getInstance().setFullTransactionLog(false, 0);
		}
		// end: Check FULL LOGS and, do notification

		//----------------------------------------------
		// Check OLDEST OPEN TRANSACTION and, do notification
		//----------------------------------------------
		int oldestOpenTranInSec          = StringUtil.parseInt(_oldestOpenTranSec_txt.getText(), 0);
		int oldestOpenTranInSecThreshold = StringUtil.parseInt(_oldestOpenTranThreshold_txt.getText(), 0);
		_logger.debug("OLDEST-OPEN-TRANSACTION="+oldestOpenTranInSec+", TEXT='"+_oldestOpenTranSec_txt.getText()+"'.");
		if (oldestOpenTranInSec > oldestOpenTranInSecThreshold)
		{
			_oldestOpenTranSec_txt.setBackground(Color.RED);

			boolean isVisibleInPrevSample = MainFrame.getInstance().hasOldestOpenTran();
			MainFrame.getInstance().setOldestOpenTran(true, oldestOpenTranInSec);

			String toTabName = "Databases";
			if ( _focusToDatabasesTab_oldestOpenTran == null )
				_focusToDatabasesTab_oldestOpenTran = new ChangeToJTabDialog(MainFrame.getInstance(), "Found A 'long' running Transaction in the SQL-Server", cm.getGuiController().getTabbedPane(), toTabName);
			
			if ( ! isVisibleInPrevSample )
				_focusToDatabasesTab_oldestOpenTran.setVisible(true);
		}
		else
		{
			_oldestOpenTranSec_txt.setBackground(_atAtServerName_txt.getBackground());

			MainFrame.getInstance().setOldestOpenTran(false, 0);
		}
		// end: Check OLDEST OPEN TRANSACTION and, do notification
	}

	@Override
	public void resetGoToTabSettings(String tabName)
	{
		if (CmActiveStatements.SHORT_NAME.equals(tabName))
		{
			_focusToBlockingTab = null;
		}

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

		// Server info
		_localServerName_txt                      .setText("");
                                                  
		_atAtServerName_txt                       .setText("");
		_listeners_txt                            .setText("");
		_onHostName_txt                           .setText("");
		_srvVersion_txt                           .setText("");
		_asePageSize_txt                          .setText("");
		_lastSampleTime_txt                       .setText("");
		_utcTimeDiff_txt                          .setText("");
                                                  
		_startDate_txt                            .setText("");
		_daysRunning_txt                          .setText("");
		_connections_txt                          .setText("");
		_connectionsDiff_txt                      .setText("");
		_distinctLoginsAbs_txt                    .setText("");
		_distinctLoginsDiff_txt                   .setText("");
		_lockWaitThreshold_txt                    .setText("");
		_lockWaits_txt                            .setText("");
		_lockWaitsDiff_txt                        .setText("");
		_rootBlockerSpids_txt                     .setText("");
		_deadlockCount_txt                        .setText("");
		_deadlockCountDiff_txt                    .setText("");
		_fullTranslog_txt                         .setText("");
                                                  
		_tempdbUsageMbAllAbs_txt                  .setText("");
		_tempdbUsageMbUserAbs_txt                 .setText("");
		_tempdbUsageMbInternalAbs_txt             .setText("");
		_tempdbUsageMbAllDiff_txt                 .setText("");
		_tempdbUsageMbUserDiff_txt                .setText("");
		_tempdbUsageMbInternalDiff_txt            .setText("");
		                                          
		_oldestOpenTranBeginTime_txt              .setText("");
		_oldestOpenTranId_txt                     .setText("");
		_oldestOpenTranSpid_txt                   .setText("");
		_oldestOpenTranName_txt                   .setText("");
		_oldestOpenTranDbname_txt                 .setText("");
		_oldestOpenTranWaitType_txt               .setText("");
		_oldestOpenTranCmd_txt                    .setText("");
		_oldestOpenTranLoginName_txt              .setText("");
		_oldestOpenTranTempdbUsageMbAll_txt       .setText("");
		_oldestOpenTranTempdbUsageMbUser_txt      .setText("");
		_oldestOpenTranTempdbUsageMbInternal_txt  .setText("");
                                                  
		_oldestOpenTranSec_txt                    .setText("");
		_oldestOpenTranThreshold_txt              .setText("");
		_maxSqlExecTimeInSec_txt                  .setText("");
		_suspectPageCount_txt                     .setText(""); _suspectPageCount_txt .setBackground(_atAtServerName_txt.getBackground());
		_suspectPageErrors_txt                    .setText(""); _suspectPageErrors_txt.setBackground(_atAtServerName_txt.getBackground());
		                                        
		_osMem_total_os_memory_mb_txt             .setText("");
		_osMem_available_os_memory_mb_txt         .setText("");
		_osMem_system_high_memory_signal_state_chk.setSelected(false);
		_osMem_system_low_memory_signal_state_chk .setSelected(false);

		_memTargetServerMb_txt                    .setText("");
		_memTotalServerMb_txt                     .setText("");
		_memTargetVsTotalMb_txt                   .setText("");
		_memUsedByServer_txt                      .setText("");
		_memLockedPagesUsedByServer_txt           .setText("");
		_memUtilizationPct_txt                    .setText("");
		_memProcessPhysicalMemoryLow_chk          .setSelected(false);
		_memProcessVirtualMemoryLow_chk           .setSelected(false);

		_memDatabaseCacheMemoryMb_abs_txt         .setText("");
		_memDatabaseCacheMemoryMb_diff_txt        .setText("");
		_memGrantedWorkspaceMemoryMb_abs_txt      .setText("");
		_memGrantedWorkspaceMemoryMb_diff_txt     .setText("");
		_memStolenServerMemoryMb_abs_txt          .setText("");
		_memStolenServerMemoryMb_diff_txt         .setText("");

		// Worker Threads
		_wt_maxWorkers_abs_txt                    .setText("");
		_wt_usedWorkers_abs_txt                   .setText("");
		_wt_usedWorkers_diff_txt                  .setText("");
		_wt_availableWorkers_abs_txt              .setText("");
		_wt_availableWorkers_diff_txt             .setText("");
		_wt_workersWaitingForCPU_abs_txt          .setText("");
		_wt_workersWaitingForCPU_diff_txt         .setText("");
		_wt_requestsWaitingForWorkers_abs_txt     .setText("");
		_wt_requestsWaitingForWorkers_diff_txt    .setText("");
		_wt_allocatedWorkers_abs_txt              .setText("");
		_wt_allocatedWorkers_diff_txt             .setText("");
		
		// CPU...
		_cpuTime_txt                              .setText("");
		_cpuUser_txt                              .setText("");
		_cpuSystem_txt                            .setText("");
		_cpuIdle_txt                              .setText("");
		_ioTotalRead_txt                          .setText("");
		_ioTotalReadDiff_txt                      .setText("");
		_ioTotalWrite_txt                         .setText("");
		_ioTotalWriteDiff_txt                     .setText("");
		_aaConnectionsAbs_txt                     .setText("");
		_aaConnectionsDiff_txt                    .setText("");
		_aaConnectionsRate_txt                    .setText("");
		_packReceived_txt                         .setText("");
		_packReceivedDiff_txt                     .setText("");
		_packSent_txt                             .setText("");
		_packSentDiff_txt                         .setText("");
		_packetErrors_txt                         .setText("");
		_packetErrorsDiff_txt                     .setText("");
		_totalErrors_txt                          .setText("");
		_totalErrorsDiff_txt                      .setText("");

		_sqlAgentStatus_txt                       .setText("");
	}

	@Override
	public void saveLayoutProps()
	{
		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);

		conf.setLayoutProperty("summaryPanel.serverInfo.width",  _dataPanelScroll.getSize().width);
		conf.setLayoutProperty("summaryPanel.serverInfo.height", _dataPanelScroll.getSize().height);

		conf.save();
	}

	private void loadProps()
	{
		Configuration conf = Configuration.getCombinedConfiguration();

		int width   = conf.getLayoutProperty("summaryPanel.serverInfo.width",  SwingUtils.hiDpiScale(300));
		int height  = conf.getLayoutProperty("summaryPanel.serverInfo.height", SwingUtils.hiDpiScale(5000));
		if (width != -1 && height != -1)
		{
			_dataPanelScroll.setPreferredSize(new Dimension(width, height));
		}
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
		else if ( ! CounterController.hasInstance() || (_cm != null && ! _cm.isConnected()) )
		{
			setWatermarkText("Not Connected...");
		}
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
//			if (_textBr == null || _textBr != null && _textBr.length < 0)
			if (_textBr == null || _textBr != null && _textBr.length == 0)
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

	@Override
	public int getClusterView()
	{
		return AseConnectionUtils.CE_SYSTEM_VIEW_UNKNOWN;
	}

	@Override
	public void setComponentProperties()
	{
	}
}
