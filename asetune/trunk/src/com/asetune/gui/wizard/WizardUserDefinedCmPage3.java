/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.asetune.gui.wizard;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.CounterController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.StringUtil;


public class WizardUserDefinedCmPage3
extends WizardPage
implements ActionListener, TableModelListener
{
	private static final long serialVersionUID = 1L;
//	private static Logger _logger          = Logger.getLogger(WizardUserDefinedCmPage3.class);

	private static final String WIZ_NAME = "PKCols";
	private static final String WIZ_DESC = "Primary Key Columns";
	private static final String WIZ_HELP = "If you want to do data difference calculation on counter data samples, you need to specify a Primary Key for the counter data. If this is not specified the counter data is just expected to contain 1 row.";
	
	private static final String[] TAB_HEADER = {"PK", "Column Name", "Column Num"};
	private static final int TAB_POS_CHECK      = 0; 
	private static final int TAB_POS_COL_NAME   = 1; 
	private static final int TAB_POS_COL_NUM    = 2; 

	private boolean    _firtsTimeRender = true;
	private String[]   _toolTipMonTables = {};

	private JCheckBox  _data_chk        = new JCheckBox("Specify Primary Key(s)", true);
	private JXTable    _table           = null;
	private JButton    _selectAll_but   = new JButton("Select All");
	private JButton    _deSelectAll_but = new JButton("Deselect All");

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPage3()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		add( new MultiLineLabel(WIZ_HELP), "wmin 100, span, pushx, growx, wrap" );

		add(_data_chk, "grow, span");
//		add(createPkTablePanel(), "grow, span, height 100%");
		
		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.add(TAB_HEADER[TAB_POS_CHECK]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_NAME]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_NUM]);

		Vector<Vector<Object>> tabData = new Vector<Vector<Object>>();

		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			public Class<?> getColumnClass(int column) 
			{
				if (column == TAB_POS_CHECK) return Boolean.class;
				return Object.class;
			}
			public boolean isCellEditable(int row, int column)
			{
				return column == TAB_POS_CHECK;
			}
		};
		defaultTabModel.addTableModelListener(this);

		// Extend the JXTable to get tooptip stuff
		_table = new JXTable()
		{
	        private static final long serialVersionUID = 0L;

			public String getToolTipText(MouseEvent e) 
			{
				String tip = null;
				Point p = e.getPoint();
				int row = rowAtPoint(p);
				int col = columnAtPoint(p);
				if (row > 0 && col > 0)
				{
					col = convertColumnIndexToModel(col);
					row = convertRowIndexToModel(row);

					if (col == TAB_POS_COL_NAME)
					{
						TableModel model = getModel();
						Object cellVal = model.getValueAt(row, col);
						if (cellVal instanceof String)
						{
							tip = MonTablesDictionary.getInstance().getDescription(_toolTipMonTables, (String)cellVal);
						}
					}
				}
				return tip;
			}
		};

		_table.setModel( defaultTabModel );
		_table.setShowGrid(false);
		_table.setSortable(true);
		_table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		_table.packAll(); // set size so that all content in all cells are visible
		_table.setSortable(true);
		_table.setColumnControlVisible(true);

		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setViewportView(_table);
		add(jScrollPane, "push, grow, height 100%, wrap");

		add(_selectAll_but,   "split");
		add(_deSelectAll_but, "");

		// ADD ACTION LISTENERS
		_data_chk       .addActionListener(this);
		_selectAll_but  .addActionListener(this);
		_deSelectAll_but.addActionListener(this);
	}



	private void refreshTable()
	{
		Vector<Object> row = new Vector<Object>();
		
		DefaultTableModel tm = (DefaultTableModel)_table.getModel();

		CounterSample sc = (CounterSample) getWizardData("CounterSample");
		if (sc != null)
		{
			List<String> names = sc.getColNames();
			if (names == null)
				return;

			boolean refresh = false;
			if (tm.getRowCount() == names.size())
			{
				for (int r=0; r<tm.getRowCount(); r++)
				{
					String colName = (String) tm.getValueAt(r, TAB_POS_COL_NAME);
					String cntName = (String) names.get(r);
					if ( ! colName.equals(cntName) )
					{
						refresh = true;
						break;
					}
				}
			}
			else
			{
				refresh = true;
			}

			if (refresh)
			{
				while (tm.getRowCount() > 0)
					tm.removeRow(0);

				int r = 0;
				for (String col : names)
				{
					r++;
					
					if (col != null)
					{
						row = new Vector<Object>();
						row.add(new Boolean( false )); // TAB_POS_CHECK
						row.add(col);                  // TAB_POS_COL_NAME
						row.add(new Integer( r ));     // TAB_POS_COL_NUM

						tm.addRow(row);
					}
				}
			}

			// CHECKBOX
			String pkStr = (String) getWizardData("pk") + ", ";

			for (int r=0; r<tm.getRowCount(); r++)
			{
				String colName  = (String) tm.getValueAt(r, TAB_POS_COL_NAME);
				boolean colIsPk = (pkStr.indexOf(colName+", ") != -1);

				tm.setValueAt(new Boolean(colIsPk), r, TAB_POS_CHECK);
			}

		}
		_table.packAll(); // set size so that all content in all cells are visible
	}

	private void applyFromTemplate()
	{
		String cmName = (String) getWizardData("cmTemplate");
		if (cmName == null)
			return;
//		CountersModel cm = GetCounters.getInstance().getCmByName(cmName);
		CountersModel cm = CounterController.getInstance().getCmByName(cmName);
		if (cm != null)
		{
			List<String> pklist = cm.getPk();
			
			if (pklist == null || ( pklist != null && pklist.size() == 0) )
			{
				_data_chk.setSelected(false);
			}
			else
			{
				TableModel tm = _table.getModel();
	
				for (int r=0; r<tm.getRowCount(); r++)
				{
					String colName  = (String) tm.getValueAt(r, TAB_POS_COL_NAME);
					if (pklist.contains(colName))
						tm.setValueAt(new Boolean(true), r, TAB_POS_CHECK);
				}
			}
		}
	}
	
	/** Called when we enter the page */
	@Override
	protected void renderingPage()
    {
		refreshTable();
		if (_firtsTimeRender)
		{
			applyFromTemplate();
		}
		_toolTipMonTables = StringUtil.commaStrToArray( (String)getWizardData("toolTipMonTables") );
	    _firtsTimeRender = false;
    }


	@Override
	protected String validateContents(Component comp, Object event)
	{
//		String name = null;
//		if (comp != null)
//			name = comp.getName();
//
//		System.out.println("validateContents: name='"+name+"',\n\ttoString='"+comp+"'\n\tcomp='"+comp+"',\n\tevent='"+event+"'.");

		boolean enable = _data_chk.isSelected();
		_table          .setEnabled(enable);
		_selectAll_but  .setEnabled(enable);
		_deSelectAll_but.setEnabled(enable);

		if (_data_chk.isSelected())
		{
			int rows = 0;
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if ( ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue() )
				{
					rows++;
				}
				//putWizardData( tm.getValueAt(r, TAB_POS_COL_NAME), "" + ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue());
			}
	
			return rows > 0 ? null : "Atleast one session needs to be checked.";
		}
		else
		{
			CounterSample sc = (CounterSample) getWizardData("CounterSample");
			if (sc.getRowCount() > 1)
			{
				return "The current SQL statement DOES return more than one row, then we need a PrimaryKey.";
			}
		}
		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

//		System.out.println("Source("+name+"): " + src);

		if (_selectAll_but.equals(src))
		{
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(new Boolean(true), r, TAB_POS_CHECK);
			}
		}

		if (_deSelectAll_but.equals(src))
		{
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				tm.setValueAt(new Boolean(false), r, TAB_POS_CHECK);
			}
		}
	}

	public void tableChanged(TableModelEvent e)
	{
		// This wasnt kicked off for a table change...
		setProblem(validateContents(null,null));
	}

	private void saveWizardData()
	{
		if (_data_chk.isSelected())
		{
			String pkStr = "";
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if ( ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue() )
				{
					pkStr += tm.getValueAt(r, TAB_POS_COL_NAME) + ", ";
				}
				//putWizardData( tm.getValueAt(r, TAB_POS_COL_NAME), "" + ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue());
			}
	
			// Discard last ', '
			if (pkStr.length() > 0)
				pkStr = pkStr.substring(0, pkStr.length()-2);

			putWizardData("pk", pkStr);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowBack(String stepName, Map settings, Wizard wizard)
    {
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
    }

	@SuppressWarnings("rawtypes")
	@Override
	public WizardPanelNavResult allowNext(String stepName, Map settings, Wizard wizard)
	{
		saveWizardData();
		return WizardPanelNavResult.PROCEED;
	}
}
