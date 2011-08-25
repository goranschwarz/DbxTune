package asemon.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.util.Properties;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;

import asemon.GetCounters;
import asemon.GetCountersGui;
import asemon.cm.CountersModel;
import asemon.utils.Configuration;
import asemon.utils.StringUtil;
import asemon.utils.SwingUtils;

public class TcpConfigDialog
extends JDialog
implements ActionListener, TableModelListener
{
	private static Logger _logger = Logger.getLogger(OfflineSessionVeiwer.class);
	private static final long	serialVersionUID	= -8717629657711689568L;

//	private Frame                  _owner           = null;

	private LocalTable             _table           = null;


	// PANEL: OK-CANCEL
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");
	private JButton                _apply           = new JButton("Apply");
	
	private static final String DIALOG_TITLE = "Settings for All Collector Tabs";

	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	private TcpConfigDialog(Frame owner)
	{
		super(owner, DIALOG_TITLE, true);
//		_owner           = owner;

		initComponents();
	}

	public static void showDialog(Frame owner)
	{
		TcpConfigDialog params = new TcpConfigDialog(owner);
		params.setLocationRelativeTo(owner);
		params.setVisible(true);
		params.dispose();
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
//		super(_owner);
//		if (_owner != null)
//			setIconImage(_owner.getIconImage());

//		setTitle(DIALOG_TITLE);

		JPanel panel = new JPanel();
		//panel.setLayout(new MigLayout("debug, insets 0 0 0 0, wrap 1","",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout("insets 0, wrap 1","",""));   // insets Top Left Bottom Right

//		panel.add(createTopPanel(),      "grow, push");
		panel.add(createTablePanel(),    "grow, push, height 100%");
		panel.add(createOkCancelPanel(), "bottom, right, push");

		loadProps();

		setContentPane(panel);

		initComponentActions();
	}

//	private JPanel createTopPanel()
//	{
//		JPanel panel = SwingUtils.createPanel("Top", false);
//
//		return panel;
//	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");

		_apply.setEnabled(false);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
		_apply        .addActionListener(this);

		return panel;
	}

	private JPanel createTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
		panel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));

		// Create the table
		_table = new LocalTable();
		_table.getModel().addTableModelListener(this); // call this.tableChanged(TableModelEvent) when the table changed

		JScrollPane scroll = new JScrollPane(_table);
//		_watermark = new Watermark(scroll, "");
		panel.add(scroll, "push, grow, height 100%, wrap");

		return panel;
	}

	private void initComponentActions()
	{
		//---- Top PANEL -----

		//---- Tab PANEL -----

		this.addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				saveProps();
			}
		});
	}
	/*---------------------------------------------------
	** END: component initialization
	**---------------------------------------------------
	*/



	/*---------------------------------------------------
	** BEGIN: Action Listeners, and helper methods for it
	**---------------------------------------------------
	*/
	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			setVisible(false);
		}

		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			doApply();
			saveProps();
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			doApply();
			saveProps();
		}
    }

	public void tableChanged(TableModelEvent e)
	{
//		System.out.println("tableChanged(): TableModelEvent="+e);
		_apply.setEnabled(true);
	}


	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	private void doApply()
	{
		for (int r=0; r<_table.getRowCount(); r++)
		{
			String  tabName   = (String)  _table.getValueAt(r, TAB_POS_TAB_NAME);

			int     postpone  = ((Integer) _table.getValueAt(r, TAB_POS_POSTPONE)).intValue();
			boolean paused    = ((Boolean) _table.getValueAt(r, TAB_POS_PAUSED)).booleanValue();
			boolean bgPoll    = ((Boolean) _table.getValueAt(r, TAB_POS_BG)).booleanValue();
			boolean rnc20     = ((Boolean) _table.getValueAt(r, TAB_POS_RNC20)).booleanValue();

			boolean storePcs  = ((Boolean) _table.getValueAt(r, TAB_POS_STORE_PCS)).booleanValue();
			boolean storeAbs  = ((Boolean) _table.getValueAt(r, TAB_POS_STORE_ABS)).booleanValue();
			boolean storeDiff = ((Boolean) _table.getValueAt(r, TAB_POS_STORE_DIFF)).booleanValue();
			boolean storeRate = ((Boolean) _table.getValueAt(r, TAB_POS_STORE_RATE)).booleanValue();

			CountersModel cm  = GetCounters.getCmByDisplayName(tabName);
			
			if (cm == null)
			{
				_logger.warn("The cm named '"+tabName+"' cant be found in the 'GetCounters' object.");
				continue;
			}

			if (_logger.isDebugEnabled())
			{
				String debugStr = "doApply() name="+StringUtil.left("'"+tabName+"'", 30) +
					" postpone="+(_table.isCellChanged(r, TAB_POS_POSTPONE  ) ? "X":" ") +
					" paused="  +(_table.isCellChanged(r, TAB_POS_PAUSED    ) ? "X":" ") +
					" bg="      +(_table.isCellChanged(r, TAB_POS_BG        ) ? "X":" ") +
					" rnc20="   +(_table.isCellChanged(r, TAB_POS_RNC20     ) ? "X":" ") +
					" pcs="     +(_table.isCellChanged(r, TAB_POS_STORE_PCS ) ? "X":" ") +
					" pcsAbs="  +(_table.isCellChanged(r, TAB_POS_STORE_ABS ) ? "X":" ") +
					" pcsDiff=" +(_table.isCellChanged(r, TAB_POS_STORE_DIFF) ? "X":" ") +
					" pcsRate=" +(_table.isCellChanged(r, TAB_POS_STORE_RATE) ? "X":" ");
				_logger.debug(debugStr);
			}

			if (_table.isCellChanged(r, TAB_POS_POSTPONE  )) cm.setPostponeTime(                 postpone);
			if (_table.isCellChanged(r, TAB_POS_PAUSED    )) cm.setPauseDataPolling(             paused);
			if (_table.isCellChanged(r, TAB_POS_BG        )) cm.setBackgroundDataPollingEnabled( bgPoll);
			if (_table.isCellChanged(r, TAB_POS_RNC20     )) cm.setNegativeDiffCountersToZero(   rnc20);

			if (_table.isCellChanged(r, TAB_POS_STORE_PCS )) cm.setPersistCounters(    storePcs);
			if (_table.isCellChanged(r, TAB_POS_STORE_ABS )) cm.setPersistCountersAbs( storeAbs);
			if (_table.isCellChanged(r, TAB_POS_STORE_DIFF)) cm.setPersistCountersDiff(storeDiff);
			if (_table.isCellChanged(r, TAB_POS_STORE_RATE)) cm.setPersistCountersRate(storeRate);
		}

		_table.resetCellChanges();
		_apply.setEnabled(false);
	}


	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		String base = "tcpConfigDialog.";

		if (tmpConf != null)
		{
			tmpConf.setProperty(base + "window.width", this.getSize().width);
			tmpConf.setProperty(base + "window.height", this.getSize().height);
			tmpConf.setProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = 675;  // initial window with   if not opened before
		int     height    = 630;  // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		String base = "tcpConfigDialog.";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getIntProperty(base + "window.width",  width);
		height = tmpConf.getIntProperty(base + "window.height", height);
		x      = tmpConf.getIntProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getIntProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			this.setLocation(x, y);
		}
		else
		{
			SwingUtils.centerWindow(this);
		}
	}
	/*---------------------------------------------------
	** END: Property handling
	**---------------------------------------------------
	*/




	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// SUB-CLASSES: LocalTable & LocalTableModel ///////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////

	private static final String[] TAB_HEADER = {"Icon", "Tab Name", "Postpone", "Paused", "Background", "Reset NC20", "Store PCS", "Abs", "Diff", "Rate"};
	private static final int TAB_POS_ICON       = 0;
	private static final int TAB_POS_TAB_NAME   = 1;
	private static final int TAB_POS_POSTPONE   = 2;
	private static final int TAB_POS_PAUSED     = 3;
	private static final int TAB_POS_BG         = 4;
	private static final int TAB_POS_RNC20      = 5;
	private static final int TAB_POS_STORE_PCS  = 6;
	private static final int TAB_POS_STORE_ABS  = 7;
	private static final int TAB_POS_STORE_DIFF = 8;
	private static final int TAB_POS_STORE_RATE = 9;

	private static final Color TAB_PCS_COL_BG = new Color(240, 240, 240);
//	private static final Color TAB_PCS_COL_BG = new Color(243, 243, 243);
//	private static final Color TAB_PCS_COL_BG = new Color(245, 245, 245);

	/*---------------------------------------------------
	** BEGIN: class LocalTableModel
	**---------------------------------------------------
	*/
	/** LocalTableModel */
	private static class LocalTableModel extends DefaultTableModel
	{
		private static final long serialVersionUID = 1L;

		private Vector _changeIndicator = new Vector();  /* is a Vector of "row" Vectors, which contains Booleans */

		LocalTableModel()
		{
			super();
			setColumnIdentifiers(TAB_HEADER);
		}

		
		public void setValueAt(Object value, int row, int column)
		{
			super.setValueAt(value, row, column);

			// hook in to set that a value was changed
			if ( _changeIndicator.size() < getRowCount() )
				_changeIndicator.setSize( getRowCount() );

			// Get the row Vector and check it's size
			Vector changeRowIndicator = (Vector) _changeIndicator.get(row);
			if (changeRowIndicator == null)
			{
				changeRowIndicator = new Vector(getColumnCount());
				_changeIndicator.set(row, changeRowIndicator);
			}
			if (changeRowIndicator.size() < getColumnCount())
				changeRowIndicator.setSize(getColumnCount());
			
			Boolean changed = (Boolean) changeRowIndicator.get(column);
			
			if ( changed == null )
				changeRowIndicator.set(column, new Boolean(true));
			else if ( ! changed.booleanValue() )
				changeRowIndicator.set(column, new Boolean(true));
		}

		public boolean isCellChanged(int row, int col)
		{
			Vector changeRowIndicator = (Vector) _changeIndicator.get(row);
			if (changeRowIndicator == null)
				return false;
			Boolean changed = (Boolean) changeRowIndicator.get(col);
			if (changed == null)
				return false;
			return changed.booleanValue();
		}

		public void resetCellChanges()
		{
			_changeIndicator = new Vector(getRowCount());
			_changeIndicator.setSize(getRowCount());
		}

		public Class getColumnClass(int column)
		{
			if (column == TAB_POS_ICON)     return Icon.class;
			if (column == TAB_POS_POSTPONE) return Integer.class;
			if (column  > TAB_POS_POSTPONE) return Boolean.class;
			return Object.class;
		}

		public boolean isCellEditable(int row, int col)
		{
			if (col == TAB_POS_BG || col > TAB_POS_STORE_PCS)
			{
				// get some values from the MODEL viewRow->modelRow translation should be done before calling isCellEditable
				boolean storePcs    = ((Boolean) getValueAt(row, TAB_POS_STORE_PCS)).booleanValue();
				String tabName      = (String)   getValueAt(row, TAB_POS_TAB_NAME);

				if (_logger.isDebugEnabled())
					_logger.debug("isCellEditable: row="+row+", col="+col+", storePcs="+storePcs+", tabName='"+tabName+"'.");

				// Get CountersModel and check if that model supports editing for Abs, Diff & Rate
				CountersModel cm  = GetCounters.getCmByDisplayName(tabName);
				if (cm != null)
				{
					if (col == TAB_POS_BG)         return cm.isBackgroundDataPollingEditable();

					if (col == TAB_POS_STORE_ABS)  return storePcs && cm.isPersistCountersAbsEditable();
					if (col == TAB_POS_STORE_DIFF) return storePcs && cm.isPersistCountersDiffEditable();
					if (col == TAB_POS_STORE_RATE) return storePcs && cm.isPersistCountersRateEditable();
				}
			}

			return col >= TAB_POS_POSTPONE;
		}
	}
	/*---------------------------------------------------
	** END: class LocalTableModel
	**---------------------------------------------------
	*/


	/*---------------------------------------------------
	** BEGIN: class LocalTable
	**---------------------------------------------------
	*/
	/** Extend the JXTable */
	private class LocalTable extends JXTable
	{
		private static final long serialVersionUID = 0L;
//		protected int           _lastTableHeaderPointX = -1;
		protected int           _lastTableHeaderColumn = -1;
		private   JPopupMenu    _popupMenu             = null;
		private   JPopupMenu    _headerPopupMenu       = null;


		LocalTable()
		{
			super();
			setModel( new LocalTableModel() );

			setShowGrid(false);
			setSortable(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			packAll(); // set size so that all content in all cells are visible
			setColumnControlVisible(true);
//			setHighlighters(_highliters);

			// Create some PopupMenus and attach them
			_popupMenu = createDataTablePopupMenu();
			setComponentPopupMenu(getDataTablePopupMenu());

			_headerPopupMenu = createDataTableHeaderPopupMenu();
			getTableHeader().setComponentPopupMenu(getDataTableHeaderPopupMenu());

			// Populate the table
			refreshTable();
		}

		/** What table header was the last header we visited */
		public int getLastTableHeaderColumn()
		{
			return _lastTableHeaderColumn;
		}

		/** TABLE HEADER tool tip. */
		protected JTableHeader createDefaultTableHeader()
		{
			JTableHeader tabHeader = new JXTableHeader(getColumnModel())
			{
                private static final long serialVersionUID = 0L;

				public String getToolTipText(MouseEvent e)
				{
					String tip = null;
					int col = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (col < 0) return null;

					switch(col)
					{
					case TAB_POS_ICON:       tip = null; break;
					case TAB_POS_TAB_NAME:   tip = "Name of Tab/Collector."; break;
					case TAB_POS_POSTPONE:   tip = "If you want to skip some intermidiate samples, Here you can specify minimum seconds between samples."; break;
					case TAB_POS_PAUSED:     tip = "Pause data polling for this Tab. This makes the values easier to read..."; break;
					case TAB_POS_BG:         tip = "Sample this panel even when this Tab is not active."; break;
					case TAB_POS_RNC20:      tip = "If the differance between 'this' and 'previous' data sample has negative counter values, reset them to be <b>zero</b>"; break;
					case TAB_POS_STORE_PCS:  tip = "Save this Counter Set to a Persistant Storage, even when we are in GUI mode.<br>Note: This is only enabled/available if you specified a Counter Storage when you connected."; break;
					case TAB_POS_STORE_ABS:  tip = "Save the Absolute Counters in the Persistent Counter Storage"; break;
					case TAB_POS_STORE_DIFF: tip = "Save the Difference Counters in the Persistent Counter Storage"; break;
					case TAB_POS_STORE_RATE: tip = "Save the Rate Counters in the Persistent Counter Storage"; break;
					}

					if (tip == null)
						return null;
					return "<html>" + tip + "</html>";
				}
			};

			// Track where we are in the TableHeader, this is used by the Popup menus
			// to decide what column of the TableHeader we are currently located on.
			tabHeader.addMouseMotionListener(new MouseMotionListener()
			{
				public void mouseMoved(MouseEvent e)
				{
					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
				}
				public void mouseDragged(MouseEvent e) {/*ignore*/}
			});

			return tabHeader;
		}

		/** CELL tool tip */
		public String getToolTipText(MouseEvent e)
		{
			String tip = null;
			Point p = e.getPoint();
			int row = super.convertRowIndexToModel( rowAtPoint(p)<0 ? 0 : rowAtPoint(p) );
			int col = super.convertColumnIndexToModel(columnAtPoint(p));

			if (col > TAB_POS_POSTPONE)
			{
				tip = "Right click on the header column to mark or unmark all rows.";
			}
			if (row >= 0)
			{
				//TableModel model = getModel();
			}
			if (tip == null)
				return null;
			return "<html>" + tip + "</html>";
		}

		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
		{
			Component c = super.prepareRenderer(renderer, row, column);
			if (column == TAB_POS_BG)
			{
				c.setEnabled( isCellEditable(row, column) );
			}
			if (column >= TAB_POS_STORE_PCS)
			{
				c.setBackground(TAB_PCS_COL_BG);
				if (column > TAB_POS_STORE_PCS)
				{
					// if not editable, lets disable it
					// calling isCellEditable instead of getModel().isCellEditable(row, column)
					// does the viewRow->modelRow translation for us.
					c.setEnabled( isCellEditable(row, column) );
				}
			}
			return c;
		}

		/** Populate information in the table */
		protected void refreshTable()
		{
			Vector row = new Vector();

			DefaultTableModel tm = (DefaultTableModel)getModel();

			JTabbedPane tabPane = MainFrame.getTabbedPane();
			if (tabPane == null)
				return;

			while (tm.getRowCount() > 0)
				tm.removeRow(0);

			int tabCount = tabPane.getTabCount();
			for (int t=0; t<tabCount; t++)
			{
				Component comp = tabPane.getComponentAt(t);

				if (comp instanceof TabularCntrPanel)
				{
					TabularCntrPanel tcp = (TabularCntrPanel) comp;
					CountersModel    cm  = tcp.getCm();
					if (cm != null)
					{
						row = new Vector();

						row.add(cm.getTabPanel() == null ? null : cm.getTabPanel().getIcon());
						row.add(cm.getDisplayName());   // TAB_POS_TAB_NAME

						row.add(new Integer( cm.getPostponeTime()) );
						row.add(new Boolean( cm.isDataPollingPaused() ));
						row.add(new Boolean( cm.isBackgroundDataPollingEnabled() ));
						row.add(new Boolean( cm.isNegativeDiffCountersToZero() ));

						row.add(new Boolean( cm.isPersistCountersEnabled() ));
						row.add(new Boolean( cm.isPersistCountersAbsEnabled() ));
						row.add(new Boolean( cm.isPersistCountersDiffEnabled() ));
						row.add(new Boolean( cm.isPersistCountersRateEnabled() ));

						tm.addRow(row);
					}
				}
			}
			resetCellChanges();
			packAll(); // set size so that all content in all cells are visible
		}

		public boolean isCellChanged(int row, int col)
		{
			int mrow = super.convertRowIndexToModel(row);
			int mcol = super.convertColumnIndexToModel(col);
			
			LocalTableModel tm = (LocalTableModel)getModel();
			return tm.isCellChanged(mrow, mcol);
		}

		/** typically called from any "apply" button. */
		public void resetCellChanges()
		{
			LocalTableModel tm = (LocalTableModel)getModel();
			tm.resetCellChanges();
			
			// redraw the table
			// Do this so that "check boxes" are pushed via: prepareRenderer()
			repaint();
		}

		
		/*---------------------------------------------------
		** BEGIN: PopupMenu on the table
		**---------------------------------------------------
		*/
		/** Get the JMeny attached to the GTabbedPane */
		public JPopupMenu getDataTablePopupMenu()
		{
			return _popupMenu;
		}

		/**
		 * Creates the JMenu on the Component, this can be overrided by a subclass.<p>
		 * If you want to add stuff to the menu, its better to use
		 * getTabPopupMenu(), then add entries to the menu. This is much
		 * better than subclass the GTabbedPane
		 */
		public JPopupMenu createDataTablePopupMenu()
		{
			_logger.debug("createDataTablePopupMenu(): called.");

			JPopupMenu popup = new JPopupMenu();
			JMenuItem show = new JMenuItem("XXX");

			popup.add(show);

			show.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
//					doActionShow();
				}
			});

			if (popup.getComponentCount() == 0)
			{
				_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
				return null;
			}
			else
				return popup;
		}

		/** Get the JMeny attached to the JTable header */
		public JPopupMenu getDataTableHeaderPopupMenu()
		{
			return _headerPopupMenu;
		}

		public JPopupMenu createDataTableHeaderPopupMenu()
		{
			_logger.debug("createDataTableHeaderPopupMenu(): called.");
			JPopupMenu popup = new JPopupMenu();
			JMenuItem mark   = new JMenuItem("Mark all rows for this column");
			JMenuItem unmark = new JMenuItem("UnMark all rows for this column");

			popup.add(mark);
			popup.add(unmark);

			mark.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					int col = getLastTableHeaderColumn();
					if (col > TAB_POS_POSTPONE)
					{
						TableModel tm = getModel();
						for (int r=0; r<tm.getRowCount(); r++)
						{
							if (tm.isCellEditable(r, col))
								tm.setValueAt(new Boolean(true), r, col);
						}
					}
				}
			});

			unmark.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					int col = getLastTableHeaderColumn();
					if (col > TAB_POS_POSTPONE)
					{
						TableModel tm = getModel();
						for (int r=0; r<tm.getRowCount(); r++)
						{
							if (tm.isCellEditable(r, col))
								tm.setValueAt(new Boolean(false), r, col);
						}
					}
				}
			});

			// add something like:
			// popup.preShow()... so we can enable/disable menu items when we are on specific columns
			//popup.add

			if (popup.getComponentCount() == 0)
			{
				_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
				return null;
			}
			else
				return popup;
		}

		/*---------------------------------------------------
		** END: PopupMenu on the table
		**---------------------------------------------------
		*/
	}

	/*---------------------------------------------------
	** END: class LocalTable
	**---------------------------------------------------
	*/








	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//// MAIN & TEST - MAIN & TEST - MAIN & TEST - MAIN & TEST ///
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////

	public static void main(String[] args)
	{
		Properties log4jProps = new Properties();
		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
		PropertyConfigurator.configure(log4jProps);

		// set native L&F
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
		catch (Exception e) {}


		Configuration conf = new Configuration("c:\\OfflineSessionsViewer.tmp.deleteme.properties");
		Configuration.setInstance(Configuration.TEMP, conf);

		MainFrame frame = new MainFrame();

		// Create and Start the "collector" thread
		GetCounters getCnt = new GetCountersGui();
		try
		{
			getCnt.init();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		getCnt.start();

		frame.pack();

		TcpConfigDialog.showDialog(frame);
	}
}
