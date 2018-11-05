package com.asetune.config.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.DbxTune;
import com.asetune.config.dbms.AseConfig;
import com.asetune.config.dbms.DbmsConfigManager;
//import com.asetune.config.dbms.AseConfig;
import com.asetune.config.dbms.IDbmsConfig;
import com.asetune.gui.SqlTextDialog;
import com.asetune.pcs.PersistReader;
import com.asetune.sql.conn.DbxConnection;
import com.asetune.utils.ConnectionProvider;
import com.asetune.utils.StringUtil;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;


public class DbmsConfigPanel
extends JPanel
implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(DbmsConfigPanel.class);
	
	private JXTable _table = null;

	private static final String SECTION_NOT_CONNECTED = "<NOT CONNECTED>"; 
	private static final String SECTION_ALL           = "<SHOW ALL SECTIONS>"; 

	private JLabel             _timestamp_lbl           = new JLabel("Time");
	private JTextField         _timestamp_txt           = new JTextField(SECTION_NOT_CONNECTED);
	private JLabel             _section_lbl             = new JLabel("Section Name");
	private JComboBox<String>  _section_cbx             = new JComboBox<String>( new String[] {SECTION_NOT_CONNECTED} );
	private JLabel             _config_lbl              = new JLabel("Config Name");
	private JTextField         _config_txt              = new JTextField();
	private JCheckBox          _excludeMonitoring_chk   = new JCheckBox("Exclude values for Monitoring", false);
	private JCheckBox          _showOnlyNonDefaults_chk = new JCheckBox("Show Only Non Default Values", false);
	private JLabel             _rowcount2_lbl           = new JLabel("");
	private JButton            _copy_but                = new JButton("Copy");

	private ConnectionProvider _connProvider    = null;
	private IDbmsConfig        _dbmsConfig      = null;

//	public DbmsConfigPanel(ConnectionProvider connProvider)
//	{
//		_connProvider = connProvider;
//		init();
//	}


	public DbmsConfigPanel(ConnectionProvider connProvider, IDbmsConfig instance)
	{
		_connProvider = connProvider;
		_dbmsConfig   = instance;
		init();
	}


	public void refresh()
	throws SQLException
	{
		Timestamp     ts        = null;
		boolean       hasGui    = DbxTune.hasGui();
		boolean       isOffline = false;
		DbxConnection conn      = null;

//		if (GetCounters.getInstance().isMonConnected())
//		{
//			ts        = null;
//			isOffline = false;
//			conn      = GetCounters.getInstance().getMonConnection();
//		}
//		else
//		{
//			ts        = null; // NOTE: this will not work, get the value from somewhere
//			isOffline = true;
//			conn      = PersistReader.getInstance().getConnection();
//		}

		conn = _connProvider.getConnection();

		if (PersistReader.hasInstance())
		{
			if (PersistReader.getInstance().isConnected())
			{
				ts        = null; // NOTE: this will not work, get the value from somewhere
				isOffline = true;
			}
		}

//		IDbmsConfig aseConfig = AseConfig.getInstance();
////		aseConfig.refresh(conn, ts);
//		aseConfig.initialize(conn, hasGui, isOffline, ts);
		_dbmsConfig.initialize(conn, hasGui, isOffline, ts);
		// 
//		_timestamp_txt.setText(AseConfig.getInstance().getTimestamp()+"");
		_timestamp_txt.setText(_dbmsConfig.getTimestamp()+"");
//		aseConfig.fireTableDataChanged();
	}

	private void init()
	{
		setLayout( new BorderLayout() );
		
//		IDbmsConfig aseConfig = AseConfig.getInstance();
//		if ( ! aseConfig.isInitialized() )
		if ( ! _dbmsConfig.isInitialized() )
		{
			JLabel notConnected = new JLabel("Not yet Initialized, please connect first.");
			notConnected.setFont(new Font(Font.DIALOG, Font.BOLD, SwingUtils.hiDpiScale(16)));
			add(notConnected,  BorderLayout.NORTH);
			
			// FIXME: the createFilterPanel() createTablePanel() components will NOT be visible when doing "refresh"...
			return;
		}

		// Set how many items the "sections" can have before a JScrollBar is visible
		_section_cbx.setMaximumRowCount(50);

		add(createFilterPanel(), BorderLayout.NORTH);
		add(createTablePanel(),  BorderLayout.CENTER);
		
		// Hide/show: Include/exclude Monitoring Checkbox
		boolean isSybaseAse = false;
		if (DbmsConfigManager.hasInstance())
		{
			IDbmsConfig configInstance = DbmsConfigManager.getInstance();
			if (configInstance instanceof AseConfig)
				isSybaseAse = true;
		}
		_excludeMonitoring_chk.setVisible(isSybaseAse);
//		try 
//		{
//			boolean isSybaseAse = DbUtils.isProductName(_connProvider.getConnection().getDatabaseProductName(), DbUtils.DB_PROD_NAME_SYBASE_ASE);
//			_excludeMonitoring_chk.setVisible(isSybaseAse);
//		}
//		catch(SQLException e) 
//		{ 
//			_excludeMonitoring_chk.setVisible(false); 
//		}
	}

	private JPanel createFilterPanel()
	{
		JPanel panel = SwingUtils.createPanel("Filter", true);
		panel.setLayout(new MigLayout());

		// Tooltip
		_timestamp_lbl          .setToolTipText("When was the configuration snapshot taken");
		_timestamp_txt          .setToolTipText("When was the configuration snapshot taken");
		_section_lbl            .setToolTipText("Show only a specific Section");
		_section_cbx            .setToolTipText("Show only a specific Section");
		_config_lbl             .setToolTipText("Show only config name with this name.");
		_config_txt             .setToolTipText("Show only config name with this name.");
		_showOnlyNonDefaults_chk.setToolTipText("Show only modified configuration values (same as sp_configure 'nondefault')");
		_excludeMonitoring_chk  .setToolTipText("Include or Exclude values from sp_configure 'Monitoring'");
		_copy_but               .setToolTipText("Copy the ASE Configuration table into the clip board as ascii table.");

		panel.add(_section_lbl,             "");
		panel.add(_section_cbx,             "split");
//		panel.add(_rowcount2_lbl,           "");
		panel.add(new JLabel(),             "pushx, growx");
		panel.add(_timestamp_lbl,           "");
		panel.add(_timestamp_txt,           "wrap");

		panel.add(_config_lbl,              "");
//		panel.add(_config_txt,              "pushx, growx, wrap");
		panel.add(_config_txt,              "span 2, split, pushx, growx");
		panel.add(_rowcount2_lbl,           "wrap");

		panel.add(_showOnlyNonDefaults_chk, "span 3, split");
		panel.add(_excludeMonitoring_chk,   "");
		panel.add(_copy_but,                "tag right, wrap");

		// disable input to some fields
		_timestamp_txt.setEnabled(false);

		// Add Items
		_section_cbx.removeAllItems();
		_section_cbx.addItem(SECTION_ALL);
//		for (String section : AseConfig.getInstance().getSectionList())
		List<String> sectionList = _dbmsConfig.getSectionList();
		Collections.sort(sectionList);
		for (String section : sectionList)
			_section_cbx.addItem(section);

//		_timestamp_txt.setText(AseConfig.getInstance().getTimestamp()+"");
		_timestamp_txt.setText(_dbmsConfig.getTimestamp()+"");

		// Add action listener
		_section_cbx            .addActionListener(this);
		_config_txt             .addActionListener(this);
		_showOnlyNonDefaults_chk.addActionListener(this);
		_excludeMonitoring_chk  .addActionListener(this);
		_copy_but               .addActionListener(this);

		// Key listener for the config
		_config_txt             .addKeyListener(new KeyListener()
		{
			@Override public void keyTyped(KeyEvent e) {}
			@Override public void keyPressed(KeyEvent e) {}
			@Override public void keyReleased(KeyEvent e) 
			{
				setTableFilter();
			}
		});

		// set auto completion
		AutoCompleteDecorator.decorate(_section_cbx);
		
		// Set initial to: only show non-default
		// to work, so lets the EventThread do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			@Override
			public void run()
			{
				_showOnlyNonDefaults_chk.setSelected(true);
				_excludeMonitoring_chk  .setSelected(_excludeMonitoring_chk.isVisible()); // Otherwise Monitoring values will not be visible when the field is hidden, but the value is TRUE...
				setTableFilter();
			}
		};
		SwingUtilities.invokeLater(deferredAction);

		return panel;
	}

	private JPanel createTablePanel()
	{
		JPanel panel = SwingUtils.createPanel("Actual Data Table", false);
		panel.setLayout(new BorderLayout());

		_table = new LocalTable();
//		_table.setModel(AseConfig.getInstance());
		_table.setModel(_dbmsConfig);

		// Create a rightClickMenuPopup if "Reverse Engineering is Possible"
		if (_dbmsConfig.isReverseEngineeringPossible())
		{
			_table.setComponentPopupMenu(createRightClickTablePopupMenu());
		}

		_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_table.packAll(); // set size so that all content in all cells are
								// visible
		_table.setSortable(true);
		_table.setColumnControlVisible(true);
//		_table.setHighlighters(_table); // a variant of cell render

		//--------------------------------------------------------------------
		// New SORTER that toggles from ASCENDING -> DESCENDING -> UNSORTED
		//--------------------------------------------------------------------
		_table.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);

		// Fixing/setting background selection color... on some platforms it
		// seems to be a strange color
		// on XP a gray color of "r=178,g=180,b=191" is the default, which looks
		// good on the screen
//		Configuration conf = Configuration.getInstance(Configuration.CONF);
//		if ( conf != null )
//		{
//			if ( conf.getBooleanProperty("table.setSelectionBackground", true) )
//			{
//				Color newBg = new Color(conf.getIntProperty("table.setSelectionBackground.r", 178), conf.getIntProperty("table.setSelectionBackground.g", 180), conf.getIntProperty("table.setSelectionBackground.b", 191));
//
//				_logger.debug("table.setSelectionBackground(" + newBg + ").");
//				_table.setSelectionBackground(newBg);
//			}
//		}
//		else
//		{
//			Color bgc = _table.getSelectionBackground();
//			if ( !(bgc.getRed() == 178 && bgc.getGreen() == 180 && bgc.getBlue() == 191) )
//			{
//				Color newBg = new Color(178, 180, 191);
//				_logger.debug("table.setSelectionBackground(" + newBg + "). Config could not be read, trusting defaults...");
//				_table.setSelectionBackground(newBg);
//			}
//		}

		// Mark the row as RED if PENDING CONFIGURATION
		_table.addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String pendingColName = _dbmsConfig.getColName_pending();
				if (StringUtil.isNullOrBlank(pendingColName))
					return false;

				int mindex = adapter.getColumnIndex(pendingColName);
				if (mindex < 0)
					return false;
				
				Object o_pending = adapter.getValue(mindex);
				if ( Boolean.TRUE.equals(o_pending) )
					return true;
				return false;
			}
		}, Color.RED, null));

		
		JScrollPane scroll = new JScrollPane(_table);
//		_watermark = new Watermark(scroll, "Not Connected...");

		// panel.add(scroll, BorderLayout.CENTER);
		// panel.add(scroll, "");
		panel.add(scroll);
		return panel;
	}

	private final static String ACTION_REVERSE_ENGINEER_SELECTED_ROW = "ACTION_REVERSE_ENGINEER_SELECTED_ROW";
	private final static String ACTION_REVERSE_ENGINEER_VISIBLE_ROWS = "ACTION_REVERSE_ENGINEER_VISIBLE_ROWS";

	private JPopupMenu createRightClickTablePopupMenu()
	{
		JPopupMenu popupMenu = new JPopupMenu();
		
		JMenuItem singleRow   = new JMenuItem("Reverse Engineer Selected row");
		JMenuItem visibleRows = new JMenuItem("Reverse Engineer All visible rows in the table");

		singleRow  .addActionListener(this);
		visibleRows.addActionListener(this);

		singleRow  .setActionCommand(ACTION_REVERSE_ENGINEER_SELECTED_ROW);
		visibleRows.setActionCommand(ACTION_REVERSE_ENGINEER_VISIBLE_ROWS);
		
		popupMenu.add(singleRow);
		popupMenu.add(visibleRows);

		return popupMenu;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		String actionCmd = e.getActionCommand();

		// --- COMBOBOX: SECTION ---
		if (_section_cbx.equals(source))
		{
			setTableFilter();
		}

		// --- CHECKBOX: CONFIG NAME ---
		if (_config_txt.equals(source))
		{
			setTableFilter();
		}

		// --- CHECKBOX: NON-DEFAULTS ---
		if (_showOnlyNonDefaults_chk.equals(source))
		{
			setTableFilter();
		}

		// --- CHECKBOX: NON-DEFAULTS ---
		if (_excludeMonitoring_chk.equals(source))
		{
			setTableFilter();
		}

		// --- BUT: COPY ---
		if (_copy_but.equals(source))
		{
			String textTable = SwingUtils.tableToString(_table);

			if (textTable != null)
			{
				StringSelection data = new StringSelection(textTable);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(data, data);
			}
		}

		// --- TABLE RIGHT CLICK MENU: ACTIONS ---
		if (   ACTION_REVERSE_ENGINEER_SELECTED_ROW.equals(actionCmd)
		    || ACTION_REVERSE_ENGINEER_VISIBLE_ROWS.equals(actionCmd) 
		   )
		{
			int[] modelRows = null;

			if (ACTION_REVERSE_ENGINEER_SELECTED_ROW.equals(actionCmd))
			{
				modelRows = new int[1];
				int selectedRow = _table.getSelectedRow();
				if (selectedRow == -1)
				{
					SwingUtils.showInfoMessage(this, "No row selected", "No row was selected, please select a row...");
					return;
				}
				modelRows[0] = _table.convertRowIndexToModel(selectedRow);
			}
			else if (ACTION_REVERSE_ENGINEER_VISIBLE_ROWS.equals(actionCmd))
			{
				int visibleRows = _table.getRowCount();
				if (visibleRows == 0)
				{
					SwingUtils.showInfoMessage(this, "No visible row(s)", "No visible row(s) in the table, please remove some filters");
					return;
				}

				modelRows = new int[visibleRows];
				for (int r=0; r<modelRows.length; r++)
				{
					modelRows[r] = _table.convertRowIndexToModel(r);
				}
			}
			
			String reverseEngineerStr = _dbmsConfig.reverseEngineer(modelRows);
			if (reverseEngineerStr != null && reverseEngineerStr.length() > 0)
			{
//				SqlTextDialog dialog = new SqlTextDialog(this, reverseEngineerStr);
				SqlTextDialog dialog = new SqlTextDialog(null, reverseEngineerStr);
				dialog.setVisible(true);
			}
			else
			{
				SwingUtils.showInfoMessage(this, "No output", "Sorry no DDL/SQL was generated.");
			}

			System.out.println("SHOW THIS IN A SMALL EDITOR='"+reverseEngineerStr+"'.");
		}
	}


	private void setTableFilter()
	{
		ArrayList<RowFilter<TableModel, Integer>> filters = new ArrayList<RowFilter<TableModel, Integer>>();

		// SECTION
		if ( _section_cbx.getSelectedIndex() != 0 )
		{
//			String colName     = AseConfig.SECTION_NAME; 
//			final int colIndex = AseConfig.getInstance().findColumn(colName);
			String colName     = _dbmsConfig.getColName_sectionName(); 
			final int colIndex = _dbmsConfig.findColumn(colName);
			final String str = _section_cbx.getSelectedItem() + "";

			if (colIndex < 0)
				_logger.warn("Column name '"+colName+"' can't be found in AseConfig table.");
			else
			{
				RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>()
				{
					@Override
					public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry)
					{
						return entry.getStringValue(colIndex).equals(str);
					}
				};
				filters.add(filter);
			}
		}

		// CONFIG NAME
		if ( ! _config_txt.getText().trim().equals("") )
		{
//			String colName     = AseConfig.CONFIG_NAME; 
//			final int colIndex = AseConfig.getInstance().findColumn(colName);
			String colName     = _dbmsConfig.getColName_configName(); 
			final int colIndex = _dbmsConfig.findColumn(colName);
			final String str = _config_txt.getText().trim();

			if (colIndex < 0)
				_logger.warn("Column name '"+colName+"' can't be found in AseConfig table.");
			else
			{
				RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>()
				{
					@Override
					public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry)
					{
						return entry.getStringValue(colIndex).indexOf(str) >= 0;
					}
				};
				filters.add(filter);
			}
		}

		// NON-DEFAULTS
		if ( _showOnlyNonDefaults_chk.isSelected() )
		{
//			String colName     = AseConfig.NON_DEFAULT; 
//			final int colIndex = AseConfig.getInstance().findColumn(colName);
			String colName     = _dbmsConfig.getColName_nonDefault(); 
			final int colIndex = _dbmsConfig.findColumn(colName);

			if (colIndex < 0)
				_logger.warn("Column name '"+colName+"' can't be found in AseConfig table.");
			else
			{
				RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>()
				{
					@Override
					public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry)
					{
						Object o = entry.getValue(colIndex);
						if (o instanceof Boolean)
						{
							boolean isNonDefault = ((Boolean)o).booleanValue();
							return isNonDefault;
						}
						return true;
					}
				};
				filters.add(filter);
			}
		}

		// Exclude-Monitoring
		if ( _excludeMonitoring_chk.isSelected() )
		{
			String colName     = _dbmsConfig.getColName_sectionName(); 
			final int colIndex = _dbmsConfig.findColumn(colName);
			final String str   = "Monitoring";

			if (colIndex < 0)
				_logger.warn("Column name '"+colName+"' can't be found in AseConfig table.");
			else
			{
				RowFilter<TableModel, Integer> filter = new RowFilter<TableModel, Integer>()
				{
					@Override
					public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry)
					{
						return ! entry.getStringValue(colIndex).equals(str);
					}
				};
				filters.add(filter);
			}
		}

		// Now SET filter
		if (filters.size() == 0)
			_table.setRowFilter( null );
		else
			_table.setRowFilter( RowFilter.andFilter(filters) );
		
		// Update the row count label
		_rowcount2_lbl.setText(_table.getModel().getRowCount() + "/" + _table.getRowCount());
	}

	
	private class LocalTable
	extends JXTable
	{
		private static final long serialVersionUID = 1L;

		// 
		// TOOL TIP for: TABLE HEADERS
		//
		@Override
		protected JTableHeader createDefaultTableHeader()
		{
			return new JXTableHeader(getColumnModel())
			{
				private static final long serialVersionUID = 1L;

				@Override
				public String getToolTipText(MouseEvent e)
				{
					// Now get the column name, which we point at
					Point p = e.getPoint();
					int index = getColumnModel().getColumnIndexAtX(p.x);
					if ( index < 0 )
						return null;
					Object colNameObj = getColumnModel().getColumn(index).getHeaderValue();

					String toolTip = null;
					if ( colNameObj instanceof String )
					{
						String colName = (String) colNameObj;
//						toolTip = AseConfig.getInstance().getColumnToolTip(colName);
						toolTip = _dbmsConfig.getColumnToolTip(colName);
					}
					return toolTip;
				}
			};
		}

		// 
		// TOOL TIP for: CELLS
		//
		@Override
		public String getToolTipText(MouseEvent e)
		{
			String tip = null;
			Point p = e.getPoint();
			int row = rowAtPoint(p);
			int col = columnAtPoint(p);
			if ( row >= 0 && col >= 0 )
			{
				int mcol = super.convertColumnIndexToModel(col);
				int mrow = super.convertRowIndexToModel(row);

				tip = _dbmsConfig.getCellToolTip(mrow, mcol);

//				TableModel tm = getModel();
//				if (tm instanceof AbstractTableModel)
//				{
//					AbstractTableModel atm = (AbstractTableModel) tm;
//
//					//String colName = tm.getColumnName(col);
//					//Object cellValue = tm.getValueAt(row, col);
//					//tip = "colName='"+colName+"', cellValue='"+cellValue+"'.";
//
//					StringBuilder sb = new StringBuilder();
//					sb.append("<html>");
//					sb.append("<b>Description:  </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.DESCRIPTION))) .append("<br>");
//					sb.append("<b>Section Name: </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.SECTION_NAME))).append("<br>");
//					sb.append("<b>Config Name:  </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.CONFIG_NAME))) .append("<br>");
//					sb.append("<b>Run Value:    </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.CONFIG_VALUE))).append("<br>");
//					sb.append("<b>Max Value:    </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.MAX_VALUE)))   .append("<br>");
//					sb.append("<b>Min Value:    </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.MIN_VALUE)))   .append("<br>");
//					if (Boolean.TRUE.equals(atm.getValueAt(row, atm.findColumn(AseConfig.PENDING))))
//					{
//						sb.append("<br>");
//						sb.append("<b>NOTE: DBMS Needs to be rebooted for this option to take effect.</b>").append("<br>");
//					}
//					sb.append("</html>");
//					tip = sb.toString();
//				}
			}
			if ( tip != null )
				return tip;
			return getToolTipText();
		}
	}

}
