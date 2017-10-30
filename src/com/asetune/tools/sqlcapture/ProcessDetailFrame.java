/**
*/

package com.asetune.tools.sqlcapture;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import org.apache.log4j.Logger;

import com.asetune.IGuiController;
import com.asetune.Version;
import com.asetune.gui.AsePlanViewer;
import com.asetune.gui.LineNumberedPaper;
import com.asetune.gui.TabularCntrPanel;
import com.asetune.gui.swing.AbstractComponentDecorator;
import com.asetune.gui.swing.GPanel;
import com.asetune.gui.swing.GTabbedPane;
import com.asetune.gui.swing.GTable;
import com.asetune.gui.swing.ListSelectionListenerDeferred;
import com.asetune.sql.JdbcUrlParser;
import com.asetune.sql.conn.ConnectionProp;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.AseConnectionFactory;
import com.asetune.utils.AseConnectionUtils;
import com.asetune.utils.Configuration;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.Memory;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;




public class ProcessDetailFrame 
	extends JFrame 
	implements IGuiController, Memory.MemoryListener
{
	public final static String DISCARD_APP_NAME = "AseTune";

	/** */
    private static final long serialVersionUID = -5013813797380259896L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(ProcessDetailFrame.class);

//	private   JFrame            pdf                          = null;
	private   JLabel            _activeStmntTableCount_lbl     = new JLabel("0 Rows");
	private   JLabel            _activeStmntSqlWhereClause_lbl = new JLabel("Where: ");
//	private   JTextField        _activeStmntSqlWhereClause_txt = new JTextField();
	protected JComboBox<String> _activeStmntSqlWhereClause_cbx = new JComboBox<String>();
	protected JButton           _activeStmntSqlWhereClause_but = new JButton("Remove from template");
	private   JLabel            _activeStmntSqlOrderBy_lbl     = new JLabel("Order By: ");
//	private   JTextField        _activeStmntSqlOrderBy_txt     = new JTextField();
	protected JComboBox<String> _activeStmntSqlOrderBy_cbx     = new JComboBox<String>();
	protected JButton           _activeStmntSqlOrderBy_but     = new JButton("Remove from template");
                                
	protected JButton           _viewXmlPlanInGui_but          = new JButton("XML Plan in GUI");
	protected JCheckBox         _viewXmlPlanInGui_chk          = new JCheckBox("Automatically load XML Plan in GUI", true);
                                
	private   int               spid;
	private   int               kpid;
	public    int               processRefreshInterv         = 1;
	private   BorderLayout      borderLayout1                = new BorderLayout();
	private   JPanel            infoPanel                    = new JPanel();
	private   JPanel            statusPanel                  = new JPanel();
	protected GTabbedPane       mainTabbedPanel              = new GTabbedPane();
	private   JScrollPane       processDetailScroll          = new JScrollPane();
	private   JPanel            statementsPan                = new JPanel();
//private   JFrame            statementsFrame              = new JFrame("Statements");
	private   BorderLayout      borderLayout2                = new BorderLayout();
//	public    TabularCntrPanel  processObjectsPanel          = new TabularCntrPanel("Objects");
//	public    TabularCntrPanel  processWaitsPanel            = new TabularCntrPanel("Waits");
//	public    TabularCntrPanel  processLocksPanel            = new TabularCntrPanel("Locks");
//public    JFrame            processObjectsFrame          = null;
//public    JFrame            processWaitsFrame            = null;
//public    JFrame            processLocksFrame            = null;
//public    JDialog           processObjectsFrame          = null;
//public    JDialog           processWaitsFrame            = null;
//public    JDialog           processLocksFrame            = null;
	private   JPanel            northPan                     = new JPanel();
	private   JScrollPane       activeStatementScrollPan     = new JScrollPane();
	private   JSplitPane        activeSqlCapturedSqlSplitPan = new JSplitPane();
	private   JSplitPane        stmtBatchplanSplitPan        = new JSplitPane();
	private   JScrollPane       historyStatementScrollPan    = new JScrollPane();
	private   JSplitPane        batchPlanSplitPan            = new JSplitPane();
	protected JScrollPane       batchScrollPan               = new JScrollPane();
	protected JScrollPane       planScrollPan                = new JScrollPane();
	//protected JTextArea         batchTextArea                = new JTextArea();
	protected JTextArea         batchTextArea                = new LineNumberedPaper(0,0);
	public    JTextArea         planTextArea                 = new JTextArea();
	private   JLabel            kpidLbl                      = new JLabel();
	private   XYLayout          xYLayout2                    = new XYLayout();
	private   GPanel            historyStatementPan          = new GPanel();
	private   GPanel            historyWhereSqlPanel         = new GPanel();
	private   JLabel            _historyStmntTableCount_lbl  = new JLabel("0 Rows");
	private   int               _historyStmntTableCount_prev = 0;
	private   JLabel            label_historyWhereSql        = new JLabel(" Restrict to: WHERE");
	protected JComboBox<String> comboBox_historyWhereSql     = new JComboBox<String>();
	private   JButton           button_historyWhereSql       = new JButton("Remove from template");
	private   JCheckBox         discardPreOpenStmntsCheckbox = new JCheckBox("Discard historical/captured statements that happened before the dialogue was opened.", true);
	private   JCheckBox         discardAseTuneApp_chk        = new JCheckBox("Discard Statements from application '"+DISCARD_APP_NAME+"'.", true);
                                
	private   JPanel            planPanel                    = new JPanel();
	private   JPanel            batchPanel                   = new JPanel();
	private   JCheckBox         planShowEnableCheckbox       = new JCheckBox("Sample showplan text", false);
	private   JCheckBox         _doExecSpShowplanfull_cbx     = new JCheckBox("<html>Exec: sp_showplanfull <i>SPID</i>, on <i>first</i> row of <b>active</b> statements, in the table below.</html>", true);
	private   JCheckBox         _hideActiveSqlWaitId250_cbx   = new JCheckBox("<html>Hide active SQL Statements with WaitEventID=250 (<i>waiting for incoming network data</i>)</html>", true);
	private   JCheckBox         batchShowEnableCheckbox      = new JCheckBox("Sample SQL batch text", true);
	protected JCheckBox         sqlTextShowProcSrcCheckbox   = new JCheckBox("Show Stored Procedure source code in Batch text window", true);
                                
	private   TitledBorder      titledBorderActive;
	private   TitledBorder      titledBorderPlan;
	private   TitledBorder      titledBorderHistory;
	public    TitledBorder      titledBorderBatch;
//	private   JScrollPane       activeStatementsPan         = new JScrollPane();
	private   GPanel            activeStatementsPan         = new GPanel();
	private   BorderLayout      borderLayout3                = new BorderLayout();
	protected JTextField        kpidFld                      = new JTextField();
	protected JTextField        spidFld                      = new JTextField();
	private   JLabel            spidLbl                      = new JLabel();
	//private   TitledBorder      titledBorder1;

	private RefreshProcess refressProcess;
//	private Connection cnx;
	private DbxConnection cnx;
	private ConnectionProvider _connectionProvider = null;

	@Override 
	public String getTablePopupDbmsVendorString() 
	{ 
		return "ase"; 
	}

	//
//	TableCellRenderer activeStmtRenderer = new DefaultTableCellRenderer()
//	{
//        private static final long serialVersionUID = 5947221518946711279L;
//
//		@Override
//		public Component getTableCellRendererComponent(JTable table,
//		    Object value,boolean isSelected,boolean hasFocus,
//		    int row, int column)
//		{
//
//			Component comp;
//			if (isSelected)
//			{
//				_logger.debug("ACTIVE/CURRENT statement table selected.");
//
//				refressProcess.setSelectedStatement(-1);
//				_historyStatementsTable.clearSelection();
//			}
//
//			comp = super.getTableCellRendererComponent(table,
//					value,isSelected,hasFocus,
//					row, column);//get the component(JLabel?)
//
//			if (value==null)
//				return comp;
//
//			return comp;
//		}
//	};

	//
//	public JXTable activeStatementTable = new JXTable()
	public GTable activeStatementTable = new GTable();
//	public JTable activeStatementTable = new JTable()
//	{
//        private static final long serialVersionUID = 1L;
//
////		@Override
////		public TableCellRenderer getCellRenderer(int row, int column)
////		{
////			return activeStmtRenderer;
////		}
//
//		/**
//		 * To Select/UnSelect rows in a table
//		 * Called when a row/cell is about to change.
//		 * getSelectedRow(), still shows what the *current* selection is
//		 */
//		@Override
//		public void changeSelection(int row, int column, boolean toggle, boolean extend) 
//		{
//			// Toggle rowIsSelected on single selection
//			// if "row we clicked on" is equal to "currently selected row" and also check that we do not do "left/right on keyboard"
//			if (row == getSelectedRow() && (column == getSelectedColumn() || getSelectedColumn() < 0) )
//				toggle = true;
//			
//			super.changeSelection(row, column, toggle, extend);
//		}
//	};


	//
//	TableCellRenderer historyStmtRenderer = new DefaultTableCellRenderer()
//	{
//        private static final long serialVersionUID = -1766679067814631283L;
//
//		@Override
//		public Component getTableCellRendererComponent(JTable table,
//		    Object value,boolean isSelected,boolean hasFocus,
//		    int row, int column)
//		{
//			Component comp;
//			if (isSelected)
//			{
//				_logger.debug("HISTORY statement table selected at row = "+row);
//
//				refressProcess.setSelectedStatement(row);
//				activeStatementTable.clearSelection();
//			}
//
//			comp = super.getTableCellRendererComponent(table,
//					value,isSelected,hasFocus,
//					row, column);//get the component(JLabel?)
//
//			if (value==null)
//				return comp;
//
//			return comp;
//		}
//	};

	//
	public GTable _historyStatementsTable = new GTable();
//	public JTable _historyStatementsTable = new JTable()
//	{
//        private static final long serialVersionUID = 5214078163042572369L;
//
////		@Override
////		public TableCellRenderer getCellRenderer(int row, int column)
////		{
////			return historyStmtRenderer;
////		}
//
//		/**
//		 * To Select/UnSelect rows in a table
//		 * Called when a row/cell is about to change.
//		 * getSelectedRow(), still shows what the *current* selection is
//		 */
//		@Override
//		public void changeSelection(int row, int column, boolean toggle, boolean extend) 
//		{
//			// Toggle rowIsSelected on single selection
//			// if "row we clicked on" is equal to "currently selected row" and also check that we do not do "left/right on keyboard"
//			if (row == getSelectedRow() && (column == getSelectedColumn() || getSelectedColumn() < 0) )
//				toggle = true;
//			
//			super.changeSelection(row, column, toggle, extend);
//		}
//	};

	private BorderLayout borderLayout4           = new BorderLayout();
	private JPanel       middleStatusPan         = new JPanel();
	private BorderLayout borderLayout5           = new BorderLayout();
	private JLabel       serverLbl               = new JLabel();
	private JLabel       statusBarLbl            = new JLabel();
	private JLabel       space                   = new JLabel();
	private JPanel       processDetailPan        = new JPanel();
	private XYLayout     xYLayout3               = new XYLayout();


	private JLabel       statusLbl               = new JLabel();
	private JLabel       suidLbl                 = new JLabel();
	private JLabel       suser_nameLbl           = new JLabel();
	private JLabel       hostnameLbl             = new JLabel();
	private JLabel       hostprocessLbl          = new JLabel();
	private JLabel       cmdLbl                  = new JLabel();
	private JLabel       blockedLbl              = new JLabel();
	private JLabel       dbidLbl                 = new JLabel();
	private JLabel       db_nameLbl              = new JLabel();
	private JLabel       uidLbl                  = new JLabel();
	private JLabel       user_nameLbl            = new JLabel();
	private JLabel       gidLbl                  = new JLabel();
	private JLabel       tran_nameLbl            = new JLabel();
	private JLabel       fidLbl                  = new JLabel();
	private JLabel       execlassLbl             = new JLabel();
	private JLabel       priorityLbl             = new JLabel();
	private JLabel       affinityLbl             = new JLabel();
	private JLabel       origsuidLbl             = new JLabel();
	private JLabel       block_xloidLbl          = new JLabel();
	private JLabel       clientnameLbl           = new JLabel();
	private JLabel       clienthostnameLbl       = new JLabel();
	private JLabel       clientapplnameLbl       = new JLabel();
	private JLabel       sys_idLbl               = new JLabel();
	private JLabel       ses_idLbl               = new JLabel();
	private JLabel       loggedindatetimeLbl     = new JLabel();
	private JLabel       ipaddrLbl               = new JLabel();
	private JLabel       program_nameLbl         = new JLabel();
	public  JTextField   statusFld               = new JTextField();
	public  JTextField   suidFld                 = new JTextField();
	public  JTextField   suser_nameFld           = new JTextField();
	public  JTextField   hostnameFld             = new JTextField();
	public  JTextField   hostprocessFld          = new JTextField();
	public  JTextField   cmdFld                  = new JTextField();
	public  JTextField   blockedFld              = new JTextField();
	public  JTextField   dbidFld                 = new JTextField();
	public  JTextField   db_nameFld              = new JTextField();
	public  JTextField   uidFld                  = new JTextField();
	public  JTextField   user_nameFld            = new JTextField();
	public  JTextField   gidFld                  = new JTextField();
	public  JTextField   tran_nameFld            = new JTextField();
	public  JTextField   fidFld                  = new JTextField();
	public  JTextField   execlassFld             = new JTextField();
	public  JTextField   priorityFld             = new JTextField();
	public  JTextField   affinityFld             = new JTextField();
	public  JTextField   origsuidFld             = new JTextField();
	public  JTextField   block_xloidFld          = new JTextField();
	public  JTextField   clientnameFld           = new JTextField();
	public  JTextField   clienthostnameFld       = new JTextField();
	public  JTextField   clientapplnameFld       = new JTextField();
	public  JTextField   sys_idFld               = new JTextField();
	public  JTextField   ses_idFld               = new JTextField();
	public  JTextField   loggedindatetimeFld     = new JTextField();
	public  JTextField   ipaddrFld               = new JTextField();
	public  JTextField   program_nameFld         = new JTextField();
	private JPanel       networkActPan           = new JPanel();
	private TitledBorder titledBorder3;
	private JLabel       PacketsReceivedLbl      = new JLabel();
	private JLabel       BytesReceivedLbl        = new JLabel();
	private JLabel       PacketsSentLbl          = new JLabel();
	private JLabel       BytesSentLbl            = new JLabel();
	private XYLayout     xYLayout4               = new XYLayout();
	public  JTextField   BytesReceivedFld        = new JTextField();
	public  JTextField   PacketsSentFld          = new JTextField();
	public  JTextField   BytesSentFld            = new JTextField();
	public  JTextField   PacketsReceivedFld      = new JTextField();
	private JLabel       network_pktszLbl        = new JLabel();
	public  JTextField   network_pktszFld        = new JTextField();
	private JPanel       transactionsPan         = new JPanel();
	private TitledBorder titledBorder4;
	private JLabel       ULCFlushFullLbl         = new JLabel();
	private JLabel       TransactionsLbl         = new JLabel();
	private JLabel       UlcFlushLbl             = new JLabel();
	private JLabel       CommitsLbl              = new JLabel();
	private JLabel       UlcBytWriteLbl          = new JLabel();
	private JLabel       RollbacksLbl            = new JLabel();
	public  JTextField   RollbacksFld            = new JTextField();
	public  JTextField   ULCFlushFullFld         = new JTextField();
	public  JTextField   TransactionsFld         = new JTextField();
	public  JTextField   UlcFlushFld             = new JTextField();
	public  JTextField   CommitsFld              = new JTextField();
	public  JTextField   UlcBytWriteFld          = new JTextField();
	private XYLayout     xYLayout5               = new XYLayout();
	private JPanel       activityPan             = new JPanel();
	private TitledBorder titledBorder5;
	private JLabel       LocksHeldLbl            = new JLabel();
	private JLabel       CPUTimeLbl              = new JLabel();
	private JLabel       memusageLbl             = new JLabel();
	private JLabel       cpuLbl                  = new JLabel();
	private JLabel       physical_ioLbl          = new JLabel();
	private XYLayout     xYLayout6               = new XYLayout();
	public  JTextField   LocksHeldFld            = new JTextField();
	public  JTextField   memusageFld             = new JTextField();
	public  JTextField   cpuFld                  = new JTextField();
	public  JTextField   physical_ioFld          = new JTextField();
	public  JTextField   CPUTimeFld              = new JTextField();
	private JLabel       IdxPgsLbl               = new JLabel();
	private JLabel       MemUsageKBLbl           = new JLabel();
	private JLabel       LogicalReadsLbl         = new JLabel();
	private JLabel       PagesWrittenLbl         = new JLabel();
	private JLabel       PhysicalReadsLbl        = new JLabel();
	private JLabel       TmpTblLbl               = new JLabel();
	private JLabel       PagesReadLbl            = new JLabel();
	private JLabel       WaitTimeLbl             = new JLabel();
	private JLabel       ScanPgsLbl              = new JLabel();
	private JLabel       PhysicalWritesLbl       = new JLabel();
	public  JTextField   ScanPgsFld              = new JTextField();
	public  JTextField   PhysicalWritesFld       = new JTextField();
	public  JTextField   IdxPgsFld               = new JTextField();
	public  JTextField   MemUsageKBFld           = new JTextField();
	public  JTextField   LogicalReadsFld         = new JTextField();
	public  JTextField   PagesWrittenFld         = new JTextField();
	public  JTextField   PhysicalReadsFld        = new JTextField();
	public  JTextField   TmpTblFld               = new JTextField();
	public  JTextField   PagesReadFld            = new JTextField();
	public  JTextField   WaitTimeFld             = new JTextField();
	public  JTextField   time_blockedFld         = new JTextField();
	private JLabel       time_blockedLbl         = new JLabel();
	private JPanel       procedurePan            = new JPanel();
	private XYLayout     xYLayout7               = new XYLayout();
	private TitledBorder titledBorder6;
	public  JTextField   procIDFld               = new JTextField();
	public  JTextField   stmtnumFld              = new JTextField();
	public  JTextField   linenumFld              = new JTextField();
	private JLabel       idLbl                   = new JLabel();
	public  JTextField   object_nameFld          = new JTextField();
	private JLabel       stmtnumLbl              = new JLabel();
	private JLabel       linenumLbl              = new JLabel();
	private JLabel       object_nameLbl          = new JLabel();
	private JPanel       identPan                = new JPanel();
	private TitledBorder titledBorder7;
	private JLabel       enginenumLbl            = new JLabel();
	public  JTextField   enginenumFld            = new JTextField();
	private XYLayout     xYLayout8               = new XYLayout();
	private JPanel       procStatusPan           = new JPanel();
	private XYLayout     xYLayout9               = new XYLayout();
	private TitledBorder titledBorder8;
	private JComboBox<String> refreshIntFld     = new JComboBox<String>();
	private JCheckBox    paused_chk              = new JCheckBox("Paused");
	private JLabel       refreshIntLbl           = new JLabel();
//	private JButton      pauseButton             = new JButton();
//	private JButton      resumeButton            = new JButton();
	private JButton      refreshButton           = new JButton();
	private JButton      clearButton             = new JButton();
	private JButton      saveButton              = new JButton();

//	private Image        frameIcon;

//	public ProcessDetailFrame(int k, int in_spid)
	public ProcessDetailFrame(ConnectionProvider connProvider, int SPID, int KPID)
	{
		_connectionProvider = connProvider;
		if (_connectionProvider == null)
			throw new RuntimeException("ProcessDetailFrame: The passed ConnectionProvider was null.");

//		pdf = this;
		try
		{
			spid = SPID;
			kpid = KPID;
			jbInit();
		}
		catch (Exception e)
		{
			_logger.error("In constructor", e);
		}
	}



	private boolean _guiInitialized = false;

	private void jbInit()
	throws Exception
	{
		ImageIcon icon16 = SwingUtils.readImageIcon(Version.class, "images/asetune_icon.gif");
		ImageIcon icon32 = SwingUtils.readImageIcon(Version.class, "images/asetune_icon_32.gif");
		if (icon16 != null || icon32 != null)
		{
			ArrayList<Image> iconList = new ArrayList<Image>();
			if (icon16 != null) iconList.add(icon16.getImage());
			if (icon32 != null) iconList.add(icon32.getImage());

			setIconImages(iconList);
		}
//		if (icon != null)
//			setIconImage(icon.getImage());

		titledBorderActive = new TitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.white, new Color(103, 101, 98), new Color(148, 145, 140)), "Active Statement");
		titledBorderPlan    = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Plan text");
		titledBorderHistory = new TitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.white, new Color(103, 101, 98), new Color(148, 145, 140)), "Historical Statements");
		titledBorderBatch   = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Batch text");

		// titledBorder1 = new TitledBorder("");
		titledBorder3 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Network");
		titledBorder4 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Transactions");
		titledBorder5 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Activity");
		titledBorder6 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Procedure");
		titledBorder7 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Ident");
		titledBorder8 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Status");
		this.setTitle("Process Detail");
		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				this_windowClosing(e);
			}
		});
		Memory.addMemoryListener(this);

		this.getContentPane().setLayout(borderLayout1);
		infoPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		infoPanel.setPreferredSize(new Dimension(10, 40));
		infoPanel.setLayout(xYLayout2);
		statementsPan.setLayout(borderLayout2);
		stmtBatchplanSplitPan.setOrientation(JSplitPane.VERTICAL_SPLIT);
		stmtBatchplanSplitPan.setBorder(null);
		activeSqlCapturedSqlSplitPan.setOrientation(JSplitPane.VERTICAL_SPLIT);
		activeSqlCapturedSqlSplitPan.setBorder(null);
		// batchTextArea.setPreferredSize(new Dimension(10000, 10000));
		batchTextArea.setEditable(false);
		batchTextArea.setBackground(new Color(230, 230, 230));
		batchTextArea.setBorder(BorderFactory.createLoweredBevelBorder());
		batchTextArea.setMinimumSize(new Dimension(10, 10));
		// batchTextArea.setBorder( new
		// LineNumberedBorder(LineNumberedBorder.LEFT_SIDE,
		// LineNumberedBorder.LEFT_JUSTIFY) );
		// ((LineNumberedPaper)batchTextArea).setLineNumberJustification(LineNumberedPaper.LEFT_JUSTIFY);
		// planTextArea.setPreferredSize(new Dimension(10000, 10000));
		planTextArea.setEditable(false);
		planTextArea.setBackground(new Color(230, 230, 230));
		planTextArea.setBorder(BorderFactory.createLoweredBevelBorder());
		planTextArea.setMinimumSize(new Dimension(10, 10));
		mainTabbedPanel.setPreferredSize(new Dimension(1024, 520));
		historyStatementScrollPan.getViewport().setBackground(new Color(230, 230, 230));
		historyStatementScrollPan.setBorder(titledBorderHistory);
		historyStatementScrollPan.setPreferredSize(new Dimension(454, 500));
		processDetailScroll.setBorder(BorderFactory.createLoweredBevelBorder());
		kpidLbl.setText("KPID :");
		northPan.setPreferredSize(new Dimension(10, 90));
		northPan.setLayout(borderLayout3);
//		activeStatementsPan.getViewport().setBackground(new Color(230, 230, 230));
		activeStatementsPan.setBorder(titledBorderActive);

		_historyStatementsTable.setBackground(new Color(230, 230, 230));
		_historyStatementsTable.setShowHorizontalLines(false);
		_historyStatementsTable.setShowVerticalLines(false);
		_historyStatementsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_historyStatementsTable.setGridColor(new Color(230, 230, 230));
		_historyStatementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_historyStatementsTable.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				historyStatementsTable_mouseClicked(e);
			}
		});
		// Selection listener
		_historyStatementsTable.getSelectionModel().addListSelectionListener(new ListSelectionListenerDeferred()
		{
			
			@Override
			public void deferredValueChanged(ListSelectionEvent event)
			{
				int vrow = _historyStatementsTable.getSelectedRow();
//System.out.println("_historyStatementsTable.valueChanged(): vrow="+vrow);
	
				refressProcess.setSelectedStatement(vrow);
				activeStatementTable.clearSelection();

//				if (vrow >= 0)
//				{
//					int mrow = _historyStatementsTable.convertRowIndexToModel(vrow);
//					TableModel tm = _historyStatementsTable.getModel();
//				}
			}
		});
		
		// Fixing/setting background selection color... on some platforms it seems to be a strange color
		// on XP a gray color of "r=178,g=180,b=191" is the default, which looks good on the screen
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
		Configuration conf = Configuration.getCombinedConfiguration();
		if (conf != null)
		{
			if (conf.getBooleanProperty("table.setSelectionBackground", true))
			{
				Color newBg = new Color(
					conf.getIntProperty("table.setSelectionBackground.r", 178),
					conf.getIntProperty("table.setSelectionBackground.g", 180),
					conf.getIntProperty("table.setSelectionBackground.b", 191));

				_logger.debug("table.setSelectionBackground("+newBg+").");
				_historyStatementsTable.setSelectionBackground(newBg);
			}
		}
		else
		{
			Color bgc = _historyStatementsTable.getSelectionBackground();
			if ( ! (bgc.getRed()==178 && bgc.getGreen()==180 && bgc.getBlue()==191) )
			{
				Color newBg = new Color(178, 180, 191);
				_logger.debug("table.setSelectionBackground("+newBg+"). Config could not be read, trusting defaults...");
				_historyStatementsTable.setSelectionBackground(newBg);
			}
		}

		kpidFld.setEditable(false);
		kpidFld.setBorder(BorderFactory.createLoweredBevelBorder());
		kpidFld.setBackground(new Color(230, 230, 230));
		spidFld.setBackground(new Color(230, 230, 230));
		spidFld.setBorder(BorderFactory.createLoweredBevelBorder());
		spidFld.setEditable(false);
		spidLbl.setText("SPID :");
		batchScrollPan.setBorder(titledBorderBatch);
		batchScrollPan.setPreferredSize(new Dimension(400, 40));
		planScrollPan.setBorder(titledBorderPlan);
		planScrollPan.setPreferredSize(new Dimension(204, 40));
		batchPlanSplitPan.setBorder(null);
		activeStatementTable.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				activeStatementTable_mouseClicked(e);
			}
		});
		activeStatementTable.setGridColor(new Color(230, 230, 230));
		activeStatementTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		activeStatementTable.setShowVerticalLines(false);
		activeStatementTable.setShowHorizontalLines(false);
		activeStatementTable.setBackground(new Color(230, 230, 230));
		activeStatementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		// Selection listener
		activeStatementTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
//				JTable tab = (JTable)e.getSource();
				int vrow = activeStatementTable.getSelectedRow();
	
				refressProcess.setSelectedStatement(-1);
				_historyStatementsTable.clearSelection();

//				if (vrow >= 0)
//				{
//					int mrow = activeStatementTable.convertRowIndexToModel(vrow);
//					TableModel tm = activeStatementTable.getModel();
//				}
			}
		});
		
//		activeStatementTable.setSortable(true);
//
//		// Set special Render to print multiple columns sorts
//		activeStatementTable.getTableHeader().setDefaultRenderer(new MultiSortTableCellHeaderRenderer());
//
//		//--------------------------------------------------------------------
//		// New SORTER that toggles from DESCENDING -> ASCENDING -> UNSORTED
//		//--------------------------------------------------------------------
//		activeStatementTable.setSortOrderCycle(SortOrder.DESCENDING, SortOrder.ASCENDING, SortOrder.UNSORTED);

		

		// Fixing/setting background selection color... on some platforms it seems to be a strange color
		// on XP a gray color of "r=178,g=180,b=191" is the default, which looks good on the screen
		//Configuration conf = Configuration.getInstance(Configuration.CONF);
		if (conf != null)
		{
			if (conf.getBooleanProperty("table.setSelectionBackground", true))
			{
				Color newBg = new Color(
					conf.getIntProperty("table.setSelectionBackground.r", 178),
					conf.getIntProperty("table.setSelectionBackground.g", 180),
					conf.getIntProperty("table.setSelectionBackground.b", 191));

				_logger.debug("table.setSelectionBackground("+newBg+").");
				activeStatementTable.setSelectionBackground(newBg);
			}
		}
		else
		{
			Color bgc = activeStatementTable.getSelectionBackground();
			if ( ! (bgc.getRed()==178 && bgc.getGreen()==180 && bgc.getBlue()==191) )
			{
				Color newBg = new Color(178, 180, 191);
				_logger.debug("table.setSelectionBackground("+newBg+"). Config could not be read, trusting defaults...");
				activeStatementTable.setSelectionBackground(newBg);
			}
		}
		
		
		
		statusPanel.setLayout(borderLayout4);
		statusPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		statusPanel.setPreferredSize(new Dimension(10, 21));
		middleStatusPan.setLayout(borderLayout5);
		serverLbl.setBorder(BorderFactory.createLoweredBevelBorder());
		serverLbl.setPreferredSize(new Dimension(300, 17));
		statusBarLbl.setBorder(BorderFactory.createLoweredBevelBorder());
		statusBarLbl.setPreferredSize(new Dimension(7, 17));
		statusBarLbl.setText(" ");
		space.setText(" ");
		middleStatusPan.setPreferredSize(new Dimension(1000, 17));
		processDetailPan.setLayout(xYLayout3);
		statusLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		statusLbl.setText("Status :");
		suidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		suidLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		suidLbl.setText("Suid :");
		suser_nameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		suser_nameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		suser_nameLbl.setText("Suser_name :");
		hostnameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		hostnameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		hostnameLbl.setText("Hostname :");
		hostprocessLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		hostprocessLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		hostprocessLbl.setText("Hostprocess :");
		cmdLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		cmdLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		cmdLbl.setText("Command  :");
		blockedLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		blockedLbl.setText("Blocked :");
		dbidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		dbidLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		dbidLbl.setText("Dbid :");
		db_nameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		db_nameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		db_nameLbl.setText("DB name :");
		uidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		uidLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		uidLbl.setText("UID :");
		user_nameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		user_nameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		user_nameLbl.setText("Username :");
		gidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		gidLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		gidLbl.setText("Gid :");
		tran_nameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		tran_nameLbl.setText("Tran name :");
		fidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		fidLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		fidLbl.setText("Family ID :");
		execlassLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		execlassLbl.setText("Execlass :");
		priorityLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		priorityLbl.setText("Priority :");
		affinityLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		affinityLbl.setText("Affinity :");
		origsuidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		origsuidLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		origsuidLbl.setText("Orig SUID :");
		block_xloidLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		block_xloidLbl.setText("Block_xloid :");
		clientnameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		clientnameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		clientnameLbl.setText("Client name :");
		clienthostnameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		clienthostnameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		clienthostnameLbl.setText("Client hostname :");
		clientapplnameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		clientapplnameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		clientapplnameLbl.setText("Client applname :");
		sys_idLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		sys_idLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		sys_idLbl.setText("Sys id :");
		ses_idLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		ses_idLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		ses_idLbl.setText("Sess ID :");
		loggedindatetimeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		loggedindatetimeLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		loggedindatetimeLbl.setText("Loggedin datetime :");
		ipaddrLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		ipaddrLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		ipaddrLbl.setText("Ipaddress :");
		program_nameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		program_nameLbl.setHorizontalTextPosition(SwingConstants.RIGHT);
		program_nameLbl.setText("Appl name :");
		networkActPan.setBorder(titledBorder3);
		networkActPan.setLayout(xYLayout4);
		PacketsReceivedLbl.setText("Pkt received :");
		BytesReceivedLbl.setText("Bytes received :");
		PacketsSentLbl.setText("Pkt sent :");
		BytesSentLbl.setText("Bytes sent :");
		network_pktszLbl.setText("Network Pktsize :");
		transactionsPan.setBorder(titledBorder4);
		transactionsPan.setLayout(xYLayout5);
		ULCFlushFullLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		ULCFlushFullLbl.setText("ULC flush full :");
		TransactionsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		TransactionsLbl.setText("Transactions :");
		UlcFlushLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		UlcFlushLbl.setText("ULC Flush :");
		CommitsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		CommitsLbl.setText("Commits :");
		UlcBytWriteLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		UlcBytWriteLbl.setText("ULC bytes write :");
		RollbacksLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		RollbacksLbl.setText("Rollbacks :");
		activityPan.setBorder(titledBorder5);
		activityPan.setLayout(xYLayout6);
		LocksHeldLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		LocksHeldLbl.setText("Locks held :");
		CPUTimeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		CPUTimeLbl.setText("CPU time :");
		memusageLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		memusageLbl.setText("Memusage :");
		cpuLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		cpuLbl.setText("Cpu :");
		physical_ioLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		physical_ioLbl.setText("Physical IO :");
		IdxPgsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		IdxPgsLbl.setText("Idx pages :");
		MemUsageKBLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		MemUsageKBLbl.setText("MemUsage KB :");
		LogicalReadsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		LogicalReadsLbl.setText("Logical Reads :");
		PagesWrittenLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		PagesWrittenLbl.setText("Page written :");
		PhysicalReadsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		PhysicalReadsLbl.setText("Physical reads :");
		TmpTblLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		TmpTblLbl.setText("Tmp tables :");
		PagesReadLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		PagesReadLbl.setText("Page reads :");
		WaitTimeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		WaitTimeLbl.setText("Wait Time :");
		ScanPgsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		ScanPgsLbl.setText("Scan pages :");
		PhysicalWritesLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		PhysicalWritesLbl.setText("Physical writes :");
		time_blockedLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		time_blockedLbl.setText("Time blocked :");
		procedurePan.setLayout(xYLayout7);
		procedurePan.setBorder(titledBorder6);
		idLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		idLbl.setText("Proc ID :");
		stmtnumLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		stmtnumLbl.setText("Stmt num :");
		linenumLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		linenumLbl.setText("Line num :");
		object_nameLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		object_nameLbl.setText("Proc name :");
		identPan.setBorder(titledBorder7);
		identPan.setLayout(xYLayout8);
		enginenumLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		enginenumLbl.setText("Enginenum :");
		procStatusPan.setLayout(xYLayout9);
		procStatusPan.setBorder(titledBorder8);
		suidFld.setBackground(new Color(230, 230, 230));
		suidFld.setMinimumSize(new Dimension(4, 17));
		suidFld.setPreferredSize(new Dimension(4, 17));
		suidFld.setEditable(false);
		suser_nameFld.setBackground(new Color(230, 230, 230));
		suser_nameFld.setMinimumSize(new Dimension(4, 17));
		suser_nameFld.setPreferredSize(new Dimension(4, 17));
		suser_nameFld.setEditable(false);
		dbidFld.setBackground(new Color(230, 230, 230));
		dbidFld.setMinimumSize(new Dimension(4, 17));
		dbidFld.setPreferredSize(new Dimension(4, 17));
		dbidFld.setEditable(false);
		db_nameFld.setBackground(new Color(230, 230, 230));
		db_nameFld.setMinimumSize(new Dimension(4, 17));
		db_nameFld.setPreferredSize(new Dimension(4, 17));
		db_nameFld.setEditable(false);
		uidFld.setBackground(new Color(230, 230, 230));
		uidFld.setMinimumSize(new Dimension(4, 17));
		uidFld.setPreferredSize(new Dimension(4, 17));
		uidFld.setEditable(false);
		user_nameFld.setBackground(new Color(230, 230, 230));
		user_nameFld.setMinimumSize(new Dimension(4, 17));
		user_nameFld.setPreferredSize(new Dimension(4, 17));
		user_nameFld.setEditable(false);
		origsuidFld.setBackground(new Color(230, 230, 230));
		origsuidFld.setMinimumSize(new Dimension(4, 17));
		origsuidFld.setPreferredSize(new Dimension(4, 17));
		origsuidFld.setEditable(false);
		gidFld.setBackground(new Color(230, 230, 230));
		gidFld.setMinimumSize(new Dimension(4, 17));
		gidFld.setPreferredSize(new Dimension(4, 17));
		gidFld.setEditable(false);
		fidFld.setBackground(new Color(230, 230, 230));
		fidFld.setMinimumSize(new Dimension(4, 17));
		fidFld.setPreferredSize(new Dimension(4, 17));
		fidFld.setEditable(false);
		hostnameFld.setBackground(new Color(230, 230, 230));
		hostnameFld.setMinimumSize(new Dimension(4, 17));
		hostnameFld.setPreferredSize(new Dimension(4, 17));
		hostnameFld.setEditable(false);
		clienthostnameFld.setBackground(new Color(230, 230, 230));
		clienthostnameFld.setMinimumSize(new Dimension(4, 17));
		clienthostnameFld.setPreferredSize(new Dimension(4, 17));
		clienthostnameFld.setEditable(false);
		clientnameFld.setBackground(new Color(230, 230, 230));
		clientnameFld.setMinimumSize(new Dimension(4, 17));
		clientnameFld.setPreferredSize(new Dimension(4, 17));
		clientnameFld.setEditable(false);
		clientapplnameFld.setBackground(new Color(230, 230, 230));
		clientapplnameFld.setMinimumSize(new Dimension(4, 17));
		clientapplnameFld.setPreferredSize(new Dimension(4, 17));
		clientapplnameFld.setEditable(false);
		loggedindatetimeFld.setBackground(new Color(230, 230, 230));
		loggedindatetimeFld.setMinimumSize(new Dimension(4, 17));
		loggedindatetimeFld.setPreferredSize(new Dimension(4, 17));
		loggedindatetimeFld.setEditable(false);
		ses_idFld.setBackground(new Color(230, 230, 230));
		ses_idFld.setMinimumSize(new Dimension(4, 17));
		ses_idFld.setPreferredSize(new Dimension(4, 17));
		ses_idFld.setEditable(false);
		sys_idFld.setBackground(new Color(230, 230, 230));
		sys_idFld.setMinimumSize(new Dimension(4, 17));
		sys_idFld.setPreferredSize(new Dimension(4, 17));
		sys_idFld.setEditable(false);
		hostprocessFld.setBackground(new Color(230, 230, 230));
		hostprocessFld.setMinimumSize(new Dimension(4, 17));
		hostprocessFld.setPreferredSize(new Dimension(4, 17));
		hostprocessFld.setEditable(false);
		ipaddrFld.setBackground(new Color(230, 230, 230));
		ipaddrFld.setMinimumSize(new Dimension(4, 17));
		ipaddrFld.setPreferredSize(new Dimension(4, 17));
		ipaddrFld.setEditable(false);
		program_nameFld.setBackground(new Color(230, 230, 230));
		program_nameFld.setMinimumSize(new Dimension(4, 17));
		program_nameFld.setPreferredSize(new Dimension(4, 17));
		program_nameFld.setEditable(false);
		cmdFld.setBackground(new Color(230, 230, 230));
		cmdFld.setMinimumSize(new Dimension(4, 17));
		cmdFld.setPreferredSize(new Dimension(4, 17));
		cmdFld.setEditable(false);
		statusFld.setBackground(new Color(230, 230, 230));
		statusFld.setEditable(false);
		enginenumFld.setBackground(new Color(230, 230, 230));
		enginenumFld.setEditable(false);
		affinityFld.setBackground(new Color(230, 230, 230));
		affinityFld.setEditable(false);
		blockedFld.setBackground(new Color(230, 230, 230));
		blockedFld.setEditable(false);
		block_xloidFld.setBackground(new Color(230, 230, 230));
		block_xloidFld.setEditable(false);
		execlassFld.setBackground(new Color(230, 230, 230));
		execlassFld.setEditable(false);
		priorityFld.setBackground(new Color(230, 230, 230));
		priorityFld.setEditable(false);
		memusageFld.setBackground(new Color(230, 230, 230));
		memusageFld.setEditable(false);
		cpuFld.setBackground(new Color(230, 230, 230));
		cpuFld.setEditable(false);
		CPUTimeFld.setBackground(new Color(230, 230, 230));
		CPUTimeFld.setEditable(false);
		LocksHeldFld.setBackground(new Color(230, 230, 230));
		LocksHeldFld.setEditable(false);
		MemUsageKBFld.setBackground(new Color(230, 230, 230));
		MemUsageKBFld.setEditable(false);
		WaitTimeFld.setBackground(new Color(230, 230, 230));
		WaitTimeFld.setEditable(false);
		LogicalReadsFld.setBackground(new Color(230, 230, 230));
		LogicalReadsFld.setEditable(false);
		PagesReadFld.setBackground(new Color(230, 230, 230));
		PagesReadFld.setEditable(false);
		PhysicalReadsFld.setBackground(new Color(230, 230, 230));
		PhysicalReadsFld.setEditable(false);
		PagesWrittenFld.setBackground(new Color(230, 230, 230));
		PagesWrittenFld.setEditable(false);
		PhysicalWritesFld.setBackground(new Color(230, 230, 230));
		PhysicalWritesFld.setEditable(false);
		physical_ioFld.setBackground(new Color(230, 230, 230));
		physical_ioFld.setEditable(false);
		IdxPgsFld.setBackground(new Color(230, 230, 230));
		IdxPgsFld.setEditable(false);
		ScanPgsFld.setBackground(new Color(230, 230, 230));
		ScanPgsFld.setEditable(false);
		time_blockedFld.setBackground(new Color(230, 230, 230));
		time_blockedFld.setEditable(false);
		TmpTblFld.setBackground(new Color(230, 230, 230));
		TmpTblFld.setEditable(false);
		procIDFld.setBackground(new Color(230, 230, 230));
		procIDFld.setEditable(false);
		object_nameFld.setBackground(new Color(230, 230, 230));
		object_nameFld.setEditable(false);
		stmtnumFld.setBackground(new Color(230, 230, 230));
		stmtnumFld.setEditable(false);
		linenumFld.setBackground(new Color(230, 230, 230));
		linenumFld.setEditable(false);
		tran_nameFld.setBackground(new Color(230, 230, 230));
		tran_nameFld.setEditable(false);
		TransactionsFld.setBackground(new Color(230, 230, 230));
		TransactionsFld.setEditable(false);
		CommitsFld.setBackground(new Color(230, 230, 230));
		CommitsFld.setEditable(false);
		RollbacksFld.setBackground(new Color(230, 230, 230));
		RollbacksFld.setEditable(false);
		UlcFlushFld.setBackground(new Color(230, 230, 230));
		UlcFlushFld.setEditable(false);
		ULCFlushFullFld.setBackground(new Color(230, 230, 230));
		ULCFlushFullFld.setEditable(false);
		UlcBytWriteFld.setBackground(new Color(230, 230, 230));
		UlcBytWriteFld.setEditable(false);
		refreshIntLbl.setText("Refresh Interv. (s) :");
		refreshIntFld.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				refreshIntFld_actionPerformed(e);
			}
		});
		paused_chk.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				pause_actionPerformed(e);
			}
		});
//		pauseButton.setText("Pause");
//		pauseButton.addActionListener(new java.awt.event.ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				pauseButton_actionPerformed(e);
//			}
//		});
//		resumeButton.setVisible(false);
//		resumeButton.setText("Resume");
//		resumeButton.addActionListener(new java.awt.event.ActionListener()
//		{
//			public void actionPerformed(ActionEvent e)
//			{
//				resumeButton_actionPerformed(e);
//			}
//		});
		refreshButton.setText("Refresh");
		refreshButton.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				refreshButton_actionPerformed(e);
			}
		});
		clearButton.setText("Clear");
		clearButton.setToolTipText("Clear information in all Table below.");
		clearButton.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clear();
			}
		});
		
//		String envNameSaveDir = DbxTune.getInstance().getAppSaveDirEnvName();  // ASETUNE_SAVE_DIR
//		String envNameHomeDir = DbxTune.getInstance().getAppHomeEnvName();     // ASETUNE_HOME
		String envNameSaveDir = "DBXTUNE_SAVE_DIR";
		String envNameHomeDir = "DBXTUNE_HOME";

		saveButton.setText("Save & Clear");
		saveButton.setToolTipText(
			"<html>" +
				"Save Captured/Historical Statements Table to files<br>" +
				"<ul>" +
				" <li> historyStmts.YYYY-MM-DD.bcp     </li>" +
				" <li> historyStmts.YYYY-MM-DD.txt     </li>" +
				" <li> historyStmts.YYYY-MM-DD.ddl.sql </li>" +
				"</ul>" +
				"in Directory "+envNameSaveDir+" or "+envNameHomeDir+"<br>" +
				envNameSaveDir + " = " + StringUtil.getEnvVariableValue(envNameSaveDir) + "<br>" +
				envNameHomeDir + " = " + StringUtil.getEnvVariableValue(envNameHomeDir) + "<br>" +
			"</html>");
		saveButton.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveHistoryStatements();
			}
		});
		
		this.getContentPane().add(infoPanel, BorderLayout.NORTH);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		this.getContentPane().add(mainTabbedPanel, BorderLayout.CENTER);

		if (spid > 0 || kpid > 0)
		{
			ImageIcon sumTabIcon   = SwingUtils.readImageIcon(Version.class, "images/sum.png");
			ImageIcon stmntTabIcon = SwingUtils.readImageIcon(Version.class, "images/sqlStatements.png");
			
			mainTabbedPanel.addTab("Summary",    sumTabIcon,   processDetailScroll, "Counters for a specififc SPID");
			mainTabbedPanel.addTab("Statements", stmntTabIcon, statementsPan,       "Active SQL Statements that are executing right now, Historical SQL Stataments that has been executed previously");
//			mainTabbedPanel.add(processDetailScroll, "Summary");
//			mainTabbedPanel.add(statementsPan,       "Statements");
//			mainTabbedPanel.add(processObjectsPanel, processObjectsPanel.getPanelName());
//			mainTabbedPanel.add(processWaitsPanel,   processWaitsPanel.getPanelName());
//			mainTabbedPanel.add(processLocksPanel,   processLocksPanel.getPanelName());
		}
		else
		{
			ImageIcon stmntTabIcon = SwingUtils.readImageIcon(Version.class, "images/sqlStatements.png");

			mainTabbedPanel.addTab("Statements", stmntTabIcon, statementsPan, "Active SQL Statements that are executing right now, Historical SQL Stataments that has been executed previously");
//			mainTabbedPanel.add(statementsPan,       "Statements");
//			mainTabbedPanel.add(processObjectsPanel, processObjectsPanel.getPanelName());
//			mainTabbedPanel.add(processWaitsPanel,   processWaitsPanel.getPanelName());
//			mainTabbedPanel.add(processLocksPanel,   processLocksPanel.getPanelName());
		}

//		mainTabbedPanel.addMouseListener(new MouseAdapter()
//		{
//			public void mouseClicked(MouseEvent e)
//			{
//				mainTabbedPanel_mouseClicked(e);
//			}
//		});
//statementsFrame    .getContentPane().add(statementsPan);
//processObjectsFrame.getContentPane().add(processObjectsPanel);
//processWaitsFrame  .getContentPane().add(processWaitsPanel);
//processLocksFrame  .getContentPane().add(processLocksPanel);
//		processObjectsFrame = new JFrame(processObjectsPanel.getPanelName());
//		processObjectsFrame.setIconImage(frameIcon);
//		processObjectsFrame.setSize(new Dimension(900, 300));
//		processObjectsFrame.addWindowListener(new WindowAdapter()
//		{
//			public void windowClosing(WindowEvent e)
//			{
//				mainTabbedPanel.add(processObjectsPanel.getPanelName(), processObjectsPanel);
//			}
//		});
//
//		processWaitsFrame = new JFrame(processWaitsPanel.getPanelName());
//		processWaitsFrame.setIconImage(frameIcon);
//		processWaitsFrame.setSize(new Dimension(750, 500));
//		processWaitsFrame.addWindowListener(new WindowAdapter()
//		{
//			public void windowClosing(WindowEvent e)
//			{
//				mainTabbedPanel.add(processWaitsPanel.getPanelName(), processWaitsPanel);
//			}
//		});
//
//		processLocksFrame = new JFrame(processLocksPanel.getPanelName());
//		processLocksFrame.setIconImage(frameIcon);
//		processLocksFrame.setSize(new Dimension(900, 300));
//		processLocksFrame.addWindowListener(new WindowAdapter()
//		{
//			public void windowClosing(WindowEvent e)
//			{
//				mainTabbedPanel.add(processLocksPanel.getPanelName(), processLocksPanel);
//			}
//		});


		processDetailScroll.getViewport().add(processDetailPan, null);
		statementsPan.add(stmtBatchplanSplitPan, BorderLayout.CENTER);

		stmtBatchplanSplitPan.add(activeSqlCapturedSqlSplitPan, JSplitPane.TOP);
		stmtBatchplanSplitPan.add(batchPlanSplitPan, JSplitPane.BOTTOM);

//		activeSqlCapturedSqlSplitPan.add(activeStatementsPan, JSplitPane.TOP);
		activeSqlCapturedSqlSplitPan.add(historyStatementPan, JSplitPane.BOTTOM);

		// SQL BATCH
		batchShowEnableCheckbox.setToolTipText("<html>Get SQL Statements from monSysSQLText<br>Note: Even if sp_configure 'sql text pipe active' is not configured. SQL from an ACTIVE SQL Statement might be visible.</html>");
		sqlTextShowProcSrcCheckbox.setToolTipText("Get the Stored Procedure text and display that (if it was a procedure that was executed)");
		batchPlanSplitPan.add(batchPanel, JSplitPane.LEFT);
//		batchPanel.setLayout(new BorderLayout());
//		batchPanel.add(batchShowEnableCheckbox, BorderLayout.NORTH);
//batchPanel.add(sqlTextShowProcSrcCheckbox, BorderLayout.SOUTH);
//		batchPanel.add(batchScrollPan, BorderLayout.CENTER);
		batchPanel.setLayout(new MigLayout("insets 0"));
		batchPanel.add(batchShowEnableCheckbox,    "");
		batchPanel.add(sqlTextShowProcSrcCheckbox, "wrap");
		batchPanel.add(batchScrollPan,             "span, grow, push");
		batchScrollPan.getViewport().add(batchTextArea, null);
		batchShowEnableCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setSqlTextSample( batchShowEnableCheckbox.isSelected() );
				saveProps();
			}
		});
		sqlTextShowProcSrcCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
			}
		});

		// PLAN
		_viewXmlPlanInGui_but      .setToolTipText("Opens a GUI window and load the current/below XML plan in that tool");
		_viewXmlPlanInGui_chk      .setToolTipText("Automatically Load the XML Plan in the ASE Plan Viewer GUI when the row is selected and it has an XML Plan attached to it.");
		_doExecSpShowplanfull_cbx  .setToolTipText("<html>Execute the procedure sp_showplanfull <i>SPID</i> on the first row in the <i>active</i> SQL Table.<br>This will enable you to see Showplan of the current running SQL Statement even if sp_configure 'plan text pipe active' and 'plan text pipe max messages' is turned off.</html>");
		_hideActiveSqlWaitId250_cbx.setToolTipText("<html>In case the Active SQL table contains WaitEventId=250 and has been waiting for more than 60 seconds... <br>Hide those statements... This is a workaround for some bug in some ASE Versions</html>");
		planShowEnableCheckbox.setToolTipText("<html>Get SQL Showplans from monSysPlanText<br>Note: Even if sp_configure 'plan text pipe active' is not configured. Showplans from an ACTIVE SQL Statement might be visible.</html>");
		batchPlanSplitPan.add(planPanel, JSplitPane.RIGHT);
//		planPanel.setLayout(new BorderLayout());
//		planPanel.add(planShowEnableCheckbox, BorderLayout.NORTH);
//		planPanel.add(planScrollPan, BorderLayout.CENTER);
		planPanel.setLayout(new MigLayout("insets 0 0 0 0"));
		planPanel.add(planShowEnableCheckbox,   "");
		planPanel.add(_viewXmlPlanInGui_chk,    "");
		planPanel.add(_viewXmlPlanInGui_but,    "tag right, hidemode 0, wrap");
//		planPanel.add(_doExecSpShowplanfull_cbx, "wrap");
		planPanel.add(planScrollPan, "span, push, grow");
		planScrollPan.getViewport().add(planTextArea, null);
		planShowEnableCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setPlanTextSample( planShowEnableCheckbox.isSelected() );
				saveProps();
			}
		});
		_doExecSpShowplanfull_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
			}
		});
		_hideActiveSqlWaitId250_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
			}
		});
		_viewXmlPlanInGui_but.setVisible(false);
		_viewXmlPlanInGui_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String xmlPlan = planTextArea.getText();

				// Remove "extra" stuff before the XML Plan
				int xmlPlanStartPos = xmlPlan.indexOf("<?xml version");
				if (xmlPlanStartPos > 0)
				{
					xmlPlan = xmlPlan.substring(xmlPlanStartPos);
				}

				// Do we still have a plan
				if (xmlPlan.startsWith("<?xml version"))
				{
					AsePlanViewer.getInstance().loadXml(xmlPlan);
//					AsePlanViewer pv = new AsePlanViewer(xmlPlan);
//					pv.setVisible(true);
				}
				else
				{
					int len = Math.min(50, xmlPlan.length());
					String startOf = xmlPlan.substring(0, len);
					String htmlMsg = 
						"<html>" +
						"<b>This doesn't seem to be a XML string.<b>" +
						"<pre>" +
						startOf +
						"</pre>" +
						"</html>";
					SwingUtils.showErrorMessage("No XML input found", htmlMsg, null);
				}
			}
		});


//		_activeStmntSqlWhereClause_txt.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				saveProps();
//			}
//		});
//		_activeStmntSqlOrderBy_txt.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent e)
//			{
//				saveProps();
//			}
//		});
		
		_activeStmntSqlWhereClause_cbx.setToolTipText("Add your extra where clauses on the monXXX table. make sure that only columns in that table are used. "+Version.getAppName()+"'s errorlog will show faulty SQL statements.");
		_activeStmntSqlWhereClause_cbx.setEditable(true);
		_activeStmntSqlWhereClause_cbx.addItem("");
		_activeStmntSqlWhereClause_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action_activeStmntSqlWhereClause_cbx(e);
			}
		});
		_activeStmntSqlWhereClause_but.setToolTipText("Remove the 'extra where' from the template.");
		_activeStmntSqlWhereClause_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action_activeStmntSqlWhereClause_but(e);
			}
		});

		_activeStmntSqlOrderBy_cbx.setToolTipText("Change 'order by' clauses on the monXXX table. "+Version.getAppName()+"'s errorlog will show faulty SQL statements.");
		_activeStmntSqlOrderBy_cbx.setEditable(true);
		_activeStmntSqlOrderBy_cbx.addItem("");
		_activeStmntSqlOrderBy_cbx.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action_activeStmntSqlOrderBy_cbx(e);
			}
		});
		_activeStmntSqlOrderBy_but.setToolTipText("Remove the 'order by' from the template.");
		_activeStmntSqlOrderBy_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				action_activeStmntSqlOrderBy_but(e);
			}
		});

		JPanel xxx = new JPanel(new MigLayout("insets 0 0 0 0"));
		xxx.add(_doExecSpShowplanfull_cbx,        "gap 5, split, span");
//		xxx.add(new JLabel(),                     "pushx, growx");
		xxx.add(_hideActiveSqlWaitId250_cbx,      "");
		xxx.add(_activeStmntTableCount_lbl,       "tag right, wrap");
		
		xxx.add(_activeStmntSqlWhereClause_lbl,  "gap 5 5 0 0");
		xxx.add(_activeStmntSqlWhereClause_cbx,  "growx, pushx");
		xxx.add(_activeStmntSqlWhereClause_but,  "gap 5 5 0 0, wrap");

		xxx.add(_activeStmntSqlOrderBy_lbl,      "gap 5 5 0 0");
		xxx.add(_activeStmntSqlOrderBy_cbx,      "growx, pushx");
		xxx.add(_activeStmntSqlOrderBy_but,      "gap 5 5 0 0, wrap");

//		xxx.add(new JScrollPane(activeStatementTable), "span, grow, push, wrap");
//		xxx.add(new JScrollPane(activeStatementTable), "span, grow, push, width 100%, height 100%, wrap");
		
		activeStatementsPan.setUseFocusableTips(true);
		activeStatementsPan.setUseFocusableTipsSize(100);
		activeStatementsPan.setLayout(new BorderLayout());
		activeStatementsPan.add(xxx, BorderLayout.NORTH);
		activeStatementScrollPan = new JScrollPane(activeStatementTable);
		activeStatementsPan.add(activeStatementScrollPan, BorderLayout.CENTER);
//		activeStatementsPan.getViewport().add(xxx, null);
//		activeStatementsPan.getViewport().add(activeStatementTable, null);
		historyStatementScrollPan.getViewport().add(_historyStatementsTable, null);

		activeSqlCapturedSqlSplitPan.add(activeStatementsPan, JSplitPane.TOP);

		spidFld.setText(Integer.toString(spid));
		kpidFld.setText(Integer.toString(kpid));

		// statementsPan.add(northPan, BorderLayout.NORTH);
		// northPan.add(activeStatementsPan, BorderLayout.CENTER);

		historyWhereSqlPanel.setUseFocusableTips(true);
		historyWhereSqlPanel.setUseFocusableTipsSize(100);
		historyStatementPan.setUseFocusableTips(true);
		historyStatementPan.setUseFocusableTipsSize(100);
		historyStatementPan.setLayout(new BorderLayout());
		historyStatementPan.add(historyWhereSqlPanel, BorderLayout.NORTH);
		historyStatementPan.add(historyStatementScrollPan, BorderLayout.CENTER);

//		historyWhereSqlPanel.setLayout(new BorderLayout());
//		historyWhereSqlPanel.add(label_historyWhereSql,        BorderLayout.WEST);
//		historyWhereSqlPanel.add(comboBox_historyWhereSql,     BorderLayout.CENTER);
//		historyWhereSqlPanel.add(button_historyWhereSql,       BorderLayout.EAST);
//		historyWhereSqlPanel.add(discardPreOpenStmntsCheckbox, BorderLayout.SOUTH);
		historyWhereSqlPanel.setLayout(new MigLayout("insets 0 0 0 0"));
		historyWhereSqlPanel.add(discardPreOpenStmntsCheckbox, "gap 5, split, span");
		historyWhereSqlPanel.add(discardAseTuneApp_chk,        "");
//		historyWhereSqlPanel.add(new JLabel(),                 "pushx, growx");
		historyWhereSqlPanel.add(_historyStmntTableCount_lbl,  "tag right, wrap");
		historyWhereSqlPanel.add(label_historyWhereSql,        "gap 5 5 0 0");
		historyWhereSqlPanel.add(comboBox_historyWhereSql,     "pushx, growx");
		historyWhereSqlPanel.add(button_historyWhereSql,       "gap 5 5 0 0");
		historyWhereSqlPanel.setToolTipText("SQL Text for 'historical' SQL Statements will be showed here.");
		

		comboBox_historyWhereSql.setToolTipText("Add your extra where clauses on the monSysStatement table. make sure that only columns in theat table are used. "+Version.getAppName()+"'s errorlog will show faulty SQL statements.");
		comboBox_historyWhereSql.setEditable(true);
		comboBox_historyWhereSql.addItem("");
		comboBox_historyWhereSql.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				comboBox_activeWhereSql_actionPerformed(e);
			}
		});
		button_historyWhereSql.setToolTipText("Remove the current restriction from the template.");
		button_historyWhereSql.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				button_historyWhereSql_actionPerformed(e);
			}
		});
		discardPreOpenStmntsCheckbox.setToolTipText("If you want to discard or show events from monSysStatements that happened prior to the window was open.");
		discardPreOpenStmntsCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setDiscardPreOpenStmnts( discardPreOpenStmntsCheckbox.isSelected() );
				saveProps();
			}
		});
		discardAseTuneApp_chk.setToolTipText("If you want to discard or show events from monSysStatements that was generated by application '"+DISCARD_APP_NAME+"'.");
		discardAseTuneApp_chk.addActionListener(new java.awt.event.ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setDiscardAseTuneAppname( discardAseTuneApp_chk.isSelected() );
				saveProps();
			}
		});


//		infoPanel.add(spidLbl,                 new XYConstraints(11,  9,   -1,  -1));
//		infoPanel.add(kpidLbl,                 new XYConstraints(137, 8,   44,  19));
//		infoPanel.add(spidFld,                 new XYConstraints(48,  8,   74,  19));
//		infoPanel.add(kpidFld,                 new XYConstraints(178, 8,   67,  19));
//		infoPanel.add(suser_nameLbl,           new XYConstraints(254, 8,   84,  19));
//		infoPanel.add(suser_nameFld,           new XYConstraints(342, 8,   88,  19));
//		infoPanel.add(refreshIntLbl,           new XYConstraints(443, 8,   111, 18));
//		infoPanel.add(refreshIntFld,           new XYConstraints(556, 8,   50,  19));
//		infoPanel.add(refreshButton,           new XYConstraints(635, 5,   85,  25));
//		infoPanel.add(clearButton,             new XYConstraints(735, 5,   85,  25));
//		infoPanel.add(saveButton,              new XYConstraints(835, 5,   100, 25));

		infoPanel.setLayout(new MigLayout());
		infoPanel.add(spidLbl,                 "");
		infoPanel.add(spidFld,                 "w 50px");
		infoPanel.add(kpidLbl,                 "");
		infoPanel.add(kpidFld,                 "w 50px");
		infoPanel.add(suser_nameLbl,           "");
		infoPanel.add(suser_nameFld,           "w 150px");
		infoPanel.add(refreshIntLbl,           "");
		infoPanel.add(refreshIntFld,           "w 30px");
		infoPanel.add(new JLabel(),            "growx, pushx");
		infoPanel.add(paused_chk,              "");
//		infoPanel.add(resumeButton,            "hidemode 3");
//		infoPanel.add(pauseButton,             "hidemode 3");
		infoPanel.add(refreshButton,           "");
		infoPanel.add(clearButton,             "");
		infoPanel.add(saveButton,              "wrap");
//		infoPanel.add(_activeStmntSqlWhereClause_lbl,  "w 60px, span, split");
//		infoPanel.add(_activeStmntSqlWhereClause_txt,  "growx, wrap");
//		infoPanel.add(_activeStmntSqlOrderBy_lbl,      "w 60px, span, split");
//		infoPanel.add(_activeStmntSqlOrderBy_txt,      "growx, wrap");
		
		statusPanel.add(middleStatusPan,   BorderLayout.CENTER);
		statusPanel.add(serverLbl,         BorderLayout.EAST);
		middleStatusPan.add(statusBarLbl,  BorderLayout.CENTER);
		middleStatusPan.add(space,         BorderLayout.EAST);

		networkActPan.add(BytesSentFld,        new XYConstraints(113, 106, 115, 20));
		networkActPan.add(PacketsReceivedLbl,  new XYConstraints(10,  36,  -1,  -1));
		networkActPan.add(PacketsReceivedFld,  new XYConstraints(87,  36,  115, 20));
		networkActPan.add(BytesReceivedLbl,    new XYConstraints(9,   60,  -1,  -1));
		networkActPan.add(BytesReceivedFld,    new XYConstraints(112, 58,  115, 20));
		networkActPan.add(PacketsSentLbl,      new XYConstraints(15,  80,  -1,  -1));
		networkActPan.add(PacketsSentFld,      new XYConstraints(113, 82,  115, 20));
		networkActPan.add(BytesSentLbl,        new XYConstraints(8,   101, -1,  -1));
		networkActPan.add(network_pktszFld,    new XYConstraints(106, 0,   115, 20));
		networkActPan.add(network_pktszLbl,    new XYConstraints(4,   2,   77,  19));
		processDetailPan.add(procedurePan,     new XYConstraints(755, 10,  253, 129));
		transactionsPan.add(tran_nameFld,      new XYConstraints(107, 0,   132, 18));
		transactionsPan.add(ULCFlushFullFld,   new XYConstraints(107, 122, 82,  18));
		transactionsPan.add(UlcFlushFld,       new XYConstraints(107, 97,  82,  18));
		transactionsPan.add(RollbacksFld,      new XYConstraints(107, 73,  82,  18));
		transactionsPan.add(CommitsFld,        new XYConstraints(107, 49,  82,  18));
		transactionsPan.add(TransactionsFld,   new XYConstraints(107, 24,  82,  18));
		transactionsPan.add(UlcBytWriteFld,    new XYConstraints(107, 146, 82,  18));
		transactionsPan.add(tran_nameLbl,      new XYConstraints(27,  0,   77,  19));
		transactionsPan.add(TransactionsLbl,   new XYConstraints(26,  26,  -1,  -1));
		transactionsPan.add(CommitsLbl,        new XYConstraints(47,  50,  -1,  -1));
		transactionsPan.add(RollbacksLbl,      new XYConstraints(43,  75,  -1,  -1));
		transactionsPan.add(UlcFlushLbl,       new XYConstraints(39,  99,  -1,  -1));
		transactionsPan.add(ULCFlushFullLbl,   new XYConstraints(24,  123, -1,  -1));
		transactionsPan.add(UlcBytWriteLbl,    new XYConstraints(12,  147, -1,  -1));
		transactionsPan.add(networkActPan,     new XYConstraints(293, 0,   243, 155));
		activityPan.add(cpuFld,                new XYConstraints(99,  24,  115, 18));
		activityPan.add(LocksHeldLbl,          new XYConstraints(31,  72,  -1,  -1));
		activityPan.add(CPUTimeLbl,            new XYConstraints(38,  49,  -1,  -1));
		activityPan.add(CPUTimeFld,            new XYConstraints(99,  47,  115, 18));
		activityPan.add(LocksHeldFld,          new XYConstraints(99,  71,  115, 18));
		activityPan.add(ScanPgsFld,            new XYConstraints(552, 24,  115, 18));
		activityPan.add(cpuLbl,                new XYConstraints(68,  25,  -1,  -1));
		activityPan.add(memusageFld,           new XYConstraints(99,  0,   115, 18));
		activityPan.add(memusageLbl,           new XYConstraints(29,  1,   -1,  -1));
		activityPan.add(WaitTimeFld,           new XYConstraints(99,  122, 115, 18));
		activityPan.add(WaitTimeLbl,           new XYConstraints(20,  122, 77,  19));
		activityPan.add(IdxPgsFld,             new XYConstraints(552, 0,   115, 18));
		activityPan.add(IdxPgsLbl,             new XYConstraints(473, 0,   77,  19));
		activityPan.add(LogicalReadsFld,       new XYConstraints(327, 0,   115, 18));
		activityPan.add(PagesReadLbl,          new XYConstraints(244, 25,  80,  19));
		activityPan.add(PhysicalReadsLbl,      new XYConstraints(229, 49,  95,  19));
		activityPan.add(PagesWrittenLbl,       new XYConstraints(247, 74,  77,  19));
		activityPan.add(physical_ioLbl,        new XYConstraints(257, 123, -1,  -1));
		activityPan.add(physical_ioFld,        new XYConstraints(327, 122, 115, 18));
		activityPan.add(MemUsageKBLbl,         new XYConstraints(3,   96,  94,  19));
		activityPan.add(MemUsageKBFld,         new XYConstraints(99,  99,  115, 18));
		activityPan.add(PagesReadFld,          new XYConstraints(327, 24,  115, 18));
		activityPan.add(PhysicalReadsFld,      new XYConstraints(327, 49,  115, 18));
		activityPan.add(PagesWrittenFld,       new XYConstraints(327, 73,  115, 18));
		activityPan.add(PhysicalWritesFld,     new XYConstraints(327, 98,  115, 18));
		activityPan.add(PhysicalWritesLbl,     new XYConstraints(233, 98,  91,  19));
		activityPan.add(LogicalReadsLbl,       new XYConstraints(229, 0,   95,  19));
		activityPan.add(ScanPgsLbl,            new XYConstraints(473, 24,  77,  19));
		activityPan.add(TmpTblLbl,             new XYConstraints(472, 73,  78,  19));
		activityPan.add(TmpTblFld,             new XYConstraints(552, 73,  115, 18));
		activityPan.add(time_blockedFld,       new XYConstraints(552, 49,  115, 18));
		activityPan.add(time_blockedLbl,       new XYConstraints(461, 49,  89,  19));
		identPan.add(uidFld,                   new XYConstraints(77,  76,  115, 18));
		identPan.add(suidLbl,                  new XYConstraints(34,  0,   40,  19));
		identPan.add(suidFld,                  new XYConstraints(77,  0,   115, 18));
		identPan.add(dbidLbl,                  new XYConstraints(32,  23,  42,  19));
		identPan.add(dbidFld,                  new XYConstraints(77,  25,  115, 18));
		identPan.add(db_nameLbl,               new XYConstraints(12,  46,  62,  19));
		identPan.add(db_nameFld,               new XYConstraints(77,  50,  115, 18));
		identPan.add(uidLbl,                   new XYConstraints(34,  76,  40,  19));
		identPan.add(user_nameLbl,             new XYConstraints(4,   101, 70,  19));
		identPan.add(user_nameFld,             new XYConstraints(77,  101, 115, 18));
		identPan.add(sys_idFld,                new XYConstraints(291, 94,  115, 18));
		identPan.add(hostnameLbl,              new XYConstraints(217, 0,   72,  19));
		identPan.add(hostnameFld,              new XYConstraints(291, 0,   115, 18));
		identPan.add(hostprocessLbl,           new XYConstraints(207, 24,  82,  19));
		identPan.add(hostprocessFld,           new XYConstraints(291, 24,  115, 18));
		identPan.add(ipaddrLbl,                new XYConstraints(216, 48,  73,  19));
		identPan.add(ipaddrFld,                new XYConstraints(291, 48,  115, 18));
		identPan.add(ses_idLbl,                new XYConstraints(212, 71,  77,  19));
		identPan.add(ses_idFld,                new XYConstraints(291, 71,  115, 18));
		identPan.add(sys_idLbl,                new XYConstraints(242, 94,  47,  19));
		identPan.add(cmdFld,                   new XYConstraints(77,  126, 170, 18));
		identPan.add(cmdLbl,                   new XYConstraints(-3,  126, 77,  19));
		identPan.add(program_nameLbl,          new XYConstraints(1,   151, 73,  19));
		identPan.add(program_nameFld,          new XYConstraints(77,  151, 170, 18));
		identPan.add(origsuidFld,              new XYConstraints(77,  177, 62,  18));
		identPan.add(fidFld,                   new XYConstraints(77,  227, 62,  18));
		identPan.add(fidLbl,                   new XYConstraints(9,   227, 65,  19));
		identPan.add(gidLbl,                   new XYConstraints(41,  202, 33,  19));
		identPan.add(gidFld,                   new XYConstraints(77,  202, 62,  18));
		identPan.add(origsuidLbl,              new XYConstraints(6,   177, 68,  19));
		identPan.add(clienthostnameFld,        new XYConstraints(353, 174, 115, 18));
		identPan.add(clientnameLbl,            new XYConstraints(268, 128, 82,  19));
		identPan.add(clientnameFld,            new XYConstraints(353, 128, 115, 18));
		identPan.add(clientapplnameLbl,        new XYConstraints(244, 151, 106, 19));
		identPan.add(clientapplnameFld,        new XYConstraints(353, 151, 115, 18));
		identPan.add(clienthostnameLbl,        new XYConstraints(249, 174, 101, 19));
		identPan.add(loggedindatetimeFld,      new XYConstraints(311, 204, 157, 18));
		identPan.add(loggedindatetimeLbl,      new XYConstraints(183, 204, 126, 19));
		processDetailPan.add(procStatusPan,    new XYConstraints(505, 10,  243, 280));
		procedurePan.add(linenumFld,           new XYConstraints(81,  69,  75,  18));
		procedurePan.add(idLbl,                new XYConstraints(1,   0,   77,  19));
		procedurePan.add(procIDFld,            new XYConstraints(81,  0,   115, 18));
		procedurePan.add(object_nameLbl,       new XYConstraints(1,   23,  77,  19));
		procedurePan.add(object_nameFld,       new XYConstraints(81,  23,  147, 18));
		procedurePan.add(stmtnumLbl,           new XYConstraints(1,   46,  77,  19));
		procedurePan.add(stmtnumFld,           new XYConstraints(81,  46,  75,  18));
		procedurePan.add(linenumLbl,           new XYConstraints(1,   69,  77,  19));
		processDetailPan.add(transactionsPan,  new XYConstraints(759, 171, 253, 203));
		processDetailPan.add(identPan,         new XYConstraints(7,   10,  487, 280));

		activeStatementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		procStatusPan.add(statusLbl,           new XYConstraints(2,   0,   77,  19));
		procStatusPan.add(blockedLbl,          new XYConstraints(2,   113, 77,  19));
		procStatusPan.add(affinityLbl,         new XYConstraints(2,   74,  77,  19));
		procStatusPan.add(statusFld,           new XYConstraints(84,  0,   115, 18));
		procStatusPan.add(priorityLbl,         new XYConstraints(2,   227, 77,  19));
		procStatusPan.add(priorityFld,         new XYConstraints(84,  227, 115, 18));
		procStatusPan.add(execlassLbl,         new XYConstraints(2,   189, 77,  19));
		procStatusPan.add(execlassFld,         new XYConstraints(84,  189, 115, 18));
		procStatusPan.add(block_xloidLbl,      new XYConstraints(2,   151, 77,  19));
		procStatusPan.add(block_xloidFld,      new XYConstraints(84,  152, 64,  18));
		procStatusPan.add(affinityFld,         new XYConstraints(84,  76,  115, 18));
		procStatusPan.add(enginenumLbl,        new XYConstraints(9,   38,  -1,  -1));
		procStatusPan.add(enginenumFld,        new XYConstraints(84,  38,  64,  18));
		procStatusPan.add(blockedFld,          new XYConstraints(84,  114, 64,  18));
		processDetailPan.add(activityPan,      new XYConstraints(7,   296, 689, 174));



		refreshIntFld.addItem("1");
		refreshIntFld.addItem("2");
		refreshIntFld.addItem("3");
		refreshIntFld.addItem("4");
		refreshIntFld.addItem("5");
		refreshIntFld.addItem("10");
		refreshIntFld.addItem("20");
		refreshIntFld.addItem("30");
		refreshIntFld.addItem("60");

		// Refresh: F5
		JPanel contentPane = (JPanel) this.getContentPane();
		KeyStroke refreshNow = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0);
		ActionListener refreshListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				refreshButton.doClick();
			}
		};
		contentPane.registerKeyboardAction(refreshListener, "ACTION_REFRESH_NOW", refreshNow, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		// Open the connection
//		cnx = OpenConnectionDlg.getAnotherConnection(Version.getAppName()+"-spid", true);
//		cnx = AseConnectionFactory.getConnection(null, Version.getAppName()+"-spid", null);
		cnx = _connectionProvider.getNewConnection(Version.getAppName()+"-spid");
		if (cnx == null)
		{
			throw new RuntimeException("Sorry could not get a connection. conn=null");
		}
		if ( ! AseConnectionUtils.checkForMonitorOptions(cnx, AseConnectionFactory.getUser(), true, this) )
		{
			// FIXME: should we continue here or not...
		}

		// Display servername on status bar
//		serverLbl.setText(MainFrame.getStatus(MainFrame.ST_CONNECT));
		String srvInfo = "";
		ConnectionProp connProp = cnx.getConnProp();
		String hostPortStr = connProp == null ? "" : " (" + JdbcUrlParser.parse(connProp.getUrl()).getHostPortStr() + ")";
		srvInfo = 
			(connProp == null ? "user" : connProp.getUsername()) 
			+ " - " 
			+ cnx.getDbmsServerName()
			+ hostPortStr;
		serverLbl.setText(srvInfo);

		loadProps();

		// Start refreshing this frame
		if (cnx != null)
		{
			refressProcess = new RefreshProcess(this, cnx, spid, kpid);

			refressProcess.setSqlTextSample( batchShowEnableCheckbox.isSelected() );
			refressProcess.setPlanTextSample( planShowEnableCheckbox.isSelected() );
			refressProcess.setDiscardPreOpenStmnts( discardPreOpenStmntsCheckbox.isSelected() );
			refressProcess.setDiscardAseTuneAppname( discardAseTuneApp_chk.isSelected() );

			// use the selected where clause...
			String sql = (String) comboBox_historyWhereSql.getSelectedItem();
			refressProcess.setHistoryRestriction(sql);

			refressProcess.start();
		}

		pack();
		initWaterMarks();
		_guiInitialized = true;
		setVisible(true);
	}

	private void historyStatementsTable_mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 1)
		{
		}
	}


	private void this_windowClosing(WindowEvent e)
	{
		if (refressProcess != null)
		{
			refressProcess.stopRefresh();
			saveProps();
		}

		// Remove this component from the memory listener
		Memory.removeMemoryListener(this);

//		if (processObjectsFrame != null) processObjectsFrame.dispose();
//		if (processWaitsFrame   != null) processWaitsFrame  .dispose();
//		if (processLocksFrame   != null) processLocksFrame  .dispose();
	}

	private void activeStatementTable_mouseClicked(MouseEvent e)
	{
	}

	private void refreshIntFld_actionPerformed(ActionEvent e)
	{
		String interv = (String) refreshIntFld.getSelectedItem();
		processRefreshInterv = Integer.parseInt(interv);

		if (refressProcess != null)
			refressProcess.setRefreshInterval(processRefreshInterv);
	}

	private void pause_actionPerformed(ActionEvent e)
	{
		if (refressProcess != null)
		{
			refressProcess.setPauseProcess(paused_chk.isSelected());
		}
	}
//	private void pauseButton_actionPerformed(ActionEvent e)
//	{
//		if (refressProcess != null)
//		{
//			refressProcess.pauseProcess();
//			pauseButton .setVisible(false);
//			resumeButton.setVisible(true);
//		}
//	}
//
//	private void resumeButton_actionPerformed(ActionEvent e)
//	{
//		if (refressProcess != null)
//		{
//			refressProcess.resumeProcess();
//			pauseButton .setVisible(true);
//			resumeButton.setVisible(false);
//		}
//	}

	private void refreshButton_actionPerformed(ActionEvent e)
	{
		if (refressProcess != null)
			refressProcess.refreshProcess();
	}

	/** REMOVE from template */
	private void button_historyWhereSql_actionPerformed(ActionEvent e)
	{
		String sql = (String) comboBox_historyWhereSql.getSelectedItem();
		if (sql != null)
		{
			sql = sql.trim();

			// Check if the string is in the template list
			Object model = comboBox_historyWhereSql.getModel();
			if (model instanceof DefaultComboBoxModel)
			{
				int index = ((DefaultComboBoxModel<?>) model).getIndexOf(sql);

				// If it does not exist in the list save it
				if (index > 0)
				{
					comboBox_historyWhereSql.removeItem(sql);
					//comboBox_historyWhereSql.setSelectedIndex(0);
				}
			}
		}

		saveProps();
	}

	/** REMOVE from template */
	private void action_activeStmntSqlWhereClause_but(ActionEvent e)
	{
		String sql = (String) _activeStmntSqlWhereClause_cbx.getSelectedItem();
		if (sql != null)
		{
			sql = sql.trim();

			// Check if the string is in the template list
			Object model = _activeStmntSqlWhereClause_cbx.getModel();
			if (model instanceof DefaultComboBoxModel)
			{
				int index = ((DefaultComboBoxModel<?>) model).getIndexOf(sql);

				// If it does not exist in the list save it
				if (index > 0)
				{
					_activeStmntSqlWhereClause_cbx.removeItem(sql);
				}
			}
		}

		saveProps();
	}

	/** REMOVE from template */
	private void action_activeStmntSqlOrderBy_but(ActionEvent e)
	{
		String sql = (String) _activeStmntSqlOrderBy_cbx.getSelectedItem();
		if (sql != null)
		{
			sql = sql.trim();

			// Check if the string is in the template list
			Object model = _activeStmntSqlOrderBy_cbx.getModel();
			if (model instanceof DefaultComboBoxModel)
			{
				int index = ((DefaultComboBoxModel<?>) model).getIndexOf(sql);

				// If it does not exist in the list save it
				if (index > 0)
				{
					_activeStmntSqlOrderBy_cbx.removeItem(sql);
				}
			}
		}

		saveProps();
	}


	/** ACTION on combobox */
	private void comboBox_activeWhereSql_actionPerformed(ActionEvent e)
	{
		//_logger.debug("comboBox_historyWhereSql.actionPerformed(): e.getActionCommand()='" + e.getActionCommand() + "', ActionEvent=" + e);

		String sql = (String) comboBox_historyWhereSql.getSelectedItem();
		sql = sql.trim();

		// Check if the select text is in the template list
		// If so, name the buttom "remove", otherwisse "save"
		Object model = comboBox_historyWhereSql.getModel();
		if (model instanceof DefaultComboBoxModel)
		{
			int index = ((DefaultComboBoxModel<?>) model).getIndexOf(sql);
			if ( index == -1 )
				comboBox_historyWhereSql.addItem(sql);
		}

		// set the restrictor in refressProcess
		if (refressProcess != null)
			refressProcess.setHistoryRestriction(sql);
		saveProps();
	}


	/** ACTION on combobox */
	private void action_activeStmntSqlWhereClause_cbx(ActionEvent e)
	{
		String sql = (String) _activeStmntSqlWhereClause_cbx.getSelectedItem();
		sql = sql.trim();

		// Check if the select text is in the template list
		// If so, name the buttom "remove", otherwisse "save"
		Object model = _activeStmntSqlWhereClause_cbx.getModel();
		if (model instanceof DefaultComboBoxModel)
		{
			int index = ((DefaultComboBoxModel<?>) model).getIndexOf(sql);
			if ( index == -1 )
				_activeStmntSqlWhereClause_cbx.addItem(sql);
		}

//		// set the restrictor in refressProcess
//		if (refressProcess != null)
//			refressProcess.xxx(sql);
		saveProps();
	}

	/** ACTION on combobox */
	private void action_activeStmntSqlOrderBy_cbx(ActionEvent e)
	{
		String sql = (String) _activeStmntSqlOrderBy_cbx.getSelectedItem();
		sql = sql.trim();

		// Check if the select text is in the template list
		// If so, name the buttom "remove", otherwisse "save"
		Object model = _activeStmntSqlOrderBy_cbx.getModel();
		if (model instanceof DefaultComboBoxModel)
		{
			int index = ((DefaultComboBoxModel<?>) model).getIndexOf(sql);
			if ( index == -1 )
				_activeStmntSqlOrderBy_cbx.addItem(sql);
		}

//		// set the restrictor in refressProcess
//		if (refressProcess != null)
//			refressProcess.xxx(sql);
		saveProps();
	}


	public static final String  PROPKEY_sample_spShowplanfull = "processDetailFrame.spid.sample.SpShowplanfull";
	public static final boolean DEFAULT_sample_spShowplanfull = true;

	public static final String  PROPKEY_hide_activeSqlWaitEventId250 = "processDetailFrame.spid.hide.ActiveSqlWaitEventId250";
	public static final boolean DEFAULT_hide_activeSqlWaitEventId250 = false;
	
	
	private void saveProps()
  	{
		// this is really ugly, do this in another way...
		if (!_guiInitialized)
			return;
		
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = "processDetailFrame.spid.";

		if (tmpConf != null)
		{
			// Get rid of all fields first
			//tmpConf.removeAll(base);

			tmpConf.setLayoutProperty(base + "window.width", this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.setLayoutProperty(base + "activeStatements.width", activeStatementsPan.getSize().width);
			tmpConf.setLayoutProperty(base + "activeStatements.height", activeStatementsPan.getSize().height);

			tmpConf.setLayoutProperty(base + "historyStatements.width", historyStatementScrollPan.getSize().width);
			tmpConf.setLayoutProperty(base + "historyStatements.height", historyStatementScrollPan.getSize().height);

			tmpConf.setLayoutProperty(base + "batch.width", batchScrollPan.getSize().width);
			tmpConf.setLayoutProperty(base + "batch.height", batchScrollPan.getSize().height);
			tmpConf.setProperty      (base + "batch.sample", batchShowEnableCheckbox.isSelected());
			tmpConf.setProperty      (base + "batch.spSrcInBatch", sqlTextShowProcSrcCheckbox.isSelected());

			tmpConf.setLayoutProperty(base + "plan.width", planScrollPan.getSize().width);
			tmpConf.setLayoutProperty(base + "plan.height", planScrollPan.getSize().height);
			tmpConf.setProperty      (base + "plan.sample", planShowEnableCheckbox.isSelected());
			
			tmpConf.setProperty(PROPKEY_sample_spShowplanfull,        _doExecSpShowplanfull_cbx  .isSelected());
			tmpConf.setProperty(PROPKEY_hide_activeSqlWaitEventId250, _hideActiveSqlWaitId250_cbx.isSelected());

//			tmpConf.setProperty(base + "objects.window.active", processObjectsFrame.isVisible());
//			tmpConf.setProperty(base + "objects.window.width",  processObjectsFrame.getSize().width);
//			tmpConf.setProperty(base + "objects.window.height", processObjectsFrame.getSize().height);
//			if ( processObjectsFrame.isVisible() )
//			{
//			tmpConf.setProperty(base + "objects.window.pos.x",  processObjectsFrame.getLocationOnScreen().x);
//			tmpConf.setProperty(base + "objects.window.pos.y",  processObjectsFrame.getLocationOnScreen().y);
//			}
//
//			tmpConf.setProperty(base + "waits.window.active", processWaitsFrame.isVisible());
//			tmpConf.setProperty(base + "waits.window.width",  processWaitsFrame.getSize().width);
//			tmpConf.setProperty(base + "waits.window.height", processWaitsFrame.getSize().height);
//			if ( processWaitsFrame.isVisible() )
//			{
//			tmpConf.setProperty(base + "waits.window.pos.x",  processWaitsFrame.getLocationOnScreen().x);
//			tmpConf.setProperty(base + "waits.window.pos.y",  processWaitsFrame.getLocationOnScreen().y);
//			}
//
//			tmpConf.setProperty(base + "locks.window.active", processLocksFrame.isVisible());
//			tmpConf.setProperty(base + "locks.window.width",  processLocksFrame.getSize().width);
//			tmpConf.setProperty(base + "locks.window.height", processLocksFrame.getSize().height);
//			if ( processLocksFrame.isVisible() )
//			{
//			tmpConf.setProperty(base + "locks.window.pos.x",  processLocksFrame.getLocationOnScreen().x);
//			tmpConf.setProperty(base + "locks.window.pos.y",  processLocksFrame.getLocationOnScreen().y);
//			}

//			tmpConf.setProperty(PROP_ACTIVE_STATEMENT_EXTRA_WHERE, _activeStmntSqlWhereClause_txt.getText());
//			tmpConf.setProperty(PROP_ACTIVE_STATEMENT_ORDER_BY,    _activeStmntSqlOrderBy_txt    .getText());

			int saveCount = 0;
			// ACTIVE Statements WHERE
			//---------------------------------------
			// Get rid of all fields first
			tmpConf.removeAll(base + "active.statement.extraWhere.");
			saveCount = 0;
			for (int i=1; i<_activeStmntSqlWhereClause_cbx.getItemCount(); i++)
			{
				Object o = _activeStmntSqlWhereClause_cbx.getItemAt(i);
				if (o != null)
				{
					saveCount++;
					tmpConf.setProperty(base + "active.statement.extraWhere."+saveCount, o.toString());

					_logger.debug("saveProps(): processDetailFrame.spid.active.statement.extraWhere."+saveCount+"='"+o.toString()+"'.");
				}
			}
			tmpConf.setProperty(   base + "active.statement.extraWhere.count", saveCount);
			_logger.debug("saveProps(): processDetailFrame.spid.active.statement.extraWhere.count='"+saveCount+"'.");

			tmpConf.setProperty(   base + "active.statement.extraWhere.active", Math.max(0, _activeStmntSqlWhereClause_cbx.getSelectedIndex()));
			_logger.debug("saveProps(): processDetailFrame.spid.active.statement.extraWhere.active='"+Math.max(0, _activeStmntSqlWhereClause_cbx.getSelectedIndex())+"'.");

			tmpConf.setProperty(PROP_ACTIVE_STATEMENT_EXTRA_WHERE, _activeStmntSqlWhereClause_cbx.getSelectedItem()+"");
			
			// ACTIVE Statements ORDER BY
			//---------------------------------------
			// Get rid of all fields first
			tmpConf.removeAll(base + "active.statement.orderBy.");
			saveCount = 0;
			for (int i=1; i<_activeStmntSqlOrderBy_cbx.getItemCount(); i++)
			{
				Object o = _activeStmntSqlOrderBy_cbx.getItemAt(i);
				if (o != null)
				{
					saveCount++;
					tmpConf.setProperty(base + "active.statement.orderBy."+saveCount, o.toString());

					_logger.debug("saveProps(): processDetailFrame.spid.active.statement.orderBy."+saveCount+"='"+o.toString()+"'.");
				}
			}
			tmpConf.setProperty(   base + "active.statement.orderBy.count", saveCount);
			_logger.debug("saveProps(): processDetailFrame.spid.active.statement.orderBy.count='"+saveCount+"'.");

			tmpConf.setProperty(   base + "active.statement.orderBy.active", Math.max(0, _activeStmntSqlOrderBy_cbx.getSelectedIndex()));
			_logger.debug("saveProps(): processDetailFrame.spid.active.statement.orderBy.active='"+Math.max(0, _activeStmntSqlOrderBy_cbx.getSelectedIndex())+"'.");

			tmpConf.setProperty(PROP_ACTIVE_STATEMENT_ORDER_BY, _activeStmntSqlOrderBy_cbx.getSelectedItem()+"");

			// HISTORY Statements WHERE
			//---------------------------------------
			// Get rid of all fields first
			tmpConf.removeAll(base + "plan.extraWhere.");
			saveCount = 0;
			for (int i=1; i<comboBox_historyWhereSql.getItemCount(); i++)
			{
				Object o = comboBox_historyWhereSql.getItemAt(i);
				if (o != null)
				{
					saveCount++;
					tmpConf.setProperty(base + "plan.extraWhere."+saveCount, o.toString());

					_logger.debug("saveProps(): processDetailFrame.spid.plan.extraWhere."+saveCount+"='"+o.toString()+"'.");
				}
			}
			tmpConf.setProperty(   base + "plan.extraWhere.count", saveCount);
			_logger.debug("saveProps(): processDetailFrame.spid.plan.extraWhere.count='"+saveCount+"'.");

			tmpConf.setProperty(   base + "plan.extraWhere.active", Math.max(0, comboBox_historyWhereSql.getSelectedIndex()));
			_logger.debug("saveProps(): processDetailFrame.spid.plan.extraWhere.active='"+Math.max(0, comboBox_historyWhereSql.getSelectedIndex())+"'.");


			tmpConf.setProperty(base + "plan.discardPreOpenStmnts", discardPreOpenStmntsCheckbox.isSelected());
			tmpConf.setProperty(base + "plan.discardAseTuneStmnts", discardAseTuneApp_chk.isSelected());

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = -1;
		int     height    = -1;
		int     x         = -1;
		int     y         = -1;
//		boolean winActive = false;
		boolean checkBox1 = false;
		boolean checkBox2 = false;

		Configuration props   = Configuration.getCombinedConfiguration();
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = "processDetailFrame.spid.";

		if (props == null)
			return;

		width  = props.getLayoutProperty(base + "window.width",  SwingUtils.hiDpiScale(1000));
		height = props.getLayoutProperty(base + "window.height", SwingUtils.hiDpiScale(700));
		x      = props.getLayoutProperty(base + "window.pos.x",  -1);
		y      = props.getLayoutProperty(base + "window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setPreferredSize(new Dimension(width, height));
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
				this.setLocation(x, y);
		}
		else
		{
			_logger.debug("Open ProcessDetailFrame in center of window.");
			Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
//			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension frameSize = this.getPreferredSize();
			if (frameSize.height > screenSize.height)
			{
				frameSize.height = screenSize.height;
			}
			if (frameSize.width > screenSize.width)
			{
				frameSize.width = screenSize.width;
			}
			setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
		}

		width  = props.getLayoutProperty(base + "activeStatements.width",  SwingUtils.hiDpiScale(1000));
		height = props.getLayoutProperty(base + "activeStatements.height", SwingUtils.hiDpiScale(160));
		if (width != -1 && height != -1)
		{
			activeStatementsPan.setPreferredSize(new Dimension(width, height));
		}

		width  = props.getLayoutProperty(base + "historyStatements.width",  SwingUtils.hiDpiScale(1000));
		height = props.getLayoutProperty(base + "historyStatements.height", SwingUtils.hiDpiScale(130));
		if (width != -1 && height != -1)
		{
			historyStatementScrollPan.setPreferredSize(new Dimension(width, height));
		}

		checkBox1 = props.getBooleanProperty(base + "batch.sample",       true);
		checkBox2 = props.getBooleanProperty(base + "batch.spSrcInBatch", true);
		width  = props.getLayoutProperty(base + "batch.width",  -1);
		height = props.getLayoutProperty(base + "batch.height", -1);
		if (width != -1 && height != -1)
		{
			batchScrollPan.setPreferredSize(new Dimension(width, height));
		}
		batchShowEnableCheckbox.setSelected(checkBox1);
		sqlTextShowProcSrcCheckbox.setSelected(checkBox2);

		checkBox1 = props.getBooleanProperty(base + "plan.sample", false);
		width  = props.getLayoutProperty(base + "plan.width",  -1);
		height = props.getLayoutProperty(base + "plan.height", -1);
		if (width != -1 && height != -1)
		{
			planScrollPan.setPreferredSize(new Dimension(width, height));
		}
		planShowEnableCheckbox.setSelected(checkBox1);
		
		_doExecSpShowplanfull_cbx  .setSelected(props.getBooleanProperty(PROPKEY_sample_spShowplanfull,        DEFAULT_sample_spShowplanfull));
		_hideActiveSqlWaitId250_cbx.setSelected(props.getBooleanProperty(PROPKEY_hide_activeSqlWaitEventId250, DEFAULT_hide_activeSqlWaitEventId250));


		// processObjects
//		winActive = props.getBooleanProperty(base + "objects.window.active", false);
		width  = props.getLayoutProperty(base + "objects.window.width",  -1);
		height = props.getLayoutProperty(base + "objects.window.height", -1);
		x      = props.getLayoutProperty(base + "objects.window.pos.x",  -1);
		y      = props.getLayoutProperty(base + "objects.window.pos.y",  -1);
//		if (width != -1 && height != -1 && x != -1 && y != -1)
//		{
//			processObjectsFrame.setSize(width, height);
//			processObjectsFrame.setLocation(x, y);
//		}
//		if (winActive)
//		{
//			processObjectsFrame.getContentPane().add(processObjectsPanel);
//			processObjectsFrame.setVisible(winActive);
//		}

		// processWaits
//		winActive = props.getBooleanProperty(base + "waits.window.active", "false");
		width  = props.getLayoutProperty(base + "waits.window.width",  -1);
		height = props.getLayoutProperty(base + "waits.window.height", -1);
		x      = props.getLayoutProperty(base + "waits.window.pos.x",  -1);
		y      = props.getLayoutProperty(base + "waits.window.pos.y",  -1);
//		if (width != -1 && height != -1 && x != -1 && y != -1)
//		{
//			processWaitsFrame.setSize(width, height);
//			processWaitsFrame.setLocation(x, y);
//		}
//		if (winActive)
//		{
//			processWaitsFrame.getContentPane().add(processWaitsPanel);
//			processWaitsFrame.setVisible(winActive);
//		}

		// processLocks
//		winActive = props.getBooleanProperty(base + "locks.window.active", "false");
		width  = props.getLayoutProperty(base + "locks.window.width",  -1);
		height = props.getLayoutProperty(base + "locks.window.height", -1);
		x      = props.getLayoutProperty(base + "locks.window.pos.x",  -1);
		y      = props.getLayoutProperty(base + "locks.window.pos.y",  -1);
//		if (width != -1 && height != -1 && x != -1 && y != -1)
//		{
//			processLocksFrame.setSize(width, height);
//			processLocksFrame.setLocation(x, y);
//		}
//		if (winActive)
//		{
//			processLocksFrame.getContentPane().add(processLocksPanel);
//			processLocksFrame.setVisible(winActive);
//		}

		int count;
		int active;
		// ACTIVE STATEMENT: WHERE           //active.statement.
		count   = props.getIntProperty(base + "active.statement.extraWhere.count", -1);
		active  = props.getIntProperty(base + "active.statement.extraWhere.active", -1);
		if ( count != -1  && active != -1 )
		{
			_logger.debug("loadProps(): processDetailFrame.spid.active.statement.extraWhere.active='"+active+"'.");

			active = Math.max(0, active);
			for (int i=1; i<=count; i++)
			{
				String str = props.getProperty(base + "active.statement.extraWhere."+i).trim();
				_activeStmntSqlWhereClause_cbx.insertItemAt(str, i);
				_logger.debug("loadProps(): processDetailFrame.active.statement.plan.extraWhere."+i+"='"+str+"'.");
			}
			_logger.debug("loadProps(): Set active template to index="+active+".");
			_activeStmntSqlWhereClause_cbx.setSelectedIndex(active);
		}
		else
		{
			// if we cant find anything in the configuration file, add some defaults
			// which will be written to the config file on next save...
			_activeStmntSqlWhereClause_cbx.addItem("WaitTime > 1000");
			_activeStmntSqlWhereClause_cbx.addItem("LogicalReads > 100");
			_activeStmntSqlWhereClause_cbx.addItem("PhysicalReads > 10");
			_activeStmntSqlWhereClause_cbx.addItem("P.SPID in (select blocked from master..sysprocesses where blocked > 0) -- SPID's that blocks others");
			_activeStmntSqlWhereClause_cbx.addItem("P.BlockingSPID > 0 -- Blocked SPID's");
			_activeStmntSqlWhereClause_cbx.addItem("object_name(S.ProcedureID,S.DBID) = 'any_proc_name'");
			_activeStmntSqlWhereClause_cbx.addItem("db_name(S.DBID) = 'any_db_name'");
		}
		// this entry is the one used by RefreshProcess
		if (tmpConf != null)
			tmpConf.setProperty(PROP_ACTIVE_STATEMENT_EXTRA_WHERE, _activeStmntSqlWhereClause_cbx.getSelectedItem()+"");

		// ACTIVE STATEMENT: ORDER BY
		count   = props.getIntProperty(base + "active.statement.orderBy.count", -1);
		active  = props.getIntProperty(base + "active.statement.orderBy.active", -1);
		if ( count != -1  && active != -1 )
		{
			_logger.debug("loadProps(): processDetailFrame.spid.active.statement.orderBy.active='"+active+"'.");

			active = Math.max(0, active);
			for (int i=1; i<=count; i++)
			{
				String str = props.getProperty(base + "active.statement.orderBy."+i).trim();
				_activeStmntSqlOrderBy_cbx.insertItemAt(str, i);
				_logger.debug("loadProps(): processDetailFrame.active.statement.plan.orderBy."+i+"='"+str+"'.");
			}
			_logger.debug("loadProps(): Set active template to index="+active+".");
			_activeStmntSqlOrderBy_cbx.setSelectedIndex(active);
		}
		else
		{
			// if we cant find anything in the configuration file, add some defaults
			// which will be written to the config file on next save...
			_activeStmntSqlOrderBy_cbx.addItem("SPID");
			_activeStmntSqlOrderBy_cbx.addItem("LogicalReads desc");
			_activeStmntSqlOrderBy_cbx.addItem("PhysicalReads desc");
		}
		// this entry is the one used by RefreshProcess
		if (tmpConf != null)
			tmpConf.setProperty(PROP_ACTIVE_STATEMENT_ORDER_BY, _activeStmntSqlOrderBy_cbx.getSelectedItem()+"");

//		_activeStmntSqlWhereClause_txt.setText(props.getProperty(PROP_ACTIVE_STATEMENT_EXTRA_WHERE, ""));
//		_activeStmntSqlOrderBy_txt    .setText(props.getProperty(PROP_ACTIVE_STATEMENT_ORDER_BY,    ""));

		// HISTORY Statements WHERE
		count   = props.getIntProperty(base + "plan.extraWhere.count", -1);
		active  = props.getIntProperty(base + "plan.extraWhere.active", -1);
		if ( count != -1  && active != -1 )
		{
			_logger.debug("loadProps(): processDetailFrame.spid.plan.extraWhere.active='"+active+"'.");

			active = Math.max(0, active);
			for (int i=1; i<=count; i++)
			{
				String str = props.getProperty(base + "plan.extraWhere."+i).trim();
				comboBox_historyWhereSql.insertItemAt(str, i);
				_logger.debug("loadProps(): processDetailFrame.spid.plan.extraWhere."+i+"='"+str+"'.");
			}
			_logger.debug("loadProps(): Set active template to index="+active+".");
			comboBox_historyWhereSql.setSelectedIndex(active);
		}
		else
		{
			// if we cant find anything in the configuration file, add some defaults
			// which will be written to the config file on next save...
			comboBox_historyWhereSql.addItem("WaitTime > 1000");
			comboBox_historyWhereSql.addItem("LogicalReads > 100");
			comboBox_historyWhereSql.addItem("LogicalReads > 1000");
			comboBox_historyWhereSql.addItem("LogicalReads > 5000");
			comboBox_historyWhereSql.addItem("LogicalReads > 10000");
			comboBox_historyWhereSql.addItem("(LogicalReads > 10000 or PhysicalReads > 300)");
			comboBox_historyWhereSql.addItem("datediff(ms, StartTime, EndTime) > 1000");
			comboBox_historyWhereSql.addItem("datediff(ms, StartTime, EndTime) > 5000");
			comboBox_historyWhereSql.addItem("SPID in (select spid from master.dbo.sysprocesses where program_name = 'isql')");
		}

		discardPreOpenStmntsCheckbox.setSelected( props.getBooleanProperty(base + "plan.discardPreOpenStmnts", true) );
		discardAseTuneApp_chk       .setSelected( props.getBooleanProperty(base + "plan.discardAseTuneStmnts", true) );
	}
  	public static final String PROP_ACTIVE_STATEMENT_EXTRA_WHERE = "processDetailFrame.spid.active.statement.extraWhere";
  	public static final String PROP_ACTIVE_STATEMENT_ORDER_BY    = "processDetailFrame.spid.active.statement.orderBy";

  	public void setRefreshError(Exception e)
	{
	}

	public void clear()
	{
		if (refressProcess != null)
			refressProcess.clear();
	}

	public void saveHistoryStatements()
	{
		if (refressProcess != null)
			refressProcess.saveHistoryStatementsToFile();

		clear();
	}

//	private void mainTabbedPanel_mouseClicked(MouseEvent e)
//	{
//		if (e.getClickCount() == 2)
//		{
//			Point p = e.getPoint();
//			int index = mainTabbedPanel.indexAtLocation(p.x, p.y);
//			String tabName = mainTabbedPanel.getTitleAt(index);
//
//			_logger.debug("TabedPanel: doubleClick on index="+index+", name='"+tabName+"'.");
//
//			// OBJECTS
//			if (tabName.equals(processObjectsPanel.getPanelName()))
//			{
////				if (processObjectsFrame == null)
////				{
////					//processObjectsFrame = new JDialog(pdf, "Objects");
////					processObjectsFrame = new JFrame(processObjectsPanel.getName());
////					processObjectsFrame.setIconImage(frameIcon);
////					processObjectsFrame.setSize(new Dimension(900, 300));
////					processObjectsFrame.addWindowListener(new WindowAdapter()
////					{
////						public void windowClosing(WindowEvent e)
////						{
////							mainTabbedPanel.add(processObjectsPanel.getName(), processObjectsPanel);
////						}
////					});
////				}
//				processObjectsFrame.getContentPane().add(processObjectsPanel);
//				processObjectsFrame.setVisible(true);
//			}
//			// WAITS
//			if (tabName.equals(processWaitsPanel.getPanelName()))
//			{
////				if (processWaitsFrame == null)
////				{
////					//processWaitsFrame = new JDialog(pdf, "Waits");
////					processWaitsFrame = new JFrame(processWaitsPanel.getName());
////					processWaitsFrame.setIconImage(frameIcon);
////					processWaitsFrame.setSize(new Dimension(750, 500));
////					processWaitsFrame.addWindowListener(new WindowAdapter()
////					{
////						public void windowClosing(WindowEvent e)
////						{
////							mainTabbedPanel.add(processWaitsPanel.getName(), processWaitsPanel);
////						}
////					});
////				}
//				processWaitsFrame.getContentPane().add(processWaitsPanel);
//				processWaitsFrame.setVisible(true);
//			}
//			// LOCKS
//			if (tabName.equals(processLocksPanel.getPanelName()))
//			{
////				if (processLocksFrame == null)
////				{
////					//processLocksFrame = new JDialog(pdf, "Locks");
////					processLocksFrame = new JFrame(processLocksPanel.getName());
////					processLocksFrame.setIconImage(frameIcon);
////					processLocksFrame.setSize(new Dimension(900, 300));
////					processLocksFrame.addWindowListener(new WindowAdapter()
////					{
////						public void windowClosing(WindowEvent e)
////						{
////							mainTabbedPanel.add(processLocksPanel.getName(), processLocksPanel);
////						}
////					});
////				}
//				processLocksFrame.getContentPane().add(processLocksPanel);
//				processLocksFrame.setVisible(true);
//			}
//		} // end: double-click
//	} // end: method
//	private void maybeShowPopup(MouseEvent e)
//	{
//		if (e.isPopupTrigger())
//		{
//			// see if the click was in one of tabs
//			int tabcount = MyTabbedPane.this.getTabCount();
//			for(int i=0;i<tabcount;i++)
//			{
//				TabbedPaneUI tpu = MyTabbedPane.this.getUI();
//				Rectangle rect = tpu.getTabBounds(MyTabbedPane.this, i);
//				int x = e.getX();
//				int y = e.getY();
//				if (x >< rect.x  ||  x > rect.x+rect.width  ||  y < rect.y  ||  y > rect.y+rect.height)
//					continue;
//
//				//setSelectedIndex shouldn't be necessary here, but just in case the listeners get
//				// called in different order (?), the callback code for each my menu items assumes
//				// that it can use getSelectedIndex() to determine which tab it should operate upon.
//				setSelectedIndex(i);
//
//				popupMenu.show(e.getComponent(), e.getX(), e.getY());
//				break;
//			}
//		}
//	}


	//-----------------------------------------------------------------------
	// BEGIN: implement IGuiController
	//-----------------------------------------------------------------------
	@Override
	public boolean hasGUI()
	{
		return true;
	}

	@Override
	public void addPanel(JPanel panel)
	{
		if (panel instanceof TabularCntrPanel)
		{
			TabularCntrPanel tcp = (TabularCntrPanel) panel;
			mainTabbedPanel.addTab(tcp.getPanelName(), tcp.getIcon(), tcp, tcp.getCm().getDescription());
		}
//		else if (panel instanceof ISummaryPanel)
//		{
//			setSummaryPanel( (ISummaryPanel)panel );
//		}
		else
		{
			mainTabbedPanel.addTab(panel.getName(), null, panel, null);
		}
	}

	@Override
	public GTabbedPane getTabbedPane()
	{
		return mainTabbedPanel;
	}

	@Override
	public void splashWindowProgress(String msg)
	{
	}

	@Override
	public Component getActiveTab()
	{
		return mainTabbedPanel.getSelectedComponent();
	}

	@Override
	public void setStatus(int type)
	{
	}

	@Override
	public void setStatus(int type, String param)
	{
	}
	
	@Override
	public Component getGuiHandle()
	{
		return this;
	}
	//-----------------------------------------------------------------------
	// END: implement IGuiController
	//-----------------------------------------------------------------------

	public void setStatusBar(String status, boolean error)
	{
		if ( "".equals(status) && statusBarLbl.getText().startsWith("Error") )
			return;

		statusBarLbl.setText(status);
		statusBarLbl.setForeground( error ? Color.RED : Color.BLACK);
	}
	public void refreshBegin()
	{
		setStatusBar("Refreshing", false);
	}

	public void refreshEnd()
	{
		setStatusBar("", false);
		
		NumberFormat numberFormat = NumberFormat.getInstance();
		
		int activeTotal  = activeStatementTable.getRowCount();
		int historyTotal = _historyStatementsTable.getRowCount();
		int historyDiff  = historyTotal - _historyStmntTableCount_prev;

		_historyStmntTableCount_prev = historyTotal;
		
		_historyStmntTableCount_lbl.setText(numberFormat.format(historyTotal) + " Rows, " + numberFormat.format(historyDiff) + " New rows");
		_activeStmntTableCount_lbl .setText(activeTotal + " Rows");
	}
	
	/**
	 * Called from the Refresh Process, at the top<br>
	 * Sp here we can check various stuff and set statuses etc.
	 */
	public void updateGuiStatus()
	{
		setWatermark();
		if (refressProcess != null)
		{
			boolean isPaused = refressProcess.isPaused();
			paused_chk.setSelected(isPaused);
//			pauseButton .setVisible(!isPaused);
//			resumeButton.setVisible( isPaused);

			activeStatementsPan.setToolTipText("<html><b>Last SQL Used To get ACTIVE SQL Statements </b><br><code><pre>"+refressProcess.getActiveStatementsSql() +"</pre></code></html>");
			historyWhereSqlPanel.setToolTipText("<html><b>Last SQL Used To get HISTORY SQL Statements</b><br><code><pre>"+refressProcess.getHistoryStatementsSql()+"</pre></code></html>");

		}
	}


	/*---------------------------------------------------
	** BEGIN: Watermark stuff
	**---------------------------------------------------
	*/
	/**
	 * Set water mark for Active SQL Table panel 
	 * @param msg
	 */
	public void setWatermarkActiveTable(String str)
	{
		_watermarkActiveStmnts.setWatermarkText(str);
	}

	/**
	 * Set water mark for Historical SQL Table panel 
	 * @param msg
	 */
	public void setWatermarkHistoryTable(String str)
	{
		_watermarkHistoryStmnts.setWatermarkText(str);
	}

	/**
	 * Set water mark for SQL Text panel 
	 * @param msg
	 */
	public void setWatermarkSqlText(String str)
	{
		_watermarkSqlText.setWatermarkText(str);
	}

	/**
	 * Set water mark for ShowPlan panel 
	 * @param msg
	 */
	public void setWatermarkPlanText(String str)
	{
		_watermarkPlanText.setWatermarkText(str);
	}

	private Watermark              _watermarkActiveStmnts  = null;
	private Watermark              _watermarkHistoryStmnts = null;
	private Watermark              _watermarkSqlText       = null;
	private Watermark              _watermarkPlanText      = null;

	private void initWaterMarks()
	{
		_watermarkBig           = new WatermarkBig((JPanel)getContentPane(), "");
		_watermarkActiveStmnts  = new Watermark(activeStatementScrollPan,  "");
		_watermarkHistoryStmnts = new Watermark(historyStatementScrollPan, "");
		_watermarkSqlText       = new Watermark(batchScrollPan,            "");
		_watermarkPlanText      = new Watermark(planScrollPan,             "");
	}

//	public void setWatermarkText(String str)
//	{
//		_watermark.setWatermarkText(str);
//	}
//	public void setWatermark()
//	{
//		// Find out in what state we are and set the water mark to something good.
//		setWatermarkText(null);
//	}


	private class Watermark
    extends AbstractComponentDecorator
    {
		public Watermark(JComponent target, String text)
		{
			super(target);
			if (text == null)
				text = "";
			_textSave = text;
			_textBr   = text.split("\n");
		}
		private String[]    _textBr = null; // Break Lines by '\n'
		private Graphics2D  g       = null;
		private Rectangle   r       = null;
		private String      _textSave       = null; // Save last text so we don't need to do repaint if no changes.

		@Override
		public void paint(Graphics graphics)
		{
			if (_textBr == null || _textBr != null && _textBr.length < 0)
				return;
	
			r = getDecorationBounds();
			g = (Graphics2D) graphics;
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Font f = g.getFont();
//			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 2.0f));
			g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 1.2f * SwingUtils.getHiDpiScale() ));
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
//			int textBlockHeight = (maxStrHeight + 2) * _textBr.length;
			int xPos = (r.width - maxStrWidth) / 2;
//			int yPos = (int) (r.height - ((r.height - fm.getHeight()) / 2) * 0.6);
//			int yPos = (int) (r.height - ((r.height - textBlockHeight) / 2));

			int yPos = maxStrHeight * 3;

			if (xPos < 0)
				xPos = 0;

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

			// If text has NOT changed, no need to continue
			if (text.equals(_textSave))
				return;

			_textSave = text;

			_textBr = text.split("\n");
			_logger.debug("setWatermarkText: to '" + text + "'.");

			repaint();
		}
    }

	//---------------------------------------------------
	// WaterMark "big" used for pause etc... 
	//---------------------------------------------------
	private WatermarkBig _watermarkBig  = null;

	public void setWatermark()
	{
		if ( paused_chk.isSelected() )
		{
			setWatermarkText("Paused");
		}
		else
		{
			setWatermarkText(null);
		}
	}

	public void setWatermarkText(String str)
	{
		_watermarkBig.setWatermarkText(str);
	}

	class WatermarkBig
	extends AbstractComponentDecorator
	{
		public WatermarkBig(JComponent target, String text)
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
	** END: Watermark stuff
	**---------------------------------------------------
	*/

	public void setPlanText(String planStr, int sqlLine)
	{
		if (planStr == null)
			planStr = "";

		planTextArea.setText(planStr);

		if (sqlLine >= 0)
			setCaretToPlanLine(planTextArea, sqlLine);

		_viewXmlPlanInGui_but.setVisible(false);
		if (planStr.indexOf("<?xml version=") >= 0)
		{
			if (_viewXmlPlanInGui_chk.isSelected())
			{
				String xmlPlan = planTextArea.getText();

				// Remove "extra" stuff before the XML Plan
				int xmlPlanStartPos = xmlPlan.indexOf("<?xml version");
				if (xmlPlanStartPos > 0)
				{
					xmlPlan = xmlPlan.substring(xmlPlanStartPos);
				}

				// Do we still have a plan
				if (xmlPlan.startsWith("<?xml version"))
				{
					// FIXME: remember which component we are at... and resore focus to that component, with SwingUtils doLater
					final Component currentComponent = _historyStatementsTable; // Is this correct all the time ???
					AsePlanViewer.getInstance().loadXml(xmlPlan);
					
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							ProcessDetailFrame.this.requestFocus();
							currentComponent.requestFocusInWindow();
						}
					});
				}
			}
			else
			{
				_viewXmlPlanInGui_but.setVisible(true);
			}
		}
	}

	public void setPlanText(String planStr)
	{
		setPlanText(planStr, -1);
	}

	public static void setCaretToPlanLine(JTextArea text, int sqlLine) 
	{
//		text.setCaretPosition(0);

		int start = text.getText().indexOf("(at line "+sqlLine+")");
		int end = text.getText().indexOf("\n", start);
		end += 1;

		setSelectionAndMoveToTop(text, start, end);
	}

	public static void setSelectionAndMoveToTop(JTextArea text, int start, int end) 
	{
		if (start > 0  && end > 0)
		{
			// MARK the text as "selected" with the text colour red
			text.setSelectedTextColor(Color.RED);
			text.setCaretPosition(start);
			text.setSelectionStart(start);
			text.setSelectionEnd(end);
			// workaround to get the selection visible
			text.getCaret().setSelectionVisible(true);

			// Move the marked text to "top of the text field"
			try
			{
				//Rectangle rectAtPos = text.modelToView(start);
				Point pointAtPos = text.modelToView(start).getLocation();
				pointAtPos.x = 0; // Always point to LEFT in the viewport/scrollbar
				
				_logger.debug("text.modelToView(start): "+ pointAtPos);
				Container parent = text.getParent();
				while ( parent != null )
				{
					// if viewport, move the position to "top of viewport"
					if (parent instanceof JViewport)
					{
						((JViewport)parent).setViewPosition(pointAtPos);
						break;
					}
					// if scrollpane, move the position to "top of viewport"
					if (parent instanceof JScrollPane)
					{
						((JScrollPane)parent).getViewport().setViewPosition(pointAtPos);
						break;
					}
					// Get next parent if it's NOT a JViewport or JScrollPane
					parent = parent.getParent();
				}
			}
			catch (BadLocationException e)
			{
				_logger.debug("text.modelToView(): " + e);
			}
		}
	}

	@Override
	public void outOfMemoryHandler()
	{
		if (refressProcess != null)
		{
			refressProcess.outOfMemoryHandler();
		}
	}

	@Override
	public void memoryConsumption(int memoryLeftInMB)
	{
		if (refressProcess != null)
		{
			refressProcess.memoryConsumption(memoryLeftInMB);
		}
	}
}