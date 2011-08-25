/**
 */

package asemon.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;

import asemon.Asemon;
import asemon.CountersModel;
import asemon.GetCounters;
import asemon.MonTablesDictionary;
import asemon.ProcessDetailFrame;
import asemon.Version;
import asemon.gui.swing.GTabbedPane;
import asemon.gui.wizard.WizardOffline;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Memory;
import asemon.utils.SwingUtils;

public class MainFrame
    extends JFrame
    implements ActionListener, ChangeListener
{
	private static final long    serialVersionUID = 8984251025337127843L;
	private static Logger        _logger          = Logger.getLogger(MainFrame.class);

	//-------------------------------------------------
	// STATUS fields
	public static final int     ST_CONNECT                 = 1;
	public static final int     ST_DISCONNECT              = 2;
	public static final int     ST_STATUS_FIELD            = 3;
	public static final int     ST_MEMORY                  = 4;

	private static final String ST_DEFAULT_STATUS_FIELD    = "Not Connected";
	private static final String ST_DEFAULT_SERVER_NAME     = "ASENAME (host:port)";
	private static final String ST_DEFAULT_SERVER_LISTENERS= "ASE Server listens on address";

	//-------------------------------------------------
	// Actions
	public static final String ACTION_CONNECT             = "CONNECT";
	public static final String ACTION_DISCONNECT          = "DISCONNECT";
	public static final String ACTION_EXIT                = "EXIT";

	public static final String ACTION_OPEN_LOG_VIEW       = "OPEN_LOG_VIEW";
	public static final String ACTION_OPEN_REFRESH_RATE   = "OPEN_REFRESH_RATE";
	public static final String ACTION_OPEN_ASE_CONFIG_MON = "OPEN_ASE_CONFIG_MON";

	public static final String ACTION_OPEN_CAPTURE_SQL    = "OPEN_CAPTURE_SQL";
	public static final String ACTION_OPEN_SQL_QUERY_WIN  = "OPEN_SQL_QUERY_WIN";
	public static final String ACTION_OPEN_LOCK_TOOL      = "OPEN_LOCK_TOOL";
	public static final String ACTION_OPEN_OFFLINE        = "OPEN_OFFLINE";

	public static final String ACTION_OPEN_ABOUT          = "OPEN_ABOUT";


	//-------------------------------------------------
	// Menus / toolbar

	private JToolBar            _toolbar          = new JToolBar();

	private JMenuBar            _main_mb          = new JMenuBar();

	// File
	private JMenu               _file_m           = new JMenu("File");
	private JMenuItem           _connect_mi       = new JMenuItem("Connect");
	private JMenuItem           _disconnect_mi    = new JMenuItem("Disconnect");
	private JMenuItem           _exit_mi          = new JMenuItem("Exit");

	// View
	private JMenu               _view_m           = new JMenu("View");
	private JMenuItem           _logView_mi       = new JMenuItem("Log Window");
	private JMenuItem           _refreshRate_mi   = new JMenuItem("Refresh Rate");
	private JMenuItem           _aseMonConf_mi    = new JMenuItem("Config ASE for Monitoring");
	private static JMenu        _graphs_m         = new JMenu("Active Graphs");
	
	// Tools
	private JMenu               _tools_m          = new JMenu("Tools");
	private JMenuItem           _captureSql_mi    = new JMenuItem("Capture SQL");
	private JMenuItem           _sqlQuery_mi      = new JMenuItem("SQL Query Window");
	private JMenuItem           _lockTool_mi      = new JMenuItem("Lock Tool (NOT YET IMPLEMENTED)");
	private JMenuItem           _createOffline_mi = new JMenuItem("Create Offline Session (NOT YET FINNISHED)");

	// Help
	private JMenu               _help_m           = new JMenu("Help");
	private JMenuItem           _about_mi         = new JMenuItem("About");

	private static GTabbedPane  _mainTabbedPane   = new GTabbedPane();

	//-------------------------------------------------
	// STATUS Panel
	private JPanel                    _statusPanel               = new JPanel();
	private static JLabel             _statusStatus              = new JLabel(ST_DEFAULT_STATUS_FIELD);
	private static JLabel             _statusServerName          = new JLabel(ST_DEFAULT_SERVER_NAME);
	private static JLabel             _statusServerListeners     = new JLabel(ST_DEFAULT_SERVER_LISTENERS);
	private static JLabel             _statusMemory              = new JLabel("JVM Memory Usage");

	//-------------------------------------------------
	// Other members
	private static JFrame             _instance                  = null;
	private        Log4jViewer        _logView                   = null;
	private static Connection         _conn                      = null;
	private static int                _refreshInterval            = 5;
	private static int                _refreshNoGuiInterval       = 60;

	/** Keep a list of all TabularCntrPanel that you have initialized */
	private static Map                _TcpMap                     = new HashMap();
	private static SummaryPanel       _summaryPanel               = new SummaryPanel();
	private static TabularCntrPanel   _currentPanel               = null;

	//-------------------------------------------------

	public static int getRefreshInterval()      { return _refreshInterval; }
	public static int getRefreshIntervalNoGui() { return _refreshNoGuiInterval; }
	
	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	public MainFrame()
	{
		super();

		_instance = this;

		//enableEvents(AWTEvent.WINDOW_EVENT_MASK);

		initComponents();
		loadProps();

		// Calculate initial size
		pack();
		//setSize(new Dimension(747, 536));
	}
	/*---------------------------------------------------
	** END: constructors
	**---------------------------------------------------
	*/

	
	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents() 
	{
		setTitle("Asemon");
		ImageIcon icon = SwingUtils.readImageIcon(Version.class, "images/asemon_icon.gif");
		if (icon != null)
			setIconImage(icon.getImage());

		JPanel contentPane = (JPanel) this.getContentPane();
		contentPane.setLayout(new BorderLayout());

		//--------------------------
		// MENU - Icons
		_connect_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/connect16.gif"));
		_disconnect_mi   .setIcon(SwingUtils.readImageIcon(Version.class, "images/disconnect16.gif"));
		_exit_mi         .setIcon(SwingUtils.readImageIcon(Version.class, "images/close.gif"));

		_logView_mi      .setIcon(SwingUtils.readImageIcon(Version.class, "images/log_viewer.gif"));
		_refreshRate_mi  .setIcon(SwingUtils.readImageIcon(Version.class, "images/refresh_rate.png"));
		_aseMonConf_mi   .setIcon(SwingUtils.readImageIcon(Version.class, "images/config_ase_mon.png"));
		_graphs_m        .setIcon(SwingUtils.readImageIcon(Version.class, "images/summary_tab.png"));
		
		_captureSql_mi   .setIcon(SwingUtils.readImageIcon(Version.class, "images/capture_sql_tool.gif"));
		_sqlQuery_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/sql_query_window.png"));
		_lockTool_mi     .setIcon(SwingUtils.readImageIcon(Version.class, "images/locktool16.gif"));
//		_createOffline_mi.setIcon(SwingUtils.readImageIcon(Version.class, "images/xxx.gif"));
		
		_about_mi        .setIcon(SwingUtils.readImageIcon(Version.class, "images/about.png"));

		
		//--------------------------
		// MENU - composition
		setJMenuBar(_main_mb);

		_main_mb.add(_file_m);
		_main_mb.add(_view_m);
		_main_mb.add(_tools_m);
		_main_mb.add(_help_m);

		_file_m.add(_connect_mi);
		_file_m.add(_disconnect_mi);
		_file_m.add(_exit_mi);

		_view_m.add(_logView_mi);
		_view_m.add(_refreshRate_mi);
		_view_m.add(_aseMonConf_mi);
		_view_m.add(_graphs_m);
		
		_tools_m.add(_captureSql_mi);
		_tools_m.add(_sqlQuery_mi);
		_tools_m.add(_lockTool_mi);
		_tools_m.add(_createOffline_mi);

		_help_m.add(_about_mi);

		//--------------------------
		// MENU - Actions
		_connect_mi      .setActionCommand(ACTION_CONNECT);
		_disconnect_mi   .setActionCommand(ACTION_DISCONNECT);
		_exit_mi         .setActionCommand(ACTION_EXIT);

		_logView_mi      .setActionCommand(ACTION_OPEN_LOG_VIEW);
		_refreshRate_mi  .setActionCommand(ACTION_OPEN_REFRESH_RATE);
		_aseMonConf_mi   .setActionCommand(ACTION_OPEN_ASE_CONFIG_MON);

		_captureSql_mi   .setActionCommand(ACTION_OPEN_CAPTURE_SQL);
		_sqlQuery_mi     .setActionCommand(ACTION_OPEN_SQL_QUERY_WIN);
		_lockTool_mi     .setActionCommand(ACTION_OPEN_LOCK_TOOL);
		_createOffline_mi.setActionCommand(ACTION_OPEN_OFFLINE);
		
		_about_mi        .setActionCommand(ACTION_OPEN_ABOUT);

		// And the action listener
		_connect_mi      .addActionListener(this);
		_disconnect_mi   .addActionListener(this);
		_exit_mi         .addActionListener(this);

		_logView_mi      .addActionListener(this);
		_refreshRate_mi  .addActionListener(this);
		_aseMonConf_mi   .addActionListener(this);

		_captureSql_mi   .addActionListener(this);
		_sqlQuery_mi     .addActionListener(this);
		_lockTool_mi     .addActionListener(this);
		_createOffline_mi.addActionListener(this);
		
		_about_mi        .addActionListener(this);
		
		//--------------------------
		// TOOLBAR
		JButton connect    = SwingUtils.makeToolbarButton(Version.class, "connect16",    ACTION_CONNECT,    this, "Connect to a ASE",         "Connect");
		JButton disConnect = SwingUtils.makeToolbarButton(Version.class, "disconnect16", ACTION_DISCONNECT, this, "Close the ASE Connection", "Disconnect");

		_toolbar.add(connect);
		_toolbar.add(disConnect);

		
		//--------------------------
		// STATUS PANEL
		_statusStatus         .setToolTipText("What are we doing or waiting for.");
		_statusServerName     .setToolTipText("<html>The local name of the ASE Server, as named in the interfaces or sql.ini file.<BR>Also show the HOST:PORT, which we are connected to.</html>");
		_statusServerListeners.setToolTipText("<html>This is the network listeners the ASE Servers listens to.<BR>This is good to see if we connect via SSH tunnels or other proxy functionality.<br>The format is netlibdriver: HOST PORT, next entry...</html>");
		_statusMemory         .setToolTipText("How much memory does the JVM consume for the moment.");

		_statusPanel.setLayout(new MigLayout("insets 0 10 0 10")); // T L B R
		_statusPanel.add(_statusStatus,                           "gaptop 3, gapbottom 5, grow, push");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusServerName,                       "right");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusServerListeners,                  "right");
		_statusPanel.add(new JSeparator(SwingConstants.VERTICAL), "grow");
		_statusPanel.add(_statusMemory,          "right");

		
		//--------------------------
		// Layout
		contentPane.add(_toolbar,        BorderLayout.NORTH);
		contentPane.add(_mainTabbedPane, BorderLayout.CENTER);
		contentPane.add(_statusPanel,    BorderLayout.SOUTH);

		
		//--------------------------
		// Tab
		_mainTabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		_mainTabbedPane.addChangeListener(this);

		
		//--------------------------
		// Add Summary TAB
		Icon summaryIcon = SwingUtils.readImageIcon(Version.class, "images/summary_tab.png");
		_mainTabbedPane.addTab("Summary", summaryIcon, _summaryPanel, "Trend Graphs");
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/

	
		
	/*---------------------------------------------------
	** BEGIN: implementing ActionListener
	**---------------------------------------------------
	*/
	public void actionPerformed(ActionEvent e)
    {
//		Object source    = e.getSource();
		String actionCmd = e.getActionCommand();

		_logger.debug("ACTION '"+actionCmd+"'.");

		if (ACTION_CONNECT.equals(actionCmd))
			action_connect(e);

		if (ACTION_DISCONNECT.equals(actionCmd))
			action_disconnect(e);

		if (ACTION_EXIT.equals(actionCmd))
			action_exit(e);


		
		if (ACTION_OPEN_LOG_VIEW.equals(actionCmd))
		{
			if (_logView == null)
				_logView = new Log4jViewer(MainFrame.this);
			_logView.setVisible(true);
		}

		if (ACTION_OPEN_REFRESH_RATE.equals(actionCmd))
			action_refreshRate(e);

		if (ACTION_OPEN_ASE_CONFIG_MON.equals(actionCmd))
			AseMonitoringConfigDialog.showDialog(this, getMonConnection(), -1);

		if (ACTION_OPEN_CAPTURE_SQL.equals(actionCmd))
			new ProcessDetailFrame(-1);


		if (ACTION_OPEN_SQL_QUERY_WIN.equals(actionCmd))
		{
			try 
			{
				Connection conn = AseConnectionFactory.getConnection(null, "asemon-QueryWindow");
				QueryFrame qf = new QueryFrame(conn, true);
				qf.openTheWindow();
			}
			catch (Exception ex) 
			{
				JOptionPane.showMessageDialog(
					MainFrame.this, 
					"Problems open SQL Query Window\n" + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE);
			}
		}

		if (ACTION_OPEN_LOCK_TOOL.equals(actionCmd))
		{
			// TO BE IMPLEMENTED
		}

		if (ACTION_OPEN_OFFLINE.equals(actionCmd))
		{
			new WizardOffline();
		}
		
		if (ACTION_OPEN_ABOUT.equals(actionCmd))
			action_about(e);
    }
	/*---------------------------------------------------
	** END: implementing ActionListener
	**---------------------------------------------------
	*/

	
	/*---------------------------------------------------
	** BEGIN: implementing ChangeListener
	**---------------------------------------------------
	*/
	public void stateChanged(ChangeEvent e)
	{
		String currentTab = _mainTabbedPane.getTitleAt(_mainTabbedPane.getSelectedIndex());
		if (_logger.isDebugEnabled())
		{
			_logger.debug("state changed for pannel named '" + currentTab + "'.");
		}


		// LOOP all TabularCntrPanel to check which is the current one...
		// if it should be done
		Iterator iter = _TcpMap.values().iterator();
		while (iter.hasNext())
		{
			TabularCntrPanel tcp = (TabularCntrPanel) iter.next();

			if (currentTab.equals(tcp.getPanelName()))
			{
				_currentPanel = tcp;
				break; // no need to continue
			}
		}

		if (_currentPanel != null)
		{
			_currentPanel.setWatermark();
		}

		if ((_currentPanel != null) && (_conn != null) && (_currentPanel.getCounterModel() != null) && (!_currentPanel.getCounterModel().isDataInitialized()))
		{
			GetCounters.setWaitEvent("data to be initialization in the panel '"+_currentPanel.getPanelName()+"'...");
			//statusFld.setText("Waiting for data to be initialization in the panel '"+currentPanel.getPanelName()+"'...");
		}
	}
	/*---------------------------------------------------
	** END: implementing ChangeListener
	**---------------------------------------------------
	*/
	

	/*---------------------------------------------------
	** BEGIN: Helper methods for actions
	**---------------------------------------------------
	*/
	private void action_connect(ActionEvent e)
	{
		if (_conn != null)
		{
			SwingUtils.showInfoMessage(this, "ASE - connect", "Connection already opened");
			return;
		}
		_conn = ConnectionDialog.showConnectionDialog(this);
		if (_conn != null)
		{
			_summaryPanel.setLocalServerName(AseConnectionFactory.getServer());
			setStatus(ST_CONNECT);
			
			// Initilize the MonTablesDictionary
			// This will serv as a dictionary for ToolTip
			MonTablesDictionary.getInstance().initialize(_conn);
			GetCounters.initExtraMonTablesDictionary();
			
			GetCounters getCnt = Asemon.getCounterCollector();
			if (getCnt != null)
			{
				getCnt.startRefresh();
			}
		}
	}


	private void action_disconnect(ActionEvent e)
	{
		AseConnectionFactory.reset();
		terminateConnection();
	}


	private void action_exit(ActionEvent e)
	{
		saveProps();
		if (isMonConnected())
		{
			int answer = AseMonitoringConfigDialog.onExit(this, _conn);

			// This means that a "prompt" was raised to ask for "disable monitoring"
			// AND the user pressed CANCEL button
			if (answer > 0)
				return;
		}
		terminateConnection();
		System.exit(0);
	}


	private void action_refreshRate(ActionEvent e)
	{
		String key1 = "Refresh Rate";
		String key2 = "Refresh Rate (no-gui)";

		LinkedHashMap in = new LinkedHashMap();
		in.put(key1, Integer.toString(_refreshInterval));
		in.put(key2, Integer.toString(_refreshNoGuiInterval));

		Map results = ParameterDialog.showParameterDialog(this, "Refresh Interval", in);

		if (results != null)
		{
			_refreshInterval      = Integer.parseInt((String) results.get(key1));
			_refreshNoGuiInterval = Integer.parseInt((String) results.get(key2));

			saveProps();
		}
	}

	private void action_about(ActionEvent e)
	{
		AboutBox dlg = new AboutBox(this);
		Dimension dlgSize = dlg.getPreferredSize();
		Dimension frmSize = getSize();
		Point loc = getLocation();
		dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
		dlg.setModal(true);
		dlg.pack();
		//dlg.show();
		dlg.setVisible(true);
	}


	// Overridden so we can exit when window is closed
	protected void processWindowEvent(WindowEvent e)
	{
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING)
		{
			action_exit(null);
		}
	}

	/*---------------------------------------------------
	** END: Helper methods for actions
	**---------------------------------------------------
	*/




	/*---------------------------------------------------
	** BEGIN: public methods
	**---------------------------------------------------
	*/
	/**
	 * Stop monitoring and disconnect from the database.
	 */
	public static void terminateConnection()
	{
		// Stop the counter refresh thread
		GetCounters getCnt = Asemon.getCounterCollector();
		if (getCnt != null)
		{
			getCnt.stopRefresh();
			while (true)
			{
				if (!getCnt.isRefreshing())
				{
					getCnt.clearComponents();
					break;
				}
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException ex)
				{/* ignore */
				}
			}
		}

		// Close the database connection
		try
		{
			if (_conn != null)
			{
				_conn.close();
				if (_logger.isDebugEnabled())
				{
					_logger.debug("Connection closed");
				}
			}
		}
		catch (SQLException ev)
		{
			_logger.error("terminateConnection", ev);
		}
		_conn = null;
		
		// Update status fields
		setStatus(ST_DISCONNECT);

		// SET WATERMARK
		SummaryPanel.getInstance().setWatermark();

		Iterator iter = _TcpMap.values().iterator();
		while (iter.hasNext())
		{
			TabularCntrPanel tcp = (TabularCntrPanel) iter.next();
			tcp.setWatermark();
		}
	}

	/**
	 * Add a Component to the Tab
	 * @param tcp the component to add
	 */
	public static void addTcp(TabularCntrPanel tcp)
	{
		// We are probably in NO-GUI mode
		if ( ! Asemon.hasGUI() )
			return;

		_mainTabbedPane.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCounterModel().getDescription());
		_TcpMap.put(tcp.getPanelName(), tcp);
	}

	/**
	 * Add a "enable/disable" checkbox in the view menu
	 * @param mi The <code>JMenuItem</code> to add.
	 */
	public static void addGraphViewMenu(JMenuItem mi)
	{
		_graphs_m.add(mi);
	}

	/**
	 * Do we have a connection to the database?
	 * @return true or false
	 */
	public static boolean isMonConnected()
	{
		return _conn != null;
	}

	/**
	 * Set the <code>Connection</code> to use for monitoring.
	 */
	public static void setMonConnection(Connection conn)
	{
		_conn = conn;
	}

	/**
	 * Gets the <code>Connection</code> to the monitored server.
	 */
	public static Connection getMonConnection()
	{
		return _conn;
	}
	
	/**
	 * What tab is currently active?
	 * @return the Component within the tab
	 */
	public static Component getActiveTab()
	{
		return _mainTabbedPane.getSelectedComponent();
	}
	
	/**
	 * Clears fields in the SummaryPanel
	 */
	public static void clearSummaryData()
	{
		_summaryPanel.clearSummaryData();
	}

	/**
	 * Updates fields in the SummaryPanel
	 */
	public static void setSummaryData(CountersModel cm)
	{
		_summaryPanel.setSummaryData(cm);
	}



	/**
	 * Gets current values from the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @return a String with the information. If not found "" (space) will be returned
	 */
	public static String getStatus(int type)
	{
		if (type == ST_CONNECT)      return _statusServerName.getText();
		if (type == ST_DISCONNECT)   return _statusServerName.getText();
		if (type == ST_STATUS_FIELD) return _statusStatus.getText();
		if (type == ST_MEMORY)       return _statusMemory.getText();
		return "";
	}

	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 */
	public static void setStatus(int type)
	{
		setStatus(type, null);
	}
	/**
	 * Sets values in the status panel.
	 * @param type <code>ST_CONNECT, ST_DISCONNECT, ST_STATUS_FIELD, ST_MEMORY</code>
	 * @param param The actual string to set (this is only used for <code>ST_STATUS_FIELD</code>)
	 */
	public static void setStatus(int type, String param)
	{
		// CONNECT
		if (type == ST_CONNECT)
		{
			if (_conn != null)
			{
				_statusStatus    .setText("Just Connected...");
				_statusServerName.setText(
						AseConnectionFactory.getServer() + " (" +
						AseConnectionFactory.getHost()   + ":" +
						AseConnectionFactory.getPort()   + ")"
						);
				_statusServerListeners.setText(AseConnectionUtils.getListeners(_conn, _instance));

				_summaryPanel.setLocalServerName(_statusServerName.getText());
			}
			else
			{
				type = ST_DISCONNECT;
			}
		}
			
		// DISCONNECT
		if (type == ST_DISCONNECT)
		{
			_statusServerName     .setText(ST_DEFAULT_SERVER_NAME);
			_statusServerListeners.setText(ST_DEFAULT_SERVER_LISTENERS);
			_statusStatus         .setText(ST_DEFAULT_STATUS_FIELD);

			_summaryPanel.setLocalServerName("");
		}

		// STATUS
		if (type == ST_STATUS_FIELD)
		{
			_statusStatus.setText(param);
		}
		
		// MEMORY
		if (type == ST_MEMORY)
		{
			_statusMemory.setText(
				"Memory: Used "+Memory.getUsedMemoryInMB() +
				" MB, Free "+Memory.getMemoryLeftInMB() + " MB");
		}
	}
	/*---------------------------------------------------
	** END: public methods
	**---------------------------------------------------
	*/

	
	
	
	/*---------------------------------------------------
	** BEGIN: private helper methods
	**---------------------------------------------------
	*/
	private void saveProps()
	{
		Asemon.getSaveProps().setProperty("main.refresh.interval", _refreshInterval);
		Asemon.getSaveProps().setProperty("nogui.sleepTime",       _refreshNoGuiInterval);

		// Done when the system exits
		// AsemonSaveProps.getInstance().save();
	}

	private void loadProps()
	{
		// Do this at the end, since it triggers the saveProps()
		_refreshInterval      = Asemon.getSaveProps().getIntProperty("main.refresh.interval", _refreshInterval);
		_refreshNoGuiInterval = Asemon.getSaveProps().getIntProperty("nogui.sleepTime",       _refreshNoGuiInterval);
	}
	/*---------------------------------------------------
	** END: private helper methods
	**---------------------------------------------------
	*/

	

	
	
	/*---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	**---- SUBCLASSES ---- SUBCLASES ---- SUBCLASES ----- 
	**---------------------------------------------------
	**---------------------------------------------------
	**---------------------------------------------------
	*/

}
