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

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.Highlighter;
import org.netbeans.spi.wizard.Wizard;
import org.netbeans.spi.wizard.WizardPage;
import org.netbeans.spi.wizard.WizardPanelNavResult;

import com.asetune.CounterController;
import com.asetune.cm.CounterSample;
import com.asetune.cm.CountersModel;
import com.asetune.config.dict.MonTablesDictionary;
import com.asetune.gui.TrendGraph;
import com.asetune.gui.swing.MultiLineLabel;
import com.asetune.utils.StringUtil;


//PAGE 1
public class WizardUserDefinedCmPage6
extends WizardPage
implements ActionListener, TableModelListener
{
	private static final long serialVersionUID = 1L;
	private static final String WIZ_NAME  = "Graph-info";
	private static final String WIZ_DESC  = "Graph information";
	private static final String WIZ_HELP  = "A graph can be attached to the Counter Data, the graph will be displaied in the Summary panel.";
	private static final String WIZ_HELP1 = "Name of the Graph.";
	private static final String WIZ_HELP2 = "The label that will be writen above the Graph.";
	private static final String WIZ_HELP3 = "In the view menu you can enable disable the graph, this is the text for that menu.";

	private static final String[] TAB_HEADER = {"PK", "Diff", "Pct", "Use", "Column Name", "Data Type", "Axis Label", "Method", "Column Num"};
	private static final int TAB_POS_COL_PK     = 0; 
	private static final int TAB_POS_COL_DIFF   = 1; 
	private static final int TAB_POS_COL_PCT    = 2; 
	private static final int TAB_POS_CHECK      = 3; 
	private static final int TAB_POS_COL_NAME   = 4; 
	private static final int TAB_POS_DATA_TYPE  = 5; 
	private static final int TAB_POS_AXIS_LABEL = 6; 
	private static final int TAB_POS_METHOD     = 7; 
	private static final int TAB_POS_COL_NUM    = 8; 
	
	private static final String NO_METHOD = "<choose one>";

	private static final String TOOLTIP_METHOD = "<html>" +
		"ABS values are taken from the Absolute counter data set" +
		"<ul>" +
		"<li> absVal: The value in the cell (only if type is 'By Row')" +
		"<li> absMax: Maximum value for this column (only if type is 'By Column')" +
		"<li> absMin: Minimum value for this column (only if type is 'By Column')" +
		"<li> absAvg: Average value for this column (only if type is 'By Column')" +
		"<li> absAvgGtZero: Average value for this column, cells with zero will be discarded in the calculation (only if type is 'By Column')" +
		"<li> absSum: The summary of all cells in this column (only if type is 'By Column')" +
		"</ul>" +

		"DIFF values are taken from the data set which is the difference by two sample interwalls " +
		"<ul>" +
		"<li> diffVal: The value in the cell (only if type is 'By Row')" +
		"<li> diffMax: Maximum value for this column (only if type is 'By Column')" +
		"<li> diffMin: Minimum value for this column (only if type is 'By Column')" +
		"<li> diffAvg: Average value for this column (only if type is 'By Column')" +
		"<li> diffAvgGtZero: Average value for this column, cells with zero will be discarded in the calculation (only if type is 'By Column')" +
		"<li> diffSum: The summary of all cells in this column (only if type is 'By Column')" +
		"</ul>" +

		"RATE values are the calculated delta values / sample time " +
		"<ul>" +
		"<li> rateVal: The value in the cell (only if type is 'By Row')" +
		"<li> rateMax: Maximum value for this column (only if type is 'By Column')" +
		"<li> rateMin: Minimum value for this column (only if type is 'By Column')" +
		"<li> rateAvg: Average value for this column (only if type is 'By Column')" +
		"<li> rateAvgGtZero: Average value for this column, cells with zero will be discarded in the calculation (only if type is 'By Column')" +
		"<li> rateSum: The summary of all cells in this column (only if type is 'By Column')" +
		"</ul>" +
		"</html>";
	private static final String TOOLTIP_AXIS_LABEL = "<html>" +
		"<html>" +
		"<p>Double click to edit the Label, which will be displayed below the Graph.</p>" +
		"<p>If graph type is 'By Row' and Label is '-pk-' the Label will consist of all the columns in the PrimaryKey for this row.</p>" +
		"</html>";

	private boolean    _firtsTimeRender = true;
	private String[]   _toolTipMonTables = {};

	private String[] _validGraphTypesStr    = {"byCol",     "byRow"};
	private String[] _validGraphTypesBoxStr = {"By Column", "By Row"};
//	private String[] _validGraphMethods = CountersModel.getValidGraphMethods("byCol");

	private JCheckBox  _crGraph_chk   = new JCheckBox("Create a User Defined Graph", false);

	private JLabel     _graphType_lbl = new JLabel("Graph Type");
	private JComboBox  _graphType_cbx = new JComboBox(_validGraphTypesBoxStr);
//	private JComboBox  _graphMethods_cbx = new JComboBox(_validGraphMethods);

	private JLabel     _name_lbl       = new JLabel("Graph Name");
	private JTextField _name_txt       = new JTextField("");
	private JLabel     _graphLabel_lbl = new JLabel("Graph Label");
	private JTextField _graphLabel_txt = new JTextField("");
	private JLabel     _menuDesc_lbl   = new JLabel("Menu Description");
	private JTextField _menuDesc_txt   = new JTextField("");
	private JXTable    _table          = null;//new JXTable();

	public static String getDescription() { return WIZ_DESC; }
	public Dimension getPreferredSize() { return WizardUserDefinedCm.preferredSize; }

	public WizardUserDefinedCmPage6()
	{
		super(WIZ_NAME, WIZ_DESC);
		
		setLayout(new MigLayout("", "[] [grow] []", ""));

		_crGraph_chk   .setName("graph");
//		_graphType_cbx .setName("graph.type");
		_name_txt      .setName("graph.name");
		_graphLabel_txt.setName("graph.label");
		_menuDesc_txt  .setName("graph.menuLabel");

		add( new MultiLineLabel(WIZ_HELP), "wmin 100, span, pushx, growx, wrap" );
		add(_crGraph_chk, "span, growx, pushx, wrap");

//		add( new MultiLineLabel(WIZ_HELP1), "wmin 100, span, pushx, growx, wrap" );
		_name_lbl.setToolTipText(WIZ_HELP1);
		_name_txt.setToolTipText(WIZ_HELP1);
		add(_name_lbl);
		add(_name_txt, "growx, pushx, wrap");

//		add( new MultiLineLabel(WIZ_HELP2), "wmin 100, span, pushx, growx, wrap" );
		_graphLabel_lbl.setToolTipText(WIZ_HELP2);
		_graphLabel_txt.setToolTipText(WIZ_HELP2);
		add(_graphLabel_lbl);
		add(_graphLabel_txt, "growx, pushx, wrap");

//		add( new MultiLineLabel(WIZ_HELP3), "wmin 100, span, pushx, growx, wrap" );
		_menuDesc_lbl.setToolTipText(WIZ_HELP3);
		_menuDesc_txt.setToolTipText(WIZ_HELP3);
		add(_menuDesc_lbl);
		add(_menuDesc_txt, "growx, pushx, wrap");

		// GRAPH TYPE
		String toolTip = "<html>" +
				"The graph type can be:" +
				"<p>By Column: \"max/min/avg/sum\" over 1 column, meaning one graph-line for each specified column</p>" +
				"<p>By Row: One graph-line per row in the counter data showing the point value for each row.</p>" +
				"<html>";
		_graphType_lbl.setToolTipText(toolTip);
		_graphType_cbx.setToolTipText(toolTip);
		add(_graphType_lbl);
		add(_graphType_cbx, "growx, pushx, wrap");

//		add(_graphMethods_cbx, "growx, pushx, wrap 30");
				

		// Create a TABLE
		Vector<String> tabHead = new Vector<String>();
		tabHead.add(TAB_HEADER[TAB_POS_COL_PK]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_DIFF]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_PCT]);
		tabHead.add(TAB_HEADER[TAB_POS_CHECK]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_NAME]);
		tabHead.add(TAB_HEADER[TAB_POS_DATA_TYPE]);
		tabHead.add(TAB_HEADER[TAB_POS_AXIS_LABEL]);
		tabHead.add(TAB_HEADER[TAB_POS_METHOD]);
		tabHead.add(TAB_HEADER[TAB_POS_COL_NUM]);

		Vector<Vector<Object>> tabData = new Vector<Vector<Object>>();

		AbstractHighlighter disableSomeRows = new AbstractHighlighter()
		{ 
			protected Component doHighlight(Component comp, ComponentAdapter adapter) 
			{
				if (_crGraph_chk.isSelected())
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

			public Class<?> getColumnClass(int column) 
			{
				if (column == TAB_POS_COL_PK)   return Boolean.class;
				if (column == TAB_POS_COL_DIFF) return Boolean.class;
				if (column == TAB_POS_COL_PCT)  return Boolean.class;
				if (column == TAB_POS_CHECK)    return Boolean.class;
				return Object.class;
			}
			public boolean isCellEditable(int row, int column)
			{
				if (column == TAB_POS_CHECK)
				{
					// IS PK Row, do NOT allow to edit
					Object o = getValueAt(row, TAB_POS_COL_PK);
					if (o instanceof Boolean && ((Boolean)o).booleanValue())
						return false;

					// Datatype is NOT number, do NOT allow edit
					String datatype = (String) getValueAt(row, TAB_POS_DATA_TYPE);
					return CounterSample.isDiffAllowedForDatatype(datatype);
				}
				// if NOT CHECKED, nothing should be editable
				Object o = getValueAt(row, TAB_POS_CHECK);
				if (o instanceof Boolean && ((Boolean)o).booleanValue() == false)
					return false;

				if (column == TAB_POS_AXIS_LABEL && _graphType_cbx.getSelectedIndex() == 0) return true;
				if (column == TAB_POS_METHOD)     return true;
				
				return false;
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

					if (col == TAB_POS_AXIS_LABEL)   return TOOLTIP_AXIS_LABEL;
					if (col == TAB_POS_METHOD)       return TOOLTIP_METHOD;
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
		add(jScrollPane, "span, push, grow, height 100%, wrap");

		// Add actions
		_graphType_cbx.addActionListener(this);
	}

	private void refreshComboBox(JComboBox cbx)
	{
		int    typeInt = _graphType_cbx.getSelectedIndex();
		String typeStr = "";
		if      (typeInt == 0) typeStr = "byCol";
		else if (typeInt == 1) typeStr = "byRow";
		else                   typeStr = "-unknown-";
			
		boolean allMethods = true;		
		String[] m = CountersModel.getValidGraphMethods(typeStr, allMethods);
		
		cbx.removeAllItems();
		cbx.addItem(NO_METHOD);
		for (int i = 0; i < m.length; i++)
			cbx.addItem(m[i]);
		
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
					
					String label = col;
					if (_graphType_cbx.getSelectedIndex() == 1)
						label = "-pk-";
					
					if (col != null)
					{
						String datatype = sc.getColSqlTypeName(r-1);

						row = new Vector<Object>();
						row.add(new Boolean( false )); // TAB_POS_COL_PK
						row.add(new Boolean( false )); // TAB_POS_COL_DIFF
						row.add(new Boolean( false )); // TAB_POS_COL_PCT
						row.add(new Boolean( false )); // TAB_POS_CHECK
						row.add(col);                  // TAB_POS_COL_NAME
						row.add(datatype);             // TAB_POS_DATA_TYPE
						row.add(label);                // TAB_POS_AXIS_LABEL
						row.add(NO_METHOD);            // TAB_POS_METHOD
						row.add(new Integer( r ));     // TAB_POS_COL_NUM

						tm.addRow(row);
					}
				}

				// TAB_POS_METHOD
				JComboBox comboBox = new JComboBox();
				refreshComboBox(comboBox);
				TableColumn tc = _table.getColumnModel().getColumn(TAB_POS_METHOD);
				tc.setCellEditor(new DefaultCellEditor(comboBox));
			}

			// CHECKBOX
			String pkStr   = (String)getWizardData("pk")                + ", ";
			String diffStr = (String)getWizardData("diff")              + ", ";
			String pctStr  = (String)getWizardData("pct")               + ", ";
			String gColStr = (String)getWizardData("graph.data.cols")   + ", ";
			String gMthStr = (String)getWizardData("graph.data.methods")+ ", ";
			String gLblStr = (String)getWizardData("graph.data.labels") + ", ";

//System.out.println("PK: "      + pkStr);
//System.out.println("DIFF: "    + diffStr);
//System.out.println("PCT: "     + pctStr);
//System.out.println("gColStr: " + gColStr);
//System.out.println("gMthStr: " + gMthStr);
//System.out.println("gLblStr: " + gLblStr);

			for (int r=0; r<tm.getRowCount(); r++)
			{
				String  colName   = (String) tm.getValueAt(r, TAB_POS_COL_NAME);

				boolean isPkCol   = (pkStr  .indexOf(colName+", ") != -1);
				boolean isDiffCol = (diffStr.indexOf(colName+", ") != -1);
				boolean isPctCol  = (pctStr .indexOf(colName+", ") != -1);
				boolean isGraphCol= (gColStr.indexOf(colName+", ") != -1);

				tm.setValueAt(new Boolean(isPkCol),   r, TAB_POS_COL_PK);
				tm.setValueAt(new Boolean(isDiffCol), r, TAB_POS_COL_DIFF);
				tm.setValueAt(new Boolean(isPctCol),  r, TAB_POS_COL_PCT);
				tm.setValueAt(new Boolean(isGraphCol),r, TAB_POS_CHECK);
				
				if (isGraphCol)
				{
					String[] gColStrArr = gColStr.split(",");
					String[] gMthStrArr = gMthStr.split(",");
					String[] gLblStrArr = gLblStr.split(",");

					for(int a=0; a<gColStrArr.length; a++)
					{
						if (colName.equals(gColStrArr[a].trim()))
						{
							String method = gMthStrArr[a].trim();
							String label  = gLblStrArr[a].trim();

							tm.setValueAt(method, r, TAB_POS_METHOD);
							tm.setValueAt(label,  r, TAB_POS_AXIS_LABEL);
							
							break;
						}
					}
				}
			}

		}
		_table.packAll(); // set size so that all content in all cells are visible
	}
	
	private void setEnabledAll(boolean b)
	{
		_graphType_lbl .setEnabled(b);
		_graphType_cbx .setEnabled(b);
		_name_lbl      .setEnabled(b);
		_name_txt      .setEnabled(b);
		_graphLabel_lbl.setEnabled(b);
		_graphLabel_txt.setEnabled(b);
		_menuDesc_lbl  .setEnabled(b);
		_menuDesc_txt  .setEnabled(b);
		_table         .setEnabled(b);
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
			boolean hasTrendGraph = cm.hasTrendGraph();
			if (hasTrendGraph)
			{
				_crGraph_chk.setSelected( true );

				// Get first row from the graph Map
				String gn = (String) cm.getTrendGraphs().keySet().iterator().next();
				TrendGraph tg = cm.getTrendGraph(gn);
				
				_name_txt      .setText( tg.getName() );
				_graphLabel_txt.setText( tg.getLabel() );
				_menuDesc_txt  .setText( tg.getViewMenuItem().getText() );

				int graphType = tg.getGraphType();
				if (graphType == TrendGraph.TYPE_BY_COL) _graphType_cbx.setSelectedIndex(0);
				if (graphType == TrendGraph.TYPE_BY_ROW) _graphType_cbx.setSelectedIndex(1);
				
				String[] colNames = tg.getDataColNames();
				String[] methods  = tg.getDataMethods();
				String[] labels   = tg.getDataLabels();

				TableModel tm = _table.getModel();
				for (int r=0; r<tm.getRowCount(); r++)
				{
					String  colName = (String) tm.getValueAt(r, TAB_POS_COL_NAME);
					boolean isGraphCol= StringUtil.arrayContains(colNames, colName);

					tm.setValueAt(new Boolean(isGraphCol),r, TAB_POS_CHECK);
					if (isGraphCol)
					{
						for(int a=0; a<colNames.length; a++)
						{
							if (colName.equals(colNames[a].trim()))
							{
								String method = methods[a].trim();
								String label  = labels[a].trim();

								tm.setValueAt(method, r, TAB_POS_METHOD);
								tm.setValueAt(label,  r, TAB_POS_AXIS_LABEL);
								
								break;
							}
						}
					}
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
			
			if (_name_txt.getText().length() == 0)
				_name_txt.setText( getWizardData("name").toString() + "Graph" );

			if (_graphLabel_txt.getText().length() == 0)
				_graphLabel_txt.setText( getWizardData("name").toString() + " Graph" );

			if (_menuDesc_txt.getText().length() == 0)
				_menuDesc_txt.setText( getWizardData("name").toString() + " Graph" );
		}

		_toolTipMonTables = StringUtil.commaStrToArray( (String)getWizardData("toolTipMonTables") );

	    _firtsTimeRender = false;
    }

	@Override
	protected String validateContents(Component comp, Object event)
	{
		if (_crGraph_chk.isSelected())
		{
			setEnabledAll(true);
		}
		else
		{
			setEnabledAll(false);
			return null;
		}
		String problem = "";
		if ( _name_txt      .getText().trim().length() <= 0) problem += "Name, ";
		if ( _graphLabel_txt.getText().trim().length() <= 0) problem += "Graph Label, ";
		if ( _menuDesc_txt  .getText().trim().length() <= 0) problem += "Menu Description, ";
		
		if (problem.length() > 0)
		{
			// Discard last ', '
			problem = problem.substring(0, problem.length()-2);
		}

		if (problem.length() > 0)
			problem = "Following fields cant be empty: " + problem;

		if ( _name_txt.getText().trim().indexOf(" ") >= 0) 
			problem = "The field 'Name' cant contain spaces.";

		if (problem.length() > 0)
			return problem;

		// GRAPH TYPE: BY COLUMN
		if (_graphType_cbx.getSelectedIndex() == 0)
		{
			int rows = 0;
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if ( ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue() )
				{
					if (tm.getValueAt(r, TAB_POS_METHOD).equals(NO_METHOD))
						return "Row "+(r+1)+" is selected, but no 'Method' has been choosen.";

					if ( ! ((Boolean)tm.getValueAt(r, TAB_POS_COL_DIFF)).booleanValue() )
					{
						String methodStr = (String) tm.getValueAt(r, TAB_POS_METHOD);
						if ( ! methodStr.startsWith("abs") )
							return "Row "+(r+1)+" is selected, but no 'Method' can only be 'abs*'. Diff/Rate is not available.";
					}
					rows++;
				}
			}
	
			return rows > 0 ? null : "Atleast one session needs to be checked.";
		}

		// GRAPH TYPE: BY ROW
		if (_graphType_cbx.getSelectedIndex() != 0)
		{
			int rows = 0;
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if ( ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue() )
				{
					if (tm.getValueAt(r, TAB_POS_METHOD).equals(NO_METHOD))
						return "Row "+(r+1)+" is selected, but no 'Method' has been choosen.";

					if ( ! ((Boolean)tm.getValueAt(r, TAB_POS_COL_DIFF)).booleanValue() )
					{
						String methodStr = (String) tm.getValueAt(r, TAB_POS_METHOD);
						if ( ! methodStr.startsWith("abs") )
							return "Row "+(r+1)+" is selected, but no 'Method' can only be 'abs*'. Diff/Rate is not available.";
					}
					rows++;
				}
			}
	
			return rows == 1 ? null : "One, and only one, must be selected.";
		}
		
		return null;
	}

	public void actionPerformed(ActionEvent ae)
	{
		JComponent src = (JComponent) ae.getSource();
		String name = (String)src.getClientProperty("NAME");
		if (name == null)
			name = "-null-";

		if (_graphType_cbx.equals(src))
		{
			JComboBox comboBox = new JComboBox();
			refreshComboBox(comboBox);
			TableColumn tc = _table.getColumnModel().getColumn(TAB_POS_METHOD);
			tc.setCellEditor(new DefaultCellEditor(comboBox));

			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				String label = (String) tm.getValueAt(r, TAB_POS_COL_NAME);
				if (_graphType_cbx.getSelectedIndex() == 1)
					label = "-pk-";
				tm.setValueAt(label, r, TAB_POS_AXIS_LABEL);

				tm.setValueAt(NO_METHOD, r, TAB_POS_METHOD);
			}
		}

//		System.out.println("Source("+name+"): " + src);
	}

	
	public void tableChanged(TableModelEvent e)
	{
		TableModel tm = _table.getModel();
		if (e.getType() == TableModelEvent.UPDATE)
		{
			int row = e.getFirstRow();
			int col = e.getColumn();

			boolean isChecked = ((Boolean)tm.getValueAt(row, TAB_POS_CHECK)).booleanValue();
			boolean hasMethod = ! tm.getValueAt(row, TAB_POS_METHOD).equals(NO_METHOD);

			if (col == TAB_POS_CHECK)
			{
				if ( ! isChecked )
					tm.setValueAt(NO_METHOD, row, TAB_POS_METHOD);
			}
			if (col == TAB_POS_METHOD)
			{
				if ( isChecked != hasMethod )
					tm.setValueAt(new Boolean(hasMethod), row, TAB_POS_CHECK);
			}
		}

		// This wasnt kicked off for a table change...
		setProblem(validateContents(null,null));
	}


	private void saveWizardData()
	{
		if (_crGraph_chk.isSelected())
		{
			String str1 = "";
			String str2 = "";
			String str3 = "";
			TableModel tm = _table.getModel();
			for (int r=0; r<tm.getRowCount(); r++)
			{
				if ( ((Boolean)tm.getValueAt(r, TAB_POS_CHECK)).booleanValue() )
				{
					str1 += tm.getValueAt(r, TAB_POS_COL_NAME)   + ", ";
					str2 += tm.getValueAt(r, TAB_POS_METHOD)     + ", ";
					str3 += tm.getValueAt(r, TAB_POS_AXIS_LABEL) + ", ";
				}
			}
	
			// Discard last ', '
			if (str1.length() > 0) str1 = str1.substring(0, str1.length()-2);
			if (str2.length() > 0) str2 = str2.substring(0, str2.length()-2);
			if (str3.length() > 0) str3 = str3.substring(0, str3.length()-2);

			String graphType = _validGraphTypesStr[_graphType_cbx.getSelectedIndex()];
			putWizardData("graph.type", graphType);
				
			putWizardData("graph.data.cols",    str1);
			putWizardData("graph.data.methods", str2);
			putWizardData("graph.data.labels",  str3);
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

