/**
*/

package asemon;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.log4j.Logger;

import asemon.gui.LineNumberedPaper;
import asemon.gui.MainFrame;
import asemon.gui.TabularCntrPanel;
import asemon.gui.swing.GTabbedPane;
import asemon.utils.AseConnectionFactory;
import asemon.utils.AseConnectionUtils;
import asemon.utils.Configuration;
import asemon.utils.SwingUtils;



public class ProcessDetailFrame extends JFrame
{
	/** */
    private static final long serialVersionUID = -5013813797380259896L;

	/** Log4j logging. */
	private static Logger _logger          = Logger.getLogger(ProcessDetailFrame.class);

//	private   JFrame           pdf                          = null;
	private   int              kpid;
	public    int              processRefreshInterv         = 1;
	private   BorderLayout     borderLayout1                = new BorderLayout();
	private   JPanel           infoPanel                    = new JPanel();
	private   JPanel           statusPanel                  = new JPanel();
	private   GTabbedPane      mainTabbedPanel              = new GTabbedPane();
	private   JScrollPane      processDetailScroll          = new JScrollPane();
	private   JPanel           statementsPan                = new JPanel();
//private   JFrame           statementsFrame              = new JFrame("Statements");
	private   BorderLayout     borderLayout2                = new BorderLayout();
	public    TabularCntrPanel processObjectsPanel          = new TabularCntrPanel("Objects");
	public    TabularCntrPanel processWaitsPanel            = new TabularCntrPanel("Waits");
	public    TabularCntrPanel processLocksPanel            = new TabularCntrPanel("Locks");
public    JFrame           processObjectsFrame          = null;
public    JFrame           processWaitsFrame            = null;
public    JFrame           processLocksFrame            = null;
//public    JDialog          processObjectsFrame          = null;
//public    JDialog          processWaitsFrame            = null;
//public    JDialog          processLocksFrame            = null;
	private   JPanel           northPan                     = new JPanel();
	private   JSplitPane       activeSqlCapturedSqlSplitPan = new JSplitPane();
	private   JSplitPane       stmtBatchplanSplitPan        = new JSplitPane();
	private   JScrollPane      capturedStatementScrollPan   = new JScrollPane();
	private   JSplitPane       batchPlanSplitPan            = new JSplitPane();
	protected JScrollPane      batchScrollPan               = new JScrollPane();
	protected JScrollPane      planScrollPan                = new JScrollPane();
	//protected JTextArea        batchTextArea                = new JTextArea();
	protected JTextArea        batchTextArea                = new LineNumberedPaper(0,0);
	public    JTextArea        planTextArea                 = new JTextArea();
	private   JLabel           kpidLbl                      = new JLabel();
	private   XYLayout         xYLayout2                    = new XYLayout();
	private   JPanel           capturedStatementPan         = new JPanel();
	private   JPanel           captureWhereSqlPanel         = new JPanel();
	private   JLabel           label_captureWhereSql        = new JLabel(" Restrict to: WHERE");
	protected JComboBox        comboBox_captureWhereSql     = new JComboBox();
	private   JButton          buttom_captureWhereSql       = new JButton("Remove from template");
	private   JCheckBox        discardPreOpenStmntsCheckbox = new JCheckBox("Discard captured statements that happened before the dialouge was opened.", true);

	private   JPanel           planPanel                    = new JPanel();
	private   JPanel           batchPanel                   = new JPanel();
	private   JCheckBox        planShowEnableCheckbox       = new JCheckBox("Sample showplan text", true);
	private   JCheckBox        batchShowEnableCheckbox      = new JCheckBox("Sample SQL batch text", true);
	protected JCheckBox        sqlTextShowProcSrcCheckbox   = new JCheckBox("Show Stored Procedure source code in Batch text window", true);

	private   TitledBorder     titledBorderCurrent;
	private   TitledBorder     titledBorderPlan;
	private   TitledBorder     titledBorderCapture;
	public    TitledBorder     titledBorderBatch;
	private   JScrollPane      currentStatementsPan         = new JScrollPane();
	private   BorderLayout     borderLayout3                = new BorderLayout();
	protected JTextField       kpidFld                      = new JTextField();
	protected JTextField       spidFld                      = new JTextField();
	private   JLabel           spidLbl                      = new JLabel();
	//private   TitledBorder     titledBorder1;

	private RefreshProcess refressProcess;
	private Connection cnx;


	//
	TableCellRenderer currentStmtRenderer = new DefaultTableCellRenderer()
	{
        private static final long serialVersionUID = 5947221518946711279L;

		public Component getTableCellRendererComponent(JTable table,
		    Object value,boolean isSelected,boolean hasFocus,
		    int row, int column)
		{

			Component comp;
			if (isSelected)
			{
				_logger.debug("CURRENT statement table selected.");

				refressProcess.setSelectedStatement(-1);
				capturedStatementsTable.clearSelection();
			}

			comp = super.getTableCellRendererComponent(table,
					value,isSelected,hasFocus,
					row, column);//get the component(JLabel?)

			if (value==null)
				return comp;

			return comp;
		}
	};

	//
	public JTable currentStatementTable = new JTable()
	{
        private static final long serialVersionUID = -6016641348388707594L;

		public TableCellRenderer getCellRenderer(int row, int column)
		{
			return currentStmtRenderer;
		}

		/**
		 * To be able select/UN-SELECT rows in a table
		 * Called when a row/cell is about to change.
		 * getSelectedRow(), still shows what the *current* selection is
		 */
		public void changeSelection(int row, int column, boolean toggle, boolean extend) 
		{
			_logger.debug("changeSelection(row="+row+", column="+column+", toggle="+toggle+", extend="+extend+"), getSelectedRow()="+getSelectedRow()+", getSelectedColumn()="+getSelectedColumn());

			// if "row we clicked on" is equal to "currently selected row"
			// and also check that we do not do "left/right on keyboard"
			if (row == getSelectedRow() && (column == getSelectedColumn() || getSelectedColumn() < 0) )
			{
				toggle = true;
				_logger.debug("changeSelection(): change toggle to "+toggle+".");
			}
			
			super.changeSelection(row, column, toggle, extend);
		}
	};


	//
	TableCellRenderer capturedStmtRenderer = new DefaultTableCellRenderer()
	{
        private static final long serialVersionUID = -1766679067814631283L;

		public Component getTableCellRendererComponent(JTable table,
		    Object value,boolean isSelected,boolean hasFocus,
		    int row, int column)
		{
			Component comp;
			if (isSelected)
			{
				_logger.debug("CAPTURED statement table selected at row = "+row);

				refressProcess.setSelectedStatement(row);
				currentStatementTable.clearSelection();
			}

			comp = super.getTableCellRendererComponent(table,
					value,isSelected,hasFocus,
					row, column);//get the component(JLabel?)

			if (value==null)
				return comp;

			return comp;
		}
	};

	//
	public JTable capturedStatementsTable = new JTable()
	{
        private static final long serialVersionUID = 5214078163042572369L;

		public TableCellRenderer getCellRenderer(int row, int column)
		{
			return capturedStmtRenderer;
		}

		/**
		 * To be able select/UN-SELECT rows in a table
		 * Called when a row/cell is about to change.
		 * getSelectedRow(), still shows what the *current* selection is
		 */
		public void changeSelection(int row, int column, boolean toggle, boolean extend) 
		{
			_logger.debug("changeSelection(row="+row+", column="+column+", toggle="+toggle+", extend="+extend+"), getSelectedRow()="+getSelectedRow()+", getSelectedColumn()="+getSelectedColumn());

			// if "row we clicked on" is equal to "currently selected row"
			// and also check that we do not do "left/right on keyboard"
			if (row == getSelectedRow() && (column == getSelectedColumn() || getSelectedColumn() < 0) )
			{
				toggle = true;
				_logger.debug("changeSelection(): change toggle to "+toggle+".");
			}
			
			super.changeSelection(row, column, toggle, extend);
		}
	};

	private BorderLayout borderLayout4           = new BorderLayout();
	private JPanel       middleStatusPan         = new JPanel();
	private BorderLayout borderLayout5           = new BorderLayout();
	private JLabel       serverLbl               = new JLabel();
	public  JLabel       statusBarLbl            = new JLabel();
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
	private JComboBox    refreshIntFld           = new JComboBox();
	private JLabel       refreshIntLbl           = new JLabel();
	private JButton      refreshButton           = new JButton();
	private JButton      clearButton             = new JButton();
	private JButton      saveButton              = new JButton();

	private Image        frameIcon;

//	public ProcessDetailFrame(int k, int in_spid)
	public ProcessDetailFrame(int KPID)
	{
//		pdf = this;
		try
		{
			kpid = KPID;
			jbInit();
		}
		catch (Exception e)
		{
			_logger.error("In constructor", e);
		}
	}



	private void jbInit()
	throws Exception
	{
		ImageIcon icon = SwingUtils.readImageIcon(Asemon.class, "images/asemon_icon.gif");
		if (icon != null)
			setIconImage(icon.getImage());

		titledBorderCurrent = new TitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.white, new Color(103, 101, 98), new Color(148, 145, 140)), "Current Statement");
		titledBorderPlan    = new TitledBorder(BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140)), "Plan text");
		titledBorderCapture = new TitledBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED, Color.white, Color.white, new Color(103, 101, 98), new Color(148, 145, 140)), "Captured Statements");
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
			public void windowClosing(WindowEvent e)
			{
				this_windowClosing(e);
			}
		});
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
		capturedStatementScrollPan.getViewport().setBackground(new Color(230, 230, 230));
		capturedStatementScrollPan.setBorder(titledBorderCapture);
		capturedStatementScrollPan.setPreferredSize(new Dimension(454, 200));
		processDetailScroll.setBorder(BorderFactory.createLoweredBevelBorder());
		kpidLbl.setText("KPID :");
		northPan.setPreferredSize(new Dimension(10, 90));
		northPan.setLayout(borderLayout3);
		currentStatementsPan.getViewport().setBackground(new Color(230, 230, 230));
		currentStatementsPan.setBorder(titledBorderCurrent);

		capturedStatementsTable.setBackground(new Color(230, 230, 230));
		capturedStatementsTable.setShowHorizontalLines(false);
		capturedStatementsTable.setShowVerticalLines(false);
		capturedStatementsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		capturedStatementsTable.setGridColor(new Color(230, 230, 230));
		capturedStatementsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		capturedStatementsTable.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				capturedStatementsTable_mouseClicked(e);
			}
		});
		// Fixing/setting background selection color... on some platforms it seems to be a strange color
		// on XP a gray color of "r=178,g=180,b=191" is the default, which looks good on the screen
		Configuration conf = Configuration.getInstance(Configuration.CONF);
		if (conf != null)
		{
			if (conf.getBooleanProperty("table.setSelectionBackground", true))
			{
				Color newBg = new Color(
					conf.getIntProperty("table.setSelectionBackground.r", 178),
					conf.getIntProperty("table.setSelectionBackground.g", 180),
					conf.getIntProperty("table.setSelectionBackground.b", 191));

				_logger.debug("table.setSelectionBackground("+newBg+").");
				capturedStatementsTable.setSelectionBackground(newBg);
			}
		}
		else
		{
			Color bgc = capturedStatementsTable.getSelectionBackground();
			if ( ! (bgc.getRed()==178 && bgc.getGreen()==180 && bgc.getBlue()==191) )
			{
				Color newBg = new Color(178, 180, 191);
				_logger.debug("table.setSelectionBackground("+newBg+"). Config could not be read, trusting defaults...");
				capturedStatementsTable.setSelectionBackground(newBg);
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
		currentStatementTable.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				currentSstatementTable_mouseClicked(e);
			}
		});
		currentStatementTable.setGridColor(new Color(230, 230, 230));
		currentStatementTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		currentStatementTable.setShowVerticalLines(false);
		currentStatementTable.setShowHorizontalLines(false);
		currentStatementTable.setBackground(new Color(230, 230, 230));
		currentStatementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

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
				currentStatementTable.setSelectionBackground(newBg);
			}
		}
		else
		{
			Color bgc = currentStatementTable.getSelectionBackground();
			if ( ! (bgc.getRed()==178 && bgc.getGreen()==180 && bgc.getBlue()==191) )
			{
				Color newBg = new Color(178, 180, 191);
				_logger.debug("table.setSelectionBackground("+newBg+"). Config could not be read, trusting defaults...");
				currentStatementTable.setSelectionBackground(newBg);
			}
		}
		
		
		
		statusPanel.setLayout(borderLayout4);
		statusPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		statusPanel.setPreferredSize(new Dimension(10, 21));
		middleStatusPan.setLayout(borderLayout5);
		serverLbl.setBorder(BorderFactory.createLoweredBevelBorder());
		serverLbl.setPreferredSize(new Dimension(200, 17));
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
			public void actionPerformed(ActionEvent e)
			{
				refreshIntFld_actionPerformed(e);
			}
		});
		refreshButton.setText("Refresh");
		refreshButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				refreshButton_actionPerformed(e);
			}
		});
		clearButton.setText("Clear");
		clearButton.setToolTipText("Clear information in all Table below.");
		clearButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				clear();
			}
		});
		saveButton.setText("Save & Clear");
		saveButton.setToolTipText("Save Captured Statements Table to file ('capStmts.YYYY-MM-DD.bcp', 'capStmts.YYYY-MM-DD.txt', 'capStmts.YYYY-MM-DD.ddl.sql' in ASEMON_SAVE_DIR or ASEMON_HOME).");
		saveButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveCapturedStatements();
			}
		});

		this.getContentPane().add(infoPanel, BorderLayout.NORTH);
		this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
		this.getContentPane().add(mainTabbedPanel, BorderLayout.CENTER);

		if (kpid > 0)
		{
			mainTabbedPanel.add(processDetailScroll, "Summary");
			mainTabbedPanel.add(statementsPan,       "Statements");
			mainTabbedPanel.add(processObjectsPanel, processObjectsPanel.getPanelName());
			mainTabbedPanel.add(processWaitsPanel,   processWaitsPanel.getPanelName());
			mainTabbedPanel.add(processLocksPanel,   processLocksPanel.getPanelName());
		}
		else
		{
			mainTabbedPanel.add(statementsPan,       "Statements");
			mainTabbedPanel.add(processObjectsPanel, processObjectsPanel.getPanelName());
			mainTabbedPanel.add(processWaitsPanel,   processWaitsPanel.getPanelName());
			mainTabbedPanel.add(processLocksPanel,   processLocksPanel.getPanelName());
		}

		mainTabbedPanel.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				mainTabbedPanel_mouseClicked(e);
			}
		});
//statementsFrame    .getContentPane().add(statementsPan);
//processObjectsFrame.getContentPane().add(processObjectsPanel);
//processWaitsFrame  .getContentPane().add(processWaitsPanel);
//processLocksFrame  .getContentPane().add(processLocksPanel);
		processObjectsFrame = new JFrame(processObjectsPanel.getPanelName());
		processObjectsFrame.setIconImage(frameIcon);
		processObjectsFrame.setSize(new Dimension(900, 300));
		processObjectsFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				mainTabbedPanel.add(processObjectsPanel.getPanelName(), processObjectsPanel);
			}
		});

		processWaitsFrame = new JFrame(processWaitsPanel.getPanelName());
		processWaitsFrame.setIconImage(frameIcon);
		processWaitsFrame.setSize(new Dimension(750, 500));
		processWaitsFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				mainTabbedPanel.add(processWaitsPanel.getPanelName(), processWaitsPanel);
			}
		});

		processLocksFrame = new JFrame(processLocksPanel.getPanelName());
		processLocksFrame.setIconImage(frameIcon);
		processLocksFrame.setSize(new Dimension(900, 300));
		processLocksFrame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				mainTabbedPanel.add(processLocksPanel.getPanelName(), processLocksPanel);
			}
		});


		processDetailScroll.getViewport().add(processDetailPan, null);
		statementsPan.add(stmtBatchplanSplitPan, BorderLayout.CENTER);

		stmtBatchplanSplitPan.add(activeSqlCapturedSqlSplitPan, JSplitPane.TOP);
		stmtBatchplanSplitPan.add(batchPlanSplitPan, JSplitPane.BOTTOM);

		activeSqlCapturedSqlSplitPan.add(currentStatementsPan, JSplitPane.TOP);
		activeSqlCapturedSqlSplitPan.add(capturedStatementPan, JSplitPane.BOTTOM);

		// SQL BATCH
		batchPlanSplitPan.add(batchPanel, JSplitPane.LEFT);
		batchPanel.setLayout(new BorderLayout());
		batchPanel.add(batchShowEnableCheckbox, BorderLayout.NORTH);
batchPanel.add(sqlTextShowProcSrcCheckbox, BorderLayout.SOUTH);
		batchPanel.add(batchScrollPan, BorderLayout.CENTER);
		batchScrollPan.getViewport().add(batchTextArea, null);
		batchShowEnableCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setSqlTextSample( batchShowEnableCheckbox.isSelected() );
				saveProps();
			}
		});
		sqlTextShowProcSrcCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				saveProps();
			}
		});

		// PLAN
		batchPlanSplitPan.add(planPanel, JSplitPane.RIGHT);
		planPanel.setLayout(new BorderLayout());
		planPanel.add(planShowEnableCheckbox, BorderLayout.NORTH);
		planPanel.add(planScrollPan, BorderLayout.CENTER);
		planScrollPan.getViewport().add(planTextArea, null);
		planShowEnableCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setPlanTextSample( planShowEnableCheckbox.isSelected() );
				saveProps();
			}
		});


		currentStatementsPan.getViewport().add(currentStatementTable, null);
		capturedStatementScrollPan.getViewport().add(capturedStatementsTable, null);

		kpidFld.setText(Integer.toString(kpid));

		// statementsPan.add(northPan, BorderLayout.NORTH);
		// northPan.add(currentStatementsPan, BorderLayout.CENTER);

		capturedStatementPan.setLayout(new BorderLayout());
		capturedStatementPan.add(captureWhereSqlPanel, BorderLayout.NORTH);
		capturedStatementPan.add(capturedStatementScrollPan, BorderLayout.CENTER);

		captureWhereSqlPanel.setLayout(new BorderLayout());
		captureWhereSqlPanel.add(label_captureWhereSql, BorderLayout.WEST);
		captureWhereSqlPanel.add(comboBox_captureWhereSql, BorderLayout.CENTER);
		captureWhereSqlPanel.add(buttom_captureWhereSql, BorderLayout.EAST);
		captureWhereSqlPanel.add(discardPreOpenStmntsCheckbox, BorderLayout.SOUTH);

		comboBox_captureWhereSql.setToolTipText("Add your extra where clauses on the monSysStatement table. make sure that only columns in theat table are used. ASEMON's errorlog will show faulty SQL statements.");
		comboBox_captureWhereSql.setEditable(true);
		comboBox_captureWhereSql.addItem("");
		comboBox_captureWhereSql.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				comboBox_captureWhereSql_actionPerformed(e);
			}
		});
		buttom_captureWhereSql.setToolTipText("Remove the current restriction from the template.");
		buttom_captureWhereSql.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				buttom_captureWhereSql_actionPerformed(e);
			}
		});
		discardPreOpenStmntsCheckbox.setToolTipText("If you want to discard or show events from monSysStatements that happened prior to the window was open.");
		discardPreOpenStmntsCheckbox.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (refressProcess != null)
					refressProcess.setDiscardPreOpenStmnts( discardPreOpenStmntsCheckbox.isSelected() );
				saveProps();
			}
		});


		infoPanel.add(spidLbl,                 new XYConstraints(11,  9,   -1,  -1));
		infoPanel.add(kpidLbl,                 new XYConstraints(137, 8,   44,  19));
		infoPanel.add(spidFld,                 new XYConstraints(48,  8,   74,  19));
		infoPanel.add(kpidFld,                 new XYConstraints(178, 8,   67,  19));
		infoPanel.add(suser_nameLbl,           new XYConstraints(254, 8,   84,  19));
		infoPanel.add(suser_nameFld,           new XYConstraints(342, 8,   88,  19));
		infoPanel.add(refreshIntLbl,           new XYConstraints(443, 8,   111, 18));
		infoPanel.add(refreshIntFld,           new XYConstraints(556, 8,   50,  19));
		infoPanel.add(refreshButton,           new XYConstraints(635, 5,   85,  25));
		infoPanel.add(clearButton,             new XYConstraints(735, 5,   85,  25));
		infoPanel.add(saveButton,              new XYConstraints(835, 5,   100, 25));
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

		currentStatementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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

		// Open the connection
//		cnx = OpenConnectionDlg.getAnotherConnection(Version.getAppName()+"-spid", true);
		cnx = AseConnectionFactory.getConnection(null, Version.getAppName()+"-spid", null);
		if ( ! AseConnectionUtils.checkForMonitorOptions(cnx, AseConnectionFactory.getUser(), true, this) )
		{
			// FIXME: should we continue here or not...
		}

		// Display servername on status bar
		serverLbl.setText(MainFrame.getStatus(MainFrame.ST_CONNECT));

		loadProps();

		// Start refreshing this frame
		if (cnx != null)
		{
			refressProcess = new RefreshProcess(this, cnx, kpid);

			refressProcess.setSqlTextSample( batchShowEnableCheckbox.isSelected() );
			refressProcess.setPlanTextSample( planShowEnableCheckbox.isSelected() );
			refressProcess.setDiscardPreOpenStmnts( discardPreOpenStmntsCheckbox.isSelected() );

			// use the selected where clause...
			String sql = (String) comboBox_captureWhereSql.getSelectedItem();
			refressProcess.setCaptureRestriction(sql);

			refressProcess.start();
		}

		pack();
		setVisible(true);
	}

	private void capturedStatementsTable_mouseClicked(MouseEvent e)
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

		if (processObjectsFrame != null) processObjectsFrame.dispose();
		if (processWaitsFrame   != null) processWaitsFrame  .dispose();
		if (processLocksFrame   != null) processLocksFrame  .dispose();
	}

	private void currentSstatementTable_mouseClicked(MouseEvent e)
	{
	}

	private void refreshIntFld_actionPerformed(ActionEvent e)
	{
		String interv = (String) refreshIntFld.getSelectedItem();
		processRefreshInterv = Integer.parseInt(interv);

		if (refressProcess != null)
			refressProcess.setRefreshInterval(processRefreshInterv);
	}

	private void refreshButton_actionPerformed(ActionEvent e)
	{
		if (refressProcess != null)
			refressProcess.refreshProcess();
	}

	private void buttom_captureWhereSql_actionPerformed(ActionEvent e)
	{
		String sql = (String) comboBox_captureWhereSql.getSelectedItem();
		if (sql != null)
		{
			sql = sql.trim();

			// Check if the string is in the template list
			Object model = comboBox_captureWhereSql.getModel();
			if (model instanceof DefaultComboBoxModel)
			{
				int index = ((DefaultComboBoxModel) model).getIndexOf(sql);

				// If it does not exist in the list save it
				if (index > 0)
				{
					comboBox_captureWhereSql.removeItem(sql);
					//comboBox_captureWhereSql.setSelectedIndex(0);
				}
			}
		}

		saveProps();
	}

	private void comboBox_captureWhereSql_actionPerformed(ActionEvent e)
	{
		//_logger.debug("comboBox_captureWhereSql.actionPerformed(): e.getActionCommand()='" + e.getActionCommand() + "', ActionEvent=" + e);

		String sql = (String) comboBox_captureWhereSql.getSelectedItem();
		sql = sql.trim();

		// Check if the select text is in the template list
		// If so, name the buttom "remove", otherwisse "save"
		Object model = comboBox_captureWhereSql.getModel();
		if (model instanceof DefaultComboBoxModel)
		{
			int index = ((DefaultComboBoxModel) model).getIndexOf(sql);
			if ( index == -1 )
				comboBox_captureWhereSql.addItem(sql);
		}

		// set the restrictor in refressProcess
		if (refressProcess != null)
			refressProcess.setCaptureRestriction(sql);
	}




	private void saveProps()
  	{
		Configuration asemonProps = Configuration.getInstance(Configuration.TEMP);
		String base = "processDetailFrame.spid.";

		if (asemonProps != null)
		{
			// Get rid of all fields first
			//asemonProps.removeAll(base);

			asemonProps.setProperty(base + "window.width", this.getSize().width);
			asemonProps.setProperty(base + "window.height", this.getSize().height);
			asemonProps.setProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			asemonProps.setProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			asemonProps.setProperty(base + "currentStatements.width", currentStatementsPan.getSize().width);
			asemonProps.setProperty(base + "currentStatements.height", currentStatementsPan.getSize().height);

			asemonProps.setProperty(base + "capturedStatements.width", capturedStatementScrollPan.getSize().width);
			asemonProps.setProperty(base + "capturedStatements.height", capturedStatementScrollPan.getSize().height);

			asemonProps.setProperty(base + "batch.width", batchScrollPan.getSize().width);
			asemonProps.setProperty(base + "batch.height", batchScrollPan.getSize().height);
			asemonProps.setProperty(base + "batch.sample", batchShowEnableCheckbox.isSelected());
			asemonProps.setProperty(base + "batch.spSrcInBatch", sqlTextShowProcSrcCheckbox.isSelected());

			asemonProps.setProperty(base + "plan.width", planScrollPan.getSize().width);
			asemonProps.setProperty(base + "plan.height", planScrollPan.getSize().height);
			asemonProps.setProperty(base + "plan.sample", planShowEnableCheckbox.isSelected());

			asemonProps.setProperty(base + "objects.window.active", processObjectsFrame.isVisible());
			asemonProps.setProperty(base + "objects.window.width",  processObjectsFrame.getSize().width);
			asemonProps.setProperty(base + "objects.window.height", processObjectsFrame.getSize().height);
			if ( processObjectsFrame.isVisible() )
			{
			asemonProps.setProperty(base + "objects.window.pos.x",  processObjectsFrame.getLocationOnScreen().x);
			asemonProps.setProperty(base + "objects.window.pos.y",  processObjectsFrame.getLocationOnScreen().y);
			}

			asemonProps.setProperty(base + "waits.window.active", processWaitsFrame.isVisible());
			asemonProps.setProperty(base + "waits.window.width",  processWaitsFrame.getSize().width);
			asemonProps.setProperty(base + "waits.window.height", processWaitsFrame.getSize().height);
			if ( processWaitsFrame.isVisible() )
			{
			asemonProps.setProperty(base + "waits.window.pos.x",  processWaitsFrame.getLocationOnScreen().x);
			asemonProps.setProperty(base + "waits.window.pos.y",  processWaitsFrame.getLocationOnScreen().y);
			}

			asemonProps.setProperty(base + "locks.window.active", processLocksFrame.isVisible());
			asemonProps.setProperty(base + "locks.window.width",  processLocksFrame.getSize().width);
			asemonProps.setProperty(base + "locks.window.height", processLocksFrame.getSize().height);
			if ( processLocksFrame.isVisible() )
			{
			asemonProps.setProperty(base + "locks.window.pos.x",  processLocksFrame.getLocationOnScreen().x);
			asemonProps.setProperty(base + "locks.window.pos.y",  processLocksFrame.getLocationOnScreen().y);
			}

			// Get rid of all fields first
			asemonProps.removeAll(base + "plan.extraWhere.");
			int saveCount = 0;
			for (int i=1; i<comboBox_captureWhereSql.getItemCount(); i++)
			{
				Object o = comboBox_captureWhereSql.getItemAt(i);
				if (o != null)
				{
					saveCount++;
					asemonProps.setProperty(base + "plan.extraWhere."+saveCount, o.toString());

					_logger.debug("saveProps(): processDetailFrame.spid.plan.extraWhere."+saveCount+"='"+o.toString()+"'.");
				}
			}
			asemonProps.setProperty(   base + "plan.extraWhere.count", saveCount);
			_logger.debug("saveProps(): processDetailFrame.spid.plan.extraWhere.count='"+saveCount+"'.");

			asemonProps.setProperty(   base + "plan.extraWhere.active", Math.max(0, comboBox_captureWhereSql.getSelectedIndex()));
			_logger.debug("saveProps(): processDetailFrame.spid.plan.extraWhere.active='"+Math.max(0, comboBox_captureWhereSql.getSelectedIndex())+"'.");


			asemonProps.setProperty(base + "plan.discardPreOpenStmnts", discardPreOpenStmntsCheckbox.isSelected());

			asemonProps.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = -1;
		int     height    = -1;
		int     x         = -1;
		int     y         = -1;
		boolean winActive = false;
		boolean checkBox1 = false;
		boolean checkBox2 = false;

		Configuration props = Configuration.getInstance(Configuration.TEMP);
		String base = "processDetailFrame.spid.";

		if (props == null)
			return;

		width  = props.getIntProperty(base + "window.width",  1000);
		height = props.getIntProperty(base + "window.height", 700);
		x      = props.getIntProperty(base + "window.pos.x",  -1);
		y      = props.getIntProperty(base + "window.pos.y",  -1);
		if (width != -1 && height != -1)
		{
			this.setPreferredSize(new Dimension(width, height));
		}
		if (x != -1 && y != -1)
		{
			this.setLocation(x, y);
		}
		else
		{
			_logger.debug("Open ProcessDetailFrame in center of window.");
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
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

		width  = props.getIntProperty(base + "currentStatements.width",  1000);
		height = props.getIntProperty(base + "currentStatements.height", 110);
		if (width != -1 && height != -1)
		{
			currentStatementsPan.setPreferredSize(new Dimension(width, height));
		}

		width  = props.getIntProperty(base + "capturedStatements.width",  1000);
		height = props.getIntProperty(base + "capturedStatements.height", 130);
		if (width != -1 && height != -1)
		{
			capturedStatementScrollPan.setPreferredSize(new Dimension(width, height));
		}

		checkBox1 = props.getBooleanProperty(base + "batch.sample",       true);
		checkBox2 = props.getBooleanProperty(base + "batch.spSrcInBatch", true);
		width  = props.getIntProperty(base + "batch.width",  -1);
		height = props.getIntProperty(base + "batch.height", -1);
		if (width != -1 && height != -1)
		{
			batchScrollPan.setPreferredSize(new Dimension(width, height));
		}
		batchShowEnableCheckbox.setSelected(checkBox1);
		sqlTextShowProcSrcCheckbox.setSelected(checkBox2);

		checkBox1 = props.getBooleanProperty(base + "plan.sample", true);
		width  = props.getIntProperty(base + "plan.width",  -1);
		height = props.getIntProperty(base + "plan.height", -1);
		if (width != -1 && height != -1)
		{
			planScrollPan.setPreferredSize(new Dimension(width, height));
		}
		planShowEnableCheckbox.setSelected(checkBox1);

		// processObjects
		winActive = props.getBooleanProperty(base + "objects.window.active", false);
		width  = props.getIntProperty(base + "objects.window.width",  -1);
		height = props.getIntProperty(base + "objects.window.height", -1);
		x      = props.getIntProperty(base + "objects.window.pos.x",  -1);
		y      = props.getIntProperty(base + "objects.window.pos.y",  -1);
		if (width != -1 && height != -1 && x != -1 && y != -1)
		{
			processObjectsFrame.setSize(width, height);
			processObjectsFrame.setLocation(x, y);
		}
		if (winActive)
		{
			processObjectsFrame.getContentPane().add(processObjectsPanel);
			processObjectsFrame.setVisible(winActive);
		}

		// processWaits
		winActive = props.getBooleanProperty(base + "waits.window.active", "false");
		width  = props.getIntProperty(base + "waits.window.width",  -1);
		height = props.getIntProperty(base + "waits.window.height", -1);
		x      = props.getIntProperty(base + "waits.window.pos.x",  -1);
		y      = props.getIntProperty(base + "waits.window.pos.y",  -1);
		if (width != -1 && height != -1 && x != -1 && y != -1)
		{
			processWaitsFrame.setSize(width, height);
			processWaitsFrame.setLocation(x, y);
		}
		if (winActive)
		{
			processWaitsFrame.getContentPane().add(processWaitsPanel);
			processWaitsFrame.setVisible(winActive);
		}

		// processLocks
		winActive = props.getBooleanProperty(base + "locks.window.active", "false");
		width  = props.getIntProperty(base + "locks.window.width",  -1);
		height = props.getIntProperty(base + "locks.window.height", -1);
		x      = props.getIntProperty(base + "locks.window.pos.x",  -1);
		y      = props.getIntProperty(base + "locks.window.pos.y",  -1);
		if (width != -1 && height != -1 && x != -1 && y != -1)
		{
			processLocksFrame.setSize(width, height);
			processLocksFrame.setLocation(x, y);
		}
		if (winActive)
		{
			processLocksFrame.getContentPane().add(processLocksPanel);
			processLocksFrame.setVisible(winActive);
		}

		int count   = props.getIntProperty(base + "plan.extraWhere.count", -1);
		int active  = props.getIntProperty(base + "plan.extraWhere.active", -1);
		if ( count != -1  && active != -1 )
		{
			_logger.debug("loadProps(): processDetailFrame.spid.plan.extraWhere.active='"+active+"'.");

			active = Math.max(0, active);
			for (int i=1; i<=count; i++)
			{
				String str = props.getProperty(base + "plan.extraWhere."+i).trim();
				comboBox_captureWhereSql.insertItemAt(str, i);
				_logger.debug("loadProps(): processDetailFrame.spid.plan.extraWhere."+i+"='"+str+"'.");

			}
			_logger.debug("loadProps(): Set active template to index="+active+".");
			comboBox_captureWhereSql.setSelectedIndex(active);
		}
		else
		{
			// if we cant find anything in the configuration file, add some defaults
			// which will be written to the config file on next save...
		    comboBox_captureWhereSql.addItem("WaitTime > 1000");
		    comboBox_captureWhereSql.addItem("LogicalReads > 100");
		    comboBox_captureWhereSql.addItem("LogicalReads > 1000");
		    comboBox_captureWhereSql.addItem("LogicalReads > 5000");
		    comboBox_captureWhereSql.addItem("LogicalReads > 10000");
		    comboBox_captureWhereSql.addItem("(LogicalReads > 10000 or PhysicalReads > 300)");
		    comboBox_captureWhereSql.addItem("datediff(ms, StartTime, EndTime) > 1000");
		    comboBox_captureWhereSql.addItem("datediff(ms, StartTime, EndTime) > 5000");
		}

		checkBox1  = props.getBooleanProperty(base + "plan.discardPreOpenStmnts", true);
		discardPreOpenStmntsCheckbox.setSelected(checkBox1);
	}

  	public void setRefreshError(Exception e)
	{
	}

	public void clear()
	{
		if (refressProcess != null)
			refressProcess.clear();
	}

	public void saveCapturedStatements()
	{
		if (refressProcess != null)
			refressProcess.saveCapturedStatementsToFile();

		clear();
	}

	private void mainTabbedPanel_mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			Point p = e.getPoint();
			int index = mainTabbedPanel.indexAtLocation(p.x, p.y);
			String tabName = mainTabbedPanel.getTitleAt(index);

			_logger.debug("TabedPanel: doubleClick on index="+index+", name='"+tabName+"'.");

			// OBJECTS
			if (tabName.equals(processObjectsPanel.getPanelName()))
			{
//				if (processObjectsFrame == null)
//				{
//					//processObjectsFrame = new JDialog(pdf, "Objects");
//					processObjectsFrame = new JFrame(processObjectsPanel.getName());
//					processObjectsFrame.setIconImage(frameIcon);
//					processObjectsFrame.setSize(new Dimension(900, 300));
//					processObjectsFrame.addWindowListener(new WindowAdapter()
//					{
//						public void windowClosing(WindowEvent e)
//						{
//							mainTabbedPanel.add(processObjectsPanel.getName(), processObjectsPanel);
//						}
//					});
//				}
				processObjectsFrame.getContentPane().add(processObjectsPanel);
				processObjectsFrame.setVisible(true);
			}
			// WAITS
			if (tabName.equals(processWaitsPanel.getPanelName()))
			{
//				if (processWaitsFrame == null)
//				{
//					//processWaitsFrame = new JDialog(pdf, "Waits");
//					processWaitsFrame = new JFrame(processWaitsPanel.getName());
//					processWaitsFrame.setIconImage(frameIcon);
//					processWaitsFrame.setSize(new Dimension(750, 500));
//					processWaitsFrame.addWindowListener(new WindowAdapter()
//					{
//						public void windowClosing(WindowEvent e)
//						{
//							mainTabbedPanel.add(processWaitsPanel.getName(), processWaitsPanel);
//						}
//					});
//				}
				processWaitsFrame.getContentPane().add(processWaitsPanel);
				processWaitsFrame.setVisible(true);
			}
			// LOCKS
			if (tabName.equals(processLocksPanel.getPanelName()))
			{
//				if (processLocksFrame == null)
//				{
//					//processLocksFrame = new JDialog(pdf, "Locks");
//					processLocksFrame = new JFrame(processLocksPanel.getName());
//					processLocksFrame.setIconImage(frameIcon);
//					processLocksFrame.setSize(new Dimension(900, 300));
//					processLocksFrame.addWindowListener(new WindowAdapter()
//					{
//						public void windowClosing(WindowEvent e)
//						{
//							mainTabbedPanel.add(processLocksPanel.getName(), processLocksPanel);
//						}
//					});
//				}
				processLocksFrame.getContentPane().add(processLocksPanel);
				processLocksFrame.setVisible(true);
			}
		} // end: double-click
	} // end: method
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

}