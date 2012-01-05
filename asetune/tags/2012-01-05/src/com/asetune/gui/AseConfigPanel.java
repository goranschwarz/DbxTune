package com.asetune.gui;

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
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.JXTableHeader;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;

import com.asetune.AseConfig;
import com.asetune.utils.SwingUtils;


public class AseConfigPanel
extends JPanel
implements ActionListener
{
	private static final long serialVersionUID = 1L;
	private static Logger _logger = Logger.getLogger(AseConfigPanel.class);
	
	private JXTable _table = null;

	private static final String SECTION_NOT_CONNECTED = "<NOT CONNECTED>"; 
	private static final String SECTION_ALL           = "<SHOW ALL SECTIONS>"; 

	private JLabel     _timestamp_lbl           = new JLabel("Time");
	private JTextField _timestamp_txt           = new JTextField(SECTION_NOT_CONNECTED);
	private JLabel     _section_lbl             = new JLabel("Section Name");
	private JComboBox  _section_cbx             = new JComboBox( new String[] {SECTION_NOT_CONNECTED} );
	private JLabel     _config_lbl              = new JLabel("Config Name");
	private JTextField _config_txt              = new JTextField();
	private JCheckBox  _showOnlyNonDefaults_chk = new JCheckBox("Show Only Non Default Values", false);
	private JButton    _copy_but                = new JButton("Copy");

	public AseConfigPanel()
	{
		init();
	}


	private void init()
	{
		setLayout( new BorderLayout() );
		
		AseConfig aseConfig = AseConfig.getInstance();
		if ( ! aseConfig.isInitialized() )
		{
			JLabel notConnected = new JLabel("Not yet Initialized, please connect first.");
			notConnected.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
			add(notConnected,  BorderLayout.NORTH);
			return;
		}

		add(createFilterPanel(), BorderLayout.NORTH);
		add(createTablePanel(),  BorderLayout.CENTER);
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
		_copy_but               .setToolTipText("Copy the ASE Configuration table into the clip board as ascii table.");

		panel.add(_section_lbl,             "");
		panel.add(_section_cbx,             "");

		panel.add(_timestamp_lbl,           "split 2, right, push");
		panel.add(_timestamp_txt,           "wrap");

		panel.add(_config_lbl,              "");
		panel.add(_config_txt,              "span, push, grow, wrap");

		panel.add(_showOnlyNonDefaults_chk, "span, split, push, grow");
		panel.add(_copy_but,                "wrap");

		// disable input to some fields
		_timestamp_txt.setEnabled(false);

		// Add Items
		_section_cbx.removeAllItems();
		_section_cbx.addItem(SECTION_ALL);
		for (String section : AseConfig.getInstance().getSectionList())
			_section_cbx.addItem(section);

		_timestamp_txt.setText(AseConfig.getInstance().getTimestamp()+"");

		// Add action listener
		_section_cbx            .addActionListener(this);
		_config_txt             .addActionListener(this);
		_showOnlyNonDefaults_chk.addActionListener(this);
		_copy_but               .addActionListener(this);

		// set auto completion
		AutoCompleteDecorator.decorate(_section_cbx);
		
		// Set initial to: only show non-default
		// to work, so lets the EventThread do it for us after the windows is visible.
		Runnable deferredAction = new Runnable()
		{
			public void run()
			{
				_showOnlyNonDefaults_chk.setSelected(true);
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
		_table.setModel(AseConfig.getInstance());

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
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Object o_pending = adapter.getValue(adapter.getColumnIndex(AseConfig.PENDING));
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


	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

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
	}


	private void setTableFilter()
	{
		ArrayList<RowFilter<TableModel, Integer>> filters = new ArrayList<RowFilter<TableModel, Integer>>();

		// SECTION
		if ( _section_cbx.getSelectedIndex() != 0 )
		{
			String colName     = AseConfig.SECTION_NAME; 
			final int colIndex = AseConfig.getInstance().findColumn(colName);
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
			String colName     = AseConfig.CONFIG_NAME; 
			final int colIndex = AseConfig.getInstance().findColumn(colName);
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
			String colName     = AseConfig.NON_DEFAULT; 
			final int colIndex = AseConfig.getInstance().findColumn(colName);

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

		// Now SET filter
		if (filters.size() == 0)
			_table.setRowFilter( null );
		else
			_table.setRowFilter( RowFilter.andFilter(filters) );
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
						toolTip = AseConfig.getInstance().getColumnToolTip(colName);
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
				col = super.convertColumnIndexToModel(col);
				row = super.convertRowIndexToModel(row);

				TableModel tm = getModel();
				AbstractTableModel atm = null;
				if (tm instanceof AbstractTableModel)
				{
					atm = (AbstractTableModel) tm;

					//String colName = tm.getColumnName(col);
					//Object cellValue = tm.getValueAt(row, col);
					//tip = "colName='"+colName+"', cellValue='"+cellValue+"'.";

					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					sb.append("<b>Description:  </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.DESCRIPTION))) .append("<br>");
					sb.append("<b>Section Name: </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.SECTION_NAME))).append("<br>");
					sb.append("<b>Config Name:  </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.CONFIG_NAME))) .append("<br>");
					sb.append("<b>Run Value:    </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.CONFIG_VALUE))).append("<br>");
					sb.append("<b>Max Value:    </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.MAX_VALUE)))   .append("<br>");
					sb.append("<b>Min Value:    </b>").append(atm.getValueAt(row, atm.findColumn(AseConfig.MIN_VALUE)))   .append("<br>");
					if (Boolean.TRUE.equals(atm.getValueAt(row, atm.findColumn(AseConfig.PENDING))))
					{
						sb.append("<br>");
						sb.append("<b>NOTE: ASE Needs to be rebooted for this option to take effect.</b>").append("<br>");
					}
					sb.append("</html>");
					tip = sb.toString();
				}
			}
			if ( tip != null )
				return tip;
			return getToolTipText();
		}
	}

}
