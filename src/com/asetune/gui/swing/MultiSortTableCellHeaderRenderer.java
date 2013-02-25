package com.asetune.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

@SuppressWarnings("serial")
public class MultiSortTableCellHeaderRenderer
extends DefaultTableCellRenderer
{
	protected SortIcon	sortIcon	= new SortIcon(8);

	public MultiSortTableCellHeaderRenderer()
	{
		setHorizontalAlignment(0);
		setHorizontalTextPosition(10);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		JTableHeader tableHeader = table.getTableHeader();
		Color fg = null;
		Color bg = null;
		Border border = null;
		Icon icon = null;

		if ( hasFocus )
		{
			fg = UIManager.getColor("TableHeader.focusCellForeground");
			bg = UIManager.getColor("TableHeader.focusCellBackground");
			border = UIManager.getBorder("TableHeader.focusCellBorder");
		}

		if ( fg == null )
			fg = tableHeader.getForeground();
		if ( bg == null )
			bg = tableHeader.getBackground();
		if ( border == null )
			border = UIManager.getBorder("TableHeader.cellBorder");
		if ( !tableHeader.isPaintingForPrint() && table.getRowSorter() != null )
			icon = getSortIcon(table, table.convertColumnIndexToModel(column));

		setFont(tableHeader.getFont());
		setText(value != null && value != "" ? value.toString() : " ");
		setBorder(border);
		setIcon(icon);

		return this;
	}

	protected Icon getSortIcon(JTable table, int column)
	{
		List<? extends SortKey> sortKeys = table.getRowSorter().getSortKeys();
		if ( sortKeys == null || sortKeys.size() == 0 )
			return null;

		int priority = 0;
		for (SortKey sortKey : sortKeys)
		{
			if (sortKey.getSortOrder() == SortOrder.UNSORTED)
				continue;

			if ( sortKey.getColumn() == column )
			{
//				if      (sortKey.getSortOrder() == SortOrder.ASCENDING)  System.out.println("  sortOrder=ASCENDING  : "+table.getColumnName(sortKey.getColumn()));
//				else if (sortKey.getSortOrder() == SortOrder.DESCENDING) System.out.println("  sortOrder=DESCENDING : "+table.getColumnName(sortKey.getColumn()));
//				else                                                     System.out.println("--sortOrder="+sortKey.getSortOrder()+"   : "+table.getColumnName(sortKey.getColumn()));

				sortIcon.setPriority(priority);
				sortIcon.setSortOrder(sortKey.getSortOrder());
				return sortIcon;
			}

			priority++;
		}

		return null;
	}
}

class SortIcon implements Icon, SwingConstants
{
	private int					baseSize;
	private int					size;
	private int					direction;
	private BasicArrowButton	iconRenderer;
//	private double[]			sizePercentages	= { 1.0, .85, .70, .55, .40, .25, .10 };
//	private double[]			sizePercentages	= { .70, .70, .70, .70, .70, .70, .70 };
	private double[]			sizePercentages	= { .55, .55, .55, .55, .55, .55, .55 };
	private int                _priority = 0;

	public SortIcon(int size)
	{
		this.baseSize = this.size = size;
		iconRenderer = new BasicArrowButton(direction);
	}

	public void setPriority(int priority)
	{
		_priority = priority;
		size = (int) (baseSize * sizePercentages[priority]);
	}

	public void setSortOrder(SortOrder sortOrder)
	{
		direction = (sortOrder == SortOrder.ASCENDING) ? NORTH : SOUTH;
	}

//	public void paintIcon(Component c, Graphics g, int x, int y)
//	{
//		iconRenderer.paintTriangle(g, x, y+2, size, direction, true);
//
//		Font f = g.getFont();
////		g.setFont(f.deriveFont(Font.BOLD, f.getSize() * 0.75f));
//		g.setFont(f.deriveFont(Font.PLAIN, f.getSize() * 0.75f));
//		g.drawString(Integer.toString(_priority+1), x+1, y+1);
//		g.setFont(f);
//	}
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		iconRenderer.paintTriangle(g, x, y-4, size, direction, true);

		Font f = g.getFont();
		g.setFont(new Font(Font.DIALOG_INPUT, Font.PLAIN, 10));
		g.drawString(Integer.toString(_priority+1), x+5, y+1);
		g.setFont(f);
	}

	@Override
	public int getIconWidth()
	{
		return size + 5;
	}

	@Override
	public int getIconHeight()
	{
		return size / 2;
	}
}
