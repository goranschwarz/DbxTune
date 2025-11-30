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

/**
 * @author <a href="mailto:goran_schwarz@hotmail.com">Goran Schwarz</a>
 */
package com.dbxtune.gui.wizard;

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

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.dbxtune.CounterController;
import com.dbxtune.cm.CounterSample;
import com.dbxtune.cm.CountersModel;
import com.dbxtune.config.dict.MonTablesDictionaryManager;
import com.dbxtune.gui.ResultSetTableModel;
import com.dbxtune.gui.swing.MultiLineLabel;
import com.dbxtune.utils.StringUtil;

import net.miginfocom.swing.MigLayout;


public class WizardUserDefinedCmPage5
extends WizardPage
implements ActionListener, TableModelListener
{
	private static final long serialVersionUID = 1L;

	private static final String WIZ_NAME = "PctColls";
	private static final String WIZ_DESC = "Percentage Calculation Columns";
	private static final String WIZ_HELP = "What Columns do you want to do Percentage calculation on.";
	
	private static final String[] TAB_HEADER = {"PK", "Diff", "Pct", "Column Name", "Data Type", "Column Num", "JDBC Type Desc", "Jdbc Type"};
	private static final int TAB_POS_COL_PK            = 0; 
	private static final int TAB_POS_COL_DIFF          = 1; 
	private static final int TAB_POS_CHECK             = 2; 
	private static final int TAB_POS_COL_NAME          = 3; 
	private static final int TAB_POS_DATA_TYPE         = 4; 
	private static final int TAB_POS_COL_NUM           = 5; 
	private static final int TAB_POS_COL_JDBC_TYPE_STR = 6; 
	private static final int TAB_POS_COL_JDBC_TYPE_INT = 7; 

	private boolean    _firtsTimeRender = true;
	private String[]   _toolTipMonTables = {};

	private JCheckBox  _data_chk        = new JCheckBox("Specify Percentage Column(s)", false);
	private JXTable    _table           = null;
	private JButton    _selectAll_but   = new JButton("Select All");
	private JButton    _deSelectAll_but = new JButton("Deselect All");

	public static String getDescription() { return WIZ_DESC; }
	@Override
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPage5()
	{
		super(WIZ_NAME, WIZ_DESC);

		setLayout(new MigLayout(WizardOffline.MigLayoutConstraints1, WizardOffline.MigLayoutConstraints2, WizardOffline.MigLayoutConstraints3));

		add( new MultiLineLabel(WIZ_HELP), "wmin 100, span, pushx, growx, wrap" );

		add(_data_chk, "grow, span");
//		add(createPkTablePanel(), "grow, span, height 100%");
		
		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.add(TAB_HEADER[TAB_POS_COL_PK]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_DIFF]);
		tabHead.add(TAB_HEADER[TAB_POS_CHECK]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_NAME]);
		tabHead.add(TAB_HEADER[TAB_POS_DATA_TYPE]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_NUM]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_JDBC_TYPE_STR]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_JDBC_TYPE_INT]);

		Vector<Vector<Object>> tabData = new Vector<Vector<Object>>();

		AbstractHighlighter disableSomeRows = new AbstractHighlighter()
		{ 
			@Override
			protected Component doHighlight(Component comp, ComponentAdapter adapter) 
			{
				if (_data_chk.isSelected())
				{
					if (adapter.column < TAB_POS_CHECK)
						comp.setEnabled( false );
					else
						comp.setEnabled( _table.isCellEditable(adapter.row, TAB_POS_CHECK) );
				}
				else
					comp.setEnabled( false );
				return comp;
			}
		};
		DefaultTableModel defaultTabModel = new DefaultTableModel(tabData, tabHead)
		{
            private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) 
			{
				if (column == TAB_POS_COL_PK)   return Boolean.class;
				if (column == TAB_POS_COL_DIFF) return Boolean.class;
				if (column == TAB_POS_CHECK)    return Boolean.class;
				return Object.class;
			}
			@Override
			public boolean isCellEditable(int row, int column)
			{
				if (column == TAB_POS_CHECK)
				{
					// IS PK Row, do NOT allow to edit
					Object o = getValueAt(row, TAB_POS_COL_PK);
					if (o instanceof Boolean && ((Boolean)o).booleanValue())
						return false;

//					// Datatype is NOT number, do NOT allow edit
//					String datatype = (String) getValueAt(row, TAB_POS_DATA_TYPE);
//					return CounterSample.isDiffAllowedForDatatype(datatype);

					// Datatype is NOT number, do NOT allow edit
					Integer jdbcType = (Integer) getValueAt(row, TAB_POS_COL_JDBC_TYPE_INT);
					return CounterSample.isDiffAllowedForDatatype(jdbcType);
				}
				
				return false;
			}
		};
		defaultTabModel.addTableModelListener(this);

		// Extend the JXTable to get tooptip stuff
		_table = new JXTable()
		{
	        private static final long serialVersionUID = 0L;

			@Override
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
							tip = MonTablesDictionaryManager.getInstance().getDescription(_toolTipMonTables, (String)cellVal);
						}
					}
				}
				return tip;
			}
		};

		_table.setHighlighters(new Highlighter[] {disableSomeRows});
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
						String datatype    = sc.getColSqlTypeName(r-1);
						int    jdbcType    = sc.getColSqlType(r-1);
						String jdbcTypeStr = ResultSetTableModel.getColumnJavaSqlTypeName(jdbcType);

						row = new Vector<Object>();
						row.add(Boolean.valueOf( false ));  // TAB_POS_COL_PK
						row.add(Boolean.valueOf( false ));  // TAB_POS_COL_DIFF
						row.add(Boolean.valueOf( false ));  // TAB_POS_CHECK
						row.add(col);                   // TAB_POS_COL_NAME
						row.add(datatype);              // TAB_POS_DATA_TYPE
						row.add(Integer.valueOf( r ));      // TAB_POS_COL_NUM
						row.add(jdbcTypeStr);           // TAB_POS_COL_JDBC_TYPE_STR
						row.add(Integer.valueOf(jdbcType)); // TAB_POS_COL_JDBC_TYPE

						tm.addRow(row);
					}
				}
			}

			// CHECKBOX
			String pkStr   = (String) getWizardData("pk")   + ", ";
			String diffStr = (String) getWizardData("diff") + ", ";
			String pctStr  = (String) getWizardData("pct")  + ", ";

			for (int r=0; r<tm.getRowCount(); r++)
			{
				String  colName   = (String) tm.getValueAt(r, TAB_POS_COL_NAME);

				boolean isPkCol   = (pkStr  .indexOf(colName+", ") != -1);
				boolean isDiffCol = (diffStr.indexOf(colName+", ") != -1);
				boolean isPctCol  = (pctStr .indexOf(colName+", ") != -1);

				tm.setValueAt(Boolean.valueOf(isPkCol),   r, TAB_POS_COL_PK);
				tm.setValueAt(Boolean.valueOf(isDiffCol), r, TAB_POS_COL_DIFF);
				tm.setValueAt(Boolean.valueOf(isPctCol),  r, TAB_POS_CHECK);
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
			boolean hasPct = false;
			TableModel tm = _table.getModel();

			for (int r=0; r<tm.getRowCount(); r++)
			{
				String colName  = (String) tm.getValueAt(r, TAB_POS_COL_NAME);
				if (cm.isPctColumn(cm.findColumn(colName)))
				{
					tm.setValueAt(Boolean.valueOf(true), r, TAB_POS_CHECK);
					hasPct = true;
				}
			}
			if (hasPct)
				_data_chk.setSelected(true);
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
		return null;
	}

	@Override
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
				if (tm.isCellEditable(r, TAB_POS_CHECK))
					tm.setValueAt(Boolean.valueOf(true), r, TAB_POS_CHECK);
			}
		}

		if (_deSelectAll_but.equals(src))
		{
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if (tm.isCellEditable(r, TAB_POS_CHECK))
					tm.setValueAt(Boolean.valueOf(false), r, TAB_POS_CHECK);
			}
		}
	}

	@Override
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
			}
	
			// Discard last ', '
			if (pkStr.length() > 0)
				pkStr = pkStr.substring(0, pkStr.length()-2);

			putWizardData("pct", pkStr);
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
