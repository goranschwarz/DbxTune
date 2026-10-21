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
package com.dbxtune.cm.mysql.gui;

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
import com.dbxtune.cm.mysql.CmSummary;
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
 * The Summary panel (left side of the GUI) for MySqlTune: the fields of CmSummary.
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
	private JLabel             _edition_lbl               = new JLabel("Edition");
	private JTextField         _edition_txt               = new JTextField();
	private JLabel             _lastSampleTime_lbl        = new JLabel("Sample time");
	private JTextField         _lastSampleTime_txt        = new JTextField();
	private JLabel             _utcTimeDiff_lbl           = new JLabel("UTC Time Diff");
	private JTextField         _utcTimeDiff_txt           = new JTextField();
	private JLabel             _startDate_lbl             = new JLabel("Start date");
	private JTextField         _startDate_txt             = new JTextField();
	private JLabel             _daysRunning_lbl           = new JLabel("Days running");
	private JTextField         _daysRunning_txt           = new JTextField();
	private JLabel             _readOnly_lbl              = new JLabel("Read Only");
	private JTextField         _readOnly_txt              = new JTextField();
	private JLabel             _replica_lbl               = new JLabel("Replica");
	private JTextField         _replica_txt               = new JTextField();

	// Connections
	private JLabel             _connections_lbl           = new JLabel("Connections");
	private JTextField         _connectionsAbs_txt        = new JTextField();
	private JTextField         _connectionsDiff_txt       = new JTextField();
	private JLabel             _threadsRunning_lbl        = new JLabel("Running");
	private JTextField         _threadsRunning_txt        = new JTextField();
	private JLabel             _maxConnections_lbl        = new JLabel("Max Used/Allowed");
	private JTextField         _maxUsedConnections_txt    = new JTextField();
	private JTextField         _maxConnections_txt        = new JTextField();

	// Transactions and locks
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
	private JLabel             _historyList_lbl           = new JLabel("History List Length");
	private JTextField         _historyList_txt           = new JTextField();
	private JLabel             _blockingWaitThreshold_lbl = new JLabel("Lock Wait Threshold");
	private JTextField         _blockingWaitThreshold_txt = new JTextField();
	private JLabel             _blockingWaitCount_lbl     = new JLabel("Lock Waits (Row/Meta)");
	private JTextField         _blockingWaitCount_txt     = new JTextField();
	private JTextField         _rowLockWaitCount_txt      = new JTextField();
	private JTextField         _mdlWaitCount_txt          = new JTextField();
	private JLabel             _blockingWaitMaxSec_lbl    = new JLabel("Lock Wait Max Sec");
	private JTextField         _blockingWaitMaxSec_txt    = new JTextField();

	// Buffer pool
	private JLabel             _bpHitPct_lbl              = new JLabel("BP Hit % (start/sample)");
	private JTextField         _bpHitPctAbs_txt           = new JTextField();
	private JTextField         _bpHitPctDiff_txt          = new JTextField();
	private JLabel             _bpPages_lbl               = new JLabel("BP Pages Free/Dirty/Total");
	private JTextField         _bpPagesFree_txt           = new JTextField();
	private JTextField         _bpPagesDirty_txt          = new JTextField();
	private JTextField         _bpPagesTotal_txt          = new JTextField();

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

		init(_localServerName_lbl,         "The name we used when " + Version.getAppName() + " connected to the server.", _localServerName_txt);
		init(_host_lbl,                    "The host where MySQL is running. @@hostname", _host_txt);
		init(_version_lbl,                 "The MySQL version. version()", _version_txt);
		init(_edition_lbl,                 "The MySQL edition/distribution. @@version_comment", _edition_txt);
		init(_lastSampleTime_lbl,          "Time of last sample.", _lastSampleTime_txt);
		init(_utcTimeDiff_lbl,             "UTC Time Difference in minutes (positive east of UK, negative west of UK).", _utcTimeDiff_txt);
		init(_startDate_lbl,               "Date and time that MySQL was started.", _startDate_txt);
		init(_daysRunning_lbl,             "Number of days since MySQL was started.", _daysRunning_txt);
		init(_readOnly_lbl,                "Is the server read only? @@read_only and @@super_read_only", _readOnly_txt);
		init(_replica_lbl,                 "Is this server a replica? (see the tab 'Replication')", _replica_txt);

		init(_connections_lbl,             "Connections right now, and the change since the last sample. Status 'Threads_connected'", _connectionsAbs_txt, _connectionsDiff_txt);
		init(_threadsRunning_lbl,          "Connections that are working (not sleeping) right now. Status 'Threads_running'", _threadsRunning_txt);
		init(_maxConnections_lbl,          "The most connections at the same time since the server started, and @@max_connections.", _maxUsedConnections_txt, _maxConnections_txt);

		init(_oldestOpenTranThreshold_lbl, "The threshold (in seconds) of the alarm 'oldestOpenTranInSec'.", _oldestOpenTranThreshold_txt);
		init(_oldestOpenTranSec_lbl,       "How long the oldest open transaction has been open, in seconds. 0 = no open transaction.", _oldestOpenTranSec_txt);
		init(_oldestOpenTranTime_lbl,      "When the oldest open transaction started.", _oldestOpenTranTime_txt);
		init(_oldestOpenTranWho_lbl,       "Who has the oldest open transaction: pid, user@host and the state of the transaction.", _oldestOpenTranWho_txt);
		init(_openTranCount_lbl,           "Open InnoDB transactions right now (also a SELECT that is running).", _openTranCount_txt);
		init(_historyList_lbl,             "InnoDB history list length: old row versions that purge has not removed yet. It grows when a transaction is open for a long time.", _historyList_txt);
		init(_blockingWaitThreshold_lbl,   "The threshold (in seconds) of the alarm 'BlockingLockCount'.", _blockingWaitThreshold_txt);
		init(_blockingWaitCount_lbl,       "Sessions that waits for a lock right now: total, row locks, metadata locks. (see the tab 'Active Statements' for who blocks who)", _blockingWaitCount_txt, _rowLockWaitCount_txt, _mdlWaitCount_txt);
		init(_blockingWaitMaxSec_lbl,      "The longest time a session has waited for a lock right now, in seconds.", _blockingWaitMaxSec_txt);

		init(_bpHitPct_lbl,                "Buffer pool hit ratio in percent: since the server started, and in the last sample.", _bpHitPctAbs_txt, _bpHitPctDiff_txt);
		init(_bpPages_lbl,                 "Buffer pool pages: free, dirty (changed, not yet written to disk) and total.", _bpPagesFree_txt, _bpPagesDirty_txt, _bpPagesTotal_txt);
		_connectionsDiff_txt.setForeground(Color.BLUE);
		_bpHitPctDiff_txt   .setForeground(Color.BLUE);

		// Counters: Abs, Diff and Rate
		_counterRows.add(new CounterRow("QUESTIONS",               "Questions",           "Statements sent by the clients. Status 'Questions'"));
		_counterRows.add(new CounterRow("COM_SELECT",              "Select",              "SELECT statements."));
		_counterRows.add(new CounterRow("COM_INSERT",              "Insert",              "INSERT statements."));
		_counterRows.add(new CounterRow("COM_UPDATE",              "Update",              "UPDATE statements."));
		_counterRows.add(new CounterRow("COM_DELETE",              "Delete",              "DELETE statements."));
		_counterRows.add(new CounterRow("HANDLER_COMMIT",          "Commits",             "Commits inside the server, also autocommit. Status 'Handler_commit'"));
		_counterRows.add(new CounterRow("HANDLER_ROLLBACK",        "Rollbacks",           "Rollbacks inside the server. Status 'Handler_rollback'"));
		_counterRows.add(new CounterRow("ROWS_READ",               "Rows Read",           "Rows read from InnoDB tables."));
		_counterRows.add(new CounterRow("ROWS_INSERTED",           "Rows Inserted",       "Rows inserted in InnoDB tables."));
		_counterRows.add(new CounterRow("ROWS_UPDATED",            "Rows Updated",        "Rows updated in InnoDB tables."));
		_counterRows.add(new CounterRow("ROWS_DELETED",            "Rows Deleted",        "Rows deleted from InnoDB tables."));
		_counterRows.add(new CounterRow("BP_READ_REQUESTS",        "Logical Reads",       "Pages read from the buffer pool."));
		_counterRows.add(new CounterRow("BP_READS",                "Physical Reads",      "Pages that had to be read from disk."));
		_counterRows.add(new CounterRow("BP_PAGES_FLUSHED",        "Pages Flushed",       "Pages written from the buffer pool to disk."));
		_counterRows.add(new CounterRow("REDO_BYTES",              "Redo Bytes",          "Bytes written to the redo log."));
		_counterRows.add(new CounterRow("ROW_LOCK_WAITS",          "Row Lock Waits",      "Times a row lock had to be waited for."));
		_counterRows.add(new CounterRow("DEADLOCKS",               "Deadlocks",           "Deadlocks."));
		_counterRows.add(new CounterRow("SLOW_QUERIES",            "Slow Queries",        "Statements that took longer than long_query_time."));
		_counterRows.add(new CounterRow("CREATED_TMP_DISK_TABLES", "Tmp Disk Tables",     "Internal temporary tables that had to be written to disk."));
		_counterRows.add(new CounterRow("SELECT_FULL_JOIN",        "Full Joins",          "Joins without an index."));
		_counterRows.add(new CounterRow("CONNECTIONS",             "Connect Attempts",    "Connection attempts (successful or not)."));
		_counterRows.add(new CounterRow("ABORTED_CONNECTS",        "Failed Connects",     "Failed attempts to connect (wrong password, no privileges, timeout...)."));
		_counterRows.add(new CounterRow("CONN_ERRORS_MAX_CONN",    "Refused, Max Conn",   "Connections that were refused because max_connections was reached."));

		//--- Server
		panel.add(_localServerName_lbl,         "");
		panel.add(_localServerName_txt,         "growx, wrap");
		panel.add(_host_lbl,                    "");
		panel.add(_host_txt,                    "growx, wrap");
		panel.add(_version_lbl,                 "");
		panel.add(_version_txt,                 "growx, wrap");
		panel.add(_edition_lbl,                 "");
		panel.add(_edition_txt,                 "growx, wrap");
		panel.add(_lastSampleTime_lbl,          "");
		panel.add(_lastSampleTime_txt,          "growx, wrap");
		panel.add(_utcTimeDiff_lbl,             "");
		panel.add(_utcTimeDiff_txt,             "growx, wrap");
		panel.add(_startDate_lbl,               "");
		panel.add(_startDate_txt,               "growx, wrap");
		panel.add(_daysRunning_lbl,             "");
		panel.add(_daysRunning_txt,             "growx, wrap");
		panel.add(_readOnly_lbl,                "");
		panel.add(_readOnly_txt,                "growx, wrap");
		panel.add(_replica_lbl,                 "");
		panel.add(_replica_txt,                 "growx, wrap 20");

		//--- Connections
		panel.add(_connections_lbl,             "");
		panel.add(_connectionsAbs_txt,          "growx, split");
		panel.add(_connectionsDiff_txt,         "growx, wrap");
		panel.add(_threadsRunning_lbl,          "");
		panel.add(_threadsRunning_txt,          "growx, wrap");
		panel.add(_maxConnections_lbl,          "");
		panel.add(_maxUsedConnections_txt,      "growx, split");
		panel.add(_maxConnections_txt,          "growx, wrap 20");

		//--- Transactions and locks
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
		panel.add(_historyList_lbl,             "");
		panel.add(_historyList_txt,             "growx, wrap 10");
		panel.add(_blockingWaitThreshold_lbl,   "");
		panel.add(_blockingWaitThreshold_txt,   "growx, wrap");
		panel.add(_blockingWaitCount_lbl,       "");
		panel.add(_blockingWaitCount_txt,       "growx, split");
		panel.add(_rowLockWaitCount_txt,        "growx, split");
		panel.add(_mdlWaitCount_txt,            "growx, wrap");
		panel.add(_blockingWaitMaxSec_lbl,      "");
		panel.add(_blockingWaitMaxSec_txt,      "growx, wrap 20");

		//--- Buffer pool
		panel.add(_bpHitPct_lbl,                "");
		panel.add(_bpHitPctAbs_txt,             "growx, split");
		panel.add(_bpHitPctDiff_txt,            "growx, wrap");
		panel.add(_bpPages_lbl,                 "");
		panel.add(_bpPagesFree_txt,             "growx, split");
		panel.add(_bpPagesDirty_txt,            "growx, split");
		panel.add(_bpPagesTotal_txt,            "growx, wrap 20");

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

		_host_txt                   .setText(cm.getAbsString (0, "host"));
		_version_txt                .setText(cm.getAbsString (0, "version"));
		_edition_txt                .setText(cm.getAbsString (0, "VERSION_COMMENT"));
		_lastSampleTime_txt         .setText(cm.getAbsString (0, "time_now"));
		_utcTimeDiff_txt            .setText(cm.getAbsString (0, "utc_minute_diff"));
		_startDate_txt              .setText(cm.getAbsString (0, "start_time"));
		_daysRunning_txt            .setText(cm.getAbsString (0, "DAYS_RUNNING"));

		boolean readOnly      = "1".equals(cm.getAbsString(0, "READ_ONLY"));
		boolean superReadOnly = "1".equals(cm.getAbsString(0, "SUPER_READ_ONLY"));
		_readOnly_txt               .setText(superReadOnly ? "Yes (super_read_only)" : readOnly ? "Yes (read_only)" : "No");

		String replicaChannels = cm.getAbsString(0, "REPLICA_CHANNELS");
		_replica_txt                .setText("0".equals(replicaChannels) || replicaChannels.isEmpty() ? "No" : "Yes, channels: " + replicaChannels);

		_connectionsAbs_txt         .setText(cm.getAbsString (0, "THREADS_CONNECTED"));
		_connectionsDiff_txt        .setText(cm.getDiffString(0, "THREADS_CONNECTED"));
		_threadsRunning_txt         .setText(cm.getAbsString (0, "THREADS_RUNNING"));
		_maxUsedConnections_txt     .setText(cm.getAbsString (0, "MAX_USED_CONNECTIONS"));
		_maxConnections_txt         .setText(cm.getAbsString (0, "MAX_CONNECTIONS"));

		_oldestOpenTranThreshold_txt.setText(cm.getAbsString (0, "OLDEST_OPEN_TRAN_IN_SEC_TH"));
		_oldestOpenTranSec_txt      .setText(cm.getAbsString (0, "OLDEST_OPEN_TRAN_IN_SEC"));
		_oldestOpenTranTime_txt     .setText(cm.getAbsString (0, "OLDEST_OPEN_TRAN_TIME"));
		String oldestPid = cm.getAbsString(0, "OLDEST_OPEN_TRAN_PID");
		_oldestOpenTranWho_txt      .setText(oldestPid.isEmpty() ? "" : "pid=" + oldestPid + ", " + cm.getAbsString(0, "OLDEST_OPEN_TRAN_USER") + "@" + cm.getAbsString(0, "OLDEST_OPEN_TRAN_HOST") + ", " + cm.getAbsString(0, "OLDEST_OPEN_TRAN_STATE"));
		_oldestOpenTranWho_txt      .setCaretPosition(0);
		_openTranCount_txt          .setText(cm.getAbsString (0, "OPEN_TRAN_COUNT"));
		_historyList_txt            .setText(cm.getAbsString (0, "HISTORY_LIST_LENGTH"));
		_blockingWaitThreshold_txt  .setText(cm.getAbsString (0, "BLOCKING_WAIT_IN_SEC_TH"));
		_blockingWaitCount_txt      .setText(cm.getAbsString (0, "COUNT_BLOCKING_WAIT"));
		_rowLockWaitCount_txt       .setText(cm.getAbsString (0, "COUNT_ROW_LOCK_WAIT"));
		_mdlWaitCount_txt           .setText(cm.getAbsString (0, "COUNT_MDL_WAIT"));
		_blockingWaitMaxSec_txt     .setText(cm.getAbsString (0, "MAX_BLOCKING_WAIT_IN_SEC"));

		_bpHitPctAbs_txt            .setText(cm.getAbsString (0, "BP_HIT_PCT"));
		_bpHitPctDiff_txt           .setText(cm.getDiffString(0, "BP_HIT_PCT"));
		_bpPagesFree_txt            .setText(cm.getAbsString (0, "BP_PAGES_FREE"));
		_bpPagesDirty_txt           .setText(cm.getAbsString (0, "BP_PAGES_DIRTY"));
		_bpPagesTotal_txt           .setText(cm.getAbsString (0, "BP_PAGES_TOTAL"));

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
				_localServerName_txt, _host_txt, _version_txt, _edition_txt, _lastSampleTime_txt, _utcTimeDiff_txt, _startDate_txt, _daysRunning_txt, _readOnly_txt, _replica_txt,
				_connectionsAbs_txt, _connectionsDiff_txt, _threadsRunning_txt, _maxUsedConnections_txt, _maxConnections_txt,
				_oldestOpenTranThreshold_txt, _oldestOpenTranSec_txt, _oldestOpenTranTime_txt, _oldestOpenTranWho_txt, _openTranCount_txt, _historyList_txt,
				_blockingWaitThreshold_txt, _blockingWaitCount_txt, _rowLockWaitCount_txt, _mdlWaitCount_txt, _blockingWaitMaxSec_txt,
				_bpHitPctAbs_txt, _bpHitPctDiff_txt, _bpPagesFree_txt, _bpPagesDirty_txt, _bpPagesTotal_txt })
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
