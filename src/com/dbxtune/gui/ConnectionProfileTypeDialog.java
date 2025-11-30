/*******************************************************************************
 * Copyright (C) 2010-2025 Goran Schwarz
 * 
 * This file is part of DbxTune
 * DbxTune is a family of sub-products *Tune, hence the Dbx
 * Here are some of the tools: AseTune, IqTune, RsTune, RaxTune, HanaTune, 
 *          SqlServerTune, PostgresTune, MySqlTune, MariaDbTune, Db2Tune, ...
 * 
 * DbxTune is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * DbxTune is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with DbxTune.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.dbxtune.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;

import com.dbxtune.gui.ConnectionProfileManager.ProfileType;
import com.dbxtune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

public class ConnectionProfileTypeDialog
extends JDialog
implements ActionListener, TableModelListener
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private JDialog              _owner        = null;

	private JLabel                   _description1    = new JLabel("<html>Choose a Profile Type in the list below, and <b>set</b> Color and Border size.</html>");
	private JLabel                   _description2    = new JLabel("<html>Or <b>add/remove</b> Profiles by pressing Add/Remove</html>");

	private ProfileType              _currentProfileType = null;

	private JLabel                   _name_lbl        = new JLabel("Name");
	private JTextField               _name_txt        = new JTextField();
	private JButton                  _nameAdd_but     = new JButton("Add/Change");
	private JButton                  _nameDelete_but  = new JButton("Remove");

	private JLabel                   _color_lbl       = new JLabel("Color");
	private JTextField               _colorEx_txt     = new JTextField(" Border Color ");
	private JTextField               _colorRgb_txt    = new JTextField(" "+ConnectionProfileManager.ProfileType.getColorRgbStr(Color.WHITE));
	private Color                    _color_val       = Color.WHITE;
	private JButton                  _color_but       = new JButton("Open Color Picker...");
	
	private JLabel                   _margins_lbl     = new JLabel("Margin Sizes");
	private JButton                  _marginPlus_but  = new JButton("All +");
	private JButton                  _marginMinus_but = new JButton("All -");

	private JLabel                   _top_lbl         = new JLabel("Top");
	private SpinnerModel             _top_spm         = new SpinnerNumberModel(0, 0, 50, 1); // start, min, max, step
	private JSpinner                 _top_sp          = new JSpinner(_top_spm);

	private JLabel                   _left_lbl        = new JLabel("Left");
	private SpinnerModel             _left_spm        = new SpinnerNumberModel(0, 0, 50, 1); // start, min, max, step
	private JSpinner                 _left_sp         = new JSpinner(_left_spm);

	private JLabel                   _right_lbl       = new JLabel("Right");
	private SpinnerModel             _right_spm       = new SpinnerNumberModel(0, 0, 50, 1); // start, min, max, step
	private JSpinner                 _right_sp        = new JSpinner(_right_spm);

	private JLabel                   _bottom_lbl      = new JLabel("Bottom");
	private SpinnerModel             _bottom_spm      = new SpinnerNumberModel(0, 0, 50, 1); // start, min, max, step
	private JSpinner                 _bottom_sp       = new JSpinner(_bottom_spm);

	private JButton                  _ok              = new JButton("OK");
	private JButton                  _cancel          = new JButton("Cancel");
	private JButton                  _apply           = new JButton("Apply");

	private boolean             _okWasPressed = false;
	
	private LocalTableModel          _tableModel      = null;
//	private DefaultTableModel        _tableModel      = null;
	private JXTable                  _table           = null;
//	private JTable                   _table           = null;
//	private GTableFilter             _tableFilter     = null;
	private LocalTableModel          _tableModelAtStart = null; // this is used in method checkForChanges()
//	private LocalTable          _tab  = null;
//	private LocalTableModel     _tm   = null;
	private String              _name = null; 

	private int                      _dialogReturnSt  = JOptionPane.CANCEL_OPTION; //JOptionPane.CLOSED_OPTION;

//	private enum     TabPos        { Name,   Color,   Margins }; 
//	private String[] _tabHeadArr = {"Name", "Color", "Margins"};
//
	public ConnectionProfileTypeDialog(JDialog owner)
	{
		super(owner, "Profile Type Editor", ModalityType.APPLICATION_MODAL);
		_owner = owner;

		pinit();

		// Select first row...
//		if (_rstm.getRowCount() > 0)
//			_tab.getSelectionModel().setSelectionInterval(0, 0);
		
//		_tableModelAtStart = SwingUtils.copyTableModel(_tableModel);
		_tableModelAtStart = new LocalTableModel(_tableModel);
		pack();
//		applyFilter();

		// Try to fit all rows on the open window
		Dimension size = getSize();
		size.height += (_table.getRowCount() + 1) * SwingUtils.hiDpiScale(18); // each row takes 18 pixels  * numer of rows + 1 extra rows...
		setSize(size);
		
		SwingUtils.setSizeWithingScreenLimit(this, 5);
		SwingUtils.installEscapeButton(this, _cancel);
		SwingUtils.setFocus(_ok);

		setLocationRelativeTo(owner);
	}

	private ProfileType createProfileType()
	{
		Number top    = (Number) _top_spm   .getValue();
		Number left   = (Number) _left_spm  .getValue();
		Number right  = (Number) _right_spm .getValue();
		Number bottom = (Number) _bottom_spm.getValue();
		
		return new ProfileType(_name_txt.getText(), _color_val, new Insets(top.intValue(), left.intValue(), bottom.intValue(), right.intValue()));
	}

	private void setExample()
	{
		ProfileType example = createProfileType();
		ConnectionProfileManager.setBorderForConnectionProfileType(getContentPane(), example);
		_colorEx_txt.setBackground(example._color);
		_colorRgb_txt.setText(" "+example.getColorRgbStr());
		
		_currentProfileType = example;
	}


	private void pinit()
	{
		JPanel panel = new JPanel();
//		panel.setLayout(new MigLayout("insets 20 20","[][grow]",""));   // insets Top Left Bottom Right
		panel.setLayout(new MigLayout());   // insets Top Left Bottom Right

		String desc = 
				"<html>"
				+ "<b>Pick a rows from the below table</b><br>"
				+ "Double click or press 'OK' to close the list.<br>"
				+ "</html>";

		panel.add(_description1, "growx, span, wrap");
		panel.add(_description2, "growx, span, wrap 10");

		panel.add(_name_lbl,       "");
		panel.add(_name_txt,       "growx, pushx");
		panel.add(_nameAdd_but,    "split");
		panel.add(_nameDelete_but, "wrap");

		panel.add(_color_lbl,      "");
		panel.add(_colorEx_txt,    "split, span");
		panel.add(_colorRgb_txt,   "");
		panel.add(_color_but,      "wrap");
		_colorEx_txt .setEditable(false);
		_colorRgb_txt.setEditable(false);

		JPanel marginPanel = new JPanel();
		marginPanel.setLayout(new MigLayout());   // insets Top Left Bottom Right
		marginPanel.add(_top_lbl,     "right, cell 1 0"); // cell column row
		marginPanel.add(_top_sp,      "right, cell 1 0");
		marginPanel.add(_left_lbl,    "right, cell 0 1");
		marginPanel.add(_left_sp,     "right, cell 0 1");
		marginPanel.add(_right_lbl,   "right, cell 3 1");
		marginPanel.add(_right_sp,    "right, cell 3 1");
		marginPanel.add(_bottom_lbl,  "right, cell 1 2");
		marginPanel.add(_bottom_sp,   "right, cell 1 2");

		panel.add(_margins_lbl,     "");
		panel.add(marginPanel,      "span, split");
		panel.add(_marginPlus_but,  "");
		panel.add(_marginMinus_but, "wrap");

		
		_table = createTable();
//		_tableFilter = new GTableFilter(_table);
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_table);

		panel.add(jScrollPane,  "span, grow, height 100%, push, wrap");
//		panel.add(_tableFilter, "growx, pushx, span, wrap");

//		panel.add(createTabPanel(),      "grow, push, wrap");
		panel.add(new JPanel(),    "span, split, growx, pushx");
		panel.add(createOkPanel(), "tag right, wrap");
//		panel.add(createOkPanel(), "span, growx, pushx, tag right, wrap");
//		panel.add(createOkPanel(), "gap top 20, right");

		setContentPane(panel);
		
		ChangeListener marginListener = new ChangeListener() 
		{
			@Override
			public void stateChanged(ChangeEvent e) 
			{
				setExample();
			}
		};
		_top_sp   .addChangeListener(marginListener);
		_left_sp  .addChangeListener(marginListener);
		_right_sp .addChangeListener(marginListener);
		_bottom_sp.addChangeListener(marginListener);

		_marginMinus_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_top_sp   .setValue(_top_sp   .getPreviousValue());
				_left_sp  .setValue(_left_sp  .getPreviousValue());
				_right_sp .setValue(_right_sp .getPreviousValue());
				_bottom_sp.setValue(_bottom_sp.getPreviousValue());
			}
		});

		_marginPlus_but.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_top_sp   .setValue(_top_sp   .getNextValue());
				_left_sp  .setValue(_left_sp  .getNextValue());
				_right_sp .setValue(_right_sp .getNextValue());
				_bottom_sp.setValue(_bottom_sp.getNextValue());
			}
		});

		
		_name_txt      .addActionListener(this);
		_nameAdd_but   .addActionListener(this);
		_nameDelete_but.addActionListener(this);
		_color_but     .addActionListener(this);
	}

	private JPanel createOkPanel()
	{
		// ADD the OK, Cancel, Apply buttons
		JPanel panel = new JPanel();
		panel.setLayout(new MigLayout("insets 0 0","",""));
		panel.add(_ok,     "tag ok");
		panel.add(_cancel, "tag cancel");
		panel.add(_apply,  "tag apply");
		
		_apply.setEnabled(false);

		_ok    .addActionListener(this);
		_cancel.addActionListener(this);
		_apply .addActionListener(this);

		return panel;
	}
//	private JPanel createTabPanel()
//	{
//		_table = createTable();
//
//		_table = new LocalTable(_name);
//
//		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));
//
//		p.add(new JScrollPane(_tab), "grow, push");
//
//		return p;
//	}

	private void apply()
	{
//		_tableFilter.resetFilter();
		
		// select NO rows.
		_table.clearSelection();

		ConnectionProfileManager.getInstance().setProfileTypes(_tableModel._rows);
		ConnectionProfileManager.getInstance().save();

		_apply.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();

		// --- FIELD: Name ---
		if (_name_txt.equals(source))
		{
			_nameAdd_but.doClick();
		}

		// --- BUTTON: ADD Entry ---
		if (_nameAdd_but.equals(source))
		{
			_tableModel.addOrChangeEntry(_currentProfileType);
		}

		// --- BUTTON: DELETE ---
		if (_nameDelete_but.equals(source))
		{
			_tableModel.deleteEntry(_currentProfileType);
		}

		// --- BUTTON: COLOR CHOOSER ---
		if (_color_but.equals(source))
		{
			Color newColor = JColorChooser.showDialog( ConnectionProfileTypeDialog.this, "Choose Color", _color_val);
			if (newColor != null)
			{
				_color_val = newColor;
				setExample();
			}
		}
		
		// --- BUTTON: OK ---
		if (_ok.equals(source))
		{
			apply();
			_okWasPressed = true;
			_dialogReturnSt = JOptionPane.OK_OPTION;
			setVisible(false);
		}

		// --- BUTTON: CANCEL ---
		if (_cancel.equals(source))
		{
			_okWasPressed = false;
			_dialogReturnSt = JOptionPane.CANCEL_OPTION;
			setVisible(false);
		}

		// --- BUTTON: APPLY ---
		if (_apply.equals(source))
		{
			apply();
		}
	}

	private void checkForChanges()
	{
//		System.out.println("checkForChanges()--------------------------");
		boolean changes = false;

		if (_tableModel.getRowCount() != _tableModelAtStart.getRowCount())
			changes = true;

		// Check for changes
		if ( ! changes )
		{
			for (int mrow=0; mrow<_tableModel.getRowCount(); mrow++)
			{
				ProfileType entry      = _tableModel       .getEntry(mrow);
				ProfileType startEntry = _tableModelAtStart.getEntry(mrow);

				if ( ! entry.equals(startEntry) )
					changes = true;

				if (changes)
					break;
			}
		}

		_apply.setEnabled(changes);
	}

	/* Called on fire* has been called on the TableModel */
	@Override
	public void tableChanged(final TableModelEvent e)
	{
		checkForChanges();
	}

//	public JTable createTable()
	public JXTable createTable()
	{
		// Create a TABLE
		_tableModel = new LocalTableModel();
		_tableModel.addTableModelListener(this);

		JXTable table = new JXTable(_tableModel);
		table.setSortable(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setColumnControlVisible(true);
		table.packAll();

		// Highlight the Color Column with the Background color of the ProfileType
		HighlightPredicate isColorColumn = new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				 return adapter.column == LocalTableModel.TAB_POS_COLOR;
			}
		};
		Highlighter highlighter = new AbstractHighlighter(isColorColumn)
		{
			@Override
			protected Component doHighlight(Component component, ComponentAdapter adapter)
			{
				int mrow = adapter.convertRowIndexToModel(adapter.row);
				ProfileType pt  = _tableModel.getEntry(mrow);
				
				// If background is the same as the ProfileType, then do not add any color... it will just look strange when selection the row
				Color defaultBgColor = UIManager.getColor("Table.background");
				if (defaultBgColor == null)
					defaultBgColor = Color.WHITE;

				if (defaultBgColor.equals(pt._color))
				{
					return component;
				}
				else
				{
					component.setBackground(pt._color);
					return component;
				}
			}
		};
		table.addHighlighter(highlighter);
		
		
//		table.setPreferredSize( new Dimension(200, SwingUtils.hiDpiScale(table.getRowCount()*16) + SwingUtils.hiDpiScale(30)) );
//		SwingUtils.calcColumnWidths(table);

		// Selection listener
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				int vrow = _table.getSelectedRow();
	
				if (vrow >= 0)
				{
					int mrow = _table.convertRowIndexToModel(vrow);

					LocalTableModel tm = (LocalTableModel) _table.getModel();

					ProfileType entry = tm.getEntry(mrow);

					_name_txt .setText(entry.getName());
					_color_val = entry._color;
					_top_spm   .setValue( entry._borderMargins.top );
					_left_spm  .setValue( entry._borderMargins.left );
					_right_spm .setValue( entry._borderMargins.right );
					_bottom_spm.setValue( entry._borderMargins.bottom );
					
					setExample();
				}
			}
		});

		return table;
	}

//	private Vector<Vector<Object>> populateTable()
//	{
//		Vector<Vector<Object>> tab = new Vector<Vector<Object>>();
//		Vector<Object>         row = new Vector<Object>();
//		List<ProfileType> _profileTypes = new ArrayList<ProfileType>();
//
//		for (ProfileType type : ConnectionProfileManager.getInstance().getProfileTypes().values())
//		{
//			row = new Vector<Object>();
//			row.setSize(TabPos.values().length);
//
//			row.set(TabPos.Name   .ordinal(), type.getName());//cm.getTabPanel().getIcon());
//			row.set(TabPos.Color  .ordinal(), type.getColorRgbStr());
//			row.set(TabPos.Margins.ordinal(), type.getMarginStr());
//
//			tab.add(row);
//
//			_profileTypes.add(type);
//		}
//
//		return tab;
//	}

	public boolean wasOkPressed()
	{
		return _okWasPressed;
	}

//	public BigDecimal getSelectedValuesAsBigDecimal(String colName) { return _tab.getSelectedValuesAsBigDecimal(colName, false); }
//	public Integer    getSelectedValuesAsInteger   (String colName) { return _tab.getSelectedValuesAsInteger   (colName, false); }
//	public Long       getSelectedValuesAsLong      (String colName) { return _tab.getSelectedValuesAsLong      (colName, false); }
//	public Object     getSelectedValuesAsObject    (String colName) { return _tab.getSelectedValuesAsObject    (colName, false); }
//	public Short      getSelectedValuesAsShort     (String colName) { return _tab.getSelectedValuesAsShort     (colName, false); }
//	public String     getSelectedValuesAsString    (String colName) { return _tab.getSelectedValuesAsString    (colName, false); }
//	public Timestamp  getSelectedValuesAsTimestamp (String colName) { return _tab.getSelectedValuesAsTimestamp (colName, false); }

	private static class LocalTableModel
	extends AbstractTableModel
	{
		private static final long serialVersionUID = 1L;

		private static final String[] TAB_HEADER = {"Name", "Color", "Margins"};
		private static final int TAB_POS_NAME     = 0;
		private static final int TAB_POS_COLOR    = 1;
		private static final int TAB_POS_MARGINS  = 2;
		
//		private enum     TabPos        { Name,   Color,   Margins }; 
//		private String[] _tabHeadArr = {"Name", "Color", "Margins"};

		private ArrayList<ProfileType> _rows = new ArrayList<ProfileType>();
//		private LinkedHashMap<String, ProfileType> _rows = new LinkedHashMap<String, ProfileType>();

		public LocalTableModel(LocalTableModel copyMe)
		{
			_rows = new ArrayList<ProfileType>(copyMe._rows);
//			_rows = new LinkedHashMap<String, ProfileType>(copyMe._rows);
		}

		public LocalTableModel()
		{
			populateTable();
		}

//		public void clear(boolean fireChange)
//		{
//			_rows.clear();
//			if (fireChange)
//				fireTableDataChanged();
//		}

		public ProfileType getEntry(int row)
		{
			return _rows.get(row);
		}

		public void deleteEntry(ProfileType entry)
		{
			_rows.remove(entry);
			fireTableDataChanged();
		}

//		public void deleteEntry(int row)
//		{
//			_rows.remove(row);
//			fireTableDataChanged();
//		}

		private int getIndexForName(String name)
		{
			for (int r=0; r<_rows.size(); r++)
			{
				ProfileType entry = _rows.get(r);
				if (name.equals(entry._name))
					return r;
			}
			return -1;
		}
		public void addOrChangeEntry(ProfileType entry)
		{
			if (entry == null)
				return;

			int index = getIndexForName(entry._name);
			if (index >= 0)
				_rows.set(index, entry);
			else
				_rows.add(entry);
//			_rows.put(entry.getName(), entry);

			fireTableDataChanged();
		}

		@Override
		public int getColumnCount() 
		{
			return TAB_HEADER.length;
		}

		@Override
		public String getColumnName(int column) 
		{
			switch (column)
			{
			case TAB_POS_NAME:    return TAB_HEADER[TAB_POS_NAME];
			case TAB_POS_COLOR:   return TAB_HEADER[TAB_POS_COLOR];
			case TAB_POS_MARGINS: return TAB_HEADER[TAB_POS_MARGINS];
			}
			return null;
		}

		@Override
		public int getRowCount()
		{
			return _rows.size();
		}

		@Override
		public Object getValueAt(int row, int column)
		{
			ProfileType entry = _rows.get(row);
			switch (column)
			{
			case TAB_POS_NAME:    return entry.getName();
//			case TAB_POS_COLOR:   return entry._color;
			case TAB_POS_COLOR:   return entry.getColorRgbStr();
			case TAB_POS_MARGINS: return entry.getMarginStr();
			}
			return null;
		}

		@Override
		public Class<?> getColumnClass(int column)
		{
			if (column == TAB_POS_COLOR) 
				return Color.class;

			return super.getColumnClass(column);
		}

		private void populateTable()
		{
			for (ProfileType entry : ConnectionProfileManager.getInstance().getProfileTypes().values())
			{
				addOrChangeEntry(entry);
			}
		}
	}

}
