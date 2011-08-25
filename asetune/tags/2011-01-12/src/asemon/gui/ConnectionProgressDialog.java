package asemon.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Properties;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.TableColumnExt;

import asemon.AseCacheConfig;
import asemon.AseConfig;
import asemon.GetCounters;
import asemon.MonTablesDictionary;
import asemon.Version;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.AseUrlHelper;
import asemon.utils.StringUtil;
import asemon.utils.SwingUtils;
import asemon.utils.SwingWorker;

public class ConnectionProgressDialog
extends JDialog 
implements ActionListener, ConnectionProgressCallback
{
	private static Logger _logger = Logger.getLogger(ConnectionProgressDialog.class);
	private static final long serialVersionUID = 1L;

	private static final int TAB_POS_ICON      = 0;
	private static final int TAB_POS_HOST_PORT = 1;
	private static final int TAB_POS_STATUS    = 2;
	private static final int TAB_POS_INFO      = 3;

	private static final String TASK_STATUS_CURRENT_TEXT   = "Executing";
	private static final String TASK_STATUS_SUCCEEDED_TEXT = "Completed";
	private static final String TASK_STATUS_FAILED_TEXT    = "Failed";
	private static final String TASK_STATUS_SKIPPED_TEXT   = "Skipped";

	private static final String HIDE_DETAILS_STR = "<< Details";
	private static final String SHOW_DETAILS_STR = "Details >>";

	private ConnectionProgressDialog _thisDialog      = this;
	private SwingWorker              _doConnectWorker = null;

	/** Connection made in background */
	private Connection   _connection       = null;
	/** if the connection has problemds */
	private Exception    _exception        = null;

	private ImageIcon    _ase_icon         = SwingUtils.readImageIcon(Version.class, "images/ase32.gif");
	
	private ImageIcon    _task_current_icon   = SwingUtils.readImageIcon(Version.class, "images/task_current.png");
	private ImageIcon    _task_succeeded_icon = SwingUtils.readImageIcon(Version.class, "images/task_succeeded.png");
	private ImageIcon    _task_skipped_icon   = SwingUtils.readImageIcon(Version.class, "images/task_skipped.png");
	private ImageIcon    _task_failed_icon    = SwingUtils.readImageIcon(Version.class, "images/tak_failed.png");

	private JLabel       _status_icon      = new JLabel(_ase_icon);

	private JLabel       _server_lbl       = new JLabel();
	private JLabel       _status_lbl       = new JLabel();

	private JLabel       _progress_lbl     = new JLabel();
	private JProgressBar _progress         = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
	
	private JButton      _hide_but         = new JButton("Hide");
	private JButton      _stop_but         = new JButton("Stop");
	private JButton      _detailes_but     = new JButton(HIDE_DETAILS_STR);

	private JSeparator   _separator        = new JSeparator(SwingConstants.HORIZONTAL);
	private JXTable      _task_tab         = null;

	private JLabel       _buttomStatus_lbl = new JLabel();

	private String       _urlStr           = null;
	private String[]     _hostPortArr      = {};
	private AseUrlHelper _urlHelper        = null;

	private static final String EXTRA_TASK_CHECK_MONITOR_CONFIG   = "Check Monitor Configuration";
	private static final String EXTRA_TASK_INIT_MONITOR_DICT      = "Init Monitor Dictionary";
	private static final String EXTRA_TASK_INIT_COUNTER_COLLECTOR = "Init Counter Collector";
	private boolean      _doExtraTasks     = true;

	/** Timer used to move the progressbar every X ms */
	private Timer              _progressTimer             = null;

	private Dimension          _rememberShowMode          = null;
	
	private final static int TIMER_INITIAL_WAIT  = 200;
	private final static int TIMER_PROGRESS_WAIT = 200;

	
	static
	{
		//_logger.setLevel(Level.DEBUG);
	}
	
	// FIXME: do this in a MUCH better way, this is a late night hack
	private static String     _fixme_jdbcDriver = null;
	private static String     _fixme_rawUrl     = null;
	private static Properties _fixme_props      = null;
	public static Connection connectWithProgressDialog(Window owner, String driver, String rawUrl, Properties props, boolean doExtraTasks)
	throws Exception
	{
		_fixme_jdbcDriver = driver;
		_fixme_rawUrl     = rawUrl;
		_fixme_props      = props;
		return connectWithProgressDialog(owner, rawUrl, doExtraTasks);
	}
	public static Connection connectWithProgressDialog(Window owner, String urlStr, boolean doExtraTasks)
	throws Exception
	{
		ConnectionProgressDialog cpd = null;

		if      (owner instanceof Dialog) cpd = new ConnectionProgressDialog((Dialog)owner, urlStr, doExtraTasks);
		else if (owner instanceof  Frame) cpd = new ConnectionProgressDialog( (Frame)owner, urlStr, doExtraTasks);
		else throw new IllegalAccessException("owner parameter can only be of the object types 'Dialog' or 'Frame'.");

		// kick off connect.
		cpd.doBackgroundConnect();

		// Sleep for 200ms, which may give the connect time to be finnished
		// If NOT, make the Progress dialog visible
		try { Thread.sleep(200); }
		catch (InterruptedException ignore) {}
		if (cpd._connection == null)
			cpd.setVisible(true);

		_logger.debug("hasConnection="+(cpd._connection!=null)+", hasException="+(cpd._exception!=null));
		
		if (cpd._connection != null)
			return cpd._connection;

		if (cpd._exception != null)
			throw cpd._exception;

		return null;
	}
	
	private ConnectionProgressDialog(Dialog owner, String urlStr, boolean doExtraTasks)
	{
		super(owner, true);
		init(owner, urlStr, doExtraTasks);
	}
	private ConnectionProgressDialog(Frame owner, String urlStr, boolean doExtraTasks)
	{
		super(owner, true);
		init(owner, urlStr, doExtraTasks);
	}
	private void init(Component owner, String urlStr, boolean doExtraTasks)
	{
		_urlStr = urlStr;
		_doExtraTasks = doExtraTasks;

		//	AseConnectionUtils.checkForMonitorOptions(conn, _user_txt.getText(), true, this);
		//	MonTablesDictionary.getInstance().initialize(conn);
		//	GetCounters.initExtraMonTablesDictionary();

		// Extra tasks, that happens after the connection.
		String[] extraTasks = {EXTRA_TASK_CHECK_MONITOR_CONFIG, EXTRA_TASK_INIT_MONITOR_DICT, EXTRA_TASK_INIT_COUNTER_COLLECTOR};
		if ( ! _doExtraTasks )
			extraTasks = null;

		try 
		{ 
			_urlHelper = AseUrlHelper.parseUrl(_urlStr);
			_hostPortArr = _urlHelper.getHostPortArr();
			if (extraTasks != null && extraTasks.length > 0 )
			{
				String[] withExtraTasks = new String[_hostPortArr.length + extraTasks.length];
				System.arraycopy(_hostPortArr, 0, withExtraTasks, 0,                   _hostPortArr.length);
				System.arraycopy(extraTasks,   0, withExtraTasks, _hostPortArr.length, extraTasks.length);
				_hostPortArr = withExtraTasks;

				_logger.debug("ADJUSTED _hostPortArr: "+StringUtil.toCommaStr(_hostPortArr));
			}

			String server = _urlHelper.getServerName();
			if (server != null)
				_server_lbl.setText("<html>Connecting to Server: <b>"+server+"</b></html>");
			else
				_server_lbl.setText("Server is not in the interfaces file ");

			// Calculate numer of "ticks" it should be in the progressbar
			int ticksPerSecond       = (1000 * 100) / TIMER_PROGRESS_WAIT / 100;
			int timeoutInSecPerEntry = 10;
			int hostPortCount        = _urlHelper.getHostPortCount();
			_progress.setMaximum( ticksPerSecond * timeoutInSecPerEntry * hostPortCount );

			_logger.debug("_progress.getMaximum()="+_progress.getMaximum());
		}
		catch (ParseException ignore) {}
		_status_lbl.setText("");
	
		// configure the timer.
		_progressTimer = new Timer(TIMER_INITIAL_WAIT, this);
		_progressTimer.setInitialDelay(TIMER_INITIAL_WAIT);
		_progressTimer.setDelay(TIMER_PROGRESS_WAIT);
	
		initComponents();
	
		// Set initial size
		pack();
	
		Dimension size = getPreferredSize();
		size.width  = 490;
//		size.height = 280;
		size.height = 320;
	
		setPreferredSize(size);
		setSize(size);
	
		setLocationRelativeTo(owner);
	
//		setVisible(true);
	}

	protected void initComponents() 
	{
		setTitle("Connection Progress");
		
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("ins 0","[fill]",""));

//		JScrollPane scroll = new JScrollPane( init() );
//		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		panel.add(getProgressPanel(), "push, grow");
		panel.add(getButtomPanel(),   "wrap 20");

		panel.add(_separator,         "span 2, push, grow, wrap");
		panel.add(getTaskPanel(),     "span 2, push, grow, wrap");
		panel.add(_buttomStatus_lbl,  "span 2, bottom, push, grow, wrap");

		setContentPane(panel);
	}

	protected JPanel getProgressPanel() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());

		panel.add(_status_icon, "gapleft 20, spany 3");

		panel.add(_server_lbl,   "align 50%, push, grow, wrap");
		panel.add(_status_lbl,   "align 50%, push, grow, wrap");
		panel.add(_progress_lbl, "align 50%, push, grow, wrap");

		panel.add(_progress,     "gapleft 10, span 2, push, grow, wrap");

		return panel;
	}
	protected JPanel getButtomPanel() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());

		panel.add(_hide_but,     "push, grow, wrap");
		panel.add(_stop_but,     "push, grow, wrap");
		panel.add(_detailes_but, "push, grow, wrap");

		_hide_but.setEnabled(false);
		_stop_but.setEnabled(true);

		_hide_but    .addActionListener(this);
		_stop_but    .addActionListener(this);
		_detailes_but.addActionListener(this);
		
		return panel;
	}
	protected JPanel getTaskPanel() 
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout());

		_task_tab = createTable();
		
		_task_tab.setModel(createTableModel());
		calcTabWidth();
//		_task_tab.packAll(); // set size so that all content in all cells are visible
		
		panel.add(new JScrollPane(_task_tab), "push, grow");

		return panel;
	}

	protected JXTable createTable()
	{
		final JXTable tab = new JXTable()
		{
			private static final long	serialVersionUID	= 1L;

			/** CELL tool tip */
			public String getToolTipText(MouseEvent e)
			{
				String tip = null;
				Point p = e.getPoint();
				int row = super.convertRowIndexToModel( rowAtPoint(p)<0 ? 0 : rowAtPoint(p) );
				int col = super.convertColumnIndexToModel(columnAtPoint(p));

				if (col == TAB_POS_INFO && row >= 0)
				{
					Object o = getModel().getValueAt(row, col);
					if (o instanceof SQLException)
					{
						SQLException sqlex = (SQLException) o;
						String msg = "";
						//Examine the SQLWarnings chained to this exception for the reason(s).
						msg += "-- BEGIN - SQLWarning/SQLException chain ----------------\n";
						msg += AseConnectionUtils.getMessageFromSQLException(sqlex)   + "\n";
						msg += "-- END - SQLWarning/SQLException chain ------------------\n";
						msg += "Stacktrace:\n";
						tip = "<pre>" + msg + StringUtil.stackTraceToString( sqlex ) + "</pre>";
					}
					else if (o instanceof Exception)
					{
						Exception ex = (Exception) o;
						tip = "<pre>" + StringUtil.stackTraceToString( ex ) + "</pre>";
					}
				}
				if (tip == null)
					return null;
				return "<html>" + tip + "</html>";
			}
		};

		tab.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (e.getClickCount() == 2)
				{
					Point p = e.getPoint();
					int row = tab.rowAtPoint(p);
					int col = tab.columnAtPoint(p);

					if (row >= 0 && col >= 0)
					{
						Object o = tab.getModel().getValueAt(row, TAB_POS_INFO);
						if (o instanceof Exception)
							SwingUtils.showErrorMessage(_thisDialog, "Problems Connecting", "Problems when connecting to the data server.\n\n" + ((Exception)o).getMessage(), (Exception)o);
					}
				}
			}
		});

		tab.setShowGrid(false);
		tab.setSortable(false);
		tab.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tab.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tab.setColumnControlVisible(false);

		return tab;
	}

	protected TableModel createTableModel()
	{
		String[] colHeader = {"", "Host and Port", "Status", "Message"};
		Vector<String> columns = new Vector<String>();
		for (int i = 0; i < colHeader.length; i++)
			columns.add(colHeader[i]);

		Vector<Vector<Object>> rows = new Vector<Vector<Object>>(); 
		Vector<Object> row = null;
		for (int i=0; i<_hostPortArr.length; i++)
		{
			row = new Vector<Object>();

			row.add(null); // Icon
			row.add(_hostPortArr[i]); // host:port
			row.add(""); // Ststus
			row.add("");  // connection

			rows.add(row);
		}
		
		DefaultTableModel model = new DefaultTableModel(rows, columns)
		{
			private static final long	serialVersionUID	= 1L;

			@Override
			public Class<?> getColumnClass(int column) 
		    {
		    	if (column == TAB_POS_ICON)
		    		return Icon.class;
		    	return Object.class;
		    }

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}

		};
		
		return model;
	}

	/**
	 * First size all individual columns, then try to extend last column to the full window size
	 */
	private void calcTabWidth()
	{
		_task_tab.packAll(); // set size so that all content in all cells are visible

		Dimension wind = getPreferredSize();
		
		int firstColWidth = -1; // first X columns except last column.
		int lastColWidth  = -1;
		int cols = _task_tab.getColumnCount();
		for (int c=0; c<cols; c++)
		{
			TableColumnExt tce = _task_tab.getColumnExt(c);
			lastColWidth = tce.getPreferredWidth();
			if (c < cols-1)
				firstColWidth += lastColWidth;
			if (c == cols-1)
				tce.setPreferredWidth( Math.max(wind.width - 25 - firstColWidth, lastColWidth) );
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
//		System.out.println("actionPerformed: actionEvent="+e);

		if ( _hide_but.equals(source) )
		{
			_logger.debug("button: HIDE was pressed");
			setVisible(false);
		}

		if ( _stop_but.equals(source) )
		{
			_logger.debug("button: STOP was pressed");
			if (_doConnectWorker != null)
				_doConnectWorker.interrupt();
		}

		if ( _detailes_but.equals(source) )
		{
			_logger.debug("button: DETAILES was pressed");

			if (_detailes_but.getText().equals(HIDE_DETAILS_STR))
			{
				// GOTO HIDE MODE
				_rememberShowMode = getSize();
				
				Point p = _separator.getLocation();
				int height = p.y + 20; // I thing I do wrap 20, when adding the Separator component

				setSize(_rememberShowMode.width, height);

				_detailes_but.setText(SHOW_DETAILS_STR);
			}
			else
			{
				// GOTO SHOW MODE
				setSize(getSize().width, _rememberShowMode.height);

				_detailes_but.setText(HIDE_DETAILS_STR);
			}
		}
		
		if (_progressTimer.equals(source))
		{
			incProgress();
		}
	}
	
	/**
	 * start the connect background thread when we show the window
	 */
	public void setVisible(boolean visible)
	{
		if (visible && _doConnectWorker == null)
			doBackgroundConnect();

//		if (visible == false && _doConnectWorker != null)
//			assignOutputFromBackgroundConnect();

		super.setVisible(visible);
	}

	public String[] getHostPortArr()
	{
		return _hostPortArr;
	}

	/**
	 * move the progressbar
	 */
	public void incProgress()
	{
		int now = _progress.getValue();
		int max = _progress.getMaximum();

		if (now >= max)
			now = 0;

		_progress.setValue( now + 1 );
	}


	public void setFinalStatus(int status)
	{
		setFinalStatus(status, null);
	}
	public void setFinalStatus(int status, Object infoObj)
	{
		// CHECK INPUT
		if      ( status == ConnectionProgressCallback.FINAL_STATUS_SUCCEEDED ) {}
		else if ( status == ConnectionProgressCallback.FINAL_STATUS_FAILED) {}
		else
		{
			String msg = "setFinalStatus: Unknown status '"+status+"'. know statuses 'FINAL_STATUS_SUCCEEDED | FINAL_STATUS_FAILED'.";
			_logger.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// SUCCEED
		if (status == ConnectionProgressCallback.FINAL_STATUS_SUCCEEDED)
		{
			_buttomStatus_lbl.setText("SUCCESS");
		}

		// FAILED
		if (status == ConnectionProgressCallback.FINAL_STATUS_FAILED)
		{
			String msg = infoObj+"";
			if (infoObj instanceof SQLException)
				msg = ((SQLException)infoObj).getMessage();

			_buttomStatus_lbl.setText("<html><font color=\"red\"><b>FAILURE:</b> "+msg+"</font></html>");
		}

		// Set progress to 100%
		_progress.setValue( _progress.getMaximum() );

		// stop pgrogressbar movement
		if ( _progressTimer.isRunning() )
			_progressTimer.stop();

	}

	/**
	 * 
	 * @param hostPortStr
	 * @param status
	 */
	public void setTaskStatus(String hostPortStr, int status)
	{
		setTaskStatus(hostPortStr, status, "");
	}

	/**
	 * 
	 * @param hostPortStr
	 * @param status
	 */
	public void setTaskStatus(String taskName, int status, Object infoObj)
	{
		// If not AST Event Queue Thread, call this method again, but from the Event Queue Thread
		if ( ! SwingUtils.isEventQueueThread() )
		{
			final String finalHostPortStr = taskName;
			final int    finalStatus      = status;
			final Object finalInfoObj     = infoObj;
			
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					setTaskStatus(finalHostPortStr, finalStatus, finalInfoObj);
				}
			});
			return;
		}
		
		// IF we forgot to strip of the string "jdbc:sybase:Tds:", then strip it off.
		String urlStartTemplate = "jdbc:sybase:Tds:";
		if (taskName.startsWith(urlStartTemplate))
			taskName = taskName.substring(urlStartTemplate.length());

		// IF we forgot to strip of the /dbname or ?options, then strip it off.
		if (taskName.indexOf("/") >= 0)
			taskName = taskName.substring(0, taskName.indexOf("/"));
		if (taskName.indexOf("?") >= 0)
			taskName = taskName.substring(0, taskName.indexOf("?"));

		TableModel tm = _task_tab.getModel();

		// On what row do we have: EXTRA_TASK_CHECK_MONITOR_CONFIG
		int rowForTaskCheckMonitorConfig = -1;
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String thisTaskName = (String) tm.getValueAt(r, TAB_POS_HOST_PORT);
			if (thisTaskName.equals(EXTRA_TASK_CHECK_MONITOR_CONFIG))
				rowForTaskCheckMonitorConfig = r;
		}

		
		// LOOP the table model and find the correct row to update progress status forS
		boolean foundHostPort = false;
		for (int r=0; r<tm.getRowCount(); r++)
		{
			String hostPort = (String) tm.getValueAt(r, TAB_POS_HOST_PORT);
			_logger.debug("-in-hostPort='"+taskName+"', -row-"+r+"-hostPort='"+hostPort+"'.");

			// current row == input parameter
			if (hostPort.equals(taskName))
			{
				foundHostPort = true;
				if (status == ConnectionProgressCallback.TASK_STATUS_CURRENT)
				{
					if ( ! _progressTimer.isRunning() )
						_progressTimer.start();

					// If we are beond the host-port stuff... mark the UNMARKED host-ports as "SKIPPED"
					if (EXTRA_TASK_CHECK_MONITOR_CONFIG.equals(taskName))
					{
						for (int x=0; x<rowForTaskCheckMonitorConfig; x++)
						{
							Object iconObj = tm.getValueAt(x, TAB_POS_ICON);
							if (iconObj == null)
							{
								tm.setValueAt(_task_skipped_icon,       x, TAB_POS_ICON);
								tm.setValueAt(TASK_STATUS_SKIPPED_TEXT, x, TAB_POS_STATUS);
							}
						}
					}

					_logger.debug("Setting: '"+hostPort+"', to='"+TASK_STATUS_CURRENT_TEXT+"', ICON="+_task_current_icon);
					tm.setValueAt(_task_current_icon,         r, TAB_POS_ICON);
					tm.setValueAt(TASK_STATUS_CURRENT_TEXT,   r, TAB_POS_STATUS);
					tm.setValueAt(infoObj,                    r, TAB_POS_INFO);
					
					_progress_lbl.setText("<html>Trying entry: <b>"+taskName+"</b></html>");
				}
				else if (status == ConnectionProgressCallback.TASK_STATUS_SUCCEEDED)
				{
					_logger.debug("Setting: '"+hostPort+"', to='"+TASK_STATUS_SUCCEEDED_TEXT+"', ICON="+_task_succeeded_icon);
					tm.setValueAt(_task_succeeded_icon,       r, TAB_POS_ICON);
					tm.setValueAt(TASK_STATUS_SUCCEEDED_TEXT, r, TAB_POS_STATUS);
					tm.setValueAt(infoObj,                    r, TAB_POS_INFO);
					
					_progress_lbl.setText("<html>Succeeded connecting to: <b>"+taskName+"</b></html>");

					// Set progress to 100%
					_progress.setValue( _progress.getMaximum() );

					// stop pgrogressbar movement
					if ( _progressTimer.isRunning() )
						_progressTimer.stop();

					// close the dialog
					// this will make the caller to continue processing...
//					setVisible(false);
				}
				else if (status == ConnectionProgressCallback.TASK_STATUS_SKIPPED)
				{
					_logger.debug("Setting: '"+hostPort+"', to='"+TASK_STATUS_SKIPPED_TEXT+"', ICON="+_task_skipped_icon);
					tm.setValueAt(_task_skipped_icon,         r, TAB_POS_ICON);
					tm.setValueAt(TASK_STATUS_SKIPPED_TEXT,   r, TAB_POS_STATUS);
					tm.setValueAt(infoObj,                    r, TAB_POS_INFO);

					_progress_lbl.setText("<html>Skipping task: <b>"+taskName+"</b></html>");
				}
				else if (    status == ConnectionProgressCallback.TASK_STATUS_FAILED 
				          || status == ConnectionProgressCallback.TASK_STATUS_FAILED_LAST 
				        )
				{
					_logger.debug("Setting: '"+hostPort+"', to='"+TASK_STATUS_FAILED+"', ICON="+_task_failed_icon);
					tm.setValueAt(_task_failed_icon,          r, TAB_POS_ICON);
					tm.setValueAt(TASK_STATUS_FAILED_TEXT,    r, TAB_POS_STATUS);
					tm.setValueAt(infoObj,                    r, TAB_POS_INFO);
					
					_progress_lbl.setText("<html>Failed connection to: <b>"+taskName+"</b></html>");
					
					// ARe we on the LAST ROW...
					if (r == tm.getRowCount()-1  ||  status == ConnectionProgressCallback.TASK_STATUS_FAILED_LAST)
					{
						_progress_lbl.setText("");
						_status_lbl.setText("<html>Connection to <b>ALL</b> host:port entries <b><font color=\"red\">FAILED</font></b></html>");
						
						// Set progress to 100%
						_progress.setValue( _progress.getMaximum() );

						// stop pgrogressbar movement
						if ( _progressTimer.isRunning() )
							_progressTimer.stop();
						
						// do NOT close the window, since the connection failured
						// setVisible(false);
					}
				}
				else
					_logger.error("hostPort='"+taskName+"', UNKNOWN STATUS="+status);
			}
		}

		// set size so that all content in all cells are visible
		calcTabWidth();

		if (! foundHostPort)
			_logger.warn("in-hostPort='"+taskName+"', wsa NOT found in tab.");
	}

	
	
	private void doBackgroundConnect()
	{
		_doConnectWorker = new SwingWorker()
		{
			public Object construct()
			{
				Thread.currentThread().setName("ConnectionProgressDialog");
				try
				{
					_logger.debug("SwingWorker.construct(): fixme_jdbcDriver='"+_fixme_jdbcDriver+"', fixme_rawUrl='"+_fixme_rawUrl+"', fixme_props='"+_fixme_props+"'.");
					
					if (_fixme_jdbcDriver != null && _fixme_rawUrl != null)
					{
						_logger.debug("SwingWorker.construct() does: RAW_URL: AseConnectionFactory.getConnection(fixme_jdbcDriver, _fixme_rawUrl, fixme_props, thisDialog)");
						Connection conn = AseConnectionFactory.getConnection(_fixme_jdbcDriver, _fixme_rawUrl, _fixme_props, _thisDialog);
						_connection = conn; 
						return _connection;
					}
					else
					{
						_logger.debug("SwingWorker.construct() does: NORMAL: AseConnectionFactory.getConnection(thisDialog)");
						Connection conn = AseConnectionFactory.getConnection(_thisDialog);

						if (conn != null && _doExtraTasks)
						{
							// add tasks for the:
							// - CHECK
							// - init MonTables dict
							setTaskStatus(EXTRA_TASK_CHECK_MONITOR_CONFIG, ConnectionProgressCallback.TASK_STATUS_CURRENT);
							boolean monCheckOk = AseConnectionUtils.checkForMonitorOptions(conn, null, true, _thisDialog);
							if (monCheckOk)
								setTaskStatus(EXTRA_TASK_CHECK_MONITOR_CONFIG, ConnectionProgressCallback.TASK_STATUS_SUCCEEDED);
							else
							{
								Exception ex = new Exception("The system is not properly configured for monitoring.");
								setTaskStatus(EXTRA_TASK_CHECK_MONITOR_CONFIG,   ConnectionProgressCallback.TASK_STATUS_FAILED, ex);
								setTaskStatus(EXTRA_TASK_INIT_MONITOR_DICT,      ConnectionProgressCallback.TASK_STATUS_SKIPPED);
								setTaskStatus(EXTRA_TASK_INIT_COUNTER_COLLECTOR, ConnectionProgressCallback.TASK_STATUS_SKIPPED);
								throw ex;
							}

							// Do checReconnect in MONITOR_DICT as well
							setTaskStatus(EXTRA_TASK_INIT_MONITOR_DICT, ConnectionProgressCallback.TASK_STATUS_CURRENT);
							if ( ! ConnectionDialog.checkReconnectVersion(conn) )
							{
								Exception ex = new Exception("Connecting to a different ASE Version, This is NOT supported now...");
								setTaskStatus(EXTRA_TASK_INIT_MONITOR_DICT,      ConnectionProgressCallback.TASK_STATUS_FAILED, ex);
								setTaskStatus(EXTRA_TASK_INIT_COUNTER_COLLECTOR, ConnectionProgressCallback.TASK_STATUS_SKIPPED);
								throw ex;
							}
							MonTablesDictionary.getInstance().initialize(conn);
							GetCounters.initExtraMonTablesDictionary();
								
							// initialize ASE Config Dictionary
							AseConfig aseCfg = AseConfig.getInstance();
							if ( ! aseCfg.isInitialized() )
							{
								aseCfg.initialize(conn, false, null);
							}

							// initialize ASE Cache Config Dictionary
							AseCacheConfig aseCacheCfg = AseCacheConfig.getInstance();
							if ( ! aseCacheCfg.isInitialized() )
							{
								aseCacheCfg.initialize(conn, false, null);
							}
							setTaskStatus(EXTRA_TASK_INIT_MONITOR_DICT, ConnectionProgressCallback.TASK_STATUS_SUCCEEDED);

							setTaskStatus(EXTRA_TASK_INIT_COUNTER_COLLECTOR, ConnectionProgressCallback.TASK_STATUS_CURRENT);
							GetCounters.getInstance().initCounters(
								conn,
								MonTablesDictionary.getInstance().aseVersionNum,
								MonTablesDictionary.getInstance().isClusterEnabled,
								MonTablesDictionary.getInstance().montablesVersionNum);
							setTaskStatus(EXTRA_TASK_INIT_COUNTER_COLLECTOR, ConnectionProgressCallback.TASK_STATUS_SUCCEEDED);
							
						}
						_connection = conn; 
						return _connection;
					}
				}
				catch (Exception e)
				{
					_exception = e;
					return e;
				}
			}

			// Called on the event dispatching thread (not on the worker thread) 
			// after the construct method has returned
			public void finished()
			{
				_hide_but.setEnabled(true);
				_stop_but.setEnabled(false);

				// get the output from the background job
				Object output = get();
				_logger.debug("Worker OUT: "+output);
				
				if (output != null)
				{
					if (output instanceof Connection)
					{
						Connection conn = (Connection) output;
						_logger.debug("Worker OUT-Connection: "+conn);
						_connection = conn;

						setFinalStatus(ConnectionProgressCallback.FINAL_STATUS_SUCCEEDED);

						// close the vindow
						setVisible(false);
					}
					else if (output instanceof Exception)
					{
						Exception ex = (Exception)output;
						_logger.debug("Worker OUT-Exception: "+ex, ex);
						_exception = ex;

						setFinalStatus(ConnectionProgressCallback.FINAL_STATUS_FAILED, ex);
					}
					else
					{
						_logger.error("Unknown output from the background swing worker was found. type="+output.getClass().getName()+", object.toString='"+output.toString()+"'.");
					}
				}
				else
				{
					setFinalStatus(ConnectionProgressCallback.FINAL_STATUS_FAILED, "The output from the worker thread was 'null'. This can't be right.");
				}
			}

			public void interrupt()
			{
				// Can we do something here it STOP the connection?
				// well I dont think so but anyway...
				//super.interrupt();
			}
		};
		
		// Start the worker and release controll to the Swing/AWT Event Dispatcher Thread
		_doConnectWorker.start();
	}
	
	
//	private void assignOutputFromBackgroundConnect()
//	{
//		if (_doConnectWorker != null)
//		{
//			// get the output from the background job
//			Object output = _doConnectWorker.get();
//			_logger.debug("Worker OUT: "+output);
//			
//			if (output != null)
//			{
//				if (output instanceof Connection)
//				{
//					_logger.debug("Worker OUT-Connection: "+output);
//					_connection = (Connection) output;
//				}
//				else if (output instanceof Exception)
//				{
//					_logger.debug("Worker OUT-Exception: "+output);
//					_exception = (Exception) output;
//				}
//				else
//				{
//					_logger.error("Unknown output from the background swing worker was found. type="+output.getClass().getName()+", object.toString='"+output.toString()+"'.");
//				}
//			}
//		}
//	}
	
	
	
	
	
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	// TEST CODE // TEST CODE // TEST CODE // TEST CODE // TEST CODE // TEST CODE //
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////

	private static class TestFrame
	extends JFrame
	implements ActionListener
	{
		private static final long	serialVersionUID	= 1L;

		private int    _dummyCounter     = 0;
		private String _hostPortUrl      = "";
		private JLabel _hostPortUrl_lbl  = new JLabel();
		private JLabel _dummyCounter_lbl = new JLabel();
		private JButton _connect_but     = new JButton("Connect");
		private Timer  _dummyTimer       = new Timer(100, this);
		public TestFrame(String url)
		{
			setSize(500, 500);
			setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			_hostPortUrl = url;
			_hostPortUrl_lbl.setText(_hostPortUrl);

			
			JPanel panel = new JPanel();
			panel.setLayout(new MigLayout());
			panel.add(_hostPortUrl_lbl,  "wrap");
			panel.add(_dummyCounter_lbl, "wrap");
			panel.add(_connect_but,      "wrap");
			setContentPane(panel);

			_connect_but.addActionListener(this);
		}
		public void actionPerformed(ActionEvent e)
		{
			Object source = e.getSource();

			if (_dummyTimer.equals(source))
			{
				_dummyCounter++;
				_dummyCounter_lbl.setText("Dummy counter at: "+_dummyCounter);
			}

			if (_connect_but.equals(source))
			{
				System.out.println("CONNECT WAS Pressed...");
				tryConnect();
			}
		}

		public void setVisible(boolean visible)
		{
			if (visible)
				_dummyTimer.start();
			else
				_dummyTimer.stop();

			super.setVisible(visible);
		}
		
		private void tryConnect()
		{
//			ConnectionProgressDialog cpd = new ConnectionProgressDialog(this, _hostPortUrl);
//			System.out.println("XXXXXXXXXXXXXXXXXX");
//
//			cpd.setVisible(true);
//			System.out.println("ZZZZZZZZZZZZZZZZZZZZZZZZzzz");

			try
			{
				Connection conn = ConnectionProgressDialog.connectWithProgressDialog(this, _hostPortUrl, true);
				System.out.println("Connection returned. conn="+conn);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args)
	{
		try	{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (Exception ignore) {}

//		String urlStr = "jdbc:sybase:Tds:host1.ericsson.se:1111,host2.ericsson.se:2222,192.168.0.125:3333/dbname?OPT=val&OPT2=val&OPT3=val";
//		String urlStr = "jdbc:sybase:Tds:localhost:5000,gorans-xp:5000";

//		String server = "GORAN_1_DS";
		String server = "AAA_CE_DS";
		String hostPortStr = AseConnectionFactory.getIHostPortStr(server);
		AseConnectionFactory.setUser("sa");
		AseConnectionFactory.setPassword("");
		AseConnectionFactory.setServer(server);
		System.out.println("Connectiong to server='"+server+"'. Which is located on '"+hostPortStr+"'.");

		String urlStr = "jdbc:sybase:Tds:"+AseConnectionFactory.getHostPortStr();
		
		JFrame frame = new TestFrame(urlStr);
		frame.setVisible(true);

		
//		System.exit(0);
	}

}
