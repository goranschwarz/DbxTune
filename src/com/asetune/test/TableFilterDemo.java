/*******************************************************************************
 * Copyright (C) 2010-2020 Goran Schwarz
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
package com.asetune.test;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;

public class TableFilterDemo 
extends JPanel
implements TableModelListener
{
	private static final long	serialVersionUID	= 1L;

	private boolean							DEBUG	= false;
	private JXTable							table;
	private JTextField						filterText;
	private JTextField						statusText;
//	private TableRowSorter<MyTableModel>	sorter;

	@Override
	public void tableChanged(TableModelEvent e)
	{
		table.tableChanged(e);
table.getTableHeader().setReorderingAllowed(true);
		table.packAll();
//		SwingUtils.calcColumnWidths(table);
	}

	public TableFilterDemo()
	{
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

//		ColumnFactory.setInstance(new ColumnFactory()
//		{
//			@Override
//			protected int getRowCount(JXTable table)
//			{
//				System.out.println("table.rowcount="+table.getRowCount()+", model.rowCount="+table.getModel().getRowCount());
//				return table.getModel().getRowCount();
//			}
//		});


		// Create a table with a sorter.
		final MyTableModel model = new MyTableModel();
//		sorter = new TableRowSorter<MyTableModel>(model);
		table = new JXTable(model);
//		table.setRowSorter(sorter);
		table.setPreferredScrollableViewportSize(new Dimension(500, 700));
		table.setFillsViewportHeight(true);
		
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.packAll(); // set size so that all content in all cells are visible
		table.setSortable(true);
		table.setSortOrderCycle(SortOrder.ASCENDING, SortOrder.DESCENDING, SortOrder.UNSORTED);
		table.setColumnControlVisible(true);
		table.setHighlighters(_highliters); // a variant of cell render

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String str = (String) adapter.getValue(adapter.getColumnIndex("FirstName"));
				if ("".equals(str) || "xxx".equals(str))
					return true;
				return false;
			}
		}, Color.RED, null));

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String str = (String) adapter.getValue(adapter.getColumnIndex("LastName"));
				if ("".equals(str) || "xxx".equals(str))
					return true;
				return false;
			}
		}, Color.GREEN, null));

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				String str = (String) adapter.getValue(adapter.getColumnIndex("Sport"));
				if ("".equals(str) || "xxx".equals(str))
					return true;
				return false;
			}
		}, Color.CYAN, null));

		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Boolean bool = (Boolean) adapter.getValue(adapter.getColumnIndex("Vegetarian"));
//				if ("".equals(str) || "xxx".equals(str))
//					return true;
//				return false;
				return bool != null && bool;
			}
		}, Color.YELLOW, null));
		
		addHighlighter( new ColorHighlighter(new HighlightPredicate()
		{
			@Override
			public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
			{
				Integer years = (Integer) adapter.getValue(adapter.getColumnIndex("# of Years"));
				return years != null && (years.intValue() % 3) == 0;
			}
		}, Color.PINK, null));
		
		
		// Deffer call to tableChanged() in the table, this will be done in the local tableChanged 
		model.removeTableModelListener(table);
		model.addTableModelListener(this);

		// This would be the dummy-get-info thread
		Thread changeTableThread = new Thread()
		{
			@Override
			public void run()
			{
				int count = 0;
				while(true)
				{
					count++;
					System.out.println("Adding: "+count);
					model.addRow("goran-"+count, "Schwarz-"+count, "any-"+count, count, false);
					model.addRow("håkan-"+count, "Schwarz-"+count, "all-"+count, count, false);

					System.out.println(" - Sleeping ");
					try { Thread.sleep(2500);}
					catch (InterruptedException ignore) {}

					System.out.println(" - Deleting ");
					model.deleteRow(-1);

					System.out.println(" - Sleeping ");
					try { Thread.sleep(2500);}
					catch (InterruptedException ignore) {}

				}
			}
		};
		changeTableThread.setName("changeTableThread");
		changeTableThread.setDaemon(true);
		changeTableThread.start();

		// For the purposes of this example, better to have a single
		// selection.
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// When selection changes, provide user with row numbers for
		// both view and model.
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent event)
			{
				int viewRow = table.getSelectedRow();
				if ( viewRow < 0 )
				{
					// Selection got filtered away.
					statusText.setText("");
				}
				else
				{
					int modelRow = table.convertRowIndexToModel(viewRow);
					statusText.setText(String.format("Selected Row in view: %d. " + "Selected Row in model: %d.", viewRow, modelRow));
				}
			}
		});

		// Create the scroll pane and add the table to it.
		JScrollPane scrollPane = new JScrollPane(table);

		// Add the scroll pane to this panel.
		add(scrollPane);

		// Create a separate form for filterText and statusText
		JPanel form = new JPanel(new SpringLayout());
		JLabel l1 = new JLabel("Filter Text:", SwingConstants.TRAILING);
		form.add(l1);
		filterText = new JTextField();
		// Whenever filterText changes, invoke newFilter.
		filterText.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				newFilter();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				newFilter();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				newFilter();
			}
		});
		l1.setLabelFor(filterText);
		form.add(filterText);
		JLabel l2 = new JLabel("Status:", SwingConstants.TRAILING);
		form.add(l2);
		statusText = new JTextField();
		l2.setLabelFor(statusText);
		form.add(statusText);
		SpringUtilities.makeCompactGrid(form, 2, 2, 6, 6, 6, 6);
		add(form);
	}

	/**
	 * Update the row filter regular expression from the expression in the text
	 * box.
	 */
	private void newFilter()
	{
//		RowFilter<MyTableModel, Object> rf = null;
		RowFilter<TableModel, Integer> rf = null;
		// If current expression doesn't parse, don't update.
		try
		{
			rf = RowFilter.regexFilter(filterText.getText(), 0);
		}
		catch (java.util.regex.PatternSyntaxException e)
		{
			return;
		}
//		sorter.setRowFilter(rf);
		table.setRowFilter(rf);
	}

	private static enum ModelOperation {INSERT, UPDATE, DELETE};
	
	class MyTableModel extends AbstractTableModel
	{
		private static final long	serialVersionUID	= 1L;

		private String[]	columnNames	= { "FirstName", "LastName", "Sport", "# of Years", "Vegetarian" };
//		private Object[][]	data		= { { "Kathy", "Smith", "Snowboarding", new Integer(5), new Boolean(false) }, { "John", "Doe", "Rowing", new Integer(3), new Boolean(true) }, { "Sue", "Black", "Knitting", new Integer(2), new Boolean(false) }, { "Jane", "White", "Speed reading", new Integer(20), new Boolean(true) }, { "Joe", "Brown", "Pool", new Integer(10), new Boolean(false) } };
		private List<List<Object>>data = new ArrayList<List<Object>>();

		public void printTableModelListeners()
		{
			int i=0;
			for (TableModelListener tml : getTableModelListeners())
			{
				System.out.println("       - tml("+(i++)+"): "+tml);
			}
		}

		private void fireXXX(final ModelOperation op, final int row)
		{
			Runnable doWork = new Runnable()
			{
				@Override
				public void run()
				{
					if (op == ModelOperation.DELETE)
						fireTableRowsDeleted(row, row);
						
					fireTableDataChanged();
					printTableModelListeners();
				}
			};

			// Invoke this job on the SWING Event Dispather Thread
			if ( ! SwingUtilities.isEventDispatchThread() )
			{
				System.out.println(" --FIRE: LATER in EventDispatchThread, I'm not EDT ("+Thread.currentThread().getName()+").");
				SwingUtilities.invokeLater(doWork);
			}
			else
			{
				System.out.println(" --FIRE: NOW i must be EDT ("+Thread.currentThread().getName()+").");
				doWork.run();
			}
		}

		public void addRow(String firstName, String lastName, String sport, Integer numYears, Boolean vegeterian)
		{
			List<Object> row = new ArrayList<Object>();
			row.add(firstName);
			row.add(lastName);
			row.add(sport);
			row.add(numYears);
			row.add(vegeterian);

			data.add(row);
			fireXXX(ModelOperation.INSERT, -1);
		}

		public void deleteRow(int row)
		{
			if (data.size() == 0)
			{
				System.out.println("Table is EMPTY.");
				return;
			}
			if (row < 0)
				data.remove( data.size() - 1 );
			else
				data.remove( row );

			fireXXX(ModelOperation.DELETE, row);
		}
		
		public MyTableModel()
		{
			init();
		}

		public void init()
		{
//			addRow("Kathy", "Smith", "Snowboarding",  new Integer(5),  new Boolean(false));
//			addRow("John",  "Doe",   "Rowing",        new Integer(3),  new Boolean(true));
//			addRow("Sue",   "Black", "Knitting",      new Integer(2),  new Boolean(false));
//			addRow("Jane",  "White", "Speed reading", new Integer(20), new Boolean(true));
//			addRow("Joe",   "Brown", "Pool",          new Integer(10), new Boolean(false));

			addRow("Kathy2", "Smith", "Snowboarding",  5,  false);
			addRow("John2",  "Doe",   "Rowing",        3,  true);
			addRow("Sue2",   "Black", "Knitting",      2,  false);
			addRow("Jane2",  "White", "Speed reading", 20, true);
			addRow("Joe2",   "Brown", "Pool",          10, false);

			System.out.println("DONE: init()");
		}

		@Override
		public int getColumnCount()
		{
			return columnNames.length;
		}

		@Override
		public int getRowCount()
		{
			//System.out.println("getRowCount() == "+data.size());
			return data.size();
		}

		@Override
		public String getColumnName(int col)
		{
			return columnNames[col];
		}

		@Override
		public Object getValueAt(int row, int col)
		{
			if (row >= data.size())
			{
				try {data.get(row);}
				catch(IndexOutOfBoundsException e) 
				{
					System.out.println("ERROR: row="+row+", col="+col+", model.rows="+(data.size()-1)+", Exception="+e );
					e.printStackTrace();
				}
				return null;
			}
			return data.get(row).get(col);
		}

		/*
		 * JTable uses this method to determine the default renderer/ editor for
		 * each cell. If we didn't implement this method, then the last column
		 * would contain text ("true"/"false"), rather than a check box.
		 */
		@Override
		public Class<?> getColumnClass(int c)
		{
			return getValueAt(0, c).getClass();
		}

		/*
		 * Don't need to implement this method unless your table's editable.
		 */
		@Override
		public boolean isCellEditable(int row, int col)
		{
			// Note that the data/cell address is constant,
			// no matter where the cell appears onscreen.
			if ( col < 2 )
			{
				return false;
			}
			else
			{
				return true;
			}
		}

		/*
		 * Don't need to implement this method unless your table's data can
		 * change.
		 */
		@Override
		public void setValueAt(Object value, int row, int col)
		{
			if ( DEBUG )
			{
				System.out.println("Setting value at " + row + "," + col + " to " + value + " (an instance of " + value.getClass() + ")");
			}

//			data[row][col] = value;
			data.get(row).set(col, value);
			fireTableCellUpdated(row, col);

			if ( DEBUG )
			{
				System.out.println("New value of data:");
				printDebugData();
			}
		}

		private void printDebugData()
		{
			int numRows = getRowCount();
			int numCols = getColumnCount();

			for (int i = 0; i < numRows; i++)
			{
				System.out.print("    row " + i + ":");
				for (int j = 0; j < numCols; j++)
				{
					System.out.print("  " + data.get(i).get(j));
				}
				System.out.println();
			}
			System.out.println("--------------------------");
		}
	}

	/**
	 * Create the GUI and show it. For thread safety, this method should be
	 * invoked from the event-dispatching thread.
	 */
	private static void createAndShowGUI()
	{
		// Create and set up the window.
		JFrame frame = new JFrame("TableFilterDemo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Create and set up the content pane.
		TableFilterDemo newContentPane = new TableFilterDemo();
		newContentPane.setOpaque(true); // content panes must be opaque
		frame.setContentPane(newContentPane);

		// Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args)
	{
		// Schedule a job for the event-dispatching thread:
		// creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				createAndShowGUI();
			}
		});
	}






	
	
	/*---------------------------------------------------
	 ** BEGIN: Highlighter stuff for the JXTable
	 **---------------------------------------------------
	 */
	public void addHighlighter(Highlighter highlighter)
	{
		table.addHighlighter(highlighter);
	}

	private HighlightPredicate	_highligtIfDelta = new HighlightPredicate()
	{
		@Override
		public boolean isHighlighted(Component renderer, ComponentAdapter adapter)
		{
			return adapter.convertColumnIndexToModel(adapter.column) == 3;
		}
	};

	private Highlighter[] _highliters = { 
			new HighlighterDiffData(_highligtIfDelta), 
			// ,HighlighterFactory.createSimpleStriping()
		};

	private static class HighlighterDiffData extends AbstractHighlighter
	{
		public HighlighterDiffData(HighlightPredicate predicate)
		{
			super(predicate);
		}

		@Override
		protected Component doHighlight(Component comp, ComponentAdapter adapter)
		{
//System.out.println("doHighlight(): model.rows()="+(adapter.getRowCount()-1)+", adapter.row="+adapter.row);
			Object value = adapter.getFilteredValueAt(adapter.row, adapter.convertColumnIndexToModel(adapter.column));
			if ( value instanceof Number )
			{
				comp.setForeground(Color.BLUE);
				if ( ((Number) value).doubleValue() != 0 )
				{
					comp.setFont(comp.getFont().deriveFont(Font.BOLD));
				}
			}
			return comp;
		}
	}

}
