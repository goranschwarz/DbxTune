package asemon.perftest;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXMultiSplitPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.MultiSplitLayout;

import asemon.Version;
import asemon.cm.CountersModel;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.SwingUtils;

import com.sybase.jdbcx.EedInfo;
import com.sybase.jdbcx.SybConnection;
import com.sybase.jdbcx.SybMessageHandler;
import com.sybase.util.ds.interfaces.SyInterfacesDriver;

public class PerfDemo
extends JFrame
implements ActionListener
{
	private static Logger _logger = Logger.getLogger(PerfDemo.class);
	private static final long serialVersionUID	= 1L;

	private Connection _qProdGenConn   = null;
	private Connection _qProdInfoConn  = null;
	private Connection _qCntrlConn     = null;
	private Connection _qConsInfoConn  = null;
	private Connection _qStatInfoConn  = null;
	
	private String _initialDbname  = "";

	
	JXMultiSplitPane _multiSplitPane = new JXMultiSplitPane(); 
	String _splitPaneLayoutFilename = Version.APP_STORE_DIR + "/perfDemo.layout.save.xml";

	// JDBC PANEL
	private JComboBox  _aseName	    = new JComboBox();
	private JTextField _aseHost     = new JTextField("");
	private JTextField _asePort     = new JTextField("");
	private JTextField _aseUsername = new JTextField("");
	private JTextField _asePassword = new JPasswordField();
	private JTextField _aseDbname   = new JTextField();
	private JButton    _aseConnect  = new JButton("Connect");

	private SyInterfacesDriver _interfacesDriver = null;

	// Queue Producer Info PANEL
	private JTextField _qProdQSize     = new JTextField();
	private JTextField _qProdQSizeRate = new JTextField();
	private int        _qProdQSizeSave = 0;
	private long       _qProdQSizeTime = 0;

	// Queue Producer Generator PANEL
	private final String QTYPE_ALL     = "All Types";
	private final String QTYPE_1       = "dest1";
	private final String QTYPE_2       = "dest2";
	private final String QTYPE_3       = "dest3";

	private JTextField _qProdGenType   = new JTextField();
	private JTextField _qProdGenNum    = new JTextField();
	private JComboBox  _qProdGenHow_cbx= new JComboBox(new String[] {"<Choose Type>", QTYPE_ALL, QTYPE_1, QTYPE_2, QTYPE_3});
	private JButton    _qProdGen_but   = new JButton("Generate new values");

	// Queue Consumer PANEL
	private final String RESUME_CONSUMERS     = "Resume All Consumers";
	private final String PAUSE_CONSUMERS_WAIT = "Pause All Consumers, Wait";
	private final String PAUSE_CONSUMERS_NOW  = "Pause All Consumers, NOW";
	private final String STOP_CONSUMERS_WAIT  = "Stop All Consumers";

	private final String Q_CONS_PARAM_DEFAULT = "<Use Default parameters>";
	private final String Q_CONS_PARAM_SET1    = "Batch Size=10, Update Queue Status=1, Use Transaction=1";
	private final String Q_CONS_PARAM_SET2    = "Batch Size=10, Update Queue Status=0, Use Transaction=1";
	private final String Q_CONS_PARAM_SET3    = "Batch Size=10, Update Queue Status=0, Use Transaction=0";
	private final String Q_CONS_PARAM_SET4    = "Batch Size=1, Update Queue Status=0, Use Transaction=0";

	private Vector<String>       _qConsTableCols  = new Vector<String>( Arrays.asList(new String[] {"spid", "status", "cmd", "qThreadInState", "msgToOther"}));
	private JXTable              _qConsTable      = null;
	private GTableModel          _qConsTableModel = null;
	private JTextField           _qConsExecCount  = new JTextField();
	private JTextField           _qConsExecParams = new JTextField();
	private JComboBox            _qConsExecParams_cbx = new JComboBox(new String[] {Q_CONS_PARAM_DEFAULT, Q_CONS_PARAM_SET1, Q_CONS_PARAM_SET2, Q_CONS_PARAM_SET3, Q_CONS_PARAM_SET4});
	private JButton              _qConsStart_but  = new JButton("Start a new Consumer");
	private JComboBox            _qConsStatus_cbx = new JComboBox(new String[] {"<Choose Status>", RESUME_CONSUMERS, PAUSE_CONSUMERS_WAIT, PAUSE_CONSUMERS_NOW, STOP_CONSUMERS_WAIT});
	private JButton              _qConsStop_but   = new JButton("Stop last Consumer");
	private List<QueueConsumer>  _qConsThreadList = new LinkedList<QueueConsumer>();

	// Queue Statistics PANEL
	private final String STAT_QUEUE_INFO = "Queue Info Statistics";
	private final String STAT_TAB_ROWS   = "Destination Table Row Count";
	private String            _qStatCurrentType   = STAT_QUEUE_INFO;
	private JComboBox         _qStatType_cbx      = new JComboBox(new String[] {STAT_QUEUE_INFO, STAT_TAB_ROWS});
	private Vector<String>    _qStatTableCols     = new Vector<String>( Arrays.asList(new String[] {"operation", "execCounter", "lastExecTime", "lastExecTimeMs", "avgExecTimeMs", "minExecTimeMs", "maxExecTimeMs"}));
	private JXTable           _qStatTable         = null;
	private GTableModel       _qStatTmDefault     = null;
	private CountersModel     _qStatTmRowCount    = null;

	// LOG PANEL
	private JTextPane         _logArea         = new JTextPane();

	public PerfDemo(String dbname)
	{
		setTitle("Performance Demo");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		_initialDbname = dbname;
		initComponents();
		pack();

		Thread qProdInfo = new QueueProducerInfoThread("qProducerInfo", this);
		qProdInfo.start();

		Thread qConsInfo = new QueueConsumerInfoThread("qConsumerInfo", this);
		qConsInfo.start();

		Thread qStatInfo = new QueueStatisticsInfoThread("qStatisticsInfo", this);
		qStatInfo.start();

		// Try to fit all rows on the open window
//		Dimension size = getSize();
//		size.height += (_table.getRowCount() - 6) * 18; // lets say 6 rows is the default showed AND each row takes 18 pixels
//		size.width = Math.min(size.width, 700);
//		setSize(size);
		setSize(1000, 800);
	}
	
	/*---------------------------------------------------
	** BEGIN: component initialization
	**---------------------------------------------------
	*/
	protected void initComponents()
	{
		setLayout( new BorderLayout()); 
	  
		String layoutDef = 
			"(COLUMN (ROW weight=0.5 " +
			"              (COLUMN weight=0.0 (LEAF name=jdbc       weight=0.0)" +
			"                                 (LEAF name=qProdInfo  weight=0.0)" +
			"                                 (LEAF name=qProdGen   weight=1.0)" +
			"              ) " +
			"              (COLUMN weight=1.0 (LEAF name=qCons  weight=0.8)" +
			"                                 (LEAF name=qStats weight=0.2)" +
			"              ) " +
			"        ) " +
			" (LEAF name=bottom  weight=0.5))"; 
		// http://today.java.net/pub/a/today/2006/03/23/multi-split-pane.html
		// String layoutDefXX = "(COLUMN (ROW weight=1.0 left (COLUMN middle.top middle middle.bottom) right) bottom)";
		try 
		{
			XMLDecoder d = new XMLDecoder(new BufferedInputStream(new FileInputStream(_splitPaneLayoutFilename)));
			MultiSplitLayout.Node model = (MultiSplitLayout.Node)(d.readObject());
			_multiSplitPane.getMultiSplitLayout().setModel(model);
			_multiSplitPane.getMultiSplitLayout().setFloatingDividers(false);
		    d.close();
		}
		catch (Exception exc) 
		{ 
			MultiSplitLayout.Node model = MultiSplitLayout.parseModel(layoutDef); 
			_multiSplitPane.getMultiSplitLayout().setModel(model);
		}	  
//		MultiSplitLayout.Node modelRoot = MultiSplitLayout.parseModel( layoutDef ); 
//		_multiSplitPane.getMultiSplitLayout().setModel( modelRoot ); 
	  
		_multiSplitPane.add( createJdbcPanel(),            "jdbc" ); 
		_multiSplitPane.add( createQueueInfoPanel(),       "qProdInfo" ); 
		_multiSplitPane.add( createQueueGeneratePanel(),   "qProdGen" ); 
		_multiSplitPane.add( createQueueConsumerPanel(),   "qCons" ); 
		_multiSplitPane.add( createQueueStatisticsPanel(), "qStats" ); 
		_multiSplitPane.add( createLogPanel(),             "bottom" ); 
	  
		// ADDING A BORDER TO THE MULTISPLITPANE CAUSES ALL SORTS OF ISSUES 
		_multiSplitPane.setBorder( BorderFactory.createEmptyBorder( 4, 4, 4, 4 ) ); 
		add( _multiSplitPane, BorderLayout.CENTER ); 
	}

	private JPanel createJdbcPanel()
	{
		JPanel panel = SwingUtils.createPanel("JDBC Info", true);
		panel.setLayout(new MigLayout());

		panel.add(new JLabel("ASE Name"));
		panel.add(_aseName, "pushx, growx, wrap");
		_aseName.addActionListener(this);

		panel.add(new JLabel("Host Name"));
		panel.add(_aseHost, "pushx, growx, wrap");

		panel.add(new JLabel("Port Number"));
		panel.add(_asePort, "pushx, growx, wrap");

		panel.add(new JLabel("DB Name"));
		panel.add(_aseDbname, "pushx, growx, wrap");

		panel.add(new JLabel("Username"));
		panel.add(_aseUsername, "pushx, growx, wrap");

		panel.add(new JLabel("Password"));
		panel.add(_asePassword, "pushx, growx, wrap");

		panel.add(_aseConnect, "span, align right");
		_aseConnect.addActionListener(this);
		
		initJdbcData();
		
		return panel;
	}

	private void initJdbcData()
	{
		try 
		{
			_interfacesDriver = new SyInterfacesDriver();
			_interfacesDriver.open();
		}
		catch(Exception ex)
		{
			_logger.error("Problems reading interfaces or sql.ini file.", ex);
		}

		if (_interfacesDriver != null)
		{
			_logger.debug("Just opened the interfaces file '"+ _interfacesDriver.getBundle() +"'.");
			
			_aseName.addItem("<-Choose a server->");
			String[] servers = _interfacesDriver.getServers();
			Arrays.sort(servers);
			for (int i=0; i<servers.length; i++)
			{
				_logger.debug("Adding server '"+ servers[i] +"' to serverListCB.");
				_aseName.addItem(servers[i]);
			}
		}

		// Add some dummy values
		_aseHost    .setText("localhost");
		_asePort    .setText("5000");
		_aseUsername.setText("sa");
		_aseDbname  .setText(_initialDbname);
	}
	
	private JPanel createQueueInfoPanel()
	{
		JPanel panel = SwingUtils.createPanel("Queue Producer Info", true);
		panel.setLayout(new MigLayout());
		
		panel.add(new JLabel("Current Queue Size"));
		panel.add(_qProdQSize, "pushx, growx, wrap");
		_qProdQSize.setEditable(false);
		_qProdQSize.setText("Unknown");

		panel.add(new JLabel("Current Queue Rate"));
		panel.add(_qProdQSizeRate, "pushx, growx, wrap");
		_qProdQSizeRate.setEditable(false);
		_qProdQSizeRate.setText("Unknown");
		
		return panel;
	}
	
	private JPanel createQueueGeneratePanel()
	{
		JPanel panel = SwingUtils.createPanel("Queue Producer Generator", true);
		panel.setLayout(new MigLayout());
		
		panel.add(new JLabel("Type of entries to generate"));
		panel.add(_qProdGenType, "pushx, growx, wrap");

		panel.add(new JLabel("Number of entries to generate"));
		panel.add(_qProdGenNum, "pushx, growx, wrap");

		panel.add(new JLabel("How to generate"));
		panel.add(_qProdGenHow_cbx, "pushx, growx, wrap");
		_qProdGenHow_cbx.addActionListener(this);

		panel.add(_qProdGen_but, "span, split, tag right");
		_qProdGen_but.addActionListener(this);
		_qProdGen_but.setEnabled(false);
		
		return panel;
	}
	
	private JPanel createQueueConsumerPanel()
	{
		JPanel panel = SwingUtils.createPanel("Queue Consumer Info", true);
		panel.setLayout(new MigLayout());

		initQueueConsumerPanel();
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_qConsTable);
		panel.add(jScrollPane, "height 100%, span, grow, wrap");

		panel.add(new JLabel("Current Running Queue Consumers"), "");
		panel.add(_qConsExecCount, "grow, push, wrap");
		_qConsExecCount.setEditable(false);

		panel.add(new JLabel("Parameters passed to Procedure 'qConsumer' on Startup"), "");
		panel.add(_qConsExecParams, "grow, push, wrap");

		panel.add(new JLabel("Pre Configured parameters to 'qConsumer'"), "");
		panel.add(_qConsExecParams_cbx, "grow, push, wrap");
		_qConsExecParams_cbx.addActionListener(this);
		
		panel.add(_qConsStart_but, "span, split, tag left");
		_qConsStart_but.addActionListener(this);
		_qConsStart_but.setEnabled(false);

		panel.add(_qConsStatus_cbx, "tag other");
		_qConsStatus_cbx.addActionListener(this);
		_qConsStatus_cbx.setEnabled(false);

		panel.add(_qConsStop_but, "tag right");
		_qConsStop_but.addActionListener(this);
		_qConsStop_but.setEnabled(false);

		return panel;
	}
	private void initQueueConsumerPanel()
	{
		_qConsTableModel = new GTableModel(_qConsTableCols);
//		_qConsTableModel.setColumnIdentifiers( _qConsTableCols );
		_qConsTable      = new JXTable(_qConsTableModel);
		_qConsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_qConsTable.setShowGrid(false);
//		_qConsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
	
	private JPanel createQueueStatisticsPanel()
	{
		JPanel panel = SwingUtils.createPanel("Statistics Info", true);
		panel.setLayout(new MigLayout());

		panel.add(new JLabel("Statistics about"), "split");
		panel.add(_qStatType_cbx, "wrap");
		_qStatType_cbx.addActionListener(this);
				
		initQueueStatisticsPanel();
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_qStatTable);
		panel.add(jScrollPane, "height 100%, grow, push, wrap");

		return panel;
	}
	private void initQueueStatisticsPanel()
	{
		_qStatTmDefault = new GTableModel(_qStatTableCols);
//		_qStatTableModel.setColumnIdentifiers( _qStatTableCols );
		_qStatTable      = new JXTable(_qStatTmDefault);
		_qStatTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
//		_qStatTable.setShowGrid(false);
//		_qStatTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		String dbname  = "perfdemo";
		String tabname = "DestTab%";
		String sql = 
			" select tabName          = name, \n" +
			"        rowsInTable      = row_count (db_id('"+dbname+"'), id), \n" +
			"        dataSizeInKB     = used_pages(db_id('"+dbname+"'), id) * (@@maxpagesize / 1024), \n" +
			"        rowsInTableRate  = row_count (db_id('"+dbname+"'), id), \n" +
			"        dataSizeInKBRate = used_pages(db_id('"+dbname+"'), id) * (@@maxpagesize / 1024)\n" +
			" from "+dbname+"..sysobjects  \n" +
			" where type = 'U'  \n" +
			"   and name like '"+tabname+"' \n" +
			" order by name ";

		int                needVersion   = 0;
		int                needCeVersion = 0;
		String[]           monTables     = new String[] {};
		String[]           needRole      = new String[] {};
		String[]           needConfig    = new String[] {};
		String[]           colsCalcDiff  = new String[] {"rowsInTableRate", "dataSizeInKBRate"};
		String[]           colsCalcPCT   = new String[] {};
		LinkedList<String> pkList        = new LinkedList<String>();
		pkList.add("tabName");

		_qStatTmRowCount = new CountersModel("StatRowCount", 
				sql, 
				pkList, colsCalcDiff, colsCalcPCT, 
				monTables, needRole, needConfig, needVersion, needCeVersion, 
				false, false);
	}

	private JPanel createLogPanel()
	{
		JPanel panel = SwingUtils.createPanel("Log", true);
		panel.setLayout(new MigLayout());
		
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_logArea);
		panel.add(jScrollPane, "height 100%, grow, push, wrap");

		return panel;
	}

//	private JPanel createOkPanel()
//	{
//		// ADD the OK, Cancel, Apply buttons
//		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 0 0","",""));
////		panel.add(_ok,     "tag ok");
////		panel.add(_cancel, "tag cancel");
////		panel.add(_apply,  "tag apply");
////		
////		_ok    .addActionListener(this);
////		_cancel.addActionListener(this);
////		_apply .addActionListener(this);
//
//		return panel;
//	}

	private void saveLayout()
	{
		// Write the layout to a file...
		try
		{
			XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(_splitPaneLayoutFilename)));
			MultiSplitLayout.Node model = _multiSplitPane.getMultiSplitLayout().getModel();
			e.writeObject(model);
			e.close();
		}
		catch (Exception e) { e.printStackTrace(); }
	}

	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	// ACTION PERFORMED
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	//-----------------------------------------------------------------------
	@Override
	public void actionPerformed(ActionEvent ae)
	{
		JComponent source = (JComponent) ae.getSource();
		String name = (String)source.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (_aseName.equals(source))
		{
			String srv = (String) _aseName.getSelectedItem();
			_aseHost.setText(AseConnectionFactory.getIHosts(srv));
			_asePort.setText(AseConnectionFactory.getIPorts(srv));
		}

		if (_aseConnect.equals(source))
		{
			String user   = _aseUsername.getText();
			String pass   = _asePassword.getText();
			String host   = _aseHost.getText();
			String port   = _asePort.getText();
			String dbname = _aseDbname.getText();

			_qCntrlConn = aseConnection(false, false, "qController", dbname, user, pass, host, port);

			// If we succeed in connection, do some more
			if (_qCntrlConn != null)
			{
				qControllerAction("RESUME");

				_qProdGen_but   .setEnabled(true);
				_qConsStart_but .setEnabled(true);
				_qConsStatus_cbx.setEnabled(true);
				_qConsStop_but  .setEnabled(true);

				_qProdGenConn  = aseConnection(false, false, "qProducer",       dbname, user, pass, host, port);
				_qProdInfoConn = aseConnection(false, false, "qProducerInfo",   dbname, user, pass, host, port);
				_qStatInfoConn = aseConnection(false, false, "qStatisticsInfo", dbname, user, pass, host, port);
				_qConsInfoConn = aseConnection(false, false, "qConsumerInfo",   dbname, user, pass, host, port);
			}

			saveLayout();
		}
		
		if (_qConsStart_but.equals(source))
		{
			if (_qCntrlConn == null)
			{
				JOptionPane.showMessageDialog(this, "NOT Connected.\nPlease Connect first.", "PerfDemo - No Connection", JOptionPane.ERROR_MESSAGE);
				return;
			}
			String user   = _aseUsername.getText();
			String pass   = _asePassword.getText();
			String host   = _aseHost.getText();
			String port   = _asePort.getText();
			String dbname = _aseDbname.getText();

			int consumerId = _qConsThreadList.size();
			Connection conn = aseConnection(false, false, "qConsumer-"+consumerId, dbname, user, pass, host, port);

			String qConsExecParams = _qConsExecParams.getText();
			
			QueueConsumer qc = new QueueConsumer(this, conn, consumerId, qConsExecParams);
			_qConsThreadList.add(qc);
			qc.execute();

			//_qConsExecCount.setText(""+_qConsThreadList.size());
		}

		if (_qConsExecParams_cbx.equals(source))
		{
			String str = (String) _qConsExecParams_cbx.getSelectedItem();
			if (Q_CONS_PARAM_DEFAULT.equals(str))
			{
				_qConsExecParams.setText("");
			}
			else if (Q_CONS_PARAM_SET1.equals(str))
			{
				_qConsExecParams.setText("@batchSize=10, @updQueueStat=1, @useTrans=1");
			}
			else if (Q_CONS_PARAM_SET2.equals(str))
			{
				_qConsExecParams.setText("@batchSize=10, @updQueueStat=0, @useTrans=1");
			}
			else if (Q_CONS_PARAM_SET3.equals(str))
			{
				_qConsExecParams.setText("@batchSize=10, @updQueueStat=0, @useTrans=0");
			}
			else if (Q_CONS_PARAM_SET4.equals(str))
			{
				_qConsExecParams.setText("@batchSize=1, @updQueueStat=0, @useTrans=0");
			}
		}

		if (_qConsStatus_cbx.equals(source))
		{
			String str = (String) _qConsStatus_cbx.getSelectedItem();
			if (RESUME_CONSUMERS.equals(str))
			{
				qControllerAction("RESUME");
			}
			else if (PAUSE_CONSUMERS_WAIT.equals(str))
			{
				qControllerAction("PAUSE_WAIT");
			}
			else if (PAUSE_CONSUMERS_NOW.equals(str))
			{
				qControllerAction("PAUSE_NOW");
			}
			else if (STOP_CONSUMERS_WAIT.equals(str))
			{
				qControllerAction("STOP_WAIT");
			}
		}

		if (_qConsStop_but.equals(source))
		{
			int consumerId = _qConsThreadList.size() - 1;
			QueueConsumer qc = _qConsThreadList.get(consumerId);
			qc.cancel(true);
		}

		if (_qProdGenHow_cbx.equals(source))
		{
			String str = (String) _qProdGenHow_cbx.getSelectedItem();
			if (QTYPE_ALL.equals(str))
			{
				_qProdGenType.setText("");
			}
			else
			{
				_qProdGenType.setText(str);
			}
		}

		if (_qProdGen_but.equals(source))
		{
			int number = -1; 
			try { number = Integer.parseInt(_qProdGenNum.getText()); } 
			catch (NumberFormatException ignore) {}

			String type = _qProdGenType.getText();
			if (type.trim().equals(""))
				type = null;

			final String fType   = type;
			final int    fNumber = number;
			PerfDemoInfoThread doIt = new PerfDemoInfoThread("qProducerGenerate", this)
			{
				@Override
				public boolean doWork()
				{
					_perfDemo.qProducerGenerate(fType, fNumber);
					return false; // do NOT execute again
				}
			};
			doIt.start();
		}

		if (_qStatType_cbx.equals(source))
		{
			String str = (String) _qStatType_cbx.getSelectedItem();
			if (STAT_QUEUE_INFO.equals(str))
			{
				_qStatCurrentType = STAT_QUEUE_INFO;
				_qStatTable.setModel(_qStatTmDefault);
			}
			else if (STAT_TAB_ROWS.equals(str))
			{
				_qStatCurrentType = STAT_TAB_ROWS;
				_qStatTable.setModel(_qStatTmRowCount);
			}
		}
	}

	protected void qProducerGenerate(String type, int batchSize)
	{
		String typeStr      = (type == null)   ? "@type=null"      : "@type='"+type+"'";
		String batchSizeStr = (batchSize <= 0) ? "@batchSize=null" : "@batchSize="+batchSize;

		Connection conn = _qProdGenConn;
		String sql = "exec qGenerate "+batchSizeStr+", "+typeStr; // @batchSize=10000, @type=null

		System.out.println("BEGIN: qProducerGenerate() sql: "+sql);
		try
		{
			Statement stmnt = conn.createStatement();
			stmnt.executeUpdate(sql);
			stmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("qProducerGenerate(): problems exec sql: "+sql);
		}
		System.out.println("END: qProducerGenerate() sql: "+sql);
	}

	private void qControllerAction(String action)
	{
		Connection conn = _qCntrlConn;
		String sql = "exec setAppStatus 'qController', null, '"+action+"'";
		try
		{
			Statement stmnt = conn.createStatement();
			stmnt.executeUpdate(sql);
			stmnt.close();
		}
		catch (SQLException e)
		{
			_logger.error("qControllerAction("+action+"): problems exec sql: "+sql);
		}
	}

	private int getProducerQueueSize()
	{
		int rows = -1;
		Connection conn = _qProdInfoConn;
//		String sql = "sp_spaceused 'TestQueue'"; 
		String sql = "select row_count(db_id(), object_id('TestQueue'))"; 
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			while(rs.next())
			{
				rows = rs.getInt(1);
			}
			rs.close();
			stmnt.close();
			
			return rows;
		}
		catch (SQLException e)
		{
			_logger.error("getProducerQueueSize(): problems exec sql '"+sql+"'. Caught "+e);
		}
		return -1;
	}
	
	protected void refreshQProducerInfo()
	{
		int size = getProducerQueueSize();
		_qProdQSize.setText(size+"");

		if (_qProdQSizeTime != 0)
		{
			int  sizeDiff = _qProdQSizeSave - size;
			long timeDiff = System.currentTimeMillis() - _qProdQSizeTime;

			if ((timeDiff / 1000) > 0)
			{
				double rate = sizeDiff / (timeDiff / 1000);
				_qProdQSizeRate.setText(rate+" per second");
			}
		}
		_qProdQSizeTime = System.currentTimeMillis();
		_qProdQSizeSave = size;
	}

	protected void refreshQConsumerInfo()
	{
		Connection conn = _qConsInfoConn;
		String sql = 
			"select spid, status, cmd, clienthostname, clientname " +
			"from master..sysprocesses " +
			"where clientapplname = 'qConsumer'";
		try
		{
			Statement stmnt = conn.createStatement();
			ResultSet rs = stmnt.executeQuery(sql);

			Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
			while(rs.next())
			{
				Vector<Object> row = new Vector<Object>();
				
				row.add(rs.getObject(1));
				row.add(rs.getObject(2));
				row.add(rs.getObject(3));
				row.add(rs.getObject(4));
				row.add(rs.getObject(5));

				rows.add(row);
			}
			rs.close();
			stmnt.close();

			_qConsTableModel.setDataVector(rows);
			if (_qConsTableModel.timeToPackAll())
				_qConsTable.packAll();
//			SwingUtils.calcColumnWidths(_qConsTable, 0, true);
			
			_qConsExecCount.setText(""+_qConsTableModel.getRowCount());
		}
		catch (SQLException e)
		{
			_logger.error("getQConsumerInfo(): problems exec sql '"+sql+"'. Caught "+e);
		}
	}

	protected void refreshQStatInfo()
	{
		Connection conn = _qStatInfoConn;
		
		if (_qStatCurrentType.equals(STAT_QUEUE_INFO))
		{
			String sql = "select * from qStatInfo";
			try
			{
				Statement stmnt = conn.createStatement();
				ResultSet rs = stmnt.executeQuery(sql);
	
				Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
				while(rs.next())
				{
					Vector<Object> row = new Vector<Object>();
					
					row.add(rs.getObject(1));
					row.add(rs.getObject(2));
					row.add(rs.getObject(3));
					row.add(rs.getObject(4));
					row.add(rs.getObject(5));
					row.add(rs.getObject(6));
					row.add(rs.getObject(7));
	
					rows.add(row);
				}
				rs.close();
				stmnt.close();
	
				_qStatTmDefault.setDataVector(rows);
				if (_qStatTmDefault.timeToPackAll())
					_qStatTable.packAll();
//				SwingUtils.calcColumnWidths(_qStatTable, 0, true);
	
			}
			catch (SQLException e)
			{
				_logger.error("refreshQStatInfo(): problems exec sql '"+sql+"'. Caught "+e);
			}
		}
		if (_qStatCurrentType.equals(STAT_TAB_ROWS))
		{
			try
			{
				_qStatTmRowCount.refresh(conn);
//				_qStatTable.packAll();
			}
			catch (Exception e)
			{
				_logger.error("refreshQStatInfo(): problems exec CounterModel. Caught "+e);
			}
		}
	}

	public void appendToLog(String str)
	{
		if (str != null && ! str.endsWith("\n"))
			str += "\n";

		JTextPane screen = _logArea;
		Document doc = screen.getDocument();
		try
		{
			doc.insertString(doc.getLength(), str, null);//screen.getStyle("bold"));

			// set at the end...
			screen.setCaretPosition(doc.getLength());
		}
		catch (BadLocationException ble)
		{
			System.out.println("Couldn't insert text");
		}
	}


	private Connection aseConnection(boolean showInfo, boolean closeConn, String appname, String dbname, String user, String passwd, String host, String port)
	{
//		String driverClassName = System.getProperty("jdbc_driver_class_name", "com.sybase.jdbc3.jdbc.SybDriver");
//		String startOfConnUrl  = System.getProperty("jdbc_start_of_conn_url", "jdbc:sybase:Tds:");


		try
		{
//			Class.forName(driverClassName).newInstance();
//			Properties props = new Properties();
//			props.put("user", user);
//			props.put("password", passwd);
////			props.put("JCONNECT_VERSION", "6");
////			props.put("USE_METADATA", "FALSE");
////			props.put("PACKETSIZE", "512");
//			props.put("APPLICATIONNAME", appname);
////			props.put("CHARSET", "iso_1");
//	
//			_logger.debug("Try getConnection to " + host + ":" + port + " user=" + user);
//			Connection conn = DriverManager.getConnection(startOfConnUrl + host + ":" + port, props);

			Connection conn = AseConnectionFactory.getConnection(host, port, dbname, user, passwd, appname, "", null);

			if (conn instanceof SybConnection)
				((SybConnection)conn).setSybMessageHandler(new PerfDemoSybMessageHandler(appname+": ", this, null));

			// select @@version
			String aseVersionStr = "unknown";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select @@version");
			while ( rs.next() )
			{
				aseVersionStr = rs.getString(1);
			}
			rs.close();
			if (showInfo)
				JOptionPane.showMessageDialog(this, "Connection succeeded.\n\n"+aseVersionStr, "PerfDemo - connect check", JOptionPane.INFORMATION_MESSAGE);
			else
				appendToLog("Connection succeeded: "+aseVersionStr);

			if (closeConn)
			{
				conn.close();
				conn = null;
				return null;
			}

			if (dbname != null && !dbname.trim().equals(""))
				AseConnectionUtils.useDbname(conn, dbname);

			String currentDbname = AseConnectionUtils.getCurrentDbname(conn);

			if (dbname != null && !dbname.trim().equals("") && ! currentDbname.equals(dbname) )
			{
				JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n" +
						"Current dbname is '"+currentDbname+"'.\n" +
						"But you requested to be in '"+dbname+"'.", 
						"PerfDemo - connect check", JOptionPane.ERROR_MESSAGE);
				conn.close();
				conn = null;
			}

//			System.out.println("Current dbname = '"+currentDbname+"'.");
//			System.out.println("  Input dbname = '"+dbname+"'.");

			return conn;
		}
		catch (SQLException e)
		{
			StringBuffer sb = new StringBuffer();
			while (e != null)
			{
				sb.append( "\n" );
				sb.append( e.getMessage() );
				e = e.getNextException();
			}
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+sb.toString(), "PerfDemo - connect check", JOptionPane.ERROR_MESSAGE);
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(this, "Connection FAILED.\n\n"+e.toString(),  "PerfDemo - connect check", JOptionPane.ERROR_MESSAGE);
		}
		return null;
	}

	private abstract class PerfDemoInfoThread
	extends Thread
	{
		protected String   _name     = null;
		protected PerfDemo _perfDemo = null;
	
		public PerfDemoInfoThread(String name, PerfDemo perfDemo)
		{
			_name       = name;
			_perfDemo   = perfDemo;
		}

		/**
		 * Do the work...
		 * @return true == keep call doWork()<br>
		 *         false == exit... 
		 */
		public abstract boolean doWork();

		@Override
		public void run()
		{
			Thread.currentThread().setName(_name);
			System.out.println(Thread.currentThread().getName()+ ": Start.");
			boolean running = true;
			while(running)
			{
				try
				{
					running = doWork();
				}
				catch(Throwable t)
				{
					_logger.error(Thread.currentThread().getName()+" Thread had problems, which will be retried. Caught "+t, t);
				}
			}
			System.out.println(Thread.currentThread().getName()+ ": Stopped.");
		}
	}

	private class QueueProducerInfoThread
	extends PerfDemoInfoThread
	{
		public QueueProducerInfoThread(String name, PerfDemo perfDemo)
		{
			super(name, perfDemo);
		}

		@Override
		public boolean doWork()
		{
			try 
			{
				if (_perfDemo._qProdInfoConn != null)
					_perfDemo.refreshQProducerInfo();

				Thread.sleep(1000); 
			}
			catch(InterruptedException ignore) 
			{
				System.out.println(Thread.currentThread().getName()+ ": was interrupted..");
				return false;
			}
			return true; // execute me again
		}
	}

	private class QueueConsumerInfoThread
	extends PerfDemoInfoThread
	{
		public QueueConsumerInfoThread(String name, PerfDemo perfDemo)
		{
			super(name, perfDemo);
		}

		@Override
		public boolean doWork()
		{
			try 
			{
				if (_perfDemo._qConsInfoConn != null)
					_perfDemo.refreshQConsumerInfo();

				Thread.sleep(1000); 
			}
			catch(InterruptedException ignore) 
			{
				System.out.println(Thread.currentThread().getName()+ ": was interrupted..");
				return false;
			}
			return true; // execute me again
		}
	}

	private class QueueStatisticsInfoThread
	extends PerfDemoInfoThread
	{
		public QueueStatisticsInfoThread(String name, PerfDemo perfDemo)
		{
			super(name, perfDemo);
		}

		@Override
		public boolean doWork()
		{
			try 
			{
				if (_perfDemo._qStatInfoConn != null)
					_perfDemo.refreshQStatInfo();

				Thread.sleep(1000); 
			}
			catch(InterruptedException ignore) 
			{
				System.out.println(Thread.currentThread().getName()+ ": was interrupted..");
				return false;
			}
			return true; // execute me again
		}
	}

	private class QueueConsumer
	extends PerfDemoSwingWorker<String, String>
	{
		private PerfDemo   _perfDemo    = null;
		private Connection _conn        = null;
		private int        _consumerId  = -1;
		private boolean    _deadlock    = false;
		private String     _qConsParams = "";
		
		public QueueConsumer(PerfDemo perfDemo, Connection conn, int consumerId, String qConsParams)
		{
			_perfDemo    = perfDemo;
			_conn        = conn;
			_consumerId  = consumerId;
			_qConsParams = qConsParams;
		}

		@Override
		protected String doInBackground() throws Exception
		{
			Thread.currentThread().setName("QueueConsumer-"+_consumerId);
			System.out.println(Thread.currentThread().getName()+ ": Start.");

			if (_conn instanceof SybConnection)
				((SybConnection)_conn).setSybMessageHandler(new PerfDemoSybMessageHandler(Thread.currentThread().getName(), _perfDemo, this));
			
			boolean running = true;
			while(running)
			{
				// DO NOT RESTART
				running = false;  

				_deadlock = false;

				String exitStatus = null;
				String sql = "exec qConsumer "+_qConsParams;
				try
				{
					Statement stmnt = _conn.createStatement();
					ResultSet rs = stmnt.executeQuery(sql);

					while(rs.next())
					{
						exitStatus = rs.getString(1);
					}
					rs.close();
					stmnt.close();
				}
				catch (SQLException e)
				{
					_logger.error("refreshQStatInfo(): problems exec sql '"+sql+"'. Caught "+e);
				}

				if (_deadlock)
				{
					running = true;
					publish("ThreadName='"+Thread.currentThread().getName()+"', UPS... Caught a DEADLOCK, lets RETRY...");
					continue;
				}

				if (exitStatus != null)
				{
					if (exitStatus.equals("STOPPED"))     running = false;
					if (exitStatus.equals("NORMAL_EXIT")) running = false;
				}
				else
				{
					running = false;
				//	running = true;
				}
				
				publish("ThreadName='"+Thread.currentThread().getName()+"', -end-of-exec-do-new-one-");
//				try 
//				{
//					Thread.sleep(1000); 
//					publish("ThreadName='"+Thread.currentThread().getName()+"', Finished sleep, and start a new one.");
//				}
//				catch(InterruptedException ignore) 
//				{
//					running = false;
//					System.out.println(Thread.currentThread().getName()+ ": was interrupted..");
//				}
			}
			_conn.close();
			publish("ThreadName='"+Thread.currentThread().getName()+"', STOPPING, at end of the run() method.");
			return null;
		}
		// isThere_a_STOP_Method____, no it doesnt look like it, maybe use: addPropertyChangeListener(PropertyChangeListener listener)

		@Override
		protected void done()
		{
		}

		@Override
		protected void process(List<String> chunks)
		{
			for (String str : chunks)
			{
//				System.out.println(Thread.currentThread().getName()+ ": process() = '"+str+"'.");
				_perfDemo.appendToLog(Thread.currentThread().getName()+ ": process() = '"+str+"'.\n");
			}
		}
		
	}

	public class PerfDemoSybMessageHandler 
	implements SybMessageHandler
	{
		private PerfDemo      _perfDemo  = null;
		private QueueConsumer _qConsW    = null;
		private String        _logPrefix = "";
		private List<Integer> _discardMsgNum = new LinkedList<Integer>(); // list of <Integer>
		private List<String>  _discardMsgStr = new LinkedList<String>(); // list of <String>

		public PerfDemoSybMessageHandler(String logPrefix, PerfDemo perfDemo, QueueConsumer qCons)
		{
			_perfDemo   = perfDemo;
			_qConsW     = qCons;

			// LogName
			setLogPrefix(logPrefix);

			// MsgNum='10351', Severity='14', Msg: Server user id %d is not a valid user in database '%s'
			addDiscardMsgNum(10351);
			
			// MsgNum='969', Severity='14', Msg: You can access database '%.*s' only from its owner instance '%.*s'. You cannot access local temporary databases from non-owner instances except to use CREATE DATABASE and DROP DATABASE with local system temporary databases.
			// This is a Cluster message, thrown by object_name(id, DBID) if it's a nonlocal tempdb, and the object_name() returns NULL instead
			addDiscardMsgNum(969); 
			
			// Cluster wide monitor command succeeded.
			addDiscardMsgStr("Cluster wide monitor command succeeded.");
		}

		
		public void resetDiscardMsgNum()
		{
			_discardMsgNum.clear();
		}
		public List<Integer> getDiscardMsgNum()
		{
			return _discardMsgNum;
		}
		public void addDiscardMsgNum(int msgNum)
		{
			Integer msgNumInt = new Integer(msgNum);
			if ( ! _discardMsgNum.contains(msgNumInt))
				_discardMsgNum.add(msgNumInt);
		}
		
		public void resetDiscardMsgStr()
		{
			_discardMsgStr.clear();
		}
		public List<String> getDiscardMsgStr()
		{
			return _discardMsgStr;
		}
		public void addDiscardMsgStr(String regexp)
		{
			if ( ! _discardMsgStr.contains(regexp))
				_discardMsgStr.add(regexp);
		}
		
		public void setLogPrefix(String msgPrefix)
		{
			_logPrefix = msgPrefix;
			if (_logPrefix == null)
				_logPrefix = "";

			if ( ! _logPrefix.endsWith(": ") )
				_logPrefix += ": ";
		}

		public String getLogPrefix()
		{
			return _logPrefix;
		}
		
		public SQLException messageHandler(SQLException sqle)
		{
			// Take care of some specific messages...
			int           code    = sqle.getErrorCode();
			Integer       codeInt = new Integer(code);
			String        msgStr  = sqle.getMessage();
			StringBuilder sb      = new StringBuilder();
			StringBuilder logMsg  = new StringBuilder();
			StringBuilder isqlMsg = new StringBuilder();

			if (sqle instanceof EedInfo)
			{
				EedInfo m = (EedInfo) sqle;
//				m.getServerName();
//				m.getSeverity();
//				m.getState();
//				m.getLineNumber();
//				m.getStatus();
//				sqle.getMessage();
//				sqle.getErrorCode();
				
				logMsg.append("Server='")  .append(m.getServerName())   .append("', ");
				logMsg.append("MsgNum='")  .append(sqle.getErrorCode()) .append("', ");
				logMsg.append("Severity='").append(m.getSeverity())     .append("', ");
//				logMsg.append("State='")   .append(m.getState())        .append("', ");
//				logMsg.append("Status='")  .append(m.getStatus())       .append("', ");
				logMsg.append("Proc='")    .append(m.getProcedureName()).append("', ");
				logMsg.append("Line='")    .append(m.getLineNumber())   .append("', ");
				logMsg.append("Msg: ")     .append(sqle.getMessage());
				// If new-line At the end, remove it
				if ( logMsg.charAt(logMsg.length()-1) == '\n' )
					logMsg.deleteCharAt(logMsg.length()-1);

				if (_logger.isDebugEnabled())
					_logger.debug(getLogPrefix() + logMsg.toString());
				
				if (m.getSeverity() <= 10)
				{
					sb.append(sqle.getMessage());
					
					// Discard empty messages
					String str = sb.toString();
					if (str == null || (str != null && str.trim().equals("")) )
						return null;
				}
				else
				{
					// Msg 222222, Level 16, State 1:
					// Server 'GORANS_1_DS', Line 1:
					//	mmmm

					isqlMsg.append("Msg ").append(sqle.getErrorCode())
						.append(", Level ").append(m.getSeverity())
						.append(", State ").append(m.getState())
						.append(":\n");

					boolean addComma = false;
					String str = m.getServerName();
					if ( str != null && !str.equals(""))
					{
						addComma = true;
						isqlMsg.append("Server '").append(str).append("'");
					}

					str = m.getProcedureName();
					if ( str != null && !str.equals(""))
					{
						if (addComma) isqlMsg.append(", ");
						addComma = true;
						isqlMsg.append("Procedure '").append(str).append("'");
					}

					str = m.getLineNumber() + "";
					if ( str != null && !str.equals(""))
					{
						if (addComma) isqlMsg.append(", ");
						addComma = true;
						isqlMsg.append("Line ").append(str).append(":");
						addComma = false;
						isqlMsg.append("\n");
					}
					isqlMsg.append(sqle.getMessage());
				}

				// If new-line At the end, remove it
				if ( isqlMsg.length() > 0 && isqlMsg.charAt(isqlMsg.length()-1) == '\n' )
				{
					isqlMsg.deleteCharAt(isqlMsg.length()-1);
				}
			}
			
			//if (code == 987612) // Just a dummy example
			//{
			//	_logger.info(getPreStr()+"Downgrading " + code + " to a warning");
			//	sqle = new SQLWarning(sqle.getMessage(), sqle.getSQLState(), sqle.getErrorCode());
			//}

			//-------------------------------
			// TREAT DIFFERENT MESSAGES
			//-------------------------------

			// 3604 Duplicate key was ignored.
			if (code == 3604)
			{
//				_logger.debug(getPreStr()+"Ignoring ASE message " + code + ": Duplicate key was ignored.");
//				super.messageAdd("INFO: Ignoring ASE message " + code + ": Duplicate key was ignored.");
//				return null;
			}


			// Not Yet Recovered
			// 921: Database 'xxx' has not been recovered yet - please wait and try again.
			// 950: Database 'xxx' is currently offline. Please wait and try your command again later.
			if (code == 921 || code == 950)
			{
			}

			// DEADLOCK
			if (code == 1205)
			{
				if (_qConsW != null)
					_qConsW._deadlock = true;
			}

			// LOCK-TIMEOUT
			if (code == 12205)
			{
			}


			//
			// Write some extra info in some cases
			//
			// error   severity description
			// ------- -------- -----------
			//    208        16 %.*s not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
			//    504        11 Stored procedure '%.*s' not found.
			//   2501        16 Table named %.*s not found; check sysobjects
			//   2812        16 Stored procedure '%.*s' not found. Specify owner.objectname or use sp_help to check whether the object exists (sp_help may produce lots of output).
			//   9938        16 Table with ID %d not found; check sysobjects.
			//  10303        14 Object named '%.*s' not found; check sysobjects.
			//  10337        16 Object '%S_OBJID' not found.
			//  11901        16 Table '%.*s' was not found.
			//  11910        16 Index '%.*s' was not found.
			//  18826         0 Procedure '%1!' not found.

			if (    code == 208 
			     || code == 504 
			     || code == 2501 
			     || code == 2812 
			     || code == 9938 
			     || code == 10303 
			     || code == 10337 
			     || code == 11901 
			     || code == 11910 
			     || code == 18826 
			   )
			{
//				_logger.info("MessageHandler for SPID "+getSpid()+": Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
//				super.messageAdd("INFO: Current database was '"+this.getDbname()+"' while receiving the above error/warning.");
			}

			// Loop MSG NUM to check if to discard the message
			if (_logger.isDebugEnabled())
				_logger.debug(getLogPrefix() + "INFO Discard message number list: "+_discardMsgNum);

			if (_discardMsgNum.contains(codeInt))
			{
				_logger.debug(getLogPrefix() + ">>>>> Discarding message: "+logMsg.toString());
				return null;
			}

			// Loop MSG STR to check if to discard the message
			if (_logger.isDebugEnabled())
				_logger.debug(getLogPrefix() + "INFO Discard message text list: "+_discardMsgStr);

			for (String regexp : _discardMsgStr)
			{
//				if (msgStr.matches(regexp))
				if (msgStr.indexOf(regexp) >= 0)
				{
					_logger.debug(getLogPrefix() + ">>>>> Discarding message: "+logMsg.toString());
					return null;
				}
			}

			// LOG THE THING...
			String msg = null;
//			if (isqlMsg.length() > 0)
//				msg = isqlMsg.toString();
//			else
				msg = logMsg.toString();

			if (_qConsW != null)
			{
				List<String> logList = new LinkedList<String>();
				logList.add(msg);
				_qConsW.process(logList);
			}
			else if (_perfDemo != null)
			{
				_perfDemo.appendToLog(msg);
			}
			else
			{
				System.out.println(msg);
			}

			// Pass the Exception on.
			return sqle;
		}
	}
	
	@SuppressWarnings("serial")
	private class GTableModel
	extends AbstractTableModel
	{
		protected Vector<Vector<Object>> _rows = new Vector<Vector<Object>>();
		protected Vector<String>         _head = new Vector<String>();
		protected long                   _packAllTime = 0;
		protected long                   _timeBetweenPackAll = 10000;

		public GTableModel(Vector<String> columnHeader)
		{
			_head = columnHeader;
		}

		public boolean timeToPackAll()
		{
			boolean doPackAll = ((System.currentTimeMillis() - _packAllTime) > _timeBetweenPackAll);
			if (doPackAll)
				_packAllTime = System.currentTimeMillis();
			return doPackAll;
		}

		public void setDataVector(Vector<Vector<Object>> rows)
		{
			// make timeToPackAll() == true, when we cange from 0 to any rows...
			if (_rows.size() == 0 && rows.size() > 0)
				_packAllTime = 0;

			_rows = rows;
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column)
		{
			return _head.get(column);
		}

		@Override
		public int getColumnCount()
		{
			return _head.size();
		}

		@Override
		public int getRowCount()
		{
			return _rows.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Vector<Object> row = _rows.get(rowIndex);
			return row.get(columnIndex);
		}
	}

	public static void main(String[] args)
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

//		Configuration conf2 = new Configuration("c:\\projects\\asemon\\asemon.properties");
//		Configuration.setInstance(Configuration.CONF, conf2);

		PerfDemo perfDemo = new PerfDemo("perfdemo");
		perfDemo.setVisible(true);
	}
}
