/*******************************************************************************
 * Copyright (C) 2010-2027 Goran Schwarz
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
package com.dbxtune.cm.db2.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dbxtune.CounterController;
import com.dbxtune.Version;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.cm.db2.CmSummary;
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

/**
 * The Summary panel (left side of the GUI) for Db2Tune: the fields of CmSummary.
 */
public class CmSummaryPanel
extends JPanel
implements ISummaryPanel, TableModelListener, GTabbedPane.ShowProperties
{
	private static final Logger _logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	private static final long    serialVersionUID      = 1L;

	private static final JTextField DUMMY_TEXTFIELD = new JTextField();
	private static final Font       RATE_FONT       = new Font(DUMMY_TEXTFIELD.getFont().getFontName(), Font.ITALIC, DUMMY_TEXTFIELD.getFont().getSize());

	private CountersModel      _cm = null;
	private Watermark          _watermark;
	private Icon               _icon = null;

	private JPanel             _serverInfoPanel;
	private JPanel             _dataPanel;
	private JScrollPane        _dataPanelScroll;
	private TrendGraphDashboardPanel _graphPanel;

	private JLabel             _title_lbl                 = new JLabel();
	private JButton            _trendGraphs_but           = new JButton();

	// Server
	private JLabel             _localServerName_lbl       = new JLabel("Connection Info");
	private JTextField         _localServerName_txt       = new JTextField();
	private JLabel             _host_lbl                  = new JLabel("Hostname");
	private JTextField         _host_txt                  = new JTextField();
	private JLabel             _version_lbl               = new JLabel("Version");
	private JTextField         _version_txt               = new JTextField();
	private JLabel             _instDb_lbl                = new JLabel("Instance / Database");
	private JTextField         _instance_txt              = new JTextField();
	private JTextField         _database_txt              = new JTextField();
	private JLabel             _lastSampleTime_lbl        = new JLabel("Sample time");
	private JTextField         _lastSampleTime_txt        = new JTextField();
	private JLabel             _utcTimeDiff_lbl           = new JLabel("UTC Time Diff");
	private JTextField         _utcTimeDiff_txt           = new JTextField();
	private JLabel             _startDate_lbl             = new JLabel("Instance start");
	private JTextField         _startDate_txt             = new JTextField();
	private JLabel             _activation_lbl            = new JLabel("Database activated");
	private JTextField         _activation_txt            = new JTextField();
	private JLabel             _dbStatus_lbl              = new JLabel("Status / Activation");
	private JTextField         _dbStatus_txt              = new JTextField();
	private JTextField         _activationState_txt       = new JTextField();
	private JLabel             _hadrRole_lbl              = new JLabel("HADR Role");
	private JTextField         _hadrRole_txt              = new JTextField();
	private JLabel             _lastBackup_lbl            = new JLabel("Last Backup");
	private JTextField         _lastBackup_txt            = new JTextField();

	// Connections
	private JLabel             _connections_lbl           = new JLabel("Connections");
	private JTextField         _connectionsAbs_txt        = new JTextField();
	private JTextField         _connectionsDiff_txt       = new JTextField();
	private JLabel             _executing_lbl             = new JLabel("Executing in Db2");
	private JTextField         _executing_txt             = new JTextField();
	private JLabel             _connectionsTop_lbl        = new JLabel("Connections Top");
	private JTextField         _connectionsTop_txt        = new JTextField();

	// Transactions, log and locks
	private JLabel             _oldestOpenTranThreshold_lbl = new JLabel("Oldest Open Tran Threshold");
	private JTextField         _oldestOpenTranThreshold_txt = new JTextField();
	private JLabel             _oldestOpenTranSec_lbl     = new JLabel("Oldest Open Tran in Sec");
	private JTextField         _oldestOpenTranSec_txt     = new JTextField();
	private JLabel             _oldestOpenTranTime_lbl    = new JLabel("Oldest Open Tran Start");
	private JTextField         _oldestOpenTranTime_txt    = new JTextField();
	private JLabel             _oldestOpenTranWho_lbl     = new JLabel("Oldest Open Tran Who");
	private JTextField         _oldestOpenTranWho_txt     = new JTextField();
	private JLabel             _openTranCount_lbl         = new JLabel("Open Transactions");
	private JTextField         _openTranCount_txt         = new JTextField();
	private JLabel             _logUsedPct_lbl            = new JLabel("Transaction Log Used %");
	private JTextField         _logUsedPct_txt            = new JTextField();
	private JLabel             _blockingWaitThreshold_lbl = new JLabel("Lock Wait Threshold");
	private JTextField         _blockingWaitThreshold_txt = new JTextField();
	private JLabel             _lockWaiters_lbl           = new JLabel("Lock Waiters / Blockers");
	private JTextField         _lockWaiters_txt           = new JTextField();
	private JTextField         _lockBlockers_txt          = new JTextField();
	private JLabel             _maxLockWaitSec_lbl        = new JLabel("Lock Wait Max Sec");
	private JTextField         _maxLockWaitSec_txt        = new JTextField();
	private JLabel             _locksHeld_lbl             = new JLabel("Locks Held");
	private JTextField         _locksHeld_txt             = new JTextField();

	// Caches
	private JLabel             _bpHitPct_lbl              = new JLabel("BP Hit % (since/sample)");
	private JTextField         _bpHitPctAbs_txt           = new JTextField();
	private JTextField         _bpHitPctDiff_txt          = new JTextField();
	private JLabel             _pkgCacheHitPct_lbl        = new JLabel("Pkg Cache Hit % (since/sample)");
	private JTextField         _pkgCacheHitPctAbs_txt     = new JTextField();
	private JTextField         _pkgCacheHitPctDiff_txt    = new JTextField();

	/** One line with Abs, Diff and Rate for a counter in CmSummary */
	private static class CounterRow
	{
		String     colName;
		JLabel     label   = new JLabel();
		JTextField absTxt  = new JTextField();
		JTextField diffTxt = new JTextField();
		JTextField rateTxt = new JTextField();

		CounterRow(String colName, String labelText, String tooltip)
		{
			this.colName = colName;
			label.setText(labelText);
			for (JComponent c : new JComponent[] { label, absTxt, diffTxt, rateTxt })
				c.setToolTipText(tooltip);
			absTxt .setEditable(false);
			diffTxt.setEditable(false);
			rateTxt.setEditable(false);
			diffTxt.setForeground(Color.BLUE);
			rateTxt.setForeground(Color.BLUE);
			rateTxt.setFont(RATE_FONT);
		}
	}
	private List<CounterRow> _counterRows = new ArrayList<>();

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

		try
		{
			initComponents();
		}
		catch (Exception ex)
		{
			_logger.error("Can't create the summary panel", ex);
		}
	}

	@Override public Icon          getIcon()           { return _icon; }
	public           void          setIcon(Icon icon)  { _icon = icon; }
	@Override public String        getPanelName()      { return CmSummary.SHORT_NAME; }
	@Override public String        getDescription()    { return CmSummary.HTML_DESC; }
	@Override public CountersModel getCm()             { return _cm; }

	private void initComponents()
	throws Exception
	{
		setLayout(new BorderLayout());

		_graphPanel      = new TrendGraphDashboardPanel();
		_dataPanel       = createDataPanel();
		_dataPanelScroll = new JScrollPane(_dataPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		_dataPanelScroll.getVerticalScrollBar().setUnitIncrement(16);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _dataPanelScroll, _graphPanel);
		add(split, BorderLayout.CENTER);

		_watermark = new Watermark(_graphPanel, "Not Connected...");

		loadProps();
	}

	private JPanel createDataPanel()
	{
		JPanel panel = SwingUtils.createPanel("title", false);
		panel.setLayout(new MigLayout("", "5[grow]5", ""));

		_title_lbl.setFont(new java.awt.Font("Dialog", 1, SwingUtils.hiDpiScale(16)));
		_title_lbl.setText("Summary panel");

		_serverInfoPanel = createServerInfoPanel();

		TrendGraph.createGraphAccessButton(_trendGraphs_but, CmSummary.CM_NAME);

		panel.add(_title_lbl,           "pushx, growx, left, split");
		panel.add(_trendGraphs_but,     "top, wrap");
		panel.add(_serverInfoPanel,     "growx, width 275lp, wrap"); //275 logical pixels

		return panel;
	}

	/** Set the tooltip and make it read only */
	private static void init(JLabel lbl, String tooltip, JTextField... fields)
	{
		lbl.setToolTipText(tooltip);
		for (JTextField f : fields)
		{
			f.setToolTipText(tooltip);
			f.setEditable(false);
		}
	}

	private JPanel createServerInfoPanel()
	{
		JPanel panel = new JPanel()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public String getToolTipText()
			{
				CountersModel cm = _cm;
				String sqlRefreshTime = (cm == null) ? "Unavailable" : cm.getSqlRefreshTime() + " ms.";
				String guiRefreshTime = (cm == null) ? "Unavailable" : cm.getGuiRefreshTime() + " ms.";
				String lcRefreshTime  = (cm == null) ? "Unavailable" : cm.getLcRefreshTime() + " ms.";

				return "<html>" +
						"SQL Refresh time: " + sqlRefreshTime + "<br>" +
						"GUI Refresh Time: " + guiRefreshTime + "<br>" +
						"Local Calculation Time: " + lcRefreshTime + "<br>" +
						"</html>";
			}
		};
		// need to set/register the tooltip, otherwise it will grab parents tooltip.
		ToolTipManager.sharedInstance().registerComponent(panel);

		panel.setBorder(BorderFactory.createTitledBorder("Server Information"));
		panel.setLayout(new MigLayout("", "[] [grow]", ""));

		init(_localServerName_lbl,         "The name we used when " + Version.getAppName() + " connected to the server (host:port/DBNAME).", _localServerName_txt);
		init(_host_lbl,                    "The host where Db2 is running.", _host_txt);
		init(_version_lbl,                 "The Db2 version (SERVICE_LEVEL).", _version_txt);
		init(_instDb_lbl,                  "The Db2 instance, and the database that " + Version.getAppName() + " monitors.", _instance_txt, _database_txt);
		init(_lastSampleTime_lbl,          "Time of last sample.", _lastSampleTime_txt);
		init(_utcTimeDiff_lbl,             "UTC Time Difference in minutes (positive east of UK, negative west of UK).", _utcTimeDiff_txt);
		init(_startDate_lbl,               "When the Db2 instance was started (DB2START).", _startDate_txt);
		init(_activation_lbl,              "When the database was activated. The counters start from 0 at this time.", _activation_txt);
		init(_dbStatus_lbl,                "Status of the database (ACTIVE, QUIESCED, ROLLFWD, ACTIVE_STANDBY...), and how it was activated: EXPLICIT (ACTIVATE DATABASE) or NONE (by the first connection, it is deactivated when the last one leaves).", _dbStatus_txt, _activationState_txt);
		init(_hadrRole_lbl,                "HADR role of the database: STANDARD (no HADR), PRIMARY or STANDBY.", _hadrRole_txt);
		init(_lastBackup_lbl,              "When the last backup of the database was taken.", _lastBackup_txt);

		init(_connections_lbl,             "Applications connected to the database right now, and the change since the last sample.", _connectionsAbs_txt, _connectionsDiff_txt);
		init(_executing_lbl,               "Applications that are executing in Db2 right now (not waiting for the client).", _executing_txt);
		init(_connectionsTop_lbl,          "The most connections at the same time since the database was activated.", _connectionsTop_txt);

		init(_oldestOpenTranThreshold_lbl, "The threshold (in seconds) of the alarm 'oldestOpenTranInSec'.", _oldestOpenTranThreshold_txt);
		init(_oldestOpenTranSec_lbl,       "How long the oldest open transaction has been open, in seconds. 0 = no open transaction.", _oldestOpenTranSec_txt);
		init(_oldestOpenTranTime_lbl,      "When the oldest open transaction started.", _oldestOpenTranTime_txt);
		init(_oldestOpenTranWho_lbl,       "Who has the oldest open transaction: application handle, user@host, application name, and what it does (UOWWAIT = waits for the client, a forgotten commit?).", _oldestOpenTranWho_txt);
		init(_openTranCount_lbl,           "Open transactions right now (not counting " + Version.getAppName() + "'s own).", _openTranCount_txt);
		init(_logUsedPct_lbl,              "Transaction log used, in percent. At 100% all changes fail with SQL0964C. (see the tab 'Transaction Log')", _logUsedPct_txt);
		init(_blockingWaitThreshold_lbl,   "The threshold (in seconds) of the alarm 'BlockingLockCount'.", _blockingWaitThreshold_txt);
		init(_lockWaiters_lbl,             "Applications that wait for a lock right now, and applications that hold the locks they wait for. (see the tab 'Active Statements' for who blocks who)", _lockWaiters_txt, _lockBlockers_txt);
		init(_maxLockWaitSec_lbl,          "The longest time an application has waited for a lock right now, in seconds.", _maxLockWaitSec_txt);
		init(_locksHeld_lbl,               "Locks held right now.", _locksHeld_txt);

		init(_bpHitPct_lbl,                "Buffer pool hit ratio in percent: since the database was activated, and in the last sample.", _bpHitPctAbs_txt, _bpHitPctDiff_txt);
		init(_pkgCacheHitPct_lbl,          "Package cache hit ratio in percent: since the database was activated, and in the last sample. Low: statements are not reused (literals?), or the cache is too small.", _pkgCacheHitPctAbs_txt, _pkgCacheHitPctDiff_txt);
		_connectionsDiff_txt   .setForeground(Color.BLUE);
		_bpHitPctDiff_txt      .setForeground(Color.BLUE);
		_pkgCacheHitPctDiff_txt.setForeground(Color.BLUE);

		// Counters: Abs, Diff and Rate
		_counterRows.add(new CounterRow("TOTAL_APP_COMMITS",   "Commits",            "Commits issued by the applications (also autocommit)."));
		_counterRows.add(new CounterRow("TOTAL_APP_ROLLBACKS", "Rollbacks",          "Rollbacks issued by the applications."));
		_counterRows.add(new CounterRow("ACT_COMPLETED_TOTAL", "Statements",         "Activities (statements, also the ones in routines) that completed successfully."));
		_counterRows.add(new CounterRow("FAILED_SQL_STMTS",    "Failed Statements",  "Statements that failed (negative SQLCODE)."));
		_counterRows.add(new CounterRow("ROWS_READ",           "Rows Read",          "Rows read from tables."));
		_counterRows.add(new CounterRow("ROWS_RETURNED",       "Rows Returned",      "Rows returned to the applications."));
		_counterRows.add(new CounterRow("ROWS_MODIFIED",       "Rows Modified",      "Rows inserted, updated or deleted."));
		_counterRows.add(new CounterRow("BP_LOGICAL_READS",    "Logical Reads",      "Pages read from the buffer pools."));
		_counterRows.add(new CounterRow("BP_PHYSICAL_READS",   "Physical Reads",     "Pages that had to be read from disk."));
		_counterRows.add(new CounterRow("POOL_WRITES",         "Page Writes",        "Pages written from the buffer pools to disk."));
		_counterRows.add(new CounterRow("LOG_WRITE_MB",        "Log Written MB",     "Transaction log written, in MB."));
		_counterRows.add(new CounterRow("LOCK_WAITS",          "Lock Waits",         "Times an application had to wait for a lock."));
		_counterRows.add(new CounterRow("LOCK_TIMEOUTS",       "Lock Timeouts",      "Lock requests that timed out."));
		_counterRows.add(new CounterRow("DEADLOCKS",           "Deadlocks",          "Deadlocks."));
		_counterRows.add(new CounterRow("LOCK_ESCALS",         "Lock Escalations",   "Row locks that were replaced by a table lock."));
		_counterRows.add(new CounterRow("TOTAL_SORTS",         "Sorts",              "Sorts."));
		_counterRows.add(new CounterRow("SORT_OVERFLOWS",      "Sort Overflows",     "Sorts that did not fit in the sort heap."));
		_counterRows.add(new CounterRow("TOTAL_CPU_TIME",      "CPU Time (us)",      "CPU time used by the database agents, in microseconds."));
		_counterRows.add(new CounterRow("TOTAL_CONS",          "Connects",           "Connects to the database."));

		//--- Server
		panel.add(_localServerName_lbl,         "");
		panel.add(_localServerName_txt,         "growx, wrap");
		panel.add(_host_lbl,                    "");
		panel.add(_host_txt,                    "growx, wrap");
		panel.add(_version_lbl,                 "");
		panel.add(_version_txt,                 "growx, wrap");
		panel.add(_instDb_lbl,                  "");
		panel.add(_instance_txt,                "growx, split");
		panel.add(_database_txt,                "growx, wrap");
		panel.add(_lastSampleTime_lbl,          "");
		panel.add(_lastSampleTime_txt,          "growx, wrap");
		panel.add(_utcTimeDiff_lbl,             "");
		panel.add(_utcTimeDiff_txt,             "growx, wrap");
		panel.add(_startDate_lbl,               "");
		panel.add(_startDate_txt,               "growx, wrap");
		panel.add(_activation_lbl,              "");
		panel.add(_activation_txt,              "growx, wrap");
		panel.add(_dbStatus_lbl,                "");
		panel.add(_dbStatus_txt,                "growx, split");
		panel.add(_activationState_txt,         "growx, wrap");
		panel.add(_hadrRole_lbl,                "");
		panel.add(_hadrRole_txt,                "growx, wrap");
		panel.add(_lastBackup_lbl,              "");
		panel.add(_lastBackup_txt,              "growx, wrap 20");

		//--- Connections
		panel.add(_connections_lbl,             "");
		panel.add(_connectionsAbs_txt,          "growx, split");
		panel.add(_connectionsDiff_txt,         "growx, wrap");
		panel.add(_executing_lbl,               "");
		panel.add(_executing_txt,               "growx, wrap");
		panel.add(_connectionsTop_lbl,          "");
		panel.add(_connectionsTop_txt,          "growx, wrap 20");

		//--- Transactions, log and locks
		panel.add(_oldestOpenTranThreshold_lbl, "");
		panel.add(_oldestOpenTranThreshold_txt, "growx, wrap");
		panel.add(_oldestOpenTranSec_lbl,       "");
		panel.add(_oldestOpenTranSec_txt,       "growx, wrap");
		panel.add(_oldestOpenTranTime_lbl,      "");
		panel.add(_oldestOpenTranTime_txt,      "growx, wrap");
		panel.add(_oldestOpenTranWho_lbl,       "");
		panel.add(_oldestOpenTranWho_txt,       "growx, wrap");
		panel.add(_openTranCount_lbl,           "");
		panel.add(_openTranCount_txt,           "growx, wrap");
		panel.add(_logUsedPct_lbl,              "");
		panel.add(_logUsedPct_txt,              "growx, wrap 10");
		panel.add(_blockingWaitThreshold_lbl,   "");
		panel.add(_blockingWaitThreshold_txt,   "growx, wrap");
		panel.add(_lockWaiters_lbl,             "");
		panel.add(_lockWaiters_txt,             "growx, split");
		panel.add(_lockBlockers_txt,            "growx, wrap");
		panel.add(_maxLockWaitSec_lbl,          "");
		panel.add(_maxLockWaitSec_txt,          "growx, wrap");
		panel.add(_locksHeld_lbl,               "");
		panel.add(_locksHeld_txt,               "growx, wrap 20");

		//--- Caches
		panel.add(_bpHitPct_lbl,                "");
		panel.add(_bpHitPctAbs_txt,             "growx, split");
		panel.add(_bpHitPctDiff_txt,            "growx, wrap");
		panel.add(_pkgCacheHitPct_lbl,          "");
		panel.add(_pkgCacheHitPctAbs_txt,       "growx, split");
		panel.add(_pkgCacheHitPctDiff_txt,      "growx, wrap 20");

		//--- Counters: Abs, Diff, Rate
		for (CounterRow row : _counterRows)
		{
			panel.add(row.label,   "");
			panel.add(row.absTxt,  "growx, split");
			panel.add(row.diffTxt, "growx, split");
			panel.add(row.rateTxt, "growx, wrap");
		}

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

		for (CounterRow row : _counterRows)
		{
			row.label  .setVisible(showAbs || showDiff || showRate);
			row.absTxt .setVisible(showAbs);
			row.diffTxt.setVisible(showDiff);
			row.rateTxt.setVisible(showRate);
		}
	}

	@Override public TrendGraphDashboardPanel getGraphPanel()          { return _graphPanel; }
	@Override public void                     clearGraph()             { _graphPanel.clearGraph(); }
	@Override public void                     addTrendGraph(TrendGraph tg) { _graphPanel.add(tg); }

	@Override
	public void setLocalServerName(String name)
	{
		_localServerName_txt.setText(name);
		_localServerName_txt.setCaretPosition(0);
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		// Do not update the GUI from the "in memory view" (the slider)
		if (MainFrame.isInMemoryViewOn())
			return;

		setSummaryData(_cm, false);
	}

	@Override
	public void setSummaryData(CountersModel cm, boolean postProcessing)
	{
		setWatermark();

		_host_txt                   .setText(cm.getAbsString (0, "HOST"));
		_version_txt                .setText(cm.getAbsString (0, "VERSION"));
		_instance_txt               .setText(cm.getAbsString (0, "INSTANCE_NAME"));
		_database_txt               .setText(cm.getAbsString (0, "DATABASE_NAME"));
		_lastSampleTime_txt         .setText(cm.getAbsString (0, "TIME_NOW"));
		_utcTimeDiff_txt            .setText(cm.getAbsString (0, "UTC_MINUTE_DIFF"));
		_startDate_txt              .setText(cm.getAbsString (0, "START_TIME"));
		_activation_txt             .setText(cm.getAbsString (0, "DB_ACTIVATION_TIME"));
		_dbStatus_txt               .setText(cm.getAbsString (0, "DB_STATUS"));
		_activationState_txt        .setText(cm.getAbsString (0, "DB_ACTIVATION_STATE"));
		_hadrRole_txt               .setText(cm.getAbsString (0, "HADR_ROLE"));
		_lastBackup_txt             .setText(cm.getAbsString (0, "LAST_BACKUP"));

		_connectionsAbs_txt         .setText(cm.getAbsString (0, "APPLS_CUR_CONS"));
		_connectionsDiff_txt        .setText(cm.getDiffString(0, "APPLS_CUR_CONS"));
		_executing_txt              .setText(cm.getAbsString (0, "APPLS_IN_DB2"));
		_connectionsTop_txt         .setText(cm.getAbsString (0, "CONNECTIONS_TOP"));

		_oldestOpenTranThreshold_txt.setText(cm.getAbsString (0, "OLDEST_OPEN_TRAN_IN_SEC_TH"));
		_oldestOpenTranSec_txt      .setText(cm.getAbsString (0, "OLDEST_OPEN_TRAN_IN_SEC"));
		_oldestOpenTranTime_txt     .setText(cm.getAbsString (0, "OLDEST_OPEN_TRAN_TIME"));
		String oldestHandle = cm.getAbsString(0, "OLDEST_OPEN_TRAN_APPL_HANDLE");
		_oldestOpenTranWho_txt      .setText(oldestHandle.isEmpty() ? "" : "handle=" + oldestHandle + ", " + cm.getAbsString(0, "OLDEST_OPEN_TRAN_USER") + "@" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_HOST") + ", " + cm.getAbsString(0, "OLDEST_OPEN_TRAN_APPL_NAME") + ", " + cm.getAbsString(0, "OLDEST_OPEN_TRAN_STATE"));
		_oldestOpenTranWho_txt      .setCaretPosition(0);
		_openTranCount_txt          .setText(cm.getAbsString (0, "OPEN_TRAN_COUNT"));
		_logUsedPct_txt             .setText(cm.getAbsString (0, "LOG_USED_PCT"));
		_blockingWaitThreshold_txt  .setText(cm.getAbsString (0, "BLOCKING_WAIT_IN_SEC_TH"));
		_lockWaiters_txt            .setText(cm.getAbsString (0, "LOCK_WAITERS"));
		_lockBlockers_txt           .setText(cm.getAbsString (0, "LOCK_BLOCKERS"));
		_maxLockWaitSec_txt         .setText(cm.getAbsString (0, "MAX_LOCK_WAIT_SEC"));
		_locksHeld_txt              .setText(cm.getAbsString (0, "NUM_LOCKS_HELD"));

		_bpHitPctAbs_txt            .setText(cm.getAbsString (0, "BP_HIT_PCT"));
		_bpHitPctDiff_txt           .setText(cm.getDiffString(0, "BP_HIT_PCT"));
		_pkgCacheHitPctAbs_txt      .setText(cm.getAbsString (0, "PKG_CACHE_HIT_PCT"));
		_pkgCacheHitPctDiff_txt     .setText(cm.getDiffString(0, "PKG_CACHE_HIT_PCT"));

		// Counters: Abs, Diff, Rate
		for (CounterRow row : _counterRows)
		{
			row.absTxt .setText(cm.getAbsString (0, row.colName));
			row.diffTxt.setText(cm.getDiffString(0, row.colName));
			row.rateTxt.setText(cm.getRateString(0, row.colName));
		}
	}

	@Override
	public void resetGoToTabSettings(String tabName)
	{
	}

	@Override
	public synchronized void clearSummaryData()
	{
		setWatermark();

		for (JTextField f : new JTextField[] {
				_localServerName_txt, _host_txt, _version_txt, _instance_txt, _database_txt, _lastSampleTime_txt, _utcTimeDiff_txt, _startDate_txt,
				_activation_txt, _dbStatus_txt, _activationState_txt, _hadrRole_txt, _lastBackup_txt,
				_connectionsAbs_txt, _connectionsDiff_txt, _executing_txt, _connectionsTop_txt,
				_oldestOpenTranThreshold_txt, _oldestOpenTranSec_txt, _oldestOpenTranTime_txt, _oldestOpenTranWho_txt, _openTranCount_txt, _logUsedPct_txt,
				_blockingWaitThreshold_txt, _lockWaiters_txt, _lockBlockers_txt, _maxLockWaitSec_txt, _locksHeld_txt,
				_bpHitPctAbs_txt, _bpHitPctDiff_txt, _pkgCacheHitPctAbs_txt, _pkgCacheHitPctDiff_txt })
		{
			f.setText("");
		}

		for (CounterRow row : _counterRows)
		{
			row.absTxt .setText("");
			row.diffTxt.setText("");
			row.rateTxt.setText("");
		}
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
			setWatermarkText("Disconnect at: \n" + dateStr);
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
		private Graphics2D  g       = null;
		private Rectangle   r       = null;

		@Override
		public void paint(Graphics graphics)
		{
			if (_textBr == null || _textBr != null && _textBr.length == 0)
				return;

			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
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
