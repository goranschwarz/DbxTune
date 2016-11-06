package com.asetune.sql;

import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.sort.RowFilters;

import com.asetune.gui.ResultSetTableModel;
import com.asetune.gui.swing.ClickListener;
import com.asetune.gui.swing.GTable;
import com.asetune.utils.SwingUtils;

import net.miginfocom.swing.MigLayout;

//import com.sap.dbmtk.demo.ui.ClickListener;
//import com.sap.dbmtk.demo.ui.GTable;
//import com.sap.dbmtk.demo.ui.ResultSetTableModel;
//import com.sap.dbmtk.demo.utils.SwingUtils;

public class SqlPickList
extends JDialog
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private Window              _owner        = null;
	private boolean             _showPickMeLabel = true;
//	private SqlStatement        _sqlStatement = null;
//	private ConnectionProvider  _connProvider = null;

	protected JLabel            _filter_lbl   = new JLabel("Filter: ");
	protected JTextField        _filter_txt   = new JTextField();
	protected JLabel            _filter_cnt   = new JLabel();

	private SqlPickListTable    _tab  = null;
	private ResultSetTableModel _rstm = null;
	private String              _name = null; 

	private JButton             _ok     = new JButton("OK");
	private JButton             _cancel = new JButton("Cancel");

	private boolean             _okWasPressed = false;
	
//	public SqlPickList(Window owner, ConnectionProvider connProvider, SqlStatement sqlStatement, String name)
//	{
//		super(owner, "Pick List", ModalityType.APPLICATION_MODAL);
//
//		_owner = owner;
//		_connProvider = connProvider;
//		_sqlStatement = sqlStatement;
//		_name         = name;
//		
//		pinit();
//		execute();
//		pack();
//		applyFilter();
//
//		SwingUtils.installEscapeButton(this, _cancel);
//
//		setLocationRelativeTo(owner);
//	}
	public SqlPickList(Window owner, ResultSetTableModel rstm, String label, boolean showPickMeLabel)
	{
		super(owner, (label==null ? "Pick List" : label), ModalityType.APPLICATION_MODAL);

		_owner = owner;
		_rstm = rstm;
		_showPickMeLabel = showPickMeLabel;

//		_connProvider = connProvider;
//		_sqlStatement = sqlStatement;
//		_name         = name;
		
		pinit();

		_tab.setModel(_rstm);
		_tab.packAll();

		// Select first row...
		if (_rstm.getRowCount() > 0)
			_tab.getSelectionModel().setSelectionInterval(0, 0);
		
		pack();
		applyFilter();

		SwingUtils.installEscapeButton(this, _cancel);

		setLocationRelativeTo(owner);
	}

	private void pinit()
	{
		setLayout(new MigLayout());

		String desc = 
				"<html>"
				+ "<b>Pick a rows from the below table</b><br>"
				+ "Double click or press 'OK' to close the list.<br>"
				+ "</html>";

		_filter_txt.setToolTipText("Client filter, that does regular expression on all table cells using this value");
		_filter_cnt.setToolTipText("Visible rows / actual rows in the GUI Table");

		if (_showPickMeLabel)
			add(new JLabel(desc),  "growx, pushx, wrap");
		add(_filter_lbl,           "split");
		add(_filter_txt,           "growx, pushx");
		add(_filter_cnt,           "wrap");
		add(createTabPanel(),      "grow, push, wrap");
		add(createOkCancelPanel(), "growx, pushx, tag right, wrap");

		
		_filter_txt.addCaretListener(new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent e)
			{
				applyFilter();
			}
		});

		_ok.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_okWasPressed = true;
				setVisible(false);
			}
		});

		_cancel.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				_okWasPressed = false;
				setVisible(false);
			}
		});
	}

	private JPanel createOkCancelPanel()
	{
		JPanel p = new JPanel(new MigLayout());

		p.add(new JLabel(),    "split, span, growx, pushx"); // dummy label to push everything to the right

		p.add(_ok,     "tag ok");
		p.add(_cancel, "tag cancel");

		return p;
	}

	private JPanel createTabPanel()
	{
		_tab = new SqlPickListTable(_name);

		JPanel p = new JPanel(new MigLayout("insets 0 0 0 0"));

		p.add(new JScrollPane(_tab), "grow, push");

		return p;
	}

	public void applyFilter()
	{
		String searchString = _filter_txt.getText().trim();
		if ( searchString.length() <= 0 ) 
			_tab.setRowFilter(null);
		else
		{
			// Create a array with all visible columns... hence: it's only those we want to search
			// Note the indices are MODEL column index
			int[] mcols = new int[_tab.getColumnCount()];
			for (int i=0; i<mcols.length; i++)
				mcols[i] = _tab.convertColumnIndexToModel(i);
			
			_tab.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, searchString + ".*", mcols));
		}
		
		String rowc = _tab.getRowCount() + "/" + _tab.getModel().getRowCount();
		_filter_cnt.setText(rowc);
	}


//	/**
//	 * Returns number of rows found 
//	 * @return
//	 */
//	public int execute()
//	{
//		try
//		{
//			// Executed the SQL
////			_sqlStatement.execute( _connProvider );
//			_sqlStatement.executeWithGuiProgress( _connProvider, getOwner() );
//			
//			if (_sqlStatement.getResultSetCount() == 0)
//				throw new Exception("No ResultSet was produced by the SQL Statement.");
//
//			// Get the ResultSet and set in in the TABLE
//			_rstm = _sqlStatement.getResultSet(0);
//
//			_tab.setModel(_rstm);
//			_tab.packAll();
//
////			Dimension dialogSize = _tab.getPreferredSize();
////			dialogSize.height += 50;
////			dialogSize.width  += 50;
////			setPreferredSize(dialogSize);
//
//			SqlStatement.showWarningMessages(_connProvider, _owner, "Warning messages from db for: "+getName());
//			
//			// Select first row...
//			if (_rstm.getRowCount() > 0)
//				_tab.getSelectionModel().setSelectionInterval(0, 0);
//
//			return _rstm.getRowCount();
//		}
//		catch (Exception ex)
//		{
//			SqlStatement.showErrorMessage(ex, _sqlStatement, _owner, "Getting "+getName()+" information <b>failed.</b>");
//		}
//
//		return -1;
//	}


	public boolean wasOkPressed()
	{
		return _okWasPressed;
	}

	public int getSelectedRow()
	{
		return _tab.getSelectedRow();
	}

	public int getRowCount()
	{
		return _tab.getRowCount();
	}

	public BigDecimal getSelectedValuesAsBigDecimal(String colName) { return _tab.getSelectedValuesAsBigDecimal(colName, false); }
	public Integer    getSelectedValuesAsInteger   (String colName) { return _tab.getSelectedValuesAsInteger   (colName, false); }
	public Long       getSelectedValuesAsLong      (String colName) { return _tab.getSelectedValuesAsLong      (colName, false); }
	public Object     getSelectedValuesAsObject    (String colName) { return _tab.getSelectedValuesAsObject    (colName, false); }
	public Short      getSelectedValuesAsShort     (String colName) { return _tab.getSelectedValuesAsShort     (colName, false); }
	public String     getSelectedValuesAsString    (String colName) { return _tab.getSelectedValuesAsString    (colName, false); }
	public Timestamp  getSelectedValuesAsTimestamp (String colName) { return _tab.getSelectedValuesAsTimestamp (colName, false); }


	private class SqlPickListTable
	extends GTable
	{
		private static final long serialVersionUID = 1L;

		public SqlPickListTable(String name)
		{
			// Reset renderer for BigDecimal, GTable formated it in a little different way
			setDefaultRenderer(BigDecimal.class, null);

			// Add selection 
			getSelectionModel().addListSelectionListener(new ListSelectionListener()
			{
				@Override
				public void valueChanged(ListSelectionEvent e)
				{
					if (e.getValueIsAdjusting())
						return;

					boolean enable = getSelectedRowCount() == 1;
					_ok.setEnabled(enable);
					_ok.setEnabled(enable);
				}
			});

			// Add double click listener on the table
			addMouseListener(new ClickListener()
			{
				@Override
				public void doubleClick(MouseEvent e)
				{
					Point p = e.getPoint();
					int vrow = rowAtPoint(p);
					if ( vrow >= 0 )
					{
						_ok.setEnabled(true);

						getSelectionModel().setSelectionInterval(vrow, vrow);
						_ok.doClick();
					}
				}
			});

			// key: ENTER
			getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "SELECT_CURRENT_ROW");
			getActionMap().put("SELECT_CURRENT_ROW", new AbstractAction("SELECT_CURRENT_ROW")
			{
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e)
				{
					_ok.doClick();
				}
			});

			// name of the table
			setName(name);

			// Set some table props
			setSortable(true);
			setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
			packAll();
			setColumnControlVisible(true);
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		}
	}
}
