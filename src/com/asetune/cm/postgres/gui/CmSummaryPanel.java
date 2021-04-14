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
package com.asetune.cm.postgres.gui;

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
import com.asetune.cm.postgres.CmSummary;
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

	
	private JTextField       _oldestRunningXactAge_txt     = new JTextField();
	private JLabel           _oldestRunningXactAge_lbl     = new JLabel();
	
	private JTextField       _oldestPreparedXactAge_txt    = new JTextField();
	private JLabel           _oldestPreparedXactAge_lbl    = new JLabel();
	
	private JTextField       _oldestReplicationSlotAge_txt = new JTextField();
	private JLabel           _oldestReplicationSlotAge_lbl = new JLabel();
	
	private JTextField       _oldestReplicaXactAge_txt     = new JTextField();
	private JLabel           _oldestReplicaXactAge_lbl     = new JLabel();
	
	
	
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
		_oldestRunningXactAge_lbl     .setText("Oldest Running Xact Age");
		_oldestRunningXactAge_lbl     .setToolTipText(tooltip);
		_oldestRunningXactAge_txt     .setToolTipText(tooltip);
		_oldestRunningXactAge_txt     .setEditable(false);

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(transaction)) FROM pg_prepared_xacts");
		_oldestPreparedXactAge_lbl    .setText("Oldest Prepared Xact Age");
		_oldestPreparedXactAge_lbl    .setToolTipText(tooltip);
		_oldestPreparedXactAge_txt    .setToolTipText(tooltip);
		_oldestPreparedXactAge_txt    .setEditable(false);

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(xmin)) FROM pg_replication_slots");
		_oldestReplicationSlotAge_lbl .setText("Oldest Replication Slot Age");
		_oldestReplicationSlotAge_lbl .setToolTipText(tooltip);
		_oldestReplicationSlotAge_txt .setToolTipText(tooltip);
		_oldestReplicationSlotAge_txt .setEditable(false);

		tooltip = baseTooltipForOldestXxx.replace("${REPLACE_THIS_WITH_SQL}", "SELECT max(age(backend_xmin)) FROM pg_stat_replication");
		_oldestReplicaXactAge_lbl     .setText("Oldest Replica Xact Age");
		_oldestReplicaXactAge_lbl     .setToolTipText(tooltip);
		_oldestReplicaXactAge_txt     .setToolTipText(tooltip);
		_oldestReplicaXactAge_txt     .setEditable(false);

		
		
		
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


		panel.add(_oldestRunningXactAge_lbl,     "");
		panel.add(_oldestRunningXactAge_txt,     "growx, wrap");

		panel.add(_oldestPreparedXactAge_lbl,    "");
		panel.add(_oldestPreparedXactAge_txt,    "growx, wrap");

		panel.add(_oldestReplicationSlotAge_lbl, "");
		panel.add(_oldestReplicationSlotAge_txt, "growx, wrap");

		panel.add(_oldestReplicaXactAge_lbl,     "");
		panel.add(_oldestReplicaXactAge_txt,     "growx, wrap");

		
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
		
		_oldestRunningXactAge_txt     .setText(cm.getAbsString (0, "oldest_running_xact_age"));
		_oldestPreparedXactAge_txt    .setText(cm.getAbsString (0, "oldest_prepared_xact_age"));
		_oldestReplicationSlotAge_txt .setText(cm.getAbsString (0, "oldest_replication_slot_age"));
		_oldestReplicaXactAge_txt     .setText(cm.getAbsString (0, "oldest_replica_xact_age"));

		// Set the background color to RED if we are in "recovery mode"
		boolean inRecoveryMode = "true".equalsIgnoreCase( cm.getAbsString (0, "in_recovery") );
		_inRecovery_txt.setBackground( inRecoveryMode ? Color.RED : _startDate_txt.getBackground() );
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


		_oldestRunningXactAge_txt     .setText("");
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
