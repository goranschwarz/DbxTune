package com.asetune.test;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import com.asetune.gui.swing.MultiSortTableCellHeaderRenderer;


public class MultiSortTable
{
	public static void main(String args[])
	{
		Runnable runner = new Runnable()
		{
			public void run()
			{
				JFrame frame = new JFrame("Sorting JTable");
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				Object rows[][] = { 
						{ "AMZN", "Amazon",          41.28, "BUY" }, 
						{ "EBAY", "eBay",            41.57, "BUY" }, 
						{ "GOOG", "Google",         388.33, "SELL" }, 
						{ "MSFT", "Microsoft",       26.56, "SELL" },
						{ "NOK",  "Nokia Corp",      17.13, "BUY" }, 
						{ "ORCL", "Oracle Corp.",    12.52, "BUY" }, 
						{ "SUNW", "Sun Microsystems", 3.86, "BUY" }, 
						{ "TWX",  "Time Warner",     17.66, "SELL" }, 
						{ "VOD",  "Vodafone Group",  26.02, "SELL" }, 
						{ "YHOO", "Yahoo!",          37.69, "BUY" } };
				String columns[] = { "Symbol", "Name", "Price", "Guidance" };
				TableModel model = new DefaultTableModel(rows, columns)
				{
					public Class getColumnClass(int column)
					{
						Class returnValue;
						if ( (column >= 0) && (column < getColumnCount()) )
						{
							returnValue = getValueAt(0, column).getClass();
						}
						else
						{
							returnValue = Object.class;
						}
						return returnValue;
					}
				};

				JTable table = new JTable(model);
				RowSorter sorter = new TableRowSorter(model);
				table.setRowSorter(sorter);
				table.getTableHeader().setDefaultRenderer(new MultiSortTableCellHeaderRenderer());

				JScrollPane pane = new JScrollPane(table);
				frame.add(pane, BorderLayout.CENTER);
				frame.setSize(300, 150);
				frame.setVisible(true);
			}
		};
		EventQueue.invokeLater(runner);
	}
}

//@SuppressWarnings("serial")
//class MultiSortTableCellHeaderRenderer extends DefaultTableCellRenderer
//{
//	protected SortIcon	sortIcon	= new SortIcon(8);
//
//	public MultiSortTableCellHeaderRenderer()
//	{
//		setHorizontalAlignment(0);
//		setHorizontalTextPosition(10);
//	}
//
//	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
//	{
//		JTableHeader tableHeader = table.getTableHeader();
//		Color fg = null;
//		Color bg = null;
//		Border border = null;
//		Icon icon = null;
//
//		if ( hasFocus )
//		{
//			fg = UIManager.getColor("TableHeader.focusCellForeground");
//			bg = UIManager.getColor("TableHeader.focusCellBackground");
//			border = UIManager.getBorder("TableHeader.focusCellBorder");
//		}
//
//		if ( fg == null )
//			fg = tableHeader.getForeground();
//		if ( bg == null )
//			bg = tableHeader.getBackground();
//		if ( border == null )
//			border = UIManager.getBorder("TableHeader.cellBorder");
//		if ( !tableHeader.isPaintingForPrint() && table.getRowSorter() != null )
//			icon = getSortIcon(table, table.convertColumnIndexToModel(column));
//
//		setFont(tableHeader.getFont());
//		setText(value != null && value != "" ? value.toString() : " ");
//		setBorder(border);
//		setIcon(icon);
//
//		return this;
//	}
//
//	protected Icon getSortIcon(JTable table, int column)
//	{
//		List<? extends SortKey> sortKeys = table.getRowSorter().getSortKeys();
//		if ( sortKeys == null || sortKeys.size() == 0 )
//			return null;
//
//		int priority = 0;
//		for (SortKey sortKey : sortKeys)
//		{
//			if ( sortKey.getColumn() == column )
//			{
//				sortIcon.setPriority(priority);
//				sortIcon.setSortOrder(sortKey.getSortOrder());
//				return sortIcon;
//			}
//
//			priority++;
//		}
//
//		return null;
//	}
//}
//
//class SortIcon implements Icon, SwingConstants
//{
//	private int					baseSize;
//	private int					size;
//	private int					direction;
//	private BasicArrowButton	iconRenderer;
////	private double[]			sizePercentages	= { 1.0, .85, .70, .55, .40, .25, .10 };
//	private double[]			sizePercentages	= { .70, .70, .70, .70, .70, .70, .70 };
//	private int                _priority = 0;
//
//	public SortIcon(int size)
//	{
//		this.baseSize = this.size = size;
//		iconRenderer = new BasicArrowButton(direction);
//	}
//
//	public void setPriority(int priority)
//	{
//		_priority = priority;
//		size = (int) (baseSize * sizePercentages[priority]);
//	}
//
//	public void setSortOrder(SortOrder sortOrder)
//	{
//		direction = sortOrder == SortOrder.ASCENDING ? NORTH : SOUTH;
//	}
//
//	public void paintIcon(Component c, Graphics g, int x, int y)
//	{
//		iconRenderer.paintTriangle(g, x, y, size, direction, true);
//
//		Font f = g.getFont();
//		g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 0.75f));
////		g.drawString(Integer.toString(_priority+1), x+6, y+4);
//		g.drawString(Integer.toString(_priority+1), x+3, y+1);
//		g.setFont(f);
//	}
//
//	public int getIconWidth()
//	{
//		return size;
//	}
//
//	public int getIconHeight()
//	{
//		return size / 2;
//	}
//}
