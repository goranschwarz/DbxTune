package com.asetune.gui;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.sort.RowFilters;
import org.jdesktop.swingx.table.TableColumnExt;

import com.asetune.Version;
import com.asetune.utils.Configuration;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class JdbcOptionsDialog
extends JDialog
implements ActionListener, TableModelListener
{
	private static Logger _logger = Logger.getLogger(JdbcOptionsDialog.class);
	private static final long	serialVersionUID	= 1L;

//	private Frame                  _owner           = null;

	private LocalTable             _table           = null;


	// PANEL: OK-CANCEL
	private JButton                _ok              = new JButton("OK");
	private JButton                _cancel          = new JButton("Cancel");
//	private JButton                _apply           = new JButton("Apply");

	private String                 _driverClassName = null;
	private String                 _urlTemplate     = null;
	private Map<String,String>     _inValues        = null;
	private Map<String,String>     _outValues       = null;

	private JLabel                 _optionsFilter_lbl = new JLabel("Filter");
	private JTextField             _optionsFilter_txt = new JTextField();
	
	private static final String DIALOG_TITLE = "Options for the JDBC Driver";

	/*---------------------------------------------------
	** BEGIN: constructors
	**---------------------------------------------------
	*/
	private JdbcOptionsDialog(Dialog owner, String driverClassName, String urlTemplate, Map<String,String> inValues)
	{
		super(owner, DIALOG_TITLE + " '"+driverClassName+"'", true);
		_driverClassName = driverClassName;
		_urlTemplate     = urlTemplate;
		_inValues        = inValues;
		
		if (_inValues == null)
			_inValues = new HashMap<String,String>(); // create an empty on 

//		_owner           = owner;

		initComponents();
	}

	public static Map<String,String> showDialog(Dialog owner, String driverClassName, String urlTemplate, Map<String,String> inValues)
	{
		JdbcOptionsDialog options = new JdbcOptionsDialog(owner, driverClassName, urlTemplate, inValues);
		options.setLocationRelativeTo(owner);
		options.setVisible(true);
		options.dispose();
		
		return options._outValues;
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

		panel.add(createTopPanel(),      "growx, pushx");
		panel.add(createTablePanel(),    "grow, push, height 100%");
		panel.add(createOkCancelPanel(), "bottom, right, pushx");

		loadProps();

		setContentPane(panel);

		initComponentActions();
	}

	private JPanel createTopPanel()
	{
		JPanel panel = SwingUtils.createPanel("Filter", false);
		panel.setLayout(new MigLayout("","",""));

		panel.add(_optionsFilter_lbl, "");
		panel.add(_optionsFilter_txt, "pushx, growx");

		_optionsFilter_txt.addActionListener(this);
		_optionsFilter_txt.addKeyListener(new KeyListener()
		{
			@Override
			public void keyTyped(KeyEvent e)
			{
				applyOptionFilter();
			}
			
			@Override public void keyReleased(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
		});
		
		return panel;
	}

	private JPanel createOkCancelPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("","",""));   // insets Top Left Bottom Right

		// ADD the OK, Cancel, Apply buttons
		panel.add(_ok,     "tag ok, right");
		panel.add(_cancel, "tag cancel");
//		panel.add(_apply,  "tag apply");

//		_apply.setEnabled(false);

		// ADD ACTIONS TO COMPONENTS
		_ok           .addActionListener(this);
		_cancel       .addActionListener(this);
//		_apply        .addActionListener(this);

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
			@Override
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
	@Override
	public void actionPerformed(ActionEvent e)
    {
		Object source = e.getSource();

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_outValues = null; // CANCEL
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
//		if (_apply.equals(source))
//		{
//			doApply();
//			saveProps();
//		}

		// --- FILTER ---
		if (_optionsFilter_txt.equals(source))
		{
			applyOptionFilter();
		}
    }

	private void applyOptionFilter()
	{
        String searchString = _optionsFilter_txt.getText().trim();
        if ( searchString.length() > 0 ) 
        	_table.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString));
        else 
        	_table.setRowFilter(null);
	}
	@Override
	public void tableChanged(TableModelEvent e)
	{
		//System.out.println("tableChanged(): TableModelEvent="+e);
		
		if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == TAB_POS_OPT_VALUE)
			if (e.getFirstRow() != TableModelEvent.HEADER_ROW)
				_table.getModel().setValueAt(new Boolean(true), e.getFirstRow(), TAB_POS_USE);

//		_apply.setEnabled(true);
	}


	/*---------------------------------------------------
	** END: Action Listeners
	**---------------------------------------------------
	*/

	private void doApply()
	{
		_outValues = new LinkedHashMap<String,String>();

		TableModel tm = _table.getModel();
		for (int r=0; r<tm.getRowCount(); r++)
		{
			boolean add = ((Boolean) tm.getValueAt(r, TAB_POS_USE)).booleanValue();
			if (add)
			{
				String  key = (String)  tm.getValueAt(r, TAB_POS_OPT_NAME);
				String  val = (String)  tm.getValueAt(r, TAB_POS_OPT_VALUE);

				//System.out.println("KEY='"+key+"', VALUE='"+val+"'.");
				Object oldVal = _outValues.put(key, val);
				if (oldVal != null)
					_logger.warn("Found an already existing value for the key '"+key+"'. The existing value '"+oldVal+"' is replaced with the new value '"+val+"'.");
			}
		}

		_table.resetCellChanges();
//		_apply.setEnabled(false);
	}


	/*---------------------------------------------------
	** BEGIN: Property handling
	**---------------------------------------------------
	*/
	private void saveProps()
  	{
		Configuration tmpConf = Configuration.getInstance(Configuration.USER_TEMP);
		String base = "jdbcOptionsDialog.";

		if (tmpConf != null)
		{
			tmpConf.setLayoutProperty(base + "window.width", this.getSize().width);
			tmpConf.setLayoutProperty(base + "window.height", this.getSize().height);
			tmpConf.setLayoutProperty(base + "window.pos.x", this.getLocationOnScreen().x);
			tmpConf.setLayoutProperty(base + "window.pos.y", this.getLocationOnScreen().y);

			tmpConf.save();
		}
  	}

  	private void loadProps()
  	{
		int     width     = SwingUtils.hiDpiScale(1024);  // initial window with   if not opened before
		int     height    = SwingUtils.hiDpiScale(630);   // initial window height if not opened before
		int     x         = -1;
		int     y         = -1;

//		Configuration tmpConf = Configuration.getInstance(Configuration.TEMP);
		Configuration tmpConf = Configuration.getCombinedConfiguration();
		String base = "jdbcOptionsDialog.";

		setSize(width, height);

		if (tmpConf == null)
			return;

		width  = tmpConf.getLayoutProperty(base + "window.width",  width);
		height = tmpConf.getLayoutProperty(base + "window.height", height);
		x      = tmpConf.getLayoutProperty(base + "window.pos.x",  -1);
		y      = tmpConf.getLayoutProperty(base + "window.pos.y",  -1);

		if (width != -1 && height != -1)
		{
			setSize(width, height);
		}
		if (x != -1 && y != -1)
		{
			if ( ! SwingUtils.isOutOfScreen(x, y, width, height) )
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

	private static final String[] TAB_HEADER = {"Use", "Non System", "Required", "Option Name", "Value", "Description"};
	private static final int TAB_POS_USE           = 0;
	private static final int TAB_POS_USER_DEFINED  = 1;
	private static final int TAB_POS_OPT_REQUIRED  = 2;
	private static final int TAB_POS_OPT_NAME      = 3;
	private static final int TAB_POS_OPT_VALUE     = 4;
	private static final int TAB_POS_OPT_DESC      = 5;

//	private static final Color TAB_XXX_COL_BG = new Color(240, 240, 240);

	/*---------------------------------------------------
	** BEGIN: class LocalTableModel
	**---------------------------------------------------
	*/
	/** LocalTableModel */
	private static class LocalTableModel extends DefaultTableModel
	{
		private static final long serialVersionUID = 1L;

		private Vector<Vector<Boolean>> _changeIndicator = new Vector<Vector<Boolean>>();  /* is a Vector of "row" Vectors, which contains Booleans */

		LocalTableModel()
		{
			super();
			setColumnIdentifiers(TAB_HEADER);
		}

		
		@Override
		public void setValueAt(Object value, int row, int column)
		{
			super.setValueAt(value, row, column);

			// hook in to set that a value was changed
			if ( _changeIndicator.size() < getRowCount() )
				_changeIndicator.setSize( getRowCount() );

			// Get the row Vector and check it's size
			Vector<Boolean> changeRowIndicator = _changeIndicator.get(row);
			if (changeRowIndicator == null)
			{
				changeRowIndicator = new Vector<Boolean>(getColumnCount());
				_changeIndicator.set(row, changeRowIndicator);
			}
			if (changeRowIndicator.size() < getColumnCount())
				changeRowIndicator.setSize(getColumnCount());
			
			Boolean changed = changeRowIndicator.get(column);
			
			if ( changed == null )
				changeRowIndicator.set(column, new Boolean(true));
			else if ( ! changed.booleanValue() )
				changeRowIndicator.set(column, new Boolean(true));
		}

		public boolean isCellChanged(int row, int col)
		{
			Vector<Boolean> changeRowIndicator = _changeIndicator.get(row);
			if (changeRowIndicator == null)
				return false;
			Boolean changed = changeRowIndicator.get(col);
			if (changed == null)
				return false;
			return changed.booleanValue();
		}

		public void resetCellChanges()
		{
			_changeIndicator = new Vector<Vector<Boolean>>(getRowCount());
			_changeIndicator.setSize(getRowCount());
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			switch (column)
			{
			case TAB_POS_USE:          return Boolean.class;
			case TAB_POS_USER_DEFINED: return Boolean.class;
			case TAB_POS_OPT_REQUIRED: return Boolean.class;
			case TAB_POS_OPT_NAME:     return String.class;
			case TAB_POS_OPT_VALUE:    return String.class;
			case TAB_POS_OPT_DESC:     return String.class;
			}
			return Object.class;
		}

		@Override
		public boolean isCellEditable(int row, int col)
		{
			switch (col)
			{
			case TAB_POS_USE:          return true;
			case TAB_POS_USER_DEFINED: return false;
			case TAB_POS_OPT_REQUIRED: return false;
			case TAB_POS_OPT_NAME:     return false;
			case TAB_POS_OPT_VALUE:    return true;
			case TAB_POS_OPT_DESC:     return false;
			}
			return false;
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
		@SuppressWarnings("unused")
		public int getLastTableHeaderColumn()
		{
			return _lastTableHeaderColumn;
		}

		/** TABLE HEADER tool tip. */
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			JTableHeader tabHeader = new JXTableHeader(getColumnModel())
			{
                private static final long serialVersionUID = 0L;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					String tip = null;
					int col = getColumnModel().getColumnIndexAtX(e.getPoint().x);
					if (col < 0) return null;

					switch (col)
					{
					case TAB_POS_USE:          tip = "Check the box if you want to select this option."; break;
					case TAB_POS_USER_DEFINED: tip = "Indicates that this is a input parameter, which is't recognized by the JDBC Driver."; break;
					case TAB_POS_OPT_REQUIRED: tip = "Is this option required when you do connect."; break;
					case TAB_POS_OPT_NAME:     tip = "Name of the JDBC option from the driver."; break;
					case TAB_POS_OPT_VALUE:    tip = "Value of this option."; break;
					case TAB_POS_OPT_DESC:     tip = "The Desction from the JDBC Driver"; break;
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
				@Override
				public void mouseMoved(MouseEvent e)
				{
					_lastTableHeaderColumn = getColumnModel().getColumnIndexAtX(e.getX());
				}
				@Override
				public void mouseDragged(MouseEvent e) {/*ignore*/}
			});

			return tabHeader;
		}

		/** CELL tool tip */
		@Override
		public String getToolTipText(MouseEvent e)
		{
			String tip = null;
			Point p = e.getPoint();
			int row = super.convertRowIndexToModel( rowAtPoint(p)<0 ? 0 : rowAtPoint(p) );
			int col = super.convertColumnIndexToModel(columnAtPoint(p));

			if (col > 0)
			{
				//tip = "Right click on the header column to mark or unmark all rows.";
			}
			if (row >= 0)
			{
				//TableModel model = getModel();
			}
			if (col >= 0 && row >= 0)
			{
				tip = (String) super.getModel().getValueAt(row, TAB_POS_OPT_NAME) + ": " +
				      (String) super.getModel().getValueAt(row, TAB_POS_OPT_DESC);
			}
			if (tip == null)
				return null;
			return "<html>" + tip + "</html>";
		}

		/** Enable/Disable + add some color to pcsStore, Abs, Diff, Rate */
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
		{
			Component c = super.prepareRenderer(renderer, row, column);
//			if (column == TAB_POS_BG)
//			{
//				c.setEnabled( isCellEditable(row, column) );
//			}
//			if (column >= TAB_POS_STORE_PCS)
//			{
//				c.setBackground(TAB_PCS_COL_BG);
//				if (column > TAB_POS_STORE_PCS)
//				{
//					// if not editable, lets disable it
//					// calling isCellEditable instead of getModel().isCellEditable(row, column)
//					// does the viewRow->modelRow translation for us.
//					c.setEnabled( isCellEditable(row, column) );
//				}
//			}
			return c;
		}

		/** Populate information in the table */
		protected void refreshTable()
		{
			Vector<Object> row = new Vector<Object>();

			DefaultTableModel tm = (DefaultTableModel)getModel();

			Driver driver = null;
			DriverPropertyInfo[] attributes = null;

			try
			{
				Class.forName(_driverClassName);
		
				Properties info = new Properties();
				driver = DriverManager.getDriver(_urlTemplate);
		
				attributes = driver.getPropertyInfo(_urlTemplate, info);
			}
			catch (Exception e)
			{
				String extraInfo = "";
				if (e.getMessage().indexOf("No suitable driver") >= 0)
				{
					extraInfo += "<br>";
					extraInfo += "<hr>";
					extraInfo += "<b>Tip:</b> No suitable driver<br>";
					extraInfo += "The selected Driver cannot handle the specified Database URL. <br>";
					extraInfo += "The most common reason for this error is that the database <b>URL contains a syntax error</b> preventing the driver from accepting it. <br>";
					extraInfo += "The error also occurs when trying to connect to a database with the wrong driver. Correct this and try again.";
					
				}
				
				SwingUtils.showErrorMessage(JdbcOptionsDialog.this, "Problems getting Connection Properties", 
						"<html>" +
						"<h2>Some problem when getting Connection Properties</h2>" +
						"<b>Driver</b>: " + _driverClassName + "<br>" +
						"<b>URL</b>: " + _urlTemplate + "<br>" +
						"<br>" +
						"<b>Message</b>: " + e.getMessage() + "<br>" +
						extraInfo +
						"</html>",
						e);
				_logger.warn("Problems getting Connection Properties for driver='"+_driverClassName+"', URL='"+_urlTemplate+"'.", e);
//				e.printStackTrace();
				return;
			}
			
			_logger.debug("Resolving properties for: " + driver.getClass().getName());

			// Take a copy of the input, this so we can remove records
			Map<String,String> inValuesCopy = new LinkedHashMap<String,String>(_inValues);

			// LOOP ALL THE ATTRIBUTES AND ADD IT TO THE TABLE
			boolean addedAnyRequired = false;
			for (int i = 0; i < attributes.length; i++)
			{
				// get the property metadata
				String   name        = attributes[i].name;
				String[] choicesArr  = attributes[i].choices;
				boolean  required    = attributes[i].required;
				String   description = attributes[i].description;
				String   value       = attributes[i].value;

				String choises = "";
				if (choicesArr != null && choicesArr.length > 0)
				{
					choises = "";
					for (int j = 0; j < choicesArr.length; j++)
						choises += "<"+ choicesArr[j] + ">, ";
					if (choises.endsWith(", "))
						choises = choises.substring(0, choises.length()-2);
				}

				
				// printout property metadata
				if (_logger.isDebugEnabled())
				{
					_logger.debug("\n-----------------------------------------------------------");
					_logger.debug(" Name:        " + name);
					_logger.debug(" Required:    " + required);
					_logger.debug(" Value:       " + value);
					_logger.debug(" Choices are: " + choises);
					_logger.debug(" Description: " + description);
				}

				//---------------------------------------
				// Adjust some stuff
				//---------------------------------------
				if (value == null)
					value = "";
				value = value.trim();

				// if string is to long make it shorter...
				if (value.length() > 80 && value.indexOf("\n", 20) != -1)
				{
					value = value.substring(0, value.indexOf("\n", 20));
				}
				
				// Skip username and password
				if (name.equalsIgnoreCase("USER"))     continue;
				if (name.equalsIgnoreCase("PASSWORD")) continue;

				// if value is 'null', make it an empty string
				if (value.equalsIgnoreCase("null") )
					value = "";

				// if appname is not set, set it to something.
				if (name.equalsIgnoreCase("APPLICATIONNAME"))
				{
					if ( value.equals("") )
						value = Version.getAppName();
				}

				if (required)
					addedAnyRequired = true;
				
				row = new Vector<Object>();
				row.setSize(TAB_HEADER.length);

				row.set(TAB_POS_USE,          new Boolean(false));
				row.set(TAB_POS_USER_DEFINED, new Boolean(false));
				row.set(TAB_POS_OPT_REQUIRED, new Boolean(required));
				row.set(TAB_POS_OPT_NAME,     name);
				row.set(TAB_POS_OPT_VALUE,    value + choises);
				row.set(TAB_POS_OPT_DESC,     description);

				if (inValuesCopy.containsKey(name))
				{
					String inStr = (String) inValuesCopy.get(name);
					row.set(TAB_POS_USE,       new Boolean(true));
					row.set(TAB_POS_OPT_VALUE, inStr);
					
					inValuesCopy.remove(name);
				}
				
				tm.addRow(row);
			}

			// ADD rows from the input map
			int insertPos = 0;
			for (Map.Entry<String,String> entry : inValuesCopy.entrySet()) 
			{
				String key = entry.getKey();
				String val = entry.getValue();
				
				row = new Vector<Object>();
				row.setSize(TAB_HEADER.length);

				row.set(TAB_POS_USE,          new Boolean(true));
				row.set(TAB_POS_USER_DEFINED, new Boolean(true));
				row.set(TAB_POS_OPT_REQUIRED, new Boolean(false));
				row.set(TAB_POS_OPT_NAME,     key);
				row.set(TAB_POS_OPT_VALUE,    val);
				row.set(TAB_POS_OPT_DESC,     "");

				tm.insertRow(insertPos++, row);
			}

			resetCellChanges();
			packAll(); // set size so that all content in all cells are visible

			// Hide the REQUIRED column if none is true.
			TableColumnExt tcx = getColumnExt(TAB_POS_OPT_REQUIRED);
			if (tcx != null)
				tcx.setVisible(addedAnyRequired);
		}

		@SuppressWarnings("unused")
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
			return null;
//			_logger.debug("createDataTablePopupMenu(): called.");
//
//			JPopupMenu popup = new JPopupMenu();
//			JMenuItem show = new JMenuItem("XXX");
//
//			popup.add(show);
//
//			show.addActionListener(new ActionListener()
//			{
//				public void actionPerformed(ActionEvent e)
//				{
////					doActionShow();
//				}
//			});
//
//			if (popup.getComponentCount() == 0)
//			{
//				_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
//				return null;
//			}
//			else
//				return popup;
		}

		/** Get the JMeny attached to the JTable header */
		public JPopupMenu getDataTableHeaderPopupMenu()
		{
			return _headerPopupMenu;
		}

		public JPopupMenu createDataTableHeaderPopupMenu()
		{
			return null;
//			_logger.debug("createDataTableHeaderPopupMenu(): called.");
//			JPopupMenu popup = new JPopupMenu();
//			JMenuItem mark   = new JMenuItem("Mark all rows for this column");
//			JMenuItem unmark = new JMenuItem("UnMark all rows for this column");
//
//			popup.add(mark);
//			popup.add(unmark);
//
//			mark.addActionListener(new ActionListener()
//			{
//				public void actionPerformed(ActionEvent e)
//				{
//					int col = getLastTableHeaderColumn();
////					if (col > TAB_POS_POSTPONE)
////					{
////						TableModel tm = getModel();
////						for (int r=0; r<tm.getRowCount(); r++)
////						{
////							if (tm.isCellEditable(r, col))
////								tm.setValueAt(new Boolean(true), r, col);
////						}
////					}
//				}
//			});
//
//			unmark.addActionListener(new ActionListener()
//			{
//				public void actionPerformed(ActionEvent e)
//				{
//					int col = getLastTableHeaderColumn();
////					if (col > TAB_POS_POSTPONE)
////					{
////						TableModel tm = getModel();
////						for (int r=0; r<tm.getRowCount(); r++)
////						{
////							if (tm.isCellEditable(r, col))
////								tm.setValueAt(new Boolean(false), r, col);
////						}
////					}
//				}
//			});
//
//			// add something like:
//			// popup.preShow()... so we can enable/disable menu items when we are on specific columns
//			//popup.add
//
//			if (popup.getComponentCount() == 0)
//			{
//				_logger.warn("No PopupMenu has been assigned for the data table in the panel.");
//				return null;
//			}
//			else
//				return popup;
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

//	public static void main(String[] args)
//	{
//		Properties log4jProps = new Properties();
//		log4jProps.setProperty("log4j.rootLogger", "INFO, A1");
//		//log4jProps.setProperty("log4j.rootLogger", "TRACE, A1");
//		log4jProps.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
//		log4jProps.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
//		log4jProps.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d - %-5p - %-30c{1} - %m%n");
//		PropertyConfigurator.configure(log4jProps);
//
//		// set native L&F
//		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
//		catch (Exception e) {}
//
//
//		Configuration conf = new Configuration("c:\\JdbcOptionsDialog.tmp.deleteme.properties");
//		Configuration.setInstance(Configuration.USER_TEMP, conf);
//
//		MainFrame frame = new MainFrameAse();
//
//		// Create and Start the "collector" thread
//		GetCounters getCnt = new GetCountersGui();
//		try
//		{
//			getCnt.init();
//		}
//		catch (Exception e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////		getCnt.start();
//
//		frame.pack();
//
//		Map<String,String> input = new HashMap<String,String>();
//		input.put("KEY", "VAL");
//		input.put("APPLICATIONNAME", "JdbcOptionDialog-test");
//
//		System.out.println("IN: "+input);
//		Map<String,String> output = JdbcOptionsDialog.showDialog(null, AseConnectionFactory.getDriver(), AseConnectionFactory.getUrlTemplate(), input);
//		System.out.println("OUT: "+output);
//	}
}
