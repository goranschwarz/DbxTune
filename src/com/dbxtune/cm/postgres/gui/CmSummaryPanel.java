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
package com.dbxtune.cm.postgres.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
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
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.postgres.CmSummary;
import com.dbxtune.gui.ChangeToJTabDialog;
import com.dbxtune.gui.ISummaryPanel;
import com.dbxtune.gui.MainFrame;
import com.dbxtune.gui.ShowCmPropertiesDialog;
import com.dbxtune.gui.TrendGraph;
import com.dbxtune.gui.TrendGraphDashboardPanel;
import com.dbxtune.gui.swing.AbstractComponentDecorator;
import com.dbxtune.gui.swing.GTabbedPane;
import com.dbxtune.utils.AseConnectionUtils;
import com.dbxtune.utils.Configuration;
import com.dbxtune.utils.SwingUtils;

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

	private ChangeToJTabDialog _focusToBlockingTab = null;
	private ChangeToJTabDialog _focusToActiveStmntsTab_oldestOpenTran = null;

	private Watermark          _watermark;

	private Icon             _icon = null;//SwingUtils.readImageIcon(Version.class, "images/summary_tab.png");

	private JPanel           _serverInfoPanel;
	private JPanel           _dataPanel;
	private JScrollPane      _dataPanelScroll;
	private TrendGraphDashboardPanel _graphPanel;
//	private JScrollPane      _graphPanelScroll;
	
	private JLabel           _title_lbl                    = new JLabel();
	private JButton          _trendGraphs_but              = new JButton();

	// SERVER INFO PANEL
	private JTextField       _localServerName_txt          = new JTextField();
	private JLabel           _localServerName_lbl          = new JLabel();

	private JLabel           _dbmsServerName_lbl           = new JLabel();
	private JTextField       _dbmsServerName_txt           = new JTextField();

	private JLabel           _dbmsListeners_lbl            = new JLabel();
	private JTextField       _dbmsListeners_txt            = new JTextField();

	private JLabel           _dbmsVersion_lbl              = new JLabel();
	private JTextField       _dbmsVersion_txt              = new JTextField();

	private JLabel           _lastSampleTime_lbl           = new JLabel();
	private JTextField       _lastSampleTime_txt           = new JTextField();

	private JLabel           _utcTimeDiff_lbl              = new JLabel();
	private JTextField       _utcTimeDiff_txt              = new JTextField();

	private JTextField       _startDate_txt                = new JTextField();
	private JLabel           _startDate_lbl                = new JLabel();

	private JTextField       _inRecovery_txt               = new JTextField();
	private JLabel           _inRecovery_lbl               = new JLabel();


	// Blocking
	private JTextField       _blockingLockCount_txt        = new JTextField();
	private JLabel           _blockingLockCount_lbl        = new JLabel();
	
	private JTextField       _blockingLockWaitStart_txt    = new JTextField();
	private JLabel           _blockingLockWaitStart_lbl    = new JLabel();

	private JTextField       _blockingLockWaitInSec_txt    = new JTextField();
	private JLabel           _blockingLockWaitInSec_lbl    = new JLabel();


//	// Oldest Transaction and Statement
//	private JTextField       _oldestXactStartInSec_txt     = new JTextField();
//	private JLabel           _oldestXactStartInSec_lbl     = new JLabel();
//	
//	private JTextField       _oldestStmntStartInSec_txt    = new JTextField();
//	private JLabel           _oldestStmntStartInSec_lbl    = new JLabel();
	

	// XID Age... backend_xmin
	private JTextField       _oldestBackendXminAge_txt     = new JTextField();
	private JLabel           _oldestBackendXminAge_lbl     = new JLabel();
	
	private JTextField       _oldestPreparedXactAge_txt    = new JTextField();
	private JLabel           _oldestPreparedXactAge_lbl    = new JLabel();
	
	private JTextField       _oldestReplicationSlotAge_txt = new JTextField();
	private JLabel           _oldestReplicationSlotAge_lbl = new JLabel();
	
	private JTextField       _oldestReplicaXactAge_txt     = new JTextField();
	private JLabel           _oldestReplicaXactAge_lbl     = new JLabel();
	

	// Oldest Transaction and Statement
	private JLabel           _oXactInfo_lbl                = new JLabel();
                                                           
	private JLabel           _oXactState_lbl               = new JLabel();
	private JTextField       _oXactState_txt               = new JTextField();

	private JLabel           _oXactPid_lbl                 = new JLabel();
	private JTextField       _oXactPid_txt                 = new JTextField();

	private JLabel           _oXactDbname_lbl              = new JLabel();
	private JTextField       _oXactDbname_txt              = new JTextField();

	private JLabel           _oXactUsername_lbl            = new JLabel();
	private JTextField       _oXactUsername_txt            = new JTextField();

	private JLabel           _oXactAppname_lbl             = new JLabel();
	private JTextField       _oXactAppname_txt             = new JTextField();

	private JLabel           _oXactIsWaiting_lbl           = new JLabel();
	private JCheckBox        _oXactIsWaiting_chk           = new JCheckBox();
	private JTextField       _oXactIsWaiting1_txt          = new JTextField();
	private JTextField       _oXactIsWaiting2_txt          = new JTextField();

	private JLabel           _oXactStartInSec_lbl          = new JLabel();
	private JTextField       _oXactStartInSec_txt          = new JTextField();
	private JTextField       _oXactStartAge_txt            = new JTextField();

	private JLabel           _oStmntStartInSec_lbl         = new JLabel();
	private JTextField       _oStmntStartInSec_txt         = new JTextField();
	private JTextField       _oStmntStartAge_txt           = new JTextField();

	private JLabel           _oStmntExecInSec_lbl          = new JLabel();
	private JTextField       _oStmntExecInSec_txt          = new JTextField();
	private JTextField       _oStmntExecAge_txt            = new JTextField();

	private JLabel           _oInCurrentStateInSec_lbl     = new JLabel();
	private JTextField       _oInCurrentStateInSec_txt     = new JTextField();
	private JTextField       _oInCurrentStateAge_txt       = new JTextField();

	
	
	private static final Color NON_CONFIGURED_MONITORING_COLOR = new Color(255, 224, 115);
	private HashMap<String, String> _originToolTip = new HashMap<String, String>(); // <name><msg>

	/** Color to be used when counters is cleared is used */
	private static final Color COUNTERS_CLEARED_COLOR = Color.ORANGE;
	
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

		tooltip = "This is the Postgres internal instance/server name.";
		_dbmsServerName_lbl   .setText("Instance name");
		_dbmsServerName_lbl   .setToolTipText(tooltip);
		_dbmsServerName_txt   .setToolTipText(tooltip);
		_dbmsServerName_txt   .setEditable(false);

		tooltip = "This is the hostname where Postgres is running";
		_dbmsListeners_lbl    .setText("Hostname");
		_dbmsListeners_lbl    .setToolTipText(tooltip);
		_dbmsListeners_txt    .setToolTipText(tooltip);
		_dbmsListeners_txt    .setEditable(false);

		tooltip = "This is the Postgres version.";
		_dbmsVersion_lbl      .setText("Version");
		_dbmsVersion_lbl      .setToolTipText(tooltip);
		_dbmsVersion_txt      .setToolTipText(tooltip);
		_dbmsVersion_txt      .setEditable(false);

		tooltip = "Time of last sample.";
		_lastSampleTime_lbl   .setText("Sample time");
		_lastSampleTime_lbl   .setToolTipText(tooltip);
		_lastSampleTime_txt   .setToolTipText(tooltip);
		_lastSampleTime_txt   .setEditable(false);

		tooltip = "UTC Time Difference in minutes (positive east of UK, negative west of UK).";
		_utcTimeDiff_lbl      .setText("UTC Time Diff");
		_utcTimeDiff_lbl      .setToolTipText(tooltip);
		_utcTimeDiff_txt      .setToolTipText(tooltip);
		_utcTimeDiff_txt      .setEditable(false);

		tooltip = "Date and time that the Postgres was started.";
		_startDate_lbl        .setText("Start date");
		_startDate_lbl        .setToolTipText(tooltip);
		_startDate_txt        .setToolTipText(tooltip);
		_startDate_txt        .setEditable(false);

		tooltip = "If Postgres is in Recovery Mode or not... If it's TRUE, then it's probably in Standby Mode, and receiving/applying transactions from the ACTIVE Postgres instance.";
		_inRecovery_lbl       .setText("In Recovery Mode");
		_inRecovery_lbl       .setToolTipText(tooltip);
		_inRecovery_txt       .setToolTipText(tooltip);
		_inRecovery_txt       .setEditable(false);


		tooltip = "Number concurrent of blocking locks";
		_blockingLockCount_lbl.setText("Blocking Lock Count");
		_blockingLockCount_lbl.setToolTipText(tooltip);
		_blockingLockCount_txt.setToolTipText(tooltip);
		_blockingLockCount_txt.setEditable(false);

		tooltip = "Start Time for oldest blocking locks";
		_blockingLockWaitStart_lbl.setText("Blocking Lock Start Time");
		_blockingLockWaitStart_lbl.setToolTipText(tooltip);
		_blockingLockWaitStart_txt.setToolTipText(tooltip);
		_blockingLockWaitStart_txt.setEditable(false);

		tooltip = "Wait time in seconds for oldest blocking locks";
		_blockingLockWaitInSec_lbl.setText("Blocking Lock Time in Sec");
		_blockingLockWaitInSec_lbl.setToolTipText(tooltip);
		_blockingLockWaitInSec_txt.setToolTipText(tooltip);
		_blockingLockWaitInSec_txt.setEditable(false);


//		tooltip = "Oldest Transaction Start Time";
//		_oldestXactStartInSec_lbl.setText("Oldest Xact Start in Sec");
//		_oldestXactStartInSec_lbl.setToolTipText(tooltip);
//		_oldestXactStartInSec_txt.setToolTipText(tooltip);
//		_oldestXactStartInSec_txt.setEditable(false);
//
//		tooltip = "Oldest Statement Start Time in Seconds";
//		_oldestStmntStartInSec_lbl.setText("Oldest Stmnt Start in Sec");
//		_oldestStmntStartInSec_lbl.setToolTipText(tooltip);
//		_oldestStmntStartInSec_txt.setToolTipText(tooltip);
//		_oldestStmntStartInSec_txt.setEditable(false);


		String baseTooltipForOldestXxx = "<html>"
				+ "<h2>Getting out of transaction ID (TXID) wraparound</h2>"
				+ "<h3>Checking if there is a stuck transaction ID</h3>"
				+ "One possible reason why the system can run out of transaction IDs is that PostgreSQL can't <b>freeze</b> (that is, mark as visible to all transactions) <br>"
				+ "any transaction IDs created after the oldest currently running transaction started because of multiversion concurrency control (MVCC) rules. <br>"
				+ "In extreme cases, such transactions can become so old, that they make it impossible for <b>VACUUM</b> to clean up any old transactions for <br>"
				+ "the entire 2 billion transaction id wraparound limit and cause the whole system to <font color='red'><b>stop accepting new DML (insert, update, delete)</b></font>. <br>"
				+ "You typically also see warnings in the log file, saying <b>WARNING: oldest xmin is far in the past.</b> <br>"
				+ "<br>"
				+ "You should move on to optimization only after the <b>stuck transaction ID</b> has been remediated.<br>"
				+ "<br>"
				+ "Here are four potential reasons why there may be a stuck transaction ID and how to mitigate each of them: <br>"
				+ "<ul>"
				+ "  <li><b>Long running transactions</b>:    Identify them and cancel or terminate the backend to unblock the vacuum.</li>"
				+ "  <li><b>Orphaned prepare transaction</b>: Rollback orphaned prepare transactions.</li>"
				+ "  <li><b>Abandoned replication slots</b>:  Drop the abandoned slots.</li>"
				+ "  <li><b>Long running transaction on replica, with <code>hot_standby_feedback = on</code></b>: Identify them and cancel or terminate the backend to unblock the vacuum.</li>"
				+ "</ul>"
				+ "<br>"
				+ "SQL Statement used to get this value: <code>${REPLACE_THIS_WITH_SQL}</code>"
				+ "</html>";

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(backend_xmin)) FROM pg_stat_activity  WHERE state != 'idle'");
		_oldestBackendXminAge_lbl     .setText("Oldest Backend Xmin 'Age'");
		_oldestBackendXminAge_lbl     .setToolTipText(tooltip);
		_oldestBackendXminAge_txt     .setToolTipText(tooltip);
		_oldestBackendXminAge_txt     .setEditable(false);

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(transaction)) FROM pg_prepared_xacts");
		_oldestPreparedXactAge_lbl    .setText("Oldest Prepared Xact 'Age'");
		_oldestPreparedXactAge_lbl    .setToolTipText(tooltip);
		_oldestPreparedXactAge_txt    .setToolTipText(tooltip);
		_oldestPreparedXactAge_txt    .setEditable(false);

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(xmin)) FROM pg_replication_slots");
		_oldestReplicationSlotAge_lbl .setText("Oldest Replication Slot 'Age'");
		_oldestReplicationSlotAge_lbl .setToolTipText(tooltip);
		_oldestReplicationSlotAge_txt .setToolTipText(tooltip);
		_oldestReplicationSlotAge_txt .setEditable(false);

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(backend_xmin)) FROM pg_stat_replication");
		_oldestReplicaXactAge_lbl     .setText("Oldest Replica Xact 'Age'");
		_oldestReplicaXactAge_lbl     .setToolTipText(tooltip);
		_oldestReplicaXactAge_txt     .setToolTipText(tooltip);
		_oldestReplicaXactAge_txt     .setEditable(false);

		
		tooltip = "Olest Active or Idle-in-transaction Transaction information.";
		_oXactInfo_lbl                .setToolTipText(tooltip); _oXactInfo_lbl.setText("Info about oldest Active/Idle Transaction:");

		tooltip = "In what 'state' do the oldest open transaction have. Probably: 'active', or 'idle in transaction' (meaning uncommitted transaction, Probably stay in AutoCommit=false for to long.)";
		_oXactState_lbl               .setToolTipText(tooltip); _oXactState_lbl.setText("In State");
		_oXactState_txt               .setToolTipText(tooltip); _oXactState_txt.setEditable(false);

		tooltip = "Backend 'pid' of the oldest transaction";
		_oXactPid_lbl                 .setToolTipText(tooltip); _oXactPid_lbl.setText("Backend Pid");
		_oXactPid_txt                 .setToolTipText(tooltip); _oXactPid_txt.setEditable(false);

		tooltip = "In what 'database' context does the oldest transaction have";
		_oXactDbname_lbl              .setToolTipText(tooltip); _oXactDbname_lbl.setText("Dbname");
		_oXactDbname_txt              .setToolTipText(tooltip); _oXactDbname_txt.setEditable(false);

		tooltip = "What 'username' holds the oldest transaction";
		_oXactUsername_lbl            .setToolTipText(tooltip); _oXactUsername_lbl.setText("Username");
		_oXactUsername_txt            .setToolTipText(tooltip); _oXactUsername_txt.setEditable(false);

		tooltip = "What 'application name' holds the oldest transaction";
		_oXactAppname_lbl             .setToolTipText(tooltip); _oXactAppname_lbl.setText("Application");
		_oXactAppname_txt             .setToolTipText(tooltip); _oXactAppname_txt.setEditable(false);

		tooltip = "<html>If the 'pid' is waiting for something. (possibly a blocking lock). In Version above 10, we can also see <b>what</b> we are waiting on.</html>";
		_oXactIsWaiting_lbl           .setToolTipText(tooltip); _oXactIsWaiting_lbl.setText("Is Waiting");
		_oXactIsWaiting_chk           .setToolTipText(tooltip); _oXactIsWaiting_chk .setEnabled(false);
		_oXactIsWaiting1_txt          .setToolTipText(tooltip); _oXactIsWaiting1_txt.setEditable(false);
		_oXactIsWaiting2_txt          .setToolTipText(tooltip); _oXactIsWaiting2_txt.setEditable(false);

		tooltip = "When did the oldest transaction start.";
		_oXactStartInSec_lbl          .setToolTipText(tooltip); _oXactStartInSec_lbl.setText("Transaction Start Time");
		_oXactStartInSec_txt          .setToolTipText(tooltip); _oXactStartInSec_txt.setEditable(false);
		_oXactStartAge_txt            .setToolTipText(tooltip); _oXactStartAge_txt  .setEditable(false);

		tooltip = "When did the last SQL Statement start.";
		_oStmntStartInSec_lbl         .setToolTipText(tooltip); _oStmntStartInSec_lbl.setText("Last Stmnt Start Time");
		_oStmntStartInSec_txt         .setToolTipText(tooltip); _oStmntStartInSec_txt.setEditable(false);
		_oStmntStartAge_txt           .setToolTipText(tooltip); _oStmntStartAge_txt  .setEditable(false);

		tooltip = "<html>For how long did the last SQL Statement execute. <br>"
				+ "(if the 'state' is 'active' then it's the execution time for the currently executing SQL Statement.</html>";
		_oStmntExecInSec_lbl          .setToolTipText(tooltip); _oStmntExecInSec_lbl.setText("Last Stmnt Exec Time");
		_oStmntExecInSec_txt          .setToolTipText(tooltip); _oStmntExecInSec_txt.setEditable(false);
		_oStmntExecAge_txt            .setToolTipText(tooltip); _oStmntExecAge_txt  .setEditable(false);

		tooltip = "<html>When did the 'state' change. <br>"
				+ "Example from 'active' to 'idle in transaction'...<br>"
				+ "The above indicates that we might have a longer transaction than needed. <br>"
				+ "Either by a manual transaction, or by running in AutoCommit=false and not turning AutoCommitt=true.<br>"
				+ "<b>Note:</b> If AutoCommit=false, it's not enough to just commit/rollback... That just starts a new transaction!</html>";
		_oInCurrentStateInSec_lbl     .setToolTipText(tooltip); _oInCurrentStateInSec_lbl.setText("In Current State Time");
		_oInCurrentStateInSec_txt     .setToolTipText(tooltip); _oInCurrentStateInSec_txt.setEditable(false);
		_oInCurrentStateAge_txt       .setToolTipText(tooltip); _oInCurrentStateAge_txt  .setEditable(false);
		
		
		//--------------------------
		// DO the LAYOUT
		//--------------------------
		panel.add(_localServerName_lbl,     "");
		panel.add(_localServerName_txt,     "growx, wrap");
		
		panel.add(_dbmsServerName_lbl,           "");
		panel.add(_dbmsServerName_txt,           "growx, wrap");
		
		panel.add(_dbmsListeners_lbl,            "");
		panel.add(_dbmsListeners_txt,            "growx, wrap");
		
		panel.add(_dbmsVersion_lbl,              "");
		panel.add(_dbmsVersion_txt,              "growx, wrap");
		
		panel.add(_lastSampleTime_lbl,           "");
		panel.add(_lastSampleTime_txt,           "growx, wrap");
		
		panel.add(_utcTimeDiff_lbl,              "");
		panel.add(_utcTimeDiff_txt,              "growx, wrap");
		
		panel.add(_startDate_lbl,                "");
		panel.add(_startDate_txt,                "growx, wrap");

		panel.add(_inRecovery_lbl,               "");
		panel.add(_inRecovery_txt,               "growx, wrap 20");


		panel.add(_blockingLockCount_lbl,        "");
		panel.add(_blockingLockCount_txt,        "growx, wrap");

		panel.add(_blockingLockWaitStart_lbl,    "");
		panel.add(_blockingLockWaitStart_txt,    "growx, wrap");

		panel.add(_blockingLockWaitInSec_lbl,    "");
		panel.add(_blockingLockWaitInSec_txt,    "growx, wrap 20");


//		panel.add(_oldestXactStartInSec_lbl,     "");
//		panel.add(_oldestXactStartInSec_txt,     "growx, wrap");
//
//		panel.add(_oldestStmntStartInSec_lbl,    "");
//		panel.add(_oldestStmntStartInSec_txt,    "growx, wrap 20");


		panel.add(_oldestBackendXminAge_lbl,     "");
		panel.add(_oldestBackendXminAge_txt,     "growx, wrap");

		panel.add(_oldestPreparedXactAge_lbl,    "");
		panel.add(_oldestPreparedXactAge_txt,    "growx, wrap");

		panel.add(_oldestReplicationSlotAge_lbl, "");
		panel.add(_oldestReplicationSlotAge_txt, "growx, wrap");

		panel.add(_oldestReplicaXactAge_lbl,     "");
		panel.add(_oldestReplicaXactAge_txt,     "growx, wrap 20");


		//--------------------------
		panel.add(_oXactInfo_lbl               , "span, wrap");
                                               
		panel.add(_oXactState_lbl              , "");
		panel.add(_oXactState_txt              , "growx, wrap");
                                               
		panel.add(_oXactPid_lbl                , "");
		panel.add(_oXactPid_txt                , "growx, wrap");
                                               
		panel.add(_oXactDbname_lbl             , "");
		panel.add(_oXactDbname_txt             , "growx, wrap");
                                               
		panel.add(_oXactUsername_lbl           , "");
		panel.add(_oXactUsername_txt           , "growx, wrap");
                                               
		panel.add(_oXactAppname_lbl            , "");
		panel.add(_oXactAppname_txt            , "growx, wrap");
                                               
		panel.add(_oXactIsWaiting_lbl          , "split 2");
		panel.add(_oXactIsWaiting_chk          , "");
		panel.add(_oXactIsWaiting1_txt         , "growx, wrap");
		panel.add(_oXactIsWaiting2_txt         , "skip, growx, wrap");
                                               
		panel.add(_oXactStartInSec_lbl         , "");
		panel.add(_oXactStartInSec_txt         , "growx, split");
		panel.add(_oXactStartAge_txt           , "growx, wrap");
                                               
		panel.add(_oStmntStartInSec_lbl        , "");
		panel.add(_oStmntStartInSec_txt        , "growx, split");
		panel.add(_oStmntStartAge_txt          , "growx, wrap");
                                               
		panel.add(_oStmntExecInSec_lbl         , "");
		panel.add(_oStmntExecInSec_txt         , "growx, split");
		panel.add(_oStmntExecAge_txt           , "growx, wrap");
                                               
		panel.add(_oInCurrentStateInSec_lbl    , "");
		panel.add(_oInCurrentStateInSec_txt    , "growx, split");
		panel.add(_oInCurrentStateAge_txt      , "growx, wrap");
		
		
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

//		_Transactions_Abs_txt   .setVisible(showAbs); 
//		_Transactions_Diff_txt  .setVisible(showDiff);
//		_Transactions_Rate_txt  .setVisible(showRate);
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

	@Override
	public void setSummaryData(CountersModel cm, boolean postProcessing)
	{
		setWatermark();
//System.out.println("getColNames="+cm.getCounterSampleAbs().getColNames());
//System.out.println("getColNames="+cm.getCounterSampleAbs().getDataCollection());

		_dbmsServerName_txt         .setText(cm.getAbsString (0, "instance_name"));
		_dbmsListeners_txt          .setText(cm.getAbsString (0, "host"));
		_dbmsVersion_txt            .setText(cm.getAbsString (0, "version"));           _dbmsVersion_txt.setCaretPosition(0);
		_lastSampleTime_txt         .setText(cm.getAbsString (0, "time_now"));
		_utcTimeDiff_txt            .setText(cm.getAbsString (0, "utc_minute_diff"));
		_startDate_txt              .setText(cm.getAbsString (0, "start_time"));
		_inRecovery_txt             .setText(cm.getAbsString (0, "in_recovery").toUpperCase() );
		
		_blockingLockCount_txt        .setText(cm.getAbsString (0, "blocking_lock_count"     ,  false, ""));
		_blockingLockWaitStart_txt    .setText(cm.getAbsString (0, "blocking_lock_wait_start",  false, ""));
		_blockingLockWaitInSec_txt    .setText(cm.getAbsString (0, "blocking_lock_wait_in_sec", false, "-1"));

//		_oldestXactStartInSec_txt     .setText(cm.getAbsString (0, "oldest_xact_start_in_sec"));
//		_oldestStmntStartInSec_txt    .setText(cm.getAbsString (0, "oldest_stmnt_start_in_sec"));
		
		_oldestBackendXminAge_txt     .setText(cm.getAbsString (0, "oldest_backend_xmin_age"));
		_oldestPreparedXactAge_txt    .setText(cm.getAbsString (0, "oldest_prepared_xact_age"));
		_oldestReplicationSlotAge_txt .setText(cm.getAbsString (0, "oldest_replication_slot_age"));
		_oldestReplicaXactAge_txt     .setText(cm.getAbsString (0, "oldest_replica_xact_age"));

		_oXactState_txt               .setText(cm.getAbsString (0, "oxact_state"));
		_oXactPid_txt                 .setText(cm.getAbsString (0, "oxact_pid"));
		_oXactDbname_txt              .setText(cm.getAbsString (0, "oxact_dbname"));
		_oXactUsername_txt            .setText(cm.getAbsString (0, "oxact_username"));
		_oXactAppname_txt             .setText(cm.getAbsString (0, "oxact_appname"));
		_oXactIsWaiting_chk           .setSelected("true".equalsIgnoreCase(cm.getAbsString (0, "oxact_is_waiting")));
		_oXactIsWaiting1_txt          .setText(cm.getAbsString (0, "oxact_is_waiting_type"));
		_oXactIsWaiting2_txt          .setText(cm.getAbsString (0, "oxact_is_waiting_event"));

		_oXactStartInSec_txt          .setText(cm.getAbsString (0, "oxact_xact_start_in_sec"));
		_oXactStartAge_txt            .setText(cm.getAbsString (0, "oxact_xact_start_age"));
		
		_oStmntStartInSec_txt         .setText(cm.getAbsString (0, "oxact_stmnt_start_in_sec"));
		_oStmntStartAge_txt           .setText(cm.getAbsString (0, "oxact_stmnt_start_age"));
		
		_oStmntExecInSec_txt          .setText(cm.getAbsString (0, "oxact_stmnt_last_exec_in_sec"));
		_oStmntExecAge_txt            .setText(cm.getAbsString (0, "oxact_stmnt_last_exec_age"));
		
		_oInCurrentStateInSec_txt     .setText(cm.getAbsString (0, "oxact_in_current_state_in_sec"));
		_oInCurrentStateAge_txt       .setText(cm.getAbsString (0, "oxact_in_current_state_age"));
		
		// Set the background color to RED if we are in "recovery mode"
		boolean inRecoveryMode = "true".equalsIgnoreCase( cm.getAbsString (0, "in_recovery") );
		_inRecovery_txt.setBackground( inRecoveryMode ? Color.RED : _startDate_txt.getBackground() );

		//----------------------------------------------
		// Check LOCK WAITS and, do notification
		//----------------------------------------------
		// Set the background color to RED if we have Blocking
		int     blockingLockCount = cm.getAbsValueAsInteger(0, "blocking_lock_count", false, -1);
		boolean hasBlockingCount  = blockingLockCount > 0;
		_blockingLockCount_txt    .setBackground( hasBlockingCount ? Color.RED : _startDate_txt.getBackground() );
		_blockingLockWaitStart_txt.setBackground( hasBlockingCount ? Color.RED : _startDate_txt.getBackground() );
		_blockingLockWaitInSec_txt.setBackground( hasBlockingCount ? Color.RED : _startDate_txt.getBackground() );
		
		if (hasBlockingCount)
		{
			boolean isVisibleInPrevSample = MainFrame.getInstance().hasBlockingLocks();
			MainFrame.getInstance().setBlockingLocks(true, blockingLockCount);

			String toTabName = "Blocking";
			if ( _focusToBlockingTab == null )
				_focusToBlockingTab = new ChangeToJTabDialog(MainFrame.getInstance(), "Found Blocking Locks...", cm.getGuiController().getTabbedPane(), toTabName);

			if ( ! isVisibleInPrevSample )
				_focusToBlockingTab.setVisible(true);
		}
		else
		{
			MainFrame.getInstance().setBlockingLocks(hasBlockingCount, 0);
		}
		// end: Check LOCK WAITS and, do notification

		//----------------------------------------------
		// Check OLDEST OPEN TRANSACTION and, do notification
		//----------------------------------------------
		int oldestOpenTranInSec          = cm.getAbsValueAsInteger(0, "oxact_xact_start_in_sec", false, -1);
		int oldestOpenTranInSecThreshold = CmSummary.DEFAULT_alarm_oldestOpenTranInSec; 
		_logger.debug("OLDEST-OPEN-TRANSACTION="+oldestOpenTranInSec+".");
		if (oldestOpenTranInSec > oldestOpenTranInSecThreshold)
		{
			_oXactStartInSec_txt.setBackground(Color.RED);
			_oXactStartAge_txt  .setBackground(Color.RED);

			boolean isVisibleInPrevSample = MainFrame.getInstance().hasOldestOpenTran();
			MainFrame.getInstance().setOldestOpenTran(true, oldestOpenTranInSec);

			String toTabName = "Active Statements";
			if ( _focusToActiveStmntsTab_oldestOpenTran == null )
				_focusToActiveStmntsTab_oldestOpenTran = new ChangeToJTabDialog(MainFrame.getInstance(), "Found A 'long' running Transaction", cm.getGuiController().getTabbedPane(), toTabName);

			if ( ! isVisibleInPrevSample )
				_focusToActiveStmntsTab_oldestOpenTran.setVisible(true);
		}
		else
		{
			_oXactStartInSec_txt.setBackground(_startDate_txt.getBackground());
			_oXactStartAge_txt  .setBackground(_startDate_txt.getBackground());

			MainFrame.getInstance().setOldestOpenTran(false, 0);
		}
		// end: Check OLDEST OPEN TRANSACTION and, do notification

		//----------------------------------------------
		// Check OLDEST STATEMENT and, do notification
		//----------------------------------------------
		int oldestStatementInSec          = cm.getAbsValueAsInteger(0, "oxact_stmnt_last_exec_in_sec", false, -1);
		int oldestStatementInSecThreshold = CmSummary.DEFAULT_alarm_oldestStatementInSec;
		if (oldestStatementInSec > oldestStatementInSecThreshold)
		{
			_oStmntExecInSec_txt.setBackground(Color.RED);
			_oStmntExecAge_txt  .setBackground(Color.RED);

//			boolean isVisibleInPrevSample = MainFrame.getInstance().hasOldestStatement();
//			MainFrame.getInstance().setOldestStatement(true, oldestOpenTranInSec);
//
//			String toTabName = "Active Statements";
//			if ( _focusToActiveStmntsTab_oldestStmnt == null )
//				_focusToActiveStmntsTab_oldestStmnt = new ChangeToJTabDialog(MainFrame.getInstance(), "Found A 'long' running Statement", cm.getGuiController().getTabbedPane(), toTabName);
//
//			if ( ! isVisibleInPrevSample )
//				_focusToActiveStmntsTab_oldestStmnt.setVisible(true);
		}
		else
		{
			_oStmntExecInSec_txt.setBackground(_startDate_txt.getBackground());
			_oStmntExecAge_txt  .setBackground(_startDate_txt.getBackground());

//			MainFrame.getInstance().setOldestStatement(false, 0);
		}
		// end: Check OLDEST STATEMENT and, do notification
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

		// Server info
		_localServerName_txt    .setText("");

		_dbmsServerName_txt         .setText("");
		_dbmsListeners_txt          .setText("");
		_dbmsVersion_txt            .setText("");
		_lastSampleTime_txt         .setText("");
		_utcTimeDiff_txt            .setText("");
		_startDate_txt              .setText("");
		_inRecovery_txt             .setText("");  _inRecovery_txt.setBackground( _startDate_txt.getBackground() );


		_blockingLockCount_txt        .setText(""); _blockingLockCount_txt    .setBackground( _startDate_txt.getBackground() );
		_blockingLockWaitStart_txt    .setText(""); _blockingLockWaitStart_txt.setBackground( _startDate_txt.getBackground() );
		_blockingLockWaitInSec_txt    .setText(""); _blockingLockWaitInSec_txt.setBackground( _startDate_txt.getBackground() );

		_oXactStartInSec_txt          .setText(""); _oXactStartInSec_txt      .setBackground( _startDate_txt.getBackground() );
		_oXactStartAge_txt            .setText(""); _oXactStartAge_txt        .setBackground( _startDate_txt.getBackground() );
		_oStmntExecInSec_txt          .setText(""); _oStmntExecInSec_txt      .setBackground( _startDate_txt.getBackground() );
		_oStmntExecAge_txt            .setText(""); _oStmntExecAge_txt        .setBackground( _startDate_txt.getBackground() );

		_oldestBackendXminAge_txt     .setText("");
		_oldestPreparedXactAge_txt    .setText("");
		_oldestReplicationSlotAge_txt .setText("");
		_oldestReplicaXactAge_txt     .setText("");
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
//		Configuration conf = Configuration.getInstance(Configuration.USER_TEMP);
//
//		conf.save();
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
}
