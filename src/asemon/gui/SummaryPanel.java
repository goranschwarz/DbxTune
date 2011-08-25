/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package asemon.gui;

/**
 * <p>SummaryPanel</p>
 * <p>Asemon : Display main counters and trend graphs </p>
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import asemon.Asemon;
import asemon.CountersModel;
import asemon.gui.swing.AbstractComponentDecorator;
import asemon.utils.SwingUtils;



public class SummaryPanel
extends JPanel
{
	static Logger _logger = Logger.getLogger(SummaryPanel.class);

	private static final long serialVersionUID = 7555710440479350306L;


	private Watermark        _watermark;

	private JPanel           _serverInfoPanel;
	private JPanel           _dataPanel;
	private JPanel           _graphPanel;

	private JLabel           _title_lbl                    = new JLabel();

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
	private JTextField       _lockWaitThreshold_txt        = new JTextField();
	private JLabel           _lockWaitThreshold_lbl        = new JLabel();
	private JTextField       _lockWaits_txt                = new JTextField();
	private JTextField       _lockWaitsDiff_txt            = new JTextField();
	private JLabel           _lockWaits_lbl                = new JLabel();
	private JTextField       _maxRecovery_txt              = new JTextField();
	private JLabel           _maxRecovery_lbl              = new JLabel();

	private JLabel             _maxChartHistoryInMinutes_lbl = new JLabel();
	private SpinnerNumberModel _maxChartHistoryInMinutes_spm = new SpinnerNumberModel(TrendGraph.getChartMaxHistoryTimeInMinutes(), 1, 999, 1);
	private JSpinner           _maxChartHistoryInMinutes_sp  = new JSpinner(_maxChartHistoryInMinutes_spm);

//	private JLabel             lbNoGraphs                  = new JLabel("Graphs will NOT be showed UNTIL you connect.");
//	private JPanel	           panNoGraphs	               = new JPanel();
	
	private List               _graphList                   = new LinkedList();

	// implements singleton pattern
	private static SummaryPanel _instance = null;


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

		JScrollPane scroll = new JScrollPane(_graphPanel);
		JSplitPane  split  = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _dataPanel, scroll);
		add(split, BorderLayout.CENTER);

		// set a Decorator to the panel, that will show text: "Not Connected..." etc
		_watermark = new Watermark(scroll, "Not Connected...");

		// assign actions for the components
		initComponentActions();
		
		// load saved properties
		loadProps();

	}

	private void initComponentActions() 
	throws Exception
	{
		// HISTORY
		_maxChartHistoryInMinutes_spm.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent ce)
			{
				int minutes = _maxChartHistoryInMinutes_spm.getNumber().intValue();
				_logger.debug("maxChartHistoryInMinutes: " + minutes);
				setChartMaxHistoryTimeInMinutes(minutes);
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
		_serverInfoPanel = createServerInfoPanel();

		panel.add(_title_lbl,       "growx, wrap");
		panel.add(_serverInfoPanel, "growx, width 275lp, wrap"); //275 logical pixels

//		panel.setMinimumSize(new Dimension(300, 600));
//		panel.setPreferredSize(new Dimension(300, 600));

		return panel;
	}
	private JPanel createServerInfoPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Server Information", true);
		panel.setLayout(new MigLayout("", "[] [grow]", ""));

		String tooltip = "";

		_title_lbl.setFont(new java.awt.Font("Dialog", 1, 16));
		_title_lbl.setText("Summary panel");
		panel.add(_title_lbl, "wrap");

		tooltip = "The name we used when ASEmon connected to the server, meaning name in sql.ini or inetfaces ";
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
		
		panel.add(_lockWaitThreshold_lbl, "");
		panel.add(_lockWaitThreshold_txt, "growx, wrap");
		
		panel.add(_lockWaits_lbl,         "");
		panel.add(_lockWaits_txt,         "growx, split");
		panel.add(_lockWaitsDiff_txt,     "growx, wrap");
		
		panel.add(_maxRecovery_lbl,       "");
		panel.add(_maxRecovery_txt,       "growx, wrap");
		
		return panel;
	}
	
	private JPanel createGraphPanel() 
	{
		JPanel panel = SwingUtils.createPanel("Graphs", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));
//		panel.setLayout(new BorderLayout());
//		panel.setLayout(new VerticalFlowLayout());

//		_maxChartHistoryInMinutes_lbl.setText("Trends Graph History in Minutes");
//		JPanel tmp = new JPanel();
//		tmp.add( _maxChartHistoryInMinutes_lbl );
//		tmp.add( _maxChartHistoryInMinutes_sp );
//		panel.add(tmp);
		
		_maxChartHistoryInMinutes_lbl.setText("Trends Graph History in Minutes");

		panel.add(_maxChartHistoryInMinutes_lbl, "gaptop 10, center, split");
		panel.add(_maxChartHistoryInMinutes_sp , "wrap 10"); // the label and spinner needs to be in the same row...
		
		String tooltip = "";
		panel.setToolTipText(tooltip);
		
		return panel;
	}

	
	
	private void setChartMaxHistoryTimeInMinutes(int minutes)
	{
		Iterator iter = _graphList.iterator();
		while (iter.hasNext())
		{
			TrendGraph tg = (TrendGraph) iter.next();
			
			if (tg != null)
			{
				tg.setChartMaxHistoryTimeInMinutes(minutes);
			}
		}
	}

	
	public void clearGraph()
	{
		Iterator iter = _graphList.iterator();
		while (iter.hasNext())
		{
			TrendGraph tg = (TrendGraph) iter.next();
			
			if (tg != null)
			{
				tg.clearGraph();
			}
		}		
	}

	public void addTrendGraph(TrendGraph tg)
	{
		_graphList.add(tg);

		// Set the Graph history time...
		Integer minutes = (Integer) _maxChartHistoryInMinutes_spm.getValue();
		tg.setChartMaxHistoryTimeInMinutes( minutes.intValue() );

		// Take away the "THERE ARE NO GRAPHS AVAILABLE" note...
		//panNoGraphs.setVisible(false);

		// Add the graph to the right side panel
//		_graphPanel.add(tg.getPanel());//, "gapy 0, span, push, growx, height pref:pref:pref, wrap");
//		_graphPanel.add(tg.getPanel(), "push, grow, hidemode 3, hmax pref*3, wrap");
		_graphPanel.add(tg.getPanel(), "push, grow, hidemode 3, hmin 114px, wrap");
	}

	public void setLocalServerName(String name) 
	{ 
		_localServerName_txt.setText(name); 
		_localServerName_txt.setCaretPosition(0);
	}

	public String getLocalServerName() { return _localServerName_txt.getText(); }
	public String getCountersCleared() { return _countersCleared_txt.getText(); }
//	public String get() { return _.getText(); }
	
	public void setSummaryData(CountersModel cm)
	{
		setWatermark();

//		_localServerName_txt  .setText();
		_atAtServerName_txt   .setText(cm.getAbsString(0, "atAtServerName"));
		_listeners_txt        .setText(cm.getAbsString(0, "NetworkAddressInfo"));
		_aseVersion_txt       .setText(cm.getAbsString(0, "aseVersion").replaceFirst("Adaptive Server Enterprise/", ""));
		_lastSampleTime_txt   .setText(cm.getAbsString(0, "timeIsNow"));

		_startDate_txt        .setText(cm.getAbsString(0, "StartDate"));
		_daysRunning_txt      .setText(cm.getAbsString(0, "DaysRunning"));
		_countersCleared_txt  .setText(cm.getAbsString(0, "CountersCleared"));
		_checkPoints_txt      .setText(cm.getAbsString(0, "CheckPoints"));
		_numDeadlocks_txt     .setText(cm.getAbsString(0, "NumDeadlocks"));
		_numDeadlocksDiff_txt .setText(cm.getDiffString(0,"NumDeadlocks"));
		_diagnosticDumps_txt  .setText(cm.getAbsString(0, "DiagnosticDumps"));
		_connections_txt      .setText(cm.getAbsString(0, "Connections"));
		_connectionsDiff_txt  .setText(cm.getDiffString(0,"Connections"));
		_lockWaitThreshold_txt.setText(cm.getAbsString(0, "LockWaitThreshold"));
		_lockWaits_txt        .setText(cm.getAbsString(0, "LockWaits"));
		_lockWaitsDiff_txt    .setText(cm.getDiffString(0,"LockWaits"));
		_maxRecovery_txt      .setText(cm.getAbsString(0, "MaxRecovery"));
	}

	public synchronized void clearSummaryData()
	{
		setWatermark();

		_localServerName_txt.setText("");

		_atAtServerName_txt   .setText("");
		_listeners_txt        .setText("");
		_aseVersion_txt       .setText("");
		_lastSampleTime_txt   .setText("");

		_startDate_txt        .setText("");
		_daysRunning_txt      .setText("");
		_countersCleared_txt  .setText("");
		_checkPoints_txt      .setText("");
		_numDeadlocks_txt     .setText("");
		_diagnosticDumps_txt  .setText("");
		_connections_txt      .setText("");
		_lockWaitThreshold_txt.setText("");
		_lockWaits_txt        .setText("");
		_maxRecovery_txt      .setText("");
	}

	private void saveProps()
  	{
		Asemon.getSaveProps().setProperty("graph.history",    _maxChartHistoryInMinutes_spm.getNumber().toString() );

		// Done when the system exits
		//AsemonSaveProps.getInstance().save();
  	}

  	private void loadProps()
  	{
		// Do this at the end, since it triggers the saveProps()
		int hist = Asemon.getSaveProps().getIntProperty("graph.history", -1);
		if (hist != -1)
		{
			Integer minutes = new Integer(hist);
			_maxChartHistoryInMinutes_spm.setValue( minutes );

			setChartMaxHistoryTimeInMinutes( minutes.intValue() );
		}
  	}

	public void refreshHistoryTimeInMinutes()
	{
		Integer minutes = (Integer) _maxChartHistoryInMinutes_spm.getValue();

		setChartMaxHistoryTimeInMinutes( minutes.intValue() );
	}

	public void setWatermark()
	{
		if ( ! MainFrame.isMonConnected() )
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
